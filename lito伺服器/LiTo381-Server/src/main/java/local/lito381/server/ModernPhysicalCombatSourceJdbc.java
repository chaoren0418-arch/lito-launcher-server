package local.lito381.server;

import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * Read-only NEW_MATCH_MELEE_V1 supplement. Reuses the existing single item/
 * character SELECT and Catalog reader, on their SAME operation-owned connection.
 * Two complete 127-row growth tables, up to three exact template-policy queries
 * and two proc-presence queries add at most SEVEN bounded statements. Known
 * Carried VIPs add at most two complete VIP/polymorph catalog queries, including
 * unequipped items needed by subsequent owner-qualified loadout transitions;
 * carried armor with configured procs adds one complete proc-row query. No global
 * cache, another connection, optional-column zero, or MyISAM atomicity claim.
 * Missing/inaccessible proc tables are unknown, never proof of no proc.
 */
final class ModernPhysicalCombatSourceJdbc {
    private static final int MAX_PROC_ROWS=1024;
    private ModernPhysicalCombatSourceJdbc() {}

    static Optional<ModernPhysicalCombat.Source> read(Connection connection,ModernBackendScope scope,
            int sourceId,String canonicalAccount,long objectId,String catalogDatabase) throws Exception {
        return read(connection,scope,sourceId,canonicalAccount,objectId,catalogDatabase,false);
    }
    static Optional<ModernPhysicalCombat.Source> read(Connection connection,ModernBackendScope scope,
            int sourceId,String canonicalAccount,long objectId,String catalogDatabase,boolean magicEnabled) throws Exception {
        return read(connection,scope,sourceId,canonicalAccount,objectId,catalogDatabase,magicEnabled,false);
    }
    static Optional<ModernPhysicalCombat.Source> read(Connection connection,ModernBackendScope scope,
            int sourceId,String canonicalAccount,long objectId,String catalogDatabase,boolean magicEnabled,boolean bowEnabled) throws Exception {
        return read(connection,scope,sourceId,canonicalAccount,objectId,catalogDatabase,magicEnabled,bowEnabled,false);
    }
    static Optional<ModernPhysicalCombat.Source> read(Connection connection,ModernBackendScope scope,
            int sourceId,String canonicalAccount,long objectId,String catalogDatabase,boolean magicEnabled,boolean bowEnabled,boolean regenEnabled) throws Exception {
        return read(connection,scope,sourceId,canonicalAccount,objectId,catalogDatabase,magicEnabled,bowEnabled,regenEnabled,false);
    }
    static Optional<ModernPhysicalCombat.Source> read(Connection connection,ModernBackendScope scope,
            int sourceId,String canonicalAccount,long objectId,String catalogDatabase,boolean magicEnabled,boolean bowEnabled,boolean regenEnabled,boolean honorEnabled) throws Exception {
        return read(connection,scope,sourceId,canonicalAccount,objectId,catalogDatabase,magicEnabled,bowEnabled,regenEnabled,honorEnabled,false);
    }
    static Optional<ModernPhysicalCombat.Source> read(Connection connection,ModernBackendScope scope,
            int sourceId,String canonicalAccount,long objectId,String catalogDatabase,boolean magicEnabled,boolean bowEnabled,boolean regenEnabled,boolean honorEnabled,boolean equipmentHasteEnabled) throws Exception {
        return read(connection,scope,sourceId,canonicalAccount,objectId,catalogDatabase,magicEnabled,bowEnabled,regenEnabled,honorEnabled,equipmentHasteEnabled,false);
    }
    static Optional<ModernPhysicalCombat.Source> read(Connection connection,ModernBackendScope scope,
            int sourceId,String canonicalAccount,long objectId,String catalogDatabase,boolean magicEnabled,boolean bowEnabled,boolean regenEnabled,boolean honorEnabled,boolean equipmentHasteEnabled,boolean playerHealingEnabled) throws Exception {
        return read(connection,scope,sourceId,canonicalAccount,objectId,catalogDatabase,magicEnabled,bowEnabled,regenEnabled,honorEnabled,equipmentHasteEnabled,playerHealingEnabled,false);
    }
    static Optional<ModernPhysicalCombat.Source> read(Connection connection,ModernBackendScope scope,
            int sourceId,String canonicalAccount,long objectId,String catalogDatabase,boolean magicEnabled,boolean bowEnabled,boolean regenEnabled,boolean honorEnabled,boolean equipmentHasteEnabled,boolean playerHealingEnabled,boolean retainedVip) throws Exception {
        requireHealingMagic(magicEnabled,playerHealingEnabled);
        ModernPhysicalCombat.identifier(catalogDatabase);scope.checkActive();
        var source=ModernCombatSourceJdbc.read(connection,scope,sourceId,canonicalAccount,objectId,catalogDatabase);
        if(source.isEmpty())return Optional.empty();
        var facts=source.orElseThrow();var items=facts.equipment().instances();
        var catalog=ModernEquipmentCatalogJdbc.read(connection,scope,sourceId,catalogDatabase,
                ModernEquipmentFacts.requiredLookups(items),ModernEquipmentFacts.requiredSetIds(items));
        return Optional.of(qualify(connection,scope,facts.qualify(catalog),magicEnabled,bowEnabled,regenEnabled,honorEnabled,equipmentHasteEnabled,playerHealingEnabled,retainedVip));
    }

