package local.lito381.server;

import static local.lito381.server.ModernEquipmentEvaluatorTest.*;
import static local.lito381.server.ModernEquipmentFacts.*;
import static local.lito381.server.ModernEquipmentFacts.Stat.*;
import static local.lito381.server.ModernItemLifetime.State.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** Pure source/effective boundary regressions. These fixtures explicitly qualify
 * lifetimes; they do not read SQL, renew timers, commit overrides or freeze a match. */
final class ModernEquipmentLifetimeTest {
    private static final long START = 1_000_000;
    private static final long CUTOFF = START + 1_000;
    private static final String DB_NOW = "2026-09-15 12:00:00";
    private static final String EXPIRY = "2026-09-15 12:00:01";

    @Test void expiredSqlEquippedArmorRemovesItsSavedAcContributionExactlyOnce() {
        var timed = simpleArmor(1, 919191, bonus(AC, -7, MAX_HP, 20, STR, 9));
        var permanent = simpleArmor(2, 828282, bonus(AC, -3, MAX_HP, 10));
        var q = timed(ModernCombatProfileTest.withVitals(qualified(List.of(timed, permanent)),
                100, 0, 80, 0, -30), timed.source().objectId(), EXPIRY);
        var savedBefore = q.source();
        var before = profile(q, Optional.empty(), CUTOFF - 1);
        var expired = profile(q, Optional.empty(), CUTOFF);
        var later = profile(q, Optional.empty(), CUTOFF + 10_000);
        assertEquals(-30, before.effective().armorClass());
        assertEquals(130, before.effective().maximumHp());
        assertEquals(-10, expired.sourceBaseline().armorClassModifier());
        assertEquals(-3, expired.equipmentEvaluation().armorClassModifier());
        assertEquals(-23, expired.effective().armorClass());
        assertEquals(110, expired.effective().maximumHp());
        assertEquals(200, expired.effective().strength());
        assertEquals(expired.effective(), later.effective());
        assertEquals(List.of(new ModernEquipmentAuthority.Equipped(1, false),
                new ModernEquipmentAuthority.Equipped(2, true)), expired.equipped());
        assertEquals(savedBefore, q.source());
        assertEquals(savedBefore, expired.source());
        assertTrue(q.source().inventory().get(0).equipped());
        assertEquals(-30, q.source().persisted().armorClass());
    }

    @Test void matchingStoredTrueCannotReviveExpiredSqlTrueOrSqlFalse() {
        for (boolean sqlEquipped : new boolean[]{false, true}) {
            var item = instance(1, 919191, ModernCharacterData.ItemKind.ARMOR, sqlEquipped,
                    0, "$919191", List.of(), bonus(AC, -7, STR, 9), Ability.zero(),
                    emptyCards(), Stone.none(), TemplatePhysical.zero());
            var q = timed(qualified(List.of(item)), 1, EXPIRY);
            var stored = stored(q, 12, Map.of(1L, true));
            var original = stored.orElseThrow();
            assertTrue(reconcile(q, stored, CUTOFF - 1).desiredLoadout().item(1).equipped());
            for (long now : new long[]{CUTOFF, CUTOFF + 1, CUTOFF + 100_000}) {
                var result = reconcile(q, stored, now);
                assertEquals(Boolean.FALSE, result.overrides().get(1L));
                assertFalse(result.desiredLoadout().item(1).equipped());
                assertTrue(result.staleItems().isEmpty());
                assertEquals(12, result.expected().revision());
                assertEquals(0, ModernEquipmentEvaluator.evaluate(q, result.overrides(), now).total(STR));
            }
            assertSame(original, stored.orElseThrow());
            assertTrue(original.value().overrides().get(0).equipped());
            assertEquals(sqlEquipped, q.source().inventory().get(0).equipped());
        }
    }

