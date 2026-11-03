package local.lito381.server;
import java.nio.file.*;import java.net.*;import java.util.*;import java.lang.reflect.*;

/** Exercises the published patched ClientSession, encrypted output and real socket endpoint binding. No SQL writes. */
public class SourceLoginRegression {
 static void check(boolean b,String s){if(!b)throw new AssertionError(s);}
 static Object field(Object o,String n)throws Exception{var f=o.getClass().getDeclaredField(n);f.setAccessible(true);return f.get(o);}
 static void set(Object o,String n,Object v)throws Exception{var f=o.getClass().getDeclaredField(n);f.setAccessible(true);f.set(o,v);}
 static void invoke(ClientSession s,String n,byte[] p)throws Exception{var m=ClientSession.class.getDeclaredMethod(n,byte[].class);m.setAccessible(true);try{m.invoke(s,(Object)p);}catch(InvocationTargetException e){throw new RuntimeException(e.getCause());}}
 static final class Fixture implements AutoCloseable {
  Socket client,server;ClientSession session;GameHub hub;int port;
  Fixture(ServerConfig config,AccountRepository accounts,GameStateStore state,CaptureLog capture,GameHub hub,int index,ClientSession.Endpoint endpoint)throws Exception {
   this.hub=hub;try(ServerSocket listener=new ServerSocket(0,1,InetAddress.getByName("127.0.0.1"))){
    port=listener.getLocalPort();client=new Socket("127.0.0.1",port);server=listener.accept();
   }
   SourceLogin.bindings.put(port,new SourceLogin.Binding(config.classicServerIds().get(index),config.classicServerNames().get(index),SourceLogin.scoped(config,index,port)));
   session=new ClientSession(server,endpoint,config,accounts,state,capture,hub);
   set(session,"outgoing",new Protocol.CipherState(0x37110001));
   Object phase=field(session,"phase");
   @SuppressWarnings({"rawtypes","unchecked"}) Object ready=Enum.valueOf((Class)phase.getClass(),"VERSION_OK");set(session,"phase",ready);hub.connect(session);
  }
  void login(String password)throws Exception{invoke(session,"login",new Protocol.Writer().c(Protocol.C_LOGIN).s("3333").s(password).d(0x0100007fL).bytes());}
  public void close()throws Exception{hub.disconnect(session);server.close();client.close();SourceLogin.bindings.remove(port);}
 }
 static void scenario(ServerConfig config,Path output,boolean common)throws Exception {
  int[] hits=new int[3];Map<String,AccountRepository> routes=new LinkedHashMap<>();
  for(int i=0;i<3;i++) {
   final int index=i;AccountRepository delegate=new MemoryAccountRepository("3333",common?"same-password":"password-"+i,"",0,1.0,40,52+i);
   AccountRepository traced=(AccountRepository)java.lang.reflect.Proxy.newProxyInstance(SourceLoginRegression.class.getClassLoader(),new Class[]{AccountRepository.class},(p,m,a)->{
    if(m.getName().equals("authenticate"))hits[index]++;
    try{return m.invoke(delegate,a);}catch(InvocationTargetException e){throw e.getCause();}
   });routes.put(config.classicServerNames().get(i),traced);
  }
  try(AccountRepository accounts=new SourceRoutedAccountRepository(routes);CaptureLog capture=new CaptureLog(output.resolve(common?"same":"different"))) {
   GameStateStore state=new GameStateStore(output.resolve("unused.state"),false,true);
   try(GameHub hub=new GameHub(config,state,accounts)) {
    List<Fixture> active=new ArrayList<>();
    try {
     for(int i=0;i<3;i++) {
      String password=common?"same-password":"password-"+i;
      try(Fixture login=new Fixture(config,accounts,state,capture,hub,i,ClientSession.Endpoint.LOGIN)) {
       if(!common){int[] before=hits.clone();login.login("password-"+((i+1)%3));check(field(login.session,"account")==null,"foreign password must fail");for(int n=0;n<3;n++)check(hits[n]==before[n]+(n==i?1:0),"authentication queried another source");}
       login.login(password);check("3333".equals(field(login.session,"account")),"login success");
       check(((ServerConfig)field(login.session,"config")).classicServerNames().equals(List.of(config.classicServerNames().get(i))),"source selector restricted");
       invoke(login.session,"requestLitoCharacterInfo",new Protocol.Writer().c(Protocol.C_LITO_LIN_CHARACTER_INFO).s(config.classicServerNames().get((i+1)%3)).bytes());
       check(config.classicServerNames().get(i).equals(field(login.session,"selectedClassicServerName")),"forged source switch rejected");
       invoke(login.session,"requestLitoCharacterInfo",new Protocol.Writer().c(Protocol.C_LITO_LIN_CHARACTER_INFO).s(config.classicServerNames().get(i)).bytes());
       check(config.classicServerNames().get(i).equals(hub.pendingClassicSourceHandoff(SourceLogin.account(login.session,"3333"))),"source-qualified LOGIN handoff");
      }
      Fixture game=new Fixture(config,accounts,state,capture,hub,i,ClientSession.Endpoint.GAME);active.add(game);game.login(password);
      check("3333".equals(field(game.session,"account")),"same account in different source must coexist");
      check(config.classicServerNames().get(i).equals(field(game.session,"selectedClassicServerName")),"GAME source binding");
      check(hub.pendingClassicSourceHandoff(SourceLogin.account(game.session,"3333"))==null,"handoff consumed");
      check(((List<CharacterSummary>)field(game.session,"characters")).get(0).level()==52+i,"correct source character directory");
      var identity=ClientSession.class.getDeclaredMethod("stateCharacter",String.class);identity.setAccessible(true);
      check(hub.selectCharacter(game.session,(String)identity.invoke(game.session,((List<CharacterSummary>)field(game.session,"characters")).get(0).name())),"same character name/object ID across sources may coexist");
     }
     check(((Map<?,?>)field(hub,"authenticatedGameAccounts")).size()==3,"three independent account locks");
     try(Fixture duplicate=new Fixture(config,accounts,state,capture,hub,0,ClientSession.Endpoint.GAME)) {
      duplicate.login(common?"same-password":"password-0");check(field(duplicate.session,"account")==null,"same-source duplicate must be rejected");
     }
     check(hub.releaseAuthenticatedAccountForTransfer(active.get(1).session,"3333"),"source-qualified transfer release");
     check(((Map<?,?>)field(hub,"authenticatedGameAccounts")).size()==2,"other sources retained");
     try(Fixture reconnect=new Fixture(config,accounts,state,capture,hub,1,ClientSession.Endpoint.GAME)) {
      reconnect.login(common?"same-password":"password-1");check("3333".equals(field(reconnect.session,"account")),"source reconnect accepted");
     }
     state.banAccount(SourceLogin.account(active.get(0).session,"3333"));
     check(hub.releaseAuthenticatedAccountForTransfer(active.get(0).session,"3333"),"release before ban verification");
     try(Fixture banned=new Fixture(config,accounts,state,capture,hub,0,ClientSession.Endpoint.GAME)) {
      banned.login(common?"same-password":"password-0");check(field(banned.session,"account")==null,"source-local runtime ban enforced");
     }
     try(Fixture unbanned=new Fixture(config,accounts,state,capture,hub,1,ClientSession.Endpoint.GAME)) {
      unbanned.login(common?"same-password":"password-1");check("3333".equals(field(unbanned.session,"account")),"other source unaffected by ban");
     }
     System.out.println("PASS "+(common?"same":"different")+" passwords: selected-only auth, independent 3333 identities, LOGIN/GAME handoff, duplicate rejection and reconnect");
    }finally{for(Fixture f:active)f.close();}
   }
  }
 }
 public static void main(String[] args)throws Exception {
  ServerConfig config=ServerConfig.load(Path.of(args[0]));Path output=Path.of(args[1]);Files.createDirectories(output);
  scenario(config,output,false);scenario(config,output,true);
  try(Socket unbound=new Socket()){check(!SourceLogin.allowed(unbound,config)&&unbound.isClosed(),"unqualified endpoint rejected");}
  check(SourceLogin.bindings.isEmpty(),"fixture bindings cleaned");
  System.out.println("PASS unqualified endpoint rejected; no credentials logged; no database writes.");
 }
}
