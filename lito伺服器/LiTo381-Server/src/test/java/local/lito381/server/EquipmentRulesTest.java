package local.lito381.server;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

final class EquipmentRulesTest {
    private static final EquipmentRules.LevelRange ALL_LEVELS = new EquipmentRules.LevelRange(1, Long.MAX_VALUE);
    private static final EquipmentRules.Policy RETAINED = new EquipmentRules.Policy(EquipmentRules.SealedItemPolicy.RETAINED_ALLOW);
    private static final EquipmentRules.Policy SEALED_BLOCKED = new EquipmentRules.Policy(EquipmentRules.SealedItemPolicy.REJECT_MUTATION);
    private static final EquipmentRules.Actor ACTOR = actor(EquipmentRules.CharacterClass.ROYAL, 60);

    @Test void classNumbersAndMasksAreThePersistedSevenClassOrder() {
        var classes = EquipmentRules.CharacterClass.values();
        assertArrayEquals(new int[] {0, 1, 2, 3, 4, 5, 6}, Arrays.stream(classes).mapToInt(EquipmentRules.CharacterClass::type).toArray());
        assertArrayEquals(new int[] {1, 2, 4, 8, 16, 32, 64}, Arrays.stream(classes).mapToInt(EquipmentRules.CharacterClass::mask).toArray());
        assertEquals(1, EquipmentRules.CharacterClass.KNIGHT.type());
        assertEquals(3, EquipmentRules.CharacterClass.WIZARD.type());
        for (var allowed : classes) {
            var loadout = loadout(weapon(1, 41, false, allowed.mask(), false));
            for (var requesting : classes) {
                var result = EquipmentRules.setEquipped(actor(requesting, 60), loadout, 1, true, RETAINED);
                if (requesting == allowed) assertInstanceOf(EquipmentRules.Transition.class, result);
                else assertReason(result, EquipmentRules.Reason.CLASS_NOT_ALLOWED);
            }
        }
    }

    @Test void katanaAndSmallKatanaUseExactCatalogClassMasksNotNameOrWeaponShape() {
        // 1-伺服器/db/380_db.sql:84644,84675. SQL order is royal,knight,mage,elf,...;
        // domain bit order is royal,knight,elf,mage,..., so copy-by-column-index is wrong.
        // item 10 dagger: 1,1,1,1,1,0,0 -> 0x1f; item 41 sword: 1,1,0,1,1,1,0 -> 0x37.
        var small = loadout(weapon(0x1_0000_0001L, 10, false, 0x1f, false));
        var katana = loadout(weapon(0x1_0000_0002L, 41, false, 0x37, false));
        var smallAllowed = List.of(EquipmentRules.CharacterClass.ROYAL, EquipmentRules.CharacterClass.KNIGHT,
                EquipmentRules.CharacterClass.ELF, EquipmentRules.CharacterClass.WIZARD, EquipmentRules.CharacterClass.DARK_ELF);
        var katanaAllowed = List.of(EquipmentRules.CharacterClass.ROYAL, EquipmentRules.CharacterClass.KNIGHT,
                EquipmentRules.CharacterClass.ELF, EquipmentRules.CharacterClass.DARK_ELF, EquipmentRules.CharacterClass.DRAGON_KNIGHT);
        for (var characterClass : EquipmentRules.CharacterClass.values()) {
            assertEquals(smallAllowed.contains(characterClass), EquipmentRules.setEquipped(actor(characterClass, 60), small,
                    0x1_0000_0001L, true, RETAINED) instanceof EquipmentRules.Transition);
            assertEquals(katanaAllowed.contains(characterClass), EquipmentRules.setEquipped(actor(characterClass, 60), katana,
                    0x1_0000_0002L, true, RETAINED) instanceof EquipmentRules.Transition);
        }
        assertReason(EquipmentRules.setEquipped(ACTOR, loadout(weapon(1, 41, false, 0, false)), 1, true, RETAINED),
                EquipmentRules.Reason.CLASS_NOT_ALLOWED);
    }

