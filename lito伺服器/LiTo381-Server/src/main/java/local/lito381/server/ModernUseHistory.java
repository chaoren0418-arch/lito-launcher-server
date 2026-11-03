package local.lito381.server;

import java.util.Objects;

/** Internal historical receipts. Current equipment must never reinterpret a
 * previous use. Not a wire capability, target-damage proof or authorization. */
final class ModernUseHistory {
    enum Lane { TELEPORT, LIGHT_ARROW, HEALING, FIRE_STORM, DISINTEGRATE,
        NPC_LIGHT_ARROW, NPC_FIRE_STORM, NPC_DISINTEGRATE, GREATER_HASTE, HASTE, SLOW }

    /** Actual actor resources/capacities and life immediately before this use. */
    record Anchor(long equipmentRevision, long life, ModernEquipmentVitals.Vitals vitals, int x, int y) {
        Anchor {
            Objects.requireNonNull(vitals);
            if (equipmentRevision < 0 || life < 1 || vitals.hp() == 0) throw bad();
        }
    }
    record Cast(long revision, long sequence, Anchor before, int cost, long mpAfter) {
        Cast {
            Objects.requireNonNull(before);
            if (revision < 2 || sequence < 1 || cost < 0 || cost > before.vitals().mp()
                    || mpAfter != before.vitals().mp() - cost) throw bad();
        }
    }
    record Spell(ModernSkillSpending.Ledger current, Cast last) {
        Spell {
            Objects.requireNonNull(current);
            if ((current.casts() == 0) != (last == null)) throw bad();
            if (last != null && (last.revision() != current.lastCastRevision()
                    || last.sequence() != current.lastCastSequence()
                    || last.cost() > current.spent() || current.casts() == 1 && last.cost() != current.spent()
                    || last.before().equipmentRevision() > current.equipmentRevision()
                    || (last.before().equipmentRevision() == current.equipmentRevision()
                        ? last.revision() <= current.profileRevision() || last.cost() != current.cost()
                        : last.revision() >= current.profileRevision()))) throw bad();
        }
        static Spell initial(long gear, long revision, int cost) {
            return new Spell(ModernSkillSpending.Ledger.initial(gear, revision, cost), null);
        }
        Spell cast(long revision, long sequence, Anchor before) {
            if (before.equipmentRevision() != current.equipmentRevision()
                    || last != null && before.life() < last.before().life()) throw bad();
            return new Spell(current.cast(revision, sequence), new Cast(revision, sequence, before,
                    current.cost(), Math.subtractExact(before.vitals().mp(), current.cost())));
        }
        Spell reprice(long gear, long revision, int cost) {
            return new Spell(current.reprice(gear, revision, cost), last);
        }
    }
    /** rolledBase is the actual qualified potion roll, zero for timed items.
     * fixedRecovery is the applied equipment bonus at this use, not now. */
    record Use(long revision, long sequence, long itemId, Anchor before,
            long fixedRecovery, long rolledBase, long restored, long hpAfter) {
        Use {
            Objects.requireNonNull(before);
            if (revision < 2 || sequence < 1 || fixedRecovery < 0 || restored < 0) throw bad();
            var kind = ModernItemProtocol.HealingKind.find(itemId);
            long expected;
            if (kind != null) {
                if (rolledBase < kind.minimum || rolledBase > kind.maximum) throw bad();
                expected = Math.min(Math.addExact(rolledBase, fixedRecovery),
                        before.vitals().maximumHp() - before.vitals().hp());
            } else {
                if (ModernTimedEffects.Kind.find(itemId) == null || rolledBase != 0 || fixedRecovery != 0) throw bad();
                expected = 0;
            }
            if (restored != expected || hpAfter != Math.addExact(before.vitals().hp(), expected)) throw bad();
        }
    }
    record Items(long equipmentRevision, long profileRevision, long fixedRecovery,
            long uses, long restored, Use last) {
        Items {
            if (equipmentRevision < 0 || profileRevision < 1 || fixedRecovery < 0 || uses < 0 || restored < 0
                    || (uses == 0) != (last == null) || uses == 0 && restored != 0) throw bad();
            if (last != null && (uses > last.sequence() || uses >= last.revision() || restored < last.restored()
                    || uses == 1 && restored != last.restored()
                    || last.before().equipmentRevision() > equipmentRevision
                    || (last.before().equipmentRevision() == equipmentRevision
                        ? last.revision() <= profileRevision || ModernItemProtocol.HealingKind.find(last.itemId()) != null
                            && last.fixedRecovery() != fixedRecovery
                        : last.revision() >= profileRevision))) throw bad();
        }
        static Items initial(long gear, long revision, long fixed) { return new Items(gear, revision, fixed, 0, 0, null); }
        Items use(long revision, long sequence, long item, Anchor before, long rolledBase) {
            if (before.equipmentRevision() != equipmentRevision || revision <= profileRevision
                    || last != null && (revision <= last.revision() || sequence <= last.sequence()
                        || before.life() < last.before().life())) throw bad();
            boolean potion = ModernItemProtocol.HealingKind.find(item) != null;
            long fixed = potion ? fixedRecovery : 0;
            long gain = potion ? Math.min(Math.addExact(rolledBase, fixed), before.vitals().maximumHp() - before.vitals().hp()) : 0;
            var receipt = new Use(revision, sequence, item, before, fixed, rolledBase, gain, Math.addExact(before.vitals().hp(), gain));
            return new Items(equipmentRevision, profileRevision, fixedRecovery, Math.incrementExact(uses),
                    Math.addExact(restored, gain), receipt);
        }
        Items reprofile(long gear, long revision, long fixed) {
            if (gear != Math.incrementExact(equipmentRevision) || revision <= profileRevision
                    || last != null && revision <= last.revision()) throw bad();
            return new Items(gear, revision, fixed, uses, restored, last);
        }
    }
    static boolean follows(Spell before, Spell after) {
        if (before == null || after == null) return false;
        if (before.equals(after)) return true;
        try {
            if (after.last() != null && before.cast(after.last().revision(), after.last().sequence(), after.last().before()).equals(after)) return true;
        } catch (IllegalArgumentException | ArithmeticException ignored) { }
        try { return before.reprice(after.current().equipmentRevision(), after.current().profileRevision(), after.current().cost()).equals(after); }
        catch (IllegalArgumentException | ArithmeticException ignored) { return false; }
    }
    static boolean follows(Items before, Items after) {
        if (before == null || after == null) return false;
        if (before.equals(after)) return true;
        try {
            if (after.last() != null && before.use(after.last().revision(), after.last().sequence(), after.last().itemId(), after.last().before(), after.last().rolledBase()).equals(after)) return true;
        } catch (IllegalArgumentException | ArithmeticException ignored) { }
        try { return before.reprofile(after.equipmentRevision(), after.profileRevision(), after.fixedRecovery()).equals(after); }
        catch (IllegalArgumentException | ArithmeticException ignored) { return false; }
    }
    private ModernUseHistory() { }
    private static IllegalArgumentException bad() { return new IllegalArgumentException("invalid historical resource use"); }
}
