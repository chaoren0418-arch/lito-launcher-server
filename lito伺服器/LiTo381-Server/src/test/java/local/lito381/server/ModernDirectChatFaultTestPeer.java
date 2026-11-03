package local.lito381.server;

import java.io.IOException;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLSocket;

/** Real TLS/navigation/rooms/private authority. Only the second committed mutation's
 * reply is withheld; no production fault hook, database or resource pack. */
public final class ModernDirectChatFaultTestPeer {
    private ModernDirectChatFaultTestPeer() {}

    public static void main(String[] args) throws Exception {
        if (args.length != 4 || !args[0].equals("serve")
                || !Set.of("silent-lobby", "silent-room", "cancel-lobby", "cancel-room", "prefs-lobby", "block-room").contains(args[2]))
            throw new IllegalArgumentException("chat fault peer arguments");
        int lifetime = Integer.parseInt(args[3]);
        if (lifetime < 1000 || lifetime > 30000) throw new IllegalArgumentException("bounded lifetime required");
        var backend = new ModernApplicationTestBackend("auth");
        var presence = new TournamentPresence();
        var rooms = new ModernRooms(Set.of(), Set.of(), java.util.Map.of(), new ModernChat(() -> 0));
        var executor = new ModernBackendExecutor(1, 2);
        var watchdog = Executors.newSingleThreadScheduledExecutor();
        var watchdogFired = new AtomicBoolean();
        int committed = 0;
        boolean clientClosed = false;
        try (executor; var listener = (SSLServerSocket) ModernTlsTestMaterial.serverContext(Path.of(args[1]), false)
                .getServerSocketFactory().createServerSocket(0, 2, InetAddress.getByAddress(new byte[]{127, 0, 0, 1}))) {
            listener.setSoTimeout(5000);
            System.out.println("READY " + listener.getLocalPort()); System.out.flush();
            try (var socket = (SSLSocket) listener.accept();
                 var transport = new ModernTlsSessionTransport(socket, ModernSessionBoundary.Policy.rooms(false).withDirectChat(),
                         new ModernTlsSessionTransport.Limits(2000, 2000), backend.verifier);
                 var gate = executor.openSession()) {
                transport.attachBackend(gate, 2000);
                var stop = watchdog.schedule(() -> { watchdogFired.set(true); transport.failClosed(); }, lifetime, TimeUnit.MILLISECONDS);
                try (var navigation = new ModernNavigationSession(transport, backend.characters, gate, 2000, presence, rooms)) {
                    var negotiated = transport.negotiate();
                    if (!ModernDirectChatProtocol.negotiated(negotiated)) throw new IOException("chat not negotiated");
                    navigation.negotiateRooms(true);
                    navigation.negotiateRecords(true);
                    navigation.negotiateMatchProtocol(negotiated);
                    int previous = negotiated.requestId();
                    for (;;) {
                        try (var message = transport.readApplication(5000)) {
                            if (message == null) throw new IOException("client left before committed fault");
                            if (Integer.compareUnsigned(message.requestId(), previous) <= 0) throw new IOException("request order");
                            previous = message.requestId();
                            if (message instanceof ModernApplicationProtocol.Ping ping) {
                                transport.writeApplication(new ModernApplicationProtocol.Pong(ping.requestId()));
                            } else if (message instanceof ModernApplicationProtocol.Auth auth) {
                                var principal = transport.authenticateForApplication(auth.account(), auth.password());
                                var sources = principal.sources().stream().sorted(Comparator.comparingInt(ModernAccountAuthentication.Source::id))
                                        .map(s -> new ModernApplicationProtocol.Source(s.id(), s.name())).toList();
                                transport.writeApplication(new ModernApplicationProtocol.AuthResult(auth.requestId(), 0, principal.account(), sources));
                            } else if (message instanceof ModernApplicationProtocol.CharacterRequest request) {
                                navigation.beforeCharacterRequest();
                                var principal = transport.authenticatedPrincipal().orElseThrow();
                                var rows = gate.call(2000, scope -> backend.characters.characters(principal, request.sourceId()));
                                transport.writeApplication(new ModernApplicationProtocol.CharacterResult(request.requestId(), request.sourceId(), request.selectionEpoch(), 0, rows));
                                navigation.recordRoster(request.sourceId(), request.selectionEpoch(), rows);
                            } else if (message instanceof ModernApplicationProtocol.CharacterDataRequest request) {
                                transport.writeApplication(navigation.characterData(request, ModernCharacterDataRepository.unavailable()));
                            } else if (message instanceof ModernApplicationProtocol.RecordRequest request) {
                                transport.writeApplication(navigation.record(request));
                            } else if (message instanceof ModernApplicationProtocol.RoomRequest request) {
                                transport.writeApplication(navigation.room(request));
                            } else if (message instanceof ModernApplicationProtocol.ChatRequest request) {
                                if(request.request().action()!=ModernChatProtocol.Action.READ)throw new IOException("public SEND is not a private fault");
                                transport.writeApplication(navigation.chat(request));
                            } else if (message instanceof ModernApplicationProtocol.DirectChatRequest request) {
                                var result=navigation.directChat(request); // Real qualified commit, not a fabricated reply.
                                var sent=request.request();
                                if(sent.action()!=ModernDirectChat.Action.READ) {
                                    committed++;
                                    var snapshot=result.result().snapshot();
                                    if(result.result().status()!=ModernDirectChat.Status.OK||snapshot.requestSequence()!=committed)
                                        throw new IOException("private mutation did not commit");
                                    if(committed==1&&(sent.action()!=ModernDirectChat.Action.SEND||snapshot.cursor()!=1
                                            ||snapshot.messages().size()!=1||!snapshot.messages().get(0).recipient().equals(snapshot.owner())))
                                        throw new IOException("first self-private confirmation missing");
                                    if(committed==2) {
                                        var expected=args[2].startsWith("prefs-")?ModernDirectChat.Action.PREFERENCES
                                                :args[2].startsWith("block-")?ModernDirectChat.Action.BLOCK:ModernDirectChat.Action.SEND;
                                        if(sent.action()!=expected)throw new IOException("wrong private fault mutation");
                                        boolean inRoom=args[2].endsWith("-room");
                                        if((sent.roomId()!=0)!=inRoom||rooms.roomCount()!=(inRoom?1:0)
                                                ||presence.ownerCount()!=1||presence.userCount()!=1||rooms.chatOwnerCount()!=1||rooms.directChatOwnerCount()!=1)
                                            throw new IOException("private committed ownership missing");
                                        var read=new ModernDirectChatProtocol.Request(sent.scope(),sent.roomId(),sent.membership(),0,0,
                                                ModernDirectChat.Action.READ,null,null,"");
                                        var history=navigation.directChat(new ModernApplicationProtocol.DirectChatRequest(request.requestId(),read)).result().snapshot();
                                        long count=expected==ModernDirectChat.Action.SEND?2:1;
                                        if(history.messages().size()!=count||history.cursor()!=count||history.requestSequence()!=2
                                                ||history.messages().stream().anyMatch(m->!m.sender().equals(history.owner())||!m.recipient().equals(history.owner())))
                                            throw new IOException("private committed history not exact");
                                        if(expected==ModernDirectChat.Action.PREFERENCES&&!history.preferences().equals(sent.preferences())
                                                ||expected==ModernDirectChat.Action.BLOCK&&!history.blocked().contains(sent.target().address()))
                                            throw new IOException("private control did not commit");
                                        var publicRead=new ModernChatProtocol.Request(sent.scope(),sent.roomId(),sent.membership(),0,0,ModernChatProtocol.Action.READ,"");
                                        if(!navigation.chat(new ModernApplicationProtocol.ChatRequest(request.requestId(),publicRead)).result().snapshot().messages().isEmpty())
                                            throw new IOException("private mutation leaked to public chat");
                                        System.out.println("COMMITTED DIRECT 2");System.out.flush();
                                        long start=System.nanoTime();
                                        try(var unexpected=transport.readApplication(2500)) {
                                            if(unexpected!=null)throw new IllegalStateException("private mutation was replayed or connection continued");
                                        } catch(IOException closedByClient) { /* TLS close/reset must be prompt, not a timeout/watchdog. */ }
                                        if(TimeUnit.NANOSECONDS.toMillis(System.nanoTime()-start)>=2200)
                                            throw new IOException("client did not promptly close uncertain private mutation");
                                        clientClosed=true;break;
                                    }
                                }
                                transport.writeApplication(result);
                            } else if (message instanceof ModernApplicationProtocol.CharacterSelection
                                    || message instanceof ModernApplicationProtocol.ChannelEnter
                                    || message instanceof ModernApplicationProtocol.CharacterReturn
                                    || message instanceof ModernApplicationProtocol.LobbyLeave) {
                                transport.writeApplication(navigation.handle(message));
                            } else throw new IOException("unexpected chat fixture operation");
                        }
                    }
                } finally { stop.cancel(false); }
            }
            // A timeout must not silently establish a replacement session and replay.
            listener.setSoTimeout(300);
            try (var unexpected = listener.accept()) { throw new IOException("unexpected automatic reconnect"); }
            catch (SocketTimeoutException expected) { /* Explicit bounded negative observation. */ }
        } finally {
            watchdog.shutdownNow(); executor.close();
            if (!watchdog.awaitTermination(1000, TimeUnit.MILLISECONDS) || !executor.awaitTermination(3000)
                    || presence.ownerCount() != 0 || presence.userCount() != 0 || rooms.ownerCount() != 0
                    || rooms.roomCount() != 0 || rooms.memberCount() != 0 || rooms.chatOwnerCount() != 0 || rooms.directChatOwnerCount() != 0)
                throw new IllegalStateException("chat fault ownership did not retire");
        }
        if (committed != 2 || !clientClosed || watchdogFired.get() || backend.authenticationCalls.get() != 2)
            throw new IllegalStateException("chat fault was not fully exercised");
    }
}