    @Test void expiryOfOneSetPieceReevaluatesHighestEligibleThreshold() {
        var one = instance(1, 91, ModernCharacterData.ItemKind.ARMOR, true, 0, "$91",
                List.of(5L), Ability.zero(), Ability.zero(), emptyCards(), Stone.none(), TemplatePhysical.zero());
        var two = instance(2, 92, ModernCharacterData.ItemKind.ARMOR, true, 0, "$92",
                List.of(5L), Ability.zero(), Ability.zero(), emptyCards(), Stone.none(), TemplatePhysical.zero());
        var q = timed(qualified(7, 0, List.of(one, two), defaults(List.of(one, two), false), false,
                List.of(new SetRule(5, 1, bonus(STR, 3, AC, 2)),
                        new SetRule(5, 2, bonus(STR, 20, AC, 8))), List.of()), 2, EXPIRY);
        var before = ModernEquipmentEvaluator.evaluate(q, Map.of(), CUTOFF - 1);
        var after = ModernEquipmentEvaluator.evaluate(q, Map.of(), CUTOFF);
        assertEquals(20, before.total(STR));
        assertEquals(-8, before.armorClassModifier());
        assertEquals(3, after.total(STR));
        assertEquals(-2, after.armorClassModifier());
        assertEquals(1, after.equippedRows());
        assertEquals(1, after.activeSets());
        assertEquals(20, ModernEquipmentEvaluator.evaluateSourceBaseline(q, CUTOFF).total(STR));
    }

    @Test void itemExpiryRemovesCardsGlobalCombosVipAndAllItemLayersTogether() {
        var armor = instance(1, 919191, ModernCharacterData.ItemKind.ARMOR, true, 0, "$1",
                List.of(), bonus(STR, 5), Ability.zero(), List.of(100L, 0L, 0L, 0L, 0L),
                Stone.none(), new TemplatePhysical(0, 0, 0, 0, 7, 0));
        var vip = instance(2, 717171, ModernCharacterData.ItemKind.ETC, true, 0, "$2",
                List.of(), Ability.zero(), Ability.zero(), List.of(101L, 0L, 0L, 0L, 0L),
                Stone.none(), TemplatePhysical.zero());
        var rules = defaults(List.of(armor, vip), true);
        rules.put(key(RuleKind.CARD, 100, 0), RuleValue.present(bonus(STR, 8)));
        rules.put(key(RuleKind.VIP, 717171, 0), RuleValue.present(bonus(STR, 30)));
        var original = qualified(7, 0, List.of(armor, vip), rules, true, List.of(),
                List.of(new ComboRule(1, Set.of(100L, 101L), bonus(STR, 20))));
        var oneExpired = timed(original, 1, EXPIRY);
        var before = ModernEquipmentEvaluator.evaluate(oneExpired, Map.of(), CUTOFF - 1);
        var after = ModernEquipmentEvaluator.evaluate(oneExpired, Map.of(), CUTOFF);
        assertEquals(63, before.total(STR));
        assertEquals(1, before.cardRows());
        assertEquals(1, before.activeCombos());
        assertEquals(7, before.magicReductionBase());
        assertEquals(30, after.total(STR));
        assertEquals(0, after.cardRows());
        assertEquals(0, after.activeCombos());
        assertEquals(0, after.magicReductionBase());
        assertEquals(1, after.vipRows());
        var bothExpired = observed(original, Map.of(1L, deadline(armor, EXPIRY), 2L, deadline(vip, EXPIRY)));
        var none = ModernEquipmentEvaluator.evaluate(bothExpired, Map.of(1L, true, 2L, true), CUTOFF);
        assertEquals(0, none.equippedRows());
        assertEquals(0, none.vipRows());
        assertEquals(Ability.zero(), none.direct());
        assertEquals(Ability.zero(), none.sets());
    }

    @Test void expiredWeaponCannotRetainWeaponFactsEnchantmentOrPhysicalModifiers() {
        long object = 0x1_0000_0001L, catalog = 0x2_0000_0001L;
        var item = ModernCombatProfileTest.weapon(object, catalog, true);
        var q = timed(qualified(List.of(item)), object, EXPIRY);
        var facts = ModernCombatProfileTest.facts(object, catalog, 1);
        var input = new ModernCombatProfile.Input(q, true, Map.of(object, facts));
        var stored = stored(q, 5, Map.of(object, true));
        var before = ModernCombatProfile.qualify(input, ModernCombatProfileTest.binding(q), stored,
                CUTOFF - 1, ModernCombatProfile.VitalPolicy.FULL_NEW_MATCH);
        var after = ModernCombatProfile.qualify(input, ModernCombatProfileTest.binding(q), stored,
                CUTOFF, ModernCombatProfile.VitalPolicy.FULL_NEW_MATCH);
        assertEquals(ModernCombatProfile.WeaponState.QUALIFIED, before.weapon().state());
        assertEquals(object, before.weapon().objectId());
        assertEquals(-5, before.weapon().enchantment());
        assertEquals(ModernCombatProfile.WeaponState.UNARMED, after.weapon().state());
        assertEquals(0, after.weapon().objectId());
        assertNull(after.weapon().facts());
        assertEquals(0, after.weapon().enchantment());
        assertEquals(TemplatePhysical.zero(), after.weapon().templateModifiers());
        assertEquals(Ability.zero(), after.weapon().itemModifiers());
        assertEquals(0, after.equipmentEvaluation().total(MELEE_DAMAGE));
        assertEquals(3, after.sourceBaseline().total(MELEE_DAMAGE));
        assertEquals(List.of(new ModernEquipmentAuthority.Equipped(object, false)), after.equipped());
        assertTrue(q.source().inventory().get(0).equipped());
    }

