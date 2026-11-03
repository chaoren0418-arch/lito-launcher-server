// ORIGINAL SOURCE moved from lito登入器/LiTo381-Launcher/server-patches/repair-033/LiToItemTemplates.java (T002); replaces CFR decompile of reference/base-0.6.71.jar
package local.lito381.server;
import java.util.*;import java.sql.*;import java.lang.reflect.*;import javax.sql.DataSource;
/** Raw, selected-source templates. Never read database credentials or query from a paint/send loop. */
final class LiToItemTemplates {
 static final Map<AccountRepository,Map<Long,Map<String,String>>> CACHE=Collections.synchronizedMap(new WeakHashMap<>());
 static void preload(AccountRepository repository) {
  if(!Boolean.getBoolean("lito.extendedItemDetails")||repository==null)return;
  synchronized(CACHE){
   if(CACHE.containsKey(repository))return;
   if(!repository.getClass().getSimpleName().equals("LegacyMySqlRepository")){CACHE.put(repository,Map.of());return;}
   try{
    Field sf=repository.getClass().getDeclaredField("resourceDatabase"),df=repository.getClass().getDeclaredField("dataSource");sf.setAccessible(true);df.setAccessible(true);
    String schema=(String)sf.get(repository);if(!schema.matches("[A-Za-z0-9_]+"))throw new SQLException("invalid resource schema");
    Map<Long,Map<String,String>> items=new HashMap<>();
    try(Connection cn=((DataSource)df.get(repository)).getConnection()){
     cn.setReadOnly(true);
     for(String table:List.of("armor","weapon"))try(Statement st=cn.createStatement();ResultSet rows=st.executeQuery("SELECT * FROM `"+schema+"`.`"+table+"`")){
      var md=rows.getMetaData();while(rows.next()){Map<String,String> item=new LinkedHashMap<>();for(int i=1;i<=md.getColumnCount();i++){String v=rows.getString(i);if(v!=null)item.put(md.getColumnLabel(i).toLowerCase(Locale.ROOT),v);}item.put("kind",table);items.put(rows.getLong("item_id"),Map.copyOf(item));}
     }
     try(Statement st=cn.createStatement();ResultSet rows=st.executeQuery("SELECT Item_Id,Info FROM `"+schema+"`.`道具_物品詳細說明`")){
      while(rows.next()){String info=rows.getString("Info");if(info==null||info.trim().isEmpty())continue;long id=rows.getLong("Item_Id");Map<String,String> item=new LinkedHashMap<>(items.getOrDefault(id,Map.of("kind","etc")));item.put("_custom",item.getOrDefault("_custom","")+info+"\n");items.put(id,Map.copyOf(item));}
     }catch(SQLException missing){if(!"42S02".equals(missing.getSQLState()))throw missing;System.err.println("[LiTo] optional item description table absent");}
    }
    CACHE.put(repository,Map.copyOf(items));System.out.println("[LiTo] full item templates ready count="+items.size());
   }catch(Exception e){CACHE.put(repository,Map.of());System.err.println("[LiTo] full item templates unavailable: "+e.getClass().getSimpleName());}
  }
 }
 static Map<String,String> get(AccountRepository repository,long catalogId){synchronized(CACHE){return CACHE.getOrDefault(repository,Map.of()).get(catalogId);}}
 static String custom(AccountRepository repository,long catalogId){synchronized(CACHE){var items=CACHE.getOrDefault(repository,Map.of());return items.getOrDefault(-1L,Map.of()).getOrDefault("_custom","")+items.getOrDefault(catalogId,Map.of()).getOrDefault("_custom","");}}
 static long number(Map<String,String> t,String key){String s=t.get(key);return s==null||s.isBlank()?0:Long.parseLong(s);}
 static void row(StringBuilder out,Map<String,String> t,String key,String label,String suffix){long value=number(t,key);if(value!=0)out.append(label).append(' ').append(value>0?"+":"").append(value).append(suffix).append('\n');}
 static String describe(RuntimeInventory.Item item,Map<String,String> t){
  StringBuilder out=new StringBuilder(item.nativeViewName()).append('\n');boolean armor=t.get("kind").equals("armor");
  if(!item.identified())return out.append("尚未鑑定").toString();
  if(armor)out.append("防禦力 ").append(number(t,"ac")).append('\n');
  else out.append("攻擊力 ").append(number(t,"dmg_small")).append('/').append(number(t,"dmg_large")).append('\n');
  String[][] fields={
   {"add_str","力量"},{"add_dex","敏捷"},{"add_con","體質"},{"add_wis","精神"},{"add_int","智力"},{"add_cha","魅力"},
   {"add_hp","體力上限"},{"add_mp","魔力上限"},{"add_hpr","HP回復"},{"add_mpr","MP回復"},{"m_def","魔法防禦"},{"add_sp","魔攻"},
   {armor?"hit_modifier":"hitmodifier","攻擊成功"},{armor?"dmg_modifier":"dmgmodifier","額外攻擊點數"},
   {"bow_hit_modifier","遠距離攻擊成功"},{"bow_dmg_modifier","遠距離額外攻擊點數"},
   {"add_hitup","近距離命中"},{"add_dmgup","近距離傷害"},{"add_bow_hitup","遠距離命中"},{"add_bow_dmgup","遠距離傷害"},
   {"damage_reduction","傷害減免"},{"add_melee_redux","近距離減免"},{"add_bow_redux","遠距離減免"},{"add_ignore_reduction","無視減免"},
   {"magicdmgmodifier","魔法傷害"},{"add_magic_hit","魔法命中"},{"add_pvp_dmg","PVP傷害"},{"add_pvp_redux","PVP減免"},
   {"add_boss_dmg","對BOSS傷害"},{"add_boss_redux","受到BOSS減傷","%"},{"add_dodge","閃避率"},{"add_er","遠距離迴避"},
   {"defense_fire","火屬性防禦"},{"defense_water","水屬性防禦"},{"defense_wind","風屬性防禦"},{"defense_earth","地屬性防禦"},
   {"regist_stun","昏迷耐性"},{"regist_stone","石化耐性"},{"regist_sleep","睡眠耐性"},{"regist_freeze","冰凍耐性"},{"regist_sustain","支撐耐性"},{"regist_blind","暗黑耐性"},
   {"add_stun_success","暈眩成功率"},{"add_potion_recovery","藥水恢復","%"},{"weight_reduction","負重減輕"}
  };
  for(String[] f:fields)row(out,t,f[0],f[1],f.length==3?f[2]:"");
  for(String[] f:new String[][]{{"haste_item","加速效果"},{"add_poison_resist","毒性抵抗"},{"add_pierce","貫穿"},{"magic_bow","魔法箭矢"}})if(number(t,f[0])>0)out.append(f[1]).append('\n');
  for(String[] f:new String[][]{{"add_adena_rate","金幣獲取"},{"add_drop_rate","掉寶獲取"},{"add_exp_rate","經驗獲取"}}){long rate=number(t,f[0]);if(rate!=0)out.append(f[1]).append(' ').append(rate>0?"+":"").append(rate).append("%\n");}
  long min=number(t,"min_lvl"),max=number(t,"max_lvl");if(min>0)out.append("最低等級 ").append(min).append('\n');if(max>0)out.append("最高等級 ").append(max).append('\n');
  if(!armor||!Set.of("ring","amulet","belt","earring").contains(t.get("type"))){long safe=number(t,"safenchant");if(safe>=0)out.append("安定值 ").append(safe).append('\n');}
  String[] keys={"use_royal","use_knight","use_elf","use_mage","use_darkelf","use_dragonknight","use_illusionist"};String[] names={"王族","騎士","妖精","法師","黑暗妖精","龍騎士","幻術士"};
  for(int i=0;i<keys.length;i++)if(number(t,keys[i])!=0)out.append('[').append(names[i]).append(']');out.append('\n');
  String material=Map.ofEntries(Map.entry("gemstone","寶石"),Map.entry("metal","金屬"),Map.entry("steel","鋼鐵"),Map.entry("silver","銀"),Map.entry("gold","金"),Map.entry("wood","木"),Map.entry("leather","皮"),Map.entry("cloth","布"),Map.entry("bone","骨"),Map.entry("iron","鐵"),Map.entry("mithril","米索莉"),Map.entry("oriharukon","奧里哈魯根"),Map.entry("blackmithril","黑色米索莉"),Map.entry("platinum","白金"),Map.entry("glass","玻璃"),Map.entry("paper","紙"),Map.entry("liquid","液體"),Map.entry("plant","植物"),Map.entry("mineral","礦石")).get(t.get("material"));
  if(material!=null)out.append("材質: ").append(material).append('\n');
  if(number(t,"cant_delete")!=0)out.append("不可刪除\n");if(number(t,"trade")!=0)out.append("不可交易\n");
  if(item.durability()!=0)out.append("損壞度 ").append(item.durability()).append('\n');
  if(item.definition().maximumChargeCount()>0)out.append("剩餘次數 ").append(item.chargeCount()).append('\n');
  out.append("重量: ").append(Math.max(number(t,"weight")>0?1:0,Math.multiplyExact(number(t,"weight"),(long)item.quantity())/1000)).append('\n');
  return out.toString();
 }
 static boolean needsExtension(Map<String,String> t){
  boolean armor=t.get("kind").equals("armor");int nativeRows=4;
  for(String key:List.of("add_str","add_dex","add_con","add_wis","add_int","add_cha","add_hp","add_mp","m_def","add_sp","defense_fire","defense_water","defense_wind","defense_earth","regist_stun","regist_stone","regist_sleep","regist_freeze","regist_sustain","regist_blind")){
   long value=number(t,key);if(value!=0)nativeRows++;
   if(value!=0&&!armor)return true;
   long limit=key.equals("add_hp")||key.equals("add_mp")?32767:127;if(value>limit||value< -limit-1)return true;
  }
  if(nativeRows>16 || Math.abs(number(t,"ac"))>255)return true;
  for(String key:List.of("add_hpr","add_mpr","hit_modifier","dmg_modifier","bow_hit_modifier","bow_dmg_modifier","add_hitup","add_dmgup","add_bow_hitup","add_bow_dmgup","damage_reduction","add_melee_redux","add_bow_redux","add_ignore_reduction","magicdmgmodifier","add_magic_hit","add_pvp_dmg","add_pvp_redux","add_boss_dmg","add_boss_redux","add_dodge","add_er","add_stun_success","add_potion_recovery","weight_reduction","add_poison_resist","add_pierce","magic_bow","add_adena_rate","add_drop_rate","add_exp_rate"))if(number(t,key)!=0)return true;
  return Math.abs(number(t,"hitmodifier"))>127||Math.abs(number(t,"dmgmodifier"))>127||number(t,"dmg_small")>255||number(t,"dmg_large")>255;
 }
}
