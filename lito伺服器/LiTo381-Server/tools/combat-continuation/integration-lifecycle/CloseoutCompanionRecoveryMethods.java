import java.nio.file.*;
import java.util.*;
import java.util.jar.*;
import org.objectweb.asm.tree.*;
import static org.objectweb.asm.Opcodes.*;

public final class CloseoutCompanionRecoveryMethods {
    static final String P="local/lito381/server/",H=P+"NativeCloseoutCompanionRecovery";
    static void patch(ClassNode c){
        if(c.name.equals(P+"BattleCompanion")||c.name.equals(P+"NativeCompanionPlacement$State")){
            String field=c.name.endsWith("BattleCompanion")?"profile":"pending";
            var f=c.fields.stream().filter(x->x.name.equals(field)).findFirst().orElseThrow();
            if((f.access&(ACC_FINAL|ACC_VOLATILE))!=0)throw new AssertionError("review field access "+field);f.access|=ACC_VOLATILE;return;
        }
        if(c.name.equals(P+"NativeSourceNpcMagicEffects")){
            int count=0;for(var m:c.methods)if(m.name.equals("allowFrame")&&m.desc.equals("(L"+P+"ClientSession;[B)Z"))for(var n:m.instructions.toArray())if(n.getOpcode()==IRETURN){
                var x=new InsnList();x.add(new VarInsnNode(ALOAD,0));x.add(new VarInsnNode(ALOAD,1));x.add(new MethodInsnNode(INVOKESTATIC,H,"allowFrame","(ZL"+P+"ClientSession;[B)Z",false));m.instructions.insertBefore(n,x);count++;
            }if(count!=1)throw new AssertionError("one original final-frame result");return;
        }
        c.fields.add(new FieldNode(ACC_PRIVATE|ACC_VOLATILE,"nativeCloseoutCompanionRecoveryClock","Ljava/lang/Object;",null,null));int open=0,close=0;
        for(var m:c.methods){
            if(m.name.equals("<init>"))for(var n:m.instructions.toArray())if(n instanceof MethodInsnNode call&&call.owner.equals(P+"NativeCloseoutNpcRecovery")&&call.name.equals("init")){var x=new InsnList();x.add(new VarInsnNode(ALOAD,0));x.add(new MethodInsnNode(INVOKESTATIC,H,"init","(L"+P+"GameHub;)V",false));m.instructions.insert(n,x);open++;}
            if(m.name.equals("close")&&m.desc.equals("()V")){var x=new InsnList();x.add(new VarInsnNode(ALOAD,0));x.add(new MethodInsnNode(INVOKESTATIC,H,"close","(L"+P+"GameHub;)V",false));m.instructions.insert(x);close++;}
        }if(open!=1||close!=1)throw new AssertionError("one Hub open/close seam");
    }
    public static void main(String[]args)throws Exception{
        var urls=new ArrayList<java.net.URL>();urls.add(Path.of(args[0]).toUri().toURL());try(var files=Files.list(Path.of(args[2]))){for(var p:files.filter(x->x.toString().endsWith(".jar")).toList())urls.add(p.toUri().toURL());}
        try(var loader=new java.net.URLClassLoader(urls.toArray(java.net.URL[]::new),ClassLoader.getPlatformClassLoader());var jar=new JarFile(args[0])){FreezeAcceptedMethods.hierarchy=loader;for(var name:List.of("GameHub","BattleCompanion","NativeCompanionPlacement$State","NativeSourceNpcMagicEffects")){var c=FreezeAcceptedMethods.read(jar,P+name);patch(c);var out=Path.of(args[1]).resolve(c.name+".class");Files.createDirectories(out.getParent());Files.write(out,FreezeAcceptedMethods.bytes(c,true));}}
    }
}
