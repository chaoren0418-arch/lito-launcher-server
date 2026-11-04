package local.lito381.server;
import java.io.*;import java.util.*;
/** Original ground lease -> pulse -> tick provenance; PC firewall only, no replacement timers. */
final class NativeSourceGroundOrigin {
 static final String P="local/lito381/server/";
 record Scene(GameHub hub,Object room,long id){}
 record Creator(ClientSession pc,NativePlayerCommit.Binding binding){}
 record Entity(Object actual,BattleGroundEffects engine,Object epoch,Scene scene,Creator creator){}
 record Actor(ClientSession pc,NativePlayerCommit.Binding binding,Object life){}
 record PulseProof(Entity entity,Actor source,Actor target){}
 static Object epoch(BattleGroundEffects engine){return engine.nativeSourceGroundEpoch;}
 static void reset(BattleGroundEffects engine){engine.nativeSourceGroundEpoch=new Object();}
 static boolean same(Entity e,BattleGroundEffects engine,Object lease){return e!=null&&e.engine()==engine&&e.actual()==lease&&e.epoch()==epoch(engine)&&e.scene()==engine.nativeSourceGroundScene;}
 static BattleGroundEffects.CastResult cast(BattleGroundEffects engine,BattleSpellCatalog.GroundSpell spell,long caster,int sx,int sy,int tx,int ty,ArenaMapCatalog.Arena arena,long now,GameHub hub,long roomId,ClientSession pc){
  Object previous=engine.nativeSourceGroundInstalling;
  try{
   if(spell!=null&&spell.skillId()==58&&NativeSourceDollCommit.enabled(pc)&&Thread.holdsLock(hub)){
    var room=((Map<?,?>)CombatAccess.field(hub,"rooms")).get(roomId);
    if(room==null||CombatAccess.field(room,"groundEffects")!=engine)throw new IllegalStateException("ground installation owner mismatch");
    if(!(engine.nativeSourceGroundScene instanceof Scene s)||s.hub()!=hub||s.room()!=room||s.id()!=roomId){if(engine.nativeSourceGroundScene!=null)reset(engine);engine.nativeSourceGroundScene=new Scene(hub,room,roomId);}
    engine.nativeSourceGroundInstalling=new Creator(pc,NativePlayerCommit.Binding.capture(pc));
   }
   return engine.cast(spell,caster,sx,sy,tx,ty,arena,now);
  }finally{engine.nativeSourceGroundInstalling=previous;}
 }
 static Object entity(Object actual,BattleGroundEffects engine){
  if(!(engine.nativeSourceGroundScene instanceof Scene s)||!(engine.nativeSourceGroundInstalling instanceof Creator creator)||(int)CombatAccess.field(actual,"skillId")!=58)return null;
  return new Entity(actual,engine,epoch(engine),s,creator);
 }
 static boolean role(Actor actor,Scene s){var pc=actor==null?null:actor.pc();return pc!=null&&actor.binding()!=null&&NativeAcceptedFreezeMutation.roleFrame(actor.binding(),pc)&&CombatAccess.field(pc,"hub")==s.hub()&&! (boolean)CombatAccess.field(pc,"sourceItemDisconnected")&&!(boolean)CombatAccess.field(pc,"observer")&&"BATTLE".equals(String.valueOf(CombatAccess.field(pc,"phase")))&&(long)CombatAccess.field(pc,"currentRoomId")==s.id()&&NativeSourceControlledDisplay.rawLife(pc)==actor.life();}
 static Map<Long,Actor> scan(BattleGroundEffects engine,List<BattleGroundEffects.Actor> actors,GameHub hub){
  if(!(engine.nativeSourceGroundScene instanceof Scene s)||s.hub()!=hub||!Thread.holdsLock(hub))return Map.of();
  var next=new HashMap<Long,Actor>();var ambiguous=new HashSet<Long>();
  if(actors!=null)for(var visible:actors)if(visible!=null&&!visible.bot())for(var value:(Collection<?>)CombatAccess.field(s.room(),"battleRoster"))if(value instanceof ClientSession pc&&pc.playerObjectId()==visible.objectId()){
   var actor=new Actor(pc,NativePlayerCommit.Binding.capture(pc),NativeSourceControlledDisplay.rawLife(pc));
   if(role(actor,s)&&!ambiguous.contains(visible.objectId())&&next.putIfAbsent(visible.objectId(),actor)!=null){next.remove(visible.objectId());ambiguous.add(visible.objectId());}
  }
  return Map.copyOf(next);
 }
 static List<BattleGroundEffects.Pulse> advance(BattleGroundEffects engine,long now,List<BattleGroundEffects.Actor> actors,GameHub hub){var previous=engine.nativeSourceGroundScan;try{engine.nativeSourceGroundScan=scan(engine,actors,hub);return engine.advance(now,actors);}finally{engine.nativeSourceGroundScan=previous;}}
 static Object pulse(BattleGroundEffects.Pulse pulse,BattleGroundEffects engine,Object lease){
  var value=CombatAccess.field(lease,"nativeSourceGroundEntity");if(!(value instanceof Entity e)||!same(e,engine,lease)||pulse.skillId()!=58||pulse.bot()||!(engine.nativeSourceGroundScan instanceof Map<?,?> scan))return null;
  var creator=e.creator();var source=new Actor(creator.pc(),creator.binding(),NativeSourceControlledDisplay.rawLife(creator.pc()));
  return new PulseProof(e,source,scan.get(pulse.targetObjectId()) instanceof Actor target?target:null);
 }
 static ClientSession caster(ClientSession original,BattleGroundEffects.Pulse pulse){return pulse.nativeSourceGroundProof instanceof PulseProof p?p.source().pc():original;}
 static Object tick(BattleGroundEffects.Pulse pulse){return pulse.nativeSourceGroundProof;}
 static boolean current(PulseProof p){if(p==null||p.entity()==null||p.source()==null||p.target()==null)return false;var e=p.entity();var s=e.scene();return s!=null&&same(e,e.engine(),e.actual())&&NativePlayerLifetime.of(s.hub()).mode!=NativePlayerLifetime.Mode.STOPPED&&!(boolean)CombatAccess.field(s.hub(),"nativeStaticMotionClosed")&&p.source().pc()==e.creator().pc()&&Objects.equals(p.source().binding(),e.creator().binding())&&role(p.source(),s)&&role(p.target(),s);}
 static boolean installed(PulseProof p){var s=p.entity().scene();synchronized(s.hub()){
  if(!current(p)||((Map<?,?>)CombatAccess.field(s.hub(),"rooms")).get(s.id())!=s.room()||!(boolean)CombatAccess.field(s.room(),"battleStarted"))return false;
  for(var lease:(List<?>)CombatAccess.field(p.entity().engine(),"leases"))if(lease==p.entity().actual())return true;return false;
 }}
 static void dispatch(ClientSession target,long room,ClientSession caster,int skill,Object tick)throws IOException{
  var value=CombatAccess.field(tick,"nativeSourceGroundProof");
  if(!(value instanceof PulseProof proof)||!CombatRules.loaded()){target.serverGroundPulse(room,caster,skill);return;}
  if(skill!=58||proof.entity()==null||proof.entity().scene()==null||proof.target()==null||proof.source()==null||proof.target().pc()!=target||proof.source().pc()!=caster||proof.entity().scene().id()!=room||!installed(proof))return;
  if(NativeSourceRawDamage.current()!=null)return;
  var action=new NativeSourceRawDamage.Action(target,target.playerObjectId(),false,caster);action.c.nativeSourceGroundProof=proof;
  NativeSourceRawDamage.owned(action,()->{if(current(proof))target.serverGroundPulse(room,caster,skill);});
 }
 static void damage(ClientSession target,int damage,ClientSession caster)throws IOException{
  var c=NativeAreaMaterialAction.current.get();if(c!=null&&c.pc==target&&c.nativeSourceGroundProof instanceof PulseProof p){if(p.target()!=null&&p.target().pc()==target&&p.source()!=null&&p.source().pc()==caster&&current(p))NativeSourceRawDamage.player(target,damage,caster,false,true);return;}
  CombatAccess.call(target,"takePlayerDamage","(IL"+P+"ClientSession;)V",damage,caster);
 }
 static void close(NativeAreaMaterialAction.Context c){if(c.nativeSourceGroundProof instanceof PulseProof)c.nativeSourceGroundProof=null;}
 static boolean allowFrame(ClientSession pc,byte[] packet){var c=NativeAreaMaterialAction.current.get();return c==null||!(c.nativeSourceGroundProof instanceof PulseProof p)||current(p);}
 private NativeSourceGroundOrigin(){}
}
