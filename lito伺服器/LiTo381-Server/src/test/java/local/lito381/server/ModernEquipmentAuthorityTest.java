package local.lito381.server;

import static local.lito381.server.ModernEquipmentAuthority.*;
import static local.lito381.server.ModernEquipmentFacts.*;
import static local.lito381.server.ModernEquipmentFacts.Stat.*;
import static local.lito381.server.ModernEquipmentOverrideStore.*;
import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.lang.reflect.Field;
import java.net.InetAddress;
import java.net.Socket;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLSocket;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Real TLS AUTH/SELECT and Navigation-owned operations; never fabricates a lease. */
class ModernEquipmentAuthorityTest {
    @TempDir Path directory;
    private static SSLContext serverContext,clientContext;
    private static final TournamentPresence.CharacterKey KEY = new TournamentPresence.CharacterKey(7,3333);
    private static final long EPOCH = Long.MIN_VALUE;
    private static final String ACCOUNT = "equipment-fixture-login", PASSWORD = "equipment-fixture-password";
    private static final ModernApplicationProtocol.CharacterRow ROW =
            new ModernApplicationProtocol.CharacterRow(3333,"Equipment Fixture",0,0,80);
    private static final ModernAccountAuthentication.Principal PRINCIPAL = new ModernAccountAuthentication.Principal(
            ACCOUNT,Set.of(new ModernAccountAuthentication.Source(7,"Sun")),Map.of(7,"canonical-fixture-owner"));

    @BeforeAll static void certificates() throws Exception {
        Path fixture = ModernTlsTestMaterial.defaultDirectory(); ModernTlsTestMaterial.prepare(fixture);
        serverContext = ModernTlsTestMaterial.serverContext(fixture,false);
        clientContext = ModernTlsTestMaterial.clientContext(fixture);
    }

    @Test void readPreservesSourceAndWideModifiersWithoutCreatingACharacterRecord() throws Exception {
        long large = 4_294_967_299L;
        var fresh = qualified(armor(1,100,true,EquipmentRules.ArmorSlot.BODY,Ability.zero().with(STR,large).with(AC,-large)));
        var saved = fresh.source().persisted();
        try (var harness = new Harness(directory,fresh)) {
            var result = harness.read(); assertEquals(OK,result.status()); assertFalse(result.changed());
            var snapshot = result.snapshot(); assertEquals(0,snapshot.revision());
            assertEquals(large,snapshot.modifiers().get(STR)); assertEquals(-large,snapshot.modifiers().get(AC));
            assertSame(saved,snapshot.source().persisted()); assertEquals(-900,snapshot.source().persisted().armorClass());
            assertEquals(1000,snapshot.source().persisted().savedHp());
            assertEquals(List.of(new Equipped(1,true)),snapshot.equipped()); assertTrue(snapshot.stale().isEmpty());
            assertEquals(ModernEquipmentBinding.catalogGeneration(fresh.catalog()).fingerprint(),snapshot.catalogGeneration());
            assertEquals(snapshot.viewToken(),harness.read().snapshot().viewToken());
            assertTrue(harness.stored().isEmpty()); assertEquals(0,harness.characterFiles());
            assertFalse(harness.lastOperation.get().isCommitAdmitted());
        }
    }

    @Test void bothAlreadyEquippedAndAlreadyUnequippedNoopsLeaveAbsentStorageAbsent() throws Exception {
        try (var harness = new Harness(directory,qualified(
                armor(1,100,true,EquipmentRules.ArmorSlot.BODY,Ability.zero()),
                armor(2,200,false,EquipmentRules.ArmorSlot.HELMET,Ability.zero())))) {
            var before = harness.read().snapshot();
            for (var desired : List.of(new Equipped(1,true),new Equipped(2,false))) {
                var result = harness.set(before,desired.objectId(),desired.equipped());
                assertEquals(OK,result.status()); assertFalse(result.changed()); assertEquals(0,result.snapshot().revision());
                assertEquals(before.viewToken(),result.snapshot().viewToken());
                assertTrue(harness.stored().isEmpty()); assertEquals(0,harness.characterFiles());
            }
        }
    }

    @Test void itemExpiryChangesViewWithoutRewritingSourceAndCannotBeReequipped() throws Exception {
        var fresh=timed(qualified(armor(1,100,true,EquipmentRules.ArmorSlot.BODY,Ability.zero().with(STR,10))));
        try(var harness=new Harness(directory,fresh)) {
            var active=harness.read().snapshot();
            assertEquals(ModernItemLifetime.State.ACTIVE,active.lifetimes().get(0).state());
            assertEquals(2000,active.nextStoneExpiry()); assertEquals(10,active.modifiers().get(STR));
            harness.clock.set(2000);
            var conflict=harness.set(active,1,true); assertEquals(CONFLICT,conflict.status());
            var expired=conflict.snapshot();
            assertNotEquals(active.viewToken(),expired.viewToken());
            assertEquals(ModernItemLifetime.State.EXPIRED,expired.lifetimes().get(0).state());
            assertTrue(expired.source().inventory().get(0).equipped());
            assertFalse(expired.equipped().get(0).equipped()); assertEquals(0,expired.modifiers().get(STR));
            var denied=harness.set(expired,1,true); assertEquals(DENIED,denied.status());
            assertEquals(EquipmentRules.Reason.UNQUALIFIED_ITEM.ordinal()+1,denied.reason());
            assertEquals(OK,harness.set(expired,1,false).status());
            assertTrue(harness.stored().isEmpty()); assertEquals(0,harness.characterFiles());
        }
    }

