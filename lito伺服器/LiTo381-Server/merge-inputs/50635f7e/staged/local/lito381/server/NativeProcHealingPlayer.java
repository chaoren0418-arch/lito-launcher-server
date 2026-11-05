// DECOMPILED from candidate-50635f7e.jar (CFR 0.152), TODO review
/*
 * Decompiled with CFR 0.152.
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
        SourceEquipmentMagicStats sourceEquipmentMagicStats = (SourceEquipmentMagicStats)CombatAccess.call(clientSession, "requiredEquipmentMagicStats", "()Llocal/lito381/server/SourceEquipmentMagicStats;", new Object[0]);
        return sourceEquipmentMagicStats.effectiveIntelligence(CombatAccess.integerCall(clientSession, "baseIntelligence", "(J)I", l));
    }

    static boolean nativeSourceEquipmentProcOriginalApply(ClientSession clientSession, ClientSession clientSession2, int n2) {
        if (clientSession == null || clientSession2 == null || n2 <= 0) {
            return false;
        }
        NativeProcSkillCatalog.Catalog catalog = NativeProcSkillCatalog.catalog();
        if (!catalog.healing(n2)) {
            return false;
        }
        NativePlayerCommit.Binding binding = NativePlayerCommit.frameBinding(clientSession);
        NativePlayerCommit.Binding binding2 = NativePlayerCommit.frameBinding(clientSession2);
        if (binding == null || binding2 == null
                || !NativeSourceEquipmentProcLife.lockedFrame(clientSession, clientSession2)
                || !binding.matches(clientSession) || !binding2.matches(clientSession2)
                || NativePlayerCommit.integer(clientSession2, "playerHp") <= 0) {
            return true;
        }
        NativeProcSkillCatalog.Skill skill = catalog.find(n2);
        if (skill == null) {
            throw new IllegalStateException("missing built-in Proc heal skill=" + n2);
        }
        long l = System.nanoTime();
        int n3 = NativeProcHealBonus.catalog().bonus(NativeProcHealingPlayer.intelligence(clientSession, l));
        int n4 = NativeProcHealingKernel.calculate(skill.damageDice(), skill.damageValue(), n3, NativePlayerCommit.integer(clientSession, "playerLawful"), 10, n -> ThreadLocalRandom.current().nextInt(n));
        NativePlayerCommit.Queue queue = NativePlayerCommit.queue(clientSession2, binding2);
        if (queue == null) {
            return true;
        }
        BattleTimedStatus battleTimedStatus = CombatPoisonAtomic.status(clientSession2);
        BattleWaterLifeStatus battleWaterLifeStatus = (BattleWaterLifeStatus)CombatAccess.field(clientSession2, "waterLifeStatus");
        NativePlayerVitalCompletions.Receipt receipt = NativePlayerVitalCompletions.reserve(clientSession2);
        if (receipt == null) {
            return true;
        }
        try {
            queue.vitalLock.lock();
            try {
                synchronized (battleTimedStatus) {
                    if (!binding2.matches(clientSession2) || !NativeSourceEquipmentProcLife.lockedFrame(clientSession, clientSession2)) {
                        return true;
                    }
                    synchronized (battleWaterLifeStatus) {
                        if (queue.closed || !binding2.matchesFast(clientSession2) || NativePlayerCommit.integer(clientSession2, "playerHp") <= 0 || !NativeSourceEquipmentProcLife.lockedFrame(clientSession, clientSession2)) {
                            return true;
                        }
                        Target target = new Target(clientSession2, battleTimedStatus, battleWaterLifeStatus, l);
                        NativeProcHealingKernel.apply(target, () -> n4, (int)CombatRules.number("NATURES_BLESSING_HEAL", 0.0), () -> {
                            target.graphics = true;
                        });
                        NativeEffectCompletions.healing(receipt, clientSession, binding, clientSession2, binding2, n2, skill.castGraphics(), target);
                    }
                }
            }
            finally {
                queue.vitalLock.unlock();
            }
            return true;
        }
        finally {
            receipt.close();
            NativePlayerVitalCompletions.drain(clientSession2);
        }
    }

    private static void graphics(ClientSession clientSession, NativePlayerCommit.Binding binding, ClientSession clientSession2, NativePlayerCommit.Binding binding2, int n, int n2) {
        if (!(n2 > 0 && NativeSourceEquipmentOutput.graphicsFrame(clientSession, clientSession2) && binding.matches(clientSession) && binding2.matches(clientSession2) && NativePlayerCommit.integer(clientSession2, "playerHp") > 0 && NativeProcPlayerState.show(clientSession))) {
            return;
        }
        GameHub gameHub = (GameHub)CombatAccess.field(clientSession, "hub");
        boolean bl = binding.phase().equals("WORLD");
        List<ClientSession> list = bl ? gameHub.classicWorldMembers(binding.map(), binding.instance()) : NativeSourceEquipmentOutput.members(gameHub, binding.room());
        boolean bl2 = ((BattleInvisibilityStatus)CombatAccess.field(clientSession, "invisibilityStatus")).active();
        ArrayList<ClientSession> arrayList = new ArrayList<ClientSession>();
        arrayList.add(clientSession);
        if (!bl2) {
            for (ClientSession object : list) {
                if (!(object == clientSession || Math.max(Math.abs((long)NativePlayerCommit.integer(object, "positionX") - (long)NativePlayerCommit.integer(clientSession, "positionX")), Math.abs((long)NativePlayerCommit.integer(object, "positionY") - (long)NativePlayerCommit.integer(clientSession, "positionY"))) > 10L)) {
                arrayList.add(object);

                }
}
        }
        byte[] object = Protocol.effectObject(clientSession2.playerObjectId(), n2);
        for (ClientSession clientSession3 : arrayList) {
            NativePlayerCommit.Binding binding3 = NativePlayerCommit.Binding.capture(clientSession3);
            if (!NativeSourceEquipmentOutput.graphicsFrame(clientSession, clientSession2) || !binding.matches(clientSession) || !binding2.matches(clientSession2) || NativePlayerCommit.integer(clientSession2, "playerHp") <= 0) break;
            if (!(!binding3.matches(clientSession3))) {
            NativePlayerCommit.safeSend(clientSession3, object, "S_EFFECT equipment-healing skill=" + n + " target=" + clientSession2.playerObjectId());

            }
}
    }

    static void nativeSourceEquipmentOutputOriginalComplete(ClientSession clientSession, NativePlayerCommit.Binding binding, ClientSession clientSession2, NativePlayerCommit.Binding binding2, int n, int n2, Object object) {
        Target target = (Target)object;
        if (!binding2.matches(clientSession2) || !NativeSourceEquipmentOutput.completeFrame(clientSession, clientSession2)) {
            return;
        }
        if (target.after != target.before) {
            NativeSourceEquipmentOutput.hp(clientSession2, new Protocol.Writer().c(13).h(target.after).h(target.maximum).bytes(), "S_HIT_POINT equipment-healing skill=" + n);
            if (binding2.matches(clientSession2) && NativeSourceEquipmentOutput.completeFrame(clientSession, clientSession2)) {
                try {
                    CombatAccess.call(clientSession2, "broadcastOwnHitRatio", "(Ljava/lang/String;)V", "equipment-healing skill=" + n);
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
        return NativeSourceEquipmentProcLife.heal(clientSession, clientSession2, n);
    }

    static void completeHealing(ClientSession clientSession, NativePlayerCommit.Binding binding, ClientSession clientSession2, NativePlayerCommit.Binding binding2, int n, int n2, Object object) {
        NativeSourceEquipmentOutput.complete(clientSession, binding, clientSession2, binding2, n, n2, object);
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
            this.nativeSourceEquipmentOutputProof = NativeSourceEquipmentOutput.born(this);
        }

        @Override
        public boolean dead() {
            return NativePlayerCommit.integer(this.pc, "playerHp") <= 0;
        }

        @Override
        public NativeProcHealingKernel.Kind kind() {
            return NativeProcHealingKernel.Kind.PLAYER;
        }

        @Override
        public int undead() {
            return 0;
        }

        @Override
        public boolean hasEffect(int n) {
            return switch (n) {
                case 170 -> {
                    if (this.water.active(this.now) || NativeProcPlayerState.active(this.pc, n, this.now)) {
                        yield true;
                    }
                    yield false;
                }
                case 173 -> {
                    if (this.status.polluteWaterActive(this.now) || NativeProcPlayerState.active(this.pc, n, this.now)) {
                        yield true;
                    }
                    yield false;
                }
                case 4012 -> {
                    if (this.status.waterDragonDirtyCurrentActive(this.now) || NativeProcPlayerState.active(this.pc, n, this.now)) {
                        yield true;
                    }
                    yield false;
                }
                case 164 -> NativeProcPlayerState.active(this.pc, n, this.now);
                default -> throw new AssertionError((Object)("unexpected Proc healing modifier=" + n));
            };
        }

        @Override
        public void removeEffect(int n) {
            if (n != 170) {
                throw new AssertionError((Object)"unexpected Proc removal");
            }
            this.water.cancel();
            NativeProcPlayerState.remove(this.pc, n);
        }

        @Override
        public void commit(int n) {
            if (n >= 0) {
                throw new AssertionError((Object)"player Proc healing only commits negative HP delta");
            }
            this.after = (int)Math.min((long)this.maximum, (long)this.before - (long)n);
            CombatAccess.set(this.pc, "playerHp", this.after);
            NativeSourceEquipmentOutput.seal(this);
        }
    }
}
