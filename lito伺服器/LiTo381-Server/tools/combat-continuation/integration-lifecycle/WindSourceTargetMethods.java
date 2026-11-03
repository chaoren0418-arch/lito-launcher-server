import java.nio.file.*;import java.util.*;import java.util.jar.*;import org.objectweb.asm.*;import org.objectweb.asm.tree.*;import static org.objectweb.asm.Opcodes.*;
/** No full ClientSession compilation: two exact metadata ports and four effect-list ports. */
public final class WindSourceTargetMethods {
 static final String P="local/lito381/server/",M=P+"NativeSkillTargetMetadata",H=P+"NativeWindSourceTargets",PC=P+"ClientSession",W=P+"BattleSpellCatalog$WindShackleSpell";
 static ClassNode read(JarFile j,String n)throws Exception{var c=new ClassNode();new ClassReader(j.getInputStream(j.getJarEntry(n+".class"))).accept(c,ClassReader.SKIP_DEBUG|ClassReader.EXPAND_FRAMES);return c;}
 static byte[] bytes(ClassNode c){var w=new ClassWriter(ClassWriter.COMPUTE_MAXS);c.accept(w);return w.toByteArray();}
 static AbstractInsnNode prev(AbstractInsnNode n){do{n=n.getPrevious();}while(n!=null&&n.getOpcode()<0);return n;}
 static void patch(ClassNode c){int count=0;
  if(c.name.equals(P+"CombatDatabase")){for(var m:c.methods)if(m.name.equals("load"))for(var n:m.instructions)if(n instanceof MethodInsnNode v&&v.owner.equals(P+"SourceSkillControls")&&v.name.equals("read")){v.owner=M;count++;}if(count!=1)throw new AssertionError("startup port");return;}
  if(c.name.equals(P+"BattleSpellCatalog")){for(var m:c.methods)for(var n:m.instructions)if(n instanceof TypeInsnNode t&&t.getOpcode()==NEW&&t.desc.equals(W)){var q=n.getNext();if(q.getOpcode()!=DUP)throw new AssertionError();q=q.getNext();for(int i=0;i<14;i++)q=q.getNext();if(q.getOpcode()!=ICONST_M1)throw new AssertionError("167 area literal");m.instructions.set(q,new InsnNode(ICONST_0));count++;}if(count!=1)throw new AssertionError("one native row");return;}
  if(c.name.equals(W)){int guard=0,accessors=0;for(var m:c.methods){if(m.name.equals("<init>"))for(var n:m.instructions.toArray())if(n instanceof VarInsnNode v&&v.getOpcode()==ILOAD&&v.var==15){var next=n.getNext();if(next!=null&&next.getOpcode()==ICONST_M1){m.instructions.set(next,new InsnNode(ICONST_0));guard++;}}
    if(Set.of("area","targetTo","maximumRange","attribute","type","nativeId").contains(m.name)&&m.desc.equals("()I")){for(var n:m.instructions.toArray())if(n.getOpcode()==IRETURN){var add=new InsnList();add.add(new LdcInsnNode(m.name));add.add(new MethodInsnNode(INVOKESTATIC,M,"windValue","(ILjava/lang/String;)I",false));m.instructions.insertBefore(n,add);accessors++;}}
   }if(guard!=1||accessors!=6)throw new AssertionError("native row schema "+guard+" "+accessors);return;}
  if(c.name.equals(PC)){for(var m:c.methods)if(m.name.equals("nativeMaterialAreaWindBody")){boolean effects=false;for(var n:m.instructions.toArray()){
    if(n instanceof VarInsnNode v&&v.getOpcode()==ISTORE&&v.var==38)effects=true;
    if(effects&&n instanceof MethodInsnNode v&&v.owner.equals("java/util/List")&&v.name.equals("iterator")&&prev(n) instanceof VarInsnNode load&&load.var>=15&&load.var<=18){var add=new InsnList();add.add(new VarInsnNode(ALOAD,0));add.add(new VarInsnNode(ALOAD,4));add.add(new MethodInsnNode(INVOKEVIRTUAL,P+"Protocol$AttackSpellRequest","targetObjectId","()J",false));add.add(new VarInsnNode(ALOAD,2));add.add(new MethodInsnNode(INVOKESTATIC,H,"filter","(Ljava/util/List;L"+PC+";JL"+W+";)Ljava/util/List;",false));m.instructions.insertBefore(n,add);count++;}
   }}if(count!=4)throw new AssertionError("four effect-list ports "+count);return;}
  throw new AssertionError(c.name);
 }
 public static void main(String[]a)throws Exception{try(var j=new JarFile(a[1])){for(String n:List.of(P+"CombatDatabase",P+"BattleSpellCatalog",W,PC)){var c=read(j,n);patch(c);var b=bytes(c);if(a[0].equals("splice")){var p=Path.of(a[2]).resolve(n+".class");Files.createDirectories(p.getParent());Files.write(p,b);}else try(var other=new JarFile(a[2])){if(!Arrays.equals(b,bytes(read(other,n))))throw new AssertionError(n);}}}System.out.println("PASS exact startup readonly metadata, area0 native row and four post-admission target-list ports; original selection/cost/numeric/output methods retained");}
}
