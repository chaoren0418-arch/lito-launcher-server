import java.nio.file.*;
import java.util.*;
import java.util.jar.*;
import org.objectweb.asm.tree.*;
import static org.objectweb.asm.Opcodes.*;

public final class CloseoutBattleLineOfSightMethods {
    static final String P="local/lito381/server/", C=P+"BattleSpellCatalog", A=P+"ArenaMapCatalog$Arena", H=P+"NativeCloseoutBattleLineOfSight";
    static final String D="(L"+A+";IIII)Z", LEGACY="nativeCloseoutLegacyLineOfSight", F="nativeCloseoutSourceTerrain";
    static void patch(ClassNode c) {
        if(c.name.equals(A)) {
            if(c.fields.stream().anyMatch(f->f.name.equals(F)))throw new AssertionError("fresh arena-owned terrain cache");
            c.fields.add(new FieldNode(ACC_PRIVATE|ACC_VOLATILE|ACC_TRANSIENT,F,"Ljava/lang/Object;",null,null));return;
        }
        var original=c.methods.stream().filter(m->m.name.equals("hasNativeLineOfSight")&&m.desc.equals(D)).findFirst().orElseThrow();
        var legacy=new MethodNode(original.access,LEGACY,D,original.signature,original.exceptions.toArray(String[]::new));original.accept(legacy);c.methods.add(legacy);
        original.instructions.clear();original.tryCatchBlocks.clear();original.localVariables=null;
        var x=original.instructions;var useSource=new LabelNode();
        x.add(new VarInsnNode(ALOAD,0));for(int i=1;i<=4;i++)x.add(new VarInsnNode(ILOAD,i));
        x.add(new MethodInsnNode(INVOKESTATIC,H,"source",D.replace(")Z",")Ljava/lang/Boolean;"),false));x.add(new VarInsnNode(ASTORE,5));
        x.add(new VarInsnNode(ALOAD,5));x.add(new JumpInsnNode(IFNONNULL,useSource));
        x.add(new VarInsnNode(ALOAD,0));for(int i=1;i<=4;i++)x.add(new VarInsnNode(ILOAD,i));
        x.add(new MethodInsnNode(INVOKESTATIC,C,LEGACY,D,false));x.add(new InsnNode(IRETURN));
        x.add(useSource);x.add(new VarInsnNode(ALOAD,5));x.add(new MethodInsnNode(INVOKEVIRTUAL,"java/lang/Boolean","booleanValue","()Z",false));x.add(new InsnNode(IRETURN));
        original.maxLocals=6;original.maxStack=5;
    }
    public static void main(String[]args)throws Exception {
        var urls=new ArrayList<java.net.URL>();urls.add(Path.of(args[0]).toUri().toURL());try(var files=Files.list(Path.of(args[2]))){for(var p:files.filter(x->x.toString().endsWith(".jar")).toList())urls.add(p.toUri().toURL());}
        try(var loader=new java.net.URLClassLoader(urls.toArray(java.net.URL[]::new),ClassLoader.getPlatformClassLoader());var jar=new JarFile(args[0])) {
            FreezeAcceptedMethods.hierarchy=loader;
            for(var name:List.of(C,A)) {var c=FreezeAcceptedMethods.read(jar,name);patch(c);var out=Path.of(args[1]).resolve(c.name+".class");Files.createDirectories(out.getParent());Files.write(out,FreezeAcceptedMethods.bytes(c,true));}
        }
    }
}
