package local.lito381.server;

import static local.lito381.server.ModernEquipmentFacts.*;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Pure reconciliation of fresh, qualified source instances with modern overrides.
 * There is no database/disk access, rebind, deletion, authentication or commit here.
 * A matching fingerprint is provenance, NOT permission under today's equipment rules.
 * The authority must re-evaluate the whole fresh candidate before publishing/commit.
 * Catalog lookup/set/combo values are deliberately re-evaluated from the fresh Catalog,
 * not frozen into an instance fingerprint; this does not invent an atomic SQL generation.
 */
final class ModernEquipmentReconciliation {
    enum Failure { INVALID_INPUT, SOURCE_KEY_MISMATCH, STORED_KEY_MISMATCH, BINDING_MISMATCH }

    static final class ReconciliationException extends IllegalArgumentException {
        private final Failure failure;
        ReconciliationException(Failure failure) {
            super("modern equipment reconciliation: " + failure);
            this.failure = failure;
        }
        Failure failure() { return failure; }
    }

    /** Caller qualifies owner and exact source configuration into this opaque binding. */
    record ExpectedBinding(TournamentPresence.CharacterKey key,
                           ModernEquipmentOverrideStore.Fingerprint fingerprint) {
        ExpectedBinding {
            if (key == null || fingerprint == null) throw error(Failure.INVALID_INPUT);
        }
        @Override public String toString() { return "ExpectedEquipmentBinding[redacted]"; }
    }

    enum StaleReason { SOURCE_ITEM_MISSING, SOURCE_CATALOG_CHANGED, SOURCE_FACTS_CHANGED }
    /** catalogId is the previous stored catalog, useful even when the item is now absent. */
    record StaleItem(long itemObjectId, long catalogId, StaleReason reason) {
        StaleItem {
            if (itemObjectId <= 0 || catalogId <= 0 || reason == null) throw error(Failure.INVALID_INPUT);
        }
    }

    /**
     * Valid overrides only; stale entries are reported, not applied or implicitly removed
     * from storage. expected retains the original revision/binding, including all stale
     * rows, so an authority's eventual explicit replacement still uses correct CAS.
     */
    static final class Result {
        private final ModernEquipmentOverrideStore.Expected expected;
        private final Map<Long, Boolean> overrides;
        private final EquipmentRules.Loadout desiredLoadout;
        private final List<StaleItem> staleItems;
        private final Map<Long, ModernEquipmentOverrideStore.Fingerprint> sourceFingerprints;

        private Result(ModernEquipmentOverrideStore.Expected expected, Map<Long, Boolean> overrides,
                       EquipmentRules.Loadout desiredLoadout, List<StaleItem> staleItems,
                       Map<Long, ModernEquipmentOverrideStore.Fingerprint> sourceFingerprints) {
            this.expected = expected;
            this.overrides = immutableOrdered(overrides);
            this.desiredLoadout = desiredLoadout;
            this.staleItems = List.copyOf(staleItems);
            this.sourceFingerprints = immutableOrdered(sourceFingerprints);
        }
        ModernEquipmentOverrideStore.Expected expected() { return expected; }
        Map<Long, Boolean> overrides() { return overrides; }
        EquipmentRules.Loadout desiredLoadout() { return desiredLoadout; }
        List<StaleItem> staleItems() { return staleItems; }
        Map<Long, ModernEquipmentOverrideStore.Fingerprint> sourceFingerprints() { return sourceFingerprints; }
        @Override public String toString() { return "EquipmentReconciliationResult[redacted]"; }
    }

    private ModernEquipmentReconciliation() {}

