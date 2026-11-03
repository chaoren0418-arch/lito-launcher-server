package local.lito381.server;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;

/** Explicit test-classpath SELECT-only qualification; no cast, MP/HP mutation, store or listener. */
public final class ModernMagicCombatReadProbe {
    private ModernMagicCombatReadProbe() {}
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
                    long characters=0,items=0,growthRows=0,supported=0,learned=0,qualifiedArrows=0,unsupportedArrows=0;
                    long minInt=Long.MAX_VALUE,maxInt=Long.MIN_VALUE,minSp=Long.MAX_VALUE,maxSp=Long.MIN_VALUE;
                    long minMr=Long.MAX_VALUE,maxMr=Long.MIN_VALUE;long[] costs=new long[3];
                    long[] unsupported=new long[ModernMagicCombat.UnsupportedReason.values().length];
                    for(var source:config.sources()) {
                        var audit=new ModernEquipmentReadProbe.Audit();
                        var dao=new ModernJdbcBackend(source.database(),config.dbConnectMillis(),config.dbNetworkMillis(),
                                (url,properties)->ModernEquipmentReadProbe.inspectOwned(new com.mysql.cj.jdbc.Driver().connect(url,properties),
                                        ModernBackendScope.current(),source.database().database(),source.catalogDatabase(),config.dbNetworkMillis(),null,audit,null));
                        var verifier=ModernAccountAuthentication.repositories(Map.of(source.id(),
                                ModernAccountAuthentication.SourceBinding.direct(source.name(),dao::authenticate)),ignored->false);
                        stage="AUTHENTICATION";var principal=ModernEquipmentRepositoryReadProbe.authenticate(gate,verifier,account,password);
                        stage="ROSTER";var rows=gate.call(5000,scope->dao.characters(principal.accountForSource(source.id())));
                        var provider=ModernEquipmentRepository.physicalJdbc(Map.of(source.id(),new ModernEquipmentRepository.Source(source,dao)),true);
                        for(var row:rows) {
                            stage="MAGIC_SOURCE_QUALIFICATION";
                            var loaded=gate.call(5000,scope->provider.read(principal,source.id(),row.objectId()));
                            var data=loaded.source();
                            if(data.input().equipment().source().sourceId()!=source.id()||!data.input().equipment().source().character().equals(row)
                                    ||data.magicGrowth()==null||data.magicGrowth().sourceId()!=source.id()
                                    ||!data.magicGrowth().database().equals(source.catalogDatabase()))throw new IllegalStateException("source mismatch");
                            stage="LEARNED_SKILL_QUALIFICATION";
                            var skills=gate.call(5000,scope->dao.skillsCharacter(source.id(),principal.accountForSource(source.id()),row.objectId(),source.catalogDatabase())).orElseThrow();
                            if(skills.sourceId()!=source.id()||!skills.character().equals(row))throw new IllegalStateException("skill source changed");
                            var binding=new ModernEquipmentReconciliation.ExpectedBinding(new TournamentPresence.CharacterKey(source.id(),row.objectId()),loaded.ownerBinding());
                            stage="PURE_QUALIFICATION";long evaluatedAt=System.currentTimeMillis();
                            var physical=ModernPhysicalCombat.qualify(data,binding,Optional.empty(),evaluatedAt);var fighter=physical.magic();
                            if(fighter==null||fighter.profile()!=physical.profile()||fighter.profile().evaluatedAtMillis()!=evaluatedAt
                                    ||data.magicGrowth().wisdom().size()!=50||fighter.mpCost()<1||fighter.mpCost()>3||fighter.range()!=10)
                                throw new IllegalStateException("magic qualification mismatch");
                            characters++;items+=fighter.profile().source().inventory().size();growthRows+=50;learned+=skills.skills().size();
                            if(fighter.supported())supported++;else unsupported[fighter.unsupportedReason().ordinal()]++;
                            costs[(int)fighter.mpCost()-1]++;
                            if(skills.lightArrow().isPresent())qualifiedArrows++;
                            if(skills.skills().stream().anyMatch(s->s.skillId()==4&&s.support()==ModernSkills.Support.UNSUPPORTED_TEMPLATE))unsupportedArrows++;
                            minInt=Math.min(minInt,fighter.offense().effectiveIntelligence());maxInt=Math.max(maxInt,fighter.offense().effectiveIntelligence());
                            minSp=Math.min(minSp,fighter.offense().itemSpellPower());maxSp=Math.max(maxSp,fighter.offense().itemSpellPower());
                            minMr=Math.min(minMr,fighter.defense().magicResistance());maxMr=Math.max(maxMr,fighter.defense().magicResistance());
                        }
                    }
                    if(characters==0)throw new IllegalStateException("no character verified");stage="RETIREMENT";
                    executor.close();if(!executor.awaitTermination(5000))throw new IllegalStateException("worker not retired");
                    var reasons=new StringBuilder("{");boolean comma=false;
                    for(var reason:ModernMagicCombat.UnsupportedReason.values())if(reason!=ModernMagicCombat.UnsupportedReason.NONE) {
                        if(comma)reasons.append(',');comma=true;reasons.append('"').append(reason.name()).append("\":").append(unsupported[reason.ordinal()]);
                    }
                    reasons.append('}');
                    System.out.println("{\"phase\":\"production-magic-source\",\"status\":\"OK\",\"ruleset\":\""+ModernMagicCombat.RULESET
                            +"\",\"sources\":"+config.sources().size()+",\"characters\":"+characters+",\"items\":"+items
                            +",\"wisdom_rows_verified\":"+growthRows+",\"learned_skills\":"+learned+",\"qualified_light_arrow_characters\":"+qualifiedArrows
                            +",\"unsupported_light_arrow_templates\":"+unsupportedArrows+",\"supported_magic_fighters\":"+supported+",\"unsupported\":"+reasons
                            +",\"mp_cost_1\":"+costs[0]+",\"mp_cost_2\":"+costs[1]+",\"mp_cost_3\":"+costs[2]
                            +",\"min_effective_int\":"+minInt+",\"max_effective_int\":"+maxInt+",\"min_item_sp\":"+minSp+",\"max_item_sp\":"+maxSp
                            +",\"min_effective_mr\":"+minMr+",\"max_effective_mr\":"+maxMr
                            +",\"store_opened\":false,\"cast_executed\":false,\"atomic_generation\":false}");exit=0;
                } finally {executor.close();if(!executor.awaitTermination(5000))throw new IllegalStateException("worker not retired");}
            }
        } catch(Exception failure) {
            String reason=failure instanceof ModernBackendExecutor.BackendException b?b.failure().name():"INVALID_RESULT_OR_SETUP";
            System.out.println("{\"phase\":\"production-magic-source\",\"status\":\"FAILED\",\"stage\":\""+stage+"\",\"reason\":\""+reason+"\"}");
        } finally {if(password!=null)Arrays.fill(password,'\0');}
        if(exit!=0)System.exit(exit);
    }
}
