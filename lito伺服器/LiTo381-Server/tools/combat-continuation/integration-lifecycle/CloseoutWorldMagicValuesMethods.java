import java.nio.file.*;
import java.util.*;
import java.util.jar.*;
import org.objectweb.asm.tree.*;
import static org.objectweb.asm.Opcodes.*;

public final class CloseoutWorldMagicValuesMethods {
    static final String P="local/lito381/server/",H=P+"NativeCloseoutWorldMagicValues",V=P+"NativeCloseoutNpcSkillValues$Values",D=P+"ClassicWorldMonsterMagicSkillCatalog$Definition",A=P+"ClassicWorldMonsterPopulation$Actor",S=P+"ClassicWorldMonsterStatusSkillCatalog$Definition";
    static void patch(ClassNode c){
        if(c.name.equals(P+"ClassicWorldMonsterMagicDamage")){
            int count=0;for(var m:c.methods)if(m.name.equals("rollSourceDamage")){
                int slot=m.maxLocals++;for(var n:m.instructions.toArray())if(n instanceof MethodInsnNode call&&call.owner.equals(D)&&Set.of("baseDamage","diceCount","dieSides").contains(call.name)){
                    var x=new InsnList();if(call.name.equals("baseDamage")){x.add(new MethodInsnNode(INVOKESTATIC,H,"capture","(L"+D+";)L"+V+";",false));x.add(new InsnNode(DUP));x.add(new VarInsnNode(ASTORE,slot));}
                    else{x.add(new InsnNode(POP));x.add(new VarInsnNode(ALOAD,slot));}m.instructions.insertBefore(n,x);call.owner=V;count++;
                }
            }if(count!=4)throw new AssertionError("four original dice value reads");return;
        }
        int methods=0,reads=0,pays=0;
        for(var m:c.methods)if(Set.of("magicAttack","ckewSkillAttack").contains(m.name)&&m.desc.contains("Ljava/util/Set;")){
            var slots=new HashMap<String,Integer>();int methodReads=0;
            for(var n:m.instructions.toArray()){
                if(n instanceof MethodInsnNode call&&Set.of(D,S).contains(call.owner)&&call.name.equals("mpCost")){
                    String owner=call.owner;int slot=slots.computeIfAbsent(owner,k->m.maxLocals++);
                    if(methodReads==0||methodReads==2&&m.name.equals("ckewSkillAttack")){
                        var definition=n.getPrevious();var mp=definition.getPrevious();var actor=mp.getPrevious();
                        if(!(definition instanceof VarInsnNode dv&&dv.getOpcode()==ALOAD&&mp instanceof FieldInsnNode f&&f.getOpcode()==GETFIELD&&f.owner.equals(A)&&f.name.equals("currentMp")&&actor instanceof VarInsnNode av&&av.getOpcode()==ALOAD&&av.var==14))throw new AssertionError("review original cost operands");
                        var x=new InsnList();var accepted=new LabelNode();x.add(new VarInsnNode(ALOAD,14));x.add(new VarInsnNode(ALOAD,dv.var));x.add(new MethodInsnNode(INVOKESTATIC,H,owner.equals(S)?"statusCost":"cost","(Ljava/lang/Object;L"+owner+";)L"+V+";",false));x.add(new VarInsnNode(ASTORE,slot));x.add(new VarInsnNode(ALOAD,14));x.add(new VarInsnNode(ALOAD,slot));x.add(new MethodInsnNode(INVOKESTATIC,H,"ready","(Ljava/lang/Object;L"+V+";)Z",false));x.add(new JumpInsnNode(IFNE,accepted));x.add(new InsnNode(ACONST_NULL));x.add(new InsnNode(ARETURN));x.add(accepted);m.instructions.insertBefore(actor,x);
                    }
                    var x=new InsnList();x.add(new InsnNode(POP));x.add(new VarInsnNode(ALOAD,slot));m.instructions.insertBefore(n,x);call.owner=V;call.name="mp";reads++;methodReads++;
                }
                if(n instanceof FieldInsnNode f&&f.getOpcode()==PUTFIELD&&f.owner.equals(A)&&f.name.equals("currentMp")&&n.getPrevious().getOpcode()==ISUB&&n.getPrevious().getPrevious() instanceof MethodInsnNode debit&&debit.owner.equals(V)&&debit.name.equals("mp")){
                    int slot=((VarInsnNode)n.getPrevious().getPrevious().getPrevious()).var;var x=new InsnList();x.add(new VarInsnNode(ALOAD,14));x.add(new VarInsnNode(ALOAD,slot));x.add(new MethodInsnNode(INVOKESTATIC,H,"payHp","(Ljava/lang/Object;L"+V+";)V",false));m.instructions.insert(n,x);pays++;
                }
            }if(methodReads!=(m.name.equals("ckewSkillAttack")?4:2))throw new AssertionError("exact original MP check/payment");methods++;
        }if(methods!=2||reads!=6||pays!=3)throw new AssertionError("three original WORLD cost routes");
    }
    public static void main(String[]args)throws Exception{
        var urls=new ArrayList<java.net.URL>();urls.add(Path.of(args[0]).toUri().toURL());try(var files=Files.list(Path.of(args[2]))){for(var p:files.filter(x->x.toString().endsWith(".jar")).toList())urls.add(p.toUri().toURL());}
        try(var loader=new java.net.URLClassLoader(urls.toArray(java.net.URL[]::new),ClassLoader.getPlatformClassLoader());var jar=new JarFile(args[0])){FreezeAcceptedMethods.hierarchy=loader;for(var name:List.of("ClassicWorldMonsterPopulation","ClassicWorldMonsterMagicDamage")){var c=FreezeAcceptedMethods.read(jar,P+name);patch(c);var out=Path.of(args[1]).resolve(c.name+".class");Files.createDirectories(out.getParent());Files.write(out,FreezeAcceptedMethods.bytes(c,true));}}
    }
}
