import java.nio.file.*;
import java.util.*;
import java.util.jar.*;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
import static org.objectweb.asm.Opcodes.*;

/** Test-only save failure port, with an independent complete original body inverse. */
public final class SourceWeaponStateFaultMethods {
    static final String P="local/lito381/server/",OWNER=P+"GameStateStore",FAULT=P+"WeaponStateFault";
    static void need(boolean b,String why){if(!b)throw new AssertionError(why);}
    public static void main(String[]args)throws Exception{
        var urls=new ArrayList<java.net.URL>();urls.add(Path.of(args[1]).toUri().toURL());
        try(var files=Files.list(Path.of(args[3]))){for(var p:files.filter(x->x.toString().endsWith(".jar")).toList())urls.add(p.toUri().toURL());}
        try(var loader=new java.net.URLClassLoader(urls.toArray(java.net.URL[]::new),ClassLoader.getPlatformClassLoader());var jar=new JarFile(args[1])){
            FreezeAcceptedMethods.hierarchy=loader;
            var original=FreezeAcceptedMethods.read(jar,OWNER);
            if(args[0].equals("splice")){
                var save=original.methods.stream().filter(m->m.name.equals("save")&&m.desc.equals("()V")).findFirst().orElseThrow();
                need((save.access&ACC_PRIVATE)!=0,"original private save");var prefix=new InsnList();prefix.add(new VarInsnNode(ALOAD,0));prefix.add(new MethodInsnNode(INVOKESTATIC,FAULT,"before","(L"+OWNER+";)V",false));save.instructions.insert(prefix);
                var path=Path.of(args[2]).resolve(OWNER+".class");Files.createDirectories(path.getParent());Files.write(path,FreezeAcceptedMethods.bytes(original,true));
                System.out.println("TEST ONLY one private save prefix before original persistence guard");return;
            }
            try(var test=new JarFile(args[2])){
                var changed=new HashSet<String>();var oldNames=new HashSet<String>();
                for(var e:Collections.list(jar.entries())){oldNames.add(e.getName());var now=test.getJarEntry(e.getName());need(now!=null,"every candidate entry retained");if(!Arrays.equals(jar.getInputStream(e).readAllBytes(),test.getInputStream(now).readAllBytes()))changed.add(e.getName());}
                need(changed.equals(Set.of(OWNER+".class")),"test changes only state save owner");
                var added=new HashSet<String>();for(var e:Collections.list(test.entries()))if(!oldNames.contains(e.getName()))added.add(e.getName());
                need(added.equals(Set.of(FAULT+".class")),"one test-only fault helper");var after=FreezeAcceptedMethods.read(test,OWNER);
                need(after.fields.size()==original.fields.size()&&after.methods.size()==original.methods.size(),"original state schema retained");
                for(int i=0;i<original.fields.size();i++){var a=original.fields.get(i);var b=after.fields.get(i);need(a.name.equals(b.name)&&a.desc.equals(b.desc)&&a.access==b.access&&Objects.equals(a.value,b.value)&&Objects.equals(a.signature,b.signature),"original state fields exact");}
                int methods=0,ports=0;
                for(var before:original.methods){var now=after.methods.stream().filter(m->m.name.equals(before.name)&&m.desc.equals(before.desc)).findFirst().orElseThrow();
                    for(var n:now.instructions.toArray())if(n instanceof MethodInsnNode call&&call.owner.equals(FAULT)){
                        need(now.name.equals("save")&&call.name.equals("before")&&call.getOpcode()==INVOKESTATIC&&call.desc.equals("(L"+OWNER+";)V"),"one reviewed test prefix");
                        var load=FreezeAcceptedMethods.prev(call);need(load instanceof VarInsnNode v&&v.getOpcode()==ALOAD&&v.var==0,"original store only");now.instructions.remove(load);now.instructions.remove(call);ports++;
                    }
                    need(Arrays.equals(SourceNpcFirstViewDelta.body(original,before,before.name),SourceNpcFirstViewDelta.body(after,now,before.name)),"complete unchanged candidate state method "+before.name);methods++;
                }
                need(ports==1,"one injected save boundary");System.out.println("PASS test-only state fault inverse: "+methods+" original methods,1 private save prefix,0 field changes; original candidate rollback body retained");
            }
        }
    }
}
