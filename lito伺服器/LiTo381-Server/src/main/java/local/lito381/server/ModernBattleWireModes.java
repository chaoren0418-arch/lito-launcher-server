package local.lito381.server;

import static local.lito381.server.ModernBattleWire.*;
import java.util.Comparator;

/** Explicit schema-one mode/NPC/result fields. No reflection, source catalog,
 * future spawn plan, result recreation or mutable engine is serialized. */
final class ModernBattleWireModes {
    static void honor(Writer w,ModernBattleObservation.Honor v) {
        w.list(v.objectives(),2,(b,o)->{b.l(o.entityId());b.i(o.team());b.i(o.action());b.i(o.graphic());b.i(o.x());b.i(o.y());b.l(o.hp());b.l(o.maximumHp());
            b.l(o.revision());b.l(o.hurtRevision());b.l(o.deathRevision());b.l(o.damageLoss());b.l(o.healGain());b.l(o.lastActorEntityId());b.i(o.lastKind());});
        w.list(v.lives(),100,(b,l)->{b.l(l.entityId());b.l(l.deathCount());b.l(l.lastDeathRevision());b.l(l.respawnCount());b.l(l.respawnRevision());
            b.l(l.respawnHpGain());b.l(l.pendingDueMillis());b.i(l.deathX());b.i(l.deathY());});
    }
    static void quest(Writer w,ModernBattleAudience.Quest v) {
        w.l(v.revision());w.e(v.flow());w.e(v.status());
        w.list(v.lanes(),2,(b,l)->{b.i(l.lane());b.i(l.stage());b.e(l.phase());b.longs(l.living(),128);});
        w.list(v.targets(),8,ModernBattleWireModes::questTarget);
        w.list(v.objectives(),2,(b,o)->{questId(b,o.id());questTarget(b,o.target());b.e(o.kind());b.l(o.hp());b.l(o.bornRevision());b.l(o.revision());b.l(o.hurtRevision());b.l(o.deathRevision());});
        w.list(v.exchanges(),100,(b,e)->{participant(b,e.participant());questId(b,e.target());b.l(e.requestSequence());b.l(e.revision());outcome(b,e.outcome());});
        w.list(v.lives(),100,(b,l)->{b.l(l.entityId());b.i(l.lane());cell(b,l.spawn());b.l(l.deaths());b.l(l.deathRevision());b.l(l.respawns());b.l(l.respawnRevision());b.l(l.hpGain());cell(b,l.deathCell());});
        w.i(v.winningLane());w.l(v.deathRevision());
    }
    private static void questId(Writer w,ModernQuestObjectives.Id v){scope(w,v.scope());w.i(v.slot());}
    private static void questTarget(Writer w,ModernQuestObjectives.Target v){w.i(v.lane());w.i(v.stage());cell(w,v.cell());w.i(v.graphic());w.l(v.maximumHp());}
    static void time(Writer w,ModernBattleAudience.TimeAttack v) {
        w.l(v.revision());w.e(v.phase());w.e(v.end());w.list(v.waves(),32,Writer::e);w.i(v.wave());w.l(v.leader());w.i(v.durationMillis());
        w.l(v.startedMillis());w.l(v.observedMillis());w.longs(v.departed(),60);w.longs(v.living(),128);cell(w,v.tower().cell());w.i(v.tower().graphic());w.l(v.tower().maximumHp());
        w.l(v.towerHp());w.l(v.towerHurtRevision());w.l(v.towerDeathRevision());w.hash(v.planSha256());
        w.list(v.exchanges(),60,(b,e)->{b.l(e.entityId());scope(b,e.target().scope());b.l(e.sequence());b.l(e.revision());outcome(b,e.outcome());});
    }
    static void siege(Writer w,ModernSiege.Snapshot v) {
        var s=v.state();var c=s.config();w.l(s.scope().roomId());w.l(s.scope().matchId());w.l(s.scope().generation());
        w.i(c.key().mode());w.i(c.key().logicalMapId());w.list(c.towers(),3,ModernBattleWireModes::siegeTower);
        // Sets have no stable iteration order. Camps retain configured spawn order.
        w.cells(c.gate().stream().sorted(Comparator.comparingInt(ModernWorldMap.Cell::x).thenComparingInt(ModernWorldMap.Cell::y)).toList(),64);
        w.cells(c.attackCamp(),50);w.cells(c.defenseCamp(),50);cell(w,c.portal().outside());cell(w,c.portal().inside());w.l(c.durationMillis());w.l(c.respawnMillis());
        w.i(s.attacker());w.e(s.phase());w.list(s.objectives(),3,ModernBattleWireModes::siegeObjective);w.l(s.revision());w.l(s.gateRevision());w.l(s.finishRevision());w.l(s.finishedAtMillis());w.i(s.winner());
        w.list(v.lives(),100,(b,l)->{b.l(l.entityId());b.l(l.deaths());b.l(l.deathRevision());b.l(l.respawns());b.l(l.respawnRevision());b.l(l.hpGain());b.l(l.dueMillis());});
    }
    private static void siegeTower(Writer w,ModernSiege.Tower t){w.e(t.slot());cell(w,t.cell());w.i(t.gfx());w.l(t.maximumHp());}
    static void siegeObjective(Writer w,ModernSiege.Objective o){siegeTower(w,o.tower());w.l(o.hp());w.l(o.damageLoss());w.l(o.hitCount());w.l(o.revision());w.l(o.deathRevision());w.l(o.lastActor());}
    static void race(Writer w,ModernBattleAudience.Race v) {
        w.l(v.revision());w.e(v.phase());w.hash(v.courseSha256());w.list(v.gates(),64,(b,g)->{b.i(g.minX());b.i(g.maxX());b.i(g.minY());b.i(g.maxY());});
        w.l(v.activeLimitNanos());w.l(v.observedElapsedNanos());w.l(v.firstFinisherEntity());w.l(v.finishWindowDeadlineElapsedNanos());w.l(v.terminalRevision());w.optional(v.resultId(),Writer::uuid);
        w.list(v.runners(),10,(b,r)->{b.l(r.entityId());b.l(r.revision());b.l(r.progressMoveSequence());b.l(r.completedLaps());b.l(r.nextCheckpoint());
            b.l(r.placement());b.l(r.finishElapsedNanos());b.e(r.dnf());b.l(r.baseGraphicsId());});
    }
    static void npcs(Writer w,ModernBattleAudience.Npcs v) {
        w.l(v.revision());w.l(v.elapsedMillis());w.l(v.spawned());w.flag(v.closed());
        w.list(v.actors(),128,(b,a)->{b.l(a.serial());b.i(a.sourceId());b.i(a.npcId());b.i(a.graphic());b.i(a.lane());b.i(a.stage());b.i(a.heading());b.e(a.action());cell(b,a.cell());
            b.l(a.hp());b.l(a.maximumHp());b.l(a.mp());b.l(a.maximumMp());b.l(a.life());b.l(a.bornRevision());b.l(a.changedRevision());b.l(a.corpseUntilMillis());b.l(a.nextMoveMillis());b.l(a.nextAttackMillis());});
        w.list(v.exchanges(),228,(b,e)->{b.e(e.direction());participant(b,e.participant());b.l(e.participantLife());scope(b,e.npc().scope());b.l(e.npc().serial());b.l(e.npcLife());
            b.l(e.requestSequence());b.l(e.revision());var o=e.outcome();b.flag(o.hit());b.l(o.rawDamage());b.l(o.damage());b.l(o.appliedDamage());b.l(o.remainingHp());b.flag(o.death());
            b.flag(o.maximumTriggered());b.flag(o.criticalTriggered());b.l(o.materialBonus());b.l(o.blessingBonus());});
        w.list(v.resources().graphics(),10,Writer::i);
        w.list(v.names().rows(),128,(b,n)->{b.i(n.sourceId());b.i(n.npcId());b.i(n.graphic());b.text(n.name(),256);b.text(n.nameId(),256);});
    }
    static void result(Writer w,ModernMatchSettlement.Event v) {
        w.uuid(v.resultId());w.l(v.matchId());w.l(v.generation());w.l(v.engineRevision());w.i(v.mode());w.i(v.mapId());w.l(v.finishedAtMillis());
        w.list(v.participants(),100,(b,p)->{b.l(p.entityId());participant(b,p.identity());b.i(p.team());b.e(p.verdict());});
        w.optional(v.timeAttack(),(b,p)->{b.l(p.roomId());b.i(p.physicalMapId());b.hash(p.terrainSha256());b.hash(p.planSha256());b.e(p.end());b.i(p.wave());b.i(p.waveCount());b.i(p.leader());b.i(p.durationMillis());
            b.l(p.startedMillis());b.l(p.observedMillis());b.l(p.aliveMask());b.l(p.departedMask());b.l(p.towerDeathRevision());});
        w.optional(v.siege(),(b,p)->{b.l(p.roomId());b.i(p.physicalMapId());b.hash(p.terrainSha256());b.hash(p.configSha256());b.i(p.attacker());b.i(p.winner());b.e(p.end());
            b.i(p.durationMillis());b.i(p.respawnMillis());b.l(p.elapsedMillis());b.l(p.gateRevision());b.list(p.towers(),3,ModernBattleWireModes::siegeObjective);});
    }
    private ModernBattleWireModes(){}
}
