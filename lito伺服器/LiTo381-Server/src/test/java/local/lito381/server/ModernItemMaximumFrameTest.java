package local.lito381.server;

import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayInputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** Maximum-width wire fixture only; no Engine, JDBC, resource load or external process. */
class ModernItemMaximumFrameTest {
    private static final long MATCH = 0x1_0000_0002L;
    private static final long GENERATION = 0x2_0000_0003L;
    private static final long FIRST_OBJECT = 0x3_0000_0004L;
    private static final long ENGINE_REVISION = 400;
    private static final long FINAL_EVENT_REVISION = ENGINE_REVISION + 1;
    private static final long SAVED_WORLD_REVISION = FINAL_EVENT_REVISION + 1;

    @Test void maximumHundredHumanFinishedItemAndCombatFramesFitExisting128KiBLimit() throws Exception {
        var frame = maximumFrame();
        var scope = new ModernRooms.Scope(7, FIRST_OBJECT, -1, 3);
        var itemRequest = new ModernItemProtocol.Request(scope, ModernItemProtocol.Action.READ, MATCH, GENERATION, 0, 0);
        var combatRequest = new ModernCombatProtocol.Request(scope, ModernCombatProtocol.Action.READ, MATCH, GENERATION, 0, 0);
        var item = new ModernApplicationProtocol.ItemResult(0xfedcba98, itemRequest,
                new ModernItemProtocol.Reply(ModernItemProtocol.Status.OK, ModernItemProtocol.Reason.NONE, frame));
        var combat = new ModernApplicationProtocol.CombatResult(0xfedcba99, combatRequest,
                new ModernCombatProtocol.Reply(ModernCombatProtocol.Status.OK, ModernCombatProtocol.Reason.NONE, frame));

        assertEquals(128 * 1024, ModernCombatProtocol.MAX_FRAME_BYTES);
        assertEquals(128 * 1024, ModernItemProtocol.MAX_FRAME_BYTES);
        assertEquals(100, frame.world().actors().size());
        assertEquals(16, frame.world().manifest().sectors().size());
        assertEquals(100, frame.world().casts().size());
        assertEquals(100, frame.vitals().size());
        assertEquals(100, frame.damages().size());
        assertEquals(100, frame.items().size());
        assertEquals(100, frame.world().completion().event().participants().size());
        assertTrue(frame.world().actors().stream().allMatch(a -> a.member().identity().isHuman()));
        assertTrue(frame.items().stream().allMatch(s -> s.stocks().size() == 3));
        assertEquals(64, frame.world().room().summary().config().name().getBytes(StandardCharsets.UTF_8).length);
        for (var actor : frame.world().actors()) {
            assertEquals(256, actor.member().name().getBytes(StandardCharsets.UTF_8).length);
            assertEquals(256, actor.member().sourceName().getBytes(StandardCharsets.UTF_8).length);
        }
        for (var message : List.<ModernApplicationProtocol.Message>of(item, combat)) {
            byte[] encoded = ModernApplicationProtocol.encode(message, 8);
            // Independent field-width sum: header16 + echo56 + result4;
            // world prefix28 + joined(98+12+4+100*548) + manifest(40+16*40)
            // + actors(4+100*36) + completion(4+60+100*28) + casts(4+100*64)
            // + vitals(4+100*104) + damage(4+100*64) + items(4+100*(92+3*24)).
            assertEquals(101_782, encoded.length);
            assertEquals(encoded.length, ByteBuffer.wrap(encoded).getInt(4));
            assertEquals(message instanceof ModernApplicationProtocol.ItemResult ? 35 : 23,
                    Short.toUnsignedInt(ByteBuffer.wrap(encoded).getShort(12)));
            assertTrue(encoded.length > 65_536, "exercise the extended frame path, not a small fixture");
            assertTrue(encoded.length <= 128 * 1024, "do not raise the production frame limit");
            assertEquals(message, ModernApplicationProtocol.decode(encoded, 8));
        }
    }

    @Test void v9HundredHumansAllThreeOriginalUnitsStillFitUnchanged128KiBLimit() throws Exception {
        var old = maximumFrame();
        var weights = old.items().stream().map(item -> new ModernWeightProtocol.State(item.entityId(),
                ModernInventoryWeightTest.weight(4_000_000_000L,8_000_000_000.5,
                        item.stocks().stream().map(stock -> new ModernInventoryWeight.Carried(stock.itemId(),stock.remaining(),
                                stock.itemId()==40010?3900:stock.itemId()==40011?4900:5850)).toList()))).toList();
        var frame = new ModernCombatProtocol.Frame(old.world(),old.vitals(),old.damages(),old.items(),weights);
        var scope = new ModernRooms.Scope(7,FIRST_OBJECT,-1,3);
        var message = new ModernApplicationProtocol.CombatResult(1,
                new ModernCombatProtocol.Request(scope,ModernCombatProtocol.Action.READ,MATCH,GENERATION,0,0),
                new ModernCombatProtocol.Reply(ModernCombatProtocol.Status.OK,ModernCombatProtocol.Reason.NONE,frame));
        byte[] encoded=ModernApplicationProtocol.encode(message,9);
        assertEquals(101_782 + 8 + 100 * (8 + 40 + 3 * 16),encoded.length);
        assertEquals(111_390,encoded.length);assertTrue(encoded.length<128*1024);
        assertEquals(message,ModernApplicationProtocol.decode(encoded,9));
        assertThrows(ModernApplicationProtocol.ProtocolException.class,()->ModernApplicationProtocol.encode(message,8));
    }

