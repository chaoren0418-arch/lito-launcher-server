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

/** Cross-source proof for selected Illusionist skill220 Cube: Harmony. */
final class NativeCubeHarmonySourceAuditTest {
    @BeforeAll
    static void assumeLocalResourcesPresent() {
        LocalResources.assumeLineageSources(
                Path.of("../1-伺服器/db/380_db.sql"),
                Path.of("../1-伺服器/其他src參考用/0507備份/yiwei380/db/380yiwei_db.sql"));
        LocalResources.assumeLitoBin(
                Path.of("D:/380/天堂(Lineage 3.81C) (1)/LiTo.bin"));
        LocalResources.assumeIdaClient(
                Path.of("D:/lineage開發/LiTo381-開發/ida-client-oep-v2/full-disassembly.asm"));
        LocalResources.assumeFiles(
                Path.of("D:/lineage開發/LiTo381-開發/runtime/LiTo.unpacked-oep-current.exe"));
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

    @Test void selectedRowsPinTwoStonesObjectAndPrimaryReuse()
            throws Exception {
        String primary = read(PRIMARY);
        String secondary = read(SECONDARY);
        assertEquals("INSERT INTO `skills` VALUES (220, 0, '立方：和諧', 28, 3,"
                + " 55, 0, 49156, 2, 5000, 20, 'none', 32, 0, 0, 0, 0, 0,"
                + " 0, 2, 0, 0, 3, 0, 8, '$5954', 19, 6727, 6727, 0, 0, 0);",
                skillRow(primary));
        assertEquals("INSERT INTO `skills` VALUES (220, '立方：和諧', 28, 3, 55,"
                + " 0, 49156, 2, 0, 20, 'none', 32, 0, 0, 0, 0, 0, 0, 2,"
                + " 0, 0, 3, 0, 8, '$5954', 19, 6727, 6727, 0, 0, 0);",
                skillRow(secondary));
        for (String sql : new String[] {primary, secondary}) {
            assertTrue(sql.contains("INSERT INTO `skills_item` VALUES"
                    + " (220, '立方：和諧', NULL, NULL);"));
            assertTrue(sql.contains("49136, '記憶水晶(立方：和諧)',"
                    + " 'skill.Skill_Illusionist', '$5700'"));
            assertTrue(sql.contains("INSERT INTO `npc` VALUES (80152,"
                    + " '幻術師技能(立方：和諧)', '', '0', '', 'L1Effect', 6724"));
        }
    }

    @Test void selectedLiToEmitsOnly141b03AndHas32MsCastClock()
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
        assertEquals(32, readPeWordAtVa(UNPACKED, 0x63592AL));
        assertEquals("14 1B 03", Protocol.hex(
                new byte[] {0x14, 0x1B, 0x03}));
    }

    @Test void retainedRuntimePinsSharedStatusAndIndependent900Clock()
            throws Exception {
        Path timerPath = retained("timecontroller/skill/EffectCubeHarmonizeTimer.java");
        Path executorPath = retained("timecontroller/skill/EffectCubeExecutor.java");
        Path cubePath = retained("model/L1Cube.java");
        Path usePath = retained("model/skill/L1SkillUse.java");
        Path packetPath = retained("clientpackets/C_UseSkill.java");
        Path stopPath = retained("model/skill/L1SkillStop.java");
        Path effectPath = retained("model/Instance/L1EffectInstance.java");
        Path startPath = retained("timecontroller/StartTimer_Skill.java");
        assertEquals("8A6074A2193CC12BE1E2BCB0403E8BEA51F556E7759EB6350A66F6420B5B50E0",
                sha256(timerPath));
        assertEquals("2AFB937E1ED5751CB9862AEE913CC6728E5A03AF97A6DAD3E7F9BCC447702D50",
                sha256(executorPath));
        assertEquals("212E6DBAF6DD79311A66D140616702FB050DE30D1C926A9E32FD18F4ED35D38D",
                sha256(cubePath));
        assertEquals("D2FD22F2DD76811579DE46F32B8482B3CBA23F45235B11DF50B5E3F6220D3B0A",
                sha256(usePath));
        assertEquals("399D76B42744A281E57C369E939A61AE94DE39B46857733BC240AAF7A9A2EFE4",
                sha256(packetPath));
        assertEquals("1C48B3681AAA4EEEA671BA9E25837D3D52EE504CA669516B50418CE62DC0AF52",
                sha256(stopPath));
        assertEquals("C62C43476EE61AAB69C348587ECB5C29B461B998BA4BC95A1B63EB3903C95D5B",
                sha256(effectPath));
        assertEquals("0A865E7A8010AF12D6CC1C894180CB0D23330C593729A60E74218E40D4ADDA28",
                sha256(startPath));

        String use = read(usePath);
        assertTrue(use.contains("(this._skillId == CUBE_BALANCE) && (gfxId == 6724)"));
        assertTrue(use.contains("L1SpawnUtil.spawnEffect(80152,"));
        String packet = read(packetPath);
        assertTrue(packet.contains("case 220:// 立方：和諧 6100"));
        assertTrue(packet.contains("targetId = readD();"));

        String executor = read(executorPath);
        for (String fragment : new String[] {
                "World.get().getVisibleObjects(effect, 3)",
                "case isCubeHarmonize:",
                "cubeToAllyId = STATUS_CUBE_BALANCE;",
                "cubeToEnemyId = STATUS_CUBE_BALANCE;",
                "cha.setSkillEffect(skillid, L1EffectInstance.CUBE_TIME)",
                "new L1Cube(effect, cha, STATUS_CUBE_BALANCE)"}) {
            assertTrue(executor.contains(fragment), fragment);
        }
        String cube = read(cubePath);
        for (String fragment : new String[] {
                "() -> runLease(lease), 900, 1000",
                "case STATUS_CUBE_BALANCE:",
                "timeCounter % 4 == 0",
                "this._cha.adjustCurrentMp(5);",
                "timeCounter % 5 == 0",
                "pc.receiveDamage(this._effect, 25, false, true);",
                "mob.receiveDamage(this._effect, 25);"}) {
            assertTrue(cube.contains(fragment), fragment);
        }
        assertTrue(read(effectPath).contains("public static final int CUBE_TIME = 8000"));
        assertTrue(read(stopPath).contains("case STATUS_CUBE_BALANCE:"));
        assertTrue(read(timerPath).contains("executor.cubeBurn(effect);"));
        assertTrue(read(startPath).contains(
                "safeSchedule(\"EffectCubeHarmonizeTimer\", new EffectCubeHarmonizeTimer(), 0, 1000"));
    }

