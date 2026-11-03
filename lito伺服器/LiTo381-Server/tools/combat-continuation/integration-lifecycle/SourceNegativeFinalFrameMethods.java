import java.nio.file.*;import java.util.*;import java.util.jar.*;import org.objectweb.asm.*;import org.objectweb.asm.tree.*;import static org.objectweb.asm.Opcodes.*;
public final class SourceNegativeFinalFrameMethods {
 static final String P="local/lito381/server/",A=P+"NativeSourceNegativeTargets$Aim",N=P+"NativeSourceNegativeTargets",W=P+"NativeSourceWeaponFinalFrame",H=P+"NativeSourceNegativeFinalFrame",OLD="lambda$run$1",COPY="nativeSourceNegativeFinalOriginalProducer";
 static final List<String> OWNERS=List.of(A,N,W);static void need(boolean b,String why){if(!b)throw new AssertionError(why);}
 static void patch(ClassNode c){
  if(c.name.equals(A)){
   need(c.fields.stream().noneMatch(f->f.name.equals("nativeSourceNegativeFinal")),"fresh bounded scope field");c.fields.add(new FieldNode(ACC_TRANSIENT,"nativeSourceNegativeFinal","Ljava/lang/Object;",null,null));int installed=0,closed=0;
   for(var m:c.methods)if(m.name.equals("<init>")){for(var n:m.instructions.toArray())if(n instanceof MethodInsnNode call&&call.owner.equals(P+"NativeSourceNegativePairFirst")&&call.name.equals("capture")){var x=new InsnList();x.add(new VarInsnNode(ALOAD,0));x.add(new MethodInsnNode(INVOKESTATIC,H,"install","(L"+A+";)V",false));m.instructions.insert(call,x);installed++;}}
   else if(m.name.equals("close")){var x=new InsnList();x.add(new VarInsnNode(ALOAD,0));x.add(new MethodInsnNode(INVOKESTATIC,H,"close","(L"+A+";)V",false));m.instructions.insert(x);closed++;}
   need(installed==1&&closed==1,"one constructor install and complete original close");return;
  }
  if(c.name.equals(N)){
   var original=c.methods.stream().filter(m->m.name.equals(OLD)).findFirst().orElseThrow();var sibling=SourceProcScalarSkillsMethods.copy(original,COPY,original.desc);sibling.access&=~(ACC_PRIVATE|ACC_SYNTHETIC);c.methods.add(sibling);int producer=0,registered=0,captured=0;
   for(var m:c.methods)if(m.name.equals("run")){for(var n:m.instructions.toArray())if(n instanceof InvokeDynamicInsnNode indy)for(int i=0;i<indy.bsmArgs.length;i++)if(indy.bsmArgs[i]instanceof Handle h&&h.getOwner().equals(N)&&h.getName().equals(OLD)){need(h.getTag()==H_INVOKESTATIC&&h.getDesc().equals(original.desc),"complete original producer shape");indy.bsmArgs[i]=new Handle(H_INVOKESTATIC,H,"produce",h.getDesc(),false);producer++;}}
   else if(m.name.equals("register")){var x=new InsnList();var old=new LabelNode();x.add(new VarInsnNode(ALOAD,0));x.add(new MethodInsnNode(INVOKESTATIC,H,"register","(L"+A+";)Z",false));x.add(new JumpInsnNode(IFEQ,old));x.add(new InsnNode(RETURN));x.add(old);m.instructions.insert(x);registered++;}
   else if(m.name.equals("captureOutput")){var x=new InsnList();var old=new LabelNode();for(int i=0;i<3;i++)x.add(new VarInsnNode(ALOAD,i));x.add(new MethodInsnNode(INVOKESTATIC,H,"capture",m.desc,false));x.add(new JumpInsnNode(IFEQ,old));x.add(new InsnNode(ICONST_1));x.add(new InsnNode(IRETURN));x.add(old);m.instructions.insert(x);captured++;}
   need(producer==1&&registered==1&&captured==1,"one complete producer handle, registration and output capture");return;
  }
  int frame=0,allow=0;for(var m:c.methods)if(m.name.equals("frame")){var x=new InsnList();var old=new LabelNode();for(int i=0;i<4;i++)x.add(new VarInsnNode(ALOAD,i));x.add(new MethodInsnNode(INVOKESTATIC,H,"frame",m.desc.replace(")V",")Z"),false));x.add(new JumpInsnNode(IFEQ,old));x.add(new InsnNode(RETURN));x.add(old);m.instructions.insert(x);frame++;}
  else if(m.name.equals("allow")){var x=new InsnList();var old=new LabelNode();x.add(new VarInsnNode(ALOAD,2));x.add(new MethodInsnNode(INVOKESTATIC,H,"kind","(Ljava/lang/Object;)Z",false));x.add(new JumpInsnNode(IFEQ,old));for(int i=0;i<3;i++)x.add(new VarInsnNode(ALOAD,i));x.add(new MethodInsnNode(INVOKESTATIC,H,"allow",m.desc,false));x.add(new InsnNode(IRETURN));x.add(old);m.instructions.insert(x);allow++;}
  need(frame==1&&allow==1,"existing explicit complete sender routing only");
 }
 public static void main(String[]args)throws Exception{
  var urls=new ArrayList<java.net.URL>();urls.add(Path.of(args[1]).toUri().toURL());try(var files=Files.list(Path.of(args[3]))){for(var p:files.filter(x->x.toString().endsWith(".jar")).toList())urls.add(p.toUri().toURL());}
  try(var loader=new java.net.URLClassLoader(urls.toArray(java.net.URL[]::new),ClassLoader.getPlatformClassLoader());var base=new JarFile(args[1])){FreezeAcceptedMethods.hierarchy=loader;for(var owner:OWNERS){var c=FreezeAcceptedMethods.read(base,owner);patch(c);var canon=new ClassNode();new ClassReader(FreezeAcceptedMethods.bytes(c,true)).accept(canon,ClassReader.SKIP_DEBUG|ClassReader.EXPAND_FRAMES);var bytes=FreezeAcceptedMethods.bytes(canon,false);if(args[0].equals("splice")){var p=Path.of(args[2]).resolve(owner+".class");Files.createDirectories(p.getParent());Files.write(p,bytes);}else try(var jar=new JarFile(args[2])){need(Arrays.equals(bytes,FreezeAcceptedMethods.bytes(FreezeAcceptedMethods.read(jar,owner),false)),"same bounded negative final splice "+owner);}}}
  System.out.println("PASS negative final splice:3 original owners,1 transient scope field,1 complete producer sibling,7 call ports plus1 producer handle; complete original PC/motion senders retained");
 }
}
