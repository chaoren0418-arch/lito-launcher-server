package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

final class GameStateMarriageTest {
    @TempDir Path temporary;

    @Test
    void marriageIsReciprocalAtomicAndDurable() throws Exception {
        Path file = temporary.resolve("marriage.properties");
        GameStateStore first = new GameStateStore(file, true);
        assertEquals(GameStateStore.MarriageStatus.MARRIED,
                first.marry("Alice", "Bob").status());
        assertEquals("Bob", first.marriagePartner("Alice"));
        assertEquals("Alice", first.marriagePartner("Bob"));
        assertEquals(GameStateStore.MarriageStatus.FIRST_ALREADY_MARRIED,
                first.marry("Alice", "Carol").status());
        assertEquals(GameStateStore.MarriageStatus.SECOND_ALREADY_MARRIED,
                first.marry("Carol", "Bob").status());

        GameStateStore reloaded = new GameStateStore(file, true);
        assertEquals("Bob", reloaded.marriagePartner("Alice"));
        assertEquals("Alice", reloaded.marriagePartner("Bob"));
        assertFalse(reloaded.divorce("Alice", "Carol").divorced());
        GameStateStore.DivorceMutation divorced = reloaded.divorce("Alice", "Bob");
        assertTrue(divorced.divorced());
        assertEquals("Bob", divorced.partner());
        assertEquals("", reloaded.marriagePartner("Alice"));
        assertEquals("", reloaded.marriagePartner("Bob"));

        GameStateStore afterDivorce = new GameStateStore(file, true);
        assertEquals("", afterDivorce.marriagePartner("Alice"));
        assertEquals("", afterDivorce.marriagePartner("Bob"));
    }

    @Test
    void rejectsSelfMarriageAndStaleDivorceReplay() throws Exception {
        GameStateStore state = new GameStateStore(
                temporary.resolve("memory-only.properties"), false);
        assertEquals(GameStateStore.MarriageStatus.INVALID,
                state.marry("Alice", "alice").status());
        assertEquals(GameStateStore.MarriageStatus.MARRIED,
                state.marry("Alice", "Bob").status());
        assertTrue(state.divorce("Alice", "Bob").divorced());
        assertFalse(state.divorce("Alice", "Bob").divorced());
    }

    @Test
    void sqlMarriageReconciliationReplacesStaleSidecarAndIsDurable() throws Exception {
        Path file = temporary.resolve("sql-marriage.properties");
        String alice = GameStateStore.sourceCharacter("380 角色庫 1", "Alice");
        String bob = GameStateStore.sourceCharacter("380 角色庫 1", "Bob");
        String stale = GameStateStore.sourceCharacter("380 角色庫 1", "Stale");
        GameStateStore state = new GameStateStore(file, true);
        assertEquals(GameStateStore.MarriageStatus.MARRIED,
                state.marry(alice, stale).status());

        state.reconcileSqlMarriage(alice, bob);
        assertEquals("Bob", state.marriagePartner(alice));
        assertEquals("Alice", state.marriagePartner(bob));
        assertEquals("", state.marriagePartner(stale));

        GameStateStore reloaded = new GameStateStore(file, true);
        assertEquals("Bob", reloaded.marriagePartner(alice));
        assertEquals("Alice", reloaded.marriagePartner(bob));
        reloaded.reconcileSqlMarriage(alice, "");
        assertEquals("", reloaded.marriagePartner(alice));
        assertEquals("", reloaded.marriagePartner(bob));
        GameStateStore afterDivorce = new GameStateStore(file, true);
        assertEquals("", afterDivorce.marriagePartner(alice));
        assertEquals("", afterDivorce.marriagePartner(bob));
    }
}
