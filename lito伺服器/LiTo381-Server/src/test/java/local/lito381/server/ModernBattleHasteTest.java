package local.lito381.server;

import static org.junit.jupiter.api.Assertions.*;
import static local.lito381.server.ModernBattleEngineTimedTest.*;
import static local.lito381.server.ModernHasteCast.Reason.*;
import static local.lito381.server.ModernHasteCatalog.Status.QUALIFIED;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

/** Real engine transactions, deliberately private until a new target-capable
 * publication contract exists. No claim of a registered client/UI command. */
class ModernBattleHasteTest {
    static final ModernUseHistory.Lane LANE=ModernUseHistory.Lane.HASTE;
    static ModernBattleEngine.PreparedMember enable(ModernBattleEngine.PreparedMember p,ModernHasteCatalog.Status status) {
        var skills=new ArrayList<>(p.skills().skills());
        if(status!=ModernHasteCatalog.Status.UNLEARNED)skills.add(new ModernSkills.Skill(43,"$1463",40,10,ModernSkills.Support.UNIMPLEMENTED));
        skills.sort(java.util.Comparator.comparingInt(ModernSkills.Skill::skillId));
        return new ModernBattleEngine.PreparedMember(p.owner(),p.scope(),p.character(),p.spawn(),p.stepIntervalNanos(),p.fighter(),p.attackIntervalNanos(),
                new ModernSkills.Snapshot(p.scope().sourceId(),p.character(),skills),p.castIntervalNanos(),p.damageMagic(),p.matchItems(),p.weightBasis(),
                p.timedEnabled(),p.bowEnabled(),p.regenEnabled(),p.fireStormEnabled(),p.disintegrate(),p.npcDisintegrate(),p.greaterHaste())
                .withHaste(new ModernHasteCatalog.Snapshot(p.fighter().key(),status));
    }
    static Fixture fixture()throws Exception{return fixture(300,QUALIFIED,QUALIFIED);}
    static Fixture fixture(long mp,ModernHasteCatalog.Status a,ModernHasteCatalog.Status b)throws Exception {
        return new Fixture(3,2,1000,1000,false,p->{
            var fighter=ModernMagicCombatTest.createFighter(p.scope().sourceId(),p.character(),1000,mp,12,0);
            var member=new ModernBattleEngine.PreparedMember(p.owner(),p.scope(),p.character(),p.spawn(),p.stepIntervalNanos(),fighter,p.attackIntervalNanos(),
                    p.skills(),p.castIntervalNanos(),p.damageMagic(),p.matchItems(),ModernInventoryWeight.qualify(fighter.profile(),100),true);
            return enable(member,p.scope().sourceId()==7?a:b);
        });
    }
    static ModernBattleObservation.Capture active(Fixture f) {
        for(var p:f.players)f.engine.ackLoadedObservation(p.owner(),p.scope(),f.ticket.matchId(),f.ticket.generation(),f.map.digest());
        var c=f.engine.serviceObservation();assertEquals(ModernBattleEngine.Phase.ACTIVE,c.phase());return c;
    }
    static ModernHasteCast.Player target(ModernBattleObservation.Capture c,int index) {
        var a=c.actors().get(index);
        return new ModernHasteCast.Player(new ModernTargetHistory.Participant(a.pose().entityId(),a.pose().member().identity()),
                a.resources().life()+1,a.resources().equipmentRevision(),a.pose().position(),a.buffs().revision());
    }
    static ModernHasteCast.Request request(ModernBattleObservation.Capture c,int caster,long sequence,ModernHasteCast.Target target) {
        var a=c.actors().get(caster);
        return new ModernHasteCast.Request(a.scope(),c.identity().matchId(),c.identity().generation(),sequence,
                a.resources().equipmentRevision(),a.resources().life()+1,a.pose().position(),a.buffs().revision(),target);
    }
    static ModernHasteCast.Request request(Fixture f,int caster,long sequence,int target) {
        var c=f.engine.serviceObservation();return request(c,caster,sequence,target(c,target));
    }
    static ModernBattleEngine.HasteResult cast(Fixture f,int caster,long sequence,int target) {
        return f.engine.haste(f.players.get(caster).owner(),request(f,caster,sequence,target));
    }
    static ModernEquipmentHistory.State history(ModernBattleObservation.Capture c,int index) {
        var a=c.actors().get(index);return new ModernEquipmentHistory.State(a.runtime().identity(),c.revision(),a.resources(),a.histories(),
                a.supplies()==null?null:a.supplies().history(),a.targets());
    }
    static void unchanged(ModernBattleObservation.CapturedActor before,ModernBattleObservation.CapturedActor after) {
        assertEquals(before.resources(),after.resources());assertEquals(before.buffs(),after.buffs());
        assertEquals(before.histories(),after.histories());assertEquals(before.targets(),after.targets());
    }
    @Test void loadingDenialThenCrossSourceSameObjidCommitsOnlyCasterMpAndVictimLease()throws Exception {
        try(var f=fixture()) {
            assertEquals(NOT_ACTIVE,cast(f,0,1,1).reason());var before=active(f);
            assertNotEquals(before.actors().get(0).pose().member().team(),before.actors().get(1).pose().member().team());
            var result=cast(f,0,2,1);assertEquals(NONE,result.reason());var c=result.observation();
            assertEquals(before.revision()+1,c.revision());var caster=c.actors().get(0);var victim=c.actors().get(1);
            assertEquals(260,caster.vital().values().mp());assertEquals(40,caster.resources().flows().spent());
            assertFalse(caster.buffs().hasEffects());assertEquals(before.actors().get(1).resources(),victim.resources());
            assertEquals(1200*SECOND,victim.buffs().hasteUntilNanos());assertEquals(c.revision(),victim.buffs().revision());
            assertEquals(ModernTimedEffects.HasteOrigin.HASTE,victim.buffs().hasteOrigin());
            var r=caster.runtime().receipts().get(LANE);assertEquals(1,r.casts());assertEquals(40,r.spent());
            assertEquals(2,r.batch().use().sequence());assertTrue(r.batch().targets().isEmpty());assertEquals(7,r.batch().source().sourceId());
            var d=(ModernHasteReceipt)r.detail();var t=(ModernHasteReceipt.Player)d.target();
            assertEquals(42,t.participant().identity().sourceId());assertEquals(3333,t.participant().identity().id());
            assertEquals(before.actors().get(1).buffs(),t.before());assertEquals(victim.buffs(),t.after());
            assertEquals(0,d.activeElapsedNanos());assertEquals(0,f.draws.get());
            assertDoesNotThrow(()->new ModernSpellRuntime.Snapshot(history(c,0),caster.runtime()));
            assertEquals(before.actors().get(0).supplies(),caster.supplies());assertEquals(before.actors().get(0).weight(),caster.weight());
        }
    }
    @Test void selfTargetUsesTheSameReceiptAndSharedSpellClockWithoutCompoundingBase()throws Exception {
        try(var f=fixture()) {
            active(f);assertEquals(NONE,cast(f,0,1,0).reason());assertEquals(TOO_FAST,cast(f,0,2,0).reason());
            f.elapsed(905*MS);assertEquals(ModernSpellProtocol.Reason.TOO_FAST,f.cast(0,3,4).reason());
            f.elapsed(906*MS);assertEquals(ModernSpellProtocol.Reason.NONE,f.cast(0,4,4).reason());
            f.elapsed(2*SECOND);var p=f.players.get(0);var at=f.engine.serviceObservation().actors().get(0).pose().position();
            assertEquals(ModernBattleEngine.MoveStatus.MOVED,f.engine.move(p.owner(),p.scope(),f.ticket.matchId(),f.ticket.generation(),1,at.x(),at.y(),0).status());
            at=f.engine.serviceObservation().actors().get(0).pose().position();
            f.elapsed(2*SECOND+150*MS);assertEquals(ModernBattleEngine.MoveStatus.TOO_FAST,f.engine.move(p.owner(),p.scope(),f.ticket.matchId(),f.ticket.generation(),2,at.x(),at.y(),0).status());
            f.elapsed(2*SECOND+151*MS);assertEquals(ModernBattleEngine.MoveStatus.MOVED,f.engine.move(p.owner(),p.scope(),f.ticket.matchId(),f.ticket.generation(),3,at.x(),at.y(),0).status());
            assertEquals(43,f.engine.serviceObservation().actors().get(0).resources().flows().spent());
        }
    }
    @Test void buffingAnotherPlayerDoesNotAccelerateTheCastersOwnCooldown()throws Exception {
        try(var f=fixture()) {
            active(f);cast(f,0,1,1);f.elapsed(906*MS);assertEquals(TOO_FAST,cast(f,0,2,1).reason());
            f.elapsed(1200*MS);assertEquals(NONE,cast(f,0,3,1).reason());
            assertEquals(1201200*MS,f.engine.serviceObservation().actors().get(1).buffs().hasteUntilNanos());
        }
    }
    @Test void staleCasterLifeGearCellAndBuffConsumeOnlyARequestNotResources()throws Exception {
        try(var f=fixture()) {
            var before=active(f).actors().get(0);
            for(int i=0;i<4;i++) {
                var q=request(f,0,i+1,1);var pos=q.cell();
                var stale=new ModernHasteCast.Request(q.scope(),q.matchId(),q.generation(),q.sequence(),q.equipmentRevision()+(i==0?1:0),q.life()+(i==1?1:0),
                        new ModernWorldMap.Cell(pos.x()+(i==2?1:0),pos.y()),q.buffRevision()+(i==3?1:0),q.target());
                var r=f.engine.haste(f.players.get(0).owner(),stale);assertEquals(STALE_STATE,r.reason());
                unchanged(before,r.observation().actors().get(0));assertTrue(r.observation().actors().get(0).runtime().receipts().isEmpty());
                assertEquals(i+1,r.observation().actors().get(0).runtime().sequence());assertFalse(r.observation().actors().get(1).buffs().hasEffects());
            }
            assertEquals(NONE,cast(f,0,5,1).reason());
        }
    }
    @Test void staleTargetLifeGearCellAndBuffNeverDebitCaster()throws Exception {
        try(var f=fixture()) {
            var before=active(f);
            for(int i=0;i<4;i++) {
                var c=f.engine.serviceObservation();var t=target(c,1);var pos=t.cell();
                var stale=new ModernHasteCast.Player(t.participant(),t.life()+(i==0?1:0),t.equipmentRevision()+(i==1?1:0),
                        new ModernWorldMap.Cell(pos.x()+(i==2?1:0),pos.y()),t.buffRevision()+(i==3?1:0));
                var r=f.engine.haste(f.players.get(0).owner(),request(c,0,i+1,stale));assertEquals(STALE_TARGET,r.reason());
                unchanged(before.actors().get(0),r.observation().actors().get(0));unchanged(before.actors().get(1),r.observation().actors().get(1));
            }
        }
    }
    @Test void forgedOwnerIdentityGenerationAndDuplicateSequenceCannotMutateAnything()throws Exception {
        try(var f=fixture()) {
            active(f);var q=request(f,0,1,1);var c=f.engine.serviceObservation();var t=target(c,1);
            assertThrows(SecurityException.class,()->f.engine.haste(f.players.get(1).owner(),q));
            var impostor=new ModernHasteCast.Player(new ModernTargetHistory.Participant(2,ModernParticipantId.human(7,3333)),t.life(),t.equipmentRevision(),t.cell(),t.buffRevision());
            assertThrows(SecurityException.class,()->f.engine.haste(f.players.get(0).owner(),request(c,0,1,impostor)));
            var foreign=new ModernHasteCast.Request(q.scope(),q.matchId(),q.generation()+1,1,q.equipmentRevision(),q.life(),q.cell(),q.buffRevision(),q.target());
            assertThrows(SecurityException.class,()->f.engine.haste(f.players.get(0).owner(),foreign));
            assertEquals(c,f.engine.serviceObservation());assertEquals(NONE,f.engine.haste(f.players.get(0).owner(),q).reason());
            var committed=f.engine.serviceObservation();assertThrows(SecurityException.class,()->f.engine.haste(f.players.get(0).owner(),q));
            assertEquals(committed,f.engine.serviceObservation());
        }
    }
    @Test void sourceGrantAndInsufficientMpCannotBeBorrowedFromTarget()throws Exception {
        for(var status:List.of(ModernHasteCatalog.Status.UNLEARNED,ModernHasteCatalog.Status.UNSUPPORTED_TEMPLATE))try(var f=fixture(300,status,QUALIFIED)) {
            active(f);assertEquals(status==ModernHasteCatalog.Status.UNLEARNED?UNLEARNED:UNSUPPORTED,cast(f,0,1,1).reason());
            assertEquals(NONE,cast(f,1,1,0).reason());assertEquals(300,f.engine.serviceObservation().actors().get(0).vital().values().mp());
        }
        try(var f=fixture(40,QUALIFIED,QUALIFIED)) {
            active(f);assertEquals(NONE,cast(f,0,1,1).reason());f.elapsed(2*SECOND);var before=f.engine.serviceObservation();
            assertEquals(NO_MP,cast(f,0,2,1).reason());var after=f.engine.serviceObservation();
            unchanged(before.actors().get(0),after.actors().get(0));unchanged(before.actors().get(1),after.actors().get(1));
            assertEquals(1,after.actors().get(0).runtime().receipts().get(LANE).casts());
        }
    }
    @Test void concurrentCastersUsingTheSameVictimRevisionHaveExactlyOneWinner()throws Exception {
        try(var f=fixture()) {
            var c=active(f);var t=target(c,1);var gate=new CountDownLatch(1);var pool=Executors.newFixedThreadPool(2);
            try {
                var a=pool.submit(()->{gate.await();return f.engine.haste(f.players.get(0).owner(),request(c,0,1,t));});
                var b=pool.submit(()->{gate.await();return f.engine.haste(f.players.get(1).owner(),request(c,1,1,t));});gate.countDown();
                var reasons=List.of(a.get(5,TimeUnit.SECONDS).reason(),b.get(5,TimeUnit.SECONDS).reason());
                assertEquals(1,reasons.stream().filter(r->r==NONE).count());
                assertEquals(1,reasons.stream().filter(r->r==STALE_STATE||r==STALE_TARGET).count());
                var after=f.engine.serviceObservation();assertEquals(40,after.actors().stream().mapToLong(x->x.resources().flows().spent()).sum());
                assertEquals(1,after.actors().stream().filter(x->x.runtime().receipts().containsKey(LANE)).count());
            }finally{pool.shutdownNow();assertTrue(pool.awaitTermination(5,TimeUnit.SECONDS));}
        }
    }
    @Test void potionOverwriteAndExpiryKeepTheOriginalTargetReceiptAndCost()throws Exception {
        try(var f=fixture()) {
            active(f);cast(f,0,1,1);var receipt=f.engine.serviceObservation().actors().get(0).runtime().receipts().get(LANE);
            f.elapsed(SECOND);assertEquals(ModernItemProtocol.Reason.NONE,f.use(1,40013).reason());
            assertEquals(ModernTimedEffects.HasteOrigin.POTION,f.engine.serviceObservation().actors().get(1).buffs().hasteOrigin());
            f.elapsed(301*SECOND);var c=f.engine.serviceObservation();assertFalse(c.actors().get(1).buffs().hasEffects());
            assertEquals(receipt,c.actors().get(0).runtime().receipts().get(LANE));assertEquals(40,c.actors().get(0).resources().flows().spent());
            assertEquals(NONE,cast(f,0,2,1).reason());f.elapsed(1501*SECOND);assertFalse(f.engine.serviceObservation().actors().get(1).buffs().hasEffects());
        }
    }
    @Test void cancellationClearsTargetButNeverRefundsCasterOrConsumesAnotherRequest()throws Exception {
        try(var f=fixture()) {
            active(f);cast(f,0,1,1);var q=request(f,0,2,1);var before=f.engine.serviceObservation();f.rooms.releasePreparation(f.ticket);
            var r=f.engine.haste(f.players.get(0).owner(),q);assertEquals(NOT_ACTIVE,r.reason());
            assertFalse(r.observation().actors().get(1).buffs().hasEffects());
            assertEquals(before.actors().get(0).resources(),r.observation().actors().get(0).resources());
            assertEquals(before.actors().get(0).runtime(),r.observation().actors().get(0).runtime());
        }
    }
    @Test void checkedWorldIncludesUncastCandidateButOldPacketCodecsStillRefuseIt()throws Exception {
        try(var f=fixture()) {
            var c=active(f);assertNotNull(c.world().hasteEffects());assertTrue(c.world().hasteEffects().targets().isEmpty());
            assertThrows(IllegalArgumentException.class,()->ModernLegacyBattleProjection.from(c));
            for(int schema:new int[]{1,2})assertThrows(IllegalArgumentException.class,()->ModernEquipmentHistoryWire.encode(history(c,0),schema));
            cast(f,0,1,1);var lease=f.engine.serviceObservation().actors().get(1).buffs();var b=ByteBuffer.allocate(128);
            assertThrows(IllegalArgumentException.class,()->ModernBuffProtocol.putStates(b,List.of(lease)));assertEquals(0,b.position());
        }
        try(var old=new Fixture()) {old.active();assertEquals(UNSUPPORTED,cast(old,0,1,1).reason());}
    }
    @Test void screenBoundaryMatchesNativeShapeAndHandlesCoordinateOverflow() {
        var center=new ModernWorldMap.Cell(100,100);
        for(int dx=-21;dx<=21;dx++)for(int dy=-21;dy<=21;dy++) {
            int distance=Math.abs(dx)+Math.abs(dy);int edge=Math.abs(dx+18)+Math.abs(dy+18);
            assertEquals(distance<=18||distance==19&&edge>=19&&edge<=52,ModernHasteCast.inScreen(center,new ModernWorldMap.Cell(100+dx,100+dy)));
            assertEquals(BattleSpellCatalog.isInNativeScreen(100,100,100+dx,100+dy),ModernHasteCast.inScreen(center,new ModernWorldMap.Cell(100+dx,100+dy)));
        }
        assertFalse(ModernHasteCast.inScreen(new ModernWorldMap.Cell(Integer.MIN_VALUE,0),new ModernWorldMap.Cell(Integer.MAX_VALUE,0)));
    }
}
