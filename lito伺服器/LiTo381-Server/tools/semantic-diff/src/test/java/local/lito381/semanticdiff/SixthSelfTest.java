package local.lito381.semanticdiff;

import java.nio.file.*;
import java.util.*;
import java.lang.reflect.*;
import org.objectweb.asm.*;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;
import static local.lito381.semanticdiff.FourthSelfTest.*;

/** The framed bytes passed to the CLI are also executed in isolated platform-parent loaders. */
public final class SixthSelfTest implements Opcodes {
    static int checks;
    static void ok(String label,boolean value){if(!value)throw new AssertionError(label);checks++;System.out.println("PASS "+label);}
    public static void main(String[] args)throws Exception {
        FourthSelfTest.work=Path.of(args[0]);FourthSelfTest.bin=Path.of(args[1]);Files.createDirectories(work);Files.createDirectories(bin);
        pair("paths-join",join(true,false),join(false,false),"SAME_PATHS",true,new Class<?>[]{int.class},-3);
        pair("paths-join-positive",join(true,false),join(false,false),"SAME_PATHS",true,new Class<?>[]{int.class},3);
        pair("paths-branch-inversion",join(true,false),join(false,true),"DIFF",false,new Class<?>[]{int.class},-3);
        pair("paths-call-order",calls(true),calls(false),"DIFF",false,new Class<?>[]{});
        pair("paths-monitor-callback",monitorCallback(true),monitorCallback(false),"DIFF",false,new Class<?>[]{Object.class},new Object());
        pair("paths-field-version",reads(true,false),reads(false,false),"DIFF",false,new Class<?>[]{});
        pair("paths-volatile-count",reads(true,true),reads(false,true),"DIFF",false,new Class<?>[]{});
        pair("paths-iinc",FifthSelfTest.loadPosition(true,1),FifthSelfTest.loadPosition(false,1),"DIFF",false,new Class<?>[]{int.class},7);
        pair("paths-exception-order",FifthSelfTest.coverage(true,3),FifthSelfTest.coverage(false,3),"DIFF",false,new Class<?>[]{int.class},0);
        // Existing review counterexamples must still run with the default fallback enabled.
        pair("paths-isub-order",subtract(true),subtract(false),"DIFF",false,new Class<?>[]{int.class,int.class},9,2);
        pair("paths-array-version",arrayReads(true),arrayReads(false),"DIFF",false,new Class<?>[]{int[].class},new int[]{1});
        pair("paths-standard-monitor",guarded(true,false),guarded(false,false),"SAME_PATHS",true,new Class<?>[]{Object.class,String.class},new Object(),"abc");
        pair("paths-standard-monitor-throw",guarded(true,false),guarded(false,false),"SAME_PATHS",true,new Class<?>[]{Object.class,String.class},new Object(),null);
        Path nl=save("monitor-nonstandard-l",guarded(true,true)),nr=save("monitor-nonstandard-r",guarded(false,false));
        cli("monitor-nonstandard",nl,nr,"DIFF");
        pair("paths-zero-branch",zero(true),zero(false),"SAME_PATHS",true,new Class<?>[]{int.class},0);
        pair("paths-zero-branch-nonzero",zero(true),zero(false),"SAME_PATHS",true,new Class<?>[]{int.class},3);
        Path vl=save("volatile-pop-l",volatilePop(true)),vr=save("volatile-pop-r",volatilePop(false));
        cli("volatile-unused-read-count",vl,vr,"DIFF");
        pathsBounds();inner();deviations();orphans();
        System.out.println("SIXTH SELFTEST ALL PASS checks="+checks);
    }
    static Path save(String name,ClassNode c)throws Exception{return jar(name,List.of(framed(c)));}
    static String cli(String name,Path l,Path r,String expected,String... flags)throws Exception {
        Path out=work.resolve(name+".json");List<String> args=new ArrayList<>(List.of("--left",l.toString(),"--right",r.toString(),"--json",out.toString(),"--md",work.resolve(name+".md").toString()));
        args.addAll(List.of(flags));ok(name+" CLI",SemanticDiff.run(args.toArray(String[]::new))==0);
        String json=Files.readString(out);ok(name+" "+expected,json.contains("\"status\": \""+expected+"\""));return json;
    }
    static void pair(String name,ClassNode a,ClassNode b,String status,boolean same,Class<?>[] types,Object... args)throws Exception {
        Path l=save(name+"-l",a),r=save(name+"-r",b);cli(name,l,r,status);
        Object x=execute(l,types,args),y=execute(r,types,args);
        ok("JVM "+name+" left="+x+" right="+y,Objects.equals(x,y)==same);
    }
    static Class<?> load(Path path)throws Exception {
        Map<String,byte[]> classes=JarIndex.load(path).classes;
        class Loader extends ClassLoader {
            Loader(){super(ClassLoader.getPlatformClassLoader());}
            @Override protected Class<?> findClass(String name)throws ClassNotFoundException {
                byte[] bytes=classes.get(name.replace('.','/'));
                if(bytes==null)throw new ClassNotFoundException(name);
                return defineClass(name,bytes,0,bytes.length);
            }
        }
        return new Loader().loadClass("Fixture");
    }
    static Object execute(Path path,Class<?>[] types,Object[] args)throws Exception {
        Object[] copy=args.clone();for(int i=0;i<copy.length;i++)if(copy[i] instanceof int[] a)copy[i]=a.clone();
        try{return load(path).getMethod("m",types).invoke(null,copy);}
        catch(InvocationTargetException e){return "throws:"+e.getCause().getClass().getName();}
    }
    static ClassNode join(boolean local,boolean wrong) {
        ClassNode c=clazz();MethodNode m=method(c,ACC_PUBLIC|ACC_STATIC,"m","(I)I");LabelNode no=new LabelNode(),end=new LabelNode();
        var(m,ILOAD,0);ins(m,ICONST_1,ISUB);m.instructions.add(new JumpInsnNode(wrong?IFGE:IFLT,no));
        ins(m,ICONST_1);if(local)var(m,ISTORE,2);m.instructions.add(new JumpInsnNode(GOTO,end));
        m.instructions.add(no);ins(m,ICONST_2);if(local)var(m,ISTORE,2);m.instructions.add(end);if(local)var(m,ILOAD,2);ins(m,IRETURN);return c;
    }
    static ClassNode subtract(boolean order) {
        ClassNode c=clazz();MethodNode m=method(c,ACC_PUBLIC|ACC_STATIC,"m","(II)I");var(m,ILOAD,order?0:1);var(m,ILOAD,order?1:0);ins(m,ISUB,IRETURN);return c;
    }
    static void field(MethodNode m,int op,String name){m.instructions.add(new FieldInsnNode(op,"Fixture",name,"I"));}
    static void invoke(MethodNode m,int op,String owner,String name,String desc){FifthSelfTest.invoke(m,op,owner,name,desc);}
    static ClassNode calls(boolean order) {
        ClassNode c=clazz();c.fields.add(new FieldNode(ACC_STATIC,"state","I",null,null));
        for(int i=1;i<=2;i++){MethodNode m=method(c,ACC_PUBLIC|ACC_STATIC,"f"+i,"()V");field(m,GETSTATIC,"state");m.instructions.add(new IntInsnNode(BIPUSH,10));ins(m,IMUL);m.instructions.add(new LdcInsnNode(i));ins(m,IADD);field(m,PUTSTATIC,"state");ins(m,RETURN);}
        MethodNode m=method(c,ACC_PUBLIC|ACC_STATIC,"m","()I");invoke(m,INVOKESTATIC,"Fixture",order?"f1":"f2","()V");invoke(m,INVOKESTATIC,"Fixture",order?"f2":"f1","()V");field(m,GETSTATIC,"state");ins(m,IRETURN);return c;
    }
    static ClassNode monitorCallback(boolean inside) {
        ClassNode c=clazz();MethodNode m=method(c,ACC_PUBLIC|ACC_STATIC,"m","(Ljava/lang/Object;)Z");
        var(m,ALOAD,0);ins(m,MONITORENTER);
        if(!inside){var(m,ALOAD,0);ins(m,MONITOREXIT);}
        var(m,ALOAD,0);invoke(m,INVOKESTATIC,"java/lang/Thread","holdsLock","(Ljava/lang/Object;)Z");
        if(inside){var(m,ALOAD,0);ins(m,MONITOREXIT);}ins(m,IRETURN);return c;
    }
    static ClassNode reads(boolean saved,boolean vol) {
        ClassNode c=clazz();c.fields.add(new FieldNode(ACC_STATIC|(vol?ACC_VOLATILE:0),"state","I",null,null));
        MethodNode m=method(c,ACC_PUBLIC|ACC_STATIC,"m","()I");
        ins(m,ICONST_1);field(m,PUTSTATIC,"state");field(m,GETSTATIC,"state");var(m,ISTORE,0);
        ins(m,ICONST_2);field(m,PUTSTATIC,"state");
        if(!vol){field(m,GETSTATIC,"state");var(m,ISTORE,1);var(m,ILOAD,saved?0:1);}
        else if(saved)var(m,ILOAD,0);else field(m,GETSTATIC,"state");
        invoke(m,INVOKESTATIC,"java/lang/Math","abs","(I)I");ins(m,IRETURN);return c;
    }
    static ClassNode arrayReads(boolean saved) {
        ClassNode c=clazz();MethodNode m=method(c,ACC_PUBLIC|ACC_STATIC,"m","([I)I");
        var(m,ALOAD,0);ins(m,ICONST_0,IALOAD);var(m,ISTORE,1);
        var(m,ALOAD,0);ins(m,ICONST_0,ICONST_2,IASTORE);
        var(m,ALOAD,0);ins(m,ICONST_0,IALOAD);var(m,ISTORE,2);var(m,ILOAD,saved?1:2);ins(m,IRETURN);return c;
    }
    static ClassNode volatilePop(boolean extra) {
        ClassNode c=constant(1);c.fields.add(new FieldNode(ACC_STATIC|ACC_VOLATILE,"state","I",null,null));
        if(extra){InsnList list=new InsnList();list.add(new FieldInsnNode(GETSTATIC,"Fixture","state","I"));list.add(new InsnNode(POP));c.methods.get(0).instructions.insert(list);}
        return c;
    }
    static ClassNode zero(boolean single) {
        ClassNode c=clazz();MethodNode m=method(c,ACC_PUBLIC|ACC_STATIC,"m","(I)I");LabelNode yes=new LabelNode();var(m,ILOAD,0);
        if(!single)ins(m,ICONST_0);m.instructions.add(new JumpInsnNode(single?IFNE:IF_ICMPNE,yes));ins(m,ICONST_1,IRETURN);m.instructions.add(yes);ins(m,ICONST_2,IRETURN);return c;
    }
    static ClassNode guarded(boolean local,boolean nonstandard) {
        ClassNode c=clazz();MethodNode m=method(c,ACC_PUBLIC|ACC_STATIC,"m","(Ljava/lang/Object;Ljava/lang/String;)I");
        LabelNode start=new LabelNode(),end=new LabelNode(),handler=new LabelNode(),hend=new LabelNode(),other=new LabelNode(),join=new LabelNode();
        var(m,ALOAD,0);ins(m,MONITORENTER);m.instructions.add(start);
        var(m,ALOAD,1);invoke(m,INVOKEVIRTUAL,"java/lang/String","length","()I");m.instructions.add(new JumpInsnNode(IFEQ,other));
        ins(m,ICONST_1);if(local)var(m,ISTORE,3);m.instructions.add(new JumpInsnNode(GOTO,join));m.instructions.add(other);
        ins(m,ICONST_2);if(local)var(m,ISTORE,3);m.instructions.add(join);if(local)var(m,ILOAD,3);
        var(m,ALOAD,0);ins(m,MONITOREXIT);m.instructions.add(end);ins(m,IRETURN);
        m.instructions.add(handler);var(m,ASTORE,2);if(nonstandard)ins(m,NOP);var(m,ALOAD,0);ins(m,MONITOREXIT);m.instructions.add(hend);var(m,ALOAD,2);ins(m,ATHROW);
        m.tryCatchBlocks.add(new TryCatchBlockNode(start,end,handler,null));m.tryCatchBlocks.add(new TryCatchBlockNode(handler,hend,handler,null));return c;
    }
    static void pathsBounds()throws Exception {
        ClassNode a=join(true,false),b=join(false,false);Path l=save("ablation-l",a),r=save("ablation-r",b);
        System.setProperty("semanticdiff.disable","paths");try{cli("paths-disabled",l,r,"DIFF");}finally{System.clearProperty("semanticdiff.disable");}
        ClassNode many=clazz();MethodNode m=method(many,ACC_PUBLIC|ACC_STATIC,"m","(I)I");
        for(int i=0;i<13;i++){LabelNode end=new LabelNode();var(m,ILOAD,0);m.instructions.add(new JumpInsnNode(IFEQ,end));ins(m,NOP);m.instructions.add(end);}
        ins(m,ICONST_1,IRETURN);Path p=save("paths-limit",many);JarIndex j=JarIndex.load(p);
        var result=PathComparison.compare(j.readTree("Fixture").methods.get(0),j.readTree("Fixture").methods.get(0),j,j);
        ok("4096 cap",!result.same()&&result.reason().contains("4096"));
        ClassNode loop=clazz();m=method(loop,ACC_PUBLIC|ACC_STATIC,"m","()V");LabelNode again=new LabelNode();m.instructions.add(again);m.instructions.add(new JumpInsnNode(GOTO,again));p=save("paths-loop",loop);j=JarIndex.load(p);
        result=PathComparison.compare(j.readTree("Fixture").methods.get(0),j.readTree("Fixture").methods.get(0),j,j);
        ok("normal cycle",!result.same()&&result.reason().contains("cycle"));
    }
    static ClassNode nested(int flags,int own) {
        ClassNode c=clazz();c.access=own;c.innerClasses.add(new InnerClassNode("Fixture","Outer","Fixture",flags));
        MethodNode m=method(c,ACC_PUBLIC|ACC_STATIC,"m","()I");ins(m,ICONST_1,IRETURN);return c;
    }
    static void inner()throws Exception {
        Path list=work.resolve("access.txt");Files.writeString(list,"Fixture # approved: https://example.test/176\n");
        Path l=save("inner-l",nested(ACC_STATIC,ACC_PUBLIC)),r=save("inner-r",nested(ACC_PRIVATE|ACC_STATIC,ACC_PUBLIC));
        cli("inner-default",l,r,"DIFF");cli("inner-approved",l,r,"SAME","--access-widen",list.toString());
        cli("inner-reverse",r,l,"DIFF","--access-widen",list.toString());
        ok("JVM inner access reflected",load(l).getModifiers()!=load(r).getModifiers());
        Path finalFlag=save("inner-final",nested(ACC_STATIC|ACC_FINAL,ACC_PUBLIC));cli("inner-other-flag",finalFlag,r,"DIFF","--access-widen",list.toString());
        Path classFlag=save("inner-class-flags",nested(ACC_STATIC,ACC_PUBLIC|ACC_FINAL));cli("inner-class-access",classFlag,r,"DIFF","--access-widen",list.toString());
        ok("JVM inner final",Modifier.isFinal(load(finalFlag).getModifiers())!=Modifier.isFinal(load(r).getModifiers()));
        ClassNode refL=constant(1),refR=constant(1);
        refL.innerClasses.add(new InnerClassNode("Other$Inner","Other","Inner",ACC_PUBLIC|ACC_STATIC));
        refR.innerClasses.add(new InnerClassNode("Other$Inner","Other","Inner",ACC_PRIVATE|ACC_STATIC));
        Files.writeString(list,"Other$Inner # approved: https://example.test/176\n");
        l=save("inner-reference-l",refL);r=save("inner-reference-r",refR);
        cli("inner-referenced-entry",l,r,"SAME","--access-widen",list.toString());
        cli("inner-referenced-reverse",r,l,"DIFF","--access-widen",list.toString());
        refL.innerClasses.get(0).access|=ACC_FINAL;l=save("inner-reference-final",refL);
        cli("inner-referenced-final",l,r,"DIFF","--access-widen",list.toString());
    }
    static ClassNode constant(int value) {ClassNode c=clazz();MethodNode m=method(c,ACC_PUBLIC|ACC_STATIC,"m","()I");m.instructions.add(new LdcInsnNode(value));ins(m,IRETURN);return c;}
    static void deviations()throws Exception {
        Path l=save("deviation-l",constant(1)),r=save("deviation-r",constant(2));Path list=work.resolve("deviations.txt");
        Files.writeString(list,"Fixture#m()I method-diff # approved: https://example.test/176\n");
        cli("method-default",l,r,"DIFF");String json=cli("method-approved",l,r,"DEVIATION","--deviations",list.toString());
        ok("full deviation",json.contains("LEFT\\n")&&json.contains("RIGHT\\n")&&json.contains("Integer:2"));
        ClassNode other=constant(1);MethodNode m=method(other,ACC_PUBLIC|ACC_STATIC,"other","()I");ins(m,ICONST_1,IRETURN);
        ClassNode other2=constant(2);m=method(other2,ACC_PUBLIC|ACC_STATIC,"other","()I");ins(m,ICONST_2,IRETURN);
        Path x=save("deviation-other-l",other),y=save("deviation-other-r",other2);cli("method-outside",x,y,"DIFF","--deviations",list.toString());
        ok("JVM unapproved other differs",!load(x).getMethod("other").invoke(null).equals(load(y).getMethod("other").invoke(null)));
        Files.writeString(list,"Fixture#typo()I method-diff # approved: https://example.test/176\n");
        boolean rejected=false;try{cli("method-typo",l,r,"DIFF","--deviations",list.toString());}catch(IllegalArgumentException expected){rejected=true;}ok("method typo rejected",rejected);
    }
    static ClassNode orphan(boolean extra,int mode) {
        ClassNode c=clazz();MethodNode m=method(c,ACC_PUBLIC|ACC_STATIC,"m",mode==1?"()Ljava/lang/String;":"()I");
        if(mode==1){m.instructions.add(new LdcInsnNode("hidden"));ins(m,ARETURN);}
        else if(mode==2){m.instructions.add(new LdcInsnNode(new Handle(H_INVOKESTATIC,"Fixture","hidden","()I",false)));invoke(m,INVOKEVIRTUAL,"java/lang/invoke/MethodHandle","invokeExact","()I");ins(m,IRETURN);}
        else if(mode==3){invoke(m,INVOKESTATIC,"Fixture","hidden","()I");ins(m,IRETURN);}
        else ins(m,ICONST_1,IRETURN);
        if(extra){m=method(c,ACC_PRIVATE|ACC_STATIC|(mode==4?0:ACC_SYNTHETIC),"hidden","()I");m.instructions.add(new LdcInsnNode(7));ins(m,IRETURN);}return c;
    }
    static void orphans()throws Exception {
        Path l=save("orphan-l",orphan(false,0)),r=save("orphan-r",orphan(true,0));cli("orphan-approved",l,r,"ORPHAN_IGNORED");cli("orphan-left",r,l,"DIFF");
        for(int mode:new int[]{1,2,3,4}) {
            l=save("orphan-l"+mode,orphan(false,mode));r=save("orphan-r"+mode,orphan(true,mode));cli("orphan-referenced"+mode,l,r,"DIFF");
            if(mode==1||mode==4) {
                boolean missing=false;try{load(l).getDeclaredMethod(mode==1?(String)execute(l,new Class<?>[]{},new Object[]{}):"hidden");}catch(NoSuchMethodException ex){missing=true;}
                ok("JVM orphan reflection "+mode,missing&&load(r).getDeclaredMethod("hidden")!=null);
            } else ok("JVM orphan linkage "+mode,!Objects.equals(execute(l,new Class<?>[]{},new Object[]{}),execute(r,new Class<?>[]{},new Object[]{})));
        }
        inheritedOrphans();
    }

