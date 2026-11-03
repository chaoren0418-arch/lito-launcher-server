import java.nio.file.*;
import java.util.*;
import java.util.jar.*;
import jdk.internal.org.objectweb.asm.tree.*;
import static jdk.internal.org.objectweb.asm.Opcodes.*;

public final class CloseoutBattleLineOfSightAudit {
    static final String P="local/lito381/server/", C="BattleSpellCatalog", A="ArenaMapCatalog$Arena", H=P+"NativeCloseoutBattleLineOfSight";
    static final String D="(L"+P+A+";IIII)Z", LEGACY="nativeCloseoutLegacyLineOfSight", F="nativeCloseoutSourceTerrain";
    static void need(boolean b,String why){CloseoutAreaOutputAudit.need(b,why);}
    static void load(AbstractInsnNode n,int op,int slot){need(n instanceof VarInsnNode v&&v.getOpcode()==op&&v.var==slot,"original typed geometry arguments");}
    static void call(AbstractInsnNode n,int op,String owner,String name,String desc){need(n instanceof MethodInsnNode c&&c.getOpcode()==op&&c.owner.equals(owner)&&c.name.equals(name)&&c.desc.equals(desc),"geometry routing call "+name);}
    static void wrapper(MethodNode m) {
        var n=m.instructions.toArray();int i=0;need(n.length==20,"exact geometry dispatcher length");
        load(n[i++],ALOAD,0);for(int k=1;k<=4;k++)load(n[i++],ILOAD,k);
        call(n[i++],INVOKESTATIC,H,"source",D.replace(")Z",")Ljava/lang/Boolean;"));load(n[i++],ASTORE,5);load(n[i++],ALOAD,5);
        need(n[i] instanceof JumpInsnNode j&&j.getOpcode()==IFNONNULL,"legacy only on absent Source result");var branch=(JumpInsnNode)n[i++];
        load(n[i++],ALOAD,0);for(int k=1;k<=4;k++)load(n[i++],ILOAD,k);
        call(n[i++],INVOKESTATIC,P+C,LEGACY,D);need(n[i++].getOpcode()==IRETURN,"legacy result unchanged");
        need(n[i++]==branch.label,"Source branch destination");load(n[i++],ALOAD,5);call(n[i++],INVOKEVIRTUAL,"java/lang/Boolean","booleanValue","()Z");need(n[i++].getOpcode()==IRETURN,"Source result unchanged");
        // ClassReader SKIP_FRAMES may retain only the single branch label.
        need(i==n.length,"complete dispatcher");
    }
    static void helper(JarFile jar)throws Exception {
        var c=CloseoutAreaOutputAudit.read(jar,"NativeCloseoutBattleLineOfSight");int snapshot=0,create=0,ray=0,cacheWrites=0;
        for(var m:c.methods)for(var n:m.instructions)if(n instanceof MethodInsnNode call) {
            need(!call.owner.startsWith("java/sql/")&&!call.owner.contains("Random")&&!call.owner.contains("Scheduled")&&!call.name.equals("load"),"no SQL, RNG, scheduler or resource reload");
            if(call.owner.equals(P+"NativeDeTerrainResources")&&call.name.equals("snapshot"))snapshot++;
            if(call.owner.equals(P+"NativeDeTerrainResources")&&call.name.equals("create"))create++;
            if(call.owner.equals(P+"NativeCloseoutSourceLineOfSight")&&call.name.equals("hasLineOfSight"))ray++;
            if(call.owner.equals(P+"CombatAccess")&&call.name.equals("set"))cacheWrites++;
        }
        need(snapshot==1&&create==1&&ray==1&&cacheWrites==1,"one readonly snapshot, original verified resource reader and exact Source ray");
        var cache=CloseoutAreaOutputAudit.read(jar,"NativeCloseoutBattleLineOfSight$Cache");
        need(cache.fields.size()==2&&cache.fields.get(0).desc.equals("L"+P+"NativeDeTerrainResources$Snapshot;")&&cache.fields.get(1).desc.equals("L"+P+"NativeDeTerrainV1;"),"data-only cache retains no actor, room, scope or timer");
    }
    public static void main(String[]args)throws Exception {
        int methods=0;try(var base=new JarFile(args[0]);var candidate=new JarFile(args[1])) {
            for(var name:List.of(C,A)) {
                var a=CloseoutAreaOutputAudit.read(base,name);var b=CloseoutAreaOutputAudit.read(candidate,name);
                if(name.equals(A)) {
                    var fields=b.fields.stream().filter(f->f.name.equals(F)).toList();need(fields.size()==1,"one arena cache field");var f=fields.get(0);
                    need(f.access==(ACC_PRIVATE|ACC_VOLATILE|ACC_TRANSIENT)&&f.desc.equals("Ljava/lang/Object;"),"volatile transient arena-owned cache");b.fields.remove(f);
                } else {
                    var original=a.methods.stream().filter(m->m.name.equals("hasNativeLineOfSight")&&m.desc.equals(D)).findFirst().orElseThrow();
                    var changed=b.methods.stream().filter(m->m.name.equals("hasNativeLineOfSight")&&m.desc.equals(D)).findFirst().orElseThrow();wrapper(changed);
                    var legacy=b.methods.stream().filter(m->m.name.equals(LEGACY)&&m.desc.equals(D)).findFirst().orElseThrow();legacy.name=original.name;
                    b.methods.set(b.methods.indexOf(changed),legacy);b.methods.remove(b.methods.lastIndexOf(legacy));
                }
                need(a.fields.size()==b.fields.size()&&a.methods.size()==b.methods.size(),"original member counts");
                for(int i=0;i<a.methods.size();i++){var x=a.methods.get(i);var y=b.methods.get(i);need(x.name.equals(y.name)&&x.desc.equals(y.desc)&&x.access==y.access&&CloseoutAreaOutputAudit.text(x).equals(CloseoutAreaOutputAudit.text(y)),"retained original method "+name+"."+x.name);methods++;}
            }
            for(var e:Collections.list(base.entries())) {if(e.getName().equals(P+C+".class")||e.getName().equals(P+A+".class"))continue;need(Arrays.equals(base.getInputStream(e).readAllBytes(),candidate.getInputStream(candidate.getJarEntry(e.getName())).readAllBytes()),"retained archive entry "+e.getName());}
            helper(candidate);
        }
        Files.writeString(Path.of(args[2]),"Static inverse: "+methods+" original catalog/arena methods retained, including the complete legacy LOS body. Shared BATTLE LOS routes loaded Source terrain through three exact extracted L1Character methods; only the map type is relocated. Original Source terrain resources/readers and all other archive entries retained. Arena-owned cache holds only readonly snapshot/terrain, no actor or room. Dynamic room door overlays and NPC area target-list ingress remain open. No tests, SQL connections or game execution.\n");
    }
}
