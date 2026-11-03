package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

final class LegacyMySqlReadOnlyTournamentTest {
    @Test
    void readOnlyTournamentMutationShortCircuitsBeforeValidationOrSql(
            @TempDir Path temporary) throws Exception {
        Path properties = temporary.resolve("database.properties");
        Files.writeString(properties, """
                URL1_LOGIN=jdbc:mysql://127.0.0.1:1/
                URL2_LOGIN=unused
                URL3_LOGIN=?connectTimeout=1
                Login=unused
                Password=unused
                Login_LOGIN=unused
                Password_LOGIN=unused
                Driver_LOGIN=com.mysql.cj.jdbc.Driver
                """, StandardCharsets.UTF_8);
        GameStateStore state = new GameStateStore(
                temporary.resolve("state.properties"), false);
        GameStateStore.TournamentProfileState otherSourceSnapshot =
                state.tournamentProfileState(GameStateStore.sourceCharacter(
                        "380 角色庫 2", "ReadOnlyRole"));

        try (AccountRepository repository = new LegacyMySqlRepository(
                properties, "380 角色庫 1", "380_ac_1", "lito_static",
                100.0, true, "lito_mysql")) {
            RepositoryTournamentMutation mutation = repository.persistTournamentProfile(
                    123, "ReadOnlyRole", null, otherSourceSnapshot);
            assertEquals(RepositoryTournamentMutationStatus.READ_ONLY, mutation.status());
        }
    }
}
