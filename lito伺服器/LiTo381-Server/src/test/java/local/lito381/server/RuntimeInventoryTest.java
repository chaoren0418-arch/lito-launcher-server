package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;

final class RuntimeInventoryTest {
    @Test
    void systemRewardPreservesExactEnchantAndUsesNativeSlotRules() {
        RuntimeInventory inventory = new RuntimeInventory();
        InventoryCatalog.Definition sword = InventoryCatalog.require(
                InventoryCatalog.SHORT_SWORD);
        assertTrue(inventory.addSystemReward(sword, 2, 3));
        assertEquals(2, inventory.size());
        assertTrue(inventory.items().stream().allMatch(item ->
                item.enchantLevel() == 3 && item.identified()
                        && !item.sourceBound() && !item.litoOwned()));

        InventoryCatalog.Definition adena = InventoryCatalog.require(
                InventoryCatalog.ADENA);
        assertTrue(inventory.addSystemReward(adena, 7, 0));
        assertEquals(7, inventory.countCatalog(InventoryCatalog.ADENA));
        assertFalse(inventory.addSystemReward(adena, 1, 1));
    }

    @Test
    void pcActionHelperUsesExactSelectedStaticWireIdentity() {
        InventoryCatalog.Definition helper = InventoryCatalog.require(
                InventoryCatalog.PC_ACTION_HELPER);
        assertEquals(0, helper.useType());
        assertEquals(1760, helper.icon());
        assertEquals(4337, helper.groundGfx());
        assertFalse(helper.stackable());
        assertEquals("天堂小幫手", helper.name());
    }

    @Test
    void timedItemScanUsesStrictBeforeAndBypassesSourceTransferGate() {
        InventoryCatalog.Definition item = InventoryCatalog.require(
                InventoryCatalog.SHORT_SWORD);
        RuntimeInventory inventory = new RuntimeInventory();
        long deadline = 1_900_000_000_000L;
        long expiredObject = inventory.addDistinctState(item,
                new RuntimeInventory.PersistentItem(
                        0, 1, 0, 0, true, RuntimeInventory.BLESSING_NORMAL,
                        true, false, false, null, 0, deadline - 1, false));
        long boundaryObject = inventory.addDistinctState(item,
                new RuntimeInventory.PersistentItem(
                        0, 1, 0, 0, true, RuntimeInventory.BLESSING_NORMAL,
                        true, false, false, null, 0, deadline, false));
        long futureObject = inventory.addDistinctState(item,
                new RuntimeInventory.PersistentItem(
                        0, 1, 0, 0, true, RuntimeInventory.BLESSING_NORMAL,
                        false, false, false, null, 0, deadline + 1, false));

        List<RuntimeInventory.Item> expired = inventory.expireTimedItems(deadline);

        assertEquals(List.of(expiredObject),
                expired.stream().map(RuntimeInventory.Item::objectId).toList());
        assertFalse(inventory.items().stream().anyMatch(
                candidate -> candidate.objectId() == expiredObject));
        assertTrue(inventory.item(boundaryObject).timed());
        assertTrue(inventory.item(futureObject).timed());
    }

    @Test
    void timedAndSealedInstanceStateSurvivesDurableAndGroundTransfers() {
        InventoryCatalog.Definition sword = InventoryCatalog.require(
                InventoryCatalog.SHORT_SWORD);
        RuntimeInventory inventory = new RuntimeInventory();
        long expiresAt = 1_900_000_000_000L;
        long objectId = inventory.addDistinctState(sword,
                new RuntimeInventory.PersistentItem(
                        0, 1, 3, 2, true, RuntimeInventory.BLESSING_NORMAL,
                        false, false, false, null, 0, expiresAt, true));
        assertTrue(objectId > 0);
        RuntimeInventory.Item original = inventory.item(objectId);
        assertTrue(original.timed());
        assertTrue(original.sealed());

        RuntimeInventory restored = RuntimeInventory.fromDurableState(
                inventory.catalogCounts(), inventory.catalogItemStates());
        RuntimeInventory.Item durable = restored.items().get(0);
        assertEquals(expiresAt, durable.expiresAtEpochMillis());
        assertTrue(durable.sealed());

        BattleGroundItems ground = new BattleGroundItems();
        BattleGroundItems.Item dropped = ground.put(durable, 1, 32740, 32798);
        assertEquals(expiresAt, dropped.inventoryItem().expiresAtEpochMillis());
        assertTrue(dropped.inventoryItem().sealed());
    }

    @Test
    void lightningWandStartsWithSixNativeChargesAndPersistsDecrement() {
        RuntimeInventory inventory = RuntimeInventory.fromCatalogCounts(
                Map.of(InventoryCatalog.LIGHTNING_WAND, 1), () -> 0x7000_4007L);
        RuntimeInventory.Item wand = inventory.items().get(0);
        assertEquals(6, wand.chargeCount());
        assertEquals(6, wand.wireItem().chargeCount());
        assertTrue(inventory.decrementCharge(wand.objectId()));
        assertEquals(5, inventory.item(wand.objectId()).chargeCount());

        RuntimeInventory restored = RuntimeInventory.fromDurableState(
                inventory.catalogCounts(), inventory.catalogItemStates(),
                () -> 0x7000_5000L);
        RuntimeInventory.Item restoredWand = restored.items().get(0);
        assertEquals(wand.objectId(), restoredWand.objectId());
        assertEquals(5, restoredWand.chargeCount());
        assertEquals("$262 (5)", restoredWand.nativeViewName());
    }

    @Test
    void nativeTournamentBoundaryKeepsOnlySourceWeaponsAndArmor() {
        RuntimeInventory inventory = new RuntimeInventory();
        InventoryCatalog.Definition sourceWeapon = InventoryCatalog.require(
                InventoryCatalog.SHORT_SWORD);
        long weaponObject = inventory.addDistinctState(sourceWeapon,
                new RuntimeInventory.PersistentItem(
                        0, 1, 0, 0, true, 1, true, true));
        long armorObject = addSourceArmor(inventory, 20_201, 2, true, 1);
        inventory.addDistinctState(InventoryCatalog.require(
                        InventoryCatalog.NATIVE_HEALING_POTION),
                new RuntimeInventory.PersistentItem(
                        0, 12, 0, 0, true, 1, true, false));
        inventory.addDistinctState(InventoryCatalog.require(
                        InventoryCatalog.SILVER_ARROW),
                new RuntimeInventory.PersistentItem(
                        0, 80, 0, 0, true, 1, true, false));
        assertTrue(inventory.add(InventoryCatalog.require(
                InventoryCatalog.EXTRA_HEALING_POTION), 3));

        RuntimeInventory retained = inventory.sourceEquipmentOnly();

        assertEquals(List.of(weaponObject, armorObject),
                retained.items().stream().map(RuntimeInventory.Item::objectId).toList());
        assertTrue(retained.items().stream().allMatch(RuntimeInventory.Item::sourceBound));
        assertTrue(retained.items().stream().allMatch(item ->
                item.definition().weapon() || item.definition().armor()));
        assertEquals(5, inventory.size());
    }

