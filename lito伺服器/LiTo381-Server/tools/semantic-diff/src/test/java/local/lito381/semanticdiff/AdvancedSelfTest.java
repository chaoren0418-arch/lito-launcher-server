package local.lito381.semanticdiff;

import java.nio.file.*;
import java.util.*;
import java.util.zip.*;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;

/** Issue #153: positive and adversarial CLI fixtures; no application classes are executed. */
public final class AdvancedSelfTest implements Opcodes {
    private static Path work, bin;
    private static int checks;
    private static final String APPROVAL=" # approved: https://example.test/issues/153\n";
    public static void main(String[] args) throws Exception {
        work=Path.of(args[0]); bin=Path.of(args[1]); Files.createDirectories(work); Files.createDirectories(bin);
        check("lambda-duplicate", lambdas("one",true,1),lambdas("two",false,1),"SAME");
        check("lambda-content", lambdas("one",false,2),lambdas("two",false,1),"DIFF");
        ClassNode swapped=lambdas("one",true,1); ((InvokeDynamicInsnNode)swapped.methods.get(0).instructions.getFirst()).bsmArgs[0]=new Handle(H_INVOKESTATIC,"Fixture","lambda$one$2","()I",false);
        MethodNode different=add(swapped,ACC_PRIVATE|ACC_STATIC|ACC_SYNTHETIC,"lambda$one$2","()I"); different.instructions.add(new InsnNode(ICONST_2)); different.instructions.add(new InsnNode(IRETURN));
        ClassNode other=lambdas("one",true,1); MethodNode different2=add(other,ACC_PRIVATE|ACC_STATIC|ACC_SYNTHETIC,"lambda$one$2","()I"); different.accept(different2);
        check("lambda-call-target",swapped,other,"DIFF");
        ClassNode noFlag=lambdas("source",false,1); noFlag.methods.get(1).access &= ~ACC_SYNTHETIC;
        check("lambda-missing-synthetic",noFlag,lambdas("asm",false,1),"SAME");
        check("private-handle",handle(H_INVOKEVIRTUAL,"Fixture",ACC_PRIVATE),handle(H_INVOKESPECIAL,"Fixture",ACC_PRIVATE),"SAME");
        check("external-handle",handle(H_INVOKEVIRTUAL,"Other",ACC_PRIVATE),handle(H_INVOKESPECIAL,"Other",ACC_PRIVATE),"DIFF");
        check("public-handle",handle(H_INVOKEVIRTUAL,"Fixture",ACC_PUBLIC),handle(H_INVOKESPECIAL,"Fixture",ACC_PUBLIC),"DIFF");
        check("object-super",ctor("java/lang/Object",true,false),ctor("java/lang/Object",false,false),"SAME");
        ClassNode calculation=ctor("java/lang/Object",true,false), calculation2=ctor("java/lang/Object",false,false);
        for(ClassNode c:List.of(calculation,calculation2)) for(AbstractInsnNode n:c.methods.get(0).instructions.toArray()) if(n instanceof FieldInsnNode) {
            c.methods.get(0).instructions.insertBefore(n,new InsnNode(ICONST_1)); c.methods.get(0).instructions.insertBefore(n,new InsnNode(IADD));
        }
        check("object-super-computation",calculation,calculation2,"SAME");
        ClassNode live=ctor("java/lang/Object",true,false),live2=ctor("java/lang/Object",false,false);
        live.methods.get(0).instructions.insert(new InsnNode(ICONST_2));
        for(AbstractInsnNode n:live2.methods.get(0).instructions.toArray()) if(n instanceof MethodInsnNode) live2.methods.get(0).instructions.insert(n,new InsnNode(ICONST_2));
        for(ClassNode c:List.of(live,live2)) c.methods.get(0).instructions.insertBefore(c.methods.get(0).instructions.getLast(),new InsnNode(POP));
        check("object-super-live-stack",live,live2,"SAME");
        check("other-super",ctor("Parent",true,false),ctor("Parent",false,false),"DIFF");
        ClassNode unsafe=ctor("java/lang/Object",true,false);
        unsafe.methods.get(0).instructions.insert(new MethodInsnNode(INVOKEVIRTUAL,"Fixture","bad","()V",false));
        unsafe.methods.get(0).instructions.insert(new VarInsnNode(ALOAD,0));
        rejected("unsafe-super",unsafe,ctor("java/lang/Object",false,false),null,null);
        check("switch-renamed",switches("one","two",false),switches("red","blue",false),"SAME");
        check("switch-swapped",switches("one","two",true),switches("red","blue",false),"DIFF");
        check("access-wide",plain(ACC_PUBLIC),plain(ACC_PROTECTED),"SAME","--access-widen","Fixture#m()V");
        check("access-unlisted",plain(ACC_PUBLIC),plain(ACC_PROTECTED),"DIFF");
        check("access-narrow",plain(ACC_PROTECTED),plain(ACC_PUBLIC),"DIFF","--access-widen","Fixture#m()V");
        check("access-final",plain(ACC_PUBLIC|ACC_FINAL),plain(ACC_PROTECTED),"DIFF","--access-widen","Fixture#m()V");
        check("extra-deviation",extra(true),extra(false),"DEVIATION","--deviations","Fixture#extra()V");
        check("extra-unlisted",extra(true),extra(false),"DIFF");
        check("extra-reverse",extra(false),extra(true),"DIFF");
        ClassNode changed=extra(true); changed.methods.get(1).instructions.insert(new InsnNode(NOP));
        check("deviation-shared",changed,extra(true),"DIFF","--deviations","Fixture#extra()V");
        check("trim-return",caught(true,false,false),caught(false,false,false),"SAME");
        check("trim-invoke",caught(true,true,false),caught(false,true,false),"DIFF");
        check("trim-monitor-return",caught(true,false,true),caught(false,false,true),"DIFF");
        ClassNode empty=caught(true,false,false); for(AbstractInsnNode n:empty.methods.get(0).instructions.toArray()) if(n instanceof MethodInsnNode) empty.methods.get(0).instructions.set(n,new InsnNode(NOP));
        ClassNode empty2=copy(empty); empty2.methods.get(0).tryCatchBlocks.clear();
        check("trim-empty",empty,empty2,"SAME");
        check("layout-reordered",layout(false,false),layout(true,false),"SAME_LAYOUT","--layout-methods","Fixture#m()V");
        check("layout-unlisted",layout(false,false),layout(true,false),"DIFF");
        check("layout-exception",layout(false,false),layout(true,true),"DIFF","--layout-methods","Fixture#m()V");
        ClassNode changedCall=layout(true,false); for(AbstractInsnNode n:changedCall.methods.get(0).instructions) if(n instanceof MethodInsnNode c && c.name.equals("cleanup")) c.name="different";
        check("layout-instruction",layout(false,false),changedCall,"DIFF","--layout-methods","Fixture#m()V");
        ClassNode unreach=layout(false,false), unreach2=layout(true,false);
        for(ClassNode c:List.of(unreach,unreach2)) { c.methods.get(0).instructions.add(new InsnNode(ICONST_1)); c.methods.get(0).instructions.add(new InsnNode(POP)); c.methods.get(0).instructions.add(new InsnNode(RETURN)); }
        check("layout-unreachable",unreach,unreach2,"SAME_LAYOUT","--layout-methods","Fixture#m()V");
        unreach2.methods.get(0).instructions.insertBefore(unreach2.methods.get(0).instructions.getLast(),new InsnNode(NOP));
        check("layout-unreachable-different",unreach,unreach2,"DIFF","--layout-methods","Fixture#m()V");
        ClassNode legacy=plain(ACC_PUBLIC); legacy.methods.get(0).instructions.insert(new VarInsnNode(RET,1));
        rejected("layout-legacy-ret",legacy,legacy,"--layout-methods","Fixture#m()V");
        check("dup-temp",temp(true,1),temp(false,1),"SAME_EFFECTS","--effects-methods","Fixture#m()V");
        check("dup-temp-twice",temp(true,2),temp(false,1),"DIFF","--effects-methods","Fixture#m()V");
        check("dup-temp-unapproved",temp(true,1),temp(false,1),"DIFF");
        check("ctor-order",ctor("Parent",false,false),ctor("Parent",true,false),"DEVIATION","--deviations","ctor-prologue-order Fixture#<init>(I)V");
        ctorLocalWrites();
        check("ctor-order-reverse",ctor("Parent",true,false),ctor("Parent",false,false),"DIFF","--deviations","ctor-prologue-order Fixture#<init>(I)V");
        ClassNode nameBefore=ctor("Parent",true,true), nameAfter=ctor("Parent",false,false);
        MethodNode nameMethod=nameAfter.methods.get(0); nameMethod.instructions.insertBefore(nameMethod.instructions.getLast(),new MethodInsnNode(INVOKESTATIC,"Names","name","()V",false));
        check("ctor-name-call-position",nameAfter,nameBefore,"DIFF","--deviations","ctor-prologue-order Fixture#<init>(I)V");
        check("ctor-order-call",ctor("Parent",false,true),ctor("Parent",true,false),"DIFF","--deviations","ctor-prologue-order Fixture#<init>(I)V");
        for(String flag:List.of("--access-widen","--layout-methods","--deviations")) {
            rejected("typo"+flag,plain(ACC_PUBLIC),plain(ACC_PUBLIC),flag,"Fixture#missing()V");
            rejected("scope"+flag,plain(ACC_PUBLIC),plain(ACC_PUBLIC),flag,"Other#m()V");
            rejected("wildcard"+flag,plain(ACC_PUBLIC),plain(ACC_PUBLIC),flag,"Fixture#*()V");
        }
        System.out.println("ADVANCED SELFTEST ALL PASS checks="+checks);
    }

