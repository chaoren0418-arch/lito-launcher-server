package local.lito381.server;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Transport/storage-independent equipment topology. This does not qualify ownership,
 * evaluate item abilities, persist a change, or grant a live-session mutation.
 * Callers must do those jobs before committing an accepted candidate.
 *
 * <p>The fixed base topology follows RuntimeInventory.toggleNativeArmor: two rings,
 * one earring, two distinct-catalog aid items, one of each named talisman/shoulder,
 * and four type-25 talismans. It does not infer later classic ring/earring expansion
 * or the newer shared type-19..25 slot allocation from missing database facts.
 * Retained C_ItemUSe additionally supplies cursed-weapon removal and two-hand/shield
 * gates. A guarder is not a shield: it may coexist with a two-handed weapon.</p>
 */
final class EquipmentRules {
    static final int MAX_ITEMS = 512;

    enum CharacterClass {
        ROYAL(0), KNIGHT(1), ELF(2), WIZARD(3), DARK_ELF(4), DRAGON_KNIGHT(5), ILLUSIONIST(6);
        private final int type;
        CharacterClass(int type) { this.type = type; }
        int type() { return type; }
        int mask() { return 1 << type; }
    }

    record Actor(CharacterClass characterClass, long level) {
        Actor {
            Objects.requireNonNull(characterClass, "character class");
            if (level < 1) throw new IllegalArgumentException("level must be positive");
        }
    }

    /** No sentinel zero: a catalog adapter must explicitly resolve unlimited bounds. */
    record LevelRange(long minimum, long maximum) {
        LevelRange {
            if (minimum < 1 || maximum < minimum) {
                throw new IllegalArgumentException("invalid equipment level range");
            }
        }
    }

    enum ArmorSlot {
        HELMET(1), BODY(2), SHIRT(3), CLOAK(4), GLOVES(5), BOOTS(6), SHIELD(7),
        AMULET(8), RING(9), BELT(10), RING_2(11), EARRING(12), GUARDER(13),
        AID_LEFT(14), AID_RIGHT(15), PANTS(17), TALISMAN_1(19), TALISMAN_2(20),
        TALISMAN_3(21), TALISMAN_4(22), SHOULDER_LEFT(23), SHOULDER_RIGHT(24), TALISMAN_5(25);
        private final int retainedType;
        ArmorSlot(int retainedType) { this.retainedType = retainedType; }
        int retainedType() { return retainedType; }
    }

    sealed interface Facts permits WeaponFacts, ArmorFacts, UnknownFacts, NonEquipmentFacts {}

    record WeaponFacts(int classMask, LevelRange levelRange, boolean twoHanded) implements Facts {
        WeaponFacts { validatePermission(classMask, levelRange); }
    }

    record ArmorFacts(int classMask, LevelRange levelRange, ArmorSlot slot) implements Facts {
        ArmorFacts {
            validatePermission(classMask, levelRange);
            Objects.requireNonNull(slot, "armor slot");
        }
    }

    /** Missing/unverified facts must never become an all-zero, unrestricted template. */
    enum UnknownFacts implements Facts { INSTANCE }
    /**
     * Explicitly qualified as occupying no weapon/armor slot, not merely an SQL
     * etcitem row. An equipped marker may represent a light or another activation
     * state and is preserved, never toggled here. This says nothing about ability
     * effects (for example equipped etcitem cards/VIP); the ability evaluator must
     * still qualify those. Use UnknownFacts when the marker semantics are unproved.
     */
    enum NonEquipmentFacts implements Facts { INSTANCE }
    enum CurseState { NOT_CURSED, CURSED, UNKNOWN }
    enum SealState { UNSEALED, SEALED, UNKNOWN }

    /**
     * The retained use/equip routes do not establish a sealed-item equipment ban.
     * A modern caller must choose its policy explicitly; neither is a default.
     */
    enum SealedItemPolicy { RETAINED_ALLOW, REJECT_MUTATION }
    record Policy(SealedItemPolicy sealedItems) {
        Policy { Objects.requireNonNull(sealedItems, "sealed-item policy"); }
    }

