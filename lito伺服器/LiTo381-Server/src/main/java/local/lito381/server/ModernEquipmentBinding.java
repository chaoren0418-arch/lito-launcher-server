package local.lito381.server;

import static local.lito381.server.ModernEquipmentFacts.*;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Comparator;

/**
 * Pure, versioned equipment provenance digests. No SQL, file access, credential
 * getter, display-name identity, native integer encoding or object serialization.
 * A digest is not an authentication/selection capability or permission to equip.
 */
final class ModernEquipmentBinding {
    enum Failure { INVALID_INPUT, UNAUTHORIZED_SOURCE, CATALOG_REQUIRED }
    static final class BindingException extends IllegalArgumentException {
        private final Failure failure;
        private BindingException(Failure failure) {
            super("modern equipment binding: " + failure);
            this.failure = failure;
        }
        Failure failure() { return failure; }
    }

    /**
     * Content identity of exactly one already-loaded immutable Catalog bundle.
     * NOT a database transaction, MyISAM generation, freshness certificate or
     * proof that separate source/catalog queries observed one atomic instant.
     * Includes explicit successful-lookup absence as well as every loaded value.
     * The caller must still qualify the source's required lookups/set IDs; a
     * hash alone does not prove that an incomplete Catalog was queried fully.
     */
    static final class CatalogGeneration {
        private final ModernEquipmentOverrideStore.Fingerprint fingerprint;
        private CatalogGeneration(ModernEquipmentOverrideStore.Fingerprint fingerprint) {
            this.fingerprint = fingerprint;
        }
        ModernEquipmentOverrideStore.Fingerprint fingerprint() { return fingerprint; }
        @Override public boolean equals(Object other) {
            return other instanceof CatalogGeneration generation && fingerprint.equals(generation.fingerprint);
        }
        @Override public int hashCode() { return fingerprint.hashCode(); }
        @Override public String toString() { return "ModernEquipmentCatalogGeneration[redacted]"; }
    }

    private static final Stat[] SORTED_STATS = sortedStats();
    private static final Comparator<RuleKey> RULE_ORDER = Comparator
            .comparing((RuleKey key) -> key.kind().name())
            .thenComparing(key -> key.itemKind().name())
            .thenComparingLong(RuleKey::first).thenComparingLong(RuleKey::second);
    private ModernEquipmentBinding() {}

    /**
     * V1 stable source configuration identity is the canonical routing descriptor:
     * source ID, normalized numeric host (from strict SourceConfig), port, exact
     * database/catalog identifiers, password scheme and TLS verification mode.
     * Append ONLY Principal.accountForSource(id), never Principal.account() or a
     * character/server display name. Source IDs are explicit operator-owned IDs,
     * not source-list positions. Character object ID remains the separate store key.
     *
     * A process restart/config reorder/password rotation does not change this
     * binding. Repointing the route or changing its canonical owner does. This
     * does NOT distinguish a database restored/replaced in place at the SAME
     * route with the SAME account. There is no inferred incarnation UUID here.
     * Neither passwords, DB login credentials, env names nor trust-store paths
     * enter the digest; no secret getter is called, including for a closed config.
     */
    static ModernEquipmentOverrideStore.Fingerprint ownerBinding(
            ModernAccountAuthentication.Principal principal, ModernServerConfig.SourceConfig source) {
        if (principal == null || source == null) throw failure(Failure.INVALID_INPUT);
        if (!principal.authorizesSource(source.id())) throw failure(Failure.UNAUTHORIZED_SOURCE);
        String catalog = source.catalogDatabase();
        if (catalog == null) throw failure(Failure.CATALOG_REQUIRED);
        var database = source.database();
        var out = new Canonical();
        out.text("LiTo381.ModernEquipment.OwnerSourceBinding"); out.i32(1);
        out.i32(source.id()); out.text(database.host()); out.i32(database.port());
        out.text(database.database()); out.text(catalog);
        out.text(database.passwordScheme()); out.text(database.sslMode());
        out.text(principal.accountForSource(source.id()));
        return out.finish();
    }

