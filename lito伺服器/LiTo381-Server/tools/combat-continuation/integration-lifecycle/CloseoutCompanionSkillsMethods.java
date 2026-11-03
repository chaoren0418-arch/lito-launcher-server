import java.nio.file.*;
import java.util.*;
import java.util.jar.*;
import org.objectweb.asm.tree.*;
import static org.objectweb.asm.Opcodes.*;

public final class CloseoutCompanionSkillsMethods {
    static final String P="local/lito381/server/",C=P+"BattleCompanion",H=P+"NativeCloseoutCompanionSkills",F="nativeCloseoutMobSkillState",D="(L"+C+";)V";
    static InsnList call(String name,int slot){var x=new InsnList();x.add(new VarInsnNode(ALOAD,slot));x.add(new MethodInsnNode(INVOKESTATIC,H,name,D,false));return x;}
    static void patch(ClassNode c){
        if(c.name.equals(C)){
            if(c.fields.stream().anyMatch(f->f.name.equals(F)))throw new AssertionError("state already installed");
            c.fields.add(new FieldNode(ACC_VOLATILE,F,"Ljava/lang/Object;",null,null));
            int installs=0,terminals=0;
            for(var m:c.methods)for(var n:m.instructions.toArray())if(n instanceof FieldInsnNode f&&f.getOpcode()==PUTFIELD&&f.owner.equals(C)){
                if(f.name.equals("nativeSourceNpcFactoryType")){
                    if(!m.name.equals("<init>")&&!m.name.equals("evolve"))throw new AssertionError("unexpected factory writer");
                    checkLock(m);m.instructions.insert(n,call("install",0));installs++;
                }
                if(m.name.equals("applyDamage")&&f.name.equals("hp") || f.name.equals("active")&&Set.of("deactivate","apply","applyDamage").contains(m.name)){
                    checkLock(m);m.instructions.insert(n,call("terminal",0));terminals++;
                }
            }
            if(installs!=2||terminals!=4)throw new AssertionError("actor seams "+installs+"/"+terminals);
        }else{
            int ticks=0;
            for(var m:c.methods)if(Set.of("serverCompanionTick","serverClassicWorldCompanionTick").contains(m.name)&&m.desc.startsWith("(L"+C+";")){
                m.instructions.insert(call("tick",1));ticks++;
            }
            if(ticks!=2)throw new AssertionError("two actual companion clocks");
        }
    }
    static void checkLock(MethodNode m){if(!m.name.equals("<init>")&&(m.access&ACC_SYNCHRONIZED)==0)throw new AssertionError("original actor monitor required");}
    public static void main(String[]args)throws Exception{
        var urls=new ArrayList<java.net.URL>();urls.add(Path.of(args[0]).toUri().toURL());try(var files=Files.list(Path.of(args[2]))){for(var p:files.filter(x->x.toString().endsWith(".jar")).toList())urls.add(p.toUri().toURL());}
        try(var loader=new java.net.URLClassLoader(urls.toArray(java.net.URL[]::new),ClassLoader.getPlatformClassLoader());var jar=new JarFile(args[0])){FreezeAcceptedMethods.hierarchy=loader;for(var name:List.of("BattleCompanion","ClientSession")){var c=FreezeAcceptedMethods.read(jar,P+name);patch(c);var out=Path.of(args[1]).resolve(c.name+".class");Files.createDirectories(out.getParent());Files.write(out,FreezeAcceptedMethods.bytes(c,true));}}
    }
}
