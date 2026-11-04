package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** Cross-source proof for selected Illusionist skill205 Cube: Ignition. */
final class NativeCubeIgnitionSourceAuditTest {
    @BeforeAll
    static void assumeLocalResourcesPresent() {
        LocalResources.assumeLineageSources();
        LocalResources.assumeLitoBin();
        LocalResources.assumeIdaClient();
    }

    private static final Path RETAINED = Path.of("..", "1-伺服器")
            .toAbsolutePath().normalize();
    private static final Path PRIMARY = RETAINED.resolve("db/380_db.sql");
    private static final Path SECONDARY = RETAINED.resolve(
            "其他src參考用/0507備份/yiwei380/db/380yiwei_db.sql");
    private static final Path SELECTED_CLIENT = Path.of(
            "D:/380/天堂(Lineage 3.81C) (1)/LiTo.bin");
    private static final Path UNPACKED = Path.of(
            "D:/lineage開發/LiTo381-開發/runtime/LiTo.unpacked-oep-current.exe");
    private static final Path DISASSEMBLY = Path.of(
            "D:/lineage開發/LiTo381-開發/ida-client-oep-v2/full-disassembly.asm");

    @Test void selectedRowsPinObjectReagentEffectsAndPrimaryReuse()
            throws Exception {
        String primary = read(PRIMARY);
        String secondary = read(SECONDARY);
        assertEquals("INSERT INTO `skills` VALUES (205, 0, '立方：燃燒', 26, 4,"
                + " 30, 0, 49156, 1, 5000, 20, 'none', 32, 10, 0, 0, 0, 0,"
                + " 0, 2, 0, 0, 3, 0, 16, '$5939', 19, 6708, 6709, 0, 0, 0);",
                skillRow(primary));
        assertEquals("INSERT INTO `skills` VALUES (205, '立方：燃燒', 26, 4, 30,"
                + " 0, 49156, 1, 0, 20, 'none', 32, 10, 0, 0, 0, 0, 0, 2,"
                + " 0, 0, 3, 0, 16, '$5939', 19, 6708, 6709, 0, 0, 0);",
                skillRow(secondary));
        for (String sql : new String[] {primary, secondary}) {
            assertTrue(sql.contains("INSERT INTO `skills_item` VALUES"
                    + " (205, '立方：燃燒', NULL, NULL);"));
            assertTrue(sql.contains("49121, '記憶水晶(立方：燃燒)',"
                    + " 'skill.Skill_Illusionist', '$5685'"));
            assertTrue(sql.contains("49156, '屬性石', '0', '$6346',"
                    + " 'material', 'normal', 'gemstone', 1000, 3279, 7090"));
            assertTrue(sql.contains("INSERT INTO `npc` VALUES (80149,"
                    + " '幻術師技能(立方：燃燒)', '', '0', '', 'L1Effect', 6706"));
        }
    }

    @Test void selectedLiToEmitsOnly141904AndHasZeroCastClock()
            throws Exception {
        assertEquals("CE6EE68ADA8FD37E6C096B1A27E46CB6888F1179F85DEFB5007AC60287360292",
                sha256(SELECTED_CLIENT));
        String asm = read(DISASSEMBLY).replace("\r\n", "\n");
        String use = asm.substring(asm.indexOf("sub_4CB5F0 proc near"),
                asm.indexOf("sub_4CB5F0 endp") + 16);
        String generic = use.substring(use.indexOf("def_4CB66E:"));
        assertTrue(use.contains("cmp     esi, 0AFh"));
        assertTrue(generic.contains("and     edx, 7"));
        assertTrue(generic.contains("sar     esi, 3"));
        assertTrue(generic.contains("push    edx\npush    esi\npush    14h\n"
                + "push    offset off_5028CC\ncall    sub_4C6AE0"));
        assertEquals(0, readPeWordAtVa(UNPACKED, 0x63590CL));
        assertEquals("14 19 04", Protocol.hex(
                new byte[] {0x14, 0x19, 0x04}));
    }