    @Test void itemExpiryDuringFileForcePreventsRenameAndDurableIntent() throws Exception {
        var reference=new AtomicReference<Harness>(); var moves=new AtomicInteger();
        var fresh=timed(qualified(armor(1,100,false,EquipmentRules.ArmorSlot.BODY,Ability.zero().with(STR,10))));
        try(var harness=new Harness(directory,fresh,new FileOps() {
            @Override public void force(FileChannel channel) throws IOException {
                FileOps.super.force(channel); reference.get().clock.set(2000);
            }
            @Override public void move(Path from,Path to) throws IOException {
                moves.incrementAndGet(); FileOps.super.move(from,to);
            }
        })) {
            reference.set(harness); var before=harness.read().snapshot();
            assertThrows(ModernBackendExecutor.BackendException.class,()->harness.set(before,1,true));
            assertEquals(Failure.INVALID_OPERATION,harness.storageFailure.get());
            assertEquals(0,moves.get()); assertEquals(0,harness.characterFiles());
            assertTrue(harness.stored().isEmpty());
            assertEquals(ModernItemLifetime.State.EXPIRED,harness.read().snapshot().lifetimes().get(0).state());
        }
    }

    @Test void itemExpiryAfterRenameWithholdsActiveAckButRetainsCommittedRevision() throws Exception {
        var reference=new AtomicReference<Harness>();
        var fresh=timed(qualified(armor(1,100,false,EquipmentRules.ArmorSlot.BODY,Ability.zero().with(STR,10))));
        try(var harness=new Harness(directory,fresh,new FileOps() {
            @Override public void move(Path from,Path to) throws IOException {
                FileOps.super.move(from,to); reference.get().clock.set(2000);
            }
        })) {
            reference.set(harness); var before=harness.read().snapshot();
            assertThrows(ModernBackendExecutor.BackendException.class,()->harness.set(before,1,true));
            assertEquals(1,harness.characterFiles());
            var durable=harness.stored().orElseThrow(); assertEquals(1,durable.revision());
            assertTrue(durable.value().overrides().get(0).equipped());
            var recovered=harness.read().snapshot(); assertEquals(1,recovered.revision());
            assertFalse(recovered.equipped().get(0).equipped()); assertEquals(0,recovered.modifiers().get(STR));
            assertEquals(ModernItemLifetime.State.EXPIRED,recovered.lifetimes().get(0).state());
        }
    }

    @Test void authorityHighWaterAlsoFencesAClockRollbackDuringFileForce() throws Exception {
        var reference=new AtomicReference<Harness>(); var moves=new AtomicInteger();
        var fresh=timed(qualified(armor(1,100,false,EquipmentRules.ArmorSlot.BODY,Ability.zero())));
        try(var harness=new Harness(directory,fresh,new FileOps() {
            @Override public void force(FileChannel channel) throws IOException {
                FileOps.super.force(channel); var active=reference.get();
                active.clock.set(2500);
                try {
                    // Exercise the exact authority clock used by concurrent
                    // publications without acquiring this selection's lease.
                    var observe=ModernEquipmentAuthority.class.getDeclaredMethod("now");
                    observe.setAccessible(true); assertEquals(2500L,observe.invoke(active.authority));
                } catch(ReflectiveOperationException failure) { throw new IOException("clock fixture failed"); }
                active.clock.set(1500); // Still inside this request's original window.
            }
            @Override public void move(Path from,Path to) throws IOException {
                moves.incrementAndGet(); FileOps.super.move(from,to);
            }
        })) {
            reference.set(harness); var before=harness.read().snapshot();
            assertThrows(ModernBackendExecutor.BackendException.class,()->harness.set(before,1,true));
            assertEquals(Failure.INVALID_OPERATION,harness.storageFailure.get());
            assertEquals(0,moves.get()); assertEquals(0,harness.characterFiles());
            harness.clock.set(2500); assertTrue(harness.stored().isEmpty());
        }
    }

    @Test void backwardsAuthorityClockCannotReturnAnEarlierActiveView() throws Exception {
        try(var harness=new Harness(directory,timed(qualified(armor(1,100,true,EquipmentRules.ArmorSlot.BODY,Ability.zero()))))) {
            harness.read(); harness.clock.set(999);
            assertThrows(ModernBackendExecutor.BackendException.class,harness::read);
            assertEquals(0,harness.characterFiles());
        }
    }

    private static Qualified timed(Qualified source) {
        var facts=new java.util.TreeMap<Long,ModernItemLifetime.Fact>();
        for(var item:source.source().inventory())facts.put(item.objectId(),new ModernItemLifetime.Fact(
                item.objectId(),item.template().catalogId(),item.template().kind(),item.equipped(),
                java.util.OptionalLong.of(0),true,java.util.OptionalLong.of(1),Optional.of("1970-01-01 00:00:01"),0,false));
        return new Qualified(source.source(),source.items(),source.catalog(),new ModernItemLifetime.Observation(
                source.source().sourceId(),source.source().character().objectId(),"1970-01-01 00:00:00",1000,0,facts));
    }

