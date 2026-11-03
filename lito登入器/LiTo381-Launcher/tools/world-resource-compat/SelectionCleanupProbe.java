package local.lito381.server;
import java.lang.reflect.*;
import java.net.*;
import java.nio.file.*;
import java.util.*;

/** Exercise actual EOF cleanup after a partially completed second selection. */
public final class SelectionCleanupProbe {
    static void set(Object x,String k,Object v)throws Exception {
        var f=x.getClass().getDeclaredField(k);f.setAccessible(true);f.set(x,v);
    }
    public static void main(String[] args)throws Exception {
        Path root=Path.of(args[0]);Files.createDirectories(root);
        Path file=root.resolve("cleanup.properties");
        Files.writeString(file,"bind=127.0.0.1\nport=32100\ndatabase.mode=memory\n"
            +"database.read.only=true\nstate.persistence.enabled=false\n"
            +"native.tournament.economy.enabled=true\n"
            +"state.file="+root.resolve("state.properties").toString().replace('\\','/')+"\n"
            +"capture.directory="+root.resolve("capture").toString().replace('\\','/')+"\n");
        ServerConfig config=ServerConfig.load(file);
        try(AccountRepository accounts=AccountRepository.open(config);
            GameHub hub=new GameHub(config);
            CaptureLog capture=new CaptureLog(root.resolve("capture"));
            ServerSocket listener=new ServerSocket(0,1,InetAddress.getLoopbackAddress());
            Socket client=new Socket(InetAddress.getLoopbackAddress(),listener.getLocalPort());
            Socket server=listener.accept()) {
            var state=new GameStateStore(root.resolve("state.properties"),false,true);
            var session=new ClientSession(server,ClientSession.Endpoint.GAME,config,accounts,state,capture,hub);
            set(session,"account","test");set(session,"selectedClassicServerName","SelfTest");
            set(session,"selectedCharacter","Second");
            set(session,"inventoryCharacter",GameStateStore.sourceCharacter("SelfTest","First"));
            set(session,"nativeTournamentEconomyInitialized",true);
            set(session,"nativeTournamentMagicBaseline",SourceEquipmentMagicStats.none());
            set(session,"nativeTournamentVitalBaseline",SourceEquipmentVitalStats.none());
            boolean fault=args.length>1&&args[1].equals("fault");
            if(fault)set(session,"timedStatus",null);
            hub.connect(session);
            if(!hub.acceptAuthenticatedAccount(session,ClientSession.Endpoint.GAME,"test"))
                throw new AssertionError("initial account claim failed");
            client.shutdownOutput();
            Throwable failed=null;
            try { session.run(); } catch(Throwable e) {failed=e;e.printStackTrace(System.out);}
            var f=GameHub.class.getDeclaredField("connectedSessions");f.setAccessible(true);
            boolean retained=((Collection<?>)f.get(hub)).contains(session);
            var a=GameHub.class.getDeclaredField("authenticatedGameAccounts");a.setAccessible(true);
            boolean claimed=((Map<?,?>)a.get(hub)).containsValue(session);
            System.out.println("CLEANUP thrown="+(failed!=null)+" connected="+retained+" accountClaim="+claimed);
            if(retained||claimed||(!fault&&failed!=null))throw new AssertionError("EOF cleanup failed",failed);
            if(fault&&failed==null)throw new AssertionError("fault was not exercised");
            var next=new ClientSession(server,ClientSession.Endpoint.GAME,config,accounts,state,capture,hub);
            hub.connect(next);
            if(!hub.acceptAuthenticatedAccount(next,ClientSession.Endpoint.GAME,"test"))
                throw new AssertionError("same-account retry rejected");
            hub.disconnect(next);
            System.out.println("PASS same-account retry after "+(fault?"fault":"normal")+" EOF");
        }
    }
}
