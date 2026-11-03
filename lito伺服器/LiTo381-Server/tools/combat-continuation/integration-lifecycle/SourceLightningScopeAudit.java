import java.util.*;import java.util.jar.*;import org.objectweb.asm.tree.*;import static org.objectweb.asm.Opcodes.*;
/** Independent output wrapper, bounded cast storage and nonblocking native frame audit. */
public final class SourceLightningScopeAudit {
 static final String P="local/lito381/server/",N=P+"NativeSourceLightningLifetime",AREA=P+"NativeAreaMaterialAction",PC=P+"ClientSession";
 static MethodNode method(ClassNode c,String name){return c.methods.stream().filter(m->m.name.equals(name)).findFirst().orElseThrow();}
 static void need(boolean b,String s){if(!b)throw new AssertionError(s);}
 public static void main(String[]a)throws Exception{
  try(var jar=new JarFile(a[0])){
   var area=FreezeSourceTargetDelta.read(jar,AREA);var wrapper=method(area,"nativeSourceWeaponBuffOriginalCaptureOutput");
   var real=Arrays.stream(wrapper.instructions.toArray()).filter(n->n.getOpcode()>=0).toList();
   int[] op={INVOKESTATIC,IFEQ,ALOAD,ALOAD,ALOAD,INVOKESTATIC,IRETURN,ALOAD,ALOAD,ALOAD,INVOKESTATIC,IRETURN};
   need(real.size()==op.length,"exact output wrapper length");for(int i=0;i<op.length;i++)need(real.get(i).getOpcode()==op[i],"output wrapper opcode="+i);
   for(int i:new int[]{2,7})for(int j=0;j<3;j++)need(((VarInsnNode)real.get(i+j)).var==j,"same original wrapper arguments");
   var active=(MethodInsnNode)real.get(0);var capture=(MethodInsnNode)real.get(5);var prior=(MethodInsnNode)real.get(10);
   need(active.owner.equals(N)&&active.name.equals("active")&&active.desc.equals("()Z"),"bounded cast authority first");
   need(capture.owner.equals(N)&&capture.name.equals("captureOutput")&&capture.desc.equals(wrapper.desc),"single bounded output port");
   need(prior.owner.equals(AREA)&&prior.name.equals("nativeSourceLightningOriginalCaptureOutput")&&prior.desc.equals(wrapper.desc),"exact retained capture fallback");
   need(FreezeSourceTargetDelta.next(((JumpInsnNode)real.get(1)).label)==real.get(7),"fallback branch retains original output path");
   var helper=FreezeSourceTargetDelta.read(jar,N);int capturedRuns=0,deferred=0,finishes=0;
   for(var m:helper.methods)for(var n:m.instructions){
    if(n instanceof MethodInsnNode call){
     if(call.owner.equals(AREA)&&call.name.equals("nativeCompanionAreaCapturedRun"))capturedRuns++;
     if(call.owner.equals(P+"NativeLightAction")&&call.name.equals("deferScoped"))deferred++;
     if(call.owner.equals(P+"NativeLightAction")&&call.name.equals("finishScoped"))finishes++;
    }
   }
   need(capturedRuns==1&&deferred==1&&finishes==1,"one existing lock boundary, output queue and completion owner");
   var frame=method(helper,"allowFrame");
   for(var n:frame.instructions){
    need(n.getOpcode()!=MONITORENTER&&n.getOpcode()!=MONITOREXIT,"no final frame monitor acquisition");
    if(n instanceof MethodInsnNode call){
     need(!Set.of("lock","tryLock","snapshot","matches","scene","npc","player","of","ownedCompanion","canAttackTarget").contains(call.name),"no blocking final frame lookup: "+call.owner+"."+call.name);
     need(!call.owner.equals(P+"GameHub")&&!call.owner.equals(P+"BattleTimedStatus"),"no Hub or timed-status call under the native monitor");
    }
   }
   var aim=FreezeSourceTargetDelta.read(jar,N+"$Aim");var close=method(aim,"close");
   int clear=0;for(var n:close.instructions)if(n instanceof FieldInsnNode f&&f.getOpcode()==PUTFIELD&&f.owner.equals(AREA+"$Context")&&f.name.equals("nativeSourceLightningAim")){need(FreezeAcceptedMethods.prev(f).getOpcode()==ACONST_NULL,"completion clears cast plan");clear++;}
   need(clear==1,"exactly one bounded context clear");
  }
  System.out.println("PASS independent output fallback, one original area/queue/completion, nonblocking final frame and bounded context clear");
 }
}
