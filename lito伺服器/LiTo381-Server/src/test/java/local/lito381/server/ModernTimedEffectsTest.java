package local.lito381.server;

import static org.junit.jupiter.api.Assertions.*;
import static local.lito381.server.ModernTimedEffects.*;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

class ModernTimedEffectsTest {
    static long sec(long value) { return TimeUnit.SECONDS.toNanos(value); }
    static State use(State old, Kind kind, long second, long revision, int type) {
        return apply(old, kind, sec(second), revision, type);
    }
    @Test void qualifiedKindsHaveExactNativeDurationsEffectsAndClassMasks() {
        long[] ids={40013,40014,40068,40016};int[] seconds={300,300,480,300},effects={191,751,751,750},masks={127,127,4,8};
        for(int i=0;i<4;i++) {
            var k=Kind.values()[i];assertSame(k,Kind.find(ids[i]));assertEquals(ids[i],k.itemId());
            assertEquals(sec(seconds[i]),k.durationNanos());assertEquals(seconds[i]*1000L,k.durationMillis());
            assertEquals(effects[i],k.effect());assertEquals(masks[i],k.classMask());
            for(int type=0;type<7;type++)assertEquals((masks[i]&(1<<type))!=0,k.eligible(type));
            assertFalse(k.eligible(-1));assertFalse(k.eligible(7));assertFalse(k.eligible(32));
        }
        assertNull(Kind.find(40010));assertNull(Kind.find(40018));
    }
    @Test void refreshReplacesDurationWithoutStackingWisdomPower() {
        var first=use(initial(1),Kind.WISDOM,10,1,3);
        var refreshed=use(first,Kind.WISDOM,150,2,3);
        assertEquals(sec(310),first.wisdomUntilNanos());assertEquals(sec(450),refreshed.wisdomUntilNanos());
        assertEquals(2,first.spBonus());assertEquals(2,refreshed.spBonus());
        assertEquals(1,intelligenceTerm(10,0,refreshed));assertEquals(52,intelligenceTerm(62,0,refreshed));
        assertEquals(sec(310),first.wisdomUntilNanos(),"prospective state never mutates its source");
    }
    @Test void braveAndElfAreExclusiveButHasteAndWisdomAreIndependent() {
        var s=use(initial(1),Kind.HASTE,0,1,2);
        s=use(s,Kind.ELF_BRAVE,1,2,2);assertEquals(3,s.braveKind());assertEquals(sec(481),s.braveUntilNanos());
        s=use(s,Kind.BRAVE,2,3,2);assertEquals(1,s.braveKind());assertEquals(sec(302),s.braveUntilNanos());
        assertEquals(sec(300),s.hasteUntilNanos());assertEquals(0,s.spBonus());
        var mage=use(use(initial(2),Kind.HASTE,0,1,3),Kind.WISDOM,1,2,3);
        mage=use(mage,Kind.BRAVE,2,3,3);assertTrue(mage.holds(Kind.HASTE));assertTrue(mage.holds(Kind.WISDOM));
        assertTrue(mage.holds(Kind.BRAVE));assertFalse(mage.holds(Kind.ELF_BRAVE));
    }
    @Test void exactExpiryBoundaryClearsOnlyDueLeaseAndNeverChangesAnIdleRead() {
        var s=use(use(initial(1),Kind.HASTE,0,1,3),Kind.WISDOM,5,2,3);
        assertSame(s,expire(s,sec(300)-1,3));
        var expired=expire(s,sec(300),3);
        assertEquals(0,expired.hasteUntilNanos());assertEquals(sec(305),expired.wisdomUntilNanos());
        assertEquals(3,expired.revision());assertEquals(sec(300),expired.observedNanos());
        assertSame(expired,expire(expired,sec(300)+1,4));
        var ended=expire(expired,sec(305),4);assertFalse(ended.hasEffects());assertEquals(0,ended.spBonus());
        assertSame(ended,expire(ended,sec(306),5));
    }
    @Test void applyAtExpiryDoesNotReviveAnUnrelatedExpiredLease() {
        var old=use(use(initial(1),Kind.HASTE,0,1,3),Kind.BRAVE,0,2,3);
        var next=use(old,Kind.WISDOM,300,3,3);
        assertEquals(0,next.hasteUntilNanos());assertEquals(0,next.braveUntilNanos());assertEquals(0,next.braveKind());
        assertEquals(sec(600),next.wisdomUntilNanos());
    }
    @Test void deathOrTerminalClearRetainsMutationEvidenceAndDoesNotExtendTimers() {
        var old=use(initial(1),Kind.WISDOM,0,1,3);var ended=clear(old,sec(1),2);
        assertFalse(ended.hasEffects());assertEquals(2,ended.revision());assertEquals(sec(1),ended.observedNanos());
        assertSame(ended,clear(ended,sec(2),3));assertEquals(2,old.spBonus());
    }
    @Test void invalidClassCannotConsumeOrCreateAProspectiveEffect() {
        var old=initial(1);
        assertThrows(IllegalArgumentException.class,()->use(old,Kind.WISDOM,0,1,0));
        assertThrows(IllegalArgumentException.class,()->use(old,Kind.ELF_BRAVE,0,1,3));
        assertEquals(initial(1),old);
    }
    @Test void backwardsTimeOrReusedRevisionRejectsInsteadOfRefreshing() {
        var old=use(initial(1),Kind.HASTE,5,10,0);
        assertThrows(IllegalArgumentException.class,()->use(old,Kind.HASTE,4,11,0));
        assertThrows(IllegalArgumentException.class,()->use(old,Kind.HASTE,6,10,0));
        assertThrows(IllegalArgumentException.class,()->expire(old,sec(4),11));
        assertThrows(IllegalArgumentException.class,()->clear(old,sec(6),9));
        assertThrows(IllegalArgumentException.class,()->expire(old,MAX_OBSERVED_NANOS+1,11));
    }
    @Test void initialAndLeaseRepresentationsAreCanonical() {
        assertThrows(IllegalArgumentException.class,()->initial(0));
        assertThrows(IllegalArgumentException.class,()->new State(1,0,1,0,0,0,0));
        assertThrows(IllegalArgumentException.class,()->new State(1,0,0,1,0,0,0));
        assertThrows(IllegalArgumentException.class,()->new State(1,1,2,2,0,0,0));
        assertThrows(IllegalArgumentException.class,()->new State(1,1,0,sec(300)+1,0,0,0));
        assertThrows(IllegalArgumentException.class,()->new State(1,1,0,0,1,0,0));
        assertThrows(IllegalArgumentException.class,()->new State(1,1,0,0,0,0,1));
        assertThrows(IllegalArgumentException.class,()->new State(1,1,0,0,1,0,2));
        assertThrows(IllegalArgumentException.class,()->new State(1,-1,0,0,0,0,0));
    }
    @Test void currentClassicPolicyUsesEveryNativeTruncationAndElfBraveSpeedOne() {
        var haste=use(initial(1),Kind.HASTE,0,1,2);
        var brave=use(initial(1),Kind.BRAVE,0,1,2);
        var elf=use(initial(1),Kind.ELF_BRAVE,0,1,2);
        var both=use(haste,Kind.BRAVE,0,2,2);var full=use(haste,Kind.ELF_BRAVE,0,2,2);
        for(var action:Action.values()) {
            assertEquals(755_000_000L,intervalNanos(sec(1),action,haste));
            assertEquals(755_000_000L,intervalNanos(sec(1),action,brave));
            assertEquals(570_000_000L,intervalNanos(sec(1),action,both));
            assertEquals(action==Action.ATTACK?580_000_000L:645_000_000L,intervalNanos(sec(1),action,elf));
            assertEquals(action==Action.ATTACK?438_000_000L:487_000_000L,intervalNanos(sec(1),action,full));
            assertEquals(1_000_000L,intervalNanos(1_000_000L,action,full));
        }
    }
    @Test void scalingNeverFeedsBackIntoTheFrozenBaseAndExpiryReturnsExactBase() {
        var haste=use(initial(1),Kind.HASTE,0,1,0);long base=333_000_000;
        assertEquals(251_000_000,intervalNanos(base,Action.MOVE,haste));
        assertEquals(251_000_000,intervalNanos(base,Action.MOVE,haste));
        assertEquals(base,intervalNanos(base,Action.MOVE,expire(haste,sec(300),2)));
        assertThrows(IllegalArgumentException.class,()->intervalNanos(base+1,Action.MOVE,haste));
        assertEquals(base+1,intervalNanos(base+1,Action.MOVE,initial(1)));
    }
    @Test void spellPowerUsesWideCheckedTermWithoutChangingWisdomOrReclampingIntelligence() {
        var wisdom=use(initial(1),Kind.WISDOM,0,1,3);
        assertEquals(4_000_000_002L,intelligenceTerm(4_000_000_012L,0,wisdom));
        assertThrows(ArithmeticException.class,()->intelligenceTerm(Long.MAX_VALUE,0,wisdom));
        assertThrows(ArithmeticException.class,()->intelligenceTerm(0,Long.MIN_VALUE,initial(1)));
        assertThrows(IllegalArgumentException.class,()->intelligenceTerm(-1,0,wisdom));
        assertEquals(Long.MAX_VALUE-12,intelligenceTerm(Long.MAX_VALUE,0,initial(1)));
        assertEquals(1,intelligenceTerm(11,0,wisdom));
        assertEquals(1,intelligenceTerm(20,-10,wisdom));
    }
    @Test void relativeClockSupportsLargeOrNegativeExternalNanoTimeOriginsWithoutCopyingThem() {
        long negativeOrigin=Long.MAX_VALUE-20,wrapped=negativeOrigin+100;
        long relative=wrapped-negativeOrigin;assertEquals(100,relative);
        var s=apply(initial(1),Kind.HASTE,relative,1,0);
        assertEquals(sec(300)+100,s.hasteUntilNanos());assertEquals(100,s.observedNanos());
    }
    @Test void wireRevisionPreservesInitialZeroAndCheckedGenerationIncrement() {
        assertSame(initialState,initialState.wireRevision());
        var s=use(initial(2),Kind.HASTE,0,1,0);assertEquals(2,s.wireRevision().revision());
        var overflow=new State(1,Long.MAX_VALUE,0,0,0,0,0);
        assertThrows(ArithmeticException.class,overflow::wireRevision);
    }
    private static final State initialState=initial(1);
}
