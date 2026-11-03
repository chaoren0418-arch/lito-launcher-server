package local.lito381.server;

import static local.lito381.server.ModernEquipmentFacts.*;
import static local.lito381.server.ModernEquipmentReconciliation.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.Set;
import java.util.function.UnaryOperator;
import org.junit.jupiter.api.Test;

final class ModernEquipmentReconciliationTest {
    @Test void v1FingerprintMatchesIndependentBigEndianUtf8Sha256KnownAnswer() {
        // Independent .NET MemoryStream/BigEndian/SHA256 reference: 1853 bytes,
        // 39 named Stat fields per ability; fixture armor(1,100,false).
        assertEquals("2F124BE598C7558D7FF804FF258AFE9D1702821ABDF057EBD449ACBB926334A4",
                HexFormat.of().withUpperCase().formatHex(fingerprint(armor(1, 100, false)).bytes()));
        failure(Failure.INVALID_INPUT, () -> fingerprint(null));
    }

    @Test void absentStoredRecordUsesFreshFlagsAndDoesNotInventPersistenceOrAnOwner() {
        var first = armor(1, 100, false);
        var second = armor(2, 101, true);
        var fresh = qualified(7, 3333, List.of(first, second));
        var result = reconcile(fresh, expected(7, 3333, 9), Optional.empty());
        assertEquals(ModernEquipmentOverrideStore.Expected.absent(), result.expected());
        assertTrue(result.overrides().isEmpty()); assertTrue(result.staleItems().isEmpty());
        assertFalse(result.desiredLoadout().item(1).equipped());
        assertTrue(result.desiredLoadout().item(2).equipped());
        assertEquals(List.of(1L, 2L), List.copyOf(result.sourceFingerprints().keySet()));
        assertEquals(fingerprint(first), result.sourceFingerprints().get(1L));
        assertEquals(fingerprint(second), result.sourceFingerprints().get(2L));
        assertEquals(false, fresh.source().inventory().get(0).equipped());
    }

    @Test void matchingNumericInstanceAppliesDesiredFlagButKeepsTheOriginalRevisionAndSource() {
        var first = armor(0x1_0000_0001L, 0x2_0000_0001L, false);
        var second = armor(Long.MAX_VALUE, Long.MAX_VALUE, true);
        var fresh = qualified(42, Long.MAX_VALUE, List.of(first, second));
        var binding = expected(42, Long.MAX_VALUE, 1);
        var stored = stored(binding, 23, List.of(saved(first, true), saved(second, false)));
        var result = reconcile(fresh, binding, Optional.of(stored));
        assertEquals(ModernEquipmentOverrideStore.Expected.of(stored), result.expected());
        assertEquals(Map.of(first.source().objectId(), true, second.source().objectId(), false), result.overrides());
        assertTrue(result.desiredLoadout().item(first.source().objectId()).equipped());
        assertFalse(result.desiredLoadout().item(second.source().objectId()).equipped());
        assertTrue(result.staleItems().isEmpty());
        assertFalse(first.source().equipped()); assertTrue(second.source().equipped());
        assertEquals(2, stored.value().overrides().size());
    }

    @Test void sourceCharacterAndBindingMismatchRejectEvenAnEmptyOldOverrideRecord() {
        var item = armor(1, 100, false);
        var fresh = qualified(7, 3333, List.of(item));
        var right = expected(7, 3333, 1);
        failure(Failure.SOURCE_KEY_MISMATCH,
                () -> reconcile(fresh, expected(42, 3333, 1), Optional.empty()));
        failure(Failure.SOURCE_KEY_MISMATCH,
                () -> reconcile(fresh, expected(7, 3334, 1), Optional.empty()));
        failure(Failure.STORED_KEY_MISMATCH, () -> reconcile(fresh, right,
                Optional.of(stored(expected(42, 3333, 1), 1, List.of()))));
        failure(Failure.STORED_KEY_MISMATCH, () -> reconcile(fresh, right,
                Optional.of(stored(expected(7, 3334, 1), 1, List.of()))));
        failure(Failure.BINDING_MISMATCH, () -> reconcile(fresh, right,
                Optional.of(stored(expected(7, 3333, 2), 1, List.of()))));
        failure(Failure.INVALID_INPUT, () -> reconcile(fresh, right, null));
        failure(Failure.INVALID_INPUT, () -> reconcile(null, right, Optional.empty()));
        failure(Failure.INVALID_INPUT, () -> reconcile(fresh, null, Optional.empty()));
    }

