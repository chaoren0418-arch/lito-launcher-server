package local.lito381.server;

import static org.junit.jupiter.api.Assertions.*;
import static local.lito381.server.ModernMatchDescriptor.*;
import java.io.ByteArrayInputStream;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.UnaryOperator;
import org.junit.jupiter.api.Test;

class ModernNpcSpellProtocolTest {
    static ModernMatchDescriptor descriptor(boolean quest) {
        return new ModernMatchDescriptor(Ruleset.NPC_ENCOUNTER,NPC|COMBAT|SPELLS|ITEMS|WEIGHT|REGEN|NPC_LIGHT_ARROW
                |(quest?ISOLATED_FORCES|QUEST_OBJECTIVES|QUEST_LIVES:0),2000,2000,0,60000);
    }
    static ModernNpcProtocol.Frame wire(ModernBattleEngineNpcTest.Fixture f,ModernNpcBattle.Frame frame) {
        return ModernNpcProtocol.Frame.from(frame,descriptor(frame.world().quest().enabled())).withResources(ModernNpcProtocol.Resources.from(f.config.catalog()));
    }
    static ModernNpcSpellProtocol.Request request(ModernBattleEngineNpcTest.Fixture f,int player,long sequence,ModernNpcRoster.Actor npc) {
        return new ModernNpcSpellProtocol.Request(f.players.get(player).scope(),f.scope,sequence,1,npc.id().serial(),npc.life());
    }
    static ModernApplicationProtocol.NpcSpellResult message(ModernBattleEngineNpcTest.Fixture f,int player,long sequence,ModernNpcRoster.Actor npc,ModernNpcMagic.Result result) {
        return new ModernApplicationProtocol.NpcSpellResult(9,request(f,player,sequence,npc),new ModernNpcSpellProtocol.Reply(
                result.reason()==ModernNpcMagic.Reason.NONE?ModernNpcProtocol.Status.OK:ModernNpcProtocol.Status.DENIED,result.reason(),
                ModernQuestVisibility.npc(wire(f,result.frame()),f.players.get(player).scope())));
    }
    static void golden(String name,ModernApplicationProtocol.Message message)throws Exception {
        var bytes=ModernApplicationProtocol.encode(message,24);
        assertEquals(message,ModernApplicationProtocol.decode(bytes,24));
        System.out.println("npc-spell-v24-"+name+"="+java.util.HexFormat.of().formatHex(bytes));
    }
    static ModernApplicationProtocol.NpcResult read(ModernBattleEngineNpcTest.Fixture f,int player) {
        var scope=f.players.get(player).scope();
        return new ModernApplicationProtocol.NpcResult(10,new ModernNpcProtocol.Request(scope,ModernNpcProtocol.Action.READ,f.scope,0,0,0),
                new ModernNpcProtocol.Reply(ModernNpcProtocol.Status.OK,ModernNpcProtocol.Reason.NONE,ModernQuestVisibility.npc(wire(f,f.view()),scope)));
    }
    @Test void requestsCarryTwoIndependentLivesAndRejectOldVersionMalformedLengthAndPadding()throws Exception {
        var r=new ModernNpcSpellProtocol.Request(new ModernRooms.Scope(7,9007199254740993L,2,3),new ModernNpcRoster.Scope(11,12,13),17,19,23,29);
        var m=new ModernApplicationProtocol.NpcSpellRequest(1,r);var bytes=ModernApplicationProtocol.encode(m,24);assertEquals(96,bytes.length);
        assertEquals(m,ModernApplicationProtocol.decode(bytes,24));assertThrows(ModernApplicationProtocol.ProtocolException.class,()->ModernApplicationProtocol.encode(m,23));
        assertThrows(ModernApplicationProtocol.ProtocolException.class,()->ModernApplicationProtocol.decode(bytes,23));
        for(int i=0;i<bytes.length;i++){var shortFrame=Arrays.copyOf(bytes,i);assertThrows(ModernApplicationProtocol.ProtocolException.class,()->ModernApplicationProtocol.decode(shortFrame,24));}
        for(int offset:List.of(37,38,39)){var bad=bytes.clone();bad[offset]=1;assertThrows(ModernApplicationProtocol.ProtocolException.class,()->ModernApplicationProtocol.decode(bad,24));}
        var coalesced=new byte[bytes.length*2];System.arraycopy(bytes,0,coalesced,0,bytes.length);System.arraycopy(bytes,0,coalesced,bytes.length,bytes.length);
        var input=new ByteArrayInputStream(coalesced);assertArrayEquals(bytes,ModernApplicationProtocol.readFrame(input,24));assertEquals(bytes.length,input.available());
        var oversized=bytes.clone();ByteBuffer.wrap(oversized).putInt(4,1_000_000);assertThrows(ModernApplicationProtocol.ProtocolException.class,()->ModernApplicationProtocol.readFrame(new ByteArrayInputStream(oversized),24));
    }
    @Test void actualEngineManaAndNpcTargetSurviveTypedWireWithoutChangingOldPvpFields()throws Exception {
        try(var f=ModernBattleEngineNpcMagicTest.fixture()) {
            f.active();var npc=f.spawn(0,11,10);golden("before",read(f,0));
            var hit=ModernBattleEngineNpcMagicTest.cast(f,0,1,npc);var m=message(f,0,1,npc,hit);
            var bytes=ModernApplicationProtocol.encode(m,24);assertEquals(m,ModernApplicationProtocol.decode(bytes,24));
            var s=m.reply().frame().combat().npcSpells().get(0);assertEquals(npc.id(),s.target());assertEquals(hit.frame().world().revision()+1,s.revision());
            assertEquals(0,m.reply().frame().combat().world().casts().get(0).mpSpent());assertEquals(0,m.reply().frame().combat().vitals().get(0).lastTargetEntityId());
            assertThrows(ModernApplicationProtocol.ProtocolException.class,()->ModernApplicationProtocol.encode(m,23));
            assertThrows(ModernApplicationProtocol.ProtocolException.class,()->ModernApplicationProtocol.decode(bytes,23));
            assertEquals(m,ModernMatchDescriptor.forPeer(m,new ModernSessionBoundary.Negotiated(1,24,1,ModernSessionProtocol.KNOWN_CAPABILITIES)));
            assertThrows(IllegalArgumentException.class,()->ModernMatchDescriptor.forPeer(m,new ModernSessionBoundary.Negotiated(1,23,1,ModernSessionProtocol.KNOWN_CAPABILITIES)));
            var fast=message(f,0,2,npc,ModernBattleEngineNpcMagicTest.cast(f,0,2,npc));assertEquals(ModernNpcMagic.Reason.TOO_FAST,fast.reply().reason());
            assertEquals(fast,ModernApplicationProtocol.decode(ModernApplicationProtocol.encode(fast,24),24));
            golden("golden",m);golden("fast",fast);
            f.elapsed(1200);var hit2=ModernBattleEngineNpcMagicTest.cast(f,0,3,npc);golden("second",message(f,0,3,npc,hit2));
            f.elapsed(2400);assertEquals(ModernSpellProtocol.Reason.NONE,ModernNpcTeleportTest.cast(f,0,4).reason());golden("teleport",read(f,0));
            f.elapsed(16000);golden("regen",read(f,0));
        }
    }
    @Test void forceProjectionHidesOtherManaTargetsAndAllowsSparseEntityTwo()throws Exception {
        try(var f=ModernBattleEngineNpcMagicTest.fixture(true,true,100,2,0,true,ModernNpcMeleeTest.catalog(Map.of(ModernNpcCatalog.Field.HP,1L),true,0),UnaryOperator.identity())) {
            f.active();var npc=ModernQuestEngineTest.living(f,1);var hit=ModernBattleEngineNpcMagicTest.cast(f,1,1,npc);var m=message(f,1,1,npc,hit);
            var frame=m.reply().frame();assertEquals(1,frame.combat().npcSpells().size());assertEquals(2,frame.combat().npcSpells().get(0).entityId());
            assertEquals(1,frame.combat().world().audienceLane());assertTrue(frame.actors().stream().allMatch(n->n.lane()==1));
            assertEquals(m,ModernApplicationProtocol.decode(ModernApplicationProtocol.encode(m,24),24));
            golden("sparse",m);
            var hidden=ModernQuestVisibility.npc(wire(f,hit.frame()),f.players.get(0).scope());assertEquals(1,hidden.combat().npcSpells().get(0).entityId());assertEquals(0,hidden.combat().npcSpells().get(0).castCount());
            f.elapsed(1200);var retired=ModernQuestVisibility.npc(wire(f,f.view()),f.players.get(1).scope());assertTrue(retired.actors().stream().noneMatch(n->n.serial()==npc.id().serial()));
            assertEquals(frame.combat().npcSpells(),retired.combat().npcSpells());
            golden("retired",read(f,1));
        }
    }
    @Test void descriptorAndOldFrameCopiesCannotOmitManaLedgerOrAdvertiseMagicToOldPeers()throws Exception {
        assertFalse(descriptor(false).supportedBy(23,ModernSessionProtocol.KNOWN_CAPABILITIES));assertTrue(descriptor(false).supportedBy(24,ModernSessionProtocol.KNOWN_CAPABILITIES));
        assertFalse(descriptor(false).supportedBy(24,ModernSessionProtocol.RUNTIME_CAPABILITIES));
        assertThrows(IllegalArgumentException.class,()->new ModernMatchDescriptor(Ruleset.PVP_SKIRMISH,COMBAT|SPELLS|NPC_LIGHT_ARROW,2000,2000,0,60000));
        try(var f=ModernBattleEngineNpcMagicTest.fixture()) {
            var frame=wire(f,f.active()).combat();
            assertThrows(IllegalArgumentException.class,()->new ModernCombatProtocol.Frame(frame.world(),frame.vitals(),frame.damages(),frame.items(),frame.weights(),frame.buffs(),frame.ammo(),frame.regen(),frame.honor(),frame.fireStorms()));
        }
    }
    @Test void stateBinarySizeReservedBytesAndAbsentPayloadAreStrict()throws Exception {
        try(var f=ModernBattleEngineNpcMagicTest.fixture()) {
            var w=wire(f,f.active()).combat().world();var states=wire(f,f.view()).combat().npcSpells();var b=ByteBuffer.allocate(8+2*120);ModernNpcSpellProtocol.putStates(b,states);assertEquals(b.capacity(),b.position());
            assertEquals(states,ModernNpcSpellProtocol.getStates(ByteBuffer.wrap(b.array()),w));
            for(int offset:List.of(2,3,7,8+74,8+79,8+80,8+119)){var bytes=b.array().clone();bytes[offset]=1;assertThrows(IllegalArgumentException.class,()->ModernNpcSpellProtocol.getStates(ByteBuffer.wrap(bytes),w));}
        }
    }
}
