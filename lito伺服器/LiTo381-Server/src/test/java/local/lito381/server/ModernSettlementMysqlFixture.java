package local.lito381.server;

import com.mysql.cj.conf.PropertySet;
import com.mysql.cj.jdbc.JdbcConnection;
import com.mysql.cj.protocol.StandardSocketFactory;
import com.sun.nio.file.ExtendedOpenOption;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.OpenOption;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.FileVisitResult;
import java.nio.file.SimpleFileVisitor;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Opt-in, test-only, fresh MySQL 5.5 fixture. No native config, service control,
 * source data directory, external URL, source schema, or production credentials.
 * All JDBC bytes/results are handled by the real Connector/J and server. The
 * Connection proxy only owns raw-socket deadlines/retirement, not SQL semantics.
 */
public final class ModernSettlementMysqlFixture implements AutoCloseable {
    static final String ENV = "LITO_MODERN_MYSQL_HOME";
    static final String DATABASE = "lito_settlement_fixture";
    private static final int PROCESS_SECONDS = 30, CLOSE_SECONDS = 8;
    private static final long LOG_LIMIT = 1024 * 1024;
    private static final ThreadLocal<Attempt> OPENING = new ThreadLocal<>();
    private final Path home, parent, directory, data;
    private final Object directoryKey;
    private final DirectoryOwner owner;
    private final char[] password;
    private final String passwordHash;
    private final Set<Attempt> attempts = ConcurrentHashMap.newKeySet();
    private final Semaphore connectionSlots = new Semaphore(8);
    private final ScheduledThreadPoolExecutor deadlines = new ScheduledThreadPoolExecutor(1);
    private final Object lifecycle = new Object();
    private volatile Process process;
    private volatile int port;
    private int starts;
    private boolean datadirRecorded;
    private final long createdNanos = System.nanoTime();
    private boolean closed, closing, successful, forced;

    private ModernSettlementMysqlFixture(Path installation, Path parent) throws Exception {
        this.home = installation;
        this.parent = parent;
        this.directory = Files.createTempDirectory(parent, "mysql-5.5-").toRealPath();
        this.directoryKey = Files.readAttributes(directory, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS).fileKey();
        this.owner = new DirectoryOwner(directory);
        try {
        this.data = Files.createDirectory(directory.resolve("data"));
        Files.createDirectory(directory.resolve("tmp"));
        byte[] random = new byte[24];new SecureRandom().nextBytes(random);
        this.password = HexFormat.of().formatHex(random).toCharArray();Arrays.fill(random, (byte)0);
        byte[] encoded = new String(password).getBytes(StandardCharsets.US_ASCII);
        byte[] first = MessageDigest.getInstance("SHA-1").digest(encoded);
        byte[] second = MessageDigest.getInstance("SHA-1").digest(first);
        this.passwordHash = "*" + HexFormat.of().withUpperCase().formatHex(second);
        Arrays.fill(encoded, (byte)0);Arrays.fill(first, (byte)0);Arrays.fill(second, (byte)0);
        deadlines.setRemoveOnCancelPolicy(true);
        deadlines.setExecuteExistingDelayedTasksAfterShutdownPolicy(false);
        } catch (Exception | Error failure) { owner.close(); throw failure; }
    }

    static ModernSettlementMysqlFixture open(Path installation) throws Exception {
        Path home = checkedHome(installation);
        String configuredTemporary=System.getProperty("java.io.tmpdir");
        if(configuredTemporary==null||configuredTemporary.isBlank())throw new IOException("mysql fixture: TEMPORARY_ROOT");
        // MySQL 5.5 returned a non-equal datadir for the previous Unicode
        // workspace path. Use a new ASCII namespace, not a reinterpretation of
        // the returned path or reuse/move of either previous data directory.
        Path temporary=checkedTemporaryRoot(Path.of(configuredTemporary));
        Path parent = temporary.resolve("lito-modern-mysql-fixtures");
        checkAncestors(parent);
        Files.createDirectories(parent);checkAncestors(parent);parent = parent.toRealPath();
        ModernSettlementMysqlFixture fixture = new ModernSettlementMysqlFixture(home, parent);
        try {
            fixture.bootstrap();fixture.recordLifecycle("bootstrap-complete", -1);ModernSlowSqlTestPeer.boot("mysql-bootstrap");
            fixture.start();fixture.recordLifecycle("mysql-ready", fixture.processId());ModernSlowSqlTestPeer.boot("mysql-start");
            fixture.installSchema();fixture.recordLifecycle("ddl-installed", fixture.processId());ModernSlowSqlTestPeer.boot("mysql-system-schema");return fixture;
        } catch (Throwable failure) {
            try {fixture.close();} catch (Exception cleanup) { /* Own artifact retained; no raw exception is printed. */ }
            throw fixture.failure(failure instanceof FixtureFailure known ? known.code : "INITIALIZATION");
        }
    }

