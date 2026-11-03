package local.lito381.server;

import static local.lito381.server.ModernEquipmentFacts.*;
import static local.lito381.server.ModernEquipmentFacts.Stat.*;
import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

/** Synthetic JDBC only. No real database, listener, native process or source writes. */
class ModernEquipmentSourceJdbcTest {
    private static final String[] CLASS_COLUMNS = {"use_royal","use_knight","use_elf","use_mage","use_darkelf","use_dragonknight","use_illusionist"};

    @Test void sourceReadCarriesRealLifetimeObservationAndQualifyPreservesIt() throws Exception {
        var row=item(1,"a","armor");var jdbc=new Jdbc(List.of(row));var source=read(jdbc).orElseThrow();
        assertEquals(3,jdbc.prepares);assertNotNull(jdbc.lifetimeJdbc);
        assertEquals(Map.of(1,3333L,2,"owner"),jdbc.lifetimeJdbc.parameters);
        source.lifetimes().validateSource(source.snapshot());
        assertEquals(ModernItemLifetime.State.PERMANENT,source.lifetimes().view(1,System.currentTimeMillis()).state());
        var catalog=new Catalog(7,"catalog",ModernEquipmentEvaluatorTest.defaults(source.instances(),false),
                requiredSetIds(source.instances()),List.of(),List.of(),VipTableState.VERIFIED_ABSENT);
        assertSame(source.lifetimes(),source.qualify(catalog).lifetimes());jdbc.assertClosed();
    }

    @Test void oldSourceConstructorKeepsLifetimeUnqualifiedInsteadOfInventingPermanence() throws Exception {
        var jdbc=new Jdbc(List.of(item(1,"a","armor")));var source=read(jdbc).orElseThrow();
        var old=new ModernEquipmentSourceJdbc.Source("catalog",source.snapshot(),source.instances());
        assertEquals(ModernItemLifetime.State.UNQUALIFIED,old.lifetimes().view(1,System.currentTimeMillis()).state());
        var other=new ModernCharacterData.Snapshot(42,source.snapshot().character(),source.snapshot().persisted(),source.snapshot().inventory());
        assertThrows(IllegalArgumentException.class,()->new ModernEquipmentSourceJdbc.Source("catalog",source.snapshot(),source.instances(),
                ModernItemLifetime.Observation.verifiedPermanent(other)));jdbc.assertClosed();
    }

    @Test void expiredEquippedAndNullableRemainingArePreservedAsFactsWithoutDeletingSourceItems() throws Exception {
        var expired=item(1,"e",null);expired.put("is_equipped",1L);expired.put("lifetime_deadline","2020-01-01 00:00:00");
        expired.put("lifetime_rule",1L);expired.put("lifetime_hours",720L);
        var unknown=item(2,"a","armor");unknown.put("lifetime_remaining",null);
        var jdbc=new Jdbc(List.of(expired,unknown));var source=read(jdbc).orElseThrow();
        assertEquals(2,source.snapshot().inventory().size());assertTrue(source.snapshot().inventory().get(0).equipped());
        assertEquals(ModernItemLifetime.State.EXPIRED,source.lifetimes().view(1,System.currentTimeMillis()).state());
        assertEquals(ModernItemLifetime.State.UNQUALIFIED,source.lifetimes().view(2,System.currentTimeMillis()).state());jdbc.assertClosed();
    }

    @Test void lifetimeTableOrCloseFailureCannotPublishPreviouslyDecodedSourceAndCollector() {
        var query=new Jdbc(List.of(item(1,"a","armor")));query.lifetimeExecuteFailure=true;
        assertThrows(SQLException.class,()->read(query));assertTrue(query.statementClosed);assertTrue(query.resultClosed);
        assertTrue(query.lifetimeJdbc.statementClosed);assertFalse(query.lifetimeJdbc.resultClosed);assertFalse(query.connectionClosed);
        var cleanup=new Jdbc(List.of(item(1,"a","armor")));cleanup.lifetimeCloseFailure=true;
        assertThrows(SQLException.class,()->read(cleanup));cleanup.assertClosed();
    }

    @Test void lifetimeMismatchAndLateCancellationRejectTheEntireSourceBundle() {
        var row=item(1,"a","armor");var jdbc=new Jdbc(List.of(row));jdbc.lifetimeRows=lifetimeProjection(List.of(row));
        jdbc.lifetimeRows.get(0).put("catalog_id",999L);assertThrows(SQLException.class,()->read(jdbc));jdbc.assertClosed();
        var cancelled=new Jdbc(List.of(row));cancelled.lifetimeCancelAtRow=1;
        var failure=assertThrows(ModernBackendExecutor.BackendException.class,()->read(cancelled));
        assertEquals(ModernBackendExecutor.Failure.CANCELLED,failure.failure());cancelled.assertClosed();
    }

    @Test void oneStatementProducesExactSnapshotAndQualifiedI64InstancesWithoutClamping() throws Exception {
        var row = item(Long.MAX_VALUE,"a","armor");
        row.put("objid",Long.MAX_VALUE); row.put("catalog_id",Long.MAX_VALUE); row.put("a_id",Long.MAX_VALUE);
        row.put("greater",Long.MAX_VALUE); row.put("enchantlvl",Long.MIN_VALUE); row.put("a_safenchant",Long.MIN_VALUE);
        row.put("a_base_STR",Long.MAX_VALUE); row.put("a_base_AC",Long.MIN_VALUE);
        row.put("a_physical_reduction",Long.MAX_VALUE); row.put("a_physical_weight",-9999999999L);
        row.put("dark_owner",Long.MAX_VALUE); row.put("dark_MAX_HP",Long.MAX_VALUE); row.put("dark_AC",-9999999999L);
        row.put("card_owner",Long.MAX_VALUE); row.put("card_1",Long.MAX_VALUE);
        row.put("magic_stone_item_id",Long.MAX_VALUE); row.put("magic_stone_level",Long.MAX_VALUE);
        row.put("magic_stone_time",Timestamp.from(java.time.Instant.ofEpochMilli(4_000_000_000_000L)));
        Jdbc jdbc = new Jdbc(List.of(row));
        String account = "owner' OR '1'='1";
        var source = scoped(() -> ModernEquipmentSourceJdbc.read(jdbc.connection(),ModernBackendScope.current(),7,account,Long.MAX_VALUE,"catalog")).orElseThrow();
        assertEquals(7,source.snapshot().sourceId()); assertEquals(Long.MAX_VALUE,source.snapshot().character().objectId());
        assertEquals(Long.MAX_VALUE,source.snapshot().character().level());
        assertEquals(100,source.snapshot().persisted().maxHp()); assertEquals(Long.MAX_VALUE,source.snapshot().persisted().savedHp());
        assertEquals(-999,source.snapshot().persisted().armorClass()); assertEquals(225,source.snapshot().persisted().food225());
        assertEquals(101,source.snapshot().persisted().strength()); assertEquals(11,source.snapshot().persisted().originalStrength());
        var instance = source.instances().get(0);
        assertEquals(Long.MAX_VALUE,instance.source().objectId()); assertEquals(Long.MAX_VALUE,instance.source().template().catalogId());
        assertEquals(Long.MIN_VALUE,instance.source().enchantment()); assertEquals(Long.MIN_VALUE,instance.template().safeEnchant());
        assertEquals(Long.MAX_VALUE,instance.greater()); assertEquals(Long.MAX_VALUE,instance.template().base().get(STR));
        assertEquals(Long.MIN_VALUE,instance.template().base().get(AC));
        assertEquals(Long.MAX_VALUE,instance.template().physical().damageReduction());
        assertEquals(-9999999999L,instance.template().physical().weightReduction());
        assertTrue(instance.dark().present()); assertEquals(Long.MAX_VALUE,instance.dark().raw().get(MAX_HP));
        assertEquals(-9999999999L,instance.dark().raw().get(AC)); assertEquals(Long.MAX_VALUE,instance.cards().get(0).longValue());
        assertEquals(Long.MAX_VALUE,instance.stone().itemId()); assertEquals(Long.MAX_VALUE,instance.stone().tier());
        assertEquals(4_000_000_000_000L,instance.stone().expiresAtEpochMillis().orElseThrow());
        assertEquals(ModernEquipmentSourceJdbc.sql("catalog"),jdbc.sql); assertEquals(3,jdbc.prepares); assertEquals(3,jdbc.executions);
        assertEquals(1,jdbc.dataPrepares);assertEquals(1,jdbc.metadataPrepares);
        assertEquals(Map.of(1,Long.MAX_VALUE,2,account),jdbc.parameters); assertFalse(jdbc.sql.contains(account));
        assertEquals(2,jdbc.sql.chars().filter(ch -> ch=='?').count()); assertEquals(513,jdbc.maxRows);
        assertTrue(jdbc.sql.endsWith("ORDER BY i.id LIMIT 513")); assertFalse(jdbc.sql.contains("COALESCE"));
        assertTrue(jdbc.sql.contains("ic.item_objid=i.id")); assertTrue(jdbc.sql.contains("d.item_objid=i.id"));
        jdbc.assertClosed();
    }

