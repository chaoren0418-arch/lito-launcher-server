package local.lito381.server;

import static org.junit.jupiter.api.Assertions.*;
import static local.lito381.server.ModernMatchDescriptor.*;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.HexFormat;
import org.junit.jupiter.api.Test;

class ModernMatchDescriptorTest {
    static ModernMatchDescriptor scene(){return new ModernMatchDescriptor(Ruleset.SCENE_ONLY,0,3000,30000,3_000_000_000L,160000);}
    @Test void independentThirtyTwoBytePolicyGoldenKeepsExactNanoClock(){
        var bytes=ByteBuffer.allocate(40);put(bytes,scene());
        assertEquals("0100000000000000000100000000000000000bb80000753000000000b2d05e000002710000000000",HexFormat.of().formatHex(bytes.array()));
        assertEquals(scene(),get(ByteBuffer.wrap(bytes.array())));
        var nanos=new ModernMatchDescriptor(Ruleset.SCENE_ONLY,0,50,50,1,50);
        var b=ByteBuffer.allocate(40);put(b,nanos);assertEquals(1,get(ByteBuffer.wrap(b.array())).countdownNanos());
    }
    @Test void everyTruncationAndReservedOrUnknownPolicyRejects(){
        var b=ByteBuffer.allocate(40);put(b,scene());byte[] good=b.array();
        for(int n=0;n<good.length;n++){var cut=Arrays.copyOf(good,n);assertThrows(RuntimeException.class,()->get(ByteBuffer.wrap(cut)));}
        for(int offset:new int[]{0,1,2,3,4,5,6,7,8,9,10,11,12,36,37,38,39}){
            byte[] bad=good.clone();bad[offset]=(byte)255;assertThrows(RuntimeException.class,()->get(ByteBuffer.wrap(bad)),"offset="+offset);
        }
        var empty=ByteBuffer.allocate(8);put(empty,null);assertNull(get(ByteBuffer.wrap(empty.array())));
    }
    @Test void dependencyClosureModeAndSafetyOnlyRulesAreStrict(){
        for(int flags:new int[]{SPELLS,LIGHT_ARROW,ITEMS,WEIGHT,TIMED,AMMO,REGEN,HONOR,FIRE_STORM,1024,-1})
            assertThrows(IllegalArgumentException.class,()->new ModernMatchDescriptor(Ruleset.PVP_SKIRMISH,flags,3000,30000,0,300000));
        var honor=new ModernMatchDescriptor(Ruleset.HONOR_TOWERS,511,3000,30000,0,300000);
        assertDoesNotThrow(()->honor.validateMode(5));assertThrows(IllegalArgumentException.class,()->honor.validateMode(1));
        var sandbox=new ModernMatchDescriptor(Ruleset.PVP_SKIRMISH,COMBAT,3000,30000,0,300000);
        for(int mode:new int[]{1,3,5})assertDoesNotThrow(()->sandbox.validateMode(mode));
        for(int mode:new int[]{0,2,4})assertThrows(IllegalArgumentException.class,()->sandbox.validateMode(mode));
        for(int mode=0;mode<6;mode++)scene().validateMode(mode);
        assertFalse(scene().settlementEnabled());assertFalse(sandbox.settlementEnabled());assertTrue(honor.settlementEnabled());
        assertThrows(IllegalArgumentException.class,()->new ModernMatchDescriptor(Ruleset.SCENE_ONLY,0,49,50,0,50));
        assertThrows(IllegalArgumentException.class,()->new ModernMatchDescriptor(Ruleset.SCENE_ONLY,0,50,50,30_000_000_001L,50));
        assertThrows(IllegalArgumentException.class,()->new ModernMatchDescriptor(Ruleset.SCENE_ONLY,0,50,50,0,1800001));
    }
    @Test void worldAppendixRoundTripsAndLegacyOneThroughFourteenRemainByteExact()throws Exception{
        var legacy=ModernWorldProtocolTest.frame();var fresh=legacy.withDescriptor(scene());
        var before=new ModernApplicationProtocol.WorldResult(7,ModernWorldProtocolTest.read(),new ModernWorldProtocol.Reply(ModernWorldProtocol.Status.OK,ModernWorldProtocol.Reason.NONE,legacy));
        var after=new ModernApplicationProtocol.WorldResult(7,ModernWorldProtocolTest.read(),new ModernWorldProtocol.Reply(ModernWorldProtocol.Status.OK,ModernWorldProtocol.Reason.NONE,fresh));
        for(int v=1;v<=14;v++)assertArrayEquals(ModernApplicationProtocol.encode(before,v),ModernApplicationProtocol.encode(after,v),"version="+v);
        byte[] bytes=ModernApplicationProtocol.encode(after,15);assertEquals(after,ModernApplicationProtocol.decode(bytes,15));
        byte[] absent=ModernApplicationProtocol.encode(before,15);assertEquals(32,bytes.length-absent.length);
        assertEquals(8,absent.length-ModernApplicationProtocol.encode(before,14).length);
        for(int i=bytes.length-40;i<bytes.length;i++){var cut=Arrays.copyOf(bytes,i);assertThrows(ModernApplicationProtocol.ProtocolException.class,()->ModernApplicationProtocol.decode(cut,15));}
    }
    @Test void negotiatedPresenceCannotBeInventedOrStrippedAndSourceCapsDoNotGrantCombat(){
        var world=ModernWorldProtocolTest.frame();
        var old=new ModernApplicationProtocol.WorldResult(7,ModernWorldProtocolTest.read(),new ModernWorldProtocol.Reply(ModernWorldProtocol.Status.OK,ModernWorldProtocol.Reason.NONE,world));
        var enabled=new ModernSessionBoundary.Negotiated(1,15,1,ModernSessionProtocol.KNOWN_CAPABILITIES);
        assertThrows(IllegalArgumentException.class,()->forPeer(old,enabled));
        var fresh=new ModernApplicationProtocol.WorldResult(7,old.echo(),new ModernWorldProtocol.Reply(old.reply().status(),old.reply().reason(),world.withDescriptor(scene())));
        assertEquals(fresh,forPeer(fresh,enabled));
        assertEquals(old,forPeer(fresh,new ModernSessionBoundary.Negotiated(1,15,1,65535)));
        assertFalse(scene().has(COMBAT));
        assertThrows(IllegalArgumentException.class,()->new ModernCombatProtocol.Frame(world.withDescriptor(scene()),ModernCombatProtocolTest.frame().vitals()));
    }
    @Test void capabilityDependencyAndEveryLegacyAckKeepTheExistingNamespace()throws Exception{
        assertEquals(2097151,ModernSessionProtocol.KNOWN_CAPABILITIES&~(ModernSessionProtocol.CAP_SLOW_DATA|ModernSessionProtocol.CAP_HASTE|ModernSessionProtocol.CAP_GREATER_HASTE|ModernSessionProtocol.CAP_ACTION_LIFE|ModernSessionProtocol.CAP_LARGE_MAPS|ModernSessionProtocol.CAP_BATTLE_DATA|ModernSessionProtocol.CAP_MATCHMAKING|ModernSessionProtocol.CAP_NPC_FIRE_STORM|ModernSessionProtocol.CAP_CHAT|ModernSessionProtocol.CAP_DIRECT_CHAT|ModernSessionProtocol.CAP_FILTERED_CHAT));
        assertThrows(ModernSessionProtocol.ProtocolException.class,()->ModernSessionProtocol.validateCapabilities(65536,0));
        for(int v=1;v<=14;v++){
            var boundary=new ModernSessionBoundary(ModernSessionBoundary.Policy.world(true,true,true,true,true,true,true,true,true).withMaxVersion(v));
            var ack=(ModernSessionProtocol.Ack)ModernSessionProtocol.decode(boundary.acceptHello(ModernSessionProtocol.encode(new ModernSessionProtocol.Hello(1,1,15,131071,3,1))));
            assertEquals(0,ack.capabilities()&65536);assertEquals(v,ack.version());
        }
        assertEquals(0,ModernSessionBoundary.Policy.current().supportedCapabilities()&65536);
        assertEquals(0,ModernSessionBoundary.Policy.equipment().supportedCapabilities()&65536);
        assertEquals(0,ModernSessionBoundary.Policy.rooms(true).supportedCapabilities()&65536);
        assertNotEquals(0,ModernSessionBoundary.Policy.world(false).supportedCapabilities()&65536);
    }
    @Test void globallyPurchasableTimedOrAmmoStockCannotBeDroppedOrFrozenIntoAnUnqualifiedBinding(){
        for(var offer:java.util.List.of(ModernShopTimedTest.offer(ModernTimedEffects.Kind.HASTE,0),ModernCombatTestPeer.arrow(40744,0,20))){
            var row=ModernShopAmmoTest.catalog().character();var catalog=new ModernShopCatalog.Snapshot(7,"sun",row,java.util.List.of(offer));
            var authority=new ModernShopAuthority(20000,7,row,"sun",true,true);var basis=ModernShopAmmoTest.basis();
            var initial=authority.apply(ModernShopAuthority.Action.READ,0,0,null,0,0,catalog,basis).snapshot();
            var bought=authority.apply(ModernShopAuthority.Action.BUY,initial.roundId(),initial.revision(),initial.token(),offer.itemId(),2,catalog,basis).snapshot();
            var limited=new ModernMatchDescriptor(Ruleset.PVP_SKIRMISH,COMBAT|ITEMS,3000,30000,0,300000);
            long deadline=ModernShopAuthorityTest.deadline();
            assertThrows(IllegalArgumentException.class,()->authority.capture(1,1,deadline,catalog,basis,limited));
            assertFalse(authority.frozen());assertEquals(bought,authority.apply(ModernShopAuthority.Action.READ,0,0,null,0,0,catalog,basis).snapshot());
            assertThrows(IllegalArgumentException.class,()->authority.capture(1,1,deadline,catalog,basis,scene()));
            var allowed=new ModernMatchDescriptor(Ruleset.PVP_SKIRMISH,127,3000,30000,0,300000);
            var stocks=authority.capture(1,1,deadline,catalog,basis,allowed);assertEquals(1,stocks.size());assertEquals(2,stocks.get(0).quantity());
            assertEquals(offer,stocks.get(0).offer());authority.terminal(1,1);authority.terminal(1,1);
            var reset=authority.apply(ModernShopAuthority.Action.READ,0,0,null,0,0,catalog,basis).snapshot();assertEquals(bought.roundId()+1,reset.roundId());assertEquals(20000,reset.balance());
        }
    }
}
