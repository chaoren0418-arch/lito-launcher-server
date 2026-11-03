package local.lito381.server;
import java.nio.file.*;import java.util.*;import java.util.concurrent.*;
/** Original native item dispatch, expired Decay Potion and actual registered scene boundaries. */
public final class WorldPotionProbe extends DecayPotionMaterialProbe {
 static int gaps;
 static void exercise(ClientSession pc,ClientSession peer,GameHub hub,boolean world,int mode)throws Exception{
  setup(pc,peer,hub,world);selectedSkill=71;selectedMode=0;probability(71);var row=BattleSpellCatalog.decayPotionSpell(71);
  int potion=mode==6?40012:40010;NativeDeItemPublication.grant(peer,potion,3);var drink=peer.nativeDeInventory(0).items().stream().filter(i->i.definition().catalogId()==potion).findFirst().orElseThrow();
  need(InventoryCatalog.blockedByDecayPotion(drink.definition()),"actual potion eligibility");
  if(mode==0||mode==5)NativeDeItemPublication.grant(pc,Math.toIntExact(row.itemConsumeId()),row.itemConsumeCount()+3);
  try(var wire=new SourceActionPairProbe.Wire(pc,peer);var other=new SourceActionPairProbe.Wire(peer,pc)){
   if(mode==0||mode==5){cast(pc,71,row);need(active(peer,71),"real targeted Decay Potion applied before drinking");}
   if(mode==2)set(peer,"observer",true);if(mode==3)set(peer,"playerHp",0);
   if(mode==4){if(world)set(peer,"classicWorldMapId",(int)get(peer,"classicWorldMapId")+1);else set(peer,"phase",Enum.valueOf((Class<Enum>)get(peer,"phase").getClass(),"LOBBY"));}
   int hp=(int)get(peer,"playerHp");long quantity=peer.nativeDeInventory(0).countCatalog(potion);
   var payload=new Protocol.Writer().c(Protocol.C_USE_ITEM).d(mode==7?0x7feeeeeeL:drink.objectId());if(drink.definition().useType()==10)payload.d(12345);else payload.s("");var packet=payload.bytes();
   call(peer,"handle",new Class[]{byte[].class},packet);
   if(mode==0){
    need((int)get(peer,"playerHp")==hp&&peer.nativeDeInventory(0).countCatalog(potion)==quantity,"active status denies healing and consumption");
    var status=(BattleTimedStatus)get(peer,"timedStatus");need(status.expireDecayPotion((long)get(status,"decayPotionExpiresAtNanos"))&&!active(peer,71),"status expires at actual deadline");call(peer,"handle",new Class[]{byte[].class},packet);
   }
   long limit=System.nanoTime()+TimeUnit.SECONDS.toNanos(2);while(NativePlayerVitalCompletions.pending(NativePlayerLifetime.of(peer))!=0&&System.nanoTime()<limit)Thread.sleep(1);
   boolean valid=mode==0||mode==1||mode==6,healed=valid&&(!world||!before);
   need(((int)get(peer,"playerHp")>hp)==healed&&peer.nativeDeInventory(0).countCatalog(potion)==quantity-(healed?1:0),"native heal/quantity follows legitimate phase, status and live scene mode="+mode+" world="+world+" before="+before);
   if(valid&&world&&before)gaps++;
   collect(wire);var packets=collect(other);if(mode==0||mode==5)need(packets.contains(HexFormat.of().formatHex(Protocol.messageCode(698))),"real visible potion rejection698");
   need(wire.locked+other.locked==0&&NativePlayerVitalCompletions.pending(NativePlayerLifetime.of(peer))==0,"outputs unlocked and item lifetime drained");
  }cases++;
 }
 public static void main(String[]args)throws Exception{
  before=args[4].equals("before");var jar=Path.of(args[3]).toRealPath();need(Path.of(ClientSession.class.getProtectionDomain().getCodeSource().getLocation().toURI()).toRealPath().equals(jar),"actual native producer");
  var base=ServerConfig.load(Path.of(args[0]),"standard");ResourceDatabase.load(base);var templates=new NativeDeItemTemplatesResources.Owner().reload(base);var definitions=new NativeDeItemDefinitionsResources.Owner().reload(base);
  var root=Path.of(args[1]);Throwable primary=null;
  try{for(boolean flag:new boolean[]{false,true})for(boolean world:new boolean[]{false,true})for(boolean app:new boolean[]{false,true})for(int skill:SKILLS)for(int mode=0;mode<8;mode++){selectedSkill=skill;selectedMode=0;
   var r=root.resolve(flag+"-"+world+"-"+app+"-"+skill+"-"+mode);Files.createDirectories(r);var cfg=copy(base,Map.of("decayPotionFixturePercentileRoll",1,"battleNaturalHpRegenEnabled",false,"battleNaturalMpRegenEnabled",false,"statePersistenceEnabled",flag,"stateFile",r.resolve("must-not-write.properties")));
   var state=new GameStateStore(r.resolve("must-not-write.properties"),false,true);var accounts=new MemoryAccountRepository("fixture","fixture","",0,1,40,52);
   try(var capture=new CaptureLog(r.resolve("capture"));var hub=new GameHub(cfg,state,accounts);
    var f=new SourceLoginRegression.Fixture(cfg,accounts,state,capture,hub,0,ClientSession.Endpoint.GAME);var p=new SourceLoginRegression.Fixture(cfg,accounts,state,capture,hub,0,ClientSession.Endpoint.GAME);
    var m=new SourceLoginRegression.Fixture(cfg,accounts,state,capture,hub,0,ClientSession.Endpoint.GAME);var o=new SourceLoginRegression.Fixture(cfg,accounts,state,capture,hub,0,ClientSession.Endpoint.GAME)){
    var clock=(ScheduledExecutorService)get(hub,"battleClock");clock.shutdownNow();need(clock.awaitTermination(2,TimeUnit.SECONDS),"unrelated clock stopped");
    var fixtures=List.of(f,p,m,o);init(fixtures,cfg);FlowRegression.setup(hub,f.session,p.session,m.session,o.session);if(world)world(fixtures,hub);if(app)NativeDeItemApplication.install(hub,templates,definitions);exercise(p.session,m.session,hub,world,mode);
   }need(!Files.exists(r.resolve("must-not-write.properties")),"both flags memory-only");
  }}catch(Throwable e){primary=e;throw e;}finally{try{NativePlayerCommit.closeFixture();}catch(Throwable e){if(primary!=null)primary.addSuppressed(e);else throw e;}}
  need(NativePlayerLifetime.allIdle(),"all lifetimes idle");String result="{\"cases\":"+cases+",\"checks\":"+ownChecks+",\"world_heal_gaps\":"+gaps+",\"actual_frames\":"+frames+",\"locked_write_calls\":"+lockedWrites+",\"full_goal_achieved\":false}";
  Files.writeString(Path.of(args[2]),result+"\n");System.out.println(result);
 }
}
