package local.lito381.server;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/** Item-local retained Enchant Armor leases used by the Giran NPC service. */
final class BattleItemArmorEnchantStatus {
    record Modifier(int skillId, long itemObjectId, int armorClass,
                    int remainingSeconds) {
        static Modifier none(long itemObjectId) {
            return new Modifier(0, itemObjectId, 0, 0);
        }

        boolean active() {
            return skillId != 0 && remainingSeconds > 0;
        }
    }

    record Expiration(int skillId, long itemObjectId, String itemName) {}

    private record Lease(int skillId, long itemObjectId, String itemName,
                         int armorClass, long expiresAtNanos) {}

    private final LinkedHashMap<Long, Lease> leases = new LinkedHashMap<>();

    synchronized int activate(long nowNanos, int durationSeconds, int skillId,
                              long itemObjectId, String itemName, int armorClass) {
        if (nowNanos < 0 || durationSeconds <= 0 || durationSeconds > 0xFFFF
                || skillId != ClassicWorldGiranCastleCatalog.ENCHANT_ARMOR_SKILL_ID
                || itemObjectId <= 0 || itemObjectId > 0xFFFF_FFFFL
                || itemName == null || itemName.isBlank()
                || armorClass >= 0 || armorClass < -127) {
            throw new IllegalArgumentException("invalid item armor-enchant lease");
        }
        Lease previous = leases.put(itemObjectId, new Lease(
                skillId, itemObjectId, itemName, armorClass,
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
        return new Modifier(lease.skillId(), itemObjectId, lease.armorClass(),
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

    synchronized int activeLeaseCount(long nowNanos) {
        return (int) leases.values().stream()
                .filter(lease -> nowNanos < lease.expiresAtNanos())
                .count();
    }

    synchronized boolean removeSkill(int skillId) {
        int before = leases.size();
        leases.entrySet().removeIf(entry -> entry.getValue().skillId() == skillId);
        return leases.size() != before;
    }

    synchronized void clear() {
        leases.clear();
    }
}
