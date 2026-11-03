package local.lito381.server;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
/** Follow the actual monlist response through C_HACTION, not just its first packet. */
public class PolymorphMenuProbe extends PolymorphMaterialProbe {
 static void answer(ClientSession pc,long object,String action,String parameter)throws Exception{
  call(pc,"handle",new Class[]{byte[].class},new Protocol.Writer().c(Protocol.C_HACTION).d(object).s(action).s(parameter).bytes());
 }
 static void exercise(ClientSession pc,ClientSession peer,GameHub hub,boolean world,int mode)throws Exception{
  setup(pc,peer,hub,world);ring(peer,true);var row=BattleSpellCatalog.polymorphSpell(67);
  int item=Math.toIntExact(row.itemConsumeId());NativeDeItemPublication.grant(pc,item,row.itemConsumeCount()+3);
  var chars=(List<CharacterSummary>)get(peer,"characters");set(peer,"characters",List.of(copy(chars.get(0),Map.of("level",99))));
  try(var wire=new SourceActionPairProbe.Wire(pc,peer);var other=new SourceActionPairProbe.Wire(peer,pc)){
   cast(pc,67,row);need(get(peer,"pendingShapeChange")!=null,"actual cast creates menu token");Object pending=get(peer,"pendingShapeChange");
   var menuForm=BattlePolymorphCatalog.require("death 52");
   need(menuForm.standardMenu()&&menuForm.minimumLevel()==52&&menuForm.gfx()==6137,"retained monlist selection contract");
   need(!BattlePolymorphCatalog.DEATH_KNIGHT.standardMenu(),"legacy death knight constant is not a monlist choice");
   String selection=menuForm.selection();
   if(mode==1)selection="invalid-test-form";
   if(mode==2)set(peer,"characters",List.of(copy(chars.get(0),Map.of("level",1))));
   if(mode==3){((BattlePolymorphStatus)get(peer,"polymorphStatus")).activate(System.nanoTime(),BattlePolymorphCatalog.DEATH_KNIGHT,60);selection="none";}
   if(mode==4){if(world)NativePlayerCommit.scene(peer,(int)get(peer,"classicWorldInstanceId")+1,"classicWorldInstanceId");else NativePlayerCommit.scene(peer,(long)get(peer,"currentRoomId")+100,"currentRoomId");}
   if(mode==7)set(peer,"playerHp",0);if(mode==8)set(peer,"observer",true);
   if(mode==9){
    // A distinct newer capability must survive a delayed callback with the old object.
    Object newer=copy((Record)pending,Map.of());set(peer,"pendingShapeChange",newer);
    var request=Protocol.hypertextActionRequest(new Protocol.Writer().c(Protocol.C_HACTION).d(peer.playerObjectId()).s(selection).s("").bytes());
    call(peer,"handleShapeChangeAction",new Class[]{Protocol.HypertextActionRequest.class,pending.getClass()},request,pending);
    need(get(peer,"pendingShapeChange")==newer&&!(((BattlePolymorphStatus)get(peer,"polymorphStatus")).active(System.nanoTime())!=null),"stale callback cannot consume newer menu");
   }else{
    answer(peer,peer.playerObjectId(),selection,mode==6?"unexpected-parameter":"");
    need(get(peer,"pendingShapeChange")==null,"answer consumes or rejects one-shot token");
    var form=((BattlePolymorphStatus)get(peer,"polymorphStatus")).active(System.nanoTime());
    need((form!=null)==(mode==0||mode==5),"valid live-context response alone applies form mode="+mode);
    if(form!=null)need(form.equals(menuForm),"requested native form exact");
    if(mode==5){answer(peer,peer.playerObjectId(),"none","");need(((BattlePolymorphStatus)get(peer,"polymorphStatus")).active(System.nanoTime()).equals(form),"replayed answer cannot clear accepted form without new token");}
   }
   need(pc.nativeDeInventory(0).countCatalog(item)==3&&(int)get(pc,"playerMp")==1000-cost(pc,67,row),"response never charges caster twice");
   var self=collect(wire);var peers=collect(other);if(mode==2)need(peers.contains(HexFormat.of().formatHex(Protocol.messageCode(181))),"insufficient level visible181");
   need(wire.locked+other.locked==0&&NativePlayerVitalCompletions.pending(NativePlayerLifetime.of(pc))==0&&NativePlayerVitalCompletions.pending(NativePlayerLifetime.of(peer))==0,"menu output unlocked and receipts released");
  }cases++;
 }
 public static void main(String[]args)throws Exception{run(args);}
 static void run(String[]args)throws Exception{
  var jar=Path.of(args[3]).toRealPath();need(Path.of(ClientSession.class.getProtectionDomain().getCodeSource().getLocation().toURI()).toRealPath().equals(jar),"actual native menu consumer");
  var base=ServerConfig.load(Path.of(args[0]),"standard");ResourceDatabase.load(base);var templates=new NativeDeItemTemplatesResources.Owner().reload(base);var definitions=new NativeDeItemDefinitionsResources.Owner().reload(base);var root=Path.of(args[1]);Throwable primary=null;
  try{for(boolean flag:new boolean[]{false,true})for(boolean world:new boolean[]{false,true})for(boolean app:new boolean[]{false,true})for(int mode=0;mode<10;mode++){
   var r=root.resolve(flag+"-"+world+"-"+app+"-"+mode);Files.createDirectories(r);var cfg=copy(base,Map.of("battleNaturalHpRegenEnabled",false,"battleNaturalMpRegenEnabled",false,"statePersistenceEnabled",flag,"stateFile",r.resolve("must-not-write.properties")));
   var state=new GameStateStore(r.resolve("must-not-write.properties"),false,true);var accounts=new MemoryAccountRepository("fixture","fixture","",0,1,40,52);
   try(var capture=new CaptureLog(r.resolve("capture"));var hub=new GameHub(cfg,state,accounts);var f=new SourceLoginRegression.Fixture(cfg,accounts,state,capture,hub,0,ClientSession.Endpoint.GAME);var p=new SourceLoginRegression.Fixture(cfg,accounts,state,capture,hub,0,ClientSession.Endpoint.GAME);var m=new SourceLoginRegression.Fixture(cfg,accounts,state,capture,hub,0,ClientSession.Endpoint.GAME);var o=new SourceLoginRegression.Fixture(cfg,accounts,state,capture,hub,0,ClientSession.Endpoint.GAME)){
    var clock=(ScheduledExecutorService)get(hub,"battleClock");clock.shutdownNow();need(clock.awaitTermination(2,TimeUnit.SECONDS),"unrelated clock stopped");var fixtures=List.of(f,p,m,o);init(fixtures,cfg);FlowRegression.setup(hub,f.session,p.session,m.session,o.session);if(world)world(fixtures,hub);if(app)NativeDeItemApplication.install(hub,templates,definitions);exercise(p.session,m.session,hub,world,mode);
   }need(!Files.exists(r.resolve("must-not-write.properties")),"memory only");
  }}catch(Throwable e){primary=e;throw e;}finally{try{NativePlayerCommit.closeFixture();}catch(Throwable e){if(primary!=null)primary.addSuppressed(e);else throw e;}}
  need(NativePlayerLifetime.allIdle(),"all lifetimes idle");Files.writeString(Path.of(args[2]),"{\"cases\":"+cases+",\"checks\":"+ownChecks+",\"frames\":"+frames+",\"locked_writes\":"+lockedWrites+",\"full_goal_achieved\":false}\n");
 }
}
