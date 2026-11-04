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

    static byte[] readFrame(InputStream inputStream, int n, ClientSession clientSession) throws IOException {
        byte[] byArray = Protocol.readFrame(inputStream, n);
        clientSession.nativeCastTimelineState().received = byArray == null ? null : new Arrival(clientSession, System.nanoTime(), System.currentTimeMillis(), Thread.currentThread().getId());
        return byArray;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    static void handle(ClientSession clientSession, byte[] byArray) throws Exception {
        State state = clientSession.nativeCastTimelineState();
        Arrival arrival = state.received;
        if (arrival != null && arrival.readerThread == Thread.currentThread().getId()) {
            state.received = null;
        } else {
            arrival = new Arrival(clientSession, System.nanoTime(), System.currentTimeMillis(), Thread.currentThread().getId());
        }
        Arrival arrival2 = packet.get();
        packet.set(arrival);
        try {
            clientSession.nativeTimelineHandle(byArray);
        }
        finally {
            NativeCastTimeline.restore(arrival2);
        }
    }

    static Arrival capture(ClientSession clientSession) {
        Arrival arrival = packet.get();
        if (arrival == null) {
            packet.remove();
        }
        return arrival != null && arrival.pc == clientSession ? arrival : null;
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

    static boolean legacyWindShackle(ClientSession clientSession, int n, String string) {
        if (NativeLightAction.timelineIngress(clientSession)) {
            return true;
        }
        return clientSession.nativeCastLegacyWindShackleAction(n, string);
    }

    static int interval(ClientSession clientSession, int n) {
        BattleAnimationCatalog.Animation animation = BattleAnimationCatalog.get().requireTimedAction(clientSession.battleGfx(), n);
        int n2 = NativeSourceSpriteActionSpeed.calculate(animation.frameCount(), animation.frameRate());
        if (n2 <= 0) {
            return 0;
        }
        long l = System.nanoTime();
        BattleTimedStatus battleTimedStatus = (BattleTimedStatus)CombatAccess.field(clientSession, "timedStatus");
        BattleTimedStatus.Speed speed = battleTimedStatus.speed(l);
        int n3 = battleTimedStatus.brave(l).mode();
        BattleWindShackleStatus battleWindShackleStatus = (BattleWindShackleStatus)CombatAccess.field(clientSession, "windShackleStatus");
        NativeSourceActionIntervalCalculator.SpeedState speedState = new NativeSourceActionIntervalCalculator.SpeedState(speed.type() == 1, speed.type() == 2, n3 == 4 || n3 == 5, n3 == 6, n3 == 1, false, n3 == 3, battleWindShackleStatus.snapshot(l).active());
        return NativeSourceActionIntervalCalculator.calculate(n2, NativeSourceActionIntervalCalculator.ActionType.SPELL, speedState);
    }

    static boolean admit(final ClientSession clientSession, final BattleSpellCatalog.LightSpell lightSpell, Arrival arrival) {
        int n;
        if (!clientSession.nativePlayerActionLock().isHeldByCurrentThread()) {
            throw new IllegalStateException("cast admission requires actual action lock");
        }
        State state = clientSession.nativeCastTimelineState();
        NativeDeItemApplication.Owner owner = NativeDeItemApplication.publicationOwner(clientSession);
        if (owner == null) {
            return false;
        }
        NativeReuseActor nativeReuseActor = NativeReuseGate.actor(clientSession);
        if (nativeReuseActor == null) {
            return false;
        }
        if (state.owner != owner) {
            state.timeline.clear();
            state.owner = owner;
        }
        if ((n = NativeCastTimeline.interval(clientSession, lightSpell.actionId())) <= 0) {
            return false;
        }
        boolean[] blArray = new boolean[1];
        Runnable runnable = () -> {
            NativeSourceSkillCastAdmission.Result result = NativeSourceSkillCastAdmission.admit(() -> {
                if (nativeReuseActor.isSkillDelay()) {
                    return false;
                }
                long l = System.nanoTime();
                for (Long l2 : ((Map<?, Long>)CombatAccess.field(clientSession, "spellReadyNanos")).values()) {
                    if (l2 - l <= 0L) continue;
                    return false;
                }
                return state.timeline.canReserveSkillAction();
            }, () -> {
                if (nativeReuseActor.isSkillDelay()) {
                    return null;
                }
                final NativeSourceClientActionTimeline.Reservation reservation = state.timeline.tryReserveSkillAction(n);
                if (reservation == null) {
                    return null;
                }
                final NativeSourceSkillDelay.Lease lease = NativeSourceSkillDelay.tryReserveSkillUse(nativeReuseActor, lightSpell.reuseDelayMillis());
                if (lease == null) {
                    reservation.cancel();
                    return null;
                }
                return new NativeSourceSkillCastAdmission.Reservation(){

                    @Override
                    public void commit() {
                        if (!reservation.commitAtRuntime()) {
                            NativeReuseLog.getLogger(NativeCastTimeline.class).error("Skill action runtime rebase lost exact token skillId={} player={}", lightSpell.skillId(), clientSession.selectedCharacterName());
                        }
                        lease.commit();
                    }

                    @Override
                    public void cancel() {
                        reservation.cancel();
                        lease.cancel();
                    }
                };
            }, () -> NativeCastMaterial.commit(clientSession, lightSpell.skillId()));
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

    static boolean admitSelf(final ClientSession clientSession, final int n, int n2, int n3, Arrival arrival) {
        int n4;
        if (!clientSession.nativePlayerActionLock().isHeldByCurrentThread()) {
            throw new IllegalStateException("cast admission requires actual action lock");
        }
        State state = clientSession.nativeCastTimelineState();
        NativeDeItemApplication.Owner owner = NativeDeItemApplication.publicationOwner(clientSession);
        if (owner == null) {
            return false;
        }
        NativeReuseActor nativeReuseActor = NativeReuseGate.actor(clientSession);
        if (nativeReuseActor == null) {
            return false;
        }
        if (state.owner != owner) {
            state.timeline.clear();
            state.owner = owner;
        }
        if ((n4 = NativeCastTimeline.interval(clientSession, n2)) <= 0) {
            return false;
        }
        boolean[] blArray = new boolean[1];
        Runnable runnable = () -> {
            NativeSourceSkillCastAdmission.Result result = NativeSourceSkillCastAdmission.admit(() -> {
                if (nativeReuseActor.isSkillDelay()) {
                    return false;
                }
                long l = System.nanoTime();
                for (Long l2 : ((Map<?, Long>)CombatAccess.field(clientSession, "spellReadyNanos")).values()) {
                    if (l2 - l <= 0L) continue;
                    return false;
                }
                return state.timeline.canReserveSkillAction();
            }, () -> {
                if (nativeReuseActor.isSkillDelay()) {
                    return null;
                }
                final NativeSourceClientActionTimeline.Reservation reservation = state.timeline.tryReserveSkillAction(n4);
                if (reservation == null) {
                    return null;
                }
                final NativeSourceSkillDelay.Lease lease = NativeSourceSkillDelay.tryReserveSkillUse(nativeReuseActor, n3);
                if (lease == null) {
                    reservation.cancel();
                    return null;
                }
                return new NativeSourceSkillCastAdmission.Reservation(){

                    @Override
                    public void commit() {
                        if (!reservation.commitAtRuntime()) {
                            NativeReuseLog.getLogger(NativeCastTimeline.class).error("Skill action runtime rebase lost exact token skillId={} player={}", n, clientSession.selectedCharacterName());
                        }
                        lease.commit();
                    }

                    @Override
                    public void cancel() {
                        reservation.cancel();
                        lease.cancel();
                    }
                };
            }, () -> NativeCastMaterial.commit(clientSession, n));
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
