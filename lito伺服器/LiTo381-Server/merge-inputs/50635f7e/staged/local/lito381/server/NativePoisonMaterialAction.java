// ORIGINAL SOURCE moved from lito伺服器/LiTo381-Server/tools/combat-continuation/poison-material/NativePoisonMaterialAction.java (T002); replaces CFR decompile of reference/base-0.6.71.jar
package local.lito381.server;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.TimeUnit;
/** Preserve existing Source scope and four bodies and their target checks under ordered player locks. */
final class NativePoisonMaterialAction {
 private static ClientSession target(ClientSession pc,byte[] packet,boolean world){
  var request=Protocol.attackSpellRequest(packet);var hub=(GameHub)CombatAccess.field(pc,"hub");
  List<ClientSession> players=world?(List<ClientSession>)CombatAccess.call(pc,"classicWorldMembers","(I)Ljava/util/List;",(int)CombatAccess.field(pc,"classicWorldMapId")):hub.roomCombatants((long)CombatAccess.field(pc,"currentRoomId"));
  return players.stream().filter(p->p.playerObjectId()==request.targetObjectId()).findFirst().orElse(pc);
 }
 static void nativeSourceArenaOriginalBattle(ClientSession pc,byte[] packet,BattleSpellCatalog.PoisonSpell spell,CombatPoisonLifecycle.Scope scope)throws IOException{
  NativeLightAction.commitPair(pc,target(pc,packet,false),()->{
   var opened=open();
   try{pc.nativeMaterialPoisonScopeBody(packet,spell,opened);}
   catch(IOException|Error|RuntimeException failure){CombatPoisonLifecycle.failed(opened,failure);throw failure;}
  });
 }
 static void poisonWorld(ClientSession pc,byte[] packet,BattleSpellCatalog.PoisonSpell spell,CombatPoisonLifecycle.Scope scope)throws IOException{
  NativeLightAction.commitPair(pc,target(pc,packet,true),()->{
   var opened=open();
   try{pc.nativeMaterialWorldPoisonScopeBody(packet,spell,opened);}
   catch(IOException|Error|RuntimeException failure){CombatPoisonLifecycle.failed(opened,failure);throw failure;}
  });
 }
 static void paralysisBattle(ClientSession pc,byte[] packet,BattleSpellCatalog.ParalysisSpell spell,CombatPoisonLifecycle.Scope scope)throws IOException{
  NativeLightAction.commitPair(pc,target(pc,packet,false),()->{
   var opened=open();
   try{pc.nativeMaterialParalysisScopeBody(packet,spell,opened);}
   catch(IOException|Error|RuntimeException failure){CombatPoisonLifecycle.failed(opened,failure);throw failure;}
  });
 }
 static void paralysisWorld(ClientSession pc,byte[] packet,BattleSpellCatalog.ParalysisSpell spell,CombatPoisonLifecycle.Scope scope)throws IOException{
  NativeLightAction.commitPair(pc,target(pc,packet,true),()->{
   var opened=open();
   try{pc.nativeMaterialWorldParalysisScopeBody(packet,spell,opened);}
   catch(IOException|Error|RuntimeException failure){CombatPoisonLifecycle.failed(opened,failure);throw failure;}
  });
 }
 private static CombatPoisonLifecycle.Scope open(){
  var scope=CombatPoisonLifecycle.open();
  NativeLightAction.finishScoped((published,failure)->{
   if(published&&failure==null)CombatPoisonLifecycle.finish(scope);
   else CombatPoisonLifecycle.failed(scope,failure!=null?failure:new CancellationException("cast publication retired"));
  });
  return scope;
 }
 private static boolean open(CombatPoisonLifecycle.Scope scope){
  return scope==null||(Boolean)CombatAccess.field(scope,"closed")==false;
 }
 static void palette(ClientSession pc,int type,String reason,CombatPoisonLifecycle.Scope scope){
  NativeLightAction.deferScoped(()->{if(open(scope))CombatPoisonLifecycle.palette(pc,type,reason,scope);});
 }
 static void monsterSend(ClientSession pc,byte[] packet,String reason,CombatPoisonLifecycle.Scope scope){
  NativeLightAction.deferScoped(()->{if(open(scope))CombatMonsterCurseLifecycle.send(pc,packet,reason,scope);});
 }
 static void monsterPaletteFinished(byte[] packet,CombatPoisonLifecycle.Scope scope){
  NativeLightAction.deferScoped(()->{if(open(scope))CombatMonsterCurseLifecycle.paletteFinished(packet,scope);});
 }
 static void poisonPaletteFinished(ClientSession pc,CombatPoisonLifecycle.Scope scope,boolean applied,int interval){
  if(pc==null||scope==null||!applied)return;
  NativeLightAction.deferScoped(()->{
   if(!open(scope))return;
   Object context=scope.find(pc);if(context==null)return;
   var timed=CombatPoisonAtomic.status(pc);
   synchronized(timed){
    var lease=(CombatPoisonLifecycle.Lease)CombatAccess.field(context,"producer");
    var state=(CombatPoisonLifecycle.State)CombatAccess.field(context,"state");
    long now=System.nanoTime();
    if(CombatAccess.field(context,"timed")==timed&&(Boolean)CombatAccess.field(context,"producerActive")&&state.isCurrent(lease)&&timed.poisonActive(now)){
     CombatAccess.set(timed,"poisonNextTickAtNanos",Math.addExact(now,TimeUnit.MILLISECONDS.toNanos(interval)));
     lease.started=true;
    }
   }
  });
 }
 static boolean admit(ClientSession pc,ClientSession target,int skill,int action,int reuse){
  if(skill!=11&&skill!=33)throw new IllegalStateException("unreviewed poison/paralysis material skill");
  return NativeLightAction.admitPair(pc,target==null?pc:target,skill,action,reuse);
 }
 static void poisonBattle(ClientSession pc,byte[] packet,BattleSpellCatalog.PoisonSpell spell,CombatPoisonLifecycle.Scope scope)throws IOException{
  NativeSourceArenaPoison.battle(pc,packet,spell,scope);
 }
 private NativePoisonMaterialAction(){}
}