    @Test void armorAlsoRequiresItsExactClassMask() {
        var item = new EquipmentRules.Item(5, 500, false, EquipmentRules.CurseState.NOT_CURSED, EquipmentRules.SealState.UNSEALED,
                new EquipmentRules.ArmorFacts(EquipmentRules.CharacterClass.ILLUSIONIST.mask(), ALL_LEVELS, EquipmentRules.ArmorSlot.BODY));
        for (var characterClass : EquipmentRules.CharacterClass.values()) {
            var result = EquipmentRules.setEquipped(actor(characterClass, 60), loadout(item), 5, true, RETAINED);
            assertEquals(characterClass == EquipmentRules.CharacterClass.ILLUSIONIST, result instanceof EquipmentRules.Transition);
        }
    }

    @Test void levelBoundsAreInclusiveAndNeverNarrowedToNativeIntegers() {
        long minimum = (long) Integer.MAX_VALUE + 400;
        long maximum = Long.MAX_VALUE - 1;
        var facts = new EquipmentRules.WeaponFacts(0x7f, new EquipmentRules.LevelRange(minimum, maximum), false);
        var loadout = loadout(item(Long.MAX_VALUE, Long.MAX_VALUE, false, facts));
        assertReason(EquipmentRules.setEquipped(actor(EquipmentRules.CharacterClass.ROYAL, minimum - 1), loadout,
                Long.MAX_VALUE, true, RETAINED), EquipmentRules.Reason.LEVEL_TOO_LOW);
        assertInstanceOf(EquipmentRules.Transition.class, EquipmentRules.setEquipped(actor(EquipmentRules.CharacterClass.ROYAL, minimum),
                loadout, Long.MAX_VALUE, true, RETAINED));
        assertInstanceOf(EquipmentRules.Transition.class, EquipmentRules.setEquipped(actor(EquipmentRules.CharacterClass.ROYAL, maximum),
                loadout, Long.MAX_VALUE, true, RETAINED));
        assertReason(EquipmentRules.setEquipped(actor(EquipmentRules.CharacterClass.ROYAL, Long.MAX_VALUE), loadout,
                Long.MAX_VALUE, true, RETAINED), EquipmentRules.Reason.LEVEL_TOO_HIGH);
        assertInstanceOf(EquipmentRules.Transition.class, EquipmentRules.setEquipped(actor(EquipmentRules.CharacterClass.ROYAL, Long.MAX_VALUE),
                loadout(weapon(1, 1, false, 0x7f, false)), 1, true, RETAINED));
    }

    @Test void removalIsNotBlockedByChangedClassOrLevelEligibility() {
        var facts = new EquipmentRules.ArmorFacts(EquipmentRules.CharacterClass.WIZARD.mask(),
                new EquipmentRules.LevelRange(100, 200), EquipmentRules.ArmorSlot.HELMET);
        var original = loadout(item(1, 1, true, facts));
        var transition = accepted(EquipmentRules.setEquipped(ACTOR, original, 1, false, RETAINED));
        assertFalse(transition.candidate().item(1).equipped());
        assertReason(EquipmentRules.setEquipped(ACTOR, transition.candidate(), 1, true, RETAINED), EquipmentRules.Reason.CLASS_NOT_ALLOWED);
    }

    @Test void cursedWeaponCanBeEquippedButCannotBeRemovedOrReplaced() {
        var cursed = withStates(weapon(1, 41, false, 0x7f, false), EquipmentRules.CurseState.CURSED, EquipmentRules.SealState.UNSEALED);
        var initial = loadout(cursed, weapon(2, 10, false, 0x7f, false));
        var equipped = accepted(EquipmentRules.setEquipped(ACTOR, initial, 1, true, RETAINED)).candidate();
        assertReason(EquipmentRules.setEquipped(ACTOR, equipped, 1, false, RETAINED), EquipmentRules.Reason.CURSED);
        var rejection = assertReason(EquipmentRules.setEquipped(ACTOR, equipped, 2, true, RETAINED), EquipmentRules.Reason.CURSED);
        assertEquals(1, rejection.blockingObjectId());
        assertTrue(equipped.item(1).equipped());
        assertFalse(equipped.item(2).equipped());
    }

    @Test void cursedArmorRemovalDoesNotMutateTheSnapshot() {
        var original = loadout(withStates(armor(1, 1, EquipmentRules.ArmorSlot.RING, true),
                EquipmentRules.CurseState.CURSED, EquipmentRules.SealState.UNSEALED));
        assertReason(EquipmentRules.setEquipped(ACTOR, original, 1, false, RETAINED), EquipmentRules.Reason.CURSED);
        assertTrue(original.item(1).equipped());
    }

