package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** Locks native skill68 and every ordinary-WORLD player damage consumer. */
final class NativeWorldImmuneToHarmSourceAuditTest {
    @BeforeAll
    static void assumeLocalResourcesPresent() {
        LocalResources.assumeLineageSources(
                Path.of("../1-伺服器/db/380_db.sql"),
                Path.of("../1-伺服器/其他src參考用/0507備份/yiwei380/db/380yiwei_db.sql"));
        LocalResources.assumeIdaClient(
                Path.of("../LiTo381-開發/ida-client-oep-v2/full-disassembly.asm"));
        LocalResources.assumeFiles(
                Path.of("../LiTo381-開發/runtime/LiTo.unpacked-oep-current.exe"));
    }

    private static final Path RETAINED_ROOT = Path.of("../1-伺服器");
    private static final Path RETAINED = RETAINED_ROOT.resolve(
            "src/com/lineage/server");
    private static final Path RECOVERED = Path.of(
            "../LiTo381-開發/runtime/LiTo.unpacked-oep-current.exe");

    @Test
    void nativeProducerDatabaseAndRetainedDamageRulesStayExact()
            throws Exception {
        String selected = LocalResources.readString(
                RETAINED_ROOT.resolve("db/380_db.sql"), StandardCharsets.UTF_8);
        String secondary = LocalResources.readString(RETAINED_ROOT.resolve(
                "其他src參考用/0507備份/yiwei380/db/380yiwei_db.sql"),
                StandardCharsets.UTF_8);
        String selectedRow = "INSERT INTO `skills` VALUES (68, 0, '聖結界', 9, "
                + "3, 30, 0, 40318, 2, 1500, 32, 'buff', 4, 0, 0, 0, 0, 0, "
                + "0, 2, 0, -1, 0, 0, 8, '$1479', 19, 228, 0, 314, 315, 280);";
        String secondaryRow = "INSERT INTO `skills` VALUES (68, '聖結界', 9, 3, "
                + "30, 0, 40318, 2, 1500, 32, 'buff', 4, 0, 0, 0, 0, 0, 0, "
                + "2, 0, -1, 0, 0, 8, '$1479', 19, 228, 0, 314, 315, 280);";
        String itemRow = "INSERT INTO `skills_item` VALUES (68, '聖結界', NULL, NULL);";
        assertEquals(1, count(selected, selectedRow));
        assertEquals(1, count(secondary, secondaryRow));
        assertEquals(1, count(selected, itemRow));
        assertEquals(1, count(secondary, itemRow));

        String assembly = LocalResources.readString(Path.of(
                "../LiTo381-開發/ida-client-oep-v2/full-disassembly.asm"),
                StandardCharsets.UTF_8);
        int dispatchStart = assembly.indexOf("loc_4CB11C:");
        int dispatchEnd = assembly.indexOf("loc_4CB126:", dispatchStart);
        String dispatch = assembly.substring(dispatchStart, dispatchEnd);
        assertTrue(dispatch.contains("mov     ecx, esi"));
        assertTrue(dispatch.contains("xor     dl, dl"));
        assertTrue(dispatch.contains("jmp     sub_4C9C80"));
        int callbackStart = assembly.indexOf("loc_4C9D76:");
        int callbackEnd = assembly.indexOf(
                "sub_4C9DC0 proc near", callbackStart);
        String callback = assembly.substring(callbackStart, callbackEnd);
        assertTrue(callback.contains("mov     eax, [esi+4]"));
        assertTrue(callback.contains("push    eax"));
        assertTrue(callback.contains("mov     eax, dword_834880"));
        assertTrue(callback.contains("and     ecx, 7"));
        assertTrue(callback.contains("sar     eax, 3"));
        assertTrue(callback.contains("push    14h"));
        assertTrue(callback.contains("push    offset aCccd"));
        assertArrayEquals("cccd\0".getBytes(StandardCharsets.US_ASCII),
                peBytes(0x00502CE0L, 5));
        assertArrayEquals(new byte[] {0}, peBytes(0x00635A44L + 67, 1));
        assertEquals(1500, Short.toUnsignedInt(ByteBuffer.wrap(
                peBytes(0x00635774L + 67L * 2L, 2))
                .order(ByteOrder.LITTLE_ENDIAN).getShort()));

        String clientUse = retained("clientpackets/C_UseSkill.java");
        assertTrue(clientUse.contains("case 68:"));
        String skillUse = retained("model/skill/L1SkillUse.java");
        assertTrue(skillUse.contains("this._skillId != IMMUNE_TO_HARM"));
        assertTrue(skillUse.contains("case IMMUNE_TO_HARM"));
        assertRetainedDivisor("model/L1AttackPc.java",
                "dmg /= ConfigSkill.IMMUNE_TO_HARM");
        assertRetainedDivisor("model/L1AttackNpc.java",
                "dmg /= ConfigSkill.IMMUNE_TO_HARM");
        assertRetainedDivisor("model/L1MagicPc.java",
                "dmg = (int) (dmg / ConfigSkill.IMMUNE_TO_HARM)");
        assertRetainedDivisor("model/L1MagicNpc.java",
                "ConfigSkill.IMMUNE_TO_HARM");
        String config = retained("../config/ConfigSkill.java");
        assertTrue(config.contains(
                "IMMUNE_TO_HARM = _getDouble(p, \"IMMUNE_TO_HARM\", 2.0)"));
        assertTrue(config.contains("IMMUNE_TO_HARM = 2.0"));
    }

