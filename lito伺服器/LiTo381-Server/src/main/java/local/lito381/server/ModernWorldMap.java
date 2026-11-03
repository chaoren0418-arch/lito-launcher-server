package local.lito381.server;

import com.sun.nio.file.ExtendedOpenOption;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Comparator;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.BooleanSupplier;

/**
 * Immutable, explicitly selected shared Lineage S32 geometry. Never consults LTmap,
 * ArenaPassabilityCatalog, SQL, PAK files, or a same-number native arena spawn.
 *
 * <p>Layer layout follows the local PakViewer S32Reader and modern_lito/map.rs.
 * Boundary ownership matches map.rs: attribute1 owns north, attribute2 owns east.
 * Modern diagonal movement requires one complete two-cardinal route; this is
 * intentionally stricter than the existing TW minimap destination-only check.
 * Portals are closed: this slice does not implement native map transitions.
 * Geometry is static, not a creature/door collision or combat authority.</p>
 *
 * <p>Reads are bounded and cancellation is checked between reads/parsing steps;
 * an OS filesystem call has no promised hard deadline. Windows leaf handles deny
 * write/delete sharing. Ancestor checks are not protection against a privileged
 * concurrent directory replacement: deploy under an administrator-owned root.</p>
 */
final class ModernWorldMap {
    // Geometry budget; wire admission separately requires the large-map capability.
    static final int SIDE = 64, MAX_SECTORS = 64, MAX_SECTOR_BYTES = 8 * 1024 * 1024;
    static final int MAX_TOTAL_BYTES = 32 * 1024 * 1024, MAX_SPAWNS = 100;
    private static final int CELLS = SIDE * SIDE, MAX_RECORDS = 262_144;
    private static final int[] DX = {0, 1, 1, 1, 0, -1, -1, -1};
    private static final int[] DY = {-1, -1, 0, 1, 1, 1, 0, -1};
    private static final Comparator<SectorKey> ORDER = Comparator.comparingInt(SectorKey::x).thenComparingInt(SectorKey::y);

    enum Failure { INVALID_SELECTION, INVALID_PATH, MISSING_SECTOR, CHANGED_FILE, TOO_LARGE,
        INVALID_S32, UNSUPPORTED_S32, CANCELLED, INVALID_SPAWNS, IO }
    static final class MapException extends IOException {
        private final Failure failure;
        MapException(Failure failure) { super("modern world map: " + failure); this.failure = failure; }
        Failure failure() { return failure; }
    }

    record Cell(int x, int y) {}
    record SectorKey(int x, int y) {
        SectorKey { if (x < 0 || x > 65535 || y < 0 || y > 65535) throw new IllegalArgumentException("invalid sector key"); }
        String fileName() { return String.format(Locale.ROOT, "%04x%04x.s32", x, y); }
        Cell origin() { return new Cell(32704 + (x - 0x7fff) * SIDE, 32704 + (y - 0x7fff) * SIDE); }
    }
    record SectorManifest(SectorKey key, int byteLength, String rawSha256) {}
    record Manifest(int mapId, List<SectorManifest> sectors, String digest) {
        Manifest { sectors = List.copyOf(sectors); }
    }
    record SectorBounds(SectorKey key, Cell origin, int width, int height) {}
    private record Sector(short[] attributes, BitSet portals, SectorManifest source) {}

    private final int mapId;
    private final Map<SectorKey, Sector> sectors;
    private final Manifest manifest;

    /** Immutable deterministic candidates only from the explicit manifest; standing excludes portals. */
    List<Cell> teleportCells() {
        var result = new ArrayList<Cell>();
        for (var sector : manifest.sectors()) {
            Cell origin = sector.key().origin();
            for (int y = 0; y < SIDE; y++) for (int x = 0; x < SIDE; x++) {
                var cell = new Cell(Math.addExact(origin.x(), x), Math.addExact(origin.y(), y));
                if (standing(cell)) result.add(cell);
            }
        }
        return List.copyOf(result);
    }

