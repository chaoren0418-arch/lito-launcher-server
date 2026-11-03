package local.lito381.server;
import java.io.*;import java.util.*;
/** Quake/Shock use the original material, status and secondary-lease stores. */
final class NativeSourceCubeStatus {
 static final String P="local/lito381/server/";
 record Link(NativeSourceCubeOrigin.Entity effect,NativeSourceCubeOrigin.Target target,Object actual,String table){}
 record Emission(Object origin,boolean expired,Map<ClientSession,NativePlayerCommit.Binding> receivers){}
 static String type(Object e){return e instanceof BattleCubeQuakeEffects?"BattleCubeQuakeEffects":e instanceof BattleCubeShockEffects?"BattleCubeShockEffects":null;}
 static String field(Object e){return e instanceof BattleCubeQuakeEffects?"cubeQuakeEffects":e instanceof BattleCubeShockEffects?"cubeShockEffects":null;}
 static int npc(Object e){return e instanceof BattleCubeQuakeEffects?80150:80151;}
 static NativeSourceCubePlacement.Draft prepare(Object engine,long caster,ClientSession pc,GameHub hub,long id){
  var batch=NativeLightAction.nativeSourceGroundPlacementBatch();if(batch==null||CombatAccess.field(batch,"pc")!=pc)return null;var accepted=CombatAccess.field(batch,"accepted");if(accepted==null||CombatAccess.field(accepted,"pc")!=pc||CombatAccess.field(accepted,"hub")!=hub)return null;
  var binding=(NativePlayerCommit.Binding)CombatAccess.field(accepted,"binding");if(binding==null||binding.object()!=caster||!"BATTLE".equals(binding.phase())||binding.room()!=id||!NativeAcceptedFreezeMutation.roleFrame(binding,pc)||!pc.nativePlayerActionLock().isHeldByCurrentThread()||(boolean)CombatAccess.field(pc,"sourceItemDisconnected"))return null;
  var room=((Map<?,?>)CombatAccess.field(hub,"rooms")).get(id);if(room==null||field(engine)==null||CombatAccess.field(room,field(engine))!=engine)return null;var old=NativeSourceCubeOrigin.scene(engine);NativeSourceCubeOrigin.Scene s;
  if(old==null)s=new NativeSourceCubeOrigin.Scene(hub,room,id);else if(old instanceof NativeSourceCubeOrigin.Scene same&&same.hub()==hub&&same.room()==room&&same.id()==id)s=same;else return null;
  return new NativeSourceCubePlacement.Draft(engine,NativeSourceCubeOrigin.epoch(engine),old,s,new NativeSourceCubeOrigin.Creator(pc,binding));
 }
 static BattleCubeQuakeEffects.Placement place(BattleCubeQuakeEffects engine,long caster,int x,int y,long now,ClientSession pc,GameHub hub,long room){
  if(!NativeSourceCubePlacement.scoped(hub))return engine.nativeMaterialPlace(caster,x,y,now,pc);var d=prepare(engine,caster,pc,hub,room);if(d==null)return BattleCubeQuakeEffects.Placement.rejected();var old=NativeSourceCubeOrigin.installing(engine);CombatAccess.set(engine,"nativeSourceCubeInstalling",d);
  try{var p=engine.nativeMaterialPlace(caster,x,y,now,pc);if(p.accepted())NativeSourceCubePlacement.publish(d,p.cube());return p;}finally{CombatAccess.set(engine,"nativeSourceCubeInstalling",old);}
 }
 static BattleCubeShockEffects.Placement place(BattleCubeShockEffects engine,long caster,int x,int y,long now,ClientSession pc,GameHub hub,long room){
  if(!NativeSourceCubePlacement.scoped(hub))return engine.nativeMaterialPlace(caster,x,y,now,pc);var d=prepare(engine,caster,pc,hub,room);if(d==null)return BattleCubeShockEffects.Placement.rejected();var old=NativeSourceCubeOrigin.installing(engine);CombatAccess.set(engine,"nativeSourceCubeInstalling",d);
  try{var p=engine.nativeMaterialPlace(caster,x,y,now,pc);if(p.accepted())NativeSourceCubePlacement.publish(d,p.cube());return p;}finally{CombatAccess.set(engine,"nativeSourceCubeInstalling",old);}
 }
 static Object entity(Object cube,Object engine){
  var epoch=NativeSourceCubeOrigin.epoch(engine);var scene=NativeSourceCubeOrigin.scene(engine);var installing=NativeSourceCubeOrigin.installing(engine);NativeSourceCubeOrigin.Creator creator=null;
  if(installing instanceof NativeSourceCubePlacement.Draft d){if(d.engine()!=engine||!NativeSourceCubePlacement.same(d))throw new IllegalStateException("cube status material draft changed");epoch=d.epoch();scene=d.scene();creator=d.creator();}
  return new NativeSourceCubeOrigin.Entity(cube,engine,epoch,scene instanceof NativeSourceCubeOrigin.Scene s?s:null,creator,npc(engine),(long)CombatAccess.call(cube,"objectId","()J"));
 }
 static Object secondary(Object actual,Object status){var value=CombatAccess.field(status,"nativeSourceCubeOrigin");return value instanceof NativeSourceCubeOrigin.Lease p?new Link(p.effect(),p.target(),actual,actual.getClass().getName().endsWith("$BindLease")?"binds":"mrLeases"):null;}
 static void renew(Object actual,Object status){CombatAccess.set(actual,"nativeSourceCubeOrigin",secondary(actual,status));}
 static NativeSourceCubeOrigin.Entity effect(Object origin){return origin instanceof NativeSourceCubeOrigin.Lease p?p.effect():origin instanceof Link l?l.effect():origin instanceof NativeSourceCubeOrigin.Entity e?e:null;}
 static NativeSourceCubeOrigin.Target target(Object origin){return origin instanceof NativeSourceCubeOrigin.Lease p?p.target():origin instanceof Link l?l.target():null;}
 static Map<ClientSession,NativePlayerCommit.Binding> audience(NativeSourceCubeOrigin.Entity e){if(e.scene()==null||!Thread.holdsLock(e.scene().hub()))return Map.of();var map=new IdentityHashMap<ClientSession,NativePlayerCommit.Binding>();for(var pc:e.scene().hub().roomMembers(e.scene().id()))map.put(pc,NativePlayerCommit.Binding.capture(pc));return Collections.unmodifiableMap(map);}
 static Object emission(Object origin,boolean expired){var e=effect(origin);return e==null||e.scene()==null||(!(origin instanceof NativeSourceCubeOrigin.Entity)&&target(origin)==null)?null:new Emission(origin,expired,audience(e));}
 static Object baseEvent(Object event){return event instanceof BattleCubeQuakeEffects.Event q&&q.kind()==BattleCubeQuakeEffects.EventKind.CUBE_EXPIRED?cubeExpired(q.cube()):event instanceof BattleCubeShockEffects.Event q&&q.kind()==BattleCubeShockEffects.EventKind.CUBE_EXPIRED?cubeExpired(q.cube()):null;}
 static Object cubeExpired(Object cube){return emission(CombatAccess.field(cube,"nativeSourceCubeEntity"),true);}
 static Object event(Object event,Object engine,Object actual){
  String kind=String.valueOf(CombatAccess.call(event,"kind","()L"+P+type(engine)+"$EventKind;"));long id=(long)CombatAccess.call(event,"targetObjectId","()J");Object cube=CombatAccess.call(event,"cube","()L"+P+type(engine)+"$Cube;");
  if(actual!=null&&(kind.contains("BIND")||kind.contains("MR_"))&&actual.getClass().getName().endsWith("$StatusLease"))actual=null;
  if(actual==null){String table=kind.contains("BIND")?"binds":kind.contains("MR_")?"mrLeases":"statuses";for(var candidate:((Map<?,?>)CombatAccess.field(engine,table)).values())if((long)CombatAccess.field(candidate,"targetObjectId")==id&&CombatAccess.field(candidate,"cube")==cube){if(table.equals("statuses")&&!kind.startsWith(String.valueOf(CombatAccess.field(candidate,"kind"))+"_"))continue;if(actual!=null)return null;actual=candidate;}}
  return actual==null?null:emission(CombatAccess.field(actual,"nativeSourceCubeOrigin"),kind.endsWith("EXPIRED"));
 }
 static NativeSourceCubeOrigin.Scan scan(Object engine,List<?> actors,GameHub hub){
  if(!(NativeSourceCubeOrigin.scene(engine) instanceof NativeSourceCubeOrigin.Scene s)||s.hub()!=hub||!Thread.holdsLock(hub))return null;var targets=new HashMap<Long,NativeSourceCubeOrigin.Target>();var ambiguous=new HashSet<Long>();
  if(actors!=null)for(var actor:actors){long id=(long)CombatAccess.call(actor,"objectId","()J");String kind=String.valueOf(CombatAccess.call(actor,"kind","()L"+P+type(engine)+"$ActorKind;"));var t=NativeSourceCubeOrigin.target(s,id,kind);if(t!=null&&!ambiguous.contains(id)&&targets.putIfAbsent(id,t)!=null){targets.remove(id);ambiguous.add(id);}}
  return new NativeSourceCubeOrigin.Scan(NativeSourceCubeOrigin.epoch(engine),Map.copyOf(targets));
 }
 static boolean same(NativeSourceCubeOrigin.Target t,NativeSourceCubeOrigin.Scan s){return t!=null&&s!=null&&NativeSourceCubePlayer.sameTarget(t,s.targets().get(t.id()));}
 static void retire(Object engine){
  NativeSourceCubePlayer.retire(engine);if(!CombatRules.loaded())return;if(!(NativeSourceCubeOrigin.scene(engine) instanceof NativeSourceCubeOrigin.Scene s)||!Thread.holdsLock(s.hub())||!(CombatAccess.field(engine,"nativeSourceCubeScan") instanceof NativeSourceCubeOrigin.Scan scan))return;retireSecondary(engine,scan);
 }
 static void retireSecondary(Object engine,NativeSourceCubeOrigin.Scan scan){
  if(!Thread.holdsLock(engine))throw new IllegalStateException("original cube engine monitor required");if(scan==null||scan.epoch()!=NativeSourceCubeOrigin.epoch(engine))return;String table=engine instanceof BattleCubeQuakeEffects?"binds":"mrLeases";
  ((Map<?,?>)CombatAccess.field(engine,table)).values().removeIf(value->{var proof=CombatAccess.field(value,"nativeSourceCubeOrigin");return proof instanceof Link l&&l.target()!=null&&!same(l.target(),scan);});
 }
 static List<BattleCubeQuakeEffects.Event> advance(BattleCubeQuakeEffects engine,long now,List<BattleCubeQuakeEffects.Actor> actors,BattleCubeQuakeEffects.Alliance alliance,BattleCubeQuakeEffects.BindProtection protection,GameHub hub){var old=CombatAccess.field(engine,"nativeSourceCubeScan");try{CombatAccess.set(engine,"nativeSourceCubeScan",scan(engine,actors,hub));var events=engine.advance(now,actors,alliance,protection);freeze(events,now);return events;}finally{CombatAccess.set(engine,"nativeSourceCubeScan",old);}}
 static List<BattleCubeShockEffects.Event> advance(BattleCubeShockEffects engine,long now,List<BattleCubeShockEffects.Actor> actors,BattleCubeShockEffects.Alliance alliance,GameHub hub){var old=CombatAccess.field(engine,"nativeSourceCubeScan");try{CombatAccess.set(engine,"nativeSourceCubeScan",scan(engine,actors,hub));return engine.advance(now,actors,alliance);}finally{CombatAccess.set(engine,"nativeSourceCubeScan",old);}}
 static boolean entityCurrent(NativeSourceCubeOrigin.Entity e){return e!=null&&e.scene()!=null&&NativeSourceCubePlayer.entityCurrent(e,NativePlayerLifetime.of(e.scene().hub()));}
 static boolean targetCurrent(Object origin){var e=effect(origin);var t=target(origin);if(!entityCurrent(e)||t==null)return false;var s=e.scene();if(t.kind()==0)return NativeSourceCubePlayer.targetCurrent(t,t.life(),s);return NativeSourceCubeNpcLease.natural(t)&&CombatAccess.field(s.room(),t.kind()==1?"questEncounter":"timeAttackEncounter")==t.owner()&&CombatAccess.field(t.owner(),"sourceProcEncounterGeneration")==t.generation()&&NativeSourceArenaPoison.rawLife(t.actor())==t.life()&&(long)CombatAccess.field(t.actor(),"objectId")==t.id()&&(int)CombatAccess.field(t.actor(),"hp")>0;}
 static String key(Object actual){String name=actual.getClass().getName();String table=name.endsWith("$StatusLease")?"statuses:"+CombatAccess.field(actual,"kind"):name.endsWith("$BindLease")?"binds":"mrLeases";return table+":"+CombatAccess.field(actual,"targetObjectId");}
 static void reset(Object engine){NativeSourceCubeOrigin.reset(engine);CombatAccess.set(engine,"nativeSourceCubeIndex",Map.of());}
 static void seal(Object engine){var index=new HashMap<String,Object>();for(String table:List.of("statuses",engine instanceof BattleCubeQuakeEffects?"binds":"mrLeases"))for(var cell:((Map<?,?>)CombatAccess.field(engine,table)).values())index.put(key(cell),cell);for(var lease:(List<?>)CombatAccess.field(engine,"cubes")){var cube=CombatAccess.field(lease,"cube");index.put("cube:"+CombatAccess.call(cube,"objectId","()J"),cube);}CombatAccess.set(engine,"nativeSourceCubeIndex",Map.copyOf(index));}
 static Object indexed(NativeSourceCubeOrigin.Entity e,Object actual){return ((Map<?,?>)CombatAccess.field(e.engine(),"nativeSourceCubeIndex")).get(key(actual));}
 static boolean installed(Object origin){var e=effect(origin);if(!targetCurrent(origin))return false;Object actual=origin instanceof Link l?l.actual():((NativeSourceCubeOrigin.Lease)origin).actual();return indexed(e,actual)==actual&&CombatAccess.field(actual,"nativeSourceCubeOrigin")==origin;}
 static int read(Object engine,long id,int query){
  if(!CombatRules.loaded()||!(NativeSourceCubeOrigin.scene(engine) instanceof NativeSourceCubeOrigin.Scene))return -1;
  // Fixed Source stores the Shock marker but does not alter MR.
  String table=query==1?"binds":query==3?"mrLeases":"statuses";for(var cell:((Map<?,?>)CombatAccess.field(engine,table)).values())if((long)CombatAccess.field(cell,"targetObjectId")==id&&(query!=0||String.valueOf(CombatAccess.field(cell,"kind")).equals("ALLY")))return String.valueOf(CombatAccess.field(cell,"targetKind")).equals("BOT")?-1:query==3?0:installed(CombatAccess.field(cell,"nativeSourceCubeOrigin"))?1:0;return 0;
 }
 static void freeze(List<BattleCubeQuakeEffects.Event> events,long now){for(var event:events)if(event.kind()==BattleCubeQuakeEffects.EventKind.ENEMY_BIND_STARTED&&event.nativeSourceCubeOrigin instanceof Emission m&&m.origin() instanceof Link l&&l.target()!=null&&(l.target().kind()==1||l.target().kind()==2)){
  var s=l.effect().scene();synchronized(s.hub()){synchronized(l.target().owner()){if(installed(l)&&NativeSourceCubeNpc.originalCurrent(l.target(),s)&&NativeSourceCubeNpc.factionAllowed(l.target().kind(),CombatAccess.field(l.effect().cube(),"nativeSourceCubeFaction"),l.target().kind()==1?(int)CombatAccess.field(l.target().actor(),"force"):-1))CombatAccess.call(l.target().actor(),"activateFreeze","(JI)Z",now,1);}}
 }}
 static Object proof(Object event){return event instanceof BattleCubeQuakeEffects.Event q?q.nativeSourceCubeOrigin:event instanceof BattleCubeShockEffects.Event s?s.nativeSourceCubeOrigin:null;}
 static boolean current(Emission m){var e=effect(m.origin());if(!entityCurrent(e))return false;if(target(m.origin())==null){var cube=((Map<?,?>)CombatAccess.field(e.engine(),"nativeSourceCubeIndex")).get("cube:"+e.objectId());return m.origin() instanceof NativeSourceCubeOrigin.Entity&&(cube==null||cube==e.cube());}if(!targetCurrent(m.origin()))return false;if(!m.expired())return installed(m.origin());Object actual=m.origin() instanceof Link l?l.actual():((NativeSourceCubeOrigin.Lease)m.origin()).actual();var current=indexed(e,actual);return(current==null||current==actual)&&CombatAccess.field(actual,"nativeSourceCubeOrigin")==m.origin()&&(long)CombatAccess.field(actual,"expiresAtNanos")<=System.nanoTime();}
 static boolean dispatch(GameHub hub,Object tick){
  var event=CombatAccess.field(tick,"event");if(!CombatRules.loaded()||(int)CombatAccess.field(tick,"mapId")>=0||!(proof(event) instanceof Emission m))return false;var e=effect(m.origin());if(e==null||e.scene()==null||e.scene().hub()!=hub||(long)CombatAccess.field(tick,"roomId")!=e.scene().id())return true;
  try{if(!current(m))return true;var t=target(m.origin());if(t!=null&&t.kind()==0){var pc=t.pc();if(CombatAccess.field(tick,"target")!=pc)return true;var action=new NativeSourceRawDamage.Action(pc,t.id(),false,null);action.c.nativeSourceCubeStatusProof=m;
    NativeSourceRawDamage.owned(action,()->{if(!current(m))return;if(event instanceof BattleCubeQuakeEffects.Event q){if(q.kind().name().contains("BIND"))pc.nativeSourceCubeStatusOriginalQuakeBind(e.scene().id(),-1,-1,q);else pc.nativeSourceCubeStatusOriginalQuakeStatus(e.scene().id(),-1,-1,null,q);}else{var q=(BattleCubeShockEffects.Event)event;if(!q.kind().name().contains("MR_"))pc.nativeSourceCubeStatusOriginalShockStatus(e.scene().id(),-1,-1,null,q);}});return true;}
   if(t!=null&&t.kind()!=1&&t.kind()!=2)return false;
   byte[] packet;if(t==null)packet=Protocol.removeObject(e.objectId());else if(event instanceof BattleCubeQuakeEffects.Event q){if(q.kind()==BattleCubeQuakeEffects.EventKind.ALLY_APPLIED||q.kind()==BattleCubeQuakeEffects.EventKind.ENEMY_APPLIED)packet=Protocol.effectObject(t.id(),q.kind()==BattleCubeQuakeEffects.EventKind.ALLY_APPLIED?6714:6715);else return true;}else{var q=(BattleCubeShockEffects.Event)event;if(q.kind()!=BattleCubeShockEffects.EventKind.ALLY_APPLIED&&q.kind()!=BattleCubeShockEffects.EventKind.ENEMY_APPLIED)return true;packet=Protocol.effectObject(t.id(),q.kind()==BattleCubeShockEffects.EventKind.ALLY_APPLIED?6720:6721);}
   for(var pc:m.receivers().keySet()){if(!current(m))break;var action=new NativeSourceRawDamage.Action(pc,pc.playerObjectId(),false,null);action.c.nativeSourceCubeStatusProof=m;NativeSourceRawDamage.owned(action,()->{if(current(m))CombatAccess.call(pc,"sendEncrypted","([BLjava/lang/String;)V",packet,"Source cube status original effect NPC");});}
  }catch(IOException|RuntimeException failure){NativeSourcePhysicalNpc.failed(NativePlayerLifetime.of(hub));}return true;
 }
 static boolean allowFrame(ClientSession receiver,byte[] packet){var c=NativeAreaMaterialAction.current.get();if(c==null||!(c.nativeSourceCubeStatusProof instanceof Emission m))return true;var binding=m.receivers().get(receiver);if(binding==null||!NativeAcceptedFreezeMutation.roleFrame(binding,receiver)||!current(m))return false;var t=target(m.origin());if(t==null||t.pc()==receiver)return true;int x=t.kind()==0?NativeSourceDamageDisplay.n(t.pc(),"positionX"):(int)CombatAccess.field(t.actor(),"x"),y=t.kind()==0?NativeSourceDamageDisplay.n(t.pc(),"positionY"):(int)CombatAccess.field(t.actor(),"y");return Math.abs((long)NativeSourceDamageDisplay.n(receiver,"positionX")-x)<=10&&Math.abs((long)NativeSourceDamageDisplay.n(receiver,"positionY")-y)<=10;}
 static boolean allowRead(Object engine,long id,int query){return read(engine,id,query)!=0;}
 static void inherit(NativeAreaMaterialAction.Context c,NativeAreaMaterialAction.Context parent){if(parent!=null)c.nativeSourceCubeStatusProof=parent.nativeSourceCubeStatusProof;}
 static void inherit(NativeAreaMaterialAction.Context c){inherit(c,NativeAreaMaterialAction.current.get());}
 static void close(NativeAreaMaterialAction.Context c){if(c.nativeSourceCubeStatusProof instanceof Emission)c.nativeSourceCubeStatusProof=null;}
 private NativeSourceCubeStatus(){}
}
