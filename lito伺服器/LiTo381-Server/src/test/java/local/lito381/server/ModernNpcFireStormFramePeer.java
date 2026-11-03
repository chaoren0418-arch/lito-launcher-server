package local.lito381.server;

import static org.junit.jupiter.api.Assertions.*;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;

/** Test-classpath only: real engine journals, complete candidate NPC frames.
 * No application handshake, SQL, rendering or player admission is implied. */
final class ModernNpcFireStormFramePeer {
    record Row(String name,ModernRooms.Scope player,ModernNpcProtocol.Frame frame) {}
    static void add(List<Row> rows,String name,ModernBattleEngineNpcTest.Fixture f,int who)throws Exception {
        rows.add(new Row(name,f.players.get(who).scope(),ModernNpcFireStormFrameTest.roundtrip(ModernNpcFireStormFrameTest.frame(f,who))));
    }
    static void add(List<Row> rows,String name,ModernTimeAttackEngineTest.Fixture f)throws Exception {
        rows.add(new Row(name,f.players.get(1).scope(),ModernNpcFireStormFrameTest.roundtrip(ModernNpcFireStormFrameTest.frame(f))));
    }
    static List<Row> normal()throws Exception {
        var rows=new ArrayList<Row>();
        try(var f=ModernBattleEngineNpcFireStormTest.fixture(1000,9007199254740993L,0,true,true,false,1)) {
            add(rows,"loading",f,0);f.active();var target=f.spawn(0,11,10);f.spawn(0,10,11);f.spawn(1,21,10);add(rows,"before",f,0);
            assertEquals(ModernNpcFireStormState.Reason.NONE,ModernBattleEngineNpcFireStormTest.cast(f,1).reason());add(rows,"hit",f,0);
            assertEquals(ModernNpcFireStormState.Reason.TOO_FAST,ModernBattleEngineNpcFireStormTest.cast(f,2).reason());add(rows,"denied",f,0);
            f.elapsed(1200);assertEquals(ModernNpcMagic.Reason.NONE,ModernBattleEngineNpcMagicTest.cast(f,0,3,target).reason());add(rows,"arrow",f,0);
            f.elapsed(2400);assertEquals(ModernNpcDisintegrateState.Reason.NONE,ModernBattleEngineNpcDisintegrateTest.cast(f,0,4,target).reason());add(rows,"disintegrate",f,0);
            f.elapsed(4400);assertEquals(ModernNpcFireStormState.Reason.NONE,ModernBattleEngineNpcFireStormTest.cast(f,5).reason());add(rows,"second",f,0);
            f.elapsed(16000);add(rows,"regen",f,0);f.rooms.releasePreparation(f.ticket);add(rows,"aborted",f,0);
        }
        return List.copyOf(rows);
    }
    static List<Row> quest(int map)throws Exception {
        var rows=new ArrayList<Row>();
        try(var f=ModernNpcFireStormStructuresTest.quest(map,1)) {
            add(rows,"loading-own",f,0);add(rows,"loading-other",f,1);f.active();
            add(rows,"active-own",f,0);add(rows,"active-other",f,1);long seq=0;
            while(f.view().world().phase()==ModernBattleEngine.Phase.ACTIVE) {
                f.elapsed(seq*2000);assertEquals(ModernNpcFireStormState.Reason.NONE,ModernNpcFireStormStructuresTest.cast(f,0,++seq).reason());
                add(rows,"cast-"+seq+"-own",f,0);add(rows,"cast-"+seq+"-other",f,1);assertTrue(seq<=8);
            }
        }
        return List.copyOf(rows);
    }
    static List<Row> lives()throws Exception {
        var rows=new ArrayList<Row>();
        try(var f=ModernNpcFireStormStructuresTest.quest(513,1,1,false)) {
            f.active();ModernNpcFireStormStructuresTest.cast(f,0,1);f.elapsed(2000);ModernNpcFireStormStructuresTest.cast(f,0,2);add(rows,"prior",f,0);
            f.randomValue=19;var dead=ModernQuestLifeTest.die(f,2100);add(rows,"dead",f,0);
            ModernQuestLifeTest.confirm(f,0,ModernQuestLifeTest.life(dead,0));add(rows,"revived",f,0);
            f.elapsed(4000);f.randomValue=0;assertEquals(ModernNpcFireStormState.Reason.NONE,ModernNpcFireStormStructuresTest.cast(f,0,3).reason());add(rows,"next-life",f,0);
        }
        return List.copyOf(rows);
    }
    static List<Row> time()throws Exception {
        var rows=new ArrayList<Row>();
        try(var f=ModernNpcFireStormStructuresTest.time(1,2)) {
            add(rows,"loading",f);f.active();add(rows,"active",f);
            assertEquals(ModernNpcFireStormState.Reason.NONE,ModernNpcFireStormStructuresTest.cast(f,0,1).reason());add(rows,"tower",f);
            f.engine.withdrawTimeAttack(f.players.get(0).owner());add(rows,"departed",f);
            for(int i=1;i<=4;i++) {
                f.elapsed(i*2000L);assertEquals(ModernNpcFireStormState.Reason.NONE,ModernNpcFireStormStructuresTest.cast(f,1,i).reason());add(rows,"wave-"+i,f);
            }
            var n=rows.get(rows.size()-1).frame();var c=n.combat();var w=c.world();assertEquals(ModernWorldProtocol.Phase.FINISHED,w.phase());
            var saved=new ModernWorldProtocol.Frame(w.matchId(),w.generation(),w.revision()+1,w.phase(),w.abortReason(),w.room(),w.manifest(),w.actors(),w.completion(),w.casts(),w.descriptor(),w.race(),w.audienceLane(),w.quest(),w.timeAttack().withSettlement(ModernWorldProtocol.SettlementState.SAVED));
            var finished=n.withCombat(new ModernCombatProtocol.Frame(saved,c.vitals(),c.damages(),c.items(),c.weights(),c.buffs(),c.ammo(),c.regen(),c.honor(),c.fireStorms(),c.npcSpells(),c.disintegrations(),c.npcDisintegrations(),c.npcFireStorms()));
            rows.add(new Row("saved",f.players.get(1).scope(),ModernNpcFireStormFrameTest.roundtrip(finished)));
        }
        return List.copyOf(rows);
    }
    static List<Row> rows(String scenario)throws Exception {
        return switch(scenario) {case "normal"->normal();case "lives"->lives();case "time"->time();case "wide"->wide();default->quest(Integer.parseInt(scenario));};
    }
    /** Synthetic legal maximum-count snapshot, not a claimed 100-player live run.
     * Each of 100 casters retains all 48 hits; no truncation to the old envelope. */
    static List<Row> wide()throws Exception {
        var catalog=ModernNpcMeleeTest.catalog(java.util.Map.of(ModernNpcCatalog.Field.HP,10000L),true,0);
        var map=ModernWorldMap.fromSectorBytes(554,java.util.Map.of(ModernBattleEngineBotTest.SECTOR,ModernWorldMapTest.openSectorBytes()),()->false);
        var positions=java.util.stream.IntStream.range(0,100).mapToObj(i->ModernBattleEngineNpcTest.cell(2+i%10*3,30+i/10*3)).toList();
        try(var f=new ModernBattleEngineNpcTest.Fixture(100,0,true,100,0,s->ModernBattleEngineNpcDisintegrateTest.source(s,1000,500),catalog,false,map,positions)) {
            var cfg=new ModernNpcBattle.Config(f.config.key(),catalog,f.config.players(),f.config.laneAnchors(),128,1000,null,true).withFireStorm(true);
            f.engine=f.build(cfg,false,f.prepared.stream().map(p->ModernBattleEngineNpcFireStormTest.enable(p,true,false)).toList());f.active();
            var base=ModernNpcFireStormFrameTest.frame(f,0);var c=base.combat();var w=c.world();
            var casts=w.casts().stream().map(s->new ModernSpellProtocol.CastState(s.entityId(),1,0,0,0,0,0,0,0,0,s.castIntervalMillis())).toList();
            var world=new ModernWorldProtocol.Frame(w.matchId(),w.generation(),1002,w.phase(),w.abortReason(),w.room(),w.manifest(),w.actors(),w.completion(),casts,w.descriptor(),w.race(),w.audienceLane(),w.quest(),w.timeAttack());
            var origins=List.of(ModernBattleEngineNpcTest.cell(10,10),ModernBattleEngineNpcTest.cell(25,10));
            var cells=new ArrayList<ModernWorldMap.Cell>();
            for(var origin:origins)for(int y=-3;y<=3;y++)for(int x=-3;x<=3;x++)if(x!=0||y!=0)cells.add(new ModernWorldMap.Cell(origin.x()+x,origin.y()+y));
            var npcs=new ArrayList<ModernNpcProtocol.Actor>();var template=catalog.npcs().get(ModernNpcMeleeTest.NPC).rosterTemplate();
            for(int i=0;i<96;i++)npcs.add(new ModernNpcProtocol.Actor(i+1,template.catalogSourceId(),template.npcId(),template.graphic(),i/48,1,0,ModernNpcRoster.Action.HURT,cells.get(i),8250,10000,template.maximumMp(),template.maximumMp(),1,1,1000,0,0,0));
            var fires=new ArrayList<ModernNpcFireStormState.State>();int[] laneCasts={0,0};
            for(var a:w.actors()) {
                int lane=a.member().team(),ordinal=laneCasts[lane]++;var targets=new ArrayList<ModernNpcFireStormState.Target>();long hp=10000-ordinal*35;
                for(int i=lane*48;i<(lane+1)*48;i++)targets.add(new ModernNpcFireStormState.Target(new ModernNpcRoster.Id(base.scope(),i+1),1,lane,cells.get(i),hp,new ModernFireStorm.Outcome(35,35,hp-35,false)));
                fires.add(new ModernNpcFireStormState.State(a.entityId(),1,a.entityId()+1,1,48,48,1,base.scope(),origins.get(lane),lane,targets));
            }
            assertArrayEquals(new int[]{50,50},laneCasts);
            var vitals=c.vitals().stream().map(v->new ModernCombatProtocol.Vital(v.entityId(),v.hp(),v.maxHp(),v.mp()-48,v.maxMp(),v.ac(),v.weaponType(),v.attackSequence(),v.attackRevision(),v.hurtRevision(),v.deathRevision(),v.lastTargetEntityId(),v.attackIntervalNanos())).toList();
            var combat=new ModernCombatProtocol.Frame(world,vitals,c.damages(),c.items(),c.weights(),c.buffs(),c.ammo(),c.regen(),c.honor(),c.fireStorms(),c.npcSpells(),c.disintegrations(),c.npcDisintegrations(),fires);
            var result=new ModernNpcProtocol.Frame(combat,base.scope(),1000,1000,96,false,npcs,List.of(),base.resources(),base.names());
            ModernNpcFireStormFrameTest.roundtrip(result);assertTrue(ModernNpcFireStormFrameTest.bytes(result).length>256*1024);
            return List.of(new Row("maximum-count",f.players.get(0).scope(),result));
        }
    }
    public static void main(String[] args)throws Exception {
        if(args.length!=1)throw new IllegalArgumentException("one explicit scenario required");
        for(var row:rows(args[0]))System.out.println("FRAME:"+row.name()+":"+row.player().sourceId()+":"+row.player().objectId()+":"+row.player().epoch()+":"+
                HexFormat.of().formatHex(ModernNpcFireStormFrameTest.bytes(row.frame())));
        System.out.println("CLOSED");
    }
}
