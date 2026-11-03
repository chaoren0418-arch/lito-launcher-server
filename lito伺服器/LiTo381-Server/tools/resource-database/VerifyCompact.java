package local.lito381.server;
import java.nio.file.*;import java.sql.*;import java.util.*;
/** Read-only verification and fresh-install SQL export of the compact schema. */
public class VerifyCompact {
 static String literal(String value){return value==null?"NULL":value.isEmpty()?"''":"CONVERT(0x"+HexFormat.of().formatHex(value.getBytes(java.nio.charset.StandardCharsets.UTF_8))+" USING utf8)";}
 public static void main(String[] a)throws Exception{
  var p=UnifiedConfiguration.read(Path.of(a[0]));String db=SharedDatabase.identifier(p.getProperty("database.lito.database"));
  try(var c=DriverManager.getConnection(p.getProperty("database.jdbc.base.url")+db+p.getProperty("database.jdbc.options"),p.getProperty("database.user"),p.getProperty("database.password"));var s=c.createStatement()){
   c.setReadOnly(true);var wanted=new TreeSet<String>();for(var suffix:CompactResources.KEEP)wanted.add(CompactResources.PREFIX+suffix);
   if(!wanted.equals(CompactResources.tables(c)))throw new AssertionError("Not exactly ten resource tables");
   try(var r=s.executeQuery("SELECT category,COUNT(*) FROM lito_resource_pending GROUP BY category")){
    var actual=new HashMap<String,Integer>();while(r.next())actual.put(r.getString(1),r.getInt(2));if(!actual.equals(Map.of("pending",6,"archive",14,"conflict",2)))throw new AssertionError("Unified notes missing");
   }
   try(var r=s.executeQuery("SELECT COUNT(*) FROM lito_resource_pending WHERE evidence_note='' OR archive_ref='' OR evidence_status=''")){r.next();if(r.getInt(1)!=0)throw new AssertionError("Empty evidence notes");}
   try(var r=s.executeQuery("SELECT COUNT(*) FROM lito_resource_buff WHERE semantic_conflict=1 AND enabled<>0")){r.next();if(r.getInt(1)!=0)throw new AssertionError("Conflict enabled");}
   int total=0;
   try(var out=Files.newBufferedWriter(Path.of(a[1]))){
    out.write("-- LiTo 0.6.63 compact resources. Select the intended LiTo database first.\n-- Fresh install only: no DROP, no REPLACE; existing tables cause an error.\nSET NAMES utf8;\n");
    for(String table:wanted){
     try(var r=s.executeQuery("SHOW CREATE TABLE `"+table+"`")){r.next();out.write(r.getString(2)+";\n");}
     try(var r=s.executeQuery("SELECT * FROM `"+table+"`")){int n=r.getMetaData().getColumnCount();while(r.next()){var values=new ArrayList<String>();for(int i=1;i<=n;i++)values.add(literal(r.getString(i)));out.write("INSERT INTO `"+table+"` VALUES ("+String.join(",",values)+");\n");total++;}}
    }
   }
   System.out.println("PASS: 10 resource tables, "+total+" rows, 22 unified notes (6 pending/14 archive/2 conflict); read-only account; fresh-install SQL exported");
  }
 }
}
