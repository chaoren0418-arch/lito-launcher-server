import java.util.*;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
import org.objectweb.asm.tree.analysis.*;
final class CounterDetectionPatch implements Opcodes {
    static final String P="local/lito381/server/",H=P+"CombatCounterDetection",C="L"+P+"ClientSession;",A=P+"CombatMagicDamage$Attempt";
    static void apply(ClassNode c,Map<String,Integer> hooks)throws AnalyzerException {
        int prepared=0,finished=0;
        for(var m:new ArrayList<>(c.methods))if(Set.of("useCounterDetectionSpell","useClassicWorldCounterDetectionSpell").contains(m.name)){
            int slot=MagicDamagePatch.appendLocal(c,m),clock=DragonPatch.local(m,"castNow"),target=-1;m.maxStack+=32;var nodes=m.instructions.toArray();var frames=new Analyzer<>(new SourceInterpreter()).analyze(c.name,m);
            for(int at=0;at<nodes.length;at++)if(nodes[at] instanceof MethodInsnNode call&&call.owner.equals(P+"ClientSession")&&call.name.equals("earthBindProtectedCombatDamage")){var f=frames[at];target=MagicDamagePatch.load(f.getStack(f.getStackSize()-3),ALOAD).var;}
            if(target<0)throw new IllegalStateException("missing72 PC receiver");
            for(var n:m.instructions.toArray())if(n instanceof MethodInsnNode call){
                if(call.owner.equals(P+"ClientSession")&&call.name.equals("ordinaryMagicDamage")&&call.desc.equals(MagicDamagePatch.ORDINARY)){
                    var args=new InsnList();args.add(new VarInsnNode(ALOAD,target));args.add(new VarInsnNode(LLOAD,clock));m.instructions.insertBefore(n,args);var bridge=new MethodInsnNode(INVOKESTATIC,H,"prepare","("+C+call.desc.substring(1,call.desc.indexOf(')'))+C+"J)L"+A+";",false);m.instructions.set(n,bridge);MagicDamagePatch.capture(m,bridge,slot,false);prepared++;
                }
                if(call.owner.equals(P+"ClientSession")&&call.name.equals("earthBindProtectedCombatDamage")&&call.desc.equals("(ILjava/lang/String;)I")){
                    var i=new InsnList();i.add(new VarInsnNode(ALOAD,slot));i.add(new VarInsnNode(ALOAD,0));i.add(new VarInsnNode(ALOAD,target));i.add(new VarInsnNode(LLOAD,clock));i.add(new MethodInsnNode(INVOKESTATIC,H,"finish","(IL"+A+";"+C+C+"J)I",false));m.instructions.insert(n,i);finished++;
                }
            }
        }
        CombatPatch.require(prepared,2,"native72 prepared PC damage");CombatPatch.require(finished,2,"native72 post-reduction source finish");hooks.put("source-counter-detection-player",prepared+finished);
    }
}
