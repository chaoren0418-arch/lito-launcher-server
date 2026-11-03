package local.lito381.server;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

/** Shared554 candidate proof, not a production mapping or original LT554 equivalence. */
class ModernQuestMapQualificationTest {
    static final ModernWorldMap.Cell ANCHOR = new ModernWorldMap.Cell(32778, 32823);
    static final List<ModernWorldMap.SectorManifest> PINS = List.of(
            pin(0x7fff, 0x8000, 62681, "0217a00231ccca7c07f1a8d9cff86ae3de319108e71a7289d4acf65181268510"),
            pin(0x7fff, 0x8001, 76468, "6470ce783beecf047103076114912a787861d2998219bf4af1b2ea9a2a31c120"),
            pin(0x8000, 0x8000, 75400, "945b9ff592e28f10a58e26f3fa8053bcc566ad8cf9c255ab6117e6fde52a6537"),
            pin(0x8000, 0x8001, 64287, "a91f86d5017d1c306f8ec6a545f333918b8d22befec00c287ffcb76b6986f8ae"));
    private static final int[] DX = {0, 1, 1, 1, 0, -1, -1, -1}, DY = {-1, -1, 0, 1, 1, 1, 0, -1};

    private static ModernWorldMap.SectorManifest pin(int x, int y, int bytes, String sha) {
        return new ModernWorldMap.SectorManifest(new ModernWorldMap.SectorKey(x, y), bytes, sha);
    }
    private static Path root() {
        var text = System.getenv("LITO_SHARED_RESOURCE_ROOT");
        Assumptions.assumeTrue(text != null && !text.isBlank(), "explicit shared resource root required");
        return Path.of(text);
    }
    static ModernWorldMap shared() throws Exception {
        var map = ModernWorldMap.load(root(), 554, PINS.stream().map(ModernWorldMap.SectorManifest::key).collect(Collectors.toSet()), () -> false);
        assertEquals(PINS, map.manifest().sectors()); return map;
    }
    private static Set<ModernWorldMap.Cell> reachable(ModernWorldMap map) {
        var seen = new HashSet<ModernWorldMap.Cell>(); var queue = new ArrayDeque<ModernWorldMap.Cell>();
        seen.add(ANCHOR); queue.add(ANCHOR);
        while (!queue.isEmpty()) {
            var from = queue.remove();
            for (int h = 0; h < 8; h++) {
                var to = new ModernWorldMap.Cell(from.x() + DX[h], from.y() + DY[h]);
                if (map.canStep(from, to) && seen.add(to)) queue.add(to);
            }
            assertTrue(seen.size() <= 4 * 4096);
        }
        return Set.copyOf(seen);
    }

    @Test void completeShared554HasIndependentJavaAndRustDirectionAndManifestAgreement() throws Exception {
        try (var entries = Files.list(root().resolve("map/554"))) {
            assertEquals(PINS.stream().map(p -> p.key().fileName()).collect(Collectors.toSet()),
                    entries.peek(p -> assertTrue(Files.isRegularFile(p, java.nio.file.LinkOption.NOFOLLOW_LINKS)))
                            .map(p -> p.getFileName().toString()).collect(Collectors.toSet()));
        }
        var map = shared();
        assertEquals("fa44590de41cc8fd77a13fbf0cb1dbbed50e3726a27a2a2aa92fe7be717a29c3", map.digest());
        var digest = MessageDigest.getInstance("SHA-256"); int standing = 0, cross = 0;
        for (var bound : map.sectorBounds()) {
            for (int y = 0; y < 64; y++) for (int x = 0; x < 64; x++) {
                var cell = new ModernWorldMap.Cell(bound.origin().x() + x, bound.origin().y() + y);
                digest.update((byte) map.directionMask(cell)); if (map.standing(cell)) standing++;
                for (int h = 0; h < 8; h++) {
                    var next = new ModernWorldMap.Cell(cell.x() + DX[h], cell.y() + DY[h]);
                    if (map.canStep(cell, next) && (next.x() < bound.origin().x() || next.x() >= bound.origin().x() + 64
                            || next.y() < bound.origin().y() || next.y() >= bound.origin().y() + 64)) cross++;
                }
            }
        }
        var direction = HexFormat.of().formatHex(digest.digest());
        assertEquals("86abc1c5961f6549a6294d077f1fe9e71466697b45b22a69404799259e54eeb6", direction);
        assertEquals(979, standing); assertEquals(0, cross); assertEquals(165, reachable(map).size());
        var candidates = map.serverChosenSpawns(ANCHOR, 100); map.validateSpawns(candidates);
        assertEquals(100, candidates.size());
        System.out.println("shared554-java candidate=true runtimeQuest=false canonical=" + map.digest()
                + " directions=" + direction + " standing=" + standing + " reachable=165 crossSector=0");
    }