    private static void ctorLocalWrites() throws Exception {
        checkCtorWrite("ctor-overwrite-int", "I", ILOAD, 1, ISTORE, 1, "DIFF");
        checkCtorWrite("ctor-overwrite-float", "F", FLOAD, 1, FSTORE, 1, "DIFF");
        checkCtorWrite("ctor-overwrite-reference", "Ljava/lang/Object;", ALOAD, 1, ASTORE, 1, "DIFF");
        checkCtorWrite("ctor-overwrite-long", "J", LLOAD, 1, LSTORE, 1, "DIFF");
        checkCtorWrite("ctor-overwrite-double", "D", DLOAD, 1, DSTORE, 1, "DIFF");
        checkCtorWrite("ctor-overwrite-iinc", "I", ILOAD, 1, IINC, 1, "DIFF");
        // Both halves of a category-2 load and store participate in the dependency.
        checkCtorWrite("ctor-wide-read-high", "J", LLOAD, 1, ISTORE, 2, "DIFF");
        checkCtorWrite("ctor-wide-read-low", "D", DLOAD, 1, ISTORE, 1, "DIFF");
        checkCtorWrite("ctor-wide-read-iinc", "J", LLOAD, 1, IINC, 2, "DIFF");
        checkCtorWrite("ctor-wide-store-low", "II", ILOAD, 1, LSTORE, 1, "DIFF");
        checkCtorWrite("ctor-wide-store-high", "II", ILOAD, 2, DSTORE, 1, "DIFF");
        checkCtorWrite("ctor-disjoint-store", "I", ILOAD, 1, ISTORE, 2, "DEVIATION");
        checkCtorWrite("ctor-disjoint-iinc", "II", ILOAD, 1, IINC, 2, "DEVIATION");
        checkCtorWrite("ctor-disjoint-wide-store", "I", ILOAD, 1, LSTORE, 2, "DEVIATION");
        checkCtorWrite("ctor-disjoint-wide-read", "J", LLOAD, 1, ISTORE, 3, "DEVIATION");
        checkCtorWrite("ctor-disjoint-wide-store-before", "III", ILOAD, 3, DSTORE, 1, "DEVIATION");
        // ASM represents WIDE operands through VarInsnNode/IincInsnNode as well.
        checkCtorWrite("ctor-high-slot-store", "I", ILOAD, 256, ISTORE, 256, "DIFF");
        checkCtorWrite("ctor-high-slot-iinc", "I", ILOAD, 256, IINC, 256, "DIFF");
    }

