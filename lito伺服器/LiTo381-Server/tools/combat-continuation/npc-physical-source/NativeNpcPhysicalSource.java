package local.lito381.server;
import java.sql.*;import java.util.*;
/** One immutable shared-static snapshot. No connection, timer or growing per-cast cache. */
final class NativeNpcPhysicalSource {
 record Snapshot(String table,Map<Integer,SourceNpcPhysicalStats> rows,Set<Integer> invalid){
  Snapshot{rows=Map.copyOf(rows);invalid=Set.copyOf(invalid);}
 }
 private static volatile Snapshot current;
 static Snapshot read(Connection connection,ServerConfig config)throws SQLException{
  String database=connection.getCatalog();
  for(String source:config.legacySourceResourceDatabases())if(!Objects.equals(database,source))throw new SQLException("NPC snapshot requires the configured common static database: "+source);
  return read(connection);
 }
 static Snapshot read(Connection connection)throws SQLException{
  String database=connection.getCatalog();if(database==null||!database.matches("[A-Za-z0-9_]+"))throw new SQLException("NPC snapshot has no qualified static catalog");
  var rows=new HashMap<Integer,SourceNpcPhysicalStats>();var invalid=new HashSet<Integer>();
  String sql="SELECT npcid,ac,size,damage_reduction,hard,undead,weakAttr,mr,IsTU,IsErase,lawful,tamable,boss,mp,str,dex,ranged,bowActId,poison_atk,paralysis_atk FROM npc";
  try(var statement=connection.prepareStatement(sql);var row=statement.executeQuery()){
   while(row.next()){
    int id=row.getInt("npcid");if(rows.containsKey(id)||invalid.contains(id))throw new SQLException("duplicate source NPC physical row: "+id);
    try{rows.put(id,SourceNpcPhysicalStats.fromSourceRow(id,row.getInt("ac"),row.getString("size"),row.getInt("damage_reduction"),row.getBoolean("hard"),row.getInt("undead"),row.getInt("weakAttr"),row.getInt("mr"),row.getBoolean("IsTU"),row.getInt("lawful"),row.getBoolean("tamable"),row.getBoolean("boss"),row.getInt("mp"),row.getInt("str"),row.getInt("dex"),row.getBoolean("IsErase"),row.getInt("ranged"),row.getInt("bowActId"),row.getInt("poison_atk"),row.getInt("paralysis_atk")));}
    catch(IllegalArgumentException invalidRow){invalid.add(id);}
   }
  }
  return new Snapshot(database+".npc",rows,invalid);
 }
 static void publish(Snapshot prepared){current=Objects.requireNonNull(prepared);}
 static SourceNpcPhysicalStats lookup(String table,int id)throws SQLException{
  if(id<=0)throw new IllegalArgumentException("invalid source NPC id");
  var snapshot=current;if(snapshot==null)return null;
  if(!snapshot.table.equals(table)&&!("`"+snapshot.table.replace(".","`.`")+"`").equals(table))throw new SQLException("NPC snapshot static source mismatch: "+table);
  if(snapshot.invalid.contains(id))throw new SQLException("invalid source NPC physical row: "+id,new IllegalArgumentException("invalid source NPC physical snapshot"));
  var result=snapshot.rows.get(id);if(result==null)throw new SQLException("source NPC physical row was not found: "+id);return result;
 }
 private NativeNpcPhysicalSource(){}
}
