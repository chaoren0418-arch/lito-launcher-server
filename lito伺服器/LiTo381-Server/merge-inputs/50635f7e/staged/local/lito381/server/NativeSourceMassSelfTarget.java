package local.lito381.server;
import java.util.*;
/** Source self-primary preserves selected secondary actors; no discovery, role or lifetime adoption. */
final class NativeSourceMassSelfTarget {
 static boolean enabled(){var c=NativeAreaMaterialAction.current.get();var a=NativeSourceMassSlowTargets.aim();if(!NativeSourcePlayerFactories.scoped(c)||a==null||a.c!=c||a.closed||a.owner!=c.pc||NativeSkillTargetMetadata.size()==0)return false;var row=NativeSkillTargetMetadata.row(76);if(row.type()!=1||row.area()!=3||row.targetTo()!=3||row.through())return false;if(a.pet==null)return a.player==c.pc&&a.target==null;var n=a.target;return a.player==null&&n!=null&&n.kind()==3&&n.actor()==a.pet&&n.owner()==c.pc&&c.npcs.get(n.id())==n;}
 static boolean self(ClientSession caster,ClientSession target){return target==caster&&enabled()&&NativeAreaMaterialAction.current.get().pc==caster;}
 static boolean excludeSelf(ClientSession target,ClientSession caster){return target==caster&&!self(caster,target);}
 static List<?> ordered(List<?> targets){var a=NativeSourceMassSlowTargets.aim();if(a==null||a.player!=a.c.pc)return targets;int first=-1;for(int i=0;i<targets.size();i++)if(targets.get(i)==a.player){first=i;break;}if(first<=0)return targets;var result=new ArrayList<Object>(targets.size());result.add(targets.get(first));for(int i=0;i<targets.size();i++)if(i!=first)result.add(targets.get(i));return result;}
 static Iterator<?> iterator(List<?> targets){return enabled()?NativeSourceNpcTargetFailure.iterator(ordered(targets).iterator(),76):null;}
 private NativeSourceMassSelfTarget(){}
}
