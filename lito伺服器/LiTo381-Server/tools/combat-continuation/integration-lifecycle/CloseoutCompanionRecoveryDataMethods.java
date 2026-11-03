import java.nio.file.*;
import java.util.*;
import java.util.jar.*;
import org.objectweb.asm.tree.*;
import static org.objectweb.asm.Opcodes.*;

public final class CloseoutCompanionRecoveryDataMethods {
    static final String P="local/lito381/server/",H=P+"NativeCloseoutCompanionRecoveryData",S=P+"NativeSourceNpcFactoryType$Stamp";
    static final String OLD="(Ljava/lang/String;ILjava/lang/String;II)V",EXT=OLD.replace(")","Ljava/lang/Object;)");
    static void patch(ClassNode c){
        if(c.name.equals(S)){
            if(c.fields.stream().noneMatch(f->f.name.equals("nativeCloseoutNpcRecovery")&&f.desc.equals("Ljava/lang/Object;")))throw new AssertionError("existing frozen recovery slot required");
            var old=c.methods.stream().filter(m->m.name.equals("<init>")&&m.desc.equals(OLD)).findFirst().orElseThrow();
            var extended=new MethodNode(old.access,"<init>",EXT,old.signature,old.exceptions.toArray(String[]::new));old.accept(extended);extended.maxLocals=Math.max(extended.maxLocals,7);int count=0;
            for(var n:extended.instructions.toArray())if(n.getOpcode()==RETURN){var x=new InsnList();x.add(new VarInsnNode(ALOAD,0));x.add(new VarInsnNode(ALOAD,6));x.add(new FieldInsnNode(PUTFIELD,S,"nativeCloseoutNpcRecovery","Ljava/lang/Object;"));extended.instructions.insertBefore(n,x);count++;}
            if(count!=1)throw new AssertionError("one complete original controlled constructor");c.methods.add(extended);return;
        }
        int count=0;for(var m:c.methods)if(m.name.equals("controlled"))for(var n:m.instructions.toArray())if(n instanceof MethodInsnNode call&&call.owner.equals(S)&&call.name.equals("<init>")&&call.desc.equals(OLD)){
            var x=new InsnList();x.add(new VarInsnNode(ILOAD,0));x.add(new MethodInsnNode(INVOKESTATIC,H,"capture","(I)Ljava/lang/Object;",false));m.instructions.insertBefore(n,x);call.desc=EXT;count++;
        }if(count!=2)throw new AssertionError("two existing controlled factory branches");
    }
    public static void main(String[]args)throws Exception{
        var urls=new ArrayList<java.net.URL>();urls.add(Path.of(args[0]).toUri().toURL());try(var files=Files.list(Path.of(args[2]))){for(var p:files.filter(x->x.toString().endsWith(".jar")).toList())urls.add(p.toUri().toURL());}
        try(var loader=new java.net.URLClassLoader(urls.toArray(java.net.URL[]::new),ClassLoader.getPlatformClassLoader());var jar=new JarFile(args[0])){FreezeAcceptedMethods.hierarchy=loader;for(var name:List.of("NativeSourceNpcFactoryType$Stamp","NativeSourceNpcFactoryType")){var c=FreezeAcceptedMethods.read(jar,P+name);patch(c);var out=Path.of(args[1]).resolve(c.name+".class");Files.createDirectories(out.getParent());Files.write(out,FreezeAcceptedMethods.bytes(c,true));}}
    }
}
