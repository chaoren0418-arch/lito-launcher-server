package local.lito381.server;

import static org.junit.jupiter.api.Assertions.*;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ModernPetRaceConfigurationTest {
    @TempDir Path temporary;
    static Properties properties(Path root,ModernWorldMap map,List<ModernPetRace.Gate> gates,long graphics,int step){
        var p=new Properties();String prefix="world.0.565.";p.setProperty("world.sharedRoot",root.toAbsolutePath().toString().replace('\\','/'));p.setProperty("world.bindings","0:565");
        p.setProperty(prefix+"physicalMap",Integer.toString(map.mapId()));p.setProperty(prefix+"sectors",map.manifest().sectors().stream().map(s->String.format("%04x%04x",s.key().x(),s.key().y())).collect(java.util.stream.Collectors.joining(",")));
        var anchor=ModernPetRaceTest.MIDDLE;p.setProperty(prefix+"spawnX",Integer.toString(anchor.x()));p.setProperty(prefix+"spawnY",Integer.toString(anchor.y()));p.setProperty(prefix+"stepMillis",Integer.toString(step));
        p.setProperty(prefix+"manifestSha256",map.digest());p.setProperty(prefix+"sectorPins",map.manifest().sectors().stream().map(s->String.format("%04x%04x",s.key().x(),s.key().y())+":"+s.byteLength()+":"+s.rawSha256()).collect(java.util.stream.Collectors.joining(",")));
        p.setProperty(prefix+"raceRules",ModernPetRace.RULESET);p.setProperty(prefix+"raceGraphicsId",Long.toString(graphics));p.setProperty(prefix+"raceGates",gates.stream().map(g->g.minX()+","+g.maxX()+","+g.minY()+","+g.maxY()).collect(java.util.stream.Collectors.joining(";")));
        p.setProperty(prefix+"raceCourseSha256",ModernPetRace.courseHash(new ModernWorldBinding.Key(0,565),map.manifest(),gates,graphics,step*1_000_000L));return p;
    }
    Properties installed()throws Exception{
        var bytes=ModernWorldMapTest.openSectorBytes();var map=ModernPetRaceTest.map();Files.createDirectories(temporary.resolve("map/5143"));Files.write(temporary.resolve("map/5143/80008000.s32"),bytes);
        return properties(temporary,map,ModernPetRaceTest.gates(),2145,100);
    }
    static String lines(Properties p){return p.stringPropertyNames().stream().sorted().map(k->k+"="+p.getProperty(k)+"\n").reduce("",String::concat);}
    String base(){return "schema=1\nbind.port=0\ntls.pkcs12="+temporary.resolve("unused.p12").toString().replace('\\','/')+"\ntls.password.env=T\nsources=7\nsource.7.name=Source\nsource.7.mysql.host=127.0.0.1\nsource.7.mysql.port=3306\nsource.7.mysql.database=source_only\nsource.7.mysql.username.env=U\nsource.7.mysql.password.env=P\nsource.7.mysql.passwordScheme=plain\nsource.7.mysql.tls=disabled\n";}
    String writer(){return "raceSettlement.enabled=true\nraceSettlement.outboxDirectory="+temporary.toString().replace('\\','/')+"\nraceSettlement.mysql.host=127.0.0.1\nraceSettlement.mysql.port=3306\nraceSettlement.mysql.database=race_only\nraceSettlement.mysql.username.env=R_U\nraceSettlement.mysql.password.env=R_P\nraceSettlement.mysql.tls=disabled\n";}
    @Test void startupLoadsPinnedOrderedCourseAndPreservesClockAndSceneSeparation()throws Exception{
        var p=installed();p.setProperty("world.0.565.activeTimeoutMillis","120000");var config=ModernWorldConfiguration.parse(p);assertTrue(config.raceEnabled());assertFalse(config.combatEnabled());
        var mapping=config.load().get(new ModernWorldBinding.Key(0,565));assertNotNull(mapping.race());assertEquals(2145,mapping.race().baseGraphicsId());assertEquals(ModernPetRaceTest.gates(),mapping.race().gates());
        assertEquals(120000,mapping.withClockOverrides(new ModernWorldService.ClockOverrides(1000,0L,120000)).clockOverrides().activeTimeoutMillis());
        assertSame(mapping.race(),mapping.withClockOverrides(new ModernWorldService.ClockOverrides(1000,0L,120000)).race());
        var descriptor=ModernMatchDescriptor.of(mapping,new ModernWorldService.Limits(4,1,1,3000,50,new ModernBattleEngine.Limits(30000,3_000_000_000L,300000)),false);
        assertEquals(1024,descriptor.features());assertEquals(ModernMatchDescriptor.Ruleset.PET_RACE,descriptor.ruleset());assertEquals(120000,descriptor.activeTimeoutMillis());assertFalse(descriptor.settlementEnabled());
    }
    @Test void everyRaceFieldIsPairedNoNativeFallbackAndExactCoursePinCannotBeBypassed()throws Exception{
        var p=installed();for(String suffix:List.of("raceRules","raceGates","raceGraphicsId","raceCourseSha256","manifestSha256","sectorPins")){
            var bad=(Properties)p.clone();bad.remove("world.0.565."+suffix);assertThrows(ModernServerConfig.ConfigException.class,()->ModernWorldConfiguration.parse(bad),suffix);}
        for(String graphic:List.of("189","2146","-1","2145.0")){var bad=(Properties)p.clone();bad.setProperty("world.0.565.raceGraphicsId",graphic);assertThrows(ModernServerConfig.ConfigException.class,()->ModernWorldConfiguration.parse(bad));}
        var bad=(Properties)p.clone();bad.setProperty("world.0.565.raceCourseSha256","01".repeat(32));assertThrows(IOException.class,()->ModernWorldConfiguration.parse(bad).load());
        var duplicated=(Properties)p.clone();duplicated.setProperty("world.0.565.raceGates",p.getProperty("world.0.565.raceGates")+";"+p.getProperty("world.0.565.raceGates"));assertThrows(ModernServerConfig.ConfigException.class,()->ModernWorldConfiguration.parse(duplicated));
        var bot=(Properties)p.clone();bot.setProperty("world.0.565.botRules",ModernWorldConfiguration.BOT_RULESET);assertThrows(ModernServerConfig.ConfigException.class,()->ModernWorldConfiguration.parse(bot));
    }
    @Test void dedicatedRaceWriterIsRequiredIndependentlyAndSourceCollisionsRejectBeforeSecrets()throws Exception{
        var p=installed();var secrets=new AtomicInteger();String content=base()+lines(p);
        assertThrows(ModernServerConfig.ConfigException.class,()->ModernServerConfig.parse(content.getBytes(StandardCharsets.UTF_8),k->{secrets.incrementAndGet();return "secret";}));assertEquals(0,secrets.get());
        for(String bad:List.of(writer().replace("race_only","source_only"),writer().replace("raceSettlement.enabled=true","raceSettlement.enabled=false"))){
            assertThrows(ModernServerConfig.ConfigException.class,()->ModernServerConfig.parse((content+bad).getBytes(StandardCharsets.UTF_8),k->{secrets.incrementAndGet();return "secret";}));assertEquals(0,secrets.get());}
        try(var config=ModernServerConfig.parse((content+writer()).getBytes(StandardCharsets.UTF_8),k->"synthetic-only")){
            assertNotNull(config.raceSettlement());assertNull(config.settlement());assertNull(config.equipmentStoreDirectory());assertNull(config.sources().get(0).catalogDatabase());assertEquals(32,ModernPetRaceSettlement.binding(config).length);
        }
    }
    @Test void SqlAnchorAndClockOverlayRetainsCourseAndNeverAddsRaceCampsOrMutatesLocalRoute()throws Exception{
        var p=installed();var config=ModernWorldConfiguration.parse(p);var original=config.load().get(new ModernWorldBinding.Key(0,565));
        var row=new ModernWorldPolicy.Row(original.binding().key(),5143,original.binding().map().digest(),ModernPetRaceTest.cell(12,12),List.of(),new ModernWorldService.ClockOverrides(null,0L,120000));
        var rows=List.of(row);String namespace="12".repeat(32);var snapshot=new ModernWorldPolicy.Snapshot(new ModernWorldPolicy.Pin(namespace,2,ModernWorldPolicy.contentHash(namespace,2,rows)),rows);
        var loaded=config.load(snapshot,()->false).get(original.binding().key());assertEquals(row.anchor(),loaded.anchor());assertEquals(original.race().digest(),loaded.race().digest());assertEquals(120000,loaded.clockOverrides().activeTimeoutMillis());assertEquals(original.anchor(),config.load().get(original.binding().key()).anchor());
        assertThrows(IllegalArgumentException.class,()->new ModernWorldPolicy.Row(row.key(),5143,row.manifestSha256(),row.anchor(),List.of(ModernPetRaceTest.cell(10,10),ModernPetRaceTest.cell(12,10)),null));
    }
}
