package local.lito381.server;

import java.util.List;

/** Exact selector-only target recovery and percentile gate for native skill188. */
final class BattleResistFear {
    record Candidate(long objectId, int x, int y) {
        Candidate {
            if (objectId <= 0) {
                throw new IllegalArgumentException("Resist Fear target object is required");
            }
        }
    }

    private BattleResistFear() {}

    static long resolveTarget(
            long lastTargetObjectId, int sourceX, int sourceY,
            int maximumRange, List<Candidate> eligibleCandidates) {
        if (maximumRange <= 0 || eligibleCandidates == null) {
            throw new IllegalArgumentException("invalid Resist Fear target inputs");
        }
        List<Candidate> inRange = eligibleCandidates.stream()
                .filter(candidate -> chebyshev(sourceX, sourceY,
                        candidate.x(), candidate.y()) <= maximumRange)
                .toList();
        if (lastTargetObjectId > 0) {
            Candidate selected = inRange.stream()
                    .filter(candidate -> candidate.objectId() == lastTargetObjectId)
                    .findFirst().orElse(null);
            if (selected != null) return selected.objectId();
        }
        return inRange.size() == 1 ? inRange.get(0).objectId() : 0;
    }

    static boolean succeeds(int probability, int oneBasedD100) {
        if (probability < 0 || probability > 100
                || oneBasedD100 < 1 || oneBasedD100 > 100) {
            throw new IllegalArgumentException("invalid Resist Fear percentile roll");
        }
        return probability > 0 && oneBasedD100 <= probability;
    }

    private static int chebyshev(int sourceX, int sourceY, int targetX, int targetY) {
        return Math.max(Math.abs(sourceX - targetX), Math.abs(sourceY - targetY));
    }
}
