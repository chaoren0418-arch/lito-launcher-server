package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** Locks native skill55 and its ordinary-WORLD combat-stat lifecycle. */
final class NativeWorldBerserkersSourceAuditTest {
    @BeforeAll
    static void assumeLocalResourcesPresent() {
        LocalResources.assumeLineageSources(
                Path.of("../1-伺服器/db/380_db.sql"),
                Path.of("../1-伺服器/src/com/lineage/server/clientpackets/C_UseSkill.java"),
                Path.of("../1-伺服器/src/com/lineage/server/model/Instance/L1PcBuffAbilityCalc.java"),
                Path.of("../1-伺服器/src/com/lineage/server/model/skill/L1SkillStop.java"),
                Path.of("../1-伺服器/src/com/lineage/server/model/skill/L1SkillUse.java"));
        LocalResources.assumeIdaClient(
                Path.of("../LiTo381-開發/ida-client-oep-v2/full-disassembly.asm"));
    }

    private static final Path RETAINED = Path.of(
            "../1-伺服器/src/com/lineage/server");

    @Test
    void nativeProducerDatabaseAndRetainedCombatRulesStayExact()
            throws Exception {
        String database = LocalResources.readString(
                Path.of("../1-伺服器/db/380_db.sql"),
                StandardCharsets.UTF_8);
        assertTrue(database.contains(
                "INSERT INTO `skills` VALUES (55, 0, '狂暴術', 7, 6, 40, 0, 0, 0, 10, 320, 'buff', 12, 0, 0, 0, 0, 0, 0, 2, 0, -1, 0, 0, 64, '$1986', 19, 3943, 0, 0, 0, 280);"));
        assertTrue(database.contains(
                "INSERT INTO `skills_buff_ability` VALUES (55, '狂暴術', 'AC', 10);"));
        assertTrue(database.contains(
                "INSERT INTO `skills_buff_ability` VALUES (55, '狂暴術', 'DMG', 5);"));
        assertTrue(database.contains(
                "INSERT INTO `skills_buff_ability` VALUES (55, '狂暴術', 'HIT', 2);"));

        String assembly = LocalResources.readString(Path.of(
                "../LiTo381-開發/ida-client-oep-v2/full-disassembly.asm"),
                StandardCharsets.UTF_8);
        int routeStart = assembly.indexOf("loc_4CB71D:");
        int routeEnd = assembly.indexOf("loc_4CB741:", routeStart);
        String route = assembly.substring(routeStart, routeEnd);
        assertTrue(route.contains("mov     bl, byte ptr [ebp+arg_4]"));
        assertTrue(route.contains("call    sub_4C9880"));
        int producerStart = assembly.indexOf("loc_4C99DB:");
        int producerEnd = assembly.indexOf("loc_4C9A0E:", producerStart);
        String producer = assembly.substring(producerStart, producerEnd);
        assertTrue(producer.contains("push    eax"));
        assertTrue(producer.contains("push    ecx"));
        assertTrue(producer.contains("push    esi"));
        assertTrue(producer.contains("push    14h"));
        assertTrue(producer.contains("push    offset aCccd"));

        String clientUse = LocalResources.readString(RETAINED.resolve(
                "clientpackets/C_UseSkill.java"), StandardCharsets.UTF_8);
        assertTrue(clientUse.contains("case 55:// 狂暴術"));
        assertTrue(clientUse.contains("targetId = readD();"));

        String skillUse = LocalResources.readString(RETAINED.resolve(
                "model/skill/L1SkillUse.java"), StandardCharsets.UTF_8);
        assertTrue(skillUse.contains("this._skillId == BERSERKERS"));
        assertTrue(skillUse.contains("pc.addAc(10)"));
        assertTrue(skillUse.contains("pc.addDmgup(5)"));
        assertTrue(skillUse.contains("pc.addHitup(2)"));
        assertTrue(skillUse.contains("pc.updateBuffStatus()"));

        String ability = LocalResources.readString(RETAINED.resolve(
                "model/Instance/L1PcBuffAbilityCalc.java"),
                StandardCharsets.UTF_8);
        assertTrue(ability.contains("SkillBuffAbilityLoader.get().getAbility"));
        assertTrue(ability.contains("AbilityType.HIT"));
        assertTrue(ability.contains("pc.addHitup(v * multiplier)"));
        assertTrue(ability.contains("AbilityType.DMG"));
        assertTrue(ability.contains("pc.addDmgup(v * multiplier)"));
        assertTrue(ability.contains("AbilityType.AC"));
        assertTrue(ability.contains("pc.addAc(v * multiplier)"));

        String skillStop = LocalResources.readString(RETAINED.resolve(
                "model/skill/L1SkillStop.java"), StandardCharsets.UTF_8);
        assertTrue(skillStop.contains("case BERSERKERS:"));
        assertTrue(skillStop.contains("cha.addAc(-10)"));
        assertTrue(skillStop.contains("cha.addDmgup(-5)"));
        assertTrue(skillStop.contains("cha.addHitup(-2)"));
    }

