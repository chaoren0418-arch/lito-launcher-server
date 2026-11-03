package local.lito381.server;

import static org.junit.jupiter.api.Assertions.*;
import static local.lito381.server.ModernEquipmentOverrideStore.*;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ModernEquipmentOverrideStoreTest {
    @TempDir Path root;
    static final TournamentPresence.CharacterKey KEY = new TournamentPresence.CharacterKey(42, 5_000_003_333L);

    @Test void boundedCanonicalCodecPreservesEveryI64AndDeepImmutableFingerprints() throws Exception {
        byte[] raw = new byte[32]; raw[0] = 9;
        var fingerprint = new Fingerprint(raw); raw[0] = 0;
        byte[] escaped = fingerprint.bytes(); escaped[0] = 0;
        assertEquals(9, fingerprint.bytes()[0]);
        assertEquals("EquipmentFingerprint[redacted]", fingerprint.toString());
        var values = new ArrayList<ItemOverride>();
        for (int index = 0; index < 512; index++) {
            values.add(new ItemOverride(Long.MAX_VALUE - index, Long.MAX_VALUE, fingerprint, index % 2 == 0));
        }
        var replacement = new Replacement(fp(1), values); values.clear();
        var key = new TournamentPresence.CharacterKey(Integer.MAX_VALUE, Long.MAX_VALUE);
        var stored = new Stored(key, Long.MAX_VALUE, replacement);
        byte[] encoded = encode(stored);
        assertEquals(MAX_BYTES, encoded.length); assertEquals(stored, decode(encoded, key));
        assertEquals(Long.MAX_VALUE - 511, replacement.overrides().get(0).itemObjectId());
        assertThrows(UnsupportedOperationException.class, () -> replacement.overrides().clear());
        assertThrows(IllegalArgumentException.class, () -> new Fingerprint(new byte[31]));
        assertThrows(IllegalArgumentException.class, () -> new Replacement(fp(1), java.util.Collections.nCopies(513, item(1, true))));
        assertThrows(IllegalArgumentException.class, () -> new Replacement(fp(1), List.of(item(1, true), item(1, false))));
        assertThrows(IllegalArgumentException.class, () -> new Expected(0, fp(1)));
        assertThrows(IllegalArgumentException.class, () -> new Expected(1, null));
    }

    @Test void codecRejectsMalformedButRehashedHeadersRowsAndExactSize() throws Exception {
        byte[] valid = encode(new Stored(KEY, 1, new Replacement(fp(1), List.of(item(1, true), item(2, false)))));
        int[] badOffsets = {0, 4, 6, 8, 12, 20, 60, 64, 72, 112, 113};
        for (int offset : badOffsets) {
            byte[] bytes = valid.clone();
            switch (offset) {
                case 0 -> ByteBuffer.wrap(bytes).putInt(0, 0);
                case 4, 6 -> ByteBuffer.wrap(bytes).putShort(offset, (short) 9);
                case 8 -> ByteBuffer.wrap(bytes).putInt(8, 7);
                case 12 -> ByteBuffer.wrap(bytes).putLong(12, 7);
                case 20 -> ByteBuffer.wrap(bytes).putLong(20, 0);
                case 60 -> ByteBuffer.wrap(bytes).putInt(60, 513);
                case 64, 72 -> ByteBuffer.wrap(bytes).putLong(offset, 0);
                case 112 -> bytes[offset] = 2;
                case 113 -> ByteBuffer.wrap(bytes).putLong(offset, 1); // duplicate object
                default -> fail("unhandled mutation");
            }
            rehash(bytes); assertCorrupt(bytes, KEY);
        }
        byte[] flipped = valid.clone(); flipped[35] ^= 1; assertCorrupt(flipped, KEY);
        assertCorrupt(Arrays.copyOf(valid, valid.length - 1), KEY);
        assertCorrupt(Arrays.copyOf(valid, valid.length + 1), KEY);
        assertCorrupt(valid, new TournamentPresence.CharacterKey(43, KEY.objectId()));
        assertCorrupt(null, KEY);
    }

    @Test void absentCreateIdempotentRetryStaleConflictAndReopen() throws Exception {
        Stored first;
        try (var store = new ModernEquipmentOverrideStore(root)) {
            assertTrue(store.read(KEY, scope()).isEmpty());
            var created = commit(store, KEY, Expected.absent(), replacement(true));
            first = created.stored(); assertTrue(created.changed()); assertEquals(1, first.revision());
            var unchanged = commit(store, KEY, Expected.of(first), replacement(true));
            assertFalse(unchanged.changed()); assertEquals(first, unchanged.stored());
            assertFailure(Failure.CONFLICT, () -> commit(store, KEY, Expected.absent(), replacement(false)));
            var second = commit(store, KEY, Expected.of(first), replacement(false));
            assertEquals(2, second.stored().revision());
            assertFailure(Failure.CONFLICT, () -> commit(store, KEY, Expected.of(first), replacement(false)));
            assertEquals(second.stored(), store.read(KEY, scope()).orElseThrow());
        }
        try (var reopened = new ModernEquipmentOverrideStore(root)) {
            assertEquals(2, reopened.read(KEY, scope()).orElseThrow().revision());
            assertFalse(reopened.read(KEY, scope()).orElseThrow().value().overrides().get(0).equipped());
        }
        assertNoTemporaryFiles();
    }

    @Test void numericSourceSeparatesSameCharacterObjidAndCopiesCannotCrossKeys() throws Exception {
        var other = new TournamentPresence.CharacterKey(43, KEY.objectId());
        try (var store = new ModernEquipmentOverrideStore(root)) {
            commit(store, KEY, Expected.absent(), replacement(true));
            assertTrue(store.read(other, scope()).isEmpty());
            commit(store, other, Expected.absent(), replacement(false));
            assertTrue(store.read(KEY, scope()).orElseThrow().value().overrides().get(0).equipped());
            assertFalse(store.read(other, scope()).orElseThrow().value().overrides().get(0).equipped());
            Files.write(recordPath(other), Files.readAllBytes(recordPath(KEY)));
            assertFailure(Failure.CORRUPT, () -> store.read(other, scope()));
        }
    }

    @Test void explicitEmptyRebindPreservesRevisionAndNeverTransfersOldOverrides() throws Exception {
        try (var store = new ModernEquipmentOverrideStore(root)) {
            var first = commit(store, KEY, Expected.absent(), replacement(true)).stored();
            assertFailure(Failure.CONFLICT, () -> commit(store, KEY, new Expected(1, fp(2)), replacement(false)));
            assertFailure(Failure.INVALID_REBIND, () -> commit(store, KEY, Expected.of(first),
                    new Replacement(fp(2), List.of(item(1, true)))));
            var rebound = commit(store, KEY, Expected.of(first), new Replacement(fp(2), List.of())).stored();
            assertEquals(2, rebound.revision()); assertTrue(rebound.value().overrides().isEmpty());
            assertFailure(Failure.CONFLICT, () -> commit(store, KEY, Expected.of(first), replacement(false)));
            var next = commit(store, KEY, Expected.of(rebound), new Replacement(fp(2), List.of(item(2, true)))).stored();
            assertEquals(3, next.revision());
        }
    }

    @Test void eachItemFingerprintSurvivesUnrelatedItemChanges() throws Exception {
        try (var store = new ModernEquipmentOverrideStore(root)) {
            var stable = item(2, true);
            var first = commit(store, KEY, Expected.absent(), new Replacement(fp(1), List.of(item(1, false), stable))).stored();
            var changedSource = new ItemOverride(1, Long.MAX_VALUE, fp(7), true);
            var second = commit(store, KEY, Expected.of(first), new Replacement(fp(1), List.of(stable, changedSource))).stored();
            assertEquals(stable, second.value().overrides().get(1));
            assertEquals(fp(7), store.read(KEY, scope()).orElseThrow().value().overrides().get(0).sourceItem());
            // Reconciliation against fresh source data belongs to the authority,
            // not this numeric persistence layer.
        }
    }

    @Test void exhaustedRevisionCannotWrapOrOverwrite() throws Exception {
        var max = new Stored(KEY, Long.MAX_VALUE, replacement(true));
        Files.write(recordPath(KEY), encode(max));
        try (var store = new ModernEquipmentOverrideStore(root)) {
            assertFailure(Failure.REVISION_EXHAUSTED, () -> commit(store, KEY, Expected.of(max), replacement(false)));
            assertEquals(max, store.read(KEY, scope()).orElseThrow());
            assertFalse(commit(store, KEY, Expected.of(max), replacement(true)).changed());
        }
    }

    @Test void corruptAndTruncatedRecordsNeverBecomeEmptyInventories() throws Exception {
        try (var store = new ModernEquipmentOverrideStore(root)) {
            Files.write(recordPath(KEY), new byte[95]);
            assertFailure(Failure.CORRUPT, () -> store.read(KEY, scope()));
            assertFailure(Failure.CORRUPT, () -> commit(store, KEY, Expected.absent(), replacement(true)));
            Files.write(recordPath(KEY), new byte[MAX_BYTES + 1]);
            assertFailure(Failure.CORRUPT, () -> store.read(KEY, scope()));
            assertEquals(MAX_BYTES + 1, Files.size(recordPath(KEY)));
        }
    }

    @Test void lifetimeLockExcludesWritersAndCloseAllowsReopen() throws Exception {
        var first = new ModernEquipmentOverrideStore(root);
        try { assertFailure(Failure.BUSY, () -> new ModernEquipmentOverrideStore(root)); }
        finally { first.close(); }
        assertTrue(first.isQuiescent());
        assertFailure(Failure.CLOSED, () -> first.read(KEY, scope()));
        try (var second = new ModernEquipmentOverrideStore(root)) { assertTrue(second.read(KEY, scope()).isEmpty()); }
    }

    @Test void actualWindowsLockEntryCannotBeDeletedOrRenamedWhileItsOwnerLives() throws Exception {
        assertEquals("\\", root.getFileSystem().getSeparator(), "Windows sharing regression must run on Windows");
        Path lock = root.resolve(".modern-equipment.lock");
        Path renamed = root.resolve("renamed-test-lock");
        try (var store = new ModernEquipmentOverrideStore(root)) {
            assertThrows(IOException.class, () -> Files.delete(lock));
            assertThrows(IOException.class, () -> Files.move(lock, renamed));
            assertTrue(Files.isRegularFile(lock)); assertFalse(Files.exists(renamed));
            assertTrue(store.read(KEY, scope()).isEmpty());
        }
        Files.move(lock, renamed); assertTrue(Files.isRegularFile(renamed));
        try (var reopened = new ModernEquipmentOverrideStore(root)) { assertTrue(reopened.read(KEY, scope()).isEmpty()); }
    }

    @Test void forceFailureDoesNotAdmitOrReplaceAndCleansOnlyOwnedTemporary() throws Exception {
        var initial = seed();
        try (var store = new ModernEquipmentOverrideStore(root, new FileOps() {
            @Override public void force(FileChannel file) throws IOException { throw new IOException("private disk path"); }
        }); var call = new Call(KEY)) {
            var failure = assertThrows(StoreException.class,
                    () -> store.compareAndSet(KEY, Expected.of(initial), replacement(false), call.scope, call.operation));
            assertEquals(Failure.IO, failure.failure()); assertNull(failure.getCause());
            assertFalse(failure.getMessage().contains("private")); assertFalse(call.operation.isCommitAdmitted());
            assertEquals(initial, store.read(KEY, scope()).orElseThrow());
        }
        assertNoTemporaryFiles();
    }

    @Test void atomicMoveFailureHasNoFallbackAndCanBeReconciledAgainstOldRevision() throws Exception {
        var initial = seed(); var attempts = new AtomicInteger();
        try (var store = new ModernEquipmentOverrideStore(root, new FileOps() {
            @Override public void move(Path from, Path to) throws IOException {
                attempts.incrementAndGet(); throw new java.nio.file.AtomicMoveNotSupportedException("private", "private", "test");
            }
        }); var call = new Call(KEY)) {
            assertFailure(Failure.COMMIT_UNCERTAIN,
                    () -> store.compareAndSet(KEY, Expected.of(initial), replacement(false), call.scope, call.operation));
            assertTrue(call.operation.isCommitAdmitted()); assertEquals(1, attempts.get());
            assertEquals(initial, store.read(KEY, scope()).orElseThrow());
        }
        assertNoTemporaryFiles();
    }

    @Test void exceptionAfterActualRenameIsUncertainButNextReadFindsNewRevision() throws Exception {
        var initial = seed();
        try (var store = new ModernEquipmentOverrideStore(root, new FileOps() {
            @Override public void move(Path from, Path to) throws IOException {
                FileOps.super.move(from, to); throw new IOException("lost acknowledgment after replacement");
            }
        })) {
            assertFailure(Failure.COMMIT_UNCERTAIN, () -> commit(store, KEY, Expected.of(initial), replacement(false)));
            var reconciled = store.read(KEY, scope()).orElseThrow();
            assertEquals(2, reconciled.revision()); assertEquals(replacement(false), reconciled.value());
            assertFailure(Failure.CONFLICT, () -> commit(store, KEY, Expected.of(initial), replacement(true)));
            assertFalse(commit(store, KEY, Expected.of(reconciled), replacement(false)).changed());
        }
        assertNoTemporaryFiles();
    }

    @Test void cancellationAfterForceBeforeAdmissionCannotModifyAuthoritativeRecord() throws Exception {
        var initial = seed();
        try (var call = new Call(KEY); var store = new ModernEquipmentOverrideStore(root, new FileOps() {
            @Override public void force(FileChannel file) throws IOException {
                FileOps.super.force(file); call.scope.cancel(ModernBackendExecutor.Failure.CANCELLED);
            }
        })) {
            assertEquals(ModernBackendExecutor.Failure.CANCELLED,
                    assertThrows(ModernBackendExecutor.BackendException.class,
                            () -> store.compareAndSet(KEY, Expected.of(initial), replacement(false), call.scope, call.operation)).failure());
            assertFalse(call.operation.isCommitAdmitted());
            assertEquals(initial, store.read(KEY, scope()).orElseThrow());
        }
        assertNoTemporaryFiles();
    }

    @Test void invalidationAfterForceBeforeAdmissionRetiresWithoutAnyCommit() throws Exception {
        var initial = seed();
        try (var call = new Call(KEY); var store = new ModernEquipmentOverrideStore(root, new FileOps() {
            @Override public void force(FileChannel file) throws IOException {
                FileOps.super.force(file); call.lease.invalidate();
            }
        })) {
            assertFailure(Failure.INVALID_OPERATION,
                    () -> store.compareAndSet(KEY, Expected.of(initial), replacement(false), call.scope, call.operation));
            assertTrue(call.lease.isRetired()); assertEquals(1, call.retired.get());
            assertEquals(initial, store.read(KEY, scope()).orElseThrow());
        }
    }

    @Test void cancellationAndInvalidationAfterAdmissionDoNotUndoPersistedCommitOrReleasePermit() throws Exception {
        var initial = seed();
        try (var call = new Call(KEY); var store = new ModernEquipmentOverrideStore(root, new FileOps() {
            @Override public void move(Path from, Path to) throws IOException {
                assertTrue(call.operation.isCommitAdmitted());
                call.scope.cancel(ModernBackendExecutor.Failure.CANCELLED); call.lease.invalidate();
                assertFalse(call.lease.isRetired()); FileOps.super.move(from, to);
            }
        })) {
            var committed = store.compareAndSet(KEY, Expected.of(initial), replacement(false), call.scope, call.operation);
            assertEquals(2, committed.stored().revision()); assertEquals(0, call.retired.get());
            assertTrue(call.lease.hasActivePermit());
            assertEquals(committed.stored(), store.read(KEY, scope()).orElseThrow());
            call.operation.close(); assertEquals(1, call.retired.get()); assertTrue(call.lease.isRetired());
        }
    }

    @Test void wrongKeyAndDifferentScopeCannotUseOperationEvenForNoOp() throws Exception {
        try (var store = new ModernEquipmentOverrideStore(root); var call = new Call(KEY)) {
            assertFailure(Failure.INVALID_OPERATION, () -> store.compareAndSet(KEY, Expected.absent(), replacement(true), scope(), call.operation));
            assertFailure(Failure.INVALID_OPERATION, () -> store.compareAndSet(new TournamentPresence.CharacterKey(43, KEY.objectId()),
                    Expected.absent(), replacement(true), call.scope, call.operation));
            assertTrue(store.read(KEY, scope()).isEmpty());
        }
    }

    @Test void twoConcurrentCasWritersCannotBothAdvanceSameRevision() throws Exception {
        var initial = seed(); var start = new CountDownLatch(1); var threads = Executors.newFixedThreadPool(2);
        try (var store = new ModernEquipmentOverrideStore(root)) {
            var tasks = new ArrayList<java.util.concurrent.Future<Boolean>>();
            for (int index = 0; index < 2; index++) tasks.add(threads.submit(() -> {
                await(start);
                try { commit(store, KEY, Expected.of(initial), replacement(false)); return true; }
                catch (StoreException conflict) { assertEquals(Failure.CONFLICT, conflict.failure()); return false; }
            }));
            start.countDown(); int won = 0;
            for (var task : tasks) if (task.get(3, TimeUnit.SECONDS)) won++;
            assertEquals(1, won); assertEquals(2, store.read(KEY, scope()).orElseThrow().revision());
        } finally { start.countDown(); threads.shutdown(); assertTrue(threads.awaitTermination(3, TimeUnit.SECONDS)); }
    }

    @Test void closeDoesNotWaitForActiveDataIoOrReleaseItsLifetimeLockPrematurely() throws Exception {
        var entered = new CountDownLatch(1); var release = new CountDownLatch(1); var threads = Executors.newFixedThreadPool(2);
        var store = new ModernEquipmentOverrideStore(root, new FileOps() {
            @Override public void force(FileChannel file) throws IOException {
                FileOps.super.force(file); entered.countDown();
                try { await(release); } catch (InterruptedException failure) { Thread.currentThread().interrupt(); throw new IOException(failure); }
            }
        });
        try {
            var pending = threads.submit(() -> commit(store, KEY, Expected.absent(), replacement(true)));
            assertTrue(entered.await(2, TimeUnit.SECONDS));
            threads.submit(store::close).get(1, TimeUnit.SECONDS);
            assertFalse(store.isQuiescent()); assertFailure(Failure.BUSY, () -> new ModernEquipmentOverrideStore(root));
            assertFailure(Failure.CLOSED, () -> store.read(KEY, scope()));
            release.countDown(); assertEquals(1, pending.get(3, TimeUnit.SECONDS).stored().revision());
            assertTrue(store.isQuiescent());
            // Store close rejects new calls; invalidating selection capabilities
            // is the authority's separate duty. This already admitted IO may finish.
            try (var reopened = new ModernEquipmentOverrideStore(root)) { assertEquals(1, reopened.read(KEY, scope()).orElseThrow().revision()); }
        } finally { release.countDown(); store.close(); threads.shutdown(); assertTrue(threads.awaitTermination(3, TimeUnit.SECONDS)); }
    }

    @Test void queuedIoWaitHonorsItsOwnDeadline() throws Exception {
        var entered = new CountDownLatch(1); var release = new CountDownLatch(1); var threads = Executors.newSingleThreadExecutor();
        try (var store = new ModernEquipmentOverrideStore(root, new FileOps() {
            @Override public void force(FileChannel file) throws IOException {
                entered.countDown();
                try { await(release); } catch (InterruptedException failure) { Thread.currentThread().interrupt(); throw new IOException(failure); }
                FileOps.super.force(file);
            }
        })) {
            var pending = threads.submit(() -> commit(store, KEY, Expected.absent(), replacement(true)));
            assertTrue(entered.await(2, TimeUnit.SECONDS));
            assertEquals(ModernBackendExecutor.Failure.DEADLINE,
                    assertThrows(ModernBackendExecutor.BackendException.class, () -> store.read(KEY, scope(40))).failure());
            release.countDown(); assertTrue(pending.get(3, TimeUnit.SECONDS).changed());
        } finally { release.countDown(); threads.shutdown(); assertTrue(threads.awaitTermination(3, TimeUnit.SECONDS)); }
    }

    @Test void invalidDirectoriesAndUnsafeAliasesAreRejectedWithoutCreatingData() throws Exception {
        assertFailure(Failure.INVALID_PATH, () -> new ModernEquipmentOverrideStore(Path.of("relative")));
        assertFailure(Failure.INVALID_PATH, () -> new ModernEquipmentOverrideStore(root.resolve("absent")));
        assertFailure(Failure.INVALID_PATH, () -> new ModernEquipmentOverrideStore(root.resolve("..")));
        var plain = root.resolve("plain-file"); Files.write(plain, new byte[0]);
        assertFailure(Failure.INVALID_PATH, () -> new ModernEquipmentOverrideStore(plain));
    }

    @Test void forcedCommitSurvivesSeparateProcessReopenAndCrossProcessLockExcludesWriter() throws Exception {
        assertEquals("WRITE_OK", peer("write"));
        assertEquals("READ_OK", peer("read"));
        try (var store = new ModernEquipmentOverrideStore(root)) {
            assertEquals("BUSY_OK", peer("busy"));
            assertEquals(1, store.read(KEY, scope()).orElseThrow().revision());
        }
        assertEquals("READ_OK", peer("read"));
    }

    private String peer(String action) throws Exception {
        String exe = Path.of(System.getProperty("java.home"), "bin", "java.exe").toString();
        String cp = System.getProperty("surefire.test.class.path", System.getProperty("java.class.path"));
        var process = new ProcessBuilder(exe, "-cp", cp, ModernEquipmentStoreTestPeer.class.getName(), action, root.toString())
                .redirectErrorStream(true).start();
        try {
            assertTrue(process.waitFor(5, TimeUnit.SECONDS), "owned equipment peer timed out");
            String output = new String(process.getInputStream().readAllBytes(), java.nio.charset.StandardCharsets.UTF_8).trim();
            assertEquals(0, process.exitValue(), output); return output;
        } finally {
            if (process.isAlive()) { process.destroyForcibly(); assertTrue(process.waitFor(3, TimeUnit.SECONDS)); }
        }
    }
    private Stored seed() throws Exception {
        try (var store = new ModernEquipmentOverrideStore(root)) { return commit(store, KEY, Expected.absent(), replacement(true)).stored(); }
    }
    private Path recordPath(TournamentPresence.CharacterKey key) {
        return root.resolve("source-" + key.sourceId() + "-character-" + key.objectId() + ".meo");
    }
    private void assertNoTemporaryFiles() throws IOException {
        try (var files = Files.list(root)) { assertTrue(files.noneMatch(path -> path.getFileName().toString().endsWith(".tmp"))); }
    }
    static Commit commit(ModernEquipmentOverrideStore store, TournamentPresence.CharacterKey key,
                         Expected expected, Replacement replacement) throws Exception {
        try (var call = new Call(key)) { return store.compareAndSet(key, expected, replacement, call.scope, call.operation); }
    }
    static Fingerprint fp(int value) { byte[] bytes = new byte[32]; Arrays.fill(bytes, (byte) value); return new Fingerprint(bytes); }
    static ItemOverride item(long id, boolean equipped) { return new ItemOverride(id, Long.MAX_VALUE, fp((int) id), equipped); }
    static Replacement replacement(boolean equipped) { return new Replacement(fp(1), List.of(item(1, equipped))); }
    static ModernBackendScope scope() { return scope(10_000); }
    static ModernBackendScope scope(long millis) {
        return new ModernBackendScope(System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(millis), new ModernBackendScope.AbortScheduler() {
            @Override public boolean execute(Runnable action) { return false; }
            @Override public void remove(Runnable action) {}
        });
    }
    static final class Call implements AutoCloseable {
        final ModernBackendScope scope = scope();
        final AtomicInteger retired = new AtomicInteger();
        final ModernEquipmentLease lease;
        final ModernEquipmentLease.Operation operation;
        Call(TournamentPresence.CharacterKey key) throws Exception {
            lease = new ModernEquipmentLease.Issuer().open(new Object(), key, -1, retired::incrementAndGet);
            operation = lease.operation(scope);
        }
        @Override public void close() { try { operation.close(); lease.invalidate(); } finally { scope.finish(); } }
    }
    private static void assertFailure(Failure failure, org.junit.jupiter.api.function.Executable action) {
        assertEquals(failure, assertThrows(StoreException.class, action).failure());
    }
    private static void assertCorrupt(byte[] bytes, TournamentPresence.CharacterKey key) {
        assertFailure(Failure.CORRUPT, () -> decode(bytes, key));
    }
    private static void rehash(byte[] bytes) throws Exception {
        byte[] digest = MessageDigest.getInstance("SHA-256").digest(Arrays.copyOf(bytes, bytes.length - 32));
        System.arraycopy(digest, 0, bytes, bytes.length - 32, 32);
    }
    private static void await(CountDownLatch latch) throws InterruptedException {
        if (!latch.await(4, TimeUnit.SECONDS)) throw new AssertionError("equipment store barrier timeout");
    }
}