    @Test void v10HundredHumansSevenStocksWeightsAndBuffsUseOnlyTheNew256KiBLimit() throws Exception {
        var old = maximumFrame();
        var items = old.items().stream().map(item -> {
            var stocks = new ArrayList<>(item.stocks());
            stocks.add(new ModernItemProtocol.Stock(40013, 1, 1));
            stocks.add(new ModernItemProtocol.Stock(40014, 1, 1));
            stocks.add(new ModernItemProtocol.Stock(40016, 1, 1));
            stocks.add(new ModernItemProtocol.Stock(40068, 1, 1));
            return new ModernItemProtocol.State(item.entityId(), item.requestSequence(), item.useCount(),
                    item.useRevision(), item.itemId(), item.restored(), item.hpBefore(), item.hpAfter(),
                    item.effect(), item.delayMillis(), item.x(), item.y(), item.fixedRecovery(), stocks);
        }).toList();
        var weights = items.stream().map(item -> new ModernWeightProtocol.State(item.entityId(),
                ModernInventoryWeightTest.weight(4_000_000_000L, 8_000_000_000.5,
                        item.stocks().stream().map(stock -> new ModernInventoryWeight.Carried(
                                stock.itemId(), stock.remaining(), switch ((int) stock.itemId()) {
                                    case 40010, 40013 -> 3900;
                                    case 40011 -> 4900;
                                    case 40012 -> 5850;
                                    case 40014, 40016 -> 4500;
                                    case 40068 -> 5000;
                                    default -> throw new AssertionError("unexpected fixture item");
                                })).toList()))).toList();
        var buffs = old.world().actors().stream().map(actor -> ModernTimedEffects.initial(actor.entityId())).toList();
        var frame = new ModernCombatProtocol.Frame(old.world(), old.vitals(), old.damages(), items, weights, buffs);
        var scope = new ModernRooms.Scope(7, FIRST_OBJECT, -1, 3);
        var combat = new ModernApplicationProtocol.CombatResult(0xfedcba9a,
                new ModernCombatProtocol.Request(scope, ModernCombatProtocol.Action.READ, MATCH, GENERATION, 0, 0),
                new ModernCombatProtocol.Reply(ModernCombatProtocol.Status.OK, ModernCombatProtocol.Reason.NONE, frame));
        var item = new ModernApplicationProtocol.ItemResult(0xfedcba9b,
                new ModernItemProtocol.Request(scope, ModernItemProtocol.Action.READ, MATCH, GENERATION, 0, 0),
                new ModernItemProtocol.Reply(ModernItemProtocol.Status.OK, ModernItemProtocol.Reason.NONE, frame));
        // A terminal cast denial carries the same complete confirmed frame; it is not a fake successful cast.
        var cast = new ModernApplicationProtocol.CastResult(0xfedcba9c,
                new ModernSpellProtocol.Request(scope, MATCH, GENERATION, 3, 5),
                new ModernSpellProtocol.Reply(ModernSpellProtocol.Status.DENIED, ModernSpellProtocol.Reason.NOT_ACTIVE, frame));

        assertEquals(100, frame.world().actors().size());
        assertEquals(16, frame.world().manifest().sectors().size());
        assertEquals(100, frame.world().casts().size());
        assertEquals(100, frame.damages().size());
        assertEquals(100, frame.buffs().size());
        assertEquals(ModernWorldProtocol.Phase.FINISHED, frame.world().phase());
        assertEquals(old.world(), frame.world());
        assertEquals(old.vitals(), frame.vitals());
        for (int i = 0; i < 100; i++) {
            var state = frame.items().get(i);
            assertEquals(7, state.stocks().size());
            assertEquals(old.items().get(i).stocks(), state.stocks().subList(0, 3));
            assertEquals(3, state.useCount());
            assertEquals(old.items().get(i).useRevision(), state.useRevision());
            assertEquals(old.items().get(i).restored(), state.restored());
            assertTrue(state.stocks().subList(3, 7).stream().allMatch(s -> s.initial() == 1 && s.remaining() == 1));
            assertEquals(7, frame.weights().get(i).snapshot().units().size());
            assertEquals(ModernTimedEffects.initial(state.entityId()), frame.buffs().get(i));
        }
        assertEquals(128 * 1024, ModernCombatProtocol.maxFrameBytes(9));
        assertEquals(256 * 1024, ModernCombatProtocol.maxFrameBytes(10));
        for (var message : List.<ModernApplicationProtocol.Message>of(combat, item, cast)) {
            byte[] encoded = ModernApplicationProtocol.encode(message, 10);
            // v9 + four stock rows/actor + four weight units/actor + full buff prefix/rows.
            // CAST has an additional target-entity i64 in its echoed request.
            int expected = 111_390 + 4 * 24 * 100 + 4 * 16 * 100 + 8 + 56 * 100
                    + (message instanceof ModernApplicationProtocol.CastResult ? 8 : 0);
            assertEquals(message instanceof ModernApplicationProtocol.CastResult ? 133_006 : 132_998, expected);
            assertEquals(expected, encoded.length);
            assertTrue(encoded.length > 128 * 1024);
            assertTrue(encoded.length < 256 * 1024);
            assertEquals(encoded.length, ByteBuffer.wrap(encoded).getInt(4));
            assertEquals(message instanceof ModernApplicationProtocol.CombatResult ? 23
                            : message instanceof ModernApplicationProtocol.ItemResult ? 35 : 27,
                    Short.toUnsignedInt(ByteBuffer.wrap(encoded).getShort(12)));
            assertArrayEquals(encoded, ModernApplicationProtocol.readFrame(new ByteArrayInputStream(encoded), 10));
            assertEquals(message, ModernApplicationProtocol.decode(encoded, 10));
            assertThrows(ModernApplicationProtocol.ProtocolException.class, () -> ModernApplicationProtocol.encode(message, 9));
            assertThrows(ModernApplicationProtocol.ProtocolException.class, () -> ModernApplicationProtocol.decode(encoded, 9));
            assertThrows(ModernApplicationProtocol.ProtocolException.class,
                    () -> ModernApplicationProtocol.readFrame(new ByteArrayInputStream(encoded), 9));
        }
    }

