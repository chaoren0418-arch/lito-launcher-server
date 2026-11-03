import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;

final class ForcedCombatPatch implements Opcodes {
    static final String P="local/lito381/server/", H=P+"ForcedCombat";
    static void session(ClassNode c) {
        var old=c.methods.stream().filter(m->m.name.equals("handle")&&m.desc.equals("([B)V")).findFirst().orElseThrow();
        old.name="handleBeforeForcedCombat";
        var m=new MethodNode(ACC_PRIVATE,"handle","([B)V",null,new String[]{"java/lang/Exception"});
        m.instructions.add(new VarInsnNode(ALOAD,0));m.instructions.add(new VarInsnNode(ALOAD,1));
        m.instructions.add(new MethodInsnNode(INVOKESTATIC,H,"dispatch","(L"+P+"ClientSession;[B)V",false));
        m.instructions.add(new InsnNode(RETURN));c.methods.add(m);
    }
    static void hub(ClassNode c) {
        var m=c.methods.stream().filter(x->x.name.equals("canAttack")&&x.desc.equals("(JL"+P+"ClientSession;L"+P+"ClientSession;)Z")).findFirst().orElseThrow();
        int count=0;
        for(var n:m.instructions.toArray()) if(n.getOpcode()==IRETURN) {
            var i=new InsnList();i.add(new VarInsnNode(ALOAD,0));i.add(new VarInsnNode(LLOAD,1));
            i.add(new VarInsnNode(ALOAD,3));i.add(new VarInsnNode(ALOAD,4));
            i.add(new MethodInsnNode(INVOKESTATIC,H,"canAttack","(ZL"+P+"GameHub;JL"+P+"ClientSession;L"+P+"ClientSession;)Z",false));
            m.instructions.insertBefore(n,i);count++;
        }
        if(count!=1)throw new IllegalStateException("canAttack return drift: "+count);
    }
}
