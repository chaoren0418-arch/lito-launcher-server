import java.nio.file.*;import java.nio.charset.*;import java.util.*;import java.sql.*;
public class InspectDatabase {
 public static void main(String[] args)throws Exception{
  Properties p=new Properties();try(var r=Files.newBufferedReader(Path.of(args[0]),StandardCharsets.UTF_8)){p.load(r);}
  try(Connection c=DriverManager.getConnection(p.getProperty("URL1_LOGIN")+p.getProperty("URL2_LOGIN")+p.getProperty("URL3_LOGIN"),p.getProperty("Login_LOGIN",p.getProperty("Login")),p.getProperty("Password_LOGIN",p.getProperty("Password","")))){
   c.setReadOnly(true);
   for(String sql:new String[]{"SELECT VERSION() AS version", "SELECT TABLE_SCHEMA,TABLE_NAME,ENGINE FROM information_schema.TABLES WHERE TABLE_SCHEMA IN ('380_ac','380_ac_1','380_ac_2','380_lito') AND (TABLE_NAME='character_lito' OR TABLE_SCHEMA='380_lito') ORDER BY TABLE_SCHEMA,TABLE_NAME", "SELECT TABLE_SCHEMA,COLUMN_NAME,COLUMN_TYPE FROM information_schema.COLUMNS WHERE TABLE_NAME='character_lito' AND TABLE_SCHEMA IN ('380_ac','380_ac_1','380_ac_2') ORDER BY TABLE_SCHEMA,ORDINAL_POSITION"})
    try(Statement s=c.createStatement();ResultSet rs=s.executeQuery(sql)){int n=rs.getMetaData().getColumnCount();while(rs.next()){for(int i=1;i<=n;i++)System.out.print(rs.getString(i)+(i==n?"\n":" | "));}}
   for(String db:List.of("380_ac","380_ac_1","380_ac_2"))try(Statement s=c.createStatement();ResultSet rs=s.executeQuery("SELECT COUNT(*) FROM `"+db+"`.`characters`")){rs.next();System.out.println(db+" characters="+rs.getLong(1));}
   for(String t:List.of("lito_battle_rule","lito_battle_object","lito_battle_spawn","lito_battle_player_spawn","lito_tournament_shop"))try(Statement s=c.createStatement();ResultSet rs=s.executeQuery("SELECT COUNT(*) FROM `380_lito`.`"+t+"`")){rs.next();System.out.println(t+" rows="+rs.getLong(1));}
  }
 }
}