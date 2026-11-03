package local.lito381.server;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

/** Real Main/SQL bridge for the two Rust controllers; only isolated fixture data. */
public final class ModernTimeAttackSqlTestPeer {
    private ModernTimeAttackSqlTestPeer() {}
    public static void main(String[] args) {
        try {
            if(args.length!=4 || !args[0].equals("serve") || !java.util.Set.of("time-attack-sql","npc77-time-attack-sql","npc4-time-attack-sql","npc70-time-attack-sql").contains(args[2])) throw new IllegalArgumentException();
            int millis=Integer.parseInt(args[3]);
            if(millis<5000 || millis>180000) throw new IllegalArgumentException();
            if(!Arrays.equals(System.in.readNBytes(3),new byte[]{'G','O','\n'})) throw new IllegalStateException("missing owned job gate");
            run(Path.of(args[1]),millis,args[2]);
        } catch(Throwable failure) {
            System.err.println("MODERN_TIME_ATTACK_SQL_PEER_FAILED "+failure.getClass().getSimpleName());
            Arrays.stream(failure.getStackTrace()).limit(6).forEach(System.err::println);
            System.exit(2);
        }
    }
    private static void run(Path tls,int millis,String mode)throws Exception {
        boolean npc77=mode.equals("npc77-time-attack-sql"),npc4=mode.equals("npc4-time-attack-sql"),npc70=mode.equals("npc70-time-attack-sql");
        var installation=Path.of(System.getenv(ModernSettlementMysqlFixture.ENV));
        var shared=Path.of(System.getenv("LITO_SHARED_RESOURCE_ROOT"));
        try(var fixture=npc70?ModernQuestSqlFixture.openNpc70TimeAttack(installation,shared,tls):npc77?ModernQuestSqlFixture.openNpc77TimeAttack(installation,shared,tls):npc4?ModernQuestSqlFixture.openNpcMagicTimeAttack(installation,shared,tls):ModernQuestSqlFixture.openTimeAttack(installation,shared,tls)) {
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
            require(Arrays.equals(fixture.mysql.counts(),new long[]{1,2,2}),"exact cooperative result counts");
            require(Arrays.equals(fixture.mysql.total(7,3333),new long[]{0,1,1}),"departed owner loss");
            require(Arrays.equals(fixture.mysql.total(42,3333),new long[]{1,0,1}),"surviving source win");
            require(fixture.mysql.botCount()==0,"no synthetic player result");
            try(var c=fixture.mysql.openConnection(ModernSettlementMysqlFixture.deadline(5))) {
                for(int id:new int[]{7,42})try(var s=c.prepareStatement("SELECT count,is_equipped FROM `"+ModernQuestSqlFixture.source(id)+"`.character_items WHERE id=1002")) {
                    s.setQueryTimeout(2);try(var rows=s.executeQuery()) {
                        require(rows.next() && rows.getLong(1)==(id==7?2:5) && rows.getLong(2)==0 && !rows.next(),"source inventory unchanged");
                    }
                }
            }
            if(npc4)try(var c=fixture.mysql.openConnection(ModernSettlementMysqlFixture.deadline(5))) {
                for(int id:new int[]{7,42})try(var s=c.prepareStatement("SELECT MaxMp,CurMp,(SELECT COUNT(*) FROM `"+ModernQuestSqlFixture.source(id)+"`.character_skills WHERE char_obj_id=3333 AND skill_id=4) FROM `"+ModernQuestSqlFixture.source(id)+"`.characters WHERE objid=3333")) {
                    s.setQueryTimeout(2);try(var rows=s.executeQuery()) {
                        require(rows.next()&&rows.getLong(1)==(id==7?30:40)&&rows.getLong(2)==(id==7?30:40)&&rows.getLong(3)==1&&!rows.next(),"imported mana/learned light arrow unchanged");
                    }
                }
            }
            if(npc77)try(var c=fixture.mysql.openConnection(ModernSettlementMysqlFixture.deadline(5))) {
                for(int id:new int[]{7,42})try(var s=c.prepareStatement("SELECT MaxMp,CurMp,Lawful,(SELECT COUNT(*) FROM `"+ModernQuestSqlFixture.source(id)+"`.character_skills WHERE char_obj_id=3333 AND skill_id=77) FROM `"+ModernQuestSqlFixture.source(id)+"`.characters WHERE objid=3333")) {
                    s.setQueryTimeout(2);try(var rows=s.executeQuery()) {
                        require(rows.next()&&rows.getLong(1)==300&&rows.getLong(2)==300&&rows.getLong(3)==500&&rows.getLong(4)==1&&!rows.next(),"imported mana/lawful/learned skill unchanged");
                    }
                }
            }
            if(npc70)ModernNpcFireStormSqlFixture.assertSourceUnchanged(fixture.mysql);
            require(fixture.mysql.activeConnections()==0,"SQL connections retained");fixture.successful();
        }
    }
    private static void require(boolean value,String stage){if(!value)throw new IllegalStateException(stage);}
}
