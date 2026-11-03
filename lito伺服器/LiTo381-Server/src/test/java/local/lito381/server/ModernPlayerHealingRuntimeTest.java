package local.lito381.server;

import static org.junit.jupiter.api.Assertions.*;
import static local.lito381.server.ModernPlayerHealingHistoryTest.*;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ModernPlayerHealingRuntimeTest {
    static ModernSpellRuntime.State runtime(ModernEquipmentHistory.State h) {
        return ModernSpellRuntime.State.initial(h.identity(),h.spells().keySet(),1,0,true);
    }
    @Test void oldRuntimeAndProjectionCannotIgnoreEvenAnEmptyHealingCapability() {
        var h=initial(7,1,true);var old=ModernSpellRuntime.State.initial(h.identity(),h.spells().keySet(),1,0);var r=runtime(h);
        assertThrows(IllegalArgumentException.class,()->old.validate(h));
        assertThrows(IllegalArgumentException.class,()->r.validate(h.worldRevision(),h.spells(),h.targets()));
        assertThrows(IllegalArgumentException.class,()->r.validate(healing(h,null)));
        assertThrows(IllegalArgumentException.class,()->ModernLegacySpellProjection.from(1,0,1200,r,h.spells()));
        assertThrows(IllegalArgumentException.class,r::latest);r.validate(h);
    }
    @Test void healingSuccessSharesSequenceAndCooldownButDeniedRequestDoesNotRestartIt() {
        var h=initial(7,1,true);var p=plan(h,h,1);var after=ModernPlayerHealingTransaction.complete(p,h,h).caster();
        var r=runtime(h).heal(h,after,p.receipt(),100);r.validate(after);
        assertEquals(1,r.sequence());assertEquals(1,r.playerHealing().casts());assertEquals(4,r.playerHealing().spent());
        assertTrue(r.cooling(109,10));assertFalse(r.cooling(110,10));assertTrue(r.healingCooling(109,10));
        var denied=r.denied(3,2);assertEquals(r.playerHealing(),denied.playerHealing());assertFalse(denied.cooling(110,10));
        assertThrows(IllegalArgumentException.class,()->r.denied(3,1));
        var observed=new ModernEquipmentHistory.State(after.identity(),3,after.resources(),after.spells(),after.items(),after.targets(),after.healing());
        var next=observed.cast(ModernUseHistory.Lane.TELEPORT,4,3,CELL,List.of());
        var both=denied.cast(observed,next,ModernUseHistory.Lane.TELEPORT,110,new ModernSpellRuntime.Teleport(new ModernWorldMap.Cell(11,10)));
        assertEquals(3,both.sequence());assertEquals(r.playerHealing(),both.playerHealing());assertTrue(both.cooling(115,10));
        assertFalse(both.healingCooling(115,10));assertEquals(0,both.lawful());
    }
    @Test void aSuccessfulHealCannotBeRepresentedAsDeniedOrLoseThePrivateScope() {
        var h=initial(7,1,true);var t=initial(42,2,false);var p=plan(h,t,1);var after=ModernPlayerHealingTransaction.complete(p,h,t).caster();
        assertThrows(IllegalArgumentException.class,()->runtime(h).denied(2,1).validate(after));
        var r=runtime(h).heal(h,after,p.receipt(),0);var receipt=p.receipt();
        var foreign=new ModernPlayerHealingTransaction.Receipt(receipt.world(),new ModernRooms.Scope(7,3333,2,0),receipt.caster(),receipt.use(),
                0,receipt.effects(),receipt.baseAmount(),receipt.adjustedAmount(),receipt.target());
        assertThrows(IllegalArgumentException.class,()->runtime(h).heal(h,after,foreign,0));
        assertEquals(receipt.impact(),foreign.impact());assertEquals(1,r.playerHealing().casts());
    }
    @Test void gearRepriceKeepsHistoricalCostAndHealingParticipatesInSignedClockWrapChecks() {
        var h=initial(7,1,true);var p=plan(h,h,1);var after=ModernPlayerHealingTransaction.complete(p,h,h).caster();
        var r=runtime(h).heal(h,after,p.receipt(),Long.MAX_VALUE-5);
        var changed=after.reprofile(3,new ModernEquipmentHistory.Profile(1,100,100,Map.of(ModernUseHistory.Lane.TELEPORT,1),0L),0);
        r.validate(changed);assertEquals(4,r.playerHealing().spent());
        var p2=plan(changed,changed,2);var next=ModernPlayerHealingTransaction.complete(p2,changed,changed).caster();
        assertThrows(IllegalArgumentException.class,()->r.heal(changed,next,p2.receipt(),Long.MAX_VALUE-6));
        assertTrue(r.cooling(Long.MIN_VALUE+4,11));assertFalse(r.cooling(Long.MIN_VALUE+4,10));
        var both=r.heal(changed,next,p2.receipt(),Long.MIN_VALUE+4);assertEquals(2,both.playerHealing().casts());assertEquals(4,both.playerHealing().spent());
        assertEquals(0,both.playerHealing().receipt().use().cost());
    }
    @Test void otherResourceChangesCannotPiggybackOnHealingSuccess() {
        var h=initial(7,1,true);var p=plan(h,h,1);var after=ModernPlayerHealingTransaction.complete(p,h,h).caster();
        var changed=resources(after,after.resources().damage(3,1));
        assertThrows(IllegalArgumentException.class,()->runtime(h).heal(h,changed,p.receipt(),0));
        var r=runtime(h).heal(h,after,p.receipt(),0);
        assertThrows(IllegalArgumentException.class,()->new ModernSpellRuntime.State(r.identity(),r.lanes(),r.birthRevision(),r.sequence(),r.requestRevision(),
                r.initialLawful(),r.lawful(),r.receipts(),r.structure(),false,r.playerHealing()));
        assertEquals(0,h.resources().flows().spent());
    }
}
