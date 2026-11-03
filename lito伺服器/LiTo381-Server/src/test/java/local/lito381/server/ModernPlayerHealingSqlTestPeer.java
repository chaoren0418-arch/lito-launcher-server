package local.lito381.server;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

/** Production Main/configuration/JDBC/real RNG; only newly owned SQL. The client
 * must obtain every scene and command receipt over authenticated TLS. */
public final class ModernPlayerHealingSqlTestPeer {
    public static void main(String[] args) {
        try {
            if(args.length!=4||!args[0].equals("serve")||!java.util.Set.of("healing-sql","healing-reject-sql","healing-mixed-sql","healing-quest-sql").contains(args[2]))throw new IllegalArgumentException();
            boolean quest=args[2].equals("healing-quest-sql");
            int millis=Integer.parseInt(args[3]);if(millis<5000||millis>(quest?240000:180000))throw new IllegalArgumentException();
            if(!Arrays.equals(System.in.readNBytes(3),new byte[]{'G','O','\n'}))throw new IllegalStateException("missing owned job gate");
            run(Path.of(args[1]),millis,!args[2].equals("healing-reject-sql"),args[2].equals("healing-mixed-sql"),quest);
        }catch(Throwable failure) {
            System.err.println("MODERN_HEALING_SQL_PEER_FAILED "+failure.getClass().getSimpleName());
            Arrays.stream(failure.getStackTrace()).limit(8).forEach(System.err::println);System.exit(2);
        }
    }
    private static void run(Path tls,int millis,boolean enabled,boolean mixed,boolean quest)throws Exception {
        var installation=Path.of(System.getenv(ModernSettlementMysqlFixture.ENV));
        var shared=Path.of(System.getenv("LITO_SHARED_RESOURCE_ROOT"));
        try(var f=quest?ModernQuestSqlFixture.openQuestPlayerHealing(installation,shared,tls):mixed?ModernQuestSqlFixture.openMixedPlayerHealing(installation,shared,tls):enabled?ModernQuestSqlFixture.openPlayerHealing(installation,shared,tls):ModernQuestSqlFixture.openTargetHaste(installation,shared,tls)) {
            require(f.config.worlds().playerHealingEnabled()==enabled&&f.config.worlds().honorEnabled()==mixed,"explicit healing/Honor config");
            var original=ModernSlowMainSqlTest.sourceRows(f);
            var server=ModernMain.start(f.config);
            try {
                System.out.println("READY "+server.address().getPort());System.out.flush();
                long deadline=System.nanoTime()+TimeUnit.MILLISECONDS.toNanos(millis);
                int expected=enabled?2:1;
                for(;;) {
                    var state=server.snapshot();if(state.accepted()==expected&&state.sessions()==0)break;
                    require(state.accepted()<=expected&&!server.listenerFailed()&&System.nanoTime()<deadline,"Main lifecycle");Thread.sleep(10);
                }
                require(server.snapshot().failed()==(enabled?0:1),"Main failure accounting");
            }finally{server.close();require(server.awaitTermination(5000),"Main workers retained");}
            require(original.equals(ModernSlowMainSqlTest.sourceRows(f)),"imported stats, inventory or skills changed");
            require(Arrays.equals(f.mysql.counts(),quest?new long[]{1,2,2}:new long[]{0,0,0})&&f.mysql.botCount()==(quest?2:0),"exact completed/aborted results and typed bots");
            if(quest) {
                require(Arrays.equals(f.mysql.total(7,3333),new long[]{1,0,1}),"source 7 exact win");
                require(Arrays.equals(f.mysql.total(42,3333),new long[]{0,1,1}),"source 42 exact loss");
                try(var c=f.mysql.openConnection(ModernSettlementMysqlFixture.deadline(5));var s=c.createStatement()) {
                    s.setQueryTimeout(3);
                    try(var r=s.executeQuery("SELECT mode,map_id,participant_count FROM `"+ModernSettlementMysqlFixture.DATABASE+"`.`lito_modern_match`")) {
                        require(r.next()&&r.getInt(1)==2&&r.getInt(2)==554&&r.getInt(3)==4&&!r.next(),"one exact four-participant Quest result");
                    }
                }
            }
            require(f.mysql.activeConnections()==0&&!f.mysql.forcedTermination(),"SQL owners retained");f.successful();
        }
    }
    private static void require(boolean condition,String stage){if(!condition)throw new IllegalStateException(stage);}
}
