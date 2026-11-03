import java.util.*;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;

/** Preserve native target admission and status snapshots; defer Striker to source commit order. */
final class RangedDamagePatch implements Opcodes {
    static final String P="local/lito381/server/",H=P+"CombatRules";
    static int flag(ClassNode c,MethodNode m) {
        int slot=m.maxLocals;var locals=new ArrayList<Object>();locals.add(c.name);
        for(var t:Type.getArgumentTypes(m.desc))locals.add(t.getSort()==Type.OBJECT?t.getInternalName():t.getSort()==Type.ARRAY?t.getDescriptor():INTEGER);
        for(var n:m.instructions)if(n instanceof FrameNode f) {
            switch(f.type){case F_FULL,F_NEW->locals=new ArrayList<>(f.local);case F_APPEND->locals.addAll(f.local);case F_CHOP->{for(int i=0;i<f.local.size();i++)locals.remove(locals.size()-1);}case F_SAME,F_SAME1->{}default->throw new IllegalStateException("unexpected ranged frame");}
            var padded=new ArrayList<Object>(locals);int width=0;for(var v:locals)width+=v.equals(LONG)||v.equals(DOUBLE)?2:1;
            if(width>slot)throw new IllegalStateException("ranged flag escaped method");while(width++<slot)padded.add(TOP);padded.add(INTEGER);f.type=F_FULL;f.local=padded;f.stack=f.stack==null?new ArrayList<>():new ArrayList<>(f.stack);
        }
        var init=new InsnList();init.add(new InsnNode(ICONST_0));init.add(new VarInsnNode(ISTORE,slot));m.instructions.insert(init);m.maxLocals++;return slot;
    }
    static void apply(ClassNode c,Map<String,Integer> hooks) {
        int captured=0,finished=0;
        for(var m:c.methods)if(Set.of("attack","attackClassicWorld","attackClassicWorldPlayer").contains(m.name)) {
            if(Arrays.stream(m.instructions.toArray()).noneMatch(n->n instanceof MethodInsnNode call&&call.owner.equals(P+"BattleStrikerGale")&&call.name.equals("damageAfterStatus")))continue;
            int slot=flag(c,m),before=0,after=0;
            for(var n:m.instructions.toArray())if(n instanceof MethodInsnNode call) {
                if(call.owner.equals(P+"BattleStrikerGale")&&call.name.equals("damageAfterStatus")&&call.desc.equals("(IZZ)I")) {
                    var i=new InsnList();i.add(new InsnNode(DUP2));i.add(new InsnNode(IAND));i.add(new VarInsnNode(ISTORE,slot));m.instructions.insertBefore(call,i);m.instructions.set(call,new MethodInsnNode(INVOKESTATIC,H,"strikerBeforeClass","(IZZ)I",false));before++;
                }
                if(call.owner.equals(P+"CombatKiringku")&&call.name.equals("finish")||call.owner.equals(P+"CombatCompanionDamage")&&call.name.equals("physical")) {
                    var i=new InsnList();i.add(new VarInsnNode(ILOAD,slot));i.add(new MethodInsnNode(INVOKESTATIC,H,"strikerAfterClass","(IZ)I",false));m.instructions.insert(call,i);after++;
                }
            }
            CombatPatch.require(before,1,"source ranged status snapshot "+m.name);CombatPatch.require(after,1,"source ranged commit position "+m.name);captured+=before;finished+=after;
        }
        CombatPatch.require(captured,3,"source Striker preparation");CombatPatch.require(finished,3,"source Striker final class order");hooks.put("source-striker-commit-order",captured+finished);
    }
}
