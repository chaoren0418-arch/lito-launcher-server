package local.lito381.server;
import java.nio.file.*;import java.sql.*;
/** Explicit maintenance only; server startup never creates or overwrites policy rows. */
public class RoomPolicySql {
 public static void main(String[] a)throws Exception{
  var p=UnifiedConfiguration.read(Path.of(a[0]));String db=SharedDatabase.identifier(p.getProperty("database.lito.database"));
  if(a.length==4 && a[1].equals("apply")) {
   var admin=new java.util.Properties();try(var reader=Files.newBufferedReader(Path.of(a[3]))){admin.load(reader);}
   p.setProperty("database.user",admin.getProperty("Login_LOGIN"));p.setProperty("database.password",admin.getProperty("Password_LOGIN",""));
  }
  try(var c=DriverManager.getConnection(p.getProperty("database.jdbc.base.url")+db+p.getProperty("database.jdbc.options"),p.getProperty("database.user"),p.getProperty("database.password"));var s=c.createStatement()){
   if(a[1].equals("apply")){
    String sql=Files.readString(Path.of(a[2])).replace("`380_lito`","`"+db+"`");
    sql=sql.replaceAll("(?m)^--.*$","");for(String statement:sql.split(";"))if(!statement.isBlank())s.execute(statement);
   }
   try(var r=s.executeQuery("SELECT mode_id,MIN(minimum_players),MAX(minimum_players),COUNT(*) FROM lito_battle_rule GROUP BY mode_id ORDER BY mode_id")){while(r.next())System.out.println("mode="+r.getInt(1)+" minimum="+r.getInt(2)+".."+r.getInt(3)+" maps="+r.getInt(4));}
   try(var r=s.executeQuery("SELECT policy_id,test_room_enabled,test_bot_enabled FROM lito_room_policy")){while(r.next())System.out.println("policy="+r.getInt(1)+" defaultRoom="+r.getInt(2)+" bot="+r.getInt(3));}
  }
 }
}
