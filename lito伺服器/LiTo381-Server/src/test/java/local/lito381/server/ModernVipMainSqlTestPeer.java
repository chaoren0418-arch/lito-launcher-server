package local.lito381.server;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/** GO-gated, Job-owned SQL only. Actual Main serves all client state via TLS;
 * stdout contains lifecycle markers, never a snapshot or LOAD credential. */
public final class ModernVipMainSqlTestPeer {
    public static void main(String[] args) {
        try {
            if(args.length!=4||!args[0].equals("serve")||!Set.of("vip-sql","vip-subset-sql").contains(args[2]))throw new IllegalArgumentException();
            int millis=Integer.parseInt(args[3]);if(millis<5000||millis>180000)throw new IllegalArgumentException();
            if(!Arrays.equals(System.in.readNBytes(3),new byte[]{'G','O','\n'}))throw new IllegalStateException("owned SQL gate");
            run(Path.of(args[1]),args[2],millis);
        }catch(Throwable failure) {
            System.err.println("MODERN_VIP_MAIN_SQL_PEER_FAILED "+failure.getClass().getSimpleName());
            Arrays.stream(failure.getStackTrace()).limit(8).forEach(System.err::println);System.exit(2);
        }
    }
    private static void run(Path tls,String mode,int millis)throws Exception {
        boolean both=mode.equals("vip-sql");
        var installation=Path.of(System.getenv(ModernSettlementMysqlFixture.ENV));var shared=Path.of(System.getenv("LITO_SHARED_RESOURCE_ROOT"));
        try(var f=ModernQuestSqlFixture.openFullVip(installation,shared,tls,both)) {
            ModernVipMainSqlTest.seed(f);var original=ModernSlowMainSqlTest.sourceRows(f);var server=ModernMain.start(f.config);
            try {
                System.out.println("READY "+server.address().getPort());System.out.flush();
                long end=System.nanoTime()+TimeUnit.MILLISECONDS.toNanos(millis);int expected=both?2:1;
                for(;;) {
                    var state=server.snapshot();if(state.accepted()==expected&&state.sessions()==0)break;
                    require(state.accepted()<=expected&&!server.listenerFailed()&&System.nanoTime()<end,"Main lifecycle");Thread.sleep(10);
                }
                require(server.snapshot().failed()==0,"Main failure accounting");
            }finally{server.close();require(server.awaitTermination(5000),"Main workers retained");}
            require(original.equals(ModernSlowMainSqlTest.sourceRows(f)),"imported source changed");ModernExpiredVipSqlFixture.auditPreserved(f);
            try(var executor=new ModernBackendExecutor(1,1);var gate=executor.openSession();var store=new ModernEquipmentOverrideStore(f.directory.resolve("equipment"))) {
                try {
                    for(int id:new int[]{7,42}) {
                        var saved=gate.call(5000,scope->store.read(new TournamentPresence.CharacterKey(id,3333),scope));
                        if(both&&id==7) {
                            var value=saved.orElseThrow();require(value.revision()==1&&value.value().overrides().size()==1,"one exact durable save");
                            var item=value.value().overrides().get(0);require(item.itemObjectId()==ModernVipMainSqlTest.ITEM&&item.catalogId()==300087&&item.equipped(),"source-qualified VIP override");
                        }else require(saved.isEmpty(),"foreign or unrequested save");
                    }
                }finally{executor.close();require(executor.awaitTermination(5000),"store workers retained");}
            }
            require(Arrays.equals(f.mysql.counts(),new long[]{0,0,0})&&f.mysql.botCount()==0,"aborted worlds must not award results");
            require(f.mysql.activeConnections()==0&&!f.mysql.forcedTermination(),"SQL owners retained");f.successful();
            System.err.println("VIP_MAIN_SQL_PRESERVED bothSources="+both+" importedRowsUnchanged=true durableAudit=true noForcedTermination=true");
        }
        System.out.println("CLOSED "+mode);System.out.flush();
    }
    private static void require(boolean condition,String stage){if(!condition)throw new IllegalStateException(stage);}
}
