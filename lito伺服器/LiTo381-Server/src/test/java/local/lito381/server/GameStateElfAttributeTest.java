package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

final class GameStateElfAttributeTest {
    @TempDir Path temporary;

    @Test
    void unchangedSourceKeepsResetAndChangedSourceRebasesIt() throws Exception {
        Path file = temporary.resolve("elf-attribute.properties");
        GameStateStore state = new GameStateStore(file, true);
        assertEquals(2, state.reconcileElfAttribute(
                "ServerA::Elf", 2).effective());
        state.saveElfAttributeState(
                "ServerA::Elf", 0, Set.of(2), Set.of(157, 175));

        GameStateStore reloaded = new GameStateStore(file, true);
        GameStateStore.ElfAttributeState unchanged =
                reloaded.reconcileElfAttribute("ServerA::Elf", 2);
        assertEquals(2, unchanged.observedSource());
        assertEquals(0, unchanged.effective());
        assertEquals(Set.of(157, 175), unchanged.forgottenSkills());
        assertEquals(Set.of(2), reloaded.purchasedSkills("ServerA::Elf"));

        GameStateStore.ElfAttributeState rebased =
                reloaded.reconcileElfAttribute("ServerA::Elf", 4);
        assertEquals(4, rebased.observedSource());
        assertEquals(4, rebased.effective());
        assertTrue(rebased.forgottenSkills().isEmpty());
        String durable = Files.readString(file);
        assertTrue(durable.contains("format.version=43"));
        assertTrue(durable.contains("elfAttribute.present=true"));
    }

    @Test
    void rejectsOverlappingLearnedAndForgottenOverlays() throws Exception {
        GameStateStore state = new GameStateStore(
                temporary.resolve("overlap.properties"), false);
        state.reconcileElfAttribute("ServerA::Elf", 1);
        assertThrows(IllegalArgumentException.class,
                () -> state.saveElfAttributeState(
                        "ServerA::Elf", 0, Set.of(157), Set.of(157)));
    }
}