    @Test void itemDeadlineSuppressesAnOtherwiseActiveOrIndefiniteStone() {
        var item = instance(1, 91, ModernCharacterData.ItemKind.ARMOR, true, 0, "$91", List.of(),
                Ability.zero(), Ability.zero(), emptyCards(), new Stone(77, 3, OptionalLong.empty()),
                TemplatePhysical.zero());
        var rules = defaults(List.of(item), false);
        rules.put(key(RuleKind.STONE, 77, 3), RuleValue.present(bonus(MAX_MP, 55, STR, 7)));
        var q = timed(qualified(7, 0, List.of(item), rules, false, List.of(), List.of()), 1, EXPIRY);
        assertEquals(55, ModernEquipmentEvaluator.evaluate(q, Map.of(), CUTOFF - 1).total(MAX_MP));
        var expired = ModernEquipmentEvaluator.evaluate(q, Map.of(1L, true), CUTOFF);
        assertEquals(0, expired.total(MAX_MP));
        assertEquals(0, expired.total(STR));
        assertEquals(OptionalLong.empty(), expired.nextStoneExpiry());
        assertEquals(55, ModernEquipmentEvaluator.evaluateSourceBaseline(q, CUTOFF).total(MAX_MP));
    }

    @Test void absoluteExpiryUsesDatabaseRemainingDurationWithoutAddingRuleHoursOrQueryLatency() {
        var item = simpleArmor(1, 919191, Ability.zero());
        var q = timed(qualified(List.of(item)), 1, EXPIRY);
        assertEquals(new ModernItemLifetime.View(1, ACTIVE, CUTOFF), q.lifetimes().view(1, CUTOFF - 1));
        assertEquals(new ModernItemLifetime.View(1, EXPIRED, CUTOFF), q.lifetimes().view(1, CUTOFF));
        assertEquals(EXPIRY + ".000000", q.lifetimes().items().get(1L).absoluteLocalTime().orElseThrow());
        var subMillisecond = timed(qualified(List.of(item)), 1, "2026-09-15 12:00:00.001999");
        assertEquals(new ModernItemLifetime.View(1, ACTIVE, START + 1), subMillisecond.lifetimes().view(1, START));
        assertEquals(EXPIRED, subMillisecond.lifetimes().view(1, START + 1).state());
        assertEquals(EXPIRED, q.lifetimes().view(1, CUTOFF + 100_000).state());
    }

    @Test void renewedSourceDeadlineDoesNotReapplyAnOldStoredEquipIntent() {
        var item = instance(1, 919191, ModernCharacterData.ItemKind.ARMOR, false, 0, "$91", List.of(),
                bonus(STR, 8), Ability.zero(), emptyCards(), Stone.none(), TemplatePhysical.zero());
        var original = timed(qualified(List.of(item)), 1, EXPIRY);
        var stored = stored(original, 8, Map.of(1L, true));
        var renewed = timed(qualified(List.of(item)), 1, "2026-09-15 12:00:02");
        var result = reconcile(renewed, stored, CUTOFF);
        assertEquals(ACTIVE, renewed.lifetimes().view(1, CUTOFF).state());
        assertEquals(List.of(new ModernEquipmentReconciliation.StaleItem(1, 919191,
                ModernEquipmentReconciliation.StaleReason.SOURCE_FACTS_CHANGED)), result.staleItems());
        assertTrue(result.overrides().isEmpty());
        assertFalse(result.desiredLoadout().item(1).equipped());
        assertEquals(8, result.expected().revision());
        assertEquals(0, ModernEquipmentEvaluator.evaluate(renewed, result.overrides(), CUTOFF).total(STR));
        assertTrue(stored.orElseThrow().value().overrides().get(0).equipped());
        assertNotEquals(reconcile(original, Optional.empty(), CUTOFF).sourceFingerprints().get(1L),
                result.sourceFingerprints().get(1L));
    }

