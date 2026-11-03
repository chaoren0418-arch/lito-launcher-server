package local.lito381.server;

import static org.junit.jupiter.api.Assertions.*;
import static local.lito381.server.ModernShopAuthority.*;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.Test;

class ModernShopAuthorityTest {
    static ModernApplicationProtocol.CharacterRow row(){return new ModernApplicationProtocol.CharacterRow(3333,"fixture",1,0,80);}
    static ModernShopCatalog.Offer offer(long buy,long sell) {
        var etc=new ModernShopCatalog.Etc("hp.UserAddHp 10 20 189 true 0 0","potion","hpr","glass",1,0,0,0,0,0,0,2,500,0,0,0,127);
        return ModernShopCatalog.Offer.fromSource(40010,0,buy,sell,new ModernShopCatalog.Template(ModernCharacterData.ItemKind.ETC,"$26",80,45,3900,etc));
    }
    static ModernShopCatalog.Snapshot catalog(){return catalog(40,40);}
    static ModernShopCatalog.Snapshot catalog(long buy,long sell){return new ModernShopCatalog.Snapshot(7,"sun",row(),List.of(offer(buy,sell)));}
    static ModernShopAuthority authority(long allowance){return new ModernShopAuthority(allowance,7,row(),"sun");}
    static Snapshot read(ModernShopAuthority authority,ModernShopCatalog.Snapshot catalog){return authority.apply(Action.READ,0,0,null,0,0,catalog).snapshot();}
    static Outcome buy(ModernShopAuthority authority,Snapshot view,long qty,ModernShopCatalog.Snapshot catalog){return authority.apply(Action.BUY,view.roundId(),view.revision(),view.token(),40010,qty,catalog);}
    static Outcome sell(ModernShopAuthority authority,Snapshot view,long qty,ModernShopCatalog.Snapshot catalog){return authority.apply(Action.SELL,view.roundId(),view.revision(),view.token(),40010,qty,catalog);}
    static long deadline(){return System.nanoTime()+TimeUnit.SECONDS.toNanos(3);}

