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

/** Cross-source proof for selected Illusionist skill210 Cube: Quake. */
final class NativeCubeQuakeSourceAuditTest {
    @BeforeAll
    static void assumeLocalResourcesPresent() {
        LocalResources.assumeLineageSources(
                Path.of("../1-伺服器/db/380_db.sql"),
                Path.of("../1-伺服器/src/com/lineage/server/model/L1Cube.java"),
                Path.of("../1-伺服器/src/com/lineage/server/model/skill/L1SkillUse.java"),
                Path.of("../1-伺服器/src/com/lineage/server/model/skill/skillmode/MOVE_STOP.java"),
                Path.of("../1-伺服器/src/com/lineage/server/serverpackets/S_Paralysis.java"),
                Path.of("../1-伺服器/src/com/lineage/server/timecontroller/skill/EffectCubeExecutor.java"),
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

    @Test void selectedRowsPinReagentObjectResistanceAndPrimaryReuse()
            throws Exception {
        String primary = read(PRIMARY);
        String secondary = read(SECONDARY);
        assertEquals("INSERT INTO `skills` VALUES (210, 0, '立方：地裂', 27, 1,"
                + " 35, 0, 49156, 1, 5000, 20, 'none', 32, 0, 0, 0, 0, 0,"
                + " 0, 2, 0, 0, 3, 0, 2, '$5944', 19, 6714, 6715, 0, 0, 0);",
                skillRow(primary));
        assertEquals("INSERT INTO `skills` VALUES (210, '立方：地裂', 27, 1, 35,"
                + " 0, 49156, 1, 0, 20, 'none', 32, 0, 0, 0, 0, 0, 0, 2,"
                + " 0, 0, 3, 0, 2, '$5944', 19, 6714, 6715, 0, 0, 0);",
                skillRow(secondary));
        for (String sql : new String[] {primary, secondary}) {
            assertTrue(sql.contains("INSERT INTO `skills_item` VALUES"
                    + " (210, '立方：地裂', NULL, NULL);"));
            assertTrue(sql.contains("49126, '記憶水晶(立方：地裂)',"
                    + " 'skill.Skill_Illusionist', '$5690'"));
            assertTrue(sql.contains("49156, '屬性石', '0', '$6346',"
                    + " 'material', 'normal', 'gemstone', 1000, 3279, 7090"));
            assertTrue(sql.contains("INSERT INTO `npc` VALUES (80150,"
                    + " '幻術師技能(立方：地裂)', '', '0', '', 'L1Effect', 6712"));
        }
    }

    @Test void selectedLiToEmitsOnly141a01AndHasZeroCastClock()
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
        assertEquals(0, readPeWordAtVa(UNPACKED, 0x635916L));
        assertEquals("14 1A 01", Protocol.hex(
                new byte[] {0x14, 0x1A, 0x01}));
    }

    @Test void retainedRuntimePinsEarthLeaseAndMovementOnlyBind()
            throws Exception {
        Path usePath = RETAINED.resolve(
                "src/com/lineage/server/model/skill/L1SkillUse.java");
        Path executorPath = RETAINED.resolve(
                "src/com/lineage/server/timecontroller/skill/EffectCubeExecutor.java");
        Path cubePath = RETAINED.resolve(
                "src/com/lineage/server/model/L1Cube.java");
        Path moveStopPath = RETAINED.resolve(
                "src/com/lineage/server/model/skill/skillmode/MOVE_STOP.java");
        Path paralysisPath = RETAINED.resolve(
                "src/com/lineage/server/serverpackets/S_Paralysis.java");
        assertEquals("D2FD22F2DD76811579DE46F32B8482B3CBA23F45235B11DF50B5E3F6220D3B0A",
                sha256(usePath));
        assertEquals("2AFB937E1ED5751CB9862AEE913CC6728E5A03AF97A6DAD3E7F9BCC447702D50",
                sha256(executorPath));
        assertEquals("212E6DBAF6DD79311A66D140616702FB050DE30D1C926A9E32FD18F4ED35D38D",
                sha256(cubePath));
        assertEquals("06211651D85807BD8B9533216996CC20FF600FDC8E4E7B1CBB2C86115092BDC6",
                sha256(moveStopPath));
        assertEquals("7E57E26309FA5E8221A189CE7C82C64BE5D03EE9171D9BE8D0D74FB4ABF2146A",
                sha256(paralysisPath));

        String use = read(usePath);
        for (String fragment : new String[] {
                "_skill.getTarget().equals(\"none\")",
                "_targetX = _user.getX();",
                "_targetY = _user.getY();",
                "(this._skillId == CUBE_QUAKE) && (gfxId == 6712)",
                "L1SpawnUtil.spawnEffect(80150,"}) {
            assertTrue(use.contains(fragment),
                    "missing retained placement evidence: " + fragment);
        }

        String executor = read(executorPath);
        for (String fragment : new String[] {
                "World.get().getVisibleObjects(effect, 3)",
                "case isCubeEruption:",
                "cha.addEarth(30);",
                "cubeToAllyId = STATUS_CUBE_QUAKE_TO_ALLY;",
                "cubeToEnemyId = STATUS_CUBE_QUAKE_TO_ENEMY;",
                "cha.setSkillEffect(skillid, L1EffectInstance.CUBE_TIME)",
                "final L1Cube cube = new L1Cube(effect, cha, skillid);"}) {
            assertTrue(executor.contains(fragment),
                    "missing retained executor evidence: " + fragment);
        }

        String cube = read(cubePath);
        for (String fragment : new String[] {
                "() -> runLease(lease), 900, 1000",
                "case STATUS_CUBE_QUAKE_TO_ENEMY:",
                "if (timeCounter % 4 != 0)",
                "this._cha.hasSkillEffect(STATUS_FREEZE)",
                "this._cha.hasSkillEffect(ABSOLUTE_BARRIER)",
                "this._cha.hasSkillEffect(ICE_LANCE)",
                "this._cha.hasSkillEffect(FREEZING_BLIZZARD)",
                "this._cha.hasSkillEffect(FREEZING_BREATH)",
                "this._cha.hasSkillEffect(EARTH_BIND)",
                "pc.setSkillEffect(MOVE_STOP, 1000)",
                "new S_Paralysis(S_Paralysis.TYPE_BIND, true)",
                "mob.setSkillEffect(STATUS_FREEZE, 1000)",
                "mob.setParalyzed(true)"}) {
            assertTrue(cube.contains(fragment),
                    "missing retained quake clock evidence: " + fragment);
        }

        String moveStop = read(moveStopPath);
        assertTrue(moveStop.contains("TYPE_BIND, false"));
        assertTrue(moveStop.contains("new S_Poison(pc.getId(), 0)"));
        String paralysis = read(paralysisPath);
        assertTrue(paralysis.contains("case TYPE_BIND"));
        assertTrue(paralysis.contains("this.writeC(0x18)"));
        assertTrue(paralysis.contains("this.writeC(0x19)"));
    }

    @Test void implementationPinsBothPhasesIndependentBindAndNpcAiGate()
            throws Exception {
        String catalog = read(Path.of(
                "src/main/java/local/lito381/server/BattleSpellCatalog.java"));
        String effects = read(Path.of(
                "src/main/java/local/lito381/server/BattleCubeQuakeEffects.java"));
        String hub = read(Path.of(
                "src/main/java/local/lito381/server/GameHub.java"));
        String session = read(Path.of(
                "src/main/java/local/lito381/server/ClientSession.java"));
        for (String fragment : new String[] {
                "CUBE_QUAKE, new CubeQuakeSpell(",
                "CUBE_QUAKE, 26, 1, 19",
                "6_712, 6_714, 6_715",
                "35, 5_000, 0, 20, 3",
                "49_156L, 1, 80_150",
                "49_126L, \"$5690\", \"$5944\""}) {
            assertTrue(catalog.contains(fragment),
                    "missing Cube: Quake catalog evidence: " + fragment);
        }
        for (String fragment : new String[] {
                "ENEMY_BIND_STARTED",
                "ENEMY_BIND_EXPIRED",
                "ENEMY_BIND_EVERY_TICKS = 4",
                "ENEMY_BIND_MILLIS = 1_000",
                "BindProtection",
                "movementStopped(",
                "bindCount("}) {
            assertTrue(effects.contains(fragment),
                    "missing Cube: Quake lifecycle evidence: " + fragment);
        }
        for (String fragment : new String[] {
                "placeCubeQuake(",
                "placeClassicWorldCubeQuake(",
                "cubeQuakeTicks(",
                "cubeQuakeEffects.movementStopped(2, nowNanos)",
                "cubeQuakeEffects.movementStopped(objectId, nowNanos)",
                "classicWorldCubeQuakeMonsterMovementStopped("}) {
            assertTrue(hub.contains(fragment),
                    "missing Cube: Quake hub evidence: " + fragment);
        }
        for (String fragment : new String[] {
                "useCubeQuakeSpell(payload,",
                "Protocol.cubeQuake(cube)",
                "serverCubeQuakeStatus(",
                "serverCubeQuakeBind(",
                "Protocol.paralysis(Protocol.ParalysisMode.BIND_APPLY)",
                "Protocol.paralysis(Protocol.ParalysisMode.BIND_RELEASE)",
                "cubeQuakeMovementStopped(moveNow)",
                "effectiveCubeQuakeEarthResistance(nowNanos)"}) {
            assertTrue(session.contains(fragment),
                    "missing Cube: Quake session evidence: " + fragment);
        }
    }

    private static String skillRow(String sql) {
        return sql.lines().filter(line -> line.startsWith(
                "INSERT INTO `skills` VALUES (210,"))
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
