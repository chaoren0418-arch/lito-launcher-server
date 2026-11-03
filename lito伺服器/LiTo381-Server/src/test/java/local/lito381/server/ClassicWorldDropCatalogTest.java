package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.Test;

final class ClassicWorldDropCatalogTest {
    @Test
    void selectedMap4DropAndItemResourcesRemainHashLocked() throws Exception {
        assertEquals(ClassicWorldDropCatalog.MAP4_DROP_COUNT,
                ClassicWorldDropCatalog.dropCount(4));
        assertEquals(ClassicWorldDropCatalog.MAP4_ITEM_COUNT,
                ClassicWorldDropCatalog.items(4).size());
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        try (InputStream in = getClass().getResourceAsStream(
                "/world/map4-drops.lwd.gz")) {
            assertNotNull(in);
            digest.update(in.readAllBytes());
        }
        assertEquals(ClassicWorldDropCatalog.MAP4_RESOURCE_SHA256,
                HexFormat.of().withUpperCase().formatHex(digest.digest()));
        assertTrue(ClassicWorldDropCatalog.rules(3, 45009).isEmpty());
        assertTrue(ClassicWorldDropCatalog.items(3).isEmpty());
    }

    @Test
    void selectedMap303DropAndItemResourcesRemainHashLocked() throws Exception {
        assertEquals(ClassicWorldDropCatalog.MAP303_DROP_COUNT,
                ClassicWorldDropCatalog.dropCount(303));
        assertEquals(ClassicWorldDropCatalog.MAP303_ITEM_COUNT,
                ClassicWorldDropCatalog.items(303).size());
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        try (InputStream in = getClass().getResourceAsStream(
                "/world/map303-drops.lwd.gz")) {
            assertNotNull(in);
            digest.update(in.readAllBytes());
        }
        assertEquals(ClassicWorldDropCatalog.MAP303_RESOURCE_SHA256,
                HexFormat.of().withUpperCase().formatHex(digest.digest()));
        assertFalse(ClassicWorldDropCatalog.rules(303, 45551).isEmpty());
        for (ClassicWorldDropCatalog.ItemWire wire
                : ClassicWorldDropCatalog.items(303)) {
            assertNotNull(wire.inventoryDefinition());
            assertNotNull(InventoryCatalog.find(wire.itemId()));
        }
    }

    @Test
    void selectedMap21DropsRemainHashLockedWithGuaranteedOgreAdena()
            throws Exception {
        assertEquals(ClassicWorldDropCatalog.MAP21_DROP_COUNT,
                ClassicWorldDropCatalog.dropCount(21));
        assertEquals(ClassicWorldDropCatalog.MAP21_ITEM_COUNT,
                ClassicWorldDropCatalog.items(21).size());
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        try (InputStream in = getClass().getResourceAsStream(
                "/world/map21-drops.lwd.gz")) {
            assertNotNull(in);
            digest.update(in.readAllBytes());
        }
        assertEquals(ClassicWorldDropCatalog.MAP21_RESOURCE_SHA256,
                HexFormat.of().withUpperCase().formatHex(digest.digest()));

        List<ClassicWorldDropCatalog.DropRule> ogre =
                ClassicWorldDropCatalog.rules(21, 45183);
        ClassicWorldDropCatalog.DropRule adena = ogre.stream()
                .filter(rule -> rule.itemId() == InventoryCatalog.ADENA)
                .findFirst().orElseThrow();
        assertEquals(61, adena.minimum());
        assertEquals(90, adena.maximum());
        assertEquals(ClassicWorldDropCatalog.CHANCE_SCALE, adena.chance());
        ClassicWorldDropCatalog.RolledItem rolled = ClassicWorldDropCatalog.roll(
                        21, 45183, 1.0, 1.0, 1.0, new Random(0x21381C))
                .stream().filter(item -> item.item().itemId() == InventoryCatalog.ADENA)
                .findFirst().orElseThrow();
        assertTrue(rolled.quantity() >= 61);
        assertTrue(rolled.quantity() <= 90);
    }