    @Test void v11HundredHumansAllNineStockWeightsAndFullAmmoFitOnlyTheExtendedLimit()throws Exception {
        var old=maximumFrame();
        var items=old.items().stream().map(item->{
            var stocks=new ArrayList<>(item.stocks());for(long id:new long[]{40013,40014,40016,40068})stocks.add(new ModernItemProtocol.Stock(id,1,1));
            return new ModernItemProtocol.State(item.entityId(),item.requestSequence(),item.useCount(),item.useRevision(),item.itemId(),item.restored(),
                    item.hpBefore(),item.hpAfter(),item.effect(),item.delayMillis(),item.x(),item.y(),item.fixedRecovery(),stocks);
        }).toList();
        var ammo=old.world().actors().stream().map(a->ModernAmmo.State.initial(a.entityId(),List.of(
                new ModernAmmo.Stock(ModernAmmoTest.facts(40744),1_000_000,1_000_000),new ModernAmmo.Stock(ModernAmmoTest.facts(40746),1_000_000,1_000_000)))).toList();
        var weights=items.stream().map(item->{var carried=new ArrayList<ModernInventoryWeight.Carried>();
            for(var stock:item.stocks())carried.add(new ModernInventoryWeight.Carried(stock.itemId(),stock.remaining(),3900));
            for(var stock:ammo.get((int)item.entityId()-1).stocks())carried.add(new ModernInventoryWeight.Carried(stock.itemId(),stock.remaining(),47));
            return new ModernWeightProtocol.State(item.entityId(),ModernInventoryWeightTest.weight(4_000_000_000L,8_000_000_000.5,carried));
        }).toList();
        var frame=new ModernCombatProtocol.Frame(old.world(),old.vitals(),old.damages(),items,weights,
                old.world().actors().stream().map(a->ModernTimedEffects.initial(a.entityId())).toList(),ammo);
        assertTrue(frame.weights().stream().allMatch(w->w.snapshot().units().size()==9));assertTrue(frame.ammo().stream().allMatch(s->s.stocks().size()==2));
        var scope=new ModernRooms.Scope(7,FIRST_OBJECT,-1,3);
        var messages=List.<ModernApplicationProtocol.Message>of(
            new ModernApplicationProtocol.CombatResult(1,new ModernCombatProtocol.Request(scope,ModernCombatProtocol.Action.READ,MATCH,GENERATION,0,0),new ModernCombatProtocol.Reply(ModernCombatProtocol.Status.OK,ModernCombatProtocol.Reason.NONE,frame)),
            new ModernApplicationProtocol.ItemResult(2,new ModernItemProtocol.Request(scope,ModernItemProtocol.Action.READ,MATCH,GENERATION,0,0),new ModernItemProtocol.Reply(ModernItemProtocol.Status.OK,ModernItemProtocol.Reason.NONE,frame)),
            new ModernApplicationProtocol.AmmoResult(3,new ModernAmmoProtocol.Request(scope,ModernAmmoProtocol.Action.READ,MATCH,GENERATION,0,0),new ModernAmmoProtocol.Reply(ModernAmmoProtocol.Status.OK,ModernAmmoProtocol.Reason.NONE,frame)),
            new ModernApplicationProtocol.CastResult(4,new ModernSpellProtocol.Request(scope,MATCH,GENERATION,3,5),new ModernSpellProtocol.Reply(ModernSpellProtocol.Status.DENIED,ModernSpellProtocol.Reason.NOT_ACTIVE,frame)));
        for(var message:messages){byte[] bytes=ModernApplicationProtocol.encode(message,11);
            assertEquals(message instanceof ModernApplicationProtocol.CastResult?156214:156206,bytes.length);
            assertEquals(message,ModernApplicationProtocol.decode(bytes,11));assertArrayEquals(bytes,ModernApplicationProtocol.readFrame(new ByteArrayInputStream(bytes),11));
            for(int version=1;version<=10;version++){int prior=version;
                assertThrows(ModernApplicationProtocol.ProtocolException.class,()->ModernApplicationProtocol.encode(message,prior));
                assertThrows(ModernApplicationProtocol.ProtocolException.class,()->ModernApplicationProtocol.decode(bytes,prior));}
            byte[] extra=java.util.Arrays.copyOf(bytes,bytes.length+8);ByteBuffer.wrap(extra).putInt(4,extra.length);
            assertThrows(ModernApplicationProtocol.ProtocolException.class,()->ModernApplicationProtocol.decode(extra,11));
        }
    }

