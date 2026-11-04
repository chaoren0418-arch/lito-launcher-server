// DECOMPILED (CFR 0.152) from reference/base-0.6.71.jar — TODO: replace with original source (T002)
/*
 * Decompiled with CFR 0.152.
 */
package local.lito381.server;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicLong;
import local.lito381.server.BattleAbilityStatus;
import local.lito381.server.BattleSpellCatalog;
import local.lito381.server.ClientSession;
import local.lito381.server.CombatAccess;
import local.lito381.server.CombatWorldPhysical;
import local.lito381.server.NativeArmorProc;
import local.lito381.server.NativeCombatAttacker;
import local.lito381.server.NativePlayerCommit;
import local.lito381.server.NativeProcHealingCompanion;
import local.lito381.server.NativeProcHealingEncounter;
import local.lito381.server.NativeProcHealingPlayer;
import local.lito381.server.NativeProcHealingWorld;
import local.lito381.server.Protocol;
import local.lito381.server.RuntimeInventory;

final class NativeEquipmentDefense {
    private static final AtomicLong probabilityDraws = new AtomicLong();
    private static final AtomicLong amountDraws = new AtomicLong();
    private static final AtomicLong prepared = new AtomicLong();
    private static final AtomicLong applied = new AtomicLong();

    NativeEquipmentDefense() {
    }

    static List<NativeArmorProc.Rule> prepare(ClientSession clientSession, boolean bl) {
        if (bl) {
            return List.of();
        }
        ArrayList<NativeArmorProc.Rule> arrayList = new ArrayList<NativeArmorProc.Rule>();
        RuntimeInventory runtimeInventory = (RuntimeInventory)CombatAccess.field(clientSession, "inventory");
        NativeArmorProc.Catalog catalog = NativeArmorProc.catalog();
        for (RuntimeInventory.Item item : runtimeInventory.items()) {
            if (!item.equipped() || !item.definition().sourceArmor()) continue;
            for (NativeArmorProc.Rule rule : catalog.selected(Math.toIntExact(item.definition().catalogId()), item.enchantLevel())) {
                int n = Math.min(100, Math.max(0, rule.hurt()));
                if (n == 0) continue;
                probabilityDraws.incrementAndGet();
                if (ThreadLocalRandom.current().nextInt(100) >= n) continue;
                arrayList.add(rule);
                prepared.incrementAndGet();
            }
        }
        return List.copyOf(arrayList);
    }

    static int amount(int n, int n2) {
        long l = Math.max(0, n);
        if (n2 > 0) {
            amountDraws.incrementAndGet();
            l += ThreadLocalRandom.current().nextLong((long)n2 + 1L);
        }
        return (int)Math.min(Integer.MAX_VALUE, l);
    }

    static int add(int n, int n2) {
        return (int)Math.min(Integer.MAX_VALUE, (long)Math.max(0, n) + (long)Math.max(0, n2));
    }

    static Resolved resolve(ClientSession clientSession, ClientSession clientSession2, List<NativeArmorProc.Rule> list) {
        return NativeEquipmentDefense.resolve(clientSession, clientSession2, list, null);
    }

    static Resolved resolve(ClientSession clientSession, ClientSession clientSession2, List<NativeArmorProc.Rule> list, NativeCombatAttacker.Ref ref) {
        int n = 0;
        int n2 = 0;
        int n3 = 0;
        ArrayList<Effect> arrayList = new ArrayList<Effect>();
        for (NativeArmorProc.Rule rule : list) {
            int n4;
            if (rule.needsTarget()) {
                if (ref == null) {
                    if (!NativeEquipmentDefense.alive(clientSession2)) continue;
                } else if (!NativeCombatAttacker.alive(clientSession, ref)) continue;
            }
            int n5 = NativeEquipmentDefense.amount(rule.damage(), rule.randomDamage());
            int n6 = NativeEquipmentDefense.amount(rule.hp(), rule.randomHp());
            int n7 = NativeEquipmentDefense.amount(rule.mp(), rule.randomMp());
            n = NativeEquipmentDefense.add(n, n5);
            n2 = NativeEquipmentDefense.add(n2, n6);
            n3 = NativeEquipmentDefense.add(n3, rule.selfHeal() ? n7 : 0);
            int n8 = n4 = rule.selfHeal() ? 0 : n7;
            if (n4 <= 0 && rule.gfx() <= 0 && rule.skill() <= 0) continue;
            arrayList.add(new Effect(rule, n4));
        }
        return new Resolved(n, n2, n3, arrayList);
    }

