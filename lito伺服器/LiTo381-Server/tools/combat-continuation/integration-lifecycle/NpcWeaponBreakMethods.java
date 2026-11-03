import java.nio.file.*;import java.net.*;import java.util.*;import java.util.jar.*;import org.objectweb.asm.*;import org.objectweb.asm.tree.*;
/** Preserve the Source double until weapon-break scaling; retain all old int logs/other locals. */
public final class NpcWeaponBreakMethods {
 static final String P="local/lito381/server/",H=P+"NativeNpcWeaponBreakDamage";
 static final Set<String> PC=Set.of("serverClassicWorldMonsterAttack","classicWorldMonsterPhysicalSkillHit","serverClassicWorldMonsterCompanionAttack","serverClassicWorldDspyFollowerAttack");
 static ClassNode read(JarFile j,String n)throws Exception{var c=new ClassNode();new ClassReader(j.getInputStream(j.getJarEntry(P+n+".class"))).accept(c,ClassReader.SKIP_DEBUG);return c;}
 static byte[] bytes(ClassNode c){var w=new ClassWriter(ClassWriter.COMPUTE_MAXS);c.accept(w);return w.toByteArray();}
 static AbstractInsnNode prev(AbstractInsnNode n){do{n=n.getPrevious();}while(n.getOpcode()<0);return n;}
 static AbstractInsnNode next(AbstractInsnNode n){do{n=n.getNext();}while(n.getOpcode()<0);return n;}
 static void patch(ClassNode c){int count=0;for(var m:c.methods)if(c.name.endsWith("/ClientSession")?PC.contains(m.name):m.name.equals("againstPlayer")&&m.desc.endsWith("JI)L"+P+"BattleTournamentNpcPhysicalDamage$Outcome;")){
   MethodInsnNode source=null,broken=null;for(var i:m.instructions.toArray())if(i instanceof MethodInsnNode call){if(call.owner.equals(P+"ClassicWorldNpcDamage")&&call.name.equals("sourceDamage")){if(source!=null)throw new AssertionError("ambiguous source prefix");source=call;}if(call.owner.equals(P+"BattleWeaponBreak")&&call.name.equals("damageAfterStacks")){if(broken!=null)throw new AssertionError("ambiguous stack phase");broken=call;}}
   if(source==null||broken==null)throw new AssertionError("missing exact source/stack spine "+m.name);
   var store=next(source);var stacks=prev(broken);var value=prev(stacks);if(!(store instanceof VarInsnNode s&&s.getOpcode()==Opcodes.ISTORE&&value instanceof VarInsnNode v&&v.getOpcode()==Opcodes.ILOAD&&v.var==s.var))throw new AssertionError("source local spine "+m.name);
   int exact=m.maxLocals;m.maxLocals+=2;source.owner=H;source.name="sourceExact";if(!source.desc.endsWith(")I"))throw new AssertionError();source.desc=source.desc.substring(0,source.desc.length()-1)+"D";
   var retain=new InsnList();retain.add(new InsnNode(Opcodes.DUP2));retain.add(new VarInsnNode(Opcodes.DSTORE,exact));retain.add(new InsnNode(Opcodes.D2I));retain.add(new InsnNode(Opcodes.ICONST_0));retain.add(new MethodInsnNode(Opcodes.INVOKESTATIC,"java/lang/Math","max","(II)I",false));m.instructions.insert(source,retain);
   m.instructions.set(value,new VarInsnNode(Opcodes.DLOAD,exact));broken.owner=H;broken.name="afterStacks";broken.desc="(DI)I";
   // The fresh double remains live across existing branches; expand every full frame
   // using an exact per-method frame recomputation in main, without touching other methods.
   count++;
  }if(count!=(c.name.endsWith("/ClientSession")?4:1))throw new AssertionError("consumer count "+count);
 }
 public static void main(String[]args)throws Exception{try(var jar=new JarFile(args[1]);var loader=new URLClassLoader(new URL[]{Path.of(args[1]).toUri().toURL()},ClassLoader.getPlatformClassLoader())){
  for(var n:List.of("ClientSession","BattleTournamentNpcPhysicalDamage")){var c=read(jar,n);patch(c);var selected=c.methods.stream().filter(m->n.equals("ClientSession")?PC.contains(m.name):m.name.equals("againstPlayer")&&m.desc.endsWith("JI)L"+P+"BattleTournamentNpcPhysicalDamage$Outcome;")).toList();
   var framed=NativeSkillConsumptionMethods.frames(c,selected,loader);for(var m:selected)c.methods.set(c.methods.indexOf(m),NativeSkillConsumptionMethods.method(framed,m.name,m.desc));var expected=bytes(c);
   if(args[0].equals("splice")){var out=Path.of(args[2]).resolve(P+n+".class");Files.createDirectories(out.getParent());Files.write(out,expected);}else try(var actual=new JarFile(args[2])){if(!Arrays.equals(expected,bytes(read(actual,n))))throw new AssertionError("exact five-consumer regeneration "+n);}
  }
 }System.out.println("PASS exact five NPC prefix replacements; unrelated methods, fields, gates, RNG draws and HP commit consumers preserved");}
}
