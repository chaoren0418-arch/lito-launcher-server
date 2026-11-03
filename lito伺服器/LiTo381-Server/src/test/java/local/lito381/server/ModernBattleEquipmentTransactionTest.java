package local.lito381.server;

import static local.lito381.server.ModernEquipmentAuthority.*;
import static local.lito381.server.ModernEquipmentOverrideStore.*;
import static local.lito381.server.ModernEquipmentFacts.*;
import static local.lito381.server.ModernEquipmentFacts.Stat.*;
import static local.lito381.server.ModernEquipmentEvaluatorTest.*;
import static local.lito381.server.ModernCombatProfileTest.binding;
import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Actual authority + durable store + captured-room engine. Internal seam tests,
 * not a claim that protocol <= 34 supports in-world equipment messages. */
class ModernBattleEquipmentTransactionTest {
    @TempDir Path root;

    @Test void actualDurablePublicationReturnsTheSameCompleteHistoryAsTheLiveOwner()throws Exception {
        try(var f=new Fixture(root,new FileOps(){},true,100,false,false)) {
            var gear=f.read();f.active();f.attack(1,1,1);var p=f.players.get(0);
            var before=f.engine.equipmentHistory(p.owner(),p.scope(),f.ticket.matchId(),f.ticket.generation());
            var down=f.set(gear,2,false);var history=down.publication().history();assertNotNull(history);
            assertEquals(down.publication().resources().balance(),history.resources());assertTrue(ModernEquipmentHistory.followsReprofile(before,history));
            assertEquals(history,ModernEquipmentHistoryWire.decodeBound(ModernEquipmentHistoryWire.encode(history),history.identity(),history.worldRevision()));
            assertEquals(history,f.engine.equipmentHistory(p.owner(),p.scope(),f.ticket.matchId(),f.ticket.generation()));
            var same=f.set(down.equipment().snapshot(),2,false);assertEquals(ModernBattleEngine.EquipmentDisposition.UNCHANGED,same.publication().disposition());
            assertEquals(history,same.publication().history());assertEquals(1,f.stored().orElseThrow().revision());
        }
    }

    @Test void durableUnequipChangesActualAttacksAndNextEquipDoesNotResetCooldown() throws Exception {
        try (var f=new Fixture(root,new FileOps(){},false,1000,false,false)) {
            var old=f.read();var before=f.active();
            var first=f.attack(0,1,2);assertEquals(ModernBattleEngine.AttackStatus.ATTACKED,first.status());
            long oldDamage=before.vitals().get(1).hp()-first.snapshot().vitals().get(1).hp();
            int reads=f.physicalReads.get();int gearReads=f.gearReads.get();
            var reply=f.set(old,1,false);
            assertEquals(OK,reply.equipment().status());assertTrue(reply.equipment().changed());
            assertEquals(ModernBattleEngine.EquipmentDisposition.APPLIED,reply.publication().disposition());
            assertEquals(1,f.stored().orElseThrow().revision());assertEquals(1,reply.publication().equipmentRevision());
            assertEquals(reads+1,f.physicalReads.get());assertEquals(gearReads,f.gearReads.get());
            var after=f.view();assertEquals(0,after.vitals().get(0).weaponType());
            assertEquals(first.snapshot().vitals().get(0).attackRevision(),after.vitals().get(0).attackRevision());
            assertEquals(first.snapshot().actors(),after.actors());
            assertEquals(ModernBattleEngine.AttackStatus.TOO_FAST,f.attack(0,2,2).status());
            f.nanos.addAndGet(TimeUnit.SECONDS.toNanos(1));
            long hp=f.view().vitals().get(1).hp();var next=f.attack(0,3,2);
            assertEquals(ModernBattleEngine.AttackStatus.ATTACKED,next.status());
            assertTrue(hp-next.snapshot().vitals().get(1).hp()<oldDamage);
            f.set(reply.equipment().snapshot(),1,true);
            assertEquals(2,f.stored().orElseThrow().revision());
            assertEquals(ModernBattleEngine.AttackStatus.TOO_FAST,f.attack(0,4,2).status());
        }
    }

