import java.util.*;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;

/** Pinned native combat boundaries for source Dragon Knight calculations. */
final class DragonPatch implements Opcodes {
    static final String P="local/lito381/server/",H=P+"CombatDragon";
    record Locals(int strike,int hit) {}
    static AbstractInsnNode next(AbstractInsnNode n) {
        do {n=n.getNext();}while(n!=null&&n.getOpcode()<0);return n;
    }
    static AbstractInsnNode prior(AbstractInsnNode n) {
        do {n=n.getPrevious();}while(n!=null&&n.getOpcode()<0);return n;
    }
    static Locals strikeLocals(MethodNode method,MethodInsnNode call) {
        if(!(next(call) instanceof VarInsnNode store)||store.getOpcode()!=ASTORE)throw new IllegalStateException("missing Foe strike local");
        int hit=-1;
        for(var n:method.instructions)if(n instanceof MethodInsnNode active&&active.owner.equals(P+"BattleFoeSlayerSequence$Strike")&&active.name.equals("active")
                &&prior(n) instanceof VarInsnNode load&&load.getOpcode()==ALOAD&&load.var==store.var) {
            var a=next(n);var b=a==null?null:next(a);var c=b==null?null:next(b);var d=c==null?null:next(c);var e=d==null?null:next(d);
            if(a instanceof JumpInsnNode first&&first.getOpcode()==IFNE
                    &&b instanceof VarInsnNode chain&&chain.getOpcode()==ILOAD
                    &&c instanceof JumpInsnNode second&&second.getOpcode()==IFEQ
                    &&d instanceof VarInsnNode success&&success.getOpcode()==ILOAD
                    &&e instanceof JumpInsnNode third&&third.getOpcode()==IFEQ
                    &&first.label==second.label&&first.label==third.label) {
                if(hit!=-1)throw new IllegalStateException("ambiguous physical hit operand");hit=success.var;
            }
        }
        if(hit<0)throw new IllegalStateException("missing physical hit provenance: "+method.name);
        return new Locals(store.var,hit);
    }
    static int local(MethodNode method,String name) {
        var slots=new TreeSet<Integer>();
        if(method.localVariables!=null)for(var v:method.localVariables)if(v.name.equals(name))slots.add(v.index);
        if(slots.size()!=1)throw new IllegalStateException("Foe local metadata missing/ambiguous: "+name+" "+slots);
        return slots.first();
    }
    static List<AbstractInsnNode> targetProducer(MethodInsnNode call) {
        var value=prior(call);
        if(value instanceof VarInsnNode local&&local.getOpcode()==LLOAD)return List.of(value);
        if(value instanceof MethodInsnNode getter&&getter.desc.equals("()J")&&Set.of("objectId","playerObjectId").contains(getter.name)
                &&prior(value) instanceof VarInsnNode receiver&&receiver.getOpcode()==ALOAD)return List.of(prior(value),value);
        throw new IllegalStateException("unqualified Foe target ID producer");
    }
    static void apply(ClassNode c,Map<String,Integer> hooks) {
        int strikes=0,multipliers=0,normal=0,zeroDamage=0;
        for(var method:c.methods) {
            var calls=Arrays.stream(method.instructions.toArray()).filter(n->n instanceof MethodInsnNode m
                    &&m.owner.equals(P+"BattleFoeSlayerSequence")&&m.name.equals("next")).map(n->(MethodInsnNode)n).toList();
            if(calls.isEmpty())continue;
            if(calls.size()!=1||!Set.of("attack","attackClassicWorld","attackClassicWorldPlayer").contains(method.name))throw new IllegalStateException("Foe physical caller drift");
            var call=calls.get(0);var locals=strikeLocals(method,call);var target=targetProducer(call);
            var entry=new InsnList();entry.add(new VarInsnNode(ALOAD,0));entry.add(new MethodInsnNode(INVOKESTATIC,H,"normalAttack","(L"+P+"ClientSession;)V",false));
            method.instructions.insert(entry);normal++;
            strikes++;
            int changed=0;
            for(var n:method.instructions.toArray())if(n instanceof MethodInsnNode critical&&critical.owner.equals(P+"BattleWeaponCritical")&&critical.name.equals("apply")
                    &&critical.desc.equals("(L"+P+"SourceWeaponPhysicalStats;IID)L"+P+"BattleWeaponCritical$DamageOutcome;")) {
                // WORLD-player Counter Barrier runs after sequence.next. Consume
                // only here, after that gate, at the shared damage-calculation boundary.
                var multiplier=new InsnList();multiplier.add(new VarInsnNode(ALOAD,locals.strike()));multiplier.add(new VarInsnNode(ALOAD,0));multiplier.add(new VarInsnNode(ILOAD,locals.hit()));
                for(var producer:target)multiplier.add(producer.clone(new HashMap<>()));
                multiplier.add(new MethodInsnNode(INVOKESTATIC,H,"consumeOnHit","(L"+P+"BattleFoeSlayerSequence$Strike;L"+P+"ClientSession;ZJ)L"+P+"BattleFoeSlayerSequence$Strike;",false));
                multiplier.add(new VarInsnNode(ASTORE,locals.strike()));multiplier.add(new VarInsnNode(ALOAD,locals.strike()));
                multiplier.add(new MethodInsnNode(INVOKESTATIC,H,"multiplier","(DL"+P+"BattleFoeSlayerSequence$Strike;)D",false));
                method.instructions.insertBefore(n,multiplier);changed++;multipliers++;
            }
            CombatPatch.require(changed,1,"foe physical multiplier "+method.name);
            int weakness=0;
            for(var n:method.instructions.toArray())if(n instanceof MethodInsnNode attack&&attack.owner.equals(P+"BattleChainswordWeakness")&&attack.name.equals("attack")) {
                if(!attack.desc.equals("(IIJJI)L"+P+"BattleChainswordWeakness$AttackOutcome;"))throw new IllegalStateException("weakness signature drift");
                method.instructions.insertBefore(n,new VarInsnNode(ALOAD,0));
                method.instructions.set(n,new MethodInsnNode(INVOKESTATIC,H,"weaknessAttack","(L"+P+"BattleChainswordWeakness;IIJJIL"+P+"ClientSession;)L"+P+"BattleChainswordWeakness$AttackOutcome;",false));weakness++;
            }
            CombatPatch.require(weakness,1,"source zero damage weakness gate "+method.name);
            zeroDamage+=weakness;
        }
        CombatPatch.require(strikes,3,"foe attack leases");CombatPatch.require(multipliers,3,"foe max multipliers");CombatPatch.require(normal,3,"normal weakness expiration");
        var cast=c.methods.stream().filter(m->m.name.equals("useFoeSlayerSpell")).findFirst().orElseThrow();
        int hp=local(cast,"remainingHp"),mp=local(cast,"remainingMp"),target=local(cast,"target"),eligible=local(cast,"targetStillEligible");
        var acceptedStart=cast.localVariables.stream().filter(v->v.name.equals("targetStillEligible")).findFirst().orElseThrow().start;
        boolean finish=false;int hpReads=0,mpReads=0;
        for(var n:cast.instructions.toArray())if(n instanceof VarInsnNode read&&read.getOpcode()==ILOAD&&(read.var==hp||read.var==mp)) {
            if(cast.instructions.indexOf(n)<cast.instructions.indexOf(acceptedStart))continue; // rejected-resource diagnostic remains unchanged
            if(!finish) {
                if(read.var!=hp)throw new IllegalStateException("unexpected Foe HUD order");
                var extra=new InsnList();extra.add(new VarInsnNode(ALOAD,0));extra.add(new VarInsnNode(ALOAD,target));extra.add(new VarInsnNode(ILOAD,eligible));
                extra.add(new MethodInsnNode(INVOKESTATIC,H,"finishFoe","(L"+P+"ClientSession;Ljava/lang/Object;Z)V",false));
                cast.instructions.insertBefore(n,extra);finish=true;
            }
            var current=new InsnList();current.add(new VarInsnNode(ALOAD,0));current.add(new FieldInsnNode(GETFIELD,c.name,read.var==hp?"playerHp":"playerMp","I"));
            cast.instructions.insertBefore(n,current);cast.instructions.remove(n);if(read.var==hp)hpReads++;else mpReads++;
        }
        if(!finish||hpReads!=3||mpReads!=3)throw new IllegalStateException("Foe HUD binding drift: "+hpReads+"/"+mpReads);
        int logs=0;
        for(var n:cast.instructions)if(n instanceof InvokeDynamicInsnNode concat)for(int i=0;i<concat.bsmArgs.length;i++)
            if(concat.bsmArgs[i] instanceof String text&&text.contains(" exposure-consumed=")) {
                concat.bsmArgs[i]=text.replace(" exposure-consumed="," exposure-preconsumed=");logs++;
            }
        CombatPatch.require(logs,1,"Foe consumption log");
        hooks.put("combat-dragon-strikes",strikes);hooks.put("combat-dragon-multipliers",multipliers);
        hooks.put("combat-dragon-expiry",normal);hooks.put("combat-foe-extra",1);hooks.put("combat-foe-current-vitals",hpReads+mpReads);
        hooks.put("combat-dragon-zero-damage",zeroDamage);
    }
}