    @Test
    void worldDispatcherPublishesOneLeaseAndAllDamageLanesConsumeIt()
            throws Exception {
        String session = LocalResources.readString(Path.of(
                "src/main/java/local/lito381/server/ClientSession.java"),
                StandardCharsets.UTF_8);

        int dispatchStart = session.indexOf(
                "BattleSpellCatalog.CombatBuffSpell worldCombatBuff");
        int dispatchEnd = session.indexOf(
                "BattleSpellCatalog.AquaProtectSpell worldAquaProtect",
                dispatchStart);
        String dispatch = session.substring(dispatchStart, dispatchEnd);
        assertTrue(dispatch.contains(
                "BattleSpellCatalog.DamageProtectionSpell worldDamageProtection"));
        assertTrue(dispatch.contains(
                "BattleSpellCatalog.damageProtectionSpell(selector.skillId())"));
        assertTrue(dispatch.contains("admitClassicWorldSpellFamily("));
        assertTrue(dispatch.contains("\"damage-protection\""));
        assertTrue(dispatch.contains(
                "useDamageProtectionSpell(payload, worldDamageProtection)"));

        int handlerStart = session.indexOf(
                "private void useDamageProtectionSpell(");
        int handlerEnd = session.indexOf(
                "private void useMassTeleportSpell(", handlerStart);
        String handler = session.substring(handlerStart, handlerEnd);
        assertTrue(handler.contains("originPhase == Phase.WORLD"));
        assertTrue(handler.contains("classicWorldMembers(originMapId)"));
        assertTrue(handler.contains("target == this || sameNonemptyClan(target)"));
        assertTrue(handler.contains("classicWorldInstanceId == originInstanceId"));
        assertTrue(handler.contains("positionX != originX"));
        assertTrue(handler.contains("target.positionX != targetX"));
        assertTrue(handler.contains(
                "target.playerObjectId() != request.targetObjectId()"));
        assertTrue(handler.contains(
                "!(target == this || sameNonemptyClan(target))"));
        assertTrue(handler.contains(
                "BattleAnimationCatalog.get().supportsTimedAction("));
        assertTrue(handler.contains("BattleSpellCatalog.isInNativeScreen("));
        assertTrue(handler.contains("target.abilityStatus.activateImmuneToHarm("));
        assertTrue(handler.contains("cancelAbsoluteBarrier("));
        assertTrue(handler.contains("revealInvisibility("));
        assertTrue(handler.contains("phase=\" + originPhase"));

        int inventory = handler.indexOf("inventory = nextInventory;");
        int mp = handler.indexOf("playerMp -= mpCost;", inventory);
        int reuse = handler.indexOf("spellReadyNanos.put(spell.skillId()", mp);
        int lease = handler.indexOf(
                "target.abilityStatus.activateImmuneToHarm(", reuse);
        int meditation = handler.indexOf(
                "cancelMeditation(\"spell-\" + spell.skillId())", lease);
        int barrier = handler.indexOf(
                "cancelAbsoluteBarrier(\"spell-\" + spell.skillId())", meditation);
        int invisibility = handler.indexOf(
                "revealInvisibility(\"spell-\" + spell.skillId())", barrier);
        int publication = handler.indexOf("Protocol.objectAction(", invisibility);
        List<Integer> order = List.of(inventory, mp, reuse, lease, meditation,
                barrier, invisibility, publication);
        assertTrue(order.stream().allMatch(value -> value >= 0), order.toString());
        assertEquals(order.stream().sorted().toList(), order);
        assertFalse(handler.contains("commitSpellReady("));

        int reductionStart = session.indexOf(
                "private int combatDamageAfterCooking(",
                session.indexOf("private synchronized boolean cancelClassicWorldArmorBreak"));
        int reductionEnd = session.indexOf(
                "private int combatPhysicalDamageAfterArmorAndCooking(",
                reductionStart);
        String reduction = session.substring(reductionStart, reductionEnd);
        assertTrue(reduction.contains("abilityStatus.immuneToHarm(now)"));
        assertTrue(reduction.contains("protection.applyTo(applied)"));
        assertTrue(reduction.contains(
                "Immune to Harm damage reduction player="));

        String worldMonsterPhysical = between(session,
                "void serverClassicWorldMonsterAttack(",
                "private void commitClassicWorldMonsterCounterBarrier(");
        assertTrue(worldMonsterPhysical.contains(
                "combatPhysicalDamageAfterArmorAndCooking("));
        String worldMonsterMagic = between(session,
                "void serverClassicWorldMonsterMagicAttack(",
                "void serverClassicWorldMonsterPhysicalSkillAttack(");
        assertTrue(worldMonsterMagic.contains(
                "combatDamageAfterCookingWithoutReductionArmor("));

        String worldPlayerPhysical = between(session,
                "private void attackClassicWorldPlayer(",
                "private void publishClassicWorldPhysicalAttack(");
        assertTrue(worldPlayerPhysical.contains(
                "target.combatDamageAfterCooking("));
        assertTrue(worldPlayerPhysical.contains(
                "target.takeClassicWorldPlayerDamage(damage, this)"));
        String worldPlayerMagic = between(session,
                "private void useClassicWorldAttackSpell(",
                "private void publishClassicWorldSpellMonsterDamage(");
        assertTrue(worldPlayerMagic.contains(
                "playerTarget.combatDamageAfterCooking("));
        assertTrue(worldPlayerMagic.contains(
                "playerTarget.takeClassicWorldPlayerDamage("));

        assertTrue(session.contains(
                "abilityStatus.expireImmuneToHarm(nowNanos)\n"
                + "                && (inBattle || inClassicWorld)"));
        assertTrue(session.contains(
                "Cancellation player cleared Immune to Harm"));
        assertTrue(session.contains(
                "+ \" target=\" + target.playerObjectId()"));

        String acceptance = LocalResources.readString(Path.of(
                "tools/classic_world_immune_to_harm_selftest.py"),
                StandardCharsets.UTF_8);
        assertTrue(acceptance.contains(
                "request == b\"\\x14\\x08\\x03\\x03\\x00\\x00\\x00\""));
        assertTrue(acceptance.contains("protected_magic"));
        assertTrue(acceptance.contains("protected_physical"));
        assertTrue(acceptance.contains("unprotected_magic"));
        assertTrue(acceptance.contains("unprotected_physical"));
        assertTrue(acceptance.contains("CLAN_TEAM_ROOM = \"LegacyEmblem\""));
        assertTrue(acceptance.contains("encrypted_stream_aligned"));
        for (String name : List.of(
                "src/test/resources/server-config/root/server.immune-to-harm-native-battle-selftest.properties",
                "src/test/resources/server-config/root/server.world-immune-to-harm-selftest.properties")) {
            String config = LocalResources.readString(Path.of(name), StandardCharsets.UTF_8);
            assertTrue(config.contains("game.bind=127.0.0.2"), name);
            assertTrue(config.contains("port=32100"), name);
            assertTrue(config.contains("database.mode=memory"), name);
            assertTrue(config.contains("database.read.only=true"), name);
            assertTrue(config.contains("state.persistence.enabled=false"), name);
            assertTrue(config.contains("spell.material.fixture.enabled=true"), name);
        }
        String worldConfig = LocalResources.readString(Path.of(
                "src/test/resources/server-config/root/server.world-immune-to-harm-selftest.properties"),
                StandardCharsets.UTF_8);
        assertTrue(worldConfig.contains("world.cancellation.fixture.enabled=true"));
        assertTrue(worldConfig.contains("classic.world.monster.ai.enabled=false"));
    }

