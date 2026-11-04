package local.lito381.server;
import java.util.*;
/** Complete missing caster membership on original typed PC lists before their cost/RNG boundary. */
final class NativeSourceAreaCasterTargets {
 static boolean ready(NativeAreaMaterialAction.Context c,ClientSession pc){
  if(!NativeSourcePlayerFactories.scoped(c)||c.pc!=pc||!(c.nativeSourceFirstPlayers instanceof NativeSourceFirstPlayers.Snapshot snapshot))return false;
  var m=snapshot.members().get(pc);
  return m!=null&&m.pc==pc&&m.binding!=null&&m.binding.equals(c.caster)&&m.life==m.acceptedLife&&!m.dead&&m.current(true)&&NativeSourceDamageDisplay.n(pc,"playerHp")>0&&pc.nativePlayerActionLock().isHeldByCurrentThread();
 }
 static List<ClientSession> players(List<ClientSession> original,ClientSession pc,int skill,long primary){
  var c=NativeAreaMaterialAction.current.get();
  if(!ready(c,pc)||NativeSkillTargetMetadata.size()==0)return original;
  NativeSkillTargetMetadata.Row row;try{row=NativeSkillTargetMetadata.row(skill);}catch(IllegalStateException missing){return original;}
  int flags=NativeSourceTargetConstruction.capture(row,skill);
  int insertion=-1;if((flags&96)!=0&&!original.stream().anyMatch(p->p==pc)){insertion=0;if((flags&32)==0&&!row.target().equals("none"))for(int i=0;i<original.size();i++){var binding=c.bindings.get(original.get(i));if(binding!=null&&binding.object()==primary){insertion=i+1;break;}}}
  return new Selection(original,pc,row,insertion,skill);
 }
 static Iterator<?> carry(Iterator<?> result,List<?> original){return original instanceof Selection selection?new Cursor((Iterator<ClientSession>)result,selection.row,selection.skill):result;}
 static Iterator<?> slowIterator(List<?> original){
  if(!(original instanceof Selection selection)||selection.skill!=76)return NativeSourceMassSlowTargets.calculationIterator(original);
  var c=NativeAreaMaterialAction.current.get();var a=NativeSourceMassSlowTargets.aim();var row=selection.row;
  boolean enabled=NativeSourcePlayerFactories.scoped(c)&&a!=null&&a.c==c&&!a.closed&&a.owner==c.pc&&row.type()==1&&row.area()==3&&row.targetTo()==3&&!row.through();
  if(enabled){if(a.pet==null)enabled=a.player==c.pc&&a.target==null;else{var n=a.target;enabled=a.player==null&&n!=null&&n.kind()==3&&n.actor()==a.pet&&n.owner()==c.pc&&c.npcs.get(n.id())==n;}}
  // This port consumes an ASM-verified PC-only list. The original NPC failure filter has no PC veto.
  if(enabled)return carry(NativeSourceMassSelfTarget.ordered(original).iterator(),original);
  if(a!=null&&a.owner==a.c.pc)return carry(Collections.emptyIterator(),original);
  return carry(original.iterator(),original);
 }
 static int available(Iterator<?> original,int skill){return original instanceof Cursor cursor&&cursor.skill==skill?1:NativeSkillTargetMetadata.size();}
 static NativeSkillTargetMetadata.Row row(int skill,Iterator<?> original){return original instanceof Cursor cursor&&cursor.skill==skill?cursor.row:NativeSkillTargetMetadata.row(skill);}
 static final class Selection extends AbstractList<ClientSession>{
  final List<ClientSession> original;final ClientSession caster;final NativeSkillTargetMetadata.Row row;final int insertion,skill;
  Selection(List<ClientSession> original,ClientSession caster,NativeSkillTargetMetadata.Row row,int insertion,int skill){this.original=original;this.caster=caster;this.row=row;this.insertion=insertion;this.skill=skill;}
  public int size(){return original.size()+(insertion>=0?1:0);}
  public ClientSession get(int index){Objects.checkIndex(index,size());return index==insertion?caster:original.get(index-(insertion>=0&&index>insertion?1:0));}
  public Iterator<ClientSession> iterator(){return new Cursor(super.iterator(),row,skill);}
 }
 static final class Cursor implements Iterator<ClientSession>{
  final Iterator<ClientSession> original;final NativeSkillTargetMetadata.Row row;final int skill;
  Cursor(Iterator<ClientSession> original,NativeSkillTargetMetadata.Row row,int skill){this.original=original;this.row=row;this.skill=skill;}
  public boolean hasNext(){return original.hasNext();}
  public ClientSession next(){return original.next();}
 }
 private NativeSourceAreaCasterTargets(){}
}
