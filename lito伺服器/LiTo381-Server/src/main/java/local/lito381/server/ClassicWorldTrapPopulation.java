package local.lito381.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.zip.GZIPInputStream;

/** Selected 380_db WorldTrap population supported by ordinary classic WORLD. */
final class ClassicWorldTrapPopulation {
    static final String RESOURCE = "/world/classic-world-traps.lwt.gz";
    static final String SOURCE_SHA256 =
            "EA25D9EEB2670113F3AB04AABE465944E808F73B20D6AA26637D9E026E13FFB7";
    static final int DEFINITION_COUNT = 190;
    static final int POPULATION = 1_322;
    static final int DETECTIONABLE_POPULATION = 278;
    private static final long DEFAULT_SEED = 0x4C_31_54_52_41_50L;

    record Definition(
            int spawnId, int trapId, int mapId, int x, int y,
            int randomX, int randomY, int count, int spanMillis,
            int gfxId, boolean detectionable) {
        Definition {
            if (spawnId <= 0 || trapId <= 0 || mapId < 0 || x <= 0 || y <= 0
                    || randomX < 0 || randomY < 0 || count <= 0
                    || spanMillis < 0 || gfxId < 0 || gfxId > 0xFFFF) {
                throw new IllegalArgumentException("invalid classic trap definition");
            }
        }
    }

    record Snapshot(
            int spawnId, int spawnNumber, int trapId, int mapId,
            int x, int y, int gfxId, boolean detectionable,
            boolean enabled, long enabledAtNanos) {}

    record Detection(List<Snapshot> activated, List<Snapshot> effects) {
        Detection {
            activated = List.copyOf(activated);
            effects = List.copyOf(effects);
        }
    }

    private static final class Trap {
        final Definition definition;
        final int spawnNumber;
        final Random random;
        int x;
        int y;
        boolean enabled = true;
        long enabledAtNanos;

        Trap(Definition definition, int spawnNumber, long seed) {
            this.definition = definition;
            this.spawnNumber = spawnNumber;
            random = new Random(seed);
            resetLocation();
        }

        void resetLocation() {
            x = definition.x();
            y = definition.y();
            if (definition.randomX() == 0 && definition.randomY() == 0) return;
            ClassicWorldMapCatalog.ClassicMap map =
                    ClassicWorldMapCatalog.forMap(definition.mapId());
            for (int attempt = 0; attempt < 50; attempt++) {
                int candidateX = definition.x()
                        + random.nextInt(definition.randomX() + 1)
                            * (random.nextBoolean() ? 1 : -1);
                int candidateY = definition.y()
                        + random.nextInt(definition.randomY() + 1)
                            * (random.nextBoolean() ? 1 : -1);
                if (map.contains(candidateX, candidateY)
                        && map.standingPassable(candidateX, candidateY)) {
                    x = candidateX;
                    y = candidateY;
                    return;
                }
            }
        }

        Snapshot snapshot() {
            return new Snapshot(
                    definition.spawnId(), spawnNumber, definition.trapId(),
                    definition.mapId(), x, y, definition.gfxId(),
                    definition.detectionable(), enabled, enabledAtNanos);
        }
    }

    private static final List<Definition> DEFINITIONS = load();
    private final List<Trap> traps = new ArrayList<>(POPULATION);

    ClassicWorldTrapPopulation() {
        this(DEFAULT_SEED);
    }

    ClassicWorldTrapPopulation(long seed) {
        int ordinal = 0;
        for (Definition definition : DEFINITIONS) {
            for (int spawnNumber = 1;
                 spawnNumber <= definition.count(); spawnNumber++) {
                traps.add(new Trap(definition, spawnNumber,
                        mix64(seed ^ ++ordinal
                                ^ Integer.toUnsignedLong(definition.spawnId())
                                    * 0x9E3779B97F4A7C15L)));
            }
        }
        if (traps.size() != POPULATION) {
            throw new ExceptionInInitializerError(
                    "classic trap population mismatch " + traps.size());
        }
    }

