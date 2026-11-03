package local.lito381.server;

import static org.junit.jupiter.api.Assertions.*;
import static local.lito381.server.ModernShopAuthority.*;
import java.util.List;
import org.junit.jupiter.api.Test;

class ModernShopWeightTest {
    static ModernInventoryWeight.Basis basis(long revision,long raw,double maximum) {
        var source=ModernPhysicalCombatTest.createFighter(7,ModernShopAuthorityTest.row(),100,10,0).profile().source();
        return new ModernInventoryWeight.Basis(source,revision,raw,maximum,1,-1);
    }
    static Snapshot read(ModernShopAuthority a,ModernInventoryWeight.Basis b) {
        return a.apply(Action.READ,0,0,null,0,0,ModernShopAuthorityTest.catalog(),b).snapshot();
    }
    static Outcome mutate(ModernShopAuthority a,Snapshot s,Action action,long quantity,ModernInventoryWeight.Basis b) {
        return a.apply(action,s.roundId(),s.revision(),s.token(),40010,quantity,ModernShopAuthorityTest.catalog(),b);
    }
    @Test void nativePlusOneBoundaryRejectsPurchaseBeforeWalletOrStockMutation() {
        var a=ModernShopAuthorityTest.authority(20000);var b=basis(0,0,8);var s=read(a,b);
        // Two red pots weigh seven, but the native admission projection is eight.
        var denied=mutate(a,s,Action.BUY,2,b);assertEquals(Reason.TOO_HEAVY,denied.reason());
        assertEquals(s,denied.snapshot());assertEquals(s,read(a,b));
        var bought=mutate(a,s,Action.BUY,1,b);assertEquals(Status.OK,bought.status());
        assertEquals(3,bought.snapshot().weight().view().suppliesWeight());
        assertEquals(List.of(new ModernInventoryWeight.Unit(40010,3900)),bought.snapshot().weight().units());
    }
    @Test void freshEquipmentCapacityChangeIncrementsRevisionAndRejectsOldBuyCas() {
        var a=ModernShopAuthorityTest.authority(20000);var old=read(a,basis(0,0,100));
        var changed=basis(1,80,100);var conflict=mutate(a,old,Action.BUY,1,changed);
        assertEquals(Status.CONFLICT,conflict.status());assertEquals(Reason.STALE_REVISION,conflict.reason());
        assertEquals(old.revision()+1,conflict.snapshot().revision());assertEquals(20000,conflict.snapshot().balance());
        assertEquals(80,conflict.snapshot().weight().view().sourceGearWeight());
        assertEquals(conflict.snapshot(),read(a,changed));
    }
    @Test void overweightStillAllowsSellingAndKeepsOriginalPurchasedUnitWeight() {
        var a=ModernShopAuthorityTest.authority(20000);var b=basis(0,0,100);var bought=mutate(a,read(a,b),Action.BUY,2,b).snapshot();
        var now=basis(1,100,100);var current=read(a,now);assertEquals(240,current.weight().view().stage());
        var sold=mutate(a,current,Action.SELL,1,now);assertEquals(Status.OK,sold.status());
        assertEquals(19960,sold.snapshot().balance());assertEquals(3,sold.snapshot().weight().view().suppliesWeight());
        assertEquals(bought.weight().units(),sold.snapshot().weight().units());
    }
    @Test void queryAnchorOnlyDeadlineDoesNotChurnRevisionButExpiredBasisNeverPublishes() {
        var a=ModernShopAuthorityTest.authority(20000);var b=basis(0,0,100);var old=read(a,b);
        var anchored=new ModernInventoryWeight.Basis(b.source(),0,0,100,1,System.currentTimeMillis()+10000);
        assertEquals(old,read(a,anchored));
        var expired=new ModernInventoryWeight.Basis(b.source(),0,0,100,1,0);
        assertThrows(IllegalArgumentException.class,()->read(a,expired));assertEquals(old,read(a,b));
    }
    @Test void capturedRoundResetsStockAndWeightButDoesNotForgetSourceLoad() {
        var a=ModernShopAuthorityTest.authority(20000);var b=basis(0,20,100);var bought=mutate(a,read(a,b),Action.BUY,2,b).snapshot();
        assertEquals(2,a.capture(1,2,ModernShopAuthorityTest.deadline(),ModernShopAuthorityTest.catalog(),b).get(0).quantity());
        a.terminal(1,2);var next=read(a,b);assertEquals(bought.roundId()+1,next.roundId());assertEquals(20000,next.balance());
        assertEquals(20,next.weight().view().totalWeight());assertTrue(next.weight().units().isEmpty());
        a.terminal(1,2);assertEquals(next,read(a,b));
        assertThrows(IllegalArgumentException.class,()->a.capture(1,2,ModernShopAuthorityTest.deadline(),ModernShopAuthorityTest.catalog(),b));
    }
    @Test void weightedAuthorityCannotFallBackToMissingOrDifferentSourceFacts() {
        var a=ModernShopAuthorityTest.authority(20000);var b=basis(0,0,100);var before=read(a,b);
        assertThrows(IllegalArgumentException.class,()->read(a,null));
        var other=new ModernCharacterData.Snapshot(42,b.source().character(),b.source().persisted(),b.source().inventory());
        assertThrows(IllegalArgumentException.class,()->read(a,new ModernInventoryWeight.Basis(other,0,0,100,1,-1)));
        assertEquals(before,read(a,b));
    }
}
