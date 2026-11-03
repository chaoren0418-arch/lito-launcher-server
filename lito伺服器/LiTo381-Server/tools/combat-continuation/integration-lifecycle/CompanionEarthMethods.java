import java.nio.file.*;import java.util.*;import java.util.jar.*;import org.objectweb.asm.*;import org.objectweb.asm.tree.*;import static org.objectweb.asm.Opcodes.*;
/** Preserve the native Earth Bind bodies except the WORLD companion selector and admission authority. */
public final class CompanionEarthMethods {
 static final String P="local/lito381/server/",PC=P+"ClientSession",EARTH=P+"NativeEarthBindMaterialAction",H=P+"NativeCompanionEarth",TARGET=P+"NativeCompanionTargets",NEG=P+"NativeCompanionNegative";
 static AbstractInsnNode previous(AbstractInsnNode n){do{n=n.getPrevious();}while(n!=null&&n.getOpcode()<0);return n;}
 static ClassNode read(JarFile j,String name)throws Exception{var c=new ClassNode();new ClassReader(j.getInputStream(j.getJarEntry(name+".class"))).accept(c,ClassReader.SKIP_DEBUG);return c;}
 static byte[] bytes(ClassNode c){var w=new ClassWriter(ClassWriter.COMPUTE_MAXS);c.accept(w);return w.toByteArray();}
 static MethodInsnNode call(String owner,String name,String desc){return new MethodInsnNode(INVOKESTATIC,owner,name,desc,false);}
 static void patch(ClassNode c){
  if(c.name.equals(EARTH)){int roots=0,bodies=0;for(var m:new ArrayList<>(c.methods)){
   for(var n:m.instructions.toArray())if(n instanceof MethodInsnNode v&&v.owner.equals(PC)&&v.name.equals("nativeMaterialWorldEarthBindBody")){m.instructions.set(v,call(H,"body","(L"+PC+";"+v.desc.substring(1)));bodies++;}
   if(Set.of("world","target","admit").contains(m.name)){String name=m.name;var w=new MethodNode(m.access,name,m.desc,m.signature,m.exceptions.toArray(String[]::new));m.name="nativeSceneOriginal"+Character.toUpperCase(name.charAt(0))+name.substring(1);m.access&=~ACC_PRIVATE;int count=name.equals("admit")?5:3;for(int i=0;i<count;i++)w.instructions.add(new VarInsnNode((name.equals("admit")&&i>=2||name.equals("target")&&i==2)?ILOAD:ALOAD,i));w.instructions.add(call(H,name,m.desc));w.instructions.add(new InsnNode(name.equals("world")?RETURN:name.equals("admit")?IRETURN:ARETURN));c.methods.add(w);roots++;}
  }if(roots!=3||bodies!=1)throw new AssertionError("Earth capture ports "+roots+" "+bodies);return;}
  int roots=0,snapshots=0,iterators=0,gates=0,maps=0;for(var m:c.methods)if(m.name.equals("nativeMaterialWorldEarthBindBody")){
   roots++;for(var n:m.instructions.toArray()){
    if(n instanceof FieldInsnNode f&&f.owner.equals(PC)&&Set.of("classicWorldMapId","classicWorldInstanceId").contains(f.name)&&previous(f) instanceof VarInsnNode owner&&owner.var==14){m.instructions.insertBefore(f,new VarInsnNode(ALOAD,0));m.instructions.set(f,call(TARGET,f.name.equals("classicWorldMapId")?"map":"instance","(L"+PC+";L"+PC+";)I"));maps++;}
    if(n instanceof MethodInsnNode v&&v.owner.equals(PC)&&v.name.equals("classicWorldCompanionSnapshots")){
     if(!(previous(v) instanceof VarInsnNode owner&&owner.var==14))throw new AssertionError("companion owner slot");AbstractInsnNode iterator=v;while(iterator!=null&&!(iterator instanceof VarInsnNode s&&s.getOpcode()==ASTORE&&s.var==13))iterator=previous(iterator);if(iterator==null||!(previous(iterator) instanceof MethodInsnNode it&&it.owner.equals("java/util/List")&&it.name.equals("iterator")))throw new AssertionError("companion iterator");var add=new InsnList();add.add(new VarInsnNode(ALOAD,0));add.add(call(NEG,"members","(Ljava/util/List;L"+PC+";)Ljava/util/List;"));m.instructions.insertBefore(previous(iterator),add);iterators++;m.instructions.insertBefore(v,new VarInsnNode(ALOAD,0));m.instructions.set(v,call(TARGET,"snapshots","(L"+PC+";L"+PC+";)Ljava/util/List;"));snapshots++;
    }
    if(n instanceof MethodInsnNode v&&v.owner.equals(PC)&&v.name.equals("sameClassicWorldMap")&&previous(v) instanceof VarInsnNode owner&&owner.var==10){m.instructions.set(v,call(TARGET,"same","(L"+PC+";L"+PC+";)Z"));gates++;}
   }
  }if(roots!=1||snapshots!=1||iterators!=1||gates!=2||maps!=2)throw new AssertionError("Earth WORLD ports "+roots+" "+snapshots+" "+iterators+" "+gates+" "+maps);
 }
 public static void main(String[]args)throws Exception{try(var j=new JarFile(args[1])){for(var name:List.of(PC,EARTH)){var c=read(j,name);patch(c);var expected=bytes(c);if(args[0].equals("splice")){var out=Path.of(args[2]).resolve(name+".class");Files.createDirectories(out.getParent());Files.write(out,expected);}else try(var candidate=new JarFile(args[2])){if(!Arrays.equals(expected,bytes(read(candidate,name))))throw new AssertionError(name);}}}System.out.println("PASS Earth WORLD companion ports/capture; original player/BATTLE predicates and cost/probability/effect bodies preserved");}
}
