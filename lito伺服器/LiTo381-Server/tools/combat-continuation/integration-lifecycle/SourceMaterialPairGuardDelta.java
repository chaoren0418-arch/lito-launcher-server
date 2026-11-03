import java.util.*;import java.util.jar.*;import org.objectweb.asm.tree.*;import static org.objectweb.asm.Opcodes.*;
/** Independently remove exactly the three new ports and compare every original method body. */
public final class SourceMaterialPairGuardDelta {
 static final String P="local/lito381/server/",PC=P+"ClientSession",LIGHT=P+"NativeLightAction",PAIR=P+"NativeSourceLightPairLife",H=P+"NativeSourceMaterialPairGuard";
 static void need(boolean b,String why){if(!b)throw new AssertionError(why);}
 public static void main(String[]a)throws Exception{try(var old=new JarFile(a[0]);var jar=new JarFile(a[1])){
  var names=new HashSet<String>();var changed=new HashSet<String>();for(var e:Collections.list(old.entries())){names.add(e.getName());var now=jar.getJarEntry(e.getName());need(now!=null,"all original entries retained");if(!Arrays.equals(old.getInputStream(e).readAllBytes(),jar.getInputStream(now).readAllBytes()))changed.add(e.getName());}
  need(changed.equals(Set.of(PC+".class",LIGHT+".class",PAIR+".class")),"three original owners only");var added=new HashSet<String>();for(var e:Collections.list(jar.entries()))if(!names.contains(e.getName()))added.add(e.getName());need(added.equals(Set.of(H+".class",H+"$Guarded.class")),"fieldless helper and bounded original Task values only");
  int methods=0,ports=0;for(var owner:List.of(PC,LIGHT,PAIR)){
   var before=FreezeAcceptedMethods.read(old,owner);var after=FreezeAcceptedMethods.read(jar,owner);need(before.fields.size()==after.fields.size()&&after.methods.size()==before.methods.size()+(owner.equals(PAIR)?1:0),"only one full original Task sibling");
   for(int i=0;i<before.fields.size();i++){var f=before.fields.get(i);var g=after.fields.get(i);need(f.name.equals(g.name)&&f.desc.equals(g.desc)&&f.access==g.access&&Objects.equals(f.signature,g.signature)&&Objects.equals(f.value,g.value),"all original fields exact");}
   for(var original:before.methods){var name=owner.equals(PAIR)&&original.name.equals("task")?"nativeSourceMaterialOriginalTask":original.name;var now=after.methods.stream().filter(m->m.name.equals(name)&&m.desc.equals(original.desc)).findFirst().orElseThrow();
    for(var n:now.instructions.toArray())if(n instanceof MethodInsnNode call){
     if(call.owner.equals(H)){need(owner.equals(LIGHT)&&now.name.equals("lambda$commitPair$9")&&call.name.equals("matches"),"only two shared after-wait identity ports");var load=FreezeAcceptedMethods.prev(call);need(load instanceof VarInsnNode v&&v.getOpcode()==ALOAD&&v.var==5,"explicit original Task local");now.instructions.remove(load);call.owner=P+"NativeSourceWeaponPlayerCounter";call.desc="(L"+P+"NativePlayerCommit$Binding;L"+PC+";)Z";ports++;}
     else if(owner.equals(PC)&&now.name.equals("nativeMaterialTargetNegativeBody")&&call.owner.equals(P+"BattleSpellCatalog$TargetNegativeStatusSpell")&&call.name.equals("skillId")&&call.getOpcode()==INVOKEINTERFACE&&FreezeAcceptedMethods.prev(call) instanceof VarInsnNode v&&v.var==2&&FreezeSourceTargetDelta.next(call) instanceof MethodInsnNode next&&Set.of(P+"NativeSourceNoneTargetRequest",P+"NativeSourceTargetLos").contains(next.owner)){call.setOpcode(INVOKEVIRTUAL);call.itf=false;ports++;}
    }
    need(Arrays.equals(SourceNpcFirstViewDelta.body(before,original,original.name),SourceNpcFirstViewDelta.body(after,now,original.name)),"whole original method inverse "+owner+"."+original.name);methods++;
   }
   if(owner.equals(PAIR)){var w=after.methods.stream().filter(m->m.name.equals("task")).findFirst().orElseThrow();var ns=Arrays.stream(w.instructions.toArray()).filter(n->n.getOpcode()>=0).toList();need(ns.size()==5,"one fieldless Task forwarding wrapper");for(int i=0;i<3;i++)need(ns.get(i)instanceof VarInsnNode v&&v.getOpcode()==ALOAD&&v.var==i,"original Task wrapper argument");need(ns.get(3)instanceof MethodInsnNode call&&call.owner.equals(H)&&call.name.equals("task")&&call.desc.equals(w.desc)&&call.getOpcode()==INVOKESTATIC&&ns.get(4).getOpcode()==ARETURN,"exact shared typed Task forwarder");}
  }need(ports==4,"two identity ports and two interface ABI opcodes");System.out.println("PASS independent material pair inverse: "+methods+" complete original methods,2 explicit Task identity ports,2 interface opcodes,1 complete original Task sibling,0 fields; original Ctrl/cost/RNG/status/reservation/transfer/exception/close retained");
 }}
}
