import java.util.*;
import org.objectweb.asm.tree.*;
import org.objectweb.asm.tree.analysis.*;
import static org.objectweb.asm.Opcodes.*;

/** Independently restores the complete original HP/death/reward publisher without removing loops. */
final class VerifyMonsterDamageCallback {
 static final String P="local/lito381/server/",C=P+"ClientSession",G=P+"GameHub",W=P+"ClassicWorldMonsterPopulation",H=P+"CombatMonsterCurseLifecycle",S=P+"CombatPoisonLifecycle$Scope",PUB="publishClassicWorldMonsterPoisonUpdate",BODY="sourcePoisonBody$",U="L"+W+"$PoisonUpdate;";
 static final Set<String> NAMES=Set.of("publishDamage","damageMembers","damageSameMap","damageSend","damageOwner","damageBoss","damageSchedule");
 static void need(boolean b,String why){if(!b)throw new AssertionError("WORLD damage callback "+why);}
 static AbstractInsnNode before(AbstractInsnNode n){return VerifyCombatPreservation.prior(n);}static AbstractInsnNode after(AbstractInsnNode n){return VerifyCombatPreservation.following(n);}
 static void load(AbstractInsnNode n,int op,int slot){need(n instanceof VarInsnNode v&&v.getOpcode()==op&&v.var==slot,"original value or appended scope local");}
 static void raw(ClassNode c){var counts=new TreeMap<String,Integer>();for(var m:c.methods)for(var n:m.instructions)if(n instanceof MethodInsnNode x&&x.owner.equals(H)&&NAMES.contains(x.name))counts.merge(x.name,1,Integer::sum);need(counts.equals(new TreeMap<>(Map.of("publishDamage",1,"damageMembers",1,"damageSameMap",2,"damageSend",4,"damageOwner",1,"damageBoss",1,"damageSchedule",1))),"exact native publisher boundaries "+counts);}
 static MethodNode normalize(MethodNode m,int scope){m.maxStack+=16;
  for(var n:m.instructions.toArray())if(n instanceof MethodInsnNode x&&x.owner.equals(H)&&NAMES.contains(x.name)){
   need(scope>=0&&x.getOpcode()==INVOKESTATIC,"method-owned typed scope");load(before(x),ALOAD,scope);m.instructions.remove(before(x));
   if(x.name.equals("publishDamage")){need(m.name.equals(BODY+"serverExpireTimedStatuses")&&x.desc.equals("(L"+C+";"+U+"JL"+S+";)V"),"only original WORLD clock publisher call");m.instructions.set(x,new MethodInsnNode(INVOKEVIRTUAL,C,PUB,"("+U+"J)V",false));continue;}
   need(m.name.equals(BODY+PUB),"only original native damage publisher body");load(before(x),ALOAD,1);m.instructions.remove(before(x));String owner=C,name,desc;
   switch(x.name){
    case "damageMembers"->{name="classicWorldMembers";desc="(I)Ljava/util/List;";}
    case "damageSameMap"->{name="sameClassicWorldMap";desc="(L"+C+";)Z";}
    case "damageSend"->{name="sendEncrypted";desc="([BLjava/lang/String;)V";}
    case "damageBoss"->{name="handleClassicWorldWaterDragonDeath";desc="(L"+W+"$Snapshot;Ljava/util/List;)V";}
    case "damageSchedule"->{owner=G;name="scheduleClassicWorldMonsterLifecycle";desc="(IL"+W+"$Snapshot;)V";}
    case "damageOwner"->{need(x.desc.equals("(L"+C+";"+U+"L"+S+";)L"+C+";"),"captured contributor owner result");var cast=before(x);need(cast instanceof TypeInsnNode t&&t.getOpcode()==CHECKCAST&&t.desc.equals(C)&&before(cast) instanceof MethodInsnNode optional&&optional.owner.equals("java/util/Optional")&&optional.name.equals("orElse")&&optional.desc.equals("(Ljava/lang/Object;)Ljava/lang/Object;"),"actual original native owner search/cast");need(after(x) instanceof VarInsnNode v&&v.getOpcode()==ASTORE,"original owner stored without alternate locals");m.instructions.remove(x);continue;}
    default->throw new AssertionError(x.name);
   }
   int end=desc.indexOf(')');need(x.desc.equals("(L"+owner+";"+desc.substring(1,end)+U+"L"+S+";"+desc.substring(end)),"original operand/result descriptors retained");
   m.instructions.set(x,new MethodInsnNode(INVOKEVIRTUAL,owner,name,desc,false));
  }return m;
 }
}
