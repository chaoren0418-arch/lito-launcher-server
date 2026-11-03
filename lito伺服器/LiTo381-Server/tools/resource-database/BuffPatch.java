import java.util.*;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;

/** Source BUFF numbers and bounded shared-group leases, on the pinned core. */
final class BuffPatch implements Opcodes {
    static final String P="local/lito381/server/",H=P+"CombatBuffLeases",A=P+"BattleAbilityStatus",FIELD="sourceBuffLeases";
    record Binding(int skill,String type,String method) { }
    static final Map<String,Map<String,Binding>> VALUES=Map.ofEntries(
        Map.entry("BattleAbilityStatus",Map.ofEntries(
            Map.entry("berserkArmorClass",new Binding(55,"AC","berserk")),Map.entry("berserkMeleeHit",new Binding(55,"HIT","berserk")),Map.entry("berserkMeleeDamage",new Binding(55,"DMG","berserk")),
            Map.entry("bounceAttackArmorClass",new Binding(89,"AC","bounceAttack")),Map.entry("bounceAttackMeleeHit",new Binding(89,"HIT","bounceAttack")),
            Map.entry("illusionOgreMeleeHit",new Binding(204,"HIT","illusionOgre")),Map.entry("illusionOgreMeleeDamage",new Binding(204,"DMG","illusionOgre")),
            Map.entry("illusionAvatarMeleeDamage",new Binding(219,"DMG","illusionAvatar")),Map.entry("illusionAvatarRangedDamage",new Binding(219,"BOW_DMG","illusionAvatar")),
            Map.entry("diamondGolemArmorClass",new Binding(214,"AC","diamondGolem")),Map.entry("braveAuraMeleeDamage",new Binding(117,"DMG","braveAura")))),
        Map.entry("BattleEarthBlessStatus",Map.of("armorClass",new Binding(159,"AC","snapshot"))),
        Map.entry("BattleIronSkinStatus",Map.of("armorClass",new Binding(168,"AC","snapshot"))),
        Map.entry("BattleGuardBrakeStatus",Map.of("armorClass",new Binding(183,"AC","snapshot"))),
        Map.entry("BattleClearMindStatus",Map.of("wisdomModifier",new Binding(137,"WIS","snapshot"))),
        Map.entry("BattleFireWeaponStatus",Map.of("meleeDamage",new Binding(148,"DMG","snapshot"))),
        Map.entry("BattleBurningWeaponStatus",Map.of("meleeHit",new Binding(163,"HIT","snapshot"),"meleeDamage",new Binding(163,"DMG","snapshot"))),
        Map.entry("BattleFireBlessStatus",Map.of("meleeDamage",new Binding(155,"DMG","snapshot"))),
        Map.entry("BattleWindShotStatus",Map.of("rangedHit",new Binding(149,"BOW_HIT","snapshot"))),
        Map.entry("BattleStormEyeStatus",Map.of("rangedHit",new Binding(156,"BOW_HIT","snapshot"),"rangedDamage",new Binding(156,"BOW_DMG","snapshot"))),
        Map.entry("BattleStormShotStatus",Map.of("rangedHit",new Binding(166,"BOW_HIT","snapshot"),"rangedDamage",new Binding(166,"BOW_DMG","snapshot"))));
    static final Map<String,String> GETTERS=Map.of("shield","Modifier","strength","Modifier","dexterity","Modifier",
            "strengthSkillId","I","dexteritySkillId","I","armorProtection","ArmorProtection","royalAura","RoyalAura");
    static boolean accepts(String entry){return entry.equals(P+"BattleArmBreakerStatus.class")||entry.startsWith(P)&&entry.endsWith(".class")&&VALUES.containsKey(entry.substring(P.length(),entry.length()-6));}
    static InsnList leases() {var i=new InsnList();i.add(new VarInsnNode(ALOAD,0));i.add(new FieldInsnNode(GETFIELD,A,FIELD,"L"+H+";"));return i;}
    static void apply(ClassNode c,Map<String,Integer> hooks) {
        if(c.name.equals(P+"BattleArmBreakerStatus")) {
            int value=0,duration=0;
            for(var m:c.methods)for(var n:m.instructions.toArray())if(n instanceof IntInsnNode constant) {
                if(m.name.equals("meleeDamageModifier")&&constant.getOpcode()==BIPUSH&&constant.operand==-2) {
                    var i=new InsnList();i.add(new LdcInsnNode(213));i.add(new LdcInsnNode("DMG"));i.add(new MethodInsnNode(INVOKESTATIC,H,"value","(IILjava/lang/String;)I",false));m.instructions.insert(n,i);value++;
                } else if(m.name.equals("activateIfAbsent")&&constant.getOpcode()==BIPUSH&&constant.operand==12) {
                    var next=DragonPatch.next(n);if(!(next instanceof JumpInsnNode jump)||jump.getOpcode()!=IF_ICMPEQ)throw new IllegalStateException("Arm Breaker duration guard drift");
                    m.instructions.set(n,new MethodInsnNode(INVOKESTATIC,H,"validArmDuration","(I)Z",false));jump.setOpcode(IFNE);duration++;
                }
            }
            CombatPatch.require(value,1,"canonical Arm Breaker213 SQL ability");CombatPatch.require(duration,1,"SQL Arm Breaker duration");hooks.put("source-arm-breaker-alias",2);return;
        }
        if(c.name.equals(P+"ClientSession")) {
            int replacements=0,auto=0,deaths=0;
            for(var m:c.methods)if(Set.of("takeClassicWorldDirectDamage","takePlayerDamage").contains(m.name))for(var n:m.instructions.toArray())
                if(n instanceof MethodInsnNode call&&call.owner.equals(P+"BattleAwakenStatus")&&call.name.equals("clear")) {
                    var i=new InsnList();i.add(new VarInsnNode(ALOAD,0));i.add(new MethodInsnNode(INVOKESTATIC,H,"onDeath","(L"+P+"BattleAwakenStatus$Snapshot;L"+P+"ClientSession;)L"+P+"BattleAwakenStatus$Snapshot;",false));
                    m.instructions.insert(n,i);deaths++;
                }
            for(var m:c.methods)for(var n:m.instructions.toArray())if(n instanceof MethodInsnNode call&&call.owner.equals(A)) {
                if(m.name.equals("useShieldSpell")&&call.name.equals("armorProtection")) {
                    m.instructions.insert(n,new MethodInsnNode(INVOKESTATIC,H,"replacement","(L"+A+"$ArmorProtection;)L"+A+"$ArmorProtection;",false));replacements++;
                }
                if(m.name.equals("maintainNativeMagicDollAutoSkills")&&Set.of("armorProtection","strengthSkillId","dexteritySkillId").contains(call.name)) {
                    String name=switch(call.name){case "armorProtection"->"autoArmorProtection";case "strengthSkillId"->"autoStrengthSkillId";default->"autoDexteritySkillId";};
                    m.instructions.set(n,new MethodInsnNode(INVOKESTATIC,H,name,"(L"+A+";"+call.desc.substring(1),false));auto++;
                }
            }
            CombatPatch.require(replacements,1,"source armor replacement icon");CombatPatch.require(auto,3,"source coexist auto-skill identities");
            CombatPatch.require(deaths,2,"source BUFF fatal damage clear");
            hooks.put("source-buff-caster-context",6);return;
        }
        var bindings=VALUES.get(c.name.substring(P.length()));if(bindings==null)return;
        var count=new HashMap<String,Integer>();
        for(var m:c.methods)for(var n:m.instructions.toArray())if(n instanceof FieldInsnNode field&&field.getOpcode()==GETFIELD&&field.owner.equals(c.name)) {
            var binding=bindings.get(field.name);if(binding==null||!binding.method().equals(m.name))continue;
            var i=new InsnList();i.add(new LdcInsnNode(binding.skill()));i.add(new LdcInsnNode(binding.type()));
            i.add(new MethodInsnNode(INVOKESTATIC,H,"value","(IILjava/lang/String;)I",false));m.instructions.insert(n,i);count.merge(field.name,1,Integer::sum);
        }
        for(var key:bindings.keySet())CombatPatch.require(count.getOrDefault(key,0),1,"source buff "+c.name+"."+key);
        hooks.put("source-buff-values-"+c.name.substring(P.length()),count.size());
        if(!c.name.equals(A))return;
        if(c.fields.stream().anyMatch(f->f.name.equals(FIELD)))throw new IllegalStateException("duplicate source buff state");
        c.fields.add(new FieldNode(ACC_PRIVATE|ACC_FINAL,FIELD,"L"+H+";",null,null));
        int ctor=0,getters=0,activate=0,clear=0,remove=0;
        for(var m:c.methods) {
            if(m.name.equals("<init>"))for(var n:m.instructions.toArray())if(n.getOpcode()==RETURN) {
                var i=new InsnList();i.add(new VarInsnNode(ALOAD,0));i.add(new TypeInsnNode(NEW,H));i.add(new InsnNode(DUP));
                i.add(new MethodInsnNode(INVOKESPECIAL,H,"<init>","()V",false));i.add(new FieldInsnNode(PUTFIELD,A,FIELD,"L"+H+";"));m.instructions.insertBefore(n,i);ctor++;
            }
            if(GETTERS.containsKey(m.name)&&m.desc.startsWith("(J)")) {
                String type=GETTERS.get(m.name),ret=type.equals("I")?"I":"L"+A+"$"+type+";";
                if(!m.desc.equals("(J)"+ret))throw new IllegalStateException("source buff getter signature drift");
                var legacy=new LabelNode();var i=new InsnList();i.add(new MethodInsnNode(INVOKESTATIC,H,"loaded","()Z",false));i.add(new JumpInsnNode(IFEQ,legacy));
                i.add(leases());i.add(new VarInsnNode(LLOAD,1));i.add(new MethodInsnNode(INVOKEVIRTUAL,H,m.name,m.desc,false));i.add(new InsnNode(type.equals("I")?IRETURN:ARETURN));
                i.add(legacy);i.add(new FrameNode(F_SAME,0,null,0,null));m.instructions.insert(i);getters++;
            }
            if(Set.of("activateArmorProtection","activateStrength","activateDexterity","activateRoyalAura").contains(m.name)&&m.desc.startsWith("(IJ")) {
                for(var n:m.instructions.toArray())if(n.getOpcode()==IRETURN||n.getOpcode()==RETURN) {
                    var i=leases();i.add(new VarInsnNode(ILOAD,1));i.add(new VarInsnNode(LLOAD,2));i.add(new VarInsnNode(ILOAD,4));
                    if(n.getOpcode()==IRETURN) {
                        // The legacy replacement ID is already on the stack.
                        // Put the lease receiver before it for instance dispatch.
                        i=leases();i.add(new InsnNode(SWAP));i.add(new VarInsnNode(ILOAD,1));i.add(new VarInsnNode(LLOAD,2));i.add(new VarInsnNode(ILOAD,4));
                        i.add(new MethodInsnNode(INVOKEVIRTUAL,H,"replace","(IIJI)I",false));
                    } else i.add(new MethodInsnNode(INVOKEVIRTUAL,H,"activate","(IJI)V",false));
                    m.instructions.insertBefore(n,i);activate++;
                }
            }
            if(m.name.equals("clearCancelable"))for(var n:m.instructions.toArray())if(n.getOpcode()==RETURN) {
                var i=leases();i.add(new MethodInsnNode(INVOKEVIRTUAL,H,"cancel","()V",false));m.instructions.insertBefore(n,i);clear++;
            }
            if(m.name.equals("clear"))for(var n:m.instructions.toArray())if(n.getOpcode()==RETURN) {
                var i=leases();i.add(new MethodInsnNode(INVOKEVIRTUAL,H,"clear","()V",false));m.instructions.insertBefore(n,i);clear++;
            }
            if(m.name.equals("removeSkill"))for(var n:m.instructions.toArray())if(n.getOpcode()==IRETURN) {
                var i=leases();i.add(new InsnNode(SWAP));i.add(new VarInsnNode(ILOAD,1));i.add(new MethodInsnNode(INVOKEVIRTUAL,H,"remove","(ZI)Z",false));m.instructions.insertBefore(n,i);remove++;
            }
        }
        CombatPatch.require(ctor,1,"source buff owner construction");CombatPatch.require(getters,7,"source shared buff getters");
        CombatPatch.require(activate,4,"source shared buff activation");CombatPatch.require(clear,2,"source shared buff cancellation/full clear");
        CombatPatch.require(remove,5,"source shared buff targeted cancellation");
        hooks.put("source-buff-shared-lease",ctor+getters+activate+clear+remove);
    }
}