    private static ModernCombatProtocol.Frame maximumRegenFrame() {
        var old=maximumFrame();
        var items=old.items().stream().map(item->{var stocks=new ArrayList<>(item.stocks());for(long id:new long[]{40013,40014,40016,40068})stocks.add(new ModernItemProtocol.Stock(id,1,1));
            return new ModernItemProtocol.State(item.entityId(),item.requestSequence(),item.useCount(),item.useRevision(),item.itemId(),item.restored(),item.hpBefore(),item.hpAfter(),item.effect(),item.delayMillis(),item.x(),item.y(),item.fixedRecovery(),stocks);}).toList();
        var ammo=old.world().actors().stream().map(a->ModernAmmo.State.initial(a.entityId(),List.of(new ModernAmmo.Stock(ModernAmmoTest.facts(40744),1_000_000,1_000_000),new ModernAmmo.Stock(ModernAmmoTest.facts(40746),1_000_000,1_000_000)))).toList();
        var weights=items.stream().map(item->{var carried=new ArrayList<ModernInventoryWeight.Carried>();for(var s:item.stocks())carried.add(new ModernInventoryWeight.Carried(s.itemId(),s.remaining(),3900));
            for(var s:ammo.get((int)item.entityId()-1).stocks())carried.add(new ModernInventoryWeight.Carried(s.itemId(),s.remaining(),47));
            return new ModernWeightProtocol.State(item.entityId(),ModernInventoryWeightTest.weight(4_000_000_000L,8_000_000_000.5,carried));}).toList();
        var vitals=old.vitals().stream().map(v->new ModernCombatProtocol.Vital(v.entityId(),v.hp(),v.maxHp(),v.mp(),v.maxMp(),v.ac(),v.weaponType(),v.attackSequence(),v.attackRevision(),
                v.hurtRevision()==0?1:v.hurtRevision(),v.deathRevision(),v.lastTargetEntityId(),v.attackIntervalNanos())).toList();
        long healing=9_000_000_120L; // three actual admitted heals, below the stock-derived upper bound.
        var regen=vitals.stream().map(v->new ModernRegenProtocol.State(v.entityId(),0,0,0,0,0,0,0,0,0,0,healing,Math.subtractExact(Math.addExact(v.maxHp(),healing),v.hp()))).toList();
        var frame=new ModernCombatProtocol.Frame(old.world(),vitals,old.damages(),items,weights,old.world().actors().stream().map(a->ModernTimedEffects.initial(a.entityId())).toList(),ammo,regen);
        return frame;
    }
    @Test void v12HundredActorsFullNineWeightsAmmoAndRegenFitTheSame256KiBLimit()throws Exception {
        var frame=maximumRegenFrame();
        var scope=new ModernRooms.Scope(7,FIRST_OBJECT,-1,3);
        var messages=List.<ModernApplicationProtocol.Message>of(
            new ModernApplicationProtocol.CombatResult(1,new ModernCombatProtocol.Request(scope,ModernCombatProtocol.Action.READ,MATCH,GENERATION,0,0),new ModernCombatProtocol.Reply(ModernCombatProtocol.Status.OK,ModernCombatProtocol.Reason.NONE,frame)),
            new ModernApplicationProtocol.ItemResult(2,new ModernItemProtocol.Request(scope,ModernItemProtocol.Action.READ,MATCH,GENERATION,0,0),new ModernItemProtocol.Reply(ModernItemProtocol.Status.OK,ModernItemProtocol.Reason.NONE,frame)),
            new ModernApplicationProtocol.AmmoResult(3,new ModernAmmoProtocol.Request(scope,ModernAmmoProtocol.Action.READ,MATCH,GENERATION,0,0),new ModernAmmoProtocol.Reply(ModernAmmoProtocol.Status.OK,ModernAmmoProtocol.Reason.NONE,frame)),
            new ModernApplicationProtocol.CastResult(4,new ModernSpellProtocol.Request(scope,MATCH,GENERATION,3,5),new ModernSpellProtocol.Reply(ModernSpellProtocol.Status.DENIED,ModernSpellProtocol.Reason.NOT_ACTIVE,frame)));
        for(var message:messages){var bytes=ModernApplicationProtocol.encode(message,12);assertEquals(message instanceof ModernApplicationProtocol.CastResult?166622:166614,bytes.length);
            assertEquals(message,ModernApplicationProtocol.decode(bytes,12));assertArrayEquals(bytes,ModernApplicationProtocol.readFrame(new ByteArrayInputStream(bytes),12));
            for(int version=1;version<=11;version++){int previous=version;assertThrows(ModernApplicationProtocol.ProtocolException.class,()->ModernApplicationProtocol.encode(message,previous));assertThrows(ModernApplicationProtocol.ProtocolException.class,()->ModernApplicationProtocol.decode(bytes,previous));}
        }
    }

