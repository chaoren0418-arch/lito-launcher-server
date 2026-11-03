package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.security.MessageDigest;
import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** Complete selected-source contract for the closed ant-cave component. */
final class ClassicWorldAntCaveCatalogTest {
    private static final List<Integer> MAP_IDS =
            List.of(43, 44, 45, 46, 47, 48, 49, 50, 51, 541, 542, 543);
    private static final Map<Integer, int[]> BOUNDS = Map.ofEntries(
            Map.entry(43, new int[] {32704, 32831, 32704, 32831}),
            Map.entry(44, new int[] {32704, 32831, 32704, 32831}),
            Map.entry(45, new int[] {32704, 32831, 32704, 32831}),
            Map.entry(46, new int[] {32704, 32831, 32704, 32831}),
            Map.entry(47, new int[] {32704, 32831, 32704, 32831}),
            Map.entry(48, new int[] {32704, 32831, 32704, 32831}),
            Map.entry(49, new int[] {32704, 32831, 32704, 32831}),
            Map.entry(50, new int[] {32704, 32831, 32704, 32831}),
            Map.entry(51, new int[] {32640, 32831, 32704, 32895}),
            Map.entry(541, new int[] {32640, 32831, 32768, 32895}),
            Map.entry(542, new int[] {32640, 32831, 32768, 32895}),
            Map.entry(543, new int[] {32704, 32895, 32704, 32895}));
    private static final Map<Integer, String> MAP_RESOURCES = Map.ofEntries(
            Map.entry(43, "3B0C65065DB526B77BD93795BDF6926EFEADD555D9ED5894B95460B5F7573695"),
            Map.entry(44, "C75B69955F49820520DAC16EE1967D376F7F31F065B3E3410FE24E5CB2A548E7"),
            Map.entry(45, "D74A790FD782B33259B38EAA393B792C6E683BC2BC9964A294AF6E0FF5592FC2"),
            Map.entry(46, "5B98055EEA8059E678FA30DE40A194612ADE716E86DC167F46718F97246B0474"),
            Map.entry(47, "1FF19198618C6FC2E339AD9E555CDB80B2D7AD666DA15DF077DEE2BB535BAED3"),
            Map.entry(48, "4F9F8FFB7ADB20D31E0F9BA62F8BE53EDF8E8B4D26CCB60AC68031CE4FE189A4"),
            Map.entry(49, "33BA880A1E7452AD8280E7559526BEBB878026F2AED892D09AFB8882461B5BA1"),
            Map.entry(50, "616B6D857A2D3FB3631B55176169508B9AE17814D6EEE992DDBE0881313CF42A"),
            Map.entry(51, "FABC1F7CF34FAC8FB4DAFF12486B9ECD61461D63EB27D5A9B46856705760AD4E"),
            Map.entry(541, "61D6BB1E01C8A4857D0789C9CF1737F2CD3F40E896DC96C137E4F84D334E1CF3"),
            Map.entry(542, "FC8C34EA86B981DB7A2F57345F6A478728AA0C6CB80D22C3675569C65769F1A4"),
            Map.entry(543, "819B5292C0B70C274D20E1AC1BB6D4F77C4FF712D7CA58B13C967E980C8B79EB"));
    private static final Map<Integer, String> NPC_RESOURCES = Map.ofEntries(
            Map.entry(43, "A19C27EDBF359CA526598A787CE9B8B6E4832589CF9A16D6F0073F04B49442D9"),
            Map.entry(44, "6FF209E85FE08D31C20A9297FC267FC00E5F5B8C422967ED46292DEBE4EC5F2B"),
            Map.entry(45, "57150D42865E5BC98E43C8D78D0F549F62A0FBDF40FA7EDA52468FEA91DB5853"),
            Map.entry(46, "AA749626BC366B4CB62EFF466D713D67EC28A76EC0EA9F8C4C5BB1FD4B743794"),
            Map.entry(47, "3FCCFC64949DB4B8A8845F3D62D5608D3CC636574673EECCDDB12255C3DA3C59"),
            Map.entry(48, "7FCD2098AFD6C670B1B38DA42115665F03AD55C06483F45C7F6CE1E09D70C6DD"),
            Map.entry(49, "A56A65DAA1DD9CE1B8EFE99286ED9402A04EBD1A6090ABC91BEF63B61BFC8C2B"),
            Map.entry(50, "1A1C579D355EB68856F470D61757A625D6A8DEADF1057B85543402FB370C85B6"),
            Map.entry(51, "19194645B15F3D5E53FD1D8228308D1BB24A9AC8FD06A79B05B3769C647B325D"),
            Map.entry(541, "047B9FAD4953A49AE00A3BBB84AA0B96D782A716E8B05C302D49DBE0C6F39BC2"),
            Map.entry(542, "78EB46A33A3F4889CCEE5AA89CE922691A44B8F337EB63410DD28F7F33B4544E"),
            Map.entry(543, "1FB258087C3D375B591F0C65017BCC60AF610273966DBBD6A6BFC4CCF10F3DE7"));
    private static final Map<Integer, String> MONSTER_RESOURCES = Map.ofEntries(
            Map.entry(43, "E3D27B1FC27856BF87DA5C21FD26A9BCF76BF9E1E6B03EEC43BAA7A78FC33857"),
            Map.entry(44, "E52B378015F0ECCAF6C728F8923BDEE35D9E78E32D8A3F4833FA92D50EADC48D"),
            Map.entry(45, "688EBF1E3A39E3FE5B26A93A08A4EB9372EB2EBA9BB382F22D0B8E0868511A96"),
            Map.entry(46, "AF29A38FAB7010A5F4BAEB62C7247E76D3F2AD749762759AEFE5FB736061C322"),
            Map.entry(47, "8EB4E5D039F13F7BEDAE17BEE5E2907E0AC5F7EE9D16F5FA550444C20557CB25"),
            Map.entry(48, "9668FB30CD8C8E90A579034083FCA78D0672FA507B7804B786268E20D3AA664C"),
            Map.entry(49, "EBD89EED6A84F212AC3DD6B4062D6FD60A58C4C13A219616C2B924315A59BC46"),
            Map.entry(50, "BA4CD17AD97A3F829AD27335BFDC024D37ACCBD665E6DC2D9B3489304F4065AF"),
            Map.entry(51, "2EA28ECAA939B8F3E836340D5D159B26FE46D8FD8E7BB13FB36162607D18A601"),
            Map.entry(541, "E8C1373FF9B901DCF4DE63A4D26FE406C8B4EF66DB132A664070833978A79476"),
            Map.entry(542, "001AC7B746B57860E65695B2DB6A412DF9CA9E8AD6DAC42C203761A86FF11722"),
            Map.entry(543, "99B21CB2B507934B25A2D5113791CF66F01B59FC6EDDBA42D41761A54504FE19"));
    private static final Map<Integer, String> DROP_RESOURCES = Map.ofEntries(
            Map.entry(43, "9E5616C18F56D105A9D82A54B4CF355867717C773D9D80A23EC06F210B554E2F"),
            Map.entry(44, "7C2A3FD2988320F9E5263AC56DCF42FF5FAB01C30875FE420AD9D7BF15252F85"),
            Map.entry(45, "0AE938D8B26849764CF27992C56A1F9AEC001123824AD0181FB903A2EA8F5288"),
            Map.entry(46, "E5B409904697D8EE20BEC3DBEE5441F2C369A9C9DB0338CCE6EECB0AE4F22F43"),
            Map.entry(47, "FC69FFCC2CEFCD7DA6F8DB1B5086E4BA88ECA7C3C8386F82C19C1D7E077234F2"),
            Map.entry(48, "AF23357BA880A368898966CEEF4AD90DFCF36D5ABD00A424A9DFBE8A70A8AC7F"),
            Map.entry(49, "5D7717B35435FF4A40C61ABBC6A58F69D42B3B980BA7FB39F5F7D54531D29975"),
            Map.entry(50, "912DB5B89FE529F31259CD6BF5F53FF5517DF3BB316F5BB60AAEA27FE89CD548"),
            Map.entry(51, "547F589C350117D648939AB1543F8F8DEC3FCDF75611CEA72EDBF10A03B192CB"),
            Map.entry(541, "7974D05D760A895B68D3A897F2FD1632C04DCD7047538E5AE94850F34324195F"),
            Map.entry(542, "DCCD70B57E649821EE4A68C43D3C39CC0AC5F605890E13E788F1A63218600E17"),
            Map.entry(543, "DFEC73272D2FCA471548C221C9D8734F09DEB81027707A27C5829B9710ED5F79"));

