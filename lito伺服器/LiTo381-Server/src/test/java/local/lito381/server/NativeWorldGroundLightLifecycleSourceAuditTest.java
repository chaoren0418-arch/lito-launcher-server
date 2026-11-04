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

/** Hash-pinned proof for player C_DROP/C_GET and NPC pickup light semantics. */
final class NativeWorldGroundLightLifecycleSourceAuditTest {
    @BeforeAll
    static void assumeLocalResourcesPresent() {
        LocalResources.assumeLineageSources();
    }

    private static final Path RETAINED = Path.of("..", "1-伺服器")
            .toAbsolutePath().normalize();
    private static final Path DROP = source("server/clientpackets/C_DropItem.java");
    private static final Path PICKUP = source("server/clientpackets/C_PickUpItem.java");
    private static final Path NPC = source("server/model/Instance/L1NpcInstance.java");

    @Test
    void retainedSourcesRemainPinned() throws Exception {
        assertEquals("74E111ECA656C740A59D0433C05698E734F699FD9199C006283484DE1F553752",
                sha256(DROP));
        assertEquals("295956C139993CB641392324B3465C6D19645A31E500101114CFC618D63CF09C",
                sha256(PICKUP));
        assertEquals("72E8DC365DAAC1B27D3F2C92672FCC77FF512825C4B6EAF9C96A6A87EA80EAD2",
                sha256(NPC));
    }

    @Test
    void playerDropRecomputesLightAfterInventoryTransfer() throws Exception {
        String drop = method(readLegacy(DROP), "public void start(", "public String getType()");
        ordered(drop, "if (item.isEquipped())",
                "pc.getInventory().tradeItem(item, dropCount",
                "pc.turnOnOffLight()");
    }

    @Test
    void playerPickupPublishesGainThenLightHeadingAndAction() throws Exception {
        String pickup = method(readLegacy(PICKUP), "public void start(",
                "public String getType()");
        ordered(pickup, "final int pickupX = item.getX()",
                "final int pickupY = item.getY()",
                "groundInventory.tradeItem(item, countToPickup",
                "new S_ServerMessage(403", "pc.turnOnOffLight()",
                "pc.setHeading(pc.targetDirection(pickupX, pickupY))",
                "new S_ChangeHeading(pc)", "new S_DoActionGFX(pc.getId()",
                "ActionCodes.ACTION_Pickup");
    }

    @Test
    void autonomousNpcPickupIntentionallySkipsLightAndOnlyRunsOnGetItem()
            throws Exception {
        String pickup = method(readLegacy(NPC), "public synchronized void pickupTargetItem",
                "public boolean noTarget()");
        ordered(pickup, "groundInv.tradeItem(targetItem",
                "//turnOnOffLight();", "onGetItem(item)");
        assertTrue(pickup.contains("//turnOnOffLight();"));
    }

    private static Path source(String relative) {
        return RETAINED.resolve("src/com/lineage/" + relative);
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
