import java.util.*;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
import static org.objectweb.asm.Opcodes.*;

/** Invert only declared NPC target-policy/state hooks; no native fields or original blocks change. */
final class VerifyNpcDebuffs {
    static final String P="local/lito381/server/",H=P+"CombatNpcDebuffs",C="L"+P+"ClientSession;";
    static AbstractInsnNode before(AbstractInsnNode n){return VerifyCombatPreservation.prior(n);}
    static AbstractInsnNode after(AbstractInsnNode n){return VerifyCombatPreservation.following(n);}
    static VarInsnNode local(AbstractInsnNode n,int opcode){return VerifyLineSpells.local(n,opcode);}
    static void require(boolean good,String message){if(!good)throw new AssertionError("NPC debuff "+message);}
    static void erase(MethodNode m,AbstractInsnNode...n){VerifyLineSpells.erase(m,n);}
    static int[] roles(MethodNode m) {
        if(m.name.equals("attackClassicWorld")){int ctx=-1;for(var n:m.instructions)if(n instanceof MethodInsnNode call&&call.owner.equals(P+"CombatWorldPhysical")&&call.name.equals("prepare"))ctx=local(after(n),ASTORE).var;require(ctx>=0,"WORLD context provenance");return new int[]{ctx,VerifyLineSpells.slot(m,"armorBreakDamageActive"),-1};}
        var calls=Arrays.stream(m.instructions.toArray()).filter(n->n instanceof MethodInsnNode call&&call.owner.equals(P+"CombatCompanionDamage")&&call.name.equals("physical")).toList();require(calls.size()==1,"BATTLE context producer count");var loads=new ArrayList<VarInsnNode>();var n=calls.get(0);for(int at=0;at<9;at++){n=before(n);require(n instanceof VarInsnNode,"native physical role producer");loads.add((VarInsnNode)n);}require(loads.get(0).getOpcode()==ALOAD&&loads.get(2).getOpcode()==ILOAD&&loads.get(7).getOpcode()==ALOAD&&loads.get(8).getOpcode()==ALOAD&&loads.get(8).var==0,"native physical role types");return new int[]{loads.get(0).var,loads.get(2).var,loads.get(7).var};
    }
    static MethodNode signedHealing(MethodNode source) {
        require(source.name.equals("npcHpDelta")&&source.desc.equals("(II)I"),"signed NPC healing identity");var m=new MethodNode(source.access,source.name,source.desc,source.signature,source.exceptions.toArray(String[]::new));source.accept(m);int count=0;
        for(var n:m.instructions.toArray())if(n.getOpcode()==POP){var amount=before(n);var branch=after(n);var failure=after(branch);require(local(amount,ILOAD).var==2&&branch instanceof JumpInsnNode jump&&jump.getOpcode()==GOTO&&failure instanceof TypeInsnNode fail&&fail.getOpcode()==NEW&&fail.desc.equals("java/lang/IllegalArgumentException"),"only signed amount guard can be bypassed");var jump=(JumpInsnNode)branch;m.instructions.set(branch,new JumpInsnNode(IFGE,jump.label));m.instructions.remove(n);count++;}require(count==1,"exact signed healing guard count");return m;
    }
    static int resultSlot(MethodNode m,String owner,String name) {
        var calls=Arrays.stream(m.instructions.toArray()).filter(n->n instanceof MethodInsnNode c&&c.owner.equals(owner)&&c.name.equals(name)).toList();require(!calls.isEmpty(),"healing native producer missing "+name);return local(after(calls.get(0)),ASTORE).var;
    }
    static int castTime(MethodNode m) {
        var slots=new ArrayList<Integer>();for(var n:m.instructions)if(n instanceof MethodInsnNode c&&c.owner.equals("java/lang/System")&&c.name.equals("nanoTime"))slots.add(local(after(n),LSTORE).var);require(slots.size()==2,"healing request/cast time producer count");return slots.get(1);
    }
    static MethodNode normalize(MethodNode source) {
        var m=new MethodNode(source.access,source.name,source.desc,source.signature,source.exceptions.toArray(String[]::new));source.accept(m);if(!Arrays.stream(m.instructions.toArray()).anyMatch(n->n instanceof MethodInsnNode call&&call.owner.equals(H)))return m;
        var counts=new TreeMap<String,Integer>();int[] roles=Set.of("attack","attackClassicWorld").contains(m.name)?roles(m):null;
        for(var n:m.instructions.toArray())if(m.instructions.contains(n)&&n instanceof MethodInsnNode call&&call.owner.equals(H)) {
            counts.merge(call.name,1,Integer::sum);
            switch(call.name) {
                case "negative" -> {
                    require(m.name.equals("useTargetNegativeStatusSpell")&&m.desc.equals("([BL"+P+"BattleSpellCatalog$TargetNegativeStatusSpell;IL"+P+"ClientSession$TargetNegativeStatusKind;)V")&&call.desc.equals("("+C+"[BL"+P+"BattleSpellCatalog$TargetNegativeStatusSpell;I)Z"),"owned Striker entry identity");
                    var roll=before(call);var spell=before(roll);var payload=before(spell);var caster=before(payload);var branch=after(call);var ret=after(branch);var first=m.instructions.getFirst();while(first.getOpcode()<0)first=first.getNext();require(first==caster&&local(caster,ALOAD).var==0&&local(payload,ALOAD).var==1&&local(spell,ALOAD).var==2&&local(roll,ILOAD).var==3&&branch instanceof JumpInsnNode j&&j.getOpcode()==IFEQ&&ret.getOpcode()==RETURN&&ret.getNext()==j.label,"conditional owned entry and native fallback");erase(m,caster,payload,spell,roll,call,branch,ret);
                }
                case "armorTarget" -> {
                    var caster=before(call);var original=before(caster);var cast=after(call);require(m.name.equals("useArmorBreakSpell")&&call.desc.equals("(Ljava/lang/Object;"+C+")Ljava/lang/Object;")&&local(caster,ALOAD).var==0&&original instanceof MethodInsnNode v&&v.owner.equals(P+"ClientSession")&&v.name.equals("resolveArmorBreakTarget")&&v.desc.endsWith(")L"+P+"ClientSession$ArmorBreakTarget;")&&cast instanceof TypeInsnNode t&&t.getOpcode()==CHECKCAST&&t.desc.equals(P+"ClientSession$ArmorBreakTarget"),"player-only Armor Break result filter");erase(m,caster,call,cast);
                }
                case "armorActive" -> {
                    require(roles!=null&&call.desc.equals("(Z"+C+")Z"),"Armor Break physical location");var player=before(call);var active=before(player);var dup=after(call);var store=after(dup);var weapon=after(store);var getType=after(weapon);var apply=after(getType);
                    require(local(active,ILOAD).var==roles[1]&&(roles[2]<0?player.getOpcode()==ACONST_NULL:local(player,ALOAD).var==roles[2])&&dup.getOpcode()==DUP&&local(store,ISTORE).var==roles[1]&&weapon instanceof VarInsnNode w&&w.getOpcode()==ALOAD&&getType instanceof MethodInsnNode t&&t.name.equals("weaponType")&&apply instanceof MethodInsnNode a&&a.owner.equals(P+"BattleArmorBreak")&&a.name.equals("damageAfterBreak"),"same flag must feed original armor calculation and source finish");erase(m,player,call,dup,store);
                }
                case "strikerActive" -> {
                    require(roles!=null&&call.desc.equals("(ZLjava/lang/Object;)Z"),"Striker physical location");var actor=before(call);var swap=before(actor);var restore=after(call);var capture=after(restore);var and=after(capture);var flag=after(and);var original=after(flag);
                    require(local(actor,ALOAD).var==roles[0]&&swap.getOpcode()==SWAP&&restore.getOpcode()==SWAP&&capture.getOpcode()==DUP2&&and.getOpcode()==IAND&&flag instanceof VarInsnNode f&&f.getOpcode()==ISTORE&&original instanceof MethodInsnNode a&&a.owner.equals(P+"CombatRules")&&a.name.equals("strikerBeforeClass"),"actual owned state must precede retained source commit flag");erase(m,swap,actor,call,restore);
                }
                case "healingBattle","healingWorld" -> {
                    boolean world=call.name.equals("healingWorld");require(world?m.name.equals("useClassicWorldHealingSpell")&&m.desc.equals("([BL"+P+"BattleSpellCatalog$HealingSpell;I)V"):m.name.equals("useHealingSpell")&&m.desc.equals("([BL"+P+"BattleSpellCatalog$HealingSpell;)V"),"single-target healing method identity");
                    require(call.desc.equals(world?"(I"+C+"L"+P+"BattleCompanion$Snapshot;J)I":"(ILjava/lang/Object;J)I"),"healing scalar signature");
                    int[] slots=world?new int[]{VerifyLineSpells.slot(m,"companionOwner"),VerifyLineSpells.slot(m,"companionTarget"),VerifyLineSpells.slot(m,"castNow")}:new int[]{resultSlot(m,P+"ClientSession","resolveBattleSpellCompanion"),castTime(m)};
                    int[] codes=world?new int[]{ALOAD,ALOAD,LLOAD}:new int[]{ALOAD,LLOAD};var removed=new ArrayList<AbstractInsnNode>();removed.add(call);var cursor=before(call);for(int at=slots.length-1;at>=0;at--){require(local(cursor,codes[at]).var==slots[at],"healing target/context role "+at);removed.add(cursor);cursor=before(cursor);}var original=after(call);require(cursor instanceof VarInsnNode amount&&amount.getOpcode()==ILOAD&&original instanceof MethodInsnNode delta&&delta.owner.equals(P+"BattleSpellCatalog$HealingSpell")&&delta.name.equals("npcHpDelta")&&delta.desc.equals("(II)I"),"healing after lawful/status scalar and before native undead conversion");erase(m,removed.toArray(AbstractInsnNode[]::new));
                }
                case "cancel" -> {
                    var actor=before(call);var snapshot=before(actor);var original=before(snapshot);require(m.name.equals("cancelMagicEffects")&&m.desc.equals("()L"+P+"BattleCompanion$MagicCancellation;")&&call.desc.equals("(L"+P+"BattleCompanion;)V")&&local(actor,ALOAD).var==0&&snapshot instanceof VarInsnNode s&&s.getOpcode()==ASTORE&&original instanceof MethodInsnNode get&&get.owner.equals(P+"BattleCompanion")&&get.name.equals("snapshot"),"cancellation after native living-target gate");erase(m,actor,call);
                }
                default -> throw new AssertionError("unexpected NPC debuff hook="+call.name);
            }
        }
        var expected=new TreeMap<String,Integer>();switch(m.name){case "useTargetNegativeStatusSpell"->expected.put("negative",1);case "useArmorBreakSpell"->expected.put("armorTarget",1);case "attack","attackClassicWorld"->{expected.put("armorActive",1);expected.put("strikerActive",1);}case "cancelMagicEffects"->expected.put("cancel",1);case "useHealingSpell"->expected.put("healingBattle",1);case "useClassicWorldHealingSpell"->expected.put("healingWorld",1);default->throw new AssertionError("unexpected NPC debuff method="+m.name);}require(counts.equals(expected),"hook counts="+counts);return m;
    }
}