    @Test
    void selectedMapsKeepExactBoundsFlagsCollisionAndResources() throws Exception {
        assertEquals(MAP_IDS, ClassicWorldMapCatalog.ANT_CAVE_MAP_IDS);
        assertEquals("51D221D9B22B3B13897FABEBC420207E73560AC22A7706DF3F2FB44B1119A7A9",
                ClassicWorldMapCatalog.ANT_CAVE_FLAGS_SOURCE_SHA256);
        for (int mapId : MAP_IDS) {
            ClassicWorldMapCatalog.ClassicMap map =
                    ClassicWorldMapCatalog.forMap(mapId);
            int[] bounds = BOUNDS.get(mapId);
            assertEquals(bounds[0], map.startX());
            assertEquals(bounds[1], map.endX());
            assertEquals(bounds[2], map.startY());
            assertEquals(bounds[3], map.endY());
            assertEquals(bounds[1] - bounds[0] + 1, map.width());
            assertEquals(bounds[3] - bounds[2] + 1, map.height());
            assertEquals(ClassicWorldMapCatalog.ANT_CAVE_MAP_SOURCE_SHA256.get(mapId),
                    map.sourceSha256());
            assertTrue(map.allowsEscape());
            assertTrue(map.allowsPetRecall());
            assertEquals(MAP_RESOURCES.get(mapId),
                    resourceHash("/maps/" + mapId + ".l38.gz"));
        }
    }

