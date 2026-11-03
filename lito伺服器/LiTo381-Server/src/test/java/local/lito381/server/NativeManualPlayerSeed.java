package local.lito381.server;

import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.sql.*;
import java.util.*;

/** Explicit owned-only account/character seed before the serving checksum baseline. Never logs credentials. */
final class NativeManualPlayerSeed {
    private static final long CHARACTER=1_800_000_001L,ITEM_BASE=1_810_000_000L,TEMPLATE=11052L;
    private static Properties properties(Path p)throws Exception {
        var result=new Properties();try(var r=Files.newBufferedReader(p,StandardCharsets.UTF_8)){result.load(r);}
        if(!result.getProperty("game.account","").matches("[a-z0-9]{4,12}")||!result.getProperty("game.password","").matches("[A-Za-z0-9]{8,14}"))throw new IllegalArgumentException("invalid local manual-player settings");return result;
    }
    static void seed(ModernSettlementMysqlFixture mysql,Path credentials,Map<String,String> evidence)throws Exception {
        var p=properties(credentials);int items=0;
        for(String db:List.of("380_ac_1","380_ac_2"))try(var c=mysql.openConnection(ModernSettlementMysqlFixture.deadline(20))) {
            try(var s=c.prepareStatement("INSERT INTO `"+db+"`.`accounts` (`access_level`,`server_no`,`login`,`password`,`lastactive`,`ip`,`host`,`banned`,`character_slot`,`spw`,`warehouse`,`ezpay`) VALUES (0,0,?,?,NOW(),'127.0.0.1','127.0.0.1',0,0,'manual',-256,0)")){
                s.setString(1,p.getProperty("game.account"));s.setString(2,p.getProperty("game.password"));if(s.executeUpdate()!=1)throw new IllegalStateException("owned manual account seed failed");
            }
            var columns=columns(c,db,"characters");var projection=new ArrayList<String>();
            for(String column:columns)projection.add(switch(column.toLowerCase(Locale.ROOT)) {
                case "account_name"->"?";case "char_name"->"'GuiKnight'";case "objid"->Long.toString(CHARACTER);
                case "accesslevel","onlinestatus","clanid","clanrank","banned"->"0";
                case "clanname"->"''";case "level","highlevel"->"80";case "curhp"->"`MaxHp`";case "curmp"->"`MaxMp`";
                default->quote(column);
            });
            try(var s=c.prepareStatement("INSERT INTO `"+db+"`.`characters` ("+String.join(",",columns.stream().map(NativeManualPlayerSeed::quote).toList())+") SELECT "+String.join(",",projection)+" FROM `"+db+"`.`characters` WHERE `objid`=?")) {
                s.setString(1,p.getProperty("game.account"));s.setLong(2,TEMPLATE);if(s.executeUpdate()!=1)throw new IllegalStateException("owned manual character template absent");
            }
            var itemColumns=columns(c,db,"character_items");var ids=new ArrayList<Long>();
            try(var s=c.prepareStatement("SELECT `id` FROM `"+db+"`.`character_items` WHERE `char_id`=? ORDER BY `id`")){s.setLong(1,TEMPLATE);try(var r=s.executeQuery()){while(r.next())ids.add(r.getLong(1));}}
            int i=0;for(long old:ids){var itemProjection=new ArrayList<String>();for(String column:itemColumns)itemProjection.add(switch(column.toLowerCase(Locale.ROOT)){case "id","char_id"->"?";default->quote(column);});
                try(var s=c.prepareStatement("INSERT INTO `"+db+"`.`character_items` ("+String.join(",",itemColumns.stream().map(NativeManualPlayerSeed::quote).toList())+") SELECT "+String.join(",",itemProjection)+" FROM `"+db+"`.`character_items` WHERE `id`=?")){
                    int bind=1;for(String column:itemColumns){if(column.equalsIgnoreCase("id"))s.setLong(bind++,ITEM_BASE+ ++i);else if(column.equalsIgnoreCase("char_id"))s.setLong(bind++,CHARACTER);}s.setLong(bind,old);if(s.executeUpdate()!=1)throw new IllegalStateException("owned manual inventory seed failed");items++;
                }
            }
        }
        evidence.put("manual_player_seed","explicit accounts/characters/character_items clone in both fresh owned sources before serving checksum baseline; access0, level80, original template inventory, no formal SQL writes");
        evidence.put("manual_character","GuiKnight");evidence.put("manual_character_object_id",Long.toString(CHARACTER));evidence.put("manual_inventory_rows",Integer.toString(items));evidence.put("manual_credentials_file",credentials.toAbsolutePath().toString());
    }
    static void verify(ServerConfig config,Path credentials,Map<String,String> evidence)throws Exception {
        var p=properties(credentials);try(var routes=AccountRepository.open(config)) {
            for(var sourceName:config.classicServerNames()) {
                var source=routes.repositoryForClassicSource(sourceName);var auth=source.authenticate(p.getProperty("game.account"),p.getProperty("game.password"));
                if(!auth.accepted())throw new IllegalStateException("owned manual game credentials rejected");
                var chars=source.characters(auth.account());if(chars.size()!=1||chars.get(0).objectId()!=CHARACTER||!chars.get(0).name().equals("GuiKnight"))throw new IllegalStateException("owned manual character roster rejected");
                if(source.sourceInventory(CHARACTER).isEmpty())throw new IllegalStateException("owned manual inventory absent");
            }
        }
        evidence.put("manual_player_validation","fingerprinted root-JAR source auth/one pre-created GuiKnight/source inventory passed on both sources; credentials not logged");
    }
    private static List<String> columns(Connection c,String db,String table)throws Exception {
        var names=new ArrayList<String>();try(var s=c.prepareStatement("SELECT COLUMN_NAME FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA=? AND TABLE_NAME=? ORDER BY ORDINAL_POSITION")){s.setString(1,db);s.setString(2,table);try(var r=s.executeQuery()){while(r.next())names.add(r.getString(1));}}if(names.isEmpty())throw new IllegalStateException("owned source table absent");return names;
    }
    private static String quote(String column){if(!column.matches("[A-Za-z0-9_]+"))throw new IllegalArgumentException("unsafe source column");return "`"+column+"`";}
}
