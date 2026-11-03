package local.lito381.server;

import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.net.InetAddress;
import java.util.*;
import static local.lito381.server.NativeCanonicalServiceProbeMain.require;

/** Separate, opt-in, owned SQL/TLS endpoint for external clients. */
public final class NativeExternalControlV2SqlMain {
    public static void main(String[] args)throws Exception {
        require(args.length==6&&args[4].equals("--external-native-source-melee-control-v2"),"explicit external fixture flag and runtime namespace");
        String namespace=args[5];require(Set.of("server","modern").contains(namespace),"authorized runtime namespace");
        int lifetime=Integer.parseInt(args[3]);require(lifetime>=60&&lifetime<=600,"bounded external fixture lifetime");
        Path requested=Path.of(args[2]).toAbsolutePath();Path output=requested.toRealPath();
        Path namespaceRoot=Path.of("D:/lito-dev-runtime/20261003-supervision/"+namespace).toAbsolutePath();
        require(requested.equals(requested.normalize())&&requested.equals(output)&&namespaceRoot.equals(namespaceRoot.toRealPath())
            &&output.getParent().equals(namespaceRoot)
            &&output.getFileName().toString().matches("native-external-navigation-runtime-[0-9a-f]{32}"),"owned runtime namespace");
        require(Path.of(System.getProperty("java.io.tmpdir")).toRealPath().equals(output),"owned SQL temporary root");
        require(!Files.exists(output.resolve("external-sql-probe.json"))&&!Files.exists(output.resolve("ready.json")),"fresh unique runtime");
        System.setProperty("native.external.namespace",namespace);
        require(System.getProperty("native.external.manifest","").matches("[0-9A-F]{64}"),"launcher manifest binding");
        Path jar=Path.of(AccountRepository.class.getProtectionDomain().getCodeSource().getLocation().toURI());
        require(NativePlayerNavigationSqlMain.sha(Files.readAllBytes(jar)).equals(NativePlayerNavigationSqlMain.JAR_SHA),"pinned repository JAR");
        System.setProperty("sun.net.httpserver.maxReqTime","5");System.setProperty("sun.net.httpserver.maxRspTime","5");
        var report=new LinkedHashMap<String,Object>();report.put("schema",2);report.put("state","STARTING");
        report.put("helper",NativeExternalControlV2Fixture.nativeOwner(ProcessHandle.current(),output.resolve("helper-identity.properties")));report.put("owned_runtime",output.toString());
        report.put("runtime_namespace",namespace);report.put("runtime_namespace_root",namespaceRoot.toString());
        report.put("candidate_manifest_sha256",System.getProperty("native.external.manifest"));report.put("test_selection_issuer",false);
        report.put("authentication_capture",false);report.put("renderer_LOADED",false);report.put("Rust_live_connected",false);
        long pid=0;int port=0;
        NativeExternalControlV2Fixture.write(output.resolve("external-sql-probe.json"),report);
        try(var mysql=ModernSettlementMysqlFixture.open(Path.of(args[0]))) {
            pid=mysql.processId();port=mysql.port();report.put("mysql_pid",pid);report.put("mysql_port",port);
            Path datadir=mysql.dataDirectory().toRealPath();require(datadir.startsWith(output)&&!datadir.equals(output),"owned SQL datadir");
            report.put("datadir",datadir.toString());
            NativeExternalControlV2Fixture.write(output.resolve("external-sql-probe.json"),report);
            var importer=NativeBaselineFixtureMain.class.getDeclaredMethod("importSql",ModernSettlementMysqlFixture.class,String.class,Map.class,Path.class);
            importer.setAccessible(true);var importing=new LinkedHashMap<String,String>();
            for(String name:List.of("lito_static.sql","380_ac_1.sql","380_ac_2.sql")) {
                report.put("stage","IMPORT_"+name);NativeExternalControlV2Fixture.write(output.resolve("external-sql-probe.json"),report);
                byte[] bytes=Files.readAllBytes(Path.of(args[1]).resolve(name));require(NativePlayerNavigationSqlMain.sha(bytes).equals(NativePlayerNavigationSqlMain.DUMPS.get(name)),"locked dump SHA");
                importer.invoke(null,mysql,new String(bytes,StandardCharsets.UTF_8),importing,output.resolve("import-progress.json"));
            }
            report.put("locked_dump_sha256",NativePlayerNavigationSqlMain.DUMPS);
            var before=NativePlayerNavigationSqlMain.checksums(mysql);report.put("dynamic_before",before);
            var jdbc=mysql.writeNativeJdbcSettings();var nativeConfig=NativePlayerNavigationSqlMain.nativeConfig(mysql,jdbc);
            var nativeResult=new LinkedHashMap<String,String>();var verification=NativeBaselineFixtureMain.class.getDeclaredMethod("verifyNative",ModernSettlementMysqlFixture.class,ServerConfig.class,Map.class);
            verification.setAccessible(true);verification.invoke(null,mysql,ServerConfig.load(nativeConfig),nativeResult);
            NativePlayerNavigationSqlMain.objectId=Long.parseLong(nativeResult.get("source_object_id"));require(NativePlayerNavigationSqlMain.objectId==10275,"original selected test object");
            report.put("native_repository",nativeResult);report.put("expected_static_bootstrap_writes",true);
            String reader="external_reader",secret=UUID.randomUUID().toString().replace("-","");
            try(var c=mysql.openConnection(ModernSettlementMysqlFixture.deadline(5));var s=c.createStatement()) {
                s.setQueryTimeout(3);s.execute("CREATE USER '"+reader+"'@'127.0.0.1' IDENTIFIED BY '"+secret+"'");
                for(String db:List.of("380_ac_1","380_ac_2","lito_static"))s.execute("GRANT SELECT ON `"+db+"`.* TO '"+reader+"'@'127.0.0.1'");
                s.execute("FLUSH PRIVILEGES");
            }
            Path tls=output.resolve("configuration-tls");ModernTlsTestMaterial.prepare(tls);Files.createDirectory(output.resolve("equipment"));
            String text="schema=1\nbind.port=0\ntls.pkcs12="+NativePlayerNavigationSqlMain.portable(tls.resolve("server.p12"))+"\ntls.password.env=T\nsources=7,42\nequipment.storeDirectory="+NativePlayerNavigationSqlMain.portable(output.resolve("equipment"))+"\nlimits.backendDeadlineMillis=10000\n";
            for(int id:List.of(7,42)) {
                String p="source."+id+".";text+=p+"name="+(id==7?"太陽神":"月亮神")+"\n"+p+"mysql.host=127.0.0.1\n"+p+"mysql.port="+port+"\n"+p+"mysql.database="+(id==7?"380_ac_1":"380_ac_2")+"\n"+p+"mysql.username.env=U\n"+p+"mysql.password.env=P\n"+p+"mysql.passwordScheme=plain\n"+p+"mysql.tls=disabled\n"+p+"catalog.database=lito_static\n";
            }
            Files.writeString(output.resolve("modern-config-no-secrets.properties"),text,StandardCharsets.UTF_8);
            try(var config=ModernServerConfig.parse(text.getBytes(StandardCharsets.UTF_8),name->switch(name){case "T"->"modern-lito-fixture-only";case "U"->reader;case "P"->secret;default->null;})) {
                var backends=new LinkedHashMap<Integer,ModernJdbcBackend>();for(var source:config.sources())backends.put(source.id(),new ModernJdbcBackend(source.database(),2000,5000));
                try(var fixture=new NativeExternalControlV2Fixture(mysql,config,backends,output,lifetime)) {
                    report.put("state","READY_FOR_EXTERNAL_CLIENT");report.put("fixture_id",fixture.id);report.put("TLS",true);report.put("control_port",fixture.control.getAddress().getPort());report.put("TLS_port",fixture.server.address().getPort());
                    NativeExternalControlV2Fixture.write(output.resolve("external-sql-probe.json"),report);
                    System.out.println("External SQL/TLS fixture READY; ready.json contains bounded endpoint/control material");
                    fixture.waitForShutdown();report.put("shutdown_reason",fixture.stopReason);
                }
            }
            var after=NativePlayerNavigationSqlMain.checksums(mysql);require(before.equals(after)&&before.size()==172,"all 172 source tables restored");
            report.put("dynamic_after",after);report.put("dynamic_changed_tables",List.of());
            require(mysql.activeConnections()==0,"all owned SQL connections closed");report.put("state","PASSED_EXTERNAL_FIXTURE_RETIREMENT");
        }catch(Throwable failure) {
            report.put("state","FAILED");Throwable e=failure instanceof java.lang.reflect.InvocationTargetException v?v.getCause():failure;
            report.put("failure_type",e.getClass().getName());report.put("failure_frames",Arrays.stream(e.getStackTrace()).limit(8).map(Object::toString).toList());
            throw new IllegalStateException("External fixture failed; inspect redacted external-sql-probe.json");
        }finally {
            report.put("mysql_process_retired",pid!=0&&ProcessHandle.of(pid).map(h->!h.isAlive()).orElse(true));
            if(port!=0)try(var socket=new java.net.ServerSocket(port,1,InetAddress.getByName("127.0.0.1"))){report.put("mysql_listener_released",true);}catch(Exception e){report.put("mysql_listener_released",false);}
            NativeExternalControlV2Fixture.write(output.resolve("external-sql-probe.json"),report);
        }
        System.out.println("External fixture retired; 172 source checksums preserved");
    }
}
