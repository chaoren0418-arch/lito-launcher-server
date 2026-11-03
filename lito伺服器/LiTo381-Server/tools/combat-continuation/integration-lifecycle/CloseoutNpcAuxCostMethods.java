import java.nio.file.*;
import java.util.*;
import java.util.jar.*;
import org.objectweb.asm.tree.*;
import static org.objectweb.asm.Opcodes.*;

public final class CloseoutNpcAuxCostMethods {
    static final String P="local/lito381/server/",H=P+"NativeCloseoutNpcAuxCost",C=P+"NativeCloseoutNpcSkillValues",V=C+"$Values";
    static InsnList capture(int def,int values){var x=new InsnList();x.add(new VarInsnNode(ALOAD,0));x.add(new VarInsnNode(ALOAD,def));x.add(new MethodInsnNode(INVOKESTATIC,H,"captureCost","(Ljava/lang/Object;Ljava/lang/Object;)L"+V+";",false));x.add(new VarInsnNode(ASTORE,values));return x;}
    static void getter(MethodNode m,MethodInsnNode call,int values){var x=new InsnList();x.add(new InsnNode(POP));x.add(new VarInsnNode(ALOAD,values));m.instructions.insertBefore(call,x);call.owner=V;call.name="mp";}
    static void ready(MethodNode m,String def,int values){int costs=0,results=0;
        for(var n:m.instructions.toArray())if(n instanceof MethodInsnNode call&&call.owner.equals(def)&&call.name.equals("mpCost")){getter(m,call,values);costs++;}
        else if(n.getOpcode()==IRETURN){var x=new InsnList();x.add(new VarInsnNode(ALOAD,0));x.add(new VarInsnNode(ALOAD,values));x.add(new MethodInsnNode(INVOKESTATIC,C,"ready","(ZLjava/lang/Object;L"+V+";)Z",false));m.instructions.insertBefore(n,x);results++;}
        if(costs!=1||results!=1)throw new AssertionError("one original cost and readiness result");
    }
    static void patch(ClassNode c){
        for(String kind:List.of("Visual","Ground")){
            String def=P+"BattleTimeAttackMagicSkillCatalog$"+kind+"Definition",name=kind.toLowerCase()+"Ready",sibling="nativeCloseout"+kind+"Ready";
            var original=c.methods.stream().filter(m->m.name.equals(name)&&m.desc.equals("(L"+def+";J)Z")).findFirst().orElseThrow();
            var captured=new MethodNode(original.access,sibling,"(L"+def+";JL"+V+";)Z",original.signature,original.exceptions.toArray(String[]::new));original.accept(captured);captured.maxLocals=Math.max(captured.maxLocals,5);ready(captured,def,4);c.methods.add(captured);
            int values=original.maxLocals++;original.instructions.insert(capture(1,values));ready(original,def,values);
            var schedule=c.methods.stream().filter(m->m.name.equals("schedule"+kind)&&m.desc.equals("(JL"+def+";)V")).findFirst().orElseThrow();
            int local=schedule.maxLocals++;schedule.instructions.insert(capture(3,local));int calls=0,costs=0,payments=0;
            for(var n:schedule.instructions.toArray()){
                if(n instanceof MethodInsnNode call&&call.owner.equals(c.name)&&call.name.equals(name)){schedule.instructions.insertBefore(call,new VarInsnNode(ALOAD,local));call.name=sibling;call.desc=captured.desc;calls++;}
                else if(n instanceof MethodInsnNode call&&call.owner.equals(def)&&call.name.equals("mpCost")){getter(schedule,call,local);costs++;}
                else if(n instanceof FieldInsnNode f&&f.getOpcode()==PUTFIELD&&f.owner.equals(c.name)&&f.name.equals("mp")){var x=new InsnList();x.add(new VarInsnNode(ALOAD,0));x.add(new VarInsnNode(ALOAD,local));x.add(new MethodInsnNode(INVOKESTATIC,C,"payHp","(Ljava/lang/Object;L"+V+";)V",false));schedule.instructions.insert(n,x);payments++;}
            }
            if(calls!=1||costs!=1||payments!=1)throw new AssertionError("one original cost/schedule span "+kind);
        }
    }
    public static void main(String[]args)throws Exception{
        var urls=new ArrayList<java.net.URL>();urls.add(Path.of(args[0]).toUri().toURL());try(var files=Files.list(Path.of(args[2]))){for(var p:files.filter(x->x.toString().endsWith(".jar")).toList())urls.add(p.toUri().toURL());}
        try(var loader=new java.net.URLClassLoader(urls.toArray(java.net.URL[]::new),ClassLoader.getPlatformClassLoader());var jar=new JarFile(args[0])){FreezeAcceptedMethods.hierarchy=loader;var c=FreezeAcceptedMethods.read(jar,P+"BattleTimeAttackEncounter$Monster");patch(c);var out=Path.of(args[1]).resolve(c.name+".class");Files.createDirectories(out.getParent());Files.write(out,FreezeAcceptedMethods.bytes(c,true));}
    }
}
