import java.nio.file.*;
import java.util.*;
import java.util.jar.*;
import org.objectweb.asm.tree.*;
import static org.objectweb.asm.Opcodes.*;

public final class CloseoutControlledNpcDataMethods {
    static final String P="local/lito381/server/",H=P+"NativeCloseoutControlledNpcData",B=H+"$Birth",S=P+"NativeSourceNpcFactoryType$Stamp",F="nativeCloseoutControlledNpcData";
    static final String OLD="(Ljava/lang/String;ILjava/lang/String;IILjava/lang/Object;)V",EXT=OLD.replace(")","Ljava/lang/Object;)");
    static void patch(ClassNode c){
        if(c.name.equals(S)){
            c.fields.add(new FieldNode(ACC_FINAL,F,"Ljava/lang/Object;",null,null));
            var old=c.methods.stream().filter(m->m.name.equals("<init>")&&m.desc.equals(OLD)).findFirst().orElseThrow();
            var extended=new MethodNode(old.access,"<init>",EXT,old.signature,old.exceptions.toArray(String[]::new));old.accept(extended);extended.maxLocals=Math.max(extended.maxLocals,8);int count=0;
            for(var n:extended.instructions.toArray())if(n.getOpcode()==RETURN){var x=new InsnList();x.add(new VarInsnNode(ALOAD,0));x.add(new VarInsnNode(ALOAD,7));x.add(new FieldInsnNode(PUTFIELD,S,F,"Ljava/lang/Object;"));extended.instructions.insertBefore(n,x);count++;}
            if(count!=1)throw new AssertionError("complete controlled birth constructor");c.methods.add(extended);return;
        }
        int captures=0,constructors=0;for(var m:c.methods)if(m.name.equals("controlled"))for(var n:m.instructions.toArray())if(n instanceof MethodInsnNode call){
            if(call.owner.equals(P+"NativeCloseoutCompanionRecoveryData")&&call.name.equals("capture")){
                if(!call.desc.equals("(I)Ljava/lang/Object;"))throw new AssertionError("review original recovery capture");call.owner=H;call.desc="(I)L"+B+";";
                var x=new InsnList();x.add(new InsnNode(DUP));x.add(new MethodInsnNode(INVOKEVIRTUAL,B,"recovery","()Ljava/lang/Object;",false));x.add(new InsnNode(SWAP));x.add(new MethodInsnNode(INVOKEVIRTUAL,B,"skills","()Ljava/lang/Object;",false));m.instructions.insert(n,x);captures++;
            }
            if(call.owner.equals(S)&&call.name.equals("<init>")&&call.desc.equals(OLD)){call.desc=EXT;constructors++;}
        }if(captures!=2||constructors!=2)throw new AssertionError("two controlled factory branches");
    }
    public static void main(String[]args)throws Exception{
        var urls=new ArrayList<java.net.URL>();urls.add(Path.of(args[0]).toUri().toURL());try(var files=Files.list(Path.of(args[2]))){for(var p:files.filter(x->x.toString().endsWith(".jar")).toList())urls.add(p.toUri().toURL());}
        try(var loader=new java.net.URLClassLoader(urls.toArray(java.net.URL[]::new),ClassLoader.getPlatformClassLoader());var jar=new JarFile(args[0])){FreezeAcceptedMethods.hierarchy=loader;for(var name:List.of("NativeSourceNpcFactoryType$Stamp","NativeSourceNpcFactoryType")){var c=FreezeAcceptedMethods.read(jar,P+name);patch(c);var out=Path.of(args[1]).resolve(c.name+".class");Files.createDirectories(out.getParent());Files.write(out,FreezeAcceptedMethods.bytes(c,true));}}
    }
}
