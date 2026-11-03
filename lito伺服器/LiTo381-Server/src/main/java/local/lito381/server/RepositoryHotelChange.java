// DECOMPILED (CFR 0.152) from reference/base-0.6.71.jar — TODO: replace with original source (T002)
/*
 * Decompiled with CFR 0.152.
 */
package local.lito381.server;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import local.lito381.server.GameStateStore;
import local.lito381.server.RepositoryHotelChangeKind;

record RepositoryHotelChange(RepositoryHotelChangeKind kind, GameStateStore.HotelLeaseState lease, GameStateStore.HotelReturnQuote returnQuote, Set<Long> keyObjectIds) {
    RepositoryHotelChange {
        if (kind == null) {
            throw new IllegalArgumentException("hotel change kind is required");
        }
        Set<Object> set = keyObjectIds = keyObjectIds == null ? Set.of() : Set.copyOf(keyObjectIds);
        if (kind == RepositoryHotelChangeKind.RENT && (lease == null || returnQuote != null || keyObjectIds.isEmpty()) || kind == RepositoryHotelChangeKind.RETURN && (lease != null || returnQuote == null)) {
            throw new IllegalArgumentException("invalid hotel SQL change");
        }
    }

    static RepositoryHotelChange rent(GameStateStore.HotelLeaseState lease, List<Long> keyObjectIds) {
        return new RepositoryHotelChange(RepositoryHotelChangeKind.RENT, lease, null, new LinkedHashSet<Long>(keyObjectIds));
    }

    static RepositoryHotelChange returning(GameStateStore.HotelReturnQuote quote) {
        return new RepositoryHotelChange(RepositoryHotelChangeKind.RETURN, null, quote, quote.returnedKeyObjectIds());
    }
}
