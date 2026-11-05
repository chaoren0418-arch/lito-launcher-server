package local.lito381.semanticdiff;

import java.lang.reflect.InvocationTargetException;
import java.nio.file.*;
import java.util.*;
import java.util.zip.*;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
import static local.lito381.semanticdiff.FourthSelfTest.*;

/** Issue 203: the CLI and isolated JVMs consume the same framed class bytes. */
public final class EighthSelfTest implements Opcodes {
    static int checks;
    static void ok(String text,boolean value){if(!value)throw new AssertionError(text);checks++;System.out.println("PASS "+text);}
    public static void main(String[] args)throws Exception {
        work=Path.of(args[0]);bin=Path.of(args[1]);Files.createDirectories(work);Files.createDirectories(bin);
        fixture("lambda-paths",lambdas(true,0),lambdas(false,0),"LAMBDA_PAIRED_BY_CALLSITE",true);
        fixture("lambda-swapped-sites",lambdas(true,0),lambdas(false,1),"DIFF",false);
        fixture("lambda-call-order",lambdas(true,0),lambdas(false,2),"DIFF",false);
        fixture("lambda-nested",lambdas(true,3),lambdas(false,3),"LAMBDA_PAIRED_BY_CALLSITE",true);
        fixture("lambda-nested-different",lambdas(true,3),lambdas(false,4),"DIFF",false);
        fixture("lambda-shared",lambdas(true,5),lambdas(false,5),"LAMBDA_PAIRED_BY_CALLSITE",true);
        fixture("lambda-shared-inconsistent",lambdas(true,5),lambdas(false,0),"DIFF",false);
        fixture("lambda-many-to-one",lambdas(true,0),lambdas(false,5),"DIFF",false);
        fixture("lambda-cfg",lambdaLoop(true,0),lambdaLoop(false,0),"LAMBDA_PAIRED_BY_CALLSITE",true);
        fixture("lambda-cfg-different",lambdaLoop(true,0),lambdaLoop(false,2),"DIFF",false);
        fixture("cfg-inversion",loop(true,0),loop(false,0),"SAME_CFG",true);
        fixture("cfg-inversion-no-swap",loop(true,0),loop(false,1),"DIFF",false);
        fixture("cfg-missing-call",loop(true,0),loop(false,2),"DIFF",false);
        fixture("cfg-backedge",loop(true,0),loop(false,3),"DIFF",false);
        fixture("cfg-handler",loop(true,4),loop(false,4),"SAME_CFG",true);
        fixture("cfg-handler-different",loop(true,4),loop(false,5),"DIFF",false);
        fixture("cfg-alias",alias(true,0),alias(false,0),"SAME_CFG",true);
        fixture("cfg-alias-overwrite",alias(true,1),alias(false,0),"DIFF",false);
        fixture("cfg-alias-branch-write",alias(true,2),alias(false,0),"DIFF",false);
        fixture("cfg-copy-transport",copies(true,false),copies(false,false),"SAME_CFG",true);
        fixture("cfg-copy-source",copies(true,false),copies(false,true),"DIFF",false);
        for(int mode=0;mode<=6;mode++)bridges(mode);
        bounds();
        System.out.println("EIGHTH SELFTEST ALL PASS checks="+checks);
    }
    static void call(MethodNode m,int op,String owner,String name,String desc){m.instructions.add(new MethodInsnNode(op,owner,name,desc,false));}
    static void jump(MethodNode m,int op,LabelNode l){m.instructions.add(new JumpInsnNode(op,l));}
    static void mark(MethodNode m,int n){m.instructions.add(new LdcInsnNode(n));call(m,INVOKESTATIC,"Fixture","mark","(I)I");ins(m,POP);}
    static ClassNode base() {
        ClassNode c=clazz();c.fields.add(new FieldNode(ACC_PUBLIC|ACC_STATIC,"trace","I",null,null));
        MethodNode m=method(c,ACC_PUBLIC|ACC_STATIC,"mark","(I)I");
        m.instructions.add(new FieldInsnNode(GETSTATIC,"Fixture","trace","I"));m.instructions.add(new IntInsnNode(BIPUSH,10));ins(m,IMUL);var(m,ILOAD,0);ins(m,IADD);
        m.instructions.add(new FieldInsnNode(PUTSTATIC,"Fixture","trace","I"));var(m,ILOAD,0);ins(m,IRETURN);return c;
    }
    static final Handle BSM=new Handle(H_INVOKESTATIC,"java/lang/invoke/LambdaMetafactory","metafactory",
            "(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite;",false);
    static void indy(MethodNode m,String name,String desc,String sam,String target,String body) {
        m.instructions.add(new InvokeDynamicInsnNode(name,desc,BSM,Type.getMethodType(sam),new Handle(H_INVOKESTATIC,"Fixture",target,body,false),Type.getMethodType(sam)));
    }
    static ClassNode lambdas(boolean local,int mode) {
        ClassNode c=base();String one=local?"lambda$source$10":"lambda$target$0",two=local?"lambda$source$11":"lambda$target$1";
        body(c,one,local,1,mode==2||mode==4);body(c,two,local,3,false);
        MethodNode m=method(c,ACC_PUBLIC|ACC_STATIC,"m","(I)I");
        if(mode==3||mode==4) {
            String outer=local?"lambda$outer$12":"lambda$outer$2";
            MethodNode o=method(c,ACC_PRIVATE|ACC_STATIC|ACC_SYNTHETIC,outer,"()Ljava/util/function/IntUnaryOperator;");
            indy(o,"applyAsInt","()Ljava/util/function/IntUnaryOperator;","(I)I",one,"(I)I");
            if(local){var(o,ASTORE,0);var(o,ALOAD,0);}ins(o,ARETURN);
            // Exact SAM descriptor avoids a CHECKCAST, which has its own observable event.
            indy(m,"call","()Ljava/util/concurrent/Callable;","()Ljava/lang/Object;",outer,"()Ljava/util/function/IntUnaryOperator;");
            call(m,INVOKEINTERFACE,"java/util/concurrent/Callable","call","()Ljava/lang/Object;");
            ((MethodInsnNode)m.instructions.getLast()).itf=true;
            m.instructions.add(new TypeInsnNode(CHECKCAST,"java/util/function/IntUnaryOperator"));
            apply(m);ins(m,POP);
        }else {
            indy(m,"applyAsInt","()Ljava/util/function/IntUnaryOperator;","(I)I",mode==1?two:one,"(I)I");apply(m);ins(m,POP);
            indy(m,"applyAsInt","()Ljava/util/function/IntUnaryOperator;","(I)I",mode==1?one:mode==5?one:two,"(I)I");apply(m);ins(m,POP);
        }
        m.instructions.add(new FieldInsnNode(GETSTATIC,"Fixture","trace","I"));ins(m,IRETURN);return c;
    }
    static void apply(MethodNode m){var(m,ILOAD,0);m.instructions.add(new MethodInsnNode(INVOKEINTERFACE,"java/util/function/IntUnaryOperator","applyAsInt","(I)I",true));}
    static void body(ClassNode c,String name,boolean local,int value,boolean change) {
        MethodNode m=method(c,ACC_PRIVATE|ACC_STATIC|ACC_SYNTHETIC,name,"(I)I");LabelNode zero=new LabelNode(),join=new LabelNode();
        var(m,ILOAD,0);jump(m,IFEQ,zero);mark(m,change?value+1:value);mark(m,change?value:value+1);ins(m,ICONST_1);
        if(local)var(m,ISTORE,1);jump(m,GOTO,join);m.instructions.add(zero);ins(m,ICONST_2);if(local)var(m,ISTORE,1);
        m.instructions.add(join);if(local)var(m,ILOAD,1);ins(m,IRETURN);
    }
    static ClassNode loop(boolean left,int mode) {
        ClassNode c=base();MethodNode m=method(c,ACC_PUBLIC|ACC_STATIC,"m","(I)I");
        LabelNode head=new LabelNode(),condition=new LabelNode(),body=new LabelNode(),done=new LabelNode(),end=new LabelNode(),handler=new LabelNode();
        m.instructions.add(head);mark(m,1);m.instructions.add(condition);var(m,ILOAD,0);
        if(left)jump(m,IFLE,done);
        else {jump(m,mode==1?IFLE:IFGT,body);jump(m,GOTO,done);}
        m.instructions.add(body);if(mode!=2)mark(m,2);
        if(mode>=4) {ins(m,ACONST_NULL);call(m,INVOKEVIRTUAL,"java/lang/Object","hashCode","()I");ins(m,POP);}
        m.instructions.add(new IincInsnNode(0,-1));jump(m,GOTO,mode==3?condition:head);
        m.instructions.add(done);m.instructions.add(new FieldInsnNode(GETSTATIC,"Fixture","trace","I"));ins(m,IRETURN);m.instructions.add(end);
        if(mode>=4) {
            m.instructions.add(handler);ins(m,POP);m.instructions.add(new LdcInsnNode(mode==5?99:42));ins(m,IRETURN);
            m.tryCatchBlocks.add(new TryCatchBlockNode(head,end,handler,"java/lang/NullPointerException"));
        }
        return c;
    }
    static ClassNode lambdaLoop(boolean left,int mode) {
        ClassNode c=loop(left,mode);MethodNode body=c.methods.stream().filter(m->m.name.equals("m")).findFirst().orElseThrow();
        body.name=left?"lambda$loop$1":"lambda$loop$2";body.access=ACC_PRIVATE|ACC_STATIC|ACC_SYNTHETIC;
        MethodNode m=method(c,ACC_PUBLIC|ACC_STATIC,"m","(I)I");
        indy(m,"applyAsInt","()Ljava/util/function/IntUnaryOperator;","(I)I",body.name,"(I)I");apply(m);ins(m,IRETURN);return c;
    }
    static ClassNode frame(ClassNode c) {
        ClassWriter writer=new ClassWriter(ClassWriter.COMPUTE_FRAMES|ClassWriter.COMPUTE_MAXS) {
            @Override protected String getCommonSuperClass(String a,String b){return "java/lang/Object";}
        };
        c.accept(writer);ClassNode out=new ClassNode();new ClassReader(writer.toByteArray()).accept(out,0);return out;
    }
    static ClassNode alias(boolean left,int mode) {
        ClassNode c=base();MethodNode m=method(c,ACC_PUBLIC|ACC_STATIC,"m","(I)I");
        m.instructions.add(new IntInsnNode(BIPUSH,7));if(left)ins(m,DUP);var(m,ISTORE,1);if(left)var(m,ISTORE,2);
        LabelNode head=new LabelNode(),done=new LabelNode(),skip=new LabelNode(),body=new LabelNode();
        if(mode==1){ins(m,ICONST_1);var(m,ISTORE,1);}
        if(mode==2){var(m,ILOAD,0);jump(m,IFEQ,skip);ins(m,ICONST_1);var(m,ISTORE,2);m.instructions.add(skip);}
        m.instructions.add(head);var(m,ILOAD,0);
        if(left)jump(m,IFEQ,done);else{jump(m,IFNE,body);jump(m,GOTO,done);}m.instructions.add(body);
        var(m,ILOAD,1);var(m,ILOAD,left?2:1);ins(m,IADD);call(m,INVOKESTATIC,"Fixture","mark","(I)I");ins(m,POP);
        m.instructions.add(new IincInsnNode(0,-1));jump(m,GOTO,head);m.instructions.add(done);
        m.instructions.add(new FieldInsnNode(GETSTATIC,"Fixture","trace","I"));ins(m,IRETURN);return c;
    }
    static ClassNode copies(boolean dup,boolean different) {
        ClassNode c=base();MethodNode m=method(c,ACC_PUBLIC|ACC_STATIC,"m","(I)I");
        LabelNode head=new LabelNode(),done=new LabelNode(),body=new LabelNode();m.instructions.add(head);var(m,ILOAD,0);
        if(dup)jump(m,IFEQ,done);else{jump(m,IFNE,body);jump(m,GOTO,done);}m.instructions.add(body);
        var(m,ILOAD,0);if(dup)ins(m,DUP);else if(different)ins(m,ICONST_1);else var(m,ILOAD,0);
        ins(m,IADD);call(m,INVOKESTATIC,"Fixture","mark","(I)I");ins(m,POP);m.instructions.add(new IincInsnNode(0,-1));jump(m,GOTO,head);
        m.instructions.add(done);m.instructions.add(new FieldInsnNode(GETSTATIC,"Fixture","trace","I"));ins(m,IRETURN);return c;
    }
    static Path save(String name,List<ClassNode> cs)throws Exception{return jar(name,cs.stream().map(EighthSelfTest::frame).toList());}
    static String cli(String name,Path l,Path r,String expected)throws Exception {
        Path out=work.resolve(name+".json");
        ok(name+" exit",SemanticDiff.run(new String[]{"--left",l.toString(),"--right",r.toString(),"--classes","Fixture","--json",out.toString(),"--md",work.resolve(name+".md").toString()})==0);
        String json=Files.readString(out);ok(name+" "+expected,json.contains("\"status\": \""+expected+"\""));return json;
    }
    static Class<?> load(Path jar)throws Exception {
        Map<String,byte[]> bytes=new HashMap<>();try(ZipFile z=new ZipFile(jar.toFile())) {
            for(var e:Collections.list(z.entries()))if(e.getName().endsWith(".class"))bytes.put(e.getName().replace('/','.').replace(".class",""),z.getInputStream(e).readAllBytes());
        }
        return new ClassLoader(ClassLoader.getPlatformClassLoader()) {
            @Override protected Class<?> findClass(String name)throws ClassNotFoundException {
                byte[] b=bytes.get(name);if(b==null)throw new ClassNotFoundException(name);return defineClass(name,b,0,b.length);
            }
        }.loadClass("Fixture");
    }
    static Object execute(Path jar,int input)throws Exception {
        try{return load(jar).getMethod("m",int.class).invoke(null,input);}
        catch(InvocationTargetException ex){return ex.getCause().getClass().getName();}
    }
    static void fixture(String name,ClassNode a,ClassNode b,String expected,boolean same)throws Exception {
        Path l=save(name+"-l",List.of(a)),r=save(name+"-r",List.of(b));cli(name,l,r,expected);cli(name+"-reverse",r,l,expected);
        Object x=execute(l,2),y=execute(r,2);ok("JVM "+name+" left="+x+" right="+y,Objects.equals(x,y)==same);
        if(name.equals("lambda-paths")||name.equals("lambda-nested"))for(int input:new int[]{0,-1})
            ok("JVM "+name+" input="+input,Objects.equals(execute(l,input),execute(r,input)));
        if(name.equals("lambda-paths")||name.equals("cfg-inversion")) {
            System.setProperty("semanticdiff.disable",name.startsWith("lambda")?"callsites":"cfg");
            try{cli(name+"-disabled",l,r,"DIFF");}finally{System.clearProperty("semanticdiff.disable");}
        }
    }
    static ClassNode named(String name,String parent,int access) {
        ClassNode c=clazz();c.name=name;c.superName=parent;c.access=access|ACC_SUPER;
        MethodNode init=method(c,ACC_PUBLIC,"<init>","()V");var(init,ALOAD,0);call(init,INVOKESPECIAL,parent,"<init>","()V");ins(init,RETURN);return c;
    }
    static void bridges(int mode)throws Exception {
        List<Path> jars=new ArrayList<>();
        for(boolean left:new boolean[]{true,false}) {
            ClassNode ancestor=named("Ancestor","java/lang/Object",mode==3?ACC_PUBLIC:0);
            String desc=mode==2?"(Ljava/lang/Object;)Ljava/lang/Object;":"(I)I";
            MethodNode inherited=method(ancestor,mode==6&&!left?0:ACC_PUBLIC,"value",desc);
            var(inherited,mode==2?ALOAD:ILOAD,1);if(mode!=2){ins(inherited,ICONST_1,IADD,IRETURN);}else ins(inherited,ARETURN);
            MethodNode other=method(ancestor,ACC_PUBLIC,"other",desc);var(other,mode==2?ALOAD:ILOAD,1);if(mode!=2){ins(other,ICONST_2,IADD,IRETURN);}else ins(other,ARETURN);
            ClassNode middle=named("Middle","Ancestor",ACC_PUBLIC),c=named("Fixture","Middle",ACC_PUBLIC);
            if(mode==4) {MethodNode override=method(middle,ACC_PUBLIC,"value",desc);var(override,ILOAD,1);ins(override,ICONST_3,IADD,IRETURN);}
            if(left) {
                MethodNode bridge=method(c,0x1041,"value",desc);var(bridge,ALOAD,0);var(bridge,mode==2?ALOAD:ILOAD,1);
                if(mode==2)bridge.instructions.add(new TypeInsnNode(CHECKCAST,"java/lang/String"));
                call(bridge,INVOKESPECIAL,mode==4?"Ancestor":"Middle",mode==1?"other":"value",desc);ins(bridge,mode==2?ARETURN:IRETURN);
            }
            if(mode==5) {MethodNode override=method(c,ACC_PUBLIC,"value","(J)J");var(override,LLOAD,1);ins(override,LRETURN);}
            MethodNode m=method(c,ACC_PUBLIC|ACC_STATIC,"m","(I)I");
            if(mode==3) {
                m.instructions.add(new LdcInsnNode(Type.getObjectType("Fixture")));call(m,INVOKEVIRTUAL,"java/lang/Class","getDeclaredMethods","()[Ljava/lang/reflect/Method;");ins(m,ARRAYLENGTH,IRETURN);
            }else {
                m.instructions.add(new TypeInsnNode(NEW,"Fixture"));ins(m,DUP);call(m,INVOKESPECIAL,"Fixture","<init>","()V");var(m,ILOAD,0);
                if(mode==2)call(m,INVOKESTATIC,"java/lang/Integer","valueOf","(I)Ljava/lang/Integer;");
                call(m,INVOKEVIRTUAL,"Fixture","value",desc);if(mode==2){ins(m,POP,ICONST_1);}ins(m,IRETURN);
            }
            jars.add(save("bridge-"+mode+"-"+left,List.of(c,middle,ancestor)));
        }
        boolean same=mode==0||mode==5;String name="bridge-"+mode;
        String json=cli(name,jars.get(0),jars.get(1),same?"VISIBILITY_BRIDGE":"DIFF");
        if(!same)ok(name+" retains ONLY_LEFT",json.contains("ONLY_LEFT"));
        Object x=execute(jars.get(0),2),y=execute(jars.get(1),2);
        // ACC_SUPER selects Middle even when a malformed bridge names Ancestor.
        // Exercise a real override dispatch difference via a separately constructed subclass below.
        ok("JVM "+name+" left="+x+" right="+y,Objects.equals(x,y)==(same||mode==4||mode==6));
        cli(name+"-reverse",jars.get(1),jars.get(0),"DIFF");
        if(mode==4)overrideDispatch();
        if(mode==6) {
            Path approval=work.resolve("bridge-access.txt"),out=work.resolve("bridge-access.json");
            Files.writeString(approval,"Ancestor#value(I)I # approved: https://example.test/203\n");
            ok("bridge explicit access CLI",SemanticDiff.run(new String[]{"--left",jars.get(0).toString(),"--right",jars.get(1).toString(),
                    "--classes","Fixture,Ancestor","--access-widen",approval.toString(),"--json",out.toString(),"--md",work.resolve("bridge-access.md").toString()})==0);
            String approved=Files.readString(out);ok("bridge explicit access accepted",approved.contains("\"DIFF\": 0")&&approved.contains("\"status\": \"VISIBILITY_BRIDGE\""));
        }
    }
    static void overrideDispatch()throws Exception {
        List<Path> jars=new ArrayList<>();
        for(boolean left:new boolean[]{true,false}) {
            ClassNode p=named("Parent","java/lang/Object",0),c=named("Fixture","Parent",ACC_PUBLIC),child=named("Child","Fixture",ACC_PUBLIC);
            MethodNode m=method(p,ACC_PUBLIC,"value","(I)I");ins(m,ICONST_1,IRETURN);
            m=method(child,ACC_PUBLIC,"value","(I)I");ins(m,ICONST_2,IRETURN);
            // A super-forwarding method with a DIFFERENT name cannot stand in for virtual dispatch.
            if(left){m=method(c,0x1041,"dispatch","(I)I");var(m,ALOAD,0);var(m,ILOAD,1);call(m,INVOKESPECIAL,"Parent","value","(I)I");ins(m,IRETURN);}
            else {m=method(p,ACC_PUBLIC,"dispatch","(I)I");var(m,ALOAD,0);var(m,ILOAD,1);call(m,INVOKEVIRTUAL,"Parent","value","(I)I");ins(m,IRETURN);}
            m=method(c,ACC_PUBLIC|ACC_STATIC,"m","(I)I");m.instructions.add(new TypeInsnNode(NEW,"Child"));ins(m,DUP);call(m,INVOKESPECIAL,"Child","<init>","()V");var(m,ILOAD,0);call(m,INVOKEVIRTUAL,"Fixture","dispatch","(I)I");ins(m,IRETURN);
            jars.add(save("bridge-override-"+left,List.of(c,p,child)));
        }
        cli("bridge-override",jars.get(0),jars.get(1),"DIFF");Object x=execute(jars.get(0),2),y=execute(jars.get(1),2);
        ok("JVM bridge-override left="+x+" right="+y,!Objects.equals(x,y));
    }
    static void bounds()throws Exception {
        ClassNode a=loop(true,0),b=loop(false,0);
        Path l=save("bounds-l",List.of(a)),r=save("bounds-r",List.of(b));JarIndex lj=JarIndex.load(l),rj=JarIndex.load(r);
        MethodNode lm=lj.readTree("Fixture").methods.stream().filter(m->m.name.equals("m")).findFirst().orElseThrow(),
                rm=rj.readTree("Fixture").methods.stream().filter(m->m.name.equals("m")).findFirst().orElseThrow();
        ok("paths rejects loop",!PathComparison.compare(lm,rm,lj,rj).same());
        ok("CFG accepts loop",CfgComparison.compare(lm,rm,lj,rj).same());
    }
}
