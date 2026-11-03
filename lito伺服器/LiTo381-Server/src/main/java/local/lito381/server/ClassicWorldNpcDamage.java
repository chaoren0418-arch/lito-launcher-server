package local.lito381.server;

/** Retained L1AttackNpc NPC-to-PC source-damage prefix. */
final class ClassicWorldNpcDamage {
    static final String L1_ATTACK_NPC_SOURCE_SHA256 =
            "7BA897448013D57FD77BA05C4213E1A09E9C709CD9199271A2C14F5D2F65C3E8";
    private static final long GAME_TIME_BASE_MILLIS = 1_735_689_600_000L;
    private ClassicWorldNpcDamage() {}

    static int sourceDamage(
            int level, int strength, int strengthDamage,
            int levelRoll, int criticalRoll,
            boolean undeadNightBonus, boolean boss) {
        return sourceDamage(level, strength, strengthDamage,
                levelRoll, criticalRoll, undeadNightBonus, boss, 10);
    }

    static int sourceDamage(
            int level, int strength, int strengthDamage,
            int levelRoll, int criticalRoll,
            boolean undeadNightBonus, boolean boss, int leverage) {
        if (level <= 0 || strength < 0 || levelRoll < 0 || levelRoll >= level
                || criticalRoll < 0 || criticalRoll >= 100 || leverage <= 0) {
            throw new IllegalArgumentException("invalid classic NPC damage roll");
        }
        double damage = levelRoll + strength * 0.8 + strengthDamage;
        if (criticalRoll < 15) damage *= 1.8;
        if (undeadNightBonus) damage *= 1.2;
        damage = (int) (damage * leverage / 10.0);
        if (boss) damage *= 1.8;
        return Math.max(0, (int) damage);
    }

    static boolean retainedNight(long systemTimeMillis) {
        long elapsed = systemTimeMillis - GAME_TIME_BASE_MILLIS;
        if (elapsed < 0) throw new IllegalArgumentException("time predates retained epoch");
        long gameSeconds = elapsed * 6L / 1_000L;
        gameSeconds -= Math.floorMod(gameSeconds, 3L);
        int hour = (int) (Math.floorMod(gameSeconds, 86_400L) / 3_600L);
        return hour < 6 || hour > 17;
    }
}
