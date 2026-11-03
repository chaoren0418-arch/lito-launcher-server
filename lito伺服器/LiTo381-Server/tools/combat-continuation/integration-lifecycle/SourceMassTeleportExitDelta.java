import java.util.*;import java.util.jar.*;import org.objectweb.asm.*;import org.objectweb.asm.tree.*;import static org.objectweb.asm.Opcodes.*;

public final class SourceMassTeleportExitDelta {
 static final String P="local/lito381/server/",A=P+"NativeAreaMaterialAction",L=P+"NativeLightAction",H=P+"NativeSourceMassTeleportExit",PC=P+"ClientSession",B=P+"NativePlayerCommit$Binding",PROOF="L"+H+"$Proof;";
 static void need(boolean b,String s){if(!b)throw new AssertionError(s);}
 static void load(MethodNode m,AbstractInsnNode n,int slot){var p=FreezeAcceptedMethods.prev(n);need(p instanceof VarInsnNode v&&v.getOpcode()==ALOAD&&v.var==slot,"exact original first-proof operand");m.instructions.remove(p);}
 static String strip(String d){int i=d.indexOf(')');need(d.substring(0,i).endsWith(PROOF),"one trailing first-proof argument");return d.substring(0,i-PROOF.length())+d.substring(i);}
 public static void main(String[]args)throws Exception {
  try(var old=new JarFile(args[0]);var jar=new JarFile(args[1])) {
   var entries=new HashSet<String>();var changed=new HashSet<String>();
   for(var e:Collections.list(old.entries())){entries.add(e.getName());var now=jar.getJarEntry(e.getName());need(now!=null,"every original jar entry retained");if(!Arrays.equals(old.getInputStream(e).readAllBytes(),jar.getInputStream(now).readAllBytes()))changed.add(e.getName());}
   need(changed.equals(Set.of(A+".class",L+".class")),"only the two reviewed original owners changed");
   var fresh=new HashSet<String>();for(var e:Collections.list(jar.entries()))if(!entries.contains(e.getName()))fresh.add(e.getName());
   need(fresh.equals(Set.of(H+".class",H+"$Proof.class")),"only fieldless helper and immutable explicit Proof added");
   int methods=0,ports=0;
   for(String owner:List.of(A,L)) {
    var before=FreezeSourceTargetDelta.read(old,owner);var after=FreezeSourceTargetDelta.read(jar,owner);
    need(before.fields.size()==after.fields.size()&&before.methods.size()+1==after.methods.size(),"zero fields and one bounded method per owner");
    for(int i=0;i<before.fields.size();i++){var f=before.fields.get(i);var g=after.fields.get(i);need(f.name.equals(g.name)&&f.desc.equals(g.desc)&&f.access==g.access&&Objects.equals(f.signature,g.signature)&&Objects.equals(f.value,g.value),"every original field retained exactly");}
    if(owner.equals(L)) {
     var bridge=after.methods.stream().filter(m->m.name.equals("nativeSourceMassTeleportIngress")).findFirst().orElseThrow();var ns=Arrays.stream(bridge.instructions.toArray()).filter(n->n.getOpcode()>=0).toList();
     need(ns.size()==5&&bridge.access==ACC_STATIC&&bridge.desc.equals("(L"+PC+";)Ljava/lang/Object;"),"bounded readonly bridge shape");
     need(ns.get(0)instanceof VarInsnNode v&&v.getOpcode()==ALOAD&&v.var==0&&ns.get(1)instanceof FieldInsnNode f&&f.owner.equals(L)&&f.name.equals("ingress")&&f.getOpcode()==GETSTATIC&&ns.get(2)instanceof MethodInsnNode get&&get.owner.equals("java/lang/ThreadLocal")&&get.name.equals("get")&&ns.get(3)instanceof MethodInsnNode call&&call.owner.equals(H)&&call.name.equals("ingress")&&ns.get(4).getOpcode()==ARETURN,"bridge only reads the existing ingress");after.methods.remove(bridge);
    } else {
     var sibling=after.methods.stream().filter(m->m.name.equals("nativeSourceMassTeleportExitOriginalUnlock")).findFirst().orElseThrow();int siblingPorts=0;
     for(var node:sibling.instructions.toArray())if(node instanceof MethodInsnNode call&&call.owner.equals(H)) {
      load(sibling,call,2);call.setOpcode(INVOKEVIRTUAL);
      if(call.name.equals("matches")){call.owner=B;call.desc="(L"+PC+";)Z";}
      else {need(call.name.equals("send"),"only original complete unlock sender port");call.owner=PC;call.name="nativeSelfOriginalSend";call.desc="([BLjava/lang/String;)V";}
      siblingPorts++;
     }
     need(siblingPorts==2,"two reviewed sibling proof ports");sibling.desc=strip(sibling.desc);
     for(var n:sibling.instructions){if(n instanceof VarInsnNode v&&v.var>=3)v.var--;else if(n instanceof IincInsnNode v&&v.var>=3)v.var--;}
     var original=before.methods.stream().filter(m->m.name.equals("lambda$massTeleport$1")).findFirst().orElseThrow();
     need(Arrays.equals(SourceNpcFirstViewDelta.body(before,original,original.name),SourceNpcFirstViewDelta.body(after,sibling,original.name)),"complete original unlock lambda body restored by two-port inverse");after.methods.remove(sibling);
    }
    for(var original:before.methods) {
     var m=after.methods.stream().filter(x->x.name.equals(original.name)&&x.desc.equals(original.desc)).findFirst().orElseThrow();
     for(var node:m.instructions.toArray())if(m.instructions.contains(node)) {
      if(node instanceof MethodInsnNode call&&call.owner.equals(H)) {
       need(owner.equals(A)&&m.name.equals("massTeleport"),"only reviewed mass-teleport original method");
       if(call.name.equals("capture")){load(m,call,3);load(m,call,0);var store=FreezeSourceTargetDelta.next(call);need(store instanceof VarInsnNode v&&v.getOpcode()==ASTORE&&v.var==9,"one initial first-proof local");m.instructions.remove(store);m.instructions.remove(call);}
       else {load(m,call,9);if(call.name.equals("matches")){call.setOpcode(INVOKEVIRTUAL);call.owner=B;call.desc="(L"+PC+";)Z";}else {need(call.name.equals("completion"),"only original publication capture port");call.owner=P+"NativeRolePublication";call.desc=strip(call.desc);}}
       ports++;
      } else if(node instanceof InvokeDynamicInsnNode dynamic) {
       for(int i=0;i<dynamic.bsmArgs.length;i++)if(dynamic.bsmArgs[i]instanceof Handle handle&&handle.getName().equals("nativeSourceMassTeleportExitOriginalUnlock")) {load(m,dynamic,9);dynamic.desc=strip(dynamic.desc);dynamic.bsmArgs[i]=new Handle(handle.getTag(),A,"lambda$massTeleport$1",strip(handle.getDesc()),handle.isInterface());ports++;}
      }
     }
     need(Arrays.equals(SourceNpcFirstViewDelta.body(before,original,original.name),SourceNpcFirstViewDelta.body(after,m,original.name)),"whole original method inverse "+owner+"."+original.name);methods++;
    }
   }
   need(ports==7,"seven bounded original method proof/capture/closure ports");
   System.out.println("PASS independent mass-teleport exit inverse: "+methods+" complete original methods,2 original owners,0 fields,7 original ports,2 original sibling ports; original material,portal,massUnlock,TLS,exception and cleanup flow retained");
  }
 }
}
