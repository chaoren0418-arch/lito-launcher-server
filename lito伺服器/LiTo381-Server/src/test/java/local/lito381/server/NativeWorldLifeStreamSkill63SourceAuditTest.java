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
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** Exact native/source/server contract for skill63 Life Stream. */
final class NativeWorldLifeStreamSkill63SourceAuditTest {
    @BeforeAll
    static void assumeLocalResourcesPresent() {
        LocalResources.assumeLineageSources();
        LocalResources.assumeIdaClient(LocalResources.IDA_DISASSEMBLY_ABSOLUTE);
        LocalResources.assumeExisting("LiTo.unpacked", LocalResources.LITO_UNPACKED);
    }

    private static final Path RETAINED = Path.of("..", "1-伺服器")
            .toAbsolutePath().normalize();
    private static final Path UNPACKED = Path.of(
            "D:/lineage開發/LiTo381-開發/runtime/LiTo.unpacked-oep-current.exe");
    private static final Path ASSEMBLY = Path.of(
            "D:/lineage開發/LiTo381-開發/ida-client-oep-v2/full-disassembly.asm");

    @Test
    void selectedAndSecondaryRowsPinOnlyReuseDivergence() throws Exception {
        String selected = read(RETAINED.resolve("db/380_db.sql"));
        String secondary = read(RETAINED.resolve(
                "其他src參考用/0507備份/yiwei380/db/380yiwei_db.sql"));
        assertEquals("INSERT INTO `skills` VALUES (63, 0, '治癒能量風暴', 8, 6,"
                + " 20, 0, 40318, 1, 10, 320, 'buff', 32, 0, 0, 0, 0, 0,"
                + " 0, 2, 0, -1, 3, 0, 64, '$1988', 19, 2231, 0, 0, 0,"
                + " 0);", skillRow(selected, 63));
        assertEquals("INSERT INTO `skills` VALUES (63, '治癒能量風暴', 8, 6,"
                + " 20, 0, 40318, 1, 0, 320, 'buff', 32, 0, 0, 0, 0, 0, 0,"
                + " 2, 0, -1, 3, 0, 64, '$1988', 19, 2231, 0, 0, 0, 0);",
                skillRow(secondary, 63));
        String item = "INSERT INTO `skills_item` VALUES"
                + " (63, '治癒能量風暴', NULL, NULL);";
        assertTrue(selected.contains(item));
        assertTrue(secondary.contains(item));
    }

    @Test
    void nativeCase62EmitsExactCcchhAndTenMillisecondClock() throws Exception {
        String assembly = read(ASSEMBLY).replace("\r\n", "\n");
        String route = between(assembly, "loc_4CBA2C:", "loc_4CBA92:");
        assertTrue(route.contains("jumptable 004CB66E cases 57,62"));
        assertTrue(route.contains("call    sub_4C9380"));
        String producer = between(assembly,
                "sub_4C9270 proc near", "sub_4C9270 endp");
        for (String anchor : new String[] {
                "push    offset aCcchh", "push    14h",
                "call    sub_4C6AE0"
        }) assertTrue(producer.contains(anchor), () -> "missing producer " + anchor);
        assertArrayEquals("cccdhh\0".getBytes(StandardCharsets.US_ASCII),
                peBytes(UNPACKED, 0x00636028L, 7));
        assertEquals(10, Short.toUnsignedInt(ByteBuffer.wrap(
                peBytes(UNPACKED, 0x00635774L + 62L * 2L, 2))
                .order(ByteOrder.LITTLE_ENDIAN).getShort()));
        byte[] request = new Protocol.Writer().c(Protocol.C_USE_SPELL)
                .c(7).c(6).h(0x8220).h(0x7E53).bytes();
        assertArrayEquals(new byte[] {
                0x14, 0x07, 0x06, 0x20, (byte) 0x82, 0x53, 0x7E
        }, request);
        Protocol.GroundSpellRequest parsed = Protocol.groundSpellRequest(request);
        assertEquals(63, parsed.skillId());
        assertEquals(0x8220, parsed.x());
        assertEquals(0x7E53, parsed.y());
    }

