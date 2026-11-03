import java.nio.file.*;import java.io.*;import java.util.*;import java.util.jar.*;import org.objectweb.asm.*;import org.objectweb.asm.tree.*;
/** Read-only, exact method disassembly of the pinned runtime. */
public final class SelectedMethods {
 public static void main(String[] a)throws Exception{try(var j=new JarFile(a[0]);var w=new PrintWriter(Files.newBufferedWriter(Path.of(a[1])))){
  for(int n=2;n<a.length;n++){var parts=a[n].split(":",2);var c=new ClassNode();new ClassReader(j.getInputStream(j.getJarEntry("local/lito381/server/"+parts[0]+".class"))).accept(c,ClassReader.SKIP_DEBUG);w.println("CLASS "+c.name);for(var f:c.fields)w.println("FIELD "+f.access+" "+f.name+" "+f.desc);
   for(var m:c.methods)if(parts.length==1||Set.of(parts[1].split(",")).contains(m.name)){w.println("METHOD "+m.access+" "+m.name+m.desc);var ins=m.instructions.toArray();for(int k=0;k<ins.length;k++){var i=ins[k];String s="";if(i instanceof MethodInsnNode v)s=v.owner+"."+v.name+v.desc;else if(i instanceof FieldInsnNode v)s=v.owner+"."+v.name+" "+v.desc;else if(i instanceof VarInsnNode v)s="local="+v.var;else if(i instanceof LdcInsnNode v)s=String.valueOf(v.cst);else if(i instanceof IntInsnNode v)s="operand="+v.operand;else if(i instanceof TypeInsnNode v)s=v.desc;else if(i instanceof JumpInsnNode v)s="to="+m.instructions.indexOf(v.label);else if(i instanceof InvokeDynamicInsnNode v)s=v.name+v.desc+" "+Arrays.toString(v.bsmArgs);w.println(k+" OP="+i.getOpcode()+" "+s);}for(var t:m.tryCatchBlocks)w.println("CATCH "+m.instructions.indexOf(t.start)+".."+m.instructions.indexOf(t.end)+" ->"+m.instructions.indexOf(t.handler)+" "+t.type);}
  }
 }}
}
