package local.lito381.server;

import static org.junit.jupiter.api.Assertions.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ModernWorldPolicyTest {
    static final String NAMESPACE="1".repeat(64);
    static final ModernWorldBinding.Key KEY=new ModernWorldBinding.Key(1,514);
    @TempDir Path temporary;
    static ModernWorldPolicy.Snapshot snapshot(long revision,List<ModernWorldPolicy.Row> rows){
        return new ModernWorldPolicy.Snapshot(new ModernWorldPolicy.Pin(NAMESPACE,revision,ModernWorldPolicy.contentHash(NAMESPACE,revision,rows)),rows);
    }
    static ModernWorldPolicy.Row row(ModernWorldMap map,ModernWorldBinding.Key key,ModernWorldMap.Cell anchor,List<ModernWorldMap.Cell> camps,ModernWorldService.ClockOverrides clocks){
        return new ModernWorldPolicy.Row(key,map.mapId(),map.manifest().digest(),anchor,camps,clocks);
    }
    static Properties properties(Path root,ModernWorldMap map,List<ModernWorldBinding.Key> keys){
        var p=new Properties();p.setProperty("world.sharedRoot",root.toString().replace('\\','/'));
        p.setProperty("world.bindings",keys.stream().map(k->k.mode()+":"+k.logicalMapId()).collect(java.util.stream.Collectors.joining(",")));
        for(var key:keys){String prefix="world."+key.mode()+"."+key.logicalMapId()+".";
            p.setProperty(prefix+"physicalMap",Integer.toString(map.mapId()));p.setProperty(prefix+"sectors","80008000");
            p.setProperty(prefix+"spawnX","32778");p.setProperty(prefix+"spawnY","32778");p.setProperty(prefix+"stepMillis","200");
            p.setProperty(prefix+"manifestSha256",map.manifest().digest());var sector=map.manifest().sectors().get(0);
            p.setProperty(prefix+"sectorPins","80008000:"+sector.byteLength()+":"+sector.rawSha256());
        }return p;
    }
    ModernWorldMap map(byte[] bytes)throws Exception{
        Files.write(Files.createDirectories(temporary.resolve("map/514")).resolve("80008000.s32"),bytes);
        return ModernWorldMap.fromSectorBytes(514,Map.of(new ModernWorldMap.SectorKey(0x8000,0x8000),bytes),()->false);
    }
    @Test void canonicalPinsBindNamespaceRevisionEveryLayoutValueAndExactSortedSet()throws Exception{
        var map=ModernWorldClockTest.mapping().binding().map();var original=row(map,KEY,new ModernWorldMap.Cell(32778,32778),List.of(),null);
        var pin=snapshot(1,List.of(original));assertEquals(pin,snapshot(1,List.of(original)));
        assertNotEquals(pin.pin().contentSha256(),snapshot(2,List.of(original)).pin().contentSha256());
        assertNotEquals(pin.pin().contentSha256(),ModernWorldPolicy.contentHash("2".repeat(64),1,List.of(original)));
        for(var changed:List.of(row(map,KEY,new ModernWorldMap.Cell(32779,32778),List.of(),null),
                row(map,KEY,original.anchor(),List.of(),new ModernWorldService.ClockOverrides(null,0L,null)),
                row(map,KEY,original.anchor(),List.of(new ModernWorldMap.Cell(32778,32778),new ModernWorldMap.Cell(32800,32800)),null))){
            assertNotEquals(pin.pin().contentSha256(),snapshot(1,List.of(changed)).pin().contentSha256());
            assertThrows(IllegalArgumentException.class,()->new ModernWorldPolicy.Snapshot(pin.pin(),List.of(changed)));
        }
        assertEquals(pin.pin().contentSha256(),snapshot(1,List.of(row(map,KEY,original.anchor(),List.of(),new ModernWorldService.ClockOverrides(null,null,null)))).pin().contentSha256());
        assertThrows(IllegalArgumentException.class,()->snapshot(1,List.of(original,original)));
        var earlier=row(map,new ModernWorldBinding.Key(1,509),original.anchor(),List.of(),null);
        assertThrows(IllegalArgumentException.class,()->snapshot(1,List.of(original,earlier)));
        var mutable=new ArrayList<>(List.of(original));var captured=snapshot(1,mutable);mutable.clear();assertEquals(1,captured.rows().size());
        assertThrows(UnsupportedOperationException.class,()->captured.rows().clear());
    }
    @Test void publishedV1MatchesIndependentBigEndian217ByteGolden(){
        // Independently serialized and SHA-256 checked outside this Java
        // implementation. Freeze the persisted v1 format, not only equality
        // between this writer and a test fixture using that same writer.
        var rows=List.of(
                new ModernWorldPolicy.Row(new ModernWorldBinding.Key(1,509),509,
                        "1178261c2d6a968251f2eadfb2029bd0febade95b7151c15872f355784df87f8",
                        new ModernWorldMap.Cell(32740,32795),List.of(),new ModernWorldService.ClockOverrides(25000,2_000_000_000L,6000)),
                new ModernWorldPolicy.Row(new ModernWorldBinding.Key(1,514),514,
                        "2feecc83de156a03c62e44effb7106067b24098a9ef7250cc471f7bf826da7a8",
                        new ModernWorldMap.Cell(32725,32784),List.of(),new ModernWorldService.ClockOverrides(null,1_000_000_000L,4000)));
        assertEquals("a6df7a70d97b3928ca254673e9faf6021c4cbd4fe5d15f048cc8f1329a546881",
                ModernWorldPolicy.contentHash("5".repeat(64),1,rows));
    }
    @Test void sqlNullClocksRetainLocalThenGlobalWhileZeroIsAnExplicitOverride(){
        var local=new ModernWorldService.ClockOverrides(23000,1L,1400);var global=new ModernBattleEngine.Limits(30000,3_000_000_000L,45000);
        for(int mask=0;mask<8;mask++){
            var sql=new ModernWorldService.ClockOverrides((mask&1)==0?null:25000,(mask&2)==0?null:0L,(mask&4)==0?null:1800000);
            var merged=ModernWorldPolicy.clocks(local,sql).resolve(global);
            assertEquals((mask&1)==0?23000:25000,merged.loadTimeoutMillis());assertEquals((mask&2)==0?1:0,merged.countdownNanos());
            assertEquals((mask&4)==0?1400:1800000,merged.activeTimeoutMillis());
            var fallback=ModernWorldPolicy.clocks(null,sql).resolve(global);assertEquals((mask&1)==0?30000:25000,fallback.loadTimeoutMillis());
        }
        assertSame(local,ModernWorldPolicy.clocks(local,null));
    }
    @Test void actualOverlayChangesOnlyEffectiveLayoutAndClocksAndPreservesOldSnapshot()throws Exception{
        var map=map(ModernWorldMapTest.openSectorBytes());var p=properties(temporary,map,List.of(KEY));
        p.setProperty("world.1.514.activeTimeoutMillis","9000");var config=ModernWorldConfiguration.parse(p);
        var original=config.load().get(KEY);var a=new ModernWorldMap.Cell(32790,32790);var b=new ModernWorldMap.Cell(32805,32805);
        var first=config.load(snapshot(1,List.of(row(map,KEY,a,List.of(a,b),new ModernWorldService.ClockOverrides(null,0L,null)))),()->false).get(KEY);
        var second=config.load(snapshot(2,List.of(row(map,KEY,b,List.of(),new ModernWorldService.ClockOverrides(25000,2L,7000)))),()->false).get(KEY);
        assertEquals(a,first.anchor());assertEquals(List.of(a,b),first.spawnCamps());assertEquals(List.of(b),second.spawnCamps());
        assertEquals(9000,first.clockOverrides().activeTimeoutMillis());assertEquals(7000,second.clockOverrides().activeTimeoutMillis());
        assertEquals(List.of(a,b),first.withClockOverrides(null).spawnCamps());var localAgain=config.load().get(KEY);
        assertEquals(original.anchor(),localAgain.anchor());assertEquals(original.clockOverrides(),localAgain.clockOverrides());
        assertEquals(original.binding().map().manifest(),first.binding().map().manifest());assertFalse(first.combatEnabled());assertFalse(first.serverBots());
        assertEquals(a,first.anchor(),"later SQL snapshot cannot mutate an already loaded server mapping");
    }
    @Test void missingExtraUnpinnedWrongManifestAndWrongPhysicalNeverFallBackEvenWithCachedGeometry()throws Exception{
        var map=map(ModernWorldMapTest.openSectorBytes());var other=new ModernWorldBinding.Key(1,509);var p=properties(temporary,map,List.of(other,KEY));
        var config=ModernWorldConfiguration.parse(p);var a=new ModernWorldMap.Cell(32790,32790);
        assertThrows(Exception.class,()->config.load(snapshot(1,List.of(row(map,KEY,a,List.of(),null))),()->false));
        var wrong=new ModernWorldPolicy.Row(KEY,514,"2".repeat(64),a,List.of(),null);
        assertThrows(Exception.class,()->config.load(snapshot(1,List.of(row(map,other,a,List.of(),null),wrong)),()->false));
        var wrongPhysical=new ModernWorldPolicy.Row(KEY,509,map.manifest().digest(),a,List.of(),null);
        assertThrows(Exception.class,()->config.load(snapshot(1,List.of(row(map,other,a,List.of(),null),wrongPhysical)),()->false));
        var single=ModernWorldConfiguration.parse(properties(temporary,map,List.of(KEY)));
        assertThrows(Exception.class,()->single.load(snapshot(1,List.of(row(map,other,a,List.of(),null),row(map,KEY,a,List.of(),null))),()->false));
        var unpinned=properties(temporary,map,List.of(KEY));unpinned.remove("world.1.514.manifestSha256");unpinned.remove("world.1.514.sectorPins");
        assertThrows(Exception.class,()->ModernWorldConfiguration.parse(unpinned).load(snapshot(1,List.of(row(map,KEY,a,List.of(),null))),()->false));
        assertThrows(Exception.class,()->single.load(snapshot(1,List.of(row(map,KEY,new ModernWorldMap.Cell(Integer.MAX_VALUE,0),List.of(),null))),()->false));
        assertThrows(Exception.class,()->single.load(snapshot(1,List.of(row(map,KEY,a,List.of(),null))),()->true));
    }
    @Test void twoCampsRequireRealTeamModeAndNeverInventHonorOrTowerPermission()throws Exception{
        var map=ModernWorldClockTest.mapping().binding().map();var camps=List.of(new ModernWorldMap.Cell(32778,32778),new ModernWorldMap.Cell(32800,32800));
        assertThrows(IllegalArgumentException.class,()->row(map,new ModernWorldBinding.Key(0,565),camps.get(0),camps,null));
        assertThrows(IllegalArgumentException.class,()->row(map,KEY,camps.get(0),List.of(camps.get(0)),null));
        assertThrows(IllegalArgumentException.class,()->row(map,KEY,camps.get(0),List.of(camps.get(0),camps.get(0)),null));
        var mapping=new ModernWorldService.Mapping(new ModernWorldBinding(1,514,map),camps.get(0),200_000_000,0,0,false,false,false,false,false,false,false,null,false,null,camps);
        assertNull(mapping.honor());assertFalse(mapping.combatEnabled());ModernWorldPolicy.qualifyLayout(mapping,100,()->false);
    }
    @Test void petRaceQualificationUsesTenCapacityButNeverGrantsRaceGameplay()throws Exception{
        byte[] bytes=ModernWorldMapTest.openSectorBytes();
        for(int y=0;y<64;y++)for(int x=0;x<64;x++)if(x>=10||y>=10)ModernWorldMapTest.setAttributes(bytes,x,y,(short)1,(short)1);
        var map=map(bytes);var key=new ModernWorldBinding.Key(0,565);var p=properties(temporary,map,List.of(key));
        p.setProperty("world.0.565.spawnX","32770");p.setProperty("world.0.565.spawnY","32770");
        var config=ModernWorldConfiguration.parse(p);var selected=row(map,key,new ModernWorldMap.Cell(32770,32770),List.of(),null);
        var loaded=config.load(snapshot(1,List.of(selected)),()->false).get(key);
        assertEquals(ModernMatchDescriptor.Ruleset.SCENE_ONLY,ModernMatchDescriptor.of(loaded,ModernWorldClockTest.globals(),false).ruleset());
        assertEquals(10,TournamentRoomPolicy.combatantCapacity(100,BattleRules.Mode.PET_RACE));
    }
    @Test void honorSqlCampsPreserveLocalTowerFactsAndOutboxInputWhileRejectingOccupiedObjectives()throws Exception{
        var map=map(ModernWorldMapTest.openSectorBytes());var key=new ModernWorldBinding.Key(5,532);var p=properties(temporary,map,List.of(key));String prefix="world.5.532.";
        p.setProperty(prefix+"combatRules",ModernPhysicalCombat.RULESET);p.setProperty(prefix+"attackMillis","1000");
        p.setProperty(prefix+"spellRules",ModernWorldConfiguration.DAMAGE_SPELL_RULESET);p.setProperty(prefix+"castMillis","1000");
        p.setProperty(prefix+"itemRules",ModernWorldConfiguration.ITEM_RULESET);p.setProperty(prefix+"weightRules",ModernWorldConfiguration.WEIGHT_RULESET);
        p.setProperty(prefix+"regenRules",ModernWorldConfiguration.REGEN_RULESET);p.setProperty(prefix+"honorRules",ModernHonor.RULESET);
        p.setProperty(prefix+"tower0","32790,32790,2665,3000");p.setProperty(prefix+"tower1","32810,32810,2669,3000");
        p.setProperty(prefix+"spawn0","32791,32790");p.setProperty(prefix+"spawn1","32811,32810");var config=ModernWorldConfiguration.parse(p);
        var bytesBefore=new java.io.ByteArrayOutputStream();config.writeHonorBinding(new java.io.DataOutputStream(bytesBefore));
        var original=config.load().get(key);var camps=List.of(new ModernWorldMap.Cell(32792,32790),new ModernWorldMap.Cell(32812,32810));
        var effective=config.load(snapshot(1,List.of(row(map,key,new ModernWorldMap.Cell(32778,32778),camps,null))),()->false).get(key);
        assertEquals(original.honor().towers(),effective.honor().towers());assertEquals(camps,effective.spawnCamps());
        assertEquals(original.honor().teamSpawns(),config.load().get(key).honor().teamSpawns());
        var bytesAfter=new java.io.ByteArrayOutputStream();config.writeHonorBinding(new java.io.DataOutputStream(bytesAfter));assertArrayEquals(bytesBefore.toByteArray(),bytesAfter.toByteArray());
        assertThrows(Exception.class,()->config.load(snapshot(2,List.of(row(map,key,original.anchor(),List.of(original.honor().towers().get(0).cell(),camps.get(1)),null))),()->false));
        assertThrows(Exception.class,()->config.load(snapshot(3,List.of(row(map,key,original.honor().towers().get(0).cell(),List.of(),null))),()->false));
    }
}
