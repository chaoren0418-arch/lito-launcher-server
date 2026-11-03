package local.lito381.server;

import static org.junit.jupiter.api.Assertions.*;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;
import org.junit.jupiter.api.Test;

class ModernSkillSpendingTest {
    @Test void changedCostPreservesActualHistoryAndDoesNotRewriteLastCast() {
        var initial=ModernSkillSpending.Ledger.initial(9,1,70);
        var first=initial.cast(2,1).cast(3,3);
        var changed=first.reprice(10,4,59);
        assertEquals(140,changed.spent());assertEquals(2,changed.casts());
        assertEquals(first.lastCastRevision(),changed.lastCastRevision());assertEquals(3,changed.lastCastSequence());
        var next=changed.cast(5,8);assertEquals(199,next.spent());assertEquals(3,next.casts());
        var up=next.reprice(11,6,70).cast(7,9);assertEquals(269,up.spent());
        assertEquals(140,first.spent());assertTrue(ModernSkillSpending.follows(first,changed));
        assertTrue(ModernSkillSpending.follows(changed,next));assertTrue(ModernSkillSpending.follows(next,next));
        assertFalse(ModernSkillSpending.follows(first,next));
    }
    @Test void repeatedZeroCastAndSameCostEpochsStayBoundedWithoutInventingCasts() {
        var value=ModernSkillSpending.Ledger.initial(0,1,3);
        for(int i=1;i<=1000;i++){var next=value.reprice(i,i+1,3);assertTrue(ModernSkillSpending.follows(value,next));value=next;}
        assertEquals(0,value.spent());assertEquals(0,value.casts());assertEquals(0,value.lastCastSequence());
        assertEquals(3,value.cast(1002,1).spent());
    }
    @Test void staleSkippedForgedAndCombinedEventsCannotRewriteEpochs() {
        var a=ModernSkillSpending.Ledger.initial(0,1,3).cast(2,1);
        assertThrows(IllegalArgumentException.class,()->a.reprice(2,3,1));
        assertThrows(IllegalArgumentException.class,()->a.reprice(1,2,1));
        assertThrows(IllegalArgumentException.class,()->a.cast(3,1));
        assertThrows(IllegalArgumentException.class,()->a.cast(2,2));
        assertThrows(IllegalArgumentException.class,()->a.reprice(1,3,-1));
        var b=a.reprice(1,3,1);
        var forged=new ModernSkillSpending.Ledger(1,3,1,1,4,1,4,2,1);
        assertFalse(ModernSkillSpending.follows(a,forged));assertFalse(ModernSkillSpending.follows(b,forged));
        assertFalse(ModernSkillSpending.follows(a,b.cast(4,2)));
        assertFalse(ModernSkillSpending.follows(null,b));
    }
    @Test void allCountersRemainI64CheckedAndFailureDoesNotMutatePrior() {
        long n=9_007_199_254_740_993L;
        var wide=new ModernSkillSpending.Ledger(1,n+2,1,n,n,n,n,n+1,n);
        assertEquals(n+1,wide.cast(n+3,n+1).spent());
        long half=Long.MAX_VALUE/2+1;
        var full=new ModernSkillSpending.Ledger(Long.MAX_VALUE,half+2,1,half,Long.MAX_VALUE,half,Long.MAX_VALUE,half+1,half);
        assertThrows(ArithmeticException.class,()->full.cast(half+3,half+1));
        assertThrows(ArithmeticException.class,()->full.reprice(0,half+3,1));assertEquals(Long.MAX_VALUE,full.spent());
        assertThrows(IllegalArgumentException.class,()->ModernSkillSpending.advance(-1,1,1));
        assertThrows(IllegalArgumentException.class,()->ModernSkillSpending.advance(0,1,1));
        assertThrows(IllegalArgumentException.class,()->ModernSkillSpending.Ledger.initial(-1,1,1));
        assertThrows(IllegalArgumentException.class,()->ModernSkillSpending.Ledger.initial(0,0,1));
        assertThrows(IllegalArgumentException.class,()->new ModernSkillSpending.Ledger(0,1,2,0,0,1,1,2,1));
        assertThrows(ArithmeticException.class,()->new ModernSkillSpending.Ledger(0,1,2,0,0,Long.MAX_VALUE,Long.MAX_VALUE,Long.MAX_VALUE,Long.MAX_VALUE));
        assertThrows(IllegalArgumentException.class,()->new ModernSkillSpending.Totals(1,Long.MAX_VALUE));
    }
    @Test void legalZeroCostHealingCountsCastsWithoutInventingMpSpendingOrRefunds() {
        var free=ModernSkillSpending.Ledger.initial(0,1,0).cast(2,1);
        assertEquals(1,free.casts());assertEquals(0,free.spent());
        var paid=free.reprice(1,3,4).cast(4,2);
        var freeAgain=paid.reprice(2,5,0).cast(6,3);
        assertEquals(3,freeAgain.casts());assertEquals(4,freeAgain.spent());
        assertTrue(ModernSkillSpending.follows(paid,paid.reprice(2,5,0)));
    }
    @Test void legacyFixedCostPacketsRemainStrictRatherThanAcceptingTheNewLedgerByAccident() {
        assertDoesNotThrow(()->new ModernDamageSpellProtocol.State(1,1,2,3,4,2,0,0,1,0,3));
        assertThrows(IllegalArgumentException.class,()->new ModernDamageSpellProtocol.State(1,2,4,4,4,2,0,0,1,0,1));
        assertDoesNotThrow(()->new ModernFireStormProtocol.State(1,1,2,48,1,0,0,48,ModernFireStorm.REUSE_MILLIS,List.of()));
        assertThrows(IllegalArgumentException.class,()->new ModernFireStormProtocol.State(1,2,4,87,2,0,0,39,ModernFireStorm.REUSE_MILLIS,List.of()));
    }
    @Test void generatedEpochCorpusHasCrossLanguageDeterministicEncoding()throws Exception {
        var digest=MessageDigest.getInstance("SHA-256");long rng=0x123456789abcdef0L;
        var state=ModernSkillSpending.Ledger.initial(0,1,70);
        for(int i=1;i<=512;i++) {
            rng^=rng<<13;rng^=rng>>>7;rng^=rng<<17;
            var next=i%3==0?state.reprice(state.equipmentRevision()+1,i+1,(int)Long.remainderUnsigned(rng,71)):state.cast(i+1,2L*i);
            assertTrue(ModernSkillSpending.follows(state,next));
            for(long n:new long[]{next.equipmentRevision(),next.profileRevision(),next.cost(),next.priorCasts(),next.priorSpent(),next.casts(),next.spent(),next.lastCastRevision(),next.lastCastSequence()})
                digest.update(ByteBuffer.allocate(8).putLong(n).array());
            state=next;
        }
        String hash=HexFormat.of().formatHex(digest.digest());
        assertEquals("c29985c2af1e8427d846dea8a29b97fff80b6782d8d79fc19b5e65e0f355831a",hash);
        System.out.println("skill-spending-v1:"+hash+":512");
    }
}