    static List<ClassNode> inheritedOrphan(boolean extra,int mode,boolean indirect) {
        ClassNode c=clazz();MethodNode m=method(c,ACC_PUBLIC|ACC_STATIC,"m","()I");
        String hiddenDesc=inheritedDescriptor(mode);
        Handle hidden=new Handle(H_INVOKESTATIC,"Child","hidden",hiddenDesc,false);
        if(mode==0)invoke(m,INVOKESTATIC,"Child","hidden","()I");
        else if(mode==1) {
            m.instructions.add(new LdcInsnNode(hidden));
            invoke(m,INVOKEVIRTUAL,"java/lang/invoke/MethodHandle","invokeExact","()I");
        } else if(mode==2) {
            m.instructions.add(new InvokeDynamicInsnNode("getAsInt","()Ljava/util/function/IntSupplier;",
                    bsm(),Type.getMethodType("()I"),hidden,Type.getMethodType("()I")));
            m.instructions.add(new MethodInsnNode(INVOKEINTERFACE,"java/util/function/IntSupplier","getAsInt","()I",true));
        } else if(mode==3) {
            Handle bootstrap=new Handle(H_INVOKESTATIC,"java/lang/invoke/ConstantBootstraps","invoke",
                    "(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/Class;Ljava/lang/invoke/MethodHandle;[Ljava/lang/Object;)Ljava/lang/Object;",false);
            m.instructions.add(new LdcInsnNode(new ConstantDynamic("value","I",bootstrap,hidden)));
        } else if(mode==4)m.instructions.add(new InvokeDynamicInsnNode("value","()I",hidden));
        else m.instructions.add(new LdcInsnNode(new ConstantDynamic("value","I",hidden)));
        ins(m,IRETURN);
        if(extra) {
            m=method(c,ACC_PRIVATE|ACC_STATIC|ACC_SYNTHETIC,"hidden",hiddenDesc);
            if(mode<4)ins(m,ICONST_5,IRETURN);
            else if(mode==4) {
                m.instructions.add(new TypeInsnNode(NEW,"java/lang/invoke/ConstantCallSite"));ins(m,DUP);
                m.instructions.add(new FieldInsnNode(GETSTATIC,"java/lang/Integer","TYPE","Ljava/lang/Class;"));
                ins(m,ICONST_5);invoke(m,INVOKESTATIC,"java/lang/Integer","valueOf","(I)Ljava/lang/Integer;");
                invoke(m,INVOKESTATIC,"java/lang/invoke/MethodHandles","constant","(Ljava/lang/Class;Ljava/lang/Object;)Ljava/lang/invoke/MethodHandle;");
                invoke(m,INVOKESPECIAL,"java/lang/invoke/ConstantCallSite","<init>","(Ljava/lang/invoke/MethodHandle;)V");ins(m,ARETURN);
            } else {
                ins(m,ICONST_5);invoke(m,INVOKESTATIC,"java/lang/Integer","valueOf","(I)Ljava/lang/Integer;");ins(m,ARETURN);
            }
        }
        ClassNode child=clazz();child.name="Child";child.superName=indirect?"Middle":"Fixture";
        ClassNode middle=clazz();middle.name="Middle";middle.superName="Fixture";
        return indirect?List.of(framed(c),framed(child),framed(middle)):List.of(framed(c),framed(child));
    }

