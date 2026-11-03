package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

final class GameStateDeathLostSkillTest {
    @TempDir Path temporary;

    @Test void deathLostSkillMaskPersistsSeparatelyFromElfAttributeForgetting()
            throws Exception {
        Path file = temporary.resolve("death-lost-skills.properties");
        GameStateStore state = new GameStateStore(file, true);
        state.reconcileElfAttribute("ServerA::Elf", 4);
        state.saveDeathLostSkills("ServerA::Elf", Set.of(1, 55, 131));

        GameStateStore reloaded = new GameStateStore(file, true);
        assertEquals(Set.of(1, 55, 131),
                reloaded.deathLostSkills("ServerA::Elf"));
        assertTrue(reloaded.elfAttribute("ServerA::Elf").forgottenSkills().isEmpty());
        String durable = Files.readString(file);
        assertTrue(durable.contains("format.version=43"));
        assertTrue(durable.contains("death.lostSkills=1,55,131"));
    }
}
