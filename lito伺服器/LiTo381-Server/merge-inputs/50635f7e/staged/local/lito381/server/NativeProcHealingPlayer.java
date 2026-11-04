// DECOMPILED (CFR 0.152) from reference/base-0.6.71.jar — TODO: replace with original source (T002)
/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  local.lito381.server.BattleInvisibilityStatus
 *  local.lito381.server.BattleTimedStatus
 *  local.lito381.server.BattleWaterLifeStatus
 *  local.lito381.server.ClientSession
 *  local.lito381.server.CombatAccess
 *  local.lito381.server.CombatPoisonAtomic
 *  local.lito381.server.CombatRules
 *  local.lito381.server.GameHub
 *  local.lito381.server.NativeEffectCompletions
 *  local.lito381.server.NativeProcHealBonus
 *  local.lito381.server.NativeProcHealingKernel
 *  local.lito381.server.NativeProcHealingKernel$Kind
 *  local.lito381.server.NativeProcHealingKernel$Target
 *  local.lito381.server.NativeProcPlayerState
 *  local.lito381.server.NativeSourceEquipmentOutput
 *  local.lito381.server.NativeSourceEquipmentProcLife
 *  local.lito381.server.Protocol
 *  local.lito381.server.Protocol$Writer
 *  local.lito381.server.SourceEquipmentMagicStats
 */
package local.lito381.server;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import local.lito381.server.BattleInvisibilityStatus;
import local.lito381.server.BattleTimedStatus;
import local.lito381.server.BattleWaterLifeStatus;
import local.lito381.server.ClientSession;
import local.lito381.server.CombatAccess;
import local.lito381.server.CombatPoisonAtomic;
import local.lito381.server.CombatRules;
import local.lito381.server.GameHub;
import local.lito381.server.NativeEffectCompletions;
import local.lito381.server.NativePlayerCommit;
import local.lito381.server.NativePlayerVitalCompletions;
import local.lito381.server.NativeProcHealBonus;
import local.lito381.server.NativeProcHealingKernel;
import local.lito381.server.NativeProcPlayerState;
import local.lito381.server.NativeProcSkillCatalog;
import local.lito381.server.NativeSourceEquipmentOutput;
import local.lito381.server.NativeSourceEquipmentProcLife;
import local.lito381.server.Protocol;
import local.lito381.server.SourceEquipmentMagicStats;

final class NativeProcHealingPlayer {
    NativeProcHealingPlayer() {
    }

