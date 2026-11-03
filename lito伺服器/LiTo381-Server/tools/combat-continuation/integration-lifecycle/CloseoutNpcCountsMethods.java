import java.nio.file.*;
import java.util.*;
import java.util.jar.*;
import org.objectweb.asm.tree.*;
import static org.objectweb.asm.Opcodes.*;

public final class CloseoutNpcCountsMethods {
    static final String P="local/lito381/server/",H=P+"NativeCloseoutNpcCounts",ROW="L"+P+"BattleTimeAttackMagicSkillCatalog$MobskillRow;";
    static final List<String> OWNERS=List.of("NativeSourceNpcSpellTiming","NativeSourceNpcSpellTiming$Data","NativeCloseoutNpcAreaAttempt$Receipt","NativeCloseoutNpcReuse","BattleQuestEncounter","BattleTimeAttackEncounter","BattleQuestEncounter$Monster","BattleTimeAttackEncounter$Monster","GameHub");
    static void need(boolean b,String why){if(!b)throw new AssertionError(why);}
    static void patch(ClassNode c){
        for(var m:c.methods)for(var n:m.instructions.toArray())if(n instanceof FrameNode)m.instructions.remove(n);
        if(c.name.endsWith("$Data")||c.name.endsWith("$Receipt")){
            boolean data=c.name.endsWith("$Data");c.fields.add(new FieldNode(ACC_TRANSIENT,data?"nativeCloseoutNpcCountLimits":"nativeCloseoutNpcCounted",data?"Ljava/lang/Object;":"Z",null,null));return;
        }
        if(c.name.equals(P+"NativeSourceNpcSpellTiming")){
            int calls=0;for(var m:c.methods)if(m.name.equals("capture"))for(var n:m.instructions.toArray())if(n.getOpcode()==ARETURN){var code=new InsnList();code.add(new VarInsnNode(ALOAD,0));code.add(new VarInsnNode(ALOAD,1));code.add(new MethodInsnNode(INVOKESTATIC,H,"freeze","(Ljava/lang/Object;L"+P+"NativeDeNpcTemplateResources$Snapshot;L"+P+"NativeDeNpcTemplate;)Ljava/lang/Object;",false));m.instructions.insertBefore(n,code);calls++;}need(calls==1,"same birth snapshot");return;
        }
        if(c.name.equals(P+"NativeCloseoutNpcReuse")){
            var m=c.methods.stream().filter(x->x.name.equals("scheduled")).findFirst().orElseThrow();var code=new InsnList();code.add(new VarInsnNode(ALOAD,0));code.add(new VarInsnNode(ALOAD,1));code.add(new MethodInsnNode(INVOKESTATIC,H,"scheduled","(Ljava/lang/Object;"+ROW+")V",false));m.instructions.insert(code);return;
        }
        if(c.name.endsWith("$Monster")){
            var m=c.methods.stream().filter(x->x.name.equals("summonReady")).findFirst().orElseThrow();int result=m.maxLocals++;var fallback=new LabelNode();var code=new InsnList();code.add(new VarInsnNode(ALOAD,0));code.add(new VarInsnNode(ALOAD,1));code.add(new MethodInsnNode(INVOKESTATIC,H,"summon","(Ljava/lang/Object;L"+P+"BattleTimeAttackMagicSkillCatalog$SummonDefinition;)Ljava/lang/Boolean;",false));code.add(new VarInsnNode(ASTORE,result));code.add(new VarInsnNode(ALOAD,result));code.add(new JumpInsnNode(IFNULL,fallback));code.add(new VarInsnNode(ALOAD,result));code.add(new MethodInsnNode(INVOKEVIRTUAL,"java/lang/Boolean","booleanValue","()Z",false));code.add(new InsnNode(IRETURN));code.add(fallback);m.instructions.insert(code);return;
        }
        if(c.name.equals(P+"GameHub")){
            int calls=0;for(var m:c.methods)if(m.name.equals("monsterAiTicks"))for(var n:m.instructions.toArray())if(n instanceof MethodInsnNode call&&call.owner.equals(P+"NativeSourceNpcPlayerView")&&call.name.equals("target")){
                var code=new InsnList();for(int slot:new int[]{0,5,8})code.add(new VarInsnNode(ALOAD,slot));code.add(new VarInsnNode(LLOAD,1));code.add(new MethodInsnNode(INVOKESTATIC,H,"noTarget","(L"+P+"ClientSession;L"+P+"GameHub;Ljava/lang/Object;Ljava/lang/Object;J)L"+P+"ClientSession;",false));m.instructions.insert(n,code);calls++;
            }need(calls==2,"two original no-target returns");return;
        }
        int calls=0;for(var m:c.methods)if(m.name.equals("aiAttackFor")&&m.desc.contains("[I"))for(var n:m.instructions.toArray())if(n instanceof MethodInsnNode call&&call.owner.equals(P+"BattleTimeAttackMagicSkillCatalog$MobskillRow")&&call.name.equals("triggerRoll")){
            var code=new InsnList();code.add(new VarInsnNode(ALOAD,10));code.add(new VarInsnNode(ALOAD,15));code.add(new MethodInsnNode(INVOKESTATIC,H,"allowed","(ZLjava/lang/Object;"+ROW+")Z",false));m.instructions.insert(n,code);calls++;
        }need(calls==1,"count gate after original chance predicate");
    }
    public static void main(String[]args)throws Exception{
        var urls=new ArrayList<java.net.URL>();urls.add(Path.of(args[0]).toUri().toURL());try(var files=Files.list(Path.of(args[2]))){for(var p:files.filter(x->x.toString().endsWith(".jar")).toList())urls.add(p.toUri().toURL());}
        try(var loader=new java.net.URLClassLoader(urls.toArray(java.net.URL[]::new),ClassLoader.getPlatformClassLoader());var jar=new JarFile(args[0])){FreezeAcceptedMethods.hierarchy=loader;for(var name:OWNERS){var c=FreezeAcceptedMethods.read(jar,P+name);patch(c);var out=Path.of(args[1]).resolve(c.name+".class");Files.createDirectories(out.getParent());Files.write(out,FreezeAcceptedMethods.bytes(c,true));}}
    }
}