    @Test
    void retainedHprAndBothServerPhasesPreserveAtomicCommitOrder()
            throws Exception {
        BattleSpellCatalog.GroundSpell spell =
                BattleSpellCatalog.groundSpell(BattleSpellCatalog.LIFE_STREAM);
        assertEquals(63, spell.skillId());
        assertEquals(7, spell.row());
        assertEquals(6, spell.column());
        assertEquals(20, spell.mpCost());
        assertEquals(40318, spell.itemConsumeId());
        assertEquals(1, spell.itemConsumeCount());
        assertEquals(10, spell.reuseDelayMillis());
        assertEquals(320, spell.durationSeconds());
        assertEquals(-1, spell.maximumRange());
        assertEquals(3, spell.effectRadius());
        assertEquals(19, spell.actionId());
        assertEquals(2231, spell.effectGfx());

        String retainedUse = read(RETAINED.resolve(
                "src/com/lineage/server/model/skill/L1SkillUse.java"));
        assertTrue(retainedUse.contains("case LIFE_STREAM:"));
        assertTrue(retainedUse.contains("L1SpawnUtil.spawnEffect(81169,"));
        String regen = read(RETAINED.resolve(
                "src/com/lineage/server/timecontroller/pc/PcRegenExecutor.java"));
        for (String anchor : new String[] {
                "effect.getNpcId() == 81169",
                "effect.getLocation().getTileLineDistance(pc.getLocation()) < 4",
                "hpDelta += 3", "if (hpVal < 0 && inLifeStream)"
        }) assertTrue(regen.contains(anchor), () -> "missing retained regen " + anchor);

        String session = read(Path.of(
                "src/main/java/local/lito381/server/ClientSession.java"));
        String battle = between(session, "private void useGroundSpell(",
                "private void useClassicWorldGroundSpell(");
        String world = between(session,
                "private void useClassicWorldGroundSpell(",
                "private boolean admitClassicWorldSpellFamily(");
        assertGroundOrder(battle, "hub.placeGroundSpell(", "spell-");
        assertGroundOrder(world, "hub.placeClassicWorldGroundSpell(",
                "classic-world-spell-");
        for (String method : new String[] {battle, world}) {
            assertTrue(method.contains("nextInventory.removeCatalog("));
            assertTrue(method.contains("spellReadyNanos.put(spell.skillId()"));
            assertFalse(method.contains("commitSpellReady("));
            assertTrue(method.contains("Protocol.effectLocation("));
        }
        assertTrue(session.contains("hub.inClassicWorldLifeStream("));
        assertTrue(session.contains("hub.inLifeStream("));
    }

    private static void assertGroundOrder(
            String method, String placement, String cleanupPrefix) {
        int place = method.indexOf(placement);
        int inventory = method.indexOf("inventory = nextInventory", place);
        int mp = method.indexOf("playerMp -= mpCost", inventory);
        int reuse = method.indexOf("spellReadyNanos.put(spell.skillId()", mp);
        int meditation = method.indexOf(
                "cancelMeditation(\"" + cleanupPrefix + "\"", reuse);
        int barrier = method.indexOf(
                "cancelAbsoluteBarrier(\"" + cleanupPrefix + "\"", meditation);
        int reveal = method.indexOf(
                "revealInvisibility(\"" + cleanupPrefix + "\"", barrier);
        int publish = method.indexOf("Protocol.effectLocation(", reveal);
        assertTrue(place >= 0 && inventory > place && mp > inventory
                && reuse > mp && meditation > reuse && barrier > meditation
                && reveal > barrier && publish > reveal);
    }

    private static String skillRow(String sql, int skillId) {
        String prefix = "INSERT INTO `skills` VALUES (" + skillId + ",";
        return sql.lines().filter(line -> line.startsWith(prefix))
                .findFirst().orElseThrow();
    }

    private static String between(String source, String start, String end) {
        int from = source.indexOf(start);
        int to = source.indexOf(end, from + start.length());
        assertTrue(from >= 0 && to > from, start + " -> " + end);
        return source.substring(from, to);
    }

    private static byte[] peBytes(Path path, long va, int length)
            throws Exception {
        byte[] bytes = Files.readAllBytes(path);
        ByteBuffer image = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);
        int pe = image.getInt(0x3C);
        int sections = Short.toUnsignedInt(image.getShort(pe + 6));
        int optional = pe + 24;
        int optionalSize = Short.toUnsignedInt(image.getShort(pe + 20));
        long rva = va - Integer.toUnsignedLong(image.getInt(optional + 28));
        int table = optional + optionalSize;
        for (int index = 0; index < sections; index++) {
            int section = table + index * 40;
            long virtualSize = Integer.toUnsignedLong(image.getInt(section + 8));
            long sectionRva = Integer.toUnsignedLong(image.getInt(section + 12));
            long rawSize = Integer.toUnsignedLong(image.getInt(section + 16));
            long rawOffset = Integer.toUnsignedLong(image.getInt(section + 20));
            if (rva >= sectionRva && rva + length <= sectionRva
                    + Math.max(virtualSize, rawSize)) {
                int offset = Math.toIntExact(rawOffset + rva - sectionRva);
                return java.util.Arrays.copyOfRange(bytes, offset, offset + length);
            }
        }
        throw new IllegalArgumentException("unmapped PE VA " + Long.toHexString(va));
    }

    private static String read(Path path) throws Exception {
        return Files.readString(path, StandardCharsets.UTF_8);
    }
}
