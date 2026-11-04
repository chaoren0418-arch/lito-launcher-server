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

/** Hash-pinned source proof for C_GIVE -> onGetItem -> refine/digest. */
final class NativeWorldMonsterItemLifecycleSourceAuditTest {
    @BeforeAll
    static void assumeLocalResourcesPresent() {
        LocalResources.assumeLineageSources();
    }

    private static final Path RETAINED = Path.of("..", "1-伺服器")
            .toAbsolutePath().normalize();
    private static final Path NPC_INSTANCE = RETAINED.resolve(
            "src/com/lineage/server/model/Instance/L1NpcInstance.java");
    private static final Path NPC_TABLE = RETAINED.resolve(
            "src/com/lineage/server/datatables/NpcTable.java");
    private static final Path GIVE = RETAINED.resolve(
            "src/com/lineage/server/clientpackets/C_GiveItem.java");
    private static final Path GIVE_TABLE = RETAINED.resolve(
            "src/com/lineage/server/datatables/ServerItemGiveTable.java");
    private static final Path DIGEST_TIMER = RETAINED.resolve(
            "src/com/lineage/server/timecontroller/npc/NpcDigestItemTimer.java");
    private static final Path DATABASE = RETAINED.resolve("db/380_db.sql");
    private static final Path RESOURCE = Path.of(
            "src/main/resources/world/classic-world-monster-item-lifecycle.lmi.gz");

    @Test
    void retainedFilesDatabaseAndGeneratedProjectionRemainPinned() throws Exception {
        assertEquals(ClassicWorldMonsterItemLifecycleCatalog.RETAINED_NPC_INSTANCE_SHA256,
                sha256(NPC_INSTANCE));
        assertEquals(ClassicWorldMonsterItemLifecycleCatalog.RETAINED_NPC_TABLE_SHA256,
                sha256(NPC_TABLE));
        assertEquals(ClassicWorldMonsterItemLifecycleCatalog.RETAINED_GIVE_HANDLER_SHA256,
                sha256(GIVE));
        assertEquals(ClassicWorldMonsterItemLifecycleCatalog.RETAINED_GIVE_TABLE_SHA256,
                sha256(GIVE_TABLE));
        assertEquals(ClassicWorldMonsterItemLifecycleCatalog.RETAINED_DIGEST_TIMER_SHA256,
                sha256(DIGEST_TIMER));
        assertEquals(ClassicWorldMonsterItemLifecycleCatalog.SELECTED_DATABASE_SHA256,
                sha256(DATABASE));
        assertEquals(ClassicWorldMonsterItemLifecycleCatalog.RESOURCE_SHA256,
                sha256(RESOURCE));
        assertTrue(readLegacy(NPC_TABLE)
                .contains("npc.set_digestitem(rs.getInt(\"digestitem\"))"));
    }

    @Test
    void retainedGiveAndOnGetItemOrderingStayExact() throws Exception {
        String give = readLegacy(GIVE);
        ordered(give,
                "findObject(targetId)",
                "isNpcItemReceivable(target.getNpcTemplate())",
                "ServerItemGiveTable.get().getTemplate(item.getItemId())",
                "Math.abs(pcx - x) >= 3",
                "targetInv.checkAddItem(item, count)",
                "inv.tradeItem(item, count, targetInv)",
                "target.onGetItem(getItem)");
        String npc = readLegacy(NPC_INSTANCE);
        String onGet = method(npc, "public void onGetItem(", "public void approachPlayer(");
        ordered(onGet, "this.refineItem()", "this.getInventory().shuffle()",
                "this.setDigestItem(item)");
    }

    @Test
    void retainedRecipesAndFiveSecondWholeStackExpiryStayPinned() throws Exception {
        String npc = readLegacy(NPC_INSTANCE);
        String refine = method(npc, "public void refineItem()", "// AI");
        ordered(refine,
                "new int[] { 40508, 40521, 40045 }",
                "new int[] { 20 }",
                "new int[] { 40494, 40521 }",
                "new int[] { 19 }",
                "new int[] { 3 }",
                "new int[] { 88, 40508, 40045 }",
                "new int[] { 100 }",
                "new int[] { 88, 40494 }",
                "new int[] { 89 }");
        String timer = readLegacy(DIGEST_TIMER);
        assertTrue(timer.contains("final int timeMillis = 5_000"));
        assertTrue(timer.contains("mob.advanceDigestItems(5)"));
        String advance = method(npc,
                "public void advanceDigestItems(", "public void onGetItem(");
        ordered(advance, "deadline._del_item_time -= elapsedSeconds",
                "removeItem(item, item.getCount())", "iterator.remove()");
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
