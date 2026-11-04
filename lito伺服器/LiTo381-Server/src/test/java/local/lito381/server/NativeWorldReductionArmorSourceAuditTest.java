package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** Locks native skill88 and its ordinary-WORLD defensive damage lifecycle. */
final class NativeWorldReductionArmorSourceAuditTest {
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
                "INSERT INTO `skills` VALUES (88, 0, '增幅防禦', 11, 7, 5, 50, 0, 0, 3000, 192, 'none', 0, 0, 0, 0, 0, 0, 0, 2, 0, 0, 0, 0, 128, '', 19, 4824, 0, 0, 0, 0);"));
        assertTrue(database.contains(
                "INSERT INTO `skills_相消免疫技能` VALUES (88, '減緩盔甲');"));
        assertTrue(database.contains(
                "INSERT INTO `skills_item` VALUES (88, '增幅防禦', NULL, NULL);"));

        String assembly = Files.readString(Path.of(
                "../LiTo381-開發/ida-client-oep-v2/full-disassembly.asm"),
                StandardCharsets.UTF_8);
        int producerStart = assembly.indexOf("sub_4CB5F0 proc near");
        int producerEnd = assembly.indexOf("sub_4CB5F0 endp", producerStart);
        String producer = assembly.substring(producerStart, producerEnd);
        int routeStart = producer.indexOf("def_4CB66E:");
        int routeEnd = producer.indexOf("loc_4CBC76:", routeStart);
        String route = producer.substring(routeStart, routeEnd);
        assertTrue(route.contains("jumptable 004CB66E default case, cases 23,80,82-85,87,88"));
        assertTrue(route.contains("and     edx, 7"));
        assertTrue(route.contains("sar     esi, 3"));
        assertTrue(route.contains("push    14h"));
        assertTrue(route.contains("push    offset off_5028CC"));
        assertTrue(assembly.contains("off_5028CC dd offset byte_636363"));

        String attackPc = Files.readString(RETAINED.resolve(
                "model/L1AttackPc.java"), StandardCharsets.UTF_8);
        assertTrue(attackPc.contains("hasSkillEffect(REDUCTION_ARMOR)"));
        assertTrue(attackPc.contains("Math.max(_targetPc.getLevel(), 50)"));
        assertTrue(attackPc.contains("dmg -= (targetPcLvl - 50) / 5 + 10"));

        for (String relative : new String[] {
                "model/L1AttackNpc.java", "model/L1MagicPc.java",
                "model/L1MagicNpc.java"}) {
            String source = Files.readString(RETAINED.resolve(relative),
                    StandardCharsets.UTF_8);
            assertTrue(source.contains("hasSkillEffect(REDUCTION_ARMOR)"));
            assertTrue(source.contains("dmg -= (targetPcLvl - 50) / 5 + 1"));
        }
        String skillUse = Files.readString(RETAINED.resolve(
                "model/skill/L1SkillUse.java"), StandardCharsets.UTF_8);
        assertTrue(skillUse.contains(
                "REDUCTION_ARMOR, BOUNCE_ATTACK, SOLID_CARRIAGE"));
        assertTrue(skillUse.contains("breakInterruptedCastEffects();"));
        assertTrue(skillUse.contains("breakInvisibilityOnCast();"));
    }

    @Test
    void worldDispatcherActivationDamageAndExpiryStayConnected()
            throws Exception {
        String session = Files.readString(Path.of(
                "src/main/java/local/lito381/server/ClientSession.java"),
                StandardCharsets.UTF_8);
        int worldStart = session.indexOf("if (phase == Phase.WORLD) {");
        int dispatch = session.indexOf(
                "BattleSpellCatalog.ReductionArmorSpell worldReductionArmor",
                worldStart);
        int unsupported = session.indexOf(
                "rejected unsupported classic-world skill", dispatch);
        assertTrue(dispatch > worldStart && unsupported > dispatch);
        assertTrue(session.substring(dispatch, unsupported).contains(
                "useReductionArmorSpell(payload, worldReductionArmor)"));

        int methodStart = session.indexOf("private void useReductionArmorSpell(");
        int methodEnd = session.indexOf(
                "private void useBounceAttackSpell(", methodStart);
        String method = session.substring(methodStart, methodEnd);
        assertTrue(method.contains("Protocol.selectorOnlySpellRequest(payload)"));
        assertTrue(method.contains("admitClassicWorldSpellFamily("));
        assertTrue(method.contains("abilityStatus.activateReductionArmor("));
        assertTrue(method.contains("playerHp -= spell.hpCost()"));
        assertTrue(method.contains("playerMp -= mpCost"));
        assertTrue(method.contains("cancelAbsoluteBarrier("));
        assertTrue(method.contains("revealInvisibility("));
        assertTrue(method.contains("classicWorldMembers(originMapId)"));
        assertTrue(method.contains("member.classicWorldInstanceId != originInstanceId"));
        assertTrue(method.contains("BattleSpellCatalog.isInNativeScreen("));

        int damageStart = session.indexOf(
                "private int combatDamageAfterCooking(", methodEnd);
        String damage = session.substring(damageStart,
                session.indexOf("private int combatPhysicalDamageAfterArmorAndCooking(",
                        damageStart));
        assertTrue(damage.contains("abilityStatus.reductionArmor(now)"));
        assertTrue(damage.contains(
                "applied, battleLevel(), playerPhysicalAttacker"));
        assertTrue(session.contains(
                "\"classic-world-player-physical\", true,"));
        assertTrue(session.contains(
                "&& (inBattle || inClassicWorld)) {\n            System.out.println(\"[LiTo] native Reduction Armor expired"));
    }
}
