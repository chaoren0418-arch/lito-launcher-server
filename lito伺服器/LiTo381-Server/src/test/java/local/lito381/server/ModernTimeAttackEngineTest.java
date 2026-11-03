package local.lito381.server;

import static org.junit.jupiter.api.Assertions.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;

/** Real room ticket + shared combat/NPC engine. The synthetic pattern is NOT
 * recovered map540 placement, a production boss catalog or a wire/UI test. */
class ModernTimeAttackEngineTest {
    static ModernWorldMap.Cell cell(int x,int y){return ModernNpcRosterTest.cell(x,y);}
    static final class Fixture implements AutoCloseable {
        final TournamentPresence presence=new TournamentPresence();
        final ModernWorldBinding binding;final ModernRooms rooms;final ModernNpcCatalog.Catalog catalog;
        final List<ModernBattleEngineBotTest.Player> players=new ArrayList<>();
        final List<ModernBattleEngine.PreparedMember> prepared=new ArrayList<>();
        final ModernRooms.FrozenPreparation ticket;final ModernTimeAttack.Plan plan;
        final AtomicLong clock=new AtomicLong(1_000_000_000),epoch=new AtomicLong(1000),draws=new AtomicLong();
        final ModernBattleEngine engine;Runnable duringRandom;long random=19;
        Fixture(int humans,int capacity,long hp,boolean dragon)throws Exception {
            this(humans,capacity,hp,dragon,1);
        }
        Fixture(int humans,int capacity,long hp,boolean dragon,long towerHp)throws Exception {
            this(humans,capacity,hp,dragon,towerHp,false);
        }
        Fixture(int humans,int capacity,long hp,boolean dragon,long towerHp,boolean disintegrate)throws Exception {
            this(humans,capacity,hp,dragon,towerHp,disintegrate,false);
        }
        Fixture(int humans,int capacity,long hp,boolean dragon,long towerHp,boolean disintegrate,boolean lightArrow)throws Exception {
            this(humans,capacity,hp,dragon,towerHp,disintegrate,lightArrow,false,1);
        }
        Fixture(int humans,int capacity,long hp,boolean dragon,long towerHp,boolean disintegrate,boolean lightArrow,boolean fireStorm,int victims)throws Exception {
            this(humans,capacity,hp,dragon,towerHp,disintegrate,lightArrow,fireStorm,victims,false);
        }
        Fixture(int humans,int capacity,long hp,boolean dragon,long towerHp,boolean disintegrate,boolean lightArrow,boolean fireStorm,int victims,boolean haste)throws Exception {
            this(humans,capacity,hp,dragon,towerHp,disintegrate,lightArrow,fireStorm,victims,haste,false);
        }
        Fixture(int humans,int capacity,long hp,boolean dragon,long towerHp,boolean disintegrate,boolean lightArrow,boolean fireStorm,int victims,boolean haste,boolean slow)throws Exception {
            this(humans,capacity,hp,dragon,towerHp,disintegrate,lightArrow,fireStorm,victims,haste,slow,slow?60_000:20_000);
        }
        Fixture(int humans,int capacity,long hp,boolean dragon,long towerHp,boolean disintegrate,boolean lightArrow,boolean fireStorm,int victims,boolean haste,boolean slow,int durationMillis)throws Exception {
            this(humans,capacity,hp,dragon,towerHp,disintegrate,lightArrow,fireStorm,victims,haste,slow,durationMillis,java.util.function.UnaryOperator.identity());
        }
        Fixture(int humans,int capacity,long hp,boolean dragon,long towerHp,boolean disintegrate,boolean lightArrow,boolean fireStorm,int victims,boolean haste,boolean slow,int durationMillis,
                java.util.function.UnaryOperator<ModernBattleEngine.PreparedMember> prepare)throws Exception {
            if(slow&&!haste)throw new IllegalArgumentException("slow fixture requires haste-qualified magic source");
            if(haste&&!lightArrow)throw new IllegalArgumentException("haste fixture requires qualified magic source");
            if(victims<1||victims>2)throw new IllegalArgumentException("test wave size");
            var map=ModernWorldMap.fromSectorBytes(554,Map.of(ModernWorldMapTest.KEY,ModernWorldMapTest.openSectorBytes()),()->false);
            binding=new ModernWorldBinding(4,540,map);rooms=new ModernRooms(Set.of(binding.key()));
            catalog=ModernNpcMeleeTest.catalog(Map.of(ModernNpcCatalog.Field.HP,1L),true,0);
            var t=catalog.npcs().get(ModernNpcMeleeTest.NPC).rosterTemplate();
            var waves=new ArrayList<ModernTimeAttack.Wave>();
            waves.add(new ModernTimeAttack.Wave(ModernTimeAttack.Kind.ORDINARY,List.of(new ModernNpcRoster.Spawn(t,0,1,cell(11,10),2))));
            waves.add(new ModernTimeAttack.Wave(ModernTimeAttack.Kind.ORDINARY,List.of(new ModernNpcRoster.Spawn(t,0,1,cell(11,10),2))));
            waves.add(new ModernTimeAttack.Wave(ModernTimeAttack.Kind.FINAL_BOSSES,List.of(new ModernNpcRoster.Spawn(t,0,2,cell(11,10),2))));
            if(dragon)waves.add(new ModernTimeAttack.Wave(ModernTimeAttack.Kind.DRAGON,List.of(new ModernNpcRoster.Spawn(t,0,3,cell(11,10),2))));
            if(victims==2)for(int i=0;i<waves.size();i++) {
                var wave=waves.get(i);var spawns=new ArrayList<>(wave.spawns());
                spawns.add(new ModernNpcRoster.Spawn(t,0,wave.kind().ordinal()+1,cell(10,9),2));
                waves.set(i,new ModernTimeAttack.Wave(wave.kind(),spawns));
            }
            plan=new ModernTimeAttack.Plan(binding.key(),new ModernTimeAttack.Tower(cell(11,11),2665,towerHp),waves,durationMillis);
            var blessings=new java.util.HashMap<ModernParticipantId,ModernNpcMelee.BlessingBinding>();
            for(int i=0;i<humans;i++) {
                var owner=presence.connect();int source=i==0?7:42;
                var row=new ModernApplicationProtocol.CharacterRow(3333,"player-"+i,1,0,80);
                var selection=new TournamentPresence.Selection(new TournamentPresence.CharacterKey(source,3333),"server-"+source,row.name(),80,1,0);
                assertEquals(TournamentPresence.SelectStatus.ACCEPTED,presence.select(owner,selection));rooms.enterLobby(owner,selection,1,0,true);
                var p=new ModernBattleEngineBotTest.Player(owner,new ModernRooms.Scope(source,3333,1,0),row);players.add(p);
                var s=i==0?null:room().summary();
                var c=i==0?new ModernRooms.Command(p.scope(),ModernRooms.Action.CREATE,0,0,0,ModernRoomsTest.draft(60,4,540,0),""):
                        new ModernRooms.Command(p.scope(),ModernRooms.Action.JOIN,s.id(),s.revision(),0,null,"");
                ModernRoomsTest.accepted(rooms.execute(owner,c));
                var input=disintegrate||lightArrow||fireStorm?ModernBattleEngineNpcDisintegrateTest.source(ModernNaturalRegenTest.source(source,row,hp,1000,0,0,225),1000,500):
                        ModernPhysicalCombatTest.fixtureSource(source,row,hp,10,1);
                if(slow)input=ModernBattleNpcSlowTest.flags(input);
                var fighter=ModernPhysicalCombat.qualify(input,ModernCombatProfileTest.binding(input.input().equipment()),Optional.empty(),1000);
                var member=disintegrate||lightArrow||fireStorm?new ModernBattleEngine.PreparedMember(owner,p.scope(),row,cell(10,10+i),200_000_000,fighter,700_000_000,
                        null,0,false,List.of(),ModernInventoryWeight.qualify(fighter.profile(),100),false,false,true):
                        new ModernBattleEngine.PreparedMember(owner,p.scope(),row,cell(10,10+i),200_000_000,fighter,700_000_000);
                if(lightArrow&&!disintegrate)member=new ModernBattleEngine.PreparedMember(member.owner(),member.scope(),member.character(),member.spawn(),member.stepIntervalNanos(),member.fighter(),member.attackIntervalNanos(),
                        new ModernSkills.Snapshot(source,row,List.of(new ModernSkills.Skill(4,"$1439",3,10,ModernSkills.Support.LIGHT_ARROW),ModernSkillsTest.teleport())),
                        1_200_000_000,false,member.matchItems(),member.weightBasis(),false,false,true);
                member=fireStorm?ModernBattleEngineNpcFireStormTest.enable(member,true,disintegrate):
                        disintegrate?ModernBattleEngineNpcDisintegrateTest.enable(member,ModernDisintegrateCatalog.Status.QUALIFIED):member;
                if(haste)member=ModernBattleHasteTest.enable(new ModernBattleEngine.PreparedMember(member.owner(),member.scope(),member.character(),member.spawn(),
                        member.stepIntervalNanos(),member.fighter(),member.attackIntervalNanos(),member.skills(),member.castIntervalNanos(),member.damageMagic(),
                        member.matchItems(),member.weightBasis(),true,member.bowEnabled(),member.regenEnabled(),member.fireStormEnabled(),member.disintegrate(),
                        member.npcDisintegrate(),member.greaterHaste()),ModernHasteCatalog.Status.QUALIFIED);
                if(slow)member=ModernBattleSlowTest.enable(member,8);
                prepared.add(prepare.apply(member));
                blessings.put(ModernParticipantId.human(source,3333),new ModernNpcMelee.BlessingBinding(source,fighter.catalogDatabase(),ModernNpcMelee.BlessingConvention.INTERNAL_ONE_BLESSED));
            }
            for(var p:players){var s=room().summary();ModernRoomsTest.accepted(rooms.execute(p.owner(),new ModernRooms.Command(p.scope(),ModernRooms.Action.READY,s.id(),s.revision(),1,null,"")));}
            var owner=players.get(0);var s=room().summary();var capture=rooms.prepareWorld(owner.owner(),owner.scope(),s.id(),s.revision());ModernRoomsTest.accepted(capture.result());ticket=capture.ticket();
            var config=new ModernNpcBattle.Config(binding.key(),catalog,blessings,List.of(cell(10,10)),capacity,1000,null,lightArrow,plan).withDisintegrate(disintegrate).withFireStorm(fireStorm);
            engine=new ModernBattleEngine(ticket,binding,prepared,List.of(),new ModernBattleEngine.Limits(2000,500_000_000,60_000),clock::get,epoch::get,
                    bound->{draws.incrementAndGet();if(duringRandom!=null){var r=duringRandom;duringRandom=null;r.run();}return Math.min(random,bound-1);},false,null,()->false,null,config);
        }
        ModernRooms.RoomSnapshot room(){var p=players.get(0);return rooms.execute(p.owner(),new ModernRooms.Command(p.scope(),ModernRooms.Action.READ,0,0,0,null,"")).view().joined();}
        ModernNpcBattle.Frame view(){for(var p:players)try{return engine.npcSnapshot(p.owner(),p.scope(),ticket.matchId(),ticket.generation());}catch(SecurityException retired){}throw new AssertionError("no remaining admitted participant");}
        void elapsed(long millis){clock.set(1_500_000_000L+millis*1_000_000L);}
        void active(){for(var p:players)engine.ackLoaded(p.owner(),p.scope(),ticket.matchId(),ticket.generation(),binding.map().digest());assertEquals(ModernBattleEngine.Phase.COUNTDOWN,view().world().phase());elapsed(0);assertEquals(ModernBattleEngine.Phase.ACTIVE,view().world().phase());}
        ModernNpcBattle.Result tower(int index,long seq){var p=players.get(index);return engine.attackTimeAttackTower(p.owner(),p.scope(),ticket.matchId(),ticket.generation(),seq,new ModernTimeAttack.TowerId(new ModernNpcRoster.Scope(room().summary().id(),ticket.matchId(),ticket.generation())));}
        ModernNpcBattle.Result hit(int index,long seq,ModernNpcRoster.Actor target){var p=players.get(index);return engine.attackNpc(p.owner(),p.scope(),ticket.matchId(),ticket.generation(),seq,target.id(),target.life());}
        ModernBattleEngine.MoveResult move(int index,long seq,int x,int y,int heading){var p=players.get(index);var from=cell(x,y);return engine.move(p.owner(),p.scope(),ticket.matchId(),ticket.generation(),seq,from.x(),from.y(),heading);}
        ModernNpcRoster.Actor living(){return view().npcs().actors().stream().filter(ModernNpcRoster.Actor::alive).findFirst().orElseThrow();}
        @Override public void close(){engine.close();rooms.releasePreparation(ticket);for(var p:players){rooms.disconnect(p.owner());presence.disconnect(p.owner());}}
    }
    @Test void towerStartsClockAndActualKillsAdvanceOrderedPatternIncludingExplicitDragon()throws Exception {
        for(boolean dragon:List.of(false,true))try(var f=new Fixture(1,8,100,dragon)) {
            assertEquals(ModernTimeAttack.Phase.WAITING_FOR_TOWER,f.view().world().timeAttack().state().phase());f.active();f.elapsed(15_000);
            assertTrue(f.view().npcs().actors().isEmpty());assertEquals(-1,f.view().world().timeAttack().state().startedMillis());
            assertEquals(ModernNpcBattle.Reason.ATTACKED,f.tower(0,1).reason());assertEquals(15_000,f.view().world().timeAttack().state().startedMillis());
            assertThrows(IllegalStateException.class,()->f.engine.spawnNpcs(f.living().id().scope(),f.plan.waves().get(0).spawns()));
            long serial=0;for(int wave=0;wave<f.plan.waves().size();wave++) {
                f.elapsed(16_000+wave*1000);var npc=f.living();assertTrue(npc.id().serial()>serial);serial=npc.id().serial();
                assertEquals(f.plan.waves().get(wave).kind().ordinal()+1,npc.stage());assertEquals(ModernNpcBattle.Reason.ATTACKED,f.hit(0,wave+2,npc).reason());
            }
            var world=f.view().world();assertEquals(ModernBattleEngine.Phase.FINISHED,world.phase());assertEquals(ModernTimeAttack.End.CLEARED,world.timeAttack().state().end());
            assertEquals(ModernMatchSettlement.Verdict.WIN,world.timeAttack().outcomes(world).get(0).verdict());assertNull(world.result(),"no two-team Quest/PvP result alias");
            assertThrows(IllegalArgumentException.class,()->ModernWorldService.fromEngine(world,world.revision(),null));
            assertEquals(ModernNpcBattle.Reason.NOT_ACTIVE,f.tower(0,99).reason());f.elapsed(100_000);assertEquals(world,f.view().world());
        }
    }
    @Test void waitingSafetyIsAbortButRealEncounterDeadlineIsCooperativeLoss()throws Exception {
        try(var f=new Fixture(1,8,100,false)) {
            f.active();f.elapsed(60_000);var w=f.view().world();assertEquals(ModernBattleEngine.Phase.ABORTED,w.phase());assertTrue(w.timeAttack().outcomes(w).isEmpty());
        }
        try(var f=new Fixture(2,8,100,false)) {
            f.active();f.elapsed(50_000);f.tower(0,1);f.elapsed(69_999);assertEquals(ModernBattleEngine.Phase.ACTIVE,f.view().world().phase());
            f.elapsed(70_000);var w=f.view().world();assertEquals(ModernTimeAttack.End.TIMEOUT,w.timeAttack().state().end());assertEquals(ModernBattleEngine.Phase.FINISHED,w.phase());
            assertTrue(w.timeAttack().outcomes(w).stream().allMatch(p->p.verdict()==ModernMatchSettlement.Verdict.LOSS));
        }
    }
    @Test void residentCorpsesDeferWholeNextWaveAndCancellationDuringDamageCannotStartIt()throws Exception {
        try(var f=new Fixture(1,1,100,false)) {
            f.active();f.tower(0,1);f.elapsed(1000);f.hit(0,2,f.living());
            var state=f.view();assertEquals(1,state.npcs().spawned());assertEquals(ModernTimeAttack.Phase.SPAWN_PENDING,state.world().timeAttack().state().phase());
            f.elapsed(1999);assertEquals(1,f.view().npcs().spawned());f.elapsed(2000);assertEquals(2,f.living().id().serial());
        }
        try(var f=new Fixture(1,8,100,false)) {
            f.active();f.duringRandom=()->f.rooms.releasePreparation(f.ticket);assertEquals(ModernNpcBattle.Reason.NOT_ACTIVE,f.tower(0,1).reason());
            var w=f.view().world();assertEquals(1,w.timeAttack().state().towerHp());assertEquals(0,w.vitals().get(0).attackSequence());assertEquals(ModernTimeAttack.Phase.ABORTED,w.timeAttack().state().phase());
        }
    }
    @Test void leaderCombatDeathIsTerminalIncludingAfterDepartureTransfersLeadership()throws Exception {
        for(boolean leader:List.of(true,false))try(var f=new Fixture(2,8,1,false)) {
            f.active();f.tower(0,1);if(!leader)f.engine.withdrawTimeAttack(f.players.get(0).owner());
            // When ownership transfers, the new leader remains the fatal boundary.
            for(int n=1;n<20&&f.view().world().phase()==ModernBattleEngine.Phase.ACTIVE;n++){f.elapsed(n*1000L);var npc=f.living();f.engine.stepNpc(npc.id(),npc.life(),4096,false);}
            var w=f.view().world();assertEquals(ModernTimeAttack.End.LEADER_DEFEATED,w.timeAttack().state().end());
            assertEquals(leader?1:2,w.timeAttack().state().leader());assertTrue(w.timeAttack().outcomes(w).stream().allMatch(p->p.verdict()==ModernMatchSettlement.Verdict.LOSS));
        }
    }
    @Test void ordinaryMemberCombatDeathDoesNotFinishOrRespawnAndOnlySurvivorsWin()throws Exception {
        try(var f=new Fixture(2,8,1,false)) {
            f.active();assertEquals(ModernBattleEngine.MoveStatus.MOVED,f.move(0,1,10,10,6).status());
            f.elapsed(200);assertEquals(ModernBattleEngine.MoveStatus.MOVED,f.move(0,2,9,10,6).status());
            f.tower(1,1);var first=f.living();f.elapsed(1000);
            assertEquals(ModernNpcBattle.Reason.ATTACKED,f.engine.stepNpc(first.id(),first.life(),4096,false).reason());
            var w=f.view().world();assertEquals(0,w.vitals().get(1).hp());assertEquals(1,w.vitals().get(0).hp());
            assertEquals(ModernBattleEngine.Phase.ACTIVE,w.phase());assertTrue(w.timeAttack().outcomes(w).isEmpty());
            assertEquals(ModernNpcBattle.Reason.DEAD,f.hit(1,2,first).reason());
            f.elapsed(1200);assertEquals(ModernBattleEngine.MoveStatus.MOVED,f.move(0,3,8,10,2).status());
            f.elapsed(1400);assertEquals(ModernBattleEngine.MoveStatus.MOVED,f.move(0,4,9,10,2).status());
            for(int wave=0;wave<f.plan.waves().size();wave++) {
                f.elapsed(2000+wave*1000);assertEquals(ModernNpcBattle.Reason.ATTACKED,f.hit(0,wave+1,f.living()).reason());
            }
            w=f.view().world();assertEquals(ModernTimeAttack.End.CLEARED,w.timeAttack().state().end());
            assertEquals(List.of(ModernMatchSettlement.Verdict.WIN,ModernMatchSettlement.Verdict.LOSS),w.timeAttack().outcomes(w).stream().map(ModernMatchSettlement.Participant::verdict).toList());
            assertEquals(0,w.vitals().get(1).hp());
        }
    }
    @Test void hugeDelayedTickEndsAtDeadlineAndNeverSpawnsOrCommitsLateDamage()throws Exception {
        for(boolean duringHit:List.of(false,true))try(var f=new Fixture(1,8,100,false)) {
            f.active();f.tower(0,1);f.elapsed(1000);var npc=f.living();long sequence=f.view().world().vitals().get(0).attackSequence();
            if(duringHit){f.duringRandom=()->f.elapsed(3_000_000);assertEquals(ModernNpcBattle.Reason.NOT_ACTIVE,f.hit(0,2,npc).reason());}
            else f.elapsed(3_000_000);
            var frame=f.view();var state=frame.world().timeAttack().state();assertEquals(ModernTimeAttack.End.TIMEOUT,state.end());
            assertEquals(state.startedMillis()+f.plan.durationMillis(),state.observedMillis());
            assertEquals(sequence,frame.world().vitals().get(0).attackSequence());assertTrue(frame.npcs().actors().get(0).alive());
        }
    }
    @Test void departedOwnerCannotCommitAnAlreadyRolledTowerOrNpcHit()throws Exception {
        for(boolean npcTarget:List.of(false,true))try(var f=new Fixture(2,8,100,false)) {
            f.active();if(npcTarget){f.tower(0,1);f.elapsed(1000);}
            var before=f.view();f.duringRandom=()->f.engine.withdrawTimeAttack(f.players.get(0).owner());
            assertEquals(ModernNpcBattle.Reason.NOT_ACTIVE,npcTarget?f.hit(0,2,f.living()).reason():f.tower(0,1).reason());
            var after=f.view();assertEquals(before.world().vitals().get(0).attackSequence(),after.world().vitals().get(0).attackSequence());
            assertEquals(before.world().timeAttack().state().towerHp(),after.world().timeAttack().state().towerHp());
            assertEquals(before.npcs(),after.npcs());assertEquals(2,after.world().timeAttack().state().leader());
            assertThrows(SecurityException.class,()->f.tower(0,3));
        }
    }
    @Test void towerUsesExactOwnerGenerationSharedSequenceCooldownAndWideHp()throws Exception {
        try(var f=new Fixture(2,8,100,false,4_000_000_000L)) {
            f.active();var p=f.players.get(0);var q=f.players.get(1);var tower=new ModernTimeAttack.TowerId(f.view().npcs().scope());var before=f.view();
            assertThrows(SecurityException.class,()->f.engine.attackTimeAttackTower(q.owner(),p.scope(),f.ticket.matchId(),f.ticket.generation(),1,tower));
            assertThrows(SecurityException.class,()->f.engine.attackTimeAttackTower(p.owner(),p.scope(),f.ticket.matchId(),f.ticket.generation()+1,1,tower));
            var wrong=new ModernTimeAttack.TowerId(new ModernNpcRoster.Scope(tower.scope().roomId(),tower.scope().matchId()+1,tower.scope().generation()));
            assertThrows(SecurityException.class,()->f.engine.attackTimeAttackTower(p.owner(),p.scope(),f.ticket.matchId(),f.ticket.generation(),1,wrong));
            assertEquals(before,f.view());assertEquals(0,f.draws.get());
            assertEquals(ModernNpcBattle.Reason.ATTACKED,f.tower(0,1).reason());var hit=f.view();var state=hit.world().timeAttack().state();
            assertTrue(state.towerHp()>Integer.MAX_VALUE);assertTrue(state.towerHp()<4_000_000_000L);assertEquals(-1,state.startedMillis());
            assertThrows(SecurityException.class,()->f.tower(0,1));assertEquals(hit,f.view());long rolls=f.draws.get();
            assertEquals(ModernNpcBattle.Reason.TOO_FAST,f.tower(0,2).reason());assertEquals(rolls,f.draws.get());assertEquals(state.towerHp(),f.view().world().timeAttack().state().towerHp());
            f.elapsed(700);assertEquals(ModernNpcBattle.Reason.ATTACKED,f.tower(0,3).reason());assertTrue(f.view().world().timeAttack().state().towerHp()<state.towerHp());
            assertEquals(ModernBattleEngine.MoveStatus.OCCUPIED,f.move(1,1,10,11,2).status());
        }
    }
    @Test void invalidRandomCannotDamageStartClockOrConsumeSuccessfulAttackCooldown()throws Exception {
        try(var f=new Fixture(1,8,100,false)) {
            f.active();f.random=-1;assertEquals(ModernNpcBattle.Reason.NUMERIC,f.tower(0,1).reason());
            var state=f.view().world().timeAttack();assertEquals(1,state.state().towerHp());assertEquals(-1,state.state().startedMillis());assertTrue(state.exchanges().isEmpty());
            f.random=19;assertEquals(ModernNpcBattle.Reason.ATTACKED,f.tower(0,2).reason());assertEquals(0,f.view().world().timeAttack().state().towerHp());
        }
    }
    @Test void departuresPreserveIndividualLossAndLastExitProducesOnlyPartyDefeat()throws Exception {
        try(var f=new Fixture(2,8,100,false)) {
            f.active();f.tower(0,1);var left=f.engine.withdrawTimeAttack(f.players.get(0).owner());assertEquals(2,left.timeAttack().state().leader());
            var ended=f.engine.withdrawTimeAttack(f.players.get(1).owner());assertEquals(ModernTimeAttack.End.PARTY_DEFEATED,ended.timeAttack().state().end());
            assertTrue(ended.timeAttack().outcomes(ended).stream().allMatch(p->p.verdict()==ModernMatchSettlement.Verdict.LOSS));assertEquals(0,f.engine.ownerCount());
        }
        try(var f=new Fixture(2,8,100,false)) {
            f.active();f.tower(0,1);f.engine.withdrawTimeAttack(f.players.get(0).owner());
            for(int wave=0;wave<f.plan.waves().size();wave++){f.elapsed(1000+wave*1000);assertEquals(ModernNpcBattle.Reason.ATTACKED,f.hit(1,wave+1,f.living()).reason());}
            var ended=f.view().world();assertEquals(List.of(ModernMatchSettlement.Verdict.LOSS,ModernMatchSettlement.Verdict.WIN),ended.timeAttack().outcomes(ended).stream().map(ModernMatchSettlement.Participant::verdict).toList());
        }
    }
    @Test void patternOrderPlacementsCatalogAndCooperativeLaneMustBeExplicit()throws Exception {
        try(var f=new Fixture(1,8,100,false)) {
            var catalog=ModernNpcMeleeTest.catalog(Map.of(ModernNpcCatalog.Field.HP,1L),true,0);
            f.plan.qualify(f.binding,catalog,List.of(cell(10,10)),8);
            var ordinary=f.plan.waves().get(0);var last=f.plan.waves().get(2);var sample=ordinary.spawns().get(0);
            assertThrows(IllegalArgumentException.class,()->new ModernTimeAttack.Plan(f.binding.key(),f.plan.tower(),List.of(last,ordinary),20_000));
            assertThrows(IllegalArgumentException.class,()->new ModernTimeAttack.Plan(f.binding.key(),f.plan.tower(),List.of(ordinary,last,ordinary),20_000));
            assertThrows(IllegalArgumentException.class,()->new ModernTimeAttack.Plan(f.binding.key(),f.plan.tower(),List.of(ordinary,last,last),20_000));
            assertThrows(IllegalArgumentException.class,()->new ModernTimeAttack.Plan(new ModernWorldBinding.Key(2,540),f.plan.tower(),f.plan.waves(),20_000));
            assertThrows(IllegalArgumentException.class,()->new ModernTimeAttack.Wave(ModernTimeAttack.Kind.ORDINARY,List.of(sample,sample)));
            assertThrows(IllegalArgumentException.class,()->new ModernTimeAttack.Wave(ModernTimeAttack.Kind.ORDINARY,List.of(new ModernNpcRoster.Spawn(sample.template(),1,1,sample.cell(),2))));
            assertThrows(IllegalArgumentException.class,()->new ModernTimeAttack.Wave(ModernTimeAttack.Kind.DRAGON,List.of(sample)));
            assertThrows(IllegalArgumentException.class,()->f.plan.qualify(f.binding,catalog,List.of(sample.cell()),8));
            var outside=new ModernTimeAttack.Plan(f.binding.key(),new ModernTimeAttack.Tower(cell(100,100),2665,1),f.plan.waves(),20_000);
            assertThrows(IllegalArgumentException.class,()->outside.qualify(f.binding,catalog,List.of(cell(10,10)),8));
            assertThrows(IllegalArgumentException.class,()->f.plan.qualify(f.binding,ModernNpcMeleeTest.catalog(),List.of(cell(10,10)),8));
            var boss=ModernNpcMeleeTest.catalog(Map.of(ModernNpcCatalog.Field.BOSS,1L),true,0);
            var player=Map.of(ModernParticipantId.human(7,3333),new ModernNpcMelee.BlessingBinding(7,"catalog",ModernNpcMelee.BlessingConvention.INTERNAL_ONE_BLESSED));
            assertThrows(IllegalArgumentException.class,()->new ModernNpcBattle.Config(f.binding.key(),boss,player,List.of(cell(10,10)),8,1000,null,false,f.plan),"a final-stage role is not boss combat qualification");
            try(var roster=new ModernNpcRoster(f.view().npcs().scope(),f.binding.map(),List.of(cell(10,10)),8,1000,false,true)) {
                assertThrows(IllegalArgumentException.class,()->roster.spawn(f.view().npcs().scope(),List.of(new ModernNpcRoster.Spawn(sample.template(),1,1,sample.cell(),2)),Set.of()));
                assertTrue(roster.snapshot().actors().isEmpty());
            }
        }
    }
    @Test void combinedFrameRequiresSameAuthoritativeLivingRosterAndNoFakeDeathReceipt()throws Exception {
        try(var f=new Fixture(1,8,100,false)) {
            f.active();var waiting=f.view().world().timeAttack().state();
            var impossible=new ModernPhysicalCombat.Outcome(true,1,1,0,false,false,false);
            assertThrows(IllegalArgumentException.class,()->ModernTimeAttack.towerHit(waiting,impossible,waiting.revision()+1));
            f.tower(0,1);var frame=f.view();var n=frame.npcs();
            var missing=new ModernNpcRoster.Snapshot(n.scope(),n.manifest(),n.revision(),n.elapsedMillis(),n.spawned(),false,List.of(),false);
            assertThrows(IllegalArgumentException.class,()->new ModernNpcBattle.Frame(frame.world(),missing,frame.exchanges()));
            assertThrows(IllegalArgumentException.class,()->ModernTimeAttack.depart(frame.world().timeAttack().state(),1,Set.of(61L),frame.world().revision()+1));
            f.clock.addAndGet(-1);assertEquals(ModernBattleEngine.AbortReason.CLOCK_REGRESSED,f.view().world().abortReason());
        }
    }
}
