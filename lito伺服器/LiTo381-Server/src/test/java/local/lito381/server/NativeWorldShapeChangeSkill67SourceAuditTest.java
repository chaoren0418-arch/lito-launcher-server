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

/** Strict current-source audit for native LiTo skill67 Shape Change. */
final class NativeWorldShapeChangeSkill67SourceAuditTest {
    @BeforeAll
    static void assumeLocalResourcesPresent() {
        LocalResources.assumeLineageSources();
        LocalResources.assumeIdaClient();
    }

    private static final Path RETAINED = Path.of("../1-伺服器");
    private static final Path RECOVERED = Path.of(
            "../LiTo381-開發/runtime/LiTo.unpacked-oep-current.exe");

    @Test
    void selectedAndSecondarySkillRowsStayExact() throws Exception {
        String selected = text(RETAINED.resolve("db/380_db.sql"));
        String secondary = text(RETAINED.resolve(
                "其他src參考用/0507備份/yiwei380/db/380yiwei_db.sql"));
        String selectedRow = "INSERT INTO `skills` VALUES (67, 0, '變形術', 9, 2, "
                + "80, 0, 40318, 1, 10, 7200, 'buff', 1, 0, 0, 0, 0, 0, 0, 2, "
                + "0, -1, 0, 0, 4, '$1478', 19, 230, 0, 0, 0, 280);";
        String secondaryRow = "INSERT INTO `skills` VALUES (67, '變形術', 9, 2, "
                + "80, 0, 40318, 1, 0, 7200, 'buff', 1, 0, 0, 0, 0, 0, 0, 2, "
                + "0, -1, 0, 0, 4, '$1478', 19, 230, 0, 0, 0, 280);";
        String itemRow = "INSERT INTO `skills_item` VALUES (67, '變形術', NULL, NULL);";
        String bossRow = "INSERT INTO `skills_負面boss免疫` VALUES (67, '變形術', 0);";
        assertEquals(1, count(selected, selectedRow));
        assertEquals(1, count(secondary, secondaryRow));
        for (String source : List.of(selected, secondary)) {
            assertEquals(1, count(source, itemRow));
            assertEquals(1, count(source, bossRow));
        }
    }

    @Test
    void selectedClientProducerFlagAndClockStayExact() throws Exception {
        String assembly = text(Path.of(
                "../LiTo381-開發/ida-client-oep-v2/full-disassembly.asm"));
        String route = between(assembly, "loc_4CB112:", "loc_4CB11C:");
        assertTrue(route.contains("cases 0,7,8,18,22,34,36,42,43,47,54,56,66"));
        assertTrue(route.contains("jmp     sub_4C9880"));
        String producer = between(
                assembly, "sub_4C9880 proc near", "sub_4C9880 endp");
        assertTrue(producer.contains("push    offset aCccd"));
        assertTrue(producer.contains("push    14h"));
        assertTrue(producer.contains("call    sub_4C6AE0"));

        assertArrayEquals("cccd\0".getBytes(StandardCharsets.US_ASCII),
                peBytes(0x00502CE0L, 5));
        assertArrayEquals(new byte[] {1}, peBytes(0x00635A44L + 66, 1));
        assertEquals(10, Short.toUnsignedInt(ByteBuffer.wrap(
                peBytes(0x00635774L + 66L * 2L, 2))
                .order(ByteOrder.LITTLE_ENDIAN).getShort()));
    }

    @Test
    void retainedMenuCatalogAndLifecycleStayExact() throws Exception {
        String skillUse = text(RETAINED.resolve(
                "src/com/lineage/server/model/skill/L1SkillUse.java"));
        assertTrue(skillUse.contains("pc.getInventory().checkEquipped(20281)"));
        assertTrue(skillUse.contains("new S_ShowPolyList(pc.getId())"));
        assertTrue(skillUse.contains("pc.setShapeChange(true)"));
        assertTrue(skillUse.contains("WandPolyTable.get().getRandomPolyId()"));
        String action = text(RETAINED.resolve(
                "src/com/lineage/server/model/L1ActionPc.java"));
        assertTrue(action.contains("if (_pc.isShapeChange())"));
        assertTrue(action.contains("L1PolyMorph.handleCommands(_pc, cmd)"));
        assertTrue(action.contains("_pc.setShapeChange(false)"));
        String polymorph = text(RETAINED.resolve(
                "src/com/lineage/server/model/L1PolyMorph.java"));
        assertTrue(polymorph.contains("weaponTakeoff"));
        assertTrue(polymorph.contains("pc.removeSkillEffect(L1SkillId.SHAPE_CHANGE)"));
        assertTrue(polymorph.contains("doPoly(pc, poly.getPolyId(), 7200"));

        assertEquals(104, BattlePolymorphCatalog.allForms().size());
        assertEquals(96, BattlePolymorphCatalog.standardMenuForms().size());
        assertEquals(21, BattlePolymorphCatalog.allForms().stream()
                .filter(form -> form.randomWeight() > 0).count());
        assertEquals(1_000_000, BattlePolymorphCatalog.allForms().stream()
                .mapToInt(BattlePolymorphCatalog.Form::randomWeight).sum());
        assertEquals(3, BattlePolymorphCatalog.standardMenuForms().stream()
                .filter(form -> !form.skillUsable()).count());
    }

