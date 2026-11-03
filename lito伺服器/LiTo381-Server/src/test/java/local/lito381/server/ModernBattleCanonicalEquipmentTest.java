package local.lito381.server;

import static org.junit.jupiter.api.Assertions.*;
import static local.lito381.server.ModernEquipmentAuthority.*;
import static local.lito381.server.ModernEquipmentOverrideStore.*;
import static local.lito381.server.ModernCombatProfileTest.binding;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Real durable authority plus canonical engine, NOT a negotiated wire/UI test. */
class ModernBattleCanonicalEquipmentTest {
    @TempDir Path root;
    static final long SECOND=1_000_000_000L;

    @Test void changedSpellPriceRetainsHistoricalSpendAndContinuesCastingWithoutLegacyProjection()throws Exception {
        try(var f=new Fixture(root,new FileOps(){})) {
            var gear=f.read();f.world.active();
            assertThrows(IllegalStateException.class,()->f.world.engine.beginEquipmentChange(f.player().owner(),f.player().scope(),f.world.ticket.matchId(),f.world.ticket.generation()));
            var first=f.world.cast(0,1,1,3);assertEquals(ModernSpellProtocol.Reason.NONE,first.reason());
            var before=first.observation().forOwner(f.player().scope()).owner().history().history();
            var result=f.set(gear,false);assertEquals(OK,result.equipment().status());
            assertEquals(ModernBattleEngine.EquipmentDisposition.APPLIED,result.publication().disposition());
            var after=result.observation().forOwner(f.player().scope()).owner().history().history();
            var lane=ModernUseHistory.Lane.HEALING;
            assertTrue(after.spells().get(lane).current().cost()>before.spells().get(lane).current().cost());
            assertEquals(before.spells().get(lane).last(),after.spells().get(lane).last());
            assertEquals(before.resources().flows().spent(),after.resources().flows().spent());
            assertTrue(ModernEquipmentHistory.followsReprofile(before,after));
            assertEquals(result.publication().history(),after);
            assertEquals(result.publication().worldRevision(),result.observation().revision());
            f.world.elapsed(2*SECOND);
            var second=f.world.cast(0,2,1,3);assertEquals(ModernSpellProtocol.Reason.NONE,second.reason());
            var actual=second.observation().forOwner(f.player().scope()).owner().history().history();
            assertEquals(2,actual.spells().get(lane).current().casts());
            assertEquals(before.spells().get(lane).current().spent()+after.spells().get(lane).current().cost(),actual.spells().get(lane).current().spent());
            assertEquals(actual.resources().flows().spent(),actual.spells().values().stream().mapToLong(s->s.current().spent()).sum());
            assertThrows(IllegalArgumentException.class,second::snapshot,"old fixed-price DTO must not be relaxed");
            assertEquals(1,f.stored().orElseThrow().revision());
        }
    }

    @Test void actualRemainingSuppliesAndNewWeightCapacityArePublishedTogether()throws Exception {
        try(var f=new Fixture(root,new FileOps(){})) {
            var gear=f.read();f.world.active();var actor=f.player();
            var request=new ModernItemProtocol.Request(actor.scope(),ModernItemProtocol.Action.USE,f.world.ticket.matchId(),f.world.ticket.generation(),1,40010);
            var used=f.world.engine.item(actor.owner(),request);
            assertEquals(ModernItemProtocol.Reason.NONE,used.reason());
            var before=used.observation().forOwner(actor.scope()).owner();
            var result=f.set(gear,false);var after=result.observation().forOwner(actor.scope()).owner();
            assertEquals(before.supplies().stocks(),after.supplies().stocks());
            assertEquals(before.supplies().requestSequence(),after.supplies().requestSequence());
            assertEquals(before.history().history().items().restored(),after.history().history().items().restored());
            assertEquals(0,after.history().history().items().fixedRecovery());
            assertTrue(before.weight().view().maximumWeight()>after.weight().view().maximumWeight());
            assertEquals(before.weight().view().totalWeight(),after.weight().view().totalWeight());
            assertEquals(before.weight().units(),after.weight().units());
            assertEquals(1,after.loadout().fighter().profile().equipmentRevision());
            assertEquals(1,after.loadout().weightBasis().equipmentRevision());
            f.world.elapsed(2*SECOND);
            var again=f.world.engine.item(actor.owner(),new ModernItemProtocol.Request(actor.scope(),ModernItemProtocol.Action.USE,f.world.ticket.matchId(),f.world.ticket.generation(),2,40010));
            assertEquals(ModernItemProtocol.Reason.NONE,again.reason());
            var latest=again.observation().forOwner(actor.scope()).owner();
            assertEquals(before.supplies().stocks().get(0).remaining()-1,latest.supplies().stocks().get(0).remaining());
        }
    }

