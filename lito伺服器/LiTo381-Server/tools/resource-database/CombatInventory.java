package local.lito381.server;
import java.nio.file.*;
import java.sql.*;
import java.util.*;
import java.lang.reflect.*;
import java.util.zip.*;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;

/** Emits evidence only. A catalog or handler is never a behavioral pass. */
public class CombatInventory {
    static String clean(Object v) { return String.valueOf(v).replace('\t',' ').replace('\n',' ').replace('\r',' '); }
    public static void main(String[] a) throws Exception {
        var config=ServerConfig.load(Path.of(a[0]),"standard");
        ResourceDatabase.load(config);
        var sourceBlessings=CombatDatabase.controls().blessings();
        System.out.println("HEADER\tBLESSING_NPC\tmap_id\tspawn_id\tnpc_id\tx\ty\theading\texecutor");
        sourceBlessings.npcs().values().stream().flatMap(List::stream).sorted(Comparator.comparingInt(ClassicWorldNpcCatalog.StaticNpc::spawnId)).forEach(n->
            System.out.println("BLESSING_NPC\t"+n.mapId()+"\t"+n.spawnId()+"\t"+n.npcId()+"\t"+n.x()+"\t"+n.y()+"\t"+n.heading()+"\t"+n.implementation()));
        System.out.println("HEADER\tBLESSING_MAP_INPUT\tpath\tsha256");sourceBlessings.mapHashes().forEach((path,hash)->System.out.println("BLESSING_MAP_INPUT\t"+path+"\t"+hash.toLowerCase(Locale.ROOT)));
        var p=UnifiedConfiguration.read(config.source());
        String db=SharedDatabase.identifier(p.getProperty("database.static.database"));
        try(var c=DriverManager.getConnection(p.getProperty("database.jdbc.base.url")+db
                +p.getProperty("database.jdbc.options"),p.getProperty("database.user"),p.getProperty("database.password"))) {
            c.setReadOnly(true);
            try(var st=c.createStatement()) {
                try(var r=st.executeQuery("SELECT m.*,s.effect_skill_id FROM mobskill m INNER JOIN skills s ON s.skill_id=m.SkillId WHERE s.effect_skill_id>0 AND s.effect_skill_id<>s.skill_id ORDER BY m.mobid,m.actNo")) {
                    var names=new ArrayList<String>();for(int i=1;i<=r.getMetaData().getColumnCount();i++)names.add(r.getMetaData().getColumnLabel(i));System.out.println("HEADER\tALIASED_MOBSKILL\t"+String.join("\t",names));
                    while(r.next()){var row=new ArrayList<String>();for(int i=1;i<=names.size();i++)row.add(clean(r.getObject(i)));System.out.println("ALIASED_MOBSKILL\t"+String.join("\t",row));}
                }
                try(var r=st.executeQuery("SELECT eventstart FROM server_event WHERE eventclass='SkillCoexistSet' LIMIT 1")) {
                    System.out.println("SOURCE_EVENT\tSkillCoexistSet\t"+(r.next()?r.getInt(1):0));
                }
                for(var entry:Map.of("EYE_CONFIG","etcitem_buff藥水設置","EYE_ABILITY","etcitem_buff藥水能力").entrySet()) {
                    try(var r=st.executeQuery("SELECT * FROM `"+entry.getValue()+"` WHERE item_id BETWEEN 42518 AND 42524 ORDER BY item_id")) {
                        var names=new ArrayList<String>();for(int i=1;i<=r.getMetaData().getColumnCount();i++)names.add(r.getMetaData().getColumnName(i));
                        System.out.println("HEADER\t"+entry.getKey()+"\t"+String.join("\t",names));
                        while(r.next()){var row=new ArrayList<String>();for(int i=1;i<=names.size();i++)row.add(clean(r.getObject(i)));System.out.println(entry.getKey()+"\t"+String.join("\t",row));}
                    }
                }
                for(var entry:Map.of("SKILL","skills","BUFF","skills_buff_ability",
                        "PROBABILITY","skills_負面技能控制","BOSS","skills_負面boss免疫","CANCEL_IMMUNE","skills_相消免疫技能").entrySet()) {
                    try(var r=st.executeQuery("SELECT * FROM `"+entry.getValue()+"` ORDER BY skill_id")) {
                        var names=new ArrayList<String>();
                        for(int i=1;i<=r.getMetaData().getColumnCount();i++) names.add(r.getMetaData().getColumnName(i));
                        System.out.println("HEADER\t"+entry.getKey()+"\t"+String.join("\t",names));
                        while(r.next()) {
                            var row=new ArrayList<String>();
                            for(int i=1;i<=names.size();i++) row.add(clean(r.getObject(i)));
                            System.out.println(entry.getKey()+"\t"+String.join("\t",row));
                        }
                    }
                }
            }
        }
        Set<String> seen=new HashSet<>();
        for(var f:BattleSpellCatalog.class.getDeclaredFields()) if(Modifier.isStatic(f.getModifiers())&&Map.class.isAssignableFrom(f.getType())) {
            f.setAccessible(true);
            for(Object v:((Map<?,?>)f.get(null)).values()) if(v!=null) {
                final Method get;
                try { get=v.getClass().getDeclaredMethod("skillId"); }
                catch(NoSuchMethodException ignored) { continue; }
                get.setAccessible(true);int skill=(int)get.invoke(v);
                String type=v.getClass().getSimpleName(); if(!seen.add(skill+":"+type))continue;
                for(var rc:v.getClass().getDeclaredFields()) {
                    if(Modifier.isStatic(rc.getModifiers()))continue;
                    final Method getter;
                    try { getter=v.getClass().getDeclaredMethod(rc.getName()); }
                    catch(NoSuchMethodException ignored) { continue; }
                    getter.setAccessible(true);
                    System.out.println("CATALOG\t"+skill+"\t"+type+"\t"+rc.getName()+"\t"+clean(getter.invoke(v)));
                }
            }
        }
        try(var z=new ZipFile(a[1])) {
            var c=new ClassNode();
            new ClassReader(z.getInputStream(z.getEntry("local/lito381/server/ClientSession.class"))).accept(c,0);
            for(var m:c.methods) {
                if(m.name.equals("useTeleportSpell")&&m.desc.equals("([B)V"))
                    System.out.println("SPECIAL\t5\tuseTeleportSpell\t"+m.desc);
                if(!m.name.startsWith("use"))continue;
                for(var t:org.objectweb.asm.Type.getArgumentTypes(m.desc)) {
                    String name=t.getInternalName();
                    if(name.startsWith("local/lito381/server/BattleSpellCatalog$"))
                        System.out.println("HANDLER\t"+name.substring(name.indexOf('$')+1)+"\t"+m.name+"\t"+m.desc);
                }
            }
        }
    }
}
