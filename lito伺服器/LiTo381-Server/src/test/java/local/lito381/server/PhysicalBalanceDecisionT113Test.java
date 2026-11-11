package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

/**
 * T113 (Issue #318): physical-formula decisions from docs/design/formulas-physical.md §7.
 *
 * <ul>
 *   <li>D12 (correctness): 380_db {@code bless} is 1-伺服器 internal (0=normal, 1=blessed).</li>
 *   <li>D9 (aligned): class attack level is added to every weapon after the multiplier.</li>
 *   <li>H7, D4, D11 (kept): deliberate LiTo rules, pinned here so a later change is explicit.</li>
 * </ul>
 */
final class PhysicalBalanceDecisionT113Test {

    // ---------------------------------------------------------------- D12

    @Test void d12SourceBlessIsSwappedFromInternalToNativeConvention() {
        // 1-伺服器 L1Item.java:246-264: internal 0=normal 1=blessed 2=cursed 3=illusion.
        assertEquals(RuntimeInventory.BLESSING_NORMAL,
                BattlePhysicalOffense.nativeBlessingFromSource(0));
        assertEquals(RuntimeInventory.BLESSING_BLESSED,
                BattlePhysicalOffense.nativeBlessingFromSource(1));
        assertEquals(RuntimeInventory.BLESSING_CURSED,
                BattlePhysicalOffense.nativeBlessingFromSource(2));
        assertEquals(3, BattlePhysicalOffense.nativeBlessingFromSource(3));
        assertThrows(IllegalArgumentException.class,
                () -> BattlePhysicalOffense.nativeBlessingFromSource(-1));
    }

    @Test void d12OnlyADatabaseBlessedWeaponRollsTheD4AgainstUndeadLikeCalcMaterialBlessDmg() {
        SourceNpcPhysicalStats undead = npc(1, 0, 0);
        SourceNpcPhysicalStats armorUndead = npc(2, 0, 0);
        SourceNpcPhysicalStats demon = npc(3, 0, 0);
        SourceNpcPhysicalStats ghost = npc(5, 0, 0);
        SourceNpcPhysicalStats living = npc(0, 0, 0);
        // 380_db.sql weapon.祝福狀態: 361 normal weapons store 0; 測試匕首 stores 1.
        SourceWeaponPhysicalStats dbNormal = weaponWithSourceBless(11, 0);
        SourceWeaponPhysicalStats dbBlessed = weaponWithSourceBless(11, 1);
        SourceWeaponPhysicalStats dbCursed = weaponWithSourceBless(11, 2);
        for (SourceNpcPhysicalStats target : new SourceNpcPhysicalStats[] {
                undead, armorUndead, demon, ghost, living}) {
            // 1-伺服器 L1AttackPc.java:1365/1373/1381: `_weaponBless == 1` and undead 1/2/3.
            int undeadType = target.undead();
            int expected = undeadType == 1 || undeadType == 2 || undeadType == 3 ? 4 : 0;
            assertEquals(expected, BattlePhysicalOffense.blessingMaximum(dbBlessed, target));
            assertEquals(0, BattlePhysicalOffense.blessingMaximum(dbNormal, target),
                    "an ordinary 380_db weapon (bless=0) must not roll the blessed d4");
            assertEquals(0, BattlePhysicalOffense.blessingMaximum(dbCursed, target));
        }
    }

    @Test void d12ProjectileBlessFromEtcitemUsesTheSameConversion() {
        SourceNpcPhysicalStats undead = npc(1, 0, 0);
        for (int sourceBless = 0; sourceBless <= 2; sourceBless++) {
            SourceProjectilePhysicalStats arrow = new SourceProjectilePhysicalStats(
                    0x1234L, 40743, -2, 5, 7, 0,
                    BattlePhysicalOffense.nativeBlessingFromSource(sourceBless),
                    66, true, 20);
            SourceWeaponPhysicalStats bow = new SourceWeaponPhysicalStats(
                    172, 4, 0, 0, 15, 0, 0, 0, 0).withProjectile(arrow);
            // 1-伺服器 L1AttackPc.java:134: `_weaponBless = _arrow.getItem().getBless()`.
            assertEquals(sourceBless == 1 ? 4 : 0,
                    BattlePhysicalOffense.blessingMaximum(bow, undead));
        }
    }

    // ---------------------------------------------------------------- D9

    @Test void d9ClassAttackLevelMatchesGetAttackLevelForEveryClass() {
        // 1-伺服器 L1*ClassFeature.getAttackLevel: 王族/10 騎士/6 妖精/12 法師/20 黑妖/16 龍騎/13 幻術/16.
        int[] divisors = {10, 6, 12, 20, 16, 13, 16};
        for (int type = 0; type < divisors.length; type++) {
            for (int level = 1; level <= 99; level++) {
                assertEquals(100 + level / divisors[type],
                        BattlePhysicalDamage.withClassAttackLevel(100, true, false, type, level),
                        "type=" + type + " level=" + level);
            }
        }
        // Issue #318 example: a level-52 knight gains 52/6 = 8 on every hit.
        assertEquals(28, BattlePhysicalDamage.withClassAttackLevel(20, true, false, 1, 52));
    }

