package local.lito381.server;

import static org.junit.jupiter.api.Assertions.*;
import java.nio.ByteBuffer;
import java.nio.BufferUnderflowException;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.List;
import org.junit.jupiter.api.Test;

class ModernBuffProtocolTest {
    static final String GOLDEN="0001000000000000"
            +"000000000000000100000000000000020000000000000003"
            +"000000000000000a0000000000000014000000000000001e0300000000000000";
    static ModernTimedEffects.State sample() { return new ModernTimedEffects.State(1,2,3,10,20,30,3); }
    @Test void independentFixedWidthLiteralRetainsAllThreeLeasesAndTheSampledClock() throws Exception {
        var out=ByteBuffer.allocate(64);ModernBuffProtocol.putStates(out,List.of(sample()));
        assertEquals(64,out.position());assertEquals(GOLDEN,HexFormat.of().formatHex(out.array()));
        var in=ByteBuffer.wrap(HexFormat.of().parseHex(GOLDEN));
        assertEquals(List.of(sample()),ModernBuffProtocol.getStates(in,1));assertFalse(in.hasRemaining());
    }
    @Test void disabledAppendixIsEightZeroBytesAndNotImplicitFullRoster() throws Exception {
        var out=ByteBuffer.allocate(8);ModernBuffProtocol.putStates(out,List.of());assertArrayEquals(new byte[8],out.array());
        assertEquals(List.of(),ModernBuffProtocol.getStates(ByteBuffer.wrap(out.array()),100));
    }
    @Test void canonicalBotsAndHumansHaveExplicitOrderedEntityIds() throws Exception {
        var states=List.of(ModernTimedEffects.initial(1),ModernTimedEffects.initial(2));
        var out=ByteBuffer.allocate(120);ModernBuffProtocol.putStates(out,states);
        assertEquals(states,ModernBuffProtocol.getStates(ByteBuffer.wrap(out.array()),2));
        assertThrows(IllegalArgumentException.class,()->ModernBuffProtocol.putStates(ByteBuffer.allocate(120),List.of(states.get(1),states.get(0))));
        byte[] duplicated=out.array().clone();ByteBuffer.wrap(duplicated).putLong(64,1);
        assertThrows(ModernApplicationProtocol.ProtocolException.class,()->ModernBuffProtocol.getStates(ByteBuffer.wrap(duplicated),2));
    }
    @Test void everyReservedByteAndUnknownBraveTypeIsRejected() {
        for(int offset:new int[]{2,3,4,5,6,7,57,58,59,60,61,62,63}) {
            var bytes=HexFormat.of().parseHex(GOLDEN);bytes[offset]=1;
            assertThrows(ModernApplicationProtocol.ProtocolException.class,()->ModernBuffProtocol.getStates(ByteBuffer.wrap(bytes),1));
        }
        var invalid=HexFormat.of().parseHex(GOLDEN);invalid[56]=2;
        assertThrows(IllegalArgumentException.class,()->ModernBuffProtocol.getStates(ByteBuffer.wrap(invalid),1));
    }
    @Test void everyTruncationAndInvalidCountIsRejectedBeforeAnIncompleteStateEscapes() {
        var complete=HexFormat.of().parseHex(GOLDEN);
        for(int size=0;size<complete.length;size++) {
            var cut=Arrays.copyOf(complete,size);
            var failure=assertThrows(Exception.class,()->ModernBuffProtocol.getStates(ByteBuffer.wrap(cut),1));
            assertTrue(failure instanceof BufferUnderflowException || failure instanceof ModernApplicationProtocol.ProtocolException);
        }
        for(int actors:new int[]{0,2,101})assertThrows(ModernApplicationProtocol.ProtocolException.class,
                ()->ModernBuffProtocol.getStates(ByteBuffer.wrap(complete),actors));
        var bytes=complete.clone();ByteBuffer.wrap(bytes).putShort(0,(short)101);
        assertThrows(ModernApplicationProtocol.ProtocolException.class,()->ModernBuffProtocol.getStates(ByteBuffer.wrap(bytes),1));
    }
    @Test void codecCannotRepresentExpiredFuturelessOrNoncanonicalInitialEffects() {
        for(int offset:new int[]{32,40,48}) {
            var bytes=HexFormat.of().parseHex(GOLDEN);ByteBuffer.wrap(bytes).putLong(offset,3);
            assertThrows(IllegalArgumentException.class,()->ModernBuffProtocol.getStates(ByteBuffer.wrap(bytes),1));
        }
        var bytes=HexFormat.of().parseHex(GOLDEN);ByteBuffer.wrap(bytes).putLong(16,0);
        assertThrows(IllegalArgumentException.class,()->ModernBuffProtocol.getStates(ByteBuffer.wrap(bytes),1));
    }
}
