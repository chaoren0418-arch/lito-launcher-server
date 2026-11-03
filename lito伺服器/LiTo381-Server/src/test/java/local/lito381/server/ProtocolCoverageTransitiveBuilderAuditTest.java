package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

final class ProtocolCoverageTransitiveBuilderAuditTest {
    @Test
    void generatorTraversesNestedProtocolBuilders() throws Exception {
        String generator = Files.readString(
                Path.of("tools", "protocol_coverage.py"), StandardCharsets.UTF_8);
        assertTrue(generator.contains("pending_protocol_methods"));
        assertTrue(generator.contains("nested in protocol_methods"));
    }

    @Test
    void generatedCoverageRecognizesProductionBuyListEmitter() throws Exception {
        String coverage = Files.readString(
                Path.of("SERVER-PROTOCOL-COVERAGE.json"), StandardCharsets.UTF_8);
        assertTrue(Pattern.compile(
                "\\\"name\\\"\\s*:\\s*\\\"S_BUY_LIST\\\".*?"
                        + "\\\"status\\\"\\s*:\\s*\\\"emitted\\\"",
                Pattern.DOTALL).matcher(coverage).find());
        assertTrue(coverage.contains(
                "\"unresolvedNativeReceivableServerPackets\": 0"));
    }
}