    static Result reconcile(Qualified fresh, ExpectedBinding expected,
                            Optional<ModernEquipmentOverrideStore.Stored> stored) {
        if (fresh == null || expected == null || stored == null) throw error(Failure.INVALID_INPUT);
        return reconcile(fresh, expected, stored, fresh.lifetimes().queryStartedEpochMillis());
    }
    static Result reconcile(Qualified fresh, ExpectedBinding expected,
                            Optional<ModernEquipmentOverrideStore.Stored> stored, long nowEpochMillis) {
        if (fresh == null || expected == null || stored == null) throw error(Failure.INVALID_INPUT);
        var sourceKey = new TournamentPresence.CharacterKey(fresh.source().sourceId(),
                fresh.source().character().objectId());
        if (!sourceKey.equals(expected.key())) throw error(Failure.SOURCE_KEY_MISMATCH);
        if (stored.isPresent()) {
            var value = stored.get();
            if (!sourceKey.equals(value.key())) throw error(Failure.STORED_KEY_MISMATCH);
            if (!expected.fingerprint().equals(value.value().binding())) throw error(Failure.BINDING_MISMATCH);
        }

        var fingerprints = new LinkedHashMap<Long, ModernEquipmentOverrideStore.Fingerprint>();
        for (var item : fresh.source().inventory()) {
            fingerprints.put(item.objectId(), fingerprint(fresh.items().get(item.objectId()), fresh.lifetimes().items().get(item.objectId())));
        }
        var overrides = new LinkedHashMap<Long, Boolean>();
        var stale = new ArrayList<StaleItem>();
        if (stored.isPresent()) {
            // Store.Replacement has already bounded/sorted/uniqued these numeric IDs.
            for (var previous : stored.get().value().overrides()) {
                Instance current = fresh.items().get(previous.itemObjectId());
                StaleReason reason = null;
                if (current == null) reason = StaleReason.SOURCE_ITEM_MISSING;
                else if (current.source().template().catalogId() != previous.catalogId()) {
                    reason = StaleReason.SOURCE_CATALOG_CHANGED;
                } else if (!previous.sourceItem().equals(fingerprints.get(previous.itemObjectId()))) {
                    reason = StaleReason.SOURCE_FACTS_CHANGED;
                }
                if (reason == null) overrides.put(previous.itemObjectId(), previous.equipped());
                else stale.add(new StaleItem(previous.itemObjectId(), previous.catalogId(), reason));
            }
        }
        var desired = new ArrayList<EquipmentRules.Item>();
        for (var item : fresh.ruleLoadout().items()) {
            // Keep the raw SQL flag and stale rows intact. An expired/unknown
            // override must not fall back to source equipped=true.
            if (!fresh.lifetimes().view(item.objectId(), nowEpochMillis).usable()) overrides.put(item.objectId(), false);
            desired.add(item.withEquipped(overrides.getOrDefault(item.objectId(), item.equipped())));
        }
        return new Result(stored.map(ModernEquipmentOverrideStore.Expected::of)
                .orElseGet(ModernEquipmentOverrideStore.Expected::absent), overrides,
                new EquipmentRules.Loadout(desired), stale, fingerprints);
    }

    /**
     * V1 canonical SHA-256 encoding, streamed without object serialization/toString.
     * Domain/version and string/count lengths are explicit; integers are big-endian.
     * All source i64 values, including negative enchant/AC, keep their full bit pattern.
     * enum kinds use named tags, abilities use sorted Stat names, not Map iteration.
     * Ordered set-ID tokens KEEP duplicates/order, and all five card slots KEEP order.
     * Source equipped/identified/sealed flags are included: a SQL change never silently
     * becomes the baseline for applying an earlier saved desired flag.
     *
     * Text is exact UTF-8 metadata, not a character/account key and not normalized.
     * Owner/source configuration are a separate authority-qualified binding.
     */
    static ModernEquipmentOverrideStore.Fingerprint fingerprint(Instance item) {
        return fingerprint(item, null);
    }
    static ModernEquipmentOverrideStore.Fingerprint fingerprint(Instance item, ModernItemLifetime.Fact lifetime) {
        if (item == null) throw error(Failure.INVALID_INPUT);
        if (lifetime != null && (lifetime.itemObjectId() != item.source().objectId()
                || lifetime.catalogId() != item.source().template().catalogId()
                || lifetime.kind() != item.source().template().kind()
                || lifetime.sourceEquipped() != item.source().equipped())) throw error(Failure.INVALID_INPUT);
        Canonical out = new Canonical();
        out.text("LiTo381.ModernEquipment.SourceInstance");
        // Preserve v1 only for positively verified permanent items. A former
        // v1 override for a timed/unknown item becomes stale, not permission.
        boolean timed = lifetime != null && !lifetime.verifiedPermanent();
        out.i32(timed ? 2 : 1);
        var source = item.source();
        out.i64(source.objectId());
        metadata(out, source.template());
        out.i64(source.quantity());
        out.flag(source.equipped());
        out.i64(source.enchantment());
        out.i64(source.durability());
        out.flag(source.identified());
        out.i64(source.blessing());
        out.flag(source.sealed());
        out.i64(source.chargeCount());

        var template = item.template();
        // Instance constructor requires source.template == template.metadata by value.
        permission(out, template.rules());
        out.i64(template.retainedType());
        out.i64(template.safeEnchant());
        out.i32(template.setIds().size());
        for (long id : template.setIds()) out.i64(id);
        ability(out, template.base());
        var physical = template.physical();
        out.i64(physical.meleeHit()); out.i64(physical.meleeDamage());
        out.i64(physical.rangedHit()); out.i64(physical.rangedDamage());
        out.i64(physical.damageReduction()); out.i64(physical.weightReduction());
        out.i64(item.greater());
        out.flag(item.dark().present());
        ability(out, item.dark().raw());
        out.i32(item.cards().size());
        for (long card : item.cards()) out.i64(card);
        out.i64(item.stone().itemId());
        out.i64(item.stone().tier());
        out.flag(item.stone().expiresAtEpochMillis().isPresent());
        if (item.stone().expiresAtEpochMillis().isPresent()) out.i64(item.stone().expiresAtEpochMillis().getAsLong());
        if (timed) {
            out.flag(lifetime.remainingSeconds().isPresent());
            if (lifetime.remainingSeconds().isPresent()) out.i64(lifetime.remainingSeconds().getAsLong());
            out.flag(lifetime.timeRulePresent()); out.flag(lifetime.timeRuleHours().isPresent());
            if (lifetime.timeRuleHours().isPresent()) out.i64(lifetime.timeRuleHours().getAsLong());
            out.flag(lifetime.absoluteLocalTime().isPresent());
            if (lifetime.absoluteLocalTime().isPresent()) out.text(lifetime.absoluteLocalTime().get());
            out.i64(lifetime.templateMaxUseSeconds()); out.flag(lifetime.lighting());
        }
        return out.finish();
    }