    @Test
    void selectedSleepingDragonDropsKeepExactRowsAndGuaranteedGandiAdena()
            throws Exception {
        int[] mapIds = {19, 20};
        int[] dropCounts = {2, 18};
        int[] itemCounts = {2, 14};
        String[] resourceHashes = {
            ClassicWorldDropCatalog.MAP19_RESOURCE_SHA256,
            ClassicWorldDropCatalog.MAP20_RESOURCE_SHA256};
        for (int index = 0; index < mapIds.length; index++) {
            assertEquals(dropCounts[index],
                    ClassicWorldDropCatalog.dropCount(mapIds[index]));
            assertEquals(itemCounts[index],
                    ClassicWorldDropCatalog.items(mapIds[index]).size());
            for (ClassicWorldDropCatalog.ItemWire wire
                    : ClassicWorldDropCatalog.items(mapIds[index])) {
                assertNotNull(wire.inventoryDefinition());
                assertNotNull(InventoryCatalog.find(wire.itemId()));
            }
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            try (InputStream in = getClass().getResourceAsStream(
                    "/world/map" + mapIds[index] + "-drops.lwd.gz")) {
                assertNotNull(in);
                digest.update(in.readAllBytes());
            }
            assertEquals(resourceHashes[index],
                    HexFormat.of().withUpperCase().formatHex(digest.digest()));
        }

        ClassicWorldDropCatalog.DropRule adena = ClassicWorldDropCatalog
                .rules(20, 45150).stream()
                .filter(rule -> rule.itemId() == InventoryCatalog.ADENA)
                .findFirst().orElseThrow();
        assertEquals(45, adena.minimum());
        assertEquals(60, adena.maximum());
        assertEquals(ClassicWorldDropCatalog.CHANCE_SCALE, adena.chance());
        ClassicWorldDropCatalog.RolledItem rolled = ClassicWorldDropCatalog.roll(
                        20, 45150, 1.0, 1.0, 1.0, new Random(0x20381C))
                .stream().filter(item -> item.item().itemId() == InventoryCatalog.ADENA)
                .findFirst().orElseThrow();
        assertTrue(rolled.quantity() >= 45);
        assertTrue(rolled.quantity() <= 60);
    }

    @Test
    void selectedMap32DropsKeepExactRowsAndGuaranteedSkeletonAdena()
            throws Exception {
        assertEquals(ClassicWorldDropCatalog.MAP32_DROP_COUNT,
                ClassicWorldDropCatalog.dropCount(32));
        assertEquals(ClassicWorldDropCatalog.MAP32_ITEM_COUNT,
                ClassicWorldDropCatalog.items(32).size());
        for (ClassicWorldDropCatalog.ItemWire wire
                : ClassicWorldDropCatalog.items(32)) {
            assertNotNull(wire.inventoryDefinition());
            assertNotNull(InventoryCatalog.find(wire.itemId()));
        }
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        try (InputStream in = getClass().getResourceAsStream(
                "/world/map32-drops.lwd.gz")) {
            assertNotNull(in);
            digest.update(in.readAllBytes());
        }
        assertEquals(ClassicWorldDropCatalog.MAP32_RESOURCE_SHA256,
                HexFormat.of().withUpperCase().formatHex(digest.digest()));

        ClassicWorldDropCatalog.DropRule adena = ClassicWorldDropCatalog
                .rules(32, 45269).stream()
                .filter(rule -> rule.itemId() == InventoryCatalog.ADENA)
                .findFirst().orElseThrow();
        assertEquals(50, adena.minimum());
        assertEquals(66, adena.maximum());
        assertEquals(ClassicWorldDropCatalog.CHANCE_SCALE, adena.chance());
        ClassicWorldDropCatalog.RolledItem rolled = ClassicWorldDropCatalog.roll(
                        32, 45269, 1.0, 1.0, 1.0, new Random(0x32381C))
                .stream().filter(item -> item.item().itemId() == InventoryCatalog.ADENA)
                .findFirst().orElseThrow();
        assertTrue(rolled.quantity() >= 50);
        assertTrue(rolled.quantity() <= 66);
    }