    @Test
    void worldDispatcherPublishesAndConsumesTheSameBerserkLease()
            throws Exception {
        String session = LocalResources.readString(Path.of(
                "src/main/java/local/lito381/server/ClientSession.java"),
                StandardCharsets.UTF_8);
        int dispatchStart = session.indexOf(
                "BattleSpellCatalog.CreateZombieSpell worldCreateZombie");
        int dispatchEnd = session.indexOf(
                "BattleSpellCatalog.AquaProtectSpell worldAquaProtect",
                dispatchStart);
        String dispatch = session.substring(dispatchStart, dispatchEnd);
        assertTrue(dispatch.contains(
                "BattleSpellCatalog.CombatBuffSpell worldCombatBuff"));
        assertTrue(dispatch.contains(
                "BattleSpellCatalog.combatBuffSpell(selector.skillId())"));
        assertTrue(dispatch.contains("admitClassicWorldSpellFamily("));
        assertTrue(dispatch.contains(
                "useCombatBuffSpell(payload, worldCombatBuff)"));

        int handlerStart = session.indexOf(
                "private void useCombatBuffSpell(");
        int handlerEnd = session.indexOf(
                "private void useDamageProtectionSpell(", handlerStart);
        String handler = session.substring(handlerStart, handlerEnd);
        assertTrue(handler.contains("originPhase == Phase.WORLD"));
        assertTrue(handler.contains("classicWorldMembers(originMapId)"));
        assertTrue(handler.contains("sameNonemptyClan(target)"));
        assertTrue(handler.contains("sameClassicParty(target)"));
        assertTrue(handler.contains("classicWorldInstanceId == originInstanceId"));
        assertTrue(handler.contains("positionX != originX"));
        assertTrue(handler.contains("target.positionX != targetX"));
        assertTrue(handler.contains("target.abilityStatus.activateBerserk("));
        assertTrue(handler.contains("cancelAbsoluteBarrier("));
        assertTrue(handler.contains("revealInvisibility("));
        assertTrue(handler.contains("BattleSpellCatalog.isInNativeScreen("));
        assertTrue(handler.contains("target.sendNativeBattleStatus("));

        int worldPhysicalStart = session.indexOf(
                "private void attackClassicWorld(");
        int worldPlayerStart = session.indexOf(
                "private void attackClassicWorldPlayer(", worldPhysicalStart);
        String worldMonsterPhysical = session.substring(
                worldPhysicalStart, worldPlayerStart);
        assertTrue(worldMonsterPhysical.contains(
                "BattleAbilityStatus.Berserk berserk"));
        assertTrue(worldMonsterPhysical.contains(
                "abilityStatus.berserk(physicalAttackNowNanos)"));
        assertTrue(worldMonsterPhysical.contains(
                "ranged ? 0 : berserk.meleeHit()"));
        assertTrue(worldMonsterPhysical.contains(
                "ranged ? 0 : berserk.meleeDamage()"));
        assertTrue(worldMonsterPhysical.contains("berserkers-hit="));
        assertTrue(worldMonsterPhysical.contains("berserkers-damage="));
        assertTrue(session.contains("abilityStatus.expireBerserk(nowNanos)"));
        assertTrue(session.contains("reason + \"-berserkers\""));
        assertTrue(session.contains(
                "Cancellation player cleared Berserkers target="));
    }
}
