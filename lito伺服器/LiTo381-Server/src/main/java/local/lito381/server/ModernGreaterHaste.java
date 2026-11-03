package local.lito381.server;

import java.util.HashSet;
import java.util.Objects;

/** Skill54's derived current-loadout facts, NOT a learned grant or a cast
 * operation. Explicit v42 service/transport admission owns the distinct spell
 * receipt/lease; these pure facts grant no player UI permission. No SQL or mutation. */
final class ModernGreaterHaste {
    static final String RULESET = "MODERN_GREATER_HASTE_54_V1";
    static final int SKILL_ID = 54, ACTION = 19, EFFECT = 3104;
    static final int BASE_MP = 60, REUSE_MILLIS = 10, DURATION_SECONDS = 2400;
    static final long WIND_HELM = 20023;

    record Profile(TournamentPresence.CharacterKey key, long equipmentRevision, int mpCost) {
        Profile {
            Objects.requireNonNull(key);
            if (equipmentRevision < 0 || mpCost < 25 || mpCost > BASE_MP) throw invalid();
        }
        @Override public String toString() { return "ModernGreaterHasteProfile[redacted]"; }
    }

    static Profile qualify(ModernPhysicalCombat.Fighter fighter) {
        Objects.requireNonNull(fighter);
        var profile = fighter.profile();
        var magic = fighter.magic();
        if (magic == null || !magic.supported() || magic.profile() != profile) throw invalid();
        // Inventory.equipped is the imported baseline, not live modern SET.
        // Resolve the current, qualified flags against this same source bundle.
        var equipped = new HashSet<Long>();
        for (var item : profile.equipped()) if (item.equipped()) equipped.add(item.objectId());
        boolean windHelm = profile.source().inventory().stream().anyMatch(item ->
                equipped.contains(item.objectId())
                && item.template().kind() == ModernCharacterData.ItemKind.ARMOR
                && item.template().catalogId() == WIND_HELM);
        long originalReduction = ModernMagicCombat.originalMpReduction(
                profile.source().character().characterClass(),
                profile.source().persisted().originalIntelligence());
        return new Profile(fighter.key(), profile.equipmentRevision(),
                effectiveMpCost(profile.effective().intelligence(), originalReduction, windHelm));
    }

    /** Native order: six circle-7 INT thresholds, Wind Helm shift, min-one,
     * then original-class INT reduction. A modern i64 INT is never narrowed
     * or capped to a native byte merely to calculate these comparisons. */
    static int effectiveMpCost(long intelligence, long originalReduction, boolean windHelm) {
        if (intelligence < 1 || originalReduction < 0 || originalReduction > 2) throw invalid();
        int result = BASE_MP;
        for (int threshold = 12; threshold <= 17; threshold++) if (intelligence > threshold) result--;
        if (windHelm) result >>= 1;
        return Math.max(0, Math.max(1, result) - Math.toIntExact(originalReduction));
    }

    private static IllegalArgumentException invalid() {
        return new IllegalArgumentException("invalid qualified modern greater haste");
    }
    private ModernGreaterHaste() { }
}
