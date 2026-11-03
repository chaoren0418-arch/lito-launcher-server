package local.lito381.server;

import static org.junit.jupiter.api.Assertions.*;
import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;

/** Pure clocks and owned real room tickets; no resource, SQL, socket or process execution. */
class ModernBattleEngineHonorTest {
    static final long SECOND=1_000_000_000L;
    static ModernWorldMap.Cell cell(int x,int y){return ModernBattleEngineBotTest.cell(x,y);}
    static ModernHonorHealing.Growth healing(int source,String database,long bonus){
        var rows=new ArrayList<ModernHonorHealing.Row>();for(int n=1;n<=127;n++)rows.add(new ModernHonorHealing.Row(n,bonus));
        return new ModernHonorHealing.Growth(source,database,rows);
    }
    static ModernPhysicalCombat.Source source(int source,ModernApplicationProtocol.CharacterRow row,long hp,long mp){
        return source(source,row,hp,mp,0);
    }
    static ModernPhysicalCombat.Source source(int source,ModernApplicationProtocol.CharacterRow row,long hp,long mp,long food){
        var s=ModernNaturalRegenTest.source(source,row,hp,mp,0,0,food);
        return new ModernPhysicalCombat.Source(s.input(),s.growth(),s.templates(),s.procCatalogIds(),s.magicGrowth(),s.magicBows(),s.weaponArrowSprites(),s.regenGrowth(),
                healing(source,s.growth().database(),0));
    }
    static final class Fixture implements AutoCloseable {
        final TournamentPresence presence=new TournamentPresence();
        final ModernWorldMap map;final ModernWorldBinding binding;final ModernRooms rooms;
        final List<ModernBattleEngineBotTest.Player> players=new ArrayList<>();
        final List<ModernBattleEngine.PreparedMember> prepared=new ArrayList<>();
        final ModernRooms.FrozenPreparation ticket;final ModernBattleEngine engine;final ModernHonor.Config honor;
        final AtomicLong clock=new AtomicLong(SECOND);final long origin;long jump;boolean invalid,cancel;int draws,invalidAt;
        final AtomicLong epoch=new AtomicLong(1000);
        java.util.function.LongUnaryOperator randomOverride;
        Fixture()throws Exception{this(100,100,100,SECOND);}
        Fixture(long towerHp,long secondHp,long mp,long origin)throws Exception {
            this(towerHp,secondHp,mp,origin,0);
        }
        Fixture(long towerHp,long secondHp,long mp,long origin,int botCount)throws Exception {
            this(towerHp,secondHp,mp,origin,botCount,false,true);
        }
        Fixture(long towerHp,long secondHp,long mp,long origin,int botCount,boolean fire,boolean honorEnabled)throws Exception {
            this(towerHp,secondHp,mp,origin,botCount,fire,honorEnabled,java.util.function.UnaryOperator.identity());
        }
        Fixture(long towerHp,long secondHp,long mp,long origin,int botCount,boolean fire,boolean honorEnabled,
                java.util.function.UnaryOperator<ModernBattleEngine.PreparedMember> prepare)throws Exception {
            this(towerHp,secondHp,mp,origin,botCount,fire,honorEnabled,prepare,java.util.function.UnaryOperator.identity());
        }
        Fixture(long towerHp,long secondHp,long mp,long origin,int botCount,boolean fire,boolean honorEnabled,
                java.util.function.UnaryOperator<ModernBattleEngine.PreparedMember> prepare,java.util.function.UnaryOperator<byte[]> terrain)throws Exception {
            this.origin=origin;clock.set(origin);
            map=ModernWorldMap.fromSectorBytes(532,Map.of(ModernBattleEngineBotTest.SECTOR,terrain.apply(ModernWorldMapTest.openSectorBytes())),()->false);
            binding=new ModernWorldBinding(honorEnabled?5:1,honorEnabled?532:514,map);rooms=new ModernRooms(Set.of(binding.key()),Set.of(binding.key()));
            for(int i=0;i<2;i++){
                int source=i==0?7:42;var owner=presence.connect();var row=new ModernApplicationProtocol.CharacterRow(3333,"human-"+i,1,0,80);
                var selection=new TournamentPresence.Selection(new TournamentPresence.CharacterKey(source,3333),"source-"+source,row.name(),row.level(),row.characterClass(),row.sex());
                assertEquals(TournamentPresence.SelectStatus.ACCEPTED,presence.select(owner,selection));rooms.enterLobby(owner,selection,1,0,true);
                var scope=new ModernRooms.Scope(source,3333,1,0);players.add(new ModernBattleEngineBotTest.Player(owner,scope,row));
                var summary=i==0?null:view().summary();
                var command=i==0?new ModernRooms.Command(scope,ModernRooms.Action.CREATE,0,0,0,ModernRoomsTest.draft(100,honorEnabled?5:1,honorEnabled?532:514,0),"")
                        :new ModernRooms.Command(scope,ModernRooms.Action.JOIN,summary.id(),summary.revision(),0,null,"");
                ModernRoomsTest.accepted(rooms.execute(owner,command));
                var fighter=ModernPhysicalCombatTest.fighter(source(source,row,i==0?100:secondHp,mp,fire?225:0));
                var supplies=ModernShopTimedTest.catalog(1).offers().stream().filter(o->o.support()==ModernShopCatalog.Support.HEALING||fire&&o.support()==ModernShopCatalog.Support.TIMED)
                        .sorted(java.util.Comparator.comparingLong(ModernShopCatalog.Offer::itemId)).map(o->new ModernShopAuthority.Stack(o,3)).toList();
                var skills=new ArrayList<ModernSkills.Skill>();skills.add(new ModernSkills.Skill(1,"$1436",4,10,ModernSkills.Support.BASIC_HEAL));skills.addAll(ModernBattleEngineBotTest.learned());
                if(fire)skills.add(new ModernSkills.Skill(70,"$1975",48,2000,ModernSkills.Support.FIRE_STORM));
                prepared.add(prepare.apply(new ModernBattleEngine.PreparedMember(owner,scope,row,cell(10+i,10),200_000_000,fighter,700_000_000,
                        new ModernSkills.Snapshot(source,row,skills),1_200_000_000,true,supplies,ModernInventoryWeight.qualify(fighter.profile(),100),true,false,true,fire)));
            }
            if(botCount>0){var gm=players.get(0);var summary=view().summary();var grant=new ModernGmCommands.Snapshot(gm.scope().sourceId(),"fixture",gm.row(),200,false,false,new ModernGmCommands.Definition("de",200,"L1SpawnDe",false));
                var added=rooms.spawnBots(gm.owner(),gm.scope(),summary.id(),summary.revision(),botCount,grant);ModernRoomsTest.accepted(added.result());assertEquals(botCount,added.added());}
            for(var p:players){var s=view().summary();ModernRoomsTest.accepted(rooms.execute(p.owner(),new ModernRooms.Command(p.scope(),ModernRooms.Action.READY,s.id(),s.revision(),1,null,"")));}
            var s=view().summary();var preparation=rooms.prepareWorld(players.get(0).owner(),players.get(0).scope(),s.id(),s.revision());ModernRoomsTest.accepted(preparation.result());ticket=preparation.ticket();
            honor=honorEnabled?new ModernHonor.Config(List.of(new ModernHonor.Tower(0,cell(10,9),2665,towerHp),new ModernHonor.Tower(1,cell(11,9),2669,towerHp)),List.of(cell(10,10),cell(11,10))):null;
            var bots=ticket.bots().stream().map(m->new ModernBattleEngine.BotPrepared(m.identity(),ModernBotTemplate.roomTestDummy(m.characterClass(),m.sex()),cell(12+ticket.bots().indexOf(m),10))).toList();
            engine=new ModernBattleEngine(ticket,binding,prepared,bots,new ModernBattleEngine.Limits(2000,0,1_800_000),clock::get,epoch::get,b->{
                draws++;if(jump!=0){clock.addAndGet(jump);jump=0;}if(cancel){rooms.releasePreparation(ticket);cancel=false;}return invalid||invalidAt>0&&draws==invalidAt?-1:randomOverride!=null?randomOverride.applyAsLong(b):b-1;},true,honor);
        }
        ModernRooms.RoomSnapshot view(){var p=players.get(0);return rooms.execute(p.owner(),new ModernRooms.Command(p.scope(),ModernRooms.Action.READ,0,0,0,null,"")).view().joined();}
        ModernBattleEngine.Snapshot active(){for(var p:players)engine.ackLoaded(p.owner(),p.scope(),ticket.matchId(),ticket.generation(),map.digest());return engine.tick();}
        void elapsed(long n){clock.set(origin+n);}
        ModernSpellProtocol.Request castRequest(int who,long seq,int skill,long target){return new ModernSpellProtocol.Request(players.get(who).scope(),ticket.matchId(),ticket.generation(),seq,skill,target);}
        ModernBattleEngine.CastResult cast(int who,long seq,int skill,long target){return engine.cast(players.get(who).owner(),castRequest(who,seq,skill,target));}
        ModernBattleEngine.AttackResult attack(int who,long seq,long target){var p=players.get(who);return engine.attack(p.owner(),p.scope(),ticket.matchId(),ticket.generation(),seq,target);}
        ModernBattleEngine.MoveResult move(int who,long seq,int heading){var p=players.get(who);var pos=engine.tick().actors().get(who).position();return engine.move(p.owner(),p.scope(),ticket.matchId(),ticket.generation(),seq,pos.x(),pos.y(),heading);}
        @Override public void close(){engine.close();rooms.releasePreparation(ticket);for(var p:players){rooms.disconnect(p.owner());presence.disconnect(p.owner());}assertEquals(0,rooms.memberCount());}
    }
    static ModernCombatProtocol.Frame frame(ModernBattleEngine.Snapshot s){
        var world=new ModernWorldProtocol.Frame(s.matchId(),s.generation(),Math.incrementExact(s.revision()),ModernWorldProtocol.Phase.valueOf(s.phase().name()),
                ModernWorldProtocol.AbortReason.valueOf(s.abortReason().name()),s.room(),s.manifest(),s.actors(),
                s.result()==null?null:new ModernWorldProtocol.Completion(s.result(),ModernWorldProtocol.SettlementState.PENDING),s.casts().stream().map(ModernSpellProtocol.CastState::wireRevision).toList());
        var v=s.vitals().stream().map(x->new ModernCombatProtocol.Vital(x.entityId(),x.hp(),x.maxHp(),x.mp(),x.maxMp(),x.ac(),x.weaponType(),x.attackSequence(),
                ModernRegenProtocolTest.event(x.attackRevision()),ModernRegenProtocolTest.event(x.hurtRevision()),ModernRegenProtocolTest.event(x.deathRevision()),x.lastTargetEntityId(),x.attackIntervalNanos())).toList();
        return new ModernCombatProtocol.Frame(world,v,s.damages().stream().map(ModernDamageSpellProtocol.State::wireRevision).toList(),s.items().stream().map(ModernItemProtocol.State::wireRevision).toList(),s.weights(),
                s.buffs().stream().map(ModernTimedEffects.State::wireRevision).toList(),s.ammo().stream().map(ModernAmmo.State::wireRevision).toList(),s.regen().stream().map(ModernRegenProtocol.State::wireRevision).toList(),s.honor().wireRevision());
    }
    static void roundtrip(Fixture f,ModernBattleEngine.Snapshot s)throws Exception{
        var request=new ModernCombatProtocol.Request(f.players.get(0).scope(),ModernCombatProtocol.Action.READ,s.matchId(),s.generation(),0,0);
        var m=new ModernApplicationProtocol.CombatResult(1,request,new ModernCombatProtocol.Reply(ModernCombatProtocol.Status.OK,ModernCombatProtocol.Reason.NONE,frame(s)));
        var bytes=ModernApplicationProtocol.encode(m,13);assertEquals(m,ModernApplicationProtocol.decode(bytes,13));assertArrayEquals(bytes,ModernApplicationProtocol.readFrame(new ByteArrayInputStream(bytes),13));
        assertThrows(ModernApplicationProtocol.ProtocolException.class,()->ModernApplicationProtocol.encode(m,12));
    }
    @Test void twoObjectivesAreNotParticipantsOwnersOrSqlCharactersAndRequireRealLoadedAcks()throws Exception{
        try(var f=new Fixture()){var loading=f.engine.tick();assertEquals(2,loading.actors().size());assertEquals(2,f.engine.ownerCount());assertEquals(List.of(3L,4L),loading.honor().objectives().stream().map(ModernHonor.Objective::entityId).toList());
            assertEquals(ModernBattleEngine.Phase.LOADING,loading.phase());roundtrip(f,loading);var active=f.active();roundtrip(f,active);assertTrue(active.honor().lives().stream().allMatch(l->l.deathCount()==0));}
    }
    @Test void fullHpFriendlyHealStillConsumesSourceMpAndPublishesOneInteractionButNoFakeDamage()throws Exception{
        try(var f=new Fixture()){var before=f.active();var result=f.cast(0,1,1,3);assertEquals(ModernSpellProtocol.Reason.NONE,result.reason());var after=result.snapshot();var h=after.honor().heals().get(0);var tower=after.honor().objectives().get(0);
            assertEquals(0,h.restored());assertEquals(4,h.mpSpent());assertEquals(96,after.vitals().get(0).mp());assertEquals(after.revision(),h.revision());assertEquals(after.revision(),tower.revision());assertEquals(3,tower.lastKind());assertEquals(1,tower.lastActorEntityId());
            assertEquals(0,tower.hurtRevision());assertEquals(before.damages(),after.damages());assertEquals(before.items(),after.items());assertEquals(0,after.casts().get(0).castCount());assertEquals(1,after.casts().get(0).requestSequence());roundtrip(f,after);}
    }
    @Test void enemyTowerDamageThenFriendlyHealingUsesActualDiceAndExactObjectiveHpLedger()throws Exception{
        try(var f=new Fixture()){f.active();var hit=f.cast(0,1,4,4);assertEquals(ModernSpellProtocol.Reason.NONE,hit.reason());long hp=hit.snapshot().honor().objectives().get(1).hp();assertTrue(hp<100);
            var healed=f.cast(1,1,1,4);assertEquals(ModernSpellProtocol.Reason.NONE,healed.reason());var h=healed.snapshot().honor().heals().get(1);assertEquals(Math.min(8,100-hp),h.restored());
            var t=healed.snapshot().honor().objectives().get(1);assertEquals(hp+h.restored(),t.hp());assertEquals(hit.snapshot().revision(),t.hurtRevision());roundtrip(f,healed.snapshot());}
    }
    @Test void friendlyAttackAndEnemyHealAreDeniedWithoutMpHpOrObjectiveEvents()throws Exception{
        try(var f=new Fixture()){f.active();var before=f.engine.tick();assertEquals(ModernBattleEngine.AttackStatus.SAME_TEAM,f.attack(0,1,3).status());assertEquals(ModernSpellProtocol.Reason.SAME_TEAM,f.cast(0,1,1,4).reason());
            var after=f.engine.tick();assertEquals(before.honor(),after.honor());assertEquals(before.vitals().get(0).mp(),after.vitals().get(0).mp());assertEquals(1,after.vitals().get(0).attackSequence());assertEquals(1,after.casts().get(0).requestSequence());roundtrip(f,after);}
    }
    @Test void playerDeathDoesNotEndHonorAndFiveSecondRespawnPreservesMpSequencesAndDeathHistory()throws Exception{
        try(var f=new Fixture(100,1,100,SECOND)){f.active();f.cast(1,1,1,4);var killed=f.cast(0,1,4,2).snapshot();assertEquals(ModernBattleEngine.Phase.ACTIVE,killed.phase());assertNull(killed.result());assertEquals(0,killed.vitals().get(1).hp());
            long death=killed.vitals().get(1).deathRevision();assertEquals(5000,killed.honor().lives().get(1).pendingDueMillis());roundtrip(f,killed);
            f.elapsed(5*SECOND-1);assertEquals(0,f.engine.tick().vitals().get(1).hp());f.elapsed(5*SECOND);var revived=f.engine.tick();assertEquals(1,revived.vitals().get(1).hp());assertEquals(96,revived.vitals().get(1).mp());
            assertEquals(death,revived.vitals().get(1).deathRevision());assertEquals(killed.actors(),revived.actors());assertEquals(killed.items(),revived.items());assertEquals(killed.casts(),revived.casts());assertEquals(1,revived.honor().lives().get(1).respawnHpGain());roundtrip(f,revived);
            f.elapsed(6*SECOND);assertEquals(ModernBattleEngine.MoveStatus.MOVED,f.move(1,1,4).status());roundtrip(f,f.engine.tick());}
    }
    @Test void deadCellAndBothTowersRemainOccupiedForMovesAndTeleportCandidates()throws Exception{
        try(var f=new Fixture(100,1,100,SECOND)){f.active();assertEquals(ModernBattleEngine.MoveStatus.OCCUPIED,f.move(0,1,0).status());f.cast(0,1,4,2);
            f.elapsed(2*SECOND);assertEquals(ModernBattleEngine.MoveStatus.OCCUPIED,f.move(0,2,2).status());var tp=f.cast(0,2,5,0);assertEquals(ModernSpellProtocol.Reason.NONE,tp.reason());
            assertTrue(tp.snapshot().honor().objectives().stream().noneMatch(o->o.cell().equals(tp.snapshot().actors().get(0).position())));}
    }
    @Test void towerDeathFinishesWithWholeTeamResultEvenWhenLosingPlayerIsAlive()throws Exception{
        try(var f=new Fixture(1,100,100,SECOND)){f.active();var done=f.cast(0,1,4,4).snapshot();assertEquals(ModernBattleEngine.Phase.FINISHED,done.phase());assertEquals(5,done.result().mode());
            assertEquals(2,done.result().participants().size());assertEquals(3333,done.result().participants().get(0).key().objectId());assertEquals(3333,done.result().participants().get(1).key().objectId());
            assertEquals(7,done.result().participants().get(0).key().sourceId());assertEquals(42,done.result().participants().get(1).key().sourceId());assertEquals(ModernMatchSettlement.Verdict.WIN,done.result().participants().get(0).verdict());
            assertEquals(100,done.vitals().get(1).hp());roundtrip(f,done);f.elapsed(100*SECOND);assertEquals(done,f.engine.tick());}
    }
    @Test void deadLosingPlayerRemainsInResultAndTerminalCancelsOnlyPendingRespawn()throws Exception{
        try(var f=new Fixture(1,1,100,SECOND)){f.active();f.cast(0,1,4,2);f.elapsed(2*SECOND);var done=f.cast(0,2,4,4).snapshot();assertEquals(ModernBattleEngine.Phase.FINISHED,done.phase());
            assertEquals(1,done.honor().lives().get(1).deathCount());assertEquals(0,done.honor().lives().get(1).pendingDueMillis());assertEquals(0,done.vitals().get(1).hp());assertEquals(2,done.result().participants().size());roundtrip(f,done);}
    }
    @Test void slowBadAndCancelledHealingNeverPartiallyCommitObjectiveHpMpOrEvent()throws Exception{
        for(int mode=0;mode<3;mode++)try(var f=new Fixture()){var before=f.active();if(mode==0)f.invalid=true;if(mode==1)f.jump=1800*SECOND;if(mode==2)f.cancel=true;
            var result=f.cast(0,1,1,3);var after=result.snapshot();assertEquals(mode==0?ModernSpellProtocol.Reason.NUMERIC:ModernSpellProtocol.Reason.NOT_ACTIVE,result.reason());
            assertEquals(before.honor().objectives(),after.honor().objectives());assertEquals(before.honor().heals(),after.honor().heals());assertEquals(before.vitals(),after.vitals());roundtrip(f,after);}
    }
    @Test void sharedCastCooldownAndSequenceSpanHealArrowAndTeleportWithoutDoubleSpending()throws Exception{
        try(var f=new Fixture()){f.active();f.cast(0,1,1,3);assertEquals(ModernSpellProtocol.Reason.TOO_FAST,f.cast(0,2,4,4).reason());f.elapsed(2*SECOND);
            assertEquals(ModernSpellProtocol.Reason.NONE,f.cast(0,3,4,4).reason());assertThrows(SecurityException.class,()->f.cast(0,3,1,3));
            var after=f.engine.tick();assertEquals(93,after.vitals().get(0).mp());assertEquals(1,after.honor().heals().get(0).castCount());assertEquals(1,after.damages().get(0).castCount());assertEquals(3,after.casts().get(0).requestSequence());roundtrip(f,after);}
    }
    @Test void signedNanoWrapStillRespawnsAtExactlyFiveSecondsAndTimeoutNeverInventsWinner()throws Exception{
        try(var f=new Fixture(100,1,100,Long.MAX_VALUE-SECOND)){f.active();f.cast(0,1,4,2);f.elapsed(5*SECOND);assertTrue(f.clock.get()<0);assertEquals(1,f.engine.tick().vitals().get(1).hp());roundtrip(f,f.engine.tick());
            f.elapsed(1800*SECOND);var timedOut=f.engine.tick();assertEquals(ModernBattleEngine.AbortReason.ACTIVE_TIMEOUT,timedOut.abortReason());assertNull(timedOut.result());roundtrip(f,timedOut);}
    }
    @Test void configuredHonorAcceptsGmBotsAsParticipantsWithRespawnButNoSourceRecoveryOrTowerIdentity()throws Exception{
        try(var f=new Fixture(1,100,100,SECOND,1)){var active=f.active();assertEquals(3,active.actors().size());assertEquals(2,f.engine.ownerCount());assertFalse(active.actors().get(2).member().identity().isHuman());
            assertEquals(List.of(4L,5L),active.honor().objectives().stream().map(ModernHonor.Objective::entityId).toList());long seq=0;ModernBattleEngine.Snapshot killed=active;
            while(killed.vitals().get(2).hp()>0&&seq<5){f.elapsed(seq*1_200_000_000L);killed=f.cast(1,++seq,4,3).snapshot();}
            assertEquals(0,killed.vitals().get(2).hp());assertEquals(1,killed.honor().lives().get(2).deathCount());assertEquals(ModernHonor.Heal.initial(3,0),killed.honor().heals().get(2));
            f.elapsed(10*SECOND);var respawn=f.engine.tick();assertEquals(40,respawn.vitals().get(2).hp());assertEquals(40,respawn.honor().lives().get(2).respawnHpGain());
            assertEquals(ModernRegenProtocol.State.initial(3).damaged(40),respawn.regen().get(2));roundtrip(f,respawn);
            var done=f.cast(1,++seq,4,4).snapshot();assertEquals(ModernBattleEngine.Phase.FINISHED,done.phase());assertEquals(3,done.result().participants().size());
            assertEquals(2,done.result().participants().stream().filter(p->p.identity().isHuman()).count());assertTrue(done.result().hasBots());roundtrip(f,done);}
    }
}
