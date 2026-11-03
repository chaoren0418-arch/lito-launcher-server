import java.util.*;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;

/** Native WORLD arithmetic/wire retained; owned view and commit stay method local. */
final class WorldCompanionPhysicalPatch implements Opcodes {
    static final String P="local/lito381/server/",H=P+"CombatWorldPhysical",T=H+"$Target",C="L"+P+"ClientSession;",V="L"+P+"ClassicWorldMonsterPopulation$Snapshot;",PH=P+"CombatCompanionDamage$Physical";
    static MethodInsnNode call(String name,String desc){return new MethodInsnNode(INVOKESTATIC,H,name,desc,false);}
    static void apply(ClassNode c,Map<String,Integer> hooks) {
        var m=CombatPatch.method(c,"attackClassicWorld","(L"+P+"Protocol$AttackRequest;L"+P+"ClientSession$AttackEnvelope;)V");int target=DragonPatch.local(m,"target"),weapon=DragonPatch.local(m,"weapon"),ranged=DragonPatch.local(m,"ranged"),raw=DragonPatch.local(m,"beforeDefense"),pre=DragonPatch.local(m,"preCritical"),critical=DragonPatch.local(m,"critical"),damage=DragonPatch.local(m,"damage"),guardian=DragonPatch.local(m,"guardianHarvest"),when=DragonPatch.local(m,"physicalAttackNowNanos");
        int ctx=MagicDamagePatch.appendLocal(c,m,T),attempt=MagicDamagePatch.appendLocal(c,m,PH),prepared=0,motion=0,hits=0,attempts=0,finish=0,commits=0,special=0;
        for(var n:m.instructions.toArray()) {
            if(n instanceof VarInsnNode store&&store.getOpcode()==ASTORE&&store.var==target) {
                var i=new InsnList();i.add(new VarInsnNode(ALOAD,target));i.add(new VarInsnNode(ALOAD,0));i.add(new VarInsnNode(ALOAD,1));i.add(call("prepare","("+V+C+"L"+P+"Protocol$AttackRequest;)L"+T+";"));i.add(new VarInsnNode(ASTORE,ctx));i.add(new VarInsnNode(ALOAD,target));i.add(new VarInsnNode(ALOAD,ctx));i.add(call("projection","("+V+"L"+T+";)"+V));i.add(new VarInsnNode(ASTORE,target));m.instructions.insert(n,i);prepared++;
            }
            if(n instanceof MethodInsnNode invoke) {
                if(invoke.owner.equals(P+"CombatKiringku")&&invoke.name.equals("hit")) {
                    var store=DragonPatch.next(n);if(!(store instanceof VarInsnNode v)||v.getOpcode()!=ISTORE)throw new IllegalStateException("missing WORLD physical hit store");var i=new InsnList();i.add(new VarInsnNode(ILOAD,v.var));i.add(new VarInsnNode(ALOAD,0));i.add(new VarInsnNode(ALOAD,ctx));i.add(new VarInsnNode(LLOAD,when));i.add(call("hit","(Z"+C+"L"+T+";J)Z"));i.add(new VarInsnNode(ISTORE,v.var));m.instructions.insert(store,i);hits++;
                }
                if(invoke.owner.equals(P+"BattlePhysicalDefense")&&invoke.name.equals("againstNpc")) {
                    var i=new InsnList();i.add(new InsnNode(DUP));i.add(new VarInsnNode(ILOAD,raw));i.add(new VarInsnNode(ILOAD,pre));i.add(new VarInsnNode(ALOAD,critical));i.add(new VarInsnNode(ALOAD,ctx));i.add(call("attempt","(L"+P+"BattlePhysicalDefense$NpcOutcome;IIL"+P+"BattleWeaponCritical$DamageOutcome;L"+T+";)L"+PH+";"));i.add(new VarInsnNode(ASTORE,attempt));m.instructions.insert(n,i);attempts++;
                }
                if(invoke.owner.equals(P+"CombatKiringku")&&invoke.name.equals("finish")) {
                    var i=new InsnList();i.add(new VarInsnNode(ALOAD,attempt));i.add(new VarInsnNode(ALOAD,ctx));m.instructions.insertBefore(n,i);m.instructions.set(n,call("finish",invoke.desc.substring(0,invoke.desc.indexOf(')'))+"L"+PH+";L"+T+";)I"));finish++;
                }
                if(invoke.owner.equals(c.name)&&invoke.name.equals("classicWorldCkewAttackAllowed")) {var i=new InsnList();i.add(new VarInsnNode(ALOAD,ctx));i.add(call("altar","(ZL"+T+";)Z"));m.instructions.insert(n,i);special++;}
            }
            if(n instanceof VarInsnNode store&&store.getOpcode()==ISTORE&&store.var==guardian) {
                var i=new InsnList();i.add(new VarInsnNode(ILOAD,guardian));i.add(new VarInsnNode(ALOAD,ctx));i.add(call("guardian","(ZL"+T+";)Z"));i.add(new VarInsnNode(ISTORE,guardian));m.instructions.insert(n,i);special++;
            }
        }
        // Preserve native range/LOS gates; safety motion returns before projectile debit.
        int debit=DragonPatch.local(m,"projectileDebit");for(var n:m.instructions.toArray())if(n instanceof VarInsnNode store&&store.getOpcode()==ASTORE&&store.var==debit&&DragonPatch.prior(n).getOpcode()==ACONST_NULL) {
            var proceed=new LabelNode();var i=new InsnList();i.add(new VarInsnNode(ALOAD,0));i.add(new VarInsnNode(ALOAD,ctx));i.add(new VarInsnNode(ALOAD,weapon));i.add(new VarInsnNode(ILOAD,ranged));i.add(new FieldInsnNode(GETSTATIC,c.name,"ATTACK_SPELL_SEQUENCE","Ljava/util/concurrent/atomic/AtomicLong;"));i.add(call("motion","("+C+"L"+T+";L"+P+"SourceWeaponPhysicalStats;ZLjava/util/concurrent/atomic/AtomicLong;)Z"));i.add(new JumpInsnNode(IFEQ,proceed));i.add(new InsnNode(RETURN));i.add(proceed);i.add(new FrameNode(F_SAME,0,null,0,null));m.instructions.insertBefore(DragonPatch.prior(n),i);motion++;
        }
        // First use after the wire publication, not the harvest calculation guard.
        boolean wire=false;for(var n:m.instructions.toArray()) {
            if(n instanceof MethodInsnNode v&&v.owner.equals(c.name)&&v.name.equals("sendEncrypted")&&DragonPatch.prior(n) instanceof InvokeDynamicInsnNode message&&Arrays.stream(message.bsmArgs).anyMatch(value->value instanceof String text&&text.startsWith("S_ATTACK classic-world attacker=")))wire=true;
            if(wire&&n instanceof VarInsnNode load&&load.getOpcode()==ILOAD&&load.var==guardian) {
                var proceed=new LabelNode();var i=new InsnList();i.add(new VarInsnNode(ALOAD,0));i.add(new VarInsnNode(ALOAD,ctx));i.add(new VarInsnNode(ILOAD,damage));i.add(call("commit","("+C+"L"+T+";I)Z"));i.add(new JumpInsnNode(IFEQ,proceed));i.add(new InsnNode(RETURN));i.add(proceed);i.add(new FrameNode(F_SAME,0,null,0,null));m.instructions.insertBefore(n,i);commits++;break;
            }
        }
        for(var entry:Map.of("prepare",prepared,"motion",motion,"hit",hits,"attempt",attempts,"finish",finish,"commit",commits).entrySet())CombatPatch.require(entry.getValue(),1,"WORLD owned physical "+entry.getKey());CombatPatch.require(special,2,"WORLD owned physical template-only exclusions");hooks.put("source-world-owned-physical",prepared+motion+hits+attempts+finish+commits+special);
    }
}
