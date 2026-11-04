package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** Locks native skill89 and its ordinary-WORLD status/weapon-damage lifecycle. */
final class NativeWorldBounceAttackSourceAuditTest {
    @BeforeAll
    static void assumeLocalResourcesPresent() {
        LocalResources.assumeLineageSources();
        LocalResources.assumeIdaClient();
    }

    private static final Path RETAINED = Path.of(
            "../1-伺服器/src/com/lineage/server");

    @Test
    void nativeProducerDatabaseAndRetainedRulesStayExact() throws Exception {
        String database = Files.readString(
                Path.of("../1-伺服器/db/380_db.sql"),
                StandardCharsets.UTF_8);
        assertTrue(database.contains(
                "INSERT INTO `skills` VALUES (89, 0, '尖刺盔甲', 12, 0, 5, 120, 0, 0, 5000, 64, 'none', 0, 5, 0, 0, 3, 0, 0, 2, 0, 0, 0, 0, 1, '', 19, 4648, 0, 0, 0, 0);"));
        assertTrue(database.contains(
                "INSERT INTO `skills_buff_ability` VALUES (89, '尖刺盔甲', 'AC', -2);"));
        assertTrue(database.contains(
                "INSERT INTO `skills_buff_ability` VALUES (89, '尖刺盔甲', 'HIT', 6);"));
        assertTrue(database.contains(
                "INSERT INTO `skills_item` VALUES (89, '尖刺盔甲', NULL, NULL);"));
        assertTrue(!database.contains(
                "INSERT INTO `skills_相消免疫技能` VALUES (89,"));

        String assembly = Files.readString(Path.of(
                "../LiTo381-開發/ida-client-oep-v2/full-disassembly.asm"),
                StandardCharsets.UTF_8);
        int producerStart = assembly.indexOf("sub_4CB5F0 proc near");
        int producerEnd = assembly.indexOf("sub_4CB5F0 endp", producerStart);
        String producer = assembly.substring(producerStart, producerEnd);
        int routeStart = producer.indexOf("def_4CB66E:");
        int routeEnd = producer.indexOf("loc_4CBC76:", routeStart);
        String route = producer.substring(routeStart, routeEnd);
        assertTrue(route.contains(
                "jumptable 004CB66E default case, cases 23,80,82-85,87,88"));
        assertTrue(route.contains("and     edx, 7"));
        assertTrue(route.contains("sar     esi, 3"));
        assertTrue(route.contains("push    14h"));
        assertTrue(route.contains("push    offset off_5028CC"));
        assertTrue(assembly.contains("off_5028CC dd offset byte_636363"));

        String physicalCommit = Files.readString(RETAINED.resolve(
                "model/combat/PhysicalHitCommitPlan.java"),
                StandardCharsets.UTF_8);
        assertTrue(physicalCommit.contains(
                "if (!targetPc.hasSkillEffect(BOUNCE_ATTACK))"));
        assertTrue(physicalCommit.contains("chance = 10;"));
        assertTrue(physicalCommit.contains(
                "ThreadLocalRandom.current().nextInt(100) + 1 <= chance"));
        assertTrue(physicalCommit.contains(
                "attacker.hasSkillEffect(SOUL_OF_FLAME)"));
        assertTrue(physicalCommit.contains(
                "attacker.getWeapon() != weapon || !weapon.isEquipped()"));
        assertTrue(physicalCommit.contains(
                "inventory.receiveDamage(weapon) != null"));
        assertTrue(physicalCommit.contains(
                "new S_ServerMessage(268, weapon.getLogName())"));

        String skillUse = Files.readString(RETAINED.resolve(
                "model/skill/L1SkillUse.java"), StandardCharsets.UTF_8);
        assertTrue(skillUse.contains(
                "REDUCTION_ARMOR, BOUNCE_ATTACK, SOLID_CARRIAGE"));
        assertTrue(skillUse.contains("breakInterruptedCastEffects();"));
        assertTrue(skillUse.contains("breakInvisibilityOnCast();"));
    }

    @Test
    void worldDispatcherActivationCombatCancellationAndExpiryStayConnected()
            throws Exception {
        String session = Files.readString(Path.of(
                "src/main/java/local/lito381/server/ClientSession.java"),
                StandardCharsets.UTF_8);
        int worldStart = session.indexOf("if (phase == Phase.WORLD) {");
        int dispatch = session.indexOf(
                "BattleSpellCatalog.BounceAttackSpell worldBounceAttack",
                worldStart);
        int unsupported = session.indexOf(
                "rejected unsupported classic-world skill", dispatch);
        assertTrue(dispatch > worldStart && unsupported > dispatch);
        assertTrue(session.substring(dispatch, unsupported).contains(
                "useBounceAttackSpell(payload, worldBounceAttack)"));

        int methodStart = session.indexOf("private void useBounceAttackSpell(");
        int methodEnd = session.indexOf(
                "private void useSolidCarriageSpell(", methodStart);
        String method = session.substring(methodStart, methodEnd);
        assertTrue(method.contains("Protocol.selectorOnlySpellRequest(payload)"));
        assertTrue(method.contains("admitClassicWorldSpellFamily("));
        assertTrue(method.contains("abilityStatus.activateBounceAttack("));
        assertTrue(method.contains("playerHp -= spell.hpCost()"));
        assertTrue(method.contains("playerMp -= mpCost"));
        assertTrue(method.contains("cancelAbsoluteBarrier("));
        assertTrue(method.contains("revealInvisibility("));
        assertTrue(method.contains("classicWorldMembers(originMapId)"));
        assertTrue(method.contains(
                "member.classicWorldInstanceId != originInstanceId"));
        assertTrue(method.contains("BattleSpellCatalog.isInNativeScreen("));
        assertTrue(method.contains("sendEncrypted(ownCharacterAttrDef()"));

        assertTrue(session.contains(
                "target.applyBounceAttackWeaponDamage(\n"
                + "                    this, weapon, sourceWeapon, sourceWeaponActive,"));
        assertTrue(session.contains("ranged ? 0 : bounce.meleeHit()"));
        assertTrue(session.contains(
                "if (damage > 0) {\n"
                + "            target.applyBounceAttackWeaponDamage("));
        assertTrue(session.contains("percentileRoll <= status.weaponDamageChance()")
                || Files.readString(Path.of(
                    "src/main/java/local/lito381/server/BattleBounceAttack.java"),
                    StandardCharsets.UTF_8).contains(
                        "percentileRoll <= status.weaponDamageChance()"));
        assertTrue(session.contains(
                "S_AC Cancellation bounce-attack current="));
        assertTrue(session.contains(
                "&& (inBattle || inClassicWorld)) {\n"
                + "            sendEncrypted(ownCharacterAttrDef()"));
    }
}
