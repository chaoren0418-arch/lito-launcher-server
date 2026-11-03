package local.lito381.server;
import java.nio.file.*;import java.util.*;
/** Read actual candidate controls without accounts, SQL writes, actors or listeners. */
public final class ControlAudit {
 public static void main(String[] args)throws Exception {
  var jar=Path.of(args[1]).toRealPath();
  if(!Path.of(CombatRules.class.getProtectionDomain().getCodeSource().getLocation().toURI()).toRealPath().equals(jar))throw new AssertionError("wrong candidate");
  CombatRules.load(ServerConfig.load(Path.of(args[0]),"standard"));
  var field=CombatRules.class.getDeclaredField("current");field.setAccessible(true);var snapshot=(CombatRules.Snapshot)field.get(null);
  var rows=new ArrayList<String>();rows.add("# Actual candidate combat snapshot; read-only; not proof of all consumers.");
  for(var entry:new TreeMap<>(snapshot.values()).entrySet())rows.add(entry.getKey()+"="+entry.getValue());
  Files.write(Path.of(args[2]),rows);System.out.println("PASS actual controls loaded="+snapshot.values().size()+"; defaults="+new TreeSet<>(snapshot.defaultedKeys()));
 }
}