    @Test void integralDecimalExtensionsAndBaseFieldsRemainExactWhileApproximateAndFractionalValuesFail() throws Exception {
        var row=item(1,"a","armor");row.put("dark_owner",1L);row.put("card_owner",1L);
        row.put("a_base_STR",Long.MAX_VALUE);row.put("a_base_AC",Long.MIN_VALUE);row.put("greater",4_294_967_299L);
        row.replaceAll((column,value)->value instanceof Number number?new BigDecimal(number.toString()).setScale(2):value);
        row.put("card_1",BigInteger.valueOf(Long.MAX_VALUE));
        Jdbc valid=new Jdbc(List.of(row));valid.numericTypes.put("is_sealed",java.sql.Types.BOOLEAN);
        var item=read(valid).orElseThrow().instances().get(0);
        assertEquals(Long.MAX_VALUE,item.template().base().get(STR));assertEquals(Long.MIN_VALUE,item.template().base().get(AC));
        assertEquals(4_294_967_299L,item.greater());assertEquals(Long.MAX_VALUE,item.cards().get(0).longValue());valid.assertClosed();
        for(String column:List.of("greater","card_owner","dark_owner","card_1","magic_stone_item_id","magic_stone_level",
                "dark_STR","a_base_STR","a_physical_reduction","a_use_royal","a_set_ids_bytes")) {
            for(Object value:List.of(new BigDecimal("1.25"),new BigInteger("18446744073709551615"),Double.valueOf(1),Float.valueOf(1),"1")) {
                var invalid=item(1,"a","armor");invalid.put("dark_owner",1L);invalid.put("card_owner",1L);invalid.put(column,value);
                rejected(List.of(invalid));
            }
        }
    }

    @Test void numericMetadataLengthsAndPresentOptionalWeaponValuesMustAlsoBeExact() {
        Jdbc metadata=new Jdbc(List.of(item(1,"w","sword")));
        metadata.metadata.get(0).put("COLUMN_NAME_BYTES",new BigDecimal("1.1"));
        assertThrows(SQLException.class,()->read(metadata));metadata.assertMetadataClosed();assertEquals(0,metadata.dataPrepares);
        for(Object value:List.of(new BigDecimal("0.1"),new BigDecimal("-9223372036854775809"),Double.valueOf(0),"0")) {
            var row=item(1,"w","sword");row.put("w_base_BLIND",value);rejected(List.of(row));
        }
    }

    @Test void sevenClassFlagsUseCharacterTypeOrderAndKatanaRestrictionsAreRealFacts() throws Exception {
        for (int type = 0; type < 7; type++) {
            var row = item(1,"w","sword");
            for (String column : CLASS_COLUMNS) row.put("w_"+column,0L);
            for (long enabled : new long[] {1,2,Long.MAX_VALUE}) {
                row.put("w_"+CLASS_COLUMNS[type],enabled);
                var source = read(new Jdbc(List.of(row))).orElseThrow();
                assertEquals(1<<type,((EquipmentRules.WeaponFacts)source.instances().get(0).template().rules()).classMask());
            }
        }
        // Catalog weapon 41: royal, knight, elf, dark elf, dragon knight; NOT mage/illusionist.
        var katana = item(1,"w","sword"); katana.put("catalog_id",41L); katana.put("w_id",41L);
        katana.put("w_use_mage",0L); katana.put("w_use_illusionist",0L);
        var facts = read(new Jdbc(List.of(katana))).orElseThrow().instances().get(0);
        assertEquals(0x37,((EquipmentRules.WeaponFacts)facts.template().rules()).classMask());
        var loadout = new EquipmentRules.Loadout(List.of(facts.ruleItem().withEquipped(false)));
        var policy = new EquipmentRules.Policy(EquipmentRules.SealedItemPolicy.RETAINED_ALLOW);
        assertInstanceOf(EquipmentRules.Transition.class,EquipmentRules.setEquipped(new EquipmentRules.Actor(EquipmentRules.CharacterClass.KNIGHT,50),loadout,1,true,policy));
        var denied = assertInstanceOf(EquipmentRules.Rejection.class,EquipmentRules.setEquipped(new EquipmentRules.Actor(EquipmentRules.CharacterClass.WIZARD,50),loadout,1,true,policy));
        assertEquals(EquipmentRules.Reason.CLASS_NOT_ALLOWED,denied.reason());
    }

    @Test void exactWeaponTypesDetermineTwoHandsAndAllArmorSlotsAreQualified() throws Exception {
        String[] weapons = {"sword","dagger","tohandsword","bow","spear","blunt","staff","throwingknife","arrow",
                "gauntlet","claw","edoryu","singlebow","singlespear","tohandblunt","tohandstaff","kiringku","chainsword"};
        Set<Integer> twoHands = Set.of(3,4,5,11,12,15,16,18);
        for (int index=0; index<weapons.length; index++) {
            var template = read(new Jdbc(List.of(item(1,"w",weapons[index])))).orElseThrow().instances().get(0).template();
            assertEquals(index+1,template.retainedType());
            assertEquals(twoHands.contains(index+1),((EquipmentRules.WeaponFacts)template.rules()).twoHanded());
        }
        String[] armors = {"helm","armor","T","cloak","glove","boots","shield","amulet","ring","belt","ring2","earring",
                "guarder","aid_left","aid_right","pants","talisman","talisman2","talisman3","talisman4","shoulder_left","shoulder_right","talisman5"};
        var slots = EquipmentRules.ArmorSlot.values(); assertEquals(slots.length,armors.length);
        for (int index=0; index<armors.length; index++) {
            var template = read(new Jdbc(List.of(item(1,"a",armors[index])))).orElseThrow().instances().get(0).template();
            assertEquals(slots[index],((EquipmentRules.ArmorFacts)template.rules()).slot());
        }
        for (String unknown : List.of("none","SWORD","unknown","18")) rejected(List.of(item(1,"w",unknown)));
        for (String unknown : List.of("none","shirt","16","unknown")) rejected(List.of(item(1,"a",unknown)));
    }