    static boolean alive(ClientSession clientSession) {
        return clientSession != null && NativePlayerCommit.integer(clientSession, "playerHp") > 0;
    }

    static void apply(ClientSession clientSession, ClientSession clientSession2, NativePlayerCommit.Binding binding, NativePlayerCommit.Binding binding2, Resolved resolved, NativeCombatAttacker.Ref ref) {
        if (!NativePlayerCommit.validFrame(clientSession)) {
            return;
        }
        applied.incrementAndGet();
        if (NativeEquipmentDefense.alive(clientSession)) {
            for (Effect effect : resolved.effects) {
                if (!NativePlayerCommit.validFrame(clientSession) || !NativeEquipmentDefense.alive(clientSession)) break;
                NativeArmorProc.Rule rule = effect.rule;
                try {
                    boolean bl = ref != null
                            ? NativeCombatAttacker.alive(clientSession, ref)
                            : clientSession2 != null && binding2 != null && binding2.matches(clientSession2) && NativeEquipmentDefense.alive(clientSession2);
                    boolean bl2 = bl;
                    if (rule.needsTarget() && !bl) continue;
                    if (effect.mpDrain > 0 && bl && (ref != null || clientSession != clientSession2)) {
                        int n;
                        int n2 = n = ref == null ? NativePlayerCommit.debitMp(clientSession2, binding2, effect.mpDrain) : NativeCombatAttacker.debit(clientSession, ref, effect.mpDrain);
                        if (n > 0) {
                            NativePlayerCommit.recover(clientSession, binding, 0, n, "equipment-drain");
                        }
                    }
                    ClientSession clientSession3 = rule.selfSkill() ? clientSession : (bl && ref == null ? clientSession2 : null);
                    ClientSession clientSession4 = clientSession3;
                    if (!rule.selfSkill() && ref != null && bl) {
                        if (rule.gfx() > 0) {
                            if (rule.gfxMode() == 0 || rule.gfxMode() == 2) {
                                NativeCombatAttacker.effect(clientSession, ref, rule.gfx(), "S_EFFECT equipment-hurt row=" + rule.id());
                            }
                            if (rule.gfxMode() == 1 || rule.gfxMode() == 2) {
                                NativePlayerCommit.safeAuxSend(clientSession, Protocol.effectObject(clientSession.playerObjectId(), rule.gfx()), "S_EFFECT equipment actor");
                            }
                        }
                        if (!(rule.skill() <= 0 || NativeProcHealingEncounter.apply(clientSession, ref, rule) || NativeProcHealingCompanion.apply(clientSession, ref, rule) || NativeProcHealingWorld.apply(clientSession, ref, rule))) {
                            throw new IllegalArgumentException("unported non-player skill target=" + rule.skill());
                        }
                    }
                    if (rule.gfx() > 0 && NativeEquipmentDefense.alive(clientSession3)) {
                        if (rule.gfxMode() == 0 || rule.gfxMode() == 2) {
                            NativePlayerCommit.safeAuxSend(clientSession3, Protocol.effectObject(clientSession3.playerObjectId(), rule.gfx()), "S_EFFECT equipment-hurt row=" + rule.id() + " target");
                        }
                        if (rule.gfxMode() == 1 || rule.gfxMode() == 2) {
                            NativePlayerCommit.safeAuxSend(clientSession, Protocol.effectObject(clientSession.playerObjectId(), rule.gfx()), "S_EFFECT equipment-hurt row=" + rule.id() + " actor");
                        }
                    }
                    if (rule.skill() <= 0 || !NativeEquipmentDefense.alive(clientSession3)) continue;
                    NativeEquipmentDefense.skill(clientSession, clientSession3, rule);
                }
                catch (RuntimeException runtimeException) {
                    NativePlayerCommit.secondaryFailure("equipment-hurt action", runtimeException);
                }
            }
            if (NativePlayerCommit.validFrame(clientSession) && NativeEquipmentDefense.alive(clientSession)) {
                NativePlayerCommit.recover(clientSession, binding, resolved.hp, resolved.mp, "equipment-hurt recovery");
            }
        }
        if (NativePlayerCommit.validFrame(clientSession) && resolved.reflect > 0 && ref != null) {
            try {
                NativeCombatAttacker.damage(clientSession, ref, resolved.reflect, "equipment-hurt reflect");
            }
            catch (RuntimeException runtimeException) {
                NativePlayerCommit.secondaryFailure("equipment NPC reflect", runtimeException);
            }
        }
        if (ref == null && NativePlayerCommit.validFrame(clientSession) && resolved.reflect > 0 && clientSession2 != null && binding2 != null && binding2.matches(clientSession2) && NativeEquipmentDefense.alive(clientSession2)) {
            try {
                if (CombatWorldPhysical.world(clientSession)) {
                    NativePlayerCommit.worldPlayer(clientSession2, resolved.reflect, clientSession, false, true);
                } else {
                    CombatAccess.call(clientSession2, "takePlayerDamage", "(ILlocal/lito381/server/ClientSession;ZZJ)V", resolved.reflect, clientSession, false, true, 0L);
                }
            }
            catch (RuntimeException runtimeException) {
                NativePlayerCommit.secondaryFailure("equipment-hurt reflect", runtimeException);
            }
        }
    }

