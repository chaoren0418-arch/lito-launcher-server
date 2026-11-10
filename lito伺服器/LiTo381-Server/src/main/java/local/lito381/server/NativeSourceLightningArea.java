package local.lito381.server;
import java.util.*;
/** Source17: the current calcType is1..4, so its unreachable LIGHTNING switch falls through to area3. */
final class NativeSourceLightningArea {
 static List<ArenaMapCatalog.Point> points(int sx,int sy,int tx,int ty,BattleSpellCatalog.DirectionalLineSpell spell){
  if(spell.skillId()!=17||NativeSkillTargetMetadata.size()==0)return BattleStraightLine.points(sx,sy,tx,ty);
  var row=NativeSkillTargetMetadata.row(17);if(row.type()!=64||row.targetTo()!=3||row.ranged()!=6||row.area()!=3||row.through()||!row.target().equals("attack"))throw new IllegalStateException("unqualified Source17 shape metadata");
  return area(tx,ty);
 }
 static List<ArenaMapCatalog.Point> area(int x,int y){var points=new ArrayList<ArenaMapCatalog.Point>(49);for(int dx=-3;dx<=3;dx++)for(int dy=-3;dy<=3;dy++)points.add(new ArenaMapCatalog.Point(Math.addExact(x,dx),Math.addExact(y,dy)));return List.copyOf(points);}
 private NativeSourceLightningArea(){}
}
