import java.nio.file.*;import java.nio.charset.*;import java.util.*;import java.sql.*;
public class InspectItems {
 public static void main(String[] args)throws Exception {
  Properties config=new Properties(),db=new Properties();try(var r=Files.newBufferedReader(Path.of(args[0]),StandardCharsets.UTF_8)){config.load(r);}try(var r=Files.newBufferedReader(Path.of(config.getProperty("database.connection.properties")),StandardCharsets.UTF_8)){db.load(r);}
  String schema=config.getProperty("database.static.database");if(!schema.matches("[a-zA-Z0-9_]+"))throw new Exception();
  try(var cn=DriverManager.getConnection(db.getProperty("URL1_LOGIN")+schema+db.getProperty("URL3_LOGIN"),db.getProperty("Login_LOGIN"),db.getProperty("Password_LOGIN"));var st=cn.createStatement()){
   cn.setReadOnly(true);
   try(var rows=st.executeQuery("SELECT * FROM `道具_物品詳細說明` WHERE Item_Id=-1 OR Item_Id BETWEEN 88888 AND 88893")){var md=rows.getMetaData();while(rows.next()){for(int i=1;i<=md.getColumnCount();i++)System.out.println(md.getColumnLabel(i)+"="+rows.getString(i));}}
   for(String table:List.of("armor","weapon")){
    try(var rows=st.executeQuery("SELECT * FROM "+table+" WHERE name LIKE '%GM%'")){var md=rows.getMetaData();System.out.println("TABLE "+table);for(int i=1;i<=md.getColumnCount();i++)System.out.print(md.getColumnLabel(i)+",");System.out.println();while(rows.next()){for(int i=1;i<=md.getColumnCount();i++){Object v=rows.getObject(i);if(v!=null&&!v.toString().equals("0")&&!v.toString().isBlank())System.out.println(md.getColumnLabel(i)+"="+v);}System.out.println("END");}}
   }
  }
 }
}
