package local.lito381.server;
import java.io.*;
/** Actual current Source194 area0/through0 selection; reuse native single-target admission and accepted HP plans. */
final class NativeSourceFreezeTarget {
 static boolean applies(BattleSpellCatalog.FreezeAttackSpell spell){return spell.skillId()==50||spell.skillId()==194;}
 static boolean line(BattleSpellCatalog.FreezeAttackSpell spell){if(spell.skillId()!=194||NativeSkillTargetMetadata.size()==0)return spell.lineTargeted();check();return false;}
 static void check(){var r=NativeSkillTargetMetadata.row(194);if(r.area()!=0||!r.target().equals("attack"))throw new IllegalStateException("Source194 target metadata requires an unqualified area selector");}
 static int range(BattleSpellCatalog.FreezeAttackSpell spell){if(spell.skillId()!=194||NativeSkillTargetMetadata.size()==0)return spell.nativeSourceOriginalMaximumRange();check();int n=NativeSkillTargetMetadata.row(194).ranged();return n<0?18:n;}
 static int roll(ClientSession pc,BattleSpellCatalog.FreezeAttackSpell spell){if(spell.skillId()==194){int value=((ServerConfig)CombatAccess.field(pc,"config")).freezingBreathFixtureD100();if(value>=1&&value<=100)return value;}return java.util.concurrent.ThreadLocalRandom.current().nextInt(1,101);}
 static void selector(ClientSession pc,byte[] packet,BattleSpellCatalog.FreezeAttackSpell spell)throws IOException{
  if(spell.skillId()!=194){pc.nativeSourceFreezeOriginalSelector(packet,spell);return;}var selected=Protocol.selectorOnlySpellRequest(packet);if(selected.skillId()!=194)return;check();var request=(Protocol.AttackSpellRequest)CombatAccess.call(pc,"resolveSelectorOnlyPrimary","(IIIILjava/lang/String;)Llocal/lito381/server/Protocol$AttackSpellRequest;",spell.skillId(),spell.row(),spell.column(),spell.maximumRange(),"freeze-source-selected");if(request==null)return;
  byte[] raw=new Protocol.Writer().c(Protocol.C_USE_SPELL).c(spell.row()).c(spell.column()).d(request.targetObjectId()).h(request.x()).h(request.y()).bytes();String phase=String.valueOf(CombatAccess.field(pc,"phase"));if(phase.equals("WORLD"))NativeAcceptedFreezeMutation.world(pc,raw,spell);else if(phase.equals("BATTLE"))NativeAcceptedFreezeMutation.battle(pc,raw,spell);
 }
 private NativeSourceFreezeTarget(){}
}
