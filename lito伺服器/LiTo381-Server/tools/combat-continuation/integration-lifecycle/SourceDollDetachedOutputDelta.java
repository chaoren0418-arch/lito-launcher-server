import java.util.*;import java.util.jar.*;import org.objectweb.asm.tree.*;import static org.objectweb.asm.Opcodes.*;
public final class SourceDollDetachedOutputDelta {
 static final String P="local/lito381/server/",N=P+"NativeSourceDollDetachedOutput",PC=P+"ClientSession",OUT=P+"NativeSourceDollOutput";static void need(boolean b,String s){if(!b)throw new AssertionError(s);}
 static byte[] body(ClassNode c,MethodNode m,String name){return SourceNpcFirstViewDelta.body(c,m,name);}
 public static void main(String[]a)throws Exception{try(var old=new JarFile(a[0]);var jar=new JarFile(a[1])){
  var original=new HashSet<String>();var changed=new HashSet<String>();
  for(var e:Collections.list(old.entries())){original.add(e.getName());var now=jar.getJarEntry(e.getName());need(now!=null,"all original entries retained");if(!Arrays.equals(old.getInputStream(e).readAllBytes(),jar.getInputStream(now).readAllBytes()))changed.add(e.getName());}
  need(changed.equals(Set.of(PC+".class",OUT+".class")),"only original ClientSession and DollOutput changed");var fresh=new HashSet<String>();for(var e:Collections.list(jar.entries()))if(!original.contains(e.getName()))fresh.add(e.getName());need(fresh.equals(Set.of(N+".class")),"one new fieldless helper only");int originals=0,ports=0,full=0;
  for(var owner:List.of(PC,OUT)){var before=FreezeSourceTargetDelta.read(old,owner);var after=FreezeSourceTargetDelta.read(jar,owner);need(before.fields.size()==after.fields.size(),"no original fields added");
   for(var f:before.fields){var g=after.fields.stream().filter(x->x.name.equals(f.name)&&x.desc.equals(f.desc)).findFirst().orElseThrow();need(f.access==g.access&&Objects.equals(f.signature,g.signature)&&Objects.equals(f.value,g.value),"all original fields retained");}
   for(var m:before.methods){var now=after.methods.stream().filter(x->x.name.equals(m.name)&&x.desc.equals(m.desc)).findFirst().orElseThrow();
    if(owner.equals(OUT)&&m.name.equals("send"))for(var n:now.instructions.toArray())if(n instanceof MethodInsnNode call&&call.owner.equals(N)&&call.name.equals("task")){
     var candidates=new ArrayList<InvokeDynamicInsnNode>();for(var prior:m.instructions)if(prior instanceof InvokeDynamicInsnNode indy&&indy.name.equals("run")&&indy.desc.equals(call.desc))candidates.add(indy);need(candidates.size()==1,"one exact original invokedynamic factory");var prior=candidates.get(0);now.instructions.set(call,new InvokeDynamicInsnNode(prior.name,prior.desc,prior.bsm,prior.bsmArgs));ports++;
    }
    need(Arrays.equals(body(before,m,m.name),body(after,now,m.name)),"complete original method inverse "+owner+"."+m.name);originals++;
    if(owner.equals(PC)&&m.name.equals("nativeStaticOriginalFrame")){
     var sibling=after.methods.stream().filter(x->x.name.equals("nativeSourceDollDetachedFinalFrame")).findFirst().orElseThrow();need(sibling.access==m.access&&(sibling.access&ACC_SYNCHRONIZED)!=0,"same original synchronized final receiver access");int guard=0;
     for(var n:sibling.instructions.toArray())if(n instanceof MethodInsnNode call&&call.owner.equals(N)&&call.name.equals("allow")){
      var operand=FreezeAcceptedMethods.prev(call);need(operand instanceof VarInsnNode v&&v.getOpcode()==ALOAD&&v.var==3,"exact explicit output operand");sibling.instructions.remove(operand);call.owner=P+"NativeSourceDollSlow";call.name="allowFrame";call.desc="(L"+PC+";[B)Z";guard++;
     }
     need(guard==1,"one exact original doll guard substitution only");SourceNpcFirstViewDelta.varsBack(sibling,4);sibling.maxLocals=m.maxLocals;need(Arrays.equals(body(before,m,m.name),body(after,sibling,m.name)),"complete original final guards/cipher/socket/log/inventory body inverse");full++;
    }
   }
   need(after.methods.size()==before.methods.size()+(owner.equals(PC)?1:0),"one synchronized full final sibling only");
  }
  need(ports==1&&full==1,"both exact ports present");var helper=FreezeSourceTargetDelta.read(jar,N);need(helper.fields.isEmpty(),"no global registry/TLS/worker/queue");
  for(var m:helper.methods){if(m.name.startsWith("lambda$"))need(!m.desc.contains("$Context;")&&!m.desc.contains("$View;")&&!m.desc.contains("$Frame;")&&!m.desc.contains("Ljava/util/Map;"),"queued lambda contains no Context/View/Frame/map");for(var n:m.instructions)if(n instanceof MethodInsnNode call)need(!Set.of("getConnection","executeQuery","executeUpdate","nextInt","damage","enqueueLocked","advance").contains(call.name),"no SQL/RNG/HP/lifetime mutation");}
  System.out.println("PASS independent detached doll output inverse: "+originals+" original methods,1 factory and1 complete original synchronized final frame; original Pending proof and all other entries byte retained");
 }}
}