    synchronized Detection detect(int mapId, int x, int y, long nowNanos) {
        advance(nowNanos);
        List<Snapshot> activated = new ArrayList<>();
        List<Snapshot> effects = new ArrayList<>();
        for (Trap trap : traps) {
            if (!trap.enabled || trap.definition.mapId() != mapId
                    || !BattleSpellCatalog.isInNativeScreen(
                            x, y, trap.x, trap.y)) continue;
            trap.enabled = false;
            trap.enabledAtNanos = trap.definition.spanMillis() > 0
                    ? nowNanos + TimeUnit.MILLISECONDS.toNanos(
                            trap.definition.spanMillis()) : 0;
            Snapshot snapshot = trap.snapshot();
            activated.add(snapshot);
            if (snapshot.detectionable() && snapshot.gfxId() != 0) {
                effects.add(snapshot);
            }
        }
        return new Detection(activated, effects);
    }

    synchronized List<Snapshot> inScreen(
            int mapId, int x, int y, long nowNanos) {
        advance(nowNanos);
        return traps.stream()
                .filter(trap -> trap.definition.mapId() == mapId)
                .map(Trap::snapshot)
                .filter(trap -> BattleSpellCatalog.isInNativeScreen(
                        x, y, trap.x(), trap.y()))
                .toList();
    }

    synchronized List<Snapshot> snapshots(long nowNanos) {
        advance(nowNanos);
        return traps.stream().map(Trap::snapshot).toList();
    }

    private void advance(long nowNanos) {
        for (Trap trap : traps) {
            if (!trap.enabled && trap.enabledAtNanos > 0
                    && nowNanos - trap.enabledAtNanos >= 0) {
                trap.enabled = true;
                trap.enabledAtNanos = 0;
                trap.resetLocation();
            }
        }
    }

    static List<Definition> definitions() {
        return DEFINITIONS;
    }

    private static List<Definition> load() {
        InputStream resource = ClassicWorldTrapPopulation.class
                .getResourceAsStream(RESOURCE);
        if (resource == null) {
            throw new ExceptionInInitializerError("missing " + RESOURCE);
        }
        try (GZIPInputStream unpacked = new GZIPInputStream(resource)) {
            byte[] canonical = unpacked.readAllBytes();
            String actual = sha256(canonical);
            if (!SOURCE_SHA256.equals(actual)) {
                throw new IOException("classic trap source hash mismatch " + actual);
            }
            List<Definition> result = new ArrayList<>();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                    new java.io.ByteArrayInputStream(canonical),
                    StandardCharsets.UTF_8))) {
                String header = reader.readLine();
                if (header == null || !header.startsWith("spawn_id\ttrap_id\t")) {
                    throw new IOException("invalid classic trap header");
                }
                for (String line; (line = reader.readLine()) != null;) {
                    if (line.isBlank()) continue;
                    String[] field = line.split("\\t", -1);
                    if (field.length != 11) {
                        throw new IOException("invalid classic trap row");
                    }
                    result.add(new Definition(
                            Integer.parseInt(field[0]), Integer.parseInt(field[1]),
                            Integer.parseInt(field[2]), Integer.parseInt(field[3]),
                            Integer.parseInt(field[4]), Integer.parseInt(field[5]),
                            Integer.parseInt(field[6]), Integer.parseInt(field[7]),
                            Integer.parseInt(field[8]), Integer.parseInt(field[9]),
                            Integer.parseInt(field[10]) != 0));
                }
            }
            if (result.size() != DEFINITION_COUNT) {
                throw new IOException(
                        "classic trap definition count " + result.size());
            }
            return List.copyOf(result);
        } catch (IOException | RuntimeException error) {
            throw new ExceptionInInitializerError(error);
        }
    }

    private static String sha256(byte[] value) {
        try {
            return HexFormat.of().withUpperCase().formatHex(
                    MessageDigest.getInstance("SHA-256").digest(value));
        } catch (NoSuchAlgorithmException impossible) {
            throw new ExceptionInInitializerError(impossible);
        }
    }

    private static long mix64(long value) {
        value = (value ^ (value >>> 30)) * 0xBF58476D1CE4E5B9L;
        value = (value ^ (value >>> 27)) * 0x94D049BB133111EBL;
        return value ^ (value >>> 31);
    }
}
