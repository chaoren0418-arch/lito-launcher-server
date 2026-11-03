package local.lito381.server;

import static org.junit.jupiter.api.Assertions.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ModernNpcConfigurationTest {
    static final String PREFIX="world.2.554.";
    static final ModernWorldBinding.Key KEY=new ModernWorldBinding.Key(2,554);
    @TempDir Path temporary;
    static Properties world(Path root)throws Exception {
        var bytes=ModernWorldMapTest.openSectorBytes();
        var sector=new ModernWorldMap.SectorKey(0x8000,0x8000);
        var map=ModernWorldMap.fromSectorBytes(554,Map.of(sector,bytes),()->false);
        Files.createDirectories(root.resolve("map/554"));Files.write(root.resolve("map/554/80008000.s32"),bytes);
        var p=new Properties();p.setProperty("world.sharedRoot",root.toString().replace('\\','/'));p.setProperty("world.bindings","2:554");
        var values=Map.ofEntries(Map.entry("physicalMap","554"),Map.entry("sectors","80008000"),
                Map.entry("spawnX","32778"),Map.entry("spawnY","32778"),Map.entry("stepMillis","100"),
                Map.entry("combatRules",ModernPhysicalCombat.RULESET),Map.entry("attackMillis","1000"),
                Map.entry("manifestSha256",map.digest()),Map.entry("sectorPins","80008000:"+bytes.length+":"+map.manifest().sectors().get(0).rawSha256()),
                Map.entry("npcRules",ModernNpcMelee.RULESET),Map.entry("npcCatalogSource","7"),Map.entry("npcCatalogDatabase","catalog"),
                Map.entry("npcIds",Integer.toString(ModernNpcMeleeTest.NPC)),Map.entry("npcSpawns",ModernNpcMeleeTest.NPC+",0,1,32779,32778,2"),
                Map.entry("npcCapacity","128"),Map.entry("npcPlayerCapacity","100"),Map.entry("npcCorpseMillis","1000"),Map.entry("npcPathBudget","16"),
                Map.entry("npcClock",ModernNpcConfiguration.CLOCK),Map.entry("npcNightStartSecond","64800"),Map.entry("npcNightEndSecond","21600"),
                Map.entry("npcCamp0","32778,32778"),Map.entry("npcCamp1","32788,32778"));
        values.forEach((k,v)->p.setProperty(PREFIX+k,v));return p;
    }
    static String lines(Properties p){return p.stringPropertyNames().stream().sorted().map(k->k+"="+p.getProperty(k)+"\n").reduce("",String::concat);}
    static String base(Path root) {
        return "schema=1\nbind.port=0\ntls.pkcs12="+root.resolve("unused.p12").toString().replace('\\','/')
                +"\ntls.password.env=T\nequipment.storeDirectory="+root.toString().replace('\\','/')
                +"\nsources=7\nsource.7.name=Configured\nsource.7.mysql.host=127.0.0.1\nsource.7.mysql.port=3306"
                +"\nsource.7.mysql.database=not_a_live_database\nsource.7.mysql.username.env=U\nsource.7.mysql.password.env=P"
                +"\nsource.7.mysql.passwordScheme=plain\nsource.7.mysql.tls=disabled\nsource.7.catalog.database=catalog"
                +"\nsource.7.npc.blessingConvention=INTERNAL_ONE_BLESSED\n";
    }
    static ModernServerConfig config(Path root,Properties p)throws Exception {
        return ModernServerConfig.parse((base(root)+lines(p)).getBytes(StandardCharsets.UTF_8),name->"unused-fixture-secret");
    }
    static Map<ModernWorldBinding.Key,ModernNpcEncounter> encounter(ModernWorldConfiguration parsed) {
        var spec=parsed.npcSpecs().get(KEY);
        var old=ModernNpcMeleeTest.catalog();var original=old.npcs().get(ModernNpcMeleeTest.NPC);
        var npc=new ModernNpcCatalog.Npc(new ModernNpcCatalog.Key(spec.catalogSource(),spec.catalogDatabase(),original.key().npcId()),
                original.name(),original.nameId(),original.implementation(),original.large(),original.facts(),original.hasMobskills());
        var catalog=new ModernNpcCatalog.Catalog(spec.catalogSource(),spec.catalogDatabase(),Map.of(npc.key().npcId(),npc),
                new ModernPhysicalCombat.Growth(spec.catalogSource(),spec.catalogDatabase(),old.growth().strength(),old.growth().dexterity()));
        assertThrows(IllegalArgumentException.class,()->spec.qualify(old,Map.of(7,ModernNpcServiceTest.Fixture.blessing(7)),()->0));
        return Map.of(KEY,spec.qualify(catalog,Map.of(7,ModernNpcServiceTest.Fixture.blessing(7)),
                ()->ClassicLineageClock.REAL_BASE_EPOCH_MILLIS));
    }
    @Test void explicitNpcConfigurationQualifiesGeometryAndCannotLoadWithoutCatalog()throws Exception {
        var p=world(temporary);try(var config=config(temporary,p)) {
            assertTrue(config.worlds().npcEnabled());assertTrue(config.worlds().combatEnabled());assertFalse(config.worlds().spellsEnabled());
            assertEquals(ModernNpcMelee.BlessingConvention.INTERNAL_ONE_BLESSED,config.sources().get(0).npcBlessing());
            assertThrows(java.io.IOException.class,config.worlds()::load);
            var mapping=config.worlds().load(null,()->false,encounter(config.worlds())).get(KEY);
            assertNotNull(mapping.npc());assertEquals(mapping.teamCamps(),mapping.npc().lanes());
            assertEquals(1,mapping.npc().initial().size());assertFalse(mapping.npc().night().getAsBoolean());
            assertEquals(128,mapping.npc().capacity());assertEquals("ModernNpcSpec[redacted]",config.worlds().npcSpecs().get(KEY).toString());
        }
    }
    @Test void everyNpcFieldAndResourcePinIsRequiredBeforeSecrets()throws Exception {
        var p=world(temporary);
        var required=new java.util.ArrayList<>(ModernNpcConfiguration.SUFFIXES);required.remove("npcMagicRules");required.remove("npcDisintegrateRules");required.remove("npcFireStormRules");required.addAll(List.of("manifestSha256","sectorPins","combatRules","attackMillis"));
        for(String field:required){var bad=(Properties)p.clone();bad.remove(PREFIX+field);rejectBeforeSecrets(base(temporary)+lines(bad));}
        rejectBeforeSecrets(base(temporary).replace("source.7.npc.blessingConvention=INTERNAL_ONE_BLESSED\n","")+lines(p));
        rejectBeforeSecrets(base(temporary).replace("INTERNAL_ONE_BLESSED","AUTO")+lines(p));
    }
    private void rejectBeforeSecrets(String text) {
        var reads=new AtomicInteger();var error=assertThrows(ModernServerConfig.ConfigException.class,()->ModernServerConfig.parse(
                text.getBytes(StandardCharsets.UTF_8),name->{reads.incrementAndGet();return "must-not-be-consulted";}));
        assertEquals(0,reads.get());assertNull(error.getCause());assertFalse(error.toString().contains("must-not-be-consulted"));
    }
    @Test void invalidTargetsDuplicateSpawnsRulesAndUnsupportedFeaturesFailClosed()throws Exception {
        var p=world(temporary);var invalid=List.of(
                Map.entry("npcCatalogSource","42"),Map.entry("npcCatalogDatabase","catalog;drop"),
                Map.entry("npcIds",ModernNpcMeleeTest.NPC+","+ModernNpcMeleeTest.NPC),Map.entry("npcCapacity","0"),Map.entry("npcCapacity","129"),
                Map.entry("npcSpawns",p.getProperty(PREFIX+"npcSpawns")+";"+p.getProperty(PREFIX+"npcSpawns")),
                Map.entry("npcSpawns","1,0,1,32779,32778,2"),Map.entry("npcCamp0","32779,32778"),
                Map.entry("npcClock","AUTO"),Map.entry("npcNightEndSecond","64800"),Map.entry("npcNightStartSecond","86400"),
                Map.entry("npcPathBudget","65"),Map.entry("npcRules","DEFAULT"),Map.entry("npcCorpseMillis","60001"),
                Map.entry("npcPlayerCapacity","0"),Map.entry("npcPlayerCapacity","1"),Map.entry("npcPlayerCapacity","101"));
        for(var item:invalid){var bad=(Properties)p.clone();bad.setProperty(PREFIX+item.getKey(),item.getValue());rejectBeforeSecrets(base(temporary)+lines(bad));}
        var spells=(Properties)p.clone();spells.setProperty(PREFIX+"spellRules",ModernWorldConfiguration.SPELL_RULESET);spells.setProperty(PREFIX+"castMillis","500");
        try(var config=config(temporary,spells)){assertTrue(config.worlds().spellsEnabled());assertFalse(config.worlds().damageMagicEnabled());}
        spells.setProperty(PREFIX+"spellRules",ModernWorldConfiguration.DAMAGE_SPELL_RULESET);
        rejectBeforeSecrets(base(temporary)+lines(spells));
        var wrong=new Properties();p.forEach((k,v)->wrong.setProperty(k.toString().replace("2.554","1.514"),v.toString().replace("2:554","1:514")));
        rejectBeforeSecrets(base(temporary)+lines(wrong));
    }
    @Test void fullResidentBoundIsRepresentableWithoutRelaxingOtherProperties()throws Exception {
        var p=world(temporary);var rows=new java.util.ArrayList<String>();
        for(int i=0;i<128;i++)rows.add(ModernNpcMeleeTest.NPC+",0,1,"+(32800+i%16)+","+(32800+i/16)+",2");
        p.setProperty(PREFIX+"npcSpawns",String.join(";",rows));
        assertTrue(p.getProperty(PREFIX+"npcSpawns").length()>1024);
        try(var config=config(temporary,p)){assertEquals(128,config.worlds().npcSpecs().get(KEY).spawns().size());}
        p.setProperty(PREFIX+"npcSpawns",String.join(";",rows)+";"+ModernNpcMeleeTest.NPC+",0,1,32900,32900,2");
        rejectBeforeSecrets(base(temporary)+lines(p));
        rejectBeforeSecrets(base(temporary).replace("source.7.name=Configured","source.7.name="+"a".repeat(1025))+lines(world(temporary)));
    }
    @Test void noNpcDefaultsDoNotReadCatalogAndStrayBlessingIsRejected()throws Exception {
        var p=world(temporary);for(String suffix:ModernNpcConfiguration.SUFFIXES)p.remove(PREFIX+suffix);
        p.remove(PREFIX+"combatRules");p.remove(PREFIX+"attackMillis");
        rejectBeforeSecrets(base(temporary)+lines(p));
        var text=base(temporary).replace("source.7.npc.blessingConvention=INTERNAL_ONE_BLESSED\n","")+lines(p);
        try(var config=ModernServerConfig.parse(text.getBytes(StandardCharsets.UTF_8),name->"unused-fixture-secret")) {
            assertFalse(config.worlds().npcEnabled());assertNull(config.sources().get(0).npcBlessing());
            assertTrue(ModernNpcConfiguration.load(config).isEmpty());
            assertNull(config.worlds().load().get(KEY).npc());
        }
    }
    @Test void clockMatchesRetainedAlignmentAndWrapsNightWithoutNativeInt32TimeLimit() {
        long base=ClassicLineageClock.REAL_BASE_EPOCH_MILLIS;
        for(long elapsed:List.of(0L,499L,500L,123456789L,50_000_000_000L)) {
            int nativeSeconds=ClassicLineageClock.atEpochMillis(base+elapsed);
            assertEquals(ClassicLineageClock.nativeClientSecondOfDay(nativeSeconds),ModernNpcConfiguration.secondOfDay(base+elapsed));
        }
        assertFalse(ModernNpcConfiguration.nightAt(base,64800,21600)); // 08:00 Taipei.
        long at18=base+10*60*60*1000L/6;long at6=base+22*60*60*1000L/6;
        assertTrue(ModernNpcConfiguration.nightAt(at18,64800,21600));assertFalse(ModernNpcConfiguration.nightAt(at6,64800,21600));
        assertFalse(ModernNpcConfiguration.nightAt(at18,21600,64800));
        assertThrows(IllegalArgumentException.class,()->ModernNpcConfiguration.nightAt(base,1,1));
        assertThrows(IllegalArgumentException.class,()->ModernNpcConfiguration.secondOfDay(base-1));
        long future=base+400_000_000_000L;assertThrows(IllegalArgumentException.class,()->ClassicLineageClock.atEpochMillis(future));
        assertTrue(ModernNpcConfiguration.secondOfDay(future)>=0);
    }
    @Test void sqlTeamCampsOverrideNpcLanesTogetherAndCannotOverlapInitialNpcs()throws Exception {
        var properties=world(temporary);properties.setProperty(PREFIX+"npcPlayerCapacity","48");
        var parsed=ModernWorldConfiguration.parse(properties);var encounters=encounter(parsed);
        var first=parsed.load(null,()->false,encounters).get(KEY);
        var camps=List.of(new ModernWorldMap.Cell(32778,32780),new ModernWorldMap.Cell(32788,32780));
        var mapping=parsed.load(policy(first,camps),()->false,encounters).get(KEY);
        assertEquals(camps,mapping.teamCamps());assertEquals(camps,mapping.npc().lanes());
        assertEquals(first.npc().initial(),mapping.npc().initial());
        assertEquals(48,mapping.npc().playerCapacity());
        var bad=List.of(first.npc().initial().get(0).cell(),camps.get(1));
        assertThrows(java.io.IOException.class,()->parsed.load(policy(first,bad),()->false,encounters));
        assertThrows(java.io.IOException.class,()->parsed.load(null,()->true,encounters));
    }
    static ModernWorldPolicy.Snapshot policy(ModernWorldService.Mapping m,List<ModernWorldMap.Cell> camps) {
        var rows=List.of(new ModernWorldPolicy.Row(KEY,554,m.binding().map().digest(),m.anchor(),camps,null));String ns="a".repeat(64);
        return new ModernWorldPolicy.Snapshot(new ModernWorldPolicy.Pin(ns,1,ModernWorldPolicy.contentHash(ns,1,rows)),rows);
    }
    @Test void trustedStagedPlanSurvivesSqlCampOverrideAndEveryFutureWaveIsRequalified()throws Exception {
        var properties=world(temporary);properties.setProperty(PREFIX+"npcPlayerCapacity","4");
        properties.setProperty(PREFIX+"npcSpawns",properties.getProperty(PREFIX+"npcSpawns")+";"+ModernNpcMeleeTest.NPC+",1,1,32789,32778,2");
        var parsed=ModernWorldConfiguration.parse(properties);var old=encounter(parsed).get(KEY);
        var plan=ModernQuestProgressTest.plan(KEY,old.catalog());
        var staged=new ModernNpcEncounter(KEY,old.catalog(),old.sources(),old.lanes(),old.capacity(),old.corpseMillis(),plan.firstWaves(),old.pathBudget(),old.night(),4,plan);
        var input=Map.of(KEY,staged);var first=parsed.load(null,()->false,input).get(KEY);
        var camps=List.of(new ModernWorldMap.Cell(32778,32780),new ModernWorldMap.Cell(32788,32780));
        var changed=parsed.load(policy(first,camps),()->false,input).get(KEY);
        assertEquals(camps,changed.npc().lanes());assertSame(plan,changed.npc().questPlan());assertEquals(4,changed.npc().playerCapacity());
        // This future-only birth is valid before an override but cannot become
        // a new camp: no already-loaded plan skips geometry qualification.
        var waves=new java.util.ArrayList<>(plan.waves());var spawn=plan.wave(0,4).spawns().get(0);
        waves.set(3,new ModernQuestProgress.Wave(0,4,List.of(new ModernNpcRoster.Spawn(spawn.template(),0,4,camps.get(0),2))));
        var future=new ModernQuestProgress.Plan(KEY,waves);
        var futureEncounter=new ModernNpcEncounter(KEY,old.catalog(),old.sources(),old.lanes(),old.capacity(),old.corpseMillis(),future.firstWaves(),old.pathBudget(),old.night(),4,future);
        assertNotNull(parsed.load(null,()->false,Map.of(KEY,futureEncounter)).get(KEY));
        assertThrows(java.io.IOException.class,()->parsed.load(policy(first,camps),()->false,Map.of(KEY,futureEncounter)));
    }
}
