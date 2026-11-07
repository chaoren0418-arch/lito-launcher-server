import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.util.*;
public class Probe {
 static String scalar(Connection c,String sql)throws Exception{try(var s=c.createStatement()){s.setQueryTimeout(10);try(var r=s.executeQuery(sql)){if(!r.next())throw new IllegalStateException("No scalar");return r.getString(1);}}}
 static void require(boolean b,String s){if(!b)throw new IllegalStateException(s);}
 public static void main(String[] args)throws Exception{
  Properties p=new Properties();try(var r=Files.newBufferedReader(Path.of(args[0]),StandardCharsets.UTF_8)){p.load(r);}
  require(p.getProperty("database.jdbc.base.url").equals("jdbc:mysql://127.0.0.1:43324/"),"endpoint mismatch");
  try(Connection c=DriverManager.getConnection(p.getProperty("database.jdbc.base.url")+"p24_lito_lito_exp"+p.getProperty("database.jdbc.options"),p.getProperty("database.user"),p.getProperty("database.password"))){
   c.setReadOnly(true);
   require(scalar(c,"SELECT CURRENT_USER()").equals("lito83_exp@127.0.0.1"),"user mismatch");
   require(scalar(c,"SELECT @@port").equals("43324"),"port mismatch");
   require(scalar(c,"SELECT @@server_uuid").equals("e3443e40-d090-11f1-8040-4cedfb679789"),"UUID mismatch");
   require(scalar(c,"SELECT @@datadir").replace('\\','/').equalsIgnoreCase("D:/BinEngine/environments/p24/db-data/"),"datadir mismatch");
   require(scalar(c,"SELECT @@bind_address").equals("127.0.0.1"),"bind mismatch");
   System.out.println("utc="+java.time.Instant.now()+" identity=PASS port=43324 uuid=e3443e40-d090-11f1-8040-4cedfb679789 bind=127.0.0.1");
   require(scalar(c,"SELECT COUNT(*) FROM p24_lito_lito_exp.lito_source_server").equals("1"),"unexpected route count");
   try(var s=c.createStatement();var r=s.executeQuery("SELECT source_id,character_database,enabled FROM p24_lito_lito_exp.lito_source_server ORDER BY source_id")){
    while(r.next()){System.out.println("route="+r.getInt(1)+","+r.getString(2)+","+r.getInt(3));require(r.getInt(1)==1&&r.getString(2).equals("p24_accounts_lito_exp")&&r.getInt(3)==1,"route mismatch; stop for review");}
   }
   System.out.println("character_lito="+scalar(c,"SELECT COUNT(*) FROM p24_accounts_lito_exp.character_lito"));
   String count=scalar(c,"SELECT COUNT(*) FROM p24_lito_lito_exp.lito_battle_settlement");System.out.println("lito_battle_settlement="+count);
   require(count.equals("0"),"Existing receipts require review before startup");
  }
 }
}
