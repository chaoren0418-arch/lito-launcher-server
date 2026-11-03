package local.lito381.server;
import java.lang.reflect.*;
import java.nio.file.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
public final class NativeWarehouseRegressionMain {
 public static void main(String[] args)throws Exception {
  int passed=0;List<String> results=new ArrayList<>();
  for(String name:args){
   String[] target=name.split("#",2);Class<?> c=Class.forName("local.lito381.server."+target[0]);
   for(Method m:Arrays.stream(c.getDeclaredMethods()).sorted(Comparator.comparing(Method::getName)).toList())if(m.isAnnotationPresent(Test.class)&&(target.length==1||m.getName().equals(target[1]))){
    Constructor<?> constructor=c.getDeclaredConstructor();constructor.setAccessible(true);Object test=constructor.newInstance();
    for(Field f:c.getDeclaredFields())if(f.isAnnotationPresent(TempDir.class)){f.setAccessible(true);f.set(test,Files.createTempDirectory(Path.of("runtime"),"case-"));}
    m.setAccessible(true);
    try{m.invoke(test);passed++;results.add(name+"."+m.getName());}
    catch(InvocationTargetException failed){System.err.println("FAILED "+name+"."+m.getName());failed.getCause().printStackTrace();System.exit(1);}
   }
  }
  System.out.println("WAREHOUSE_TESTS_PASSED="+passed);
  for(String result:results)System.out.println("PASS "+result);
 }
}