    @Test void damageDuringForceAndRenameIsNotRestoredAndIoDoesNotHoldEngineMonitor() throws Exception {
        var ref=new AtomicReference<Fixture>();var during=new AtomicReference<ModernBattleEngine.Snapshot>();
        FileOps files=new FileOps(){
            @Override public void force(FileChannel file)throws IOException {
                FileOps.super.force(file);during.set(onOtherThread(()->ref.get().attack(1,1,1).snapshot()));
            }
            @Override public void move(Path from,Path to)throws IOException {
                FileOps.super.move(from,to);
                ref.get().nanos.addAndGet(TimeUnit.SECONDS.toNanos(1));
                during.set(onOtherThread(()->ref.get().attack(1,2,1).snapshot()));
            }
        };
        try(var f=new Fixture(root,files,false,1000,false,false)) {
            ref.set(f);var old=f.read();f.active();var reply=f.set(old,1,false);
            var actual=f.view();var intervening=during.get().vitals().get(0);var current=actual.vitals().get(0);
            assertEquals(intervening.hp(),current.hp());assertTrue(current.hp()<1000);
            assertEquals(intervening.hurtRevision(),current.hurtRevision());
            assertEquals(during.get().vitals().get(1),actual.vitals().get(1));
            assertEquals(during.get().revision()+1,actual.revision());
            assertEquals(actual.revision(),reply.publication().worldRevision());
            var balance=reply.publication().resources().balance();
            assertEquals(1000-current.hp(),balance.flows().damaged());assertEquals(0,balance.flows().hpClipped());
            assertEquals(current.hp(),balance.vitals().hp());assertEquals(f.resources(),reply.publication().resources());
        }
    }

    @Test void maximumClippingAndReequipUseLiveValuesNotFullHealthOrSavedSql() throws Exception {
        try(var f=new Fixture(root,new FileOps(){},true,100,false,false)) {
            var old=f.read();var before=f.active();assertEquals(1000,before.vitals().get(0).maxHp());
            var damaged=f.attack(1,1,1);long liveHp=damaged.snapshot().vitals().get(0).hp();
            var down=f.set(old,2,false);var clipped=f.view().vitals().get(0);
            assertEquals(100,clipped.hp());assertEquals(100,clipped.maxHp());
            assertEquals(50,clipped.mp());assertEquals(50,clipped.maxMp());
            var balances=down.publication().resources().balance();
            assertEquals(1000,balances.birthHp());assertEquals(250,balances.birthMp());
            assertEquals(liveHp-100,balances.flows().hpClipped());assertEquals(200,balances.flows().mpClipped());
            assertEquals(1000-liveHp,balances.flows().damaged());assertEquals(0,balances.flows().spent());
            var again=f.set(down.equipment().snapshot(),2,true);var up=f.view().vitals().get(0);
            assertEquals(balances.flows(),again.publication().resources().balance().flows());
            assertTrue(ModernVitalBalance.followsEquipment(balances,again.publication().resources().balance()));
            assertEquals(1000,up.maxHp());assertEquals(100,up.hp());
            assertEquals(250,up.maxMp());assertEquals(50,up.mp());
        }
    }

    @Test void preAdmissionForceFailureDoesNotChangeWorldOrStoreAndReservationCanRetry() throws Exception {
        var failures=new AtomicInteger(1);
        try(var f=new Fixture(root,new FileOps(){
            @Override public void force(FileChannel file)throws IOException {
                if(failures.getAndDecrement()>0)throw new IOException("fixture force failure");FileOps.super.force(file);
            }
        },false,100,false,false)) {
            var old=f.read();var before=f.active();var failed=f.set(old,1,false);
            assertEquals(BACKEND_UNAVAILABLE,failed.equipment().status());assertNull(failed.publication());
            assertEquals(before,f.view());assertTrue(f.stored().isEmpty());assertNoPending(root);
            assertEquals(OK,f.set(old,1,false).equipment().status());
        }
    }

