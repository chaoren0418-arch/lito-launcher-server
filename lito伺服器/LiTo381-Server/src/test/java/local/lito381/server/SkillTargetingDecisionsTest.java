package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Issue #321（T114）：技能目標判定 3 項使用者決定。
 *
 * <ul>
 *   <li>S1 範圍攻擊：維持 LiTo，規則寫在 {@code docs/design/gameplay.md} §2.9，程式不改。</li>
 *   <li>S2（G05）76 以自己或自己的寵物為主目標：對齊 1-伺服器 —— 扣 MP、播施法動作、不對任何人生效
 *       （{@code L1SkillUse.runSkill} 的 {@code isTargetCalc(_target)} 在逐目標計算前返回）。</li>
 *   <li>S3 103 對 1～3 格的寵物：1-伺服器 射程外清單為空時同樣扣 MP、播動作、送失敗訊息，
 *       與 LiTo 現況一致，程式不改；本測試鎖住 LiTo 現況。</li>
 * </ul>
 */
final class SkillTargetingDecisionsTest {
    private static final Path MAIN = Path.of("src/main/java/local/lito381/server");

    private static String source(String name) throws Exception {
        return Files.readString(MAIN.resolve(name), StandardCharsets.UTF_8);
    }

    private static String method(String session, String signature) {
        int start = session.indexOf(signature);
        assertTrue(start >= 0, signature);
        int end = session.indexOf("\n   void ", start + signature.length());
        return session.substring(start, end < 0 ? session.length() : end);
    }

    @Test void s2SelfPrimaryEffectListIsEmpty() {
        assertFalse(NativeSourceMassSelfTarget.selfPrimaryEffects(List.of("enemy", "party", "monster")).hasNext());
        assertFalse(NativeSourceMassSelfTarget.selfPrimaryEffects(List.of()).hasNext());
        assertThrows(NullPointerException.class, () -> NativeSourceMassSelfTarget.selfPrimaryEffects(null));
    }

    @Test void s2SourceModelHasNoEffectForSelfOrOwnPetPrimary() {
        var row = new NativeSkillTargetMetadata.Row("buff", 3, -1, 3, false, 1, 1, 8);
        assertTrue(SourceSkillTargeting.effects(76, row, SourceSkillTargeting.Kind.SELF).isEmpty());
        assertTrue(SourceSkillTargeting.effects(76, row, SourceSkillTargeting.Kind.PET).isEmpty());
        assertTrue(SourceSkillTargeting.effects(76, row, SourceSkillTargeting.Kind.ENEMY)
                .contains(SourceSkillTargeting.Kind.ENEMY));
    }

    /** 三個「自施／自己寵物為主目標」分支都改成空清單，不再保留旁人。 */
    @Test void s2AllSelfPrimaryBranchesUseEmptyEffects() throws Exception {
        String self = source("NativeSourceMassSelfTarget.java");
        assertTrue(self.contains("NativeSourceNpcTargetFailure.iterator(selfPrimaryEffects(targets),76)"));
        assertFalse(self.contains("ordered("), "self-primary must not keep secondary targets");
        assertTrue(source("NativeSourceAreaCasterTargets.java")
                .contains("if(enabled)return carry(NativeSourceMassSelfTarget.selfPrimaryEffects(original),original);"));
        assertTrue(source("NativeSourceSlowFailureMetadata.java")
                .contains("if(enabled(row))selected=NativeSourceMassSelfTarget.selfPrimaryEffects(original);"));
    }

    /** 扣 MP 與 S_ACTION 都在目標迭代器之前，所以清單為空時仍扣 MP、播動作（1-伺服器 L1SkillUse:1121-1140 送動作後 runSkill，:1196-1199 再扣費；同樣不看清單是否為空）。 */
    @Test void s2CostAndMotionPrecedeTargetCalculation() throws Exception {
        String session = source("ClientSession.java");
        String battle = method(session, "void nativeAreaBattleSlowBody(");
        int battleMp = battle.indexOf("this.playerMp = NativeSkillConsumption.boundMp(this, this.playerMp - var24);");
        int battleAction = battle.indexOf("\"S_ACTION Mass Slow caster=\"");
        int battleTargets = battle.indexOf("NativeSourceAreaCasterTargets.slowIterator(var12)");
        int battleNpcTargets = battle.indexOf("NativeSourceSlowFailureMetadata.iterator(var16, var12)");
        assertTrue(battleMp > 0 && battleAction > battleMp && battleTargets > battleAction && battleNpcTargets > battleTargets,
                "battle order mp=" + battleMp + " action=" + battleAction + " targets=" + battleTargets);
        assertTrue(battle.contains("\"S_MANA_POINT Mass Slow=\""));

        String world = method(session, "void nativeAreaWorldSlowBody(");
        int worldMp = world.indexOf("this.playerMp = NativeSkillConsumption.boundMp(this, this.playerMp - var28);");
        int worldAction = world.indexOf("\"S_ACTION classic-world Mass Slow caster=\"");
        int worldTargets = world.indexOf("NativeSourceMassSlowTargets.calculationIterator(var17)");
        assertTrue(worldMp > 0 && worldAction > worldMp && worldTargets > worldAction,
                "world order mp=" + worldMp + " action=" + worldAction + " targets=" + worldTargets);
    }

    /**
     * S3：103 ranged=0。寵物在 1～3 格時 LiTo 准入（目錄射程 3）→ 扣 MP、播動作，命中判定看 ranged=0 → 不生效、
     * 送失敗訊息；與 1-伺服器（射程外清單為空，TYPE_NORMAL 仍扣費並送 sysmsg 失敗訊息）相同。
     */
    @Test void s3DarkBlindPetDistanceStaysAlignedWithSource() throws Exception {
        var row = new NativeSkillTargetMetadata.Row("buff", 3, 0, 0, false, 4, 0, 64);
        assertTrue(SourceSkillTargeting.inRange(row, 0, true));
        for (int distance = 1; distance <= 3; distance++)
            assertFalse(SourceSkillTargeting.inRange(row, distance, true), "distance " + distance);
        assertEquals(3, BattleSpellCatalog.blindSpell(103).maximumRange());

        String blind = source("NativeBlindSceneAction.java");
        int companion = blind.indexOf("static void companion(");
        int gate = blind.indexOf("distance>spell.maximumRange()", companion);
        int cost = blind.indexOf("CombatAccess.set(pc,\"playerMp\",mp)", companion);
        int selected = blind.indexOf("if(selected(c,skill))", companion);
        int failure = blind.indexOf("if(!applied)send(pc,Protocol.messageCode(spell.failureMessage())", companion);
        assertTrue(companion > 0 && gate > companion && cost > gate && selected > cost && failure > selected,
                "companion order gate=" + gate + " cost=" + cost + " selected=" + selected + " failure=" + failure);
        assertTrue(blind.contains("row.ranged()>=0&&Math.max(Math.abs(x-p[0]),Math.abs(y-p[1]))>row.ranged()"));
    }
}
