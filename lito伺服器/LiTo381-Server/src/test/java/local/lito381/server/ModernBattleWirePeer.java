package local.lito381.server;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.util.Comparator;
import java.util.HexFormat;
import static local.lito381.server.ModernBattlePublicationTest.*;
import static local.lito381.server.ModernBattleAudienceTest.published;
import static org.junit.jupiter.api.Assertions.*;

/** Complete real publication -> Rust typed data -> fresh Rust encoding ->
 * exact digest. Independent fixture facts pin field meaning as well as bytes.
 * Not complete semantic admission, a TLS endpoint or SET authority. */
public final class ModernBattleWirePeer {
    private final BufferedReader input=new BufferedReader(new InputStreamReader(System.in,StandardCharsets.US_ASCII));
    private int count;
    private boolean presentation;
    private boolean greaterHaste;
    private void send(String label,ModernBattleAudience.View v)throws Exception {
        var bytes=ModernBattleWire.encode(v,greaterHaste?3:presentation?2:1);var w=v.world();var p=v.personal();
        var a=w.actors().get(0);var v0=a.vital();var c=w.room().summary().config();
        long[] facts={w.identity().roomId(),w.identity().matchId(),w.identity().generation(),w.revision(),p.scope().sourceId(),p.scope().objectId(),p.scope().epoch(),p.entityId(),
            w.phase().ordinal(),w.lane(),w.descriptor().ruleset().ordinal(),c.mode(),c.mapId(),w.manifest().mapId(),w.actors().size(),a.pose().entityId(),a.pose().position().x(),a.pose().position().y(),
            v0==null?0:v0.values().hp(),v0==null?0:v0.values().maximumHp(),v0==null?0:v0.values().mp(),v0==null?0:v0.values().maximumMp(),v0==null?0:v0.ac(),
            p.equipment()==null?-1:p.equipment().source().persisted().food225(),p.skills().size(),a.spells().receipts().size(),w.npcs()==null?-1:w.npcs().actors().size(),
            w.result()==null?-1:w.result().engineRevision(),p.history()==null?-1:p.history().resources().flows().spent(),p.ammo()==null?-1:p.ammo().shotCount()};
        var metadata=java.util.Arrays.stream(facts).mapToObj(Long::toString).collect(java.util.stream.Collectors.joining(","));
        System.out.println("FRAME:"+label+":"+metadata+":"+HexFormat.of().formatHex(bytes));System.out.flush();
        digestAck(bytes);
    }
    private void digestAck(byte[] bytes)throws Exception {
        var reply=new StringBuilder();int ch;while((ch=input.read())!=-1&&ch!='\n'){if(ch=='\r')continue;if(reply.length()>=64)throw new IllegalArgumentException("oversized digest");reply.append((char)ch);}
        if(!reply.toString().equals(HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes))))throw new IllegalStateException("wrong payload digest");count++;
    }
    private void run()throws Exception {
        try(var f=new ModernBattleEngineHonorTest.Fixture()) {
            f.active();f.cast(0,1,1,3);var c=f.engine.serviceObservation();var p=new ModernBattlePublication(c,honor(c),null,null);
            send("honor-a",p.audience(f.players.get(0).scope()));send("honor-b",p.audience(f.players.get(1).scope()));
        }
        try(var f=new ModernNpcServiceTest.Fixture(100,true,false,new ModernQuestServiceTest.Journal(),513)) {
            var last=ModernQuestServiceTest.clearToLast(f);f.service.npc(f.a.owner(),last);stopScheduler(f.service);var p=published(f.service,f.a.owner());
            send("quest-win",p.audience(f.a.scope()));send("quest-loss",p.audience(f.b.scope()));
        }
        var root=Files.createTempDirectory("modern-battle-wire-").toRealPath();
        try {
            try(var f=new ModernTimeAttackServiceTest.Fixture(root,1,new ModernTimeAttackServiceTest.Journal())) {
                var frame=f.active();stopScheduler(f.service);var p=f.players.get(0);f.tower(p,frame);for(int n=0;n<3;n++)f.hit(p,frame,n+2);
                send("time",published(f.service,p.owner()).audience(p.scope()));
            }
        } finally {
            // Only this newly-created owned directory; never an input/shared root.
            try(var paths=Files.walk(root)){for(var path:paths.sorted(Comparator.reverseOrder()).toList()) {
                if(!path.toAbsolutePath().normalize().startsWith(root))throw new IllegalStateException("fixture cleanup scope");Files.delete(path);
            }}
        }
        try(var f=new ModernSiegeServiceTest.Fixture(0,500,new ModernTimeAttackServiceTest.Journal())) {
            f.active();stopScheduler(f.service);ModernNpcServiceTest.await(()->f.world().phase()==ModernWorldProtocol.Phase.FINISHED);var p=f.players.get(0);
            send("siege",published(f.service,p.owner()).audience(p.scope()));
        }
        try(var f=new ModernBattleEnginePetRaceTest.Fixture(30_000,1_000_000_000L)) {
            f.active();var first=f.finish(0);f.elapsed(first.race().finishWindowDeadlineElapsedNanos());var c=f.engine.serviceObservation();var l=c.limits();
            var d=new ModernMatchDescriptor(ModernMatchDescriptor.Ruleset.PET_RACE,ModernMatchDescriptor.PET_RACE,2000,l.loadTimeoutMillis(),l.countdownNanos(),l.activeTimeoutMillis());
            send("race",new ModernBattlePublication(c,d,null,null).audience(f.a.scope()));
        }
        skillsAndStocks();hundred();sceneCases();modeCases();
        System.out.println("CLOSED:"+count);System.out.flush();
    }
    private void skillsAndStocks()throws Exception {
        try(var f=new ModernBattleEngineHonorTest.Fixture(1000,100,500,1_000_000_000L,0,true,true)) {
            f.active();f.cast(0,1,1,3);f.elapsed(2_000_000_000L);f.cast(0,2,4,2);f.elapsed(4_000_000_000L);f.cast(0,3,70,0);
            var c=f.engine.serviceObservation();send("honor-skills",ModernBattleWireTest.publication(c,null).audience(f.players.get(0).scope()));
        }
        try(var f=ModernBattleEngineDisintegrateTest.fixture(true,0,1000,1000,500,true)) {
            f.active();ModernBattleEngineDisintegrateTest.cast(f,1,2);var c=f.engine.serviceObservation();send("disintegrate",ModernBattleWireTest.publication(c,null).audience(f.players.get(0).scope()));
        }
        try(var f=ModernBattleEngineNpcFireStormTest.fixture(1000,10000,0,true,true,false,0)) {
            f.active();var n=f.spawn(0,11,10);ModernBattleEngineNpcMagicTest.cast(f,0,1,n);f.elapsed(1200);ModernBattleEngineNpcFireStormTest.cast(f,2);
            f.elapsed(2400);ModernBattleEngineNpcDisintegrateTest.cast(f,0,3,n);var c=f.engine.serviceObservation();send("npc-skills",ModernBattleWireTest.publication(c,f.config.catalog()).audience(f.players.get(0).scope()));
        }
        try(var f=ModernNpcFireStormStructuresTest.quest(513,1)) {
            f.active();ModernNpcFireStormStructuresTest.cast(f,0,1);f.elapsed(2000);ModernNpcFireStormStructuresTest.cast(f,0,2);f.elapsed(4000);ModernNpcFireStormStructuresTest.cast(f,0,3);
            var c=f.engine.serviceObservation();send("quest-structure",ModernBattleWireTest.publication(c,f.config.catalog()).audience(f.players.get(0).scope()));
        }
        try(var f=ModernNpcFireStormStructuresTest.time(100,1)) {
            f.active();ModernNpcFireStormStructuresTest.cast(f,0,1);var c=f.engine.serviceObservation();
            send("time-structure",ModernBattleWireTest.publication(c,f.catalog).audience(f.players.get(0).scope()));
        }
        try(var f=new ModernBattleEngineAmmoTest.Fixture()) {
            f.active();f.select(1,40746);f.attack(1);var c=f.engine.serviceObservation();send("ammo",ModernBattleWireTest.publication(c,null).audience(f.room.players.get(0).scope()));
        }
        try(var f=new ModernBattleEngineRegenTest.Fixture()) {
            f.active();f.cast(1,1,4);f.heal(0);f.elapsed(16_000_000_000L);var c=f.engine.serviceObservation();
            send("regen-items",ModernBattleWireTest.publication(c,null).audience(f.room.players.get(0).scope()));
        }
    }
    private void sceneCases()throws Exception {
        try(var f=new ModernBattleItemRuntimeTest.Fixture()) {
            f.game.active();f.game.cast(0,1,1,3);
            ModernBattleLifeProofTest.kill(f);f.game.clock.addAndGet(5_000_000_000L);f.view();
            f.equipped(false);ModernBattleLifeProofTest.kill(f);f.game.clock.addAndGet(5_000_000_000L);
            var c=f.game.engine.serviceObservation();assertEquals(290,c.actors().get(0).resources().flows().hpGranted());
            send("honor-equipment-eras",ModernBattleWireTest.publication(c,null).audience(f.game.players.get(0).scope()));
        }
        try(var f=ModernQuestLifeTest.fixture(2)) {
            f.active();var dead=ModernQuestLifeTest.die(f,100);var c=f.engine.serviceObservation();
            send("quest-dead",ModernBattleWireTest.publication(c,f.config.catalog()).audience(f.players.get(0).scope()));
            assertEquals(ModernQuestLife.Reason.REVIVED,ModernQuestLifeTest.confirm(f,0,ModernQuestLifeTest.life(dead,0)).reason());
            c=f.engine.serviceObservation();send("quest-revived",ModernBattleWireTest.publication(c,f.config.catalog()).audience(f.players.get(0).scope()));
            f.rooms.releasePreparation(f.ticket);c=f.engine.serviceObservation();
            send("quest-aborted",ModernBattleWireTest.publication(c,f.config.catalog()).audience(f.players.get(0).scope()));
        }
        for(int mode=0;mode<4;mode++)try(var f=new ModernTimeAttackEngineTest.Fixture(2,8,100,true)) {
            f.active();
            if(mode==2)f.elapsed(60_000);
            else {
                f.tower(0,1);
                if(mode==1)f.elapsed(20_000);
                else {
                    if(mode==3)f.engine.withdrawTimeAttack(f.players.get(0).owner());
                    for(int wave=0;wave<f.plan.waves().size();wave++) {
                        f.elapsed(1000+wave*1000L);assertEquals(ModernNpcBattle.Reason.ATTACKED,f.hit(mode==3?1:0,mode==3?wave+1:wave+2,f.living()).reason());
                    }
                }
            }
            var c=f.engine.serviceObservation();var labels=new String[]{"time-cleared","time-timeout","time-aborted","time-departed-win"};
            send(labels[mode],ModernBattleWireTest.publication(c,f.catalog).audience(f.players.get(mode==3?1:0).scope()));
        }
        try(var f=ModernBattleEngineNpcFireStormTest.fixture(1000,1,0,true,true,false,0)) {
            f.active();var npc=f.spawn(0,11,10);ModernBattleEngineNpcMagicTest.cast(f,0,1,npc);f.elapsed(1200);
            var c=f.engine.serviceObservation();assertTrue(c.npcs().roster().actors().isEmpty());
            send("npc-retired",ModernBattleWireTest.publication(c,f.config.catalog()).audience(f.players.get(0).scope()));
        }
    }
    private void modeCases()throws Exception {
        for(int kind=0;kind<3;kind++)try(var f=new ModernBattleEngineHonorTest.Fixture(1,100,100,1_000_000_000L)) {
            f.active();
            if(kind==0)assertEquals(ModernSpellProtocol.Reason.NONE,f.cast(0,1,4,4).reason());
            else if(kind==1)assertEquals(ModernBattleEngine.AttackStatus.ATTACKED,f.attack(0,1,4).status());
            else f.rooms.releasePreparation(f.ticket);
            send(new String[]{"honor-terminal-arrow","honor-terminal-melee","honor-aborted"}[kind],ModernBattleWireTest.publication(f.engine.serviceObservation(),null).audience(f.players.get(0).scope()));
        }
        try(var f=new ModernBattleEngineSiegeTest.Fixture(0,1,120_000,1000,0,true)) {
            send("siege-waiting",ModernBattleWireTest.publication(f.engine.serviceObservation(),null).audience(f.players.get(0).scope()));
            f.active();send("siege-outer",ModernBattleWireTest.publication(f.engine.serviceObservation(),null).audience(f.players.get(0).scope()));
            assertEquals(ModernBattleEngine.AttackStatus.ATTACKED,f.tower(0,1,ModernSiege.Slot.OUTER_LEFT).status());
            f.walk(0,ModernBattleEngineSiegeTest.cell(11,10));f.waitMillis(200);
            assertEquals(ModernBattleEngine.AttackStatus.ATTACKED,f.tower(0,2,ModernSiege.Slot.OUTER_RIGHT).status());
            send("siege-inner",ModernBattleWireTest.publication(f.engine.serviceObservation(),null).audience(f.players.get(0).scope()));
            f.walk(0,ModernBattleEngineSiegeTest.cell(29,10));f.walk(0,ModernBattleEngineSiegeTest.cell(29,9));f.walk(0,ModernBattleEngineSiegeTest.cell(31,9));f.waitMillis(200);
            assertEquals(ModernBattleEngine.AttackStatus.ATTACKED,f.tower(0,3,ModernSiege.Slot.INNER).status());
            send("siege-inner-win",ModernBattleWireTest.publication(f.engine.serviceObservation(),null).audience(f.players.get(0).scope()));
        }
        try(var f=new ModernBattleEngineSiegeTest.Fixture()) {
            f.active();f.rooms.releasePreparation(f.ticket);
            send("siege-aborted",ModernBattleWireTest.publication(f.engine.serviceObservation(),null).audience(f.players.get(0).scope()));
        }
        try(var f=new ModernBattleEnginePetRaceTest.Fixture()) {
            send("race-waiting",ModernBattleWireTest.publication(f.engine.serviceObservation(),null).audience(f.a.scope()));
            f.active();send("race-running",ModernBattleWireTest.publication(f.engine.serviceObservation(),null).audience(f.a.scope()));
            f.finish(0);send("race-first",ModernBattleWireTest.publication(f.engine.serviceObservation(),null).audience(f.a.scope()));
            f.finish(1);send("race-two",ModernBattleWireTest.publication(f.engine.serviceObservation(),null).audience(f.a.scope()));
        }
        for(int kind=0;kind<3;kind++)try(var f=new ModernBattleEnginePetRaceTest.Fixture()) {
            f.active();
            if(kind<2)f.engine.withdrawRacer(f.b.owner(),kind==0?ModernPetRace.Dnf.LEFT:ModernPetRace.Dnf.DISCONNECTED);
            else f.elapsed(30_000_000_000L);
            send(new String[]{"race-left","race-disconnected","race-aborted"}[kind],ModernBattleWireTest.publication(f.engine.serviceObservation(),null).audience(f.a.scope()));
        }
        try(var room=new ModernBattleEngineBotTest.RoomFixture(1,1)) {
            var ticket=room.capture();var clock=new java.util.concurrent.atomic.AtomicLong(1_000_000_000L);var p=room.players.get(0);
            try(var engine=new ModernBattleEngine(ticket,room.binding,room.prepared(ticket),room.bots(ticket),ModernBattleEngineBotTest.LIMITS,clock::get,()->1000,bound->0,true)) {
                engine.ackLoadedObservation(p.owner(),p.scope(),ticket.matchId(),ticket.generation(),room.map.digest());
                for(int n=1;n<=3;n++) {
                    assertEquals(ModernBattleEngine.AttackStatus.ATTACKED,engine.attack(p.owner(),p.scope(),ticket.matchId(),ticket.generation(),n,2).status());
                    clock.addAndGet(700_000_000L);
                }
                assertEquals(ModernBattleEngine.Phase.FINISHED,engine.tick().phase());
                send("elimination-terminal",ModernBattleWireTest.publication(engine.serviceObservation(),null).audience(p.scope()));
            }finally{room.rooms.releasePreparation(ticket);}
        }
    }
    private void hundred()throws Exception {
        try(var room=new ModernBattleEngineBotTest.RoomFixture(100,0)) {
            var ticket=room.capture();var prepared=new java.util.ArrayList<ModernBattleEngine.PreparedMember>();
            for(var p:room.prepared(ticket)) {int n=prepared.size();prepared.add(new ModernBattleEngine.PreparedMember(p.owner(),p.scope(),p.character(),ModernBattleEngineBotTest.cell(10+(n%10)*2,10+(n/10)*2),p.stepIntervalNanos(),p.fighter(),p.attackIntervalNanos(),p.skills(),p.castIntervalNanos(),p.damageMagic()));}
            try(var engine=new ModernBattleEngine(ticket,room.binding,prepared,java.util.List.of(),ModernBattleEngineBotTest.LIMITS,()->1_000_000_000L,()->1000,bound->0,false)) {
                for(var p:room.players)engine.ackLoadedObservation(p.owner(),p.scope(),ticket.matchId(),ticket.generation(),room.map.digest());
                for(var p:room.players)assertEquals(ModernSpellProtocol.Reason.NONE,engine.cast(p.owner(),new ModernSpellProtocol.Request(p.scope(),ticket.matchId(),ticket.generation(),1,5,0)).reason());
                send("hundred",ModernBattleWireTest.publication(engine.serviceObservation(),null).audience(room.players.get(0).scope()));
            }finally{room.rooms.releasePreparation(ticket);}
        }
    }
    private final java.util.Map<String,Integer> traceCounts=new java.util.LinkedHashMap<>();
    private void trace(String group,ModernBattleEngine engine,ModernRooms.Scope scope,ModernNpcCatalog.Catalog catalog)throws Exception {
        int index=traceCounts.getOrDefault(group,0);traceCounts.put(group,index+1);
        send(group+"-"+index,ModernBattleWireTest.publication(engine.serviceObservation(),catalog).audience(scope));
    }
    private void followRun()throws Exception {
        try(var f=new ModernBattleEngineHonorTest.Fixture(10000,10000,1000,1_000_000_000L)) {
            var p=f.players.get(0).scope();f.active();trace("arrow-player",f.engine,p,null);
            assertEquals(ModernSpellProtocol.Reason.NONE,f.cast(0,1,4,2).reason());trace("arrow-player",f.engine,p,null);
            f.elapsed(100_000_000L);assertEquals(ModernBattleEngine.MoveStatus.MOVED,f.move(1,1,2).status());trace("arrow-player",f.engine,p,null);
            f.elapsed(300_000_000L);assertEquals(ModernBattleEngine.MoveStatus.MOVED,f.move(1,2,2).status());trace("arrow-player",f.engine,p,null);
        }
        try(var f=new ModernBattleEngineHonorTest.Fixture(10000,1,1000,1_000_000_000L)) {
            var p=f.players.get(0).scope();f.active();f.elapsed(200_000_000L);
            assertEquals(ModernBattleEngine.MoveStatus.MOVED,f.move(1,1,2).status());trace("arrow-life",f.engine,p,null);
            var hit=f.cast(0,1,4,2);assertEquals(ModernSpellProtocol.Reason.NONE,hit.reason());assertEquals(0,hit.snapshot().vitals().get(1).hp());trace("arrow-life",f.engine,p,null);
            f.elapsed(5_200_000_000L);assertEquals(1,f.engine.tick().vitals().get(1).hp());trace("arrow-life",f.engine,p,null);
            f.elapsed(5_400_000_000L);assertEquals(ModernBattleEngine.MoveStatus.MOVED,f.move(1,2,4).status());trace("arrow-life",f.engine,p,null);
        }
        try(var f=new ModernBattleEngineHonorTest.Fixture(10000,10000,1000,1_000_000_000L)) {
            var p=f.players.get(0).scope();f.active();trace("arrow-honor",f.engine,p,null);
            assertEquals(ModernSpellProtocol.Reason.NONE,f.cast(0,1,4,4).reason());trace("arrow-honor",f.engine,p,null);trace("arrow-honor",f.engine,p,null);
        }
        for(boolean hurt:new boolean[]{false,true})try(var f=ModernBattleEngineNpcFireStormTest.fixture()) {
            String group=hurt?"arrow-hurt":"arrow-npc";var p=f.players.get(0).scope();f.active();var npc=f.spawn(0,hurt?12:15,10);f.randomValue=19;
            trace(group,f.engine,p,f.config.catalog());assertEquals(ModernNpcMagic.Reason.NONE,ModernBattleEngineNpcMagicTest.cast(f,0,1,npc).reason());
            trace(group,f.engine,p,f.config.catalog());f.elapsed(100);assertEquals(ModernNpcBattle.Reason.MOVED,f.step(npc).reason());trace(group,f.engine,p,f.config.catalog());
            f.elapsed(200);assertEquals(hurt?ModernNpcBattle.Reason.ATTACKED:ModernNpcBattle.Reason.MOVED,f.step(npc).reason());
            if(hurt)assertTrue(f.view().world().vitals().get(0).hp()>0&&f.view().world().vitals().get(0).hp()<100);
            trace(group,f.engine,p,f.config.catalog());
        }
        try(var f=ModernBattleEngineNpcFireStormTest.fixture(1000,1,0,true,false,false,0)) {
            var p=f.players.get(0).scope();f.active();f.spawn(0,11,10);f.spawn(0,10,11);trace("spell70-npc",f.engine,p,f.config.catalog());
            var hit=ModernBattleEngineNpcFireStormTest.cast(f,1);assertEquals(ModernNpcFireStormState.Reason.NONE,hit.reason());assertEquals(2,hit.frame().world().npcFireStorms().get(0).targets().size());
            trace("spell70-npc",f.engine,p,f.config.catalog());f.elapsed(1200);assertTrue(f.view().npcs().actors().isEmpty());trace("spell70-npc",f.engine,p,f.config.catalog());
        }
        for(boolean critical:new boolean[]{false,true})try(var f=ModernBattleEngineDisintegrateTest.fixture(true,0,1,1000,500,false)) {
            String group=critical?"spell77-critical":"spell77-normal";var p=f.players.get(0).scope();
            f.active();f.randomOverride=b->b==100?(critical?0:99):0;trace(group,f.engine,p,null);
            var killed=ModernBattleEngineDisintegrateTest.cast(f,1,2);assertEquals(ModernDisintegrateState.Reason.NONE,killed.reason());
            assertEquals(0,killed.snapshot().vitals().get(1).hp());assertEquals(critical,killed.snapshot().disintegrations().get(0).hit().critical());
            trace(group,f.engine,p,null);f.elapsed(5_000_000_000L);assertEquals(1,f.engine.tick().vitals().get(1).hp());trace(group,f.engine,p,null);
        }
        try(var f=ModernBattleEngineFireStormTest.fixture(true,0,1000,40)) {
            var p=f.players.get(0).scope();f.active();trace("spell70",f.engine,p,null);
            var cast=f.cast(0,1,70,0);assertEquals(ModernSpellProtocol.Reason.NONE,cast.reason());
            assertEquals(2,cast.snapshot().fireStorms().get(0).targets().size());assertEquals(0,cast.snapshot().vitals().get(1).hp());
            trace("spell70",f.engine,p,null);f.elapsed(5_000_000_000L);trace("spell70",f.engine,p,null);
        }
        try(var f=ModernBattleEngineFireStormTest.fixture(true,0,1000,1000)) {
            var p=f.players.get(0).scope();f.active();
            for(int n=1;n<=4;n++){f.elapsed(n*1_000_000_000L);assertEquals(ModernBattleEngine.MoveStatus.MOVED,f.move(0,n,6).status());}
            f.elapsed(5_000_000_000L);trace("spell70-empty",f.engine,p,null);
            var cast=f.cast(0,1,70,0);assertEquals(ModernSpellProtocol.Reason.NONE,cast.reason());assertTrue(cast.snapshot().fireStorms().get(0).targets().isEmpty());
            trace("spell70-empty",f.engine,p,null);trace("spell70-empty",f.engine,p,null);
        }
        try(var f=ModernBattleEngineNpcDisintegrateTest.fixture()) {
            var p=f.players.get(0).scope();f.active();var npc=f.spawn(0,11,10);f.randomValue=0;trace("spell77-npc",f.engine,p,f.config.catalog());
            assertEquals(ModernNpcDisintegrateState.Reason.NONE,ModernBattleEngineNpcDisintegrateTest.cast(f,0,1,npc).reason());
            trace("spell77-npc",f.engine,p,f.config.catalog());trace("spell77-npc",f.engine,p,f.config.catalog());
        }
        try(var f=new ModernBattleEngineHonorTest.Fixture()) {
            // Observe another player's public receipts: denied request numbers
            // cannot later be reused by a successful cast under a newer revision.
            var p=f.players.get(1).scope();f.active();trace("requests",f.engine,p,null);
            org.junit.jupiter.api.Assertions.assertEquals(ModernSpellProtocol.Reason.SAME_TEAM,f.cast(0,1,1,4).reason());
            org.junit.jupiter.api.Assertions.assertEquals(ModernSpellProtocol.Reason.SAME_TEAM,f.cast(0,2,1,4).reason());
            trace("requests",f.engine,p,null);
            org.junit.jupiter.api.Assertions.assertEquals(ModernSpellProtocol.Reason.NONE,f.cast(0,3,1,3).reason());
            trace("requests",f.engine,p,null);
            org.junit.jupiter.api.Assertions.assertEquals(ModernSpellProtocol.Reason.TOO_FAST,f.cast(0,4,1,3).reason());
            trace("requests",f.engine,p,null);f.elapsed(2_000_000_000L);
            org.junit.jupiter.api.Assertions.assertEquals(ModernSpellProtocol.Reason.NONE,f.cast(0,5,1,3).reason());
            trace("requests",f.engine,p,null);
        }
        try(var f=new ModernBattleEngineHonorTest.Fixture(1,1,100,1_000_000_000L)) {
            var p=f.players.get(0).scope();trace("honor",f.engine,p,null);f.active();trace("honor",f.engine,p,null);
            f.elapsed(100_000_000L);trace("honor",f.engine,p,null);f.elapsed(1_000_000_000L);trace("honor",f.engine,p,null);
            f.cast(0,1,1,3);trace("honor",f.engine,p,null);f.elapsed(3_000_000_000L);f.cast(0,2,4,2);trace("honor",f.engine,p,null);
            f.elapsed(8_000_000_000L);trace("honor",f.engine,p,null);f.cast(0,3,4,4);trace("honor",f.engine,p,null);
            f.elapsed(100_000_000_000L);trace("honor",f.engine,p,null);
        }
        try(var f=new ModernBattleEngineHonorTest.Fixture()) {
            var p=f.players.get(0).scope();f.active();trace("teleport",f.engine,p,null);
            f.cast(0,1,5,0);trace("teleport",f.engine,p,null);f.elapsed(2_000_000_000L);f.cast(0,2,5,0);trace("teleport",f.engine,p,null);
            f.elapsed(4_000_000_000L);boolean moved=false;
            for(int heading=0;heading<8;heading++)if(f.move(0,heading+1,heading).status()==ModernBattleEngine.MoveStatus.MOVED){moved=true;break;}
            assertTrue(moved,"teleport destination must allow a confirmed next move");trace("teleport",f.engine,p,null);
        }
        try(var f=new ModernBattleItemRuntimeTest.Fixture()) {
            var p=f.game.players.get(0).scope();f.game.active();trace("equipment",f.game.engine,p,null);
            f.game.cast(0,1,1,3);ModernBattleLifeProofTest.kill(f);trace("equipment",f.game.engine,p,null);
            f.game.clock.addAndGet(5_000_000_000L);trace("equipment",f.game.engine,p,null);
            f.equipped(false);trace("equipment",f.game.engine,p,null);
            ModernBattleLifeProofTest.kill(f);f.game.clock.addAndGet(5_000_000_000L);trace("equipment",f.game.engine,p,null);
        }
        try(var f=ModernQuestLifeTest.fixture(2)) {
            var p=f.players.get(0).scope();var c=f.config.catalog();trace("quest",f.engine,p,c);f.active();trace("quest",f.engine,p,c);
            var dead=ModernQuestLifeTest.die(f,100);trace("quest",f.engine,p,c);ModernQuestLifeTest.confirm(f,0,ModernQuestLifeTest.life(dead,0));trace("quest",f.engine,p,c);
            f.rooms.releasePreparation(f.ticket);trace("quest",f.engine,p,c);trace("quest",f.engine,p,c);
        }
        try(var f=new ModernTimeAttackEngineTest.Fixture(2,8,100,true)) {
            var p=f.players.get(0).scope();trace("time",f.engine,p,f.catalog);f.active();trace("time",f.engine,p,f.catalog);
            f.tower(0,1);trace("time",f.engine,p,f.catalog);
            for(int wave=0;wave<f.plan.waves().size();wave++) {f.elapsed(1000+wave*1000L);f.hit(0,wave+2,f.living());trace("time",f.engine,p,f.catalog);}
            trace("time",f.engine,p,f.catalog);
        }
        try(var f=new ModernTimeAttackEngineTest.Fixture(2,8,100,true)) {
            var p=f.players.get(0).scope();f.active();f.tower(0,1);trace("time-leave",f.engine,p,f.catalog);
            f.engine.withdrawTimeAttack(f.players.get(0).owner());trace("time-leave",f.engine,p,f.catalog);
            f.engine.withdrawTimeAttack(f.players.get(1).owner());trace("time-leave",f.engine,p,f.catalog);
        }
        try(var f=new ModernBattleEnginePetRaceTest.Fixture()) {
            var p=f.a.scope();trace("race",f.engine,p,null);f.active();trace("race",f.engine,p,null);
            f.elapsed(1);trace("race",f.engine,p,null);
            f.step(0,ModernPetRaceTest.FIRST_GATE);trace("race",f.engine,p,null);
            f.step(0,ModernPetRaceTest.MIDDLE);trace("race",f.engine,p,null);
            f.finish(0);trace("race",f.engine,p,null);
            f.finish(1);trace("race",f.engine,p,null);
            var first=f.engine.serviceObservation().modes().race();f.elapsed(first.finishWindowDeadlineElapsedNanos());trace("race",f.engine,p,null);trace("race",f.engine,p,null);
        }
        try(var f=new ModernBattleEngineSiegeTest.Fixture(0,1,120_000,1000,0,true)) {
            var p=f.players.get(0).scope();trace("siege",f.engine,p,null);f.active();trace("siege",f.engine,p,null);
            f.tower(0,1,ModernSiege.Slot.OUTER_LEFT);trace("siege",f.engine,p,null);
            f.walk(0,ModernBattleEngineSiegeTest.cell(11,10));f.waitMillis(200);f.tower(0,2,ModernSiege.Slot.OUTER_RIGHT);trace("siege",f.engine,p,null);
            f.walk(0,ModernBattleEngineSiegeTest.cell(29,10));f.walk(0,ModernBattleEngineSiegeTest.cell(29,9));f.walk(0,ModernBattleEngineSiegeTest.cell(31,9));f.waitMillis(200);
            f.tower(0,3,ModernSiege.Slot.INNER);trace("siege",f.engine,p,null);trace("siege",f.engine,p,null);
        }
        try(var f=ModernBattleEngineNpcFireStormTest.fixture(1000,1,0,true,true,false,0)) {
            var p=f.players.get(0).scope();f.active();var npc=f.spawn(0,11,10);trace("npc",f.engine,p,f.config.catalog());
            ModernBattleEngineNpcMagicTest.cast(f,0,1,npc);trace("npc",f.engine,p,f.config.catalog());
            f.elapsed(1200);trace("npc",f.engine,p,f.config.catalog());
        }
        try(var f=new ModernBattleEngineNpcTest.Fixture()) {
            var p=f.players.get(0).scope();f.active();var npc=f.spawn(0,13,10);f.randomValue=19;
            trace("motion",f.engine,p,f.config.catalog());
            for(int tick=1;tick<=2;tick++) {
                f.elapsed(tick*100);assertEquals(ModernNpcBattle.Reason.MOVED,f.step(npc).reason());
                trace("motion",f.engine,p,f.config.catalog());
            }
            f.elapsed(300);var hit=f.step(npc);assertEquals(ModernNpcBattle.Reason.ATTACKED,hit.reason());
            assertTrue(hit.frame().world().vitals().get(0).hp()>0&&hit.frame().world().vitals().get(0).hp()<100);
            trace("motion",f.engine,p,f.config.catalog());
            var hurt=f.attack(0,1,npc);assertEquals(ModernNpcBattle.Reason.ATTACKED,hurt.reason());
            assertTrue(hurt.frame().npcs().actors().get(0).hp()>0&&hurt.frame().npcs().actors().get(0).hp()<npc.hp());
            trace("motion",f.engine,p,f.config.catalog());trace("motion",f.engine,p,f.config.catalog());
        }
        System.out.println("CLOSED:"+count);System.out.flush();
    }
    private void speedReadRun()throws Exception {
        var text=new StringBuilder();int ch;
        while((ch=input.read())!=-1&&ch!='\n'){if(ch=='\r')continue;if(text.length()>=64)throw new IllegalArgumentException("oversized hello");text.append((char)ch);}
        var boundary=new ModernSessionBoundary(new ModernSessionBoundary.Policy(ModernSessionProtocol.KNOWN_CAPABILITIES,7,46));
        var ack=boundary.acceptHello(HexFormat.of().parseHex(text.toString()));
        var negotiated=boundary.negotiated().orElseThrow();assertTrue(ModernBattleSpeedWire.negotiated(negotiated));
        System.out.println("ACK:"+HexFormat.of().formatHex(ack));System.out.flush();
        ModernSpeedWireTest.trace((label,view)->{
            assertTrue(view.world().descriptor().supportedBy(negotiated.version(),negotiated.capabilities()));
            var echo=new ModernBattleReadProtocol.Request(view.personal().scope(),view.world().identity());
            var response=new ModernApplicationProtocol.BattleReadResult(77,echo,new ModernBattleReadProtocol.Reply(ModernBattleReadProtocol.State.DATA,view));
            var admitted=ModernMatchDescriptor.forPeer(response,negotiated);
            var bytes=ModernApplicationProtocol.encode(admitted,negotiated.version());
            System.out.println("READ:"+label+":"+HexFormat.of().formatHex(bytes));System.out.flush();digestAck(bytes);
        });
        System.out.println("CLOSED:"+count);System.out.flush();boundary.close();
    }
    private void speedRun()throws Exception {
        ModernSpeedWireTest.trace((label,view)->{
            var bytes=ModernBattleWire.encodeVersioned(view,46);
            System.out.println("SPEED:"+label+":"+HexFormat.of().formatHex(bytes));System.out.flush();
            digestAck(bytes);
        });
        System.out.println("CLOSED:"+count);System.out.flush();
    }
    /** Test-only deterministic first-cast miss from the real engine. No
     * production RNG, SQL source or service configuration is changed. */
    private void speedFirstMissRun()throws Exception {
        try(var f=ModernBattleSlowTest.fixture(2,100,1000,false)) {
            var before=ModernBattleSlowTest.active(f);
            ModernSpeedWireTest.Sink sink=(label,view)->{
                var bytes=ModernBattleWire.encodeVersioned(view,46);
                System.out.println("SPEED:"+label+":"+HexFormat.of().formatHex(bytes));System.out.flush();
                digestAck(bytes);
            };
            ModernSpeedWireTest.pair(sink,"first-before",before,null);
            // The fixture's default bounded RNG returns bound-1: roll 100,
            // while native-qualified Slow probability is capped at 90.
            var cast=ModernBattleSlowTest.cast(f,0,1,1);
            assertEquals(ModernSlowCast.Reason.NONE,cast.reason());
            var after=cast.observation();
            var receipt=(ModernSlowReceipt)after.actors().get(0).runtime()
                    .receipts().get(ModernUseHistory.Lane.SLOW).detail();
            assertEquals(ModernSlowReceipt.Outcome.MISS,receipt.outcome());
            assertEquals(100,receipt.roll());
            assertEquals(1,after.actors().get(0).materials().uses());
            assertEquals(80,after.actors().get(0).resources().vitals().mp());
            assertEquals(0,after.actors().get(1).buffs().slowUntilNanos());
            ModernSpeedWireTest.pair(sink,"first-miss",after,null);
            // The next planned command addresses the caster, not a retry of
            // the missed source-42 target. It uses the second stock once.
            f.elapsed(2_000_000_000L);f.randomMode=3;
            var self=ModernBattleSlowTest.cast(f,0,2,0);
            assertEquals(ModernSlowCast.Reason.NONE,self.reason());
            var continued=self.observation();
            var selfReceipt=(ModernSlowReceipt)continued.actors().get(0).runtime()
                    .receipts().get(ModernUseHistory.Lane.SLOW).detail();
            assertEquals(ModernSlowReceipt.Outcome.APPLIED,selfReceipt.outcome());
            assertEquals(2,continued.actors().get(0).materials().uses());
            assertEquals(60,continued.actors().get(0).resources().vitals().mp());
            assertEquals(0,continued.actors().get(1).buffs().slowUntilNanos());
            ModernSpeedWireTest.pair(sink,"next-self",continued,null);
        }
        assertEquals(6,count);
        System.out.println("CLOSED:"+count);System.out.flush();
    }
    public static void main(String[] args)throws Exception {
        if(args.length==1&&args[0].equals("vip10")){ModernBattleVipWirePeer.main(new String[0]);return;}
        if(args.length==1&&args[0].equals("vip-selected")){ModernVipSelectedProtocolTest.main(new String[0]);return;}
        if(args.length==1&&args[0].equals("vip-appearance")){ModernVipAppearanceTest.main(new String[0]);return;}
        if(args.length==1&&args[0].equals("vip-appearance-shared")){ModernVipAppearanceTest.main(new String[]{"shared"});return;}
        if(args.length==1&&args[0].equals("vip-appearance-real-map")){ModernVipAppearanceTest.main(new String[]{"real-map"});return;}
        if(args.length==1&&args[0].equals("vip-appearance-body")){ModernVipAppearanceTest.main(new String[]{"body"});return;}
        if(args.length==1&&args[0].equals("vip-appearance-cast")){ModernVipAppearanceTest.main(new String[]{"cast"});return;}
        if(args.length==1&&args[0].equals("vip-appearance-utility")){ModernVipAppearanceTest.main(new String[]{"utility"});return;}
        if(args.length==1&&args[0].equals("vip-appearance-walk")){ModernVipAppearanceTest.main(new String[]{"walk"});return;}
        if(args.length==1&&args[0].equals("vip-selected-shared-armor-gfx0")){ModernVipSelectedProtocolTest.main(new String[]{"shared-armor-gfx0"});return;}
        if(args.length==1&&args[0].equals("vip-equipment")){ModernVipEquipmentProtocolTest.main(new String[0]);return;}
        if(args.length==1&&args[0].equals("armor8"))new ModernBattleWirePeer().armorRun();
        else if(args.length==1&&args[0].equals("armor-render"))new ModernBattleWirePeer().armorRenderRun();
        else if(args.length==1&&args[0].equals("armor-lifecycle"))new ModernBattleWirePeer().armorLifecycleRun();
        else if(args.length==1&&args[0].equals("armor9"))new ModernBattleWirePeer().armorPresentationRun();
        else if(args.length==1&&args[0].equals("armor-selected"))new ModernBattleWirePeer().armorSelectedRun();
        else if(args.length==1&&args[0].equals("healing7"))new ModernBattleWirePeer().healingRun(false);
        else if(args.length==1&&args[0].equals("healing-command"))new ModernBattleWirePeer().healingCommandRun();
        else if(args.length==1&&args[0].equals("healing7-scene"))new ModernBattleWirePeer().healingRun(true);
        else if(args.length==1&&args[0].equals("healing7-replies"))new ModernBattleWirePeer().healingRepliesRun();
        else if(args.length==1&&args[0].equals("speed46-read"))new ModernBattleWirePeer().speedReadRun();
        else if(args.length==1&&args[0].equals("speed29"))new ModernBattleWirePeer().speedRun();
        else if(args.length==1&&args[0].equals("speed-first-miss"))new ModernBattleWirePeer().speedFirstMissRun();
        else if(args.length==1&&args[0].equals("haste43-command"))new ModernBattleWirePeer().targetHasteCommandRun();
        else if(args.length==1&&args[0].equals("haste43"))new ModernBattleWirePeer().targetHasteRun();
        else if(args.length==1&&args[0].equals("haste43-time"))new ModernBattleWirePeer().targetHasteTimeRun();
        else if(args.length==1&&args[0].equals("npc-motion-presentation"))new ModernBattleWirePeer().npcMotionPresentationRun();
        else if(args.length==1&&args[0].equals("npc-motion"))new ModernBattleWirePeer().npcMotionRun();
        else if(args.length==1&&args[0].equals("greater-haste-command"))new ModernBattleWirePeer().greaterHasteCommandRun();
        else if(args.length==1&&args[0].equals("greater-haste")){var p=new ModernBattleWirePeer();p.greaterHaste=true;p.greaterHasteRun();}
        else if(args.length==1&&args[0].equals("presentation")){var p=new ModernBattleWirePeer();p.presentation=true;p.presentationRun();}
        else if(args.length==1&&args[0].equals("item-effects")){var p=new ModernBattleWirePeer();p.presentation=true;p.itemEffectsRun();}
        else if(args.length==1&&args[0].equals("bow-effects")){var p=new ModernBattleWirePeer();p.presentation=true;p.bowEffectsRun();}
        else if(args.length==1&&args[0].equals("healing-effects")){var p=new ModernBattleWirePeer();p.presentation=true;p.healingEffectsRun();}
        else if(args.length==1&&args[0].equals("durable-equipment")){var p=new ModernBattleWirePeer();p.presentation=true;p.durableEquipmentRun();}
        else if(args.length==1&&args[0].equals("service-equipment")){var p=new ModernBattleWirePeer();p.presentation=true;p.serviceEquipmentRun();}
        else if(args.length==1&&args[0].equals("operations"))new ModernBattleWirePeer().operationsRun();
        else if(args.length==1&&args[0].equals("world-lifecycle"))new ModernBattleWirePeer().worldLifecycleRun();
        else if(args.length==1&&args[0].equals("follow"))new ModernBattleWirePeer().followRun();
        else if(args.length==1&&args[0].equals("player-input"))new ModernBattleWirePeer().playerInputRun();
        else if(args.length==0)new ModernBattleWirePeer().run();else throw new IllegalArgumentException("unknown peer mode");
    }
    private void armorRenderRun()throws Exception {
        ModernBattleArmorRenderTraceTest.trace((label,ext,request,reply)->{
            System.out.println("REPLY:"+label+":"+ext+":"+HexFormat.of().formatHex(request)+":"+HexFormat.of().formatHex(reply));
            System.out.flush();digestAck(reply);
        });
        System.out.println("CLOSED:"+count);System.out.flush();
    }
    private void armorLifecycleRun()throws Exception {
        var root=Files.createTempDirectory("modern-armor-lifecycle-").toRealPath();
        System.err.println("ARMOR_LIFECYCLE_FIXTURE:"+root);
        ModernBattleArmorLifecycleTraceTest.trace(root,(label,ext,request,reply)->{
            System.out.println("REPLY:"+label+":"+ext+":"+HexFormat.of().formatHex(request)+":"+HexFormat.of().formatHex(reply));
            System.out.flush();digestAck(reply);
        });
        System.out.println("CLOSED:"+count);System.out.flush();
    }
    private void armorPresentationRun()throws Exception {
        var root=Files.createTempDirectory("modern-armor-presentation-").toRealPath();
        // Keep this exact owned fixture directory for failure diagnosis as well
        // as successful cross-language evidence. Never clean another run.
        System.err.println("ARMOR_PRESENTATION_FIXTURE:"+root);
        for(int ext:new int[]{12,13,15}) {
            var h=ModernArmorSelectedProtocolTest.handshake(ext);
            System.out.println("SESSION:"+ext+":"+HexFormat.of().formatHex(h.hello())+":"+HexFormat.of().formatHex(h.ack()));
        }
        System.out.flush();
        ModernArmorSelectedProtocolTest.Sink sink=(label,ext,request,reply)->{
            System.out.println("REPLY:"+label+":"+ext+":"+HexFormat.of().formatHex(request)+":"+HexFormat.of().formatHex(reply));
            System.out.flush();digestAck(reply);
        };
        ModernBattleArmorPresentationWireTest.selectedTrace(Files.createDirectory(root.resolve("read")),sink);
        ModernArmorSelectedProtocolTest.healingTrace(Files.createDirectory(root.resolve("heal")),sink,15);
        System.out.println("CLOSED:"+count);System.out.flush();
    }
    private void armorSelectedRun()throws Exception {
        var root=Files.createTempDirectory("modern-armor-selected-").toRealPath();
        try {
            for(int ext:new int[]{4,5,7}) {
                var h=ModernArmorSelectedProtocolTest.handshake(ext);
                System.out.println("SESSION:"+ext+":"+HexFormat.of().formatHex(h.hello())+":"+HexFormat.of().formatHex(h.ack()));
            }
            System.out.flush();
            ModernArmorSelectedProtocolTest.Sink sink=(label,ext,request,reply)->{
                System.out.println("REPLY:"+label+":"+ext+":"+HexFormat.of().formatHex(request)+":"+HexFormat.of().formatHex(reply));
                System.out.flush();digestAck(reply);
            };
            ModernArmorSelectedProtocolTest.trace(Files.createDirectory(root.resolve("read")),sink);
            ModernArmorSelectedProtocolTest.healingTrace(Files.createDirectory(root.resolve("heal")),sink);
            System.out.println("CLOSED:"+count);System.out.flush();
        } finally {
            try(var paths=Files.walk(root)){for(var path:paths.sorted(Comparator.reverseOrder()).toList()) {
                if(!path.toAbsolutePath().normalize().startsWith(root))throw new IllegalStateException("fixture cleanup scope");Files.delete(path);
            }}
        }
    }
    private void armorRun()throws Exception {
        var root=Files.createTempDirectory("modern-armor-wire-").toRealPath();
        try {
            ModernBattleArmorWireTest.trace(root,(label,view)->{
                var bytes=ModernBattleWire.encodeArmorProtection(view);
                System.out.println("FRAME:"+label+":"+HexFormat.of().formatHex(bytes));System.out.flush();digestAck(bytes);
            });
            System.out.println("CLOSED:"+count);System.out.flush();
        } finally {
            try(var paths=Files.walk(root)){for(var path:paths.sorted(Comparator.reverseOrder()).toList()) {
                if(!path.toAbsolutePath().normalize().startsWith(root))throw new IllegalStateException("fixture cleanup scope");Files.delete(path);
            }}
        }
    }
    private void healingRepliesRun()throws Exception {
        ModernPlayerHealingRepliesPeer.trace((label,request,reply)->{
            System.out.println("REPLY:"+label+":"+HexFormat.of().formatHex(request)+":"+HexFormat.of().formatHex(reply));
            System.out.flush();digestAck(reply);
        });
        System.out.println("CLOSED:"+count);System.out.flush();
    }
    /** Actual engine states missing from the general temporal fixture: a live
     * quest objective and the defender at both ends of an accepted portal. */
    private void playerInputRun()throws Exception {
        try(var f=ModernNpcFireStormStructuresTest.quest(513,100)) {
            f.active();send("input-quest-before",ModernBattleWireTest.publication(f.engine.serviceObservation(),f.config.catalog()).audience(f.players.get(0).scope()));
            assertEquals(ModernNpcFireStormState.Reason.NONE,ModernNpcFireStormStructuresTest.cast(f,0,1).reason());
            var objective=ModernQuestObjectivesTest.objective(f,0);assertEquals(100,objective.hp());
            send("input-quest-objective",ModernBattleWireTest.publication(f.engine.serviceObservation(),f.config.catalog()).audience(f.players.get(0).scope()));
        }
        try(var f=new ModernBattleEngineSiegeTest.Fixture()) {
            f.active();f.walk(1,ModernBattleEngineSiegeTest.cell(31,11));f.waitMillis(200);
            send("input-portal-before",ModernBattleWireTest.publication(f.engine.serviceObservation(),null).audience(f.players.get(1).scope()));
            assertEquals(ModernSiege.PortalReason.MOVED,f.portal(1).reason());
            send("input-portal-after",ModernBattleWireTest.publication(f.engine.serviceObservation(),null).audience(f.players.get(1).scope()));
        }
        System.out.println("CLOSED:"+count);System.out.flush();
    }
    private void pair(String name,ModernBattleObservation.Capture c,ModernRooms.Scope a,ModernRooms.Scope b)throws Exception {
        var p=ModernBattleWireTest.publication(c,null);send(name+"-a",p.audience(a));send(name+"-b",p.audience(b));
    }
    private void operation(String label,ModernBattleLifecycleServiceTest.Fixture f,ModernApplicationProtocol.BattleOperationRequest request)throws Exception {
        var result=f.service.operation(f.player(0).owner(),request);
        var bytes=ModernBattleOperationProtocolTest.encoded(result);
        var expected=ModernApplicationProtocol.encode(request,38);
        System.out.println("OPERATION:"+label+":"+HexFormat.of().formatHex(expected)+":"+HexFormat.of().formatHex(bytes));System.out.flush();
        digestAck(bytes);
    }
    private void operationsRun()throws Exception {
        var root=Files.createTempDirectory("modern-operation-wire-").toRealPath();
        try {
            try(var f=new ModernBattleLifecycleServiceTest.Fixture(root,null,38)) {
                f.reprice();
                operation("mixed-move",f,ModernBattleOperationProtocolTest.move(f,++f.moves));
                assertThrows(IllegalArgumentException.class,()->ModernLegacyBattleProjection.from(f.engine.serviceObservation()));
                Thread.sleep(2);
                var spell=new ModernApplicationProtocol.CastRequest(10,new ModernSpellProtocol.Request(f.player(0).scope(),f.world.matchId(),f.world.generation(),3,70,0));
                operation("mixed-no-mp",f,new ModernApplicationProtocol.BattleOperationRequest(10,new ModernBattleActionProtocol.Request(
                        ModernBattleCommandLifeTest.proof(f.engine,0),ModernBattleActionProtocol.NO_TARGET,spell)));
                f.move(0);f.win();
                ModernBattleLifecycleServiceTest.ModernWorldServiceItemTestAwait.until(()->f.lifecycle().settlement()==ModernWorldProtocol.SettlementState.SAVED);
                operation("mixed-saved",f,ModernBattleOperationProtocolTest.move(f,++f.moves));
                assertEquals(1,f.persisted.get());assertEquals(f.event.get(),f.lifecycle().result());
                f.leave(0);f.leave(1);assertEquals(0,f.service.matchCount());
            }
        } finally {
            try(var paths=Files.walk(root)){for(var path:paths.sorted(Comparator.reverseOrder()).toList()) {
                if(!path.toAbsolutePath().normalize().startsWith(root))throw new IllegalStateException("fixture cleanup scope");Files.delete(path);
            }}
        }
        System.out.println("CLOSED:"+count);System.out.flush();
    }
    private ModernBattleWorldProtocol.Reply world(String label,ModernBattleLifecycleServiceTest.Fixture f,int player,
            ModernBattleWorldProtocol.Action action,boolean discovery)throws Exception {
        var q=new ModernBattleWorldProtocol.Request(f.player(player).scope(),action,
                discovery?0:f.world.room().summary().id(),0,discovery?0:f.world.matchId(),discovery?0:f.world.generation(),
                action==ModernBattleWorldProtocol.Action.LOADED?f.world.manifest().digest():ModernWorldProtocol.ZERO_DIGEST);
        var reply=f.service.battleWorld(f.player(player).owner(),q);
        var expected=ModernApplicationProtocol.encode(new ModernApplicationProtocol.BattleWorldRequest(count+1,q),39);
        var bytes=ModernApplicationProtocol.encode(new ModernApplicationProtocol.BattleWorldResult(count+1,q,reply),39);
        System.out.println("WORLD:"+label+":"+HexFormat.of().formatHex(expected)+":"+HexFormat.of().formatHex(bytes));System.out.flush();
        digestAck(bytes);return reply;
    }
    private void worldLifecycleRun()throws Exception {
        var root=Files.createTempDirectory("modern-world-lifecycle-wire-").toRealPath();
        try {
            try(var f=new ModernBattleLifecycleServiceTest.Fixture(root,null,39)) {
                f.reprice();assertThrows(IllegalArgumentException.class,f::legacyRead);
                world("mixed-read-a",f,0,ModernBattleWorldProtocol.Action.READ,false);
                world("mixed-read-b",f,1,ModernBattleWorldProtocol.Action.READ,false);
                world("mixed-loaded",f,0,ModernBattleWorldProtocol.Action.LOADED,false);
                f.win();ModernBattleLifecycleServiceTest.ModernWorldServiceItemTestAwait.until(()->f.lifecycle().settlement()==ModernWorldProtocol.SettlementState.SAVED);
                var saved=world("saved-read-a",f,0,ModernBattleWorldProtocol.Action.READ,false);
                assertEquals(f.event.get(),saved.view().world().result());
                world("leave-a",f,0,ModernBattleWorldProtocol.Action.LEAVE,false);
                var retained=world("saved-read-b",f,1,ModernBattleWorldProtocol.Action.READ,false);
                assertEquals(saved.view().world().result(),retained.view().world().result());
                world("detached-a",f,0,ModernBattleWorldProtocol.Action.READ,true);
                world("leave-b",f,1,ModernBattleWorldProtocol.Action.LEAVE,false);
                assertEquals(1,f.persisted.get());assertEquals(0,f.service.matchCount());
            }
        } finally {
            try(var paths=Files.walk(root)){for(var path:paths.sorted(Comparator.reverseOrder()).toList()) {
                if(!path.toAbsolutePath().normalize().startsWith(root))throw new IllegalStateException("fixture cleanup scope");Files.delete(path);
            }}
        }
        System.out.println("CLOSED:"+count);System.out.flush();
    }
    private void healingCommandRun()throws Exception {
        var root=Files.createTempDirectory("modern-healing-command-wire-").toRealPath();
        try {
            ModernPlayerHealingProtocolTest.trace(root,new ModernPlayerHealingProtocolTest.Sink(){
                @Override public byte[] request(String label,ModernBattleAudience.View before,ModernPlayerHealingProtocol.Request q)throws Exception {
                    var previous=ModernBattleWire.encodePlayerHealing(before);
                    System.out.println("HEAL-BEGIN:"+label+":"+HexFormat.of().formatHex(previous)+":"+q.target().participant().entityId()+":"+Integer.toUnsignedString(q.requestId()));System.out.flush();
                    // The peer supplies its own encoded intent, not a digest of
                    // Java's request. Bound input before parsing/allocation.
                    var hex=new StringBuilder();int ch;
                    while((ch=input.read())!=-1&&ch!='\n'){
                        if(ch=='\r')continue;if(hex.length()>=2*ModernPlayerHealingProtocol.REQUEST_BYTES)throw new IllegalArgumentException("oversized healing intent");hex.append((char)ch);
                    }
                    if(ch==-1||hex.length()!=2*ModernPlayerHealingProtocol.REQUEST_BYTES)throw new IllegalArgumentException("truncated healing intent");
                    return HexFormat.of().parseHex(hex.toString());
                }
                @Override public void accept(String label,ModernBattleAudience.View before,ModernPlayerHealingProtocol.Request q,ModernPlayerHealingProtocol.Reply r)throws Exception {
                    var result=ModernPlayerHealingProtocol.encodeCandidateReply(r);
                    System.out.println("HEAL-REPLY:"+label+":"+HexFormat.of().formatHex(result));System.out.flush();digestAck(result);
                }
            });
        }finally{
            try(var paths=Files.walk(root)){for(var path:paths.sorted(Comparator.reverseOrder()).toList()) {
                if(!path.toAbsolutePath().normalize().startsWith(root))throw new IllegalStateException("fixture cleanup scope");Files.delete(path);
            }}
        }
        System.out.println("CLOSED:"+count);System.out.flush();
    }
    private void healingFrames(String label,ModernBattleObservation.Capture c)throws Exception {
        healingFrames(label,c,null);
    }
    private void healingFrames(String label,ModernBattleObservation.Capture c,ModernNpcCatalog.Catalog catalog)throws Exception {
        var p=ModernBattleHealingWireTest.publication(c,catalog);
        for(var scope:c.ownerViews(c.world()).keySet().stream().sorted(Comparator.comparingInt(ModernRooms.Scope::sourceId)).toList()) {
            var raw=ModernBattleHealingWireTest.check(p.audience(scope));
            System.out.println("FRAME:"+label+"-"+scope.sourceId()+":"+HexFormat.of().formatHex(raw));System.out.flush();digestAck(raw);
        }
    }
    private void healingRun(boolean scene)throws Exception {
        try(var f=ModernBattlePlayerHealingTest.fixture(1,100,100)) {
            healingFrames("loading",f.engine.serviceObservation());healingFrames("active",ModernBattlePlayerHealingTest.active(f));
            healingFrames("bot",ModernBattlePlayerHealingTest.heal(f,0,1,2).observation());
            healingFrames("denied",ModernBattlePlayerHealingTest.heal(f,0,2,1).observation());f.elapsed(2_000_000_000L);
            healingFrames("cross",ModernBattlePlayerHealingTest.heal(f,0,3,1).observation());
            healingFrames("hurt",f.cast(1,1,4,1).observation());f.elapsed(4_000_000_000L);
            healingFrames("self",ModernBattlePlayerHealingTest.heal(f,0,4,0).observation());
            f.rooms.releasePreparation(f.ticket);healingFrames("abort",f.engine.serviceObservation());
        }
        for(int kind=1;kind<=2;kind++)try(var f=ModernBattleHealingWireTest.statusFixture(kind)) {
            var c=ModernBattlePlayerHealingTest.active(f);healingFrames("status"+kind+"-initial",c);
            var r=f.engine.haste(f.players.get(0).owner(),ModernBattleHasteTest.request(c,0,1,ModernBattleHasteTest.target(c,1)));
            assertEquals(ModernHasteCast.Reason.NONE,r.reason());healingFrames("status"+kind+"-haste",r.observation());
            f.elapsed(2_000_000_000L);healingFrames("status"+kind+"-heal",ModernBattlePlayerHealingTest.heal(f,0,2,1).observation());
            if(kind==2) {
                f.elapsed(4_000_000_000L);c=f.engine.serviceObservation();
                var s=f.engine.slow(f.players.get(0).owner(),ModernBattleSlowTest.request(c,0,3,ModernBattleSlowTest.target(c,1)));
                assertEquals(ModernSlowCast.Reason.NONE,s.reason());healingFrames("status2-slow",s.observation());
            }
        }
        if(scene) {
            var root=Files.createTempDirectory("modern-healing-scene-wire-").toRealPath();
            try { ModernHealingSceneTraceTest.trace(root,this::healingFrames); }
            finally {
                try(var paths=Files.walk(root)){for(var path:paths.sorted(Comparator.reverseOrder()).toList()) {
                    if(!path.toAbsolutePath().normalize().startsWith(root))throw new IllegalStateException("fixture cleanup scope");Files.delete(path);
                }}
            }
        }
        System.out.println("CLOSED:"+count);System.out.flush();
    }
    private void haste(String label,ModernBattleObservation.Capture c,ModernRooms.Scope scope)throws Exception {
        send(label,ModernGreaterHasteWireTest.publication(c).audience(scope));
    }
    private void hasteCommand(String label,ModernBattleEngineTimedTest.Fixture f,int index,long sequence,ModernGreaterHasteCast.Reason expected)throws Exception {
        hasteCommand(label,f,index,sequence,expected,()->{});
    }
    private void hasteCommand(String label,ModernBattleEngineTimedTest.Fixture f,int index,long sequence,ModernGreaterHasteCast.Reason expected,Runnable between)throws Exception {
        var before=f.engine.serviceObservation();var q=ModernGreaterHasteProtocolTest.intent(before,index,sequence);
        var previous=ModernBattleWire.encode(ModernGreaterHasteWireTest.publication(before).audience(q.proof().player()),3);
        var input=ModernGreaterHasteProtocol.encodeRequest(q,42);
        between.run();
        var result=f.engine.greaterHaste(f.players.get(index).owner(),ModernGreaterHasteProtocol.decodeRequest(input,42).command());
        assertEquals(expected,result.reason());var reply=ModernGreaterHasteProtocol.encodeReply(ModernGreaterHasteProtocolTest.reply(q,result),42);
        System.out.println("HASTE:"+label+":"+HexFormat.of().formatHex(previous)+":"+HexFormat.of().formatHex(input)+":"+HexFormat.of().formatHex(reply));System.out.flush();digestAck(reply);
    }
    private void targetCommand(String label,ModernBattleEngine engine,TournamentPresence.Owner owner,
            ModernBattleObservation.Capture before,int index,long sequence,ModernHasteCast.Target target,
            java.util.function.Function<ModernBattleObservation.Capture,ModernBattlePublication> publish,ModernHasteCast.Reason expected)throws Exception {
        var q=ModernHasteProtocolTest.intent(before,index,sequence,target);
        var previous=ModernBattleWire.encodeVersioned(publish.apply(before).audience(q.proof().player()),44);
        var input=ModernHasteProtocol.encodeRequest(q,44);
        var result=engine.canonicalHaste(owner,ModernHasteProtocol.decodeRequest(input,44));
        assertEquals(expected,result.reason());
        var reply=ModernHasteProtocol.encodeReply(ModernHasteProtocolTest.reply(q,result,publish.apply(result.observation())),44);
        System.out.println("TARGET43:"+label+":"+HexFormat.of().formatHex(previous)+":"+HexFormat.of().formatHex(input)+":"+HexFormat.of().formatHex(reply));
        System.out.flush();digestAck(reply);
    }
    private void targetHasteCommandRun()throws Exception {
        try(var f=ModernBattleHasteTest.fixture()) {
            var c=f.engine.serviceObservation();
            targetCommand("loading",f.engine,f.players.get(0).owner(),c,0,1,ModernBattleHasteTest.target(c,1),ModernHasteEffectsTest::publish,ModernHasteCast.Reason.NOT_ACTIVE);
            c=ModernBattleHasteTest.active(f);
            targetCommand("pc-other",f.engine,f.players.get(0).owner(),c,0,2,ModernBattleHasteTest.target(c,1),ModernHasteEffectsTest::publish,ModernHasteCast.Reason.NONE);
            c=f.engine.serviceObservation();
            targetCommand("pc-cooldown",f.engine,f.players.get(0).owner(),c,0,3,ModernBattleHasteTest.target(c,1),ModernHasteEffectsTest::publish,ModernHasteCast.Reason.TOO_FAST);
            c=f.engine.serviceObservation();
            targetCommand("pc-self",f.engine,f.players.get(1).owner(),c,1,1,ModernBattleHasteTest.target(c,1),ModernHasteEffectsTest::publish,ModernHasteCast.Reason.NONE);
            f.rooms.releasePreparation(f.ticket);c=f.engine.serviceObservation();
            targetCommand("retired",f.engine,f.players.get(0).owner(),c,0,4,ModernBattleHasteTest.target(c,0),ModernHasteEffectsTest::publish,ModernHasteCast.Reason.NOT_ACTIVE);
        }
        try(var f=ModernBattleHasteTest.fixture(40,ModernHasteCatalog.Status.QUALIFIED,ModernHasteCatalog.Status.QUALIFIED)) {
            var c=ModernBattleHasteTest.active(f);
            targetCommand("last-mp",f.engine,f.players.get(0).owner(),c,0,1,ModernBattleHasteTest.target(c,1),ModernHasteEffectsTest::publish,ModernHasteCast.Reason.NONE);
            f.elapsed(2_000_000_000L);c=f.engine.serviceObservation();
            targetCommand("no-mp",f.engine,f.players.get(0).owner(),c,0,2,ModernBattleHasteTest.target(c,1),ModernHasteEffectsTest::publish,ModernHasteCast.Reason.NO_MP);
        }
        try(var f=ModernBattleHasteTest.fixture(300,ModernHasteCatalog.Status.UNLEARNED,ModernHasteCatalog.Status.QUALIFIED)) {
            var c=ModernBattleHasteTest.active(f);
            targetCommand("unlearned",f.engine,f.players.get(0).owner(),c,0,1,ModernBattleHasteTest.target(c,1),ModernHasteEffectsTest::publish,ModernHasteCast.Reason.UNLEARNED);
        }
        try(var f=ModernBattleNpcHasteTest.fixture(false,2,0,300)) {
            ModernBattleNpcHasteTest.active(f);var n=ModernBattleNpcHasteTest.spawn(f,0,11,10);var target=ModernBattleNpcHasteTest.target(n);
            java.util.function.Function<ModernBattleObservation.Capture,ModernBattlePublication> publish=c->ModernBattleWireTest.publication(c,f.config.catalog());
            targetCommand("npc",f.engine,f.players.get(0).owner(),f.engine.serviceObservation(),0,1,target,publish,ModernHasteCast.Reason.NONE);
            targetCommand("npc-stale",f.engine,f.players.get(0).owner(),f.engine.serviceObservation(),0,2,target,publish,ModernHasteCast.Reason.STALE_TARGET);
            var c=f.engine.serviceObservation();target=ModernBattleNpcHasteTest.target(ModernBattleNpcHasteTest.npc(c));
            targetCommand("npc-cooldown",f.engine,f.players.get(0).owner(),c,0,3,target,publish,ModernHasteCast.Reason.TOO_FAST);
            f.elapsed(2000);c=f.engine.serviceObservation();target=ModernBattleNpcHasteTest.target(ModernBattleNpcHasteTest.npc(c));
            targetCommand("npc-refresh",f.engine,f.players.get(0).owner(),c,0,4,target,publish,ModernHasteCast.Reason.NONE);
        }
        try(var f=ModernBattleNpcHasteTest.fixture(false,1,1,300)) {
            var c=ModernBattleNpcHasteTest.active(f);
            targetCommand("bot",f.engine,f.players.get(0).owner(),c,0,1,ModernBattleHasteTest.target(c,1),
                    x->ModernBattleWireTest.publication(x,f.config.catalog()),ModernHasteCast.Reason.NONE);
        }
        System.out.println("CLOSED:"+count);System.out.flush();
    }
    private void greaterHasteCommandRun()throws Exception {
        try(var f=ModernBattleGreaterHasteTest.fixture()) {
            hasteCommand("loading",f,0,1,ModernGreaterHasteCast.Reason.NOT_ACTIVE);ModernBattleGreaterHasteTest.active(f);
            hasteCommand("success",f,0,2,ModernGreaterHasteCast.Reason.NONE);
            hasteCommand("cooldown",f,0,3,ModernGreaterHasteCast.Reason.TOO_FAST);
            hasteCommand("other-source",f,1,1,ModernGreaterHasteCast.Reason.NONE);
            f.elapsed(2_000_000_000L);hasteCommand("refresh",f,0,4,ModernGreaterHasteCast.Reason.NONE);
            f.rooms.releasePreparation(f.ticket);hasteCommand("retired",f,0,5,ModernGreaterHasteCast.Reason.NOT_ACTIVE);
        }
        try(var f=ModernBattleGreaterHasteTest.fixture(60,1000,1000,false,ModernGreaterHasteCatalog.Status.QUALIFIED,ModernGreaterHasteCatalog.Status.QUALIFIED)) {
            ModernBattleGreaterHasteTest.active(f);hasteCommand("last-mp",f,0,1,ModernGreaterHasteCast.Reason.NONE);
            f.elapsed(2_000_000_000L);hasteCommand("no-mp",f,0,2,ModernGreaterHasteCast.Reason.NO_MP);
        }
        try(var f=ModernBattleGreaterHasteTest.fixture()) {
            ModernBattleGreaterHasteTest.active(f);
            // Each frame is real/valid but another own cast is NOT the result
            // of this pending self-cast. The client must reject the transition.
            hasteCommand("intervening-spend",f,0,2,ModernGreaterHasteCast.Reason.TOO_FAST,
                    ()->assertEquals(ModernSpellProtocol.Reason.NONE,f.cast(0,1,4).reason()));
        }
        System.out.println("CLOSED:"+count);System.out.flush();
    }
    private void greaterHasteRun()throws Exception {
        try(var f=ModernBattleGreaterHasteTest.fixture()) {
            var a=f.players.get(0).scope();var b=f.players.get(1).scope();
            haste("initial",ModernBattleGreaterHasteTest.active(f),a);
            var cast=ModernBattleGreaterHasteTest.cast(f,0,1);assertEquals(ModernGreaterHasteCast.Reason.NONE,cast.reason());
            haste("cast-a",cast.observation(),a);haste("cast-b",cast.observation(),b);
            var denied=ModernBattleGreaterHasteTest.cast(f,0,2);assertEquals(ModernGreaterHasteCast.Reason.TOO_FAST,denied.reason());
            haste("denied",denied.observation(),a);f.elapsed(1_000_000_000L);
            assertEquals(ModernItemProtocol.Reason.NONE,f.use(0,40013).reason());haste("potion",f.engine.serviceObservation(),a);
            f.elapsed(2_000_000_000L);cast=ModernBattleGreaterHasteTest.cast(f,0,3);assertEquals(ModernGreaterHasteCast.Reason.NONE,cast.reason());
            haste("refresh",cast.observation(),a);f.rooms.releasePreparation(f.ticket);haste("aborted",f.engine.serviceObservation(),a);
        }
        var root=Files.createTempDirectory("modern-greater-haste-wire-").toRealPath();
        try {
            try(var f=new ModernBattleCanonicalEquipmentTest.Fixture(root,new ModernEquipmentOverrideStore.FileOps(){},
                    p->ModernBattleGreaterHasteTest.enable(p,ModernGreaterHasteCatalog.Status.QUALIFIED),ModernGreaterHasteEquipmentTest::enoughMp)) {
                var gear=f.read();ModernGreaterHasteEquipmentTest.active(f);var scope=f.player().scope();haste("gear-initial",f.capture(),scope);
                var cast=f.world.engine.greaterHaste(f.player().owner(),ModernGreaterHasteEquipmentTest.request(f,1));assertEquals(ModernGreaterHasteCast.Reason.NONE,cast.reason());
                haste("gear-cast",cast.observation(),scope);
                var changed=f.set(gear,false);assertEquals(ModernBattleEngine.EquipmentDisposition.APPLIED,changed.publication().disposition());
                haste("gear-set",changed.observation(),scope);f.world.elapsed(2_000_000_000L);
                cast=f.world.engine.greaterHaste(f.player().owner(),ModernGreaterHasteEquipmentTest.request(f,2));assertEquals(ModernGreaterHasteCast.Reason.NONE,cast.reason());
                haste("gear-recast",cast.observation(),scope);f.killAndRevive();haste("gear-revived",f.capture(),scope);
                assertEquals(1,f.stored().orElseThrow().revision());
            }
        } finally {
            // Newly-created, owned fixture only; never input/shared resources.
            try(var paths=Files.walk(root)){for(var path:paths.sorted(Comparator.reverseOrder()).toList()) {
                if(!path.toAbsolutePath().normalize().startsWith(root))throw new IllegalStateException("fixture cleanup scope");Files.delete(path);
            }}
        }
        System.out.println("CLOSED:"+count);System.out.flush();
    }
    private void durableEquipmentRun()throws Exception {
        var root=Files.createTempDirectory("modern-canonical-equipment-wire-").toRealPath();
        try {
            try(var f=new ModernBattleCanonicalEquipmentTest.Fixture(root,new ModernEquipmentOverrideStore.FileOps(){})) {
                var gear=f.read();f.world.active();var a=f.player().scope();var b=f.world.players.get(1).scope();
                pair("gear-0",f.capture(),a,b);
                assertEquals(ModernSpellProtocol.Reason.NONE,f.world.cast(0,1,1,3).reason());
                assertEquals(ModernItemProtocol.Reason.NONE,f.use(1).reason());pair("gear-1",f.capture(),a,b);
                var down=f.set(gear,false);assertEquals(ModernBattleEngine.EquipmentDisposition.APPLIED,down.publication().disposition());
                pair("gear-2",down.observation(),a,b);
                f.world.elapsed(2_000_000_000L);assertEquals(ModernSpellProtocol.Reason.NONE,f.world.cast(0,2,1,3).reason());
                assertEquals(ModernItemProtocol.Reason.NONE,f.use(2).reason());pair("gear-3",f.capture(),a,b);
                var up=f.set(down.equipment().snapshot(),true);assertEquals(ModernBattleEngine.EquipmentDisposition.APPLIED,up.publication().disposition());
                pair("gear-4",up.observation(),a,b);
                f.world.elapsed(4_000_000_000L);assertEquals(ModernSpellProtocol.Reason.NONE,f.world.cast(0,3,1,3).reason());
                assertEquals(ModernItemProtocol.Reason.NONE,f.use(3).reason());pair("gear-5",f.capture(),a,b);
                assertEquals(2,f.stored().orElseThrow().revision());
            }
        } finally {
            try(var paths=Files.walk(root)){for(var path:paths.sorted(Comparator.reverseOrder()).toList()) {
                if(!path.toAbsolutePath().normalize().startsWith(root))throw new IllegalStateException("fixture cleanup scope");Files.delete(path);
            }}
        }
        System.out.println("CLOSED:"+count);System.out.flush();
    }
    private void serviceEquipmentRun()throws Exception {
        var root=Files.createTempDirectory("modern-service-equipment-wire-").toRealPath();
        try {
            try(var f=new ModernBattleLifecycleServiceTest.Fixture(root,null)) {
                for(int stage=0;stage<4;stage++) {
                    if(stage==1)f.reprice();
                    if(stage==2)f.win();
                    if(stage==3)ModernBattleLifecycleServiceTest.ModernWorldServiceItemTestAwait.until(
                            ()->f.lifecycle().settlement()==ModernWorldProtocol.SettlementState.SAVED);
                    send("service-gear-"+stage+"-a",f.read(0).view());
                    send("service-gear-"+stage+"-b",f.read(1).view());
                }
                assertEquals(1,f.persisted.get());assertEquals(f.event.get(),f.lifecycle().result());
                f.leave(0);f.leave(1);assertEquals(0,f.service.matchCount());
            }
        } finally {
            try(var paths=Files.walk(root)){for(var path:paths.sorted(Comparator.reverseOrder()).toList()) {
                if(!path.toAbsolutePath().normalize().startsWith(root))throw new IllegalStateException("fixture cleanup scope");Files.delete(path);
            }}
        }
        System.out.println("CLOSED:"+count);System.out.flush();
    }
    private void presentationRun()throws Exception {
        try(var f=new ModernBattleEngineAmmoTest.Fixture()) {
            f.active();var a=f.prepared.get(0);var b=f.prepared.get(1);pair("bow",f.engine.serviceObservation(),a.scope(),b.scope());
            f.clock.addAndGet(123_000_000L);assertEquals(ModernBattleEngine.AttackStatus.ATTACKED,f.attack(1).status());pair("bow",f.engine.serviceObservation(),a.scope(),b.scope());
            assertEquals(ModernBattleEngine.AttackStatus.TOO_FAST,f.attack(2).status());pair("bow",f.engine.serviceObservation(),a.scope(),b.scope());
            f.clock.addAndGet(1_000_000_000L);
            var at=f.engine.serviceObservation().actors().get(1).pose().position();
            assertEquals(ModernBattleEngine.MoveStatus.MOVED,f.engine.move(b.owner(),b.scope(),f.ticket.matchId(),f.ticket.generation(),1,at.x(),at.y(),2).status());
            pair("bow",f.engine.serviceObservation(),a.scope(),b.scope());
            assertEquals(ModernBattleEngine.AttackStatus.ATTACKED,f.attack(3).status());pair("bow",f.engine.serviceObservation(),a.scope(),b.scope());
        }
        try(var f=new ModernBattleEngineTimedTest.Fixture()) {
            f.active();var a=f.players.get(0).scope();var b=f.players.get(1).scope();pair("item",f.engine.serviceObservation(),a,b);
            assertEquals(ModernItemProtocol.Reason.NONE,f.use(0,40010).reason());pair("item",f.engine.serviceObservation(),a,b);
            f.elapsed(250_000_000L);assertEquals(ModernItemProtocol.Reason.NONE,f.use(0,40013).reason());pair("item",f.engine.serviceObservation(),a,b);
            assertEquals(ModernItemProtocol.Reason.CLASS_RESTRICTED,f.use(0,40068).reason());pair("item",f.engine.serviceObservation(),a,b);
            f.elapsed(301_000_000_000L);f.engine.tick();pair("item",f.engine.serviceObservation(),a,b);
        }
        System.out.println("CLOSED:"+count);System.out.flush();
    }
    private void itemEffectsRun()throws Exception {
        try(var f=new ModernBattleEngineTimedTest.Fixture()) {
            f.active();var a=f.players.get(0).scope();var b=f.players.get(1).scope();pair("effects",f.engine.serviceObservation(),a,b);
            int[] ids={40010,40011,40012,40013,40014,40016,40068};long[] moves={0,0};
            for(int i=0;i<ids.length;i++) {
                int owner=i==6?1:0;long elapsed=(i+1)*2_000_000_000L;f.elapsed(elapsed);
                assertEquals(ModernItemProtocol.Reason.NONE,f.use(owner,ids[i]).reason());pair("effects",f.engine.serviceObservation(),a,b);
                f.elapsed(elapsed+100_000_000L);assertEquals(ModernBattleEngine.MoveStatus.MOVED,f.move(owner,++moves[owner],0).status());
                pair("effects",f.engine.serviceObservation(),a,b);
            }
        }
        try(var f=new ModernBattleEngineTimedTest.Fixture()) {
            f.active();var a=f.players.get(0).scope();var b=f.players.get(1).scope();pair("mixed",f.engine.serviceObservation(),a,b);
            assertEquals(ModernSpellProtocol.Reason.NONE,f.cast(0,1,4).reason());pair("mixed",f.engine.serviceObservation(),a,b);
            f.elapsed(100_000_000L);assertEquals(ModernItemProtocol.Reason.NONE,f.use(0,40016).reason());pair("mixed",f.engine.serviceObservation(),a,b);
            f.elapsed(200_000_000L);assertEquals(ModernItemProtocol.Reason.NONE,f.use(1,40014).reason());pair("mixed",f.engine.serviceObservation(),a,b);
        }
        System.out.println("CLOSED:"+count);System.out.flush();
    }
    private static ModernBattleEngine.PreparedMember pacedBow(ModernBattleEngine.PreparedMember original) {
        var p=ModernBattlePresentationTest.bow(original);var stocks=new java.util.ArrayList<>(p.matchItems());
        ModernShopTimedTest.catalog(1).offers().stream().filter(o->o.itemId()==40013||o.itemId()==40014)
                .forEach(o->stocks.add(new ModernShopAuthority.Stack(o,3)));
        stocks.sort(java.util.Comparator.comparingLong(s->s.offer().itemId()));
        return new ModernBattleEngine.PreparedMember(p.owner(),p.scope(),p.character(),p.spawn(),p.stepIntervalNanos(),p.fighter(),p.attackIntervalNanos(),
                p.skills(),p.castIntervalNanos(),p.damageMagic(),stocks,p.weightBasis(),true,true,true,p.fireStormEnabled());
    }
    private void healingEffectsRun()throws Exception {
        try(var f=new ModernBattleEngineHonorTest.Fixture(1000,100,500,1_000_000_000L)) {
            f.active();var a=f.players.get(0).scope();var b=f.players.get(1).scope();pair("heal-full",f.engine.serviceObservation(),a,b);
            var heal=f.cast(0,1,1,3);assertEquals(ModernSpellProtocol.Reason.NONE,heal.reason());
            assertEquals(0,heal.snapshot().honor().heals().get(0).restored());pair("heal-full",f.engine.serviceObservation(),a,b);
            assertEquals(ModernSpellProtocol.Reason.TOO_FAST,f.cast(0,2,1,3).reason());pair("heal-full",f.engine.serviceObservation(),a,b);
            f.elapsed(1_200_000_000L);assertEquals(ModernBattleEngine.MoveStatus.MOVED,f.move(0,1,4).status());pair("heal-full",f.engine.serviceObservation(),a,b);
            f.elapsed(2_000_000_000L);assertEquals(ModernSpellProtocol.Reason.NONE,f.cast(0,3,1,3).reason());pair("heal-full",f.engine.serviceObservation(),a,b);
            f.elapsed(3_300_000_000L);assertEquals(ModernSpellProtocol.Reason.NONE,f.cast(0,4,1,3).reason());pair("heal-full",f.engine.serviceObservation(),a,b);
            f.elapsed(3_900_000_000L);pair("heal-full",f.engine.serviceObservation(),a,b);
        }
        try(var f=new ModernBattleEngineHonorTest.Fixture(1000,100,500,1_000_000_000L)) {
            f.active();var a=f.players.get(0).scope();var b=f.players.get(1).scope();
            assertEquals(ModernBattleEngine.AttackStatus.ATTACKED,f.attack(1,1,3).status());pair("heal-hurt",f.engine.serviceObservation(),a,b);
            var healed=f.cast(0,1,1,3);assertEquals(ModernSpellProtocol.Reason.NONE,healed.reason());
            assertTrue(healed.snapshot().honor().heals().get(0).restored()>0);pair("heal-hurt",f.engine.serviceObservation(),a,b);
            f.elapsed(700_000_000L);var hit=f.attack(1,2,1);assertEquals(ModernBattleEngine.AttackStatus.ATTACKED,hit.status());
            assertTrue(hit.snapshot().vitals().get(0).hp()<100);pair("heal-hurt",f.engine.serviceObservation(),a,b);
        }
        for(boolean mixed:new boolean[]{false,true})try(var f=new ModernBattleEngineHonorTest.Fixture(1000,100,500,1_000_000_000L,0,true,true)) {
            f.active();var a=f.players.get(0);var b=f.players.get(1);String name=mixed?"heal-mixed":"heal-paced";
            pair(name,f.engine.serviceObservation(),a.scope(),b.scope());
            assertEquals(ModernSpellProtocol.Reason.NONE,f.cast(0,1,1,3).reason());pair(name,f.engine.serviceObservation(),a.scope(),b.scope());
            if(mixed) {
                assertEquals(ModernSpellProtocol.Reason.NONE,f.cast(1,1,4,1).reason());pair(name,f.engine.serviceObservation(),a.scope(),b.scope());
                f.elapsed(100_000_000L);assertEquals(ModernItemProtocol.Reason.NONE,f.engine.item(b.owner(),new ModernItemProtocol.Request(b.scope(),ModernItemProtocol.Action.USE,f.ticket.matchId(),f.ticket.generation(),1,40014)).reason());
                pair(name,f.engine.serviceObservation(),a.scope(),b.scope());
            } else for(int i=0;i<2;i++) {
                f.elapsed(100_000_000L+i*100_000_000L);
                assertEquals(ModernItemProtocol.Reason.NONE,f.engine.item(a.owner(),new ModernItemProtocol.Request(a.scope(),ModernItemProtocol.Action.USE,f.ticket.matchId(),f.ticket.generation(),i+1,i==0?40013:40014)).reason());
                pair(name,f.engine.serviceObservation(),a.scope(),b.scope());
            }
        }
        System.out.println("CLOSED:"+count);System.out.flush();
    }
    private void npcMotionRun()throws Exception {
        ModernNpcMotionWireTest.trace((label,p)->{
            var bytes=ModernNpcMotionWireTest.verified(p);
            System.out.println("MOTION:"+label+":"+HexFormat.of().formatHex(bytes));System.out.flush();digestAck(bytes);
        });
        System.out.println("CLOSED:"+count);System.out.flush();
    }
    private void targetHasteRun()throws Exception {
        ModernHasteWireTest.trace((label,p)->{
            var bytes=ModernHasteWireTest.verified(p);
            System.out.println("HASTE43:"+label+":"+HexFormat.of().formatHex(bytes));System.out.flush();digestAck(bytes);
        });
        System.out.println("CLOSED:"+count);System.out.flush();
    }
    private void targetHasteTimeRun()throws Exception {
        ModernHasteTimeAttackWireTest.trace((label,p)->{
            var bytes=ModernHasteWireTest.verified(p,45);
            System.out.println("HASTE43:"+label+":"+HexFormat.of().formatHex(bytes));System.out.flush();digestAck(bytes);
        });
        System.out.println("CLOSED:"+count);System.out.flush();
    }
    private void npcMotionPresentationRun()throws Exception {
        ModernNpcMotionWireTest.presentationTrace((label,p)->{
            var bytes=ModernNpcMotionWireTest.verified(p);
            System.out.println("MOTION:"+label+":"+HexFormat.of().formatHex(bytes));System.out.flush();digestAck(bytes);
        });
        System.out.println("CLOSED:"+count);System.out.flush();
    }
    private void bowEffectsRun()throws Exception {
        for(boolean miss:new boolean[]{false,true})try(var f=new ModernBattleEngineAmmoTest.Fixture()) {
            f.active();var a=f.prepared.get(0);var b=f.prepared.get(1);long move=0;
            for(int n=0;n<4;n++) {
                f.clock.addAndGet(200_000_000L);var at=f.engine.tick().actors().get(1).position();
                assertEquals(ModernBattleEngine.MoveStatus.MOVED,f.engine.move(b.owner(),b.scope(),f.ticket.matchId(),f.ticket.generation(),++move,at.x(),at.y(),2).status());
            }
            String label=miss?"bow-miss":"bow-hit";f.miss=miss;pair(label,f.engine.serviceObservation(),a.scope(),b.scope());
            assertEquals(ModernBattleEngine.AttackStatus.ATTACKED,f.attack(1).status());pair(label,f.engine.serviceObservation(),a.scope(),b.scope());
            for(long nanos:new long[]{200_000_000L,200_000_000L}) {
                f.clock.addAndGet(nanos);var at=f.engine.tick().actors().get(1).position();
                assertEquals(ModernBattleEngine.MoveStatus.MOVED,f.engine.move(b.owner(),b.scope(),f.ticket.matchId(),f.ticket.generation(),++move,at.x(),at.y(),2).status());
                pair(label,f.engine.serviceObservation(),a.scope(),b.scope());
            }
        }
        try(var f=new ModernBattleEngineHonorTest.Fixture(10000,10000,500,1_000_000_000L,0,false,true,ModernBattlePresentationTest::bow)) {
            f.active();var a=f.players.get(0).scope();var b=f.players.get(1).scope();pair("bow-hurt",f.engine.serviceObservation(),a,b);
            assertEquals(ModernBattleEngine.AttackStatus.ATTACKED,f.attack(0,1,2).status());pair("bow-hurt",f.engine.serviceObservation(),a,b);
            f.elapsed(100_000_000L);assertEquals(ModernBattleEngine.AttackStatus.ATTACKED,f.attack(1,1,1).status());pair("bow-hurt",f.engine.serviceObservation(),a,b);
        }
        try(var f=new ModernBattleEngineHonorTest.Fixture(10000,1,500,1_000_000_000L,0,false,true,ModernBattlePresentationTest::bow)) {
            f.active();var a=f.players.get(0).scope();var b=f.players.get(1).scope();
            for(int n=1;n<=4;n++){f.elapsed(n*200_000_000L);assertEquals(ModernBattleEngine.MoveStatus.MOVED,f.move(1,n,2).status());}
            pair("bow-life",f.engine.serviceObservation(),a,b);f.elapsed(900_000_000L);
            assertEquals(ModernBattleEngine.AttackStatus.ATTACKED,f.attack(0,1,2).status());pair("bow-life",f.engine.serviceObservation(),a,b);
            f.elapsed(5_950_000_000L);f.engine.tick();pair("bow-life",f.engine.serviceObservation(),a,b);
            f.elapsed(6_200_000_000L);assertEquals(ModernBattleEngine.MoveStatus.MOVED,f.move(1,5,2).status());pair("bow-life",f.engine.serviceObservation(),a,b);
        }
        try(var f=new ModernBattleEngineHonorTest.Fixture(10000,10000,500,1_000_000_000L,0,false,true,ModernBattlePresentationTest::bow)) {
            f.active();var a=f.players.get(0).scope();var b=f.players.get(1).scope();pair("bow-honor",f.engine.serviceObservation(),a,b);
            assertEquals(ModernBattleEngine.AttackStatus.ATTACKED,f.attack(0,1,4).status());pair("bow-honor",f.engine.serviceObservation(),a,b);
            pair("bow-honor",f.engine.serviceObservation(),a,b);
        }
        try(var f=new ModernBattleEngineHonorTest.Fixture(10000,10000,500,1_000_000_000L,0,false,true,ModernBattleWirePeer::pacedBow)) {
            f.active();var a=f.players.get(0);var b=f.players.get(1);pair("bow-paced",f.engine.serviceObservation(),a.scope(),b.scope());
            assertEquals(ModernBattleEngine.AttackStatus.ATTACKED,f.attack(0,1,4).status());pair("bow-paced",f.engine.serviceObservation(),a.scope(),b.scope());
            for(int i=0;i<2;i++) {
                f.elapsed(100_000_000L+i*20_000_000L);
                assertEquals(ModernItemProtocol.Reason.NONE,f.engine.item(a.owner(),new ModernItemProtocol.Request(a.scope(),ModernItemProtocol.Action.USE,f.ticket.matchId(),f.ticket.generation(),i+1,i==0?40013:40014)).reason());
                pair("bow-paced",f.engine.serviceObservation(),a.scope(),b.scope());
            }
        }
        System.out.println("CLOSED:"+count);System.out.flush();
    }
}
