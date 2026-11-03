import java.util.*;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
import org.objectweb.asm.tree.analysis.*;
final class ClassDamagePatch implements Opcodes {
    static final String P="local/lito381/server/",H=P+"CombatClassDamage";
    static int playerLocal(ClassNode c,MethodNode m)throws AnalyzerException {
        var frames=new Analyzer<>(new SourceInterpreter()).analyze(c.name,m);var found=new HashSet<Integer>();var nodes=m.instructions.toArray();
        for(int at=0;at<nodes.length;at++)if(nodes[at] instanceof MethodInsnNode call&&call.owner.equals(c.name)&&call.name.equals("combatDamageAfterCooking")) {
            var frame=frames[at];var receiver=frame.getStack(frame.getStackSize()-Type.getArgumentTypes(call.desc).length-1);
            if(receiver.insns.size()!=1||!(receiver.insns.iterator().next() instanceof VarInsnNode load)||load.getOpcode()!=ALOAD||load.var==0)throw new IllegalStateException("ambiguous class damage PC receiver");found.add(load.var);
        }
        if(found.size()!=1)throw new IllegalStateException("missing class damage PC receiver: "+found);return found.iterator().next();
    }
    static void apply(ClassNode c,Map<String,Integer> hooks)throws AnalyzerException {
        int sites=0;
        for(var m:c.methods)if(Set.of("attack","attackClassicWorld","attackClassicWorldPlayer").contains(m.name))for(var n:m.instructions.toArray())
            if(n instanceof MethodInsnNode call&&call.owner.equals(P+"BattleStrikerGale")&&call.name.equals("damageAfterStatus")) {
                var i=new InsnList();i.add(new VarInsnNode(ALOAD,0));
                if(m.name.equals("attackClassicWorld"))i.add(new InsnNode(ACONST_NULL));else i.add(new VarInsnNode(ALOAD,playerLocal(c,m)));
                i.add(new MethodInsnNode(INVOKESTATIC,H,"physical","(IL"+P+"ClientSession;L"+P+"ClientSession;)I",false));
                var next=m.instructions.iterator();MethodInsnNode strike=null;while(next.hasNext()){var node=next.next();if(node instanceof MethodInsnNode v&&v.owner.equals(P+"BattleFoeSlayerSequence")&&v.name.equals("next"))strike=v;}
                if(strike==null)throw new IllegalStateException("missing class damage hit boundary");int hit=DragonPatch.strikeLocals(m,strike).hit();
                i.add(new InsnNode(DUP));i.add(new VarInsnNode(ILOAD,hit));i.add(new InsnNode(SWAP));i.add(new MethodInsnNode(INVOKESTATIC,H,"hit","(ZI)Z",false));i.add(new VarInsnNode(ISTORE,hit));
                m.instructions.insert(call,i);sites++;
            }
        CombatPatch.require(sites,3,"source class physical damage boundaries");hooks.put("source-class-physical-damage",sites);
    }
}
