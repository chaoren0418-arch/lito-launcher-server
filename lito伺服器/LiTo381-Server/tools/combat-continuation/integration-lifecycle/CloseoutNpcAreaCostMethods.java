import java.nio.file.*;
import java.util.*;
import java.util.jar.*;
import org.objectweb.asm.tree.*;
import static org.objectweb.asm.Opcodes.*;

public final class CloseoutNpcAreaCostMethods {
    static final String P="local/lito381/server/",C=P+"NativeCloseoutNpcSpellAdmission",H=P+"NativeCloseoutNpcAreaCost",ROW="L"+P+"BattleTimeAttackMagicSkillCatalog$MobskillRow;";
    static void patch(ClassNode c){int count=0;for(var m:c.methods)if(Set.of("selectionSilence","selectionReady","attempt").contains(m.name)){
        boolean ready=m.name.equals("selectionReady");int row=ready?1:3,time=ready?2:1;
        var original=new LabelNode();var x=new InsnList();x.add(new VarInsnNode(ALOAD,0));x.add(new VarInsnNode(ALOAD,row));x.add(new MethodInsnNode(INVOKESTATIC,H,"matches","(Ljava/lang/Object;"+ROW+")Z",false));x.add(new JumpInsnNode(IFEQ,original));
        if(m.name.equals("selectionSilence"))x.add(new InsnNode(ICONST_0));
        else{x.add(new VarInsnNode(ALOAD,0));if(ready){x.add(new VarInsnNode(ALOAD,row));x.add(new VarInsnNode(LLOAD,time));}else{x.add(new VarInsnNode(LLOAD,time));x.add(new VarInsnNode(ALOAD,row));}x.add(new MethodInsnNode(INVOKESTATIC,H,ready?"ready":"attempt",m.desc,false));}
        x.add(new InsnNode(IRETURN));x.add(original);m.instructions.insert(x);count++;
    }if(count!=3)throw new AssertionError("three existing typed admission boundaries");}
    public static void main(String[]args)throws Exception{
        var urls=new ArrayList<java.net.URL>();urls.add(Path.of(args[0]).toUri().toURL());try(var files=Files.list(Path.of(args[2]))){for(var p:files.filter(x->x.toString().endsWith(".jar")).toList())urls.add(p.toUri().toURL());}
        try(var loader=new java.net.URLClassLoader(urls.toArray(java.net.URL[]::new),ClassLoader.getPlatformClassLoader());var jar=new JarFile(args[0])){FreezeAcceptedMethods.hierarchy=loader;var c=FreezeAcceptedMethods.read(jar,C);patch(c);var out=Path.of(args[1]).resolve(c.name+".class");Files.createDirectories(out.getParent());Files.write(out,FreezeAcceptedMethods.bytes(c,true));}
    }
}
