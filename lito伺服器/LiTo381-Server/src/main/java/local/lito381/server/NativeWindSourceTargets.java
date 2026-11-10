package local.lito381.server;
import java.util.*;
/** Correct Source skill167 target scope after original selection, admission and costs. */
final class NativeWindSourceTargets {
 static long id(Object actor){return actor instanceof ClientSession pc?pc.playerObjectId():((Number)CombatAccess.call(actor,"objectId","()J")).longValue();}
 static int coordinate(Object actor,String axis){return actor instanceof ClientSession pc?NativePlayerCommit.integer(pc,axis.equals("x")?"positionX":"positionY"):((Number)CombatAccess.call(actor,axis,"()I")).intValue();}
 static List<?> filter(List<?> original,ClientSession pc,long selected,BattleSpellCatalog.WindShackleSpell spell){
  var c=NativeAreaMaterialAction.current.get();if(c==null||c.pc!=pc||!c.scene())return List.of();var row=NativeSkillTargetMetadata.row(167);
  if(row.area()!=0)throw new IllegalStateException("skill167 nonzero Source area requires its complete spatial target collector");
  int x=NativePlayerCommit.integer(pc,"positionX"),y=NativePlayerCommit.integer(pc,"positionY");
  return original.stream().filter(actor->id(actor)==selected&&(row.targetTo()&(actor instanceof ClientSession?1:2))!=0).filter(actor->{int tx=coordinate(actor,"x"),ty=coordinate(actor,"y");if(row.ranged()>=0&&Math.max(Math.abs(x-tx),Math.abs(y-ty))>row.ranged())return false;return !c.phase.equals("WORLD")||row.through()&&(row.type()&64)==0||ClassicWorldMapCatalog.forMap(c.map).attackPosition(x,y,tx,ty,15);}).toList();
 }
 private NativeWindSourceTargets(){}
}
