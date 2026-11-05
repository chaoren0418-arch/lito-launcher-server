package local.lito381.semanticdiff;

import java.nio.file.*;
import java.util.*;
import java.util.zip.*;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;

/** Issue 166 positive/adversarial fixtures. Each assertion uses the actual class comparer. */
public final class FourthSelfTest implements Opcodes {
    static Path work, bin; static int checks;
    public static void main(String[] args) throws Exception {
        work=Path.of(args[0]); bin=Path.of(args[1]); Files.createDirectories(work);Files.createDirectories(bin);
        for (int op : new int[]{INVOKESTATIC,INVOKEVIRTUAL,INVOKEINTERFACE})
            check("forward-"+op,forward(true,op,0),forward(false,op,0),true);
        check("forward-extra",forward(true,INVOKESTATIC,1),forward(false,INVOKESTATIC,0),false);
        check("forward-order",forward(true,INVOKESTATIC,2),forward(false,INVOKESTATIC,0),false);
        check("forward-reference",forward(true,INVOKESTATIC,3),forward(false,INVOKESTATIC,3),false);
        check("forward-ldc",forward(true,INVOKESTATIC,4),forward(false,INVOKESTATIC,4),false);
        check("adjacent",caught(true,false,false),caught(false,false,false),true);
        ClassNode reversed=caught(true,false,false);Collections.reverse(reversed.methods.get(0).tryCatchBlocks);
        check("adjacent-reverse-table",reversed,caught(false,false,false),true);
        check("unprotected-call",caught(true,true,false),caught(false,true,false),false);
        check("handler-priority",caught(true,false,true),caught(false,false,true),false);
        check("single-temp",temp(true,1,false,false),temp(false,1,false,false),true);
        check("multi-temp",temp(true,3,false,false),temp(false,3,false,false),true);
        check("copies-mismatch",temp(true,2,false,false),temp(false,3,false,false),false);
        check("call-order",temp(true,2,true,false),temp(false,2,false,false),false);
        check("overwrite",temp(true,2,false,true),temp(false,2,false,false),false);
        check("branch-temp",branchTemp(true),branchTemp(false),true);
        check("wide-temp",wideTemp(true),wideTemp(false),true);
        check("wrong-use-position",wrongPosition(true),wrongPosition(false),false);
        check("monitor-order",monitor(true),monitor(false),false);
        check("captures",captures(false,0),captures(true,0),true);
        check("captures-call",captures(false,1),captures(true,1),false);
        check("captures-field",captures(false,2),captures(true,2),false);
        check("captures-body-mismatch",captures(false,0),captures(true,3),false);
        System.setProperty("semanticdiff.disable","forward");
        check("captures-without-forwarding",captures(false,0),captures(true,0),true);
        check("nested-captures",nestedCaptures(false),nestedCaptures(true),true);
        System.clearProperty("semanticdiff.disable");
        check("copies-per-origin",copyOrigins(true),copyOrigins(false),false);
        check("web-diamond",diamond(true,false),diamond(false,false),true);
        check("web-diamond-overwrite",diamond(false,true),diamond(false,false),false);
        check("web-loop",loop(7),loop(12),true);
        check("web-reuse",web(true,false),web(false,false),true);
        check("web-overlap",web(true,true),web(false,true),false);
        check("web-handler",handlerWeb(true),handlerWeb(false),true);
        fieldChecks();
        crossReference();
        // Execute valid fixtures in isolated loaders. No server or application classes are loaded.
        execute(branchTemp(true),branchTemp(false),"branch-temp");
        execute(wideTemp(true),wideTemp(false),"wide-temp");
        System.out.println("FOURTH SELFTEST ALL PASS checks="+checks);
    }
    static ClassNode clazz(){ClassNode c=new ClassNode();c.version=V17;c.access=ACC_PUBLIC;c.name="Fixture";c.superName="java/lang/Object";return c;}
    static MethodNode method(ClassNode c,int access,String name,String desc){MethodNode m=new MethodNode(access,name,desc,null,null);m.maxStack=30;m.maxLocals=100;c.methods.add(m);return m;}
    static void ins(MethodNode m,int...ops){for(int op:ops)m.instructions.add(new InsnNode(op));}
    static void var(MethodNode m,int op,int slot){m.instructions.add(new VarInsnNode(op,slot));}
    static void call(MethodNode m,String name,String desc){m.instructions.add(new MethodInsnNode(INVOKESTATIC,"Sink",name,desc,false));}
    static Handle bsm(){return new Handle(H_INVOKESTATIC,"java/lang/invoke/LambdaMetafactory","metafactory","(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite;",false);}
    static InvokeDynamicInsnNode indy(String desc,Handle impl){return new InvokeDynamicInsnNode("get",desc,bsm(),Type.getMethodType("()I"),impl,Type.getMethodType("()I"));}
    static ClassNode forward(boolean wrapper,int op,int bad){
        ClassNode c=clazz();String desc=op==INVOKESTATIC?"(II)I":"(LTarget;I)I";
        MethodNode m=method(c,ACC_PUBLIC|ACC_STATIC,"m",desc);
        var(m,op==INVOKESTATIC?ILOAD:ALOAD,0);var(m,ILOAD,1);
        Handle target=new Handle(op==INVOKESTATIC?H_INVOKESTATIC:op==INVOKEVIRTUAL?H_INVOKEVIRTUAL:H_INVOKEINTERFACE,"Target","x",op==INVOKESTATIC?"(II)I":"(I)I",op==INVOKEINTERFACE);
        m.instructions.add(indy(desc.replace(")I",")Ljava/util/function/IntSupplier;"),wrapper?new Handle(H_INVOKESTATIC,c.name,"lambda$m$0",desc,false):target));ins(m,POP,ICONST_0,IRETURN);
        if(wrapper){MethodNode l=method(c,ACC_PRIVATE|ACC_STATIC|ACC_SYNTHETIC,"lambda$m$0",desc);
            var(l,op==INVOKESTATIC?ILOAD:ALOAD,bad==2?1:0);var(l,ILOAD,bad==2?0:1);
            if(bad==1)ins(l,NOP);l.instructions.add(new MethodInsnNode(op,target.getOwner(),target.getName(),target.getDesc(),target.isInterface()));ins(l,IRETURN);}
        if(bad==3){MethodNode x=method(c,ACC_STATIC,"other","()V");ins(x,ICONST_1,ICONST_2);x.instructions.add(new MethodInsnNode(INVOKESTATIC,c.name,"lambda$m$0",desc,false));ins(x,POP,RETURN);}
        if(bad==4){MethodNode x=method(c,ACC_STATIC,"other","()V");x.instructions.add(new LdcInsnNode(new Handle(H_INVOKESTATIC,c.name,"lambda$m$0",desc,false)));ins(x,POP,RETURN);}
        return c;
    }
    static ClassNode caught(boolean split,boolean gap,boolean priority){
        ClassNode c=clazz();MethodNode m=method(c,ACC_STATIC,"m","()V");LabelNode a=new LabelNode(),b=new LabelNode(),d=new LabelNode(),e=new LabelNode(),h=new LabelNode(),h2=new LabelNode();
        m.instructions.add(a);call(m,"a","()V");m.instructions.add(b);if(gap)call(m,"gap","()V");m.instructions.add(d);call(m,"b","()V");m.instructions.add(e);ins(m,RETURN);m.instructions.add(h);ins(m,ATHROW);m.instructions.add(h2);ins(m,POP,RETURN);
        m.tryCatchBlocks.add(new TryCatchBlockNode(a,split?b:e,h,"java/lang/Exception"));
        if(priority)m.tryCatchBlocks.add(new TryCatchBlockNode(d,e,h2,"java/lang/Throwable"));
        if(split)m.tryCatchBlocks.add(new TryCatchBlockNode(d,e,h,"java/lang/Exception"));return c;
    }
    static ClassNode temp(boolean local,int reads,boolean order,boolean overwrite){
        ClassNode c=clazz();MethodNode m=method(c,ACC_STATIC,"m","()V");call(m,"value","()Ljava/lang/Object;");
        if(local)var(m,ASTORE,5);else for(int i=1;i<reads;i++)ins(m,DUP);
        if(order)call(m,"second","()V");call(m,"first","()V");if(!order)call(m,"second","()V");
        for(int i=0;i<reads;i++){if(local)var(m,ALOAD,5);call(m,"consume","(Ljava/lang/Object;)V");if(overwrite&&i==0){ins(m,ACONST_NULL);var(m,ASTORE,5);}}
        ins(m,RETURN);return c;
    }
    static ClassNode branchTemp(boolean local){
        ClassNode c=clazz();MethodNode m=method(c,ACC_PUBLIC|ACC_STATIC,"m","(Ljava/lang/Object;)Ljava/lang/Object;");LabelNode no=new LabelNode();
        var(m,ALOAD,0);if(local){var(m,ASTORE,1);var(m,ALOAD,1);}else ins(m,DUP);
        m.instructions.add(new JumpInsnNode(IFNULL,no));if(local)var(m,ALOAD,1);ins(m,ARETURN);m.instructions.add(no);if(!local)ins(m,POP);ins(m,ACONST_NULL,ARETURN);return c;
    }
    static ClassNode wideTemp(boolean local){ClassNode c=clazz();MethodNode m=method(c,ACC_PUBLIC|ACC_STATIC,"m","(J)J");var(m,LLOAD,0);if(local){var(m,LSTORE,3);var(m,LLOAD,3);var(m,LLOAD,3);}else ins(m,DUP2);ins(m,LADD,LRETURN);return c;}
    static ClassNode wrongPosition(boolean local){ClassNode c=clazz();MethodNode m=method(c,ACC_STATIC,"m","()I");ins(m,ICONST_1);if(local)var(m,ISTORE,1);ins(m,ICONST_2);if(local)var(m,ILOAD,1);ins(m,ISUB,IRETURN);return c;}
    static ClassNode monitor(boolean local){ClassNode c=temp(local,1,false,false);MethodNode m=c.methods.get(0);AbstractInsnNode before=m.instructions.getFirst();m.instructions.insertBefore(before,new FieldInsnNode(GETSTATIC,"Sink","lock","Ljava/lang/Object;"));m.instructions.insertBefore(before,new InsnNode(local?MONITORENTER:MONITOREXIT));return c;}
    static ClassNode captures(boolean swapped,int bad){
        ClassNode c=clazz();String desc=swapped?"(Ljava/lang/String;Ljava/lang/Object;)I":"(Ljava/lang/Object;Ljava/lang/String;)I";
        MethodNode m=method(c,ACC_PUBLIC|ACC_STATIC,"m","(Ljava/lang/Object;Ljava/lang/String;)V");
        for(int j:swapped?new int[]{1,0}:new int[]{0,1}){
            if(bad==1)call(m,"get"+j,j==0?"()Ljava/lang/Object;":"()Ljava/lang/String;");
            else if(bad==2){var(m,ALOAD,0);m.instructions.add(new FieldInsnNode(GETFIELD,"Sink","value"+j,j==0?"Ljava/lang/Object;":"Ljava/lang/String;"));}
            else var(m,ALOAD,j);
        }
        m.instructions.add(indy(desc.replace(")I",")Ljava/util/function/IntSupplier;"),new Handle(H_INVOKESTATIC,c.name,"lambda$m$0",desc,false)));ins(m,POP,RETURN);
        MethodNode l=method(c,ACC_PRIVATE|ACC_STATIC|ACC_SYNTHETIC,"lambda$m$0",desc);
        var(l,ALOAD,swapped?1:0);var(l,ALOAD,swapped?0:1);call(l,bad==3?"different":"pair","(Ljava/lang/Object;Ljava/lang/String;)I");ins(l,IRETURN);return c;
    }
    static ClassNode nestedCaptures(boolean swapped){ClassNode c=captures(swapped,0);MethodNode outer=c.methods.get(1),inner=new MethodNode(outer.access,"lambda$inner$0",outer.desc,null,null);outer.accept(inner);inner.name="lambda$inner$0";c.methods.add(inner);outer.instructions.clear();
        var(outer,ALOAD,0);var(outer,ALOAD,1);outer.instructions.add(indy(outer.desc.replace(")I",")Ljava/util/function/IntSupplier;"),new Handle(H_INVOKESTATIC,c.name,inner.name,inner.desc,false)));ins(outer,POP,ICONST_0,IRETURN);return c;}
    static ClassNode web(boolean reuse,boolean overlap){ClassNode c=clazz();MethodNode m=method(c,ACC_STATIC,"m","()V");
        ins(m,ICONST_1);var(m,ISTORE,9);if(!overlap){var(m,ILOAD,9);call(m,"one","(I)V");}
        ins(m,ICONST_2);var(m,ISTORE,reuse?9:10);var(m,ILOAD,reuse?9:10);call(m,"two","(I)V");if(overlap){var(m,ILOAD,9);call(m,"one","(I)V");}ins(m,RETURN);return c;}
    static ClassNode copyOrigins(boolean first){ClassNode c=clazz();MethodNode m=method(c,ACC_STATIC,"m","()V");
        ins(m,ICONST_1);if(first)ins(m,DUP,POP);ins(m,POP,ICONST_2);if(!first)ins(m,DUP,POP);ins(m,POP,RETURN);return c;}
    static ClassNode diamond(boolean reuse,boolean wrong){ClassNode c=clazz();MethodNode m=method(c,ACC_STATIC,"m","(Z)I");LabelNode alternate=new LabelNode(),join=new LabelNode();
        ins(m,ICONST_1);var(m,ISTORE,7);var(m,ILOAD,0);m.instructions.add(new JumpInsnNode(IFEQ,alternate));ins(m,ICONST_2);var(m,ISTORE,reuse?7:9);m.instructions.add(new JumpInsnNode(GOTO,join));m.instructions.add(alternate);var(m,ILOAD,7);var(m,ISTORE,reuse?7:9);m.instructions.add(join);var(m,ILOAD,wrong?7:reuse?7:9);ins(m,IRETURN);return c;}
    static ClassNode loop(int slot){ClassNode c=clazz();MethodNode m=method(c,ACC_STATIC,"m","()I");LabelNode again=new LabelNode();ins(m,ICONST_3);var(m,ISTORE,slot);m.instructions.add(again);m.instructions.add(new IincInsnNode(slot,-1));var(m,ILOAD,slot);m.instructions.add(new JumpInsnNode(IFNE,again));var(m,ILOAD,slot);ins(m,IRETURN);return c;}
    static ClassNode handlerWeb(boolean reuse){ClassNode c=web(true,false);MethodNode m=c.methods.get(0);LabelNode a=new LabelNode(),b=new LabelNode(),h=new LabelNode();
        m.instructions.insert(a);m.instructions.insertBefore(m.instructions.getLast(),b);m.instructions.add(h);var(m,ASTORE,reuse?9:10);var(m,ALOAD,reuse?9:10);ins(m,ATHROW);m.tryCatchBlocks.add(new TryCatchBlockNode(a,b,h,"java/lang/Throwable"));return c;}
    static ClassNode owner(String name,String parent,boolean field){ClassNode c=clazz();c.name=name;c.superName=parent;if(field)c.fields.add(new FieldNode(ACC_PUBLIC,"value","I",null,null));return c;}
    static ClassNode field(String owner,int op){ClassNode c=clazz();MethodNode m=method(c,ACC_STATIC,"m","(LChild;)I");if(op==GETFIELD||op==PUTFIELD)var(m,ALOAD,0);if(op==PUTFIELD||op==PUTSTATIC)ins(m,ICONST_1);m.instructions.add(new FieldInsnNode(op,owner,"value","I"));if(op==PUTFIELD||op==PUTSTATIC)ins(m,ICONST_0);ins(m,IRETURN);return c;}
    static void fieldChecks()throws Exception{
        for(int op:new int[]{GETFIELD,PUTFIELD,GETSTATIC,PUTSTATIC}){
            ClassNode parent=owner("Parent","java/lang/Object",true),child=owner("Child","Parent",false);
            if(op==GETSTATIC||op==PUTSTATIC)parent.fields.get(0).access|=ACC_STATIC;
            Path dependency=jar("field-dependency-"+op,List.of(parent,child));
            JarIndex l=JarIndex.load(jar("field-l-"+op,List.of(field("Child",op)))),r=JarIndex.load(jar("field-r-"+op,List.of(field("Parent",op),parent,child)));
            l.addClasspath(dependency);assertion("field-owner-"+op,ClassComparer.compare(l.readTree("Fixture"),r.readTree("Fixture"),l,r)==null);
            ClassNode shadow=owner("Child","Parent",true);shadow.fields.get(0).access=parent.fields.get(0).access;
            l=JarIndex.load(jar("field-shadow-"+op,List.of(field("Child",op),shadow,parent)));
            assertion("field-shadow-"+op,ClassComparer.compare(l.readTree("Fixture"),r.readTree("Fixture"),l,r)!=null);
            l=JarIndex.load(jar("field-missing-"+op,List.of(field("Child",op))));
            assertion("field-unresolved-"+op,ClassComparer.compare(l.readTree("Fixture"),r.readTree("Fixture"),l,r)!=null);
            l=JarIndex.load(jar("field-unresolved-declaration-"+op,List.of(field("Parent",op))));
            r=JarIndex.load(jar("field-resolved-child-"+op,List.of(field("Child",op),parent,child)));
            assertion("field-unresolved-must-not-match-declaration-"+op,ClassComparer.compare(l.readTree("Fixture"),r.readTree("Fixture"),l,r)!=null);
        }
    }
    static void crossReference()throws Exception{
        ClassNode external=clazz();external.name="NestCaller";MethodNode m=method(external,ACC_STATIC,"m","()V");ins(m,ICONST_1,ICONST_2);m.instructions.add(new MethodInsnNode(INVOKESTATIC,"Fixture","lambda$m$0","(II)I",false));ins(m,POP,RETURN);
        JarIndex l=JarIndex.load(jar("external-l",List.of(forward(true,INVOKESTATIC,0),external))),r=JarIndex.load(jar("external-r",List.of(forward(false,INVOKESTATIC,0),external)));
        assertion("forward-external-reference",ClassComparer.compare(l.readTree("Fixture"),r.readTree("Fixture"),l,r)!=null);
    }
    static Path jar(String name,List<ClassNode> cs)throws Exception{Path p=bin.resolve(name+".jar");try(ZipOutputStream z=new ZipOutputStream(Files.newOutputStream(p))){for(ClassNode c:cs){ClassWriter w=new ClassWriter(0);c.accept(w);z.putNextEntry(new ZipEntry(c.name+".class"));z.write(w.toByteArray());z.closeEntry();}}return p;}
    static void check(String name,ClassNode a,ClassNode b,boolean same)throws Exception{
        JarIndex l=JarIndex.load(jar(name+"-l",List.of(a))),r=JarIndex.load(jar(name+"-r",List.of(b)));
        var diff=ClassComparer.compare(l.readTree("Fixture"),r.readTree("Fixture"),l,r);
        assertion(name,(diff==null)==same);Files.writeString(work.resolve(name+".txt"),diff==null?"SAME":diff.toString());
    }
    static void assertion(String name,boolean ok){if(!ok)throw new AssertionError(name);checks++;System.out.println("PASS "+name);}
    static void execute(ClassNode a,ClassNode b,String name)throws Exception{
        class Loader extends ClassLoader{Class<?> load(ClassNode c){ClassWriter w=new ClassWriter(ClassWriter.COMPUTE_FRAMES|ClassWriter.COMPUTE_MAXS);c.accept(w);byte[] bytes=w.toByteArray();return defineClass("Fixture",bytes,0,bytes.length);}}
        Class<?> l=new Loader().load(a),r=new Loader().load(b);
        if(name.equals("wide-temp"))for(long x:new long[]{0,1,-1,Long.MAX_VALUE})assertion("JVM "+name+" "+x,Objects.equals(l.getMethod("m",long.class).invoke(null,x),r.getMethod("m",long.class).invoke(null,x)));
        else for(Object x:new Object[]{null,"hello"})assertion("JVM "+name+" "+x,Objects.equals(l.getMethod("m",Object.class).invoke(null,x),r.getMethod("m",Object.class).invoke(null,x)));
    }
}
