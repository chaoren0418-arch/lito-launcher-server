package local.lito381.server;
import java.sql.*;import java.util.*;

/** Shared-static startup snapshot; never opens SQL from a cast or owns per-actor state. */
final class NativeCancellationNpcRules {
 record Rule(int npcId,int type,int targetId,int durationSeconds,String nameId,int passispeed) {
  Rule {nameId=nameId==null?"":nameId.trim();}
  boolean changesAppearance(int originalGfx,int currentGfx){return type==1&&currentGfx==originalGfx&&currentGfx!=targetId;}
 }
 record Snapshot(String table,Map<Integer,Rule> rows,Map<Integer,String> invalid) {
  Snapshot {rows=Map.copyOf(rows);invalid=Map.copyOf(invalid);}
 }
 private static volatile Snapshot current;
 static Snapshot read(Connection c,ServerConfig cfg)throws SQLException {
  String db=c.getCatalog();
  for(String source:cfg.legacySourceResourceDatabases())if(!Objects.equals(db,source))throw new SQLException("Cancellation NPC rules require the configured common static database: "+source);
  return read(c);
 }
 static Snapshot read(Connection c)throws SQLException {
  String db=c.getCatalog();if(db==null||!db.matches("[A-Za-z0-9_]+"))throw new SQLException("Cancellation NPC rules have no qualified static catalog");
  var rows=new HashMap<Integer,Rule>();var invalid=new HashMap<Integer,String>();var seen=new HashSet<Integer>();
  String sql="SELECT r.npc_id,r.transform_type,r.target_id,r.duration_seconds,r.new_name_id,r.new_passispeed,s.npcid AS source_exists,t.npcid AS target_exists FROM `skills_相消怪物轉換` r LEFT JOIN npc s ON s.npcid=r.npc_id LEFT JOIN npc t ON t.npcid=r.target_id";
  try(var st=c.prepareStatement(sql);var rs=st.executeQuery()) {
   while(rs.next()) {
    int id=rs.getInt("npc_id");if(!seen.add(id))throw new SQLException("duplicate cancellation NPC transform: "+id);
    Rule rule=new Rule(id,rs.getInt("transform_type"),rs.getInt("target_id"),rs.getInt("duration_seconds"),rs.getString("new_name_id"),rs.getInt("new_passispeed"));
    String reason=invalidReason(rule,rs.getObject("source_exists")!=null,rs.getObject("target_exists")!=null);
    if(reason==null)rows.put(id,rule);else invalid.put(id,reason);
   }
  }
  return new Snapshot(db+".skills_相消怪物轉換",rows,invalid);
 }
 static String invalidReason(Rule rule,boolean sourceExists,boolean targetExists) {
  if(!sourceExists)return "source NPC does not exist";
  if(rule.type()!=1&&rule.type()!=2)return "transform type must be 1 or 2";
  if(rule.targetId()<=0)return "target must be positive";
  if(rule.durationSeconds()<0||rule.passispeed()<0)return "duration or passive speed is negative";
  if(rule.type()==2&&rule.targetId()==rule.npcId())return "replacement cannot be the source NPC";
  if(rule.type()==2&&!targetExists)return "replacement NPC does not exist";
  return null;
 }
 static void publish(Snapshot snapshot){
  Objects.requireNonNull(snapshot);
  snapshot.invalid().forEach((id,reason)->System.err.println("[LiTo] cancellation NPC rule skipped: npc="+id+" reason="+reason));
  current=snapshot;
 }
 static Rule lookup(int id){Snapshot snapshot=current;return snapshot==null?null:snapshot.rows().get(id);}
 private NativeCancellationNpcRules(){}
}