    @Test void uncertainMoveBeforeOrAfterRenameAbortsInsteadOfPretendingRollback() throws Exception {
        for(boolean moved:List.of(false,true))try(var f=new Fixture(root.resolve("move-"+moved),new FileOps(){
            @Override public void move(Path from,Path to)throws IOException {
                if(moved)FileOps.super.move(from,to);throw new IOException("fixture ambiguous rename");
            }
        },false,100,false,false)) {
            var old=f.read();var before=f.active();
            var failure=assertThrows(StoreException.class,()->f.set(old,1,false));
            assertEquals(Failure.COMMIT_UNCERTAIN,failure.failure());
            assertEquals(moved,f.stored().isPresent());
            var stopped=f.view();assertEquals(ModernBattleEngine.Phase.ABORTED,stopped.phase());
            assertEquals(before.vitals(),stopped.vitals());assertNoPending(f.path);
        }
    }

    @Test void cancelledSelectionDuringForcePreventsRenameWithoutStoppingUnchangedBattle() throws Exception {
        var ref=new AtomicReference<Fixture>();var moves=new AtomicInteger();
        try(var f=new Fixture(root,new FileOps(){
            @Override public void force(FileChannel file)throws IOException {FileOps.super.force(file);ref.get().lease.invalidate();}
            @Override public void move(Path from,Path to)throws IOException {moves.incrementAndGet();FileOps.super.move(from,to);}
        },false,100,false,false)) {
            ref.set(f);var old=f.read();var before=f.active();
            assertEquals(Failure.INVALID_OPERATION,assertThrows(StoreException.class,()->f.set(old,1,false)).failure());
            assertEquals(0,moves.get());assertTrue(f.stored().isEmpty());assertEquals(before,f.view());assertNoPending(root);
        }
    }

    @Test void deathBeforeAdmissionRejectsWriteAndDeathAfterAdmissionIsNeverResurrected() throws Exception {
        for(boolean afterAdmission:List.of(false,true)) {
            var ref=new AtomicReference<Fixture>();var death=new AtomicReference<ModernBattleEngine.Snapshot>();
            try(var f=new Fixture(root.resolve("death-"+afterAdmission),new FileOps(){
                @Override public void force(FileChannel file)throws IOException {
                    FileOps.super.force(file);if(!afterAdmission)death.set(ref.get().attack(1,1,1).snapshot());
                }
                @Override public void move(Path from,Path to)throws IOException {
                    FileOps.super.move(from,to);if(afterAdmission)death.set(ref.get().attack(1,1,1).snapshot());
                }
            },false,1,false,false)) {
                ref.set(f);var old=f.read();f.active();
                if(afterAdmission)assertEquals(ModernBattleEngine.EquipmentDisposition.APPLIED,f.set(old,1,false).publication().disposition());
                else assertEquals(Failure.INVALID_OPERATION,assertThrows(StoreException.class,()->f.set(old,1,false)).failure());
                var actual=f.view();assertEquals(0,actual.vitals().get(0).hp());
                assertEquals(death.get().vitals().get(0).deathRevision(),actual.vitals().get(0).deathRevision());
                assertEquals(afterAdmission,f.stored().isPresent());
                var balance=f.resources().balance();assertEquals(0,balance.vitals().hp());
                assertEquals(1,balance.flows().damaged());assertEquals(0,balance.flows().hpGranted());
                assertEquals(0,balance.life());assertEquals(actual.vitals().get(0).deathRevision(),f.resources().deathRevision());
                assertThrows(IllegalStateException.class,f::reserve);
            }
        }
    }

    @Test void matchFinishingAfterRenamePreservesExactTerminalResultAndRecordsPersistedRetired() throws Exception {
        var ref=new AtomicReference<Fixture>();var terminal=new AtomicReference<ModernBattleEngine.Snapshot>();
        try(var f=new Fixture(root,new FileOps(){
            @Override public void move(Path from,Path to)throws IOException {
                FileOps.super.move(from,to);terminal.set(ref.get().attack(1,1,1).snapshot());
            }
        },false,1,true,false)) {
            ref.set(f);var old=f.read();f.active();var reply=f.set(old,1,false);
            assertEquals(ModernBattleEngine.Phase.FINISHED,terminal.get().phase());assertNotNull(terminal.get().result());
            assertEquals(ModernBattleEngine.EquipmentDisposition.PERSISTED_RETIRED,reply.publication().disposition());
            assertEquals(BACKEND_UNAVAILABLE,reply.equipment().status());assertNull(reply.equipment().snapshot());
            assertNull(reply.publication().resources());
            assertEquals(terminal.get(),f.view());assertEquals(1,f.stored().orElseThrow().revision());
        }
    }

