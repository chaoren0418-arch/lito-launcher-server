package local.lito381.server;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;

/** Real room ticket, qualified source fighter and engine; no socket, SQL or client damage. */
class ModernBattleEngineMagicTest {
    private static final ModernWorldMap.SectorKey SECTOR=new ModernWorldMap.SectorKey(0x8000,0x8000);
    private static final long STEP=TimeUnit.MILLISECONDS.toNanos(200),ATTACK=TimeUnit.MILLISECONDS.toNanos(700),CAST=TimeUnit.MILLISECONDS.toNanos(1200);
    private static final ModernBattleEngine.Limits LIMITS=new ModernBattleEngine.Limits(10_000,0,60_000);
    private static ModernWorldMap.Cell cell(int x,int y){return new ModernWorldMap.Cell(SECTOR.origin().x()+x,SECTOR.origin().y()+y);}
    private static ModernWorldMap map(boolean blocked)throws Exception {
        var bytes=ModernWorldMapTest.openSectorBytes();
        if(blocked)ModernWorldMapTest.setAttributes(bytes,2,2,(short)0,(short)1);
        return ModernWorldMap.fromSectorBytes(514,Map.of(SECTOR,bytes),()->false);
    }
    private static List<ModernSkills.Skill> learned() {
        return List.of(new ModernSkills.Skill(4,"$1439",3,10,ModernSkills.Support.LIGHT_ARROW),
                new ModernSkills.Skill(5,"$1440",10,10,ModernSkills.Support.TELEPORT));
    }
    @Test void targetedMagicCommitsMpHpAndIndependentEventInExactlyOneRevision()throws Exception {
        try(var f=new Fixture()) {
            var before=f.active();var cast=f.arrow(0,1,2);var after=cast.snapshot();
            assertEquals(ModernSpellProtocol.Reason.NONE,cast.reason());assertEquals(before.revision()+1,after.revision());
            var event=after.damages().get(0);assertEquals(1,event.castCount());assertEquals(4,event.skillId());
            assertEquals(2,event.targetEntityId());assertEquals(3,event.mpSpent());assertEquals(3,event.mpCost());assertEquals(after.revision(),event.castRevision());
            assertEquals(before.actors(),after.actors());assertEquals(27,after.vitals().get(0).mp());
            assertEquals(100,after.vitals().get(0).hp());assertTrue(after.vitals().get(1).hp()<100);
            assertEquals(after.revision(),after.vitals().get(1).hurtRevision());assertEquals(0,after.vitals().get(1).deathRevision());
            assertEquals(1,after.casts().get(0).requestSequence());assertEquals(0,after.casts().get(0).castCount());
            assertEquals(0,after.casts().get(0).castRevision());
            assertTrue(after.vitals().stream().allMatch(v->v.attackSequence()==0&&v.attackRevision()==0&&v.lastTargetEntityId()==0));
            assertEquals(before.actors().get(0).position(),new ModernWorldMap.Cell(event.fromX(),event.fromY()));
            assertEquals(before.actors().get(1).position(),new ModernWorldMap.Cell(event.toX(),event.toY()));
        }
    }
    @Test void teleportAndDamageShareSequenceButRetainSeparateMpAndVisualHistories()throws Exception {
        try(var f=new Fixture()) {
            f.active();var teleported=f.teleport(0,1).snapshot();var originalTeleport=teleported.casts().get(0);
            assertEquals(20,teleported.vitals().get(0).mp());f.clock.addAndGet(CAST);
            var reply=f.arrow(0,2,2);assertEquals(ModernSpellProtocol.Reason.NONE,reply.reason());var after=reply.snapshot();
            var cast=after.casts().get(0);assertEquals(2,cast.requestSequence());assertEquals(1,cast.castCount());assertEquals(10,cast.mpSpent());
            assertEquals(originalTeleport.castRevision(),cast.castRevision());assertEquals(originalTeleport.fromX(),cast.fromX());
            assertEquals(originalTeleport.fromY(),cast.fromY());assertEquals(originalTeleport.toX(),cast.toX());assertEquals(originalTeleport.toY(),cast.toY());
            assertEquals(1,after.damages().get(0).castCount());assertEquals(3,after.damages().get(0).mpSpent());assertEquals(17,after.vitals().get(0).mp());
            assertEquals(teleported.actors(),after.actors());assertThrows(SecurityException.class,()->f.teleport(0,2));
            var stable=f.view();assertEquals(after,stable);
        }
    }
    @Test void sharedCooldownBlocksMovementMeleeAndTeleportAfterMagic()throws Exception {
        try(var f=new Fixture()) {
            f.active();var accepted=f.arrow(0,1,2).snapshot();var event=accepted.damages().get(0);
            assertEquals(ModernSpellProtocol.Reason.TOO_FAST,f.teleport(0,2).reason());
            assertEquals(ModernBattleEngine.AttackStatus.TOO_FAST,f.attack(0,1,2).status());
            assertEquals(ModernBattleEngine.MoveStatus.TOO_FAST,f.move(0,1,cell(2,2),0).status());
            f.clock.addAndGet(CAST-1);assertEquals(ModernSpellProtocol.Reason.TOO_FAST,f.arrow(0,3,2).reason());
            assertEquals(event,f.view().damages().get(0));assertEquals(27,f.view().vitals().get(0).mp());
            f.clock.incrementAndGet();assertEquals(ModernSpellProtocol.Reason.NONE,f.arrow(0,4,2).reason());
        }
    }
    @Test void movementMeleeAndTeleportCooldownEachFenceMagicWithoutRestartingOnDenial()throws Exception {
        for(int prior=0;prior<3;prior++) {
            try(var f=new Fixture()) {
                f.active();long next=1,wait;
                if(prior==0){assertEquals(ModernBattleEngine.MoveStatus.MOVED,f.move(0,1,cell(2,2),0).status());wait=STEP;}
                else if(prior==1){assertEquals(ModernBattleEngine.AttackStatus.ATTACKED,f.attack(0,1,2).status());wait=ATTACK;}
                else{assertEquals(ModernSpellProtocol.Reason.NONE,f.teleport(0,1).reason());next=2;wait=CAST;}
                var before=f.view();var denied=f.arrow(0,next,2);assertEquals(ModernSpellProtocol.Reason.TOO_FAST,denied.reason());
                assertEquals(before.vitals(),denied.snapshot().vitals());assertEquals(before.actors(),denied.snapshot().actors());
                assertEquals(before.damages(),denied.snapshot().damages());
                f.clock.addAndGet(wait);assertEquals(ModernSpellProtocol.Reason.NONE,f.arrow(0,next+1,2).reason());
            }
        }
    }
    @Test void noMpAndExactExhaustionNeverProduceFreeDamageOrNegativeMp()throws Exception {
        for(long mp:new long[]{0,2,3}) {
            try(var f=new Fixture(2,map(false),List.of(cell(2,2),cell(3,2)),mp,100,learned(),true,false,bound->0,new AtomicLong(1_000_000_000L),12)) {
                var before=f.active();var first=f.arrow(0,1,2);
                assertEquals(mp==3?ModernSpellProtocol.Reason.NONE:ModernSpellProtocol.Reason.NO_MP,first.reason());
                if(mp<3){assertNoDamage(before,first.snapshot(),1);assertEquals(mp,first.snapshot().vitals().get(0).mp());}
                else {
                    assertEquals(0,first.snapshot().vitals().get(0).mp());f.clock.addAndGet(CAST);
                    var denied=f.arrow(0,2,2);assertEquals(ModernSpellProtocol.Reason.NO_MP,denied.reason());
                    assertEquals(first.snapshot().damages(),denied.snapshot().damages());assertEquals(first.snapshot().vitals(),denied.snapshot().vitals());
                }
            }
        }
    }
    @Test void unlearnedMalformedTemplateAndLegacyModeCannotAuthorizeAttackMagic()throws Exception {
        var teleport=learned().get(1);
        for(var skills:List.of(List.of(teleport),List.of(new ModernSkills.Skill(4,"$1439",4,10,ModernSkills.Support.UNSUPPORTED_TEMPLATE),teleport))) {
            try(var f=new Fixture(2,map(false),List.of(cell(2,2),cell(3,2)),30,100,skills,true,false,bound->0,new AtomicLong(1_000_000_000L),12)) {
                var before=f.active();var result=f.arrow(0,1,2);
                assertEquals(skills.size()==1?ModernSpellProtocol.Reason.UNLEARNED:ModernSpellProtocol.Reason.UNSUPPORTED,result.reason());
                assertNoDamage(before,result.snapshot(),1);
            }
        }
        try(var f=new Fixture(2,map(false),List.of(cell(2,2),cell(3,2)),30,100,learned(),false,false,bound->0,new AtomicLong(1_000_000_000L),12)) {
            var before=f.active();assertTrue(before.damages().isEmpty());var result=f.arrow(0,1,2);
            assertEquals(ModernSpellProtocol.Reason.UNSUPPORTED,result.reason());assertNoDamage(before,result.snapshot(),1);
        }
    }
    @Test void sameTeamRangeAndMapEdgesAreAuthoritativeAndDoNotStartCooldown()throws Exception {
        try(var f=new Fixture(3,map(false),List.of(cell(2,2),cell(3,2),cell(2,3)),30,100,learned(),true,false,bound->0,new AtomicLong(1_000_000_000L),12)) {
            var before=f.active();assertEquals(before.actors().get(0).member().team(),before.actors().get(2).member().team());
            var denied=f.arrow(0,1,3);assertEquals(ModernSpellProtocol.Reason.SAME_TEAM,denied.reason());assertNoDamage(before,denied.snapshot(),1);
            assertEquals(ModernSpellProtocol.Reason.NONE,f.arrow(0,2,2).reason());
        }
        try(var f=new Fixture(2,map(false),List.of(cell(2,2),cell(13,2)),30,100,learned(),true,false,bound->0,new AtomicLong(1_000_000_000L),12)) {
            var before=f.active();var denied=f.arrow(0,1,2);assertEquals(ModernSpellProtocol.Reason.OUT_OF_RANGE,denied.reason());assertNoDamage(before,denied.snapshot(),1);
        }
        try(var f=new Fixture(2,map(true),List.of(cell(2,2),cell(3,2)),30,100,learned(),true,false,bound->0,new AtomicLong(1_000_000_000L),12)) {
            var before=f.active();assertFalse(f.map.canStep(cell(2,2),cell(3,2)));
            var denied=f.arrow(0,1,2);assertEquals(ModernSpellProtocol.Reason.BLOCKED,denied.reason());assertNoDamage(before,denied.snapshot(),1);
        }
    }
    @Test void killedActorCannotCastAndDeadTargetCannotReceiveAnotherDamageEvent()throws Exception {
        try(var f=new Fixture(2,map(false),List.of(cell(2,2),cell(3,2)),30,1,learned(),true,false,bound->0,new AtomicLong(1_000_000_000L),12)) {
            f.active();var kill=f.arrow(0,1,2).snapshot();assertEquals(0,kill.vitals().get(1).hp());
            assertEquals(kill.revision(),kill.vitals().get(1).deathRevision());assertEquals(ModernBattleEngine.Phase.ACTIVE,kill.phase());
            var dead=f.arrow(1,1,1);assertEquals(ModernSpellProtocol.Reason.DEAD,dead.reason());assertEquals(30,dead.snapshot().vitals().get(1).mp());
            var again=f.arrow(0,2,2);assertEquals(ModernSpellProtocol.Reason.TARGET_DEAD,again.reason());
            assertEquals(kill.vitals(),again.snapshot().vitals());assertEquals(kill.damages(),again.snapshot().damages());
        }
    }
    @Test void genuineHighLevelMagicResistanceAllowsZeroDamageCastButNeverAFalseVictory()throws Exception {
        try(var f=new Fixture(2,map(false),List.of(cell(2,2),cell(3,2)),30,100,learned(),true,true,
                bound->0,new AtomicLong(1_000_000_000L),12,120_000)) {
            var before=f.active();
            assertEquals(120_000,f.prepared.get(1).character().level());
            assertTrue(f.prepared.get(1).fighter().magic().defense().magicResistance()>=60_000);
            var result=f.arrow(0,1,2);var after=result.snapshot();
            assertEquals(ModernSpellProtocol.Reason.NONE,result.reason());
            assertEquals(ModernBattleEngine.Phase.ACTIVE,after.phase());assertNull(after.result());
            assertEquals(before.vitals().get(1),after.vitals().get(1));
            assertEquals(27,after.vitals().get(0).mp());assertEquals(1,after.damages().get(0).castCount());
            assertEquals(1,after.casts().get(0).requestSequence());assertEquals(before.revision()+1,after.revision());
        }
    }
    @Test void lethalMagicFinishesWithSameSourceQualifiedEventAndFrozenFinalRevision()throws Exception {
        try(var f=new Fixture(2,map(false),List.of(cell(2,2),cell(3,2)),30,1,learned(),true,true,bound->0,new AtomicLong(1_000_000_000L),12)) {
            var before=f.active();var result=f.arrow(0,1,2);var finalState=result.snapshot();
            assertEquals(ModernSpellProtocol.Reason.NONE,result.reason());assertEquals(ModernBattleEngine.Phase.FINISHED,finalState.phase());
            assertEquals(before.revision()+1,finalState.revision());assertEquals(finalState.revision(),finalState.result().engineRevision());
            assertEquals(List.of(new TournamentPresence.CharacterKey(7,3333),new TournamentPresence.CharacterKey(42,3333)),
                    finalState.result().participants().stream().map(ModernMatchSettlement.Participant::key).toList());
            assertEquals(List.of(ModernMatchSettlement.Verdict.WIN,ModernMatchSettlement.Verdict.LOSS),finalState.result().participants().stream().map(ModernMatchSettlement.Participant::verdict).toList());
            assertEquals(27,finalState.vitals().get(0).mp());assertEquals(finalState.revision(),finalState.damages().get(0).castRevision());
            assertEquals(finalState,f.arrow(0,2,2).snapshot());assertEquals(finalState,f.teleport(1,1).snapshot());
            f.clock.addAndGet(TimeUnit.SECONDS.toNanos(61));assertEquals(finalState,f.engine.tick());
        }
    }
    @Test void badRngAndNumericOverflowConsumeOnlyRequestAndNeverPartialHpMpEvents()throws Exception {
        for(boolean negative:List.of(false,true)) {
            try(var f=new Fixture(2,map(false),List.of(cell(2,2),cell(3,2)),30,100,learned(),true,false,bound->negative?-1:bound,new AtomicLong(1_000_000_000L),12)) {
                var before=f.active();var reply=f.arrow(0,1,2);assertEquals(ModernSpellProtocol.Reason.NUMERIC,reply.reason());assertNoDamage(before,reply.snapshot(),1);
                assertEquals(ModernSpellProtocol.Reason.NUMERIC,f.arrow(0,2,2).reason());
            }
        }
        try(var f=new Fixture(2,map(false),List.of(cell(2,2),cell(3,2)),30,100,learned(),true,false,bound->0,new AtomicLong(1_000_000_000L),Long.MAX_VALUE/4)) {
            var before=f.active();var reply=f.arrow(0,1,2);assertEquals(ModernSpellProtocol.Reason.NUMERIC,reply.reason());assertNoDamage(before,reply.snapshot(),1);
        }
    }
    @Test void slowRngAfterDeadlineOrClockRegressionAbortsWithoutLateMpHpOrSequencePublication()throws Exception {
        for(boolean regress:List.of(false,true))for(int randomReply:List.of(0,1,2)) {
            var clock=new AtomicLong(1_000_000_000L);var calls=new AtomicLong();
            ModernPhysicalCombat.BoundedRandom random=bound->{
                if(calls.incrementAndGet()==1)clock.addAndGet(regress?-1:TimeUnit.MILLISECONDS.toNanos(LIMITS.activeTimeoutMillis()));
                return randomReply==0?0:randomReply==1?-1:bound;
            };
            try(var f=new Fixture(2,map(false),List.of(cell(2,2),cell(3,2)),30,100,learned(),true,false,random,clock,12)) {
                var before=f.active();var reply=f.arrow(0,1,2);assertEquals(ModernSpellProtocol.Reason.NOT_ACTIVE,reply.reason());
                var after=reply.snapshot();assertEquals(ModernBattleEngine.Phase.ABORTED,after.phase());
                assertEquals(regress?ModernBattleEngine.AbortReason.CLOCK_REGRESSED:ModernBattleEngine.AbortReason.ACTIVE_TIMEOUT,after.abortReason());
                assertEquals(before.revision()+1,after.revision()); // Only abort, not a preceding NUMERIC request revision.
                assertEquals(before.actors(),after.actors());assertEquals(before.vitals(),after.vitals());assertEquals(before.casts(),after.casts());assertEquals(before.damages(),after.damages());
                assertEquals(randomReply==0?2:1,calls.get());assertEquals(after,f.arrow(0,1,2).snapshot());
            }
        }
    }
    @Test void wrongScopeSourceOwnerMatchSequenceAndSelfTargetFailBeforeAnyMutation()throws Exception {
        try(var f=new Fixture()) {
            var before=f.active();var p=f.players.get(0);var scope=p.scope();
            for(var wrong:List.of(new ModernRooms.Scope(42,scope.objectId(),scope.epoch(),scope.channelId()),
                    new ModernRooms.Scope(scope.sourceId(),scope.objectId()+1,scope.epoch(),scope.channelId()),
                    new ModernRooms.Scope(scope.sourceId(),scope.objectId(),scope.epoch()+1,scope.channelId()),
                    new ModernRooms.Scope(scope.sourceId(),scope.objectId(),scope.epoch(),1)))
                assertThrows(SecurityException.class,()->f.engine.cast(p.owner(),new ModernSpellProtocol.Request(wrong,f.ticket.matchId(),f.ticket.generation(),1,4,2)));
            assertThrows(SecurityException.class,()->f.engine.cast(f.players.get(1).owner(),f.request(0,1,4,2)));
            assertThrows(SecurityException.class,()->f.engine.cast(p.owner(),new ModernSpellProtocol.Request(scope,f.ticket.matchId()+1,f.ticket.generation(),1,4,2)));
            assertThrows(SecurityException.class,()->f.arrow(0,2,2));assertThrows(SecurityException.class,()->f.arrow(0,1,1));
            assertThrows(SecurityException.class,()->f.arrow(0,1,3));assertEquals(before,f.view());
            f.arrow(0,1,2);var committed=f.view();assertThrows(SecurityException.class,()->f.arrow(0,1,2));assertEquals(committed,f.view());
        }
    }
    @Test void cancellationAndDisconnectPreventSubsequentMagicAndDoNotReleaseFalseWins()throws Exception {
        for(boolean disconnect:List.of(false,true))try(var f=new Fixture()) {
            var before=f.active();if(disconnect)f.engine.disconnect(f.players.get(1).owner());else f.rooms.rooms.releasePreparation(f.ticket);
            var reply=f.arrow(0,1,2);assertEquals(ModernSpellProtocol.Reason.NOT_ACTIVE,reply.reason());
            assertEquals(ModernBattleEngine.Phase.ABORTED,reply.snapshot().phase());assertNull(reply.snapshot().result());
            assertEquals(before.vitals(),reply.snapshot().vitals());assertEquals(before.casts(),reply.snapshot().casts());assertEquals(before.damages(),reply.snapshot().damages());
        }
    }
    @Test void preparedMagicNeedsExactFreshProfileSkillsAndUniformOptInNotAFlagAlone()throws Exception {
        try(var f=new Fixture()) {
            var p=f.prepared.get(0);var stale=new ModernApplicationProtocol.CharacterRow(p.character().objectId(),p.character().name(),p.character().characterClass(),p.character().sex(),31);
            assertThrows(IllegalArgumentException.class,()->new ModernBattleEngine.PreparedMember(p.owner(),p.scope(),p.character(),p.spawn(),STEP,p.fighter(),ATTACK,
                    new ModernSkills.Snapshot(42,p.character(),learned()),CAST,true));
            assertThrows(IllegalArgumentException.class,()->new ModernBattleEngine.PreparedMember(p.owner(),p.scope(),p.character(),p.spawn(),STEP,p.fighter(),ATTACK,
                    new ModernSkills.Snapshot(7,stale,learned()),CAST,true));
            var noMagic=ModernPhysicalCombatTest.createFighter(7,p.character(),100,10,0);
            assertThrows(IllegalArgumentException.class,()->new ModernBattleEngine.PreparedMember(p.owner(),p.scope(),p.character(),p.spawn(),STEP,noMagic,ATTACK,p.skills(),CAST,true));
        }
    }
    private static void assertNoDamage(ModernBattleEngine.Snapshot before,ModernBattleEngine.Snapshot after,long sequence) {
        assertEquals(before.actors(),after.actors());assertEquals(before.vitals(),after.vitals());assertEquals(before.damages(),after.damages());
        assertEquals(sequence,after.casts().get(0).requestSequence());assertEquals(before.casts().get(0).castCount(),after.casts().get(0).castCount());
        assertEquals(before.casts().get(0).castRevision(),after.casts().get(0).castRevision());assertEquals(before.casts().get(0).mpSpent(),after.casts().get(0).mpSpent());
    }
    private static final class Fixture implements AutoCloseable {
        final ModernRoomsTest.Fixture rooms=new ModernRoomsTest.Fixture();
        final List<ModernRoomsTest.Player> players=new ArrayList<>();
        final ModernRooms.FrozenPreparation ticket;final ModernWorldMap map;final AtomicLong clock;
        final List<ModernBattleEngine.PreparedMember> prepared;final ModernBattleEngine engine;
        Fixture()throws Exception{this(2,map(false),List.of(cell(2,2),cell(3,2)),30,100,learned(),true,false,bound->0,new AtomicLong(1_000_000_000L),12);}
        Fixture(int count,ModernWorldMap map,List<ModernWorldMap.Cell> spawns,long mp,long hp,List<ModernSkills.Skill> skills,
                boolean enabled,boolean settlement,ModernPhysicalCombat.BoundedRandom random,AtomicLong clock,long intelligence)throws Exception {
            this(count,map,spawns,mp,hp,skills,enabled,settlement,random,clock,intelligence,30);
        }
        Fixture(int count,ModernWorldMap map,List<ModernWorldMap.Cell> spawns,long mp,long hp,List<ModernSkills.Skill> skills,
                boolean enabled,boolean settlement,ModernPhysicalCombat.BoundedRandom random,AtomicLong clock,long intelligence,long defenderLevel)throws Exception {
            this.map=map;this.clock=clock;
            for(int index=0;index<count;index++)players.add(rooms.player(index==0?7:index==1?42:43,3333,index==1?defenderLevel:30,0));
            ModernRoomsTest.accepted(rooms.create(players.get(0),ModernRoomsTest.draft(count,1,514,0)));
            for(int index=1;index<count;index++)ModernRoomsTest.accepted(rooms.join(players.get(index),rooms.view(players.get(0)).joined().summary(),""));
            for(var p:players)ModernRoomsTest.accepted(rooms.act(p,ModernRooms.Action.READY,1));
            var summary=rooms.view(players.get(0)).joined().summary();var preparation=rooms.rooms.prepareWorld(players.get(0).owner(),players.get(0).scope(),summary.id(),summary.revision());
            ModernRoomsTest.accepted(preparation.result());ticket=preparation.ticket();var input=new ArrayList<ModernBattleEngine.PreparedMember>();
            for(int index=0;index<count;index++) {
                var p=ticket.participants().get(index);var member=p.member();
                var row=new ModernApplicationProtocol.CharacterRow(member.key().objectId(),member.name(),member.characterClass(),member.sex(),member.level());
                var fighter=ModernMagicCombatTest.createFighter(member.key().sourceId(),row,hp,mp,intelligence,0);
                input.add(new ModernBattleEngine.PreparedMember(p.owner(),p.scope(),row,spawns.get(index),STEP,fighter,ATTACK,
                        new ModernSkills.Snapshot(member.key().sourceId(),row,skills),CAST,enabled));
            }
            prepared=List.copyOf(input);engine=new ModernBattleEngine(ticket,new ModernWorldBinding(1,514,map),prepared,LIMITS,clock::get,()->1000L,random,settlement);
        }
        ModernBattleEngine.Snapshot view(){var p=players.get(0);return engine.snapshot(p.owner(),p.scope(),ticket.matchId(),ticket.generation());}
        ModernBattleEngine.Snapshot active(){for(var p:players)engine.ackLoaded(p.owner(),p.scope(),ticket.matchId(),ticket.generation(),map.digest());
            var snapshot=engine.tick();assertEquals(ModernBattleEngine.Phase.ACTIVE,snapshot.phase());return snapshot;}
        ModernSpellProtocol.Request request(int index,long seq,int skill,long target){return new ModernSpellProtocol.Request(players.get(index).scope(),ticket.matchId(),ticket.generation(),seq,skill,target);}
        ModernBattleEngine.CastResult arrow(int index,long seq,long target){return engine.cast(players.get(index).owner(),request(index,seq,4,target));}
        ModernBattleEngine.CastResult teleport(int index,long seq){return engine.cast(players.get(index).owner(),request(index,seq,5,0));}
        ModernBattleEngine.MoveResult move(int index,long seq,ModernWorldMap.Cell from,int heading){var p=players.get(index);return engine.move(p.owner(),p.scope(),ticket.matchId(),ticket.generation(),seq,from.x(),from.y(),heading);}
        ModernBattleEngine.AttackResult attack(int index,long seq,long target){var p=players.get(index);return engine.attack(p.owner(),p.scope(),ticket.matchId(),ticket.generation(),seq,target);}
        public void close(){engine.close();rooms.rooms.releasePreparation(ticket);for(var p:players){rooms.rooms.disconnect(p.owner());rooms.presence.disconnect(p.owner());}}
    }
}
