package local.lito381.server;
import java.nio.file.*;import java.sql.*;import java.util.*;import java.nio.charset.StandardCharsets;
public class Database {
 public static void main(String[] a)throws Exception {
  var p=UnifiedConfiguration.read(Path.of(a[0]));String db=SharedDatabase.identifier(p.getProperty("database.lito.database")),source=SharedDatabase.identifier(p.getProperty("database.static.database"));
  Properties auth=p;
  if(a.length>1){auth=new Properties();try(var reader=Files.newBufferedReader(Path.of(a[1]),StandardCharsets.UTF_8)){auth.load(reader);}}
  String user=auth.getProperty("database.user",auth.getProperty("Login",auth.getProperty("Login_LOGIN"))),password=auth.getProperty("database.password",auth.getProperty("Password",auth.getProperty("Password_LOGIN","")));
  try(var c=DriverManager.getConnection(p.getProperty("database.jdbc.base.url")+db+p.getProperty("database.jdbc.options"),user,password);var st=c.createStatement()) {
   if(a.length>1)try(var q=c.prepareStatement("INSERT INTO `"+db+"`.commands (name,access_level,class_name,note,system) SELECT 'polyme',200,'L1PolyMe','自身變身：.polyme 外型代號；7200秒；不寫回角色庫',0 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `"+db+"`.commands WHERE name='polyme')")){System.out.println("LiTo command rows inserted: "+q.executeUpdate());}
   c.setReadOnly(true);
   for(String sql:new String[]{"SELECT name,access_level,class_name,note,system FROM `"+db+"`.commands WHERE name='polyme'","SELECT name,polyid,minlevel,weaponequip,armorequip,isSkillUse,cause FROM `"+source+"`.polymorphs WHERE polyid IN (29,6080,378,4000)"})try(var rows=st.executeQuery(sql)){while(rows.next()){var out=new ArrayList<String>();for(int i=1;i<=rows.getMetaData().getColumnCount();i++)out.add(rows.getString(i));System.out.println(String.join(" | ",out));}}
   var form=GmPolyMe.readForm(c,"`"+source+"`.polymorphs",6080);System.out.println("PASS shared SQL form "+form.gfx()+" "+form.selection());
  }
 }
}
