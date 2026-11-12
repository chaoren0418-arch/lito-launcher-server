package local.lito381.server;
import java.io.*;import java.nio.channels.*;import java.nio.file.*;import java.nio.charset.StandardCharsets;import java.security.*;import java.util.*;
/** Compact write-ahead outcomes, independent of sockets, rooms and inventory. */
final class SettlementJournal implements AutoCloseable {
 record Entry(String id,String battle,String database,String source,String controls,long character,String name,boolean won,boolean disconnected) {
  Entry {if(!id.matches("[a-f0-9]{64}")||!battle.matches("[a-f0-9-]{36}")||character<=0||name.isBlank()||won&&disconnected)throw new IllegalArgumentException("invalid settlement");SharedDatabase.identifier(database);SharedDatabase.identifier(controls);}
  static String key(String battle,String db,long id){try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest((battle+"|"+db+"|"+id).getBytes(StandardCharsets.UTF_8)));}catch(Exception e){throw new IllegalStateException(e);}}
 }
 interface Sink {void apply(List<Entry> entries)throws Exception;}
 private final Path directory;private final FileChannel lockChannel;private final FileLock lock;
 SettlementJournal(Path directory)throws IOException{
  this.directory=directory.toAbsolutePath().normalize();Files.createDirectories(this.directory);
  lockChannel=FileChannel.open(this.directory.resolve("owner.lock"),StandardOpenOption.CREATE,StandardOpenOption.WRITE);
  try{lock=lockChannel.tryLock();if(lock==null)throw new IOException("settlement journal already owned");}catch(RuntimeException|IOException e){lockChannel.close();throw e;}
  // Refuse malformed committed files instead of silently dropping results.
  try{for(Path p:files())read(p);}catch(IOException|RuntimeException e){close();throw e;}
 }
 synchronized void save(List<Entry> entries)throws IOException{
  if(!lock.isValid())throw new IOException("settlement journal closed");
  if(entries.isEmpty())return;Properties p=new Properties();p.setProperty("format","1");p.setProperty("count",String.valueOf(entries.size()));
  for(int i=0;i<entries.size();i++){Entry e=entries.get(i);String k=i+".";p.setProperty(k+"id",e.id());p.setProperty(k+"battle",e.battle());p.setProperty(k+"database",e.database());p.setProperty(k+"source",e.source());p.setProperty(k+"controls",e.controls());p.setProperty(k+"character",String.valueOf(e.character()));p.setProperty(k+"name",e.name());p.setProperty(k+"won",String.valueOf(e.won()));p.setProperty(k+"disconnected",String.valueOf(e.disconnected()));}
  ByteArrayOutputStream bytes=new ByteArrayOutputStream();p.store(bytes,"LiTo frozen battle results; no credentials or inventory");
  String name=UUID.randomUUID().toString();Path tmp=directory.resolve(name+".tmp"),target=directory.resolve(name+".pending");
  try(FileChannel f=FileChannel.open(tmp,StandardOpenOption.CREATE_NEW,StandardOpenOption.WRITE)){var buffer=java.nio.ByteBuffer.wrap(bytes.toByteArray());while(buffer.hasRemaining())f.write(buffer);f.force(true);}
  Files.move(tmp,target,StandardCopyOption.ATOMIC_MOVE);
 }
 private List<Path> files()throws IOException{try(var s=Files.list(directory)){return s.filter(p->p.getFileName().toString().endsWith(".pending")).sorted().toList();}}
 private List<Entry> read(Path file)throws IOException{
  try{Properties p=new Properties();try(var in=Files.newInputStream(file)){p.load(in);}if(!"1".equals(p.getProperty("format")))throw new IOException("unsupported settlement journal");int count=Integer.parseInt(p.getProperty("count"));if(count<1||count>1000)throw new IOException("invalid settlement count");var out=new ArrayList<Entry>();
   for(int i=0;i<count;i++){String k=i+".";String won=p.getProperty(k+"won"),dc=p.getProperty(k+"disconnected");if(!Set.of("true","false").contains(won)||!Set.of("true","false").contains(dc))throw new IOException("invalid result boolean");Entry e=new Entry(p.getProperty(k+"id"),p.getProperty(k+"battle"),p.getProperty(k+"database"),p.getProperty(k+"source"),p.getProperty(k+"controls"),Long.parseLong(p.getProperty(k+"character")),p.getProperty(k+"name"),Boolean.parseBoolean(won),Boolean.parseBoolean(dc));if(!e.id().equals(Entry.key(e.battle(),e.database(),e.character())))throw new IOException("result identity mismatch");out.add(e);}return List.copyOf(out);
  }catch(RuntimeException e){throw new IOException("invalid settlement journal: "+file.getFileName(),e);}
 }
 /**
  * Applies every committed file; a file is deleted only after its sink call succeeded. A failing file
  * (e.g. a renamed source character) stays pending and no longer blocks the files after it (T124);
  * the first failure is rethrown after all files were attempted, later ones are suppressed onto it.
  */
 void replay(Sink sink)throws Exception{
  Exception first=null;
  for(Path p:files()){
   try{sink.apply(read(p));}catch(Exception e){if(first==null)first=e;else first.addSuppressed(e);continue;}
   Files.delete(p);
  }
  if(first!=null)throw first;
 }
 synchronized int pending()throws IOException{return files().size();}
 public void close()throws IOException{lock.release();lockChannel.close();}
}
