import java.nio.file.*;
import java.util.*;
import java.util.jar.*;
import org.objectweb.asm.tree.*;
import static org.objectweb.asm.Opcodes.*;

public final class CloseoutNpcAreaAttemptMethods {
    static final String P="local/lito381/server/",H=P+"NativeCloseoutNpcAreaAttempt";
    static final List<String> OWNERS=List.of("GameHub","GameHub$MonsterAiTick","NativeCloseoutNpcAreaScene","ClientSession","NativeCloseoutNpcAreaDoors");
    static void need(boolean b,String why){if(!b)throw new AssertionError(why);}
    static void patch(ClassNode c){
        for(var m:c.methods)for(var n:m.instructions.toArray())if(n instanceof FrameNode)m.instructions.remove(n);
        if(c.name.equals(P+"GameHub$MonsterAiTick")){
            need(c.fields.stream().noneMatch(f->f.name.equals("nativeCloseoutNpcAreaReceipt")),"new receipt field");
            c.fields.add(new FieldNode(ACC_TRANSIENT,"nativeCloseoutNpcAreaReceipt","Ljava/lang/Object;",null,null));return;
        }
        if(c.name.equals(P+"GameHub")){
            var m=c.methods.stream().filter(x->x.name.equals("nativeSourceNpcPlayerFirstAppend")).findFirst().orElseThrow();
            int token=m.maxLocals++,failure=m.maxLocals++;
            var start=new LabelNode();var end=new LabelNode();var handler=new LabelNode();
            var enter=new InsnList();for(int slot:new int[]{12,13,1})enter.add(new VarInsnNode(ALOAD,slot));
            enter.add(new MethodInsnNode(INVOKESTATIC,H,"enter","(Ljava/lang/Object;Ljava/lang/Object;Ljava/util/List;)Ljava/lang/Object;",false));enter.add(new VarInsnNode(ASTORE,token));enter.add(start);m.instructions.insert(enter);
            int returns=0;for(var n:m.instructions.toArray())if(n.getOpcode()==RETURN){var leave=new InsnList();leave.add(new VarInsnNode(ALOAD,token));leave.add(new VarInsnNode(ALOAD,1));leave.add(new InsnNode(ICONST_1));leave.add(new MethodInsnNode(INVOKESTATIC,H,"leave","(Ljava/lang/Object;Ljava/util/List;Z)V",false));m.instructions.insertBefore(n,leave);returns++;}
            need(returns>0,"original returns");m.instructions.add(end);m.instructions.add(handler);m.instructions.add(new VarInsnNode(ASTORE,failure));m.instructions.add(new VarInsnNode(ALOAD,token));m.instructions.add(new VarInsnNode(ALOAD,1));m.instructions.add(new InsnNode(ICONST_0));m.instructions.add(new MethodInsnNode(INVOKESTATIC,H,"leave","(Ljava/lang/Object;Ljava/util/List;Z)V",false));m.instructions.add(new VarInsnNode(ALOAD,failure));m.instructions.add(new InsnNode(ATHROW));m.tryCatchBlocks.add(new TryCatchBlockNode(start,end,handler,null));return;
        }
        if(c.name.equals(P+"NativeCloseoutNpcAreaDoors")){
            var old=c.methods.stream().filter(m->m.name.equals("lineOfSight")).findFirst().orElseThrow();
            var m=new MethodNode(old.access,"nativeCloseoutOriginLineOfSight",old.desc.replace("NativeCloseoutNpcAreaTargets$Selection","NativeSourceNpcPlayer$Origin"),null,null);old.accept(m);m.name="nativeCloseoutOriginLineOfSight";
            int count=0;for(var n:m.instructions.toArray())if(n instanceof FieldInsnNode f&&f.owner.equals(P+"NativeCloseoutNpcAreaTargets$Selection")&&f.name.equals("scope")){
                var origin=n.getNext();need(origin instanceof MethodInsnNode call&&call.owner.equals(P+"NativeSourceNpcPlayer$Scope")&&call.name.equals("origin"),"only two origin accesses");m.instructions.remove(origin);m.instructions.remove(n);count++;
            }need(count==2,"two original origin references");c.methods.add(m);return;
        }
        String name=c.name.endsWith("AreaScene")?"attempt":"serverMonsterAiTick";
        var m=c.methods.stream().filter(x->x.name.equals(name)).findFirst().orElseThrow();var code=new InsnList();var fallback=new LabelNode();
        if(name.equals("attempt")){
            for(int slot:new int[]{0,3,4})code.add(new VarInsnNode(ALOAD,slot));code.add(new MethodInsnNode(INVOKESTATIC,H,"permit","(Ljava/lang/Object;L"+P+"BattleTimeAttackMagicSkillCatalog$MobskillRow;Ljava/lang/Object;)Z",false));
            code.add(new JumpInsnNode(IFNE,fallback));code.add(new InsnNode(ICONST_0));code.add(new InsnNode(IRETURN));
        }else{
            code.add(new VarInsnNode(ALOAD,0));code.add(new VarInsnNode(ALOAD,1));code.add(new MethodInsnNode(INVOKESTATIC,H,"deliver","(L"+P+"ClientSession;L"+P+"GameHub$MonsterAiTick;)Z",false));
            code.add(new JumpInsnNode(IFEQ,fallback));code.add(new InsnNode(RETURN));
        }code.add(fallback);m.instructions.insert(code);
    }
    public static void main(String[]args)throws Exception{
        var urls=new ArrayList<java.net.URL>();urls.add(Path.of(args[0]).toUri().toURL());try(var files=Files.list(Path.of(args[2]))){for(var p:files.filter(x->x.toString().endsWith(".jar")).toList())urls.add(p.toUri().toURL());}
        try(var loader=new java.net.URLClassLoader(urls.toArray(java.net.URL[]::new),ClassLoader.getPlatformClassLoader());var jar=new JarFile(args[0])){FreezeAcceptedMethods.hierarchy=loader;for(var name:OWNERS){var c=FreezeAcceptedMethods.read(jar,P+name);patch(c);var out=Path.of(args[1]).resolve(c.name+".class");Files.createDirectories(out.getParent());Files.write(out,FreezeAcceptedMethods.bytes(c,true));}}
    }
}
