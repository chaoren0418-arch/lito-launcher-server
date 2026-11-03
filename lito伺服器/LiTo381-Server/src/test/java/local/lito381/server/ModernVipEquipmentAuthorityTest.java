package local.lito381.server;

import static org.junit.jupiter.api.Assertions.*;
import static local.lito381.server.ModernVipEquipment.Field.*;
import static local.lito381.server.ModernVipEquipmentAuthority.Status.*;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Exact selected-owner lease, real engine, provider and isolated durable store.
 * Provider rows/clocks are artificial; this is not Main/TLS/SQL/UI admission. */
class ModernVipEquipmentAuthorityTest {
    @TempDir Path directory;
    @Test void readNoOpUnequipReequipAndSourceIsolationUseOneBundlePerCall()throws Exception {
        try(var f=new Fixture(directory,false,Map.of(MAX_HP,4_294_967_299L,MAX_MP,4_294_967_307L))) {
            var first=f.inspect(0);var sibling=f.inspect(1);assertEquals(2,f.reads.get());assertTrue(f.stored(0).isEmpty());
            assertNotEquals(first.token(),sibling.token());assertEquals(3333,first.proof().life().player().objectId());
            assertEquals(UNCHANGED,f.set(0,first,1,true).status());assertTrue(f.stored(0).isEmpty());
            var removed=f.set(0,first,1,false);assertEquals(APPLIED,removed.status());
            assertEquals(1,removed.publication().equipmentRevision());assertEquals(100,removed.quote().view().personal().equipment().effective().maximumHp());
            assertEquals(1,f.stored(0).orElseThrow().revision());assertTrue(f.stored(1).isEmpty());
            assertEquals(0,sibling.proof().versions().savedEquipmentRevision());
            var equipped=f.set(0,removed.quote(),1,true);assertEquals(APPLIED,equipped.status());
            assertEquals(4_294_967_399L,equipped.quote().view().personal().equipment().effective().maximumHp());
            assertEquals(100,equipped.quote().view().personal().history().resources().vitals().hp());
            assertEquals(2,f.stored(0).orElseThrow().revision());assertEquals(5,f.reads.get());
            assertEquals(equipped.quote().token(),f.inspect(0).token());
            assertEquals("VipEquipmentQuote[redacted]",equipped.quote().toString());
            assertEquals("VipEquipmentAuthorityResult[redacted]",equipped.toString());
        }
    }
    @Test void expiryKeepsSavedZeroAndNextOrdinarySavePublishesRuntimeTwo()throws Exception {
        try(var f=new Fixture(directory,true,Map.of(MAX_HP,100L))) {
            var old=f.inspect(0);f.world.epoch.set(2000);
            var rejected=f.world.engine.admitVipEquipmentChange(f.player(0).owner(),old.proof());
            assertEquals(ModernBattleVipCommandState.Rejection.STALE_RUNTIME_ABILITY,rejected.rejection());
            var fresh=f.inspect(0);assertEquals(new ModernBattleVipObservation.Versions(0,1,1),fresh.proof().versions());
            var result=f.set(0,fresh,3,true);assertEquals(APPLIED,result.status());
            assertEquals(new ModernBattleVipObservation.Versions(1,2,2),result.quote().proof().versions());
            assertEquals(100,result.quote().view().personal().history().resources().vitals().hp());
            assertEquals(140,result.quote().view().personal().equipment().effective().maximumHp());
            assertEquals(1,f.stored(0).orElseThrow().revision());
        }
    }
    @Test void fullCatalogReloadConflictsWithOldQuoteAndOnlyReequipAdoptsNewValues()throws Exception {
        try(var f=new Fixture(directory,false,Map.of(MAX_HP,100L,CHA,5L))) {
            var old=f.inspect(0);f.sources.set(0,ModernVipLifecycleTest.change(f.sources.get(0),300086,CHA,9000,0,null));
            var conflict=f.set(0,old,3,true);assertEquals(CONFLICT,conflict.status());assertNull(conflict.publication());assertTrue(f.stored(0).isEmpty());
            var fresh=f.inspect(0);assertNotEquals(old.token(),fresh.token());assertEquals(5,fresh.view().personal().equipment().vipAbilities().get(CHA));
            var armor=f.set(0,fresh,3,true);assertEquals(APPLIED,armor.status());assertEquals(5,armor.quote().view().personal().equipment().vipAbilities().get(CHA));
            var removed=f.set(0,armor.quote(),1,false);var reequipped=f.set(0,removed.quote(),1,true);
            assertEquals(APPLIED,reequipped.status());assertEquals(9000,reequipped.quote().view().personal().equipment().vipAbilities().get(CHA));
            assertEquals(3,f.stored(0).orElseThrow().revision());assertTrue(f.stored(1).isEmpty());
        }
    }
    @Test void catalogDeletionStillAllowsRemovalOfActualAppliedTemplate()throws Exception {
        try(var f=new Fixture(directory,false,Map.of(MAX_HP,100L))) {
            var before=f.inspect(0);var s=f.sources.get(0);
            f.sources.set(0,ModernVipLoadoutTest.withCatalog(s,new ModernVipEquipment.Catalog(7,"catalog",Map.of(),Map.of(),Set.of(300086L))));
            assertEquals(CONFLICT,f.set(0,before,1,false).status());
            var current=f.inspect(0);var removed=f.set(0,current,1,false);assertEquals(APPLIED,removed.status());
            assertTrue(removed.quote().view().personal().equipment().vipItems().isEmpty());
            assertEquals(DENIED,f.set(0,removed.quote(),1,true).status());assertEquals(1,f.stored(0).orElseThrow().revision());
        }
    }
    @Test void ordinarySetAfterReloadRetainsEarlierFreshDeadline()throws Exception {
        try(var f=new Fixture(directory,true,Map.of(MAX_HP,100L))) {
            f.world.epoch.set(1100);
            f.sources.set(0,ModernVipLifecycleTest.timed(f.sources.get(0),1000,"2026-10-01 00:00:00.500000",1));
            var result=f.set(0,f.inspect(0),3,true);assertEquals(APPLIED,result.status());
            assertEquals(1500,result.quote().view().personal().equipment().validUntilEpochMillis());
            f.world.epoch.set(1500);var next=f.inspect(0);
            assertEquals(new ModernBattleVipObservation.Versions(1,2,2),next.proof().versions());
            assertTrue(next.view().personal().equipment().vipItems().isEmpty());assertEquals(1,f.stored(0).orElseThrow().revision());
        }
    }
    @Test void laterSqlClockCannotResurrectExpiredRawEquippedVipDuringOrdinarySet()throws Exception {
        try(var f=new Fixture(directory,true,Map.of(MAX_HP,100L))) {
            f.world.epoch.set(2100);f.world.view();
            f.sources.set(0,ModernVipLifecycleTest.timed(f.sources.get(0),1800,"2026-10-01 00:00:00",1));
            var before=f.inspect(0);assertTrue(before.view().personal().equipment().source().inventory().get(0).equipped());
            var result=f.set(0,before,3,true);assertEquals(APPLIED,result.status());assertEquals(140,result.quote().view().personal().equipment().effective().maximumHp());
            assertTrue(result.quote().view().personal().equipment().vipItems().isEmpty());
            var denied=f.set(0,result.quote(),1,true);assertEquals(DENIED,denied.status());assertEquals(EquipmentRules.Reason.UNQUALIFIED_ITEM,denied.denial());
            assertEquals(1,f.stored(0).orElseThrow().revision());
        }
    }
    @Test void forgedOwnerOperationAndUnauthorizedPrincipalFailBeforeProvider()throws Exception {
        try(var f=new Fixture(directory,false,Map.of(MAX_HP,100L))) {
            var proof=f.proof(0);int calls=f.reads.get();
            try(var change=f.reserve(0,proof)) {
                assertThrows(SecurityException.class,()->f.call(1,op->f.authority.inspect(f.principal,f.row(0),op,proof,change)));
            }
            try(var change=f.reserve(0,proof)) {
                var wrong=new ModernAccountAuthentication.Principal("other",Set.of(new ModernAccountAuthentication.Source(42,"other")));
                assertThrows(SecurityException.class,()->f.call(0,op->f.authority.inspect(wrong,f.row(0),op,proof,change)));
            }
            try(var change=f.reserve(0,proof)) {
                assertThrows(SecurityException.class,()->f.call(0,new Object(),op->f.authority.inspect(f.principal,f.row(0),op,proof,change)));
            }
            assertEquals(calls,f.reads.get());assertTrue(f.stored(0).isEmpty());assertNotNull(f.inspect(0));
        }
    }
    @Test void reservedStateCannotBeReplacedByDifferentRequestVersionsOrForeignToken()throws Exception {
        try(var f=new Fixture(directory,false,Map.of(MAX_HP,100L))) {
            var quote=f.inspect(0);var foreign=f.inspect(1);var forged=new ModernVipEquipmentAuthority.Quote(quote.view(),foreign.token());
            assertEquals(CONFLICT,f.set(0,forged,3,true).status());assertTrue(f.stored(0).isEmpty());
            var proof=quote.proof();var wrong=new ModernBattleVipCommandState.Proof(proof.life(),new ModernBattleVipObservation.Versions(0,0,1));
            try(var change=f.reserve(0,proof)) {
                assertThrows(SecurityException.class,()->f.call(0,op->f.authority.execute(f.principal,f.row(0),op,
                        new ModernVipEquipmentAuthority.Request(wrong,3,true,quote.token()),change)));
            }
        }
    }
    @Test void changedBaseInstanceSourceAndBindingNeverCreateHybridSave()throws Exception {
        try(var f=new Fixture(directory,false,Map.of(MAX_HP,100L))) {
            var original=f.sources.get(0);var q=original.input().equipment();var quote=f.inspect(0);
            f.sources.set(0,ModernVipLoadoutTest.withEquipment(original,ModernCombatProfileTest.withVitals(q,999,50,50,20,10)));
            assertThrows(IllegalArgumentException.class,()->f.set(0,quote,3,true));assertTrue(f.stored(0).isEmpty());
            f.sources.set(0,f.sources.get(1));assertThrows(IOException.class,()->f.set(0,quote,3,true));
            f.sources.set(0,original);var distinctBinding=new byte[32];distinctBinding[0]=1;
            f.bindingOverride=new ModernEquipmentOverrideStore.Fingerprint(distinctBinding);
            assertThrows(IllegalArgumentException.class,()->f.set(0,quote,3,true));assertTrue(f.stored(0).isEmpty());
            f.bindingOverride=null;assertEquals(APPLIED,f.set(0,quote,3,true).status());
        }
    }
    @Test void sourceReadReturningOnlyLegacyVipDataCannotAuthorizeEvenNoOp()throws Exception {
        try(var f=new Fixture(directory,false,Map.of(MAX_HP,100L))) {
            var quote=f.inspect(0);var s=f.sources.get(0);
            f.sources.set(0,new ModernPhysicalCombat.Source(s.input(),s.growth(),s.templates(),s.procCatalogIds(),s.magicGrowth(),s.magicBows(),
                    s.weaponArrowSprites(),s.regenGrowth(),s.healingGrowth(),null,s.armorProcs(),s.weaponProcCatalogIds()));
            assertThrows(IOException.class,()->f.set(0,quote,1,true));assertTrue(f.stored(0).isEmpty());
        }
    }
    @Test void expiredBetweenForceAndAdmissionDoesNotWriteOrAbortHealthyWorld()throws Exception {
        var force=new AtomicReference<Runnable>();
        try(var f=new Fixture(directory,true,Map.of(MAX_HP,100L),ops(force,new AtomicReference<>()))) {
            var quote=f.inspect(0);force.set(()->f.world.epoch.set(2000));
            var failure=assertThrows(ModernEquipmentOverrideStore.StoreException.class,()->f.set(0,quote,1,false));
            assertEquals(ModernEquipmentOverrideStore.Failure.INVALID_OPERATION,failure.failure());assertTrue(f.stored(0).isEmpty());
            assertEquals(ModernBattleEngine.Phase.ACTIVE,f.world.view().phase());assertNotNull(f.inspect(0));
        }
    }
    @Test void invalidatedSelectionBeforeAdmissionCannotSave()throws Exception {
        var force=new AtomicReference<Runnable>();
        try(var f=new Fixture(directory,false,Map.of(MAX_HP,100L),ops(force,new AtomicReference<>()))) {
            var quote=f.inspect(0);force.set(()->f.activeLease.invalidate());
            var failure=assertThrows(ModernEquipmentOverrideStore.StoreException.class,()->f.set(0,quote,1,false));
            assertEquals(ModernEquipmentOverrideStore.Failure.INVALID_OPERATION,failure.failure());assertTrue(f.stored(0).isEmpty());
            assertEquals(ModernBattleEngine.Phase.ACTIVE,f.world.view().phase());
        }
    }
    @Test void concurrentDamageIsPreservedByRealAuthorityPublication()throws Exception {
        var move=new AtomicReference<Runnable>();var hp=new AtomicReference<Long>();
        try(var f=new Fixture(directory,false,Map.of(MAX_HP,100L),ops(new AtomicReference<>(),move))) {
            var quote=f.inspect(0);move.set(()->{var attack=f.world.attack(1,1,1);assertEquals(ModernBattleEngine.AttackStatus.ATTACKED,attack.status());
                hp.set(attack.observation().actors().get(0).resources().vitals().hp());});
            var result=f.set(0,quote,1,false);assertEquals(APPLIED,result.status());assertTrue(hp.get()<200);
            assertEquals(Math.min(100,hp.get()),result.quote().view().personal().history().resources().vitals().hp());
            assertTrue(result.quote().view().personal().history().resources().flows().damaged()>0);
        }
    }
    @Test void expiryAfterDurableAdmissionRetainsSaveButNotLiveSuccess()throws Exception {
        var move=new AtomicReference<Runnable>();
        try(var f=new Fixture(directory,true,Map.of(MAX_HP,100L),ops(new AtomicReference<>(),move))) {
            var quote=f.inspect(0);move.set(()->{f.world.epoch.set(2000);f.world.view();});
            var result=f.set(0,quote,1,false);assertEquals(PERSISTED_RETIRED,result.status());assertNull(result.quote());
            assertEquals(1,result.publication().equipmentRevision());assertEquals(1,f.stored(0).orElseThrow().revision());
            assertEquals(ModernBattleEngine.Phase.ABORTED,result.observation().phase());
        }
    }
    @Test void cancellationAfterAdmissionSuppressesAckWithoutUndoingDurableOrLiveState()throws Exception {
        var move=new AtomicReference<Runnable>();
        try(var f=new Fixture(directory,false,Map.of(MAX_HP,100L),ops(new AtomicReference<>(),move))) {
            var quote=f.inspect(0);move.set(()->{f.activeLease.invalidate();f.activeScope.cancel(ModernBackendExecutor.Failure.CANCELLED);});
            assertThrows(ModernBackendExecutor.BackendException.class,()->f.set(0,quote,1,false));
            assertEquals(1,f.stored(0).orElseThrow().revision());
            assertEquals(1,f.proof(0).versions().savedEquipmentRevision());assertEquals(ModernBattleEngine.Phase.ACTIVE,f.world.view().phase());
        }
    }
    @Test void uncertainRenameDoesNotPretendRollbackOrLeaveActiveReservation()throws Exception {
        try(var f=new Fixture(directory,false,Map.of(MAX_HP,100L),new ModernEquipmentOverrideStore.FileOps(){
            @Override public void move(Path from,Path to)throws IOException {
                ModernEquipmentOverrideStore.FileOps.super.move(from,to);throw new IOException("injected unknown move outcome");
            }
        })) {
            var quote=f.inspect(0);var failure=assertThrows(ModernEquipmentOverrideStore.StoreException.class,()->f.set(0,quote,1,false));
            assertEquals(ModernEquipmentOverrideStore.Failure.COMMIT_UNCERTAIN,failure.failure());
            assertEquals(1,f.stored(0).orElseThrow().revision());assertEquals(ModernBattleEngine.Phase.ABORTED,f.world.view().phase());
        }
    }
    @Test void unknownNonEquipmentAndDuplicateIntentsHaveDistinctResults()throws Exception {
        try(var f=new Fixture(directory,false,Map.of(MAX_HP,100L))) {
            var before=f.inspect(0);var missing=f.set(0,before,99999,true);assertEquals(DENIED,missing.status());assertEquals(EquipmentRules.Reason.UNKNOWN_ITEM,missing.denial());
            assertTrue(f.stored(0).isEmpty());var applied=f.set(0,before,3,true);assertEquals(APPLIED,applied.status());
            assertEquals(UNCHANGED,f.set(0,applied.quote(),3,true).status());assertEquals(1,f.stored(0).orElseThrow().revision());
            var rejected=f.world.engine.admitVipEquipmentChange(f.player(0).owner(),before.proof());
            assertEquals(ModernBattleVipCommandState.Rejection.STALE_SAVED_EQUIPMENT,rejected.rejection());
        }
    }
    static ModernEquipmentOverrideStore.FileOps ops(AtomicReference<Runnable> force,AtomicReference<Runnable> move) {
        return new ModernEquipmentOverrideStore.FileOps(){
            @Override public void force(FileChannel file)throws IOException {ModernEquipmentOverrideStore.FileOps.super.force(file);var r=force.getAndSet(null);if(r!=null)r.run();}
            @Override public void move(Path from,Path to)throws IOException {var r=move.getAndSet(null);if(r!=null)r.run();ModernEquipmentOverrideStore.FileOps.super.move(from,to);}
        };
    }
    @FunctionalInterface interface Work<T>{T run(ModernEquipmentLease.Operation operation)throws Exception;}
    static final class Fixture implements AutoCloseable {
        final ModernBattleEngineVipTest.Fixture world;final ModernEquipmentOverrideStore store;final ModernVipEquipmentAuthority authority;
        final List<ModernPhysicalCombat.Source> sources=new ArrayList<>();final AtomicInteger reads=new AtomicInteger();
        final ModernAccountAuthentication.Principal principal=new ModernAccountAuthentication.Principal("vip-authority-test",
                Set.of(new ModernAccountAuthentication.Source(7,"source7"),new ModernAccountAuthentication.Source(42,"source42")),
                Map.of(7,"sun-vip-test",42,"moon-vip-test"));
        ModernEquipmentOverrideStore.Fingerprint bindingOverride;ModernEquipmentLease activeLease;ModernBackendScope activeScope;
        Fixture(Path directory,boolean timed,Map<ModernVipEquipment.Field,Long> fields)throws Exception {this(directory,timed,fields,new ModernEquipmentOverrideStore.FileOps(){});}
        Fixture(Path directory,boolean timed,Map<ModernVipEquipment.Field,Long> fields,ModernEquipmentOverrideStore.FileOps files)throws Exception {
            world=new ModernBattleEngineVipTest.Fixture(timed,fields);sources.addAll(world.sources);store=new ModernEquipmentOverrideStore(directory,files);
            authority=new ModernVipEquipmentAuthority((principal,id,obj)->{reads.incrementAndGet();var s=sources.get(id==7?0:1);
                return new ModernEquipmentAuthority.PhysicalLoaded(s,bindingOverride==null?ModernCombatProfileTest.binding(s.input().equipment()).fingerprint():bindingOverride);},store,world.epoch::get);
            world.active();
        }
        ModernRoomsTest.Player player(int i){return world.players.get(i);}
        ModernApplicationProtocol.CharacterRow row(int i){return world.sources.get(i).input().equipment().source().character();}
        ModernBattleVipCommandState.Proof proof(int i){return ModernBattleVipCommandState.from(ModernBattleVipObservation.from(world.view()).forOwner(player(i).scope()));}
        ModernBattleEngine.EquipmentChange reserve(int i,ModernBattleVipCommandState.Proof p) {
            var out=world.engine.admitVipEquipmentChange(player(i).owner(),p);assertEquals(ModernBattleVipCommandState.Rejection.NONE,out.rejection());return out.change();
        }
        <T>T call(int i,Work<T> work)throws Exception{return call(i,player(i).owner(),work);}
        <T>T call(int i,Object owner,Work<T> work)throws Exception {
            var p=player(i);var scope=ModernEquipmentOverrideStoreTest.scope();
            var lease=new ModernEquipmentLease.Issuer().open(owner,new TournamentPresence.CharacterKey(p.scope().sourceId(),p.scope().objectId()),p.scope().epoch(),()->{});
            activeLease=lease;activeScope=scope;
            try(var op=lease.operation(scope)) {ModernBackendScope.bind(scope);try{return work.run(op);}finally{ModernBackendScope.unbind();}}
            finally{lease.invalidate();scope.finish();activeLease=null;activeScope=null;}
        }
        ModernVipEquipmentAuthority.Quote inspect(int i)throws Exception {
            var p=proof(i);return call(i,op->authority.inspect(principal,row(i),op,p,reserve(i,p)));
        }
        ModernVipEquipmentAuthority.Result set(int i,ModernVipEquipmentAuthority.Quote q,long item,boolean desired)throws Exception {
            return call(i,op->authority.execute(principal,row(i),op,new ModernVipEquipmentAuthority.Request(q.proof(),item,desired,q.token()),reserve(i,q.proof())));
        }
        Optional<ModernEquipmentOverrideStore.Stored> stored(int i)throws Exception{return call(i,op->store.read(op.key(),op.scope()));}
        @Override public void close(){store.close();world.close();}
    }
}