    @Test void weaponReplacementReturnsOneAtomicSortedCandidateAndTwoChanges() {
        var original = loadout(weapon(20, 41, true, 0x7f, false), weapon(10, 10, false, 0x7f, false));
        var transition = accepted(EquipmentRules.setEquipped(ACTOR, original, 10, true, RETAINED));
        assertEquals(List.of(new EquipmentRules.Change(10, true), new EquipmentRules.Change(20, false)), transition.changes());
        assertTrue(transition.candidate().item(10).equipped());
        assertFalse(transition.candidate().item(20).equipped());
        assertFalse(original.item(10).equipped());
        assertTrue(original.item(20).equipped());
        assertEquals(1, transition.candidate().items().stream().filter(EquipmentRules.Item::equipped).count());
    }

    @Test void twoHandedWeaponAndShieldAreRejectedInBothDirections() {
        var shieldFirst = loadout(weapon(1, 1, false, 0x7f, true), armor(2, 2, EquipmentRules.ArmorSlot.SHIELD, true));
        assertEquals(2, assertReason(EquipmentRules.setEquipped(ACTOR, shieldFirst, 1, true, RETAINED),
                EquipmentRules.Reason.TWO_HANDED_SHIELD_CONFLICT).blockingObjectId());
        var weaponFirst = loadout(weapon(1, 1, true, 0x7f, true), armor(2, 2, EquipmentRules.ArmorSlot.SHIELD, false));
        assertEquals(1, assertReason(EquipmentRules.setEquipped(ACTOR, weaponFirst, 2, true, RETAINED),
                EquipmentRules.Reason.TWO_HANDED_SHIELD_CONFLICT).blockingObjectId());
        var failedReplacement = loadout(weapon(1, 1, true, 0x7f, false), armor(2, 2, EquipmentRules.ArmorSlot.SHIELD, true),
                weapon(3, 3, false, 0x7f, true));
        assertReason(EquipmentRules.setEquipped(ACTOR, failedReplacement, 3, true, RETAINED), EquipmentRules.Reason.TWO_HANDED_SHIELD_CONFLICT);
        assertTrue(failedReplacement.item(1).equipped());
    }

    @Test void guarderCanCoexistWithTwoHandedWeaponInEitherOrder() {
        for (boolean weaponFirst : List.of(false, true)) {
            var initial = loadout(weapon(1, 1, weaponFirst, 0x7f, true),
                    armor(2, 2, EquipmentRules.ArmorSlot.GUARDER, !weaponFirst));
            var transition = accepted(EquipmentRules.setEquipped(ACTOR, initial, weaponFirst ? 2 : 1, true, RETAINED));
            assertTrue(transition.candidate().item(1).equipped());
            assertTrue(transition.candidate().item(2).equipped());
        }
    }

    @Test void shieldAndGuarderMutuallyExcludeAndNeverAutoUnequipArmor() {
        for (boolean shieldFirst : List.of(false, true)) {
            var initial = loadout(armor(1, 1, EquipmentRules.ArmorSlot.SHIELD, shieldFirst),
                    armor(2, 2, EquipmentRules.ArmorSlot.GUARDER, !shieldFirst));
            assertReason(EquipmentRules.setEquipped(ACTOR, initial, shieldFirst ? 2 : 1, true, RETAINED),
                    EquipmentRules.Reason.SHIELD_GUARDER_CONFLICT);
        }
    }

    @Test void bothRetainedRingTypesShareExactlyTwoPositions() {
        var original = loadout(armor(1, 100, EquipmentRules.ArmorSlot.RING, true),
                armor(2, 100, EquipmentRules.ArmorSlot.RING_2, false), armor(3, 101, EquipmentRules.ArmorSlot.RING, false));
        var equipped = accepted(EquipmentRules.setEquipped(ACTOR, original, 2, true, RETAINED)).candidate();
        assertReason(EquipmentRules.setEquipped(ACTOR, equipped, 3, true, RETAINED), EquipmentRules.Reason.RING_LIMIT);
        var removed = accepted(EquipmentRules.setEquipped(ACTOR, equipped, 1, false, RETAINED)).candidate();
        assertTrue(accepted(EquipmentRules.setEquipped(ACTOR, removed, 3, true, RETAINED)).candidate().item(3).equipped());
    }

