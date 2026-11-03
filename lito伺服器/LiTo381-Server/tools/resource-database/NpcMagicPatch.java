import java.util.*;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
import org.objectweb.asm.tree.analysis.*;
final class NpcMagicPatch implements Opcodes {
    static final String P="local/lito381/server/",H=P+"CombatNpcMagic",A=P+"CombatMagicDamage$Attempt",C=P+"ClientSession";
    static void cloneNpc(ClassNode c,Map<String,Integer> hooks){
        var source=CombatPatch.method(c,"ordinaryMagicDamage",MagicDamagePatch.ORDINARY);var m=new MethodNode(source.access,"sourceNpcMagicBeforeResistance",MagicDamagePatch.PRE_MR,null,source.exceptions.toArray(String[]::new));source.accept(m);m.name="sourceNpcMagicBeforeResistance";m.desc=MagicDamagePatch.PRE_MR;int mr=0,signed=0,returns=0;
        for(var n:m.instructions.toArray()){
            if(n instanceof MethodInsnNode call&&call.owner.equals(P+"BattleMagicResistance")&&call.name.equals("damageAfterResistance")){m.instructions.set(call,new MethodInsnNode(INVOKESTATIC,P+"CombatMagicDamage","deferMr","(III)I",false));mr++;}
            if(n instanceof MethodInsnNode call&&call.owner.equals("java/lang/Math")&&call.name.equals("max")&&call.desc.equals("(II)I")){if(DragonPatch.prior(n).getOpcode()!=ICONST_0||DragonPatch.prior(DragonPatch.prior(n)).getOpcode()!=ISUB)continue;m.instructions.set(call,new MethodInsnNode(INVOKESTATIC,H,"signedPreMr","(II)I",false));signed++;}
            if(n.getOpcode()==IRETURN){var i=new InsnList();i.add(new VarInsnNode(ILOAD,5));i.add(new VarInsnNode(ALOAD,0));i.add(new MethodInsnNode(INVOKEVIRTUAL,C,"originalMagicHit","()I",false));i.add(new MethodInsnNode(INVOKESTATIC,H,"attempt","(III)L"+A+";",false));m.instructions.insertBefore(n,i);m.instructions.set(n,new InsnNode(ARETURN));returns++;}
        }
        CombatPatch.require(mr,1,"NPC deferred MR clone");CombatPatch.require(signed,1,"NPC signed pre-MR clone");CombatPatch.require(returns,1,"NPC scalar attempt clone");c.methods.add(m);hooks.put("source-npc-magic-clone",mr+signed+returns);
    }
    static void apply(ClassNode c,Map<String,Integer> hooks)throws AnalyzerException {
        var paths=new TreeMap<String,Integer>();int total=0;
        for(var m:c.methods){if(!Set.of("useLineAttackSpell","useCenteredFreezeAttackSpell","useClassicWorldLineAttackSpell","useClassicWorldLineFreezeAttackSpell","useClassicWorldCenteredFreezeAttackSpell","useClassicWorldCenteredAreaAttackSpell","useClassicWorldAreaAttackSpell","useAreaAttackSpell","useCenteredAreaAttackSpell").contains(m.name))continue;
            m.maxStack+=64;var nodes=m.instructions.toArray();var frames=new Analyzer<>(new SourceInterpreter()).analyze(c.name,m);
            for(int at=0;at<nodes.length;at++)if(nodes[at] instanceof MethodInsnNode call&&call.owner.equals(C)&&Set.of("ordinaryMagicDamage","areaSpellDamage","lineSpellDamage").contains(call.name)){
                var frame=frames[at];var armor=frame.getStack(frame.getStackSize()-2);if(armor.insns.size()!=1||armor.insns.iterator().next().getOpcode()!=ICONST_0)throw new IllegalStateException("NPC magic call has a player armor producer "+m.name);
                String name=call.name.equals("ordinaryMagicDamage")?"ordinaryComplete":call.name.equals("areaSpellDamage")?"area":"line";m.instructions.set(call,new MethodInsnNode(INVOKESTATIC,H,name,"(L"+C+";"+call.desc.substring(1),false));paths.merge(m.name,1,Integer::sum);total++;
            }
        }
        var expected=new TreeMap<>(Map.of("useLineAttackSpell",4,"useCenteredFreezeAttackSpell",4,"useClassicWorldLineAttackSpell",1,"useClassicWorldLineFreezeAttackSpell",1,"useClassicWorldCenteredFreezeAttackSpell",1,"useClassicWorldCenteredAreaAttackSpell",4,"useClassicWorldAreaAttackSpell",4,"useAreaAttackSpell",4,"useCenteredAreaAttackSpell",4));if(!paths.equals(expected))throw new IllegalStateException("NPC magic callers changed="+paths);CombatPatch.require(total,27,"NPC area/line MR scalar paths");hooks.put("source-npc-area-line-magic",total);cloneNpc(c,hooks);
    }
}