    @Test void d9TermIsAddedAfterTheMultiplierLikePcDmgMode() {
        // L1AttackPc.java:860: pcDmgMode runs after weaponDamage2 multiplied the
        // subtotal.  Subtotal 10, multiplier x2 -> 20, then +8 -> 28 (not (10+8)*2 = 36).
        int multiplied = (int) (10 * 2.0D);
        assertEquals(28, BattlePhysicalDamage.withClassAttackLevel(multiplied, true, false, 1, 52));
    }

    @Test void d9SkipsMissesAndKiringkuWhichAlreadyAddsItsOwnTerm() {
        assertEquals(20, BattlePhysicalDamage.withClassAttackLevel(20, false, false, 1, 52));
        // CombatKiringku.finish already adds BattleKiringkuDamage.classAttackLevel.
        assertEquals(20, BattlePhysicalDamage.withClassAttackLevel(20, true, true, 1, 52));
        assertEquals(8, BattlePhysicalDamage.withClassAttackLevel(0, true, false, 1, 52));
        assertThrows(IllegalArgumentException.class,
                () -> BattlePhysicalDamage.withClassAttackLevel(-1, true, false, 1, 52));
        assertThrows(IllegalArgumentException.class,
                () -> BattlePhysicalDamage.withClassAttackLevel(20, true, false, 1, 0));
    }

    // ---------------------------------------------------------------- H7 (kept)

    @Test void h7DodgePointsStillSubtractDirectlyInsteadOfDividingByEight() {
        // Deliberate LiTo rule (3.81 semantics): dodge 5 = attacker die -5.
        // 1-伺服器 L1AttackMode.java:141-186 would use clamp(dodge/8, 1, 20).
        for (int dodge = 0; dodge <= 10; dodge++) {
            for (int d20 = dodge + 1; d20 <= 20; d20++) {
                assertEquals(
                        BattlePhysicalAttack.hitChanceAgainst(
                                false, 1, 0, 52, 1, 10, 0, 0, 0, 0, d20 - dodge, 0),
                        BattlePhysicalAttack.hitChanceAgainst(
                                false, 1, 0, 52, 1, 10, 0, 0, dodge, 0, d20, 0),
                        "dodge=" + dodge + " d20=" + d20);
            }
        }
        assertThrows(IllegalArgumentException.class,
                () -> BattlePhysicalAttack.hitChanceAgainst(
                        false, 1, 0, 52, 1, 10, 0, 0, 11, 0, 10, 0));
    }

    // ---------------------------------------------------------------- D4 (kept)

    @Test void d4BowDexterityDamageIsAddedWithoutThePvpOrPveMultiplier() {
        // Deliberate LiTo rule: no DEX x2.5 (PvP) / x1.5 (PvE) for bows
        // (1-伺服器 L1AttackPc.java:748-764 is not adopted; see ModernRangedCombat).
        SourceProjectilePhysicalStats arrow = new SourceProjectilePhysicalStats(
                0x1234L, 40743, -2, 5, 7, 0, 1, 66, true, 20);
        SourceWeaponPhysicalStats bow = new SourceWeaponPhysicalStats(
                172, 4, 0, 0, 15, 0, 0, 0, 0).withProjectile(arrow);
        for (int dexDamage = 0; dexDamage <= 20; dexDamage++) {
            assertEquals(3 + dexDamage, BattlePhysicalDamage.sourceDamage(
                    bow, true, false, 0, 3, 0, dexDamage, 0, 0));
        }
    }

    // ---------------------------------------------------------------- D11 (kept)

    @Test void d11NpcElementalResistanceStaysTheWeakAttributePlusMinusFifty() {
        // Deliberate LiTo rule: PvE earth/fire/water/wind uses the NPC weak
        // attribute (+-50) instead of the NPC's four element resistances.
        for (int kind : new int[] {1, 2, 4, 8}) {
            SourceWeaponPhysicalStats weapon = weaponWithAttribute(kind);
            assertEquals(-50, BattlePhysicalOffense.attributeResistance(weapon, npc(0, kind, 0)));
            assertEquals(50, BattlePhysicalOffense.attributeResistance(weapon, npc(0, -kind, 0)));
            assertEquals(0, BattlePhysicalOffense.attributeResistance(weapon, npc(0, 0, 100)));
        }
        // Light/dark/holy/evil keep using MR, the same as 1-伺服器.
        for (int kind : new int[] {16, 32, 64, 128}) {
            assertEquals(70, BattlePhysicalOffense.attributeResistance(
                    weaponWithAttribute(kind), npc(0, 0, 70)));
        }
    }

    // ---------------------------------------------------------------- helpers

    private static SourceNpcPhysicalStats npc(int undead, int weakAttribute, int magicResistance) {
        return new SourceNpcPhysicalStats(
                1, 10, false, 0, false, undead, weakAttribute, magicResistance);
    }

    private static SourceWeaponPhysicalStats weaponWithSourceBless(int material, int sourceBless) {
        return new SourceWeaponPhysicalStats(24, 1, 6, 8, 1, 0, 0, 0, 0)
                .withCombatTraits(new SourceWeaponCombatTraits(
                        material, BattlePhysicalOffense.nativeBlessingFromSource(sourceBless),
                        0, 0, 0));
    }

    private static SourceWeaponPhysicalStats weaponWithAttribute(int kind) {
        return new SourceWeaponPhysicalStats(24, 1, 6, 8, 1, 0, 0, 0, 0)
                .withCombatTraits(new SourceWeaponCombatTraits(11, 1, kind, 1, 10));
    }
}
