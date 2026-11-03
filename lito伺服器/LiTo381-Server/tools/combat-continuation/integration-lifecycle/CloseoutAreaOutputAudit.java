import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.jar.*;
import jdk.internal.org.objectweb.asm.*;
import jdk.internal.org.objectweb.asm.tree.*;
import jdk.internal.org.objectweb.asm.util.*;
import static jdk.internal.org.objectweb.asm.Opcodes.*;

/** Independent bytecode inversion only. Never loads application classes. */
public final class CloseoutAreaOutputAudit {
    static final String P="local/lito381/server/", H=P+"NativeCloseoutAreaOutput";
    static void need(boolean b,String why){if(!b)throw new AssertionError(why);}
    static ClassNode read(JarFile jar,String name)throws Exception{
        var c=new ClassNode(ASM8);new ClassReader(jar.getInputStream(jar.getJarEntry(P+name+".class"))).accept(c,ClassReader.SKIP_DEBUG|ClassReader.SKIP_FRAMES);return c;
    }
    static String text(MethodNode m){
        // Writer recomputes stack limits for inserted arguments. Compare original
        // instructions, handlers and annotations after inversion, not those limits.
        int stack=m.maxStack,locals=m.maxLocals;m.maxStack=0;m.maxLocals=0;
        try{var p=new Textifier();m.accept(new TraceMethodVisitor(p));var s=new StringWriter();p.print(new PrintWriter(s));return s.toString();}
        finally{m.maxStack=stack;m.maxLocals=locals;}
    }
    static void prefix(MethodNode m,String method){
        AbstractInsnNode end=null;int calls=0;
        for(var n:m.instructions.toArray()){
            if(n instanceof MethodInsnNode c&&c.owner.equals(H)&&c.name.equals(method))calls++;
            if(n instanceof JumpInsnNode j){need(j.getOpcode()==IFEQ&&calls==1,"bounded prefix "+m.name);end=j.label;break;}
        }
        need(end!=null,"prefix terminator "+m.name);
        while(m.instructions.getFirst()!=end)m.instructions.remove(m.instructions.getFirst());m.instructions.remove(end);
    }
    static void inverse(ClassNode c){
        if(c.name.endsWith("$Context")){
            need(c.fields.removeIf(f->f.name.equals("nativeCloseoutAreaOutput")&&f.desc.equals("Ljava/lang/Object;")&&(f.access&ACC_TRANSIENT)!=0),"transient scope field");
            int calls=0;for(var m:c.methods)if(m.name.equals("npc"))for(var n:m.instructions.toArray())if(n instanceof MethodInsnNode call&&call.owner.equals(H)&&call.name.equals("npc")){
                var id=n.getPrevious();var context=id.getPrevious();need(id instanceof VarInsnNode a&&a.getOpcode()==LLOAD&&a.var==1&&context instanceof VarInsnNode b&&b.getOpcode()==ALOAD&&b.var==0,"NPC predicate arguments");
                m.instructions.remove(id);m.instructions.remove(context);m.instructions.remove(n);calls++;
            }need(calls==1,"one NPC predicate guard");return;
        }
        if(c.name.equals(P+"ClientSession")){
            int calls=0;for(var m:c.methods)for(var n:m.instructions.toArray())if(n instanceof MethodInsnNode call&&call.owner.equals(H)&&call.name.equals("iterator")){
                need(Set.of("nativeAreaBattleSlowBody","nativeMaterialAreaWindBody").contains(m.name)&&n.getPrevious() instanceof MethodInsnNode previous&&previous.owner.equals(P+"NativeSourceAreaClassSelection")&&previous.name.equals("iterator"),"original class iterator order");
                m.instructions.remove(n);calls++;
            }need(calls==7,"seven pre-calculation filters");return;
        }
        if(c.name.equals(P+"NativeAreaMaterialAction")){for(var m:c.methods)if(m.name.equals("captureOutput"))prefix(m,"capture");return;}
        if(c.name.endsWith("$Aim")){
            int installs=0,closes=0;
            for(var m:c.methods)if(m.name.equals("<init>")){
                for(var n:m.instructions.toArray())if(n instanceof MethodInsnNode call&&call.owner.equals(H)&&call.name.equals("install")){
                    var load=n.getPrevious();need(load instanceof VarInsnNode v&&v.getOpcode()==ALOAD&&v.var==0,"install receiver");
                    var original=load.getPrevious();need(original instanceof MethodInsnNode q&&q.owner.equals(P+"NativeSourceAreaPairFirst")&&q.name.equals("capture"),"after first capture");
                    m.instructions.remove(load);m.instructions.remove(n);installs++;
                }
            }else if(m.name.equals("close")){
                var load=m.instructions.getFirst();var call=load.getNext();need(load instanceof VarInsnNode v&&v.var==0&&call instanceof MethodInsnNode q&&q.owner.equals(H)&&q.name.equals("close"),"close prefix");
                m.instructions.remove(load);m.instructions.remove(call);closes++;
            }
            need(installs==1&&closes==1,"one install/close");return;
        }
        if(c.name.equals(P+"NativeSourceWeaponFinalFrame")){
            for(var m:c.methods)if(m.name.equals("frame"))prefix(m,"frame");else if(m.name.equals("allow"))prefix(m,"kind");return;
        }
        var original=c.methods.stream().filter(m->m.name.equals("lambda$run$2")).findFirst().orElseThrow();
        var copy=c.methods.stream().filter(m->m.name.equals("nativeCloseoutAreaOriginalProducer")).findFirst().orElseThrow();
        need(copy.desc.equals(original.desc)&&text(copy).equals(text(original)),"complete producer sibling");c.methods.remove(copy);
        int handles=0,fallbacks=0;
        for(var m:c.methods)if(m.name.equals("run")){
            for(var n:m.instructions.toArray())if(n instanceof MethodInsnNode call&&call.owner.equals(P+"NativeCloseoutAreaNatural")){
                var npc=n.getPrevious();var context=npc.getPrevious();
                need(npc instanceof VarInsnNode x&&x.var==6&&x.getOpcode()==ALOAD&&context instanceof VarInsnNode y&&y.var==5&&y.getOpcode()==ALOAD,"first Context/npc arguments");
                m.instructions.remove(npc);m.instructions.remove(context);call.owner=c.name;call.name="original";
                call.desc=call.desc.replace("L"+P+"NativeAreaMaterialAction$Context;L"+P+"NativeAreaMaterialAction$Npc;)V",")V");fallbacks++;
            }
            for(var n:m.instructions.toArray())if(n instanceof InvokeDynamicInsnNode indy)for(int i=0;i<indy.bsmArgs.length;i++){
                if(indy.bsmArgs[i] instanceof Handle h&&h.getOwner().equals(H)&&h.getName().equals("produce")){
                    need(h.getTag()==H_INVOKESTATIC&&h.getDesc().equals(original.desc),"producer signature");
                    indy.bsmArgs[i]=new Handle(H_INVOKESTATIC,c.name,original.name,original.desc,false);handles++;
                }
            }
        }else if(m.name.equals("register"))prefix(m,"register");else if(m.name.equals("captureOutput"))prefix(m,"capture");
        need(handles==1&&fallbacks==1,"one changed producer handle and natural fallback");
    }
    public static void main(String[]args)throws Exception{
        int methods=0;var owners=List.of("NativeSourceMassSlowTargets","NativeSourceWindTargets","NativeSourceWeaponFinalFrame","ClientSession","NativeAreaMaterialAction","NativeAreaMaterialAction$Context","NativeSourceMassSlowTargets$Aim","NativeSourceWindTargets$Aim");
        try(var base=new JarFile(args[0]);var candidate=new JarFile(args[1])){
            for(var name:owners){var a=read(base,name);var b=read(candidate,name);inverse(b);
                need(a.fields.size()==b.fields.size()&&a.methods.size()==b.methods.size(),"member count "+name);
                for(int i=0;i<a.fields.size();i++){var x=a.fields.get(i);var y=b.fields.get(i);need(x.name.equals(y.name)&&x.desc.equals(y.desc)&&x.access==y.access&&Objects.equals(x.value,y.value),"field retained "+name);}
                for(int i=0;i<a.methods.size();i++){var x=a.methods.get(i);var y=b.methods.get(i);need(x.name.equals(y.name)&&x.desc.equals(y.desc)&&x.access==y.access&&text(x).equals(text(y)),"original method retained "+name+"."+x.name);methods++;}
            }
            for(var entry:Collections.list(base.entries())){
                if(owners.stream().anyMatch(n->entry.getName().equals(P+n+".class")))continue;
                need(Arrays.equals(base.getInputStream(entry).readAllBytes(),candidate.getInputStream(candidate.getJarEntry(entry.getName())).readAllBytes()),"unrelated entry retained "+entry.getName());
            }
        }
        Files.writeString(Path.of(args[2]),"Static bytecode inversion: "+owners.size()+" original classes / "+methods+" original methods retained; two complete producer siblings; unrelated archive entries identical. No application class loading or runtime tests.\n");
    }
}