    @Test
    void selectedRoutesKeepAllFiftyEightRowsAndReachEveryFloor() {
        assertEquals("BD139F15A7FFCD1E87E8C65FF6F1FE3BFDC27660EEB5214ACB93F46AD928C9A8",
                ClassicWorldDungeonCatalog.ANT_CAVE_SOURCE_SHA256);
        assertEquals("A4282AF6B9A3A6FA40B8FF47DB968165694EC9689A9045A3B8E0C54D843459FF",
                ClassicWorldDungeonCatalog.ANT_CAVE_NORMALIZED_SOURCE_SHA256);
        List<ClassicWorldDungeonCatalog.Route> routes =
                ClassicWorldDungeonCatalog.all().stream()
                        .filter(route -> route.routeId() >= 69 && route.routeId() <= 126)
                        .toList();
        assertEquals(58, routes.size());
        for (int index = 0; index < routes.size(); index++) {
            ClassicWorldDungeonCatalog.Route route = routes.get(index);
            assertEquals(69 + index, route.routeId());
            assertEquals(0, route.requiredItemCatalogId());
            assertFalse(route.scheduledPirateShip());
            assertTrue(route.sourceMapId() == 4 || MAP_IDS.contains(route.sourceMapId()));
            assertTrue(MAP_IDS.contains(route.destinationMapId()));
            // A retained dungeon trigger may occupy a non-standing overlay cell;
            // only its authoritative landing must be a walkable standing cell.
            assertTrue(ClassicWorldMapCatalog.forMap(route.sourceMapId())
                    .contains(route.sourceX(), route.sourceY()));
            assertTrue(ClassicWorldMapCatalog.forMap(route.destinationMapId())
                    .standingPassable(route.destinationX(), route.destinationY()));
        }

        Set<Integer> reached = new HashSet<>();
        ArrayDeque<Integer> pending = new ArrayDeque<>();
        reached.add(4);
        pending.add(4);
        while (!pending.isEmpty()) {
            int source = pending.removeFirst();
            routes.stream().filter(route -> route.sourceMapId() == source)
                    .map(ClassicWorldDungeonCatalog.Route::destinationMapId)
                    .filter(reached::add).forEach(pending::addLast);
        }
        assertTrue(reached.containsAll(MAP_IDS));
        assertEquals(43, ClassicWorldDungeonCatalog.find(4, 32755, 33208)
                .destinationMapId());
        assertEquals(541, ClassicWorldDungeonCatalog.find(48, 32778, 32801)
                .destinationMapId());
        assertEquals(543, ClassicWorldDungeonCatalog.find(51, 32679, 32848)
                .destinationMapId());
        assertEquals(51, ClassicWorldDungeonCatalog.find(543, 32874, 32749)
                .destinationMapId());
    }

