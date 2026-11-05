package local.lito381.server;
import java.util.*;

/** Mutable quest NPC presentation belongs to a map instance, never to the SQL template. */
final class NativeStaticNpcScene {
 static final String FIELD="nativeStaticNpcScenes";
 record Key(int map,int instance,long id){}
 static final class Actor {
  Integer gfx,heading;
  NativeCancellationNpcStatic.Appearance appearance;volatile boolean nativeStaticCancellationRetired;volatile Object nativeStaticRespawn;
 }
 static final class State {final Map<Key,Actor> actors=new HashMap<>();volatile Object nativeStaticMotion;}
 static int instance(ClientSession pc){return (int)CombatAccess.field(pc,"classicWorldInstanceId");}
 static boolean scoped(ClassicWorldNpcCatalog.StaticNpc npc,ClientSession pc){return npc!=null&&instance(pc)>0&&npc.implementation().equals("L1Quest");}
 static State state(GameHub hub,boolean create){
  if(!Thread.holdsLock(hub))throw new IllegalStateException("static NPC scene requires hub ownership");
  var s=(State)CombatAccess.field(hub,FIELD);
  if(s==null&&create){s=new State();CombatAccess.set(hub,FIELD,s);}return s;
 }
 static Actor actor(GameHub hub,ClassicWorldNpcCatalog.StaticNpc npc,int instance,boolean create){
  var s=state(hub,create);if(s==null)return null;
  var key=new Key(npc.mapId(),instance,npc.objectId());
  return create?s.actors.computeIfAbsent(key,k->new Actor()):s.actors.get(key);
 }
 static int gfx(GameHub hub,ClassicWorldNpcCatalog.StaticNpc npc,ClientSession pc){
  if(!scoped(npc,pc))return hub.classicWorldNpcGfx(npc);
  synchronized(hub){var a=actor(hub,npc,instance(pc),false);return a==null||a.gfx==null?npc.gfxId():a.gfx;}
 }
 static int heading(GameHub hub,ClassicWorldNpcCatalog.StaticNpc npc,ClientSession pc){
  if(!scoped(npc,pc))return NativeStaticNpcMotion.heading(hub.classicWorldNpcHeading(npc),hub,npc,pc);
  int result; synchronized(hub){var a=actor(hub,npc,instance(pc),false);result=a==null||a.heading==null?npc.heading():a.heading;}return NativeStaticNpcMotion.heading(result,hub,npc,pc);
 }
 static boolean transform(GameHub hub,ClassicWorldNpcCatalog.StaticNpc npc,int expected,int next,ClientSession pc){
  if(!scoped(npc,pc)){boolean result=hub.transformClassicWorldNpcGfx(npc,expected,next);NativeStaticNpcMotion.changed(hub);return result;}
  boolean result;
  synchronized(hub){
   if(ClassicWorldNpcCatalog.npc(npc.mapId(),npc.objectId())!=npc||expected!=npc.gfxId()||next<=0||next>65535||next==expected||gfx(hub,npc,pc)!=expected)result=false;
   else{actor(hub,npc,instance(pc),true).gfx=next;result=true;}
  }
  NativeStaticNpcMotion.changed(hub);return result;
 }
 static boolean restore(GameHub hub,ClassicWorldNpcCatalog.StaticNpc npc,int expected,ClientSession pc){
  if(!scoped(npc,pc)){boolean result=hub.restoreClassicWorldNpcGfx(npc,expected);NativeStaticNpcMotion.changed(hub);return result;}
  boolean result;
  synchronized(hub){
   if(ClassicWorldNpcCatalog.npc(npc.mapId(),npc.objectId())!=npc||expected<=0||expected>65535||expected==npc.gfxId()||gfx(hub,npc,pc)!=expected)result=false;
   else{var a=actor(hub,npc,instance(pc),false);if(a!=null){a.gfx=null;a.appearance=null;trim(hub,npc,instance(pc),a);}result=true;}
  }
  NativeStaticNpcMotion.changed(hub);return result;
 }
 static boolean turn(GameHub hub,ClassicWorldNpcCatalog.StaticNpc npc,int heading,ClientSession pc){
  if(!scoped(npc,pc))return NativeStaticNpcMotion.turned(hub.changeClassicWorldNpcHeading(npc,heading),hub,npc,heading,pc);
  if(heading<0||heading>7||ClassicWorldNpcCatalog.npc(npc.mapId(),npc.objectId())==null)return NativeStaticNpcMotion.turned(false,hub,npc,heading,pc);
  synchronized(hub){actor(hub,npc,instance(pc),true).heading=heading;}return NativeStaticNpcMotion.turned(true,hub,npc,heading,pc);
 }
 static NativeCancellationNpcStatic.Appearance appearance(GameHub hub,ClassicWorldNpcCatalog.StaticNpc npc,int instance){
  if(instance<=0)return NativeCancellationNpcStatic.appearance(hub,npc);
  synchronized(hub){var a=actor(hub,npc,instance,false);return a==null||a.appearance==null?new NativeCancellationNpcStatic.Appearance(npc.nameId(),npc.passispeed()):a.appearance;}
 }
 static void appearance(GameHub hub,ClassicWorldNpcCatalog.StaticNpc npc,int instance,NativeCancellationNpcStatic.Appearance value){
  synchronized(hub){actor(hub,npc,instance,true).appearance=value;}NativeStaticNpcMotion.changed(hub);
 }
 static void trim(GameHub hub,ClassicWorldNpcCatalog.StaticNpc npc,int instance,Actor a){
  var s=state(hub,false);if(s!=null&&a.gfx==null&&a.heading==null&&a.appearance==null)s.actors.remove(new Key(npc.mapId(),instance,npc.objectId()));
  if(s!=null&&NativeStaticNpcMotion.empty(s.actors,hub))CombatAccess.set(hub,FIELD,null);
 }
 static boolean inScene(ClientSession pc,GameHub hub,int map,int instance){
  return NativeStaticNpcMotion.alive(!(boolean)CombatAccess.field(pc,NativeDeItemApplication.DISCONNECTED)
   &&String.valueOf(CombatAccess.field(pc,"phase")).equals("WORLD")
   &&(int)CombatAccess.field(pc,"classicWorldMapId")==map&&instance(pc)==instance
   &&hub.classicWorldMembers(map,instance).contains(pc),hub);
 }
 static Object location(GameHub hub,ClientSession pc){
  synchronized(hub){return ((Map<?,?>)CombatAccess.field(hub,"classicWorldMembership")).get(pc);}
 }
 /** Called after membership mutation. Public-world state persists until quest restore or hub close. */
 static void nativeStaticRespawnOriginalPrune(GameHub hub){NativeStaticNpcMotion.prune(hub);
  synchronized(hub){
   var s=state(hub,false);if(s==null)return;
   s.actors.keySet().removeIf(k->NativeStaticNpcRespawn.prunable(hub.classicWorldMembers(k.map(),k.instance()).isEmpty(),hub,k));
   if(NativeStaticNpcMotion.empty(s.actors,hub))CombatAccess.set(hub,FIELD,null);
  }
 }
 static void nativeStaticRespawnOriginalClose(GameHub hub){NativeStaticNpcMotion.stop(hub);
  synchronized(hub){CombatAccess.set(hub,FIELD,null);CombatAccess.set(hub,NativeCancellationNpcStatic.FIELD,null);}
 }
 static void prune(GameHub hub){NativeStaticNpcRespawn.prune(hub);}
 static void close(GameHub hub){NativeStaticNpcRespawn.close(hub);}
 private NativeStaticNpcScene(){}
}