    @Test void levelZeroSentinelsAreExplicitAndSignedInvalidOrReversedRangesFail() throws Exception {
        var row = item(1,"w","sword");
        var range = ((EquipmentRules.WeaponFacts)read(new Jdbc(List.of(row))).orElseThrow().instances().get(0).template().rules()).levelRange();
        assertEquals(new EquipmentRules.LevelRange(1,Long.MAX_VALUE),range);
        row.put("w_min_lvl",4_000_000_000L); row.put("w_max_lvl",Long.MAX_VALUE);
        range = ((EquipmentRules.WeaponFacts)read(new Jdbc(List.of(row))).orElseThrow().instances().get(0).template().rules()).levelRange();
        assertEquals(new EquipmentRules.LevelRange(4_000_000_000L,Long.MAX_VALUE),range);
        for (long[] invalid : List.of(new long[]{-1,0},new long[]{0,-1},new long[]{3,2})) {
            row.put("w_min_lvl",invalid[0]); row.put("w_max_lvl",invalid[1]); rejected(List.of(row));
        }
    }

    @Test void darkAndCardAbsenceAndQueriedNullableDefaultsAreQualifiedButOwnersRemainStrict() throws Exception {
        var absent = item(1,"a","armor");
        var instance = read(new Jdbc(List.of(absent))).orElseThrow().instances().get(0);
        assertFalse(instance.dark().present()); assertEquals(Ability.zero(),instance.dark().raw());
        assertEquals(List.of(0L,0L,0L,0L,0L),instance.cards());
        var present = item(1,"a","armor"); present.put("dark_owner",1L); present.put("card_owner",1L);
        instance = read(new Jdbc(List.of(present))).orElseThrow().instances().get(0);
        assertTrue(instance.dark().present()); assertEquals(Ability.zero(),instance.dark().raw());
        for (Stat stat : Stat.values()) {
            var nullable = new HashMap<>(present); nullable.put("dark_"+stat.name(),null);
            if (stat == PIERCE || stat == PVP_REDUCTION) rejected(List.of(nullable)); // non-null SQL constants
            else assertEquals(0,read(new Jdbc(List.of(nullable))).orElseThrow().instances().get(0).dark().raw().get(stat));
        }
        for (int card=1; card<=5; card++) {
            var nullable = new HashMap<>(present); nullable.put("card_"+card,null);
            assertEquals(0,read(new Jdbc(List.of(nullable))).orElseThrow().instances().get(0).cards().get(card-1).longValue());
        }
        for (String owner : List.of("card_owner","dark_owner")) { var bad = new HashMap<>(present); bad.put(owner,2L); rejected(List.of(bad)); }
        present.put("card_3",-1L); rejected(List.of(present));
    }

    @Test void allTemplateNumericFieldsAndInstanceQualificationFlagsRejectNull() throws Exception {
        for (String alias : List.of("w","a")) {
            var valid = item(1,alias,alias.equals("w")?"sword":"armor");
            var columns = new ArrayList<String>(List.of("is_equipped","is_id","is_sealed"));
            for (String column : CLASS_COLUMNS) columns.add(alias+"_"+column);
            columns.addAll(List.of(alias+"_min_lvl",alias+"_max_lvl",alias+"_safenchant",alias+"_physical_melee_hit",
                    alias+"_physical_melee_damage",alias+"_physical_ranged_hit",alias+"_physical_ranged_damage",alias+"_physical_reduction",alias+"_physical_weight"));
            for (Stat stat : Stat.values()) columns.add(alias+"_base_"+stat.name());
            for (String column : columns) { var bad=new HashMap<>(valid); bad.put(column,null); rejected(List.of(bad)); }
            for (String column : CLASS_COLUMNS) for (long flag : new long[]{-1,Long.MIN_VALUE}) {
                var bad=new HashMap<>(valid); bad.put(alias+"_"+column,flag); rejected(List.of(bad));
            }
        }
        for (String column : List.of("greater","magic_stone_item_id","magic_stone_level")) {
            var bad=item(1,"a","armor"); bad.put(column,-1L); rejected(List.of(bad));
        }
    }

    @Test void setIdsRetainOrderAndDuplicatesWithStrictBoundsAndNoMalformedFallback() throws Exception {
        var row = item(1,"a","armor"); text(row,"a_set_ids","5,5,9223372036854775807");
        assertEquals(List.of(5L,5L,Long.MAX_VALUE),read(new Jdbc(List.of(row))).orElseThrow().instances().get(0).template().setIds());
        for (String empty : List.of(""," ","0")) {
            text(row,"a_set_ids",empty); assertTrue(read(new Jdbc(List.of(row))).orElseThrow().instances().get(0).template().setIds().isEmpty());
        }
        text(row,"a_set_ids",String.join(",",java.util.Collections.nCopies(64,"1")));
        assertEquals(64,read(new Jdbc(List.of(row))).orElseThrow().instances().get(0).template().setIds().size());
        for (String bad : List.of("1,,2","1,","-1","0,1","9223372036854775808","one","1;2","1\n2",
                String.join(",",java.util.Collections.nCopies(65,"1")),"1".repeat(4097))) {
            text(row,"a_set_ids",bad); rejected(List.of(row));
        }
        row.put("a_set_ids",null); row.put("a_set_ids_bytes",null);
        assertTrue(read(new Jdbc(List.of(row))).orElseThrow().instances().get(0).template().setIds().isEmpty());
        row.put("a_set_ids_bytes",0L); rejected(List.of(row));
    }

    @Test void nullableExtensionDefaultsRequireQueriedColumnsAndNeverHideMissingSchema() throws Exception {
        var row=item(1,"a","armor");
        for(String column:List.of("greater","magic_stone_item_id","magic_stone_level"))row.put(column,null);
        var instance=read(new Jdbc(List.of(row))).orElseThrow().instances().get(0);
        assertEquals(0,instance.greater());assertEquals(Stone.none(),instance.stone());
        row.put("magic_stone_item_id",9L);
        assertEquals(0,read(new Jdbc(List.of(row))).orElseThrow().instances().get(0).stone().tier());
        for(String missing:List.of("greater","magic_stone_item_id","magic_stone_level","a_set_ids")) {
            var bad=new HashMap<>(row);bad.remove(missing);rejected(List.of(bad));
        }
        row.put("dark_owner",1L);row.put("card_owner",1L);
        for(String missing:List.of("dark_STR","card_1")) {var bad=new HashMap<>(row);bad.remove(missing);rejected(List.of(bad));}
    }

    @Test void timestampExpiryPreservesMillisecondsAndRejectsLossyPrecisionOrImpossibleAbsentStone() throws Exception {
        var row=item(1,"a","armor");
        assertEquals(Stone.none(),read(new Jdbc(List.of(row))).orElseThrow().instances().get(0).stone());
        row.put("magic_stone_item_id",1L);
        assertTrue(read(new Jdbc(List.of(row))).orElseThrow().instances().get(0).stone().expiresAtEpochMillis().isEmpty());
        row.put("magic_stone_time",Timestamp.from(java.time.Instant.ofEpochMilli(999)));
        var stone=read(new Jdbc(List.of(row))).orElseThrow().instances().get(0).stone();
        assertEquals(999,stone.expiresAtEpochMillis().orElseThrow()); assertTrue(stone.active(998)); assertFalse(stone.active(999));
        row.put("magic_stone_time",Timestamp.from(java.time.Instant.ofEpochSecond(1,123)));
        rejected(List.of(row));
        row.put("magic_stone_time",Timestamp.from(java.time.Instant.ofEpochMilli(-1))); rejected(List.of(row));
        row.put("magic_stone_time",null); row.put("magic_stone_item_id",0L); row.put("magic_stone_level",1L); rejected(List.of(row));
        row.put("magic_stone_level",0L); row.put("magic_stone_time",Timestamp.from(java.time.Instant.EPOCH)); rejected(List.of(row));
    }