    @Test void fingerprintsTrackRawLifetimeFactsButNotMovingEvaluationTimeOrObservationClock() {
        var item = simpleArmor(1, 919191, bonus(STR, 1));
        var q = timed(qualified(List.of(item)), 1, EXPIRY);
        var before = reconcile(q, Optional.empty(), CUTOFF - 1);
        var after = reconcile(q, Optional.empty(), CUTOFF);
        assertEquals(before.sourceFingerprints(), after.sourceFingerprints());
        var sameFacts = new ModernItemLifetime.Observation(7, 3333, "2026-09-15 12:00:00.500000",
                START + 500, 999, q.lifetimes().items());
        var reread = new Qualified(q.source(), q.items(), q.catalog(), sameFacts);
        assertEquals(CUTOFF, reread.lifetimes().view(1, CUTOFF - 1).expiresAtMillis());
        assertEquals(before.sourceFingerprints(), reconcile(reread, Optional.empty(), CUTOFF).sourceFingerprints());
        var canonical = timed(qualified(List.of(item)), 1, EXPIRY + ".000000");
        assertEquals(before.sourceFingerprints(), reconcile(canonical, Optional.empty(), CUTOFF).sourceFingerprints());
    }

    @Test void unknownRelativeUnstartedAndLightingCannotBecomeEffectiveEquipment() {
        var armor = simpleArmor(1, 919191, bonus(STR, 8, AC, -7));
        var unknown = new ModernItemLifetime.Fact(1, 919191, ModernCharacterData.ItemKind.ARMOR, true,
                OptionalLong.empty(), false, OptionalLong.empty(), Optional.empty(), 0, false);
        var relative = new ModernItemLifetime.Fact(1, 919191, ModernCharacterData.ItemKind.ARMOR, true,
                OptionalLong.of(60), false, OptionalLong.empty(), Optional.empty(), 0, false);
        var missing = new ModernItemLifetime.Fact(1, 919191, ModernCharacterData.ItemKind.ARMOR, true,
                OptionalLong.of(0), true, OptionalLong.of(1), Optional.empty(), 0, false);
        var templateTimer = new ModernItemLifetime.Fact(1, 919191, ModernCharacterData.ItemKind.ARMOR, true,
                OptionalLong.of(0), false, OptionalLong.empty(), Optional.empty(), 60, false);
        var facts = List.of(unknown, relative, missing, templateTimer);
        var states = List.of(UNQUALIFIED, RELATIVE_UNQUALIFIED, UNSTARTED_OR_MISSING, UNSTARTED_OR_MISSING);
        for (int index = 0; index < facts.size(); index++) {
            var q = observed(qualified(List.of(armor)), Map.of(1L, facts.get(index)));
            assertEquals(states.get(index), q.lifetimes().view(1, START).state());
            assertEquals(0, ModernEquipmentEvaluator.evaluate(q, Map.of(1L, true), START).total(STR));
            assertEquals(-7, ModernEquipmentEvaluator.evaluateSourceBaseline(q, START).armorClassModifier());
            assertFalse(reconcile(q, stored(q, 1, Map.of(1L, true)), START).desiredLoadout().item(1).equipped());
        }
        var etc = instance(1, 919191, ModernCharacterData.ItemKind.ETC, true, 0, "$91", List.of(),
                Ability.zero(), Ability.zero(), emptyCards(), Stone.none(), TemplatePhysical.zero());
        var rules = defaults(List.of(etc), true);
        rules.put(key(RuleKind.VIP, 919191, 0), RuleValue.present(bonus(STR, 30)));
        var lighting = new ModernItemLifetime.Fact(1, 919191, ModernCharacterData.ItemKind.ETC, true,
                OptionalLong.of(0), false, OptionalLong.empty(), Optional.of(EXPIRY), 0, true);
        var q = observed(qualified(7, 0, List.of(etc), rules, true, List.of(), List.of()), Map.of(1L, lighting));
        assertEquals(LIGHTING_UNQUALIFIED, q.lifetimes().view(1, START).state());
        assertEquals(0, ModernEquipmentEvaluator.evaluate(q, Map.of(1L, true), START).vipRows());
        assertEquals(30, ModernEquipmentEvaluator.evaluateSourceBaseline(q, START).total(STR));
    }

