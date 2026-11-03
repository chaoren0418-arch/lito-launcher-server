package local.lito381.server;

import static local.lito381.server.ModernSessionProtocol.*;
import static local.lito381.server.ModernSessionExtensions.*;
import static org.junit.jupiter.api.Assertions.*;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import org.junit.jupiter.api.Test;

/** Real complete engine captures -> ordinary selected READ. Pipe test only;
 * no production WorldService, authentication, SQL source or rendering grant. */
public final class ModernVipSelectedProtocolTest {
    record Handshake(byte[] hello,byte[] ack,ModernSessionBoundary.Negotiated peer){}
    static Handshake handshake(int ext)throws Exception {
        var offer=new Offer(ext,ext);
        var hello=encode(new ExtendedHello(new Hello(0xfedcba98,47,47,KNOWN_CAPABILITIES,DEPENDENCIES,1),offer));
        var boundary=new ModernSessionBoundary(new ModernSessionBoundary.Policy(KNOWN_CAPABILITIES,DEPENDENCIES,47),offer);
        var ack=boundary.acceptHello(hello);
        return new Handshake(hello,ack,boundary.negotiated().orElseThrow());
    }
    static ModernApplicationProtocol.BattleReadResult result(int id,ModernBattleVipWire.Payload p) {
        return new ModernApplicationProtocol.BattleReadResult(id,
                new ModernBattleReadProtocol.Request(p.view().personal().scope(),p.view().world().identity()),
                ModernBattleReadProtocol.Reply.vip(p));
    }
    @Test void fullVipReadIsAnExplicitIndependentSelectionNotACommandGrant()throws Exception {
        assertEquals(3,HEALING.required());assertEquals(4,ARMOR.required());assertEquals(7,HEALING_ARMOR.required());
        assertEquals(12,ARMOR_PRESENTATION.required());assertEquals(15,HEALING_ARMOR_PRESENTATION.required());
        for(int bits:new int[]{16,17,28,29,31}) {
            var h=handshake(bits);var p=h.peer();assertEquals(bits,p.extensions());
            assertTrue(allowsVipData(p));assertEquals(bits==31,allowsCommands(p));
            assertEquals(ModernBattleVipWire.MAX_BYTES,ModernBattleWire.Encoding.selected(p).maximum());
        }
        for(var old:List.of(NONE,HEALING,ARMOR,HEALING_ARMOR,ARMOR_PRESENTATION,HEALING_ARMOR_PRESENTATION)) {
            var boundary=new ModernSessionBoundary(new ModernSessionBoundary.Policy(KNOWN_CAPABILITIES,DEPENDENCIES,47),old);
            assertThrows(ProtocolException.class,()->boundary.acceptHello(handshake(16).hello()));
            assertEquals(ModernSessionBoundary.State.CLOSED,boundary.state());
        }
        assertThrows(IllegalArgumentException.class,()->new Offer(32,0));
        for(int bit=1;bit!=0;bit<<=1)if((bit&DEPENDENCIES)!=0) {
            int base=KNOWN_CAPABILITIES&~bit;
            assertThrows(ProtocolException.class,()->encode(new ExtendedAck(new Ack(1,47,1,base),16)));
        }
    }
    @Test void ordinaryReadRetainsAllSectionsAndCannotDowngradeOrChangeOwner()throws Exception {
        var peer=handshake(29).peer();int id=1;
        var frames=ModernBattleVipWirePeer.frames();assertEquals(64,frames.size());
        for(var frame:frames) {
            var p=frame.payload();var r=result(id++,p);
            var bytes=ModernApplicationProtocol.encode(ModernMatchDescriptor.forPeer(r,peer),peer);
            assertEquals(61,ByteBuffer.wrap(bytes).getShort(12));
            assertArrayEquals(ModernBattleVipWire.encode(p),Arrays.copyOfRange(bytes,72,bytes.length));
            assertEquals(10,ByteBuffer.wrap(bytes).getShort(76));
            assertEquals(18,ByteBuffer.wrap(bytes).getShort(84));
            for(int old:new int[]{0,1,3,4,5,7,12,13,15}) {
                var denied=handshake(old).peer();
                assertFalse(p.descriptor().supportedBy(denied));
                assertThrows(Exception.class,()->ModernApplicationProtocol.encode(r,denied));
                assertThrows(IllegalArgumentException.class,()->ModernMatchDescriptor.forPeer(r,denied));
            }
            for(int version=1;version<=47;version++) {
                int v=version;assertFalse(p.descriptor().supportedBy(v,KNOWN_CAPABILITIES));
                assertThrows(Exception.class,()->ModernBattleReadProtocol.encodeResult(r,v));
            }
            for(int ext:new int[]{16,17,28,29,31}) {
                var n=handshake(ext).peer();boolean admitted=(!p.descriptor().has(ModernMatchDescriptor.PLAYER_HEALING)||allowsData(n))
                        &&(!p.descriptor().has(ModernMatchDescriptor.ARMOR_PROTECTION)||allowsArmorPresentation(n));
                assertEquals(admitted,p.descriptor().supportedBy(n));
                if(admitted)assertArrayEquals(bytes,ModernApplicationProtocol.encode(r,n));
                else assertThrows(Exception.class,()->ModernApplicationProtocol.encode(r,n));
            }
            var s=r.echo().scope();
            var wrong=new ModernBattleReadProtocol.Request(new ModernRooms.Scope(s.sourceId()==7?42:7,s.objectId(),s.epoch(),s.channelId()),r.echo().world());
            var swapped=new ModernApplicationProtocol.BattleReadResult(r.requestId(),wrong,r.reply());
            assertThrows(Exception.class,()->ModernBattleReadProtocol.encodeResult(swapped,peer));
            assertThrows(IllegalArgumentException.class,()->new ModernBattleReadProtocol.Reply(ModernBattleReadProtocol.State.PREPARING,null,p));
            assertThrows(IllegalArgumentException.class,()->new ModernBattleReadProtocol.Reply(ModernBattleReadProtocol.State.DATA,null,null));
        }
    }
    public static void main(String[] args)throws Exception {
        if(args.length>1||args.length==1&&!args[0].equals("shared-armor-gfx0"))throw new IllegalArgumentException("explicit fixture mode");
        var input=new BufferedReader(new InputStreamReader(System.in,StandardCharsets.US_ASCII));
        for(int ext:new int[]{16,17,28,29,31}) {
            var h=handshake(ext);System.out.println("SESSION:"+ext+":"+HexFormat.of().formatHex(h.hello())+":"+HexFormat.of().formatHex(h.ack()));
        }
        var peer=handshake(29).peer();int count=0;
        for(var f:ModernBattleVipWirePeer.frames(args.length==1)) {
            var r=result(++count,f.payload());
            var request=ModernApplicationProtocol.encode(new ModernApplicationProtocol.BattleReadRequest(r.requestId(),r.echo()),47);
            var bytes=ModernApplicationProtocol.encode(ModernMatchDescriptor.forPeer(r,peer),peer);
            System.out.println("REPLY:"+f.label()+":"+HexFormat.of().formatHex(request)+":"+HexFormat.of().formatHex(bytes));System.out.flush();
            var digest=new StringBuilder();int ch;
            while((ch=input.read())!=-1&&ch!='\n'){if(ch=='\r')continue;if(digest.length()>=64)throw new IllegalArgumentException("oversized digest");digest.append((char)ch);}
            assertEquals(HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes)),digest.toString());
        }
        System.out.println("CLOSED:"+count);System.out.flush();
    }
}