    @Test void etcIsExplicitlyNoEquipmentSlotButRetainsItsVipCardAndDarkProvenance() throws Exception {
        var row=item(1,"e",null); row.put("is_equipped",1L); row.put("dark_owner",1L); row.put("dark_STR",123L);
        row.put("card_owner",1L); row.put("card_1",10L); row.put("greater",2L);
        var instance=read(new Jdbc(List.of(row))).orElseThrow().instances().get(0);
        assertEquals(EquipmentRules.NonEquipmentFacts.INSTANCE,instance.template().rules());
        assertEquals(Ability.zero(),instance.template().base()); assertEquals(TemplatePhysical.zero(),instance.template().physical());
        assertEquals(123,instance.dark().raw().get(STR)); assertEquals(10,instance.cards().get(0).longValue());
        assertTrue(instance.source().equipped()); assertEquals(2,instance.greater());
    }

    @Test void full512ItemsRemainOrderedImmutableAnd513OrDuplicateJoinsRejectTheWholeResult() throws Exception {
        var rows=new ArrayList<Map<String,Object>>();
        for(long id=1;id<=512;id++)rows.add(item(id,"a","armor"));
        var source=read(new Jdbc(rows)).orElseThrow();
        assertEquals(512,source.instances().size()); assertEquals(512,source.instances().get(511).source().objectId());
        assertThrows(UnsupportedOperationException.class,()->source.instances().clear());
        assertThrows(UnsupportedOperationException.class,()->source.instances().get(0).cards().clear());
        rows.add(item(513,"a","armor")); rejected(rows);
        var first=item(1,"a","armor");
        rejected(List.of(first,new HashMap<>(first))); // duplicate dark/card/catalog rows cannot be deduplicated
        rejected(List.of(item(2,"a","armor"),first));
        var second=item(2,"a","armor"); second.put("Str",999L); rejected(List.of(first,second));
    }

    @Test void absentCharacterEmptyInventoryAndWrongIdentityOrAmbiguousCatalogAreDistinct() throws Exception {
        var absentJdbc=new Jdbc(List.of());assertTrue(read(absentJdbc).isEmpty());
        assertEquals(2,absentJdbc.prepares);assertNull(absentJdbc.lifetimeJdbc);absentJdbc.assertClosed();
        var emptyJdbc=new Jdbc(List.of(character()));var empty=read(emptyJdbc).orElseThrow();
        assertTrue(empty.snapshot().inventory().isEmpty()); assertTrue(empty.instances().isEmpty());
        assertTrue(empty.lifetimes().items().isEmpty());assertNotNull(emptyJdbc.lifetimeJdbc);emptyJdbc.assertClosed();
        for(String field:List.of("objid","character_type")) {var row=item(1,"a","armor");row.put(field,99L);rejected(List.of(row));}
        var missing=item(1,"a","armor");missing.put("a_id",null);rejected(List.of(missing));
        var ambiguous=item(1,"a","armor");ambiguous.put("w_id",1L);rejected(List.of(ambiguous));
        var mismatch=item(1,"a","armor");mismatch.put("a_id",2L);rejected(List.of(mismatch));
        rejected(List.of(character(),item(1,"a","armor")));
    }

    @Test void sourceQualifyRequiresExactSourceDatabaseAndCompleteEveryCandidateLookup() throws Exception {
        var source=read(new Jdbc(List.of(item(1,"a","armor")))).orElseThrow();
        var lookups=new HashMap<RuleKey,RuleValue>();
        for(var key:ModernEquipmentFacts.requiredLookups(source.instances()))lookups.put(key,RuleValue.absent());
        var catalog=new Catalog(7,"catalog",lookups,Set.of(),List.of(),List.of(),VipTableState.VERIFIED_ABSENT);
        assertEquals(source.snapshot(),source.qualify(catalog).source());
        assertThrows(IllegalArgumentException.class,()->source.qualify(new Catalog(8,"catalog",lookups,Set.of(),List.of(),List.of(),VipTableState.VERIFIED_ABSENT)));
        assertThrows(IllegalArgumentException.class,()->source.qualify(new Catalog(7,"other",lookups,Set.of(),List.of(),List.of(),VipTableState.VERIFIED_ABSENT)));
        assertThrows(IllegalArgumentException.class,()->source.qualify(new Catalog(7,"catalog",Map.of(),Set.of(),List.of(),List.of(),VipTableState.VERIFIED_ABSENT)));
        assertThrows(IllegalArgumentException.class,()->new ModernEquipmentSourceJdbc.Source("catalog",source.snapshot(),List.of()));
        assertEquals("ModernEquipmentSource[redacted]",source.toString());
    }

    @Test void invalidIdentifiersAccountsReadonlyOrWrongScopeFailBeforeAnyStatement() throws Exception {
        for(String database:List.of("catalog;DROP TABLE characters","other.table","`catalog`","a".repeat(65),"")) {
            Jdbc jdbc=new Jdbc(List.of());
            assertThrows(SQLException.class,()->scoped(()->ModernEquipmentSourceJdbc.read(jdbc.connection(),ModernBackendScope.current(),7,"owner",3333,database)));
            assertEquals(0,jdbc.prepares);
        }
        for(String account:List.of(""," ","\u00a0","bad\naccount","a".repeat(65),"\ud800")) {
            Jdbc jdbc=new Jdbc(List.of());
            assertThrows(SQLException.class,()->scoped(()->ModernEquipmentSourceJdbc.read(jdbc.connection(),ModernBackendScope.current(),7,account,3333,"catalog")));
            assertEquals(0,jdbc.prepares);
        }
        Jdbc writable=new Jdbc(List.of());writable.readOnly=false;
        assertThrows(SQLException.class,()->read(writable));assertEquals(0,writable.prepares);
        Jdbc wrong=new Jdbc(List.of());
        assertThrows(SQLException.class,()->scoped(()->ModernEquipmentSourceJdbc.read(wrong.connection(),scope(30),7,"owner",3333,"catalog")));
        assertEquals(0,wrong.prepares);
    }

    @Test void boundedTextRejectsControlInvalidUtf8AndTruncatedOriginalLengths() throws Exception {
        for(String field:List.of("char_name","a_name","a_type","a_set_ids")) {
            for(String value:List.of("bad\u0000value","bad\nvalue","\ud800")) {
                var row=item(1,"a","armor");text(row,field,value);rejected(List.of(row));
            }
        }
        for(String field:List.of("a_type","a_set_ids")) {
            var row=item(1,"a","armor");row.put(field+"_bytes",4097L);rejected(List.of(row));
            row=item(1,"a","armor");row.put(field+"_bytes",null);rejected(List.of(row));
        }
        var truncated=item(1,"a","armor");text(truncated,"a_type","armor");truncated.put("a_type_bytes",6L);rejected(List.of(truncated));
    }

    @Test void cancellationDuringRowsOrAfterLastRowNeverPublishesPartialInstancesAndClosesResources() {
        for(boolean atEnd:List.of(false,true)) {
            Jdbc jdbc=new Jdbc(List.of(item(1,"a","armor"),item(2,"a","armor")));
            jdbc.cancelAtRow=atEnd?3:2;
            var error=assertThrows(ModernBackendExecutor.BackendException.class,()->read(jdbc));
            assertEquals(ModernBackendExecutor.Failure.CANCELLED,error.failure());jdbc.assertClosed();
        }
    }

