import java.util.*;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;

/** Pinned-base hooks only; asserts prevent silently applying to a different core. */
final class CombatPatch implements Opcodes {
    static final String P="local/lito381/server/", H=P+"CombatRules";
    static final Set<String> NUMERIC_FIELDS=Set.of("mpCost","hpCost","reuseDelayMillis","durationSeconds",
            "baseDamage","baseDiceCount","dieSides","diceCount","itemConsumeId","itemConsumeCount",
            "probabilityDice","probabilityValue");
    static final Set<String> CONTROL_FIELDS=Set.of("specialCriticalChancePercent","specialCriticalGfx",
            "manaGain","warningDelayMillis","paralysisDurationMillis","baseStunSeconds",
            "randomStunSeconds","minimumBindSeconds","maximumBindSeconds","baseReflectionDamage");
    static final Set<String> CLASSES=Set.of(P+"BattleShockStunEffects.class",P+"BattlePhysicalDefense.class",
            P+"BattlePhysicalProcBuff.class", P+"BattleStrikerGale.class",P+"BattleArmorBreak.class",
            P+"BattleAbilityStatus$Protection.class",P+"BattleJoyOfPain.class",P+"BattleTurnUndead.class",P+"BattleMortalBody$Decision.class");
    static void apply(ClassNode c, Map<String,Integer> hooks) {
        if(c.name.equals(P+"BattleStrikerGale")||c.name.equals(P+"BattleArmorBreak")) {
            var method=method(c,c.name.equals(P+"BattleStrikerGale")?"playerWeaponEligible":"supportsWeaponType","(I)Z");
            var prefix=new InsnList();prefix.add(new VarInsnNode(ILOAD,0));prefix.add(call("weaponActionType","(I)I"));prefix.add(new VarInsnNode(ISTORE,0));method.instructions.insert(prefix);
            hooks.merge("source-weapon-type-adapters",1,Integer::sum);
        }
        if(c.name.equals(P+"BattleShockStunEffects")) {
            var m=method(c,"place","(JIIJI)L"+P+"BattleShockStunEffects$Placement;");int count=0;
            for(var n:m.instructions.toArray())if(n.getOpcode()==ICONST_4&&n.getNext() instanceof JumpInsnNode jump&&Set.of(IF_ICMPLE,IF_ICMPGT).contains(jump.getOpcode())){m.instructions.set(n,new LdcInsnNode(65535));count++;}
            require(count,1,"source Shock Stun effect duration bound");hooks.put("combat-shock-effect-duration",count);
        }
        if(c.name.equals(P+"BattleMortalBody$Decision")) {
            var m=method(c,"<init>","(ZIIZIII)V");int count=0;
            for(var n:m.instructions.toArray())if(n instanceof JumpInsnNode jump&&jump.getOpcode()==IFLE
                    &&n.getPrevious() instanceof VarInsnNode local&&local.getOpcode()==ILOAD&&local.var==5) {
                jump.setOpcode(IFLT);count++;
            }
            require(count,1,"mortal-zero-base");hooks.put("combat-mortal-zero-base",count);
        }
        if(c.name.equals(P+"BattlePhysicalDefense")) {
            var m=method(c,"againstPlayer","(IIIIIIII)L"+P+"BattlePhysicalDefense$PlayerOutcome;");
            int count=0;
            for(var n:m.instructions.toArray()) if(n instanceof VarInsnNode v
                    && v.getOpcode()==ISTORE && v.var==14) {
                var i=new InsnList();i.add(new VarInsnNode(ILOAD,14));i.add(new VarInsnNode(ILOAD,2));
                i.add(call("damageAfterExtraAc","(II)I"));i.add(new VarInsnNode(ISTORE,14));
                m.instructions.insert(n,i);count++;
            }
            require(count,1,"extra-ac");hooks.put("combat-extra-ac",count);
        }
        if(c.name.equals(P+"ClientSession")) {
            var m=method(c,"companionPhysicalDamage","(IIIIIII)I");
            int count=replaceConstants(m,0.3,"PET_TO_PC_DAMAGE_MULTIPLIER");
            require(count,1,"pet-to-pc");hooks.put("combat-pet-to-pc",count);
            var teleport=method(c,"useTeleportSpell","([B)V");int mp=0,reuse=0;
            for(var n:teleport.instructions.toArray()) {
                if(n instanceof IntInsnNode v&&v.operand==10) {
                    boolean delay=n.getNext() instanceof VarInsnNode store&&store.getOpcode()==ISTORE&&store.var==6;
                    var i=skillValue(5,"TeleportSpell",delay?"reuseDelayMillis":"mpCost");teleport.instructions.insert(n,i);
                    if(delay)reuse++;else mp++;
                }
                if(n instanceof LdcInsnNode v && Long.valueOf(10000000L).equals(v.cst)) {
                    var i=new InsnList();i.add(new InsnNode(POP2));i.add(new IntInsnNode(BIPUSH,10));
                    i.add(skillValue(5,"TeleportSpell","reuseDelayMillis"));i.add(new InsnNode(I2L));
                    i.add(new LdcInsnNode(1000000L));i.add(new InsnNode(LMUL));teleport.instructions.insert(n,i);reuse++;
                }
            }
            require(mp,3,"teleport-mp");require(reuse,2,"teleport-reuse");hooks.put("combat-teleport-values",mp+reuse);
            int materials=0;
            for(var casterMethod:c.methods)if(casterMethod.name.startsWith("use")&&casterMethod.desc.contains("BattleSpellCatalog$")) {
                for(var n:casterMethod.instructions.toArray()) if(n instanceof MethodInsnNode call
                        &&call.owner.equals(P+"RuntimeInventory")&&call.name.equals("removeCatalog")&&call.desc.equals("(JJ)Z")) {
                    casterMethod.instructions.set(n,new MethodInsnNode(INVOKESTATIC,P+"CombatDatabase","consumeMaterial",
                            "(L"+P+"RuntimeInventory;JJ)Z",false));materials++;
                }
            }
            if(materials<1)throw new IllegalStateException("missing typed spell material consumers");
            hooks.put("combat-material-consumers",materials);
        }
        if(c.name.equals(P+"BattlePhysicalProcBuff")) {
            for(var binding:List.of(new String[]{"burningSpirit","33","BURNING_CHANCE","BURNING_DMG"},
                    new String[]{"elementalFire","33","ELEMENTAL_FIRE_RND","ELEMENTAL_FIRE"},
                    new String[]{"doubleBrake","15","DOUBLE_BREAK_CHANCE","DOUBLE_BREAK_DMG"})) {
                var m=c.methods.stream().filter(x->x.name.equals(binding[0])).findFirst().orElseThrow();
                int chance=Integer.parseInt(binding[1]),count=0;
                for(var n:m.instructions.toArray()) if(n instanceof IntInsnNode v&&v.operand==chance) {
                    var i=new InsnList();i.add(new LdcInsnNode(binding[2]));
                    i.add(call("replaceInt","(ILjava/lang/String;)I"));m.instructions.insert(n,i);count++;
                }
                require(count,3,binding[0]+"-chance");
                require(replaceConstants(m,1.5,binding[3]),1,binding[0]+"-multiplier");
                hooks.put("combat-proc-"+binding[0],count+1);
            }
            require(replaceConstants(method(c,"soulOfFlameMultiplier","(Z)D"),1.5,
                    "SOUL_OF_FLAME_DAMAGE"),1,"soul-of-flame");hooks.put("combat-soul-of-flame",1);
        }
        if(c.name.equals(P+"BattleStrikerGale")) {
            require(replaceConstants(method(c,"damageAfterStatus","(IZZ)I"),1.2,"STRIKER_DMG2"),1,"striker");
            hooks.put("combat-striker",1);
        }
        if(c.name.equals(P+"BattleAbilityStatus$Protection")) {
            var m=method(c,"applyTo","(I)I");int count=0;
            for(var n:m.instructions.toArray()) if(n.getOpcode()==IDIV) {
                // Stack is damage, legacy integer divisor; state still controls expiry.
                m.instructions.insertBefore(n,new InsnNode(POP));
                m.instructions.set(n,call("protectedDamage","(I)I"));count++;
            }
            require(count,1,"immune-to-harm");hooks.put("combat-immune-to-harm",count);
        }
        if(c.name.equals(P+"BattleJoyOfPain")) {
            var m=method(c,"feedbackDamage","(III)I");int divisor=0,cap=0;
            for(var n:m.instructions.toArray()) {
                if(n.getOpcode()==ICONST_3) {
                    var i=new InsnList();i.add(new LdcInsnNode("JOY_OF_PAIN_COUNTDMG"));
                    i.add(call("replaceInt","(ILjava/lang/String;)I"));m.instructions.insert(n,i);divisor++;
                }
                if(n instanceof IntInsnNode v&&v.operand==1500) {
                    var i=new InsnList();i.add(new LdcInsnNode("JOY_OF_PAIN_DMG"));
                    i.add(call("replaceInt","(ILjava/lang/String;)I"));m.instructions.insert(n,i);cap++;
                }
            }
            require(divisor,1,"joy-divisor");require(cap,1,"joy-cap");hooks.put("combat-joy",2);
        }
        if(c.name.equals(P+"BattleTurnUndead")) {
            var level=method(c,"magicLevel","(II)I");level.instructions.clear();level.tryCatchBlocks.clear();
            if(level.localVariables!=null)level.localVariables.clear();
            level.instructions.add(new VarInsnNode(ILOAD,0));level.instructions.add(new VarInsnNode(ILOAD,1));
            level.instructions.add(call("magicLevel","(II)I"));level.instructions.add(new InsnNode(IRETURN));
            int count=0;
            var probability=method(c,"probability","(IIIIIIIILjava/util/function/IntUnaryOperator;)I");
            for(var n:probability.instructions.toArray())if(n.getOpcode()==ICONST_1
                    && n.getPrevious() instanceof VarInsnNode v&&v.getOpcode()==ILOAD&&v.var==0
                    &&n.getNext() instanceof JumpInsnNode jump&&jump.getOpcode()==IF_ICMPNE) {
                probability.instructions.set(n,new InsnNode(ICONST_3));count++;
            }
            require(count,1,"source-wizard-dice-type");hooks.put("combat-magic-class-types",2);
        }
    }
    static MethodNode method(ClassNode c,String name,String desc) {
        return c.methods.stream().filter(m->m.name.equals(name)&&m.desc.equals(desc)).findFirst()
                .orElseThrow(()->new IllegalStateException("missing hook "+c.name+"."+name+desc));
    }
    static int replaceConstants(MethodNode m,double old,String key) {
        int count=0;
        for(var n:m.instructions.toArray()) if(n instanceof LdcInsnNode v
                && v.cst instanceof Double d && Double.compare(d,old)==0) {
            var i=new InsnList();i.add(new LdcInsnNode(key));
            i.add(call("replace","(DLjava/lang/String;)D"));m.instructions.insert(n,i);count++;
        }
        return count;
    }
    static MethodInsnNode call(String name,String desc) {
        return new MethodInsnNode(INVOKESTATIC,H,name,desc,false);
    }
    static InsnList skillValue(int id,String type,String field) {
        var i=new InsnList();i.add(new LdcInsnNode(id));i.add(new LdcInsnNode(type));i.add(new LdcInsnNode(field));
        i.add(new MethodInsnNode(INVOKESTATIC,P+"CombatDatabase","value","(IILjava/lang/String;Ljava/lang/String;)I",false));return i;
    }
    static void require(int actual,int expected,String hook) {
        if(actual!=expected)throw new IllegalStateException(hook+": expected "+expected+" sites, got "+actual);
    }
}
