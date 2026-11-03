import java.nio.file.*;import java.nio.charset.*;import java.util.*;import java.sql.*;import java.security.*;
/** One-time provisioning. Source rows are never modified; secrets are never printed. */
public class ProvisionUser {
 public static void main(String[] args)throws Exception {
  Path source=Path.of(args[0]),destination=Path.of(args[1]);Properties p=new Properties();
  try(var r=Files.newBufferedReader(source,StandardCharsets.UTF_8)){p.load(r);}
  String user="lito_shared",password=HexFormat.of().formatHex(new SecureRandom().generateSeed(24));
  try(Connection c=DriverManager.getConnection(p.getProperty("URL1_LOGIN")+p.getProperty("URL2_LOGIN")+p.getProperty("URL3_LOGIN"),p.getProperty("Login_LOGIN",p.getProperty("Login")),p.getProperty("Password_LOGIN",p.getProperty("Password","")));Statement s=c.createStatement()) {
   // Plain CREATE intentionally refuses to replace an existing account.
   s.executeUpdate("CREATE USER '"+user+"'@'localhost' IDENTIFIED BY '"+password+"'");
   for(String db:List.of("380_db","380_lito","380_ac","380_ac_1","380_ac_2"))
    s.executeUpdate("GRANT SELECT ON `"+db.replace("_","\\_")+"`.* TO '"+user+"'@'localhost'");
   for(String db:List.of("380_ac","380_ac_1","380_ac_2"))
    s.executeUpdate("GRANT INSERT,UPDATE ON `"+db+"`.`character_lito` TO '"+user+"'@'localhost'");
  }
  String text=Files.readString(destination,StandardCharsets.UTF_8);
  Map<String,String> values=new LinkedHashMap<>();
  for(String key:List.of("Driver_LOGIN","URL1_LOGIN","URL3_LOGIN","URL1","URL3"))values.put(key,p.getProperty(key));
  values.put("URL2_LOGIN","380_ac");values.put("URL2","380_db");
  values.put("Login_LOGIN",user);values.put("Login",user);values.put("Password_LOGIN",password);values.put("Password",password);
  for(var e:values.entrySet())text=text.replaceAll("(?m)^"+e.getKey()+"=.*$",java.util.regex.Matcher.quoteReplacement(e.getKey()+"="+e.getValue()));
  Files.writeString(destination,text,StandardCharsets.UTF_8);
  System.out.println("Provisioned lito_shared: SELECT on five schemas; INSERT/UPDATE only on three character_lito tables. Credentials saved without display.");
 }
}
