import java.util.*;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
import org.objectweb.asm.tree.analysis.*;
import static org.objectweb.asm.Opcodes.*;

/** Scoped NPC factories and exact native poison recipient-loop boundaries. */
final class MonsterCurseCallbackPatch {
 static final String P="local/lito381/server/",C=P+"ClientSession",G=P+"GameHub",W=P+"ClassicWorldMonsterPopulation",H=P+"CombatMonsterCurseLifecycle",S=P+"CombatPoisonLifecycle$Scope",BODY="sourcePoisonBody$",PUBLISH="publishNpcMagicCancellation";
 static final Set<String> HUB=Set.of("paralyzeClassicWorldMonster","cureClassicWorldMonsterParalysis","cancelClassicWorldMonsterMagic","pollClassicWorldMonsterParalyses","poisonClassicWorldMonsterFromNpc","cureClassicWorldMonsterPoison","poisonClassicWorldMonster","pollClassicWorldMonsterPoisons");
 static AbstractInsnNode before(AbstractInsnNode n){return DragonPatch.prior(n);}static AbstractInsnNode after(AbstractInsnNode n){return DragonPatch.next(n);}
 static MethodInsnNode call(String n,String d){return new MethodInsnNode(INVOKESTATIC,H,n,d,false);}
 static boolean scoped(MethodInsnNode n){return n.owner.equals(G)&&HUB.contains(n.name);}
 static String append(String d){int at=d.indexOf(')');return d.substring(0,at)+"L"+S+";"+d.substring(at);}
 static AbstractInsnNode unique(SourceValue value){if(value==null||value.insns.size()!=1)throw new IllegalStateException("NPC callback operand is not unique");return value.insns.iterator().next();}
 static boolean clockFactory(MethodInsnNode factory,Frame<SourceValue>[] frames,MethodNode m){
  var f=frames[m.instructions.indexOf(factory)];var id=unique(f.getStack(f.getStackSize()-2));if(!(id instanceof MethodInsnNode getter)||!getter.owner.equals(W+"$Snapshot")||!getter.name.equals("objectId"))return false;
  var gf=frames[m.instructions.indexOf(getter)];var receiver=unique(gf.getStack(gf.getStackSize()-1));if(!(receiver instanceof VarInsnNode v)||v.getOpcode()!=ALOAD)return false;
  var definition=unique(frames[m.instructions.indexOf(receiver)].getLocal(v.var));return definition instanceof VarInsnNode stored&&stored.getOpcode()==ASTORE&&before(stored) instanceof MethodInsnNode target&&target.owner.equals(W+"$ParalysisUpdate")&&target.name.equals("target");
 }
 static void bridge(MethodNode m,int scope,Map<String,Integer> counts){
  boolean publisher=m.name.equals(BODY+PUBLISH);boolean relevant=publisher||m.name.equals(BODY+"serverExpireTimedStatuses")||Arrays.stream(m.instructions.toArray()).anyMatch(n->n instanceof MethodInsnNode x&&scoped(x));
  m.maxStack+=32;Frame<SourceValue>[] frames;try{frames=new Analyzer<>(new SourceInterpreter()).analyze(C,m);}catch(AnalyzerException e){throw new IllegalStateException(e);}
  var nodes=m.instructions.toArray();var factories=new IdentityHashMap<MethodInsnNode,Integer>();var sends=new IdentityHashMap<MethodInsnNode,MethodInsnNode>();var loops=new IdentityHashMap<MethodInsnNode,JumpInsnNode>();
  if(relevant)for(var n:nodes)if(n instanceof MethodInsnNode x&&x.owner.equals(P+"Protocol")&&x.name.equals("poison")&&x.desc.equals("(JI)[B")){
   if(m.name.equals(BODY+"serverExpireTimedStatuses")&&!clockFactory(x,frames,m))continue;
   if(m.name.equals(BODY+"attemptEnchantVenomPoison")){var f=frames[m.instructions.indexOf(x)];var id=unique(f.getStack(f.getStackSize()-2));if(!(id instanceof MethodInsnNode getter)||!getter.owner.equals(W+"$Snapshot")||!getter.name.equals("objectId"))continue;}
   int local=after(x) instanceof VarInsnNode store&&store.getOpcode()==ASTORE?store.var:-1;if(local<0&&!publisher)throw new IllegalStateException("NPC poison packet must have original local");factories.put(x,local);
  }
  for(int i=0;i<nodes.length;i++)if(nodes[i] instanceof MethodInsnNode x&&x.owner.equals(C)&&x.name.equals("sendEncrypted")&&frames[i]!=null){
   var f=frames[i];if(f.getStack(f.getStackSize()-2).insns.size()!=1)continue;var value=unique(f.getStack(f.getStackSize()-2));MethodInsnNode factory=null;
   if(value instanceof MethodInsnNode v&&factories.containsKey(v))factory=v;
   if(value instanceof VarInsnNode v&&v.getOpcode()==ALOAD){var definition=frames[m.instructions.indexOf(v)].getLocal(v.var);if(definition.insns.size()==1&&unique(definition) instanceof VarInsnNode stored&&stored.getOpcode()==ASTORE&&before(stored) instanceof MethodInsnNode v2&&factories.containsKey(v2))factory=v2;}
   if(factory==null)continue;sends.put(x,factory);JumpInsnNode exit=null;
   for(var a=x.getPrevious();a!=null;a=a.getPrevious())if(a instanceof MethodInsnNode test&&test.owner.equals("java/util/Iterator")&&test.name.equals("hasNext")&&after(test) instanceof JumpInsnNode jump&&jump.getOpcode()==IFEQ&&m.instructions.indexOf(jump.label)>i){exit=jump;break;}
   if(exit==null)throw new IllegalStateException("native NPC recipient loop exit missing");var old=loops.putIfAbsent(factory,exit);if(old!=null&&old!=exit)throw new IllegalStateException("NPC packet crosses recipient loops");
  }
  for(var entry:factories.entrySet()){
   var factory=entry.getKey();var exit=loops.get(factory);if(exit==null)throw new IllegalStateException("NPC poison factory has no proven native send/loop");
   m.instructions.insertBefore(factory,new VarInsnNode(ALOAD,0));m.instructions.insertBefore(factory,new VarInsnNode(ALOAD,scope));m.instructions.set(factory,call("packet","(JIL"+C+";L"+S+";)[B"));counts.merge("monsterPacket",1,Integer::sum);
   var originalEnd=exit.label;var body=new LabelNode();exit.setOpcode(IFNE);exit.label=body;var extra=new InsnList();
   if(entry.getValue()>=0){extra.add(new VarInsnNode(ALOAD,entry.getValue()));extra.add(new VarInsnNode(ALOAD,scope));extra.add(call("paletteFinished","([BL"+S+";)V"));counts.merge("monsterPaletteFinished",1,Integer::sum);}
   else{extra.add(new VarInsnNode(LLOAD,1));extra.add(new VarInsnNode(ALOAD,scope));extra.add(call("paletteFinishedForId","(JL"+S+";)V"));counts.merge("monsterPaletteFinishedForId",1,Integer::sum);}
   extra.add(new JumpInsnNode(GOTO,originalEnd));extra.add(body);m.instructions.insert(exit,extra);
  }
  for(var x:sends.keySet()){m.instructions.insertBefore(x,new VarInsnNode(ALOAD,scope));m.instructions.set(x,call("send","(L"+C+";[BLjava/lang/String;L"+S+";)V"));counts.merge("monsterSend",1,Integer::sum);}
  for(var n:nodes)if(n instanceof MethodInsnNode x){
   if(scoped(x)){String name=switch(x.name){case "poisonClassicWorldMonster"->"playerDamageWorld";case "pollClassicWorldMonsterPoisons"->"pollDamageWorld";case "paralyzeClassicWorldMonster"->"activateWorld";case "cureClassicWorldMonsterParalysis"->"cureWorld";case "cancelClassicWorldMonsterMagic"->"magicWorld";case "poisonClassicWorldMonsterFromNpc"->"poisonWorld";case "cureClassicWorldMonsterPoison"->"curePoisonWorld";default->"pollWorld";};m.instructions.insertBefore(x,new VarInsnNode(ALOAD,0));m.instructions.insertBefore(x,new VarInsnNode(ALOAD,scope));int at=x.desc.indexOf(')');String d="(L"+G+";"+x.desc.substring(1,at)+"L"+C+";L"+S+";"+x.desc.substring(at);m.instructions.set(x,call(name,d));counts.merge("monster"+name,1,Integer::sum);}
   else if(x.owner.equals(C)&&x.name.equals(PUBLISH)){m.instructions.insertBefore(x,new VarInsnNode(ALOAD,scope));x.name=BODY+PUBLISH;x.desc=append(x.desc);counts.merge("monsterPublisherScope",1,Integer::sum);}
  }
 }
}
