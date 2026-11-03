import java.util.*;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;

/** LiTo death never enters the source item/skill/experience penalty dispatcher. */
final class DeathPenaltyPatch implements Opcodes {
    static void apply(ClassNode c,Map<String,Integer> hooks) {
        int count=0;
        for(var m:c.methods)for(var n:m.instructions.toArray())if(n instanceof MethodInsnNode call&&call.owner.equals(c.name)&&call.name.equals("applyClassicWorldDeathExperiencePenalty")) {
            if(!m.name.equals("takeClassicWorldDirectDamage")||!call.desc.equals("(J)V")||call.getOpcode()!=INVOKEVIRTUAL)throw new IllegalStateException("unexpected death penalty dispatcher");
            // Preserve the existing stack evaluation and all surrounding death/war/restart work.
            var replacement=new InsnList();replacement.add(new InsnNode(POP2));replacement.add(new InsnNode(POP));m.instructions.insertBefore(call,replacement);m.instructions.remove(call);count++;
        }
        CombatPatch.require(count,1,"world death no-penalty boundary");hooks.put("lito-no-death-penalties",count);
    }
}