    @Test void staleMissingReusedAndChangedInstancesAreReportedNotAppliedOrDeleted() {
        var removed = armor(1, 100, false);
        var reused = armor(2, 200, false);
        var changed = armor(3, 300, false);
        var valid = armor(4, 400, false);
        var freshChanged = withSource(changed, s -> inventory(s, s.template(), s.quantity() + 1,
                s.equipped(), s.enchantment(), s.durability(), s.identified(), s.blessing(), s.sealed(), s.chargeCount()));
        var fresh = qualified(7, 3333, List.of(armor(2, 201, false), freshChanged, valid));
        var binding = expected(7, 3333, 4);
        var old = stored(binding, 88, List.of(saved(removed, true), saved(reused, true), saved(changed, true), saved(valid, true)));
        var result = reconcile(fresh, binding, Optional.of(old));
        assertEquals(List.of(new StaleItem(1, 100, StaleReason.SOURCE_ITEM_MISSING),
                new StaleItem(2, 200, StaleReason.SOURCE_CATALOG_CHANGED),
                new StaleItem(3, 300, StaleReason.SOURCE_FACTS_CHANGED)), result.staleItems());
        assertEquals(Map.of(4L, true), result.overrides());
        assertNull(result.desiredLoadout().item(1));
        assertFalse(result.desiredLoadout().item(2).equipped());
        assertFalse(result.desiredLoadout().item(3).equipped());
        assertTrue(result.desiredLoadout().item(4).equipped());
        assertEquals(88, result.expected().revision());
        assertEquals(4, old.value().overrides().size()); // No implicit cleanup or new Stored.
        assertEquals(Set.of(2L, 3L, 4L), result.sourceFingerprints().keySet());
    }

    @Test void freshEquippedFlagChangeIsStaleEvenWhenTheOldDesiredValueNowMatches() {
        var original = armor(1, 100, false);
        var changed = withSource(original, s -> inventory(s, s.template(), s.quantity(), true,
                s.enchantment(), s.durability(), s.identified(), s.blessing(), s.sealed(), s.chargeCount()));
        var binding = expected(7, 3333, 1);
        var result = reconcile(qualified(7, 3333, List.of(changed)), binding,
                Optional.of(stored(binding, 5, List.of(saved(original, true)))));
        assertTrue(result.overrides().isEmpty());
        assertEquals(List.of(new StaleItem(1, 100, StaleReason.SOURCE_FACTS_CHANGED)), result.staleItems());
        assertTrue(result.desiredLoadout().item(1).equipped()); // Fresh source only, not replay.
        assertEquals(5, result.expected().revision());
    }