    @Test
    void nativeTournamentBoundaryKeepsRacePrizeButDropsBoughtItems() {
        RuntimeInventory inventory = new RuntimeInventory();
        // 0.7.387 compatibility: an unopened bag saved before format-34 has
        // no explicit reward bit, but its catalog was never sold by LiTo.
        assertTrue(inventory.add(InventoryCatalog.require(
                BattlePetRaceRewardCatalog.HERO_PUMPKIN_BAG), 2));
        assertTrue(inventory.add(InventoryCatalog.require(
                InventoryCatalog.NATIVE_HEALING_POTION), 30));

        RuntimeInventory retained = inventory.sourceEquipmentOnly();

        assertEquals(Map.of(BattlePetRaceRewardCatalog.HERO_PUMPKIN_BAG, 2),
                retained.catalogCounts());
        assertFalse(retained.items().get(0).transferable());
    }

    @Test
    void rewardProvenanceSeparatesSameCatalogStacksAndSurvivesRoundReset() {
        RuntimeInventory inventory = new RuntimeInventory();
        InventoryCatalog.Definition potion = InventoryCatalog.require(
                InventoryCatalog.NATIVE_HEALING_POTION);
        assertTrue(inventory.add(potion, 5));
        assertTrue(inventory.addTournamentReward(potion, 30));

        assertEquals(2, inventory.items().size());
        assertEquals(35, inventory.catalogCounts().get(potion.catalogId()));
        assertEquals(List.of(false, true), inventory.items().stream()
                .map(RuntimeInventory.Item::tournamentReward).toList());

        RuntimeInventory restored = RuntimeInventory.fromDurableState(
                inventory.catalogCounts(), inventory.catalogItemStates());
        assertEquals(List.of(false, true), restored.items().stream()
                .map(RuntimeInventory.Item::tournamentReward).toList());
        RuntimeInventory retained = restored.sourceEquipmentOnly();
        assertEquals(30, retained.catalogCounts().get(potion.catalogId()));
        assertTrue(retained.items().get(0).tournamentReward());
    }

    @Test
    void nativeTournamentBoundaryRebindsSourceBowOnlyToPurchasedArrow() {
        long bowObject = 0x7000_3811L;
        long sourceArrowObject = 0x7000_3812L;
        SourceProjectilePhysicalStats sourceArrow =
                new SourceProjectilePhysicalStats(
                        sourceArrowObject, (int) InventoryCatalog.SILVER_ARROW,
                        -2, 7, 6, 8, 1, 66, true, 40);
        SourceWeaponPhysicalStats sourceBow = new SourceWeaponPhysicalStats(
                (int) InventoryCatalog.CKEW_ELF_BOW_REWARD,
                4, 3, 3, 15, 2, 4, 0, 0).withProjectile(sourceArrow);
        RuntimeInventory inventory = RuntimeInventory.fromDurableState(
                Map.of(
                        InventoryCatalog.CKEW_ELF_BOW_REWARD, 1,
                        InventoryCatalog.SILVER_ARROW, 40),
                Map.of(
                        InventoryCatalog.CKEW_ELF_BOW_REWARD, List.of(
                                new RuntimeInventory.PersistentItem(
                                        bowObject, 1, 0, 0, true, 1, true, true)),
                        InventoryCatalog.SILVER_ARROW, List.of(
                                new RuntimeInventory.PersistentItem(
                                        sourceArrowObject, 40, 0, 0, true, 1,
                                        true, false))),
                Map.of(), Map.of(bowObject, sourceBow));

        RuntimeInventory retained = inventory.sourceEquipmentOnly();

        assertFalse(retained.equippedPhysicalWeapon().projectile().present());
        assertTrue(retained.add(InventoryCatalog.require(
                InventoryCatalog.MITHRIL_ARROW), 12));
        SourceProjectilePhysicalStats tournamentArrow =
                retained.equippedPhysicalWeapon().projectile();
        assertTrue(tournamentArrow.present());
        assertEquals(InventoryCatalog.MITHRIL_ARROW, tournamentArrow.itemId());
        assertNotEquals(sourceArrowObject, tournamentArrow.objectId());
        assertEquals(12, tournamentArrow.count());
    }

    @Test
    void sourceBoundCombatStatePersistsAndCannotUseTransferAdmission() {
        InventoryCatalog.Definition weapon = InventoryCatalog.require(
                InventoryCatalog.SHORT_SWORD);
        RuntimeInventory source = new RuntimeInventory();
        long objectId = source.addDistinctState(weapon,
                new RuntimeInventory.PersistentItem(
                        0, 1, 0, 0, true, 1, true));
        RuntimeInventory.Item item = source.item(objectId);
        assertTrue(item.sourceBound());
        assertFalse(item.transferable());
        assertFalse(new RuntimeInventory().addTransferred(item, 1));

        RuntimeInventory restored = RuntimeInventory.fromDurableState(
                source.catalogCounts(), source.catalogItemStates());
        assertTrue(restored.item(objectId).sourceBound());
    }

    @Test
    void retainedStableStackObjectsDoNotMergeDuringRestore() {
        InventoryCatalog.Definition adena = InventoryCatalog.require(InventoryCatalog.ADENA);
        RuntimeInventory restored = RuntimeInventory.fromDurableState(
                Map.of(InventoryCatalog.ADENA, 12),
                Map.of(InventoryCatalog.ADENA, List.of(
                        new RuntimeInventory.PersistentItem(
                                0x7000_0101L, 5, 0, 0, true, 1),
                        new RuntimeInventory.PersistentItem(
                                0x7000_0102L, 7, 0, 0, true, 1))),
                Map.of(InventoryCatalog.ADENA, adena));

        assertEquals(2, restored.items().size());
        assertEquals(List.of(0x7000_0101L, 0x7000_0102L),
                restored.items().stream().map(RuntimeInventory.Item::objectId).toList());
        assertEquals(List.of(5, 7),
                restored.items().stream().map(RuntimeInventory.Item::quantity).toList());
    }
    @Test void decayPotionBlocksOnlyImplementedDrinkAndFoodExecutors() {
        long[] blocked = {
                InventoryCatalog.NATIVE_HEALING_POTION,
                InventoryCatalog.SELF_HASTE_POTION,
                InventoryCatalog.BRAVE_POTION,
                InventoryCatalog.WISDOM_POTION,
                InventoryCatalog.ELVEN_WAFER,
                InventoryCatalog.BLUE_POTION,
                InventoryCatalog.CURE_POISON_POTION,
                InventoryCatalog.BLINDNESS_POTION,
                InventoryCatalog.BLESS_OF_EVA,
                InventoryCatalog.LIQUOR,
                InventoryCatalog.FLOATING_EYE_STEAK
        };
        for (long catalogId : blocked) {
            assertTrue(InventoryCatalog.blockedByDecayPotion(
                    InventoryCatalog.require(catalogId)),
                    "expected potion/food block for catalog " + catalogId);
        }
        assertFalse(InventoryCatalog.blockedByDecayPotion(
                InventoryCatalog.require(InventoryCatalog.FIRE_DRAGON_EYE)));
        assertFalse(InventoryCatalog.blockedByDecayPotion(
                InventoryCatalog.require(InventoryCatalog.POLYMORPH_SCROLL)));
        assertFalse(InventoryCatalog.blockedByDecayPotion(
                InventoryCatalog.require(InventoryCatalog.COOKING_FUEL)));
        assertFalse(InventoryCatalog.blockedByDecayPotion(null));
    }

