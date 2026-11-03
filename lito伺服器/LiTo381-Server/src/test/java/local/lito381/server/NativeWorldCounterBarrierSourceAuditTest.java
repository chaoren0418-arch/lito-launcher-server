package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

/** Locks native skill91 and both ordinary-WORLD reflection paths. */
final class NativeWorldCounterBarrierSourceAuditTest {
    private static final Path RETAINED = Path.of(
            "../1-伺服器/src/com/lineage/server");

    @Test
    void nativeProducerDatabaseWeaponAndReflectionRulesStayExact()
            throws Exception {
        String database = Files.readString(
                Path.of("../1-伺服器/db/380_db.sql"),
                StandardCharsets.UTF_8);
        assertTrue(database.contains(
                "INSERT INTO `skills` VALUES (91, 0, '反擊屏障', 12, 2, 10, 100, 0, 0, 3000, 64, 'none', 0, 0, 0, 0, 20, 0, 0, 2, 0, 0, 0, 0, 4, '', 19, 5832, 0, 0, 0, 0);"));
        assertTrue(database.contains(
                "INSERT INTO `skills_相消免疫技能` VALUES (91, '反擊屏障');"));
        assertTrue(database.contains(
                "INSERT INTO `skills_item` VALUES (91, '反擊屏障', NULL, NULL);"));
        assertTrue(database.contains(
                "INSERT INTO `skills_負面boss免疫` VALUES (91, '反擊屏障', 0);"));
        assertTrue(database.contains(
                "INSERT INTO `skills_負面技能控制` VALUES (91, '反擊屏障', 55, 45, 35, 0, 0, 0, 0, 0, 1, 0.05, 0, 0);"));

        String assembly = Files.readString(Path.of(
                "../LiTo381-開發/ida-client-oep-v2/full-disassembly.asm"),
                StandardCharsets.UTF_8);
        int routeStart = assembly.indexOf("loc_4CBAF4:");
        int routeEnd = assembly.indexOf("loc_4CBBDE:", routeStart);
        String route = assembly.substring(routeStart, routeEnd);
        assertTrue(route.contains("cmp     byte ptr [eax+18h], 32h"));
        assertTrue(route.contains("push    2"));
        assertTrue(route.contains("push    0Bh"));
        assertTrue(assembly.contains("jumptable 004CB66E case 90"));
        assertTrue(assembly.contains("off_5028CC dd offset byte_636363"));

        String clientUse = Files.readString(RETAINED.resolve(
                "clientpackets/C_UseSkill.java"), StandardCharsets.UTF_8);
        assertTrue(clientUse.contains("case 91:// 反擊屏障"));
        assertTrue(clientUse.contains("getTypeEquipped(1, 3) >= 1"));
        String attackMode = Files.readString(RETAINED.resolve(
                "model/L1AttackMode.java"), StandardCharsets.UTF_8);
        assertTrue(attackMode.contains("COUNTER_BARRIER_TRIGGER_GFX = 10710"));
        assertTrue(attackMode.contains("weapon.getItem().getType() == 3"));
        assertTrue(attackMode.contains(
                "weapon.getItem().getDmgModifier()) << 1"));
        String pcFlow = Files.readString(RETAINED.resolve(
                "model/combat/PcPhysicalAttackFlow.java"),
                StandardCharsets.UTF_8);
        assertTrue(pcFlow.contains("targetPc.hasSkillEffect(COUNTER_BARRIER)"));
        assertTrue(pcFlow.contains("attack.commitCounterBarrier()"));
        String npcFlow = Files.readString(RETAINED.resolve(
                "model/combat/NpcPhysicalAttackFlow.java"),
                StandardCharsets.UTF_8);
        assertTrue(npcFlow.contains("targetPc.hasSkillEffect(COUNTER_BARRIER)"));
        assertTrue(npcFlow.contains("attack.isShortDistance()"));
        assertTrue(npcFlow.contains("attack.commitCounterBarrier()"));
        String npcAttack = Files.readString(RETAINED.resolve(
                "model/L1AttackNpc.java"), StandardCharsets.UTF_8);
        assertTrue(npcAttack.contains("isLongRange && (bowActId > 0)"));
        assertTrue(npcAttack.contains("ConfigSkill.COUNTER_BARRIER_DMG"));
    }

