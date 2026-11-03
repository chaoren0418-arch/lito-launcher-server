package local.lito381.server;

import static org.junit.jupiter.api.Assertions.*;
import static local.lito381.server.ModernBattleWire.*;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Real engine publications; synthetic terrain, no gameplay negotiation. */
class ModernNpcMotionWireTest {
    static void assertPayload43(byte[] frame,ModernBattleAudience.View view) {
        var expected=ModernBattleWire.encodeNpcMotion(ModernNpcMotion.Payload.selected(view));
        assertArrayEquals(expected,Arrays.copyOfRange(frame,frame.length-expected.length,frame.length));
        assertEquals(frame.length,ByteBuffer.wrap(frame).getInt(4));
    }
    @FunctionalInterface interface Sink { void accept(String label, ModernNpcMotion.Payload payload) throws Exception; }
    static void pair(Sink sink,String label,ModernBattleEngineNpcTest.Fixture f)throws Exception {
        var p=ModernBattleWireTest.publication(f.engine.serviceObservation(),f.config.catalog());
        for(int i=0;i<2;i++)sink.accept(label+"-"+i,ModernNpcMotion.Payload.select(p,f.players.get(i).scope()));
    }
    static void trace(Sink sink)throws Exception {
        try(var f=new ModernBattleEngineNpcTest.Fixture()) {
            pair(sink,"loading",f);f.active();pair(sink,"active",f);
            var n=f.spawn(0,10,20);pair(sink,"spawn",f);
            for(long millis:new long[]{100,200,10_000}) {
                f.elapsed(millis);assertEquals(ModernNpcBattle.Reason.MOVED,f.step(n).reason());pair(sink,"walk"+millis,f);
            }
            pair(sink,"repeat",f);
            f.rooms.releasePreparation(f.ticket);pair(sink,"abort",f);
        }
        try(var f=new ModernBattleEngineNpcTest.Fixture()) {
            f.active();var t=f.config.catalog().npcs().get(ModernNpcMeleeTest.NPC).rosterTemplate();
            var rows=new ArrayList<ModernNpcRoster.Spawn>();
            for(int i=0;i<128;i++)rows.add(new ModernNpcRoster.Spawn(t,0,1,ModernBattleEngineNpcTest.cell(40+i%16,40+i/16),0));
            f.engine.spawnNpcs(f.scope,rows);pair(sink,"full",f);
        }
        try(var f=new ModernNpcServiceTest.Fixture(100,true,true,new ModernQuestServiceTest.Journal(),513)) {
            f.active();ModernBattlePublicationTest.stopScheduler(f.service);
            var p=ModernBattleAudienceTest.published(f.service,f.a.owner());
            sink.accept("isolated-0",ModernNpcMotion.Payload.select(p,f.a.scope()));
            sink.accept("isolated-1",ModernNpcMotion.Payload.select(p,f.b.scope()));
        }
        try(var f=ModernBattleGreaterHasteTest.fixture()) {
            ModernBattleGreaterHasteTest.active(f);
            var p=ModernGreaterHasteWireTest.publication(ModernBattleGreaterHasteTest.cast(f,0,1).observation());
            for(int i=0;i<2;i++)sink.accept("haste-"+i,ModernNpcMotion.Payload.select(p,f.players.get(i).scope()));
        }
    }
    static byte[] verified(ModernNpcMotion.Payload p) {
        var encoded=encodeNpcMotion(p);var v=p.audience();
        int base=v.world().descriptor().has(ModernMatchDescriptor.GREATER_HASTE)?3:2;
        var old=encode(v,base);assertArrayEquals(old,encode(v,base));
        // Every existing section (including its framing) is byte-identical.
        assertArrayEquals(Arrays.copyOfRange(old,16,old.length),Arrays.copyOfRange(encoded,16,old.length));
        var b=ByteBuffer.wrap(encoded);assertEquals(MAGIC,b.getInt());assertEquals(4,b.getShort());assertEquals(0,b.getShort());
        assertEquals(encoded.length,b.getInt());assertEquals(14,b.getShort());assertEquals(0,b.getShort());b.position(old.length);
        assertEquals(14,b.getShort());assertEquals(0,b.getShort());int size=b.getInt();
        assertEquals(encoded.length-b.position(),size);assertTrue(size<=NPC_MOTION_LIMIT);
        var s=p.state();assertEquals(s==null?0:1,b.get());
        if(s!=null) {
            assertEquals(s.scope().roomId(),b.getLong());assertEquals(s.scope().matchId(),b.getLong());assertEquals(s.scope().generation(),b.getLong());
            assertEquals(s.revision(),b.getLong());assertEquals(s.elapsedMillis(),b.getLong());assertEquals(s.actors().size(),b.getInt());
            assertEquals(v.world().npcs().actors().stream().map(ModernBattleAudience.Npc::serial).toList(),s.actors().stream().map(a->a.actor().serial()).toList());
            for(var a:s.actors()) {
                assertEquals(a.actor().serial(),b.getLong());assertEquals(a.life(),b.getLong());assertEquals(a.pace().walkMillis(),b.getInt());assertEquals(a.pace().attackMillis(),b.getInt());
                var w=a.walk();assertEquals(w==null?0:1,b.get());
                if(w!=null) {
                    assertEquals(w.sequence(),b.getLong());assertEquals(w.life(),b.getLong());assertEquals(w.rosterRevision(),b.getLong());assertEquals(w.elapsedMillis(),b.getLong());assertEquals(w.durationMillis(),b.getInt());
                    assertEquals(w.from().x(),b.getInt());assertEquals(w.from().y(),b.getInt());assertEquals(w.to().x(),b.getInt());assertEquals(w.to().y(),b.getInt());assertEquals(w.heading(),b.getInt());
                }
            }
        }
        assertFalse(b.hasRemaining());assertArrayEquals(encoded,encodeNpcMotion(p));
        assertThrows(IllegalArgumentException.class,()->encode(v,4));return encoded;
    }
    /** Engine ticks are 100ms; only every 200ms publication reaches the reader.
     * Two durable players and two real NPCs, synthetic terrain, no SQL/TLS claim. */
    static void presentationTrace(Sink sink)throws Exception {
        try(var f=new ModernBattleEngineNpcTest.Fixture(2,0,false,10_000,0,
                java.util.function.UnaryOperator.identity(),ModernNpcMeleeTest.catalog(),true)) {
            // Both lane0 NPCs pursue the lane0 participant at (10,10), not
            // the closer lane1 participant. Use two equally distant origins.
            f.active();var a=f.spawn(0,10,20);var b=f.spawn(0,0,10);
            pair(sink,"cadence0",f);
            for(int tick=1;tick<=24;tick++) {
                f.elapsed(tick*100L);
                for(var n:List.of(a,b)) {
                    var reason=f.step(n).reason();
                    var expected=tick<=9?ModernNpcBattle.Reason.MOVED:(tick==10||tick==18)?ModernNpcBattle.Reason.ATTACKED:ModernNpcBattle.Reason.TOO_FAST;
                    assertEquals(expected,reason,"NPC cadence tick "+tick);
                }
                if(tick%2==0)pair(sink,"cadence"+(tick*100),f);
            }
        }
    }
    @Test void twoNpcPresentationKeepsTheLastConfirmedStepAtTheActualReaderCadence()throws Exception {
        var all=new ArrayList<ModernNpcMotion.Payload>();
        presentationTrace((label,p)->{verified(p);all.add(p);});
        assertEquals(26,all.size());
        for(var p:all)assertEquals(2,p.state().actors().size());
        for(int pair=1;pair<=4;pair++)for(var actor:all.get(pair*2).state().actors()) {
            assertEquals(pair*2,actor.walk().sequence());
            assertEquals(pair*200,actor.walk().elapsedMillis());
            assertEquals(100,actor.pace().walkMillis());assertEquals(750,actor.pace().attackMillis());
        }
        assertTrue(all.get(10).audience().world().npcs().actors().stream().allMatch(n->n.action()==ModernNpcRoster.Action.ATTACK));
        assertTrue(all.get(18).audience().world().npcs().actors().stream().allMatch(n->n.action()==ModernNpcRoster.Action.ATTACK));
    }
    @Test @org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable(named="LITO_SHARED_RESOURCE_ROOT",matches=".+")
    void sharedCadenceFixtureUsesOnlyTheConfiguredServiceSearchBound()throws Exception {
        var map=ModernQuestMapQualificationTest.shared();
        var cells=map.serverChosenSpawns(ModernQuestMapQualificationTest.ANCHOR,100);
        var path=ModernNpcServiceTestPeer.qualifyCadencePath(map,ModernQuestProgressTest.encounterCatalog(),cells);
        assertEquals(73,path.playerIndex());assertEquals(9,path.moves());assertTrue(path.maximumVisited()<=64);
        System.out.println("shared554 cadence fixture moves="+path.moves()+" maxVisited="+path.maximumVisited()+" playerIndex="+path.playerIndex());
    }
    @Test @org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable(named="LITO_SHARED_RESOURCE_ROOT",matches=".+")
    void sharedDistantFixtureMustResumeAtOneNodeWithoutSelectingANearerSpawn()throws Exception {
        var map=ModernQuestMapQualificationTest.shared();
        var cells=map.serverChosenSpawns(ModernQuestMapQualificationTest.ANCHOR,100);
        var path=ModernNpcServiceTestPeer.qualifyPursuitPath(map,ModernQuestProgressTest.encounterCatalog(),cells);
        assertEquals(99,path.playerIndex());assertEquals(12,path.moves());
        assertEquals(1,path.maximumVisited());assertTrue(path.budgetYields()>0);
        System.out.println("shared554 distant pursuit moves="+path.moves()+" maxVisited="+path.maximumVisited()+" yields="+path.budgetYields()+" playerIndex="+path.playerIndex());
    }
    @Test void realMotionEmptyClosedSparseMaximumAndHastePublicationsPreserveLegacyBytes()throws Exception {
        var all=new ArrayList<ModernNpcMotion.Payload>();
        trace((label,p)->{verified(p);all.add(p);});assertEquals(22,all.size());
        assertEquals(3,all.get(10).state().actors().get(0).walk().sequence());
        assertEquals(128,all.get(16).state().actors().size());
        assertEquals(0,all.get(18).audience().world().lane());assertEquals(1,all.get(19).audience().world().lane());
        assertFalse(all.get(18).state().actors().isEmpty());assertFalse(all.get(19).state().actors().isEmpty());
        assertTrue(all.get(18).state().actors().stream().noneMatch(a->all.get(19).state().actors().stream().anyMatch(b->a.actor().equals(b.actor()))));
        assertNull(all.get(20).state());assertNull(all.get(21).state());
    }
    @Test void exactRegistrationAndImmutablePublicationCannotBeReboundToAnotherCapture()throws Exception {
        try(var f=new ModernBattleEngineNpcTest.Fixture()) {
            f.active();var n=f.spawn(0,10,20);f.elapsed(100);f.step(n);
            var pub=ModernBattleWireTest.publication(f.engine.serviceObservation(),f.config.catalog());
            var scope=f.players.get(0).scope();var p=ModernNpcMotion.Payload.select(pub,scope);var before=verified(p);
            assertSame(p.state(),pub.audience(f.players.get(1).scope()).npcMotion(),"one immutable motion projection per visible lane");
            assertThrows(SecurityException.class,()->ModernNpcMotion.Payload.select(pub,new ModernRooms.Scope(scope.sourceId(),scope.objectId(),scope.epoch()+1,scope.channelId())));
            assertThrows(UnsupportedOperationException.class,()->p.state().actors().clear());
            assertTrue(Arrays.stream(ModernNpcMotion.Payload.class.getDeclaredConstructors()).allMatch(c->java.lang.reflect.Modifier.isPrivate(c.getModifiers())));
            assertEquals(List.of(ModernBattleAudience.View.class,ModernNpcMotion.State.class),Arrays.stream(ModernNpcMotion.Payload.class.getDeclaredFields()).map(java.lang.reflect.Field::getType).toList());
            f.elapsed(200);f.step(n);assertArrayEquals(before,verified(p));
            assertEquals(1,p.state().actors().get(0).walk().sequence());assertEquals("NpcMotionPayload[redacted]",p.toString());
            assertEquals(47,ModernSessionProtocol.MAX_VERSION);
        }
    }
    @Test void v43ReadUsesTheSameBoundPayloadAndOlderCanonicalVersionsKeepTheirSections()throws Exception {
        trace((label,p)->{
            var v=p.audience();
            var q=new ModernBattleReadProtocol.Request(v.personal().scope(),v.world().identity());
            var r=new ModernApplicationProtocol.BattleReadResult(91,q,new ModernBattleReadProtocol.Reply(ModernBattleReadProtocol.State.DATA,v));
            var full=ModernApplicationProtocol.encode(r,43);assertPayload43(full,v);
            assertEquals(4,ByteBuffer.wrap(full).getShort(76));
            assertEquals(61,ByteBuffer.wrap(full).getShort(12));
            int old=v.world().descriptor().has(ModernMatchDescriptor.GREATER_HASTE)?42:35;
            assertArrayEquals(encode(v,old==42?3:1),Arrays.copyOfRange(ModernApplicationProtocol.encode(r,old),72,ModernApplicationProtocol.encode(r,old).length));
            assertArrayEquals(ModernBattleWire.encodeVersioned(v,43),ModernBattleWire.encodeVersioned(v,44));
            assertArrayEquals(ModernBattleWire.encodeVersioned(v,43),ModernBattleWire.encodeVersioned(v,45));
            assertThrows(IllegalArgumentException.class,()->ModernBattleWire.encodeVersioned(v,ModernSessionProtocol.MAX_VERSION+1));
        });
    }
}
