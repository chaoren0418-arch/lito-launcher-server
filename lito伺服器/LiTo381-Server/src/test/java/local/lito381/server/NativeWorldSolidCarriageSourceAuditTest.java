package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** Locks native skill90 and its ordinary-WORLD equipment/ER lifecycle. */
final class NativeWorldSolidCarriageSourceAuditTest {
    @BeforeAll
    static void assumeLocalResourcesPresent() {
        LocalResources.assumeLineageSources();
        LocalResources.assumeIdaClient();
    }

    private static final Path RETAINED = Path.of(
            "../1-伺服器/src/com/lineage/server");

    @Test
    void nativeProducerDatabaseEquipmentAndErRulesStayExact() throws Exception {
        String database = Files.readString(
                Path.of("../1-伺服器/db/380_db.sql"),
                StandardCharsets.UTF_8);
        assertTrue(database.contains(
                "INSERT INTO `skills` VALUES (90, 0, '堅固防護', 12, 1, 10, 100, 0, 0, 3000, 64, 'none', 0, 0, 0, 0, 0, 0, 0, 2, 0, 0, 0, 0, 2, '', 19, 5831, 0, 0, 0, 0);"));
        assertTrue(database.contains(
                "INSERT INTO `skills_相消免疫技能` VALUES (90, '堅固防護');"));
        assertTrue(database.contains(
                "INSERT INTO `skills_item` VALUES (90, '堅固防護', NULL, NULL);"));

        String assembly = Files.readString(Path.of(
                "../LiTo381-開發/ida-client-oep-v2/full-disassembly.asm"),
                StandardCharsets.UTF_8);
        int routeStart = assembly.indexOf("loc_4CBAD6:");
        int routeEnd = assembly.indexOf("loc_4CBBDE:", routeStart);
        String route = assembly.substring(routeStart, routeEnd);
        assertTrue(route.contains("call    sub_447A00"));
        assertTrue(route.contains("push    1"));
        assertTrue(route.contains("push    0Bh"));
        assertTrue(assembly.contains("cmp     byte ptr [eax+6Ch], 19h"));
        assertTrue(assembly.contains("off_5028CC dd offset byte_636363"));

        String mode = Files.readString(RETAINED.resolve(
                "model/skill/skillmode/SOLID_CARRIAGE.java"),
                StandardCharsets.UTF_8);
        assertTrue(mode.contains("getTypeEquipped(2, 7)"));
        assertTrue(mode.contains("getTypeEquipped(2, 13)"));
        assertTrue(mode.contains("ConfigSkill.SOLID_CARRIAGE_MODE == 1"));

        String itemTable = Files.readString(RETAINED.resolve(
                "datatables/ItemTable.java"), StandardCharsets.UTF_8);
        assertTrue(itemTable.contains("_useTypes.put(\"shield\", 25)"));
        assertTrue(itemTable.contains("_useTypes.put(\"guarder\", 25)"));
        assertTrue(itemTable.contains("_armorTypes.put(\"shield\", 7)"));
        assertTrue(itemTable.contains("_armorTypes.put(\"guarder\", 13)"));

        String itemUse = Files.readString(RETAINED.resolve(
                "clientpackets/C_ItemUSe.java"), StandardCharsets.UTF_8);
        assertTrue(itemUse.contains("if (type == 7)"));
        assertTrue(itemUse.contains("removeSkillEffect(SOLID_CARRIAGE)"));
        assertTrue(itemUse.contains(
                "type == 13 && com.lineage.config.ConfigSkill.SOLID_CARRIAGE_MODE == 1"));

        String pc = Files.readString(RETAINED.resolve(
                "model/Instance/L1PcInstance.java"), StandardCharsets.UTF_8);
        assertTrue(pc.contains("this.hasSkillEffect(SOLID_CARRIAGE)"));
        assertTrue(pc.contains("er += 15"));
        String attackPc = Files.readString(RETAINED.resolve(
                "model/L1AttackPc.java"), StandardCharsets.UTF_8);
        assertTrue(attackPc.contains("if (_weaponType == 20)"));
        assertTrue(attackPc.contains("return calcErEvasion()"));
        String attackMode = Files.readString(RETAINED.resolve(
                "model/L1AttackMode.java"), StandardCharsets.UTF_8);
        assertTrue(attackMode.contains("final int er = _targetPc.getEr()"));
        assertTrue(attackMode.contains(
                "ThreadLocalRandom.current().nextInt(100) + 1"));
        assertTrue(attackMode.contains("return er < rnd"));

        String skillUse = Files.readString(RETAINED.resolve(
                "model/skill/L1SkillUse.java"), StandardCharsets.UTF_8);
        assertTrue(skillUse.contains(
                "REDUCTION_ARMOR, BOUNCE_ATTACK, SOLID_CARRIAGE"));
        assertTrue(skillUse.contains("breakInterruptedCastEffects();"));
        assertTrue(skillUse.contains("breakInvisibilityOnCast();"));
    }