    /** Package seam for an already-qualified bundle; caller still owns the connection/scope. */
    static ModernPhysicalCombat.Source qualify(Connection connection,ModernBackendScope scope,
            ModernCombatProfile.Input input) throws Exception {
        return qualify(connection,scope,input,false);
    }
    static ModernPhysicalCombat.Source qualify(Connection connection,ModernBackendScope scope,
            ModernCombatProfile.Input input,boolean magicEnabled) throws Exception {
        return qualify(connection,scope,input,magicEnabled,false);
    }
    static ModernPhysicalCombat.Source qualify(Connection connection,ModernBackendScope scope,
            ModernCombatProfile.Input input,boolean magicEnabled,boolean bowEnabled) throws Exception {
        return qualify(connection,scope,input,magicEnabled,bowEnabled,false);
    }
    static ModernPhysicalCombat.Source qualify(Connection connection,ModernBackendScope scope,
            ModernCombatProfile.Input input,boolean magicEnabled,boolean bowEnabled,boolean regenEnabled) throws Exception {
        return qualify(connection,scope,input,magicEnabled,bowEnabled,regenEnabled,false);
    }
    static ModernPhysicalCombat.Source qualify(Connection connection,ModernBackendScope scope,
            ModernCombatProfile.Input input,boolean magicEnabled,boolean bowEnabled,boolean regenEnabled,boolean honorEnabled) throws Exception {
        return qualify(connection,scope,input,magicEnabled,bowEnabled,regenEnabled,honorEnabled,false);
    }
    static ModernPhysicalCombat.Source qualify(Connection connection,ModernBackendScope scope,
            ModernCombatProfile.Input input,boolean magicEnabled,boolean bowEnabled,boolean regenEnabled,boolean honorEnabled,boolean equipmentHasteEnabled) throws Exception {
        return qualify(connection,scope,input,magicEnabled,bowEnabled,regenEnabled,honorEnabled,equipmentHasteEnabled,false);
    }
    static ModernPhysicalCombat.Source qualify(Connection connection,ModernBackendScope scope,
            ModernCombatProfile.Input input,boolean magicEnabled,boolean bowEnabled,boolean regenEnabled,boolean honorEnabled,boolean equipmentHasteEnabled,boolean playerHealingEnabled) throws Exception {
        return qualify(connection,scope,input,magicEnabled,bowEnabled,regenEnabled,honorEnabled,equipmentHasteEnabled,playerHealingEnabled,false);
    }
    static ModernPhysicalCombat.Source qualify(Connection connection,ModernBackendScope scope,
            ModernCombatProfile.Input input,boolean magicEnabled,boolean bowEnabled,boolean regenEnabled,boolean honorEnabled,boolean equipmentHasteEnabled,boolean playerHealingEnabled,boolean retainedVip) throws Exception {
        requireHealingMagic(magicEnabled,playerHealingEnabled);
        scope.checkActive();
        if(connection==null||input==null||!input.weaponsQualified()||ModernBackendScope.current()!=scope)throw unavailable();
        String database=input.equipment().catalog().resourceDatabase();ModernPhysicalCombat.identifier(database);
        try {
            if(!connection.isReadOnly())throw unavailable();
            var growth=new ModernPhysicalCombat.Growth(input.equipment().source().sourceId(),database,
                    growth(connection,scope,database,"角色_力量成長"),growth(connection,scope,database,"角色_敏捷成長"));
            var policies=new LinkedHashMap<Long,ModernPhysicalCombat.TemplatePolicy>();
            var all=new java.util.TreeSet<Long>();
            for(var kind:ModernCharacterData.ItemKind.values()) {
                var ids=new java.util.TreeSet<Long>();
                for(var item:input.equipment().source().inventory())if(item.template().kind()==kind)ids.add(item.template().catalogId());
                if(ids.isEmpty())continue;all.addAll(ids);
                policies.putAll(templates(connection,scope,database,kind,List.copyOf(ids),equipmentHasteEnabled));
            }
            var procs=new HashSet<Long>();var armorPresence=new HashSet<Long>();var weaponPresence=new HashSet<Long>();
            if(!all.isEmpty()) {
                weaponPresence.addAll(procs(connection,scope,database,"裝備_weapon_魔法","weapon_id",List.copyOf(all)));
                procs.addAll(weaponPresence);
                armorPresence.addAll(procs(connection,scope,database,"裝備_armor_魔法","item_id",List.copyOf(all)));
                procs.addAll(armorPresence);
            }
            var armorIds=new TreeSet<Long>();
            for(var item:input.equipment().source().inventory())if(item.template().kind()==ModernCharacterData.ItemKind.ARMOR
                    &&procs.contains(item.template().catalogId()))armorIds.add(item.template().catalogId());
            var armorProcs=armorIds.isEmpty()?null:ModernArmorProcsJdbc.read(connection,scope,
                    input.equipment().source().sourceId(),database,List.copyOf(armorIds));
            if(armorProcs!=null) {
                var expected=new HashSet<Long>(armorPresence);expected.retainAll(armorIds);
                if(!armorProcs.presentIds().equals(expected))throw unavailable();
            }
            var vipIds=new TreeSet<Long>();
            for(var item:input.equipment().source().inventory())if(item.template().kind()==ModernCharacterData.ItemKind.ETC
                    &&policies.get(item.template().catalogId()).className().equals("add.VIP"))vipIds.add(item.template().catalogId());
            var vip=vipIds.isEmpty()?null:ModernVipEquipmentJdbc.read(connection,scope,
                    input.equipment().source().sourceId(),database,List.copyOf(vipIds),retainedVip);
            scope.checkActive();
            var magic=magicEnabled?ModernMagicCombatSourceJdbc.read(connection,scope,input.equipment().source().sourceId(),database):null;
            scope.checkActive();
            var bows=bowEnabled?magicBows(connection,scope,database,input):null;
            var regen=regenEnabled?ModernNaturalRegenSourceJdbc.read(connection,scope,input.equipment().source().sourceId(),database):null;
            // One source-owned growth read for either use; an enabled Honor map
            // is not a prerequisite for participant healing in other modes.
            var healing=honorEnabled||playerHealingEnabled?ModernHonorHealingJdbc.read(connection,scope,input.equipment().source().sourceId(),database):null;
            scope.checkActive();return new ModernPhysicalCombat.Source(input,growth,policies,procs,magic,bows==null?null:bows.magic(),bows==null?null:bows.sprites(),regen,healing,vip,armorProcs,weaponPresence);
        } catch(Exception failure) {
            scope.checkActive(); // Cancellation wins over a redacted JDBC failure.
            throw unavailable();
        }
    }

