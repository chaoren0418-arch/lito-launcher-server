package local.lito381.semanticdiff;

import java.net.URLClassLoader;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.*;
import java.util.*;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
import static local.lito381.semanticdiff.FourthSelfTest.*;

/** #254: executable framed fixtures; comparer and isolated JVM load the same JAR. */
public final class EleventhSelfTest implements Opcodes {
    private static int checks;
    private static void ok(String name, boolean result) {
        if (!result) throw new AssertionError(name);
        checks++; System.out.println("PASS " + name);
    }
    public static void main(String[] args) throws Exception {
        work=Path.of(args[0]);bin=Path.of(args[1]);Files.createDirectories(work);Files.createDirectories(bin);
        pair("discard",discard(true,false),discard(false,false),true,false);
        pair("discard-call-effect",discard(true,true),discard(false,true),false,false);
        guards();
        pair("merge-fixed-point-loop",loopMerge(true,false),loopMerge(false,false),true,true);
        pair("merge-inline-cleanup",inlineCleanup(true),inlineCleanup(false),true,true);
        pair("merge-block-local-temporaries",temporaries(true),temporaries(false),true,true);
        pair("merge-different-successor",loopMerge(true,true),loopMerge(false,true),false,true);
        pair("merge-try-coverage",tryMerge(true),tryMerge(false),false,true);
        pair("merge-stack-values",stackMerge(true,false),stackMerge(false,false),false,true);
        pair("merge-common-stack",stackMerge(true,true),stackMerge(false,true),true,true);
        ClassNode constants=stackMerge(true,false);
        for(AbstractInsnNode n:constants.methods.get(0).instructions.toArray())
            if(n.getOpcode()==ICONST_1)constants.methods.get(0).instructions.set(n,new InsnNode(ICONST_2));
        pair("merge-equal-inherited-constants",constants,stackMerge(false,false),true,true);
        System.out.println("ELEVENTH SELFTEST ALL PASS checks="+checks);
    }
    private static void pair(String name,ClassNode a,ClassNode b,boolean same,boolean cfg) throws Exception {
        Path l=jar(name+"-l",List.of(framed(a))),r=jar(name+"-r",List.of(framed(b)));
        boolean different=false;
        for(int input:new int[]{0,1,2,3}) {
            Object x=execute(l,input),y=execute(r,input);different|=!x.equals(y);
            if(same)ok(name+" JVM input="+input+" "+x+"/"+y,x.equals(y));
            else System.out.println("JVM "+name+" input="+input+" "+x+"/"+y);
        }
        if(!same)ok(name+" JVM behavior differs",different);
        for(boolean reverse:new boolean[]{false,true}) {
            Path left=reverse?r:l,right=reverse?l:r;
            cli(name+reverse,left,right,same,null);
            if(cfg) {
                JarIndex lj=JarIndex.load(left),rj=JarIndex.load(right);
                MethodNode lm=lj.readTree("Fixture").methods.get(0),rm=rj.readTree("Fixture").methods.get(0);
                ok(name+" direct CFG "+reverse,CfgComparison.compare(lm,rm,lj,rj).same()==same);
                if(name.equals("merge-stack-values")&&!reverse) {
                    var merged=CfgComparison.coalesce(BlockExpressions.view(lm,lj,true));
                    ok("different inherited values keep both IRETURN blocks",merged.stream()
                            .filter(x->x.events().size()==1&&x.events().get(0).text().equals("IRETURN")).count()==2);
                }
                if(same) {
                    cli(name+reverse+"-cfg",left,right,true,"paths,blocks,transport");
                    cli(name+reverse+"-ablate",left,right,false,"paths,blocks,transport,cfg-merge");
                }
            } else if(same) cli(name+reverse+"-ablate",left,right,false,"discarded-loads,paths,blocks,transport,cfg");
        }
    }
    private static void cli(String name,Path l,Path r,boolean same,String disable)throws Exception {
        String old=System.getProperty("semanticdiff.disable");
        try {
            if(disable!=null)System.setProperty("semanticdiff.disable",disable);
            Path json=work.resolve(name+".json");
            ok(name+" CLI exit",SemanticDiff.run(new String[]{"--left",l.toString(),"--right",r.toString(),
                    "--json",json.toString(),"--md",work.resolve(name+".md").toString()})==0);
            String text=Files.readString(json);
            ok(name+" CLI result",text.contains("\"DIFF\": "+(same?0:1)));
            if(same&&disable!=null&&disable.equals("paths,blocks,transport"))
                ok(name+" SAME_CFG",text.contains("\"SAME_CFG\": 1"));
        } finally { if(old==null)System.clearProperty("semanticdiff.disable");else System.setProperty("semanticdiff.disable",old); }
    }
    private static ClassNode framed(ClassNode c) {
        ClassWriter w=new ClassWriter(ClassWriter.COMPUTE_FRAMES|ClassWriter.COMPUTE_MAXS);c.accept(w);
        ClassNode result=new ClassNode();new ClassReader(w.toByteArray()).accept(result,0);return result;
    }
    private static Object execute(Path p,int input)throws Exception {
        try(URLClassLoader l=new URLClassLoader(new java.net.URL[]{p.toUri().toURL()},ClassLoader.getPlatformClassLoader())) {
            try { return l.loadClass("Fixture").getMethod("m",int.class).invoke(null,input); }
            catch(InvocationTargetException e) {
                if(e.getCause() instanceof ArithmeticException)return "ArithmeticException";
                throw e;
            }
        }
    }
    private static void invoke(MethodNode m,String name,String desc) {
        m.instructions.add(new MethodInsnNode(INVOKESTATIC,"Fixture",name,desc,false));
    }
    private static ClassNode discard(boolean extra,boolean effects) {
        ClassNode c=clazz();MethodNode m=method(c,ACC_PUBLIC|ACC_STATIC,"m","(I)I");
        c.fields.add(new FieldNode(ACC_PUBLIC|ACC_STATIC,"count","I",null,null));
        if(extra) {
            if(effects){invoke(m,"tick","()I");ins(m,POP);}
            else {
                var(m,ILOAD,0);ins(m,POP,ACONST_NULL,POP,ICONST_M1,POP,FCONST_2,POP);
                m.instructions.add(new IntInsnNode(BIPUSH,40));ins(m,POP);
                m.instructions.add(new IntInsnNode(SIPUSH,400));ins(m,POP);
                for(Object constant:List.of(44,3.5f,"text",7L,4.5d)) {
                    m.instructions.add(new LdcInsnNode(constant));ins(m,constant instanceof Long||constant instanceof Double?POP2:POP);
                }
                ins(m,LCONST_1);var(m,LSTORE,1);var(m,LLOAD,1);ins(m,POP2);
                ins(m,DCONST_1);var(m,DSTORE,3);var(m,DLOAD,3);ins(m,POP2);
                ins(m,FCONST_1);var(m,FSTORE,5);var(m,FLOAD,5);ins(m,POP);
                ins(m,ACONST_NULL);var(m,ASTORE,6);var(m,ALOAD,6);ins(m,POP);
                // Leave initialization in both methods; only the load/discard is extra.
                var(m,ILOAD,0);ins(m,DUP,POP,POP);
            }
        } else if(!effects) {
            ins(m,LCONST_1);var(m,LSTORE,1);ins(m,DCONST_1);var(m,DSTORE,3);
            ins(m,FCONST_1);var(m,FSTORE,5);ins(m,ACONST_NULL);var(m,ASTORE,6);
        }
        m.instructions.add(new FieldInsnNode(GETSTATIC,"Fixture","count","I"));ins(m,IRETURN);
        MethodNode tick=method(c,ACC_PUBLIC|ACC_STATIC,"tick","()I");
        tick.instructions.add(new FieldInsnNode(GETSTATIC,"Fixture","count","I"));ins(tick,ICONST_1,IADD,DUP);
        tick.instructions.add(new FieldInsnNode(PUTSTATIC,"Fixture","count","I"));ins(tick,IRETURN);
        return c;
    }
    private static ClassNode loopMerge(boolean duplicate,boolean bad) {
        ClassNode c=clazz();MethodNode m=method(c,ACC_PUBLIC|ACC_STATIC,"m","(I)I");
        LabelNode test=new LabelNode(),other=new LabelNode(),tail=new LabelNode(),tail2=new LabelNode(),end=new LabelNode();
        ins(m,ICONST_0);var(m,ISTORE,1);m.instructions.add(test);var(m,ILOAD,0);m.instructions.add(new JumpInsnNode(IFLE,end));
        m.instructions.add(new IincInsnNode(0,-1));var(m,ILOAD,0);m.instructions.add(new JumpInsnNode(IFEQ,other));
        m.instructions.add(new IincInsnNode(1,1));m.instructions.add(new JumpInsnNode(GOTO,tail));
        m.instructions.add(other);m.instructions.add(new IincInsnNode(1,1));m.instructions.add(new JumpInsnNode(GOTO,duplicate?tail2:tail));
        m.instructions.add(tail);m.instructions.add(new IincInsnNode(1,2));m.instructions.add(new JumpInsnNode(GOTO,bad&&duplicate?end:test));
        if(duplicate){m.instructions.add(tail2);m.instructions.add(new IincInsnNode(1,2));m.instructions.add(new JumpInsnNode(GOTO,test));}
        m.instructions.add(end);var(m,ILOAD,1);ins(m,IRETURN);return c;
    }
    private static ClassNode tryMerge(boolean distinct) {
        ClassNode c=clazz();MethodNode m=method(c,ACC_PUBLIC|ACC_STATIC,"m","(I)I");
        LabelNode other=new LabelNode(),start=new LabelNode(),end=new LabelNode(),handler=new LabelNode();
        var(m,ILOAD,0);m.instructions.add(new JumpInsnNode(IFEQ,other));
        m.instructions.add(start);ins(m,ICONST_1,ICONST_0,IDIV,IRETURN);m.instructions.add(end);
        m.instructions.add(other);ins(m,ICONST_1,ICONST_0,IDIV,IRETURN);
        LabelNode allEnd=new LabelNode();m.instructions.add(allEnd);
        m.instructions.add(handler);ins(m,POP,ICONST_M1,IRETURN);
        m.tryCatchBlocks.add(new TryCatchBlockNode(start,distinct?end:allEnd,handler,"java/lang/ArithmeticException"));return c;
    }
    private static ClassNode inlineCleanup(boolean duplicate) {
        ClassNode c=clazz();MethodNode m=method(c,ACC_PUBLIC|ACC_STATIC,"m","(I)I");
        c.fields.add(new FieldNode(ACC_PUBLIC|ACC_STATIC,"count","I",null,null));
        LabelNode test=new LabelNode(),other=new LabelNode(),cleanup=new LabelNode(),end=new LabelNode();
        m.instructions.add(test);var(m,ILOAD,0);m.instructions.add(new JumpInsnNode(IFLE,end));
        m.instructions.add(new IincInsnNode(0,-1));var(m,ILOAD,0);m.instructions.add(new JumpInsnNode(IFEQ,other));
        invoke(m,"first","()V");
        if(duplicate){invoke(m,"cleanup","()V");m.instructions.add(new JumpInsnNode(GOTO,test));}
        else m.instructions.add(new JumpInsnNode(GOTO,cleanup));
        m.instructions.add(other);invoke(m,"last","()V");
        m.instructions.add(cleanup);invoke(m,"cleanup","()V");m.instructions.add(new JumpInsnNode(GOTO,test));
        m.instructions.add(end);m.instructions.add(new FieldInsnNode(GETSTATIC,"Fixture","count","I"));ins(m,IRETURN);
        int amount=1;
        for(String name:List.of("first","last","cleanup")) {
            MethodNode helper=method(c,ACC_PUBLIC|ACC_STATIC,name,"()V");
            helper.instructions.add(new FieldInsnNode(GETSTATIC,"Fixture","count","I"));
            ins(helper,ICONST_0+amount,IADD);helper.instructions.add(new FieldInsnNode(PUTSTATIC,"Fixture","count","I"));ins(helper,RETURN);amount++;
        }
        return c;
    }
    private static ClassNode temporaries(boolean duplicate) {
        ClassNode c=clazz();MethodNode m=method(c,ACC_PUBLIC|ACC_STATIC,"m","(I)I");
        LabelNode other=new LabelNode(),shared=new LabelNode();var(m,ILOAD,0);m.instructions.add(new JumpInsnNode(IFEQ,other));
        if(duplicate)tempReturn(m,1);else m.instructions.add(new JumpInsnNode(GOTO,shared));
        m.instructions.add(other);m.instructions.add(shared);tempReturn(m,duplicate?2:1);return c;
    }
    private static void tempReturn(MethodNode m,int slot) {
        var(m,ILOAD,0);m.instructions.add(new MethodInsnNode(INVOKESTATIC,"java/lang/Math","abs","(I)I",false));
        var(m,ISTORE,slot);var(m,ILOAD,slot);ins(m,IRETURN);
    }
    private static ClassNode stackMerge(boolean duplicate,boolean common) {
        ClassNode c=clazz();MethodNode m=method(c,ACC_PUBLIC|ACC_STATIC,"m","(I)I");
        LabelNode other=new LabelNode(),a=new LabelNode(),b=new LabelNode();
        if(common)ins(m,ICONST_2);
        var(m,ILOAD,0);m.instructions.add(new JumpInsnNode(IFEQ,other));
        if(!common)ins(m,duplicate?ICONST_1:ICONST_2);
        m.instructions.add(new JumpInsnNode(GOTO,a));
        m.instructions.add(other);if(!common)ins(m,ICONST_2);
        m.instructions.add(new JumpInsnNode(GOTO,duplicate?b:a));
        m.instructions.add(a);ins(m,IRETURN);if(duplicate){m.instructions.add(b);ins(m,IRETURN);}return c;
    }
    private static void guards() {
        for(String mode:List.of("label","try-start","try-end","annotation","pop2-prefix","condy","class","handle","method-type")) {
            ClassNode c=clazz();MethodNode m=method(c,ACC_STATIC,"m","()V");
            AbstractInsnNode load=new InsnNode(ICONST_1);
            if(mode.equals("condy"))load=new LdcInsnNode(new ConstantDynamic("x","I",new Handle(H_INVOKESTATIC,"Bootstrap","value","()I",false)));
            if(mode.equals("class"))load=new LdcInsnNode(Type.getObjectType("Missing"));
            if(mode.equals("method-type"))load=new LdcInsnNode(Type.getMethodType("()LMissing;"));
            if(mode.equals("handle"))load=new LdcInsnNode(new Handle(H_INVOKESTATIC,"Missing","f","()I",false));
            m.instructions.add(load);
            if(mode.equals("label")||mode.startsWith("try-"))m.instructions.add(new LabelNode());
            if(mode.equals("annotation"))load.visibleTypeAnnotations=new ArrayList<>(List.of(new TypeAnnotationNode(0,null,"LMarker;")));
            ins(m,mode.equals("pop2-prefix")?POP2:POP,RETURN);
            int size=m.instructions.size();DiscardedLoads.normalize(m);ok("guard "+mode,m.instructions.size()==size);
        }
    }
}