    static String inheritedDescriptor(int mode) {
        if(mode<4)return "()I";
        return "(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;"+(mode==4
                ?"Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite;":"Ljava/lang/Class;)Ljava/lang/Object;");
    }

    static void inheritedOrphans()throws Exception {
        String[] modes={"call","handle","indy-argument","condy-argument","indy-bootstrap","condy-bootstrap"};
        for(int mode=0;mode<modes.length;mode++)for(boolean indirect:new boolean[]{false,true}) {
            String name="orphan-inherited-"+modes[mode]+(indirect?"-indirect":"");
            Path l=jar(name+"-l",inheritedOrphan(false,mode,indirect));
            Path r=jar(name+"-r",inheritedOrphan(true,mode,indirect));
            // Execute exactly the JAR bytes compared below, with a fresh platform-parent loader per side.
            Object left=execute(l,new Class<?>[]{},new Object[]{}),right=execute(r,new Class<?>[]{},new Object[]{});
            ok("JVM "+name+" left="+left+" right="+right,
                    Objects.equals(left,"throws:java.lang.NoSuchMethodError")&&Objects.equals(right,5));
            String json=cli(name,l,r,"DIFF");
            ok(name+" method ONLY_RIGHT",json.contains("\"method\": \"hidden"+inheritedDescriptor(mode)+"\", \"status\": \"ONLY_RIGHT\""));
            ok(name+" no orphan ignored",json.contains("\"ORPHAN_IGNORED\": 0"));
        }
        // Child may be supplied only at runtime. Missing hierarchy metadata is not evidence of no reference.
        Path l=jar("orphan-unknown-owner-l",List.of(inheritedOrphan(false,0,false).get(0)));
        Path r=jar("orphan-unknown-owner-r",List.of(inheritedOrphan(true,0,false).get(0)));
        String json=cli("orphan-unknown-owner",l,r,"DIFF");
        ok("unknown owner method ONLY_RIGHT",json.contains("\"method\": \"hidden()I\", \"status\": \"ONLY_RIGHT\""));
    }
}
