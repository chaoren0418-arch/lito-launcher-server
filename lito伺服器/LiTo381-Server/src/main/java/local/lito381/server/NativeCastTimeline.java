// DECOMPILED (CFR 0.152) from reference/base-0.6.71.jar — TODO: replace with original source (T002)
/*
 * Decompiled with CFR 0.152.
 */
package local.lito381.server;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import local.lito381.server.BattleAnimationCatalog;
import local.lito381.server.BattleSpellCatalog;
import local.lito381.server.BattleTimedStatus;
import local.lito381.server.BattleWindShackleStatus;
import local.lito381.server.ClientSession;
import local.lito381.server.CombatAccess;
import local.lito381.server.NativeCastMaterial;
import local.lito381.server.NativeDeItemApplication;
import local.lito381.server.NativeLightAction;
import local.lito381.server.NativeReuseActor;
import local.lito381.server.NativeReuseGate;
import local.lito381.server.NativeReuseLog;
import local.lito381.server.NativeSourceActionIntervalCalculator;
import local.lito381.server.NativeSourceClientActionTimeline;
import local.lito381.server.NativeSourcePacketExecutionContext;
import local.lito381.server.NativeSourceSkillCastAdmission;
import local.lito381.server.NativeSourceSkillDelay;
import local.lito381.server.NativeSourceSpriteActionSpeed;
import local.lito381.server.Protocol;

final class NativeCastTimeline {
    private static final ThreadLocal<Arrival> packet = new ThreadLocal();

