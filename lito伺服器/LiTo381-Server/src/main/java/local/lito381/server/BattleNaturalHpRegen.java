package local.lito381.server;

import java.util.concurrent.ThreadLocalRandom;

/** Exact one-second 3.81 player HPR counter used by Life Stream. */
final class BattleNaturalHpRegen {
    static final int STATE_ATTACK = 1;
    static final int STATE_MOVE = 2;
    static final int STATE_NONE = 4;
    static final int LIFE_STREAM_BONUS = 3;
    private static final long SECOND_NANOS = 1_000_000_000L;
    private static final int[] LEVEL_THRESHOLDS = {30, 25, 20, 16, 14, 12, 11, 10, 9, 3, 2};

    record Profile(int level, int type, int constitution, int originalConstitution,
                   int food, int constitutionHprMaximum, int equipmentHpRegen) {
        Profile {
            if (level < 1 || type < 0 || type > 6 || constitution < 0
                    || originalConstitution < 0 || food < 0
                    || constitutionHprMaximum < 1) {
                throw new IllegalArgumentException("invalid native HPR profile");
            }
        }
    }

    private int counter;
    private int state = STATE_NONE;
    private long nextSecondNanos = Long.MIN_VALUE;

    void reset(long nowNanos) {
        if (nowNanos < 0) throw new IllegalArgumentException("negative native HPR clock");
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
     * Returns how many natural-HPR events became due. Inactive seconds advance
     * the global timer but neither increment the counter nor clear the last
     * activity state, matching HprExecutor.check() short-circuiting.
     */
    int poll(long nowNanos, boolean active, int level, int type) {
        if (nowNanos < 0) throw new IllegalArgumentException("negative native HPR clock");
        if (nextSecondNanos == Long.MIN_VALUE) {
            reset(nowNanos);
            return 0;
        }
        if (nowNanos < nextSecondNanos) return 0;

        long elapsed = ((nowNanos - nextSecondNanos) / SECOND_NANOS) + 1L;
        nextSecondNanos = Math.addExact(nextSecondNanos,
                Math.multiplyExact(elapsed, SECOND_NANOS));
        if (!active) return 0;

        int threshold = threshold(level, type);
        int due = 0;
        // The battle clock normally supplies one boundary at a time. Preserve
        // exact state-reset behavior if the scheduler catches up after a stall.
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

    static int threshold(int level, int type) {
        int index = Math.min(10, Math.max(1, level));
        if (level >= 30 && type == 1) index = 11; // knight-only source branch
        return LEVEL_THRESHOLDS[index - 1] << 2;
    }

    static int fallbackConHprMaximum(int constitution) {
        int con = Math.max(1, Math.min(50, constitution));
        if (con <= 15) return 1;
        if (con == 16) return 2;
        if (con == 17) return 3;
        return Math.max(1, (con - 12) / 2);
    }

    static int originalHpRegen(int type, int originalConstitution) {
        int con = originalConstitution;
        return switch (type) {
            case 0 -> con == 13 || con == 14 ? 1
                    : con == 15 || con == 16 ? 2 : con == 17 ? 3 : con == 18 ? 4 : 0;
            case 1 -> con == 16 || con == 17 ? 2 : con == 18 ? 4 : 0;
            case 2 -> con == 14 || con == 15 ? 1 : con == 16 ? 2 : con >= 17 ? 3 : 0;
            case 3 -> con == 17 ? 1 : con == 18 ? 2 : 0;
            case 4 -> con == 11 || con == 12 ? 1 : con >= 13 ? 2 : 0;
            case 5 -> con == 16 || con == 17 ? 1 : con == 18 ? 3 : 0;
            case 6 -> con == 14 || con == 15 ? 1 : con >= 16 ? 2 : 0;
            default -> 0;
        };
    }

    static int roll(Profile profile, boolean inLifeStream) {
        return roll(profile, inLifeStream, 0);
    }

    static int roll(Profile profile, boolean inLifeStream, int cookingHpRegen) {
        return roll(profile, inLifeStream, cookingHpRegen, 0);
    }

    static int roll(Profile profile, boolean inLifeStream, int cookingHpRegen,
                    int skillHpRegen) {
        int base = ThreadLocalRandom.current().nextInt(1,
                profile.constitutionHprMaximum() + 1);
        return amount(profile, inLifeStream, base, cookingHpRegen, skillHpRegen);
    }

    static int amount(Profile profile, boolean inLifeStream, int baseRoll) {
        return amount(profile, inLifeStream, baseRoll, 0);
    }

    static int amount(Profile profile, boolean inLifeStream, int baseRoll,
                      int cookingHpRegen) {
        return amount(profile, inLifeStream, baseRoll, cookingHpRegen, 0);
    }

    static int amount(Profile profile, boolean inLifeStream, int baseRoll,
                      int cookingHpRegen, int skillHpRegen) {
        if (profile == null || baseRoll < 1
                || baseRoll > profile.constitutionHprMaximum() || cookingHpRegen < 0
                || skillHpRegen < 0) {
            throw new IllegalArgumentException("invalid native HPR roll");
        }
        return baseRoll
                + originalHpRegen(profile.type(), profile.originalConstitution())
                + profile.equipmentHpRegen()
                + (inLifeStream ? LIFE_STREAM_BONUS : 0)
                + cookingHpRegen
                + skillHpRegen;
    }
}
