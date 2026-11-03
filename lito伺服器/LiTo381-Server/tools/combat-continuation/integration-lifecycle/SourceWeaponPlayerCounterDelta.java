import java.util.*;
import java.util.jar.*;
import org.objectweb.asm.tree.*;
import static org.objectweb.asm.Opcodes.*;

/** Independently undo the new operands/call and compare every original method. */
public final class SourceWeaponPlayerCounterDelta {
    static void need(boolean ok,String why){if(!ok)throw new AssertionError(why);}
    static void pop(MethodNode m,AbstractInsnNode at,int opcode,int slot){
        var operand=FreezeAcceptedMethods.prev(at);
        need(operand instanceof VarInsnNode v&&v.getOpcode()==opcode&&v.var==slot,"exact prior operand "+slot);
        m.instructions.remove(operand);
    }
    public static void main(String[]args)throws Exception{
        String p="local/lito381/server/",owner=p+"ClientSession",helper=p+"NativeSourceWeaponPlayerCounter";
        try(var old=new JarFile(args[0]);var jar=new JarFile(args[1])){
            var names=new HashSet<String>();var changed=new HashSet<String>();
            for(var e:Collections.list(old.entries())){
                names.add(e.getName());var now=jar.getJarEntry(e.getName());need(now!=null,"every inherited entry retained");
                if(!Arrays.equals(old.getInputStream(e).readAllBytes(),jar.getInputStream(now).readAllBytes()))changed.add(e.getName());
            }
            need(changed.equals(Set.of(owner+".class",p+"NativeWeaponBreakBattleCompanion.class",p+"NativeLightAction.class")),"three changed original owners");
            var added=new HashSet<String>();for(var e:Collections.list(jar.entries()))if(!names.contains(e.getName()))added.add(e.getName());
            need(added.equals(Set.of(helper+".class")),"one new fieldless helper");
            int ports=0,methods=0;
            for(var name:List.of(owner,p+"NativeWeaponBreakBattleCompanion",p+"NativeLightAction")){
            var before=FreezeAcceptedMethods.read(old,name);var after=FreezeAcceptedMethods.read(jar,name);
            need(before.fields.size()==after.fields.size()&&before.methods.size()==after.methods.size(),"no original member additions/removals");
            for(int i=0;i<before.fields.size();i++){
                var a=before.fields.get(i);var b=after.fields.get(i);
                need(a.name.equals(b.name)&&a.desc.equals(b.desc)&&a.access==b.access&&Objects.equals(a.value,b.value)&&Objects.equals(a.signature,b.signature),"every original field exact");
            }
            for(var original:before.methods){
                var now=after.methods.stream().filter(m->m.name.equals(original.name)&&m.desc.equals(original.desc)).findFirst().orElseThrow();
                for(var n:now.instructions.toArray())if(n instanceof MethodInsnNode call&&call.owner.equals(helper)){
                    if(name.equals(p+"NativeLightAction")){
                        need(now.name.equals("lambda$commitPair$9")&&call.name.equals("matches")&&call.getOpcode()==INVOKESTATIC&&call.desc.equals("(L"+p+"NativePlayerCommit$Binding;L"+p+"ClientSession;)Z"),"reviewed original after-pair binding guard");
                        call.setOpcode(INVOKEVIRTUAL);call.owner=p+"NativePlayerCommit$Binding";call.name="matches";call.desc="(L"+p+"ClientSession;)Z";ports++;continue;
                    }
                    if(name.equals(p+"NativeWeaponBreakBattleCompanion")){
                        need(now.name.equals("cast")&&call.name.equals("earth")&&call.getOpcode()==INVOKESTATIC&&call.desc.equals("(L"+p+"BattleCompanion;J)Z"),"one reviewed controlled Source27 Earth Bind gate");
                        call.setOpcode(INVOKEVIRTUAL);call.owner=p+"BattleCompanion";call.name="earthBindActive";call.desc="(J)Z";ports++;continue;
                    }
                    need(now.name.equals("nativeMaterialWeaponBreakBody")&&call.name.equals("consume")&&call.getOpcode()==INVOKESTATIC
                        &&call.desc.equals("(L"+p+"BattleTimedStatus;JL"+p+"ClientSession;L"+p+"ClientSession;)Z"),"one reviewed PC weapon counter call");
                    var target=FreezeAcceptedMethods.prev(call);need(target instanceof VarInsnNode v&&v.getOpcode()==ALOAD,"existing target operand");
                    int targetSlot=((VarInsnNode)target).var;now.instructions.remove(target);pop(now,call,ALOAD,0);
                    var time=FreezeAcceptedMethods.prev(call);var status=FreezeAcceptedMethods.prev(time);var source=FreezeAcceptedMethods.prev(status);
                    need(time instanceof VarInsnNode t&&t.getOpcode()==LLOAD&&status instanceof FieldInsnNode f&&f.getOpcode()==GETFIELD&&f.owner.equals(owner)&&f.name.equals("timedStatus")&&source instanceof VarInsnNode v&&v.var==targetSlot&&v.getOpcode()==ALOAD,"same original target status and time");
                    call.setOpcode(INVOKEVIRTUAL);call.owner=p+"BattleTimedStatus";call.name="consumeCounterMagic";call.desc="(J)Z";ports++;
                }
                need(Arrays.equals(SourceNpcFirstViewDelta.body(before,original,original.name),SourceNpcFirstViewDelta.body(after,now,original.name)),"whole original inverse "+original.name);
                methods++;
            }
            }
            need(ports==4,"one PC counter, one controlled Earth Bind and two original pair bindings total");
            System.out.println("PASS independent Source weapon PC counter inverse: "+methods+" original methods,4 selected PC-counter/controlled-Earth/after-pair-binding calls,0 field/sibling changes; complete Source query/cost/RNG/rollback/IO bodies retained");
        }
    }
}
