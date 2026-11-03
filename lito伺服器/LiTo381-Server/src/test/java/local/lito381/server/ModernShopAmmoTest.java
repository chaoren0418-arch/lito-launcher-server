package local.lito381.server;

import static org.junit.jupiter.api.Assertions.*;
import static local.lito381.server.ModernShopAuthority.*;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class ModernShopAmmoTest {
    static ModernShopCatalog.Snapshot catalog(){return new ModernShopCatalog.Snapshot(7,"sun",ModernShopTimedTest.row(1),List.of(ModernCombatTestPeer.arrow(40744,0,20),ModernCombatTestPeer.arrow(40746,1,30)));}
    static ModernInventoryWeight.Basis basis(){return ModernShopWeightTest.basis(0,0,1000);}
    static Snapshot read(ModernShopAuthority a,ModernShopCatalog.Snapshot c){return a.apply(Action.READ,0,0,null,0,0,c,basis()).snapshot();}
    static Outcome buy(ModernShopAuthority a,ModernShopCatalog.Snapshot c,Snapshot s,long id,long count){return a.apply(Action.BUY,s.roundId(),s.revision(),s.token(),id,count,c,basis());}
    @Test void exactArrowTuplesRetainSourceDiceAndDoNotQualifyMissingRawEvidence() {
        assertEquals(4,ModernShopCatalog.Support.AMMO.ordinal());
        for(var o:catalog().offers()) {assertEquals(ModernShopCatalog.Support.AMMO,o.support());assertNull(o.healing());assertNull(o.timed());
            var t=o.template();var absent=new ModernShopCatalog.Template(t.kind(),t.nameToken(),t.inventoryGraphic(),t.groundGraphic(),t.weight(),t.etc());
            assertEquals(ModernShopCatalog.Support.UNIMPLEMENTED,ModernShopCatalog.Offer.fromSource(o.itemId(),0,20,20,absent).support());
            for(var arrow:List.of(new ModernShopCatalog.Arrow(0,1,-1,1),new ModernShopCatalog.Arrow(1,1,-1,0),new ModernShopCatalog.Arrow(1,1,100,1))) {
                var invalid=new ModernShopCatalog.Template(t.kind(),t.nameToken(),t.inventoryGraphic(),t.groundGraphic(),t.weight(),t.etc(),arrow);
                assertEquals(ModernShopCatalog.Support.UNSUPPORTED_TEMPLATE,ModernShopCatalog.Offer.fromSource(o.itemId(),0,20,20,invalid).support());}
        }
    }
    @Test void disabledScopeProjectsAmmoUnsupportedWithDistinctTokenAndCannotBuy() {
        var c=catalog();var on=new ModernShopAuthority(1000,7,c.character(),"sun",true,true);var off=new ModernShopAuthority(1000,7,c.character(),"sun",true);
        var yes=read(on,c);var no=read(off,c);assertTrue(yes.ammoEnabled());assertFalse(no.ammoEnabled());assertNotEquals(yes.token(),no.token());
        assertTrue(no.offers().stream().allMatch(o->o.support()==ModernShopCatalog.Support.UNIMPLEMENTED&&o.ammo()==null));
        assertEquals(Reason.UNSUPPORTED_ITEM,buy(off,c,no,40744,1).reason());assertTrue(read(off,c).stacks().isEmpty());
    }
    @Test void purchasesCaptureOriginalArrowFactsAndTerminalResetsWithoutSourceMutation() {
        var c=catalog();var a=new ModernShopAuthority(1000,7,c.character(),"sun",true,true);var first=read(a,c);var bought=buy(a,c,first,40744,3);
        assertEquals(940,bought.snapshot().balance());assertEquals(Reason.STALE_REVISION,buy(a,c,first,40744,3).reason());
        var captured=a.capture(10,20,System.nanoTime()+1_000_000_000L,c,basis());assertEquals(1,captured.size());assertEquals(3,captured.get(0).quantity());
        assertEquals(c.offers().get(0).ammo(),captured.get(0).offer().ammo());assertEquals(Status.FROZEN,buy(a,c,bought.snapshot(),40746,1).status());
        a.terminal(10,20);a.terminal(10,20);var next=read(a,c);assertEquals(2,next.roundId());assertEquals(1000,next.balance());assertTrue(next.stacks().isEmpty());
        assertThrows(IllegalArgumentException.class,()->a.capture(10,20,System.nanoTime()+1_000_000_000L,c,basis()));
    }
    @Test void changedSourceArrowDieCannotBeCapturedOrMergedWithOwnedStack() {
        var c=catalog();var a=new ModernShopAuthority(1000,7,c.character(),"sun",true,true);var bought=buy(a,c,read(a,c),40744,1).snapshot();
        var offers=new ArrayList<>(c.offers());var old=offers.get(0);var t=old.template();offers.set(0,ModernShopCatalog.Offer.fromSource(old.itemId(),0,20,20,
                new ModernShopCatalog.Template(t.kind(),t.nameToken(),t.inventoryGraphic(),t.groundGraphic(),t.weight(),t.etc(),new ModernShopCatalog.Arrow(8,6,-1,1))));
        var changed=new ModernShopCatalog.Snapshot(7,"sun",c.character(),offers);assertNotEquals(c.token(),changed.token());
        assertEquals(Status.CONFLICT,buy(a,changed,bought,40744,1).status());
        assertThrows(IllegalArgumentException.class,()->a.capture(1,1,System.nanoTime()+1_000_000_000L,changed,basis()));
    }
    @Test void v11ArrowAppendixRoundtripsAndOldVersionsCannotAcceptSupportFour()throws Exception {
        var c=catalog();var a=new ModernShopAuthority(1000,7,c.character(),"sun",true,true);var snapshot=read(a,c);var base=ModernShopTimedTest.reply(true,true);
        var message=new ModernApplicationProtocol.ShopResult(1,base.echo(),new ModernShopProtocol.Reply(new Outcome(Status.OK,Reason.NONE,snapshot),base.reply().rooms()));
        byte[] bytes=ModernApplicationProtocol.encode(message,11);assertEquals(message,ModernApplicationProtocol.decode(bytes,11));
        int start=bytes.length-72;assertEquals(1,bytes[start]);assertEquals(2,ByteBuffer.wrap(bytes).getShort(start+4));
        assertEquals(40744,ByteBuffer.wrap(bytes).getLong(start+8));assertEquals(7,ByteBuffer.wrap(bytes).getLong(start+16));
        for(int version=1;version<=10;version++){int prior=version;assertThrows(ModernApplicationProtocol.ProtocolException.class,()->ModernApplicationProtocol.encode(message,prior));assertThrows(ModernApplicationProtocol.ProtocolException.class,()->ModernApplicationProtocol.decode(bytes,prior));}
        for(int offset:new int[]{1,2,3,6,7,8+31}){byte[] invalid=bytes.clone();invalid[start+offset]=1;assertThrows(ModernApplicationProtocol.ProtocolException.class,()->ModernApplicationProtocol.decode(invalid,11));}
        var noSnapshot=new ModernApplicationProtocol.ShopResult(2,base.echo(),ModernShopProtocol.Reply.exceptional(Status.BUSY,Reason.BUSY));
        assertEquals(128,ModernApplicationProtocol.encode(noSnapshot,11).length);
    }
}
