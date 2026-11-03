package local.lito381.server;
import java.nio.file.*;import java.sql.*;import java.util.*;import java.lang.reflect.*;
/** Read-only formal-JAR/source-SQL comparison; does not alter runtime state or tables. */
public class CombatParityAudit {
 public static void main(String[] a)throws Exception {
  var config=ServerConfig.load(Path.of(a[0]),"standard");ResourceDatabase.load(config);
  var p=UnifiedConfiguration.read(config.source());String db=SharedDatabase.identifier(p.getProperty("database.static.database"));
  try(var c=DriverManager.getConnection(p.getProperty("database.jdbc.base.url")+db+p.getProperty("database.jdbc.options"),p.getProperty("database.user"),p.getProperty("database.password"));var st=c.createStatement()){
   c.setReadOnly(true);
   Map<Integer,Map<String,Integer>> sqlSkills=new HashMap<>();
   try(var rows=st.executeQuery("SELECT skill_id,mpConsume,hpConsume,reuseDelay,buffDuration FROM skills")){while(rows.next()){var fields=new HashMap<String,Integer>();fields.put("mpCost",rows.getInt("mpConsume"));fields.put("hpCost",rows.getInt("hpConsume"));fields.put("reuseDelayMillis",rows.getInt("reuseDelay"));fields.put("durationSeconds",rows.getInt("buffDuration"));sqlSkills.put(rows.getInt(1),fields);}}
   int comparisons=0,differences=0;Set<Integer> checkedIds=new HashSet<>();
   for(Field f:BattleSpellCatalog.class.getDeclaredFields())if(Modifier.isStatic(f.getModifiers())&&Map.class.isAssignableFrom(f.getType())) {
    f.setAccessible(true);for(Object v:((Map<?,?>)f.get(null)).values())try {
     Method idMethod=v.getClass().getDeclaredMethod("skillId");idMethod.setAccessible(true);int id=(int)idMethod.invoke(v);var row=sqlSkills.get(id);if(row==null)continue;
     for(var cell:row.entrySet())try{var method=v.getClass().getDeclaredMethod(cell.getKey());method.setAccessible(true);Object value=method.invoke(v);if(!(value instanceof Number n))continue;comparisons++;checkedIds.add(id);if(n.intValue()!=cell.getValue()){differences++;System.out.println("FIELD_DIFFERENCE skill="+id+" record="+v.getClass().getSimpleName()+" field="+cell.getKey()+" lito="+n+" sql="+cell.getValue());}}catch(NoSuchMethodException ignored){}
    }catch(NoSuchMethodException ignored){}
   }
   System.out.println("FIELD_COMPARISON skills="+checkedIds.size()+" comparisons="+comparisons+" differences="+differences+" fields=mpCost,hpCost,reuseDelayMillis,durationSeconds");
   for(String table:List.of("skills_buff_ability","skills_負面技能控制","skills_負面boss免疫"))try(var rows=st.executeQuery("SELECT * FROM `"+table+"` WHERE skill_id IN (11,43,68,105,132,181)")){while(rows.next()){var cols=new ArrayList<String>();for(int i=1;i<=rows.getMetaData().getColumnCount();i++)cols.add(rows.getMetaData().getColumnName(i)+"="+rows.getString(i));System.out.println("CONTROL "+table+" "+String.join(" ",cols));}}
   for(String table:List.of("skills","skills_buff_ability","skills_負面技能控制","skills_負面boss免疫")) {
    try(var rows=st.executeQuery("SELECT * FROM `"+table+"` LIMIT 0")) {var cols=new ArrayList<String>();for(int i=1;i<=rows.getMetaData().getColumnCount();i++)cols.add(rows.getMetaData().getColumnName(i));System.out.println("COLUMNS "+table+" "+cols);}
   }
   try(var rows=st.executeQuery("SELECT * FROM skills WHERE skill_id IN (1,4,11,43,68,105,132,181)")){while(rows.next()){var cols=new ArrayList<String>();for(int i=1;i<=rows.getMetaData().getColumnCount();i++)cols.add(rows.getMetaData().getColumnName(i)+"="+rows.getString(i));System.out.println("SQL "+String.join(" ",cols));}}
  }
  Set<Integer> wanted=Set.of(1,4,11,43,68,105,132,181);Set<Integer> ids=new TreeSet<>();
  for(Field f:BattleSpellCatalog.class.getDeclaredFields())if(Modifier.isStatic(f.getModifiers())&&Map.class.isAssignableFrom(f.getType())){
   f.setAccessible(true);for(Object v:((Map<?,?>)f.get(null)).values())try{Method m=v.getClass().getDeclaredMethod("skillId");m.setAccessible(true);int id=(int)m.invoke(v);ids.add(id);if(wanted.contains(id))System.out.println("LITO "+f.getName()+" "+v);}catch(NoSuchMethodException ignored){}
  }
  System.out.println("CATALOG_DISTINCT_IDS "+ids.size()+" "+ids);
 }
}
