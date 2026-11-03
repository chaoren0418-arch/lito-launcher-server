import java.nio.file.*;import java.net.*;import java.util.*;import java.util.jar.*;import org.objectweb.asm.*;import org.objectweb.asm.tree.*;import static org.objectweb.asm.Opcodes.*;
/** Initial PC poison tick begins after the palette, as Source schedules its first tick. */
public final class PoisonTickMethods {
 static final String P="local/lito381/server/",PC=P+"ClientSession",H=P+"NativePoisonMaterialAction";
 static final Map<String,int[]> SPECS=Map.of("nativeMaterialPoisonScopeBody",new int[]{5,42},"nativeMaterialWorldPoisonScopeBody",new int[]{10,47});
 static ClassNode transform(JarFile jar,ClassLoader loader)throws Exception{
  var c=NativeSkillConsumptionMethods.read(jar.getInputStream(jar.getJarEntry(PC+".class")).readAllBytes());var changed=new ArrayList<MethodNode>();
  for(var row:SPECS.entrySet()){var m=c.methods.stream().filter(x->x.name.equals(row.getKey())).findFirst().orElseThrow();int count=0;
   for(var n:m.instructions.toArray())if(n instanceof MethodInsnNode call&&call.owner.equals(P+"Protocol")&&call.name.equals("manaPoint")){var start=n;for(int i=0;i<4;i++)start=NativeSkillConsumptionMethods.prev(start);if(!(start instanceof VarInsnNode v)||v.getOpcode()!=ALOAD||v.var!=0)throw new AssertionError("MP tail receiver");
    var i=new InsnList();i.add(new VarInsnNode(ALOAD,row.getValue()[0]));i.add(new VarInsnNode(ALOAD,3));i.add(new VarInsnNode(ILOAD,row.getValue()[1]));i.add(new VarInsnNode(ALOAD,2));i.add(new MethodInsnNode(INVOKEVIRTUAL,P+"BattleSpellCatalog$PoisonSpell","tickIntervalMillis","()I",false));i.add(new MethodInsnNode(INVOKESTATIC,H,"poisonPaletteFinished","(L"+PC+";L"+P+"CombatPoisonLifecycle$Scope;ZI)V",false));m.instructions.insertBefore(start,i);count++;
   }if(count!=1)throw new AssertionError(row.getKey()+" MP tails="+count);changed.add(m);
  }
  var framed=NativeSkillConsumptionMethods.frames(c,changed,loader);for(var m:changed)c.methods.set(c.methods.indexOf(m),NativeSkillConsumptionMethods.method(framed,m.name,m.desc));return c;
 }
 public static void main(String[]args)throws Exception{try(var old=new JarFile(args[1]);var loader=new URLClassLoader(new URL[]{Path.of(args[1]).toUri().toURL()},ClassLoader.getPlatformClassLoader())){
  var expected=transform(old,loader);if(args[0].equals("splice")){var out=Path.of(args[2]).resolve(PC+".class");Files.createDirectories(out.getParent());Files.write(out,NativeSkillConsumptionMethods.bytes(expected,false));return;}
  try(var next=new JarFile(args[2])){var actual=NativeSkillConsumptionMethods.read(next.getInputStream(next.getJarEntry(PC+".class")).readAllBytes());if(!Arrays.equals(NativeSkillConsumptionMethods.bytes(actual,true),NativeSkillConsumptionMethods.bytes(expected,true)))throw new AssertionError("unexpected PC transform");var parent=NativeSkillConsumptionMethods.read(old.getInputStream(old.getJarEntry(PC+".class")).readAllBytes());for(var name:SPECS.keySet()){var a=actual.methods.stream().filter(m->m.name.equals(name)).findFirst().orElseThrow();var b=parent.methods.stream().filter(m->m.name.equals(name)).findFirst().orElseThrow();actual.methods.set(actual.methods.indexOf(a),b);}if(!Arrays.equals(NativeSkillConsumptionMethods.bytes(actual,true),NativeSkillConsumptionMethods.bytes(parent,true)))throw new AssertionError("unrelated PC instruction changed");System.out.println("PASS exact two palette completion anchors; other PC methods unchanged");}
 }}
}
