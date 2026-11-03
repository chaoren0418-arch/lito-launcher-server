package local.lito381.server;
import java.io.IOException;import java.lang.reflect.*;import java.util.*;
/** Explicit native targets; skill handlers retain cost, range, LOS and effect validation. */
final class IllusionSkillRequests {
 static final Set<Integer> ATTACK=Set.of(202,203,207,208,213);
 static final Set<Integer> OTHER=Set.of(204,209,211,212,214,216,217,218,219);
 private static final ThreadLocal<Protocol.AttackSpellRequest> REQUEST=new ThreadLocal<>();
 static Protocol.AttackSpellRequest current(){return REQUEST.get();}
 static boolean dispatch(ClientSession session,byte[] payload)throws IOException {
  int skill=Protocol.spellSelector(payload).skillId();
  if(!ATTACK.contains(skill)&&!OTHER.contains(skill))return false;
  if(payload.length!=11){System.out.println("[LiTo] Illusion spell rejected missing explicit target skill="+skill);return true;}
  var request=Protocol.attackSpellRequest(payload);
  if(request.targetObjectId()<=0)return true;
  REQUEST.set(request);
  try {
   System.out.println("[LiTo] Illusion explicit target skill="+skill+" target="+request.targetObjectId());
   var method=ClientSession.class.getDeclaredMethod("useSpellBeforeDragonTargets",byte[].class);method.setAccessible(true);
   method.invoke(session,(Object)Arrays.copyOf(payload,3));
  }catch(InvocationTargetException e){
   if(e.getCause() instanceof IOException x)throw x;
   if(e.getCause() instanceof RuntimeException x)throw x;
   throw new IllegalStateException(e.getCause());
  }catch(ReflectiveOperationException e){throw new IllegalStateException(e);}
  finally{REQUEST.remove();}
  return true;
 }
 static ClientSession selectPc(ClientSession caster,List<ClientSession> eligible) {
  long id=current().targetObjectId();
  if(id==caster.playerObjectId())return caster;
  for(var candidate:eligible)if(candidate.playerObjectId()==id)return candidate;
  return null;
 }
 static Object selectActor(ClientSession caster,Map<Long,?> eligible) {
  long id=current().targetObjectId();
  if(id!=caster.playerObjectId())return eligible.get(id);
  try {
   var type=Class.forName(ClientSession.class.getName()+"$PatienceTarget");
   var ctor=type.getDeclaredConstructor(long.class,int.class,int.class,BattleTrueTarget.Kind.class,ClientSession.class);ctor.setAccessible(true);
   return ctor.newInstance(id,coordinate(caster,"positionX"),coordinate(caster,"positionY"),BattleTrueTarget.Kind.PLAYER,caster);
  }catch(ReflectiveOperationException e){throw new IllegalStateException(e);}
 }
 private static int coordinate(ClientSession s,String name)throws ReflectiveOperationException{var f=ClientSession.class.getDeclaredField(name);f.setAccessible(true);return f.getInt(s);}
}
