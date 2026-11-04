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
import java.util.HexFormat;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** Hash-pinned proof for C_DROP gates, ground visibility and NPC onGetItem. */
final class NativeWorldDropVisibilityNpcPickupSourceAuditTest {
    @BeforeAll
    static void assumeLocalResourcesPresent() {
        LocalResources.assumeLineageSources();
    }

    private static final Path SERVER = Path.of("..", "1-伺服器")
            .toAbsolutePath().normalize();
    private static final Path LINEAGE = SERVER.resolve("src/com/lineage");
    private static final Path DROP = LINEAGE.resolve(
            "server/clientpackets/C_DropItem.java");
    private static final Path GROUND = LINEAGE.resolve(
            "server/model/L1GroundInventory.java");
    private static final Path NPC = LINEAGE.resolve(
            "server/model/Instance/L1NpcInstance.java");
    private static final Path DROP_TABLE = LINEAGE.resolve(
            "server/datatables/ItemDropTable.java");
    private static final Path CONFIG = LINEAGE.resolve("config/ConfigAlt.java");
    private static final Path SETTINGS = SERVER.resolve("config/altsettings.properties");
    private static final Path DATABASE = SERVER.resolve("db/380_db.sql");

    @Test
    void retainedSourcesAndSelectedInputsRemainPinned() throws Exception {
        assertEquals("74E111ECA656C740A59D0433C05698E734F699FD9199C006283484DE1F553752",
                sha256(DROP));
        assertEquals("2819D84FE5F4DA0CE6042BBD9B5E58493CF9D7E285A152BA4F3FAADBAB6D6208",
                sha256(GROUND));
        assertEquals("72E8DC365DAAC1B27D3F2C92672FCC77FF512825C4B6EAF9C96A6A87EA80EAD2",
                sha256(NPC));
        assertEquals("41692C657CA9B8E00FC32482D18186401E281FE63AADA436845BFABD47BB48CC",
                sha256(DROP_TABLE));
        assertEquals("80F97BDACF2AC9D4907CF25B1AA764A213E0B196D94DB20136A1FBF278C22B84",
                sha256(CONFIG));
        assertEquals("5CC73B4443F5BCBD2EA28A2BA0DBECF158320EED794A9333EC991814823FBB4A",
                sha256(SETTINGS));
        assertEquals("DA185BCA20CC254CB175EA2C367A2F5270514F4957B3AC99343C6101A2593766",
                sha256(DATABASE));
    }

    @Test
    void dropGatesAndMessagesKeepRetainedOrder() throws Exception {
        String drop = method(readLegacy(DROP), "public void start(",
                "public String getType()");
        ordered(drop, "if (pc.isGhost())", "if (!pc.isGm())",
                "ItemDropTable.get().isDroppable", "if (!ConfigAlt.DORP_ITEM)",
                "new S_ServerMessage(125)", "isTradable()",
                "new S_ServerMessage(210", "item.get_time() != null",
                "pc.getPetList()", "pet.getItemObjId()", "pc.getDoll(item.getId())",
                "new S_ServerMessage(1181)", "item.isEquipped()",
                "item.isSealed()", "pc.getInventory().tradeItem(item, dropCount",
                "pc.turnOnOffLight()");
    }

    @Test
    void selectedGlobalGateIsFalseAndWhitelistContainsOnlyMeat() throws Exception {
        String config = readLegacy(CONFIG);
        ordered(config, "public static boolean DORP_ITEM",
                "getProperty(\"dorpitem\", \"true\")");
        assertTrue(readLegacy(SETTINGS).contains("dorpitem = false"));

        String table = readLegacy(DROP_TABLE);
        ordered(table, "SELECT `itemid` FROM `道具_物品強制丟棄`",
                "_whiteList.add(itemid)", "isDroppable(final int itemid)");
        String database = readLegacy(DATABASE);
        int start = database.indexOf("-- Records of 道具_物品強制丟棄");
        int end = database.indexOf("-- ----------------------------", start + 40);
        assertTrue(start >= 0 && end > start);
        String rows = database.substring(start, end);
        assertEquals(1, occurrences(rows,
                "INSERT INTO `道具_物品強制丟棄` VALUES"));
        assertTrue(rows.contains("VALUES (40056, '肉')"));
    }

    @Test
    void groundInsertUpdateAndDeleteUseRecognizedPlayersAndKnownObjects()
            throws Exception {
        String ground = readLegacy(GROUND);
        String perceive = method(ground, "public void onPerceive",
                "public void insertItem");
        ordered(perceive, "perceivedFrom.get_showId() != item.get_showId()",
                "!perceivedFrom.knownsObject(item)",
                "perceivedFrom.addKnownObject(item)", "new S_NPCPack_Item(item)");
        String insert = method(ground, "public void insertItem",
                "public void updateItem");
        ordered(insert, "this.setTimer(item)",
                "World.get().getRecognizePlayer(item)",
                "pc.get_showId() != item.get_showId()",
                "new S_NPCPack_Item(item)", "pc.addKnownObject(item)");
        String update = method(ground, "public void updateItem",
                "public void deleteItem");
        ordered(update, "World.get().getRecognizePlayer(item)",
                "pc.get_showId() != item.get_showId()",
                "new S_NPCPack_Item(item)");
        String delete = method(ground, "public void deleteItem", "}");
        ordered(delete, "ServerDeleteItemTimer.removeByItemId(item.getId())",
                "World.get().getRecognizePlayer(item)", "new S_RemoveObject(item)",
                "pc.removeKnownObject(item)");
    }

    @Test
    void autonomousPickupRemovesGroundThenRunsRefineShuffleAndDigestWithoutLight()
            throws Exception {
        String npc = readLegacy(NPC);
        String pickup = method(npc, "public synchronized void pickupTargetItem",
                "public boolean noTarget()");
        ordered(pickup, "groundInv.tradeItem(targetItem",
                "//turnOnOffLight();", "onGetItem(item)",
                "setSleepTime(1000)");
        String onGet = method(npc, "public void onGetItem",
                "public void approachPlayer");
        ordered(onGet, "this.refineItem()", "this.getInventory().shuffle()",
                "get_digestitem() > 0", "this.setDigestItem(item)");
    }

    private static int occurrences(String source, String fragment) {
        int count = 0;
        int position = -1;
        while ((position = source.indexOf(fragment, position + 1)) >= 0) count++;
        return count;
    }

    private static void ordered(String source, String... fragments) {
        int position = -1;
        for (String fragment : fragments) {
            position = source.indexOf(fragment, position + 1);
            assertTrue(position >= 0,
                    "missing/out-of-order source fragment: " + fragment);
        }
    }

    private static String method(String source, String startText, String endText) {
        int start = source.indexOf(startText);
        int end = source.indexOf(endText, start + startText.length());
        assertTrue(start >= 0 && end > start, "missing method boundary " + startText);
        return source.substring(start, end);
    }

    private static String readLegacy(Path path) throws Exception {
        byte[] bytes = Files.readAllBytes(path);
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
                        .digest(Files.readAllBytes(path)));
    }
}
