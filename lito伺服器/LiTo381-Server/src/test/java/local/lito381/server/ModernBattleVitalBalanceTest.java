package local.lito381.server;

import static org.junit.jupiter.api.Assertions.*;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Actual engine commits, not a fabricated dynamic-equipment wire capability. */
class ModernBattleVitalBalanceTest {
    static ModernVitalBalance.State read(ModernBattleEngine e,ModernRooms.FrozenPreparation t,ModernBattleEngineBotTest.Player p) {
        return e.equipmentResources(p.owner(),p.scope(),t.matchId(),t.generation()).balance();
    }
    static Object actor(ModernBattleEngine e,int index)throws Exception {
        var f=ModernBattleEngine.class.getDeclaredField("actors");f.setAccessible(true);return ((List<?>)f.get(e)).get(index);
    }
    static ModernVitalBalance.State raw(ModernBattleEngine e,int index)throws Exception {
        var a=actor(e,index);var f=a.getClass().getDeclaredField("equipmentResources");f.setAccessible(true);return (ModernVitalBalance.State)f.get(a);
    }
    static void set(ModernBattleEngine e,int index,ModernVitalBalance.State value)throws Exception {
        var a=actor(e,index);var f=a.getClass().getDeclaredField("equipmentResources");f.setAccessible(true);f.set(a,value);
    }
    static void matches(ModernVitalBalance.State b,ModernBattleEngine.Snapshot s,int i) {
        var v=s.vitals().get(i);assertEquals(new ModernEquipmentVitals.Vitals(v.hp(),v.maxHp(),v.mp(),v.maxMp()),b.vitals());
        assertTrue(b.revision()<=s.revision());
    }
    @Test void actualHealingArrowAndTeleportChargeIndependentHistoriesIntoOneBalance()throws Exception {
        try(var f=new ModernBattleEngineHonorTest.Fixture()) {
            f.active();long spent=0;
            int[] skills={1,4,5};long[] targets={3,4,0};
            for(int i=0;i<skills.length;i++) {
                f.elapsed(i*2_000_000_000L);var result=f.cast(0,i+1,skills[i],targets[i]);assertEquals(ModernSpellProtocol.Reason.NONE,result.reason());
                var b=read(f.engine,f.ticket,f.players.get(0));matches(b,result.snapshot(),0);
                spent+=i==0?4:i==1?3:10;assertEquals(spent,b.flows().spent());assertEquals(0,b.flows().damaged());
                assertEquals(result.snapshot().revision(),b.revision());assertEquals(100,b.birthMp());
            }
        }
    }
    @Test void npcArrowFireAndDisintegrateAllChargeBeforeRosterCommit()throws Exception {
        try(var f=ModernBattleEngineNpcFireStormTest.fixture(1000,10000,0,true,true,false,0)) {
            f.active();var n=f.spawn(0,11,10);
            assertEquals(ModernNpcMagic.Reason.NONE,ModernBattleEngineNpcMagicTest.cast(f,0,1,n).reason());
            f.elapsed(1200);assertEquals(ModernNpcFireStormState.Reason.NONE,ModernBattleEngineNpcFireStormTest.cast(f,2).reason());
            f.elapsed(2400);assertEquals(ModernNpcDisintegrateState.Reason.NONE,ModernBattleEngineNpcDisintegrateTest.cast(f,0,3,n).reason());
            var b=read(f.engine,f.ticket,f.players.get(0));var s=f.engine.tick();matches(b,s,0);
            long spent=ModernBattleUseHistoryTest.read(f.engine,f.ticket,f.players.get(0)).spells().values().stream().mapToLong(h->h.current().spent()).sum();
            assertEquals(spent,b.flows().spent());assertEquals(3,ModernBattleUseHistoryTest.read(f.engine,f.ticket,f.players.get(0)).spells().values().stream().mapToLong(h->h.current().casts()).sum());
        }
    }
    @Test void aoeUsesOneRevisionForCasterHumanAndTypedBotAndDenialsDoNotChangeBalances()throws Exception {
        try(var f=ModernBattleEngineFireStormTest.fixture(false,2,1000,40)) {
            f.active();var reply=f.cast(0,1,70,0);assertEquals(ModernSpellProtocol.Reason.NONE,reply.reason());
            var effects=ModernBattleTargetHistoryTest.history(f.engine,f.ticket,f.players.get(0),ModernUseHistory.Lane.FIRE_STORM).last();
            for(var hit:effects.targets()) {
                int i=Math.toIntExact(((ModernTargetHistory.Participant)hit.before().target()).entityId()-1);
                var b=raw(f.engine,i);matches(b,reply.snapshot(),i);assertEquals(hit.applied(),b.flows().damaged());assertEquals(reply.snapshot().revision(),b.revision());
            }
            var caster=raw(f.engine,0);assertEquals(effects.use().cost(),caster.flows().spent());assertEquals(0,caster.flows().damaged());
            f.cast(0,2,70,0);assertEquals(caster,raw(f.engine,0));
        }
    }
    @Test void disintegrateKillChargesAndDamagesTogetherWithoutInventingRespawn()throws Exception {
        try(var f=ModernBattleEngineDisintegrateTest.fixture(false,0,100,300,500,false)) {
            f.active();f.randomOverride=b->b==100?99:0;var reply=ModernBattleEngineDisintegrateTest.cast(f,1,2);
            assertEquals(ModernDisintegrateState.Reason.NONE,reply.reason());var caster=read(f.engine,f.ticket,f.players.get(0));var victim=read(f.engine,f.ticket,f.players.get(1));
            assertEquals(reply.snapshot().disintegrations().get(0).mpSpent(),caster.flows().spent());assertEquals(100,victim.flows().damaged());
            assertEquals(0,victim.vitals().hp());assertEquals(0,victim.life());assertEquals(0,victim.flows().hpGranted());matches(victim,reply.snapshot(),1);
        }
    }
    @Test void potionAndNaturalRecoveryKeepSeparateAppliedFlowsAndOriginalBirth()throws Exception {
        try(var f=new ModernBattleEngineRegenTest.Fixture()) {
            f.active();for(int n=0;n<3;n++){f.elapsed(n*1_200_000_000L);assertEquals(ModernSpellProtocol.Reason.NONE,f.cast(1,n+1,4).reason());}
            assertEquals(ModernItemProtocol.Reason.NONE,f.heal(0).reason());f.elapsed(16_000_000_000L);var s=f.engine.tick();
            for(int i=0;i<2;i++) {
                var b=read(f.engine,f.ticket,f.room.players.get(i));matches(b,s,i);var old=s.regen().get(i);
                assertEquals(old.damageLoss(),b.flows().damaged());assertEquals(old.itemGain(),b.flows().restored());
                assertEquals(old.hpGain(),b.flows().hpGain());assertEquals(old.mpGain(),b.flows().mpGain());assertEquals(1000,b.birthHp());
            }
            assertTrue(raw(f.engine,0).flows().restored()>0);assertTrue(raw(f.engine,0).flows().hpGain()>0);
        }
    }
    @Test void negativeNaturalRegenIsNotDamageAndNeverChangesLife()throws Exception {
        try(var f=new ModernBattleEngineRegenTest.Fixture(-4,-4,225,100,0,1000,false)) {
            f.active();f.cast(0,1,4);f.elapsed(1_000_000_000_000L);var s=f.engine.tick();
            var a=read(f.engine,f.ticket,f.room.players.get(0));var b=read(f.engine,f.ticket,f.room.players.get(1));matches(a,s,0);matches(b,s,1);
            assertEquals(27,a.flows().mpLoss());assertEquals(3,a.flows().spent());assertEquals(1,b.vitals().hp());
            assertTrue(b.flows().hpLoss()>0);assertEquals(s.regen().get(1).damageLoss(),b.flows().damaged());assertEquals(0,b.life());
        }
    }
    @Test void honorRespawnGrantsActualHpButRetainsSpentMpAndPriorDamage()throws Exception {
        try(var f=new ModernBattleEngineHonorTest.Fixture(100,1,100,1_000_000_000L)) {
            f.active();f.cast(1,1,1,4);f.cast(0,1,4,2);var dead=read(f.engine,f.ticket,f.players.get(1));
            assertEquals(0,dead.vitals().hp());assertEquals(96,dead.vitals().mp());f.elapsed(5_000_000_000L);var s=f.engine.tick();
            var alive=read(f.engine,f.ticket,f.players.get(1));matches(alive,s,1);assertEquals(1,alive.life());assertEquals(1,alive.flows().hpGranted());
            assertEquals(dead.flows().damaged(),alive.flows().damaged());assertEquals(4,alive.flows().spent());assertEquals(0,alive.flows().mpGranted());
        }
    }
    @Test void siegeRespawnKeepsResourceLifeAndCampTransitionTogether()throws Exception {
        try(var f=new ModernBattleEngineSiegeTest.Fixture(0,1,120000,1234,0,false)) {
            f.active();f.walk(1,ModernBattleEngineSiegeTest.cell(31,11));f.waitMillis(200);f.portal(1);f.walk(1,ModernBattleEngineSiegeTest.cell(11,10));f.waitMillis(200);
            f.attack(0,1,2);var dead=read(f.engine,f.ticket,f.players.get(1));assertEquals(0,dead.vitals().hp());
            f.waitMillis(1234);var s=f.tick();var alive=read(f.engine,f.ticket,f.players.get(1));matches(alive,s,1);
            assertEquals(1,alive.life());assertEquals(1,alive.flows().hpGranted());assertEquals(dead.vitals().mp(),alive.vitals().mp());assertEquals(0,alive.flows().mpGranted());
        }
    }
    @Test void questNpcDeathAndExplicitRespawnKeepEveryIncarnationWithoutMpRefill()throws Exception {
        try(var f=ModernQuestLifeTest.fixture(2)) {
            f.active();for(int i=0;i<2;i++) {
                var dead=ModernQuestLifeTest.die(f,100+i*1000);var before=read(f.engine,f.ticket,f.players.get(0));assertEquals(i,before.life());assertEquals(i+1,before.flows().damaged());
                assertEquals(ModernQuestLife.Reason.REVIVED,ModernQuestLifeTest.confirm(f,0,ModernQuestLifeTest.life(dead,0)).reason());
                var after=read(f.engine,f.ticket,f.players.get(0));assertEquals(i+1,after.life());assertEquals(i+1,after.flows().hpGranted());
                assertEquals(before.vitals().mp(),after.vitals().mp());assertEquals(0,after.flows().mpGranted());
            }
        }
    }
    @Test void lastAoeVictimCounterOverflowPublishesNoOtherVictimOrCasterState()throws Exception {
        try(var f=ModernBattleEngineFireStormTest.fixture(false,2,1000,1000)) {
            var active=f.active();int last=3;assertNotEquals(active.actors().get(0).member().team(),active.actors().get(last).member().team());
            var old=raw(f.engine,last);var z=old.flows();
            set(f.engine,last,new ModernVitalBalance.State(old.revision(),old.equipmentRevision(),old.life(),old.birthHp(),old.birthMp(),old.vitals(),
                    new ModernVitalBalance.Flows(Long.MAX_VALUE,0,z.restored(),Long.MAX_VALUE,0,0,0,0,0,0,0)));
            var before=f.engine.tick();var caster=raw(f.engine,0);var victim=raw(f.engine,1);var finalVictim=raw(f.engine,last);
            assertEquals(ModernSpellProtocol.Reason.NUMERIC,f.cast(0,1,70,0).reason());assertEquals(before.vitals(),f.engine.tick().vitals());
            assertEquals(caster,raw(f.engine,0));assertEquals(victim,raw(f.engine,1));assertEquals(finalVictim,raw(f.engine,last));
            assertNull(ModernBattleTargetHistoryTest.history(f.engine,f.ticket,f.players.get(0),ModernUseHistory.Lane.FIRE_STORM));
        }
    }
    @Test void npcSpendingOverflowCannotDamageRosterOrConsumeCast()throws Exception {
        try(var f=ModernBattleEngineNpcFireStormTest.fixture(1000,10000,0,true,true,false,0)) {
            f.active();var n=f.spawn(0,11,10);var old=raw(f.engine,0);
            set(f.engine,0,new ModernVitalBalance.State(old.revision(),old.equipmentRevision(),0,old.birthHp(),old.birthMp(),old.vitals(),
                    new ModernVitalBalance.Flows(0,0,0,0,0,Long.MAX_VALUE,0,Long.MAX_VALUE,0,0,0)));
            var before=f.view();var balance=raw(f.engine,0);assertEquals(ModernNpcMagic.Reason.NUMERIC,ModernBattleEngineNpcMagicTest.cast(f,0,1,n).reason());
            assertEquals(before.npcs(),f.view().npcs());assertEquals(balance,raw(f.engine,0));assertEquals(0,ModernBattleUseHistoryTest.read(f.engine,f.ticket,f.players.get(0)).spells().get(ModernUseHistory.Lane.NPC_LIGHT_ARROW).current().casts());
        }
    }
    @Test void secondActorRegenOverflowAbortsWithoutPublishingFirstRecovery()throws Exception {
        try(var f=new ModernBattleEngineRegenTest.Fixture()) {
            f.active();f.attack(0,1);f.attack(1,1);var old=raw(f.engine,1);var z=old.flows();
            set(f.engine,1,new ModernVitalBalance.State(old.revision(),old.equipmentRevision(),0,old.birthHp(),old.birthMp(),old.vitals(),
                    new ModernVitalBalance.Flows(Long.MAX_VALUE,Long.MAX_VALUE,z.restored(),z.damaged(),0,0,0,0,0,0,0)));
            var before=f.engine.tick();var a=raw(f.engine,0);var b=raw(f.engine,1);f.elapsed(3_000_000_000L);var after=f.engine.tick();
            assertEquals(ModernBattleEngine.AbortReason.SERVER_ABORT,after.abortReason());assertEquals(before.vitals(),after.vitals());
            assertEquals(a,raw(f.engine,0));assertEquals(b,raw(f.engine,1));assertEquals(before.regen(),after.regen());
        }
    }
    @Test void resourceObservationDoesNotAuthorizeAdvancedEquipmentOrForeignOwners()throws Exception {
        try(var f=new ModernBattleEngineHonorTest.Fixture()) {
            f.active();var p=f.players.get(0);var q=f.players.get(1);assertNotNull(read(f.engine,f.ticket,p));
            assertThrows(IllegalStateException.class,()->f.engine.beginEquipmentChange(p.owner(),p.scope(),f.ticket.matchId(),f.ticket.generation()));
            assertThrows(SecurityException.class,()->f.engine.equipmentResources(q.owner(),p.scope(),f.ticket.matchId(),f.ticket.generation()));
            assertThrows(SecurityException.class,()->f.engine.equipmentResources(p.owner(),p.scope(),f.ticket.matchId(),f.ticket.generation()+1));
        }
    }
    @Test void reentrantRegenAndPotionAreNotOverwrittenByAnOldBatchOrItsClocks()throws Exception {
        try(var f=new ModernBattleEngineRegenTest.Fixture()) {
            f.active();f.attack(0,1);f.attack(1,1);f.elapsed(3_000_000_000L);
            var committed=new java.util.concurrent.atomic.AtomicReference<ModernBattleEngine.Snapshot>();
            f.duringRandom=()->{var r=f.heal(0);assertEquals(ModernItemProtocol.Reason.NONE,r.reason());committed.set(r.snapshot());};
            var result=f.engine.tick();assertNotNull(committed.get());assertEquals(committed.get(),result);assertEquals(result,f.engine.tick());
            var balance=read(f.engine,f.ticket,f.room.players.get(0));matches(balance,result,0);
            assertEquals(result.regen().get(0).itemGain(),balance.flows().restored());assertEquals(result.regen().get(0).hpGain(),balance.flows().hpGain());
        }
    }
}
