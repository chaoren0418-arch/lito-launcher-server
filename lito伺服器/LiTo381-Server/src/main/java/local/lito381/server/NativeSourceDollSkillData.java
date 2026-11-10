package local.lito381.server;
import java.sql.*;import java.util.*;
/** Immutable static skill metadata from the already owned, readonly startup transaction. */
final class NativeSourceDollSkillData {
 static final String SELECT="SELECT skill_id,skill_level,attr,castgfx,castgfx2,buffDuration,damage_value,damage_dice,damage_dice_count FROM skills";
 record Row(int id,int level,int attribute,int gfx,int secondGfx,int duration,int damage,int die,int dice){
  Row{if(id<=0||id>1000000||level<0||attribute<0||gfx<0||secondGfx<0||duration<0||damage<0||die<0||dice<0||dice>1000||dice>0&&die==0)throw new IllegalArgumentException("invalid Source doll skill metadata");}
 }
 private static volatile Map<Integer,Row> rows=Map.of();
 static void read(Connection connection)throws SQLException{
  if(!connection.isReadOnly()||connection.getAutoCommit())throw new SQLException("doll skills require the existing readonly startup transaction");
  var next=new HashMap<Integer,Row>();try(var statement=connection.createStatement();var result=statement.executeQuery(SELECT)){
   while(result.next()){int[] values=new int[9];for(int i=0;i<values.length;i++){values[i]=result.getInt(i+1);if(result.wasNull())throw new SQLException("null Source doll skill metadata");}
    final Row row;try{row=new Row(values[0],values[1],values[2],values[3],values[4],values[5],values[6],values[7],values[8]);}catch(IllegalArgumentException failure){throw new SQLException(failure);}
    if(next.putIfAbsent(row.id(),row)!=null)throw new SQLException("duplicate Source doll skill metadata");
   }
  }if(next.isEmpty())throw new SQLException("empty Source doll skill metadata");rows=Map.copyOf(next);
 }
 static Row row(int id){var row=rows.get(id);if(row==null)throw new IllegalStateException("Source doll skill metadata not loaded: "+id);return row;}
 static int level(int id){return Math.max(1,row(id).level());} // Both Source level0 and level1 enter the <=6 critical branch.
 private NativeSourceDollSkillData(){}
}
