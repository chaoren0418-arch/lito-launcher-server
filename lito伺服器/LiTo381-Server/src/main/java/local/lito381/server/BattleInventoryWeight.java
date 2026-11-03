package local.lito381.server;

/** Exact same-family 3.81 inventory-weight and natural-regeneration gates. */
final class BattleInventoryWeight {
    static final int NORMAL_REGEN_LIMIT = 120;
    static final int BUFFED_REGEN_LIMIT = 197;

    record Profile(
            int rawWeight, double serverWeightRate,
            int type, int strength, int constitution,
            int originalStrength, int originalConstitution,
            int equipmentWeightReductionPercent,
            int weightLimitRatePercent, boolean decreaseWeight) {
        Profile {
            if (rawWeight < 0 || !Double.isFinite(serverWeightRate)
                    || serverWeightRate <= 0.0 || type < 0 || type > 6
                    || strength < 0 || constitution < 0
                    || originalStrength < 0 || originalConstitution < 0
                    || weightLimitRatePercent <= -100) {
                throw new IllegalArgumentException("invalid native inventory-weight profile");
            }
        }
    }

    private BattleInventoryWeight() {}

    /** Mirrors L1ItemInstance.getWeight(), including its one-unit floor. */
    static int itemWeight(long count, int unitWeight) {
        if (count <= 0 || unitWeight < 0) return Integer.MAX_VALUE;
        if (unitWeight == 0) return 0;
        try {
            long value = Math.max(Math.multiplyExact(count, (long) unitWeight) / 1_000L, 1L);
            return value > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) value;
        } catch (ArithmeticException overflow) {
            return Integer.MAX_VALUE;
        }
    }

    /** Mirrors L1Inventory's saturating aggregate. */
    static int addWeight(int left, int right) {
        if (left < 0 || right < 0 || left > Integer.MAX_VALUE - right) {
            return Integer.MAX_VALUE;
        }
        return left + right;
    }

    static double maxWeight(Profile profile) {
        if (profile == null) throw new IllegalArgumentException("native weight profile is required");
        double weightUp = 1.0 + profile.weightLimitRatePercent() / 100.0;
        double result = 150.0
                * Math.floor(0.6 * profile.strength() + 0.4 * profile.constitution() + 1.0)
                * weightUp;
        double originalReduction = 0.04 * (
                originalStrengthReduction(profile.type(), profile.originalStrength())
                        + originalConstitutionReduction(
                                profile.type(), profile.originalConstitution()));
        result *= 1.0 + profile.equipmentWeightReductionPercent() / 100.0
                + originalReduction;
        if (profile.decreaseWeight()) result += 180.0;
        return result * profile.serverWeightRate();
    }

    /** Mirrors L1PcInventory.calcWeight240(). */
    static int weight240(Profile profile) {
        return weight240(profile, profile.rawWeight());
    }

    private static int weight240(Profile profile, int rawWeight) {
        if (profile == null || rawWeight < 0) {
            throw new IllegalArgumentException("native weight profile/raw weight is required");
        }
        double maximum = maxWeight(profile);
        if (maximum <= 0.0) return 0;
        if (rawWeight > maximum) return 240;
        return (int) Math.round(rawWeight * 240.0 / maximum);
    }

    /**
     * Mirrors L1PcInventory.checkAddItem's pre-mutation weight projection:
     * currentWeight + itemUnitWeight * count / 1000 + 1.  Overflow is
     * deliberately fail-closed instead of retaining the source's signed-long
     * wraparound weakness.
     */
    static int projectedAddRawWeight(int currentRawWeight, long count, int unitWeight) {
        if (currentRawWeight < 0 || count <= 0 || unitWeight < 0) {
            return Integer.MAX_VALUE;
        }
        try {
            long itemWeight = Math.addExact(
                    Math.multiplyExact(count, (long) unitWeight) / 1_000L, 1L);
            long projected = Math.addExact((long) currentRawWeight, itemWeight);
            return projected > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) projected;
        } catch (ArithmeticException overflow) {
            return Integer.MAX_VALUE;
        }
    }

    /** Native admission rejects equality as well as values above stage 240. */
    static boolean permitsAdd(Profile currentProfile, long count, int unitWeight) {
        if (currentProfile == null) return false;
        int projected = projectedAddRawWeight(
                currentProfile.rawWeight(), count, unitWeight);
        return weight240(currentProfile, projected) < 240;
    }

    /**
     * Mirrors L1Shop's distinct whole-order check.  Shop orders compare the
     * current aggregate and unrounded template weight directly with maxWeight;
     * they do not call checkAddItem and therefore have neither its +1 nor its
     * stage-240 rounding boundary.
     */
    static boolean permitsShopOrder(Profile currentProfile, long orderUnitWeight) {
        if (currentProfile == null || orderUnitWeight < 0) return false;
        double maximumUnits = maxWeight(currentProfile) * 1_000.0;
        if (!Double.isFinite(maximumUnits) || maximumUnits < 0.0) return false;
        try {
            long currentUnits = Math.multiplyExact(
                    (long) currentProfile.rawWeight(), 1_000L);
            long projectedUnits = Math.addExact(currentUnits, orderUnitWeight);
            return projectedUnits <= maximumUnits;
        } catch (ArithmeticException overflow) {
            return false;
        }
    }

    static boolean permitsNaturalRegen(int weight240, boolean weightBypassActive) {
        if (weight240 < 0 || weight240 > 240) {
            throw new IllegalArgumentException("native weight stage is outside 0..240");
        }
        if (weight240 < NORMAL_REGEN_LIMIT) return true;
        return weightBypassActive && weight240 < BUFFED_REGEN_LIMIT;
    }

    static boolean permitsNaturalHpRegen(int weight240, boolean weightBypassActive) {
        return permitsNaturalRegen(weight240, weightBypassActive);
    }

    static int originalStrengthReduction(int type, int originalStrength) {
        return switch (type) {
            case 0 -> originalStrength <= 13 ? 0
                    : originalStrength <= 16 ? 1 : originalStrength <= 19 ? 2 : 3;
            case 1 -> 0;
            case 2 -> originalStrength <= 15 ? 0 : 2;
            case 3 -> originalStrength <= 9 ? 0 : 1;
            case 4 -> originalStrength <= 12 ? 0 : originalStrength <= 15 ? 2 : 3;
            case 5 -> originalStrength <= 15 ? 0 : 1;
            case 6 -> originalStrength <= 17 ? 0 : 1;
            default -> 0;
        };
    }

    static int originalConstitutionReduction(int type, int originalConstitution) {
        return switch (type) {
            case 0 -> originalConstitution <= 10 ? 0 : 1;
            case 1 -> originalConstitution <= 14 ? 0 : 1;
            case 2 -> originalConstitution <= 14 ? 0 : 2;
            case 3 -> originalConstitution <= 12 ? 0
                    : originalConstitution <= 14 ? 1 : 2;
            case 4 -> originalConstitution <= 8 ? 0 : 1;
            case 5 -> 0;
            case 6 -> originalConstitution <= 16 ? 0
                    : originalConstitution == 17 ? 1 : 2;
            default -> 0;
        };
    }
}
