import java.nio.file.*;
import java.util.*;
import java.util.zip.*;
import java.security.*;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;

/** Preserve every previously deployed patch; change only the verified call sites. */
public class Patch implements Opcodes {
    static final String P="local/lito381/server/", H=P+"DragonSkillRequests";
    public static void main(String[] args)throws Exception {
        Path base=Path.of(args[0]),classes=Path.of(args[1]),out=Path.of(args[2]);
        if(!HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(base)))
            .equals("205dc1baf5598523e67899e6c92a4b9bb07494d42dfa995cd741e67330d9b542"))
            throw new IllegalStateException("Unexpected deployed baseline");
        int dispatch=0,primary=0,guard=0,fear=0,npc=0,mr=0,result=0;
        try(var z=new ZipFile(base.toFile());var o=new ZipOutputStream(Files.newOutputStream(out))) {
            for(var it=z.entries();it.hasMoreElements();) {
                var e=it.nextElement();byte[] data=z.getInputStream(e).readAllBytes();
                if(e.getName().equals(P+"ClientSession.class")) {
                    var c=new ClassNode();new ClassReader(data).accept(c,0);
                    for(var m:new ArrayList<>(c.methods)) {
                        if(m.name.equals("useSpell")&&m.desc.equals("([B)V")) {
                            m.name="useSpellBeforeDragonTargets";
                            var wrapper=new MethodNode(ACC_PRIVATE,"useSpell","([B)V",null,new String[]{"java/io/IOException"});
                            wrapper.instructions.add(new VarInsnNode(ALOAD,0));wrapper.instructions.add(new VarInsnNode(ALOAD,1));
                            wrapper.instructions.add(new MethodInsnNode(INVOKESTATIC,H,"dispatch","(L"+P+"ClientSession;[B)V",false));
                            wrapper.instructions.add(new InsnNode(RETURN));c.methods.add(wrapper);dispatch++;
                        }
                        if(m.name.equals("resolveSelectorOnlyPrimary")) {
                            var i=new InsnList();var normal=new LabelNode();
                            i.add(new MethodInsnNode(INVOKESTATIC,H,"current","()L"+P+"Protocol$AttackSpellRequest;",false));
                            i.add(new InsnNode(DUP));i.add(new JumpInsnNode(IFNULL,normal));i.add(new InsnNode(ARETURN));
                            i.add(normal);i.add(new FrameNode(F_SAME1,0,null,1,new Object[]{P+"Protocol$AttackSpellRequest"}));
                            i.add(new InsnNode(POP));m.instructions.insert(i);primary++;
                        }
                        for(var n:m.instructions.toArray()) {
                            if(m.name.equals("recordBattleResult") && n instanceof MethodInsnNode call
                                && call.owner.equals(P+"GameStateStore") && call.name.equals("recordResult")) {
                                m.instructions.insertBefore(n,new VarInsnNode(ALOAD,0));
                                String desc="(L"+P+"GameStateStore;"+call.desc.substring(1,call.desc.indexOf(')'))+"L"+P+"ClientSession;"+call.desc.substring(call.desc.indexOf(')'));
                                m.instructions.set(n,new MethodInsnNode(INVOKESTATIC,P+"DisconnectedBattleResult","record",desc,false));result++;
                            }
                            if(n instanceof MethodInsnNode call && Set.of("resolveDragonKnightNegativeTarget","resolveFoeSlayerTarget").contains(m.name)
                                && call.name.equals("resolveTarget")) {
                                if(call.owner.equals(P+"BattleGuardBrake")){call.owner=H;call.name="guardTarget";guard++;}
                                else if(call.owner.equals(P+"BattleResistFear")){call.owner=H;call.name="fearTarget";fear++;}
                            }
                            if(m.name.equals("useResistFearSpell")) {
                                if(n instanceof VarInsnNode v && v.getOpcode()==ISTORE && v.var==16) {
                                    var i=new InsnList();i.add(new VarInsnNode(ALOAD,12));
                                    i.add(new MethodInsnNode(INVOKESTATIC,H,"fearNpcId","(ILjava/lang/Object;)I",false));
                                    m.instructions.insertBefore(n,i);npc++;
                                }
                                if(n instanceof MethodInsnNode call && call.owner.equals(P+"SourceNpcPhysicalStats")&&call.name.equals("magicResistance")) {
                                    var i=new InsnList();i.add(new VarInsnNode(ALOAD,12));i.add(new VarInsnNode(LLOAD,25));m.instructions.insertBefore(n,i);
                                    m.instructions.set(n,new MethodInsnNode(INVOKESTATIC,H,"fearMagicResistance","(L"+P+"SourceNpcPhysicalStats;Ljava/lang/Object;J)I",false));mr++;
                                }
                            }
                        }
                    }
                    var w=new ClassWriter(ClassWriter.COMPUTE_MAXS);c.accept(w);data=w.toByteArray();
                }
                o.putNextEntry(new ZipEntry(e.getName()));o.write(data);o.closeEntry();
            }
            for(String name:List.of("DragonSkillRequests","DisconnectedBattleResult")) {
                String helper=P+name+".class";o.putNextEntry(new ZipEntry(helper));o.write(Files.readAllBytes(classes.resolve(helper)));o.closeEntry();
            }
        }
        if(result!=1 || !List.of(dispatch,primary,guard,fear,npc,mr).equals(List.of(1,1,2,1,1,1)))
            throw new IllegalStateException("Unexpected hook coverage "+List.of(dispatch,primary,guard,fear,npc,mr));
        System.out.println("PASS dispatch=1 explicit-primary=1 strict-guard=2 strict-fear=1 companion-npc=1 companion-mr=1");
    }
}
