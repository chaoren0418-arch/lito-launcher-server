package local.lito381.server;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/** Server-owned exact source/catalog pairing. No fallback to primary or wire account. */
@FunctionalInterface
interface ModernCharacterDataRepository {
    Optional<ModernCharacterData.Snapshot> character(ModernAccountAuthentication.Principal principal,
            int sourceId, long objectId) throws Exception;

    record Source(ModernJdbcBackend backend, String catalogDatabase) {
        public Source { Objects.requireNonNull(backend); ModernCharacterDataJdbc.sql(catalogDatabase); }
        @Override public String toString() { return "ModernCharacterDataSource[redacted]"; }
    }
    static ModernCharacterDataRepository jdbc(Map<Integer, Source> configured) {
        if (configured == null || configured.isEmpty() || configured.size() > 64
                || configured.keySet().stream().anyMatch(id -> id == null || id <= 0)) {
            throw new IllegalArgumentException("explicit modern character data sources required");
        }
        Map<Integer, Source> sources = Map.copyOf(configured);
        return (principal, sourceId, objectId) -> {
            Objects.requireNonNull(principal);
            if (!principal.authorizesSource(sourceId)) throw new SecurityException("unauthorized character data source");
            Source source = sources.get(sourceId);
            if (source == null) throw new IllegalStateException("authorized character data source unavailable");
            return source.backend.persistedCharacter(sourceId, principal.accountForSource(sourceId), objectId, source.catalogDatabase);
        };
    }
    static ModernCharacterDataRepository unavailable() {
        return (principal, sourceId, objectId) -> {
            throw new IllegalStateException("character data backend unavailable");
        };
    }
}