    @Test void fireStormRepricesWithoutRewritingPastHitsAndStillEnforcesOriginalCooldown()throws Exception {
        try(var f=new Fixture(root,new FileOps(){})) {
            var gear=f.read();f.world.active();var first=f.world.cast(0,1,70,0);
            assertEquals(ModernSpellProtocol.Reason.NONE,first.reason());
            var old=first.observation().forOwner(f.player().scope()).owner().history().history();
            var result=f.set(gear,false);var updated=result.observation().forOwner(f.player().scope()).owner().history().history();
            var lane=ModernUseHistory.Lane.FIRE_STORM;
            assertTrue(updated.spells().get(lane).current().cost()>old.spells().get(lane).current().cost());
            assertEquals(old.targets(),updated.targets());assertEquals(old.spells().get(lane).last(),updated.spells().get(lane).last());
            var denied=f.world.cast(0,2,70,0);assertEquals(ModernSpellProtocol.Reason.TOO_FAST,denied.reason());
            assertEquals(updated.resources().flows().spent(),denied.observation().actors().get(0).resources().flows().spent());
            f.world.elapsed(2*SECOND);var next=f.world.cast(0,3,70,0);assertEquals(ModernSpellProtocol.Reason.NONE,next.reason());
            var history=next.observation().forOwner(f.player().scope()).owner().history().history();
            assertEquals(old.spells().get(lane).current().spent()+updated.spells().get(lane).current().cost(),history.spells().get(lane).current().spent());
            assertEquals(2,history.spells().get(lane).current().casts());
            assertThrows(IllegalArgumentException.class,next::snapshot);
        }
    }

    @Test void staleLifeWrongIdentityFutureRevisionAndParallelReservationsCannotReachStore()throws Exception {
        try(var f=new Fixture(root,new FileOps(){})) {
            assertThrows(IllegalStateException.class,()->f.reserve(f.proof()));f.world.active();var p=f.proof();
            assertThrows(IllegalStateException.class,()->f.reserve(new ModernBattleCommandLife.Proof(p.player(),p.world(),p.entity(),p.life()+1,p.revision())));
            assertThrows(SecurityException.class,()->f.reserve(new ModernBattleCommandLife.Proof(p.player(),p.world(),p.entity()+1,p.life(),p.revision())));
            assertThrows(SecurityException.class,()->f.reserve(new ModernBattleCommandLife.Proof(p.player(),p.world(),p.entity(),p.life(),Long.MAX_VALUE)));
            assertThrows(SecurityException.class,()->f.world.engine.beginCanonicalEquipmentChange(f.world.players.get(1).owner(),p));
            try(var held=f.reserve(p)){assertThrows(IllegalStateException.class,()->f.reserve(p));}
            assertTrue(f.stored().isEmpty());
            try(var after=f.reserve(p)){}
        }
    }

    @Test void noopReturnsExactCanonicalCaptureWithoutReplacingLoadoutOrAdvancingRevision()throws Exception {
        try(var f=new Fixture(root,new FileOps(){})) {
            var gear=f.read();f.world.active();var before=f.capture();var result=f.set(gear,true);
            assertFalse(result.equipment().changed());assertEquals(ModernBattleEngine.EquipmentDisposition.UNCHANGED,result.publication().disposition());
            assertEquals(before,result.observation());assertTrue(f.stored().isEmpty());
            assertSame(before.forOwner(f.player().scope()).owner().loadout(),result.observation().forOwner(f.player().scope()).owner().loadout());
        }
    }

