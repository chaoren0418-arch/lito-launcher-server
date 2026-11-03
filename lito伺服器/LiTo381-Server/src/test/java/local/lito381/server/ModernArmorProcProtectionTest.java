package local.lito381.server;

import static org.junit.jupiter.api.Assertions.*;
import static local.lito381.server.ModernArmorProcProtection.*;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class ModernArmorProcProtectionTest {
    static Actor actor(){return new Actor(new UUID(0,1),new TournamentPresence.CharacterKey(7,3333),1,0);}
    static Selection selection(){return new Selection(actor(),0,List.of(new Equipped(1003,ModernArmorProcsTest.rule(50,88889,-1,88,0,10))));}

    @Test void triggeringHitUsesOldStatusThenThreeSecondProtectionAppliesToLaterDamage() {
        var selection=selection();var before=initial(actor());
        assertEquals(101,protectedDamage(before,0,101));
        var candidate=prepareHurt(selection,before,0,1000,101,101,true,bound->9);
        assertEquals(899,candidate.hpAfter());assertEquals(3*SECOND,candidate.after().untilNanos());assertEquals(1,candidate.triggers().size());
        assertTrue(candidate.triggers().get(0).appliedProtection());
        var active=accept(candidate,selection,before,1000);
        assertEquals(50,protectedDamage(active,1,101));assertEquals(0,protectedDamage(active,1,1));
        assertEquals(101,protectedDamage(active,3*SECOND,101));assertEquals(0,before.untilNanos());
        assertEquals(Long.MAX_VALUE/2,protectedDamage(active,1,Long.MAX_VALUE));
    }
    @Test void failureBoundaryAndNoRefreshPreserveExistingLeaseButKeepProcVisualEvent() {
        var sel=selection();var first=prepareHurt(sel,initial(actor()),0,1000,1,1,true,b->0).after();
        var failed=prepareHurt(sel,first,SECOND,999,1,1,true,b->10);assertTrue(failed.triggers().isEmpty());
        var again=prepareHurt(sel,failed.after(),2*SECOND,998,1,1,true,b->9);
        assertEquals(3*SECOND,again.after().untilNanos());assertFalse(again.triggers().get(0).appliedProtection());assertEquals(11101,again.triggers().get(0).graphics());
        var expired=expire(again.after(),3*SECOND);assertEquals(0,expired.untilNanos());
        assertEquals(6*SECOND,prepareHurt(sel,expired,3*SECOND,997,1,1,true,b->0).after().untilNanos());
    }
    @Test void lethalHitsRollOnceButCannotGrantBuffOrReviveAndFeedbackSuppressionDoesNotRoll() {
        var calls=new AtomicInteger();var sel=selection();
        var lethal=prepareHurt(sel,initial(actor()),0,1,1,1,true,b->{calls.incrementAndGet();return 0;});
        assertEquals(1,calls.get());assertEquals(0,lethal.hpAfter());assertEquals(0,lethal.after().untilNanos());assertTrue(lethal.triggers().isEmpty());
        for(boolean feedback:new boolean[]{false,true}) {
            long damage=feedback?0:1;
            var no=prepareHurt(sel,initial(actor()),0,1000,damage,damage,feedback,b->{fail("no recursive or zero-incoming-hit roll");return 0;});
            assertTrue(no.triggers().isEmpty());assertEquals(0,no.after().untilNanos());
        }
    }
    @Test void positiveIncomingHitReducedToZeroStillTriggersAfterAcceptedHpCommit() {
        var hit=prepareHurt(selection(),initial(actor()),0,1000,10,0,true,b->0);
        assertEquals(1000,hit.hpAfter());assertEquals(3*SECOND,hit.after().untilNanos());assertEquals(1,hit.triggers().size());
        var notEligible=prepareHurt(selection(),initial(actor()),0,1000,0,0,true,b->{fail("no incoming hit");return 0;});
        assertTrue(notEligible.triggers().isEmpty());
    }
    @Test void allGroupsAndDuplicatePhysicalCopiesRollIndependentlyWithoutRefreshingFirstBuff() {
        var first=ModernArmorProcsTest.rule(50,88889,-1,88,0,100);var second=ModernArmorProcsTest.rule(51,88889,-1,89,0,100);
        var sel=new Selection(actor(),0,List.of(new Equipped(1003,first),new Equipped(1003,second),new Equipped(1004,first)));
        var calls=new AtomicInteger();var hit=prepareHurt(sel,initial(actor()),0,1000,1,1,true,b->{calls.incrementAndGet();return 99;});
        assertEquals(3,calls.get());assertEquals(3,hit.triggers().size());assertEquals(1,hit.triggers().stream().filter(Trigger::appliedProtection).count());
        assertEquals(3*SECOND,hit.after().untilNanos());
    }
    @Test void candidateRejectsReplayHpRaceGearChangeWorldChangeAndNewLifeWithoutRedrawing() {
        var sel=selection();var before=initial(actor());var hit=prepareHurt(sel,before,0,1000,1,1,true,b->0);
        for(var changed:List.of(new Selection(actor(),1,sel.rules()),new Selection(new Actor(new UUID(0,2),actor().owner(),1,0),0,sel.rules()),
                new Selection(new Actor(actor().match(),actor().owner(),1,1),0,sel.rules()),new Selection(actor(),0,List.of()))) {
            assertEquals(Failure.STALE_CANDIDATE,assertThrows(ProcException.class,()->accept(hit,changed,before,1000)).failure());
        }
        assertThrows(ProcException.class,()->accept(hit,sel,hit.after(),999));assertThrows(ProcException.class,()->accept(hit,sel,before,999));
        assertEquals(before,initial(actor()));
    }
    @Test void unequippingStopsNewTriggersButDoesNotEraseAnAlreadyAppliedSkill() {
        var active=prepareHurt(selection(),initial(actor()),0,1000,1,1,true,b->0).after();
        var empty=new Selection(actor(),1,List.of());
        var hit=prepareHurt(empty,active,SECOND,999,1,1,true,b->{fail();return 0;});
        assertEquals(3*SECOND,hit.after().untilNanos());assertEquals(50,protectedDamage(hit.after(),2*SECOND,100));
    }
    @Test void unknownSiblingEffectAndAttackProcRemainUnsupportedRatherThanBeingDropped() {
        var known=selection().rules().get(0);
        var attack=new Equipped(1004,ModernArmorProcsTest.rule(51,88890,-1,0,10,0));
        assertEquals(Failure.UNSUPPORTED_EFFECT,assertThrows(ProcException.class,()->new Selection(actor(),0,List.of(known,attack))).failure());
        var other=new ModernArmorProcs.Rule(51,88890,-1,0,0,10,0,0,0,0,0,0,0,67,3,1,11101,1);
        assertThrows(ProcException.class,()->new Selection(actor(),0,List.of(new Equipped(1004,other))));
    }
    @Test void invalidRngAndTimeNeverMutateOldStateAndWideHpIsRetained() {
        var sel=selection();var before=initial(actor());
        for(long draw:new long[]{-1,100})assertThrows(IllegalArgumentException.class,()->prepareHurt(sel,before,0,1000,1,1,true,b->draw));
        assertThrows(IllegalArgumentException.class,()->prepareHurt(sel,before,-1,1000,1,1,true,b->0));
        assertThrows(IllegalArgumentException.class,()->prepareHurt(sel,before,MAX_TIME+1,1000,1,1,true,b->0));
        assertThrows(IllegalArgumentException.class,()->prepareHurt(sel,before,0,1000,1001,1001,true,b->0));
        assertThrows(IllegalArgumentException.class,()->prepareHurt(sel,before,0,1000,-1,0,true,b->0));
        assertEquals(Long.MAX_VALUE-1,prepareHurt(sel,before,MAX_TIME,Long.MAX_VALUE,1,1,true,b->0).hpAfter());
        assertEquals(initial(actor()),before);
    }
}
