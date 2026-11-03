package local.lito381.server;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.ToIntFunction;

/** Authoritative main-course and dessert leases from {@code L1PcInstance}. */
final class BattleCookingStatus {
    record SlotSnapshot(BattleCookingCatalog.Recipe recipe, int remainingSeconds) {
        static SlotSnapshot empty() {
            return new SlotSnapshot(null, 0);
        }

        boolean active() {
            return recipe != null && remainingSeconds > 0;
        }
    }

    record Snapshot(SlotSnapshot mainCourse, SlotSnapshot dessert) {
        Snapshot {
            if (mainCourse == null || dessert == null) {
                throw new IllegalArgumentException("cooking snapshots are required");
            }
        }

        List<BattleCookingCatalog.Recipe> recipes() {
            List<BattleCookingCatalog.Recipe> result = new ArrayList<>(2);
            if (mainCourse.active()) result.add(mainCourse.recipe());
            if (dessert.active()) result.add(dessert.recipe());
            return List.copyOf(result);
        }

        int maxHp() { return sum(BattleCookingCatalog.Recipe::maxHp); }
        int maxMp() { return sum(BattleCookingCatalog.Recipe::maxMp); }
        int armorClass() { return sum(BattleCookingCatalog.Recipe::armorClass); }
        int allElementResistance() {
            return sum(BattleCookingCatalog.Recipe::allElementResistance);
        }
        int magicResistance() {
            return sum(BattleCookingCatalog.Recipe::magicResistance);
        }
        int hpRegen() { return sum(BattleCookingCatalog.Recipe::hpRegen); }
        int mpRegen() { return sum(BattleCookingCatalog.Recipe::mpRegen); }
        int spellPower() { return sum(BattleCookingCatalog.Recipe::spellPower); }
        int meleeHit() { return sum(BattleCookingCatalog.Recipe::meleeHit); }
        int meleeDamage() { return sum(BattleCookingCatalog.Recipe::meleeDamage); }
        int rangedHit() { return sum(BattleCookingCatalog.Recipe::rangedHit); }
        int rangedDamage() { return sum(BattleCookingCatalog.Recipe::rangedDamage); }
        int physicalHit(boolean ranged) { return ranged ? rangedHit() : meleeHit(); }
        int physicalDamage(boolean ranged) {
            return ranged ? rangedDamage() : meleeDamage();
        }
        int damageReduction() {
            return sum(BattleCookingCatalog.Recipe::damageReduction);
        }

        int experiencePercent() {
            int main = mainCourse.active()
                    ? mainCourse.recipe().experiencePercent() : 0;
            int sweet = dessert.active() ? dessert.recipe().experiencePercent() : 0;
            return Math.max(main, sweet);
        }

        private int sum(ToIntFunction<BattleCookingCatalog.Recipe> field) {
            int total = 0;
            if (mainCourse.active()) total = Math.addExact(total,
                    field.applyAsInt(mainCourse.recipe()));
            if (dessert.active()) total = Math.addExact(total,
                    field.applyAsInt(dessert.recipe()));
            return total;
        }
    }

    private static final class Lease {
        BattleCookingCatalog.Recipe recipe;
        long expiresAtNanos;
    }

    private final Lease mainCourse = new Lease();
    private final Lease dessert = new Lease();

    synchronized BattleCookingCatalog.Recipe activate(
            long nowNanos, BattleCookingCatalog.Recipe next, int durationSeconds) {
        if (nowNanos < 0 || next == null || durationSeconds <= 0
                || durationSeconds > 0xFFFF) {
            throw new IllegalArgumentException("invalid classic cooking lease");
        }
        Lease lease = lease(next.slot());
        BattleCookingCatalog.Recipe previous = lease.recipe;
        lease.recipe = next;
        lease.expiresAtNanos = Math.addExact(nowNanos,
                TimeUnit.SECONDS.toNanos(durationSeconds));
        return previous;
    }

    synchronized BattleCookingCatalog.Recipe remove(BattleCookingCatalog.Slot slot) {
        Lease lease = lease(slot);
        BattleCookingCatalog.Recipe previous = lease.recipe;
        clear(lease);
        return previous;
    }

    synchronized Snapshot active(long nowNanos) {
        return new Snapshot(snapshot(mainCourse, nowNanos), snapshot(dessert, nowNanos));
    }

    synchronized List<BattleCookingCatalog.Recipe> expire(long nowNanos) {
        List<BattleCookingCatalog.Recipe> expired = new ArrayList<>(2);
        expire(mainCourse, nowNanos, expired);
        expire(dessert, nowNanos, expired);
        return List.copyOf(expired);
    }

    synchronized List<BattleCookingCatalog.Recipe> clear() {
        List<BattleCookingCatalog.Recipe> previous = new ArrayList<>(2);
        if (mainCourse.recipe != null) previous.add(mainCourse.recipe);
        if (dessert.recipe != null) previous.add(dessert.recipe);
        clear(mainCourse);
        clear(dessert);
        return List.copyOf(previous);
    }

    private Lease lease(BattleCookingCatalog.Slot slot) {
        return slot == BattleCookingCatalog.Slot.DESSERT ? dessert : mainCourse;
    }

    private static SlotSnapshot snapshot(Lease lease, long nowNanos) {
        if (lease.recipe == null || lease.expiresAtNanos == 0
                || nowNanos >= lease.expiresAtNanos) {
            return SlotSnapshot.empty();
        }
        long remainingNanos = lease.expiresAtNanos - nowNanos;
        long seconds = (remainingNanos + TimeUnit.SECONDS.toNanos(1) - 1)
                / TimeUnit.SECONDS.toNanos(1);
        return new SlotSnapshot(lease.recipe,
                (int) Math.min(0xFFFFL, Math.max(1L, seconds)));
    }

    private static void expire(Lease lease, long nowNanos,
                               List<BattleCookingCatalog.Recipe> expired) {
        if (lease.recipe == null || lease.expiresAtNanos == 0
                || nowNanos < lease.expiresAtNanos) {
            return;
        }
        expired.add(lease.recipe);
        clear(lease);
    }

    private static void clear(Lease lease) {
        lease.recipe = null;
        lease.expiresAtNanos = 0;
    }
}