    @Test void deadlineAndLargeTimeoutStayBoundedAndBackendExceptionsDoNotExposeSecrets() throws Exception {
        Jdbc expired=new Jdbc(List.of());var dead=scope(-1);
        var timeout=assertThrows(ModernBackendExecutor.BackendException.class,()->ModernEquipmentSourceJdbc.read(expired.connection(),dead,7,"owner",3333,"catalog"));
        assertEquals(ModernBackendExecutor.Failure.DEADLINE,timeout.failure());assertEquals(0,expired.prepares);
        Jdbc large=new Jdbc(List.of());var scope=scope(TimeUnit.DAYS.toSeconds(30));
        ModernBackendScope.bind(scope);
        try {assertTrue(ModernEquipmentSourceJdbc.read(large.connection(),scope,7,"owner",3333,"catalog").isEmpty());}
        finally {scope.finish();ModernBackendScope.unbind();}
        assertEquals(2147484,large.querySeconds);large.assertClosed();
        Jdbc failed=new Jdbc(List.of());failed.executeFailure=true;failed.closeFailure=true;
        var error=assertThrows(SQLException.class,()->read(failed));
        assertEquals("modern equipment source unavailable",error.getMessage());assertNull(error.getCause());assertEquals(0,error.getSuppressed().length);
        assertTrue(failed.statementClosed);assertFalse(failed.connectionClosed);
    }

    @Test void optionalResistanceMetadataQualifiesAllSixAbsentWithoutDroppingKnownValues() throws Exception {
        var row=item(1,"w","sword");
        for(Stat stat:RESISTANCE_STATS)row.put("w_base_"+stat.name(),Long.MAX_VALUE);
        Jdbc absent=new Jdbc(List.of(row));absent.metadata.clear();absent.metadata.add(metadataColumn("item_id"));
        var source=read(absent).orElseThrow();
        for(Stat stat:RESISTANCE_STATS) {
            assertEquals(0,source.instances().get(0).template().base().get(stat));
            assertTrue(absent.sql.contains("0 AS w_base_"+stat.name()));
        }
        assertTrue(absent.sql.contains("w.`add_magic_hit` AS w_base_MAGIC_HIT"));
        absent.assertClosed();assertEquals(9,absent.prepares);assertEquals(1,absent.dataPrepares);assertEquals(6,absent.guards.size());
        assertEquals(6,absent.guards.stream().map(guard->guard.query).distinct().count());
        for(var guard:absent.guards)assertTrue(guard.query.matches("SELECT `regist_(blind|sleep|freeze|stone|sustain|stun)` FROM `catalog`\\.`weapon` LIMIT 0"));
        Jdbc present=new Jdbc(List.of(row));var full=read(present).orElseThrow();
        for(Stat stat:RESISTANCE_STATS)assertEquals(Long.MAX_VALUE,full.instances().get(0).template().base().get(stat));
        present.assertClosed();
    }

    @Test void optionalResistancePartialAndCaseInsensitiveMetadataStillRequiresPresentNonNullValues() throws Exception {
        var row=item(1,"w","sword");row.put("w_base_BLIND",11L);row.put("w_base_STUN",12L);
        Jdbc partial=new Jdbc(List.of(row));partial.metadata.clear();
        for(String name:List.of("item_id","REGIST_BLIND","regist_stun"))partial.metadata.add(metadataColumn(name));
        var base=read(partial).orElseThrow().instances().get(0).template().base();
        assertEquals(11,base.get(BLIND));assertEquals(12,base.get(STUN));assertEquals(0,base.get(FREEZE));
        assertTrue(partial.sql.contains("w.`regist_blind` AS w_base_BLIND"));partial.assertClosed();
        row.put("w_base_BLIND",null);rejected(List.of(row));
        row.remove("w_base_BLIND");rejected(List.of(row));
        var required=item(1,"w","sword");required.remove("w_base_STR");
        Jdbc missing=new Jdbc(List.of(required));missing.metadata.clear();missing.metadata.add(metadataColumn("item_id"));
        assertThrows(SQLException.class,()->read(missing));missing.assertClosed();
    }

    @Test void metadataFailureOrMissingWeaponTableNeverFallsBackToZeroAndClosesMetadataResources() {
        Jdbc unavailable=new Jdbc(List.of());unavailable.metadataFailure=true;
        var error=assertThrows(SQLException.class,()->read(unavailable));
        assertEquals("modern equipment source unavailable",error.getMessage());assertNull(error.getCause());assertEquals(0,error.getSuppressed().length);
        assertTrue(unavailable.metadataStatementClosed);assertEquals(0,unavailable.dataPrepares);assertFalse(unavailable.connectionClosed);
        for(List<Map<String,Object>> missing:List.of(List.<Map<String,Object>>of(),List.of(metadataColumn("unrelated_column")))) {
            Jdbc jdbc=new Jdbc(List.of());jdbc.metadata.clear();jdbc.metadata.addAll(missing);
            assertThrows(SQLException.class,()->read(jdbc));jdbc.assertMetadataClosed();assertEquals(0,jdbc.dataPrepares);
        }
    }

    @Test void duplicateMalformedAndOverlongMetadataIsRejectedWithoutPartialDataStatement() throws Exception {
        var badName=metadataColumn("regist_stun");badName.put("COLUMN_NAME",null);
        var badLength=metadataColumn("regist_stun");badLength.put("COLUMN_NAME_BYTES",null);
        var oversized=metadataColumn("x".repeat(257));
        for(List<Map<String,Object>> bad:List.of(List.of(metadataColumn("item_id"),metadataColumn("ITEM_ID")),
                List.of(badName),List.of(badLength),List.of(oversized),List.of(metadataColumn("bad\ncolumn")),
                List.of(metadataColumn(" item_id")))) {
            Jdbc jdbc=new Jdbc(List.of());jdbc.metadata.clear();jdbc.metadata.addAll(bad);
            assertThrows(SQLException.class,()->read(jdbc));jdbc.assertMetadataClosed();assertEquals(0,jdbc.dataPrepares);
        }
        Jdbc tooMany=new Jdbc(List.of());tooMany.metadata.clear();tooMany.metadata.add(metadataColumn("item_id"));
        for(int index=1;index<=256;index++)tooMany.metadata.add(metadataColumn("extra_"+index));
        assertThrows(SQLException.class,()->read(tooMany));tooMany.assertMetadataClosed();assertEquals(0,tooMany.dataPrepares);
        Jdbc full=new Jdbc(List.of());full.metadata.clear();full.metadata.addAll(tooMany.metadata.subList(0,256));
        assertTrue(read(full).isEmpty());full.assertClosed();
    }

    @Test void metadataCancellationUsesTheSameDeadlineAndNeverStartsCharacterRead() {
        Jdbc jdbc=new Jdbc(List.of());jdbc.metadataCancel=true;
        var error=assertThrows(ModernBackendExecutor.BackendException.class,()->read(jdbc));
        assertEquals(ModernBackendExecutor.Failure.CANCELLED,error.failure());jdbc.assertMetadataClosed();assertEquals(0,jdbc.dataPrepares);
        assertEquals(Map.of(1,"catalog",2,"weapon"),jdbc.metadataParameters);assertEquals(257,jdbc.metadataMaxRows);
        assertTrue(jdbc.metadataSql.contains("LEFT(COLUMN_NAME,257)"));assertTrue(jdbc.metadataSql.endsWith("LIMIT 257"));
    }

