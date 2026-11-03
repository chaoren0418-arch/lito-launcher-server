import java.util.*;
import org.objectweb.asm.tree.*;
import static org.objectweb.asm.Opcodes.*;

/** Independent inverse validates exact original target/action/HUD adjacency and restores all native opcodes. */
final class VerifyWorldFoe {
 static final String P="local/lito381/server/",C=P+"ClientSession",H=P+"CombatWorldFoe",A=H+"$Pending";
 static void need(boolean yes,String why){if(!yes)throw new AssertionError("WORLD Foe "+why);}
 static AbstractInsnNode before(AbstractInsnNode n){return VerifyCombatPreservation.prior(n);}static AbstractInsnNode after(AbstractInsnNode n){return VerifyCombatPreservation.following(n);}
 static void load(AbstractInsnNode n,int op,int slot){need(n instanceof VarInsnNode v&&v.getOpcode()==op&&v.var==slot,"original operand "+slot);}
 static ClassNode normalize(ClassNode original){var c=new ClassNode();original.accept(c);if(!c.name.equals(C))return c;int restored=0;var inventory=new TreeMap<String,Map<String,Integer>>();
  for(var m:c.methods){int temporary=-1,captureCount=0,finishCount=0;
   for(var n:m.instructions.toArray())if(n instanceof MethodInsnNode call&&call.owner.equals(H)){
    inventory.computeIfAbsent(m.name,k->new TreeMap<>()).merge(call.name,1,Integer::sum);restored++;
    if(call.name.equals("capture")){
     need(m.name.equals("useFoeSlayerSpell")&&call.desc.equals("(L"+C+";Ljava/lang/Object;)L"+A+";"),"only original Foe target capture");int target=VerifyLineSpells.slot(m,"target");var targetLoad=before(call);var caster=before(targetLoad);load(targetLoad,ALOAD,target);load(caster,ALOAD,0);var originalStore=before(caster);load(originalStore,ASTORE,target);var resolver=before(originalStore);need(resolver instanceof MethodInsnNode x&&x.owner.equals(C)&&x.name.equals("resolveFoeSlayerTarget")&&x.desc.endsWith("L"+C+"$FoeSlayerTarget;"),"capture directly after original exact target resolution");var stored=after(call);need(stored instanceof VarInsnNode v&&v.getOpcode()==ASTORE,"only caller-local Pending");temporary=((VarInsnNode)stored).var;var retained=after(stored);load(retained,ALOAD,target);need(after(retained) instanceof JumpInsnNode j&&j.getOpcode()==IFNONNULL,"original null-target guard remains before original costs and actions");
     m.instructions.remove(targetLoad);m.instructions.remove(caster);m.instructions.remove(stored);m.instructions.remove(call);captureCount++;
    }else if(call.name.equals("finish")){
     need(m.name.equals("useFoeSlayerSpell")&&temporary>=0&&call.desc.equals("(L"+C+";Ljava/lang/Object;ZL"+A+";)V"),"typed original mode finish with one Pending");var pending=before(call);load(pending,ALOAD,temporary);var accepted=before(pending);load(accepted,ILOAD,VerifyLineSpells.slot(m,"targetStillEligible"));var target=before(accepted);load(target,ALOAD,VerifyLineSpells.slot(m,"target"));load(before(target),ALOAD,0);var hud=after(call);load(hud,ALOAD,0);need(after(hud) instanceof FieldInsnNode f&&f.getOpcode()==GETFIELD&&f.owner.equals(C)&&f.name.equals("playerHp")&&f.desc.equals("I"),"original finish remains at native current-HP HUD boundary");m.instructions.remove(pending);m.instructions.set(call,new MethodInsnNode(INVOKESTATIC,P+"CombatDragon","finishFoe","(L"+C+";Ljava/lang/Object;Z)V",false));finishCount++;
    }else throw new AssertionError("undeclared Foe bridge "+call.name);
   }
   if(temporary>=0){need(captureCount==1&&finishCount==1,"one paired target capture and finish");int init=0;for(var n:m.instructions.toArray())if(n instanceof VarInsnNode v&&v.var==temporary){need(v.getOpcode()==ASTORE&&before(v).getOpcode()==ACONST_NULL,"only Pending null initializer remains");var nullValue=before(v);need(before(nullValue)==null,"temporary initialization is the sole method-entry pair");m.instructions.remove(nullValue);m.instructions.remove(v);init++;}need(init==1,"one Pending initializer");final int slot=temporary;need(Arrays.stream(m.instructions.toArray()).noneMatch(n->n instanceof VarInsnNode v&&v.var==slot),"Pending never escapes caller scope");}
  }
  need(restored==2&&inventory.equals(new TreeMap<>(Map.of("useFoeSlayerSpell",new TreeMap<>(Map.of("capture",1,"finish",1))))),"exact two bridge inventory");return c;
 }
}
