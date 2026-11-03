import java.nio.file.*;
import java.io.*;
import jdk.internal.org.objectweb.asm.*;
import jdk.internal.org.objectweb.asm.tree.*;
import jdk.internal.org.objectweb.asm.util.*;

public final class SelectionRecoveryPatch {
    static String text(MethodNode m) {
        var printer=new Textifier();m.accept(new TraceMethodVisitor(printer));
        var out=new StringWriter();printer.print(new PrintWriter(out));return out.toString();
    }
    public static void main(String[] args)throws Exception {
        var reader=new ClassReader(Files.readAllBytes(Path.of(args[0])));
        var original=new ClassNode(Opcodes.ASM8);var changed=new ClassNode(Opcodes.ASM8);
        reader.accept(original,0);reader.accept(changed,0);
        int constructors=0,details=0,cleanup=0;
        for(var method:changed.methods) {
            if(changed.name.equals("local/lito381/server/InventoryCatalog$NativeArmorStatus")) {
                if(method.name.equals("<init>")) {
                    for(var n:method.instructions.toArray()) {
                        if(n instanceof IntInsnNode limit && limit.operand==255
                            && n.getPrevious() instanceof VarInsnNode field
                            && field.getOpcode()==Opcodes.ILOAD && field.var==1) {
                            method.instructions.set(n,new LdcInsnNode(Integer.MAX_VALUE));constructors++;
                        }
                    }
                } else if(method.name.equals("detail")) {
                    for(var n:method.instructions.toArray()) {
                        if(n instanceof FieldInsnNode field && field.name.equals("displayArmorClass")
                            && field.getOpcode()==Opcodes.GETFIELD) {
                            var code=new InsnList();code.add(new IntInsnNode(Opcodes.SIPUSH,255));
                            code.add(new MethodInsnNode(Opcodes.INVOKESTATIC,"java/lang/Math","min","(II)I",false));
                            method.instructions.insert(n,code);details++;
                        }
                    }
                }
            } else if(changed.name.equals("local/lito381/server/ClientSession") && method.name.equals("run")) {
                for(var n:method.instructions.toArray()) {
                    if(n instanceof MethodInsnNode call && call.owner.equals("local/lito381/server/NativeServerLifetime")
                        && call.name.equals("leaveSession")) {
                        var code=new InsnList();code.add(new InsnNode(Opcodes.DUP));
                        code.add(new MethodInsnNode(Opcodes.INVOKESTATIC,
                            "local/lito381/server/SelectionRecovery","finish",
                            "(Llocal/lito381/server/ClientSession;)V",false));
                        method.instructions.insertBefore(n,code);cleanup++;
                    }
                }
            }
        }
        if(!((constructors==1&&details==1&&cleanup==0)||(constructors==0&&details==0&&cleanup==2)))
            throw new AssertionError("seam mismatch "+constructors+"/"+details+"/"+cleanup);
        var writer=new ClassWriter(reader,ClassWriter.COMPUTE_MAXS);changed.accept(writer);
        var checked=new ClassNode(Opcodes.ASM8);new ClassReader(writer.toByteArray()).accept(checked,0);
        int unchanged=0;
        for(int i=0;i<original.methods.size();i++) {
            var a=original.methods.get(i);var b=checked.methods.get(i);
            if(text(a).equals(text(b)))unchanged++;
            else if(!(cleanup==2?a.name.equals("run"):(a.name.equals("<init>")||a.name.equals("detail"))))
                throw new AssertionError("unrelated method changed "+a.name);
        }
        Files.write(Path.of(args[1]),writer.toByteArray());
        System.out.println("PASS "+changed.name+" unchanged-methods="+unchanged);
    }
}
