import java.nio.file.*;
import java.util.*;
import java.util.jar.*;
import org.objectweb.asm.tree.*;
import static org.objectweb.asm.Opcodes.*;

public final class CloseoutCompanionMagicHealMethods {
    static final String P="local/lito381/server/",C=P+"NativeSourceNpcMagicEffects",H=P+"NativeCloseoutCompanionMagicHeal",S=C+"$Source",SCOPE=P+"NativeSourceNpcPlayer$Scope";
    static void patch(ClassNode c){
        int prefixes=0,frames=0;
        for(var m:c.methods){
            if(Set.of("source","sourceCurrent","heal").contains(m.name)){
                String arg=m.name.equals("source")?SCOPE:S;String pred=m.name.equals("source")?"controlled":"handles";
                String delegate=switch(m.name){case "source"->"capture";case "sourceCurrent"->"currentSource";default->"heal";};
                var original=new LabelNode();var x=new InsnList();x.add(new VarInsnNode(ALOAD,0));x.add(new MethodInsnNode(INVOKESTATIC,H,pred,"(L"+arg+";)Z",false));x.add(new JumpInsnNode(IFEQ,original));
                x.add(new VarInsnNode(ALOAD,0));if(m.name.equals("heal"))x.add(new VarInsnNode(ILOAD,1));x.add(new MethodInsnNode(INVOKESTATIC,H,delegate,m.desc,false));x.add(new InsnNode(m.name.equals("source")?ARETURN:m.name.equals("sourceCurrent")?IRETURN:RETURN));x.add(original);m.instructions.insert(x);prefixes++;
            }
            if(m.name.equals("allowFrame"))for(var n:m.instructions.toArray())if(n.getOpcode()==IRETURN){var x=new InsnList();x.add(new VarInsnNode(ALOAD,0));x.add(new VarInsnNode(ALOAD,1));x.add(new MethodInsnNode(INVOKESTATIC,H,"allowFrame","(ZL"+P+"ClientSession;[B)Z",false));m.instructions.insertBefore(n,x);frames++;}
        }if(prefixes!=3||frames!=1)throw new AssertionError("three source/heal branches and one final guard");
    }
    public static void main(String[]args)throws Exception{
        var urls=new ArrayList<java.net.URL>();urls.add(Path.of(args[0]).toUri().toURL());try(var files=Files.list(Path.of(args[2]))){for(var p:files.filter(x->x.toString().endsWith(".jar")).toList())urls.add(p.toUri().toURL());}
        try(var loader=new java.net.URLClassLoader(urls.toArray(java.net.URL[]::new),ClassLoader.getPlatformClassLoader());var jar=new JarFile(args[0])){FreezeAcceptedMethods.hierarchy=loader;var c=FreezeAcceptedMethods.read(jar,C);patch(c);var out=Path.of(args[1]).resolve(c.name+".class");Files.createDirectories(out.getParent());Files.write(out,FreezeAcceptedMethods.bytes(c,true));}
    }
}
