package local.lito381.server;

import java.util.List;

/** Exact 56-slot reward table retained by {@code Roy_Pouch}. */
final class RoiPouchCatalog {
    static final int ROLL_BOUND = 56;
    static final int ELVEN_SILVER_ARMOR_MISSING_TEMPLATE = 26000;
    static final int BLESSED_OGRE_BELT_MISSING_TEMPLATE = 120317;

    record Outcome(int minimumRoll, int maximumRoll, long itemId, int quantity,
                   boolean selectedTemplatePresent) {
        Outcome {
            if (minimumRoll < 0 || maximumRoll < minimumRoll
                    || maximumRoll >= ROLL_BOUND || itemId <= 0 || quantity <= 0) {
                throw new IllegalArgumentException("invalid Roi pouch outcome");
            }
        }

        boolean contains(int roll) {
            return roll >= minimumRoll && roll <= maximumRoll;
        }

        int slots() {
            return maximumRoll - minimumRoll + 1;
        }
    }

    private static final List<Outcome> OUTCOMES = List.of(
            new Outcome(0, 9, InventoryCatalog.ADENA, 1_000, true),
            new Outcome(10, 19, 40016, 1, true),
            new Outcome(20, 29, InventoryCatalog.BLUE_POTION, 1, true),
            new Outcome(30, 39, 40089, 1, true),
            new Outcome(40, 47, InventoryCatalog.POLYMORPH_SCROLL, 1, true),
            // Both ids are hard-coded by the executor but absent from every
            // selected armor/etcitem/weapon template table. ItemTable returns
            // null after the pouch has already been consumed.
            new Outcome(48, 48, ELVEN_SILVER_ARMOR_MISSING_TEMPLATE, 1, false),
            new Outcome(49, 49, BLESSED_OGRE_BELT_MISSING_TEMPLATE, 1, false),
            new Outcome(50, 55, 40074, 1, true));

    private RoiPouchCatalog() {}

    static List<Outcome> all() {
        return OUTCOMES;
    }

    static Outcome outcome(int zeroBasedRoll) {
        if (zeroBasedRoll < 0 || zeroBasedRoll >= ROLL_BOUND) {
            throw new IllegalArgumentException("Roi pouch roll must be 0..55");
        }
        return OUTCOMES.stream().filter(candidate -> candidate.contains(zeroBasedRoll))
                .findFirst().orElseThrow(() ->
                        new IllegalStateException("Roi pouch table has a gap"));
    }

    static int coveredSlots() {
        return OUTCOMES.stream().mapToInt(Outcome::slots).sum();
    }
}
