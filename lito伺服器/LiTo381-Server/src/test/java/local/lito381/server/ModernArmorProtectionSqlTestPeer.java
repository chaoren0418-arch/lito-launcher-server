package local.lito381.server;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

/** Ordinary Rust controllers use the real Main, TLS and owned SQL only.
 * Missing11101 and explicit gfx0 fixtures stay separate; no fabricated LOAD. */
public final class ModernArmorProtectionSqlTestPeer {
    public static void main(String[] args) {
        try {
            if(args.length!=4||!args[0].equals("serve")||!java.util.Set.of("armor-sql","armor-render-sql","armor-reject-sql").contains(args[2]))throw new IllegalArgumentException();
            int millis=Integer.parseInt(args[3]);if(millis<5000||millis>180000)throw new IllegalArgumentException();
            if(!Arrays.equals(System.in.readNBytes(3),new byte[]{'G','O','\n'}))throw new IllegalStateException("missing owned job gate");
            run(Path.of(args[1]),millis,!args[2].equals("armor-reject-sql"),args[2].equals("armor-render-sql"));
        }catch(Throwable failure) {
            System.err.println("MODERN_ARMOR_SQL_PEER_FAILED "+failure.getClass().getSimpleName());
            Arrays.stream(failure.getStackTrace()).limit(8).forEach(System.err::println);System.exit(2);
        }
    }
    private static void run(Path tls,int millis,boolean armor,boolean render)throws Exception {
        var installation=Path.of(System.getenv(ModernSettlementMysqlFixture.ENV));
        var shared=Path.of(System.getenv("LITO_SHARED_RESOURCE_ROOT"));
        try(var f=armor?ModernQuestSqlFixture.openArmorProtection(installation,shared,tls):ModernQuestSqlFixture.openPlayerHealing(installation,shared,tls)) {
            if(armor) {
                ModernArmorProtectionMainSqlTest.seed(f);
                // The same authentic character can enter a non-armor map only
                // with a qualified ordinary loadout. No runtime source rewrite.
                try(var c=f.mysql.openConnection(ModernSettlementMysqlFixture.deadline(5));var s=c.createStatement()) {
                    s.setQueryTimeout(3);
                    for(int id:java.util.List.of(7,42))s.execute("UPDATE `"+ModernQuestSqlFixture.source(id)+"`.character_items SET is_equipped=0 WHERE item_id=88889");
                    // Only this explicitly named, owned fixture uses native
                    // gfx0. Proc rules live in its catalog, not character DBs.
                    // Ordinary11101 and operator SQL are never substituted.
                    if(render)require(s.executeUpdate("UPDATE `"+ModernQuestSqlFixture.CATALOG+"`.`裝備_armor_魔法` SET gfx_id=0 WHERE item_id=88889")==2,"explicit gfx0 fixture rows");
                }
            }
            var original=ModernSlowMainSqlTest.sourceRows(f);
            var originalArmor=armor?ModernArmorProcsSqlTest.snapshot(f):null;
            var server=ModernMain.start(f.config);
            try {
                System.out.println("READY "+server.address().getPort());System.out.flush();
                long deadline=System.nanoTime()+TimeUnit.MILLISECONDS.toNanos(millis);
                int expected=armor?2:1;
                for(;;) {
                    var state=server.snapshot();if(state.accepted()==expected&&state.sessions()==0)break;
                    require(state.accepted()<=expected&&!server.listenerFailed()&&System.nanoTime()<deadline,"Main lifecycle");Thread.sleep(10);
                }
                require(server.snapshot().failed()==(armor?0:1),"Main failure accounting");
            }finally{server.close();require(server.awaitTermination(5000),"Main workers retained");}
            require(original.equals(ModernSlowMainSqlTest.sourceRows(f)),"imported source changed");
            if(armor)require(originalArmor.equals(ModernArmorProcsSqlTest.snapshot(f)),"armor source changed");
            require(Arrays.equals(f.mysql.counts(),new long[]{0,0,0}),"withdrawal is not a win");
            require(f.mysql.activeConnections()==0&&!f.mysql.forcedTermination(),"SQL owners retained");
            System.err.println("ARMOR_RUST_MAIN_SQL_PRESERVED armor="+armor+" explicitGfx0Fixture="+render+" sources=2 importedRowsUnchanged=true");f.successful();
        }
    }
    private static void require(boolean condition,String stage){if(!condition)throw new IllegalStateException(stage);}
}