    @Test
    void worldCastPlayerAndMonsterReflectionStayConnected() throws Exception {
        String session = Files.readString(Path.of(
                "src/main/java/local/lito381/server/ClientSession.java"),
                StandardCharsets.UTF_8);
        int worldStart = session.indexOf("if (phase == Phase.WORLD) {");
        int dispatch = session.indexOf(
                "BattleSpellCatalog.CounterBarrierSpell worldCounterBarrier",
                worldStart);
        int unsupported = session.indexOf(
                "rejected unsupported classic-world skill", dispatch);
        assertTrue(dispatch > worldStart && unsupported > dispatch);
        assertTrue(session.substring(dispatch, unsupported).contains(
                "useCounterBarrierSpell(payload, worldCounterBarrier)"));

        int methodStart = session.indexOf("private void useCounterBarrierSpell(");
        int methodEnd = session.indexOf(
                "private boolean hasCounterBarrierWeapon(", methodStart);
        String method = session.substring(methodStart, methodEnd);
        assertTrue(method.contains("Protocol.selectorOnlySpellRequest(payload)"));
        assertTrue(method.contains("admitClassicWorldSpellFamily("));
        assertTrue(method.contains("hasCounterBarrierWeapon(spell)"));
        assertTrue(method.contains("abilityStatus.activateCounterBarrier("));
        assertTrue(method.contains("playerHp -= spell.hpCost()"));
        assertTrue(method.contains("playerMp -= mpCost"));
        assertTrue(method.contains("cancelAbsoluteBarrier("));
        assertTrue(method.contains("revealInvisibility("));
        assertTrue(method.contains("classicWorldMembers(originMapId)"));
        assertTrue(method.contains("member.classicWorldInstanceId != originInstanceId"));
        assertTrue(method.contains("local-only=true"));

        int monsterStart = session.indexOf(
                "void serverClassicWorldMonsterAttack(");
        int monsterEnd = session.indexOf(
                "private void applyClassicWorldMonsterPoison(", monsterStart);
        String monster = session.substring(monsterStart, monsterEnd);
        int decision = monster.indexOf("counterBarrierDecision(");
        int damageRoll = monster.indexOf("int rawDamage = 0;");
        assertTrue(decision > 0 && damageRoll > decision);
        assertTrue(monster.contains(
                "!(distance > 1\n                && actor.template().bowActionId() > 0)"));
        assertTrue(monster.contains("actor.template().magicResistance()"));
        assertTrue(monster.contains("actor.template().boss()"));
        assertTrue(monster.contains(
                "commitClassicWorldMonsterCounterBarrier(actor, counterBarrier)"));
        assertTrue(monster.contains("original-attack-suppressed=true"));
        assertTrue(monster.contains(
                "hub.damageClassicWorldMonster("));
        assertTrue(monster.contains("distributeClassicWorldMonsterRewards("));
        assertTrue(monster.contains("scheduleClassicWorldMonsterLifecycle("));

        int playerCommit = session.indexOf(
                "private void commitClassicWorldPlayerCounterBarrier(");
        int rewards = session.indexOf(
                "private void distributeClassicWorldMonsterRewards(", playerCommit);
        String player = session.substring(playerCommit, rewards);
        assertTrue(player.contains("gfx=\" + spell.triggerEffectGfx()"));
        assertTrue(player.contains("original-attack-suppressed=true"));
        assertTrue(player.contains("recursive-reflection-suppressed=true"));
        assertTrue(player.contains("takeClassicWorldPlayerDamage("));

        assertTrue(session.contains("int fixture = config.counterBarrierFixtureD100()"));
        assertTrue(session.contains("counterBarrierRoll(), nowNanos"));
        assertTrue(session.contains(
                "abilityStatus.expireCounterBarrier(nowNanos)\n"
                + "                && (inBattle || inClassicWorld)"));
    }
}