    static void requireHealingMagic(boolean magicEnabled,boolean playerHealingEnabled) {
        if(playerHealingEnabled&&!magicEnabled)throw new IllegalArgumentException("player healing requires qualified magic source");
    }

    /** Opt-in only: no missing/NULL magic_bow is treated as an ordinary bow. */
    private record BowRows(Map<Long,Long> magic,Map<Long,Long> sprites){}
    private static BowRows magicBows(Connection connection,ModernBackendScope scope,String database,ModernCombatProfile.Input input)throws Exception {
        var ids=input.weapons().values().stream().map(ModernCombatProfile.WeaponFacts::catalogId).distinct().sorted().toList();
        if(ids.isEmpty())return new BowRows(Map.of(),Map.of());
        String sql="SELECT item_id,magic_bow,arrow_spr FROM "+qualified(database,"weapon")+" WHERE item_id IN ("+placeholders(ids.size())+") ORDER BY item_id LIMIT 513";
        var result=new TreeMap<Long,Long>();var sprites=new TreeMap<Long,Long>();
        try(var statement=prepare(connection,scope,sql,513)) {
            bind(statement,ids);try(var rows=statement.executeQuery()) {
                long previous=0;while(rows.next()) {
                    scope.checkActive();long id=ModernJdbcNumbers.required(rows,"item_id"),value=ModernJdbcNumbers.required(rows,"magic_bow");
                    long sprite=ModernJdbcNumbers.required(rows,"arrow_spr");
                    if(result.size()==512||id<=previous||!ids.contains(id)||value<0||sprite< -1)throw unavailable();previous=id;result.put(id,value);sprites.put(id,sprite);
                }
            }
        }
        scope.checkActive();if(!result.keySet().equals(Set.copyOf(ids)))throw unavailable();return new BowRows(Map.copyOf(result),Map.copyOf(sprites));
    }