    static Path checkedTemporaryRoot(Path path) throws IOException {
        if(!path.isAbsolute()||!path.equals(path.normalize())||!asciiPath(path))throw new IOException("mysql fixture: TEMPORARY_ROOT");
        checkAncestors(path);
        Path real=path.toRealPath();
        if(!asciiPath(real)||!Files.isDirectory(real,LinkOption.NOFOLLOW_LINKS))throw new IOException("mysql fixture: TEMPORARY_ROOT");
        return real;
    }
    private static boolean asciiPath(Path path) {
        String text=path.toString();
        for(int i=0;i<text.length();i++)if(text.charAt(i)<32||text.charAt(i)>126)return false;
        return true;
    }

    static Path checkedHome(Path path) throws IOException {
        if (!path.isAbsolute() || !path.equals(path.normalize())) throw new IOException("mysql fixture: INSTALLATION_PATH");
        checkAncestors(path);
        Path home = path.toRealPath();
        for (String member : List.of("bin/mysqld.exe", "share/mysql_system_tables.sql", "share/mysql_system_tables_data.sql")) {
            Path file = home.resolve(member);checkAncestors(file);
            if (!Files.isRegularFile(file, LinkOption.NOFOLLOW_LINKS)) throw new IOException("mysql fixture: INSTALLATION_MEMBER");
        }
        return home;
    }

    private static void checkAncestors(Path path) throws IOException {
        for (Path p = path; p != null; p = p.getParent()) {
            if (Files.isSymbolicLink(p) || Files.exists(p, LinkOption.NOFOLLOW_LINKS)
                    && (Files.readAttributes(p, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS).isOther()
                    || !p.toRealPath().equals(p.toAbsolutePath().normalize())))
                throw new IOException("mysql fixture: UNSAFE_PATH");
        }
    }

    private static String readBounded(Path file, int cap) throws IOException {
        checkAncestors(file);
        try (var input = Files.newInputStream(file, LinkOption.NOFOLLOW_LINKS)) {
            byte[] bytes = input.readNBytes(cap + 1);
            if (bytes.length == 0 || bytes.length > cap) throw new IOException("mysql fixture: INPUT_BOUND");
            return StandardCharsets.UTF_8.newDecoder().decode(java.nio.ByteBuffer.wrap(bytes)).toString();
        }
    }

    /** Arguments contain no password and never inherit a system my.ini. */
    private List<String> arguments(boolean bootstrap) {
        var args = new ArrayList<String>();
        args.add(home.resolve("bin/mysqld.exe").toString());args.add("--no-defaults");
        args.add("--basedir=" + home);args.add("--datadir=" + data);
        args.add("--tmpdir=" + directory.resolve("tmp"));args.add("--pid-file=" + directory.resolve("owned.pid"));
        args.add("--log-error=" + directory.resolve("server-error.log"));
        args.add("--innodb-data-home-dir=" + data);args.add("--innodb-log-group-home-dir=" + data);
        args.add("--innodb-data-file-path=ibdata1:10M:autoextend");args.add("--innodb-buffer-pool-size=16M");
        args.add("--innodb-additional-mem-pool-size=2M");args.add("--innodb-log-file-size=5M");
        args.add("--innodb-log-buffer-size=1M");args.add("--innodb-flush-log-at-trx-commit=1");
        args.add("--innodb-lock-wait-timeout=5");args.add("--default-storage-engine=InnoDB");
        args.add("--bind-address=127.0.0.1");args.add("--port=" + port);
        args.add("--skip-name-resolve");args.add("--skip-named-pipe");args.add("--skip-shared-memory");
        args.add("--skip-log-bin");args.add("--general-log=OFF");args.add("--slow-query-log=OFF");
        args.add("--max-connections=12");args.add("--max-allowed-packet=1M");
        if (bootstrap) {args.add("--bootstrap");args.add("--skip-networking");}
        else args.add("--console");
        return List.copyOf(args);
    }