    @Test void independentSharedMapRejectsUnreachableNpcEvenWhenThatCellIsStanding() throws Exception {
        var map = shared(); var candidates = map.serverChosenSpawns(ANCHOR, 100); var connected = reachable(map);
        var remote = map.teleportCells().stream().filter(c -> !connected.contains(c)).findFirst().orElseThrow();
        assertTrue(map.standing(remote));
        var scope = ModernNpcRosterTest.SCOPE;
        try (var roster = new ModernNpcRoster(scope, map, List.of(ANCHOR, candidates.get(1)), 20, 3000)) {
            var before = roster.snapshot();
            var spawn = new ModernNpcRoster.Spawn(ModernNpcRosterTest.template(), 0, 1, remote, 0);
            assertThrows(IllegalArgumentException.class, () -> roster.spawn(scope, List.of(spawn), Set.of()));
            assertEquals(before, roster.snapshot());
        }
    }

    @Test void serverNpcActuallyPursuesAndRetiresOnShared554WithoutPlayerOrNativeBinSubstitution() throws Exception {
        var map = shared(); var candidates = map.serverChosenSpawns(ANCHOR, 100); var scope = ModernNpcRosterTest.SCOPE;
        var target = new ModernNpcRoster.Target(ModernParticipantId.human(42, 3333), 0, candidates.get(99), true);
        try (var roster = new ModernNpcRoster(scope, map, List.of(ANCHOR, candidates.get(1)), 20, 3000)) {
            var npc = roster.spawn(scope, List.of(new ModernNpcRoster.Spawn(ModernNpcRosterTest.template(), 0, 1, ANCHOR, 0)), Set.of(target.cell())).get(0);
            int moves = 0; boolean inRange = false;
            for (int tick = 1; tick <= 165; tick++) {
                roster.advance(scope, tick * 100);
                var plan = roster.pursue(scope, npc.id(), npc.life(), List.of(target), Set.of(target.cell()), 4096, () -> false);
                assertTrue(plan.visited() <= 165);
                if (plan.kind() == ModernNpcRoster.PlanKind.IN_RANGE) { inRange = true; break; }
                assertEquals(ModernNpcRoster.PlanKind.MOVE, plan.kind());
                var old = npc; npc = roster.move(scope, plan, Set.of(target.cell())); moves++;
                assertTrue(map.canStep(old.cell(), npc.cell())); assertNotEquals(target.cell(), npc.cell());
                assertEquals(scope, npc.id().scope()); assertEquals(1, npc.life());
            }
            assertTrue(inRange); assertTrue(moves > 0); assertEquals(280, npc.hp(), "in-range AI proposal is not damage authority");
            var corpse = roster.damage(scope, npc.id(), 1, Long.MAX_VALUE);
            assertFalse(corpse.alive()); roster.advance(scope, corpse.corpseUntilMillis());
            assertTrue(roster.snapshot().actors().isEmpty());
            System.out.println("shared554-java npcMoves=" + moves + " actorSerial=" + npc.id().serial()
                    + " inRangeProposal=true corpseRetired=true worldWire=false playerAttack=false");
        }
    }
}
