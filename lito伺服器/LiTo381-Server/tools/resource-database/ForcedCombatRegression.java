package local.lito381.server;
import java.nio.file.*;import java.util.*;

public class ForcedCombatRegression {
 static void check(boolean b,String m){if(!b)throw new AssertionError(m);}
 static Object get(Object o,String n)throws Exception{return NativeDirectoryRegression.get(o,n);}
 static void set(Object o,String n,Object v)throws Exception{NativeDirectoryRegression.set(o,n,v);}
 static void call(Object o,String n,Class<?>[] t,Object... a)throws Exception{NativeDirectoryRegression.call(o,n,t,a);}
 static byte[] forced(byte[] p){var r=Arrays.copyOf(p,p.length+8);byte[] t={0x4c,0x46,0x43,0x31,p[0],1,0x67,(byte)0xa5};System.arraycopy(t,0,r,p.length,8);return r;}
 static byte[] spell(int skill,long id,int x,int y){return new Protocol.Writer().c(0x14).c((skill-1)>>3).c((skill-1)&7).d(id).h(x).h(y).bytes();}
 static void dispatch(ClientSession s,byte[] p)throws Exception{((Map<?,?>)get(s,"spellReadyNanos")).clear();call(s,"handle",new Class[]{byte[].class},p);}
 static void prepare(ClientSession s,long id,int x,int y)throws Exception {
  var m=Regression.class.getDeclaredMethod("prepareBattle",ClientSession.class,long.class,int.class,int.class);m.setAccessible(true);m.invoke(null,s,id,x,y);
  set(s,"playerHp",5000);set(s,"playerMaxHp",5000);set(s,"playerMp",1000);set(s,"playerMaxMp",1000);set(s,"learnedSkills",Set.of(1,4,10,11,19,25,45));
  set(s,"sourcePhysicalGrowthStats",SourcePhysicalGrowthStats.classic381());
 }
 public static void main(String[] a)throws Exception {
  byte[] plain=spell(4,42,32740,32798);check(Arrays.equals(ForcedCombat.unwrap(forced(plain)),plain),"extension decode");check(ForcedCombat.unwrap(plain)==plain,"legacy altered");
  byte[] bad=forced(plain);bad[bad.length-1]=0;try{ForcedCombat.unwrap(bad);throw new AssertionError("bad version");}catch(IllegalArgumentException expected){}
  for(int skill:new int[]{5,51,58,60,63,69,116,118})check(ForcedCombat.target(spell(skill,42,32740,32798))==0,"non-target layout");
  var config=ServerConfig.load(Path.of(a[0]),"standard");ResourceDatabase.load(config);var root=Path.of(a[1]);Files.createDirectories(root);
  var state=new GameStateStore(root.resolve("unused"),false,true);var accounts=new MemoryAccountRepository("fixture","fixture","",0,1,40,52);
  try(var capture=new CaptureLog(root.resolve("capture"));var hub=new GameHub(config,state,accounts);
      var f=new SourceLoginRegression.Fixture(config,accounts,state,capture,hub,0,ClientSession.Endpoint.GAME);
      var p=new SourceLoginRegression.Fixture(config,accounts,state,capture,hub,1,ClientSession.Endpoint.GAME);
      var q=new SourceLoginRegression.Fixture(config,accounts,state,capture,hub,0,ClientSession.Endpoint.GAME)) {
   NativeDirectoryRegression.init(f,"ForceA",0,config.classicServerNames().get(0));NativeDirectoryRegression.init(p,"ForceB",0,config.classicServerNames().get(1));
   NativeDirectoryRegression.init(q,"ForceC",0,config.classicServerNames().get(0));
   long room=hub.createRoom(0,new GameHub.RoomSettings("ForcedFixture","",8,4,0,0,0,0));hub.joinRoom(room,f.session);hub.joinRoom(room,p.session);
   prepare(f.session,room,32740,32798);prepare(p.session,room,32741,32798);
   hub.joinRoom(room,q.session);prepare(q.session,room,32741,32799);
   check(hub.roomRules(room).mode()==BattleRules.Mode.TIME_ATTACK,"fixture mode");check(!hub.canAttack(room,f.session,p.session),"ordinary cooperative PvP");
   byte[] arrow=spell(4,p.session.playerObjectId(),32741,32798);
   dispatch(f.session,arrow);check((int)get(p.session,"playerHp")==5000,"ordinary friendly damage");
   dispatch(f.session,forced(arrow));check((int)get(p.session,"playerHp")<5000,"forced Light Arrow did not damage friend");
   ((Map<Integer,Long>)get(f.session,"spellReadyNanos")).put(4,System.nanoTime()+1_000_000_000L);
   int mpAfter=(int)get(f.session,"playerMp");call(f.session,"handle",new Class[]{byte[].class},forced(arrow));check((int)get(f.session,"playerMp")==mpAfter,"force bypassed shared cooldown");
   check(!hub.canAttack(room,f.session,p.session),"force context leaked");
   int hp=(int)get(p.session,"playerHp");dispatch(f.session,arrow);check((int)get(p.session,"playerHp")==hp,"ordinary request inherited force");
   set(p.session,"positionX",32820);dispatch(f.session,forced(arrow));check((int)get(p.session,"playerHp")==hp,"forced range bypass");set(p.session,"positionX",32741);
   var ac=new Protocol.CipherState(0x37110001);var bc=new Protocol.CipherState(0x37110001);f.client.setSoTimeout(40);p.client.setSoTimeout(40);
   var packets=NativeDirectoryRegression.drain(p,bc);check(packets.stream().anyMatch(x->(x[0]&255)==Protocol.S_ATTACK),"missing encrypted attack presentation");NativeDirectoryRegression.drain(f,ac);
   for(int i=0;i<8;i++) {dispatch(f.session,forced(new Protocol.Writer().c(0x17).d(p.session.playerObjectId()).h(32741).h(32798).bytes()));}
   check((int)get(p.session,"playerHp")<hp,"forced melee did not hit friend");check(!hub.canAttack(room,f.session,p.session),"melee scope leaked");
   set(f.session,"inventory",RuntimeInventory.fromCatalogCounts(Map.of(40318L,10)));
   byte[] poison=spell(11,p.session.playerObjectId(),32741,32798);int mp=(int)get(f.session,"playerMp");dispatch(f.session,poison);check((int)get(f.session,"playerMp")==mp,"ordinary friendly poison charged");
   dispatch(f.session,forced(poison));check((int)get(f.session,"playerMp")<mp,"forced negative spell not admitted");
   check(((RuntimeInventory)get(f.session,"inventory")).countCatalog(40318L)==9,"forced negative reagent count");
   check(!hub.canAttack(room,f.session,p.session),"negative scope leaked");
   byte[] unknown=spell(4,0x12345678L,32741,32798);mp=(int)get(f.session,"playerMp");dispatch(f.session,forced(unknown));check((int)get(f.session,"playerMp")==mp,"unknown target charged");
   set(p.session,"playerHp",0);dispatch(f.session,forced(arrow));check((int)get(f.session,"playerMp")==mp,"dead target charged");set(p.session,"playerHp",hp);
   set(f.session,"observer",true);dispatch(f.session,forced(arrow));check((int)get(f.session,"playerMp")==mp,"observer cast");set(f.session,"observer",false);
   check(!hub.canAttack(room,f.session,p.session),"early-return scope leaked");
   hp=(int)get(p.session,"playerHp");dispatch(f.session,forced(spell(25,p.session.playerObjectId(),32741,32798)));
   check((int)get(p.session,"playerHp")<hp,"forced area primary did not take damage");check((int)get(q.session,"playerHp")==5000,"area force leaked to unselected ally");
   check(!hub.canAttack(room,f.session,p.session),"area scope leaked");
   System.out.println("PASS actual dispatch: cooperative normal rejection, forced Light Arrow/melee/poison/area primary, unselected ally protection, shared cooldown/range/dead/observer/unknown gates, MP and reagent costs, request scope isolation, encrypted presentation; no SQL writes");
  }
 }
}
