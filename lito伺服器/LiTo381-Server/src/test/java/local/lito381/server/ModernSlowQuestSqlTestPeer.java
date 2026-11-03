package local.lito381.server;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

/** Test-only bridge: production Main/auth/source JDBC/Slow29 qualification and TLS,
 * with newly owned SQL rows and explicit shared554 Quest/bot rules. No synthetic provider. */
public final class ModernSlowQuestSqlTestPeer {
    private ModernSlowQuestSqlTestPeer() { }
    public static void main(String[] args) {
        try {
            if(args.length!=4||!args[0].equals("serve")||!args[2].equals("slow-quest-sql"))throw new IllegalArgumentException();
            int millis=Integer.parseInt(args[3]);if(millis<5000||millis>180000)throw new IllegalArgumentException();
            if(!Arrays.equals(System.in.readNBytes(3),new byte[]{'G','O','\n'}))throw new IllegalStateException("missing owned job gate");
            run(Path.of(args[1]),millis);
        }catch(Throwable failure) {
            System.err.println("MODERN_SLOW_QUEST_SQL_PEER_FAILED "+failure.getClass().getSimpleName());
            Arrays.stream(failure.getStackTrace()).limit(8).forEach(System.err::println);System.exit(2);
        }
    }
    private static void run(Path tls,int millis)throws Exception {
        try(var f=ModernQuestSqlFixture.openSlowQuest(Path.of(System.getenv(ModernSettlementMysqlFixture.ENV)),
                Path.of(System.getenv("LITO_SHARED_RESOURCE_ROOT")),tls)) {
            var original=ModernSlowMainSqlTest.sourceRows(f);
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
            require(original.equals(ModernSlowMainSqlTest.sourceRows(f)),"imported source rows changed");
            require(Arrays.equals(f.mysql.counts(),new long[]{1,2,2})&&f.mysql.botCount()==2,"one saved match, two humans, two typed bots");
            require(Arrays.equals(f.mysql.total(7,3333),new long[]{1,0,1})&&Arrays.equals(f.mysql.total(42,3333),new long[]{0,1,1}),"source-separated win/loss");
            require(f.mysql.activeConnections()==0&&!f.mysql.forcedTermination(),"SQL owners retained");f.successful();
        }
    }
    private static void require(boolean condition,String stage){if(!condition)throw new IllegalStateException(stage);}
}