    record Item(long objectId, long catalogId, boolean equipped, CurseState curse,
                SealState seal, Facts facts) {
        Item {
            if (objectId < 1 || catalogId < 1) {
                throw new IllegalArgumentException("item identities must be positive i64");
            }
            Objects.requireNonNull(curse, "curse qualification");
            Objects.requireNonNull(seal, "seal qualification");
            Objects.requireNonNull(facts, "equipment facts");
        }
        Item withEquipped(boolean desired) {
            return new Item(objectId, catalogId, desired, curse, seal, facts);
        }
    }

    /** Canonical, immutable, bounded input; quantities/stat values are not narrowed here. */
    record Loadout(List<Item> items) {
        Loadout {
            Objects.requireNonNull(items, "items");
            if (items.size() > MAX_ITEMS) throw new IllegalArgumentException("too many equipment items");
            var sorted = new ArrayList<Item>(items.size());
            Set<Long> ids = new HashSet<>();
            for (Item item : items) {
                Objects.requireNonNull(item, "item");
                if (!ids.add(item.objectId())) throw new IllegalArgumentException("duplicate item identity");
                sorted.add(item);
            }
            sorted.sort(Comparator.comparingLong(Item::objectId));
            items = List.copyOf(sorted);
        }
        Item item(long objectId) {
            for (Item item : items) if (item.objectId() == objectId) return item;
            return null;
        }
    }

    enum Reason {
        INVALID_REQUEST, UNKNOWN_ITEM, UNQUALIFIED_ITEM, NOT_EQUIPMENT,
        UNQUALIFIED_EQUIPPED_ITEM, INVALID_LOADOUT, CLASS_NOT_ALLOWED,
        LEVEL_TOO_LOW, LEVEL_TOO_HIGH, CURSED, SEALED, SLOT_OCCUPIED,
        RING_LIMIT, AID_LIMIT, DUPLICATE_AID_CATALOG, TALISMAN_LIMIT,
        SHIELD_GUARDER_CONFLICT, TWO_HANDED_SHIELD_CONFLICT, LAYER_DEPENDENCY
    }

    sealed interface Result permits Transition, Rejection {}
    record Change(long objectId, boolean equipped) {}
    /** Changes and resulting items are sorted by i64 object ID; not packet-send order. */
    record Transition(Loadout candidate, List<Change> changes) implements Result {
        Transition {
            Objects.requireNonNull(candidate, "candidate");
            changes = List.copyOf(changes);
        }
        boolean changed() { return !changes.isEmpty(); }
    }
    /** blockingObjectId is zero only when no inventory instance identifies the rejection. */
    record Rejection(Reason reason, long blockingObjectId) implements Result {
        Rejection {
            Objects.requireNonNull(reason, "rejection reason");
            if (blockingObjectId < 0) throw new IllegalArgumentException("invalid blocking identity");
        }
    }

    private EquipmentRules() {}

