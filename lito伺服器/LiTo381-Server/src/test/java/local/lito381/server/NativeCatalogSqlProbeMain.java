package local.lito381.server;

import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.MessageDigest;
import java.sql.Connection;
import java.util.*;
import static local.lito381.server.NativeCanonicalServiceProbeMain.require;

/** Dedicated locked-dump SQL owner. Never opens an installed SQL instance. */
public final class NativeCatalogSqlProbeMain {
    static long objectId;
    static final Map<String,String> DUMPS=Map.of(
        "lito_static.sql","D4111D8AEFF50EA5D9D36630C7381132812F7F727E8432FFAF035B89C9CBB075",
        "380_ac_1.sql","B384802BFB1A8243C07393BF525E62DAB99FB363C6280607D3FA2401D47AEA3A",
        "380_ac_2.sql","8F5F43F549024E27867F929A1BCA39C1BB99CC94D22EC1B9FDC2689C6370F4B0");
    static final String JAR_SHA="B1A881C9D588862A4B707319EC903E32F7ABFF00677E001B84E2047CFE53CDAE";
    public static void main(String[] args)throws Exception {
        Path output=Path.of(args[2]).toRealPath();
        require(output.toString().matches("[\\x20-\\x7e]+")&&output.getFileName().toString().startsWith("native-canonical-sql-"),"unique ASCII owner root");
        Path jar=Path.of(AccountRepository.class.getProtectionDomain().getCodeSource().getLocation().toURI());
        require(Files.isRegularFile(jar)&&sha(Files.readAllBytes(jar)).equals(JAR_SHA),"pinned repository JAR");
        var result=new LinkedHashMap<String,Object>();var importing=new LinkedHashMap<String,String>();
        result.put("state","STARTING");result.put("owned_runtime",output.toString());result.put("candidate_jar_sha256",JAR_SHA);
        result.put("helper_pid",ProcessHandle.current().pid());result.put("helper_start_utc",ProcessHandle.current().info().startInstant().orElseThrow().toString());write(output,result);
        long mysqlPid=0;int port=0;
        try(var mysql=ModernSettlementMysqlFixture.open(Path.of(args[0]))) {
            mysqlPid=mysql.processId();port=mysql.port();result.put("mysql_pid",mysqlPid);result.put("mysql_port",port);
            result.put("mysql_start_utc",ProcessHandle.of(mysqlPid).orElseThrow().info().startInstant().orElseThrow().toString());
            result.put("datadir",mysql.dataDirectory().toRealPath().toString());result.put("datadir_guard","owned fixture verifies exact datadir/port/version before each connection");write(output,result);
            var importer=NativeBaselineFixtureMain.class.getDeclaredMethod("importSql",ModernSettlementMysqlFixture.class,String.class,Map.class,Path.class);importer.setAccessible(true);
            for(String file:List.of("lito_static.sql","380_ac_1.sql","380_ac_2.sql")) {
                result.put("stage","IMPORT_"+file);write(output,result);byte[] bytes=Files.readAllBytes(Path.of(args[1]).resolve(file));require(sha(bytes).equals(DUMPS.get(file)),"locked dump fingerprint");
                importer.invoke(null,mysql,new String(bytes,StandardCharsets.UTF_8),importing,output.resolve("import-progress.json"));System.out.println("Imported fingerprinted "+file);
            }
            result.put("locked_dump_sha256",DUMPS);var dynamicBefore=checksums(mysql);result.put("dynamic_before",dynamicBefore);
            var staticBefore=allChecksums(mysql,"lito_static");result.put("static_before",staticBefore);
            var jdbc=mysql.writeNativeJdbcSettings();var nativeConfig=nativeConfig(mysql,jdbc);var nativeResult=new LinkedHashMap<String,String>();
            var verification=NativeBaselineFixtureMain.class.getDeclaredMethod("verifyNative",ModernSettlementMysqlFixture.class,ServerConfig.class,Map.class);verification.setAccessible(true);
            verification.invoke(null,mysql,ServerConfig.load(nativeConfig),nativeResult);objectId=Long.parseLong(nativeResult.get("source_object_id"));
            result.put("native_repository",nativeResult);result.put("object_id",objectId);result.put("central_after_bootstrap",central(mysql));
            result.put("expected_bootstrap_writes",List.of("release migration in owned static schema, including lito_battle_player_spawn", "source1 profile INSERT then UPDATE: version2 wins2", "source2 same-object profile INSERT: version1 losses1"));
            result.put("stage","ACTUAL_SQL_TLS");write(output,result);
            String reader="canonical_reader",secret=UUID.randomUUID().toString().replace("-","");
            try(var c=mysql.openConnection(ModernSettlementMysqlFixture.deadline(5));var s=c.createStatement()){
                s.setQueryTimeout(3);s.execute("CREATE USER '"+reader+"'@'127.0.0.1' IDENTIFIED BY '"+secret+"'");
                for(String db:List.of("380_ac_1","380_ac_2","lito_static"))s.execute("GRANT SELECT ON `"+db+"`.* TO '"+reader+"'@'127.0.0.1'");
                s.execute("FLUSH PRIVILEGES");
            }
            Path tls=output.resolve("configuration-tls");ModernTlsTestMaterial.prepare(tls);Files.createDirectory(output.resolve("equipment"));
            String configText="schema=1\nbind.port=0\ntls.pkcs12="+portable(tls.resolve("server.p12"))+"\ntls.password.env=T\nsources=7,42\nequipment.storeDirectory="+portable(output.resolve("equipment"))+"\nlimits.backendDeadlineMillis=10000\n";
            for(int id:List.of(7,42)) {
                String p="source."+id+".";configText+=p+"name="+(id==7?"太陽神":"月亮神")+"\n"+p+"mysql.host=127.0.0.1\n"+p+"mysql.port="+port+"\n"+p+"mysql.database="+(id==7?"380_ac_1":"380_ac_2")+"\n"+p+"mysql.username.env=U\n"+p+"mysql.password.env=P\n"+p+"mysql.passwordScheme=plain\n"+p+"mysql.tls=disabled\n"+p+"catalog.database=lito_static\n";
            }
            Files.writeString(output.resolve("modern-config-no-secrets.properties"),configText,StandardCharsets.UTF_8);
            try(var config=ModernServerConfig.parse(configText.getBytes(StandardCharsets.UTF_8),name->switch(name){case "T"->"modern-lito-fixture-only";case "U"->reader;case "P"->secret;default->null;})) {
                var backends=new LinkedHashMap<Integer,ModernJdbcBackend>();for(var source:config.sources())backends.put(source.id(),new ModernJdbcBackend(source.database(),2000,5000));
                NativeCatalogSqlTlsPeer.run(new String[]{portable(output.resolve("native-dm564-candidate.properties")),portable(output)},config,backends,result);
            }
            centralCas(mysql,nativeConfig,result,output);
            var after=checksums(mysql);require(dynamicBefore.equals(after),"all 172 dynamic checksums unchanged after TLS/stale/reprepare/CAS");
            result.put("dynamic_after",after);result.put("dynamic_changed_tables",List.of());
            var staticAfter=allChecksums(mysql,"lito_static");result.put("static_after",staticAfter);var delta=new ArrayList<String>();for(var key:staticAfter.keySet())if(!Objects.equals(staticBefore.get(key),staticAfter.get(key)))delta.add(key);result.put("static_changed_tables",delta);
            result.put("central_after",central(mysql));result.put("active_SQL_connections_before_shutdown",mysql.activeConnections());
            require(mysql.activeConnections()==0,"owned SQL connections closed");result.put("state","PASSED_CATALOG_SQL_SUBSET");write(output,result);
        }catch(Throwable failure){
            result.put("state","FAILED");Throwable detail=failure instanceof java.lang.reflect.InvocationTargetException invocation?invocation.getCause():failure;
            result.put("failure_type",detail.getClass().getName());result.put("failure_frames",Arrays.stream(detail.getStackTrace()).limit(12).map(Object::toString).toList());write(output,result);
            throw new IllegalStateException("Isolated SQL probe failed; inspect redacted catalog-sql-probe.json");
        }finally{
            result.put("mysql_process_retired",mysqlPid!=0&&ProcessHandle.of(mysqlPid).map(h->!h.isAlive()).orElse(true));
            if(port!=0)try(var listener=new java.net.ServerSocket(port,1,java.net.InetAddress.getByName("127.0.0.1"))){result.put("mysql_listener_released",true);}catch(Exception occupied){result.put("mysql_listener_released",false);}
            write(output,result);
        }
        System.out.println("Canonical SQL subset complete; own MySQL process and listener retired");
    }
    static void centralCas(ModernSettlementMysqlFixture mysql,Path path,Map<String,Object> out,Path output)throws Exception {
        try(var routes=AccountRepository.open(ServerConfig.load(path))) {
            var one=routes.repositoryForClassicSource("太陽神");var two=routes.repositoryForClassicSource("月亮神");
            var a=one.tournamentProfiles().profiles().stream().filter(p->p.characterObjectId()==objectId).findFirst().orElseThrow();var b=two.tournamentProfiles().profiles().stream().filter(p->p.characterObjectId()==objectId).findFirst().orElseThrow();
            try{one.persistTournamentProfile(objectId,a.characterName(),a,b.state());throw new IllegalStateException("cross-source persistence accepted");}catch(IllegalArgumentException expected){}
            require(one.persistTournamentProfile(objectId,a.characterName(),null,a.state()).status()==RepositoryTournamentMutationStatus.CONFLICT,"stale absent profile CAS rejected");
            var update=one.persistTournamentProfile(objectId,a.characterName(),a,a.state());require(update.status()==RepositoryTournamentMutationStatus.SUCCESS&&update.profile().snapshotVersion()==a.snapshotVersion()+1,"explicit no-result-state persistence CAS");
            require(one.persistTournamentProfile(objectId,a.characterName(),a,a.state()).status()==RepositoryTournamentMutationStatus.CONFLICT,"old version CAS rejected");
            require(two.tournamentProfiles().profiles().stream().filter(p->p.characterObjectId()==objectId).findFirst().orElseThrow().equals(b),"other-source same-object full snapshot preserved");
            require(one.tournamentProfiles().profiles().stream().filter(p->p.characterObjectId()==objectId).findFirst().orElseThrow().equals(update.profile()),"negative writes preserve current full snapshot");
            out.put("expected_post_TLS_SQL_write","one explicit source1 central CAS update version2->3 with unchanged result state; SCENE_ONLY does not settle combat");out.put("central_cross_source_and_stale_CAS",true);
        }
    }
    static Path nativeConfig(ModernSettlementMysqlFixture mysql,Path jdbc)throws Exception {
        Path config=jdbc.resolveSibling("canonical-native.properties");
        Files.writeString(config,"bind=127.0.0.1\nport=57399\nsource.servers.file=canonical-sources.properties\ndatabase.mode=lito_mysql\ndatabase.connection.properties="+jdbc.getFileName()+"\ndatabase.static.database=lito_static\ndatabase.dynamic.databases=380_ac_1,380_ac_2\ndatabase.read.only=true\nstate.persistence.enabled=false\n",StandardCharsets.UTF_8);
        Files.writeString(config.resolveSibling("canonical-sources.properties"),"source.count=2\nsource.1.name=太陽神\nsource.1.id=1\nsource.1.database=380_ac_1\nsource.1.weight.rate=100.0\nsource.2.name=月亮神\nsource.2.id=2\nsource.2.database=380_ac_2\nsource.2.weight.rate=100.0\n",StandardCharsets.UTF_8);return config;
    }
    @SuppressWarnings("unchecked") static Map<String,Long> checksums(ModernSettlementMysqlFixture mysql)throws Exception {
        var method=NativeBaselineFixtureMain.class.getDeclaredMethod("checksums",ModernSettlementMysqlFixture.class);method.setAccessible(true);return (Map<String,Long>)method.invoke(null,mysql);
    }
    static Map<String,Long> allChecksums(ModernSettlementMysqlFixture mysql,String db)throws Exception {
        var out=new TreeMap<String,Long>();try(var c=mysql.openConnection(ModernSettlementMysqlFixture.deadline(20));var s=c.createStatement()) {
            var names=new ArrayList<String>();try(var rows=s.executeQuery("SHOW TABLES FROM `"+db+"`")){while(rows.next())names.add(rows.getString(1));}
            for(String name:names)try(var rows=s.executeQuery("CHECKSUM TABLE `"+db+"`.`"+name.replace("`","``")+"`")){require(rows.next(),"checksum exists");out.put(db+"."+name,rows.getLong(2));require(!rows.wasNull(),"checksum defined");}
        }return out;
    }
    static Map<String,Object> central(ModernSettlementMysqlFixture mysql)throws Exception {
        var out=new LinkedHashMap<String,Object>();try(var c=mysql.openConnection(ModernSettlementMysqlFixture.deadline(5));var s=c.createStatement();var rows=s.executeQuery("SHOW TABLES FROM lito_static")) {
            var tables=new ArrayList<String>();while(rows.next())if(rows.getString(1).contains("tournament")||rows.getString(1).contains("character_record"))tables.add(rows.getString(1));
            for(String table:tables)try(var q=c.createStatement();var r=q.executeQuery("SELECT * FROM lito_static.`"+table+"` ORDER BY 1,2")){var records=new ArrayList<List<String>>();int count=r.getMetaData().getColumnCount();while(r.next()){var values=new ArrayList<String>();for(int i=1;i<=count;i++)values.add(r.getString(i));records.add(values);}out.put(table,records);}
        }return out;
    }
    static String portable(Path path){return path.toString().replace('\\','/');}
    static String sha(byte[] bytes)throws Exception{return HexFormat.of().withUpperCase().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));}
    static void write(Path root,Map<String,Object> result)throws Exception {Path target=root.resolve("catalog-sql-probe.json"),temp=root.resolve("catalog-sql-probe.tmp");Files.writeString(temp,NativeTerrainProbeMain.json(result),StandardCharsets.UTF_8);Files.move(temp,target,StandardCopyOption.REPLACE_EXISTING,StandardCopyOption.ATOMIC_MOVE);}
}
