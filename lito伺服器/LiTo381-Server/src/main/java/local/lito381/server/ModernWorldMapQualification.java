package local.lito381.server;

import java.nio.file.Path;
import java.util.Objects;
import java.util.Set;
import java.util.function.BooleanSupplier;
import java.util.stream.Collectors;

/** Exact administrator-supplied shared-map evidence, never a same-number native-map fallback. */
final class ModernWorldMapQualification {
    record Expected(ModernWorldBinding.Key logical, ModernWorldMap.Manifest manifest,
                    ModernWorldMap.Cell anchor, int capacity) {
        Expected {
            Objects.requireNonNull(logical); Objects.requireNonNull(manifest); Objects.requireNonNull(anchor);
            if (capacity < 1 || capacity > ModernWorldMap.MAX_SPAWNS || manifest.mapId() < 0 || manifest.mapId() > 65535
                    || manifest.sectors().isEmpty() || manifest.sectors().size() > ModernWorldMap.MAX_SECTORS
                    || !hash(manifest.digest())) throw invalid();
            long total = 0; int x = -1, y = -1;
            for (var sector : manifest.sectors()) {
                if (sector == null || sector.key() == null || sector.key().x() < x
                        || sector.key().x() == x && sector.key().y() <= y || sector.byteLength() < 49_158
                        || sector.byteLength() > ModernWorldMap.MAX_SECTOR_BYTES || !hash(sector.rawSha256())) throw invalid();
                total += sector.byteLength();
                if (total > ModernWorldMap.MAX_TOTAL_BYTES) throw invalid();
                x = sector.key().x(); y = sector.key().y();
            }
            if (!ModernWorldProtocol.contains(manifest, anchor)) throw invalid();
        }
        Set<ModernWorldMap.SectorKey> sectors() {
            return manifest.sectors().stream().map(ModernWorldMap.SectorManifest::key).collect(Collectors.toUnmodifiableSet());
        }
    }
    private ModernWorldMapQualification() {}

    /** Filesystem loading remains bounded and cancellation-aware, with the existing owned-leaf protections. */
    static ModernWorldBinding load(Path root, Expected expected, BooleanSupplier cancelled) throws ModernWorldMap.MapException {
        Objects.requireNonNull(expected); Objects.requireNonNull(cancelled);
        var map = ModernWorldMap.load(root, expected.manifest().mapId(), expected.sectors(), cancelled);
        var binding = qualify(map, expected);
        if (cancelled.getAsBoolean()) throw new ModernWorldMap.MapException(ModernWorldMap.Failure.CANCELLED);
        return binding;
    }

    /** Pure immutable validation. A shared geometry cache must call this for every logical binding. */
    static ModernWorldBinding qualify(ModernWorldMap map, Expected expected) throws ModernWorldMap.MapException {
        Objects.requireNonNull(map); Objects.requireNonNull(expected);
        if (!map.manifest().equals(expected.manifest()))
            throw new ModernWorldMap.MapException(ModernWorldMap.Failure.CHANGED_FILE);
        var spawns = map.serverChosenSpawns(expected.anchor(), expected.capacity());
        map.validateSpawns(spawns);
        return new ModernWorldBinding(expected.logical().mode(), expected.logical().logicalMapId(), map);
    }
    private static boolean hash(String value) {
        return value != null && value.matches("[0-9a-f]{64}") && !value.equals("0".repeat(64));
    }
    private static IllegalArgumentException invalid() { return new IllegalArgumentException("invalid shared-map qualification"); }
}
