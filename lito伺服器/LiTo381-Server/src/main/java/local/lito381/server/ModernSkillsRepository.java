package local.lito381.server;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/** Exact authenticated source/canonical account; never consults native name-keyed skill overrides. */
@FunctionalInterface
interface ModernSkillsRepository {
    Optional<ModernSkills.Snapshot> character(ModernAccountAuthentication.Principal principal,
            int sourceId, long objectId) throws Exception;

    record Source(ModernJdbcBackend backend, String catalogDatabase, boolean fireStorm) {
        Source(ModernJdbcBackend backend,String catalogDatabase){this(backend,catalogDatabase,false);}
        public Source { Objects.requireNonNull(backend); ModernSkillsJdbc.sql(catalogDatabase, true); }
        @Override public String toString() { return "ModernSkillsSource[redacted]"; }
    }
    static ModernSkillsRepository jdbc(Map<Integer, Source> configured) {
        if (configured == null || configured.isEmpty() || configured.size() > 64
                || configured.keySet().stream().anyMatch(id -> id == null || id <= 0)) {
            throw new IllegalArgumentException("explicit modern skill sources required");
        }
        Map<Integer, Source> sources = Map.copyOf(configured);
        return (principal, sourceId, objectId) -> {
            Objects.requireNonNull(principal);
            if (!principal.authorizesSource(sourceId)) throw new SecurityException("unauthorized skill source");
            Source source = sources.get(sourceId);
            if (source == null) throw new IllegalStateException("authorized skill source unavailable");
            return source.backend.skillsCharacter(sourceId, principal.accountForSource(sourceId), objectId, source.catalogDatabase,source.fireStorm);
        };
    }
    static ModernSkillsRepository unavailable() {
        return (principal, sourceId, objectId) -> { throw new IllegalStateException("skill backend unavailable"); };
    }
}