    @Test void everyInventorySourceFieldAndExactMetadataParticipateInFingerprint() {
        var item = armor(1, 100, false);
        var src = item.source();
        var mutations = List.<UnaryOperator<ModernCharacterData.InventoryItem>>of(
                s -> new ModernCharacterData.InventoryItem(2, s.template(), s.quantity(), s.equipped(), s.enchantment(), s.durability(), s.identified(), s.blessing(), s.sealed(), s.chargeCount()),
                s -> inventory(s, s.template(), 2, s.equipped(), s.enchantment(), s.durability(), s.identified(), s.blessing(), s.sealed(), s.chargeCount()),
                s -> inventory(s, s.template(), s.quantity(), true, s.enchantment(), s.durability(), s.identified(), s.blessing(), s.sealed(), s.chargeCount()),
                s -> inventory(s, s.template(), s.quantity(), s.equipped(), Long.MIN_VALUE, s.durability(), s.identified(), s.blessing(), s.sealed(), s.chargeCount()),
                s -> inventory(s, s.template(), s.quantity(), s.equipped(), s.enchantment(), Long.MAX_VALUE, s.identified(), s.blessing(), s.sealed(), s.chargeCount()),
                s -> inventory(s, s.template(), s.quantity(), s.equipped(), s.enchantment(), s.durability(), false, s.blessing(), s.sealed(), s.chargeCount()),
                s -> inventory(s, s.template(), s.quantity(), s.equipped(), s.enchantment(), s.durability(), s.identified(), 255, s.sealed(), s.chargeCount()),
                s -> inventory(s, s.template(), s.quantity(), s.equipped(), s.enchantment(), s.durability(), s.identified(), s.blessing(), true, s.chargeCount()),
                s -> inventory(s, s.template(), s.quantity(), s.equipped(), s.enchantment(), s.durability(), s.identified(), s.blessing(), s.sealed(), Long.MAX_VALUE));
        for (var mutation : mutations) assertNotEquals(fingerprint(item), fingerprint(withSource(item, mutation)));
        var original = src.template();
        for (var metadata : List.of(
                new ModernCharacterData.ItemTemplate(101, original.kind(), original.nameToken(), 1, 2, 3),
                new ModernCharacterData.ItemTemplate(100, original.kind(), "$100changed", 1, 2, 3),
                new ModernCharacterData.ItemTemplate(100, original.kind(), original.nameToken(), 0x1_0000_0001L, 2, 3),
                new ModernCharacterData.ItemTemplate(100, original.kind(), original.nameToken(), 1, 0x1_0000_0002L, 3),
                new ModernCharacterData.ItemTemplate(100, original.kind(), original.nameToken(), 1, 2, Long.MAX_VALUE))) {
            assertNotEquals(fingerprint(item), fingerprint(withSource(item, s -> inventory(s, metadata, s.quantity(), s.equipped(),
                    s.enchantment(), s.durability(), s.identified(), s.blessing(), s.sealed(), s.chargeCount()))));
        }
        var narrow = withSource(item, s -> inventory(s, s.template(), 1, false, 0, 0, true, 1, false, 1));
        var wide = withSource(item, s -> inventory(s, s.template(), 1, false, 0, 0, true, 1, false, 0x1_0000_0001L));
        assertNotEquals(fingerprint(narrow), fingerprint(wide));
        var composed = new ModernCharacterData.ItemTemplate(100, original.kind(), "\u00e9", 1, 2, 3);
        var decomposed = new ModernCharacterData.ItemTemplate(100, original.kind(), "e\u0301", 1, 2, 3);
        assertNotEquals(fingerprint(withSource(item, s -> inventory(s, composed, 1, false, 0, 0, true, 1, false, 0))),
                fingerprint(withSource(item, s -> inventory(s, decomposed, 1, false, 0, 0, true, 1, false, 0))));
    }

    @Test void completeRulesRetainedTypeSafeEnchantAndPhysicalTemplateParticipate() {
        var item = armor(1, 100, false);
        var base = item.template();
        var range = new EquipmentRules.LevelRange(1, Long.MAX_VALUE);
        for (var rules : List.of(
                new EquipmentRules.ArmorFacts(1, range, EquipmentRules.ArmorSlot.BODY),
                new EquipmentRules.ArmorFacts(127, new EquipmentRules.LevelRange(2, Long.MAX_VALUE), EquipmentRules.ArmorSlot.BODY),
                new EquipmentRules.ArmorFacts(127, new EquipmentRules.LevelRange(1, Long.MAX_VALUE - 1), EquipmentRules.ArmorSlot.BODY),
                new EquipmentRules.ArmorFacts(127, range, EquipmentRules.ArmorSlot.HELMET))) {
            assertNotEquals(fingerprint(item), fingerprint(withTemplate(item,
                    new Template(base.metadata(), rules, rules.slot().retainedType(), base.safeEnchant(), base.setIds(), base.base(), base.physical()))));
        }
        assertNotEquals(fingerprint(item), fingerprint(withTemplate(item,
                new Template(base.metadata(), base.rules(), base.retainedType(), -1, base.setIds(), base.base(), base.physical()))));
        for (var physical : List.of(new TemplatePhysical(1,0,0,0,0,0), new TemplatePhysical(0,1,0,0,0,0),
                new TemplatePhysical(0,0,1,0,0,0), new TemplatePhysical(0,0,0,1,0,0),
                new TemplatePhysical(0,0,0,0,1,0), new TemplatePhysical(0,0,0,0,0,1))) {
            assertNotEquals(fingerprint(item), fingerprint(withTemplate(item,
                    new Template(base.metadata(), base.rules(), base.retainedType(), base.safeEnchant(), base.setIds(), base.base(), physical))));
        }
        var weapon = weapon(1, 100, false);
        var w = weapon.template();
        assertNotEquals(fingerprint(item), fingerprint(weapon)); // kind and rule discriminator.
        for (var rules : List.of(new EquipmentRules.WeaponFacts(127, range, true),
                new EquipmentRules.WeaponFacts(1, range, false),
                new EquipmentRules.WeaponFacts(127, new EquipmentRules.LevelRange(2, Long.MAX_VALUE), false),
                new EquipmentRules.WeaponFacts(127, new EquipmentRules.LevelRange(1, Long.MAX_VALUE - 1), false))) {
            assertNotEquals(fingerprint(weapon), fingerprint(withTemplate(weapon, new Template(w.metadata(),
                    rules, 1, 6, List.of(), w.base(), w.physical()))));
        }
        assertNotEquals(fingerprint(weapon), fingerprint(withTemplate(weapon, new Template(w.metadata(), w.rules(),
                2, 6, List.of(), w.base(), w.physical()))));
        assertNotEquals(fingerprint(item), fingerprint(etc(1, 100, false)));
    }

