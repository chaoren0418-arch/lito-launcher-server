import java.util.*;
import java.util.jar.*;
import org.objectweb.asm.tree.*;
import static org.objectweb.asm.Opcodes.*;

/** Independently undo the new operands/call and compare every original method. */
public final class SourceWeaponCounterMagicDelta {
    static void need(boolean ok,String why){if(!ok)throw new AssertionError(why);}
    static void pop(MethodNode m,AbstractInsnNode at,int opcode,int slot){
        var operand=FreezeAcceptedMethods.prev(at);
        need(operand instanceof VarInsnNode v&&v.getOpcode()==opcode&&v.var==slot,"exact prior operand "+slot);
        m.instructions.remove(operand);
    }
    public static void main(String[]args)throws Exception{
        String p="local/lito381/server/",owner=p+"NativeWeaponBreakBattleCompanion",helper=p+"NativeSourceWeaponCounterMagic";
        try(var old=new JarFile(args[0]);var jar=new JarFile(args[1])){
            var names=new HashSet<String>();var changed=new HashSet<String>();
            for(var e:Collections.list(old.entries())){
                names.add(e.getName());var now=jar.getJarEntry(e.getName());need(now!=null,"every inherited entry retained");
                if(!Arrays.equals(old.getInputStream(e).readAllBytes(),jar.getInputStream(now).readAllBytes()))changed.add(e.getName());
            }
            need(changed.equals(Set.of(owner+".class")),"one changed original owner");
            var added=new HashSet<String>();for(var e:Collections.list(jar.entries()))if(!names.contains(e.getName()))added.add(e.getName());
            need(added.equals(Set.of(helper+".class")),"one new fieldless helper");
            var before=FreezeAcceptedMethods.read(old,owner);var after=FreezeAcceptedMethods.read(jar,owner);
            need(before.fields.size()==after.fields.size()&&before.methods.size()==after.methods.size(),"no original member additions/removals");
            for(int i=0;i<before.fields.size();i++){
                var a=before.fields.get(i);var b=after.fields.get(i);
                need(a.name.equals(b.name)&&a.desc.equals(b.desc)&&a.access==b.access&&Objects.equals(a.value,b.value)&&Objects.equals(a.signature,b.signature),"every original field exact");
            }
            int ports=0,methods=0;
            for(var original:before.methods){
                var now=after.methods.stream().filter(m->m.name.equals(original.name)&&m.desc.equals(original.desc)).findFirst().orElseThrow();
                for(var n:now.instructions.toArray())if(n instanceof MethodInsnNode call&&call.owner.equals(helper)){
                    need(now.name.equals("cast")&&call.name.equals("stack")&&call.getOpcode()==INVOKESTATIC
                        &&call.desc.equals("(L"+p+"BattleCompanion;L"+p+"ClientSession;Ljava/util/List;J)I"),"one reviewed helper call");
                    var id=FreezeAcceptedMethods.prev(call);
                    need(id instanceof MethodInsnNode invoke&&invoke.getOpcode()==INVOKEVIRTUAL&&invoke.owner.equals(p+"BattleCompanion$Snapshot")&&invoke.name.equals("objectId")&&invoke.desc.equals("()J"),"same original target id");
                    now.instructions.remove(id);pop(now,call,ALOAD,8);pop(now,call,ALOAD,28);pop(now,call,ALOAD,0);
                    call.setOpcode(INVOKEVIRTUAL);call.owner=p+"BattleCompanion";call.name="addWeaponBreakStack";call.desc="()I";ports++;
                }
                need(Arrays.equals(SourceNpcFirstViewDelta.body(before,original,original.name),SourceNpcFirstViewDelta.body(after,now,original.name)),"whole original inverse "+original.name);
                methods++;
            }
            need(ports==1,"one successful-stack port total");
            System.out.println("PASS independent Source weapon Counter Magic inverse: "+methods+" original methods,1 selected successful-stack call,0 field/sibling changes; complete Source query/cost/RNG/rollback/IO bodies retained");
        }
    }
}
