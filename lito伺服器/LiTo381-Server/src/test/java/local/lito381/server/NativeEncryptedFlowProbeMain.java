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
import java.util.List;

/** Real encrypted sockets to an owned fixture; never a GUI acceptance claim. */
public final class NativeEncryptedFlowProbeMain {
    public static void main(String[] args) throws Exception {
        if (args.length != 3) throw new IllegalArgumentException("port, expected JAR SHA, evidence path required");
        Path code = Path.of(Protocol.class.getProtectionDomain().getCodeSource().getLocation().toURI());
        String sha = HexFormat.of().withUpperCase().formatHex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(code)));
        if (!sha.equals(args[1])) throw new IllegalStateException("Protocol must load from the fingerprinted artifact");
        int port = Integer.parseInt(args[0]);
        if (port < 1024 || port == 2000 || port == 3306) throw new IllegalArgumentException("Owned port required");
        var stages = new ArrayList<String>();
        String stage = "login-version";
        try (Peer login = new Peer("127.0.0.1",port)) {
            login.authenticate();
            login.await(Protocol.S_LITO_VS_SERVER_STATUS);
            stages.add("LOGIN version/auth/source-list");
            stage="source-handoff";
            login.send(new Protocol.Writer().c(Protocol.C_LITO_LIN_CHARACTER_INFO).s("月亮神").bytes());
            login.await(Protocol.S_LITO_VS_SERVER_IP);
            stages.add("LOGIN source-2 redirect");
            try (Peer game = new Peer("127.0.0.2",port)) {
                stage="game-news-barrier";
                game.authenticate();
                game.await(Protocol.S_LITO_NEWS);
                game.send(new Protocol.Writer().c(Protocol.C_LITO_READ_NEWS).bytes());
                game.await(Protocol.S_LITO_CHARACTER_INFO);
                stages.add("GAME version/auth/news-ack/source-directory");
                stage="source-character-selection";
                game.send(new Protocol.Writer().c(Protocol.C_LITO_SELECT_CHAR).s("3332").bytes());
                byte[] selection=game.await(Protocol.litoCharacterSelectResult(Protocol.LitoCharacterSelectResult.ACCEPTED)[0]&255);
                if (!Arrays.equals(selection,Protocol.litoCharacterSelectResult(Protocol.LitoCharacterSelectResult.ACCEPTED)))
                    throw new IOException("Character selection rejected");
                game.await(Protocol.S_ADD_INVENTORY_BATCH);
                stages.add("source-2 character accepted/source-inventory published");
                stage="channel-entry";
                game.send(new Protocol.Writer().c(Protocol.C_LITO_ENTER_CHANNEL).c(0).bytes());
                byte[] channel=game.await(Protocol.S_LITO_ENTER_CHANNEL_RESULT);
                if (channel.length<3 || channel[1]!=0 || channel[2]!=0) throw new IOException("Channel entry rejected");
                game.await(Protocol.S_LITO_CHANNEL_INFO);
                stages.add("channel-0 entry/status");
                stage="room-create";
                long first=game.createRoom("probe-one");
                stages.add("compact room-create accepted");
                stage="room-exit-and-recreate";
                game.send(new Protocol.Writer().c(Protocol.C_LITO_EXIT_ROOM).bytes());
                var exit=new Protocol.Reader(game.await(Protocol.S_LITO_EXIT_ROOM));exit.c();
                if(exit.d()!=first)throw new IOException("Wrong room exit acknowledgement");
                // The empty default room deliberately retains its stable ID.
                // Validate the new requested name and the explicit exit ACK.
                game.createRoom("probe-two");
                stages.add("room exit/new room accepted");
                game.send(new Protocol.Writer().c(Protocol.C_QUIT).bytes());
            }
            write(Path.of(args[2]),"passed",stages,"",sha);
        } catch (Exception failure) {
            write(Path.of(args[2]),"failed",stages,stage+":"+failure.getClass().getSimpleName(),sha);
            throw new IllegalStateException("Encrypted fixture probe failed; see redacted stage evidence");
        }
    }
    private static final class Peer implements AutoCloseable {
        final Socket socket=new Socket();
        final Protocol.CipherState incoming,outgoing;
        Peer(String host,int port)throws Exception {
            socket.connect(new InetSocketAddress(host,port),3000);socket.setSoTimeout(15000);socket.setTcpNoDelay(true);
            byte[] key=Protocol.readFrame(socket.getInputStream(),8192);
            if(key==null || key.length!=5 || (key[0]&255)!=Protocol.S_KEY)throw new IOException("Invalid S_KEY");
            var reader=new Protocol.Reader(key);reader.c();int seed=(int)reader.d();
            incoming=new Protocol.CipherState(seed);outgoing=new Protocol.CipherState(seed);
        }
        void send(byte[] payload)throws IOException { Protocol.writeFrame(socket.getOutputStream(),outgoing.encrypt(payload)); }
        byte[] await(int opcode)throws IOException {
            for(int i=0;i<200;i++) {
                byte[] frame=Protocol.readFrame(socket.getInputStream(),65535);
                if(frame==null)throw new IOException("Peer closed before expected opcode");
                byte[] payload=incoming.decrypt(frame);
                if(payload.length>0 && (payload[0]&255)==opcode)return payload;
            }
            throw new IOException("Expected opcode frame limit");
        }
        void authenticate()throws IOException {
            send(new Protocol.Writer().c(Protocol.C_VERSION).h(Protocol.VERSION_MAGIC).d(Protocol.VERSION_PRODUCT).c(2).h(Protocol.VERSION_BUILD).bytes());
            byte[] version=await(Protocol.S_VERSION_CHECK);
            if(version.length<2 || version[1]!=0)throw new IOException("Version rejected");
            // Credentials belong only to the pinned, freshly imported test dump.
            send(new Protocol.Writer().c(Protocol.C_LOGIN).s("3333").s("3333").d(0x0100007fL).bytes());
        }
        long createRoom(String name)throws IOException {
            send(new Protocol.Writer().c(Protocol.C_LITO_CREATE_ROOM).s(name).c(GameHub.RoomSettings.ALLOW_OBSERVERS).s("").h(10).bytes());
            byte[] joined=await(Protocol.S_LITO_JOIN_RESULT);
            var reader=new Protocol.Reader(joined);reader.c();
            if(reader.c()!=0)throw new IOException("Room create rejected");
            long id=reader.d();if(id==0 || !reader.s().equals(name))throw new IOException("Invalid room identity/name");return id;
        }
        public void close()throws IOException {socket.close();}
    }
    private static void write(Path path,String status,List<String> stages,String failure,String sha)throws IOException {
        String rows=String.join(",",stages.stream().map(NativeEncryptedFlowProbeMain::quote).toList());
        Files.writeString(path,"{\"status\":"+quote(status)+",\"artifact_sha256\":"+quote(sha)+",\"gui_acceptance\":false,\"stages\":["+rows+"],\"failure\":"+quote(failure)+"}",StandardCharsets.UTF_8);
    }
    private static String quote(String value) { return "\""+value.replace("\\","\\\\").replace("\"","\\\"")+"\""; }
}
