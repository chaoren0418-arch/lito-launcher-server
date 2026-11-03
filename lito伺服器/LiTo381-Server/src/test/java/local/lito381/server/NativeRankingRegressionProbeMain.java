package local.lito381.server;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Encrypted, isolated-SQL regression. No GUI acceptance and no production endpoint. */
public final class NativeRankingRegressionProbeMain {
    public static void main(String[] args) throws Exception {
        int port = Integer.parseInt(args[0]);
        Path output = Path.of(args[3]);
        String sha = HexFormat.of().withUpperCase().formatHex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(Path.of(Protocol.class.getProtectionDomain().getCodeSource().getLocation().toURI()))));
        if (!sha.equals(args[1]) || port < 1024 || port == 2000 || port == 55952)
            throw new IllegalStateException("owned candidate and isolated endpoint required");
        ServerConfig config = ServerConfig.load(Path.of(args[2]));
        if (!config.databaseReadOnly() || config.port() != port) throw new IllegalStateException("isolated read-only source config required");
        List<String> checks = new ArrayList<>();
        Map<String, RepositoryTournamentProfile> preserved = new LinkedHashMap<>();
        try (var routes = AccountRepository.open(config)) {
            for (String source : config.classicServerNames()) {
                var repo = routes.repositoryForClassicSource(source);
                var existing = repo.tournamentProfiles().profiles().stream().filter(p -> p.snapshotVersion() > 0).findFirst().orElseThrow();
                preserved.put(source, existing);
            }
            if (preserved.values().stream().map(RepositoryTournamentProfile::characterObjectId).distinct().count() != 1)
                throw new IllegalStateException("same object ID across sources required");
            if (!args[4].equals("baseline")) verifyDefaults(routes, config, output.getParent(), checks);
            String stage = "start";
            try {
                // Source 2 first matches the human failure; source 1 follows on another encrypted session.
                for (String source : List.of(config.classicServerNames().get(1), config.classicServerNames().get(0))) {
                    stage = source + ":ranking";
                    try (Peer login = new Peer("127.0.0.1", port)) {
                        login.authenticate(); login.await(Protocol.S_LITO_VS_SERVER_STATUS);
                        login.send(new Protocol.Writer().c(Protocol.C_LITO_LIN_CHARACTER_INFO).s(source).bytes());
                        login.await(Protocol.S_LITO_VS_SERVER_IP);
                        try (Peer game = new Peer("127.0.0.2", port)) {
                            game.authenticate(); game.await(Protocol.S_LITO_NEWS);
                            game.send(new Protocol.Writer().c(Protocol.C_LITO_READ_NEWS).bytes());
                            game.await(Protocol.S_LITO_CHARACTER_INFO);
                            game.send(new Protocol.Writer().c(Protocol.C_LITO_SELECT_CHAR).s("3332").bytes());
                            byte[] selected = game.await(Protocol.S_LITO_SELECT_CHAR_RESULT);
                            if (selected.length < 2 || selected[1] != 0) throw new IOException("selection rejected");
                            game.await(Protocol.S_ADD_INVENTORY_BATCH);
                            game.send(new Protocol.Writer().c(Protocol.C_LITO_ENTER_CHANNEL).c(0).bytes());
                            game.await(Protocol.S_LITO_ENTER_CHANNEL_RESULT); game.await(Protocol.S_LITO_CHANNEL_INFO);
                            for (int scope = -1; scope <= 4; scope++) {
                                Protocol.Writer ranking = new Protocol.Writer().c(Protocol.C_LITO_RANKING);
                                if (scope >= 0) ranking.c(scope);
                                game.send(ranking.bytes());
                                byte[] reply = game.await(Protocol.S_LITO_RANKING);
                                Protocol.Reader reader = new Protocol.Reader(reply); reader.c(); long count = reader.d();
                                if (count > 100) throw new IOException("invalid ranking row count");
                                checks.add(source + ":C_D7 selector=" + scope + " -> S_D9 rows=" + count);
                                game.send(new Protocol.Writer().c(Protocol.C_LITO_ROOM_LIST).bytes());
                                game.await(Protocol.S_LITO_ROOM_INFO);
                                checks.add(source + ":same session C_CE -> S_CC after ranking selector=" + scope);
                            }
                            game.send(new Protocol.Writer().c(Protocol.C_QUIT).bytes());
                        }
                    }
                    var refreshed = routes.repositoryForClassicSource(source).tournamentProfiles();
                    if (refreshed.profiles().stream().anyMatch(p -> p.snapshotVersion() == 0))
                        throw new IllegalStateException("missing source-correct roster initialization");
                    var before = preserved.get(source);
                    var after = refreshed.profiles().stream().filter(p -> p.characterObjectId() == before.characterObjectId()).findFirst().orElseThrow();
                    if (after.state().wins() != before.state().wins() || after.state().losses() != before.state().losses()
                            || after.state().disconnects() != before.state().disconnects()
                            || after.state().rankingPoints() != before.state().rankingPoints()
                            || !after.state().classicServerName().equals(source))
                        throw new IllegalStateException("authoritative record overwritten");
                    checks.add(source + ":existing SQL wins/losses/points/source preserved; full roster initialized");
                    if (source.equals(config.classicServerNames().get(1))) {
                        var untouched = routes.repositoryForClassicSource(config.classicServerNames().get(0)).tournamentProfiles().profiles().stream().filter(p -> p.characterObjectId() == before.characterObjectId()).findFirst().orElseThrow();
                        if (!untouched.equals(preserved.get(config.classicServerNames().get(0))))
                            throw new IllegalStateException("other-source same object ID changed");
                        checks.add("source2 ranking leaves source1 same-object-ID full SQL snapshot/version byte-equivalent");
                    }
                }
                var one = routes.repositoryForClassicSource(config.classicServerNames().get(0));
                var two = routes.repositoryForClassicSource(config.classicServerNames().get(1));
                var a = one.tournamentProfiles().profiles().stream().filter(p -> p.characterObjectId() == preserved.values().iterator().next().characterObjectId()).findFirst().orElseThrow();
                var b = two.tournamentProfiles().profiles().stream().filter(p -> p.characterObjectId() == a.characterObjectId()).findFirst().orElseThrow();
                try { one.persistTournamentProfile(a.characterObjectId(), a.characterName(), a, b.state()); throw new IllegalStateException("cross-source write accepted"); }
                catch (IllegalArgumentException expected) { checks.add("cross-source replacement rejected before SQL mutation"); }
                if (one.persistTournamentProfile(a.characterObjectId(), a.characterName(), null, a.state()).status() != RepositoryTournamentMutationStatus.CONFLICT)
                    throw new IllegalStateException("stale CAS accepted");
                if (!one.tournamentProfiles().profiles().stream().filter(p -> p.characterObjectId() == a.characterObjectId()).findFirst().orElseThrow().equals(a))
                    throw new IllegalStateException("negative CAS changed snapshot");
                checks.add("stale CAS rejected; existing snapshot/version unchanged");
                write(output, "passed", sha, checks, "");
            } catch (Exception failure) {
                write(output, args[4].equals("baseline") ? "baseline_failed_as_expected" : "failed", sha, checks, stage + ":" + failure.getClass().getSimpleName());
                if (!args[4].equals("baseline")) throw failure;
            }
        }
    }

    private static void verifyDefaults(AccountRepository routes, ServerConfig config, Path out, List<String> checks) throws Exception {
        String source = config.classicServerNames().get(1);
        var roster = routes.repositoryForClassicSource(source).tournamentProfiles().profiles();
        var old = roster.stream().filter(p -> p.snapshotVersion() > 0).findFirst().orElseThrow();
        var fresh = roster.stream().filter(p -> p.snapshotVersion() == 0).findFirst().orElseThrow();
        GameStateStore state = new GameStateStore(out.resolve("not-created-default.state"), false);
        String key = GameStateStore.sourceCharacter(source, old.characterName());
        state.reconcileTournamentProfileStates(Map.of(key, old.state()));
        state.ranking(Protocol.LitoRankingScope.OVERALL, null, 0);
        var fullSnapshot = state.tournamentProfileState(key);
        if (!fullSnapshot.rankingSnapshotPresent()) throw new IllegalStateException("existing snapshot required");
        String next = GameStateStore.sourceCharacter(source, fresh.characterName());
        state.initializeTournamentRosterDefaults(Map.of(next, fresh.state(), key, fresh.state()));
        if (!state.tournamentProfileState(key).equals(fullSnapshot) || !state.tournamentProfileState(next).equals(withBoundaries(fresh.state(), state.tournamentProfileState(next))))
            throw new IllegalStateException("defaults changed existing snapshot or failed source initialization");
        var newState = state.tournamentProfileState(next);
        state.initializeTournamentRosterDefaults(Map.of(next, fresh.state()));
        if (!state.tournamentProfileState(next).equals(newState)) throw new IllegalStateException("default initialization not idempotent");
        try { state.initializeTournamentRosterDefaults(Map.of(GameStateStore.sourceCharacter(config.classicServerNames().get(0), fresh.characterName()), fresh.state())); throw new IllegalStateException("mismatched default accepted"); }
        catch (IllegalArgumentException expected) { }
        if (!state.tournamentProfileState(key).equals(fullSnapshot)) throw new IllegalStateException("negative default changed existing snapshot");
        checks.add("default initialization: source-correct/idempotent; existing full snapshot and global boundaries preserved; wrong-source rejected");
    }
    // Global snapshot boundaries belong to the store, not to a new version-zero roster row.
    private static GameStateStore.TournamentProfileState withBoundaries(GameStateStore.TournamentProfileState expected, GameStateStore.TournamentProfileState actual) throws Exception {
        var components = GameStateStore.TournamentProfileState.class.getRecordComponents();
        Object[] values = new Object[components.length]; Class<?>[] types = new Class<?>[components.length];
        for (int i = 0; i < components.length; i++) { types[i] = components[i].getType(); values[i] = components[i].getAccessor().invoke(components[i].getName().endsWith("BoundaryEpochSeconds") ? actual : expected); }
        var ctor = GameStateStore.TournamentProfileState.class.getDeclaredConstructor(types); ctor.setAccessible(true);
        return ctor.newInstance(values);
    }
    private static final class Peer implements AutoCloseable {
        final Socket socket = new Socket(); final Protocol.CipherState incoming, outgoing;
        Peer(String host, int port) throws Exception {
            socket.connect(new InetSocketAddress(host, port), 3000); socket.setSoTimeout(15000); socket.setTcpNoDelay(true);
            byte[] key = Protocol.readFrame(socket.getInputStream(), 8192);
            if (key == null || key.length != 5 || (key[0] & 255) != Protocol.S_KEY) throw new IOException("invalid S_KEY");
            var read = new Protocol.Reader(key); read.c(); int seed = (int) read.d();
            incoming = new Protocol.CipherState(seed); outgoing = new Protocol.CipherState(seed);
        }
        void send(byte[] bytes) throws IOException { Protocol.writeFrame(socket.getOutputStream(), outgoing.encrypt(bytes)); }
        byte[] await(int opcode) throws IOException {
            for (int i = 0; i < 200; i++) { byte[] frame = Protocol.readFrame(socket.getInputStream(), 65535); if (frame == null) throw new IOException("session closed before expected response"); byte[] payload = incoming.decrypt(frame); if (payload.length > 0 && (payload[0] & 255) == opcode) return payload; }
            throw new IOException("response frame limit");
        }
        void authenticate() throws IOException {
            send(new Protocol.Writer().c(Protocol.C_VERSION).h(Protocol.VERSION_MAGIC).d(Protocol.VERSION_PRODUCT).c(2).h(Protocol.VERSION_BUILD).bytes());
            byte[] version = await(Protocol.S_VERSION_CHECK); if (version.length < 2 || version[1] != 0) throw new IOException("version rejected");
            send(new Protocol.Writer().c(Protocol.C_LOGIN).s("3333").s("3333").d(0x0100007fL).bytes());
        }
        public void close() throws IOException { socket.close(); }
    }
    private static String quote(String text) { return "\"" + text.replace("\\", "\\\\").replace("\"", "\\\"") + "\""; }
    private static void write(Path file, String state, String sha, List<String> checks, String failure) throws IOException {
        Files.writeString(file, "{\"status\":" + quote(state) + ",\"sha256\":" + quote(sha) + ",\"gui_acceptance\":false,\"checks\":[" + String.join(",", checks.stream().map(NativeRankingRegressionProbeMain::quote).toList()) + "],\"failure\":" + quote(failure) + "}", StandardCharsets.UTF_8);
    }
}
