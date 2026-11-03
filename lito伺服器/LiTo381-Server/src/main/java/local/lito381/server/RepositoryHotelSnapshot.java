// DECOMPILED (CFR 0.152) from reference/base-0.6.71.jar — TODO: replace with original source (T002)
/*
 * Decompiled with CFR 0.152.
 */
package local.lito381.server;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import local.lito381.server.GameStateStore;

record RepositoryHotelSnapshot(boolean authoritative, List<GameStateStore.HotelLeaseState> leases, List<GameStateStore.HotelKeyState> keys) {
    RepositoryHotelSnapshot {
        leases = leases == null ? List.of() : List.copyOf(leases);
        keys = keys == null ? List.of() : List.copyOf(keys);
        LinkedHashSet<Integer> shows = new LinkedHashSet<Integer>();
        LinkedHashMap<Integer, GameStateStore.HotelLeaseState> byShow = new LinkedHashMap<Integer, GameStateStore.HotelLeaseState>();
        for (GameStateStore.HotelLeaseState lease : leases) {
            if (lease == null || !shows.add(lease.showId())) {
                throw new IllegalArgumentException("duplicate/null repository hotel lease");
            }
            byShow.put(lease.showId(), lease);
        }
        LinkedHashSet<Long> objects = new LinkedHashSet<Long>();
        for (GameStateStore.HotelKeyState key : keys) {
            GameStateStore.HotelLeaseState lease;
            GameStateStore.HotelLeaseState hotelLeaseState = lease = key == null ? null : (GameStateStore.HotelLeaseState)byShow.get(key.showId());
            if (key != null && objects.add(key.itemObjectId()) && lease != null && key.npcId() == lease.npcId() && key.meetingHall() == lease.meetingHall() && key.expiresAtEpochSeconds() == lease.expiresAtEpochSeconds()) continue;
            throw new IllegalArgumentException("invalid repository hotel key relation");
        }
        if (!(authoritative || leases.isEmpty() && keys.isEmpty())) {
            throw new IllegalArgumentException("unavailable hotel snapshot has rows");
        }
    }

    static RepositoryHotelSnapshot unavailable() {
        return new RepositoryHotelSnapshot(false, List.of(), List.of());
    }

    GameStateStore.HotelOccupancy occupancy(int npcId, String renterIdentity) {
        String identity = renterIdentity == null ? "" : renterIdentity.trim();
        long now = System.currentTimeMillis() / 1000L;
        GameStateStore.HotelLeaseState renter = null;
        boolean[] occupied = new boolean[16];
        int rooms = 0;
        int halls = 0;
        for (GameStateStore.HotelLeaseState lease : this.leases) {
            if (lease.npcId() != npcId || !lease.active(now)) continue;
            occupied[lease.roomNumber()] = true;
            if (lease.meetingHall()) {
                ++halls;
            } else {
                ++rooms;
            }
            if (!lease.renterIdentity().equalsIgnoreCase(identity)) continue;
            renter = lease;
        }
        int free = -1;
        for (int index = 0; index < occupied.length; ++index) {
            if (occupied[index]) continue;
            free = index;
            break;
        }
        return new GameStateStore.HotelOccupancy(renter, rooms, halls, free);
    }

    GameStateStore.HotelLeaseState activeLeaseForKeys(int npcId, Set<Long> objectIds) {
        if (objectIds == null || objectIds.isEmpty()) {
            return null;
        }
        long now = System.currentTimeMillis() / 1000L;
        Map<Integer, GameStateStore.HotelLeaseState> byShow = this.leases.stream().collect(Collectors.toMap(GameStateStore.HotelLeaseState::showId, lease -> lease));
        for (GameStateStore.HotelKeyState key : this.keys) {
            GameStateStore.HotelLeaseState lease2;
            if (key.npcId() != npcId || !objectIds.contains(key.itemObjectId()) || (lease2 = byShow.get(key.showId())) == null || !lease2.active(now)) continue;
            return lease2;
        }
        return null;
    }

    GameStateStore.HotelReturnQuote returnQuote(int npcId, String renterIdentity, Set<Long> inventoryObjectIds) {
        String identity = renterIdentity == null ? "" : renterIdentity.trim();
        long now = System.currentTimeMillis() / 1000L;
        GameStateStore.HotelLeaseState owned = this.leases.stream().filter(lease -> lease.npcId() == npcId && !lease.released() && lease.renterIdentity().equalsIgnoreCase(identity)).sorted(Comparator.comparingInt(lease -> lease.active(now) ? 0 : 1).thenComparingInt(GameStateStore.HotelLeaseState::roomNumber)).findFirst().orElse(null);
        Set returned = this.keys.stream().filter(key -> key.npcId() == npcId && inventoryObjectIds != null && inventoryObjectIds.contains(key.itemObjectId())).map(GameStateStore.HotelKeyState::itemObjectId).collect(Collectors.toCollection(LinkedHashSet::new));
        int refund = Math.multiplyExact(returned.size(), 20);
        if (owned != null && owned.active(now)) {
            refund = Math.addExact(refund, 60);
        }
        return new GameStateStore.HotelReturnQuote(npcId, identity, refund, owned != null, returned);
    }
}
