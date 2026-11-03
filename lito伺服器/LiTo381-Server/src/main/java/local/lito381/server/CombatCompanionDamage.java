package local.lito381.server;

/** Source PC-to-owned-NPC damage. No actor or cast is retained by this helper. */
final class CombatCompanionDamage {
    static final String KEY="PC_TO_PET_DAMAGE_MULTIPLIER";
    private static final String P="local/lito381/server/";
    record Physical(int rawDamage,int reduction,boolean hardRanged,double criticalFraction) {}
    static BattleCompanion controlled(Object target) {
        return target instanceof BattleCompanion c?c:(BattleCompanion)CombatAccess.get(target,"controlled");
    }
    static boolean eligible(Object target) {
        var controlled=controlled(target);if(controlled==null)return false;
        var kind=controlled.snapshot().kind();return kind==BattleCompanion.Kind.PET||kind==BattleCompanion.Kind.SUMMON||kind==BattleCompanion.Kind.TAMED;
    }
    static double multiplier(){return CombatRules.number(KEY,0.125);}
    static Physical physicalAttempt(BattlePhysicalDefense.NpcOutcome outcome,int raw,int beforeCritical,BattleWeaponCritical.DamageOutcome critical,Object context) {
        if(!CombatRules.loaded()||!eligible(context))return null;
        double fraction=(beforeCritical+(double)critical.fixedBonus())*critical.multiplier()-critical.damage();
        return new Physical(raw,outcome.totalReduction(),outcome.hardRangedHalved(),fraction);
    }
    static int physical(int legacy,ClientSession caster,ClientSession player,SourceWeaponPhysicalStats weapon,
                        BattleWeaponCritical.DamageOutcome critical,CombatKiringku.Defense defense,boolean hit,boolean armorBreak,
                        Physical attempt,Object context) {
        if(!CombatRules.loaded()||attempt==null||!eligible(context))return CombatKiringku.finish(legacy,caster,player,weapon,critical,defense,hit,armorBreak);
        if(!hit)return 0;
        if(weapon.weaponType()==17)return CombatKiringku.finish(legacy,caster,player,weapon,critical,defense,true,armorBreak,multiplier());
        double damage=(attempt.rawDamage()+attempt.criticalFraction()-attempt.reduction())*multiplier();
        if(attempt.hardRanged())damage/=2; // Source Armor Break applies only to player defense.
        String key=CombatClassDamage.key(caster.battleCharacterType(),false);if(key!=null)damage*=CombatRules.number(key,1);
        return Math.max(0,(int)damage);
    }
    static CombatMagicDamage.Attempt ordinary(ClientSession caster,int skill,int attribute,int source,int elemental,int mr,int armor,String description,ClientSession player,Object companion,boolean complete) {
        if(!CombatRules.loaded()||player!=null)return CombatMagicDamage.ordinary(caster,skill,attribute,source,elemental,mr,armor,description,player);
        if(!eligible(companion))return CombatNpcMagic.ordinary(caster,skill,attribute,source,elemental,mr,armor,description,complete);
        var raw=(CombatMagicDamage.Attempt)CombatAccess.call(caster,"sourceNpcMagicBeforeResistance","(IIIIIILjava/lang/String;)L"+P+"CombatMagicDamage$Attempt;",skill,attribute,source,elemental,mr,armor,description);
        var attempt=new CombatMagicDamage.Attempt(raw.damage(),raw.magicResistance(),raw.originalMagicHit(),null,true);
        return complete?new CombatMagicDamage.Attempt(magic(attempt.damage(),attempt,false),mr,attempt.originalMagicHit(),null,true):attempt;
    }
    static CombatMagicDamage.Attempt finalBurn(int source,int armor,int offset,int mr,int hit,ClientSession player,Object companion,boolean complete) {
        if(!CombatRules.loaded()||player!=null||!eligible(companion))return CombatMagicDamage.finalBurn(source,armor,offset,mr,hit,player);
        var result=BattleFinalBurn.resolve(source,armor,offset,mr,hit);
        var deferred=new BattleFinalBurn.Result(result.sourceMp(),result.armorReduction(),result.preMrDamage(),result.preMrDamage(),result.casterHpAfter(),result.casterMpAfter());
        return new CombatMagicDamage.Attempt(deferred.damage(),mr,hit,deferred,true);
    }
    static int beforeCritical(BattleSpellCatalog.AttackSpell spell,int damage,boolean critical,ClientSession player,CombatMagicDamage.Attempt attempt) {
        if(CombatRules.loaded()&&attempt!=null&&attempt.companion())return magic(damage,attempt,critical);
        if(CombatRules.loaded()&&attempt!=null&&attempt.npc())return CombatNpcMagic.magic(damage,attempt,critical);
        return CombatMagicDamage.beforeCritical(spell,damage,critical,player);
    }
    static int ordinaryComplete(ClientSession caster,int skill,int attribute,int source,int elemental,int mr,int armor,String description,Object companion) {
        return ordinary(caster,skill,attribute,source,elemental,mr,armor,description,null,companion,true).damage();
    }
    static int line(ClientSession caster,BattleSpellCatalog.DirectionalLineSpell spell,int source,int elemental,int mr,int armor,String description,Object companion) {
        return ordinaryComplete(caster,spell.skillId(),spell.attribute(),source,elemental,mr,armor,description,companion);
    }
    static int area(ClientSession caster,BattleSpellCatalog.AreaAttackSpell spell,int source,int elemental,int mr,int armor,String description,Object companion) {
        return ordinaryComplete(caster,spell.skillId(),spell.attribute(),source,elemental,mr,armor,description,companion);
    }
    static int magic(int damage,CombatMagicDamage.Attempt attempt,boolean critical) {
        int scaled=(int)(damage*multiplier());if(critical)scaled*=2;
        // Preserve signed source arithmetic through the common NPC MR calculation.
        return Math.max(0,CombatNpcMagic.resistance(scaled,attempt.magicResistance(),attempt.originalMagicHit()));
    }
}