    @Test void implementationPinsBothPhasesAndAllAuthorityTransitions()
            throws Exception {
        String catalog = read(Path.of(
                "src/main/java/local/lito381/server/BattleSpellCatalog.java"));
        String effects = read(Path.of(
                "src/main/java/local/lito381/server/BattleCubeHarmonyEffects.java"));
        String hub = read(Path.of(
                "src/main/java/local/lito381/server/GameHub.java"));
        String session = read(Path.of(
                "src/main/java/local/lito381/server/ClientSession.java"));
        for (String fragment : new String[] {
                "CUBE_HARMONY, new CubeHarmonySpell(",
                "CUBE_HARMONY, 27, 3, 19",
                "6_724, 6_727",
                "55, 5_000, 32, 20, 3",
                "32, 2, 8, 5, 25",
                "49_156L, 2, 80_152",
                "49_136L, \"$5700\", \"$5954\""}) {
            assertTrue(catalog.contains(fragment), fragment);
        }
        for (String fragment : new String[] {
                "INITIAL_DELAY_MILLIS = 900",
                "MP_EVERY_TICKS = 4",
                "HP_EVERY_TICKS = 5",
                "MP_RECOVERY = 5",
                "HP_DAMAGE = 25",
                "private final Map<Long, StatusLease> statuses"}) {
            assertTrue(effects.contains(fragment), fragment);
        }
        for (String fragment : new String[] {
                "placeCubeHarmony(",
                "placeClassicWorldCubeHarmony(",
                "cubeHarmonyTicks(",
                "classicWorldCubeHarmonyEffects",
                "cubeHarmonyEffects.advance("}) {
            assertTrue(hub.contains(fragment), fragment);
        }
        for (String fragment : new String[] {
                "useCubeHarmonySpell(payload,",
                "Protocol.cubeHarmony(cube)",
                "serverCubeHarmonyMpRecovery(",
                "serverCubeHarmonyDamage(",
                "serverCubeHarmonyNonPlayerMpRecovery(",
                "serverCubeHarmonyNonPlayerDamage("}) {
            assertTrue(session.contains(fragment), fragment);
        }
    }

    private static Path retained(String relative) {
        return RETAINED.resolve("src/com/lineage/server/" + relative);
    }

    private static String skillRow(String sql) {
        return sql.lines().filter(line -> line.startsWith(
                "INSERT INTO `skills` VALUES (220,"))
                .findFirst().orElseThrow();
    }

    private static int readPeWordAtVa(Path path, long va) throws Exception {
        byte[] bytes = LocalResources.readAllBytes(path);
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
        assertTrue(Files.isRegularFile(LocalResources.assumeExternalFile(path)), "missing source evidence: " + path);
        return LocalResources.readString(path, StandardCharsets.UTF_8);
    }

    private static String sha256(Path path) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        try (var stream = LocalResources.newInputStream(path)) {
            byte[] buffer = new byte[64 * 1024];
            for (int count; (count = stream.read(buffer)) >= 0;) {
                if (count > 0) digest.update(buffer, 0, count);
            }
        }
        return HexFormat.of().withUpperCase().formatHex(digest.digest());
    }
}
