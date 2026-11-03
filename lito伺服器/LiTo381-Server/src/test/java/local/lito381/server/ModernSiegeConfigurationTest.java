package local.lito381.server;

import static org.junit.jupiter.api.Assertions.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Synthetic shared S32, never claimed to be native map547 geometry. */
class ModernSiegeConfigurationTest {
    static final String PREFIX="world.3.547.";
    @TempDir Path temporary;
    static ModernSiege.Config policy(int count,int duration){var c=ModernSiegeTest.config(547,1,duration,1000);
        var attack=new ArrayList<ModernWorldMap.Cell>();var defend=new ArrayList<ModernWorldMap.Cell>();attack.add(ModernSiegeTest.cell(10,10));defend.add(ModernSiegeTest.cell(31,10));
        for(int i=0;i<count-1;i++){attack.add(ModernSiegeTest.cell(2+i%7,20+i/7));defend.add(ModernSiegeTest.cell(40+i%7,20+i/7));}
        return new ModernSiege.Config(c.key(),c.towers(),c.gate(),attack,defend,c.portal(),duration,1000);}
    static Properties world(Path root,int duration)throws Exception {
        Files.createDirectories(root.resolve("quest-results"));
        var c=policy(50,duration);var p=new Properties();var bytes=ModernWorldMapTest.openSectorBytes();var sector=ModernBattleEngineBotTest.SECTOR;
        var map=ModernWorldMap.fromSectorBytes(4,Map.of(sector,bytes),()->false);Files.createDirectories(root.resolve("map/4"));
        String key=String.format("%04x%04x",sector.x(),sector.y());Files.write(root.resolve("map/4/"+key+".s32"),bytes);
        p.setProperty("world.sharedRoot",root.toString().replace('\\','/'));p.setProperty("world.bindings","3:547");
        Map.ofEntries(Map.entry("physicalMap","4"),Map.entry("sectors",key),Map.entry("spawnX",""+c.attackCamp().get(0).x()),Map.entry("spawnY",""+c.attackCamp().get(0).y()),
                Map.entry("stepMillis","1"),Map.entry("attackMillis","1"),Map.entry("combatRules",ModernPhysicalCombat.RULESET),Map.entry("botRules",ModernWorldConfiguration.BOT_RULESET),
                Map.entry("countdownNanos","0"),Map.entry("activeTimeoutMillis",""+duration),Map.entry("manifestSha256",map.digest()),Map.entry("sectorPins",key+":"+bytes.length+":"+map.manifest().sectors().get(0).rawSha256())).forEach((k,v)->p.setProperty(PREFIX+k,v));
        policy(p,c);return p;
    }
    static String cells(java.util.Collection<ModernWorldMap.Cell> rows){return rows.stream().map(c->c.x()+","+c.y()).collect(java.util.stream.Collectors.joining(";"));}
    static void policy(Properties p,ModernSiege.Config c){p.setProperty(PREFIX+"siegeRules",ModernSiegeConfiguration.RULESET);
        p.setProperty(PREFIX+"siegeTowers",c.towers().stream().map(t->t.cell().x()+","+t.cell().y()+","+t.gfx()+","+t.maximumHp()).collect(java.util.stream.Collectors.joining(";")));
        p.setProperty(PREFIX+"siegeGate",cells(c.gate()));p.setProperty(PREFIX+"siegeAttackCamp",cells(c.attackCamp()));p.setProperty(PREFIX+"siegeDefenseCamp",cells(c.defenseCamp()));
        p.setProperty(PREFIX+"siegePortal",cells(List.of(c.portal().outside(),c.portal().inside())));p.setProperty(PREFIX+"siegeRespawnMillis",""+c.respawnMillis());p.setProperty(PREFIX+"siegePolicySha256",ModernSiegeSettlement.configHash(c));}
    static String text(Path root,Properties p){return base(root)+ModernQuestConfigurationTest.settlement(root)+ModernNpcConfigurationTest.lines(p);}
    static String base(Path root){return ModernNpcConfigurationTest.base(root).replace("source.7.npc.blessingConvention=INTERNAL_ONE_BLESSED\n","");}
    static ModernWorldService.Mapping mapping(Path root,Properties p)throws Exception {try(var config=ModernServerConfig.parse(text(root,p).getBytes(StandardCharsets.UTF_8),n->"fixture-only")){
        assertTrue(config.worlds().siegeEnabled());return config.worlds().load().get(new ModernWorldBinding.Key(3,547));}}
    @Test void explicitPolicyPinsLoadsAndPreservesRoleGeometryAndDescriptor()throws Exception {
        var p=world(temporary,60000);var m=mapping(temporary,p);assertEquals(policy(50,60000),m.siege());assertEquals(m.siege(),m.withClockOverrides(m.clockOverrides()).siege());
        var d=ModernMatchDescriptor.of(m,ModernSiegeServiceTest.limits(),true);assertTrue(d.journaledResult());assertFalse(d.settlementEnabled());
        assertFalse(d.supportedBy(25,ModernSessionProtocol.KNOWN_CAPABILITIES));assertTrue(d.supportedBy(26,ModernSessionProtocol.KNOWN_CAPABILITIES));
        assertFalse(d.supportedBy(26,ModernSessionProtocol.KNOWN_CAPABILITIES&~ModernSessionProtocol.CAP_MATCH_DESCRIPTOR));
    }
    @Test void missingPolicyPinsJournalWrongHashAndUnsupportedMixFailBeforeSecrets()throws Exception {
        var p=world(temporary,60000);for(String field:java.util.stream.Stream.concat(ModernSiegeConfiguration.SUFFIXES.stream(),List.of("manifestSha256","sectorPins","activeTimeoutMillis","combatRules").stream()).toList()){
            var q=(Properties)p.clone();q.remove(PREFIX+field);ModernTimeAttackConfigurationTest.reject(text(temporary,q));}
        ModernTimeAttackConfigurationTest.reject(base(temporary)+ModernNpcConfigurationTest.lines(p));
        for(var entry:List.of(Map.entry("siegePolicySha256","0".repeat(64)),Map.entry("siegeRespawnMillis","0"),Map.entry("siegeGate",p.getProperty(PREFIX+"siegeGate")+";"+p.getProperty(PREFIX+"siegeGate")),Map.entry("spellRules",ModernWorldConfiguration.DAMAGE_SPELL_RULESET))){
            var q=(Properties)p.clone();q.setProperty(PREFIX+entry.getKey(),entry.getValue());ModernTimeAttackConfigurationTest.reject(text(temporary,q));}
    }
    @Test void genuineCutsetRequiredEvenWhenChangedPolicyHashIsValid()throws Exception {
        var p=world(temporary,60000);var c=policy(50,60000);policy(p,ModernSiegeTest.gate(c,java.util.Set.of(ModernSiegeTest.cell(30,10))));
        assertThrows(java.io.IOException.class,()->mapping(temporary,p));
    }
    @Test void noSiegeWritesNoNamespaceBytesAndPolicyChangesRebindOutbox()throws Exception {
        var p=world(temporary,60000);var bytes=new java.io.ByteArrayOutputStream();ModernWorldConfiguration.parse(p).writeSiegeBinding(new java.io.DataOutputStream(bytes));assertTrue(bytes.size()>0);
        var old=bytes.toByteArray();p.setProperty(PREFIX+"attackMillis","2");bytes.reset();ModernWorldConfiguration.parse(p).writeSiegeBinding(new java.io.DataOutputStream(bytes));assertFalse(java.util.Arrays.equals(old,bytes.toByteArray()));
        for(String s:ModernSiegeConfiguration.SUFFIXES)p.remove(PREFIX+s);bytes.reset();ModernWorldConfiguration.parse(p).writeSiegeBinding(new java.io.DataOutputStream(bytes));assertEquals(0,bytes.size());
    }
}
