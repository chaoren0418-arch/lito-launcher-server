package local.lito381.server;
/** TEST-JAR ONLY: observe existing distance operands without changing the original calculation. */
final class SourceNegativeGeometryTrace {
 static int signedDistance29(int dx,int dy,ClientSession selected,ClientSession owner,BattleCompanion pet,BattleCompanion.Snapshot snapshot,int tx,int ty){
  var c=NativeAreaMaterialAction.current.get();var a=NativeSourceNegativeTargets.aim();
  System.out.println("TEST-GEOMETRY skill29 dx="+dx+" dy="+dy+" caster="+(c==null?"absent":c.caster.object())+" selected="+(a==null?"absent":a.target.id())+" original-aim="+(a==null?"absent":a.x+","+a.y)+" current-pet="+(a==null||a.pet==null?"absent":a.pet.snapshot()));
  System.out.println("TEST-SELECTION selected-PC="+(selected==null?"null":selected.playerObjectId()+"/"+CombatAccess.field(selected,"positionX")+","+CombatAccess.field(selected,"positionY"))+" owner="+(owner==null?"null":owner.playerObjectId())+" same-pet="+(a!=null&&pet==a.pet)+" selected-snapshot="+snapshot+" target-locals="+tx+","+ty);
  return NativeSourceVisibilityRange.signedDistance29(dx,dy);
 }
 private SourceNegativeGeometryTrace(){}
}