    private Process spawn(boolean bootstrap) throws IOException {
        if (process != null && process.isAlive()) throw failure("ALREADY_RUNNING");
        validateOwned();
        for(String member:List.of("data","tmp","owned.pid","server-error.log","bootstrap-out.log","bootstrap-err.log","server-out.log","server-err.log"))
            checkAncestors(directory.resolve(member));
        ProcessBuilder builder = new ProcessBuilder(arguments(bootstrap)).directory(directory.toFile());
        // An explicit minimal environment cannot copy source/test credentials
        // or load MYSQL_HOME/my.ini from an unrelated deployment.
        builder.environment().clear();
        String systemRoot=System.getenv("SystemRoot");
        if(systemRoot==null||systemRoot.isBlank())throw failure("WINDOWS_ENVIRONMENT");
        builder.environment().put("SystemRoot",systemRoot);builder.environment().put("WINDIR",systemRoot);
        builder.environment().put("PATH",Path.of(systemRoot,"System32").toString());
        builder.environment().put("TEMP",directory.resolve("tmp").toString());builder.environment().put("TMP",directory.resolve("tmp").toString());
        builder.redirectOutput(directory.resolve(bootstrap ? "bootstrap-out.log" : "server-out.log").toFile());
        builder.redirectError(directory.resolve(bootstrap ? "bootstrap-err.log" : "server-err.log").toFile());
        // ProcessBuilder returns a newly created process, never an existing
        // service. Retain its exact handle immediately; numeric PIDs may be
        // reused and are not an ownership or cleanup authority.
        process = builder.start();
        recordLifecycle(bootstrap ? "bootstrap-start" : "server-start", process.pid());
        return process;
    }

    private void bootstrap() throws Exception {
        port = freePort();
        String tables = readBounded(home.resolve("share/mysql_system_tables.sql"), 65536);
        String initial = readBounded(home.resolve("share/mysql_system_tables_data.sql"), 16384);
        // Only fresh, offline system tables. Never copy the installed data/mysql.
        String sql = "CREATE DATABASE mysql;\nUSE mysql;\n" + tables + "\n" + initial
                + "\nDELETE FROM mysql.user WHERE User<>'root' OR Host NOT IN ('127.0.0.1','localhost');\n"
                + "UPDATE mysql.user SET Password='" + passwordHash + "' WHERE User='root';\n";
        byte[] bytes = sql.getBytes(StandardCharsets.UTF_8);
        Process owned = spawn(true);var writer = Executors.newSingleThreadExecutor();
        var writing = writer.submit(() -> {try (var stream = owned.getOutputStream()) {stream.write(bytes);}return true;});
        boolean retired = false;
        try {
            if (!awaitExit(owned, deadline(PROCESS_SECONDS)) || owned.exitValue() != 0) throw failure("BOOTSTRAP");
            writing.get(2, TimeUnit.SECONDS);retired = true;
        } finally {
            if (owned.isAlive()) terminate(owned);
            writing.cancel(true);writer.shutdownNow();
            boolean joined = awaitExecutor(writer,5);
            Arrays.fill(bytes, (byte)0);
            if (!joined) throw failure("BOOTSTRAP_WRITER_RETIREMENT");
            if (retired) process = null;
        }
    }

    private void start() throws Exception {
        if (!attempts.isEmpty()) throw failure("CONNECTIONS_STILL_OWNED");
        port = freePort();datadirRecorded = false;Process owned = spawn(false);owned.getOutputStream().close();starts++;
        long until = deadline(PROCESS_SECONDS);
        String last="NO_ATTEMPT";
        while (until - System.nanoTime() > 0) {
            checkLogs();
            if (!owned.isAlive()) throw failure("SERVER_EXITED");
            try (Connection ignored = openConnection(Math.min(until, deadline(2)))) {return;}
            catch (SQLException unavailable) {last=unavailable instanceof ConnectionFailure known?known.diagnostic:"UNKNOWN";Thread.sleep(50);}
        }
        throw failure("START_DEADLINE; last="+last);
    }