    @Test void v13HundredActorsTwoObjectivesAllHistoriesAndAppendicesStayBoundedAndRejectOldVersions()throws Exception {
        var old=maximumRegenFrame();var w=old.world();var summary=w.room().summary();var config=summary.config();
        var room=new ModernRooms.RoomSnapshot(new ModernRooms.Summary(summary.id(),summary.revision(),new ModernRooms.Config(config.name(),100,5,532,config.attributes()),100,100,true),w.room().owner(),w.room().members());
        var event=w.completion().event();var result=new ModernMatchSettlement.Event(event.resultId(),event.matchId(),event.generation(),event.engineRevision(),5,532,event.finishedAtMillis(),event.participants());
        var world=new ModernWorldProtocol.Frame(w.matchId(),w.generation(),w.revision(),w.phase(),w.abortReason(),room,w.manifest(),w.actors(),new ModernWorldProtocol.Completion(result,w.completion().state()),w.casts());
        var origin=w.manifest().sectors().get(0).key().origin();var towers=List.of(
                ModernHonor.Objective.initial(100,new ModernHonor.Tower(0,new ModernWorldMap.Cell(origin.x()+12,origin.y()+11),2665,3000)),
                ModernHonor.Objective.initial(100,new ModernHonor.Tower(1,new ModernWorldMap.Cell(origin.x()+12,origin.y()+12),2669,3000)).damaged(3000,50,2,FINAL_EVENT_REVISION));
        var vitals=old.vitals().stream().map(v->v.hp()>0?v:new ModernCombatProtocol.Vital(v.entityId(),v.hp(),v.maxHp(),v.mp(),v.maxMp(),v.ac(),v.weaponType(),v.attackSequence(),v.attackRevision(),390,390,v.lastTargetEntityId(),v.attackIntervalNanos())).toList();
        var lives=new ArrayList<ModernHonor.Life>();for(int i=0;i<100;i++){var v=vitals.get(i);var l=ModernHonor.Life.initial(v.entityId());if(v.hp()==0)l=l.died(v.deathRevision(),100,w.actors().get(i).position()).terminal();lives.add(l);}
        var heals=w.actors().stream().map(a->ModernHonor.Heal.initial(a.entityId(),4)).toList();var honor=new ModernHonor.Snapshot(towers,lives,heals);
        var damages=new ArrayList<>(old.damages());var d=damages.get(49);damages.set(49,new ModernDamageSpellProtocol.State(d.entityId(),d.castCount(),d.castRevision(),d.mpSpent(),d.skillId(),102,d.fromX(),d.fromY(),towers.get(1).x(),towers.get(1).y(),d.mpCost()));
        var frame=new ModernCombatProtocol.Frame(world,vitals,damages,old.items(),old.weights(),old.buffs(),old.ammo(),old.regen(),honor);
        var scope=new ModernRooms.Scope(7,FIRST_OBJECT,-1,3);
        var messages=List.<ModernApplicationProtocol.Message>of(
            new ModernApplicationProtocol.CombatResult(1,new ModernCombatProtocol.Request(scope,ModernCombatProtocol.Action.READ,MATCH,GENERATION,0,0),new ModernCombatProtocol.Reply(ModernCombatProtocol.Status.OK,ModernCombatProtocol.Reason.NONE,frame)),
            new ModernApplicationProtocol.ItemResult(2,new ModernItemProtocol.Request(scope,ModernItemProtocol.Action.READ,MATCH,GENERATION,0,0),new ModernItemProtocol.Reply(ModernItemProtocol.Status.OK,ModernItemProtocol.Reason.NONE,frame)),
            new ModernApplicationProtocol.AmmoResult(3,new ModernAmmoProtocol.Request(scope,ModernAmmoProtocol.Action.READ,MATCH,GENERATION,0,0),new ModernAmmoProtocol.Reply(ModernAmmoProtocol.Status.OK,ModernAmmoProtocol.Reason.NONE,frame)),
            new ModernApplicationProtocol.CastResult(4,new ModernSpellProtocol.Request(scope,MATCH,GENERATION,3,1,101),new ModernSpellProtocol.Reply(ModernSpellProtocol.Status.DENIED,ModernSpellProtocol.Reason.NOT_ACTIVE,frame)));
        for(var message:messages){var bytes=ModernApplicationProtocol.encode(message,13);assertEquals(message instanceof ModernApplicationProtocol.CastResult?182022:182014,bytes.length);
            assertTrue(bytes.length<256*1024);assertEquals(message,ModernApplicationProtocol.decode(bytes,13));assertArrayEquals(bytes,ModernApplicationProtocol.readFrame(new ByteArrayInputStream(bytes),13));
            for(int version=1;version<=12;version++){int prior=version;assertThrows(ModernApplicationProtocol.ProtocolException.class,()->ModernApplicationProtocol.encode(message,prior));assertThrows(ModernApplicationProtocol.ProtocolException.class,()->ModernApplicationProtocol.decode(bytes,prior));}
            var extra=java.util.Arrays.copyOf(bytes,bytes.length+8);ByteBuffer.wrap(extra).putInt(4,extra.length);assertThrows(ModernApplicationProtocol.ProtocolException.class,()->ModernApplicationProtocol.decode(extra,13));
        }
    }

