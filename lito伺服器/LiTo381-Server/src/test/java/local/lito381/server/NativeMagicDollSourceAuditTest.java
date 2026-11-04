package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** Hash-pinned authority for the retained magic-doll lifecycle. */
final class NativeMagicDollSourceAuditTest {
    @BeforeAll
    static void assumeLocalResourcesPresent() {
        LocalResources.assumeLineageSources(
                Path.of("../1-伺服器/db/380_db.sql"),
                Path.of("../1-伺服器/src/com/lineage/data/item_etcitem/doll/Magic_Doll.java"),
                Path.of("../1-伺服器/src/com/lineage/server/datatables/DollPowerTable.java"),
                Path.of("../1-伺服器/src/com/lineage/server/model/Instance/L1DollInstance.java"),
                Path.of("../1-伺服器/src/com/lineage/server/model/L1EquipmentSlot.java"),
                Path.of("../1-伺服器/src/com/lineage/server/model/drop/SetDrop.java"),
                Path.of("../1-伺服器/src/com/lineage/server/serverpackets/S_NPCPack_Doll.java"),
                Path.of("../1-伺服器/src/com/lineage/server/serverpackets/S_PacketBox.java"),
                Path.of("../1-伺服器/src/com/lineage/server/timecontroller/pc/PcRegenExecutor.java"),
                Path.of("../1-伺服器/src/com/lineage/server/timecontroller/pet/DollTimer.java"),
                Path.of("../1-伺服器/src/com/lineage/server/utils/CalcExp.java"));
    }

    private static final Path SERVER = Path.of("..", "1-伺服器")
            .toAbsolutePath().normalize();
    private static final Path LINEAGE = SERVER.resolve("src/com/lineage");
    private static final Path USE = LINEAGE.resolve(
            "data/item_etcitem/doll/Magic_Doll.java");
    private static final Path INSTANCE = LINEAGE.resolve(
            "server/model/Instance/L1DollInstance.java");
    private static final Path PACK = LINEAGE.resolve(
            "server/serverpackets/S_NPCPack_Doll.java");
    private static final Path BOX = LINEAGE.resolve(
            "server/serverpackets/S_PacketBox.java");
    private static final Path TIMER = LINEAGE.resolve(
            "server/timecontroller/pet/DollTimer.java");
    private static final Path POWER = LINEAGE.resolve(
            "server/datatables/DollPowerTable.java");
    private static final Path EQUIPMENT = LINEAGE.resolve(
            "server/model/L1EquipmentSlot.java");
    private static final Path CALC_EXP = LINEAGE.resolve(
            "server/utils/CalcExp.java");
    private static final Path DROP = LINEAGE.resolve(
            "server/model/drop/SetDrop.java");
    private static final Path REGEN = LINEAGE.resolve(
            "server/timecontroller/pc/PcRegenExecutor.java");
    private static final Path SQL = SERVER.resolve("db/380_db.sql");
    private static final Path ACCOUNTS = Path.of(
            "src/main/java/local/lito381/server/Accounts.java")
            .toAbsolutePath().normalize();
    private static final Path CLIENT = Path.of(
            "src/main/java/local/lito381/server/ClientSession.java")
            .toAbsolutePath().normalize();

    @Test
    void retainedSourcesRemainPinned() throws Exception {
        assertEquals("A9AF7F362C1CA7A2F2BC83E211E35333429BAA134FE52BA62C74258A32706DE1",
                sha256(USE));
        assertEquals("6A20B9D7B36B0E05E9C9C636E7AEB187036B01A049AB2B4B3708C1E779140A37",
                sha256(INSTANCE));
        assertEquals("3E162622CDC9789FC5F166E68662D24B94A6B9350236DB9F0E9FC7E9425971C2",
                sha256(PACK));
        assertEquals("817BA5FD5B8ADA480C6CEF6D0529B9CB8AC8C6A33D84AF550F8479A31FCCE977",
                sha256(BOX));
        assertEquals("62175E2B5B96D1CB068284888AEDA24ECB49FB7BA59C0DCBC66690EFA8149FFB",
                sha256(TIMER));
        assertEquals("A3237A8E86B94E1A1468589D638B81A7A2FA838CE0D949F4A7EDDC3521A5D896",
                sha256(POWER));
        assertEquals("1E072D0D794F3735B3A4A41C2E9C10E0E6060EDE54D3AED98D90E7E1FA9AA850",
                sha256(EQUIPMENT));
        assertEquals("5F8159AE44974ADCDEB6C2C0AE4458F9CF16ED77727F9DCA8BFD67A49799EEA3",
                sha256(CALC_EXP));
        assertEquals("17D961DD773C4ABAFC662010E2505EA4D2883780FF202008AF6056ACF5175808",
                sha256(DROP));
        assertEquals("4B69D91E865630CE7218A8DB7574989C7052CFF84EA98738CE570017E606FDB1",
                sha256(REGEN));
    }