    @Test void trueBuySellAreAtomicAndSourceSnapshotRemainsUnchanged() {
        var catalog=catalog();var original=catalog.offers();var authority=authority(20000);var before=read(authority,catalog);
        var purchased=buy(authority,before,3,catalog);assertEquals(Status.OK,purchased.status());assertEquals(19880,purchased.snapshot().balance());
        assertEquals(List.of(new Owned(40010,3,40)),purchased.snapshot().stacks());assertEquals(1,purchased.snapshot().revision());
        var sold=sell(authority,purchased.snapshot(),2,catalog);assertEquals(19960,sold.snapshot().balance());assertEquals(1,sold.snapshot().stacks().get(0).quantity());
        assertEquals(original,catalog.offers());assertEquals(20000,sell(authority,sold.snapshot(),1,catalog).snapshot().balance());
    }
    @Test void insufficientBalanceAndExcessSellCannotPartiallyChangeWalletOrBag() {
        var a=authority(100);var c=catalog();var v=read(a,c);
        assertEquals(Reason.INSUFFICIENT_FUNDS,buy(a,v,3,c).reason());assertEquals(v,read(a,c));
        assertEquals(Reason.NOT_OWNED,sell(a,v,1,c).reason());assertEquals(v,read(a,c));
        var next=buy(a,v,2,c).snapshot();assertEquals(Reason.QUANTITY_LIMIT,sell(a,next,3,c).reason());assertEquals(next,read(a,c));
    }
    @Test void duplicateMutationUsesRevisionFenceWithoutDoubleCharging() {
        var a=authority(20000);var c=catalog();var old=read(a,c);var first=buy(a,old,2,c);
        var repeat=buy(a,old,2,c);assertEquals(Status.CONFLICT,repeat.status());assertEquals(first.snapshot(),repeat.snapshot());
    }
    @Test void changedCatalogReturnsNewRevisionBeforeAnyChargeAndOldStackCannotMergeNewPrice() {
        var a=authority(20000);var c=catalog();var old=buy(a,read(a,c),2,c).snapshot();var changed=catalog(50,70);
        var stale=buy(a,old,1,changed);assertEquals(Status.CONFLICT,stale.status());assertEquals(old.balance(),stale.snapshot().balance());
        assertEquals(old.revision()+1,stale.snapshot().revision());assertEquals(Reason.CATALOG_CHANGED,buy(a,stale.snapshot(),1,changed).reason());
        var sold=sell(a,stale.snapshot(),2,changed);assertEquals(20000,sold.snapshot().balance());assertTrue(sold.snapshot().stacks().isEmpty());
    }
    @Test void sellPriceAboveBuyNeverMintsAllowanceEvenAcrossRepeatedCycles() {
        var a=authority(100);var c=catalog(40,Long.MAX_VALUE);
        for(int i=0;i<20;i++){var bought=buy(a,read(a,c),2,c);assertEquals(20,bought.snapshot().balance());assertEquals(100,sell(a,bought.snapshot(),2,c).snapshot().balance());}
    }
    @Test void arithmeticOverflowAndQuantityCeilingLeaveConfirmedStateIntact() {
        var a=authority(Long.MAX_VALUE);var c=catalog(Long.MAX_VALUE,1);var old=read(a,c);
        assertThrows(ArithmeticException.class,()->buy(a,old,2,c));assertEquals(old,read(a,c));
        var cheap=catalog(1,1);var active=read(a,cheap);var max=buy(a,active,MAX_QUANTITY,cheap).snapshot();
        assertEquals(Reason.QUANTITY_LIMIT,buy(a,max,1,cheap).reason());assertEquals(max,read(a,cheap));
    }
    @Test void wrongSourceCanonicalCharacterOrMissingCatalogCannotActAsSameIdentity() {
        var a=authority(100);var c=catalog();var original=read(a,c);
        for(var wrong:List.of(new ModernShopCatalog.Snapshot(42,"sun",row(),c.offers()),new ModernShopCatalog.Snapshot(7,"moon",row(),c.offers()),
                new ModernShopCatalog.Snapshot(7,"sun",new ModernApplicationProtocol.CharacterRow(3333,"changed",1,0,80),c.offers())))
            assertThrows(SecurityException.class,()->read(a,wrong));
        assertEquals(original,read(a,c));
    }
    @Test void sourceIdCollisionDoesNotShareWalletOrPurchasedStacks() {
        var a=authority(100);var c=catalog();var moon=new ModernShopCatalog.Snapshot(42,"moon",row(),c.offers());
        var b=new ModernShopAuthority(100,42,row(),"moon");buy(a,read(a,c),1,c);
        assertEquals(100,read(b,moon).balance());assertTrue(read(b,moon).stacks().isEmpty());
    }
    @Test void capturePreservesQualifiedOriginalOfferButFreshlyRechecksTemplateAndPriceChangeIsSafe() {
        var a=authority(100);var c=catalog();buy(a,read(a,c),1,c);var captured=a.capture(1,2,deadline(),catalog(60,30));
        assertEquals(List.of(new Stack(offer(40,40),1)),captured);assertTrue(a.frozen());
        assertEquals(Status.FROZEN,a.apply(Action.READ,0,0,null,0,0,c).status());
        assertThrows(UnsupportedOperationException.class,()->captured.clear());
    }
    @Test void deletedOrChangedHealingTemplateNeverEntersWorldAndFailedCaptureDoesNotFreeze() {
        var a=authority(100);var c=catalog();buy(a,read(a,c),1,c);
        assertThrows(IllegalArgumentException.class,()->a.capture(1,2,deadline(),new ModernShopCatalog.Snapshot(7,"sun",row(),List.of())));
        var old=offer(40,40).template();var altered=new ModernShopCatalog.Template(old.kind(),old.nameToken(),old.inventoryGraphic(),old.groundGraphic(),3901,old.etc());
        assertThrows(IllegalArgumentException.class,()->a.capture(1,2,deadline(),new ModernShopCatalog.Snapshot(7,"sun",row(),List.of(ModernShopCatalog.Offer.fromSource(40010,0,40,40,altered)))));
        assertFalse(a.frozen());assertEquals(1,a.capture(1,2,deadline(),c).size());
    }
    @Test void terminalResetsExactlyOnceAndTombstoneRejectsLateCaptureEvenIfCaptureNeverStarted() {
        var a=authority(100);var c=catalog();buy(a,read(a,c),2,c);a.terminal(4,20);var fresh=read(a,c);
        assertEquals(2,fresh.roundId());assertEquals(100,fresh.balance());assertTrue(fresh.stacks().isEmpty());
        a.terminal(4,20);a.terminal(3,18);assertEquals(fresh,read(a,c));
        assertThrows(IllegalArgumentException.class,()->a.capture(4,20,deadline(),c));
        assertThrows(IllegalArgumentException.class,()->a.terminal(4,21));
        a.capture(5,21,deadline(),c);a.terminal(5,21);assertEquals(3,read(a,c).roundId());
    }
    @Test void closedSelectionAndExpiredCaptureNeverReviveOrAlterSource() {
        var a=authority(100);var c=catalog();var before=read(a,c);
        assertThrows(IllegalArgumentException.class,()->a.capture(1,2,System.nanoTime()-1,c));assertEquals(before,read(a,c));
        a.close();a.terminal(1,2);assertThrows(IllegalStateException.class,()->read(a,c));assertThrows(IllegalStateException.class,()->a.capture(2,3,deadline(),c));
    }
    @Test void concurrentSameRevisionHasOneWinnerAndOneConfirmedConflict() throws Exception {
        var a=authority(100);var c=catalog();var before=read(a,c);var workers=Executors.newFixedThreadPool(2);
        try{var one=workers.submit(()->buy(a,before,1,c));var two=workers.submit(()->buy(a,before,1,c));var results=List.of(one.get(2,TimeUnit.SECONDS),two.get(2,TimeUnit.SECONDS));
            assertEquals(1,results.stream().filter(r->r.status()==Status.OK).count());assertEquals(1,read(a,c).stacks().get(0).quantity());assertEquals(60,read(a,c).balance());}
        finally{workers.shutdownNow();assertTrue(workers.awaitTermination(2,TimeUnit.SECONDS));}
    }
    @Test void fullUnsignedWidthAndViewBoundsRemainExplicit() {
        var huge=new Product(Long.MAX_VALUE,65535,Long.MAX_VALUE,1,"$1",Long.MAX_VALUE,Long.MAX_VALUE,Long.MAX_VALUE,ModernShopCatalog.Support.UNIMPLEMENTED,null);
        var s=new Snapshot(1,Long.MAX_VALUE,Long.MAX_VALUE,Long.MAX_VALUE,catalog().token(),List.of(huge),List.of());assertEquals(Long.MAX_VALUE,s.offers().get(0).itemId());
        assertThrows(IllegalArgumentException.class,()->new Snapshot(1,0,100,101,catalog().token(),List.of(),List.of()));
        assertThrows(IllegalArgumentException.class,()->new Snapshot(1,0,100,100,catalog().token(),List.of(huge,huge),List.of()));
        assertThrows(IllegalArgumentException.class,()->new Product(1,0,1,1,"\u00a0",0,0,0,ModernShopCatalog.Support.UNIMPLEMENTED,null));
    }
}
