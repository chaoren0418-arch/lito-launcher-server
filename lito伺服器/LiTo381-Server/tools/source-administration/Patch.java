import java.nio.file.*;
import java.util.*;
import java.util.zip.*;
import java.security.*;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;

/** Narrow patch of the deployed .42 core; never rebuilds unrelated gameplay classes. */
public class Patch implements Opcodes {
    static final String P = "local/lito381/server/", A = P + "SourceAdministration";
    static final String AUTH = "(L"+P+"AccountRepository;Ljava/lang/String;L"+P+"ClientSession;)";
    static InsnList clear(int local) {
        var x = new InsnList(); x.add(new VarInsnNode(ALOAD, local));
        x.add(new MethodInsnNode(INVOKESTATIC,A,"clear","(L"+P+"ClientSession;)V",false));return x;
    }
    public static void main(String[] args) throws Exception {
        Path base=Path.of(args[0]),classes=Path.of(args[1]),out=Path.of(args[2]);
        String hash=HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(base)));
        if (!hash.equals("004b1ba8cc258f2f95a04ed3e6f1cbd63c1fe7737dc3db5d6b4c0de009130951"))
            throw new IllegalStateException("Unexpected deployed baseline");
        int authority=0,clears=0,commandReads=0,bypass=0,disconnect=0,unified=0,floors=0,commandTable=0;
        try(var z=new ZipFile(base.toFile());var o=new ZipOutputStream(Files.newOutputStream(out))) {
            for(var entries=z.entries();entries.hasMoreElements();) {
                var e=entries.nextElement();byte[] bytes=z.getInputStream(e).readAllBytes();
                if(e.getName().equals(P+"UnifiedConfiguration.class")) {
                    bytes=Files.readAllBytes(classes.resolve(e.getName()));unified++;
                }
                if(Set.of(P+"ClientSession.class",P+"GameHub.class",P+"LiToGmCommands.class",P+"SharedDatabase.class").contains(e.getName())) {
                    var c=new ClassNode();new ClassReader(bytes).accept(c,0);
                    for(var m:c.methods) {
                        if(c.name.equals(P+"ClientSession") && m.name.equals("isAdministrator")) {
                            m.instructions.clear();m.tryCatchBlocks.clear();if(m.localVariables!=null)m.localVariables.clear();
                            m.instructions.add(new VarInsnNode(ALOAD,0));
                            m.instructions.add(new MethodInsnNode(INVOKESPECIAL,c.name,"selectedSourceAccounts","()L"+P+"AccountRepository;",false));
                            m.instructions.add(new VarInsnNode(ALOAD,0));
                            m.instructions.add(new FieldInsnNode(GETFIELD,c.name,"account","Ljava/lang/String;"));
                            m.instructions.add(new VarInsnNode(ALOAD,0));
                            m.instructions.add(new MethodInsnNode(INVOKESTATIC,A,"administrator",AUTH+"Z",false));
                            m.instructions.add(new InsnNode(IRETURN));authority++;continue;
                        }
                        if(c.name.equals(P+"GameHub") && m.name.equals("disconnect") && m.desc.startsWith("(L"+P+"ClientSession;")) {
                            m.instructions.insert(clear(1));disconnect++;
                        }
                        for(var n:m.instructions.toArray()) {
                            if(c.name.equals(P+"ClientSession") && n instanceof FieldInsnNode f && f.getOpcode()==PUTFIELD
                                    && f.owner.equals(c.name) && Set.of("account","selectedClassicServerName").contains(f.name)) {
                                m.instructions.insert(n,clear(0));clears++;
                            }
                            if(n instanceof MethodInsnNode call) {
                                if(c.name.equals(P+"SharedDatabase") && m.name.equals("<clinit>")
                                        && call.owner.equals("java/util/Set") && call.name.equals("of")
                                        && Type.getArgumentTypes(call.desc).length==5) {
                                    m.instructions.insertBefore(n,new LdcInsnNode("commands"));
                                    call.desc="("+"Ljava/lang/Object;".repeat(6)+")Ljava/util/Set;";
                                    commandTable++;
                                }
                                boolean de=c.name.equals(P+"ClientSession")&&m.name.equals("handleGmChatCommand");
                                boolean gm=c.name.equals(P+"LiToGmCommands")&&m.name.equals("handle");
                                if(de && call.name.equals("isAdministrator")) {
                                    var replacement=new InsnList();replacement.add(new InsnNode(POP));replacement.add(new InsnNode(ICONST_0));
                                    m.instructions.insertBefore(n,replacement);m.instructions.remove(n);bypass++;
                                } else if((de||gm)&&call.owner.equals(P+"AccountRepository")&&call.name.equals("accessLevel")) {
                                    m.instructions.insertBefore(n,new VarInsnNode(ALOAD,0));
                                    m.instructions.set(n,new MethodInsnNode(INVOKESTATIC,A,"accessLevel",AUTH+"I",false));commandReads++;
                                } else if(de && call.owner.equals(P+"GmCommandDefinition") && call.name.equals("requiredAccessLevel")) {
                                    var floor=new InsnList();floor.add(new InsnNode(ICONST_1));
                                    floor.add(new MethodInsnNode(INVOKESTATIC,"java/lang/Math","max","(II)I",false));
                                    m.instructions.insert(n,floor);floors++;
                                }
                            }
                        }
                    }
                    var w=new ClassWriter(ClassWriter.COMPUTE_MAXS);c.accept(w);bytes=w.toByteArray();
                }
                o.putNextEntry(new ZipEntry(e.getName()));o.write(bytes);o.closeEntry();
            }
            try(var fs=Files.list(classes.resolve(P))) {
                for(Path f:fs.filter(p->p.getFileName().toString().startsWith("SourceAdministration")&&p.toString().endsWith(".class")).toList()) {
                    o.putNextEntry(new ZipEntry(P+f.getFileName()));o.write(Files.readAllBytes(f));o.closeEntry();
                }
            }
        }
        if(authority!=1||commandReads!=2||bypass!=1||disconnect!=1||unified!=1||clears<9||floors!=3||commandTable!=1)
            throw new IllegalStateException("Hook coverage: "+List.of(authority,clears,commandReads,bypass,disconnect,unified));
        System.out.println("PASS authority=1 account/source invalidations="+clears+" commands=2 bypass_removed=1 disconnect=1 config=1 lito_command_table=1");
    }
}
