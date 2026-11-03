package local.lito381.server;

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
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;
import com.sun.nio.file.ExtendedOpenOption;

/**
 * Isolated numeric-key persistence, NOT an ownership/equipment/ability authority.
 * Callers qualify the source binding and each instance, evaluate the whole candidate,
 * and retain their selection Operation until backend Work.close(). This class never
 * writes source SQL or GameStateStore's name-keyed native sidecar.
 *
 * <p>A complete bounded snapshot is forced to a new sibling, then atomically renamed.
 * No non-atomic replacement fallback is allowed. Success is a force-confirmed local
 * commit and is tested across process/store reopen; Java cannot promise directory
 * entry survival after Windows power loss. The root must be an administrator-owned
 * local directory: path checks are not protection against a privileged ancestor swap.
 * The lifetime file lock excludes cooperating writers, not arbitrary external edits.</p>
 */
final class ModernEquipmentOverrideStore implements AutoCloseable {
    private static final int MAGIC = 0x4d454f31; // MEO1; independent of native inventory encoding.
    private static final int HEADER = 64, ROW = 49, DIGEST = 32;
    static final int MAX_BYTES = HEADER + ModernCharacterData.MAX_ITEMS * ROW + DIGEST;

    enum Failure { INVALID_PATH, BUSY, CLOSED, CORRUPT, IO, COMMIT_UNCERTAIN,
        CONFLICT, INVALID_REBIND, REVISION_EXHAUSTED, INVALID_OPERATION }
    static final class StoreException extends IOException {
        private final Failure failure;
        StoreException(Failure failure) {
            super("modern equipment storage: " + failure); // No file/account/source text.
            this.failure = failure;
        }
        Failure failure() { return failure; }
    }

    /** Provenance digest, not a password or authentication token. No mutable arrays escape. */
    static final class Fingerprint {
        private final byte[] bytes;
        Fingerprint(byte[] bytes) {
            if (bytes == null || bytes.length != DIGEST) throw new IllegalArgumentException("invalid equipment fingerprint");
            this.bytes = bytes.clone();
        }
        byte[] bytes() { return bytes.clone(); }
        @Override public boolean equals(Object other) {
            return other instanceof Fingerprint value && MessageDigest.isEqual(bytes, value.bytes);
        }
        @Override public int hashCode() { return Arrays.hashCode(bytes); }
        @Override public String toString() { return "EquipmentFingerprint[redacted]"; }
    }

    record ItemOverride(long itemObjectId, long catalogId, Fingerprint sourceItem, boolean equipped) {
        ItemOverride {
            if (itemObjectId <= 0 || catalogId <= 0) throw new IllegalArgumentException("invalid equipment identity");
            Objects.requireNonNull(sourceItem);
        }
    }

    /** Binding must include stable source configuration and qualified account owner, not display names. */
    record Replacement(Fingerprint binding, List<ItemOverride> overrides) {
        Replacement {
            Objects.requireNonNull(binding);
            overrides = canonical(overrides);
        }
    }
    record Stored(TournamentPresence.CharacterKey key, long revision, Replacement value) {
        Stored {
            Objects.requireNonNull(key); Objects.requireNonNull(value);
            if (revision <= 0) throw new IllegalArgumentException("invalid stored equipment revision");
        }
    }
    /** Revision 0 means an absent record only, never a persisted tombstone that can cause ABA. */
    record Expected(long revision, Fingerprint binding) {
        Expected {
            if (revision < 0 || ((revision == 0) != (binding == null))) {
                throw new IllegalArgumentException("invalid equipment expectation");
            }
        }
        static Expected absent() { return new Expected(0, null); }
        static Expected of(Stored value) { return new Expected(value.revision(), value.value().binding()); }
    }
    record Commit(Stored stored, boolean changed) {}

    /** Trusted in-memory admission only. Called after force and the selection
     * permit, with no lease/navigation monitor held; must never perform I/O.
     * Its own world monitor must be released before the atomic move starts. */
    @FunctionalInterface interface CommitGuard { boolean admit(); }

