package local.lito381.server;

import static org.junit.jupiter.api.Assertions.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ModernQuestConfigurationTest {
    static final String PREFIX=ModernNpcConfigurationTest.PREFIX;
    static final ModernWorldBinding.Key KEY=ModernNpcConfigurationTest.KEY;
    @TempDir Path temporary;
    static Properties world(Path root)throws Exception {
        java.nio.file.Files.createDirectories(root.resolve("quest-results"));
        var p=ModernNpcConfigurationTest.world(root);p.remove(PREFIX+"npcSpawns");
        p.setProperty(PREFIX+"questRules",ModernQuestConfiguration.RULESET);
        p.setProperty(PREFIX+"questLives",ModernQuestConfiguration.LIVES);
        p.setProperty(PREFIX+"questObjectives","0,4,32781,32778,2665,9007199254740993;1,4,32781,32778,2671,9223372036854775807");
        p.setProperty(PREFIX+"npcCamp1",p.getProperty(PREFIX+"npcCamp0"));p.setProperty(PREFIX+"npcPlayerCapacity","4");
        for(int lane=0;lane<2;lane++)for(int stage=1;stage<=4;stage++)
            p.setProperty(PREFIX+ModernQuestConfiguration.waveKey(lane,stage),ModernNpcMeleeTest.NPC+","+(32778+stage)+",32779,2");
        p.setProperty(PREFIX+"itemRules",ModernWorldConfiguration.ITEM_RULESET);
        p.setProperty(PREFIX+"weightRules",ModernWorldConfiguration.WEIGHT_RULESET);
        p.setProperty(PREFIX+"regenRules",ModernWorldConfiguration.REGEN_RULESET);return p;
    }
    static String settlement(Path root) {
        return "settlement.enabled=true\nsettlement.outboxDirectory="+root.resolve("quest-results").toString().replace('\\','/')
                +"\nsettlement.mysql.host=127.0.0.1\nsettlement.mysql.port=3306\nsettlement.mysql.database=modern_results"
                +"\nsettlement.mysql.username.env=WU\nsettlement.mysql.password.env=WP\nsettlement.mysql.tls=disabled\n";
    }
    static String text(Path root,Properties p){return ModernNpcConfigurationTest.base(root)+"source.7.weight.rate=1\nshop.allowance=1\n"+settlement(root)+ModernNpcConfigurationTest.lines(p);}
    /** Serialize an explicit fixture policy through the production parser;
     * interop must not inject a hand-built Plan into the loaded world. */
    static Properties properties(ModernWorldService.Mapping m,Path sharedRoot) {
        var p=new Properties();var key=m.binding().key();String prefix="world."+key.mode()+"."+key.logicalMapId()+".";
        p.setProperty("world.sharedRoot",sharedRoot.toString().replace('\\','/'));p.setProperty("world.bindings",key.mode()+":"+key.logicalMapId());
        var manifest=m.binding().map().manifest();var n=m.npc();var plan=n.questPlan();
        p.setProperty(prefix+"physicalMap",Integer.toString(manifest.mapId()));
        p.setProperty(prefix+"sectors",manifest.sectors().stream().map(s->String.format("%04x%04x",s.key().x(),s.key().y())).collect(java.util.stream.Collectors.joining(",")));
        p.setProperty(prefix+"sectorPins",manifest.sectors().stream().map(s->String.format("%04x%04x",s.key().x(),s.key().y())+":"+s.byteLength()+":"+s.rawSha256()).collect(java.util.stream.Collectors.joining(",")));
        p.setProperty(prefix+"manifestSha256",manifest.digest());p.setProperty(prefix+"spawnX",Integer.toString(m.anchor().x()));p.setProperty(prefix+"spawnY",Integer.toString(m.anchor().y()));
        p.setProperty(prefix+"stepMillis",Long.toString(m.stepIntervalNanos()/1_000_000));p.setProperty(prefix+"attackMillis",Long.toString(m.attackIntervalNanos()/1_000_000));
        p.setProperty(prefix+"combatRules",ModernPhysicalCombat.RULESET);p.setProperty(prefix+"npcRules",ModernNpcMelee.RULESET);
        p.setProperty(prefix+"npcCatalogSource",Integer.toString(n.catalog().sourceId()));p.setProperty(prefix+"npcCatalogDatabase",n.catalog().database());
        p.setProperty(prefix+"npcIds",n.catalog().npcs().keySet().stream().sorted().map(Object::toString).collect(java.util.stream.Collectors.joining(",")));
        p.setProperty(prefix+"npcCapacity",Integer.toString(n.capacity()));p.setProperty(prefix+"npcPlayerCapacity",Integer.toString(n.playerCapacity()));
        p.setProperty(prefix+"npcCorpseMillis",Long.toString(n.corpseMillis()));p.setProperty(prefix+"npcPathBudget",Integer.toString(n.pathBudget()));
        p.setProperty(prefix+"npcClock",ModernNpcConfiguration.CLOCK);p.setProperty(prefix+"npcNightStartSecond","64800");p.setProperty(prefix+"npcNightEndSecond","21600");
        for(int lane=0;lane<2;lane++){var c=n.lanes().get(lane);p.setProperty(prefix+"npcCamp"+lane,c.x()+","+c.y());}
        p.setProperty(prefix+"questRules",ModernQuestConfiguration.RULESET);p.setProperty(prefix+"questLives",ModernQuestConfiguration.LIVES);
        for(var w:plan.waves())p.setProperty(prefix+ModernQuestConfiguration.waveKey(w.lane(),w.stage()),w.spawns().stream()
                .map(s->s.template().npcId()+","+s.cell().x()+","+s.cell().y()+","+s.heading()).collect(java.util.stream.Collectors.joining(";")));
        p.setProperty(prefix+"questObjectives",plan.objectives().targets().stream().map(t->t.lane()+","+t.stage()+","+t.cell().x()+","+t.cell().y()+","+t.graphic()+","+t.maximumHp()).collect(java.util.stream.Collectors.joining(";")));
        p.setProperty(prefix+"itemRules",ModernWorldConfiguration.ITEM_RULESET);p.setProperty(prefix+"weightRules",ModernWorldConfiguration.WEIGHT_RULESET);p.setProperty(prefix+"regenRules",ModernWorldConfiguration.REGEN_RULESET);
        return p;
    }
    static ModernServerConfig config(Path root,Properties p)throws Exception{return parse(text(root,p));}
    private static ModernServerConfig parse(String text)throws Exception{return ModernServerConfig.parse(text.getBytes(StandardCharsets.UTF_8),name->"fixture-only");}
    private static void reject(String text) {
        var reads=new AtomicInteger();var error=assertThrows(ModernServerConfig.ConfigException.class,
                ()->ModernServerConfig.parse(text.getBytes(StandardCharsets.UTF_8),name->{reads.incrementAndGet();return "secret-not-read";}));
        assertEquals(0,reads.get());assertNull(error.getCause());assertFalse(error.toString().contains("secret-not-read"));
    }
    private byte[] binding(Properties p)throws Exception{try(var c=config(temporary,p)){return ModernSettlementCoordinator.binding(c);}}
    @Test void explicitEightWavesAndI64ObjectivesProduceTheConfiguredV22Encounter()throws Exception {
        var p=world(temporary);try(var c=config(temporary,p)) {
            assertTrue(c.worlds().questEnabled());assertNotNull(c.settlement());
            var spec=c.worlds().npcSpecs().get(KEY);assertEquals(2,spec.spawns().size());assertNotNull(spec.quest());
            var encounters=ModernNpcConfigurationTest.encounter(c.worlds());
            var m=c.worlds().load(null,()->false,encounters).get(KEY);var plan=m.npc().questPlan();
            assertTrue(plan.participantLives());assertEquals(8,plan.waves().size());assertEquals(plan.firstWaves(),m.npc().initial());
            assertEquals(9007199254740993L,plan.objectives().target(0,4).maximumHp());
            assertEquals(Long.MAX_VALUE,plan.objectives().target(1,4).maximumHp());
            assertEquals(m.teamCamps().get(0),m.teamCamps().get(1));assertEquals("ModernQuestSpec[redacted]",spec.quest().toString());
        }
    }
    @Test void everyQuestFieldDependencyAndSettlementIsRequiredBeforeSecrets()throws Exception {
        var p=world(temporary);var required=new ArrayList<>(ModernQuestConfiguration.SUFFIXES);
        required.addAll(List.of("npcRules","npcIds","npcCatalogSource","npcCatalogDatabase","npcCamp0","npcCamp1","npcCapacity",
                "npcPlayerCapacity","manifestSha256","sectorPins","combatRules","attackMillis","itemRules","weightRules","regenRules"));
        for(String field:required){var bad=(Properties)p.clone();bad.remove(PREFIX+field);reject(text(temporary,bad));}
        reject(text(temporary,p).replace(settlement(temporary),""));
        reject(text(temporary,p).replace("source.7.weight.rate=1\n",""));
        reject(text(temporary,p).replace("shop.allowance=1\n",""));
    }
    @Test void wrongLifePolicyUnsupportedBotsSpellsPartialWavesAndAmbiguousInitialRosterFailClosed()throws Exception {
        var p=world(temporary);String wave=p.getProperty(PREFIX+"questWave0_1");
        var cases=List.of(Map.entry("questRules","AUTO"),Map.entry("questLives","false"),Map.entry("npcSpawns","45262,0,1,32779,32779,2"),
                Map.entry("questWave0_1",wave+";"+wave),Map.entry("questWave1_4","1,32779,32779,2"),
                Map.entry("questWave1_4","45262,32778,32778,2"),Map.entry("questWave1_4","45262,32779,32779,8"),
                Map.entry("questWave1_4","45262,-0,32779,2"),Map.entry("npcCapacity","1"),
                Map.entry("npcIds","45262,45263"),Map.entry("botRules","AUTOMATIC_QUEST_AI"),
                Map.entry("questObjectives","0,4,32781,32778,2665,9223372036854775808;1,4,32781,32778,2671,1"),
                Map.entry("questObjectives","0,4,32778,32778,2665,1;1,4,32781,32778,2671,1"),
                Map.entry("questObjectives","0,3,32781,32778,2665,1;1,4,32781,32778,2671,1"));
        for(var entry:cases){var bad=(Properties)p.clone();bad.setProperty(PREFIX+entry.getKey(),entry.getValue());reject(text(temporary,bad));}
        reject(text(temporary,p)+PREFIX+"questTeleport=enabled\n");
        reject(text(temporary,p)+PREFIX+"spellRules="+ModernWorldConfiguration.DAMAGE_SPELL_RULESET+"\n"+PREFIX+"castMillis=500\n");
        p.setProperty(PREFIX+"spellRules",ModernWorldConfiguration.SPELL_RULESET);p.setProperty(PREFIX+"castMillis","500");
        try(var c=config(temporary,p)){assertTrue(c.worlds().spellsEnabled());assertFalse(c.worlds().damageMagicEnabled());}
    }
    @Test void passiveGmBotsRequireExplicitOptInAndKeepASeparateDurableNamespace()throws Exception {
        var p=world(temporary);var original=binding(p);
        try(var c=config(temporary,p)){assertFalse(c.worlds().botsEnabled());}
        p.setProperty(PREFIX+"botRules",ModernWorldConfiguration.BOT_RULESET);
        try(var c=config(temporary,p)) {
            assertEquals(List.of(KEY),c.worlds().botBindings());
            var m=c.worlds().load(null,()->false,ModernNpcConfigurationTest.encounter(c.worlds())).get(KEY);
            assertTrue(m.serverBots());assertTrue(m.npc().questPlan().participantLives());
            assertTrue(m.withClockOverrides(new ModernWorldService.ClockOverrides(3000,0L,60_000)).serverBots());
        }
        assertFalse(Arrays.equals(original,binding(p)));
        p.remove(PREFIX+"botRules");assertArrayEquals(original,binding(p));
    }
    @Test void symbolMapsRequireExactlyFourOrderedTargetsPerForceAndNoLogicalMapAliasing()throws Exception {
        for(int logical:List.of(513,527)) {
            var p=world(temporary);var q=new Properties();p.forEach((k,v)->q.setProperty(k.toString().replace("2.554.","2."+logical+"."),v.toString()));
            q.setProperty("world.bindings","2:"+logical);var prefix="world.2."+logical+".";
            reject(text(temporary,q));var targets=new ArrayList<String>();
            for(int lane=0;lane<2;lane++)for(int stage=1;stage<=4;stage++)targets.add(lane+","+stage+",32781,32778,2665,1");
            q.setProperty(prefix+"questObjectives",String.join(";",targets));
            try(var c=config(temporary,q)){assertEquals(8,c.worlds().npcSpecs().get(new ModernWorldBinding.Key(2,logical)).quest().objectives().targets().size());}
        }
    }
    @Test void fullResidentBudgetAcrossAllEightWavesFitsStrictPropertiesButAnExtraResidentDoesNot()throws Exception {
        var p=world(temporary);
        for(int lane=0;lane<2;lane++)for(int stage=1;stage<=4;stage++) {
            var rows=new ArrayList<String>();for(int i=0;i<64;i++)rows.add("45262,"+(32800+i%8)+","+(32800+i/8)+",2");
            p.setProperty(PREFIX+ModernQuestConfiguration.waveKey(lane,stage),String.join(";",rows));
        }
        assertTrue(p.getProperty(PREFIX+"questWave0_1").length()>1024);
        try(var c=config(temporary,p)){assertEquals(512,c.worlds().npcSpecs().get(KEY).quest().waves().stream().mapToInt(List::size).sum());}
        p.setProperty(PREFIX+"questWave1_4",p.getProperty(PREFIX+"questWave1_4")+";45262,32820,32820,2");reject(text(temporary,p));
        reject(text(temporary,world(temporary)).replace("source.7.name=Configured","source.7.name="+"a".repeat(1025)));
    }
    @Test void futureBirthsAndObjectivesAreReservedForEachForceNotOnlyForFirstWave()throws Exception {
        var p=world(temporary);var parsed=ModernWorldConfiguration.parse(p);
        var m=parsed.load(null,()->false,ModernNpcConfigurationTest.encounter(parsed)).get(KEY);
        var roster=new ArrayList<ModernRooms.Member>();for(int i=0;i<4;i++)roster.add(new ModernRooms.Member(ModernParticipantId.human(7,i+1),"p","p",80,1,0,i%2,true));
        var positions=ModernQuestSpace.allocate(m,roster,Set.of(),()->false);
        for(int lane=0;lane<2;lane++) {
            var blocked=new HashSet<ModernWorldMap.Cell>();
            for(var w:m.npc().questPlan().waves())if(w.lane()==lane)for(var s:w.spawns())blocked.add(s.cell());
            for(var t:m.npc().questPlan().objectives().targets())if(t.lane()==lane)blocked.add(t.cell());
            for(int i=0;i<4;i++)if(roster.get(i).team()==lane)assertFalse(blocked.contains(positions.get(i)));
        }
        assertEquals(positions.get(0),positions.get(1));assertEquals(positions.get(2),positions.get(3));
    }
    @Test void sqlOverlayRequalifiesFutureWavesAndLoadedStalePlansCannotReplaceConfiguredPolicy()throws Exception {
        var p=world(temporary);var parsed=ModernWorldConfiguration.parse(p);var encounters=ModernNpcConfigurationTest.encounter(parsed);
        var first=parsed.load(null,()->false,encounters).get(KEY);
        var camps=List.of(new ModernWorldMap.Cell(32770,32770),new ModernWorldMap.Cell(32780,32770));
        var changed=parsed.load(ModernNpcConfigurationTest.policy(first,camps),()->false,encounters).get(KEY);
        assertEquals(camps,changed.npc().lanes());assertSame(first.npc().questPlan(),changed.npc().questPlan());
        var bad=List.of(first.npc().questPlan().wave(0,4).spawns().get(0).cell(),camps.get(1));
        assertThrows(java.io.IOException.class,()->parsed.load(ModernNpcConfigurationTest.policy(first,bad),()->false,encounters));
        p.setProperty(PREFIX+"questWave0_4","45262,32799,32799,2");var next=ModernWorldConfiguration.parse(p);
        assertThrows(java.io.IOException.class,()->next.load(null,()->false,encounters));
        assertNotNull(next.load(null,()->false,ModernNpcConfigurationTest.encounter(next)).get(KEY));
        p.setProperty(PREFIX+"questWave0_4","45262,123,123,2");var outside=ModernWorldConfiguration.parse(p);
        assertThrows(java.io.IOException.class,()->outside.load(null,()->false,ModernNpcConfigurationTest.encounter(outside)));
    }
    @Test void durableRoutingIncludesEveryFutureWaveAndTargetPlusLifetimeAndPlacementPolicy()throws Exception {
        var p=world(temporary);byte[] original=binding(p);
        for(int lane=0;lane<2;lane++)for(int stage=1;stage<=4;stage++) {
            var q=(Properties)p.clone();q.setProperty(PREFIX+ModernQuestConfiguration.waveKey(lane,stage),"45262,32799,32799,7");assertFalse(Arrays.equals(original,binding(q)));
        }
        var changes=Map.ofEntries(Map.entry("questObjectives",p.getProperty(PREFIX+"questObjectives").replace("9007199254740993","9007199254740994")),
                Map.entry("npcCatalogDatabase","other_catalog"),Map.entry("npcCamp1","32788,32778"),Map.entry("npcCapacity","127"),
                Map.entry("npcCorpseMillis","999"),Map.entry("npcPathBudget","15"),Map.entry("npcPlayerCapacity","6"),
                Map.entry("npcNightStartSecond","64000"),Map.entry("npcNightEndSecond","21000"),Map.entry("stepMillis","101"),
                Map.entry("attackMillis","1001"),Map.entry("spawnX","32777"),Map.entry("countdownNanos","0"));
        for(var e:changes.entrySet()){var q=(Properties)p.clone();q.setProperty(PREFIX+e.getKey(),e.getValue());assertFalse(Arrays.equals(original,binding(q)),e.getKey());}
        var casting=(Properties)p.clone();casting.setProperty(PREFIX+"spellRules",ModernWorldConfiguration.SPELL_RULESET);casting.setProperty(PREFIX+"castMillis","1200");
        var castBinding=binding(casting);assertFalse(Arrays.equals(original,castBinding));casting.setProperty(PREFIX+"castMillis","1201");assertFalse(Arrays.equals(castBinding,binding(casting)));
        try(var renamed=parse(text(temporary,p).replace("name=Configured","name=DisplayOnly"))){assertArrayEquals(original,ModernSettlementCoordinator.binding(renamed));}
        try(var blessing=parse(text(temporary,p).replace("INTERNAL_ONE_BLESSED","LEGACY_ZERO_BLESSED"))){assertFalse(Arrays.equals(original,ModernSettlementCoordinator.binding(blessing)));}
        java.nio.file.Files.createDirectory(temporary.resolve("binding-box"));
        try(var box=new ModernMatchOutbox(temporary.resolve("binding-box"),original)){assertTrue(box.isHealthy());}
        p.setProperty(PREFIX+"questWave1_4","45262,32799,32799,2");var changed=binding(p);
        assertThrows(java.io.IOException.class,()->new ModernMatchOutbox(temporary.resolve("binding-box"),changed));
        try(var reopened=new ModernMatchOutbox(temporary.resolve("binding-box"),original)){assertTrue(reopened.isHealthy());}
    }
}