    @Test void optionalGuardRejectsPermissionMissingTableOrWrongErrorIdentity() {
        for(String mode:List.of("permission","missing-table","wrong-vendor","wrong-state")) {
            Jdbc jdbc=hiddenColumn(mode);var error=assertThrows(SQLException.class,()->read(jdbc));
            assertEquals("modern equipment source unavailable",error.getMessage());assertNull(error.getCause());assertEquals(0,error.getSuppressed().length);
            assertEquals(0,jdbc.dataPrepares);assertEquals(1,jdbc.guards.size());jdbc.assertMetadataClosed();jdbc.assertGuardsClosed();
        }
    }

    @Test void optionalGuardSuccessCannotResolveMetadataDisagreement() {
        for(String mode:List.of("success","unexpected-row")) {
            Jdbc jdbc=hiddenColumn(mode);assertThrows(SQLException.class,()->read(jdbc));
            assertEquals(0,jdbc.dataPrepares);assertEquals(1,jdbc.guards.size());
            assertTrue(jdbc.guards.get(0).resultOpened);jdbc.assertMetadataClosed();jdbc.assertGuardsClosed();
        }
    }

    @Test void optionalGuardRequiresCleanQueryPhaseFailureNotSetupOrCleanup() {
        for(String mode:List.of("unknown-close","unknown-suppressed","close-unknown","result-close-unknown","setup-unknown")) {
            Jdbc jdbc=hiddenColumn(mode);var error=assertThrows(SQLException.class,()->read(jdbc));
            assertEquals("modern equipment source unavailable",error.getMessage());assertNull(error.getCause());assertEquals(0,error.getSuppressed().length);
            assertEquals(0,jdbc.dataPrepares);assertEquals(1,jdbc.guards.size());jdbc.assertMetadataClosed();
            var guard=jdbc.guards.get(0);assertTrue(guard.closed);assertFalse(jdbc.connectionClosed);
            if(guard.resultOpened)assertTrue(guard.resultClosed);
            if(!mode.equals("setup-unknown"))jdbc.assertGuardsClosed();
        }
    }

    @Test void optionalGuardKnownPrepareFailureIsAcceptedWithoutLeakingResources() throws Exception {
        Jdbc jdbc=hiddenColumn("prepare-unknown");assertTrue(read(jdbc).isEmpty());
        // Metadata + the failed guard prepare + an empty character SELECT.
        // No guard statement was allocated, and no character means no lifetime SELECT.
        assertEquals(3,jdbc.prepares);assertEquals(2,jdbc.executions);assertNull(jdbc.lifetimeJdbc);
        assertEquals(1,jdbc.dataPrepares);assertEquals(1,jdbc.guards.size());
        assertFalse(jdbc.guards.get(0).statementOpened);assertTrue(jdbc.sql.contains("0 AS w_base_BLIND"));jdbc.assertClosed();
    }

    @Test void optionalGuardCancellationNeverStartsDataRead() {
        Jdbc jdbc=hiddenColumn("cancel");var error=assertThrows(ModernBackendExecutor.BackendException.class,()->read(jdbc));
        assertEquals(ModernBackendExecutor.Failure.CANCELLED,error.failure());assertEquals(0,jdbc.dataPrepares);
        assertEquals(1,jdbc.guards.size());jdbc.assertMetadataClosed();jdbc.assertGuardsClosed();
    }

    @Test void originalPersistedSnapshotReaderKeepsItsOutputAndRequiresNoEquipmentExtensionColumns() throws Exception {
        var rows=List.of(item(1,"w","sword"),item(2,"a","armor"),item(3,"e",null));
        var equipment=read(new Jdbc(rows)).orElseThrow();
        Jdbc base=new Jdbc(rows);
        var snapshot=scoped(()->ModernCharacterDataJdbc.read(base.connection(),ModernBackendScope.current(),7,"owner",3333,"catalog")).orElseThrow();
        assertEquals(snapshot,equipment.snapshot());assertEquals(ModernCharacterDataJdbc.sql("catalog"),base.sql);
        assertFalse(base.sql.contains("character_items_dark"));assertFalse(base.sql.contains("character_items_card"));
        assertFalse(base.sql.contains("safenchant"));base.assertClosed();
    }

    @Test void fixedProjectionKeepsRetainedArmorWeaponDarkSignsAndDoesNotGuessMissingFields() {
        String sql=ModernEquipmentSourceJdbc.sql("catalog");
        for(String expected:List.of("w.`add_str` AS w_base_STR","a.`ac` AS a_base_AC","a.`m_def` AS a_base_MR",
                "0 AS w_base_AC","0 AS w_base_MR","0 AS a_base_REDUCTION","0 AS a_base_WEIGHT_REDUCTION",
                "a.damage_reduction AS a_physical_reduction","a.weight_reduction AS a_physical_weight",
                "d.`add_ac` AS dark_AC","d.`add_weight_reduction` AS dark_WEIGHT_REDUCTION","0 AS dark_PIERCE","0 AS dark_PVP_REDUCTION",
                "w.use_elf AS w_use_elf","w.use_mage AS w_use_mage","w.`regist_stone` AS w_base_STONE"))assertTrue(sql.contains(expected),expected);
        assertTrue(sql.contains("LEFT(a.set_id,4097)"));assertFalse(sql.contains("INFORMATION_SCHEMA"));
        assertFalse(sql.contains(" FOR UPDATE"));assertFalse(sql.contains("COALESCE"));assertFalse(sql.contains("DISTINCT"));
    }