    @Test void durableCatalogCountsBecomeDistinctNativeObjectIds() {
        Map<Long, Integer> durable = new LinkedHashMap<>();
        durable.put(InventoryCatalog.TRAINING_SWORD, 2);
        durable.put(InventoryCatalog.HEALING_POTION, 3);
        AtomicLong ids = new AtomicLong(0x7000_0100L);

        RuntimeInventory inventory = RuntimeInventory.fromCatalogCounts(durable, ids::getAndIncrement);

        assertEquals(3, inventory.size());
        assertEquals(durable, inventory.catalogCounts());
        assertTrue(inventory.items().stream().allMatch(item -> item.objectId() >= 0x7000_0100L));
        assertTrue(inventory.items().stream().noneMatch(item ->
                item.objectId() == item.definition().catalogId()));
        assertEquals(2, inventory.items().stream()
                .filter(item -> item.definition().catalogId() == InventoryCatalog.TRAINING_SWORD)
                .count());
        assertEquals(1, inventory.items().stream()
                .filter(item -> item.definition().catalogId() == InventoryCatalog.HEALING_POTION)
                .count());
    }

    @Test void format25DurableStateRestoresTheExactNativeObjectId() {
        long collarObjectId = 0x7000_3810L;
        RuntimeInventory.PersistentItem collar = new RuntimeInventory.PersistentItem(
                collarObjectId, 1, 0, 0, true, RuntimeInventory.BLESSING_BLESSED);
        AtomicLong unexpectedAllocator = new AtomicLong(0x7000_9990L);

        RuntimeInventory restored = RuntimeInventory.fromDurableState(
                Map.of(InventoryCatalog.PET_COLLAR, 1),
                Map.of(InventoryCatalog.PET_COLLAR, List.of(collar)),
                unexpectedAllocator::getAndIncrement);

        assertEquals(collarObjectId, restored.items().get(0).objectId());
        assertEquals(collarObjectId,
                restored.catalogItemStates().get(InventoryCatalog.PET_COLLAR)
                        .get(0).objectId());
        assertEquals(0x7000_9990L, unexpectedAllocator.get());
    }

    @Test void copySupportsAtomicObjectIdSalesWithoutChangingOriginal() {
        AtomicLong ids = new AtomicLong(0x7000_0200L);
        RuntimeInventory original = RuntimeInventory.fromCatalogCounts(
                Map.of(InventoryCatalog.HEALING_POTION, 3), ids::getAndIncrement);
        long objectId = original.items().get(0).objectId();
        RuntimeInventory staged = original.copy();

        assertTrue(staged.remove(objectId, 2));
        assertEquals(1, staged.item(objectId).quantity());
        assertEquals(3, original.item(objectId).quantity());
        assertFalse(staged.remove(InventoryCatalog.HEALING_POTION, 1));
    }

    @Test void retainedGroundTransferMergesStackableByCatalogAndKeepsOwnedState() {
        AtomicLong ids = new AtomicLong(0x7000_0280L);
        RuntimeInventory inventory = RuntimeInventory.fromCatalogCounts(
                Map.of(InventoryCatalog.ADENA, 100_000), ids::getAndIncrement);
        RuntimeInventory.Item owned = inventory.items().get(0);
        RuntimeInventory.Item unidentifiedGround = new RuntimeInventory.Item(
                0x5800_0000L, InventoryCatalog.require(InventoryCatalog.ADENA),
                90, false, 0, 0, false, owned.blessing());

        assertTrue(inventory.addTransferred(unidentifiedGround, 90,
                ids::getAndIncrement));
        assertEquals(1, inventory.size());
        assertEquals(100_090, inventory.item(owned.objectId()).quantity());
        assertTrue(inventory.item(owned.objectId()).identified());
        assertEquals(null, inventory.item(unidentifiedGround.objectId()));
    }

    @Test void retainedGroundTransferKeepsWholeObjectWhenNoStackExists() {
        AtomicLong ids = new AtomicLong(0x7000_0290L);
        RuntimeInventory inventory = new RuntimeInventory();
        RuntimeInventory.Item ground = new RuntimeInventory.Item(
                0x5800_0001L, InventoryCatalog.require(InventoryCatalog.ADENA),
                70, false, 0, 0, false, RuntimeInventory.BLESSING_BLESSED);

        assertTrue(inventory.addTransferred(ground, 70, ids::getAndIncrement));
        assertEquals(1, inventory.size());
        assertEquals(70, inventory.item(ground.objectId()).quantity());
        assertFalse(inventory.item(ground.objectId()).identified());
        assertEquals(0x7000_0290L, ids.get());
    }

    @Test void groundTransferDoesNotReapplyThePlayerTradabilityGate() {
        InventoryCatalog.Definition untradable = new InventoryCatalog.Definition(
                65_784, 0, 321, 322, 1, true,
                "Ground Untradable", "native ground transfer", 0, 0,
                true, false, BattleActionCodes.WALK, 0,
                InventoryCatalog.Effect.NONE, 0, 100, 0, null, null);
        RuntimeInventory.Item ground = new RuntimeInventory.Item(
                0x5800_0784L, untradable, 1, false);

        assertEquals(null, new RuntimeInventory().addTransferredResult(ground, 1));
        RuntimeInventory inventory = new RuntimeInventory();
        RuntimeInventory.Item admitted = inventory.addGroundTransferredResult(ground, 1);
        assertEquals(ground.objectId(), admitted.objectId());
        assertEquals(untradable, admitted.definition());
    }

    @Test void equippingOneWeaponUnequipsThePreviousWeapon() {
        AtomicLong ids = new AtomicLong(0x7000_0300L);
        RuntimeInventory inventory = RuntimeInventory.fromCatalogCounts(
                Map.of(InventoryCatalog.TRAINING_SWORD, 2), ids::getAndIncrement);
        long first = inventory.items().get(0).objectId();
        long second = inventory.items().get(1).objectId();

        assertEquals("+0 Training Sword", inventory.item(first).nativeViewName());
        assertEquals(1, inventory.toggleWeapon(first).size());
        assertTrue(inventory.item(first).equipped());
        assertEquals("+0 Training Sword ($9)", inventory.item(first).nativeViewName());
        assertEquals(BattleActionCodes.SWORD_WALK, inventory.equippedWalkAction());
        assertEquals(2, inventory.toggleWeapon(second).size());
        assertFalse(inventory.item(first).equipped());
        assertEquals("+0 Training Sword", inventory.item(first).nativeViewName());
        assertTrue(inventory.item(second).equipped());
        assertEquals("+0 Training Sword ($9)", inventory.item(second).nativeViewName());
        assertEquals(1, inventory.toggleWeapon(second).size());
        assertEquals(BattleActionCodes.WALK, inventory.equippedWalkAction());
    }

    @Test void equippedWeaponStateSurvivesDurableRestore() {
        RuntimeInventory inventory = RuntimeInventory.fromCatalogCounts(
                Map.of(InventoryCatalog.SHORT_SWORD, 1));
        long objectId = inventory.items().get(0).objectId();
        assertEquals(1, inventory.toggleWeapon(objectId).size());

        RuntimeInventory restored = RuntimeInventory.fromDurableState(
                inventory.catalogCounts(), inventory.catalogItemStates());

        assertTrue(restored.item(objectId).equipped());
        assertEquals(objectId, restored.equippedWeaponItem().objectId());
        assertTrue(restored.catalogItemStates().get(InventoryCatalog.SHORT_SWORD)
                .get(0).equipped());
    }

    @Test void polymorphForceUnequipReturnsTheExactChangedWeaponOnce() {
        AtomicLong ids = new AtomicLong(0x7000_0340L);
        RuntimeInventory inventory = RuntimeInventory.fromCatalogCounts(
                Map.of(InventoryCatalog.TRAINING_SWORD, 1), ids::getAndIncrement);
        long sword = inventory.items().get(0).objectId();
        inventory.toggleWeapon(sword);

        assertEquals(1, inventory.unequipWeapons().size());
        assertFalse(inventory.item(sword).equipped());
        assertTrue(inventory.unequipWeapons().isEmpty());
    }