    private static List<ModernPhysicalCombat.GrowthRow> growth(Connection connection,ModernBackendScope scope,
            String database,String table) throws Exception {
        var result=new ArrayList<ModernPhysicalCombat.GrowthRow>(127);
        String sql="SELECT id,`命中` AS hit,`傷害` AS damage FROM "+qualified(database,table)+" ORDER BY id LIMIT 128";
        try(var statement=prepare(connection,scope,sql,128);var rows=statement.executeQuery()) {
            while(rows.next()) {
                scope.checkActive();
                if(result.size()==127)throw unavailable();
                long id=ModernJdbcNumbers.required(rows,"id");
                if(id!=result.size()+1)throw unavailable();
                result.add(new ModernPhysicalCombat.GrowthRow(id,ModernJdbcNumbers.required(rows,"hit"),ModernJdbcNumbers.required(rows,"damage")));
            }
        }
        scope.checkActive();if(result.size()!=127)throw unavailable();return List.copyOf(result);
    }

    private static Map<Long,ModernPhysicalCombat.TemplatePolicy> templates(Connection connection,ModernBackendScope scope,
            String database,ModernCharacterData.ItemKind kind,List<Long> ids,boolean equipmentHasteEnabled) throws Exception {
        String table=switch(kind){case WEAPON->"weapon";case ARMOR->"armor";case ETC->"etcitem";};
        boolean haste=equipmentHasteEnabled&&kind!=ModernCharacterData.ItemKind.ETC;
        String sql="SELECT item_id,LEFT(classname,257) AS class_name,OCTET_LENGTH(classname) AS class_bytes"
                +(kind==ModernCharacterData.ItemKind.WEAPON?",dmg_cri AS critical_multiplier":"")
                +(haste?",haste_item":"")
                +" FROM "+qualified(database,table)+" WHERE item_id IN ("+placeholders(ids.size())+") ORDER BY item_id LIMIT 513";
        var result=new TreeMap<Long,ModernPhysicalCombat.TemplatePolicy>();
        try(var statement=prepare(connection,scope,sql,513)) {
            bind(statement,ids);
            try(var rows=statement.executeQuery()) {
                long previous=0;
                while(rows.next()) {
                    scope.checkActive();long id=ModernJdbcNumbers.required(rows,"item_id");
                    if(result.size()==512||id<=previous||!ids.contains(id))throw unavailable();previous=id;
                    var multiplier=kind==ModernCharacterData.ItemKind.WEAPON?multiplier(rows):null;
                    result.put(id,new ModernPhysicalCombat.TemplatePolicy(kind,text(rows),multiplier,
                            haste?OptionalLong.of(ModernJdbcNumbers.required(rows,"haste_item")):OptionalLong.empty()));
                }
            }
        }
        scope.checkActive();if(!result.keySet().equals(Set.copyOf(ids)))throw unavailable();return Map.copyOf(result);
    }

