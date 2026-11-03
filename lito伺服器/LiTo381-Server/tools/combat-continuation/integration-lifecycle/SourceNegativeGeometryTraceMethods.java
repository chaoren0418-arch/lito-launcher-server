import java.nio.file.*;import java.util.*;import java.util.jar.*;import java.util.zip.*;import org.objectweb.asm.*;import org.objectweb.asm.tree.*;
/** Replace one static call in a separate diagnostic archive, never in the candidate. */
public final class SourceNegativeGeometryTraceMethods {
 public static void main(String[]a)throws Exception {
  String p="local/lito381/server/",owner=p+"ClientSession.class";var entries=new TreeMap<String,byte[]>();
  try(var jar=new JarFile(a[0])){for(var e:Collections.list(jar.entries()))try(var in=jar.getInputStream(e)){entries.put(e.getName(),in.readAllBytes());}}
  var node=new ClassNode();new ClassReader(entries.get(owner)).accept(node,0);int count=0;
  for(var m:node.methods)if(m.name.equals("nativeMaterialEntangleBody"))for(var n:m.instructions)if(n instanceof MethodInsnNode call&&call.owner.equals(p+"NativeSourceVisibilityRange")&&call.name.equals("signedDistance29")&&call.desc.equals("(II)I")){var values=new InsnList();for(int slot:new int[]{8,9,10,11})values.add(new VarInsnNode(Opcodes.ALOAD,slot));for(int slot:new int[]{16,17})values.add(new VarInsnNode(Opcodes.ILOAD,slot));m.instructions.insertBefore(call,values);m.maxStack+=6;call.owner=p+"SourceNegativeGeometryTrace";call.desc="(IIL"+p+"ClientSession;L"+p+"ClientSession;L"+p+"BattleCompanion;L"+p+"BattleCompanion$Snapshot;II)I";count++;}
  if(count!=1)throw new AssertionError("one original geometry call required: "+count);
  var writer=new ClassWriter(0);node.accept(writer);entries.put(owner,writer.toByteArray());entries.put(p+"SourceNegativeGeometryTrace.class",Files.readAllBytes(Path.of(a[2]).resolve(p+"SourceNegativeGeometryTrace.class")));
  try(var out=new ZipOutputStream(Files.newOutputStream(Path.of(a[1])))){for(var e:entries.entrySet()){var z=new ZipEntry(e.getKey());z.setTime(315532800000L);out.putNextEntry(z);out.write(e.getValue());out.closeEntry();}}
  System.out.println("TEST-ONLY geometry trace: one original static distance call, no changed arguments/result; no deployment or candidate mutation");
 }
}