    @Test void damageHealingConsumptionAndRegenerationDuringDurableIoAreRetained()throws Exception {
        var ref=new AtomicReference<Fixture>();var during=new AtomicReference<ModernBattleObservation.Capture>();
        try(var f=new Fixture(root,new FileOps(){
            @Override public void force(FileChannel file)throws IOException {
                FileOps.super.force(file);onOtherThread(()->{
                    var x=ref.get();assertEquals(ModernBattleEngine.AttackStatus.ATTACKED,x.world.attack(1,1,1).status());
                    assertEquals(ModernSpellProtocol.Reason.NONE,x.world.cast(0,1,1,3).reason());
                    assertEquals(ModernItemProtocol.Reason.NONE,x.use(1).reason());return null;
                });
            }
            @Override public void move(Path from,Path to)throws IOException {
                FileOps.super.move(from,to);during.set(onOtherThread(()->{
                    var x=ref.get();x.world.clock.addAndGet(60*SECOND);x.capture();
                    assertEquals(ModernBattleEngine.AttackStatus.ATTACKED,x.world.attack(1,2,1).status());
                    return x.capture();
                }));
            }
        })) {
            ref.set(f);var gear=f.read();f.world.active();var result=f.set(gear,false);
            var before=during.get().forOwner(f.player().scope()).owner();var after=result.observation().forOwner(f.player().scope()).owner();
            var old=before.history().history();var now=after.history().history();
            assertTrue(old.resources().flows().damaged()>0);assertTrue(old.resources().flows().restored()>0);
            assertTrue(old.resources().flows().mpGain()>0);
            assertTrue(ModernEquipmentHistory.followsReprofile(old,now));
            assertEquals(before.regenClock(),after.regenClock());
            assertEquals(before.supplies().stocks(),after.supplies().stocks());
            assertEquals(before.history().spells(),after.history().spells());
            assertEquals(during.get().revision()+1,result.observation().revision());
        }
    }

    @Test void revivalBeforeAdmissionRejectsCommitAndRetiresOnlyItsReservation()throws Exception {
        var ref=new AtomicReference<Fixture>();
        try(var f=new Fixture(root,new FileOps(){
            @Override public void force(FileChannel file)throws IOException {
                FileOps.super.force(file);onOtherThread(()->{ref.get().killAndRevive();return null;});
            }
        })) {
            ref.set(f);var gear=f.read();f.world.active();
            assertEquals(Failure.INVALID_OPERATION,assertThrows(StoreException.class,()->f.set(gear,false)).failure());
            var live=f.capture();assertEquals(ModernBattleEngine.Phase.ACTIVE,live.phase());
            assertEquals(1,live.forOwner(f.player().scope()).owner().history().history().resources().life());
            assertTrue(f.stored().isEmpty());try(var next=f.reserve(f.proof())){}
        }
    }

    @Test void revivalAfterRenamePreservesSavedGearButCannotApplyToAnotherLife()throws Exception {
        var ref=new AtomicReference<Fixture>();var revived=new AtomicReference<ModernBattleObservation.Capture>();
        try(var f=new Fixture(root,new FileOps(){
            @Override public void move(Path from,Path to)throws IOException {
                FileOps.super.move(from,to);revived.set(onOtherThread(()->{ref.get().killAndRevive();return ref.get().capture();}));
            }
        })) {
            ref.set(f);var gear=f.read();f.world.active();var result=f.set(gear,false);
            assertEquals(ModernBattleEngine.EquipmentDisposition.PERSISTED_RETIRED,result.publication().disposition());
            assertEquals(BACKEND_UNAVAILABLE,result.equipment().status());assertNull(result.equipment().snapshot());
            assertEquals(1,f.stored().orElseThrow().revision());
            var after=result.observation();assertEquals(ModernBattleEngine.Phase.ABORTED,after.phase());
            assertEquals(ModernBattleEngine.AbortReason.SERVER_ABORT,after.abortReason());
            assertEquals(revived.get().actors().get(0).resources(),after.actors().get(0).resources());
            assertSame(revived.get().actors().get(0).loadout(),after.actors().get(0).loadout());
            assertEquals(0,after.actors().get(0).resources().equipmentRevision());
        }
    }