    private static Set<Long> procs(Connection connection,ModernBackendScope scope,String database,String table,
            String column,List<Long> ids) throws Exception {
        String sql="SELECT id,`"+column+"` AS item_id FROM "+qualified(database,table)+" WHERE `"+column+"` IN ("
                +placeholders(ids.size())+") ORDER BY id LIMIT 1025";
        var result=new HashSet<Long>();
        try(var statement=prepare(connection,scope,sql,MAX_PROC_ROWS+1)) {
            bind(statement,ids);
            try(var rows=statement.executeQuery()) {
                long previous=0;int count=0;
                while(rows.next()) {
                    scope.checkActive();long id=ModernJdbcNumbers.required(rows,"id"),item=ModernJdbcNumbers.required(rows,"item_id");
                    if(++count>MAX_PROC_ROWS||id<=previous||!ids.contains(item))throw unavailable();previous=id;
                    // Several configured stages/proc groups may belong to ONE
                    // item. Presence conservatively excludes its equipped use;
                    // this is not a claim that every stage currently triggers.
                    result.add(item);
                }
            }
        }
        scope.checkActive();return Set.copyOf(result);
    }

    private static ModernPhysicalCombat.Rational multiplier(ResultSet rows) throws SQLException {
        int index=rows.findColumn("critical_multiplier");var metadata=rows.getMetaData();
        if(index<1||metadata==null)throw unavailable();
        switch(metadata.getColumnType(index)) {
            case Types.TINYINT,Types.SMALLINT,Types.INTEGER,Types.BIGINT,Types.DECIMAL,Types.NUMERIC -> { }
            default -> throw unavailable(); // Never convert FLOAT/DOUBLE/text into an allegedly exact multiplier.
        }
        var value=rows.getBigDecimal(index);boolean nullValue=rows.wasNull();
        if(value==null||nullValue)throw unavailable();return ModernPhysicalCombat.Rational.decimal(value);
    }
    private static String text(ResultSet rows) throws SQLException {
        String value=rows.getString("class_name");boolean nullValue=rows.wasNull();
        long length=ModernJdbcNumbers.required(rows,"class_bytes");
        if(nullValue||value==null||!ModernCharacterData.validText(value)||value.length()>256
                ||!StandardCharsets.UTF_8.newEncoder().canEncode(value)||length<1||length>256
                ||value.getBytes(StandardCharsets.UTF_8).length!=length)throw unavailable();return value;
    }
    private static PreparedStatement prepare(Connection connection,ModernBackendScope scope,String sql,int maxRows) throws Exception {
        scope.checkActive();PreparedStatement result=connection.prepareStatement(sql);
        try {
            result.setMaxRows(maxRows);result.setQueryTimeout((int)Math.max(1,((long)scope.remainingMillis()+999L)/1000L));
            scope.checkActive();return result;
        } catch(Exception failure) {
            try{result.close();}catch(Exception ignored){}scope.checkActive();throw unavailable();
        }
    }
    private static void bind(PreparedStatement statement,List<Long> ids) throws SQLException {
        for(int i=0;i<ids.size();i++)statement.setLong(i+1,ids.get(i));
    }
    private static String qualified(String database,String fixedTable){return "`"+database+"`.`"+fixedTable+"`";}
    private static String placeholders(int count){if(count<1||count>512)throw new IllegalArgumentException("invalid physical item count");return String.join(",",java.util.Collections.nCopies(count,"?"));}
    private static SQLException unavailable(){return new SQLException("modern physical source unavailable");}
}
