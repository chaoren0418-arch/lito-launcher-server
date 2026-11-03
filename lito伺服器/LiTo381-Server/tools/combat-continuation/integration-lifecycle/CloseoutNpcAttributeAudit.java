import java.nio.file.*;
import java.util.*;
import java.util.jar.*;
import jdk.internal.org.objectweb.asm.tree.*;
import static jdk.internal.org.objectweb.asm.Opcodes.*;

public final class CloseoutNpcAttributeAudit {
    static final String P="local/lito381/server/",D=P+"BattleTimeAttackMagicSkillCatalog$Definition",H=P+"NativeCloseoutNpcAttribute";
    static void need(boolean b,String why){CloseoutAreaOutputAudit.need(b,why);}
    public static void main(String[]args)throws Exception{
        int count=0,methods=0;
        try(var base=new JarFile(args[0]);var candidate=new JarFile(args[1])){
            var a=CloseoutAreaOutputAudit.read(base,"ClientSession");var b=CloseoutAreaOutputAudit.read(candidate,"ClientSession");
            for(var m:b.methods)for(var n:m.instructions.toArray())if(n instanceof MethodInsnNode call&&call.owner.equals(H)){
                need(Set.of("serverQuestMonsterMagicAttack","serverTimeAttackMonsterMagicAttack").contains(m.name)&&call.getOpcode()==INVOKESTATIC&&call.name.equals("attribute")&&call.desc.equals("(L"+D+";)I")&&call.getNext() instanceof MethodInsnNode next&&next.owner.equals(b.name)&&next.name.equals("effectiveSpellElementalResistance"),"bounded existing attribute read");
                call.setOpcode(INVOKEVIRTUAL);call.owner=D;call.desc="()I";count++;
            }
            need(count==2&&a.fields.size()==b.fields.size()&&a.methods.size()==b.methods.size(),"exact two attribute seams and member counts");
            for(int i=0;i<a.fields.size();i++){var x=a.fields.get(i);var y=b.fields.get(i);need(x.name.equals(y.name)&&x.desc.equals(y.desc)&&x.access==y.access&&Objects.equals(x.value,y.value),"original field");}
            for(int i=0;i<a.methods.size();i++){var x=a.methods.get(i);var y=b.methods.get(i);need(x.name.equals(y.name)&&x.desc.equals(y.desc)&&x.access==y.access&&CloseoutAreaOutputAudit.text(x).equals(CloseoutAreaOutputAudit.text(y)),"retained method "+x.name);methods++;}
            for(var e:Collections.list(base.entries())){if(e.getName().equals(P+"ClientSession.class"))continue;need(Arrays.equals(base.getInputStream(e).readAllBytes(),candidate.getInputStream(candidate.getJarEntry(e.getName())).readAllBytes()),"retained entry "+e.getName());}
        }
        Files.writeString(Path.of(args[2]),"Static inverse: "+methods+" ClientSession methods retained after reverting exactly two attribute reads. All other original entries byte-identical; existing resistance and coefficient arithmetic retained. No application execution or tests.\n");
    }
}
