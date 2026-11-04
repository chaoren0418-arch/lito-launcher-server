// ORIGINAL SOURCE aligned to base-0.6.71.jar (formal behavior restored from reference JAR)
package local.lito381.server;

import java.lang.reflect.Field;
import java.lang.reflect.RecordComponent;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;

final class SourceLogin {
   static final Map<Integer, SourceLogin.Binding> bindings = new ConcurrentHashMap<>();
   static final Map<GameHub, List<ServerSocket>> listeners = new ConcurrentHashMap<>();
   static final Field sessionConfig;

   static ServerConfig scoped(ServerConfig var0, int var1, int var2) throws Exception {
      RecordComponent[] var3 = ServerConfig.class.getRecordComponents();
      Class[] var4 = new Class[var3.length];
      Object[] var5 = new Object[var3.length];

      for (int var6 = 0; var6 < var3.length; var6++) {
         RecordComponent var7 = var3[var6];
         var4[var6] = var7.getType();
         Object var8 = var7.getAccessor().invoke(var0);
         String var9 = var7.getName();

         var5[var6] = switch (var9) {
            case "port", "advertisedPort" -> var2;
            case "channelPorts" -> List.of(var2);
            case "classicServerNames", "classicServerIds", "legacySourceDatabases", "legacySourceResourceDatabases", "legacySourceWeightRates" -> List.of(
               ((List)var8).get(var1)
            );
            default -> var8;
         };
      }

      return ServerConfig.class.getDeclaredConstructor(var4).newInstance(var5);
   }

   static void start(ServerConfig var0, AccountRepository var1, GameStateStore var2, CaptureLog var3, GameHub var4, ExecutorService var5) throws Exception {
      if (SharedDatabase.enabled(var0)) {
         int var6 = Integer.parseInt(SharedDatabase.properties(var0.source()).getProperty("login.source.port.base", "32000"));
         if (var6 < 1024 || var6 > 65527) {
            throw new IllegalArgumentException("Invalid login.source.port.base");
         } else if (var0.dedicatedChannelPortsEnabled()) {
            throw new IllegalArgumentException("Source login requires shared logical channels");
         } else {
            ArrayList<ServerSocket> var7 = new ArrayList<>();
            ArrayList<Runnable> var8 = new ArrayList<>();
            ArrayList<Integer> var9 = new ArrayList<>();

            try {
               for (int var10 = 0; var10 < var0.classicServerNames().size(); var10++) {
                  int var24 = var0.classicServerIds().get(var10);
                  int var27 = var6 + var24;
                  if (var27 == var0.port() || var0.channelPorts().contains(var27)) {
                     throw new IllegalArgumentException("Source port overlaps base listener");
                  }

                  SourceLogin.Binding var13 = new SourceLogin.Binding(var24, var0.classicServerNames().get(var10), scoped(var0, var10, var27));
                  if (bindings.putIfAbsent(var27, var13) != null) {
                     throw new IllegalArgumentException("Duplicate source port");
                  }

                  var9.add(var27);

                  for (ClientSession.Endpoint var17 : ClientSession.Endpoint.values()) {
                     ServerSocket var18 = new ServerSocket();
                     var7.add(var18);
                     String var19 = var17 == ClientSession.Endpoint.LOGIN ? var0.bind() : var0.gameBind();
                     NativeServerLifetime.bindSource(var18, new InetSocketAddress(InetAddress.getByName(var19), var27), 16, var4);
                     var8.add(() -> {
                        try {
                           try {
                              while (!var18.isClosed()) {
                                 Socket var8x = NativeServerLifetime.acceptSocket(var18, var4);

                                 try {
                                    NativeServerLifetime.submitAccepted(var5, new ClientSession(var8x, var17, var0, var1, var2, var3, var4));
                                 } catch (Exception var11) {
                                    var8x.close();
                                    throw var11;
                                 }
                              }
                           } catch (Exception var12x) {
                              if (!var18.isClosed()) {
                                 System.out.println("[LiTo] source-listener failed: " + var12x.getClass().getSimpleName());
                              }
                           }

                           NativeServerLifetime.abortAccept();
                        } catch (Throwable var13x) {
                           NativeServerLifetime.abortAccept();
                           throw var13x;
                        }
                     });
                  }

                  System.out.println("[LiTo] source-login id=" + var24 + " name=" + var13.name() + " port=" + var27 + " auth=selected-source-only");
               }

               listeners.put(var4, var7);

               for (Runnable var25 : var8) {
                  var5.submit(var25);
               }
            } catch (Exception var21) {
               for (ServerSocket var12 : var7) {
                  try {
                     var12.close();
                  } catch (Exception var20) {
                  }
               }

               for (int var26 : var9) {
                  bindings.remove(var26);
               }

               throw var21;
            }
         }
      }
   }

   static void close(GameHub var0) {
      List<ServerSocket> var1 = listeners.remove(var0);
      if (var1 != null) {
         for (ServerSocket var3 : var1) {
            bindings.remove(var3.getLocalPort());

            try {
               var3.close();
            } catch (Exception var5) {
            }
         }
      }
   }

   static ServerConfig configuration(ServerConfig var0, Socket var1) {
      SourceLogin.Binding var2 = var1 == null ? null : bindings.get(var1.getLocalPort());
      return var2 == null ? var0 : var2.config();
   }

   static AccountRepository repository(AccountRepository var0, Socket var1) {
      SourceLogin.Binding var2 = var1 == null ? null : bindings.get(var1.getLocalPort());
      return var2 == null ? var0 : var0.repositoryForClassicSource(var2.name());
   }

   static boolean allowed(Socket var0, ServerConfig var1) throws Exception {
      if (SharedDatabase.enabled(var1) && !bindings.containsKey(var0.getLocalPort())) {
         var0.close();
         return false;
      } else {
         return true;
      }
   }

   static String account(ClientSession var0, String var1) {
      if (var0 != null && var1 != null && !var1.isBlank()) {
         try {
            ServerConfig var2 = (ServerConfig)sessionConfig.get(var0);
            SourceLogin.Binding var3 = bindings.get(var2.port());
            return var3 == null ? var1 : "source:" + var3.id() + ":" + var1;
         } catch (IllegalAccessException var4) {
            throw new IllegalStateException(var4);
         }
      } else {
         return var1;
      }
   }

   static {
      try {
         sessionConfig = ClientSession.class.getDeclaredField("config");
         sessionConfig.setAccessible(true);
      } catch (Exception var1) {
         throw new ExceptionInInitializerError(var1);
      }
   }

   record Binding(int id, String name, ServerConfig config) {
   }
}
