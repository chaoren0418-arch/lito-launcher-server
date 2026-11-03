package local.lito381.server;

import static org.junit.jupiter.api.Assertions.*;
import static local.lito381.server.ModernSessionProtocol.*;
import static local.lito381.server.ModernSessionExtensions.*;
import java.io.ByteArrayInputStream;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.HexFormat;
import org.junit.jupiter.api.Test;

class ModernSessionExtensionsTest {
    static Hello base(){return new Hello(0xfedcba98,47,47,KNOWN_CAPABILITIES,DEPENDENCIES,1);}
    static ModernSessionBoundary.Policy policy(){return new ModernSessionBoundary.Policy(KNOWN_CAPABILITIES,DEPENDENCIES,47);}
    static final String HELLO_HEX="4c544d3100000028fedcba9800500000002f002fffffffff18012eff000100000000000300000003";
    static final String ACK_HEX="4c544d310000001cfedcba9800510000002f0001ffffffff00000003";
    @Test void armorDataHasAnIndependentBitAndNoImplicitHealingPermission()throws Exception {
        for(int ext:new int[]{4,5,7}) {
            var offer=new Offer(ext,ext);var b=new ModernSessionBoundary(policy(),offer);
            var a=(ExtendedAck)decode(b.acceptHello(encode(new ExtendedHello(base(),offer))));
            assertEquals(ext,a.extensions());var n=b.negotiated().orElseThrow();
            assertTrue(allowsArmor(n));assertEquals(ext!=4,allowsData(n));assertEquals(ext==7,allowsCommands(n));
        }
        assertEquals("4c544d3100000028fedcba9800500000002f002fffffffff18012eff000100000000000400000004",
                HexFormat.of().formatHex(encode(new ExtendedHello(base(),ARMOR))));
        for(var old:java.util.List.of(NONE,new Offer(1,1),HEALING)) {
            var b=new ModernSessionBoundary(policy(),old);
            assertThrows(ProtocolException.class,()->b.acceptHello(encode(new ExtendedHello(base(),ARMOR))));
            assertEquals(ModernSessionBoundary.State.CLOSED,b.state());
        }
        assertEquals(3,HEALING.supported());assertEquals(7,HEALING_ARMOR.required());
    }
    @Test void explicitKindsPreserveLegacyWordAndExactCrossLanguageGolden()throws Exception {
        var h=new ExtendedHello(base(),HEALING);var raw=encode(h);
        assertEquals(HELLO_HEX,HexFormat.of().formatHex(raw));assertEquals(h,decode(raw));
        var boundary=new ModernSessionBoundary(policy(),HEALING);var answer=boundary.acceptHello(raw);
        assertEquals(ACK_HEX,HexFormat.of().formatHex(answer));var a=(ExtendedAck)decode(answer);
        assertEquals(HEALING_MASK,a.extensions());assertEquals(-1,a.base().capabilities());
        var n=boundary.negotiated().orElseThrow();assertTrue(allowsCommands(n));assertTrue(allowsData(n));
        assertEquals(47,n.version());assertEquals(3,n.extensions());
        assertThrows(ProtocolException.class,()->boundary.acceptHello(raw));assertEquals(ModernSessionBoundary.State.CLOSED,boundary.state());assertTrue(boundary.negotiated().isEmpty());
        var legacy=new ModernSessionBoundary(policy());var old=encode(base());assertEquals(32,old.length);
        assertInstanceOf(Ack.class,decode(legacy.acceptHello(old)));assertEquals(0,legacy.negotiated().orElseThrow().extensions());
        assertFalse(allowsData(legacy.negotiated().orElseThrow()));
        for(int version=1;version<=47;version++){int v=version;assertThrows(ModernApplicationProtocol.ProtocolException.class,()->ModernApplicationProtocol.decode(raw,v));}
    }
    @Test void missingDependenciesUnknownBitsVersionSchemaAndUnmetRequiredExtensionsFailClosed()throws Exception {
        for(int bit=1;bit!=0;bit<<=1)if((bit&DEPENDENCIES)!=0){int without=DEPENDENCIES&~bit;
            assertThrows(ProtocolException.class,()->encode(new ExtendedHello(new Hello(1,47,47,KNOWN_CAPABILITIES,without,1),HEALING)));
            assertThrows(ProtocolException.class,()->encode(new ExtendedAck(new Ack(1,47,1,KNOWN_CAPABILITIES&~(DEPENDENCIES&~without)),3)));
        }
        for(int bits:new int[]{2,6,8,-1,Integer.MIN_VALUE})assertThrows(IllegalArgumentException.class,()->new Offer(bits,0));
        assertThrows(IllegalArgumentException.class,()->new Offer(1,3));
        for(var h:java.util.List.of(new Hello(1,46,47,KNOWN_CAPABILITIES,DEPENDENCIES,1),new Hello(1,47,48,KNOWN_CAPABILITIES,DEPENDENCIES,1),new Hello(1,47,47,KNOWN_CAPABILITIES,DEPENDENCIES,2)))
            assertThrows(ProtocolException.class,()->encode(new ExtendedHello(h,HEALING)));
        for(var offer:java.util.List.of(NONE,new Offer(1,1))) {
            var b=new ModernSessionBoundary(policy(),offer);
            assertThrows(ProtocolException.class,()->b.acceptHello(encode(new ExtendedHello(base(),HEALING))));
            assertEquals(ModernSessionBoundary.State.CLOSED,b.state());assertTrue(b.negotiated().isEmpty());
        }
        var mandatory=new ModernSessionBoundary(policy(),HEALING);
        assertThrows(ProtocolException.class,()->mandatory.acceptHello(encode(base())));
        var optional=new ModernSessionBoundary(policy(),new Offer(1,0));
        var reply=(ExtendedAck)decode(optional.acceptHello(encode(new ExtendedHello(base(),new Offer(3,0)))));
        assertEquals(1,reply.extensions());assertTrue(allowsData(optional.negotiated().orElseThrow()));assertFalse(allowsCommands(optional.negotiated().orElseThrow()));
        assertThrows(IllegalArgumentException.class,()->new ModernSessionBoundary(ModernSessionBoundary.Policy.current(),HEALING));
    }
    @Test void boundedReadersRejectTruncatedTrailingWrongKindAndUnknownExtensionsBeforeAdmission()throws Exception {
        for(var m:java.util.List.of(new ExtendedHello(base(),HEALING),new ExtendedAck(new Ack(1,47,1,KNOWN_CAPABILITIES),3))) {
            var bytes=encode(m);var stream=new ByteArrayInputStream(ByteBuffer.allocate(bytes.length*2).put(bytes).put(bytes).array());
            assertArrayEquals(bytes,readFrame(stream));assertEquals(bytes.length,stream.available());
            for(int size=0;size<bytes.length;size++){var prefix=Arrays.copyOf(bytes,size);assertThrows(ProtocolException.class,()->decode(prefix));}
            var extra=Arrays.copyOf(bytes,bytes.length+1);ByteBuffer.wrap(extra).putInt(4,extra.length);assertThrows(ProtocolException.class,()->decode(extra));
            for(int offset:new int[]{0,4,13,15,bytes.length-1}){var bad=bytes.clone();bad[offset]^=offset==bytes.length-1?0x80:0x7f;assertThrows(ProtocolException.class,()->decode(bad));}
            for(int size:new int[]{0,16,31,41,Integer.MAX_VALUE}){var bad=bytes.clone();ByteBuffer.wrap(bad).putInt(4,size);var input=new ByteArrayInputStream(bad);
                assertThrows(ProtocolException.class,()->readFrame(input));assertEquals(bytes.length-16,input.available());}
        }
    }
}
