import java.nio.file.*;
import java.util.*;
import java.util.jar.*;
import org.objectweb.asm.tree.*;
import static org.objectweb.asm.Opcodes.*;

/** Adapt only actual NPC consumers. Record accessors/equality and reuse remain unchanged. */
public final class CloseoutNpcSkillValuesMethods {
    static final String P="local/lito381/server/", H=P+"NativeCloseoutNpcSkillValues", V=H+"$Values", D=P+"BattleTimeAttackMagicSkillCatalog$Definition";
    static final String CAPTURE="(L"+D+";)L"+V+";";
    static InsnList capture(int definition, int values) {
        var code=new InsnList();code.add(new VarInsnNode(ALOAD,0));code.add(new VarInsnNode(ALOAD,definition));code.add(new MethodInsnNode(INVOKESTATIC,H,"captureCost","(Ljava/lang/Object;L"+D+";)L"+V+";",false));code.add(new VarInsnNode(ASTORE,values));return code;
    }
    static void getter(MethodNode m, MethodInsnNode call, int values) {
        var code=new InsnList();code.add(new InsnNode(POP));code.add(new VarInsnNode(ALOAD,values));m.instructions.insertBefore(call,code);
        call.owner=V; call.name=call.name.equals("mpCost")?"mp":call.name;
    }
    static void ready(MethodNode m,int values) {
        int fields=0,results=0;
        for(var n:m.instructions.toArray()) {
            if(n instanceof MethodInsnNode call&&call.owner.equals(D)&&call.name.equals("mpCost")){getter(m,call,values);fields++;}
            else if(n.getOpcode()==IRETURN){var code=new InsnList();code.add(new VarInsnNode(ALOAD,0));code.add(new VarInsnNode(ALOAD,values));code.add(new MethodInsnNode(INVOKESTATIC,H,"ready","(ZLjava/lang/Object;L"+V+";)Z",false));m.instructions.insertBefore(n,code);results++;}
        }
        if(fields!=1||results!=1)throw new AssertionError("original NPC readiness shape");
    }
    static void patch(ClassNode c) {
        if(c.name.equals(P+"CombatDatabase")){
            if(c.methods.stream().anyMatch(m->m.name.equals("nativeCloseoutSnapshot")))throw new AssertionError("fresh accessor");
            var m=new MethodNode(ACC_STATIC,"nativeCloseoutSnapshot","()L"+P+"CombatDatabase$Snapshot;",null,null);
            m.instructions.add(new FieldInsnNode(GETSTATIC,c.name,"current","L"+P+"CombatDatabase$Snapshot;"));m.instructions.add(new InsnNode(ARETURN));c.methods.add(m);return;
        }
        if(c.name.equals(P+"ClientSession")){
            int changed=0;
            for(var m:c.methods)if(Set.of("serverQuestMonsterMagicAttack","serverTimeAttackMonsterMagicAttack").contains(m.name)){
                int values=m.maxLocals++;int base=0,count=0,die=0;
                for(var n:m.instructions.toArray())if(n instanceof MethodInsnNode call&&call.owner.equals(D)){
                    if(call.name.equals("baseDamage")){
                        var code=new InsnList();code.add(new MethodInsnNode(INVOKESTATIC,H,"capture",CAPTURE,false));code.add(new InsnNode(DUP));code.add(new VarInsnNode(ASTORE,values));m.instructions.insertBefore(call,code);call.owner=V;base++;
                    }else if(call.name.equals("diceCount")){getter(m,call,values);count++;}
                    else if(call.name.equals("dieSides")){getter(m,call,values);die++;}
                }
                if(base!=1||count!=1||die!=1)throw new AssertionError("one original damage loop "+m.name);changed++;
            }
            if(changed!=2)throw new AssertionError("two original NPC magic producers");return;
        }
        var original=c.methods.stream().filter(m->m.name.equals("magicReady")&&m.desc.equals("(L"+D+";J)Z")).findFirst().orElseThrow();
        var captured=new MethodNode(original.access,"nativeCloseoutMagicReady","(L"+D+";JL"+V+";)Z",original.signature,original.exceptions.toArray(String[]::new));
        original.accept(captured);captured.maxLocals=Math.max(captured.maxLocals,5);ready(captured,4);c.methods.add(captured);
        int values=original.maxLocals++;original.instructions.insert(capture(1,values));ready(original,values);
        int schedules=0;
        for(var m:c.methods)if(m.name.equals("scheduleMagic")&&m.desc.equals("(JL"+D+";)V")){
            int local=m.maxLocals++;m.instructions.insert(capture(3,local));int calls=0,costs=0,payments=0;
            for(var n:m.instructions.toArray()){
                if(n instanceof MethodInsnNode call&&call.owner.equals(c.name)&&call.name.equals("magicReady")){
                    m.instructions.insertBefore(call,new VarInsnNode(ALOAD,local));call.name="nativeCloseoutMagicReady";call.desc=captured.desc;calls++;
                }else if(n instanceof MethodInsnNode call&&call.owner.equals(D)&&call.name.equals("mpCost")){getter(m,call,local);costs++;}
                else if(n instanceof FieldInsnNode f&&f.getOpcode()==PUTFIELD&&f.owner.equals(c.name)&&f.name.equals("mp")){
                    var code=new InsnList();code.add(new VarInsnNode(ALOAD,0));code.add(new VarInsnNode(ALOAD,local));code.add(new MethodInsnNode(INVOKESTATIC,H,"payHp","(Ljava/lang/Object;L"+V+";)V",false));m.instructions.insert(n,code);payments++;
                }
            }
            if(calls!=1||costs!=1||payments!=1)throw new AssertionError("one original NPC payment span");schedules++;
        }
        if(schedules!=1)throw new AssertionError("one original schedule");
    }
    public static void main(String[]args)throws Exception{
        var urls=new ArrayList<java.net.URL>();urls.add(Path.of(args[0]).toUri().toURL());
        try(var files=Files.list(Path.of(args[2]))){for(var p:files.filter(x->x.toString().endsWith(".jar")).toList())urls.add(p.toUri().toURL());}
        try(var loader=new java.net.URLClassLoader(urls.toArray(java.net.URL[]::new),ClassLoader.getPlatformClassLoader());var jar=new JarFile(args[0])){
            FreezeAcceptedMethods.hierarchy=loader;
            for(var name:List.of("ClientSession","CombatDatabase","BattleQuestEncounter$Monster","BattleTimeAttackEncounter$Monster")){
                var c=FreezeAcceptedMethods.read(jar,P+name);patch(c);var out=Path.of(args[1]).resolve(P+name+".class");Files.createDirectories(out.getParent());Files.write(out,FreezeAcceptedMethods.bytes(c,true));
            }
        }
    }
}