    static int freePort() throws IOException {
        try (ServerSocket socket = new ServerSocket(0, 1, InetAddress.getByAddress(new byte[]{127,0,0,1}))) {
            int port = socket.getLocalPort();if (port == 3306 || port < 1024 || port > 65535) throw new IOException("mysql fixture: UNSAFE_PORT");
            return port;
        }
    }
    static long deadline(int seconds) {return System.nanoTime() + TimeUnit.SECONDS.toNanos(seconds);}
    int port() {return port;}
    int starts() {return starts;}
    boolean forcedTermination() {return forced;}
    int activeConnections() {return attempts.size();}
    Path artifactDirectory() {return directory;}
    Path dataDirectory() {return data;}
    long processId() {return process == null ? -1 : process.pid();}
    /** Native acceptance settings are confined to this owned fixture; never printed. */
    Path writeNativeJdbcSettings() throws Exception {
        validateOwned();
        try (Connection ignored = openConnection(deadline(3))) { /* Verify exact physical datadir first. */ }
        Path file = directory.resolve("native-database.properties");checkAncestors(file);
        Properties settings = new Properties();
        settings.setProperty("URL1_LOGIN", "jdbc:mysql://127.0.0.1:" + port + "/");
        settings.setProperty("URL3_LOGIN", "?useSSL=false&characterEncoding=UTF-8&connectTimeout=2000&socketTimeout=5000");
        settings.setProperty("Login_LOGIN", "root");settings.setProperty("Password_LOGIN", new String(password));
        settings.setProperty("Login", "root");settings.setProperty("Password", new String(password));
        settings.setProperty("Driver_LOGIN", "com.mysql.cj.jdbc.Driver");
        try (var writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8,
                StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE, LinkOption.NOFOLLOW_LINKS)) {
            settings.store(writer, "Owned isolated fixture only; credentials are not evidence output.");
        } finally {settings.clear();}
        return file;
    }
    private void recordLifecycle(String event, long pid) throws IOException {
        Path evidence = parent.resolve("lifecycle.log");checkAncestors(evidence);
        String line = "event=" + event + " pid=" + pid + " port=" + port
                + " elapsed_ms=" + TimeUnit.NANOSECONDS.toMillis(System.nanoTime()-createdNanos)
                + " utc=" + java.time.Instant.now()
                + " datadir=" + data.toAbsolutePath().normalize() + System.lineSeparator();
        synchronized (ModernSettlementMysqlFixture.class) {
            Files.writeString(evidence, line, StandardCharsets.UTF_8, StandardOpenOption.CREATE,
                    StandardOpenOption.APPEND, LinkOption.NOFOLLOW_LINKS);
        }
    }
    void successful() {successful = true;}
    ModernMatchSettlementJdbc store() {return new ModernMatchSettlementJdbc(this::openConnection, DATABASE);}

    Connection openConnection(long deadline) throws SQLException {
        return openConnection(deadline,false);
    }
    private Connection openConnection(long deadline,boolean shutdown) throws SQLException {
        Attempt attempt;
        synchronized(lifecycle) {
            if (closed || closing&&!shutdown || process == null || !process.isAlive() || OPENING.get()!=null || port == 3306 || port < 1024
                    || deadline - System.nanoTime() <= 0 || deadline - System.nanoTime() > TimeUnit.SECONDS.toNanos(30)
                    || !connectionSlots.tryAcquire()) throw connectionFailure(ConnectionStage.ADMISSION,null);
            attempt = new Attempt(deadline);attempts.add(attempt);
            try {attempt.expiration=deadlines.schedule(attempt::expire,Math.max(0,deadline-System.nanoTime()),TimeUnit.NANOSECONDS);}
            catch(RuntimeException failure){attempt.retire();throw connectionFailure(ConnectionStage.DEADLINE,failure);}
        }
        Properties props = new Properties();Connection connection = null;
        ConnectionStage stage=ConnectionStage.CONFIGURATION;
        try {
            OPENING.set(attempt);
            props.setProperty("user", "root");props.setProperty("password", new String(password));
            props.setProperty("socketFactory", FixtureSocketFactory.class.getName());
            props.setProperty("connectTimeout", "1000");props.setProperty("socketTimeout", "1000");
            for (String option : List.of("autoReconnect", "allowMultiQueries", "allowLoadLocalInfile", "allowUrlInLocalInfile",
                    "allowPublicKeyRetrieval", "autoDeserialize", "useCompression", "profileSQL", "traceProtocol", "enablePacketDebug")) props.setProperty(option,"false");
            props.setProperty("sslMode", "DISABLED");props.setProperty("characterEncoding", "UTF-8");
            props.setProperty("queryTimeoutKillsConnection", "true");props.setProperty("logger", "com.mysql.cj.log.NullLogger");
            stage=ConnectionStage.CONNECT;
            connection = new com.mysql.cj.jdbc.Driver().connect("jdbc:mysql://127.0.0.1:"+port+"/", props);
            if (connection == null) throw sqlFailure();
            stage=ConnectionStage.NETWORK;
            connection.setNetworkTimeout(Runnable::run, 1000);
            // Guard every physical connection before any writes. A port race
            // cannot silently route fixture SQL into another running server.
            stage=ConnectionStage.GUARD_QUERY;
            try (var statement = connection.prepareStatement("SELECT @@datadir AS fixture_data,@@port AS fixture_port,@@version AS fixture_version")) {
                statement.setQueryTimeout(1);statement.setMaxRows(2);
                try (var rows = statement.executeQuery()) {
                    stage=ConnectionStage.GUARD_ROWS;
                    if (!rows.next()) throw sqlFailure();
                    stage=ConnectionStage.PORT;
                    if (rows.getInt("fixture_port") != port || rows.wasNull()) throw sqlFailure();
                    stage=ConnectionStage.DATADIR;
                    String physicalData=rows.getString("fixture_data");
                    if (physicalData==null || !Path.of(physicalData).toRealPath().equals(data.toRealPath())) throw sqlFailure();
                    stage=ConnectionStage.VERSION;
                    String version=rows.getString("fixture_version");
                    if(version==null || !version.startsWith("5.5.22"))throw sqlFailure();
                    if (!datadirRecorded) {recordLifecycle("physical-datadir-verified", process.pid());datadirRecorded = true;}
                    stage=ConnectionStage.GUARD_ROWS;
                    if(rows.next())throw sqlFailure();
                }
            }
            stage=ConnectionStage.WRAP;
            Connection actual = connection;
            return (Connection)Proxy.newProxyInstance(getClass().getClassLoader(),new Class<?>[]{Connection.class},(proxy,method,args)->{
                if (method.getName().equals("close")) {
                    if (attempt.closed.compareAndSet(false,true)) try {actual.close();} finally {attempt.retire();}
                    return null;
                }
                if (method.getName().equals("toString")) return "OwnedIsolatedMysqlConnection[redacted]";
                try {return method.invoke(actual,args);} catch (InvocationTargetException error) {throw error.getCause();}
            });
        } catch (Exception failure) {
            ConnectionFailure diagnostic=connectionFailure(stage,failure);
            // Raw close happens first, so even cleanup during a failed handshake is bounded.
            attempt.expire();
            if (connection != null) try {connection.close();} catch (Exception ignored) {}
            attempt.retire();throw diagnostic;
        } finally {OPENING.remove();props.clear();}
    }

    public static final class FixtureSocketFactory extends StandardSocketFactory {
        public FixtureSocketFactory() {}
        @Override protected Socket createSocket(PropertySet properties) {
            Attempt attempt = OPENING.get();if (attempt == null) throw new IllegalStateException("mysql fixture socket has no owner");
            return attempt.createSocket();
        }
    }
    private final class Attempt {
        final long deadline;final AtomicBoolean closed = new AtomicBoolean();
        Socket socket;boolean retired, expired;ScheduledFuture<?> expiration;
        Attempt(long deadline) {this.deadline=deadline;}
        synchronized Socket createSocket() {
            if (socket != null || retired || expired || deadline-System.nanoTime()<=0) throw new IllegalStateException("mysql fixture socket unavailable");
            socket = new Socket();return socket;
        }
        synchronized void expire() {expired=true;if(socket!=null)try{socket.close();}catch(IOException ignored){}}
        synchronized void retire() {
            if(retired)return;retired=true;expire();if(expiration!=null)expiration.cancel(false);
            attempts.remove(this);connectionSlots.release();
        }
    }

    private void installSchema() throws Exception {
        String ddl=readBounded(Path.of("db","modern_match_settlement.sql").toAbsolutePath().normalize(),16384);
        String body=ddl.replaceAll("(?m)^\\s*--[^\\r\\n]*", "");
        String[] statements=body.split(";");int count=0;
        try(Connection connection=openConnection(deadline(10))) {
            execute(connection,"CREATE DATABASE `"+DATABASE+"` CHARACTER SET ascii");
            execute(connection,"USE `"+DATABASE+"`");
            for(String raw:statements) {
                String sql=raw.trim();if(sql.isEmpty())continue;
                if(!sql.startsWith("CREATE TABLE `lito_modern_") || ++count>4)throw failure("UNEXPECTED_DDL");
                execute(connection,sql);
            }
        } catch(Exception failure) {throw failure("SCHEMA_SETUP");}
        if(count!=4)throw failure("SCHEMA_COUNT");
    }
    private static void execute(Connection connection,String sql) throws SQLException {
        try(var statement=connection.prepareStatement(sql)){statement.setQueryTimeout(2);statement.execute();}
    }
    long[] counts() throws Exception {
        try(Connection c=openConnection(deadline(5))) {
            long[] counts=new long[3];int i=0;
            for(String table:List.of(ModernMatchSettlementJdbc.MATCH,ModernMatchSettlementJdbc.MEMBER,ModernMatchSettlementJdbc.TOTAL)) {
                try(var statement=c.prepareStatement("SELECT COUNT(*) FROM `"+DATABASE+"`.`"+table+"`")) {
                    statement.setQueryTimeout(2);try(var rows=statement.executeQuery()){if(!rows.next())throw sqlFailure();counts[i++]=rows.getLong(1);if(rows.next())throw sqlFailure();}
                }
            }return counts;
        }
    }
    long[] total(int source,long object) throws Exception {
        try(Connection c=openConnection(deadline(5));var statement=c.prepareStatement("SELECT wins,losses,revision FROM `"+DATABASE+"`.`"+ModernMatchSettlementJdbc.TOTAL+"` WHERE source_id=? AND object_id=?")) {
            statement.setQueryTimeout(2);statement.setInt(1,source);statement.setLong(2,object);
            try(var rows=statement.executeQuery()){if(!rows.next())throw sqlFailure();long[] value={rows.getLong(1),rows.getLong(2),rows.getLong(3)};if(rows.next())throw sqlFailure();return value;}
        }
    }
    long botCount() throws Exception {
        try(Connection c=openConnection(deadline(5));var statement=c.prepareStatement("SELECT COUNT(*) FROM `"+DATABASE+"`.`"+ModernMatchSettlementJdbc.BOT+"`")) {
            statement.setQueryTimeout(2);statement.setMaxRows(2);
            try(var rows=statement.executeQuery()) {
                if(!rows.next())throw sqlFailure();long count=rows.getLong(1);
                if(rows.wasNull() || count<0 || rows.next())throw sqlFailure();return count;
            }
        }
    }
    void verifyInnoDb() throws Exception {
        try(Connection c=openConnection(deadline(5));var statement=c.prepareStatement("SELECT TABLE_NAME,ENGINE FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_SCHEMA=? ORDER BY TABLE_NAME")) {
            statement.setQueryTimeout(2);statement.setMaxRows(5);statement.setString(1,DATABASE);
            try(var rows=statement.executeQuery()){int count=0;while(rows.next()){if(++count>4||!"InnoDB".equalsIgnoreCase(rows.getString(2)))throw sqlFailure();}if(count!=4)throw sqlFailure();}
        }
    }
    void restart() throws Exception {if(!attempts.isEmpty())throw failure("RESTART_WITH_CONNECTION");stop();if(forced)throw failure("RESTART_NOT_GRACEFUL");start();}

    private boolean awaitExit(Process owned,long until) throws Exception {
        while(until-System.nanoTime()>0){checkLogs();if(owned.waitFor(100,TimeUnit.MILLISECONDS))return true;}return !owned.isAlive();
    }
    private void checkLogs() throws IOException {
        for(String file:List.of("server-error.log","server-out.log","server-err.log","bootstrap-out.log","bootstrap-err.log")) {
            Path path=directory.resolve(file);if(Files.exists(path,LinkOption.NOFOLLOW_LINKS)&&Files.size(path)>LOG_LIMIT)throw failure("LOG_BOUND");
        }
    }
    private void stop() throws Exception {
        Process owned=process;if(owned==null)return;
        long until=deadline(CLOSE_SECONDS);
        if(owned.isAlive()) {
            try(Connection c=openConnection(Math.min(until,deadline(3)),true)) {c.unwrap(JdbcConnection.class).shutdownServer();}
            catch(Exception ignored) { /* Command may close its own socket; owned exit status decides below. */ }
            if(!awaitExit(owned,until))terminate(owned);
        }
        if(owned.isAlive())throw failure("PROCESS_RETIREMENT");
        if(owned.exitValue()!=0)throw failure("SERVER_NONZERO_EXIT");
        recordLifecycle("server-stopped", owned.pid());process=null;
    }
    private void terminate(Process owned) throws Exception {
        if(owned!=process)throw failure("FOREIGN_PROCESS");
        if(owned.isAlive()){forced=true;owned.destroy();if(!awaitProcess(owned,1))owned.destroyForcibly();}
        if(!awaitProcess(owned,5))throw failure("PROCESS_RETIREMENT");
    }
    private static boolean awaitProcess(Process process,int seconds) {
        boolean interrupted=Thread.interrupted();long until=deadline(seconds);
        try {while(process.isAlive()&&until-System.nanoTime()>0)try{process.waitFor(Math.max(1,until-System.nanoTime()),TimeUnit.NANOSECONDS);}catch(InterruptedException ignored){interrupted=true;}
            return !process.isAlive();
        } finally {if(interrupted)Thread.currentThread().interrupt();}
    }
    static boolean awaitExecutor(ExecutorService executor,int seconds) {
        boolean interrupted=Thread.interrupted();long until=deadline(seconds);
        try {while(!executor.isTerminated()&&until-System.nanoTime()>0)try{executor.awaitTermination(Math.max(1,until-System.nanoTime()),TimeUnit.NANOSECONDS);}catch(InterruptedException ignored){interrupted=true;}
            return executor.isTerminated();
        } finally {if(interrupted)Thread.currentThread().interrupt();}
    }
    private void validateOwned() throws IOException {
        checkAncestors(directory);
        BasicFileAttributes attributes=Files.readAttributes(directory,BasicFileAttributes.class,LinkOption.NOFOLLOW_LINKS);
        if(!directory.getParent().equals(parent) || !directory.toRealPath().equals(directory) || !attributes.isDirectory()
                || directoryKey!=null && !directoryKey.equals(attributes.fileKey())
                || directoryKey==null && !owner.windowsSharing)throw failure("FIXTURE_OWNERSHIP");
        owner.validate();
    }

    /** A fresh CREATE_NEW lock pins the namespace on Windows even when JDK
     * fileKey is null. Unsupported sharing options fail closed. Cleanup keeps
     * this handle until all recursive work is finished; after release it only
     * deletes the exact lock and now-empty root, never another traversal. */
    static final class DirectoryOwner implements AutoCloseable {
        final Path lockPath;
        final boolean windowsSharing;
        private final FileChannel file;
        private final FileLock lock;
        private final Object identity;
        DirectoryOwner(Path directory) throws IOException {
            checkAncestors(directory);
            lockPath=directory.resolve(".isolated-mysql-owner.lock");
            windowsSharing="\\".equals(directory.getFileSystem().getSeparator());
            var options=new HashSet<OpenOption>(List.of(StandardOpenOption.CREATE_NEW,StandardOpenOption.READ,StandardOpenOption.WRITE,LinkOption.NOFOLLOW_LINKS));
            if(windowsSharing){options.add(ExtendedOpenOption.NOSHARE_DELETE);options.add(ExtendedOpenOption.NOSHARE_WRITE);}
            FileChannel opened=null;FileLock acquired=null;
            try {
                opened=FileChannel.open(lockPath,Set.copyOf(options));
                acquired=opened.tryLock();if(acquired==null)throw new IOException("mysql fixture: OWNER_BUSY");
                checkAncestors(lockPath);
                identity=Files.readAttributes(lockPath,BasicFileAttributes.class,LinkOption.NOFOLLOW_LINKS).fileKey();
                if(identity==null&&!windowsSharing)throw new IOException("mysql fixture: OWNER_IDENTITY");
                file=opened;lock=acquired;
            } catch(IOException|RuntimeException|Error failure) {
                if(acquired!=null)try{acquired.close();}catch(IOException ignored){}
                if(opened!=null)try{opened.close();}catch(IOException ignored){}
                throw failure;
            }
        }
        void validate() throws IOException {
            checkAncestors(lockPath);
            var attributes=Files.readAttributes(lockPath,BasicFileAttributes.class,LinkOption.NOFOLLOW_LINKS);
            if(!file.isOpen()||!lock.isValid()||!attributes.isRegularFile()
                    ||identity!=null&&!identity.equals(attributes.fileKey())
                    ||identity==null&&!windowsSharing)throw new IOException("mysql fixture: OWNER_IDENTITY");
        }
        @Override public void close() throws IOException {try{if(lock.isValid())lock.close();}finally{file.close();}}
    }
    private void sanitizeLogs() throws IOException {
        validateOwned();
        for(String name:List.of("server-error.log","server-out.log","server-err.log","bootstrap-out.log","bootstrap-err.log")) {
            Path file=directory.resolve(name);if(!Files.exists(file,LinkOption.NOFOLLOW_LINKS))continue;checkAncestors(file);
            String text=Files.size(file)>LOG_LIMIT?"fixture log exceeded bound; content discarded":readBoundedAllowEmpty(file);
            text=text.replace(new String(password),"[redacted]").replace(passwordHash,"[redacted]");
            Files.writeString(file,text,StandardCharsets.UTF_8);
        }
    }
    private static String readBoundedAllowEmpty(Path file) throws IOException {
        try(var input=Files.newInputStream(file,LinkOption.NOFOLLOW_LINKS)){return new String(input.readNBytes((int)LOG_LIMIT),StandardCharsets.UTF_8);}
    }
    @Override public void close() throws Exception {
        synchronized(lifecycle){if(closed||closing)return;closing=true;}
        Exception problem=null;boolean interrupted=Thread.interrupted();
        try {
            if(!attempts.isEmpty())problem=failure("UNRETIRED_CONNECTIONS");
            for(Attempt attempt:List.copyOf(attempts))attempt.expire();
            try{stop();}catch(Exception failure){problem=failure;}
            if(process!=null&&process.isAlive())try{terminate(process);}catch(Exception failure){problem=failure;}
            synchronized(lifecycle){closed=true;}
            // Do not disguise an unclosed caller handle by retiring its lease.
            // All raw sockets are closed above; a remaining handle is a failed
            // ownership invariant and keeps the artifact instead of reporting success.
            deadlines.shutdownNow();if(!awaitExecutor(deadlines,5))problem=failure("DEADLINE_WORKER_RETIREMENT");
            sanitizeLogs();
            if(problem==null && successful && !forced) {
                validateOwned();
                Files.walkFileTree(directory,new SimpleFileVisitor<>() {
                    int visited;
                    private void inspect(Path path) throws IOException {
                        if(++visited>2048 || !path.startsWith(directory) || path.equals(parent))throw failure("CLEANUP_TARGET");
                        checkAncestors(path);
                    }
                    @Override public FileVisitResult preVisitDirectory(Path dir,BasicFileAttributes attributes) throws IOException {inspect(dir);return FileVisitResult.CONTINUE;}
                    @Override public FileVisitResult visitFile(Path file,BasicFileAttributes attributes) throws IOException {inspect(file);if(!file.equals(owner.lockPath))Files.delete(file);return FileVisitResult.CONTINUE;}
                    @Override public FileVisitResult postVisitDirectory(Path dir,IOException error) throws IOException {
                        if(error!=null)throw error;checkAncestors(dir);
                        if(dir.equals(directory)) {
                            validateOwned();owner.close();
                            // No recursive filesystem actions after releasing
                            // ownership: exact lock then an empty directory.
                            checkAncestors(owner.lockPath);Files.delete(owner.lockPath);
                        }
                        Files.delete(dir);return FileVisitResult.CONTINUE;
                    }
                });
            }
        } finally {try{owner.close();}finally{Arrays.fill(password,'\0');if(interrupted)Thread.currentThread().interrupt();}}
        if(problem!=null)throw failure("CLEANUP");
    }
    private static final class FixtureFailure extends IOException {
        final String code;
        FixtureFailure(String code,Path directory) {super("isolated mysql fixture: "+code+"; retained artifact: "+directory);this.code=code;}
    }
    private FixtureFailure failure(String code) {return new FixtureFailure(code,directory);}
    private static SQLException sqlFailure() {return new SQLException("isolated mysql fixture connection unavailable");}

    enum ConnectionStage { ADMISSION, DEADLINE, CONFIGURATION, CONNECT, NETWORK, GUARD_QUERY, GUARD_ROWS, DATADIR, PORT, VERSION, WRAP }
    static final class ConnectionFailure extends SQLException {
        final String diagnostic;
        ConnectionFailure(ConnectionStage stage,String state,int vendor) {
            super("isolated mysql fixture connection: stage="+stage+"; sqlstate="+state+"; vendor="+vendor,state,vendor);
            diagnostic="stage="+stage+"; sqlstate="+state+"; vendor="+vendor;
        }
    }
    /** Fixed stages + five ASCII SQLSTATE characters + numeric vendor code are
     * the entire diagnostic surface. Never retain raw causes, driver messages,
     * URL, credentials, returned datadir/version, or arbitrary exception text. */
    static ConnectionFailure connectionFailure(ConnectionStage stage,Exception cause) {
        String state=cause instanceof SQLException sql?sql.getSQLState():null;
        if(state==null||!state.matches("[0-9A-Z]{5}"))state="NONE";
        int vendor=cause instanceof SQLException sql?sql.getErrorCode():0;
        return new ConnectionFailure(stage,state,vendor);
    }
}
