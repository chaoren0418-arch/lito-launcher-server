// DECOMPILED (CFR 0.152) from reference/base-0.6.71.jar — TODO: replace with original source (T002)
/*
 * Decompiled with CFR 0.152.
 */
package local.lito381.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.util.ArrayDeque;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import local.lito381.server.AccountRepository;
import local.lito381.server.CaptureLog;
import local.lito381.server.ClientSession;
import local.lito381.server.CombatAccess;
import local.lito381.server.GameHub;
import local.lito381.server.LitoMapRepository;
import local.lito381.server.NativePlayerLifetime;

final class NativeServerLifetime {
    static final ThreadLocal<NativeServerLifetime> main = new ThreadLocal();
    static final ThreadLocal<NativeServerLifetime> session = new ThreadLocal();
    static final ThreadLocal<NativeServerLifetime> accepting = new ThreadLocal();
    static final ThreadLocal<Socket> acceptedSocket = new ThreadLocal();
    static final Object UNOWNED = new Object();
    static final ThreadLocal<ArrayDeque<Object>> packets = ThreadLocal.withInitial(ArrayDeque::new);
    final GameHub hub;
    final ExecutorService clients;
    final List<ServerSocket> listeners;
    final CaptureLog capture;
    final LitoMapRepository maps;
    final AccountRepository accounts;
    final Object fence = new Object();
    final Object control = new Object();
    final Object resourceFence = new Object();
    final Set<ClientSession> sessions = ConcurrentHashMap.newKeySet();
    final Set<ServerSocket> sourceListeners = ConcurrentHashMap.newKeySet();
    final Set<AutoCloseable> closeAttempted = Collections.newSetFromMap(new IdentityHashMap());
    final CountDownLatch damageFinished = new CountDownLatch(1);
    final Thread hook;
    volatile ServerSocket login;
    volatile boolean closing;
    volatile boolean combatDone;
    volatile boolean networkDone;
    volatile boolean retryStarted;
    volatile boolean finished;
    int handlers;
    int accepts;

    private NativeServerLifetime(GameHub gameHub, ExecutorService executorService, List<ServerSocket> list, CaptureLog captureLog, LitoMapRepository litoMapRepository, AccountRepository accountRepository) {
        this.hub = gameHub;
        this.clients = executorService;
        this.listeners = list;
        this.capture = captureLog;
        this.maps = litoMapRepository;
        this.accounts = accountRepository;
        this.hook = new Thread(() -> {
            try {
                this.closeApplication();
                this.closeResources();
            }
            catch (Throwable throwable) {
                System.err.println("[LiTo] application shutdown fenced: " + String.valueOf(throwable));
            }
        }, "LiTo-native-shutdown");
    }

    static void install(GameHub gameHub, ExecutorService executorService, List<ServerSocket> list, CaptureLog captureLog, LitoMapRepository litoMapRepository, AccountRepository accountRepository) {
        NativeServerLifetime nativeServerLifetime;
        if (main.get() != null || NativePlayerLifetime.of((GameHub)gameHub).application != null) {
            throw new IllegalStateException("native application already installed");
        }
        NativePlayerLifetime.of((GameHub)gameHub).application = nativeServerLifetime = new NativeServerLifetime(gameHub, executorService, list, captureLog, litoMapRepository, accountRepository);
        main.set(nativeServerLifetime);
        Runtime.getRuntime().addShutdownHook(nativeServerLifetime.hook);
    }

    static NativeServerLifetime of(GameHub gameHub) {
        return NativePlayerLifetime.of((GameHub)gameHub).application;
    }

