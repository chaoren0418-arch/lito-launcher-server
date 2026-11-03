package local.lito381.server;

import java.util.HashSet;
import java.util.Objects;

/** Skill43's source/equipment-derived cost. Not a learned grant, cast, target
 * authority or speed lease. Ordinary Haste is targeted (PC/NPC, including self),
 * not skill54's self-only operation and not the 300-second potion. */
final class ModernHaste {
    static final String RULESET = "MODERN_HASTE_43_V1";
    static final int SKILL_ID = 43, ACTION = 19, EFFECT = 755;
    static final int BASE_MP = 40, REUSE_MILLIS = 10, DURATION_SECONDS = 1200;

    record Profile(TournamentPresence.CharacterKey key, long equipmentRevision, int mpCost) {
        Profile {
            Objects.requireNonNull(key);
            if (equipmentRevision < 0 || mpCost < 15 || mpCost > BASE_MP) throw invalid();
        }
        @Override public String toString() { return "ModernHasteProfile[redacted]"; }
    }

    static Profile qualify(ModernPhysicalCombat.Fighter fighter) {
        Objects.requireNonNull(fighter);
        var profile = fighter.profile();
        var magic = fighter.magic();
        if (magic == null || !magic.supported() || magic.profile() != profile) throw invalid();
        var equipped = new HashSet<Long>();
        for (var item : profile.equipped()) if (item.equipped()) equipped.add(item.objectId());
        // Resolve live SET flags against this source; the imported inventory's
        // equipped flag can be stale. Wind Helm20023 affects skill54, not43.
        boolean helm = profile.source().inventory().stream().anyMatch(item ->
                equipped.contains(item.objectId())
                && item.template().kind() == ModernCharacterData.ItemKind.ARMOR
                && (item.template().catalogId() == 20008 || item.template().catalogId() == 20013));
        long original = ModernMagicCombat.originalMpReduction(
                profile.source().character().characterClass(),
                profile.source().persisted().originalIntelligence());
        return new Profile(fighter.key(), profile.equipmentRevision(),
                effectiveMpCost(profile.effective().intelligence(), original, helm));
    }

    /** Five circle-6 thresholds, helm half (floor), min-one, original INT.
     * No byte narrowing or native INT ceiling for modern source values. */
    static int effectiveMpCost(long intelligence, long originalReduction, boolean helm) {
        if (intelligence < 1 || originalReduction < 0 || originalReduction > 2) throw invalid();
        int cost = BASE_MP;
        for (int threshold = 12; threshold <= 16; threshold++) if (intelligence > threshold) cost--;
        if (helm) cost >>= 1;
        return Math.max(0, Math.max(1, cost) - Math.toIntExact(originalReduction));
    }

    private static IllegalArgumentException invalid() {
        return new IllegalArgumentException("invalid qualified modern haste");
    }
    private ModernHaste() { }
}
