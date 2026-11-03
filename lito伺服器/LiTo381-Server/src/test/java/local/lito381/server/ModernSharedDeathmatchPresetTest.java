package local.lito381.server;

import static org.junit.jupiter.api.Assertions.*;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.nio.file.Path;
import java.util.Map;
import java.util.Properties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ModernSharedDeathmatchPresetTest {
    @TempDir Path temporary;
    static Properties worlds(Path root) {
        return worlds(root,java.util.List.of(551,552));
    }
    static Properties worlds(Path root,java.util.List<Integer> ids) {
        var p=new Properties();p.setProperty("world.sharedRoot",root.toAbsolutePath().toString().replace('\\','/'));
        p.setProperty("world.bindings",ids.stream().map(id->"1:"+id).collect(java.util.stream.Collectors.joining(",")));
        for(int id:ids){String prefix=ModernSharedDeathmatchPreset.prefix(id);
            p.setProperty(ModernSharedDeathmatchPreset.key(id),ModernSharedDeathmatchPreset.name(id));
            p.setProperty(prefix+"stepMillis","200");p.setProperty(prefix+"attackMillis","50");
            p.setProperty(prefix+"combatRules",ModernPhysicalCombat.RULESET);p.setProperty(prefix+"botRules",ModernWorldConfiguration.BOT_RULESET);
        }return p;
    }
    static byte[] binding(Properties p)throws Exception {
        var b=new ByteArrayOutputStream();try(var out=new DataOutputStream(b)){ModernWorldConfiguration.parse(p).writeRoomCapacityBinding(out);}return b.toByteArray();
    }
    @Test void explicitImmutablePresetAndManualExpansionHaveIdenticalQualificationAndRouting()throws Exception {
        var empty=new Properties();assertSame(empty,ModernSharedDeathmatchPreset.expand(empty));assertEquals(0,binding(empty).length);
        var p=worlds(temporary);var old=(Properties)p.clone();var q=ModernSharedDeathmatchPreset.expand(p);
        assertEquals(old,p);assertNotSame(p,q);assertArrayEquals(binding(p),binding(q));assertTrue(binding(p).length>0);
        assertTrue(ModernWorldConfiguration.parse(p).completelyPinned());assertTrue(ModernWorldConfiguration.parse(p).combatEnabled());
        for(int id:ModernSharedDeathmatchPreset.FIFTY_PLAYER_MAPS){assertFalse(q.containsKey(ModernSharedDeathmatchPreset.key(id)));assertTrue(ModernWorldConfiguration.allowedKeys().contains(ModernSharedDeathmatchPreset.key(id)));}
        var reordered=(Properties)p.clone();reordered.setProperty("world.bindings","1:552,1:551");assertArrayEquals(binding(p),binding(reordered));
        for(String field: new String[]{"roomCapacity","spawnX","manifestSha256"}) {
            var changed=(Properties)q.clone();String key="world.1.551."+field;
            changed.setProperty(key,field.equals("roomCapacity")?"40":field.equals("spawnX")?"32730":"a".repeat(64));
            assertFalse(java.util.Arrays.equals(binding(p),binding(changed)));
        }
    }
    @Test void fullServerConfigurationAcceptsTheOptInAndRejectsAmbiguityBeforeSecrets()throws Exception {
        java.nio.file.Files.createDirectory(temporary.resolve("quest-results"));
        var p=worlds(temporary);
        String text=ModernSiegeConfigurationTest.text(temporary,p);
        try(var config=ModernServerConfig.parse(text.getBytes(java.nio.charset.StandardCharsets.UTF_8),n->"fixture-only")) {
            assertTrue(config.worlds().completelyPinned());assertTrue(config.worlds().combatEnabled());
            var expanded=ModernSharedDeathmatchPreset.expand(p);
            try(var manual=ModernServerConfig.parse(ModernSiegeConfigurationTest.text(temporary,expanded).getBytes(java.nio.charset.StandardCharsets.UTF_8),n->"fixture-only")) {
                assertArrayEquals(ModernSettlementCoordinator.binding(config),ModernSettlementCoordinator.binding(manual));
            }
            expanded.setProperty("world.1.551.roomCapacity","40");
            try(var changed=ModernServerConfig.parse(ModernSiegeConfigurationTest.text(temporary,expanded).getBytes(java.nio.charset.StandardCharsets.UTF_8),n->"fixture-only")) {
                assertFalse(java.util.Arrays.equals(ModernSettlementCoordinator.binding(config),ModernSettlementCoordinator.binding(changed)));
            }
        }
        p.setProperty("world.1.551.roomCapacity","100");
        assertThrows(ModernServerConfig.ConfigException.class,()->ModernServerConfig.parse(
                ModernSiegeConfigurationTest.text(temporary,p).getBytes(java.nio.charset.StandardCharsets.UTF_8),n->{fail("reject mixed preset before secret lookup");return "unused";}));
    }
    @Test void mixedUnknownUnboundUnpinnedAndWalkOnlyPresetsFailClosed()throws Exception {
        var p=worlds(temporary);
        for(int id:ModernSharedDeathmatchPreset.FIFTY_PLAYER_MAPS) for(String field:ModernSharedDeathmatchPreset.fields(id).keySet()) {
            var q=(Properties)p.clone();q.setProperty(ModernSharedDeathmatchPreset.prefix(id)+field,"unexpected");
            assertThrows(ModernServerConfig.ConfigException.class,()->ModernWorldConfiguration.parse(q));
        }
        for(String name:new String[]{"","SHARED_DEATHMATCH_LATEST","SHARED_DEATHMATCH_100_V1"}) {
            var q=(Properties)p.clone();q.setProperty(ModernSharedDeathmatchPreset.key(551),name);assertThrows(ModernServerConfig.ConfigException.class,()->ModernWorldConfiguration.parse(q));
        }
        for(String capacity:new String[]{"0","1","101","50.0","-1"}) {
            var q=ModernSharedDeathmatchPreset.expand(p);q.setProperty("world.1.551.roomCapacity",capacity);assertThrows(ModernServerConfig.ConfigException.class,()->ModernWorldConfiguration.parse(q));
        }
        var unused=(Properties)p.clone();unused.setProperty("world.bindings","1:551");assertThrows(ModernServerConfig.ConfigException.class,()->ModernWorldConfiguration.parse(unused));
        var unpinned=ModernSharedDeathmatchPreset.expand(p);unpinned.remove("world.1.551.manifestSha256");unpinned.remove("world.1.551.sectorPins");assertThrows(ModernServerConfig.ConfigException.class,()->ModernWorldConfiguration.parse(unpinned));
        var scene=(Properties)p.clone();scene.remove("world.1.551.combatRules");scene.remove("world.1.551.attackMillis");scene.remove("world.1.551.botRules");assertThrows(ModernServerConfig.ConfigException.class,()->ModernWorldConfiguration.parse(scene));
    }
    @Test void actualSharedMapsLoadAndCapacitySurvivesMappingCopiesAndCannotBeRaisedToOneHundred()throws Exception {
        var p=worlds(ModernDeathmatchSharedMapTest.sharedRoot());var mappings=ModernWorldConfiguration.parse(p).load();assertEquals(2,mappings.size());
        for(var mapping:mappings.values()) {
            assertEquals(50,mapping.roomCapacity());assertEquals(50,mapping.playerCapacity());assertTrue(mapping.combatEnabled());assertTrue(mapping.serverBots());
            assertEquals(50,mapping.withClockOverrides(new ModernWorldService.ClockOverrides(30000,0L,10000)).roomCapacity());
            assertEquals(50,mapping.withDisintegrate(false).roomCapacity());assertEquals(50,mapping.withRace(null).roomCapacity());
        }
        assertFalse(mappings.get(new ModernWorldBinding.Key(1,551)).binding().map().standing(ModernWorldTestPeer.ANCHOR),
                "old map514 fixture anchor cannot silently qualify shared551");
        var high=ModernSharedDeathmatchPreset.expand(p);high.setProperty("world.1.551.roomCapacity","100");
        // An explicit 100 is also qualified, not silently treated as an absent override.
        assertThrows(java.io.IOException.class,()->ModernWorldConfiguration.parse(high).load());
        assertThrows(java.io.IOException.class,()->ModernWorldConfiguration.parse(p).load(null,()->true));
    }
    @Test void roomOptionsCreateAndReconfigureEnforceTheLoadedBoundWithoutChangingOtherMaps()throws Exception {
        var mappings=ModernWorldConfiguration.parse(worlds(ModernDeathmatchSharedMapTest.sharedRoot())).load();
        var capacities=mappings.entrySet().stream().collect(java.util.stream.Collectors.toMap(Map.Entry::getKey,e->e.getValue().playerCapacity()));
        var rooms=new ModernRooms(mappings.keySet(),mappings.keySet(),capacities);
        var presence=new TournamentPresence();var owner=presence.connect();var scope=new ModernRooms.Scope(7,3333,1,0);
        rooms.enterLobby(owner,new TournamentPresence.Selection(new TournamentPresence.CharacterKey(7,3333),"source","player",80,1,0),1,0);
        java.util.function.Supplier<ModernRooms.View> view=()->rooms.execute(owner,new ModernRooms.Command(scope,ModernRooms.Action.READ,0,0,0,null,"")).view();
        try {
            var before=view.get();
            for(var o:before.options())assertEquals(o.mode()==1&&ModernSharedDeathmatchPreset.FIFTY_PLAYER_MAPS.contains(o.mapId())?50:100,o.maximumPlayers());
            var denied=rooms.execute(owner,new ModernRooms.Command(scope,ModernRooms.Action.CREATE,0,0,0,ModernRoomsTest.draft(51,1,551,0),""));
            assertEquals(ModernRooms.Reason.INVALID_CONFIGURATION,denied.reason());assertEquals(before,view.get());
            ModernRoomsTest.accepted(rooms.execute(owner,new ModernRooms.Command(scope,ModernRooms.Action.CREATE,0,0,0,ModernRoomsTest.draft(50,1,551,0),"")));
            before=view.get();var s=before.joined().summary();
            denied=rooms.execute(owner,new ModernRooms.Command(scope,ModernRooms.Action.CONFIGURE,s.id(),s.revision(),0,ModernRoomsTest.draft(100,1,552,0),""));
            assertEquals(ModernRooms.Reason.INVALID_CONFIGURATION,denied.reason());assertEquals(before,view.get());
            ModernRoomsTest.accepted(rooms.execute(owner,new ModernRooms.Command(scope,ModernRooms.Action.CONFIGURE,s.id(),s.revision(),0,ModernRoomsTest.draft(50,1,552,0),"")));
        } finally{rooms.disconnect(owner);presence.disconnect(owner);}
    }
    @Test void preparationIndependentlyRejectsAnOversizedRoomEvenIfAdmissionAdapterOmitsTheBound()throws Exception {
        var mappings=ModernWorldConfiguration.parse(worlds(ModernDeathmatchSharedMapTest.sharedRoot())).load();
        var f=new ModernRoomsTest.Fixture();var first=f.player();var second=f.player();
        ModernRoomsTest.accepted(f.create(first,ModernRoomsTest.draft(51,1,551,0)));
        ModernRoomsTest.accepted(f.join(second,f.view(first).joined().summary(),""));
        ModernRoomsTest.accepted(f.act(first,ModernRooms.Action.READY,1));ModernRoomsTest.accepted(f.act(second,ModernRooms.Action.READY,1));
        var limits=new ModernWorldService.Limits(1,1,0,1000,10,new ModernBattleEngine.Limits(1000,0,1000));
        var service=new ModernWorldService(f.rooms,mappings,limits);
        try {
            for(var player:java.util.List.of(first,second))service.register(player.owner(),player.scope(),d->{fail("over-capacity must precede source reads");return null;});
            var room=f.view(first).joined().summary();
            var reply=service.handle(first.owner(),new ModernWorldProtocol.Request(first.scope(),ModernWorldProtocol.Action.PREPARE,0,room.id(),room.revision(),0,0,0,0,0,"0".repeat(64)));
            assertEquals(ModernWorldProtocol.Status.UNAVAILABLE,reply.status());assertEquals(ModernWorldProtocol.Reason.CAPACITY,reply.reason());assertNull(reply.frame());
        } finally{service.close();assertTrue(service.awaitTermination(4000));f.rooms.disconnect(first.owner());f.rooms.disconnect(second.owner());f.presence.disconnect(first.owner());f.presence.disconnect(second.owner());}
    }
}
