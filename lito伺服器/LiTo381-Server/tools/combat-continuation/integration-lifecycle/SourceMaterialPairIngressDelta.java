import java.util.*;import java.util.jar.*;import org.objectweb.asm.tree.*;import static org.objectweb.asm.Opcodes.*;
public final class SourceMaterialPairIngressDelta {
 static final String P="local/lito381/server/",PC=P+"ClientSession",L=P+"NativeLightAction",V=P+"NativePlayerVitalCompletions",W=P+"NativeWeaponBreakTargetLifecycle",H=P+"NativeSourceMaterialPairIngress";
 static void need(boolean b,String why){if(!b)throw new AssertionError(why);}
 static void load(MethodNode m,AbstractInsnNode at,int slot){var n=FreezeAcceptedMethods.prev(at);need(n instanceof VarInsnNode v&&v.getOpcode()==ALOAD&&v.var==slot,"exact Task/first-proof operand "+slot);m.instructions.remove(n);}
 static void body(ClassNode a,MethodNode old,ClassNode b,MethodNode now){need(Arrays.equals(SourceNpcFirstViewDelta.body(a,old,old.name),SourceNpcFirstViewDelta.body(b,now,old.name)),"complete original inverse "+a.name+"."+old.name);}
 static void guard(MethodNode m,MethodInsnNode call,boolean nil){var branch=FreezeSourceTargetDelta.next(call);need(branch.getOpcode()==IFNE,"bounded pre-reserve guard");var ret=FreezeSourceTargetDelta.next(branch);if(nil){need(ret.getOpcode()==ACONST_NULL,"original reserve rejection value");var next=FreezeSourceTargetDelta.next(ret);need(next.getOpcode()==ARETURN,"reserve rejection return");m.instructions.remove(next);}else need(ret.getOpcode()==RETURN,"commitPair early return before registration");m.instructions.remove(call);m.instructions.remove(branch);m.instructions.remove(ret);}
 static void shift(MethodNode m,MethodNode old){for(var n:m.instructions){if(n instanceof VarInsnNode v&&v.var>=2)v.var--;if(n instanceof IincInsnNode v&&v.var>=2)v.var--;}m.desc=old.desc;m.maxLocals=old.maxLocals;m.access=old.access;}
 public static void main(String[]a)throws Exception{try(var old=new JarFile(a[0]);var jar=new JarFile(a[1])){
  var names=new HashSet<String>();var changed=new HashSet<String>();for(var e:Collections.list(old.entries())){names.add(e.getName());var n=jar.getJarEntry(e.getName());need(n!=null,"original entry retained");if(!Arrays.equals(old.getInputStream(e).readAllBytes(),jar.getInputStream(n).readAllBytes()))changed.add(e.getName());}need(changed.equals(Set.of(L+".class",V+".class",W+".class")),"three original reservation owners only");var added=new HashSet<String>();for(var e:Collections.list(jar.entries()))if(!names.contains(e.getName()))added.add(e.getName());need(added.equals(Set.of(H+".class")),"one fieldless ingress helper only");
  int methods=0,ports=0,siblings=0;for(var owner:List.of(L,V,W)){
   var before=FreezeAcceptedMethods.read(old,owner);var after=FreezeAcceptedMethods.read(jar,owner);need(before.fields.size()==after.fields.size()&&after.methods.size()==before.methods.size()+(owner.equals(L)?2:1),"four complete original siblings; no original fields");
   for(int i=0;i<before.fields.size();i++){var f=before.fields.get(i);var g=after.fields.get(i);need(f.name.equals(g.name)&&f.desc.equals(g.desc)&&f.access==g.access&&Objects.equals(f.signature,g.signature)&&Objects.equals(f.value,g.value),"original field bytes exact");}
   for(var m:before.methods){var now=after.methods.stream().filter(x->x.name.equals(m.name)&&x.desc.equals(m.desc)).findFirst().orElseThrow();for(var n:now.instructions.toArray())if(n instanceof MethodInsnNode call&&call.owner.equals(H)){
    need(owner.equals(L)&&m.name.equals("commitPair"),"only original commitPair entry edited");switch(call.name){
     case "prepare"->{call.owner=P+"NativeSourceLightPairLife";call.name="task";ports++;}
     case "enter"->{load(now,call,2);guard(now,call,false);ports++;}
     case "binding","target","ingress"->{String name=call.name;load(now,call,2);if(name.equals("ingress")){var cast=FreezeSourceTargetDelta.next(call);need(cast instanceof TypeInsnNode t&&t.getOpcode()==CHECKCAST&&t.desc.equals(L+"$Ingress"),"private original Ingress cast");now.instructions.remove(cast);}call.owner=name.equals("ingress")?L:W;call.name=name.equals("binding")?"targetBinding":name.equals("target")?"reserveTarget":"reserve";call.desc="(L"+PC+";)L"+(name.equals("binding")?P+"NativePlayerCommit$Binding":name.equals("target")?V+"$Receipt":L+"$Ingress")+";";ports++;}
     default->throw new AssertionError("unexpected original entry port "+call.name);
    }
   }body(before,m,after,now);methods++;}
   var original=before.methods.stream().filter(m->m.name.equals(owner.equals(W)?"reserveTarget":"reserve")).findFirst().orElseThrow();var pairs=owner.equals(L)?List.of("nativeSourceMaterialPairOriginalReserve","nativeSourceMaterialPairIngress"):List.of(owner.equals(V)?"nativeSourceMaterialPairReserve":"nativeSourceMaterialPairTarget");
   for(var name:pairs){var m=after.methods.stream().filter(x->x.name.equals(name)).findFirst().orElseThrow();need(m.access==(owner.equals(L)?original.access&~ACC_PRIVATE:original.access),"only package bridge visibility for original private ingress");int local=0;
    for(var n:m.instructions.toArray())if(m.instructions.contains(n)&&n instanceof MethodInsnNode call&&call.owner.equals(H)){
     switch(call.name){
      case "initial"->{load(m,call,1);load(m,call,0);guard(m,call,true);local++;}
      case "application"->{for(int slot:new int[]{3,1,0})load(m,call,slot);var branch=FreezeSourceTargetDelta.next(call);need(branch instanceof JumpInsnNode j&&j.getOpcode()==IFEQ,"original null/unlock application branch");m.instructions.remove(branch);m.instructions.remove(call);local++;}
      case "processes"->{for(int slot:new int[]{3,1,0})load(m,call,slot);m.instructions.remove(call);local++;}
      case "lifetime"->{load(m,call,1);call.owner=P+"NativePlayerLifetime";call.name="of";call.desc="(L"+PC+";)L"+P+"NativePlayerLifetime;";local++;}
      case "binding"->{need(owner.equals(L),"source binding in original ingress clone");load(m,call,1);call.owner=P+"NativePlayerCommit$Binding";call.name="capture";call.desc="(L"+PC+";)L"+P+"NativePlayerCommit$Binding;";local++;}
      case "reserve"->{need(owner.equals(L),"source receipt in original ingress clone");load(m,call,1);call.owner=V;call.desc="(L"+PC+";)L"+V+"$Receipt;";local++;}
      case "reuse","targetReserve"->{need(owner.equals(W),"target receipt calls in original target clone");load(m,call,1);call.owner=P+"NativeSourceWeaponTargetLife";call.name=call.name.equals("reuse")?"reuse":"reserve";call.desc=call.desc.replace("L"+P+"NativeSelfCastAction$Task;","");local++;}
      default->throw new AssertionError("unexpected sibling port "+call.name);
     }
    }
    need(local==(owner.equals(V)?4:owner.equals(W)||name.equals("nativeSourceMaterialPairIngress")?2:0),"exact native reserve sibling port count");
    if(!name.equals("nativeSourceMaterialPairOriginalReserve"))shift(m,original);else m.access=original.access;
    body(before,original,after,m);siblings++;
   }
  }need(ports==5&&siblings==4,"four original call ports,one gate,four complete reserve siblings");System.out.println("PASS independent material pair ingress inverse: "+methods+" complete original methods,4 entry call ports,1 pre-reservation gate,4 complete original siblings,0 fields; original app fence/pending/Receipt/rollback/transfer/exception/lock/Ctrl/cost/RNG bodies retained");
 }}
}
