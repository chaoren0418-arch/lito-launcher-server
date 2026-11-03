import java.util.*;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
import static org.objectweb.asm.Opcodes.*;

/** Extracts the native physical/doll publishers verbatim; calculation and action remain native. */
final class WorldCommitPatch {
 static final String P="local/lito381/server/",C=P+"ClientSession",G=P+"GameHub",W=P+"ClassicWorldMonsterPopulation",T=P+"BattleTimedStatus",H=P+"CombatWorldCommit",A=H+"$Attempt",U="L"+W+"$PoisonUpdate;",S="L"+P+"CombatPoisonLifecycle$Scope;";
 static final String PHYSICAL="(L"+W+"$Hit;L"+W+"$Snapshot;L"+P+"SourceNpcPhysicalStats;L"+P+"NativeMagicDollEffects$AttackProc;Z"+U+S+")V",DOLL="(L"+W+"$Hit;Ljava/lang/String;J"+U+S+")V";
 static MethodInsnNode call(String name,String desc){return new MethodInsnNode(INVOKESTATIC,H,name,desc,false);}
 static AbstractInsnNode prior(AbstractInsnNode n){return DragonPatch.prior(n);}static AbstractInsnNode next(AbstractInsnNode n){return DragonPatch.next(n);}
 static int slot(MethodNode m,String name){return DragonPatch.local(m,name);}
 static MethodNode suffix(MethodNode source,AbstractInsnNode start,String name,String desc,Map<Integer,Integer> initial){
  var m=new MethodNode(ACC_PRIVATE,name,desc,null,new String[]{"java/io/IOException"});var labels=new HashMap<LabelNode,LabelNode>();for(var n:source.instructions)if(n instanceof LabelNode l)labels.put(l,new LabelNode());
  var map=new HashMap<>(initial);int[] free={Type.getArgumentsAndReturnSizes(desc)>>2};
  for(var n=start;n!=null;n=n.getNext()){if(n instanceof FrameNode||n instanceof LineNumberNode)continue;var copied=n.clone(labels);if(copied instanceof VarInsnNode v){int old=v.var;if(!map.containsKey(old)){map.put(old,free[0]);free[0]+=v.getOpcode()==LLOAD||v.getOpcode()==LSTORE||v.getOpcode()==DLOAD||v.getOpcode()==DSTORE?2:1;}v.var=map.get(old);}if(copied instanceof IincInsnNode i)i.var=map.computeIfAbsent(i.var,k->free[0]++);m.instructions.add(copied);}
  m.maxLocals=free[0];m.maxStack=source.maxStack+16;
  // Parameters retain their original debug-local names for independent inverse slot mapping.
  var begin=new LabelNode();var end=new LabelNode();m.instructions.insert(begin);m.instructions.add(end);m.localVariables=new ArrayList<>();
  for(var v:source.localVariables)if(initial.containsKey(v.index)&&m.localVariables.stream().noneMatch(x->x.index==initial.get(v.index)))m.localVariables.add(new LocalVariableNode(v.name,v.desc,v.signature,begin,end,initial.get(v.index)));
  return m;
 }
 static MethodInsnNode damage(MethodNode m){return Arrays.stream(m.instructions.toArray()).filter(n->n instanceof MethodInsnNode x&&x.owner.equals(G)&&x.name.equals("damageClassicWorldMonster")&&x.desc.equals("(IIJJI)L"+W+"$Hit;")).map(n->(MethodInsnNode)n).reduce((a,b)->b).orElseThrow();}
 static void bridges(MethodNode m,int update,int scope,Map<String,Integer> counts){
  for(var n:m.instructions.toArray())if(n instanceof MethodInsnNode x){String name=null;String owner=x.owner,desc=x.desc;
   if(owner.equals(C))name=switch(x.name){case "classicWorldMembers"->"members";case "sameClassicWorldMap"->"same";case "sendEncrypted"->"send";case "playerName"->"name";case "classicWorldCompanionSnapshots"->"companions";case "ownedCompanion"->"owned";case "handleClassicWorldWaterDragonDeath"->"boss";case "distributeClassicWorldMonsterRewards"->"rewards";case "applyNativeMagicDollClassicWorldAttackProcs"->"dolls";default->null;};
   if(owner.equals(G))name=switch(x.name){case "provokeClassicWorldMonster"->"provoke";case "scheduleClassicWorldMonsterLifecycle"->"schedule";default->null;};
   if(name==null)continue;int split=desc.indexOf(')');m.instructions.insertBefore(x,new VarInsnNode(ALOAD,update));m.instructions.insertBefore(x,new VarInsnNode(ALOAD,scope));m.instructions.set(x,call(name,"(L"+owner+";"+desc.substring(1,split)+U+S+desc.substring(split)));counts.merge(name,1,Integer::sum);
  }
 }
 static void session(ClassNode c,Map<String,Integer> hooks){var methods=new ArrayList<MethodNode>();var counts=new TreeMap<String,Integer>();
  var m=CombatPatch.method(c,"attackClassicWorld","(L"+P+"Protocol$AttackRequest;L"+C+"$AttackEnvelope;)V");int target=slot(m,"target"),stats=slot(m,"targetPhysical"),dolls=slot(m,"magicDollAttackProc"),weapon=slot(m,"weapon"),beforeSleep=slot(m,"sleepActiveBeforeDamage");
  var hp=damage(m);int hit=((VarInsnNode)next(hp)).var;if(!(next(hp) instanceof VarInsnNode store)||store.getOpcode()!=ASTORE||m.localVariables.stream().noneMatch(v->v.index==hit&&v.name.equals("transition")&&v.desc.equals("L"+W+"$Hit;")))throw new IllegalStateException("native WORLD physical HP store");
  var body=suffix(m,next(next(hp)),"sourceWorldPhysicalCommitted",PHYSICAL,Map.of(0,0,hit,1,target,2,stats,3,dolls,4,beforeSleep,5));bridges(body,6,7,counts);c.methods.add(body);methods.add(body);
  int attempt=MagicDamagePatch.appendLocal(c,m,A);int prepared=0,venom=0;String weaponType=Type.getType(m.localVariables.stream().filter(v->v.index==weapon).findFirst().orElseThrow().desc).getInternalName();
  for(var n:m.instructions.toArray())if(n instanceof MethodInsnNode x){
   if(x.owner.equals(P+"CombatRules")&&x.name.equals("strikerAfterClass")){var add=new InsnList();add.add(new VarInsnNode(ALOAD,0));add.add(new VarInsnNode(ALOAD,target));add.add(new VarInsnNode(ALOAD,weapon));add.add(new MethodInsnNode(INVOKEVIRTUAL,weaponType,"weaponType","()I",false));m.instructions.insertBefore(x,add);var replacement=call("prepare","(IZL"+C+";L"+W+"$Snapshot;I)L"+A+";");m.instructions.set(x,replacement);var tail=new InsnList();tail.add(new InsnNode(DUP));tail.add(new VarInsnNode(ASTORE,attempt));tail.add(call("shown","(L"+A+";)I"));m.instructions.insert(replacement,tail);prepared++;
   }else if(x.owner.equals(C)&&x.name.equals("attemptEnchantVenomPoison")){m.instructions.insertBefore(x,new VarInsnNode(ALOAD,attempt));m.instructions.set(x,call("venom","(L"+C+";"+x.desc.substring(1,x.desc.indexOf(')'))+"L"+A+";)V"));venom++;}
  }
  // Insert the deferred branch before the complete original HP/publisher tail; fallback remains exact.
  AbstractInsnNode start;
  // Include the preceding sleep-before query in the retained tail, but enqueue before that query.
  AbstractInsnNode sleepStore=null;for(var n=m.instructions.getFirst();n!=hp;n=n.getNext())if(n instanceof VarInsnNode sleepVar&&sleepVar.getOpcode()==ISTORE&&sleepVar.var==beforeSleep)sleepStore=n;
  if(sleepStore==null)throw new IllegalStateException("physical sleep-before snapshot");start=sleepStore;while(!(start instanceof VarInsnNode v&&v.getOpcode()==ALOAD&&v.var==0&&next(start) instanceof FieldInsnNode f&&f.name.equals("hub")))start=start.getPrevious();
  var retained=new LabelNode();var insert=new InsnList();insert.add(new VarInsnNode(ALOAD,attempt));insert.add(new VarInsnNode(ALOAD,0));insert.add(new VarInsnNode(ALOAD,target));insert.add(new VarInsnNode(ALOAD,stats));insert.add(new VarInsnNode(ALOAD,dolls));insert.add(call("enqueue","(L"+A+";L"+C+";L"+W+"$Snapshot;L"+P+"SourceNpcPhysicalStats;L"+P+"NativeMagicDollEffects$AttackProc;)Z"));insert.add(new JumpInsnNode(IFEQ,retained));insert.add(new InsnNode(RETURN));insert.add(retained);m.instructions.insertBefore(start,insert);methods.add(m);
  if(prepared!=1||venom!=1)throw new IllegalStateException("physical producer hook counts "+prepared+"/"+venom);
  var d=CombatPatch.method(c,"commitNativeMagicDollClassicWorldProcDamage","(JILjava/lang/String;)V");var dhp=damage(d);int dhit=((VarInsnNode)next(dhp)).var;var dollBody=suffix(d,next(next(dhp)),"sourceWorldDollCommitted",DOLL,Map.of(0,0,dhit,1,4,2,1,3));bridges(dollBody,5,6,counts);c.methods.add(dollBody);methods.add(dollBody);
  var head=new InsnList();var original=new LabelNode();head.add(new VarInsnNode(ALOAD,0));head.add(new VarInsnNode(LLOAD,1));head.add(new VarInsnNode(ILOAD,3));head.add(new VarInsnNode(ALOAD,4));head.add(call("enqueueDoll","(L"+C+";JILjava/lang/String;)Z"));head.add(new JumpInsnNode(IFEQ,original));head.add(new InsnNode(RETURN));head.add(original);d.instructions.insert(head);methods.add(d);
  var subset=new ClassNode();subset.version=c.version;subset.access=ACC_PUBLIC;subset.name=c.name;subset.superName=c.superName;subset.methods.addAll(methods);var writer=new ClassWriter(ClassWriter.COMPUTE_FRAMES|ClassWriter.COMPUTE_MAXS){protected ClassLoader getClassLoader(){return PoisonLifecyclePatch.frameLoader;}};subset.accept(writer);var framed=new ClassNode();new ClassReader(writer.toByteArray()).accept(framed,0);for(var fixed:framed.methods)for(int i=0;i<c.methods.size();i++)if(c.methods.get(i).name.equals(fixed.name)&&c.methods.get(i).desc.equals(fixed.desc)){c.methods.set(i,fixed);break;}
  System.out.println("source-WORLD-physical-commit="+counts);hooks.put("source-WORLD-physical-producer",3);hooks.put("source-WORLD-doll-producer",1);hooks.put("source-WORLD-physical-publisher",counts.values().stream().mapToInt(Integer::intValue).sum());
 }
 static void population(ClassNode c,Map<String,Integer> hooks){var m=CombatPatch.method(c,"damage","(IJL"+W+"$Contribution;I)L"+W+"$Hit;");int actor=slot(m,"actor"),input=5,applied=slot(m,"applied");var counts=new TreeMap<String,Integer>();
  for(var n:m.instructions.toArray()){
   if(n instanceof JumpInsnNode j&&j.getOpcode()==IFLE&&prior(j) instanceof VarInsnNode v&&v.getOpcode()==ILOAD&&v.var==input){m.instructions.insertBefore(j,new VarInsnNode(ALOAD,actor));m.instructions.insertBefore(j,call("input","(ILjava/lang/Object;)I"));counts.merge("input",1,Integer::sum);}
   if(n instanceof MethodInsnNode x&&x.owner.equals("java/lang/Math")&&x.name.equals("min")&&x.desc.equals("(II)I")){m.instructions.insertBefore(x,new VarInsnNode(ALOAD,actor));m.instructions.set(x,call("delta","(IILjava/lang/Object;)I"));counts.merge("delta",1,Integer::sum);}
   if(n instanceof MethodInsnNode x&&x.owner.equals(T)&&x.name.equals("cancelSleep")){m.instructions.insertBefore(x,new VarInsnNode(ILOAD,input));m.instructions.set(x,call("sleep","(L"+T+";I)Z"));counts.merge("sleep",1,Integer::sum);}
   if(n instanceof JumpInsnNode j&&j.getOpcode()==IFNULL&&prior(j) instanceof VarInsnNode v&&v.getOpcode()==ALOAD&&v.var==4){m.instructions.insertBefore(j,new VarInsnNode(ILOAD,input));m.instructions.insertBefore(j,call("hate","(L"+W+"$Contribution;I)L"+W+"$Contribution;"));counts.merge("hate",1,Integer::sum);}
   if(n instanceof VarInsnNode v&&v.getOpcode()==ILOAD&&v.var==applied&&next(v) instanceof VarInsnNode died&&died.getOpcode()==ILOAD){m.instructions.insert(v,call("applied","(I)I"));counts.merge("applied",1,Integer::sum);}
  }
  if(!counts.equals(new TreeMap<>(Map.of("input",1,"delta",1,"sleep",1,"hate",1,"applied",1))))throw new IllegalStateException("WORLD signed HP kernel shapes "+counts);hooks.put("source-WORLD-signed-HP",5);
 }
}
