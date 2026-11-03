package local.lito381.server;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;

/** Native UI/transport fixture only: production ClientSession/GameHub, no SQL repository or writes. */
public class NativeSourceFixture {
 public static void main(String[] args) throws Exception {
  ServerConfig config=ServerConfig.load(Path.of(args[0]));
  var routes=new LinkedHashMap<String,AccountRepository>();
  for(int i=0;i<config.classicServerNames().size();i++)
   routes.put(config.classicServerNames().get(i),new MemoryAccountRepository("qatest","testpass","",0,1.0,40,52+i));
  ExecutorService clients=Executors.newCachedThreadPool();
  try(AccountRepository accounts=new SourceRoutedAccountRepository(routes);
      CaptureLog capture=new CaptureLog(Path.of(args[1]));
      GameHub hub=new GameHub(config,new GameStateStore(Path.of(args[1]).resolve("unused.state"),false,true),accounts)) {
   SourceLogin.start(config,accounts,new GameStateStore(Path.of(args[1]).resolve("unused2.state"),false,true),capture,hub,clients);
   System.out.println("NATIVE_FIXTURE_READY no_sql=true");System.out.flush();
   var input=new java.io.BufferedReader(new java.io.InputStreamReader(System.in));
   while(true){String line=input.readLine();if(line==null || line.equals("stop"))break;
    if(line.equals("state")){
     var f=GameHub.class.getDeclaredField("authenticatedGameAccounts");f.setAccessible(true);
     System.out.println("NATIVE_FIXTURE_LOCKS "+((Map<?,?>)f.get(hub)).size());System.out.flush();
    }
   }
  } finally {clients.shutdownNow();clients.awaitTermination(5,TimeUnit.SECONDS);}
 }
}