    @Test void allSingleArmorSlotsRejectReplacementAndRetainTheirOwnIdentities() {
        for (var slot : EquipmentRules.ArmorSlot.values()) {
            if (List.of(EquipmentRules.ArmorSlot.RING, EquipmentRules.ArmorSlot.RING_2, EquipmentRules.ArmorSlot.AID_LEFT,
                    EquipmentRules.ArmorSlot.AID_RIGHT, EquipmentRules.ArmorSlot.TALISMAN_5,
                    EquipmentRules.ArmorSlot.SHIELD, EquipmentRules.ArmorSlot.GUARDER).contains(slot)) continue;
            var initial = loadout(armor(1, 1, slot, true), armor(2, 2, slot, false));
            assertReason(EquipmentRules.setEquipped(ACTOR, initial, 2, true, RETAINED), EquipmentRules.Reason.SLOT_OCCUPIED);
            assertTrue(initial.item(1).equipped());
        }
        assertEquals(23, EquipmentRules.ArmorSlot.values().length);
        assertEquals(23, Arrays.stream(EquipmentRules.ArmorSlot.values()).map(EquipmentRules.ArmorSlot::retainedType).distinct().count());
    }

    @Test void aidSlotsShareCapacityTwoButRequireDifferentCatalogIds() {
        var initial = loadout(armor(1, 100, EquipmentRules.ArmorSlot.AID_LEFT, true),
                armor(2, 100, EquipmentRules.ArmorSlot.AID_RIGHT, false), armor(3, 101, EquipmentRules.ArmorSlot.AID_LEFT, false),
                armor(4, 102, EquipmentRules.ArmorSlot.AID_RIGHT, false));
        assertReason(EquipmentRules.setEquipped(ACTOR, initial, 2, true, RETAINED), EquipmentRules.Reason.DUPLICATE_AID_CATALOG);
        var full = accepted(EquipmentRules.setEquipped(ACTOR, initial, 3, true, RETAINED)).candidate();
        assertReason(EquipmentRules.setEquipped(ACTOR, full, 4, true, RETAINED), EquipmentRules.Reason.AID_LIMIT);
    }

    @Test void namedAndGenericTalismanTopologyIsExplicitlyTheCurrentLitoBaseNotExpandedClassic() {
        var items = new ArrayList<EquipmentRules.Item>();
        for (int i = 0; i < 5; i++) items.add(armor(100 + i, 200 + i, EquipmentRules.ArmorSlot.TALISMAN_5, i < 4));
        items.add(armor(1, 1, EquipmentRules.ArmorSlot.TALISMAN_1, true));
        items.add(armor(2, 2, EquipmentRules.ArmorSlot.TALISMAN_2, true));
        items.add(armor(3, 3, EquipmentRules.ArmorSlot.TALISMAN_3, true));
        items.add(armor(4, 4, EquipmentRules.ArmorSlot.TALISMAN_4, true));
        var full = new EquipmentRules.Loadout(items);
        assertReason(EquipmentRules.setEquipped(ACTOR, full, 104, true, RETAINED), EquipmentRules.Reason.TALISMAN_LIMIT);
        var removed = accepted(EquipmentRules.setEquipped(ACTOR, full, 100, false, RETAINED)).candidate();
        assertTrue(accepted(EquipmentRules.setEquipped(ACTOR, removed, 104, true, RETAINED)).candidate().item(104).equipped());
    }