    private static void metadata(Canonical out, ModernCharacterData.ItemTemplate value) {
        out.i64(value.catalogId());
        out.text(value.kind().name());
        out.text(value.nameToken());
        out.i64(value.inventoryGraphic()); out.i64(value.groundGraphic()); out.i64(value.unitWeight());
    }
    private static void permission(Canonical out, EquipmentRules.Facts value) {
        if (value instanceof EquipmentRules.WeaponFacts weapon) {
            out.text("WEAPON"); out.i32(weapon.classMask());
            out.i64(weapon.levelRange().minimum()); out.i64(weapon.levelRange().maximum());
            out.flag(weapon.twoHanded());
        } else if (value instanceof EquipmentRules.ArmorFacts armor) {
            out.text("ARMOR"); out.i32(armor.classMask());
            out.i64(armor.levelRange().minimum()); out.i64(armor.levelRange().maximum());
            out.text(armor.slot().name()); out.i32(armor.slot().retainedType());
        } else if (value == EquipmentRules.NonEquipmentFacts.INSTANCE) {
            out.text("QUALIFIED_NON_EQUIPMENT");
        } else {
            // Future rule variants must not accidentally share an old digest format.
            throw error(Failure.INVALID_INPUT);
        }
    }
    private static void ability(Canonical out, Ability value) {
        Stat[] fields = Stat.values();
        Arrays.sort(fields, Comparator.comparing(Stat::name));
        out.i32(fields.length);
        for (Stat field : fields) { out.text(field.name()); out.i64(value.get(field)); }
    }
    private static <T> Map<Long, T> immutableOrdered(Map<Long, T> input) {
        return Collections.unmodifiableMap(new LinkedHashMap<>(input));
    }
    private static ReconciliationException error(Failure failure) { return new ReconciliationException(failure); }

    /** Fixed-width streaming encoder; never writes a file or exposes its input buffer. */
    private static final class Canonical {
        private final MessageDigest digest;
        Canonical() {
            try { digest = MessageDigest.getInstance("SHA-256"); }
            catch (NoSuchAlgorithmException impossible) {
                throw new IllegalStateException("equipment fingerprint algorithm unavailable");
            }
        }
        void text(String value) {
            byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
            i32(bytes.length); digest.update(bytes);
        }
        void flag(boolean value) { digest.update((byte) (value ? 1 : 0)); }
        void i32(int value) {
            for (int shift = 24; shift >= 0; shift -= 8) digest.update((byte) (value >>> shift));
        }
        void i64(long value) {
            for (int shift = 56; shift >= 0; shift -= 8) digest.update((byte) (value >>> shift));
        }
        ModernEquipmentOverrideStore.Fingerprint finish() {
            return new ModernEquipmentOverrideStore.Fingerprint(digest.digest());
        }
    }
}
