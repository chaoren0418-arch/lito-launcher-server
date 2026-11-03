package local.lito381.server;

import java.util.concurrent.TimeUnit;

/** Server-authoritative lease for the mutually-exclusive classic weapon-buff group. */
final class BattleWeaponBuffStatus {
    record Modifier(int skillId, int meleeHit, int meleeDamage,
                    int rangedHit, int rangedDamage, int remainingSeconds) {
        static Modifier none() {
            return new Modifier(0, 0, 0, 0, 0, 0);
        }

        int hit(boolean ranged) {
            return ranged ? rangedHit : meleeHit;
        }

        int damage(boolean ranged) {
            return ranged ? rangedDamage : meleeDamage;
        }

        boolean active() {
            return skillId != 0 && remainingSeconds > 0;
        }
    }

    record Cancellation(int skillId) {
        boolean present() {
            return skillId != 0;
        }
    }

    private int skillId;
    private int meleeHit;
    private int meleeDamage;
    private int rangedHit;
    private int rangedDamage;
    private long expiresAtNanos;

    synchronized int activate(long nowNanos, int durationSeconds, int newSkillId,
                              int newMeleeHit, int newMeleeDamage,
                              int newRangedHit, int newRangedDamage) {
        if (nowNanos < 0 || durationSeconds <= 0 || durationSeconds > 0xFFFF
                || (newSkillId != 8 && newSkillId != 16 && newSkillId != 48)
                || newMeleeHit < -127 || newMeleeHit > 127
                || newMeleeDamage < -127 || newMeleeDamage > 127
                || newRangedHit < -127 || newRangedHit > 127
                || newRangedDamage < -127 || newRangedDamage > 127
                || (newMeleeHit == 0 && newMeleeDamage == 0
                    && newRangedHit == 0 && newRangedDamage == 0)) {
            throw new IllegalArgumentException("invalid classic weapon-buff lease");
        }
        int replaced = skillId;
        skillId = newSkillId;
        meleeHit = newMeleeHit;
        meleeDamage = newMeleeDamage;
        rangedHit = newRangedHit;
        rangedDamage = newRangedDamage;
        expiresAtNanos = Math.addExact(
                nowNanos, TimeUnit.SECONDS.toNanos(durationSeconds));
        return replaced;
    }

    synchronized Modifier modifier(long nowNanos) {
        if (expiresAtNanos == 0 || nowNanos >= expiresAtNanos) {
            return Modifier.none();
        }
        long remainingNanos = expiresAtNanos - nowNanos;
        long remainingSeconds = (remainingNanos + TimeUnit.SECONDS.toNanos(1) - 1)
                / TimeUnit.SECONDS.toNanos(1);
        return new Modifier(skillId, meleeHit, meleeDamage, rangedHit, rangedDamage,
                (int) Math.min(0xFFFFL, Math.max(1L, remainingSeconds)));
    }

    synchronized Modifier expire(long nowNanos) {
        if (expiresAtNanos == 0 || nowNanos < expiresAtNanos) {
            return Modifier.none();
        }
        Modifier expired = new Modifier(
                skillId, meleeHit, meleeDamage, rangedHit, rangedDamage, 0);
        clear();
        return expired;
    }

    synchronized Cancellation cancelMagicEffects() {
        Cancellation result = new Cancellation(skillId);
        clear();
        return result;
    }

    synchronized boolean removeSkill(int expectedSkillId) {
        if (skillId == 0 || skillId != expectedSkillId) return false;
        clear();
        return true;
    }

    synchronized void clear() {
        skillId = 0;
        meleeHit = 0;
        meleeDamage = 0;
        rangedHit = 0;
        rangedDamage = 0;
        expiresAtNanos = 0;
    }
}
