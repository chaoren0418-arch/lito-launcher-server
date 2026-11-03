package local.lito381.server;
import java.nio.file.*;import java.nio.charset.StandardCharsets;import java.sql.*;import java.util.*;import java.security.*;import java.util.zip.*;

/** Explicit, archived migration. Never runs at game startup. */
public class CompactResources {
 static final String PREFIX="lito_resource_";
 static final Set<String> KEEP=Set.of("skill_value","buff","projectile","map","zone","portal","npc_identity","map_page","release","pending");
 static String json(String s){if(s==null)return "null";var b=new StringBuilder("\"");for(char c:s.toCharArray())switch(c){case '"'->b.append("\\\"");case '\\'->b.append("\\\\");case '\n'->b.append("\\n");case '\r'->b.append("\\r");case '\t'->b.append("\\t");default->{if(c<32)b.append(String.format("\\u%04x",(int)c));else b.append(c);}}return b.append('"').toString();}
 static String hash(byte[] bytes)throws Exception{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));}
 static String digest(List<String> values)throws Exception {var b=new java.io.ByteArrayOutputStream();for(String v:values){if(v==null)b.write("-1:".getBytes(StandardCharsets.UTF_8));else{byte[] x=v.getBytes(StandardCharsets.UTF_8);b.write((x.length+":").getBytes(StandardCharsets.UTF_8));b.write(x);}}return hash(b.toByteArray());}
 static String tableHash(Connection c,String table,java.io.Writer out)throws Exception{
  var hashes=new ArrayList<String>();try(var st=c.createStatement();var rs=st.executeQuery("SELECT * FROM `"+table+"`")){
   var meta=rs.getMetaData();int n=meta.getColumnCount();
   while(rs.next()){var values=new ArrayList<String>();for(int i=1;i<=n;i++)values.add(rs.getString(i));hashes.add(digest(values));
    if(out!=null)out.write("{\"table\":"+json(table)+",\"values\":["+String.join(",",values.stream().map(CompactResources::json).toList())+"]}\n");
   }
  }Collections.sort(hashes);return hashes.size()+"\t"+hash(String.join("",hashes).getBytes(StandardCharsets.UTF_8));
 }
 static Set<String> tables(Connection c)throws Exception{
  var result=new TreeSet<String>();try(var q=c.prepareStatement("SELECT table_name FROM information_schema.tables WHERE table_schema=DATABASE() AND LEFT(table_name,14)=?")){q.setString(1,PREFIX);try(var rs=q.executeQuery()){while(rs.next())result.add(rs.getString(1));}}return result;
 }
 public static void main(String[] a)throws Exception{
  String mode=a[0];Path root=Path.of(a[3]);var p=UnifiedConfiguration.read(Path.of(a[1]));var credentials=new Properties();try(var r=Files.newBufferedReader(Path.of(a[2]))){credentials.load(r);}
  String db=SharedDatabase.identifier(p.getProperty("database.lito.database"));
  var expected=new LinkedHashSet<String>();for(String line:Files.readAllLines(root.resolve("original-counts.tsv"))){String name=line.split("\t")[0];if(!name.matches("lito_resource_[a-z_]+"))throw new IllegalArgumentException("Table scope");expected.add(name);}if(expected.size()!=24)throw new IllegalArgumentException("Expected 24 source tables");
  try(var c=DriverManager.getConnection(p.getProperty("database.jdbc.base.url")+db+p.getProperty("database.jdbc.options"),credentials.getProperty("Login_LOGIN"),credentials.getProperty("Password_LOGIN",""))){
   if(mode.equals("export")){
    if(!tables(c).equals(expected))throw new IllegalStateException("Resource table set changed");
    c.setReadOnly(true);c.setTransactionIsolation(Connection.TRANSACTION_REPEATABLE_READ);c.setAutoCommit(false);
    try(var data=Files.newBufferedWriter(root.resolve("data.jsonl"));var schema=Files.newBufferedWriter(root.resolve("schema.jsonl"));var manifest=Files.newBufferedWriter(root.resolve("snapshot.tsv"))){
     for(String table:expected){try(var st=c.createStatement();var rs=st.executeQuery("SHOW CREATE TABLE `"+table+"`")){rs.next();schema.write("{\"table\":"+json(table)+",\"ddl\":"+json(rs.getString(2))+"}\n");}
      manifest.write(table+"\t"+tableHash(c,table,data)+"\n");
     }
    }c.rollback();System.out.println("Exported consistent snapshot of 24 resource tables; no writes");return;
   }
   if(!mode.equals("apply"))throw new IllegalArgumentException("Unknown mode");
   Path archive=Path.of(a[4]);String archiveHash=a[5];if(!hash(Files.readAllBytes(archive)).equals(archiveHash))throw new IllegalStateException("Archive checksum mismatch");
   try(var z=new ZipFile(archive.toFile())){for(String name:List.of("data.jsonl","schema.jsonl","snapshot.tsv")){var entry=z.getEntry(name);if(entry==null||!Arrays.equals(z.getInputStream(entry).readAllBytes(),Files.readAllBytes(root.resolve(name))))throw new IllegalStateException("Archive content mismatch: "+name);}}
   if(!tables(c).equals(expected))throw new IllegalStateException("Resource table set changed; refuse destructive migration");
   for(String line:Files.readAllLines(root.resolve("snapshot.tsv"))){var v=line.split("\t",2);if(!tableHash(c,v[0],null).equals(v[1]))throw new IllegalStateException("Live table changed after archive: "+v[0]);}
   String stage="lito_stage063_pending",old="lito_archive063_pending";
   try(var st=c.createStatement()){
    st.execute("CREATE TABLE "+stage+" (id INT PRIMARY KEY,category VARCHAR(32) NOT NULL,source_table VARCHAR(96) NOT NULL,record_key VARCHAR(96) NULL,evidence_status VARCHAR(40) NOT NULL,evidence_note TEXT NOT NULL,known_evidence TEXT NOT NULL,missing_evidence TEXT NOT NULL,required_evidence TEXT NOT NULL,archive_ref VARCHAR(255) NOT NULL) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='LiTo 統一待補證據與封存資料備註'");
    int count=0;for(String line:Files.readAllLines(root.resolve("pending-inserts.sql"))){if(!line.startsWith("INSERT INTO `lito_stage063_pending` "))throw new IllegalArgumentException("Only staged notes inserts allowed");st.execute(line);count++;}
    try(var rs=st.executeQuery("SELECT COUNT(*) FROM "+stage)){rs.next();if(rs.getInt(1)!=count||count<20)throw new IllegalStateException("Notes incomplete");}
    st.execute("RENAME TABLE lito_resource_pending TO "+old+", "+stage+" TO lito_resource_pending");
    var removed=new ArrayList<String>();for(String table:expected)if(!KEEP.contains(table.substring(PREFIX.length())))removed.add("`"+table+"`");if(removed.size()!=14)throw new IllegalStateException("Drop scope");
    removed.add("`"+old+"`");st.execute("DROP TABLE "+String.join(",",removed));
    st.executeUpdate("UPDATE lito_resource_release SET note=CONCAT(note,' | compact-v1: 10 tables; research evidence archived; unified pending notes') WHERE release_id=1");
   }
   var wanted=new TreeSet<String>();for(String suffix:KEEP)wanted.add(PREFIX+suffix);if(!tables(c).equals(wanted))throw new IllegalStateException("Final table set mismatch");
   // Verify every runtime data table is byte-for-byte unchanged, except release.note.
   for(String line:Files.readAllLines(root.resolve("snapshot.tsv"))){var v=line.split("\t",2);if(wanted.contains(v[0])&&!Set.of(PREFIX+"pending",PREFIX+"release").contains(v[0])&&!tableHash(c,v[0],null).equals(v[1]))throw new IllegalStateException("Runtime data altered: "+v[0]);}
   try(var st=c.createStatement()){
    st.executeUpdate("UPDATE lito_resource_skill_value SET evidence_note=REPLACE(evidence_note,'欄位來源見 lito_resource_skill/provenance。','欄位來源見 lito_resource_pending 封存索引（原 skill/provenance 資料）。')");
   }
   System.out.println("PASS: 24 -> 10 resource tables; 14 research tables archived and removed; unified notes installed; runtime values unchanged, skill provenance text redirected to archive; no player tables modified");
  }
 }
}
