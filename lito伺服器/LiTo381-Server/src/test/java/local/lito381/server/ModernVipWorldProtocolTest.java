package local.lito381.server;

import static org.junit.jupiter.api.Assertions.*;
import java.io.ByteArrayInputStream;
import java.nio.ByteBuffer;
import java.util.Arrays;
import org.junit.jupiter.api.Test;

class ModernVipWorldProtocolTest {
    static ModernSessionBoundary.Negotiated peer(int bits){return new ModernSessionBoundary.Negotiated(1,47,1,-1,bits);}
    static ModernApplicationProtocol.VipWorldRequest request(){return new ModernApplicationProtocol.VipWorldRequest(1,
            new ModernBattleWorldProtocol.Request(new ModernRooms.Scope(7,3333,1,0),ModernBattleWorldProtocol.Action.READ,0,0,0,0,ModernWorldProtocol.ZERO_DIGEST));}
    @Test void independentWorldSelectionAndNoVersionOnlyFallback()throws Exception {
        for(int bits:new int[]{16,48,80,112}) {
            var p=peer(bits);assertTrue(ModernSessionExtensions.allowsVipData(p));
            assertEquals((bits&32)!=0,ModernSessionExtensions.allowsVipEquipment(p));
            assertEquals((bits&64)!=0,ModernSessionExtensions.allowsVipWorld(p));
            if((bits&64)!=0){var bytes=ModernApplicationProtocol.encode(request(),p);assertEquals(request(),ModernApplicationProtocol.decode(bytes,p));}
            else assertThrows(ModernApplicationProtocol.ProtocolException.class,()->ModernApplicationProtocol.encode(request(),p));
        }
        for(int bits:new int[]{32,64,96,128})assertThrows(IllegalArgumentException.class,()->new ModernSessionExtensions.Offer(bits,0));
        for(int version=1;version<=47;version++){int v=version;assertThrows(ModernApplicationProtocol.ProtocolException.class,()->ModernApplicationProtocol.encode(request(),v));}
    }
    @Test void exactRequestRoundTripTruncationReservedAndLengthFailClosed()throws Exception {
        var p=peer(112);var bytes=ModernApplicationProtocol.encode(request(),p);assertEquals(112,bytes.length);assertEquals(84,ByteBuffer.wrap(bytes).getShort(12));
        assertArrayEquals(bytes,ModernVipWorldProtocol.encodeRequest(ModernVipWorldProtocol.decodeRequest(bytes,p),p));
        for(int n=0;n<bytes.length;n++){var prefix=Arrays.copyOf(bytes,n);assertThrows(ModernApplicationProtocol.ProtocolException.class,()->ModernApplicationProtocol.decode(prefix,p));}
        for(int at:new int[]{0,4,12,14,36,40,41,47,48,64,80}){var bad=bytes.clone();bad[at]^=0xff;assertThrows(ModernApplicationProtocol.ProtocolException.class,()->ModernApplicationProtocol.decode(bad,p),"offset "+at);}
        for(int n:new int[]{0,16,111,113,Integer.MAX_VALUE}){var bad=bytes.clone();ByteBuffer.wrap(bad).putInt(4,n);assertThrows(ModernApplicationProtocol.ProtocolException.class,()->ModernApplicationProtocol.decode(bad,p));}
    }
    @Test void headerAdmissionRejectsUnselectedOpcodeBeforeReadingBody()throws Exception {
        var raw=ModernApplicationProtocol.encode(request(),peer(112));
        for(int bits:new int[]{0,16,48,63}){
            var stream=new ByteArrayInputStream(raw);assertThrows(ModernApplicationProtocol.ProtocolException.class,()->ModernApplicationProtocol.readFrame(stream,peer(bits)));
            assertEquals(96,stream.available());
        }
        var input=new ByteArrayInputStream(ByteBuffer.allocate(224).put(raw).put(raw).array());
        assertArrayEquals(raw,ModernApplicationProtocol.readFrame(input,peer(112)));assertEquals(112,input.available());
    }
}
