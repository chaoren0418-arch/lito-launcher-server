package local.lito381.server;
import java.io.IOException;
import java.util.List;
/** Preserve existing Source scope and four bodies and their target checks under ordered player locks. */
final class NativePoisonMaterialAction {
 private static ClientSession target(ClientSession pc,byte[] packet,boolean world){
  var request=Protocol.attackSpellRequest(packet);var hub=(GameHub)CombatAccess.field(pc,"hub");
  List<ClientSession> players=world?(List<ClientSession>)CombatAccess.call(pc,"classicWorldMembers","(I)Ljava/util/List;",(int)CombatAccess.field(pc,"classicWorldMapId")):hub.roomCombatants((long)CombatAccess.field(pc,"currentRoomId"));
  return players.stream().filter(p->p.playerObjectId()==request.targetObjectId()).findFirst().orElse(pc);
 }
 static void poisonBattle(ClientSession pc,byte[] packet,BattleSpellCatalog.PoisonSpell spell,CombatPoisonLifecycle.Scope scope)throws IOException{
  NativeLightAction.commitPair(pc,target(pc,packet,false),()->{var owned=open();try{pc.nativeMaterialPoisonScopeBody(packet,spell,owned);}catch(IOException|RuntimeException|Error failure){CombatPoisonLifecycle.failed(owned,failure);throw failure;}});
 }
 static void poisonWorld(ClientSession pc,byte[] packet,BattleSpellCatalog.PoisonSpell spell,CombatPoisonLifecycle.Scope scope)throws IOException{
  NativeLightAction.commitPair(pc,target(pc,packet,true),()->{var owned=open();try{pc.nativeMaterialWorldPoisonScopeBody(packet,spell,owned);}catch(IOException|RuntimeException|Error failure){CombatPoisonLifecycle.failed(owned,failure);throw failure;}});
 }
 static void paralysisBattle(ClientSession pc,byte[] packet,BattleSpellCatalog.ParalysisSpell spell,CombatPoisonLifecycle.Scope scope)throws IOException{
  NativeLightAction.commitPair(pc,target(pc,packet,false),()->{var owned=open();try{pc.nativeMaterialParalysisScopeBody(packet,spell,owned);}catch(IOException|RuntimeException|Error failure){CombatPoisonLifecycle.failed(owned,failure);throw failure;}});
 }
 static void paralysisWorld(ClientSession pc,byte[] packet,BattleSpellCatalog.ParalysisSpell spell,CombatPoisonLifecycle.Scope scope)throws IOException{
  NativeLightAction.commitPair(pc,target(pc,packet,true),()->{var owned=open();try{pc.nativeMaterialWorldParalysisScopeBody(packet,spell,owned);}catch(IOException|RuntimeException|Error failure){CombatPoisonLifecycle.failed(owned,failure);throw failure;}});
 }
 private static CombatPoisonLifecycle.Scope open(){var owned=CombatPoisonLifecycle.open();NativeLightAction.finishScoped((published,failure)->{if(published&&failure==null)CombatPoisonLifecycle.finish(owned);else CombatPoisonLifecycle.failed(owned,failure!=null?failure:new java.util.concurrent.CancellationException("cast publication retired"));});return owned;}
 private static boolean open(CombatPoisonLifecycle.Scope scope){return scope==null||!(boolean)CombatAccess.field(scope,"closed");}
 static void palette(ClientSession pc,int type,String reason,CombatPoisonLifecycle.Scope scope){NativeLightAction.deferScoped(()->{if(open(scope))CombatPoisonLifecycle.palette(pc,type,reason,scope);});}
 static void monsterSend(ClientSession recipient,byte[] packet,String reason,CombatPoisonLifecycle.Scope scope){NativeLightAction.deferScoped(()->{if(open(scope))CombatMonsterCurseLifecycle.send(recipient,packet,reason,scope);});}
 static void monsterPaletteFinished(byte[] packet,CombatPoisonLifecycle.Scope scope){NativeLightAction.deferScoped(()->{if(open(scope))CombatMonsterCurseLifecycle.paletteFinished(packet,scope);});}
 static void poisonPaletteFinished(ClientSession target,CombatPoisonLifecycle.Scope scope,boolean applied,int interval){
  if(target==null||scope==null||!applied)return;
  NativeLightAction.deferScoped(()->{if(!open(scope))return;Object context=scope.find(target);if(context==null)return;
   var timed=CombatPoisonAtomic.status(target);synchronized(timed){var producer=(CombatPoisonLifecycle.Lease)CombatAccess.field(context,"producer");var state=(CombatPoisonLifecycle.State)CombatAccess.field(context,"state");long now=System.nanoTime();
    if(CombatAccess.field(context,"timed")==timed&&(boolean)CombatAccess.field(context,"producerActive")&&state.isCurrent(producer)&&timed.poisonActive(now)){CombatAccess.set(timed,"poisonNextTickAtNanos",Math.addExact(now,java.util.concurrent.TimeUnit.MILLISECONDS.toNanos(interval)));producer.started=true;}
   }
  });
 }
 static boolean admit(ClientSession pc,ClientSession target,int skill,int action,int reuse){
  if(skill!=11&&skill!=33)throw new IllegalStateException("unreviewed poison/paralysis material skill");
  return NativeLightAction.admitPair(pc,target==null?pc:target,skill,action,reuse);
 }
 private NativePoisonMaterialAction(){}
}
