import java.util.*;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
final class CompanionSpellPatch implements Opcodes {
    static final String P="local/lito381/server/";
    static void apply(ClassNode c,Map<String,Integer> hooks) {
        var m=CombatPatch.method(c,"useShockStunSpell","([BL"+P+"BattleSpellCatalog$ShockStunSpell;)V");var fallback=new LabelNode();var i=new InsnList();
        i.add(new VarInsnNode(ALOAD,0));i.add(new VarInsnNode(ALOAD,1));i.add(new VarInsnNode(ALOAD,2));i.add(new MethodInsnNode(INVOKESTATIC,P+"CombatCompanionSpells","shock","(L"+P+"ClientSession;[BL"+P+"BattleSpellCatalog$ShockStunSpell;)Z",false));i.add(new JumpInsnNode(IFEQ,fallback));i.add(new InsnNode(RETURN));i.add(fallback);i.add(new FrameNode(F_SAME,0,null,0,null));m.instructions.insert(i);hooks.put("source-battle-controlled-shock",1);
        var centered=CombatPatch.method(c,"useCenteredFreezeAttackSpell","([BL"+P+"BattleSpellCatalog$FreezeAttackSpell;)V");String batch=P+"CombatCompanionSpells$Batch",spell="L"+P+"BattleSpellCatalog$FreezeAttackSpell;",client="L"+P+"ClientSession;";
        int slot=MagicDamagePatch.appendLocal(c,centered,batch),targetNow=DragonPatch.local(centered,"targetNow"),castNow=DragonPatch.local(centered,"castNow"),wire=DragonPatch.local(centered,"wireTargets"),recipients=DragonPatch.local(centered,"recipients"),applied=DragonPatch.local(centered,"applied");int capture=0,draw=0,commit=0;
        for(var node:centered.instructions.toArray()) {
            if(node instanceof VarInsnNode store&&store.getOpcode()==LSTORE&&store.var==targetNow) {var args=new InsnList();args.add(new VarInsnNode(ALOAD,0));args.add(new VarInsnNode(ALOAD,2));args.add(new VarInsnNode(LLOAD,targetNow));args.add(new MethodInsnNode(INVOKESTATIC,P+"CombatCompanionSpells","captureCenteredFreeze","("+client+spell+"J)L"+batch+";",false));args.add(new VarInsnNode(ASTORE,slot));centered.instructions.insert(node,args);capture++;}
            if(node instanceof MethodInsnNode call&&call.owner.equals("java/util/List")&&call.name.equals("isEmpty")&&MagicDamagePatch.prior(call) instanceof VarInsnNode load&&load.getOpcode()==ALOAD&&load.var==wire) {var args=new InsnList();args.add(new VarInsnNode(ALOAD,0));args.add(new VarInsnNode(ALOAD,2));args.add(new VarInsnNode(ALOAD,slot));args.add(new VarInsnNode(LLOAD,castNow));args.add(new VarInsnNode(ALOAD,wire));args.add(new MethodInsnNode(INVOKESTATIC,P+"CombatCompanionSpells","drawCenteredFreeze","("+client+spell+"L"+batch+";JLjava/util/List;)L"+batch+";",false));args.add(new VarInsnNode(ASTORE,slot));centered.instructions.insertBefore(load,args);draw++;}
            if(node instanceof VarInsnNode store&&store.getOpcode()==ISTORE&&store.var==applied&&MagicDamagePatch.prior(node).getOpcode()==ICONST_0) {var args=new InsnList();args.add(new VarInsnNode(ALOAD,0));args.add(new VarInsnNode(ALOAD,2));args.add(new VarInsnNode(ALOAD,slot));args.add(new VarInsnNode(LLOAD,castNow));args.add(new VarInsnNode(ALOAD,recipients));args.add(new MethodInsnNode(INVOKESTATIC,P+"CombatCompanionSpells","commitCenteredFreeze","("+client+spell+"L"+batch+";JLjava/util/List;)I",false));args.add(new VarInsnNode(ISTORE,applied));centered.instructions.insert(node,args);commit++;}
        }
        CombatPatch.require(capture,1,"controlled centered-freeze target snapshot");CombatPatch.require(draw,1,"controlled centered-freeze wire/damage preparation");CombatPatch.require(commit,1,"controlled centered-freeze native commit phase");hooks.put("source-battle-controlled-centered-freeze",capture+draw+commit);
    }
}