    /** The seam permits deterministic force/move failure tests, not an alternate production writer. */
    interface FileOps {
        default void force(FileChannel file) throws IOException { file.force(true); }
        default void move(Path from, Path to) throws IOException {
            Files.move(from, to, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private final Path directory, lockPath;
    private final boolean windowsSharing;
    private final FileChannel lockFile;
    private final FileLock writerLock;
    private final Object lockIdentity;
    private final FileOps files;
    private final ReentrantLock io = new ReentrantLock();
    private final AtomicBoolean closing = new AtomicBoolean();
    private volatile boolean poisoned;

    ModernEquipmentOverrideStore(Path directory) throws StoreException {
        this(directory, new FileOps() {});
    }
    ModernEquipmentOverrideStore(Path directory, FileOps files) throws StoreException {
        this.files = Objects.requireNonNull(files);
        if (directory == null || !directory.isAbsolute() || directory.getFileName() == null) throw error(Failure.INVALID_PATH);
        this.directory = directory;
        windowsSharing = "\\".equals(directory.getFileSystem().getSeparator());
        lockPath = directory.resolve(".modern-equipment.lock");
        FileChannel opened = null;
        FileLock acquired = null;
        try {
            checked(lockPath, false);
            opened = FileChannel.open(lockPath, openOptions(false, StandardOpenOption.CREATE,
                    StandardOpenOption.READ, StandardOpenOption.WRITE));
            acquired = opened.tryLock();
            if (acquired == null) throw error(Failure.BUSY);
            checked(lockPath, true);
            lockIdentity = attributes(lockPath).fileKey();
            // JDK 17 Windows BasicFileAttributes.fileKey() is optional/null.
            // NOSHARE_DELETE pins the lock's directory entry for its entire
            // handle lifetime. Unsupported options fail closed, never retry
            // with a weaker share mode. Other providers still require fileKey.
            if (lockIdentity == null && !windowsSharing) throw error(Failure.INVALID_PATH);
            lockFile = opened; writerLock = acquired;
        } catch (IOException | RuntimeException failure) {
            if (acquired != null) try { acquired.close(); } catch (IOException ignored) {}
            if (opened != null) try { opened.close(); } catch (IOException ignored) {}
            if (failure instanceof StoreException known) throw known;
            throw error(failure instanceof OverlappingFileLockException ? Failure.BUSY : Failure.INVALID_PATH);
        }
    }

    Optional<Stored> read(TournamentPresence.CharacterKey key, ModernBackendScope scope) throws IOException {
        Objects.requireNonNull(key); Objects.requireNonNull(scope);
        enter(scope);
        try {
            verifyNamespace();
            return readFile(key, scope);
        } catch (StoreException | ModernBackendExecutor.BackendException failure) {
            throw failure;
        } catch (IOException | RuntimeException failure) {
            throw error(Failure.IO);
        } finally { leave(); }
    }

    /**
     * A stale revision is a conflict even if the desired state happens to match.
     * An acknowledged retry uses read + the latest revision, not inverse toggling.
     * Rebinding an existing numeric character identity must explicitly clear its
     * prior overrides, preserving the monotonically increasing revision.
     */
    Commit compareAndSet(TournamentPresence.CharacterKey key, Expected expected, Replacement replacement,
            ModernBackendScope scope, ModernEquipmentLease.Operation operation) throws IOException {
        return compareAndSet(key, expected, replacement, scope, operation, () -> true);
    }

    Commit compareAndSet(TournamentPresence.CharacterKey key, Expected expected, Replacement replacement,
            ModernBackendScope scope, ModernEquipmentLease.Operation operation, CommitGuard guard) throws IOException {
        Objects.requireNonNull(guard);
        Objects.requireNonNull(key); Objects.requireNonNull(expected); Objects.requireNonNull(replacement);
        Objects.requireNonNull(scope); Objects.requireNonNull(operation);
        if (!key.equals(operation.key()) || operation.scope() != scope) throw error(Failure.INVALID_OPERATION);
        enter(scope);
        Path temporary = null;
        boolean createdTemporary = false, moveAttempted = false, moved = false;
        try {
            verifyNamespace();
            Stored old = readFile(key, scope).orElse(null);
            if (old == null ? expected.revision() != 0
                    : expected.revision() != old.revision() || !old.value().binding().equals(expected.binding())) {
                throw error(Failure.CONFLICT);
            }
            if (old != null && !old.value().binding().equals(replacement.binding()) && !replacement.overrides().isEmpty()) {
                throw error(Failure.INVALID_REBIND);
            }
            if (old != null && old.value().equals(replacement)) {
                // Still fence the selection: a no-op is not permission to ACK a
                // retired owner. The operation holder, never this store, releases it.
                if (!operation.tryCommit() || !guard.admit()) throw error(Failure.INVALID_OPERATION);
                return new Commit(old, false);
            }
            long revision;
            try { revision = Math.addExact(expected.revision(), 1); }
            catch (ArithmeticException overflow) { throw error(Failure.REVISION_EXHAUSTED); }
            Stored next = new Stored(key, revision, replacement);
            byte[] encoded = encode(next);
            Path destination = path(key);
            temporary = directory.resolve(".equipment-pending-" + UUID.randomUUID() + ".tmp");
            checked(temporary, false);
            try (FileChannel file = FileChannel.open(temporary,
                    openOptions(true, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE))) {
                createdTemporary = true;
                ByteBuffer bytes = ByteBuffer.wrap(encoded);
                while (bytes.hasRemaining()) {
                    scope.checkActive();
                    if (file.write(bytes) <= 0) throw error(Failure.IO);
                }
                files.force(file);
            }
            scope.checkActive();
            verifyNamespace();
            checked(destination, false);
            // Ownership and invalidation serialize only this short admission.
            // File IO MUST NOT execute under navigation/presence/lease monitors.
            if (!operation.tryCommit() || !guard.admit()) throw error(Failure.INVALID_OPERATION);
            moveAttempted = true;
            files.move(temporary, destination);
            moved = true;
            // No further failing IO between the successful commit and returning
            // its immutable result. Caller cancellation can still suppress ACK;
            // the committed revision remains authoritative on reconnect.
            return new Commit(next, true);
        } catch (StoreException | ModernBackendExecutor.BackendException failure) {
            throw failure;
        } catch (IOException | RuntimeException failure) {
            throw error(moveAttempted ? Failure.COMMIT_UNCERTAIN : Failure.IO);
        } finally {
            if (createdTemporary && !moved) {
                try { Files.deleteIfExists(temporary); }
                catch (IOException | RuntimeException cleanupFailed) { poisoned = true; }
            }
            leave();
        }
    }

    /**
     * Reject new calls without waiting for running data IO. Already entered
     * calls still own their IO admission and may commit: the owning authority
     * must invalidate selection leases when shutting down. The last IO call
     * releases the OS lock; closing an idle OS handle has no Java hard deadline.
     */
    @Override public void close() {
        closing.set(true);
        // A test/backend callback can request close reentrantly; it must not
        // release the writer identity while its own IO operation still runs.
        if (io.isHeldByCurrentThread()) return;
        if (io.tryLock()) {
            try { closeOwner(); } finally { io.unlock(); }
        }
    }
    boolean isQuiescent() { return closing.get() && !lockFile.isOpen(); }
    private void enter(ModernBackendScope scope) throws IOException {
        if (closing.get()) throw error(Failure.CLOSED);
        boolean locked;
        try { locked = io.tryLock(scope.remainingMillis(), TimeUnit.MILLISECONDS); }
        catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw new ModernBackendExecutor.BackendException(ModernBackendExecutor.Failure.CANCELLED);
        }
        if (!locked) { scope.checkActive(); throw error(Failure.BUSY); }
        try {
            scope.checkActive();
            if (closing.get()) throw error(Failure.CLOSED);
            if (poisoned) throw error(Failure.IO);
        } catch (IOException | RuntimeException failure) { leave(); throw failure; }
    }
    private void leave() {
        try { if (closing.get()) closeOwner(); } finally { io.unlock(); }
    }
    private void closeOwner() {
        try { writerLock.close(); } catch (IOException ignored) { poisoned = true; }
        try { lockFile.close(); } catch (IOException ignored) { poisoned = true; }
    }
    private void verifyNamespace() throws StoreException {
        try {
            checked(lockPath, true);
            if (!writerLock.isValid() || !Objects.equals(lockIdentity, attributes(lockPath).fileKey())) throw error(Failure.INVALID_PATH);
        } catch (IOException failure) {
            if (failure instanceof StoreException known) throw known;
            throw error(Failure.INVALID_PATH);
        }
    }
    private Path path(TournamentPresence.CharacterKey key) {
        return directory.resolve("source-" + key.sourceId() + "-character-" + key.objectId() + ".meo");
    }
    private Optional<Stored> readFile(TournamentPresence.CharacterKey key, ModernBackendScope scope) throws IOException {
        Path path = path(key);
        checked(path, false);
        BasicFileAttributes before;
        try { before = attributes(path); }
        catch (NoSuchFileException absent) { return Optional.empty(); }
        if (!before.isRegularFile() || before.size() < HEADER + DIGEST || before.size() > MAX_BYTES) throw error(Failure.CORRUPT);
        ByteBuffer bytes = ByteBuffer.allocate((int) before.size() + 1);
        try (FileChannel file = FileChannel.open(path, openOptions(true, StandardOpenOption.READ))) {
            while (bytes.hasRemaining()) {
                scope.checkActive();
                int read = file.read(bytes);
                if (read < 0) break;
                if (read == 0) throw error(Failure.IO);
            }
            checked(path, true);
            BasicFileAttributes after = attributes(path);
            if (!Objects.equals(before.fileKey(), after.fileKey()) || before.size() != after.size()
                    || !before.lastModifiedTime().equals(after.lastModifiedTime()) || bytes.position() != before.size()) {
                throw error(Failure.CORRUPT);
            }
        }
        scope.checkActive();
        return Optional.of(decode(Arrays.copyOf(bytes.array(), bytes.position()), key));
    }

    static byte[] encode(Stored stored) {
        int count = stored.value().overrides().size();
        byte[] bytes = new byte[HEADER + count * ROW + DIGEST];
        ByteBuffer output = ByteBuffer.wrap(bytes);
        output.putInt(MAGIC).putShort((short) 1).putShort((short) 0)
                .putInt(stored.key().sourceId()).putLong(stored.key().objectId()).putLong(stored.revision())
                .put(stored.value().binding().bytes).putInt(count);
        for (ItemOverride item : stored.value().overrides()) {
            output.putLong(item.itemObjectId()).putLong(item.catalogId()).put(item.sourceItem().bytes)
                    .put((byte) (item.equipped() ? 1 : 0));
        }
        output.put(digest(bytes, output.position()));
        return bytes;
    }
    static Stored decode(byte[] bytes, TournamentPresence.CharacterKey key) throws StoreException {
        if (bytes == null || bytes.length < HEADER + DIGEST || bytes.length > MAX_BYTES) throw error(Failure.CORRUPT);
        if (!MessageDigest.isEqual(digest(bytes, bytes.length - DIGEST), Arrays.copyOfRange(bytes, bytes.length - DIGEST, bytes.length))) {
            throw error(Failure.CORRUPT);
        }
        try {
            ByteBuffer input = ByteBuffer.wrap(bytes);
            if (input.getInt() != MAGIC || input.getShort() != 1 || input.getShort() != 0) throw error(Failure.CORRUPT);
            var actualKey = new TournamentPresence.CharacterKey(input.getInt(), input.getLong());
            if (!actualKey.equals(key)) throw error(Failure.CORRUPT);
            long revision = input.getLong();
            byte[] binding = new byte[DIGEST]; input.get(binding);
            int count = input.getInt();
            if (count < 0 || count > ModernCharacterData.MAX_ITEMS || bytes.length != HEADER + count * ROW + DIGEST) throw error(Failure.CORRUPT);
            var overrides = new ArrayList<ItemOverride>(count);
            long previous = 0;
            for (int index = 0; index < count; index++) {
                long item = input.getLong(), catalog = input.getLong();
                byte[] fingerprint = new byte[DIGEST]; input.get(fingerprint);
                byte flag = input.get();
                if (item <= previous || (flag != 0 && flag != 1)) throw error(Failure.CORRUPT);
                overrides.add(new ItemOverride(item, catalog, new Fingerprint(fingerprint), flag == 1));
                previous = item;
            }
            return new Stored(actualKey, revision, new Replacement(new Fingerprint(binding), overrides));
        } catch (RuntimeException invalid) { throw error(Failure.CORRUPT); }
    }
    private static List<ItemOverride> canonical(List<ItemOverride> values) {
        if (values == null || values.size() > ModernCharacterData.MAX_ITEMS) throw new IllegalArgumentException("invalid equipment override count");
        var copy = new ArrayList<>(values);
        copy.sort(Comparator.comparingLong(ItemOverride::itemObjectId));
        long previous = 0;
        for (ItemOverride value : copy) {
            if (value.itemObjectId() <= previous) throw new IllegalArgumentException("duplicate equipment override");
            previous = value.itemObjectId();
        }
        return List.copyOf(copy);
    }
    private static byte[] digest(byte[] value, int length) {
        try {
            MessageDigest hash = MessageDigest.getInstance("SHA-256");
            hash.update(value, 0, length);
            return hash.digest();
        } catch (NoSuchAlgorithmException impossible) { throw new AssertionError(impossible); }
    }
    private static BasicFileAttributes attributes(Path path) throws IOException {
        return Files.readAttributes(path, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
    }
    private Set<OpenOption> openOptions(boolean denyOtherWriters, StandardOpenOption... standard) {
        var options = new HashSet<OpenOption>(List.of(standard));
        options.add(LinkOption.NOFOLLOW_LINKS);
        if (windowsSharing) {
            options.add(ExtendedOpenOption.NOSHARE_DELETE);
            if (denyOtherWriters) options.add(ExtendedOpenOption.NOSHARE_WRITE);
        }
        return Set.copyOf(options);
    }
    private static void checked(Path path, boolean required) throws StoreException {
        try { ModernServerConfig.checkedLocalPath(path, required); }
        catch (ModernServerConfig.ConfigException failure) { throw error(Failure.INVALID_PATH); }
    }
    private static StoreException error(Failure failure) { return new StoreException(failure); }
}
