package local.lito381.server;
import java.util.*;
/** Read-only projection under the existing target action lock; never captures role/life or emits packets. */
final class NativeSourceIcons1Projection {
 record Entry(int skill,String status,String expiry){}
 static final List<Entry> ENTRIES=List.of(
  new Entry(32,"timedStatus","meditationExpiresAtNanos"),new Entry(31,"timedStatus","counterMagicExpiresAtNanos"),new Entry(71,"timedStatus","decayPotionExpiresAtNanos"),new Entry(78,"timedStatus","absoluteBarrierExpiresAtNanos"),new Entry(104,"timedStatus","venomResistExpiresAtNanos"),new Entry(47,"timedStatus","weaknessExpiresAtNanos"),new Entry(56,"timedStatus","diseaseExpiresAtNanos"),
  new Entry(171,"timedStatus","elementalFireExpiresAtNanos"),new Entry(174,"timedStatus","strikerGaleExpiresAtNanos"),new Entry(175,"timedStatus","soulOfFlameExpiresAtNanos"),new Entry(173,"timedStatus","polluteWaterExpiresAtNanos"),new Entry(206,"timedStatus","concentrationExpiresAtNanos"),new Entry(193,"timedStatus","horrorOfDeathExpiresAtNanos"),
  new Entry(111,"abilityStatus","dressEvasionExpiresAtNanos"),new Entry(55,"abilityStatus","berserkExpiresAtNanos"),new Entry(216,"abilityStatus","insightExpiresAtNanos"),new Entry(217,"abilityStatus","panicExpiresAtNanos"),new Entry(191,"abilityStatus","mortalBodyExpiresAtNanos"),
  new Entry(14,"weightStatus","decreaseWeightExpiresAtNanos"),new Entry(176,"weightStatus","additionalFireExpiresAtNanos"),
  new Entry(158,"naturesTouchStatus","expiresAtNanos"),new Entry(167,"windShackleStatus","expiresAtNanos"),new Entry(153,"eraseMagicStatus","expiresAtNanos"),new Entry(134,"counterMirrorStatus","expiresAtNanos"),new Entry(133,"elementalFallDownStatus","expiresAtNanos"),
  new Entry(148,"fireWeaponStatus","expiresAtNanos"),new Entry(155,"fireBlessStatus","expiresAtNanos"),new Entry(163,"burningWeaponStatus","expiresAtNanos"),new Entry(149,"windShotStatus","expiresAtNanos"),new Entry(156,"stormEyeStatus","expiresAtNanos"),new Entry(166,"stormShotStatus","expiresAtNanos"),
  new Entry(188,"resistFearStatus","expiresAtNanos"),new Entry(183,"guardBrakeStatus","expiresAtNanos"),new Entry(181,"dragonSkinStatus","expiresAtNanos"));
 static int seconds(long expiry,long now){if(expiry<=now)return 0;long delta=expiry-now;if(delta<0)return Integer.MAX_VALUE;long value=delta/1_000_000_000L+(delta%1_000_000_000L==0?0:1);return (int)Math.min(Integer.MAX_VALUE,value);}
 static void add(Set<Integer> active,Map<Integer,Integer> seconds,int skill,long expiry,long now){if(expiry>now){active.add(skill);seconds.put(skill,seconds(expiry,now));}}
 static NativeSourceIcons1.Snapshot project(ClientSession pc,NativeSourceIcons1Policy.Policy policy,int override,int duration,long now,long wallMillis){
  if(pc==null||policy==null||now<0||!pc.nativePlayerActionLock().isHeldByCurrentThread()||!String.valueOf(CombatAccess.field(pc,"phase")).equals("BATTLE"))return null;
  var active=new HashSet<Integer>();var remaining=new HashMap<Integer,Integer>();var states=new HashMap<String,Object>();
  for(var entry:ENTRIES){Object state=states.get(entry.status());if(state==null){state=CombatAccess.field(pc,entry.status());if(state==null)return null;states.put(entry.status(),state);}long expiry;synchronized(state){if(CombatAccess.field(pc,entry.status())!=state)return null;expiry=(long)CombatAccess.field(state,entry.expiry());}add(active,remaining,entry.skill(),expiry,now);}
  Object timed=states.get("timedStatus");synchronized(timed){if(CombatAccess.field(pc,"timedStatus")!=timed)return null;if((int)CombatAccess.field(timed,"braveMode")==BattleTimedStatus.BRAVE_ELF)add(active,remaining,1017,(long)CombatAccess.field(timed,"braveExpiresAtNanos"),now);}
  var hub=(GameHub)CombatAccess.field(pc,"hub");if(hub==null)return null;synchronized(hub){Object value=CombatAccess.field(hub,"rooms");if(!(value instanceof Map<?,?> rooms))return null;Object room=rooms.get(CombatAccess.field(pc,"currentRoomId"));if(room==null)return null;Object patience=CombatAccess.field(room,"patienceStatus");if(patience==null)return null;synchronized(patience){if(CombatAccess.field(room,"patienceStatus")!=patience)return null;Object leases=CombatAccess.field(patience,"expiresAtByObjectId");if(!(leases instanceof Map<?,?> values))return null;Object expiry=values.get(pc.playerObjectId());if(expiry instanceof Long deadline)add(active,remaining,211,deadline,now);}}
  for(var entry:states.entrySet())if(CombatAccess.field(pc,entry.getKey())!=entry.getValue())return null;
  return new NativeSourceIcons1.Snapshot(active,remaining,policy.enabled(),override,duration,wallMillis);
 }
 private NativeSourceIcons1Projection(){}
}