    @Test void cancelledPreparationBeforeRenamePreventsWriteAndAfterRenameRetiresPublication() throws Exception {
        for(boolean afterAdmission:List.of(false,true)) {
            var ref=new AtomicReference<Fixture>();
            try(var f=new Fixture(root.resolve("retire-"+afterAdmission),new FileOps(){
                private void retire(){var value=ref.get();value.rooms.rooms.releasePreparation(value.ticket);}
                @Override public void force(FileChannel file)throws IOException {FileOps.super.force(file);if(!afterAdmission)retire();}
                @Override public void move(Path from,Path to)throws IOException {FileOps.super.move(from,to);if(afterAdmission)retire();}
            },false,100,false,false)) {
                ref.set(f);var old=f.read();var before=f.active();
                if(afterAdmission)assertEquals(ModernBattleEngine.EquipmentDisposition.PERSISTED_RETIRED,f.set(old,1,false).publication().disposition());
                else assertEquals(Failure.INVALID_OPERATION,assertThrows(StoreException.class,()->f.set(old,1,false)).failure());
                assertEquals(afterAdmission,f.stored().isPresent());assertEquals(before.vitals(),f.view().vitals());
                assertEquals(ModernBattleEngine.Phase.ABORTED,f.view().phase());
            }
        }
    }

    @Test void closedEngineAfterRenameCannotBeReopenedByPublication() throws Exception {
        var ref=new AtomicReference<Fixture>();
        try(var f=new Fixture(root,new FileOps(){
            @Override public void move(Path from,Path to)throws IOException {FileOps.super.move(from,to);ref.get().engine.close();}
        },false,100,false,false)) {
            ref.set(f);var old=f.read();f.active();var reply=f.set(old,1,false);
            assertEquals(ModernBattleEngine.EquipmentDisposition.PERSISTED_RETIRED,reply.publication().disposition());
            assertTrue(f.engine.isClosed());assertEquals(0,f.engine.ownerCount());assertTrue(f.stored().isPresent());
        }
    }

    @Test void conflictAndNoopDoNotCreateStoreOrWorldRevisions() throws Exception {
        try(var f=new Fixture(root,new FileOps(){},false,100,false,false)) {
            var old=f.read();var before=f.active();
            var wrong=new Snapshot(old.source(),old.revision(),ModernEquipmentOverrideStoreTest.fp(99),old.catalogGeneration(),
                    old.evaluatedAtMillis(),old.nextStoneExpiry(),old.modifiers(),old.magicReductionBase(),old.equipped(),old.stale(),old.lifetimes());
            assertEquals(CONFLICT,f.set(wrong,1,false).equipment().status());assertEquals(before,f.view());
            var unchanged=f.set(old,1,true);assertFalse(unchanged.equipment().changed());
            assertEquals(ModernBattleEngine.EquipmentDisposition.UNCHANGED,unchanged.publication().disposition());
            assertEquals(f.resources(),unchanged.publication().resources());
            assertEquals(before,f.view());assertTrue(f.stored().isEmpty());
            assertEquals(OK,f.set(old,1,false).equipment().status());
        }
    }

    @Test void sourceChangesSinceSpawnAreRejectedBeforeAnyDurableIntent() throws Exception {
        try(var f=new Fixture(root,new FileOps(){},false,100,false,false)) {
            var old=f.read();var before=f.active();
            f.physicalOverride=ModernPhysicalCombatTest.withVitals(7,f.sources.get(0),101,10);
            assertThrows(IllegalStateException.class,()->f.set(old,1,false));
            assertTrue(f.stored().isEmpty());assertEquals(before,f.view());
        }
    }