    @Test
    void useAndInstanceOrderMatchRetainedLifecycle() throws Exception {
        String use = readLegacy(USE);
        ordered(use, "pc.getDoll(itemObjectId) != null",
                "pc.getDoll(itemObjectId).deleteDoll()",
                "pc.getDolls().size() >= ConfigAlt.MAX_DOLL_COUNT",
                "type.get_need()", "consumeItemsAtomically",
                "new L1DollInstance");
        String instance = readLegacy(INSTANCE);
        ordered(instance, "setGfxId(type.get_gfxid())",
                "set_time(type.get_time())", "setHeading(5)",
                "DollAbilityHelper.apply", "S_PacketBox.DOLL", "5935");
        assertTrue(instance.contains("new S_NPCPack_Doll(this, perceivedFrom)"));
        ordered(instance, "public void deleteDoll()", "5936",
                "DollAbilityHelper.apply(masterpc, _type, false)", "deleteMe()");
    }

    @Test
    void packetsTimerAndRuntimeProjectionUseExactFields() throws Exception {
        String pack = readLegacy(PACK);
        ordered(pack, "writeC(S_OPCODE_CHARPACK)", "writeH(pet.getX())",
                "writeD(pet.getId())", "writeH(pet.getGfxId())",
                "writeC(pet.getStatus())", "writeC(pet.getHeading())",
                "writeS(pet.getNameId())", "writeS(pet.getMaster().getName())",
                "writeC(0xff)");
        String box = readLegacy(BOX);
        assertTrue(box.contains("public static final int DOLL = 56"));
        ordered(box, "case DOLL:", "this.writeH(value)");
        String timer = readLegacy(TIMER);
        ordered(timer, "final int timeMillis = 1000",
                "final int time = doll.get_time() - 1", "if (time <= 0)",
                "outDoll(doll)");
        String accounts = LocalResources.readString(ACCOUNTS);
        assertTrue(accounts.contains("resourceTable(\"etcitem_doll_type\")"));
        assertTrue(accounts.contains("resourceTable(\"etcitem_doll_power_new\")"));
        assertTrue(accounts.contains("interval_time"));
        assertTrue(accounts.contains("passive_skill_id"));
        assertTrue(accounts.contains("trigger_skill_id"));
        assertTrue(accounts.contains("magic-doll type references missing"));
        assertTrue(accounts.contains("d.powers AS doll_powers"));
        assertTrue(accounts.contains("d.need AS doll_material_ids"));
        assertTrue(accounts.contains("d.`count` AS doll_material_counts"));
        assertTrue(accounts.contains("d.time AS doll_duration"));
        assertTrue(accounts.contains("d.gfxid AS doll_gfx"));
        assertTrue(accounts.contains("d.nameid AS doll_name"));
    }

