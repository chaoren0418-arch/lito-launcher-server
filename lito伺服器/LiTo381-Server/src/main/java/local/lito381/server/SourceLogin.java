// DECOMPILED (CFR 0.152) from reference/base-0.6.71.jar — TODO: replace with original source (T002)
/*
 * Decompiled with CFR 0.152.
 */
package local.lito381.server;

import java.lang.reflect.Field;
import java.lang.reflect.RecordComponent;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import local.lito381.server.AccountRepository;
import local.lito381.server.CaptureLog;
import local.lito381.server.ClientSession;
import local.lito381.server.GameHub;
import local.lito381.server.GameStateStore;
import local.lito381.server.NativeServerLifetime;
import local.lito381.server.ServerConfig;
import local.lito381.server.SharedDatabase;

final class SourceLogin {
    static final Map<Integer, Binding> bindings = new ConcurrentHashMap<Integer, Binding>();
    static final Map<GameHub, List<ServerSocket>> listeners = new ConcurrentHashMap<GameHub, List<ServerSocket>>();
    static final Field sessionConfig;

    SourceLogin() {
    }

    static ServerConfig scoped(ServerConfig serverConfig, int n, int n2) throws Exception {
        RecordComponent[] recordComponentArray = ServerConfig.class.getRecordComponents();
        Class[] classArray = new Class[recordComponentArray.length];
        Object[] objectArray = new Object[recordComponentArray.length];
        for (int i = 0; i < recordComponentArray.length; ++i) {
            RecordComponent recordComponent = recordComponentArray[i];
            classArray[i] = recordComponent.getType();
            List<Integer> list = recordComponent.getAccessor().invoke(serverConfig, new Object[0]);
            objectArray[i] = switch (recordComponent.getName()) {
                case "port", "advertisedPort" -> n2;
                case "channelPorts" -> List.of(Integer.valueOf(n2));
                case "classicServerNames", "classicServerIds", "legacySourceDatabases", "legacySourceResourceDatabases", "legacySourceWeightRates" -> List.of(((List)list).get(n));
                default -> list;
            };
        }
        return (ServerConfig)ServerConfig.class.getDeclaredConstructor(classArray).newInstance(objectArray);
    }

    static void start(ServerConfig serverConfig, AccountRepository accountRepository, GameStateStore gameStateStore, CaptureLog captureLog, GameHub gameHub, ExecutorService executorService) throws Exception {
        if (!SharedDatabase.enabled(serverConfig)) {
            return;
        }
        int n = Integer.parseInt(SharedDatabase.properties(serverConfig.source()).getProperty("login.source.port.base", "32000"));
        if (n < 1024 || n > 65527) {
            throw new IllegalArgumentException("Invalid login.source.port.base");
        }
        if (serverConfig.dedicatedChannelPortsEnabled()) {
            throw new IllegalArgumentException("Source login requires shared logical channels");
        }
        ArrayList<ServerSocket> arrayList = new ArrayList<ServerSocket>();
        ArrayList<Runnable> arrayList2 = new ArrayList<Runnable>();
        ArrayList<Integer> arrayList3 = new ArrayList<Integer>();
        try {
            for (int i = 0; i < serverConfig.classicServerNames().size(); ++i) {
                int n2 = serverConfig.classicServerIds().get(i);
                int n3 = n + n2;
                if (n3 == serverConfig.port() || serverConfig.channelPorts().contains(n3)) {
                    throw new IllegalArgumentException("Source port overlaps base listener");
                }
                Binding binding = new Binding(n2, serverConfig.classicServerNames().get(i), SourceLogin.scoped(serverConfig, i, n3));
                if (bindings.putIfAbsent(n3, binding) != null) {
                    throw new IllegalArgumentException("Duplicate source port");
                }
                arrayList3.add(n3);
                for (ClientSession.Endpoint endpoint : ClientSession.Endpoint.values()) {
                    ServerSocket serverSocket = new ServerSocket();
                    arrayList.add(serverSocket);
                    String string = endpoint == ClientSession.Endpoint.LOGIN ? serverConfig.bind() : serverConfig.gameBind();
                    NativeServerLifetime.bindSource(serverSocket, new InetSocketAddress(InetAddress.getByName(string), n3), 16, gameHub);
                    arrayList2.add(() -> {
                        block9: {
                            try {
                                try {}
                                catch (Exception exception) {
                                    if (!serverSocket.isClosed()) {
                                        System.out.println("[LiTo] source-listener failed: " + exception.getClass().getSimpleName());
                                    }
                                    break block9;
                                }
                            }
                            catch (Throwable throwable) {
                                NativeServerLifetime.abortAccept();
                                throw throwable;
                            }
                            while (!serverSocket.isClosed()) {
                                Socket socket = NativeServerLifetime.acceptSocket(serverSocket, gameHub);
                                try {
                                    NativeServerLifetime.submitAccepted(executorService, new ClientSession(socket, endpoint, serverConfig, accountRepository, gameStateStore, captureLog, gameHub));
                                }
                                catch (Exception exception) {
                                    socket.close();
                                    throw exception;
                                }
                            }
                        }
                        NativeServerLifetime.abortAccept();
                    });
                }
                System.out.println("[LiTo] source-login id=" + n2 + " name=" + binding.name() + " port=" + n3 + " auth=selected-source-only");
            }
            listeners.put(gameHub, arrayList);
            for (Runnable runnable : arrayList2) {
                executorService.submit(runnable);
            }
        }
        catch (Exception exception) {
            for (ServerSocket serverSocket : arrayList) {
                try {
                    serverSocket.close();
                }
                catch (Exception exception2) {}
            }
            Iterator iterator = arrayList3.iterator();
            while (iterator.hasNext()) {
                int n4 = (Integer)iterator.next();
                bindings.remove(n4);
            }
            throw exception;
        }
    }

    static void close(GameHub gameHub) {
        List<ServerSocket> list = listeners.remove(gameHub);
        if (list == null) {
            return;
        }
        for (ServerSocket serverSocket : list) {
            bindings.remove(serverSocket.getLocalPort());
            try {
                serverSocket.close();
            }
            catch (Exception exception) {}
        }
    }

    static ServerConfig configuration(ServerConfig serverConfig, Socket socket) {
        Binding binding = socket == null ? null : bindings.get(socket.getLocalPort());
        return binding == null ? serverConfig : binding.config();
    }

    static AccountRepository repository(AccountRepository accountRepository, Socket socket) {
        Binding binding = socket == null ? null : bindings.get(socket.getLocalPort());
        return binding == null ? accountRepository : accountRepository.repositoryForClassicSource(binding.name());
    }

    static boolean allowed(Socket socket, ServerConfig serverConfig) throws Exception {
        if (!SharedDatabase.enabled(serverConfig) || bindings.containsKey(socket.getLocalPort())) {
            return true;
        }
        socket.close();
        return false;
    }

    static String account(ClientSession clientSession, String string) {
        if (clientSession == null || string == null || string.isBlank()) {
            return string;
        }
        try {
            ServerConfig serverConfig = (ServerConfig)sessionConfig.get(clientSession);
            Binding binding = bindings.get(serverConfig.port());
            return binding == null ? string : "source:" + binding.id() + ":" + string;
        }
        catch (IllegalAccessException illegalAccessException) {
            throw new IllegalStateException(illegalAccessException);
        }
    }

    static {
        try {
            sessionConfig = ClientSession.class.getDeclaredField("config");
            sessionConfig.setAccessible(true);
        }
        catch (Exception exception) {
            throw new ExceptionInInitializerError(exception);
        }
    }

    record Binding(int id, String name, ServerConfig config) {
    }
}
