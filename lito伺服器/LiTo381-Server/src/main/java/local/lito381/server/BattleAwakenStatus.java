package local.lito381.server;

import java.util.concurrent.TimeUnit;

/** Server-authoritative indefinite Dragon Knight awakening lifecycle. */
final class BattleAwakenStatus {
    record Snapshot(int skillId, int armorClassModifier, int maximumHpBonus,
                    int dragonGfx, int mpDrainAmount, int mpDrainIntervalSeconds,
                    int magicResistanceModifier, int allElementalResistance,
                    int allAbilityScoreBonus,
                    long nextMpDrainAtNanos) {
        Snapshot {
            if (skillId == 0) {
                if (armorClassModifier != 0 || maximumHpBonus != 0 || dragonGfx != 0
                        || mpDrainAmount != 0 || mpDrainIntervalSeconds != 0
                        || magicResistanceModifier != 0
                        || allElementalResistance != 0
                        || allAbilityScoreBonus != 0
                        || nextMpDrainAtNanos != 0) {
                    throw new IllegalArgumentException("invalid empty awakening snapshot");
                }
            } else if (skillId <= 0 || maximumHpBonus < 0 || dragonGfx <= 0
                    || mpDrainAmount <= 0 || mpDrainIntervalSeconds <= 0
                    || magicResistanceModifier < -128
                    || magicResistanceModifier > 127
                    || allElementalResistance < -128
                    || allElementalResistance > 127
                    || allAbilityScoreBonus < 0
                    || nextMpDrainAtNanos <= 0) {
                throw new IllegalArgumentException("invalid active awakening snapshot");
            }
        }

        static Snapshot none() {
            return new Snapshot(0, 0, 0, 0, 0, 0, 0, 0, 0, 0);
        }

        boolean active() {
            return skillId != 0;
        }
    }

    record Toggle(boolean activated, boolean deactivated, Snapshot snapshot) {
        Toggle {
            if (activated == deactivated || snapshot == null
                    || (activated && !snapshot.active())
                    || (deactivated && snapshot.active())) {
                throw new IllegalArgumentException("invalid awakening toggle");
            }
        }
    }

    private int skillId;
    private int armorClassModifier;
    private int maximumHpBonus;
    private int dragonGfx;
    private int mpDrainAmount;
    private int mpDrainIntervalSeconds;
    private int magicResistanceModifier;
    private int allElementalResistance;
    private int allAbilityScoreBonus;
    private long nextMpDrainAtNanos;

    synchronized Toggle toggle(long nowNanos, int requestedSkillId,
                               int requestedArmorClassModifier,
                               int requestedMaximumHpBonus, int requestedDragonGfx,
                               int requestedMpDrainAmount,
                               int requestedMpDrainIntervalSeconds,
                               int requestedMagicResistanceModifier,
                               int requestedAllElementalResistance,
                               int requestedAllAbilityScoreBonus) {
        if (nowNanos < 0 || requestedSkillId <= 0 || requestedMaximumHpBonus < 0
                || requestedDragonGfx <= 0 || requestedMpDrainAmount <= 0
                || requestedMpDrainIntervalSeconds <= 0
                || requestedMagicResistanceModifier < -128
                || requestedMagicResistanceModifier > 127
                || requestedAllElementalResistance < -128
                || requestedAllElementalResistance > 127
                || requestedAllAbilityScoreBonus < 0) {
            throw new IllegalArgumentException("invalid awakening activation");
        }
        if (skillId == requestedSkillId) {
            clearFields();
            return new Toggle(false, true, Snapshot.none());
        }
        if (skillId != 0) {
            throw new IllegalStateException("another awakening is already active");
        }
        skillId = requestedSkillId;
        armorClassModifier = requestedArmorClassModifier;
        maximumHpBonus = requestedMaximumHpBonus;
        dragonGfx = requestedDragonGfx;
        mpDrainAmount = requestedMpDrainAmount;
        mpDrainIntervalSeconds = requestedMpDrainIntervalSeconds;
        magicResistanceModifier = requestedMagicResistanceModifier;
        allElementalResistance = requestedAllElementalResistance;
        allAbilityScoreBonus = requestedAllAbilityScoreBonus;
        nextMpDrainAtNanos = Math.addExact(nowNanos,
                TimeUnit.SECONDS.toNanos(requestedMpDrainIntervalSeconds));
        return new Toggle(true, false, snapshot());
    }

    synchronized Snapshot snapshot() {
        if (skillId == 0) return Snapshot.none();
        return new Snapshot(skillId, armorClassModifier, maximumHpBonus,
                dragonGfx, mpDrainAmount, mpDrainIntervalSeconds,
                magicResistanceModifier, allElementalResistance,
                allAbilityScoreBonus,
                nextMpDrainAtNanos);
    }

    synchronized int pollMpDrainTicks(long nowNanos) {
        if (nowNanos < 0) throw new IllegalArgumentException("invalid awakening clock");
        if (skillId == 0 || nowNanos < nextMpDrainAtNanos) return 0;
        long intervalNanos = TimeUnit.SECONDS.toNanos(mpDrainIntervalSeconds);
        long due = ((nowNanos - nextMpDrainAtNanos) / intervalNanos) + 1L;
        int ticks = (int) Math.min(Integer.MAX_VALUE, due);
        nextMpDrainAtNanos = Math.addExact(nextMpDrainAtNanos,
                Math.multiplyExact((long) ticks, intervalNanos));
        return ticks;
    }

    synchronized Snapshot clear() {
        Snapshot previous = snapshot();
        clearFields();
        return previous;
    }

    private void clearFields() {
        skillId = 0;
        armorClassModifier = 0;
        maximumHpBonus = 0;
        dragonGfx = 0;
        mpDrainAmount = 0;
        mpDrainIntervalSeconds = 0;
        magicResistanceModifier = 0;
        allElementalResistance = 0;
        allAbilityScoreBonus = 0;
        nextMpDrainAtNanos = 0;
    }
}
