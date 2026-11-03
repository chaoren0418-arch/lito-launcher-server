import java.util.*;
import org.objectweb.asm.tree.*;
import static org.objectweb.asm.Opcodes.*;

/** Keep the exact native HP/death/reward body and route only its captured WORLD origin. */
final class MonsterDamageCallbackPatch {
 static final String P="local/lito381/server/",C=P+"ClientSession",G=P+"GameHub",W=P+"ClassicWorldMonsterPopulation",H=P+"CombatMonsterCurseLifecycle",S=P+"CombatPoisonLifecycle$Scope",PUB="publishClassicWorldMonsterPoisonUpdate",BODY="sourcePoisonBody$",U="L"+W+"$PoisonUpdate;";
 static MethodInsnNode call(String n,String d){return new MethodInsnNode(INVOKESTATIC,H,n,d,false);}
 static void bridge(MethodNode m,int scope,Map<String,Integer> counts){boolean publisher=m.name.equals(BODY+PUB);var local=new TreeMap<String,Integer>();
  for(var n:m.instructions.toArray()){
   if(n instanceof MethodInsnNode x&&x.owner.equals(C)&&x.name.equals(PUB)){m.instructions.insertBefore(x,new VarInsnNode(ALOAD,scope));m.instructions.set(x,call("publishDamage","(L"+C+";"+U+"JL"+S+";)V"));counts.merge("monsterDamagePublisher",1,Integer::sum);continue;}
   if(!publisher)continue;String name=null,desc=null;
   if(n instanceof MethodInsnNode x){
    if(x.owner.equals(C)&&x.name.equals("classicWorldMembers")){name="damageMembers";desc="(L"+C+";I"+U+"L"+S+";)Ljava/util/List;";}
    else if(x.owner.equals(C)&&x.name.equals("sameClassicWorldMap")){name="damageSameMap";desc="(L"+C+";L"+C+";"+U+"L"+S+";)Z";}
    else if(x.owner.equals(C)&&x.name.equals("sendEncrypted")){name="damageSend";desc="(L"+C+";[BLjava/lang/String;"+U+"L"+S+";)V";}
    else if(x.owner.equals(C)&&x.name.equals("handleClassicWorldWaterDragonDeath")){name="damageBoss";desc="(L"+C+";L"+W+"$Snapshot;Ljava/util/List;"+U+"L"+S+";)V";}
    else if(x.owner.equals(G)&&x.name.equals("scheduleClassicWorldMonsterLifecycle")){name="damageSchedule";desc="(L"+G+";IL"+W+"$Snapshot;"+U+"L"+S+";)V";}
    if(name!=null){m.instructions.insertBefore(x,new VarInsnNode(ALOAD,1));m.instructions.insertBefore(x,new VarInsnNode(ALOAD,scope));m.instructions.set(x,call(name,desc));local.merge(name,1,Integer::sum);}
   }else if(n instanceof TypeInsnNode cast&&cast.getOpcode()==CHECKCAST&&cast.desc.equals(C)&&DragonPatch.prior(cast) instanceof MethodInsnNode optional&&optional.owner.equals("java/util/Optional")&&optional.name.equals("orElse")){
    var add=new InsnList();add.add(new VarInsnNode(ALOAD,1));add.add(new VarInsnNode(ALOAD,scope));add.add(call("damageOwner","(L"+C+";"+U+"L"+S+";)L"+C+";"));m.instructions.insert(cast,add);local.merge("damageOwner",1,Integer::sum);
   }
  }
  if(publisher){var expected=new TreeMap<>(Map.of("damageMembers",1,"damageSameMap",2,"damageSend",4,"damageBoss",1,"damageSchedule",1,"damageOwner",1));if(!local.equals(expected))throw new IllegalStateException("native damage publication hooks "+local);for(var e:local.entrySet())counts.merge(e.getKey(),e.getValue(),Integer::sum);}
 }
}
