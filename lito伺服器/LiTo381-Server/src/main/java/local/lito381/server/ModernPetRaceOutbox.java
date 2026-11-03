package local.lito381.server;

import com.sun.nio.file.ExtendedOpenOption;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.NoSuchFileException;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;

/**
 * One bounded force-confirmed local snapshot of unacknowledged match events.
 * No SQL, configuration loading, Java serialization, or native state adapter.
 * The caller supplies an immutable routing binding and acknowledges only after
 * an exact idempotent SQL commit has been confirmed. Local recording is NOT
 * proof of a SQL commit or permission to claim that an uncertain commit failed.
 *
 * <p>The administrator-owned local directory uses a lifetime cooperating-writer
 * lock. Windows handles deny delete sharing (and data handles deny writers).
 * Path checks do not defend against a privileged ancestor replacement. Sibling
 * force + atomic rename is tested for process death, not Windows power-loss
 * directory-entry durability. FileChannel force/close themselves have no Java
 * hard deadline; admitted IO remains owned until it really returns.</p>
 */
final class ModernPetRaceOutbox implements AutoCloseable {
    static final int MAX_PENDING = 1_000;
    static final int MAX_BYTES = 4 * 1024 * 1024;
    private static final int MAGIC = 0x52504f31; // RPO1, independent race namespace
    private static final int HEADER = 52, HASH = 32;
    static final int MAX_EVENT_BYTES = 4096;
    private static final long MAX_DEADLINE_NANOS = TimeUnit.SECONDS.toNanos(60);
    private static final Comparator<ModernPetRaceSettlement.Event> ORDER = (a, b) -> compare(a.resultId(), b.resultId());

    enum Failure { INVALID_PATH, BUSY, CLOSED, CORRUPT, BINDING_MISMATCH, CONFLICT,
        CAPACITY, REVISION_EXHAUSTED, DEADLINE, IO, COMMIT_UNCERTAIN }
    static final class OutboxException extends IOException {
        private final Failure failure;
        OutboxException(Failure failure) { super("modern pet race outbox: " + failure); this.failure = failure; }
        Failure failure() { return failure; }
    }
    interface FileOps {
        default void force(FileChannel file) throws IOException { file.force(true); }
        default void move(Path from, Path to) throws IOException {
            Files.move(from, to, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        }
    }
    private record Snapshot(long revision, List<ModernPetRaceSettlement.Event> events) {
        Snapshot { if (revision < 1) throw new IllegalArgumentException("invalid outbox revision"); events = List.copyOf(events); }
    }

    private final Path directory, destination, lockPath;
    private final byte[] binding;
    private final boolean windowsSharing;
    private final FileOps files;
    private final FileChannel lockFile;
    private final FileLock writerLock;
    private final Object lockIdentity;
    private final ReentrantLock io = new ReentrantLock();
    private final AtomicBoolean closing = new AtomicBoolean();
    private volatile boolean poisoned;
    private volatile Snapshot known;

    ModernPetRaceOutbox(Path directory, byte[] binding) throws IOException { this(directory, binding, new FileOps() {}); }
    ModernPetRaceOutbox(Path directory, byte[] binding, FileOps files) throws IOException {
        if (directory == null || !directory.isAbsolute() || directory.getFileName() == null
                || binding == null || binding.length != HASH) throw error(Failure.INVALID_PATH);
        this.directory = directory; this.binding = binding.clone(); this.files = Objects.requireNonNull(files);
        destination = directory.resolve("race-outbox.rpo"); lockPath = directory.resolve(".modern-pet-race-outbox.lock");
        windowsSharing = "\\".equals(directory.getFileSystem().getSeparator());
        FileChannel opened = null; FileLock acquired = null;
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(30);
        try {
            checked(lockPath, false); checkDeadline(deadline);
            opened = FileChannel.open(lockPath, options(false, StandardOpenOption.CREATE, StandardOpenOption.READ, StandardOpenOption.WRITE));
            acquired = opened.tryLock(); if (acquired == null) throw error(Failure.BUSY);
            checked(lockPath, true); lockIdentity = attributes(lockPath).fileKey();
            if (lockIdentity == null && !windowsSharing) throw error(Failure.INVALID_PATH);
            lockFile = opened; writerLock = acquired;
            var existing = readSnapshot(deadline, true);
            if (existing == null) replace(new Snapshot(1, List.of()), deadline);
            else known = existing;
        } catch (IOException | RuntimeException | Error failure) {
            if (acquired != null) try { acquired.close(); } catch (IOException ignored) {}
            if (opened != null) try { opened.close(); } catch (IOException ignored) {}
            if (failure instanceof Error fatal) throw fatal;
            if (failure instanceof OutboxException exact) throw exact;
            throw error(failure instanceof OverlappingFileLockException ? Failure.BUSY : Failure.IO);
        }
    }

