// DECOMPILED from base-0.6.71.jar, TODO review
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

final class NativeServerLifetime {
   static final ThreadLocal<NativeServerLifetime> main = new ThreadLocal<>();
   static final ThreadLocal<NativeServerLifetime> session = new ThreadLocal<>();
   static final ThreadLocal<NativeServerLifetime> accepting = new ThreadLocal<>();
   static final ThreadLocal<Socket> acceptedSocket = new ThreadLocal<>();
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
   final Set<AutoCloseable> closeAttempted = Collections.newSetFromMap(new IdentityHashMap<>());
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

   private NativeServerLifetime(GameHub var1, ExecutorService var2, List<ServerSocket> var3, CaptureLog var4, LitoMapRepository var5, AccountRepository var6) {
      this.hub = var1;
      this.clients = var2;
      this.listeners = var3;
      this.capture = var4;
      this.maps = var5;
      this.accounts = var6;
      this.hook = new Thread(() -> {
         try {
            this.closeApplication();
            this.closeResources();
         } catch (Throwable var2x) {
            System.err.println("[LiTo] application shutdown fenced: " + var2x);
         }
      }, "LiTo-native-shutdown");
   }

   static void install(GameHub var0, ExecutorService var1, List<ServerSocket> var2, CaptureLog var3, LitoMapRepository var4, AccountRepository var5) {
      if (main.get() == null && NativePlayerLifetime.of(var0).application == null) {
         NativeServerLifetime var6 = new NativeServerLifetime(var0, var1, var2, var3, var4, var5);
         NativePlayerLifetime.of(var0).application = var6;
         main.set(var6);
         Runtime.getRuntime().addShutdownHook(var6.hook);
      } else {
         throw new IllegalStateException("native application already installed");
      }
   }

   static NativeServerLifetime of(GameHub var0) {
      return NativePlayerLifetime.of(var0).application;
   }

   static NativeServerLifetime of(ClientSession var0) {
      return of((GameHub)CombatAccess.field(var0, "hub"));
   }

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

   static void bindSource(ServerSocket var0, SocketAddress var1, int var2, GameHub var3) throws IOException {
      bind(of(var3), var0, var1, var2);
   }

   static void bindMain(ServerSocket var0, SocketAddress var1, int var2) throws IOException {
      bind(main.get(), var0, var1, var2);
   }

   private static void bind(NativeServerLifetime var0, ServerSocket var1, SocketAddress var2, int var3) throws IOException {
      if (var0 == null) {
         bindNative(var1, var2, var3);
      } else {
         synchronized (var0.fence) {
            if (var0.closing) {
               closeSocketQuiet(var1);
               throw new IOException("listener admission closed");
            } else {
               bindNative(var1, var2, var3);
               var0.sourceListeners.add(var1);
            }
         }
      }
   }

   private static void bindNative(ServerSocket var0, SocketAddress var1, int var2) throws IOException {
      try {
         var0.bind(var1, var2);
      } catch (IOException var6) {
         try {
            var0.close();
         } catch (IOException var5) {
            var6.addSuppressed(var5);
         }

         throw var6;
      }
   }

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

   static Future<?> submitAccepted(ExecutorService var0, Runnable var1) {
      boolean var2 = false;

      Future var4;
      try {
         Future var3 = var0.submit(var1);
         var2 = true;
         var4 = var3;
      } finally {
         leaveAccept(var2);
      }

      return var4;
   }

   static void abortAccept() {
      leaveAccept(false);
   }

   private static void leaveAccept(boolean var0) {
      NativeServerLifetime var1 = accepting.get();
      if (var1 != null) {
         Socket var2 = acceptedSocket.get();
         accepting.remove();
         acceptedSocket.remove();
         if (!var0) {
            try {
               var2.close();
            } catch (IOException var6) {
            }
         }

         synchronized (var1.fence) {
            if (--var1.accepts < 0) {
               throw new IllegalStateException("accepted socket receipt underflow");
            } else {
               var1.fence.notifyAll();
            }
         }
      }
   }

   static void closeSocketQuiet(ServerSocket var0) {
      if (var0 != null) {
         try {
            var0.close();
         } catch (Exception var2) {
         }
      }
   }

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

   static void awaitCleanup(ClientSession var0) {
      NativeServerLifetime var1 = of(var0);
      if (var1 != null && var1.closing) {
         boolean var2 = false;

         while (true) {
            try {
               var1.damageFinished.await();
               break;
            } catch (InterruptedException var4) {
               var2 = true;
            }
         }

         if (var2) {
            Thread.currentThread().interrupt();
         }
      }
   }

   static void closeSessionSocket(Socket var0, ClientSession var1) throws IOException {
      awaitCleanup(var1);
      var0.close();
   }

   static void closeHub(GameHub var0) {
      abortAccept();
      NativeServerLifetime var1 = of(var0);
      if (var1 == null) {
         var0.close();
      } else {
         var1.ensureExternal();

         try {
            var1.closeApplication();
         } catch (RuntimeException var3) {
            var1.retry();
            throw var3;
         }
      }
   }

   static void closeResource(AutoCloseable var0) throws Exception {
      NativeServerLifetime var1 = main.get();
      if (var1 == null) {
         var0.close();
      } else {
         var1.closeOwned(var0);
      }
   }

   static List<Runnable> finishClients(ExecutorService var0) {
      NativeServerLifetime var1 = main.get();
      if (var1 == null) {
         return var0.shutdownNow();
      } else {
         List var2;
         try {
            if (!var1.networkDone) {
               var1.retry();
            } else {
               var1.releaseIfDone();
            }

            var2 = List.of();
         } finally {
            main.remove();
         }

         return var2;
      }
   }

   private void ensureExternal() {
      if (accepting.get() != this && session.get() != this && !packets.get().contains(this) && !NativePlayerLifetime.onWorker(this.hub)) {
         if (packets.get().isEmpty()) {
            packets.remove();
         }
      } else {
         throw new IllegalStateException("application cannot synchronously close from its own accept/session/packet/combat worker");
      }
   }

   boolean safe() {
      return this.combatDone && this.networkDone && this.clients.isTerminated();
   }

   private void closeApplication() {
      this.ensureExternal();
      synchronized (this.control) {
         if (!this.networkDone) {
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
   }

   private void closeOwned(AutoCloseable var1) throws Exception {
      synchronized (this.resourceFence) {
         if (!this.safe()) {
            throw new IllegalStateException("accepted work/session cleanup remains active; resource close fenced");
         } else if (this.closeAttempted.add(var1)) {
            try {
               var1.close();
            } finally {
               this.releaseIfDone();
            }
         }
      }
   }

   private void closeResources() throws Exception {
      Exception var1 = null;

      for (AutoCloseable var5 : new AutoCloseable[]{this.capture, this.maps, this.accounts}) {
         try {
            this.closeOwned(var5);
         } catch (Exception var7) {
            if (var1 == null) {
               var1 = var7;
            } else {
               var1.addSuppressed(var7);
            }
         }
      }

      if (var1 != null) {
         throw var1;
      }
   }

   private void releaseIfDone() {
      synchronized (this.resourceFence) {
         if (this.safe()
            && this.closeAttempted.contains(this.capture)
            && this.closeAttempted.contains(this.maps)
            && this.closeAttempted.contains(this.accounts)) {
            this.finished = true;
            NativePlayerLifetime.of(this.hub).application = null;

            try {
               Runtime.getRuntime().removeShutdownHook(this.hook);
            } catch (IllegalStateException var4) {
            }
         }
      }
   }

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