    private ModernWorldMap(int mapId, Map<SectorKey, Sector> sectors, BooleanSupplier cancelled) throws MapException {
        this.mapId = mapId;
        this.sectors = Map.copyOf(sectors);
        List<SectorManifest> sources = sectors.keySet().stream().sorted(ORDER).map(key -> sectors.get(key).source).toList();
        this.manifest = new Manifest(mapId, sources, canonicalDigest(sources, cancelled));
    }

    /** No enumeration or fallback: these numeric map/sector identifiers are the entire allowlist. */
    static ModernWorldMap load(Path sharedRoot, int mapId, Set<SectorKey> allowlist, BooleanSupplier cancelled)
            throws MapException {
        List<SectorKey> keys = selected(mapId, allowlist);
        Objects.requireNonNull(cancelled);
        rejectRoot(sharedRoot);
        Map<SectorKey, Sector> parsed = new TreeMap<>(ORDER);
        long total = 0;
        for (SectorKey key : keys) {
            active(cancelled);
            Path path = sharedRoot.resolve("map").resolve(Integer.toString(mapId)).resolve(key.fileName());
            byte[] bytes = readSector(path, (int) (MAX_TOTAL_BYTES - total), cancelled);
            total += bytes.length;
            if (total > MAX_TOTAL_BYTES) throw error(Failure.TOO_LARGE);
            parsed.put(key, parse(key, bytes, cancelled));
        }
        return new ModernWorldMap(mapId, parsed, cancelled);
    }

    /** Uses the same strict decoder and manifest; copies caller bytes before hashing and parsing. */
    static ModernWorldMap fromSectorBytes(int mapId, Map<SectorKey, byte[]> inputs, BooleanSupplier cancelled)
            throws MapException {
        if (inputs == null) throw error(Failure.INVALID_SELECTION);
        List<SectorKey> keys = selected(mapId, inputs.keySet());
        Objects.requireNonNull(cancelled);
        Map<SectorKey, Sector> parsed = new TreeMap<>(ORDER);
        long total = 0;
        for (SectorKey key : keys) {
            active(cancelled);
            byte[] source = inputs.get(key);
            if (source == null) throw error(Failure.INVALID_S32);
            total += source.length;
            if (source.length > MAX_SECTOR_BYTES || total > MAX_TOTAL_BYTES) throw error(Failure.TOO_LARGE);
            parsed.put(key, parse(key, source.clone(), cancelled));
        }
        return new ModernWorldMap(mapId, parsed, cancelled);
    }

    int mapId() { return mapId; }
    Manifest manifest() { return manifest; }
    String digest() { return manifest.digest(); }
    List<SectorBounds> sectorBounds() {
        return manifest.sectors().stream().map(row -> new SectorBounds(row.key(), row.key().origin(), SIDE, SIDE)).toList();
    }
    boolean contains(Cell cell) { return cell != null && sector(cell.x(), cell.y()) != null; }
    boolean standing(Cell cell) {
        if (!contains(cell) || portal(cell.x(), cell.y())) return false;
        for (int heading = 0; heading < 8; heading += 2) {
            if (cardinal(cell.x(), cell.y(), (long) cell.x() + DX[heading], (long) cell.y() + DY[heading])) return true;
        }
        return false;
    }
    boolean canStep(Cell from, Cell to) {
        if (from == null || to == null || !contains(from) || !contains(to)) return false;
        long dx = (long) to.x() - from.x(), dy = (long) to.y() - from.y();
        if ((dx == 0 && dy == 0) || Math.abs(dx) > 1 || Math.abs(dy) > 1 || !standing(to)) return false;
        if (dx == 0 || dy == 0) return cardinal(from.x(), from.y(), to.x(), to.y());
        return (cardinal(from.x(), from.y(), to.x(), from.y()) && cardinal(to.x(), from.y(), to.x(), to.y()))
                || (cardinal(from.x(), from.y(), from.x(), to.y()) && cardinal(from.x(), to.y(), to.x(), to.y()));
    }