    @Test
    void selectedStaticActorsKeepNativeCorpseAndGatekeeperPages() throws Exception {
        for (int mapId : MAP_IDS) {
            assertEquals(NPC_RESOURCES.get(mapId), resourceHash(
                    "/world/map" + mapId + "-static-npcs.lwn.gz"));
        }
        List<ClassicWorldNpcCatalog.StaticNpc> corpses =
                ClassicWorldNpcCatalog.all(43);
        assertEquals(3, corpses.size());
        assertNpc(corpses.get(0), 1283, 80143, 32785, 32748, 0,
                "$5652", 6901, "wcorpse1", false);
        assertNpc(corpses.get(1), 1284, 80144, 32808, 32725, 0,
                "$5652", 6903, "wcorpse4", false);
        assertNpc(corpses.get(2), 1285, 80145, 32728, 32782, 0,
                "$5652", 6905, "wcorpse7", false);

        List<ClassicWorldNpcCatalog.StaticNpc> ants =
                ClassicWorldNpcCatalog.all(45);
        assertEquals(2, ants.size());
        assertNpc(ants.get(0), 1132, 70782, 32775, 32728, 5,
                "$2014", 2437, "", true);
        assertNpc(ants.get(1), 1133, 70779, 32740, 32730, 5,
                "$2015", 2438, "antsn", false);
        assertTrue(ants.get(1).fixedMerchant());
        assertEquals("antsn", ants.get(1).initialHtml(0));
        assertTrue(ClassicWorldNpcCatalog.all(44).isEmpty());
        assertTrue(ClassicWorldNpcCatalog.all(543).isEmpty());
    }

    @Test
    void selectedMonstersKeepPopulationBossHabitatAndResources() throws Exception {
        int[] definitions = {1, 1, 1, 1, 1, 1, 6, 6, 4, 1, 1, 5};
        int[] populations = {30, 35, 35, 35, 35, 35, 110, 100, 70, 1, 1, 5};
        int total = 0;
        for (int index = 0; index < MAP_IDS.size(); index++) {
            int mapId = MAP_IDS.get(index);
            List<ClassicWorldMonsterCatalog.SpawnDefinition> rows =
                    ClassicWorldMonsterCatalog.definitions(mapId);
            assertEquals(definitions[index], rows.size());
            assertEquals(populations[index], rows.stream()
                    .mapToInt(ClassicWorldMonsterCatalog.SpawnDefinition::count).sum());
            assertEquals(MONSTER_RESOURCES.get(mapId), resourceHash(
                    "/world/map" + mapId + "-monsters.lwm.gz"));
            total += populations[index];
        }
        assertEquals(492, total);
        assertEquals(492, ClassicWorldMonsterCatalog.ANT_CAVE_POPULATION);

        int[][] habitat = {{906, 32848, 32855}, {907, 32747, 32776},
                {908, 32858, 32798}, {909, 32771, 32856},
                {910, 32768, 32739}};
        List<ClassicWorldMonsterCatalog.SpawnDefinition> rows =
                ClassicWorldMonsterCatalog.definitions(543);
        for (int index = 0; index < rows.size(); index++) {
            ClassicWorldMonsterCatalog.SpawnDefinition row = rows.get(index);
            assertEquals(habitat[index][0], row.spawnId());
            assertEquals(habitat[index][1], row.locX());
            assertEquals(habitat[index][2], row.locY());
            assertEquals(45952, row.template().npcId());
            assertEquals("$5285", row.template().nameId());
            assertEquals(6290, row.template().gfxId());
            assertEquals(44, row.template().level());
            assertEquals(4400, row.template().hp());
            assertEquals(229, row.template().mp());
            assertEquals(1937, row.template().experience());
            assertEquals(880, row.template().atkspeed());
            assertFalse(row.template().boss());
        }
    }