    private static void assertRetainedDivisor(String relative, String divisor)
            throws Exception {
        String source = retained(relative);
        assertTrue(source.contains("hasSkillEffect(IMMUNE_TO_HARM)"));
        assertTrue(source.contains(divisor));
    }

    private static String retained(String relative) throws Exception {
        return LocalResources.readString(RETAINED.resolve(relative).normalize(),
                StandardCharsets.UTF_8);
    }

    private static int count(String source, String value) {
        int result = 0;
        for (int offset = source.indexOf(value); offset >= 0;
                offset = source.indexOf(value, offset + value.length())) {
            result++;
        }
        return result;
    }

    private static byte[] peBytes(long virtualAddress, int length) throws Exception {
        byte[] data = LocalResources.readAllBytes(RECOVERED);
        ByteBuffer image = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN);
        int pe = image.getInt(0x3C);
        int sectionCount = Short.toUnsignedInt(image.getShort(pe + 6));
        int optionalSize = Short.toUnsignedInt(image.getShort(pe + 20));
        int optional = pe + 24;
        long imageBase = Integer.toUnsignedLong(image.getInt(optional + 28));
        long rva = virtualAddress - imageBase;
        int table = optional + optionalSize;
        for (int index = 0; index < sectionCount; index++) {
            int section = table + index * 40;
            long virtualSize = Integer.toUnsignedLong(image.getInt(section + 8));
            long sectionRva = Integer.toUnsignedLong(image.getInt(section + 12));
            long rawSize = Integer.toUnsignedLong(image.getInt(section + 16));
            long rawOffset = Integer.toUnsignedLong(image.getInt(section + 20));
            if (sectionRva <= rva
                    && rva + length <= sectionRva + Math.max(virtualSize, rawSize)) {
                int offset = Math.toIntExact(rawOffset + rva - sectionRva);
                byte[] result = new byte[length];
                System.arraycopy(data, offset, result, 0, length);
                return result;
            }
        }
        throw new IllegalArgumentException(
                "unmapped PE address " + Long.toHexString(virtualAddress));
    }

    private static String between(String source, String start, String end) {
        int begin = source.indexOf(start);
        int finish = source.indexOf(end, begin);
        assertTrue(begin >= 0, start);
        assertTrue(finish > begin, end);
        return source.substring(begin, finish);
    }
}
