import java.nio.file.*;import java.util.*;import java.util.jar.*;import org.objectweb.asm.*;import org.objectweb.asm.tree.*;import static org.objectweb.asm.Opcodes.*;

public final class SourceMassTeleportExitMethods {
 static final String P="local/lito381/server/",A=P+"NativeAreaMaterialAction",L=P+"NativeLightAction",H=P+"NativeSourceMassTeleportExit",PC=P+"ClientSession",B=P+"NativePlayerCommit$Binding",PROOF="L"+H+"$Proof;",SIBLING="nativeSourceMassTeleportExitOriginalUnlock";
 static final List<String> OWNERS=List.of(A,L);
 static void need(boolean ok,String why){if(!ok)throw new AssertionError(why);}
 static String append(String desc){return desc.replace(")",PROOF+")");}
 static void patch(ClassNode c) {
  if(c.name.equals(L)) {
   need(c.methods.stream().noneMatch(m->m.name.equals("nativeSourceMassTeleportIngress")),"fresh readonly ingress bridge");
   var m=new MethodNode(ACC_STATIC,"nativeSourceMassTeleportIngress","(L"+PC+";)Ljava/lang/Object;",null,null);
   m.instructions.add(new VarInsnNode(ALOAD,0));m.instructions.add(new FieldInsnNode(GETSTATIC,L,"ingress","Ljava/lang/ThreadLocal;"));m.instructions.add(new MethodInsnNode(INVOKEVIRTUAL,"java/lang/ThreadLocal","get","()Ljava/lang/Object;",false));m.instructions.add(new MethodInsnNode(INVOKESTATIC,H,"ingress","(L"+PC+";Ljava/lang/Object;)Ljava/lang/Object;",false));m.instructions.add(new InsnNode(ARETURN));c.methods.add(m);return;
  }
  var original=c.methods.stream().filter(m->m.name.equals("lambda$massTeleport$1")).findFirst().orElseThrow();
  var sibling=new MethodNode(original.access,SIBLING,append(original.desc),null,original.exceptions.toArray(String[]::new));original.accept(sibling);sibling.name=SIBLING;sibling.desc=append(original.desc);
  for(var n:sibling.instructions.toArray()){if(n instanceof FrameNode)sibling.instructions.remove(n);else if(n instanceof VarInsnNode v&&v.var>=2)v.var++;else if(n instanceof IincInsnNode v&&v.var>=2)v.var++;}
  int send=0,inside=0;
  for(var n:sibling.instructions.toArray())if(n instanceof MethodInsnNode call) {
   if(call.owner.equals(B)&&call.name.equals("matches")){sibling.instructions.insertBefore(call,new VarInsnNode(ALOAD,2));call.setOpcode(INVOKESTATIC);call.owner=H;call.desc="(L"+B+";L"+PC+";"+PROOF+")Z";inside++;}
   else if(call.owner.equals(PC)&&call.name.equals("nativeSelfOriginalSend")){sibling.instructions.insertBefore(call,new VarInsnNode(ALOAD,2));call.setOpcode(INVOKESTATIC);call.owner=H;call.name="send";call.desc="(L"+PC+";[BLjava/lang/String;"+PROOF+")V";send++;}
  }
  need(send==1&&inside==1,"one complete original unlock sibling with explicit proof at its two original ports");c.methods.add(sibling);
  var m=c.methods.stream().filter(x->x.name.equals("massTeleport")).findFirst().orElseThrow();int slot=m.maxLocals;need(slot==9,"reviewed original massTeleport local layout");
  var store=Arrays.stream(m.instructions.toArray()).filter(n->n instanceof VarInsnNode v&&v.getOpcode()==ASTORE&&v.var==3).findFirst().orElseThrow();
  var first=new InsnList();first.add(new VarInsnNode(ALOAD,0));first.add(new VarInsnNode(ALOAD,3));first.add(new MethodInsnNode(INVOKESTATIC,H,"capture","(L"+PC+";L"+B+";)"+PROOF,false));first.add(new VarInsnNode(ASTORE,slot));m.instructions.insert(store,first);
  int matches=0,closures=0,completions=0;
  for(var n:m.instructions.toArray()) {
   if(n instanceof MethodInsnNode call&&call.owner.equals(B)&&call.name.equals("matches")) {m.instructions.insertBefore(call,new VarInsnNode(ALOAD,slot));call.setOpcode(INVOKESTATIC);call.owner=H;call.desc="(L"+B+";L"+PC+";"+PROOF+")Z";matches++;}
   else if(n instanceof MethodInsnNode call&&call.owner.equals(P+"NativeRolePublication")&&call.name.equals("completion")) {m.instructions.insertBefore(call,new VarInsnNode(ALOAD,slot));call.owner=H;call.desc=append(call.desc);completions++;}
   else if(n instanceof InvokeDynamicInsnNode dynamic)for(int i=0;i<dynamic.bsmArgs.length;i++)if(dynamic.bsmArgs[i] instanceof Handle handle&&handle.getOwner().equals(A)&&handle.getName().equals(original.name)) {m.instructions.insertBefore(dynamic,new VarInsnNode(ALOAD,slot));dynamic.desc=append(dynamic.desc);dynamic.bsmArgs[i]=new Handle(handle.getTag(),A,SIBLING,append(handle.getDesc()),handle.isInterface());closures++;}
  }
  need(matches==2&&closures==2&&completions==2,"both original normal and exceptional finally paths retained");
 }
 public static void main(String[]args)throws Exception {
  var urls=new ArrayList<java.net.URL>();urls.add(Path.of(args[1]).toUri().toURL());try(var files=Files.list(Path.of(args[3]))){for(var p:files.filter(x->x.toString().endsWith(".jar")).toList())urls.add(p.toUri().toURL());}
  try(var loader=new java.net.URLClassLoader(urls.toArray(java.net.URL[]::new),ClassLoader.getPlatformClassLoader());var base=new JarFile(args[1])) {
   FreezeAcceptedMethods.hierarchy=loader;
   for(var owner:OWNERS) {var c=FreezeAcceptedMethods.read(base,owner);patch(c);var canon=new ClassNode();new ClassReader(FreezeAcceptedMethods.bytes(c,true)).accept(canon,ClassReader.SKIP_DEBUG|ClassReader.EXPAND_FRAMES);byte[] bytes=FreezeAcceptedMethods.bytes(canon,false);
    if(args[0].equals("splice")){var path=Path.of(args[2]).resolve(owner+".class");Files.createDirectories(path.getParent());Files.write(path,bytes);}else try(var candidate=new JarFile(args[2])){need(Arrays.equals(bytes,FreezeAcceptedMethods.bytes(FreezeAcceptedMethods.read(candidate,owner),false)),"deterministic bounded mass-teleport splice "+owner);}
   }
  }
  System.out.println("PASS mass-teleport exit splice:2 original owners,0 fields,1 readonly ingress bridge,1 complete original unlock sibling,7 original-method proof/closure/completion ports and2 sibling ports");
 }
}