    @Test void unequipAndReequipCommitDesiredFlagsAndDoNotRewritePersistedSource() throws Exception {
        var fresh = qualified(armor(1,100,true,EquipmentRules.ArmorSlot.BODY,Ability.zero().with(STR,10)));
        try (var harness = new Harness(directory,fresh)) {
            var first = harness.set(harness.read().snapshot(),1,false);
            assertEquals(OK,first.status()); assertTrue(first.changed()); assertEquals(1,first.snapshot().revision());
            assertEquals(List.of(new Equipped(1,false)),first.snapshot().equipped()); assertEquals(0,first.snapshot().modifiers().get(STR));
            assertEquals(false,harness.stored().orElseThrow().value().overrides().get(0).equipped());
            var second = harness.set(first.snapshot(),1,true);
            assertEquals(OK,second.status()); assertTrue(second.changed()); assertEquals(2,second.snapshot().revision());
            assertEquals(10,second.snapshot().modifiers().get(STR)); assertTrue(second.snapshot().equipped().get(0).equipped());
            assertTrue(harness.stored().orElseThrow().value().overrides().isEmpty(),"return to source flag removes only its override");
            assertEquals(1,harness.characterFiles()); assertTrue(fresh.source().inventory().get(0).equipped());
        }
    }

    @Test void weaponSwapIsOneRevisionContainingBothExplicitTransitions() throws Exception {
        try (var harness = new Harness(directory,qualified(
                weapon(1,100,true,false,1,Ability.zero().with(STR,2)),
                weapon(2,200,false,false,1,Ability.zero().with(STR,20))))) {
            var result = harness.set(harness.read().snapshot(),2,true);
            assertEquals(OK,result.status()); assertTrue(result.changed()); assertEquals(1,result.snapshot().revision());
            assertEquals(List.of(new Equipped(1,false),new Equipped(2,true)),result.snapshot().equipped());
            assertEquals(20,result.snapshot().modifiers().get(STR));
            var entries = harness.stored().orElseThrow().value().overrides();
            assertEquals(2,entries.size()); assertEquals(1,entries.get(0).itemObjectId()); assertFalse(entries.get(0).equipped());
            assertEquals(2,entries.get(1).itemObjectId()); assertTrue(entries.get(1).equipped());
        }
    }

    @Test void classAndUnknownItemRejectionsKeepTheOriginalViewAndDoNotWrite() throws Exception {
        var item = armor(1,100,false,EquipmentRules.ArmorSlot.BODY,Ability.zero());
        item = permissions(item,new EquipmentRules.ArmorFacts(EquipmentRules.CharacterClass.KNIGHT.mask(),
                new EquipmentRules.LevelRange(1,Long.MAX_VALUE),EquipmentRules.ArmorSlot.BODY));
        try (var harness = new Harness(directory,qualified(item))) {
            var before = harness.read().snapshot();
            denied(harness.set(before,1,true),EquipmentRules.Reason.CLASS_NOT_ALLOWED,1,before);
            denied(harness.set(before,999,true),EquipmentRules.Reason.UNKNOWN_ITEM,999,before);
            assertTrue(harness.stored().isEmpty()); assertEquals(0,harness.characterFiles());
        }
    }

    @Test void cursedSwapAndTwoHandedShieldAndOccupiedArmorAreDeniedWithoutWrites() throws Exception {
        var cases = List.of(
                qualified(weapon(1,100,true,false,2,Ability.zero()),weapon(2,200,false,false,1,Ability.zero())),
                qualified(armor(1,100,true,EquipmentRules.ArmorSlot.SHIELD,Ability.zero()),weapon(2,200,false,true,1,Ability.zero())),
                qualified(armor(1,100,true,EquipmentRules.ArmorSlot.BODY,Ability.zero()),armor(2,200,false,EquipmentRules.ArmorSlot.BODY,Ability.zero())));
        var reasons = List.of(EquipmentRules.Reason.CURSED,EquipmentRules.Reason.TWO_HANDED_SHIELD_CONFLICT,EquipmentRules.Reason.SLOT_OCCUPIED);
        for (int index=0;index<cases.size();index++) {
            try (var harness = new Harness(directory.resolve("case"+index),cases.get(index))) {
                var before = harness.read().snapshot(); denied(harness.set(before,2,true),reasons.get(index),1,before);
                assertEquals(0,harness.characterFiles()); assertTrue(harness.stored().isEmpty());
            }
        }
    }

    @Test void freshLevelIsUsedForPermissionInsteadOfTheEarlierSelectedRosterLevel() throws Exception {
        var item = permissions(armor(1,100,false,EquipmentRules.ArmorSlot.BODY,Ability.zero()),
                new EquipmentRules.ArmorFacts(127,new EquipmentRules.LevelRange(52,Long.MAX_VALUE),EquipmentRules.ArmorSlot.BODY));
        try (var harness = new Harness(directory,qualified(item))) {
            harness.input.set(withCharacter(harness.input.get(),new ModernApplicationProtocol.CharacterRow(3333,ROW.name(),0,0,14)));
            var read = harness.read(); assertEquals(OK,read.status()); assertEquals(14,read.snapshot().source().character().level());
            denied(harness.set(read.snapshot(),1,true),EquipmentRules.Reason.LEVEL_TOO_LOW,1,read.snapshot());
            assertEquals(0,harness.characterFiles());
        }
    }

