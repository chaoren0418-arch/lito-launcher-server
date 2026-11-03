package local.lito381.server;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.LongSupplier;

/**
 * Optional shared, one-shot rental-town equipment handoff journal.
 *
 * <p>The native S_F4 transition can reconnect GAME to another process or host,
 * so a {@link GameHub}-local map is not sufficient.  This journal uses a
 * dedicated lock file and an atomic snapshot replacement.  Two Java processes
 * sharing the same path therefore cannot both consume one capsule.  The path
 * may also be on a shared filesystem whose locking and atomic-rename semantics
 * are honored by every participating host.</p>
 */
final class RentalBattleHandoffStore {
    private static final int FORMAT = 1;
    private static final int MAX_ENTRIES = 1_024;
    private static final int MAX_ITEMS = RuntimeInventory.MAX_SLOTS;
    private static final long MAX_BYTES = 4L * 1024L * 1024L;
    private static final ConcurrentMap<Path, Object> JVM_LOCKS =
            new ConcurrentHashMap<>();

    record Stored(String character, RuntimeInventory inventory,
                  long expiresAtEpochMillis) {
        Stored {
            character = normalize(character);
            if (character.isEmpty() || inventory == null
                    || inventory.size() == 0 || expiresAtEpochMillis <= 0) {
                throw new IllegalArgumentException("invalid stored rental handoff");
            }
            inventory = inventory.copy();
        }

        @Override public RuntimeInventory inventory() {
            return inventory.copy();
        }
    }

    private final Path file;
    private final Path lockFile;
    private final LongSupplier clock;

    RentalBattleHandoffStore(Path file) {
        this(file, System::currentTimeMillis);
    }

    RentalBattleHandoffStore(Path file, LongSupplier clock) {
        this.file = file == null ? null : file.toAbsolutePath().normalize();
        this.lockFile = this.file == null ? null
                : this.file.resolveSibling(this.file.getFileName() + ".lock");
        this.clock = clock == null ? System::currentTimeMillis : clock;
    }

    boolean enabled() {
        return file != null;
    }

    Path file() {
        return file;
    }

    boolean stage(String account, String character, RuntimeInventory inventory,
                  long ttlMillis) throws IOException {
        requireEnabled();
        String accountKey = normalize(account);
        String characterKey = normalize(character);
        if (accountKey.isEmpty() || characterKey.isEmpty() || inventory == null
                || inventory.size() == 0 || ttlMillis <= 0) return false;
        validatePortable(inventory);
        long now = clock.getAsLong();
        long expiresAt;
        try {
            expiresAt = Math.addExact(now, ttlMillis);
        } catch (ArithmeticException overflow) {
            throw new IOException("rental handoff expiry overflow", overflow);
        }
        return locked(() -> {
            LinkedHashMap<String, Stored> entries = read();
            purge(entries, now);
            if (entries.size() >= MAX_ENTRIES && !entries.containsKey(accountKey)) {
                return false;
            }
            entries.put(accountKey,
                    new Stored(characterKey, inventory, expiresAt));
            write(entries);
            return true;
        });
    }

    boolean has(String account, String character) throws IOException {
        requireEnabled();
        String accountKey = normalize(account);
        String characterKey = normalize(character);
        if (accountKey.isEmpty() || characterKey.isEmpty()) return false;
        long now = clock.getAsLong();
        return locked(() -> {
            LinkedHashMap<String, Stored> entries = read();
            boolean changed = purge(entries, now);
            Stored stored = entries.get(accountKey);
            if (changed) write(entries);
            return stored != null && stored.character().equals(characterKey);
        });
    }

    Stored consume(String account, String character) throws IOException {
        requireEnabled();
        String accountKey = normalize(account);
        String characterKey = normalize(character);
        if (accountKey.isEmpty() || characterKey.isEmpty()) return null;
        long now = clock.getAsLong();
        return locked(() -> {
            LinkedHashMap<String, Stored> entries = read();
            boolean changed = purge(entries, now);
            Stored stored = entries.get(accountKey);
            if (stored != null && stored.character().equals(characterKey)) {
                entries.remove(accountKey);
                write(entries);
                return stored;
            }
            if (changed) write(entries);
            return null;
        });
    }

    private <T> T locked(IoOperation<T> operation) throws IOException {
        Path parent = file.getParent();
        if (parent != null) Files.createDirectories(parent);
        Object monitor = JVM_LOCKS.computeIfAbsent(lockFile, ignored -> new Object());
        synchronized (monitor) {
            try (FileChannel channel = FileChannel.open(lockFile,
                    StandardOpenOption.CREATE, StandardOpenOption.WRITE);
                 FileLock ignored = channel.lock()) {
                return operation.run();
            }
        }
    }

