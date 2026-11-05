package local.lito381.semanticdiff;

import java.lang.reflect.InvocationTargetException;
import java.nio.file.*;
import java.util.*;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
import static local.lito381.semanticdiff.FourthSelfTest.*;

/** Issue 186: CLI assertions and execution of the identical framed fixture bytes. */
public final class SeventhSelfTest implements Opcodes {
    static int checks;
    static void ok(String name, boolean value) { if (!value) throw new AssertionError(name); checks++; System.out.println("PASS " + name); }
    public static void main(String[] args) throws Exception {
        work=Path.of(args[0]); bin=Path.of(args[1]); Files.createDirectories(work); Files.createDirectories(bin);
        check("switch-synthetic", switches(true,false,false,false), switches(false,false,false,false), "SAME");
        check("switch-renamed", switches(true,false,false,false), switches(false,false,false,true), "SAME");
        check("switch-uses-swapped", switches(true,false,false,false), switches(false,true,false,false), "DIFF");
        check("switch-enum-ordinal", switches(true,false,false,false), switches(false,false,true,false), "DIFF");
        ClassNode a=switches(true,false,false,false), b=switches(false,false,false,true);
        a.fields.add(new FieldNode(ACC_STATIC,"$SwitchMap$ordinary","I",null,null));
        b.fields.add(new FieldNode(ACC_STATIC,"$SwitchMap$renamedOrdinary","I",null,null));
        check("switch-non-array",a,b,"DIFF");
        crossClassSwitch();
        check("own-private",privateCall(INVOKESPECIAL,true,false),privateCall(INVOKEVIRTUAL,true,false),"SAME");
        check("own-public",privateCall(INVOKESPECIAL,false,false),privateCall(INVOKEVIRTUAL,false,false),"DIFF");
        check("other-owner",privateCall(INVOKESPECIAL,true,true),privateCall(INVOKEVIRTUAL,true,true),"DIFF");
        nestmate();
        for (int mode : new int[]{0,1,2,3,4,5,6,7}) rethrow(mode);
        handlerGuards();
        System.out.println("SEVENTH SELFTEST ALL PASS checks="+checks);
    }
    static String cli(String name,Path l,Path r,String expected,String... flags) throws Exception {
        Path out=work.resolve(name+".json"); List<String> args=new ArrayList<>(List.of("--left",l.toString(),"--right",r.toString(),"--json",out.toString(),"--md",work.resolve(name+".md").toString()));
        args.addAll(List.of(flags)); ok(name+" CLI",SemanticDiff.run(args.toArray(String[]::new))==0);
        String json=Files.readString(out); ok(name+" "+expected,json.contains("\"status\": \""+expected+"\"")); return json;
    }
    static void check(String name,ClassNode a,ClassNode b,String expected) throws Exception {
        Path l=jar(name+"-l",List.of(a)),r=jar(name+"-r",List.of(b));
        cli(name,l,r,expected); cli(name+"-reverse",r,l,expected);
    }
    static void invoke(MethodNode m,int op,String owner,String name,String desc) { m.instructions.add(new MethodInsnNode(op,owner,name,desc,false)); }
    static void crossClassSwitch() throws Exception {
        List<Path> paths=new ArrayList<>();
        for(boolean left:new boolean[]{true,false}) {
            ClassNode holder=switches(left,false,false,!left),caller=clazz();caller.name="Caller";
            MethodNode m=holder.methods.remove(1);caller.methods.add(m);
            paths.add(jar("switch-caller-"+left,List.of(holder,caller)));
        }
        cli("switch-cross-class",paths.get(0),paths.get(1),"SAME");
        JarIndex l=JarIndex.load(paths.get(0)),r=JarIndex.load(paths.get(1));
        ok("cross-class GETSTATIC references",ClassComparer.compare(l.readTree("Caller"),r.readTree("Caller"),l,r)==null);
    }
    static void nestmate() throws Exception {
        List<Path> paths=new ArrayList<>();
        for(int opcode:new int[]{INVOKESPECIAL,INVOKEVIRTUAL}) {
            ClassNode c=privateCall(opcode,true,true);c.nestMembers=new ArrayList<>(List.of("Other"));
            ClassNode target=clazz();target.name="Other";target.nestHostClass="Fixture";
            MethodNode m=method(target,ACC_PRIVATE,"target","()I");ins(m,ICONST_1,IRETURN);
            paths.add(jar("nestmate-"+opcode,List.of(c,target)));
        }
        cli("private-nestmate",paths.get(0),paths.get(1),"DIFF");
        cli("private-nestmate-reverse",paths.get(1),paths.get(0),"DIFF");
    }
    static ClassNode privateCall(int opcode,boolean priv,boolean other) {
        ClassNode c=clazz(); MethodNode target=method(c,priv?ACC_PRIVATE:ACC_PUBLIC,"target","()I"); ins(target,ICONST_1,IRETURN);
        MethodNode m=method(c,ACC_PUBLIC,"m","()I"); var(m,ALOAD,0); invoke(m,opcode,other?"Other":"Fixture","target","()I"); ins(m,IRETURN); return c;
    }
    static ClassNode switches(boolean synthetic,boolean swapped,boolean ordinal,boolean renamed) {
        ClassNode c=clazz(); String[] fields=renamed?new String[]{"$SwitchMap$B","$SwitchMap$A"}:new String[]{"$SwitchMap$first","$SwitchMap$second"};
        for(String f:fields)c.fields.add(new FieldNode(ACC_STATIC|ACC_FINAL|(synthetic?ACC_SYNTHETIC:0),f,"[I",null,null));
        MethodNode init=method(c,ACC_STATIC,"<clinit>","()V");
        for(int i=0;i<2;i++) {
            ins(init,ICONST_2);init.instructions.add(new IntInsnNode(NEWARRAY,T_INT));
            init.instructions.add(new FieldInsnNode(PUTSTATIC,"Fixture",fields[i],"[I"));
            init.instructions.add(new FieldInsnNode(GETSTATIC,"Fixture",fields[i],"[I"));
            init.instructions.add(new FieldInsnNode(GETSTATIC,"java/lang/Thread$State",ordinal?"RUNNABLE":"NEW","Ljava/lang/Thread$State;"));
            invoke(init,INVOKEVIRTUAL,"java/lang/Thread$State","ordinal","()I");ins(init,ICONST_1+i,IASTORE);
        }
        ins(init,RETURN); MethodNode m=method(c,ACC_PUBLIC|ACC_STATIC,"m","()I");
        for(int i=0;i<2;i++){m.instructions.add(new FieldInsnNode(GETSTATIC,"Fixture",fields[swapped?1-i:i],"[I"));ins(m,ICONST_0,IALOAD);if(i==0)ins(m,ICONST_2,IMUL);}
        ins(m,IADD,IRETURN);return c;
    }
    // 0 pure; 1 wrapping; 2 helper log; 3 handler log; 4 right handler;
    // 5 safe cast; 6 unsafe cast; 7 unapproved pure helper.
    static void rethrow(int mode) throws Exception {
        String name="rethrow-"+mode;
        Path l=jar(name+"-l",List.of(framed(caught(true,mode)))),r=jar(name+"-r",List.of(framed(caught(false,mode))));
        Path approved=work.resolve("extra.txt");Files.writeString(approved,"Fixture#rethrow(Ljava/lang/Throwable;)V # approved: https://github.com/chaoren0418-arch/lito-launcher-server/issues/186\n");
        String expected=mode==0||mode==5?"SAME":"DIFF";
        String[] flags=mode==7?new String[0]:new String[]{"--extra-methods",approved.toString()};
        cli(name,l,r,expected,flags);
        for(Throwable input:new Throwable[]{new java.io.IOException("test"),new IllegalArgumentException("test")}) {
            String x=execute(l,input),y=execute(r,input);
            boolean same=mode==0||mode==5||mode==7||mode==6&&input instanceof RuntimeException;
            ok("JVM "+name+" "+input.getClass().getSimpleName()+" left="+x+" right="+y, x.equals(y)==same);
        }
        if(mode==0) {
            cli("rethrow-direction",r,l,"DIFF");
            System.setProperty("semanticdiff.disable","rethrow");
            try{cli("rethrow-ablation",l,r,"DIFF",flags);}finally{System.clearProperty("semanticdiff.disable");}
        }
    }
    static ClassNode caught(boolean left,int mode) {
        ClassNode c=clazz();c.fields.add(new FieldNode(ACC_PUBLIC|ACC_STATIC,"logs","I",null,null));
        MethodNode log=method(c,ACC_PRIVATE|ACC_STATIC,"log","()V");
        log.instructions.add(new FieldInsnNode(GETSTATIC,"Fixture","logs","I"));ins(log,ICONST_1,IADD);log.instructions.add(new FieldInsnNode(PUTSTATIC,"Fixture","logs","I"));ins(log,RETURN);
        if(left) {
            MethodNode helper=method(c,ACC_PRIVATE|ACC_STATIC,"rethrow","(Ljava/lang/Throwable;)V");
            if(mode==2)invoke(helper,INVOKESTATIC,"Fixture","log","()V");
            if(mode==1){helper.instructions.add(new TypeInsnNode(NEW,"java/lang/RuntimeException"));ins(helper,DUP);var(helper,ALOAD,0);invoke(helper,INVOKESPECIAL,"java/lang/RuntimeException","<init>","(Ljava/lang/Throwable;)V");}
            else {var(helper,ALOAD,0);if(mode==5||mode==6)helper.instructions.add(new TypeInsnNode(CHECKCAST,mode==5?"java/lang/Exception":"java/lang/RuntimeException"));}
            ins(helper,ATHROW);
        }
        MethodNode m=method(c,ACC_PUBLIC|ACC_STATIC,"m","(Ljava/lang/Throwable;)V");
        LabelNode start=new LabelNode(),end=new LabelNode(),handler=new LabelNode();m.instructions.add(start);var(m,ALOAD,0);ins(m,ATHROW);m.instructions.add(end);
        if(left||mode==4){m.instructions.add(handler);var(m,ASTORE,1);
            if(left){if(mode==3)invoke(m,INVOKESTATIC,"Fixture","log","()V");var(m,ALOAD,1);invoke(m,INVOKESTATIC,"Fixture","rethrow","(Ljava/lang/Throwable;)V");}
            ins(m,RETURN);m.tryCatchBlocks.add(new TryCatchBlockNode(start,end,handler,"java/lang/Exception"));}
        return c;
    }
    static void handlerGuards() throws Exception {
        for(String kind:List.of("synchronized","shared-entry","alias-entry","helper-extra-op","handler-extra-op","goto","athrow","fallthrough")) {
            ClassNode c=caught(true,0),other=caught(false,0);
            MethodNode m=c.methods.stream().filter(x->x.name.equals("m")).findFirst().orElseThrow();
            MethodNode h=c.methods.stream().filter(x->x.name.equals("rethrow")).findFirst().orElseThrow();
            boolean same=Set.of("goto","athrow","fallthrough").contains(kind);
            switch(kind) {
                case "synchronized" -> h.access|=ACC_SYNCHRONIZED;
                case "helper-extra-op" -> h.instructions.insert(new InsnNode(NOP));
                case "handler-extra-op" -> m.instructions.insert(m.tryCatchBlocks.get(0).handler,new InsnNode(NOP));
                case "shared-entry", "alias-entry" -> {
                    // A legal ordinary jump enters the catch with a Throwable on its stack.
                    LabelNode target=m.tryCatchBlocks.get(0).handler;
                    if(kind.equals("alias-entry")){LabelNode alias=new LabelNode();m.instructions.insertBefore(target,alias);target=alias;}
                    InsnList prefix=new InsnList();prefix.add(new VarInsnNode(ALOAD,0));prefix.add(new JumpInsnNode(GOTO,target));m.instructions.insert(prefix);
                }
                case "athrow" -> {
                    h.desc="(Ljava/lang/Throwable;)Ljava/lang/Throwable;";
                    for(AbstractInsnNode n:m.instructions)if(n instanceof MethodInsnNode call&&call.name.equals("rethrow"))call.desc=h.desc;
                    m.instructions.set(m.instructions.getLast(),new InsnNode(ATHROW));
                }
                case "goto", "fallthrough" -> {
                    // Real javac shape: a normal branch jumps over the catch, whose
                    // never-returning helper falls through to the shared continuation.
                    LabelNode join=new LabelNode();m.instructions.insertBefore(m.instructions.getLast(),join);
                    if(kind.equals("goto"))m.instructions.insertBefore(join,new JumpInsnNode(GOTO,join));
                    InsnList prefix=new InsnList();prefix.add(new VarInsnNode(ALOAD,0));prefix.add(new JumpInsnNode(IFNULL,join));m.instructions.insert(prefix);
                    MethodNode right=other.methods.stream().filter(x->x.name.equals("m")).findFirst().orElseThrow();LabelNode rjoin=new LabelNode();
                    InsnList rp=new InsnList();rp.add(new VarInsnNode(ALOAD,0));rp.add(new JumpInsnNode(IFNULL,rjoin));right.instructions.insert(rp);right.instructions.add(rjoin);ins(right,RETURN);
                }
            }
            Path ownApproval=work.resolve(kind+"-extra.txt");Files.writeString(ownApproval,"Fixture#rethrow"+h.desc+" # approved: https://github.com/chaoren0418-arch/lito-launcher-server/issues/186\n");
            Path l=jar(kind+"-l",List.of(c)),r=jar(kind+"-r",List.of(other));
            cli("handler-"+kind,l,r,same?"SAME":"DIFF","--extra-methods",ownApproval.toString());
            if(!same) {
                JarIndex lj=JarIndex.load(l),rj=JarIndex.load(r);lj.extraMethods=Map.of("Fixture",Set.of("rethrow"+h.desc));
                MethodNode lm=lj.readTree("Fixture").methods.stream().filter(x->x.name.equals("m")).findFirst().orElseThrow();
                MethodNode rm=rj.readTree("Fixture").methods.stream().filter(x->x.name.equals("m")).findFirst().orElseThrow();
                ok("handler guard leaves original "+kind,RethrowHandlers.normalize(lm,rm,lj,rj)==lm);
            }
        }
    }
    static String execute(Path jar,Throwable input) throws Exception {
        Class<?> c=SixthSelfTest.load(jar);String outcome="return";
        try{c.getMethod("m",Throwable.class).invoke(null,input);}catch(InvocationTargetException e){Throwable cause=e.getCause();outcome=cause.getClass().getName()+":identity="+(cause==input);}
        return outcome+":logs="+c.getField("logs").get(null);
    }
}
