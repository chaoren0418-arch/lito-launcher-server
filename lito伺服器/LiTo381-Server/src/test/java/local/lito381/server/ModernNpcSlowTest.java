package local.lito381.server;

import static org.junit.jupiter.api.Assertions.*;
import static local.lito381.server.ModernNpcRosterTest.*;
import static local.lito381.server.ModernNpcHasteTest.current;
import static local.lito381.server.ModernNpcHasteTest.haste;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

/** Actual victim roster commits; caster admission/material/MP remain outside this test. */
class ModernNpcSlowTest {
    static ModernNpcRoster.Actor slow(ModernNpcRoster r, ModernNpcRoster.Actor a) {
        return r.commitIncoming(ModernNpcSlow.plan(r.snapshot(),a.id(),a.life()));
    }
    static void noOldProjection(ModernNpcRoster.Actor a) {
        ModernNpcHasteTest.noOldProjection(a);
        assertThrows(IllegalArgumentException.class, () -> ModernHasteEffects.Motion.from(a));
        var out = ByteBuffer.allocate(100);
        assertThrows(IllegalArgumentException.class, () -> ModernBattleHasteWire.speed(new ModernBattleWire.Writer(out),a.speed()));
        assertEquals(0,out.position());
    }
    @Test void slowRefreshExpiryAndHasteCannotRewriteAnAlreadyCommittedStep() throws Exception {
        try (var r=roster()) {
            var first=ModernNpcMotionTest.moved(r);r.advance(SCOPE,150);var s=slow(r,first);
            assertSame(first.walk(),s.walk());assertSame(first.template(),s.template());assertEquals(200,s.nextMoveMillis());
            assertEquals(new ModernNpcMotion.Pace(200,1500),s.pace());assertEquals(64_150,s.speed().slowUntilMillis());
            rejectedUnchanged(r,()->r.move(SCOPE,plan(r,s,2),Set.of()));
            r.advance(SCOPE,200);var step=r.move(SCOPE,plan(r,s,2),Set.of());
            assertEquals(200,step.walk().durationMillis());assertEquals(400,step.nextMoveMillis());assertSame(s.speed(),step.walkSpeed());
            r.advance(SCOPE,220);var refresh=slow(r,step);
            assertEquals(64_220,refresh.speed().slowUntilMillis());assertSame(step.walk(),refresh.walk());assertEquals(400,refresh.nextMoveMillis());
            r.advance(SCOPE,64_219);assertTrue(current(r).speed().slowed());
            r.advance(SCOPE,64_220);var expired=current(r);
            assertFalse(expired.speed().slowed());assertSame(step.walk(),expired.walk());assertSame(step.walkSpeed(),expired.walkSpeed());
            assertEquals(100,expired.pace().walkMillis());noOldProjection(expired);
            var normal=r.move(SCOPE,plan(r,expired,2),Set.of());assertEquals(100,normal.walk().durationMillis());
            assertEquals(64_320,normal.nextMoveMillis());assertEquals(3,normal.walk().sequence());noOldProjection(normal);
        }
        try (var r=roster()) {
            var s=slow(r,one(r));r.advance(SCOPE,100);var step=r.move(SCOPE,plan(r,s,2),Set.of());
            r.advance(SCOPE,150);var neutral=haste(r,step);
            assertFalse(neutral.speed().hasEffects());assertSame(step.walk(),neutral.walk());assertEquals(300,neutral.nextMoveMillis());
            var fast=haste(r,neutral);assertTrue(fast.speed().hasted());assertEquals(75,fast.pace().walkMillis());
            assertSame(step.walkSpeed(),fast.walkSpeed());assertEquals(300,fast.nextMoveMillis());noOldProjection(fast);
        }
    }
    @Test void nativeHasteSlowNeutralizationIsNotOverwritingAndExactExpiryIsNotLive() throws Exception {
        try (var r=roster()) {
            var a=haste(r,one(r));var neutral=slow(r,a);assertFalse(neutral.speed().hasEffects());
            var s=slow(r,neutral);assertTrue(s.speed().slowed());
            var neutralAgain=haste(r,s);assertFalse(neutralAgain.speed().hasEffects());
            var h=haste(r,neutralAgain);assertTrue(h.speed().hasted());
            assertEquals(s.speed().slowRevision(),h.speed().slowRevision());noOldProjection(h);
        }
        var s=ModernNpcSpeed.slow(ModernNpcSpeed.initial(),0,1);
        assertEquals(1_264_000,ModernNpcSpeed.haste(s,64_000,2).hasteUntilMillis());
        var h=ModernNpcSpeed.haste(ModernNpcSpeed.initial(),0,1);
        assertEquals(1_264_000,ModernNpcSpeed.slow(h,1_200_000,2).slowUntilMillis());
    }
    @Test void slowAttackClockSurvivesRefreshAndCannotPermitAnEarlyHit() throws Exception {
        try (var r=roster()) {
            var a=slow(r,one(r));r.advance(SCOPE,100);var t=target(7,0,11,10);var players=Set.of(t.cell());
            var p=r.pursue(SCOPE,a.id(),1,List.of(t),players,4096,()->false);
            var hit=r.attack(SCOPE,p,t,players);assertEquals(1600,hit.nextAttackMillis());
            r.advance(SCOPE,101);var refresh=slow(r,hit);assertEquals(1600,refresh.nextAttackMillis());
            var early=r.pursue(SCOPE,a.id(),1,List.of(t),players,4096,()->false);
            rejectedUnchanged(r,()->r.attack(SCOPE,early,t,players));
            r.advance(SCOPE,1600);var next=r.pursue(SCOPE,a.id(),1,List.of(t),players,4096,()->false);
            assertEquals(3100,r.attack(SCOPE,next,t,players).nextAttackMillis());
            r.advance(SCOPE,64_101);var ended=r.pursue(SCOPE,a.id(),1,List.of(t),players,4096,()->false);
            assertEquals(64_851,r.attack(SCOPE,ended,t,players).nextAttackMillis());
        }
    }
    @Test void deathRevivalRetirementAndFreshSpawnNeverInheritTheOldLease() throws Exception {
        try (var r=roster()) {
            var a=slow(r,one(r));r.advance(SCOPE,100);var step=r.move(SCOPE,plan(r,a,2),Set.of());
            var old=ModernNpcSlow.plan(r.snapshot(),a.id(),1);
            var dead=r.damage(SCOPE,a.id(),1,Long.MAX_VALUE);assertFalse(dead.speed().hasEffects());noOldProjection(dead);
            assertSame(step.walk(),dead.walk());assertSame(step.walkSpeed(),dead.walkSpeed());
            rejectedUnchanged(r,()->r.commitIncoming(old));rejectedUnchanged(r,()->ModernNpcSlow.plan(r.snapshot(),a.id(),1));
            var revived=r.resurrect(SCOPE,a.id(),1,280,10,Set.of());assertEquals(2,revived.life());
            assertFalse(revived.speed().hasEffects());assertSame(step.walk(),revived.walk());assertEquals(200,revived.nextMoveMillis());
            assertEquals(850,revived.nextAttackMillis());noOldProjection(revived);
            rejectedUnchanged(r,()->r.commitIncoming(old));rejectedUnchanged(r,()->ModernNpcSlow.plan(r.snapshot(),a.id(),1));
            var fresh=slow(r,revived);assertTrue(fresh.speed().slowed());
            r.damage(SCOPE,a.id(),2,Long.MAX_VALUE);r.advance(SCOPE,3100);assertTrue(r.snapshot().actors().isEmpty());
            var newborn=one(r);assertNotEquals(a.id(),newborn.id());assertEquals(ModernNpcSpeed.initial(),newborn.speed());
            assertDoesNotThrow(()->ModernHasteEffects.Motion.from(newborn));
        }
    }
    @Test void staleForeignDuplicateAndClosedPlansRejectWithoutMutation() throws Exception {
        try (var r=roster()) {
            var a=one(r);var p=ModernNpcSlow.plan(r.snapshot(),a.id(),1);r.advance(SCOPE,1);
            rejectedUnchanged(r,()->r.commitIncoming(p));
            rejectedUnchanged(r,()->ModernNpcSlow.plan(r.snapshot(),new ModernNpcRoster.Id(new ModernNpcRoster.Scope(11,22,34),1),1));
            rejectedUnchanged(r,()->ModernNpcSlow.plan(r.snapshot(),new ModernNpcRoster.Id(SCOPE,99),1));
            rejectedUnchanged(r,()->ModernNpcSlow.plan(r.snapshot(),a.id(),2));
            var beforeHurt=ModernNpcSlow.plan(r.snapshot(),a.id(),1);r.damage(SCOPE,a.id(),1,1);
            rejectedUnchanged(r,()->r.commitIncoming(beforeHurt));
            var valid=ModernNpcSlow.plan(r.snapshot(),a.id(),1);r.commitIncoming(valid);
            rejectedUnchanged(r,()->r.commitIncoming(valid));
            var beforeMove=ModernNpcSlow.plan(r.snapshot(),a.id(),1);r.advance(SCOPE,100);r.move(SCOPE,plan(r,current(r),2),Set.of());
            rejectedUnchanged(r,()->r.commitIncoming(beforeMove));r.close();
            rejectedUnchanged(r,()->r.commitIncoming(valid));rejectedUnchanged(r,()->ModernNpcSlow.plan(r.snapshot(),a.id(),1));
        }
    }
    @Test void concurrentSameRevisionHasExactlyOneWinner() throws Exception {
        try (var r=roster()) {
            var a=one(r);var before=r.snapshot();var plan=ModernNpcSlow.plan(before,a.id(),1);
            var go=new CountDownLatch(1);var pool=Executors.newFixedThreadPool(2);
            try {
                java.util.concurrent.Callable<Boolean> call=()->{go.await();try{r.commitIncoming(plan);return true;}catch(IllegalArgumentException e){return false;}};
                var first=pool.submit(call);var second=pool.submit(call);go.countDown();
                assertNotEquals(first.get(5,TimeUnit.SECONDS),second.get(5,TimeUnit.SECONDS));
                assertEquals(before.revision()+1,r.snapshot().revision());assertEquals(64_000,current(r).speed().slowUntilMillis());
            } finally {go.countDown();pool.shutdown();assertTrue(pool.awaitTermination(5,TimeUnit.SECONDS));}
        }
    }
    @Test void ordinaryDamageHealAndManaPreserveSlowButAoeDeathClearsIt() throws Exception {
        try (var r=roster()) {
            var a=slow(r,one(r));assertSame(a.speed(),r.damage(SCOPE,a.id(),1,1).speed());
            assertSame(a.speed(),r.heal(SCOPE,a.id(),1,1).speed());assertSame(a.speed(),r.drainMana(SCOPE,a.id(),1,1).speed());
        }
        for (long hp:new long[]{1,1000}) try (var r=roster()) {
            var catalog=ModernNpcMeleeTest.catalog(Map.of(ModernNpcCatalog.Field.HP,hp,ModernNpcCatalog.Field.MR,0L),true,0);
            var rows=ModernNpcFireStormTest.spawn(r,catalog);for(var a:rows)slow(r,a);
            var before=r.snapshot().actors();var after=r.commitIncoming(ModernNpcFireStormTest.plan(r,catalog,before));
            for(int i=0;i<after.size();i++){
                assertEquals(hp>1,after.get(i).speed().slowed());
                assertEquals(before.get(i).speed().slowRevision(),after.get(i).speed().slowRevision());
                if(hp>1)assertSame(before.get(i).speed(),after.get(i).speed());
                else assertEquals(r.snapshot().revision(),after.get(i).speed().revision());
            }
        }
    }
    @Test void mixedSpeedTerminalClearIsScopedIdempotentAndPreservesStepAndClocks() throws Exception {
        try(var r=roster()) {
            var a=slow(r,one(r));var other=r.spawn(SCOPE,List.of(spawn(12,10)),Set.of()).get(0);haste(r,other);
            r.advance(SCOPE,100);var step=r.move(SCOPE,plan(r,a,4),Set.of());
            var before=r.snapshot();rejectedUnchanged(r,()->r.clearHaste(new ModernNpcRoster.Scope(11,22,34)));
            r.clearHaste(SCOPE);var after=r.snapshot();
            assertEquals(before.revision()+1,after.revision());assertTrue(after.actors().stream().noneMatch(x->x.speed().hasEffects()));
            var cleared=current(r);assertSame(step.walk(),cleared.walk());assertSame(step.walkSpeed(),cleared.walkSpeed());
            assertEquals(step.nextMoveMillis(),cleared.nextMoveMillis());assertEquals(step.hp(),cleared.hp());noOldProjection(cleared);
            r.clearHaste(SCOPE);assertEquals(after,r.snapshot());assertTrue(before.actors().get(0).speed().slowed());
        }
    }
    @Test void terminalSingleDamageAndAttackClearSlowInTheSameRevision() throws Exception {
        for(long amount:new long[]{0,1,Long.MAX_VALUE})try(var r=roster()){
            var a=slow(r,one(r));var other=r.spawn(SCOPE,List.of(spawn(12,10)),Set.of()).get(0);haste(r,other);
            rejectedUnchanged(r,()->r.damage(SCOPE,a.id(),2,amount,true));
            var before=r.snapshot();var hit=r.damage(SCOPE,a.id(),1,amount,true);var after=r.snapshot();
            assertEquals(before.revision()+1,after.revision());assertSame(after.actors().get(0),hit);
            assertTrue(after.actors().stream().noneMatch(x->x.speed().hasEffects()));assertEquals(other.hp(),after.actors().get(1).hp());
        }
        try(var r=roster()){
            var a=slow(r,one(r));r.advance(SCOPE,100);var t=target(7,0,11,10);var players=Set.of(t.cell());
            var stale=r.pursue(SCOPE,a.id(),1,List.of(t),players,4096,()->false);r.damage(SCOPE,a.id(),1,1);
            rejectedUnchanged(r,()->r.attack(SCOPE,stale,t,players,true));var before=r.snapshot();
            var p=r.pursue(SCOPE,a.id(),1,List.of(t),players,4096,()->false);var hit=r.attack(SCOPE,p,t,players,true);
            assertEquals(before.revision()+1,r.snapshot().revision());assertFalse(hit.speed().hasEffects());assertEquals(1600,hit.nextAttackMillis());
        }
    }
    @Test void terminalAoeClearsOtherLaneSlowInOneCasAndRetainsImmutableBefore() throws Exception {
        try(var r=roster()){
            var c=ModernNpcMeleeTest.catalog(Map.of(ModernNpcCatalog.Field.HP,1000L,ModernNpcCatalog.Field.MR,0L),true,0);
            var victims=ModernNpcFireStormTest.spawn(r,c);
            var other=r.spawn(SCOPE,List.of(new ModernNpcRoster.Spawn(victims.get(0).template(),1,1,cell(20,10),2)),Set.of()).get(0);
            for(var a:r.snapshot().actors())slow(r,a);
            var stale=ModernNpcFireStormTest.plan(r,c,r.snapshot().actors().subList(0,2));
            r.damage(SCOPE,victims.get(1).id(),1,1);rejectedUnchanged(r,()->r.commitIncoming(stale,true));
            var before=r.snapshot();var batch=ModernNpcFireStormTest.plan(r,c,before.actors().subList(0,2));
            var changed=r.commitIncoming(batch,true);var after=r.snapshot();
            assertEquals(before.revision()+1,after.revision());assertEquals(after.actors().subList(0,2),changed);
            assertTrue(after.actors().stream().noneMatch(a->a.speed().hasEffects()));
            assertTrue(after.actors().stream().allMatch(a->a.speed().revision()==after.revision()));
            assertEquals(List.of(965L,964L),changed.stream().map(ModernNpcRoster.Actor::hp).toList());
            assertEquals(other.hp(),after.actors().get(2).hp());assertTrue(before.actors().stream().allMatch(a->a.speed().slowed()));
            rejectedUnchanged(r,()->r.commitIncoming(batch,true));
        }
    }
    @Test void allNativeBaseIntervalsCanDoubleAndFrozenProjectionsKeepSixtySecondBound() throws Exception {
        var s=ModernNpcSpeed.slow(ModernNpcSpeed.initial(),0,1);
        for(int base=1;base<=60_000;base++){
            var pace=new ModernNpcMotion.Pace(base,60_001-base);var actual=ModernNpcSpeed.pace(pace,s);
            assertEquals(base*2,actual.walkMillis());assertEquals((60_001-base)*2,actual.attackMillis());
            if(base>=40)assertEquals(BattleActionInterval.npcSourceMillis(base,false,true),actual.walkMillis());
            assertSame(pace,ModernNpcSpeed.pace(pace,ModernNpcSpeed.expire(s,64_000,2)));
        }
        for(int wide:new int[]{60_001,120_000}){
            var base=new ModernNpcMotion.Pace(wide,wide);var id=new ModernNpcRoster.Id(SCOPE,1);
            assertThrows(IllegalArgumentException.class,()->ModernNpcSpeed.pace(base,ModernNpcSpeed.initial()));
            assertThrows(IllegalArgumentException.class,()->new ModernNpcMotion.View(id,1,base,null));
            assertThrows(IllegalArgumentException.class,()->new ModernHasteEffects.Motion(id,1,base,ModernNpcSpeed.initial(),ModernNpcSpeed.initial(),null));
            assertThrows(IllegalArgumentException.class,()->new ModernNpcRoster.Template(7,"catalog",45262,1649,280,10,wide,wide));
        }
        try(var r=roster()){
            var base=new ModernNpcRoster.Template(7,"catalog",45262,1649,280,10,60_000,60_000);
            var a=r.spawn(SCOPE,List.of(new ModernNpcRoster.Spawn(base,0,1,cell(10,10),2)),Set.of()).get(0);
            long now=ModernNpcRoster.MAX_ELAPSED_MILLIS;r.advance(SCOPE,now);a=slow(r,a);
            var step=r.move(SCOPE,plan(r,a,2),Set.of());assertEquals(120_000,step.walk().durationMillis());
            assertEquals(now+120_000,step.nextMoveMillis());assertSame(base,step.template());noOldProjection(step);
            var t=target(7,0,12,10);var players=Set.of(t.cell());
            var p=r.pursue(SCOPE,a.id(),1,List.of(t),players,4096,()->false);
            rejectedUnchanged(r,()->r.attack(SCOPE,p,t,players)); // This walk is still in flight.
        }
        try(var r=roster()){
            var base=new ModernNpcRoster.Template(7,"catalog",45262,1649,280,10,60_000,60_000);
            var a=r.spawn(SCOPE,List.of(new ModernNpcRoster.Spawn(base,0,1,cell(10,10),2)),Set.of()).get(0);
            long now=ModernNpcRoster.MAX_ELAPSED_MILLIS;r.advance(SCOPE,now);a=slow(r,a);
            var t=target(7,0,11,10);var players=Set.of(t.cell());
            var p=r.pursue(SCOPE,a.id(),1,List.of(t),players,4096,()->false);var hit=r.attack(SCOPE,p,t,players);
            assertEquals(now+120_000,hit.nextAttackMillis());assertSame(base,hit.template());noOldProjection(hit);
        }
    }
    @Test void invalidStateAndExactExpiryRejectWhileIdleReadsDoNotMutate() throws Exception {
        var s=ModernNpcSpeed.slow(ModernNpcSpeed.initial(),100,5);
        assertThrows(IllegalArgumentException.class,()->new ModernNpcSpeed.State(1,0,1,2,1));
        assertThrows(IllegalArgumentException.class,()->new ModernNpcSpeed.State(1,0,0,64_000,0));
        assertThrows(IllegalArgumentException.class,()->new ModernNpcSpeed.State(1,0,0,64_001,1));
        assertThrows(IllegalArgumentException.class,()->new ModernNpcSpeed.State(1,0,0,0,2));
        assertThrows(IllegalArgumentException.class,()->s.at(64_100));
        assertThrows(IllegalArgumentException.class,()->ModernNpcSpeed.slow(s,99,6));
        assertThrows(IllegalArgumentException.class,()->ModernNpcSpeed.slow(s,100,5));
        assertThrows(IllegalArgumentException.class,()->ModernNpcSpeed.slow(s,ModernNpcRoster.MAX_ELAPSED_MILLIS+1,6));
        assertSame(s,ModernNpcSpeed.expire(s,64_099,6));
        var ended=ModernNpcSpeed.expire(s,64_100,6);assertFalse(ended.hasEffects());assertEquals(5,ended.slowRevision());
        assertSame(ended,ModernNpcSpeed.clear(ended,64_101,7));
        try(var r=roster()){
            var a=slow(r,one(r));var old=r.snapshot();r.advance(SCOPE,63_999);assertSame(a,current(r));
            r.advance(SCOPE,64_000);var after=r.snapshot();assertFalse(current(r).speed().slowed());noOldProjection(current(r));
            assertEquals(after,r.advance(SCOPE,64_000));assertTrue(old.actors().get(0).speed().slowed());
            assertThrows(IllegalArgumentException.class,()->new ModernNpcRoster.Snapshot(SCOPE,old.manifest(),after.revision(),64_000,old.spawned(),false,old.actors()));
        }
    }
}
