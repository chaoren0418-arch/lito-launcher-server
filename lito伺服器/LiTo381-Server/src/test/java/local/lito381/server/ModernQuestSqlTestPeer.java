package local.lito381.server;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

/** Test-only bridge. Main, SQL repositories, combat RNG, outbox and result
 * readers are the production implementations; only owned SQL rows/config are
 * fixtures. Rust assigns a kill-on-close process job before sending GO. */
public final class ModernQuestSqlTestPeer {
    private ModernQuestSqlTestPeer(){}
    public static void main(String[] args){
        try{
            if(args.length!=4||!args[0].equals("serve")||!java.util.Set.of("quest-sql","quest-supplies-sql","quest-teleport-sql","quest-supplied-teleport-sql","quest-magic-sql","quest-bots-sql","quest-supplied-bots-sql","quest-npc77-sql","quest-npc70-sql","quest-matchmaking-sql").contains(args[2]))throw new IllegalArgumentException();
            int millis=Integer.parseInt(args[3]);if(millis<5000||millis>180000)throw new IllegalArgumentException();
            if(!Arrays.equals(System.in.readNBytes(3),new byte[]{'G','O','\n'}))throw new IllegalStateException("missing owned job gate");
            // Repeated teleport/movement needs the existing 1000-HP supplies
            // fixture, not an immortal actor or modified production combat RNG.
            boolean suppliedTeleport=args[2].equals("quest-supplied-teleport-sql");
            boolean suppliedBots=args[2].equals("quest-supplied-bots-sql");
            run(Path.of(args[1]),millis,args[2].equals("quest-supplies-sql")||suppliedTeleport||suppliedBots,args[2].equals("quest-teleport-sql")||suppliedTeleport,args[2].equals("quest-magic-sql")||args[2].equals("quest-bots-sql")||suppliedBots,args[2].equals("quest-bots-sql")||suppliedBots,args[2].equals("quest-npc77-sql"),args[2].equals("quest-npc70-sql")||args[2].equals("quest-matchmaking-sql"),args[2].equals("quest-matchmaking-sql"));
        }catch(Throwable failure){
            System.err.println("MODERN_QUEST_SQL_PEER_FAILED "+failure.getClass().getSimpleName());
            Arrays.stream(failure.getStackTrace()).limit(6).forEach(System.err::println);System.exit(2);
        }
    }
    private static void run(Path tls,int millis,boolean supplies,boolean teleport,boolean npcMagic,boolean bots,boolean npc77,boolean npc70,boolean matchmaking)throws Exception {
        var installation=Path.of(System.getenv(ModernSettlementMysqlFixture.ENV));
        var shared=Path.of(System.getenv("LITO_SHARED_RESOURCE_ROOT"));
        try(var f=matchmaking?ModernQuestSqlFixture.openMatchmakingQuest(installation,shared,tls):npc70?ModernQuestSqlFixture.openNpc70Quest(installation,shared,tls):npc77?ModernQuestSqlFixture.openNpc77Quest(installation,shared,tls):ModernQuestSqlFixture.open(installation,shared,tls,supplies,teleport,npcMagic,bots)){
            var server=ModernMain.start(f.config);
            try{
                System.out.println("READY "+server.address().getPort());System.out.flush();
                long deadline=System.nanoTime()+TimeUnit.MILLISECONDS.toNanos(millis);
                for(;;){var state=server.snapshot();if(state.accepted()==2&&state.sessions()==0)break;
                    require(state.accepted()<=2&&!server.listenerFailed()&&System.nanoTime()<deadline,"Main lifecycle");Thread.sleep(10);}
                require(server.snapshot().failed()==0,"failed Main session");
            }finally{server.close();require(server.awaitTermination(5000),"Main workers retained");}
            require(Arrays.equals(f.mysql.counts(),teleport?new long[]{0,0,0}:new long[]{1,2,2}),"exact completed/aborted result counts");
            if(!teleport) {
                require(Arrays.equals(f.mysql.total(7,3333),new long[]{1,0,1}),"source 7 win");
                require(Arrays.equals(f.mysql.total(42,3333),new long[]{0,1,1}),"source 42 loss");
            }
            require(f.mysql.botCount()==(bots?2:0),"typed bot results do not enter human totals");
            try(var c=f.mysql.openConnection(ModernSettlementMysqlFixture.deadline(5))){
                for(int id:new int[]{7,42})try(var s=c.prepareStatement("SELECT count,is_equipped FROM `"+ModernQuestSqlFixture.source(id)+"`.character_items WHERE id=1002")){
                    s.setQueryTimeout(2);try(var rows=s.executeQuery()){
                        require(rows.next()&&rows.getLong(1)==(id==7?2:5)&&rows.getLong(2)==0&&!rows.next(),"source inventory unchanged");
                    }
                }
            }
            if(npc77)try(var c=f.mysql.openConnection(ModernSettlementMysqlFixture.deadline(5))) {
                for(int id:new int[]{7,42})try(var s=c.prepareStatement("SELECT MaxMp,CurMp,Lawful,(SELECT COUNT(*) FROM `"+ModernQuestSqlFixture.source(id)+"`.character_skills WHERE char_obj_id=3333 AND skill_id=77) FROM `"+ModernQuestSqlFixture.source(id)+"`.characters WHERE objid=3333")) {
                    s.setQueryTimeout(2);try(var rows=s.executeQuery()) {
                        require(rows.next()&&rows.getLong(1)==300&&rows.getLong(2)==300&&rows.getLong(3)==500&&rows.getLong(4)==1&&!rows.next(),"imported mana/lawful/learned skill unchanged");
                    }
                }
            }
            if(npc70)ModernNpcFireStormSqlFixture.assertSourceUnchanged(f.mysql);
            if(supplies&&teleport)try(var c=f.mysql.openConnection(ModernSettlementMysqlFixture.deadline(5))) {
                for(int id:new int[]{7,42})try(var s=c.prepareStatement("SELECT MaxHp,CurHp,MaxMp,CurMp,(SELECT COUNT(*) FROM `"+ModernQuestSqlFixture.source(id)+"`.character_skills WHERE char_obj_id=3333 AND skill_id=5) FROM `"+ModernQuestSqlFixture.source(id)+"`.characters WHERE objid=3333")) {
                    s.setQueryTimeout(2);try(var rows=s.executeQuery()) {
                        require(rows.next()&&rows.getLong(1)==1000&&rows.getLong(2)==1000&&rows.getLong(3)==(id==7?30:40)&&rows.getLong(4)==(id==7?30:40)&&rows.getLong(5)==1&&!rows.next(),"teleport imported vitals and learned skill unchanged");
                    }
                }
            }
            require(f.mysql.activeConnections()==0,"SQL connections retained");f.successful();
        }
    }
    private static void require(boolean condition,String stage){if(!condition)throw new IllegalStateException(stage);}
}
