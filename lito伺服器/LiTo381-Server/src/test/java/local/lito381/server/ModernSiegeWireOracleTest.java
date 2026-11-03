package local.lito381.server;

import static org.junit.jupiter.api.Assertions.*;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** Independent Rust fixtures: real authority transitions, frozen UUID only.
 * Synthetic shared S32 geometry; not a native-map or live-client acceptance. */
class ModernSiegeWireOracleTest {
    private static ModernCombatProtocol.Frame frame(ModernBattleEngineSiegeTest.Fixture f,ModernBattleEngine.Snapshot s) {
        var result=s.siege().finished()?f.engine.siegeResult():null;
        if(result!=null)result=new ModernMatchSettlement.Event(UUID.fromString("12345678-1234-5678-9abc-def012345678"),
                result.matchId(),result.generation(),result.engineRevision(),result.mode(),result.mapId(),result.finishedAtMillis(),result.participants(),null,result.siege());
        var w=ModernSiegeProtocol.frame(s,s.revision()+1,ModernSiegeProtocolTest.descriptor(f),result,ModernWorldProtocol.SettlementState.PENDING);
        return ModernWorldService.fromCombat(s,w);
    }
    private static void emit(String name,ModernApplicationProtocol.Message message)throws Exception {
        var b=ModernApplicationProtocol.encode(message,26);assertEquals(message,ModernApplicationProtocol.decode(b,26));
        assertThrows(ModernApplicationProtocol.ProtocolException.class,()->ModernApplicationProtocol.encode(message,25));
        if(message instanceof ModernApplicationProtocol.SiegeResult result) {
            var current=ModernApplicationProtocol.encode(message,27);
            var expected=java.util.Arrays.copyOf(b,b.length+(result.reply().frame()==null?0:8));
            java.nio.ByteBuffer.wrap(expected).putInt(4,expected.length);
            assertArrayEquals(expected,current,"v27 appends only the empty Disintegrate appendix: "+name);
            assertEquals(message,ModernApplicationProtocol.decode(current,27));
            assertArrayEquals(current,ModernApplicationProtocol.encode(message,28),"v28 has no NPC names in a siege-only frame");
            assertEquals(message,ModernApplicationProtocol.decode(current,28));
            assertTrue(result.reply().frame()==null||result.reply().frame().disintegrations().isEmpty());
        }
        System.out.println("siege-v26-"+name+"="+java.util.HexFormat.of().formatHex(b));
    }
    private static void read(String name,ModernBattleEngineSiegeTest.Fixture f,ModernBattleEngine.Snapshot s)throws Exception {
        read(name,f,frame(f,s));
    }
    private static void read(String name,ModernBattleEngineSiegeTest.Fixture f,ModernCombatProtocol.Frame c)throws Exception {
        var w=c.world();emit(name,new ModernApplicationProtocol.CombatResult(71,new ModernCombatProtocol.Request(f.players.get(0).scope(),
                ModernCombatProtocol.Action.READ,w.matchId(),w.generation(),0,0),new ModernCombatProtocol.Reply(ModernCombatProtocol.Status.OK,ModernCombatProtocol.Reason.NONE,c)));
    }
    private static void tower(String name,ModernBattleEngineSiegeTest.Fixture f,int who,long seq,ModernSiege.Slot slot)throws Exception {
        var hit=f.tower(who,seq,slot);assertEquals(ModernBattleEngine.AttackStatus.ATTACKED,hit.status());var c=frame(f,hit.snapshot());
        emit(name,new ModernApplicationProtocol.SiegeResult(72,new ModernSiegeProtocol.Request(f.players.get(who).scope(),c.world().siege().snapshot().state().scope(),
                ModernSiegeProtocol.Action.ATTACK_TOWER,seq,slot),new ModernSiegeProtocol.Reply(ModernCombatProtocol.Status.OK,ModernSiegeProtocol.Reason.NONE,c)));
    }
    @Test void realEngineTowerCaptureAndImmutableReceipt()throws Exception {
        try(var f=new ModernBattleEngineSiegeTest.Fixture(0,1,120000,1000,0,true)) {
            read("loading",f,f.tick());read("active",f,f.active());tower("left",f,0,1,ModernSiege.Slot.OUTER_LEFT);
            f.walk(0,ModernSiegeTest.cell(11,10));f.waitMillis(200);read("beforeright",f,f.tick());tower("right",f,0,2,ModernSiege.Slot.OUTER_RIGHT);
            f.walk(0,ModernSiegeTest.cell(29,10));f.walk(0,ModernSiegeTest.cell(29,9));f.walk(0,ModernSiegeTest.cell(31,9));f.waitMillis(200);
            read("beforeinner",f,f.tick());f.epoch.set(9007199254740993L);tower("inner",f,0,3,ModernSiege.Slot.INNER);
            var base=frame(f,f.tick());var w=base.world();
            for(var state:List.of(ModernWorldProtocol.SettlementState.SAVED,ModernWorldProtocol.SettlementState.FAILED)){
                var updated=new ModernWorldProtocol.Frame(w.matchId(),w.generation(),w.revision()+1,w.phase(),w.abortReason(),w.room(),w.manifest(),w.actors(),
                        null,w.casts(),w.descriptor(),w.race(),-1,w.quest(),null,w.siege().withSettlement(state));
                read(state.name().toLowerCase(),f,new ModernCombatProtocol.Frame(updated,base.vitals(),base.damages(),base.items(),base.weights(),base.buffs(),base.ammo(),base.regen(),base.honor(),base.fireStorms(),base.npcSpells()));
            }
        }
    }
    @Test void preparingAndPreEngineCancellationHaveNoCombatOrOptimisticLoadedActors()throws Exception {
        try(var f=new ModernBattleEngineSiegeTest.Fixture()) {
            read("preloading",f,f.tick());
            var loaded=frame(f,f.tick()).world();var data=ModernSiegeProtocol.preparing(loaded.siege().snapshot().state().scope(),f.config,0,30);
            var actors=loaded.actors().stream().map(a->new ModernBattleEngine.Actor(a.entityId(),a.member(),a.position(),a.heading(),false,0,a.stepIntervalNanos())).toList();
            for(boolean abort:List.of(false,true)) {
                var w=new ModernWorldProtocol.Frame(loaded.matchId(),loaded.generation(),abort?2:1,
                        abort?ModernWorldProtocol.Phase.ABORTED:ModernWorldProtocol.Phase.PREPARING,
                        abort?ModernWorldProtocol.AbortReason.SERVER_ABORT:ModernWorldProtocol.AbortReason.NONE,
                        loaded.room(),loaded.manifest(),actors,null,loaded.casts(),loaded.descriptor(),loaded.race(),-1,loaded.quest(),null,
                        abort?data.abortedBeforeEngine():data);
                var q=new ModernWorldProtocol.Request(f.players.get(0).scope(),ModernWorldProtocol.Action.READ,0,0,0,w.matchId(),w.generation(),0,0,0,ModernWorldProtocol.ZERO_DIGEST);
                emit(abort?"preabort":"preparing",new ModernApplicationProtocol.WorldResult(73,q,new ModernWorldProtocol.Reply(ModernWorldProtocol.Status.OK,ModernWorldProtocol.Reason.NONE,w)));
            }
        }
        try(var f=new ModernBattleEngineSiegeTest.Fixture(0,1,120000,1000,2_000_000_000L,true)) {
            read("countdown",f,f.active());f.waitMillis(2000);read("countdownactive",f,f.tick());
        }
    }
    @Test void bothRoleTimeoutsHundredMembersAndAbort()throws Exception {
        for(int attacker:List.of(0,1))try(var f=new ModernBattleEngineSiegeTest.Fixture(attacker,Long.MAX_VALUE,50,1000,0,true,50)) {
            read("wide"+attacker,f,f.active());f.waitMillis(50);read("timeout"+attacker,f,f.tick());
        }
        try(var f=new ModernBattleEngineSiegeTest.Fixture()){
            read("beforeabort",f,f.active());f.rooms.releasePreparation(f.ticket);read("aborted",f,f.tick());
        }
    }
    @Test void portalRespawnWithAndWithoutRegenAndNoMoveSequenceReset()throws Exception {
        for(boolean regen:List.of(false,true))try(var f=new ModernBattleEngineSiegeTest.Fixture(0,1,120000,1234,0,regen)) {
            var suffix=regen?"regen":"plain";read("lifeactive"+suffix,f,f.active());f.walk(1,ModernSiegeTest.cell(31,11));f.waitMillis(200);
            read("beforeportal"+suffix,f,f.tick());var crossed=f.portal(1);assertEquals(ModernSiege.PortalReason.MOVED,crossed.reason());var c=frame(f,crossed.snapshot());
            emit("portal"+suffix,new ModernApplicationProtocol.SiegeResult(72,new ModernSiegeProtocol.Request(f.players.get(1).scope(),c.world().siege().snapshot().state().scope(),
                    ModernSiegeProtocol.Action.PORTAL,c.world().actors().get(1).lastMoveSequence(),null),new ModernSiegeProtocol.Reply(ModernCombatProtocol.Status.OK,ModernSiegeProtocol.Reason.NONE,c)));
            read("afterportal"+suffix,f,f.tick());f.walk(1,ModernSiegeTest.cell(11,10));f.waitMillis(200);read("beforedeath"+suffix,f,f.tick());
            read("dead"+suffix,f,f.attack(0,1,2).snapshot());f.waitMillis(1234);read("respawn"+suffix,f,f.tick());
            f.walk(1,ModernSiegeTest.cell(31,11));read("afterrespawnmove"+suffix,f,f.tick());
        }
    }
    @Test void coalescedTeleportThenDeathAndRespawnUsesLatestRelocation()throws Exception {
        try(var f=new ModernBattleEngineSiegeTest.Fixture(0,1,120000,1234,0,true)) {
            f.active();f.walk(1,ModernSiegeTest.cell(31,11));f.waitMillis(200);f.portal(1);f.walk(1,ModernSiegeTest.cell(11,10));f.waitMillis(200);
            read("beforeteleportlife",f,f.tick());var cast=f.teleport(1,1);assertEquals(ModernSpellProtocol.Reason.NONE,cast.reason());read("teleportlife",f,cast.snapshot());
            var target=cast.snapshot().actors().get(1).position();f.walk(0,new ModernWorldMap.Cell(target.x()-1,target.y()));f.waitMillis(200);
            read("teleportdead",f,f.attack(0,1,2).snapshot());assertEquals(0,f.tick().vitals().get(1).hp());
            f.waitMillis(1234);read("teleportrespawn",f,f.tick());
        }
    }
}
