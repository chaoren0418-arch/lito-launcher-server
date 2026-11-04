package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.opentest4j.TestAbortedException;

/** Exact-path assume contract: an unused twin candidate must not satisfy the gate. */
final class LocalResourcesTest {
    @Test
    void unusedTwinCandidateDoesNotSatisfyExactPathAssume() throws Exception {
        Path unused = Files.createTempFile("lito-unused-ida-", ".asm");
        unused.toFile().deleteOnExit();
        Path used = unused.resolveSibling("this-test-does-not-open-" + unused.getFileName());
        assertTrue(LocalResources.present(unused));
        assertFalse(LocalResources.present(used));
        assertThrows(TestAbortedException.class, () -> LocalResources.assumeIdaClient(used));
        if (!LocalResources.present(LocalResources.LITO_BIN)) {
            assertThrows(TestAbortedException.class, LocalResources::assumeLitoBin);
        }
    }

    @Test
    void sharedResourceRootIgnoresSystemProperty() {
        String previous = System.getProperty("lito.shared.resource.root");
        System.setProperty("lito.shared.resource.root", "/tmp");
        try {
            String env = System.getenv("LITO_SHARED_RESOURCE_ROOT");
            if (env == null || env.isBlank()) {
                assertThrows(TestAbortedException.class, LocalResources::sharedResourceRoot);
            }
        } finally {
            if (previous == null) {
                System.clearProperty("lito.shared.resource.root");
            } else {
                System.setProperty("lito.shared.resource.root", previous);
            }
        }
    }
}
