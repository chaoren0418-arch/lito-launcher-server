package local.lito381.server;

import static org.junit.jupiter.api.Assertions.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Explicit synthetic shared 554 geometry, NOT a recovered native map540. */
class ModernTimeAttackConfigurationTest {
    static final String PREFIX="world.4.540.";
    static final ModernWorldBinding.Key KEY=new ModernWorldBinding.Key(4,540);
    @TempDir Path temporary;
    static Properties world(Path root)throws Exception {
        var old=ModernNpcConfigurationTest.world(root);var p=new Properties();
        for(String k:old.stringPropertyNames())p.setProperty(k.replace(ModernNpcConfigurationTest.PREFIX,PREFIX),old.getProperty(k));
        p.setProperty("world.bindings","4:540");p.remove(PREFIX+"npcSpawns");p.remove(PREFIX+"npcCamp1");
        p.setProperty(PREFIX+"npcPlayerCapacity","60");p.setProperty(PREFIX+"npcCapacity","8");p.setProperty(PREFIX+"npcCorpseMillis","1");
        p.setProperty(PREFIX+"attackMillis","1");p.setProperty(PREFIX+"countdownNanos","0");p.setProperty(PREFIX+"activeTimeoutMillis","60000");
        p.setProperty(PREFIX+"timeAttackRules",ModernTimeAttackConfiguration.RULESET);
        p.setProperty(PREFIX+"timeAttackTower","32779,32779,2665,1");p.setProperty(PREFIX+"timeAttackDurationMillis","20000");
        p.setProperty(PREFIX+"timeAttackWaves","ORDINARY,ORDINARY,FINAL_BOSSES");
        for(int i=0;i<3;i++)p.setProperty(PREFIX+"timeAttackWave"+i,ModernNpcMeleeTest.NPC+",32779,32778,2");
        java.nio.file.Files.createDirectories(root.resolve("quest-results"));return p;
    }
    static String text(Path root,Properties p){return ModernNpcConfigurationTest.base(root)+ModernQuestConfigurationTest.settlement(root)+ModernNpcConfigurationTest.lines(p);}
    static ModernServerConfig config(Path root,Properties p)throws Exception{return ModernServerConfig.parse(text(root,p).getBytes(StandardCharsets.UTF_8),name->"fixture-only");}
    static ModernNpcEncounter encounter(ModernWorldConfiguration config) {
        var spec=config.npcSpecs().get(KEY);var old=ModernNpcMeleeTest.catalog(Map.of(ModernNpcCatalog.Field.HP,1L),true,0);
        var n=old.npcs().get(ModernNpcMeleeTest.NPC);
        var npc=new ModernNpcCatalog.Npc(new ModernNpcCatalog.Key(spec.catalogSource(),spec.catalogDatabase(),n.key().npcId()),n.name(),n.nameId(),n.implementation(),n.large(),n.facts(),n.hasMobskills());
        var catalog=new ModernNpcCatalog.Catalog(spec.catalogSource(),spec.catalogDatabase(),Map.of(npc.key().npcId(),npc),
                new ModernPhysicalCombat.Growth(spec.catalogSource(),spec.catalogDatabase(),old.growth().strength(),old.growth().dexterity()));
        return spec.qualify(catalog,Map.of(7,ModernNpcServiceTest.Fixture.blessing(7),42,ModernNpcServiceTest.Fixture.blessing(42)),()->ClassicLineageClock.REAL_BASE_EPOCH_MILLIS);
    }
    static ModernWorldService.Mapping mapping(Path root,Properties p)throws Exception {
        try(var c=config(root,p)){return c.worlds().load(null,()->false,Map.of(KEY,encounter(c.worlds()))).get(KEY);}
    }
    static void reject(String text) {
        var reads=new AtomicInteger();assertThrows(ModernServerConfig.ConfigException.class,()->ModernServerConfig.parse(text.getBytes(StandardCharsets.UTF_8),name->{reads.incrementAndGet();return "not-read";}));
        assertEquals(0,reads.get());
    }
    byte[] binding(Properties p)throws Exception {try(var c=config(temporary,p)){return ModernSettlementCoordinator.binding(c);}}
    @Test void explicitPolicyLoadsFullFuturePlanAndOneCooperativeCampBeforeAdmission()throws Exception {
        var p=world(temporary);var m=mapping(temporary,p);assertNotNull(m.timeAttack());assertEquals(3,m.timeAttack().waves().size());
        assertTrue(m.npc().initial().isEmpty());assertEquals(1,m.spawnCamps().size());assertEquals(60,m.npc().playerCapacity());
        assertEquals(m.timeAttack(),m.withClockOverrides(m.clockOverrides()).timeAttack());
        var d=ModernMatchDescriptor.of(m,new ModernWorldService.Limits(2,1,1,2000,1000,new ModernBattleEngine.Limits(2000,0,60000)),true);
        assertEquals(ModernMatchDescriptor.Ruleset.COOPERATIVE_TIME_ATTACK,d.ruleset());assertFalse(d.settlementEnabled());assertTrue(d.journaledResult());
        assertFalse(d.supportedBy(24,ModernSessionProtocol.KNOWN_CAPABILITIES));assertTrue(d.supportedBy(25,ModernSessionProtocol.KNOWN_CAPABILITIES));
    }
    @Test void lightArrowNeedsExplicitQualificationDistinctNamespaceAndVersion30()throws Exception {
        var p=world(temporary);
        p.setProperty(PREFIX+"spellRules",ModernWorldConfiguration.SPELL_RULESET);p.setProperty(PREFIX+"castMillis","1200");
        var without=binding(p);p.setProperty(PREFIX+"npcMagicRules",ModernNpcMagic.RULESET);
        var m=mapping(temporary,p);assertTrue(m.npc().lightArrow());assertTrue(m.spellsEnabled());
        assertFalse(Arrays.equals(without,binding(p)));
        var d=ModernMatchDescriptor.of(m,new ModernWorldService.Limits(2,1,1,2000,1000,new ModernBattleEngine.Limits(2000,0,60000)),true);
        for(int v=1;v<30;v++)assertFalse(d.supportedBy(v,ModernSessionProtocol.KNOWN_CAPABILITIES));
        assertTrue(d.supportedBy(30,ModernSessionProtocol.KNOWN_CAPABILITIES));
        for(String key:List.of("spellRules","castMillis")){var q=(Properties)p.clone();q.remove(PREFIX+key);reject(text(temporary,q));}
        p.setProperty(PREFIX+"npcMagicRules","unknown");reject(text(temporary,p));
    }
    @Test void timeAttackHasteRequiresExplicitTimedRulesAndV45WithoutChangingLegacyPolicy()throws Exception {
        var p=world(temporary);var original=mapping(temporary,p);var old=binding(p);
        assertFalse(original.hasteEnabled());assertThrows(IllegalArgumentException.class,()->original.withHaste(true));
        p.setProperty("shop.allowance","1000");p.setProperty("source.7.weight.rate","1");
        p.setProperty(PREFIX+"itemRules",ModernWorldConfiguration.ITEM_RULESET);
        p.setProperty(PREFIX+"weightRules",ModernWorldConfiguration.WEIGHT_RULESET);
        p.setProperty(PREFIX+"buffRules",ModernWorldConfiguration.TIMED_RULESET);
        p.setProperty(PREFIX+"spellRules",ModernWorldConfiguration.SPELL_RULESET);
        p.setProperty(PREFIX+"castMillis","1200");p.setProperty(PREFIX+"hasteRules",ModernHaste.RULESET);
        var m=mapping(temporary,p);assertTrue(m.hasteEnabled());assertTrue(m.timedEnabled());
        assertEquals(original.timeAttack(),m.timeAttack());assertFalse(Arrays.equals(old,binding(p)));
        assertTrue(m.withClockOverrides(m.clockOverrides()).hasteEnabled());assertThrows(IllegalArgumentException.class,()->m.withHaste(false));
        var d=ModernMatchDescriptor.of(m,new ModernWorldService.Limits(2,1,1,2000,1000,new ModernBattleEngine.Limits(2000,0,60000)),true);
        assertTrue(d.has(ModernMatchDescriptor.TIME_ATTACK|ModernMatchDescriptor.HASTE));assertTrue(d.journaledResult());
        for(int version=1;version<45;version++)assertFalse(d.supportedBy(version,ModernSessionProtocol.KNOWN_CAPABILITIES));
        assertTrue(d.supportedBy(45,ModernSessionProtocol.KNOWN_CAPABILITIES));
        assertFalse(d.supportedBy(45,ModernSessionProtocol.KNOWN_CAPABILITIES&~ModernSessionProtocol.CAP_HASTE));
        for(String key:List.of("itemRules","weightRules","buffRules","spellRules","castMillis","hasteRules")){
            var bad=(Properties)p.clone();bad.remove(PREFIX+key);reject(text(temporary,bad));
        }
        var bad=(Properties)p.clone();bad.setProperty(PREFIX+"greaterHasteRules",ModernGreaterHaste.RULESET);reject(text(temporary,bad));
        var legacy=java.nio.ByteBuffer.allocate(64);assertThrows(IllegalArgumentException.class,()->ModernMatchDescriptor.put(legacy,d));assertEquals(0,legacy.position());
    }
    @Test void requiredPolicyPinsWriterAndBoundsFailBeforeAnySecretReads()throws Exception {
        var p=world(temporary);
        for(String field:List.of("timeAttackRules","timeAttackTower","timeAttackDurationMillis","timeAttackWaves","timeAttackWave0","timeAttackWave1","timeAttackWave2",
                "npcCamp0","npcRules","npcIds","activeTimeoutMillis","sectorPins","manifestSha256")) {
            var q=(Properties)p.clone();q.remove(PREFIX+field);reject(text(temporary,q));
        }
        reject(ModernNpcConfigurationTest.base(temporary)+ModernNpcConfigurationTest.lines(p));
        for(var e:List.of(Map.entry("npcPlayerCapacity","61"),Map.entry("npcCamp1","32788,32778"),Map.entry("npcSpawns","1,0,1,1,1,0"),
                Map.entry("activeTimeoutMillis","60001"),Map.entry("botRules",ModernWorldConfiguration.BOT_RULESET),
                Map.entry("timeAttackDurationMillis","1800001"),Map.entry("timeAttackWaves","FINAL_BOSSES,ORDINARY"),
                Map.entry("timeAttackWave3","45262,32779,32778,2"),Map.entry("timeAttackTower","32778,32778,2665,1"),
                Map.entry("timeAttackWave0","45262,32779,32779,2"),Map.entry("npcMagicRules",ModernNpcMagic.RULESET))) {
            var q=(Properties)p.clone();q.setProperty(PREFIX+e.getKey(),e.getValue());reject(text(temporary,q));
        }
    }
    @Test void namespaceBindsWaveTowerClocksAndBlessingAndAbsentPolicyWritesNothing()throws Exception {
        var p=world(temporary);var original=binding(p);
        for(var e:List.of(Map.entry("timeAttackTower","32779,32779,2667,9007199254740993"),Map.entry("timeAttackWave2","45262,32780,32778,2"),
                Map.entry("timeAttackDurationMillis","21000"),Map.entry("attackMillis","2"),Map.entry("npcCorpseMillis","2"),Map.entry("npcCamp0","32777,32778"))) {
            var q=(Properties)p.clone();q.setProperty(PREFIX+e.getKey(),e.getValue());assertFalse(Arrays.equals(original,binding(q)),e.getKey());
        }
        try(var c=ModernServerConfig.parse(text(temporary,p).replace("INTERNAL_ONE_BLESSED","LEGACY_ZERO_BLESSED").getBytes(StandardCharsets.UTF_8),n->"fixture-only")) {
            assertFalse(Arrays.equals(original,ModernSettlementCoordinator.binding(c)));
        }
        try(var c=ModernNpcConfigurationTest.config(temporary,ModernNpcConfigurationTest.world(temporary))) {
            var bytes=new java.io.ByteArrayOutputStream();c.worlds().writeTimeAttackBinding(new java.io.DataOutputStream(bytes));assertEquals(0,bytes.size());
        }
    }
    @Test void changedQualifiedPlanAndUnqualifiedGeometryCannotReuseConfiguredPolicy()throws Exception {
        var p=world(temporary);try(var c=config(temporary,p)) {
            assertThrows(java.io.IOException.class,c.worlds()::load);var original=encounter(c.worlds());
            var plan=original.timeAttack();var changed=new ModernTimeAttack.Plan(plan.key(),plan.tower(),plan.waves(),plan.durationMillis()+1);
            var foreign=new ModernNpcEncounter(original.key(),original.catalog(),original.sources(),original.lanes(),original.capacity(),original.corpseMillis(),List.of(),original.pathBudget(),original.night(),original.playerCapacity(),null,false,changed);
            assertThrows(java.io.IOException.class,()->c.worlds().load(null,()->false,Map.of(KEY,foreign)));
        }
        p.setProperty(PREFIX+"timeAttackWave2","45262,0,0,2");assertThrows(java.io.IOException.class,()->mapping(temporary,p));
    }
}
