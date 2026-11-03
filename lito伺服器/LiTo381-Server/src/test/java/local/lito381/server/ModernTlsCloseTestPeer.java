package local.lito381.server;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLEngineResult;

/** Test-only strict TLS closure oracle. SSLEngine distinguishes an authenticated
 * close_notify from a raw TCP EOF; ordinary SSLSocket EOF is not that proof.
 * Uses the generated disposable identity, a single loopback peer, bounded
 * buffers/time, and real HELLO/PING codecs. Never accepts player credentials. */
public final class ModernTlsCloseTestPeer {
    private ModernTlsCloseTestPeer() {}
    public static void main(String[] args) {
        try {
            if(args.length!=4 || !args[0].equals("serve")
                    || !Set.of("idle","idle-tls12","abrupt","held-ping","silent-after-ack").contains(args[2]))
                throw new IllegalArgumentException();
            int lifetime=Integer.parseInt(args[3]);
            if(lifetime<1000 || lifetime>30000)throw new IllegalArgumentException();
            run(Path.of(args[1]),args[2],lifetime);
        } catch(Exception failure) {
            System.err.println("MODERN_TLS_CLOSE_PEER_FAILED "+failure.getClass().getSimpleName());
            java.util.Arrays.stream(failure.getStackTrace()).limit(5).forEach(System.err::println);
            System.exit(2);
        }
    }
    private static void run(Path fixture,String mode,int lifetime)throws Exception {
        var tls=ModernTlsTestMaterial.serverContext(fixture,false);
        try(var listener=new ServerSocket(0,1,InetAddress.getByAddress(new byte[]{127,0,0,1}))) {
            listener.setSoTimeout(lifetime);
            System.out.println("READY "+listener.getLocalPort());System.out.flush();
            try(var socket=listener.accept()) {
                socket.setSoTimeout(lifetime);socket.setTcpNoDelay(true);
                var timer=Executors.newSingleThreadScheduledExecutor(task->{
                    var thread=new Thread(task,"modern-close-oracle-deadline");thread.setDaemon(true);return thread;
                });
                var expiry=timer.schedule(()->{try{socket.close();}catch(IOException ignored){}},lifetime,TimeUnit.MILLISECONDS);
                try {
                    var engine=tls.createSSLEngine();engine.setUseClientMode(false);
                    engine.setEnabledProtocols(new String[]{mode.equals("idle-tls12")?"TLSv1.2":"TLSv1.3"});
                    var wire=new EngineStream(socket,engine);wire.handshake();
                    var hello=ModernSessionProtocol.readFrame(wire);
                    var boundary=new ModernSessionBoundary(ModernSessionBoundary.Policy.current());
                    try {
                        wire.writeApplication(boundary.acceptHello(hello));
                        if(mode.equals("held-ping")) {
                            var frame=ModernApplicationProtocol.readFrame(wire);
                            try(var request=ModernApplicationProtocol.decode(frame)) {
                                if(!(request instanceof ModernApplicationProtocol.Ping))throw new IllegalStateException();
                            } finally {java.util.Arrays.fill(frame,(byte)0);}
                            System.out.println("PING_HELD");System.out.flush();
                        } else if(mode.equals("silent-after-ack")) {
                            // The client must retire without waiting for this peer to
                            // consume the alert or return its own close_notify.
                            Thread.sleep(800);
                        }
                        if(wire.read()!=-1)throw new IllegalStateException("unexpected application data");
                        boolean expected=!mode.equals("held-ping")&&!mode.equals("abrupt");
                        if(wire.receivedCloseNotify!=expected) {
                            System.err.println("TLS_CLOSE_PROOF expected="+(expected?1:0)+" received="+(wire.receivedCloseNotify?1:0));
                            throw new IllegalStateException("TLS closure mismatch");
                        }
                    } finally {boundary.close();java.util.Arrays.fill(hello,(byte)0);}
                } finally {
                    expiry.cancel(false);timer.shutdownNow();
                    if(!timer.awaitTermination(2000,TimeUnit.MILLISECONDS))throw new IllegalStateException("oracle timer retained");
                }
            }
        }
    }
    /** One-owner synchronous test adapter, not a replacement production TLS stack. */
    private static final class EngineStream extends InputStream {
        private static final int CAPACITY=65536;
        private static final ByteBuffer EMPTY=ByteBuffer.allocate(0);
        private final Socket socket;
        private final SSLEngine engine;
        // encrypted is kept in write mode; plaintext is kept in read mode.
        private final ByteBuffer encrypted=ByteBuffer.allocate(CAPACITY);
        private final ByteBuffer plaintext=ByteBuffer.allocate(CAPACITY);
        private final ByteBuffer outbound=ByteBuffer.allocate(CAPACITY);
        private boolean eof,receivedCloseNotify;
        EngineStream(Socket socket,SSLEngine engine) {
            this.socket=socket;this.engine=engine;plaintext.limit(0);
        }
        void handshake()throws IOException {
            engine.beginHandshake();
            while(engine.getHandshakeStatus()!=SSLEngineResult.HandshakeStatus.NOT_HANDSHAKING) {
                if(eof || receivedCloseNotify)throw new IOException("handshake closed");
                step();
            }
        }
        private void tasks() {
            for(int count=0;engine.getHandshakeStatus()==SSLEngineResult.HandshakeStatus.NEED_TASK;count++) {
                if(count==64)throw new IllegalStateException("TLS task bound");
                Runnable task=engine.getDelegatedTask();if(task==null)throw new IllegalStateException();task.run();
            }
        }
        private void wrap(ByteBuffer application)throws IOException {
            outbound.clear();var result=engine.wrap(application,outbound);
            if(result.getStatus()!=SSLEngineResult.Status.OK)throw new IOException("TLS wrap failed");
            outbound.flip();socket.getOutputStream().write(outbound.array(),0,outbound.remaining());
            socket.getOutputStream().flush();tasks();
        }
        private void step()throws IOException {
            tasks();
            if(engine.getHandshakeStatus()==SSLEngineResult.HandshakeStatus.NEED_WRAP) {wrap(EMPTY);return;}
            if(plaintext.hasRemaining())throw new IOException("unconsumed application bytes");
            plaintext.clear();encrypted.flip();
            var result=engine.unwrap(encrypted,plaintext);
            encrypted.compact();plaintext.flip();tasks();
            switch(result.getStatus()) {
                case CLOSED -> receivedCloseNotify=true;
                case BUFFER_OVERFLOW -> throw new IOException("TLS plaintext bound");
                case BUFFER_UNDERFLOW -> {
                    if(!encrypted.hasRemaining())throw new IOException("TLS packet bound");
                    int count=socket.getInputStream().read(encrypted.array(),encrypted.position(),encrypted.remaining());
                    if(count<0)eof=true;else encrypted.position(encrypted.position()+count);
                }
                case OK -> {
                    if(result.bytesConsumed()==0 && result.bytesProduced()==0
                            && result.getHandshakeStatus()==SSLEngineResult.HandshakeStatus.NOT_HANDSHAKING)
                        throw new IOException("TLS no progress");
                }
            }
        }
        @Override public int read()throws IOException {
            while(!plaintext.hasRemaining()) {
                if(eof || receivedCloseNotify)return -1;
                step();
            }
            return Byte.toUnsignedInt(plaintext.get());
        }
        @Override public int read(byte[] buffer,int offset,int length)throws IOException {
            java.util.Objects.checkFromIndexSize(offset,length,buffer.length);
            if(length==0)return 0;
            int first=read();if(first<0)return -1;buffer[offset]=(byte)first;
            int extra=Math.min(length-1,plaintext.remaining());plaintext.get(buffer,offset+1,extra);return extra+1;
        }
        void writeApplication(byte[] frame)throws IOException {
            var input=ByteBuffer.wrap(frame);
            try {while(input.hasRemaining())wrap(input);}
            finally {java.util.Arrays.fill(frame,(byte)0);}
        }
    }
}