    @Test void persistedOverridesSurviveClosingAndReopeningTheRealStore() throws Exception {
        try (var harness = new Harness(directory,qualified(armor(1,100,false,EquipmentRules.ArmorSlot.BODY,Ability.zero().with(STR,9))))) {
            var committed = harness.set(harness.read().snapshot(),1,true).snapshot();
            var disk = harness.stored().orElseThrow(); harness.reopenStore();
            var reopened = harness.read().snapshot();
            assertEquals(committed.revision(),reopened.revision()); assertEquals(committed.viewToken(),reopened.viewToken());
            assertEquals(committed.equipped(),reopened.equipped()); assertEquals(9,reopened.modifiers().get(STR));
            assertEquals(disk,harness.stored().orElseThrow());
        }
    }

    @Test void revisionAndTokenConflictsReturnFreshViewWithoutRepeatingTheMutation() throws Exception {
        try (var harness = new Harness(directory,qualified(armor(1,100,false,EquipmentRules.ArmorSlot.BODY,Ability.zero())))) {
            var old = harness.read().snapshot(); var current = harness.set(old,1,true).snapshot();
            var stale = harness.set(old,1,false); assertEquals(CONFLICT,stale.status()); assertFalse(stale.changed());
            assertEquals(current.viewToken(),stale.snapshot().viewToken()); assertEquals(1,stale.snapshot().revision());
            byte[] wrong = current.viewToken().bytes(); wrong[0]^=1;
            var conflict = harness.call(new Request(7,3333,EPOCH,SET,false,1,current.revision(),new Fingerprint(wrong)));
            assertEquals(CONFLICT,conflict.status()); assertEquals(current.equipped(),conflict.snapshot().equipped());
            assertEquals(1,harness.stored().orElseThrow().revision());
        }
    }

    @Test void changedSourceFactsConflictEvenWhenStoredRevisionHasNotChanged() throws Exception {
        var item = armor(1,100,false,EquipmentRules.ArmorSlot.BODY,Ability.zero());
        try (var harness = new Harness(directory,qualified(item))) {
            var before = harness.read().snapshot(); harness.input.set(qualified(quantity(item,2)));
            var result = harness.set(before,1,true);
            assertEquals(CONFLICT,result.status()); assertEquals(0,result.snapshot().revision());
            assertNotEquals(before.viewToken(),result.snapshot().viewToken());
            assertEquals(2,result.snapshot().source().inventory().get(0).quantity()); assertEquals(0,harness.characterFiles());
        }
    }

    @Test void changedCatalogGenerationConflictsAndDoesNotApplyOldViewIntent() throws Exception {
        try (var harness = new Harness(directory,qualified(armor(1,100,true,EquipmentRules.ArmorSlot.BODY,Ability.zero())))) {
            var before = harness.read().snapshot(); var fresh = harness.input.get();
            var lookups = new LinkedHashMap<>(fresh.catalog().lookups());
            var key = new RuleKey(RuleKind.BLESS_ITEM,ModernCharacterData.ItemKind.ARMOR,100,1);
            lookups.put(key,RuleValue.present(Ability.zero().with(STR,7)));
            var catalog = fresh.catalog();
            harness.input.set(new Qualified(fresh.source(),fresh.items(),new Catalog(7,catalog.resourceDatabase(),lookups,
                    catalog.verifiedSetIds(),catalog.sets(),catalog.combos(),catalog.vipTableState()),fresh.lifetimes()));
            var result = harness.set(before,1,false);
            assertEquals(CONFLICT,result.status()); assertNotEquals(before.catalogGeneration(),result.snapshot().catalogGeneration());
            assertEquals(7,result.snapshot().modifiers().get(STR)); assertTrue(result.snapshot().equipped().get(0).equipped());
            assertEquals(0,harness.characterFiles());
        }
    }

    @Test void ownerRouteBindingMismatchDoesNotSilentlyRebindOrExposeOldOverrides() throws Exception {
        try (var harness = new Harness(directory,qualified(armor(1,100,false,EquipmentRules.ArmorSlot.BODY,Ability.zero())))) {
            var written = harness.set(harness.read().snapshot(),1,true); assertEquals(OK,written.status());
            var stored = harness.stored().orElseThrow(); harness.changeRoute();
            var result = harness.read(); assertEquals(BINDING_MISMATCH,result.status()); assertNull(result.snapshot());
            assertFalse(result.changed()); assertEquals(stored,harness.stored().orElseThrow());
        }
    }

    @Test void unrelatedStaleRecordsRemainOnDiskWhileAnExplicitFreshItemIsChanged() throws Exception {
        var removed = armor(1,100,false,EquipmentRules.ArmorSlot.BODY,Ability.zero());
        var helmet = armor(2,200,false,EquipmentRules.ArmorSlot.HELMET,Ability.zero().with(STR,4));
        try (var harness = new Harness(directory,qualified(removed,helmet))) {
            harness.set(harness.read().snapshot(),1,true);
            var preserved = harness.stored().orElseThrow().value().overrides().get(0);
            harness.input.set(qualified(helmet)); var fresh = harness.read().snapshot();
            assertEquals(1,fresh.stale().size()); assertEquals(ModernEquipmentReconciliation.StaleReason.SOURCE_ITEM_MISSING,fresh.stale().get(0).reason());
            var result = harness.set(fresh,2,true); assertEquals(OK,result.status()); assertEquals(2,result.snapshot().revision());
            assertEquals(4,result.snapshot().modifiers().get(STR)); assertEquals(1,result.snapshot().stale().size());
            var disk = harness.stored().orElseThrow().value().overrides();
            assertEquals(2,disk.size()); assertEquals(preserved,disk.get(0)); assertEquals(2,disk.get(1).itemObjectId());
        }
    }

