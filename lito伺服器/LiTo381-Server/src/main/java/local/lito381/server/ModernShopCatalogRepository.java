package local.lito381.server;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/** Every call resolves a concrete selected source and its source-specific canonical account. */
@FunctionalInterface
interface ModernShopCatalogRepository {
    Optional<ModernShopCatalog.Snapshot> character(ModernAccountAuthentication.Principal principal,int sourceId,long objectId) throws Exception;
    record Source(ModernServerConfig.SourceConfig config,ModernJdbcBackend backend) {
        public Source {
            Objects.requireNonNull(config);Objects.requireNonNull(backend);
            if(!backend.usesConfiguration(config.database()))throw new IllegalArgumentException("shop DAO binding mismatch");
            ModernShopCatalogJdbc.sql(config.catalogDatabase());
        }
        @Override public String toString(){return "ModernShopSource[redacted]";}
    }
    static ModernShopCatalogRepository jdbc(Map<Integer,Source> configured) {
        return jdbc(configured,false);
    }
    static ModernShopCatalogRepository jdbc(Map<Integer,Source> configured,boolean bowEnabled) {
        if(configured==null || configured.isEmpty() || configured.size()>64 || configured.entrySet().stream()
                .anyMatch(e->e.getKey()==null || e.getValue()==null || e.getKey()!=e.getValue().config().id()))
            throw new IllegalArgumentException("explicit shop source bindings required");
        Map<Integer,Source> sources=Map.copyOf(configured);
        return (principal,sourceId,objectId)->{
            Objects.requireNonNull(principal);
            if(!principal.authorizesSource(sourceId) || objectId<1)throw new SecurityException("unauthorized shop source");
            Source source=sources.get(sourceId);if(source==null)throw new IllegalStateException("shop source unavailable");
            if(principal.sources().stream().noneMatch(s->s.id()==sourceId && s.name().equals(source.config().name())))
                throw new SecurityException("shop source identity mismatch");
            return source.backend().shopCharacter(sourceId,principal.accountForSource(sourceId),objectId,source.config().catalogDatabase(),bowEnabled);
        };
    }
    static ModernShopCatalogRepository unavailable(){return (p,s,o)->{throw new IllegalStateException("shop source unavailable");};}
}
