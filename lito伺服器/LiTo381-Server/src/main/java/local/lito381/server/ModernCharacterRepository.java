package local.lito381.server;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Read-only listing adapter. Every source has an EXPLICIT repository, never primary fallback. */
@FunctionalInterface
interface ModernCharacterRepository {
    List<ModernApplicationProtocol.CharacterRow> characters(ModernAccountAuthentication.Principal principal,
                                                           int sourceId) throws Exception;

    static ModernCharacterRepository repositories(Map<Integer, AccountRepository> configured) {
        if (configured == null || configured.isEmpty() || configured.size() > 64
                || configured.keySet().stream().anyMatch(id -> id == null || id <= 0)) {
            throw new IllegalArgumentException("explicit character source repositories required");
        }
        Map<Integer, AccountRepository> repositories = Map.copyOf(configured);
        if (repositories.values().stream().anyMatch(repository -> repository instanceof SourceRoutedAccountRepository)) {
            throw new IllegalArgumentException("map each source to its concrete repository, not the primary router");
        }
        return (principal, sourceId) -> {
            Objects.requireNonNull(principal);
            if (!principal.authorizesSource(sourceId)) throw new SecurityException("unauthorized character source");
            AccountRepository repository = repositories.get(sourceId);
            if (repository == null) throw new IllegalStateException("authorized character source unavailable");
            // The owner is the canonical account returned by THIS source's
            // authentication, not the wire/display principal account or input alias.
            // Do not call SourceRoutedAccountRepository.characters(): that routes to primary.
            List<CharacterSummary> snapshots = repository.characters(principal.accountForSource(sourceId));
            if (snapshots == null || snapshots.size() > ModernApplicationProtocol.MAX_ROWS) {
                throw new IllegalStateException("invalid character snapshot");
            }
            return snapshots.stream().map(snapshot -> new ModernApplicationProtocol.CharacterRow(
                            snapshot.objectId(), snapshot.name(), snapshot.type(), snapshot.sex(), snapshot.level()))
                    .sorted(Comparator.comparingLong(ModernApplicationProtocol.CharacterRow::objectId)).toList();
        };
    }
}
