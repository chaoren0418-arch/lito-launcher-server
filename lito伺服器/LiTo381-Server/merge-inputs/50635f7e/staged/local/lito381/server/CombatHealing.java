// ported from candidate 50635f7e; B07 三方移植（base-0.6.71 / candidate / src/main）。
package local.lito381.server;

/** Source TYPE_HEAL order at native group-heal boundaries; no retained actor state. */
final class CombatHealing {
    private static final String P="local/lito381/server/",T="L"+P+"ClientSession$MassSlowCompanionTarget;";
    static int blessingPlayer(int adjusted) {return CombatRules.naturesBlessing(adjusted);}
    static int groupPlayerBase(int lawful,ClientSession target,long now) {
        if(!CombatRules.loaded())return lawful;
        var water=(BattleWaterLifeStatus)CombatAccess.field(target,"waterLifeStatus");
        int base=water.active(now)?lawful<<1:lawful;water.cancel();return base;
    }
    static int owned(BattleCompanion actor,int lawful,ClientSession caster,int skill,long now) {
        if(!CombatRules.loaded())return NativeSourceControlledHealing.heal(actor,lawful);
        var before=actor.snapshot();if(!before.active()||before.hp()<=0)return before.hp();
        // The selected-source NPC-ID cache is the same immutable template used by single-target healing.
        var accounts=(AccountRepository)CombatAccess.call(caster,"selectedSourceAccounts","()L"+P+"AccountRepository;");
        final SourceNpcPhysicalStats stats;
        try{stats=accounts.npcPhysicalStats(before.npcId());}catch(Exception error){throw new IllegalStateException("group healing source NPC unavailable="+before.npcId(),error);}
        if(stats==null)throw new IllegalStateException("group healing source NPC missing="+before.npcId());
        int adjusted=CombatNpcDebuffs.healingOwned(lawful,actor,now);
        if(skill==164)adjusted=CombatRules.naturesBlessing(adjusted);
        int delta=NativeSourceNpcFactoryType.ownedDelta(NativeSourceHealingType.undead(stats,actor)==1?-adjusted:NativeSourceHealingType.undead(stats,actor)==3?0:adjusted,actor,adjusted);
        if(delta>0)NativeSourceControlledHealing.heal(actor,delta);
        else if(delta<0){
            if(CombatWorldPhysical.world(caster))CombatAccess.call(caster,"applyClassicWorldMonsterCompanionDamage","(L"+P+"BattleCompanion;L"+P+"BattleCompanion$Snapshot;IJLjava/lang/String;)V",actor,before,-delta,caster.playerObjectId(),"group-healing-source-"+skill);
            else {
                var target=CombatAccess.call(caster,"resolveBattleSpellCompanion","(J)"+T,before.objectId());
                if(target!=null&&CombatCompanionDamage.controlled(target)==actor)CombatAccess.integerCall(caster,"applyBattleSpellCompanionDamage","("+T+"II)I",target,-delta,skill);
            }
        }
        return actor.snapshot().hp();
    }
}