    static NativeServerLifetime of(ClientSession clientSession) {
        return NativeServerLifetime.of((GameHub)CombatAccess.field(clientSession, "hub"));
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    static void login(GameHub gameHub, ServerSocket serverSocket) {
        NativeServerLifetime nativeServerLifetime = NativeServerLifetime.of(gameHub);
        Object object = nativeServerLifetime.fence;
        synchronized (object) {
            if (nativeServerLifetime.closing) {
                NativeServerLifetime.closeSocketQuiet(serverSocket);
                throw new IllegalStateException("application admission closed during startup");
            }
            nativeServerLifetime.login = serverSocket;
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    static boolean addListener(List<ServerSocket> list, ServerSocket serverSocket) {
        NativeServerLifetime nativeServerLifetime = main.get();
        if (nativeServerLifetime == null) {
            return list.add(serverSocket);
        }
        Object object = nativeServerLifetime.fence;
        synchronized (object) {
            if (nativeServerLifetime.closing) {
                NativeServerLifetime.closeSocketQuiet(serverSocket);
                throw new IllegalStateException("application admission closed during listener startup");
            }
            return list.add(serverSocket);
        }
    }

    static void bindSource(ServerSocket serverSocket, SocketAddress socketAddress, int n, GameHub gameHub) throws IOException {
        NativeServerLifetime.bind(NativeServerLifetime.of(gameHub), serverSocket, socketAddress, n);
    }

    static void bindMain(ServerSocket serverSocket, SocketAddress socketAddress, int n) throws IOException {
        NativeServerLifetime.bind(main.get(), serverSocket, socketAddress, n);
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private static void bind(NativeServerLifetime nativeServerLifetime, ServerSocket serverSocket, SocketAddress socketAddress, int n) throws IOException {
        if (nativeServerLifetime == null) {
            NativeServerLifetime.bindNative(serverSocket, socketAddress, n);
            return;
        }
        Object object = nativeServerLifetime.fence;
        synchronized (object) {
            if (nativeServerLifetime.closing) {
                NativeServerLifetime.closeSocketQuiet(serverSocket);
                throw new IOException("listener admission closed");
            }
            NativeServerLifetime.bindNative(serverSocket, socketAddress, n);
            nativeServerLifetime.sourceListeners.add(serverSocket);
        }
    }

    private static void bindNative(ServerSocket serverSocket, SocketAddress socketAddress, int n) throws IOException {
        try {
            serverSocket.bind(socketAddress, n);
        }
        catch (IOException iOException) {
            try {
                serverSocket.close();
            }
            catch (IOException iOException2) {
                iOException.addSuppressed(iOException2);
            }
            throw iOException;
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    static Socket acceptSocket(ServerSocket serverSocket, GameHub gameHub) throws IOException {
        NativeServerLifetime nativeServerLifetime = NativeServerLifetime.of(gameHub);
        Socket socket = serverSocket.accept();
        if (nativeServerLifetime == null) {
            return socket;
        }
        Object object = nativeServerLifetime.fence;
        synchronized (object) {
            if (nativeServerLifetime.closing) {
                try {
                    socket.close();
                }
                catch (IOException iOException) {
                    // empty catch block
                }
                throw new SocketException("application admission closed after accept");
            }
            if (accepting.get() != null) {
                try {
                    socket.close();
                }
                catch (IOException iOException) {
                    // empty catch block
                }
                throw new IllegalStateException("nested application accept");
            }
            ++nativeServerLifetime.accepts;
            accepting.set(nativeServerLifetime);
            acceptedSocket.set(socket);
            return socket;
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    static Future<?> submitAccepted(ExecutorService executorService, Runnable runnable) {
        boolean bl = false;
        try {
            Future<?> future = executorService.submit(runnable);
            bl = true;
            Future<?> future2 = future;
            return future2;
        }
        finally {
            NativeServerLifetime.leaveAccept(bl);
        }
    }

    static void abortAccept() {
        NativeServerLifetime.leaveAccept(false);
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private static void leaveAccept(boolean bl) {
        NativeServerLifetime nativeServerLifetime = accepting.get();
        if (nativeServerLifetime == null) {
            return;
        }
        Socket socket = acceptedSocket.get();
        accepting.remove();
        acceptedSocket.remove();
        if (!bl) {
            try {
                socket.close();
            }
            catch (IOException iOException) {
                // empty catch block
            }
        }
        Object object = nativeServerLifetime.fence;
        synchronized (object) {
            if (--nativeServerLifetime.accepts < 0) {
                throw new IllegalStateException("accepted socket receipt underflow");
            }
            nativeServerLifetime.fence.notifyAll();
        }
    }

    static void closeSocketQuiet(ServerSocket serverSocket) {
        if (serverSocket != null) {
            try {
                serverSocket.close();
            }
            catch (Exception exception) {
                // empty catch block
            }
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    static boolean enterSession(ClientSession clientSession) {
        NativeServerLifetime nativeServerLifetime = NativeServerLifetime.of(clientSession);
        if (nativeServerLifetime == null) {
            return true;
        }
        Object object = nativeServerLifetime.fence;
        synchronized (object) {
            if (nativeServerLifetime.closing) {
                try {
                    ((Socket)CombatAccess.field(clientSession, "socket")).close();
                }
                catch (Exception exception) {
                    // empty catch block
                }
                return false;
            }
            nativeServerLifetime.sessions.add(clientSession);
            session.set(nativeServerLifetime);
            return true;
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    static void leaveSession(ClientSession clientSession) {
        NativeServerLifetime nativeServerLifetime = session.get();
        if (nativeServerLifetime != null) {
            nativeServerLifetime.sessions.remove(clientSession);
            session.remove();
            Object object = nativeServerLifetime.fence;
            synchronized (object) {
                nativeServerLifetime.fence.notifyAll();
            }
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    static boolean enterPacket(ClientSession clientSession) {
        NativeServerLifetime nativeServerLifetime = NativeServerLifetime.of(clientSession);
        if (nativeServerLifetime == null) {
            packets.get().push(UNOWNED);
            return true;
        }
        Object object = nativeServerLifetime.fence;
        synchronized (object) {
            if (!nativeServerLifetime.closing) {
                ++nativeServerLifetime.handlers;
                packets.get().push(nativeServerLifetime);
                return true;
            }
        }
        NativeServerLifetime.awaitCleanup(clientSession);
        CombatAccess.set(clientSession, "quitRequested", true);
        return false;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    static void leavePacket() {
        ArrayDeque<Object> arrayDeque = packets.get();
        Object object = arrayDeque.pop();
        if (arrayDeque.isEmpty()) {
            packets.remove();
        }
        if (object instanceof NativeServerLifetime) {
            NativeServerLifetime nativeServerLifetime = (NativeServerLifetime)object;
            Object object2 = nativeServerLifetime.fence;
            synchronized (object2) {
                if (--nativeServerLifetime.handlers < 0) {
                    throw new IllegalStateException("native packet receipt underflow");
                }
                nativeServerLifetime.fence.notifyAll();
            }
        }
    }

    static void awaitCleanup(ClientSession clientSession) {
        NativeServerLifetime nativeServerLifetime = NativeServerLifetime.of(clientSession);
        if (nativeServerLifetime == null || !nativeServerLifetime.closing) {
            return;
        }
        boolean bl = false;
        while (true) {
            try {
                nativeServerLifetime.damageFinished.await();
            }
            catch (InterruptedException interruptedException) {
                bl = true;
                continue;
            }
            break;
        }
        if (bl) {
            Thread.currentThread().interrupt();
        }
    }

    static void closeSessionSocket(Socket socket, ClientSession clientSession) throws IOException {
        NativeServerLifetime.awaitCleanup(clientSession);
        socket.close();
    }

    static void closeHub(GameHub gameHub) {
        NativeServerLifetime.abortAccept();
        NativeServerLifetime nativeServerLifetime = NativeServerLifetime.of(gameHub);
        if (nativeServerLifetime == null) {
            gameHub.close();
            return;
        }
        nativeServerLifetime.ensureExternal();
        try {
            nativeServerLifetime.closeApplication();
        }
        catch (RuntimeException runtimeException) {
            nativeServerLifetime.retry();
            throw runtimeException;
        }
    }

    static void closeResource(AutoCloseable autoCloseable) throws Exception {
        NativeServerLifetime nativeServerLifetime = main.get();
        if (nativeServerLifetime == null) {
            autoCloseable.close();
            return;
        }
        nativeServerLifetime.closeOwned(autoCloseable);
    }

    static List<Runnable> finishClients(ExecutorService executorService) {
        NativeServerLifetime nativeServerLifetime = main.get();
        if (nativeServerLifetime == null) {
            return executorService.shutdownNow();
        }
        try {
            if (!nativeServerLifetime.networkDone) {
                nativeServerLifetime.retry();
            } else {
                nativeServerLifetime.releaseIfDone();
            }
            List<Runnable> list = List.of();
            return list;
        }
        finally {
            main.remove();
        }
    }

    private void ensureExternal() {
        if (accepting.get() == this || session.get() == this || packets.get().contains(this) || NativePlayerLifetime.onWorker(this.hub)) {
            throw new IllegalStateException("application cannot synchronously close from its own accept/session/packet/combat worker");
        }
        if (packets.get().isEmpty()) {
            packets.remove();
        }
    }

    boolean safe() {
        return this.combatDone && this.networkDone && this.clients.isTerminated();
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private void closeApplication() {
        this.ensureExternal();
        Object object = this.control;
        synchronized (object) {
            if (this.networkDone) {
                return;
            }
            Object object2 = this.fence;
            synchronized (object2) {
                this.closing = true;
                NativeServerLifetime.closeSocketQuiet(this.login);
                for (ServerSocket serverSocket : List.copyOf(this.listeners)) {
                    NativeServerLifetime.closeSocketQuiet(serverSocket);
                }
                for (ServerSocket serverSocket : this.sourceListeners) {
                    NativeServerLifetime.closeSocketQuiet(serverSocket);
                }
            }
            ScheduledExecutorService battleClock = (ScheduledExecutorService)CombatAccess.field(this.hub, "battleClock");
            battleClock.shutdownNow();
            boolean bl = false;
            try {
                long l = System.nanoTime() + TimeUnit.SECONDS.toNanos(30L);
                Object clientSessionArray = this.fence;
                synchronized (this.fence) {
                    while (this.accepts != 0 || this.handlers != 0) {
                        long l2 = l - System.nanoTime();
                        if (l2 <= 0L) {
                            throw new IllegalStateException("accepted constructors/packet handlers remain active; application resources fenced");
                        }
                        TimeUnit.NANOSECONDS.timedWait(this.fence, l2);
                    }
                    // ** MonitorExit[var6_9] (shouldn't be in output)
                    if (!battleClock.awaitTermination(3L, TimeUnit.SECONDS)) {
                        throw new IllegalStateException("battle clock remains active; application resources fenced");
                    }
                    if (!this.combatDone) {
                        this.hub.close();
                        if (!NativePlayerLifetime.snapshot(this.hub).terminated()) {
                            throw new IllegalStateException("combat workers remain active; application resources fenced");
                        }
                        this.combatDone = true;
                        this.damageFinished.countDown();
                    }
                    for (ClientSession clientSession : (ClientSession[])this.sessions.toArray(ClientSession[]::new)) {
                        try {
                            ((Socket)CombatAccess.field(clientSession, "socket")).close();
                        }
                        catch (IOException iOException) {
                            // empty catch block
                        }
                    }
                    this.clients.shutdown();
                    if (!this.clients.awaitTermination(3L, TimeUnit.SECONDS)) {
                        this.clients.shutdownNow();
                        if (!this.clients.awaitTermination(3L, TimeUnit.SECONDS)) {
                            throw new IllegalStateException("session executor remains active; application resources fenced");
                        }
                    }
                    long l3 = System.nanoTime() + TimeUnit.SECONDS.toNanos(6L);
                    Object object3 = this.fence;
                    synchronized (object3) {
                        while (!this.sessions.isEmpty()) {
                            long l4 = l3 - System.nanoTime();
                            if (l4 <= 0L) {
                                throw new IllegalStateException("session cleanup remains active; application resources fenced");
                            }
                            TimeUnit.NANOSECONDS.timedWait(this.fence, l4);
                        }
                    }
                    this.networkDone = true;
                    this.releaseIfDone();
                }
            }
            catch (InterruptedException interruptedException) {
                bl = true;
                throw new IllegalStateException("application close interrupted; resources fenced", interruptedException);
            }
            finally {
                if (bl) {
                    Thread.currentThread().interrupt();
                }
            }
            {
            }
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private void closeOwned(AutoCloseable autoCloseable) throws Exception {
        Object object = this.resourceFence;
        synchronized (object) {
            if (!this.safe()) {
                throw new IllegalStateException("accepted work/session cleanup remains active; resource close fenced");
            }
            if (!this.closeAttempted.add(autoCloseable)) {
                return;
            }
            try {
                autoCloseable.close();
            }
            finally {
                this.releaseIfDone();
            }
        }
    }

    private void closeResources() throws Exception {
        Exception exception = null;
        for (AutoCloseable autoCloseable : new AutoCloseable[]{this.capture, this.maps, this.accounts}) {
            try {
                this.closeOwned(autoCloseable);
            }
            catch (Exception exception2) {
                if (exception == null) {
                    exception = exception2;
                    continue;
                }
                exception.addSuppressed(exception2);
            }
        }
        if (exception != null) {
            throw exception;
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private void releaseIfDone() {
        Object object = this.resourceFence;
        synchronized (object) {
            if (!(this.safe() && this.closeAttempted.contains(this.capture) && this.closeAttempted.contains(this.maps) && this.closeAttempted.contains(this.accounts))) {
                return;
            }
            this.finished = true;
            NativePlayerLifetime.of((GameHub)this.hub).application = null;
            try {
                Runtime.getRuntime().removeShutdownHook(this.hook);
            }
            catch (IllegalStateException illegalStateException) {
                // empty catch block
            }
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private void retry() {
        Object object = this.fence;
        synchronized (object) {
            if (this.retryStarted || this.finished) {
                return;
            }
            this.retryStarted = true;
        }
        object = new Thread(() -> {
            int n = 0;
            while (!this.finished) {
                try {
                    this.closeApplication();
                    this.closeResources();
                    return;
                }
                catch (Throwable throwable) {
                    if (++n == 1 || (n & n - 1) == 0) {
                        System.err.println("[LiTo] deferred application close still fenced: " + String.valueOf(throwable));
                    }
                    try {
                        Thread.sleep(2000L);
                    }
                    catch (InterruptedException interruptedException) {}
                }
            }
        }, "LiTo-native-close-retry");
        ((Thread)object).setDaemon(true);
        ((Thread)object).start();
    }
}
