package local.lito381.server;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.Map;

/** Opt-in TEST CLASSPATH only. Source SELECT evidence; never creates bots or opens a game listener. */
public final class ModernGmCommandReadProbe {
    private ModernGmCommandReadProbe() {}
    public static void main(String[] args) {
        int exit=2;char[] password=null;String stage="CONFIGURATION";
        try {
            if(args.length!=1)throw new IllegalArgumentException("explicit configuration required");
            Path file=Path.of(args[0]);String account=System.getenv("LITO_MODERN_PROBE_ACCOUNT"),secret=System.getenv("LITO_MODERN_PROBE_PASSWORD");
            if(account==null||secret==null)throw new IllegalArgumentException("explicit probe credentials required");password=secret.toCharArray();
            try(var settings=ModernDatabaseReadProbe.load(file);
                var config=ModernDatabaseReadProbe.modernConfig(settings,ModernCharacterDataReadProbe.catalogDatabase(file));
                var executor=new ModernBackendExecutor(1,1);var gate=executor.openSession()) {
                try {
                    long characters=0;long[] decisions=new long[ModernGmCommands.Decision.values().length];
                    for(var source:config.sources()) {
                        var audit=new ModernEquipmentReadProbe.Audit();
                        var dao=new ModernJdbcBackend(source.database(),config.dbConnectMillis(),config.dbNetworkMillis(),
                                (url,properties)->ModernEquipmentReadProbe.inspectOwned(new com.mysql.cj.jdbc.Driver().connect(url,properties),
                                        ModernBackendScope.current(),source.database().database(),source.catalogDatabase(),config.dbNetworkMillis(),null,audit,null));
                        var verifier=ModernAccountAuthentication.repositories(Map.of(source.id(),
                                ModernAccountAuthentication.SourceBinding.direct(source.name(),dao::authenticate)),ignored->false);
                        stage="AUTHENTICATION";var principal=ModernEquipmentRepositoryReadProbe.authenticate(gate,verifier,account,password);
                        stage="ROSTER";var rows=gate.call(5000,scope->dao.characters(principal.accountForSource(source.id())));
                        var repository=ModernGmCommandRepository.jdbc(Map.of(source.id(),new ModernGmCommandRepository.Source(source,dao)));
                        for(var row:rows) {
                            stage="SELECTED_SOURCE_COMMAND_POLICY";
                            var policy=gate.call(5000,scope->repository.character(principal,source.id(),row.objectId())).orElseThrow();
                            if(policy.sourceId()!=source.id()||!policy.character().equals(row)
                                    ||!policy.canonicalAccount().equals(principal.accountForSource(source.id())))
                                throw new IllegalStateException("source or roster changed");
                            decisions[policy.decision().ordinal()]++;characters++;
                        }
                    }
                    if(characters==0)throw new IllegalStateException("no character verified");stage="RETIREMENT";
                    executor.close();if(!executor.awaitTermination(5000))throw new IllegalStateException("worker not retired");
                    var counts=new StringBuilder("{");boolean comma=false;
                    for(var decision:ModernGmCommands.Decision.values()) {
                        if(comma)counts.append(',');comma=true;counts.append('"').append(decision.name()).append("\":").append(decisions[decision.ordinal()]);
                    }
                    counts.append('}');
                    System.out.println("{\"phase\":\"production-gm-command-read\",\"status\":\"OK\",\"sources\":"+config.sources().size()
                            +",\"characters\":"+characters+",\"decisions\":"+counts
                            +",\"source_database_verified\":true,\"database_written\":false,\"bots_created\":0,\"listener_opened\":false,\"cached_grant\":false}");exit=0;
                } finally {executor.close();if(!executor.awaitTermination(5000))throw new IllegalStateException("worker not retired");}
            }
        } catch(Exception failure) {
            String reason=failure instanceof ModernBackendExecutor.BackendException b?b.failure().name():"INVALID_RESULT_OR_SETUP";
            System.out.println("{\"phase\":\"production-gm-command-read\",\"status\":\"FAILED\",\"stage\":\""+stage+"\",\"reason\":\""+reason+"\"}");
        } finally {if(password!=null)Arrays.fill(password,'\0');}
        if(exit!=0)System.exit(exit);
    }
}
