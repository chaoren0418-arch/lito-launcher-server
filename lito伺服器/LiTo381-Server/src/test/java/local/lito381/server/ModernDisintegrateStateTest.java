package local.lito381.server;

import static org.junit.jupiter.api.Assertions.*;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

class ModernDisintegrateStateTest {
    static ModernDisintegrateState.Hit hit(boolean critical) {
        return new ModernDisintegrateState.Hit(2,1,1,10,10,11,10,1000,800,200,200,200,critical);
    }
    static ModernDisintegrateState.State cast(boolean critical) {
        return ModernDisintegrateState.State.initial(1,70,500).cast(9,3,hit(critical));
    }
    @Test void independentlyWrittenBigEndianVectorProvesOffsetsWidthsCriticalAndReservedBytes() {
        for(boolean critical:new boolean[]{false,true}) {
            var bytes=ModernDisintegrateState.encode(List.of(cast(critical)));
            assertEquals(160,bytes.length);
            var expected=ByteBuffer.allocate(160);
            expected.putShort((short)1).put(new byte[6]);
            for(long value:new long[]{1,1,9,70,3,500,501})expected.putLong(value);
            expected.putInt(70).putInt(5000);
            for(long value:new long[]{2,1,1})expected.putLong(value);
            for(int value:new int[]{10,10,11,10})expected.putInt(value);
            for(long value:new long[]{1000,800,200,200,200})expected.putLong(value);
            expected.put((byte)(critical?1:0)).put(new byte[7]);
            assertArrayEquals(expected.array(),bytes);
            assertEquals(List.of(cast(critical)),ModernDisintegrateState.decode(expected.array()));
            assertEquals(critical?1:0,Byte.toUnsignedInt(bytes[152]));
        }
    }
    @Test void emptyInitialFullRosterAndI64ExtremesHaveBoundedExactLengths() {
        assertEquals(List.of(),ModernDisintegrateState.decode(new byte[8]));
        var roster=new ArrayList<ModernDisintegrateState.State>();
        for(int i=1;i<=100;i++)roster.add(ModernDisintegrateState.State.initial(i,i%2==0?0:59,i%2==0?0:Long.MIN_VALUE));
        assertEquals(15208,ModernDisintegrateState.encode(roster).length);
        assertEquals(roster,ModernDisintegrateState.decode(ModernDisintegrateState.encode(roster)));
        roster.add(ModernDisintegrateState.State.initial(101,0,0));
        assertThrows(IllegalArgumentException.class,()->ModernDisintegrateState.encode(roster));
        long n=1L<<34;
        var h=new ModernDisintegrateState.Hit(2,Long.MAX_VALUE,Long.MAX_VALUE,Integer.MIN_VALUE,Integer.MAX_VALUE,Integer.MIN_VALUE+1,Integer.MAX_VALUE,
                Long.MAX_VALUE,Long.MAX_VALUE-n,n,n,n,true);
        var s=ModernDisintegrateState.State.initial(1,59,n).cast(n,n,h);
        assertEquals(List.of(s),ModernDisintegrateState.decode(ModernDisintegrateState.encode(List.of(s))));
        assertEquals(n+1,s.wireRevision().revision());assertEquals(s.hit(),s.wireRevision().hit());assertEquals(n+1,s.wireRevision().lawful());
    }
    @Test void truncatedTrailingOversizedReservedAndNonBooleanPayloadsAreRejected() {
        var original=ModernDisintegrateState.encode(List.of(cast(false)));
        for(int n=0;n<original.length;n++) {
            var broken=Arrays.copyOf(original,n);assertThrows(RuntimeException.class,()->ModernDisintegrateState.decode(broken));
        }
        assertThrows(IllegalArgumentException.class,()->ModernDisintegrateState.decode(Arrays.copyOf(original,original.length+1)));
        assertThrows(IllegalArgumentException.class,()->ModernDisintegrateState.decode(new byte[15209]));
        assertThrows(IllegalArgumentException.class,()->ModernDisintegrateState.decode(null));
        for(int offset:new int[]{2,3,4,5,6,7,152,153,154,155,156,157,158,159}) {
            var broken=original.clone();broken[offset]=2;assertThrows(IllegalArgumentException.class,()->ModernDisintegrateState.decode(broken));
        }
        var initial=ModernDisintegrateState.encode(List.of(ModernDisintegrateState.State.initial(1,70,500)));
        for(int offset=72;offset<160;offset++) {
            var broken=initial.clone();broken[offset]=1;assertThrows(IllegalArgumentException.class,()->ModernDisintegrateState.decode(broken));
        }
    }
    @Test void totalsInitialLawfulCostLifeTargetDistanceAndDamageCannotBeForged() {
        var original=ModernDisintegrateState.encode(List.of(cast(false)));
        // Direct byte mutations, not merely constructor roundtrips.
        int[] offsets={8,16,24,32,40,48,56,64,68,72,80,88,96,104,112,120,128,136,144};
        long[] values={2,2,0,71,0,499,502,58,4999,1,0,0,Integer.MIN_VALUE,Integer.MAX_VALUE,0,999,199,199,199};
        for(int i=0;i<offsets.length;i++) {
            var broken=original.clone();var b=ByteBuffer.wrap(broken);
            if(offsets[i]==64||offsets[i]==68||offsets[i]==96||offsets[i]==104)b.putInt(offsets[i],(int)values[i]);
            else b.putLong(offsets[i],values[i]);
            assertThrows(RuntimeException.class,()->ModernDisintegrateState.decode(broken),"offset "+offsets[i]);
        }
        assertThrows(IllegalArgumentException.class,()->ModernDisintegrateState.State.initial(1,0,500));
        assertThrows(IllegalArgumentException.class,()->ModernDisintegrateState.State.initial(1,70,499).cast(9,1,hit(false)));
        assertThrows(RuntimeException.class,()->ModernDisintegrateState.State.initial(1,70,Long.MAX_VALUE).cast(9,1,hit(false)));
        assertThrows(IllegalArgumentException.class,()->cast(false).cast(9,4,hit(false)));
        assertThrows(IllegalArgumentException.class,()->cast(false).cast(10,3,hit(false)));
    }
    @Test void checkedMpAndLawfulCountsCannotWrapToSmallValues() {
        long count=Long.MAX_VALUE/70+1;
        assertThrows(ArithmeticException.class,()->new ModernDisintegrateState.State(1,count,1,0,count,500,500+count,70,5000,hit(false)));
        assertThrows(ArithmeticException.class,()->new ModernDisintegrateState.State(1,1,1,70,1,Long.MAX_VALUE,Long.MIN_VALUE,70,5000,hit(false)));
    }
}
