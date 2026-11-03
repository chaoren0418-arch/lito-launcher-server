package local.lito381.server;

import java.nio.file.Path;

/** Explicit stopped-server migration for LiTo-owned dynamic and static extensions. */
public final class DatabaseSchemaMigrationMain {
    private DatabaseSchemaMigrationMain() {}

    public static void main(String[] args) throws Exception {
        if (args.length != 2 || !"--apply-lito-extensions".equals(args[1])) {
            throw new IllegalArgumentException(
                    "usage: DatabaseSchemaMigrationMain <server.properties> "
                            + "--apply-lito-extensions");
        }
        ServerConfig config = ServerConfig.load(Path.of(args[0]));
        if (!"lito_mysql".equals(config.databaseMode())) {
            throw new IllegalArgumentException("migration requires database.mode=lito_mysql");
        }
        try (AccountRepository repository = AccountRepository.open(config)) {
            repository.migrateLitoExtensions();
            repository.healthCheck();
        }
        System.out.println("[LiTo] SQL extension migration complete routes="
                + config.legacySourceDatabases().size());
    }
}
