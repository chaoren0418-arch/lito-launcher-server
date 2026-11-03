package local.lito381.server;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;

/** Explicit, real SELECT-only source qualification. No attack, HP mutation, store or listener. */
public final class ModernPhysicalCombatReadProbe {
    private ModernPhysicalCombatReadProbe() {}
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
                    long characters=0,items=0,growthRows=0,templates=0,configuredProcItems=0,supported=0;
                    long minHp=Long.MAX_VALUE,maxHp=Long.MIN_VALUE,minHit=Long.MAX_VALUE,maxHit=Long.MIN_VALUE;
                    long[] unsupported=new long[ModernPhysicalCombat.UnsupportedReason.values().length];
                    long[] lifetimes=new long[ModernItemLifetime.State.values().length];
                    long sqlEquippedExpired=0,effectiveEquippedExpired=0;
                    for(var source:config.sources()) {
                        var audit=new ModernEquipmentReadProbe.Audit();
                        var dao=new ModernJdbcBackend(source.database(),config.dbConnectMillis(),config.dbNetworkMillis(),
                                (url,properties)->ModernEquipmentReadProbe.inspectOwned(new com.mysql.cj.jdbc.Driver().connect(url,properties),
                                        ModernBackendScope.current(),source.database().database(),source.catalogDatabase(),config.dbNetworkMillis(),null,audit,null));
                        var verifier=ModernAccountAuthentication.repositories(Map.of(source.id(),
                                ModernAccountAuthentication.SourceBinding.direct(source.name(),dao::authenticate)),ignored->false);
                        stage="AUTHENTICATION";var principal=ModernEquipmentRepositoryReadProbe.authenticate(gate,verifier,account,password);
                        stage="ROSTER";var rows=gate.call(5000,scope->dao.characters(principal.accountForSource(source.id())));
                        var provider=ModernEquipmentRepository.physicalJdbc(Map.of(source.id(),new ModernEquipmentRepository.Source(source,dao)));
                        for(var row:rows) {
                            stage="PHYSICAL_SOURCE_QUALIFICATION";
                            var loaded=gate.call(5000,scope->provider.read(principal,source.id(),row.objectId()));
                            var data=loaded.source();
                            if(data.input().equipment().source().sourceId()!=source.id()||!data.input().equipment().source().character().equals(row))throw new IllegalStateException("source changed");
                            var binding=new ModernEquipmentReconciliation.ExpectedBinding(new TournamentPresence.CharacterKey(source.id(),row.objectId()),loaded.ownerBinding());
                            stage="PURE_QUALIFICATION";long evaluatedAt=System.currentTimeMillis();
                            var fighter=ModernPhysicalCombat.qualify(data,binding,Optional.empty(),evaluatedAt);
                            var profile=fighter.profile();
                            var blockingProcs=profile.equipped().stream().filter(flag->flag.equipped()
                                    ||data.input().equipment().items().get(flag.objectId()).source().equipped())
                                    .map(flag->data.input().equipment().items().get(flag.objectId()).source().template().catalogId())
                                    .filter(data.procCatalogIds()::contains).distinct().sorted().toList();
                            System.out.println("{\"phase\":\"production-physical-character\",\"source_id\":"+source.id()
                                    +",\"object_id\":"+row.objectId()+",\"supported\":"+fighter.supported()
                                    +",\"reason\":\""+fighter.unsupportedReason()+"\",\"blocking_proc_catalog_ids\":"+blockingProcs+"}");
                            long expectedAc=Math.addExact(profile.source().persisted().armorClass(),Math.subtractExact(
                                    profile.equipmentEvaluation().total(ModernEquipmentFacts.Stat.AC),
                                    profile.sourceBaseline().total(ModernEquipmentFacts.Stat.AC)));
                            if(profile.equipmentRevision()!=0||profile.effective().armorClass()!=expectedAc
                                    ||profile.effective().initialHp()!=profile.effective().maximumHp()
                                    ||profile.evaluatedAtMillis()!=evaluatedAt
                                    ||data.growth().strength().size()!=127||data.growth().dexterity().size()!=127)throw new IllegalStateException("physical qualification mismatch");
                            // Use the same qualification instant for every observed state. The
                            // reader already anchored each DB-local interval at query START.
                            var views=data.input().equipment().lifetimes().views(profile.source(),evaluatedAt);
                            if(profile.equipped().size()!=views.size())throw new IllegalStateException("lifetime flag mismatch");
                            for(int index=0;index<views.size();index++) {
                                var view=views.get(index);var item=profile.source().inventory().get(index);var flag=profile.equipped().get(index);
                                if(view.itemObjectId()!=item.objectId()||flag.objectId()!=item.objectId())throw new IllegalStateException("lifetime identity mismatch");
                                lifetimes[view.state().ordinal()]=Math.incrementExact(lifetimes[view.state().ordinal()]);
                                if(view.state()==ModernItemLifetime.State.EXPIRED) {
                                    if(item.equipped())sqlEquippedExpired=Math.incrementExact(sqlEquippedExpired);
                                    if(flag.equipped())effectiveEquippedExpired=Math.incrementExact(effectiveEquippedExpired);
                                }
                            }
                            if(effectiveEquippedExpired!=0)throw new IllegalStateException("expired effective equipment");
                            characters++;items+=profile.source().inventory().size();growthRows+=254;templates+=data.templates().size();configuredProcItems+=data.procCatalogIds().size();
                            if(fighter.supported())supported++;else unsupported[fighter.unsupportedReason().ordinal()]++;
                            minHp=Math.min(minHp,profile.effective().maximumHp());maxHp=Math.max(maxHp,profile.effective().maximumHp());
                            minHit=Math.min(minHit,fighter.offense().totalHit());maxHit=Math.max(maxHit,fighter.offense().totalHit());
                        }
                    }
                    if(characters==0)throw new IllegalStateException("no character verified");stage="RETIREMENT";
                    executor.close();if(!executor.awaitTermination(5000))throw new IllegalStateException("worker not retired");
                    StringBuilder reasons=new StringBuilder("{");boolean comma=false;
                    for(var reason:ModernPhysicalCombat.UnsupportedReason.values())if(reason!=ModernPhysicalCombat.UnsupportedReason.NONE) {
                        if(comma)reasons.append(',');comma=true;reasons.append('"').append(reason.name()).append("\":").append(unsupported[reason.ordinal()]);
                    }
                    reasons.append('}');
                    StringBuilder lifetimeStates=new StringBuilder("{");comma=false;long countedLifetimes=0;
                    for(var state:ModernItemLifetime.State.values()) {
                        if(comma)lifetimeStates.append(',');comma=true;
                        lifetimeStates.append('"').append(state.name()).append("\":").append(lifetimes[state.ordinal()]);
                        countedLifetimes=Math.addExact(countedLifetimes,lifetimes[state.ordinal()]);
                    }
                    lifetimeStates.append('}');if(countedLifetimes!=items)throw new IllegalStateException("lifetime count mismatch");
                    System.out.println("{\"phase\":\"production-physical-source\",\"status\":\"OK\",\"ruleset\":\""+ModernPhysicalCombat.RULESET
                            +"\",\"sources\":"+config.sources().size()+",\"characters\":"+characters+",\"items\":"+items
                            +",\"growth_rows_verified\":"+growthRows+",\"template_policies_verified\":"+templates+",\"configured_proc_items\":"+configuredProcItems
                            +",\"supported_fighters\":"+supported+",\"unsupported\":"+reasons+",\"min_effective_hp\":"+minHp+",\"max_effective_hp\":"+maxHp
                            +",\"min_qualified_hit\":"+minHit+",\"max_qualified_hit\":"+maxHit+",\"saved_ac_not_added_twice\":true"
                            +",\"lifetime_states\":"+lifetimeStates+",\"sql_equipped_expired\":"+sqlEquippedExpired
                            +",\"effective_equipped_expired\":"+effectiveEquippedExpired+",\"lifetime_clock\":\"database_local_query_start\""
                            +",\"store_opened\":false,\"attack_executed\":false,\"atomic_generation\":false}");exit=0;
                }finally{executor.close();if(!executor.awaitTermination(5000))throw new IllegalStateException("worker not retired");}
            }
        }catch(Exception failure) {
            String reason=failure instanceof ModernBackendExecutor.BackendException b?b.failure().name():"INVALID_RESULT_OR_SETUP";
            System.out.println("{\"phase\":\"production-physical-source\",\"status\":\"FAILED\",\"stage\":\""+stage+"\",\"reason\":\""+reason+"\"}");
        }finally{if(password!=null)Arrays.fill(password,'\0');}
        if(exit!=0)System.exit(exit);
    }
}
