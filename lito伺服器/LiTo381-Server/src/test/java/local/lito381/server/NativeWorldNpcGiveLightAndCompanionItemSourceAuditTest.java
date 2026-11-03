package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
import org.junit.jupiter.api.Test;

/** Hash-pinned proof for C_GIVE light and pet/summon onGetItem semantics. */
final class NativeWorldNpcGiveLightAndCompanionItemSourceAuditTest {
    private static final Path RETAINED = Path.of("..", "1-伺服器")
            .toAbsolutePath().normalize();
    private static final Path GIVE = source(
            "server/clientpackets/C_GiveItem.java");
    private static final Path CHARACTER = source(
            "server/model/L1Character.java");
    private static final Path LIGHT_ITEM = source(
            "data/item_etcitem/Light.java");
    private static final Path LIGHT_PACKET = source(
            "server/serverpackets/S_Light.java");
    private static final Path PET = source(
            "server/model/Instance/L1PetInstance.java");
    private static final Path SUMMON = source(
            "server/model/Instance/L1SummonInstance.java");
    private static final Path NPC = source(
            "server/model/Instance/L1NpcInstance.java");

    @Test
    void retainedSourcesRemainPinned() throws Exception {
        assertEquals("21F4BD7FDAE6D6F30284AE0834E105D5158A82FF9C437B369D319D4178758212",
                sha256(GIVE));
        assertEquals("56DAD7E55D73924D631D28EE04A56413536F3921BE13D732607C412F24148C29",
                sha256(CHARACTER));
        assertEquals("105F6AB37AA84A4069DC11D312AE5FAA34BE39D0FA30A9E60D9B1281D044443E",
                sha256(LIGHT_ITEM));
        assertEquals("D633C820C10EC2EDA64381BE17EA3ADFBE00E256B0C212CF242414FED2815DD9",
                sha256(LIGHT_PACKET));
        assertEquals("C1AFF14740C290FB91E5C0F5840818372E61F88D2FE8129ADC5712452A899CA3",
                sha256(PET));
        assertEquals("BC0DAB6D576CF7AC91830E49A543792814AA69893364EF5CD2952856966AEC9D",
                sha256(SUMMON));
        assertEquals("72E8DC365DAAC1B27D3F2C92672FCC77FF512825C4B6EAF9C96A6A87EA80EAD2",
                sha256(NPC));
    }

    @Test
    void givePublishesTargetThenGiverLightAfterSpecialOnGetItem() throws Exception {
        String give = readLegacy(GIVE);
        ordered(give, "inv.tradeItem(item, count, targetInv)",
                "target.onGetItem(getItem)", "target.turnOnOffLight()",
                "pc.turnOnOffLight()");

        String character = method(readLegacy(CHARACTER),
                "public void turnOnOffLight()", "public int getOwnLightSize()");
        ordered(character, "lightSize = npc.getLightSize()",
                "item.isNowLighting()", "this.hasSkillEffect(LIGHT)",
                "pc.sendPackets(new S_Light", "this.broadcastPacketAll(new S_Light");

        String packet = readLegacy(LIGHT_PACKET);
        ordered(packet, "this.writeC(S_OPCODE_LIGHT)",
                "this.writeD(objid)", "this.writeC(type)");
    }

    @Test
    void lightingStateIsIndependentFromEquipmentState() throws Exception {
        String light = readLegacy(LIGHT_ITEM);
        ordered(light, "item.isNowLighting()", "item.setNowLighting(false)",
                "item.setNowLighting(true)", "pc.sendPackets(new S_ItemName(item))");
        assertFalse(light.contains("setEquipped"));
    }

    @Test
    void petAndSummonOverrideUseReceivedPotionClassImmediately() throws Exception {
        for (Path source : List.of(PET, SUMMON)) {
            String onGet = method(readLegacy(source),
                    "public void onGetItem(final L1ItemInstance item)",
                    "protected void afterCurrentHpMutation");
            ordered(onGet, "this.setDigestItem(item)",
                    "Arrays.sort(healPotions)", "Arrays.sort(haestPotions)",
                    "Arrays.binarySearch(healPotions",
                    "this.getCurrentHp() != this.getMaxHp()",
                    "this.useItem(USEITEM_HEAL, 100)",
                    ".binarySearch(haestPotions",
                    "this.useItem(USEITEM_HASTE, 100)");
            assertFalse(onGet.contains("super.onGetItem"));
        }
    }

    @Test
    void inheritedNpcUseItemPriorityAndDurationsRemainExact() throws Exception {
        String use = method(readLegacy(NPC),
                "public void useItem(final int type, final int chance)",
                "public synchronized void setDirectionMoveSrc");
        ordered(use, "is_decay_potion()", "POTION_OF_GREATER_HEALING, 1",
                "this.useHealPotion(75, 197)",
                "POTION_OF_EXTRA_HEALING, 1", "this.useHealPotion(45, 194)",
                "POTION_OF_HEALING, 1", "this.useHealPotion(15, 189)",
                "this.hasSkillEffect(STATUS_HASTE)",
                "B_POTION_OF_GREATER_HASTE_SELF, 1", "this.useHastePotion(2100)",
                "POTION_OF_GREATER_HASTE_SELF, 1", "this.useHastePotion(1800)",
                "B_POTION_OF_HASTE_SELF, 1", "this.useHastePotion(350)",
                "POTION_OF_HASTE_SELF, 1", "this.useHastePotion(300)");
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
