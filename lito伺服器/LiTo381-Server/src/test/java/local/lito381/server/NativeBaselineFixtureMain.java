package local.lito381.server;

import java.net.InetAddress;
import java.net.ServerSocket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Owned, opt-in native acceptance fixture. No connection to an installed SQL service. */
public final class NativeBaselineFixtureMain {
    private static final Map<String,String> DUMPS = Map.of(
            "lito_static.sql", "D4111D8AEFF50EA5D9D36630C7381132812F7F727E8432FFAF035B89C9CBB075",
            "380_ac_1.sql", "B384802BFB1A8243C07393BF525E62DAB99FB363C6280607D3FA2401D47AEA3A",
            "380_ac_2.sql", "8F5F43F549024E27867F929A1BCA39C1BB99CC94D22EC1B9FDC2689C6370F4B0");
    private NativeBaselineFixtureMain() {}
    public static void main(String[] args) throws Exception {
        if (args.length != 3 && args.length != 4) throw new IllegalArgumentException("installation, locked generated root, expected native JAR SHA, optional owned manual-player properties required");
        Path nativeCode = Path.of(AccountRepository.class.getProtectionDomain().getCodeSource().getLocation().toURI());
        if (!Files.isRegularFile(nativeCode) || !hash(Files.readAllBytes(nativeCode)).equals(args[2]))
            throw new IllegalStateException("Native repository must load from the fingerprinted baseline/candidate JAR");
        Path root = Path.of(System.getProperty("java.io.tmpdir")).toRealPath();
        Path evidence = root.resolve("native-fixture.json");
        Map<String,String> result = new LinkedHashMap<>();
        result.put("state", "starting");result.put("native_jar", nativeCode.toString());result.put("native_jar_sha256", args[2]);
        result.put("helper_pid", Long.toString(ProcessHandle.current().pid()));write(evidence,result);
        try (var mysql = ModernSettlementMysqlFixture.open(Path.of(args[0]))) {
            result.put("mysql_pid",Long.toString(mysql.processId()));result.put("mysql_port",Integer.toString(mysql.port()));
            result.put("datadir",mysql.dataDirectory().toRealPath().toString());result.put("fixture_directory",mysql.artifactDirectory().toString());
            result.put("datadir_guard","exact real path and port/version verified before SQL");write(evidence,result);
            for (String file : List.of("lito_static.sql","380_ac_1.sql","380_ac_2.sql")) {
                result.put("stage","import:"+file);write(evidence,result);
                Path dump=Path.of(args[1]).resolve(file);byte[] bytes=Files.readAllBytes(dump);
                if (!hash(bytes).equals(DUMPS.get(file))) throw new IllegalStateException("Locked generated SQL fingerprint mismatch");
                long started=System.nanoTime();
                importSql(mysql,new String(bytes,StandardCharsets.UTF_8),result,evidence);
                result.put(file+"_import_ms",Long.toString((System.nanoTime()-started)/1_000_000));write(evidence,result);
                System.out.println("[fixture] locked schema imported: "+file);
            }
            if(args.length==4)NativeManualPlayerSeed.seed(mysql,Path.of(args[3]),result);
            Path jdbc = mysql.writeNativeJdbcSettings();
            Path config = mysql.artifactDirectory().resolve("native-server.properties");
            int nativePort=freeNativePort();
            Files.writeString(config,"""
                    bind=127.0.0.1
                    game.bind=127.0.0.2
                    port=%d
                    advertised.host=127.0.0.2
                    advertised.port=%d
                    source.servers.file=native-sources.properties
                    database.mode=lito_mysql
                    database.connection.properties=%s
                    database.static.database=lito_static
                    database.dynamic.databases=380_ac_1,380_ac_2
                    database.read.only=true
                    state.persistence.enabled=false
                    state.file=./native.state
                    capture.directory=./native-captures
                    classic.world.compatibility.enabled=false
                    classic.world.monster.ai.enabled=false
                    battle.compatibility.service.actors.enabled=false
                    battle.ai.enabled=false
                    login.bootstrap.sequence=login_check,classic_server_list,lito_server_list
                    login.bootstrap.step.delay.ms=250
                    lobby.character.list.enabled=true
                    """.formatted(nativePort,nativePort,jdbc.getFileName()),StandardCharsets.UTF_8);
            Files.writeString(config.resolveSibling("native-sources.properties"),"""
                    source.count=2
                    source.1.name=太陽神
                    source.1.id=1
                    source.1.database=380_ac_1
                    source.1.weight.rate=100.0
                    source.2.name=月亮神
                    source.2.id=2
                    source.2.database=380_ac_2
                    source.2.weight.rate=100.0
                    """,StandardCharsets.UTF_8);
            result.put("stage","native-repository-validation");write(evidence,result);
            verifyNative(mysql,ServerConfig.load(config),result);
            if(args.length==4)NativeManualPlayerSeed.verify(ServerConfig.load(config),Path.of(args[3]),result);
            result.put("profile_path",config.toString());result.put("native_port",Integer.toString(nativePort));
            result.put("state","ready_for_native_client");write(evidence,result);
            System.out.println("[fixture] native SQL reads and central CAS passed; profile ready");
            Map<String,Long> servingChecksums=checksums(mysql);
            Path stop=root.resolve("stop.request");
            while(!Files.exists(stop)) Thread.sleep(500);
            if(!servingChecksums.equals(checksums(mysql)))throw new IllegalStateException("Source schema changed while serving native clients");
            result.put("post_serving_source_checksums","all 172 dynamic tables unchanged after native client activity");
            result.put("state","stopping");write(evidence,result);
        } catch(Exception failure) {
            result.put("state","failed");result.put("failure_type",failure.getClass().getSimpleName());
            result.put("failure_stack",String.join(" | ",java.util.Arrays.stream(failure.getStackTrace()).limit(8).map(Object::toString).toList()));
            if(failure instanceof java.sql.SQLException sql){
                result.put("sql_state",String.valueOf(sql.getSQLState()));result.put("vendor_code",Integer.toString(sql.getErrorCode()));
                if(sql.getErrorCode()==1146){var table=java.util.regex.Pattern.compile("Table '([^']+)' doesn't exist").matcher(String.valueOf(sql.getMessage()));if(table.find())result.put("missing_table",table.group(1));}
            }
            write(evidence,result);
            throw new IllegalStateException("Native isolated fixture failed; see redacted evidence");
        }
        result.put("state","stopped");result.put("cleanup","owned mysql closed; retained fixture files preserved");write(evidence,result);
    }
    private static void verifyNative(ModernSettlementMysqlFixture mysql,ServerConfig config,Map<String,String> result) throws Exception {
        Map<String,Long> before=checksums(mysql);
        try(var routes=AccountRepository.open(config)) {
            // The preserved generated dumps predate the mandatory player-spawn
            // table. Invoke the fingerprinted release's explicit migration only
            // in this guarded owned instance, before any native server runs.
            try(var guarded=mysql.openConnection(ModernSettlementMysqlFixture.deadline(3))) { }
            routes.migrateLitoExtensions();
            result.put("baseline_migration","Release migration applied to owned schemas; adds missing lito_battle_player_spawn and validated defaults");
            routes.healthCheck();
            BattleControlCatalog.load(routes);
            AuthResult auth=routes.authenticate("3333","3333");
            if(!auth.accepted())throw new IllegalStateException("Fixture source login rejected");
            var one=routes.repositoryForClassicSource(config.classicServerNames().get(0));
            var two=routes.repositoryForClassicSource(config.classicServerNames().get(1));
            CharacterSummary first=one.characters("3333").stream().filter(c->c.name().equals("3332")).findFirst().orElseThrow();
            CharacterSummary second=two.characters("3333").stream().filter(c->c.objectId()==first.objectId()).findFirst().orElseThrow();
            if(one.sourceInventory(first.objectId()).isEmpty())throw new IllegalStateException("Source inventory empty");
            GameStateStore state=new GameStateStore(mysql.artifactDirectory().resolve("cas.state"),false);
            String firstIdentity=GameStateStore.sourceCharacter(config.classicServerNames().get(0),first.name());
            String secondIdentity=GameStateStore.sourceCharacter(config.classicServerNames().get(1),second.name());
            state.bindRankingIdentity(firstIdentity,config.classicServerNames().get(0),0,Math.min(255,first.level()),first.type());
            state.bindRankingIdentity(secondIdentity,config.classicServerNames().get(1),0,Math.min(255,second.level()),second.type());
            state.recordResult(firstIdentity,true,100);
            var initial=one.persistTournamentProfile(first.objectId(),first.name(),null,state.tournamentProfileState(firstIdentity));
            if(initial.status()!=RepositoryTournamentMutationStatus.SUCCESS)throw new IllegalStateException("Central insert failed");
            if(one.persistTournamentProfile(first.objectId(),first.name(),null,state.tournamentProfileState(firstIdentity)).status()!=RepositoryTournamentMutationStatus.CONFLICT)
                throw new IllegalStateException("Stale CAS was not rejected");
            state.recordResult(firstIdentity,true,100);
            var updated=one.persistTournamentProfile(first.objectId(),first.name(),initial.profile(),state.tournamentProfileState(firstIdentity));
            if(updated.status()!=RepositoryTournamentMutationStatus.SUCCESS)throw new IllegalStateException("Central update failed");
            state.recordResult(secondIdentity,false,0);
            if(two.persistTournamentProfile(second.objectId(),second.name(),null,state.tournamentProfileState(secondIdentity)).status()!=RepositoryTournamentMutationStatus.SUCCESS)
                throw new IllegalStateException("Second route insert failed");
            var reread=one.tournamentProfiles().profiles().stream().filter(p->p.characterObjectId()==first.objectId()).findFirst().orElseThrow();
            if(reread.state().wins()!=2||reread.snapshotVersion()!=2)throw new IllegalStateException("Central reload failed");
            result.put("source_object_id",Long.toString(first.objectId()));result.put("source_inventory_rows",Integer.toString(one.sourceInventory(first.objectId()).size()));
        }
        if(!before.equals(checksums(mysql)))throw new IllegalStateException("Retained dynamic table checksum changed");
        result.put("source_schema_checksums","all 172 dynamic tables unchanged across native read and central writes");
        result.put("central_cas","insert, stale reject, update, re-read, same objid/different source passed");
    }
    private static Map<String,Long> checksums(ModernSettlementMysqlFixture mysql) throws Exception {
        var result=new LinkedHashMap<String,Long>();
        for(String db:List.of("380_ac_1","380_ac_2"))try(Connection c=mysql.openConnection(ModernSettlementMysqlFixture.deadline(20))) {
            List<String> names=new ArrayList<>();
            try(var s=c.createStatement();var rows=s.executeQuery("SHOW TABLES FROM `"+db+"`")){while(rows.next())names.add(rows.getString(1));}
            for(String name:names)try(var s=c.createStatement();var rows=s.executeQuery("CHECKSUM TABLE `"+db+"`.`"+name.replace("`","``")+"`")) {
                if(!rows.next())throw new IllegalStateException("Checksum missing");long checksum=rows.getLong(2);
                if(rows.wasNull())throw new IllegalStateException("Checksum unavailable");result.put(db+"."+name,checksum);
            }
        }
        if(result.size()!=172)throw new IllegalStateException("Dynamic schema table count differs");return result;
    }
    private static void importSql(ModernSettlementMysqlFixture mysql,String script,Map<String,String> result,Path evidence) throws Exception {
        List<String> statements=coalesce(statements(script));
        for(int start=0;start<statements.size();start+=50)try(Connection c=mysql.openConnection(ModernSettlementMysqlFixture.deadline(25))) {
            c.setNetworkTimeout(Runnable::run, 5000);
            result.put("import_statement",start+"/"+statements.size());write(evidence,result);
            // Each fresh connection needs the schema selected by this dump's explicit USE.
            String use=statements.stream().filter(s->s.startsWith("USE ")).findFirst().orElseThrow();
            try(var s=c.createStatement()) {
                s.setQueryTimeout(5);
                if(start>0)s.execute(use);
                s.execute("SET FOREIGN_KEY_CHECKS=0");
                for(int i=start;i<Math.min(start+50,statements.size());i++)s.execute(statements.get(i));
            }
        }
    }
    static List<String> coalesce(List<String> statements) {
        var result=new ArrayList<String>();String prefix=null;var values=new StringBuilder();int rows=0;
        for(String statement:statements) {
            var match=java.util.regex.Pattern.compile("^(INSERT INTO `[^`]+` VALUES )(.+)$",java.util.regex.Pattern.DOTALL).matcher(statement);
            if(match.matches()&&match.group(2).startsWith("(")) {
                String next=match.group(1);
                if(!next.equals(prefix)||rows==200||values.length()+statement.length()>500_000) {
                    if(prefix!=null)result.add(prefix+values);prefix=next;values.setLength(0);rows=0;
                }
                if(rows++>0)values.append(',');values.append(match.group(2));
            } else {
                if(prefix!=null){result.add(prefix+values);prefix=null;values.setLength(0);rows=0;}result.add(statement);
            }
        }
        if(prefix!=null)result.add(prefix+values);return result;
    }
    static List<String> statements(String sql) {
        var result=new ArrayList<String>();var current=new StringBuilder();char quote=0;boolean block=false,line=false;
        for(int i=0;i<sql.length();i++) {
            char c=sql.charAt(i),next=i+1<sql.length()?sql.charAt(i+1):0;
            if(line){if(c=='\n'){line=false;current.append('\n');}continue;}
            if(block){if(c=='*'&&next=='/'){block=false;i++;current.append(' ');}continue;}
            if(quote!=0){current.append(c);if(c=='\\'&&quote!='`'&&i+1<sql.length()){current.append(sql.charAt(++i));continue;}if(c==quote){if(next==quote){current.append(next);i++;}else quote=0;}continue;}
            if(c=='/'&&next=='*'){block=true;i++;continue;}
            if(c=='#'||c=='-'&&next=='-'&&(i+2==sql.length()||Character.isWhitespace(sql.charAt(i+2)))){line=true;if(c=='-')i++;continue;}
            if(c=='\''||c=='"'||c=='`'){quote=c;current.append(c);continue;}
            if(c==';'){String statement=current.toString().trim();if(!statement.isEmpty())result.add(statement);current.setLength(0);}else current.append(c);
        }
        if(quote!=0||block||!current.toString().trim().isEmpty())throw new IllegalArgumentException("Incomplete SQL dump");return result;
    }
    private static int freeNativePort() throws Exception {
        try(var one=new ServerSocket(0,1,InetAddress.getByName("127.0.0.1"));var two=new ServerSocket(one.getLocalPort(),1,InetAddress.getByName("127.0.0.2"))){return one.getLocalPort();}
    }
    private static String hash(byte[] bytes)throws Exception{return HexFormat.of().withUpperCase().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));}
    private static String quote(String text){return "\""+text.replace("\\","/").replace("\"","\\\"").replace("\n","\\n")+"\"";}
    private static void write(Path path,Map<String,String> values)throws Exception{
        var entries=new ArrayList<String>();values.forEach((key,value)->entries.add(quote(key)+":"+quote(value)));
        Files.writeString(path,"{\n"+String.join(",\n",entries)+"\n}",StandardCharsets.UTF_8);
    }
}
