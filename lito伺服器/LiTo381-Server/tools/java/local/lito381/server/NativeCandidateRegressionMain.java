package local.lito381.server;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import org.junit.platform.engine.discovery.DiscoverySelectors;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;
import org.junit.platform.launcher.listeners.SummaryGeneratingListener;

/** Runs selected tests against an explicitly fingerprinted release artifact. */
public final class NativeCandidateRegressionMain {
    private static final List<String> SUITES = List.of(
            "LitoDatabaseLayoutTest", "DedicatedServerLayoutTest",
            "ExternalSourceServersConfigTest", "ServerConfigChannelPortsTest",
            "ClientSessionLoginPreambleTest", "ClientSessionLitoChannelSelectionIntegrationTest",
            "ClientSessionLitoCreateRoomClanIntegrationTest", "ClientSessionBattleResultStatisticsTest",
            "GameHubVirtualPlayerTest", "GameStateEphemeralSourceProfileTest",
            "ClientSessionClassicSourceHandoffIntegrationTest", "GameHubClassicSourceHandoffTest",
            "GameHubChannelTest", "TournamentPresenceTest", "NativeLitoRentalHotelCatalogTest",
            "RentalTownHotelRegistryTest", "ClientSessionNativeRentalTownCitizenshipTest",
            "ClientSessionRentalBattleHandoffIntegrationTest", "ClientSessionLitoBuddyPresenceIntegrationTest");

    public static void main(String[] args) throws Exception {
        if (args.length != 3) throw new IllegalArgumentException("jar, expected SHA256, evidence path required");
        Path artifact = Path.of(args[0]).toRealPath();
        String sha = HexFormat.of().withUpperCase().formatHex(
                MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(artifact)));
        if (!sha.equals(args[1])) throw new IllegalStateException("Candidate fingerprint changed");
        for (Class<?> type : List.of(Main.class, AccountRepository.class,
                ClientSession.class, GameHub.class, BattleRules.class)) {
            Path loaded = Path.of(type.getProtectionDomain().getCodeSource().getLocation().toURI()).toRealPath();
            if (!artifact.equals(loaded)) throw new IllegalStateException("Wrong artifact for " + type.getSimpleName());
        }
        var request = LauncherDiscoveryRequestBuilder.request();
        for (String suite : SUITES) request.selectors(DiscoverySelectors.selectClass(
                Class.forName("local.lito381.server." + suite)));
        var listener = new SummaryGeneratingListener();
        var launcher = LauncherFactory.create();
        launcher.registerTestExecutionListeners(listener);
        launcher.execute(request.build());
        var summary = listener.getSummary();
        var failures = new ArrayList<String>();
        for (var failure : summary.getFailures()) {
            // Method/class and exception type are sufficient; avoid emitting SQL credentials.
            failures.add("{\"test\":" + quote(failure.getTestIdentifier().getUniqueId())
                    + ",\"type\":" + quote(failure.getException().getClass().getName()) + "}");
        }
        String evidence = "{\n\"jar\":" + quote(artifact.toString())
                + ",\n\"sha256\":" + quote(sha)
                + ",\n\"artifact_bound_classes\":[\"Main\",\"AccountRepository\",\"ClientSession\",\"GameHub\",\"BattleRules\"]"
                + ",\n\"suite_count\":" + SUITES.size()
                + ",\n\"tests_found\":" + summary.getTestsFoundCount()
                + ",\n\"tests_succeeded\":" + summary.getTestsSucceededCount()
                + ",\n\"tests_failed\":" + summary.getTestsFailedCount()
                + ",\n\"tests_skipped\":" + summary.getTestsSkippedCount()
                + ",\n\"containers_failed\":" + summary.getContainersFailedCount()
                + ",\n\"failures\":[" + String.join(",", failures) + "]\n}";
        Files.writeString(Path.of(args[2]), evidence, StandardCharsets.UTF_8);
        System.out.println("[candidate-regression] " + summary.getTestsSucceededCount()
                + "/" + summary.getTestsFoundCount() + " passed; failures=" + failures.size());
        if (!failures.isEmpty() || summary.getTestsFoundCount() == 0
                || summary.getTestsSkippedCount() > 0) System.exit(1);
    }

    private static String quote(String value) {
        return "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"")
                .replace("\n", "\\n").replace("\r", "\\r") + "\"";
    }
}
