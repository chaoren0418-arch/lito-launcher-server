package local.lito381.server;

import java.util.List;

/** Exact retained skill-112 targeting, probability and melee damage rules. */
final class BattleArmorBreak {
    static final int DAMAGE_PERCENT = 158;
    static final int EXCLUDED_WEAPON_TYPE_BOW = 20;
    static final int EXCLUDED_WEAPON_TYPE_GAUNTLET = 62;

    record Candidate(long objectId, int x, int y) {
        Candidate {
            if (objectId <= 0) {
                throw new IllegalArgumentException("Armor Break target object is required");
            }
        }
    }

    private BattleArmorBreak() {}

    /**
     * Compensates for LiTo.bin's selector-only skill-112 request. The exact
     * last explicitly selected physical target wins while it is still in
     * range; otherwise a target is accepted only when the adjacent eligible
     * set is unambiguous.
     */
    static long resolveTarget(long lastTargetObjectId, int sourceX, int sourceY,
                              List<Candidate> eligibleCandidates) {
        if (eligibleCandidates == null) {
            throw new IllegalArgumentException("Armor Break candidates are required");
        }
        List<Candidate> adjacent = eligibleCandidates.stream()
                .filter(candidate -> chebyshev(sourceX, sourceY,
                        candidate.x(), candidate.y()) <= 1)
                .toList();
        if (lastTargetObjectId > 0) {
            Candidate selected = adjacent.stream()
                    .filter(candidate -> candidate.objectId() == lastTargetObjectId)
                    .findFirst().orElse(null);
            if (selected != null) return selected.objectId();
        }
        return adjacent.size() == 1 ? adjacent.get(0).objectId() : 0;
    }

    static int probability(int casterLevel, int targetLevel,
                           int probabilityHigh, int probabilityEqual,
                           int probabilityLow, boolean bossImmune) {
        if (casterLevel <= 0 || targetLevel <= 0
                || probabilityHigh < 0 || probabilityHigh > 100
                || probabilityEqual < 0 || probabilityEqual > 100
                || probabilityLow < 0 || probabilityLow > 100) {
            throw new IllegalArgumentException("invalid Armor Break probability inputs");
        }
        if (bossImmune) return 0;
        int result = casterLevel > targetLevel ? probabilityHigh
                : casterLevel == targetLevel ? probabilityEqual : probabilityLow;
        return Math.max(1, Math.min(100, result));
    }

    static boolean succeeds(int probability, int oneBasedD100) {
        if (probability < 0 || probability > 100
                || oneBasedD100 < 1 || oneBasedD100 > 100) {
            throw new IllegalArgumentException("invalid Armor Break percentile roll");
        }
        return probability > 0 && oneBasedD100 <= probability;
    }

    static boolean supportsWeaponType(int weaponType) {
        weaponType = CombatRules.weaponActionType(weaponType);
        return weaponType != EXCLUDED_WEAPON_TYPE_BOW
                && weaponType != EXCLUDED_WEAPON_TYPE_GAUNTLET;
    }

    /** Retained Java cast semantics: {@code (int) (damage * 1.58D)}. */
    static int damageAfterBreak(int damage, boolean active, int weaponType) {
        if (damage < 0) throw new IllegalArgumentException("negative combat damage");
        if (!active || !supportsWeaponType(weaponType)) return damage;
        return (int) Math.min(Integer.MAX_VALUE, damage * 1.58D);
    }

    private static int chebyshev(int sourceX, int sourceY, int targetX, int targetY) {
        return Math.max(Math.abs(sourceX - targetX), Math.abs(sourceY - targetY));
    }
}