    @Test void fullStaleOverrideStoreRejectsAnAdditionalItemBeforePermitWithoutPruning() throws Exception {
        var forces = new AtomicInteger();
        var fresh = armor(513,2000,false,EquipmentRules.ArmorSlot.HELMET,Ability.zero().with(STR,4));
        try (var harness = new Harness(directory,qualified(fresh),new FileOps() {
            @Override public void force(FileChannel channel) throws IOException { forces.incrementAndGet(); FileOps.super.force(channel); }
        })) {
            // Seed only the real private store through an authenticated selection's
            // Work-owned operation. All 512 old instances are absent from SQL's
            // current fixture, not malformed rows that could be discarded.
            var stale = new ArrayList<ItemOverride>();
            for (long id=1;id<=ModernCharacterData.MAX_ITEMS;id++) {
                var removed = armor(id,1000+id,false,EquipmentRules.ArmorSlot.BODY,Ability.zero());
                stale.add(new ItemOverride(id,1000+id,ModernEquipmentReconciliation.fingerprint(removed),true));
            }
            var seeded = harness.navigation.get().equipment(7,3333,EPOCH,(principal,selected,operation) -> {
                var binding = ModernEquipmentBinding.ownerBinding(principal,harness.source.get());
                return harness.store.compareAndSet(KEY,Expected.absent(),new Replacement(binding,stale),
                        operation.scope(),operation).stored();
            });
            assertEquals(1,seeded.revision()); assertEquals(ModernCharacterData.MAX_ITEMS,seeded.value().overrides().size());
            forces.set(0);
            var before = harness.read().snapshot();
            assertEquals(1,before.revision()); assertEquals(ModernCharacterData.MAX_ITEMS,before.stale().size());
            assertTrue(before.stale().stream().allMatch(row -> row.reason()==ModernEquipmentReconciliation.StaleReason.SOURCE_ITEM_MISSING));
            assertEquals(List.of(new Equipped(513,false)),before.equipped());
            var request = new Request(7,3333,EPOCH,SET,true,513,before.revision(),before.viewToken());
            var refused = harness.navigation.get().equipment(7,3333,EPOCH,(principal,selected,operation) -> {
                var result = harness.authority.execute(principal,selected,operation,request);
                assertFalse(operation.isCommitAdmitted(),"full storage must be rejected before a permit, not merely retired afterwards");
                return result;
            });
            assertEquals(BACKEND_UNAVAILABLE,refused.status()); assertFalse(refused.changed()); assertNull(refused.snapshot());
            assertEquals(0,forces.get()); assertEquals(seeded,harness.stored().orElseThrow());
            var after = harness.read().snapshot();
            assertEquals(before.revision(),after.revision()); assertEquals(before.viewToken(),after.viewToken());
            assertEquals(before.stale(),after.stale()); assertEquals(before.equipped(),after.equipped());
            assertEquals(1,harness.characterFiles());
        }
    }

    @Test void candidateI64OverflowFailsBeforeForcePermitOrCharacterFileCreation() throws Exception {
        var forces = new AtomicInteger();
        try (var harness = new Harness(directory,qualified(
                armor(1,100,true,EquipmentRules.ArmorSlot.BODY,Ability.zero().with(STR,Long.MAX_VALUE)),
                armor(2,200,false,EquipmentRules.ArmorSlot.HELMET,Ability.zero().with(STR,1))),new FileOps() {
                    @Override public void force(FileChannel channel) throws IOException { forces.incrementAndGet(); FileOps.super.force(channel); }
                })) {
            var before = harness.read().snapshot(); assertEquals(Long.MAX_VALUE,before.modifiers().get(STR));
            var failure = assertThrows(ModernBackendExecutor.BackendException.class,() -> harness.set(before,2,true));
            assertEquals(ModernBackendExecutor.Failure.BACKEND,failure.failure()); assertNull(failure.getCause());
            assertEquals(0,forces.get()); assertEquals(0,harness.characterFiles()); assertTrue(harness.stored().isEmpty());
            assertEquals(before.viewToken(),harness.read().snapshot().viewToken());
        }
    }

    @Test void uncertainMovePropagatesAsFailureAndNextReadReconcilesCommittedRevision() throws Exception {
        var moves = new AtomicInteger();
        try (var harness = new Harness(directory,qualified(armor(1,100,false,EquipmentRules.ArmorSlot.BODY,Ability.zero())),new FileOps() {
            @Override public void move(Path from,Path to) throws IOException {
                FileOps.super.move(from,to); if(moves.incrementAndGet()==1)throw new IOException("private synthetic move uncertainty");
            }
        })) {
            var before = harness.read().snapshot();
            assertThrows(ModernBackendExecutor.BackendException.class,() -> harness.set(before,1,true));
            assertEquals(ModernEquipmentOverrideStore.Failure.COMMIT_UNCERTAIN,harness.storageFailure.get());
            assertFalse(harness.lastOperation.get().isCommitAdmitted());
            var actual = harness.read().snapshot(); assertEquals(1,actual.revision()); assertTrue(actual.equipped().get(0).equipped());
            var retry = harness.set(actual,1,true); assertEquals(OK,retry.status()); assertFalse(retry.changed());
            assertEquals(1,retry.snapshot().revision()); assertEquals(1,moves.get());
        }
    }