    static Result setEquipped(Actor actor, Loadout current, long objectId,
                              boolean desired, Policy policy) {
        Objects.requireNonNull(actor, "actor");
        Objects.requireNonNull(current, "loadout");
        Objects.requireNonNull(policy, "policy");
        if (objectId < 1) return reject(Reason.INVALID_REQUEST, 0);
        Item requested = current.item(objectId);
        if (requested == null) return reject(Reason.UNKNOWN_ITEM, objectId);
        if (requested.facts() == UnknownFacts.INSTANCE) return reject(Reason.UNQUALIFIED_ITEM, objectId);
        if (requested.facts() == NonEquipmentFacts.INSTANCE) return reject(Reason.NOT_EQUIPMENT, objectId);
        Rejection invalid = validateTopology(current);
        if (invalid != null) return invalid;
        if (requested.curse() == CurseState.UNKNOWN || requested.seal() == SealState.UNKNOWN) {
            return reject(Reason.UNQUALIFIED_ITEM, objectId);
        }
        // Idempotent acknowledgement is not a new equip attempt: no class/level/curse
        // or sealed policy can turn an already confirmed desired state into a toggle.
        if (requested.equipped() == desired) return new Transition(current, List.of());
        Rejection stateGate = changingItemGate(requested, desired, policy);
        if (stateGate != null) return stateGate;
        if (desired) {
            int mask;
            LevelRange range;
            if (requested.facts() instanceof WeaponFacts weapon) {
                mask = weapon.classMask(); range = weapon.levelRange();
            } else {
                ArmorFacts armor = (ArmorFacts) requested.facts();
                mask = armor.classMask(); range = armor.levelRange();
            }
            if ((mask & actor.characterClass().mask()) == 0) return reject(Reason.CLASS_NOT_ALLOWED, objectId);
            if (actor.level() < range.minimum()) return reject(Reason.LEVEL_TOO_LOW, objectId);
            if (actor.level() > range.maximum()) return reject(Reason.LEVEL_TOO_HIGH, objectId);
        }

        Item replacedWeapon = null;
        if (requested.facts() instanceof WeaponFacts weapon) {
            if (desired) {
                if (weapon.twoHanded()) {
                    Item shield = firstArmor(current, ArmorSlot.SHIELD);
                    if (shield != null) return reject(Reason.TWO_HANDED_SHIELD_CONFLICT, shield.objectId());
                }
                for (Item item : current.items()) {
                    if (item.equipped() && item.facts() instanceof WeaponFacts) {
                        Rejection replacementGate = changingItemGate(item, false, policy);
                        if (replacementGate != null) return replacementGate;
                        replacedWeapon = item;
                    }
                }
            }
        } else {
            Rejection armorGate = armorTransitionGate(current, requested,
                    ((ArmorFacts) requested.facts()).slot(), desired);
            if (armorGate != null) return armorGate;
        }

        var candidate = new ArrayList<Item>(current.items().size());
        var changes = new ArrayList<Change>(2);
        for (Item item : current.items()) {
            boolean next = item.objectId() == objectId ? desired
                    : replacedWeapon != null && item.objectId() == replacedWeapon.objectId() ? false : item.equipped();
            candidate.add(next == item.equipped() ? item : item.withEquipped(next));
            if (next != item.equipped()) changes.add(new Change(item.objectId(), next));
        }
        return new Transition(new Loadout(candidate), changes);
    }

    private static Rejection changingItemGate(Item item, boolean desired, Policy policy) {
        if (item.curse() == CurseState.UNKNOWN || item.seal() == SealState.UNKNOWN) {
            return reject(Reason.UNQUALIFIED_ITEM, item.objectId());
        }
        if (item.seal() == SealState.SEALED && policy.sealedItems() == SealedItemPolicy.REJECT_MUTATION) {
            return reject(Reason.SEALED, item.objectId());
        }
        return !desired && item.curse() == CurseState.CURSED ? reject(Reason.CURSED, item.objectId()) : null;
    }

    private static Rejection armorTransitionGate(Loadout current, Item requested, ArmorSlot slot, boolean desired) {
        if (slot == ArmorSlot.SHIRT) {
            Item covering = firstArmor(current, ArmorSlot.BODY, ArmorSlot.CLOAK);
            if (covering != null) return reject(Reason.LAYER_DEPENDENCY, covering.objectId());
        } else if (slot == ArmorSlot.BODY) {
            Item covering = firstArmor(current, ArmorSlot.CLOAK);
            if (covering != null) return reject(Reason.LAYER_DEPENDENCY, covering.objectId());
        }
        if (!desired) return null;
        if (slot == ArmorSlot.SHIELD || slot == ArmorSlot.GUARDER) {
            Item defensive = firstArmor(current, ArmorSlot.SHIELD, ArmorSlot.GUARDER);
            if (defensive != null) return reject(Reason.SHIELD_GUARDER_CONFLICT, defensive.objectId());
            if (slot == ArmorSlot.SHIELD) {
                for (Item item : current.items()) {
                    if (item.equipped() && item.facts() instanceof WeaponFacts weapon && weapon.twoHanded()) {
                        return reject(Reason.TWO_HANDED_SHIELD_CONFLICT, item.objectId());
                    }
                }
            }
            return null;
        }
        if (ring(slot)) return countArmor(current, ArmorSlot.RING, ArmorSlot.RING_2) >= 2
                ? reject(Reason.RING_LIMIT, requested.objectId()) : null;
        if (aid(slot)) {
            for (Item item : current.items()) {
                if (item.equipped() && item.facts() instanceof ArmorFacts armor && aid(armor.slot())
                        && item.catalogId() == requested.catalogId()) {
                    return reject(Reason.DUPLICATE_AID_CATALOG, item.objectId());
                }
            }
            return countArmor(current, ArmorSlot.AID_LEFT, ArmorSlot.AID_RIGHT) >= 2
                    ? reject(Reason.AID_LIMIT, requested.objectId()) : null;
        }
        if (slot == ArmorSlot.TALISMAN_5) return countArmor(current, slot) >= 4
                ? reject(Reason.TALISMAN_LIMIT, requested.objectId()) : null;
        Item occupying = firstArmor(current, slot);
        return occupying == null ? null : reject(Reason.SLOT_OCCUPIED, occupying.objectId());
    }

