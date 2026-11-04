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

/** Hash-pinned proof for retained monster ground-item targeting and transfer. */
final class NativeWorldMonsterPickupSourceAuditTest {
    @BeforeAll
    static void assumeLocalResourcesPresent() {
        LocalResources.assumeLineageSources(
                Path.of("../1-伺服器/config/altsettings.properties"),
                Path.of("../1-伺服器/config/rates.properties"),
                Path.of("../1-伺服器/db/380_db.sql"),
                Path.of("../1-伺服器/src/com/lineage/server/model/Instance/L1NpcInstance.java"),
                Path.of("../1-伺服器/src/com/lineage/server/model/Instance/NpcAI.java"),
                Path.of("../1-伺服器/src/com/lineage/server/model/L1GroundInventory.java"));
    }

    private static final Path RETAINED = Path.of("..", "1-伺服器")
            .toAbsolutePath().normalize();
    private static final Path NPC = RETAINED.resolve(
            "src/com/lineage/server/model/Instance/L1NpcInstance.java");
    private static final Path AI = RETAINED.resolve(
            "src/com/lineage/server/model/Instance/NpcAI.java");
    private static final Path GROUND = RETAINED.resolve(
            "src/com/lineage/server/model/L1GroundInventory.java");
    private static final Path DATABASE = RETAINED.resolve("db/380_db.sql");
    private static final Path ALT = RETAINED.resolve("config/altsettings.properties");
    private static final Path RATES = RETAINED.resolve("config/rates.properties");

    @Test
    void retainedSourcesAndSelectedDatabaseRemainHashPinned() throws Exception {
        assertEquals("72E8DC365DAAC1B27D3F2C92672FCC77FF512825C4B6EAF9C96A6A87EA80EAD2",
                sha256(NPC));
        assertEquals("F31A67C15C4C8473CC236E4E74D79B7CBE625DA807F0E40BEE715AC7640EFA41",
                sha256(AI));
        assertEquals("2819D84FE5F4DA0CE6042BBD9B5E58493CF9D7E285A152BA4F3FAADBAB6D6208",
                sha256(GROUND));
        assertEquals(ClassicWorldMonsterPickupCatalog.SELECTED_DATABASE_SHA256,
                sha256(DATABASE));
        assertEquals("5CC73B4443F5BCBD2EA28A2BA0DBECF158320EED794A9333EC991814823FBB4A",
                sha256(ALT));
        assertEquals("DE9A1DAAD8DB84D3F26D8261E91CBE2D63A638D1014FE0C2665A0A2B6B6993B0",
                sha256(RATES));
    }

    @Test
    void npcRetainsSearchQueueDistancePathPickupAndDelayOrder() throws Exception {
        String source = readLegacy(NPC);
        assertTrue(source.contains("this.setPickupItem(template.is_picupitem());"));
        String search = method(source, "public void searchTargetItem()",
                "public void searchItemFromAir()");
        ordered(search, "World.get().getVisibleObjects(this)",
                "Math.random() * gInventorys.size()",
                "checkAddItem(item, item.getCount())",
                "this._targetItem = item",
                "this._targetItemList.add(this._targetItem)");

        String check = method(source, "public void checkTargetItem()",
                "public void onTargetItem()");
        ordered(check, "while ((_targetItem == null)",
                "_targetItem.getMapId() != getMapId()",
                "getTileDistance(_targetItem.getLocation()) > 15",
                "_targetItemList.isEmpty()",
                "_targetItem = _targetItemList.remove(0)");

        String target = method(source, "public void onTargetItem()",
                "public synchronized void pickupTargetItem");
        ordered(target, "getTileLineDistance(_targetItem.getLocation()) == 1",
                "pickupTargetItem(_targetItem)",
                "moveDirection(_targetItem.getX(), _targetItem.getY())",
                "if (dir == -1)", "_targetItemList.remove(_targetItem)",
                "_npcMove.setDirectionMove(dir)",
                "calcSleepTime(getPassispeed(), MOVE_SPEED)");

        String pickup = method(source, "public synchronized void pickupTargetItem",
                "public boolean noTarget()");
        ordered(pickup, "targetItem == null || deletionStarted() || destroyed()",
                "World.get().getInventory(x, y, m)",
                "groundInv.getItem(targetItem.getId()) != targetItem",
                "final long transferCount = targetItem.getCount()",
                "groundInv.tradeItem(targetItem, transferCount, getInventory())",
                "if (item == null)", "onGetItem(item)",
                "_targetItemLock.lock()", "_targetItemList.remove(targetItem)",
                "if (_targetItem == targetItem)", "_targetItem = null",
                "_targetItemLock.unlock()", "setSleepTime(1000)");
    }

    @Test
    void aiPrioritizesCombatThenItemBranchAndGroundDeleteBroadcastsRemove()
            throws Exception {
        String ai = method(readLegacy(AI), "private boolean AIProcess()",
                "public static String getMonitorLine()");
        ordered(ai, "checkTarget()", "searchTarget()", "onItemUse()",
                "is_now_target() == null", "checkTargetItem()",
                "isPickupItem()", "searchTargetItem()",
                "is_now_targetItem() == null", "noTarget()",
                "groundInventory.checkItem", "onTargetItem()",
                "setSleepTime(1000)", "onTarget()");

        String ground = method(readLegacy(GROUND),
                "public void deleteItem", "}");
        assertTrue(ground.contains("pc.sendPackets(new S_RemoveObject(item))"));
        assertTrue(readLegacy(ALT).contains("MaxNpcItem = 8"));
        assertTrue(readLegacy(RATES).contains("RateWeightLimitforPet = 1.0"));
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
        assertTrue(start >= 0 && end > start,
                "missing method boundary " + startText);
        return source.substring(start, end);
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