    @Test void v14HundredActorsAllAppendicesAndFortyEightDistinctEnemyReceiptsExceedOldLimitButRoundTripBoundedly()throws Exception {
        // 101 targets is only the grammar ceiling. Here each historical cast
        // has 48 distinct enemy cells around its own centre (not overlapping
        // teammates or 101 simultaneous occupants in a seven-by-seven area).
        // These are zero-damage historical casts; no corpse can be killed twice.
        var old=maximumRegenFrame();var w=old.world();var original=w.room().summary();var c=original.config();
        var room=new ModernRooms.RoomSnapshot(new ModernRooms.Summary(original.id(),original.revision(),
                new ModernRooms.Config(c.name(),100,5,532,c.attributes()),100,100,true),w.room().owner(),w.room().members());
        var casts=w.casts().stream().map(s->new ModernSpellProtocol.CastState(s.entityId(),4,s.castCount(),s.castRevision(),s.mpSpent(),s.skillId(),s.fromX(),s.fromY(),s.toX(),s.toY(),s.castIntervalMillis())).toList();
        var world=new ModernWorldProtocol.Frame(w.matchId(),w.generation(),20000,ModernWorldProtocol.Phase.ACTIVE,ModernWorldProtocol.AbortReason.NONE,room,w.manifest(),w.actors(),null,casts);
        var vitals=old.vitals().stream().map(v->new ModernCombatProtocol.Vital(v.entityId(),4_000_000_080L,v.maxHp(),v.mp()-48,v.maxMp(),v.ac(),v.weaponType(),v.attackSequence(),v.attackRevision(),5000,0,v.lastTargetEntityId(),v.attackIntervalNanos())).toList();
        var regen=vitals.stream().map(v->new ModernRegenProtocol.State(v.entityId(),0,0,0,0,0,0,0,0,0,0,9_000_000_120L,v.maxHp()+9_000_000_120L-v.hp())).toList();
        var origin=w.manifest().sectors().get(0).key().origin();
        var honor=new ModernHonor.Snapshot(List.of(
                ModernHonor.Objective.initial(100,new ModernHonor.Tower(0,new ModernWorldMap.Cell(origin.x()+12,origin.y()+11),2665,3000)),
                ModernHonor.Objective.initial(100,new ModernHonor.Tower(1,new ModernWorldMap.Cell(origin.x()+12,origin.y()+12),2669,3000))),
                w.actors().stream().map(a->ModernHonor.Life.initial(a.entityId())).toList(),w.actors().stream().map(a->ModernHonor.Heal.initial(a.entityId(),4)).toList());
        var fires=new ArrayList<ModernFireStormProtocol.State>();
        for(int i=0;i<100;i++){
            var targets=new ArrayList<ModernFireStormProtocol.Target>();int offset=0;
            for(int j=i<50?50:0;j<(i<50?98:48);j++){
                int cell=offset++;if(cell>=24)cell++;int x=origin.x()+17+cell%7,y=origin.y()+17+cell/7;
                var v=vitals.get(j);targets.add(new ModernFireStormProtocol.Target(v.entityId(),v.hp(),v.hp(),0,regen.get(j).damageLoss(),0,x,y));
            }
            fires.add(new ModernFireStormProtocol.State(i+1,1,10000+i,48,3,origin.x()+20,origin.y()+20,48,2000,targets));
        }
        var frame=new ModernCombatProtocol.Frame(world,vitals,old.damages(),old.items(),old.weights(),old.buffs(),old.ammo(),regen,honor,fires);
        var scope=new ModernRooms.Scope(7,FIRST_OBJECT,-1,3);
        var messages=List.<ModernApplicationProtocol.Message>of(
            new ModernApplicationProtocol.CombatResult(1,new ModernCombatProtocol.Request(scope,ModernCombatProtocol.Action.READ,MATCH,GENERATION,0,0),new ModernCombatProtocol.Reply(ModernCombatProtocol.Status.OK,ModernCombatProtocol.Reason.NONE,frame)),
            new ModernApplicationProtocol.ItemResult(2,new ModernItemProtocol.Request(scope,ModernItemProtocol.Action.READ,MATCH,GENERATION,0,0),new ModernItemProtocol.Reply(ModernItemProtocol.Status.OK,ModernItemProtocol.Reason.NONE,frame)),
            new ModernApplicationProtocol.AmmoResult(3,new ModernAmmoProtocol.Request(scope,ModernAmmoProtocol.Action.READ,MATCH,GENERATION,0,0),new ModernAmmoProtocol.Reply(ModernAmmoProtocol.Status.OK,ModernAmmoProtocol.Reason.NONE,frame)),
            new ModernApplicationProtocol.CastResult(4,new ModernSpellProtocol.Request(scope,MATCH,GENERATION,4,70),new ModernSpellProtocol.Reply(ModernSpellProtocol.Status.DENIED,ModernSpellProtocol.Reason.TOO_FAST,frame)));
        assertEquals(1_048_576,ModernCombatProtocol.maxFrameBytes(14));assertEquals(262144,ModernCombatProtocol.maxFrameBytes(13));
        for(var message:messages){var bytes=ModernApplicationProtocol.encode(message,14);
            // ACTIVE has no completion: subtract the old 2864-byte result;
            // all other complete appendices remain in this payload.
            assertTrue(bytes.length>ModernEquipmentProtocol.MAX_FRAME_BYTES);assertTrue(bytes.length<1_048_576);
            assertEquals(message,ModernApplicationProtocol.decode(bytes,14));assertArrayEquals(bytes,ModernApplicationProtocol.readFrame(new ByteArrayInputStream(bytes),14));
            assertThrows(ModernApplicationProtocol.ProtocolException.class,()->ModernApplicationProtocol.encode(message,13));
            assertThrows(ModernApplicationProtocol.ProtocolException.class,()->ModernApplicationProtocol.decode(bytes,13));
            assertThrows(ModernApplicationProtocol.ProtocolException.class,()->ModernApplicationProtocol.readFrame(new ByteArrayInputStream(bytes),13));
            byte[] overflow=java.util.Arrays.copyOf(bytes,16);ByteBuffer.wrap(overflow).putInt(4,1_048_577);
            assertThrows(ModernApplicationProtocol.ProtocolException.class,()->ModernApplicationProtocol.readFrame(new ByteArrayInputStream(overflow),14));
        }
        var descriptor=new ModernMatchDescriptor(ModernMatchDescriptor.Ruleset.HONOR_TOWERS,1023,3000,30000,3_000_000_000L,160000);
        var full=new ModernCombatProtocol.Frame(world.withDescriptor(descriptor),vitals,old.damages(),old.items(),old.weights(),old.buffs(),old.ammo(),regen,honor,fires);
        var current=List.<ModernApplicationProtocol.Message>of(
            new ModernApplicationProtocol.CombatResult(1,new ModernCombatProtocol.Request(scope,ModernCombatProtocol.Action.READ,MATCH,GENERATION,0,0),new ModernCombatProtocol.Reply(ModernCombatProtocol.Status.OK,ModernCombatProtocol.Reason.NONE,full)),
            new ModernApplicationProtocol.ItemResult(2,new ModernItemProtocol.Request(scope,ModernItemProtocol.Action.READ,MATCH,GENERATION,0,0),new ModernItemProtocol.Reply(ModernItemProtocol.Status.OK,ModernItemProtocol.Reason.NONE,full)),
            new ModernApplicationProtocol.AmmoResult(3,new ModernAmmoProtocol.Request(scope,ModernAmmoProtocol.Action.READ,MATCH,GENERATION,0,0),new ModernAmmoProtocol.Reply(ModernAmmoProtocol.Status.OK,ModernAmmoProtocol.Reason.NONE,full)),
            new ModernApplicationProtocol.CastResult(4,new ModernSpellProtocol.Request(scope,MATCH,GENERATION,4,70),new ModernSpellProtocol.Reply(ModernSpellProtocol.Status.DENIED,ModernSpellProtocol.Reason.TOO_FAST,full)));
        for(var message:current){var bytes=ModernApplicationProtocol.encode(message,15);
            assertEquals(ModernApplicationProtocol.encode(message,14).length+40,bytes.length);
            assertEquals(message,ModernApplicationProtocol.decode(bytes,15));assertArrayEquals(bytes,ModernApplicationProtocol.readFrame(new ByteArrayInputStream(bytes),15));
            byte[] overflow=java.util.Arrays.copyOf(bytes,16);ByteBuffer.wrap(overflow).putInt(4,1_048_577);
            assertThrows(ModernApplicationProtocol.ProtocolException.class,()->ModernApplicationProtocol.readFrame(new ByteArrayInputStream(overflow),15));
        }
    }