    @Test
    void current190DollRowsReferenceExactlyThe15ClosedRuntimePowerIds()
            throws Exception {
        String sql = readLegacy(SQL);
        Pattern powerRow = Pattern.compile(
                "(?m)^INSERT INTO `etcitem_doll_power_new` VALUES \\((\\d+), '[^']*', '([^']+)'",
                Pattern.UNICODE_CASE);
        Map<Integer, String> classes = new LinkedHashMap<>();
        Matcher powers = powerRow.matcher(sql);
        while (powers.find()) {
            classes.put(Integer.parseInt(powers.group(1)), powers.group(2));
        }

        Pattern typeRow = Pattern.compile(
                "(?m)^INSERT INTO `etcitem_doll_type` VALUES \\([^,]+, [^,]+, [^,]+, '[^']*', '([^']*)'",
                Pattern.UNICODE_CASE);
        Matcher types = typeRow.matcher(sql);
        Set<Integer> referenced = new LinkedHashSet<>();
        int rowCount = 0;
        while (types.find()) {
            rowCount++;
            if (types.group(1).isBlank()) continue;
            for (String token : types.group(1).split(",")) {
                referenced.add(Integer.parseInt(token.trim()));
            }
        }
        assertEquals(190, rowCount);
        assertEquals(Set.of(1, 4, 10, 30, 33, 50, 72, 96, 97, 98,
                102, 106, 107, 135, 137), referenced);
        assertEquals(Set.of("PASSIVE_STR", "PASSIVE_DEX", "PASSIVE_INT",
                "PASSIVE_HIT", "PASSIVE_DMG", "PASSIVE_AC",
                "PASSIVE_DEF_WATER", "PASSIVE_EXP_RATE",
                "PASSIVE_DROP_RATE", "PASSIVE_WEIGHT_REDUCTION",
                "PASSIVE_SP", "TIMED_MP_REGEN", "STATUS_UNDERWATER"),
                referenced.stream().map(classes::get)
                        .collect(java.util.stream.Collectors.toSet()));
    }

    @Test
    void retainedConsumersAndLitoRuntimeUseTheSameFiveRemainingAbilities()
            throws Exception {
        String equipment = readLegacy(EQUIPMENT);
        assertTrue(equipment.contains("case SP:"));
        assertTrue(equipment.contains("owner.addSp(adjustedValue)"));
        assertTrue(equipment.contains("case WEIGHT_REDUCTION:"));
        assertTrue(equipment.contains("owner.addWeightReduction(adjustedValue)"));
        ordered(equipment, "case STATUS_UNDERWATER_BREATH:",
                "S_SkillIconBlessOfEva(owner.getId(), -1)",
                "STATUS_UNDERWATER_BREATH, 0");

        String calcExp = readLegacy(CALC_EXP);
        ordered(calcExp, "AbilityType.EXP_RATE", "bonusExpRate",
                "add_exp * (bonusExpRate / 100.0D)");
        String drop = readLegacy(DROP);
        ordered(drop, "pc.getDropRate()",
                "1.0 + playerDropRate / 100.0",
                "drop.getChance() * effectiveDropRate * rateOfMapId * dropRateMultiplier");
        String regen = readLegacy(REGEN);
        ordered(regen, "hpVal < 0",
                "pc.hasSkillEffect(STATUS_UNDERWATER_BREATH)",
                "hpVal = 0");

        String client = LocalResources.readString(CLIENT);
        assertTrue(client.contains(
                "activeMagicDollPowers().value(\"PASSIVE_SP\")"));
        assertTrue(client.contains(
                "percentMultiplier(\"PASSIVE_EXP_RATE\")"));
        assertTrue(client.contains(
                "percentMultiplier(\"PASSIVE_DROP_RATE\")"));
        assertTrue(client.contains("\"PASSIVE_WEIGHT_REDUCTION\""));
        assertTrue(client.contains("\"STATUS_UNDERWATER\""));
        assertTrue(client.contains(
                "Protocol.underwaterBreath(playerObjectId(), -1)"));
    }

    private static void ordered(String source, String... fragments) {
        int position = -1;
        for (String fragment : fragments) {
            position = source.indexOf(fragment, position + 1);
            assertTrue(position >= 0,
                    "missing/out-of-order source fragment: " + fragment);
        }
    }

    private static String readLegacy(Path path) throws Exception {
        byte[] bytes = LocalResources.readAllBytes(path);
        for (Charset charset : List.of(StandardCharsets.UTF_8,
                Charset.forName("Big5"), StandardCharsets.UTF_16)) {
            try {
                return charset.newDecoder().decode(ByteBuffer.wrap(bytes)).toString();
            } catch (CharacterCodingException ignored) {
                // Try the next retained source encoding.
            }
        }
        throw new IllegalArgumentException("unsupported retained source " + path);
    }

    private static String sha256(Path path) throws Exception {
        return HexFormat.of().withUpperCase().formatHex(
                MessageDigest.getInstance("SHA-256")
                        .digest(LocalResources.readAllBytes(path)));
    }
}