    @Test void controlRingsUseNativeAbilityTypesAndTwoBaseSlots() {
        InventoryCatalog.Definition teleport = InventoryCatalog.require(
                InventoryCatalog.RING_OF_TELEPORT_CONTROL);
        InventoryCatalog.Definition polymorph = InventoryCatalog.require(
                InventoryCatalog.RING_OF_POLYMORPH_CONTROL);
        InventoryCatalog.Definition summon = InventoryCatalog.require(
                InventoryCatalog.RING_OF_SUMMON_CONTROL);
        assertEquals(23, teleport.useType());
        assertEquals(1, teleport.controlAbilityType());
        assertEquals(2, polymorph.controlAbilityType());
        assertEquals(5, summon.controlAbilityType());
        assertEquals(1194, teleport.icon());
        assertEquals(3165, teleport.groundGfx());
        assertEquals(64, teleport.itemDescriptionId());

        AtomicLong ids = new AtomicLong(0x7000_0350L);
        RuntimeInventory inventory = RuntimeInventory.fromCatalogCounts(Map.of(
                InventoryCatalog.RING_OF_TELEPORT_CONTROL, 1,
                InventoryCatalog.RING_OF_POLYMORPH_CONTROL, 1,
                InventoryCatalog.RING_OF_SUMMON_CONTROL, 1), ids::getAndIncrement);
        long teleportId = inventory.items().stream()
                .filter(item -> item.definition().catalogId()
                        == InventoryCatalog.RING_OF_TELEPORT_CONTROL)
                .findFirst().orElseThrow().objectId();
        long polymorphId = inventory.items().stream()
                .filter(item -> item.definition().catalogId()
                        == InventoryCatalog.RING_OF_POLYMORPH_CONTROL)
                .findFirst().orElseThrow().objectId();
        long summonId = inventory.items().stream()
                .filter(item -> item.definition().catalogId()
                        == InventoryCatalog.RING_OF_SUMMON_CONTROL)
                .findFirst().orElseThrow().objectId();

        assertEquals(1, inventory.toggleControlRing(teleportId).size());
        assertTrue(inventory.controlAbilityEnabled(1));
        // Retained L1ItemStatus prefixes every identified armor view with its
        // signed enchant value, including zero.
        assertEquals("+0 $241 ($117)", inventory.item(teleportId).nativeViewName());
        assertEquals(1, inventory.toggleControlRing(polymorphId).size());
        assertEquals(RuntimeInventory.NATIVE_RING_SLOT_LIMIT,
                inventory.equippedControlRingCount());
        assertTrue(inventory.controlAbilityEnabled(2));
        assertTrue(inventory.toggleControlRing(summonId).isEmpty());
        assertFalse(inventory.item(summonId).equipped());

        assertEquals(1, inventory.toggleControlRing(teleportId).size());
        assertFalse(inventory.controlAbilityEnabled(1));
        assertEquals(1, inventory.toggleControlRing(summonId).size());
        assertTrue(inventory.controlAbilityEnabled(5));
    }

    @Test void exactFireRingUsesAnOrdinaryNativeRingSlotAndPublishesResistance() {
        InventoryCatalog.Definition fire = InventoryCatalog.require(
                InventoryCatalog.RING_OF_FIRE_RESISTANCE);
        assertEquals(20304, fire.catalogId());
        assertEquals(23, fire.useType());
        assertEquals(628, fire.icon());
        assertEquals(21, fire.groundGfx());
        assertEquals(3000, fire.unitWeight());
        assertEquals(30, fire.fireResistance());
        assertTrue(fire.ring());
        assertFalse(fire.controlRing());

        AtomicLong ids = new AtomicLong(0x7000_0380L);
        RuntimeInventory inventory = RuntimeInventory.fromCatalogCounts(Map.of(
                InventoryCatalog.RING_OF_FIRE_RESISTANCE, 1,
                InventoryCatalog.RING_OF_TELEPORT_CONTROL, 1,
                InventoryCatalog.RING_OF_SUMMON_CONTROL, 1), ids::getAndIncrement);
        long fireId = inventory.items().stream()
                .filter(item -> item.definition().catalogId()
                        == InventoryCatalog.RING_OF_FIRE_RESISTANCE)
                .findFirst().orElseThrow().objectId();
        long teleportId = inventory.items().stream()
                .filter(item -> item.definition().catalogId()
                        == InventoryCatalog.RING_OF_TELEPORT_CONTROL)
                .findFirst().orElseThrow().objectId();
        long summonId = inventory.items().stream()
                .filter(item -> item.definition().catalogId()
                        == InventoryCatalog.RING_OF_SUMMON_CONTROL)
                .findFirst().orElseThrow().objectId();

        assertEquals(1, inventory.toggleRing(fireId).size());
        assertEquals("+0 $1663 ($117)", inventory.item(fireId).nativeViewName());
        assertEquals(30, inventory.activeFireResistance());
        assertEquals(1, inventory.toggleRing(teleportId).size());
        assertEquals(2, inventory.equippedRingCount());
        assertTrue(inventory.toggleRing(summonId).isEmpty());

        assertEquals(1, inventory.toggleRing(fireId).size());
        assertEquals(0, inventory.activeFireResistance());
        assertEquals(1, inventory.toggleRing(summonId).size());
        assertEquals(2, inventory.equippedRingCount());
    }

    @Test void consumesSpellReagentsByCatalogWithoutConfusingRuntimeObjectIds() {
        AtomicLong ids = new AtomicLong(0x7000_0400L);
        RuntimeInventory inventory = RuntimeInventory.fromCatalogCounts(
                Map.of(InventoryCatalog.MAGIC_GEM, 3), ids::getAndIncrement);
        long objectId = inventory.items().get(0).objectId();

        assertTrue(inventory.removeCatalog(InventoryCatalog.MAGIC_GEM, 1));
        assertEquals(2, inventory.item(objectId).quantity());
        assertFalse(inventory.removeCatalog(InventoryCatalog.MAGIC_GEM, 3));
        assertEquals(2, inventory.item(objectId).quantity());
        assertNotEquals(InventoryCatalog.MAGIC_GEM, objectId);
    }

    @Test void stagesNativeWeaponDamageAndRepairWithoutMutatingTheOriginalCopy() {
        AtomicLong ids = new AtomicLong(0x7000_0450L);
        RuntimeInventory original = RuntimeInventory.fromCatalogCounts(
                Map.of(InventoryCatalog.SHORT_SWORD, 1, InventoryCatalog.ADENA, 1_000),
                ids::getAndIncrement);
        long sword = original.items().stream()
                .filter(item -> item.definition().weapon())
                .findFirst().orElseThrow().objectId();
        assertTrue(original.damageWeapon(sword, 3));
        assertEquals(3, original.item(sword).durability());
        assertArrayEquals(new byte[] {
                0x01, 0x06, 0x08, 0x0B, 0x1E, 0, 0, 0, 0x03, 0x03, 0x07, 0x3F
        }, original.item(sword).nativeStatusDetail());
        assertEquals(1, original.repairableWeapons().size());

        RuntimeInventory staged = original.copy();
        assertTrue(staged.repairWeapon(sword));
        assertTrue(staged.removeCatalog(InventoryCatalog.ADENA, 600));
        assertEquals(0, staged.item(sword).durability());
        assertArrayEquals(new byte[] {
                0x01, 0x06, 0x08, 0x0B, 0x1E, 0, 0, 0, 0x07, 0x3F
        }, staged.item(sword).nativeStatusDetail());
        assertEquals(400, staged.catalogCounts().get(InventoryCatalog.ADENA));
        assertEquals(3, original.item(sword).durability());
        assertEquals(1_000, original.catalogCounts().get(InventoryCatalog.ADENA));
    }

