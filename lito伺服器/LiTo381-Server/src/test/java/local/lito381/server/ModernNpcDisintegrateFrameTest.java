package local.lito381.server;

import static org.junit.jupiter.api.Assertions.*;
import static local.lito381.server.ModernMatchDescriptor.*;
import static local.lito381.server.ModernNpcDisintegrateState.Reason.*;
import java.io.ByteArrayInputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** Complete v29 atomic NPC frames from the real engine. No request-route or UI claim. */
class ModernNpcDisintegrateFrameTest {
    static final Map<String,String> GOLDEN_HASHES=Map.ofEntries(
        Map.entry("time-attack-finished","208c6d150a4f3a582864f51a646ac0f42a45a8cc069e0270246cd713a7b6ac1e"),
        Map.entry("time-attack-saved","3087354b33cbacb10fc370fb178c1d6af3d6416d62885c412afcdfbb70d42920"),
        Map.entry("quest-own","55150b6da4ac172f670567d1198225c7412a3b0e08022b309b4143450df8e55b"),
        Map.entry("quest-other","b869ce9985a892c8441cc7e645af77a1e31f5bde7eac5973a8b2ecf56b59ca5b"),
        Map.entry("quest-retired","0ec5459ff973873fa782f9a0b212c0da62b6f8fc229905abb5807ef62c6dd791"),
        Map.entry("normal-loading","0f17e831703151649c826544d4a7a66f7fbecd0c6132164cd0dac4fcc2debbea"),
        Map.entry("normal-before","593032910c74cdb081557cb18188cf7df57f79feb07801083ab62fb2d152fc98"),
        Map.entry("normal-hit","174efd00a041fddb9309c0b6d8fc62ed2f759c8a83edb7d586fdf690d2e5744b"),
        Map.entry("normal-denied","a1b29694d864ac331152313ac795ac8943dc2b78c06842f481d1e0acc13832ec"),
        Map.entry("normal-arrow","587c7a98f30d90c82ee50b1dec5736468f337b49e460fe50bafbe269f480017e"),
        Map.entry("normal-second","83a7686cca81489e3f94f09cc38285184a0f2f7939a897f48c305815210d4281"),
        Map.entry("normal-regen","6af21fb0f3cdb6bb01e7b650cfaf92d7b401ab79ea900ec32cfa0caf5a9bee8a"),
        Map.entry("critical-loading","0f17e831703151649c826544d4a7a66f7fbecd0c6132164cd0dac4fcc2debbea"),
        Map.entry("critical-before","593032910c74cdb081557cb18188cf7df57f79feb07801083ab62fb2d152fc98"),
        Map.entry("critical-hit","73f133aa111a5b0764cf279cdcd5dd614dd73f48785db15c92ec3b3b614bf713"),
        Map.entry("critical-denied","08a0d3139ea8632c67b0fac1f58e8a1e35b93fcb39eb01f72f76fbd61406a0f6"),
        Map.entry("critical-arrow","7c222415b9fc6aa6eb0189cfb03d979044a97ef29605c60feb1bcf8472372601"),
        Map.entry("critical-second","47b19691cfeec46eabbe17b581163f45ede3101565160a6c86b9ebf12f1d8b85"),
        Map.entry("critical-regen","7c575a536011483c3d03879a6cf42ba92c3297a9384e25f918166d237c4fb467"));
    static ModernMatchDescriptor descriptor(ModernBattleEngine.Snapshot w) {
        int flags=NPC|COMBAT|SPELLS|ITEMS|WEIGHT|REGEN|NPC_DISINTEGRATE;
        if(!w.npcSpells().isEmpty())flags|=NPC_LIGHT_ARROW;
        if(w.isolatedForces())flags|=ISOLATED_FORCES;
        if(w.quest().enabled())flags|=QUEST_OBJECTIVES|QUEST_LIVES;
        if(w.timeAttack().enabled())flags|=TIME_ATTACK;
        return new ModernMatchDescriptor(w.timeAttack().enabled()?Ruleset.COOPERATIVE_TIME_ATTACK:Ruleset.NPC_ENCOUNTER,flags,2000,2000,0,60000);
    }
    static ModernNpcProtocol.Frame frame(ModernBattleEngineNpcTest.Fixture f,ModernNpcBattle.Frame value,int who) {
        return ModernQuestVisibility.npc(ModernNpcProtocol.Frame.from(value,descriptor(value.world())).withResources(ModernNpcProtocol.Resources.from(f.config.catalog())).withNames(ModernNpcNames.Table.from(f.config.catalog())),f.players.get(who).scope());
    }
    static ModernApplicationProtocol.NpcResult message(ModernRooms.Scope player,ModernNpcProtocol.Frame f) {
        return new ModernApplicationProtocol.NpcResult(77,new ModernNpcProtocol.Request(player,ModernNpcProtocol.Action.READ,f.scope(),0,0,0),new ModernNpcProtocol.Reply(ModernNpcProtocol.Status.OK,ModernNpcProtocol.Reason.NONE,f));
    }
    static byte[] golden(String name,ModernRooms.Scope player,ModernNpcProtocol.Frame f)throws Exception {
        var m=message(player,f);var bytes=ModernApplicationProtocol.encode(m,29);
        assertEquals(m,ModernApplicationProtocol.decode(bytes,29));assertArrayEquals(bytes,ModernApplicationProtocol.readFrame(new ByteArrayInputStream(bytes),29));
        assertEquals(GOLDEN_HASHES.get(name),HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(bytes)),name+" complete-frame golden changed");
        System.out.println("npc77-frame:"+name+":"+HexFormat.of().formatHex(bytes));return bytes;
    }
    static ModernCombatProtocol.Frame states(ModernCombatProtocol.Frame c,List<ModernNpcDisintegrateState.State> states) {
        return new ModernCombatProtocol.Frame(c.world(),c.vitals(),c.damages(),c.items(),c.weights(),c.buffs(),c.ammo(),c.regen(),c.honor(),c.fireStorms(),c.npcSpells(),c.disintegrations(),states);
    }
    static ModernNpcProtocol.Frame roundtrip(ModernBattleEngineNpcTest.Fixture f,int who)throws Exception {
        var nf=frame(f,f.view(),who);var m=message(f.players.get(who).scope(),nf);
        assertEquals(m,ModernApplicationProtocol.decode(ModernApplicationProtocol.encode(m,29),29));return nf;
    }
    @Test void fullWidthHpZeroDamageAndBotInitialStateSurviveCompleteFrames()throws Exception {
        for(long mr:List.of(0L,Long.MAX_VALUE))try(var f=ModernBattleEngineNpcDisintegrateTest.fixture(false,100,1,1,false,
                ModernDisintegrateCatalog.Status.QUALIFIED,ModernNpcMeleeTest.catalog(Map.of(ModernNpcCatalog.Field.HP,9007199254740993L,ModernNpcCatalog.Field.MR,mr),true,0),
                s->ModernBattleEngineNpcDisintegrateTest.source(s,1000,500))) {
            roundtrip(f,0);f.active();var target=f.spawn(0,11,10);assertEquals(NONE,ModernBattleEngineNpcDisintegrateTest.cast(f,0,1,target).reason());
            var nf=roundtrip(f,0);var h=nf.combat().npcDisintegrations().get(0).hit();
            assertEquals(9007199254740993L,h.hpBefore());assertEquals(h.hpBefore()-h.outcome().appliedDamage(),nf.actors().get(0).hp());
            assertEquals(930,nf.combat().vital(1).mp());assertEquals(ModernNpcDisintegrateState.State.initial(2,0,0),nf.combat().npcDisintegrations().get(1));
            if(mr==Long.MAX_VALUE){assertEquals(0,h.outcome().appliedDamage());assertNotEquals(ModernNpcRoster.Action.HURT,nf.actors().get(0).action());}
        }
    }
    @Test void questDeathAndRespawnPreservePriorCasterLifeAndAllowNewLifeReceipt()throws Exception {
        try(var f=ModernBattleEngineNpcDisintegrateTest.fixture(true,1,2,0,true,ModernDisintegrateCatalog.Status.QUALIFIED,
                ModernNpcMeleeTest.catalog(Map.of(ModernNpcCatalog.Field.HP,10000L),true,0),s->ModernBattleEngineNpcDisintegrateTest.source(s,1000,500))) {
            f.active();var target=ModernQuestEngineTest.living(f,0);assertEquals(NONE,ModernBattleEngineNpcDisintegrateTest.cast(f,0,1,target).reason());
            var first=roundtrip(f,0).combat().npcDisintegrations().get(0);var dead=ModernQuestLifeTest.die(f,100);
            assertEquals(DEAD,ModernBattleEngineNpcDisintegrateTest.cast(f,0,2,target).reason());assertEquals(first,roundtrip(f,0).combat().npcDisintegrations().get(0));
            assertEquals(ModernQuestLife.Reason.REVIVED,ModernQuestLifeTest.confirm(f,0,ModernQuestLifeTest.life(dead,0)).reason());
            assertEquals(first,roundtrip(f,0).combat().npcDisintegrations().get(0));
            f.elapsed(5000);assertEquals(NONE,ModernBattleEngineNpcDisintegrateTest.cast(f,0,3,target).reason());
            var after=roundtrip(f,0);assertEquals(2,after.combat().npcDisintegrations().get(0).hit().casterLife());assertEquals(140,after.combat().npcDisintegrations().get(0).mpSpent());
        }
    }
    static ModernNpcDisintegrateState.State revision(ModernNpcDisintegrateState.State s,long revision) {
        return new ModernNpcDisintegrateState.State(s.entityId(),s.castCount(),revision,s.lastSequence(),s.mpSpent(),s.mpCost(),s.initialLawful(),s.lawful(),s.hit());
    }
    @Test void completeFrameCarriesInitialNormalCriticalDenialRecastAndExactMana()throws Exception {
        for(boolean critical:List.of(false,true))try(var f=ModernBattleEngineNpcDisintegrateTest.fixture()) {
            String prefix=critical?"critical":"normal";var p=f.players.get(0).scope();
            golden(prefix+"-loading",p,frame(f,f.view(),0));f.active();var target=f.spawn(0,11,10);f.randomValue=critical?0:99;
            golden(prefix+"-before",p,frame(f,f.view(),0));var hit=ModernBattleEngineNpcDisintegrateTest.cast(f,0,1,target);assertEquals(NONE,hit.reason());
            var wire=frame(f,hit.frame(),0);var s=wire.combat().npcDisintegrations().get(0);
            assertEquals(hit.frame().world().revision()+1,s.revision());assertEquals(930,wire.combat().vitals().get(0).mp());assertEquals(501,s.lawful());
            assertEquals(0,wire.combat().world().casts().get(0).mpSpent());assertEquals(0,wire.combat().vitals().get(0).lastTargetEntityId());golden(prefix+"-hit",p,wire);
            assertEquals(TOO_FAST,ModernBattleEngineNpcDisintegrateTest.cast(f,0,2,target).reason());golden(prefix+"-denied",p,frame(f,f.view(),0));
            f.elapsed(1200);assertEquals(ModernNpcMagic.Reason.NONE,ModernBattleEngineNpcMagicTest.cast(f,0,3,target).reason());golden(prefix+"-arrow",p,frame(f,f.view(),0));
            f.elapsed(5000);assertEquals(NONE,ModernBattleEngineNpcDisintegrateTest.cast(f,0,4,target).reason());golden(prefix+"-second",p,frame(f,f.view(),0));
            f.elapsed(16000);golden(prefix+"-regen",p,frame(f,f.view(),0));
        }
    }
    @Test void olderVersionsAndMissingCapabilityCannotReceiveNpc77Frames()throws Exception {
        try(var f=ModernBattleEngineNpcDisintegrateTest.fixture()) {
            f.active();var nf=frame(f,f.view(),0);var m=message(f.players.get(0).scope(),nf);var bytes=ModernApplicationProtocol.encode(m,29);
            for(int v=1;v<29;v++){final int version=v;assertThrows(ModernApplicationProtocol.ProtocolException.class,()->ModernApplicationProtocol.encode(m,version));assertThrows(ModernApplicationProtocol.ProtocolException.class,()->ModernApplicationProtocol.decode(bytes,version));}
            assertFalse(nf.combat().world().descriptor().supportedBy(29,(ModernSessionProtocol.KNOWN_CAPABILITIES&~(ModernSessionProtocol.CAP_SLOW_DATA|ModernSessionProtocol.CAP_HASTE|ModernSessionProtocol.CAP_GREATER_HASTE|ModernSessionProtocol.CAP_ACTION_LIFE|ModernSessionProtocol.CAP_LARGE_MAPS|ModernSessionProtocol.CAP_BATTLE_DATA|ModernSessionProtocol.CAP_MATCHMAKING|ModernSessionProtocol.CAP_NPC_FIRE_STORM|ModernSessionProtocol.CAP_CHAT|ModernSessionProtocol.CAP_DIRECT_CHAT|ModernSessionProtocol.CAP_FILTERED_CHAT))&~ModernSessionProtocol.CAP_NPC_DISINTEGRATE));
            assertThrows(IllegalArgumentException.class,()->ModernMatchDescriptor.forPeer(m,new ModernSessionBoundary.Negotiated(1,29,1,(ModernSessionProtocol.KNOWN_CAPABILITIES&~(ModernSessionProtocol.CAP_SLOW_DATA|ModernSessionProtocol.CAP_HASTE|ModernSessionProtocol.CAP_GREATER_HASTE|ModernSessionProtocol.CAP_ACTION_LIFE|ModernSessionProtocol.CAP_LARGE_MAPS|ModernSessionProtocol.CAP_BATTLE_DATA|ModernSessionProtocol.CAP_MATCHMAKING|ModernSessionProtocol.CAP_NPC_FIRE_STORM|ModernSessionProtocol.CAP_CHAT|ModernSessionProtocol.CAP_DIRECT_CHAT|ModernSessionProtocol.CAP_FILTERED_CHAT))&~ModernSessionProtocol.CAP_NPC_DISINTEGRATE)));
            assertEquals(m,ModernMatchDescriptor.forPeer(m,new ModernSessionBoundary.Negotiated(1,29,1,(ModernSessionProtocol.KNOWN_CAPABILITIES&~(ModernSessionProtocol.CAP_SLOW_DATA|ModernSessionProtocol.CAP_HASTE|ModernSessionProtocol.CAP_GREATER_HASTE|ModernSessionProtocol.CAP_ACTION_LIFE|ModernSessionProtocol.CAP_LARGE_MAPS|ModernSessionProtocol.CAP_BATTLE_DATA|ModernSessionProtocol.CAP_MATCHMAKING|ModernSessionProtocol.CAP_NPC_FIRE_STORM|ModernSessionProtocol.CAP_CHAT|ModernSessionProtocol.CAP_DIRECT_CHAT|ModernSessionProtocol.CAP_FILTERED_CHAT))|ModernSessionProtocol.CAP_NPC_DISINTEGRATE)));
            assertDoesNotThrow(()->ModernSessionProtocol.encode(new ModernSessionProtocol.Hello(1,29,29,(ModernSessionProtocol.KNOWN_CAPABILITIES&~(ModernSessionProtocol.CAP_SLOW_DATA|ModernSessionProtocol.CAP_HASTE|ModernSessionProtocol.CAP_GREATER_HASTE|ModernSessionProtocol.CAP_ACTION_LIFE|ModernSessionProtocol.CAP_LARGE_MAPS|ModernSessionProtocol.CAP_BATTLE_DATA|ModernSessionProtocol.CAP_MATCHMAKING|ModernSessionProtocol.CAP_NPC_FIRE_STORM|ModernSessionProtocol.CAP_CHAT|ModernSessionProtocol.CAP_DIRECT_CHAT|ModernSessionProtocol.CAP_FILTERED_CHAT)),0,1)));
            assertEquals(0,ModernSessionProtocol.RUNTIME_CAPABILITIES&ModernSessionProtocol.CAP_NPC_DISINTEGRATE);
        }
    }
    @Test void droppingEvenInitialLedgerOrMixingPlayerDamageCannotBuildAnAtomicFrame()throws Exception {
        try(var f=ModernBattleEngineNpcDisintegrateTest.fixture()) {
            f.active();var c=frame(f,f.view(),0).combat();assertThrows(IllegalArgumentException.class,()->states(c,List.of()));
            assertThrows(IllegalArgumentException.class,()->new ModernCombatProtocol.Frame(c.world(),c.vitals(),c.damages(),c.items(),c.weights(),c.buffs(),c.ammo(),c.regen(),c.honor(),c.fireStorms(),c.npcSpells(),c.disintegrations()));
            var target=f.spawn(0,11,10);var hit=ModernBattleEngineNpcDisintegrateTest.cast(f,0,1,target);var next=frame(f,hit.frame(),0).combat();
            var missing=new ArrayList<>(next.npcDisintegrations());missing.set(0,ModernNpcDisintegrateState.State.initial(1,70,500));
            assertThrows(IllegalArgumentException.class,()->states(next,missing),"MP cannot drop its NPC77 expenditure");
            var wrong=descriptor(hit.frame().world());assertThrows(IllegalArgumentException.class,()->new ModernMatchDescriptor(Ruleset.PVP_SKIRMISH,wrong.features(),2000,2000,0,60000));
        }
    }
    @Test void questSparseSecondEntityKeepsOwnReceiptAndHidesOtherTeam()throws Exception {
        try(var f=ModernBattleEngineNpcDisintegrateTest.fixture(true,100,2,0,true,ModernDisintegrateCatalog.Status.QUALIFIED,
                ModernNpcMeleeTest.catalog(Map.of(ModernNpcCatalog.Field.HP,1L),true,0),s->ModernBattleEngineNpcDisintegrateTest.source(s,1000,500))) {
            f.active();var target=ModernQuestEngineTest.living(f,1);var hit=ModernBattleEngineNpcDisintegrateTest.cast(f,1,1,target);assertEquals(NONE,hit.reason());
            var own=frame(f,hit.frame(),1);var other=frame(f,hit.frame(),0);
            assertEquals(List.of(2L),own.combat().npcDisintegrations().stream().map(ModernNpcDisintegrateState.State::entityId).toList());
            assertEquals(1,own.combat().world().audienceLane());assertEquals(930,own.combat().vital(2).mp());assertEquals(1,own.combat().npcDisintegrations().get(0).castCount());
            assertEquals(0,other.combat().npcDisintegrations().get(0).castCount());assertEquals(1000,other.combat().vital(1).mp());
            golden("quest-own",f.players.get(1).scope(),own);golden("quest-other",f.players.get(0).scope(),other);
            f.elapsed(1200);var retired=frame(f,f.view(),1);assertTrue(retired.actors().stream().noneMatch(a->a.serial()==target.id().serial()));golden("quest-retired",f.players.get(1).scope(),retired);
        }
    }
    @Test void foreignSpellAndNpcMeleeRevisionsCannotBeReusedAsNpc77Events()throws Exception {
        try(var f=ModernBattleEngineNpcDisintegrateTest.fixture()) {
            f.active();var target=f.spawn(0,11,10);var another=f.spawn(1,21,10);ModernBattleEngineNpcDisintegrateTest.cast(f,0,1,target);
            ModernBattleEngineNpcMagicTest.cast(f,1,1,another);var c=frame(f,f.view(),0).combat();var rows=new ArrayList<>(c.npcDisintegrations());
            rows.set(0,revision(rows.get(0),c.npcSpells().get(1).revision()));assertThrows(IllegalArgumentException.class,()->states(c,rows));
            f.elapsed(1200);f.attack(1,1,another);var nf=frame(f,f.view(),0);var states=new ArrayList<>(nf.combat().npcDisintegrations());
            states.set(0,revision(states.get(0),nf.exchanges().get(0).revision()));
            // A new NPC receipt cannot borrow an unrelated attack revision even when generic vitals don't record NPC targets.
            assertThrows(IllegalArgumentException.class,()->nf.withCombat(states(nf.combat(),states)));
        }
    }
    @Test void wrongNpcLifeCellHpAndCrossGenerationReceiptCannotPassFullFrame()throws Exception {
        try(var f=ModernBattleEngineNpcDisintegrateTest.fixture()) {
            f.active();var target=f.spawn(0,11,10);var nf=frame(f,ModernBattleEngineNpcDisintegrateTest.cast(f,0,1,target).frame(),0);var c=nf.combat();var s=c.npcDisintegrations().get(0);var h=s.hit();
            for(int change=0;change<4;change++) {
                var id=change==0?new ModernNpcRoster.Id(new ModernNpcRoster.Scope(nf.scope().roomId(),nf.scope().matchId(),nf.scope().generation()+1),1):h.target();
                var altered=new ModernNpcDisintegrateState.Hit(id,change==1?2:1,0,1,h.from(),change==2?new ModernWorldMap.Cell(h.to().x()+1,h.to().y()):h.to(),
                        change==3?h.hpBefore()+1:h.hpBefore(),change==3?new ModernDisintegrate.Outcome(h.outcome().rawDamage(),h.outcome().appliedDamage(),h.outcome().remainingHp()+1,false,h.outcome().critical()):h.outcome());
                var rows=new ArrayList<>(c.npcDisintegrations());rows.set(0,new ModernNpcDisintegrateState.State(1,1,s.revision(),1,70,70,500,501,altered));
                assertThrows(IllegalArgumentException.class,()->nf.withCombat(states(c,rows)));
            }
        }
    }
    @Test void fullFramesRejectTruncationUnknownFlagsAndLostLedgerWithoutChangingOldBytes()throws Exception {
        try(var f=ModernBattleEngineNpcDisintegrateTest.fixture()) {
            f.active();var target=f.spawn(0,11,10);var c=frame(f,ModernBattleEngineNpcDisintegrateTest.cast(f,0,1,target).frame(),0);var bytes=ModernApplicationProtocol.encode(message(f.players.get(0).scope(),c),29);
            for(int n=0;n<bytes.length;n++){var truncated=Arrays.copyOf(bytes,n);assertThrows(ModernApplicationProtocol.ProtocolException.class,()->ModernApplicationProtocol.decode(truncated,29));}
            var b=ByteBuffer.allocate(ModernNpcProtocol.MAX_FRAME_BYTES);ModernCombatProtocol.putFrame(b,c.combat(),29);int stateStart=92+b.position()-8-c.combat().npcDisintegrations().size()*144;
            for(int offset:List.of(stateStart,stateStart+2,stateStart+8+64,stateStart+8+66,stateStart+8+143)) {
                var changed=bytes.clone();changed[offset]^=0x40;assertThrows(ModernApplicationProtocol.ProtocolException.class,()->ModernApplicationProtocol.decode(changed,29),"offset "+offset);
            }
        }
        try(var f=ModernBattleEngineNpcMagicTest.fixture()) {
            f.active();var read=ModernNpcSpellProtocolTest.read(f,0);var old=message(read.echo().player(),read.reply().frame().withNames(ModernNpcNames.Table.from(f.config.catalog())));var oldBytes=ModernApplicationProtocol.encode(old,28);var newBytes=ModernApplicationProtocol.encode(old,29);
            assertEquals(oldBytes.length+8,newBytes.length);assertEquals(old,ModernApplicationProtocol.decode(newBytes,29));assertEquals(old,ModernApplicationProtocol.decode(oldBytes,28));
            assertThrows(ModernApplicationProtocol.ProtocolException.class,()->ModernApplicationProtocol.decode(newBytes,28));assertThrows(ModernApplicationProtocol.ProtocolException.class,()->ModernApplicationProtocol.decode(oldBytes,29));
        }
    }
    @Test void timeAttackLastNpcDeathAndLaterSavedRevisionRetainExactTerminalReceipt()throws Exception {
        try(var f=new ModernTimeAttackEngineTest.Fixture(2,8,100,true,1,true)) {
            f.active();f.tower(0,1);for(int i=0;i<f.plan.waves().size();i++){f.elapsed(1000+5000L*i);assertEquals(NONE,ModernNpcDisintegrateTimeAttackTest.cast(f,0,i+1,f.living()).reason());}
            var value=f.view();var e=f.engine.timeAttackResult();
            e=new ModernMatchSettlement.Event(UUID.fromString("12345678-1234-5678-9abc-def012345678"),e.matchId(),e.generation(),e.engineRevision(),e.mode(),e.mapId(),e.finishedAtMillis(),e.participants(),e.timeAttack());
            var nf=ModernTimeAttackProtocol.frame(value,descriptor(value.world()),e,ModernWorldProtocol.SettlementState.PENDING,new ModernNpcProtocol.Resources(List.of(value.npcs().actors().get(0).template().graphic())))
                    .withNames(ModernNpcNames.Table.from(ModernNpcMeleeTest.catalog(Map.of(ModernNpcCatalog.Field.HP,1L),true,0)));
            golden("time-attack-finished",f.players.get(0).scope(),nf);assertEquals(e.engineRevision()+1,nf.combat().npcDisintegrations().get(0).revision());
            var w=nf.combat().world();var saved=new ModernWorldProtocol.Frame(w.matchId(),w.generation(),w.revision()+1,w.phase(),w.abortReason(),w.room(),w.manifest(),w.actors(),w.completion(),w.casts(),w.descriptor(),w.race(),w.audienceLane(),w.quest(),w.timeAttack().withSettlement(ModernWorldProtocol.SettlementState.SAVED));
            var c=nf.combat();var finalFrame=nf.withCombat(new ModernCombatProtocol.Frame(saved,c.vitals(),c.damages(),c.items(),c.weights(),c.buffs(),c.ammo(),c.regen(),c.honor(),c.fireStorms(),c.npcSpells(),c.disintegrations(),c.npcDisintegrations()));
            golden("time-attack-saved",f.players.get(0).scope(),finalFrame);
        }
    }
}