    void record(ModernPetRaceSettlement.Event event, long deadline) throws IOException {
        Objects.requireNonNull(event); enter(deadline);
        try {
            Snapshot current = current(deadline);
            for (var existing : current.events()) if (existing.resultId().equals(event.resultId())) {
                if (!existing.sameAuthority(event)) throw error(Failure.CONFLICT);
                checkDeadline(deadline);
                return; // Same event/UUID is confirmation, not a replacement or new revision.
            }
            if (current.events().size() == MAX_PENDING) throw error(Failure.CAPACITY);
            var events = new ArrayList<>(current.events()); events.add(event); events.sort(ORDER);
            replace(new Snapshot(nextRevision(current), events), deadline);
        } finally { leave(); }
    }

    List<ModernPetRaceSettlement.Event> pending(long deadline) throws IOException {
        enter(deadline);
        try { return current(deadline).events(); }
        finally { leave(); }
    }

    /** SQL success is the caller's obligation; no local absence can prove a rollback. */
    void acknowledge(ModernPetRaceSettlement.Event event, long deadline) throws IOException {
        Objects.requireNonNull(event); enter(deadline);
        try {
            Snapshot current = current(deadline); var events = new ArrayList<>(current.events());
            int found = -1;
            for (int index = 0; index < events.size(); index++) if (events.get(index).resultId().equals(event.resultId())) {
                if (!events.get(index).sameAuthority(event)) throw error(Failure.CONFLICT);
                found = index; break;
            }
            if (found < 0) { checkDeadline(deadline); return; }
            events.remove(found); replace(new Snapshot(nextRevision(current), events), deadline);
        } finally { leave(); }
    }

    /** Last verified count only; callers must check isHealthy before admission. */
    int size() { Snapshot snapshot = known; return snapshot == null ? 0 : snapshot.events().size(); }
    boolean isHealthy() { return !closing.get() && !poisoned && lockFile.isOpen() && writerLock.isValid(); }
    boolean isQuiescent() { return closing.get() && !lockFile.isOpen(); }
    @Override public void close() {
        closing.set(true);
        if (io.isHeldByCurrentThread()) return;
        if (io.tryLock()) try { closeOwner(); } finally { io.unlock(); }
    }

