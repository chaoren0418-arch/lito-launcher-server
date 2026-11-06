package local.lito381.server;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;

/** Read-only qualification/export of existing maps; does not invent mode geometry. */
public final class ModernMapHandoffAuditMain {
    public static void main(String[] args)throws Exception {
        if(args.length!=2)throw new IllegalArgumentException("shared root, owned output directory required");
        Path shared=Path.of(args[0]).toRealPath(),out=Path.of(args[1]).toRealPath();
        var ids=ModernSharedDeathmatchPreset.MAPS.stream().sorted().toList();
        var p=ModernSharedDeathmatchPresetTest.worlds(shared,ids);
        for(int id:ids) {
            String prefix=ModernSharedDeathmatchPreset.prefix(id);
            p.remove(prefix+"botRules");p.setProperty(prefix+"attackMillis","1000");
            p.setProperty(prefix+"spellRules",ModernWorldConfiguration.DAMAGE_SPELL_RULESET);p.setProperty(prefix+"castMillis","700");
        }
        geometry(p,509,ModernMapRotationTestPeer.qualified509(shared),ModernMapRotationTestPeer.ANCHOR509);
        var map514=ModernWorldTestPeer.sharedMap(shared);ModernMapRotationTestPeer.qualify514(map514);
        geometry(p,514,map514,ModernWorldTestPeer.ANCHOR);
        p.setProperty("world.bindings",java.util.stream.Stream.concat(ids.stream(),java.util.stream.Stream.of(509,514)).sorted().map(id->"1:"+id).collect(Collectors.joining(",")));
        var expanded=ModernSharedDeathmatchPreset.expand(p);
        var loaded=ModernWorldConfiguration.parse(expanded).load();
        var bindings=new ArrayList<String>();
        for(var entry:loaded.entrySet().stream().sorted(Comparator.comparingInt(e->e.getKey().logicalMapId())).toList()) {
            var mapping=entry.getValue();var map=mapping.binding().map();
            var spawns=map.serverChosenSpawns(mapping.anchor(),mapping.playerCapacity());map.validateSpawns(spawns);
            bindings.add("{\"mode\":1,\"logical\":"+entry.getKey().logicalMapId()+",\"physical\":"+map.mapId()+",\"sectors\":"+map.manifest().sectors().size()+",\"manifest\":"+quote(map.digest())+",\"spawn\":["+mapping.anchor().x()+","+mapping.anchor().y()+"],\"qualified_capacity\":"+mapping.playerCapacity()+"}");
        }
        var text=new StringBuilder();
        for(String key:expanded.stringPropertyNames().stream().sorted().toList())text.append(key).append('=').append(expanded.getProperty(key)).append('\n');
        Files.writeString(out.resolve("qualified-legacy-dm-worlds.properties"),text,StandardCharsets.UTF_8);
        var natives=new ArrayList<String>();var births=BattleControlCatalog.defaultPlayerSpawns();
        for(var mode:BattleRules.Mode.values())if(mode.isBattleScene())for(int id:mode.maps()) {
            var rule=BattleRules.decode(new GameHub.RoomSettings("", "", 2, mode.wireId(), 0, 0, 0, 0),id,BattleControlCatalog.defaults());var arena=ArenaMapCatalog.forMap(id);
            var row=births.stream().filter(s->s.modeId()==mode.wireId()&&s.mapId()==id).toList();
            var nativeEntry=NativeLitoMapTableCatalog.forMap(id);
            natives.add("{\"mode\":"+mode.wireId()+",\"logical\":"+id+",\"native_name\":"+quote(nativeEntry.name())+",\"native_category\":"+quote(nativeEntry.category())+",\"native_sector_count\":"+arena.loadedSectors().size()+",\"native_builtin_rule_seconds\":"+rule.timeLimitSeconds()+",\"native_builtin_goal\":"+rule.goal()+",\"native_builtin_minimum_players\":"+rule.minimumPlayers()+",\"native_player_spawn_count\":"+row.size()+",\"native_spawns\":["+row.stream().map(s->"["+s.x()+","+s.y()+"]").collect(Collectors.joining(","))+"]}");
        }
        Files.writeString(out.resolve("map-qualification-java.json"),"{\"scope\":\"Existing explicit Modern shared Lineage DM bindings; Native LT-map metadata is a separate namespace, not aliases or Modern mode qualification\",\"qualified_modern_dm\":["+String.join(",",bindings)+"],\"native_builtin_catalog\":["+String.join(",",natives)+"]}",StandardCharsets.UTF_8);
        System.out.println("[map-audit] qualified existing DM="+loaded.size()+"; native catalog="+natives.size());
    }
    private static void geometry(Properties p,int id,ModernWorldMap map,ModernWorldMap.Cell anchor) {
        String prefix="world.1."+id+".";
        p.setProperty(prefix+"physicalMap",Integer.toString(map.mapId()));p.setProperty(prefix+"spawnX",Integer.toString(anchor.x()));p.setProperty(prefix+"spawnY",Integer.toString(anchor.y()));
        p.setProperty(prefix+"sectors",map.manifest().sectors().stream().map(s->String.format("%04x%04x",s.key().x(),s.key().y())).collect(Collectors.joining(",")));
        p.setProperty(prefix+"manifestSha256",map.digest());p.setProperty(prefix+"sectorPins",map.manifest().sectors().stream().map(s->String.format("%04x%04x",s.key().x(),s.key().y())+":"+s.byteLength()+":"+s.rawSha256()).collect(Collectors.joining(",")));
        p.setProperty(prefix+"roomCapacity","100");p.setProperty(prefix+"stepMillis","200");p.setProperty(prefix+"combatRules",ModernPhysicalCombat.RULESET);p.setProperty(prefix+"attackMillis","1000");p.setProperty(prefix+"spellRules",ModernWorldConfiguration.DAMAGE_SPELL_RULESET);p.setProperty(prefix+"castMillis","700");
    }
    private static String quote(String value){return "\""+value.replace("\\","\\\\").replace("\"","\\\"")+"\"";}
}
