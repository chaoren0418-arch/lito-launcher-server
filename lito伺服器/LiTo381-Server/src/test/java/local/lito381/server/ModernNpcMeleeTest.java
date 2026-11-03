package local.lito381.server;

import static org.junit.jupiter.api.Assertions.*;
import static local.lito381.server.ModernEquipmentEvaluatorTest.*;
import static local.lito381.server.ModernEquipmentFacts.*;
import static local.lito381.server.ModernPhysicalCombatTest.*;
import static local.lito381.server.ModernNpcCatalog.Field.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class ModernNpcMeleeTest {
    static final int NPC = 45262;
    static final ModernNpcMelee.BlessingBinding INTERNAL = new ModernNpcMelee.BlessingBinding(7, "catalog", ModernNpcMelee.BlessingConvention.INTERNAL_ONE_BLESSED);
    static ModernNpcCatalog.Catalog catalog(Map<ModernNpcCatalog.Field, Long> changes, boolean large, long growthDamage) {
        var numbers = ModernNpcCatalogJdbcTest.numbers(); numbers.put(AC, 10L); numbers.putAll(changes);
        var npc = new ModernNpcCatalog.Npc(new ModernNpcCatalog.Key(88, "npc_static", NPC), "fixture-npc", "$1498", "L1Monster", large,
                new ModernNpcCatalog.Facts(numbers), false);
        var rows = new ArrayList<ModernPhysicalCombat.GrowthRow>();
        for (int i = 1; i <= 127; i++) rows.add(new ModernPhysicalCombat.GrowthRow(i, 0, growthDamage));
        return new ModernNpcCatalog.Catalog(88, "npc_static", Map.of(NPC, npc), new ModernPhysicalCombat.Growth(88, "npc_static", rows, rows));
    }
    static ModernNpcCatalog.Catalog catalog() { return catalog(Map.of(), true, 0); }
    static ModernNpcRoster.Actor actor(ModernNpcCatalog.Catalog c) {
        return new ModernNpcRoster.Actor(new ModernNpcRoster.Id(ModernNpcRosterTest.SCOPE, 1), c.npcs().get(NPC).rosterTemplate(),
                0, 1, ModernNpcRosterTest.cell(10, 10), 2, c.npcs().get(NPC).get(HP), c.npcs().get(NPC).get(MP),
                1, ModernNpcRoster.Action.IDLE, 2, 2, 0, 100, 0, null);
    }
    static ModernPhysicalCombat.Source armed() { return source(7, 0, 30, List.of(weapon(1, 1, true, 0, Ability.zero(), TemplatePhysical.zero()))); }
    static ModernNpcMelee.Player player(ModernPhysicalCombat.Source source) { return ModernNpcMelee.player(fighter(source), INTERNAL); }
    static final class Rolls implements ModernPhysicalCombat.BoundedRandom {
        final long[] values; final List<Long> bounds = new ArrayList<>(); int index;
        Rolls(long... values) { this.values = values; }
        public long nextLong(long bound) { assertTrue(index < values.length, "unexpected extra random draw"); bounds.add(bound); return values[index++]; }
        void consumed(long... expected) { assertEquals(values.length, index); assertEquals(java.util.Arrays.stream(expected).boxed().toList(), bounds); }
    }
    static void failure(ModernNpcMelee.Failure reason, org.junit.jupiter.api.function.Executable action) {
        assertEquals(reason, assertThrows(ModernNpcMelee.CombatException.class, action).failure());
    }

    @Test void blessingBindingUsesTheQualifiedPlayerCatalogNotNpcRealmPacketOrCharacterInstance() {
        var source = weaponFacts(armed(), new ModernCombatProfile.WeaponFacts(1, 1, 1, 8, 12, 1, 11, 0, 0, 0, 0, 0, 0));
        var fighter = fighter(source); assertEquals("catalog", fighter.catalogDatabase());
        assertEquals(1, fighter.profile().source().inventory().get(0).blessing(), "instance differs deliberately from template");
        var c = catalog(Map.of(UNDEAD, 1L), true, 0);
        var legacy = ModernNpcMelee.player(fighter, new ModernNpcMelee.BlessingBinding(7, "catalog", ModernNpcMelee.BlessingConvention.LEGACY_ZERO_BLESSED));
        assertEquals(1, ModernNpcMelee.againstNpc(legacy, c, actor(c), 0, b -> 0).blessingBonus());
        assertEquals(0, ModernNpcMelee.againstNpc(ModernNpcMelee.player(fighter, INTERNAL), c, actor(c), 0, b -> 0).blessingBonus());
        failure(ModernNpcMelee.Failure.INVALID_STATE, () -> ModernNpcMelee.player(fighter, new ModernNpcMelee.BlessingBinding(42, "catalog", INTERNAL.convention())));
        failure(ModernNpcMelee.Failure.INVALID_STATE, () -> ModernNpcMelee.player(fighter, new ModernNpcMelee.BlessingBinding(7, "other", INTERNAL.convention())));
        assertThrows(NullPointerException.class, () -> ModernNpcMelee.player(fighter, null));
        assertThrows(NullPointerException.class, () -> new ModernNpcMelee.BlessingBinding(7, "catalog", null));
    }

    @Test void playerToNpcHasFixedAcHitAndDistinctLargeSmallAndFallbackDamageDice() {
        var source = source(7, 0, 30, List.of(weapon(1, 1, true, 0, bonus(Stat.MELEE_DAMAGE, 100), TemplatePhysical.zero())));
        var p = player(source); var c = catalog(Map.of(AC, -100L), true, 0); var rolls = new Rolls(19, 0, 11, 0);
        var large = ModernNpcMelee.againstNpc(p, c, actor(c), 0, rolls);
        assertEquals(115, large.rawDamage()); assertEquals(100, large.damage()); rolls.consumed(20, 100, 12, 15);
        var smallCatalog = catalog(Map.of(AC, -100L), false, 0); var smallRolls = new Rolls(19, 0, 7, 0);
        assertEquals(96, ModernNpcMelee.againstNpc(p, smallCatalog, actor(smallCatalog), 0, smallRolls).damage()); smallRolls.consumed(20, 100, 8, 15);
        var fallback = player(weaponFacts(source, new ModernCombatProfile.WeaponFacts(1, 1, 1, 8, 0, 1, 11, 1, 0, 0, 0, 0, 0)));
        assertEquals(96, ModernNpcMelee.againstNpc(fallback, c, actor(c), 0, new Rolls(19, 0, 7, 0)).damage());
        var missRolls = new Rolls(0, 99); var miss = ModernNpcMelee.againstNpc(p, c, actor(c), 0, missRolls);
        assertFalse(miss.hit()); assertEquals(280, miss.remainingHp()); missRolls.consumed(20, 100);
    }

    @Test void fixedDefenseEqualityUsesFifteenPercentAndWeightPenaltyRemainsExplicit() {
        var c = catalog(Map.of(AC, -14L), true, 0); var p = player(armed());
        assertFalse(ModernNpcMelee.againstNpc(p, c, actor(c), 0, new Rolls(0, 15)).hit());
        var r = new Rolls(0, 14, 0, 0); assertTrue(ModernNpcMelee.againstNpc(p, c, actor(c), 0, r).hit()); r.consumed(20, 100, 12, 3);
        var nearer = catalog(Map.of(AC, -13L), true, 0);
        assertTrue(ModernNpcMelee.againstNpc(p, nearer, actor(nearer), 0, new Rolls(0, 99, 0, 0)).hit());
        assertFalse(ModernNpcMelee.againstNpc(p, nearer, actor(nearer), 1, new Rolls(0, 99)).hit());
        for (int value : new int[]{-1, 2, 4, 6}) failure(ModernNpcMelee.Failure.INVALID_STATE, () -> ModernNpcMelee.againstNpc(p, c, actor(c), value, b -> { throw new AssertionError(); }));
    }

    @Test void allMaterialUndeadAndBlessingBranchesUseTemplateCodeUnderExplicitConvention() {
        for (int material : new int[]{11, 14, 17, 22}) for (long undead = 0; undead <= 5; undead++) for (long bless = 0; bless <= 3; bless++) {
            var s = weaponFacts(armed(), new ModernCombatProfile.WeaponFacts(1, 1, 1, 8, 12, 1, material, bless, 0, 0, 0, 0, 0));
            var c = catalog(Map.of(UNDEAD, undead), true, 0);
            var hit = ModernNpcMelee.againstNpc(player(s), c, actor(c), 0, b -> 0);
            boolean metal = (undead == 1 || undead == 3 || undead == 5) && material != 11 || undead == 2 && (material == 17 || material == 22);
            boolean blessed = bless == 1 && undead >= 1 && undead <= 3;
            assertEquals(metal ? 1 : 0, hit.materialBonus()); assertEquals(blessed ? 1 : 0, hit.blessingBonus());
            assertEquals(4 + (metal ? 1 : 0) + (blessed ? 1 : 0), hit.rawDamage());
        }
    }

    @Test void maximumAndCriticalKeepLargeDieBranchAndApplyBeforeNpcDefense() {
        var p = player(weaponFacts(armed(), new ModernCombatProfile.WeaponFacts(1, 1, 1, 8, 12, 1, 11, 1, 10, 100, 1.5, 0, 0)));
        var c = catalog(); var maximumRolls = new Rolls(19, 0, 0, 0);
        var max = ModernNpcMelee.againstNpc(p, c, actor(c), 0, maximumRolls); assertEquals(22, max.rawDamage()); assertTrue(max.maximumTriggered()); assertTrue(max.criticalTriggered());
        maximumRolls.consumed(20, 100, 100, 100);
        var otherRolls = new Rolls(19, 0, 99, 10, 0); var other = ModernNpcMelee.againstNpc(p, c, actor(c), 0, otherRolls);
        assertEquals(21, other.rawDamage()); assertFalse(other.maximumTriggered()); otherRolls.consumed(20, 100, 100, 11, 100);
    }

    @Test void unarmedReplacementAndNpcReductionNeverReinterpretPlayerIgnoreOrPvpTerms() {
        var s = source(7, 0, 30, List.of(simpleArmor(2, 2, bonus(Stat.MELEE_DAMAGE, 999, Stat.IGNORE_REDUCTION, 999))));
        var c = catalog(Map.of(HARD, 1L, REDUCTION, 2L), true, 0);
        var hit = ModernNpcMelee.againstNpc(player(s), c, actor(c), 0, new Rolls(19, 0, 0));
        assertEquals(4, hit.rawDamage()); assertEquals(2, hit.appliedDamage());
        failure(ModernNpcMelee.Failure.UNSUPPORTED, () -> ModernNpcMelee.againstNpc(player(armed()), c, actor(c), 0, b -> { throw new AssertionError(); }));
        var a = source(7, 0, 30, List.of(weapon(1, 1, true, 0, bonus(Stat.IGNORE_REDUCTION, 999), TemplatePhysical.zero())));
        var normal = catalog(Map.of(REDUCTION, 100L), true, 0);
        assertEquals(0, ModernNpcMelee.againstNpc(player(a), normal, actor(normal), 0, b -> 0).appliedDamage());
    }

    @Test void npcToPlayerUsesOneTimesNegativeAcAndIndependentSourceStrengthGrowth() {
        var c = catalog(Map.of(), true, 7); var s = withVitals(7, source(7, 0, 30, List.of()), 100, -20);
        var r = new Rolls(19, 0, 0, 0, 99, 0);
        var hit = ModernNpcMelee.againstPlayer(c, actor(c), player(s), 100, false, r);
        assertEquals(23, hit.rawDamage()); assertEquals(22, hit.appliedDamage()); assertEquals(78, hit.remainingHp());
        r.consumed(20, 20, 100, 26, 100, 10);
        var positive = new Rolls(19, 0, 0, 99);
        assertEquals(23, ModernNpcMelee.againstPlayer(c, actor(c), player(source(7, 0, 30, List.of())), 100, false, positive).appliedDamage());
        positive.consumed(20, 100, 26, 100);
    }

    @Test void npcFumbleAndEvasionReturnBeforeDamageRollsWithoutFakeBossAccuracy() {
        var c = catalog(); var p = player(source(7, 0, 30, List.of()));
        for (long die = 0; die <= 2; die++) { var r = new Rolls(die, 15); assertFalse(ModernNpcMelee.againstPlayer(c, actor(c), p, 100, false, r).hit()); r.consumed(20, 100); }
        assertTrue(ModernNpcMelee.againstPlayer(c, actor(c), p, 100, false, new Rolls(3, 99, 0, 99)).hit());
        var evade = player(source(7, 0, 30, List.of(simpleArmor(2, 2, bonus(Stat.EVASION, 1000)))));
        var r = new Rolls(999); assertFalse(ModernNpcMelee.againstPlayer(c, actor(c), evade, 100, false, r).hit()); r.consumed(1000);
        var boss = catalog(Map.of(BOSS, 1L), true, 0);
        failure(ModernNpcMelee.Failure.UNSUPPORTED, () -> ModernNpcMelee.againstPlayer(boss, actor(boss), p, 100, false, b -> { throw new AssertionError(); }));
    }

    @Test void npcDamageUsesArmorRandomizationButNotPvpOrDirectionalReduction() {
        var armor = instance(2, 2, ModernCharacterData.ItemKind.ARMOR, true, 0, "$2", List.of(),
                bonus(Stat.MELEE_REDUCTION, 99, Stat.PVP_REDUCTION, 99, Stat.REDUCTION, 999), Ability.zero(), emptyCards(), Stone.none(), new TemplatePhysical(0, 0, 0, 0, 12, 0));
        var p = player(source(7, 0, 30, List.of(armor))); var c = catalog(); var r = new Rolls(19, 0, 25, 99, 1);
        var hit = ModernNpcMelee.againstPlayer(c, actor(c), p, 100, false, r);
        assertEquals(41, hit.rawDamage()); assertEquals(29, hit.damage()); r.consumed(20, 100, 26, 100, 2);
    }

    @Test void rationalPrefixMatchesIndependentDecimalOracleAndOrdinaryRetainedIntegerResults() {
        for (long strength = 0; strength <= 127; strength++) for (long roll = 0; roll < 26; roll++) for (boolean critical : new boolean[]{false, true}) for (boolean night : new boolean[]{false, true}) {
            long got = ModernNpcMelee.sourceSubtotal(26, strength, 0, roll, critical, night);
            assertEquals(decimal(strength, 0, roll, critical, night), got);
            assertEquals(ClassicWorldNpcDamage.sourceDamage(26, (int) strength, 0, (int) roll, critical ? 0 : 99, night, false), got);
        }
        for (long strength : new long[]{0, 1, 0x1_0000_0000L, 9_007_199_254_740_993L, Long.MAX_VALUE})
            assertEquals(decimal(strength, -7, 0, false, false), ModernNpcMelee.sourceSubtotal(1, strength, -7, 0, false, false));
        assertEquals(-7, ModernNpcMelee.sourceSubtotal(1, 0, -7, 0, false, false), "do not clamp a subtotal before defense");
        failure(ModernNpcMelee.Failure.ARITHMETIC_OVERFLOW, () -> ModernNpcMelee.sourceSubtotal(1, Long.MAX_VALUE, Long.MAX_VALUE, 0, true, true));
    }
    private static long decimal(long strength, long growth, long roll, boolean critical, boolean night) {
        var value = BigDecimal.valueOf(strength).multiply(new BigDecimal("0.8")).add(BigDecimal.valueOf(growth)).add(BigDecimal.valueOf(roll));
        if (critical) value = value.multiply(new BigDecimal("1.8")); if (night) value = value.multiply(new BigDecimal("1.2"));
        return value.setScale(0, RoundingMode.DOWN).longValueExact();
    }

    @Test void nightBonusOnlyAppliesToUndeadOneThreeFourAndCritThresholdIsFifteen() {
        var p = player(source(7, 0, 30, List.of()));
        for (long undead = 0; undead <= 5; undead++) {
            var c = catalog(Map.of(UNDEAD, undead), true, 0); boolean night = undead == 1 || undead == 3 || undead == 4;
            var hit = ModernNpcMelee.againstPlayer(c, actor(c), p, 100, true, new Rolls(19, 0, 0, 14));
            assertEquals(decimal(20, 0, 0, true, night), hit.rawDamage()); assertTrue(hit.criticalTriggered());
            var ordinary = ModernNpcMelee.againstPlayer(c, actor(c), p, 100, true, new Rolls(19, 0, 0, 15));
            assertEquals(decimal(20, 0, 0, false, night), ordinary.rawDamage()); assertFalse(ordinary.criticalTriggered());
        }
    }

    @Test void wideHpDamageAndSignedDefenseKeepExactValuesAndLimitOnlyAppliedHp() {
        var s = withVitals(7, source(7, 0, 30, List.of(weapon(1, 1, true, 0, bonus(Stat.MELEE_DAMAGE, 5_000_000_000L), TemplatePhysical.zero()))), Long.MAX_VALUE, 10);
        var c = catalog(Map.of(HP, 4_000_000_000L, STR, 5_000_000_000L), true, 0); var p = player(s);
        var incoming = ModernNpcMelee.againstNpc(p, c, actor(c), 0, b -> 0);
        assertEquals(5_000_000_004L, incoming.rawDamage()); assertEquals(4_000_000_000L, incoming.appliedDamage()); assertTrue(incoming.death());
        var outgoing = ModernNpcMelee.againstPlayer(c, actor(c), p, Long.MAX_VALUE, false, new Rolls(19, 0, 0, 99));
        assertEquals(4_000_000_000L, outgoing.rawDamage()); assertEquals(Long.MAX_VALUE - 4_000_000_000L, outgoing.remainingHp());
        var negative = catalog(Map.of(STR, 0L), true, -7);
        var armor = instance(2, 2, ModernCharacterData.ItemKind.ARMOR, true, 0, "$2", List.of(), Ability.zero(), Ability.zero(), emptyCards(), Stone.none(), new TemplatePhysical(0, 0, 0, 0, -5, 0));
        var target = player(source(7, 0, 30, List.of(armor)));
        var result = ModernNpcMelee.againstPlayer(negative, actor(negative), target, 100, false, new Rolls(19, 0, 0, 99));
        assertEquals(-7, result.rawDamage()); assertEquals(0, result.damage());
    }

    @Test void unknownBehaviorSourceOrNpcLifeAndUnqualifiedPlayerNeverBecomeDamagePermission() {
        var p = player(armed());
        for (var field : List.of(POISON, PARALYSIS, BRAVE, TELEPORT, DOPPEL, HP_REGEN, RANDOM_HP, BOSS)) {
            var c = catalog(Map.of(field, 1L), true, 0);
            failure(ModernNpcMelee.Failure.UNSUPPORTED, () -> ModernNpcMelee.againstNpc(p, c, actor(c), 0, b -> { throw new AssertionError(); }));
        }
        var c = catalog(); var old = actor(c); var t = old.template();
        var foreign = new ModernNpcRoster.Actor(old.id(), new ModernNpcRoster.Template(88, "other", NPC, t.graphic(), t.maximumHp(), t.maximumMp(), t.walkMillis(), t.attackMillis()),
                old.lane(), old.stage(), old.cell(), old.heading(), old.hp(), old.mp(), old.life(), old.action(), old.bornRevision(), old.changedRevision(), 0, old.nextMoveMillis(), 0, old.walk());
        assertThrows(IllegalArgumentException.class, () -> ModernNpcMelee.againstNpc(p, c, foreign, 0, b -> { throw new AssertionError(); }));
        failure(ModernNpcMelee.Failure.UNSUPPORTED, () -> ModernNpcMelee.player(fighter(withType(armed(), 4)), INTERNAL));
        for (long hp : new long[]{0, -1, 101}) failure(ModernNpcMelee.Failure.INVALID_STATE, () -> ModernNpcMelee.againstPlayer(c, old, p, hp, false, b -> { throw new AssertionError(); }));
    }

    @Test void invalidRandomAndOverflowFailWithoutPublishingAnyNpcState() throws Exception {
        var c = catalog(); var p = player(armed());
        for (long invalid : new long[]{-1, 20, Long.MAX_VALUE}) failure(ModernNpcMelee.Failure.INVALID_RANDOM, () -> ModernNpcMelee.againstNpc(p, c, actor(c), 0, b -> invalid));
        var huge = catalog(Map.of(LEVEL, Long.MAX_VALUE), true, 0);
        failure(ModernNpcMelee.Failure.ARITHMETIC_OVERFLOW, () -> ModernNpcMelee.againstPlayer(huge, actor(huge), p, 100, false, b -> 0));
        var extreme = catalog(Map.of(AC, Long.MIN_VALUE), true, 0);
        failure(ModernNpcMelee.Failure.ARITHMETIC_OVERFLOW, () -> ModernNpcMelee.againstNpc(p, extreme, actor(extreme), 0, b -> 0));
        try (var roster = ModernNpcRosterTest.roster()) {
            var a = roster.spawn(ModernNpcRosterTest.SCOPE, List.of(new ModernNpcRoster.Spawn(c.npcs().get(NPC).rosterTemplate(), 0, 1, ModernNpcRosterTest.cell(10, 10), 0)), Set.of()).get(0);
            var before = roster.snapshot();
            failure(ModernNpcMelee.Failure.INVALID_RANDOM, () -> ModernNpcMelee.prepareIncoming(p, c, before, a.id(), a.life(), 0, b -> -1));
            assertEquals(before, roster.snapshot());
        }
    }

    @Test void compareAndCommitConsumesEachHitOrMissOnceAndRejectsAnyChangedFrameOrRevivedLife() throws Exception {
        var c = catalog(); var p = player(armed()); var scope = ModernNpcRosterTest.SCOPE;
        try (var roster = ModernNpcRosterTest.roster()) {
            var a = roster.spawn(scope, List.of(new ModernNpcRoster.Spawn(c.npcs().get(NPC).rosterTemplate(), 0, 1, ModernNpcRosterTest.cell(10, 10), 0)), Set.of()).get(0);
            var frame = roster.snapshot(); var plan = ModernNpcMelee.prepareIncoming(p, c, frame, a.id(), a.life(), 0, b -> 0);
            assertEquals(frame, roster.snapshot()); assertEquals(p.fighter().key(), plan.attacker());
            assertEquals(p.fighter().profile().equipmentRevision(), plan.equipmentRevision());
            assertEquals(276, roster.commitIncoming(plan).hp()); assertEquals(frame.revision() + 1, roster.snapshot().revision());
            ModernNpcRosterTest.rejectedUnchanged(roster, () -> roster.commitIncoming(plan));
            var stale = ModernNpcMelee.prepareIncoming(p, c, roster.snapshot(), a.id(), 1, 0, b -> 0);
            roster.advance(scope, 1); ModernNpcRosterTest.rejectedUnchanged(roster, () -> roster.commitIncoming(stale));
            var oldLife = ModernNpcMelee.prepareIncoming(p, c, roster.snapshot(), a.id(), 1, 0, b -> 0);
            roster.damage(scope, a.id(), 1, Long.MAX_VALUE); roster.resurrect(scope, a.id(), 1, 280, 10, Set.of());
            ModernNpcRosterTest.rejectedUnchanged(roster, () -> roster.commitIncoming(oldLife));
            failure(ModernNpcMelee.Failure.INVALID_STATE, () -> ModernNpcMelee.prepareIncoming(p, c, roster.snapshot(), a.id(), 1, 0, b -> 0));
            var weak = player(source(7, 0, 1, List.of())); var beforeMiss = roster.snapshot();
            // AC10 gives defense0; reduce hit rate with the explicit weight5 term to reach the 15% branch.
            var miss = ModernNpcMelee.prepareIncoming(weak, c, beforeMiss, a.id(), 2, 5, new Rolls(0, 99)); assertFalse(miss.outcome().hit());
            assertEquals(beforeMiss.actors().get(0), roster.commitIncoming(miss), "a miss must not fabricate an animation event");
            assertEquals(beforeMiss.revision() + 1, roster.snapshot().revision()); ModernNpcRosterTest.rejectedUnchanged(roster, () -> roster.commitIncoming(miss));
        }
    }

    @Test void shared554PursuitLeadsToBidirectionalCalculatedDamageAndOldLifePlanRejection() throws Exception {
        var map = ModernQuestMapQualificationTest.shared(); var cells = map.serverChosenSpawns(ModernQuestMapQualificationTest.ANCHOR, 100);
        var scope = ModernNpcRosterTest.SCOPE; var c = catalog(Map.of(AC, -4L), true, 0);
        var target = new ModernNpcRoster.Target(ModernParticipantId.human(7, 3333), 0, cells.get(99), true);
        // Bind the actual test participant object id, not the helper's default source character id.
        var row = new ModernApplicationProtocol.CharacterRow(3333, "fixture-player", 0, 0, 30);
        var actual = fixtureSource(7, row, 100, 10, 1); var p = player(actual);
        try (var roster = new ModernNpcRoster(scope, map, List.of(cells.get(0), cells.get(1)), 20, 3000)) {
            var a = roster.spawn(scope, List.of(new ModernNpcRoster.Spawn(c.npcs().get(NPC).rosterTemplate(), 0, 1, cells.get(0), 0)), Set.of(target.cell())).get(0);
            int moves = 0;
            for (int tick = 1; tick <= 165; tick++) {
                roster.advance(scope, tick * 100L);
                var pursuit = roster.pursue(scope, a.id(), a.life(), List.of(target), Set.of(target.cell()), 4096, () -> false);
                if (pursuit.kind() == ModernNpcRoster.PlanKind.IN_RANGE) break;
                assertEquals(ModernNpcRoster.PlanKind.MOVE, pursuit.kind()); a = roster.move(scope, pursuit, Set.of(target.cell())); moves++;
            }
            assertEquals(12, moves); assertTrue(map.canStep(a.cell(), target.cell()));
            var outgoing = ModernNpcMelee.againstPlayer(c, a, p, 100, false, new Rolls(19, 0, 0, 99));
            assertEquals(16, outgoing.appliedDamage()); assertEquals(84, outgoing.remainingHp());
            var attackProposal = roster.pursue(scope, a.id(), a.life(), List.of(target), Set.of(target.cell()), 4096, () -> false);
            a = roster.attack(scope, attackProposal, target, Set.of(target.cell()));
            assertEquals(ModernNpcRoster.Action.ATTACK, a.action()); assertEquals(roster.snapshot().elapsedMillis() + 750, a.nextAttackMillis());
            var tooFast = roster.pursue(scope, a.id(), a.life(), List.of(target), Set.of(target.cell()), 4096, () -> false);
            ModernNpcRosterTest.rejectedUnchanged(roster, () -> roster.attack(scope, tooFast, target, Set.of(target.cell())));
            int strikes = 0; ModernNpcMelee.Incoming finalPlan = null;
            while (a.alive()) {
                finalPlan = ModernNpcMelee.prepareIncoming(p, c, roster.snapshot(), a.id(), a.life(), 0, b -> b - 1);
                a = roster.commitIncoming(finalPlan); assertTrue(++strikes < 100);
            }
            assertEquals(24, strikes); assertFalse(a.alive());
            var oldPlan = finalPlan; var revived = roster.resurrect(scope, a.id(), a.life(), 280, 10, Set.of(target.cell())); assertEquals(2, revived.life());
            ModernNpcRosterTest.rejectedUnchanged(roster, () -> roster.commitIncoming(oldPlan));
            roster.damage(scope, revived.id(), revived.life(), Long.MAX_VALUE); var corpse = roster.snapshot().actors().get(0);
            roster.advance(scope, corpse.corpseUntilMillis()); assertTrue(roster.snapshot().actors().isEmpty());
            System.out.println("shared554-java npcMelee=true moves=" + moves + " pcDamageCalculated=16 npcKillStrikes=" + strikes + " staleLifeRejected=true npcClock=true worldWire=false playerClockOwner=false");
        }
    }
}
