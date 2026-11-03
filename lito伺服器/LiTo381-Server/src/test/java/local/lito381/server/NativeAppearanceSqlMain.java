package local.lito381.server;

import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

/** Fresh locked-dump SELECT evidence; does not mint selection or appearance authority. */
public final class NativeAppearanceSqlMain {
    static void write(Path root, Map<String,Object> v) throws Exception {
        Path temp=root.resolve("appearance-sql-probe.tmp");
        Files.writeString(temp,NativeTerrainProbeMain.json(v),StandardCharsets.UTF_8);
        Files.move(temp,root.resolve("appearance-sql-probe.json"),StandardCopyOption.REPLACE_EXISTING,StandardCopyOption.ATOMIC_MOVE);
    }
    public static void main(String[] args) throws Exception {
        Path output=Path.of(args[2]).toRealPath();
        NativeCanonicalServiceProbeMain.require(output.toString().matches("[\\x20-\\x7e]+")
            &&output.getFileName().toString().startsWith("native-canonical-sql-appearance-"),"owned ASCII root");
        var result=new LinkedHashMap<String,Object>();
        result.put("state","STARTING");result.put("helper_pid",ProcessHandle.current().pid());
        result.put("helper_start_utc",ProcessHandle.current().info().startInstant().orElseThrow().toString());
        result.put("owned_runtime",output.toString());
        result.put("selection_authority",false);result.put("TLS",false);result.put("GUI",false);
        long pid=0;int port=0;write(output,result);
        try(var mysql=ModernSettlementMysqlFixture.open(Path.of(args[0]))) {
            pid=mysql.processId();port=mysql.port();
            result.put("mysql_pid",pid);result.put("mysql_port",port);
            result.put("datadir",mysql.dataDirectory().toRealPath().toString());write(output,result);
            var importer=NativeBaselineFixtureMain.class.getDeclaredMethod("importSql",ModernSettlementMysqlFixture.class,String.class,Map.class,Path.class);
            importer.setAccessible(true);var importing=new LinkedHashMap<String,String>();
            for(String name:List.of("lito_static.sql","380_ac_1.sql","380_ac_2.sql")) {
                byte[] bytes=Files.readAllBytes(Path.of(args[1]).resolve(name));
                NativeCanonicalServiceProbeMain.require(NativePlayerLoadoutSqlMain.sha(bytes).equals(NativePlayerLoadoutSqlMain.DUMPS.get(name)),"locked dump SHA");
                result.put("stage","IMPORT_"+name);write(output,result);
                importer.invoke(null,mysql,new String(bytes,StandardCharsets.UTF_8),importing,output.resolve("import-progress.json"));
            }
            result.put("locked_dump_sha256",NativePlayerLoadoutSqlMain.DUMPS);
            var before=NativePlayerLoadoutSqlMain.checksums(mysql);result.put("dynamic_before",before);
            var rows=new ArrayList<Map<String,Object>>();
            for(var source:Map.of(7,"380_ac_1",42,"380_ac_2").entrySet()) {
                try(var c=mysql.openConnection(ModernSettlementMysqlFixture.deadline(10));var s=c.prepareStatement(
                    "SELECT objid,char_name,Type,Sex,Class FROM `"+source.getValue()+"`.characters WHERE objid IN (?,?) ORDER BY objid")) {
                    s.setQueryTimeout(5);s.setLong(1,10163);s.setLong(2,10275);
                    try(var r=s.executeQuery()) {while(r.next()) {
                        var row=new LinkedHashMap<String,Object>();row.put("sourceId",source.getKey());row.put("schema",source.getValue());
                        for(String f:List.of("objid","Type","Sex","Class"))row.put(f,r.getLong(f));
                        row.put("char_name",r.getString("char_name"));rows.add(row);
                    }}
                }
            }
            NativeCanonicalServiceProbeMain.require(rows.size()==4,"two original objects per source");
            result.put("character_rows",rows);
            result.put("query","SELECT objid,char_name,Type,Sex,Class FROM source.characters WHERE objid IN (10163,10275) ORDER BY objid");
            var after=NativePlayerLoadoutSqlMain.checksums(mysql);
            NativeCanonicalServiceProbeMain.require(before.equals(after)&&before.size()==172,"172 source checksums preserved");
            result.put("dynamic_after",after);result.put("dynamic_changed_tables",List.of());
            NativeCanonicalServiceProbeMain.require(mysql.activeConnections()==0,"all source connections closed");
            result.put("state","PASSED_FRESH_APPEARANCE_CLASS_SQL_READ");write(output,result);
        } finally {
            result.put("mysql_process_retired",pid!=0&&ProcessHandle.of(pid).map(p->!p.isAlive()).orElse(true));
            if(port!=0)try(var socket=new java.net.ServerSocket(port,1,java.net.InetAddress.getByName("127.0.0.1"))){result.put("mysql_listener_released",true);}catch(Exception e){result.put("mysql_listener_released",false);}
            write(output,result);
        }
        System.out.println("Fresh appearance Class SQL evidence complete; owned MySQL retired");
    }
}
