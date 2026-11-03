// ORIGINAL SOURCE moved from lito伺服器/LiTo381-Server/tools/shared-database/UnifiedConfiguration.java (T002); replaces CFR decompile of reference/base-0.6.71.jar
package local.lito381.server;
import java.nio.file.*;import java.nio.charset.StandardCharsets;import java.sql.*;import java.util.*;

/** Single operator config; SQL owns source membership, not file mirrors. */
public final class UnifiedConfiguration {
 static final Set<String> KEYS=Set.of("bind","game.bind","port","server.name","database.jdbc.base.url","database.jdbc.options","database.user","database.password","database.static.database","database.lito.database","weight.rate","capture.directory","battle.ai.enabled","battle.physical.hit.enabled","battle.natural.mp.regen.enabled","native.siege.roster.enforced","native.tournament.allowance","memory.initial","memory.maximum","cpu.limit","logs.directory","startup.timeout.seconds");
 record Source(int id,String name,String database,boolean enabled) {}
 static Properties raw(Path file)throws Exception {
  Properties strict=new Properties(){@Override public synchronized Object put(Object k,Object v){if(containsKey(k))throw new IllegalArgumentException("Duplicate setting: "+k);return super.put(k,v);}};
  try(var reader=Files.newBufferedReader(file,StandardCharsets.UTF_8)){strict.load(reader);}Properties result=new Properties();result.putAll(strict);return result;
 }
 static Properties read(Path file)throws Exception {
  Properties p=raw(file);
  if(p.containsKey("configuration.base.file")) {
   for(String k:p.stringPropertyNames())if(!Set.of("configuration.base.file","capture.directory").contains(k))throw new IllegalArgumentException("Unsupported runtime override: "+k);
   Path source=file.getParent().resolve(p.getProperty("configuration.base.file")).normalize();Properties base=raw(source);validate(base);
   if(p.containsKey("capture.directory"))base.setProperty("capture.directory",p.getProperty("capture.directory"));
   base.setProperty("configuration.base.file",source.toAbsolutePath().toString());p=base;
  } else if(p.containsKey("database.jdbc.base.url")){validate(p);p.setProperty("configuration.base.file",file.toAbsolutePath().toString());}
  aliases(p);return p;
 }
 static void validate(Properties p) {
  for(String key:p.stringPropertyNames())if(!KEYS.contains(key))throw new IllegalArgumentException("Unknown/retired setting: "+key);
  for(String key:KEYS)if(!p.containsKey(key))throw new IllegalArgumentException("Missing setting: "+key);
  if(!p.getProperty("database.jdbc.base.url").startsWith("jdbc:mysql://")||!p.getProperty("database.jdbc.base.url").endsWith("/"))throw new IllegalArgumentException("Invalid database.jdbc.base.url");
  if(!p.getProperty("database.jdbc.options").startsWith("?"))throw new IllegalArgumentException("Invalid database.jdbc.options");
  for(String k:List.of("database.static.database","database.lito.database"))SharedDatabase.identifier(p.getProperty(k));
  if(p.getProperty("database.user").isBlank())throw new IllegalArgumentException("Empty database.user");
  double weight=Double.parseDouble(p.getProperty("weight.rate"));if(!Double.isFinite(weight)||weight<=0||weight>1000)throw new IllegalArgumentException("weight.rate must be >0 and <=1000");
  for(String k:List.of("battle.ai.enabled","battle.physical.hit.enabled","battle.natural.mp.regen.enabled","native.siege.roster.enforced"))if(!Set.of("true","false").contains(p.getProperty(k)))throw new IllegalArgumentException("Invalid boolean: "+k);
  number(p,"port",1024,65527);number(p,"cpu.limit",1,256);number(p,"startup.timeout.seconds",5,300);number(p,"native.tournament.allowance",0,1_000_000);
  if(memory(p.getProperty("memory.initial"))>memory(p.getProperty("memory.maximum")))throw new IllegalArgumentException("Initial heap exceeds maximum");
 }
 static long memory(String value){if(!value.matches("[1-9][0-9]*[mMgG]"))throw new IllegalArgumentException("Invalid heap size");long n=Long.parseLong(value.substring(0,value.length()-1));return Math.multiplyExact(n,value.toLowerCase(Locale.ROOT).endsWith("g")?1024L:1L);}
 static int number(Properties p,String k,int min,int max){int n=Integer.parseInt(p.getProperty(k));if(n<min||n>max)throw new IllegalArgumentException("Out of range: "+k);return n;}
 /** Legacy repository constructor only sees in-memory aliases; there is no second credentials file. */
 public static void aliases(Properties p) {
  if(!p.containsKey("database.jdbc.base.url"))return;
  for(String k:List.of("URL1_LOGIN","URL1"))p.setProperty(k,p.getProperty("database.jdbc.base.url"));
  for(String k:List.of("URL3_LOGIN","URL3"))p.setProperty(k,p.getProperty("database.jdbc.options"));
  for(String k:List.of("Login_LOGIN","Login"))p.setProperty(k,p.getProperty("database.user"));
  for(String k:List.of("Password_LOGIN","Password"))p.setProperty(k,p.getProperty("database.password"));
  p.setProperty("login.source.port.base",p.getProperty("port"));
  p.setProperty("database.persistence.policy",SharedDatabase.POLICY);p.setProperty("database.lito.character.table","character_lito");
 }
 static List<Source> sources(Properties p)throws Exception {
  Class.forName("com.mysql.cj.jdbc.Driver");List<Source> rows=new ArrayList<>();
  try(Connection c=DriverManager.getConnection(p.getProperty("database.jdbc.base.url")+SharedDatabase.identifier(p.getProperty("database.lito.database"))+p.getProperty("database.jdbc.options"),p.getProperty("database.user"),p.getProperty("database.password"));Statement s=c.createStatement()) {
   c.setReadOnly(true);s.setQueryTimeout(10);
   try(ResultSet rs=s.executeQuery("SELECT source_id,display_name,character_database,enabled FROM lito_source_server ORDER BY source_id")) {
    while(rs.next()){int enabled=rs.getInt(4);if(rs.wasNull()||enabled<0||enabled>1)throw new IllegalArgumentException("Invalid source enabled flag");rows.add(new Source(rs.getInt(1),rs.getString(2),rs.getString(3),enabled==1));}
   }
  }
  validateSources(rows);return rows.stream().filter(Source::enabled).toList();
 }
 static void validateSources(List<Source> rows) {
  Set<Integer> ids=new HashSet<>();Set<String> names=new HashSet<>(),dbs=new HashSet<>();int active=0;
  for(Source r:rows){if(r.id<1||r.id>8||!ids.add(r.id))throw new IllegalArgumentException("Source ID must be unique 1..8");if(!r.enabled)continue;active++;
   if(r.name==null||r.name.isBlank()||r.name.contains(",")||r.name.getBytes(java.nio.charset.Charset.forName("MS950")).length>63||!java.nio.charset.Charset.forName("MS950").newEncoder().canEncode(r.name)||r.name.chars().anyMatch(Character::isISOControl)||!names.add(r.name.toLowerCase(Locale.ROOT)))throw new IllegalArgumentException("Invalid/duplicate source name");
   if(!dbs.add(SharedDatabase.identifier(r.database).toLowerCase(Locale.ROOT)))throw new IllegalArgumentException("Duplicate source database");
  }
  if(active==0)throw new IllegalArgumentException("No enabled source servers");
 }
 public static void loaded(Properties p,Path file) {
  if(p.containsKey("database.jdbc.base.url")&&!p.containsKey("configuration.base.file"))p.setProperty("configuration.base.file",file.toAbsolutePath().normalize().toString());
 }
 static void apply(Properties target,Path base)throws Exception {
  Path file=target.containsKey("configuration.base.file")?base.resolve(target.getProperty("configuration.base.file")):base.resolve("server.properties");
  Properties p=read(file);if(target.containsKey("configuration.base.file")&&target.containsKey("capture.directory"))p.setProperty("capture.directory",target.getProperty("capture.directory"));
  List<Source> rows=sources(p);String canonical=p.getProperty("configuration.base.file",file.toAbsolutePath().toString());
  target.clear();target.putAll(p);target.setProperty("database.connection.properties",canonical.replace('\\','/'));
  target.setProperty("database.mode","lito_mysql");target.setProperty("database.read.only","true");target.setProperty("state.persistence.enabled","false");
  target.setProperty("classic.world.compatibility.enabled","false");target.setProperty("classic.world.monster.ai.enabled","false");target.setProperty("battle.compatibility.service.actors.enabled","false");
  target.setProperty("native.tournament.economy.enabled","true");target.setProperty("lito.rental.town.directory.enabled","false");target.setProperty("lito.rental.teleport.routes","");
  target.setProperty("lobby.server.select.response.delay.ms","0");target.setProperty("lobby.enter.channel.response.delay.ms","0");
  target.setProperty("login.bootstrap.sequence","login_check,classic_server_list,lito_server_list");target.setProperty("login.bootstrap.step.delay.ms","250");target.setProperty("lobby.character.list.enabled","true");
  target.setProperty("origin.source.id","lito-shared");target.setProperty("origin.source.name",target.getProperty("server.name"));
  target.setProperty("advertised.host",target.getProperty("game.bind"));target.setProperty("advertised.port",target.getProperty("port"));target.setProperty("login.source.port.base",target.getProperty("port"));
  target.setProperty("state.file",Path.of(canonical).getParent().resolve("../runtime/shared-database-memory/server.state").normalize().toString().replace('\\','/'));
  for(String key:List.of("capture.directory","logs.directory"))target.setProperty(key,Path.of(canonical).getParent().resolve(target.getProperty(key)).normalize().toString().replace('\\','/'));
  target.setProperty("login.classic.server.names",String.join(",",rows.stream().map(Source::name).toList()));
  target.setProperty("login.classic.server.ids",String.join(",",rows.stream().map(x->Integer.toString(x.id)).toList()));
  target.setProperty("database.dynamic.databases",String.join(",",rows.stream().map(Source::database).toList()));
  target.setProperty("database.source.weight.rates",String.join(",",Collections.nCopies(rows.size(),target.getProperty("weight.rate"))));
 }
 static String quote(String s){StringBuilder b=new StringBuilder("\"");for(char c:s.toCharArray()){if(c=='"'||c=='\\')b.append('\\').append(c);else if(c<32)b.append(String.format("\\u%04x",(int)c));else b.append(c);}return b.append('"').toString();}
 public static void main(String[] args) {
  try {Path path=Path.of(args[0]).toAbsolutePath();Properties p=raw(path);loaded(p,path);apply(p,path.getParent());StringJoiner settings=new StringJoiner(",","{","}");
   for(String k:new TreeSet<>(p.stringPropertyNames()))if(!k.startsWith("database.jdbc.")&&!k.equals("database.password")&&!k.equals("database.user")&&!k.startsWith("Password")&&!k.startsWith("Login")&&!k.startsWith("URL"))settings.add(quote(k)+":"+quote(p.getProperty(k)));
   String[] ids=p.getProperty("login.classic.server.ids").split(","),names=p.getProperty("login.classic.server.names").split(","),dbs=p.getProperty("database.dynamic.databases").split(",");StringJoiner routes=new StringJoiner(",","[","]");
   for(int i=0;i<ids.length;i++)routes.add("{\"id\":"+ids[i]+",\"name\":"+quote(names[i])+",\"database\":"+quote(dbs[i])+",\"port\":"+(Integer.parseInt(p.getProperty("port"))+Integer.parseInt(ids[i]))+"}");
   System.out.println("{\"settings\":"+settings+",\"routes\":"+routes+"}");
  }catch(Exception e){System.err.println("LiTo configuration unavailable: "+e.getClass().getSimpleName()+ (e instanceof IllegalArgumentException ? " "+e.getMessage():"; check database connectivity, permissions and lito_source_server"));System.exit(1);}
 }
}