    static void skill(ClientSession clientSession, ClientSession clientSession2, NativeArmorProc.Rule rule) {
        if (NativeProcHealingPlayer.apply(clientSession, clientSession2, rule.skill())) {
            return;
        }
        if (rule.skill() != 68) {
            throw new IllegalArgumentException("unported equipment-hurt skill helper=" + rule.skill());
        }
        long l = System.nanoTime();
        BattleAbilityStatus battleAbilityStatus = (BattleAbilityStatus)CombatAccess.field(clientSession2, "abilityStatus");
        BattleSpellCatalog.DamageProtectionSpell damageProtectionSpell = BattleSpellCatalog.damageProtectionSpell(68);
        boolean bl = NativePlayerCommit.mutateOwned(clientSession2, NativePlayerCommit.frameBinding(clientSession2), () -> {
            BattleAbilityStatus battleAbilityStatus2 = battleAbilityStatus;
            synchronized (battleAbilityStatus2) {
                if (rule.selfSkill() && battleAbilityStatus.immuneToHarm(l).remainingSeconds() > 0) {
                    return false;
                }
                int n = rule.seconds() > 0 ? rule.seconds() : damageProtectionSpell.durationSeconds();
                battleAbilityStatus.activateImmuneToHarm(l, n, damageProtectionSpell.damageDivisor());
                return true;
            }
        });
        if (!bl) {
            return;
        }
        NativePlayerCommit.safeAuxSend(rule.seconds() > 0 ? clientSession : clientSession2, Protocol.effectObject(clientSession2.playerObjectId(), damageProtectionSpell.effectGfx()), "S_EFFECT equipment-hurt skill=68 seconds=" + (rule.seconds() > 0 ? rule.seconds() : damageProtectionSpell.durationSeconds()));
    }

    static long probabilityDraws() {
        return probabilityDraws.get();
    }

    static long amountDraws() {
        return amountDraws.get();
    }

    static long prepared() {
        return prepared.get();
    }

    static long applied() {
        return applied.get();
    }

    record Resolved(int reflect, int hp, int mp, List<Effect> effects) {Resolved {
            effects = List.copyOf(effects);
        }

        static Resolved empty() {
            return new Resolved(0, 0, 0, List.of());
        }
    }

    record Effect(NativeArmorProc.Rule rule, int mpDrain) {
    }
}
