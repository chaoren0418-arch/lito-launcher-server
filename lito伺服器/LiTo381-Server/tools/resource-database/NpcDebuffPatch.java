import java.util.*;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
final class NpcDebuffPatch implements Opcodes {
    static final String P="local/lito381/server/",H=P+"CombatNpcDebuffs",C="L"+P+"ClientSession;";
    static MethodInsnNode call(String n,String d){return new MethodInsnNode(INVOKESTATIC,H,n,d,false);}
    static void healingCatalog(ClassNode c,Map<String,Integer> hooks) {
        var m=CombatPatch.method(c,"npcHpDelta","(II)I");int count=0;for(var n:m.instructions.toArray())if(n instanceof JumpInsnNode jump&&jump.getOpcode()==IFGE&&DragonPatch.prior(n) instanceof VarInsnNode amount&&amount.getOpcode()==ILOAD&&amount.var==2){var i=new InsnList();i.add(new InsnNode(POP));i.add(new JumpInsnNode(GOTO,jump.label));m.instructions.insertBefore(n,i);m.instructions.remove(n);count++;}CombatPatch.require(count,1,"source signed healing before native undead conversion");hooks.put("source-npc-signed-healing",count);
    }
    static void companion(ClassNode c,Map<String,Integer> hooks) {
        var m=CombatPatch.method(c,"cancelMagicEffects","()L"+P+"BattleCompanion$MagicCancellation;");int changes=0;
        for(var n:m.instructions.toArray())if(n instanceof MethodInsnNode v&&v.owner.equals(c.name)&&v.name.equals("snapshot")){var store=DragonPatch.next(n);if(!(store instanceof VarInsnNode s)||s.getOpcode()!=ASTORE)throw new IllegalStateException("missing NPC cancellation life gate");var i=new InsnList();i.add(new VarInsnNode(ALOAD,0));i.add(call("cancel","(L"+P+"BattleCompanion;)V"));m.instructions.insert(store,i);changes++;}
        CombatPatch.require(changes,1,"source NPC Striker cancellation");hooks.put("source-npc-striker-cancel",changes);
    }
    static int firstResult(MethodNode m,String owner,String name) {
        for(var n:m.instructions)if(n instanceof MethodInsnNode v&&v.owner.equals(owner)&&v.name.equals(name)){var next=DragonPatch.next(n);if(!(next instanceof VarInsnNode store)||store.getOpcode()!=ASTORE)throw new IllegalStateException("missing native healing target producer "+name);return store.var;}throw new IllegalStateException("missing native healing target "+name);
    }
    static int battleCastTime(MethodNode m) {
        var slots=new ArrayList<Integer>();for(var n:m.instructions)if(n instanceof MethodInsnNode v&&v.owner.equals("java/lang/System")&&v.name.equals("nanoTime")){var next=DragonPatch.next(n);if(!(next instanceof VarInsnNode store)||store.getOpcode()!=LSTORE)throw new IllegalStateException("native healing cast time changed");slots.add(store.var);}CombatPatch.require(slots.size(),2,"native healing request/cast clocks");return slots.get(1);
    }
    static void healing(ClassNode c,Map<String,Integer> hooks) {
        int count=0;
        for(var m:c.methods)if(m.name.equals("useHealingSpell")||m.name.equals("useClassicWorldHealingSpell")&&m.desc.equals("([BL"+P+"BattleSpellCatalog$HealingSpell;I)V"))for(var n:m.instructions.toArray())if(n instanceof MethodInsnNode v&&v.owner.equals(P+"BattleSpellCatalog$HealingSpell")&&v.name.equals("npcHpDelta")&&v.desc.equals("(II)I")) {
            if(!(DragonPatch.prior(n) instanceof VarInsnNode amount)||amount.getOpcode()!=ILOAD)throw new IllegalStateException("native healing scalar producer changed");var i=new InsnList();
            if(m.name.equals("useHealingSpell")){
                i.add(new VarInsnNode(ALOAD,firstResult(m,P+"ClientSession","resolveBattleSpellCompanion")));i.add(new VarInsnNode(LLOAD,battleCastTime(m)));i.add(call("healingBattle","(ILjava/lang/Object;J)I"));
            }else{
                for(String name:new String[]{"companionOwner","companionTarget"})i.add(new VarInsnNode(ALOAD,DragonPatch.local(m,name)));i.add(new VarInsnNode(LLOAD,DragonPatch.local(m,"castNow")));i.add(call("healingWorld","(I"+C+"L"+P+"BattleCompanion$Snapshot;J)I"));
            }
            m.instructions.insertBefore(n,i);count++;
        }
        CombatPatch.require(count,2,"native NPC single-target healing status consumers");hooks.put("source-npc-healing-consumers",count);
    }
    static void session(ClassNode c,Map<String,Integer> hooks) {
        healing(c,hooks);
        var negative=CombatPatch.method(c,"useTargetNegativeStatusSpell","([BL"+P+"BattleSpellCatalog$TargetNegativeStatusSpell;IL"+P+"ClientSession$TargetNegativeStatusKind;)V");var fallback=new LabelNode();var prefix=new InsnList();for(int slot:new int[]{0,1,2})prefix.add(new VarInsnNode(ALOAD,slot));prefix.add(new VarInsnNode(ILOAD,3));prefix.add(call("negative","("+C+"[BL"+P+"BattleSpellCatalog$TargetNegativeStatusSpell;I)Z"));prefix.add(new JumpInsnNode(IFEQ,fallback));prefix.add(new InsnNode(RETURN));prefix.add(fallback);prefix.add(new FrameNode(F_SAME,0,null,0,null));negative.instructions.insert(prefix);
        var armor=CombatPatch.method(c,"useArmorBreakSpell","([BL"+P+"BattleSpellCatalog$ArmorBreakSpell;)V");int targets=0,policies=0,consumers=0;
        for(var n:armor.instructions.toArray())if(n instanceof MethodInsnNode v&&v.owner.equals(c.name)&&v.name.equals("resolveArmorBreakTarget")){var i=new InsnList();i.add(new VarInsnNode(ALOAD,0));i.add(call("armorTarget","(Ljava/lang/Object;"+C+")Ljava/lang/Object;"));i.add(new TypeInsnNode(CHECKCAST,P+"ClientSession$ArmorBreakTarget"));armor.instructions.insert(n,i);targets++;}
        for(var m:c.methods)if(Set.of("attack","attackClassicWorld").contains(m.name)) {
            var calls=Arrays.stream(m.instructions.toArray()).filter(n->n instanceof MethodInsnNode v&&v.owner.equals(P+"CombatRules")&&v.name.equals("strikerBeforeClass")).toList();if(calls.isEmpty())continue;CombatPatch.require(calls.size(),1,"NPC Striker physical caller");
            boolean world=m.name.equals("attackClassicWorld");int context,flag,player;
            if(world){context=-1;for(var n:m.instructions)if(n instanceof MethodInsnNode v&&v.owner.equals(P+"CombatWorldPhysical")&&v.name.equals("prepare"))context=((VarInsnNode)DragonPatch.next(n)).var;if(context<0)throw new IllegalStateException("missing WORLD owned target local");}
            else context=-1;
            if(world){flag=DragonPatch.local(m,"armorBreakDamageActive");player=-1;}
            else {
                var physical=Arrays.stream(m.instructions.toArray()).filter(n->n instanceof MethodInsnNode v&&v.owner.equals(P+"CombatCompanionDamage")&&v.name.equals("physical")).toList();CombatPatch.require(physical.size(),1,"native companion physical provenance");
                var loads=new ArrayList<VarInsnNode>();var n=physical.get(0);for(int pos=0;pos<9;pos++){n=DragonPatch.prior(n);if(!(n instanceof VarInsnNode load))throw new IllegalStateException("companion physical argument producer changed");loads.add(load);}
                if(loads.get(0).getOpcode()!=ALOAD||loads.get(2).getOpcode()!=ILOAD||loads.get(7).getOpcode()!=ALOAD||loads.get(8).getOpcode()!=ALOAD||loads.get(8).var!=0)throw new IllegalStateException("companion physical role operands changed");context=loads.get(0).var;flag=loads.get(2).var;player=loads.get(7).var;
            }
            var capture=DragonPatch.prior(DragonPatch.prior(DragonPatch.prior(calls.get(0))));if(capture.getOpcode()!=DUP2)throw new IllegalStateException("Striker source capture shape changed");var adapt=new InsnList();adapt.add(new InsnNode(SWAP));adapt.add(new VarInsnNode(ALOAD,context));adapt.add(call("strikerActive","(ZLjava/lang/Object;)Z"));adapt.add(new InsnNode(SWAP));m.instructions.insertBefore(capture,adapt);consumers++;
            int changed=0;
            for(var n:m.instructions.toArray())if(n instanceof MethodInsnNode armorCall&&armorCall.owner.equals(P+"BattleArmorBreak")&&armorCall.name.equals("damageAfterBreak")){
                var weaponCall=DragonPatch.prior(n);var weaponLoad=DragonPatch.prior(weaponCall);var active=DragonPatch.prior(weaponLoad);if(!(active instanceof VarInsnNode value)||value.getOpcode()!=ILOAD||value.var!=flag)throw new IllegalStateException("native Armor Break operand changed");
                var i=new InsnList();i.add(world?new InsnNode(ACONST_NULL):new VarInsnNode(ALOAD,player));i.add(call("armorActive","(Z"+C+")Z"));i.add(new InsnNode(DUP));i.add(new VarInsnNode(ISTORE,flag));m.instructions.insert(active,i);changed++;
            }
            CombatPatch.require(changed,1,"NPC Armor Break policy "+m.name);policies+=changed;
        }
        CombatPatch.require(targets,1,"Armor Break player-only target");CombatPatch.require(policies,2,"NPC Armor Break physical exclusions");CombatPatch.require(consumers,2,"owned Striker physical state consumers");hooks.put("source-npc-target-policies",targets+policies);hooks.put("source-owned-striker",1+consumers);
    }
}
