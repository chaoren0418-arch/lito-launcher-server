import java.nio.file.*;import java.util.*;import java.util.jar.*;import org.objectweb.asm.*;import org.objectweb.asm.tree.*;import static org.objectweb.asm.Opcodes.*;
public final class SourceNegativePairFirstMethods {
 static final String P="local/lito381/server/",AREA=P+"NativeAreaMaterialAction",AIM=P+"NativeSourceNegativeTargets$Aim",H=P+"NativeSourceNegativePairFirst";
 static final List<String> OWNERS=List.of(AREA,AIM);static void need(boolean b,String why){if(!b)throw new AssertionError(why);}
 static void patch(ClassNode c){
  if(c.name.equals(AIM)){
   need(c.fields.stream().noneMatch(f->f.name.equals("nativeSourceNegativePairFirst")),"fresh bounded first intent field");c.fields.add(new FieldNode(ACC_TRANSIENT,"nativeSourceNegativePairFirst","Ljava/lang/Object;",null,null));int capture=0,valid=0,closed=0;
   for(var m:c.methods)if(m.name.equals("<init>")){for(var n:m.instructions.toArray())if(n.getOpcode()==RETURN){var x=new InsnList();x.add(new VarInsnNode(ALOAD,0));x.add(new MethodInsnNode(INVOKESTATIC,H,"capture","(L"+AIM+";)V",false));m.instructions.insertBefore(n,x);capture++;}}
   else if(m.name.equals("valid")){var x=new InsnList();var old=new LabelNode();x.add(new VarInsnNode(ALOAD,0));x.add(new MethodInsnNode(INVOKESTATIC,H,"current","(L"+AIM+";)Z",false));x.add(new JumpInsnNode(IFNE,old));x.add(new InsnNode(ICONST_0));x.add(new InsnNode(IRETURN));x.add(old);m.instructions.insert(x);valid++;}
   else if(m.name.equals("close")){var x=new InsnList();x.add(new VarInsnNode(ALOAD,0));x.add(new MethodInsnNode(INVOKESTATIC,H,"close","(L"+AIM+";)V",false));m.instructions.insert(x);closed++;}
   need(capture==1&&valid==1&&closed==1,"one original constructor, validity and cleanup each");return;
  }
  int retry=0,scene=0;for(var m:c.methods)if(m.name.equals("nativeCompanionAreaCapturedRun"))for(var n:m.instructions.toArray())if(n instanceof MethodInsnNode call){
   if(call.owner.equals(P+"NativePlayerCommit$Binding")&&call.name.equals("matches")){m.instructions.insertBefore(call,new VarInsnNode(ALOAD,3));call.setOpcode(INVOKESTATIC);call.owner=H;call.name="retry";call.desc="(L"+P+"NativePlayerCommit$Binding;L"+P+"ClientSession;L"+AREA+"$Context;)Z";call.itf=false;retry++;}
   else if(call.owner.equals(P+"NativeSourceAreaPlayerProof")&&call.name.equals("scene")){call.owner=H;scene++;}
  }need(retry==1&&scene==1,"two actual original roster-retry/precommit checks only");
 }
 public static void main(String[]args)throws Exception{
  var urls=new ArrayList<java.net.URL>();urls.add(Path.of(args[1]).toUri().toURL());try(var files=Files.list(Path.of(args[3]))){for(var p:files.filter(x->x.toString().endsWith(".jar")).toList())urls.add(p.toUri().toURL());}
  try(var loader=new java.net.URLClassLoader(urls.toArray(java.net.URL[]::new),ClassLoader.getPlatformClassLoader());var base=new JarFile(args[1])){FreezeAcceptedMethods.hierarchy=loader;for(var owner:OWNERS){var c=FreezeAcceptedMethods.read(base,owner);patch(c);var canon=new ClassNode();new ClassReader(FreezeAcceptedMethods.bytes(c,true)).accept(canon,ClassReader.SKIP_DEBUG|ClassReader.EXPAND_FRAMES);byte[] bytes=FreezeAcceptedMethods.bytes(canon,false);if(args[0].equals("splice")){var p=Path.of(args[2]).resolve(owner+".class");Files.createDirectories(p.getParent());Files.write(p,bytes);}else try(var jar=new JarFile(args[2])){need(Arrays.equals(bytes,FreezeAcceptedMethods.bytes(FreezeAcceptedMethods.read(jar,owner),false)),"same bounded first-proof splice "+owner);}}}
  System.out.println("PASS negative first intent splice:2 original owners,5 explicit ports,1 transient Aim field; original roster retry/unlock/precommit/producer and cleanup retained");
 }
}
