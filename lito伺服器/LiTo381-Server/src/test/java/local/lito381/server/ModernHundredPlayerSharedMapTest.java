package local.lito381.server;

import static org.junit.jupiter.api.Assertions.*;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Whole shared Lineage terrain; geometry/capacity proof is not 100 live clients. */
class ModernHundredPlayerSharedMapTest {
    static final List<Integer> IDS=List.of(508,531,534);
    @TempDir Path temporary;
    private record Facts(String manifest,String directions,int standing,List<Integer> islands,int x,int y) {}
    private static final Map<Integer,Facts> FACTS=Map.of(
        508,new Facts("d2970a8ea9c057fc0be096874866b230f80dfbd984d2abf265aa358eaf3ef392","785c9b8f503d5194626947f1f7bc6223dc5d72fe4a342a4ab685a25296239856",8097,List.of(8097),32832,33151),
        531,new Facts("f4f2b42676cb98cd9b1c95df19c03f7f5c3c69bc854e644d0f6f318bbdb728bf","1d67671c0671c6bac78dfcf44b00a389c65fa19160d861f5cd4ad744ab82807e",7001,List.of(6999,2),32799,32797),
        534,new Facts("314fd5c5b78d996e8bf2b6c3135bb63a29cb78d80c04fc96f77fd2a5bfbad2f4","8320a953597b90f5aa72bcf10e2bdbf6bc57ac5bcef9b89f83e4bead5f6d7daf",6364,List.of(6364),32817,32832));
    private static Properties worlds(Path root) { return ModernSharedDeathmatchPresetTest.worlds(root,IDS); }
    @Test void wholePhysicalManifestsDirectionsAndAnchorsMatchTheIndependentReadProbe()throws Exception {
        var mappings=ModernWorldConfiguration.parse(worlds(ModernDeathmatchSharedMapTest.sharedRoot())).load();
        assertEquals(3,mappings.size());
        for(int id:IDS) {
            var f=FACTS.get(id);var mapping=mappings.get(new ModernWorldBinding.Key(1,id));var map=mapping.binding().map();
            assertEquals(ModernDeathmatchMapReadProbe.sectors(id),map.manifest().sectors().stream().map(ModernWorldMap.SectorManifest::key).collect(java.util.stream.Collectors.toSet()));
            assertEquals(f.manifest(),map.digest());assertEquals(f.directions(),ModernDeathmatchSharedMapTest.directionDigest(map));
            assertEquals(f.standing(),map.teleportCells().size());
            var components=ModernDeathmatchSharedMapTest.components(map);
            assertEquals(f.islands(),components.stream().map(Set::size).toList());
            assertEquals(new ModernWorldMap.Cell(f.x(),f.y()),ModernDeathmatchSharedMapTest.centre(components.get(0)));
            assertEquals(new ModernWorldMap.Cell(f.x(),f.y()),mapping.anchor());
            assertEquals(100,mapping.playerCapacity());assertEquals(100,mapping.roomCapacity());
            assertEquals(100,mapping.withClockOverrides(new ModernWorldService.ClockOverrides(30000,0L,10000)).roomCapacity());
            assertEquals(100,mapping.withDisintegrate(false).roomCapacity());
            assertEquals(100,mapping.withRace(null).roomCapacity());
        }
    }
    @Test void all891AdvertisedRosterLayoutsHaveIndependentWalkingExits()throws Exception {
        int checked=0;
        for(int id:IDS) {
            var map=ModernWorldMap.load(ModernDeathmatchSharedMapTest.sharedRoot(),id,ModernDeathmatchMapReadProbe.sectors(id),()->false);
            var f=FACTS.get(id);var anchor=new ModernWorldMap.Cell(f.x(),f.y());
            for(int size=2;size<=100;size++)for(int humans:List.of(1,size/2,size)) {
                var roster=ModernWorldSpawnsTest.roster(humans,size-humans);
                var plan=ModernWorldSpawns.allocate(map,roster,List.of(anchor),Set.of(),()->false);
                ModernWorldSpawnsTest.assertPlan(map,roster,plan,Set.of());checked++;
            }
        }
        assertEquals(891,checked);
    }
    @Test void explicitPresetAndManualExpansionHaveIdenticalDurableIdentityWithoutChangingInputs()throws Exception {
        var p=worlds(temporary);var original=(Properties)p.clone();var expanded=ModernSharedDeathmatchPreset.expand(p);
        assertEquals(original,p);assertNotSame(p,expanded);
        assertArrayEquals(ModernSharedDeathmatchPresetTest.binding(p),ModernSharedDeathmatchPresetTest.binding(expanded));
        var reversed=ModernSharedDeathmatchPresetTest.worlds(temporary,List.of(534,531,508));
        assertArrayEquals(ModernSharedDeathmatchPresetTest.binding(p),ModernSharedDeathmatchPresetTest.binding(reversed));
        java.nio.file.Files.createDirectory(temporary.resolve("quest-results"));
        try(var config=ModernServerConfig.parse(ModernSiegeConfigurationTest.text(temporary,p).getBytes(java.nio.charset.StandardCharsets.UTF_8),n->"fixture-only");
            var manual=ModernServerConfig.parse(ModernSiegeConfigurationTest.text(temporary,expanded).getBytes(java.nio.charset.StandardCharsets.UTF_8),n->"fixture-only")) {
            assertArrayEquals(ModernSettlementCoordinator.binding(config),ModernSettlementCoordinator.binding(manual));
            assertTrue(config.worlds().completelyPinned());assertTrue(config.worlds().combatEnabled());
        }
        for(int id:IDS) {
            assertTrue(ModernWorldConfiguration.allowedKeys().contains(ModernSharedDeathmatchPreset.key(id)));
            assertFalse(expanded.containsKey(ModernSharedDeathmatchPreset.key(id)));
            var changed=(Properties)expanded.clone();changed.setProperty(ModernSharedDeathmatchPreset.prefix(id)+"roomCapacity","99");
            assertFalse(java.util.Arrays.equals(ModernSharedDeathmatchPresetTest.binding(p),ModernSharedDeathmatchPresetTest.binding(changed)));
        }
    }
    @Test void wrongPresetNamesMixedFieldsUnboundMapsAndInvalidCapacitiesFailClosed()throws Exception {
        var p=worlds(temporary);
        for(int id:IDS) {
            for(String name:List.of("",ModernSharedDeathmatchPreset.NAME,"SHARED_DEATHMATCH_LATEST")) {
                var q=(Properties)p.clone();q.setProperty(ModernSharedDeathmatchPreset.key(id),name);
                assertThrows(ModernServerConfig.ConfigException.class,()->ModernWorldConfiguration.parse(q));
            }
            for(String field:ModernSharedDeathmatchPreset.fields(id).keySet()) {
                var q=(Properties)p.clone();q.setProperty(ModernSharedDeathmatchPreset.prefix(id)+field,"unexpected");
                assertThrows(ModernServerConfig.ConfigException.class,()->ModernWorldConfiguration.parse(q));
            }
            for(String capacity:List.of("0","1","101","100.0","-1")) {
                var q=ModernSharedDeathmatchPreset.expand(p);q.setProperty(ModernSharedDeathmatchPreset.prefix(id)+"roomCapacity",capacity);
                assertThrows(ModernServerConfig.ConfigException.class,()->ModernWorldConfiguration.parse(q));
            }
        }
        var unbound=(Properties)p.clone();unbound.setProperty("world.bindings","1:508,1:531");
        assertThrows(ModernServerConfig.ConfigException.class,()->ModernWorldConfiguration.parse(unbound));
        var cancelled=ModernWorldConfiguration.parse(worlds(ModernDeathmatchSharedMapTest.sharedRoot()));
        assertThrows(java.io.IOException.class,()->cancelled.load(null,()->true));
    }
    @Test void loadedWholeMapsAdvertiseAndAcceptOneHundredWithoutChangingLegacyFiftyBound()throws Exception {
        var mappings=ModernWorldConfiguration.parse(ModernSharedDeathmatchPresetTest.worlds(ModernDeathmatchSharedMapTest.sharedRoot(),List.of(508,531,534,551,552))).load();
        var capacities=mappings.entrySet().stream().collect(java.util.stream.Collectors.toMap(Map.Entry::getKey,e->e.getValue().playerCapacity()));
        var rooms=new ModernRooms(mappings.keySet(),mappings.keySet(),capacities);var presence=new TournamentPresence();
        for(int id:IDS) {
            var owner=presence.connect();var scope=new ModernRooms.Scope(7,3333,1,0);
            rooms.enterLobby(owner,new TournamentPresence.Selection(new TournamentPresence.CharacterKey(7,3333),"source","player",80,1,0),1,0);
            try {
                var view=rooms.execute(owner,new ModernRooms.Command(scope,ModernRooms.Action.READ,0,0,0,null,"")).view();
                for(var option:view.options())if(option.mode()==1&&mappings.containsKey(new ModernWorldBinding.Key(1,option.mapId()))) {
                    assertTrue(option.worldAvailable());
                    assertEquals(ModernSharedDeathmatchPreset.FIFTY_PLAYER_MAPS.contains(option.mapId())?50:100,option.maximumPlayers());
                }
                ModernRoomsTest.accepted(rooms.execute(owner,new ModernRooms.Command(scope,ModernRooms.Action.CREATE,0,0,0,ModernRoomsTest.draft(100,1,id,0),"")));
            } finally {rooms.disconnect(owner);presence.disconnect(owner);}
        }
    }
}
