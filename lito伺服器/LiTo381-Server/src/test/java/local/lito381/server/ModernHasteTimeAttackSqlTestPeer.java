package local.lito381.server;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/** Production Main/JDBC/TLS, owned SQL and shared554; no native540/UI claim. */
public final class ModernHasteTimeAttackSqlTestPeer {
    private ModernHasteTimeAttackSqlTestPeer() {}
    public static void main(String[] args) {
        try {
            if(args.length!=4||!args[0].equals("serve")||!Set.of("target-haste-time-sql","target-haste-timeout-sql").contains(args[2]))throw new IllegalArgumentException();
            int millis=Integer.parseInt(args[3]);if(millis<5000||millis>180000)throw new IllegalArgumentException();
            if(!Arrays.equals(System.in.readNBytes(3),new byte[]{'G','O','\n'}))throw new IllegalStateException("missing owned job gate");
            run(Path.of(args[1]),millis,args[2].equals("target-haste-timeout-sql"));
        }catch(Throwable failure) {
            System.err.println("MODERN_TARGET_HASTE_TIME_SQL_PEER_FAILED "+failure.getClass().getSimpleName());
            Arrays.stream(failure.getStackTrace()).limit(10).forEach(System.err::println);System.exit(2);
        }
    }
    private static void run(Path tls,int millis,boolean timeout)throws Exception {
        try(var f=ModernQuestSqlFixture.openTargetHasteTimeAttack(Path.of(System.getenv(ModernSettlementMysqlFixture.ENV)),
                Path.of(System.getenv("LITO_SHARED_RESOURCE_ROOT")),tls)) {
            // Owned fixture, before Main loads its SELECT-only catalog. Keep a
            // living hasted enemy for the actual 20-second deadline; do not
            // let the supplies fixture's rapid attacks win the race by killing
            // the leader. Production clocks/RNG/damage and source HP stay intact.
            if(timeout)try(var c=f.mysql.openConnection(ModernSettlementMysqlFixture.deadline(5));var s=c.createStatement()) {
                s.setQueryTimeout(3);s.executeUpdate("UPDATE `"+ModernQuestSqlFixture.CATALOG+"`.`npc` SET atkspeed=60000");
            }
            var server=ModernMain.start(f.config);
            try {
                System.out.println("READY "+server.address().getPort());System.out.flush();
                long deadline=System.nanoTime()+TimeUnit.MILLISECONDS.toNanos(millis);
                for(;;) {
                    var state=server.snapshot();if(state.accepted()==2&&state.sessions()==0)break;
                    require(state.accepted()<=2&&!server.listenerFailed()&&System.nanoTime()<deadline,"Main lifecycle");Thread.sleep(10);
                }
                require(server.snapshot().failed()==0,"failed Main session");
            }finally{server.close();require(server.awaitTermination(5000),"Main workers retained");}
            ModernHasteSqlTestPeer.assertSourceUnchanged(f);
            require(Arrays.equals(f.mysql.counts(),new long[]{1,2,2})&&f.mysql.botCount()==0,"one cooperative result, two real sources");
            require(Arrays.equals(f.mysql.total(7,3333),new long[]{0,1,1}),"departed or timeout source loss");
            require(Arrays.equals(f.mysql.total(42,3333),timeout?new long[]{0,1,1}:new long[]{1,0,1}),"surviving source verdict");
            require(f.mysql.activeConnections()==0&&!f.mysql.forcedTermination(),"SQL owners retained");f.successful();
        }
    }
    private static void require(boolean condition,String stage){if(!condition)throw new IllegalStateException(stage);}
}
