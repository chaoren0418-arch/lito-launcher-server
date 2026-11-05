package local.lito381.server;
import java.util.*;
/** Original Sleep construction uses one frozen skill row and the original first-discovery caster Member. */
final class NativeSourceSleepConstruction {
 static final class Frozen {
  final NativeSkillTargetMetadata.Row row;final int skill;final NativeSourceRawDamage.Member source;
  Frozen(NativeSkillTargetMetadata.Row row,int skill,NativeSourceRawDamage.Member source){this.row=row;this.skill=skill;this.source=source;}
 }
 static Frozen capture(ClientSession pc,int skill){var c=NativeAreaMaterialAction.current.get();if(!NativeSourcePlayerFactories.scoped(c)||c.pc!=pc||!(c.nativeSourceFirstPlayers instanceof NativeSourceFirstPlayers.Snapshot first)||NativeSkillTargetMetadata.size()==0)return null;var m=first.members().get(pc);if(m==null||m.binding==null||!m.binding.equals(c.caster))return null;try{return new Frozen(NativeSkillTargetMetadata.row(skill),skill,m);}catch(IllegalStateException missing){return null;}}
 static boolean valid(Frozen f){var c=NativeAreaMaterialAction.current.get();return f!=null&&NativeSourceAreaCasterTargets.ready(c,f.source.pc)&&c.nativeSourceFirstPlayers instanceof NativeSourceFirstPlayers.Snapshot first&&first.members().get(f.source.pc)==f.source;}
 static int flags(Frozen f){return NativeSourceTargetConstruction.capture(f.row,f.skill);}
 static boolean only(Frozen f){return valid(f)&&(flags(f)&32)!=0;}
 static boolean self(Frozen f){return valid(f)&&!NativeSourceTargetConstruction.reject(null,100,null,true,f.row.target().equals("none"),f.row.type(),f.skill,f.source.pc,Integer.valueOf(flags(f)));}
 static Protocol.AttackSpellRequest request(Protocol.AttackSpellRequest original,ClientSession pc,int skill,Frozen f){
  if(f==null)return NativeSourceNoneTargetRequest.request(original,pc,skill);
  if(original==null||original.skillId()!=skill||f.skill!=skill||f.source.pc!=pc||!valid(f)||!f.row.target().equals("none")&&(flags(f)&32)==0)return original;
  var m=f.source;return original.targetObjectId()==m.binding.object()&&original.x()==m.x&&original.y()==m.y?original:new Protocol.AttackSpellRequest(original.row(),original.column(),m.binding.object(),m.x,m.y);
 }
 static Map<?,?> phantasms(Map<?,?> original,Frozen f,Protocol.AttackSpellRequest request){
  if(original==null||!valid(f)||request==null||request.skillId()!=f.skill)return original;
  boolean me=only(f),required=me||(flags(f)&64)!=0||request.targetObjectId()==f.source.binding.object()&&self(f);
  if(!required)return original;
  long id=f.source.binding.object();if(me)return Map.of(id,f.source.pc.nativeSourceSleepCaster(id,f.source.x,f.source.y,false));
  if(original.containsKey(id))return original;
  var result=new LinkedHashMap<Object,Object>(original);result.put(id,f.source.pc.nativeSourceSleepCaster(id,f.source.x,f.source.y,false));return result;
 }
 static boolean identity(Object value,Frozen f){return valid(f)&&value!=null&&value.getClass().getName().equals("local.lito381.server.ClientSession$PatienceTarget")&&CombatAccess.field(value,"player")==f.source.pc&&CombatAccess.field(value,"kind")==BattleTrueTarget.Kind.PLAYER&&((Number)CombatAccess.field(value,"objectId")).longValue()==f.source.binding.object()&&(int)CombatAccess.field(value,"x")==f.source.x&&(int)CombatAccess.field(value,"y")==f.source.y;}
 static boolean primary(boolean original,Object value,Frozen f){return original||identity(value,f)&&self(f);}
 static boolean canAttack(GameHub hub,long room,ClientSession pc,ClientSession target,Frozen f){return target==pc&&valid(f)&&f.source.pc==pc&&self(f)||hub.canAttack(room,pc,target);}
 static boolean line(ArenaMapCatalog.Arena arena,int x,int y,int tx,int ty,ClientSession pc,int skill,Frozen f){if(!valid(f)||f.source.pc!=pc||f.skill!=skill)return NativeSourceTargetLos.line(arena,x,y,tx,ty,pc,skill);return only(f)||!NativeSourceTargetLos.requires(false,f.row.type(),f.row.through())||BattleSpellCatalog.hasNativeLineOfSight(arena,x,y,tx,ty);}
 static List<?> targets(List<?> original,Frozen f,Protocol.AttackSpellRequest request){
  if(!valid(f)||request==null||request.skillId()!=f.skill)return original;
  if(only(f))return new Selection(List.of(f.source.pc.nativeSourceSleepCaster(f.source.binding.object(),f.source.x,f.source.y,true)),f);
  boolean include=(flags(f)&64)!=0||request.targetObjectId()==f.source.binding.object()&&self(f);var selected=new ArrayList<Object>(original);int primary=-1;boolean found=false;
  for(int i=0;i<selected.size();i++){Object raw=selected.get(i);if(NativeSourceAreaClassSelection.node(raw)==f.source.pc)found=true;Object id=raw==null?null:CombatAccess.field(raw,"identity");if(id!=null&&((Number)CombatAccess.field(id,"objectId")).longValue()==request.targetObjectId()&&primary<0)primary=i;}
  boolean none=f.row.target().equals("none");if(!none&&primary>0){var value=selected.remove(primary);selected.add(0,value);primary=0;}
  if(include&&!found)selected.add(!none&&primary==0?1:0,f.source.pc.nativeSourceSleepCaster(f.source.binding.object(),f.source.x,f.source.y,true));
  return new Selection(selected,f);
 }
 static NativeSkillTargetMetadata.Row row(int skill,Iterator<?> original){return original instanceof Cursor cursor&&cursor.frozen.skill==skill?cursor.frozen.row:NativeSourceAreaCasterTargets.row(skill,original);}
 static int available(Iterator<?> original,int skill){return original instanceof Cursor cursor&&cursor.frozen.skill==skill?1:NativeSourceAreaCasterTargets.available(original,skill);}
 static final class Selection extends AbstractList<Object>{final List<?> original;final Frozen frozen;Selection(List<?> original,Frozen frozen){this.original=List.copyOf(original);this.frozen=frozen;}public int size(){return original.size();}public Object get(int i){return original.get(i);}public Iterator<Object> iterator(){return new Cursor(original.iterator(),frozen);}}
 static final class Cursor implements Iterator<Object>{final Iterator<?> original;final Frozen frozen;Cursor(Iterator<?> original,Frozen frozen){this.original=original;this.frozen=frozen;}public boolean hasNext(){return original.hasNext();}public Object next(){return original.next();}}
 private NativeSourceSleepConstruction(){}
}
