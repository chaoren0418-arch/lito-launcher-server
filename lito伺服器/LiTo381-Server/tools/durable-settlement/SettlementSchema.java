package local.lito381.server;
import java.sql.*;import java.nio.file.*;import java.util.*;
public class SettlementSchema {
 static String ddl(String database){return "CREATE TABLE IF NOT EXISTS `"+SharedDatabase.identifier(database)+"`.`lito_battle_settlement` (result_id CHAR(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL PRIMARY KEY,sequence_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT UNIQUE,battle_id CHAR(36) CHARACTER SET ascii NOT NULL,source_database VARCHAR(64) NOT NULL,source_name VARCHAR(100) NOT NULL,char_obj_id BIGINT UNSIGNED NOT NULL,char_name VARCHAR(100) NOT NULL,won BOOLEAN NOT NULL,disconnected BOOLEAN NOT NULL,applied BOOLEAN NOT NULL DEFAULT 0,created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,applied_at TIMESTAMP NULL,INDEX pending_character(source_database,char_obj_id,applied,sequence_id)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin";}
 static void apply(Connection c,String controls,List<String> databases)throws Exception{
  try(var s=c.createStatement()){s.executeUpdate(ddl(controls));}
  for(String db:databases){SharedDatabase.identifier(db);boolean found;try(var p=c.prepareStatement("SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=? AND TABLE_NAME='character_lito' AND COLUMN_NAME='last_result_id'")){p.setString(1,db);try(var r=p.executeQuery()){r.next();found=r.getInt(1)>0;}}
   if(!found)try(var s=c.createStatement()){s.executeUpdate("ALTER TABLE `"+db+"`.`character_lito` ADD COLUMN last_result_id CHAR(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL DEFAULT '' COMMENT 'LiTo last atomically applied settlement receipt'");}
  }
 }
 static Connection connect(Properties p,String db)throws Exception{return DriverManager.getConnection(p.getProperty("database.jdbc.base.url")+db+p.getProperty("database.jdbc.options",""),p.getProperty("database.user"),p.getProperty("database.password"));}
 static Map<String,List<String>> snapshot(Connection c,List<String> databases)throws SQLException{
  Map<String,List<String>> out=new TreeMap<>();for(String db:databases){var rows=new ArrayList<String>();try(var s=c.createStatement();var r=s.executeQuery("SELECT char_obj_id,char_name,wins,losses,version,updated_at FROM `"+db+"`.character_lito ORDER BY char_obj_id")){while(r.next()){var values=new ArrayList<String>();for(int i=1;i<=6;i++)values.add(r.getString(i));rows.add(values.toString());}}out.put(db,rows);}return out;
 }
 public static void main(String[] args)throws Exception{
  Properties p=UnifiedConfiguration.read(Path.of(args[0]));String controls=SharedDatabase.identifier(p.getProperty("database.lito.database"));String runtimeIdentity;
  try(var c=connect(p,controls);var s=c.createStatement();var r=s.executeQuery("SELECT CURRENT_USER()")){r.next();runtimeIdentity=r.getString(1);}
  if(args.length>2){Properties admin=new Properties();try(var in=Files.newBufferedReader(Path.of(args[2]))){admin.load(in);}p.setProperty("database.user",admin.getProperty("Login_LOGIN"));p.setProperty("database.password",admin.getProperty("Password_LOGIN",""));}
  try(var c=connect(p,controls)){
   List<String> dbs=new ArrayList<>();try(var s=c.createStatement();var r=s.executeQuery("SELECT character_database FROM lito_source_server WHERE enabled=1 ORDER BY source_id")){while(r.next())dbs.add(SharedDatabase.identifier(r.getString(1)));}
   if(args.length>1&&args[1].equals("--apply")){
    var before=snapshot(c,dbs);apply(c,controls,dbs);
    if(args.length>2){String[] identity=runtimeIdentity.split("@",2);if(identity.length!=2||!identity[0].matches("[A-Za-z0-9_]+")||!identity[1].matches("[A-Za-z0-9_.%:-]+"))throw new SQLException("Unexpected runtime SQL identity");try(var s=c.createStatement()){s.executeUpdate("GRANT SELECT,INSERT,UPDATE ON `"+controls+"`.`lito_battle_settlement` TO '"+identity[0]+"'@'"+identity[1]+"'");}}
    if(!before.equals(snapshot(c,dbs)))throw new SQLException("Existing player results changed during maintenance");
    System.out.println("Settlement schema ready: "+controls+"; source marker columns="+dbs+"; existing results/version/timestamps unchanged; runtime receipt privileges SELECT/INSERT/UPDATE only");
   }
   else System.out.println("Settlement migration targets: "+controls+"; "+dbs);
  }
 }
}
