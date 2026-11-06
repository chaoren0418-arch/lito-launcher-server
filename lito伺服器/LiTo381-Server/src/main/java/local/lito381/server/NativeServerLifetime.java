// DECOMPILED from base-0.6.71.jar, TODO review
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
        if (main.get() != null || NativePlayerLifetime.of((GameHub)gameHub).application != null) {
            throw new IllegalStateException("native application already installed");
        }
        NativeServerLifetime nativeServerLifetime = new NativeServerLifetime(gameHub, executorService, list, captureLog, litoMapRepository, accountRepository);
        NativePlayerLifetime.of(gameHub).application = nativeServerLifetime;
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
   static void login(GameHub var0, ServerSocket var1) {
      NativeServerLifetime var2 = of(var0);
      synchronized (var2.fence) {
         if (var2.closing) {
            closeSocketQuiet(var1);
            throw new IllegalStateException("application admission closed during startup");
         } else {
            var2.login = var1;
         }
      }
   }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
   static boolean addListener(List<ServerSocket> var0, ServerSocket var1) {
      NativeServerLifetime var2 = main.get();
      if (var2 == null) {
         return var0.add(var1);
      } else {
         synchronized (var2.fence) {
            if (var2.closing) {
               closeSocketQuiet(var1);
               throw new IllegalStateException("application admission closed during listener startup");
            } else {
               return var0.add(var1);
            }
         }
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
        synchronized (nativeServerLifetime.fence) {
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
   static Socket acceptSocket(ServerSocket var0, GameHub var1) throws IOException {
      NativeServerLifetime var2 = of(var1);
      Socket var3 = var0.accept();
      if (var2 == null) {
         return var3;
      } else {
         synchronized (var2.fence) {
            if (var2.closing) {
               try {
                  var3.close();
               } catch (IOException var7) {
               }

               throw new SocketException("application admission closed after accept");
            } else if (accepting.get() != null) {
               try {
                  var3.close();
               } catch (IOException var8) {
               }

               throw new IllegalStateException("nested application accept");
            } else {
               var2.accepts++;
               accepting.set(var2);
               acceptedSocket.set(var3);
               return var3;
            }
         }
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
            return future;
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
        synchronized (nativeServerLifetime.fence) {
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
   static boolean enterSession(ClientSession var0) {
      NativeServerLifetime var1 = of(var0);
      if (var1 == null) {
         return true;
      } else {
         synchronized (var1.fence) {
            if (var1.closing) {
               try {
                  ((Socket)CombatAccess.field(var0, "socket")).close();
               } catch (Exception var5) {
               }

               return false;
            } else {
               var1.sessions.add(var0);
               session.set(var1);
               return true;
            }
         }
      }
   }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
   static void leaveSession(ClientSession var0) {
      NativeServerLifetime var1 = session.get();
      if (var1 != null) {
         var1.sessions.remove(var0);
         session.remove();
         synchronized (var1.fence) {
            var1.fence.notifyAll();
         }
      }
   }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
   static boolean enterPacket(ClientSession var0) {
      NativeServerLifetime var1 = of(var0);
      if (var1 == null) {
         packets.get().push(UNOWNED);
         return true;
      } else {
         synchronized (var1.fence) {
            if (!var1.closing) {
               var1.handlers++;
               packets.get().push(var1);
               return true;
            }
         }

         awaitCleanup(var0);
         CombatAccess.set(var0, "quitRequested", true);
         return false;
      }
   }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
   static void leavePacket() {
      ArrayDeque var0 = packets.get();
      Object var1 = var0.pop();
      if (var0.isEmpty()) {
         packets.remove();
      }

      if (var1 instanceof NativeServerLifetime var2) {
         synchronized (var2.fence) {
            if (--var2.handlers < 0) {
               throw new IllegalStateException("native packet receipt underflow");
            }

            var2.fence.notifyAll();
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
            return List.of();
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
      synchronized (this.control) {
         if (this.networkDone) return;

            synchronized (this.fence) {
               this.closing = true;
               closeSocketQuiet(this.login);

               for (ServerSocket var4 : List.copyOf(this.listeners)) {
                  closeSocketQuiet(var4);
               }

               for (ServerSocket var31 : this.sourceListeners) {
                  closeSocketQuiet(var31);
               }
            }

            ScheduledExecutorService var28 = (ScheduledExecutorService)CombatAccess.field(this.hub, "battleClock");
            var28.shutdownNow();
            boolean var30 = false;

            try {
               long var32 = System.nanoTime() + TimeUnit.SECONDS.toNanos(30L);
               synchronized (this.fence) {
                  while (this.accepts != 0 || this.handlers != 0) {
                     long var7 = var32 - System.nanoTime();
                     if (var7 <= 0L) {
                        throw new IllegalStateException("accepted constructors/packet handlers remain active; application resources fenced");
                     }

                     TimeUnit.NANOSECONDS.timedWait(this.fence, var7);
                  }
               }

               if (!var28.awaitTermination(3L, TimeUnit.SECONDS)) {
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

               for (ClientSession var9 : this.sessions.toArray(ClientSession[]::new)) {
                  try {
                     ((Socket)CombatAccess.field(var9, "socket")).close();
                  } catch (IOException var21) {
                  }
               }

               this.clients.shutdown();
               if (!this.clients.awaitTermination(3L, TimeUnit.SECONDS)) {
                  this.clients.shutdownNow();
                  if (!this.clients.awaitTermination(3L, TimeUnit.SECONDS)) {
                     throw new IllegalStateException("session executor remains active; application resources fenced");
                  }
               }

               long var34 = System.nanoTime() + TimeUnit.SECONDS.toNanos(6L);
               synchronized (this.fence) {
                  while (!this.sessions.isEmpty()) {
                     long var37 = var34 - System.nanoTime();
                     if (var37 <= 0L) {
                        throw new IllegalStateException("session cleanup remains active; application resources fenced");
                     }

                     TimeUnit.NANOSECONDS.timedWait(this.fence, var37);
                  }
               }

               this.networkDone = true;
               this.releaseIfDone();
            } catch (InterruptedException var24) {
               var30 = true;
               throw new IllegalStateException("application close interrupted; resources fenced", var24);
            } finally {
               if (var30) {
                  Thread.currentThread().interrupt();
               }
            }

      }
   }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private void closeOwned(AutoCloseable autoCloseable) throws Exception {
        synchronized (this.resourceFence) {
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
        synchronized (this.resourceFence) {
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
      synchronized (this.fence) {
         if (this.retryStarted || this.finished) {
            return;
         }

         this.retryStarted = true;
      }

      Thread var4 = new Thread(() -> {
         int var1 = 0;

         while (!this.finished) {
            try {
               this.closeApplication();
               this.closeResources();
               return;
            } catch (Throwable var5) {
               var1++;
               if (var1 == 1 || (var1 & var1 - 1) == 0) {
                  System.err.println("[LiTo] deferred application close still fenced: " + var5);
               }

               try {
                  Thread.sleep(2000L);
               } catch (InterruptedException var4x) {
               }
            }
         }
      }, "LiTo-native-close-retry");
      var4.setDaemon(true);
      var4.start();
   }
}
