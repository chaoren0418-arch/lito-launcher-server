import java.util.*;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
import org.objectweb.asm.tree.analysis.*;
import static org.objectweb.asm.Opcodes.*;

/** Inverse proof uses original actor fields and poll-result definitions. */
final class VerifyMonsterParalysis {
 static final String P="local/lito381/server/",W=P+"ClassicWorldMonsterPopulation",T=P+"BattleTimedStatus",H=P+"CombatMonsterParalysis",Q=H+"$Poll",N=T+"$ParalysisPoll";
 static void need(boolean b,String why){if(!b)throw new AssertionError("WORLD monster independent paralysis "+why);}
 static AbstractInsnNode before(AbstractInsnNode n){return VerifyCombatPreservation.prior(n);}static AbstractInsnNode after(AbstractInsnNode n){return VerifyCombatPreservation.following(n);}
 static int load(AbstractInsnNode n,int op){need(n instanceof VarInsnNode v&&v.getOpcode()==op,"original operand load");return ((VarInsnNode)n).var;}
 static void raw(ClassNode c){var counts=new TreeMap<String,Integer>();for(var m:c.methods)for(var n:m.instructions)if(n instanceof MethodInsnNode x){if(x.owner.equals(H))counts.merge(x.name,1,Integer::sum);if(x.owner.equals(Q))counts.merge("pollAccessors",1,Integer::sum);}need(counts.equals(new TreeMap<>(Map.ofEntries(Map.entry("cancelMagic",1),Map.entry("resetMagic",5),Map.entry("activate",1),Map.entry("curseVisual",1),Map.entry("poisonVisual",1),Map.entry("cancelCurse",1),Map.entry("poll",1),Map.entry("paletteFirst",1),Map.entry("legacyPoisonCancellation",1),Map.entry("pollAccessors",5),Map.entry("poisonEvent",1)))),"exact nineteen typed hooks "+counts);}
 static MethodNode normalize(MethodNode original){
  original=VerifyWorldCommit.kernel(original);
  original=VerifyMonsterSilence.normalize(VerifyMonsterDamage.normalize(VerifyMonsterPoisonParalysis.normalize(original)));
  var m=new MethodNode(original.access,original.name,original.desc,original.signature,original.exceptions.toArray(String[]::new));original.accept(m);m.maxStack+=24;Frame<SourceValue>[] frames;
  try{frames=new Analyzer<>(new SourceInterpreter()).analyze(W,m);}catch(AnalyzerException e){throw new AssertionError(e);}int poisonExpired=0;
  for(var n:m.instructions.toArray())if(n instanceof MethodInsnNode x){
   if(x.owner.equals(Q)){need(m.name.equals("pollParalyses")&&x.desc.equals("()Z")&&Set.of("activated","expired","poisonExpired").contains(x.name),"only original poll accessors");if(x.name.equals("poisonExpired")){poisonExpired++;x.name="expired";}x.owner=N;continue;}
   if(!x.owner.equals(H))continue;
   if(x.name.equals("paletteFirst")){
    need(m.name.equals("poisonPalette")&&x.desc.equals("(ZL"+T+";J)Z"),"only native freeze palette gate");var time=before(x);var field=before(time);var actor=before(field);var nativeCall=before(actor);
    need(field instanceof FieldInsnNode f&&f.getOpcode()==GETFIELD&&f.owner.equals(W+"$Actor")&&f.name.equals("damagePoisonStatus")&&f.desc.equals("L"+T+";"),"same native status field");int a=load(actor,ALOAD),clock=load(time,LLOAD);
    need(nativeCall instanceof MethodInsnNode f&&f.getOpcode()==INVOKEVIRTUAL&&f.owner.equals(T)&&f.name.equals("freezeVisualLockActive")&&f.desc.equals("(J)Z")&&load(before(f),LLOAD)==clock,"original freeze decision and clock retained");var oldField=before(before(nativeCall));need(oldField instanceof FieldInsnNode f&&f.getOpcode()==GETFIELD&&f.owner.equals(W+"$Actor")&&f.name.equals("damagePoisonStatus")&&load(before(f),ALOAD)==a,"original same actor");
    for(int j=0;j<3;j++)m.instructions.remove(before(x));m.instructions.remove(x);continue;
   }
   if(x.name.equals("poisonEvent")){
    need(m.name.equals("pollParalyses")&&x.desc.equals("(ZL"+Q+";)Z")&&after(x) instanceof MethodInsnNode ctor&&ctor.getOpcode()==INVOKESPECIAL&&ctor.owner.equals(W+"$ParalysisUpdate")&&ctor.name.equals("<init>")&&ctor.desc.equals("(L"+W+"$Snapshot;ZZZ)V"),"original outgoing update flag only");
    var read=before(x);int result=load(read,ALOAD);load(before(read),ILOAD);var frame=frames[m.instructions.indexOf(read)];var definition=frame.getLocal(result);need(definition.insns.size()==1,"unique poll local definition");var stored=definition.insns.iterator().next();need(stored instanceof VarInsnNode v&&v.getOpcode()==ASTORE&&v.var==result&&before(stored) instanceof MethodInsnNode factory&&factory.owner.equals(T)&&factory.name.equals("pollParalysis"),"actual native poll result dominates outgoing event");
    m.instructions.remove(read);m.instructions.remove(x);continue;
   }
   if(x.name.equals("legacyPoisonCancellation")){need(m.name.equals("cureParalysis")&&x.desc.equals("(L"+T+"$ParalysisCancellation;)Z"),"native obsolete shared poison provenance guard");m.instructions.set(x,new MethodInsnNode(INVOKEVIRTUAL,T+"$ParalysisCancellation","present","()Z",false));continue;}
   String method,desc;int depth;
   switch(x.name){
    case "cancelMagic","resetMagic"->{need(x.desc.equals("(L"+T+";)L"+T+"$MagicCancellation;"),"native cancellation/death/capture/removal lease retirement");method="cancelMagicEffects";desc="()L"+T+"$MagicCancellation;";depth=1;}
    case "activate"->{need(m.name.equals("paralysis")&&x.desc.equals("(L"+T+";JII)Z"),"original curse activation");method="activateParalysis";desc="(JII)Z";depth=4;}
    case "curseVisual"->{need(m.name.equals("paralysisActive")&&x.desc.equals("(L"+T+";J)Z"),"ordinary curse-only admission");method="paralysisVisualActive";desc="(J)Z";depth=2;}
    case "poisonVisual"->{need(m.name.equals("poisonLeaseActive")&&x.desc.equals("(L"+T+";J)Z"),"poison-only existing status gate");method="paralysisVisualActive";desc="(J)Z";depth=2;}
    case "cancelCurse"->{need(m.name.equals("cureParalysis")&&x.desc.equals("(L"+T+";)L"+T+"$ParalysisCancellation;"),"ordinary curse-only cure");method="cancelParalysis";desc="()L"+T+"$ParalysisCancellation;";depth=1;}
    case "poll"->{need(m.name.equals("pollParalyses")&&x.desc.equals("(L"+T+";J)L"+Q+";"),"original actor clock");method="pollParalysis";desc="(J)L"+N+";";depth=2;}
    default->throw new AssertionError("unknown monster paralysis bridge "+x.name);
   }
   var frame=frames[m.instructions.indexOf(x)];var receiver=frame.getStack(frame.getStackSize()-depth);need(receiver.insns.size()==1,"unique native timed status receiver");var receiverNode=receiver.insns.iterator().next();need(receiverNode instanceof FieldInsnNode f&&f.getOpcode()==GETFIELD&&f.owner.equals(W+"$Actor")&&f.name.equals("damagePoisonStatus")&&f.desc.equals("L"+T+";"),"unchanged actual native actor status operand");
   m.instructions.set(x,new MethodInsnNode(INVOKEVIRTUAL,T,method,desc,false));
  }
  if(m.name.equals("pollParalyses"))need(poisonExpired==1,"exact source-only poison expiry guard");
  if(m.localVariables!=null)for(var v:m.localVariables)if(v.desc.equals("L"+Q+";"))v.desc="L"+N+";";
  for(var n:m.instructions)if(n instanceof FrameNode f){if(f.local!=null)for(int i=0;i<f.local.size();i++)if(Q.equals(f.local.get(i)))f.local.set(i,N);if(f.stack!=null)for(int i=0;i<f.stack.size();i++)if(Q.equals(f.stack.get(i)))f.stack.set(i,N);}
  return m;
 }
}