    static int intelligence(ClientSession clientSession, long l) {
        SourceEquipmentMagicStats sourceEquipmentMagicStats = (SourceEquipmentMagicStats)CombatAccess.call((Object)clientSession, (String)"requiredEquipmentMagicStats", (String)"()Llocal/lito381/server/SourceEquipmentMagicStats;", (Object[])new Object[0]);
        return sourceEquipmentMagicStats.effectiveIntelligence(CombatAccess.integerCall((Object)clientSession, (String)"baseIntelligence", (String)"(J)I", (Object[])new Object[]{l}));
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     * Enabled aggressive block sorting
     * Enabled unnecessary exception pruning
     * Enabled aggressive exception aggregation
     */
    static boolean nativeSourceEquipmentProcOriginalApply(ClientSession clientSession, ClientSession clientSession2, int n2) {
        if (clientSession == null) return false;
        if (clientSession2 == null) return false;
        if (n2 <= 0) {
            return false;
        }
        NativeProcSkillCatalog.Catalog catalog = NativeProcSkillCatalog.catalog();
        if (!catalog.healing(n2)) {
            return false;
        }
        NativePlayerCommit.Binding binding = NativePlayerCommit.frameBinding(clientSession);
        NativePlayerCommit.Binding binding2 = NativePlayerCommit.frameBinding(clientSession2);
        if (binding == null) return true;
        if (binding2 == null) return true;
        if (!NativeSourceEquipmentProcLife.lockedFrame((ClientSession)clientSession, (ClientSession)clientSession2)) return true;
        if (!binding.matches(clientSession)) return true;
        if (!binding2.matches(clientSession2)) return true;
        if (NativePlayerCommit.integer(clientSession2, "playerHp") <= 0) {
            return true;
        }
        NativeProcSkillCatalog.Skill skill = catalog.find(n2);
        if (skill == null) {
            throw new IllegalStateException("missing built-in Proc heal skill=" + n2);
        }
        long l = System.nanoTime();
        int n3 = NativeProcHealBonus.catalog().bonus(NativeProcHealingPlayer.intelligence(clientSession, l));
        int n4 = NativeProcHealingKernel.calculate((int)skill.damageDice(), (int)skill.damageValue(), (int)n3, (int)NativePlayerCommit.integer(clientSession, "playerLawful"), (int)10, n -> ThreadLocalRandom.current().nextInt(n));
        NativePlayerCommit.Queue queue = NativePlayerCommit.queue(clientSession2, binding2);
        if (queue == null) {
            return true;
        }
        BattleTimedStatus battleTimedStatus = CombatPoisonAtomic.status((ClientSession)clientSession2);
        BattleWaterLifeStatus battleWaterLifeStatus = (BattleWaterLifeStatus)CombatAccess.field((Object)clientSession2, (String)"waterLifeStatus");
        NativePlayerVitalCompletions.Receipt receipt = NativePlayerVitalCompletions.reserve(clientSession2);
        if (receipt == null) {
            return true;
        }
        try {
            BattleWaterLifeStatus battleWaterLifeStatus2;
            queue.vitalLock.lock();
            BattleTimedStatus battleTimedStatus2 = battleTimedStatus;
            synchronized (battleTimedStatus2) {
                if (!binding2.matches(clientSession2) || !NativeSourceEquipmentProcLife.lockedFrame((ClientSession)clientSession, (ClientSession)clientSession2)) {
                    boolean bl = true;
                    // MONITOREXIT @DISABLED, blocks:[0, 17, 1, 10] lbl38 : MonitorExitStatement: MONITOREXIT : var16_14
                    queue.vitalLock.unlock();
                    return bl;
                }
                battleWaterLifeStatus2 = battleWaterLifeStatus;
            }
            {
                synchronized (battleWaterLifeStatus2) {
                    if (queue.closed || !binding2.matchesFast(clientSession2) || NativePlayerCommit.integer(clientSession2, "playerHp") <= 0 || !NativeSourceEquipmentProcLife.lockedFrame((ClientSession)clientSession, (ClientSession)clientSession2)) {
                        boolean bl = true;
                        // MONITOREXIT @DISABLED, blocks:[16, 0, 10, 11] lbl45 : MonitorExitStatement: MONITOREXIT : var17_17
                        // MONITOREXIT @DISABLED, blocks:[16, 0, 10] lbl46 : MonitorExitStatement: MONITOREXIT : var16_14
                        queue.vitalLock.unlock();
                        return bl;
                    }
                    try {
                        Target target = new Target(clientSession2, battleTimedStatus, battleWaterLifeStatus, l);
                        NativeProcHealingKernel.apply((NativeProcHealingKernel.Target)target, () -> n4, (int)((int)CombatRules.number((String)"NATURES_BLESSING_HEAL", (double)0.0)), () -> {
                            target.graphics = true;
                        });
                        NativeEffectCompletions.healing((NativePlayerVitalCompletions.Receipt)receipt, (ClientSession)clientSession, (NativePlayerCommit.Binding)binding, (ClientSession)clientSession2, (NativePlayerCommit.Binding)binding2, (int)n2, (int)skill.castGraphics(), (Object)target);
                    }
                    finally {
                        queue.vitalLock.unlock();
                    }
                }
            }
            boolean bl = true;
            return bl;
        }
        finally {
            receipt.close();
            NativePlayerVitalCompletions.drain(clientSession2);
        }
    }

    private static void graphics(ClientSession clientSession, NativePlayerCommit.Binding binding, ClientSession clientSession2, NativePlayerCommit.Binding binding2, int n, int n2) {
        if (!(n2 > 0 && NativeSourceEquipmentOutput.graphicsFrame((ClientSession)clientSession, (ClientSession)clientSession2) && binding.matches(clientSession) && binding2.matches(clientSession2) && NativePlayerCommit.integer(clientSession2, "playerHp") > 0 && NativeProcPlayerState.show((ClientSession)clientSession))) {
            return;
        }
        GameHub gameHub = (GameHub)CombatAccess.field((Object)clientSession, (String)"hub");
        boolean bl = binding.phase().equals("WORLD");
        List<ClientSession> list = bl ? gameHub.classicWorldMembers(binding.map(), binding.instance()) : NativeSourceEquipmentOutput.members((GameHub)gameHub, (long)binding.room());
        boolean bl2 = ((BattleInvisibilityStatus)CombatAccess.field((Object)clientSession, (String)"invisibilityStatus")).active();
        ArrayList<ClientSession> arrayList = new ArrayList<ClientSession>();
        arrayList.add(clientSession);
        if (!bl2) {
            for (ClientSession clientSession3 : list) {
                if (clientSession3 == clientSession || Math.max(Math.abs((long)NativePlayerCommit.integer(clientSession3, "positionX") - (long)NativePlayerCommit.integer(clientSession, "positionX")), Math.abs((long)NativePlayerCommit.integer(clientSession3, "positionY") - (long)NativePlayerCommit.integer(clientSession, "positionY"))) > 10L) continue;
                arrayList.add(clientSession3);
            }
        }
        Object object = Protocol.effectObject((long)clientSession2.playerObjectId(), (int)n2);
        for (ClientSession clientSession4 : arrayList) {
            NativePlayerCommit.Binding binding3 = NativePlayerCommit.Binding.capture(clientSession4);
            if (!NativeSourceEquipmentOutput.graphicsFrame((ClientSession)clientSession, (ClientSession)clientSession2) || !binding.matches(clientSession) || !binding2.matches(clientSession2) || NativePlayerCommit.integer(clientSession2, "playerHp") <= 0) break;
            if (!binding3.matches(clientSession4)) continue;
            NativePlayerCommit.safeSend(clientSession4, (byte[])object, "S_EFFECT equipment-healing skill=" + n + " target=" + clientSession2.playerObjectId());
        }
    }

    static void nativeSourceEquipmentOutputOriginalComplete(ClientSession clientSession, NativePlayerCommit.Binding binding, ClientSession clientSession2, NativePlayerCommit.Binding binding2, int n, int n2, Object object) {
        Target target = (Target)object;
        if (!binding2.matches(clientSession2) || !NativeSourceEquipmentOutput.completeFrame((ClientSession)clientSession, (ClientSession)clientSession2)) {
            return;
        }
        if (target.after != target.before) {
            NativeSourceEquipmentOutput.hp((ClientSession)clientSession2, (byte[])new Protocol.Writer().c(13).h(target.after).h(target.maximum).bytes(), (String)("S_HIT_POINT equipment-healing skill=" + n));
            if (binding2.matches(clientSession2) && NativeSourceEquipmentOutput.completeFrame((ClientSession)clientSession, (ClientSession)clientSession2)) {
                try {
                    CombatAccess.call((Object)clientSession2, (String)"broadcastOwnHitRatio", (String)"(Ljava/lang/String;)V", (Object[])new Object[]{"equipment-healing skill=" + n});
                }
                catch (RuntimeException runtimeException) {
                    NativePlayerCommit.secondaryFailure("Proc heal ratio", runtimeException);
                }
            }
        }
        if (target.graphics) {
            NativeProcHealingPlayer.graphics(clientSession, binding, clientSession2, binding2, n, n2);
        }
    }

    static boolean apply(ClientSession clientSession, ClientSession clientSession2, int n) {
        return NativeSourceEquipmentProcLife.heal((ClientSession)clientSession, (ClientSession)clientSession2, (int)n);
    }

    static void completeHealing(ClientSession clientSession, NativePlayerCommit.Binding binding, ClientSession clientSession2, NativePlayerCommit.Binding binding2, int n, int n2, Object object) {
        NativeSourceEquipmentOutput.complete((ClientSession)clientSession, (NativePlayerCommit.Binding)binding, (ClientSession)clientSession2, (NativePlayerCommit.Binding)binding2, (int)n, (int)n2, (Object)object);
    }

    private static final class Target
    implements NativeProcHealingKernel.Target {
        final ClientSession pc;
        final BattleTimedStatus status;
        final BattleWaterLifeStatus water;
        final long now;
        final int before;
        final int maximum;
        int after;
        boolean graphics;
        volatile Object nativeSourceEquipmentOutputProof;

        Target(ClientSession clientSession, BattleTimedStatus battleTimedStatus, BattleWaterLifeStatus battleWaterLifeStatus, long l) {
            this.pc = clientSession;
            this.status = battleTimedStatus;
            this.water = battleWaterLifeStatus;
            this.now = l;
            this.before = this.after = NativePlayerCommit.integer(clientSession, "playerHp");
            this.maximum = NativePlayerCommit.integer(clientSession, "playerMaxHp");
            this.nativeSourceEquipmentOutputProof = NativeSourceEquipmentOutput.born((Object)this);
        }

        public boolean dead() {
            return NativePlayerCommit.integer(this.pc, "playerHp") <= 0;
        }

        public NativeProcHealingKernel.Kind kind() {
            return NativeProcHealingKernel.Kind.PLAYER;
        }

        public int undead() {
            return 0;
        }

        public boolean hasEffect(int n) {
            return switch (n) {
                case 170 -> {
                    if (this.water.active(this.now) || NativeProcPlayerState.active((ClientSession)this.pc, (int)n, (long)this.now)) {
                        yield true;
                    }
                    yield false;
                }
                case 173 -> {
                    if (this.status.polluteWaterActive(this.now) || NativeProcPlayerState.active((ClientSession)this.pc, (int)n, (long)this.now)) {
                        yield true;
                    }
                    yield false;
                }
                case 4012 -> {
                    if (this.status.waterDragonDirtyCurrentActive(this.now) || NativeProcPlayerState.active((ClientSession)this.pc, (int)n, (long)this.now)) {
                        yield true;
                    }
                    yield false;
                }
                case 164 -> NativeProcPlayerState.active((ClientSession)this.pc, (int)n, (long)this.now);
                default -> throw new AssertionError((Object)("unexpected Proc healing modifier=" + n));
            };
        }

        public void removeEffect(int n) {
            if (n != 170) {
                throw new AssertionError((Object)"unexpected Proc removal");
            }
            this.water.cancel();
            NativeProcPlayerState.remove((ClientSession)this.pc, (int)n);
        }

        public void commit(int n) {
            if (n >= 0) {
                throw new AssertionError((Object)"player Proc healing only commits negative HP delta");
            }
            this.after = (int)Math.min((long)this.maximum, (long)this.before - (long)n);
            CombatAccess.set((Object)this.pc, (String)"playerHp", (Object)this.after);
            NativeSourceEquipmentOutput.seal((Object)this);
        }
    }
}

