package local.lito381.server;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/** Prepared skill77 facts bound to the authenticated source/account. Still not
 * a wire capability or permission to execute a cast in any particular match.
 */
@FunctionalInterface
interface ModernDisintegrateRepository {
    Optional<ModernDisintegrateCatalog.Snapshot> character(ModernAccountAuthentication.Principal principal,int sourceId,long objectId)throws Exception;
    record Source(ModernJdbcBackend backend,String catalogDatabase) {
        public Source {Objects.requireNonNull(backend);ModernDisintegrateCatalog.sql(catalogDatabase,true);}
        @Override public String toString(){return "ModernDisintegrateSource[redacted]";}
    }
    static ModernDisintegrateRepository jdbc(Map<Integer,Source> configured) {
        if(configured==null||configured.isEmpty()||configured.size()>64||configured.keySet().stream().anyMatch(k->k==null||k<1))
            throw new IllegalArgumentException("explicit disintegrate sources required");
        var sources=Map.copyOf(configured);
        return (principal,sourceId,objectId)->{
            Objects.requireNonNull(principal);
            if(!principal.authorizesSource(sourceId))throw new SecurityException("unauthorized disintegrate source");
            var source=sources.get(sourceId);if(source==null)throw new IllegalStateException("authorized disintegrate source unavailable");
            return source.backend.disintegrateCharacter(sourceId,principal.accountForSource(sourceId),objectId,source.catalogDatabase);
        };
    }
}
