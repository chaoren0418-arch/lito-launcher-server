import java.util.*;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
import org.objectweb.asm.tree.analysis.*;

final class KiringkuPatch implements Opcodes {
    static final String P="local/lito381/server/",H=P+"CombatKiringku",D=H+"$Defense",W=P+"SourceWeaponPhysicalStats",C=P+"ClientSession";
    static VarInsnNode load(SourceValue value,int opcode){return MagicDamagePatch.load(value,opcode);}
    static void apply(ClassNode c,Map<String,Integer> hooks)throws AnalyzerException {
        int projections=0,finishes=0,hits=0;
        for(var m:c.methods)if(Set.of("attack","attackClassicWorld","attackClassicWorldPlayer").contains(m.name)) {
            var nodes=m.instructions.toArray();if(Arrays.stream(nodes).noneMatch(v->v instanceof MethodInsnNode x&&x.owner.equals(P+"BattleWeaponCritical")&&x.name.equals("apply")))continue;var frames=new Analyzer<>(new SourceInterpreter()).analyze(c.name,m);int weapon=-1,critical=-1,armor=-1;
            for(int n=0;n<nodes.length;n++)if(nodes[n] instanceof MethodInsnNode call&&call.owner.equals(P+"BattleWeaponCritical")&&call.name.equals("apply")) {
                weapon=load(frames[n].getStack(frames[n].getStackSize()-4),ALOAD).var;
                var next=DragonPatch.next(call);if(!(next instanceof VarInsnNode store)||store.getOpcode()!=ASTORE)throw new IllegalStateException("missing Kiringku critical projection");critical=store.var;
            }
            for(int n=0;n<nodes.length;n++)if(nodes[n] instanceof MethodInsnNode call&&call.owner.equals(P+"BattleArmorBreak")&&call.name.equals("damageAfterBreak")) {
                int value=load(frames[n].getStack(frames[n].getStackSize()-2),ILOAD).var;if(armor!=-1&&armor!=value)throw new IllegalStateException("ambiguous Kiringku Armor Break context");armor=value;
            }
            if(armor<0&&!m.name.equals("attackClassicWorldPlayer"))throw new IllegalStateException("missing source Kiringku Armor Break context "+m.name);
            if(weapon<0||critical<0)throw new IllegalStateException("missing Kiringku source weapon context");
            int slot=MagicDamagePatch.appendLocal(c,m,D),sites=0;
            for(int n=0;n<nodes.length;n++)if(nodes[n] instanceof MethodInsnNode call) {
                if(call.owner.equals(P+"BattlePhysicalDefense")&&Set.of("againstNpc","againstPlayer").contains(call.name)) {
                    var i=new InsnList();i.add(new InsnNode(DUP));boolean player=call.name.equals("againstPlayer");
                    if(player)i.add(new VarInsnNode(ILOAD,load(frames[n].getStack(frames[n].getStackSize()-6),ILOAD).var));i.add(new VarInsnNode(ALOAD,weapon));
                    i.add(new MethodInsnNode(INVOKESTATIC,H,player?"player":"npc","(L"+P+"BattlePhysicalDefense$"+(player?"PlayerOutcome;I":"NpcOutcome;")+"L"+W+";)L"+D+";",false));i.add(new VarInsnNode(ASTORE,slot));m.instructions.insert(call,i);projections++;
                }
                if(call.owner.equals(P+"BattleFoeSlayerSequence")&&call.name.equals("next")) {
                    int hit=DragonPatch.strikeLocals(m,call).hit();var i=new InsnList();i.add(new VarInsnNode(ILOAD,hit));i.add(new VarInsnNode(ALOAD,weapon));i.add(new MethodInsnNode(INVOKESTATIC,H,"hit","(ZL"+W+";)Z",false));i.add(new VarInsnNode(ISTORE,hit));m.instructions.insertBefore(DragonPatch.targetProducer(call).get(0),i);hits++;
                }
                if(call.owner.equals(P+"CombatClassDamage")&&call.name.equals("physical")) {
                    MethodInsnNode strike=null;for(var v:nodes)if(v instanceof MethodInsnNode x&&x.owner.equals(P+"BattleFoeSlayerSequence")&&x.name.equals("next"))strike=x;
                    int hit=DragonPatch.strikeLocals(m,strike).hit();var i=new InsnList();i.add(new VarInsnNode(ALOAD,weapon));i.add(new VarInsnNode(ALOAD,critical));i.add(new VarInsnNode(ALOAD,slot));i.add(new VarInsnNode(ILOAD,hit));
                    i.add(m.name.equals("attackClassicWorldPlayer")?new InsnNode(ICONST_0):new VarInsnNode(ILOAD,armor));m.instructions.insertBefore(call,i);
                    m.instructions.set(call,new MethodInsnNode(INVOKESTATIC,H,"finish","(IL"+C+";L"+C+";L"+W+";L"+P+"BattleWeaponCritical$DamageOutcome;L"+D+";ZZ)I",false));finishes++;sites++;
                }
            }
            CombatPatch.require(sites,1,"source Kiringku final damage "+m.name);
        }
        CombatPatch.require(projections,4,"source Kiringku original defense values");CombatPatch.require(finishes,3,"source Kiringku native damage paths");CombatPatch.require(hits,3,"source Kiringku automatic hit paths");
        hooks.put("source-kiringku-defense-values",projections);hooks.put("source-kiringku-finish",finishes);hooks.put("source-kiringku-hit",hits);
    }
}
