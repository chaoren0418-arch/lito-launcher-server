import java.nio.file.*;
import java.util.*;
import java.util.jar.*;
import org.objectweb.asm.tree.*;
import static org.objectweb.asm.Opcodes.*;

public final class CloseoutCompanionEvolutionMethods {
    static final String P="local/lito381/server/",C=P+"BattleCompanion",H=P+"NativeCloseoutCompanionEvolution",F="nativeSourceNpcFactoryType";
    static void patch(ClassNode c){
        var field=c.fields.stream().filter(f->f.name.equals(F)).findFirst().orElseThrow();if(field.access!=ACC_FINAL||!field.desc.equals("Ljava/lang/Object;"))throw new AssertionError("review original factory proof field");field.access=ACC_VOLATILE;
        int count=0;for(var m:c.methods)if(m.name.equals("evolve")){
            if((m.access&ACC_SYNCHRONIZED)==0)throw new AssertionError("original pet monitor required");int slot=m.maxLocals++;
            for(var n:m.instructions.toArray())if(n instanceof FieldInsnNode f&&f.getOpcode()==PUTFIELD&&f.owner.equals(C)&&f.name.equals("profile")){
                var before=new InsnList();before.add(new InsnNode(DUP2));before.add(new MethodInsnNode(INVOKESTATIC,H,"capture","(L"+C+";L"+C+"$Profile;)Ljava/lang/Object;",false));before.add(new VarInsnNode(ASTORE,slot));m.instructions.insertBefore(n,before);
                var after=new InsnList();after.add(new VarInsnNode(ALOAD,0));after.add(new VarInsnNode(ALOAD,slot));after.add(new FieldInsnNode(PUTFIELD,C,F,"Ljava/lang/Object;"));m.instructions.insert(n,after);count++;
            }
        }if(count!=1)throw new AssertionError("one accepted evolution profile write");
    }
    public static void main(String[]args)throws Exception{
        var urls=new ArrayList<java.net.URL>();urls.add(Path.of(args[0]).toUri().toURL());try(var files=Files.list(Path.of(args[2]))){for(var p:files.filter(x->x.toString().endsWith(".jar")).toList())urls.add(p.toUri().toURL());}
        try(var loader=new java.net.URLClassLoader(urls.toArray(java.net.URL[]::new),ClassLoader.getPlatformClassLoader());var jar=new JarFile(args[0])){FreezeAcceptedMethods.hierarchy=loader;var c=FreezeAcceptedMethods.read(jar,C);patch(c);var out=Path.of(args[1]).resolve(c.name+".class");Files.createDirectories(out.getParent());Files.write(out,FreezeAcceptedMethods.bytes(c,true));}
    }
}
