package local.lito381.server;

import static org.junit.jupiter.api.Assertions.*;
import static local.lito381.server.ModernBattleHasteTest.*;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import org.junit.jupiter.api.Test;

/** Actual checked publication -> schema5; synthetic terrain, no negotiation. */
class ModernHasteWireTest {
    @FunctionalInterface interface Sink {void accept(String label,ModernBattleAudience.View view)throws Exception;}
    static void pair(Sink sink,String label,ModernBattlePublication p)throws Exception {
        for(var a:p.capture().actors())if(a.scope()!=null)sink.accept(label+"-"+a.pose().entityId(),p.audience(a.scope()));
    }
    static ModernBattlePublication mixed(ModernBattleObservation.Capture c) {
        var l=c.limits();return new ModernBattlePublication(c,new ModernMatchDescriptor(ModernMatchDescriptor.Ruleset.PVP_SKIRMISH,
                63|ModernMatchDescriptor.GREATER_HASTE|ModernMatchDescriptor.HASTE,2000,l.loadTimeoutMillis(),l.countdownNanos(),l.activeTimeoutMillis()),null,null);
    }
    static ModernBattlePublication elimination(ModernBattleObservation.Capture c) {
        var l=c.limits();return new ModernBattlePublication(c,new ModernMatchDescriptor(ModernMatchDescriptor.Ruleset.DEATHMATCH_ELIMINATION,
                63|ModernMatchDescriptor.HASTE,2000,l.loadTimeoutMillis(),l.countdownNanos(),l.activeTimeoutMillis()),null,null);
    }
    static void trace(Sink sink)throws Exception {
        try(var f=fixture()) {
            pair(sink,"pc-loading",ModernHasteEffectsTest.publish(f.engine.serviceObservation()));
            pair(sink,"pc-active",ModernHasteEffectsTest.publish(active(f)));
            pair(sink,"pc-target",ModernHasteEffectsTest.publish(cast(f,0,1,1).observation()));
            f.elapsed(2_000_000_000L);pair(sink,"pc-other",ModernHasteEffectsTest.publish(cast(f,0,2,0).observation()));
            pair(sink,"pc-refresh",ModernHasteEffectsTest.publish(cast(f,1,1,1).observation()));
            pair(sink,"pc-repeat",ModernHasteEffectsTest.publish(f.engine.serviceObservation()));
            f.elapsed(1_202_000_000_000L);pair(sink,"pc-expired",ModernHasteEffectsTest.publish(f.engine.serviceObservation()));
            pair(sink,"pc-abort",ModernHasteEffectsTest.publish(f.engine.abortObservation()));
        }
        try(var f=fixture(5_000_000_000L,ModernHasteCatalog.Status.QUALIFIED,ModernHasteCatalog.Status.QUALIFIED)) {
            active(f);var c=cast(f,0,1,1).observation();assertEquals(4_999_999_960L,c.actors().get(0).resources().vitals().mp());
            pair(sink,"pc-wide",ModernHasteEffectsTest.publish(c));
        }
        try(var f=ModernHasteNpcTargetsTest.fixture(280)) {
            ModernBattleNpcHasteTest.active(f);var n=ModernBattleNpcHasteTest.spawn(f,0,14,10);
            f.elapsed(100);assertEquals(ModernNpcBattle.Reason.MOVED,f.engine.stepNpcUnpublished(n.id(),1,4096,false));
            pair(sink,"npc-normal",ModernHasteEffectsTest.publish(f));
            f.elapsed(150);ModernBattleNpcHasteTest.cast(f,0,1,ModernBattleNpcHasteTest.npc(f.engine.serviceObservation()));
            pair(sink,"npc-haste",ModernHasteEffectsTest.publish(f));
            f.elapsed(200);assertEquals(ModernNpcBattle.Reason.MOVED,f.engine.stepNpcUnpublished(n.id(),1,4096,false));
            pair(sink,"npc-walk",ModernHasteEffectsTest.publish(f));
            f.elapsed(2000);ModernBattleNpcHasteTest.cast(f,0,2,ModernBattleNpcHasteTest.npc(f.engine.serviceObservation()));
            pair(sink,"npc-refresh",ModernHasteEffectsTest.publish(f));
            f.elapsed(1_202_000);pair(sink,"npc-expired",ModernHasteEffectsTest.publish(f));
            f.elapsed(1_202_100);assertEquals(ModernNpcBattle.Reason.MOVED,f.engine.stepNpcUnpublished(n.id(),1,4096,false));
            pair(sink,"npc-resumed",ModernHasteEffectsTest.publish(f));
            f.engine.abortObservation();pair(sink,"npc-abort",ModernHasteEffectsTest.publish(f));
        }
        try(var f=ModernBattleNpcHasteTest.fixture(true,2,0,300)) {
            var c=ModernBattleNpcHasteTest.active(f);
            for(int i=0;i<2;i++)ModernBattleNpcHasteTest.cast(f,i,1,c.npcs().roster().actors().get(i));
            pair(sink,"quest-lanes",ModernHasteEffectsTest.publish(f));
        }
        try(var f=ModernHasteNpcTargetsTest.fixture(280,false,false,10_000)) {
            ModernBattleNpcHasteTest.active(f);var template=f.config.catalog().npcs().get(ModernNpcMeleeTest.NPC).rosterTemplate();
            var spawns=new ArrayList<ModernNpcRoster.Spawn>();
            for(int i=0;i<128;i++)spawns.add(new ModernNpcRoster.Spawn(template,0,1,ModernBattleEngineNpcTest.cell(2+i%16,2+i/16),2));
            var npcs=f.engine.spawnNpcsObservation(f.scope,spawns).npcs().roster().actors();
            for(int i=0;i<128;i++){f.elapsed(i*2000);assertEquals(ModernHasteCast.Reason.NONE,ModernBattleNpcHasteTest.cast(f,0,i+1,npcs.get(i)).reason());}
            pair(sink,"npc-full",ModernHasteEffectsTest.publish(f));
        }
        try(var f=ModernBattleNpcHasteTest.fixture(false,1,1,300)) {
            var c=ModernBattleNpcHasteTest.active(f);
            pair(sink,"bot-start",ModernHasteEffectsTest.publish(f));
            assertEquals(ModernHasteCast.Reason.NONE,f.engine.haste(f.players.get(0).owner(),request(c,0,1,target(c,1))).reason());
            pair(sink,"bot-target",ModernHasteEffectsTest.publish(f));
            f.engine.abortObservation();pair(sink,"bot-abort",ModernHasteEffectsTest.publish(f));
        }
        try(var f=ModernHasteNpcTargetsTest.fixture(1,true,true)) {
            var c=ModernBattleNpcHasteTest.active(f);var player=f.players.get(0);
            pair(sink,"quest-start",ModernHasteEffectsTest.publish(f));
            f.engine.haste(player.owner(),request(c,0,1,target(c,0)));
            var other=c.npcs().roster().actors().stream().filter(n->n.lane()==1).findFirst().orElseThrow();
            ModernBattleNpcHasteTest.cast(f,1,1,other);f.elapsed(100);
            assertEquals(ModernNpcBattle.Reason.MOVED,f.engine.stepNpcUnpublished(other.id(),1,4096,false));f.engine.npcTickObservation();f.randomValue=19;
            pair(sink,"quest-combat",ModernHasteEffectsTest.publish(f));
            for(int stage=1;stage<=4;stage++){f.elapsed(stage*2000);assertEquals(ModernNpcFireStormState.Reason.NONE,
                    f.engine.fireStormNpc(player.owner(),new ModernNpcFireStormState.Request(player.scope(),f.scope,stage+1,1)).reason());}
            f.elapsed(10_000);var won=f.engine.fireStormNpc(player.owner(),new ModernNpcFireStormState.Request(player.scope(),f.scope,6,1)).observation();
            assertEquals(ModernBattleEngine.Phase.FINISHED,won.phase());pair(sink,"quest-finished",ModernBattleWireTest.publication(won,f.config.catalog()));
        }
        try(var f=ModernHasteNpcTargetsTest.fixture(1,true)) {
            var initial=ModernBattleNpcHasteTest.active(f);var n=initial.npcs().roster().actors().get(1);
            ModernBattleNpcHasteTest.cast(f,1,1,n);pair(sink,"corpse-cast",ModernHasteEffectsTest.publish(f));
            f.elapsed(2000);f.randomValue=19;var p=f.players.get(1);
            assertEquals(ModernNpcBattle.Reason.ATTACKED,f.engine.attackNpc(p.owner(),p.scope(),f.ticket.matchId(),f.ticket.generation(),1,n.id(),1).reason());
            pair(sink,"corpse-dead",ModernHasteEffectsTest.publish(f));f.elapsed(4000);
            assertTrue(f.engine.serviceObservation().npcs().roster().actors().stream().noneMatch(a->a.id().equals(n.id())));
            pair(sink,"corpse-retired",ModernHasteEffectsTest.publish(f));
        }
        try(var f=new ModernBattleEngineTimedTest.Fixture(3,2,1000,1,true,p->enable(p,ModernHasteCatalog.Status.QUALIFIED))) {
            pair(sink,"elimination-start",elimination(active(f)));
            pair(sink,"elimination-haste",elimination(cast(f,0,1,0).observation()));f.elapsed(2_000_000_000L);
            var hit=f.cast(0,2,4);assertEquals(ModernSpellProtocol.Reason.NONE,hit.reason());
            assertEquals(ModernBattleEngine.Phase.FINISHED,hit.observation().phase());pair(sink,"elimination-finished",elimination(hit.observation()));
        }
        var root=java.nio.file.Files.createTempDirectory("modern-haste43-wire-").toRealPath();
        try {
            try(var f=new ModernBattleCanonicalEquipmentTest.Fixture(root,new ModernEquipmentOverrideStore.FileOps(){},
                    p->enable(p,ModernHasteCatalog.Status.QUALIFIED),ModernGreaterHasteEquipmentTest::enoughMp)) {
                var gear=f.read();ModernHasteEquipmentTest.active(f);pair(sink,"gear-initial",ModernHasteEffectsTest.publish(f.capture()));
                var first=f.world.engine.haste(f.player().owner(),request(f.capture(),0,1,target(f.capture(),1))).observation();
                pair(sink,"gear-cast",ModernHasteEffectsTest.publish(first));
                var changed=f.set(gear,false);assertEquals(ModernBattleEngine.EquipmentDisposition.APPLIED,changed.publication().disposition());
                pair(sink,"gear-set",ModernHasteEffectsTest.publish(changed.observation()));f.world.elapsed(2_000_000_000L);
                var second=f.world.engine.haste(f.player().owner(),request(f.capture(),0,2,target(f.capture(),0))).observation();
                pair(sink,"gear-recast",ModernHasteEffectsTest.publish(second));f.killAndRevive();pair(sink,"gear-revived",ModernHasteEffectsTest.publish(f.capture()));
            }
        } finally {
            // Only this newly-created, owned fixture; never shared input files.
            try(var paths=java.nio.file.Files.walk(root)){for(var path:paths.sorted(java.util.Comparator.reverseOrder()).toList()){
                if(!path.toAbsolutePath().normalize().startsWith(root))throw new IllegalStateException("fixture cleanup scope");java.nio.file.Files.delete(path);
            }}
        }
        try(var room=new ModernBattleEngineBotTest.RoomFixture(100,0)) {
            var ticket=room.capture();var prepared=new ArrayList<ModernBattleEngine.PreparedMember>();
            for(var p:room.prepared(ticket)){
                var fighter=ModernMagicCombatTest.createFighter(p.scope().sourceId(),p.character(),p.fighter().profile().effective().maximumHp(),100,12,0);int i=prepared.size();
                prepared.add(enable(new ModernBattleEngine.PreparedMember(p.owner(),p.scope(),p.character(),ModernBattleEngineBotTest.cell(10+i%10*2,10+i/10*2),
                        p.stepIntervalNanos(),fighter,p.attackIntervalNanos(),p.skills(),p.castIntervalNanos(),p.damageMagic(),java.util.List.of(),ModernInventoryWeight.qualify(fighter.profile(),100),true),ModernHasteCatalog.Status.QUALIFIED));
            }
            try(var e=new ModernBattleEngine(ticket,room.binding,prepared,java.util.List.of(),ModernBattleEngineBotTest.LIMITS,()->1_000_000_000L,()->1000,b->0,false)){
                for(var p:room.players)e.ackLoadedObservation(p.owner(),p.scope(),ticket.matchId(),ticket.generation(),room.map.digest());
                for(int i=0;i<100;i++){var c=e.serviceObservation();assertEquals(ModernHasteCast.Reason.NONE,e.haste(room.players.get(i).owner(),request(c,i,1,target(c,i))).reason());}
                var p=ModernHasteEffectsTest.publish(e.serviceObservation());
                sink.accept("pc-full-1",p.audience(room.players.get(0).scope()));sink.accept("pc-full-100",p.audience(room.players.get(99).scope()));
            }
        }
        try(var f=new ModernBattleEngineTimedTest.Fixture(3,2,1000,1000,false,p->{
            var fighter=ModernMagicCombatTest.createFighter(p.scope().sourceId(),p.character(),1000,300,12,0);
            var member=new ModernBattleEngine.PreparedMember(p.owner(),p.scope(),p.character(),p.spawn(),p.stepIntervalNanos(),fighter,p.attackIntervalNanos(),
                    p.skills(),p.castIntervalNanos(),p.damageMagic(),p.matchItems(),ModernInventoryWeight.qualify(fighter.profile(),100),true);
            return enable(ModernBattleGreaterHasteTest.enable(member,ModernGreaterHasteCatalog.Status.QUALIFIED),ModernHasteCatalog.Status.QUALIFIED);
        })) {
            active(f);pair(sink,"mixed-43",mixed(cast(f,0,1,0).observation()));f.elapsed(2_000_000_000L);
            var changed=f.engine.greaterHaste(f.players.get(0).owner(),ModernBattleGreaterHasteTest.request(f,0,2));assertEquals(ModernGreaterHasteCast.Reason.NONE,changed.reason());
            pair(sink,"mixed-54",mixed(changed.observation()));f.elapsed(4_000_000_000L);
            var back=cast(f,0,3,0);assertEquals(ModernHasteCast.Reason.NONE,back.reason());pair(sink,"mixed-back",mixed(back.observation()));
            f.elapsed(6_000_000_000L);assertEquals(ModernItemProtocol.Reason.NONE,f.use(0,40013).reason());pair(sink,"mixed-potion",mixed(f.engine.serviceObservation()));
        }
    }
    static byte[] verified(ModernBattleAudience.View v) {return verified(v,44);}
    static byte[] verified(ModernBattleAudience.View v,int requiredVersion) {
        var bytes=ModernBattleWire.encodeHaste(v);assertTrue(bytes.length<=ModernBattleHasteWire.MAX_BYTES);
        assertArrayEquals(bytes,ModernBattleWire.encodeHaste(v));
        assertArrayEquals(bytes,ModernBattleWire.encodeVersioned(v,requiredVersion));
        assertTrue(v.world().descriptor().supportedBy(requiredVersion,ModernSessionProtocol.KNOWN_CAPABILITIES));
        assertFalse(v.world().descriptor().supportedBy(43,ModernSessionProtocol.KNOWN_CAPABILITIES&~ModernSessionProtocol.CAP_HASTE));
        assertFalse(v.world().descriptor().supportedBy(requiredVersion,ModernSessionProtocol.KNOWN_CAPABILITIES&~ModernSessionProtocol.CAP_HASTE));
        var b=ByteBuffer.wrap(bytes);assertEquals(ModernBattleWire.MAGIC,b.getInt());assertEquals(5,b.getShort());assertEquals(0,b.getShort());
        assertEquals(bytes.length,b.getInt());assertEquals(14,b.getShort());assertEquals(0,b.getShort());
        for(int i=1;i<=14;i++){
            assertEquals(i,b.getShort());assertEquals(0,b.getShort());int n=b.getInt();assertTrue(n>=0&&n<=(i==14?ModernBattleHasteWire.LIMIT:ModernBattleWire.sectionLimit(i)));
            if(i==14){var s=v.world().hasteEffects();assertEquals(s.world().roomId(),b.getLong());assertEquals(s.world().matchId(),b.getLong());assertEquals(s.world().generation(),b.getLong());assertEquals(s.revision(),b.getLong());assertEquals(s.targets().size(),b.getInt());b.position(b.position()+n-36);}
            else b.position(b.position()+n);
        }
        assertFalse(b.hasRemaining());
        var history=v.personal().history();var h=ModernEquipmentHistoryWire.encode(history,3);
        assertEquals(history,ModernEquipmentHistoryWire.decode(h,3));
        for(int s=1;s<=2;s++){int schema=s;assertThrows(IllegalArgumentException.class,()->ModernEquipmentHistoryWire.encode(history,schema));assertThrows(IllegalArgumentException.class,()->ModernEquipmentHistoryWire.decode(h,schema));}
        for(int s=1;s<=5;s++){int schema=s;assertThrows(IllegalArgumentException.class,()->ModernBattleWire.encode(v,schema));}
        for(int version=35;version<requiredVersion;version++){int selected=version;assertThrows(IllegalArgumentException.class,()->ModernBattleWire.encodeVersioned(v,selected));}
        return bytes;
    }
    @Test void completeRealPublicationsRetainAllTargetAndOwnerHistoryFacts()throws Exception {
        var labels=new ArrayList<String>();trace((label,v)->{verified(v);labels.add(label);});assertEquals(77,labels.size());
    }
    @Test void candidateApiDoesNotUpgradeAWorldWithoutTargetHaste()throws Exception {
        try(var f=new ModernBattleEngineTimedTest.Fixture()){f.active();var p=ModernBattleWireTest.publication(f.engine.serviceObservation(),null);
            var view=p.audience(f.players.get(0).scope());
            assertThrows(IllegalArgumentException.class,()->ModernBattleWire.encodeHaste(view));
            assertArrayEquals(ModernBattleWire.encodeVersioned(view,43),ModernBattleWire.encodeVersioned(view,44));}
    }
    @Test void sectionBoundIsIndependentOfTheLegacyMotionAndOuterPayloadBounds() {
        assertTrue(ModernBattleHasteWire.LIMIT>ModernBattleWire.NPC_MOTION_LIMIT);
        assertEquals(ModernBattleWire.MAX_PRESENTATION_BYTES+8+ModernBattleHasteWire.LIMIT,ModernBattleHasteWire.MAX_BYTES);
        var w=new ModernBattleWire.Writer(null);assertThrows(IllegalArgumentException.class,()->w.section(14,ModernBattleHasteWire.LIMIT,b->b.bytes(new byte[ModernBattleHasteWire.LIMIT+1])));
    }
}
