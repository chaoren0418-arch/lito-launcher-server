import java.util.*;import java.util.jar.*;import org.objectweb.asm.tree.*;import static org.objectweb.asm.Opcodes.*;
public final class SourceControlledNegativeFinalDelta {
 static final String P="local/lito381/server/",C=P+"NativeAreaMaterialAction$Context",N=P+"NativeSourceTargetNegativeLife",W=P+"NativeSourceWeaponFinalFrame",B=P+"NativeControlledNegativeMaterial",H=P+"NativeSourceControlledNegativeFinal";
 static void need(boolean b,String why){if(!b)throw new AssertionError(why);}
 static void loads(MethodNode m,AbstractInsnNode n,int...slots){for(int slot:slots){var p=FreezeAcceptedMethods.prev(n);need(p instanceof VarInsnNode v&&v.getOpcode()==ALOAD&&v.var==slot,"bounded existing operand "+slot);m.instructions.remove(p);}}
 static void remove(MethodNode m,AbstractInsnNode...nodes){for(var n:nodes)m.instructions.remove(n);}
 public static void main(String[]args)throws Exception {
  try(var old=new JarFile(args[0]);var jar=new JarFile(args[1])) {
   var originalEntries=new HashSet<String>();var changed=new HashSet<String>();for(var e:Collections.list(old.entries())){originalEntries.add(e.getName());need(jar.getJarEntry(e.getName())!=null,"all original archive entries retained");if(!Arrays.equals(old.getInputStream(e).readAllBytes(),jar.getInputStream(jar.getJarEntry(e.getName())).readAllBytes()))changed.add(e.getName());}
   need(changed.equals(Set.of(C+".class",N+".class",W+".class",B+".class")),"only four reviewed original owners changed");var fresh=new HashSet<String>();for(var e:Collections.list(jar.entries()))if(!originalEntries.contains(e.getName()))fresh.add(e.getName());need(fresh.equals(Set.of(H+".class",H+"$Scope.class",H+"$Value.class")),"fieldless helper, bounded Scope and immutable Value only");int methods=0,ports=0;
   for(var owner:List.of(C,N,W,B)) {
    var before=FreezeSourceTargetDelta.read(old,owner);var after=FreezeSourceTargetDelta.read(jar,owner);
    if(owner.equals(C)){var field=after.fields.stream().filter(f->f.name.equals("nativeSourceControlledNegativeFinal")).findFirst().orElseThrow();need(field.access==ACC_TRANSIENT&&field.desc.equals("Ljava/lang/Object;"),"one transient instance producing slot");after.fields.remove(field);}
    need(before.fields.size()==after.fields.size()&&before.methods.size()==after.methods.size(),"every original schema/method retained");
    for(int i=0;i<before.fields.size();i++){var f=before.fields.get(i);var g=after.fields.get(i);need(f.name.equals(g.name)&&f.desc.equals(g.desc)&&f.access==g.access&&Objects.equals(f.signature,g.signature)&&Objects.equals(f.value,g.value),"every original field exact");}
    for(var original:before.methods){var m=after.methods.stream().filter(x->x.name.equals(original.name)&&x.desc.equals(original.desc)).findFirst().orElseThrow();for(var node:m.instructions.toArray())if(m.instructions.contains(node)&&node instanceof MethodInsnNode call&&call.owner.equals(H)){
     switch(call.name){
      case "install"->{need(owner.equals(B),"first controlled capture only");loads(m,call,4);var previous=FreezeAcceptedMethods.prev(call);need(previous instanceof MethodInsnNode p&&p.owner.equals(N)&&p.name.equals("capture"),"install immediately follows original first-life capture without consuming its boolean");remove(m,call);}
      case "produce"->{need(owner.equals(B),"one actual original producer");call.owner=P+"CombatNpcDebuffs";call.name="nativeMaterialControlledNegativeBody";}
      case "close"->{need(owner.equals(N)&&m.name.equals("close"),"original cleanup backstop only");loads(m,call,0);remove(m,call);}
      case "capture"->{need(owner.equals(W)&&m.name.equals("capture"),"optional original weapon capture port");loads(m,call,2,1,0);var branch=FreezeSourceTargetDelta.next(call);var value=FreezeSourceTargetDelta.next(branch);var ret=FreezeSourceTargetDelta.next(value);need(branch.getOpcode()==IFEQ&&value.getOpcode()==ICONST_1&&ret.getOpcode()==IRETURN,"handled output preserves original false branch");remove(m,call,branch,value,ret);}
      case "frame"->{need(owner.equals(W)&&m.name.equals("frame"),"existing complete sender routing only");loads(m,call,3,2,1,0);var branch=FreezeSourceTargetDelta.next(call);var ret=FreezeSourceTargetDelta.next(branch);need(branch.getOpcode()==IFEQ&&ret.getOpcode()==RETURN,"unrecognized values preserve old route");remove(m,call,branch,ret);}
      case "kind"->{need(owner.equals(W)&&m.name.equals("allow"),"typed final cipher route only");loads(m,call,2);var branch=FreezeSourceTargetDelta.next(call);need(branch.getOpcode()==IFEQ,"unrecognized original final proof bypass");remove(m,call,branch);}
      case "allow"->{need(owner.equals(W)&&m.name.equals("allow"),"original final monitor check only");loads(m,call,2,1,0);var ret=FreezeSourceTargetDelta.next(call);need(ret.getOpcode()==IRETURN,"explicit final result");remove(m,call,ret);}
      default->throw new AssertionError("unexpected controlled-negative call port "+call.name);
     }ports++;
    }need(Arrays.equals(SourceNpcFirstViewDelta.body(before,original,original.name),SourceNpcFirstViewDelta.body(after,m,original.name)),"complete original method inverse "+owner+"."+original.name);methods++;}
   }
   need(ports==7,"seven explicit call ports across six existing methods");System.out.println("PASS independent controlled-negative final inverse: "+methods+" complete original methods,4 original owners,7 explicit call ports,1 transient producing slot,0 original PC/motion sender changes; original costs/status/RNG/locks/exceptions retained");
  }
 }
}
