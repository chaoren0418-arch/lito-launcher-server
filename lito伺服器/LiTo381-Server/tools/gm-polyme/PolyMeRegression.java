package local.lito381.server;
import java.nio.file.*;import java.lang.reflect.*;import java.util.*;import java.util.concurrent.*;import java.util.concurrent.atomic.*;
public class PolyMeRegression {
 static int checks;
 static void check(boolean ok,String name){checks++;if(!ok)throw new AssertionError(name);}
 static Object get(ClientSession s,String n)throws Exception{return GmTravel.get(s,n);}
 static void set(ClientSession s,String n,Object v)throws Exception{GmTravel.set(s,n,v);}
 static Object call(ClientSession s,String n,Class<?>[] t,Object...v)throws Exception{return GmTravel.call(s,n,t,v);}
 public static void main(String[] a)throws Exception {
  var config=ServerConfig.load(Path.of(a[0]),"standard");Path root=Path.of(a[1]);Files.createDirectories(root);
  BattlePolymorphCatalog.Form form;
  try(var sources=SharedDatabase.open(config)){form=GmPolyMe.readForm(sources.repositoryForClassicSource(config.classicServerNames().get(0)),6080);}
  check(form.gfx()==6080&&form.minimumLevel()==200,"shared source metadata, GM level bypass fixture");
  var access=new AtomicInteger(0);var writes=new AtomicInteger();var loads=new AtomicInteger();
  var mode=new AtomicInteger(0);
  AccountRepository base=new MemoryAccountRepository("fixture","fixture","",0,1,40,52);
  AccountRepository accounts=(AccountRepository)Proxy.newProxyInstance(PolyMeRegression.class.getClassLoader(),new Class[]{AccountRepository.class},(proxy,m,args)->{
   if(m.getName().equals("repositoryForClassicSource"))return proxy;
   if(m.getName().equals("accessLevel"))return access.get();
   if(m.getName().startsWith("persist")||m.getName().startsWith("save")){writes.incrementAndGet();throw new AssertionError("source write "+m.getName());}
   if(m.getName().equals("gmCommand")){if(mode.get()==1)return null;if(mode.get()==2)throw new java.sql.SQLException("fixture");return new GmCommandDefinition("polyme",200,mode.get()==3?"L1Move":"L1PolyMe","",mode.get()==4);}
   try{return m.invoke(base,args);}catch(InvocationTargetException e){throw e.getCause();}
  });
  GmPolyMe.Forms loader=(repo,gfx)->{loads.incrementAndGet();if(gfx!=6080)throw new IllegalArgumentException("unsupported fixture");return form;};
  var state=new GameStateStore(root.resolve("must-not-write"),false,true);
  try(var capture=new CaptureLog(root.resolve("capture"));var hub=new GameHub(config,state,accounts);
      var f=new SourceLoginRegression.Fixture(config,accounts,state,capture,hub,0,ClientSession.Endpoint.GAME);
      var peer=new SourceLoginRegression.Fixture(config,accounts,state,capture,hub,0,ClientSession.Endpoint.GAME)){
   var clock=(ScheduledExecutorService)NativeDirectoryRegression.get(hub,"battleClock");clock.shutdownNow();check(clock.awaitTermination(2,TimeUnit.SECONDS),"fixture clock stopped");
   NativeDirectoryRegression.init(f,"PolyA",0,config.classicServerNames().get(0));NativeDirectoryRegression.init(peer,"PolyB",0,config.classicServerNames().get(0));set(f.session,"account","fixture");
   long room=hub.createRoom(0,new GameHub.RoomSettings("PolyMe","",8,1,0,0,0,0));hub.joinRoom(room,f.session);hub.joinRoom(room,peer.session);
   var prepare=Regression.class.getDeclaredMethod("prepareBattle",ClientSession.class,long.class,int.class,int.class);prepare.setAccessible(true);
   var origin=ArenaMapCatalog.forMap(hub.battleMapId(room,f.session)).spawn(0);prepare.invoke(null,f.session,room,origin.x(),origin.y());prepare.invoke(null,peer.session,room,origin.x()+1,origin.y());
   var cipher=new Protocol.CipherState(0x37110001);var other=new Protocol.CipherState(0x37110001);f.client.setSoTimeout(30);peer.client.setSoTimeout(30);
   NativeDirectoryRegression.drain(f,cipher);NativeDirectoryRegression.drain(peer,other);
   var status=(BattlePolymorphStatus)get(f.session,"polymorphStatus");
   // Actual chat dispatch must reach new command and reject a non-GM before SQL/form access.
   check((boolean)call(f.session,"handleGmChatCommand",new Class[]{String.class},".polyme 6080"),"actual chat hook");
   check(NativeDirectoryRegression.drain(f,cipher).stream().anyMatch(p->new String(p,Protocol.LEGACY_TEXT).contains("GM 權限不足")),"visible denial from real route");
   access.set(200);SourceAdministration.clear(f.session);
   for(int invalid=1;invalid<=4;invalid++){mode.set(invalid);GmPolyMe.command(f.session,".polyme 6080",loader);check(status.active(System.nanoTime())==null,"policy disabled/mismatch/failure");}mode.set(0);
   for(String input:List.of(".polyme",".polyme 0",".polyme -1",".polyme abc",".polyme 65536",".polyme 6080 1")){GmPolyMe.command(f.session,input,loader);check(status.active(System.nanoTime())==null,"bad argument "+input);}
   check(loads.get()==0,"bad requests never query forms");
   check(!GmPolyMe.command(f.session,".loc",loader),"other commands remain routed");
   set(f.session,"observer",true);GmPolyMe.command(f.session,".polyme 6080",loader);check(status.active(System.nanoTime())==null,"observer rejected");set(f.session,"observer",false);
   int hp=(int)get(f.session,"playerHp"),mp=(int)get(f.session,"playerMp");var inventory=((RuntimeInventory)get(f.session,"inventory")).catalogCounts();
   set(f.session,"playerHp",0);GmPolyMe.command(f.session,".polyme 6080",loader);check(status.active(System.nanoTime())==null,"dead rejected");set(f.session,"playerHp",hp);
   NativeDirectoryRegression.drain(f,cipher);NativeDirectoryRegression.drain(peer,other);
   GmPolyMe.command(f.session,".PoLyMe 6080",loader);check(status.active(System.nanoTime())==form,"valid GM activated despite minimum level");
   int walk=(int)call(f.session,"battleWalkAction",new Class[]{});byte[] expected=Protocol.polymorph(f.session.playerObjectId(),6080,walk);
   check(NativeDirectoryRegression.drain(f,cipher).stream().anyMatch(p->Arrays.equals(p,expected)),"self native appearance packet");check(NativeDirectoryRegression.drain(peer,other).stream().anyMatch(p->Arrays.equals(p,expected)),"peer native appearance packet");
   long expires=(long)NativeDirectoryRegression.get(status,"expiresAtNanos");check(Math.abs(expires-System.nanoTime()-TimeUnit.SECONDS.toNanos(7200))<TimeUnit.SECONDS.toNanos(2),"7200 second duration");
   GmPolyMe.command(f.session,".polyme 6080",loader);check((long)NativeDirectoryRegression.get(status,"expiresAtNanos")>expires,"repeat refreshes lease");
   check(hp==(int)get(f.session,"playerHp")&&mp==(int)get(f.session,"playerMp")&&inventory.equals(((RuntimeInventory)get(f.session,"inventory")).catalogCounts()),"no HP MP or item consumption");
   NativeDirectoryRegression.drain(f,cipher);NativeDirectoryRegression.drain(peer,other);
   NativeDirectoryRegression.set(status,"expiresAtNanos",System.nanoTime()-1);f.session.serverExpireTimedStatuses(System.nanoTime());
   check(status.active(System.nanoTime())==null,"existing expiry clears form");int baseGfx=(int)call(f.session,"battleGfx",new Class[]{});check(baseGfx!=6080,"base appearance restored");
   final byte[] restored=Protocol.polymorph(f.session.playerObjectId(),baseGfx,(int)call(f.session,"battleWalkAction",new Class[]{}));
   check(NativeDirectoryRegression.drain(peer,other).stream().anyMatch(p->Arrays.equals(p,restored)),"expiration broadcast");
   GmPolyMe.command(f.session,".polyme 6080",(repo,gfx)->{NativePlayerCommit.selected(f.session,f.session.selectedCharacterName());return form;});check(status.active(System.nanoTime())==null,"role replacement during DB read rejected");
   GmPolyMe.command(f.session,".polyme 6080",loader);check(status.active(System.nanoTime())!=null,"reactivate before exit");call(f.session,"exitRoom",new Class[]{});check(get(f.session,"phase").toString().equals("RESULT"),"exit enters settlement");call(f.session,"exitRoom",new Class[]{});check(status.active(System.nanoTime())==null,"settlement acknowledgement cleanup");
   GmPolyMe.command(f.session,".polyme 6080",loader);check(status.active(System.nanoTime())==null,"lobby rejected");
   check(writes.get()==0&&!Files.exists(root.resolve("must-not-write")),"memory only, no persistence");
  } finally {NativePlayerCommit.closeFixture();}
  check(NativePlayerLifetime.allIdle(),"owned jobs cleaned");System.out.println("PASS GM polyme checks="+checks+"; real chat hook; SQL read-only; permissions; malformed inputs; self/peer frames; refresh/expiry; role invalidation; exit; memory-only cleanup");
 }
}
