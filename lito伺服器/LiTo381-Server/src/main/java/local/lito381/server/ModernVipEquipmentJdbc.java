package local.lito381.server;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;

/** SELECT-only full VIP supplement, at most two bounded catalog statements on
 * the calling operation's owned connection. Missing VIP rows, denied tables,
 * missing/NULL/non-integral columns are unknown, never zero. A successfully
 * queried absent polymorph row matches PolyAbilityTable's no-added-ability case.
 * The caller must authenticate/authorize its inventory before selecting IDs.
 * No executor invocation, character writes, or historical-template assumption. */
final class ModernVipEquipmentJdbc {
    private ModernVipEquipmentJdbc(){}

    static ModernVipEquipment.Catalog read(Connection connection,ModernBackendScope scope,
            int sourceId,String database,List<Long> catalogIds) throws Exception {
        return read(connection,scope,sourceId,database,catalogIds,false);
    }
    /** Only the full-VIP transition reader requests verified absence. It lets
     * an existing applied ledger be removed; absence grants no new template.
     * A failed/incomplete query still fails, never returning absent/zero rows. */
    static ModernVipEquipment.Catalog read(Connection connection,ModernBackendScope scope,
            int sourceId,String database,List<Long> catalogIds,boolean retainedVip) throws Exception {
        if(connection==null||scope==null||scope!=ModernBackendScope.current()||sourceId<1)throw unavailable();
        ModernPhysicalCombat.identifier(database);
        scope.checkActive();
        try {
            if(!connection.isReadOnly()||catalogIds==null||catalogIds.size()>ModernCharacterData.MAX_ITEMS)throw unavailable();
            var ids=new TreeSet<Long>();
            for(Long id:catalogIds)if(id==null||id<1||!ids.add(id))throw unavailable();
            if(ids.isEmpty())return new ModernVipEquipment.Catalog(sourceId,database,Map.of(),Map.of());
            StringBuilder sql=new StringBuilder("SELECT v.`道具編號` AS catalog_id,v.`類型` AS exclusive_type,")
                    .append("v.`狀態圖編號` AS icon_id,v.`變身編號` AS polymorph_id,")
                    .append("LEFT(e.classname,257) AS executor_name,OCTET_LENGTH(e.classname) AS executor_bytes");
            for(var field:ModernVipEquipment.Field.values())sql.append(",v.`").append(field.vipColumn).append("` AS `").append(field.name()).append('`');
            sql.append(" FROM ").append(table(database,"道具_自訂穿脫能力")).append(" v JOIN ")
                    .append(table(database,"etcitem")).append(" e ON e.item_id=v.`道具編號` WHERE v.`道具編號` IN (")
                    .append(placeholders(ids.size())).append(") ORDER BY v.`道具編號` LIMIT 513");
            var vips=new TreeMap<Long,ModernVipEquipment.Vip>();var polyIds=new TreeSet<Long>();
            try(var query=prepare(connection,scope,sql.toString())) {
                bind(query,ids);
                try(var rows=query.executeQuery()) {
                    long previous=0;
                    while(rows.next()) {
                        scope.checkActive();long id=ModernJdbcNumbers.required(rows,"catalog_id");
                        if(vips.size()==512||id<=previous||!ids.contains(id))throw unavailable();previous=id;
                        String executor=rows.getString("executor_name");
                        if(!"add.VIP".equals(executor)||ModernJdbcNumbers.required(rows,"executor_bytes")!=7)throw unavailable();
                        var vip=new ModernVipEquipment.Vip(id,ModernJdbcNumbers.required(rows,"exclusive_type"),
                                ModernJdbcNumbers.required(rows,"icon_id"),ModernJdbcNumbers.required(rows,"polymorph_id"),abilities(rows,false));
                        vips.put(id,vip);if(vip.polymorphId()>0)polyIds.add(vip.polymorphId());
                    }
                }
            }
            scope.checkActive();var absent=new TreeSet<Long>(ids);absent.removeAll(vips.keySet());
            if(!retainedVip&&!absent.isEmpty())throw unavailable();
            var polys=new TreeMap<Long,ModernVipEquipment.Polymorph>();
            if(!polyIds.isEmpty()) {
                sql=new StringBuilder("SELECT gfxId AS graphics_id,deadExp AS no_death_exp,cancellation AS keep_shape");
                for(var field:ModernVipEquipment.Field.values())if(field.polyColumn!=null)
                    sql.append(",`").append(field.polyColumn).append("` AS `").append(field.name()).append('`');
                sql.append(" FROM ").append(table(database,"poly_變身賦予能力")).append(" WHERE gfxId IN (")
                        .append(placeholders(polyIds.size())).append(") ORDER BY gfxId LIMIT 513");
                try(var query=prepare(connection,scope,sql.toString())) {
                    bind(query,polyIds);
                    try(var rows=query.executeQuery()) {
                        long previous=0;
                        while(rows.next()) {
                            scope.checkActive();long id=ModernJdbcNumbers.required(rows,"graphics_id");
                            if(polys.size()==512||id<=previous||!polyIds.contains(id))throw unavailable();previous=id;
                            polys.put(id,new ModernVipEquipment.Polymorph(id,true,ModernJdbcNumbers.required(rows,"no_death_exp")!=0,
                                    ModernJdbcNumbers.required(rows,"keep_shape")!=0,abilities(rows,true)));
                        }
                    }
                }
                scope.checkActive();
                // This is verified absence only after full, successful iteration.
                for(long id:polyIds)polys.putIfAbsent(id,ModernVipEquipment.Polymorph.absent(id));
            }
            scope.checkActive();return new ModernVipEquipment.Catalog(sourceId,database,vips,polys,absent);
        } catch(Exception failure) {
            scope.checkActive();throw unavailable();
        }
    }
    private static ModernVipEquipment.Abilities abilities(ResultSet rows,boolean poly) throws SQLException {
        var values=new EnumMap<ModernVipEquipment.Field,Long>(ModernVipEquipment.Field.class);
        for(var field:ModernVipEquipment.Field.values()) {
            // Fields not applied by PolyAbilityTable are explicitly inapplicable,
            // not missing-column fallbacks. Every applied field is mandatory.
            long value=poly&&field.polyColumn==null?0:ModernJdbcNumbers.required(rows,field.name());
            if(poly&&field==ModernVipEquipment.Field.AC)value=Math.negateExact(value);
            values.put(field,value);
        }
        return new ModernVipEquipment.Abilities(values);
    }
    private static PreparedStatement prepare(Connection connection,ModernBackendScope scope,String sql) throws Exception {
        scope.checkActive();var query=connection.prepareStatement(sql);
        try {
            query.setMaxRows(513);query.setQueryTimeout((int)Math.max(1L,((long)scope.remainingMillis()+999)/1000));
            scope.checkActive();return query;
        } catch(Exception failure) { try{query.close();}catch(Exception ignored){}throw failure; }
    }
    private static void bind(PreparedStatement query,TreeSet<Long> ids)throws SQLException {
        int index=1;for(long id:ids)query.setLong(index++,id);
    }
    private static String table(String database,String fixedTable){return "`"+database+"`.`"+fixedTable+"`";}
    private static String placeholders(int count){return String.join(",",java.util.Collections.nCopies(count,"?"));}
    private static SQLException unavailable(){return new SQLException("modern VIP source unavailable");}
}