    @Test
    void orderedCommitExactTokenAndWorldPublicationStayConnected()
            throws Exception {
        String session = text(Path.of(
                "src/main/java/local/lito381/server/ClientSession.java"));
        String handler = between(session,
                "private void usePolymorphSpell(", "private void useLightSpell(");
        int inventory = handler.indexOf("inventory = nextInventory;");
        int mp = handler.indexOf("playerMp -= mpCost;", inventory);
        int reuse = handler.indexOf(
                "spellReadyNanos.put(spell.skillId()", mp);
        int meditation = handler.indexOf(
                "cancelMeditation(\"spell-\" + spell.skillId())", reuse);
        int barrier = handler.indexOf(
                "cancelAbsoluteBarrier(\"spell-\" + spell.skillId())", meditation);
        int invisibility = handler.indexOf(
                "revealInvisibility(\"spell-\" + spell.skillId())", barrier);
        int publication = handler.indexOf("Protocol.objectAction(", invisibility);
        List<Integer> order = List.of(
                inventory, mp, reuse, meditation, barrier, invisibility, publication);
        assertTrue(order.stream().allMatch(value -> value >= 0), order.toString());
        assertEquals(order.stream().sorted().toList(), order);
        assertFalse(handler.contains("commitSpellReady("));
        assertTrue(handler.contains(
                "target.playerObjectId() == request.targetObjectId()"));
        assertTrue(handler.contains(
                "BattleAnimationCatalog.get().supportsTimedAction("));

        String callback = between(session,
                "private void handleShapeChangeAction(",
                "private void requestPlayerTrade(");
        assertTrue(callback.contains("pendingShapeChange != pending"));
        assertTrue(callback.contains("pendingShapeChange = null"));
        assertTrue(callback.contains("request.worldObjectId() == playerObjectId()"));
        assertTrue(session.contains("if (pendingShapeChange == shapeChange)"));
        assertTrue(session.contains(
                "expiredPolymorph != null && (inBattle || inClassicWorld)"));

        String scroll = between(session,
                "private void usePolymorphScroll(",
                "private void useCookingItem(");
        assertTrue(scroll.contains(
                "originPhase != Phase.BATTLE && originPhase != Phase.WORLD"));
        assertTrue(scroll.contains("classicWorldMapId == originMapId"));
        assertTrue(scroll.contains(
                "classicWorldInstanceId == originInstanceId"));
        assertTrue(scroll.indexOf("inventory.remove(item.objectId(), 1)")
                < scroll.indexOf("cancelAbsoluteBarrier(\"item-polymorph\")"));

        String wield = between(session,
                "private void broadcastWieldState(",
                "private void broadcastLightState(");
        assertTrue(wield.contains("int originInstanceId = classicWorldInstanceId"));
        assertTrue(wield.contains(
                "member.classicWorldInstanceId != originInstanceId"));
        assertTrue(wield.contains("BattleSpellCatalog.isInNativeScreen("));
    }

    private static byte[] peBytes(long virtualAddress, int length) throws Exception {
        byte[] data = Files.readAllBytes(RECOVERED);
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

    private static String text(Path path) throws Exception {
        return Files.readString(path, StandardCharsets.UTF_8);
    }

    private static int count(String source, String value) {
        int result = 0;
        for (int offset = source.indexOf(value); offset >= 0;
                offset = source.indexOf(value, offset + value.length())) {
            result++;
        }
        return result;
    }

    private static String between(String source, String start, String end) {
        int begin = source.indexOf(start);
        int finish = source.indexOf(end, begin + start.length());
        assertTrue(begin >= 0, start);
        assertTrue(finish > begin, end);
        return source.substring(begin, finish);
    }
}
