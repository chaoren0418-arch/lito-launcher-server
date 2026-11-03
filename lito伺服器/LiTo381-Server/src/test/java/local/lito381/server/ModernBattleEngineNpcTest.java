package local.lito381.server;

import static org.junit.jupiter.api.Assertions.*;
import static local.lito381.server.ModernNpcCatalog.Field.*;
import static local.lito381.server.ModernNpcBattle.Reason.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.UnaryOperator;
import org.junit.jupiter.api.Test;

/** Actual room capabilities + ONE engine's actors/clock/regen, not a second fake player authority. */
class ModernBattleEngineNpcTest {
    static final long MS=1_000_000,STEP=200*MS,ATTACK=700*MS;
    static ModernWorldMap.Cell cell(int x,int y){return ModernBattleEngineBotTest.cell(x,y);}
    static final class Fixture implements AutoCloseable {
        final TournamentPresence presence=new TournamentPresence();
        final ModernWorldMap map;final ModernWorldBinding binding;final ModernRooms rooms;
        final List<ModernBattleEngineBotTest.Player> players=new ArrayList<>();
        final List<ModernBattleEngine.PreparedMember> prepared;final List<ModernBattleEngine.BotPrepared> bots;
        final ModernRooms.FrozenPreparation ticket;final ModernNpcRoster.Scope scope;final ModernNpcBattle.Config config;
        final AtomicLong clock=new AtomicLong(1000*MS),epoch=new AtomicLong(1000),draws=new AtomicLong();
        final ModernBattleEngine.Limits limits;ModernBattleEngine engine;
        Runnable duringRandom;boolean invalidRandom;long randomValue;long[] scripted;int scriptIndex;
        Fixture()throws Exception{this(2,0,false,100,0,UnaryOperator.identity(),ModernNpcMeleeTest.catalog(),true);}
        Fixture(int humans,int botCount,boolean regen,long hp,long countdown,UnaryOperator<ModernPhysicalCombat.Source> transform,
                ModernNpcCatalog.Catalog catalog,boolean build)throws Exception {
            this(humans,botCount,regen,hp,countdown,transform,catalog,build,
                    ModernWorldMap.fromSectorBytes(554,Map.of(ModernBattleEngineBotTest.SECTOR,ModernWorldMapTest.openSectorBytes()),()->false),
                    java.util.stream.IntStream.range(0,humans+botCount).mapToObj(i->cell(10+i*10,10)).toList());
        }
        Fixture(int humans,int botCount,boolean regen,long hp,long countdown,UnaryOperator<ModernPhysicalCombat.Source> transform,
                ModernNpcCatalog.Catalog catalog,boolean build,ModernWorldMap world,List<ModernWorldMap.Cell> positions)throws Exception {
            this(humans,botCount,regen,hp,countdown,transform,catalog,build,world,positions,554);
        }
        Fixture(int humans,int botCount,boolean regen,long hp,long countdown,UnaryOperator<ModernPhysicalCombat.Source> transform,
                ModernNpcCatalog.Catalog catalog,boolean build,ModernWorldMap world,List<ModernWorldMap.Cell> positions,int logicalMap)throws Exception {
            map=world;
            binding=new ModernWorldBinding(2,logicalMap,map);rooms=new ModernRooms(Set.of(binding.key()),Set.of(binding.key()));
            limits=new ModernBattleEngine.Limits(2000,countdown,60_000);
            for(int i=0;i<humans;i++) {
                int source=i==0?7:42+i-1;var owner=presence.connect();var row=new ModernApplicationProtocol.CharacterRow(1,"human-"+i,1,0,80);
                var selection=new TournamentPresence.Selection(new TournamentPresence.CharacterKey(source,1),"server-"+source,row.name(),row.level(),row.characterClass(),row.sex());
                assertEquals(TournamentPresence.SelectStatus.ACCEPTED,presence.select(owner,selection));rooms.enterLobby(owner,selection,1,0,true);
                var p=new ModernBattleEngineBotTest.Player(owner,new ModernRooms.Scope(source,1,1,0),row);players.add(p);
                var summary=i==0?null:room().summary();
                ModernRoomsTest.accepted(rooms.execute(owner,i==0?new ModernRooms.Command(p.scope(),ModernRooms.Action.CREATE,0,0,0,ModernRoomsTest.draft(100,2,logicalMap,0),""):
                        new ModernRooms.Command(p.scope(),ModernRooms.Action.JOIN,summary.id(),summary.revision(),0,null,"")));
            }
            var first=players.get(0);var summary=room().summary();
            var grant=new ModernGmCommands.Snapshot(7,"fixture",first.row(),200,false,false,new ModernGmCommands.Definition("de",200,"L1SpawnDe",false));
            var added=rooms.spawnBots(first.owner(),first.scope(),summary.id(),summary.revision(),botCount,grant);ModernRoomsTest.accepted(added.result());
            assertEquals(botCount,added.added());
            for(var p:players){summary=room().summary();ModernRoomsTest.accepted(rooms.execute(p.owner(),new ModernRooms.Command(p.scope(),ModernRooms.Action.READY,summary.id(),summary.revision(),1,null,"")));}
            summary=room().summary();var frozen=rooms.prepareWorld(first.owner(),first.scope(),summary.id(),summary.revision());ModernRoomsTest.accepted(frozen.result());ticket=frozen.ticket();
            scope=new ModernNpcRoster.Scope(summary.id(),ticket.matchId(),ticket.generation());
            var bindings=new HashMap<ModernParticipantId,ModernNpcMelee.BlessingBinding>();var members=new ArrayList<ModernBattleEngine.PreparedMember>();
            for(var p:players) {
                var source=transform.apply(regen?ModernNaturalRegenTest.source(p.scope().sourceId(),p.row(),hp,30,0,0,225):
                        ModernPhysicalCombatTest.fixtureSource(p.scope().sourceId(),p.row(),hp,10,1));
                var fighter=ModernPhysicalCombat.qualify(source,ModernCombatProfileTest.binding(source.input().equipment()),Optional.empty(),1000);
                int index=ticket.snapshot().members().indexOf(ticket.participants().stream().filter(a->a.owner()==p.owner()).findFirst().orElseThrow().member());
                members.add(new ModernBattleEngine.PreparedMember(p.owner(),p.scope(),p.row(),positions.get(index),STEP,fighter,ATTACK,null,0,false,
                        regen?List.of():null,regen?ModernInventoryWeight.qualify(fighter.profile(),100):null,false,false,regen));
                bindings.put(ModernParticipantId.human(p.scope().sourceId(),1),new ModernNpcMelee.BlessingBinding(p.scope().sourceId(),fighter.catalogDatabase(),ModernNpcMelee.BlessingConvention.INTERNAL_ONE_BLESSED));
            }
            prepared=List.copyOf(members);
            bots=ticket.bots().stream().map(b->new ModernBattleEngine.BotPrepared(b.identity(),ModernBotTemplate.roomTestDummy(b.characterClass(),b.sex()),
                    positions.get(ticket.snapshot().members().indexOf(b)))).toList();
            config=new ModernNpcBattle.Config(binding.key(),catalog,bindings,List.of(positions.get(0),positions.get(1)),128,1000);
            if(build)engine=build(config);
        }
        ModernBattleEngine build(ModernNpcBattle.Config config) {return build(config,false);}
        ModernBattleEngine build(ModernNpcBattle.Config config,boolean settlement) {
            return build(config,settlement,prepared);
        }
        ModernBattleEngine build(ModernNpcBattle.Config config,boolean settlement,List<ModernBattleEngine.PreparedMember> members) {
            return new ModernBattleEngine(ticket,binding,members,bots,limits,clock::get,epoch::get,b->{draws.incrementAndGet();
                if(duringRandom!=null){var action=duringRandom;duringRandom=null;action.run();}
                if(scripted!=null){assertTrue(scriptIndex<scripted.length);return scripted[scriptIndex++];}
                return invalidRandom?-1:Math.min(randomValue,b-1);},settlement,null,()->false,null,config);
        }
        ModernRooms.RoomSnapshot room(){var p=players.get(0);return rooms.execute(p.owner(),new ModernRooms.Command(p.scope(),ModernRooms.Action.READ,0,0,0,null,"")).view().joined();}
        ModernNpcBattle.Frame view(){var p=players.get(0);return engine.npcSnapshot(p.owner(),p.scope(),ticket.matchId(),ticket.generation());}
        void ack(int i){var p=players.get(i);engine.ackLoaded(p.owner(),p.scope(),ticket.matchId(),ticket.generation(),map.digest());}
        ModernNpcBattle.Frame active(){for(int i=0;i<players.size();i++)ack(i);clock.addAndGet(limits.countdownNanos());return view();}
        ModernNpcRoster.Actor spawn(int lane,int x,int y){return spawn(lane,cell(x,y));}
        ModernNpcRoster.Actor spawn(int lane,ModernWorldMap.Cell cell){var t=config.catalog().npcs().get(ModernNpcMeleeTest.NPC).rosterTemplate();
            var frame=engine.spawnNpcs(scope,List.of(new ModernNpcRoster.Spawn(t,lane,1,cell,2)));return frame.npcs().actors().get(frame.npcs().actors().size()-1);}
        ModernNpcBattle.Result attack(int player,long sequence,ModernNpcRoster.Actor target){var p=players.get(player);return engine.attackNpc(p.owner(),p.scope(),ticket.matchId(),ticket.generation(),sequence,target.id(),target.life());}
        ModernNpcBattle.Result step(ModernNpcRoster.Actor npc){return engine.stepNpc(npc.id(),npc.life(),4096,false);}
        void elapsed(long millis){clock.set(1000*MS+limits.countdownNanos()+millis*MS);}
        ModernBattleEngine.MoveResult move(int player,long seq,int x,int y,int heading){var p=players.get(player);return engine.move(p.owner(),p.scope(),ticket.matchId(),ticket.generation(),seq,cell(x,y).x(),cell(x,y).y(),heading);}
        @Override public void close(){if(engine!=null)engine.close();rooms.releasePreparation(ticket);for(var p:players){rooms.disconnect(p.owner());presence.disconnect(p.owner());}
            assertEquals(0,rooms.memberCount());assertEquals(0,rooms.ownerCount());}
    }
    @Test void npcDamageAndQualifiedSuppliesShareHpWeightCooldownAndRecoveryLedger()throws Exception {
        try(var f=new Fixture(2,0,true,100,0,UnaryOperator.identity(),ModernNpcMeleeTest.catalog(),false)) {
            var stocked=f.prepared.stream().map(p->new ModernBattleEngine.PreparedMember(
                    p.owner(),p.scope(),p.character(),p.spawn(),p.stepIntervalNanos(),p.fighter(),p.attackIntervalNanos(),
                    null,0,false,List.of(new ModernShopAuthority.Stack(ModernShopCatalogTest.offer(40010,0),2)),
                    p.weightBasis(),false,false,true)).toList();
            f.engine=f.build(f.config,false,stocked);var loaded=f.active();
            assertEquals(2,loaded.world().items().get(0).stocks().get(0).remaining());
            var npc=f.spawn(0,11,10);f.elapsed(100);var hurt=f.step(npc).frame();
            var p=f.players.get(0);long hp=hurt.world().vitals().get(0).hp();assertTrue(hp>0&&hp<100);
            var request=new ModernItemProtocol.Request(p.scope(),ModernItemProtocol.Action.USE,f.ticket.matchId(),f.ticket.generation(),1,40010);
            var used=f.engine.item(p.owner(),request);assertEquals(ModernItemProtocol.Reason.NONE,used.reason());
            var after=f.view();var receipt=after.world().items().get(0);
            assertEquals(Math.min(10,100-hp),receipt.restored());assertEquals(hp+receipt.restored(),after.world().vitals().get(0).hp());
            assertEquals(1,receipt.useCount());assertEquals(1,receipt.stocks().get(0).remaining());
            assertEquals(receipt.restored(),after.world().regen().get(0).itemGain());
            assertEquals(hurt.world().regen().get(0).damageLoss(),after.world().regen().get(0).damageLoss());
            assertEquals(hurt.exchanges(),after.exchanges());assertEquals(hurt.npcs(),after.npcs());
            assertEquals(hurt.world().items().get(1),after.world().items().get(1));
            assertEquals(ModernInventoryWeight.fromShop(stocked.get(0).weightBasis(),
                    List.of(new ModernShopAuthority.Stack(ModernShopCatalogTest.offer(40010,0),1))),after.world().weights().get(0).snapshot());
            var fast=f.engine.item(p.owner(),new ModernItemProtocol.Request(p.scope(),ModernItemProtocol.Action.USE,f.ticket.matchId(),f.ticket.generation(),2,40010));
            assertEquals(ModernItemProtocol.Reason.TOO_FAST,fast.reason());
            assertEquals(receipt.stocks(),fast.snapshot().items().get(0).stocks());
            assertEquals(receipt.useRevision(),fast.snapshot().items().get(0).useRevision());
        }
    }
    @Test void npcOptInRequiresExactHumanBindingsAndCannotConsumeTicketOnFailedAssembly()throws Exception {
        try(var f=new Fixture(2,0,false,100,0,UnaryOperator.identity(),ModernNpcMeleeTest.catalog(),false)) {
            assertThrows(IllegalArgumentException.class,()->f.build(null));
            var missing=new ModernNpcBattle.Config(f.binding.key(),f.config.catalog(),Map.of(ModernParticipantId.human(7,1),f.config.players().get(ModernParticipantId.human(7,1))),f.config.laneAnchors(),128,1000);
            assertThrows(IllegalArgumentException.class,()->f.build(missing));
            var bindings=new HashMap<>(f.config.players());bindings.put(ModernParticipantId.human(7,1),new ModernNpcMelee.BlessingBinding(7,"other_catalog",ModernNpcMelee.BlessingConvention.INTERNAL_ONE_BLESSED));
            var wrong=new ModernNpcBattle.Config(f.binding.key(),f.config.catalog(),bindings,f.config.laneAnchors(),128,1000);
            assertThrows(IllegalArgumentException.class,()->f.build(wrong));
            f.engine=f.build(f.config);assertEquals(2,f.engine.ownerCount());assertThrows(IllegalStateException.class,()->f.build(f.config));
        }
    }
    @Test void catalogAndSourceConventionAreImmutableAndUnsupportedNpcCannotBeAdmitted()throws Exception {
        try(var f=new Fixture()) {
            assertThrows(UnsupportedOperationException.class,()->f.config.players().clear());
            for(var change:List.of(Map.of(BOSS,1L),Map.of(RANGE,2L),Map.of(HP_REGEN,1L)))assertThrows(IllegalArgumentException.class,()->
                    new ModernNpcBattle.Config(f.binding.key(),ModernNpcMeleeTest.catalog(change,true,0),f.config.players(),f.config.laneAnchors(),128,1000));
            assertThrows(IllegalArgumentException.class,()->new ModernNpcBattle.Config(new ModernWorldBinding.Key(1,514),f.config.catalog(),f.config.players(),f.config.laneAnchors(),128,1000));
        }
    }
    @Test void loadedBarrierAndCountdownPrecedeSpawnAndNpcClockOrigin()throws Exception {
        try(var f=new Fixture(1,1,false,100,3000*MS,UnaryOperator.identity(),ModernNpcMeleeTest.catalog(),true)) {
            assertEquals(ModernBattleEngine.Phase.LOADING,f.view().world().phase());assertThrows(IllegalStateException.class,()->f.spawn(0,11,10));
            assertEquals(0,f.view().npcs().elapsedMillis());f.ack(0);assertThrows(IllegalStateException.class,()->f.spawn(0,11,10));
            f.clock.addAndGet(3000*MS-1);assertEquals(ModernBattleEngine.Phase.COUNTDOWN,f.view().world().phase());
            f.clock.incrementAndGet();assertEquals(ModernBattleEngine.Phase.ACTIVE,f.view().world().phase());assertEquals(0,f.view().npcs().elapsedMillis());
            assertEquals(100,f.spawn(0,11,10).nextMoveMillis());
        }
    }
    @Test void playerStrikeCommitsOneWorldRevisionAndTypedTargetNotAnAliasedPvpEntity()throws Exception {
        try(var f=new Fixture()) {
            f.active();var npc=f.spawn(0,11,10);var before=f.view();var result=f.attack(0,1,npc);assertEquals(ATTACKED,result.reason());
            var after=result.frame();assertEquals(before.world().revision()+1,after.world().revision());
            assertTrue(after.npcs().actors().get(0).hp()<npc.hp());assertEquals(1,after.world().vitals().get(0).attackSequence());
            assertEquals(0,after.world().vitals().get(0).lastTargetEntityId());assertEquals(0,after.world().vitals().get(0).attackRevision());
            var event=after.exchanges().get(0);assertEquals(npc.id(),event.npc());assertEquals(ModernParticipantId.human(7,1),event.participant());
            assertEquals(after.world().revision(),event.revision());assertEquals(npc.hp()-event.outcome().appliedDamage(),after.npcs().actors().get(0).hp());
            assertEquals(before.world().vitals().get(1),after.world().vitals().get(1));assertThrows(UnsupportedOperationException.class,()->after.exchanges().clear());
        }
    }
    @Test void acceptedAndDeniedNpcAttacksShareMovementClockAndNeverResetLastAcceptedTime()throws Exception {
        try(var f=new Fixture()) {
            f.active();var npc=f.spawn(0,11,10);assertEquals(ATTACKED,f.attack(0,1,npc).reason());
            assertEquals(TOO_FAST,f.attack(0,2,npc).reason());assertEquals(ModernBattleEngine.MoveStatus.TOO_FAST,f.move(0,1,10,10,0).status());
            f.elapsed(699);assertEquals(TOO_FAST,f.attack(0,3,npc).reason());f.elapsed(700);assertEquals(ModernBattleEngine.MoveStatus.MOVED,f.move(0,2,10,10,0).status());
            assertEquals(TOO_FAST,f.attack(0,4,npc).reason());f.elapsed(900);assertEquals(ATTACKED,f.attack(0,5,npc).reason());
        }
    }
    @Test void fullOwnerScopeNpcRoomMatchGenerationAndLifeRejectWithoutMutation()throws Exception {
        try(var f=new Fixture()) {
            f.active();var npc=f.spawn(0,11,10);var before=f.view();var p=f.players.get(0);var q=f.players.get(1);
            assertThrows(SecurityException.class,()->f.engine.attackNpc(q.owner(),p.scope(),f.ticket.matchId(),f.ticket.generation(),1,npc.id(),1));
            assertThrows(SecurityException.class,()->f.engine.attackNpc(p.owner(),q.scope(),f.ticket.matchId(),f.ticket.generation(),1,npc.id(),1));
            assertThrows(SecurityException.class,()->f.engine.attackNpc(p.owner(),p.scope(),f.ticket.matchId(),f.ticket.generation()+1,1,npc.id(),1));
            for(var scope:List.of(new ModernNpcRoster.Scope(f.scope.roomId()+1,f.scope.matchId(),f.scope.generation()),new ModernNpcRoster.Scope(f.scope.roomId(),f.scope.matchId()+1,f.scope.generation()),new ModernNpcRoster.Scope(f.scope.roomId(),f.scope.matchId(),f.scope.generation()+1)))
                assertThrows(SecurityException.class,()->f.engine.attackNpc(p.owner(),p.scope(),f.ticket.matchId(),f.ticket.generation(),1,new ModernNpcRoster.Id(scope,1),1));
            assertThrows(SecurityException.class,()->f.engine.attackNpc(p.owner(),p.scope(),f.ticket.matchId(),f.ticket.generation(),1,npc.id(),2));
            assertThrows(SecurityException.class,()->f.attack(0,2,npc));assertEquals(before,f.view());
            f.attack(0,1,npc);var after=f.view();assertThrows(SecurityException.class,()->f.attack(0,1,npc));assertEquals(after,f.view());
        }
    }
    @Test void playerLanePvpAndNpcOccupancyAreNotBypassedByTheNewMode()throws Exception {
        try(var f=new Fixture()) {
            f.active();var other=f.spawn(1,11,10);assertEquals(WRONG_LANE,f.attack(0,1,other).reason());
            assertEquals(ModernBattleEngine.MoveStatus.OCCUPIED,f.move(0,1,10,10,2).status());var p=f.players.get(0);var before=f.view();
            assertEquals(ModernBattleEngine.AttackStatus.NOT_ACTIVE,f.engine.attack(p.owner(),p.scope(),f.ticket.matchId(),f.ticket.generation(),2,2).status());
            assertEquals(before,f.view());assertEquals(WAIT,f.engine.stepNpc(other.id(),1,1,false).reason());
            f.elapsed(100);assertEquals(BUDGET,f.engine.stepNpc(other.id(),1,1,false).reason());
            assertEquals(100,f.view().world().vitals().get(0).hp());
        }
    }
    @Test void diagonalAttackAndMoveCannotCutThroughTwoLiveNpcCorners()throws Exception {
        try(var f=new Fixture()) {
            f.active();var target=f.spawn(0,11,9);f.spawn(0,10,9);f.spawn(0,11,10);f.spawn(0,9,10);
            assertEquals(BLOCKED,f.attack(0,1,target).reason());assertEquals(ModernBattleEngine.MoveStatus.BLOCKED,f.move(0,1,10,10,7).status());
            assertEquals(0,f.draws.get());
        }
    }
    @Test void npcStrikeCommitsPlayerHpAndClockTogetherAndLateTickNeverBursts()throws Exception {
        try(var f=new Fixture()) {
            f.active();var npc=f.spawn(0,11,10);assertEquals(TOO_FAST,f.step(npc).reason());f.elapsed(100);var before=f.view();
            var hit=f.step(npc);assertEquals(ATTACKED,hit.reason());var after=hit.frame();
            assertEquals(before.world().revision()+1,after.world().revision());assertTrue(after.world().vitals().get(0).hp()<100);
            assertEquals(after.world().revision(),after.world().vitals().get(0).hurtRevision());assertEquals(850,after.npcs().actors().get(0).nextAttackMillis());
            assertEquals(after,f.step(npc).frame());f.elapsed(849);assertEquals(TOO_FAST,f.step(npc).reason());
            f.elapsed(850);assertEquals(ATTACKED,f.step(npc).reason());f.elapsed(5000);assertEquals(ATTACKED,f.step(npc).reason());
            assertEquals(5750,f.view().npcs().actors().get(0).nextAttackMillis());
        }
    }
    @Test void sameNumericBotHumanAndNpcIdentitiesNeverShareHpOrCreateBotSourceRows()throws Exception {
        try(var f=new Fixture(1,1,true,100,0,UnaryOperator.identity(),ModernNpcMeleeTest.catalog(),true)) {
            f.active();var npc=f.spawn(1,21,10);f.elapsed(100);var hit=f.step(npc);assertEquals(ATTACKED,hit.reason());
            var frame=hit.frame();assertEquals(1,npc.id().serial());assertEquals(1,f.engine.ownerCount());assertEquals(100,frame.world().vitals().get(0).hp());
            assertTrue(frame.world().vitals().get(1).hp()<40);assertEquals(ModernParticipantId.bot(7,1),frame.exchanges().get(0).participant());
            assertEquals(ModernRegenProtocol.State.initial(2).damaged(frame.world().regen().get(1).damageLoss()),frame.world().regen().get(1));
            assertThrows(ModernNpcMelee.CombatException.class,()->ModernNpcMelee.victim(ModernParticipantId.human(7,1),ModernBotTemplate.roomTestDummy(1,0)));
        }
    }
    @Test void npcDamageUsesExistingRegenLedgerAndStaleHpCannotOverwriteNaturalRecovery()throws Exception {
        try(var f=new Fixture(2,0,true,100,0,UnaryOperator.identity(),ModernNpcMeleeTest.catalog(),true)) {
            f.active();var npc=f.spawn(0,11,10);f.elapsed(100);var hit=f.step(npc).frame();long hp=hit.world().vitals().get(0).hp();
            assertEquals(100-hp,hit.world().regen().get(0).damageLoss());f.elapsed(1999);var before=f.view();
            f.duringRandom=()->f.clock.addAndGet(MS);var stale=f.step(npc);assertEquals(STALE_STATE,stale.reason());
            assertEquals(hp+1,stale.frame().world().vitals().get(0).hp());assertEquals(hit.npcs().actors().get(0).nextAttackMillis(),stale.frame().npcs().actors().get(0).nextAttackMillis());
            assertEquals(before.exchanges(),stale.frame().exchanges());assertEquals(1,stale.frame().world().regen().get(0).hpGain());
            ModernRegenProtocol.validateTotals(stale.frame().world().regen().get(0),stale.frame().world().vitals().get(0),0,0);
        }
    }
    @Test void invalidRandomAndNumericOverflowPublishNoHalfStrike()throws Exception {
        for(boolean player:new boolean[]{false,true})try(var f=new Fixture()) {
            f.active();var npc=f.spawn(0,11,10);f.elapsed(100);var before=f.view();f.invalidRandom=true;
            var denied=player?f.attack(0,1,npc):f.step(npc);assertEquals(NUMERIC,denied.reason());
            assertEquals(before.npcs(),denied.frame().npcs());assertEquals(before.exchanges(),denied.frame().exchanges());
            assertEquals(before.world().vitals().get(0).hp(),denied.frame().world().vitals().get(0).hp());
            assertEquals(player?1:0,denied.frame().world().vitals().get(0).attackSequence());
        }
        var overflow=ModernNpcMeleeTest.catalog(Map.of(STR,Long.MAX_VALUE),true,Long.MAX_VALUE);
        try(var f=new Fixture(2,0,false,100,0,UnaryOperator.identity(),overflow,true)) {
            f.active();var npc=f.spawn(0,11,10);f.elapsed(100);var before=f.view();assertEquals(NUMERIC,f.step(npc).reason());assertEquals(before,f.view());
        }
    }
    @Test void hpAboveNativeWidthsSurvivesCommonOwnerWithoutNarrowingOrDoubleRounding()throws Exception {
        long huge=9_007_199_254_740_993L;var catalog=ModernNpcMeleeTest.catalog(Map.of(HP,huge,STR,5_000_000_000L),true,127);
        try(var f=new Fixture(2,0,false,huge,0,UnaryOperator.identity(),catalog,true)) {
            f.active();var npc=f.spawn(0,11,10);f.randomValue=19;f.elapsed(100);var hit=f.step(npc);assertEquals(ATTACKED,hit.reason());
            assertEquals(huge-hit.frame().exchanges().get(0).outcome().appliedDamage(),hit.frame().world().vitals().get(0).hp());
            var player=f.attack(0,1,npc);assertEquals(ATTACKED,player.reason());assertTrue(player.frame().npcs().actors().get(0).hp()>Integer.MAX_VALUE);
        }
    }
    @Test void timeoutClockRegressionAndCancellationDuringEitherResolverCommitNoExchange()throws Exception {
        for(int failure=0;failure<3;failure++)for(boolean player:new boolean[]{false,true})try(var f=new Fixture()) {
            f.active();var npc=f.spawn(0,11,10);f.elapsed(100);var before=f.view();int selected=failure;
            f.duringRandom=()->{if(selected==0)f.elapsed(60_000);else if(selected==1)f.clock.decrementAndGet();else f.rooms.releasePreparation(f.ticket);};
            var denied=player?f.attack(0,1,npc):f.step(npc);assertEquals(NOT_ACTIVE,denied.reason());
            assertEquals(ModernBattleEngine.Phase.ABORTED,denied.frame().world().phase());assertTrue(denied.frame().npcs().closed());
            assertEquals(before.world().vitals(),denied.frame().world().vitals());assertTrue(denied.frame().exchanges().isEmpty());
        }
    }
    @Test void equipmentExpiryDuringNpcOrPlayerResolutionAbortsWithoutCommittingHpOrSequence()throws Exception {
        for(boolean player:new boolean[]{false,true})try(var f=new Fixture(2,0,false,100,0,ModernBattleEngineNpcTest::expires,ModernNpcMeleeTest.catalog(),true)) {
            f.active();var npc=f.spawn(0,11,10);f.elapsed(100);var before=f.view();f.duringRandom=()->f.epoch.set(2000);
            var denied=player?f.attack(0,1,npc):f.step(npc);assertEquals(NOT_ACTIVE,denied.reason());
            assertEquals(ModernBattleEngine.AbortReason.SERVER_ABORT,denied.frame().world().abortReason());assertEquals(before.world().vitals(),denied.frame().world().vitals());
        }
    }
    static ModernPhysicalCombat.Source expires(ModernPhysicalCombat.Source source) {
        var q=source.input().equipment();var facts=new TreeMap<Long,ModernItemLifetime.Fact>();
        for(var item:q.source().inventory())facts.put(item.objectId(),new ModernItemLifetime.Fact(item.objectId(),item.template().catalogId(),item.template().kind(),item.equipped(),
                OptionalLong.of(0),false,OptionalLong.empty(),Optional.of("1970-01-01 00:00:01.000000"),0,false));
        var observation=new ModernItemLifetime.Observation(q.source().sourceId(),q.source().character().objectId(),"1970-01-01 00:00:00.000000",1000,0,facts);
        var qualified=new ModernEquipmentFacts.Qualified(q.source(),q.items(),q.catalog(),observation);
        return new ModernPhysicalCombat.Source(new ModernCombatProfile.Input(qualified,source.input().weaponsQualified(),source.input().weapons()),source.growth(),source.templates(),source.procCatalogIds());
    }
    @Test void concurrentDuplicateRequestAppliesOneHitAndKeepsSingleOwnerOfBothSides()throws Exception {
        try(var f=new Fixture()) {
            f.active();var npc=f.spawn(0,11,10);var workers=Executors.newFixedThreadPool(2);
            try {
                var tasks=List.of(workers.submit(()->{try{return f.attack(0,1,npc).reason()==ATTACKED;}catch(SecurityException e){return false;}}),
                        workers.submit(()->{try{return f.attack(0,1,npc).reason()==ATTACKED;}catch(SecurityException e){return false;}}));
                int accepted=0;for(var task:tasks)if(task.get(2,TimeUnit.SECONDS))accepted++;
                assertEquals(1,accepted);assertEquals(1,f.view().exchanges().size());assertEquals(1,f.view().world().vitals().get(0).attackSequence());
            } finally {workers.shutdownNow();assertTrue(workers.awaitTermination(2,TimeUnit.SECONDS));}
        }
    }
    @Test void participantDeathStopsCommandsWithoutFabricatedQuestWinAndNpcCorpseRetires()throws Exception {
        try(var f=new Fixture(2,0,true,1,0,UnaryOperator.identity(),ModernNpcMeleeTest.catalog(),true)) {
            f.active();var npc=f.spawn(0,11,10);f.elapsed(100);var death=f.step(npc).frame();assertEquals(0,death.world().vitals().get(0).hp());
            assertEquals(death.world().revision(),death.world().vitals().get(0).deathRevision());assertNull(death.world().result());assertEquals(ModernBattleEngine.Phase.ACTIVE,death.world().phase());
            assertEquals(DEAD,f.attack(0,1,npc).reason());assertEquals(NO_TARGET,f.step(npc).reason());f.elapsed(5000);assertEquals(0,f.view().world().vitals().get(0).hp());
        }
        try(var f=new Fixture(2,0,false,100,0,UnaryOperator.identity(),ModernNpcMeleeTest.catalog(Map.of(HP,1L),true,0),true)) {
            f.active();var npc=f.spawn(0,11,10);assertEquals(ATTACKED,f.attack(0,1,npc).reason());assertEquals(DEAD,f.step(npc).reason());
            f.elapsed(1000);assertTrue(f.view().npcs().actors().isEmpty());assertEquals(TARGET_DEAD,f.attack(0,2,npc).reason());
            var next=f.spawn(0,11,10);assertEquals(2,next.id().serial());assertEquals(TARGET_DEAD,f.attack(0,3,npc).reason());
        }
    }
    @Test void missedStrikesConsumeOnlyAttackersClockAndReceiptNotVictimHpOrHurt()throws Exception {
        try(var f=new Fixture(2,0,false,100,0,UnaryOperator.identity(),ModernNpcMeleeTest.catalog(Map.of(AC,-1000L),true,0),true)) {
            f.active();var npc=f.spawn(0,11,10);f.elapsed(100);var before=f.view();
            f.scripted=new long[]{0,99};var npcMiss=f.step(npc);assertEquals(ATTACKED,npcMiss.reason());assertEquals(2,f.scriptIndex);
            assertEquals(before.world().vitals(),npcMiss.frame().world().vitals());assertFalse(npcMiss.frame().exchanges().get(0).outcome().hit());
            assertEquals(850,npcMiss.frame().npcs().actors().get(0).nextAttackMillis());
            f.scriptIndex=0;var playerMiss=f.attack(0,1,npc);assertEquals(ATTACKED,playerMiss.reason());assertEquals(2,f.scriptIndex);
            assertEquals(npcMiss.frame().npcs(),playerMiss.frame().npcs());assertEquals(2,playerMiss.frame().exchanges().size());
            assertTrue(playerMiss.frame().exchanges().stream().allMatch(e->!e.outcome().hit()));assertEquals(TOO_FAST,f.attack(0,2,npc).reason());
        }
    }
    @Test void frozenCatalogPreventsForgedSpawnAndBatchFailureCannotPublishFirstNpc()throws Exception {
        try(var f=new Fixture()) {
            f.active();var before=f.view();var t=f.config.catalog().npcs().get(ModernNpcMeleeTest.NPC).rosterTemplate();
            var forged=new ModernNpcRoster.Template(t.catalogSourceId(),t.catalogDatabase(),t.npcId(),t.graphic(),t.maximumHp()+1,t.maximumMp(),t.walkMillis(),t.attackMillis());
            assertThrows(IllegalArgumentException.class,()->f.engine.spawnNpcs(f.scope,List.of(new ModernNpcRoster.Spawn(forged,0,1,cell(11,10),0))));
            var valid=new ModernNpcRoster.Spawn(t,0,1,cell(11,10),0);
            assertThrows(IllegalArgumentException.class,()->f.engine.spawnNpcs(f.scope,List.of(valid,new ModernNpcRoster.Spawn(t,1,1,cell(20,10),0))));
            assertEquals(before,f.view());assertEquals(1,f.spawn(0,11,10).id().serial());
        }
    }
    @Test void movementDuringEitherCalculationInvalidatesOldGeometryWithoutRollingBackValidMove()throws Exception {
        for(boolean player:new boolean[]{false,true})try(var f=new Fixture()) {
            f.active();var npc=f.spawn(0,11,10);f.elapsed(100);var before=f.view();
            f.duringRandom=()->assertEquals(ModernBattleEngine.MoveStatus.MOVED,f.move(0,1,10,10,0).status());
            var stale=player?f.attack(0,1,npc):f.step(npc);assertEquals(STALE_STATE,stale.reason());
            assertEquals(cell(10,9),stale.frame().world().actors().get(0).position());assertEquals(before.npcs(),stale.frame().npcs());
            assertEquals(100,stale.frame().world().vitals().get(0).hp());assertTrue(stale.frame().exchanges().isEmpty());
        }
    }
    @Test void nanosecondOriginMayWrapButNpcElapsedClockRemainsBoundedAndMonotonic()throws Exception {
        try(var f=new Fixture(2,0,false,100,0,UnaryOperator.identity(),ModernNpcMeleeTest.catalog(),false)) {
            f.clock.set(Long.MAX_VALUE-50*MS);f.engine=f.build(f.config);f.active();var npc=f.spawn(0,11,10);f.clock.addAndGet(100*MS);
            assertTrue(f.clock.get()<0);var hit=f.step(npc);assertEquals(ATTACKED,hit.reason());assertEquals(100,hit.frame().npcs().elapsedMillis());
            f.clock.decrementAndGet();assertEquals(ModernBattleEngine.AbortReason.CLOCK_REGRESSED,f.view().world().abortReason());
        }
    }
    @Test void forgedCombinedFrameCannotAliasOtherGenerationSourceOrDuplicateEvent()throws Exception {
        try(var f=new Fixture()) {
            f.active();var npc=f.spawn(0,11,10);var frame=f.attack(0,1,npc).frame();var n=frame.npcs();var e=frame.exchanges().get(0);
            assertThrows(IllegalArgumentException.class,()->new ModernNpcBattle.Frame(frame.world(),n,List.of(e,e)));
            var unknown=new ModernNpcBattle.Exchange(e.direction(),ModernParticipantId.human(99,1),1,e.npc(),e.npcLife(),e.requestSequence(),e.revision(),e.outcome());
            assertThrows(IllegalArgumentException.class,()->new ModernNpcBattle.Frame(frame.world(),n,List.of(unknown)));
            var wrong=new ModernNpcRoster.Snapshot(new ModernNpcRoster.Scope(f.scope.roomId(),f.scope.matchId(),f.scope.generation()+1),n.manifest(),n.revision(),n.elapsedMillis(),n.spawned(),false,List.of());
            assertThrows(IllegalArgumentException.class,()->new ModernNpcBattle.Frame(frame.world(),wrong,List.of()));
        }
    }
    @Test void actualShared554UsesEnginePursuitThenBothSidesDamageAndPlayerCooldownThroughCorpseRetirement()throws Exception {
        var map=ModernQuestMapQualificationTest.shared();var cells=map.serverChosenSpawns(ModernQuestMapQualificationTest.ANCHOR,100);
        try(var f=new Fixture(2,0,false,100,0,UnaryOperator.identity(),ModernNpcMeleeTest.catalog(Map.of(AC,-4L),true,0),true,map,List.of(cells.get(99),cells.get(1)))) {
            f.active();var npc=f.spawn(0,cells.get(0));f.randomValue=19;int moves=0;long millis=0;ModernNpcBattle.Result hit=null;
            for(int tick=1;tick<=165;tick++) {
                millis=tick*100L;f.elapsed(millis);var result=f.step(npc);
                if(result.reason()==ATTACKED){hit=result;break;}
                assertEquals(MOVED,result.reason());moves++;
            }
            assertNotNull(hit);assertTrue(moves>0);assertTrue(hit.frame().world().vitals().get(0).hp()<100);
            assertEquals(TOO_FAST,f.step(npc).reason());int strikes=0;long sequence=0;
            while(f.view().npcs().actors().get(0).alive()) {
                assertTrue(strikes<80);var result=f.attack(0,++sequence,npc);assertEquals(ATTACKED,result.reason());strikes++;
                if(!result.frame().npcs().actors().get(0).alive())break;
                assertEquals(TOO_FAST,f.attack(0,++sequence,npc).reason());millis+=700;f.elapsed(millis);
            }
            var corpse=f.view().npcs().actors().get(0);assertEquals(ModernNpcRoster.Action.DEATH,corpse.action());assertNull(f.view().world().result());
            f.elapsed(corpse.corpseUntilMillis());assertTrue(f.view().npcs().actors().isEmpty());
            System.out.println("shared554-java commonEngine=true moves="+moves+" committedPlayerStrikes="+strikes
                    +" bothHpOwners=one npcAndPlayerClocks=true corpseRetired=true worldWire=false runtimeQuest=false");
        }
    }
}
