import java.nio.file.*;
import java.util.*;
import java.util.zip.*;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
import org.objectweb.asm.tree.analysis.*;

/** Read-only operand provenance for the pinned core's negative-skill call sites. */
public class ProbeProbabilityTargets {
    static final String P="local/lito381/server/";
    static boolean probability(MethodInsnNode x) {
        return x.owner.startsWith(P+"BattleSpellCatalog$") &&
                Set.of("probability","probabilityForTarget","triggerProbability","successChancePercent","freezeProbability").contains(x.name);
    }
    static boolean targetOwner(String owner) {
        return owner.equals(P+"ClientSession") || owner.startsWith(P+"ClientSession$")&&owner.endsWith("Target")
                || Set.of(P+"SourceNpcPhysicalStats",P+"ClassicWorldMonsterPopulation$Snapshot",
                    P+"BattleCompanion$Snapshot",P+"BattleQuestEncounter$Snapshot",P+"BattleTimeAttackEncounter$Snapshot").contains(owner);
    }
    static Map<Integer,String> confusionLocals(MethodNode method,AbstractInsnNode at) throws Exception {
        var result=new TreeMap<Integer,String>();int position=method.instructions.indexOf(at);
        for(var local:method.localVariables==null?List.<LocalVariableNode>of():method.localVariables) {
            if(!Set.of("target","targetMonster","targetTimeBoss","companionTarget","npcStats",
                    "playerTarget","monsterTarget","controlledCompanion").contains(local.name))continue;
            if(method.instructions.indexOf(local.start)>position||method.instructions.indexOf(local.end)<=position)continue;
            Type type=Type.getType(local.desc);
            if(type.getSort()!=Type.OBJECT||!targetOwner(type.getInternalName())&&!type.getInternalName().equals(P+"BattleCompanion"))
                throw new IllegalStateException("unexpected confusion local "+local.name+local.desc);
            result.put(local.index,local.name+":"+type.getInternalName());
        }
        // The pinned repair020 battle method has no local-variable metadata.
        // Recover only target receivers that were already used before this gate.
        if(result.isEmpty()&&method.name.equals("useAttackSpell")) {
            for(var node:method.instructions)if(node instanceof MethodInsnNode call
                    &&call.owner.equals(P+"BattleSpellCatalog$ConfusionSpell")&&call.name.equals("succeeds")) {
                position=method.instructions.indexOf(node);break;
            }
            var frames=new Analyzer<>(new SourceInterpreter()).analyze(P+"ClientSession",method);
            var types=new Analyzer<>(new BasicInterpreter()).analyze(P+"ClientSession",method);
            var trace=new Trace(method,frames);var nodes=method.instructions.toArray();
            for(int i=0;i<position;i++) {
                if(frames[i]==null)continue;
                if(nodes[i] instanceof MethodInsnNode call&&call.getOpcode()!=Opcodes.INVOKESTATIC&&targetOwner(call.owner)) {
                    int receiver=frames[i].getStackSize()-Type.getArgumentTypes(call.desc).length-1;
                    trace.object(frames[i].getStack(receiver),call.owner,0);
                } else if(nodes[i] instanceof FieldInsnNode field&&field.getOpcode()==Opcodes.GETFIELD&&targetOwner(field.owner))
                    trace.object(frames[i].getStack(frames[i].getStackSize()-1),field.owner,0);
            }
            for(var entry:trace.targets.entrySet())if(entry.getKey()<types[position].getLocals()
                    &&types[position].getLocal(entry.getKey()).isReference())result.put(entry.getKey(),entry.getValue().toString());
            if(!result.keySet().equals(Set.of(13,15,16,18,24)))throw new IllegalStateException("pinned confusion target provenance drift: "+result);
        }
        return result;
    }
    static class Trace {
        AbstractInsnNode[] nodes;Map<AbstractInsnNode,Integer> indexes=new IdentityHashMap<>();
        Frame<SourceValue>[] frames;Map<Integer,Set<String>> targets=new TreeMap<>();Set<String> visited=new HashSet<>();
        Trace(MethodNode m,Frame<SourceValue>[] f) {nodes=m.instructions.toArray();frames=f;for(int i=0;i<nodes.length;i++)indexes.put(nodes[i],i);}
        void object(SourceValue value,String owner,int depth) {
            if(value==null||depth>35)return;
            for(var n:value.insns) {
                var frame=frames[indexes.get(n)];if(frame==null)continue;
                if(n instanceof VarInsnNode v&&v.getOpcode()==Opcodes.ALOAD) {
                    if(v.var!=0&&targetOwner(owner))targets.computeIfAbsent(v.var,k->new TreeSet<>()).add(owner);
                } else if(n instanceof TypeInsnNode && n.getOpcode()==Opcodes.CHECKCAST)
                    object(frame.getStack(frame.getStackSize()-1),owner,depth+1);
                else if(n instanceof MethodInsnNode call && call.getOpcode()!=Opcodes.INVOKESTATIC) {
                    int receiver=frame.getStackSize()-Type.getArgumentTypes(call.desc).length-1;
                    if(receiver>=0)object(frame.getStack(receiver),call.owner,depth+1);
                } else if(n instanceof FieldInsnNode field&&n.getOpcode()==Opcodes.GETFIELD)
                    object(frame.getStack(frame.getStackSize()-1),field.owner,depth+1);
            }
        }
        void scalar(SourceValue value,int depth) {
            if(value==null||depth>35)return;
            for(var n:value.insns) {
                int at=indexes.get(n);String key=at+":"+depth;
                if(!visited.add(key))continue;
                var frame=frames[at];if(frame==null)continue;
                if(n instanceof VarInsnNode v) {
                    if(v.getOpcode()==Opcodes.ILOAD||v.getOpcode()==Opcodes.LLOAD||v.getOpcode()==Opcodes.DLOAD||v.getOpcode()==Opcodes.FLOAD)
                        scalar(frame.getLocal(v.var),depth+1);
                    else if(v.getOpcode()==Opcodes.ISTORE||v.getOpcode()==Opcodes.LSTORE||v.getOpcode()==Opcodes.DSTORE||v.getOpcode()==Opcodes.FSTORE)
                        scalar(frame.getStack(frame.getStackSize()-1),depth+1);
                } else if(n instanceof MethodInsnNode call) {
                    int size=frame.getStackSize(),argc=Type.getArgumentTypes(call.desc).length;
                    if(call.getOpcode()!=Opcodes.INVOKESTATIC && size>argc)object(frame.getStack(size-argc-1),call.owner,depth+1);
                    var arguments=Type.getArgumentTypes(call.desc);
                    for(int j=Math.max(0,size-argc);j<size;j++) {
                        var type=arguments[j-(size-argc)];
                        if(type.getSort()==Type.OBJECT)object(frame.getStack(j),type.getInternalName(),depth+1);
                        scalar(frame.getStack(j),depth+1);
                    }
                } else if(n instanceof FieldInsnNode field&&n.getOpcode()==Opcodes.GETFIELD)
                    object(frame.getStack(frame.getStackSize()-1),field.owner,depth+1);
                else if(n.getOpcode()>=Opcodes.IADD&&n.getOpcode()<=Opcodes.LXOR) {
                    for(int j=Math.max(0,frame.getStackSize()-2);j<frame.getStackSize();j++)scalar(frame.getStack(j),depth+1);
                }
            }
        }
    }
    public static void main(String[] a)throws Exception {
        var lines=new ArrayList<String>();lines.add("caller\tcallee\tdescriptor\ttarget_reference_candidates");int sites=0,missing=0;
        try(var z=new ZipFile(a[0])) {
            var c=new ClassNode();new ClassReader(z.getInputStream(z.getEntry(P+"ClientSession.class"))).accept(c,0);
            for(var m:c.methods) {
                if(Arrays.stream(m.instructions.toArray()).anyMatch(n->n instanceof MethodInsnNode call
                        &&call.owner.equals(P+"ClientSession")&&call.name.equals("counterBarrierDecision"))) {
                    var frames=new Analyzer<>(new SourceInterpreter()).analyze(c.name,m);var nodes=m.instructions.toArray();
                    for(int i=0;i<nodes.length;i++)if(nodes[i] instanceof MethodInsnNode call
                            &&call.owner.equals(P+"ClientSession")&&call.name.equals("counterBarrierDecision")) {
                        var trace=new Trace(m,frames);var frame=frames[i];int argc=Type.getArgumentTypes(call.desc).length;
                        for(int j=frame.getStackSize()-argc;j<frame.getStackSize();j++)trace.scalar(frame.getStack(j),0);
                        System.out.println("Counter Barrier target operands: "+m.name+" "+trace.targets);
                    }
                }
                for(var n:m.instructions)if(n instanceof MethodInsnNode call&&call.owner.equals(P+"BattleSpellCatalog$ConfusionSpell")
                        &&Set.of("chancePercent","succeeds").contains(call.name))
                    System.out.println("Confusion target metadata: "+m.name+"."+call.name+" "+confusionLocals(m,call));
                if(Arrays.stream(m.instructions.toArray()).noneMatch(n->n instanceof MethodInsnNode x&&probability(x)))continue;
                var frames=new Analyzer<>(new SourceInterpreter()).analyze(c.name,m);
                var types=new Analyzer<>(new BasicInterpreter()).analyze(c.name,m);
                var nodes=m.instructions.toArray();
                for(int i=0;i<nodes.length;i++)if(nodes[i] instanceof MethodInsnNode call&&probability(call)) {
                    var trace=new Trace(m,frames);var frame=frames[i];int argc=Type.getArgumentTypes(call.desc).length;
                    for(int j=frame.getStackSize()-argc;j<frame.getStackSize();j++)trace.scalar(frame.getStack(j),0);
                    final int position=i;
                    trace.targets.entrySet().removeIf(e->e.getKey()>=types[position].getLocals()||!types[position].getLocal(e.getKey()).isReference());
                    sites++;if(trace.targets.isEmpty())missing++;
                    lines.add(m.name+"\t"+call.owner.substring(call.owner.indexOf('$')+1)+"."+call.name+"\t"+call.desc+"\t"+trace.targets);
                }
            }
        }
        Files.write(Path.of(a[1]),lines);
        System.out.println("Probability call sites="+sites+"; sites without live target reference candidates="+missing+"; evidence="+a[1]);
    }
}
