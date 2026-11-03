package local.lito381.server;
import java.nio.file.*;import java.sql.*;import java.util.*;import java.nio.charset.StandardCharsets;
public class TravelDatabase {
 public static void main(String[] a)throws Exception {
  var p=UnifiedConfiguration.read(Path.of(a[0]));String db=SharedDatabase.identifier(p.getProperty("database.lito.database")),source=SharedDatabase.identifier(p.getProperty("database.static.database"));
  Properties auth=p;if(a.length>1){auth=new Properties();try(var reader=Files.newBufferedReader(Path.of(a[1]),StandardCharsets.UTF_8)){auth.load(reader);}}
  String user=auth.getProperty("database.user",auth.getProperty("Login",auth.getProperty("Login_LOGIN"))),password=auth.getProperty("database.password",auth.getProperty("Password",auth.getProperty("Password_LOGIN","")));
  try(var c=DriverManager.getConnection(p.getProperty("database.jdbc.base.url")+db+p.getProperty("database.jdbc.options"),user,password);var st=c.createStatement()) {
   if(a.length>1){c.setAutoCommit(false);try {
    for(String[] row:new String[][]{{"loc","L1Loc","查詢座標：.loc；顯示 X/Y/地圖/方向"},{"move","L1Move","同地圖傳送：.move X Y [目前地圖ID]；不寫回來源角色庫"}}){
     try(var q=c.prepareStatement("INSERT INTO `"+db+"`.commands (name,access_level,class_name,note,system) SELECT ?,200,?,?,0 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `"+db+"`.commands WHERE name=?)")){q.setString(1,row[0]);q.setString(2,row[1]);q.setString(3,row[2]);q.setString(4,row[0]);q.executeUpdate();}
    }c.commit();
   }catch(Exception e){c.rollback();throw e;}}
   for(String sql:new String[]{"SELECT name,access_level,class_name,note,system FROM `"+db+"`.commands WHERE name IN ('loc','move')","SELECT item_id,name,classname,use_type FROM `"+source+"`.etcitem WHERE classname='wand.Storm_Walk'"})try(var rows=st.executeQuery(sql)){while(rows.next()){var out=new ArrayList<String>();for(int i=1;i<=rows.getMetaData().getColumnCount();i++)out.add(rows.getString(i));System.out.println(String.join(" | ",out));}}
  }
 }
}
