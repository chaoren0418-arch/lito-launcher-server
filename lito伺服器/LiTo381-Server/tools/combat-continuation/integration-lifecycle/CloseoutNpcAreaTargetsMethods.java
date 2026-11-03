import java.nio.file.*;
import java.util.*;
import java.util.jar.*;
import org.objectweb.asm.tree.*;
import static org.objectweb.asm.Opcodes.*;

public final class CloseoutNpcAreaTargetsMethods {
    static final String P="local/lito381/server/", C=P+"ClientSession", H=P+"NativeCloseoutNpcAreaTargets";
    static final Set<String> METHODS=Set.of("serverQuestMonsterMagicAttack","serverTimeAttackMonsterMagicAttack");
    static void patch(MethodNode m) {
        for(var n:m.instructions.toArray())if(n instanceof FrameNode)m.instructions.remove(n);
        MethodInsnNode targeting=null, clock=null, capture=null, hasNext=null;
        for(var n:m.instructions)if(n instanceof MethodInsnNode c){
            if(c.owner.equals(P+"BattleTimeAttackMagicSkillCatalog$Definition")&&c.name.equals("targeting")&&targeting==null)targeting=c;
            if(c.owner.equals("java/lang/System")&&c.name.equals("nanoTime")&&clock==null)clock=c;
            if(c.owner.equals(P+"NativeCloseoutNpcSkillValues")&&c.name.equals("capture"))capture=c;
            if(c.owner.equals("java/util/Iterator")&&c.name.equals("hasNext")&&hasNext==null)hasNext=c;
        }
        if(targeting==null||clock==null||capture==null||hasNext==null)throw new AssertionError("original area anchors");
        var start=targeting.getPrevious();var merge=clock.getPrevious();
        if(!(start instanceof VarInsnNode v&&v.getOpcode()==ALOAD&&v.var==4)||!(merge instanceof LabelNode))throw new AssertionError("original selection boundaries");
        int slot=m.maxLocals++;var fallback=new LabelNode();var x=new InsnList();
        for(int i=0;i<3;i++)x.add(new VarInsnNode(ALOAD,i));
        x.add(new MethodInsnNode(INVOKESTATIC,H,"select","(L"+C+";L"+P+"GameHub$MonsterAiTick;Ljava/lang/Object;)Ljava/util/List;",false));
        x.add(new VarInsnNode(ASTORE,slot));x.add(new VarInsnNode(ALOAD,slot));x.add(new JumpInsnNode(IFNULL,fallback));
        x.add(new VarInsnNode(ALOAD,slot));x.add(new VarInsnNode(ASTORE,5));x.add(new JumpInsnNode(GOTO,(LabelNode)merge));x.add(fallback);
        m.instructions.insertBefore(start,x);
        var target=capture.getPrevious().getPrevious();var loop=hasNext.getPrevious().getPrevious();
        if(!(target instanceof VarInsnNode t&&t.getOpcode()==ASTORE)||!(loop instanceof LabelNode))throw new AssertionError("original iterator target/continue");
        x=new InsnList();x.add(new VarInsnNode(ALOAD,5));x.add(new VarInsnNode(ALOAD,((VarInsnNode)target).var));
        x.add(new MethodInsnNode(INVOKESTATIC,H,"allowed","(Ljava/util/List;L"+C+";)Z",false));x.add(new JumpInsnNode(IFEQ,(LabelNode)loop));
        m.instructions.insert(target,x);
    }
    public static void main(String[]args)throws Exception {
        var urls=new ArrayList<java.net.URL>();urls.add(Path.of(args[0]).toUri().toURL());try(var files=Files.list(Path.of(args[2]))){for(var p:files.filter(x->x.toString().endsWith(".jar")).toList())urls.add(p.toUri().toURL());}
        try(var loader=new java.net.URLClassLoader(urls.toArray(java.net.URL[]::new),ClassLoader.getPlatformClassLoader());var jar=new JarFile(args[0])) {
            FreezeAcceptedMethods.hierarchy=loader;var c=FreezeAcceptedMethods.read(jar,C);int count=0;
            for(var m:c.methods)if(METHODS.contains(m.name)){patch(m);count++;}if(count!=2)throw new AssertionError("two area ingress methods");
            var out=Path.of(args[1]).resolve(c.name+".class");Files.createDirectories(out.getParent());Files.write(out,FreezeAcceptedMethods.bytes(c,true));
        }
    }
}
