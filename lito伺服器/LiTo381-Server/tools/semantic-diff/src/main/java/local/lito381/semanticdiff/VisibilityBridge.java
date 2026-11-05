package local.lito381.semanticdiff;

import java.util.*;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;

/** javac visibility bridge only; descriptor adaptation and public ancestors are excluded. */
final class VisibilityBridge implements Opcodes {
    private VisibilityBridge() {}
    static boolean matches(ClassNode left,ClassNode right,MethodNode m,JarIndex lj,JarIndex rj) {
        if(!FourthNormalizations.enabled("visibility")||m.access!=0x1041
                ||(left.access&ACC_PUBLIC)==0||!Objects.equals(left.superName,right.superName)
                ||!m.tryCatchBlocks.isEmpty()||FourthNormalizations.metadata(m)
                ||left.methods.stream().filter(x->x.name.equals(m.name)&&x.desc.equals(m.desc)).count()!=1
                ||right.methods.stream().anyMatch(x->x.name.equals(m.name)&&x.desc.equals(m.desc)))return false;
        List<AbstractInsnNode> code=Arrays.stream(m.instructions.toArray()).filter(n->n.getOpcode()>=0).toList();
        Type[] args=Type.getArgumentTypes(m.desc);
        if(code.size()!=args.length+3||code.stream().anyMatch(FourthNormalizations::annotated))return false;
        if(!(code.get(0) instanceof VarInsnNode self)||self.getOpcode()!=ALOAD||self.var!=0)return false;
        int slot=1;
        for(int i=0;i<args.length;i++) {
            if(!(code.get(i+1) instanceof VarInsnNode v)||v.var!=slot||v.getOpcode()!=args[i].getOpcode(ILOAD))return false;
            slot+=args[i].getSize();
        }
        if(!(code.get(args.length+1) instanceof MethodInsnNode call)||call.getOpcode()!=INVOKESPECIAL
                ||call.itf||!call.owner.equals(left.superName)||!call.name.equals(m.name)||!call.desc.equals(m.desc)
                ||code.get(code.size()-1).getOpcode()!=Type.getReturnType(m.desc).getOpcode(IRETURN))return false;
        String a=declaration(left.superName,m,lj,Map.of()),b=declaration(right.superName,m,rj,lj.accessWiden);
        return a!=null&&a.equals(b);
    }
    private static String declaration(String owner,MethodNode method,JarIndex jar,Map<String,Set<String>> approvedWiden) {
        Set<String> seen=new HashSet<>();
        while(owner!=null&&seen.add(owner)) {
            ClassNode c=jar.header(owner);if(c==null)return null;
            for(MethodNode m:c.methods)if(m.name.equals(method.name)&&m.desc.equals(method.desc)) {
                // An explicit existing access-widen approval may explain why javac
                // sees a public inherited method only on the staged side.
                boolean publicMethod=(m.access&ACC_PUBLIC)!=0
                        ||approvedWiden.getOrDefault(c.name,Set.of()).contains(m.name+m.desc);
                if((c.access&ACC_PUBLIC)!=0||!publicMethod
                        ||(m.access&(ACC_STATIC|ACC_PRIVATE|ACC_ABSTRACT))!=0)return null;
                return c.name;
            }
            owner=c.superName;
        }
        return null;
    }
}
