package local.lito381.server;
import java.nio.file.*;import java.sql.*;import java.util.*;
/** Diagnostic SELECT-only snapshot. No schema creation, migrations, or source writes. */
public final class CancellationSqlAudit {
 public static void main(String[] args)throws Exception{
  var cfg=ServerConfig.load(Path.of(args[0]),"standard");var p=UnifiedConfiguration.read(cfg.source());String db=SharedDatabase.identifier(p.getProperty("database.static.database"));var output=new StringBuilder();
  var queries=List.of("SELECT skill_id,name,`type`,target,probability_dice,probability_value FROM skills WHERE skill_id=44",
   "SELECT * FROM `skills_負面技能控制` WHERE skill_id=44","SELECT skill_id FROM `skills_魔屏無效技能` WHERE skill_id=44",
   "SELECT skill_id FROM `skills_相消免疫技能` ORDER BY skill_id","SELECT npc_id,transform_type,target_id,duration_seconds,new_name_id,new_passispeed FROM `skills_相消怪物轉換` ORDER BY npc_id");
  int rows=0;
  try(var c=DriverManager.getConnection(p.getProperty("database.jdbc.base.url")+db+p.getProperty("database.jdbc.options"),p.getProperty("database.user"),p.getProperty("database.password"))){
   c.setReadOnly(true);c.setAutoCommit(false);c.setTransactionIsolation(Connection.TRANSACTION_REPEATABLE_READ);
   try{for(var query:queries){if(!query.startsWith("SELECT "))throw new AssertionError();output.append(query).append('\n');try(var st=c.prepareStatement(query);var result=st.executeQuery()){
    var meta=result.getMetaData();for(int i=1;i<=meta.getColumnCount();i++){if(i>1)output.append('\t');output.append(meta.getColumnLabel(i));}output.append('\n');
    while(result.next()){rows++;for(int i=1;i<=meta.getColumnCount();i++){if(i>1)output.append('\t');var value=result.getString(i);output.append(value==null?"NULL":value.replace("\t"," ").replace("\n"," "));}output.append('\n');}
   }}}finally{c.rollback();}
  }
  output.append("read_only=true; rollback=true; source_mutated=false; rows=").append(rows).append('\n');Files.writeString(Path.of(args[1]),output);System.out.println("PASS SELECT-only cancellation SQL audit; rows="+rows);
 }
}
