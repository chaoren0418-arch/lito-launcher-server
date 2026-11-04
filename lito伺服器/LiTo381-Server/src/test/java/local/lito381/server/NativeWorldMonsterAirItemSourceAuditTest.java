package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** Locks the retained damaged-fly potion/food landing and item-AI order. */
final class NativeWorldMonsterAirItemSourceAuditTest {
    @BeforeAll
    static void assumeLocalResourcesPresent() {
        LocalResources.assumeLineageSources();
    }

    private static final Path RETAINED = Path.of("../1-伺服器");

    @Test
    void retainedSearchItemFromAirOrderAndTypesStayExact() throws Exception {
        Path npcPath = RETAINED.resolve(
                "src/com/lineage/server/model/Instance/L1NpcInstance.java");
        assertEquals(
                "72E8DC365DAAC1B27D3F2C92672FCC77FF512825C4B6EAF9C96A6A87EA80EAD2",
                sha256(npcPath));
        String npc = Files.readString(npcPath, StandardCharsets.UTF_8)
                .replace("\r\n", "\n");
        String method = slice(npc, "public void searchItemFromAir()",
                "public static void shuffle");
        ordered(method,
                "World.get().getVisibleObjects(this)",
                "Math.random() * gInventorys.size()",
                "item.getItem().getType() == 6",
                "item.getItem().getType() == 7",
                "checkAddItem(item, item.getCount()) == L1Inventory.OK",
                "getHiddenStatus() == HIDDEN_STATUS_FLY",
                "setHiddenStatus(HIDDEN_STATUS_NONE)",
                "ActionCodes.ACTION_Movedown",
                "setStatus(0)",
                "new S_NPCPack(this)",
                "onNpcAI()",
                "startChat(CHAT_TIMING_HIDE)",
                "this._targetItem = item",
                "this._targetItemList.add(this._targetItem)");

        String approach = slice(npc, "public void approachPlayer(",
                "public void appearOnGround(");
        ordered(approach,
                "case HIDDEN_STATUS_FLY:",
                "getCurrentHp() == this.getMaxHp()",
                "getTileLineDistance(this.getLocation()) <= 1",
                "this.appearOnGround(pc)",
                "else",
                "this.searchItemFromAir()");
    }

    @Test
    void retainedItemTypeAndNpcAiPriorityStayExact() throws Exception {
        Path itemTable = RETAINED.resolve(
                "src/com/lineage/server/datatables/ItemTable.java");
        Path npcAi = RETAINED.resolve(
                "src/com/lineage/server/model/Instance/NpcAI.java");
        assertEquals(
                "539483FF27709DD0A2350776D28BE0DAF3E255B61933376C0532AB87C8886E3D",
                sha256(itemTable));
        assertEquals(
                "F31A67C15C4C8473CC236E4E74D79B7CBE625DA807F0E40BEE715AC7640EFA41",
                sha256(npcAi));
        String types = Files.readString(itemTable, StandardCharsets.UTF_8);
        assertTrue(types.contains("_etcItemTypes.put(\"potion\", 6)"));
        assertTrue(types.contains("_etcItemTypes.put(\"food\", 7)"));
        assertTrue(types.contains(
                "item.setType((_etcItemTypes.get(rs.getString(\"item_type\"))).intValue())"));

        String ai = Files.readString(npcAi, StandardCharsets.UTF_8)
                .replace("\r\n", "\n");
        String process = slice(ai, "private boolean AIProcess()",
                "return false; // NPC AI");
        ordered(process, "_npc.searchTarget()", "_npc.onItemUse()",
                "_npc.checkTargetItem()", "_npc.searchTargetItem()",
                "_npc.onTargetItem()");
    }

    @Test
    void implementationCarriesNativeBranchAndWireOrder() throws Exception {
        Path root = Path.of("src/main/java/local/lito381/server");
        String population = Files.readString(
                root.resolve("ClassicWorldMonsterPopulation.java"),
                StandardCharsets.UTF_8);
        ordered(population,
                "synchronized AirItemLanding searchItemFromAir(",
                "NativeEtcItemTypeCatalog.airbornePickupEligible(",
                "actor.hiddenStatus = HIDDEN_STATUS_NONE",
                "actor.actorStatus = 0",
                "actor.targetItemObjectId = item.objectId()",
                "return new AirItemLanding(");
        String hub = Files.readString(root.resolve("GameHub.java"),
                StandardCharsets.UTF_8);
        ordered(hub, "population.revealFlyNear(",
                "population.damagedFlyingInScreen(",
                "population.searchItemFromAir(",
                "serverClassicWorldMonsterAirItemLanding(",
                "population.revealDamagedIceInScreen(");
        String session = Files.readString(root.resolve("ClientSession.java"),
                StandardCharsets.UTF_8);
        String sender = slice(session,
                "void serverClassicWorldMonsterAirItemLanding(",
                "void serverClassicWorldMonsterRemoved(");
        ordered(sender, "Protocol.objectAction(",
                "landing.actionId()", "sendClassicWorldMonster(");

        assertEquals(126, NativeEtcItemTypeCatalog.POTION_COUNT);
        assertEquals(98, NativeEtcItemTypeCatalog.FOOD_COUNT);
        assertEquals(
                "DA185BCA20CC254CB175EA2C367A2F5270514F4957B3AC99343C6101A2593766",
                NativeEtcItemTypeCatalog.SOURCE_SHA256);
    }

    private static String slice(String source, String start, String end) {
        int from = source.indexOf(start);
        int to = source.indexOf(end, from + start.length());
        assertTrue(from >= 0 && to > from,
                () -> "missing source slice " + start + " .. " + end);
        return source.substring(from, to);
    }

    private static void ordered(String source, String... fragments) {
        int position = -1;
        for (String fragment : fragments) {
            int next = source.indexOf(fragment, position + 1);
            assertTrue(next >= 0,
                    () -> "missing/out-of-order fragment " + fragment);
            position = next;
        }
    }

    private static String sha256(Path path) throws Exception {
        return HexFormat.of().withUpperCase().formatHex(
                MessageDigest.getInstance("SHA-256")
                        .digest(Files.readAllBytes(path)));
    }
}
