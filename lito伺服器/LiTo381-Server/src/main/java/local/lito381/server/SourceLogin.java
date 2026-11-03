// ORIGINAL SOURCE moved from lito伺服器/LiTo381-Server/tools/shared-database/SourceLogin.java (T002); replaces CFR decompile of reference/base-0.6.71.jar
package local.lito381.server;

import java.net.*;
import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.*;

/** Source is fixed by the listening endpoint BEFORE any account credentials arrive. */
final class SourceLogin {
    record Binding(int id, String name, ServerConfig config) {}
    static final Map<Integer, Binding> bindings = new ConcurrentHashMap<>();
    static final Map<GameHub, List<ServerSocket>> listeners = new ConcurrentHashMap<>();
    static final Field sessionConfig;
    static { try { sessionConfig=ClientSession.class.getDeclaredField("config");sessionConfig.setAccessible(true); }
        catch(Exception e){throw new ExceptionInInitializerError(e);} }

    static ServerConfig scoped(ServerConfig original, int index, int port) throws Exception {
        RecordComponent[] fields=ServerConfig.class.getRecordComponents();
        Class<?>[] types=new Class<?>[fields.length];Object[] values=new Object[fields.length];
        for(int n=0;n<fields.length;n++) {
            var f=fields[n];types[n]=f.getType();Object value=f.getAccessor().invoke(original);
            values[n]=switch(f.getName()) {
                case "port", "advertisedPort" -> port;
                case "channelPorts" -> List.of(port);
                case "classicServerNames", "classicServerIds", "legacySourceDatabases",
                     "legacySourceResourceDatabases", "legacySourceWeightRates" -> List.of(((List<?>)value).get(index));
                default -> value;
            };
        }
        return ServerConfig.class.getDeclaredConstructor(types).newInstance(values);
    }
    static void start(ServerConfig config, AccountRepository accounts, GameStateStore state,
                      CaptureLog capture, GameHub hub, ExecutorService clients) throws Exception {
        if(!SharedDatabase.enabled(config))return;
        int base=Integer.parseInt(SharedDatabase.properties(config.source()).getProperty("login.source.port.base","32000"));
        if(base<1024 || base>65527)throw new IllegalArgumentException("Invalid login.source.port.base");
        if(config.dedicatedChannelPortsEnabled())throw new IllegalArgumentException("Source login requires shared logical channels");
        List<ServerSocket> opened=new ArrayList<>();List<Runnable> tasks=new ArrayList<>();List<Integer> registered=new ArrayList<>();
        try {
            for(int i=0;i<config.classicServerNames().size();i++) {
                int id=config.classicServerIds().get(i),port=base+id;
                if(port==config.port() || config.channelPorts().contains(port))throw new IllegalArgumentException("Source port overlaps base listener");
                Binding binding=new Binding(id,config.classicServerNames().get(i),scoped(config,i,port));
                if(bindings.putIfAbsent(port,binding)!=null)throw new IllegalArgumentException("Duplicate source port");
                registered.add(port);
                for(var endpoint:ClientSession.Endpoint.values()) {
                    ServerSocket server=new ServerSocket();opened.add(server);
                    String address=endpoint==ClientSession.Endpoint.LOGIN?config.bind():config.gameBind();
                    server.bind(new InetSocketAddress(InetAddress.getByName(address),port),16);
                    tasks.add(()->{
                        try { while(!server.isClosed()) {
                            Socket socket=server.accept();
                            try {clients.submit(new ClientSession(socket,endpoint,config,accounts,state,capture,hub));}
                            catch(Exception e){socket.close();throw e;}
                        }} catch(Exception e){if(!server.isClosed())System.out.println("[LiTo] source-listener failed: "+e.getClass().getSimpleName());}
                    });
                }
                System.out.println("[LiTo] source-login id="+id+" name="+binding.name()+" port="+port+" auth=selected-source-only");
            }
            listeners.put(hub,opened);
            for(Runnable task:tasks)clients.submit(task);
        } catch(Exception failure){for(var s:opened)try{s.close();}catch(Exception ignored){}for(int p:registered)bindings.remove(p);throw failure;}
    }
    static void close(GameHub hub) {
        var sockets=listeners.remove(hub);if(sockets==null)return;
        for(var socket:sockets){bindings.remove(socket.getLocalPort());try{socket.close();}catch(Exception ignored){}}
    }
    static ServerConfig configuration(ServerConfig config,Socket socket) {
        Binding b=socket==null?null:bindings.get(socket.getLocalPort());return b==null?config:b.config();
    }
    static AccountRepository repository(AccountRepository repository,Socket socket) {
        Binding b=socket==null?null:bindings.get(socket.getLocalPort());return b==null?repository:repository.repositoryForClassicSource(b.name());
    }
    static boolean allowed(Socket socket,ServerConfig config) throws Exception {
        if(!SharedDatabase.enabled(config) || bindings.containsKey(socket.getLocalPort()))return true;
        socket.close();return false; // The old unqualified endpoint can never authenticate across databases.
    }
    static String account(ClientSession session,String raw) {
        if(session==null || raw==null || raw.isBlank())return raw;
        try {
            ServerConfig config=(ServerConfig)sessionConfig.get(session);
            Binding b=bindings.get(config.port());
            return b==null?raw:"source:"+b.id()+":"+raw;
        } catch(IllegalAccessException e){throw new IllegalStateException(e);}
    }
}
