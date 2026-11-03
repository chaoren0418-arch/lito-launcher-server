package local.lito381.server;

import java.nio.file.Path;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Properties;
import java.sql.DriverManager;

/** Read-only baseline reproduction: no repository, JDBC, network, or state file writes. */
public final class NativeRankingIdentityDiagnosticMain {
    public static void main(String[] args) throws Exception {
        Path code = Path.of(GameStateStore.class.getProtectionDomain().getCodeSource().getLocation().toURI());
        String sha = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(code)));
        if (!sha.equalsIgnoreCase("3025E3DF1C28F55019E79D5F21F9C1587B7268FB6417D6A75A591B6AECACB4B4"))
            throw new IllegalStateException("diagnostic requires unchanged formal baseline JAR");
        GameStateStore state = new GameStateStore(Path.of(args[0]).resolve("not-created.state"), false);
        String source = "月亮神";
        String selected = GameStateStore.sourceCharacter(source, "GuiKnight");
        state.bindRankingIdentity(selected, source, 0, 80, 1);
        var active = state.tournamentProfileState(selected);
        var uninitialized = state.tournamentProfileState(GameStateStore.sourceCharacter(source, "RosterOnly"));
        if (!active.classicServerName().equals(source) || !uninitialized.classicServerName().isEmpty())
            throw new IllegalStateException("baseline behavior differs from diagnosis");
        if (Files.exists(Path.of(args[0]).resolve("not-created.state")))
            throw new IllegalStateException("read-only replay wrote a state file");
        int missing = -1;
        if (args.length == 2) {
            Properties jdbc = new Properties();
            try (var input = Files.newInputStream(Path.of(args[1]))) { jdbc.load(input); }
            String original = jdbc.getProperty("URL1_LOGIN");
            if (original == null || !original.startsWith("jdbc:mysql://127.0.0.1:55904/"))
                throw new IllegalStateException("only the owned current fixture SQL endpoint allowed");
            String url = original.replaceFirst("/[^/?]+(?=\\?|$)", "/380_ac_2");
            Class.forName(jdbc.getProperty("Driver_LOGIN"));
            try (var connection = DriverManager.getConnection(url, jdbc.getProperty("Login_LOGIN"), jdbc.getProperty("Password_LOGIN"))) {
                connection.setReadOnly(true);
                connection.setCatalog("380_ac_2");
                try (var query = connection.prepareStatement("SELECT COUNT(*) FROM characters c LEFT JOIN lito_static.lito_tournament_character_record t ON t.source_server_id=2 AND t.char_obj_id=c.objid AND t.char_name=c.char_name WHERE c.Banned=0 AND c.char_name<>? AND (t.snapshot_version IS NULL OR t.snapshot_version=0)")) {
                    query.setString(1, "GuiKnight");
                    try (var rows = query.executeQuery()) { rows.next(); missing = rows.getInt(1); }
                }
            }
        }
        System.out.println("{\"baseline_sha256\":\"" + sha + "\",\"selected_source_bound\":true,"
                + "\"roster_only_source_empty\":true,\"repository_source_validation_would_reject\":true,"
                + "\"sql_select_only\":" + (args.length == 2) + ",\"live_other_roster_without_snapshot\":" + missing
                + ",\"state_file_written\":false}");
    }
}