    @Test
    void selectedDropsAndItemsKeepExactMapContracts() throws Exception {
        int[] dropCounts = {0, 0, 0, 0, 0, 0, 51, 51, 36, 7, 7, 7};
        int[] itemCounts = {0, 0, 0, 0, 0, 0, 17, 17, 16, 7, 7, 7};
        for (int index = 0; index < MAP_IDS.size(); index++) {
            int mapId = MAP_IDS.get(index);
            assertEquals(dropCounts[index], ClassicWorldDropCatalog.dropCount(mapId));
            assertEquals(itemCounts[index], ClassicWorldDropCatalog.items(mapId).size());
            assertEquals(DROP_RESOURCES.get(mapId), resourceHash(
                    "/world/map" + mapId + "-drops.lwd.gz"));
            for (ClassicWorldDropCatalog.ItemWire item
                    : ClassicWorldDropCatalog.items(mapId)) {
                assertNotNull(item.inventoryDefinition());
                assertNotNull(InventoryCatalog.find(item.itemId()));
            }
        }
        for (int mapId : List.of(541, 542, 543)) {
            assertEquals(7, ClassicWorldDropCatalog.rules(mapId, 45952).size());
        }
    }

    @Test
    void selectedGetbackRowsAndAbsentBossRowsKeepNativeSemantics() {
        assertEquals("5E82372EFB4784CACFCEAA7FBA86513FEE86E0CD07923A47ABE046E8A81CE2BE",
                ClassicWorldGetbackCatalog.ANT_CAVE_SOURCE_SHA256);
        for (int mapId = 43; mapId <= 51; mapId++) {
            assertEquals(new ClassicWorldGetbackCatalog.Destination(
                            4, 33071, 33402, 2),
                    ClassicWorldGetbackCatalog.resolve(mapId, 32780, 32780, 1, 0));
        }
        for (int mapId : List.of(541, 542, 543)) {
            assertEquals(new ClassicWorldGetbackCatalog.Destination(
                            4, 33089, 33397, 2),
                    ClassicWorldGetbackCatalog.resolve(mapId, 32780, 32780, 1, 0));
        }
    }

    private static void assertNpc(
            ClassicWorldNpcCatalog.StaticNpc npc, int spawnId, int npcId,
            int x, int y, int heading, String nameId, int gfxId,
            String html, boolean changesHead) {
        assertEquals(spawnId, npc.spawnId());
        assertEquals(ClassicWorldNpcCatalog.objectId(spawnId), npc.objectId());
        assertEquals(npcId, npc.npcId());
        assertEquals(x, npc.x());
        assertEquals(y, npc.y());
        assertEquals(heading, npc.heading());
        assertEquals(nameId, npc.nameId());
        assertEquals("L1Merchant", npc.implementation());
        assertEquals(gfxId, npc.gfxId());
        assertEquals(html, npc.normalAction());
        assertEquals(html, npc.chaoticAction());
        assertEquals(changesHead, npc.changeHead());
    }

    private static String resourceHash(String name) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        try (InputStream in = ClassicWorldAntCaveCatalogTest.class
                .getResourceAsStream(name)) {
            assertNotNull(in);
            digest.update(in.readAllBytes());
        }
        return HexFormat.of().withUpperCase().formatHex(digest.digest());
    }
}
