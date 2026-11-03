package local.lito381.server;

/**
 * Exact retained {@code L1AttackNpc} NPC-to-NPC physical prefix used by a
 * classic WORLD pet, tamed monster, or summon.
 *
 * <p>The selected LiTo 3.81 client has no pet-equipment request receiver, so
 * callers deliberately provide zero for the retained pet weapon hit/damage
 * hooks.  The hooks remain explicit here so the server formula does not lose
 * the source ordering.</p>
 */
final class ClassicWorldCompanionNpcPhysicalAttack {
    static final String L1_ATTACK_NPC_SOURCE_SHA256 =
            "7BA897448013D57FD77BA05C4213E1A09E9C709CD9199271A2C14F5D2F65C3E8";
    static final String L1_ATTACK_MODE_SOURCE_SHA256 =
            "F29C4D0BCC85CA3E6399497FA45B97B14047D4161E32A02525940E14290AE2C2";
    static final String L1_PET_INSTANCE_SOURCE_SHA256 =
            "80B32582477FB3A2395135EAD8C4E7424A7EA7374787E0596F8D810021422729";

    private ClassicWorldCompanionNpcPhysicalAttack() {}

    record HitOutcome(int baseHitRate, int attackerDice, int defenderDice,
                      int normalizedHitRate, int percentileRoll, boolean hit) {}

    record DamageOutcome(
            int sourceRollBound, int sourceRoll, int criticalRoll,
            boolean critical, boolean undeadNightBonus,
            int targetAcMaximum, int targetAcRoll, int targetReduction,
            int weaponBreakStacks, int immuneDivisor,
            double beforeTargetReduction, int damage) {}

    static HitOutcome hit(
            int level, int hitUp, int petWeaponHit, boolean boss,
            int targetArmorClass, int d20Roll, int negativeAcRoll,
            int percentileRoll) {
        if (level <= 0 || hitUp < 0 || petWeaponHit < 0
                || d20Roll < 1 || d20Roll > 20
                || percentileRoll < 1 || percentileRoll > 100) {
            throw new IllegalArgumentException("invalid companion NPC hit roll");
        }
        int defenderValue = Math.max(0, -targetArmorClass);
        if (targetArmorClass >= 0 && negativeAcRoll != 0
                || targetArmorClass < 0
                    && (negativeAcRoll < 1 || negativeAcRoll > defenderValue)) {
            throw new IllegalArgumentException("invalid companion NPC defender roll");
        }
        int baseHitRate = Math.addExact(level + 3,
                Math.addExact(hitUp, petWeaponHit));
        int attackerDice = Math.addExact(d20Roll + baseHitRate - 3,
                boss ? 30 : 0);
        int defenderDice = targetArmorClass >= 0
                ? 10 - targetArmorClass : 10 + negativeAcRoll;
        int normalizedHitRate;
        if (attackerDice <= baseHitRate) normalizedHitRate = 15;
        else if (attackerDice >= baseHitRate + 19) normalizedHitRate = 100;
        else normalizedHitRate = attackerDice > defenderDice ? 100 : 15;
        return new HitOutcome(baseHitRate, attackerDice, defenderDice,
                normalizedHitRate, percentileRoll,
                normalizedHitRate >= percentileRoll);
    }

    static DamageOutcome damage(
            BattleCompanion.Kind kind,
            int runtimeLevel, int sourceTemplateLevel,
            int strength, int summonStrengthDamage,
            int sourceRoll, int criticalRoll,
            int petWeaponDamage, int damageUp, int undead,
            boolean boss, int leverage, int weaponBreakStacks,
            SourceNpcPhysicalStats target, int targetAcRoll,
            int immuneDivisor, long systemTimeMillis) {
        if (kind == null || kind == BattleCompanion.Kind.ARENA
                || runtimeLevel <= 0 || sourceTemplateLevel <= 0
                || strength < 0 || summonStrengthDamage < 0
                || petWeaponDamage < 0 || undead < 0
                || criticalRoll < 0 || criticalRoll >= 100
                || leverage <= 0 || weaponBreakStacks < 0
                || weaponBreakStacks > BattleWeaponBreak.MAX_STACKS
                || target == null || immuneDivisor <= 0) {
            throw new IllegalArgumentException("invalid companion NPC damage input");
        }
        boolean petFormula = kind == BattleCompanion.Kind.PET
                || kind == BattleCompanion.Kind.TAMED;
        int sourceRollBound = petFormula ? sourceTemplateLevel : runtimeLevel;
        if (sourceRoll < 0 || sourceRoll >= sourceRollBound) {
            throw new IllegalArgumentException("invalid companion NPC source roll");
        }
        int targetAcMaximum = BattlePhysicalDefense.npcAcMaximum(target);
        if (targetAcMaximum == 0 ? targetAcRoll != 0
                : targetAcRoll < 0 || targetAcRoll >= targetAcMaximum) {
            throw new IllegalArgumentException("invalid companion NPC AC roll");
        }

        double amount = petFormula
                ? sourceRoll + strength / 2 + 1
                    + runtimeLevel / 14 + petWeaponDamage
                : sourceRoll + strength / 2 + summonStrengthDamage;
        boolean critical = criticalRoll < 15;
        if (critical) amount *= 1.80D;
        amount += damageUp;
        boolean undeadNightBonus = switch (undead) {
            case 1, 3, 4 -> ClassicWorldNpcDamage.retainedNight(systemTimeMillis);
            default -> false;
        };
        if (undeadNightBonus) amount *= 1.20D;
        amount = (int) (amount * (leverage / 10D));
        if (boss) amount *= 1.80D;
        if (weaponBreakStacks > 0) {
            amount = amount
                    * (100 - weaponBreakStacks * BattleWeaponBreak.PERCENT_PER_STACK)
                    / 100.0D;
            if (amount < 1) amount = 1;
        }
        double beforeTargetReduction = amount;
        int targetReduction = Math.addExact(target.damageReduction(),
                targetAcMaximum == 0 ? 0 : targetAcMaximum + targetAcRoll);
        amount -= targetReduction;
        amount /= immuneDivisor;
        int damage = amount <= 0 ? 0 : (int) amount;
        return new DamageOutcome(sourceRollBound, sourceRoll, criticalRoll,
                critical, undeadNightBonus, targetAcMaximum, targetAcRoll,
                targetReduction, weaponBreakStacks, immuneDivisor,
                beforeTargetReduction, damage);
    }
}
