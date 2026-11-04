package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** Locks native skill108 and its ordinary-WORLD exceptional resource order. */
final class NativeWorldFinalBurnSourceAuditTest {
    @BeforeAll
    static void assumeLocalResourcesPresent() {
        LocalResources.assumeLineageSources();
    }

    private static final Path RETAINED = Path.of("../1-伺服器");

    @Test
    void retainedSourcePinsCurrentMpDamageAndHpFirstAdmission() throws Exception {
        String database = Files.readString(
                RETAINED.resolve("db/380_db.sql"), StandardCharsets.UTF_8);
        assertTrue(database.contains(
                "INSERT INTO `skills` VALUES (108, 0, '會心一擊', 14, 3, 1, 1, 0, 0, 0, 0, 'attack', 3, 0, 0, 0, 0, 0, 0, 64, 0, 1, 0, 0, 8, '$2515', 18, 2952, 0, 0, 0, 0);"));
        assertTrue(database.contains(
                "INSERT INTO `skills_item` VALUES (108, '會心一擊', NULL, NULL);"));

        String magic = Files.readString(RETAINED.resolve(
                "src/com/lineage/server/model/L1MagicPc.java"),
                StandardCharsets.UTF_8);
        assertTrue(magic.contains("if (skillId == FINAL_BURN)"));
        assertTrue(magic.contains("dmg = _pc.getCurrentMp();"));
        assertTrue(magic.contains("dmg -= _targetPc.getDamageReductionByArmor();"));
        assertTrue(magic.contains("damage = calcMrDefense(damage);"));

        String skillUse = Files.readString(RETAINED.resolve(
                "src/com/lineage/server/model/skill/L1SkillUse.java"),
                StandardCharsets.UTF_8);
        assertTrue(skillUse.contains("currentHp < this._hpConsume + 1"));
        assertTrue(skillUse.contains("this._player.setCurrentHp(1);"));
        assertTrue(skillUse.contains("this._player.setCurrentMp(0);"));
    }

    @Test
    void worldRouteAndExceptionalCommitRemainConnected() throws Exception {
        String session = Files.readString(Path.of(
                "src/main/java/local/lito381/server/ClientSession.java"),
                StandardCharsets.UTF_8);
        int worldStart = session.indexOf("if (phase == Phase.WORLD) {");
        int dispatch = session.indexOf(
                "BattleSpellCatalog.AttackSpell worldAttack", worldStart);
        int unsupported = session.indexOf(
                "rejected unsupported classic-world skill", dispatch);
        String route = session.substring(dispatch, unsupported);
        assertTrue(route.contains(
                "BattleSpellCatalog.attackSpell(selector.skillId())"));
        assertTrue(route.contains(
                "useClassicWorldAttackSpell(payload, worldAttack)"));

        int methodStart = session.indexOf(
                "private void useClassicWorldAttackSpell(");
        int methodEnd = session.indexOf(
                "private void publishClassicWorldAttackSpellPacket(", methodStart);
        String method = session.substring(methodStart, methodEnd);
        int stateGate = method.indexOf("if (castNow < readyAt");
        int hpGate = method.indexOf(
                "if (hpCost > 0 && playerHp <= hpCost)", stateGate);
        int mpGate = method.indexOf("if (playerMp < mpCost)", hpGate);
        assertTrue(stateGate >= 0 && hpGate > stateGate && mpGate > hpGate);
        assertFalse(method.substring(stateGate, hpGate).contains("playerMp < mpCost"));
        assertTrue(method.contains(
                "S_MESSAGE_CODE classic-world-attack-spell-insufficient-hp=279"));
        assertTrue(method.contains("sourceDamage = playerMp;"));
        assertTrue(method.contains("playerHp = 1;"));
        assertTrue(method.contains("playerMp = 0;"));
        assertTrue(method.contains("BattleFinalBurn.resolve("));
        assertTrue(method.contains(
                "playerTarget.timedStatus.consumeCounterMagic(castNow)"));
        assertTrue(method.contains("hub.damageClassicWorldMonster("));
        assertTrue(method.contains(
                "publishClassicWorldAttackSpellResources("));
    }
}