    @Test void everyAbilityAndDarkPresenceIsHashedWithoutMapIterationOrNumericNarrowing() {
        var item = armor(1, 100, false);
        var template = item.template();
        for (Stat stat : Stat.values()) {
            var changed = Ability.zero().with(stat, Long.MIN_VALUE);
            assertNotEquals(fingerprint(item), fingerprint(withTemplate(item, new Template(template.metadata(), template.rules(),
                    template.retainedType(), template.safeEnchant(), template.setIds(), changed, template.physical()))), stat.name());
            assertNotEquals(fingerprint(item), fingerprint(new Instance(item.source(), template, 0,
                    RuleValue.present(changed), item.cards(), item.stone())), stat.name());
        }
        assertNotEquals(fingerprint(item), fingerprint(new Instance(item.source(), template, 0,
                RuleValue.present(Ability.zero()), item.cards(), item.stone())));
        var reversed = new HashMap<Stat, Long>();
        Stat[] stats = Stat.values();
        for (int i = stats.length - 1; i >= 0; i--) reversed.put(stats[i], 0L);
        assertEquals(fingerprint(item), fingerprint(withTemplate(item, new Template(template.metadata(), template.rules(),
                template.retainedType(), template.safeEnchant(), template.setIds(), new Ability(reversed), template.physical()))));
        assertEquals(fingerprint(item), fingerprint(item));
    }

    @Test void orderedSetDuplicatesCardSlotsGreaterAndAllStoneFieldsAreNotDiscarded() {
        var item = armor(1, 100, false);
        var template = item.template();
        var seen = new java.util.HashSet<ModernEquipmentOverrideStore.Fingerprint>();
        for (var sets : List.of(List.<Long>of(), List.of(5L), List.of(5L, 5L), List.of(5L, 6L), List.of(6L, 5L))) {
            assertTrue(seen.add(fingerprint(withTemplate(item, new Template(template.metadata(), template.rules(),
                    template.retainedType(), template.safeEnchant(), sets, template.base(), template.physical())))));
        }
        seen.clear();
        for (int slot = 0; slot < 5; slot++) {
            var cards = new ArrayList<>(emptyCards()); cards.set(slot, 0x1_0000_0001L);
            assertTrue(seen.add(fingerprint(new Instance(item.source(), template, 0, item.dark(), cards, Stone.none()))));
        }
        assertNotEquals(fingerprint(item), fingerprint(new Instance(item.source(), template, Long.MAX_VALUE,
                item.dark(), item.cards(), Stone.none())));
        seen.clear();
        for (var stone : List.of(Stone.none(), new Stone(1, 0, OptionalLong.empty()),
                new Stone(2, 0, OptionalLong.empty()), new Stone(1, 1, OptionalLong.empty()),
                new Stone(1, 0, OptionalLong.of(0)), new Stone(1, 0, OptionalLong.of(Long.MAX_VALUE)))) {
            assertTrue(seen.add(fingerprint(new Instance(item.source(), template, 0, item.dark(), item.cards(), stone))));
        }
    }