    @Test void whetstoneRecoversExactlyOneDamageAndRejectsUndamagedTargets() {
        AtomicLong ids = new AtomicLong(0x7000_0460L);
        RuntimeInventory inventory = RuntimeInventory.fromCatalogCounts(
                Map.of(InventoryCatalog.SHORT_SWORD, 1), ids::getAndIncrement);
        long sword = inventory.items().get(0).objectId();
        assertTrue(inventory.damageWeapon(sword, 3));
        assertTrue(inventory.recoverDamage(sword));
        assertEquals(2, inventory.item(sword).durability());
        assertTrue(inventory.recoverDamage(sword));
        assertTrue(inventory.recoverDamage(sword));
        assertEquals(0, inventory.item(sword).durability());
        assertFalse(inventory.recoverDamage(sword));
        assertFalse(inventory.recoverDamage(0x7000_9999L));
    }

    @Test void createMagicalWeaponAddsNativeEnchantTagDamageAndDurableSequence() {
        AtomicLong ids = new AtomicLong(0x7000_0470L);
        RuntimeInventory original = RuntimeInventory.fromCatalogCounts(
                Map.of(InventoryCatalog.SHORT_SWORD, 1, InventoryCatalog.MAGIC_GEM, 10),
                ids::getAndIncrement);
        long sword = original.items().stream()
                .filter(item -> item.definition().catalogId() == InventoryCatalog.SHORT_SWORD)
                .findFirst().orElseThrow().objectId();
        assertEquals("+0 $100", original.item(sword).nativeViewName());
        assertEquals(6, InventoryCatalog.nativeWeaponStatus(
                InventoryCatalog.SHORT_SWORD).safeEnchant());

        RuntimeInventory staged = original.copy();
        assertTrue(staged.createMagicalWeapon(sword));
        assertFalse(staged.createMagicalWeapon(sword));
        assertEquals(1, staged.item(sword).enchantLevel());
        assertEquals("+1 $100", staged.item(sword).nativeViewName());
        assertArrayEquals(new byte[] {
                0x01, 0x06, 0x08, 0x0B, 0x1E, 0, 0, 0,
                0x02, 0x01, 0x07, 0x3F
        }, staged.item(sword).nativeStatusDetail());
        assertEquals(Map.of(InventoryCatalog.SHORT_SWORD, List.of(1)),
                staged.catalogEnchantments());
        staged.toggleWeapon(sword);
        assertEquals(1, staged.equippedPhysicalWeapon().enchantLevel());
        assertEquals(0, original.item(sword).enchantLevel());

        AtomicLong reloadIds = new AtomicLong(0x7000_0480L);
        RuntimeInventory reloaded = RuntimeInventory.fromCatalogCounts(
                staged.catalogCounts(), staged.catalogEnchantments(),
                reloadIds::getAndIncrement);
        RuntimeInventory.Item restored = reloaded.items().stream()
                .filter(item -> item.definition().catalogId() == InventoryCatalog.SHORT_SWORD)
                .findFirst().orElseThrow();
        assertEquals(1, restored.enchantLevel());
        assertFalse(reloaded.createMagicalWeapon(restored.objectId()));

        RuntimeInventory unsupported = RuntimeInventory.fromCatalogCounts(
                Map.of(InventoryCatalog.TRAINING_SWORD, 1), reloadIds::getAndIncrement);
        assertFalse(unsupported.createMagicalWeapon(
                unsupported.items().get(0).objectId()));
    }

    @Test void selfHastePotionMatchesTheClassicEtcitemResource() {
        InventoryCatalog.Definition potion =
                InventoryCatalog.require(InventoryCatalog.SELF_HASTE_POTION);
        assertEquals(40013, potion.catalogId());
        assertEquals(0, potion.useType());
        assertEquals(77, potion.icon());
        assertEquals(0, potion.blessing());
        assertTrue(potion.stackable());
        assertEquals(InventoryCatalog.Effect.HASTE, potion.effect());
        assertEquals(300, InventoryCatalog.SELF_HASTE_DURATION_SECONDS);
        assertEquals(191, InventoryCatalog.SELF_HASTE_EFFECT_GFX);
    }

    @Test void bravePotionMatchesTheClassicEtcitemAndStatusResource() {
        InventoryCatalog.Definition potion =
                InventoryCatalog.require(InventoryCatalog.BRAVE_POTION);
        assertEquals(40014, potion.catalogId());
        assertEquals(0, potion.useType());
        assertEquals(79, potion.icon());
        assertEquals(0, potion.blessing());
        assertTrue(potion.stackable());
        assertEquals(InventoryCatalog.Effect.BRAVE, potion.effect());
        assertEquals(300, InventoryCatalog.BRAVE_DURATION_SECONDS);
        assertEquals(751, InventoryCatalog.BRAVE_EFFECT_GFX);
    }

    @Test void blindnessPotionMatchesTheClassicEtcitemResource() {
        InventoryCatalog.Definition potion =
                InventoryCatalog.require(InventoryCatalog.BLINDNESS_POTION);
        assertEquals(40025, potion.catalogId());
        assertEquals(0, potion.useType());
        assertEquals(82, potion.icon());
        assertEquals(0, potion.blessing());
        assertTrue(potion.stackable());
        assertEquals(InventoryCatalog.Effect.BLIND, potion.effect());
        assertEquals(16, InventoryCatalog.BLINDNESS_DURATION_SECONDS);
    }

    @Test void antidoteMatchesTheClassicEtcitemAndEffectResource() {
        InventoryCatalog.Definition antidote =
                InventoryCatalog.require(InventoryCatalog.CURE_POISON_POTION);
        assertEquals(40017, antidote.catalogId());
        assertEquals(0, antidote.useType());
        assertEquals(76, antidote.icon());
        assertEquals(0, antidote.blessing());
        assertTrue(antidote.stackable());
        assertEquals(InventoryCatalog.Effect.ANTIDOTE, antidote.effect());
        assertEquals(192, InventoryCatalog.CURE_POISON_EFFECT_GFX);
    }

    @Test void polymorphScrollMatchesTheClassicEtcitemAndUseTypeResource() {
        InventoryCatalog.Definition scroll =
                InventoryCatalog.require(InventoryCatalog.POLYMORPH_SCROLL);
        assertEquals(40088, scroll.catalogId());
        assertEquals(16, scroll.useType());
        assertEquals(472, scroll.icon());
        assertEquals(0, scroll.blessing());
        assertTrue(scroll.stackable());
        assertEquals(InventoryCatalog.Effect.POLYMORPH, scroll.effect());
    }

