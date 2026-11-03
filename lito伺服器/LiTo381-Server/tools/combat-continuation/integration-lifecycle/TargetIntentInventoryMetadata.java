package local.lito381.server;
import java.sql.*;import java.util.*;
/** Audit-only static metadata, read through the existing startup readonly transaction. */
final class TargetIntentInventoryMetadata {
 record Row(int id,String name,int level,int number,String target,int targetTo,int range,int area,boolean through,int type,int attribute,int nativeId,int action,int gfx){}
 static volatile Map<Integer,Row> rows=Map.of();
 static SourceSkillControls.Snapshot read(Connection connection)throws SQLException{
  if(!connection.isReadOnly()||connection.getAutoCommit())throw new SQLException("inventory requires the existing readonly startup transaction");
  var controls=NativeSkillTargetMetadata.read(connection);var next=new TreeMap<Integer,Row>();
  try(var s=connection.createStatement();var r=s.executeQuery("SELECT skill_id,name,skill_level,skill_number,target,target_to,ranged,area,through,type,attr,id,action_id,castgfx FROM skills ORDER BY skill_id")){
   while(r.next()){int id=r.getInt(1);var row=new Row(id,r.getString(2),r.getInt(3),r.getInt(4),r.getString(5),r.getInt(6),r.getInt(7),r.getInt(8),r.getBoolean(9),r.getInt(10),r.getInt(11),r.getInt(12),r.getInt(13),r.getInt(14));if(row.name()==null||next.put(id,row)!=null)throw new SQLException("invalid static inventory row");}
  }
  rows=Collections.unmodifiableMap(next);return controls;
 }
}