    /** Fixed heading order N,NE,E,SE,S,SW,W,NW; no mutable masks escape the immutable geometry. */
    int directionMask(Cell cell) {
        if (!contains(cell)) return 0;
        int result = 0;
        for (int heading = 0; heading < 8; heading++) {
            if (canStep(cell, new Cell(cell.x() + DX[heading], cell.y() + DY[heading]))) result |= 1 << heading;
        }
        return result;
    }

    /** All requested spawns must be distinct, standing and in one reachable component. */
    void validateSpawns(List<Cell> spawns) throws MapException {
        if (spawns == null || spawns.isEmpty() || spawns.size() > MAX_SPAWNS) throw error(Failure.INVALID_SPAWNS);
        Set<Cell> wanted = new HashSet<>();
        for (Cell cell : spawns) if (!standing(cell) || !wanted.add(cell)) throw error(Failure.INVALID_SPAWNS);
        Set<Cell> reachable = reachable(spawns.get(0), sectors.size() * CELLS);
        if (!reachable.containsAll(wanted)) throw error(Failure.INVALID_SPAWNS);
    }

    /** Deterministic nearest reachable cells, including anchor first; never borrows LT spawn coordinates. */
    List<Cell> serverChosenSpawns(Cell anchor, int count) throws MapException {
        return serverChosenSpawns(anchor,count,Set.of());
    }
    List<Cell> serverChosenSpawns(Cell anchor,int count,Set<Cell> excluded)throws MapException {
        excluded=Set.copyOf(excluded);
        if(excluded.size()>102)throw error(Failure.INVALID_SPAWNS);
        if (count < 1 || count > MAX_SPAWNS || !standing(anchor)) throw error(Failure.INVALID_SPAWNS);
        List<Cell> result = new ArrayList<>(count);
        Set<Cell> seen = new HashSet<>();
        ArrayDeque<Cell> queue = new ArrayDeque<>();
        seen.add(anchor); queue.add(anchor);
        while (!queue.isEmpty() && result.size() < count) {
            Cell cell = queue.remove(); if(!excluded.contains(cell))result.add(cell);
            addNeighbors(cell, seen, queue);
        }
        if (result.size() != count) throw error(Failure.INVALID_SPAWNS);
        return List.copyOf(result);
    }

    private Set<Cell> reachable(Cell first, int budget) throws MapException {
        Set<Cell> seen = new HashSet<>();
        ArrayDeque<Cell> queue = new ArrayDeque<>();
        seen.add(first); queue.add(first);
        while (!queue.isEmpty()) {
            addNeighbors(queue.remove(), seen, queue);
            if (seen.size() > budget) throw error(Failure.INVALID_SPAWNS);
        }
        return seen;
    }
    private void addNeighbors(Cell cell, Set<Cell> seen, ArrayDeque<Cell> queue) {
        for (int heading = 0; heading < 8; heading++) {
            Cell next = new Cell(cell.x() + DX[heading], cell.y() + DY[heading]);
            if (canStep(cell, next) && seen.add(next)) queue.add(next);
        }
    }

    private boolean cardinal(long x, long y, long nx, long ny) {
        if (sector(x, y) == null || sector(nx, ny) == null || portal(x, y) || portal(nx, ny)) return false;
        if (nx == x && ny == y - 1) return attributeOpen(x, y, 0);
        if (nx == x + 1 && ny == y) return attributeOpen(x, y, 1);
        if (nx == x && ny == y + 1) return attributeOpen(x, y + 1, 0);
        if (nx == x - 1 && ny == y) return attributeOpen(x - 1, y, 1);
        return false;
    }
    private Sector sector(long x, long y) {
        long bx = 0x7fffL + Math.floorDiv(x - 32704L, SIDE), by = 0x7fffL + Math.floorDiv(y - 32704L, SIDE);
        if (bx < 0 || bx > 65535 || by < 0 || by > 65535) return null;
        return sectors.get(new SectorKey((int) bx, (int) by));
    }
    private static int index(long x, long y) {
        return (int) (Math.floorMod(y - 32704L, SIDE) * SIDE + Math.floorMod(x - 32704L, SIDE));
    }
    private boolean portal(long x, long y) { Sector value = sector(x, y); return value == null || value.portals.get(index(x, y)); }
    private boolean attributeOpen(long x, long y, int attribute) {
        Sector value = sector(x, y);
        return value != null && (value.attributes[index(x, y) * 2 + attribute] & 1) == 0;
    }

