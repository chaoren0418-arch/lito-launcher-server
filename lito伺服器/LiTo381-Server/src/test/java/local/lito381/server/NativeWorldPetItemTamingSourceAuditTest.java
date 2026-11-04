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

/** Hash-pinned source proof for retained C_GIVE item-based pet taming. */
final class NativeWorldPetItemTamingSourceAuditTest {
    @BeforeAll
    static void assumeLocalResourcesPresent() {
        LocalResources.assumeLineageSources(
                Path.of("../1-伺服器/db/380_db.sql"),
                Path.of("../1-伺服器/src/com/lineage/server/clientpackets/C_GiveItem.java"),
                Path.of("../1-伺服器/src/com/lineage/server/datatables/PetTypeTable.java"),
                Path.of("../1-伺服器/src/com/lineage/server/model/Instance/L1PetInstance.java"),
                Path.of("../1-伺服器/src/com/lineage/server/templates/L1PetType.java"));
    }

    private static final Path RETAINED = Path.of("..", "1-伺服器")
            .toAbsolutePath().normalize();
    private static final Path GIVE = RETAINED.resolve(
            "src/com/lineage/server/clientpackets/C_GiveItem.java");
    private static final Path PET_TYPES = RETAINED.resolve(
            "src/com/lineage/server/datatables/PetTypeTable.java");
    private static final Path PET_TYPE = RETAINED.resolve(
            "src/com/lineage/server/templates/L1PetType.java");
    private static final Path PET = RETAINED.resolve(
            "src/com/lineage/server/model/Instance/L1PetInstance.java");
    private static final Path DATABASE = RETAINED.resolve("db/380_db.sql");

    @Test
    void retainedTamingSourcesRemainHashPinned() throws Exception {
        assertEquals("21F4BD7FDAE6D6F30284AE0834E105D5158A82FF9C437B369D319D4178758212",
                sha256(GIVE));
        assertEquals("EB35CEACA5B873A0E50B0174DEA4EFA95605D1E00582C246957AF5C81B8D2AFC",
                sha256(PET_TYPES));
        assertEquals("1675A9907EFF7A0F4D3C05301CAE0190D0B1934298750AC4AB6FF3CA53B41538",
                sha256(PET_TYPE));
        assertEquals("C1AFF14740C290FB91E5C0F5840818372E61F88D2FE8129ADC5712452A899CA3",
                sha256(PET));
        assertEquals("DA185BCA20CC254CB175EA2C367A2F5270514F4957B3AC99343C6101A2593766",
                sha256(DATABASE));
    }

    @Test
    void retainedGiveOnGetItemAndTamePetOrderingStayExact() throws Exception {
        String give = readLegacy(GIVE);
        ordered(give,
                "inv.tradeItem(item, count, targetInv)",
                "target.onGetItem(getItem)",
                "PetTypeTable.getInstance()",
                "if ((petType == null) || target.isDead())",
                "getItem.getItemId() == petType.getItemIdForTaming()",
                "this.tamePet(pc, target, petType)");
        String tame = method(give, "private void tamePet(",
                "private void tryEvolvePet(");
        ordered(tame,
                "target instanceof L1PetInstance",
                "target instanceof L1SummonInstance",
                "petType.getChaCost()",
                "pc.canTakePet(chaCost)",
                "new S_ServerMessage(489)",
                "inv.getSize() < L1PcInventory.getMaxInventorySize()",
                "this.isTamePet(target, petType)",
                "petType.getCollarId()",
                "inv.storeItem(collarId, 1)",
                "new L1PetInstance(finalTarget, finalPc, finalPetamu.getId())",
                "pet.initialize()",
                "new S_ItemName(finalPetamu)",
                "new S_ServerMessage(324)");
    }

    @Test
    void retainedHpRateBagTransferAndDatabaseRowsStayPinned() throws Exception {
        String give = readLegacy(GIVE);
        String probability = method(give, "private boolean isTamePet(",
                "//@Override");
        ordered(probability,
                "npc.getMaxHp() / 3 <= npc.getCurrentHp()",
                "petType.getTameRate()",
                "_random.nextInt(10000) < tameRate");

        String pet = readLegacy(PET);
        String constructor = method(pet,
                "public L1PetInstance(final L1NpcInstance target",
                "public L1PetInstance(final int npcid");
        ordered(constructor,
                "this.setCurrentHpDirect(target.getCurrentHp())",
                "this.setCurrentMpDirect(target.getCurrentMp())",
                "this.setInventory(target.getInventory())",
                "target.setInventory(null)",
                "target.deleteMe()",
                "this.setMaster(master)",
                "World.get().storeObject(this)");
        String initialize = method(pet, "public boolean initialize()",
                "public void receiveDamage(");
        assertTrue(initialize.contains(
                "PetTable.get().storeNewPet(this, this.getId(), this._initItemId)"));

        String database = readLegacy(DATABASE);
        assertTrue(database.contains("`TameRate` int(11) NOT NULL DEFAULT 10000"));
        assertTrue(database.contains("`ItemIdForTaming` int(10) NOT NULL"));
        assertTrue(database.contains(
                "INSERT INTO `pettypes` VALUES (45034, '\u7267\u7f8a\u72ac', 40314, 6, 1500, 40057"));
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
