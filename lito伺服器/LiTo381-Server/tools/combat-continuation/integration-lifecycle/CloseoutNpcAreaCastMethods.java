import java.nio.file.*;
import java.util.*;
import java.util.jar.*;
import org.objectweb.asm.tree.*;
import static org.objectweb.asm.Opcodes.*;

public final class CloseoutNpcAreaCastMethods {
    static final String P="local/lito381/server/", C=P+"ClientSession", S=P+"NativeCloseoutNpcAreaTargets$Selection", H=P+"NativeCloseoutNpcAreaCast";
    public static void main(String[]args)throws Exception {
        var urls=new ArrayList<java.net.URL>();urls.add(Path.of(args[0]).toUri().toURL());try(var files=Files.list(Path.of(args[2]))){for(var p:files.filter(x->x.toString().endsWith(".jar")).toList())urls.add(p.toUri().toURL());}
        try(var loader=new java.net.URLClassLoader(urls.toArray(java.net.URL[]::new),ClassLoader.getPlatformClassLoader());var jar=new JarFile(args[0])) {
            FreezeAcceptedMethods.hierarchy=loader;
            for(var name:List.of(C,S)) {
                var c=FreezeAcceptedMethods.read(jar,name);
                if(name.equals(S)) {
                    if(c.fields.stream().anyMatch(f->f.name.equals("nativeCloseoutGmBuffResults")))throw new AssertionError("fresh invocation-local result field");
                    c.fields.add(new FieldNode(ACC_TRANSIENT,"nativeCloseoutGmBuffResults","Ljava/lang/Object;",null,null));
                } else {
                    int count=0;
                    for(var m:c.methods)if(Set.of("serverQuestMonsterMagicAttack","serverTimeAttackMonsterMagicAttack").contains(m.name))
                        for(var n:m.instructions.toArray())if(n instanceof MethodInsnNode call&&call.owner.equals(P+"NativeCloseoutNpcAreaTargets")&&call.name.equals("allowed")) {
                            var target=call.getPrevious();var branch=call.getNext();
                            if(!(target instanceof VarInsnNode v&&v.getOpcode()==ALOAD)||!(branch instanceof JumpInsnNode j&&j.getOpcode()==IFEQ))throw new AssertionError("original target guard");
                            var x=new InsnList();x.add(new VarInsnNode(ALOAD,5));x.add(new VarInsnNode(ALOAD,((VarInsnNode)target).var));x.add(new VarInsnNode(ALOAD,4));
                            x.add(new MethodInsnNode(INVOKESTATIC,H,"calculate","(Ljava/util/List;L"+C+";L"+P+"BattleTimeAttackMagicSkillCatalog$Definition;)Z",false));
                            x.add(new JumpInsnNode(IFEQ,((JumpInsnNode)branch).label));m.instructions.insert(branch,x);count++;
                        }
                    if(count!=2)throw new AssertionError("two original magic damage loops");
                }
                var out=Path.of(args[1]).resolve(c.name+".class");Files.createDirectories(out.getParent());Files.write(out,FreezeAcceptedMethods.bytes(c,true));
            }
        }
    }
}