    private static Optional<ModernEquipmentSourceJdbc.Source> read(Jdbc jdbc)throws Exception {
        return scoped(()->ModernEquipmentSourceJdbc.read(jdbc.connection(),ModernBackendScope.current(),7,"owner",3333,"catalog"));
    }
    private static void rejected(List<Map<String,Object>> rows) {
        Jdbc jdbc=new Jdbc(rows);var error=assertThrows(SQLException.class,()->read(jdbc));
        assertEquals("modern equipment source unavailable",error.getMessage());assertNull(error.getCause());assertEquals(0,error.getSuppressed().length);jdbc.assertClosed();
    }
    static ModernBackendScope scope(long seconds) {
        return new ModernBackendScope(System.nanoTime()+TimeUnit.SECONDS.toNanos(seconds),new ModernBackendScope.AbortScheduler(){
            public boolean execute(Runnable action){action.run();return true;}public void remove(Runnable action){}
        });
    }
    static <T>T scoped(Callable<T> work)throws Exception {
        var scope=scope(30);ModernBackendScope.bind(scope);
        try{return work.call();}finally{scope.finish();ModernBackendScope.unbind();}
    }
    static Map<String,Object> character() {
        var row=new HashMap<String,Object>();row.put("objid",3333L);text(row,"char_name","fixture-character");
        row.put("character_type",0L);row.put("character_sex",0L);row.put("level",Long.MAX_VALUE);
        String[] fields={"MaxHp","MaxMp","CurHp","CurMp","Ac","Str","Dex","Con","Intel","Wis","Cha","Exp","Lawful",
                "OriginalStr","OriginalDex","OriginalCon","OriginalInt","OriginalWis","Food"};
        long[] values={100,50,Long.MAX_VALUE,500,-999,101,102,103,104,105,106,Long.MAX_VALUE,-32768,11,12,13,14,15,225};
        for(int index=0;index<fields.length;index++)row.put(fields[index],values[index]);
        row.put("item_objid",null);return row;
    }
    static Map<String,Object> item(long id,String alias,String type) {
        var row=character();row.put("item_objid",id);row.put("catalog_id",id);row.put("item_count",1L);
        for(String flag:List.of("is_equipped","is_id","is_sealed"))row.put(flag,0L);
        for(String number:List.of("enchantlvl","durability","charge_count","greater","magic_stone_item_id","magic_stone_level"))row.put(number,0L);
        // Explicit source fixture facts for the separate real lifetime decoder.
        row.put("lifetime_remaining",0L);row.put("lifetime_maximum",0L);
        row.put("lifetime_rule",null);row.put("lifetime_hours",null);row.put("lifetime_deadline",null);
        row.put("bless",1L);row.put("magic_stone_time",null);row.put("dark_owner",null);row.put("card_owner",null);
        for(int index=1;index<=5;index++)row.put("card_"+index,0L);
        for(String candidate:List.of("w","a","e"))row.put(candidate+"_id",null);
        row.put(alias+"_id",id);text(row,alias+"_name","$fixture");row.put(alias+"_inventory_graphic",1L);row.put(alias+"_ground_graphic",1L);row.put(alias+"_weight",1L);
        for(Stat stat:Stat.values()) {row.put("dark_"+stat.name(),0L);row.put(alias+"_base_"+stat.name(),0L);}
        if(!alias.equals("e")) {
            text(row,alias+"_type",type);text(row,alias+"_set_ids","0");row.put(alias+"_min_lvl",0L);row.put(alias+"_max_lvl",0L);row.put(alias+"_safenchant",6L);
            for(String column:CLASS_COLUMNS)row.put(alias+"_"+column,1L);
            for(String physical:List.of("melee_hit","melee_damage","ranged_hit","ranged_damage","reduction","weight"))row.put(alias+"_physical_"+physical,0L);
        }
        return row;
    }
    private static void text(Map<String,Object> row,String key,String value) {row.put(key,value);row.put(key+"_bytes",value==null?null:(long)value.getBytes(StandardCharsets.UTF_8).length);}
    private static final List<Stat> RESISTANCE_STATS=List.of(BLIND,SLEEP,FREEZE,STONE,SUSTAIN,STUN);
    private static Map<String,Object> metadataColumn(String name){
        var column=new HashMap<String,Object>();column.put("COLUMN_NAME",name);column.put("COLUMN_NAME_BYTES",(long)name.getBytes(StandardCharsets.UTF_8).length);return column;
    }
    private static Jdbc hiddenColumn(String mode){
        Jdbc jdbc=new Jdbc(List.of());jdbc.metadata.removeIf(column->"regist_blind".equals(column.get("COLUMN_NAME")));
        jdbc.guardModes.put("regist_blind",mode);return jdbc;
    }
    static final class Jdbc {
        final List<Map<String,Object>> rows;final Map<Integer,Object> parameters=new LinkedHashMap<>();
        final Map<String,Integer> numericTypes=new HashMap<>();
        final List<Map<String,Object>> metadata=new ArrayList<>();final Map<Integer,Object> metadataParameters=new LinkedHashMap<>();
        final List<Guard> guards=new ArrayList<>();final Map<String,String> guardModes=new HashMap<>();
        String sql,metadataSql;int prepares,executions,dataPrepares,metadataPrepares,maxRows,querySeconds,cancelAtRow,metadataMaxRows,metadataSeconds;
        Jdbc lifetimeJdbc;List<Map<String,Object>> lifetimeRows;int lifetimeCancelAtRow;
        boolean lifetimeExecuteFailure,lifetimeCloseFailure;
        boolean readOnly=true,statementClosed,resultClosed,connectionClosed,executeFailure,closeFailure;
        boolean metadataStatementClosed,metadataResultClosed,metadataFailure,metadataCancel;
        Jdbc(List<Map<String,Object>> rows){this.rows=List.copyOf(rows);
            for(String column:List.of("item_id","regist_blind","regist_sleep","regist_freeze","regist_stone","regist_sustain","regist_stun"))metadata.add(metadataColumn(column));
        }
        Connection connection(){return (Connection)Proxy.newProxyInstance(getClass().getClassLoader(),new Class<?>[]{Connection.class},(p,m,a)->switch(m.getName()){
            case "isReadOnly"->readOnly;case "close"->{connectionClosed=true;yield null;}
            case "prepareStatement"->{prepares++;String query=(String)a[0];
                if(query.contains("INFORMATION_SCHEMA")){metadataPrepares++;metadataSql=query;yield metadataStatement();}
                if(query.startsWith("SELECT c.objid AS owner_object,")&&!rows.isEmpty()&&!rows.get(0).containsKey("owner_object")){
                    lifetimeJdbc=new Jdbc(lifetimeRows==null?lifetimeProjection(rows):lifetimeRows);lifetimeJdbc.sql=query;
                    lifetimeJdbc.executeFailure=lifetimeExecuteFailure;lifetimeJdbc.closeFailure=lifetimeCloseFailure;
                    lifetimeJdbc.cancelAtRow=lifetimeCancelAtRow;
                    for(String field:List.of("database_now","item_deadline","etc_type"))lifetimeJdbc.numericTypes.put(field,Types.VARCHAR);
                    PreparedStatement delegate=lifetimeJdbc.statement();
                    yield Proxy.newProxyInstance(getClass().getClassLoader(),new Class<?>[]{PreparedStatement.class},(owner,method,args)->{
                        if(method.getName().equals("executeQuery"))executions++;
                        try{return method.invoke(delegate,args);}catch(java.lang.reflect.InvocationTargetException failure){throw failure.getCause();}
                    });}
                if(query.startsWith("SELECT `")&&query.endsWith("FROM `catalog`.`weapon` LIMIT 0")){
                    String column=query.substring(8,query.indexOf('`',8));var guard=new Guard(query,guardModes.getOrDefault(column,"unknown"));guards.add(guard);
                    if(guard.mode.equals("prepare-unknown"))throw missingColumn();yield guard.statement();}
                dataPrepares++;sql=query;yield statement();}
            default->throw new UnsupportedOperationException(m.getName());
        });}
        final class Guard {
            final String query,mode;int maxRows,seconds;boolean statementOpened,closed,resultOpened,resultClosed;
            Guard(String query,String mode){this.query=query;this.mode=mode;}
            PreparedStatement statement(){statementOpened=true;return (PreparedStatement)Proxy.newProxyInstance(getClass().getClassLoader(),new Class<?>[]{PreparedStatement.class},(p,m,a)->switch(m.getName()){
                case "setMaxRows"->{maxRows=(Integer)a[0];if(mode.equals("setup-unknown"))throw missingColumn();yield null;}
                case "setQueryTimeout"->{seconds=(Integer)a[0];yield null;}
                case "executeQuery"->{executions++;
                    yield switch(mode){
                        case "unknown","unknown-close" -> throw missingColumn();
                        case "permission" -> throw new SQLException("SYNTHETIC-SECRET permission","42000",1142);
                        case "missing-table" -> throw new SQLException("SYNTHETIC-SECRET table","42S02",1146);
                        case "wrong-vendor" -> throw new SQLException("SYNTHETIC-SECRET vendor","42S22",1055);
                        case "wrong-state" -> throw new SQLException("SYNTHETIC-SECRET state","42000",1054);
                        case "unknown-suppressed" -> {var failure=missingColumn();failure.addSuppressed(new SQLException("SYNTHETIC-SECRET cleanup"));throw failure;}
                        case "cancel" -> {ModernBackendScope.current().cancel(ModernBackendExecutor.Failure.CANCELLED);throw missingColumn();}
                        default -> {resultOpened=true;yield result();}
                    };
                }
                case "close"->{closed=true;if(mode.equals("unknown-close")||mode.equals("close-unknown"))throw missingColumn();yield null;}
                default->throw new UnsupportedOperationException(m.getName());
            });}
            ResultSet result(){return (ResultSet)Proxy.newProxyInstance(getClass().getClassLoader(),new Class<?>[]{ResultSet.class},(p,m,a)->switch(m.getName()){
                case "next"->mode.equals("unexpected-row");
                case "close"->{resultClosed=true;if(mode.equals("result-close-unknown"))throw missingColumn();yield null;}
                default->throw new UnsupportedOperationException(m.getName());
            });}
        }
        private static SQLException missingColumn(){return new SQLException("SYNTHETIC-SECRET unknown","42S22",1054);}
        PreparedStatement metadataStatement(){return (PreparedStatement)Proxy.newProxyInstance(getClass().getClassLoader(),new Class<?>[]{PreparedStatement.class},(p,m,a)->switch(m.getName()){
            case "setString"->{metadataParameters.put((Integer)a[0],a[1]);yield null;}
            case "setMaxRows"->{metadataMaxRows=(Integer)a[0];yield null;}case "setQueryTimeout"->{metadataSeconds=(Integer)a[0];yield null;}
            case "executeQuery"->{executions++;if(metadataFailure)throw new SQLException("SYNTHETIC-SECRET metadata");yield metadataResult();}
            case "close"->{metadataStatementClosed=true;yield null;}
            default->throw new UnsupportedOperationException(m.getName());
        });}
        ResultSet metadataResult(){final int[] index={-1};final boolean[] wasNull={false};
            var columns=new ModernJdbcNumbersTest.Columns(column->metadata.get(index[0]).get(column));
            return (ResultSet)Proxy.newProxyInstance(getClass().getClassLoader(),new Class<?>[]{ResultSet.class},(p,m,a)->switch(m.getName()){
                case "next"->{index[0]++;if(metadataCancel)ModernBackendScope.current().cancel(ModernBackendExecutor.Failure.CANCELLED);yield index[0]<metadata.size();}
                case "wasNull"->wasNull[0];case "close"->{metadataResultClosed=true;yield null;}
                case "findColumn"->columns.find((String)a[0]);case "getMetaData"->columns.metadata();
                case "getString","getLong","getBigDecimal"->{Object value=metadata.get(index[0]).get(columns.name(a[0]));wasNull[0]=value==null;
                    if(m.getName().equals("getString"))yield value==null?null:value.toString();
                    if(m.getName().equals("getBigDecimal"))yield ModernJdbcNumbersTest.Columns.decimal(value);yield value==null?0L:((Number)value).longValue();}
                default->throw new UnsupportedOperationException(m.getName());
            });
        }
        PreparedStatement statement(){return (PreparedStatement)Proxy.newProxyInstance(getClass().getClassLoader(),new Class<?>[]{PreparedStatement.class},(p,m,a)->switch(m.getName()){
            case "setLong","setString"->{parameters.put((Integer)a[0],a[1]);yield null;}
            case "setMaxRows"->{maxRows=(Integer)a[0];yield null;}case "setQueryTimeout"->{querySeconds=(Integer)a[0];yield null;}
            case "executeQuery"->{executions++;if(executeFailure)throw new SQLException("SYNTHETIC-SECRET database password");yield result();}
            case "close"->{statementClosed=true;if(closeFailure)throw new SQLException("SYNTHETIC-SECRET close");yield null;}
            default->throw new UnsupportedOperationException(m.getName());
        });}
        ResultSet result(){final int[] index={-1};final boolean[] wasNull={false};
            ModernJdbcNumbersTest.Value projected=key->{
                boolean qualifiedZero=RESISTANCE_STATS.stream().anyMatch(stat->key.equals("w_base_"+stat.name()))&&sql.contains("0 AS "+key);
                if(!qualifiedZero&&!rows.get(index[0]).containsKey(key))throw new SQLException("missing synthetic projected column");
                return qualifiedZero?0L:rows.get(index[0]).get(key);
            };
            var columns=new ModernJdbcNumbersTest.Columns(projected,numericTypes);
            return (ResultSet)Proxy.newProxyInstance(getClass().getClassLoader(),new Class<?>[]{ResultSet.class},(p,m,a)->switch(m.getName()){
                case "next"->{index[0]++;if(cancelAtRow>0&&index[0]+1==cancelAtRow)ModernBackendScope.current().cancel(ModernBackendExecutor.Failure.CANCELLED);yield index[0]<rows.size();}
                case "wasNull"->wasNull[0];case "close"->{resultClosed=true;yield null;}
                case "findColumn"->columns.find((String)a[0]);case "getMetaData"->columns.metadata();
                case "getLong","getString","getTimestamp","getBigDecimal","getDouble"->{String key=columns.name(a[0]);
                    Object value=projected.read(key);wasNull[0]=value==null;
                    if(m.getName().equals("getString"))yield value==null?null:value.toString();
                    if(m.getName().equals("getBigDecimal"))yield ModernJdbcNumbersTest.Columns.decimal(value);
                    if(m.getName().equals("getDouble"))yield value==null?0.0:((Number)value).doubleValue();
                    if(m.getName().equals("getTimestamp"))yield value;yield value==null?0L:((Number)value).longValue();}
                default->throw new UnsupportedOperationException(m.getName());
            });
        }
        void assertMetadataClosed(){assertTrue(metadataStatementClosed);assertTrue(metadataResultClosed);assertFalse(connectionClosed);assertTrue(metadataSeconds>=1);assertEquals(257,metadataMaxRows);}
        void assertGuardsClosed(){for(var guard:guards){if(!guard.statementOpened){assertFalse(guard.closed);continue;}assertTrue(guard.closed);if(guard.resultOpened)assertTrue(guard.resultClosed);assertEquals(1,guard.maxRows);assertTrue(guard.seconds>=1);}assertFalse(connectionClosed);}
        void assertClosed(){assertTrue(statementClosed);assertTrue(resultClosed);assertFalse(connectionClosed);assertTrue(querySeconds>=1);assertEquals(513,maxRows);if(metadataPrepares>0)assertMetadataClosed();assertGuardsClosed();if(lifetimeJdbc!=null)lifetimeJdbc.assertClosed();}
    }

