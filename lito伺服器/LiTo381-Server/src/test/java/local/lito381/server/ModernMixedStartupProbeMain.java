package local.lito381.server;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

/** Startup timing only; no synthetic player flow or GUI verdict. */
public final class ModernMixedStartupProbeMain {
    public static void main(String[] args)throws Exception {
        if(args.length!=3)throw new IllegalArgumentException("installation/shared/TLS paths required");
        Path evidence=Path.of(System.getProperty("java.io.tmpdir")).resolve("startup-timing.json");
        var state=new LinkedHashMap<String,String>();long started=System.nanoTime();
        state.put("helper_pid",Long.toString(ProcessHandle.current().pid()));state.put("stage","fixture-open");write(evidence,state);
        try(var f=ModernQuestSqlFixture.openMixedModes(Path.of(args[0]),Path.of(args[1]),Path.of(args[2]),true,true)) {
            state.put("fixture_ready_ms",elapsed(started));state.put("mysql_pid",Long.toString(f.mysql.processId()));
            state.put("mysql_port",Integer.toString(f.mysql.port()));state.put("datadir",f.mysql.dataDirectory().toRealPath().toString());
            state.put("stage","source-snapshot");write(evidence,state);
            var before=ModernSlowMainSqlTest.sourceRows(f);state.put("source_snapshot_ms",elapsed(started));
            state.put("stage","main-start");write(evidence,state);
            var server=ModernMain.start(f.config);
            try {
                state.put("ready_ms",elapsed(started));state.put("tls_port",Integer.toString(server.address().getPort()));
                state.put("stage","ready");write(evidence,state);System.out.println("READY "+server.address().getPort());
            }finally{server.close();if(!server.awaitTermination(5000))throw new IllegalStateException("Startup probe Main workers retained");}
            if(!before.equals(ModernSlowMainSqlTest.sourceRows(f)))throw new IllegalStateException("Startup mutated fixture source rows");
            if(f.mysql.activeConnections()!=0||f.mysql.forcedTermination())throw new IllegalStateException("SQL ownership cleanup failed");
            f.successful();state.put("source_rows_unchanged","true");
        }catch(Throwable failure) {
            state.put("failure_type",failure.getClass().getSimpleName());
            state.put("failure_stack",String.join(" | ",java.util.Arrays.stream(failure.getStackTrace()).limit(8).map(Object::toString).toList()));
            state.put("stage","failed");write(evidence,state);throw new IllegalStateException("Owned startup probe failed; see redacted timing evidence");
        }
        state.put("closed_ms",elapsed(started));state.put("stage","stopped");write(evidence,state);
    }
    private static String elapsed(long started){return Long.toString((System.nanoTime()-started)/1_000_000);}
    private static String q(String value){return "\""+value.replace("\\","/").replace("\"","\\\"")+"\"";}
    private static void write(Path evidence,Map<String,String> fields)throws Exception{
        var lines=new java.util.ArrayList<String>();fields.forEach((key,value)->lines.add(q(key)+":"+q(value)));
        Files.writeString(evidence,"{\n"+String.join(",\n",lines)+"\n}",StandardCharsets.UTF_8);
    }
}
