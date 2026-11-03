package local.lito381.server;

/** Retained Water Dragon instance-only recovery afflictions (skills 4011..4013). */
final class BattleWaterDragonAffliction {
    static final int DURATION_SECONDS = 12;
    static final int TIMER_INTERVAL_MILLIS = 5_000;

    enum Kind {
        POTION_EROSION(4011, 49011, 7781),
        DIRTY_CURRENT(4012, 49012, 7782),
        HEALING_EROSION(4013, 49013, 7780);

        private final int skillId;
        private final int aliasSkillId;
        private final int effectGfx;

        Kind(int skillId, int aliasSkillId, int effectGfx) {
            this.skillId = skillId;
            this.aliasSkillId = aliasSkillId;
            this.effectGfx = effectGfx;
        }

        int skillId() {
            return skillId;
        }

        int aliasSkillId() {
            return aliasSkillId;
        }

        int effectGfx() {
            return effectGfx;
        }
    }

    private BattleWaterDragonAffliction() {}

    /** Retained AD80_2_Timer uses strict bounds, not inclusive bounds. */
    static boolean insideTimerArea(int x, int y) {
        return x > 32942 && x < 32980 && y > 32810 && y < 32871;
    }

    /** Retained nextInt(3) selector order. */
    static Kind timerKind(int zeroBasedRoll) {
        return switch (zeroBasedRoll) {
            case 0 -> Kind.POTION_EROSION;
            case 1 -> Kind.DIRTY_CURRENT;
            case 2 -> Kind.HEALING_EROSION;
            default -> throw new IllegalArgumentException(
                    "Water Dragon timer roll must be 0..2");
        };
    }
}
