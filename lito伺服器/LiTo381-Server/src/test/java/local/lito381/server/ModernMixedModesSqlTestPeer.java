package local.lito381.server;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/** Owned Main, SELECT-only source SQL, shared pinned maps and two real TLS owners.
 * This peer only hosts/audits; all room/world actions come from the Rust controllers. */
public final class ModernMixedModesSqlTestPeer {
    private ModernMixedModesSqlTestPeer() {}
    static void boot(String stage,long started) {
        System.err.println("MIXED_SQL_BOOT stage="+stage+" elapsedMs="+TimeUnit.NANOSECONDS.toMillis(System.nanoTime()-started));
        System.err.flush();
    }
    static Properties worlds(Path shared)throws Exception {
        var p=ModernQuestSqlFixture.worlds(shared,false,true,true,false,null,false,false,false,false,true);
        merge(p,ModernQuestSqlFixture.worlds(shared,false,true,false,true,null,false,false,false,false,false));
        merge(p,ModernSharedSiegePresetTest.worlds(shared));
        merge(p,ModernPetRaceTestPeer.properties(shared));
        p.setProperty("world.0.565.countdownNanos","0");
        var dm=ModernWorldTestPeer.sharedMap(shared);
        geometry(p,"world.1.514.",dm,32724,32784);
        Map.ofEntries(Map.entry("roomCapacity","2"),Map.entry("stepMillis","200"),
                Map.entry("combatRules",ModernPhysicalCombat.RULESET),Map.entry("attackMillis","700"),
                Map.entry("spellRules",ModernWorldConfiguration.SPELL_RULESET),Map.entry("castMillis","700"),
                Map.entry("itemRules",ModernWorldConfiguration.ITEM_RULESET),Map.entry("weightRules",ModernWorldConfiguration.WEIGHT_RULESET),
                Map.entry("buffRules",ModernWorldConfiguration.TIMED_RULESET),Map.entry("slowRules",ModernSlow.RULESET),
                Map.entry("countdownNanos","0"),Map.entry("activeTimeoutMillis","120000"))
                .forEach((k,v)->p.setProperty("world.1.514."+k,v));
        addHonor(p,shared);
        p.setProperty("world.bindings","1:514,2:554,3:547,4:540,5:532,0:565");
        return p;
    }
    /** Shared532 qualification, reused without changing the original six-mode policy. */
    static void addHonor(Properties p,Path shared)throws Exception {
        var honor=ModernHonorTestPeer.qualified532(shared);
        geometry(p,"world.5.532.",honor,ModernHonorTestPeer.ANCHOR.x(),ModernHonorTestPeer.ANCHOR.y());
        Map.ofEntries(Map.entry("stepMillis","100"),Map.entry("attackMillis","700"),
                Map.entry("combatRules",ModernPhysicalCombat.RULESET),Map.entry("spellRules",ModernWorldConfiguration.DAMAGE_SPELL_RULESET),
                Map.entry("castMillis","1200"),Map.entry("itemRules",ModernWorldConfiguration.ITEM_RULESET),
                Map.entry("weightRules",ModernWorldConfiguration.WEIGHT_RULESET),Map.entry("regenRules",ModernWorldConfiguration.REGEN_RULESET),
                Map.entry("honorRules",ModernHonor.RULESET),Map.entry("tower0","32792,32842,2665,3000"),
                Map.entry("tower1","32798,32842,2669,3000"),Map.entry("spawn0","32792,32843"),Map.entry("spawn1","32798,32843"),
                Map.entry("countdownNanos","0"),Map.entry("activeTimeoutMillis","120000"))
                .forEach((k,v)->p.setProperty("world.5.532."+k,v));
    }
    private static void merge(Properties into,Properties from) {
        for(String name:from.stringPropertyNames()) {
            if(name.equals("world.bindings"))continue;
            String before=into.getProperty(name),after=from.getProperty(name);
            if(before!=null&&!before.equals(after))throw new IllegalArgumentException("conflicting mixed fixture policy");
            into.setProperty(name,after);
        }
    }
    private static void geometry(Properties p,String prefix,ModernWorldMap map,int x,int y) {
        p.setProperty(prefix+"physicalMap",Integer.toString(map.manifest().mapId()));
        p.setProperty(prefix+"spawnX",Integer.toString(x));p.setProperty(prefix+"spawnY",Integer.toString(y));
        p.setProperty(prefix+"manifestSha256",map.digest());
        p.setProperty(prefix+"sectors",map.manifest().sectors().stream().map(s->String.format("%04x%04x",s.key().x(),s.key().y())).collect(Collectors.joining(",")));
        p.setProperty(prefix+"sectorPins",map.manifest().sectors().stream().map(s->String.format("%04x%04x",s.key().x(),s.key().y())+":"+s.byteLength()+":"+s.rawSha256()).collect(Collectors.joining(",")));
    }
    public static void main(String[] args) {
        try {
            if(args.length!=4||!args[0].equals("serve")||!List.of("mixed-modes-sql","mixed-results-sql","mixed-quest-results-sql","mixed-objectives-sql","mixed-all-results-sql").contains(args[2]))throw new IllegalArgumentException();
            boolean all=args[2].equals("mixed-all-results-sql");
            String vipFixture=System.getenv("LITO_MODERN_TEST_EXPIRED_VIP");
            boolean expiredVip="true".equals(vipFixture);
            if(vipFixture!=null&&(!expiredVip||!all))throw new IllegalArgumentException("invalid explicit VIP scenario");
            boolean objectives=all||args[2].equals("mixed-objectives-sql");
            boolean quest=objectives||args[2].equals("mixed-quest-results-sql");
            boolean results=!args[2].equals("mixed-modes-sql");
            int millis=Integer.parseInt(args[3]);if(millis<5000||millis>(all?620000:results?420000:300000))throw new IllegalArgumentException();
            if(!Arrays.equals(System.in.readNBytes(3),new byte[]{'G','O','\n'}))throw new IllegalStateException("missing owned job gate");
            try(var f=ModernQuestSqlFixture.openMixedModes(Path.of(System.getenv(ModernSettlementMysqlFixture.ENV)),
                    Path.of(System.getenv("LITO_SHARED_RESOURCE_ROOT")),Path.of(args[1]),results,all)) {
                if(expiredVip)ModernExpiredVipSqlFixture.seed(f);
                var sourceBefore=ModernSlowMainSqlTest.sourceRows(f);
                long mainStarted=System.nanoTime();
                if (System.getenv("LITO_MODERN_TEST_SERVER_JAR") != null) {
                    ModernPackagedServerTestPeer.run(f, millis);
                } else {
                var server=ModernMain.start(f.config);
                try {
                    boot("main-start",mainStarted);
                    System.out.println("READY "+server.address().getPort());System.out.flush();
                    long deadline=System.nanoTime()+TimeUnit.MILLISECONDS.toNanos(millis);
                    for(;;) {
                        var state=server.snapshot();if(state.accepted()==2&&state.sessions()==0)break;
                        require(state.accepted()<=2&&!server.listenerFailed()&&System.nanoTime()<deadline,"Main lifecycle");Thread.sleep(10);
                    }
                    require(server.snapshot().failed()==0,"failed Main session");
                }finally{server.close();require(server.awaitTermination(5000),"Main workers retained");}
                }
                require(sourceBefore.equals(ModernSlowMainSqlTest.sourceRows(f)),"imported source rows changed");
                if(expiredVip)ModernExpiredVipSqlFixture.auditPreserved(f);
                // Cooperative all-party departure is a real defeat, unlike aborted PvP/Quest/Siege or an unfinished race.
                require(Arrays.equals(f.mysql.counts(),all?new long[]{6,12,2}:objectives?new long[]{5,10,2}:quest?new long[]{4,8,2}:results?new long[]{3,6,2}:new long[]{1,2,2})&&f.mysql.botCount()==(all?28:0),"exact expected results only");
                if(!all)for(int source:List.of(7,42))require(Arrays.equals(f.mysql.total(source,3333),
                        objectives?(source==7?new long[]{4,1,5}:new long[]{2,3,5}):quest?(source==7?new long[]{2,2,4}:new long[]{1,3,4}):results?new long[]{1,2,3}:new long[]{0,1,1}),"source-scoped exact totals");
                try(var c=f.mysql.openConnection(ModernSettlementMysqlFixture.deadline(5));var s=c.createStatement()) {
                    s.setQueryTimeout(3);
                    // Winner source: -1 none; 0 both cooperative players; positive one exact source.
                    int siegeWinner=0;
                    var expected=all?new int[][]{{1,514,7},{5,532,7},{3,547,-2},{4,540,0},{2,554,7},{1,514,42}}:
                            objectives?new int[][]{{1,514,7},{5,532,7},{4,540,0},{2,554,7},{1,514,42}}:
                            quest?new int[][]{{1,514,7},{4,540,-1},{2,554,7},{1,514,42}}:
                            results?new int[][]{{1,514,7},{4,540,-1},{1,514,42}}:new int[][]{{4,540,-1}};
                    try(var r=s.executeQuery("SELECT mode,map_id,participant_count,result_id FROM `"+ModernSettlementMysqlFixture.DATABASE+"`.`lito_modern_match` ORDER BY finished_at_millis,result_id")) {
                        for(int n=0;n<expected.length;n++) {
                            require(r.next()&&r.getInt(1)==expected[n][0]&&r.getInt(2)==expected[n][1]&&r.getInt(3)==(expected[n][0]==3?30:2),"ordered exact mode results");
                            try(var q=c.prepareStatement("SELECT source_id,object_id,team,verdict FROM `"+ModernSettlementMysqlFixture.DATABASE+"`.`lito_modern_match_participant` WHERE result_id=? ORDER BY source_id")) {
                                q.setQueryTimeout(3);q.setBytes(1,r.getBytes(4));
                                try(var p=q.executeQuery()) {
                                    for(int i=0;i<2;i++) {
                                        require(p.next(),"missing human result");
                                        if(expected[n][0]==3&&i==0){require(p.getInt(4)==0||p.getInt(4)==1,"Siege verdict");siegeWinner=p.getInt(4)==0?7:42;}
                                        int winner=expected[n][0]==3?siegeWinner:expected[n][2];
                                        int verdict=winner==0||winner==(i==0?7:42)?0:1;
                                        require(p.getInt(1)==(i==0?7:42)&&p.getLong(2)==3333&&p.getInt(4)==verdict,"source-scoped per-match verdict");
                                        require(p.getInt(3)==(expected[n][0]==4?0:i),"actual result team");
                                    }
                                    require(!p.next(),"extra participant");
                                }
                            }
                            if(expected[n][0]==3)try(var q=c.prepareStatement("SELECT source_id,bot_id,team,verdict FROM `"+ModernSettlementMysqlFixture.DATABASE+"`.`lito_modern_match_bot` WHERE result_id=?")) {
                                q.setQueryTimeout(3);q.setBytes(1,r.getBytes(4));int[] teams={0,0};
                                try(var bots=q.executeQuery()){while(bots.next()){
                                    int team=bots.getInt(3);require(bots.getInt(1)==7&&bots.getLong(2)>0&&(team==0||team==1),"typed Siege BOT");
                                    require(bots.getInt(4)==((team==0?7:42)==siegeWinner?0:1),"BOT follows actual Siege team verdict");teams[team]++;
                                }}
                                require(Arrays.equals(teams,new int[]{14,14}),"balanced 28-BOT journal");
                            }
                        }
                        require(!r.next(),"extra match result");
                    }
                    if(all){
                        require(siegeWinner==7||siegeWinner==42,"completed Siege winner");
                        for(int source:List.of(7,42)) {
                            long won=source==siegeWinner?1:0;
                            require(Arrays.equals(f.mysql.total(source,3333),source==7?new long[]{4+won,2-won,6}:new long[]{2+won,4-won,6}),"six exact combat totals excluding ranked race");
                        }
                        auditRankedRace(c);
                    } else for(String table:List.of("lito_modern_pet_race_result","lito_modern_pet_race_participant"))
                        try(var r=s.executeQuery("SELECT COUNT(*) FROM `"+ModernPetRaceTestPeer.DATABASE+"`.`"+table+"`")) {
                            require(r.next()&&r.getLong(1)==0,"unfinished race stored ranked result");
                        }
                }
                require(f.mysql.activeConnections()==0&&!f.mysql.forcedTermination(),"SQL owners retained");f.successful();
            }
        }catch(Throwable failure) {
            System.err.println("MODERN_MIXED_MODES_SQL_PEER_FAILED "+failure.getClass().getSimpleName()+": "+failure.getMessage());
            Arrays.stream(failure.getStackTrace()).limit(12).forEach(System.err::println);System.exit(2);
        }
    }
    private static void auditRankedRace(java.sql.Connection c)throws Exception {
        String db="`"+ModernPetRaceTestPeer.DATABASE+"`.";
        byte[] result;
        try(var s=c.createStatement()) {
            s.setQueryTimeout(3);
            try(var r=s.executeQuery("SELECT result_id,logical_map_id,physical_map_id,participant_count,content_hash,event_body FROM "+db+"`lito_modern_pet_race_result`")) {
                require(r.next()&&r.getInt(2)==565&&r.getInt(3)==5143&&r.getInt(4)==2,"one ranked shared course result");
                result=r.getBytes(1);require(result.length==16&&Arrays.equals(r.getBytes(5),java.security.MessageDigest.getInstance("SHA-256").digest(r.getBytes(6))),"ranked canonical content hash");
                require(!r.next(),"extra ranked result");
            }
            try(var r=s.executeQuery("SELECT result_id,source_id,object_id,completed_laps,next_checkpoint,placement,finish_elapsed_nanos,dnf_reason,progress_move_sequence FROM "+db+"`lito_modern_pet_race_participant` ORDER BY source_id")) {
                for(int i=0;i<2;i++) {
                    require(r.next()&&Arrays.equals(result,r.getBytes(1))&&r.getInt(2)==(i==0?7:42)&&r.getLong(3)==3333,"ranked source identities");
                    require(r.getLong(4)==(i==0?4:0)&&r.getLong(5)==0&&r.getLong(6)==(i==0?1:0)
                            &&r.getLong(8)==(i==0?ModernPetRace.Dnf.NONE:ModernPetRace.Dnf.WINDOW_CLOSED).ordinal(),"four-lap winner and window-closed runner");
                    require(i==0?r.getLong(7)>0&&r.getLong(9)>=116:r.getLong(7)==0&&r.getLong(9)==0,"ranked movement/finish evidence");
                }
                require(!r.next(),"extra ranked participant");
            }
        }
    }
    private static void require(boolean condition,String stage){if(!condition)throw new IllegalStateException(stage);}
}
