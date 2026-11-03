import java.nio.file.*;
import java.util.*;
import java.util.jar.*;
import org.objectweb.asm.tree.*;
import static org.objectweb.asm.Opcodes.*;

public final class CloseoutNpcReuseMethods {
    static final String P="local/lito381/server/",H=P+"NativeCloseoutNpcReuse",R=P+"BattleTimeAttackMagicSkillCatalog$MobskillRow";
    static boolean physical(MethodInsnNode call){
        if(!call.name.equals("schedule")||!call.desc.equals("(JI)V"))return false;
        var n=call.getPrevious();int count=0;
        while(n!=null&&count++<18){if(n instanceof MethodInsnNode m&&m.owner.equals(P+"BattleTimeAttackMagicSkillCatalog$PhysicalDefinition")&&m.name.equals("effectiveActionId"))return true;if(n.getOpcode()==RETURN||n instanceof JumpInsnNode)break;n=n.getPrevious();}return false;
    }
    static InsnList arguments(int actor,int row,int now){var code=new InsnList();code.add(new VarInsnNode(ALOAD,actor));code.add(new VarInsnNode(ALOAD,row));code.add(new VarInsnNode(LLOAD,now));return code;}
    static void patch(ClassNode c){int distance=0,scheduled=0,rolls=0,summons=0;
        for(var m:c.methods)if(m.name.equals("aiAttackFor")&&m.desc.startsWith("(JIIJ[III)")){
            for(var n:m.instructions.toArray())if(n instanceof MethodInsnNode call){
                if(call.owner.equals(R)&&call.name.equals("triggerDistance")){
                    var code=arguments(10,15,5);code.add(new MethodInsnNode(INVOKESTATIC,H,"distance","(ZLjava/lang/Object;L"+R+";J)Z",false));m.instructions.insert(call,code);distance++;
                }else if(call.owner.equals(c.name+"$Monster")&&(Set.of("scheduleMagic","scheduleSummon","scheduleGround","scheduleVisual").contains(call.name)||physical(call))){
                    var code=arguments(10,16,5);code.add(new MethodInsnNode(INVOKESTATIC,H,"scheduled","(Ljava/lang/Object;L"+R+";J)V",false));m.instructions.insert(call,code);scheduled++;
                }
            }
        }else if(m.name.equals("aiAttackFor")&&m.desc.startsWith("(JIIJ)")){
            for(var n:m.instructions.toArray())if(n instanceof MethodInsnNode call&&call.owner.equals("java/util/concurrent/ThreadLocalRandom")&&call.name.equals("nextInt")){
                if(call.desc.equals("(II)I")){
                    var code=new InsnList();code.add(new VarInsnNode(ALOAD,8));code.add(new VarInsnNode(ALOAD,9));code.add(new VarInsnNode(ILOAD,11));code.add(new VarInsnNode(LLOAD,5));m.instructions.insertBefore(call,code);
                    call.setOpcode(INVOKESTATIC);call.owner=H;call.name="roll";call.desc="(Ljava/util/concurrent/ThreadLocalRandom;IILjava/lang/Object;Ljava/util/List;IJ)I";rolls++;
                }else if(call.getPrevious() instanceof MethodInsnNode prior&&prior.owner.equals(P+"BattleTimeAttackMagicSkillCatalog$SummonDefinition")&&prior.name.equals("summonRandomBound")){
                    m.instructions.insertBefore(call,arguments(8,11,5));call.setOpcode(INVOKESTATIC);call.owner=H;call.name="summonRoll";call.desc="(Ljava/util/concurrent/ThreadLocalRandom;ILjava/lang/Object;L"+P+"BattleTimeAttackMagicSkillCatalog$SummonDefinition;J)I";summons++;
                }
            }
        }
        int expected=c.name.endsWith("BattleQuestEncounter")?3:5;
        if(distance!=1||scheduled!=expected||rolls!=1||summons!=1)throw new AssertionError("exact NPC cooldown seams "+c.name+" "+distance+"/"+scheduled+"/"+rolls+"/"+summons);
    }
    public static void main(String[]args)throws Exception{
        var urls=new ArrayList<java.net.URL>();urls.add(Path.of(args[0]).toUri().toURL());try(var files=Files.list(Path.of(args[2]))){for(var p:files.filter(x->x.toString().endsWith(".jar")).toList())urls.add(p.toUri().toURL());}
        try(var loader=new java.net.URLClassLoader(urls.toArray(java.net.URL[]::new),ClassLoader.getPlatformClassLoader());var jar=new JarFile(args[0])){
            FreezeAcceptedMethods.hierarchy=loader;
            for(var name:List.of("BattleQuestEncounter","BattleTimeAttackEncounter")){var c=FreezeAcceptedMethods.read(jar,P+name);patch(c);var out=Path.of(args[1]).resolve(P+name+".class");Files.createDirectories(out.getParent());Files.write(out,FreezeAcceptedMethods.bytes(c,true));}
        }
    }
}
