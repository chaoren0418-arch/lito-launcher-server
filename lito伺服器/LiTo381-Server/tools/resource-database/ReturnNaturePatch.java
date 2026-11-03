import java.util.*;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;

final class ReturnNaturePatch implements Opcodes {
    static final String P="local/lito381/server/",H=P+"CombatReturnNature",A=H+"$Attempt",S=P+"BattleSpellCatalog$ReturnToNatureSpell";
    /** Append one typed local without changing existing local slots or handlers. */
    static int attemptLocal(ClassNode c,MethodNode m) {
        int slot=m.maxLocals;var locals=new ArrayList<Object>();locals.add(c.name);
        for(var type:Type.getArgumentTypes(m.desc))locals.add(type.getSort()==Type.OBJECT?type.getInternalName():type.getSort()==Type.ARRAY?type.getDescriptor():INTEGER);
        for(var n:m.instructions)if(n instanceof FrameNode f) {
            switch(f.type){case F_FULL,F_NEW->locals=new ArrayList<>(f.local);case F_APPEND->locals.addAll(f.local);case F_CHOP->{for(int i=0;i<f.local.size();i++)locals.remove(locals.size()-1);}case F_SAME,F_SAME1->{}default->throw new IllegalStateException("unexpected Return Nature frame");}
            var padded=new ArrayList<Object>(locals);int width=0;for(var value:locals)width+=value.equals(LONG)||value.equals(DOUBLE)?2:1;
            if(width>slot)throw new IllegalStateException("Return Nature local frame escaped method");while(width++<slot)padded.add(TOP);padded.add(A);
            f.type=F_FULL;f.local=padded;f.stack=f.stack==null?new ArrayList<>():new ArrayList<>(f.stack);
        }
        var init=new InsnList();init.add(new InsnNode(ACONST_NULL));init.add(new VarInsnNode(ASTORE,slot));m.instructions.insert(init);m.maxLocals++;return slot;
    }
    static void apply(ClassNode c,Map<String,Integer> hooks) {
        int prepared=0,committed=0,boss=0;
        for(var m:c.methods)if(m.name.equals("useReturnToNatureSpell")) {
            int slot=attemptLocal(c,m),owner=DragonPatch.local(m,"targetOwner"),controlled=DragonPatch.local(m,"controlled"),now=DragonPatch.local(m,"requestNow");
            for(var n:m.instructions.toArray())if(n instanceof MethodInsnNode call) {
                if(call.owner.equals(S)&&call.name.equals("probability")) {
                    if(!call.desc.equals("(IIIIII)I"))throw new IllegalStateException("Return Nature probability signature drift");
                    var args=new InsnList();args.add(new VarInsnNode(ALOAD,0));args.add(new VarInsnNode(ALOAD,owner));args.add(new VarInsnNode(ALOAD,controlled));args.add(new VarInsnNode(LLOAD,now));m.instructions.insertBefore(call,args);
                    m.instructions.set(call,new MethodInsnNode(INVOKESTATIC,H,"prepare","(L"+S+";IIIIIIL"+P+"ClientSession;L"+P+"ClientSession;L"+P+"BattleCompanion;J)L"+A+";",false));
                    prepared++;
                }
            }
            // The preparation is the sole replacement; save its exact lease beside the chance.
            for(var n:m.instructions.toArray())if(n instanceof MethodInsnNode call) {
                if(call.owner.equals(H)&&call.name.equals("prepare")) {
                    var i=new InsnList();i.add(new InsnNode(DUP));i.add(new VarInsnNode(ASTORE,slot));i.add(new InsnNode(DUP));i.add(new MethodInsnNode(INVOKEVIRTUAL,A,"magicResistance","()I",false));i.add(new VarInsnNode(ISTORE,DragonPatch.local(m,"targetMr")));i.add(new MethodInsnNode(INVOKEVIRTUAL,A,"probability","()I",false));m.instructions.insert(call,i);
                }
                if(call.owner.equals(c.name)&&call.name.equals("sendInventoryDelta")) {
                    var i=new InsnList();i.add(new VarInsnNode(ALOAD,slot));i.add(new VarInsnNode(ALOAD,controlled));i.add(new MethodInsnNode(INVOKESTATIC,H,"commit","(L"+A+";L"+P+"BattleCompanion;)V",false));m.instructions.insertBefore(call,i);committed++;
                }
                if(call.owner.equals(P+"SourceNpcPhysicalStats")&&call.name.equals("boss")) {
                    var i=new InsnList();i.add(new VarInsnNode(ALOAD,2));i.add(new MethodInsnNode(INVOKESTATIC,P+"CombatProbability","bossGate","(ZLjava/lang/Object;)Z",false));m.instructions.insert(call,i);boss++;
                }
            }
        }
        CombatPatch.require(prepared,1,"Return Nature live target proposal");CombatPatch.require(committed,1,"Return Nature post-cost status commit");CombatPatch.require(boss,1,"Return Nature source boss gate");
        hooks.put("combat-return-nature-target",prepared);hooks.put("combat-return-nature-commit",committed);hooks.put("combat-return-nature-boss-gate",boss);
    }
}
