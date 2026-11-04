package local.lito381.server;
import java.util.*;
/** Remaining typed PC lists use original caster admission and immutable Row metadata. */
final class NativeSourceRemainingAreaLists {
 static List<ClientSession> players(List<ClientSession> original,ClientSession pc,int skill,long primary){return NativeSourceAreaCasterTargets.players(original,pc,skill,primary);}
 static LinkedHashSet<ClientSession> set(LinkedHashSet<ClientSession> original,ClientSession pc,int skill){
  if(!NativeSourceAreaCasterTargets.ready(NativeAreaMaterialAction.current.get(),pc))return original;
  var selected=NativeSourceAreaCasterTargets.players(new ArrayList<>(original),pc,skill,0);
  return selected instanceof NativeSourceAreaCasterTargets.Selection selection?new Targets(selection):original;
 }
 static Iterator<?> attach(Iterator<?> original,Object anchor,int skill){NativeSkillTargetMetadata.Row row;if(anchor instanceof NativeSourceAreaCasterTargets.Selection selected&&selected.skill==skill)row=selected.row;else if(anchor instanceof Targets selected&&selected.skill==skill)row=selected.row;else return original;return new FrozenCursor(original,row,skill);}
 static NativeSkillTargetMetadata.Row row(int skill,Iterator<?> original){return original instanceof FrozenCursor cursor&&cursor.skill==skill?cursor.row:original instanceof Cursor cursor&&cursor.skill==skill?cursor.row:NativeSourceSleepConstruction.row(skill,original);}
 static int available(Iterator<?> original,int skill){return original instanceof FrozenCursor cursor&&cursor.skill==skill?1:original instanceof Cursor cursor&&cursor.skill==skill?1:NativeSourceSleepConstruction.available(original,skill);}
 static final class Targets extends LinkedHashSet<ClientSession>{
  final NativeSkillTargetMetadata.Row row;final int skill;
  Targets(NativeSourceAreaCasterTargets.Selection selected){super(selected);row=selected.row;skill=selected.skill;}
  public Iterator<ClientSession> iterator(){return new Cursor(super.iterator(),row,skill);}
 }
 static final class Cursor implements Iterator<ClientSession>{
  final Iterator<ClientSession> original;final NativeSkillTargetMetadata.Row row;final int skill;
  Cursor(Iterator<ClientSession> original,NativeSkillTargetMetadata.Row row,int skill){this.original=original;this.row=row;this.skill=skill;}
  public boolean hasNext(){return original.hasNext();}public ClientSession next(){return original.next();}public void remove(){original.remove();}
 }
 static final class FrozenCursor implements Iterator<Object>{final Iterator<?> original;final NativeSkillTargetMetadata.Row row;final int skill;FrozenCursor(Iterator<?> original,NativeSkillTargetMetadata.Row row,int skill){this.original=original;this.row=row;this.skill=skill;}public boolean hasNext(){return original.hasNext();}public Object next(){return original.next();}public void remove(){original.remove();}}
 private NativeSourceRemainingAreaLists(){}
}
