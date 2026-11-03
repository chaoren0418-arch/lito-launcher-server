package local.lito381.server;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Server-authoritative, item-local leases for temporary weapon enchantments.
 *
 * <p>The retained 3.81 implementation stores the modifier and timer on each
 * {@code L1ItemInstance}; it is not a character-wide buff.  Consequently two
 * inventory weapons can carry independent leases while a recast on the same
 * object replaces that object's previous timer.</p>
 */
final class BattleItemWeaponEnchantStatus {
    record Modifier(int skillId, long itemObjectId, int meleeDamage,
                    int rangedDamage, int remainingSeconds) {
        static Modifier none(long itemObjectId) {
            return new Modifier(0, itemObjectId, 0, 0, 0);
        }

        int damage(boolean ranged) {
            return ranged ? rangedDamage : meleeDamage;
        }

        boolean active() {
            return skillId != 0 && remainingSeconds > 0;
        }
    }

    record Expiration(int skillId, long itemObjectId, String itemName) {}

    private record Lease(int skillId, long itemObjectId, String itemName,
                         int meleeDamage, int rangedDamage,
                         long expiresAtNanos) {}

    private final LinkedHashMap<Long, Lease> leases = new LinkedHashMap<>();

    synchronized int activate(long nowNanos, int durationSeconds, int skillId,
                              long itemObjectId, String itemName,
                              int meleeDamage, int rangedDamage) {
        if (nowNanos < 0 || durationSeconds <= 0 || durationSeconds > 0xFFFF
                || (skillId != BattleSpellCatalog.SHADOW_FANG
                    && skillId != ClassicWorldGiranCastleCatalog.ENCHANT_WEAPON_SKILL_ID)
                || itemObjectId <= 0 || itemObjectId > 0xFFFF_FFFFL
                || itemName == null || itemName.isBlank()
                || meleeDamage < -127 || meleeDamage > 127
                || rangedDamage < -127 || rangedDamage > 127
                || (meleeDamage == 0 && rangedDamage == 0)) {
            throw new IllegalArgumentException("invalid item weapon-enchant lease");
        }
        Lease previous = leases.put(itemObjectId, new Lease(
                skillId, itemObjectId, itemName, meleeDamage, rangedDamage,
                Math.addExact(nowNanos, TimeUnit.SECONDS.toNanos(durationSeconds))));
        return previous == null ? 0 : previous.skillId();
    }

    synchronized Modifier modifier(long nowNanos, long itemObjectId) {
        Lease lease = leases.get(itemObjectId);
        if (lease == null || nowNanos >= lease.expiresAtNanos()) {
            return Modifier.none(itemObjectId);
        }
        long remainingNanos = lease.expiresAtNanos() - nowNanos;
        long remainingSeconds = (remainingNanos + TimeUnit.SECONDS.toNanos(1) - 1)
                / TimeUnit.SECONDS.toNanos(1);
        return new Modifier(lease.skillId(), itemObjectId,
                lease.meleeDamage(), lease.rangedDamage(),
                (int) Math.min(0xFFFFL, Math.max(1L, remainingSeconds)));
    }

    synchronized List<Expiration> expire(long nowNanos) {
        List<Expiration> expired = new ArrayList<>();
        for (Map.Entry<Long, Lease> entry : List.copyOf(leases.entrySet())) {
            Lease lease = entry.getValue();
            if (nowNanos < lease.expiresAtNanos()) continue;
            leases.remove(entry.getKey());
            expired.add(new Expiration(
                    lease.skillId(), lease.itemObjectId(), lease.itemName()));
        }
        return List.copyOf(expired);
    }

    /** Removes only the selected weapon's mutually-exclusive enchant lease. */
    synchronized int remove(long itemObjectId) {
        if (itemObjectId <= 0 || itemObjectId > 0xFFFF_FFFFL) return 0;
        Lease removed = leases.remove(itemObjectId);
        return removed == null ? 0 : removed.skillId();
    }

    synchronized int activeLeaseCount(long nowNanos) {
        return (int) leases.values().stream()
                .filter(lease -> nowNanos < lease.expiresAtNanos())
                .count();
    }

    synchronized void clear() {
        leases.clear();
    }
}
