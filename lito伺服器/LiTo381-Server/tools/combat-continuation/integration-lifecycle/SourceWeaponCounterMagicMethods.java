import java.nio.file.*;
import java.util.*;
import java.util.jar.*;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
import static org.objectweb.asm.Opcodes.*;

/** Replace one successful NPC stack call; all original cost/RNG/IO paths stay intact. */
public final class SourceWeaponCounterMagicMethods {
    static final String P="local/lito381/server/", OWNER=P+"NativeWeaponBreakBattleCompanion",
        HELPER=P+"NativeSourceWeaponCounterMagic", PC=P+"ClientSession";
    static void need(boolean ok,String why){if(!ok)throw new AssertionError(why);}
    static void patch(ClassNode owner){
        var cast=owner.methods.stream().filter(m->m.name.equals("cast")).findFirst().orElseThrow();
        int sites=0;
        for(var node:cast.instructions.toArray()){
            if(node instanceof MethodInsnNode call && call.owner.equals(P+"BattleCompanion")
                    && call.name.equals("addWeaponBreakStack") && call.desc.equals("()I")){
                var args=new InsnList();
                args.add(new VarInsnNode(ALOAD,0));
                args.add(new VarInsnNode(ALOAD,28));
                args.add(new VarInsnNode(ALOAD,8));
                args.add(new MethodInsnNode(INVOKEVIRTUAL,P+"BattleCompanion$Snapshot","objectId","()J",false));
                cast.instructions.insertBefore(call,args);
                call.setOpcode(INVOKESTATIC);call.owner=HELPER;call.name="stack";
                call.desc="(L"+P+"BattleCompanion;L"+PC+";Ljava/util/List;J)I";
                sites++;
            }
        }
        need(sites==1,"one successful controlled-NPC stack call");
    }
    public static void main(String[]args)throws Exception{
        var urls=new ArrayList<java.net.URL>();urls.add(Path.of(args[1]).toUri().toURL());
        try(var files=Files.list(Path.of(args[3]))){
            for(var p:files.filter(x->x.toString().endsWith(".jar")).toList())urls.add(p.toUri().toURL());
        }
        try(var loader=new java.net.URLClassLoader(urls.toArray(java.net.URL[]::new),ClassLoader.getPlatformClassLoader());
            var jar=new JarFile(args[1])){
            FreezeAcceptedMethods.hierarchy=loader;
            var owner=FreezeAcceptedMethods.read(jar,OWNER);patch(owner);
            var canonical=new ClassNode();
            new ClassReader(FreezeAcceptedMethods.bytes(owner,true)).accept(canonical,ClassReader.SKIP_DEBUG|ClassReader.EXPAND_FRAMES);
            var bytes=FreezeAcceptedMethods.bytes(canonical,false);
            if(args[0].equals("splice")){
                var path=Path.of(args[2]).resolve(OWNER+".class");Files.createDirectories(path.getParent());Files.write(path,bytes);
                Files.write(Path.of(args[2]).getParent().resolve("weapon-counter-magic-ports.tsv"),
                    List.of("owner\tmethod\tphase",OWNER+"\tcast\tSource27 successful controlled stack / Counter Magic exception"));
            }else try(var result=new JarFile(args[2])){
                need(Arrays.equals(bytes,FreezeAcceptedMethods.bytes(FreezeAcceptedMethods.read(result,OWNER),false)),"same one-call Counter Magic splice");
            }
        }
        System.out.println("PASS Source27 controlled Counter Magic single successful-stack call port");
    }
}
