import java.util.*;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
final class HealingPatch implements Opcodes {
    static final String P="local/lito381/server/",H=P+"CombatHealing",C="L"+P+"ClientSession;";
    static MethodInsnNode call(String name,String desc){return new MethodInsnNode(INVOKESTATIC,H,name,desc,false);}
    static void apply(ClassNode c,Map<String,Integer> hooks) {
        int extra=0,water=0,owned=0,diagnostic=0;
        for(var m:c.methods){boolean nature=m.name.equals("useNaturesBlessingSpell")&&m.desc.equals("([BL"+P+"BattleSpellCatalog$NaturesBlessingSpell;I)V"),group=m.name.equals("useGroupHealingSpell");if(!nature&&!group)continue;
            if(nature)for(var n:m.instructions)if(n instanceof InvokeDynamicInsnNode d)for(int at=0;at<d.bsmArgs.length;at++)if(d.bsmArgs[at] instanceof String value&&value.contains("custom-extra-heal=excluded")){d.bsmArgs[at]=value.replace("custom-extra-heal=excluded","source-extra-after-status=true");diagnostic++;}
            for(var n:m.instructions.toArray())if(n instanceof MethodInsnNode v){
                if(v.owner.equals(P+"BattleHealingRecovery")&&v.name.equals("adjustedAmount")&&v.desc.equals("(IZZZ)I")){
                    if(nature){m.instructions.insert(n,call("blessingPlayer","(I)I"));extra++;}
                    else {
                        var cursor=DragonPatch.prior(n);int target=-1,clock=DragonPatch.local(m,"recoveryNow");
                        for(String flag:new String[]{"waterDragonHealingErosionActive","waterDragonDirtyCurrentActive","polluteWaterActive"}){
                            if(!(cursor instanceof MethodInsnNode b)||!b.owner.equals(P+"BattleTimedStatus")||!b.name.equals(flag)||!b.desc.equals("(J)Z"))throw new IllegalStateException("group healing source flag order");cursor=DragonPatch.prior(cursor);if(!(cursor instanceof VarInsnNode time)||time.getOpcode()!=LLOAD||time.var!=clock)throw new IllegalStateException("group recovery clock");cursor=DragonPatch.prior(cursor);if(!(cursor instanceof FieldInsnNode field)||field.getOpcode()!=GETFIELD||!field.name.equals("timedStatus")||!field.owner.equals(P+"ClientSession"))throw new IllegalStateException("group timed status producer");cursor=DragonPatch.prior(cursor);if(!(cursor instanceof VarInsnNode actor)||actor.getOpcode()!=ALOAD||target>=0&&target!=actor.var)throw new IllegalStateException("group player target identity");target=actor.var;cursor=DragonPatch.prior(cursor);
                        }
                        if(!(cursor instanceof VarInsnNode amount)||amount.getOpcode()!=ILOAD)throw new IllegalStateException("group lawful healing producer");var i=new InsnList();i.add(new VarInsnNode(ALOAD,target));i.add(new VarInsnNode(LLOAD,clock));i.add(call("groupPlayerBase","(I"+C+"J)I"));m.instructions.insert(cursor,i);water++;
                    }
                }
                if(v.owner.equals(P+"BattleCompanion")&&v.name.equals("heal")&&v.desc.equals("(I)I")){
                    var amount=DragonPatch.prior(n);var actor=DragonPatch.prior(amount);if(!(amount instanceof VarInsnNode a)||a.getOpcode()!=ILOAD||!(actor instanceof VarInsnNode t)||t.getOpcode()!=ALOAD||t.var!=DragonPatch.local(m,"companion"))throw new IllegalStateException("native group owned healing operands");var i=new InsnList();i.add(new VarInsnNode(ALOAD,0));i.add(new VarInsnNode(ALOAD,2));i.add(new MethodInsnNode(INVOKEVIRTUAL,P+"BattleSpellCatalog$"+(nature?"NaturesBlessingSpell":"GroupHealingSpell"),"skillId","()I",false));i.add(new VarInsnNode(LLOAD,DragonPatch.local(m,"castNow")));m.instructions.insertBefore(n,i);m.instructions.set(n,call("owned","(L"+P+"BattleCompanion;I"+C+"IJ)I"));owned++;
                }
            }
        }
        CombatPatch.require(diagnostic,1,"Nature source diagnostic");hooks.put("source-nature-healing-diagnostic",diagnostic);CombatPatch.require(extra,1,"Nature extra after source recovery modifiers");CombatPatch.require(water,1,"Heal All Water Life before source recovery modifiers");CombatPatch.require(owned,2,"native group owned source healing commits");hooks.put("source-nature-extra-after-status",extra);hooks.put("source-group-water-life",water);hooks.put("source-group-owned-healing",owned);
    }
}
