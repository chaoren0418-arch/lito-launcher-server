import java.nio.file.*;import java.util.*;import java.util.jar.*;import org.objectweb.asm.*;import org.objectweb.asm.tree.*;
/** Read actual native archive only. A call-site inventory is not runtime qualification. */
public final class AuditAcceptedMutationPorts {
 static final String P="local/lito381/server/";
 static boolean selected(MethodInsnNode m){return m.owner.equals(P+"BattleEraseMagicStatus")&&Set.of("captureLease","lease","consume","consumeLease").contains(m.name)||m.owner.equals(P+"BattleTimedStatus")&&(m.name.contains("Freeze")||m.name.contains("ThunderGrab"))||m.owner.equals(P+"BattleFreezeEffects")&&Set.of("spawn","create","add").contains(m.name);}
 static String insn(AbstractInsnNode n){String s="op="+n.getOpcode();if(n instanceof MethodInsnNode m)return s+" "+m.owner+"."+m.name+m.desc;if(n instanceof FieldInsnNode f)return s+" "+f.owner+"."+f.name+":"+f.desc;if(n instanceof LdcInsnNode v)return s+" ldc="+v.cst;if(n instanceof VarInsnNode v)return s+" var="+v.var;if(n instanceof IntInsnNode v)return s+" int="+v.operand;if(n instanceof TypeInsnNode v)return s+" type="+v.desc;if(n instanceof InvokeDynamicInsnNode v)return s+" indy="+v.name+v.desc+" "+Arrays.toString(v.bsmArgs);return s;}
 public static void main(String[]args)throws Exception {
  var rows=new ArrayList<String>();rows.add("owner\tmethod\tdescriptor\tinstruction\tconsumer\tcall\tcall_descriptor");var bodies=new StringBuilder();int count=0;
  try(var jar=new JarFile(args[0])){for(var e:jar.stream().filter(x->x.getName().endsWith(".class")).sorted(Comparator.comparing(JarEntry::getName)).toList()){
   var c=new ClassNode();new ClassReader(jar.getInputStream(e)).accept(c,ClassReader.SKIP_DEBUG|ClassReader.SKIP_FRAMES);
   for(var m:c.methods){boolean used=false;int i=0;for(var n:m.instructions){if(n instanceof MethodInsnNode call&&selected(call)){rows.add(String.join("\t",c.name,m.name,m.desc,""+i,call.owner,call.name,call.desc));used=true;count++;}i++;}
    if(used||Set.of(P+"BattleEraseMagicStatus",P+"CombatWorldFreeze",P+"CombatWorldFreeze$Pending",P+"CombatWorldFreeze$Batch").contains(c.name)){bodies.append("\n=== ").append(c.name).append('.').append(m.name).append(m.desc).append(" access=").append(m.access).append(" ===\n");i=0;for(var n:m.instructions){if(n.getOpcode()>=0)bodies.append(i).append(' ').append(insn(n)).append('\n');i++;}}
   }
  }}Files.write(Path.of(args[1]),rows);Files.writeString(Path.of(args[2]),bodies);System.out.println("ports="+count);
 }
}