    private String canonicalDigest(List<SectorManifest> sources, BooleanSupplier cancelled) throws MapException {
        try {
            var storage = new ByteArrayOutputStream();
            var out = new DataOutputStream(storage);
            out.write("LITO-WORLD-MAP\0".getBytes(java.nio.charset.StandardCharsets.US_ASCII));
            out.writeShort(1); out.writeInt(mapId); out.writeShort(sources.size());
            for (SectorManifest row : sources) {
                active(cancelled);
                Cell origin = row.key().origin();
                out.writeShort(row.key().x()); out.writeShort(row.key().y());
                out.writeInt(origin.x()); out.writeInt(origin.y()); out.writeShort(SIDE); out.writeShort(SIDE);
                out.writeInt(row.byteLength()); out.write(HexFormat.of().parseHex(row.rawSha256()));
                for (int y = 0; y < SIDE; y++) {
                    active(cancelled);
                    for (int x = 0; x < SIDE; x++) out.writeByte(directionMask(new Cell(origin.x() + x, origin.y() + y)));
                }
            }
            out.flush(); return sha256(storage.toByteArray());
        } catch (MapException error) { throw error; }
        catch (IOException impossible) { throw new AssertionError(impossible); }
    }

    private static Sector parse(SectorKey key, byte[] bytes, BooleanSupplier cancelled) throws MapException {
        if (bytes.length > MAX_SECTOR_BYTES) throw error(Failure.TOO_LARGE);
        Reader reader = new Reader(bytes, cancelled);
        reader.skip(128 * SIDE * 4); // L1: two half-tiles per world cell.
        reader.records(reader.u16(), 6, 65535); // L2.
        short[] attributes = new short[CELLS * 2];
        for (int i = 0; i < attributes.length; i++) attributes[i] = (short) reader.u16();
        int groups = reader.count(65536), tiles = 0; // L4 is mandatory.
        for (int group = 0; group < groups; group++) {
            reader.u16(); int count = reader.u16(); tiles += count;
            if (tiles > MAX_RECORDS) throw error(Failure.INVALID_S32);
            reader.records(count, 7, MAX_RECORDS);
        }
        BitSet portals = new BitSet(CELLS);
        if (reader.remaining() != 0) reader.records(reader.count(MAX_RECORDS), 5, MAX_RECORDS); // L5.
        if (reader.remaining() != 0) reader.records(reader.count(MAX_RECORDS), 4, MAX_RECORDS); // L6.
        if (reader.remaining() != 0) {
            int count = reader.u16();
            for (int portal = 0; portal < count; portal++) {
                reader.skip(reader.u8()); int halfX = reader.u8(), y = reader.u8();
                if (halfX >= 128 || y >= SIDE) throw error(Failure.UNSUPPORTED_S32);
                portals.set(y * SIDE + halfX / 2);
                reader.skip(6);
            }
        }
        if (reader.remaining() != 0) {
            int packed = reader.u16();
            reader.records(packed & 0x7fff, (packed & 0x8000) != 0 ? 10 : 6, 32767);
        }
        if (reader.remaining() != 0) throw error(Failure.UNSUPPORTED_S32);
        active(cancelled);
        return new Sector(attributes, portals, new SectorManifest(key, bytes.length, sha256(bytes)));
    }

