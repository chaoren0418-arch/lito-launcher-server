package local.lito381.server;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import javax.net.ssl.SSLContext;

/**
 * Opt-in Rust peer: production TLS listener, real Navigation/Rooms/WorldService,
 * strict shared S32 bytes, synthetic source-scoped read-only repositories, no DB.
 * serve <TLS fixture directory> <normal|changed|disconnect> <lifetime ms>
 * LITO_SHARED_RESOURCE_ROOT is mandatory. Exactly two clients use sources 7/42,
 * object 3333, a mode-1/map-514 room with allow-upper-tier attribute 16.
 * Only READY/port is printed; no account, character, backend cause or path output.
 */
public final class ModernWorldTestPeer {
    static final ModernWorldMap.Cell ANCHOR = new ModernWorldMap.Cell(32724, 32784);
    static final String RAW_SHA = "ccfe52b614dfbdf97fd713f80d61e4cdd35e7c8c3000a06b4bba8868c96d1271";
    static final String MANIFEST_SHA = "2feecc83de156a03c62e44effb7106067b24098a9ef7250cc471f7bf826da7a8";
    static final int CAPABILITIES = 111; // I64/source/navigation/data/rooms/world; no equipment authority.
    private ModernWorldTestPeer() {}

    public static void main(String[] args) {
        try {
            if (args.length != 4 || !args[0].equals("serve") || !Set.of("normal", "changed", "disconnect").contains(args[2]))
                throw new IllegalArgumentException("invalid bounded world peer request");
            int lifetime = Integer.parseInt(args[3]);
            if (lifetime < 5_000 || lifetime > 60_000) throw new IllegalArgumentException("invalid bounded world peer lifetime");
            String root = System.getenv("LITO_SHARED_RESOURCE_ROOT");
            if (root == null || root.isBlank()) throw new IllegalArgumentException("explicit shared resource root required");
            ModernWorldMap map = sharedMap(Path.of(root));
            try (var harness = new Harness(ModernTlsTestMaterial.serverContext(Path.of(args[1]), false), map, args[2])) {
                System.out.println("READY " + harness.server.address().getPort()); System.out.flush();
                long deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(lifetime);
                for (;;) {
                    var state = harness.server.snapshot();
                    if (state.accepted() == 2 && state.sessions() == 0) break;
                    if (state.accepted() > 2 || harness.server.listenerFailed() || System.nanoTime() - deadline >= 0)
                        throw new IllegalStateException("world peer flow incomplete");
                    Thread.sleep(10);
                }
                int sun = harness.sunReads.get(), moon = harness.moonReads.get();
                boolean disconnected = args[2].equals("disconnect");
                if (harness.fixture.authenticationCalls.get() != 4 || sun < 2 || moon < 2
                        || (disconnected ? Math.max(sun, moon) < 3 : Math.min(sun, moon) < 3)
                        || harness.server.snapshot().failed() > (disconnected ? 2 : 0))
                    throw new IllegalStateException("world peer did not exercise authenticated qualification");
            }
        } catch (Exception failure) {
            System.err.println("MODERN_WORLD_PEER_FAILED"); System.exit(2);
        }
    }

    static ModernWorldMap sharedMap(Path root) throws Exception {
        if (root == null || !root.isAbsolute()) throw new IllegalArgumentException("absolute shared root required");
        var map = ModernWorldMap.load(root, 514, Set.of(new ModernWorldMap.SectorKey(0x7fff, 0x8000)), () -> false);
        var sector = map.manifest().sectors().get(0);
        if (sector.byteLength() != 137_784 || !sector.rawSha256().equals(RAW_SHA) || !map.digest().equals(MANIFEST_SHA)
                || !map.canStep(ANCHOR, new ModernWorldMap.Cell(32725, 32784)))
            throw new IllegalStateException("fixed shared world candidate changed");
        map.validateSpawns(map.serverChosenSpawns(ANCHOR, 2));
        return map;
    }

    static final class Harness implements AutoCloseable {
        final TournamentPresence presence = new TournamentPresence();
        final ModernRooms rooms = new ModernRooms(Set.of(new ModernWorldBinding.Key(1, 514)));
        final ModernApplicationTestBackend fixture = new ModernApplicationTestBackend("auth");
        final AtomicInteger sunReads = new AtomicInteger(), moonReads = new AtomicInteger();
        final AtomicBoolean changeFresh = new AtomicBoolean();
        final ModernWorldMap map;
        final ModernWorldService worlds;
        final ModernServer server;

        Harness(SSLContext tls, ModernWorldMap map, String mode) throws Exception {
            if (!Set.of("normal", "changed", "disconnect").contains(mode)) throw new IllegalArgumentException("unknown fixture mode");
            this.map = map; changeFresh.set(mode.equals("changed"));
            var binding = new ModernWorldBinding(1, 514, map);
            worlds = new ModernWorldService(rooms, Map.of(binding.key(), new ModernWorldService.Mapping(binding, ANCHOR,
                    TimeUnit.MILLISECONDS.toNanos(200))), new ModernWorldService.Limits(4, 2, 2, 3_000, 50,
                    new ModernBattleEngine.Limits(10_000, TimeUnit.SECONDS.toNanos(3), 30_000)));
            ModernCharacterRepository characters = (principal, source) -> {
                if (!principal.authorizesSource(source) || source != 7 && source != 42
                        || !(source == 7 ? ModernApplicationTestBackend.SUN_ACCOUNT : ModernApplicationTestBackend.MOON_ACCOUNT)
                                .equals(principal.accountForSource(source))) throw new SecurityException("fixture source qualification failed");
                // This calls the real concrete-source adapter, not a constant
                // character response and not an injected fake session principal.
                List<ModernApplicationProtocol.CharacterRow> rows = fixture.characters.characters(principal, source);
                int reads = (source == 7 ? sunReads : moonReads).incrementAndGet();
                if (source == 7 && reads >= 3 && changeFresh.get()) return rows.stream().map(row ->
                        new ModernApplicationProtocol.CharacterRow(row.objectId(), "Changed Sun Knight", row.characterClass(), row.sex(), row.level())).toList();
                return rows;
            };
            boolean transferred = false;
            try {
                server = new ModernServer(new InetSocketAddress(InetAddress.getByAddress(new byte[]{127, 0, 0, 1}), 0), tls,
                        new ModernServer.Limits(4, 2, 600, 4, 2, 4, 2_000, 2_000, 2_000, 10_000),
                        fixture.verifier, characters, presence, ModernCharacterDataRepository.unavailable(), null, null, rooms, worlds);
                transferred = true;
            } finally {
                if (!transferred) { worlds.close(); if (!worlds.awaitTermination(3_000)) throw new IllegalStateException("failed peer world owner retained"); }
            }
        }
        @Override public void close() throws Exception {
            server.close();
            if (!server.awaitTermination(5_000) || presence.ownerCount() != 0 || presence.userCount() != 0
                    || rooms.ownerCount() != 0 || rooms.roomCount() != 0 || rooms.memberCount() != 0
                    || worlds.registrationCount() != 0 || !worlds.isQuiescent())
                throw new IllegalStateException("world peer ownership did not retire");
        }
    }
}
