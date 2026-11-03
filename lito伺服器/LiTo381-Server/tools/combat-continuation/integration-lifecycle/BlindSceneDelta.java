import java.util.*;import java.util.jar.*;import org.objectweb.asm.*;import org.objectweb.asm.tree.*;import static org.objectweb.asm.Opcodes.*;
/** Independent inverse of every original-method port and the unchanged archive/schema. */
public final class BlindSceneDelta {
 static final String P="local/lito381/server/",PC=P+"ClientSession",B=P+"BattleCompanion",T=P+"BattleTimedStatus",H=P+"NativeBlindSceneAction",S=P+"NativeBlindSourceState",OLD=P+"NativeBlindMaterialAction";
 static ClassNode read(JarFile j,String name)throws Exception{var c=new ClassNode();new ClassReader(j.getInputStream(j.getJarEntry(name+".class"))).accept(c,ClassReader.SKIP_DEBUG|ClassReader.EXPAND_FRAMES);return c;}
 static byte[] method(ClassNode c,MethodNode m,String name,int access){var q=new ClassNode();q.version=c.version;q.access=c.access;q.name=c.name;q.superName=c.superName;var copy=new MethodNode(access,name,m.desc,m.signature,m.exceptions.toArray(String[]::new));m.accept(copy);q.methods.add(copy);var w=new ClassWriter(ClassWriter.COMPUTE_MAXS);q.accept(w);return w.toByteArray();}
 static AbstractInsnNode prev(AbstractInsnNode n){do{n=n.getPrevious();}while(n!=null&&n.getOpcode()<0);return n;}
 static void before(MethodNode m,AbstractInsnNode n,int count){for(int i=0;i<count;i++)m.instructions.remove(prev(n));}
 public static void main(String[]a)throws Exception{try(var old=new JarFile(a[0]);var next=new JarFile(a[1])){
  var names=new HashSet<String>();var changed=new HashSet<String>();for(var e:Collections.list(old.entries())){names.add(e.getName());var n=next.getJarEntry(e.getName());if(n==null)throw new AssertionError("removed entry");if(!Arrays.equals(old.getInputStream(e).readAllBytes(),next.getInputStream(n).readAllBytes()))changed.add(e.getName());}var fresh=new HashSet<String>();for(var e:Collections.list(next.entries()))if(!names.contains(e.getName()))fresh.add(e.getName());if(!changed.equals(Set.of(PC+".class",B+".class",T+".class"))||!fresh.equals(Set.of(H+".class",S+".class")))throw new AssertionError("archive boundary");
  int roots=0,chance=0,admit=0,erase=0,sleep=0,counter=0,cancel=0,report=0,clear=0;
  for(String name:List.of(PC,B,T)){var originalClass=read(old,name);var actualClass=read(next,name);if(originalClass.fields.size()!=actualClass.fields.size()||actualClass.methods.size()!=originalClass.methods.size()+(name.equals(PC)?1:0))throw new AssertionError("schema");for(int i=0;i<originalClass.fields.size();i++){var x=originalClass.fields.get(i);var y=actualClass.fields.get(i);if(!x.name.equals(y.name)||!x.desc.equals(y.desc)||x.access!=y.access||!Objects.equals(x.value,y.value))throw new AssertionError("field schema");}
   for(var original:originalClass.methods){var m=actualClass.methods.stream().filter(v->v.name.equals(original.name)&&v.desc.equals(original.desc)).findFirst().orElseThrow();for(var n:m.instructions.toArray())if(n instanceof MethodInsnNode v){
     if(name.equals(PC)&&v.owner.equals(H))switch(v.name){
      case "battle","world"->{if(!Set.of("useBlindSpell","useClassicWorldBlindSpell").contains(m.name))throw new AssertionError("root only");v.owner=OLD;roots++;}
      case "chance"->{before(m,n,3);m.instructions.remove(n);chance++;}
      case "admit"->{v.owner=OLD;admit++;}
      case "erase"->{v.setOpcode(INVOKEVIRTUAL);v.owner=PC;v.name="consumeEraseMagicAfterProbability";v.desc="("+v.desc.substring(("(L"+PC+";").length());erase++;}
      case "sleep"->{v.setOpcode(INVOKEVIRTUAL);v.owner=PC;if(v.desc.contains("BattleQuestEncounter")){before(m,n,2);v.name="breakSleepForStatusSpell";v.desc="("+v.desc.substring(("(L"+PC+";").length()).replace("I)V",")V");}else{before(m,n,3);v.name="cancelSleep";v.desc="(Ljava/lang/String;)V";}sleep++;}
      case "counter"->{before(m,n,3);v.setOpcode(INVOKEVIRTUAL);v.owner=T;v.name="consumeCounterMagic";v.desc="(J)Z";counter++;}
      default->throw new AssertionError("unknown PC port "+v.name);
     }
     if(name.equals(B)&&v.owner.equals(S)){if(!m.name.equals("cancelMagicEffects")||!v.name.equals("cancel")||(m.access&ACC_SYNCHRONIZED)==0)throw new AssertionError("owned actor cancel");before(m,n,1);m.instructions.remove(n);cancel++;}
     if(name.equals(T)&&v.owner.equals(S)){if(!m.name.equals("cancelMagicEffects")||(m.access&ACC_SYNCHRONIZED)==0)throw new AssertionError("owned status cancel");switch(v.name){case "removed"->{m.instructions.remove(n);report++;}case "cleared"->{var field=prev(n);var receiver=prev(field);if(!(field instanceof FieldInsnNode f)||!f.name.equals("blindExpiresAtNanos")||!(receiver instanceof VarInsnNode r)||r.var!=0)throw new AssertionError("exact lease field");m.instructions.remove(field);m.instructions.remove(receiver);m.instructions.set(n,new InsnNode(LCONST_0));clear++;}default->throw new AssertionError("unknown status port");}}
    }
    if(!Arrays.equals(method(originalClass,original,original.name,original.access),method(actualClass,m,m.name,m.access)))throw new AssertionError("original method changed "+name+"."+m.name);
   }
   if(name.equals(PC)){var original=originalClass.methods.stream().filter(m->m.name.equals("combatProbability$57")).findFirst().orElseThrow();var alias=actualClass.methods.stream().filter(m->m.name.equals("nativeCompanionBlindChance")).findFirst().orElseThrow();if(!Arrays.equals(method(originalClass,original,original.name,original.access),method(actualClass,alias,original.name,original.access)))throw new AssertionError("actual probability clone");}
  }
  if(roots!=2||chance!=2||admit!=2||erase!=2||sleep!=2||counter!=2||cancel!=1||report!=1||clear!=1)throw new AssertionError("port counts");for(String helper:List.of(H,S)){var c=read(next,helper);if(!c.fields.isEmpty())throw new AssertionError("stateless helper");for(var m:c.methods)for(var n:m.instructions){if(n instanceof TypeInsnNode t&&t.getOpcode()==NEW&&(t.desc.contains("Thread")||t.desc.contains("Map")||t.desc.contains("Executor")))throw new AssertionError("registry/thread");if(n instanceof MethodInsnNode v&&(v.owner.startsWith("java/sql/")||v.owner.startsWith("java/nio/file/")||v.owner.contains("AccountRepository")||v.owner.contains("GameStateStore")||v.owner.contains("Executor")))throw new AssertionError("new IO");}}
 }System.out.println("PASS independent inverse of all ports; original probability/cost/predicates/remaining methods and archive preserved; zero added actor fields/TLS/registries/workers/SQL");}
}
