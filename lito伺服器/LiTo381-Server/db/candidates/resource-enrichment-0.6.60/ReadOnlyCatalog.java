// 未匯入、未接線。Offline reflection of a pinned JAR; never starts ServerMain.
import java.lang.reflect.*;
import java.util.*;
import java.nio.file.*;
import java.sql.*;
public class ReadOnlyCatalog {
  static String json(Object v) {
    if(v==null)return "null";
    if(v instanceof Number || v instanceof Boolean)return v.toString();
    if(v instanceof Map<?,?> m){var a=new ArrayList<String>();for(var e:m.entrySet())a.add(json(e.getKey().toString())+":"+json(e.getValue()));return "{"+String.join(",",a)+"}";}
    if(v instanceof Collection<?> c){var a=new ArrayList<String>();for(var e:c)a.add(json(e));return "["+String.join(",",a)+"]";}
    return "\""+v.toString().replace("\\","\\\\").replace("\"","\\\"").replace("\r","\\r").replace("\n","\\n").replace("\t","\\t")+"\"";
  }
  static Map<String,Object> record(Object o) throws Exception {
    var out=new LinkedHashMap<String,Object>();
    for(var c:o.getClass().getRecordComponents()){var m=c.getAccessor();m.setAccessible(true);var v=m.invoke(o);if(v!=null && v.getClass().isRecord())v=record(v);out.put(c.getName(),v);}
    return out;
  }
  public static void main(String[] args) throws Exception {
    if(args.length==2 && args[0].equals("--db-select")) {
      var p=new Properties();try(var r=Files.newBufferedReader(Path.of(args[1]))){p.load(r);}
      String base=p.getProperty("database.jdbc.base.url"), schema=p.getProperty("database.static.database"), opts=p.getProperty("database.jdbc.options","");
      if(!schema.matches("[A-Za-z0-9_]+"))throw new Exception("invalid schema");
      String url=base+(base.endsWith("/")?"":"/")+schema+(opts.isEmpty()?"":(opts.startsWith("?")?opts:"?"+opts));
      try(var conn=DriverManager.getConnection(url,p.getProperty("database.user"),p.getProperty("database.password"))){
        conn.setReadOnly(true);var out=new LinkedHashMap<String,Object>();
        out.put("package_status","未匯入、未接線");
        try(var s=conn.createStatement();var r=s.executeQuery("SELECT VERSION()")){r.next();out.put("mysql_version",r.getString(1));}
        out.put("observed_at_utc",java.time.Instant.now().toString());out.put("schema",schema);
        for(String table:List.of("skills","etcitem","weapon","armor","npc")){
          String id=table.equals("skills")?"skill_id":table.equals("npc")?"npcid":"item_id";
          var ids=new ArrayList<Integer>();try(var s=conn.createStatement();var r=s.executeQuery("SELECT `"+id+"` FROM `"+table+"`")){while(r.next())ids.add(r.getInt(1));}Collections.sort(ids);out.put(table+"_ids",ids);
        }
        out.put("scope","SELECT VERSION and public static definition IDs only; readOnly=true; no player data; no writes");System.out.println(json(out));
      }return;
    }
    var out=new LinkedHashMap<String,Object>();out.put("package_status","未匯入、未接線");var bindings=new ArrayList<Object>();
    Class<?> spell=Class.forName("local.lito381.server.BattleSpellCatalog");
    for(var f:spell.getDeclaredFields())if(Modifier.isStatic(f.getModifiers()) && Map.class.isAssignableFrom(f.getType())){
      f.setAccessible(true);var value=f.get(null);if(value instanceof Map<?,?> map)for(var e:map.entrySet()){
        Object v=e.getValue();if(v!=null && v.getClass().isRecord()){
          var r=record(v);if(r.containsKey("skillId")){
            r.entrySet().removeIf(e2 -> !Set.of("skillId","row","column","key","actionId","databaseActionId","effectGfx","castGfx","impactGfx","specialCriticalGfx","attackerEffectGfx","targetEffectGfx","consumeEffectGfx","allyEffectGfx","enemyEffectGfx","earthEffectGfx","fireEffectGfx","waterEffectGfx","windEffectGfx","coexistGraphic","cubeGfx","dragonGfx","effectNpcGfx","spawnEffectGfx","triggerEffectGfx","iconType","legacyIconType","targeting","requestShape","lineTargeted","soundId","templatePresent").contains(e2.getKey()));
            r.put("binding",f.getName());r.put("record_type",v.getClass().getSimpleName());bindings.add(r);
          }
        }
      }
    }bindings.sort(Comparator.comparingInt(o -> ((Number)((Map<?,?>)o).get("skillId")).intValue()));out.put("spell_records",bindings);
    for(String cat:List.of("NativeLitoMapTableCatalog","NativeMapItemCatalog","NativeLitoZoneCatalog")){
      var c=Class.forName("local.lito381.server."+cat);var fields=new LinkedHashMap<String,Object>();
      for(var f:c.getDeclaredFields())if(Modifier.isStatic(f.getModifiers()) && List.class.isAssignableFrom(f.getType())){
        f.setAccessible(true);var rows=new ArrayList<Object>();for(var v:(List<?>)f.get(null))rows.add(record(v));fields.put(f.getName(),rows);
      }out.put(cat,fields);
    }System.out.println(json(out));
  }
}