    @Test
    void selectedMap30Map31And33DropsKeepExactRowsAndWireItems()
            throws Exception {
        int[] mapIds = {30, 31, 33};
        int[] dropCounts = {ClassicWorldDropCatalog.MAP30_DROP_COUNT,
                ClassicWorldDropCatalog.MAP31_DROP_COUNT,
                ClassicWorldDropCatalog.MAP33_DROP_COUNT};
        int[] itemCounts = {ClassicWorldDropCatalog.MAP30_ITEM_COUNT,
                ClassicWorldDropCatalog.MAP31_ITEM_COUNT,
                ClassicWorldDropCatalog.MAP33_ITEM_COUNT};
        String[] resourceHashes = {ClassicWorldDropCatalog.MAP30_RESOURCE_SHA256,
                ClassicWorldDropCatalog.MAP31_RESOURCE_SHA256,
                ClassicWorldDropCatalog.MAP33_RESOURCE_SHA256};
        for (int index = 0; index < mapIds.length; index++) {
            int mapId = mapIds[index];
            assertEquals(dropCounts[index], ClassicWorldDropCatalog.dropCount(mapId));
            assertEquals(itemCounts[index], ClassicWorldDropCatalog.items(mapId).size());
            for (ClassicWorldDropCatalog.ItemWire wire
                    : ClassicWorldDropCatalog.items(mapId)) {
                assertNotNull(wire.inventoryDefinition());
                assertNotNull(InventoryCatalog.find(wire.itemId()));
            }
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            try (InputStream in = getClass().getResourceAsStream(
                    "/world/map" + mapId + "-drops.lwd.gz")) {
                assertNotNull(in);
                digest.update(in.readAllBytes());
            }
            assertEquals(resourceHashes[index],
                    HexFormat.of().withUpperCase().formatHex(digest.digest()));
            ClassicWorldDropCatalog.DropRule adena = ClassicWorldDropCatalog
                    .rules(mapId, 45269).stream()
                    .filter(rule -> rule.itemId() == InventoryCatalog.ADENA)
                    .findFirst().orElseThrow();
            assertEquals(50, adena.minimum());
            assertEquals(66, adena.maximum());
            assertEquals(ClassicWorldDropCatalog.CHANCE_SCALE, adena.chance());
            ClassicWorldDropCatalog.RolledItem rolled = ClassicWorldDropCatalog.roll(
                            mapId, 45269, 1.0, 1.0, 1.0,
                            new Random(0x310000 + mapId))
                    .stream().filter(item ->
                            item.item().itemId() == InventoryCatalog.ADENA)
                    .findFirst().orElseThrow();
            assertTrue(rolled.quantity() >= 50);
            assertTrue(rolled.quantity() <= 66);
        }
    }

    @Test
    void selectedDeepDragonValleyDropsKeepExactRowsAndWireItems()
            throws Exception {
        int[] mapIds = {35, 36, 37};
        int[] dropCounts = {ClassicWorldDropCatalog.MAP35_DROP_COUNT,
                ClassicWorldDropCatalog.MAP36_DROP_COUNT,
                ClassicWorldDropCatalog.MAP37_DROP_COUNT};
        int[] itemCounts = {ClassicWorldDropCatalog.MAP35_ITEM_COUNT,
                ClassicWorldDropCatalog.MAP36_ITEM_COUNT,
                ClassicWorldDropCatalog.MAP37_ITEM_COUNT};
        String[] resourceHashes = {ClassicWorldDropCatalog.MAP35_RESOURCE_SHA256,
                ClassicWorldDropCatalog.MAP36_RESOURCE_SHA256,
                ClassicWorldDropCatalog.MAP37_RESOURCE_SHA256};
        for (int index = 0; index < mapIds.length; index++) {
            int mapId = mapIds[index];
            assertEquals(dropCounts[index], ClassicWorldDropCatalog.dropCount(mapId));
            assertEquals(itemCounts[index], ClassicWorldDropCatalog.items(mapId).size());
            for (ClassicWorldDropCatalog.ItemWire wire
                    : ClassicWorldDropCatalog.items(mapId)) {
                assertNotNull(wire.inventoryDefinition());
                assertNotNull(InventoryCatalog.find(wire.itemId()));
            }
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            try (InputStream in = getClass().getResourceAsStream(
                    "/world/map" + mapId + "-drops.lwd.gz")) {
                assertNotNull(in);
                digest.update(in.readAllBytes());
            }
            assertEquals(resourceHashes[index],
                    HexFormat.of().withUpperCase().formatHex(digest.digest()));
        }
        assertTrue(ClassicWorldDropCatalog.items(37).isEmpty());
        assertTrue(ClassicWorldDropCatalog.roll(
                37, 45060, 1.0, 1.0, 1.0, new Random(37)).isEmpty());
    }

