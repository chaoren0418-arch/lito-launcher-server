package local.lito381.server;

/** Source PC->NPC magic order; only scalar attempts are retained within native casts. */
final class CombatNpcMagic {
    private static final String P="local/lito381/server/";
    static int signedPreMr(int value,int ignoredZero){return CombatRules.loaded()?value:Math.max(value,ignoredZero);}
    static CombatMagicDamage.Attempt attempt(int value,int mr,int hit){return new CombatMagicDamage.Attempt(value,mr,hit,null,false,true);}
    static CombatMagicDamage.Attempt ordinary(ClientSession caster,int skill,int attribute,int source,int elemental,int mr,int armor,String description,boolean complete){
        var raw=(CombatMagicDamage.Attempt)CombatAccess.call(caster,"sourceNpcMagicBeforeResistance","(IIIIIILjava/lang/String;)L"+P+"CombatMagicDamage$Attempt;",skill,attribute,source,elemental,mr,armor,description);
        return complete?new CombatMagicDamage.Attempt(magic(raw.damage(),raw,false),-1,raw.originalMagicHit(),null):raw;
    }
    static int ordinaryComplete(ClientSession caster,int skill,int attribute,int source,int elemental,int mr,int armor,String description){
        if(!CombatRules.loaded())return CombatAccess.integerCall(caster,"ordinaryMagicDamage","(IIIIIILjava/lang/String;)I",skill,attribute,source,elemental,mr,armor,description);
        return ordinary(caster,skill,attribute,source,elemental,mr,armor,description,true).damage();
    }
    static int line(ClientSession caster,BattleSpellCatalog.DirectionalLineSpell spell,int source,int elemental,int mr,int armor,String description){return ordinaryComplete(caster,spell.skillId(),spell.attribute(),source,elemental,mr,armor,description);}
    static int area(ClientSession caster,BattleSpellCatalog.AreaAttackSpell spell,int source,int elemental,int mr,int armor,String description){return ordinaryComplete(caster,spell.skillId(),spell.attribute(),source,elemental,mr,armor,description);}
    static int resistance(int damage,int mr,int hit){
        int floor=mr==0?1:mr<=50?2:mr<=100?3:mr<=120?4:mr<=140?5:mr<=160?6:mr<=180?7:mr<=200?8:mr<=220?9:mr<=240?10:11;
        double coefficient=mr<=50?1:mr<=120?0.9:mr<=160?0.8:mr<=200?0.7:mr<=240?0.6:0.5;
        return (int)(damage*(coefficient-0.01*Math.floor((mr-hit)/(double)floor)));
    }
    static int magic(int damage,CombatMagicDamage.Attempt attempt,boolean critical){if(critical)damage*=2;return Math.max(0,resistance(damage,attempt.magicResistance(),attempt.originalMagicHit()));}
}
