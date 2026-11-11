package local.lito381.server;
import java.util.*;
/** Complete Source76 NPC selection/filter/geometry metadata from its existing pre-cost PC anchor. */
final class NativeSourceSlowFailureMetadata {
 static NativeSkillTargetMetadata.Row row(Object anchor){var c=NativeAreaMaterialAction.current.get();return NativeSourcePlayerFactories.scoped(c)&&anchor instanceof NativeSourceAreaCasterTargets.Selection selected&&selected.skill==76&&selected.caster==c.pc?selected.row:null;}
 static boolean probability(NativeSkillTargetMetadata.Row row){return row.type()==1||row.type()==4;}
 static boolean selectionErase(SourceNpcPhysicalStats stats,Object target,Object anchor){var row=row(anchor);return row==null?NativeSourceNpcTargetFailure.selectionErase(stats,target):NativeSourceNpcTargetFailure.known(target)!=null&&probability(row)||stats.eraseEligible();}
 static boolean enabled(NativeSkillTargetMetadata.Row row){var c=NativeAreaMaterialAction.current.get();var a=NativeSourceMassSlowTargets.aim();if(!NativeSourcePlayerFactories.scoped(c)||a==null||a.c!=c||a.closed||a.owner!=c.pc||row.type()!=1||row.area()!=3||row.targetTo()!=3||row.through())return false;if(a.pet==null)return a.player==c.pc&&a.target==null;var n=a.target;return a.player==null&&n!=null&&n.kind()==3&&n.actor()==a.pet&&n.owner()==c.pc&&c.npcs.get(n.id())==n;}
 static Iterator<?> iterator(List<?> original,Object anchor){var row=row(anchor);if(row==null)return NativeSourceMassSlowTargets.calculationIterator(original);var a=NativeSourceMassSlowTargets.aim();Iterator<?> selected;if(enabled(row))selected=NativeSourceMassSelfTarget.selfPrimaryEffects(original);else if(a!=null&&a.owner==a.c.pc)selected=Collections.emptyIterator();else selected=original.iterator();return probability(row)?new NativeSourceNpcTargetFailure.Cursor(selected,76):selected;}
 static int visualCount(int original,Object anchor){var row=row(anchor);return row==null?NativeSourceMassSlowTargets.visualCount(original):row.area()!=0&&!row.target().equals("none")&&!row.target().equals("attack")&&row.type()!=64?1:original;}
 static int distance(int dx,int dy,Object anchor){var row=row(anchor);return row==null?NativeSourceVisibilityRange.signedDistance76(dx,dy):row.ranged()==-1&&!BattleSpellCatalog.isInNativeScreen(dx,dy,0,0)?Integer.MAX_VALUE:Math.max(Math.abs(dx),Math.abs(dy));}
 private NativeSourceSlowFailureMetadata(){}
}
