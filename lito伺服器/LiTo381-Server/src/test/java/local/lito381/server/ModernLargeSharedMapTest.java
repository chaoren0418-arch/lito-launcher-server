package local.lito381.server;

import static org.junit.jupiter.api.Assertions.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

/** Whole shared maps, not cropped manifests or LT resources. */
class ModernLargeSharedMapTest {
    static Set<ModernWorldMap.SectorKey> sectors516() {
        return List.of("7ffd7fff","7ffd8000","7ffe7fff","7ffe8000","7ffe8001","7fff7fff","7fff8000","7fff8001",
                "80007fff","80008000","80008001","80017ffd","80017ffe","80047ffe","80047fff","80048000",
                "80057fff","80058000","80067fff","80068000").stream()
                .map(s->new ModernWorldMap.SectorKey(Integer.parseInt(s.substring(0,4),16),Integer.parseInt(s.substring(4),16)))
                .collect(Collectors.toUnmodifiableSet());
    }
    static Map<ModernWorldMap.SectorKey,byte[]> open(int count) {
        var result=new HashMap<ModernWorldMap.SectorKey,byte[]>();
        for(int i=0;i<count;i++)result.put(new ModernWorldMap.SectorKey(0x8000+i%8,0x8000+i/8),ModernWorldMapTest.openSectorBytes());
        return result;
    }
    @Test void fullSixtyFourSectorGeometryIsBoundedAndStillCrossesSectorEdges()throws Exception {
        var map=ModernWorldMap.fromSectorBytes(516,open(64),()->false);
        assertEquals(64,map.manifest().sectors().size());
        assertEquals(64*4096,map.teleportCells().size());
        var edge=new ModernWorldMap.Cell(32831,32799);var adjacent=new ModernWorldMap.Cell(32832,32799);
        assertTrue(map.canStep(edge,adjacent));assertTrue(map.canStep(adjacent,edge));
    }
    @Test void sixtyFifthSectorAndOriginalByteBudgetsRemainRejected()throws Exception {
        assertEquals(ModernWorldMap.Failure.INVALID_SELECTION,assertThrows(ModernWorldMap.MapException.class,
                ()->ModernWorldMap.fromSectorBytes(516,open(65),()->false)).failure());
        assertEquals(32*1024*1024,ModernWorldMap.MAX_TOTAL_BYTES);
        assertEquals(8*1024*1024,ModernWorldMap.MAX_SECTOR_BYTES);
        var checks=new AtomicInteger();
        assertEquals(ModernWorldMap.Failure.CANCELLED,assertThrows(ModernWorldMap.MapException.class,
                ()->ModernWorldMap.fromSectorBytes(516,open(64),()->checks.incrementAndGet()>20)).failure());
    }
    @Test void actualWholeShared516HasNoOmittedSectors()throws Exception {
        var map=ModernWorldMap.load(ModernDeathmatchSharedMapTest.sharedRoot(),516,sectors516(),()->false);
        assertEquals(20,map.manifest().sectors().size());
        var groups=ModernDeathmatchSharedMapTest.components(map);var anchor=ModernDeathmatchSharedMapTest.centre(groups.get(0));
        assertEquals("a226e91341ea946a4ba0fec2e34b49094a1d713317d4743f41e2f19ad3111a1c",map.digest());
        assertEquals("92004a1a7ea3c790091d37eedb089739635590c5c9657efc4ff84990d92b339c",ModernDeathmatchSharedMapTest.directionDigest(map));
        assertEquals(List.of(6971,2486,2240,1794,1048,874,687,687,131,112,12,4,3,3,2,2),groups.stream().map(Set::size).toList());
        assertEquals(new ModernWorldMap.Cell(33123,32772),anchor);
        System.out.println("LARGE_SHARED map=516 canonical="+map.digest()+" directions="+ModernDeathmatchSharedMapTest.directionDigest(map)
                +" standing="+map.teleportCells().size()+" components="+groups.stream().map(Set::size).toList()+" anchor="+anchor);
        for(int count=2;count<=100;count++)for(int humans:List.of(1,count/2,count)) {
            var roster=ModernWorldSpawnsTest.roster(humans,count-humans);
            var plan=ModernWorldSpawns.allocate(map,roster,List.of(anchor),Set.of(),()->false);
            ModernWorldSpawnsTest.assertPlan(map,roster,plan,Set.of());
        }
    }
    @Test void whole516PresetIsPinnedExplicitAndEquivalentToManualConfiguration()throws Exception {
        var input=ModernSharedDeathmatchPresetTest.worlds(ModernDeathmatchSharedMapTest.sharedRoot(),List.of(516));
        var original=(java.util.Properties)input.clone();
        var expanded=ModernSharedDeathmatchPreset.expand(input);
        assertEquals(original,input);
        assertEquals("SHARED_DEATHMATCH_100_V1",ModernSharedDeathmatchPreset.name(516));
        assertTrue(ModernSharedDeathmatchPreset.HUNDRED_PLAYER_MAPS.contains(516));
        assertArrayEquals(ModernSharedDeathmatchPresetTest.binding(input),ModernSharedDeathmatchPresetTest.binding(expanded));
        var config=ModernWorldConfiguration.parse(input);
        assertTrue(config.completelyPinned());
        var mapping=config.load().get(new ModernWorldBinding.Key(1,516));
        assertEquals(100,mapping.roomCapacity());assertEquals(100,mapping.playerCapacity());
        assertTrue(mapping.combatEnabled());assertTrue(mapping.serverBots());
        assertEquals(20,mapping.binding().map().manifest().sectors().size());
        assertEquals("a226e91341ea946a4ba0fec2e34b49094a1d713317d4743f41e2f19ad3111a1c",mapping.binding().map().digest());
        assertThrows(java.io.IOException.class,()->config.load(null,()->true));
        expanded.setProperty("world.1.516.manifestSha256","a".repeat(64));
        assertThrows(java.io.IOException.class,()->ModernWorldConfiguration.parse(expanded).load());
    }
}