    @Test void externalCatalogChangesStillRequireFreshEvaluationNotAutomaticOverridePermission() {
        var item = armor(1, 100, false);
        var before = qualified(7, 3333, List.of(item));
        var lookups = new HashMap<>(before.catalog().lookups());
        var key = new RuleKey(RuleKind.GREATER_TYPE, ModernCharacterData.ItemKind.ARMOR, 2, 0);
        lookups.put(key, RuleValue.present(Ability.zero().with(Stat.STR, 50)));
        var catalog = new Catalog(7, "catalog", lookups, Set.of(), List.of(), List.of(), VipTableState.VERIFIED_ABSENT);
        var fresh = new Qualified(before.source(), before.items(), catalog, before.lifetimes());
        var binding = expected(7, 3333, 1);
        var result = reconcile(fresh, binding, Optional.of(stored(binding, 1, List.of(saved(item, true)))));
        assertTrue(result.staleItems().isEmpty());
        assertEquals(Map.of(1L, true), result.overrides());
        assertEquals(50, ModernEquipmentEvaluator.evaluate(fresh, result.overrides(), 0).total(Stat.STR));
        assertEquals(0, ModernEquipmentEvaluator.evaluate(before, result.overrides(), 0).total(Stat.STR));
        // It can construct an impossible candidate; it never claims topology permission.
        var other = armor(2, 101, false);
        var two = qualified(7, 3333, List.of(item, other));
        var both = reconcile(two, binding, Optional.of(stored(binding, 2, List.of(saved(item, true), saved(other, true)))));
        assertTrue(both.desiredLoadout().item(1).equipped()); assertTrue(both.desiredLoadout().item(2).equipped());
        var rejection = EquipmentRules.setEquipped(new EquipmentRules.Actor(EquipmentRules.CharacterClass.KNIGHT, 80),
                both.desiredLoadout(), 1, true, new EquipmentRules.Policy(EquipmentRules.SealedItemPolicy.RETAINED_ALLOW));
        assertInstanceOf(EquipmentRules.Rejection.class, rejection);
    }

    @Test void full512FreshFingerprintsAndResultsAreImmutableAndNamesNeverBecomeIdentity() {
        var items = new ArrayList<Instance>();
        for (int i = 0; i < 512; i++) items.add(armor(0x1_0000_0000L + i, 0x2_0000_0000L + i, false));
        var fresh = qualified(7, 3333, items);
        var binding = expected(7, 3333, 1);
        var result = reconcile(fresh, binding, Optional.empty());
        assertEquals(512, result.sourceFingerprints().size());
        assertEquals(512, result.desiredLoadout().items().size());
        assertThrows(UnsupportedOperationException.class, () -> result.sourceFingerprints().clear());
        assertThrows(UnsupportedOperationException.class, () -> result.overrides().put(1L, true));
        assertThrows(UnsupportedOperationException.class, () -> result.staleItems().add(new StaleItem(1, 1, StaleReason.SOURCE_ITEM_MISSING)));
        assertThrows(UnsupportedOperationException.class, () -> result.desiredLoadout().items().clear());
        var fingerprint = result.sourceFingerprints().values().iterator().next();
        byte[] clone = fingerprint.bytes(); clone[0] ^= 1;
        assertFalse(Arrays.equals(clone, fingerprint.bytes()));
        var snapshot = fresh.source();
        var character = snapshot.character();
        var renamed = new ModernCharacterData.Snapshot(7, new ModernApplicationProtocol.CharacterRow(3333,
                "another display name", character.characterClass(), character.sex(), character.level()), snapshot.persisted(), snapshot.inventory());
        var sameIdentity = new Qualified(renamed, fresh.items(), fresh.catalog(), fresh.lifetimes());
        assertEquals(result.sourceFingerprints(), reconcile(sameIdentity, binding, Optional.empty()).sourceFingerprints());
        assertEquals("EquipmentReconciliationResult[redacted]", result.toString());
        assertEquals("ExpectedEquipmentBinding[redacted]", binding.toString());
        assertEquals("EquipmentFingerprint[redacted]", fingerprint.toString());
    }

