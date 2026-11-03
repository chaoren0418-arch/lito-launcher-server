import java.util.*;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
import org.objectweb.asm.tree.analysis.*;

final class MagicDamagePatch implements Opcodes {
    static final String P="local/lito381/server/",H=P+"CombatMagicDamage",A=H+"$Attempt",C=P+"ClientSession";
    static final String ORDINARY="(IIIIIILjava/lang/String;)I",PRE_MR="(IIIIIILjava/lang/String;)L"+A+";";
    static final Set<String> SINGLE=Set.of("useAttackSpell","useClassicWorldAttackSpell","useFreezeAttackSpell","useClassicWorldFreezeAttackSpell","useShockStunSpell","useClassicWorldShockStunSpell","nativeMagicDollTriggeredSkillDamage");
    static final Map<String,Integer> MIRRORS=Map.ofEntries(
        Map.entry("useLineAttackSpell",1),Map.entry("useAreaAttackSpell",1),Map.entry("useCenteredAreaAttackSpell",1),Map.entry("useCenteredFreezeAttackSpell",1),
        Map.entry("useFreezeAttackSpell",1),Map.entry("useClassicWorldFreezeAttackSpell",1),Map.entry("useAttackSpell",3),Map.entry("useClassicWorldAttackSpell",3),
        Map.entry("useClassicWorldLineAttackSpell",1),Map.entry("useClassicWorldLineFreezeAttackSpell",1),Map.entry("useClassicWorldCenteredFreezeAttackSpell",1),
        Map.entry("useClassicWorldCenteredAreaAttackSpell",1),Map.entry("useClassicWorldAreaAttackSpell",1));
    static AbstractInsnNode prior(AbstractInsnNode n){do{n=n.getPrevious();}while(n!=null&&n.getOpcode()<0);return n;}
    static VarInsnNode load(SourceValue value,int opcode) {
        if(value.insns.size()!=1||!(value.insns.iterator().next() instanceof VarInsnNode v)||v.getOpcode()!=opcode)throw new IllegalStateException("ambiguous magic value producer");return v;
    }
    static int appendLocal(ClassNode c,MethodNode m) {
        return appendLocal(c,m,A);
    }
    static int appendLocal(ClassNode c,MethodNode m,String type) {
        int slot=m.maxLocals;var locals=new ArrayList<Object>();locals.add(c.name);
        for(var t:Type.getArgumentTypes(m.desc))locals.add(t.getSort()==Type.OBJECT?t.getInternalName():t.getSort()==Type.ARRAY?t.getDescriptor():t.getSort()==Type.LONG?LONG:t.getSort()==Type.DOUBLE?DOUBLE:t.getSort()==Type.FLOAT?FLOAT:INTEGER);
        for(var n:m.instructions)if(n instanceof FrameNode f) {
            switch(f.type){case F_FULL,F_NEW->locals=new ArrayList<>(f.local);case F_APPEND->locals.addAll(f.local);case F_CHOP->{for(int i=0;i<f.local.size();i++)locals.remove(locals.size()-1);}case F_SAME,F_SAME1->{}default->throw new IllegalStateException("unexpected magic frame");}
            var padded=new ArrayList<Object>(locals);int width=0;for(var v:locals)width+=v.equals(LONG)||v.equals(DOUBLE)?2:1;
            if(width>slot)throw new IllegalStateException("magic local escaped method");while(width++<slot)padded.add(TOP);padded.add(type);f.type=F_FULL;f.local=padded;f.stack=f.stack==null?new ArrayList<>():new ArrayList<>(f.stack);
        }
        var init=new InsnList();init.add(new InsnNode(ACONST_NULL));init.add(new VarInsnNode(ASTORE,slot));m.instructions.insert(init);m.maxLocals++;return slot;
    }
    static void capture(MethodNode m,MethodInsnNode call,int slot,boolean burn) {
        var i=new InsnList();i.add(new InsnNode(DUP));i.add(new VarInsnNode(ASTORE,slot));i.add(new MethodInsnNode(INVOKEVIRTUAL,A,burn?"finalBurn":"damage",burn?"()L"+P+"BattleFinalBurn$Result;":"()I",false));m.instructions.insert(call,i);
    }
    static void finish(InsnList i,int slot,int critical) {
        i.add(new VarInsnNode(ALOAD,slot));i.add(new VarInsnNode(ALOAD,0));i.add(critical<0?new InsnNode(ICONST_0):new VarInsnNode(ILOAD,critical));i.add(new MethodInsnNode(INVOKESTATIC,H,"finish","(IL"+A+";L"+C+";Z)I",false));
    }
    static void cloneBeforeMr(ClassNode c) {
        var source=CombatPatch.method(c,"ordinaryMagicDamage",ORDINARY);var m=new MethodNode(source.access,"sourceMagicDamageBeforeResistance",PRE_MR,null,source.exceptions.toArray(String[]::new));source.accept(m);m.name="sourceMagicDamageBeforeResistance";m.desc=PRE_MR;
        int calls=0,returns=0;
        for(var n:m.instructions.toArray()) {
            if(n instanceof MethodInsnNode call&&call.owner.equals(P+"BattleMagicResistance")&&call.name.equals("damageAfterResistance")){m.instructions.set(call,new MethodInsnNode(INVOKESTATIC,H,"deferMr","(III)I",false));calls++;}
            if(n.getOpcode()==IRETURN){var i=new InsnList();i.add(new VarInsnNode(ILOAD,5));i.add(new VarInsnNode(ALOAD,0));i.add(new MethodInsnNode(INVOKEVIRTUAL,C,"originalMagicHit","()I",false));i.add(new MethodInsnNode(INVOKESTATIC,H,"attempt","(III)L"+A+";",false));m.instructions.insertBefore(n,i);m.instructions.set(n,new InsnNode(ARETURN));returns++;}
        }
        CombatPatch.require(calls,1,"source magic deferred MR clone");CombatPatch.require(returns,1,"source magic attempt return");c.methods.add(m);
    }
    static void apply(ClassNode c,Map<String,Integer> hooks)throws AnalyzerException {
        int prepared=0,mirrors=0,criticals=0,directs=0,mindReductions=0,mindSkips=0;var seen=new TreeMap<String,Integer>();
        for(var m:new ArrayList<>(c.methods)) {
            if(!MIRRORS.containsKey(m.name)&&!SINGLE.contains(m.name))continue;
            var nodes=m.instructions.toArray();if(Arrays.stream(nodes).noneMatch(v->v instanceof MethodInsnNode x&&x.owner.equals(C)&&Set.of("ordinaryMagicDamage","lineSpellDamage","areaSpellDamage").contains(x.name)))continue;var frames=new Analyzer<>(new SourceInterpreter()).analyze(c.name,m);var index=new IdentityHashMap<AbstractInsnNode,Integer>();for(int n=0;n<nodes.length;n++)index.put(nodes[n],n);
            int pc=-1,critical=-1,mind=-1,mainDamage=-1;
            for(int n=0;n<nodes.length;n++)if(nodes[n] instanceof MethodInsnNode call) {
                if(call.owner.equals(C)&&call.name.equals("counterMirrorDecision")) {var f=frames[n];int receiver=load(f.getStack(f.getStackSize()-5),ALOAD).var;if(pc!=-1&&pc!=receiver)throw new IllegalStateException("magic target identity changed");pc=receiver;}
                if(call.owner.equals(P+"BattleSpellCatalog$AttackSpell")&&call.name.equals("damageAfterSpecialCritical")){critical=load(frames[n].getStack(frames[n].getStackSize()-1),ILOAD).var;mainDamage=load(frames[n].getStack(frames[n].getStackSize()-2),ILOAD).var;}
                if(call.owner.equals(P+"BattleSpellCatalog$MindBreakSpell")&&call.name.equals("damageAfterMultiplier"))mind=load(frames[n].getStack(frames[n].getStackSize()-2),ALOAD).var;
            }
            boolean single=SINGLE.contains(m.name);
            if(single&&pc<0) {
                if(m.name.equals("nativeMagicDollTriggeredSkillDamage"))pc=2;
                else for(int n=0;n<nodes.length;n++)if(nodes[n] instanceof MethodInsnNode call&&call.owner.equals(C)&&call.name.equals("combatDamageAfterCooking")&&call.desc.equals("(ILjava/lang/String;)I"))pc=load(frames[n].getStack(frames[n].getStackSize()-3),ALOAD).var;
                if(pc<0)throw new IllegalStateException("missing source magic PC context "+m.name);
            }
            int slot=appendLocal(c,m),casts=0,ordinal=0;
            for(int n=0;n<nodes.length;n++)if(nodes[n] instanceof MethodInsnNode call) {
                boolean ordinary=call.owner.equals(C)&&Set.of("ordinaryMagicDamage","lineSpellDamage","areaSpellDamage").contains(call.name);
                boolean burn=call.owner.equals(P+"BattleFinalBurn")&&call.name.equals("resolve");
                if(ordinary||burn) {
                    int target=pc;
                    if(!single) {
                        var f=frames[n];var armor=f.getStack(f.getStackSize()-2);
                        if(armor.insns.size()!=1||!(armor.insns.iterator().next() instanceof MethodInsnNode getter)||!getter.owner.equals(C)||!getter.name.equals("effectiveArmorMagicReductionBase"))continue;
                        target=load(frames[index.get(getter)].getStack(frames[index.get(getter)].getStackSize()-1),ALOAD).var;
                        if(target!=pc)throw new IllegalStateException("spell calculation/mirror target mismatch");
                    }
                    var args=new InsnList();args.add(new VarInsnNode(ALOAD,target));m.instructions.insertBefore(call,args);
                    String name=burn?"finalBurn":call.name.equals("ordinaryMagicDamage")?"ordinary":call.name.equals("lineSpellDamage")?"line":"area";
                    String desc=call.desc.substring(1,call.desc.indexOf(')'));if(!burn)desc="L"+C+";"+desc;desc="("+desc+"L"+C+";)L"+A+";";
                    var bridge=new MethodInsnNode(INVOKESTATIC,H,name,desc,false);m.instructions.set(call,bridge);capture(m,bridge,slot,burn);prepared++;casts++;
                }
                if(call.owner.equals(P+"BattleSpellCatalog$AttackSpell")&&call.name.equals("damageAfterSpecialCritical")) {
                    m.instructions.insertBefore(call,new VarInsnNode(ALOAD,pc));m.instructions.set(call,new MethodInsnNode(INVOKESTATIC,H,"beforeCritical","(L"+P+"BattleSpellCatalog$AttackSpell;IZL"+C+";)I",false));criticals++;
                }
                if(call.owner.equals(C)&&call.name.equals("counterMirrorDecision")) {
                    ordinal++;var f=frames[n];var damage=load(f.getStack(f.getStackSize()-4),ILOAD);int value=damage.var;
                    if(critical>=0&&ordinal==2) {
                        var reduce=new InsnList();reduce.add(new VarInsnNode(ALOAD,pc));reduce.add(new MethodInsnNode(INVOKESTATIC,H,"reduceMindBreak","(IL"+C+";)I",false));reduce.add(new InsnNode(DUP));reduce.add(new VarInsnNode(ISTORE,value));m.instructions.insert(damage,reduce);mindReductions++;
                    }
                    var i=new InsnList();i.add(new VarInsnNode(ILOAD,value));finish(i,slot,critical);i.add(new VarInsnNode(ISTORE,value));m.instructions.insert(call,i);mirrors++;
                }
                if(critical>=0&&call.owner.equals(C)&&call.name.equals("combatDamageAfterCooking")&&call.desc.equals("(ILjava/lang/String;)I")) {
                    // The first bone/arm calculation is separate; only skip the later shared reduction.
                    if(nodes[n+1] instanceof VarInsnNode store&&store.getOpcode()==ISTORE) {
                        if(store.var==mainDamage){if(mind<0)throw new IllegalStateException("missing Mind Break context");m.instructions.insertBefore(call,new VarInsnNode(ALOAD,mind));m.instructions.set(call,new MethodInsnNode(INVOKESTATIC,H,"cookingAfterMode","(L"+C+";ILjava/lang/String;L"+P+"BattleSpellCatalog$MindBreakSpell;)I",false));mindSkips++;}
                    }
                }
                if(m.name.contains("ShockStunSpell")&&call.owner.equals(C)&&call.name.equals("earthBindProtectedCombatDamage")&&call.desc.equals("(ILjava/lang/String;)I")) {
                    var i=new InsnList();i.add(new VarInsnNode(ALOAD,slot));i.add(new VarInsnNode(ALOAD,0));i.add(new VarInsnNode(ALOAD,pc));i.add(new InsnNode(ICONST_0));i.add(new MethodInsnNode(INVOKESTATIC,H,"direct","(IL"+A+";L"+C+";L"+C+";Z)I",false));m.instructions.insert(call,i);directs++;
                }
            }
            if(m.name.equals("nativeMagicDollTriggeredSkillDamage"))for(var node:m.instructions.toArray())if(node.getOpcode()==IRETURN&&prior(node) instanceof MethodInsnNode getter&&getter.owner.equals(A)&&getter.name.equals("damage")){var i=new InsnList();i.add(new VarInsnNode(ALOAD,slot));i.add(new VarInsnNode(ALOAD,0));i.add(new VarInsnNode(ALOAD,pc));i.add(new InsnNode(ICONST_1));i.add(new MethodInsnNode(INVOKESTATIC,H,"direct","(IL"+A+";L"+C+";L"+C+";Z)I",false));m.instructions.insertBefore(node,i);directs++;}
            CombatPatch.require(casts,critical>=0?5:1,"source magic preparation "+m.name);if(ordinal>0)seen.put(m.name,ordinal);
        }
        if(!seen.equals(new TreeMap<>(MIRRORS)))throw new IllegalStateException("source magic mirror paths drift: "+seen);
        CombatPatch.require(prepared,24,"source magic pre-MR attempts");CombatPatch.require(mirrors,17,"source magic post-mirror commits");CombatPatch.require(criticals,2,"source deferred Disintegrate");CombatPatch.require(directs,3,"source direct shock/doll magic");CombatPatch.require(mindReductions,2,"source pre-mirror Mind Break reduction");CombatPatch.require(mindSkips,2,"source Mind Break no double reduction");
        cloneBeforeMr(c);hooks.put("source-magic-attempts",prepared);hooks.put("source-magic-mirrors",mirrors);hooks.put("source-magic-critical",criticals);hooks.put("source-magic-direct",directs);hooks.put("source-magic-mind-order",mindReductions+mindSkips);
    }
}