    @Test void threeArgumentQualifiedDoesNotSilentlyAssertPermanentLifetimes() {
        var item = simpleArmor(1, 919191, bonus(STR, 8));
        var fixture = qualified(List.of(item));
        var unknown = new Qualified(fixture.source(), fixture.items(), fixture.catalog());
        assertEquals(UNQUALIFIED, unknown.lifetimes().view(1, START).state());
        assertEquals(0, ModernEquipmentEvaluator.evaluate(unknown, Map.of(), START).total(STR));
        var trusted = new Qualified(fixture.source(), fixture.items(), fixture.catalog(),
                ModernItemLifetime.Observation.verifiedPermanent(fixture.source()));
        assertEquals(PERMANENT, trusted.lifetimes().view(1, Long.MAX_VALUE).state());
        assertEquals(8, ModernEquipmentEvaluator.evaluate(trusted, Map.of(), Long.MAX_VALUE).total(STR));
    }

    @Test void lifetimeFactsCannotCrossSourceObjectCatalogKindOrSqlEquippedIdentity() {
        var q = timed(qualified(List.of(simpleArmor(1, 919191, Ability.zero()))), 1, EXPIRY);
        var observation = q.lifetimes();
        for (var wrong : List.of(
                new ModernItemLifetime.Observation(42, 3333, DB_NOW, START, 0, observation.items()),
                new ModernItemLifetime.Observation(7, 3334, DB_NOW, START, 0, observation.items()))) {
            assertThrows(IllegalArgumentException.class, () -> new Qualified(q.source(), q.items(), q.catalog(), wrong));
        }
        for (var wrong : List.of(
                new ModernItemLifetime.Fact(1, 919192, ModernCharacterData.ItemKind.ARMOR, true,
                        OptionalLong.of(0), false, OptionalLong.empty(), Optional.empty(), 0, false),
                new ModernItemLifetime.Fact(1, 919191, ModernCharacterData.ItemKind.WEAPON, true,
                        OptionalLong.of(0), false, OptionalLong.empty(), Optional.empty(), 0, false),
                new ModernItemLifetime.Fact(1, 919191, ModernCharacterData.ItemKind.ARMOR, false,
                        OptionalLong.of(0), false, OptionalLong.empty(), Optional.empty(), 0, false))) {
            assertThrows(IllegalArgumentException.class, () -> observed(q, Map.of(1L, wrong)));
        }
        var wrongSource = new ModernEquipmentReconciliation.ExpectedBinding(
                new TournamentPresence.CharacterKey(42, 3333), ModernCombatProfileTest.BINDING);
        assertEquals(ModernEquipmentReconciliation.Failure.SOURCE_KEY_MISMATCH,
                assertThrows(ModernEquipmentReconciliation.ReconciliationException.class,
                        () -> ModernEquipmentReconciliation.reconcile(q, wrongSource, Optional.empty(), CUTOFF)).failure());
        var previous = stored(q, 1, Map.of(1L, true));
        byte[] otherOwner = new byte[32];
        otherOwner[0] = 1;
        var wrongOwner = new ModernEquipmentReconciliation.ExpectedBinding(ModernCombatProfileTest.binding(q).key(),
                new ModernEquipmentOverrideStore.Fingerprint(otherOwner));
        assertEquals(ModernEquipmentReconciliation.Failure.BINDING_MISMATCH,
                assertThrows(ModernEquipmentReconciliation.ReconciliationException.class,
                        () -> ModernEquipmentReconciliation.reconcile(q, wrongOwner, previous, CUTOFF)).failure());
    }

    @Test void backwardsClockNeverMakesTheSameObservationUsableBeforeItsQueryStart() {
        var q = timed(qualified(List.of(simpleArmor(1, 919191, bonus(STR, 8)))), 1, EXPIRY);
        assertEquals(EXPIRED, q.lifetimes().view(1, CUTOFF).state());
        assertEquals(UNQUALIFIED, q.lifetimes().view(1, START - 1).state());
        assertFalse(reconcile(q, stored(q, 1, Map.of(1L, true)), START - 1).desiredLoadout().item(1).equipped());
        assertEquals(0, ModernEquipmentEvaluator.evaluate(q, Map.of(1L, true), START - 1).total(STR));
        // This pure observation is not a durable high-watermark across calls or restarts.
    }