    private LinkedHashMap<String, Stored> read() throws IOException {
        LinkedHashMap<String, Stored> result = new LinkedHashMap<>();
        if (!Files.exists(file)) return result;
        long size = Files.size(file);
        if (size <= 0 || size > MAX_BYTES) {
            throw new IOException("invalid rental handoff journal size " + size);
        }
        Properties source = new Properties();
        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            source.load(reader);
        }
        int format = integer(source, "format", 1, FORMAT);
        if (format != FORMAT) {
            throw new IOException("unsupported rental handoff journal format " + format);
        }
        int count = integer(source, "entry.count", 0, MAX_ENTRIES);
        for (int index = 0; index < count; index++) {
            String prefix = "entry." + index + ".";
            String account = text(source, prefix + "account");
            String character = text(source, prefix + "character");
            long expiresAt = number(source, prefix + "expiresAtEpochMillis",
                    1, Long.MAX_VALUE);
            int itemCount = integer(source, prefix + "item.count", 1, MAX_ITEMS);
            RuntimeInventory inventory = inventory(source, prefix, itemCount);
            String accountKey = normalize(account);
            if (accountKey.isEmpty() || result.containsKey(accountKey)) {
                throw new IOException("duplicate or empty rental handoff account");
            }
            try {
                result.put(accountKey, new Stored(character, inventory, expiresAt));
            } catch (IllegalArgumentException invalid) {
                throw new IOException("invalid rental handoff entry " + index, invalid);
            }
        }
        return result;
    }

    private RuntimeInventory inventory(Properties source, String prefix,
                                       int itemCount) throws IOException {
        Map<Long, Integer> counts = new LinkedHashMap<>();
        Map<Long, List<RuntimeInventory.PersistentItem>> states =
                new LinkedHashMap<>();
        Map<Long, InventoryCatalog.Definition> definitions = new LinkedHashMap<>();
        for (int itemIndex = 0; itemIndex < itemCount; itemIndex++) {
            String item = prefix + "item." + itemIndex + ".";
            long catalogId = number(source, item + "catalogId", 1, 0xFFFF_FFFFL);
            long objectId = number(source, item + "objectId", 1, 0xFFFF_FFFFL);
            int quantity = integer(source, item + "quantity", 1, Integer.MAX_VALUE);
            int enchant = integer(source, item + "enchant", -128, 127);
            int durability = integer(source, item + "durability", 0, 0x7F);
            boolean identified = bool(source, item + "identified");
            int blessing = integer(source, item + "blessing",
                    RuntimeInventory.BLESSING_BLESSED,
                    RuntimeInventory.BLESSING_UNIDENTIFIED);
            boolean sourceBound = bool(source, item + "sourceBound");
            boolean tournamentReward = bool(source, item + "tournamentReward");
            boolean equipped = bool(source, item + "equipped");
            int chargeCount = integer(source, item + "chargeCount", 0, 0xFF);
            long itemExpires = number(source, item + "expiresAtEpochMillis",
                    0, Long.MAX_VALUE);
            boolean sealed = bool(source, item + "sealed");
            InventoryCatalog.Definition override;
            try {
                override = DurableItemDefinitionCodec.decode(
                        source.getProperty(item + "definition", ""));
            } catch (IllegalArgumentException invalid) {
                throw new IOException("invalid rental item definition", invalid);
            }
            InventoryCatalog.Definition definition = override;
            if (definition == null && catalogId <= Integer.MAX_VALUE) {
                definition = RentalItemDefinitionCatalog.find((int) catalogId);
            }
            if (definition == null) definition = InventoryCatalog.find(catalogId);
            if (definition == null || definition.catalogId() != catalogId) {
                throw new IOException("unknown rental handoff item " + catalogId);
            }
            if (sourceBound) {
                throw new IOException("source-bound item cannot cross rental handoff");
            }
            RuntimeInventory.PersistentItem state;
            try {
                state = new RuntimeInventory.PersistentItem(
                        objectId, quantity, enchant, durability, identified, blessing,
                        false, tournamentReward, equipped, override, chargeCount,
                        itemExpires, sealed);
                counts.merge(catalogId, quantity, Math::addExact);
                states.computeIfAbsent(catalogId, ignored -> new ArrayList<>()).add(state);
                definitions.putIfAbsent(catalogId, definition);
            } catch (RuntimeException invalid) {
                throw new IOException("invalid rental handoff item state", invalid);
            }
        }
        try {
            RuntimeInventory result = RuntimeInventory.fromDurableState(
                    counts, states, definitions, () -> 1L);
            if (result.size() != itemCount) {
                throw new IOException("rental handoff item count changed while decoding");
            }
            validatePortable(result);
            return result;
        } catch (IllegalStateException | IllegalArgumentException invalid) {
            throw new IOException("invalid rental handoff inventory", invalid);
        }
    }

    private void write(Map<String, Stored> source) throws IOException {
        if (source.size() > MAX_ENTRIES) {
            throw new IOException("too many rental handoff entries");
        }
        Properties target = new Properties();
        target.setProperty("format", Integer.toString(FORMAT));
        List<Map.Entry<String, Stored>> entries = source.entrySet().stream()
                .sorted(Map.Entry.comparingByKey()).toList();
        target.setProperty("entry.count", Integer.toString(entries.size()));
        for (int index = 0; index < entries.size(); index++) {
            Map.Entry<String, Stored> entry = entries.get(index);
            Stored stored = entry.getValue();
            String prefix = "entry." + index + ".";
            target.setProperty(prefix + "account", text(entry.getKey()));
            target.setProperty(prefix + "character", text(stored.character()));
            target.setProperty(prefix + "expiresAtEpochMillis",
                    Long.toString(stored.expiresAtEpochMillis()));
            List<RuntimeInventory.Item> items = new ArrayList<>(stored.inventory().items());
            items.sort(Comparator.comparingLong(RuntimeInventory.Item::objectId));
            target.setProperty(prefix + "item.count", Integer.toString(items.size()));
            for (int itemIndex = 0; itemIndex < items.size(); itemIndex++) {
                RuntimeInventory.Item value = items.get(itemIndex);
                RuntimeInventory.PersistentItem state = value.persistentItem();
                String item = prefix + "item." + itemIndex + ".";
                target.setProperty(item + "catalogId",
                        Long.toString(value.definition().catalogId()));
                target.setProperty(item + "objectId", Long.toString(state.objectId()));
                target.setProperty(item + "quantity", Integer.toString(state.quantity()));
                target.setProperty(item + "enchant", Integer.toString(state.enchantLevel()));
                target.setProperty(item + "durability", Integer.toString(state.durability()));
                target.setProperty(item + "identified", Boolean.toString(state.identified()));
                target.setProperty(item + "blessing", Integer.toString(state.blessing()));
                target.setProperty(item + "sourceBound", Boolean.toString(state.sourceBound()));
                target.setProperty(item + "tournamentReward",
                        Boolean.toString(state.tournamentReward()));
                target.setProperty(item + "equipped", Boolean.toString(state.equipped()));
                target.setProperty(item + "chargeCount", Integer.toString(state.chargeCount()));
                target.setProperty(item + "expiresAtEpochMillis",
                        Long.toString(state.expiresAtEpochMillis()));
                target.setProperty(item + "sealed", Boolean.toString(state.sealed()));
                target.setProperty(item + "definition",
                        DurableItemDefinitionCodec.encode(state.definitionOverride()));
            }
        }
        Path temporary = file.resolveSibling(file.getFileName() + ".tmp");
        try (Writer writer = Files.newBufferedWriter(temporary, StandardCharsets.UTF_8)) {
            target.store(writer, "LiTo381 shared rental battle handoff; atomic one-shot journal");
        }
        if (Files.size(temporary) > MAX_BYTES) {
            throw new IOException("rental handoff journal exceeds maximum size");
        }
        try {
            Files.move(temporary, file, StandardCopyOption.ATOMIC_MOVE,
                    StandardCopyOption.REPLACE_EXISTING);
        } catch (AtomicMoveNotSupportedException ignored) {
            Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static void validatePortable(RuntimeInventory inventory) {
        if (inventory == null || inventory.size() == 0
                || inventory.size() > MAX_ITEMS) {
            throw new IllegalArgumentException("invalid portable rental inventory");
        }
        for (RuntimeInventory.Item item : inventory.items()) {
            if (item.sourceBound()) {
                throw new IllegalArgumentException(
                        "source-bound item lacks portable physical source metadata");
            }
        }
    }

    private static boolean purge(Map<String, Stored> entries, long now) {
        int before = entries.size();
        entries.entrySet().removeIf(entry ->
                now >= entry.getValue().expiresAtEpochMillis());
        return entries.size() != before;
    }

    private void requireEnabled() {
        if (!enabled()) throw new IllegalStateException("rental handoff store is disabled");
    }

    private static int integer(Properties source, String key,
                               int minimum, int maximum) throws IOException {
        long value = number(source, key, minimum, maximum);
        return (int) value;
    }

    private static long number(Properties source, String key,
                               long minimum, long maximum) throws IOException {
        String raw = source.getProperty(key);
        if (raw == null) throw new IOException("missing rental handoff field " + key);
        try {
            long value = Long.parseLong(raw.trim());
            if (value < minimum || value > maximum) {
                throw new IOException("rental handoff field out of range " + key);
            }
            return value;
        } catch (NumberFormatException invalid) {
            throw new IOException("invalid rental handoff number " + key, invalid);
        }
    }

    private static boolean bool(Properties source, String key) throws IOException {
        String raw = source.getProperty(key);
        if ("true".equals(raw)) return true;
        if ("false".equals(raw)) return false;
        throw new IOException("invalid rental handoff boolean " + key);
    }

    private static String text(String value) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(
                value.getBytes(StandardCharsets.UTF_8));
    }

    private static String text(Properties source, String key) throws IOException {
        String raw = source.getProperty(key);
        if (raw == null || raw.length() > 2_048) {
            throw new IOException("invalid rental handoff text " + key);
        }
        try {
            byte[] bytes = Base64.getUrlDecoder().decode(raw);
            if (bytes.length == 0 || bytes.length > 512) {
                throw new IOException("invalid rental handoff text size " + key);
            }
            return new String(bytes, StandardCharsets.UTF_8);
        } catch (IllegalArgumentException invalid) {
            throw new IOException("invalid rental handoff text encoding " + key, invalid);
        }
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    @FunctionalInterface
    private interface IoOperation<T> {
        T run() throws IOException;
    }
}