    @Test void shirtBodyCloakLayeringRequiresCorrectEquipAndRemovalOrder() {
        var current = loadout(armor(1, 1, EquipmentRules.ArmorSlot.SHIRT, false),
                armor(2, 2, EquipmentRules.ArmorSlot.BODY, false), armor(3, 3, EquipmentRules.ArmorSlot.CLOAK, false));
        current = accepted(EquipmentRules.setEquipped(ACTOR, current, 1, true, RETAINED)).candidate();
        current = accepted(EquipmentRules.setEquipped(ACTOR, current, 2, true, RETAINED)).candidate();
        assertReason(EquipmentRules.setEquipped(ACTOR, current, 1, false, RETAINED), EquipmentRules.Reason.LAYER_DEPENDENCY);
        current = accepted(EquipmentRules.setEquipped(ACTOR, current, 3, true, RETAINED)).candidate();
        assertReason(EquipmentRules.setEquipped(ACTOR, current, 1, false, RETAINED), EquipmentRules.Reason.LAYER_DEPENDENCY);
        assertReason(EquipmentRules.setEquipped(ACTOR, current, 2, false, RETAINED), EquipmentRules.Reason.LAYER_DEPENDENCY);
        current = accepted(EquipmentRules.setEquipped(ACTOR, current, 3, false, RETAINED)).candidate();
        current = accepted(EquipmentRules.setEquipped(ACTOR, current, 2, false, RETAINED)).candidate();
        current = accepted(EquipmentRules.setEquipped(ACTOR, current, 1, false, RETAINED)).candidate();
        assertTrue(current.items().stream().noneMatch(EquipmentRules.Item::equipped));
    }

    @Test void shirtCannotBeInsertedUnderBodyOrCloakAndBodyCannotBeInsertedUnderCloak() {
        for (var covering : List.of(EquipmentRules.ArmorSlot.BODY, EquipmentRules.ArmorSlot.CLOAK)) {
            assertReason(EquipmentRules.setEquipped(ACTOR, loadout(armor(1, 1, EquipmentRules.ArmorSlot.SHIRT, false),
                    armor(2, 2, covering, true)), 1, true, RETAINED), EquipmentRules.Reason.LAYER_DEPENDENCY);
        }
        assertReason(EquipmentRules.setEquipped(ACTOR, loadout(armor(1, 1, EquipmentRules.ArmorSlot.BODY, false),
                armor(2, 2, EquipmentRules.ArmorSlot.CLOAK, true)), 1, true, RETAINED), EquipmentRules.Reason.LAYER_DEPENDENCY);
    }

    @Test void sameDesiredStateIsIdempotentEvenForCursedSealedOrNowIneligibleItems() {
        var requested = withStates(weapon(1, 41, true, EquipmentRules.CharacterClass.WIZARD.mask(), false),
                EquipmentRules.CurseState.CURSED, EquipmentRules.SealState.SEALED);
        var current = loadout(requested);
        var same = accepted(EquipmentRules.setEquipped(ACTOR, current, 1, true, SEALED_BLOCKED));
        assertSame(current, same.candidate());
        assertFalse(same.changed());
        assertEquals(List.of(), same.changes());
        var unequipped = loadout(requested.withEquipped(false));
        assertFalse(accepted(EquipmentRules.setEquipped(ACTOR, unequipped, 1, false, SEALED_BLOCKED)).changed());
        var initial = loadout(weapon(1, 1, false, 0x7f, false));
        var committed = accepted(EquipmentRules.setEquipped(ACTOR, initial, 1, true, RETAINED)).candidate();
        assertFalse(accepted(EquipmentRules.setEquipped(ACTOR, committed, 1, true, RETAINED)).changed());
    }

    @Test void sealedMutationPolicyIsRequiredAndAppliedToDirectAndImplicitChanges() {
        var sealed = withStates(weapon(1, 41, false, 0x7f, false), EquipmentRules.CurseState.NOT_CURSED, EquipmentRules.SealState.SEALED);
        var original = loadout(sealed, weapon(2, 10, false, 0x7f, false));
        assertReason(EquipmentRules.setEquipped(ACTOR, original, 1, true, SEALED_BLOCKED), EquipmentRules.Reason.SEALED);
        var equipped = accepted(EquipmentRules.setEquipped(ACTOR, original, 1, true, RETAINED)).candidate();
        assertReason(EquipmentRules.setEquipped(ACTOR, equipped, 1, false, SEALED_BLOCKED), EquipmentRules.Reason.SEALED);
        assertReason(EquipmentRules.setEquipped(ACTOR, equipped, 2, true, SEALED_BLOCKED), EquipmentRules.Reason.SEALED);
        assertFalse(accepted(EquipmentRules.setEquipped(ACTOR, equipped, 1, false, RETAINED)).candidate().item(1).equipped());
    }