    @Test void expiredAcStillChecksSignedI64DeltaOverflowInsteadOfClampingOrSkippingBaseline() {
        var item = simpleArmor(0x1_0000_0001L, 0x2_0000_0001L, bonus(AC, Long.MIN_VALUE));
        var q = timed(qualified(List.of(item)), item.source().objectId(), EXPIRY);
        assertEquals(Long.MIN_VALUE, ModernEquipmentEvaluator.evaluateSourceBaseline(q, CUTOFF).total(AC));
        assertEquals(0, ModernEquipmentEvaluator.evaluate(q, Map.of(), CUTOFF).total(AC));
        var savedBefore = q.source();
        assertThrows(ArithmeticException.class, () -> profile(q, Optional.empty(), CUTOFF));
        assertEquals(savedBefore, q.source());
    }

    @Test void frozenProfileDeadlineTakesCurrentEquipmentAndStoneMinimumButNotInventoryOnlyTimers() {
        var worn = instance(1, 919191, ModernCharacterData.ItemKind.ARMOR, true, 0, "$1", List.of(),
                bonus(MAX_HP, 20), Ability.zero(), emptyCards(), new Stone(77, 3, OptionalLong.of(START + 2_000)),
                TemplatePhysical.zero());
        var carried = instance(2, 828282, ModernCharacterData.ItemKind.ARMOR, false, 0, "$2", List.of(),
                bonus(MAX_HP, 10), Ability.zero(), emptyCards(), Stone.none(), TemplatePhysical.zero());
        var rules = defaults(List.of(worn, carried), false);
        rules.put(key(RuleKind.STONE, 77, 3), RuleValue.present(bonus(STR, 7)));
        var base = qualified(7, 0, List.of(worn, carried), rules, false, List.of(), List.of());
        var laterItem = observed(base, Map.of(1L, deadline(worn, "2026-09-15 12:00:03"),
                2L, deadline(carried, EXPIRY)));
        var stoneFirst = profile(laterItem, Optional.empty(), START);
        assertEquals(START + 2_000, stoneFirst.validUntilMillis());
        assertEquals(OptionalLong.of(START + 2_000), stoneFirst.equipmentEvaluation().nextStoneExpiry());
        assertEquals(ACTIVE, laterItem.lifetimes().view(2, START).state());
        assertFalse(stoneFirst.equipped().get(1).equipped());

        var earlierItem = observed(base, Map.of(1L, deadline(worn, "2026-09-15 12:00:01.500000"),
                2L, deadline(carried, EXPIRY)));
        assertEquals(START + 1_500, profile(earlierItem, Optional.empty(), START).validUntilMillis());
        var removed = profile(laterItem, stored(laterItem, 1, Map.of(1L, false)), START);
        assertEquals(-1, removed.validUntilMillis());
        assertEquals(OptionalLong.empty(), removed.equipmentEvaluation().nextStoneExpiry());
        assertTrue(laterItem.source().inventory().get(0).equipped());
        assertEquals(OptionalLong.of(START + 2_000), removed.sourceBaseline().nextStoneExpiry());

        var swapped = profile(laterItem, stored(laterItem, 2, Map.of(1L, false, 2L, true)), START);
        assertEquals(CUTOFF, swapped.validUntilMillis());
        assertEquals(OptionalLong.empty(), swapped.equipmentEvaluation().nextStoneExpiry());
        assertTrue(swapped.equipped().get(1).equipped());
    }

    @Test void expiredSqlEquippedCustomExecutorStillBlocksUnqualifiedBaselineButUnwornInventoryDoesNot() {
        assertUnsupportedSourceBaseline(false);
    }

    @Test void expiredSqlEquippedProcStillBlocksUnqualifiedBaselineButUnwornInventoryDoesNot() {
        assertUnsupportedSourceBaseline(true);
    }

