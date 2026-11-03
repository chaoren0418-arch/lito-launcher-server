package local.lito381.server;
import java.nio.file.*;
import java.net.*;
import java.lang.reflect.*;
import java.util.*;
public class BotRegression {
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
 static ServerConfig fixtures(ServerConfig base)throws Exception {
  var components=ServerConfig.class.getRecordComponents();Class<?>[] types=new Class[components.length];Object[] values=new Object[components.length];
  Map<String,Integer> overrides=Map.of("darkBlindFixtureRoll",1,"weaknessFixtureDieRoll",1,"weaknessFixturePercentileRoll",1,"manaDrainFixtureDieRoll",1,"manaDrainFixturePercentileRoll",1,"manaDrainFixtureTransferRoll",1);
  for(int i=0;i<components.length;i++){types[i]=components[i].getType();var m=components[i].getAccessor();m.setAccessible(true);values[i]=overrides.containsKey(components[i].getName())?overrides.get(components[i].getName()):m.invoke(base);}
  var constructor=ServerConfig.class.getDeclaredConstructor(types);constructor.setAccessible(true);return constructor.newInstance(values);
 }
 public static void main(String[] args)throws Exception{
  Path root=Path.of(args[1]);Files.createDirectories(root);var cfg=fixtures(ServerConfig.load(Path.of(args[0])));
  var state=new GameStateStore(root.resolve("target.state"),false);
  try(var accounts=AccountRepository.open(cfg);var hub=new GameHub(cfg);var listener=new ServerSocket(0,2,InetAddress.getLoopbackAddress());
      var ca=new Socket(InetAddress.getLoopbackAddress(),listener.getLocalPort());var sa=listener.accept();
      var cb=new Socket(InetAddress.getLoopbackAddress(),listener.getLocalPort());var sb=listener.accept()){
   var a=new ClientSession(sa,ClientSession.Endpoint.GAME,cfg,accounts,state,new CaptureLog(root.resolve("a")),hub);
   var b=new ClientSession(sb,ClientSession.Endpoint.GAME,cfg,accounts,state,new CaptureLog(root.resolve("b")),hub);
   init(a,"TargetCaster",0x38102001);init(b,"TargetReceiver",0x38102002);
   hub.connect(a);hub.enterChannel(0,a);
   long room=hub.createRoom(0,new GameHub.RoomSettings("Targets","",2,1,0,0,0,0));
   hub.joinRoom(room,a);hub.setForce(room,a,0);hub.setReady(room,a,true);
   long future=System.nanoTime()+60_000_000_000L;
   for(int i=0;i<3;i++)call(hub,"countdownWork",new Class[]{long.class},future+i*2_000_000_000L);
   for(var s:List.of(a,b)){phase(s,"BATTLE");set(s,"currentRoomId",room);set(s,"positionX",32740);set(s,"positionY",32798);set(s,"playerHp",800);set(s,"playerMaxHp",1000);set(s,"playerMp",1000);set(s,"playerMaxMp",1000);}
   set(b,"positionX",32741);
   var pos=hub.botPosition(room);check(pos!=null,"started bot has position");set(a,"positionX",pos.x()+1);set(a,"positionY",pos.y());
   set(a,"learnedSkills",Set.of(40,47,39));
   var inventory=(RuntimeInventory)get(a,"inventory");
   for(int id:new int[]{40,47}){
    Object spell=id==40?BattleSpellCatalog.blindSpell(id):BattleSpellCatalog.weaknessSpell(id);
    int reagent=((Number)call(spell,"itemConsumeId",new Class[]{})).intValue();
    int amount=((Number)call(spell,"itemConsumeCount",new Class[]{})).intValue();
    if(reagent!=0)inventory.add(InventoryCatalog.require(reagent),amount+10);
    ((Map<?,?>)get(a,"spellReadyNanos")).clear();
    byte[] packet=new Protocol.Writer().c(0x14).c((id-1)/8).c((id-1)%8).d(2).h(pos.x()).h(pos.y()).bytes();
    int mp=(int)get(a,"playerMp");call(a,"useSpell",new Class[]{byte[].class},packet);check((int)get(a,"playerMp")<mp,"bot status cost "+id);
   }
   long now=System.nanoTime();check(LiToBotEffects.blind(hub,room,now),"bot actual blind lease");check(LiToBotEffects.damageModifier(hub,room,now)<0,"bot actual weakness damage");
   int before=LiToBotEffects.mp(hub,room);int id=39;((Map<?,?>)get(a,"spellReadyNanos")).clear();call(a,"useSpell",new Class[]{byte[].class},new Protocol.Writer().c(0x14).c((id-1)/8).c((id-1)%8).d(2).h(pos.x()).h(pos.y()).bytes());check(LiToBotEffects.mp(hub,room)<before,"bot MP actually drained");
   set(a,"positionX",pos.x()+5);set(a,"positionY",pos.y());
   var ticks=(List<?>)call(hub,"botTicks",new Class[]{long.class},System.nanoTime());check(ticks.isEmpty(),"blind bot cannot pursue distant player");
   check(!LiToBotEffects.blind(hub,room,now+2_000_000_000_000L),"blind expiry");check(LiToBotEffects.damageModifier(hub,room,now+2_000_000_000_000L)==0,"weakness expiry");
   Object rooms=get(hub,"rooms");Object roomObject=((Map<?,?>)rooms).get(room);Field generation=roomObject.getClass().getDeclaredField("battleGeneration");generation.setAccessible(true);generation.setLong(roomObject,generation.getLong(roomObject)+1);
   check(LiToBotEffects.mp(hub,room)==100&&!LiToBotEffects.blind(hub,room,now),"new battle resets bot MP/status");
   System.out.println("PASS: real bot dark/weakness/drain packets, reagent/MP cost, status/AI/MP changes, expiry and room generation reset");

  }
 }
}


