package local.lito381.server;

import java.util.Collection;

/** Server-authoritative target identity gate for native royal skill 113. */
final class BattleTrueTarget {
    enum Kind {
        PLAYER,
        ARENA_BOT,
        COMPANION,
        WORLD_MONSTER,
        GUARDIAN_TOWER,
        QUEST_MONSTER,
        TIME_ATTACK_ACTOR
    }

    record Candidate(long objectId, int x, int y, Kind kind,
                     boolean alive, boolean visible, boolean sameWorld) {
        Candidate {
            if (objectId <= 0 || objectId > 0xFFFF_FFFFL
                    || x < 0 || x > 0xFFFF || y < 0 || y > 0xFFFF
                    || kind == null) {
                throw new IllegalArgumentException("invalid True Target candidate");
            }
        }

        boolean eligible() {
            return alive && visible && sameWorld;
        }
    }

    private BattleTrueTarget() {}

    /**
     * Object ids are a world-table key.  A duplicate is therefore an unsafe
     * alias and fails closed instead of allowing collection order to choose a
     * different actor.
     */
    static Candidate resolve(long requestedObjectId,
                             Collection<Candidate> candidates) {
        if (requestedObjectId <= 0 || requestedObjectId > 0xFFFF_FFFFL
                || candidates == null) return null;
        Candidate resolved = null;
        for (Candidate candidate : candidates) {
            if (candidate == null || candidate.objectId() != requestedObjectId) continue;
            if (resolved != null) return null;
            resolved = candidate;
        }
        return resolved != null && resolved.eligible() ? resolved : null;
    }
}
