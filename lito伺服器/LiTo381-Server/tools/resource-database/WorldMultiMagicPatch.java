import java.util.*;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
import static org.objectweb.asm.Opcodes.*;

/** Three ordinary-monster loops only; original branches retained verbatim. */
final class WorldMultiMagicPatch {
 static final String P="local/lito381/server/",C=P+"ClientSession",W=P+"ClassicWorldMonsterPopulation",H=P+"CombatWorldMultiMagic",B=H+"$Batch",SNAP="L"+W+"$Snapshot;",LEASE="L"+P+"BattleEraseMagicStatus$Lease;";
 static final Set<String> PATHS=Set.of("useClassicWorldLineAttackSpell","useClassicWorldCenteredAreaAttackSpell","useClassicWorldAreaAttackSpell");
 static AbstractInsnNode prior(AbstractInsnNode n){return DragonPatch.prior(n);}static AbstractInsnNode next(AbstractInsnNode n){return DragonPatch.next(n);}
 static int local(MethodNode m,String name,String desc,AbstractInsnNode at){int index=m.instructions.indexOf(at);return m.localVariables.stream().filter(v->v.name.equals(name)&&(desc==null||v.desc.equals(desc))&&m.instructions.indexOf(v.start)<=index&&m.instructions.indexOf(v.end)>index).mapToInt(v->v.index).findFirst().orElseThrow(()->new IllegalStateException(m.name+" missing scoped "+name+"/"+desc));}
 static MethodInsnNode call(String n,String d){return new MethodInsnNode(INVOKESTATIC,H,n,d,false);}
 static void apply(ClassNode c,Map<String,Integer> hooks){
  var touched=new ArrayList<MethodNode>();
  for(var m:c.methods){if(!PATHS.contains(m.name)||!Arrays.stream(m.instructions.toArray()).anyMatch(n->n instanceof MethodInsnNode x&&x.owner.equals(P+"GameHub")&&x.name.equals("damageClassicWorldMonster")))continue;
   var hp=Arrays.stream(m.instructions.toArray()).filter(n->n instanceof MethodInsnNode x&&x.owner.equals(P+"GameHub")&&x.name.equals("damageClassicWorldMonster")).findFirst().orElseThrow();int hit=((VarInsnNode)next(hp)).var;
   var transition=m.localVariables.stream().filter(v->v.index==hit&&v.name.equals("transition")&&v.desc.equals("L"+W+"$Hit;")).findFirst().orElseThrow();int entry=local(m,"entry","Ljava/util/Map$Entry;",hp),spell=DragonPatch.local(m,"spell"),damages=DragonPatch.local(m,"monsterDamages");
   MethodInsnNode put=null;
   for(var n:m.instructions.toArray())if(n instanceof MethodInsnNode x&&x.owner.equals("java/util/Map")&&x.name.equals("put")){
    var cursor=prior(x);boolean map=false;for(int i=0;i<7&&cursor!=null;i++,cursor=prior(cursor))if(cursor instanceof VarInsnNode v&&v.getOpcode()==ALOAD&&v.var==damages)map=true;
    if(map){if(put!=null)throw new IllegalStateException("duplicate ordinary damage put");put=x;}
   }
   if(put==null)throw new IllegalStateException("no ordinary monster damage map");int target=local(m,"current",SNAP,put),erase=local(m,"eraseLease",LEASE,put);
   MethodInsnNode scalar=null;for(var n=put.getPrevious();n!=null;n=n.getPrevious())if(n instanceof MethodInsnNode x&&x.owner.equals(P+"CombatNpcMagic")&&Set.of("area","line").contains(x.name)){scalar=x;break;}
   if(scalar==null)throw new IllegalStateException("no ordinary monster scalar");m.instructions.insertBefore(scalar,new VarInsnNode(ALOAD,target));m.instructions.set(scalar,call(scalar.name,scalar.desc.substring(0,scalar.desc.indexOf(')'))+SNAP+")I"));
   int batch=MagicDamagePatch.appendLocal(c,m,B);var init=Arrays.stream(m.instructions.toArray()).filter(n->n instanceof VarInsnNode v&&v.getOpcode()==ASTORE&&v.var==batch).findFirst().orElseThrow();m.instructions.set(prior(init),call("create","()L"+B+";"));
   var add=new InsnList();add.add(new VarInsnNode(ALOAD,batch));add.add(new VarInsnNode(ALOAD,0));add.add(new VarInsnNode(ALOAD,target));add.add(new VarInsnNode(ALOAD,erase));m.instructions.insertBefore(put,add);m.instructions.set(put,call("capturePut","(Ljava/util/Map;Ljava/lang/Object;Ljava/lang/Object;L"+B+";L"+C+";"+SNAP+LEASE+")Ljava/lang/Object;"));
   AbstractInsnNode start=hp;while(!(start instanceof VarInsnNode v&&v.getOpcode()==ASTORE&&v.var==hit&&prior(v).getOpcode()==ACONST_NULL))start=start.getPrevious();start=prior(start);
   add=new InsnList();add.add(new VarInsnNode(ALOAD,batch));add.add(new VarInsnNode(ALOAD,0));add.add(new VarInsnNode(ALOAD,entry));add.add(new VarInsnNode(ALOAD,spell));String spellType=m.name.equals("useClassicWorldLineAttackSpell")?"LineAttackSpell":"AreaAttackSpell";add.add(new MethodInsnNode(INVOKEVIRTUAL,P+"BattleSpellCatalog$"+spellType,"skillId","()I",false));add.add(call("enqueue","(L"+B+";L"+C+";Ljava/util/Map$Entry;I)Z"));add.add(new JumpInsnNode(IFNE,transition.end));m.instructions.insertBefore(start,add);touched.add(m);
  }
  if(touched.size()!=3)throw new IllegalStateException("WORLD multi paths="+touched.size());
  var subset=new ClassNode();subset.version=c.version;subset.access=ACC_PUBLIC;subset.name=c.name;subset.superName=c.superName;subset.methods.addAll(touched);var writer=new ClassWriter(ClassWriter.COMPUTE_FRAMES|ClassWriter.COMPUTE_MAXS){protected ClassLoader getClassLoader(){return PoisonLifecyclePatch.frameLoader;}};subset.accept(writer);var framed=new ClassNode();new ClassReader(writer.toByteArray()).accept(framed,0);for(var fixed:framed.methods)for(int i=0;i<c.methods.size();i++)if(c.methods.get(i).name.equals(fixed.name)&&c.methods.get(i).desc.equals(fixed.desc)){c.methods.set(i,fixed);break;}
  hooks.put("source-WORLD-multi-magic-commit",12);
 }
}
