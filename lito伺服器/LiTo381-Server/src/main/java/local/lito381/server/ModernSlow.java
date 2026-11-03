package local.lito381.server;

import java.util.Objects;

/** Selected skill29 source arithmetic, not a learned grant or permission to cast.
 * Material/target admission and atomic speed/MP/item commit are separate gates.
 * In particular, the saved row's attr=8 is NOT a range of eight cells. */
final class ModernSlow {
    static final String RULESET = "MODERN_SLOW_29_V1";
    static final int SKILL_ID = 29, ACTION = 19, EFFECT = 752;
    static final int BASE_MP = 20, REUSE_MILLIS = 500, DURATION_SECONDS = 64;
    static final int SOURCE_RANGE = -1, CLIENT_CLICK_RANGE = 10;
    static final int HAPPEN_MESSAGE = 277, STOP_MESSAGE = 0, FAILURE_MESSAGE = 280;
    static final long MATERIAL_ID = 40318;
    static final int MATERIAL_COUNT = 1, BOSS_DIVISOR = 5;

    record Profile(TournamentPresence.CharacterKey key, long equipmentRevision, int mpCost) {
        Profile {
            Objects.requireNonNull(key);
            if (equipmentRevision < 0 || mpCost < 15 || mpCost > BASE_MP) throw invalid();
        }
        @Override public String toString() { return "ModernSlowProfile[redacted]"; }
    }

    static Profile qualify(ModernPhysicalCombat.Fighter fighter) {
        Objects.requireNonNull(fighter);
        var profile = fighter.profile();
        var magic = fighter.magic();
        if (magic == null || !magic.supported() || magic.profile() != profile) throw invalid();
        long original = ModernMagicCombat.originalMpReduction(
                profile.source().character().characterClass(),
                profile.source().persisted().originalIntelligence());
        // Skill29 is circle four: exactly three current-INT thresholds.
        // Haste/Greater Haste helmets do not halve this skill's MP or material.
        return new Profile(fighter.key(), profile.equipmentRevision(),
                effectiveMpCost(profile.effective().intelligence(), original));
    }

    static int effectiveMpCost(long intelligence, long originalReduction) {
        if (intelligence < 1 || originalReduction < 0 || originalReduction > 2) throw invalid();
        int result = BASE_MP;
        for (int threshold = 12; threshold <= 14; threshold++) if (intelligence > threshold) result--;
        return Math.max(0, Math.max(1, result) - Math.toIntExact(originalReduction));
    }

    /** Retains the selected binary64 operation order, including its boundary
     * rounding, then floors/caps and applies the boss divisor. Source abilities
     * and levels are i64; none is narrowed to native byte/short/int first.
     * INT/MR are nonnegative; MagicHit is a signed sum, not a probability.
     * Every i64 input remains finite in these binary64 operations.
     * The caller must qualify MagicHit, MR, boss identity and all immunity gates. */
    static int probability(long casterLevel, long targetLevel, long intelligence,
            long magicHit, long targetMr, boolean boss) {
        if (casterLevel < 1 || targetLevel < 1 || intelligence < 0 || targetMr < 0)
            throw invalid();
        double result = casterLevel > targetLevel ? 55 : casterLevel == targetLevel ? 45 : 35;
        result += intelligence * 0.30d;
        result += magicHit;
        result -= targetMr * 0.05d;
        result = Math.max(1.0d, Math.min(100.0d, result));
        int capped = Math.max(1, Math.min(90, (int) result));
        return boss ? Math.max(1, capped / BOSS_DIVISOR) : capped;
    }

    /** One qualified equipment gate only, NOT complete Slow target admission.
     * Missing template flags must never become a non-immune target. The engine
     * still has to check zone, evasion, counter-magic, brave=5 and active effects. */
    static boolean equipmentImmune(ModernPhysicalCombat.Fighter target) {
        Objects.requireNonNull(target);
        if(target.magic()==null||!target.magic().supported()
                ||target.magic().profile()!=target.profile())throw invalid();
        return target.hasteItemCount().orElseThrow(ModernSlow::invalid)>0;
    }

    /** This only implements the selected ranged=-1 screen gate; the modern
     * target adapter must additionally enforce LOS, identity, force and life.
     * CLIENT_CLICK_RANGE belongs to native producer UX, not this server gate. */
    static boolean inSourceRange(ModernWorldMap.Cell from, ModernWorldMap.Cell to) {
        return ModernHasteCast.inScreen(Objects.requireNonNull(from), Objects.requireNonNull(to));
    }

    private static IllegalArgumentException invalid() { return new IllegalArgumentException("invalid qualified modern slow"); }
    private ModernSlow() { }
}
