package local.lito381.server;
import java.nio.file.*;
import java.net.*;
import java.lang.reflect.*;
import java.util.*;
public class AreaRegression {
 static Object get(Object o,String n)throws Exception{var f=o.getClass().getDeclaredField(n);f.setAccessible(true);return f.get(o);}
 static void set(Object o,String n,Object v)throws Exception{var f=o.getClass().getDeclaredField(n);f.setAccessible(true);f.set(o,v);}
 static Object call(Object o,String n,Class<?>[] t,Object...v)throws Exception{var m=o.getClass().getDeclaredMethod(n,t);m.setAccessible(true);try{return m.invoke(o,v);}catch(InvocationTargetException e){throw new RuntimeException(n,e.getCause());}}
 @SuppressWarnings({"unchecked","rawtypes"}) static void phase(ClientSession s,String p)throws Exception{set(s,"phase",Enum.valueOf((Class)Class.forName(ClientSession.class.getName()+"$Phase"),p));}
 static void check(boolean b,String n){if(!b)throw new AssertionError(n);}
 static CharacterSummary character(String n,long id){return new CharacterSummary(n,"",2,0,50,1000,1000,10,12,12,12,12,18,12,18,12,id,100,0,0,0,32740,32798,4,12,40,1,12,12,0);}
 static void init(ClientSession s,String n,long id)throws Exception{
  var c=character(n,id);set(s,"account",n);set(s,"selectedCharacter",n);set(s,"characters",List.of(c));set(s,"inventory",new RuntimeInventory());
  set(s,"sourceEquipmentMagicStats",SourceEquipmentMagicStats.none());set(s,"sourceEquipmentVitalStats",SourceEquipmentVitalStats.none());
  set(s,"sourceEquippedFireResistance",new SourceEquippedFireResistance(0,0));set(s,"sourceBaseMagicResistance",SourceBaseMagicResistance.from(c));
  set(s,"sourceInventoryWeightStats",SourceInventoryWeightStats.none(100));set(s,"retainedSourceInventoryWeight",0);
  set(s,"learnedSkills",Set.of(1,4,19,35,57));set(s,"outgoing",new Protocol.CipherState(123));
 }
 @SuppressWarnings("unchecked") static void cast(ClientSession s,int id,long target,int x,int y)throws Exception{
  ((Map<Integer,Long>)get(s,"spellReadyNanos")).clear();
  var w=new Protocol.Writer().c(0x14).c((id-1)/8).c((id-1)%8).d(target);
  if(BattleSpellCatalog.attackSpell(id)!=null || BattleSpellCatalog.areaAttackSpell(id)!=null)w.h(x).h(y);
  call(s,"useSpell",new Class[]{byte[].class},w.bytes());
 }
 public static void main(String[] args)throws Exception{
  Path root=Path.of(args[1]);Files.createDirectories(root);var cfg=ServerConfig.load(Path.of(args[0]));
  var state=new GameStateStore(root.resolve("target.state"),false);
  try(var accounts=AccountRepository.open(cfg);var hub=new GameHub(cfg);var listener=new ServerSocket(0,2,InetAddress.getLoopbackAddress());
      var ca=new Socket(InetAddress.getLoopbackAddress(),listener.getLocalPort());var sa=listener.accept();
      var cb=new Socket(InetAddress.getLoopbackAddress(),listener.getLocalPort());var sb=listener.accept()){
   var a=new ClientSession(sa,ClientSession.Endpoint.GAME,cfg,accounts,state,new CaptureLog(root.resolve("a")),hub);
   var b=new ClientSession(sb,ClientSession.Endpoint.GAME,cfg,accounts,state,new CaptureLog(root.resolve("b")),hub);
   init(a,"TargetCaster",0x38102001);init(b,"TargetReceiver",0x38102002);
   hub.connect(a);hub.connect(b);hub.enterChannel(0,a);hub.enterChannel(0,b);
   long room=hub.createRoom(0,new GameHub.RoomSettings("Targets","",2,1,0,0,0,0));
   hub.joinRoom(room,a);hub.joinRoom(room,b);hub.setForce(room,a,0);hub.setForce(room,b,1);hub.setReady(room,a,true);hub.setReady(room,b,true);
   long future=System.nanoTime()+60_000_000_000L;
   for(int i=0;i<3;i++)call(hub,"countdownWork",new Class[]{long.class},future+i*2_000_000_000L);
   for(var s:List.of(a,b)){phase(s,"BATTLE");set(s,"currentRoomId",room);set(s,"positionX",32740);set(s,"positionY",32798);set(s,"playerHp",800);set(s,"playerMaxHp",1000);set(s,"playerMp",1000);set(s,"playerMaxMp",1000);}
   set(b,"positionX",32741);
   set(a,"learnedSkills",Set.of(25));
   BattleCompanion companion=(BattleCompanion)get(a,"battleCompanion");
   var original=companion.snapshot();
   companion=BattleCompanion.tamed(original.objectId(),original.npcId(),original.gfx(),"OwnTarget",50,5000,5000,1000,0,12,12,32741,32799);
   set(a,"battleCompanion",companion);
   call(companion,"activate",new Class[]{int.class,int.class,int.class,int.class},32741,32799,5000,1000);
   long actor=companion.snapshot().objectId();
   int hp=companion.snapshot().hp();int enemyHp=(int)get(b,"playerHp");int mp=(int)get(a,"playerMp");
   cast(a,25,actor,0,0);
   check(companion.snapshot().hp()<hp,"Burning Fireball own summon HP decreases");
   check((int)get(b,"playerHp")<enemyHp,"area includes nearby hostile player");
   check((int)get(a,"playerMp")<mp,"successful cast consumes MP");
   check((int)call(a,"battleExitRemainingSeconds",new Class[]{})==10,"companion attack combat timer");
   hp=companion.snapshot().hp();mp=(int)get(a,"playerMp");
   call(a,"useSpell",new Class[]{byte[].class},new Protocol.Writer().c(0x14).c(3).c(0).d(actor).h(32741).h(32799).bytes());
   check(companion.snapshot().hp()==hp && (int)get(a,"playerMp")==mp,"cooldown repeat costs nothing");
   companion.relocate(32000,32000);cast(a,25,actor,32741,32799);
   check((int)get(a,"playerMp")==mp,"server position overrides forged near coordinates");
   companion.relocate(32741,32799);companion.applyDamage(100000);cast(a,25,actor,32741,32799);
   check((int)get(a,"playerMp")==mp,"dead summon rejected before cost");
   cast(a,25,0x7fffff,32741,32799);check((int)get(a,"playerMp")==mp,"unknown target rejected before cost");
   System.out.println("PASS: Burning Fireball own summon + mixed nearby player, authoritative range, cooldown, dead/unknown rejection, MP and combat timer");

  }
 }
}


