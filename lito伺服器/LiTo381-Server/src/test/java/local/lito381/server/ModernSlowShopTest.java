package local.lito381.server;

import static org.junit.jupiter.api.Assertions.*;
import static local.lito381.server.ModernSpellMaterialsTest.*;
import java.nio.ByteBuffer;
import java.util.*;
import org.junit.jupiter.api.Test;

class ModernSlowShopTest {
    @Test void v47RoundTripsMaterialsAndOldPeersCannotInterpretTheAppendix()throws Exception {
        var f=new ModernRoomsTest.Fixture();var p=f.player(7,3333,80,0);
        var rooms=f.rooms.execute(p.owner(),new ModernRooms.Command(p.scope(),ModernRooms.Action.READ,0,0,0,null,""));
        var c=catalog(7,2000);var b=basis(7,0,1000);
        try(var a=shop(true)) {
            var first=read(a,c,b);
            var q=new ModernShopProtocol.Request(p.scope(),ModernShopAuthority.Action.BUY,0,0,first.roundId(),first.revision(),first.token(),40318,3);
            var bought=buy(a,first,40318,3,c,b);
            var message=new ModernApplicationProtocol.ShopResult(1,q,new ModernShopProtocol.Reply(bought,rooms));
            var wire=ModernApplicationProtocol.encode(message,47);
            assertEquals(message,ModernApplicationProtocol.decode(wire,47));
            assertEquals(1,wire[wire.length-8]);assertEquals(6,bought.snapshot().weight().view().suppliesWeight());
            assertSame(message,ModernMatchDescriptor.forPeer(message,new ModernSessionBoundary.Negotiated(1,47,1,ModernSlowServiceTest.CAPS)));
            for(int version=8;version<47;version++) {
                final int v=version;
                assertThrows(Exception.class,()->ModernApplicationProtocol.encode(message,v));
                assertThrows(ModernApplicationProtocol.ProtocolException.class,()->ModernApplicationProtocol.decode(wire,v));
            }
            for(var peer:List.of(new ModernSessionBoundary.Negotiated(1,46,1,ModernSlowServiceTest.CAPS),
                    new ModernSessionBoundary.Negotiated(1,47,1,ModernSlowServiceTest.CAPS&~ModernSessionProtocol.CAP_SLOW_DATA)))
                assertThrows(IllegalArgumentException.class,()->ModernMatchDescriptor.forPeer(message,peer));
            for(int index=wire.length-8;index<wire.length;index++) {
                byte[] bad=wire.clone();bad[index]=2;
                assertThrows(ModernApplicationProtocol.ProtocolException.class,()->ModernApplicationProtocol.decode(bad,47));
            }
            byte[] disabled=wire.clone();disabled[wire.length-8]=0;
            assertThrows(ModernApplicationProtocol.ProtocolException.class,()->ModernApplicationProtocol.decode(disabled,47));
            for(int n=1;n<=8;n++) {
                byte[] shortWire=Arrays.copyOf(wire,wire.length-n);ByteBuffer.wrap(shortWire).putInt(4,shortWire.length);
                assertThrows(ModernApplicationProtocol.ProtocolException.class,()->ModernApplicationProtocol.decode(shortWire,47));
            }
        }
    }
    @Test void materialCapableButEmptyShopCanEnterOldRoomsAndOldShopsRemainDisabled()throws Exception {
        var c=catalog(7,2000);var b=basis(7,0,1000);
        int common=ModernMatchDescriptor.COMBAT|ModernMatchDescriptor.SPELLS|ModernMatchDescriptor.ITEMS|ModernMatchDescriptor.WEIGHT|ModernMatchDescriptor.TIMED;
        var old=new ModernMatchDescriptor(ModernMatchDescriptor.Ruleset.PVP_SKIRMISH,common,1000,1000,0,10000);
        try(var a=shop(true)) {
            read(a,c,b);assertTrue(a.capture(2,3,ModernShopAuthorityTest.deadline(),c,b,old).isEmpty());
            assertTrue(a.frozen());a.terminal(2,3);assertFalse(a.frozen());
            var bought=buy(a,read(a,c,b),40318,1,c,b).snapshot();
            assertThrows(IllegalArgumentException.class,()->a.capture(3,4,ModernShopAuthorityTest.deadline(),c,b,old));
            assertFalse(a.frozen());assertEquals(bought,read(a,c,b));
        }
        var f=new ModernRoomsTest.Fixture();var p=f.player(7,3333,80,0);
        var rooms=f.rooms.execute(p.owner(),new ModernRooms.Command(p.scope(),ModernRooms.Action.READ,0,0,0,null,""));
        var q=new ModernShopProtocol.Request(p.scope(),ModernShopAuthority.Action.READ,0,0,0,0,new ModernEquipmentOverrideStore.Fingerprint(new byte[32]),0,0);
        try(var legacy=shop(false)) {
            var msg=new ModernApplicationProtocol.ShopResult(1,q,new ModernShopProtocol.Reply(legacy.apply(ModernShopAuthority.Action.READ,0,0,null,0,0,c,b),rooms));
            var wire=ModernApplicationProtocol.encode(msg,47);assertEquals(0,wire[wire.length-8]);
            assertEquals(msg,ModernApplicationProtocol.decode(wire,47));
        }
    }
}
