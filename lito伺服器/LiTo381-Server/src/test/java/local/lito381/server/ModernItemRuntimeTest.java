package local.lito381.server;

import static org.junit.jupiter.api.Assertions.*;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class ModernItemRuntimeTest {
    static ModernUseHistory.Anchor anchor(long gear,long hp,long maximum){return new ModernUseHistory.Anchor(gear,1,new ModernEquipmentVitals.Vitals(hp,maximum,50,50),10,20);}
    static ModernItemProtocol.Stock stock(long id,long remaining){return new ModernItemProtocol.Stock(id,3,remaining);}
    static ModernUseHistory.Items initial(){return ModernUseHistory.Items.initial(0,1,8);}
    static ModernUseHistory.Items first(){return initial().use(2,1,40010,anchor(0,40,190),20);}
    static ModernItemRuntime.State used(){return ModernItemRuntime.State.initial(List.of(stock(40010,3))).used(2,1,initial(),first());}
    @Test void mixedEquipmentBonusesKeepHistoricalRestorationAndExactConsumption() {
        var old=first();var runtime=used();var changed=old.reprofile(1,3,0);runtime.validate(3,changed);
        var next=changed.use(4,2,40010,anchor(1,40,100),20);var after=runtime.used(4,2,changed,next);
        assertEquals(28,old.last().restored());assertEquals(8,old.last().fixedRecovery());assertEquals(20,next.last().restored());
        assertEquals(0,next.last().fixedRecovery());assertEquals(48,next.restored());assertEquals(2,after.requestSequence());assertEquals(4,after.requestRevision());
        assertEquals(1,after.stocks().get(0).remaining());assertEquals(2,runtime.stocks().get(0).remaining());after.validate(4,next);
    }
    @Test void deniedRequestAdvancesOnlyItsAckAndPreservesUseReceiptAndStocks() {
        var history=first();var before=used();var denied=before.requested(3,2);denied.validate(3,history);
        assertEquals(before.stocks(),denied.stocks());assertEquals(2,history.last().revision());assertEquals(1,history.last().sequence());
        assertEquals(3,denied.requestRevision());assertEquals(2,denied.requestSequence());assertEquals(189,ModernItemRuntime.effect(history));
        assertEquals(500,ModernItemRuntime.delayMillis(history));
        var after=history.use(4,3,40010,anchor(0,40,190),10);denied.used(4,3,history,after).validate(4,after);
        assertThrows(IllegalArgumentException.class,()->denied.requested(4,2));assertThrows(IllegalArgumentException.class,()->denied.requested(3,3));
    }
    @Test void timedUseKeepsPriorTotalButHasNoInventedPotionBonusOrHealingDelay() {
        var initial=initial();var state=ModernItemRuntime.State.initial(List.of(stock(40010,3),stock(40013,3)));
        var healed=first();state=state.used(2,1,initial,healed);var changed=healed.reprofile(1,3,0);
        var timed=changed.use(4,2,40013,anchor(1,50,100),0);var after=state.used(4,2,changed,timed);
        assertEquals(28,timed.restored());assertEquals(0,timed.last().restored());assertEquals(0,timed.last().fixedRecovery());
        assertEquals(191,ModernItemRuntime.effect(timed));assertEquals(0,ModernItemRuntime.delayMillis(timed));
        assertEquals(2,after.stocks().get(0).remaining());assertEquals(2,after.stocks().get(1).remaining());after.validate(4,timed);
    }
    @Test void malformedStockOrderingCounterAndReceiptBindingsAreRejected() {
        assertThrows(IllegalArgumentException.class,()->new ModernItemRuntime.State(0,0,List.of(stock(40010,2))));
        assertThrows(IllegalArgumentException.class,()->new ModernItemRuntime.State(1,0,List.of(stock(40010,3))));
        assertThrows(IllegalArgumentException.class,()->new ModernItemRuntime.State(1,1,List.of(stock(40010,3))));
        assertThrows(IllegalArgumentException.class,()->new ModernItemRuntime.State(0,0,List.of(stock(40013,3),stock(40010,3))));
        assertThrows(IllegalArgumentException.class,()->new ModernItemRuntime.State(0,0,List.of(stock(40010,3),stock(40010,3))));
        assertThrows(IllegalArgumentException.class,()->new ModernItemRuntime.State(1,2,List.of(stock(40011,2))).validate(2,first()));
        assertThrows(IllegalArgumentException.class,()->new ModernItemRuntime.State(1,2,List.of(stock(40010,3))).validate(2,first()));
        assertThrows(IllegalArgumentException.class,()->new ModernItemRuntime.State(1,3,List.of(stock(40010,2))).validate(3,first()));
        assertThrows(IllegalArgumentException.class,()->new ModernItemRuntime.State(2,2,List.of(stock(40010,2))));
        assertThrows(IllegalArgumentException.class,()->used().validate(1,first()));
        assertThrows(IllegalArgumentException.class,()->used().validate(2,first().reprofile(1,3,0)));
    }
    @Test void acceptedUseCannotSkipProfileChangesMintStockOrCommitAForeignReceipt() {
        var before=ModernItemRuntime.State.initial(List.of(stock(40010,3)));
        var repriced=initial().reprofile(1,2,0);var foreign=repriced.use(3,1,40010,anchor(1,40,100),20);
        assertThrows(IllegalArgumentException.class,()->before.used(3,1,initial(),foreign));
        assertThrows(IllegalArgumentException.class,()->before.used(3,1,initial(),first()));
        assertThrows(IllegalArgumentException.class,()->ModernItemRuntime.State.initial(List.of(stock(40011,3))).used(2,1,initial(),first()));
        assertThrows(IllegalArgumentException.class,()->used().used(3,2,first(),first()));
        assertEquals(3,before.stocks().get(0).remaining());assertEquals(0,before.requestSequence());
    }
    @Test void immutableSuppliesDeriveEffectAndDelayFromTheExactHistoricalItem() {
        var mutable=new ArrayList<>(List.of(stock(40010,3)));var before=ModernItemRuntime.State.initial(mutable);mutable.clear();
        assertEquals(1,before.stocks().size());assertThrows(UnsupportedOperationException.class,()->before.stocks().clear());
        var history=first().reprofile(1,3,0);var runtime=used();runtime.validate(3,history);
        var supplies=new ModernBattleObservation.Supplies(1,2,history,runtime.stocks(),189,500);
        assertSame(history,supplies.history());assertEquals(2,supplies.requestRevision());
        assertThrows(IllegalArgumentException.class,()->new ModernBattleObservation.Supplies(1,2,history,runtime.stocks(),191,500));
        assertThrows(IllegalArgumentException.class,()->new ModernBattleObservation.Supplies(1,2,history,runtime.stocks(),189,0));
        assertThrows(IllegalArgumentException.class,()->new ModernBattleObservation.Supplies(1,3,history,runtime.stocks(),189,500));
        assertEquals(List.of(long.class,long.class,List.class),java.util.Arrays.stream(ModernItemRuntime.State.class.getRecordComponents()).map(java.lang.reflect.RecordComponent::getType).toList());
    }
}
