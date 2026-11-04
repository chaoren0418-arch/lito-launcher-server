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
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** Hash-pinned proof for the complete retained C_GIVE NPC receiver domain. */
final class NativeWorldNpcGiveReceiversSourceAuditTest {
    @BeforeAll
    static void assumeLocalResourcesPresent() {
        LocalResources.assumeLineageSources(
                Path.of("../1-伺服器/db/380_db.sql"),
                Path.of("../1-伺服器/src/com/lineage/server/clientpackets/C_GiveItem.java"),
                Path.of("../1-伺服器/src/com/lineage/server/model/Instance/L1NpcInstance.java"));
    }

    private static final Path RETAINED = Path.of("..", "1-伺服器")
            .toAbsolutePath().normalize();
    private static final Path GIVE = RETAINED.resolve(
            "src/com/lineage/server/clientpackets/C_GiveItem.java");
    private static final Path NPC_INSTANCE = RETAINED.resolve(
            "src/com/lineage/server/model/Instance/L1NpcInstance.java");
    private static final Path DATABASE = RETAINED.resolve("db/380_db.sql");

    @Test
    void retainedSourcesAndSelectedDatabaseRemainPinned() throws Exception {
        assertEquals(ClassicWorldStaticNpcInventories.RETAINED_GIVE_HANDLER_SHA256,
                sha256(GIVE));
        assertEquals(ClassicWorldStaticNpcInventories.RETAINED_NPC_INSTANCE_SHA256,
                sha256(NPC_INSTANCE));
        assertEquals(ClassicWorldStaticNpcInventories.SELECTED_DATABASE_SHA256,
                sha256(DATABASE));
    }

    @Test
    void receiverDomainAndTransferOnGetItemOrderRemainExact() throws Exception {
        String give = readLegacy(GIVE);
        String receivers = method(give,
                "private final static String receivableImpls[]",
                "private boolean isNpcItemReceivable");
        ordered(receivers, "\"L1Npc\"", "\"L1Monster\"", "\"L1Guardian\"",
                "\"L1Teleporter\"", "\"L1Guard\"");
        assertEquals(5, receivers.lines().filter(line -> line.contains("\"L1")).count());
        ordered(give,
                "isNpcItemReceivable(target.getNpcTemplate())",
                "targetInv.checkAddItem(item, count)",
                "inv.tradeItem(item, count, targetInv)",
                "target.onGetItem(getItem)");

        String npc = readLegacy(NPC_INSTANCE);
        String onGet = method(npc,
                "public void onGetItem(", "public void approachPlayer(");
        ordered(onGet, "this.refineItem()", "this.getInventory().shuffle()",
                "this.setDigestItem(item)");
    }

    @Test
    void importedStaticAndPromotedReceiverPopulationsStayExact() {
        List<ClassicWorldNpcCatalog.StaticNpc> staticReceivers =
                ClassicWorldMapCatalog.supportedMapIds().stream()
                        .flatMap(mapId -> ClassicWorldNpcCatalog.all(mapId).stream())
                        .filter(ClassicWorldStaticNpcInventories::staticReceiver)
                        .toList();
        assertEquals(ClassicWorldStaticNpcInventories.STATIC_RECEIVER_POPULATION,
                staticReceivers.size());
        Map<String, Long> implementations = staticReceivers.stream()
                .collect(Collectors.groupingBy(
                        ClassicWorldNpcCatalog.StaticNpc::implementation,
                        Collectors.counting()));
        assertEquals(Map.of("L1Npc", 31L, "L1Monster", 2L,
                "L1Teleporter", 27L, "L1Guard", 66L), implementations);
        assertEquals(54, ClassicWorldGuardianCatalog.all().size());
        assertEquals(190, ClassicWorldGuardCatalog.all().size());
        assertTrue(ClassicWorldGuardianCatalog.all().stream()
                .allMatch(ClassicWorldGuardianCatalog::isGuardian));
        assertTrue(ClassicWorldGuardCatalog.all().stream()
                .allMatch(ClassicWorldGuardCatalog::isGuard));
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
