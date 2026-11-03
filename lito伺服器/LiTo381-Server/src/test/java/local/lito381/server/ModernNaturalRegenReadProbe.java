package local.lito381.server;

import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.sql.Connection;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/** TEST CLASSPATH ONLY. Explicit source-only/readonly-override observation.
 * Reuses the bounded SELECT-only physical source pipeline and its new opt-in
 * CON table. Never starts a listener/native Main, creates a store, mutates SQL,
 * or treats this multi-query observation as a battle-admission authority ACK.
 * Arguments/environment match ModernInventoryWeightReadProbe except no weight
 * rate is required. Credentials, database/catalog names and exceptions are
 * never emitted. Every operation retains the backend's single socket owner.
 */
public final class ModernNaturalRegenReadProbe {
    record Summary(int sourceId,long objectId,int sourceItems,long equipmentRevision,
                   ModernInventoryWeightReadProbe.OverrideMode overrideMode,ModernNaturalRegen.Profile profile) {
        @Override public String toString(){return "NaturalRegenProbeSummary[redacted]";}
    }
    public static void main(String[] args) {
        var output=new PrintWriter(new OutputStreamWriter(System.out,StandardCharsets.UTF_8),true);
        ModernDatabaseReadProbe.Settings settings=null;ModernServerConfig config=null;
        ModernInventoryWeightReadProbe.OverrideInput overrides=null;ModernBackendExecutor executor=null;
        boolean retired=true;int exit=2;
        try {
            if(args.length!=1)throw new IllegalArgumentException();
            Path main=Path.of(args[0]);settings=ModernDatabaseReadProbe.load(main);
            config=ModernDatabaseReadProbe.modernConfig(settings,ModernCharacterDataReadProbe.catalogDatabase(main));
            overrides=ModernInventoryWeightReadProbe.overrideInput(System.getenv("LITO_MODERN_WEIGHT_PROBE_OVERRIDE_DIRECTORY"),
                    System.getenv("LITO_MODERN_WEIGHT_PROBE_NO_OVERRIDES"));
            final var readonly=overrides;
            String account=System.getenv("LITO_MODERN_PROBE_ACCOUNT"),password=System.getenv("LITO_MODERN_PROBE_PASSWORD");
            if(!ModernCharacterDataReadProbe.boundedText(account,64,false)||!ModernCharacterDataReadProbe.boundedText(password,256,true))
                throw new IllegalArgumentException();
            executor=new ModernBackendExecutor(1,0);retired=false;boolean passed=true;
            sources:for(var source:config.sources()) {
                var auditRef=new AtomicReference<>(new ModernInventoryWeightReadProbe.Audit());
                var inspection=new AtomicReference<ModernInventoryWeightReadProbe.Inspection>();
                int network=config.dbNetworkMillis();
                var backend=new ModernJdbcBackend(source.database(),config.dbConnectMillis(),network,(url,properties)->{
                    Connection connection=new com.mysql.cj.jdbc.Driver().connect(url,properties);
                    if(connection==null)throw new java.sql.SQLException("regen observation unavailable");
                    return ModernInventoryWeightReadProbe.inspectOwned(connection,ModernBackendScope.current(),source.database().database(),
                            network,auditRef.get(),inspection.get());
                });
                var verifier=ModernAccountAuthentication.repositories(Map.of(source.id(),
                        ModernAccountAuthentication.SourceBinding.direct(source.name(),backend::authenticate)),ignored->false);
                try(var gate=executor.openSession()) {
                    ModernInventoryWeightReadProbe.SourcePlan plan=null;
                    char[] owned=password.toCharArray();
                    try {
                        plan=gate.call(5_000,new ModernBackendExecutor.Work<ModernInventoryWeightReadProbe.SourcePlan>() {
                            @Override public ModernInventoryWeightReadProbe.SourcePlan run(ModernBackendScope scope)throws Exception {
                                var principal=ModernAccountAuthentication.verify(verifier,account,owned);
                                return new ModernInventoryWeightReadProbe.SourcePlan(principal,backend.characters(principal.accountForSource(source.id())));
                            }
                            @Override public void close(){Arrays.fill(owned,'\0');}
                        });
                    }catch(ModernBackendExecutor.BackendException failure){passed=false;output.println(failure(source.id(),0,"AUTHENTICATION_OR_ROSTER"));}
                    if(!quiescent(executor)){passed=false;output.println(failure(source.id(),0,"WORKER_NOT_RETIRED"));break;}
                    if(plan==null)continue;
                    final var authorized=plan;
                    output.println("{\"phase\":\"regen-roster\",\"source_id\":"+source.id()+",\"character_count\":"+plan.roster().size()+"}");
                    for(var row:plan.roster()) {
                        auditRef.set(new ModernInventoryWeightReadProbe.Audit());var summary=new AtomicReference<Summary>();
                        inspection.set((connection,scope)->summary.set(readPipeline(connection,scope,source,authorized.principal(),row,readonly)));
                        boolean complete=false;
                        try {
                            gate.call(5_000,scope->{
                                try {
                                    if(!backend.characters(authorized.principal().accountForSource(source.id())).equals(authorized.roster())||summary.get()==null)
                                        throw new IllegalArgumentException("source changed during observation");
                                    readonly.verify();scope.checkActive();return null;
                                }finally{inspection.set(null);}
                            });complete=true;
                        }catch(ModernBackendExecutor.BackendException failure){passed=false;output.println(failure(source.id(),row.objectId(),"SOURCE_QUALIFICATION"));}
                        if(!quiescent(executor)){passed=false;output.println(failure(source.id(),row.objectId(),"WORKER_NOT_RETIRED"));break sources;}
                        inspection.set(null);
                        if(complete)output.println(render(summary.get()));
                    }
                }
            }
            exit=passed?0:1;
        }catch(Throwable failure){output.println(failure(0,0,"CONFIGURATION_OR_STARTUP"));}
        finally {
            if(executor!=null)try{executor.close();retired=executor.awaitTermination(3_000);}catch(Throwable failure){retired=false;}
            if(retired) {
                try{try{if(overrides!=null)overrides.close();}finally{try{if(config!=null)config.close();}finally{if(settings!=null)settings.close();}}}
                catch(Throwable failure){output.println(failure(0,0,"OWNER_CLOSE_FAILED"));exit=2;}
            }else{output.println(failure(0,0,"WORKER_NOT_RETIRED"));exit=2;}
        }
        output.flush();if(exit!=0)System.exit(exit);
    }
    static Summary readPipeline(Connection connection,ModernBackendScope scope,ModernServerConfig.SourceConfig source,
            ModernAccountAuthentication.Principal principal,ModernApplicationProtocol.CharacterRow expected,
            ModernInventoryWeightReadProbe.OverrideInput overrides)throws Exception {
        var physical=ModernPhysicalCombatSourceJdbc.read(connection,scope,source.id(),principal.accountForSource(source.id()),
                expected.objectId(),source.catalogDatabase(),false,false,true).orElseThrow();
        var q=physical.input().equipment();if(!q.source().character().equals(expected))throw new IllegalArgumentException();
        var key=new TournamentPresence.CharacterKey(source.id(),expected.objectId());
        var binding=new ModernEquipmentReconciliation.ExpectedBinding(key,ModernEquipmentBinding.ownerBinding(principal,source));
        var stored=overrides.read(key,scope);
        var profile=ModernCombatProfile.qualify(physical.input(),binding,stored,System.currentTimeMillis(),ModernCombatProfile.VitalPolicy.FULL_NEW_MATCH);
        var regen=ModernNaturalRegen.qualify(physical,profile,physical.regenGrowth());
        scope.checkActive();return new Summary(source.id(),expected.objectId(),q.source().inventory().size(),
                stored.map(ModernEquipmentOverrideStore.Stored::revision).orElse(0L),overrides.mode(),regen);
    }
    static String render(Summary row) {
        var p=row.profile();var v=p.values();
        return "{\"phase\":\"regen-read\",\"status\":\""+(p.supported()?"OK":"UNSUPPORTED")
                +"\",\"observation\":\"readonly_not_admission\",\"consistency\":\"multi_query_not_atomic_generation\""
                +",\"policy\":\""+ModernNaturalRegen.RULESET+"\",\"override_mode\":\""+row.overrideMode()+"\",\"source_id\":"+row.sourceId()
                +",\"object_id\":"+row.objectId()+",\"source_items\":"+row.sourceItems()+",\"equipment_revision\":"+row.equipmentRevision()
                +",\"constitution_rows\":50,\"effective_con\":"+v.constitution()+",\"con_hpr_maximum\":"+v.constitutionHprMaximum()
                +",\"original_con\":"+v.originalConstitution()+",\"equipment_hpr\":"+v.equipmentHpRegen()+",\"equipment_mpr\":"+v.equipmentMpRegen()
                +",\"hp_min\":"+ModernNaturalRegen.hpAmount(v,1)+",\"hp_max\":"+ModernNaturalRegen.hpAmount(v,v.constitutionHprMaximum())
                +",\"mp_amount\":"+ModernNaturalRegen.mpAmount(v)+",\"food225\":"+v.food()+",\"food_eligible\":"+v.fed()
                +",\"hp_threshold\":"+ModernNaturalRegen.hpThreshold(v.level(),v.type())+",\"mp_threshold\":"+ModernNaturalRegen.mpThreshold(v.type())
                +",\"unsupported_reason\":\""+p.unsupportedReason()+"\"}";
    }
    private static String failure(int source,long object,String stage){return "{\"phase\":\"regen-read\",\"status\":\"FAILED\",\"source_id\":"+source
            +",\"object_id\":"+object+",\"reason\":\""+stage+"\"}";}
    private static boolean quiescent(ModernBackendExecutor executor)throws InterruptedException {
        long end=System.nanoTime()+TimeUnit.SECONDS.toNanos(3);
        while(!executor.isQuiescent()){if(System.nanoTime()-end>=0)return false;Thread.sleep(5);}return true;
    }
    private ModernNaturalRegenReadProbe(){}
}
