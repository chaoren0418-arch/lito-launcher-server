package local.lito381.server;

/** Exact deterministic core of the 3.81 {@code getKiringkuDamage} branch. */
final class BattleKiringkuDamage {
    static final int WEAPON_TYPE = 17;
    static final int SPECIAL_INVENTORY_GFX = 3018;
    static final int SPECIAL_EFFECT_GFX = 6983;
    static final int DEFAULT_EFFECT_GFX = 7049;

    private BattleKiringkuDamage() {}

    record Outcome(
            int diceTotal, int intelligenceTerm, double intelligenceCoefficient,
            int scaledDiceDamage, int preResistanceDamage,
            int targetMagicResistance, int originalMagicHit,
            int resistanceFloor, double resistanceCoefficient,
            int damage, int effectGfx) {
        static Outcome none() {
            return new Outcome(0, 0, 0.0D, 0, 0, 0, 0, 0, 0.0D, 0, 0);
        }
    }

    static Outcome roll(
            int dieOne, int dieTwo,
            int effectiveIntelligence, int equipmentSpellPower,
            int enchantLevel, int targetMagicResistance,
            int originalMagicHit, int inventoryGfx) {
        if (dieOne < 1 || dieOne > 5 || dieTwo < 1 || dieTwo > 5
                || effectiveIntelligence < 1 || enchantLevel < 0
                || targetMagicResistance < 0 || originalMagicHit < 0
                || inventoryGfx < 0 || inventoryGfx > 0xFFFF) {
            throw new IllegalArgumentException("invalid Kiringku damage input");
        }

        int diceTotal = Math.addExact(Math.addExact(dieOne, dieTwo), 14);
        int intelligenceTerm = Math.max(
                1, effectiveIntelligence + equipmentSpellPower - 12);
        double intelligenceCoefficient =
                1.0D + intelligenceTerm * 3.0D / 32.0D;
        // The original uses compound int *= double assignment and therefore
        // truncates here before adding weapon enchant.
        int scaledDiceDamage = (int) (diceTotal * intelligenceCoefficient);
        int preResistanceDamage = Math.addExact(scaledDiceDamage, enchantLevel);

        int divisor = targetMagicResistance <= 100 ? 2 : 10;
        int resistanceFloor = (int) Math.floor(
                (targetMagicResistance - originalMagicHit) / (double) divisor);
        double resistanceCoefficient = targetMagicResistance <= 100
                ? 1.0D - 0.01D * resistanceFloor
                : 0.6D - 0.01D * resistanceFloor;
        // L1AttackPc adds getKiringkuDamage * 0.9 to an integral physical
        // subtotal. Truncating this positive component is equivalent to the
        // final Java double-to-int conversion after integral target reductions.
        int damage = (int) (preResistanceDamage * resistanceCoefficient * 0.9D);
        int effectGfx = inventoryGfx == SPECIAL_INVENTORY_GFX
                ? SPECIAL_EFFECT_GFX : DEFAULT_EFFECT_GFX;
        return new Outcome(
                diceTotal, intelligenceTerm, intelligenceCoefficient,
                scaledDiceDamage, preResistanceDamage,
                targetMagicResistance, originalMagicHit,
                resistanceFloor, resistanceCoefficient,
                Math.max(0, damage), effectGfx);
    }

    /** Exact {@code L1ClassFeature.getAttackLevel} family used after weapon damage. */
    static int classAttackLevel(int characterType, int level) {
        if (level < 1) throw new IllegalArgumentException("invalid character level");
        return switch (characterType) {
            case 0 -> level / 10;
            case 1 -> level / 6;
            case 2 -> level / 12;
            case 3 -> level / 20;
            case 4, 6 -> level / 16;
            case 5 -> level / 13;
            default -> throw new IllegalArgumentException("unsupported classic class");
        };
    }
}
