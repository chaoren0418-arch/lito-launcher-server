import java.nio.file.*;
import java.net.*;
import java.util.*;
import java.util.jar.*;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
import static org.objectweb.asm.Opcodes.*;

/** Keep the native packet shape; match Source's authoritative no-ring choice and post-cost level outcome. */
public final class SummonSelectionMethods {
    static final String P="local/lito381/server/",PC=P+"ClientSession",PROTOCOL=P+"Protocol";
    static AbstractInsnNode prev(AbstractInsnNode n){return NativeSkillConsumptionMethods.prev(n);}
    static AbstractInsnNode next(AbstractInsnNode n){do{n=n.getNext();}while(n!=null&&n.getOpcode()<0);return n;}
    static void remove(MethodNode m,AbstractInsnNode first,AbstractInsnNode end){
        for(var n=first;n!=end;){if(n==null)throw new AssertionError("unbounded selection slice");var following=n.getNext();m.instructions.remove(n);n=following;}
    }
    static ClassNode transform(byte[] original,String name,ClassLoader loader)throws Exception {
        var c=NativeSkillConsumptionMethods.read(original);
        if(name.equals(PC)) {
            var m=c.methods.stream().filter(x->x.name.equals("nativeMaterialSummonBody")).findFirst().orElseThrow();
            MethodInsnNode permits=null,direct=null,close=null;JumpInsnNode elseBranch=null;
            for(var n:m.instructions.toArray()){
                if(n instanceof JumpInsnNode j&&j.getOpcode()==IFEQ&&prev(j) instanceof VarInsnNode v&&v.getOpcode()==ILOAD&&v.var==6&&elseBranch==null)elseBranch=j;
                if(n instanceof MethodInsnNode call){
                    if(call.name.equals("permitsControlSelection"))permits=call;
                    if(call.name.equals("directGrade"))direct=call;
                    if(call.owner.equals(PC)&&call.name.equals("closeSummonList")&&prev(call) instanceof LdcInsnNode text&&text.cst.equals("cast"))close=call;
                }
            }
            if(permits==null||direct==null||close==null||elseBranch==null)throw new AssertionError("selection layout differs");
            var start=prev(prev(prev(prev(permits))));
            if(!(start instanceof VarInsnNode a)||a.var!=2||a.getOpcode()!=ALOAD)throw new AssertionError("level preflight start");
            JumpInsnNode accepted=null;
            for(AbstractInsnNode n=permits;n!=elseBranch.label;n=n.getNext())if(n instanceof JumpInsnNode j&&j.getOpcode()==IF_ICMPGE)accepted=j;
            if(accepted==null)throw new AssertionError("level preflight success");
            LabelNode common=accepted.label;
            remove(m,start,elseBranch.label);
            m.instructions.insertBefore(elseBranch.label,new JumpInsnNode(GOTO,common));
            // The no-ring DWORD is not a trusted target or tier. Keep the computed grade and choice validation.
            MethodInsnNode selection=null;
            for(AbstractInsnNode n=direct;n!=common;n=n.getNext())if(n instanceof MethodInsnNode call&&call.owner.equals(PROTOCOL+"$SummonSpellRequest")&&call.name.equals("selection")){selection=call;break;}
            if(selection==null)throw new AssertionError("direct DWORD comparison absent");
            var grade=next(selection);var conversion=next(grade);var cmp=next(conversion);var jump=next(cmp);
            if(!(grade instanceof VarInsnNode g)||g.getOpcode()!=ILOAD||g.var!=8||conversion.getOpcode()!=I2L||cmp.getOpcode()!=LCMP||!(jump instanceof JumpInsnNode j)||j.getOpcode()!=IFNE)throw new AssertionError("direct comparison shape");
            remove(m,prev(selection),jump.getNext());
            // SUMMON_MONSTER.start: map recall check first, then the ring level check after accepted cost.
            var control=prev(prev(prev(prev(close))));
            if(!(control instanceof VarInsnNode v)||v.var!=6||v.getOpcode()!=ILOAD||!(next(control) instanceof JumpInsnNode gate)||gate.getOpcode()!=IFEQ)throw new AssertionError("post-map control ring branch");
            var okay=new LabelNode();var code=new InsnList();
            code.add(new VarInsnNode(ILOAD,6));code.add(new JumpInsnNode(IFEQ,okay));
            code.add(new VarInsnNode(ILOAD,5));code.add(new VarInsnNode(ALOAD,7));
            code.add(new MethodInsnNode(INVOKEVIRTUAL,P+"BattleSpellCatalog$SummonChoice","requiredLevel","()I",false));
            code.add(new JumpInsnNode(IF_ICMPGE,okay));code.add(new VarInsnNode(ALOAD,0));
            code.add(new VarInsnNode(ALOAD,2));code.add(new MethodInsnNode(INVOKEVIRTUAL,P+"BattleSpellCatalog$SummonSpell","levelFailureMessage","()I",false));
            code.add(new InsnNode(ICONST_0));code.add(new TypeInsnNode(ANEWARRAY,"java/lang/String"));
            code.add(new MethodInsnNode(INVOKESTATIC,PROTOCOL,"messageCode","(I[Ljava/lang/String;)[B",false));
            code.add(new LdcInsnNode("S_MESSAGE_CODE summon-level-after-accepted-cast"));
            code.add(new MethodInsnNode(INVOKEVIRTUAL,PC,"sendEncrypted","([BLjava/lang/String;)V",false));
            code.add(new InsnNode(RETURN));code.add(okay);m.instructions.insertBefore(control,code);
            var framed=NativeSkillConsumptionMethods.frames(c,List.of(m),loader);
            c.methods.set(c.methods.indexOf(m),NativeSkillConsumptionMethods.method(framed,m.name,m.desc));
        } else {
            var m=c.methods.stream().filter(x->x.name.equals("summonSpellRequest")).findFirst().orElseThrow();
            MethodInsnNode selection=null;
            for(var n:m.instructions)if(n instanceof MethodInsnNode call&&call.name.equals("selection")){if(selection!=null)throw new AssertionError("duplicate selection read");selection=call;}
            if(selection==null)throw new AssertionError("zero DWORD gate absent");
            var zero=next(selection);var cmp=next(zero);var jump=next(cmp);
            if(zero.getOpcode()!=LCONST_0||cmp.getOpcode()!=LCMP||!(jump instanceof JumpInsnNode j)||j.getOpcode()!=IFEQ)throw new AssertionError("zero gate shape");
            remove(m,prev(selection),jump.getNext());
            var framed=NativeSkillConsumptionMethods.frames(c,List.of(m),loader);
            c.methods.set(c.methods.indexOf(m),NativeSkillConsumptionMethods.method(framed,m.name,m.desc));
        }
        return c;
    }
    public static void main(String[] args)throws Exception {
        try(var old=new JarFile(args[1]);var loader=new URLClassLoader(new URL[]{Path.of(args[1]).toUri().toURL()},ClassLoader.getPlatformClassLoader())){
            for(String name:List.of(PC,PROTOCOL)){
                byte[] bytes=old.getInputStream(old.getJarEntry(name+".class")).readAllBytes();
                var changed=transform(bytes,name,loader);
                if(args[0].equals("splice")){
                    var path=Path.of(args[2]).resolve(name+".class");Files.createDirectories(path.getParent());Files.write(path,NativeSkillConsumptionMethods.bytes(changed,false));
                }else try(var candidate=new JarFile(args[2])){
                    var actual=NativeSkillConsumptionMethods.read(candidate.getInputStream(candidate.getJarEntry(name+".class")).readAllBytes());
                    if(!Arrays.equals(NativeSkillConsumptionMethods.bytes(actual,true),NativeSkillConsumptionMethods.bytes(changed,true)))throw new AssertionError("transform mismatch "+name);
                    var before=NativeSkillConsumptionMethods.read(bytes);String method=name.equals(PC)?"nativeMaterialSummonBody":"summonSpellRequest";
                    var original=before.methods.stream().filter(x->x.name.equals(method)).findFirst().orElseThrow();
                    var target=NativeSkillConsumptionMethods.method(actual,method,original.desc);actual.methods.set(actual.methods.indexOf(target),original);
                    if(!Arrays.equals(NativeSkillConsumptionMethods.bytes(actual,true),NativeSkillConsumptionMethods.bytes(before,true)))throw new AssertionError("unrelated methods changed "+name);
                }
            }
        }
        System.out.println("PASS exact summon selection changes and unrelated-method inverse");
    }
}
