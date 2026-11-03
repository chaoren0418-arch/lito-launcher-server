package local.lito381.server;

import static org.junit.jupiter.api.Assertions.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ModernWorldMapQualificationTest {
    static final ModernWorldMap.SectorKey SECTOR = new ModernWorldMap.SectorKey(0x7fff, 0x8000);
    static final ModernWorldMap.Cell ANCHOR = new ModernWorldMap.Cell(32724, 32784);
    static ModernWorldMap map() throws Exception {
        return ModernWorldMap.fromSectorBytes(509, Map.of(SECTOR, ModernWorldMapTest.openSectorBytes()), () -> false);
    }
    static ModernWorldMapQualification.Expected expected(ModernWorldMap.Manifest manifest) {
        return new ModernWorldMapQualification.Expected(new ModernWorldBinding.Key(1, 509), manifest, ANCHOR, 100);
    }
    @Test void exactImmutableEvidenceQualifiesOneHundredConnectedSpawnsAndAnExplicitLogicalBinding() throws Exception {
        var map = map(); var expected = expected(map.manifest());
        var binding = ModernWorldMapQualification.qualify(map, expected);
        assertEquals(new ModernWorldBinding.Key(1,509), binding.key()); assertSame(map, binding.map());
        var cells = map.serverChosenSpawns(ANCHOR,100); assertEquals(100,cells.size()); assertEquals(100,cells.stream().distinct().count()); map.validateSpawns(cells);
        assertEquals(java.util.Set.of(SECTOR), expected.sectors()); assertThrows(UnsupportedOperationException.class,()->expected.sectors().clear());
        var explicitAlias = new ModernWorldMapQualification.Expected(new ModernWorldBinding.Key(1,514),map.manifest(),ANCHOR,100);
        assertEquals(509,ModernWorldMapQualification.qualify(map,explicitAlias).map().mapId());
    }
    @Test void sameCanonicalCannotHideWrongRawHashLengthPhysicalIdOrSectorSet() throws Exception {
        var map=map();var m=map.manifest();var sector=m.sectors().get(0);
        var wrong = List.of(new ModernWorldMap.Manifest(514,m.sectors(),m.digest()),
                new ModernWorldMap.Manifest(509,m.sectors(),"a".repeat(64)),
                new ModernWorldMap.Manifest(509,List.of(new ModernWorldMap.SectorManifest(SECTOR,sector.byteLength()+1,sector.rawSha256())),m.digest()),
                new ModernWorldMap.Manifest(509,List.of(new ModernWorldMap.SectorManifest(SECTOR,sector.byteLength(),"b".repeat(64))),m.digest()),
                new ModernWorldMap.Manifest(509,List.of(sector,new ModernWorldMap.SectorManifest(new ModernWorldMap.SectorKey(0x7fff,0x8001),sector.byteLength(),sector.rawSha256())),m.digest()));
        for(var forged:wrong) assertEquals(ModernWorldMap.Failure.CHANGED_FILE,
                assertThrows(ModernWorldMap.MapException.class,()->ModernWorldMapQualification.qualify(map,expected(forged))).failure());
    }
    @Test void malformedEvidenceIsRejectedBeforeItCouldBecomeAQualificationToken() throws Exception {
        var m=map().manifest();var sector=m.sectors().get(0);
        for(String hash:List.of("", "0".repeat(64), "A".repeat(64), "a".repeat(63), "a".repeat(65), " "+m.digest()))
            assertThrows(IllegalArgumentException.class,()->expected(new ModernWorldMap.Manifest(509,m.sectors(),hash)));
        for(int length:new int[]{0,49157,ModernWorldMap.MAX_SECTOR_BYTES+1})
            assertThrows(IllegalArgumentException.class,()->expected(new ModernWorldMap.Manifest(509,List.of(new ModernWorldMap.SectorManifest(SECTOR,length,sector.rawSha256())),m.digest())));
        assertThrows(IllegalArgumentException.class,()->expected(new ModernWorldMap.Manifest(509,List.of(),m.digest())));
        assertThrows(IllegalArgumentException.class,()->expected(new ModernWorldMap.Manifest(509,List.of(sector,sector),m.digest())));
        var many=new ArrayList<ModernWorldMap.SectorManifest>();
        for(int n=0;n<5;n++)many.add(new ModernWorldMap.SectorManifest(new ModernWorldMap.SectorKey(0x7fff,0x8000+n),ModernWorldMap.MAX_SECTOR_BYTES,sector.rawSha256()));
        assertThrows(IllegalArgumentException.class,()->expected(new ModernWorldMap.Manifest(509,many,m.digest())));
        for(int capacity:new int[]{0,101})assertThrows(IllegalArgumentException.class,()->new ModernWorldMapQualification.Expected(new ModernWorldBinding.Key(1,509),m,ANCHOR,capacity));
        assertThrows(IllegalArgumentException.class,()->new ModernWorldMapQualification.Expected(new ModernWorldBinding.Key(1,509),m,new ModernWorldMap.Cell(0,0),100));
    }
    @Test void matchingHashAloneDoesNotQualifyAnInsufficientConnectedSpawnComponent() throws Exception {
        byte[] bytes=ModernWorldMapTest.openSectorBytes();
        for(int y=0;y<64;y++)for(int x=0;x<64;x++)ModernWorldMapTest.setAttributes(bytes,x,y,(short)1,(short)1);
        int localX=ANCHOR.x()-SECTOR.origin().x(),localY=ANCHOR.y()-SECTOR.origin().y();
        for(int y=localY-1;y<=localY+1;y++)for(int x=localX-1;x<=localX+1;x++)ModernWorldMapTest.setAttributes(bytes,x,y,(short)0,(short)0);
        var map=ModernWorldMap.fromSectorBytes(509,Map.of(SECTOR,bytes),()->false);
        assertTrue(map.standing(ANCHOR));
        assertEquals(ModernWorldMap.Failure.INVALID_SPAWNS,assertThrows(ModernWorldMap.MapException.class,
                ()->ModernWorldMapQualification.qualify(map,expected(map.manifest()))).failure());
    }
}
