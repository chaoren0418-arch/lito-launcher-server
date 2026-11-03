package local.lito381.server;

/** Per-cast proposal only; rejected costs cannot consume target status. */
final class CombatReturnNature {
    record Attempt(int probability,int magicResistance,BattleEraseMagicStatus.Lease eraseMagic) { }
    static Attempt prepare(BattleSpellCatalog.ReturnToNatureSpell spell,int casterLevel,int ownerLevel,int intelligence,int magicLevel,int magicHit,int baseMr,
                           ClientSession caster,ClientSession owner,BattleCompanion controlled,long now) {
        if(CombatDatabase.controlsOrNull()==null)return new Attempt(spell.probability(casterLevel,ownerLevel,intelligence,magicLevel,magicHit,baseMr),baseMr,BattleEraseMagicStatus.Lease.none());
        var target=controlled.snapshot();boolean summon=target.kind()==BattleCompanion.Kind.SUMMON||target.kind()==BattleCompanion.Kind.TAMED;
        int level=summon?CombatAccess.integerCall(owner,"battleLevel","()I"):target.level();
        BattleEraseMagicStatus.Lease lease;int mr;boolean blocked;
        synchronized(controlled){lease=controlled.captureEraseMagic(now);mr=lease.present()?baseMr>>2:baseMr;blocked=controlled.earthBindActive(now);}
        int chance;
        if(blocked)chance=0;
        else if(CombatDatabase.controls().probabilities().containsKey(145))chance=CombatProbability.dispatch(spell,"probability","(IIIIII)I","casterLevel,targetOwnerLevel,effectiveIntelligence,magicLevel,magicHit,targetMagicResistance",
                new Object[]{casterLevel,level,intelligence,magicLevel,magicHit,mr},caster,new Object[]{controlled});
        else {
            int originalHit=CombatAccess.integerCall(caster,"originalMagicHit","()I");
            int dice=CombatDatabase.value(0,145,"ReturnToNatureSpell","probabilityDice"),value=CombatDatabase.value(0,145,"ReturnToNatureSpell","probabilityValue");
            int base=Math.addExact((int)((dice/10D)*(casterLevel-level)),value);
            base=Math.addExact(base,Math.addExact(originalHit*2,magicHit));base=Math.subtractExact(base,mr/80);
            var controls=CombatDatabase.controls();var npc=controls.npcAttributes().get(target.npcId());if(npc==null)throw new IllegalStateException("missing Return Nature NPC attributes");
            int boss=npc.boss()?controls.bossModifiers().getOrDefault(145,-1):-1;
            chance=boss==0?0:Math.max(1,boss>0?Math.min(90,base)/boss:Math.min(90,base));
        }
        return new Attempt(chance,mr,lease);
    }
    static void commit(Attempt attempt,BattleCompanion controlled) {
        if(CombatDatabase.controlsOrNull()==null||attempt==null||controlled==null)return;
        controlled.consumeEraseMagic(attempt.eraseMagic());controlled.cancelSleep();
    }
}
