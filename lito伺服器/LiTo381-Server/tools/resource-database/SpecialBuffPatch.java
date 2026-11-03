import java.util.*;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;

/** Existing special-buff leases, source table projections and no new actor fields. */
final class SpecialBuffPatch implements Opcodes {
    static final String P="local/lito381/server/",C=P+"ClientSession",H=P+"CombatSpecialBuffs",E=P+"BattleDragonEyeCatalog$Eye";
    static final Set<String> EYE_INTS=Set.of("buffId","durationSeconds","castGfx","statusIcon","sharedDelayId","sharedDelayMillis","itemDelayMillis","canOverwrite",
            "meleeDamage","magicHit","damageReduction","evasionRate","stoneResistance","sleepResistance","freezeResistance","poisonResistance");
    static final Map<String,String> ABILITIES=Map.of("effectiveCharisma","CHA","baseStrength","STR","baseDexterity","DEX","effectiveConstitution","CON","baseIntelligence","INT","effectiveWisdom","WIS",
            "equipmentAdjustedBaseMaxHp","HP","equipmentAdjustedBaseMaxMp","MP");
    static final Map<String,String> REPLACE=Map.of("waterDragonBlessingArmorClass","AC","waterDragonBlessingHpRegen","HP_REGEN","waterDragonBlessingMpRegen","MP_REGEN");
    static final Map<String,String> SCALAR=Map.of("magicResistanceModifier","MR","spellPowerModifier","SP","effectiveArmorMagicReductionBase","DMG_REDUX");
    static boolean accepts(String path){return path.equals(E+".class");}
    static InsnList timed(String name,String type,boolean clock) {
        var i=new InsnList();i.add(new VarInsnNode(ALOAD,0));
        i.add(clock?new MethodInsnNode(INVOKESTATIC,"java/lang/System","nanoTime","()J",false):new VarInsnNode(LLOAD,1));i.add(new LdcInsnNode(type));
        i.add(new MethodInsnNode(INVOKESTATIC,H,name,"(IL"+C+";JLjava/lang/String;)I",false));return i;
    }
    static void apply(ClassNode c,Map<String,Integer> hooks) {
        if(c.name.equals(E)) {
            int ints=0,strings=0;
            for(var m:c.methods)for(var n:m.instructions.toArray()) {
                if(n.getOpcode()==IRETURN&&EYE_INTS.contains(m.name)) {
                    var i=new InsnList();i.add(new VarInsnNode(ALOAD,0));i.add(new LdcInsnNode(m.name));i.add(new MethodInsnNode(INVOKESTATIC,H,"eyeField","(IL"+E+";Ljava/lang/String;)I",false));m.instructions.insertBefore(n,i);ints++;
                } else if(n.getOpcode()==ARETURN&&Set.of("group","endMessage").contains(m.name)) {
                    var i=new InsnList();i.add(new VarInsnNode(ALOAD,0));i.add(new LdcInsnNode(m.name));i.add(new MethodInsnNode(INVOKESTATIC,H,"eyeText","(Ljava/lang/String;L"+E+";Ljava/lang/String;)Ljava/lang/String;",false));m.instructions.insertBefore(n,i);strings++;
                }
            }
            CombatPatch.require(ints,16,"eye SQL numeric settings/abilities");CombatPatch.require(strings,2,"eye SQL group/end message");hooks.put("source-eye-projections",18);return;
        }
        if(!c.name.equals(C))return;
        var counts=new TreeMap<String,Integer>();
        for(var m:c.methods) {
            boolean eyePhase=false;
            for(var n:m.instructions.toArray()) {
                if(n instanceof MethodInsnNode call) {
                    if(call.owner.equals(P+"ClassicWorldWaterDragonCatalog$Blessing")&&Set.of("maximumHp","maximumMp","bowHit","bowDamage").contains(call.name)
                            ||call.owner.equals(P+"BattleDragonEyeStatus$Snapshot")&&call.name.equals("meleeDamage")&&Set.of("attack","attackClassicWorldPlayer").contains(m.name)) {
                        m.instructions.insert(n,new MethodInsnNode(INVOKESTATIC,H,"legacy","(I)I",false));counts.merge("legacy",1,Integer::sum);
                    }
                    String type=ABILITIES.get(m.name);
                    if(type!=null&&call.owner.equals(P+"NumericBreakthrough")&&Set.of("clampVital","clampAbility").contains(call.name)&&call.desc.equals("(J)I")) {
                        var i=new InsnList();i.add(new VarInsnNode(ALOAD,0));i.add(new LdcInsnNode(type));i.add(new MethodInsnNode(INVOKESTATIC,H,"add","(JL"+C+";Ljava/lang/String;)J",false));m.instructions.insertBefore(n,i);counts.merge("add",1,Integer::sum);
                    }
                    if(Set.of("effectiveFireResistance","effectiveSpellElementalResistance","effectiveWeaponAttributeResistance").contains(m.name)
                            &&call.owner.equals(P+"NumericBreakthrough")&&call.name.equals("clampElementalResistance")) {
                        var i=new InsnList();i.add(new VarInsnNode(ALOAD,0));
                        if(m.name.equals("effectiveFireResistance")){i.add(new LdcInsnNode(2));i.add(new VarInsnNode(LLOAD,1));}
                        else {i.add(new VarInsnNode(ILOAD,1));i.add(new VarInsnNode(LLOAD,m.name.equals("effectiveWeaponAttributeResistance")?DragonPatch.local(m,"now"):2));}
                        i.add(new MethodInsnNode(INVOKESTATIC,H,"element","(JL"+C+";IJ)J",false));m.instructions.insertBefore(n,i);counts.merge("element",1,Integer::sum);
                    }
                    if(m.name.equals("effectiveRangedEvasionRate")&&call.owner.equals(P+"BattleRangedEvasion")&&call.name.equals("total")) {
                        var i=new InsnList();i.add(new VarInsnNode(ALOAD,0));i.add(new VarInsnNode(LLOAD,1));i.add(new MethodInsnNode(INVOKESTATIC,H,"evasion","(IL"+C+";J)I",false));m.instructions.insert(n,i);counts.merge("evasion",1,Integer::sum);
                    }
                    if(m.name.equals("weightProfile")&&call.owner.equals(P+"SourceEquipmentMagicStats")&&call.name.equals("totalWeightReduction")) {
                        var i=new InsnList();i.add(new VarInsnNode(ALOAD,0));i.add(new VarInsnNode(LLOAD,2));i.add(new MethodInsnNode(INVOKESTATIC,H,"weight","(IL"+C+";J)I",false));m.instructions.insert(n,i);counts.merge("weight",1,Integer::sum);
                    }
                    if(m.name.equals("useDragonEye")&&call.owner.equals(P+"GameHub")&&call.name.equals("roomMembers")) {
                        m.instructions.insertBefore(n,new VarInsnNode(ALOAD,0));m.instructions.set(n,new MethodInsnNode(INVOKESTATIC,H,"eyeRecipients","(L"+P+"GameHub;JL"+C+";)Ljava/util/List;",false));counts.merge("eyeRecipients",1,Integer::sum);
                    }
                }
                if(n instanceof FieldInsnNode field&&m.name.equals("useDragonEye")&&field.getOpcode()==GETFIELD&&field.owner.equals(C)&&field.name.equals("phase")&&!eyePhase) {
                    var i=new InsnList();i.add(new FieldInsnNode(GETSTATIC,P+"ClientSession$Phase","BATTLE","L"+P+"ClientSession$Phase;"));i.add(new VarInsnNode(ALOAD,0));
                    i.add(new MethodInsnNode(INVOKESTATIC,H,"eyePhase","(Ljava/lang/Object;Ljava/lang/Object;L"+C+";)Ljava/lang/Object;",false));i.add(new TypeInsnNode(CHECKCAST,P+"ClientSession$Phase"));m.instructions.insert(n,i);eyePhase=true;counts.merge("eyePhase",1,Integer::sum);
                }
                if(m.name.equals("applyClassicWorldWaterDragonSaellBlessing")&&n instanceof IntInsnNode value&&value.operand==4018){value.operand=4016;counts.merge("saellAlias",1,Integer::sum);}
                if(n.getOpcode()==IRETURN) {
                    if(REPLACE.containsKey(m.name)){m.instructions.insertBefore(n,timed("replace",REPLACE.get(m.name),false));counts.merge("replace",1,Integer::sum);}
                    if(SCALAR.containsKey(m.name)){m.instructions.insertBefore(n,timed("addScalar",SCALAR.get(m.name),m.name.equals("effectiveArmorMagicReductionBase")));counts.merge("addScalar",1,Integer::sum);}
                    if(m.name.equals("waterDragonBlessingWaterResistance")){m.instructions.insertBefore(n,new MethodInsnNode(INVOKESTATIC,H,"legacy","(I)I",false));counts.merge("legacy",1,Integer::sum);}
                    if(m.name.equals("expireWaterDragonBlessing")) {
                        var i=new InsnList();i.add(new VarInsnNode(ALOAD,0));i.add(new VarInsnNode(LLOAD,1));i.add(new MethodInsnNode(INVOKESTATIC,H,"expire","(ZL"+C+";J)Z",false));m.instructions.insertBefore(n,i);counts.merge("expire",1,Integer::sum);
                    }
                }
            }
        }
        var expected=Map.ofEntries(Map.entry("legacy",9),Map.entry("add",8),Map.entry("element",3),Map.entry("evasion",1),Map.entry("weight",1),Map.entry("eyeRecipients",1),Map.entry("eyePhase",1),
                Map.entry("saellAlias",1),Map.entry("replace",3),Map.entry("addScalar",3),Map.entry("expire",1));
        if(!counts.equals(expected))throw new IllegalStateException("special BUFF native consumer drift: "+counts);
        hooks.put("source-special-buff-consumers",counts.values().stream().mapToInt(Integer::intValue).sum());
    }
}
