package local.lito381.server;

import static org.junit.jupiter.api.Assertions.*;
import static local.lito381.server.ModernWorldProtocol.Status;
import static local.lito381.server.ModernWorldProtocol.Phase;
import static local.lito381.server.ModernWorldProtocol.AbortReason;
import static local.lito381.server.ModernWorldProtocol.SettlementState;
import static local.lito381.server.ModernWorldProtocol.ZERO_DIGEST;
import static local.lito381.server.ModernVipEquipmentProtocol.*;
import static local.lito381.server.ModernVipEquipment.Field.MAX_HP;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Real Rooms/preparation worker/WorldService/engine/file CAS/settlement. The
 * source provider and open map are controlled fixtures, not Main/TLS/SQL/UI. */
class ModernVipWorldServiceTest {
    @TempDir Path directory;
    static final class Fixture implements AutoCloseable {
        final ModernRoomsTest.Fixture room=new ModernRoomsTest.Fixture();
        final List<ModernRoomsTest.Player> players=List.of(room.player(7,3333,30,0),room.player(42,3333,30,0));
        final List<ModernPhysicalCombat.Source> sources=new ArrayList<>();
        final ModernWorldMap map=ModernWorldMap.fromSectorBytes(514,Map.of(new ModernWorldMap.SectorKey(0x8000,0x8000),ModernWorldMapTest.openSectorBytes()),()->false);
        final ModernEquipmentOverrideStore store;final ModernVipEquipmentAuthority gear;final ModernWorldService service;
        final ModernWorldService.Mapping mapping;final ModernSessionBoundary.Negotiated peer;
        final ModernAccountAuthentication.Principal principal=new ModernAccountAuthentication.Principal("vip-service",
                Set.of(new ModernAccountAuthentication.Source(7,"sun"),new ModernAccountAuthentication.Source(42,"moon")),
                Map.of(7,"sun-vip-service",42,"moon-vip-service"));
        final AtomicInteger sourceReads=new AtomicInteger(),gearReads=new AtomicInteger(),settlements=new AtomicInteger();
        final AtomicReference<ModernMatchSettlement.Event> event=new AtomicReference<>();
        final AtomicReference<Runnable> beforeRead=new AtomicReference<>();
        final AtomicReference<Runnable> beforeMove=new AtomicReference<>();
        boolean missingVip;ModernTargetHistory.World world;
        Fixture(Path path)throws Exception{this(path,48,4_294_967_299L);}
        Fixture(Path path,int extensions,long hp)throws Exception {
            store=new ModernEquipmentOverrideStore(path,new ModernEquipmentOverrideStore.FileOps(){
                @Override public void move(Path from,Path to)throws java.io.IOException {
                    var hook=beforeMove.getAndSet(null);if(hook!=null)hook.run();
                    ModernEquipmentOverrideStore.FileOps.super.move(from,to);
                }
            });
            var origin=new ModernWorldMap.SectorKey(0x8000,0x8000).origin();
            mapping=new ModernWorldService.Mapping(new ModernWorldBinding(1,514,map),new ModernWorldMap.Cell(origin.x()+10,origin.y()+10),1_000_000,1_000_000).withFullVip(true);
            peer=new ModernSessionBoundary.Negotiated(1,47,1,ModernBattleReadProtocolTest.CAPS|ModernSessionProtocol.CAP_ACTION_LIFE,extensions);
            ModernSessionExtensions.requireContext(peer);
            service=new ModernWorldService(room.rooms,Map.of(mapping.binding().key(),mapping),
                    new ModernWorldService.Limits(2,1,0,10000,10,new ModernBattleEngine.Limits(15000,0,60000)),b->0,
                    (e,d)->{event.set(e);settlements.incrementAndGet();},Map.of());
            ModernRoomsTest.accepted(room.create(players.get(0),ModernRoomsTest.draft(100,1,514,0)));
            ModernRoomsTest.accepted(room.join(players.get(1),room.view(players.get(0)).joined().summary(),""));
            for(var p:players)ModernRoomsTest.accepted(room.act(p,ModernRooms.Action.READY,1));
            for(int i=0;i<2;i++) {
                final int index=i;var p=players.get(i);var member=room.view(p).joined().members().stream().filter(m->m.key().equals(new TournamentPresence.CharacterKey(p.scope().sourceId(),p.scope().objectId()))).findFirst().orElseThrow();
                var row=new ModernApplicationProtocol.CharacterRow(3333,member.name(),member.characterClass(),member.sex(),member.level());
                sources.add(ModernBattleEngineVipTest.source(p.scope().sourceId(),row,Map.of(MAX_HP,hp),false));
                ModernWorldService.FullVipReader reader=(deadline,armor)->{
                    sourceReads.incrementAndGet();var hook=beforeRead.getAndSet(null);if(hook!=null)hook.run();assertFalse(armor);
                    var s=sources.get(index);var binding=ModernCombatProfileTest.binding(s.input().equipment());
                    if(missingVip)return ModernPhysicalCombat.qualify(s,binding,Optional.empty(),System.currentTimeMillis());
                    return call(index,op->{var now=System.currentTimeMillis();var state=ModernVipCharacterState.start(
                            new ModernVipLifecycle.Actor(UUID.randomUUID(),op.key(),1,0),s,binding,store.read(op.key(),op.scope()),now,0);
                        return ModernPhysicalCombat.qualifyVip(state,now);});
                };
                service.register(p.owner(),p.scope(),d->row,reader,true,null,false,null,false,false,false,false,false,false,peer);
            }
            gear=new ModernVipEquipmentAuthority((principal,id,obj)->{gearReads.incrementAndGet();var s=sources.get(id==7?0:1);
                return new ModernEquipmentAuthority.PhysicalLoaded(s,ModernCombatProfileTest.binding(s.input().equipment()).fingerprint());},store,System::currentTimeMillis);
        }
        ModernRoomsTest.Player player(int i){return players.get(i);}
        ModernApplicationProtocol.CharacterRow row(int i){return sources.get(i).input().equipment().source().character();}
        ModernWorldService.VipWorldReply prepare() {
            var summary=room.view(player(0)).joined().summary();var result=service.vipWorld(player(0).owner(),
                    new ModernBattleWorldProtocol.Request(player(0).scope(),ModernBattleWorldProtocol.Action.PREPARE,summary.id(),summary.revision(),0,0,ZERO_DIGEST));
            assertEquals(Status.OK,result.status());assertEquals(Phase.PREPARING,result.lifecycle().phase());assertNull(result.payload());world=result.world();return result;
        }
        ModernBattleWorldProtocol.Request request(int i,ModernBattleWorldProtocol.Action action) {
            return new ModernBattleWorldProtocol.Request(player(i).scope(),action,world.roomId(),0,world.matchId(),world.generation(),action==ModernBattleWorldProtocol.Action.LOADED?map.digest():ZERO_DIGEST);
        }
        ModernWorldService.VipWorldReply read(int i){return service.vipWorld(player(i).owner(),request(i,ModernBattleWorldProtocol.Action.READ));}
        ModernBattleReadProtocol.Reply data(int i){return service.readBattle(player(i).owner(),new ModernBattleReadProtocol.Request(player(i).scope(),world));}
        void active()throws Exception {
            prepare();await(()->read(0).lifecycle().phase()!=Phase.PREPARING);
            assertEquals(Phase.LOADING,read(0).lifecycle().phase());
            for(int i=0;i<2;i++)assertEquals(Status.OK,service.vipWorld(player(i).owner(),request(i,ModernBattleWorldProtocol.Action.LOADED)).status());
            ModernBattlePublicationTest.stopScheduler(service);assertEquals(Phase.ACTIVE,read(0).lifecycle().phase());
        }
        Object entry()throws Exception{return ModernBattlePublicationTest.entry(service,player(0).owner());}
        ModernBattleEngine engine()throws Exception{return (ModernBattleEngine)ModernBattlePublicationTest.field(entry(),"engine");}
        ModernBattleVipCommandState.Proof proof(int i){return ModernBattleVipCommandState.from(data(i).vip().view());}
        Reply inspect(int i)throws Exception{return equipment(i,Request.inspect(1,proof(i)));}
        Reply equipment(int i,Request q)throws Exception {
            return call(i,op->service.vipEquipment(player(i).owner(),new ModernApplicationProtocol.VipEquipmentRequest(q),gear,principal,row(i),op).reply());
        }
        Reply set(int i,Reply quote,long item,boolean desired)throws Exception {
            return equipment(i,new Request(2,ModernBattleVipCommandState.from(quote.payload().view()),Action.SET,item,desired,quote.token()));
        }
        <T>T call(int i,Work<T> work)throws Exception {
            var p=player(i);var scope=ModernEquipmentOverrideStoreTest.scope();var lease=new ModernEquipmentLease.Issuer().open(p.owner(),new TournamentPresence.CharacterKey(p.scope().sourceId(),p.scope().objectId()),p.scope().epoch(),()->{});
            try(var operation=lease.operation(scope)){ModernBackendScope.bind(scope);try{return work.run(operation);}finally{ModernBackendScope.unbind();}}
            finally{lease.invalidate();scope.finish();}
        }
        Optional<ModernEquipmentOverrideStore.Stored> stored(int i)throws Exception{return call(i,op->store.read(op.key(),op.scope()));}
        @Override public void close(){service.close();store.close();for(var p:players){room.rooms.disconnect(p.owner());room.presence.disconnect(p.owner());}assertTrue(service.isQuiescent());}
    }
    @FunctionalInterface interface Work<T>{T run(ModernEquipmentLease.Operation operation)throws Exception;}
    static void await(java.util.function.BooleanSupplier condition)throws Exception {
        long deadline=System.nanoTime()+TimeUnit.SECONDS.toNanos(8);
        while(!condition.getAsBoolean()){if(System.nanoTime()>=deadline)fail("bounded service observation timed out");Thread.sleep(2);}
    }
    @Test void realServicePreparesLoadsPublishesCompletePrivateOwnersAndAdvancesClocks()throws Exception {
        try(var f=new Fixture(directory)) {
            f.active();var a=f.data(0);var b=f.data(1);assertNull(a.view());assertNull(b.view());
            assertEquals(3333,a.vip().view().personal().scope().objectId());assertEquals(3333,b.vip().view().personal().scope().objectId());
            assertNotEquals(a.vip().view().personal().scope().sourceId(),b.vip().view().personal().scope().sourceId());
            assertEquals(4_294_967_399L,a.vip().view().personal().equipment().effective().maximumHp());
            assertEquals(2,f.sourceReads.get());var before=a.vip().view().world();Thread.sleep(2);var after=f.data(0).vip().view().world();
            assertEquals(before.revision(),after.revision());assertTrue(after.clock().activeElapsedNanos()>before.clock().activeElapsedNanos());
            var publication=(ModernBattlePublication)ModernBattlePublicationTest.field(f.entry(),"publication");
            assertSame(publication.vip(f.player(0).scope()).view().world(),publication.vip(f.player(1).scope()).view().world());
            assertThrows(SecurityException.class,()->publication.audience(f.player(0).scope()));assertThrows(SecurityException.class,()->publication.owner(f.player(0).scope()));
            assertThrows(IllegalArgumentException.class,()->ModernLegacyBattleProjection.from(publication.capture()));
            assertNotNull(ModernApplicationProtocol.encode(new ModernApplicationProtocol.BattleReadResult(7,new ModernBattleReadProtocol.Request(f.player(0).scope(),f.world),f.data(0)),f.peer));
        }
    }
    @Test void liveInspectionCasAndStaleReplyUseExactServicePublicationWithoutTouchingSibling()throws Exception {
        try(var f=new Fixture(directory)) {
            f.active();var first=f.inspect(0);assertEquals(Outcome.INSPECTED,first.outcome());
            assertEquals(Outcome.UNCHANGED,f.set(0,first,1,true).outcome());assertTrue(f.stored(0).isEmpty());
            var removed=f.set(0,first,1,false);assertEquals(Outcome.APPLIED,removed.outcome());assertEquals(1,removed.durableRevision());
            assertEquals(100,f.data(0).vip().view().personal().equipment().effective().maximumHp());
            var stale=f.set(0,first,1,false);assertEquals(ModernBattleVipCommandState.Rejection.STALE_SAVED_EQUIPMENT,stale.rejection());
            var equipped=f.set(0,removed,1,true);assertEquals(2,equipped.durableRevision());
            assertEquals(4_294_967_399L,equipped.payload().view().personal().equipment().effective().maximumHp());
            assertEquals(100,equipped.payload().view().personal().history().resources().vitals().hp());
            assertEquals(2,f.stored(0).orElseThrow().revision());assertTrue(f.stored(1).isEmpty());
            assertEquals(0,f.data(1).vip().view().personal().equipment().versions().savedEquipmentRevision());
            assertNotNull(ModernApplicationProtocol.encode(new ModernApplicationProtocol.VipEquipmentResult(equipped),f.peer));
        }
    }
    @Test void dataOnlySourceCannotUseDedicatedEquipmentPath()throws Exception {
        try(var f=new Fixture(directory,16,100)) {f.active();assertThrows(SecurityException.class,()->f.inspect(0));assertEquals(0,f.gearReads.get());assertTrue(f.stored(0).isEmpty());}
    }
    @Test void wrongConnectionSelectionAndForeignLeaseFailBeforeProviderOrSave()throws Exception {
        try(var f=new Fixture(directory)) {
            f.active();var q=Request.inspect(3,f.proof(0));
            assertThrows(SecurityException.class,()->f.equipment(1,q));
            assertThrows(SecurityException.class,()->f.call(1,op->f.service.vipEquipment(f.player(0).owner(),new ModernApplicationProtocol.VipEquipmentRequest(q),f.gear,f.principal,f.row(0),op)));
            var scope=f.player(0).scope();var foreign=new ModernRooms.Scope(scope.sourceId(),scope.objectId(),scope.epoch()+1,scope.channelId());
            assertThrows(SecurityException.class,()->f.service.readBattle(f.player(0).owner(),new ModernBattleReadProtocol.Request(foreign,f.world)));
            assertEquals(0,f.gearReads.get());assertTrue(f.stored(0).isEmpty());assertTrue(f.stored(1).isEmpty());
        }
    }
    @Test void oldWorldAndCombatCommandsRejectBeforeMutationOrRetirement()throws Exception {
        try(var f=new Fixture(directory)) {
            f.active();var p=f.player(0);long before=f.engine().serviceObservation().revision();
            assertThrows(SecurityException.class,()->f.service.battleWorld(p.owner(),f.request(0,ModernBattleWorldProtocol.Action.LEAVE)));
            assertThrows(SecurityException.class,()->f.service.handle(p.owner(),new ModernWorldProtocol.Request(p.scope(),ModernWorldProtocol.Action.LEAVE,0,0,0,f.world.matchId(),f.world.generation(),0,0,0,ZERO_DIGEST)));
            assertThrows(SecurityException.class,()->f.service.combat(p.owner(),new ModernCombatProtocol.Request(p.scope(),ModernCombatProtocol.Action.ATTACK,f.world.matchId(),f.world.generation(),1,2)));
            var pose=f.engine().serviceObservation().actors().get(0).pose();
            var move=new ModernApplicationProtocol.WorldRequest(9,new ModernWorldProtocol.Request(p.scope(),ModernWorldProtocol.Action.MOVE,0,0,0,f.world.matchId(),f.world.generation(),1,pose.position().x(),pose.position().y(),ZERO_DIGEST));
            var guarded=new ModernApplicationProtocol.BattleActionRequest(9,new ModernBattleActionProtocol.Request(f.proof(0).life(),ModernBattleActionProtocol.NO_TARGET,move));
            assertThrows(SecurityException.class,()->f.service.canonicalAction(p.owner(),guarded));
            assertEquals(before,f.engine().serviceObservation().revision());assertEquals(Phase.ACTIVE,f.read(0).lifecycle().phase());assertTrue(f.stored(0).isEmpty());
        }
    }
    @Test void engineWinIsPublishedSavedOnceAndRetainedForBothFollowers()throws Exception {
        try(var f=new Fixture(directory,48,-99)) {
            f.active();var p=f.player(0);var out=f.engine().attack(p.owner(),p.scope(),f.world.matchId(),f.world.generation(),1,2);
            assertEquals(ModernBattleEngine.AttackStatus.ATTACKED,out.status());f.service.tickNow();
            await(()->f.read(0).lifecycle().settlement()==SettlementState.SAVED);
            var result=f.read(0);assertEquals(Phase.FINISHED,result.lifecycle().phase());assertEquals(f.event.get(),result.lifecycle().result());
            assertEquals(result.lifecycle(),f.read(1).lifecycle());assertEquals(1,f.settlements.get());
            for(int i=0;i<2;i++){var denied=f.inspect(i);assertEquals(Outcome.REJECTED,denied.outcome());assertEquals(ModernBattleVipCommandState.Rejection.NOT_ACTIVE,denied.rejection());assertEquals(SettlementState.SAVED,denied.lifecycle().settlement());}
            assertNull(f.service.vipWorld(p.owner(),f.request(0,ModernBattleWorldProtocol.Action.LEAVE)).world());
            assertEquals(SettlementState.SAVED,f.read(1).lifecycle().settlement());
            assertNull(f.service.vipWorld(f.player(1).owner(),f.request(1,ModernBattleWorldProtocol.Action.LEAVE)).world());assertEquals(0,f.service.matchCount());
        }
    }
    @Test void disconnectPublishesAbortToSiblingAndPreventsLateReads()throws Exception {
        try(var f=new Fixture(directory)) {
            f.active();f.service.detach(f.player(0).owner());var remaining=f.read(1);assertEquals(Phase.ABORTED,remaining.lifecycle().phase());
            assertEquals(AbortReason.DISCONNECTED,remaining.lifecycle().abortReason());assertNotNull(remaining.payload());
            assertThrows(SecurityException.class,()->f.data(0));assertEquals(Outcome.REJECTED,f.inspect(1).outcome());
            f.service.vipWorld(f.player(1).owner(),f.request(1,ModernBattleWorldProtocol.Action.LEAVE));assertEquals(0,f.service.matchCount());
        }
    }
    @Test void disconnectAfterCommitAdmissionRetainsDurableSaveButNeverAcknowledgesRetiredOwner()throws Exception {
        try(var f=new Fixture(directory)) {
            f.active();var quote=f.inspect(0);
            f.beforeMove.set(()->f.service.detach(f.player(0).owner()));
            assertThrows(SecurityException.class,()->f.set(0,quote,1,false));
            assertEquals(1,f.stored(0).orElseThrow().revision());assertTrue(f.stored(1).isEmpty());
            assertEquals(Phase.ABORTED,f.read(1).lifecycle().phase());assertThrows(SecurityException.class,()->f.data(0));
            f.service.vipWorld(f.player(1).owner(),f.request(1,ModernBattleWorldProtocol.Action.LEAVE));
            assertEquals(0,f.service.matchCount());
        }
    }
    @Test void claimedFullReaderReturningOldFighterFailsPreparationWithoutLegacyFallback()throws Exception {
        try(var f=new Fixture(directory)) {
            f.missingVip=true;f.prepare();await(()->f.read(0).lifecycle().phase()!=Phase.PREPARING);
            assertEquals(Phase.ABORTED,f.read(0).lifecycle().phase());assertEquals(AbortReason.PREPARATION_FAILED,f.read(0).lifecycle().abortReason());
            assertNull(f.read(0).payload());assertEquals(ModernBattleReadProtocol.State.UNAVAILABLE,f.data(0).state());assertEquals(0,f.gearReads.get());
        }
    }
    @Test void preparationCancellationKeepsWorkerCapacityUntilActualReaderExit()throws Exception {
        var entered=new CountDownLatch(1);var release=new CountDownLatch(1);
        try(var f=new Fixture(directory)) {
            f.beforeRead.set(()->{entered.countDown();boolean interrupted=false;while(release.getCount()!=0){try{release.await(100,TimeUnit.MILLISECONDS);}catch(InterruptedException e){interrupted=true;}}if(interrupted)Thread.currentThread().interrupt();});
            try {
                f.prepare();assertTrue(entered.await(3,TimeUnit.SECONDS));f.service.detach(f.player(0).owner());assertEquals(Phase.ABORTED,f.read(1).lifecycle().phase());
                f.service.vipWorld(f.player(1).owner(),f.request(1,ModernBattleWorldProtocol.Action.LEAVE));assertFalse(f.service.isQuiescent());
            } finally {release.countDown();}
            await(f.service::isQuiescent);assertEquals(0,f.service.matchCount());assertEquals(0,f.gearReads.get());
        }
    }
    @Test void mappingCopiesPreserveExplicitVipAndRefuseUnsupportedWorlds()throws Exception {
        try(var f=new Fixture(directory)) {
            var m=f.mapping;for(var copy:List.of(m.withClockOverrides(new ModernWorldService.ClockOverrides(12000,0L,60000)),m.withRace(null),
                    m.withDisintegrate(false),m.withGreaterHaste(false),m.withHaste(false),m.withSlow(false),m.withPlayerHealing(false),m.withArmorProtection(false)))assertTrue(copy.fullVipEnabled());
            assertFalse(m.withFullVip(false).fullVipEnabled());
            var bot=new ModernWorldService.Mapping(m.binding(),m.anchor(),1_000_000,1_000_000,0,false,true);assertThrows(IllegalArgumentException.class,()->bot.withFullVip(true));
            int id=BattleRules.Mode.HONOR_MATCH.maps().get(0);
            var honorMap=ModernWorldMap.fromSectorBytes(id,Map.of(new ModernWorldMap.SectorKey(0x8000,0x8000),ModernWorldMapTest.openSectorBytes()),()->false);
            var other=new ModernWorldService.Mapping(new ModernWorldBinding(5,id,honorMap),m.anchor(),1_000_000,1_000_000);assertThrows(IllegalArgumentException.class,()->other.withFullVip(true));
        }
    }
}