    @Test void transportCancellationAfterAdmissionRetainsRoleUntilActualAuthorityWorkFinally() throws Exception {
        var admitted = new CountDownLatch(1); var release = new CountDownLatch(1);
        var callers = Executors.newFixedThreadPool(2);
        var fresh = qualified(armor(1,100,false,EquipmentRules.ArmorSlot.BODY,Ability.zero()));
        try (var harness = new Harness(directory,fresh,new FileOps() {
            @Override public void move(Path from,Path to) throws IOException { admitted.countDown(); await(release); FileOps.super.move(from,to); }
        })) {
            try {
                var before = harness.read().snapshot();
                var pending = callers.submit(() -> harness.set(before,1,true)); assertTrue(admitted.await(2,TimeUnit.SECONDS));
                var operation = harness.lastOperation.get(); assertTrue(operation.isCommitAdmitted());
                callers.submit(() -> harness.transport.get().failClosed()).get(1,TimeUnit.SECONDS);
                assertTrue(harness.serverSocket.get().isClosed()); assertTrue(operation.isCommitAdmitted());
                assertEquals(1,harness.presence.ownerCount());
                var cause = assertThrows(ExecutionException.class,() -> pending.get(1,TimeUnit.SECONDS)).getCause();
                assertInstanceOf(ModernBackendExecutor.BackendException.class,cause);
                var contender = harness.presence.connect();
                try {
                    assertEquals(TournamentPresence.SelectStatus.BUSY,harness.presence.select(contender,selection()));
                    release.countDown(); harness.backend.close(); assertTrue(harness.backend.awaitTermination(3_000));
                    assertFalse(operation.isCommitAdmitted()); assertEquals(1,harness.presence.ownerCount());
                    assertEquals(TournamentPresence.SelectStatus.ACCEPTED,harness.presence.select(contender,selection()));
                } finally { harness.presence.disconnect(contender); }
            } finally { release.countDown(); }
        } finally { release.countDown(); callers.shutdown(); assertTrue(callers.awaitTermination(4,TimeUnit.SECONDS)); }
        // Reconnect with a newly authenticated/selected owner and real Work,
        // not a fabricated lease/scope, to observe the cancelled commit on disk.
        try (var reconnected = new Harness(directory,fresh)) {
            var recovered = reconnected.read().snapshot(); assertEquals(1,recovered.revision());
            assertTrue(recovered.equipped().get(0).equipped()); assertEquals(1,reconnected.stored().orElseThrow().revision());
        }
    }

    @Test void providerCannotSubstituteAnotherSelectedCharacterOrSource() throws Exception {
        try (var harness = new Harness(directory,qualified(armor(1,100,false,EquipmentRules.ArmorSlot.BODY,Ability.zero())))) {
            var original = harness.input.get();
            harness.input.set(withCharacter(original,new ModernApplicationProtocol.CharacterRow(3334,ROW.name(),0,0,80)));
            var wrongObject = harness.read(); assertEquals(BACKEND_UNAVAILABLE,wrongObject.status()); assertNull(wrongObject.snapshot());
            var catalog = original.catalog();
            var wrongSource = new ModernCharacterData.Snapshot(42,ROW,original.source().persisted(),original.source().inventory());
            harness.input.set(new Qualified(wrongSource,original.items(),new Catalog(42,catalog.resourceDatabase(),catalog.lookups(),
                    catalog.verifiedSetIds(),catalog.sets(),catalog.combos(),catalog.vipTableState()),
                    ModernItemLifetime.Observation.verifiedPermanent(wrongSource)));
            var wrongRoute = harness.read(); assertEquals(BACKEND_UNAVAILABLE,wrongRoute.status()); assertNull(wrongRoute.snapshot());
            assertEquals(0,harness.characterFiles());
        }
    }