    /**
     * Refuse impossible/unqualified weapon/armor topology, never silently repair
     * source equipment. This is not validation of non-equipment activation states
     * or the complete character ability snapshot.
     */
    private static Rejection validateTopology(Loadout current) {
        Item weapon = null;
        var counts = new EnumMap<ArmorSlot, Integer>(ArmorSlot.class);
        Set<Long> aidCatalogs = new HashSet<>();
        int rings = 0, aids = 0, defenses = 0;
        for (Item item : current.items()) {
            if (!item.equipped()) continue;
            if (item.facts() == UnknownFacts.INSTANCE) return reject(Reason.UNQUALIFIED_EQUIPPED_ITEM, item.objectId());
            if (item.facts() instanceof WeaponFacts) {
                if (weapon != null) return reject(Reason.INVALID_LOADOUT, item.objectId());
                weapon = item;
            } else if (item.facts() instanceof ArmorFacts armor) {
                ArmorSlot slot = armor.slot();
                int count = counts.merge(slot, 1, Integer::sum);
                if (ring(slot)) {
                    if (++rings > 2) return reject(Reason.INVALID_LOADOUT, item.objectId());
                } else if (aid(slot)) {
                    if (++aids > 2 || !aidCatalogs.add(item.catalogId())) return reject(Reason.INVALID_LOADOUT, item.objectId());
                } else if (slot == ArmorSlot.TALISMAN_5) {
                    if (count > 4) return reject(Reason.INVALID_LOADOUT, item.objectId());
                } else if (count > 1) return reject(Reason.INVALID_LOADOUT, item.objectId());
                if ((slot == ArmorSlot.SHIELD || slot == ArmorSlot.GUARDER) && ++defenses > 1) {
                    return reject(Reason.INVALID_LOADOUT, item.objectId());
                }
            }
        }
        if (weapon != null && ((WeaponFacts) weapon.facts()).twoHanded()
                && counts.containsKey(ArmorSlot.SHIELD)) return reject(Reason.INVALID_LOADOUT, weapon.objectId());
        return null;
    }

    private static boolean ring(ArmorSlot slot) { return slot == ArmorSlot.RING || slot == ArmorSlot.RING_2; }
    private static boolean aid(ArmorSlot slot) { return slot == ArmorSlot.AID_LEFT || slot == ArmorSlot.AID_RIGHT; }
    private static Item firstArmor(Loadout loadout, ArmorSlot... slots) {
        for (Item item : loadout.items()) {
            if (!item.equipped() || !(item.facts() instanceof ArmorFacts armor)) continue;
            for (ArmorSlot slot : slots) if (armor.slot() == slot) return item;
        }
        return null;
    }
    private static int countArmor(Loadout loadout, ArmorSlot... slots) {
        int count = 0;
        for (Item item : loadout.items()) {
            if (!item.equipped() || !(item.facts() instanceof ArmorFacts armor)) continue;
            for (ArmorSlot slot : slots) if (armor.slot() == slot) { count++; break; }
        }
        return count;
    }
    private static void validatePermission(int classMask, LevelRange range) {
        if (classMask < 0 || classMask > 0x7f) throw new IllegalArgumentException("invalid equipment class mask");
        Objects.requireNonNull(range, "qualified level range");
    }
    private static Rejection reject(Reason reason, long objectId) { return new Rejection(reason, objectId); }
}