    private static ModernCombatProtocol.Frame maximumFrame() {
        var members = new ArrayList<ModernRooms.Member>();
        var actors = new ArrayList<ModernBattleEngine.Actor>();
        var participants = new ArrayList<ModernMatchSettlement.Participant>();
        var vitals = new ArrayList<ModernCombatProtocol.Vital>();
        var casts = new ArrayList<ModernSpellProtocol.CastState>();
        var damages = new ArrayList<ModernDamageSpellProtocol.State>();
        var items = new ArrayList<ModernItemProtocol.State>();
        var origin = new ModernWorldMap.SectorKey(0x7fff, 0x8000).origin();
        for (int i = 0; i < 100; i++) {
            long entity = i + 1L;
            int team = i < 50 ? 0 : 1;
            var key = new TournamentPresence.CharacterKey(7, FIRST_OBJECT + i);
            String name = "漢".repeat(84) + String.format(Locale.ROOT, "%04d", i);
            var member = new ModernRooms.Member(key, name, "源".repeat(84) + "ABCD",
                    4_000_000_000L + i, i % 7, i % 2, team, true);
            members.add(member);
            int x = origin.x() + 2 + i % 10, y = origin.y() + 2 + i / 10;
            actors.add(new ModernBattleEngine.Actor(entity, member, new ModernWorldMap.Cell(x, y),
                    i % 8, true, 0x4_0000_0000L + i, 50_000_000));
            participants.add(new ModernMatchSettlement.Participant(entity, key, team,
                    team == 0 ? ModernMatchSettlement.Verdict.WIN : ModernMatchSettlement.Verdict.LOSS));
            long death = team == 0 ? 0 : i == 99 ? FINAL_EVENT_REVISION : 390;
            vitals.add(new ModernCombatProtocol.Vital(entity, team == 0 ? 4_000_000_080L : 0,
                    8_000_000_000L, 3_999_999_987L, 4_000_000_000L, Long.MIN_VALUE, 0,
                    0, 0, death, death, 0, 1_000_000_000));
            casts.add(new ModernSpellProtocol.CastState(entity, 2, 1, 10 + entity, 10,
                    5, x - 1, y, x, y, 10_000));
            int targetIndex = i < 50 ? i + 50 : i - 50;
            damages.add(new ModernDamageSpellProtocol.State(entity, 1,
                    i == 49 ? FINAL_EVENT_REVISION : 210 + entity, 3, 4, targetIndex + 1L,
                    x, y, origin.x() + 2 + targetIndex % 10, origin.y() + 2 + targetIndex / 10, 3));
            items.add(new ModernItemProtocol.State(entity, 4, 3, 110 + entity,
                    40012, 3_000_000_080L, 1_000_000_000L, 4_000_000_080L, 197, 1000,
                    x, y, 3_000_000_000L, List.of(
                        new ModernItemProtocol.Stock(40010, 1_000_000, 999_999),
                        new ModernItemProtocol.Stock(40011, 1_000_000, 999_999),
                        new ModernItemProtocol.Stock(40012, 1_000_000, 999_999))));
        }
        var summary = new ModernRooms.Summary(0x5_0000_0006L, GENERATION,
                new ModernRooms.Config("房".repeat(20) + "ABCD", 100, 1, 504, 32), 100, 100, true);
        var room = new ModernRooms.RoomSnapshot(summary, members.get(0).key(), members);
        var sectors = new ArrayList<ModernWorldMap.SectorManifest>();
        for (int x = 0; x < 4; x++) for (int y = 0; y < 4; y++)
            sectors.add(new ModernWorldMap.SectorManifest(new ModernWorldMap.SectorKey(0x7fff + x, 0x8000 + y),
                    2 * 1024 * 1024, "11".repeat(32)));
        var manifest = new ModernWorldMap.Manifest(514, sectors, "22".repeat(32));
        var event = new ModernMatchSettlement.Event(new UUID(0x12345678, 0x11223344), MATCH, GENERATION,
                ENGINE_REVISION, 1, 504, Long.MAX_VALUE, participants);
        var world = new ModernWorldProtocol.Frame(MATCH, GENERATION, SAVED_WORLD_REVISION,
                ModernWorldProtocol.Phase.FINISHED, ModernWorldProtocol.AbortReason.NONE, room, manifest, actors,
                new ModernWorldProtocol.Completion(event, ModernWorldProtocol.SettlementState.SAVED), casts);
        return new ModernCombatProtocol.Frame(world, vitals, damages, items);
    }
}
