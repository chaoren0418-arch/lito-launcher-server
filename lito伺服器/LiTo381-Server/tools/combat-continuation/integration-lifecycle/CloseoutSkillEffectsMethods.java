import java.nio.file.*;
import java.util.*;
import java.util.jar.*;
import org.objectweb.asm.tree.*;
import static org.objectweb.asm.Opcodes.*;

public final class CloseoutSkillEffectsMethods {
    static final String P="local/lito381/server/",H=P+"NativeCloseoutSkillEffects",ROW=P+"NativeSkillTargetMetadata$Row";
    static final String OLD="SELECT skill_id,target,target_to,ranged,area,through,type,attr,id FROM skills";
    // Preserve the first nine ordinal columns; table.* includes effect_skill_id only when that optional column exists.
    static final String QUERY="SELECT skill_id,target,target_to,ranged,area,through,type,attr,id,skills.* FROM skills";
    static void need(boolean b,String why){if(!b)throw new AssertionError(why);}
    static void patch(ClassNode c){
        if(c.name.equals(ROW)){
            need(c.fields.stream().noneMatch(f->f.name.equals("nativeCloseoutEffectSkillId")),"new optional effect field");
            c.fields.add(new FieldNode(ACC_TRANSIENT,"nativeCloseoutEffectSkillId","I",null,null));return;
        }
        int query=0,attach=0;
        for(var m:c.methods)if(m.name.equals("read")){
            for(var n:m.instructions.toArray())if(n instanceof FrameNode)m.instructions.remove(n);
            int result=-1;for(var n:m.instructions)if(n instanceof MethodInsnNode call&&call.owner.equals("java/sql/ResultSet")&&call.name.equals("next")){
                need(n.getPrevious() instanceof VarInsnNode v&&v.getOpcode()==ALOAD,"original result cursor");result=((VarInsnNode)n.getPrevious()).var;
            }need(result>=0,"one original cursor");
            for(var n:m.instructions.toArray())if(n instanceof LdcInsnNode text&&Objects.equals(text.cst,OLD)){text.cst=QUERY;query++;}
            else if(n instanceof MethodInsnNode call&&call.owner.equals(ROW)&&call.name.equals("<init>")){
                need(n.getNext() instanceof MethodInsnNode put&&put.owner.equals("java/util/HashMap")&&put.name.equals("put"),"attach before row insertion and atomic map publication");
                var code=new InsnList();code.add(new VarInsnNode(ALOAD,result));code.add(new MethodInsnNode(INVOKESTATIC,H,"attach","(L"+ROW+";Ljava/sql/ResultSet;)L"+ROW+";",false));m.instructions.insert(n,code);attach++;
            }
        }need(query==1&&attach==1,"one existing readonly query and row constructor");
    }
    public static void main(String[]args)throws Exception{
        var urls=new ArrayList<java.net.URL>();urls.add(Path.of(args[0]).toUri().toURL());try(var files=Files.list(Path.of(args[2]))){for(var p:files.filter(x->x.toString().endsWith(".jar")).toList())urls.add(p.toUri().toURL());}
        try(var loader=new java.net.URLClassLoader(urls.toArray(java.net.URL[]::new),ClassLoader.getPlatformClassLoader());var jar=new JarFile(args[0])){FreezeAcceptedMethods.hierarchy=loader;for(var name:List.of("NativeSkillTargetMetadata","NativeSkillTargetMetadata$Row")){var c=FreezeAcceptedMethods.read(jar,P+name);patch(c);var out=Path.of(args[1]).resolve(c.name+".class");Files.createDirectories(out.getParent());Files.write(out,FreezeAcceptedMethods.bytes(c,true));}}
    }
}
