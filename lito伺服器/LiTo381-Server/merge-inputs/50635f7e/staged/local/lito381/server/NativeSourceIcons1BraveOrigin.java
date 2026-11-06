package local.lito381.server;
import java.util.*;
/** Source1017 is type4 fruit, not ordinary type3 elf brave. Scalar provenance only; no actor/output authority. */
final class NativeSourceIcons1BraveOrigin {

 static void activateFruit(BattleTimedStatus timed,long now,int seconds){synchronized(timed){timed.activateBrave(now,seconds,BattleTimedStatus.BRAVE_HOLY_WALK);BattleTimedStatus owner;(owner=timed).nativeSourceIcons1BraveSkill=1017;NativeSourceIcons1Epoch.bump(owner);}}
 static NativeSourceIcons1.Snapshot finish(NativeSourceIcons1.Snapshot original,ClientSession pc,long now,Object first){
  if(original==null||pc==null||!(first instanceof BattleTimedStatus timed)||!pc.nativePlayerActionLock().isHeldByCurrentThread())return null;
  boolean fruit;long expiry;synchronized(timed){if(CombatAccess.field(pc,"timedStatus")!=timed)return null;expiry=(long)CombatAccess.field(timed,"braveExpiresAtNanos");fruit=timed.nativeSourceIcons1BraveSkill==1017&&(int)CombatAccess.field(timed,"braveMode")==BattleTimedStatus.BRAVE_HOLY_WALK&&expiry>now;}
  if(!fruit&&!original.active().contains(1017))return original;
  var active=new HashSet<>(original.active());var seconds=new HashMap<>(original.seconds());active.remove(1017);seconds.remove(1017);if(fruit){active.add(1017);seconds.put(1017,NativeSourceIcons1Projection.seconds(expiry,now));}
  return new NativeSourceIcons1.Snapshot(active,seconds,original.coexist(),original.overrideSkill(),original.overrideSeconds(),original.wallMillis());
 }
 private NativeSourceIcons1BraveOrigin(){}
}
