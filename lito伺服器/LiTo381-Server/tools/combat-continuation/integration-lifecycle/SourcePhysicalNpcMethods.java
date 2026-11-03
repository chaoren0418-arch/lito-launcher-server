import java.nio.file.*;import java.util.*;import java.util.jar.*;import org.objectweb.asm.*;import org.objectweb.asm.tree.*;import static org.objectweb.asm.Opcodes.*;
public final class SourcePhysicalNpcMethods {
 static final String P="local/lito381/server/",N=P+"NativeSourcePhysicalNpc",OLD=P+"NativeSourceCubeNpc",COM=N+"$Commit",H=P+"GameHub",PC=P+"ClientSession",CTX=P+"NativeAreaMaterialAction$Context",LEASE=P+"NativeSourceArenaPoison$Lease",POISON=P+"NativeSourceArenaPoison",DISP=P+"NativeProcHealingEncounter$Dispatch",NPC=P+"NativeNpcLifetime",PLAN=P+"CombatWorldCommit$Plan";
 static final List<String>TARGETS=List.of(H,PC,CTX,P+"NativeSourceDollNpc",P+"NativeSourceDollAction",DISP,NPC,PLAN);
 static MethodNode copy(MethodNode m){var n=new MethodNode(m.access,m.name,m.desc,null,m.exceptions.toArray(String[]::new));m.accept(n);return n;}
 static void guard(MethodNode m,String method,boolean result,int...slots){var x=new InsnList();for(int slot:slots)x.add(new VarInsnNode(ALOAD,slot));String desc=method.equals("allowFrame")?"(L"+PC+";[B)Z":method.equals("status")?"(L"+PC+";[BLjava/lang/String;Ljava/lang/Object;)Z":"(Ljava/lang/Object;)Z";x.add(new MethodInsnNode(INVOKESTATIC,N,method,desc,false));var next=new LabelNode();x.add(new JumpInsnNode(result?IFEQ:IFNE,next));x.add(new InsnNode(RETURN));x.add(next);m.instructions.insert(x);}
 static void patch(ClassNode c){
  if(c.name.equals(PLAN)){c.permittedSubclasses.add(N+"$Ticket");return;}
  if(c.name.equals(H)||c.name.equals(PC)){
   var originals=new ArrayList<>(c.methods);for(var original:originals)if(original.name.startsWith(c.name.equals(H)?"nativeSourceCube":"nativeSourceCube")&&(c.name.equals(H)?Set.of("nativeSourceCubeQuestDamage","nativeSourceCubeTimeDamage"):Set.of("nativeSourceCubeQuestCommitted","nativeSourceCubeTimeCommitted","nativeSourceCubeQuestWaveAdvance")).contains(original.name)){
    var m=copy(original);m.name=m.name.replace("nativeSourceCube","nativeSourcePhysical");m.desc=m.desc.replace(OLD+"$Commit",COM);
    for(var node:m.instructions.toArray())if(node instanceof MethodInsnNode call){if(call.owner.equals(OLD)){call.owner=N;call.desc=call.desc.replace(OLD+"$Commit",COM);}else if(call.owner.equals(PC)&&call.name.startsWith("nativeSourceCube")){call.name=call.name.replace("nativeSourceCube","nativeSourcePhysical");call.desc=call.desc.replace(OLD+"$Commit",COM);}}

    c.methods.add(m);
   }
   if(c.name.equals(PC))for(var m:originals)if(m.name.equals("nativeStaticOriginalFrame"))guard(m,"allowFrame",false,0,1);
   return;
  }
  if(c.name.equals(CTX)){
   for(var field:List.of("nativeSourcePhysicalNpcOutput","nativeSourcePhysicalNpcCommit"))c.fields.add(new FieldNode(ACC_VOLATILE,field,"Ljava/lang/Object;",null,null));int count=0;
   for(var m:c.methods)if(m.name.equals("<init>"))for(var node:m.instructions.toArray())if(node.getOpcode()==RETURN){var x=new InsnList();x.add(new VarInsnNode(ALOAD,0));boolean copy=m.desc.equals("(L"+CTX+";)V");if(copy)x.add(new VarInsnNode(ALOAD,1));x.add(new MethodInsnNode(INVOKESTATIC,N,"inherit",copy?"(L"+CTX+";L"+CTX+";)V":"(L"+CTX+";)V",false));m.instructions.insertBefore(node,x);count++;}if(count!=2)throw new AssertionError("two existing Context constructors");
   var ctor=new MethodNode(0,"<init>","(L"+PC+";L"+COM+";)V",null,null);var x=ctor.instructions;x.add(new VarInsnNode(ALOAD,0));x.add(new VarInsnNode(ALOAD,1));x.add(new MethodInsnNode(INVOKESPECIAL,CTX,"<init>","(L"+PC+";)V",false));
   x.add(new VarInsnNode(ALOAD,0));x.add(new VarInsnNode(ALOAD,2));x.add(new FieldInsnNode(GETFIELD,COM,"members","Ljava/util/List;"));x.add(new FieldInsnNode(PUTFIELD,CTX,"players","Ljava/util/List;"));
   x.add(new VarInsnNode(ALOAD,0));x.add(new TypeInsnNode(NEW,"java/util/IdentityHashMap"));x.add(new InsnNode(DUP));x.add(new VarInsnNode(ALOAD,2));x.add(new FieldInsnNode(GETFIELD,COM,"viewers","Ljava/util/Map;"));x.add(new MethodInsnNode(INVOKESPECIAL,"java/util/IdentityHashMap","<init>","(Ljava/util/Map;)V",false));x.add(new FieldInsnNode(PUTFIELD,CTX,"bindings","Ljava/util/IdentityHashMap;"));
   x.add(new VarInsnNode(ALOAD,0));x.add(new VarInsnNode(ALOAD,2));x.add(new MethodInsnNode(INVOKESTATIC,N,"bind","(L"+CTX+";L"+COM+";)V",false));x.add(new InsnNode(RETURN));c.methods.add(ctor);return;
  }
  if(c.name.equals(P+"NativeSourceDollNpc")){
   for(var name:List.of("natural","naturalCommit")){var old=c.methods.stream().filter(m->m.name.equals(name)).findFirst().orElseThrow();old.name=name.equals("natural")?"nativeSourcePhysicalOriginalNatural":"nativeSourcePhysicalOriginalNaturalCommit";var m=new MethodNode(old.access,name,old.desc,null,old.exceptions.toArray(String[]::new));int slot=0;for(var type:Type.getArgumentTypes(m.desc)){m.instructions.add(new VarInsnNode(type.getOpcode(ILOAD),slot));slot+=type.getSize();}m.instructions.add(new MethodInsnNode(INVOKESTATIC,N,name.equals("natural")?"physical":"doll",old.desc,false));m.instructions.add(new InsnNode(Type.getReturnType(m.desc).getOpcode(IRETURN)));c.methods.add(m);}return;
  }
  if(c.name.equals(P+"NativeSourceDollAction")){
   for(var name:List.of("stamp","powers")){var old=c.methods.stream().filter(m->m.name.equals(name)).findFirst().orElseThrow();old.name=name.equals("stamp")?"nativeSourcePhysicalOriginalStamp":"nativeSourcePhysicalOriginalPowers";var m=new MethodNode(old.access,name,old.desc,null,old.exceptions.toArray(String[]::new));m.instructions.add(new VarInsnNode(ALOAD,0));m.instructions.add(new MethodInsnNode(INVOKESTATIC,N,name,old.desc,false));m.instructions.add(new InsnNode(ARETURN));c.methods.add(m);}return;
  }
  if(c.name.equals(DISP)){int n=0;for(var m:c.methods)if(m.name.equals("run"))for(var node:m.instructions.toArray())if(node instanceof MethodInsnNode call&&call.owner.equals(OLD+"Queue")&&call.name.equals("consume")){
   var branch=(JumpInsnNode)FreezeAcceptedMethods.next(call);var jump=(JumpInsnNode)FreezeAcceptedMethods.next(branch);var retry=new LabelNode();var x=new InsnList();x.add(new VarInsnNode(ALOAD,0));x.add(new FieldInsnNode(GETFIELD,DISP,"hub","L"+H+";"));x.add(new VarInsnNode(ALOAD,0));x.add(new FieldInsnNode(GETFIELD,DISP,"queue","L"+P+"CombatNpcDamageQueue$Queue;"));x.add(new VarInsnNode(ALOAD,2));x.add(new MethodInsnNode(INVOKESTATIC,N,"consume",call.desc,false));x.add(new JumpInsnNode(IFEQ,retry));x.add(new JumpInsnNode(GOTO,jump.label));x.add(retry);
   // Insert before the existing cube argument loads inside the original protected try region.
   AbstractInsnNode first=call;for(int k=0;k<5;k++)first=FreezeAcceptedMethods.prev(first);m.instructions.insertBefore(first,x);n++;
  }if(n!=1)throw new AssertionError("existing typed drain route");return;}
  if(c.name.equals(NPC)){int n=0;for(var m:c.methods)if(m.name.equals("clearing")){var x=new InsnList();x.add(new VarInsnNode(ALOAD,0));x.add(new MethodInsnNode(INVOKESTATIC,N,"clearing","(L"+P+"CombatNpcDamageQueue$Queue;)V",false));m.instructions.insert(x);n++;}if(n!=1)throw new AssertionError("original queue cancellation hook");return;}
  throw new AssertionError("unexpected target "+c.name);
 }
 public static void main(String[]a)throws Exception{var urls=new ArrayList<java.net.URL>();urls.add(Path.of(a[1]).toUri().toURL());try(var f=Files.list(Path.of(a[3]))){for(var p:f.filter(x->x.toString().endsWith(".jar")).toList())urls.add(p.toUri().toURL());}try(var hierarchy=new java.net.URLClassLoader(urls.toArray(java.net.URL[]::new),ClassLoader.getPlatformClassLoader());var base=new JarFile(a[1])){FreezeAcceptedMethods.hierarchy=hierarchy;for(var name:TARGETS){var c=FreezeAcceptedMethods.read(base,name);patch(c);var canon=new ClassNode();new ClassReader(FreezeAcceptedMethods.bytes(c,true)).accept(canon,ClassReader.SKIP_DEBUG|ClassReader.EXPAND_FRAMES);var bytes=FreezeAcceptedMethods.bytes(canon,false);if(a[0].equals("splice")){var p=Path.of(a[2]).resolve(name+".class");Files.createDirectories(p.getParent());Files.write(p,bytes);}else try(var next=new JarFile(a[2])){if(!Arrays.equals(bytes,FreezeAcceptedMethods.bytes(FreezeAcceptedMethods.read(next,name),false)))throw new AssertionError(name);}}}System.out.println("PASS physical NPC original producer/FIFO/rolled-proc/synchronous Context/final Frame and retained accepted siblings");}
}