    @Test void sameSqlRowWithChangedGrowthCannotSilentlyRefreshEvenAnIdempotentIntent() throws Exception {
        try(var f=new Fixture(root,new FileOps(){},false,100,false,false)) {
            var old=f.read();var before=f.active();var s=f.sources.get(0);
            f.physicalOverride=new ModernPhysicalCombat.Source(s.input(),ModernPhysicalCombatTest.growth(7,999,999),s.templates(),s.procCatalogIds());
            for(boolean desired:List.of(false,true))assertThrows(IllegalStateException.class,()->f.set(old,1,desired));
            assertTrue(f.stored().isEmpty());assertEquals(before,f.view());
        }
    }

    @Test void sameSqlAndPhysicalStatsCannotSilentlyImportAnUnpinnedMagicProjection()throws Exception {
        try(var f=new Fixture(root,new FileOps(){},false,100,false,false)) {
            var old=f.read();var before=f.active();
            f.physicalOverride=ModernMagicCombatTest.enable(f.sources.get(0));
            for(boolean desired:List.of(false,true))assertThrows(IllegalStateException.class,()->f.set(old,1,desired));
            assertTrue(f.stored().isEmpty());assertEquals(before,f.view());
        }
    }
    @Test void cancellationAfterRenameDrainsPublicationBeforeWithholdingAck() throws Exception {
        var ref=new AtomicReference<Fixture>();
        try(var f=new Fixture(root,new FileOps(){
            @Override public void move(Path from,Path to)throws IOException {
                FileOps.super.move(from,to);ref.get().lease.invalidate();
                ModernBackendScope.current().cancel(ModernBackendExecutor.Failure.CLOSED);
            }
        },false,100,false,false)) {
            ref.set(f);var old=f.read();f.active();
            assertThrows(ModernBackendExecutor.BackendException.class,()->f.set(old,1,false));
            assertEquals(1,f.stored().orElseThrow().revision());assertEquals(0,f.view().vitals().get(0).weaponType());
            // The old lease is retired, but its durable operation is fully
            // resolved; a new trusted selection is not stuck behind its ticket.
            try(var next=f.reserve()){}
        }
    }

    @Test void regressedClockAfterRenameCannotProduceAnActiveSuccessOrUndoTheSavedRevision() throws Exception {
        var ref=new AtomicReference<Fixture>();
        try(var f=new Fixture(root,new FileOps(){
            @Override public void move(Path from,Path to)throws IOException {FileOps.super.move(from,to);ref.get().epoch.set(999);}
        },false,100,false,false)) {
            ref.set(f);var old=f.read();var before=f.active();
            assertThrows(IOException.class,()->f.set(old,1,false));
            assertEquals(1,f.stored().orElseThrow().revision());
            assertEquals(ModernBattleEngine.AbortReason.CLOCK_REGRESSED,f.view().abortReason());
            assertEquals(before.vitals(),f.view().vitals());
        }
    }

    @Test void opaqueOwnerAndFullWorldScopeAreRequiredAndParallelReservationIsDenied() throws Exception {
        try(var f=new Fixture(root,new FileOps(){},false,100,false,false)) {
            var old=f.read();f.active();var p=f.players.get(0);var s=p.scope();
            for(var wrong:List.of(new ModernRooms.Scope(42,s.objectId(),s.epoch(),s.channelId()),
                    new ModernRooms.Scope(s.sourceId(),s.objectId()+1,s.epoch(),s.channelId()),
                    new ModernRooms.Scope(s.sourceId(),s.objectId(),s.epoch()+1,s.channelId()),
                    new ModernRooms.Scope(s.sourceId(),s.objectId(),s.epoch(),1)))
                assertThrows(SecurityException.class,()->f.engine.beginEquipmentChange(p.owner(),wrong,f.ticket.matchId(),f.ticket.generation()));
            assertThrows(SecurityException.class,()->f.engine.beginEquipmentChange(f.players.get(1).owner(),s,f.ticket.matchId(),f.ticket.generation()));
            assertThrows(SecurityException.class,()->f.engine.beginEquipmentChange(p.owner(),s,f.ticket.matchId()+1,f.ticket.generation()));
            assertThrows(SecurityException.class,()->f.engine.beginEquipmentChange(p.owner(),s,f.ticket.matchId(),f.ticket.generation()+1));
            try(var held=f.reserve()){assertThrows(IllegalStateException.class,f::reserve);}
            int reads=f.physicalReads.get();
            assertThrows(SecurityException.class,()->f.call(new Object(),1,op->f.authority.executeInMatch(f.principal,f.row(),op,
                    f.request(old,1,false),f.reserve())));
            assertThrows(SecurityException.class,()->f.call(p.owner(),2,op->f.authority.executeInMatch(f.principal,f.row(),op,
                    f.request(old,1,false),f.reserve())));
            assertEquals(reads,f.physicalReads.get());assertTrue(f.stored().isEmpty());
            assertEquals(OK,f.set(old,1,false).equipment().status());
        }
    }