    @Test
    void worldDispatcherCastErUnequipAndExpiryStayConnected() throws Exception {
        String session = Files.readString(Path.of(
                "src/main/java/local/lito381/server/ClientSession.java"),
                StandardCharsets.UTF_8);
        int worldStart = session.indexOf("if (phase == Phase.WORLD) {");
        int dispatch = session.indexOf(
                "BattleSpellCatalog.SolidCarriageSpell worldSolidCarriage",
                worldStart);
        int unsupported = session.indexOf(
                "rejected unsupported classic-world skill", dispatch);
        assertTrue(dispatch > worldStart && unsupported > dispatch);
        assertTrue(session.substring(dispatch, unsupported).contains(
                "useSolidCarriageSpell(payload, worldSolidCarriage)"));

        int methodStart = session.indexOf("private void useSolidCarriageSpell(");
        int methodEnd = session.indexOf(
                "private void useCounterBarrierSpell(", methodStart);
        String method = session.substring(methodStart, methodEnd);
        assertTrue(method.contains("Protocol.selectorOnlySpellRequest(payload)"));
        assertTrue(method.contains("admitClassicWorldSpellFamily("));
        assertTrue(method.contains("hasSolidCarriageEquipment()"));
        assertTrue(method.contains("abilityStatus.activateSolidCarriage("));
        assertTrue(method.contains("playerHp -= spell.hpCost()"));
        assertTrue(method.contains("playerMp -= mpCost"));
        assertTrue(method.contains("cancelAbsoluteBarrier("));
        assertTrue(method.contains("revealInvisibility("));
        assertTrue(method.contains("classicWorldMembers(originMapId)"));
        assertTrue(method.contains(
                "member.classicWorldInstanceId != originInstanceId"));
        assertTrue(method.contains("BattleSpellCatalog.isInNativeScreen("));

        assertTrue(session.contains(
                "abilityStatus.solidCarriage(nowNanos)\n"
                + "                                                .evasionRate()"));
        assertTrue(session.contains("rangedEvasionChecked = true"));
        assertTrue(session.contains(
                "targetEvasionRate = target.effectiveRangedEvasionRate(nowNanos)"));
        assertTrue(session.contains("target-er=\" + targetEvasionRate"));
        assertTrue(session.contains(
                "removeSolidCarriageUnlessEquipped(\"source-armor-change\")"));
        assertTrue(session.contains(
                "removeSolidCarriageUnlessEquipped(\"native-armor-change\")"));
        assertTrue(session.contains(
                "removeSolidCarriageUnlessEquipped(\"destroy-equipped-armor\")"));
        assertTrue(session.contains(
                "&& (inBattle || inClassicWorld)) {\n"
                + "            System.out.println(\"[LiTo] native Solid Carriage expired"));
    }
}
