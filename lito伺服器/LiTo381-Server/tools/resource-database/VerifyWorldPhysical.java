import java.util.*;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
import static org.objectweb.asm.Opcodes.*;

/** Exact inverse of WORLD owned views; native arithmetic, wire and ordinary commits compare to formal. */
final class VerifyWorldPhysical {
    static final String P="local/lito381/server/",H=P+"CombatWorldPhysical",T=H+"$Target",C="L"+P+"ClientSession;",V="L"+P+"ClassicWorldMonsterPopulation$Snapshot;",PH=P+"CombatCompanionDamage$Physical";
    static AbstractInsnNode before(AbstractInsnNode n){return VerifyCombatPreservation.prior(n);}
    static AbstractInsnNode after(AbstractInsnNode n){return VerifyCombatPreservation.following(n);}
    static int slot(MethodNode m,String name){return VerifyLineSpells.slot(m,name);}
    static VarInsnNode local(AbstractInsnNode n,int opcode){return VerifyLineSpells.local(n,opcode);}
    static void erase(MethodNode m,AbstractInsnNode... nodes){VerifyLineSpells.erase(m,nodes);}
    static void require(boolean good,String message){if(!good)throw new AssertionError("WORLD owned "+message);}
    static MethodNode normalize(MethodNode source) {
        var m=new MethodNode(source.access,source.name,source.desc,source.signature,source.exceptions.toArray(String[]::new));source.accept(m);
        if(!Arrays.stream(m.instructions.toArray()).anyMatch(n->n instanceof MethodInsnNode v&&v.owner.equals(H)))return m;
        require(m.name.equals("attackClassicWorld")&&m.desc.equals("(L"+P+"Protocol$AttackRequest;L"+P+"ClientSession$AttackEnvelope;)V"),"unexpected actor entry");
        int ctx=-1,attempt=-1,target=slot(m,"target"),guardian=slot(m,"guardianHarvest"),weapon=slot(m,"weapon");var counts=new TreeMap<String,Integer>();
        for(var n:m.instructions.toArray())if(m.instructions.contains(n)&&n instanceof MethodInsnNode call&&call.owner.equals(H)) {
            counts.merge(call.name,1,Integer::sum);
            switch(call.name) {
                case "prepare" -> {
                    var request=before(call);var caster=before(request);var original=before(caster);var store=after(call);ctx=local(store,ASTORE).var;
                    require(call.desc.equals("("+V+C+"L"+P+"Protocol$AttackRequest;)L"+T+";")&&local(original,ALOAD).var==target&&local(caster,ALOAD).var==0&&local(request,ALOAD).var==1&&local(before(original),ASTORE).var==target,"prepare identity and native projection store");
                    var load=after(store);var context=after(load);var projection=after(context);var finish=after(projection);
                    require(local(load,ALOAD).var==target&&local(context,ALOAD).var==ctx&&projection instanceof MethodInsnNode p&&p.owner.equals(H)&&p.name.equals("projection")&&p.desc.equals("("+V+"L"+T+";)"+V)&&local(finish,ASTORE).var==target,"stack-only projection");counts.merge("projection",1,Integer::sum);erase(m,original,caster,request,call,store,load,context,projection,finish);
                }
                case "motion" -> {
                    var sequence=before(call);var ranged=before(sequence);var equipped=before(ranged);var context=before(equipped);var caster=before(context);var branch=after(call);var ret=after(branch);
                    require(call.desc.equals("("+C+"L"+T+";L"+P+"SourceWeaponPhysicalStats;ZLjava/util/concurrent/atomic/AtomicLong;)Z")&&sequence instanceof FieldInsnNode f&&f.getOpcode()==GETSTATIC&&f.owner.equals(P+"ClientSession")&&f.name.equals("ATTACK_SPELL_SEQUENCE")&&f.desc.equals("Ljava/util/concurrent/atomic/AtomicLong;")&&local(ranged,ILOAD).var==slot(m,"ranged")&&local(equipped,ALOAD).var==weapon&&local(context,ALOAD).var==ctx&&local(caster,ALOAD).var==0&&after(ret).getOpcode()==ACONST_NULL&&local(after(after(ret)),ASTORE).var==slot(m,"projectileDebit"),"safety before native projectile debit");require(branch instanceof JumpInsnNode j&&j.getOpcode()==IFEQ&&ret.getOpcode()==RETURN&&ret.getNext()==j.label,"motion conditional return");erase(m,caster,context,equipped,ranged,sequence,call,branch,ret);
                }
                case "hit" -> {
                    var when=before(call);var context=before(when);var caster=before(context);var value=before(caster);var store=after(call);var nativeStore=before(value);var nativeCall=before(nativeStore);
                    require(call.desc.equals("(Z"+C+"L"+T+";J)Z")&&local(when,LLOAD).var==slot(m,"physicalAttackNowNanos")&&local(context,ALOAD).var==ctx&&local(caster,ALOAD).var==0&&local(value,ILOAD).var==local(store,ISTORE).var&&local(nativeStore,ISTORE).var==local(store,ISTORE).var&&nativeCall instanceof MethodInsnNode v&&v.owner.equals(P+"CombatKiringku")&&v.name.equals("hit"),"protection after native hit result");erase(m,value,caster,context,when,call,store);
                }
                case "attempt" -> {
                    var context=before(call);var critical=before(context);var pre=before(critical);var raw=before(pre);var dup=before(raw);var original=before(dup);var store=after(call);attempt=local(store,ASTORE).var;
                    require(call.desc.equals("(L"+P+"BattlePhysicalDefense$NpcOutcome;IIL"+P+"BattleWeaponCritical$DamageOutcome;L"+T+";)L"+PH+";")&&local(context,ALOAD).var==ctx&&local(critical,ALOAD).var==slot(m,"critical")&&local(pre,ILOAD).var==slot(m,"preCritical")&&local(raw,ILOAD).var==slot(m,"beforeDefense")&&dup.getOpcode()==DUP&&original instanceof MethodInsnNode v&&v.owner.equals(P+"BattlePhysicalDefense")&&v.name.equals("againstNpc"),"original defense/critical provenance");erase(m,dup,raw,pre,critical,context,call,store);
                }
                case "finish" -> {
                    var context=before(call);var prepared=before(context);String original="(I"+C+C+"L"+P+"SourceWeaponPhysicalStats;L"+P+"BattleWeaponCritical$DamageOutcome;L"+P+"CombatKiringku$Defense;ZZ)I";
                    require(call.desc.equals(original.substring(0,original.indexOf(')'))+"L"+PH+";L"+T+";)I")&&local(context,ALOAD).var==ctx&&local(prepared,ALOAD).var==attempt,"source finish context");erase(m,prepared,context);m.instructions.set(call,new MethodInsnNode(INVOKESTATIC,P+"CombatKiringku","finish",original,false));
                }
                case "altar" -> {
                    var context=before(call);var original=before(context);require(call.desc.equals("(ZL"+T+";)Z")&&local(context,ALOAD).var==ctx&&original instanceof MethodInsnNode v&&v.owner.equals(P+"ClientSession")&&v.name.equals("classicWorldCkewAttackAllowed"),"ordinary altar predicate preserved");erase(m,context,call);
                }
                case "guardian" -> {
                    var context=before(call);var load=before(context);var original=before(load);var store=after(call);require(call.desc.equals("(ZL"+T+";)Z")&&local(context,ALOAD).var==ctx&&local(load,ILOAD).var==guardian&&local(original,ISTORE).var==guardian&&local(store,ISTORE).var==guardian,"ordinary harvest predicate preserved");erase(m,load,context,call,store);
                }
                case "commit" -> {
                    var damage=before(call);var context=before(damage);var caster=before(context);var branch=after(call);var ret=after(branch);
                    require(call.desc.equals("("+C+"L"+T+";I)Z")&&local(damage,ILOAD).var==slot(m,"damage")&&local(context,ALOAD).var==ctx&&local(caster,ALOAD).var==0&&branch instanceof JumpInsnNode j&&j.getOpcode()==IFEQ&&ret.getOpcode()==RETURN&&ret.getNext()==j.label&&local(after(ret),ILOAD).var==guardian,"commit before original post-wire harvest return");
                    boolean wire=false;for(var v=m.instructions.getFirst();v!=caster;v=v.getNext())if(v instanceof MethodInsnNode send&&send.owner.equals(P+"ClientSession")&&send.name.equals("sendEncrypted")&&before(send) instanceof InvokeDynamicInsnNode log&&Arrays.stream(log.bsmArgs).anyMatch(value->value instanceof String text&&text.startsWith("S_ATTACK classic-world attacker=")))wire=true;require(wire,"native attack wire must precede owned commit");erase(m,caster,context,damage,call,branch,ret);
                }
                default -> throw new AssertionError("unexpected WORLD owned hook="+call.name);
            }
        }
        var expected=new TreeMap<String,Integer>();for(var name:List.of("prepare","projection","motion","hit","attempt","finish","altar","guardian","commit"))expected.put(name,1);require(counts.equals(expected),"hook counts="+counts);
        var slots=new HashSet<>(Set.of(ctx,attempt));for(int i=0;i<2;i++){var first=m.instructions.getFirst();while(first.getOpcode()<0)first=first.getNext();var store=after(first);require(first.getOpcode()==ACONST_NULL&&slots.remove(local(store,ASTORE).var),"method-local initialization");erase(m,first,store);}require(slots.isEmpty(),"local initialization removal");return m;
    }
}
