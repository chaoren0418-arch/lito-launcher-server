package local.lito381.semanticdiff;

import java.net.URLClassLoader;
import java.nio.file.*;
import java.util.*;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
import static local.lito381.semanticdiff.FourthSelfTest.*;

/** #259: the comparator and isolated ClassLoaders consume identical verified bytes. */
public final class TwelfthSelfTest implements Opcodes {
    private static int checks;
    private static void ok(String name, boolean good) {
        if (!good) throw new AssertionError(name);
        checks++;
        System.out.println("PASS " + name);
    }
    public static void main(String[] args) throws Exception {
        work=Path.of(args[0]); bin=Path.of(args[1]);
        Files.createDirectories(work); Files.createDirectories(bin);
        pair("four-stores",shuffle(false,false,false),shuffle(true,false,false),true,true);
        pair("four-stores-cross-block",shuffle(false,false,true),shuffle(true,false,true),true,true);
        pair("evaluation-order",shuffle(false,false,false),shuffle(true,true,false),false,true);
        pair("repeated-read",repeated(false,false),repeated(true,false),true,true);
        pair("rewritten-between-reads",repeated(false,false),repeated(true,true),false,true);
        pair("dup-local",dup(false,false),dup(true,false),true,true);
        pair("dup-different-expression",dup(false,false),dup(true,true),false,true);
        pair("receiver-constant-prefix",prefix(false,false),prefix(true,false),true,true);
        pair("prefix-different-constant",prefix(false,false),prefix(true,true),false,true);
        pair("null-cast-return",nullCast(false),nullCast(true),true,true);
        pair("reused-local-type",reusedType(false),reusedType(true),true,true);
        pair("handler-observed-local",handlerObserved(false),handlerObserved(true),false,true);
        pair("wide-shuffle",wideShuffle(false,false),wideShuffle(true,false),true,true);
        pair("wide-different-value",wideShuffle(false,false),wideShuffle(true,true),false,true);
        pair("dead-duplicate-save",deadSave(false,false),deadSave(true,false),true,true);
        pair("dead-save-producer-effect",deadSave(false,false),deadSave(true,true),false,true);
        for (int opcode : new int[]{DUP_X1,DUP_X2,DUP2,DUP2_X1,DUP2_X2,SWAP})
            pair("shuffle-"+opcode,stackShuffle(opcode,false),stackShuffle(opcode,true),true,true);
        pair("switch-content",switches(false,0),switches(true,0),true,false);
        pair("switch-case-value",switches(false,0),switches(true,1),false,false);
        pair("switch-constant-set",switches(false,0),switches(true,2),false,false);
        guards();
        System.out.println("TWELFTH SELFTEST ALL PASS checks="+checks);
    }
    private static void pair(String name,ClassNode a,ClassNode b,boolean same,boolean paths) throws Exception {
        Path left=jar(name+"-left",List.of(framed(a))),right=jar(name+"-right",List.of(framed(b)));
        boolean different=false;
        for(int input:new int[]{0,1,2}) {
            Object x=execute(left,a.name,input),y=execute(right,b.name,input);
            System.out.println("JVM "+name+" input="+input+" left="+x+" right="+y);
            different |= !x.equals(y);
            if(same)ok(name+" equal execution "+input,x.equals(y));
        }
        if(!same)ok(name+" behavior differs",different);
        for(boolean reverse:new boolean[]{false,true}) {
            Path l=reverse?right:left,r=reverse?left:right;
            Path json=work.resolve(name+reverse+".json");
            ok(name+" CLI exit "+reverse,SemanticDiff.run(new String[]{"--left",l.toString(),"--right",r.toString(),
                    "--json",json.toString(),"--md",work.resolve(name+reverse+".md").toString()})==0);
            ok(name+" CLI result "+reverse,Files.readString(json).contains("\"DIFF\": "+(same?0:1)));
            JarIndex lj=JarIndex.load(l),rj=JarIndex.load(r);
            MethodNode lm=lj.readTree(a.name).methods.stream().filter(m -> m.name.equals("m")).findFirst().orElseThrow();
            MethodNode rm=rj.readTree(b.name).methods.stream().filter(m -> m.name.equals("m")).findFirst().orElseThrow();
            if(paths) {
                ok(name+" paths "+reverse,PathComparison.compare(lm,rm,lj,rj).same()==same);
                ok(name+" CFG "+reverse,CfgComparison.compare(lm,rm,lj,rj).same()==same);
                if (name.equals("four-stores-cross-block") || name.equals("receiver-constant-prefix")) {
                    System.setProperty("semanticdiff.disable","value-flow");
                    try { ok(name+" CFG ablation "+reverse,!CfgComparison.compare(lm,rm,lj,rj).same()); }
                    finally { System.clearProperty("semanticdiff.disable"); }
                }
            }
            if(name.startsWith("four-stores")) {
                Path approved=work.resolve("effects.txt");
                Files.writeString(approved,"Fixture#m(I)I # approved: https://github.com/chaoren0418-arch/lito-launcher-server/issues/259\n");
                ok(name+" effects exit "+reverse,SemanticDiff.run(new String[]{"--left",l.toString(),"--right",r.toString(),
                        "--effects-methods",approved.toString(),"--json",json.toString(),"--md",work.resolve(name+reverse+"-effects.md").toString()})==0);
                ok(name+" effects result "+reverse,Files.readString(json).contains("\"DIFF\": 0"));
            }
        }
    }
    private static ClassNode framed(ClassNode c) {
        ClassWriter writer=new ClassWriter(ClassWriter.COMPUTE_FRAMES|ClassWriter.COMPUTE_MAXS);
        c.accept(writer); ClassNode result=new ClassNode(); new ClassReader(writer.toByteArray()).accept(result,0); return result;
    }
    private static Object execute(Path jar,String owner,int input) throws Exception {
        try(URLClassLoader loader=new URLClassLoader(new java.net.URL[]{jar.toUri().toURL()},ClassLoader.getPlatformClassLoader())) {
            Object result=loader.loadClass(owner.replace('/','.')).getMethod("m",int.class).invoke(null,input);
            return String.valueOf(result);
        }
    }
    private static void call(MethodNode m,String owner,String name,String desc) {
        m.instructions.add(new MethodInsnNode(INVOKESTATIC,owner,name,desc,false));
    }
    private static void field(MethodNode m,int op,String name,String desc) {
        m.instructions.add(new FieldInsnNode(op,"Fixture",name,desc));
    }
    private static ClassNode shuffle(boolean delayed,boolean reverse,boolean branch) {
        ClassNode c=clazz(); c.fields.add(new FieldNode(ACC_PUBLIC|ACC_STATIC,"trace","I",null,null));
        MethodNode m=method(c,ACC_PUBLIC|ACC_STATIC,"m","(I)I");
        for(int index=1;index<=4;index++) {
            int producer=reverse&&index<=2?3-index:index;
            call(m,"Fixture","produce"+producer,"()Ljava/lang/Integer;");
            if(!delayed)var(m,ASTORE,index);
        }
        if(delayed)for(int index=4;index>=1;index--)var(m,ASTORE,index);
        LabelNode target=new LabelNode();
        if(branch){var(m,ILOAD,0);m.instructions.add(new JumpInsnNode(IFEQ,target));ins(m,NOP);m.instructions.add(target);}
        for(int index=1;index<=4;index++)var(m,ALOAD,index);
        call(m,"Fixture","sum","(Ljava/lang/Integer;Ljava/lang/Integer;Ljava/lang/Integer;Ljava/lang/Integer;)I");
        field(m,GETSTATIC,"trace","I");ins(m,IADD,IRETURN);
        for(int index=1;index<=4;index++) {
            MethodNode producer=method(c,ACC_PUBLIC|ACC_STATIC,"produce"+index,"()Ljava/lang/Integer;");
            field(producer,GETSTATIC,"trace","I");producer.instructions.add(new IntInsnNode(BIPUSH,10));
            ins(producer,IMUL,ICONST_0+index,IADD);field(producer,PUTSTATIC,"trace","I");
            ins(producer,ICONST_0+index);call(producer,"java/lang/Integer","valueOf","(I)Ljava/lang/Integer;");ins(producer,ARETURN);
        }
        MethodNode sum=method(c,ACC_PUBLIC|ACC_STATIC,"sum","(Ljava/lang/Integer;Ljava/lang/Integer;Ljava/lang/Integer;Ljava/lang/Integer;)I");
        for(int i=0;i<4;i++) {
            var(sum,ALOAD,i);sum.instructions.add(new MethodInsnNode(INVOKEVIRTUAL,"java/lang/Integer","intValue","()I",false));
            if(i>0)ins(sum,IADD);
        }
        ins(sum,IRETURN);return c;
    }
    private static ClassNode repeated(boolean temporary,boolean rewrite) {
        ClassNode c=clazz();MethodNode m=method(c,ACC_PUBLIC|ACC_STATIC,"m","(I)I");
        ins(m,ICONST_1);
        if(temporary) {
            var(m,ISTORE,1);var(m,ILOAD,1);
            if(rewrite){ins(m,ICONST_2);var(m,ISTORE,1);}
            var(m,ILOAD,1);
        } else ins(m,DUP);
        ins(m,IADD,IRETURN);return c;
    }
    private static ClassNode dup(boolean load,boolean different) {
        ClassNode c=clazz();MethodNode m=method(c,ACC_PUBLIC|ACC_STATIC,"m","(I)I");
        var(m,ILOAD,0);
        if(load){var(m,ILOAD,0);if(different)ins(m,ICONST_1,IADD);}else ins(m,DUP);
        ins(m,IADD,IRETURN);return c;
    }
    private static ClassNode prefix(boolean early,boolean different) {
        ClassNode c=clazz();c.fields.add(new FieldNode(ACC_PUBLIC,"value","I",null,null));
        MethodNode ctor=method(c,ACC_PUBLIC,"<init>","()V");var(ctor,ALOAD,0);
        ctor.instructions.add(new MethodInsnNode(INVOKESPECIAL,"java/lang/Object","<init>","()V",false));ins(ctor,RETURN);
        MethodNode m=method(c,ACC_PUBLIC|ACC_STATIC,"m","(I)I");
        m.instructions.add(new TypeInsnNode(NEW,"Fixture"));ins(m,DUP);
        m.instructions.add(new MethodInsnNode(INVOKESPECIAL,"Fixture","<init>","()V",false));var(m,ASTORE,1);
        if(early){var(m,ALOAD,1);ins(m,different?ICONST_1:ICONST_0);}
        var(m,ALOAD,1);call(m,"Fixture","release","(LFixture;)V");
        if(!early){var(m,ALOAD,1);ins(m,ICONST_0);}
        field(m,PUTFIELD,"value","I");var(m,ALOAD,1);field(m,GETFIELD,"value","I");ins(m,IRETURN);
        MethodNode release=method(c,ACC_PUBLIC|ACC_STATIC,"release","(LFixture;)V");
        var(release,ALOAD,0);ins(release,ICONST_2);field(release,PUTFIELD,"value","I");ins(release,RETURN);return c;
    }
    private static ClassNode nullCast(boolean stack) {
        ClassNode c=clazz();MethodNode m=method(c,ACC_PUBLIC|ACC_STATIC,"m","(I)Ljava/lang/String;");
        var(m,ILOAD,0);call(m,"Fixture","value","(I)Ljava/lang/Object;");
        if(stack)ins(m,DUP);else{var(m,ASTORE,1);var(m,ALOAD,1);}
        LabelNode nil=new LabelNode();m.instructions.add(new JumpInsnNode(IFNULL,nil));
        if(!stack)var(m,ALOAD,1);
        m.instructions.add(new TypeInsnNode(CHECKCAST,"java/lang/String"));ins(m,ARETURN);m.instructions.add(nil);
        if(stack)ins(m,POP);
        m.instructions.add(new LdcInsnNode("empty"));ins(m,ARETURN);
        MethodNode value=method(c,ACC_PUBLIC|ACC_STATIC,"value","(I)Ljava/lang/Object;");
        LabelNode text=new LabelNode();var(value,ILOAD,0);value.instructions.add(new JumpInsnNode(IFNE,text));
        ins(value,ACONST_NULL,ARETURN);value.instructions.add(text);value.instructions.add(new LdcInsnNode("value"));ins(value,ARETURN);return c;
    }
    private static ClassNode switches(boolean reverse,int change) {
        ClassNode c=clazz();c.name="Fixture$1";
        String[] names=reverse?new String[]{"$SwitchMap$19","$SwitchMap$2"}:new String[]{"$SwitchMap$1","$SwitchMap$9"};
        String[] enums={"java/lang/Thread$State","java/time/DayOfWeek"};
        for(int index:reverse?new int[]{1,0}:new int[]{0,1})c.fields.add(new FieldNode(ACC_PUBLIC|ACC_STATIC|ACC_FINAL,names[index],"[I",null,null));
        MethodNode init=method(c,ACC_STATIC,"<clinit>","()V");
        for(int index:reverse?new int[]{1,0}:new int[]{0,1}) {
            call(init,enums[index],"values","()[L"+enums[index]+";");ins(init,ARRAYLENGTH);
            init.instructions.add(new IntInsnNode(NEWARRAY,T_INT));init.instructions.add(new FieldInsnNode(PUTSTATIC,c.name,names[index],"[I"));
            LabelNode start=new LabelNode(),end=new LabelNode(),handler=new LabelNode(),next=new LabelNode();
            init.instructions.add(start);init.instructions.add(new FieldInsnNode(GETSTATIC,c.name,names[index],"[I"));
            String constant=index==0?(change==2?"RUNNABLE":"NEW"):"MONDAY";
            init.instructions.add(new FieldInsnNode(GETSTATIC,enums[index],constant,"L"+enums[index]+";"));
            init.instructions.add(new MethodInsnNode(INVOKEVIRTUAL,enums[index],"ordinal","()I",false));
            ins(init,index==0&&change==1?ICONST_3:ICONST_1+index,IASTORE);
            init.instructions.add(end);init.instructions.add(new JumpInsnNode(GOTO,next));
            init.instructions.add(handler);var(init,ASTORE,0);init.instructions.add(next);
            init.tryCatchBlocks.add(new TryCatchBlockNode(start,end,handler,"java/lang/NoSuchFieldError"));
        }
        ins(init,RETURN);MethodNode m=method(c,ACC_PUBLIC|ACC_STATIC,"m","(I)I");
        for(int i=0;i<2;i++) {
            m.instructions.add(new FieldInsnNode(GETSTATIC,c.name,names[i],"[I"));ins(m,ICONST_0,IALOAD);
            if(i==0)ins(m,ICONST_5,IMUL);
        }
        ins(m,IADD,IRETURN);return c;
    }
    private static ClassNode reusedType(boolean local) {
        ClassNode c=clazz();MethodNode m=method(c,ACC_PUBLIC|ACC_STATIC,"m","(I)I");
        m.instructions.add(new LdcInsnNode("7"));
        if(local){var(m,ASTORE,1);var(m,ALOAD,1);}
        call(m,"java/lang/Integer","parseInt","(Ljava/lang/String;)I");
        if(local){var(m,ISTORE,1);var(m,ILOAD,1);}
        ins(m,IRETURN);return c;
    }
    private static ClassNode handlerObserved(boolean changed) {
        ClassNode c=clazz();MethodNode m=method(c,ACC_PUBLIC|ACC_STATIC,"m","(I)I");
        ins(m,changed?ICONST_2:ICONST_1);var(m,ISTORE,1);
        LabelNode start=new LabelNode(),end=new LabelNode(),handler=new LabelNode();
        m.instructions.add(start);call(m,"Fixture","fail","()V");m.instructions.add(end);
        var(m,ILOAD,1);ins(m,IRETURN);m.instructions.add(handler);var(m,ASTORE,2);var(m,ILOAD,1);ins(m,IRETURN);
        m.tryCatchBlocks.add(new TryCatchBlockNode(start,end,handler,"java/lang/RuntimeException"));
        MethodNode fail=method(c,ACC_PUBLIC|ACC_STATIC,"fail","()V");
        fail.instructions.add(new TypeInsnNode(NEW,"java/lang/RuntimeException"));ins(fail,DUP);
        fail.instructions.add(new MethodInsnNode(INVOKESPECIAL,"java/lang/RuntimeException","<init>","()V",false));
        ins(fail,ATHROW);return c;
    }
    private static ClassNode wideShuffle(boolean shuffle,boolean different) {
        ClassNode c=clazz();MethodNode m=method(c,ACC_PUBLIC|ACC_STATIC,"m","(I)I");
        if(shuffle)ins(m,LCONST_0,different?LCONST_0:LCONST_1,DUP2_X2);
        else ins(m,LCONST_1,LCONST_0,LCONST_1);
        ins(m,LADD,LADD,L2I,IRETURN);return c;
    }
    private static ClassNode deadSave(boolean save,boolean effect) {
        ClassNode c=clazz();MethodNode m=method(c,ACC_PUBLIC|ACC_STATIC,"m","(I)I");
        c.fields.add(new FieldNode(ACC_PUBLIC|ACC_STATIC,"trace","I",null,null));
        var(m,ILOAD,0);
        if(save){ins(m,DUP);var(m,ISTORE,1);}
        if(effect){call(m,"Fixture","tick","()I");var(m,ISTORE,2);}
        field(m,GETSTATIC,"trace","I");ins(m,IADD,IRETURN);
        MethodNode tick=method(c,ACC_PUBLIC|ACC_STATIC,"tick","()I");
        field(tick,GETSTATIC,"trace","I");ins(tick,ICONST_1,IADD,DUP);field(tick,PUTSTATIC,"trace","I");ins(tick,IRETURN);
        return c;
    }
    private static ClassNode stackShuffle(int opcode,boolean shuffle) {
        int[] input,output;
        switch(opcode) {
            case DUP_X1 -> { input=new int[]{1,2}; output=new int[]{2,1,2}; }
            case DUP_X2 -> { input=new int[]{1,2,3}; output=new int[]{3,1,2,3}; }
            case DUP2 -> { input=new int[]{1,2}; output=new int[]{1,2,1,2}; }
            case DUP2_X1 -> { input=new int[]{1,2,3}; output=new int[]{2,3,1,2,3}; }
            case DUP2_X2 -> { input=new int[]{1,2,3,4}; output=new int[]{3,4,1,2,3,4}; }
            case SWAP -> { input=new int[]{1,2}; output=new int[]{2,1}; }
            default -> throw new IllegalArgumentException();
        }
        ClassNode c=clazz();MethodNode m=method(c,ACC_PUBLIC|ACC_STATIC,"m","(I)I");
        for(int value:shuffle?input:output)ins(m,ICONST_0+value);
        if(shuffle)ins(m,opcode);
        for(int i=1;i<output.length;i++)ins(m,ISUB);
        ins(m,IRETURN);return c;
    }
    private static void guards() throws Exception {
        ClassNode c=shuffle(false,false,false);MethodNode m=c.methods.get(0);
        LocalWebs.normalize(c.name,m);
        ok("all four temporaries eligible",TemporaryValues.eligible(m).size()==4);
        m=handlerObserved(false).methods.get(0);
        ok("handler observation blocks temporary",!TemporaryValues.eligible(m).contains(1));
        ClassNode invalid=switches(false,0);
        invalid.methods.get(0).instructions.insert(new MethodInsnNode(INVOKESTATIC,"java/lang/System","gc","()V",false));
        ok("extra initializer call refuses content mapping",SwitchMaps.inspect(invalid)==null);
        invalid=switches(false,0);invalid.methods.get(0).tryCatchBlocks.get(0).type="java/lang/Throwable";
        ok("different handler refuses content mapping",SwitchMaps.inspect(invalid)==null);
    }
}
