package local.lito381.server;

import static org.junit.jupiter.api.Assertions.*;
import static local.lito381.server.ModernBattleEngineTimedTest.*;
import static local.lito381.server.ModernGreaterHasteCast.Reason.*;
import static local.lito381.server.ModernGreaterHasteCatalog.Status.QUALIFIED;
import static local.lito381.server.ModernGreaterHasteCatalog.Status.UNSUPPORTED_TEMPLATE;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Real captured room, two sources sharing objid3333, canonical engine reads.
 * This is NOT a registered network command or a client/UI acceptance test. */
class ModernBattleGreaterHasteTest {
    static final ModernUseHistory.Lane LANE=ModernUseHistory.Lane.GREATER_HASTE;
    static ModernBattleEngine.PreparedMember enable(ModernBattleEngine.PreparedMember p,ModernGreaterHasteCatalog.Status status) {
        var skills=new ArrayList<>(p.skills().skills());
        if(status!=ModernGreaterHasteCatalog.Status.UNLEARNED)skills.add(new ModernSkills.Skill(54,"$1976",60,10,ModernSkills.Support.UNIMPLEMENTED));
        skills.sort(java.util.Comparator.comparingInt(ModernSkills.Skill::skillId));
        return new ModernBattleEngine.PreparedMember(p.owner(),p.scope(),p.character(),p.spawn(),p.stepIntervalNanos(),p.fighter(),p.attackIntervalNanos(),
                new ModernSkills.Snapshot(p.scope().sourceId(),p.character(),skills),p.castIntervalNanos(),p.damageMagic(),p.matchItems(),p.weightBasis(),
                p.timedEnabled(),p.bowEnabled(),p.regenEnabled(),p.fireStormEnabled(),p.disintegrate(),p.npcDisintegrate())
                .withGreaterHaste(new ModernGreaterHasteCatalog.Snapshot(p.fighter().key(),status));
    }
    static Fixture fixture()throws Exception{return fixture(300,1000,1000,false,QUALIFIED,QUALIFIED);}
    static Fixture fixture(long mp,long hp0,long hp1,boolean settle,ModernGreaterHasteCatalog.Status first,ModernGreaterHasteCatalog.Status second)throws Exception {
        return new Fixture(3,2,hp0,hp1,settle,p->{
            var fighter=ModernMagicCombatTest.createFighter(p.scope().sourceId(),p.character(),p.fighter().profile().effective().maximumHp(),mp,12,0);
            var updated=new ModernBattleEngine.PreparedMember(p.owner(),p.scope(),p.character(),p.spawn(),p.stepIntervalNanos(),fighter,p.attackIntervalNanos(),
                    p.skills(),p.castIntervalNanos(),p.damageMagic(),p.matchItems(),ModernInventoryWeight.qualify(fighter.profile(),100),true);
            return enable(updated,p.scope().sourceId()==7?first:second);
        });
    }
    static ModernBattleObservation.Capture active(Fixture f) {
        ModernBattleObservation.Capture result=null;
        for(var p:f.players)result=f.engine.ackLoadedObservation(p.owner(),p.scope(),f.ticket.matchId(),f.ticket.generation(),f.map.digest());
        assertEquals(ModernBattleEngine.Phase.ACTIVE,result.phase());result.world();return result;
    }
    static ModernGreaterHasteCast.Request request(Fixture f,int index,long sequence) {
        var c=f.engine.serviceObservation();var a=c.actors().get(index);var p=f.players.get(index);
        return new ModernGreaterHasteCast.Request(p.scope(),f.ticket.matchId(),f.ticket.generation(),sequence,
                a.resources().equipmentRevision(),a.resources().life()+1,a.pose().position().x(),a.pose().position().y(),a.buffs().revision());
    }
    static ModernBattleEngine.GreaterHasteResult cast(Fixture f,int index,long sequence) {
        var result=f.engine.greaterHaste(f.players.get(index).owner(),request(f,index,sequence));
        result.observation().forOwner(f.players.get(index).scope());return result;
    }
    @Test void loadingDeniedThenAtomicMpLeaseAndExactReceiptAreScopedToTheSelectedSource()throws Exception {
        try(var f=fixture()) {
            assertEquals(NOT_ACTIVE,cast(f,0,1).reason());var before=active(f);
            var success=cast(f,0,2);assertEquals(NONE,success.reason());var c=success.observation();var a=c.actors().get(0);
            assertEquals(before.revision()+1,c.revision());assertEquals(240,a.vital().values().mp());assertEquals(60,a.resources().flows().spent());
            assertEquals(2400*SECOND,a.buffs().hasteUntilNanos());assertEquals(ModernTimedEffects.HasteOrigin.GREATER_HASTE,a.buffs().hasteOrigin());
            assertEquals(c.revision(),a.buffs().revision());var receipt=a.runtime().receipts().get(LANE);
            assertEquals(1,receipt.casts());assertEquals(60,receipt.spent());assertEquals(2,receipt.batch().use().sequence());assertTrue(receipt.batch().targets().isEmpty());
            assertEquals(new ModernSpellRuntime.GreaterHaste(0,2400*SECOND),receipt.detail());
            assertEquals(f.players.get(0).scope(),receipt.batch().source());assertEquals(300,c.actors().get(1).vital().values().mp());
            assertFalse(c.actors().get(1).buffs().hasEffects());assertEquals(0,f.draws.get());
            assertEquals(before.actors().get(0).supplies(),a.supplies());assertEquals(before.actors().get(0).weight(),a.weight());
            assertThrows(SecurityException.class,()->f.engine.greaterHaste(f.players.get(1).owner(),request(f,0,3)));
            assertThrows(SecurityException.class,()->f.engine.greaterHaste(f.players.get(0).owner(),request(f,0,2)));
        }
    }
    @Test void lifeGearPositionAndBuffRevisionsRejectWithoutAnyPartialCost()throws Exception {
        try(var f=fixture()) {
            active(f);var before=f.engine.serviceObservation().actors().get(0);var p=f.players.get(0);
            for(int fault=0;fault<4;fault++) {
                var q=request(f,0,fault+1);var changed=new ModernGreaterHasteCast.Request(q.scope(),q.matchId(),q.generation(),q.sequence(),
                        q.equipmentRevision()+(fault==0?1:0),q.life()+(fault==1?1:0),q.x()+(fault==2?1:0),q.y(),q.buffRevision()+(fault==3?1:0));
                var result=f.engine.greaterHaste(p.owner(),changed);assertEquals(STALE_STATE,result.reason());
                var a=result.observation().actors().get(0);assertEquals(before.resources(),a.resources());assertEquals(before.buffs(),a.buffs());
                assertEquals(before.histories(),a.histories());assertTrue(a.runtime().receipts().isEmpty());assertEquals(fault+1,a.runtime().sequence());
            }
            assertEquals(NONE,cast(f,0,5).reason());
        }
    }
    @Test void unlearnedAndUnsupportedSourceCannotBorrowOtherSourcesGrant()throws Exception {
        for(var status:List.of(ModernGreaterHasteCatalog.Status.UNLEARNED,UNSUPPORTED_TEMPLATE))try(var f=fixture(300,1000,1000,false,status,QUALIFIED)) {
            active(f);assertEquals(status==ModernGreaterHasteCatalog.Status.UNLEARNED?ModernGreaterHasteCast.Reason.UNLEARNED:UNSUPPORTED,cast(f,0,1).reason());
            assertEquals(NONE,cast(f,1,1).reason());var c=f.engine.serviceObservation();
            assertEquals(300,c.actors().get(0).vital().values().mp());assertEquals(240,c.actors().get(1).vital().values().mp());
            assertFalse(c.actors().get(0).buffs().hasEffects());
            var p=f.prepared.get(0);assertThrows(IllegalArgumentException.class,()->p.withGreaterHaste(new ModernGreaterHasteCatalog.Snapshot(f.prepared.get(1).fighter().key(),QUALIFIED)));
        }
    }
    @Test void sharedSpellCooldownIsAcceleratedButCannotBeBypassedByDifferentSkillOrRepeatedDenial()throws Exception {
        try(var f=fixture()) {
            active(f);assertEquals(NONE,cast(f,0,1).reason());
            assertEquals(TOO_FAST,cast(f,0,2).reason());f.elapsed(905*MS);
            assertEquals(ModernSpellProtocol.Reason.TOO_FAST,f.cast(0,3,4).reason());
            f.elapsed(906*MS);assertEquals(ModernSpellProtocol.Reason.NONE,f.cast(0,4,4).reason());
            var c=f.engine.serviceObservation();assertEquals(1,c.actors().get(0).runtime().receipts().get(LANE).casts());
            assertEquals(63,c.actors().get(0).resources().flows().spent());
        }
    }
    @Test void actualMovementUses151msAndDoesNotCompoundTheFrozenBase()throws Exception {
        try(var f=fixture()) {
            active(f);cast(f,0,1);f.elapsed(SECOND);var p=f.players.get(0);var at=f.engine.serviceObservation().actors().get(0).pose().position();
            var moved=f.engine.move(p.owner(),p.scope(),f.ticket.matchId(),f.ticket.generation(),1,at.x(),at.y(),0);
            assertEquals(ModernBattleEngine.MoveStatus.MOVED,moved.status());at=moved.observation().actors().get(0).pose().position();
            f.elapsed(SECOND+150*MS);assertEquals(ModernBattleEngine.MoveStatus.TOO_FAST,f.engine.move(p.owner(),p.scope(),f.ticket.matchId(),f.ticket.generation(),2,at.x(),at.y(),0).status());
            f.elapsed(SECOND+151*MS);assertEquals(ModernBattleEngine.MoveStatus.MOVED,f.engine.move(p.owner(),p.scope(),f.ticket.matchId(),f.ticket.generation(),3,at.x(),at.y(),0).status());
        }
    }
    @Test void potionOverwriteKeepsHistoricalSpendAndOtherBuffsPreserveSkillOrigin()throws Exception {
        try(var f=fixture()) {
            active(f);cast(f,0,1);f.elapsed(SECOND);
            assertEquals(ModernItemProtocol.Reason.NONE,f.use(0,40016).reason());var a=f.engine.serviceObservation().actors().get(0);
            assertEquals(ModernTimedEffects.HasteOrigin.GREATER_HASTE,a.buffs().hasteOrigin());assertEquals(2400*SECOND,a.buffs().hasteUntilNanos());
            f.elapsed(2*SECOND);assertEquals(ModernItemProtocol.Reason.NONE,f.use(0,40013).reason());a=f.engine.serviceObservation().actors().get(0);
            assertEquals(ModernTimedEffects.HasteOrigin.POTION,a.buffs().hasteOrigin());assertEquals(302*SECOND,a.buffs().hasteUntilNanos());
            f.elapsed(302*SECOND);a=f.engine.serviceObservation().actors().get(0);assertFalse(a.buffs().hasEffects());assertEquals(ModernTimedEffects.HasteOrigin.NONE,a.buffs().hasteOrigin());
            assertEquals(60,a.resources().flows().spent());assertEquals(1,a.runtime().receipts().get(LANE).casts());
            assertEquals(NONE,cast(f,0,2).reason());assertEquals(2702*SECOND,f.engine.serviceObservation().actors().get(0).buffs().hasteUntilNanos());
        }
    }
    @Test void refreshUsesNewCommitTimeAndMatchTimeoutClears40MinuteLeaseWithoutExtendingWorldLimit()throws Exception {
        try(var f=fixture()) {
            active(f);cast(f,0,1);f.elapsed(2*SECOND);assertEquals(NONE,cast(f,0,2).reason());
            var before=f.engine.serviceObservation().actors().get(0);assertEquals(2402*SECOND,before.buffs().hasteUntilNanos());assertEquals(120,before.resources().flows().spent());
            f.elapsed(1800*SECOND);var terminal=f.engine.serviceObservation();terminal.forOwner(f.players.get(0).scope());
            assertEquals(ModernBattleEngine.AbortReason.ACTIVE_TIMEOUT,terminal.abortReason());assertFalse(terminal.actors().get(0).buffs().hasEffects());
            assertEquals(before.resources(),terminal.actors().get(0).resources());assertEquals(NOT_ACTIVE,cast(f,0,3).reason());
        }
    }
    @Test void insufficientMpDoesNotExtendTheExistingLeaseOrCreateASecondSuccess()throws Exception {
        try(var f=fixture(60,1000,1000,false,QUALIFIED,QUALIFIED)) {
            active(f);cast(f,0,1);f.elapsed(2*SECOND);var before=f.engine.serviceObservation().actors().get(0);
            assertEquals(NO_MP,cast(f,0,2).reason());var after=f.engine.serviceObservation().actors().get(0);
            assertEquals(before.resources(),after.resources());assertEquals(before.buffs(),after.buffs());assertEquals(before.histories(),after.histories());
            assertEquals(1,after.runtime().receipts().get(LANE).casts());assertEquals(0,f.draws.get());
        }
    }
    @Test void lethalResolutionClearsWinnerAndLoserLeasesAtSettlementRevision()throws Exception {
        try(var f=fixture(300,1000,1,true,QUALIFIED,QUALIFIED)) {
            active(f);cast(f,0,1);cast(f,1,1);f.elapsed(2*SECOND);
            var result=f.cast(0,2,4);assertEquals(ModernSpellProtocol.Reason.NONE,result.reason());var c=result.observation();c.world();
            assertEquals(ModernBattleEngine.Phase.FINISHED,c.phase());assertEquals(c.revision(),c.result().engineRevision());
            assertTrue(c.actors().stream().allMatch(a->!a.buffs().hasEffects()&&a.buffs().revision()==c.revision()));
            assertEquals(60,c.actors().get(1).resources().flows().spent());
        }
    }
    @Test void cancelledPreparationClearsLeaseWithoutRefundOrAnotherCommandCommit()throws Exception {
        try(var f=fixture()) {
            active(f);cast(f,0,1);var before=f.engine.serviceObservation().actors().get(0);var q=request(f,0,2);
            f.rooms.releasePreparation(f.ticket);var result=f.engine.greaterHaste(f.players.get(0).owner(),q);assertEquals(NOT_ACTIVE,result.reason());
            var after=result.observation().actors().get(0);assertEquals(before.resources(),after.resources());assertEquals(before.runtime(),after.runtime());
            assertFalse(after.buffs().hasEffects());assertEquals(ModernBattleEngine.AbortReason.PREPARATION_CANCELLED,result.observation().abortReason());
        }
    }
    @Test void oldPublicationsAndSerializersRefuseEvenAnUncastEnabledWorld()throws Exception {
        try(var f=fixture()) {
            var c=active(f);assertThrows(IllegalArgumentException.class,()->ModernLegacyBattleProjection.from(c));
            assertThrows(IllegalArgumentException.class,()->ModernBattleWireTest.publication(c,null));
            var a=c.actors().get(0);assertThrows(IllegalArgumentException.class,()->ModernLegacySpellProjection.from(1,0,1200,a.runtime(),a.histories()));
            var h=c.forOwner(f.players.get(0).scope()).owner().history().history();assertThrows(IllegalArgumentException.class,()->ModernEquipmentHistoryWire.encode(h));
            // Defense in depth: even direct trusted audience assembly cannot encode it.
            var world=c.world();var descriptor=new ModernMatchDescriptor(ModernMatchDescriptor.Ruleset.PVP_SKIRMISH,63,2000,2000,0,1_800_000);
            var audience=ModernBattleAudience.project(world,descriptor,null,null,c.ownerViews(world),c).get(f.players.get(0).scope());
            for(int schema:new int[]{1,2})assertThrows(IllegalArgumentException.class,()->ModernBattleWire.encode(audience,schema));
            var lease=cast(f,0,1).observation().actors().get(0).buffs();var bytes=ByteBuffer.allocate(64);
            assertThrows(IllegalArgumentException.class,()->ModernBuffProtocol.putStates(bytes,List.of(lease)));assertEquals(0,bytes.position());
            assertThrows(IllegalArgumentException.class,()->new ModernTimedEffects.State(1,2,0,2400*SECOND,0,0,0));
        }
        try(var old=new Fixture()) {
            old.active();assertEquals(UNSUPPORTED,cast(old,0,1).reason());assertFalse(old.engine.tick().buffs().get(0).hasEffects());
        }
    }
    static ModernBattleObservation.Capture withBuff(ModernBattleObservation.Capture c,ModernTimedEffects.State buff) {
        var actors=new ArrayList<>(c.actors());var a=actors.get(0);
        actors.set(0,new ModernBattleObservation.CapturedActor(a.pose(),a.scope(),a.loadout(),a.skills(),a.vital(),a.resources(),a.castIntervalNanos(),
                a.histories(),a.targets(),a.runtime(),a.supplies(),a.weight(),buff,a.ammo(),a.regeneration(),a.regenClock(),a.presentation()));
        return new ModernBattleObservation.Capture(c.identity(),c.revision(),c.phase(),c.abortReason(),c.room(),c.manifest(),c.features(),c.clock(),
                c.limits(),c.loadingOriginNanos(),actors,c.modes(),c.npcs(),c.result());
    }
    @Test void activeLeaseRequiresItsExactCastReceiptNotOnlyAPlausibleDuration()throws Exception {
        try(var f=fixture()) {
            var initial=active(f);var fabricated=ModernTimedEffects.applyGreaterHaste(initial.actors().get(0).buffs(),0,initial.revision());
            assertThrows(IllegalArgumentException.class,()->withBuff(initial,fabricated).world());
            var real=cast(f,0,1).observation();var lease=real.actors().get(0).buffs();
            var changed=new ModernTimedEffects.State(1,lease.revision(),0,lease.hasteUntilNanos()-1,0,0,0,ModernTimedEffects.HasteOrigin.GREATER_HASTE);
            assertThrows(IllegalArgumentException.class,()->withBuff(real,changed).world());
            assertDoesNotThrow(real::world);assertEquals(60,real.actors().get(0).resources().flows().spent());
        }
    }
}
