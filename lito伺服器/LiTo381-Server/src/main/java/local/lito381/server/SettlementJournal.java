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
 /** Exactly the names {@link #save} gives its temporary files. */
 private static final java.util.regex.Pattern TEMPORARY=java.util.regex.Pattern.compile("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}\\.tmp");
 /** Operator void list (T136): one result id per line, optional reason after whitespace, '#' comments. */
 static final String VOID_REQUESTS="void-requests.txt",VOIDED_DIRECTORY="voided",VOID_AUDIT="void-audit.log";
 private final List<Path> removedTemporary,keptTemporary;
 SettlementJournal(Path directory)throws IOException{
  this.directory=directory.toAbsolutePath().normalize();Files.createDirectories(this.directory);
  lockChannel=FileChannel.open(this.directory.resolve("owner.lock"),StandardOpenOption.CREATE,StandardOpenOption.WRITE);
  try{lock=lockChannel.tryLock();if(lock==null)throw new IOException("settlement journal already owned");}catch(RuntimeException|IOException e){lockChannel.close();throw e;}
  long ownedSince=System.currentTimeMillis();
  try{
   List<Path> removed=new ArrayList<>(),kept=new ArrayList<>();cleanTemporary(ownedSince,removed,kept);removedTemporary=List.copyOf(removed);keptTemporary=List.copyOf(kept);
   // Refuse malformed committed files instead of silently dropping results.
   for(Path p:files())read(p);
  }catch(IOException|RuntimeException e){close();throw e;}
 }
 /**
  * T136: removes leftovers of a save() that died between the .tmp write and the rename. A .tmp file is deleted only if
  * (1) this process holds owner.lock, so no other process can be writing into the directory, (2) the name is exactly
  * save()'s {@code <UUID>.tmp}, (3) it is a regular file (not a link or directory) and (4) it was last modified no later
  * than the moment the lock was taken, so it predates every save() of this process. Anything else is kept and reported.
  */
 private void cleanTemporary(long ownedSince,List<Path> removed,List<Path> kept)throws IOException{
  List<Path> candidates;try(var s=Files.list(directory)){candidates=s.filter(p->p.getFileName().toString().endsWith(".tmp")).sorted().toList();}
  for(Path p:candidates){
   var a=Files.readAttributes(p,java.nio.file.attribute.BasicFileAttributes.class,LinkOption.NOFOLLOW_LINKS);
   if(!TEMPORARY.matcher(p.getFileName().toString()).matches()||!a.isRegularFile()||a.lastModifiedTime().toMillis()>ownedSince){kept.add(p);continue;}
   Files.deleteIfExists(p);removed.add(p);
  }
 }
 List<Path> removedTemporaryFiles(){return removedTemporary;}
 List<Path> keptTemporaryFiles(){return keptTemporary;}
 Path directory(){return directory;}
 /** Parses the operator void list; result id → reason. A malformed line refuses startup rather than being skipped. */
 static Map<String,String> voidRequests(Path file)throws IOException{
  Map<String,String> out=new LinkedHashMap<>();int number=0;
  for(String raw:Files.readAllLines(file,StandardCharsets.UTF_8)){number++;
   String line=(number==1&&raw.startsWith("\uFEFF")?raw.substring(1):raw).strip();if(line.isEmpty()||line.startsWith("#"))continue;
   String[] parts=line.split("\\s+",2);String id=parts[0].toLowerCase(Locale.ROOT),reason=parts.length>1?parts[1].strip():"identity changed in source database";
   if(!id.matches("[a-f0-9]{64}"))throw new IOException(file.getFileName()+" line "+number+": not a 64-hex result id");
   if(reason.length()>200||reason.chars().anyMatch(Character::isISOControl))throw new IOException(file.getFileName()+" line "+number+": reason must be one line of at most 200 characters");
   out.putIfAbsent(id,reason);
  }
  return out;
 }
 /** Copies (never moves) every committed file holding one of these results into {@code voided/} as the audit copy. */
 synchronized List<Path> archive(Set<String> resultIds)throws IOException{
  List<Path> copied=new ArrayList<>();if(resultIds.isEmpty())return copied;Path target=directory.resolve(VOIDED_DIRECTORY);
  for(Path p:files()){List<Entry> entries;try{entries=read(p);}catch(NoSuchFileException gone){continue;}
   if(entries.stream().noneMatch(e->resultIds.contains(e.id())))continue;
   Files.createDirectories(target);Path copy=target.resolve(p.getFileName());if(!Files.exists(copy))Files.copy(p,copy,StandardCopyOption.COPY_ATTRIBUTES);copied.add(copy);
  }
  return copied;
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
   // Another replayer already applied and deleted this file; the result ids make a repeat harmless.
   List<Entry> entries;try{entries=read(p);}catch(NoSuchFileException gone){continue;}catch(Exception e){if(first==null)first=e;else first.addSuppressed(e);continue;}
   try{sink.apply(entries);}catch(Exception e){if(first==null)first=e;else first.addSuppressed(e);continue;}
   Files.deleteIfExists(p);
  }
  if(first!=null)throw first;
 }
 synchronized int pending()throws IOException{return files().size();}
 public void close()throws IOException{lock.release();lockChannel.close();}
}
