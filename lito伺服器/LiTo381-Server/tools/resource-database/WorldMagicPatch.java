import java.util.*;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
import static org.objectweb.asm.Opcodes.*;

/** One ordinary WORLD magic branch; exact original branch remains as fallback. */
final class WorldMagicPatch {
 static final String P="local/lito381/server/",C=P+"ClientSession",W=P+"ClassicWorldMonsterPopulation",H=P+"CombatWorldMagic",A=H+"$Attempt",SPELL="L"+P+"BattleSpellCatalog$AttackSpell;",U="L"+W+"$PoisonUpdate;",S="L"+P+"CombatPoisonLifecycle$Scope;";
 static int slot(MethodNode m,String name){return DragonPatch.local(m,name);}
 static MethodInsnNode call(String n,String d){return new MethodInsnNode(INVOKESTATIC,H,n,d,false);}
 static AbstractInsnNode prior(AbstractInsnNode n){return DragonPatch.prior(n);}static AbstractInsnNode next(AbstractInsnNode n){return DragonPatch.next(n);}
 static void apply(ClassNode c,Map<String,Integer> hooks){
  var m=CombatPatch.method(c,"useClassicWorldAttackSpell","(L"+P+"Protocol$AttackSpellRequest;"+SPELL+")V");int ordinary=slot(m,"validOrdinaryMonster"),target=slot(m,"monsterTarget"),spell=slot(m,"spell"),thunder=slot(m,"thunderGrab"),stats=slot(m,"npcStats"),level=slot(m,"casterLevel"),damage=slot(m,"damage"),erase=slot(m,"eraseMagicLease"),mr=slot(m,"targetMagicResistance");int attempt=MagicDamagePatch.appendLocal(c,m,A);int critical=0,prepared=0;
  for(var n:m.instructions.toArray())if(n instanceof MethodInsnNode x){
   if(x.owner.equals(P+"CombatCompanionDamage")&&x.name.equals("beforeCritical")){m.instructions.insertBefore(x,new VarInsnNode(ILOAD,ordinary));m.instructions.insertBefore(x,new VarInsnNode(ILOAD,mr));m.instructions.insertBefore(x,new VarInsnNode(ALOAD,0));m.instructions.insertBefore(x,new VarInsnNode(ALOAD,target));m.instructions.set(x,call("critical",x.desc.substring(0,x.desc.indexOf(')'))+"ZIL"+C+";L"+W+"$Snapshot;)I"));critical++;}
   if(x.owner.equals(C)&&x.name.equals("publishClassicWorldAttackSpellPacket")&&critical==1){var add=new InsnList();add.add(new VarInsnNode(ALOAD,0));add.add(new VarInsnNode(ILOAD,ordinary));add.add(new VarInsnNode(ALOAD,target));add.add(new VarInsnNode(ALOAD,thunder));add.add(new VarInsnNode(ALOAD,stats));add.add(new VarInsnNode(ILOAD,level));add.add(call("prepare","(L"+C+";ZL"+W+"$Snapshot;L"+P+"BattleSpellCatalog$ThunderGrabSpell;L"+P+"SourceNpcPhysicalStats;I)L"+A+";"));add.add(new VarInsnNode(ASTORE,attempt));m.instructions.insertBefore(x,add);prepared++;}
  }
  if(critical!=1||prepared!=1)throw new IllegalStateException("WORLD magic scalar/action count "+critical+"/"+prepared);
  var hp=Arrays.stream(m.instructions.toArray()).filter(n->n instanceof MethodInsnNode x&&x.owner.equals(P+"GameHub")&&x.name.equals("damageClassicWorldMonster")).findFirst().orElseThrow();var store=next(hp);if(!(store instanceof VarInsnNode hpStore&&hpStore.getOpcode()==ASTORE))throw new IllegalStateException("native magic HP store");var local=m.localVariables.stream().filter(v->v.index==((VarInsnNode)store).var&&v.name.equals("transition")&&v.desc.equals("L"+W+"$Hit;")).findFirst().orElseThrow();AbstractInsnNode start=hp;
  while(!(start instanceof JumpInsnNode j&&j.getOpcode()==IFGT&&prior(j) instanceof VarInsnNode v&&v.getOpcode()==ILOAD&&v.var==damage))start=start.getPrevious();start=prior(start);
  var add=new InsnList();add.add(new VarInsnNode(ALOAD,attempt));add.add(new VarInsnNode(ALOAD,0));add.add(new VarInsnNode(ILOAD,damage));add.add(new VarInsnNode(ALOAD,spell));add.add(new VarInsnNode(ALOAD,erase));add.add(call("enqueue","(L"+A+";L"+C+";I"+SPELL+"L"+P+"BattleEraseMagicStatus$Lease;)Z"));add.add(new JumpInsnNode(IFNE,local.end));m.instructions.insertBefore(start,add);
  var original=CombatPatch.method(c,"publishClassicWorldSpellMonsterDamage","(IL"+W+"$Hit;)V");var publisher=WorldCommitPatch.suffix(original,original.instructions.getFirst(),"sourceWorldMagicPublished","(IL"+W+"$Hit;"+U+S+")V",Map.of(0,0,1,1,2,2));var bridges=new TreeMap<String,Integer>();WorldCommitPatch.bridges(publisher,3,4,bridges);c.methods.add(publisher);
  var subset=new ClassNode();subset.version=c.version;subset.access=ACC_PUBLIC;subset.name=c.name;subset.superName=c.superName;subset.methods.add(m);subset.methods.add(publisher);var writer=new ClassWriter(ClassWriter.COMPUTE_FRAMES|ClassWriter.COMPUTE_MAXS){protected ClassLoader getClassLoader(){return PoisonLifecyclePatch.frameLoader;}};subset.accept(writer);var framed=new ClassNode();new ClassReader(writer.toByteArray()).accept(framed,0);for(var fixed:framed.methods)for(int i=0;i<c.methods.size();i++)if(c.methods.get(i).name.equals(fixed.name)&&c.methods.get(i).desc.equals(fixed.desc)){c.methods.set(i,fixed);break;}
  hooks.put("source-WORLD-magic-commit",3);hooks.put("source-WORLD-magic-native-publisher",bridges.values().stream().mapToInt(Integer::intValue).sum());System.out.println("source-WORLD-magic-commit="+bridges);
 }
}
