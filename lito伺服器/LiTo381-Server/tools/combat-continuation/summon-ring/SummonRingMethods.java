import java.nio.file.*;
import java.net.*;
import java.util.*;
import java.util.jar.*;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
import static org.objectweb.asm.Opcodes.*;

/** Source C_UseSkill accepts both 20284 and 120284; preserve all other ability selectors. */
public final class SummonRingMethods {
    static final String TYPE="local/lito381/server/InventoryCatalog$Definition";
    static ClassNode transform(byte[] bytes,ClassLoader loader)throws Exception {
        var c=NativeSkillConsumptionMethods.read(bytes);
        var m=NativeSkillConsumptionMethods.method(c,"controlAbilityType","()I");
        var original=new LabelNode();var code=new InsnList();
        code.add(new VarInsnNode(ALOAD,0));code.add(new FieldInsnNode(GETFIELD,TYPE,"catalogId","J"));
        code.add(new LdcInsnNode(120284L));code.add(new InsnNode(LCMP));code.add(new JumpInsnNode(IFNE,original));
        code.add(new InsnNode(ICONST_5));code.add(new InsnNode(IRETURN));code.add(original);m.instructions.insert(code);
        var framed=NativeSkillConsumptionMethods.frames(c,List.of(m),loader);
        c.methods.set(c.methods.indexOf(m),NativeSkillConsumptionMethods.method(framed,m.name,m.desc));return c;
    }
    public static void main(String[] args)throws Exception {
        try(var old=new JarFile(args[1]);var loader=new URLClassLoader(new URL[]{Path.of(args[1]).toUri().toURL()},ClassLoader.getPlatformClassLoader())){
            byte[] bytes=old.getInputStream(old.getJarEntry(TYPE+".class")).readAllBytes();var changed=transform(bytes,loader);
            if(args[0].equals("splice")){
                var p=Path.of(args[2]).resolve(TYPE+".class");Files.createDirectories(p.getParent());Files.write(p,NativeSkillConsumptionMethods.bytes(changed,false));
            }else try(var candidate=new JarFile(args[2])){
                var actual=NativeSkillConsumptionMethods.read(candidate.getInputStream(candidate.getJarEntry(TYPE+".class")).readAllBytes());
                if(!Arrays.equals(NativeSkillConsumptionMethods.bytes(actual,true),NativeSkillConsumptionMethods.bytes(changed,true)))throw new AssertionError("transform mismatch");
                var parent=NativeSkillConsumptionMethods.read(bytes);var original=NativeSkillConsumptionMethods.method(parent,"controlAbilityType","()I");
                actual.methods.set(actual.methods.indexOf(NativeSkillConsumptionMethods.method(actual,original.name,original.desc)),original);
                if(!Arrays.equals(NativeSkillConsumptionMethods.bytes(actual,true),NativeSkillConsumptionMethods.bytes(parent,true)))throw new AssertionError("unrelated item methods changed");
            }
        }System.out.println("PASS one additional Source item id; unrelated-method inverse exact");
    }
}