    private static void failure(Failure reason, org.junit.jupiter.api.function.Executable action) {
        var error = assertThrows(ReconciliationException.class, action);
        assertEquals(reason, error.failure());
        assertEquals("modern equipment reconciliation: " + reason, error.getMessage());
    }
    private static ExpectedBinding expected(int source, long character, int marker) {
        byte[] digest = new byte[32]; Arrays.fill(digest, (byte) marker);
        return new ExpectedBinding(new TournamentPresence.CharacterKey(source, character),
                new ModernEquipmentOverrideStore.Fingerprint(digest));
    }
    private static ModernEquipmentOverrideStore.Stored stored(ExpectedBinding binding, long revision,
                                                               List<ModernEquipmentOverrideStore.ItemOverride> items) {
        return new ModernEquipmentOverrideStore.Stored(binding.key(), revision,
                new ModernEquipmentOverrideStore.Replacement(binding.fingerprint(), items));
    }
    private static ModernEquipmentOverrideStore.ItemOverride saved(Instance item, boolean desired) {
        return new ModernEquipmentOverrideStore.ItemOverride(item.source().objectId(), item.source().template().catalogId(), fingerprint(item), desired);
    }
    private static List<Long> emptyCards() { return List.of(0L, 0L, 0L, 0L, 0L); }
    private static Instance armor(long object, long catalog, boolean equipped) {
        return instance(object, catalog, equipped, ModernCharacterData.ItemKind.ARMOR);
    }
    private static Instance weapon(long object, long catalog, boolean equipped) {
        return instance(object, catalog, equipped, ModernCharacterData.ItemKind.WEAPON);
    }
    private static Instance etc(long object, long catalog, boolean equipped) {
        return instance(object, catalog, equipped, ModernCharacterData.ItemKind.ETC);
    }
    private static Instance instance(long object, long catalog, boolean equipped, ModernCharacterData.ItemKind kind) {
        var metadata = new ModernCharacterData.ItemTemplate(catalog, kind, "$" + catalog, 1, 2, 3);
        var source = new ModernCharacterData.InventoryItem(object, metadata, 1, equipped, 0, 0, true, 1, false, 0);
        var range = new EquipmentRules.LevelRange(1, Long.MAX_VALUE);
        EquipmentRules.Facts rules = switch (kind) {
            case WEAPON -> new EquipmentRules.WeaponFacts(127, range, false);
            case ARMOR -> new EquipmentRules.ArmorFacts(127, range, EquipmentRules.ArmorSlot.BODY);
            case ETC -> EquipmentRules.NonEquipmentFacts.INSTANCE;
        };
        var template = new Template(metadata, rules, kind == ModernCharacterData.ItemKind.WEAPON ? 1 : kind == ModernCharacterData.ItemKind.ARMOR ? 2 : 0,
                kind == ModernCharacterData.ItemKind.ETC ? 0 : 6, List.of(), Ability.zero(), TemplatePhysical.zero());
        return new Instance(source, template, 0, RuleValue.absent(), emptyCards(), Stone.none());
    }
    private static ModernCharacterData.InventoryItem inventory(ModernCharacterData.InventoryItem original,
            ModernCharacterData.ItemTemplate metadata, long quantity, boolean equipped, long enchantment, long durability,
            boolean identified, long blessing, boolean sealed, long charges) {
        return new ModernCharacterData.InventoryItem(original.objectId(), metadata, quantity, equipped, enchantment, durability, identified, blessing, sealed, charges);
    }
    private static Instance withSource(Instance original, UnaryOperator<ModernCharacterData.InventoryItem> change) {
        var source = change.apply(original.source()); var t = original.template();
        var template = new Template(source.template(), t.rules(), t.retainedType(), t.safeEnchant(), t.setIds(), t.base(), t.physical());
        return new Instance(source, template, original.greater(), original.dark(), original.cards(), original.stone());
    }
    private static Instance withTemplate(Instance original, Template template) {
        return new Instance(original.source(), template, original.greater(), original.dark(), original.cards(), original.stone());
    }
    private static Qualified qualified(int sourceId, long characterId, List<Instance> instances) {
        var sorted = instances.stream().sorted(java.util.Comparator.comparingLong(item -> item.source().objectId())).toList();
        var snapshot = new ModernCharacterData.Snapshot(sourceId,
                new ModernApplicationProtocol.CharacterRow(characterId, "same display name", 1, 0, 80),
                new ModernCharacterData.PersistedValues(100, 50, 90, 40, -10, 18, 18, 18, 18, 18, 18,
                        0, 0, 16, 16, 16, 16, 16, 225), sorted.stream().map(Instance::source).toList());
        Map<Long, Instance> items = new HashMap<>();
        for (var item : sorted) items.put(item.source().objectId(), item);
        var lookups = new HashMap<RuleKey, RuleValue>();
        for (var key : ModernEquipmentFacts.requiredLookups(sorted)) {
            if (key.kind() != RuleKind.VIP) lookups.put(key, RuleValue.absent());
        }
        var catalog = new Catalog(sourceId, "catalog", lookups, ModernEquipmentFacts.requiredSetIds(sorted),
                List.of(), List.of(), VipTableState.VERIFIED_ABSENT);
        return new Qualified(snapshot, items, catalog, ModernItemLifetime.Observation.verifiedPermanent(snapshot));
    }
}
