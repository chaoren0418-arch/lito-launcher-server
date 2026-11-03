package local.lito381.server;

import java.nio.file.Path;
import java.util.List;
import java.util.Set;

/** Opt-in read-only geometry evidence, not ModernMain, a game, or native resource loading. */
public final class ModernWorldMapReadProbe {
    public static void main(String[] arguments) {
        try {
            if (arguments.length != 1) throw new IllegalArgumentException("absolute shared root required");
            var key = new ModernWorldMap.SectorKey(0x7fff, 0x8000);
            var map = ModernWorldMap.load(Path.of(arguments[0]), 514, Set.of(key), () -> false);
            var source = map.manifest().sectors().get(0);
            if (source.byteLength() != 137784 || !source.rawSha256().equals("ccfe52b614dfbdf97fd713f80d61e4cdd35e7c8c3000a06b4bba8868c96d1271")) {
                throw new IllegalStateException("candidate bytes changed");
            }
            if (!map.digest().equals("2feecc83de156a03c62e44effb7106067b24098a9ef7250cc471f7bf826da7a8")) {
                throw new IllegalStateException("candidate geometry changed");
            }
            var first = new ModernWorldMap.Cell(32724, 32784);
            var second = new ModernWorldMap.Cell(32725, 32784);
            map.validateSpawns(List.of(first, second));
            if (!map.canStep(first, second) || !map.canStep(second, first)) throw new IllegalStateException("candidate passage changed");
            var spawns = map.serverChosenSpawns(first, 100); map.validateSpawns(spawns);
            int standing = 0;
            for (int y = 0; y < 64; y++) for (int x = 0; x < 64; x++) {
                if (map.standing(new ModernWorldMap.Cell(key.origin().x() + x, key.origin().y() + y))) standing++;
            }
            if (standing != 986) throw new IllegalStateException("candidate standing cells changed");
            System.out.println("WORLD_MAP_CANDIDATE map=514 sector=7fff8000 bytes=" + source.byteLength()
                    + " rawSha256=" + source.rawSha256() + " manifest=" + map.digest()
                    + " standing=" + standing + " uniqueConnectedSpawns=" + spawns.size()
                    + " anchorX=" + first.x() + " anchorY=" + first.y() + " worldAvailable=false");
        } catch (Exception failure) {
            System.err.println("WORLD_MAP_CANDIDATE_FAILED");
            System.exit(2);
        }
    }
}
