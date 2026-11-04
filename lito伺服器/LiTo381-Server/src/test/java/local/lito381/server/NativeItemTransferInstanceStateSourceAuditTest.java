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

/** Hash-pinned proof for timed, sealed and active magic-doll item state. */
final class NativeItemTransferInstanceStateSourceAuditTest {
    @BeforeAll
    static void assumeLocalResourcesPresent() {
        LocalResources.assumeLineageSources(
                Path.of("../1-伺服器/src/com/lineage/data/item_etcitem/doll/Magic_Doll.java"),
                Path.of("../1-伺服器/src/com/lineage/server/datatables/user/CharItemsTable.java"),
                Path.of("../1-伺服器/src/com/lineage/server/datatables/user/CharItemsTimeTable.java"),
                Path.of("../1-伺服器/src/com/lineage/server/model/CharItemsTimePersistenceService.java"),
                Path.of("../1-伺服器/src/com/lineage/server/model/Instance/L1ItemInstance.java"),
                Path.of("../1-伺服器/src/com/lineage/server/model/L1Character.java"),
                Path.of("../1-伺服器/src/com/lineage/server/timecontroller/server/ServerItemUserTimer.java"));
    }

    private static final Path SERVER = Path.of("..", "1-伺服器")
            .toAbsolutePath().normalize();
    private static final Path LINEAGE = SERVER.resolve("src/com/lineage");
    private static final Path ITEM = LINEAGE.resolve(
            "server/model/Instance/L1ItemInstance.java");
    private static final Path ITEM_TIME = LINEAGE.resolve(
            "server/datatables/user/CharItemsTimeTable.java");
    private static final Path ITEM_TIME_STORE = LINEAGE.resolve(
            "server/model/CharItemsTimePersistenceService.java");
    private static final Path CHARACTER_ITEMS = LINEAGE.resolve(
            "server/datatables/user/CharItemsTable.java");
    private static final Path CHARACTER = LINEAGE.resolve(
            "server/model/L1Character.java");
    private static final Path MAGIC_DOLL = LINEAGE.resolve(
            "data/item_etcitem/doll/Magic_Doll.java");
    private static final Path ITEM_USER_TIMER = LINEAGE.resolve(
            "server/timecontroller/server/ServerItemUserTimer.java");
    private static final Path RUNTIME_ACCOUNTS = Path.of(
            "src/main/java/local/lito381/server/Accounts.java")
            .toAbsolutePath().normalize();

    @Test
    void retainedInstanceStateSourcesRemainPinned() throws Exception {
        assertEquals("10DD4F54A90BF2328A82DF1F014287BFFB4C9C7B1A80BD9EA0C430996B1E915C",
                sha256(ITEM));
        assertEquals("B0AD2BC0D437A9B65520E8C77B3C79F9A0AFC7E5FBC61B1E07A758657A0D01B9",
                sha256(ITEM_TIME));
        assertEquals("1AB48659564BB5B86C8CFDBB838CB408762AE3F198634ED9493D1ACFCB4379FE",
                sha256(ITEM_TIME_STORE));
        assertEquals("3FFE122376DD72A6897F5D108B85C351E5321AA8B2A7B23B541E44894E3FA4C1",
                sha256(CHARACTER_ITEMS));
        assertEquals("56DAD7E55D73924D631D28EE04A56413536F3921BE13D732607C412F24148C29",
                sha256(CHARACTER));
        assertEquals("A9AF7F362C1CA7A2F2BC83E211E35333429BAA134FE52BA62C74258A32706DE1",
                sha256(MAGIC_DOLL));
        assertEquals("E564547D82B78AFF8C4C83E3B8E07013A97A31193D38204F8B6B38CC2FF3F167",
                sha256(ITEM_USER_TIMER));
    }

    @Test
    void itemKeepsDeadlineAndSealIndependentFromEquipmentAndBlessing()
            throws Exception {
        String item = readLegacy(ITEM);
        ordered(item, "private final AtomicInteger _remainingTime",
                "private int _bless", "private boolean _isSealed",
                "public boolean isSealed()", "public void setSealed",
                "private Timestamp _time = null", "public void set_time",
                "public Timestamp get_time()");

        String table = readLegacy(CHARACTER_ITEMS);
        ordered(table, "final int is_equipped", "final int remaining_time",
                "boolean isSealed", "item.setEquipped", "item.setRemainingTime",
                "item.setSealed");
        assertTrue(table.contains("SET `remaining_time`=? WHERE `id`=?"));
        assertTrue(table.contains("SET `is_sealed`=? WHERE `id`=?"));
    }

    @Test
    void absoluteItemDeadlineUsesItsOwnSideTableAndObjectId() throws Exception {
        String table = readLegacy(ITEM_TIME);
        ordered(table, "loadTimedItems()", "addValue(record.itemObjId, record.userTime)",
                "item.set_time(usertime)", "delete(itemr_obj_id)",
                "ensureTimedItemPersisted", "item.get_time() != null",
                "item.set_time(ts)", "get().addTime(item.getId(), ts)");
        String persistence = readLegacy(ITEM_TIME_STORE);
        ordered(persistence, "SELECT * FROM `character_items_time`",
                "DELETE FROM `character_items_time` WHERE `itemr_obj_id`=?",
                "INSERT INTO `character_items_time` SET `itemr_obj_id`=?,`usertime`=?",
                "UPDATE `character_items_time` SET `usertime`=? WHERE `itemr_obj_id`=?");
    }

    @Test
    void activeDollMapIsRuntimeStateKeyedByBackingItemObjectId() throws Exception {
        String character = readLegacy(CHARACTER);
        ordered(character, "Map<Integer, L1DollInstance> _dolls",
                "_dolls.put(doll.getItemObjId(), doll)",
                "_dolls.remove(doll.getItemObjId(), doll)",
                "public L1DollInstance getDoll", "return _dolls.get(key)");
        String use = readLegacy(MAGIC_DOLL);
        ordered(use, "if (pc.getDoll(itemObjectId) != null)",
                "pc.getDoll(itemObjectId).deleteDoll()",
                "new L1DollInstance", "pc.getDoll(itemObjectId)");
    }

    @Test
    void timedItemScanAndReadOnlyProjectionMatchRetainedObjectDeadline()
            throws Exception {
        String timer = readLegacy(ITEM_USER_TIMER);
        ordered(timer, "final int timeMillis = 60 * 1000",
                "final Timestamp ts = new Timestamp(System.currentTimeMillis())",
                "item.get_time() == null", "item.get_time().before(ts)",
                "tgpc.getInventory().removeItem(item)",
                "CharItemsTable.get().deleteItem(item.get_char_objid(), item)");

        String accounts = LocalResources.readString(RUNTIME_ACCOUNTS);
        assertTrue(accounts.contains(
                "LEFT JOIN character_items_time t ON t.itemr_obj_id=i.id"));
        assertTrue(accounts.contains("i.is_sealed AS instance_sealed"));
        assertTrue(accounts.contains("t.usertime AS expires_at"));
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
