package local.lito381.server;
import java.util.*;
/** Provenance only: actual installed cube and status lease, never a replacement player HP source. */
final class NativeSourceCubeOrigin {
 static final String P="local/lito381/server/";
 record Scene(GameHub hub,Object room,long id){}
 record Creator(ClientSession pc,NativePlayerCommit.Binding binding){}
 record Entity(Object cube,Object engine,Object epoch,Scene scene,Creator creator,int npcId,long objectId){}
 /** Status follows its actual actor when it moves; its role/life/scene is independent of pose. */
 record Target(long id,int kind,ClientSession pc,NativePlayerCommit.Binding binding,Object actor,Object owner,Object life,Object generation){}
 record Scan(Object epoch,Map<Long,Target> targets){}
 record Lease(Entity effect,Object actual,Target target,long expires){}
 record Event(Lease lease){}
 static Object epoch(Object engine){return CombatAccess.field(engine,"nativeSourceCubeEpoch");}
 static void reset(Object engine){CombatAccess.set(engine,"nativeSourceCubeEpoch",new Object());}
 static Object scene(Object engine){return CombatAccess.field(engine,"nativeSourceCubeScene");}
 static Object installing(Object engine){return CombatAccess.field(engine,"nativeSourceCubeInstalling");}
 static Object prepare(Object engine,GameHub hub,long roomId,ClientSession pc){
  Object previous=installing(engine);
  if(!NativeSourceDollCommit.enabled(pc)||!Thread.holdsLock(hub))return previous;
  Object room=((Map<?,?>)CombatAccess.field(hub,"rooms")).get(roomId);String field=engine instanceof BattleCubeIgnitionEffects?"cubeIgnitionEffects":"cubeHarmonyEffects";
  if(room==null||CombatAccess.field(room,field)!=engine)throw new IllegalStateException("cube installation owner mismatch");
  if(!(scene(engine)instanceof Scene current)||current.hub!=hub||current.room!=room||current.id!=roomId){if(scene(engine)!=null)reset(engine);CombatAccess.set(engine,"nativeSourceCubeScene",new Scene(hub,room,roomId));}
  CombatAccess.set(engine,"nativeSourceCubeInstalling",new Creator(pc,NativePlayerCommit.Binding.capture(pc)));return previous;
 }
 static BattleCubeIgnitionEffects.Placement place(BattleCubeIgnitionEffects engine,long caster,int x,int y,long now,GameHub hub,long room,ClientSession pc){Object previous=prepare(engine,hub,room,pc);try{return engine.place(caster,x,y,now);}finally{CombatAccess.set(engine,"nativeSourceCubeInstalling",previous);}}
 static BattleCubeHarmonyEffects.Placement place(BattleCubeHarmonyEffects engine,long caster,int x,int y,long now,GameHub hub,long room,ClientSession pc){Object previous=prepare(engine,hub,room,pc);try{return engine.place(caster,x,y,now);}finally{CombatAccess.set(engine,"nativeSourceCubeInstalling",previous);}}

