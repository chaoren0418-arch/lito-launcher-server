package local.lito381.server;

import static org.junit.jupiter.api.Assertions.*;
import static local.lito381.server.ModernNpcCatalog.Field.*;
import static local.lito381.server.ModernNpcRosterTest.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

/** Calculator and victim-side atomic batch only, not a client/wire claim. */
class ModernNpcFireStormTest {
    static ModernNpcFireStorm.Incoming plan(ModernNpcRoster r,ModernNpcCatalog.Catalog c,List<ModernNpcRoster.Actor> targets) {
        return ModernNpcFireStorm.prepareIncoming(ModernNpcDisintegrateTest.caster(),c,r.snapshot(),cell(10,10),0,targets,b->0);
    }
    static List<ModernNpcRoster.Actor> spawn(ModernNpcRoster r,ModernNpcCatalog.Catalog c) {
        var t=c.npcs().get(ModernNpcMeleeTest.NPC).rosterTemplate();
        return r.spawn(SCOPE,List.of(new ModernNpcRoster.Spawn(t,0,1,cell(11,10),2),
                new ModernNpcRoster.Spawn(t,0,1,cell(10,11),2)),Set.of(cell(10,10)));
    }
    static long mrMultiplier(long mr) {
        long[] maximum={0,50,100,120,140,160,180,200,220,240,Long.MAX_VALUE};
        int[] divisor={1,2,3,4,5,6,7,8,9,10,11},percent={100,100,90,90,80,80,70,70,60,60,50};
        int i=0;while(mr>maximum[i])i++;return Math.max(0,percent[i]-mr/divisor[i]);
    }
    @Test void battleNpcDiceMrAndIgnoredCriticalUseNoPlayerArmorOrWeakAttribute() {
        var caster=ModernFireStorm.qualify(ModernNpcDisintegrateTest.caster());
        for(long mr:new long[]{0,1,50,51,100,101,120,121,140,141,160,161,180,181,200,201,220,221,240,241,550,Long.MAX_VALUE})
            for(int die:new int[]{0,7,14})for(int ignoredCritical:new int[]{0,99}) {
                var c=ModernNpcMeleeTest.catalog(Map.of(HP,10000L,MR,mr),true,0);var bounds=new ArrayList<Long>();
                var out=ModernNpcFireStorm.againstNpc(caster,c,ModernNpcMeleeTest.actor(c),b->{bounds.add(b);return bounds.size()<=8?die:ignoredCritical;});
                long expected=(24+8*(die+1))*35/32*mrMultiplier(mr)/100;
                assertEquals(expected,out.rawDamage());assertEquals(10000-expected,out.remainingHp());
                assertEquals(List.of(15L,15L,15L,15L,15L,15L,15L,15L,100L),bounds);
            }
    }
    @Test void wholeBatchHasIndependentRollsOneRevisionAndImmutableProof()throws Exception {
        var c=ModernNpcMeleeTest.catalog(Map.of(HP,1000L,MR,0L),true,0);
        try(var r=roster()) {
            var targets=spawn(r,c);var before=r.snapshot();var batch=plan(r,c,List.of(targets.get(1),targets.get(0)));
            assertEquals(before,r.snapshot());assertEquals(targets,batch.hits().stream().map(ModernNpcFireStorm.Hit::before).toList());
            var pc=ModernNpcDisintegrateTest.caster();assertEquals(pc.key(),batch.attacker());
            assertEquals(pc.profile().equipmentRevision(),batch.equipmentRevision());assertEquals(48,batch.mpCost());
            assertEquals("NpcFireStormIncoming[redacted]",batch.toString());
            assertThrows(UnsupportedOperationException.class,()->batch.hits().clear());
            var after=r.commitIncoming(batch);assertEquals(List.of(965L,965L),after.stream().map(ModernNpcRoster.Actor::hp).toList());
            assertEquals(before.revision()+1,r.snapshot().revision());
            assertTrue(after.stream().allMatch(a->a.action()==ModernNpcRoster.Action.HURT&&a.changedRevision()==r.snapshot().revision()));
            rejectedUnchanged(r,()->r.commitIncoming(batch));
        }
    }
    @Test void invalidLastTargetAndLateInvalidRollCannotPartiallyDamageFirstTarget()throws Exception {
        var c=ModernNpcMeleeTest.catalog(Map.of(HP,1000L,MR,0L),true,0);
        try(var r=roster()) {
            var targets=spawn(r,c);var batch=plan(r,c,targets);
            r.damage(SCOPE,targets.get(1).id(),1,1);
            rejectedUnchanged(r,()->r.commitIncoming(batch));assertEquals(1000,r.snapshot().actors().get(0).hp());
            rejectedUnchanged(r,()->batch.revalidated(r.snapshot()));
            var before=r.snapshot();int[] draws={0};
            assertThrows(ModernFireStorm.CombatException.class,()->ModernNpcFireStorm.prepareIncoming(ModernNpcDisintegrateTest.caster(),c,before,
                    cell(10,10),0,before.actors(),b->++draws[0]==18?-1:0));
            assertEquals(18,draws[0]);assertEquals(before,r.snapshot());
        }
    }
    @Test void targetQualificationPrecedesEveryRandomDraw()throws Exception {
        var c=ModernNpcMeleeTest.catalog(Map.of(HP,1000L,MR,0L),true,0);
        try(var r=roster()) {
            var targets=spawn(r,c);var t=targets.get(0).template();
            var far=r.spawn(SCOPE,List.of(new ModernNpcRoster.Spawn(t,0,1,cell(14,10),2)),Set.of()).get(0);
            for(var invalid:List.of(List.of(targets.get(0),targets.get(0)),List.of(targets.get(0),far)))
                rejectedUnchanged(r,()->ModernNpcFireStorm.prepareIncoming(ModernNpcDisintegrateTest.caster(),c,r.snapshot(),cell(10,10),0,invalid,
                        b->{fail("all victims must qualify before RNG");return 0;}));
            rejectedUnchanged(r,()->ModernNpcFireStorm.prepareIncoming(ModernNpcDisintegrateTest.caster(),c,r.snapshot(),cell(10,10),1,targets,b->{fail();return 0;}));
        }
    }
    @Test void emptyAndFullyResistedBatchConsumeRevisionWithoutFakeHurt()throws Exception {
        for(boolean empty:List.of(false,true))try(var r=roster()) {
            var c=ModernNpcMeleeTest.catalog(Map.of(MR,Long.MAX_VALUE),true,0);var targets=spawn(r,c);var before=r.snapshot();
            var batch=plan(r,c,empty?List.of():targets);var changed=r.commitIncoming(batch);
            assertEquals(empty?List.of():targets,changed);assertEquals(before.actors(),r.snapshot().actors());
            assertEquals(before.revision()+1,r.snapshot().revision());rejectedUnchanged(r,()->r.commitIncoming(batch));
        }
    }
    @Test void clockOnlyRevalidationNeverIncludesAnUncapturedNewVictim()throws Exception {
        var c=ModernNpcMeleeTest.catalog(Map.of(HP,1000L,MR,0L),true,0);
        try(var r=roster()) {
            var targets=spawn(r,c);var batch=plan(r,c,targets);r.advance(SCOPE,1);
            var late=r.spawn(SCOPE,List.of(new ModernNpcRoster.Spawn(targets.get(0).template(),0,1,cell(9,10),2)),Set.of()).get(0);
            var fresh=batch.revalidated(r.snapshot());assertEquals(2,r.commitIncoming(fresh).size());
            assertEquals(late,r.snapshot().actors().get(2));rejectedUnchanged(r,()->r.commitIncoming(batch));
        }
    }
    @Test void concurrentDuplicateBatchCommitsEveryVictimExactlyOnce()throws Exception {
        var c=ModernNpcMeleeTest.catalog(Map.of(HP,1000L,MR,0L),true,0);var pool=Executors.newFixedThreadPool(2);
        try(var r=roster()) {
            var batch=plan(r,c,spawn(r,c));var start=new CountDownLatch(1);
            java.util.concurrent.Callable<Boolean> work=()->{start.await();try{r.commitIncoming(batch);return true;}catch(IllegalArgumentException stale){return false;}};
            var a=pool.submit(work);var b=pool.submit(work);start.countDown();assertNotEquals(a.get(2,TimeUnit.SECONDS),b.get(2,TimeUnit.SECONDS));
            assertEquals(List.of(965L,965L),r.snapshot().actors().stream().map(ModernNpcRoster.Actor::hp).toList());
        }finally{pool.shutdownNow();assertTrue(pool.awaitTermination(2,TimeUnit.SECONDS));}
    }
}