    @Test void appraisalScrollMatchesClassicMetadataAndRuntimeIdentification() {
        InventoryCatalog.Definition scroll =
                InventoryCatalog.require(InventoryCatalog.APPRAISAL_SCROLL);
        assertEquals(40126, scroll.catalogId());
        assertEquals(7, scroll.useType());
        assertEquals(475, scroll.icon());
        assertEquals(22, scroll.groundGfx());
        assertEquals(1339, scroll.itemDescriptionId());
        assertEquals(630, scroll.unitWeight());
        assertEquals(12, scroll.packetWeight(20));
        assertEquals(InventoryCatalog.Effect.APPRAISAL, scroll.effect());

        InventoryCatalog.Definition unknown = new InventoryCatalog.Definition(
                0x00FF_0001L, 0, 1, 1, 0, false, "$test", "$test",
                0, 0, false, true, BattleActionCodes.WALK, 0,
                InventoryCatalog.Effect.NONE);
        RuntimeInventory inventory = new RuntimeInventory();
        assertTrue(inventory.add(unknown, 1, () -> 0x7000_2000L));
        RuntimeInventory.Item item = inventory.item(0x7000_2000L);
        assertFalse(item.identified());
        assertEquals(0, item.wireItem().identified());
        assertTrue(inventory.identify(item.objectId()));
        assertTrue(inventory.item(item.objectId()).identified());
        assertEquals(1, inventory.item(item.objectId()).wireItem().identified());
    }

    @Test void homeScrollMatchesClassicMetadataAndSemanticEffect() {
        assertEquals("A754539F710E6B6C6237705F749DC0D49B5A3245E3FAB1DCFC2EF88089FC512B",
                InventoryCatalog.HOME_SCROLL_SOURCE_SHA256);
        InventoryCatalog.Definition scroll =
                InventoryCatalog.require(InventoryCatalog.HOME_SCROLL);
        assertEquals(40079, scroll.catalogId());
        assertEquals(9, scroll.useType());
        assertEquals(474, scroll.icon());
        assertEquals(22, scroll.groundGfx());
        assertEquals(0, scroll.blessing());
        assertEquals("$505", scroll.name());
        assertEquals(1338, scroll.itemDescriptionId());
        assertEquals(630, scroll.unitWeight());
        assertTrue(scroll.stackable());
        assertTrue(scroll.tradable());
        assertEquals(InventoryCatalog.Effect.HOME_TELEPORT, scroll.effect());
    }

    @Test void curseRemovalScrollMatchesClassicMetadataAndMutatesOnlyTheStagedInventory() {
        InventoryCatalog.Definition scroll =
                InventoryCatalog.require(InventoryCatalog.CURSE_REMOVAL_SCROLL);
        assertEquals(40119, scroll.catalogId());
        assertEquals(0, scroll.useType());
        assertEquals(470, scroll.icon());
        assertEquals(22, scroll.groundGfx());
        assertEquals(1340, scroll.itemDescriptionId());
        assertEquals(630, scroll.unitWeight());
        assertEquals(InventoryCatalog.Effect.CURSE_REMOVAL, scroll.effect());

        AtomicLong ids = new AtomicLong(0x7000_2100L);
        RuntimeInventory original = RuntimeInventory.fromCatalogCounts(
                Map.of(InventoryCatalog.SHORT_SWORD, 1), ids::getAndIncrement);
        long sword = original.items().get(0).objectId();
        assertTrue(original.setBlessing(sword, RuntimeInventory.BLESSING_CURSED));
        RuntimeInventory staged = original.copy();
        assertEquals(1, staged.removeCurses().size());
        assertEquals(RuntimeInventory.BLESSING_NORMAL, staged.item(sword).blessing());
        assertEquals(RuntimeInventory.BLESSING_NORMAL,
                staged.item(sword).wireItem().blessing());
        assertEquals(RuntimeInventory.BLESSING_CURSED, original.item(sword).blessing());
    }

    @Test void bluePotionFamilyPreservesSignedUseTypeAndSourceDurations() {
        InventoryCatalog.Definition normal =
                InventoryCatalog.require(InventoryCatalog.BLUE_POTION);
        assertEquals(40015, normal.catalogId());
        assertEquals(0xFA, normal.useType());
        assertEquals(75, normal.icon());
        assertEquals(20, normal.groundGfx());
        assertEquals(0, normal.blessing());
        assertEquals(7_800, normal.unitWeight());
        assertEquals(InventoryCatalog.Effect.BLUE_MPR, normal.effect());
        assertEquals(600, InventoryCatalog.bluePotionDurationSeconds(normal.catalogId()));

        InventoryCatalog.Definition blessed =
                InventoryCatalog.require(InventoryCatalog.BLESSED_BLUE_POTION);
        assertEquals(1, blessed.blessing());
        assertEquals(700, InventoryCatalog.bluePotionDurationSeconds(blessed.catalogId()));

        InventoryCatalog.Definition coin =
                InventoryCatalog.require(InventoryCatalog.WISDOM_COIN);
        assertEquals(0xFA, coin.useType());
        assertEquals(1482, coin.icon());
        assertEquals(3683, coin.groundGfx());
        assertEquals(0, coin.unitWeight());
        assertEquals(600, InventoryCatalog.bluePotionDurationSeconds(coin.catalogId()));
    }

    @Test void levelOneCookingItemsMatchAllExactFood3Rows() {
        long[] ids = {41277, 41278, 41279, 41280, 41281, 41282, 41283, 41284,
                41285, 41286, 41287, 41288, 41289, 41290, 41291, 41292};
        int[] icons = {2486, 2572, 2495, 2570, 2484, 2485, 2573, 2575,
                2486, 2572, 2495, 2570, 2484, 2485, 2573, 2575};
        int[] ground = {5983, 6167, 5991, 6165, 5981, 5982, 6169, 6170,
                5983, 6167, 5991, 6165, 5981, 5982, 6169, 6170};
        int[] descriptions = {2562, 2563, 2564, 2565, 2566, 2567, 2568, 2569,
                2781, 2782, 2783, 2784, 2785, 2786, 2787, 2788};
        int[] weights = {2000, 2000, 2000, 2000, 2000, 2000, 2000, 2000,
                2000, 2000, 2000, 3000, 3000, 3000, 2000, 2000};
        for (int index = 0; index < ids.length; index++) {
            InventoryCatalog.Definition food = InventoryCatalog.require(ids[index]);
            assertEquals(38, food.useType());
            assertEquals(icons[index], food.icon());
            assertEquals(ground[index], food.groundGfx());
            assertEquals(descriptions[index], food.itemDescriptionId());
            assertEquals(weights[index], food.unitWeight());
            assertEquals(0, food.blessing());
            assertTrue(food.stackable());
            assertEquals(InventoryCatalog.Effect.COOKING, food.effect());
            assertEquals(ids[index], BattleCookingCatalog.require(ids[index]).catalogId());
        }
    }

    @Test void levelTwoCookingItemsMatchAllExactFood3Rows() {
        int[] icons = {2736, 2733, 2741, 2740, 2730, 2735, 2728, 2738,
                2736, 2733, 2741, 2740, 2730, 2735, 2728, 2738};
        for (int index = 0; index < 16; index++) {
            assertCookingRow(49049 + index, icons[index], 6165,
                    2953 + index, 2_000);
        }
    }

    @Test void levelThreeCookingItemsMatchAllExactFood3Rows() {
        int[] icons = {2940, 2925, 2939, 2927, 2930, 2928, 2936, 2933,
                2940, 2925, 2939, 2927, 2930, 2928, 2936, 2933};
        int[] ground = {6549, 6535, 6548, 6537, 6540, 6538, 6545, 6543,
                6549, 6535, 6548, 6537, 6540, 6538, 6545, 6543};
        for (int index = 0; index < 16; index++) {
            assertCookingRow(49244 + index, icons[index], ground[index],
                    3618 + index, 2_000);
        }
    }

