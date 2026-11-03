package local.lito381.server;

import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

/** Test-only real Main/TLS/SQL, two Rust controllers and shared sprite workers.
 * Explicit modes select synthetic pinned S32 or unchanged shared Kent terrain;
 * neither claims native LiTo547 geometry reconstruction. */
public final class ModernSiegeSqlTestPeer {
    private ModernSiegeSqlTestPeer() {}
    public static void main(String[] args) {
        try {
            if(args.length!=4 || !args[0].equals("serve") || !java.util.Set.of("siege-sql","siege-shared-sql").contains(args[2])) throw new IllegalArgumentException();
            int millis=Integer.parseInt(args[3]);
            if(millis<5000 || millis>180000) throw new IllegalArgumentException();
            if(!Arrays.equals(System.in.readNBytes(3),new byte[]{'G','O','\n'})) throw new IllegalStateException("missing owned job gate");
            run(Path.of(args[1]),millis,args[2].equals("siege-shared-sql"));
        } catch(Throwable failure) {
            System.err.println("MODERN_SIEGE_SQL_PEER_FAILED "+failure.getClass().getSimpleName());
            Arrays.stream(failure.getStackTrace()).limit(6).forEach(System.err::println);
            System.exit(2);
        }
    }
    private static void run(Path tls,int millis,boolean shared)throws Exception {
        Path arena=shared?null:Files.createTempDirectory("modern-siege-arena-").toAbsolutePath().normalize();
        var worlds=shared?ModernSharedSiegePresetTest.worlds(Path.of(System.getenv("LITO_SHARED_RESOURCE_ROOT"))):ModernSiegeConfigurationTest.world(arena,120000);
        try(var fixture=ModernQuestSqlFixture.openSiege(Path.of(System.getenv(ModernSettlementMysqlFixture.ENV)),tls,worlds)) {
            // Keep the actual 10,000-HP preset towers. Only this isolated test
            // catalog gives its two players a GM weapon, through normal JDBC
            // qualification and real server RNG/damage, to bound the test run.
            if(shared)try(var c=fixture.mysql.openConnection(ModernSettlementMysqlFixture.deadline(5));var s=c.createStatement()) {
                s.setQueryTimeout(2);require(s.executeUpdate("UPDATE `"+ModernQuestSqlFixture.CATALOG+"`.`weapon` SET dmgmodifier=20000 WHERE item_id=1")==1,"isolated GM weapon");
            }
            var server=ModernMain.start(fixture.config);
            try {
                System.out.println("READY "+server.address().getPort());System.out.flush();
                long deadline=System.nanoTime()+TimeUnit.MILLISECONDS.toNanos(millis);
                for(;;) {
                    var state=server.snapshot();if(state.accepted()==2 && state.sessions()==0)break;
                    require(state.accepted()<=2 && !server.listenerFailed() && System.nanoTime()<deadline,"Main lifecycle");
                    Thread.sleep(10);
                }
                require(server.snapshot().failed()==0,"failed Main session");
            } finally { server.close();require(server.awaitTermination(5000),"Main workers retained"); }
            require(Arrays.equals(fixture.mysql.counts(),new long[]{1,2,2}),"exact siege result counts");
            var a=fixture.mysql.total(7,3333);var b=fixture.mysql.total(42,3333);
            require(a[0]+b[0]==1 && a[1]+b[1]==1 && a[2]==1 && b[2]==1,"source-scoped siege totals");
            // BOT audit rows are intentional; counts() above proves only the
            // two source-scoped humans entered character result/totals tables.
            require(fixture.mysql.botCount()==28,"all dummies retained only in BOT audit");
            require(fixture.mysql.activeConnections()==0,"SQL connections retained");fixture.successful();
        }
        // Only the newly owned arena's fixed plain files/directories, after
        // Main/SQL exit. Preserve unexpected contents as diagnostic evidence.
        if(shared)return; // Real shared resources are read-only; never clean them.
        Path map=arena.resolve("map/4");
        try(var entries=Files.list(map)) {for(Path file:entries.toList()) {
            require(file.getParent().equals(map) && Files.isRegularFile(file,LinkOption.NOFOLLOW_LINKS) && file.getFileName().toString().endsWith(".s32"),"owned map contents");
            Files.delete(file);
        }}
        Files.delete(map);Files.delete(arena.resolve("map"));Files.delete(arena.resolve("quest-results"));Files.delete(arena);
    }
    private static void require(boolean value,String stage){if(!value)throw new IllegalStateException(stage);}
}
