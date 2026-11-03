package local.lito381.server;

import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Optional;

/** One bounded SELECT; no native open/migrations, no fallback table, no catalog cache. */
final class ModernShopCatalogJdbc {
    private ModernShopCatalogJdbc() {}
    static String sql(String catalog) {
        return sql(catalog,false);
    }
    static String sql(String catalog,boolean bowEnabled) {
        if(catalog==null || !catalog.matches("[A-Za-z0-9_]{1,64}"))throw new IllegalArgumentException("explicit shop catalog identifier required");
        String prefix="`"+catalog+"`.";
        return "SELECT "+textSql("a.login","canonical_account",64)+",a.banned AS account_banned,c.objid,"
                +textSql("c.char_name","char_name",256)+",c.Type AS character_type,c.Sex AS character_sex,c.level,c.Banned AS character_banned,"
                +"s.item_id AS shop_item,s.sort_order,s.buy_price,s.sell_price,s.enabled,"
                +templateSql("w")+","+templateSql("a1")+","+templateSql("e")+","
                +textSql("e.classname","executor",256)+","+textSql("e.item_type","item_type",64)+","
                +textSql("e.use_type","use_type",64)+","+textSql("e.material","material",64)+","
                +"e.stackable,e.max_charge_count,e.min_lvl,e.max_lvl,e.bless,e.trade,e.cant_delete,e.delay_id,e.delay_time,"
                +"e.delay_effect,e.food_volume,e.save_at_once,e.limit_class"
                +(bowEnabled?",p.item_id AS ammo_id,p.dmg_small AS ammo_small,p.dmg_large AS ammo_large,p.arrow_spr AS ammo_sprite,p.consume_on_attack AS ammo_consume":"")
                +" FROM accounts a JOIN characters c ON c.account_name=a.login"
                +" LEFT JOIN "+prefix+"`lito_tournament_shop` s ON 1=1"
                +" LEFT JOIN "+prefix+"`weapon` w ON w.item_id=s.item_id"
                +" LEFT JOIN "+prefix+"`armor` a1 ON a1.item_id=s.item_id"
                +" LEFT JOIN "+prefix+"`etcitem` e ON e.item_id=s.item_id"
                +(bowEnabled?" LEFT JOIN "+prefix+"`etcitem_arrow` p ON p.item_id=s.item_id AND s.item_id IN (40744,40746)":"")
                +" WHERE a.login=? AND c.objid=? ORDER BY s.sort_order,s.item_id LIMIT "+(ModernShopCatalog.MAX_ROWS+1);
    }
    private static String textSql(String column,String alias,int limit) {
        return "LEFT("+column+","+(limit+1)+") AS "+alias+",OCTET_LENGTH("+column+") AS "+alias+"_bytes";
    }
    private static String templateSql(String alias) {
        return alias+".item_id AS "+alias+"_id,"+textSql(alias+".name_id",alias+"_name",256)+","+alias+".invgfx AS "+alias
                +"_invgfx,"+alias+".grdgfx AS "+alias+"_grdgfx,"+alias+".weight AS "+alias+"_weight";
    }
    static Optional<ModernShopCatalog.Snapshot> read(Connection connection,ModernBackendScope scope,int sourceId,
                                                    String canonicalAccount,long objectId,String catalog)throws Exception {
        return read(connection,scope,sourceId,canonicalAccount,objectId,catalog,false);
    }
    static Optional<ModernShopCatalog.Snapshot> read(Connection connection,ModernBackendScope scope,int sourceId,
                                                    String canonicalAccount,long objectId,String catalog,boolean bowEnabled)throws Exception {
        if(sourceId<1 || objectId<1 || !ModernShopCatalog.validText(canonicalAccount,64))throw unavailable();
        String query=sql(catalog,bowEnabled);scope.checkActive();
        try {
            Optional<ModernShopCatalog.Snapshot> result;
            try(var statement=connection.prepareStatement(query)) {
                statement.setMaxRows(ModernShopCatalog.MAX_ROWS+1);
                statement.setQueryTimeout((int)Math.max(1L,((long)scope.remainingMillis()+999)/1000));
                statement.setString(1,canonicalAccount);statement.setLong(2,objectId);scope.checkActive();
                try(var rows=statement.executeQuery()) {result=decode(rows,scope,sourceId,canonicalAccount,objectId,bowEnabled);}
            }
            scope.checkActive();return result;
        } catch(Exception failure) {scope.checkActive();throw unavailable();}
    }
    private static Optional<ModernShopCatalog.Snapshot> decode(ResultSet rows,ModernBackendScope scope,int sourceId,
                                                              String canonicalAccount,long objectId,boolean bowEnabled)throws Exception {
        ModernApplicationProtocol.CharacterRow character=null;var offers=new ArrayList<ModernShopCatalog.Offer>();
        var ids=new HashSet<Long>();int previous=-1,count=0;boolean empty=false;
        while(rows.next()) {
            scope.checkActive();if(++count>ModernShopCatalog.MAX_ROWS)throw unavailable();
            String account=text(rows,"canonical_account",64);long id=number(rows,"objid"),type=number(rows,"character_type"),sex=number(rows,"character_sex");
            if(!canonicalAccount.equals(account) || id!=objectId || type<0 || type>65535 || sex<0 || sex>1
                    || flag(rows,"account_banned") || flag(rows,"character_banned"))throw unavailable();
            var fresh=new ModernApplicationProtocol.CharacterRow(id,text(rows,"char_name",256),(int)type,(int)sex,number(rows,"level"));
            if(character==null)character=fresh;else if(!character.equals(fresh))throw unavailable();
            var item=ModernJdbcNumbers.nullable(rows,"shop_item");
            if(item.isEmpty()) {
                if(empty || count!=1)throw unavailable();empty=true;
                for(String column:new String[]{"sort_order","buy_price","sell_price","enabled","w_id","a1_id","e_id"})
                    if(ModernJdbcNumbers.nullable(rows,column).isPresent())throw unavailable();
                continue;
            }
            if(empty || item.getAsLong()<1 || !ids.add(item.getAsLong()))throw unavailable();
            long sort=number(rows,"sort_order");if(sort<0 || sort>Integer.MAX_VALUE || sort<=previous)throw unavailable();previous=(int)sort;
            boolean enabled=flag(rows,"enabled");String chosen=null;
            for(String alias:new String[]{"w","a1","e"}) {
                var match=ModernJdbcNumbers.nullable(rows,alias+"_id");
                if(match.isPresent()) {if(chosen!=null || match.getAsLong()!=item.getAsLong())throw unavailable();chosen=alias;}
            }
            if(chosen==null)throw unavailable();
            var kind=chosen.equals("w")?ModernCharacterData.ItemKind.WEAPON:chosen.equals("a1")?ModernCharacterData.ItemKind.ARMOR:ModernCharacterData.ItemKind.ETC;
            ModernShopCatalog.Etc etc=null;
            if(kind==ModernCharacterData.ItemKind.ETC)etc=new ModernShopCatalog.Etc(nullableText(rows,"executor",256),text(rows,"item_type",64),
                    text(rows,"use_type",64),text(rows,"material",64),number(rows,"stackable"),number(rows,"max_charge_count"),
                    number(rows,"min_lvl"),number(rows,"max_lvl"),number(rows,"bless"),number(rows,"trade"),number(rows,"cant_delete"),
                    number(rows,"delay_id"),number(rows,"delay_time"),number(rows,"delay_effect"),number(rows,"food_volume"),
                    number(rows,"save_at_once"),number(rows,"limit_class"));
            ModernShopCatalog.Arrow arrow=null;
            if(bowEnabled) {
                var arrowId=ModernJdbcNumbers.nullable(rows,"ammo_id");
                if(ModernAmmo.Kind.find(item.getAsLong())!=null) {
                    if(arrowId.isEmpty()||arrowId.getAsLong()!=item.getAsLong()||kind!=ModernCharacterData.ItemKind.ETC)throw unavailable();
                    arrow=new ModernShopCatalog.Arrow(number(rows,"ammo_small"),number(rows,"ammo_large"),number(rows,"ammo_sprite"),number(rows,"ammo_consume"));
                } else if(arrowId.isPresent())throw unavailable();
            }
            var template=new ModernShopCatalog.Template(kind,text(rows,chosen+"_name",256),number(rows,chosen+"_invgfx"),
                    number(rows,chosen+"_grdgfx"),number(rows,chosen+"_weight"),etc,arrow);
            var offer=ModernShopCatalog.Offer.fromSource(item.getAsLong(),(int)sort,number(rows,"buy_price"),number(rows,"sell_price"),template);
            if(enabled)offers.add(offer);
        }
        scope.checkActive();return character==null?Optional.empty():Optional.of(new ModernShopCatalog.Snapshot(sourceId,canonicalAccount,character,offers));
    }
    private static String nullableText(ResultSet rows,String column,int maximum)throws SQLException {
        String value=rows.getString(column);var bytes=ModernJdbcNumbers.nullable(rows,column+"_bytes");
        if(value==null) {if(bytes.isPresent())throw unavailable();return null;}
        if(bytes.isEmpty() || !ModernShopCatalog.validText(value,maximum) || value.getBytes(StandardCharsets.UTF_8).length!=bytes.getAsLong())throw unavailable();return value;
    }
    private static String text(ResultSet rows,String column,int maximum)throws SQLException {String value=nullableText(rows,column,maximum);if(value==null)throw unavailable();return value;}
    private static long number(ResultSet rows,String column)throws SQLException {return ModernJdbcNumbers.required(rows,column);}
    private static boolean flag(ResultSet rows,String column)throws SQLException {long value=number(rows,column);if(value!=0 && value!=1)throw unavailable();return value==1;}
    private static SQLException unavailable(){return new SQLException("modern shop source unavailable");}
}
