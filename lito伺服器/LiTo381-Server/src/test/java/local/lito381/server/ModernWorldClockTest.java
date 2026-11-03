package local.lito381.server;

import static org.junit.jupiter.api.Assertions.*;
import java.nio.ByteBuffer;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ModernWorldClockTest {
    static ModernWorldService.Mapping mapping()throws Exception {
        var map=ModernWorldMap.fromSectorBytes(514,Map.of(new ModernWorldMap.SectorKey(0x8000,0x8000),ModernWorldMapTest.openSectorBytes()),()->false);
        return new ModernWorldService.Mapping(new ModernWorldBinding(1,514,map),new ModernWorldMap.Cell(32778,32778),1_000_000);
    }
    static ModernWorldService.Limits globals(){return new ModernWorldService.Limits(4,1,1,2000,5,new ModernBattleEngine.Limits(2000,3_000_000_001L,45000));}
    @Test void oldConstructorsAndEachOptionalFieldPreserveExactIndependentFallback()throws Exception {
        var mapping=mapping();assertNull(mapping.clockOverrides());var global=globals();
        var original=ModernMatchDescriptor.of(mapping,global,false);assertEquals(global.engineLimits(),original.engineLimits());
        for(int mask=0;mask<8;mask++) {
            var local=new ModernWorldService.ClockOverrides((mask&1)==0?null:50,(mask&2)==0?null:30_000_000_000L,(mask&4)==0?null:1800000);
            var descriptor=ModernMatchDescriptor.of(mapping.withClockOverrides(local),global,false);
            assertEquals((mask&1)==0?2000:50,descriptor.loadTimeoutMillis());
            assertEquals((mask&2)==0?3_000_000_001L:30_000_000_000L,descriptor.countdownNanos());
            assertEquals((mask&4)==0?45000:1800000,descriptor.activeTimeoutMillis());
            assertEquals(2000,descriptor.preparationTimeoutMillis());assertFalse(descriptor.settlementEnabled());
            assertEquals(ModernMatchDescriptor.Ruleset.SCENE_ONLY,descriptor.ruleset());
            assertEquals(global,globals());assertNull(mapping.clockOverrides());
        }
    }
    @Test void overridesKeepTheEngineBoundsAndRejectNumericOverflowBeforeAnyPreparation(){
        for(int value:new int[]{Integer.MIN_VALUE,-1,0,49,30001,Integer.MAX_VALUE})
            assertThrows(IllegalArgumentException.class,()->new ModernWorldService.ClockOverrides(value,null,null));
        for(long value:new long[]{Long.MIN_VALUE,-1,30_000_000_001L,Long.MAX_VALUE})
            assertThrows(IllegalArgumentException.class,()->new ModernWorldService.ClockOverrides(null,value,null));
        for(int value:new int[]{Integer.MIN_VALUE,-1,0,49,1800001,Integer.MAX_VALUE})
            assertThrows(IllegalArgumentException.class,()->new ModernWorldService.ClockOverrides(null,null,value));
        assertEquals(new ModernBattleEngine.Limits(50,0,50),new ModernWorldService.ClockOverrides(50,0L,50).resolve(globals().engineLimits()));
        assertEquals(new ModernBattleEngine.Limits(30000,30_000_000_000L,1800000),new ModernWorldService.ClockOverrides(30000,30_000_000_000L,1800000).resolve(globals().engineLimits()));
        assertThrows(NullPointerException.class,()->new ModernWorldService.ClockOverrides(null,null,null).resolve(null));
    }
    @Test void frozenDescriptorDoesNotReresolveAndAllLegacyBytesStayUnchanged()throws Exception {
        var mapping=mapping().withClockOverrides(new ModernWorldService.ClockOverrides(null,1L,50));
        var descriptor=ModernMatchDescriptor.of(mapping,globals(),false);
        var later=ModernMatchDescriptor.of(mapping,new ModernWorldService.Limits(4,1,1,3000,5,new ModernBattleEngine.Limits(30000,0,1800000)),false);
        assertEquals(2000,descriptor.loadTimeoutMillis());assertEquals(30000,later.loadTimeoutMillis());assertEquals(2000,descriptor.preparationTimeoutMillis());
        var world=ModernWorldProtocolTest.frame();
        var old=new ModernApplicationProtocol.WorldResult(7,ModernWorldProtocolTest.read(),new ModernWorldProtocol.Reply(ModernWorldProtocol.Status.OK,ModernWorldProtocol.Reason.NONE,world));
        var fresh=new ModernApplicationProtocol.WorldResult(7,old.echo(),new ModernWorldProtocol.Reply(old.reply().status(),old.reply().reason(),world.withDescriptor(descriptor)));
        for(int version=1;version<=14;version++)assertArrayEquals(ModernApplicationProtocol.encode(old,version),ModernApplicationProtocol.encode(fresh,version));
        assertEquals(fresh,ModernApplicationProtocol.decode(ModernApplicationProtocol.encode(fresh,15),15));
        var bytes=ByteBuffer.allocate(40);ModernMatchDescriptor.put(bytes,descriptor);
        assertEquals(0,bytes.array()[11],"timeout policy remains SafetyAbortNoResult");assertEquals(0,bytes.array()[36],"server monotonic clock");
    }
}
