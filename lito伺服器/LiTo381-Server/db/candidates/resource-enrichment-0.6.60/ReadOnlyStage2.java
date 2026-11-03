// 未匯入、未接線。Only public SELECT or pure packet/catalog functions. No Main/TCP.
import java.nio.file.*;
import java.lang.reflect.*;
import java.sql.*;
import java.util.*;
import java.security.*;
import java.util.zip.*;
public class ReadOnlyStage2 {
  static String json(Object o){return ReadOnlyCatalog.json(o);}
  static String hash(Object o)throws Exception{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(json(o).getBytes(java.nio.charset.StandardCharsets.UTF_8)));}
  static Map<String,Object> record(Object o)throws Exception{
    var r=new LinkedHashMap<String,Object>();for(var c:o.getClass().getRecordComponents()){
      var m=c.getAccessor();m.setAccessible(true);r.put(c.getName(),m.invoke(o));
    }return r;
  }
  static Object packet(String method,Class<?>[] types,Object...args)throws Exception{
    var c=Class.forName("local.lito381.server.Protocol");var m=c.getDeclaredMethod(method,types);m.setAccessible(true);
    return HexFormat.of().formatHex((byte[])m.invoke(null,args));
  }
  static void presentation()throws Exception{
    var out=new LinkedHashMap<String,Object>();out.put("package_status","未匯入、未接線");out.put("profile",System.getProperty("lito.resourceProfile","lito-legacy"));
    var durations=new ArrayList<Map<String,Object>>();var c=Class.forName("local.lito381.server.BattleSpellCatalog");
    for(var f:c.getDeclaredFields())if(Modifier.isStatic(f.getModifiers())&&Map.class.isAssignableFrom(f.getType())){
      f.setAccessible(true);for(Object value:((Map<?,?>)f.get(null)).values())if(value!=null && value.getClass().isRecord()){
        var r=record(value);if(r.containsKey("skillId") && r.containsKey("durationSeconds")){
          r.entrySet().removeIf(e->!Set.of("skillId","durationSeconds","iconType","legacyIconType","key","effectGfx","castGfx").contains(e.getKey()));r.put("binding",f.getName());durations.add(r);
        }
      }
    }durations.sort(Comparator.comparingInt(r->((Number)r.get("skillId")).intValue()));out.put("presentation_duration_records",durations);
    var packets=new ArrayList<Object>();
    for(int gfx:new int[]{167,1797,1799,1583,11738,11746}){
      var r=new LinkedHashMap<String,Object>();r.put("builder","attackSpell");r.put("input_gfx",gfx);
      r.put("hex",packet("attackSpell",new Class[]{int.class,long.class,long.class,int.class,int.class,long.class,int.class,int.class,int.class,int.class,int.class},18,1L,2L,0,0,1L,gfx,32768,32768,32770,32770));packets.add(r);
    }
    for(int gfx:new int[]{754,2941,11722}){
      var r=new LinkedHashMap<String,Object>();r.put("builder","effectObject");r.put("input_gfx",gfx);r.put("hex",packet("effectObject",new Class[]{long.class,int.class},1L,gfx));packets.add(r);
    }
    var arrow=new LinkedHashMap<String,Object>();arrow.put("builder","rangedPhysicalAttack");arrow.put("input_gfx",66);arrow.put("hex",packet("rangedPhysicalAttack",new Class[]{int.class,long.class,long.class,int.class,int.class,long.class,int.class,int.class,int.class,int.class,int.class},1,1L,2L,0,0,1L,66,32768,32768,32770,32770));packets.add(arrow);
    for(int skill:new int[]{181,201,204,206,209,211,214,216,217,219})for(int duration:new int[]{64,0}){
      var r=new LinkedHashMap<String,Object>();r.put("builder","mageShield");r.put("selector",skill);r.put("duration",duration);r.put("hex",packet("mageShield",new Class[]{int.class,int.class},duration,skill));packets.add(r);
    }
    for(int page=1;page<=18;page++){
      var r=new LinkedHashMap<String,Object>();r.put("builder","showMap");r.put("page",page);r.put("hex",packet("showMap",new Class[]{long.class,int.class},1L,page));packets.add(r);
    }
    out.put("pure_packet_fixtures",packets);
    var npcs=new ArrayList<Object>();var town=Class.forName("local.lito381.server.NativeLitoRentalTownNpcCatalog");var field=town.getDeclaredField("NPCS");field.setAccessible(true);
    for(Object v:(List<?>)field.get(null)){
      var r=record(v);r.entrySet().removeIf(e->!Set.of("resourceIndex","name","startHtml","endHtml","role","action","emblemResourceIndex","emblemName").contains(e.getKey()));npcs.add(r);
    }out.put("npc_resource_services",npcs);
    out.put("network_opened",false);out.put("database_used",false);System.out.println(json(out));
  }
  static List<Map<String,Object>> rows(Connection c,String sql)throws Exception{
    var out=new ArrayList<Map<String,Object>>();try(var s=c.createStatement();var rs=s.executeQuery(sql)){
      var m=rs.getMetaData();while(rs.next()){var r=new LinkedHashMap<String,Object>();for(int i=1;i<=m.getColumnCount();i++)r.put(m.getColumnLabel(i),rs.getObject(i));out.add(r);}
    }return out;
  }
  static String identifier(String s){if(!s.matches("[A-Za-z0-9_]+"))throw new IllegalArgumentException("identifier");return "`"+s+"`";}
  static void select(String properties,String jar)throws Exception{
    var p=new Properties();try(var f=Files.newBufferedReader(Path.of(properties))){p.load(f);}
    String db=p.getProperty("database.static.database"), lito=p.getProperty("database.lito.database");identifier(db);identifier(lito);
    String base=p.getProperty("database.jdbc.base.url"),opts=p.getProperty("database.jdbc.options","");String url=base+(base.endsWith("/")?"":"/")+db+(opts.isEmpty()?"":opts.startsWith("?")?opts:"?"+opts);
    var out=new LinkedHashMap<String,Object>();out.put("package_status","未匯入、未接線");out.put("observed_at_utc",java.time.Instant.now().toString());out.put("scope","SELECT only; public configuration rows and template similarities; no players/credentials/receipts");
    try(var c=DriverManager.getConnection(url,p.getProperty("database.user"),p.getProperty("database.password"))){
      c.setReadOnly(true);out.put("mysql_version",rows(c,"SELECT VERSION() AS version").get(0).get("version"));
      var configs=new LinkedHashMap<String,Object>();
      for(String table:List.of("lito_battle_rule","lito_battle_object","lito_battle_spawn","lito_battle_player_spawn","lito_tournament_shop","commands","lito_source_server")){
        var r=rows(c,"SELECT * FROM "+identifier(lito)+"."+identifier(table));r.sort(Comparator.comparing(ReadOnlyStage2::json));configs.put(table,r);
      }out.put("lito_configuration",configs);out.put("settlement_count",rows(c,"SELECT COUNT(*) AS n FROM "+identifier(lito)+".`lito_battle_settlement`").get(0).get("n"));
      var templates=new LinkedHashMap<String,List<Map<String,Object>>>();
      for(String table:List.of("weapon","armor","etcitem")){
        var cols=rows(c,"SELECT COLUMN_NAME FROM information_schema.COLUMNS WHERE TABLE_SCHEMA='"+db+"' AND TABLE_NAME='"+table+"' ORDER BY ORDINAL_POSITION");
        var use=new ArrayList<String>();for(var col:cols){String name=col.get("COLUMN_NAME").toString();if(Set.of("item_id","name","unidentified_name_id","identified_name_id","type","material","grdgfx","invgfx","weight","dmg_small","dmg_large","safenchant","ac").contains(name))use.add(identifier(name));}
        templates.put(table,rows(c,"SELECT "+String.join(",",use)+" FROM "+identifier(db)+"."+identifier(table)));
      }
      var matches=new ArrayList<Object>();String section=null;String[] headers=null;
      try(var z=new ZipFile(jar)){
        String text=new String(z.getInputStream(z.getEntry("rental/retained-lito-rental-merchant-2010.tsv")).readAllBytes(),java.nio.charset.StandardCharsets.UTF_8);
        for(String line:text.split("\\R")){
          if(line.isBlank())continue;if(line.startsWith("[")){section=line.substring(1,line.length()-1);headers=null;continue;}
          if(headers==null){headers=line.split("\\t");continue;}String[] vals=line.split("\\t",-1);var retained=new LinkedHashMap<String,Object>();for(int i=0;i<headers.length;i++)retained.put(headers[i],vals[i]);
          String name=retained.get("selected_name").toString();var alternatives=new ArrayList<Object>();
          for(var row:templates.get(section.equals("weapon")?"weapon":"armor")){
            if(!name.equals(row.get("name")))continue;
            var criteria=new LinkedHashMap<String,Object>();for(String[] pair:new String[][]{{"type","type"},{"ground_gfx","grdgfx"},{"weight","weight"},{"safe_enchant","safenchant"},{"damage_small","dmg_small"},{"damage_large","dmg_large"}})if(retained.containsKey(pair[0])&&row.containsKey(pair[1]))criteria.put(pair[0],retained.get(pair[0]).toString().equals(row.get(pair[1]).toString()));
            var alt=new LinkedHashMap<String,Object>();alt.put("item_id",row.get("item_id"));alt.put("name",row.get("name"));alt.put("same_fields",criteria);alt.put("comparison_signature_sha256",hash(row));alternatives.add(alt);
          }
          var result=new LinkedHashMap<String,Object>();result.put("resource_index",Integer.valueOf(retained.get("resource_index").toString()));result.put("retained_source_item_id",Integer.valueOf(retained.get("source_item_id").toString()));result.put("retained_name",name);result.put("same_name_candidates",alternatives);result.put("mapped_shared_item_id",null);result.put("mapping_proven",false);result.put("reason","same name/template similarity is not original ID crosswalk evidence");matches.add(result);
        }
      }out.put("rental_template_similarity",matches);
      out.put("currency_name_candidates",templates.get("etcitem").stream().filter(r->r.get("name")!=null&&r.get("name").toString().contains("光之寶石")).map(r->{var small=new LinkedHashMap<String,Object>();small.put("item_id",r.get("item_id"));small.put("name",r.get("name"));return small;}).toList());
      out.put("shared_counts",Map.of("weapon",templates.get("weapon").size(),"armor",templates.get("armor").size(),"etcitem",templates.get("etcitem").size()));
    }System.out.println(json(out));
  }
  public static void main(String[] a)throws Exception{if(a.length==3&&a[0].equals("--db-select"))select(a[1],a[2]);else if(a.length==1&&a[0].equals("--presentation"))presentation();else throw new IllegalArgumentException("explicit --presentation or --db-select config jar required");}
}