    private static void denied(Result result,EquipmentRules.Reason reason,long blocking,Snapshot expected) {
        assertEquals(DENIED,result.status()); assertFalse(result.changed()); assertEquals(reason.ordinal()+1,result.reason());
        assertEquals(blocking,result.blockingItem()); assertEquals(expected.viewToken(),result.snapshot().viewToken());
        assertEquals(expected.revision(),result.snapshot().revision()); assertEquals(expected.equipped(),result.snapshot().equipped());
    }
    private static Instance armor(long object,long catalog,boolean equipped,EquipmentRules.ArmorSlot slot,Ability ability) {
        var item = ModernEquipmentEvaluatorTest.instance(object,catalog,ModernCharacterData.ItemKind.ARMOR,equipped,0,"$"+catalog,
                List.of(),ability,Ability.zero(),ModernEquipmentEvaluatorTest.emptyCards(),Stone.none(),TemplatePhysical.zero());
        return permissions(item,new EquipmentRules.ArmorFacts(127,new EquipmentRules.LevelRange(1,Long.MAX_VALUE),slot));
    }
    private static Instance weapon(long object,long catalog,boolean equipped,boolean twoHanded,long blessing,Ability ability) {
        var item = ModernEquipmentEvaluatorTest.instance(object,catalog,ModernCharacterData.ItemKind.WEAPON,equipped,0,"$"+catalog,
                List.of(),ability,Ability.zero(),ModernEquipmentEvaluatorTest.emptyCards(),Stone.none(),TemplatePhysical.zero());
        var source = item.source();
        source = new ModernCharacterData.InventoryItem(source.objectId(),source.template(),source.quantity(),source.equipped(),source.enchantment(),
                source.durability(),source.identified(),blessing,source.sealed(),source.chargeCount());
        item = new Instance(source,item.template(),item.greater(),item.dark(),item.cards(),item.stone());
        return permissions(item,new EquipmentRules.WeaponFacts(127,new EquipmentRules.LevelRange(1,Long.MAX_VALUE),twoHanded));
    }
    private static Instance permissions(Instance item,EquipmentRules.Facts permissions) {
        var template = item.template();
        long type = permissions instanceof EquipmentRules.ArmorFacts armor ? armor.slot().retainedType() : template.retainedType();
        return new Instance(item.source(),new Template(template.metadata(),permissions,type,template.safeEnchant(),template.setIds(),template.base(),template.physical()),
                item.greater(),item.dark(),item.cards(),item.stone());
    }
    private static Instance quantity(Instance item,long quantity) {
        var source = item.source();
        return new Instance(new ModernCharacterData.InventoryItem(source.objectId(),source.template(),quantity,source.equipped(),source.enchantment(),
                source.durability(),source.identified(),source.blessing(),source.sealed(),source.chargeCount()),item.template(),item.greater(),item.dark(),item.cards(),item.stone());
    }
    private static Qualified qualified(Instance... items) { return withCharacter(ModernEquipmentEvaluatorTest.qualified(List.of(items)),ROW); }
    private static Qualified withCharacter(Qualified source,ModernApplicationProtocol.CharacterRow character) {
        var snapshot=new ModernCharacterData.Snapshot(source.source().sourceId(),character,source.source().persisted(),source.source().inventory());
        return new Qualified(snapshot,source.items(),source.catalog(),
                character.objectId()==source.source().character().objectId()?source.lifetimes():
                ModernItemLifetime.Observation.verifiedPermanent(snapshot));
    }
    private static TournamentPresence.Selection selection() { return new TournamentPresence.Selection(KEY,"Sun",ROW.name(),ROW.level(),ROW.characterClass(),ROW.sex()); }
    private static void await(CountDownLatch latch) throws IOException {
        try { if(!latch.await(4,TimeUnit.SECONDS))throw new IOException("authority test barrier timeout"); }
        catch(InterruptedException failure){Thread.currentThread().interrupt();throw new IOException("authority test interrupted");}
    }

