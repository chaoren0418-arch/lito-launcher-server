import java.nio.file.*;import java.util.*;import java.util.jar.*;import org.objectweb.asm.*;import org.objectweb.asm.tree.*;import static org.objectweb.asm.Opcodes.*;
/** Complete original reservation siblings, retaining original fences, rejection branches and handlers. */
public final class SourceMaterialPairIngressMethods {
 static final String P="local/lito381/server/",PC=P+"ClientSession",LIGHT=P+"NativeLightAction",V=P+"NativePlayerVitalCompletions",W=P+"NativeWeaponBreakTargetLifecycle",H=P+"NativeSourceMaterialPairIngress",TASK=P+"NativeSelfCastAction$Task",APP=P+"NativeServerLifetime",RECEIPT="L"+V+"$Receipt;";
 static final List<String> OWNERS=List.of(LIGHT,V,W);
 static void need(boolean b,String why){if(!b)throw new AssertionError(why);}
 static MethodNode method(ClassNode c,String name){return c.methods.stream().filter(m->m.name.equals(name)).findFirst().orElseThrow();}
 static MethodNode sibling(ClassNode c,String old,String name){var m=method(c,old);var n=SourceProcScalarSkillsMethods.copy(m,name,m.desc.replace(")","L"+TASK+";)"));SourceProcScalarSkillsMethods.shift(n,1,1);c.methods.add(n);return n;}
 static void callTask(MethodNode m,MethodInsnNode call,String name,int slot){m.instructions.insertBefore(call,new VarInsnNode(ALOAD,slot));call.setOpcode(INVOKESTATIC);call.owner=H;call.name=name;call.desc=call.desc.replace(")","L"+TASK+";)");call.itf=false;}
 static InsnList args(int app){var x=new InsnList();x.add(new VarInsnNode(ALOAD,0));x.add(new VarInsnNode(ALOAD,1));if(app>=0)x.add(new VarInsnNode(ALOAD,app));return x;}
 static void patch(ClassNode c){
  if(c.name.equals(W)){
   var m=sibling(c,"reserveTarget","nativeSourceMaterialPairTarget");int sites=0;
   for(var n:m.instructions.toArray())if(n instanceof MethodInsnNode call&&call.owner.equals(P+"NativeSourceWeaponTargetLife")&&Set.of("reserve","reuse").contains(call.name)){callTask(m,call,call.name.equals("reserve")?"targetReserve":"reuse",1);sites++;}
   need(sites==2,"original target receipt fallback and reuse only");return;
  }
  if(c.name.equals(LIGHT)){
   var original=method(c,"reserve");need((original.access&ACC_PRIVATE)!=0,"private original ingress reserve");var alias=SourceProcScalarSkillsMethods.copy(original,"nativeSourceMaterialPairOriginalReserve",original.desc);alias.access=(alias.access&~ACC_PRIVATE);c.methods.add(alias);
   var reserve=sibling(c,"reserve","nativeSourceMaterialPairIngress");reserve.access=(reserve.access&~ACC_PRIVATE);int sites=0;
   for(var n:reserve.instructions.toArray())if(n instanceof MethodInsnNode call){if(call.owner.equals(P+"NativePlayerCommit$Binding")&&call.name.equals("capture")){callTask(reserve,call,"binding",1);sites++;}else if(call.owner.equals(V)&&call.name.equals("reserve")){callTask(reserve,call,"reserve",1);sites++;}}
   need(sites==2,"original source binding and Receipt calls only");
   var m=method(c,"commitPair");int prep=0,binding=0,target=0,ingress=0;
   for(var n:m.instructions.toArray())if(n instanceof MethodInsnNode call){
    if(call.owner.equals(P+"NativeSourceLightPairLife")&&call.name.equals("task")){call.owner=H;call.name="prepare";var store=FreezeSourceTargetDelta.next(call);need(store instanceof VarInsnNode v&&v.getOpcode()==ASTORE&&v.var==2,"original prepared Task local");var gate=new InsnList();var ok=new LabelNode();gate.add(new VarInsnNode(ALOAD,2));gate.add(new MethodInsnNode(INVOKESTATIC,H,"enter","(L"+TASK+";)Z",false));gate.add(new JumpInsnNode(IFNE,ok));gate.add(new InsnNode(RETURN));gate.add(ok);m.instructions.insert(store,gate);prep++;}
    else if(call.owner.equals(W)&&call.name.equals("targetBinding")){callTask(m,call,"binding",2);binding++;}
    else if(call.owner.equals(W)&&call.name.equals("reserveTarget")){callTask(m,call,"target",2);target++;}
    else if(call.owner.equals(LIGHT)&&call.name.equals("reserve")){callTask(m,call,"ingress",2);call.desc=call.desc.substring(0,call.desc.indexOf(')')+1)+"Ljava/lang/Object;";m.instructions.insert(call,new TypeInsnNode(CHECKCAST,LIGHT+"$Ingress"));ingress++;}
   }need(prep==1&&binding==1&&target==1&&ingress==1,"four bounded shared original entry calls");return;
  }
  var m=sibling(c,"reserve","nativeSourceMaterialPairReserve");int life=0,process=0;
  for(var n:m.instructions.toArray())if(n instanceof MethodInsnNode call&&call.owner.equals(P+"NativePlayerLifetime")){
   if(call.name.equals("of")){callTask(m,call,"lifetime",1);life++;}
   else if(call.name.equals("processes")){var x=args(3);x.add(new MethodInsnNode(INVOKESTATIC,H,"processes","(ZL"+PC+";L"+TASK+";L"+APP+";)Z",false));m.instructions.insert(call,x);process++;}
  }need(life==1&&process==1,"one first-lifetime and no-app native processes gate");
  AbstractInsnNode denied=null;for(var n:m.instructions.toArray())if(n.getOpcode()==ACONST_NULL){var next=FreezeSourceTargetDelta.next(n);if(next instanceof VarInsnNode v&&v.getOpcode()==ALOAD&&v.var==4&&FreezeSourceTargetDelta.next(next).getOpcode()==MONITOREXIT){need(denied==null,"original one null/unlock return");denied=n;}}
  need(denied!=null,"original app-closing rejection");var reject=new LabelNode();m.instructions.insertBefore(denied,reject);var first=Arrays.stream(m.instructions.toArray()).filter(n->n instanceof FieldInsnNode f&&f.getOpcode()==GETSTATIC&&f.owner.equals(APP)&&f.name.equals("packets")).findFirst().orElseThrow();var gate=args(3);gate.add(new MethodInsnNode(INVOKESTATIC,H,"application","(L"+PC+";L"+TASK+";L"+APP+";)Z",false));gate.add(new JumpInsnNode(IFEQ,reject));m.instructions.insertBefore(first,gate);
  var entry=args(-1);var ok=new LabelNode();entry.add(new MethodInsnNode(INVOKESTATIC,H,"initial","(L"+PC+";L"+TASK+";)Z",false));entry.add(new JumpInsnNode(IFNE,ok));entry.add(new InsnNode(ACONST_NULL));entry.add(new InsnNode(ARETURN));entry.add(ok);m.instructions.insert(entry);
 }
 public static void main(String[]a)throws Exception{
  var urls=new ArrayList<java.net.URL>();urls.add(Path.of(a[1]).toUri().toURL());try(var files=Files.list(Path.of(a[3]))){for(var p:files.filter(x->x.toString().endsWith(".jar")).toList())urls.add(p.toUri().toURL());}
  try(var loader=new java.net.URLClassLoader(urls.toArray(java.net.URL[]::new),ClassLoader.getPlatformClassLoader());var base=new JarFile(a[1])){FreezeAcceptedMethods.hierarchy=loader;
   for(var owner:OWNERS){var c=FreezeAcceptedMethods.read(base,owner);patch(c);var canon=new ClassNode();new ClassReader(FreezeAcceptedMethods.bytes(c,true)).accept(canon,ClassReader.SKIP_DEBUG|ClassReader.EXPAND_FRAMES);var bytes=FreezeAcceptedMethods.bytes(canon,false);
    if(a[0].equals("splice")){var path=Path.of(a[2]).resolve(owner+".class");Files.createDirectories(path.getParent());Files.write(path,bytes);}else try(var jar=new JarFile(a[2])){need(Arrays.equals(bytes,FreezeAcceptedMethods.bytes(FreezeAcceptedMethods.read(jar,owner),false)),"exact material pair ingress splice "+owner);}
   }
  }if(a[0].equals("splice"))Files.write(Path.of(a[2]).getParent().resolve("material-pair-ingress-ports.tsv"),List.of("owner\tmethod\tphase",LIGHT+"\tcommitPair/reserve\tfirst Task entry and complete original ingress reserve siblings",W+"\treserveTarget\tcomplete original target receipt reuse/fallback sibling",V+"\treserve\tcomplete original native receipt sibling; first lifetime and original app fence"));
  System.out.println("PASS shared material ingress:4 original entry call ports,1 pre-reservation gate,4 complete original reserve siblings; first binding/lifetime through original receipt fences/handlers");
 }
}
