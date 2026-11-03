import java.util.*;
import org.objectweb.asm.*;import org.objectweb.asm.tree.*;
/** Narrow change over the hash-pinned release; avoids unrelated modern source edits. */
final class EquipmentPatch implements Opcodes {
 static final String P="local/lito381/server/", C=P+"ClientSession";
 static final Set<String> KEEP=Set.of("sourceEquipmentObjects","consumedSourceEquipmentObjects","sourceProjectileObjects","sourceMagicDolls","activeMagicDoll","sourceEquipmentMagicStats","sourceEquipmentVitalStats","sourceEquippedFireResistance");
 static int apply(ClassNode c) {
  int preserved=0,staged=0,wield=0,committed=0,label=0;
  for(var m:c.methods){
   if(m.name.equals("characterSelect")&&m.desc.equals("([BZ)V"))for(var n:m.instructions.toArray())if(n instanceof FieldInsnNode f&&f.getOpcode()==PUTFIELD&&f.owner.equals(C)&&KEEP.contains(f.name)){
    var code=new InsnList();code.add(new VarInsnNode(ALOAD,0));code.add(new LdcInsnNode(f.name));code.add(new MethodInsnNode(INVOKESTATIC,P+"EquipmentTransitions","preserve","(Ljava/lang/Object;L"+C+";Ljava/lang/String;)Ljava/lang/Object;",false));code.add(new TypeInsnNode(CHECKCAST,Type.getType(f.desc).getInternalName()));m.instructions.insertBefore(n,code);preserved++;
   }
   boolean weapon=m.name.equals("useSourceWeapon"),armor=m.name.equals("useSourceArmor");
   if(weapon||armor)for(var n:m.instructions.toArray()){
    if(n instanceof MethodInsnNode call&&call.owner.equals(C)&&call.name.equals("sendStagedSourceEquipmentState")){m.instructions.set(n,new InsnNode(POP2));staged++;}
    else if(n instanceof MethodInsnNode call&&call.owner.equals(C)&&call.name.equals("broadcastWieldState")){
     var code=new InsnList();code.add(new InsnNode(POP2));code.add(new InsnNode(POP));m.instructions.insertBefore(n,code);m.instructions.remove(n);wield++;
    }
    else if(n instanceof FieldInsnNode f&&f.getOpcode()==PUTFIELD&&f.owner.equals(C)&&f.name.equals("sourceEquippedFireResistance")){
     var code=new InsnList();code.add(new VarInsnNode(ALOAD,0));code.add(new VarInsnNode(ALOAD,weapon?4:5));code.add(new MethodInsnNode(INVOKEVIRTUAL,C,"sendStagedSourceEquipmentState","(Ljava/util/List;)V",false));
     if(weapon){code.add(new VarInsnNode(ALOAD,0));code.add(new VarInsnNode(ALOAD,0));code.add(new FieldInsnNode(GETFIELD,C,"inventory","L"+P+"RuntimeInventory;"));code.add(new MethodInsnNode(INVOKEVIRTUAL,P+"RuntimeInventory","equippedWalkAction","()I",false));code.add(new LdcInsnNode("source-weapon-committed"));code.add(new MethodInsnNode(INVOKEVIRTUAL,C,"broadcastWieldState","(ILjava/lang/String;)V",false));}
     m.instructions.insert(n,code);committed++;
    }
   }
   if(m.name.equals("sendStagedSourceEquipmentState"))for(var n:m.instructions.toArray())if(n instanceof LdcInsnNode l&&"staged-equipped".equals(l.cst)){l.cst="committed-equipped";label++;}
  }
  if(preserved!=8||staged!=2||wield!=1||committed!=2||label!=1)throw new IllegalStateException("Equipment patch drift: "+List.of(preserved,staged,wield,committed,label));
  return preserved+staged+wield+committed+label;
 }
}
