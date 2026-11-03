package local.lito381.server;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/** Source/account-owned learned skill43 facts. This is a bounded read boundary,
 * not a wire capability, a cached grant or permission to cast in a match. */
@FunctionalInterface
interface ModernHasteRepository {
    Optional<ModernHasteCatalog.Snapshot> character(
            ModernAccountAuthentication.Principal principal,int sourceId,long objectId)throws Exception;

    record Source(ModernJdbcBackend backend,String catalogDatabase) {
        public Source {
            Objects.requireNonNull(backend);
            ModernHasteCatalog.sql(catalogDatabase,true);
        }
        @Override public String toString(){return "ModernHasteSource[redacted]";}
    }

    static ModernHasteRepository jdbc(Map<Integer,Source> configured) {
        if(configured==null||configured.isEmpty()||configured.size()>64
                ||configured.keySet().stream().anyMatch(id->id==null||id<1))
            throw new IllegalArgumentException("explicit haste sources required");
        var sources=Map.copyOf(configured);
        return (principal,sourceId,objectId)->{
            Objects.requireNonNull(principal);
            if(!principal.authorizesSource(sourceId))throw new SecurityException("unauthorized haste source");
            var source=sources.get(sourceId);
            if(source==null)throw new IllegalStateException("authorized haste source unavailable");
            return source.backend.hasteCharacter(sourceId,principal.accountForSource(sourceId),
                    objectId,source.catalogDatabase);
        };
    }
}
