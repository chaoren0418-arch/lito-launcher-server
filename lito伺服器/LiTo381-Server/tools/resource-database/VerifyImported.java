package local.lito381.server;
import java.nio.file.*;import java.sql.*;import java.util.*;import java.security.*;import java.nio.charset.StandardCharsets;
public class VerifyImported {
 public static void main(String[] a)throws Exception{
  var p=UnifiedConfiguration.read(Path.of(a[0]));String db=SharedDatabase.identifier(p.getProperty("database.lito.database"));
  try(var c=DriverManager.getConnection(p.getProperty("database.jdbc.base.url")+db+p.getProperty("database.jdbc.options"),p.getProperty("database.user"),p.getProperty("database.password"));var s=c.createStatement()){
   c.setReadOnly(true);int total=0;
   for(String line:Files.readAllLines(Path.of(a[1]))){String[] v=line.split("\t");var hashes=new ArrayList<String>();String[] fields=v[1].split(",");
    if(!v[0].matches("lito_resource_[a-z_]+")||!v[1].matches("[a-z_0-9,]+"))throw new IllegalArgumentException("scope");
    try(var r=s.executeQuery("SELECT "+String.join(",",Arrays.stream(fields).map(f->"`"+f+"`").toList())+" FROM `"+v[0]+"`")){
     while(r.next()){var hash=MessageDigest.getInstance("SHA-256");for(int i=1;i<=fields.length;i++){
      String value=r.getString(i);if(value==null)hash.update("-1:".getBytes(StandardCharsets.UTF_8));else{byte[] bytes=value.getBytes(StandardCharsets.UTF_8);hash.update((bytes.length+":").getBytes(StandardCharsets.UTF_8));hash.update(bytes);}
     }hashes.add(HexFormat.of().formatHex(hash.digest()));}
    }
    Collections.sort(hashes);String actual=HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(String.join("",hashes).getBytes(StandardCharsets.UTF_8)));
    if(hashes.size()!=Integer.parseInt(v[2])||!actual.equals(v[3]))throw new AssertionError("roundtrip mismatch "+v[0]);total+=hashes.size();System.out.println("PASS "+v[0]+" rows="+hashes.size());
   }
   System.out.println("PASS full MySQL/JSON content comparison: "+total+" rows; activation/annotation columns verified separately");
  }
 }
}