    /** Project explicit source test rows into the lifetime SELECT, not an Observation bypass. */
    static List<Map<String,Object>> lifetimeProjection(List<Map<String,Object>> sourceRows) {
        var output=new ArrayList<Map<String,Object>>();
        for(var source:sourceRows) {
            var row=ModernItemLifetimeJdbcTest.empty();row.put("owner_object",source.get("objid"));
            Object id=source.get("item_objid");
            if(id!=null) {
                row.put("item_object",id);row.put("catalog_id",source.get("catalog_id"));row.put("is_equipped",source.get("is_equipped"));
                for(String field:List.of("lifetime_remaining","lifetime_maximum","lifetime_rule","lifetime_hours","lifetime_deadline"))
                    if(!source.containsKey(field))throw new AssertionError("missing explicit lifetime fixture field "+field);
                row.put("remaining_time",source.get("lifetime_remaining"));row.put("rule_catalog",source.get("lifetime_rule"));
                row.put("rule_hours",source.get("lifetime_hours"));
                String deadline=(String)source.get("lifetime_deadline");row.put("timed_object",deadline==null?null:id);text(row,"item_deadline",deadline);
                for(String[] kind:new String[][]{{"w","weapon"},{"a","armor"},{"e","etc"}}) {
                    row.put(kind[1]+"_catalog",source.get(kind[0]+"_id"));
                    if(source.get(kind[0]+"_id")!=null) {
                        if(kind[0].equals("e"))text(row,"etc_type","other");
                        else row.put(kind[1]+"_maximum",source.get("lifetime_maximum"));
                    }
                }
            }
            output.add(row);
        }
        return output;
    }
}
