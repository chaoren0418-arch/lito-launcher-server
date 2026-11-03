package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

/** Locks selected skill32 production, retained resource rules and activity lifecycle. */
final class NativeWorldMeditationSourceAuditTest {
    private static final Path WORKSPACE = Path.of("..");

    @Test
    void selectedAndSecondaryRowsAndLiToProducerStayExact() throws Exception {
        String primary = read(WORKSPACE.resolve("1-伺服器/db/380_db.sql"));
        assertTrue(primary.contains(
                "INSERT INTO `skills` VALUES (32, 0, '冥想術', 4, 7, 5, 30, 0, 0, "
                + "10, 640, 'none', 0, 0, 0, 0, 0, 0, 0, 2, 0, 0, 0, 0, "
                + "128, '$1981', 19, 2173, 0, 0, 0, 0);"));
        assertTrue(primary.contains(
                "INSERT INTO `skills_item` VALUES (32, '冥想術', NULL, NULL);"));

        String secondary = read(WORKSPACE.resolve(
                "1-伺服器/其他src參考用/0507備份/yiwei380/db/380yiwei_db.sql"));
        assertTrue(secondary.contains(
                "INSERT INTO `skills` VALUES (32, '冥想術', 4, 7, 5, 30, 0, 0, "
                + "0, 640, 'none', 0, 0, 0, 0, 0, 0, 0, 2, 0, 0, 0, 0, "
                + "128, '$1981', 19, 2173, 0, 0, 0, 0);"));

        String retainedIds = read(WORKSPACE.resolve(
                "1-伺服器/src/com/lineage/server/model/skill/L1SkillId.java"));
        assertTrue(retainedIds.contains("public static final int MEDITATION = 32;"));
        String retainedClient = read(WORKSPACE.resolve(
                "1-伺服器/src/com/lineage/server/clientpackets/C_UseSkill.java"));
        assertTrue(retainedClient.contains("case 32:// 冥想術 1250"));

        String assembly = read(WORKSPACE.resolve(
                "LiTo381-開發/ida-client-oep-v2/full-disassembly.asm"));
        String dispatch = between(assembly, "loc_4CBBD4:", "loc_4CBBED:");
        assertTrue(dispatch.contains("cases 1,2,12,13,30,31"));
        assertTrue(dispatch.contains("and     ecx, 7"));
        assertTrue(dispatch.contains("sar     esi, 3"));
        assertTrue(dispatch.contains("push    14h"));
        assertTrue(dispatch.contains("push    offset off_5028CC"));
        assertTrue(dispatch.contains("call    sub_4C6AE0"));
    }

    @Test
    void retainedMprCostsAndOnlyCommittedActivitiesBreakMeditation() throws Exception {
        String retainedUse = read(WORKSPACE.resolve(
                "1-伺服器/src/com/lineage/server/model/skill/L1SkillUse.java"));
        assertTrue(retainedUse.contains("if ((this._player.getInt() > 14)"));
        assertTrue(retainedUse.contains("this._skillId > WEAK_ELEMENTAL"));
        assertTrue(retainedUse.contains("this._mpConsume = Math.max(this._mpConsume, 1)"));
        assertTrue(retainedUse.contains(
                "this._mpConsume -= this._player.getOriginalMagicConsumeReduction()"));
        assertTrue(retainedUse.contains("if (currentHp < this._hpConsume + 1)"));
        assertTrue(retainedUse.contains("_player.killSkillEffectTimer(MEDITATION);"));

        String regen = read(WORKSPACE.resolve(
                "1-伺服器/src/com/lineage/server/timecontroller/pc/PcRegenExecutor.java"));
        assertTrue(regen.contains("_mpSkills.put(MEDITATION, 5);"));
        assertTrue(regen.contains("if (tgpc.hasSkillEffect(skillid))"));
        assertTrue(regen.contains("baseMpr += integer;"));

        for (String relative : new String[] {
                "clientpackets/C_MoveChar.java", "clientpackets/C_Attack.java",
                "clientpackets/C_AttackBow.java", "utils/Teleportation.java"}) {
            String source = read(WORKSPACE.resolve(
                    "1-伺服器/src/com/lineage/server/" + relative));
            assertTrue(source.contains("killSkillEffectTimer(MEDITATION)"), relative);
        }
        String move = read(WORKSPACE.resolve(
                "1-伺服器/src/com/lineage/server/clientpackets/C_MoveChar.java"));
        assertTrue(move.contains("Apply movement side effects only after a step/portal actually commits."));
        String attack = read(WORKSPACE.resolve(
                "1-伺服器/src/com/lineage/server/clientpackets/C_Attack.java"));
        assertTrue(attack.indexOf("actionStarted = true;")
                < attack.indexOf("pc.killSkillEffectTimer(MEDITATION);"));
    }

    @Test
    void serverActivationMprCancellationAndWorldExpiryStayConnected() throws Exception {
        String session = read(Path.of(
                "src/main/java/local/lito381/server/ClientSession.java"));
        String handler = between(session, "private void useRegenSpell(",
                "private boolean sameNonemptyClan(");
        assertTrue(handler.contains("Protocol.selectorOnlySpellRequest(payload)"));
        assertTrue(handler.contains("spell.effectiveMpCost("));
        assertTrue(handler.contains("playerHp < spell.hpCost() + 1"));
        assertTrue(handler.contains("playerMp < mpCost"));
        assertTrue(handler.contains("target.timedStatus.activateMeditation("));
        assertTrue(handler.contains("playerHp -= spell.hpCost()"));
        assertTrue(handler.contains("playerMp -= mpCost"));
        assertTrue(session.contains("timedStatus.expireMeditation(nowNanos)\n"
                + "                && (inBattle || inClassicWorld)"));
        for (String reason : new String[] {"authoritative-move", "move", "attack",
                "classic-world-attack", "classic-world-player-attack", "teleport"}) {
            assertTrue(session.contains("cancelMeditation(\"" + reason), reason);
        }

        BattleSpellCatalog.RegenSpell spell =
                BattleSpellCatalog.regenSpell(BattleSpellCatalog.MEDITATION);
        assertEquals(5, spell.effectiveMpCost(12, 0));
        assertEquals(2, spell.effectiveMpCost(15, 0));
        assertEquals(0, spell.effectiveMpCost(15, 2));

        BattleTimedStatus status = new BattleTimedStatus();
        long start = TimeUnit.SECONDS.toNanos(10);
        status.activateMeditation(start, 640);
        assertTrue(status.meditationActive(start + 1));
        assertTrue(status.cancelMeditation());
        assertFalse(status.meditationActive(start + 2));
        status.activateMeditation(start, 640);
        assertTrue(status.expireMeditation(
                start + TimeUnit.SECONDS.toNanos(640)));
        assertEquals(6, BattleNaturalMpRegen.amount(
                new BattleNaturalMpRegen.Profile(0, 12, 12, 0),
                false, true, false, 0));
    }

    private static String read(Path path) throws Exception {
        return Files.readString(path, StandardCharsets.UTF_8);
    }

    private static String between(String source, String start, String end) {
        int begin = source.indexOf(start);
        int finish = source.indexOf(end, begin + start.length());
        assertTrue(begin >= 0 && finish > begin, start + " .. " + end);
        return source.substring(begin, finish);
    }
}
