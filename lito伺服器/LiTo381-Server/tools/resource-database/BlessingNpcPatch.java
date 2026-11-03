import java.util.*;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
final class BlessingNpcPatch implements Opcodes {
    static final String P="local/lito381/server/",H=P+"CombatSpecialBuffs",N=P+"ClassicWorldNpcCatalog$StaticNpc",M=P+"ClassicWorldMapCatalog$ClassicMap";
    static boolean accepts(String path){return path.equals(P+"ClassicWorldNpcCatalog.class")||path.equals(P+"ClassicWorldMapCatalog.class");}
    static void apply(ClassNode c,Map<String,Integer> hooks) {
        int count=0;
        for(var m:c.methods) {
            if(c.name.equals(P+"ClassicWorldNpcCatalog")&&m.name.equals("all")&&m.desc.equals("(I)Ljava/util/List;"))for(var n:m.instructions.toArray())if(n.getOpcode()==ARETURN) {
                var i=new InsnList();i.add(new VarInsnNode(ILOAD,0));i.add(new MethodInsnNode(INVOKESTATIC,H,"npcs","(Ljava/util/List;I)Ljava/util/List;",false));m.instructions.insertBefore(n,i);count++;
            }
            if(c.name.equals(P+"ClassicWorldMapCatalog")) {
                if(m.name.equals("forMap")) {
                    var i=new InsnList();var fallback=new LabelNode();i.add(new VarInsnNode(ILOAD,0));i.add(new MethodInsnNode(INVOKESTATIC,H,"sourceMap","(I)L"+M+";",false));i.add(new InsnNode(DUP));i.add(new JumpInsnNode(IFNULL,fallback));i.add(new InsnNode(ARETURN));i.add(fallback);i.add(new FrameNode(F_SAME1,0,null,1,new Object[]{M}));i.add(new InsnNode(POP));m.instructions.insert(i);count++;
                }
                if(m.name.equals("supports"))for(var n:m.instructions.toArray())if(n.getOpcode()==IRETURN) {
                    var i=new InsnList();i.add(new VarInsnNode(ILOAD,0));i.add(new MethodInsnNode(INVOKESTATIC,H,"mapSupported","(ZI)Z",false));m.instructions.insertBefore(n,i);count++;
                }
                if(m.name.equals("supportedMapIds"))for(var n:m.instructions.toArray())if(n.getOpcode()==ARETURN) {m.instructions.insertBefore(n,new MethodInsnNode(INVOKESTATIC,H,"mapIds","(Ljava/util/List;)Ljava/util/List;",false));count++;}
            }
            if(c.name.equals(P+"ClientSession")) {
                if(m.name.equals("openClassicWorldHypertext"))for(var n:m.instructions.toArray())if(n instanceof MethodInsnNode call&&call.owner.equals(c.name)&&call.name.equals("isClassicWorldWaterDragonSaell")) {
                    var i=new InsnList();i.add(new VarInsnNode(ALOAD,DragonPatch.local(m,"npc")));i.add(new MethodInsnNode(INVOKESTATIC,H,"blessingNpc","(ZL"+N+";)Z",false));m.instructions.insert(n,i);count++;
                }
                if(m.name.equals("applyClassicWorldWaterDragonSaellBlessing")) {
                    var i=new InsnList();var fallback=new LabelNode();i.add(new VarInsnNode(ALOAD,0));i.add(new VarInsnNode(ALOAD,1));i.add(new MethodInsnNode(INVOKESTATIC,H,"tryTalk","(L"+P+"ClientSession;L"+N+";)Z",false));i.add(new JumpInsnNode(IFEQ,fallback));i.add(new InsnNode(RETURN));i.add(fallback);i.add(new FrameNode(F_SAME,0,null,0,null));m.instructions.insert(i);count++;
                }
            }
        }
        int expected=c.name.equals(P+"ClassicWorldNpcCatalog")?1:c.name.equals(P+"ClassicWorldMapCatalog")?3:c.name.equals(P+"ClientSession")?2:0;
        CombatPatch.require(count,expected,"source blessing NPC/map hooks "+c.name);if(count>0)hooks.put("source-blessing-npcs-"+c.name.substring(P.length()),count);
    }
}
