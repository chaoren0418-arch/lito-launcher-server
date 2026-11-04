package local.lito381.server;

import static org.junit.jupiter.api.Assertions.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.opentest4j.TestAbortedException;

final class LocalResourcesTest {
    @TempDir Path root;

    @Test void binOverrideCannotSubstituteForTheActualSelectedClient() throws Exception {
        Path alternate = Files.writeString(root.resolve("alternate.bin"), "fixture");
        String previous = System.getProperty("lito.bin");
        try {
            System.setProperty("lito.bin", alternate.toString());
            assertThrows(TestAbortedException.class,
                    () -> LocalResources.assumeLitoBin(root.resolve("selected.bin")));
            assertDoesNotThrow(() -> LocalResources.assumeLitoBin(alternate));
        } finally {
            if (previous == null) System.clearProperty("lito.bin");
            else System.setProperty("lito.bin", previous);
        }
    }

    @Test void absoluteIdaCandidateCannotSubstituteForMissingRelativeInput() throws Exception {
        Path alternate = Files.writeString(root.resolve("absolute.asm"), "fixture");
        assertThrows(TestAbortedException.class,
                () -> LocalResources.assumeIdaClient(root.resolve("relative.asm")));
        assertDoesNotThrow(() -> LocalResources.assumeIdaClient(alternate));
    }

    @Test void primarySqlDoesNotSubstituteForMissingSecondarySource() throws Exception {
        Path primary = Files.writeString(root.resolve("380_db.sql"), "fixture");
        assertThrows(TestAbortedException.class,
                () -> LocalResources.assumeLineageSources(primary, root.resolve("L1SkillUse.java")));
        Path secondary = Files.writeString(root.resolve("L1SkillUse.java"), "fixture");
        assertDoesNotThrow(() -> LocalResources.assumeLineageSources(primary, secondary));
    }

    @Test void dynamicExternalReadsAbortOnTheExactMissingFile() throws Exception {
        Path evidence = Files.createDirectory(root.resolve("LiTo381-開發"));
        Files.writeString(evidence.resolve("other.json"), "fixture");
        Path missing = evidence.resolve("required.json");
        TestAbortedException aborted = assertThrows(TestAbortedException.class,
                () -> LocalResources.readString(missing));
        assertTrue(aborted.getMessage().contains(missing.toAbsolutePath().toString()));
        assertThrows(TestAbortedException.class, () -> LocalResources.readAllBytes(missing));
        assertThrows(TestAbortedException.class, () -> LocalResources.newInputStream(missing));
        assertThrows(TestAbortedException.class, () -> LocalResources.newBufferedReader(missing));
        Files.writeString(missing, "actual bytes");
        assertEquals("actual bytes", LocalResources.readString(missing));
    }

    @Test void missingRepositoryOrGeneratedFilesRemainErrors() {
        assertThrows(IOException.class, () -> LocalResources.readString(root.resolve("missing.java")));
    }

    @Test void aDirectoryCannotStandInForAFile() {
        assertThrows(TestAbortedException.class, () -> LocalResources.assumeFiles(root));
    }

    @Test void sharedRootPropertyCannotSubstituteForAbsentEnvironment() {
        String previous = System.getProperty("lito.shared.resource.root");
        try {
            System.setProperty("lito.shared.resource.root", root.toString());
            assertThrows(TestAbortedException.class, () -> LocalResources.assumeSharedResourceRoot(null));
        } finally {
            if (previous == null) System.clearProperty("lito.shared.resource.root");
            else System.setProperty("lito.shared.resource.root", previous);
        }
    }

    @Test void sharedRootRequiresTheExactMapSector() throws Exception {
        Path map = Files.createDirectories(root.resolve("map/514"));
        Files.writeString(map.resolve("other.s32"), "fixture");
        assertThrows(TestAbortedException.class, () -> LocalResources.assumeSharedResourceRoot(root.toString()));
        Files.writeString(map.resolve("7fff8000.s32"), "fixture");
        assertEquals(root, LocalResources.assumeSharedResourceRoot(root.toString()));
    }
}