    @Test void unknownTemplateCurseSealAndImplicitlyReplacedWeaponFactsFailClosed() {
        for (var unknown : List.of(item(1, 1, false, EquipmentRules.UnknownFacts.INSTANCE),
                withStates(weapon(1, 1, false, 0x7f, false), EquipmentRules.CurseState.UNKNOWN, EquipmentRules.SealState.UNSEALED),
                withStates(weapon(1, 1, false, 0x7f, false), EquipmentRules.CurseState.NOT_CURSED, EquipmentRules.SealState.UNKNOWN))) {
            assertReason(EquipmentRules.setEquipped(ACTOR, loadout(unknown), 1, true, RETAINED), EquipmentRules.Reason.UNQUALIFIED_ITEM);
            assertReason(EquipmentRules.setEquipped(ACTOR, loadout(unknown), 1, false, RETAINED), EquipmentRules.Reason.UNQUALIFIED_ITEM);
        }
        var otherUnknown = loadout(weapon(1, 1, false, 0x7f, false), item(2, 2, true, EquipmentRules.UnknownFacts.INSTANCE));
        assertReason(EquipmentRules.setEquipped(ACTOR, otherUnknown, 1, true, RETAINED), EquipmentRules.Reason.UNQUALIFIED_EQUIPPED_ITEM);
        var replacement = loadout(weapon(1, 1, false, 0x7f, false),
                withStates(weapon(2, 2, true, 0x7f, false), EquipmentRules.CurseState.UNKNOWN, EquipmentRules.SealState.UNSEALED));
        assertEquals(2, assertReason(EquipmentRules.setEquipped(ACTOR, replacement, 1, true, RETAINED), EquipmentRules.Reason.UNQUALIFIED_ITEM).blockingObjectId());
    }

    @Test void unrelatedUnequippedUnknownAndKnownNonEquipmentDoNotInventSlotsOrAbilities() {
        var initial = loadout(weapon(1, 1, false, 0x7f, false), item(2, 2, false, EquipmentRules.UnknownFacts.INSTANCE),
                item(3, 3, true, EquipmentRules.NonEquipmentFacts.INSTANCE));
        var candidate = accepted(EquipmentRules.setEquipped(ACTOR, initial, 1, true, RETAINED)).candidate();
        // A qualified no-slot activation marker is preserved, not certified as an
        // ability-free etcitem. The rule result grants no general item-use operation.
        assertEquals(initial.item(3), candidate.item(3));
        assertReason(EquipmentRules.setEquipped(ACTOR, initial, 3, false, RETAINED), EquipmentRules.Reason.NOT_EQUIPMENT);
        assertReason(EquipmentRules.setEquipped(ACTOR, initial, 0, true, RETAINED), EquipmentRules.Reason.INVALID_REQUEST);
        assertReason(EquipmentRules.setEquipped(ACTOR, initial, 999, true, RETAINED), EquipmentRules.Reason.UNKNOWN_ITEM);
    }

    @Test void impossibleExistingTopologyIsNeverSilentlyRepairedByAnEquipOrNoop() {
        var invalid = List.of(
                loadout(weapon(1, 1, true, 0x7f, false), weapon(2, 2, true, 0x7f, false)),
                loadout(armor(1, 1, EquipmentRules.ArmorSlot.HELMET, true), armor(2, 2, EquipmentRules.ArmorSlot.HELMET, true)),
                loadout(armor(1, 1, EquipmentRules.ArmorSlot.SHIELD, true), armor(2, 2, EquipmentRules.ArmorSlot.GUARDER, true)),
                loadout(weapon(1, 1, true, 0x7f, true), armor(2, 2, EquipmentRules.ArmorSlot.SHIELD, true)),
                loadout(armor(1, 100, EquipmentRules.ArmorSlot.AID_LEFT, true), armor(2, 100, EquipmentRules.ArmorSlot.AID_RIGHT, true)),
                loadout(armor(1, 1, EquipmentRules.ArmorSlot.RING, true), armor(2, 2, EquipmentRules.ArmorSlot.RING, true),
                        armor(3, 3, EquipmentRules.ArmorSlot.RING_2, true)));
        for (var initial : invalid) {
            assertReason(EquipmentRules.setEquipped(ACTOR, initial, 1, false, RETAINED), EquipmentRules.Reason.INVALID_LOADOUT);
            assertReason(EquipmentRules.setEquipped(ACTOR, initial, 1, true, RETAINED), EquipmentRules.Reason.INVALID_LOADOUT);
        }
    }

