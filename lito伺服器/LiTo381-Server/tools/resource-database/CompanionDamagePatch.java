import java.util.*;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
import org.objectweb.asm.tree.analysis.*;

final class CompanionDamagePatch implements Opcodes {
    static final String P="local/lito381/server/",H=P+"CombatCompanionDamage",M=P+"CombatMagicDamage",A=M+"$Attempt",C=P+"ClientSession",B=P+"BattleCompanion",PH=H+"$Physical";
    static final Set<String> COMMON=Set.of("useAttackSpell","useClassicWorldAttackSpell","useFreezeAttackSpell","useClassicWorldFreezeAttackSpell","useClassicWorldShockStunSpell");
    static int context(SourceValue receiver,Frame<SourceValue>[] frames,IdentityHashMap<AbstractInsnNode,Integer> index) {
        if(receiver.insns.size()!=1)throw new IllegalStateException("ambiguous live companion receiver");var n=receiver.insns.iterator().next();
        if(n instanceof VarInsnNode v&&v.getOpcode()==ALOAD)return v.var;
        if(n instanceof MethodInsnNode getter&&getter.name.equals("controlled")&&getter.desc.equals("()L"+B+";")) {
            var f=frames[index.get(n)];return MagicDamagePatch.load(f.getStack(f.getStackSize()-1),ALOAD).var;
        }
        throw new IllegalStateException("unsupported companion context producer");
    }
    static MethodInsnNode mrProducer(SourceValue value,Frame<SourceValue>[] frames,IdentityHashMap<AbstractInsnNode,Integer> index,Set<AbstractInsnNode> seen) {
        if(value==null||value.insns.size()!=1)return null;var n=value.insns.iterator().next();if(!seen.add(n))return null;
        if(n instanceof MethodInsnNode call&&call.owner.equals(B)&&call.name.equals("effectiveMagicResistance"))return call;
        if(n instanceof VarInsnNode v) {
            var f=frames[index.get(n)];if(f==null)return null;
            if(v.getOpcode()==ILOAD)return mrProducer(f.getLocal(v.var),frames,index,seen);
            if(v.getOpcode()==ISTORE)return mrProducer(f.getStack(f.getStackSize()-1),frames,index,seen);
        }
        return null;
    }
    static int singleContext(AbstractInsnNode[] nodes,Frame<SourceValue>[] frames,IdentityHashMap<AbstractInsnNode,Integer> index,boolean physical) {
        var found=new HashSet<Integer>();for(int n=0;n<nodes.length;n++)if(nodes[n] instanceof MethodInsnNode call&&call.owner.equals(B)
                &&call.name.equals(physical?"earthBindActive":"effectiveMagicResistance")) {
            var f=frames[n];found.add(context(f.getStack(f.getStackSize()-Type.getArgumentTypes(call.desc).length-1),frames,index));
        }
        if(found.size()!=1)throw new IllegalStateException("missing/ambiguous companion context: "+found);return found.iterator().next();
    }
    static void apply(ClassNode c,Map<String,Integer> hooks)throws AnalyzerException {
        int common=0,finals=0,critical=0,plain=0,physical=0,physicalAttempt=0;var paths=new TreeMap<String,Integer>();
        for(var m:c.methods) {
            boolean attack=m.name.equals("attack");if(!attack&&!MagicDamagePatch.MIRRORS.containsKey(m.name)&&!COMMON.contains(m.name))continue;
            var nodes=m.instructions.toArray();boolean hasCommon=COMMON.contains(m.name)&&Arrays.stream(nodes).anyMatch(v->v instanceof MethodInsnNode call&&call.owner.equals(M)&&Set.of("ordinary","finalBurn").contains(call.name));
            boolean hasPhysical=attack&&Arrays.stream(nodes).anyMatch(v->v instanceof MethodInsnNode call&&call.owner.equals(P+"CombatKiringku")&&call.name.equals("finish"));
            boolean hasPlain=Arrays.stream(nodes).anyMatch(v->v instanceof MethodInsnNode call&&call.owner.equals(C)&&Set.of("ordinaryMagicDamage","areaSpellDamage","lineSpellDamage").contains(call.name));
            if(!hasCommon&&!hasPhysical&&!hasPlain)continue;
            var frames=new Analyzer<>(new SourceInterpreter()).analyze(c.name,m);var index=new IdentityHashMap<AbstractInsnNode,Integer>();for(int n=0;n<nodes.length;n++)index.put(nodes[n],n);
            int ctx=-1;try{if(hasCommon||hasPhysical)ctx=singleContext(nodes,frames,index,hasPhysical);}catch(IllegalStateException e){throw new IllegalStateException(m.name+m.desc+": "+e.getMessage(),e);}int attemptSlot=-1;
            if(hasCommon)for(var n:nodes)if(n instanceof MethodInsnNode call&&call.owner.equals(M)&&Set.of("ordinary","finalBurn").contains(call.name)) {
                var dup=DragonPatch.next(call);var store=DragonPatch.next(dup);if(dup.getOpcode()!=DUP||!(store instanceof VarInsnNode s)||s.getOpcode()!=ASTORE)throw new IllegalStateException("missing companion magic attempt local");if(attemptSlot!=-1&&attemptSlot!=s.var)throw new IllegalStateException("companion magic attempt identity drift");attemptSlot=s.var;
            }
            int physicalSlot=hasPhysical?MagicDamagePatch.appendLocal(c,m,PH):-1,preCrit=-1,criticalSlot=-1;
            if(hasPhysical)for(int n=0;n<nodes.length;n++)if(nodes[n] instanceof MethodInsnNode call&&call.owner.equals(P+"BattleWeaponCritical")&&call.name.equals("apply")) {
                var f=frames[n];preCrit=MagicDamagePatch.load(f.getStack(f.getStackSize()-3),ILOAD).var;criticalSlot=((VarInsnNode)DragonPatch.next(call)).var;
            }
            for(int n=0;n<nodes.length;n++)if(nodes[n] instanceof MethodInsnNode call) {
                if(hasCommon&&call.owner.equals(M)&&Set.of("ordinary","finalBurn").contains(call.name)) {
                    var args=new InsnList();args.add(new VarInsnNode(ALOAD,ctx));args.add(new InsnNode(Set.of("useAttackSpell","useClassicWorldAttackSpell").contains(m.name)?ICONST_0:ICONST_1));m.instructions.insertBefore(call,args);
                    String desc=call.desc.substring(0,call.desc.indexOf(')'))+"Ljava/lang/Object;Z)L"+A+";";m.instructions.set(call,new MethodInsnNode(INVOKESTATIC,H,call.name,desc,false));
                    if(call.name.equals("finalBurn"))finals++;else common++;
                }
                if(hasCommon&&call.owner.equals(M)&&call.name.equals("beforeCritical")) {
                    m.instructions.insertBefore(call,new VarInsnNode(ALOAD,attemptSlot));m.instructions.set(call,new MethodInsnNode(INVOKESTATIC,H,"beforeCritical",call.desc.substring(0,call.desc.indexOf(')'))+"L"+A+";)I",false));critical++;
                }
                if(call.owner.equals(C)&&Set.of("ordinaryMagicDamage","areaSpellDamage","lineSpellDamage").contains(call.name)) {
                    var f=frames[n];var producer=mrProducer(f.getStack(f.getStackSize()-3),frames,index,new HashSet<>());if(producer==null)continue;
                    var pf=frames[index.get(producer)];int context=context(pf.getStack(pf.getStackSize()-Type.getArgumentTypes(producer.desc).length-1),frames,index);
                    m.instructions.insertBefore(call,new VarInsnNode(ALOAD,context));String name=call.name.equals("ordinaryMagicDamage")?"ordinaryComplete":call.name.equals("lineSpellDamage")?"line":"area";
                    m.instructions.set(call,new MethodInsnNode(INVOKESTATIC,H,name,"(L"+C+";"+call.desc.substring(1,call.desc.indexOf(')'))+"Ljava/lang/Object;)I",false));plain++;paths.merge(m.name,1,Integer::sum);
                }
                if(hasPhysical&&call.owner.equals(P+"BattlePhysicalDefense")&&call.name.equals("againstNpc")) {
                    int raw=MagicDamagePatch.load(frames[n].getStack(frames[n].getStackSize()-5),ILOAD).var;var i=new InsnList();i.add(new InsnNode(DUP));i.add(new VarInsnNode(ILOAD,raw));i.add(new VarInsnNode(ILOAD,preCrit));i.add(new VarInsnNode(ALOAD,criticalSlot));i.add(new VarInsnNode(ALOAD,ctx));i.add(new MethodInsnNode(INVOKESTATIC,H,"physicalAttempt","(L"+P+"BattlePhysicalDefense$NpcOutcome;IIL"+P+"BattleWeaponCritical$DamageOutcome;Ljava/lang/Object;)L"+PH+";",false));i.add(new VarInsnNode(ASTORE,physicalSlot));m.instructions.insert(call,i);physicalAttempt++;
                }
                if(hasPhysical&&call.owner.equals(P+"CombatKiringku")&&call.name.equals("finish")) {
                    var i=new InsnList();i.add(new VarInsnNode(ALOAD,physicalSlot));i.add(new VarInsnNode(ALOAD,ctx));m.instructions.insertBefore(call,i);
                    m.instructions.set(call,new MethodInsnNode(INVOKESTATIC,H,"physical",call.desc.substring(0,call.desc.indexOf(')'))+"L"+PH+";Ljava/lang/Object;)I",false));physical++;
                }
            }
        }
        CombatPatch.require(common,11,"source owned companion targeted magic preparation");CombatPatch.require(finals,2,"source companion Final Burn");CombatPatch.require(critical,2,"source companion critical/MR order");CombatPatch.require(physical,1,"source companion physical finish");CombatPatch.require(physicalAttempt,1,"source companion original NPC defense");
        CombatPatch.require(plain,7,"source companion area/line spell paths");
        if(!paths.equals(new TreeMap<>(Map.of("useAreaAttackSpell",1,"useCenteredAreaAttackSpell",1,"useClassicWorldAreaAttackSpell",1,"useClassicWorldCenteredAreaAttackSpell",1,"useClassicWorldCenteredFreezeAttackSpell",1,"useClassicWorldLineAttackSpell",1,"useClassicWorldLineFreezeAttackSpell",1))))throw new IllegalStateException("companion area/line callers changed: "+paths);
        hooks.put("source-companion-target-magic",common+finals);hooks.put("source-companion-critical",critical);hooks.put("source-companion-area-magic",plain);hooks.put("source-companion-physical",physical+physicalAttempt);
    }
}
