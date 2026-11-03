import java.util.*;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
import org.objectweb.asm.tree.analysis.*;

/** Typed adapters only; the native WORLD actor loops and admission remain intact. */
final class MonsterParalysisPatch implements Opcodes {
 static final String P="local/lito381/server/",W=P+"ClassicWorldMonsterPopulation",T=P+"BattleTimedStatus",H=P+"CombatMonsterParalysis",Q=H+"$Poll",N=T+"$ParalysisPoll";
 static AbstractInsnNode before(AbstractInsnNode n){return DragonPatch.prior(n);}
 static MethodInsnNode call(String n,String d){return new MethodInsnNode(INVOKESTATIC,H,n,d,false);}
 static void apply(ClassNode c,Map<String,Integer> hooks)throws AnalyzerException{
  var counts=new TreeMap<String,Integer>();
  for(var m:c.methods){m.maxStack+=16;int expired=0,pollLocal=-1;
   for(var n:m.instructions.toArray())if(n instanceof MethodInsnNode x){String name=null,desc=null;
    if(x.owner.equals(T)){
     if(x.name.equals("cancelMagicEffects")){name=m.name.equals("cancelMagicEffects")?"cancelMagic":"resetMagic";desc="(L"+T+";)L"+T+"$MagicCancellation;";}
     if(m.name.equals("paralysis")&&x.name.equals("activateParalysis")){name="activate";desc="(L"+T+";JII)Z";}
     if(m.name.equals("paralysisActive")&&x.name.equals("paralysisVisualActive")){name="curseVisual";desc="(L"+T+";J)Z";}
     if(m.name.equals("poisonLeaseActive")&&x.name.equals("paralysisVisualActive")){name="poisonVisual";desc="(L"+T+";J)Z";}
     if(m.name.equals("cureParalysis")&&x.name.equals("cancelParalysis")){name="cancelCurse";desc="(L"+T+";)L"+T+"$ParalysisCancellation;";}
     if(m.name.equals("pollParalyses")&&x.name.equals("pollParalysis")){name="poll";desc="(L"+T+";J)L"+Q+";";if(!(DragonPatch.next(x) instanceof VarInsnNode v)||v.getOpcode()!=ASTORE)throw new IllegalStateException("native monster poll result");pollLocal=((VarInsnNode)DragonPatch.next(x)).var;}
     if(m.name.equals("poisonPalette")&&x.name.equals("freezeVisualLockActive")){
      var time=before(x);var field=before(time);var actor=before(field);
      if(!(time instanceof VarInsnNode v)||v.getOpcode()!=LLOAD||!(field instanceof FieldInsnNode f)||f.getOpcode()!=GETFIELD||!f.owner.equals(W+"$Actor")||!f.name.equals("damagePoisonStatus")||!(actor instanceof VarInsnNode a)||a.getOpcode()!=ALOAD)throw new IllegalStateException("native monster palette status/time");
      var add=new InsnList();add.add(new VarInsnNode(ALOAD,((VarInsnNode)actor).var));add.add(new FieldInsnNode(GETFIELD,W+"$Actor","damagePoisonStatus","L"+T+";"));add.add(new VarInsnNode(LLOAD,((VarInsnNode)time).var));add.add(call("paletteFirst","(ZL"+T+";J)Z"));m.instructions.insert(x,add);counts.merge("paletteFirst",1,Integer::sum);
     }
    }
    if(m.name.equals("cureParalysis")&&x.owner.equals(T+"$ParalysisCancellation")&&x.name.equals("present")){name="legacyPoisonCancellation";desc="(L"+T+"$ParalysisCancellation;)Z";}
    if(m.name.equals("pollParalyses")&&x.owner.equals(N)){
     if(!Set.of("activated","expired").contains(x.name)||!x.desc.equals("()Z"))throw new IllegalStateException("native monster poll accessor");
     if(x.name.equals("expired")&&expired++==0)x.name="poisonExpired";
     x.owner=Q;counts.merge("pollAccessors",1,Integer::sum);
    }
    if(m.name.equals("pollParalyses")&&x.owner.equals(W+"$ParalysisUpdate")&&x.name.equals("<init>")){
     if(pollLocal<0||!x.desc.equals("(L"+W+"$Snapshot;ZZZ)V")||!(before(x) instanceof VarInsnNode v)||v.getOpcode()!=ILOAD)throw new IllegalStateException("native monster paralysis update");
     m.instructions.insertBefore(x,new VarInsnNode(ALOAD,pollLocal));m.instructions.insertBefore(x,call("poisonEvent","(ZL"+Q+";)Z"));counts.merge("poisonEvent",1,Integer::sum);
    }
    if(name!=null){m.instructions.set(x,call(name,desc));counts.merge(name,1,Integer::sum);}
   }
   if(m.name.equals("pollParalyses")){
    if(pollLocal<0||expired!=3)throw new IllegalStateException("native monster poll/expiry shape");
    if(m.localVariables!=null)for(var v:m.localVariables)if(v.desc.equals("L"+N+";"))v.desc="L"+Q+";";
    for(var n:m.instructions)if(n instanceof FrameNode f){if(f.local!=null)for(int i=0;i<f.local.size();i++)if(N.equals(f.local.get(i)))f.local.set(i,Q);if(f.stack!=null)for(int i=0;i<f.stack.size();i++)if(N.equals(f.stack.get(i)))f.stack.set(i,Q);}
   }
  }
  var expected=new TreeMap<>(Map.ofEntries(Map.entry("cancelMagic",1),Map.entry("resetMagic",5),Map.entry("activate",1),Map.entry("curseVisual",1),Map.entry("poisonVisual",1),Map.entry("cancelCurse",1),Map.entry("poll",1),Map.entry("paletteFirst",1),Map.entry("legacyPoisonCancellation",1),Map.entry("pollAccessors",5),Map.entry("poisonEvent",1)));
  if(!counts.equals(expected))throw new IllegalStateException("native monster curse hooks "+counts);
  System.out.println("source-monster-independent-paralysis="+counts);hooks.put("source-monster-independent-paralysis",counts.values().stream().mapToInt(Integer::intValue).sum());
 }
}
