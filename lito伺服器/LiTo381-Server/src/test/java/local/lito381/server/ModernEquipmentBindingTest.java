package local.lito381.server;

import static local.lito381.server.ModernEquipmentBinding.*;
import static local.lito381.server.ModernEquipmentFacts.*;
import static local.lito381.server.ModernEquipmentFacts.Stat.*;
import static org.junit.jupiter.api.Assertions.*;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ModernEquipmentBindingTest {
    @TempDir Path temporary;
    private static final Map<String,String> SECRETS = Map.of(
            "TEST_TLS","synthetic-key-secret", "TEST_USER","synthetic-db-user",
            "TEST_PASSWORD","synthetic-db-password", "TEST_TRUST","synthetic-trust-secret");
    private static final RuleKey ARMOR_KEY = new RuleKey(RuleKind.BLESS_ITEM,ModernCharacterData.ItemKind.ARMOR,4_000_000_001L,2);
    private static final RuleKey CARD_KEY = new RuleKey(RuleKind.CARD,ModernCharacterData.ItemKind.ETC,8_589_934_594L,0);

    @Test void bothVersionOneDigestsMatchIndependentBigEndianUtf8Sha256KnownAnswers() throws Exception {
        // Independently constructed .NET MemoryStream + SHA256 reference, not
        // bytes produced by either Java encoder: owner 128 B; catalog 4066 B,
        // 39 lexical Stat fields per Ability; includes signed i64 MIN/MAX.
        try (var config = parse(base(),SECRETS)) {
            assertEquals("A5C395155C09D65A8FBAA3A899F99074B4341C44D1366B6349BC93C8C08A3D00",
                    hex(ownerBinding(principal("display","sun-owner"),source(config,7))));
        }
        assertEquals("2C92112FED371780ED796DFB151012F2D586DD1A4EB8419D56F3642F5C9FFD0C",
                hex(catalogGeneration(catalog()).fingerprint()));
    }

    @Test void ownerUsesExactSourceGrantNeverDisplayNamesCredentialsOrListPosition() throws Exception {
        var principal = principal("visible-account","sun-owner");
        var renamedPrincipal = new ModernAccountAuthentication.Principal("other-visible-account",
                Set.of(new ModernAccountAuthentication.Source(7,"renamed display")),Map.of(7,"sun-owner"));
        try (var config = parse(base(),SECRETS)) {
            var expected = ownerBinding(principal,source(config,7));
            assertEquals(expected,ownerBinding(renamedPrincipal,source(config,7)));
            assertNotEquals(expected,ownerBinding(principal("visible-account","Sun-owner"),source(config,7)));
            assertNotEquals(ownerBinding(principal("visible-account","\u00c5-owner"),source(config,7)),
                    ownerBinding(principal("visible-account","A\u030a-owner"),source(config,7)),"canonical keys are not Unicode-normalized");
            var changedSecrets = Map.of("TEST_TLS","another-key-secret","TEST_USER","another-db-user",
                    "TEST_PASSWORD","another-db-password","TEST_TRUST","another-trust-secret");
            try (var renamed = parse(base().replace("name=Sun","name=renamed-config-display"),changedSecrets)) {
                assertEquals(expected,ownerBinding(principal,source(renamed,7)));
                source(renamed,7).database().close();
                assertEquals(expected,ownerBinding(principal,source(renamed,7)),
                        "no DB username/password/trust-password getter is read even after secret copies close");
            }
            try (var reordered = parse(base().replace("sources=7","sources=7,3")+sourceText(3,"Moon"),SECRETS)) {
                assertEquals(3,reordered.sources().get(0).id());
                assertEquals(expected,ownerBinding(principal,source(reordered,7)));
            }
            assertEquals("EquipmentFingerprint[redacted]",expected.toString());
            assertFalse(expected.toString().contains("sun-owner"));
        }
    }

    @Test void everyStableRoutingDimensionChangesOwnerBindingButNoSourceUuidIsInvented() throws Exception {
        try (var original = parse(base(),SECRETS)) {
            var expected = ownerBinding(principal("display","sun-owner"),source(original,7));
            for (String variant : List.of(
                    base().replace("mysql.host=127.0.0.1","mysql.host=127.0.0.2"),
                    base().replace("mysql.port=3306","mysql.port=3307"),
                    base().replace("mysql.database=lito_7","mysql.database=lito_other"),
                    base().replace("catalog.database=catalog_a","catalog.database=catalog_b"),
                    base().replace("mysql.tls=disabled","mysql.tls=verify_identity")
                            + "source.7.mysql.trustStore.pkcs12="+portable(temporary.resolve("trust.p12"))
                            + "\nsource.7.mysql.trustStore.password.env=TEST_TRUST\n")) {
                try (var changed = parse(variant,SECRETS)) {
                    assertNotEquals(expected,ownerBinding(principal("display","sun-owner"),source(changed,7)));
                }
            }
            try (var changedId = parse(base().replace("sources=7","sources=42").replace("source.7.","source.42."),SECRETS)) {
                var granted42 = new ModernAccountAuthentication.Principal("display",
                        Set.of(new ModernAccountAuthentication.Source(42,"Sun")),Map.of(42,"sun-owner"));
                assertNotEquals(expected,ownerBinding(granted42,source(changedId,42)));
            }
            // A separate parse with the same descriptor is intentionally the
            // same identity: it cannot detect an in-place DB restore/incarnation.
            try (var restarted = parse(base(),SECRETS)) {
                assertEquals(expected,ownerBinding(principal("display","sun-owner"),source(restarted,7)));
            }
        }
    }

    @Test void unauthorizedAndCataloglessSourcesFailWithStaticRedactedErrors() throws Exception {
        try (var config = parse(base(),SECRETS);
             var catalogless = parse(base().replace("source.7.catalog.database=catalog_a\n",""),SECRETS)) {
            var source = source(config,7);
            var other = new ModernAccountAuthentication.Principal("private-display",
                    Set.of(new ModernAccountAuthentication.Source(42,"other source")),Map.of(42,"private-owner"));
            failure(Failure.UNAUTHORIZED_SOURCE,() -> ownerBinding(other,source));
            failure(Failure.CATALOG_REQUIRED,() -> ownerBinding(principal("display","sun-owner"),source(catalogless,7)));
            failure(Failure.INVALID_INPUT,() -> ownerBinding(null,source));
            failure(Failure.INVALID_INPUT,() -> ownerBinding(principal("display","sun-owner"),null));
            failure(Failure.INVALID_INPUT,() -> catalogGeneration(null));
        }
    }

    @Test void catalogIgnoresContainerIterationOrderButPreservesAllSemanticContents() {
        var original = catalog();
        var entries = new ArrayList<>(original.lookups().entrySet()); Collections.reverse(entries);
        var reversed = new LinkedHashMap<RuleKey,RuleValue>(); entries.forEach(entry -> reversed.put(entry.getKey(),entry.getValue()));
        var setIds = new ArrayList<>(original.verifiedSetIds()); Collections.reverse(setIds);
        var sets = new ArrayList<>(original.sets()); Collections.reverse(sets);
        var cards = new ArrayList<>(original.combos().get(0).requiredCards()); Collections.reverse(cards);
        var reordered = new Catalog(original.sourceId(),original.resourceDatabase(),reversed,new LinkedHashSet<>(setIds),sets,
                List.of(new ComboRule(3333,new LinkedHashSet<>(cards),original.combos().get(0).raw())),original.vipTableState());
        var generation = catalogGeneration(original);
        assertEquals(generation,catalogGeneration(reordered));
        assertEquals(generation.hashCode(),catalogGeneration(reordered).hashCode());
        assertEquals("ModernEquipmentCatalogGeneration[redacted]",generation.toString());
        assertNotEquals(generation,generation.fingerprint()); assertNotEquals(generation,null);
        var bytes = generation.fingerprint().bytes(); bytes[0] ^= 1;
        assertEquals(generation,catalogGeneration(original),"returned digest bytes are a defensive copy");
        reversed.clear(); setIds.clear(); sets.clear(); cards.clear();
        assertEquals(generation,catalogGeneration(reordered),"Catalog owns immutable copies of every input container");
    }

    @Test void explicitAbsenceUnqueriedKeysAndPresentZeroAreDifferentCatalogGenerations() {
        var original = catalog(); var expected = catalogGeneration(original);
        var noLookup = new LinkedHashMap<>(original.lookups()); noLookup.remove(CARD_KEY);
        assertNotEquals(expected,catalogGeneration(replace(original,noLookup,original.verifiedSetIds(),original.sets(),original.combos())));
        var presentZero = new LinkedHashMap<>(original.lookups()); presentZero.put(CARD_KEY,RuleValue.present(Ability.zero()));
        assertNotEquals(expected,catalogGeneration(replace(original,presentZero,original.verifiedSetIds(),original.sets(),original.combos())));
        assertNotEquals(expected,catalogGeneration(new Catalog(42,original.resourceDatabase(),original.lookups(),original.verifiedSetIds(),
                original.sets(),original.combos(),original.vipTableState())));
        assertNotEquals(expected,catalogGeneration(new Catalog(7,"catalog_b",original.lookups(),original.verifiedSetIds(),
                original.sets(),original.combos(),original.vipTableState())));
        assertNotEquals(expected,catalogGeneration(new Catalog(7,original.resourceDatabase(),original.lookups(),original.verifiedSetIds(),
                original.sets(),original.combos(),VipTableState.VERIFIED_ABSENT)));
        var extraCheckedSet = new LinkedHashSet<>(original.verifiedSetIds()); extraCheckedSet.add(2L);
        assertNotEquals(expected,catalogGeneration(replace(original,original.lookups(),extraCheckedSet,original.sets(),original.combos())));
    }

    @Test void allLookupKeyPartsSetCountsAndComboRequirementsParticipateInTheGeneration() {
        var original = catalog(); var expected = catalogGeneration(original);
        for (RuleKey changedKey : List.of(
                new RuleKey(RuleKind.ENCHANT_ITEM,ARMOR_KEY.itemKind(),ARMOR_KEY.first(),ARMOR_KEY.second()),
                new RuleKey(ARMOR_KEY.kind(),ModernCharacterData.ItemKind.WEAPON,ARMOR_KEY.first(),ARMOR_KEY.second()),
                new RuleKey(ARMOR_KEY.kind(),ARMOR_KEY.itemKind(),ARMOR_KEY.first()+1,ARMOR_KEY.second()),
                new RuleKey(ARMOR_KEY.kind(),ARMOR_KEY.itemKind(),ARMOR_KEY.first(),ARMOR_KEY.second()+1))) {
            var lookups = new LinkedHashMap<>(original.lookups()); var value = lookups.remove(ARMOR_KEY); lookups.put(changedKey,value);
            assertNotEquals(expected,catalogGeneration(replace(original,lookups,original.verifiedSetIds(),original.sets(),original.combos())));
        }
        for (SetRule changed : List.of(new SetRule(1,3,original.sets().get(0).raw()),
                new SetRule(Long.MAX_VALUE,2,original.sets().get(0).raw()))) {
            assertNotEquals(expected,catalogGeneration(replace(original,original.lookups(),original.verifiedSetIds(),
                    List.of(changed,original.sets().get(1)),original.combos())));
        }
        assertNotEquals(expected,catalogGeneration(replace(original,original.lookups(),original.verifiedSetIds(),
                List.of(original.sets().get(1)),original.combos())));
        var combo = original.combos().get(0);
        for (ComboRule changed : List.of(new ComboRule(combo.comboId()+1,combo.requiredCards(),combo.raw()),
                new ComboRule(combo.comboId(),Set.of(4_294_967_297L),combo.raw()),
                new ComboRule(combo.comboId(),Set.of(4_294_967_297L,Long.MAX_VALUE,42L),combo.raw()))) {
            assertNotEquals(expected,catalogGeneration(replace(original,original.lookups(),original.verifiedSetIds(),original.sets(),List.of(changed))));
        }
        assertNotEquals(expected,catalogGeneration(replace(original,original.lookups(),original.verifiedSetIds(),original.sets(),List.of())));
    }

    @Test void everyI64AbilityFieldInLookupSetAndComboIsIncludedWithoutNarrowingOrEvaluation() {
        var original = catalog(); var expected = catalogGeneration(original);
        for (Stat stat : Stat.values()) {
            var lookups = new LinkedHashMap<>(original.lookups()); var raw = lookups.get(ARMOR_KEY).raw();
            lookups.put(ARMOR_KEY,RuleValue.present(raw.with(stat,raw.get(stat)^1L)));
            assertNotEquals(expected,catalogGeneration(replace(original,lookups,original.verifiedSetIds(),original.sets(),original.combos())),stat.name());
            var set = original.sets().get(0); var changedSet = new SetRule(set.setId(),set.pieceCount(),set.raw().with(stat,set.raw().get(stat)^1L));
            assertNotEquals(expected,catalogGeneration(replace(original,original.lookups(),original.verifiedSetIds(),
                    List.of(changedSet,original.sets().get(1)),original.combos())),stat.name());
            var combo = original.combos().get(0);
            var changedCombo = new ComboRule(combo.comboId(),combo.requiredCards(),combo.raw().with(stat,combo.raw().get(stat)^1L));
            assertNotEquals(expected,catalogGeneration(replace(original,original.lookups(),original.verifiedSetIds(),original.sets(),List.of(changedCombo))),stat.name());
        }
        // Catalog identity hashes exact facts; evaluating this synthetic AC MIN
        // is not required and must not cause a negate overflow in the hasher.
        assertEquals(Long.MIN_VALUE,original.lookups().get(ARMOR_KEY).raw().get(AC));
        assertEquals(Long.MAX_VALUE,original.lookups().get(ARMOR_KEY).raw().get(STR));
    }

    private String base() {
        return "schema=1\nbind.port=0\ntls.pkcs12="+portable(temporary.resolve("server.p12"))
                +"\ntls.password.env=TEST_TLS\nsources=7\n"+sourceText(7,"Sun");
    }
    private static String sourceText(int source,String name) {
        String prefix = "source."+source+".";
        return prefix+"name="+name+"\n"+prefix+"mysql.host=127.0.0.1\n"+prefix+"mysql.port=3306\n"
                +prefix+"mysql.database=lito_"+source+"\n"+prefix+"mysql.username.env=TEST_USER\n"
                +prefix+"mysql.password.env=TEST_PASSWORD\n"+prefix+"mysql.passwordScheme=plain\n"
                +prefix+"mysql.tls=disabled\n"+prefix+"catalog.database=catalog_a\n";
    }
    private static String portable(Path path) { return path.toAbsolutePath().toString().replace('\\','/'); }
    private static ModernServerConfig parse(String config,Map<String,String> environment) throws Exception {
        return ModernServerConfig.parse(config.getBytes(StandardCharsets.UTF_8),environment::get);
    }
    private static ModernServerConfig.SourceConfig source(ModernServerConfig config,int source) {
        return config.sources().stream().filter(value -> value.id()==source).findFirst().orElseThrow();
    }
    private static ModernAccountAuthentication.Principal principal(String display,String canonical) {
        return new ModernAccountAuthentication.Principal(display,Set.of(new ModernAccountAuthentication.Source(7,"Sun")),Map.of(7,canonical));
    }
    private static Catalog catalog() {
        return new Catalog(7,"catalog_a",Map.of(
                ARMOR_KEY,RuleValue.present(Ability.zero().with(AC,Long.MIN_VALUE).with(STR,Long.MAX_VALUE).with(MAX_HP,4_294_967_299L)),
                CARD_KEY,RuleValue.absent()),Set.of(1L,Long.MAX_VALUE),
                List.of(new SetRule(1,2,Ability.zero().with(AC,-3).with(STR,4)),
                        new SetRule(Long.MAX_VALUE,Long.MAX_VALUE,Ability.zero().with(MAX_HP,5))),
                List.of(new ComboRule(3333,Set.of(4_294_967_297L,Long.MAX_VALUE),Ability.zero().with(MAGIC_HIT,-7).with(SP,17))),
                VipTableState.VERIFIED_PRESENT);
    }
    private static Catalog replace(Catalog original,Map<RuleKey,RuleValue> lookups,Set<Long> verifiedSets,List<SetRule> sets,List<ComboRule> combos) {
        return new Catalog(original.sourceId(),original.resourceDatabase(),lookups,verifiedSets,sets,combos,original.vipTableState());
    }
    private static String hex(ModernEquipmentOverrideStore.Fingerprint value) {
        return HexFormat.of().withUpperCase().formatHex(value.bytes());
    }
    private static void failure(Failure expected,Runnable action) {
        var error = assertThrows(BindingException.class,action::run);
        assertEquals(expected,error.failure()); assertEquals("modern equipment binding: "+expected,error.getMessage());
        assertNull(error.getCause()); assertEquals(0,error.getSuppressed().length);
        for (String secret : SECRETS.values()) assertFalse(error.toString().contains(secret));
        assertFalse(error.toString().contains("private-owner"));
    }
}
