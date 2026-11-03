import java.util.*;import java.util.jar.*;import org.objectweb.asm.*;import org.objectweb.asm.tree.*;import static org.objectweb.asm.Opcodes.*;
public final class SourceNegativeFinalFrameDelta {
 static final String P="local/lito381/server/",A=P+"NativeSourceNegativeTargets$Aim",N=P+"NativeSourceNegativeTargets",W=P+"NativeSourceWeaponFinalFrame",H=P+"NativeSourceNegativeFinalFrame",OLD="lambda$run$1",COPY="nativeSourceNegativeFinalOriginalProducer";
 static void need(boolean b,String why){if(!b)throw new AssertionError(why);}
 static void loads(MethodNode m,AbstractInsnNode n,int...slots){for(int slot:slots){var p=FreezeAcceptedMethods.prev(n);need(p instanceof VarInsnNode v&&v.getOpcode()==ALOAD&&v.var==slot,"bounded original operand "+slot);m.instructions.remove(p);}}
 static void remove(MethodNode m,AbstractInsnNode...nodes){for(var n:nodes)m.instructions.remove(n);}
 public static void main(String[]args)throws Exception{
  try(var old=new JarFile(args[0]);var jar=new JarFile(args[1])){
   var entries=new HashSet<String>();var changed=new HashSet<String>();for(var e:Collections.list(old.entries())){entries.add(e.getName());need(jar.getJarEntry(e.getName())!=null,"original entry retained");if(!Arrays.equals(old.getInputStream(e).readAllBytes(),jar.getInputStream(jar.getJarEntry(e.getName())).readAllBytes()))changed.add(e.getName());}
   need(changed.equals(Set.of(A+".class",N+".class",W+".class")),"only three reviewed original owners changed");var fresh=new HashSet<String>();for(var e:Collections.list(jar.entries()))if(!entries.contains(e.getName()))fresh.add(e.getName());need(fresh.equals(Set.of(H+".class",H+"$Scope.class",H+"$Value.class")),"fieldless helper, bounded producing Scope and immutable Value only");int methods=0,ports=0,handles=0;
   for(var owner:List.of(A,N,W)){
    var before=FreezeSourceTargetDelta.read(old,owner);var after=FreezeSourceTargetDelta.read(jar,owner);
    if(owner.equals(A)){var f=after.fields.stream().filter(x->x.name.equals("nativeSourceNegativeFinal")).findFirst().orElseThrow();need(f.access==ACC_TRANSIENT&&f.desc.equals("Ljava/lang/Object;"),"one transient instance producing field");after.fields.remove(f);}
    if(owner.equals(N)){
     var original=before.methods.stream().filter(x->x.name.equals(OLD)).findFirst().orElseThrow();var sibling=after.methods.stream().filter(x->x.name.equals(COPY)).findFirst().orElseThrow();need(sibling.desc.equals(original.desc)&&sibling.access==(original.access&~(ACC_PRIVATE|ACC_SYNTHETIC)),"one complete same-descriptor accessible original producer sibling");var restored=SourceProcScalarSkillsMethods.copy(sibling,OLD,original.desc);restored.access=original.access;need(Arrays.equals(SourceNpcFirstViewDelta.body(before,original,OLD),SourceNpcFirstViewDelta.body(after,restored,OLD)),"complete original producer instructions, handlers and calls retained");after.methods.remove(sibling);
    }
    need(before.fields.size()==after.fields.size()&&before.methods.size()==after.methods.size(),"every original schema/method retained");for(int i=0;i<before.fields.size();i++){var f=before.fields.get(i);var g=after.fields.get(i);need(f.name.equals(g.name)&&f.desc.equals(g.desc)&&f.access==g.access&&Objects.equals(f.signature,g.signature)&&Objects.equals(f.value,g.value),"original field exact");}
    for(var original:before.methods){var m=after.methods.stream().filter(x->x.name.equals(original.name)&&x.desc.equals(original.desc)).findFirst().orElseThrow();for(var node:m.instructions.toArray()){
     if(node instanceof InvokeDynamicInsnNode indy)for(int i=0;i<indy.bsmArgs.length;i++)if(indy.bsmArgs[i]instanceof Handle h&&h.getOwner().equals(H)&&h.getName().equals("produce")){need(owner.equals(N)&&m.name.equals("run")&&h.getTag()==H_INVOKESTATIC,"one exact original producer handle");indy.bsmArgs[i]=new Handle(H_INVOKESTATIC,N,OLD,h.getDesc(),false);handles++;}
     if(m.instructions.contains(node)&&node instanceof MethodInsnNode call&&call.owner.equals(H)){
      switch(call.name){
       case "install","close"->{need(owner.equals(A)&&m.name.equals(call.name.equals("install")?"<init>":"close"),"bounded Aim first/cleanup port");loads(m,call,0);remove(m,call);}
       case "register"->{need(owner.equals(N)&&m.name.equals("register"),"original bounded finish registration only");loads(m,call,0);var branch=FreezeSourceTargetDelta.next(call);var ret=FreezeSourceTargetDelta.next(branch);need(branch.getOpcode()==IFEQ&&ret.getOpcode()==RETURN,"unhandled registration keeps original branch");remove(m,call,branch,ret);}
       case "capture"->{need(owner.equals(N)&&m.name.equals("captureOutput"),"original negative capture port only");loads(m,call,2,1,0);var branch=FreezeSourceTargetDelta.next(call);var value=FreezeSourceTargetDelta.next(branch);var ret=FreezeSourceTargetDelta.next(value);need(branch.getOpcode()==IFEQ&&value.getOpcode()==ICONST_1&&ret.getOpcode()==IRETURN,"handled capture preserves original false branch");remove(m,call,branch,value,ret);}
       case "frame"->{need(owner.equals(W)&&m.name.equals("frame"),"existing complete sender routing");loads(m,call,3,2,1,0);var branch=FreezeSourceTargetDelta.next(call);var ret=FreezeSourceTargetDelta.next(branch);need(branch.getOpcode()==IFEQ&&ret.getOpcode()==RETURN,"unrecognized final proof preserves original route");remove(m,call,branch,ret);}
       case "kind"->{need(owner.equals(W)&&m.name.equals("allow"),"typed final monitor route");loads(m,call,2);var branch=FreezeSourceTargetDelta.next(call);need(branch.getOpcode()==IFEQ,"unrecognized proof retains old route");remove(m,call,branch);}
       case "allow"->{need(owner.equals(W)&&m.name.equals("allow"),"original final cipher monitor only");loads(m,call,2,1,0);var ret=FreezeSourceTargetDelta.next(call);need(ret.getOpcode()==IRETURN,"explicit final result");remove(m,call,ret);}
       default->throw new AssertionError("unexpected negative final call port "+call.name);
      }ports++;
     }
    }need(Arrays.equals(SourceNpcFirstViewDelta.body(before,original,original.name),SourceNpcFirstViewDelta.body(after,m,original.name)),"complete original method inverse "+owner+"."+original.name);methods++;}
   }need(ports==7&&handles==1,"seven call ports and one complete producer handle only");System.out.println("PASS independent negative final inverse: "+methods+" complete original methods,3 original owners,7 call ports,1 producer handle,1 complete sibling,1 transient producing field; original cost/RNG/status/locks/handlers retained");
  }
 }
}
