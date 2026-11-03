package local.lito381.server;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/** Selected-source authority only; must be re-read for each command, not cached at login. */
@FunctionalInterface
interface ModernGmCommandRepository {
    Optional<ModernGmCommands.Snapshot> character(ModernAccountAuthentication.Principal principal,
            int sourceId,long objectId) throws Exception;

    record Source(ModernServerConfig.SourceConfig config,ModernJdbcBackend backend) {
        public Source {
            Objects.requireNonNull(config); Objects.requireNonNull(backend);
            if (!backend.usesConfiguration(config.database())) throw new IllegalArgumentException("GM DAO binding mismatch");
            ModernGmCommandJdbc.sql(config.catalogDatabase());
        }
        @Override public String toString() { return "ModernGmSource[redacted]"; }
    }
    static ModernGmCommandRepository jdbc(Map<Integer,Source> configured) {
        if (configured == null || configured.isEmpty() || configured.size() > 64
                || configured.entrySet().stream().anyMatch(e -> e.getKey() == null || e.getValue() == null
                    || e.getKey() != e.getValue().config().id())) throw new IllegalArgumentException("explicit GM sources required");
        Map<Integer,Source> sources = Map.copyOf(configured);
        return (principal,sourceId,objectId) -> {
            Objects.requireNonNull(principal);
            if (!principal.authorizesSource(sourceId) || objectId <= 0) throw new SecurityException("unauthorized GM source");
            var source = sources.get(sourceId);
            if (source == null) throw new IllegalStateException("GM source unavailable");
            if (principal.sources().stream().noneMatch(s -> s.id() == sourceId && s.name().equals(source.config().name())))
                throw new SecurityException("GM source identity mismatch");
            return source.backend().gmCommandCharacter(sourceId,principal.accountForSource(sourceId),objectId,source.config().catalogDatabase());
        };
    }
    static ModernGmCommandRepository unavailable() {
        return (principal,sourceId,objectId) -> { throw new IllegalStateException("GM source unavailable"); };
    }
}
