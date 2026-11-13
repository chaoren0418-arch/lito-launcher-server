package local.lito381.server;
import java.io.*;import java.util.*;import java.util.concurrent.ThreadLocalRandom;
/** Source firewall PC arithmetic inside the original paired raw action. */
final class NativeSourceGroundPlayer {
 static final String P="local/lito381/server/";
 static int modifier(int skill){
  if(skill>=3008&&skill<=3015||skill>=3024&&skill<=3031||skill>=3040&&skill<=3047)return -5;
  if(skill>=3048&&skill<=3051)return -2;
  return skill==181?-10:skill==211?-15:0;
 }
 static int resolve(int source,int fire,Iterable<Integer> active){
  double reduction=fire>0?(int)(0.32*Math.abs(fire))/32.0:0;
  int damage=Math.max(0,(int)((1.0-reduction)*source));boolean half=false;var seen=new HashSet<Integer>();
  if(active!=null)for(var skill:active)if(skill!=null&&seen.add(skill)){if(skill==68)half=true;else damage+=modifier(skill);}
  if(half)damage/=2;return Math.max(0,damage);
 }
 static boolean immune(BattleTimedStatus status,long now){return status.freezeActive(now)||status.absoluteBarrierActive(now)||status.earthBindActive(now);}
 static Set<Integer> skills(BattleCookingStatus.Snapshot cooking,BattleDragonSkinStatus.Snapshot dragon,boolean patience,BattleAbilityStatus.Protection immunity){
  var ids=new HashSet<Integer>();for(var recipe:cooking.recipes())ids.add(recipe.statusId());if(dragon.active())ids.add(181);if(patience)ids.add(211);if(immunity.remainingSeconds()>0)ids.add(68);return Set.copyOf(ids);
 }
 static Set<Integer> skills(ClientSession target,NativeSourceGroundOrigin.Scene scene,long now){
  var cooking=((BattleCookingStatus)CombatAccess.field(target,"cookingStatus")).active(now);var dragon=((BattleDragonSkinStatus)CombatAccess.field(target,"dragonSkinStatus")).snapshot(now);
  var patience=((BattlePatienceStatus)CombatAccess.field(scene.room(),"patienceStatus")).active(target.playerObjectId(),now);var immunity=((BattleAbilityStatus)CombatAccess.field(target,"abilityStatus")).immuneToHarm(now);return skills(cooking,dragon,patience,immunity);
 }
 static boolean permitted(NativeSourceGroundOrigin.PulseProof p){
  if(!NativeSourceGroundOrigin.current(p)||!NativeSourceGroundQueued.sameEntity(p.entity())||!NativeSourceGroundOrigin.installed(p))return false;
  var s=p.entity().scene();if(!Thread.holdsLock(s.hub()))return false;var match=(BattleRules.MatchState)CombatAccess.field(s.room(),"match");
  return match.phase()==BattleRules.Phase.RUNNING&&match.canAttack(p.source().binding().object(),p.target().binding().object());
 }
 /** A nonmatching scope keeps the exact original method; a stale matching Source pulse is handled and dropped. */
 static boolean pulse(ClientSession target,long room,ClientSession caster,int skill)throws IOException{
  var action=NativeSourceRawDamage.current();var c=NativeAreaMaterialAction.current.get();
  if(!CombatRules.loaded()||skill!=58||c==null||!(c.nativeSourceGroundProof instanceof NativeSourceGroundOrigin.PulseProof p))return false;
  if(action==null||action.c!=c||c.pc!=target||p.entity()==null||p.entity().scene()==null||p.source()==null||p.target()==null||p.target().pc()!=target||p.source().pc()!=caster||p.entity().scene().id()!=room||!target.nativePlayerActionLock().isHeldByCurrentThread()||!caster.nativePlayerActionLock().isHeldByCurrentThread())return true;
  var scene=p.entity().scene();var life=NativePlayerLifetime.of(scene.hub());
  try{
   synchronized(scene.hub()){if(!permitted(p)||!action.frame()||NativeSourceDamageDisplay.n(target,"playerHp")<=0||immune((BattleTimedStatus)CombatAccess.field(target,"timedStatus"),System.nanoTime()))return true;}
   // Existing effective stat readers, under the original actor pair and outside the Hub monitor.
   int intelligence=caster.nativeSkillConsumptionIntelligence();long now=System.nanoTime();int fire=(int)CombatAccess.call(target,"effectiveFireResistance","(J)I",now);Set<Integer> skills;
   synchronized(scene.hub()){if(!permitted(p)||!action.frame()||NativeSourceDamageDisplay.n(target,"playerHp")<=0||immune((BattleTimedStatus)CombatAccess.field(target,"timedStatus"),now))return true;skills=skills(target,scene,now);}
   int damage=resolve(19+ThreadLocalRandom.current().nextInt(NativeSourceGroundNpc.bound(intelligence)),fire,skills);if(damage<=0)return true;
   var packet=Protocol.objectAction(target.playerObjectId(),2);
   // Original frozen action audience/receiver bindings and deferred output lifecycle, before raw HP.
   for(var receiver:c.players)if(inRange(receiver,target))CombatAccess.call(receiver,"sendEncrypted","([BLjava/lang/String;)V",packet,"S_ACTION Source firewall PC");
   if(NativeSourceGroundOrigin.current(p))NativeSourceRawDamage.player(target,damage,caster,false,true);return true;
  }catch(RuntimeException failure){NativeSourcePhysicalNpc.failed(life);return true;}
 }
 static boolean inRange(ClientSession receiver,ClientSession target){return BattleSpellCatalog.isInNativeScreen(NativeSourceDamageDisplay.n(receiver,"positionX"),NativeSourceDamageDisplay.n(receiver,"positionY"),NativeSourceDamageDisplay.n(target,"positionX"),NativeSourceDamageDisplay.n(target,"positionY"));}
 static boolean reaction(byte[] packet,long target){return packet!=null&&packet.length==6&&(packet[0]&255)==Protocol.S_ACTION&&NativeSourcePhysicalNpc.object(packet)==target&&(packet[5]&255)==2;}
 /** Final Frame range predicate reads existing poses only; it adds no actor/Hub/status lock over IO. */
 static boolean allowFrame(ClientSession receiver,byte[] packet){var c=NativeAreaMaterialAction.current.get();return c==null||!(c.nativeSourceGroundProof instanceof NativeSourceGroundOrigin.PulseProof p)||p.target()==null||!reaction(packet,p.target().binding().object())||inRange(receiver,p.target().pc());}
 private NativeSourceGroundPlayer(){}
}
