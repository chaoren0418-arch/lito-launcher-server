package local.lito381.server;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

/** Explicit authenticated SELECT-only diagnosis. No listener, attack, item executor or store.
 * Fixed catalog 88889 only; no credentials, endpoints, account/name or item-object IDs in output.
 * Optional --protection-catalog reads skill68 through the production same-connection provider;
 * it does not grant combat access or execute a proc. The original one-argument path is unchanged. */
public final class ModernEquipmentProcReadProbe {
    private static final long CATALOG=88889;
    private static final List<String> COLUMNS=List.of("id","item_id","enchant_level","proc_group",
            "on_attack_chance","on_hurt_chance","flat_damage","rand_damage","heal_hp","heal_hp_rand",
            "heal_mp","heal_mp_rand","heal_mode","trigger_skill","skill_time_sec","skill_target","gfx_id","gfx_mode");
    private ModernEquipmentProcReadProbe(){}
    static boolean protectionCatalog(String[] args) {
        if(args.length==1)return false;
        if(args.length==2&&"--protection-catalog".equals(args[1]))return true;
        throw new IllegalArgumentException("explicit native configuration and optional catalog audit required");
    }
    record Observation(long objectId,long enchantment,boolean equipped,ModernArmorProcs.Rule rule,String output) {
        @Override public String toString(){return "ArmorProcObservation[redacted]";}
    }

    static List<Observation> read(Connection connection,ModernBackendScope scope,String catalog,
            ModernEquipmentReadProbe.Plan plan)throws Exception {
        ModernPhysicalCombat.identifier(catalog);scope.checkActive();
        if(!connection.isReadOnly())throw new SQLException("proc audit requires read-only connection");
        String columns=String.join(",",COLUMNS.stream().map(name->"p.`"+name+"`").toList());
        String sql="SELECT i.id AS owned_item,i.enchantlvl AS owned_enchantment,i.is_equipped AS owned_equipped,"
                +columns+" FROM characters c JOIN character_items i ON i.char_id=c.objid JOIN `"+catalog
                +"`.`裝備_armor_魔法` p ON p.item_id=i.item_id WHERE c.objid=? AND c.account_name=?"
                +" AND c.Banned=0 AND i.item_id=? ORDER BY i.id,p.id LIMIT 4097";
        var output=new ArrayList<Observation>();long previousItem=0,previousProc=0;
        try(var statement=connection.prepareStatement(sql)) {
            statement.setMaxRows(4097);statement.setQueryTimeout((int)Math.max(1L,((long)scope.remainingMillis()+999)/1000));
            statement.setLong(1,plan.objectId());statement.setString(2,plan.canonical());statement.setLong(3,CATALOG);
            try(var rows=statement.executeQuery()) {
                while(rows.next()) {
                    scope.checkActive();long item=ModernJdbcNumbers.required(rows,"owned_item"),id=ModernJdbcNumbers.required(rows,"id");
                    if(output.size()==4096||item<1||id<1||item<previousItem||item==previousItem&&id<=previousProc
                            ||ModernJdbcNumbers.required(rows,"item_id")!=CATALOG)throw new SQLException("proc audit row bound/identity");
                    previousItem=item;previousProc=id;
                    long equipped=ModernJdbcNumbers.required(rows,"owned_equipped");
                    if(equipped!=0&&equipped!=1)throw new SQLException("proc audit equipped flag");
                    long enchantment=ModernJdbcNumbers.required(rows,"owned_enchantment");
                    var text=new StringBuilder("{\"source_id\":").append(plan.sourceId())
                            .append(",\"saved_enchantment\":").append(enchantment)
                            .append(",\"sql_equipped\":").append(equipped==1).append(",\"proc\":{");
                    boolean comma=false;var values=new long[COLUMNS.size()];int index=0;
                    for(var column:COLUMNS) {
                        if(comma)text.append(',');comma=true;
                        values[index]=ModernJdbcNumbers.required(rows,column);
                        text.append('"').append(column).append("\":").append(values[index++]);
                    }
                    var rule=new ModernArmorProcs.Rule(values[0],values[1],values[2],values[3],values[4],values[5],values[6],values[7],values[8],
                            values[9],values[10],values[11],values[12],values[13],values[14],values[15],values[16],values[17]);
                    output.add(new Observation(item,enchantment,equipped==1,rule,text.append("}}").toString()));
                }
            }
        }
        scope.checkActive();return List.copyOf(output);
    }

