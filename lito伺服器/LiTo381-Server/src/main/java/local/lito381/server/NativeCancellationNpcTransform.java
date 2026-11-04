// ORIGINAL SOURCE moved from lito伺服器/LiTo381-Server/tools/combat-continuation/cancellation-npc/NativeCancellationNpcTransform.java (T002); replaces CFR decompile of reference/base-0.6.71.jar
package local.lito381.server;
/** Exact Source transform branch with explicit scene-owned entity and publication ports. */
final class NativeCancellationNpcTransform {
 sealed interface Notice permits Name,Shape {}
 record Name(long objectId,String name) implements Notice {}
 record Shape(long objectId,int gfx) implements Notice {}
 interface Target {
  long getId(); int getTempCharGfx(); int getGfxId();
  int getX(); int getY(); int getMapId(); int getHeading();
  void setTempCharGfx(int gfx); void setPassispeed(int speed); void setNameId(String name);
  void broadcastPacketAll(Notice notice); void deleteMe();
 }
 @FunctionalInterface interface Spawn {Target create(int npcId,int x,int y,int map,int heading,int durationSeconds);}
	static boolean apply(final Target npc, final NativeCancellationNpcRules.Rule rule, final Spawn spawn) {
		if (rule.type() == 1) {
			// 已被其他功能改變外型時不覆蓋；也避免重複施放反覆觸發。
			if (npc.getTempCharGfx() != npc.getGfxId()
					|| npc.getTempCharGfx() == rule.targetId()) {
				return false;
			}
			npc.setTempCharGfx(rule.targetId());
			if (rule.passispeed() > 0) {
				npc.setPassispeed(rule.passispeed());
			}
			if (!rule.nameId().isEmpty()) {
				npc.setNameId(rule.nameId());
				npc.broadcastPacketAll(new Name(npc.getId(), rule.nameId()));
			}
			npc.broadcastPacketAll(new Shape(npc.getId(), rule.targetId()));
			return true;
		}

		if (rule.type() == 2) {
			final Target replacement = spawn.create(rule.targetId(), npc.getX(), npc.getY(),
					npc.getMapId(), npc.getHeading(), rule.durationSeconds());
			if (replacement == null) {
				return false;
			}
			npc.deleteMe();
			return true;
		}
		return false;
	}

 private NativeCancellationNpcTransform(){}
}
