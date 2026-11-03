import java.util.*;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
import org.objectweb.asm.tree.analysis.*;
final class PoisonParalysisPatch implements Opcodes {
 static final String P="local/lito381/server/",T=P+"BattleTimedStatus",C=P+"ClientSession",H=P+"CombatPoisonParalysis",F="sourcePoisonParalysis";
 static MethodInsnNode call(String name,String desc){return new MethodInsnNode(INVOKESTATIC,H,name,desc,false);}
 static void timed(ClassNode c,Map<String,Integer> hooks){
  c.fields.add(new FieldNode(ACC_PRIVATE,F,"L"+T+";",null,null));int count=0;
  for(var m:c.methods){if(Set.of("paralysisVisualActive","paralysisActive").contains(m.name)&&m.desc.equals("(J)Z"))for(var n:m.instructions.toArray())if(n.getOpcode()==IRETURN){var add=new InsnList();add.add(new VarInsnNode(ALOAD,0));add.add(new VarInsnNode(LLOAD,1));add.add(call(m.name.equals("paralysisActive")?"otherActive":"otherVisual","(ZL"+T+";J)Z"));m.instructions.insertBefore(n,add);count++;}
   if(m.name.equals("clear")&&m.desc.equals("()V")){var add=new InsnList();add.add(new VarInsnNode(ALOAD,0));add.add(call("discard","(L"+T+";)V"));m.instructions.insert(add);count++;}
  }
  CombatPatch.require(count,3,"independent poison paralysis status state");hooks.put("source-poison-paralysis-state",count);
 }
 static void session(ClassNode c,Map<String,Integer> hooks)throws AnalyzerException{
  var counts=new TreeMap<String,Integer>();
  for(var m:c.methods){m.maxStack+=20;var nodes=m.instructions.toArray();var frames=new Analyzer<>(new SourceInterpreter()).analyze(c.name,m);
   for(int i=0;i<nodes.length;i++)if(nodes[i] instanceof MethodInsnNode n){String name=null,desc=null;
    if(n.owner.equals(P+"CombatNpcPoison")&&n.name.equals("cancelParalysis")&&Set.of("useInventoryItem","useClassicWorldCurseRemovalSpell").contains(m.name)){name="cancelNpc";desc=n.desc;}
    else if(n.owner.equals(T)){
     if(m.name.equals("sourceNpcPoisonState")&&n.name.equals("activateParalysis")){name="activate";desc="(L"+T+";JII)Z";}
     else if(n.name.equals("paralysisVisualActive")&&(m.name.equals("sourceNpcPoisonState")||Set.of("useClassicWorldParalysisSpell","useParalysisSpell").contains(m.name))){name=m.name.equals("sourceNpcPoisonState")?"poisonVisual":"curseVisual";desc="(L"+T+";J)Z";}
     else if(n.name.equals("noteParalysisLockSent")){name="note";desc="(L"+T+";J)Z";}
     else if(n.name.equals("pollParalysis")&&m.name.equals("serverExpireTimedStatuses")){name="poll";desc="(L"+T+";JL"+C+";)L"+T+"$ParalysisPoll;";m.instructions.insertBefore(n,new VarInsnNode(ALOAD,0));}
     else if(n.name.equals("freezeVisualLockActive")&&m.name.equals("currentPoisonPalette")){var add=new InsnList();add.add(new VarInsnNode(ALOAD,0));add.add(new FieldInsnNode(GETFIELD,C,"timedStatus","L"+T+";"));add.add(new VarInsnNode(LLOAD,1));add.add(call("paletteFirst","(ZL"+T+";J)Z"));m.instructions.insert(n,add);counts.merge("paletteFirst",1,Integer::sum);}
    }else if(n.owner.equals(C)&&n.name.equals("sendEncrypted")&&n.desc.equals("([BLjava/lang/String;)V")&&frames[i]!=null){var value=frames[i].getStack(frames[i].getStackSize()-2);if(value.insns.size()==1&&value.insns.iterator().next() instanceof MethodInsnNode factory&&factory.owner.equals(P+"Protocol")&&factory.name.equals("paralysis")&&DragonPatch.prior(factory) instanceof FieldInsnNode mode&&mode.name.equals("PARALYSIS_RELEASE")){name=m.name.equals("serverExpireTimedStatuses")?"releaseAt":"release";desc="(L"+C+";[BLjava/lang/String;"+(name.equals("releaseAt")?"J":"")+")V";if(name.equals("releaseAt"))m.instructions.insertBefore(n,new VarInsnNode(LLOAD,1));}}
    if(name!=null){m.instructions.set(n,call(name,desc));counts.merge(name,1,Integer::sum);}
   }
  }
  System.out.println("source-poison-paralysis-session="+counts);
  CombatPatch.require(counts.getOrDefault("activate",0),1,"NPC poison independent paralysis activation");CombatPatch.require(counts.getOrDefault("poisonVisual",0),1,"NPC independent existing poison gate");CombatPatch.require(counts.getOrDefault("curseVisual",0),2,"ordinary curse independent admission");CombatPatch.require(counts.getOrDefault("poll",0),1,"independent paralysis poll");CombatPatch.require(counts.getOrDefault("note",0),1,"independent paralysis lock marker");CombatPatch.require(counts.getOrDefault("paletteFirst",0),1,"curse palette priority");
  CombatPatch.require(counts.getOrDefault("cancelNpc",0),2,"native antidote/skill9 poison-only cancellation");CombatPatch.require(counts.getOrDefault("release",0),5,"native release publishers");CombatPatch.require(counts.getOrDefault("releaseAt",0),1,"native clock release time");hooks.put("source-poison-paralysis-session",counts.values().stream().mapToInt(Integer::intValue).sum());
 }
}
