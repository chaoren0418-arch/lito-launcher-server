package local.lito381.semanticdiff;

import java.nio.file.*;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
import static local.lito381.semanticdiff.FourthSelfTest.*;

/** Valid ASM fixtures: compare the same framed bytes that isolated JVM loaders execute. */
public final class FifthSelfTest implements Opcodes {
    static int total;
    public static void main(String[] args)throws Exception {
        FourthSelfTest.work=Path.of(args[0]);FourthSelfTest.bin=Path.of(args[1]);
        Files.createDirectories(work);Files.createDirectories(bin);
        pair("coverage-gap",coverage(true,0),coverage(false,0),true,new Class<?>[]{int.class},2);
        pair("coverage-idiv",coverage(true,1),coverage(false,1),false,new Class<?>[]{int.class},0);
        pair("coverage-handler",coverage(true,2),coverage(false,2),false,new Class<?>[]{int.class},0);
        pair("coverage-order",coverage(true,3),coverage(false,3),false,new Class<?>[]{int.class},0);
        pair("coverage-invoke",invokeCoverage(true),invokeCoverage(false),false,new Class<?>[]{String.class},"bad");
        pair("nonnull-load",nonnull(true,0),nonnull(false,0),true,new Class<?>[]{Object[].class,boolean.class},new Object[2],true);
        pair("nonnull-all-paths",nonnull(true,1),nonnull(false,1),true,new Class<?>[]{Object[].class,boolean.class},new Object[2],false);
        pair("nonnull-missing-path",nonnull(true,2),nonnull(false,2),false,new Class<?>[]{Object[].class,boolean.class},null,false);
        pair("nonnull-overwrite",nonnull(true,3),nonnull(false,3),false,new Class<?>[]{Object[].class,boolean.class},new Object[2],true);
        pair("nonnull-caught-receiver",nonnull(true,4),nonnull(false,4),false,new Class<?>[]{Object[].class,boolean.class},null,true);
        pair("nonnull-dup",duplicateNull(true),duplicateNull(false),true,new Class<?>[]{Object[].class},(Object)new Object[1]);
        pair("nonnull-loop-fresh-value",loopNull(true),loopNull(false),false,new Class<?>[]{});
        pair("load-position",loadPosition(true,0),loadPosition(false,0),true,new Class<?>[]{int.class},7);
        pair("load-iinc",loadPosition(true,1),loadPosition(false,1),false,new Class<?>[]{int.class},7);
        pair("load-store",loadPosition(true,2),loadPosition(false,2),false,new Class<?>[]{int.class},7);
        pair("temp-dup",temporary(true,0),temporary(false,0),true,new Class<?>[]{int.class},7);
        pair("temp-count",temporary(true,1),temporary(false,1),false,new Class<?>[]{int.class},7);
        pair("temp-order",temporary(true,2),temporary(false,2),false,new Class<?>[]{int.class},7);
        pair("temp-live-out",liveOut(true),liveOut(false),false,new Class<?>[]{int.class},7);
        pair("block-merged-entry",mergedEntry(true,false),mergedEntry(false,false),true,new Class<?>[]{boolean.class,int.class},true,-7);
        pair("block-merged-entry-false",mergedEntry(true,false),mergedEntry(false,false),true,new Class<?>[]{boolean.class,int.class},false,-7);
        pair("block-merged-entry-swapped",mergedEntry(true,false),mergedEntry(false,true),false,new Class<?>[]{boolean.class,int.class},false,-7);
        Path ml=jar("merged-block-ablation-l",List.of(framed(mergedEntry(true,false)))),mr=jar("merged-block-ablation-r",List.of(framed(mergedEntry(false,false))));
        System.setProperty("semanticdiff.disable","blocks");
        try{compare("block-ablation",ml,mr,false);}finally{System.clearProperty("semanticdiff.disable");}
        exceptions();
        System.out.println("FIFTH SELFTEST ALL PASS checks="+total);
    }
    static void ok(String name,boolean condition){if(!condition)throw new AssertionError(name);total++;System.out.println("PASS "+name);}
    static void pair(String name,ClassNode a,ClassNode b,boolean same,Class<?>[] types,Object... args)throws Exception {
        a=framed(a);b=framed(b);
        Path l=jar(name+"-l",List.of(a)),r=jar(name+"-r",List.of(b));
        compare(name,l,r,same);compare(name+"-reverse",r,l,same);
        Object x=execute(l,types,args),y=execute(r,types,args);
        ok("JVM "+name+" left="+x+" right="+y,Objects.equals(x,y)==same);
    }
    static Object execute(Path jar,Class<?>[] types,Object[] args)throws Exception {
        class Loader extends ClassLoader {
            Loader(){super(ClassLoader.getPlatformClassLoader());}
            Class<?> define(byte[] bytes){return defineClass("Fixture",bytes,0,bytes.length);}
        }
        Class<?> c=new Loader().define(JarIndex.load(jar).classes.get("Fixture"));
        try{return c.getMethod("m",types).invoke(null,args);}
        catch(InvocationTargetException e){return "throws:"+e.getCause().getClass().getName();}
    }
    static void compare(String name,Path l,Path r,boolean same)throws Exception {
        JarIndex lj=JarIndex.load(l),rj=JarIndex.load(r);
        var d=ClassComparer.compare(lj.readTree("Fixture"),rj.readTree("Fixture"),lj,rj);
        Files.writeString(work.resolve(name+".txt"),d==null?"SAME":d.oneLine());
        ok(name+(d==null?" SAME":" "+d.oneLine()),(d==null)==same);
        Path json=work.resolve(name+".json");
        ok(name+" CLI",SemanticDiff.run(new String[]{"--left",l.toString(),"--right",r.toString(),"--json",json.toString()})==0);
        ok(name+" status",Files.readString(json).contains("\"DIFF\": "+(same?0:1)));
    }
    static void constant(MethodNode m,int n){m.instructions.add(new LdcInsnNode(n));}
    static void invoke(MethodNode m,int op,String owner,String name,String desc){m.instructions.add(new MethodInsnNode(op,owner,name,desc,op==INVOKEINTERFACE));}
    static ClassNode coverage(boolean left,int bad){
        ClassNode c=clazz();MethodNode m=method(c,ACC_PUBLIC|ACC_STATIC,"m","(I)I");
        LabelNode a=new LabelNode(),b=new LabelNode(),d=new LabelNode(),e=new LabelNode(),h=new LabelNode(),h2=new LabelNode();
        m.instructions.add(a);constant(m,10);var(m,ILOAD,0);ins(m,IDIV);m.instructions.add(b);
        var(m,ILOAD,0);ins(m,POP,DUP,POP,ICONST_1,IADD);m.instructions.add(d);
        constant(m,2);ins(m,IDIV);m.instructions.add(e);ins(m,IRETURN);
        m.instructions.add(h);ins(m,POP,ICONST_M1,IRETURN);m.instructions.add(h2);ins(m,POP,ICONST_2,IRETURN);
        if(!(bad==1&&left))m.tryCatchBlocks.add(new TryCatchBlockNode(a,left?b:e,bad==2&&left?h2:h,"java/lang/ArithmeticException"));
        if(left)m.tryCatchBlocks.add(new TryCatchBlockNode(d,e,h,"java/lang/ArithmeticException"));
        if(bad==3){TryCatchBlockNode broad=new TryCatchBlockNode(a,e,h2,"java/lang/Throwable");if(left)m.tryCatchBlocks.add(0,broad);else m.tryCatchBlocks.add(broad);}
        return c;
    }
    static ClassNode invokeCoverage(boolean left){
        ClassNode c=clazz();MethodNode m=method(c,ACC_PUBLIC|ACC_STATIC,"m","(Ljava/lang/String;)I");
        LabelNode a=new LabelNode(),b=new LabelNode(),e=new LabelNode(),h=new LabelNode();
        m.instructions.add(a);var(m,ALOAD,0);m.instructions.add(b);invoke(m,INVOKESTATIC,"java/lang/Integer","parseInt","(Ljava/lang/String;)I");
        m.instructions.add(e);ins(m,IRETURN);m.instructions.add(h);ins(m,POP,ICONST_M1,IRETURN);
        m.tryCatchBlocks.add(new TryCatchBlockNode(a,left?b:e,h,"java/lang/RuntimeException"));return c;
    }
    static void useArray(MethodNode m){var(m,ALOAD,0);ins(m,ARRAYLENGTH,POP);}
    static void nullCheck(MethodNode m){var(m,ALOAD,0);invoke(m,INVOKESTATIC,"java/util/Objects","requireNonNull","(Ljava/lang/Object;)Ljava/lang/Object;");ins(m,POP);}
    static ClassNode nonnull(boolean check,int mode){
        ClassNode c=clazz();MethodNode m=method(c,ACC_PUBLIC|ACC_STATIC,"m","([Ljava/lang/Object;Z)I");
        LabelNode other=new LabelNode(),join=new LabelNode(),start=new LabelNode(),end=new LabelNode(),handler=new LabelNode();
        if(mode==1||mode==2){var(m,ILOAD,1);m.instructions.add(new JumpInsnNode(IFEQ,other));useArray(m);m.instructions.add(new JumpInsnNode(GOTO,join));m.instructions.add(other);if(mode==1)useArray(m);m.instructions.add(join);}
        else if(mode==4){m.instructions.add(start);useArray(m);m.instructions.add(end);m.instructions.add(new JumpInsnNode(GOTO,join));m.instructions.add(handler);ins(m,POP);m.instructions.add(join);m.tryCatchBlocks.add(new TryCatchBlockNode(start,end,handler,"java/lang/NullPointerException"));}
        else useArray(m);
        if(mode==3){ins(m,ACONST_NULL);var(m,ASTORE,0);}
        if(check)nullCheck(m);constant(m,7);ins(m,IRETURN);return c;
    }
    static ClassNode duplicateNull(boolean check){
        ClassNode c=clazz();MethodNode m=method(c,ACC_PUBLIC|ACC_STATIC,"m","([Ljava/lang/Object;)I");
        useArray(m);var(m,ALOAD,0);
        if(check){ins(m,DUP);invoke(m,INVOKESTATIC,"java/util/Objects","requireNonNull","(Ljava/lang/Object;)Ljava/lang/Object;");ins(m,POP);}
        ins(m,ARRAYLENGTH,IRETURN);return c;
    }
    static ClassNode loopNull(boolean check){
        ClassNode c=clazz();MethodNode pick=method(c,ACC_PUBLIC|ACC_STATIC,"pick","(I)[Ljava/lang/Object;");LabelNode nil=new LabelNode();
        var(pick,ILOAD,0);pick.instructions.add(new JumpInsnNode(IFNE,nil));ins(pick,ICONST_0);pick.instructions.add(new TypeInsnNode(ANEWARRAY,"java/lang/Object"));ins(pick,ARETURN);pick.instructions.add(nil);ins(pick,ACONST_NULL,ARETURN);
        MethodNode m=method(c,ACC_PUBLIC|ACC_STATIC,"m","()I");LabelNode loop=new LabelNode(),second=new LabelNode(),next=new LabelNode();
        ins(m,ICONST_0);var(m,ISTORE,1);m.instructions.add(loop);var(m,ILOAD,1);invoke(m,INVOKESTATIC,"Fixture","pick","(I)[Ljava/lang/Object;");var(m,ASTORE,0);
        var(m,ILOAD,1);m.instructions.add(new JumpInsnNode(IFNE,second));useArray(m);m.instructions.add(new JumpInsnNode(GOTO,next));m.instructions.add(second);if(check)nullCheck(m);
        m.instructions.add(next);m.instructions.add(new IincInsnNode(1,1));var(m,ILOAD,1);ins(m,ICONST_2);m.instructions.add(new JumpInsnNode(IF_ICMPLT,loop));constant(m,7);ins(m,IRETURN);return c;
    }
    static void side(ClassNode c){MethodNode m=method(c,ACC_PUBLIC|ACC_STATIC,"side","()V");ins(m,RETURN);}
    static ClassNode loadPosition(boolean early,int mode){
        ClassNode c=clazz();side(c);MethodNode m=method(c,ACC_PUBLIC|ACC_STATIC,"m","(I)I");
        if(early)var(m,ILOAD,0);invoke(m,INVOKESTATIC,"Fixture","side","()V");
        if(mode==1)m.instructions.add(new IincInsnNode(0,1));
        if(mode==2){constant(m,9);var(m,ISTORE,0);}
        if(!early)var(m,ILOAD,0);ins(m,IRETURN);return c;
    }
    static ClassNode temporary(boolean local,int mode){
        ClassNode c=clazz();c.fields.add(new FieldNode(ACC_PUBLIC|ACC_STATIC,"state","I",null,null));
        MethodNode first=method(c,ACC_PUBLIC|ACC_STATIC,"first","()V");constant(first,1);first.instructions.add(new FieldInsnNode(PUTSTATIC,"Fixture","state","I"));ins(first,RETURN);
        MethodNode second=method(c,ACC_PUBLIC|ACC_STATIC,"second","()V");constant(second,2);second.instructions.add(new FieldInsnNode(PUTSTATIC,"Fixture","state","I"));ins(second,RETURN);
        MethodNode m=method(c,ACC_PUBLIC|ACC_STATIC,"m","(I)I");var(m,ILOAD,0);
        if(local)var(m,ISTORE,2);else ins(m,DUP);
        invoke(m,INVOKESTATIC,"Fixture",mode==2&&local?"second":"first","()V");
        invoke(m,INVOKESTATIC,"Fixture",mode==2&&local?"first":"second","()V");
        if(local){var(m,ILOAD,2);var(m,ILOAD,2);if(mode==1){var(m,ILOAD,2);ins(m,IADD);}}
        ins(m,IADD);m.instructions.add(new FieldInsnNode(GETSTATIC,"Fixture","state","I"));ins(m,IADD,IRETURN);return c;
    }
    static ClassNode liveOut(boolean changed){
        ClassNode c=clazz();MethodNode m=method(c,ACC_PUBLIC|ACC_STATIC,"m","(I)I");LabelNode after=new LabelNode();
        var(m,ILOAD,0);var(m,ISTORE,2);var(m,ILOAD,2);ins(m,POP);
        if(changed){constant(m,9);var(m,ISTORE,2);}m.instructions.add(new JumpInsnNode(GOTO,after));m.instructions.add(after);var(m,ILOAD,2);ins(m,IRETURN);return c;
    }
    static ClassNode mergedEntry(boolean local,boolean swapped){
        ClassNode c=clazz();MethodNode m=method(c,ACC_PUBLIC|ACC_STATIC,"m","(ZI)I");
        LabelNode other=new LabelNode(),join=new LabelNode();var(m,ILOAD,0);m.instructions.add(new JumpInsnNode(IFEQ,other));
        constant(m,swapped?2:1);m.instructions.add(new JumpInsnNode(GOTO,join));m.instructions.add(other);constant(m,swapped?1:2);m.instructions.add(join);
        var(m,ISTORE,2);var(m,ILOAD,1);invoke(m,INVOKESTATIC,"java/lang/Math","abs","(I)I");
        if(local)var(m,ISTORE,3);else ins(m,DUP);
        var(m,ILOAD,2);invoke(m,INVOKESTATIC,"java/lang/Math","abs","(I)I");
        if(local){var(m,ISTORE,4);var(m,ILOAD,3);var(m,ILOAD,3);var(m,ILOAD,4);} // right stack already has these three values
        ins(m,IADD,IADD,IRETURN);return c;
    }
    static void exceptions()throws Exception {
        ClassNode a=loadPosition(false,0),b=loadPosition(false,0);a.methods.get(1).exceptions.add("java/io/IOException");
        Path l=jar("exceptions-l",List.of(framed(a))),r=jar("exceptions-r",List.of(framed(b)));
        compare("exceptions-default",l,r,false);
        Path approved=work.resolve("exceptions.txt");Files.writeString(approved,"Fixture#m(I)I # approved: https://example.test/174\n");
        policy("exceptions-approved",l,r,approved,true);
        a.methods.get(0).exceptions.add("java/io/IOException");Path outside=jar("exceptions-outside",List.of(framed(a)));
        policy("exceptions-outside",outside,r,approved,false);
        a=loadPosition(false,2);a.methods.get(1).exceptions.add("java/io/IOException");Path changed=jar("exceptions-body",List.of(framed(a)));
        policy("exceptions-body",changed,r,approved,false);
        for(String entry:List.of("Fixture#typo(I)I # approved: https://example.test/174\n","Other#m(I)I # approved: https://example.test/174\n","Fixture#m(I)I\n")) {
            Files.writeString(approved,entry);boolean rejected=false;
            try{policy("exceptions-invalid",l,r,approved,false);}catch(IllegalArgumentException expected){rejected=true;}
            ok("exceptions rejected "+entry.trim(),rejected);
        }
    }
    static void policy(String name,Path l,Path r,Path policy,boolean same)throws Exception {
        Path json=work.resolve(name+".json");
        ok(name+" CLI",SemanticDiff.run(new String[]{"--left",l.toString(),"--right",r.toString(),"--exceptions-exempt",policy.toString(),"--json",json.toString()})==0);
        ok(name+" result",Files.readString(json).contains("\"DIFF\": "+(same?0:1)));
    }
}
