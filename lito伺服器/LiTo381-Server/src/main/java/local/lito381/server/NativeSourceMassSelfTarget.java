package local.lito381.server;
import java.util.*;
/** Source76 self / own-companion primary: cost and motion stay, no target is calculated (L1SkillUse:2779). */
final class NativeSourceMassSelfTarget {
 static boolean enabled(){var c=NativeAreaMaterialAction.current.get();var a=NativeSourceMassSlowTargets.aim();if(!NativeSourcePlayerFactories.scoped(c)||a==null||a.c!=c||a.closed||a.owner!=c.pc||NativeSkillTargetMetadata.size()==0)return false;var row=NativeSkillTargetMetadata.row(76);if(row.type()!=1||row.area()!=3||row.targetTo()!=3||row.through())return false;if(a.pet==null)return a.player==c.pc&&a.target==null;var n=a.target;return a.player==null&&n!=null&&n.kind()==3&&n.actor()==a.pet&&n.owner()==c.pc&&c.npcs.get(n.id())==n;}
 static boolean self(ClientSession caster,ClientSession target){return target==caster&&enabled()&&NativeAreaMaterialAction.current.get().pc==caster;}
 static boolean excludeSelf(ClientSession target,ClientSession caster){return target==caster&&!self(caster,target);}
 /**
  * T114 (Issue #321, G05): Source {@code L1SkillUse.runSkill} rejects a self / own-companion primary through
  * {@code isTargetCalc(_target)} (MASS_SLOW excludes the caster and the caster's pet/summon) and returns before
  * any per-target calculation. MP, reuse and the cast motion are already committed by then, so the effect list
  * is empty while the existing cost/motion path is kept.
  */
 static <T> Iterator<T> selfPrimaryEffects(List<T> targets){Objects.requireNonNull(targets);return Collections.emptyIterator();}
 static Iterator<?> iterator(List<?> targets){return enabled()?NativeSourceNpcTargetFailure.iterator(selfPrimaryEffects(targets),76):null;}
 private NativeSourceMassSelfTarget(){}
}
