package local.lito381.server;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/** Test-only owned Main/SQL peer; no gameplay dispatch or observation backdoor.
 * The actual Rust client must discover, load and cast exclusively over TLS. */
public final class ModernSlowSqlTestPeer {
    private ModernSlowSqlTestPeer() {}
    private static final ThreadLocal<Long> BOOT_STARTED = new ThreadLocal<>();
    static void boot(String stage) {
        Long started=BOOT_STARTED.get();
        if(started!=null)System.err.println("R388_OWNED_BOOT stage="+stage+
                " elapsed_ms="+TimeUnit.NANOSECONDS.toMillis(System.nanoTime()-started)+
                " pid="+ProcessHandle.current().pid());
    }
    public static void main(String[] args) {
        try {
            BOOT_STARTED.set(System.nanoTime());boot("peer-entry");
            if(args.length!=4||!args[0].equals("serve"))throw new IllegalArgumentException();
            int millis=lifetimeMillis(args[2],args[3]);
            if(!Arrays.equals(System.in.readNBytes(3),new byte[]{'G','O','\n'}))throw new IllegalStateException("missing owned job gate");
            boot("owned-gate-released");
            if(args[2].equals("slow-healing-dev"))runDev(Path.of(args[1]),millis);
            else run(Path.of(args[1]),millis,worldTimeoutMillis(args[2]),args[2].equals("slow-healing-window"));
        } catch(Throwable failure) {
            System.err.println("MODERN_SLOW_SQL_PEER_FAILED "+failure.getClass().getSimpleName());
            Arrays.stream(failure.getStackTrace()).limit(8).forEach(System.err::println);System.exit(2);
        }
    }
    // An explicit test-only window mode gets a bounded operator interval.
    // Source/settlement/worker checks below are identical to the headless mode.
    static int lifetimeMillis(String mode,String value) {
        int millis=Integer.parseInt(value);
        int minimum,maximum;
        switch(mode) {
            case "slow-sql" -> {minimum=5000;maximum=180000;}
            case "slow-window" -> {minimum=30000;maximum=600000;}
            case "slow-healing-window" -> {minimum=30000;maximum=600000;}
            case "slow-healing-dev" -> {minimum=60000;maximum=43_200_000;}
            default -> throw new IllegalArgumentException("unknown owned Slow peer mode");
        }
        if(millis<minimum||millis>maximum)throw new IllegalArgumentException("owned Slow peer lifetime");
        return millis;
    }
    static long worldTimeoutMillis(String mode) {
        return switch(mode) {
            case "slow-sql" -> 120000L;
            // The bounded 540 s operator interval must fit inside this actual
            // configured world, not only inside the peer process lifetime.
            case "slow-window" -> 600000L;
            case "slow-healing-window" -> 600000L;
            case "slow-healing-dev" -> 1_800_000L;
            default -> throw new IllegalArgumentException("unknown owned Slow peer mode");
        };
    }
    private static void run(Path tls,int millis,long worldTimeoutMillis,boolean healing)throws Exception {
        boot("fixture-open");
        var installation=Path.of(System.getenv(ModernSettlementMysqlFixture.ENV));
        var shared=Path.of(System.getenv("LITO_SHARED_RESOURCE_ROOT"));
        try(var f=healing?ModernQuestSqlFixture.openSlowPlayerHealingWindow(installation,shared,tls):
                ModernQuestSqlFixture.openSlow(installation,shared,tls,worldTimeoutMillis)) {
            boot("fixture-ready");
            require(f.config.worlds().playerHealingEnabled()==healing,"explicit combined healing policy");
            var sourceBefore=ModernSlowMainSqlTest.sourceRows(f);
            var server=ModernMain.start(f.config);
            boot("main-ready");
            try {
                System.out.println("READY "+server.address().getPort());System.out.flush();
                long deadline=System.nanoTime()+TimeUnit.MILLISECONDS.toNanos(millis);
                for(;;) {
                    var state=server.snapshot();if(state.accepted()==2&&state.sessions()==0)break;
                    require(state.accepted()<=2&&!server.listenerFailed()&&System.nanoTime()<deadline,"Main lifecycle");Thread.sleep(10);
                }
                require(server.snapshot().failed()==0,"failed Main session");
            }finally{server.close();require(server.awaitTermination(5000),"Main workers retained");}
            require(sourceBefore.equals(ModernSlowMainSqlTest.sourceRows(f)),"imported stats, inventory or skills changed");
            require(Arrays.equals(f.mysql.counts(),new long[]{0,0,0})&&f.mysql.botCount()==0,"aborted battle cannot store a win");
            require(f.mysql.activeConnections()==0&&!f.mysql.forcedTermination(),"SQL owners retained");f.successful();
        }
        BOOT_STARTED.remove();
    }
    /** Local operator showcase, not a test verdict tied to exactly two sessions. */
    private static void runDev(Path tls,int millis)throws Exception {
        boot("dev-fixture-open");
        var installation=Path.of(System.getenv(ModernSettlementMysqlFixture.ENV));
        var shared=Path.of(System.getenv("LITO_SHARED_RESOURCE_ROOT"));
        try(var f=ModernQuestSqlFixture.openSlowPlayerHealingDev(installation,shared,tls)) {
            boot("dev-fixture-ready");
            var sourceBefore=ModernSlowMainSqlTest.sourceRows(f);
            boot("dev-source-snapshot");
            boot("dev-main-start");
            var server=ModernMain.start(f.config);
            boot("dev-main-ready");
            try {
                System.out.println("READY "+server.address().getPort());System.out.flush();
                var commands=new LinkedBlockingQueue<String>();
                var reader=new Thread(()->{
                    try(var input=new BufferedReader(new InputStreamReader(System.in,StandardCharsets.US_ASCII))) {
                        String command;
                        while((command=input.readLine())!=null)commands.offer(command);
                    }catch(Exception ignored) {/* Closing the owned operator pipe requests stop. */}
                    commands.offer("STOP");
                },"modern-dev-operator");
                reader.setDaemon(true);reader.start();
                long deadline=System.nanoTime()+TimeUnit.MILLISECONDS.toNanos(millis);
                for(;;) {
                    require(!server.listenerFailed(),"dev Main listener");
                    if(System.nanoTime()>=deadline){System.out.println("DEV_EXPIRED");System.out.flush();break;}
                    String command=commands.poll(250,TimeUnit.MILLISECONDS);
                    if(command==null)continue;
                    if(command.equals("STOP"))break;
                    if(command.equals("STATUS")){
                        var state=server.snapshot();
                        System.out.println("DEV_STATUS accepted="+state.accepted()+" sessions="+state.sessions()+" failed="+state.failed());
                    }else System.out.println("DEV_UNKNOWN_COMMAND");
                    System.out.flush();
                }
            }finally{server.close();require(server.awaitTermination(5000),"dev Main workers retained");}
            require(sourceBefore.equals(ModernSlowMainSqlTest.sourceRows(f)),"dev source rows changed");
            require(f.mysql.activeConnections()==0&&!f.mysql.forcedTermination(),"dev SQL owners retained");
            f.successful();
        }
        System.out.println("DEV_STOPPED");System.out.flush();
        BOOT_STARTED.remove();
    }
    private static void require(boolean condition,String stage){if(!condition)throw new IllegalStateException(stage);}
}