    @Test void retainedRuntimePinsCasterTileDuplicateRadiusAndIndependentLeases()
            throws Exception {
        String use = read(RETAINED.resolve(
                "src/com/lineage/server/model/skill/L1SkillUse.java"));
        for (String fragment : new String[] {
                "_skill.getTarget().equals(\"none\")",
                "_targetX = _user.getX();",
                "_targetY = _user.getY();",
                "World.get().getVisibleObjects(pc, 3)",
                "(this._skillId == CUBE_IGNITION) && (gfxId == 6706)",
                "L1SpawnUtil.spawnEffect(80149,",
                "_user.getMapId(), _user, _skillId)"}) {
            assertTrue(use.contains(fragment),
                    "missing retained placement evidence: " + fragment);
        }

        String executor = read(RETAINED.resolve(
                "src/com/lineage/server/timecontroller/skill/EffectCubeExecutor.java"));
        for (String fragment : new String[] {
                "World.get().getVisibleObjects(effect, 3)",
                "effect.get_showId() != pc.get_showId()",
                "pc.getId() == user.getId()",
                "user.getClanid() == pc.getClanid()",
                "pc.getParty().isMember(user)",
                "pc.isSafetyZone()",
                "cha.addFire(30);",
                "cha.setSkillEffect(skillid, L1EffectInstance.CUBE_TIME)",
                "final L1Cube cube = new L1Cube(effect, cha, skillid);"}) {
            assertTrue(executor.contains(fragment),
                    "missing retained cube executor evidence: " + fragment);
        }

        String cube = read(RETAINED.resolve(
                "src/com/lineage/server/model/L1Cube.java"));
        for (String fragment : new String[] {
                "() -> runLease(lease), 900, 1000",
                "case STATUS_CUBE_IGNITION_TO_ENEMY:",
                "if (timeCounter % 4 != 0)",
                "this._cha.hasSkillEffect(ABSOLUTE_BARRIER)",
                "this._cha.hasSkillEffect(ICE_LANCE)",
                "this._cha.hasSkillEffect(FREEZING_BLIZZARD)",
                "this._cha.hasSkillEffect(FREEZING_BREATH)",
                "this._cha.hasSkillEffect(EARTH_BIND)",
                "pc.receiveDamage(this._effect, 10, false, true)",
                "mob.receiveDamage(this._effect, 10)"}) {
            assertTrue(cube.contains(fragment),
                    "missing retained enemy lease evidence: " + fragment);
        }

        String effect = read(RETAINED.resolve(
                "src/com/lineage/server/model/Instance/L1EffectInstance.java"));
        assertTrue(effect.contains("public static final int CUBE_TIME = 8000"));
        assertTrue(effect.contains("case 80149"));
        assertTrue(effect.contains("this._effectType = L1EffectType.isCubeBurn"));
        String stop = read(RETAINED.resolve(
                "src/com/lineage/server/model/skill/L1SkillStop.java"));
        assertTrue(stop.contains("case STATUS_CUBE_IGNITION_TO_ALLY"));
        assertTrue(stop.contains("cha.addFire(-30);"));

        String inventory = read(Path.of(
                "src/main/java/local/lito381/server/InventoryCatalog.java"));
        assertTrue(inventory.contains("static final long ATTRIBUTE_STONE = 49156;"));
        assertTrue(inventory.contains(
                "registerMaterial(items, ATTRIBUTE_STONE, 3_279, 7_090,"));
        String catalog = read(Path.of(
                "src/main/java/local/lito381/server/BattleSpellCatalog.java"));
        assertTrue(catalog.contains("CUBE_IGNITION, new CubeIgnitionSpell("));
        String session = read(Path.of(
                "src/main/java/local/lito381/server/ClientSession.java"));
        for (String fragment : new String[] {
                "useCubeIgnitionSpell(payload,",
                "hub.placeCubeIgnition(",
                "hub.placeClassicWorldCubeIgnition(",
                "Protocol.cubeIgnition(cube)",
                "serverCubeIgnitionStatus(",
                "serverCubeIgnitionDamage("}) {
            assertTrue(session.contains(fragment),
                    "missing skill205 implementation evidence: " + fragment);
        }
        String coverage = read(Path.of("tools/protocol_coverage.py"));
        assertTrue(coverage.contains(
                "skill205 Cube: Ignition is selected-client selector-only"));
    }

    private static String skillRow(String sql) {
        return sql.lines().filter(line -> line.startsWith(
                "INSERT INTO `skills` VALUES (205,"))
                .findFirst().orElseThrow();
    }

    private static int readPeWordAtVa(Path path, long va) throws Exception {
        byte[] bytes = Files.readAllBytes(path);
        ByteBuffer image = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);
        int pe = image.getInt(0x3C);
        int sections = Short.toUnsignedInt(image.getShort(pe + 6));
        int optional = pe + 24;
        int table = optional + Short.toUnsignedInt(image.getShort(pe + 20));
        long rva = va - Integer.toUnsignedLong(image.getInt(optional + 28));
        for (int i = 0; i < sections; i++) {
            int section = table + i * 40;
            long virtualSize = Integer.toUnsignedLong(image.getInt(section + 8));
            long sectionRva = Integer.toUnsignedLong(image.getInt(section + 12));
            long rawSize = Integer.toUnsignedLong(image.getInt(section + 16));
            long rawOffset = Integer.toUnsignedLong(image.getInt(section + 20));
            if (rva >= sectionRva
                    && rva + 2 <= sectionRva + Math.max(virtualSize, rawSize)) {
                return Short.toUnsignedInt(image.getShort(Math.toIntExact(
                        rawOffset + rva - sectionRva)));
            }
        }
        throw new IllegalArgumentException("unmapped PE VA");
    }

    private static String read(Path path) throws Exception {
        assertTrue(Files.isRegularFile(path), "missing source evidence: " + path);
        return Files.readString(path, StandardCharsets.UTF_8);
    }

    private static String sha256(Path path) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        try (var stream = Files.newInputStream(path)) {
            byte[] buffer = new byte[64 * 1024];
            for (int count; (count = stream.read(buffer)) >= 0;) {
                if (count > 0) digest.update(buffer, 0, count);
            }
        }
        return HexFormat.of().withUpperCase().formatHex(digest.digest());
    }
}