    public static void main(String[] args) {
        int exit=2;char[] password=null;String stage="CONFIGURATION";
        try {
            boolean protection=protectionCatalog(args);
            var file=Path.of(args[0]);var account=System.getenv("LITO_MODERN_PROBE_ACCOUNT");
            var secret=System.getenv("LITO_MODERN_PROBE_PASSWORD");
            if(account==null||secret==null)throw new IllegalArgumentException("explicit test account required");password=secret.toCharArray();
            try(var settings=ModernDatabaseReadProbe.load(file);
                var config=ModernDatabaseReadProbe.modernConfig(settings,ModernCharacterDataReadProbe.catalogDatabase(file));
                var executor=new ModernBackendExecutor(1,1);var gate=executor.openSession()) {
                try {
                    long count=0,characters=0;
                    for(var source:config.sources()) {
                        var plan=new AtomicReference<ModernEquipmentReadProbe.Plan>();
                        var observed=new AtomicReference<List<Observation>>();var audit=new ModernEquipmentReadProbe.Audit();
                        var dao=new ModernJdbcBackend(source.database(),config.dbConnectMillis(),config.dbNetworkMillis(),
                                (url,properties)->ModernEquipmentReadProbe.inspectOwned(new com.mysql.cj.jdbc.Driver().connect(url,properties),
                                        ModernBackendScope.current(),source.database().database(),source.catalogDatabase(),config.dbNetworkMillis(),plan.get(),audit,
                                        (connection,scope,catalog,current,ignored)->{observed.set(read(connection,scope,catalog,current));return null;}));
                        var verifier=ModernAccountAuthentication.repositories(Map.of(source.id(),
                                ModernAccountAuthentication.SourceBinding.direct(source.name(),dao::authenticate)),ignored->false);
                        stage="AUTHENTICATION";var principal=ModernEquipmentRepositoryReadProbe.authenticate(gate,verifier,account,password);
                        stage="ROSTER";var roster=gate.call(5000,scope->dao.characters(principal.accountForSource(source.id())));
                        for(var row:roster) {
                            stage="QUALIFIED_CHARACTER";observed.set(null);
                            plan.set(new ModernEquipmentReadProbe.Plan(source.id(),principal.accountForSource(source.id()),row));
                            try {
                                ModernArmorProtectionCatalog.BoundSource bundle=protection
                                        ?gate.call(5000,scope->dao.armorProtectionCharacter(source.id(),principal.accountForSource(source.id()),row.objectId(),
                                                source.catalogDatabase(),false,false,false,false,false,false)).orElseThrow():null;
                                var data=bundle!=null?bundle.physical():gate.call(5000,scope->dao.physicalCharacter(source.id(),principal.accountForSource(source.id()),row.objectId(),source.catalogDatabase())).orElseThrow();
                                if(!data.input().equipment().source().character().equals(row))throw new IllegalStateException("roster changed");
                                var entries=observed.get();if(entries==null)throw new IllegalStateException("audit not observed");
                                for(var entry:entries) {
                                    var item=data.input().equipment().items().get(entry.objectId());
                                    if(item==null||item.source().enchantment()!=entry.enchantment()||item.source().equipped()!=entry.equipped()
                                            ||data.armorProcs()==null||!data.armorProcs().byItem().getOrDefault(CATALOG,List.of()).contains(entry.rule()))
                                        throw new IllegalStateException("production proc facts differ from audit");
                                }
                                if(bundle!=null) {
                                    if(bundle.skill().status()!=ModernArmorProtectionCatalog.Status.QUALIFIED)throw new IllegalStateException("unsupported protection catalog");
                                    System.out.println("{\"phase\":\"armor-protection-catalog\",\"source_id\":"+source.id()
                                            +",\"skill_id\":68,\"status\":\"QUALIFIED\",\"same_owned_connection\":true,\"runtime_grant\":false}");
                                }
                            }finally{plan.set(null);}
                            var entries=observed.get();if(entries==null)throw new IllegalStateException("audit not observed");
                            for(var entry:entries)System.out.println(entry.output());count+=entries.size();characters++;
                        }
                    }
                    if(characters==0)throw new IllegalStateException("empty audit");
                    stage="RETIREMENT";executor.close();if(!executor.awaitTermination(5000))throw new IllegalStateException("worker retained");
                    System.out.println("{\"phase\":\"armor-proc-audit\",\"status\":\"OK\",\"characters\":"+characters+",\"rows\":"+count
                            +",\"production_facts_matched\":true,\"attack_executed\":false,\"sql_written\":false,\"atomic_generation\":false}");exit=0;
                }finally{executor.close();if(!executor.awaitTermination(5000))throw new IllegalStateException("worker retained");}
            }
        }catch(Exception failure) {
            System.out.println("{\"phase\":\"armor-proc-audit\",\"status\":\"FAILED\",\"stage\":\""+stage+"\"}");
        }finally{if(password!=null)Arrays.fill(password,'\0');}
        if(exit!=0)System.exit(exit);
    }
}
