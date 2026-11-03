package local.lito381.server;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.Map;

/** Opt-in, SELECT-only verification of the production provider. No store/listener/native entry point. */
public final class ModernEquipmentRepositoryReadProbe {
    private ModernEquipmentRepositoryReadProbe() {}
    public static void main(String[] args) {
        int exit=2;
        try {
            if (args.length!=1) throw new IllegalArgumentException("one explicit configuration required");
            Path file=Path.of(args[0]);
            String account=System.getenv("LITO_MODERN_PROBE_ACCOUNT");
            char[] password=System.getenv("LITO_MODERN_PROBE_PASSWORD").toCharArray();
            try (var settings=ModernDatabaseReadProbe.load(file);
                 var config=ModernDatabaseReadProbe.modernConfig(settings,ModernCharacterDataReadProbe.catalogDatabase(file));
                 var executor=new ModernBackendExecutor(1,1);var gate=executor.openSession()) {
                try {
                    int count=0;
                    for (var source:config.sources()) {
                        var dao=new ModernJdbcBackend(source.database(),config.dbConnectMillis(),config.dbNetworkMillis());
                        var verifier=ModernAccountAuthentication.repositories(Map.of(source.id(),
                                ModernAccountAuthentication.SourceBinding.direct(source.name(),dao::authenticate)),ignored->false);
                        var principal=authenticate(gate,verifier,account,password);
                        var rows=gate.call(5000,scope->dao.characters(principal.accountForSource(source.id())));
                        var provider=ModernEquipmentRepository.jdbc(Map.of(source.id(),new ModernEquipmentRepository.Source(source,dao)));
                        for (var row:rows) {
                            // Uses the production provider/DAO cleanup, not a connector
                            // callback which replaces the actual equipment read.
                            var loaded=gate.call(5000,scope->provider.read(principal,source.id(),row.objectId()));
                            var qualified=loaded.qualified();
                            if (qualified.source().character().objectId()!=row.objectId() || qualified.source().sourceId()!=source.id()) {
                                throw new IllegalStateException("source mismatch");
                            }
                            var evaluated=ModernEquipmentEvaluator.evaluate(qualified,Map.of(),System.currentTimeMillis());
                            ModernEquipmentBinding.catalogGeneration(qualified.catalog());
                            System.out.println("{\"phase\":\"production-equipment-provider\",\"source_id\":"+source.id()
                                    +",\"object_id\":"+row.objectId()+",\"items\":"+qualified.items().size()+",\"equipped\":"+evaluated.equippedRows()
                                    +",\"hp_modifier\":"+evaluated.total(ModernEquipmentFacts.Stat.MAX_HP)+",\"ac_modifier\":"+evaluated.armorClassModifier()
                                    +",\"status\":\"OK\"}");
                            count++;
                        }
                    }
                    if (count==0) throw new IllegalStateException("no character verified");
                    exit=0;
                } finally {
                    executor.close();
                    if (!executor.awaitTermination(5000)) throw new IllegalStateException("provider worker not retired");
                }
            } finally { Arrays.fill(password,'\0'); }
        } catch (Exception failure) {
            // No credentials/SQL/path or raw driver exception leaves this probe.
            String reason=failure instanceof ModernBackendExecutor.BackendException backend?backend.failure().name():"INVALID_RESULT_OR_SETUP";
            System.out.println("{\"phase\":\"production-equipment-provider\",\"status\":\"FAILED\",\"reason\":\""+reason+"\"}");
            exit=2;
        }
        if (exit!=0) System.exit(exit);
    }

    static ModernAccountAuthentication.Principal authenticate(ModernBackendExecutor.SessionGate gate,
            ModernAccountAuthentication.CredentialVerifier verifier,String account,char[] sourcePassword) throws Exception {
        // verify() consumes/wipes its argument. Each source gets a new owned
        // attempt; queued cancellation also wipes it through Work.close().
        char[] attempt=sourcePassword.clone();
        return gate.call(5000,new ModernBackendExecutor.Work<ModernAccountAuthentication.Principal>() {
            @Override public ModernAccountAuthentication.Principal run(ModernBackendScope scope) throws Exception {
                return ModernAccountAuthentication.verify(verifier,account,attempt);
            }
            @Override public void close() { Arrays.fill(attempt,'\0'); }
        });
    }
}
