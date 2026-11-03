package local.lito381.server;
import java.nio.file.*;
import java.sql.*;
import java.util.*;
/** Read-only static weapon templates only. Never opens repositories or player tables. */
public class ProbeIncomingEquipment {
 public static void main(String[] args)throws Exception{
  var config=ServerConfig.load(Path.of(args[0]),"standard");var p=SharedDatabase.properties(config.legacySqlProperties());var rows=new ArrayList<String>();
  String columns="item_id,name_id,type,dmg_small,dmg_large,material,weight,safenchant,invgfx,grdgfx,bless,trade,itemdesc_id,cant_delete,use_royal,use_knight,use_elf,use_mage,use_darkelf,use_dragonknight,use_illusionist,`range`,hitmodifier,dmgmodifier";rows.add("database\t"+columns.replace("`","").replace(',', '\t'));
  for(String database:new LinkedHashSet<>(config.legacySourceResourceDatabases())){
   if(!database.matches("[A-Za-z0-9_]+"))throw new IllegalArgumentException("invalid configured static database");
   try(Connection c=DriverManager.getConnection(p.getProperty("URL1_LOGIN")+database+p.getProperty("URL3_LOGIN",""),p.getProperty("Login_LOGIN",p.getProperty("Login")),p.getProperty("Password_LOGIN",p.getProperty("Password","")))){
    c.setReadOnly(true);c.setAutoCommit(false);
    try(Statement s=c.createStatement();ResultSet r=s.executeQuery("SELECT "+columns+" FROM `weapon` WHERE item_id IN (145,149) ORDER BY item_id")){
     while(r.next()){var values=new ArrayList<String>();values.add(database);for(int i=1;i<=24;i++){String value=r.getString(i);if(value==null||value.contains("\t")||value.contains("\n")||value.contains("\r"))throw new IllegalStateException("invalid static template cell");values.add(value);}rows.add(String.join("\t",values));}
    }finally{c.rollback();}
   }catch(SQLException e){throw new IllegalStateException("read-only static weapon probe SQLState="+e.getSQLState()+" code="+e.getErrorCode());}
  }
  Files.write(Path.of(args[1]),rows);for(String row:rows)System.out.println(row);
 }
}
