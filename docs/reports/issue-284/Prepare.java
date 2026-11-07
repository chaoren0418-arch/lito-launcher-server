import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.lang.reflect.*;
public class Prepare {
 public static void main(String[] args) throws Exception {
  Path run=Path.of(args[0]);
  Properties secret=new Properties(), p=new Properties();
  try(var r=Files.newBufferedReader(Path.of(args[1]),StandardCharsets.UTF_8)){secret.load(r);}
  try(var r=Files.newBufferedReader(Path.of(args[2]),StandardCharsets.UTF_8)){p.load(r);}
  if(!"jdbc:mysql://127.0.0.1:43324/".equals(secret.getProperty("database.jdbc.base.url"))||!"lito83_exp".equals(secret.getProperty("database.user")))throw new IllegalStateException("Unexpected private handoff endpoint/account");
  for(String k:List.of("database.user","database.password","database.jdbc.options"))p.setProperty(k,Objects.requireNonNull(secret.getProperty(k)));
  p.setProperty("bind","127.0.0.1");p.setProperty("game.bind","127.0.0.2");p.setProperty("port","42083");
  p.setProperty("server.name","LiTo T014 P24 Experiment");p.setProperty("database.jdbc.base.url","jdbc:mysql://127.0.0.1:43324/");
  p.setProperty("database.static.database","p24_world_lito_exp");p.setProperty("database.lito.database","p24_lito_lito_exp");
  p.setProperty("capture.directory","../captures");p.setProperty("logs.directory","../logs");p.setProperty("startup.timeout.seconds","120");
  Class<?> c=Class.forName("local.lito381.server.UnifiedConfiguration");Field f=c.getDeclaredField("KEYS");f.setAccessible(true);
  if(!p.keySet().equals(f.get(null)))throw new IllegalStateException("Formal JAR KEYS mismatch");
  Method m=c.getDeclaredMethod("validate",Properties.class);m.setAccessible(true);m.invoke(null,p);
  try(var w=Files.newBufferedWriter(run.resolve("config/experiment.properties"),StandardCharsets.UTF_8,StandardOpenOption.CREATE_NEW)){p.store(w,"T014 isolated experiment; private, never commit");}
  p.setProperty("database.user","[REDACTED]");p.setProperty("database.password","[REDACTED]");
  try(var w=Files.newBufferedWriter(run.resolve("config-public.properties"),StandardCharsets.UTF_8,StandardOpenOption.CREATE_NEW)){for(String k:new TreeSet<>(p.stringPropertyNames()))w.write(k+"="+p.getProperty(k)+"\n");}
  System.out.println("formal_jar_keys="+p.size()+" validation=PASS secrets_not_printed=true");
 }
}
