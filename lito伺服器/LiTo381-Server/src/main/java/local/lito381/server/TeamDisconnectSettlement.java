package local.lito381.server;
import java.util.*;
final class TeamDisconnectSettlement {
 static BattleRules.Resolution resolve(Set<Long> roster,Set<Long> disconnected,GameHub hub,long room){
  var players=new LinkedHashSet<>(roster);var departed=new LinkedHashSet<>(disconnected);departed.retainAll(players);
  var survivors=new LinkedHashSet<>(players);survivors.removeAll(departed);BattleRules.Resolution result;
  if(players.size()>=2 && survivors.isEmpty())result=new BattleRules.Resolution(true,false,Set.of(),Set.of(),"all-combatants-disconnected-invalid");
  else {
   var rules=hub.roomRules(room);Set<Integer> forces=new HashSet<>();
   for(var s:hub.battleResultCombatants(room))if(survivors.contains(s.playerObjectId()))forces.add(hub.forceOf(room,s));
   boolean continues=!survivors.isEmpty() && (!rules.mode().playerVersusPlayer() || (rules.teamBased()?forces.size()>1:survivors.size()>1));
   result=new BattleRules.Resolution(!continues,false,continues?Set.of():survivors,departed,continues?"member-disconnected-battle-continues":"combatant-disconnected");
  }
  return DurableBattleSettlement.freezeDisconnect(hub,room,hub.battleResultCombatants(room),result,departed);
 }
 static Object continuingWork(GameHub hub,long roomId,List<ClientSession> roster,BattleRules.Resolution result,Set<Long> departed){
  if(result.ended())return null;
  Object room=((Map<?,?>)DurableBattleSettlement.get(hub,"rooms")).get(roomId);
  Object match=DurableBattleSettlement.get(room,"match");((Set<Long>)DurableBattleSettlement.get(match,"alive")).removeAll(departed);
  ((Collection<ClientSession>)DurableBattleSettlement.get(room,"battleRoster")).removeIf(s->departed.contains(s.playerObjectId()));
  ((Set<?>)DurableBattleSettlement.get(room,"disconnectedBattleCombatants")).clear();DurableBattleSettlement.set(room,"disconnectResolutionPending",false);
  try{Class<?> type=Class.forName("local.lito381.server.GameHub$BattleDisconnectWork");var ctor=type.getDeclaredConstructor(long.class,List.class,List.class,BattleRules.Resolution.class,List.class,boolean.class);ctor.setAccessible(true);return ctor.newInstance(roomId,List.of(),roster,result,List.of(),false);}catch(ReflectiveOperationException e){throw new IllegalStateException(e);}
 }
 static Object freezeWork(Object work,GameHub hub){
  if(work==null)return null;var roster=(List<ClientSession>)DurableBattleSettlement.get(work,"roster");var result=(BattleRules.Resolution)DurableBattleSettlement.get(work,"resolution");
  DurableBattleSettlement.freezeDisconnect(hub,(long)DurableBattleSettlement.get(work,"roomId"),roster,result,result.losers());return work;
 }
 static void retryDisconnect(GameHub hub,long room,long generation){
  if(DurableBattleSettlement.cleanupAllowed(hub,room))return;
  var clock=(java.util.concurrent.ScheduledExecutorService)DurableBattleSettlement.get(hub,"battleClock");
  clock.schedule(()->DurableBattleSettlement.call(hub,"resolveBattleDisconnectSafely",new Class[]{long.class,long.class},room,generation),2,java.util.concurrent.TimeUnit.SECONDS);
 }
}