    static byte[] readFrame(InputStream inputStream, int n, ClientSession pc) throws IOException {
        byte[] byArray = Protocol.readFrame(inputStream, n);
        pc.nativeCastTimelineState().received = byArray == null ? null : new Arrival(pc, System.nanoTime(), System.currentTimeMillis(), Thread.currentThread().getId());
        return byArray;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    static void handle(ClientSession pc, byte[] byArray) throws Exception {
        State state = pc.nativeCastTimelineState();
        Arrival arrival = state.received;
        if (arrival != null && arrival.readerThread == Thread.currentThread().getId()) {
            state.received = null;
        } else {
            arrival = new Arrival(pc, System.nanoTime(), System.currentTimeMillis(), Thread.currentThread().getId());
        }
        Arrival arrival2 = packet.get();
        packet.set(arrival);
        try {
            pc.nativeTimelineHandle(byArray);
        }
        finally {
            NativeCastTimeline.restore(arrival2);
        }
    }

    static Arrival capture(ClientSession pc) {
        Arrival arrival = packet.get();
        if (arrival == null) {
            packet.remove();
        }
        return arrival != null && arrival.pc == pc ? arrival : null;
    }

    static Arrival detach() {
        Arrival arrival = packet.get();
        packet.remove();
        return arrival;
    }

    static void restore(Arrival arrival) {
        if (arrival == null) {
            packet.remove();
        } else {
            packet.set(arrival);
        }
    }

    static boolean legacyWindShackle(ClientSession pc, int n, String string) {
        if (NativeLightAction.timelineIngress(pc)) {
            return true;
        }
        return pc.nativeCastLegacyWindShackleAction(n, string);
    }

    static int interval(ClientSession pc, int n) {
        BattleAnimationCatalog.Animation animation = BattleAnimationCatalog.get().requireTimedAction(pc.battleGfx(), n);
        int n2 = NativeSourceSpriteActionSpeed.calculate(animation.frameCount(), animation.frameRate());
        if (n2 <= 0) {
            return 0;
        }
        long l = System.nanoTime();
        BattleTimedStatus battleTimedStatus = (BattleTimedStatus)CombatAccess.field(pc, "timedStatus");
        BattleTimedStatus.Speed speed = battleTimedStatus.speed(l);
        int n3 = battleTimedStatus.brave(l).mode();
        BattleWindShackleStatus battleWindShackleStatus = (BattleWindShackleStatus)CombatAccess.field(pc, "windShackleStatus");
        NativeSourceActionIntervalCalculator.SpeedState speedState = new NativeSourceActionIntervalCalculator.SpeedState(speed.type() == 1, speed.type() == 2, n3 == 4 || n3 == 5, n3 == 6, n3 == 1, false, n3 == 3, battleWindShackleStatus.snapshot(l).active());
        return NativeSourceActionIntervalCalculator.calculate(n2, NativeSourceActionIntervalCalculator.ActionType.SPELL, speedState);
    }

    static boolean admit(final ClientSession pc, final BattleSpellCatalog.LightSpell spell, Arrival arrival) {
        if (!pc.nativePlayerActionLock().isHeldByCurrentThread()) {
            throw new IllegalStateException("cast admission requires actual action lock");
        }
        State state = pc.nativeCastTimelineState();
        NativeDeItemApplication.Owner owner = NativeDeItemApplication.publicationOwner(pc);
        if (owner == null) {
            return false;
        }
        NativeReuseActor nativeReuseActor = NativeReuseGate.actor(pc);
        if (nativeReuseActor == null) {
            return false;
        }
        if (state.owner != owner) {
            state.timeline.clear();
            state.owner = owner;
        }
        int n = NativeCastTimeline.interval(pc, spell.actionId());
        if (n <= 0) {
            return false;
        }
        boolean[] blArray = new boolean[1];
        Runnable runnable = () -> {
            NativeSourceSkillCastAdmission.Result result = NativeSourceSkillCastAdmission.admit(() -> {
                if (nativeReuseActor.isSkillDelay()) {
                    return false;
                }
                long l = System.nanoTime();
                for (Long l2 : ((Map<?, Long>)CombatAccess.field(pc, "spellReadyNanos")).values()) {
                    if (l2 - l > 0L) return false;
                }
                return state.timeline.canReserveSkillAction();
            }, () -> {
                if (nativeReuseActor.isSkillDelay()) {
                    return null;
                }
                final NativeSourceClientActionTimeline.Reservation action = state.timeline.tryReserveSkillAction(n);
                if (action == null) {
                    return null;
                }
                final NativeSourceSkillDelay.Lease reuse = NativeSourceSkillDelay.tryReserveSkillUse(nativeReuseActor, spell.reuseDelayMillis());
                if (reuse == null) {
                    action.cancel();
                    return null;
                }
                return new NativeSourceSkillCastAdmission.Reservation(){

                    @Override
                    public void commit() {
                        if (!action.commitAtRuntime()) {
                            NativeReuseLog.getLogger(NativeCastTimeline.class).error("Skill action runtime rebase lost exact token skillId={} player={}", spell.skillId(), pc.selectedCharacterName());
                        }
                        reuse.commit();
                    }

                    @Override
                    public void cancel() {
                        action.cancel();
                        reuse.cancel();
                    }
                };
            }, () -> NativeCastMaterial.commit(pc, spell.skillId()));
            blArray[0] = result == NativeSourceSkillCastAdmission.Result.ACCEPTED;
        };
        if (arrival == null) {
            runnable.run();
        } else {
            NativeSourcePacketExecutionContext.runWithArrival(arrival.nanos, arrival.epochMillis, NativeSourcePacketExecutionContext.Lane.COMBAT, runnable);
        }
        return blArray[0];
    }

    private NativeCastTimeline() {
    }

    static boolean admitSelf(final ClientSession pc, final int skillId, int n2, int n3, Arrival arrival) {
        if (!pc.nativePlayerActionLock().isHeldByCurrentThread()) {
            throw new IllegalStateException("cast admission requires actual action lock");
        }
        State state = pc.nativeCastTimelineState();
        NativeDeItemApplication.Owner owner = NativeDeItemApplication.publicationOwner(pc);
        if (owner == null) {
            return false;
        }
        NativeReuseActor nativeReuseActor = NativeReuseGate.actor(pc);
        if (nativeReuseActor == null) {
            return false;
        }
        if (state.owner != owner) {
            state.timeline.clear();
            state.owner = owner;
        }
        int n4 = NativeCastTimeline.interval(pc, n2);
        if (n4 <= 0) {
            return false;
        }
        boolean[] blArray = new boolean[1];
        Runnable runnable = () -> {
            NativeSourceSkillCastAdmission.Result result = NativeSourceSkillCastAdmission.admit(() -> {
                if (nativeReuseActor.isSkillDelay()) {
                    return false;
                }
                long l = System.nanoTime();
                for (Long l2 : ((Map<?, Long>)CombatAccess.field(pc, "spellReadyNanos")).values()) {
                    if (l2 - l > 0L) return false;
                }
                return state.timeline.canReserveSkillAction();
            }, () -> {
                if (nativeReuseActor.isSkillDelay()) {
                    return null;
                }
                final NativeSourceClientActionTimeline.Reservation action = state.timeline.tryReserveSkillAction(n4);
                if (action == null) {
                    return null;
                }
                final NativeSourceSkillDelay.Lease reuse = NativeSourceSkillDelay.tryReserveSkillUse(nativeReuseActor, n3);
                if (reuse == null) {
                    action.cancel();
                    return null;
                }
                return new NativeSourceSkillCastAdmission.Reservation(){

                    @Override
                    public void commit() {
                        if (!action.commitAtRuntime()) {
                            NativeReuseLog.getLogger(NativeCastTimeline.class).error("Skill action runtime rebase lost exact token skillId={} player={}", skillId, pc.selectedCharacterName());
                        }
                        reuse.commit();
                    }

                    @Override
                    public void cancel() {
                        action.cancel();
                        reuse.cancel();
                    }
                };
            }, () -> NativeCastMaterial.commit(pc, skillId));
            blArray[0] = result == NativeSourceSkillCastAdmission.Result.ACCEPTED;
        };
        if (arrival == null) {
            runnable.run();
        } else {
            NativeSourcePacketExecutionContext.runWithArrival(arrival.nanos, arrival.epochMillis, NativeSourcePacketExecutionContext.Lane.COMBAT, runnable);
        }
        return blArray[0];
    }

    static final class State {
        final NativeSourceClientActionTimeline timeline = new NativeSourceClientActionTimeline();
        volatile Arrival received;
        NativeDeItemApplication.Owner owner;
        NativeReuseActor reuseActor;

        State() {
        }
    }

    record Arrival(ClientSession pc, long nanos, long epochMillis, long readerThread) {
    }
}
