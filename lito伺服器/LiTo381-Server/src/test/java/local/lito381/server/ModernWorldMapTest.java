package local.lito381.server;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ModernWorldMapTest {
    @TempDir Path temporary;
    static final ModernWorldMap.SectorKey KEY = new ModernWorldMap.SectorKey(0x7fff, 0x8000);
    private static final int LAYER3 = 32770, LAYER4 = 49154;

    /** Complete known L1-L8; all geometry edges open, no portal or decoration. */
    static byte[] openSectorBytes() { return new byte[49170]; }
    static void setAttributes(byte[] bytes, int x, int y, short attribute1, short attribute2) {
        if (x < 0 || x >= 64 || y < 0 || y >= 64) throw new IllegalArgumentException("invalid fixture cell");
        ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).position(LAYER3 + (y * 64 + x) * 4)
                .putShort(attribute1).putShort(attribute2);
    }
    private static byte[] blockedSectorBytes() {
        byte[] bytes = openSectorBytes();
        for (int y = 0; y < 64; y++) for (int x = 0; x < 64; x++) setAttributes(bytes, x, y, (short) 1, (short) 1);
        return bytes;
    }
    private static ModernWorldMap map(byte[] bytes) throws Exception {
        return ModernWorldMap.fromSectorBytes(514, Map.of(KEY, bytes), () -> false);
    }
    private static ModernWorldMap.Cell cell(int x, int y) {
        return new ModernWorldMap.Cell(KEY.origin().x() + x, KEY.origin().y() + y);
    }

    @Test void worldCoordinatesMatchSharedRustAndMinimapAtEverySectorBoundary() throws Exception {
        assertEquals(new ModernWorldMap.Cell(32704, 32768), KEY.origin());
        assertEquals(new ModernWorldMap.Cell(32512, 32192), new ModernWorldMap.SectorKey(0x7ffc, 0x7ff7).origin());
        assertEquals(new ModernWorldMap.Cell(-2064384, -2064384), new ModernWorldMap.SectorKey(0, 0).origin());
        assertEquals(new ModernWorldMap.Cell(2129856, 2129856), new ModernWorldMap.SectorKey(65535, 65535).origin());
        var map = map(openSectorBytes());
        assertTrue(map.contains(cell(0, 0))); assertTrue(map.contains(cell(63, 63)));
        assertFalse(map.contains(cell(-1, 0))); assertFalse(map.contains(cell(64, 63)));
        assertFalse(map.contains(new ModernWorldMap.Cell(Integer.MIN_VALUE, Integer.MAX_VALUE)));
        assertThrows(IllegalArgumentException.class, () -> new ModernWorldMap.SectorKey(-1, 0));
    }

    @Test void allEightDirectionsAreExactAdjacentStepsAndMissingSectorsAreClosed() throws Exception {
        var map = map(openSectorBytes());
        assertEquals(255, map.directionMask(cell(10, 10)));
        assertEquals(0x1c, map.directionMask(cell(0, 0)));
        assertFalse(map.canStep(cell(10, 10), cell(10, 10)));
        assertFalse(map.canStep(cell(10, 10), cell(12, 10)));
        assertFalse(map.canStep(cell(-1, 0), cell(0, 0)));
        assertFalse(map.canStep(cell(0, 0), cell(-1, 0)));
        assertFalse(map.canStep(new ModernWorldMap.Cell(Integer.MIN_VALUE, 0), new ModernWorldMap.Cell(Integer.MAX_VALUE, 0)));
    }

    @Test void standingUsesNeighborOwnedEdgesAndPreservesSignedAttributeBits() throws Exception {
        byte[] bytes = blockedSectorBytes();
        setAttributes(bytes, 10, 10, (short) 0x8001, (short) 0x8000); // Only east open.
        var map = map(bytes);
        assertTrue(map.standing(cell(10, 10)));
        assertTrue(map.standing(cell(11, 10)), "west boundary belongs to the neighbor");
        assertEquals(1 << 2, map.directionMask(cell(10, 10)));
        assertEquals(1 << 6, map.directionMask(cell(11, 10)));
        assertFalse(map.standing(cell(12, 10)));
    }

    @Test void diagonalNeedsOneCompleteCornerRouteNotMerelyAStandingDestination() throws Exception {
        byte[] bytes = blockedSectorBytes();
        setAttributes(bytes, 10, 10, (short) 1, (short) 0); // A -> east corner.
        setAttributes(bytes, 11, 10, (short) 0, (short) 1); // east corner -> NE target.
        assertTrue(map(bytes).canStep(cell(10, 10), cell(11, 9)));
        setAttributes(bytes, 11, 10, (short) 1, (short) 1);
        setAttributes(bytes, 11, 9, (short) 0, (short) 1); // Target still stands but routes blocked.
        var closed = map(bytes);
        assertTrue(closed.standing(cell(11, 9)));
        assertFalse(closed.canStep(cell(10, 10), cell(11, 9)));
        setAttributes(bytes, 10, 10, (short) 0, (short) 0); // Alternate north route.
        setAttributes(bytes, 10, 9, (short) 1, (short) 0);
        assertTrue(map(bytes).canStep(cell(10, 10), cell(11, 9)));
    }

    @Test void portalsAreNeitherSpawnTilesNorTraversableEdges() throws Exception {
        byte[] bytes = withPortal(10, 10);
        var map = map(bytes);
        assertTrue(map.contains(cell(10, 10))); assertFalse(map.standing(cell(10, 10)));
        assertFalse(map.canStep(cell(9, 10), cell(10, 10)));
        assertFalse(map.canStep(cell(10, 10), cell(9, 10)));
        assertThrows(ModernWorldMap.MapException.class, () -> map.validateSpawns(List.of(cell(10, 10))));
        byte[] bad = bytes.clone(); bad[49169] = (byte) 128;
        assertEquals(ModernWorldMap.Failure.UNSUPPORTED_S32, assertThrows(ModernWorldMap.MapException.class, () -> map(bad)).failure());
    }

    @Test void multiSectorBoundaryAndCanonicalOrderDependOnTheCompleteExplicitAllowlist() throws Exception {
        var east = new ModernWorldMap.SectorKey(KEY.x() + 1, KEY.y());
        var single = map(openSectorBytes());
        var both = ModernWorldMap.fromSectorBytes(514, Map.of(KEY, openSectorBytes(), east, openSectorBytes()), () -> false);
        assertFalse(single.canStep(cell(63, 10), cell(64, 10)));
        assertTrue(both.canStep(cell(63, 10), cell(64, 10)));
        assertTrue(both.canStep(cell(64, 10), cell(63, 10)));
        var reversed = new HashMap<ModernWorldMap.SectorKey, byte[]>();
        reversed.put(east, openSectorBytes()); reversed.put(KEY, openSectorBytes());
        assertEquals(both.manifest(), ModernWorldMap.fromSectorBytes(514, reversed, () -> false).manifest());
        assertNotEquals(single.digest(), both.digest());
        assertEquals(List.of(KEY, east), both.manifest().sectors().stream().map(ModernWorldMap.SectorManifest::key).toList());
        assertThrows(UnsupportedOperationException.class, () -> both.manifest().sectors().clear());
    }

    @Test void exactSourceBytesAndMapIdentityAreBoundEvenIfPassabilityIsUnchanged() throws Exception {
        byte[] bytes = openSectorBytes(); var original = map(bytes); String digest = original.digest();
        // Independently generated by a bounded PowerShell/.NET BE encoder, not this decoder.
        assertEquals("ef81ffbc2095f821ee3ae6e2d67fb8505b266ad2163bef49997d5e9cfce0a0a2", original.manifest().sectors().get(0).rawSha256());
        assertEquals("5dd1c01c5f862a2b6bc4ac3b81a731871777feed9788e20bbdc15a2ce9dffa5e", digest);
        bytes[3] = 7; // Uninterpreted L1 byte, retained by the raw-file fingerprint.
        var changed = map(bytes);
        assertEquals(original.directionMask(cell(8, 8)), changed.directionMask(cell(8, 8)));
        assertNotEquals(digest, changed.digest());
        assertEquals(digest, original.digest(), "caller array mutation must not change existing geometry");
        assertNotEquals(digest, ModernWorldMap.fromSectorBytes(515, Map.of(KEY, openSectorBytes()), () -> false).digest());
        assertTrue(digest.matches("[0-9a-f]{64}"));
    }

    @Test void uniqueSpawnsMustBeStandingAndInOneComponentWithDeterministicBfsOrder() throws Exception {
        var open = map(openSectorBytes());
        assertEquals(List.of(cell(10, 10), cell(10, 9), cell(11, 9), cell(11, 10)), open.serverChosenSpawns(cell(10, 10), 4));
        var hundred = open.serverChosenSpawns(cell(10, 10), 100);
        assertEquals(100, Set.copyOf(hundred).size()); open.validateSpawns(hundred);
        assertThrows(ModernWorldMap.MapException.class, () -> open.validateSpawns(List.of(cell(10, 10), cell(10, 10))));
        assertThrows(ModernWorldMap.MapException.class, () -> open.serverChosenSpawns(cell(10, 10), 101));
        byte[] blocked = blockedSectorBytes();
        setAttributes(blocked, 10, 10, (short) 1, (short) 0);
        setAttributes(blocked, 20, 20, (short) 1, (short) 0);
        var disconnected = map(blocked);
        assertThrows(ModernWorldMap.MapException.class, () -> disconnected.validateSpawns(List.of(cell(10, 10), cell(20, 20))));
        assertThrows(ModernWorldMap.MapException.class, () -> disconnected.serverChosenSpawns(cell(10, 10), 3));
    }

    @Test void malformedCountsTruncatedLayersAndUnknownSuffixNeverProduceGeometry() throws Exception {
        byte[] bytes = openSectorBytes();
        for (int length : new int[] {0, 32767, 32768, 32769, 49153, 49154, 49155, 49157, 49159, 49161, 49163, 49165, 49167, 49169}) {
            assertThrows(ModernWorldMap.MapException.class, () -> map(Arrays.copyOf(bytes, length)));
        }
        for (int offset : new int[] {LAYER4, 49158, 49162}) {
            byte[] invalid = bytes.clone(); ByteBuffer.wrap(invalid).order(ByteOrder.LITTLE_ENDIAN).putInt(offset, -1);
            assertThrows(ModernWorldMap.MapException.class, () -> map(invalid));
        }
        byte[] overlay = bytes.clone(); ByteBuffer.wrap(overlay).order(ByteOrder.LITTLE_ENDIAN).putShort(32768, (short) 65535);
        assertThrows(ModernWorldMap.MapException.class, () -> map(overlay));
        byte[] tail = Arrays.copyOf(bytes, bytes.length + 1);
        assertEquals(ModernWorldMap.Failure.UNSUPPORTED_S32, assertThrows(ModernWorldMap.MapException.class, () -> map(tail)).failure());
        // Suffixes may be absent only at exact complete L4/L5/L6/L7 boundaries.
        for (int length : new int[] {49158, 49162, 49166, 49168}) assertNotNull(map(Arrays.copyOf(bytes, length)));
    }

    @Test void cancellationAndExplicitByteSectorBudgetsFailClosed() throws Exception {
        assertEquals(ModernWorldMap.Failure.CANCELLED, assertThrows(ModernWorldMap.MapException.class,
                () -> ModernWorldMap.fromSectorBytes(514, Map.of(KEY, openSectorBytes()), () -> true)).failure());
        AtomicInteger checks = new AtomicInteger();
        assertEquals(ModernWorldMap.Failure.CANCELLED, assertThrows(ModernWorldMap.MapException.class,
                () -> ModernWorldMap.fromSectorBytes(514, Map.of(KEY, openSectorBytes()), () -> checks.incrementAndGet() > 100)).failure());
        assertEquals(ModernWorldMap.Failure.TOO_LARGE, assertThrows(ModernWorldMap.MapException.class,
                () -> map(new byte[ModernWorldMap.MAX_SECTOR_BYTES + 1])).failure());
        assertThrows(ModernWorldMap.MapException.class, () -> ModernWorldMap.fromSectorBytes(514, Map.of(), () -> false));
        var many = new HashMap<ModernWorldMap.SectorKey, byte[]>();
        for (int x = 0; x <= ModernWorldMap.MAX_SECTORS; x++) many.put(new ModernWorldMap.SectorKey(x, 0), openSectorBytes());
        assertEquals(ModernWorldMap.Failure.INVALID_SELECTION, assertThrows(ModernWorldMap.MapException.class,
                () -> ModernWorldMap.fromSectorBytes(514, many, () -> false)).failure());
    }

    @Test void loaderReadsOnlySelectedSharedSectorAndNeverFallsBackToLtOrAnotherExtension() throws Exception {
        Path directory = Files.createDirectories(temporary.resolve("map/514"));
        Files.write(directory.resolve(KEY.fileName()), openSectorBytes());
        var loaded = ModernWorldMap.load(temporary, 514, Set.of(KEY), () -> false);
        assertEquals(map(openSectorBytes()).manifest(), loaded.manifest());
        var missing = new ModernWorldMap.SectorKey(0x7ffe, 0x8000);
        Files.write(directory.resolve("7ffe8000.seg"), openSectorBytes());
        assertEquals(ModernWorldMap.Failure.MISSING_SECTOR, assertThrows(ModernWorldMap.MapException.class,
                () -> ModernWorldMap.load(temporary, 514, Set.of(missing), () -> false)).failure());
        assertEquals(ModernWorldMap.Failure.INVALID_PATH, assertThrows(ModernWorldMap.MapException.class,
                () -> ModernWorldMap.load(temporary.resolve("LTmap"), 514, Set.of(KEY), () -> false)).failure());
        assertThrows(ModernWorldMap.MapException.class, () -> ModernWorldMap.load(Path.of("relative"), 514, Set.of(KEY), () -> false));
        assertThrows(ModernWorldMap.MapException.class, () -> ModernWorldMap.load(temporary.resolve("../escape"), 514, Set.of(KEY), () -> false));
    }

    @Test void loaderRejectsSymlinkLeafWithoutFollowingIt() throws Exception {
        Path directory = Files.createDirectories(temporary.resolve("map/514"));
        Path source = Files.write(temporary.resolve("outside.s32"), openSectorBytes());
        try { Files.createSymbolicLink(directory.resolve(KEY.fileName()), source); }
        catch (UnsupportedOperationException | java.io.IOException denied) { Assumptions.abort("symlink creation unavailable in this test environment"); }
        assertEquals(ModernWorldMap.Failure.INVALID_PATH, assertThrows(ModernWorldMap.MapException.class,
                () -> ModernWorldMap.load(temporary, 514, Set.of(KEY), () -> false)).failure());
    }

    @Test void explicitlySelectedRealSharedSectorMatchesIndependentGeometryGolden() throws Exception {
        String root = System.getenv("LITO_SHARED_RESOURCE_ROOT");
        Assumptions.assumeTrue(root != null && !root.isBlank(), "explicit shared resource root required");
        var map = ModernWorldMap.load(Path.of(root), 514, Set.of(KEY), () -> false);
        assertEquals(137784, map.manifest().sectors().get(0).byteLength());
        assertEquals("ccfe52b614dfbdf97fd713f80d61e4cdd35e7c8c3000a06b4bba8868c96d1271", map.manifest().sectors().get(0).rawSha256());
        assertEquals("2feecc83de156a03c62e44effb7106067b24098a9ef7250cc471f7bf826da7a8", map.digest());
        int standing = 0;
        for (int y = 0; y < 64; y++) for (int x = 0; x < 64; x++) if (map.standing(cell(x, y))) standing++;
        assertEquals(986, standing);
        assertEquals(60, map.directionMask(cell(20, 16)));
        map.validateSpawns(List.of(cell(20, 16), cell(21, 16)));
    }

    private static byte[] withPortal(int x, int y) {
        byte[] bytes = Arrays.copyOf(openSectorBytes(), 49179);
        ByteBuffer out = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN); out.position(49166);
        out.putShort((short) 1).put((byte) 0).put((byte) (x * 2)).put((byte) y).putShort((short) 1).putInt(7).putShort((short) 0);
        return bytes;
    }
}