    @Test void full512ItemLoadoutAndMaximumI64IdentitiesRemainExactAndImmutable() {
        var source = new ArrayList<EquipmentRules.Item>();
        for (int i = 0; i < 511; i++) source.add(item(0x1_0000_0000L + i, 0x2_0000_0000L + i, false, EquipmentRules.NonEquipmentFacts.INSTANCE));
        source.add(weapon(Long.MAX_VALUE, Long.MAX_VALUE, false, 0x7f, false));
        var original = new EquipmentRules.Loadout(source);
        source.clear();
        var transition = accepted(EquipmentRules.setEquipped(actor(EquipmentRules.CharacterClass.ROYAL, Long.MAX_VALUE),
                original, Long.MAX_VALUE, true, RETAINED));
        assertEquals(512, original.items().size());
        assertEquals(512, transition.candidate().items().size());
        assertEquals(Long.MAX_VALUE, transition.candidate().items().get(511).objectId());
        assertEquals(Long.MAX_VALUE, transition.candidate().items().get(511).catalogId());
        assertEquals(List.of(new EquipmentRules.Change(Long.MAX_VALUE, true)), transition.changes());
        assertThrows(UnsupportedOperationException.class, () -> transition.changes().clear());
        assertThrows(UnsupportedOperationException.class, () -> original.items().clear());
        assertFalse(original.item(Long.MAX_VALUE).equipped());
        var tooMany = new ArrayList<>(original.items());
        tooMany.add(item(1, 1, false, EquipmentRules.NonEquipmentFacts.INSTANCE));
        assertThrows(IllegalArgumentException.class, () -> new EquipmentRules.Loadout(tooMany));
    }

    @Test void malformedInputsCannotBecomeDefaultPermissionOrUnknownSlotZero() {
        assertThrows(IllegalArgumentException.class, () -> new EquipmentRules.Actor(EquipmentRules.CharacterClass.ROYAL, 0));
        assertThrows(NullPointerException.class, () -> new EquipmentRules.Actor(null, 1));
        assertThrows(IllegalArgumentException.class, () -> new EquipmentRules.LevelRange(0, Long.MAX_VALUE));
        assertThrows(IllegalArgumentException.class, () -> new EquipmentRules.LevelRange(10, 9));
        assertThrows(IllegalArgumentException.class, () -> new EquipmentRules.WeaponFacts(128, ALL_LEVELS, false));
        assertThrows(IllegalArgumentException.class, () -> new EquipmentRules.WeaponFacts(-1, ALL_LEVELS, false));
        assertThrows(NullPointerException.class, () -> new EquipmentRules.WeaponFacts(127, null, false));
        assertThrows(NullPointerException.class, () -> new EquipmentRules.ArmorFacts(127, ALL_LEVELS, null));
        assertThrows(NullPointerException.class, () -> new EquipmentRules.Policy(null));
        assertThrows(IllegalArgumentException.class, () -> weapon(0, 1, false, 127, false));
        assertThrows(IllegalArgumentException.class, () -> weapon(1, -1, false, 127, false));
        assertThrows(NullPointerException.class, () -> item(1, 1, false, null));
        assertThrows(IllegalArgumentException.class, () -> loadout(weapon(1, 1, false, 127, false), weapon(1, 2, false, 127, false)));
        assertThrows(NullPointerException.class, () -> new EquipmentRules.Loadout(Arrays.asList((EquipmentRules.Item) null)));
        assertEquals(List.of(), new EquipmentRules.Loadout(List.of()).items());
    }