    static Object entity(Object object, Object object2) {
        return NativeSourceCubePlacement.entity(object, object2);
    }
 static Target target(Scene scene,long id,String kind){
  if(kind.equals("PLAYER")){
   for(var value:(Collection<?>)CombatAccess.field(scene.room,"battleRoster"))if(value instanceof ClientSession pc&&pc.playerObjectId()==id&&String.valueOf(CombatAccess.field(pc,"phase")).equals("BATTLE")&&(long)CombatAccess.field(pc,"currentRoomId")==scene.id){var binding=NativePlayerCommit.Binding.capture(pc);return new Target(id,0,pc,binding,pc,scene.room,NativeSourceDamageDisplay.life(pc),null);}
   return null;
  }
  if(!kind.equals("MONSTER"))return null; // Native BOT is not thereby proven to be a Source L1MonsterInstance.
  Target selected=null;
  for(int k=1;k<=2;k++){var owner=CombatAccess.field(scene.room,k==1?"questEncounter":"timeAttackEncounter");var actor=CombatAccess.call(owner,"monster","(J)L"+P+(k==1?"BattleQuestEncounter":"BattleTimeAttackEncounter")+"$Monster;",id);if(actor==null)continue;if(selected!=null)return null;selected=new Target(id,k,null,null,actor,owner,NativeCombatAttacker.token((BattleTimedStatus)CombatAccess.field(actor,"damagePoison"),true),NativeProcHealingEncounter.generation(owner));}
  return selected;
 }
 static Object scan(Object engine,List<?> actors,GameHub hub){
  if(!(scene(engine)instanceof Scene scene)||scene.hub!=hub||!Thread.holdsLock(hub))return null;
  var targets=new HashMap<Long,Target>();var ambiguous=new HashSet<Long>();for(var actor:actors){long id=(long)CombatAccess.call(actor,"objectId","()J");String kind=String.valueOf(CombatAccess.call(actor,"kind","()L"+P+(engine instanceof BattleCubeIgnitionEffects?"BattleCubeIgnitionEffects":"BattleCubeHarmonyEffects")+"$ActorKind;"));var target=target(scene,id,kind);if(target!=null&&!ambiguous.contains(id)&&targets.putIfAbsent(id,target)!=null){targets.remove(id);ambiguous.add(id);}}
  return new Scan(epoch(engine),Map.copyOf(targets));
 }
 static List<BattleCubeIgnitionEffects.Event> advance(BattleCubeIgnitionEffects engine,long now,List<BattleCubeIgnitionEffects.Actor> actors,BattleCubeIgnitionEffects.Alliance alliance,GameHub hub){Object previous=CombatAccess.field(engine,"nativeSourceCubeScan");var scan=scan(engine,actors,hub);try{CombatAccess.set(engine,"nativeSourceCubeScan",scan);return engine.advance(now,actors,alliance);}finally{CombatAccess.set(engine,"nativeSourceCubeScan",previous);}}
 static List<BattleCubeHarmonyEffects.Event> advance(BattleCubeHarmonyEffects engine,long now,List<BattleCubeHarmonyEffects.Actor> actors,BattleCubeHarmonyEffects.Alliance alliance,GameHub hub){Object previous=CombatAccess.field(engine,"nativeSourceCubeScan");var scan=scan(engine,actors,hub);try{CombatAccess.set(engine,"nativeSourceCubeScan",scan);return engine.advance(now,actors,alliance);}finally{CombatAccess.set(engine,"nativeSourceCubeScan",previous);}}
 static Object lease(Object actual,Object engine){
  Object cube=CombatAccess.field(actual,"cube");var effect=(Entity)CombatAccess.field(cube,"nativeSourceCubeEntity");if(effect==null)return null;
  long id=(long)CombatAccess.field(actual,"targetObjectId");var scan=CombatAccess.field(engine,"nativeSourceCubeScan");var target=scan instanceof Scan s&&s.epoch==effect.epoch?s.targets.get(id):null;
  return new Lease(effect,actual,target,(long)CombatAccess.field(actual,"expiresAtNanos"));
 }
 static Object event(Object event,Object engine,Object exact){
  if(exact==null){Object cube=CombatAccess.call(event,"cube","()L"+P+(engine instanceof BattleCubeIgnitionEffects?"BattleCubeIgnitionEffects":"BattleCubeHarmonyEffects")+"$Cube;");long id=(long)CombatAccess.call(event,"targetObjectId","()J");String eventKind=String.valueOf(CombatAccess.call(event,"kind","()L"+P+(engine instanceof BattleCubeIgnitionEffects?"BattleCubeIgnitionEffects":"BattleCubeHarmonyEffects")+"$EventKind;"));
   for(var value:((Map<?,?>)CombatAccess.field(engine,"statuses")).values())if(CombatAccess.field(value,"cube")==cube&&(long)CombatAccess.field(value,"targetObjectId")==id){if(engine instanceof BattleCubeIgnitionEffects){String kind=String.valueOf(CombatAccess.field(value,"kind"));if(!eventKind.startsWith(kind+"_"))continue;}if(exact!=null)return null;exact=value;}
  }
  if(exact==null)return null;var lease=CombatAccess.field(exact,"nativeSourceCubeOrigin");return lease instanceof Lease original?new Event(original):null;
 }
 private NativeSourceCubeOrigin(){}

 static Object nativeSourceCubePlacementOriginalEntity(Object cube,Object engine){
  if(!(engine instanceof BattleCubeIgnitionEffects)&&!(engine instanceof BattleCubeHarmonyEffects))throw new IllegalArgumentException("unbound cube kind");
  return new Entity(cube,engine,epoch(engine),scene(engine)instanceof Scene s?s:null,installing(engine)instanceof Creator c?c:null,engine instanceof BattleCubeIgnitionEffects?80149:80152,(long)CombatAccess.call(cube,"objectId","()J"));
 }
}
