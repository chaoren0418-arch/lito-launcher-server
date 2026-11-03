package local.lito381.server;
import java.nio.file.*;
/** Invokes only the verified pure static predicate; never constructs a repository or datasource. */
public final class SourceReadonlyInventoryModeProbe {
 public static void main(String[]a)throws Exception{var method=LegacyMySqlRepository.class.getDeclaredMethod("nativeSourceOwnedInventoryMode",String.class,Object.class,boolean.class);method.setAccessible(true);int cases=0;for(boolean readonly:new boolean[]{false,true})for(String mode:new String[]{"lito_mysql","legacy_mysql","memory",""}){boolean actual=(boolean)method.invoke(null,mode,"lito_mysql",readonly);if(actual!=(!readonly&&mode.equals("lito_mysql")))throw new AssertionError("readonly safety and original writable-mode result");cases++;}Files.writeString(Path.of(a[0]),"{\"cases\":"+cases+",\"SQL_connections\":0,\"repository_instances\":0,\"created_Hub\":false,\"full_goal_achieved\":false}\n");System.out.println("PASS8 readonly/writable mode predicates without constructing a repository or SQL connection");}
}