    @Test
    void selectedFairyDungeonsPreserveTheirExplicitlyEmptyDropResources()
            throws Exception {
        int[] mapIds = {209, 213};
        String[] resourceHashes = {
            ClassicWorldDropCatalog.MAP209_RESOURCE_SHA256,
            ClassicWorldDropCatalog.MAP213_RESOURCE_SHA256};
        for (int index = 0; index < mapIds.length; index++) {
            int mapId = mapIds[index];
            assertEquals(0, ClassicWorldDropCatalog.dropCount(mapId));
            assertTrue(ClassicWorldDropCatalog.items(mapId).isEmpty());
            assertTrue(ClassicWorldDropCatalog.roll(
                    mapId, 45009, 1.0, 1.0, 1.0, new Random(209 + index))
                    .isEmpty());
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            try (InputStream in = getClass().getResourceAsStream(
                    "/world/map" + mapId + "-drops.lwd.gz")) {
                assertNotNull(in);
                digest.update(in.readAllBytes());
            }
            assertEquals(resourceHashes[index],
                    HexFormat.of().withUpperCase().formatHex(digest.digest()));
        }
        assertEquals(ClassicWorldDropCatalog.MAP209_SOURCE_SHA256,
                ClassicWorldDropCatalog.MAP213_SOURCE_SHA256);
    }

    @Test
    void selectedMap445PreservesItsExplicitlyEmptyDropResource()
            throws Exception {
        assertEquals(0, ClassicWorldDropCatalog.dropCount(445));
        assertTrue(ClassicWorldDropCatalog.items(445).isEmpty());
        assertEquals(ClassicWorldDropCatalog.MAP209_SOURCE_SHA256,
                ClassicWorldDropCatalog.MAP445_SOURCE_SHA256);
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        try (InputStream in = getClass().getResourceAsStream(
                "/world/map445-drops.lwd.gz")) {
            assertNotNull(in);
            digest.update(in.readAllBytes());
        }
        assertEquals(ClassicWorldDropCatalog.MAP445_RESOURCE_SHA256,
                HexFormat.of().withUpperCase().formatHex(digest.digest()));
    }

    @Test
    void selectedMap447PreservesItsExplicitlyEmptyDropResource()
            throws Exception {
        assertEquals(0, ClassicWorldDropCatalog.dropCount(447));
        assertTrue(ClassicWorldDropCatalog.items(447).isEmpty());
        assertEquals(ClassicWorldDropCatalog.MAP209_SOURCE_SHA256,
                ClassicWorldDropCatalog.MAP447_SOURCE_SHA256);
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        try (InputStream in = getClass().getResourceAsStream(
                "/world/map447-drops.lwd.gz")) {
            assertNotNull(in);
            digest.update(in.readAllBytes());
        }
        assertEquals(ClassicWorldDropCatalog.MAP447_RESOURCE_SHA256,
                HexFormat.of().withUpperCase().formatHex(digest.digest()));
    }

    @Test
    void selectedMap17PreservesItsExplicitlyEmptyDropResource()
            throws Exception {
        assertEquals(0, ClassicWorldDropCatalog.dropCount(17));
        assertTrue(ClassicWorldDropCatalog.items(17).isEmpty());
        assertEquals(ClassicWorldDropCatalog.MAP209_SOURCE_SHA256,
                ClassicWorldDropCatalog.MAP17_SOURCE_SHA256);
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        try (InputStream in = getClass().getResourceAsStream(
                "/world/map17-drops.lwd.gz")) {
            assertNotNull(in);
            digest.update(in.readAllBytes());
        }
        assertEquals(ClassicWorldDropCatalog.MAP17_RESOURCE_SHA256,
                HexFormat.of().withUpperCase().formatHex(digest.digest()));
    }

