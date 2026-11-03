package local.lito381.server;

import static org.junit.jupiter.api.Assertions.*;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Canonical ENGINE-revision proof. Real life transitions first, then forged
 * cross-field inputs; no legacy frame construction is needed to reject them. */
class ModernBattleLifeProofTest {
    static ModernBattleObservation.Capture honor(ModernBattleObservation.Capture c,List<ModernHonor.Life> lives) {
        var m=c.modes();return c.withModes(new ModernBattleObservation.Modes(m.isolatedForces(),
                new ModernBattleObservation.Honor(m.honor().objectives(),lives),m.quest(),m.timeAttack(),m.siege(),m.race(),m.raceResultId(),m.timeAttackResult(),m.siegeResult()));
    }
    static ModernBattleObservation.Capture quest(ModernBattleObservation.Capture c,List<ModernQuestLife.State> lives) {
        var m=c.modes();var q=m.quest();return c.withModes(new ModernBattleObservation.Modes(m.isolatedForces(),m.honor(),
                new ModernBattleObservation.Quest(q.progress(),q.objectives(),q.exchanges(),lives),m.timeAttack(),m.siege(),m.race(),m.raceResultId(),m.timeAttackResult(),m.siegeResult()));
    }
    static ModernBattleObservation.Capture siege(ModernBattleObservation.Capture c,List<ModernSiege.Life> lives) {
        var m=c.modes();return c.withModes(new ModernBattleObservation.Modes(m.isolatedForces(),m.honor(),m.quest(),m.timeAttack(),
                new ModernSiege.Snapshot(m.siege().state(),lives),m.race(),m.raceResultId(),m.timeAttackResult(),m.siegeResult()));
    }
    static <T> List<T> replace(List<T> rows,int index,T value) {var next=new ArrayList<>(rows);next.set(index,value);return List.copyOf(next);}
    static ModernHonor.Life honorGain(ModernHonor.Life l,long gain) {
        return new ModernHonor.Life(l.entityId(),l.deathCount(),l.lastDeathRevision(),l.respawnCount(),l.respawnRevision(),gain,l.pendingDueMillis(),l.deathX(),l.deathY());
    }
    static ModernQuestLife.State questGain(ModernQuestLife.State l,long gain) {
        return new ModernQuestLife.State(l.entityId(),l.lane(),l.spawn(),l.deaths(),l.deathRevision(),l.respawns(),l.respawnRevision(),gain,l.deathCell());
    }
    static ModernSiege.Life siegeGain(ModernSiege.Life l,long gain) {
        return new ModernSiege.Life(l.entityId(),l.deaths(),l.deathRevision(),l.respawns(),l.respawnRevision(),gain,l.dueMillis());
    }
    static void siegeDeath(ModernBattleEngineSiegeTest.Fixture f) {
        f.active();f.walk(1,ModernBattleEngineSiegeTest.cell(31,11));f.waitMillis(200);
        assertEquals(ModernSiege.PortalReason.MOVED,f.portal(1).reason());f.walk(1,ModernBattleEngineSiegeTest.cell(11,10));f.waitMillis(200);
        assertEquals(ModernBattleEngine.AttackStatus.ATTACKED,f.attack(0,1,2).status());
        assertEquals(0,f.engine.serviceObservation().world().actors().get(1).vital().values().hp());
    }
    @Test void honorCannotClaimMoreRespawnHpThanTheAppliedResourceLedger()throws Exception {
        try(var f=new ModernBattleEngineHonorTest.Fixture(1000,1,100,1_000_000_000L)) {
            f.active();assertEquals(ModernSpellProtocol.Reason.NONE,f.cast(0,1,4,2).reason());f.elapsed(5_000_000_000L);
            var c=f.engine.serviceObservation();var rows=c.modes().honor().lives();assertEquals(1,rows.get(1).respawnHpGain());
            var forged=honor(c,replace(rows,1,honorGain(rows.get(1),2)));
            assertThrows(IllegalArgumentException.class,forged::world);
        }
    }
    @Test void questCannotClaimMoreRespawnHpThanTheAppliedResourceLedger()throws Exception {
        try(var f=ModernQuestLifeTest.fixture(2)) {
            f.active();var dead=ModernQuestLifeTest.die(f,100);
            assertEquals(ModernQuestLife.Reason.REVIVED,ModernQuestLifeTest.confirm(f,0,ModernQuestLifeTest.life(dead,0)).reason());
            var c=f.engine.serviceObservation();var rows=c.modes().quest().lives();assertEquals(1,rows.get(0).hpGain());
            var forged=quest(c,replace(rows,0,questGain(rows.get(0),2)));
            assertThrows(IllegalArgumentException.class,forged::world);
        }
    }
    @Test void siegeCannotClaimMoreRespawnHpThanTheAppliedResourceLedger()throws Exception {
        try(var f=new ModernBattleEngineSiegeTest.Fixture(0,1,120_000,1234,0,false)) {
            siegeDeath(f);f.waitMillis(1234);var c=f.engine.serviceObservation();var rows=c.modes().siege().lives();assertEquals(1,rows.get(1).hpGain());
            var forged=siege(c,replace(rows,1,siegeGain(rows.get(1),2)));
            assertThrows(IllegalArgumentException.class,forged::world);
        }
    }
    static ModernBattleObservation.World actors(ModernBattleObservation.World w,List<ModernBattleObservation.Actor> actors) {
        return new ModernBattleObservation.World(w.identity(),w.revision(),w.phase(),w.abortReason(),w.room(),w.manifest(),w.features(),w.clock(),actors,w.modes(),w.npcs(),w.result());
    }
    static ModernBattleObservation.Actor resources(ModernBattleObservation.Actor a,ModernVitalBalance.State resources) {
        return new ModernBattleObservation.Actor(a.pose(),a.vital(),resources,a.spells(),a.castIntervalNanos(),a.buffs(),a.regeneration(),a.presentation());
    }
    static ModernBattleObservation.Actor position(ModernBattleObservation.Actor a,ModernWorldMap.Cell cell) {
        var p=a.pose();return new ModernBattleObservation.Actor(new ModernBattleEngine.Actor(p.entityId(),p.member(),cell,p.heading(),p.loaded(),p.lastMoveSequence(),p.stepIntervalNanos()),
                a.vital(),a.resources(),a.spells(),a.castIntervalNanos(),a.buffs(),a.regeneration(),a.presentation());
    }
    @Test void honorDeadPositionTimerAndResourceRevisionMustMatchOneObservation()throws Exception {
        try(var f=new ModernBattleEngineHonorTest.Fixture(1000,1,100,1_000_000_000L)) {
            f.active();f.cast(0,1,4,2);var c=f.engine.serviceObservation();var rows=c.modes().honor().lives();var l=rows.get(1);
            for(long due:new long[]{0,l.pendingDueMillis()+1}) {
                var changed=new ModernHonor.Life(l.entityId(),l.deathCount(),l.lastDeathRevision(),l.respawnCount(),l.respawnRevision(),l.respawnHpGain(),due,l.deathX(),l.deathY());
                assertThrows(IllegalArgumentException.class,()->honor(c,replace(rows,1,changed)).world());
            }
            var moved=new ModernHonor.Life(l.entityId(),l.deathCount(),l.lastDeathRevision(),l.respawnCount(),l.respawnRevision(),l.respawnHpGain(),l.pendingDueMillis(),l.deathX()+1,l.deathY());
            assertThrows(IllegalArgumentException.class,()->honor(c,replace(rows,1,moved)).world());
            var w=c.world();var a=w.actors().get(1);var r=a.resources();
            var stale=new ModernVitalBalance.State(1,r.equipmentRevision(),r.life(),r.birthHp(),r.birthMp(),r.vitals(),r.flows());
            assertThrows(IllegalArgumentException.class,()->actors(w,replace(w.actors(),1,resources(a,stale))));
            assertEquals(w,c.world());
        }
    }
    @Test void honorAbortClearsPendingWithoutInventingResurrectionAndLoadingStaysInitial()throws Exception {
        try(var f=new ModernBattleEngineHonorTest.Fixture(1000,1,100,1_000_000_000L)) {
            var loading=f.engine.serviceObservation().world();assertEquals(ModernBattleEngine.Phase.LOADING,loading.phase());
            assertTrue(loading.modes().honor().lives().stream().allMatch(l->l.deathCount()==0));
            f.active();f.cast(0,1,4,2);var dead=f.engine.serviceObservation();f.rooms.releasePreparation(f.ticket);
            var aborted=f.engine.serviceObservation();var l=aborted.modes().honor().lives().get(1);
            assertEquals(ModernBattleEngine.Phase.ABORTED,aborted.world().phase());assertEquals(0,l.pendingDueMillis());assertEquals(0,l.respawnHpGain());
            assertEquals(0,aborted.world().actors().get(1).vital().values().hp());
            assertThrows(IllegalArgumentException.class,()->honor(aborted,dead.modes().honor().lives()).world());
        }
    }
    @Test void questCannotOmitRequiredLivesSwitchForceOrMoveTheRecordedSpawn()throws Exception {
        try(var f=ModernQuestLifeTest.fixture(2)) {
            var initial=f.engine.serviceObservation();assertThrows(IllegalArgumentException.class,()->quest(initial,List.of()).world());
            var rows=initial.modes().quest().lives();var l=rows.get(0);
            var foreign=new ModernQuestLife.State(l.entityId(),1-l.lane(),l.spawn(),0,0,0,0,0,l.deathCell());
            assertThrows(IllegalArgumentException.class,()->quest(initial,replace(rows,0,foreign)).world());
            var moved=new ModernQuestLife.State(l.entityId(),l.lane(),ModernQuestLifeTest.cell(12,12),0,0,0,0,0,l.deathCell());
            assertThrows(IllegalArgumentException.class,()->quest(initial,replace(rows,0,moved)).world());
            assertThrows(IllegalArgumentException.class,()->quest(initial,rows.subList(0,1)).world());
            assertEquals(initial.world(),f.engine.serviceObservation().world());
        }
    }
    @Test void questWaitsForConfirmationAndBindsActualReturnPositionAtEachLife()throws Exception {
        try(var f=ModernQuestLifeTest.fixture(2)) {
            f.active();assertEquals(ModernBattleEngine.MoveStatus.MOVED,f.move(0,1,10,10,0).status());
            var dead=ModernQuestLifeTest.die(f,100);f.elapsed(5000);var waiting=f.engine.serviceObservation();
            assertEquals(0,waiting.world().actors().get(0).vital().values().hp());assertEquals(0,waiting.modes().quest().lives().get(0).hpGain());
            var revived=ModernQuestLifeTest.confirm(f,0,ModernQuestLifeTest.life(dead,0)).observation();var w=revived.world();
            assertEquals(ModernQuestLifeTest.cell(10,10),w.actors().get(0).pose().position());
            assertEquals(1,w.actors().get(0).resources().flows().hpGranted());
            assertThrows(IllegalArgumentException.class,()->actors(w,replace(w.actors(),0,position(w.actors().get(0),ModernQuestLifeTest.cell(12,12)))));
            var second=ModernQuestLifeTest.die(f,5100);var after=ModernQuestLifeTest.confirm(f,0,ModernQuestLifeTest.life(second,0)).observation();
            assertEquals(2,after.world().actors().get(0).resources().life());assertEquals(2,after.modes().quest().lives().get(0).hpGain());
            assertEquals(1,revived.world().actors().get(0).resources().life());
        }
    }
    @Test void siegeTimerAndRoleCampCannotBeForgedOnDeathOrResurrection()throws Exception {
        try(var f=new ModernBattleEngineSiegeTest.Fixture(0,1,120_000,1234,0,false)) {
            siegeDeath(f);var c=f.engine.serviceObservation();var rows=c.modes().siege().lives();var l=rows.get(1);
            for(long due:new long[]{0,l.dueMillis()+1}) {
                var wrong=new ModernSiege.Life(l.entityId(),l.deaths(),l.deathRevision(),l.respawns(),l.respawnRevision(),l.hpGain(),due);
                assertThrows(IllegalArgumentException.class,()->siege(c,replace(rows,1,wrong)).world());
            }
            f.waitMillis(1234);var revived=f.engine.serviceObservation().world();
            assertThrows(IllegalArgumentException.class,()->actors(revived,replace(revived.actors(),1,position(revived.actors().get(1),ModernBattleEngineSiegeTest.cell(12,10)))));
            assertEquals(1,revived.actors().get(1).resources().flows().hpGranted());
        }
    }
    @Test void occupiedSiegeCampKeepsOverdueDeadLifeUntilARealCellIsFree()throws Exception {
        try(var f=new ModernBattleEngineSiegeTest.Fixture(0,1,120_000,50_000,0,false)) {
            siegeDeath(f);f.waitMillis(1200);assertEquals(ModernBattleEngine.AttackStatus.ATTACKED,f.tower(0,2,ModernSiege.Slot.OUTER_LEFT).status());
            // The defender's corpse still owns (11,10); use the open row below it.
            f.walk(0,ModernBattleEngineSiegeTest.cell(12,11));f.walk(0,ModernBattleEngineSiegeTest.cell(12,10));f.waitMillis(200);
            assertEquals(ModernBattleEngine.AttackStatus.ATTACKED,f.tower(0,3,ModernSiege.Slot.OUTER_RIGHT).status());
            f.walk(0,ModernBattleEngineSiegeTest.cell(31,10));var before=f.engine.serviceObservation();
            long remaining=before.modes().siege().lives().get(1).dueMillis()-before.clock().activeElapsedNanos()/1_000_000;
            assertTrue(remaining>0);f.waitMillis(remaining+1);var blocked=f.engine.serviceObservation();
            assertEquals(0,blocked.world().actors().get(1).vital().values().hp());assertEquals(0,blocked.modes().siege().lives().get(1).hpGain());
            assertTrue(blocked.modes().siege().lives().get(1).dueMillis()<blocked.clock().activeElapsedNanos()/1_000_000);
            assertEquals(ModernBattleEngine.MoveStatus.MOVED,f.step(0,4).status());var free=f.engine.serviceObservation();
            assertEquals(1,free.world().actors().get(1).vital().values().hp());assertEquals(1,free.modes().siege().lives().get(1).hpGain());
            assertEquals(ModernBattleEngineSiegeTest.cell(31,10),free.world().actors().get(1).pose().position());
        }
    }
    static void kill(ModernBattleItemRuntimeTest.Fixture f) {
        for(int n=0;n<100;n++) {
            if(f.view().world().actors().get(0).vital().values().hp()==0)return;
            f.game.clock.addAndGet(1_000_000_000L);assertEquals(ModernBattleEngine.AttackStatus.ATTACKED,f.game.attack(1,++f.attackSequence,1).status());
        }
        fail("real attacker failed to produce a death");
    }
    @Test void genuineEquipmentErasRespawn190Then100WithoutRewritingOldGrantOrSpell()throws Exception {
        try(var f=new ModernBattleItemRuntimeTest.Fixture()) {
            f.game.active();assertEquals(ModernSpellProtocol.Reason.NONE,f.game.cast(0,1,1,3).reason());
            var cast=f.view().world().actors().get(0).spells();kill(f);f.game.clock.addAndGet(5_000_000_000L);
            var first=f.view();assertEquals(190,first.world().actors().get(0).resources().flows().hpGranted());
            f.equipped(false);assertEquals(100,f.view().world().actors().get(0).vital().values().hp());
            assertThrows(IllegalArgumentException.class,()->ModernLegacyBattleProjection.from(f.game.engine.serviceObservation()));
            kill(f);f.game.clock.addAndGet(5_000_000_000L);var second=f.view();var r=second.world().actors().get(0).resources();
            assertEquals(2,r.life());assertEquals(290,r.flows().hpGranted());assertEquals(100,r.vitals().hp());
            assertEquals(290,second.world().modes().honor().lives().get(0).respawnHpGain());
            assertEquals(cast.receipts(),second.world().actors().get(0).spells().receipts());
            assertEquals(first.world().actors().get(0).resources().flows().spent(),r.flows().spent());assertEquals(0,r.flows().mpGranted());
            var c=f.game.engine.serviceObservation();var rows=c.modes().honor().lives();
            assertThrows(IllegalArgumentException.class,()->honor(c,replace(rows,0,honorGain(rows.get(0),200))).world());
            var d=ModernBattlePublicationTest.honor(c);d=new ModernMatchDescriptor(d.ruleset(),d.features()|ModernMatchDescriptor.FIRE_STORM,d.preparationTimeoutMillis(),d.loadTimeoutMillis(),d.countdownNanos(),d.activeTimeoutMillis());
            var audience=new ModernBattlePublication(c,d,null,null).audience(f.game.players.get(0).scope());
            assertEquals(r,audience.world().actors().get(0).resources());assertEquals(second.owner().history().history(),audience.personal().history());
            f.equipped(true);var equipped=f.view();assertEquals(100,equipped.world().actors().get(0).vital().values().hp());
            assertEquals(190,equipped.world().actors().get(0).vital().values().maximumHp());assertEquals(290,equipped.world().actors().get(0).resources().flows().hpGranted());
            assertEquals(190,first.world().modes().honor().lives().get(0).respawnHpGain());
        }
    }
    @Test void nonResurrectionModeCannotMintAnOtherwiseBalancedLifeGrant()throws Exception {
        try(var f=new ModernBattleEngineHonorTest.Fixture(100,100,100,1_000_000_000L,0,false,false)) {
            f.active();var w=f.engine.serviceObservation().world();var a=w.actors().get(0);var r=a.resources();
            var flows=new ModernVitalBalance.Flows(0,0,0,100,100,0,0,0,0,0,0);
            var invented=new ModernVitalBalance.State(w.revision(),r.equipmentRevision(),1,r.birthHp(),r.birthMp(),r.vitals(),flows);
            assertThrows(IllegalArgumentException.class,()->ModernBattleLifeProof.absent(replace(w.actors(),0,resources(a,invented))));
            assertDoesNotThrow(()->ModernBattleLifeProof.absent(w.actors()));
        }
    }
    @Test void impossibleEventCountIsRejectedWithoutOverflowingAtSigned64Boundary()throws Exception {
        try(var f=new ModernBattleEngineHonorTest.Fixture()) {
            f.active();var w=f.engine.serviceObservation().world();var a=w.actors().get(0);var p=a.pose();var v=a.vital();
            var values=new ModernEquipmentVitals.Vitals(0,v.values().maximumHp(),0,v.values().maximumMp());
            var r=new ModernVitalBalance.State(Long.MAX_VALUE,0,Long.MAX_VALUE-1,1,0,values,new ModernVitalBalance.Flows(0,0,0,Long.MAX_VALUE,Long.MAX_VALUE-1,0,0,0,0,0,0));
            var vital=new ModernBattleObservation.Vital(values,v.ac(),v.weaponType(),0,0,Long.MAX_VALUE,Long.MAX_VALUE,0,v.attackIntervalNanos());
            var forged=new ModernBattleObservation.Actor(p,vital,r,a.spells(),a.castIntervalNanos(),a.buffs(),a.regeneration(),a.presentation());
            var l=new ModernHonor.Life(1,Long.MAX_VALUE,Long.MAX_VALUE,Long.MAX_VALUE-1,Long.MAX_VALUE-1,Long.MAX_VALUE-1,5000,p.position().x(),p.position().y());
            assertThrows(IllegalArgumentException.class,()->ModernBattleLifeProof.honor(Long.MAX_VALUE,w.phase(),w.clock(),w.manifest(),replace(w.actors(),0,forged),replace(w.modes().honor().lives(),0,l)));
        }
    }
    @Test void honorReadMayAdvanceClockWithoutCreatingAnotherWorldRevision()throws Exception {
        try(var f=new ModernBattleEngineHonorTest.Fixture(1000,1,100,1_000_000_000L)) {
            f.active();f.cast(0,1,4,2);var dead=f.engine.serviceObservation().world();f.elapsed(100_000_000L);
            var waiting=f.engine.serviceObservation().world();assertEquals(dead.revision(),waiting.revision());
            assertTrue(waiting.clock().activeElapsedNanos()>dead.clock().activeElapsedNanos());
            assertEquals(dead.modes().honor().lives(),waiting.modes().honor().lives());assertEquals(0,waiting.actors().get(1).vital().values().hp());
        }
    }
    @Test void siegeReadMayAdvanceClockWithoutCreatingAnotherWorldRevision()throws Exception {
        try(var f=new ModernBattleEngineSiegeTest.Fixture(0,1,120_000,1234,0,false)) {
            siegeDeath(f);var dead=f.engine.serviceObservation().world();f.waitMillis(100);
            var waiting=f.engine.serviceObservation().world();assertEquals(dead.revision(),waiting.revision());
            assertTrue(waiting.clock().activeElapsedNanos()>dead.clock().activeElapsedNanos());
            assertEquals(dead.modes().siege().lives(),waiting.modes().siege().lives());assertEquals(0,waiting.actors().get(1).vital().values().hp());
        }
    }
}
