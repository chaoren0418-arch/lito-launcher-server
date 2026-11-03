import java.nio.file.*;
import java.io.*;
import jdk.internal.org.objectweb.asm.*;
import jdk.internal.org.objectweb.asm.tree.*;
import jdk.internal.org.objectweb.asm.util.*;

/** Only expand the proved class domain in the already-qualified candidate. */
public final class ClassIconServerPatch {
    static String text(MethodNode m) {
        var p=new Textifier();m.accept(new TraceMethodVisitor(p));
        var w=new StringWriter();p.print(new PrintWriter(w));return w.toString();
    }
    public static void main(String[] args)throws Exception {
        var r=new ClassReader(Files.readAllBytes(Path.of(args[0])));
        var before=new ClassNode(Opcodes.ASM8);var after=new ClassNode(Opcodes.ASM8);
        r.accept(before,0);r.accept(after,0);int changes=0;
        boolean protocol=after.name.equals("local/lito381/server/Protocol");
        if(!protocol&&!after.name.equals("local/lito381/server/ClientSession"))throw new AssertionError();
        String target=protocol?"litoRoomParticipant":"sendRoomParticipant";
        for(var m:after.methods) {
            if(!m.name.equals(target))continue;
            if(!protocol&&!m.desc.startsWith("(JLlocal/lito381/server/ClientSession;"))continue;
            for(var n:m.instructions.toArray()) {
                if(n.getOpcode()==Opcodes.ICONST_4 && n.getPrevious() instanceof VarInsnNode v
                    && v.getOpcode()==Opcodes.ILOAD && v.var==(protocol?4:7)
                    && n.getNext() instanceof JumpInsnNode j
                    && j.getOpcode()==(protocol?Opcodes.IF_ICMPGT:Opcodes.IF_ICMPLE)) {
                    m.instructions.set(n,new IntInsnNode(Opcodes.BIPUSH,6));changes++;
                }
            }
        }
        if(changes!=1)throw new AssertionError("class bound seam count="+changes);
        var w=new ClassWriter(r,0);after.accept(w);
        var check=new ClassNode(Opcodes.ASM8);new ClassReader(w.toByteArray()).accept(check,0);
        int edited=0;
        for(int i=0;i<before.methods.size();i++) {
            if(!text(before.methods.get(i)).equals(text(check.methods.get(i)))) {
                if(!before.methods.get(i).name.equals(target))throw new AssertionError("unrelated method changed");
                edited++;
            }
        }
        if(edited!=1)throw new AssertionError("edited methods="+edited);
        Files.write(Path.of(args[1]),w.toByteArray());
        System.out.println("PASS "+after.name+": one class bound changed 4 -> 6; unrelated methods unchanged");
    }
}
