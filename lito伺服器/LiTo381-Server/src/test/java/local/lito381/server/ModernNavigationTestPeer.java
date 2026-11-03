package local.lito381.server;

import java.io.IOException;
import java.net.InetAddress;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLSocket;

/** Real TLS + verifier + navigation authority; deliberately corrupt/drop exactly one committed reply. */
public final class ModernNavigationTestPeer {
    private ModernNavigationTestPeer() {}
    public static void main(String[] args) throws Exception {
        if (args.length != 4 || !"serve".equals(args[0])
                || !Set.of("wrong-source", "wrong-object", "wrong-action", "wrong-epoch", "wrong-channel", "denied-channel",
                        "silent-enter", "cancel-select", "cancel-enter", "wrong-leave-epoch", "wrong-leave-selected", "silent-leave", "cancel-leave",
                        "wrong-data-epoch", "wrong-data-object", "wrong-data-source", "silent-data", "cancel-data").contains(args[2])) {
            throw new IllegalArgumentException("serve <fixture-dir> <navigation-fault> <lifetime-ms>");
        }
        int lifetime = Integer.parseInt(args[3]);
        if (lifetime < 1000 || lifetime > 30_000) throw new IllegalArgumentException("bounded fixture lifetime required");
        var context = ModernTlsTestMaterial.serverContext(Path.of(args[1]), false);
        var repository = new ModernApplicationTestBackend("auth");
        var data = new ModernCharacterDataTestFixtures(false);
        var presence = new TournamentPresence();
        var executor = new ModernBackendExecutor(1, 1);
        boolean faultExercised = false;
        var watchdogFired = new AtomicBoolean();
        TournamentPresence.Selection committedSelection = null;
        try (executor; var listener = (SSLServerSocket) context.getServerSocketFactory().createServerSocket(
                0, 1, InetAddress.getByAddress(new byte[]{127, 0, 0, 1}))) {
            listener.setSoTimeout(5000);
            System.out.println("READY " + listener.getLocalPort()); System.out.flush();
            try (var socket = (SSLSocket) listener.accept();
                 var transport = new ModernTlsSessionTransport(socket, ModernSessionBoundary.Policy.current(),
                         new ModernTlsSessionTransport.Limits(2000, 2000), repository.verifier);
                 var gate = executor.openSession()) {
                transport.attachBackend(gate, 2000);
                try (var navigation = new ModernNavigationSession(transport, repository.characters, gate, 2000, presence)) {
                    var watchdog = Executors.newSingleThreadScheduledExecutor();
                    var stop = watchdog.schedule(() -> {
                        watchdogFired.set(true);
                        transport.failClosed();
                    }, lifetime, TimeUnit.MILLISECONDS);
                    try {
                        var negotiated = transport.negotiate();
                        navigation.negotiateRecords(negotiated.version() >= 5);
                        int previous = negotiated.requestId();
                        for (;;) {
                            try (var message = transport.readApplication(3000)) {
                                if (message == null) break;
                                if (Integer.compareUnsigned(message.requestId(), previous) <= 0) throw new IOException("fixture request order");
                                previous = message.requestId();
                                if (message instanceof ModernApplicationProtocol.Ping ping) {
                                    transport.writeApplication(new ModernApplicationProtocol.Pong(ping.requestId()));
                                } else if (message instanceof ModernApplicationProtocol.RecordRequest request) {
                                    // Current-version controller may read after SELECT.
                                    // This fault peer has no central SQL: preserve a true
                                    // Unavailable reply through production lease handling.
                                    transport.writeApplication(navigation.record(request));
                                } else if (message instanceof ModernApplicationProtocol.Auth auth) {
                                    var principal = transport.authenticateForApplication(auth.account(), auth.password());
                                    var sources = principal.sources().stream().sorted(Comparator.comparingInt(ModernAccountAuthentication.Source::id))
                                            .map(source -> new ModernApplicationProtocol.Source(source.id(), source.name())).toList();
                                    transport.writeApplication(new ModernApplicationProtocol.AuthResult(auth.requestId(), 0, principal.account(), sources));
                                } else if (message instanceof ModernApplicationProtocol.CharacterRequest request) {
                                    navigation.beforeCharacterRequest();
                                    var principal = transport.authenticatedPrincipal().orElseThrow();
                                    var rows = gate.call(2000, scope -> repository.characters.characters(principal, request.sourceId()));
                                    transport.writeApplication(new ModernApplicationProtocol.CharacterResult(request.requestId(), request.sourceId(), request.selectionEpoch(), 0, rows));
                                    navigation.recordRoster(request.sourceId(), request.selectionEpoch(), rows);
                                } else if (message instanceof ModernApplicationProtocol.CharacterDataRequest request) {
                                    // Enforce the actual selected context and finish the real
                                    // bounded backend read BEFORE deliberately damaging/dropping
                                    // its response. This is not a canned pre-selection packet.
                                    var result = navigation.characterData(request, data);
                                    if (result.status() != 0 || result.snapshot() == null || data.completedReads.get() == 0) {
                                        throw new IOException("fixture character data was not read");
                                    }
                                    if (Set.of("silent-data", "cancel-data").contains(args[2])) {
                                        if (presence.ownerCount() != 1 || presence.userCount() != 0) {
                                            throw new IOException("missing character data selection ownership");
                                        }
                                        var row = result.snapshot().character();
                                        String sourceName = transport.authenticatedPrincipal().orElseThrow().sources().stream()
                                                .filter(source -> source.id() == request.sourceId()).findFirst().orElseThrow().name();
                                        committedSelection = new TournamentPresence.Selection(
                                                new TournamentPresence.CharacterKey(result.sourceId(), row.objectId()), sourceName,
                                                row.name(), row.level(), row.characterClass(), row.sex());
                                        var contender = presence.connect();
                                        try {
                                            if (presence.select(contender, committedSelection) != TournamentPresence.SelectStatus.BUSY) {
                                                throw new IOException("character data selection is not reserved");
                                            }
                                        } finally { presence.disconnect(contender); }
                                        faultExercised = true;
                                        if (args[2].equals("cancel-data")) {
                                            // Data lookup finished inside the selected session;
                                            // the parent cancels while its response is withheld.
                                            System.out.println("COMMITTED 14"); System.out.flush();
                                        }
                                        long waitStarted = System.nanoTime();
                                        try (var unexpected = transport.readApplication(3000)) {
                                            if (unexpected != null) throw new IllegalStateException("client must close timed out data session");
                                        } catch (IOException resetByClient) { /* Client's bounded exchange closes the selected session. */ }
                                        if (TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - waitStarted) > 1500) {
                                            throw new IOException("client did not promptly close its timed out data session");
                                        }
                                        break;
                                    }
                                    if (Set.of("wrong-data-epoch", "wrong-data-object", "wrong-data-source").contains(args[2])) {
                                        int source = args[2].equals("wrong-data-source") ? (result.sourceId() == 7 ? 42 : 7) : result.sourceId();
                                        long object = args[2].equals("wrong-data-object") ? result.objectId() + 1 : result.objectId();
                                        long epoch = args[2].equals("wrong-data-epoch") ? result.selectionEpoch() ^ Long.MIN_VALUE : result.selectionEpoch();
                                        if (epoch == 0) epoch = 1;
                                        var prior = result.snapshot();
                                        var row = prior.character();
                                        var changed = new ModernCharacterData.Snapshot(source,
                                                new ModernApplicationProtocol.CharacterRow(object, row.name(), row.characterClass(), row.sex(), row.level()),
                                                prior.persisted(), prior.inventory());
                                        // Context and contained identity still form a valid frame;
                                        // the client must reject correlation with its own request.
                                        result = new ModernApplicationProtocol.CharacterDataResult(result.requestId(), source, object, epoch, 0, changed);
                                        faultExercised = true;
                                    }
                                    transport.writeApplication(result);
                                    if (faultExercised) break;
                                } else {
                                    var result = navigation.handle(message); // Real selection/channel lock is committed FIRST.
                                    if (result.status() != 0) throw new IOException("fixture navigation was not accepted");
                                    boolean selected = message instanceof ModernApplicationProtocol.CharacterSelection;
                                    boolean entered = message instanceof ModernApplicationProtocol.ChannelEnter;
                                    boolean left = message instanceof ModernApplicationProtocol.LobbyLeave;
                                    if ((entered && Set.of("silent-enter", "cancel-enter").contains(args[2]))
                                            || (selected && args[2].equals("cancel-select"))
                                            || (left && Set.of("silent-leave", "cancel-leave").contains(args[2]))) {
                                        if (presence.ownerCount() != 1 || presence.userCount() != (entered ? 1 : 0)) {
                                            throw new IOException("missing committed navigation ownership");
                                        }
                                        var row = result.selected();
                                        String sourceName = transport.authenticatedPrincipal().orElseThrow().sources().stream()
                                                .filter(source -> source.id() == rowSource(message)).findFirst().orElseThrow().name();
                                        committedSelection = new TournamentPresence.Selection(
                                                new TournamentPresence.CharacterKey(result.sourceId(), row.objectId()), sourceName,
                                                row.name(), row.level(), row.characterClass(), row.sex());
                                        var contender = presence.connect();
                                        try {
                                            if (presence.select(contender, committedSelection) != TournamentPresence.SelectStatus.BUSY) {
                                                throw new IOException("committed character is not actually reserved");
                                            }
                                        } finally { presence.disconnect(contender); }
                                        faultExercised = true;
                                        if (args[2].startsWith("cancel-")) {
                                            // Test-only out-of-band barrier: mutation is committed, but
                                            // no ACK has been written. Parent cancels only after this line.
                                            System.out.println("COMMITTED " + result.action()); System.out.flush();
                                        }
                                        long waitStarted = System.nanoTime();
                                        try (var unexpected = transport.readApplication(3000)) {
                                            if (unexpected != null) throw new IllegalStateException("client must close uncertain mutation");
                                        } catch (IOException resetByClient) { /* Actual client deadline aborts committed session. */ }
                                        if (TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - waitStarted) > 1500) {
                                            throw new IOException("client did not promptly close its uncertain mutation");
                                        }
                                        break;
                                    }
                                    if ((selected && Set.of("wrong-source", "wrong-object", "wrong-action", "wrong-epoch").contains(args[2]))
                                            || (entered && Set.of("wrong-channel", "denied-channel").contains(args[2]))
                                            || (left && Set.of("wrong-leave-epoch", "wrong-leave-selected").contains(args[2]))) {
                                        long epoch = Set.of("wrong-epoch", "wrong-leave-epoch").contains(args[2])
                                                ? result.selectionEpoch() ^ Long.MIN_VALUE : result.selectionEpoch();
                                        if (epoch == 0) epoch = 1;
                                        var row = result.selected();
                                        long object = result.objectId();
                                        if (args[2].equals("wrong-object")) {
                                            object++;
                                            row = new ModernApplicationProtocol.CharacterRow(object, row.name(), row.characterClass(), row.sex(), row.level());
                                        }
                                        if (args[2].equals("wrong-leave-selected")) {
                                            row = new ModernApplicationProtocol.CharacterRow(object, row.name() + " changed",
                                                    row.characterClass(), row.sex(), row.level());
                                        }
                                        boolean wrongAction = args[2].equals("wrong-action");
                                        result = new ModernApplicationProtocol.NavigationResult(result.requestId(), wrongAction ? 12 : result.action(),
                                                args[2].equals("denied-channel") ? 3 : result.status(), wrongAction ? 0 : result.phase(),
                                                wrongAction ? 255 : args[2].equals("wrong-channel") ? (result.channelId() + 1) % 4 : result.channelId(),
                                                args[2].equals("wrong-source") ? 42 : result.sourceId(), object, epoch,
                                                wrongAction ? null : row, wrongAction ? java.util.List.of() : result.channels());
                                        faultExercised = true;
                                    }
                                    transport.writeApplication(result);
                                    if (faultExercised) break;
                                }
                            }
                        }
                    } finally {
                        stop.cancel(false); watchdog.shutdownNow();
                        if (!watchdog.awaitTermination(1000, TimeUnit.MILLISECONDS)) throw new IOException("fixture timer did not retire");
                    }
                }
            }
        } finally {
            executor.close();
            if (!executor.awaitTermination(3000)) throw new IOException("fixture backend did not retire");
            if (presence.ownerCount() != 0 || presence.userCount() != 0) throw new IOException("fixture navigation ownership leaked");
        }
        if (watchdogFired.get()) throw new IOException("fixture watchdog cannot stand in for client cancellation");
        if (committedSelection != null) {
            var contender = presence.connect();
            try {
                if (presence.select(contender, committedSelection) != TournamentPresence.SelectStatus.ACCEPTED) {
                    throw new IOException("retired character reservation leaked");
                }
            } finally { presence.disconnect(contender); }
        }
        if (!faultExercised) throw new IOException("navigation fault was not exercised");
    }

    private static int rowSource(ModernApplicationProtocol.Message message) {
        if (message instanceof ModernApplicationProtocol.CharacterSelection selection) return selection.sourceId();
        if (message instanceof ModernApplicationProtocol.ChannelEnter enter) return enter.sourceId();
        if (message instanceof ModernApplicationProtocol.LobbyLeave leave) return leave.sourceId();
        throw new IllegalArgumentException("fixture expected selection context");
    }
}
