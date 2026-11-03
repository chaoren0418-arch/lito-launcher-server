package local.lito381.server;

import static org.junit.jupiter.api.Assertions.*;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Properties;
import org.junit.jupiter.api.Test;

/** Complete shared504/505/506, never cropped to the selected battle component. */
class ModernFurtherLargeMapsTest {
    static final List<Integer> IDS=List.of(504,505,506);
    private record Facts(String canonical,String directions,int standing,List<Integer> components,int sectors) {}
    private static final Map<Integer,Facts> FACTS=Map.of(
        504,new Facts("ae157073502ac84560f17cee7d60546e86151a9189f139dacfc52dae9ab4cb73","6d45817493a489c8f157ba0e75fc520396657847c0ceccf4a61221c52970ae80",29456,List.of(9596,6971,4052,2486,2240,1012,874,687,687,414,162,131,112,12,4,3,3,2,2,2,2,2),31),
        505,new Facts("44867dd7fe08a479ae5fc0de7a951a157b6336d5493e40aa7d423beeedc4ad0f","41de4510d162f60ca1eecc52b0d5e2b9c144f1b77b4849180f8e248a29c21e64",30455,List.of(9596,7173,6971,2486,2240,1012,687,162,112,4,3,3,2,2,2),31),
        506,new Facts("8b072b684306f964c5f8692c5c2f8ec4879271a5fdd51e048cb4b144ac8de49a","813f5fcb62823a86b12a5ce0107e2bae5ee94a579f50ade9e40a316b115e5121",27000,List.of(9596,6971,2486,2221,1794,1048,1012,874,687,162,112,12,9,4,3,3,2,2,2),35));
    static Properties worlds(){return ModernSharedDeathmatchPresetTest.worlds(ModernDeathmatchSharedMapTest.sharedRoot(),IDS);}
    @Test void completeGeometryPinsComponentsAndCapacityMatchBothIndependentDecoders()throws Exception {
        var maps=ModernWorldConfiguration.parse(worlds()).load();assertEquals(3,maps.size());
        for(int id:IDS) {
            var f=FACTS.get(id);var mapping=maps.get(new ModernWorldBinding.Key(1,id));var map=mapping.binding().map();
            assertEquals(ModernDeathmatchMapReadProbe.sectors(id),map.manifest().sectors().stream().map(ModernWorldMap.SectorManifest::key).collect(java.util.stream.Collectors.toSet()));
            assertEquals(f.sectors(),map.manifest().sectors().size());assertEquals(f.canonical(),map.digest());
            assertEquals(f.directions(),ModernDeathmatchSharedMapTest.directionDigest(map));assertEquals(f.standing(),map.teleportCells().size());
            var groups=ModernDeathmatchSharedMapTest.components(map);
            assertEquals(f.components(),groups.stream().map(Set::size).toList());
            assertEquals(new ModernWorldMap.Cell(32933,32862),ModernDeathmatchSharedMapTest.centre(groups.get(0)));
            assertEquals(new ModernWorldMap.Cell(32933,32862),mapping.anchor());
            assertEquals(100,mapping.playerCapacity());assertEquals(100,mapping.roomCapacity());assertTrue(mapping.combatEnabled());assertTrue(mapping.serverBots());
        }
    }
    @Test void all891RosterAllocationsHaveDistinctPositionsAndIndependentWalkingExits()throws Exception {
        var root=ModernDeathmatchSharedMapTest.sharedRoot(); // Preserve opt-in skip on the test thread.
        var pool=java.util.concurrent.Executors.newFixedThreadPool(3);
        try {
            var jobs=IDS.stream().map(id->(java.util.concurrent.Callable<Integer>)()->{
            int checked=0;
            var map=ModernWorldMap.load(root,id,ModernDeathmatchMapReadProbe.sectors(id),()->false);
            for(int count=2;count<=100;count++)for(int humans:List.of(1,count/2,count)) {
                var roster=ModernWorldSpawnsTest.roster(humans,count-humans);
                var plan=ModernWorldSpawns.allocate(map,roster,List.of(new ModernWorldMap.Cell(32933,32862)),Set.of(),()->false);
                ModernWorldSpawnsTest.assertPlan(map,roster,plan,Set.of());checked++;
            }
            return checked;
            }).toList();
            int checked=0;for(var result:pool.invokeAll(jobs))checked+=result.get();
            assertEquals(891,checked);
        } finally {
            pool.shutdown();
            if(!pool.awaitTermination(5,java.util.concurrent.TimeUnit.SECONDS)) {
                pool.shutdownNow();assertTrue(pool.awaitTermination(5,java.util.concurrent.TimeUnit.SECONDS));
            }
        }
    }
    @Test void presetManualAndOrderHaveTheSameDurableBindingButWrongPinsFailClosed()throws Exception {
        var p=worlds();var before=(Properties)p.clone();var expanded=ModernSharedDeathmatchPreset.expand(p);
        assertEquals(before,p);assertNotSame(p,expanded);
        assertArrayEquals(ModernSharedDeathmatchPresetTest.binding(p),ModernSharedDeathmatchPresetTest.binding(expanded));
        var reverse=ModernSharedDeathmatchPresetTest.worlds(ModernDeathmatchSharedMapTest.sharedRoot(),List.of(506,505,504));
        assertArrayEquals(ModernSharedDeathmatchPresetTest.binding(p),ModernSharedDeathmatchPresetTest.binding(reverse));
        assertTrue(ModernWorldConfiguration.parse(p).completelyPinned());
        assertThrows(java.io.IOException.class,()->ModernWorldConfiguration.parse(p).load(null,()->true));
        for(int id:IDS) {
            assertEquals(ModernSharedDeathmatchPreset.HUNDRED_NAME,ModernSharedDeathmatchPreset.name(id));
            var wrong=(Properties)expanded.clone();wrong.setProperty("world.1."+id+".manifestSha256","a".repeat(64));
            assertThrows(java.io.IOException.class,()->ModernWorldConfiguration.parse(wrong).load());
            var mixed=(Properties)p.clone();mixed.setProperty("world.1."+id+".spawnX","32933");
            assertThrows(ModernServerConfig.ConfigException.class,()->ModernWorldConfiguration.parse(mixed));
            var legacy=(Properties)p.clone();legacy.setProperty(ModernSharedDeathmatchPreset.key(id),ModernSharedDeathmatchPreset.NAME);
            assertThrows(ModernServerConfig.ConfigException.class,()->ModernWorldConfiguration.parse(legacy));
        }
    }
    @Test void everyNewRoomRequiresAnOptedInOwnerAndCanStillReturnToTheLobby()throws Exception {
        var maps=ModernWorldConfiguration.parse(worlds()).load();
        var rooms=new ModernRooms(maps.keySet(),maps.keySet(),maps.entrySet().stream().collect(java.util.stream.Collectors.toMap(Map.Entry::getKey,e->e.getValue().playerCapacity())));
        rooms.configureLargeWorlds(maps.keySet());var presence=new TournamentPresence();
        for(int id:IDS) {
            var owner=presence.connect();var scope=new ModernRooms.Scope(7,3333,1,0);
            rooms.enterLobby(owner,new TournamentPresence.Selection(new TournamentPresence.CharacterKey(7,3333),"source","player",80,1,0),1,0);
            try {
                var create=new ModernRooms.Command(scope,ModernRooms.Action.CREATE,0,0,0,ModernRoomsTest.draft(100,1,id,0),"");
                assertEquals(ModernRooms.Status.DENIED,rooms.execute(owner,create).status());
                rooms.enableLargeMaps(owner,scope);var created=rooms.execute(owner,create);ModernRoomsTest.accepted(created);
                assertEquals(100,created.view().joined().summary().config().capacity());
                assertTrue(created.view().options().stream().filter(o->o.mode()==1&&o.mapId()==id).findFirst().orElseThrow().worldAvailable());
                ModernRoomsTest.accepted(rooms.execute(owner,new ModernRooms.Command(scope,ModernRooms.Action.LEAVE,created.view().joined().summary().id(),created.view().joined().summary().revision(),0,null,"")));
            }finally{rooms.disconnect(owner);presence.disconnect(owner);}
        }
        assertEquals(0,rooms.memberCount());
    }
}
