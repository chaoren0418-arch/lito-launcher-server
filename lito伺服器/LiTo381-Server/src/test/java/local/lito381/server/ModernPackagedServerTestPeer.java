package local.lito381.server;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;
import java.util.concurrent.*;
import java.util.regex.Pattern;

/** Isolated fixture owner only. Child JVM runs -jar without any test classpath. */
final class ModernPackagedServerTestPeer {
    private ModernPackagedServerTestPeer() {}
    private static final String END = "PACKAGED_STREAM_END";

    static void run(ModernQuestSqlFixture fixture, int millis) throws Exception {
        Path jar = Path.of(Objects.requireNonNull(System.getenv("LITO_MODERN_TEST_SERVER_JAR")));
        String expected = Objects.requireNonNull(System.getenv("LITO_MODERN_TEST_SERVER_JAR_SHA256"));
        require(jar.isAbsolute() && Files.isRegularFile(jar, LinkOption.NOFOLLOW_LINKS), "explicit packaged JAR");
        String hash;
        try (var stream = Files.newInputStream(jar)) {
            var digest = MessageDigest.getInstance("SHA-256"); byte[] buffer = new byte[65536]; int n;
            while ((n = stream.read(buffer)) != -1) digest.update(buffer, 0, n);
            hash = HexFormat.of().withUpperCase().formatHex(digest.digest());
        }
        require(hash.equals(expected), "packaged JAR hash");
        var builder = new ProcessBuilder(Path.of(System.getProperty("java.home"), "bin", "java.exe").toString(),
                "-Dfile.encoding=UTF-8", "-jar", jar.toString(), "--modern-config",
                fixture.directory.resolve("packaged-modern.properties").toString(), "--console");
        builder.directory(fixture.directory.toFile());
        builder.redirectError(fixture.directory.resolve("packaged-stderr.log").toFile());
        var env = builder.environment();
        // JVM launch variables could otherwise inject an agent/classpath into the packaged process.
        for (String name : List.of("JAVA_TOOL_OPTIONS", "JDK_JAVA_OPTIONS", "_JAVA_OPTIONS", "CLASSPATH")) env.remove(name);
        secret(env, "T", fixture.config.tls().password());
        var source = fixture.config.sources().get(0).database();
        env.put("U", source.username()); secret(env, "P", source.password());
        env.put("WU", fixture.config.settlement().database().username());
        secret(env, "WP", fixture.config.settlement().database().password());
        env.put("R_U", fixture.config.raceSettlement().database().username());
        secret(env, "R_P", fixture.config.raceSettlement().database().password());
        Process child = builder.start();
        env.clear(); // ProcessBuilder no longer needs these credential strings.
        var lines = new ArrayBlockingQueue<String>(8);
        Thread reader = new Thread(() -> {
            try (var input = new BufferedReader(new InputStreamReader(child.getInputStream(), StandardCharsets.UTF_8));
                    var log = Files.newBufferedWriter(fixture.directory.resolve("packaged-stdout.log"), StandardCharsets.UTF_8, StandardOpenOption.CREATE_NEW)) {
                for (;;) {
                    var line = new StringBuilder(); int c;
                    while ((c = input.read()) != -1 && c != '\n') { if (line.length() >= 512) throw new IOException("oversized packaged output"); line.append((char)c); }
                    if (c == -1) break;
                    String value = line.toString().stripTrailing(); log.write(value); log.newLine(); log.flush(); lines.put(value);
                }
            } catch (Exception failure) { /* END is rejected unless normal retirement was observed. */ }
            finally { lines.offer(END); }
        }, "packaged-server-output");
        reader.setDaemon(true); reader.start();
        boolean normal = false;
        try (var commands = new BufferedWriter(new OutputStreamWriter(child.getOutputStream(), StandardCharsets.US_ASCII))) {
            long deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(millis);
            var ready = Pattern.compile("\\[Modern LiTo\\] authentication/roster listener ready at /127\\.0\\.0\\.1:(\\d{1,5})")
                    .matcher(next(lines, Math.min(deadline, System.nanoTime()+TimeUnit.SECONDS.toNanos(30))));
            require(ready.matches(), "dedicated JAR READY");
            int port = Integer.parseInt(ready.group(1)); require(port > 0 && port <= 65535, "dedicated JAR port");
            System.err.println("PACKAGED_SERVER_READY pid="+child.pid()+" sha256="+hash+" testClasspath=false");
            System.out.println("READY " + port); System.out.flush();
            var status = Pattern.compile("MODERN_STATUS accepted=(\\d+) sessions=(\\d+) failed=0 listenerFailed=false");
            for (;;) {
                require(child.isAlive(), "dedicated JAR stopped early");
                commands.write("status\n"); commands.flush();
                var state = status.matcher(next(lines, deadline)); require(state.matches(), "dedicated JAR health");
                int accepted = Integer.parseInt(state.group(1)), sessions = Integer.parseInt(state.group(2));
                require(accepted <= 2 && sessions <= 2, "dedicated JAR session count");
                if (accepted == 2 && sessions == 0) break;
                Thread.sleep(50);
            }
            commands.write("stop\n"); commands.flush();
            require(next(lines, deadline).equals("MODERN_STOPPED accepted=2 sessions=0 failed=0 listenerFailed=false"), "dedicated JAR graceful retirement");
            require(child.waitFor(5000, TimeUnit.MILLISECONDS) && child.exitValue() == 0, "dedicated JAR exit");
            reader.join(1000); require(!reader.isAlive(), "dedicated JAR pipe retirement");
            normal = true;
            System.err.println("PACKAGED_SERVER_STOPPED accepted=2 sessions=0 failed=0 exit=0 graceful=true");
        } finally {
            if (child.isAlive()) { child.destroy(); if (!child.waitFor(2000, TimeUnit.MILLISECONDS)) { child.destroyForcibly(); child.waitFor(2000, TimeUnit.MILLISECONDS); } }
            reader.interrupt(); reader.join(1000);
            if (!normal) System.err.println("PACKAGED_SERVER_FAILED evidence retained in owned SQL fixture");
        }
    }

    private static String next(BlockingQueue<String> lines, long deadline) throws Exception {
        long remaining = deadline-System.nanoTime(); require(remaining>0, "dedicated JAR deadline");
        String value = lines.poll(remaining, TimeUnit.NANOSECONDS);
        require(value != null && !value.equals(END), "dedicated JAR output"); return value;
    }
    private static void secret(Map<String,String> environment, String name, char[] value) {
        try { environment.put(name, new String(value)); } finally { Arrays.fill(value, '\0'); }
    }
    private static void require(boolean value, String stage) { if (!value) throw new IllegalStateException(stage); }
}