    @Test void savedReplacementSnapshotRemainsImmutableWhenWorldContinues()throws Exception {
        try(var f=new Fixture(root,new FileOps(){})) {
            var gear=f.read();f.world.active();var result=f.set(gear,false);var exact=result.observation();
            var after=f.world.cast(0,1,1,3).observation();
            assertTrue(after.revision()>exact.revision());
            assertEquals(0,exact.actors().get(0).runtime().sequence());assertEquals(1,after.actors().get(0).runtime().sequence());
            assertEquals(result.publication().history(),exact.forOwner(f.player().scope()).owner().history().history());
        }
    }

    static <T>T onOtherThread(java.util.concurrent.Callable<T> work)throws IOException {
        var executor=java.util.concurrent.Executors.newSingleThreadExecutor();
        try{return executor.submit(work).get(5,java.util.concurrent.TimeUnit.SECONDS);}
        catch(Exception failure){throw new IOException("gear I/O retained the engine monitor or concurrent action failed",failure);}
        finally{executor.shutdownNow();try{assertTrue(executor.awaitTermination(5,java.util.concurrent.TimeUnit.SECONDS));}catch(InterruptedException failure){Thread.currentThread().interrupt();}}
    }

    @FunctionalInterface interface Work<T>{T run(ModernEquipmentLease.Operation operation)throws Exception;}
    static final class Fixture implements AutoCloseable {
        final ModernBattleEngineHonorTest.Fixture world;
        final List<ModernPhysicalCombat.Source> sources=new ArrayList<>();
        final ModernEquipmentOverrideStore store;
        final ModernEquipmentAuthority authority;
        final ModernAccountAuthentication.Principal principal=new ModernAccountAuthentication.Principal("canonical-gear-test",Set.of(new ModernAccountAuthentication.Source(7,"source-7")));
        Fixture(Path root,FileOps files)throws Exception {
            this(root,files,java.util.function.UnaryOperator.identity());
        }
        Fixture(Path root,FileOps files,java.util.function.UnaryOperator<ModernBattleEngine.PreparedMember> configure)throws Exception {
            this(root,files,configure,java.util.function.UnaryOperator.identity());
        }
        Fixture(Path root,FileOps files,java.util.function.UnaryOperator<ModernBattleEngine.PreparedMember> configure,
                java.util.function.UnaryOperator<ModernPhysicalCombat.Source> sourceTransform)throws Exception {
            this(root,files,configure,sourceTransform,false);
        }
        Fixture(Path root,FileOps files,java.util.function.UnaryOperator<ModernBattleEngine.PreparedMember> configure,
                java.util.function.UnaryOperator<ModernPhysicalCombat.Source> sourceTransform,boolean protection)throws Exception {
            store=new ModernEquipmentOverrideStore(root,files);
            world=new ModernBattleEngineHonorTest.Fixture(100000,1000,500,SECOND,0,true,true,p->{
                var base=ModernBattleLoadoutTest.source(p.scope().sourceId());var q=base.input().equipment();
                var s=q.source().persisted();
                var saved=new ModernCharacterData.PersistedValues(s.maxHp(),s.maxMp(),s.savedHp(),s.savedMp(),s.armorClass(),s.strength(),s.dexterity(),s.constitution(),s.intelligence(),s.wisdom(),s.charisma(),s.experience(),s.lawful(),s.originalStrength(),s.originalDexterity(),s.originalConstitution(),s.originalIntelligence(),s.originalWisdom(),225);
                var source=new ModernCharacterData.Snapshot(q.source().sourceId(),p.character(),saved,q.source().inventory());
                var qualified=new ModernEquipmentFacts.Qualified(source,q.items(),q.catalog(),ModernItemLifetime.Observation.verifiedPermanent(source));
                var full=new ModernPhysicalCombat.Source(new ModernCombatProfile.Input(qualified,true,base.input().weapons()),base.growth(),base.templates(),base.procCatalogIds(),base.magicGrowth(),base.magicBows(),base.weaponArrowSprites(),base.regenGrowth(),base.healingGrowth());
                full=sourceTransform.apply(full);sources.add(full);
                var fighter=protection?ModernPhysicalCombat.qualifyProtection(ModernArmorProtectionLoadoutTest.bound(full),binding(full.input().equipment()),Optional.empty(),1000)
                        :ModernPhysicalCombat.qualify(full,binding(full.input().equipment()),Optional.empty(),1000);
                return configure.apply(new ModernBattleEngine.PreparedMember(p.owner(),p.scope(),p.character(),p.spawn(),p.stepIntervalNanos(),fighter,p.attackIntervalNanos(),p.skills(),p.castIntervalNanos(),p.damageMagic(),p.matchItems(),ModernInventoryWeight.qualify(fighter.profile(),100),p.timedEnabled(),p.bowEnabled(),p.regenEnabled(),p.fireStormEnabled()));
            });
            authority=new ModernEquipmentAuthority((p,s,o)->new Loaded(sources.get(0).input().equipment(),binding(sources.get(0).input().equipment()).fingerprint()),null,
                    (p,s,o)->new PhysicalLoaded(sources.get(0),binding(sources.get(0).input().equipment()).fingerprint(),
                            protection?ModernArmorProtectionLoadoutTest.boundInScope(sources.get(0)):null),store,world.epoch::get);
        }
        ModernBattleEngineBotTest.Player player(){return world.players.get(0);}
        ModernBattleObservation.Capture capture(){return world.engine.serviceObservation();}
        ModernBattleCommandLife.Proof proof(){var c=capture();return new ModernBattleCommandLife.Proof(player().scope(),c.identity(),1,c.forOwner(player().scope()).owner().history().history().resources().life()+1,c.revision());}
        ModernBattleEngine.EquipmentChange reserve(ModernBattleCommandLife.Proof p){return world.engine.beginCanonicalEquipmentChange(player().owner(),p);}
        <T>T call(Work<T> work)throws Exception {
            var scope=ModernEquipmentOverrideStoreTest.scope();var lease=new ModernEquipmentLease.Issuer().open(player().owner(),binding(sources.get(0).input().equipment()).key(),1,()->{});
            try(var operation=lease.operation(scope)){ModernBackendScope.bind(scope);try{return work.run(operation);}finally{ModernBackendScope.unbind();}}
            finally{lease.invalidate();scope.finish();}
        }
        Snapshot read()throws Exception{return call(op->authority.execute(principal,player().row(),op,new Request(7,3333,1,READ,false,0,0,ZERO)).snapshot());}
        Optional<Stored> stored()throws Exception{return call(op->store.read(op.key(),op.scope()));}
        MatchResult set(Snapshot before,boolean equipped)throws Exception{return call(op->authority.executeInMatch(principal,player().row(),op,new Request(7,3333,1,SET,equipped,1,before.revision(),before.viewToken()),reserve(proof())));}
        ModernBattleEngine.ItemResult use(long sequence){return world.engine.item(player().owner(),new ModernItemProtocol.Request(player().scope(),ModernItemProtocol.Action.USE,world.ticket.matchId(),world.ticket.generation(),sequence,40010));}
        void killAndRevive(){
            for(int sequence=1;sequence<=100;sequence++) {
                world.clock.addAndGet(2*SECOND);var hit=world.cast(1,sequence,4,1);
                assertEquals(ModernSpellProtocol.Reason.NONE,hit.reason());
                if(hit.observation().actors().get(0).vital().values().hp()==0){
                    world.clock.addAndGet(5*SECOND);var revived=capture();
                    assertTrue(revived.actors().get(0).vital().values().hp()>0);return;
                }
            }
            fail("actual qualified damage did not kill within bounded fixture attempts");
        }
        @Override public void close(){world.close();store.close();assertTrue(store.isQuiescent());}
    }
}
