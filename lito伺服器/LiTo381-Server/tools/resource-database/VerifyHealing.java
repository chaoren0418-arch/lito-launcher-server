import java.util.*;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
import static org.objectweb.asm.Opcodes.*;

/** Independently invert source healing hooks while retaining native admission, rolls, commits and packets. */
final class VerifyHealing {
    static final String P="local/lito381/server/",H=P+"CombatHealing",C="L"+P+"ClientSession;";
    static AbstractInsnNode before(AbstractInsnNode n){return VerifyCombatPreservation.prior(n);}
    static AbstractInsnNode after(AbstractInsnNode n){return VerifyCombatPreservation.following(n);}
    static int slot(MethodNode m,String name){return VerifyLineSpells.slot(m,name);}
    static VarInsnNode local(AbstractInsnNode n,int opcode){return VerifyLineSpells.local(n,opcode);}
    static void require(boolean good,String why){if(!good)throw new AssertionError("source healing "+why);}
    static void erase(MethodNode m,AbstractInsnNode...nodes){VerifyLineSpells.erase(m,nodes);}
    static MethodNode normalize(MethodNode source){
        var m=new MethodNode(source.access,source.name,source.desc,source.signature,source.exceptions.toArray(String[]::new));source.accept(m);if(!Arrays.stream(m.instructions.toArray()).anyMatch(n->n instanceof MethodInsnNode c&&c.owner.equals(H)))return m;
        boolean nature=m.name.equals("useNaturesBlessingSpell")&&m.desc.equals("([BL"+P+"BattleSpellCatalog$NaturesBlessingSpell;I)V"),group=m.name.equals("useGroupHealingSpell")&&m.desc.equals("([BL"+P+"BattleSpellCatalog$GroupHealingSpell;)V");require(nature||group,"native group method identity");var counts=new TreeMap<String,Integer>();
        for(var n:m.instructions.toArray())if(m.instructions.contains(n)&&n instanceof MethodInsnNode call&&call.owner.equals(H)){
            counts.merge(call.name,1,Integer::sum);
            switch(call.name){
                case "blessingPlayer"->{var original=before(call);var store=after(call);require(nature&&call.desc.equals("(I)I")&&original instanceof MethodInsnNode recovery&&recovery.owner.equals(P+"BattleHealingRecovery")&&recovery.name.equals("adjustedAmount")&&recovery.desc.equals("(IZZZ)I")&&local(store,ISTORE).var==slot(m,"adjustedHealing"),"Nature extra must follow original signed status adjustments and precede original healing result store");erase(m,call);}
                case "groupPlayerBase"->{var time=before(call);var target=before(time);var lawful=before(target);var flagTarget=after(call);var field=after(flagTarget);var flagTime=after(field);var flag=after(flagTime);require(group&&call.desc.equals("(I"+C+"J)I")&&local(time,LLOAD).var==slot(m,"recoveryNow")&&lawful instanceof VarInsnNode a&&a.getOpcode()==ILOAD&&local(target,ALOAD).var==local(flagTarget,ALOAD).var&&field instanceof FieldInsnNode f&&f.getOpcode()==GETFIELD&&f.owner.equals(P+"ClientSession")&&f.name.equals("timedStatus")&&local(flagTime,LLOAD).var==local(time,LLOAD).var&&flag instanceof MethodInsnNode get&&get.owner.equals(P+"BattleTimedStatus")&&get.name.equals("polluteWaterActive")&&get.desc.equals("(J)Z"),"Water Life uses actual per-target recovery clock before original status operands");erase(m,target,time,call);}
                case "owned"->{var time=before(call);var skill=before(time);var spell=before(skill);var caster=before(spell);var amount=before(caster);var actor=before(amount);String catalog=P+"BattleSpellCatalog$"+(nature?"NaturesBlessingSpell":"GroupHealingSpell");require(call.desc.equals("(L"+P+"BattleCompanion;I"+C+"IJ)I")&&local(time,LLOAD).var==slot(m,"castNow")&&skill instanceof MethodInsnNode get&&get.getOpcode()==INVOKEVIRTUAL&&get.owner.equals(catalog)&&get.name.equals("skillId")&&get.desc.equals("()I")&&local(spell,ALOAD).var==2&&local(caster,ALOAD).var==0&&amount instanceof VarInsnNode a&&a.getOpcode()==ILOAD&&local(actor,ALOAD).var==slot(m,"companion"),"owned commit actual catalog, caster, companion and accepted cast clock");
                    var store=before(actor);require(store instanceof VarInsnNode value&&value.getOpcode()==ISTORE&&value.var==local(amount,ILOAD).var&&before(store) instanceof MethodInsnNode lawful&&lawful.owner.equals(catalog)&&lawful.name.equals("applyLawfulRevision")&&lawful.desc.equals("(IJ)I"),"owned scalar must be original lawful result");erase(m,caster,spell,skill,time);m.instructions.set(call,new MethodInsnNode(INVOKEVIRTUAL,P+"BattleCompanion","heal","(I)I",false));}
                default->throw new AssertionError("unexpected source healing hook="+call.name);
            }
        }
        int diagnostic=0;for(var n:m.instructions)if(n instanceof InvokeDynamicInsnNode d)for(int at=0;at<d.bsmArgs.length;at++)if(d.bsmArgs[at] instanceof String value&&value.contains("source-extra-after-status=true")){require(nature,"Nature diagnostic method");d.bsmArgs[at]=value.replace("source-extra-after-status=true","custom-extra-heal=excluded");diagnostic++;}require(diagnostic==(nature?1:0),"exact Nature source diagnostic count");
        require(counts.equals(new TreeMap<>(nature?Map.of("blessingPlayer",1,"owned",1):Map.of("groupPlayerBase",1,"owned",1))),"exact group hook counts="+counts);return m;
    }
}