    private static void checkCtorWrite(String name, String parameters, int loadOpcode, int readSlot,
            int writeOpcode, int writeSlot, String status) throws Exception {
        ClassNode left = ctorWithWrite(false, parameters, loadOpcode, readSlot, writeOpcode, writeSlot);
        ClassNode right = ctorWithWrite(true, parameters, loadOpcode, readSlot, writeOpcode, writeSlot);
        check(name, left, right, status, "--deviations", "ctor-prologue-order Fixture#<init>(" + parameters + ")V");
    }

    private static ClassNode ctorWithWrite(boolean before, String parameters, int loadOpcode, int readSlot,
            int writeOpcode, int writeSlot) {
        ClassNode c = ctor("Parent", before, false);
        String fieldDescriptor = switch (loadOpcode) {
            case LLOAD -> "J";
            case FLOAD -> "F";
            case DLOAD -> "D";
            case ALOAD -> "Ljava/lang/Object;";
            default -> "I";
        };
        c.fields.get(0).desc = fieldDescriptor;
        MethodNode m = c.methods.get(0);
        m.desc = "(" + parameters + ")V";
        for (AbstractInsnNode n : m.instructions.toArray()) {
            if (n instanceof VarInsnNode v && v.getOpcode() == ILOAD) {
                m.instructions.set(n, new VarInsnNode(loadOpcode, readSlot));
            }
            if (n instanceof FieldInsnNode f) f.desc = fieldDescriptor;
            if (n instanceof MethodInsnNode) {
                InsnList write = new InsnList();
                if (writeOpcode == IINC) {
                    write.add(new IincInsnNode(writeSlot, 1));
                } else {
                    int constant = switch (writeOpcode) {
                        case LSTORE -> LCONST_1;
                        case FSTORE -> FCONST_2;
                        case DSTORE -> DCONST_1;
                        case ASTORE -> ACONST_NULL;
                        default -> ICONST_2;
                    };
                    write.add(new InsnNode(constant));
                    write.add(new VarInsnNode(writeOpcode, writeSlot));
                }
                m.instructions.insertBefore(n.getPrevious(), write);
            }
        }
        return c;
    }