    /**
     * V1 canonical layout: length-prefixed exact UTF-8 domain + version i32;
     * source i32/database text/VIP state named tag; sorted lookup keys plus their
     * present flag and complete Ability; sorted verified-set IDs; sorted set
     * rows; sorted combo rows with sorted required-card IDs. Counts are i32,
     * values/IDs i64 big-endian, flags one byte. Ability writes every named Stat
     * in lexical order, never enum ordinal/Map iteration order. No signed i64 is
     * narrowed, clamped, evaluated, normalized or converted to floating point.
     * Model constructors already bound lookup/set/combo/card counts; hashing is
     * streamed and does not allocate a serialized full-catalog byte buffer.
     */
    static CatalogGeneration catalogGeneration(Catalog catalog) {
        if (catalog == null) throw failure(Failure.INVALID_INPUT);
        var out = new Canonical();
        out.text("LiTo381.ModernEquipment.CatalogBundle"); out.i32(1);
        out.i32(catalog.sourceId()); out.text(catalog.resourceDatabase()); out.text(catalog.vipTableState().name());
        out.i32(catalog.lookups().size());
        for (RuleKey key : catalog.lookups().keySet().stream().sorted(RULE_ORDER).toList()) {
            out.text(key.kind().name()); out.text(key.itemKind().name()); out.i64(key.first()); out.i64(key.second());
            RuleValue rule = catalog.lookups().get(key);
            out.flag(rule.present()); ability(out,rule.raw());
        }
        out.i32(catalog.verifiedSetIds().size());
        for (long id : catalog.verifiedSetIds().stream().sorted().toList()) out.i64(id);
        out.i32(catalog.sets().size());
        for (SetRule rule : catalog.sets().stream().sorted(Comparator.comparingLong(SetRule::setId)
                .thenComparingLong(SetRule::pieceCount)).toList()) {
            out.i64(rule.setId()); out.i64(rule.pieceCount()); ability(out,rule.raw());
        }
        out.i32(catalog.combos().size());
        for (ComboRule rule : catalog.combos().stream().sorted(Comparator.comparingLong(ComboRule::comboId)).toList()) {
            out.i64(rule.comboId()); out.i32(rule.requiredCards().size());
            for (long card : rule.requiredCards().stream().sorted().toList()) out.i64(card);
            ability(out,rule.raw());
        }
        return new CatalogGeneration(out.finish());
    }

    private static void ability(Canonical out, Ability ability) {
        out.i32(SORTED_STATS.length);
        for (Stat stat : SORTED_STATS) { out.text(stat.name()); out.i64(ability.get(stat)); }
    }
    private static Stat[] sortedStats() {
        Stat[] fields = Stat.values(); Arrays.sort(fields,Comparator.comparing(Stat::name)); return fields;
    }
    private static BindingException failure(Failure failure) { return new BindingException(failure); }

    /** Exact canonical bytes are ephemeral and never exposed through diagnostics. */
    private static final class Canonical {
        private final MessageDigest digest;
        Canonical() {
            try { digest = MessageDigest.getInstance("SHA-256"); }
            catch (NoSuchAlgorithmException impossible) { throw new IllegalStateException("equipment binding digest unavailable"); }
        }
        void text(String value) {
            byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
            try { i32(bytes.length); digest.update(bytes); }
            finally { Arrays.fill(bytes,(byte)0); }
        }
        void flag(boolean value) { digest.update((byte)(value ? 1 : 0)); }
        void i32(int value) { for (int shift = 24; shift >= 0; shift -= 8) digest.update((byte)(value >>> shift)); }
        void i64(long value) { for (int shift = 56; shift >= 0; shift -= 8) digest.update((byte)(value >>> shift)); }
        ModernEquipmentOverrideStore.Fingerprint finish() {
            return new ModernEquipmentOverrideStore.Fingerprint(digest.digest());
        }
    }
}
