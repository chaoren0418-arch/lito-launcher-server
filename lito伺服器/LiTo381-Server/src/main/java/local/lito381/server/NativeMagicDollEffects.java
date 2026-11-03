package local.lito381.server;

import java.util.ArrayList;
import java.util.List;
import java.util.function.IntSupplier;

/** Exact runtime grouping for native magic-doll proc and one-second timers. */
final class NativeMagicDollEffects {
    private NativeMagicDollEffects() {}

    record AttackProc(int additionalDamage,
                      List<SourceMagicDollPower> damageTriggers,
                      List<SourceMagicDollPower> skillTriggers) {
        AttackProc {
            if (additionalDamage < 0 || damageTriggers == null
                    || skillTriggers == null) {
                throw new IllegalArgumentException("invalid doll attack proc result");
            }
            damageTriggers = List.copyOf(damageTriggers);
            skillTriggers = List.copyOf(skillTriggers);
        }

        static AttackProc empty() {
            return new AttackProc(0, List.of(), List.of());
        }

        boolean triggered() {
            return additionalDamage > 0 || !skillTriggers.isEmpty();
        }
    }

    record DefenseProc(int damageReduction,
                       List<SourceMagicDollPower> triggers) {
        DefenseProc {
            if (damageReduction < 0 || triggers == null) {
                throw new IllegalArgumentException("invalid doll defense proc result");
            }
            triggers = List.copyOf(triggers);
        }

        static DefenseProc empty() {
            return new DefenseProc(0, List.of());
        }
    }

    static AttackProc attack(NativeMagicDollPowers powers, IntSupplier d1000) {
        if (powers == null || d1000 == null) {
            throw new IllegalArgumentException("doll attack proc inputs are required");
        }
        int damage = 0;
        List<SourceMagicDollPower> damageTriggers = new ArrayList<>();
        List<SourceMagicDollPower> skillTriggers = new ArrayList<>();
        for (SourceMagicDollPower power : powers.activeDefinitions()) {
            if (!(power.className().equals("ACTIVE_SKILL")
                    || power.className().equals("ACTIVE_DMG")
                        && power.mode() == 1)) continue;
            if (!triggered(power.probability(), d1000.getAsInt())) continue;
            if (power.className().equals("ACTIVE_SKILL")) {
                skillTriggers.add(power);
            } else {
                damage = Math.addExact(damage, power.valueSub());
                damageTriggers.add(power);
            }
        }
        return new AttackProc(damage, damageTriggers, skillTriggers);
    }

    static DefenseProc defense(NativeMagicDollPowers powers, IntSupplier d1000) {
        if (powers == null || d1000 == null) {
            throw new IllegalArgumentException("doll defense proc inputs are required");
        }
        int reduction = 0;
        List<SourceMagicDollPower> triggers = new ArrayList<>();
        for (SourceMagicDollPower power : powers.activeDefinitions()) {
            boolean reductionProc = power.className().equals("ACTIVE_REDUX")
                    || power.className().equals("ACTIVE_DMG") && power.mode() == 2;
            if (!reductionProc
                    || !triggered(power.probability(), d1000.getAsInt())) continue;
            reduction = Math.addExact(reduction, power.valueSub());
            triggers.add(power);
        }
        return new DefenseProc(reduction, triggers);
    }

    private static boolean triggered(int probability, int roll) {
        if (roll < 0 || roll >= 1000) {
            throw new IllegalArgumentException("doll proc roll must be 0..999");
        }
        return probability > 0 && roll < Math.min(1000, probability);
    }

    enum AwardType { HP, MP, ITEM }

    record PeriodicAward(AwardType type, int amount, int itemId, int itemCount) {
        PeriodicAward {
            if (type == null || amount < 0 || itemId < 0 || itemCount < 0) {
                throw new IllegalArgumentException("invalid doll periodic award");
            }
        }

        static PeriodicAward recovery(AwardType type, int amount) {
            return new PeriodicAward(type, amount, 0, 0);
        }

        static PeriodicAward item(int itemId, int itemCount) {
            return new PeriodicAward(AwardType.ITEM, 0, itemId, itemCount);
        }
    }

    /** One mutable cadence ledger; caller serializes it with the doll lifecycle. */
    static final class PeriodicState {
        private static final PeriodicState EMPTY =
                new PeriodicState(0, 0, 0, 0, 0, 0, 0);

        private final int hpAmount;
        private final int hpInterval;
        private final int mpAmount;
        private final int mpInterval;
        private final int itemId;
        private final int itemCount;
        private final int itemInterval;
        private int hpRemaining;
        private int mpRemaining;
        private int itemRemaining;

        private PeriodicState(int hpAmount, int hpInterval,
                              int mpAmount, int mpInterval,
                              int itemId, int itemCount, int itemInterval) {
            this.hpAmount = hpAmount;
            this.hpInterval = hpInterval;
            this.mpAmount = mpAmount;
            this.mpInterval = mpInterval;
            this.itemId = itemId;
            this.itemCount = itemCount;
            this.itemInterval = itemInterval;
            hpRemaining = hpInterval;
            mpRemaining = mpInterval;
            itemRemaining = itemInterval;
        }

        static PeriodicState empty() {
            return EMPTY;
        }

        static PeriodicState from(NativeMagicDollPowers powers) {
            if (powers == null) throw new IllegalArgumentException("powers are required");
            int hpAmount = 0, hpInterval = 0;
            int mpAmount = 0, mpInterval = 0;
            int itemId = 0, itemCount = 0, itemInterval = 0;
            for (SourceMagicDollPower power : powers.timedDefinitions()) {
                switch (power.className()) {
                    case "TIMED_HP_REGEN" -> {
                        hpAmount = Math.addExact(hpAmount, power.valueMain());
                        hpInterval = Math.addExact(hpInterval, power.intervalSeconds());
                    }
                    case "TIMED_MP_REGEN" -> {
                        mpAmount = Math.addExact(mpAmount, power.valueMain());
                        mpInterval = Math.addExact(mpInterval, power.intervalSeconds());
                    }
                    case "TIMED_ITEM" -> {
                        itemId = Math.addExact(itemId, power.itemId());
                        itemCount = Math.addExact(itemCount, power.itemCount());
                        itemInterval = Math.addExact(itemInterval, power.intervalSeconds());
                    }
                    default -> { }
                }
            }
            if (hpInterval == 0 && mpInterval == 0 && itemInterval == 0) return EMPTY;
            return new PeriodicState(hpAmount, hpInterval, mpAmount, mpInterval,
                    itemId, itemCount, itemInterval);
        }

        List<PeriodicAward> advance(boolean hpEligible, boolean mpEligible) {
            if (this == EMPTY) return List.of();
            List<PeriodicAward> awards = new ArrayList<>(3);
            if (hpEligible && hpInterval > 0 && --hpRemaining <= 0) {
                hpRemaining = hpInterval;
                awards.add(PeriodicAward.recovery(AwardType.HP, hpAmount));
            }
            if (mpEligible && mpInterval > 0 && --mpRemaining <= 0) {
                mpRemaining = mpInterval;
                awards.add(PeriodicAward.recovery(AwardType.MP, mpAmount));
            }
            if (itemInterval > 0 && --itemRemaining <= 0) {
                itemRemaining = itemInterval;
                awards.add(PeriodicAward.item(itemId, itemCount));
            }
            return List.copyOf(awards);
        }
    }
}
