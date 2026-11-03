import java.nio.file.*;import java.util.*;import java.util.jar.*;import org.objectweb.asm.tree.*;
/** Add two reviewed population methods; never replace the compiled population/nested actors. */
public final class PopulationReplacementMethods {
 static final String C="local/lito381/server/ClassicWorldMonsterPopulation";
 static final Set<String> NAMES=Set.of("nativeCancellationSpawn","nativeCancellationDelete");
 public static void main(String[]args)throws Exception {
  try(var old=new JarFile(args[1])) {
   var prior=NativeSkillConsumptionMethods.read(old.getInputStream(old.getJarEntry(C+".class")).readAllBytes());
   if(args[0].equals("splice")) {
    var donor=NativeSkillConsumptionMethods.read(Files.readAllBytes(Path.of(args[2]).resolve(C+".class")));
    for(String name:NAMES){if(prior.methods.stream().anyMatch(m->m.name.equals(name)))throw new AssertionError("duplicate replacement method");
     var method=donor.methods.stream().filter(m->m.name.equals(name)).findFirst().orElseThrow();prior.methods.add(method);
    }
    var path=Path.of(args[3]).resolve(C+".class");Files.createDirectories(path.getParent());Files.write(path,NativeSkillConsumptionMethods.bytes(prior,false));return;
   }
   try(var next=new JarFile(args[2])) {
    var actual=NativeSkillConsumptionMethods.read(next.getInputStream(next.getJarEntry(C+".class")).readAllBytes());
    if(actual.methods.stream().filter(m->NAMES.contains(m.name)).count()!=2)throw new AssertionError("replacement methods absent");
    actual.methods.removeIf(m->NAMES.contains(m.name));
    if(!Arrays.equals(NativeSkillConsumptionMethods.bytes(actual,true),NativeSkillConsumptionMethods.bytes(prior,true)))throw new AssertionError("existing population changed");
    System.out.println("PASS two additive population methods; all original fields/methods preserved");
   }
  }
 }
}
