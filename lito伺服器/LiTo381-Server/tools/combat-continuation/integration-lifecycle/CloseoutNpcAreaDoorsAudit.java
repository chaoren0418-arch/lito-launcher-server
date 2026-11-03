import java.nio.file.*;
import java.util.*;
import java.util.jar.*;
import jdk.internal.org.objectweb.asm.tree.*;
import static jdk.internal.org.objectweb.asm.Opcodes.*;

public final class CloseoutNpcAreaDoorsAudit {
    static final String P="local/lito381/server/", C="NativeCloseoutNpcAreaTargets", H=P+"NativeCloseoutNpcAreaDoors";
    static void need(boolean b,String why){CloseoutAreaOutputAudit.need(b,why);}
    public static void main(String[]args)throws Exception {
        int count=0,methods=0;try(var base=new JarFile(args[0]);var candidate=new JarFile(args[1])) {
            var a=CloseoutAreaOutputAudit.read(base,C);var b=CloseoutAreaOutputAudit.read(candidate,C);
            for(var m:b.methods)for(var n:m.instructions.toArray())if(n instanceof MethodInsnNode c&&c.owner.equals(H)) {
                need(m.name.equals("eligible")&&c.name.equals("lineOfSight")&&c.desc.equals("(L"+P+"ArenaMapCatalog$Arena;IIIIL"+P+C+"$Selection;)Z"),"single explicit first-scope LOS bridge");
                var load=n.getPrevious();need(load instanceof VarInsnNode v&&v.getOpcode()==ALOAD&&v.var==0,"first Selection proof");m.instructions.remove(load);
                c.owner=P+"BattleSpellCatalog";c.name="hasNativeLineOfSight";c.desc="(L"+P+"ArenaMapCatalog$Arena;IIII)Z";count++;
            }
            need(count==1&&a.fields.size()==b.fields.size()&&a.methods.size()==b.methods.size(),"one bridge with original members");
            for(int i=0;i<a.methods.size();i++){var x=a.methods.get(i);var y=b.methods.get(i);need(x.name.equals(y.name)&&x.desc.equals(y.desc)&&CloseoutAreaOutputAudit.text(x).equals(CloseoutAreaOutputAudit.text(y)),"retained target admission "+x.name);methods++;}
            for(var e:Collections.list(base.entries())){if(e.getName().equals(P+C+".class"))continue;need(Arrays.equals(base.getInputStream(e).readAllBytes(),candidate.getInputStream(candidate.getJarEntry(e.getName())).readAllBytes()),"retained archive entry "+e.getName());}
            var helper=CloseoutAreaOutputAudit.read(candidate,"NativeCloseoutNpcAreaDoors");need(helper.fields.isEmpty(),"no retained room or terrain state");int fresh=0,doors=0,rays=0;
            for(var m:helper.methods)for(var n:m.instructions)if(n instanceof MethodInsnNode c){
                need(!c.owner.contains("Random")&&!c.owner.startsWith("java/sql/")&&!c.owner.contains("Scheduled")&&!Set.of("setTile","setTestTile","setArrowPassableOverride").contains(c.name),"no RNG/SQL/scheduler/raw grid writes");
                if(c.owner.equals(P+"NativeDeTerrainV1")&&c.name.equals("<init>")&&c.desc.equals("()V"))fresh++;
                if(c.owner.equals(P+"NativeDeTerrainV1")&&c.name.equals("setPassable")&&c.desc.equals("(IIZI)V"))doors++;
                if(c.owner.equals(P+"NativeCloseoutSourceLineOfSight")&&c.name.equals("hasLineOfSight"))rays++;
                if(c.owner.equals(P+"CombatAccess")&&c.name.equals("set"))need(m.name.equals("overlay"),"writes confined to new overlay initialization");
            }need(fresh==1&&doors==1&&rays==2,"original Source terrain ctor/door setter/ray");
        }
        Files.writeString(Path.of(args[2]),"Static inverse: "+methods+" target helper methods retained; one explicit first-scope LOS bridge. All other archive entries, including ClientSession, exact Source ray and terrain methods, are byte-identical. Fresh per-evaluation terrain shares only readonly arrays/bounds and owns its door map; no retained room/cache state. No tests, game execution or database connection.\n");
    }
}