    private void enter(long deadline) throws IOException {
        validateDeadline(deadline);
        if (closing.get()) throw error(Failure.CLOSED);
        boolean locked;
        try { locked = io.tryLock(Math.max(1, deadline - System.nanoTime()), TimeUnit.NANOSECONDS); }
        catch (InterruptedException interrupted) { Thread.currentThread().interrupt(); throw error(Failure.DEADLINE); }
        if (!locked) throw error(Failure.DEADLINE);
        try {
            checkDeadline(deadline);
            if (closing.get()) throw error(Failure.CLOSED);
            if (poisoned) throw error(Failure.COMMIT_UNCERTAIN);
        } catch (IOException | RuntimeException failure) { leave(); throw failure; }
    }
    private void leave() { try { if (closing.get()) closeOwner(); } finally { io.unlock(); } }
    private void closeOwner() {
        try { writerLock.close(); } catch (IOException ignored) { poisoned = true; }
        try { lockFile.close(); } catch (IOException ignored) { poisoned = true; }
    }
    private Snapshot current(long deadline) throws IOException {
        try {
            verifyNamespace(); Snapshot read = readSnapshot(deadline, false);
            if (!read.equals(known)) throw error(Failure.CORRUPT); // No uncoordinated replacement/rollback in this process.
            return read;
        } catch (OutboxException failure) {
            if (failure.failure() != Failure.DEADLINE) poisoned = true;
            throw failure;
        } catch (IOException | RuntimeException failure) { poisoned = true; throw error(Failure.IO); }
    }
    private void replace(Snapshot next, long deadline) throws IOException {
        byte[] bytes = encode(next, binding);
        Path temporary = directory.resolve(".modern-pet-race-outbox-" + UUID.randomUUID() + ".tmp");
        boolean created = false, moveAttempted = false, moved = false;
        try {
            checkDeadline(deadline); verifyNamespace(); checked(temporary, false);
            try (var file = FileChannel.open(temporary, options(true, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE))) {
                created = true; ByteBuffer buffer = ByteBuffer.wrap(bytes);
                while (buffer.hasRemaining()) { checkDeadline(deadline); if (file.write(buffer) <= 0) throw error(Failure.IO); }
                checkDeadline(deadline); files.force(file);
            }
            checkDeadline(deadline); verifyNamespace(); checked(destination, false);
            moveAttempted = true; files.move(temporary, destination); moved = true;
            // No post-commit failing IO or deadline check may turn a known commit into a false rollback claim.
            known = next;
        } catch (IOException | RuntimeException failure) {
            if (moveAttempted) { poisoned = true; throw error(Failure.COMMIT_UNCERTAIN); }
            if (failure instanceof OutboxException exact) throw exact;
            throw error(Failure.IO);
        } finally {
            Arrays.fill(bytes, (byte) 0);
            if (moveAttempted && !moved) poisoned = true; // Also fence a fatal callback Error after a possible move.
            if (created && !moved) {
                try { Files.deleteIfExists(temporary); }
                catch (IOException | RuntimeException failure) { poisoned = true; }
            }
        }
    }
    private Snapshot readSnapshot(long deadline, boolean allowAbsent) throws IOException {
        checked(destination, false); checkDeadline(deadline);
        BasicFileAttributes before;
        try { before = attributes(destination); }
        catch (NoSuchFileException absent) { if (allowAbsent) return null; throw error(Failure.CORRUPT); }
        if (!before.isRegularFile() || before.size() < HEADER + HASH || before.size() > MAX_BYTES) throw error(Failure.CORRUPT);
        ByteBuffer buffer = ByteBuffer.allocate((int) before.size() + 1);
        try (var file = FileChannel.open(destination, options(true, StandardOpenOption.READ))) {
            while (buffer.hasRemaining()) {
                checkDeadline(deadline); int read = file.read(buffer);
                if (read < 0) break; if (read == 0) throw error(Failure.IO);
            }
            checked(destination, true); var after = attributes(destination);
            if (!Objects.equals(before.fileKey(), after.fileKey()) || before.size() != after.size()
                    || !before.lastModifiedTime().equals(after.lastModifiedTime()) || buffer.position() != before.size()) throw error(Failure.CORRUPT);
        }
        checkDeadline(deadline);
        Snapshot result = decode(Arrays.copyOf(buffer.array(), buffer.position()), binding);
        checkDeadline(deadline); return result;
    }
    private void verifyNamespace() throws IOException {
        checked(lockPath, true);
        if (!writerLock.isValid() || !Objects.equals(lockIdentity, attributes(lockPath).fileKey())) throw error(Failure.INVALID_PATH);
    }
    private static long nextRevision(Snapshot snapshot) throws OutboxException {
        try { return Math.addExact(snapshot.revision(), 1); }
        catch (ArithmeticException exhausted) { throw error(Failure.REVISION_EXHAUSTED); }
    }
    private static byte[] encode(Snapshot snapshot, byte[] binding) throws OutboxException {
        if (snapshot.events().size() > MAX_PENDING) throw error(Failure.CAPACITY);
        var payloads = new ArrayList<byte[]>(snapshot.events().size()); int length = HEADER + HASH;
        try {
            UUID previous = null;
            for (var event : snapshot.events()) {
                if (previous != null && compare(previous, event.resultId()) >= 0) throw error(Failure.CORRUPT);
                byte[] payload = encodeEvent(event); payloads.add(payload);
                length = Math.addExact(length, 4 + payload.length); previous = event.resultId();
            }
            if (length > MAX_BYTES) throw error(Failure.CAPACITY);
            ByteBuffer out = ByteBuffer.allocate(length);
            out.putInt(MAGIC).putShort((short) 1).putShort((short) 0).put(binding).putLong(snapshot.revision()).putInt(payloads.size());
            for (byte[] payload : payloads) out.putInt(payload.length).put(payload);
            out.put(hash(out.array(), out.position())); return out.array();
        } catch (OutboxException exact) { throw exact; }
        catch (Exception failure) { throw error(Failure.CORRUPT); }
    }
    private static Snapshot decode(byte[] bytes, byte[] expectedBinding) throws OutboxException {
        if (bytes.length < HEADER + HASH || bytes.length > MAX_BYTES
                || !MessageDigest.isEqual(hash(bytes, bytes.length - HASH), Arrays.copyOfRange(bytes, bytes.length - HASH, bytes.length))) throw error(Failure.CORRUPT);
        try {
            ByteBuffer in = ByteBuffer.wrap(bytes, 0, bytes.length - HASH);
            if (in.getInt() != MAGIC || in.getShort() != 1 || in.getShort() != 0) throw error(Failure.CORRUPT);
            byte[] actualBinding = new byte[HASH]; in.get(actualBinding);
            if (!MessageDigest.isEqual(actualBinding, expectedBinding)) throw error(Failure.BINDING_MISMATCH);
            long revision = in.getLong(); int count = in.getInt();
            if (revision < 1 || count < 0 || count > MAX_PENDING || count > in.remaining() / (4 + 1)) throw error(Failure.CORRUPT);
            var events = new ArrayList<ModernPetRaceSettlement.Event>(count); UUID previous = null;
            for (int index = 0; index < count; index++) {
                int length = in.getInt();
                if (length < 1 || length > MAX_EVENT_BYTES || length > in.remaining()) throw error(Failure.CORRUPT);
                byte[] payload = new byte[length]; in.get(payload); var event = decodeEvent(payload);
                if (previous != null && compare(previous, event.resultId()) >= 0) throw error(Failure.CORRUPT);
                events.add(event); previous = event.resultId();
            }
            if (in.hasRemaining()) throw error(Failure.CORRUPT);
            return new Snapshot(revision, events);
        } catch (OutboxException exact) { throw exact; }
        catch (RuntimeException malformed) { throw error(Failure.CORRUPT); }
    }
    /** Complete replay envelope, distinct from the result's authoritative wire body. */
    static byte[] encodeEvent(ModernPetRaceSettlement.Event event) throws OutboxException {
        try {
            var out = ByteBuffer.allocate(MAX_EVENT_BYTES);
            out.putInt(1).putLong(event.resultId().getMostSignificantBits()).putLong(event.resultId().getLeastSignificantBits());
            out.putLong(event.matchId()).putLong(event.generation()).putLong(event.terminalRevision()).putInt(event.logicalMapId());
            var manifest = event.manifest();
            if (manifest.sectors().isEmpty() || manifest.sectors().size() > ModernWorldMap.MAX_SECTORS) throw error(Failure.CORRUPT);
            out.putInt(manifest.mapId()).put(java.util.HexFormat.of().parseHex(manifest.digest())).putInt(manifest.sectors().size());
            for (var sector : manifest.sectors()) {
                out.putInt(sector.key().x()).putInt(sector.key().y()).putInt(sector.byteLength()).put(java.util.HexFormat.of().parseHex(sector.rawSha256()));
            }
            out.putLong(event.stepIntervalNanos()).putInt(event.gates().size());
            for (var gate : event.gates()) out.putInt(gate.minX()).putInt(gate.maxX()).putInt(gate.minY()).putInt(gate.maxY());
            out.put(java.util.HexFormat.of().parseHex(event.courseSha256())).putLong(event.firstFinisherEntity())
                    .putLong(event.finishWindowDeadlineElapsedNanos()).putInt(event.rows().size());
            for (var row : event.rows()) {
                var r = row.runner();
                out.putInt(row.source().sourceId()).putLong(row.source().objectId()).putLong(r.entityId()).putLong(r.revision())
                        .putLong(r.progressMoveSequence()).putLong(r.completedLaps()).putLong(r.nextCheckpoint())
                        .putLong(r.placement()).putLong(r.finishElapsedNanos()).putLong(r.dnf().ordinal()).putLong(r.baseGraphicsId());
            }
            out.put(java.util.HexFormat.of().parseHex(event.contentSha256())).putLong(event.recordedAtMillis());
            byte[] bytes = Arrays.copyOf(out.array(), out.position());
            // The same strict decoder bounds manifest metadata even for direct Store callers.
            decodeEvent(bytes);
            return bytes;
        } catch (OutboxException exact) { throw exact; }
        catch (RuntimeException malformed) { throw error(Failure.CORRUPT); }
    }
    static ModernPetRaceSettlement.Event decodeEvent(byte[] bytes) throws OutboxException {
        try {
            if (bytes == null || bytes.length < 1 || bytes.length > MAX_EVENT_BYTES) throw error(Failure.CORRUPT);
            var in = ByteBuffer.wrap(bytes);
            if (in.getInt() != 1) throw error(Failure.CORRUPT);
            var id = new UUID(in.getLong(), in.getLong());
            long match = in.getLong(), generation = in.getLong(), revision = in.getLong();
            int logical = in.getInt(), physical = in.getInt();
            String digest = readHash(in);
            int sectors = in.getInt();
            if (physical < 0 || sectors < 1 || sectors > ModernWorldMap.MAX_SECTORS || in.remaining() < sectors * 44) throw error(Failure.CORRUPT);
            var sources = new ArrayList<ModernWorldMap.SectorManifest>(sectors);
            ModernWorldMap.SectorKey previous = null; long total = 0;
            for (int i = 0; i < sectors; i++) {
                var key = new ModernWorldMap.SectorKey(in.getInt(), in.getInt()); int length = in.getInt();
                if (length < 1 || length > ModernWorldMap.MAX_SECTOR_BYTES
                        || previous != null && (key.x() < previous.x() || key.x() == previous.x() && key.y() <= previous.y())) throw error(Failure.CORRUPT);
                total += length; if (total > ModernWorldMap.MAX_TOTAL_BYTES) throw error(Failure.CORRUPT);
                sources.add(new ModernWorldMap.SectorManifest(key, length, readHash(in))); previous = key;
            }
            var manifest = new ModernWorldMap.Manifest(physical, sources, digest);
            long interval = in.getLong(); int count = in.getInt();
            if (count < 2 || count > 64 || in.remaining() < count * 16) throw error(Failure.CORRUPT);
            var gates = new ArrayList<ModernPetRace.Gate>(count);
            for (int i = 0; i < count; i++) gates.add(new ModernPetRace.Gate(in.getInt(), in.getInt(), in.getInt(), in.getInt()));
            String course = readHash(in); long first = in.getLong(), deadline = in.getLong(); count = in.getInt();
            if (count < 2 || count > 10 || in.remaining() != count * 84 + 40) throw error(Failure.CORRUPT);
            var rows = new ArrayList<ModernPetRaceProtocol.ResultRow>(count);
            for (int i = 0; i < count; i++) {
                var key = new TournamentPresence.CharacterKey(in.getInt(), in.getLong());
                long entity = in.getLong(), rev = in.getLong(), seq = in.getLong(), laps = in.getLong(), next = in.getLong();
                long place = in.getLong(), elapsed = in.getLong(), dnf = in.getLong(), graphic = in.getLong();
                if (dnf < 0 || dnf >= ModernPetRace.Dnf.values().length) throw error(Failure.CORRUPT);
                rows.add(new ModernPetRaceProtocol.ResultRow(key, new ModernPetRace.Runner(entity, rev, seq, laps, next, place, elapsed, ModernPetRace.Dnf.values()[(int) dnf], graphic)));
            }
            String content = readHash(in); long recorded = in.getLong();
            if (in.hasRemaining()) throw error(Failure.CORRUPT);
            return new ModernPetRaceSettlement.Event(id, match, generation, revision, logical, manifest, interval, gates, course, first, deadline, rows, content, recorded);
        } catch (OutboxException exact) { throw exact; }
        catch (RuntimeException malformed) { throw error(Failure.CORRUPT); }
    }
    private static String readHash(ByteBuffer in) {
        byte[] hash = new byte[32]; in.get(hash); return java.util.HexFormat.of().formatHex(hash);
    }
    private static int compare(UUID a, UUID b) {
        int high = Long.compareUnsigned(a.getMostSignificantBits(), b.getMostSignificantBits());
        return high == 0 ? Long.compareUnsigned(a.getLeastSignificantBits(), b.getLeastSignificantBits()) : high;
    }
    private static byte[] hash(byte[] bytes, int length) {
        try { var sha = MessageDigest.getInstance("SHA-256"); sha.update(bytes, 0, length); return sha.digest(); }
        catch (NoSuchAlgorithmException impossible) { throw new AssertionError(impossible); }
    }
    private static void validateDeadline(long deadline) throws OutboxException {
        long remaining = deadline - System.nanoTime();
        if (remaining <= 0 || remaining > MAX_DEADLINE_NANOS) throw error(Failure.DEADLINE);
        checkDeadline(deadline);
    }
    private static void checkDeadline(long deadline) throws OutboxException {
        if (Thread.currentThread().isInterrupted() || deadline - System.nanoTime() <= 0) throw error(Failure.DEADLINE);
    }
    private Set<OpenOption> options(boolean denyWriters, StandardOpenOption... standard) {
        var options = new HashSet<OpenOption>(List.of(standard)); options.add(LinkOption.NOFOLLOW_LINKS);
        if (windowsSharing) { options.add(ExtendedOpenOption.NOSHARE_DELETE); if (denyWriters) options.add(ExtendedOpenOption.NOSHARE_WRITE); }
        return Set.copyOf(options);
    }
    private static BasicFileAttributes attributes(Path path) throws IOException { return Files.readAttributes(path, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS); }
    private static void checked(Path path, boolean exists) throws OutboxException {
        try { ModernServerConfig.checkedLocalPath(path, exists); }
        catch (ModernServerConfig.ConfigException failure) { throw error(Failure.INVALID_PATH); }
    }
    private static OutboxException error(Failure failure) { return new OutboxException(failure); }
}