    static ClassNode clazz(){ ClassNode c=new ClassNode(); c.version=V17; c.access=ACC_PUBLIC; c.name="Fixture"; c.superName="java/lang/Object"; return c; }
    static MethodNode add(ClassNode c,int access,String name,String desc){ MethodNode m=new MethodNode(access,name,desc,null,null); c.methods.add(m); return m; }
    static ClassNode plain(int access){ ClassNode c=clazz(); add(c,access|ACC_STATIC,"m","()V").instructions.add(new InsnNode(RETURN)); return c; }
    static ClassNode copy(ClassNode c){ ClassNode n=new ClassNode(); c.accept(n); return n; }
    static ClassNode extra(boolean more){ ClassNode c=plain(ACC_PUBLIC); if(more) add(c,ACC_PUBLIC,"extra","()V").instructions.add(new InsnNode(RETURN)); return c; }
    static Handle bsm(){ return new Handle(H_INVOKESTATIC,"Bootstrap","make","()Ljava/lang/invoke/CallSite;",false); }
    static ClassNode lambdas(String prefix,boolean duplicate,int value){
        ClassNode c=clazz(); MethodNode m=add(c,ACC_PUBLIC|ACC_STATIC,"m","()V");
        for(int i=0;i<2;i++){ m.instructions.add(new InvokeDynamicInsnNode("get","()Ljava/util/function/IntSupplier;",bsm(),new Handle(H_INVOKESTATIC,c.name,"lambda$"+prefix+"$"+(duplicate?i:0),"()I",false))); m.instructions.add(new InsnNode(POP)); }
        m.instructions.add(new InsnNode(RETURN));
        for(int i=0;i<(duplicate?2:1);i++){ MethodNode l=add(c,ACC_PRIVATE|ACC_STATIC|ACC_SYNTHETIC,"lambda$"+prefix+"$"+i,"()I"); l.instructions.add(new InsnNode(ICONST_0+value)); l.instructions.add(new InsnNode(IRETURN)); }
        return c;
    }
    static ClassNode handle(int tag,String owner,int access){ ClassNode c=clazz(); MethodNode m=add(c,ACC_PUBLIC,"m","()V");
        m.instructions.add(new InvokeDynamicInsnNode("get","()V",bsm(),new Handle(tag,owner,"lambda$work$0","()V",false))); m.instructions.add(new InsnNode(RETURN));
        add(c,access|ACC_SYNTHETIC,"lambda$work$0","()V").instructions.add(new InsnNode(RETURN)); return c; }
    static ClassNode ctor(String parent,boolean before,boolean call){ ClassNode c=clazz(); c.superName=parent; c.fields.add(new FieldNode(ACC_PRIVATE,"value","I",null,null));
        MethodNode m=add(c,ACC_PUBLIC,"<init>","(I)V");
        if(before) assignment(m);
        if(call) m.instructions.add(new MethodInsnNode(INVOKESTATIC,"Names","name","()V",false));
        m.instructions.add(new VarInsnNode(ALOAD,0)); m.instructions.add(new MethodInsnNode(INVOKESPECIAL,parent,"<init>","()V",false));
        if(!before) assignment(m); m.instructions.add(new InsnNode(RETURN)); return c; }
    static void assignment(MethodNode m){m.instructions.add(new VarInsnNode(ALOAD,0));m.instructions.add(new VarInsnNode(ILOAD,1));m.instructions.add(new FieldInsnNode(PUTFIELD,"Fixture","value","I"));}
    static ClassNode switches(String a,String b,boolean swapped){ClassNode c=clazz(); for(String name:List.of(a,b))c.fields.add(new FieldNode(ACC_STATIC|ACC_SYNTHETIC,"$SwitchMap$"+name,"[I",null,null));
        MethodNode m=add(c,ACC_STATIC,"m","()V"); for(String name:swapped?List.of(b,a):List.of(a,b)){m.instructions.add(new FieldInsnNode(GETSTATIC,"Fixture","$SwitchMap$"+name,"[I"));m.instructions.add(new InsnNode(POP));} m.instructions.add(new InsnNode(RETURN)); return c;}
    static ClassNode caught(boolean extended,boolean invoke,boolean monitor){ClassNode c=clazz();MethodNode m=add(c,ACC_STATIC,"m","()V");
        LabelNode start=new LabelNode(),end=new LabelNode(),handler=new LabelNode(); if(monitor)m.instructions.add(new InsnNode(MONITORENTER));
        m.instructions.add(start);m.instructions.add(new MethodInsnNode(INVOKESTATIC,"Sink","call","()V",false));
        if(!extended)m.instructions.add(end); if(invoke)m.instructions.add(new MethodInsnNode(INVOKESTATIC,"Sink","second","()V",false));
        m.instructions.add(new InsnNode(RETURN));if(extended)m.instructions.add(end);m.instructions.add(handler);m.instructions.add(new InsnNode(ATHROW));
        m.tryCatchBlocks.add(new TryCatchBlockNode(start,end,handler,"java/lang/Throwable"));return c;}
    static ClassNode layout(boolean reorder,boolean wrongCatch){ClassNode c=clazz();MethodNode m=add(c,ACC_STATIC,"m","()V");
        LabelNode start=new LabelNode(),end=new LabelNode(),cleanup=new LabelNode(),handler=new LabelNode(),done=new LabelNode();
        m.instructions.add(start);m.instructions.add(new MethodInsnNode(INVOKESTATIC,"Sink","call","()V",false));m.instructions.add(end);m.instructions.add(new JumpInsnNode(GOTO,cleanup));
        if(reorder){m.instructions.add(handler);m.instructions.add(new InsnNode(ATHROW));}
        m.instructions.add(cleanup);m.instructions.add(new MethodInsnNode(INVOKESTATIC,"Sink","cleanup","()V",false));m.instructions.add(new JumpInsnNode(GOTO,done));
        if(!reorder){m.instructions.add(handler);m.instructions.add(new InsnNode(ATHROW));}
        m.instructions.add(done);m.instructions.add(new InsnNode(RETURN));m.tryCatchBlocks.add(new TryCatchBlockNode(start,end,handler,wrongCatch?"java/lang/Exception":"java/lang/Throwable"));return c;}
    static ClassNode temp(boolean local,int reads){ClassNode c=clazz();MethodNode m=add(c,ACC_STATIC,"m","()V");m.instructions.add(new InsnNode(ACONST_NULL));
        if(local){m.instructions.add(new VarInsnNode(ASTORE,1));for(int i=0;i<reads;i++){m.instructions.add(new VarInsnNode(ALOAD,1));m.instructions.add(new InsnNode(POP));}}
        else {m.instructions.add(new InsnNode(DUP));m.instructions.add(new InsnNode(POP));m.instructions.add(new InsnNode(POP));}m.instructions.add(new InsnNode(RETURN));return c;}
    static Path jar(String name,ClassNode c)throws Exception{Path p=bin.resolve(name+".jar");ClassWriter w=new ClassWriter(0);for(MethodNode m:c.methods){m.maxStack=20;m.maxLocals=30;}c.accept(w);try(ZipOutputStream z=new ZipOutputStream(Files.newOutputStream(p))){z.putNextEntry(new ZipEntry(c.name+".class"));z.write(w.toByteArray());z.closeEntry();}return p;}
    static String[] args(String name,ClassNode l,ClassNode r,String...flags)throws Exception{List<String>a=new ArrayList<>(List.of("--left",jar(name+"-l",l).toString(),"--right",jar(name+"-r",r).toString(),"--json",work.resolve(name+".json").toString(),"--md",work.resolve(name+".md").toString()));
        if(flags.length>0 && flags[0]!=null){Path p=work.resolve(name+".txt");Files.writeString(p,flags[1]+APPROVAL);a.addAll(List.of(flags[0],p.toString()));}return a.toArray(String[]::new);}
    static void check(String name,ClassNode l,ClassNode r,String status,String... flags)throws Exception{SemanticDiff.run(args(name,l,r,flags));String s=Files.readString(work.resolve(name+".json"));if(!s.contains("\""+status+"\": 1"))throw new AssertionError(name+" expected "+status+"\n"+s);checks++;System.out.println("PASS "+name);}
    static void rejected(String name,ClassNode l,ClassNode r,String flag,String entry)throws Exception{String[] a=args(name,l,r,flag,entry);Files.deleteIfExists(work.resolve(name+".json"));try{SemanticDiff.run(a);throw new AssertionError("accepted "+name);}catch(IllegalArgumentException expected){if(Files.exists(work.resolve(name+".json")))throw new AssertionError("success report on error");}checks++;System.out.println("PASS rejected "+name);}
}
