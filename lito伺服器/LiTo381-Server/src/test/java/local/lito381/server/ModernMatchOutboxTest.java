package local.lito381.server;

import static local.lito381.server.ModernMatchOutbox.Failure.*;
import static local.lito381.server.ModernMatchOutboxTestPeer.*;
import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ModernMatchOutboxTest {
    @TempDir Path root;

    @Test void initialEmptySnapshotIsForcedAndRetainsBindingAfterEveryAcknowledgement() throws Exception {
        var forces = new java.util.concurrent.atomic.AtomicInteger();
        var operations = new ModernMatchOutbox.FileOps() {
            @Override public void force(FileChannel file) throws IOException { forces.incrementAndGet(); ModernMatchOutbox.FileOps.super.force(file); }
        };
        try (var box = new ModernMatchOutbox(root, binding(), operations)) {
            assertTrue(box.isHealthy()); assertFalse(box.isQuiescent()); assertEquals(0, box.size());
            assertEquals(1, forces.get()); assertEquals(1, revision());
            assertEquals(84, Files.size(snapshot())); assertEquals(List.of(), box.pending(deadline()));
            box.record(event(1), deadline()); box.acknowledge(event(1), deadline());
            assertEquals(3, revision()); assertEquals(0, box.size()); assertEquals(3, forces.get());
        }
        byte[] other = binding(); other[0] ^= 1;
        failure(BINDING_MISMATCH, () -> new ModernMatchOutbox(root, other));
        try (var reopened = new ModernMatchOutbox(root, binding())) { assertTrue(reopened.pending(deadline()).isEmpty()); }
    }

    @Test void exactRecordAndAbsentAcknowledgementAreIdempotentWithoutNewRevisionOrWrite() throws Exception {
        try (var box = new ModernMatchOutbox(root, binding())) {
            box.record(event(1), deadline()); byte[] committed = Files.readAllBytes(snapshot());
            assertEquals(108, ModernMatchSettlementJdbc.canonical(event(1)).length); // 64 + 2 * (8+4+8+1+1)
            assertEquals(84 + 4 + 108, committed.length);
            box.record(event(1), deadline()); box.acknowledge(event(2), deadline());
            assertArrayEquals(committed, Files.readAllBytes(snapshot())); assertEquals(1, box.size());
            assertEquals(List.of(event(1)), box.pending(deadline()));
            assertThrows(UnsupportedOperationException.class, () -> box.pending(deadline()).clear());
            box.acknowledge(event(1), deadline()); committed = Files.readAllBytes(snapshot());
            box.acknowledge(event(1), deadline()); assertArrayEquals(committed, Files.readAllBytes(snapshot()));
        }
    }

    @Test void sameUuidDifferentWholePayloadCannotRecordOrAcknowledge() throws Exception {
        try (var box = new ModernMatchOutbox(root, binding())) {
            var old = event(1); box.record(old, deadline()); byte[] committed = Files.readAllBytes(snapshot());
            var changed = new ModernMatchSettlement.Event(old.resultId(), old.matchId(), old.generation(), old.engineRevision(), old.mode(), old.mapId(), old.finishedAtMillis() + 1, old.participants());
            failure(CONFLICT, () -> box.record(changed, deadline())); failure(CONFLICT, () -> box.acknowledge(changed, deadline()));
            assertTrue(box.isHealthy()); assertArrayEquals(committed, Files.readAllBytes(snapshot()));
            assertEquals(List.of(old), box.pending(deadline()));
        }
    }

    @Test void immutableBindingCopyAndUnsignedUuidOrderingPreserveExactI64EventFields() throws Exception {
        byte[] input = binding();
        var low = event(1); var middle = withUuid(low, new UUID(0, Long.MIN_VALUE)); var high = withUuid(low, new UUID(Long.MIN_VALUE, 0));
        try (var box = new ModernMatchOutbox(root, input)) {
            Arrays.fill(input, (byte) 99); box.record(high, deadline()); box.record(middle, deadline()); box.record(low, deadline());
            assertEquals(List.of(low, middle, high), box.pending(deadline()));
            assertEquals(Long.MAX_VALUE, box.pending(deadline()).get(0).generation());
            assertEquals(0x20_0000_0000_0001L, box.pending(deadline()).get(0).finishedAtMillis());
        }
        try (var box = new ModernMatchOutbox(root, binding())) { assertEquals(List.of(low, middle, high), box.pending(deadline())); }
    }

    @Test void maximumCountIsReadableButAnotherRecordCannotEvictOrDropAnExistingResult() throws Exception {
        var events = new ArrayList<ModernMatchSettlement.Event>(); for (int i = 1; i <= ModernMatchOutbox.MAX_PENDING; i++) events.add(event(i));
        Files.write(snapshot(), raw(events, 17, binding()));
        try (var box = new ModernMatchOutbox(root, binding())) {
            assertEquals(1_000, box.size()); assertEquals(events, box.pending(deadline())); byte[] before = Files.readAllBytes(snapshot());
            failure(CAPACITY, () -> box.record(event(1_001), deadline())); assertArrayEquals(before, Files.readAllBytes(snapshot()));
            box.record(event(1), deadline()); assertArrayEquals(before, Files.readAllBytes(snapshot()));
            box.acknowledge(event(1), deadline()); box.record(event(1_001), deadline()); assertEquals(1_000, box.size());
            assertEquals(event(1_001), box.pending(deadline()).get(999));
        }
    }

    @Test void maximumParticipantsHaveExactCoverageAndRemainWellBelowSnapshotByteBudget() throws Exception {
        var members = new ArrayList<ModernMatchSettlement.Participant>();
        for (int i = 1; i <= 100; i++) members.add(new ModernMatchSettlement.Participant(i, new TournamentPresence.CharacterKey(i, Long.MAX_VALUE), i % 2,
                i % 2 == 0 ? ModernMatchSettlement.Verdict.WIN : ModernMatchSettlement.Verdict.LOSS));
        var many = new ModernMatchSettlement.Event(new UUID(1, 1), 1, 1, Long.MAX_VALUE, 1, 514, Long.MAX_VALUE, members);
        try (var box = new ModernMatchOutbox(root, binding())) {
            box.record(many, deadline()); assertEquals(List.of(many), box.pending(deadline()));
            assertEquals(84 + 4 + 64 + 100 * 22, Files.size(snapshot()));
            assertTrue(Files.size(snapshot()) < ModernMatchOutbox.MAX_BYTES);
        }
    }

    @Test void revisionExhaustionDoesNotWrapDeleteOrChangeTheFile() throws Exception {
        Files.write(snapshot(), raw(List.of(event(1)), Long.MAX_VALUE, binding()));
        try (var box = new ModernMatchOutbox(root, binding())) {
            byte[] before = Files.readAllBytes(snapshot());
            failure(REVISION_EXHAUSTED, () -> box.record(event(2), deadline()));
            failure(REVISION_EXHAUSTED, () -> box.acknowledge(event(1), deadline()));
            box.record(event(1), deadline()); box.acknowledge(event(2), deadline());
            assertArrayEquals(before, Files.readAllBytes(snapshot())); assertTrue(box.isHealthy());
        }
    }

    @Test void duplicateUnsortedMalformedAndTrailingEventsAreRejectedBeforeReplay() throws Exception {
        byte[] valid = ModernMatchSettlementJdbc.canonical(event(1));
        var candidates = new ArrayList<byte[]>();
        candidates.add(raw(List.of(event(1), event(1)), 1, binding()));
        candidates.add(raw(List.of(event(2), event(1)), 1, binding()));
        byte[] version = valid.clone(); ByteBuffer.wrap(version).putInt(2); candidates.add(rawPayloads(List.of(version), 1, binding()));
        byte[] emptyId = valid.clone(); Arrays.fill(emptyId, 4, 20, (byte) 0); candidates.add(rawPayloads(List.of(emptyId), 1, binding()));
        byte[] gap = valid.clone(); ByteBuffer.wrap(gap).putLong(64, 2); candidates.add(rawPayloads(List.of(gap), 1, binding()));
        byte[] badVerdict = valid.clone(); badVerdict[64 + 21] = 2; candidates.add(rawPayloads(List.of(badVerdict), 1, binding()));
        byte[] badCount = valid.clone(); ByteBuffer.wrap(badCount).putInt(60, 101); candidates.add(rawPayloads(List.of(badCount), 1, binding()));
        candidates.add(rawPayloads(List.of(Arrays.copyOf(valid, valid.length + 1)), 1, binding()));
        candidates.add(rawPayloads(List.of(Arrays.copyOf(valid, valid.length - 1)), 1, binding()));
        byte[] flags = raw(List.of(event(1)), 1, binding()); flags[7] = 1; candidates.add(resign(flags));
        byte[] negativeCount = raw(List.of(event(1)), 1, binding()); ByteBuffer.wrap(negativeCount).putInt(48, -1); candidates.add(resign(negativeCount));
        byte[] largeCount = raw(List.of(event(1)), 1, binding()); ByteBuffer.wrap(largeCount).putInt(48, 1_001); candidates.add(resign(largeCount));
        byte[] largeLength = raw(List.of(event(1)), 1, binding()); ByteBuffer.wrap(largeLength).putInt(52, 4_097); candidates.add(resign(largeLength));
        for (byte[] bytes : candidates) {
            Files.write(snapshot(), bytes); failure(CORRUPT, () -> new ModernMatchOutbox(root, binding()));
            assertArrayEquals(bytes, Files.readAllBytes(snapshot()));
        }
    }

    @Test void badDigestTruncationAndOversizedFileAreNotSilentlyReinitialized() throws Exception {
        byte[] intact = raw(List.of(event(1)), 1, binding()); byte[] badHash = intact.clone(); badHash[70] ^= 1;
        for (byte[] broken : List.of(new byte[0], Arrays.copyOf(intact, 50), badHash, new byte[ModernMatchOutbox.MAX_BYTES + 1])) {
            Files.write(snapshot(), broken); failure(CORRUPT, () -> new ModernMatchOutbox(root, binding()));
            assertArrayEquals(broken, Files.readAllBytes(snapshot()));
        }
    }

    @Test void uncoordinatedLiveReplacementPoisonsUntilReopenAndNeverUpdatesCachedAdmissionCount() throws Exception {
        try (var box = new ModernMatchOutbox(root, binding())) {
            box.record(event(1), deadline()); Files.write(snapshot(), raw(List.of(event(1), event(2)), 3, binding()));
            failure(CORRUPT, () -> box.pending(deadline())); assertFalse(box.isHealthy()); assertEquals(1, box.size());
            failure(COMMIT_UNCERTAIN, () -> box.record(event(3), deadline()));
        }
        try (var reopened = new ModernMatchOutbox(root, binding())) { assertEquals(List.of(event(1), event(2)), reopened.pending(deadline())); }
    }

    @Test void relativeTraversalAdsAndNonDirectoryPathsAreRejectedWithoutNamespaceCreation() throws Exception {
        failure(INVALID_PATH, () -> new ModernMatchOutbox(Path.of("relative-outbox"), binding()));
        failure(INVALID_PATH, () -> new ModernMatchOutbox(root.resolve(".."), binding()));
        try {
            Path ads = root.resolve("box:stream");
            failure(INVALID_PATH, () -> new ModernMatchOutbox(ads, binding()));
        } catch (java.nio.file.InvalidPathException rejectedByProvider) {
            assertEquals("\\", root.getFileSystem().getSeparator()); // Windows rejects the ADS before a Path can reach the store.
        }
        failure(INVALID_PATH, () -> new ModernMatchOutbox(root.resolve("missing-directory"), binding()));
        Path plain = root.resolve("plain"); Files.write(plain, new byte[0]);
        failure(INVALID_PATH, () -> new ModernMatchOutbox(plain, binding()));
        failure(INVALID_PATH, () -> new ModernMatchOutbox(root, new byte[31]));
    }

    @Test void ownedWriterLockExcludesSecondOpenAndErrorMessagesHaveNoPrivatePathOrBinding() throws Exception {
        try (var box = new ModernMatchOutbox(root, binding())) {
            var failure = failure(BUSY, () -> new ModernMatchOutbox(root, binding()));
            assertFalse(failure.getMessage().contains(root.toString())); assertTrue(box.isHealthy());
            assertEquals("BUSY", peer("busy"));
        }
        try (var next = new ModernMatchOutbox(root, binding())) { assertTrue(next.isHealthy()); }
    }

    @Test void failedForceLeavesTheOldSnapshotAndNeverDeletesUnknownOrAbandonedFiles() throws Exception {
        initialize(); byte[] original = Files.readAllBytes(snapshot());
        Path unknown = root.resolve("operator-notes.txt"), orphan = root.resolve(".modern-match-outbox-old.tmp");
        Files.writeString(unknown, "not owned by this operation"); Files.write(orphan, new byte[]{1, 2, 3});
        var operations = new ModernMatchOutbox.FileOps() {
            @Override public void force(FileChannel file) throws IOException { ModernMatchOutbox.FileOps.super.force(file); throw new IOException("fixture private path"); }
        };
        try (var box = new ModernMatchOutbox(root, binding(), operations)) {
            failure(IO, () -> box.record(event(1), deadline())); assertArrayEquals(original, Files.readAllBytes(snapshot()));
            assertTrue(box.pending(deadline()).isEmpty()); assertTrue(box.isHealthy());
        }
        assertEquals("not owned by this operation", Files.readString(unknown)); assertArrayEquals(new byte[]{1, 2, 3}, Files.readAllBytes(orphan));
        try (var paths = Files.list(root)) { assertEquals(1, paths.filter(p -> p.getFileName().toString().endsWith(".tmp")).count()); }
    }

    @Test void bothBeforeAndAfterMoveExceptionsAreUncertainAndRequireReadingTheUniqueSnapshotOnReopen() throws Exception {
        for (boolean moved : List.of(false, true)) {
            Path directory = Files.createDirectory(root.resolve(moved ? "moved" : "not-moved"));
            try (var seed = new ModernMatchOutbox(directory, binding())) { assertEquals(0, seed.size()); }
            var operations = new ModernMatchOutbox.FileOps() {
                @Override public void move(Path from, Path to) throws IOException {
                    if (moved) ModernMatchOutbox.FileOps.super.move(from, to);
                    throw new IOException("fixture move acknowledgement lost");
                }
            };
            try (var box = new ModernMatchOutbox(directory, binding(), operations)) {
                failure(COMMIT_UNCERTAIN, () -> box.record(event(1), deadline())); assertFalse(box.isHealthy());
                failure(COMMIT_UNCERTAIN, () -> box.pending(deadline()));
            }
            try (var reopened = new ModernMatchOutbox(directory, binding())) {
                assertEquals(moved ? List.of(event(1)) : List.of(), reopened.pending(deadline()));
                reopened.record(event(1), deadline()); assertEquals(List.of(event(1)), reopened.pending(deadline()));
            }
        }
    }

    @Test void atomicMoveUnsupportedHasNoWeakerFallbackAndAcknowledgementUncertaintyRetainsTheTruth() throws Exception {
        initialize(); byte[] empty = Files.readAllBytes(snapshot());
        try (var box = new ModernMatchOutbox(root, binding(), new ModernMatchOutbox.FileOps() {
            @Override public void move(Path from, Path to) throws IOException { throw new AtomicMoveNotSupportedException("redacted", "redacted", "fixture"); }
        })) { failure(COMMIT_UNCERTAIN, () -> box.record(event(1), deadline())); assertFalse(box.isHealthy()); }
        assertArrayEquals(empty, Files.readAllBytes(snapshot()));
        try (var box = new ModernMatchOutbox(root, binding())) { box.record(event(1), deadline()); }
        try (var box = new ModernMatchOutbox(root, binding(), new ModernMatchOutbox.FileOps() {
            @Override public void move(Path from, Path to) throws IOException { ModernMatchOutbox.FileOps.super.move(from, to); throw new IOException("lost acknowledgement"); }
        })) { failure(COMMIT_UNCERTAIN, () -> box.acknowledge(event(1), deadline())); }
        try (var reopened = new ModernMatchOutbox(root, binding())) { assertTrue(reopened.pending(deadline()).isEmpty()); reopened.acknowledge(event(1), deadline()); }
    }

    @Test void deadlinesAreAbsoluteAndInterruptAfterForcePreventsPublicationWithoutClearingTheInterrupt() throws Exception {
        initialize(); byte[] original = Files.readAllBytes(snapshot());
        try (var box = new ModernMatchOutbox(root, binding(), new ModernMatchOutbox.FileOps() {
            @Override public void force(FileChannel file) throws IOException { ModernMatchOutbox.FileOps.super.force(file); Thread.currentThread().interrupt(); }
        })) {
            failure(DEADLINE, () -> box.record(event(1), System.nanoTime() - 1));
            failure(DEADLINE, () -> box.pending(System.nanoTime() + TimeUnit.SECONDS.toNanos(61)));
            try { failure(DEADLINE, () -> box.record(event(1), deadline())); assertTrue(Thread.currentThread().isInterrupted()); }
            finally { Thread.interrupted(); }
            assertArrayEquals(original, Files.readAllBytes(snapshot())); assertTrue(box.pending(deadline()).isEmpty());
        }
    }

    @Test void closeDoesNotDetachAnAdmittedForceAndItsRealFinallyRetiresTheLock() throws Exception {
        initialize(); var entered = new CountDownLatch(1); var release = new CountDownLatch(1); var workers = Executors.newSingleThreadExecutor();
        var operations = new ModernMatchOutbox.FileOps() {
            @Override public void force(FileChannel file) throws IOException {
                entered.countDown();
                try { if (!release.await(3, TimeUnit.SECONDS)) throw new IOException("fixture force barrier timed out"); }
                catch (InterruptedException interrupted) { Thread.currentThread().interrupt(); throw new IOException("fixture force interrupted"); }
                ModernMatchOutbox.FileOps.super.force(file);
            }
        };
        try (var box = new ModernMatchOutbox(root, binding(), operations)) {
            try {
                var recorded = workers.submit(() -> { box.record(event(1), deadline()); return true; });
                assertTrue(entered.await(1, TimeUnit.SECONDS)); box.close();
                assertFalse(box.isHealthy()); assertFalse(box.isQuiescent());
                failure(CLOSED, () -> box.pending(deadline())); failure(BUSY, () -> new ModernMatchOutbox(root, binding()));
                release.countDown(); assertTrue(recorded.get(2, TimeUnit.SECONDS)); assertTrue(box.isQuiescent());
            } finally { release.countDown(); workers.shutdownNow(); assertTrue(workers.awaitTermination(3, TimeUnit.SECONDS)); }
        }
        try (var reopened = new ModernMatchOutbox(root, binding())) { assertEquals(List.of(event(1)), reopened.pending(deadline())); }
    }

    @Test void processKillAfterTempForceDoesNotPromoteTheUncommittedTempAndReleasesTheOsLock() throws Exception {
        initialize(); byte[] before = Files.readAllBytes(snapshot()); killPeerAt("hold-force", "FORCED");
        assertArrayEquals(before, Files.readAllBytes(snapshot()));
        try (var reopened = new ModernMatchOutbox(root, binding())) { assertTrue(reopened.pending(deadline()).isEmpty()); }
        try (var paths = Files.list(root)) { assertTrue(paths.anyMatch(p -> p.getFileName().toString().endsWith(".tmp"))); }
        assertEquals("EMPTY", peer("read"));
    }

    @Test void processKillAfterAtomicMoveOrConfirmedRecordReopensTheExactSameUuid() throws Exception {
        for (boolean confirmed : List.of(false, true)) {
            Path directory = Files.createDirectory(root.resolve(confirmed ? "confirmed" : "move-unknown"));
            try (var empty = new ModernMatchOutbox(directory, binding())) { assertEquals(0, empty.size()); }
            killPeerAt(directory, confirmed ? "hold-record" : "hold-move", confirmed ? "RECORDED" : "MOVED");
            try (var reopened = new ModernMatchOutbox(directory, binding())) {
                assertEquals(List.of(event(1)), reopened.pending(deadline()));
                byte[] before = Files.readAllBytes(directory.resolve("outbox.mso")); reopened.record(event(1), deadline());
                assertArrayEquals(before, Files.readAllBytes(directory.resolve("outbox.mso")));
            }
        }
    }

    @Test void processKillAfterAcknowledgementMoveLeavesAnEmptyBindingSnapshotNotAResurrectedEvent() throws Exception {
        try (var box = new ModernMatchOutbox(root, binding())) { box.record(event(1), deadline()); }
        // The test supplies the acknowledge precondition; this is NOT a real SQL-commit/crash test.
        killPeerAt("hold-ack", "ACK_MOVED");
        try (var box = new ModernMatchOutbox(root, binding())) { assertTrue(box.pending(deadline()).isEmpty()); box.acknowledge(event(1), deadline()); }
        byte[] other = binding(); other[1] ^= 1; failure(BINDING_MISMATCH, () -> new ModernMatchOutbox(root, other));
    }
    @Test void cooperativeSoloProofSurvivesExactOwnedProcessCrashBoundaries()throws Exception {
        for(String phase:List.of("force","move","record","ack")) {
            Path directory=Files.createDirectory(root.resolve("timeattack-"+phase));var value=cooperativeEvent(1);
            try(var initial=new ModernMatchOutbox(directory,binding())){if(phase.equals("ack"))initial.record(value,deadline());}
            String marker=switch(phase){case "force"->"FORCED";case "move"->"MOVED";case "record"->"RECORDED";default->"ACK_MOVED";};
            killPeerAt(directory,"hold-"+phase+"-timeattack",marker);
            try(var reopened=new ModernMatchOutbox(directory,binding())) {
                boolean committed=phase.equals("move")||phase.equals("record");
                assertEquals(committed?List.of(value):List.of(),reopened.pending(deadline()));
                if(committed){byte[] before=Files.readAllBytes(directory.resolve("outbox.mso"));reopened.record(value,deadline());assertArrayEquals(before,Files.readAllBytes(directory.resolve("outbox.mso")));}
            }
        }
    }

    @Test void crashRecoveryProbeCannotCreateMissingLockOrBypassAnExistingWriter() throws Exception {
        assertThrows(java.nio.file.NoSuchFileException.class, () -> writerLockReleased(root));
        assertFalse(Files.exists(root.resolve(".modern-match-outbox.lock")));
        initialize(); byte[] before = Files.readAllBytes(snapshot());
        byte[] lockBefore = Files.readAllBytes(root.resolve(".modern-match-outbox.lock"));
        try (var owner = new ModernMatchOutbox(root, binding())) {
            assertThrows(java.nio.channels.OverlappingFileLockException.class, () -> writerLockReleased(root));
            failure(BUSY, () -> new ModernMatchOutbox(root, binding()));
        }
        assertTrue(writerLockReleased(root));
        assertArrayEquals(before, Files.readAllBytes(snapshot()));
        assertArrayEquals(lockBefore, Files.readAllBytes(root.resolve(".modern-match-outbox.lock")));
    }
    private void initialize() throws Exception { try (var box = new ModernMatchOutbox(root, binding())) { assertTrue(box.isHealthy()); } }
    private Path snapshot() { return root.resolve("outbox.mso"); }
    private long revision() throws IOException { return ByteBuffer.wrap(Files.readAllBytes(snapshot())).getLong(40); }
    private static ModernMatchSettlement.Event withUuid(ModernMatchSettlement.Event e, UUID id) { return new ModernMatchSettlement.Event(id, e.matchId(), e.generation(), e.engineRevision(), e.mode(), e.mapId(), e.finishedAtMillis(), e.participants()); }
    private static ModernMatchOutbox.OutboxException failure(ModernMatchOutbox.Failure expected, org.junit.jupiter.api.function.Executable action) {
        var failure = assertThrows(ModernMatchOutbox.OutboxException.class, action); assertEquals(expected, failure.failure()); return failure;
    }
    private static byte[] raw(List<ModernMatchSettlement.Event> events, long revision, byte[] binding) throws Exception {
        var values = new ArrayList<byte[]>(); for (var event : events) values.add(ModernMatchSettlementJdbc.canonical(event));
        return rawPayloads(values, revision, binding);
    }
    private static byte[] rawPayloads(List<byte[]> events, long revision, byte[] binding) throws Exception {
        var bytes = new ByteArrayOutputStream();
        try (var out = new DataOutputStream(bytes)) {
            out.writeInt(0x4d534f31); out.writeShort(1); out.writeShort(0); out.write(binding); out.writeLong(revision); out.writeInt(events.size());
            for (var event : events) { out.writeInt(event.length); out.write(event); }
            out.write(new byte[32]);
        }
        return resign(bytes.toByteArray());
    }
    private static byte[] resign(byte[] value) throws Exception {
        var digest = MessageDigest.getInstance("SHA-256"); digest.update(value, 0, value.length - 32);
        System.arraycopy(digest.digest(), 0, value, value.length - 32, 32); return value;
    }
    private String peer(String mode) throws Exception {
        var process = process(root, mode); var reader = Executors.newSingleThreadExecutor();
        try {
            var output = reader.submit(() -> marker(process));
            assertTrue(process.waitFor(5, TimeUnit.SECONDS)); assertEquals(0, process.exitValue()); return output.get(1, TimeUnit.SECONDS);
        } finally { stop(process, reader); }
    }
    private void killPeerAt(String mode, String expected) throws Exception { killPeerAt(root, mode, expected); }
    private static void killPeerAt(Path directory, String mode, String expected) throws Exception {
        var process = process(directory, mode); var reader = Executors.newSingleThreadExecutor();
        try {
            var output = reader.submit(() -> marker(process));
            assertEquals(expected, output.get(3, TimeUnit.SECONDS)); assertTrue(process.isAlive());
            process.destroyForcibly(); assertTrue(process.waitFor(3, TimeUnit.SECONDS));
        } finally { stop(process, reader); }
        assertFalse(process.isAlive(), "only the exact retired fixture can enter recovery");
        // A terminal process handle alone did not prove writer availability in
        // the observed BUSY failure. Independently acquire/release the existing
        // OS lock before recovery; never sleep blindly or weaken production BUSY.
        long end = System.nanoTime() + TimeUnit.SECONDS.toNanos(3);
        while (!writerLockReleased(directory)) {
            assertTrue(System.nanoTime() < end, "owned peer retired but OS writer lock remains busy");
            Thread.sleep(5);
        }
    }
    private static boolean writerLockReleased(Path directory) throws IOException {
        // No CREATE, truncation, outbox parsing, promotion, binding change or DDL.
        try (var file = FileChannel.open(directory.resolve(".modern-match-outbox.lock"),
                java.nio.file.StandardOpenOption.READ, java.nio.file.StandardOpenOption.WRITE, java.nio.file.LinkOption.NOFOLLOW_LINKS);
             var lock = file.tryLock()) {
            return lock != null;
        }
    }
    private static Process process(Path directory, String mode) throws IOException {
        LocalResources.assumeWindowsJvm();
        String executable = Path.of(System.getProperty("java.home"), "bin", "java.exe").toString();
        String classpath = System.getProperty("surefire.test.class.path", System.getProperty("java.class.path"));
        return new ProcessBuilder(executable, "-cp", classpath, ModernMatchOutboxTestPeer.class.getName(), mode, directory.toString()).redirectErrorStream(true).start();
    }
    private static String marker(Process process) throws IOException {
        var bytes = new ByteArrayOutputStream(); var stream = process.getInputStream();
        for (int index = 0; index < 128; index++) {
            int value = stream.read(); if (value == '\n') return bytes.toString(java.nio.charset.StandardCharsets.US_ASCII).stripTrailing();
            if (value < 0 || value > 127) throw new IOException("invalid owned peer marker"); bytes.write(value);
        }
        throw new IOException("owned peer marker exceeds bound");
    }
    private static void stop(Process process, java.util.concurrent.ExecutorService reader) throws Exception {
        try {
            if (process.isAlive()) { process.destroyForcibly(); assertTrue(process.waitFor(3, TimeUnit.SECONDS)); }
            process.getInputStream().close(); process.getOutputStream().close();
        } finally { reader.shutdownNow(); assertTrue(reader.awaitTermination(3, TimeUnit.SECONDS)); }
    }
}