    @Test void loadingAndUnqualifiedItemProjectionCannotEnterInternalTransaction() throws Exception {
        try(var f=new Fixture(root.resolve("loading"),new FileOps(){},false,100,false,false)) {
            assertThrows(IllegalStateException.class,f::reserve);f.active();try(var ignored=f.reserve()){}
        }
        try(var f=new Fixture(root.resolve("items"),new FileOps(){},false,100,false,true)) {
            f.active();assertThrows(IllegalStateException.class,f::reserve);assertTrue(f.stored().isEmpty());
        }
    }

    @Test void resourceProofIsBoundToOwnerSelectionSourceChannelAndWorld()throws Exception {
        try(var f=new Fixture(root,new FileOps(){},false,100,false,false)) {
            f.active();var p=f.players.get(0);var proof=f.resources();
            assertEquals(p.scope(),proof.scope());assertEquals(f.ticket.matchId(),proof.matchId());
            assertEquals(f.ticket.generation(),proof.generation());assertEquals(1,proof.entityId());
            assertThrows(SecurityException.class,()->f.engine.equipmentResources(f.players.get(1).owner(),p.scope(),f.ticket.matchId(),f.ticket.generation()));
            for(var wrong:List.of(new ModernRooms.Scope(42,3333,1,0),new ModernRooms.Scope(7,3334,1,0),
                    new ModernRooms.Scope(7,3333,2,0),new ModernRooms.Scope(7,3333,1,1)))
                assertThrows(SecurityException.class,()->f.engine.equipmentResources(p.owner(),wrong,f.ticket.matchId(),f.ticket.generation()));
            assertThrows(SecurityException.class,()->f.engine.equipmentResources(p.owner(),p.scope(),f.ticket.matchId()+1,f.ticket.generation()));
            assertThrows(SecurityException.class,()->f.engine.equipmentResources(p.owner(),p.scope(),f.ticket.matchId(),f.ticket.generation()+1));
        }
    }
    @Test void repeatedActualEquipmentCommitsDoNotCreateRecoveryDamageCastsOrLifeGrants()throws Exception {
        try(var f=new Fixture(root,new FileOps(){},true,100,false,false)) {
            var gear=f.read();f.active();
            for(int i=1;i<=20;i++) {
                var reply=f.set(gear,2,i%2==0);gear=reply.equipment().snapshot();
                var b=f.resources().balance();assertEquals(i,b.equipmentRevision());
                assertEquals(100,b.vitals().hp());assertEquals(50,b.vitals().mp());
                assertEquals(900,b.flows().hpClipped());assertEquals(200,b.flows().mpClipped());
                assertEquals(0,b.flows().damaged());assertEquals(0,b.flows().restored());
                assertEquals(0,b.flows().spent());assertEquals(0,b.life());assertEquals(0,b.flows().hpGranted());
                assertEquals(0,f.view().vitals().get(0).hurtRevision());
            }
        }
    }
    private static <T> T onOtherThread(java.util.concurrent.Callable<T> action)throws IOException {
        var worker=Executors.newSingleThreadExecutor();
        try{return worker.submit(action).get(3,TimeUnit.SECONDS);}
        catch(Exception failure){throw new IOException("world monitor retained during equipment I/O",failure);}
        finally{worker.shutdownNow();try{assertTrue(worker.awaitTermination(3,TimeUnit.SECONDS));}catch(InterruptedException failure){Thread.currentThread().interrupt();}}
    }
    private static void assertNoPending(Path path)throws IOException {
        try(var files=Files.list(path)){assertTrue(files.noneMatch(p->p.getFileName().toString().endsWith(".tmp")));}
    }
    @FunctionalInterface private interface Work<T>{T run(ModernEquipmentLease.Operation operation)throws Exception;}
    private static final class Fixture implements AutoCloseable {
        final Path path;final ModernRoomsTest.Fixture rooms=new ModernRoomsTest.Fixture();
        final List<ModernRoomsTest.Player> players=new ArrayList<>();final List<ModernPhysicalCombat.Source> sources=new ArrayList<>();
        final ModernRooms.FrozenPreparation ticket;final ModernBattleEngine engine;final ModernWorldMap map;
        final AtomicLong nanos=new AtomicLong(1_000_000_000L),epoch=new AtomicLong(1000);
        final AtomicInteger physicalReads=new AtomicInteger(),gearReads=new AtomicInteger();
        final ModernEquipmentOverrideStore store;final ModernEquipmentAuthority authority;
        final ModernAccountAuthentication.Principal principal=new ModernAccountAuthentication.Principal("fixture-owner",Set.of(new ModernAccountAuthentication.Source(7,"source-7")));
        ModernEquipmentLease lease;ModernPhysicalCombat.Source physicalOverride;
        Fixture(Path path,FileOps files,boolean bonuses,long hp,boolean settle,boolean items)throws Exception {
            this.path=path;Files.createDirectories(path);store=new ModernEquipmentOverrideStore(path,files);
            players.add(rooms.player(7,3333,30,0));players.add(rooms.player(42,3333,30,0));
            ModernRoomsTest.accepted(rooms.create(players.get(0),ModernRoomsTest.draft(100,1,514,0)));
            ModernRoomsTest.accepted(rooms.join(players.get(1),rooms.view(players.get(0)).joined().summary(),""));
            for(var p:players)ModernRoomsTest.accepted(rooms.act(p,ModernRooms.Action.READY,1));
            var summary=rooms.view(players.get(0)).joined().summary();
            var prep=rooms.rooms.prepareWorld(players.get(0).owner(),players.get(0).scope(),summary.id(),summary.revision());
            ModernRoomsTest.accepted(prep.result());ticket=prep.ticket();
            var sector=new ModernWorldMap.SectorKey(0x8000,0x8000);
            map=ModernWorldMap.fromSectorBytes(514,Map.of(sector,ModernWorldMapTest.openSectorBytes()),()->false);
            var prepared=new ArrayList<ModernBattleEngine.PreparedMember>();
            for(int i=0;i<ticket.participants().size();i++) {
                var p=ticket.participants().get(i);var m=p.member();
                var row=new ModernApplicationProtocol.CharacterRow(m.key().objectId(),m.name(),m.characterClass(),m.sex(),m.level());
                var source=source(m.key().sourceId(),row,hp,bonuses);sources.add(source);
                var fighter=ModernPhysicalCombat.qualify(source,binding(source.input().equipment()),Optional.empty(),1000);
                var cell=new ModernWorldMap.Cell(sector.origin().x()+2+i,sector.origin().y()+2);
                prepared.add(items?new ModernBattleEngine.PreparedMember(p.owner(),p.scope(),row,cell,200_000_000,fighter,1_000_000_000,null,0,false,List.of())
                        :new ModernBattleEngine.PreparedMember(p.owner(),p.scope(),row,cell,200_000_000,fighter,1_000_000_000));
            }
            engine=new ModernBattleEngine(ticket,new ModernWorldBinding(1,514,map),prepared,
                    new ModernBattleEngine.Limits(10_000,0,60_000),nanos::get,epoch::get,bound->0,settle);
            authority=new ModernEquipmentAuthority((p,s,o)->{gearReads.incrementAndGet();var q=sources.get(0).input().equipment();return new Loaded(q,binding(q).fingerprint());},null,
                    (p,s,o)->{physicalReads.incrementAndGet();var value=physicalOverride==null?sources.get(0):physicalOverride;return new PhysicalLoaded(value,binding(value.input().equipment()).fingerprint());},store,epoch::get);
        }
        private static ModernPhysicalCombat.Source source(int id,ModernApplicationProtocol.CharacterRow row,long hp,boolean bonuses) {
            var value=ModernPhysicalCombatTest.source(id,row.characterClass(),row.level(),List.of(
                    ModernPhysicalCombatTest.weapon(1,1,true,0,bonus(MELEE_DAMAGE,20),TemplatePhysical.zero()),
                    simpleArmor(2,2,bonuses?bonus(MAX_HP,900,MAX_MP,200,AC,-7):Ability.zero())));
            value=ModernPhysicalCombatTest.withVitals(id,value,hp,10);var old=value.input().equipment();
            var snapshot=new ModernCharacterData.Snapshot(id,row,old.source().persisted(),old.source().inventory());
            var q=new Qualified(snapshot,old.items(),old.catalog(),ModernItemLifetime.Observation.verifiedPermanent(snapshot));
            return new ModernPhysicalCombat.Source(new ModernCombatProfile.Input(q,true,value.input().weapons()),value.growth(),value.templates(),value.procCatalogIds());
        }
        ModernApplicationProtocol.CharacterRow row(){return sources.get(0).input().equipment().source().character();}
        <T>T call(Object owner,long selectionEpoch,Work<T> work)throws Exception {
            var key=binding(sources.get(0).input().equipment()).key();var scope=ModernEquipmentOverrideStoreTest.scope();
            lease=new ModernEquipmentLease.Issuer().open(owner,key,selectionEpoch,()->{});
            try(var operation=lease.operation(scope)) {ModernBackendScope.bind(scope);try{return work.run(operation);}finally{ModernBackendScope.unbind();}}
            finally{lease.invalidate();scope.finish();}
        }
        <T>T call(Work<T> work)throws Exception{return call(players.get(0).owner(),1,work);}
        Snapshot read()throws Exception{return call(op->authority.execute(principal,row(),op,new Request(7,3333,1,READ,false,0,0,ZERO)).snapshot());}
        Optional<Stored> stored()throws Exception{return call(op->store.read(op.key(),op.scope()));}
        Request request(Snapshot before,long item,boolean desired){return new Request(7,3333,1,SET,desired,item,before.revision(),before.viewToken());}
        MatchResult set(Snapshot before,long item,boolean desired)throws Exception{return call(op->authority.executeInMatch(principal,row(),op,request(before,item,desired),reserve()));}
        ModernBattleEngine.EquipmentChange reserve(){var p=players.get(0);return engine.beginEquipmentChange(p.owner(),p.scope(),ticket.matchId(),ticket.generation());}
        ModernBattleEngine.Snapshot view(){var p=players.get(0);return engine.snapshot(p.owner(),p.scope(),ticket.matchId(),ticket.generation());}
        ModernBattleEngine.EquipmentResources resources(){var p=players.get(0);return engine.equipmentResources(p.owner(),p.scope(),ticket.matchId(),ticket.generation());}
        ModernBattleEngine.Snapshot active(){for(var p:players)engine.ackLoaded(p.owner(),p.scope(),ticket.matchId(),ticket.generation(),map.digest());return view();}
        ModernBattleEngine.AttackResult attack(int index,long sequence,long target){var p=players.get(index);return engine.attack(p.owner(),p.scope(),ticket.matchId(),ticket.generation(),sequence,target);}
        @Override public void close(){engine.close();rooms.rooms.releasePreparation(ticket);for(var p:players){rooms.rooms.disconnect(p.owner());rooms.presence.disconnect(p.owner());}store.close();assertTrue(store.isQuiescent());}
    }
}
