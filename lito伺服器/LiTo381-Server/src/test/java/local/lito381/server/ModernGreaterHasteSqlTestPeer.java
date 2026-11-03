package local.lito381.server;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

/** Test-only bridge: production Main/auth/source JDBC/GH qualification and TLS,
 * with newly owned SQL rows and explicit shared514 rules. No synthetic provider. */
public final class ModernGreaterHasteSqlTestPeer {
    private ModernGreaterHasteSqlTestPeer() { }
    public static void main(String[] args) {
        try {
            if(args.length!=4||!args[0].equals("serve")||!args[2].equals("greater-haste-sql"))throw new IllegalArgumentException();
            int millis=Integer.parseInt(args[3]);if(millis<5000||millis>180000)throw new IllegalArgumentException();
            if(!Arrays.equals(System.in.readNBytes(3),new byte[]{'G','O','\n'}))throw new IllegalStateException("missing owned job gate");
            run(Path.of(args[1]),millis);
        }catch(Throwable failure) {
            System.err.println("MODERN_HASTE_SQL_PEER_FAILED "+failure.getClass().getSimpleName());
            Arrays.stream(failure.getStackTrace()).limit(8).forEach(System.err::println);System.exit(2);
        }
    }
    private static void run(Path tls,int millis)throws Exception {
        try(var f=ModernQuestSqlFixture.openGreaterHaste(Path.of(System.getenv(ModernSettlementMysqlFixture.ENV)),
                Path.of(System.getenv("LITO_SHARED_RESOURCE_ROOT")),tls)) {
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
            assertSourceUnchanged(f);
            require(Arrays.equals(f.mysql.counts(),new long[]{0,0,0})&&f.mysql.botCount()==0,"aborted battle cannot store a win");
            require(f.mysql.activeConnections()==0&&!f.mysql.forcedTermination(),"SQL owners retained");f.successful();
        }
    }
    static void assertSourceUnchanged(ModernQuestSqlFixture f)throws Exception {
        try(var c=f.mysql.openConnection(ModernSettlementMysqlFixture.deadline(5))) {
            for(int id:new int[]{7,42}) {
                String db="`"+ModernQuestSqlFixture.source(id)+"`";
                try(var s=c.prepareStatement("SELECT MaxHp,CurHp,MaxMp,CurMp,Intel,Food,(SELECT COUNT(*) FROM "+db+".character_skills WHERE char_obj_id=3333 AND skill_id=54) FROM "+db+".characters WHERE objid=3333")) {
                    s.setQueryTimeout(2);try(var r=s.executeQuery()) {
                        require(r.next()&&r.getLong(1)==1000&&r.getLong(2)==1000&&r.getLong(3)==300&&r.getLong(4)==300
                                &&r.getLong(5)==(id==7?5000000000L:12)&&r.getLong(6)==(id==7?225:180)&&r.getLong(7)==(id==7?1:0)&&!r.next(),"imported stats and learned skill unchanged");
                    }
                }
                try(var s=c.prepareStatement("SELECT id,count,is_equipped FROM "+db+".character_items ORDER BY id")) {
                    s.setQueryTimeout(2);try(var r=s.executeQuery()) {
                        require(r.next()&&r.getLong(1)==1001&&r.getLong(2)==1&&r.getLong(3)==1,"source weapon unchanged");
                        require(r.next()&&r.getLong(1)==1002&&r.getLong(2)==(id==7?2:5)&&r.getLong(3)==0,"source supplies unchanged");
                        if(id==7)require(r.next()&&r.getLong(1)==1003&&r.getLong(2)==1&&r.getLong(3)==1,"source helm unchanged");
                        require(!r.next(),"unexpected source inventory");
                    }
                }
            }
        }
    }
    private static void require(boolean condition,String stage){if(!condition)throw new IllegalStateException(stage);}
}
