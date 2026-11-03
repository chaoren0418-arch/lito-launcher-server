import java.nio.file.*;import java.util.*;import java.util.jar.*;import org.objectweb.asm.*;import org.objectweb.asm.tree.*;import static org.objectweb.asm.Opcodes.*;
public final class ShockCompanionMethods {
 static final String P="local/lito381/server/",PC=P+"ClientSession",N=P+"CombatCompanionSpells",H=P+"NativeShockCompanionAction",INV=P+"RuntimeInventory",SP=P+"BattleSpellCatalog$ShockStunSpell";
 static ClassNode read(JarFile j,String n)throws Exception{var c=new ClassNode();new ClassReader(j.getInputStream(j.getJarEntry(n+".class"))).accept(c,ClassReader.SKIP_DEBUG|ClassReader.EXPAND_FRAMES);return c;}
 static byte[] bytes(ClassNode c){var w=new ClassWriter(ClassWriter.COMPUTE_MAXS);c.accept(w);return w.toByteArray();}
 static AbstractInsnNode prev(AbstractInsnNode n){do{n=n.getPrevious();}while(n!=null&&n.getOpcode()<0);return n;}
 static void remove(MethodNode m,AbstractInsnNode from,AbstractInsnNode until){while(from!=until){var next=from.getNext();m.instructions.remove(from);from=next;}}
 static void patch(ClassNode c){
  if(c.name.equals(P+"NativeAreaMaterialAction")){var old=c.methods.stream().filter(m->m.name.equals("captureOutput")).findFirst().orElseThrow();old.name="nativeShockOriginalCaptureOutput";var w=new MethodNode(old.access,"captureOutput",old.desc,null,old.exceptions.toArray(String[]::new));for(int i=0;i<3;i++)w.instructions.add(new VarInsnNode(ALOAD,i));w.instructions.add(new MethodInsnNode(INVOKESTATIC,H,"captureOutput",old.desc,false));w.instructions.add(new InsnNode(IRETURN));c.methods.add(w);return;}
  var old=c.methods.stream().filter(m->m.name.equals("shock")&&Type.getArgumentTypes(m.desc).length==4).findFirst().orElseThrow();var m=new MethodNode(old.access,"nativeShockCompanionBody",old.desc.replace(")Z","Ljava/lang/Object;)Z"),null,old.exceptions.toArray(String[]::new));old.accept(m);
  int resolve=0,available=0,monitors=0;MethodInsnNode consume=null;VarInsnNode nextStore=null,param=null;
  for(var n:m.instructions.toArray()){
   if((n.getOpcode()==MONITORENTER||n.getOpcode()==MONITOREXIT)&&prev(n)instanceof VarInsnNode v&&v.var==32){m.instructions.set(n,new InsnNode(POP));monitors++;}
   if(n instanceof MethodInsnNode v){
    if(v.owner.equals(P+"CombatAccess")&&v.name.equals("call")){var first=n;while(!(first instanceof LdcInsnNode s&&s.cst.equals("resolveBattleSpellCompanion"))&&first!=null)first=prev(first);if(first!=null){first=prev(first);var end=n.getNext();remove(m,first,end);param=new VarInsnNode(ALOAD,4);m.instructions.insertBefore(end,param);resolve++;}}
    if(v.owner.equals(N)&&v.name.equals("available")){v.owner=H;available++;}
    if(v.owner.equals(P+"CombatDatabase")&&v.name.equals("consumeMaterial"))consume=v;
   }
   if(n instanceof VarInsnNode v&&v.getOpcode()==ASTORE&&v.var==31)nextStore=v;
  }
  if(resolve!=1||available!=4||monitors!=5||consume==null||nextStore==null)throw new AssertionError("shock exact shape "+List.of(resolve,available,monitors));
  var start=prev(nextStore);while(!(start instanceof VarInsnNode beginLoad&&beginLoad.getOpcode()==ALOAD&&beginLoad.var==0))start=prev(start);var branch=consume.getNext();remove(m,start,branch);var gate=new InsnList();for(int local:new int[]{30,31}){gate.add(new InsnNode(ACONST_NULL));gate.add(new VarInsnNode(ASTORE,local));}gate.add(new VarInsnNode(ALOAD,0));gate.add(new VarInsnNode(ALOAD,5));gate.add(new VarInsnNode(ALOAD,2));gate.add(new MethodInsnNode(INVOKESTATIC,H,"admit","(L"+PC+";Ljava/lang/Object;L"+SP+";)Z",false));m.instructions.insertBefore(branch,gate);
  if(!(branch instanceof JumpInsnNode jump)||jump.getOpcode()!=IFNE)throw new AssertionError("material admission branch");var accepted=jump.label.getNext();while(accepted.getOpcode()<0)accepted=accepted.getNext();var material=new InsnList();for(int i=0;i<2;i++){material.add(new VarInsnNode(ALOAD,0));material.add(new MethodInsnNode(INVOKESTATIC,P+"NativeCastMaterial",i==0?"previous":"next","(L"+PC+";)L"+INV+";",false));material.add(new VarInsnNode(ASTORE,30+i));}material.add(new MethodInsnNode(INVOKESTATIC,"java/lang/System","nanoTime","()J",false));material.add(new VarInsnNode(LSTORE,26));m.instructions.insertBefore(accepted,material);
  for(var n:m.instructions){if(n instanceof VarInsnNode v&&v.var>=4&&n!=param)v.var++;if(n instanceof IincInsnNode v&&v.var>=4)v.var++;if(n instanceof FrameNode f){while(f.local.size()<4)f.local.add(TOP);f.local.add(4,"java/lang/Object");}}
  m.maxLocals=old.maxLocals+1;c.methods.add(m);var w=new MethodNode(old.access,old.name,old.desc,null,old.exceptions.toArray(String[]::new));for(int i=0;i<4;i++)w.instructions.add(new VarInsnNode(ALOAD,i));w.instructions.add(new MethodInsnNode(INVOKESTATIC,H,"shock",old.desc,false));w.instructions.add(new InsnNode(IRETURN));c.methods.set(c.methods.indexOf(old),w);
 }
 public static void main(String[]a)throws Exception{try(var j=new JarFile(a[1])){for(String name:List.of(N,P+"NativeAreaMaterialAction")){var c=read(j,name);patch(c);var b=bytes(c);if(a[0].equals("splice")){var p=Path.of(a[2]).resolve(name+".class");Files.createDirectories(p.getParent());Files.write(p,b);}else try(var next=new JarFile(a[2])){if(!Arrays.equals(b,bytes(read(next,name))))throw new AssertionError(name);}}}System.out.println("PASS exact controlled Shock Stun ports; original numeric/effect body preserved");}
}