    @Test
    void orcDropRowsKeepSelectedSourceChanceScaleAndGuaranteedAdena() {
        List<ClassicWorldDropCatalog.DropRule> rules =
                ClassicWorldDropCatalog.rules(4, 45009);
        assertEquals(List.of(
                        1, 21, 20034, 20135, 40005, 40008,
                        40088, 40308, 40405, 40496, 40899),
                rules.stream().map(
                        ClassicWorldDropCatalog.DropRule::itemId).toList());
        ClassicWorldDropCatalog.DropRule adena = rules.stream()
                .filter(rule -> rule.itemId() == InventoryCatalog.ADENA)
                .findFirst().orElseThrow();
        assertEquals(10, adena.minimum());
        assertEquals(10, adena.maximum());
        assertEquals(ClassicWorldDropCatalog.CHANCE_SCALE, adena.chance());

        List<ClassicWorldDropCatalog.RolledItem> rolled =
                ClassicWorldDropCatalog.roll(4, 45009, 1.0, 1.0, 1.0,
                        new Random(0x381C));
        ClassicWorldDropCatalog.RolledItem currency = rolled.stream()
                .filter(item -> item.item().itemId() == InventoryCatalog.ADENA)
                .findFirst().orElseThrow();
        assertEquals(10, currency.quantity());
        assertTrue(currency.chanceRoll() >= 1);
        assertTrue(currency.chanceRoll() <= ClassicWorldDropCatalog.CHANCE_SCALE);
        assertTrue(ClassicWorldDropCatalog.roll(
                4, 45009, 0.0, 1.0, 1.0, new Random(0x381C)).isEmpty());
    }

    @Test
    void phantasmMushroomKeepsSelectedSourceGuaranteedAdena() {
        List<ClassicWorldDropCatalog.DropRule> rules =
                ClassicWorldDropCatalog.rules(303, 45551);
        assertEquals(List.of(
                        40006, 40008, 40014, 40016, 40033, 40034, 40035,
                        40036, 40037, 40038, 40074, 40087, 40308, 40499, 84072),
                rules.stream().map(
                        ClassicWorldDropCatalog.DropRule::itemId).toList());
        ClassicWorldDropCatalog.DropRule adena = rules.stream()
                .filter(rule -> rule.itemId() == InventoryCatalog.ADENA)
                .findFirst().orElseThrow();
        assertEquals(70, adena.minimum());
        assertEquals(90, adena.maximum());
        assertEquals(ClassicWorldDropCatalog.CHANCE_SCALE, adena.chance());

        ClassicWorldDropCatalog.RolledItem currency =
                ClassicWorldDropCatalog.roll(
                        303, 45551, 1.0, 1.0, 1.0, new Random(0x303381C))
                .stream()
                .filter(item -> item.item().itemId() == InventoryCatalog.ADENA)
                .findFirst().orElseThrow();
        assertTrue(currency.quantity() >= 70);
        assertTrue(currency.quantity() <= 90);
    }

    @Test
    void everyImportedDropItemCanBuildNativeInventoryWireMetadata() {
        for (ClassicWorldDropCatalog.ItemWire wire
                : ClassicWorldDropCatalog.items(4)) {
            InventoryCatalog.Definition definition = wire.inventoryDefinition();
            assertEquals(wire.itemId(), definition.catalogId());
            assertEquals(wire.inventoryGfx(), definition.icon());
            assertEquals(wire.groundGfx(), definition.groundGfx());
            assertEquals(wire.stackable(), definition.stackable());
            assertEquals(wire.tradable(), definition.tradable());
            assertEquals(wire.blessing(), definition.blessing());
            if (wire.kind().equals("weapon")) {
                assertNotNull(wire.nativeWeaponStatus());
            } else {
                assertNull(wire.nativeWeaponStatus());
            }
            assertNotNull(InventoryCatalog.find(wire.itemId()));
        }
        ClassicWorldDropCatalog.ItemWire adena =
                ClassicWorldDropCatalog.item(4, (int) InventoryCatalog.ADENA);
        assertNotNull(adena);
        assertEquals("$4", adena.nameId());
        assertEquals(7, adena.groundGfx());
        assertTrue(adena.stackable());
        assertFalse(adena.kind().equals("weapon"));
    }
}
