package local.lito381.semanticdiff;

import java.util.*;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;
import org.objectweb.asm.tree.analysis.*;

/** Collapse only adjacent DUP; STORE a; STORE b aliases with unique reaching writes. */
final class CfgTransport implements Opcodes {
    private CfgTransport() {}
    static MethodNode normalize(MethodNode source)throws AnalyzerException {
        MethodNode m=LocalTransport.normalize(source);
        if(m.visibleLocalVariableAnnotations!=null||m.invisibleLocalVariableAnnotations!=null)return m;
        boolean changed;
        do {
            changed=false;
            Frame<SourceValue>[] frames=new Analyzer<>(new LocalWebs.Definitions()).analyze("CfgTransport",m);
            for(AbstractInsnNode n:m.instructions.toArray()) {
                if(!(n instanceof VarInsnNode a)||!LocalWebs.store(a.getOpcode())
                        ||!(n.getNext() instanceof VarInsnNode b)||b.getOpcode()!=a.getOpcode()||a.var==b.var
                        ||a.var<LocalWebs.parameters(m)||b.var<LocalWebs.parameters(m))continue;
                AbstractInsnNode dup=n.getPrevious();int width=LocalWebs.width(a.getOpcode());
                if(dup==null||dup.getOpcode()!=(width==2?DUP2:DUP)||FourthNormalizations.annotated(dup)
                        ||FourthNormalizations.annotated(a)||FourthNormalizations.annotated(b))continue;
                if(!unique(m,a,frames)||!unique(m,b,frames))continue;
                for(AbstractInsnNode x:m.instructions)if(x instanceof VarInsnNode v&&v.var==b.var&&v!=b)v.var=a.var;
                m.instructions.remove(dup);m.instructions.remove(b);changed=true;break;
            }
        }while(changed);
        return m;
    }
    private static boolean unique(MethodNode m,VarInsnNode store,Frame<SourceValue>[] frames) {
        int width=LocalWebs.width(store.getOpcode());
        for(AbstractInsnNode n:m.instructions) {
            if(n instanceof IincInsnNode i&&i.var>=store.var&&i.var<store.var+width)return false;
            if(!(n instanceof VarInsnNode v)||n==store)continue;
            if(v.var>=store.var+width||v.var+LocalWebs.width(v.getOpcode())<=store.var)continue;
            if(v.var!=store.var||v.getOpcode()!=store.getOpcode()-(ISTORE-ILOAD)||FourthNormalizations.annotated(v))return false;
            Frame<SourceValue> f=frames[m.instructions.indexOf(v)];
            if(f==null||!f.getLocal(v.var).insns.equals(Set.of(store)))return false;
        }
        return true;
    }
}
