import java.util.*;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;

/** Only source awakening policy and existing awakening ability consumers. */
final class AwakenPatch implements Opcodes {
    static final String P="local/lito381/server/",H=P+"CombatAwaken",S=P+"BattleAwakenStatus$Snapshot";
    static final Map<String,Integer> ACTIVE=Map.of("useTeleportSpell",5,"useSpellBeforeDragonTargets",1,
            "admitClassicWorldSpellFamily",1,"centeredAreaCasterStillAdmitted",1,
            "decayPotionCasterStillAdmitted",1,"counterDetectionCasterStillAdmitted",1,"battleGfx",1);
    static final Map<String,String> STATS=Map.of("effectiveCharisma","CHA","baseStrength","STR",
            "effectiveConstitution","CON","baseDexterity","DEX","baseIntelligence","INT","effectiveWisdom","WIS");
    static void apply(ClassNode c,Map<String,Integer> hooks) {
        var active=new HashMap<String,Integer>();var stats=new HashMap<String,Integer>();int drain=0,clear=0,hp=0,ac=0,admission=0;
        var cast=c.methods.stream().filter(m->m.name.equals("useAwakenSpell")).findFirst().orElseThrow();
        int contextSlot=DragonPatch.local(cast,"sameCasterContext");
        for(var n:cast.instructions.toArray())if(n instanceof VarInsnNode local&&local.getOpcode()==ILOAD&&local.var==contextSlot) {
            var gate=new InsnList();gate.add(new VarInsnNode(ALOAD,0));gate.add(new VarInsnNode(ALOAD,2));
            gate.add(new MethodInsnNode(INVOKESTATIC,H,"sameCasterContext","(ZL"+P+"ClientSession;L"+P+"BattleSpellCatalog$AwakenSpell;)Z",false));
            cast.instructions.insert(n,gate);admission++;
        }
        for(var m:c.methods)for(var n:m.instructions.toArray())if(n instanceof MethodInsnNode call) {
            if(call.owner.equals(S)&&call.name.equals("active")&&ACTIVE.containsKey(m.name)) {
                m.instructions.set(n,new MethodInsnNode(INVOKESTATIC,H,"classicActive","(L"+S+";)Z",false));active.merge(m.name,1,Integer::sum);
            } else if(call.owner.equals(P+"BattleAwakenStatus")&&call.name.equals("pollMpDrainTicks")) {
                if(!m.name.equals("serverAwakeningMpDrain")||!call.desc.equals("(J)I"))throw new IllegalStateException("awakening drain caller drift");
                m.instructions.set(n,new MethodInsnNode(INVOKESTATIC,H,"pollMpDrainTicks","(L"+P+"BattleAwakenStatus;J)I",false));drain++;
            } else if(call.owner.equals(P+"BattlePolymorphStatus")&&call.name.equals("clear")&&m.name.equals("useAwakenSpell")) {
                var context=new InsnList();context.add(new VarInsnNode(ALOAD,2));context.add(new VarInsnNode(ALOAD,DragonPatch.local(m,"toggle")));
                m.instructions.insertBefore(n,context);
                m.instructions.set(n,new MethodInsnNode(INVOKESTATIC,H,"clearOnToggle","(L"+P+"BattlePolymorphStatus;L"+P+"BattleSpellCatalog$AwakenSpell;L"+P+"BattleAwakenStatus$Toggle;)L"+P+"BattlePolymorphCatalog$Form;",false));clear++;
            } else if(call.owner.equals(S)&&Set.of("allAbilityScoreBonus","maximumHpBonus","armorClassModifier").contains(call.name)) {
                String type;
                if(call.name.equals("allAbilityScoreBonus")) {
                    type=STATS.get(m.name);if(type==null)throw new IllegalStateException("awakening stat caller drift: "+m.name);
                    stats.merge(type,1,Integer::sum);
                } else if(call.name.equals("maximumHpBonus")) {type="HP";hp++;}
                else {type="AC";ac++;}
                m.instructions.insertBefore(n,new LdcInsnNode(type));
                m.instructions.set(n,new MethodInsnNode(INVOKESTATIC,H,"ability","(L"+S+";Ljava/lang/String;)I",false));
            }
        }
        if(!active.equals(ACTIVE)||!stats.equals(Map.of("CHA",1,"STR",1,"CON",1,"DEX",1,"INT",1,"WIS",1)))throw new IllegalStateException("awakening consumers drift: "+active+" "+stats);
        CombatPatch.require(drain,1,"source awake drain");CombatPatch.require(clear,1,"source awake polymorph");
        CombatPatch.require(hp,1,"source awake HP");CombatPatch.require(ac,1,"source awake AC");
        CombatPatch.require(admission,1,"source cross-awakening exclusion before costs");
        hooks.put("source-awakening-policy",14);hooks.put("source-awakening-abilities",8);
    }
}
