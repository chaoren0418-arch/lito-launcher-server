package local.lito381.server;
import java.sql.*;import java.util.*;
/** Source spawnlist_npc/spawnlist_time read in the existing readonly startup transaction. */
final class NativeStaticSpawnMetadata {
 record Row(int id,int npc,int count,int x,int y,int randomX,int randomY,int heading,int delay,int map,int movement,boolean timed) {
  boolean fixed(){return count>0&&randomX==0&&randomY==0&&heading>=0&&heading<8&&!timed;}
  long delayMillis(){return Math.max(0,delay*1000);}
 }
 private static volatile Map<Integer,Row> rows=Map.of();
 static void read(Connection c)throws SQLException {
  if(!c.isReadOnly()||c.getAutoCommit())throw new SQLException("static spawn metadata requires existing readonly transaction");var timed=new HashSet<Integer>();try(var s=c.createStatement();var r=s.executeQuery("SELECT npc_id FROM spawnlist_time")){while(r.next())timed.add(r.getInt(1));}
  var next=new HashMap<Integer,Row>();try(var s=c.createStatement();var r=s.executeQuery("SELECT id,npc_templateid,count,locx,locy,randomx,randomy,heading,respawn_delay,mapid,movement_distance FROM spawnlist_npc")){
   while(r.next()){var row=new Row(r.getInt(1),r.getInt(2),r.getInt(3),r.getInt(4),r.getInt(5),r.getInt(6),r.getInt(7),r.getInt(8),r.getInt(9),r.getInt(10),r.getInt(11),timed.contains(r.getInt(2)));if(row.id()<=0||next.put(row.id(),row)!=null)throw new SQLException("invalid or duplicate static spawn id");}
  }rows=Map.copyOf(next);
 }
 static Row row(ClassicWorldNpcCatalog.StaticNpc npc){var r=rows.get(npc.spawnId());if(r!=null&&(r.npc()!=npc.npcId()||r.map()!=npc.mapId()||r.x()!=npc.x()||r.y()!=npc.y()))throw new IllegalStateException("static NPC does not match its Source spawn owner");return r;}
 static int size(){return rows.size();}
 private NativeStaticSpawnMetadata(){}
}
