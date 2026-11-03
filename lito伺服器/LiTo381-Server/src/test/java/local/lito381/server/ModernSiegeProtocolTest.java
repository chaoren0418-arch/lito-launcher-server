package local.lito381.server;

import static org.junit.jupiter.api.Assertions.*;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

class ModernSiegeProtocolTest {
    static ModernMatchDescriptor descriptor(ModernBattleEngineSiegeTest.Fixture f){return new ModernMatchDescriptor(ModernMatchDescriptor.Ruleset.SIEGE_OBJECTIVES,
            ModernMatchDescriptor.SIEGE|ModernMatchDescriptor.COMBAT|ModernMatchDescriptor.SPELLS|ModernMatchDescriptor.ITEMS|ModernMatchDescriptor.WEIGHT
                    |(f.engine.tick().regen().isEmpty()?0:ModernMatchDescriptor.REGEN),2000,2000,0,Math.toIntExact(f.config.durationMillis()));}
    static ModernCombatProtocol.Frame project(ModernBattleEngineSiegeTest.Fixture f,ModernBattleEngine.Snapshot s)throws Exception {
        var w=ModernSiegeProtocol.frame(s,s.revision()+1,descriptor(f),s.siege().finished()?f.engine.siegeResult():null,ModernWorldProtocol.SettlementState.PENDING);
        var combat=ModernWorldService.fromCombat(s,w);var p=f.players.get(0);
        var m=new ModernApplicationProtocol.CombatResult(71,new ModernCombatProtocol.Request(p.scope(),ModernCombatProtocol.Action.READ,s.matchId(),s.generation(),0,0),
                new ModernCombatProtocol.Reply(ModernCombatProtocol.Status.OK,ModernCombatProtocol.Reason.NONE,combat));
        var bytes=ModernApplicationProtocol.encode(m,26);assertEquals(m,ModernApplicationProtocol.decode(bytes,26));
        assertThrows(ModernApplicationProtocol.ProtocolException.class,()->ModernApplicationProtocol.encode(m,25));return combat;
    }
    @Test void everyEnginePhaseBothRolesWideTowersAndHundredMembersRoundTripWithoutOldProjection()throws Exception {
        for(int attacker:List.of(0,1))try(var f=new ModernBattleEngineSiegeTest.Fixture(attacker,Long.MAX_VALUE,50,1000,0,true,50)) {
            assertEquals(100,project(f,f.tick()).world().actors().size());project(f,f.active());f.waitMillis(50);
            var done=project(f,f.tick());assertEquals(f.engine.siegeResult(),done.world().siege().event(done.world()));
            assertEquals(1-attacker,done.world().siege().snapshot().state().winner());assertNull(done.world().completion());
            assertThrows(IllegalArgumentException.class,()->done.world().withDescriptor(null));
        }
        try(var f=new ModernBattleEngineSiegeTest.Fixture()){project(f,f.tick());f.rooms.releasePreparation(f.ticket);assertEquals(ModernWorldProtocol.Phase.ABORTED,project(f,f.tick()).world().phase());}
    }
    @Test void gateInnerCaptureAndSourceQualifiedReceiptRemainExact()throws Exception {
        try(var f=new ModernBattleEngineSiegeTest.Fixture(0,1,120000,1000,0,true)) {
            f.active();project(f,f.tower(0,1,ModernSiege.Slot.OUTER_LEFT).snapshot());f.walk(0,ModernSiegeTest.cell(11,10));f.waitMillis(200);
            project(f,f.tower(0,2,ModernSiege.Slot.OUTER_RIGHT).snapshot());f.walk(0,ModernSiegeTest.cell(29,10));f.walk(0,ModernSiegeTest.cell(29,9));f.walk(0,ModernSiegeTest.cell(31,9));f.waitMillis(200);
            var end=project(f,f.tower(0,3,ModernSiege.Slot.INNER).snapshot()).world();assertEquals(f.engine.siegeResult(),end.siege().event(end));
            assertEquals(0,end.siege().snapshot().state().winner());assertEquals(7,end.siege().event(end).participants().get(0).identity().sourceId());assertEquals(42,end.siege().event(end).participants().get(1).identity().sourceId());
            var saved=end.siege().withSettlement(ModernWorldProtocol.SettlementState.SAVED);assertEquals(end.siege().event(end),saved.event(end));
        }
    }
    @Test void respawnIsNotInvalidDeathStateAndRegenLedgerKeepsExactHp()throws Exception {
        for(boolean regen:List.of(false,true))try(var f=new ModernBattleEngineSiegeTest.Fixture(0,1,120000,1234,0,regen)) {
            f.active();f.walk(1,ModernSiegeTest.cell(31,11));f.waitMillis(200);project(f,f.portal(1).snapshot());
            f.walk(1,ModernSiegeTest.cell(11,10));f.waitMillis(200);project(f,f.attack(0,1,2).snapshot());f.waitMillis(1234);
            var alive=project(f,f.tick());assertEquals(1,alive.vitals().get(1).hp());assertTrue(alive.vitals().get(1).deathRevision()>0);
            assertEquals(1,alive.world().siege().snapshot().lives().get(1).hpGain());
        }
    }
    @Test void dedicatedRequestBoundsReservedBytesDowngradeAndTruncationRejected()throws Exception {
        var p=new ModernRooms.Scope(7,3333,1,0);var scope=new ModernSiege.Scope(1,2,3);
        for(var action:ModernSiegeProtocol.Action.values()){
            var q=new ModernApplicationProtocol.SiegeRequest(71,new ModernSiegeProtocol.Request(p,scope,action,1,action==ModernSiegeProtocol.Action.PORTAL?null:ModernSiege.Slot.INNER));
            var bytes=ModernApplicationProtocol.encode(q,26);assertEquals(72,bytes.length);assertEquals(q,ModernApplicationProtocol.decode(bytes,26));
            assertThrows(ModernApplicationProtocol.ProtocolException.class,()->ModernApplicationProtocol.decode(bytes,25));
            for(int offset:List.of(37,38,39)){var broken=bytes.clone();broken[offset]=(byte)254;assertThrows(ModernApplicationProtocol.ProtocolException.class,()->ModernApplicationProtocol.decode(broken,26));}
            assertThrows(ModernApplicationProtocol.ProtocolException.class,()->ModernApplicationProtocol.decode(Arrays.copyOf(bytes,71),26));
        }
        try(var f=new ModernBattleEngineSiegeTest.Fixture()){
            var data=project(f,f.active()).world().siege();var b=ByteBuffer.allocate(20000);ModernSiegeProtocol.put(b,data);byte[] bytes=Arrays.copyOf(b.array(),b.position());
            for(int offset:List.of(1,8,14,15,16,17,18)){var broken=bytes.clone();broken[offset]=(byte)255;
                assertThrows(RuntimeException.class,()->ModernSiegeProtocol.get(ByteBuffer.wrap(broken),data.snapshot().state().scope(),f.config.key()));}
            for(int n:new int[]{0,7,19,300,bytes.length-1})assertThrows(RuntimeException.class,()->ModernSiegeProtocol.get(ByteBuffer.wrap(Arrays.copyOf(bytes,n)),data.snapshot().state().scope(),f.config.key()));
        }
    }
}