    @Test void levelFourCookingItemsMatchAllExactFood3Rows() {
        int[] icons = {4955, 4956, 4944, 4978};
        int[] weights = {1_000, 2_000, 2_000, 2_000};
        String[] names = {"強壯的牛排", "敏捷的煎鮭魚", "炭烤的火雞", "養生的雞湯"};
        for (int index = 0; index < 4; index++) {
            long itemId = 49825 + index;
            assertCookingRow(itemId, icons[index], 4337, 0, weights[index]);
            assertEquals(names[index], InventoryCatalog.require(itemId).name());
        }
    }

    @Test void blessOfEvaMatchesClassicItemAndBreathMetadata() {
        InventoryCatalog.Definition potion =
                InventoryCatalog.require(InventoryCatalog.BLESS_OF_EVA);
        assertEquals(40032, potion.catalogId());
        assertEquals(0, potion.useType());
        assertEquals(557, potion.icon());
        assertEquals(1733, potion.groundGfx());
        assertEquals(411, potion.itemDescriptionId());
        assertEquals(2_000, potion.unitWeight());
        assertEquals(InventoryCatalog.Effect.BREATH, potion.effect());
        assertEquals(1_800, InventoryCatalog.BLESS_OF_EVA_DURATION_SECONDS);
        assertEquals(3_600, InventoryCatalog.UNDERWATER_BREATH_MAX_SECONDS);
        assertEquals(190, InventoryCatalog.BLESS_OF_EVA_EFFECT_GFX);
    }

    private static void assertCookingRow(long itemId, int icon, int groundGfx,
                                         int descriptionId, int unitWeight) {
        InventoryCatalog.Definition food = InventoryCatalog.require(itemId);
        assertEquals(38, food.useType());
        assertEquals(icon, food.icon());
        assertEquals(groundGfx, food.groundGfx());
        assertEquals(descriptionId, food.itemDescriptionId());
        assertEquals(unitWeight, food.unitWeight());
        assertEquals(0, food.blessing());
        assertTrue(food.stackable());
        assertEquals(InventoryCatalog.Effect.COOKING, food.effect());
        assertEquals(itemId, BattleCookingCatalog.require(itemId).catalogId());
    }

    @Test void whistleMatchesTheClassicInstrumentAndSoundResource() {
        InventoryCatalog.Definition whistle = InventoryCatalog.require(InventoryCatalog.WHISTLE);
        assertEquals(40315, whistle.catalogId());
        assertEquals(15, whistle.useType());
        assertEquals(477, whistle.icon());
        assertEquals(0, whistle.blessing());
        assertFalse(whistle.stackable());
        assertEquals(InventoryCatalog.Effect.WHISTLE, whistle.effect());
        assertEquals(437, InventoryCatalog.WHISTLE_SOUND_ID);
    }

    @Test void liquorMatchesTheClassicNormalPotionResource() {
        InventoryCatalog.Definition liquor = InventoryCatalog.require(InventoryCatalog.LIQUOR);
        assertEquals(40858, liquor.catalogId());
        assertEquals(0, liquor.useType());
        assertEquals(82, liquor.icon());
        assertEquals(0, liquor.blessing());
        assertTrue(liquor.stackable());
        assertEquals(InventoryCatalog.Effect.LIQUOR, liquor.effect());
    }

    @Test void magicLanternMatchesNativeLightResourceAndTogglesWithoutConsumption() {
        InventoryCatalog.Definition lantern =
                InventoryCatalog.require(InventoryCatalog.MAGIC_LANTERN);
        assertEquals(40004, lantern.catalogId());
        assertEquals(10, lantern.useType());
        assertEquals(140, lantern.icon());
        assertEquals(0, lantern.blessing());
        assertFalse(lantern.stackable());
        assertTrue(lantern.light());
        assertEquals(0x16, lantern.lightRange());

        AtomicLong ids = new AtomicLong(0x7000_0500L);
        RuntimeInventory inventory = RuntimeInventory.fromCatalogCounts(
                Map.of(InventoryCatalog.MAGIC_LANTERN, 1), ids::getAndIncrement);
        long objectId = inventory.items().get(0).objectId();
        assertEquals(0, inventory.activeLightRange());
        assertEquals(1, inventory.toggleLight(objectId).size());
        assertEquals(0x16, inventory.activeLightRange());
        assertTrue(inventory.item(objectId).equipped());
        assertEquals("魔法燈籠 ($10)", inventory.item(objectId).nativeViewName());
        assertEquals(1, inventory.item(objectId).quantity());
        inventory.toggleLight(objectId);
        assertEquals(0, inventory.activeLightRange());
        assertFalse(inventory.item(objectId).equipped());
        assertEquals("魔法燈籠", inventory.item(objectId).nativeViewName());
    }

    @Test void enforcesNativeInventorySlotLimit() {
        AtomicLong ids = new AtomicLong(0x7000_1000L);
        RuntimeInventory inventory = new RuntimeInventory();
        assertTrue(inventory.add(InventoryCatalog.require(InventoryCatalog.TRAINING_SWORD),
                RuntimeInventory.MAX_SLOTS, ids::getAndIncrement));
        assertEquals(RuntimeInventory.MAX_SLOTS, inventory.size());
        assertFalse(inventory.add(InventoryCatalog.require(InventoryCatalog.HEALING_POTION),
                1, ids::getAndIncrement));
        assertNotEquals(InventoryCatalog.TRAINING_SWORD, inventory.items().get(0).objectId());
    }

    @Test void sourceArmorUsesRetainedRingSlotsAndCursedRemovalRule() {
        RuntimeInventory inventory = new RuntimeInventory();
        long first = addSourceArmor(inventory, 90_001, 9, false,
                RuntimeInventory.BLESSING_NORMAL);
        long second = addSourceArmor(inventory, 90_002, 11, false,
                RuntimeInventory.BLESSING_NORMAL);
        long third = addSourceArmor(inventory, 90_003, 9, false,
                RuntimeInventory.BLESSING_NORMAL);

        assertEquals(1, inventory.toggleSourceArmor(first).size());
        assertEquals(1, inventory.toggleSourceArmor(second).size());
        assertTrue(inventory.toggleSourceArmor(third).isEmpty());
        assertEquals(2, inventory.equippedRingCount());

        RuntimeInventory cursed = new RuntimeInventory();
        long cursedRing = addSourceArmor(cursed, 90_004, 9, false,
                RuntimeInventory.BLESSING_CURSED);
        assertEquals(1, cursed.toggleSourceArmor(cursedRing).size());
        assertTrue(cursed.toggleSourceArmor(cursedRing).isEmpty());
        assertTrue(cursed.item(cursedRing).equipped());
    }

    @Test void equippedRetainedHasteArmorDrivesNativeSlowImmunityPredicate() {
        RuntimeInventory inventory = new RuntimeInventory();
        long normal = addSourceArmor(inventory, 90_010, 1, false,
                RuntimeInventory.BLESSING_NORMAL, false);
        long haste = addSourceArmor(inventory, 90_011, 4, false,
                RuntimeInventory.BLESSING_NORMAL, true);
        assertFalse(inventory.hasEquippedHasteArmor());
        assertEquals(1, inventory.toggleSourceArmor(normal).size());
        assertFalse(inventory.hasEquippedHasteArmor());
        assertEquals(1, inventory.toggleSourceArmor(haste).size());
        assertTrue(inventory.hasEquippedHasteArmor());
        assertEquals(1, inventory.toggleSourceArmor(haste).size());
        assertFalse(inventory.hasEquippedHasteArmor());
    }