    private static void assertUnsupportedSourceBaseline(boolean proc) {
        var expected = proc ? ModernPhysicalCombat.UnsupportedReason.EQUIPMENT_PROC
                : ModernPhysicalCombat.UnsupportedReason.CUSTOM_EXECUTOR;
        for (boolean sqlEquipped : new boolean[]{true, false}) {
            var item = instance(1, 919191, ModernCharacterData.ItemKind.ARMOR, sqlEquipped, 0, "$1", List.of(),
                    bonus(AC, -7, STR, 8), Ability.zero(), emptyCards(), Stone.none(), TemplatePhysical.zero());
            var original = ModernPhysicalCombatTest.source(7, 0, 30, List.of(item));
            var q = timed(original.input().equipment(), 1, EXPIRY);
            var policies = new HashMap<>(original.templates());
            if (!proc) policies.put(919191L, new ModernPhysicalCombat.TemplatePolicy(
                    ModernCharacterData.ItemKind.ARMOR, "fixture.CustomExecutor", null));
            var source = new ModernPhysicalCombat.Source(new ModernCombatProfile.Input(q, true, original.input().weapons()),
                    original.growth(), policies, proc ? Set.of(919191L) : Set.of());
            var desired = stored(q, 1, Map.of(1L, true));
            var active = ModernPhysicalCombat.qualify(source, ModernCombatProfileTest.binding(q), desired, CUTOFF - 1);
            assertFalse(active.supported());
            assertEquals(expected, active.unsupportedReason());

            var expired = ModernPhysicalCombat.qualify(source, ModernCombatProfileTest.binding(q), desired, CUTOFF);
            assertFalse(expired.profile().equipped().get(0).equipped());
            assertEquals(0, expired.profile().equipmentEvaluation().total(STR));
            assertEquals(sqlEquipped ? -7 : 0, expired.profile().sourceBaseline().armorClassModifier());
            assertEquals(sqlEquipped ? 17 : 10, expired.profile().effective().armorClass());
            assertEquals(!sqlEquipped, expired.supported());
            assertEquals(sqlEquipped ? expected : ModernPhysicalCombat.UnsupportedReason.NONE, expired.unsupportedReason());
            assertEquals(-1, expired.profile().validUntilMillis());
            assertEquals(sqlEquipped, q.source().inventory().get(0).equipped());

            if (!sqlEquipped) {
                var justCarried = ModernPhysicalCombat.qualify(source, ModernCombatProfileTest.binding(q), Optional.empty(), CUTOFF - 1);
                assertTrue(justCarried.supported());
                assertFalse(justCarried.profile().equipped().get(0).equipped());
                assertEquals(-1, justCarried.profile().validUntilMillis());
            }
        }
    }

    private static ModernItemLifetime.Fact deadline(Instance item, String expiry) {
        return new ModernItemLifetime.Fact(item.source().objectId(), item.source().template().catalogId(),
                item.source().template().kind(), item.source().equipped(), OptionalLong.of(0), true,
                OptionalLong.of(24), Optional.of(expiry), 0, false);
    }

    private static Qualified timed(Qualified q, long itemId, String expiry) {
        return observed(q, Map.of(itemId, deadline(q.items().get(itemId), expiry)));
    }

    private static Qualified observed(Qualified q, Map<Long, ModernItemLifetime.Fact> replacements) {
        var facts = new HashMap<>(ModernItemLifetime.Observation.verifiedPermanent(q.source()).items());
        facts.putAll(replacements);
        return new Qualified(q.source(), q.items(), q.catalog(), new ModernItemLifetime.Observation(
                q.source().sourceId(), q.source().character().objectId(), DB_NOW, START, 17, facts));
    }

    private static ModernEquipmentReconciliation.Result reconcile(Qualified q,
            Optional<ModernEquipmentOverrideStore.Stored> stored, long now) {
        return ModernEquipmentReconciliation.reconcile(q, ModernCombatProfileTest.binding(q), stored, now);
    }

    private static Optional<ModernEquipmentOverrideStore.Stored> stored(Qualified q, long revision,
            Map<Long, Boolean> desired) {
        var fingerprints = reconcile(q, Optional.empty(), START).sourceFingerprints();
        var rows = desired.entrySet().stream().map(entry -> new ModernEquipmentOverrideStore.ItemOverride(
                entry.getKey(), q.items().get(entry.getKey()).source().template().catalogId(),
                fingerprints.get(entry.getKey()), entry.getValue())).toList();
        return Optional.of(new ModernEquipmentOverrideStore.Stored(ModernCombatProfileTest.binding(q).key(), revision,
                new ModernEquipmentOverrideStore.Replacement(ModernCombatProfileTest.BINDING, rows)));
    }

    private static ModernCombatProfile profile(Qualified q, Optional<ModernEquipmentOverrideStore.Stored> stored, long now) {
        return ModernCombatProfile.qualify(ModernCombatProfile.Input.unqualified(q), ModernCombatProfileTest.binding(q),
                stored, now, ModernCombatProfile.VitalPolicy.FULL_NEW_MATCH);
    }
}
