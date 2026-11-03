package local.lito381.server;

/** Exact one-second 3.81 player MPR counter and qualified base amount. */
final class BattleNaturalMpRegen {
    static final int MEDITATION_BONUS = 5;
    static final int CONCENTRATION_BONUS = 2;
    static final int STATE_ATTACK = 1;
    static final int STATE_MOVE = 2;
    static final int STATE_NONE = 4;
    static final int WIZARD_THRESHOLD = 40;
    static final int OTHER_THRESHOLD = 64;
    private static final long SECOND_NANOS = 1_000_000_000L;

    record Profile(int type, int wisdom, int originalWisdom, int equipmentMpRegen) {
        Profile {
            if (type < 0 || type > 6 || wisdom < 0 || originalWisdom < 0) {
                throw new IllegalArgumentException("invalid native MPR profile");
            }
        }
    }

    private int counter;
    private int state = STATE_NONE;
    private long nextSecondNanos = Long.MIN_VALUE;

    void reset(long nowNanos) {
        if (nowNanos < 0) throw new IllegalArgumentException("negative native MPR clock");
        counter = 0;
        state = STATE_NONE;
        nextSecondNanos = Math.addExact(nowNanos, SECOND_NANOS);
    }

    void noteMove() {
        state = STATE_MOVE;
    }

    void noteAttack() {
        state = STATE_ATTACK;
    }

    /**
     * Returns how many MPR events became due. Seconds for which the source
     * executor's outer check is inactive advance time but do not alter its
     * counter or last activity state.
     */
    int poll(long nowNanos, boolean active, int type) {
        if (nowNanos < 0) throw new IllegalArgumentException("negative native MPR clock");
        if (nextSecondNanos == Long.MIN_VALUE) {
            reset(nowNanos);
            return 0;
        }
        if (nowNanos < nextSecondNanos) return 0;

        long elapsed = ((nowNanos - nextSecondNanos) / SECOND_NANOS) + 1L;
        nextSecondNanos = Math.addExact(nextSecondNanos,
                Math.multiplyExact(elapsed, SECOND_NANOS));
        if (!active) return 0;

        int due = 0;
        int threshold = threshold(type);
        for (long second = 0; second < elapsed; second++) {
            counter += state;
            state = STATE_NONE;
            if (counter >= threshold) {
                counter = 0;
                due++;
            }
        }
        return due;
    }

    static int threshold(int type) {
        if (type < 0 || type > 6) {
            throw new IllegalArgumentException("unsupported native character type");
        }
        return type == 3 ? WIZARD_THRESHOLD : OTHER_THRESHOLD;
    }

    static int wisdomBase(int wisdom) {
        if (wisdom < 0) throw new IllegalArgumentException("negative native wisdom");
        if (wisdom <= 14) return 1;
        if (wisdom <= 16) return 2;
        return 3;
    }

    static int originalMpRegen(int type, int originalWisdom) {
        int wisdom = originalWisdom;
        return switch (type) {
            case 0 -> wisdom == 13 || wisdom == 14 ? 1 : wisdom >= 15 ? 2 : 0;
            case 1 -> wisdom == 11 || wisdom == 12 ? 1 : wisdom == 13 ? 2 : 0;
            case 2 -> wisdom >= 15 && wisdom <= 17 ? 1 : wisdom == 18 ? 2 : 0;
            case 3 -> wisdom == 14 || wisdom == 15 ? 1
                    : wisdom == 16 || wisdom == 17 ? 2 : wisdom == 18 ? 3 : 0;
            case 4 -> wisdom >= 13 ? 1 : 0;
            case 5 -> wisdom == 15 || wisdom == 16 ? 1 : wisdom >= 17 ? 2 : 0;
            case 6 -> wisdom >= 14 && wisdom <= 16 ? 1 : wisdom >= 17 ? 2 : 0;
            default -> 0;
        };
    }

    static int amount(Profile profile) {
        return amount(profile, false);
    }

    static int amount(Profile profile, boolean bluePotionActive) {
        return amount(profile, bluePotionActive, false, false);
    }

    static int amount(Profile profile, boolean bluePotionActive,
                      boolean meditationActive, boolean concentrationActive) {
        return amount(profile, bluePotionActive, meditationActive,
                concentrationActive, 0);
    }

    static int amount(Profile profile, boolean bluePotionActive,
                      boolean meditationActive, boolean concentrationActive,
                      int cookingMpRegen) {
        if (profile == null) throw new IllegalArgumentException("native MPR profile required");
        if (cookingMpRegen < 0) throw new IllegalArgumentException("negative cooking MPR");
        return wisdomBase(profile.wisdom())
                + originalMpRegen(profile.type(), profile.originalWisdom())
                + profile.equipmentMpRegen()
                + (bluePotionActive ? bluePotionBonus(profile.wisdom()) : 0)
                + (meditationActive ? MEDITATION_BONUS : 0)
                + (concentrationActive ? CONCENTRATION_BONUS : 0)
                + cookingMpRegen;
    }

    static int bluePotionBonus(int wisdom) {
        if (wisdom < 0) throw new IllegalArgumentException("negative native wisdom");
        return wisdom < 11 ? 1 : wisdom - 10;
    }
}
