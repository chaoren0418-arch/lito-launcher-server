import java.nio.file.*;
import java.util.*;
import java.util.jar.*;
import jdk.internal.org.objectweb.asm.tree.*;
import static jdk.internal.org.objectweb.asm.Opcodes.*;

public final class CloseoutSkillEffectsAudit {
    static final String P="local/lito381/server/",H=P+"NativeCloseoutSkillEffects",ROW=P+"NativeSkillTargetMetadata$Row";
    static final String OLD="SELECT skill_id,target,target_to,ranged,area,through,type,attr,id FROM skills", QUERY="SELECT skill_id,target,target_to,ranged,area,through,type,attr,id,skills.* FROM skills";
    static void need(boolean b,String why){CloseoutAreaOutputAudit.need(b,why);}
    public static void main(String[]args)throws Exception{
        int methods=0,queries=0,attach=0;
        try(var base=new JarFile(args[0]);var candidate=new JarFile(args[1])){
            for(var name:List.of("NativeSkillTargetMetadata","NativeSkillTargetMetadata$Row")){
                var a=CloseoutAreaOutputAudit.read(base,name);var b=CloseoutAreaOutputAudit.read(candidate,name);
                if(name.endsWith("$Row"))need(b.fields.removeIf(f->f.name.equals("nativeCloseoutEffectSkillId")&&f.desc.equals("I")&&(f.access&ACC_TRANSIENT)!=0),"optional noncomponent field; original record ABI retained");
                for(var m:b.methods)for(var n:m.instructions.toArray()){
                    if(n instanceof LdcInsnNode text&&Objects.equals(text.cst,QUERY)){need(m.name.equals("read"),"original readonly transaction");text.cst=OLD;queries++;}
                    if(n instanceof MethodInsnNode call&&call.owner.equals(H)){
                        need(m.name.equals("read")&&call.name.equals("attach")&&call.desc.equals("(L"+ROW+";Ljava/sql/ResultSet;)L"+ROW+";"),"one row attachment");
                        var result=n.getPrevious();need(result instanceof VarInsnNode v&&v.getOpcode()==ALOAD&&v.var==4,"original result set");
                        need(result.getPrevious() instanceof MethodInsnNode ctor&&ctor.owner.equals(ROW)&&ctor.name.equals("<init>")&&n.getNext() instanceof MethodInsnNode put&&put.owner.equals("java/util/HashMap")&&put.name.equals("put"),"before insertion and original atomic publication");
                        m.instructions.remove(result);m.instructions.remove(n);attach++;
                    }
                }
                need(a.fields.size()==b.fields.size()&&a.methods.size()==b.methods.size(),"original members");
                for(int i=0;i<a.methods.size();i++){var x=a.methods.get(i);var y=b.methods.get(i);need(x.name.equals(y.name)&&x.desc.equals(y.desc)&&x.access==y.access&&CloseoutAreaOutputAudit.text(x).equals(CloseoutAreaOutputAudit.text(y)),"original body "+name+"."+x.name);methods++;}
            }
            need(queries==1&&attach==1,"exact optional field ingress");
            for(var e:Collections.list(base.entries())){if(Set.of(P+"NativeSkillTargetMetadata.class",ROW+".class").contains(e.getName()))continue;need(Arrays.equals(base.getInputStream(e).readAllBytes(),candidate.getInputStream(candidate.getJarEntry(e.getName())).readAllBytes()),"unrelated entry "+e.getName());}
            var helper=CloseoutAreaOutputAudit.read(candidate,"NativeCloseoutSkillEffects");int writes=0,reads=0,lookups=0;
            for(var m:helper.methods)for(var n:m.instructions){
                if(n instanceof FieldInsnNode field&&field.getOpcode()==PUTFIELD){need(m.name.equals("attach")&&field.owner.equals(ROW)&&field.name.equals("nativeCloseoutEffectSkillId"),"only prepublication row write");writes++;}
                if(n instanceof MethodInsnNode call){need(!call.owner.contains("Random")&&!call.owner.equals(P+"Protocol")&&!call.owner.contains("Scheduled"),"data-only helper");
                    if(call.owner.startsWith("java/sql/")){need(m.name.equals("attach")&&call.owner.equals("java/sql/ResultSet")&&call.name.equals("getInt")&&call.desc.equals("(Ljava/lang/String;)I"),"no connection, query or SQL write added");reads++;}
                    if(call.owner.equals("java/util/function/IntFunction")&&call.name.equals("apply")){need(m.name.equals("mode"),"mode lookup boundary");lookups++;}
                }
            }need(writes==1&&reads==1&&lookups==2,"one optional read/write and bounded direct-first fallback");
            var read=helper.methods.stream().filter(m->m.name.equals("attach")).findFirst().orElseThrow();need(read.tryCatchBlocks.size()==1&&read.tryCatchBlocks.get(0).type.equals("java/sql/SQLException"),"Source missing-column compatibility");
        }
        Files.writeString(Path.of(args[2]),"Static inverse: "+methods+" original metadata/record methods retained; original readonly/autocommit guards, row validation, resource cleanup and atomic map publication remain. One existing query includes table columns while preserving first nine ordinals; one prepublication optional effect id read. Other archive entries byte-identical. Source direct-mode priority and one-hop fallback API compiled; execution consumers remain open. No database connection, tests or deployment.\n");
    }
}