    @Test void supportedArmorTransitionsMatchExistingNativeRuntimeWithoutChangingNativeImplementation() {
        RuntimeInventory nativeInventory = new RuntimeInventory();
        int[] slots = {3, 2, 4, 7, 13, 9, 11, 9, 14, 15, 15, 23, 23, 25, 25, 25, 25, 25};
        long[] catalogs = {1, 2, 3, 4, 5, 6, 7, 8, 100, 100, 101, 11, 12, 13, 14, 15, 16, 17};
        long[] ids = new long[slots.length];
        var items = new ArrayList<EquipmentRules.Item>();
        for (int i = 0; i < slots.length; i++) {
            ids[i] = addNativeArmor(nativeInventory, 90_000 + catalogs[i], slots[i]);
            final int retainedType = slots[i];
            var slot = Arrays.stream(EquipmentRules.ArmorSlot.values()).filter(value -> value.retainedType() == retainedType).findFirst().orElseThrow();
            items.add(armor(ids[i], 90_000 + catalogs[i], slot, false));
        }
        var current = new EquipmentRules.Loadout(items);
        int[] sequence = {0, 1, 0, 2, 1, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 2, 1, 0};
        for (int index : sequence) {
            long id = ids[index];
            boolean desired = !current.item(id).equipped();
            var result = EquipmentRules.setEquipped(ACTOR, current, id, desired, RETAINED);
            boolean nativeAccepted = !nativeInventory.toggleSourceArmor(id).isEmpty();
            assertEquals(nativeAccepted, result instanceof EquipmentRules.Transition, "retained slot " + slots[index]);
            if (result instanceof EquipmentRules.Transition transition) current = transition.candidate();
            for (long itemId : ids) assertEquals(nativeInventory.item(itemId).equipped(), current.item(itemId).equipped());
        }
    }

    private static EquipmentRules.Actor actor(EquipmentRules.CharacterClass characterClass, long level) {
        return new EquipmentRules.Actor(characterClass, level);
    }
    private static EquipmentRules.Item item(long id, long catalog, boolean equipped, EquipmentRules.Facts facts) {
        return new EquipmentRules.Item(id, catalog, equipped, EquipmentRules.CurseState.NOT_CURSED, EquipmentRules.SealState.UNSEALED, facts);
    }
    private static EquipmentRules.Item weapon(long id, long catalog, boolean equipped, int mask, boolean twoHanded) {
        return item(id, catalog, equipped, new EquipmentRules.WeaponFacts(mask, ALL_LEVELS, twoHanded));
    }
    private static EquipmentRules.Item armor(long id, long catalog, EquipmentRules.ArmorSlot slot, boolean equipped) {
        return item(id, catalog, equipped, new EquipmentRules.ArmorFacts(127, ALL_LEVELS, slot));
    }
    private static EquipmentRules.Item withStates(EquipmentRules.Item item, EquipmentRules.CurseState curse, EquipmentRules.SealState seal) {
        return new EquipmentRules.Item(item.objectId(), item.catalogId(), item.equipped(), curse, seal, item.facts());
    }
    private static EquipmentRules.Loadout loadout(EquipmentRules.Item... items) { return new EquipmentRules.Loadout(List.of(items)); }
    private static EquipmentRules.Transition accepted(EquipmentRules.Result result) {
        return assertInstanceOf(EquipmentRules.Transition.class, result);
    }
    private static EquipmentRules.Rejection assertReason(EquipmentRules.Result result, EquipmentRules.Reason reason) {
        var rejection = assertInstanceOf(EquipmentRules.Rejection.class, result);
        assertEquals(reason, rejection.reason());
        return rejection;
    }
    private static long addNativeArmor(RuntimeInventory inventory, long catalogId, int armorType) {
        int useType = switch (armorType) {
            case 1 -> 22; case 2 -> 2; case 3 -> 18; case 4 -> 19; case 5 -> 20; case 6 -> 21;
            case 7, 13 -> 25; case 8 -> 24; case 9, 11 -> 23; case 10 -> 37; case 12 -> 40;
            case 14 -> 43; case 15 -> 44; case 17 -> 70; default -> 49;
        };
        var status = new InventoryCatalog.NativeArmorStatus(0, 20, 1_000, 127, -1,
                0, 0, 0, 0, 0, 0, 0, 0, 0, 0, false, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, armorType);
        var definition = new InventoryCatalog.Definition(catalogId, useType, 100, 101, 1, true,
                "rule-fixture-" + catalogId, "", 0, 0, false, false, BattleActionCodes.WALK,
                0, InventoryCatalog.Effect.NONE, 0, 1_000, 0, null, status);
        return inventory.addDistinctState(definition, new RuntimeInventory.PersistentItem(0, 1, 0, 0, true, 1, true, false));
    }
}
