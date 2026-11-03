package local.lito381.server;

import static org.junit.jupiter.api.Assertions.*;
import static local.lito381.server.ModernInventoryWeight.*;
import static local.lito381.server.ModernCharacterData.*;
import local.lito381.server.ModernInventoryWeight.Snapshot;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class ModernInventoryWeightTest {
    static ModernCharacterData.Snapshot source(List<InventoryItem> inventory) {
        return new ModernCharacterData.Snapshot(1,new ModernApplicationProtocol.CharacterRow(3333,"Weight Fixture",0,0,80),
                new PersistedValues(100,100,50,50,10,20,20,20,20,20,20,0,0,13,10,10,10,10,225),inventory);
    }
    static InventoryItem item(long id, ItemKind kind, long quantity, long weight, boolean equipped) {
        return new InventoryItem(id,new ItemTemplate(id,kind,"$1",1,1,weight),quantity,equipped,0,0,true,0,false,0);
    }
    static ModernEquipmentAuthority.Snapshot equipment(List<InventoryItem> items, List<ModernItemLifetime.View> lives, long until) {
        return new ModernEquipmentAuthority.Snapshot(source(items),0,ModernEquipmentAuthority.ZERO,ModernEquipmentAuthority.ZERO,
                12,until,ModernEquipmentFacts.Ability.zero(),0,items.stream().map(i->new ModernEquipmentAuthority.Equipped(i.objectId(),i.equipped())).toList(),List.of(),lives);
    }
    static ModernEquipmentAuthority.Snapshot equipment(List<InventoryItem> items) {
        return equipment(items,items.stream().map(i->new ModernItemLifetime.View(i.objectId(),ModernItemLifetime.State.PERMANENT,-1)).toList(),-1);
    }
    static Basis basis(long gear, double max) { return new Basis(source(List.of()),0,gear,max,1,-1); }
    static Snapshot weight(long gear, double max, List<Carried> stocks) { return evaluate(basis(gear,max),stocks); }

    @Test void nativePerStackFloorDoesNotAggregateSeparateMinimumWeightStacks() {
        assertEquals(1,itemWeight(1,1)); assertEquals(1,itemWeight(999,1)); assertEquals(1,itemWeight(1000,1));
        assertEquals(2,itemWeight(2001,1)); assertEquals(0,itemWeight(0,Long.MAX_VALUE)); assertEquals(0,itemWeight(Long.MAX_VALUE,0));
        assertEquals(2,weight(0,3150,List.of(new Carried(1,1,1),new Carried(2,1,1))).view().suppliesWeight());
    }
    @Test void fullI64WeightAndWideIntermediateAreLosslessButTrueOverflowRejects() {
        assertEquals(4_000_000_000L,itemWeight(4_000_000_000L,1000));
        assertEquals(Long.MAX_VALUE,itemWeight(Long.MAX_VALUE,1000));
        assertThrows(ArithmeticException.class,()->itemWeight(Long.MAX_VALUE,1001));
        assertThrows(IllegalArgumentException.class,()->itemWeight(-1,1));
        assertThrows(IllegalArgumentException.class,()->itemWeight(1,-1));
        assertThrows(ArithmeticException.class,()->weight(1,Double.MAX_VALUE,List.of(new Carried(1,Long.MAX_VALUE,1000))));
    }
    @Test void aggregateCannotWrapAcrossStacks() {
        assertThrows(ArithmeticException.class,()->weight(0,Double.MAX_VALUE,
                List.of(new Carried(1,Long.MAX_VALUE,1000),new Carried(2,1,1))));
    }
    @Test void orderedBinary64MatchesRetainedNativeFormulaForAllSevenPositiveOriginalClasses() {
        for(int type=0;type<7;type++)for(int original=1;original<=25;original++)for(int stat:new int[]{0,1,13,20,127,32768,Integer.MAX_VALUE})
            for(int bonus:new int[]{-50,0,1,50,125})for(double rate:new double[]{0.01,0.1,1,1.3,100}) {
                double expected=BattleInventoryWeight.maxWeight(new BattleInventoryWeight.Profile(0,rate,type,stat,stat,original,original,bonus,0,false));
                double actual=maximumWeight(new Parameters(type,stat,stat,original,original,bonus,rate));
                assertEquals(Double.doubleToRawLongBits(expected),Double.doubleToRawLongBits(actual));
            }
        assertEquals(3150,maximumWeight(new Parameters(0,20,20,13,10,0,1)));
        assertEquals(3465.0000000000005,maximumWeight(new Parameters(0,20,20,13,10,10,1)));
    }
    @Test void hugeModernAbilityDoesNotNarrowToNativeInt() {
        assertEquals(3,originalStrengthReduction(0,Long.MAX_VALUE)); assertEquals(2,originalConstitutionReduction(6,Long.MAX_VALUE));
        double max=maximumWeight(new Parameters(0,Long.MAX_VALUE,Long.MAX_VALUE,Long.MAX_VALUE,Long.MAX_VALUE,0,1));
        assertTrue(Double.isFinite(max)); assertTrue(max>Long.MAX_VALUE);
    }
    @Test void invalidCapacityAndUninitializedOriginalFactsCannotInventBonus() {
        for(double rate:new double[]{0,-0.0,-1,Double.NaN,Double.POSITIVE_INFINITY})
            assertThrows(IllegalArgumentException.class,()->new Parameters(0,20,20,13,10,0,rate));
        assertThrows(IllegalArgumentException.class,()->new Parameters(0,20,20,0,10,0,1));
        assertThrows(IllegalArgumentException.class,()->new Parameters(0,20,20,13,0,0,1));
        assertThrows(IllegalArgumentException.class,()->maximumWeight(new Parameters(0,20,20,13,10,-100,1)));
        assertThrows(IllegalArgumentException.class,()->maximumWeight(new Parameters(0,Long.MAX_VALUE,Long.MAX_VALUE,13,10,Long.MAX_VALUE,Double.MAX_VALUE)));
    }
    @Test void stageUsesNativeRoundingAndClampsOnlyActualOverCapacity() {
        assertEquals(196,weight240(1964,2400)); assertEquals(197,weight240(1965,2400));
        assertEquals(239,weight240(2394,2400)); assertEquals(240,weight240(2395,2400));
        assertEquals(240,weight240(Long.MAX_VALUE,2400)); assertEquals(0,weight240(0,2400));
        assertThrows(IllegalArgumentException.class,()->weight240(1,-0.0));
    }
    @Test void tournamentBuyAddsOneEvenForZeroWeightAndDiffersFromClassicWholeOrder() {
        assertTrue(permitsAdd(weight(2393,2400,List.of()).view(),1,0));
        assertFalse(permitsAdd(weight(2394,2400,List.of()).view(),1,0));
        assertFalse(permitsAdd(weight(0,3150,List.of()).view(),Long.MAX_VALUE,Long.MAX_VALUE));
        assertFalse(permitsAdd(weight(0,3150,List.of()).view(),0,1));
        assertFalse(permitsAdd(notApplicable().view(),1,1));
        var nativeProfile=new BattleInventoryWeight.Profile(2995,1,0,19,19,13,10,0,0,false);
        assertTrue(BattleInventoryWeight.permitsShopOrder(nativeProfile,0));
        assertFalse(BattleInventoryWeight.permitsAdd(nativeProfile,1,0));
    }
    @Test void sourceScopeIncludesUnequippedGearButNeverSourceEtcOrAdena() {
        var e=equipment(List.of(item(1,ItemKind.WEAPON,1,2000,true),item(2,ItemKind.ARMOR,1,3000,false),
                item(3,ItemKind.ETC,Long.MAX_VALUE,Long.MAX_VALUE,false)));
        var b=qualify(e,1); assertEquals(5,b.sourceGearWeight()); assertEquals(3150,b.maximumWeight());
        assertEquals(315000,qualify(e,100).maximumWeight());
        assertEquals(0,qualify(equipment(List.of(item(1,ItemKind.ETC,1,1,false)),
                List.of(new ModernItemLifetime.View(1,ModernItemLifetime.State.UNQUALIFIED,-1)),-1),1).sourceGearWeight());
    }
    @Test void expiredGearExcludedButUnknownGearFailsAndFutureExpiredIsNotTrusted() {
        var items=List.of(item(1,ItemKind.ARMOR,1,Long.MAX_VALUE,false));
        assertEquals(0,qualify(equipment(items,List.of(new ModernItemLifetime.View(1,ModernItemLifetime.State.EXPIRED,12)),-1),1).sourceGearWeight());
        assertThrows(IllegalArgumentException.class,()->qualify(equipment(items,List.of(new ModernItemLifetime.View(1,ModernItemLifetime.State.UNQUALIFIED,-1)),-1),1));
        assertThrows(IllegalArgumentException.class,()->qualify(equipment(items,List.of(new ModernItemLifetime.View(1,ModernItemLifetime.State.EXPIRED,13)),-1),1));
        assertThrows(IllegalArgumentException.class,()->qualify(equipment(items,List.of(),-1),1));
    }
    @Test void allCarriedTimedGearPinsLifetimeEvenWhenUnequipped() {
        var items=List.of(item(1,ItemKind.ARMOR,1,1000,false));
        assertEquals(20,qualify(equipment(items,List.of(new ModernItemLifetime.View(1,ModernItemLifetime.State.ACTIVE,20)),-1),1).validUntilMillis());
        assertEquals(15,qualify(equipment(items,List.of(new ModernItemLifetime.View(1,ModernItemLifetime.State.ACTIVE,20)),15),1).validUntilMillis());
        assertThrows(IllegalArgumentException.class,()->qualify(equipment(items,List.of(new ModernItemLifetime.View(1,ModernItemLifetime.State.ACTIVE,12)),-1),1));
    }
    @Test void confirmedConsumptionReducesWeightAcrossAttackThresholdAndRetainsDepletedUnit() {
        var b=basis(2575,3150); var unit=List.of(new Unit(40010,3000));
        var before=world(b,List.of(new ModernItemProtocol.Stock(40010,2,2)),unit);
        assertEquals(2581,before.view().totalWeight()); assertEquals(197,before.view().stage()); assertFalse(before.view().permitsAttackCast());
        var after=world(b,List.of(new ModernItemProtocol.Stock(40010,2,1)),unit);
        assertEquals(2578,after.view().totalWeight()); assertEquals(196,after.view().stage()); assertTrue(after.view().permitsAttackCast());
        var empty=world(b,List.of(new ModernItemProtocol.Stock(40010,2,0)),unit);
        assertEquals(unit,empty.units()); assertEquals(0,empty.view().suppliesWeight());
        assertThrows(IllegalArgumentException.class,()->world(b,List.of(new ModernItemProtocol.Stock(40011,2,1)),unit));
    }
    @Test void hitPenaltyKeepsNativeIntervalsAndDoesNotMasqueradeAsDamageOrMovementGate() {
        for(int stage=0;stage<=240;stage++) {
            int expected=stage>=81&&stage<=120?1:stage>=121&&stage<=160?3:stage>=161&&stage<=200?5:0;
            assertEquals(expected,hitPenalty(stage));
            var view=new View(stage,0,stage,240,stage);
            assertEquals(stage<197,view.permitsAttackCast()); assertEquals(stage<120,view.permitsNaturalRegen());
        }
    }
    @Test void sameFactsIgnoresQueryAnchorOnlyNotSourceEquipmentRateOrCapacity() {
        var b=basis(5,3150);
        assertTrue(b.sameFacts(new Basis(b.source(),0,5,3150,1,100)));
        for(var other:List.of(new Basis(b.source(),1,5,3150,1,-1),new Basis(b.source(),0,6,3150,1,-1),
                new Basis(b.source(),0,5,3151,1,-1),new Basis(b.source(),0,5,3150,2,-1),
                new Basis(new ModernCharacterData.Snapshot(2,b.source().character(),b.source().persisted(),b.source().inventory()),0,5,3150,1,-1)))
            assertFalse(b.sameFacts(other));
        assertFalse(b.sameFacts(null)); assertFalse(b.toString().contains("3333"));
    }
    @Test void botNotApplicableIsCanonicalAndDistinctFromEmptyHuman() {
        assertFalse(notApplicable().view().applicable()); assertTrue(weight(0,3150,List.of()).view().applicable());
        assertThrows(IllegalArgumentException.class,()->new View(0,0,0,-0.0,0));
        assertThrows(IllegalArgumentException.class,()->new View(0,0,0,Double.NaN,0));
        assertThrows(IllegalArgumentException.class,()->new View(1,0,1,0,0));
        assertThrows(IllegalArgumentException.class,()->new View(1,2,4,3150,0));
        assertThrows(IllegalArgumentException.class,()->new Snapshot(notApplicable().view(),List.of(new Unit(1,0))));
    }
    @Test void unitRowsAreImmutableSortedBoundedAndRejectDuplicateStacks() {
        var rows=new ArrayList<Unit>();rows.add(new Unit(1,1));
        var snapshot=new Snapshot(weight(0,3150,List.of()).view(),rows); rows.clear();assertEquals(1,snapshot.units().size());
        assertThrows(UnsupportedOperationException.class,()->snapshot.units().clear());
        assertThrows(IllegalArgumentException.class,()->weight(0,3150,List.of(new Carried(1,1,1),new Carried(1,2,1))));
        assertThrows(IllegalArgumentException.class,()->weight(0,3150,List.of(new Carried(2,1,1),new Carried(1,2,1))));
        var tooMany=new ArrayList<Carried>();for(int i=1;i<=257;i++)tooMany.add(new Carried(i,0,0));
        assertThrows(IllegalArgumentException.class,()->evaluate(basis(0,3150),tooMany));
    }
}
