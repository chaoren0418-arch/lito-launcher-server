package local.lito381.server;

import java.util.List;

/** Strict output adapter only. Canonical capture/owner observation never calls
 * these <=34 constructors or their current-capacity/fixed-price validators. */
final class ModernLegacyBattleProjection {
    static ModernBattleEngine.Snapshot from(ModernBattleObservation.Capture c) {
        if(c.hasVip())throw new IllegalArgumentException("VIP cannot be projected as legacy equipment");
        if(c.features().armorProtection())throw new IllegalArgumentException("armor protection needs a negotiated modern projection");
        if(c.features().playerHealing())throw new IllegalArgumentException("player healing needs a negotiated modern projection");
        if(c.features().greaterHaste())throw new IllegalArgumentException("greater haste needs a negotiated modern projection");
        var f=c.features();var actors=c.actors();var m=c.modes();
        var spells=actors.stream().map(a->ModernLegacySpellProjection.from(a.pose().entityId(),a.pose().member().team(),
                Math.toIntExact(a.castIntervalNanos()/1_000_000),a.runtime(),a.histories())).toList();
        var snapshot=new ModernBattleEngine.Snapshot(c.identity().matchId(),c.identity().generation(),c.revision(),c.phase(),c.abortReason(),
                c.room(),c.manifest(),actors.stream().map(ModernBattleObservation.CapturedActor::pose).toList(),
                f.combat()?actors.stream().map(ModernLegacyBattleProjection::vital).toList():List.of(),c.result(),
                f.spells()?spells.stream().map(ModernLegacySpellProjection.Projection::teleport).toList():List.of(),
                f.arrow()?spells.stream().map(ModernLegacySpellProjection.Projection::arrow).toList():List.of(),
                f.items()?actors.stream().map(ModernLegacyBattleProjection::supplies).toList():List.of(),
                f.weight()?actors.stream().map(a->new ModernWeightProtocol.State(a.pose().entityId(),a.weight())).toList():List.of(),
                f.timed()?actors.stream().map(ModernBattleObservation.CapturedActor::buffs).toList():List.of(),
                f.ammo()?actors.stream().map(ModernBattleObservation.CapturedActor::ammo).toList():List.of(),
                f.regen()?actors.stream().map(ModernLegacyBattleProjection::regeneration).toList():List.of(),
                m.honor().enabled()?new ModernHonor.Snapshot(m.honor().objectives(),m.honor().lives(),spells.stream().map(ModernLegacySpellProjection.Projection::healing).toList()):ModernHonor.Snapshot.disabled(),
                f.fire()?spells.stream().map(ModernLegacySpellProjection.Projection::fireStorm).toList():List.of(),
                m.race()==null?ModernPetRaceProtocol.Snapshot.disabled():ModernPetRaceProtocol.fromState(m.race(),m.raceResultId()),m.isolatedForces(),quest(m.quest()),
                f.npcArrow()?spells.stream().map(ModernLegacySpellProjection.Projection::npcArrow).toList():List.of(),m.timeAttack(),m.siege(),
                f.disintegrate()?spells.stream().map(ModernLegacySpellProjection.Projection::disintegrate).toList():List.of(),
                f.npcDisintegrate()?spells.stream().map(ModernLegacySpellProjection.Projection::npcDisintegrate).toList():List.of(),
                f.npcFire()?spells.stream().map(ModernLegacySpellProjection.Projection::npcFireStorm).toList():List.of());
        snapshot.timeAttack().validate(snapshot);snapshot.siege().validate(snapshot);return snapshot;
    }
    static ModernNpcBattle.Frame npcs(ModernBattleObservation.Capture c) {
        if(c.npcs()==null)throw new IllegalArgumentException("NPC observation unavailable");
        return new ModernNpcBattle.Frame(from(c),c.npcs().roster(),c.npcs().exchanges());
    }
    private static ModernQuestObjectives.Snapshot quest(ModernBattleObservation.Quest q) {
        return q.progress()==null||q.progress().plan().objectives()==null?ModernQuestObjectives.Snapshot.disabled()
                :new ModernQuestObjectives.Snapshot(q.progress(),q.objectives(),q.exchanges(),q.lives());
    }
    private static ModernCombatProtocol.Vital vital(ModernBattleObservation.CapturedActor a) {
        var v=a.vital();var h=v.values();
        return new ModernCombatProtocol.Vital(a.pose().entityId(),h.hp(),h.maximumHp(),h.mp(),h.maximumMp(),v.ac(),v.weaponType(),
                v.attackSequence(),v.attackRevision(),v.hurtRevision(),v.deathRevision(),v.target(),v.attackIntervalNanos());
    }
    private static ModernItemProtocol.State supplies(ModernBattleObservation.CapturedActor a) {
        var s=a.supplies();var h=s.history();var use=h.last();long entity=a.pose().entityId();
        if(use==null)return new ModernItemProtocol.State(entity,s.requestSequence(),0,0,0,0,0,0,0,0,0,0,h.fixedRecovery(),s.stocks());
        return new ModernItemProtocol.State(entity,s.requestSequence(),h.uses(),use.revision(),use.itemId(),use.restored(),use.before().vitals().hp(),use.hpAfter(),
                s.effect(),s.delayMillis(),use.before().x(),use.before().y(),h.fixedRecovery(),s.stocks());
    }
    private static ModernRegenProtocol.State regeneration(ModernBattleObservation.CapturedActor a) {
        var r=a.regeneration();var h=r.hp();var m=r.mp();
        return new ModernRegenProtocol.State(a.pose().entityId(),h.gain(),h.loss(),h.revision(),h.before(),h.after(),
                m.gain(),m.loss(),m.revision(),m.before(),m.after(),r.itemGain(),r.damageLoss());
    }
    private ModernLegacyBattleProjection(){}
}
