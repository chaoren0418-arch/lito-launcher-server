package local.lito381.server;
import java.nio.file.*;import java.sql.*;import java.util.*;
/** One-time migration: creates only LiTo control table; never touches character data. */
public class MigrateSources {
 public static void main(String[] args)throws Exception {
  Properties admin=UnifiedConfiguration.raw(Path.of(args[0])),old=UnifiedConfiguration.raw(Path.of(args[1]));String schema=SharedDatabase.identifier(args[2]);
  String ddl="CREATE TABLE IF NOT EXISTS `"+schema+"`.lito_source_server (source_id TINYINT UNSIGNED NOT NULL PRIMARY KEY COMMENT '固定來源編號 1~8，勿重編既有來源',display_name VARCHAR(45) NOT NULL COMMENT '原生選服顯示名稱',character_database VARCHAR(64) NULL COMMENT '角色來源資料庫，未啟用可留 NULL',enabled TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '0 關閉；1 開放，下次啟動生效',UNIQUE KEY uq_character_database(character_database),CONSTRAINT chk_lito_source_id CHECK(source_id BETWEEN 1 AND 8),CONSTRAINT chk_lito_source_enabled CHECK(enabled IN (0,1))) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='LiTo 來源伺服器清單；戰鬥可跨服，登入資料依此分流'";
  try(Connection c=DriverManager.getConnection(admin.getProperty("URL1_LOGIN")+schema+admin.getProperty("URL3_LOGIN",""),admin.getProperty("Login_LOGIN",admin.getProperty("Login")),admin.getProperty("Password_LOGIN",admin.getProperty("Password","")));Statement st=c.createStatement()) {
   st.executeUpdate(ddl);
   try(PreparedStatement q=c.prepareStatement("INSERT INTO `"+schema+"`.lito_source_server(source_id,display_name,character_database,enabled) SELECT ?,?,?,? FROM DUAL WHERE NOT EXISTS(SELECT 1 FROM `"+schema+"`.lito_source_server WHERE source_id=?)")) {
    for(int i=1;i<=8;i++){String pre="source."+i+".",db=old.getProperty(pre+"database","");q.setInt(1,i);q.setString(2,old.getProperty(pre+"name"));q.setString(3,db.isBlank()?null:db);q.setInt(4,Boolean.parseBoolean(old.getProperty(pre+"enabled"))?1:0);q.setInt(5,i);q.executeUpdate();}
   }
   try(ResultSet rows=st.executeQuery("SELECT source_id,display_name,character_database,enabled FROM `"+schema+"`.lito_source_server ORDER BY source_id")){while(rows.next())System.out.println(rows.getInt(1)+" "+rows.getString(2)+" "+rows.getString(3)+" enabled="+rows.getInt(4));}
  }
  System.out.println("PASS source settings migrated; existing rows preserved; no player tables modified");
 }
}