    private static final class Harness implements AutoCloseable {
        final Path path;
        final AtomicLong clock = new AtomicLong(1000);
        final AtomicReference<Qualified> input;
        final AtomicReference<ModernEquipmentLease.Operation> lastOperation = new AtomicReference<>();
        final AtomicReference<ModernEquipmentOverrideStore.Failure> storageFailure = new AtomicReference<>();
        final TournamentPresence presence = new TournamentPresence();
        final ModernBackendExecutor backend = new ModernBackendExecutor(2,2);
        final java.util.concurrent.ExecutorService worker = Executors.newSingleThreadExecutor();
        final AtomicReference<ModernNavigationSession> navigation = new AtomicReference<>();
        final AtomicReference<ModernTlsSessionTransport> transport = new AtomicReference<>();
        final AtomicReference<SSLSocket> serverSocket = new AtomicReference<>();
        final AtomicBoolean closed = new AtomicBoolean();
        final AtomicReference<ModernServerConfig.SourceConfig> source = new AtomicReference<>();
        final List<ModernServerConfig> configurations = new ArrayList<>();
        final FileOps files;
        ModernEquipmentOverrideStore store;
        ModernEquipmentAuthority authority;
        SSLServerSocket listener;
        SSLSocket client;
        java.util.concurrent.Future<?> server;
        Harness(Path path,Qualified input) throws Exception { this(path,input,new FileOps() {}); }
        Harness(Path path,Qualified input,FileOps files) throws Exception {
            this.path=path;this.input=new AtomicReference<>(input);this.files=files;
            boolean ready=false;
            try {
                Files.createDirectories(path); store=new ModernEquipmentOverrideStore(path,files);
                configure("lito_7"); rebuildAuthority();
                listener=(SSLServerSocket)serverContext.getServerSocketFactory().createServerSocket(0,1,InetAddress.getByName("127.0.0.1"));
                listener.setSoTimeout(3_000);
                server=worker.submit(() -> {
                    try(var accepted=(SSLSocket)listener.accept()) {
                        serverSocket.set(accepted);
                        var connection=new ModernTlsSessionTransport(accepted,ModernSessionBoundary.Policy.current(),
                                new ModernTlsSessionTransport.Limits(3_000,3_000),(account,password) ->
                                ACCOUNT.equals(account)&&Arrays.equals(PASSWORD.toCharArray(),password)?PRINCIPAL:null);
                        transport.set(connection);
                        ModernCharacterRepository characters=(principal,id) -> {
                            if(id!=7||!"canonical-fixture-owner".equals(principal.accountForSource(id)))throw new IOException("invalid fixture owner");
                            return List.of(ROW);
                        };
                        var application=new ModernApplicationSession(connection,characters,backend,10_000,5_000,presence);
                        Field field=ModernApplicationSession.class.getDeclaredField("navigation");field.setAccessible(true);
                        navigation.set((ModernNavigationSession)field.get(application)); application.run();
                    }
                    return null;
                });
                int port=listener.getLocalPort();
                client=(SSLSocket)clientContext.getSocketFactory().createSocket(new Socket("127.0.0.1",port),"localhost",port,true);
                var parameters=client.getSSLParameters();parameters.setEndpointIdentificationAlgorithm("HTTPS");client.setSSLParameters(parameters);client.setSoTimeout(3_000);
                client.startHandshake();
                client.getOutputStream().write(ModernSessionProtocol.encode(new ModernSessionProtocol.Hello(1,1,1,7,3,1)));client.getOutputStream().flush();
                assertInstanceOf(ModernSessionProtocol.Ack.class,ModernSessionProtocol.decode(ModernSessionProtocol.readFrame(client.getInputStream())));
                try(var auth=new ModernApplicationProtocol.Auth(2,ACCOUNT,PASSWORD.toCharArray())) {
                    assertEquals(0,((ModernApplicationProtocol.AuthResult)exchange(auth)).status());
                }
                assertEquals(0,((ModernApplicationProtocol.CharacterResult)exchange(new ModernApplicationProtocol.CharacterRequest(3,7,EPOCH))).status());
                assertEquals(0,((ModernApplicationProtocol.NavigationResult)exchange(new ModernApplicationProtocol.CharacterSelection(4,7,3333,EPOCH))).status());
                ready=true;
            } finally { if(!ready)close(); }
        }
        Result read() throws Exception { return call(new Request(7,3333,EPOCH,READ,false,0,0,ZERO)); }
        Result set(Snapshot expected,long item,boolean desired) throws Exception {
            return call(new Request(7,3333,EPOCH,SET,desired,item,expected.revision(),expected.viewToken()));
        }
        Result call(Request request) throws Exception {
            return navigation.get().equipment(request.sourceId(),request.objectId(),request.epoch(),(principal,selected,operation) -> {
                lastOperation.set(operation);
                try{return authority.execute(principal,selected,operation,request);}
                catch(StoreException failure){storageFailure.set(failure.failure());throw failure;}
            });
        }
        Optional<Stored> stored() throws Exception {
            return navigation.get().equipment(7,3333,EPOCH,(principal,selected,operation) -> store.read(KEY,operation.scope()));
        }
        long characterFiles() throws IOException {
            try(var files=Files.list(path)){return files.filter(file -> file.getFileName().toString().endsWith(".meo")).count();}
        }
        void reopenStore() throws Exception { store.close();assertTrue(store.isQuiescent());store=new ModernEquipmentOverrideStore(path,files);rebuildAuthority(); }
        void changeRoute() throws Exception { configure("lito_7_rebound"); }
        void configure(String database) throws Exception {
            String config="schema=1\nbind.port=0\ntls.pkcs12="+path.resolve("unused.p12").toString().replace('\\','/')
                    +"\ntls.password.env=T_TLS\nsources=7\nsource.7.name=Sun\nsource.7.mysql.host=127.0.0.1\nsource.7.mysql.port=3306"
                    +"\nsource.7.mysql.database="+database+"\nsource.7.mysql.username.env=T_USER\nsource.7.mysql.password.env=T_PASSWORD"
                    +"\nsource.7.mysql.passwordScheme=plain\nsource.7.mysql.tls=disabled\nsource.7.catalog.database=catalog\n";
            var parsed=ModernServerConfig.parse(config.getBytes(java.nio.charset.StandardCharsets.UTF_8),name -> switch(name){
                case "T_TLS"->"test-key-only";case "T_USER"->"test-user-only";case "T_PASSWORD"->"test-password-only";default->null;});
            configurations.add(parsed);source.set(parsed.sources().get(0));
        }
        void rebuildAuthority() {
            authority=new ModernEquipmentAuthority((principal,id,object) -> {
                assertEquals("canonical-fixture-owner",principal.accountForSource(id));assertEquals(KEY.objectId(),object);
                return new Loaded(input.get(),ModernEquipmentBinding.ownerBinding(principal,source.get()));
            },store,clock::get);
        }
        ModernApplicationProtocol.Message exchange(ModernApplicationProtocol.Message request) throws Exception {
            byte[] encoded=ModernApplicationProtocol.encode(request);
            try{client.getOutputStream().write(encoded);client.getOutputStream().flush();}finally{Arrays.fill(encoded,(byte)0);}
            byte[] reply=ModernApplicationProtocol.readFrame(client.getInputStream());
            try{return ModernApplicationProtocol.decode(reply);}finally{if(reply!=null)Arrays.fill(reply,(byte)0);}
        }
        @Override public void close() throws Exception {
            if(!closed.compareAndSet(false,true))return;
            if(transport.get()!=null)transport.get().failClosed();
            if(client!=null)try{client.close();}catch(IOException ignored){}
            if(listener!=null)listener.close();
            backend.close();worker.shutdown();
            boolean retired=backend.awaitTermination(5_000);boolean threadRetired=worker.awaitTermination(5,TimeUnit.SECONDS);
            assertTrue(retired);assertTrue(threadRetired);
            if(server!=null) {
                try{server.get(1,TimeUnit.SECONDS);}
                catch(ExecutionException failure){assertInstanceOf(IOException.class,failure.getCause());}
            }
            if(store!=null){store.close();assertTrue(store.isQuiescent());}
            for(var config:configurations)config.close();
            assertEquals(0,presence.ownerCount());assertEquals(0,presence.userCount());
        }
    }
}
