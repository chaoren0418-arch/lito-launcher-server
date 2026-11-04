package local.lito381.server;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.ExecutionCondition;
import org.junit.jupiter.api.extension.ExtensionContext;

/**
 * Default {@code mvn test} stays portable: skip workstation-only classes unless
 * {@code -Dlito.workstation.tests=true}. Those classes need a live
 * {@code config/server.properties}, the local {@code 1-伺服器} tree, {@code LiTo.bin},
 * or Windows/{@code java.exe}.
 */
public final class LitoCiSafeTestGuard implements ExecutionCondition {
    private static final Set<String> SKIP = load();

    @Override
    public ConditionEvaluationResult evaluateExecutionCondition(ExtensionContext context) {
        if (Boolean.getBoolean("lito.workstation.tests")) {
            return ConditionEvaluationResult.enabled("lito.workstation.tests");
        }
        String name = context.getRequiredTestClass().getSimpleName();
        if (SKIP.contains(name)) {
            return ConditionEvaluationResult.disabled(
                    "workstation-only; set -Dlito.workstation.tests=true to run");
        }
        return ConditionEvaluationResult.enabled("portable");
    }

    private static Set<String> load() {
        InputStream in = LitoCiSafeTestGuard.class.getResourceAsStream(
                "/lito-ci-skip-classes.txt");
        if (in == null) {
            return Set.of();
        }
        Set<String> names = new LinkedHashSet<>();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(in, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }
                names.add(line);
            }
        } catch (Exception ignored) {
            return Set.of();
        }
        return Collections.unmodifiableSet(names);
    }
}