    private static final class Reader {
        final ByteBuffer data; final BooleanSupplier cancelled;
        Reader(byte[] bytes, BooleanSupplier cancelled) { data = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN); this.cancelled = cancelled; }
        int remaining() { return data.remaining(); }
        void need(int bytes) throws MapException { active(cancelled); if (bytes < 0 || data.remaining() < bytes) throw error(Failure.INVALID_S32); }
        int u8() throws MapException { need(1); return Byte.toUnsignedInt(data.get()); }
        int u16() throws MapException { need(2); return Short.toUnsignedInt(data.getShort()); }
        int count(int maximum) throws MapException { need(4); int count = data.getInt(); if (count < 0 || count > maximum) throw error(Failure.INVALID_S32); return count; }
        void skip(int bytes) throws MapException { need(bytes); data.position(data.position() + bytes); }
        void records(int count, int size, int maximum) throws MapException {
            if (count < 0 || count > maximum || (long) count * size > Integer.MAX_VALUE) throw error(Failure.INVALID_S32);
            skip(count * size);
        }
    }

    private static List<SectorKey> selected(int mapId, Set<SectorKey> keys) throws MapException {
        if (mapId < 0 || keys == null || keys.isEmpty() || keys.size() > MAX_SECTORS || keys.stream().anyMatch(Objects::isNull)) {
            throw error(Failure.INVALID_SELECTION);
        }
        return keys.stream().sorted(ORDER).toList();
    }
    private static void rejectRoot(Path root) throws MapException {
        if (root == null || !root.isAbsolute() || root.getFileName() == null) throw error(Failure.INVALID_PATH);
        for (Path part : root) {
            if (Set.of("lito", "ltmap", "lttext", "ltsound", "ltsurf", "lttile", "ltsprite", "lticon")
                    .contains(part.toString().toLowerCase(Locale.ROOT))) throw error(Failure.INVALID_PATH);
        }
    }
    private static byte[] readSector(Path path, int remainingBudget, BooleanSupplier cancelled) throws MapException {
        try {
            ModernServerConfig.checkedLocalPath(path, false);
            if (!Files.exists(path, LinkOption.NOFOLLOW_LINKS)) throw error(Failure.MISSING_SECTOR);
            ModernServerConfig.checkedLocalPath(path, true);
            BasicFileAttributes before = Files.readAttributes(path, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
            int limit = Math.min(MAX_SECTOR_BYTES, remainingBudget);
            if (before.size() > limit) throw error(Failure.TOO_LARGE);
            Set<OpenOption> options = new HashSet<>(Set.of(StandardOpenOption.READ, LinkOption.NOFOLLOW_LINKS));
            boolean windows = "\\".equals(path.getFileSystem().getSeparator());
            if (windows) { options.add(ExtendedOpenOption.NOSHARE_WRITE); options.add(ExtendedOpenOption.NOSHARE_DELETE); }
            try (FileChannel file = FileChannel.open(path, options)) {
                long size = file.size();
                if (size > limit) throw error(Failure.TOO_LARGE);
                if (size != before.size()) throw error(Failure.CHANGED_FILE);
                byte[] bytes = new byte[(int) size];
                ByteBuffer buffer = ByteBuffer.wrap(bytes);
                while (buffer.hasRemaining()) {
                    active(cancelled);
                    int read = file.read(buffer);
                    if (read <= 0) throw error(Failure.CHANGED_FILE);
                }
                active(cancelled);
                if (file.size() != size || file.read(ByteBuffer.allocate(1)) != -1) throw error(Failure.CHANGED_FILE);
                ModernServerConfig.checkedLocalPath(path, true);
                BasicFileAttributes after = Files.readAttributes(path, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
                if ((!windows && before.fileKey() == null) || !Objects.equals(before.fileKey(), after.fileKey())
                        || before.size() != after.size() || !before.lastModifiedTime().equals(after.lastModifiedTime())) {
                    throw error(Failure.CHANGED_FILE);
                }
                return bytes;
            }
        } catch (MapException known) { throw known; }
        catch (ModernServerConfig.ConfigException failure) { throw error(Failure.INVALID_PATH); }
        catch (IOException | RuntimeException failure) { throw error(Failure.IO); }
    }
    private static String sha256(byte[] bytes) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes)); }
        catch (NoSuchAlgorithmException impossible) { throw new AssertionError(impossible); }
    }
    private static void active(BooleanSupplier cancelled) throws MapException {
        if (cancelled.getAsBoolean() || Thread.currentThread().isInterrupted()) throw error(Failure.CANCELLED);
    }
    private static MapException error(Failure failure) { return new MapException(failure); }
}
