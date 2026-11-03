package local.lito381.server;

import java.util.Objects;

/** Source authorization only; totals live exclusively in the explicit settlement database. */
@FunctionalInterface
interface ModernCharacterRecordRepository {
    ModernCharacterRecord.Snapshot character(ModernAccountAuthentication.Principal principal,
                                             int sourceId, long objectId) throws Exception;

    static ModernCharacterRecordRepository jdbc(ModernMatchSettlementJdbc.ConnectionFactory factory, String database) {
        var reader = new ModernCharacterRecordJdbc(Objects.requireNonNull(factory), database);
        return (principal, sourceId, objectId) -> {
            Objects.requireNonNull(principal);
            if (!principal.authorizesSource(sourceId)) throw new SecurityException("character record source denied");
            return reader.read(ModernBackendScope.current(), new TournamentPresence.CharacterKey(sourceId, objectId));
        };
    }

    static ModernCharacterRecordRepository unavailable() {
        return (principal, sourceId, objectId) -> { throw new IllegalStateException("modern character records unavailable"); };
    }
}
