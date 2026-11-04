package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Set;
import org.junit.jupiter.api.Test;

final class BattleDisconnectTest {
    @Test void oneDisconnectedSideLosesToTheRemainingSide() {
        BattleRules.Resolution result = GameHub.resolveBattleDisconnect(
                Set.of(1L, 3L), Set.of(3L));

        assertEquals(Set.of(1L), result.winners());
        assertEquals(Set.of(3L), result.losers());
        assertEquals("combatant-disconnected", result.reason());
    }

    @Test void bothDisconnectedSidesInvalidateTheMatch() {
        BattleRules.Resolution result = GameHub.resolveBattleDisconnect(
                Set.of(1L, 3L), Set.of(1L, 3L));

        assertEquals(Set.of(), result.winners());
        assertEquals(Set.of(), result.losers());
        assertEquals("all-combatants-disconnected-invalid", result.reason());
    }

    @Test void solePlayerDisconnectLosesToTheSyntheticArenaBot() {
        LocalResources.assumeNativeCatalogPins();
        BattleRules.Resolution result = GameHub.resolveBattleDisconnect(
                Set.of(1L), Set.of(1L));

        assertEquals(Set.of(2L), result.winners());
        assertEquals(Set.of(1L), result.losers());
    }
}
