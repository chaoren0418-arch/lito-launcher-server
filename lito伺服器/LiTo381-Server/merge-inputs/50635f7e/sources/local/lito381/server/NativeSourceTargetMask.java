package local.lito381.server;
/** Source class-mask projection after original selection; original clan/party/competition authority remains. */
final class NativeSourceTargetMask {
 static boolean reject(int mask,boolean player,boolean self,boolean primaryPc,boolean pet,boolean owned,int type,int skill){
  if(player){if(self){if(type==64||skill==49)return true;if((mask&13)!=0)return false;}return (mask&1)==0&&(!primaryPc||(mask&12)==0);}
  return (mask&2)==0&&(!(pet&&owned)||(mask&16)==0);
 }
 static boolean owned(Object node,NativeAreaMaterialAction.Context c){
  if(node instanceof BattleCompanion.Snapshot s){var n=c.npcs.get(s.objectId());return n!=null&&n.kind()==3&&n.owner()==c.pc;}
  if(node instanceof BattleCompanion actor)for(var n:c.npcs.values())if(n.kind()==3&&n.actor()==actor)return n.owner()==c.pc;
  return false;
 }
 static boolean reject(NativeSourceNpcFactoryType.Stamp selected,int maximum,NativeSourceNpcFactoryType.Stamp primary,boolean player,boolean none,int type,int skill,Object node,Integer mask){
  if(NativeSourceAreaClassSelection.reject(selected,maximum,primary,player,none,type,skill))return true;
  var c=NativeAreaMaterialAction.current.get();if(mask==null||!NativeSourcePlayerFactories.scoped(c))return false;
  boolean self=player&&node==c.pc;
  if(self&&(type==64||skill==49))return true;
  if(self&&(mask&13)!=0)return false;
  // Preserve the earlier Source absolute-barrier and hidden detection return paths.
  if(player&&((BattleTimedStatus)CombatAccess.field(node,"timedStatus")).absoluteBarrierActive(System.nanoTime()))return false;
  if(!player&&(skill==13||skill==72))return false;
  if(!player&&selected==null)return false;
  return reject(mask,player,self,primary==null,selected!=null&&(selected.kind()==5||selected.kind()==6),!player&&owned(node,c),type,skill);
 }
 private NativeSourceTargetMask(){}
}
