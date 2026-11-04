package local.lito381.server;
import java.sql.*;import java.util.*;
/** Immutable static skill data; shares the existing startup readonly transaction. */
final class NativeSkillTargetMetadata {
 record Row(String target,int targetTo,int ranged,int area,boolean through,int type,int attribute,int nativeId){}
 private static volatile Map<Integer,Row> rows=Map.of();
 static SourceSkillControls.Snapshot read(Connection connection)throws SQLException{
  if(!connection.isReadOnly()||connection.getAutoCommit())throw new SQLException("skill target snapshot requires the existing readonly startup transaction");
  var controls=SourceSkillControls.read(connection);var next=new HashMap<Integer,Row>();
  try(var s=connection.createStatement();var r=s.executeQuery("SELECT skill_id,target,target_to,ranged,area,through,type,attr,id FROM skills")){
   while(r.next()){int id=r.getInt(1);String target=r.getString(2);int targetTo=r.getInt(3),range=r.getInt(4),area=r.getInt(5),through=r.getInt(6),type=r.getInt(7),attribute=r.getInt(8),nativeId=r.getInt(9);
    if(id<=0||target==null||targetTo<0||range< -1||area< -1||through<0||through>1||type<0||attribute<0||nativeId<0||next.put(id,new Row(target,targetTo,range,area,through==1,type,attribute,nativeId))!=null)throw new SQLException("invalid or duplicate static skill target row");
   }
  }
  if(!next.containsKey(167))throw new SQLException("missing skill167 target row");rows=Map.copyOf(next);return controls;
 }
 static Row row(int skill){var result=rows.get(skill);if(result==null)throw new IllegalStateException("skill target metadata not loaded");return result;}
 static int size(){return rows.size();}
 static int windValue(int original,String field){var row=rows.get(167);if(row==null)return original;return switch(field){case "area"->row.area();case "targetTo"->row.targetTo();case "maximumRange"->row.ranged();case "attribute"->row.attribute();case "type"->row.type();case "nativeId"->row.nativeId();default->throw new IllegalArgumentException("unreviewed target field");};}
 private NativeSkillTargetMetadata(){}
}
