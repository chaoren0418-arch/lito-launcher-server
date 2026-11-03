package local.lito381.server;
import java.nio.file.*;import java.sql.*;import java.util.*;
/** Static-only SELECT on the already-owned readonly bootstrap connection; never creates a Hub. */
public final class StaticRespawnMetadataAuditProbe {
 static final List<String> rows=new ArrayList<>();static int count;static boolean inspected;static String tableStatus;
 public static void inspect(Connection c)throws SQLException {
  if(!c.isReadOnly()||c.getAutoCommit())throw new AssertionError("existing readonly transaction only");inspected=true;
  rows.add("spawn_id\tnpc_id\tcount\tx\ty\trandom_x\trandom_y\theading\trespawn_delay\tmap\tmovement_distance");
  try(var s=c.createStatement();var r=s.executeQuery("SELECT id,npc_templateid,count,locx,locy,randomx,randomy,heading,respawn_delay,mapid,movement_distance FROM spawnlist_npc ORDER BY id")){
   while(r.next()){var out=new ArrayList<String>();for(int n=1;n<=11;n++)out.add(Integer.toString(r.getInt(n)));rows.add(String.join("\t",out));count++;}tableStatus="read-shared-static-spawnlist_npc";
  }catch(SQLException unavailable){tableStatus="unavailable-state-"+unavailable.getSQLState()+"-code-"+unavailable.getErrorCode();throw unavailable;}
 }
 public static void main(String[]args)throws Exception {var cfg=ServerConfig.load(Path.of(args[0]),"standard");ResourceDatabase.load(cfg);if(!inspected)throw new AssertionError("audit hook not reached");Files.write(Path.of(args[1]),rows);System.out.println("STATIC_RESPAWN_ROWS="+count);Files.writeString(Path.of(args[1]).resolveSibling("observations.json"),"{\"rows\":"+count+",\"created_Hub\":false,\"used_existing_readonly_transaction\":true,\"Source_or_player_SQL_or_WAL_written\":false,\"full_goal_achieved\":false}\n");}
}