    @Test void sourceArmorUsesRetainedLayerAndDefensiveSlotDependencies() {
        RuntimeInventory inventory = new RuntimeInventory();
        long shirt = addSourceArmor(inventory, 91_001, 3, false, 1);
        long body = addSourceArmor(inventory, 91_002, 2, false, 1);
        long cloak = addSourceArmor(inventory, 91_003, 4, false, 1);
        long shield = addSourceArmor(inventory, 91_004, 7, false, 1);
        long guarder = addSourceArmor(inventory, 91_005, 13, false, 1);

        assertEquals(1, inventory.toggleSourceArmor(shirt).size());
        assertEquals(1, inventory.toggleSourceArmor(body).size());
        assertTrue(inventory.toggleSourceArmor(shirt).isEmpty());
        assertEquals(1, inventory.toggleSourceArmor(cloak).size());
        assertTrue(inventory.toggleSourceArmor(body).isEmpty());
        assertEquals(1, inventory.toggleSourceArmor(shield).size());
        assertTrue(inventory.toggleSourceArmor(guarder).isEmpty());
        assertEquals(4, inventory.equippedSourceArmorCount());
    }

    @Test void sourceArmorSeparatesAidShoulderAndTalismanPositions() {
        RuntimeInventory inventory = new RuntimeInventory();
        long aidOne = addSourceArmor(inventory, 92_001, 14, false, 1);
        long aidDuplicate = addSourceArmor(inventory, 92_001, 15, false, 1);
        long aidOther = addSourceArmor(inventory, 92_002, 15, false, 1);
        long aidThird = addSourceArmor(inventory, 92_003, 14, false, 1);
        long shoulder = addSourceArmor(inventory, 92_004, 23, false, 1);
        long shoulderDuplicate = addSourceArmor(inventory, 92_005, 23, false, 1);

        assertEquals(1, inventory.toggleSourceArmor(aidOne).size());
        assertTrue(inventory.toggleSourceArmor(aidDuplicate).isEmpty());
        assertEquals(1, inventory.toggleSourceArmor(aidOther).size());
        assertTrue(inventory.toggleSourceArmor(aidThird).isEmpty());
        assertEquals(1, inventory.toggleSourceArmor(shoulder).size());
        assertTrue(inventory.toggleSourceArmor(shoulderDuplicate).isEmpty());

        for (int index = 0; index < 5; index++) {
            long talisman = addSourceArmor(
                    inventory, 93_000 + index, 25, false, 1);
            if (index < 4) assertEquals(1,
                    inventory.toggleSourceArmor(talisman).size());
            else assertTrue(inventory.toggleSourceArmor(talisman).isEmpty());
        }
    }

    @Test void ckewRewardsRetainWeaponCombatAndManaCrystalArmorEffects() {
        long[] rewards = {
                InventoryCatalog.CKEW_CROWN_REWARD,
                InventoryCatalog.CKEW_KNIGHT_REWARD,
                InventoryCatalog.CKEW_ELF_SWORD_REWARD,
                InventoryCatalog.CKEW_ELF_BOW_REWARD
        };
        int[] small = {14, 16, 14, 3};
        int[] large = {14, 8, 6, 3};
        int[] hit = {1, 2, 0, 2};
        int[] damage = {0, 0, 0, 4};
        for (int index = 0; index < rewards.length; index++) {
            RuntimeInventory inventory = RuntimeInventory.fromCatalogCounts(
                    Map.of(rewards[index], 1));
            RuntimeInventory.Item reward = inventory.items().get(0);
            assertEquals(1, inventory.toggleWeapon(reward.objectId()).size());
            SourceWeaponPhysicalStats weapon = inventory.equippedPhysicalWeapon();
            assertEquals(rewards[index], weapon.itemId());
            assertEquals(small[index], weapon.damageSmall());
            assertEquals(large[index], weapon.damageLarge());
            assertEquals(hit[index], weapon.hitModifier());
            assertEquals(damage[index], weapon.damageModifier());
            assertEquals(index == 3 ? 15 : 1, weapon.range());
        }

        RuntimeInventory bow = RuntimeInventory.fromCatalogCounts(
                Map.of(InventoryCatalog.CKEW_ELF_BOW_REWARD, 1));
        InventoryCatalog.Definition arrow = new ClassicWorldDropCatalog.ItemWire(
                40743, "etc", "$61", "arrow", 7, 66, 0,
                true, true, 30, 0, "", 0, 0,
                "wood", -1, 0).inventoryDefinition();
        assertTrue(bow.add(arrow, 10));
        RuntimeInventory.Item bowItem = bow.items().stream()
                .filter(item -> item.definition().catalogId()
                        == InventoryCatalog.CKEW_ELF_BOW_REWARD)
                .findFirst().orElseThrow();
        bow.toggleWeapon(bowItem.objectId());
        SourceProjectilePhysicalStats projectile =
                bow.equippedPhysicalWeapon().projectile();
        assertTrue(projectile.present());
        assertEquals(40743, projectile.itemId());
        assertEquals(6, projectile.damageSmall());
        assertEquals(5, projectile.damageLarge());
        assertEquals(8, projectile.material());
        assertEquals(10, projectile.count());

        RuntimeInventory armor = RuntimeInventory.fromCatalogCounts(
                Map.of(InventoryCatalog.CKEW_WIZARD_REWARD, 1));
        RuntimeInventory.Item crystal = armor.items().get(0);
        assertEquals(1, armor.toggleNativeArmor(crystal.objectId()).size());
        RuntimeInventory.NativeArmorBonuses bonuses =
                armor.equippedNativeArmorBonuses();
        assertEquals(-2, bonuses.armorClassModifier());
        assertEquals(100, bonuses.maximumMp());
        assertEquals(0, bonuses.maximumHp());
        assertEquals(0, bonuses.charisma());
        assertEquals(1, armor.toggleNativeArmor(crystal.objectId()).size());
        assertEquals(new RuntimeInventory.NativeArmorBonuses(0, 0, 0, 0),
                armor.equippedNativeArmorBonuses());
    }

    private static long addSourceArmor(RuntimeInventory inventory, long catalogId,
                                       int armorType, boolean equipped, int blessing) {
        return addSourceArmor(
                inventory, catalogId, armorType, equipped, blessing, false);
    }

    private static long addSourceArmor(RuntimeInventory inventory, long catalogId,
                                       int armorType, boolean equipped, int blessing,
                                       boolean haste) {
        int useType = switch (armorType) {
            case 1 -> 22; case 2 -> 2; case 3 -> 18; case 4 -> 19;
            case 5 -> 20; case 6 -> 21; case 7, 13 -> 25;
            case 8 -> 24; case 9, 11 -> 23; case 10 -> 37;
            case 12 -> 40; case 14 -> 43; case 15 -> 44;
            case 17 -> 70; default -> 49;
        };
        InventoryCatalog.NativeArmorStatus status =
                new InventoryCatalog.NativeArmorStatus(
                        0, 20, 1_000, 0x7F, -1,
                        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, haste,
                        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, armorType);
        InventoryCatalog.Definition definition = new InventoryCatalog.Definition(
                catalogId, useType, 100, 101, blessing, true,
                "source-armor-" + catalogId, "", 0, 0, false, false,
                BattleActionCodes.WALK, 0, InventoryCatalog.Effect.NONE,
                0, 1_000, 0, null, status);
        return inventory.addDistinctState(definition,
                new RuntimeInventory.PersistentItem(
                        0, 1, 0, 0, true, blessing, true, equipped));
    }
}
