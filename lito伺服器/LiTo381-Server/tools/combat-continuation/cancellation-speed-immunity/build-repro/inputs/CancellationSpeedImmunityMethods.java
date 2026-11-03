import java.nio.file.*;import java.util.*;import java.util.jar.*;import org.objectweb.asm.tree.*;
/** Preserve the complete parent except for two explicit player-cancellation call sites. */
public final class CancellationSpeedImmunityMethods {
 static final String P="local/lito381/server/",KEY=P+"ClientSession.class",FROM=P+"CombatPoisonLifecycle",TO=P+"NativeCancellationSpeedImmunity";
 static final Set<String> METHODS=Set.of("nativeMaterialCancellationBody","nativeMaterialWorldCancellationBody");
 static byte[] norm(ClassNode n){return NativeSkillConsumptionMethods.bytes(n,true);}
 static ClassNode read(byte[] b){return NativeSkillConsumptionMethods.read(b);}
 static void redirect(ClassNode c,String from,String to){
  int count=0;
  for(var m:c.methods)if(METHODS.contains(m.name)){
   int local=0;
   for(var n:m.instructions.toArray())if(n instanceof MethodInsnNode call&&call.owner.equals(from)&&call.name.equals("magic")){
    if(!call.desc.equals("(L"+P+"BattleTimedStatus;L"+P+"ClientSession;L"+P+"CombatPoisonLifecycle$Scope;)L"+P+"BattleTimedStatus$MagicCancellation;"))throw new AssertionError("unknown magic contract");
    call.owner=to;local++;count++;
   }
   if(local!=1)throw new AssertionError("unexpected call count "+m.name+" "+local);
  }
  if(count!=2)throw new AssertionError("missing explicit player cancellation callers");
 }
 public static void main(String[] args)throws Exception{
  try(var parent=new JarFile(args[1])){
   var raw=parent.getInputStream(parent.getJarEntry(KEY)).readAllBytes();var projected=read(raw);redirect(projected,FROM,TO);
   if(args[0].equals("splice")){var p=Path.of(args[2]).resolve(KEY);Files.createDirectories(p.getParent());Files.write(p,NativeSkillConsumptionMethods.bytes(projected,false));return;}
   try(var next=new JarFile(args[2])){
    var actual=read(next.getInputStream(next.getJarEntry(KEY)).readAllBytes());if(!Arrays.equals(norm(actual),norm(projected)))throw new AssertionError("unexpected changes");
    redirect(actual,TO,FROM);if(!Arrays.equals(norm(actual),norm(read(raw))))throw new AssertionError("inverse failed");
    for(var e:parent.stream().toList())if(!e.getName().equals(KEY)&&!Arrays.equals(parent.getInputStream(e).readAllBytes(),next.getInputStream(next.getJarEntry(e.getName())).readAllBytes()))throw new AssertionError("unrelated entry "+e.getName());
    if(next.size()!=parent.size()+1||next.getJarEntry(TO+".class")==null)throw new AssertionError("unexpected added entries");
   }
   System.out.println("PASS exact two-call inverse; all other entries and cleanup callers unchanged");
  }
 }
}
