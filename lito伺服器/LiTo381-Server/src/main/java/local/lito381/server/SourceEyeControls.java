package local.lito381.server;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.*;

/** Read-only GenericBuffItem settings and wide abilities for the seven eyes. */
final class SourceEyeControls {
    record Config(int buff,int seconds,int gfx,int icon,int sharedDelay,int sharedMillis,int itemMillis,String group,boolean overwrite,String endMessage) { }
    record Bundle(Map<Long,Config> configs,Map<Integer,Map<String,Integer>> abilities) { }
    private static final Map<String,String> COLUMNS=Map.ofEntries(
        Map.entry("str","STR"),Map.entry("dex","DEX"),Map.entry("con","CON"),Map.entry("int","INT"),Map.entry("wis","WIS"),Map.entry("cha","CHA"),
        Map.entry("hp","HP"),Map.entry("mp","MP"),Map.entry("hpr","HP_REGEN"),Map.entry("mpr","MP_REGEN"),Map.entry("dmg","DMG"),Map.entry("hit","HIT"),
        Map.entry("bow_dmg","BOW_DMG"),Map.entry("bow_hit","BOW_HIT"),Map.entry("sp","SP"),Map.entry("magic_hit","MAGIC_HIT"),Map.entry("ac","AC"),Map.entry("mr","MR"),
        Map.entry("dmg_redux","DMG_REDUX"),Map.entry("er","ER"),Map.entry("dodge","DODGE"),Map.entry("regist_stone","REGIST_STONE"),Map.entry("regist_sleep","REGIST_SLEEP"),
        Map.entry("regist_freeze","REGIST_FREEZE"),Map.entry("regist_stun","REGIST_STUN"),Map.entry("regist_blind","REGIST_BLIND"),Map.entry("regist_sustain","REGIST_SUSTAIN"),
        Map.entry("resist_fire","DEF_FIRE"),Map.entry("resist_water","DEF_WATER"),Map.entry("resist_wind","DEF_WIND"),Map.entry("resist_earth","DEF_EARTH"),
        Map.entry("regist_poison","POISON_RESIST"),Map.entry("exp_rate","EXP_RATE"),Map.entry("adena_rate","ADENA_RATE"));
    static Bundle read(Connection c)throws SQLException {
        var configs=new HashMap<Long,Config>();var abilities=new HashMap<Integer,Map<String,Integer>>();
        try(var st=c.createStatement();var r=st.executeQuery("SELECT b.*,e.delay_id,e.delay_time,e.delay_effect FROM `etcitem_buff藥水設置` b INNER JOIN etcitem e ON e.item_id=b.item_id WHERE b.item_id BETWEEN 42518 AND 42524 ORDER BY b.item_id")) {
            while(r.next()) {
                long id=r.getLong("item_id");int time=r.getInt("buff_time_sec");String group=r.getString("group_check");
                if(time==0)time=600; // GenericBuffItem.buffTimerSec
                if(time<1||time>65535||r.getInt("consume_item_id")!=0||r.getInt("consume_count")!=0||r.getBoolean("use_custom_packet"))
                    throw new SQLException("eye item="+id+": unsupported indefinite timer, item consumption or custom packet; source profile is not partially applied");
                var value=new Config(r.getInt("buff_id"),time,r.getInt("cast_gfx"),r.getInt("status_icon"),r.getInt("delay_id"),r.getInt("delay_time"),
                        r.getInt("delay_effect"),group==null?"":group.trim(),r.getBoolean("can_overwrite"),r.getString("end_msg_str"));
                if(value.buff()<=0||value.gfx()<=0||value.gfx()>65535||value.icon()<0||value.icon()>65535||value.sharedDelay()<0||value.sharedMillis()<0||value.itemMillis()<0)
                    throw new SQLException("eye item="+id+": invalid source setting");
                if(configs.putIfAbsent(id,value)!=null)throw new SQLException("duplicate eye item="+id);
            }
        }
        if(configs.size()!=7)throw new SQLException("seven source eye settings are required; got="+configs.size());
        var all=new ArrayList<>(configs.values());
        for(int i=0;i<all.size();i++)for(int j=i+1;j<all.size();j++) {
            var groups=new HashSet<>(groups(all.get(i).group()));groups.retainAll(groups(all.get(j).group()));
            if(groups.isEmpty())throw new SQLException("source eye groups require multiple concurrent eyes, not supported by this native lease yet");
        }
        var seen=new HashSet<Long>();
        try(var st=c.createStatement();var r=st.executeQuery("SELECT * FROM `etcitem_buff藥水能力` WHERE item_id BETWEEN 42518 AND 42524 ORDER BY item_id")) {
            while(r.next()) {
                long id=r.getLong("item_id");if(!seen.add(id))throw new SQLException("duplicate eye ability item="+id);
                var config=configs.get(id);if(config==null)throw new SQLException("missing eye setting item="+id);
                var row=abilities.computeIfAbsent(config.buff(),key->new HashMap<>());
                for(var column:COLUMNS.entrySet()) {int value=r.getInt(column.getKey());if(value==0)continue;
                    if(!SourceSkillControls.supportedAbility(column.getValue()))throw new SQLException("eye item="+id+": unimplemented source ability="+column.getValue());
                    row.merge(column.getValue(),value,Math::addExact);
                }
            }
        }
        if(seen.size()!=7)throw new SQLException("seven source eye ability rows are required");
        return new Bundle(Map.copyOf(configs),abilities);
    }
    private static Set<String> groups(String text) {var result=new HashSet<String>();for(String value:text.split(","))if(!value.isBlank())result.add(value.trim().toUpperCase(Locale.ROOT));return result;}
    static Map<Integer,Map<String,Integer>> merge(Map<Integer,Map<String,Integer>> primary,Map<Integer,Map<String,Integer>> extra) {
        var result=new HashMap<Integer,Map<String,Integer>>();primary.forEach((id,row)->result.put(id,new HashMap<>(row)));
        extra.forEach((id,row)->{var target=result.computeIfAbsent(id,key->new HashMap<>());row.forEach((type,value)->target.merge(type,value,Math::addExact));});return result;
    }
}
