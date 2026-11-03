package local.lito381.server;

import static org.junit.jupiter.api.Assertions.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;

/** Real 15-v-15 Rooms tickets (two source-qualified humans plus 28 typed
 * server BOTs), actual combat calculator, shared-map engine and fake clocks. */
class ModernBattleEngineSiegeTest {
    static final long SECOND=1_000_000_000L;
    static ModernWorldMap.Cell cell(int x,int y){return ModernSiegeTest.cell(x,y);}
    static final class Fixture implements AutoCloseable {
        final TournamentPresence presence=new TournamentPresence();
        final ModernWorldBinding binding;final ModernRooms rooms;final ModernSiege.Config config;
        final List<ModernBattleEngineBotTest.Player> players=new ArrayList<>();
        final ModernRooms.FrozenPreparation ticket;final ModernBattleEngine engine;
        final AtomicLong clock=new AtomicLong(SECOND),epoch=new AtomicLong(1000);long jump;boolean cancel,invalid,hitSlow;int draws;
        Fixture()throws Exception{this(0,1,60_000,1000,0,true);}
        Fixture(int attacker,long towerHp,int duration,long delay,long countdown,boolean regen)throws Exception {
            this(attacker,towerHp,duration,delay,countdown,regen,15);
        }
        Fixture(int attacker,long towerHp,int duration,long delay,long countdown,boolean regen,int perTeam)throws Exception {
            this(attacker,towerHp,duration,delay,countdown,regen,perTeam,false);
        }
        Fixture(int attacker,long towerHp,int duration,long delay,long countdown,boolean regen,int perTeam,boolean slow)throws Exception {
            binding=ModernSiegeTest.binding(547);rooms=new ModernRooms(Set.of(binding.key()),Set.of(binding.key()));
            var original=ModernSiegeTest.config(547,towerHp,duration,delay);
            var attack=new ArrayList<ModernWorldMap.Cell>();var defend=new ArrayList<ModernWorldMap.Cell>();
            attack.add(cell(10,10));defend.add(cell(31,10));
            for(int i=0;i<perTeam-1;i++){attack.add(cell(2+i%7,20+i/7));defend.add(cell(40+i%7,20+i/7));}
            config=new ModernSiege.Config(original.key(),original.towers(),original.gate(),attack,defend,original.portal(),duration,delay);
            var prepared=new ArrayList<ModernBattleEngine.PreparedMember>();
            for(int i=0;i<2;i++) {
                int source=i==0?7:42;var owner=presence.connect();var row=new ModernApplicationProtocol.CharacterRow(3333,"human-"+i,1,0,80);
                var selection=new TournamentPresence.Selection(new TournamentPresence.CharacterKey(source,3333),"source-"+source,row.name(),row.level(),row.characterClass(),row.sex());
                assertEquals(TournamentPresence.SelectStatus.ACCEPTED,presence.select(owner,selection));rooms.enterLobby(owner,selection,1,0,true);
                var scope=new ModernRooms.Scope(source,3333,1,0);players.add(new ModernBattleEngineBotTest.Player(owner,scope,row));
                var summary=i==0?null:view().summary();
                ModernRoomsTest.accepted(rooms.execute(owner,i==0?new ModernRooms.Command(scope,ModernRooms.Action.CREATE,0,0,0,ModernRoomsTest.draft(100,3,547,0),"")
                        :new ModernRooms.Command(scope,ModernRooms.Action.JOIN,summary.id(),summary.revision(),0,null,"")));
                var input=ModernBattleEngineHonorTest.source(source,row,i==attacker?100:1,100);
                if(slow)input=ModernBattleNpcSlowTest.flags(input);
                var fighter=ModernPhysicalCombatTest.fighter(input);
                var member=new ModernBattleEngine.PreparedMember(owner,scope,row,config.camp(i,attacker).get(0),200_000_000,fighter,700_000_000,
                        new ModernSkills.Snapshot(source,row,List.of(new ModernSkills.Skill(5,"$1440",10,10,ModernSkills.Support.TELEPORT))),1_200_000_000,false,
                        List.of(),ModernInventoryWeight.qualify(fighter.profile(),100),slow,false,regen,false);
                prepared.add(slow?ModernBattleSlowTest.enable(member,4):member);
            }
            var gm=players.get(0);var s=view().summary();
            var grant=new ModernGmCommands.Snapshot(gm.scope().sourceId(),"fixture",gm.row(),200,false,false,new ModernGmCommands.Definition("de",200,"L1SpawnDe",false));
            int count=perTeam*2-2;var botsAdded=rooms.spawnBots(gm.owner(),gm.scope(),s.id(),s.revision(),count,grant);ModernRoomsTest.accepted(botsAdded.result());assertEquals(count,botsAdded.added());
            for(var p:players){s=view().summary();ModernRoomsTest.accepted(rooms.execute(p.owner(),new ModernRooms.Command(p.scope(),ModernRooms.Action.READY,s.id(),s.revision(),1,null,"")));}
            s=view().summary();assertTrue(s.readySatisfied());var ready=rooms.prepareWorld(gm.owner(),gm.scope(),s.id(),s.revision());ModernRoomsTest.accepted(ready.result());ticket=ready.ticket();
            var bots=new ArrayList<ModernBattleEngine.BotPrepared>();int[] indices={1,1};
            for(var m:ticket.bots())bots.add(new ModernBattleEngine.BotPrepared(m.identity(),ModernBotTemplate.roomTestDummy(m.characterClass(),m.sex()),config.camp(m.team(),attacker).get(indices[m.team()]++)));
            engine=new ModernBattleEngine(ticket,binding,prepared,bots,new ModernBattleEngine.Limits(2000,countdown,duration),clock::get,epoch::get,b->{
                draws++;if(jump!=0){clock.addAndGet(jump);jump=0;}if(cancel){rooms.releasePreparation(ticket);cancel=false;}return invalid?-1:hitSlow?0:b-1;
            },false,null,()->false,null,null,config,attacker);
        }
        ModernRooms.RoomSnapshot view(){var p=players.get(0);return rooms.execute(p.owner(),new ModernRooms.Command(p.scope(),ModernRooms.Action.READ,0,0,0,null,"")).view().joined();}
        ModernBattleEngine.Snapshot active(){for(var p:players)engine.ackLoaded(p.owner(),p.scope(),ticket.matchId(),ticket.generation(),binding.map().digest());return engine.tick();}
        ModernBattleEngine.Snapshot tick(){return engine.tick();}
        void waitMillis(long millis){clock.addAndGet(millis*1_000_000);}
        ModernBattleEngine.AttackResult tower(int who,long sequence,ModernSiege.Slot slot){var p=players.get(who);return engine.attackSiegeTower(p.owner(),p.scope(),ticket.matchId(),ticket.generation(),sequence,new ModernSiege.Target(new ModernSiege.Scope(ticket.snapshot().summary().id(),ticket.matchId(),ticket.generation()),slot));}
        ModernBattleEngine.AttackResult attack(int who,long seq,long target){var p=players.get(who);return engine.attack(p.owner(),p.scope(),ticket.matchId(),ticket.generation(),seq,target);}
        ModernBattleEngine.MoveResult step(int who,int heading){var p=players.get(who);var a=tick().actors().get(who);return engine.move(p.owner(),p.scope(),ticket.matchId(),ticket.generation(),a.lastMoveSequence()+1,a.position().x(),a.position().y(),heading);}
        void walk(int who,ModernWorldMap.Cell destination){
            for(int n=0;n<100;n++){var from=tick().actors().get(who).position();if(from.equals(destination))return;
                waitMillis(1200);int dx=Integer.compare(destination.x(),from.x()),dy=Integer.compare(destination.y(),from.y());
                int heading=dx==0?(dy<0?0:4):dy==0?(dx>0?2:6):dx>0?(dy<0?1:3):(dy<0?7:5);
                assertEquals(ModernBattleEngine.MoveStatus.MOVED,step(who,heading).status());}
            fail("walk did not arrive");
        }
        ModernBattleEngine.SiegePortalResult portal(int who){var p=players.get(who);return engine.useSiegePortal(p.owner(),p.scope(),ticket.matchId(),ticket.generation(),tick().actors().get(who).lastMoveSequence()+1);}
        ModernBattleEngine.CastResult teleport(int who,long sequence){var p=players.get(who);return engine.cast(p.owner(),new ModernSpellProtocol.Request(p.scope(),ticket.matchId(),ticket.generation(),sequence,5,0));}
        @Override public void close(){engine.close();rooms.releasePreparation(ticket);for(var p:players){rooms.disconnect(p.owner());presence.disconnect(p.owner());}assertEquals(0,rooms.memberCount());}
    }
    @Test void actualThirtyMemberTicketAndBothHumanResourceAcksAreRequired()throws Exception {
        try(var f=new Fixture()) {
            var start=f.tick();assertEquals(30,start.actors().size());assertEquals(2,f.engine.ownerCount());assertEquals(28,start.actors().stream().filter(a->a.loaded()).count());
            assertEquals(15,start.actors().stream().filter(a->a.member().team()==0).count());assertEquals(ModernSiege.Phase.WAITING,start.siege().state().phase());
            var p=f.players.get(0);f.engine.ackLoaded(p.owner(),p.scope(),f.ticket.matchId(),f.ticket.generation(),f.binding.map().digest());assertEquals(ModernBattleEngine.Phase.LOADING,f.tick().phase());
            assertEquals(ModernBattleEngine.Phase.ACTIVE,f.active().phase());assertEquals(ModernSiege.Phase.OUTER,f.tick().siege().state().phase());
            assertFalse(f.tick().honor().enabled());assertNull(f.tick().result());
        }
    }
    @Test void outerTowersOpenRealGateAndInnerWinDoesNotRequireKillingDefenders()throws Exception {
        for(int attacker:List.of(0,1))try(var f=new Fixture(attacker,1,120_000,1000,0,true)) {
            f.active();assertEquals(ModernBattleEngine.AttackStatus.BLOCKED,f.tower(attacker,1,ModernSiege.Slot.INNER).status());
            assertEquals(ModernBattleEngine.AttackStatus.ATTACKED,f.tower(attacker,2,ModernSiege.Slot.OUTER_LEFT).status());assertFalse(f.tick().siege().state().open());
            f.walk(attacker,cell(11,10));f.waitMillis(200);
            assertEquals(ModernBattleEngine.AttackStatus.ATTACKED,f.tower(attacker,3,ModernSiege.Slot.OUTER_RIGHT).status());assertTrue(f.tick().siege().state().open());
            f.walk(attacker,cell(29,10));f.walk(attacker,cell(29,9));f.walk(attacker,cell(31,9));f.waitMillis(200);var done=f.tower(attacker,4,ModernSiege.Slot.INNER).snapshot();
            assertEquals(ModernBattleEngine.Phase.FINISHED,done.phase());assertEquals(attacker,done.siege().state().winner());
            assertTrue(done.vitals().stream().allMatch(v->v.hp()>0));assertEquals(done.revision(),done.siege().state().finishRevision());
            assertEquals(0,done.vitals().get(attacker).attackRevision());assertEquals(4,done.vitals().get(attacker).attackSequence());
            assertNull(done.result()); // No invented SQL event or generic deathmatch victory.
            assertEquals(done,f.tower(attacker,5,ModernSiege.Slot.INNER).snapshot());assertEquals(done,f.tick());
            f.rooms.releasePreparation(f.ticket);assertEquals(done,f.tick());
        }
    }
    @Test void closedGateDeniesMovementDiagonalAndRandomTeleportAcrossTheWall()throws Exception {
        try(var f=new Fixture()) {
            f.active();f.walk(0,cell(29,10));f.walk(0,cell(29,9));f.waitMillis(1200);
            assertEquals(ModernBattleEngine.MoveStatus.BLOCKED,f.step(0,2).status());
            assertEquals(ModernBattleEngine.MoveStatus.BLOCKED,f.step(0,3).status());
            var before=f.tick();var tele=f.teleport(0,1);assertEquals(ModernSpellProtocol.Reason.NONE,tele.reason());
            var pos=tele.snapshot().actors().get(0).position();assertTrue(pos.x()<cell(30,0).x());
            assertEquals(before.vitals().get(0).mp()-10,tele.snapshot().vitals().get(0).mp());assertFalse(f.config.towerCells().contains(pos));
        }
    }
    @Test void defenderPortalUsesServerEndpointsMovementSequenceAndCooldown()throws Exception {
        try(var f=new Fixture()) {
            f.active();assertEquals(ModernSiege.PortalReason.ATTACKER,f.portal(0).reason());assertEquals(ModernSiege.PortalReason.NOT_AT_PORTAL,f.portal(1).reason());
            f.walk(1,cell(31,11));assertEquals(ModernSiege.PortalReason.TOO_FAST,f.portal(1).reason());f.waitMillis(200);
            var crossed=f.portal(1);assertEquals(ModernSiege.PortalReason.MOVED,crossed.reason());assertEquals(cell(29,10),crossed.snapshot().actors().get(1).position());
            assertEquals(ModernSiege.PortalReason.TOO_FAST,f.portal(1).reason());f.waitMillis(200);
            assertEquals(ModernSiege.PortalReason.MOVED,f.portal(1).reason());assertEquals(cell(31,11),f.tick().actors().get(1).position());
            var p=f.players.get(1);assertThrows(SecurityException.class,()->f.engine.useSiegePortal(p.owner(),p.scope(),f.ticket.matchId(),f.ticket.generation(),1));
        }
    }
    @Test void pvpDeathRespawnsAtRoleCampWithoutEliminationMpRefillOrSequenceReset()throws Exception {
        for(boolean regen:List.of(false,true))try(var f=new Fixture(0,1,120_000,1234,0,regen)) {
            f.active();f.walk(1,cell(31,11));f.waitMillis(200);assertEquals(ModernSiege.PortalReason.MOVED,f.portal(1).reason());
            f.walk(1,cell(11,10));f.waitMillis(200);var dead=f.attack(0,1,2).snapshot();
            assertEquals(0,dead.vitals().get(1).hp());assertEquals(ModernBattleEngine.Phase.ACTIVE,dead.phase());assertEquals(1,dead.siege().lives().get(1).deaths());
            long sequence=dead.actors().get(1).lastMoveSequence(),mp=dead.vitals().get(1).mp(),revision=dead.revision();
            f.waitMillis(1233);assertEquals(0,f.tick().vitals().get(1).hp());f.waitMillis(1);var alive=f.tick();
            assertEquals(cell(31,10),alive.actors().get(1).position());assertEquals(1,alive.vitals().get(1).hp());assertEquals(mp,alive.vitals().get(1).mp());
            assertEquals(sequence,alive.actors().get(1).lastMoveSequence());assertEquals(revision,alive.vitals().get(1).deathRevision());
            assertEquals(1,alive.siege().lives().get(1).hpGain());assertEquals(1,alive.siege().lives().get(1).respawns());assertEquals(alive,f.tick());
        }
    }
    @Test void timeoutIsDefenderVictoryEvenWhenFirstActiveTickArrivesAfterWholeDeadline()throws Exception {
        try(var f=new Fixture(1,1,50,1000,SECOND,true)) {
            assertEquals(ModernBattleEngine.Phase.COUNTDOWN,f.active().phase());f.waitMillis(1050);var done=f.tick();
            assertEquals(ModernBattleEngine.Phase.FINISHED,done.phase());assertEquals(0,done.siege().state().winner());assertEquals(50,done.siege().state().finishedAtMillis());
            assertEquals(ModernBattleEngine.AbortReason.NONE,done.abortReason());assertEquals(done,f.tick());
        }
    }
    @Test void slowResolverCannotDamageTowerAfterDeadlineOrCancellation()throws Exception {
        try(var f=new Fixture(0,100,50,1000,0,true)) {
            f.active();f.jump=50_000_000;var result=f.tower(0,1,ModernSiege.Slot.OUTER_LEFT);
            assertEquals(ModernBattleEngine.AttackStatus.NOT_ACTIVE,result.status());assertEquals(1,result.snapshot().siege().state().winner());
            assertEquals(100,result.snapshot().siege().state().objectives().get(0).hp());assertEquals(0,result.snapshot().vitals().get(0).attackSequence());
        }
        try(var f=new Fixture()) {
            f.active();f.cancel=true;var result=f.tower(0,1,ModernSiege.Slot.OUTER_LEFT);
            assertEquals(ModernBattleEngine.Phase.ABORTED,result.snapshot().phase());assertEquals(ModernSiege.Phase.ABORTED,result.snapshot().siege().state().phase());
            assertEquals(-1,result.snapshot().siege().state().winner());assertEquals(1,result.snapshot().siege().state().objectives().get(0).hp());
        }
    }
    @Test void invalidRandomStaleRequestWrongScopeAndDefenderAttackHaveNoTowerDamage()throws Exception {
        try(var f=new Fixture()) {
            f.active();f.invalid=true;assertEquals(ModernBattleEngine.AttackStatus.NUMERIC,f.tower(0,1,ModernSiege.Slot.OUTER_LEFT).status());f.invalid=false;
            assertThrows(SecurityException.class,()->f.tower(0,1,ModernSiege.Slot.OUTER_LEFT));
            assertEquals(ModernBattleEngine.AttackStatus.SAME_TEAM,f.tower(1,1,ModernSiege.Slot.OUTER_LEFT).status());
            var p=f.players.get(0);var state=f.tick().siege().state();var target=new ModernSiege.Target(new ModernSiege.Scope(state.scope().roomId(),state.scope().matchId(),state.scope().generation()+1),ModernSiege.Slot.OUTER_LEFT);
            assertThrows(SecurityException.class,()->f.engine.attackSiegeTower(p.owner(),p.scope(),f.ticket.matchId(),f.ticket.generation(),2,target));
            assertEquals(1,f.tick().siege().state().objectives().get(0).hp());assertEquals(0,f.tick().siege().state().objectives().get(0).hitCount());
        }
    }
    @Test void oldWorldProjectionIsExplicitlyRefusedRatherThanLosingObjectivesOrResult()throws Exception {
        try(var f=new Fixture()){var snapshot=f.active();assertThrows(IllegalArgumentException.class,()->ModernWorldService.fromEngine(snapshot,snapshot.revision()+1,null));}
    }
    @Test void missingAckOrRegressedClockAbortsWithoutWinOrSqlEvent()throws Exception {
        try(var f=new Fixture()){f.waitMillis(2000);var done=f.tick();assertEquals(ModernBattleEngine.AbortReason.LOAD_TIMEOUT,done.abortReason());assertEquals(-1,done.siege().state().winner());assertNull(done.result());}
        try(var f=new Fixture()){f.active();f.clock.decrementAndGet();var done=f.tick();assertEquals(ModernBattleEngine.AbortReason.CLOCK_REGRESSED,done.abortReason());assertEquals(-1,done.siege().state().winner());}
    }
    @Test void fullFiftyPerForceRosterKeepsBotsTypedAndTimeoutDoesNotInventCharacterRows()throws Exception {
        try(var f=new Fixture(0,1,50,1000,0,true,50)){
            var active=f.active();assertEquals(100,active.actors().size());assertEquals(98,active.actors().stream().filter(a->!a.member().identity().isHuman()).count());
            assertEquals(50,active.actors().stream().filter(a->a.member().team()==0).count());assertEquals(2,f.engine.ownerCount());
            f.waitMillis(50);var done=f.tick();assertEquals(1,done.siege().state().winner());assertEquals(100,done.siege().lives().size());assertNull(done.result());
        }
    }
    @Test void occupiedPortalDoesNotOverlapActorsOrSpendMovementCooldown()throws Exception {
        try(var f=new Fixture()){
            f.active();f.walk(0,cell(29,10));f.walk(1,cell(31,11));f.waitMillis(200);
            var before=f.tick();var denied=f.portal(1);assertEquals(ModernSiege.PortalReason.OCCUPIED,denied.reason());
            assertEquals(before.actors().get(1).position(),denied.snapshot().actors().get(1).position());
            assertEquals(before.actors().get(1).lastMoveSequence()+1,denied.snapshot().actors().get(1).lastMoveSequence());
            f.walk(0,cell(28,10));assertEquals(ModernSiege.PortalReason.MOVED,f.portal(1).reason());
        }
    }
}
