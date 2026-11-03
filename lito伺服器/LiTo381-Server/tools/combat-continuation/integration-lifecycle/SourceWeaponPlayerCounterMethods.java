import java.nio.file.*;
import java.util.*;
import java.util.jar.*;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
import static org.objectweb.asm.Opcodes.*;

/** Replace one successful NPC stack call; all original cost/RNG/IO paths stay intact. */
public final class SourceWeaponPlayerCounterMethods {
    static final String NPC="local/lito381/server/NativeWeaponBreakBattleCompanion",LIGHT="local/lito381/server/NativeLightAction";
    static final String P="local/lito381/server/", OWNER=P+"ClientSession",
        HELPER=P+"NativeSourceWeaponPlayerCounter", PC=P+"ClientSession";
    static void need(boolean ok,String why){if(!ok)throw new AssertionError(why);}
    static void patch(ClassNode owner){
        if(owner.name.equals(LIGHT)){
            var method=owner.methods.stream().filter(m->m.name.equals("lambda$commitPair$9")).findFirst().orElseThrow();int sites=0;
            for(var node:method.instructions.toArray())if(node instanceof MethodInsnNode call&&call.owner.equals(P+"NativePlayerCommit$Binding")&&call.name.equals("matches")&&call.desc.equals("(L"+PC+";)Z")){
                call.setOpcode(INVOKESTATIC);call.owner=HELPER;call.name="matches";call.desc="(L"+P+"NativePlayerCommit$Binding;L"+PC+";)Z";sites++;
            }
            need(sites==2,"both original after-pair Binding.matches calls");return;
        }
        if(owner.name.equals(NPC)){
            var method=owner.methods.stream().filter(m->m.name.equals("cast")).findFirst().orElseThrow();int sites=0;
            for(var node:method.instructions.toArray())if(node instanceof MethodInsnNode call&&call.owner.equals(P+"BattleCompanion")&&call.name.equals("earthBindActive")&&call.desc.equals("(J)Z")){
                call.setOpcode(INVOKESTATIC);call.owner=HELPER;call.name="earth";call.desc="(L"+P+"BattleCompanion;J)Z";sites++;
            }
            need(sites==1,"one original controlled Source27 Earth Bind probability gate");return;
        }
        var cast=owner.methods.stream().filter(m->m.name.equals("nativeMaterialWeaponBreakBody")).findFirst().orElseThrow();
        int sites=0;
        for(var node:cast.instructions.toArray()){
            if(node instanceof MethodInsnNode call && call.owner.equals(P+"BattleTimedStatus")
                    && call.name.equals("consumeCounterMagic") && call.desc.equals("(J)Z")){
                var time=FreezeAcceptedMethods.prev(call);
                var status=FreezeAcceptedMethods.prev(time);
                var target=FreezeAcceptedMethods.prev(status);
                need(time instanceof VarInsnNode t&&t.getOpcode()==LLOAD
                    &&status instanceof FieldInsnNode f&&f.getOpcode()==GETFIELD&&f.owner.equals(PC)&&f.name.equals("timedStatus")
                    &&target instanceof VarInsnNode v&&v.getOpcode()==ALOAD,"exact existing target timed status/time operands");
                var args=new InsnList();args.add(new VarInsnNode(ALOAD,0));args.add(new VarInsnNode(ALOAD,((VarInsnNode)target).var));
                cast.instructions.insertBefore(call,args);
                call.setOpcode(INVOKESTATIC);call.owner=HELPER;call.name="consume";
                call.desc="(L"+P+"BattleTimedStatus;JL"+PC+";L"+PC+";)Z";sites++;
            }
        }
        need(sites==1,"one original PC weapon Counter Magic call");
    }
    public static void main(String[]args)throws Exception{
        var urls=new ArrayList<java.net.URL>();urls.add(Path.of(args[1]).toUri().toURL());
        try(var files=Files.list(Path.of(args[3]))){
            for(var p:files.filter(x->x.toString().endsWith(".jar")).toList())urls.add(p.toUri().toURL());
        }
        try(var loader=new java.net.URLClassLoader(urls.toArray(java.net.URL[]::new),ClassLoader.getPlatformClassLoader());
            var jar=new JarFile(args[1])){
            FreezeAcceptedMethods.hierarchy=loader;
            for(var name:List.of(OWNER,NPC,LIGHT)){
                var owner=FreezeAcceptedMethods.read(jar,name);patch(owner);var canonical=new ClassNode();
                new ClassReader(FreezeAcceptedMethods.bytes(owner,true)).accept(canonical,ClassReader.SKIP_DEBUG|ClassReader.EXPAND_FRAMES);
                var bytes=FreezeAcceptedMethods.bytes(canonical,false);
                if(args[0].equals("splice")){var path=Path.of(args[2]).resolve(name+".class");Files.createDirectories(path.getParent());Files.write(path,bytes);}
                else try(var result=new JarFile(args[2])){need(Arrays.equals(bytes,FreezeAcceptedMethods.bytes(FreezeAcceptedMethods.read(result,name),false)),"same PC counter / controlled Earth Bind splice");}
            }
            if(args[0].equals("splice"))Files.write(Path.of(args[2]).getParent().resolve("weapon-player-counter-ports.tsv"),
                List.of("owner\tmethod\tphase",OWNER+"\tnativeMaterialWeaponBreakBody\tSource27 first PC target dynamic counter exception",NPC+"\tcast\tSource27 controlled Earth Bind exception",LIGHT+"\tlambda$commitPair$9\tSource27 first binding after original pair wait"));
        }
        System.out.println("PASS Source27 PC Counter Magic one original consume call port");
    }
}
