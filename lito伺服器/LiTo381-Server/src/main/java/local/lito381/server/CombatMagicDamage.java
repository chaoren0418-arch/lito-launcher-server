package local.lito381.server;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

/** Source PC magic order. Attempts are method-local values, never actor owners. */
final class CombatMagicDamage {
    private static final String ORDINARY="(IIIIIILjava/lang/String;)I";
    private static final String PRE_MR="(IIIIIILjava/lang/String;)Llocal/lito381/server/CombatMagicDamage$Attempt;";
    private static final List<String> KEYS=List.of("Crown","Knight","Elf","Wizard","Darkelf","DragonKnight","Illusionist")
            .stream().map(job->"Other_ADD_is"+job+"_MagicPC").toList();
    private static final Set<String> KEY_SET=Set.copyOf(KEYS);
    static Set<String> keys(){return KEY_SET;}
    record Attempt(int damage,int magicResistance,int originalMagicHit,BattleFinalBurn.Result finalBurn,boolean companion,boolean npc) {
        Attempt(int damage,int mr,int hit,BattleFinalBurn.Result burn,boolean companion){this(damage,mr,hit,burn,companion,false);}
        Attempt(int damage,int magicResistance,int originalMagicHit,BattleFinalBurn.Result finalBurn){this(damage,magicResistance,originalMagicHit,finalBurn,false);}
    }
    static Attempt attempt(int damage,int mr,int hit){return new Attempt(damage,mr,hit,null);}
    static int deferMr(int damage,int mr,int hit){return CombatRules.loaded()?damage:BattleMagicResistance.damageAfterResistance(damage,mr,hit);}
    static Attempt ordinary(ClientSession caster,int skill,int attribute,int source,int elemental,int mr,int armor,String description,ClientSession target) {
        if(CombatRules.loaded()){if(target!=null&&CombatPlayerMagic.protectedZero(target,System.nanoTime()))return new Attempt(0,-2,0,null);if(target==null)return CombatNpcMagic.ordinary(caster,skill,attribute,source,elemental,mr,armor,description,true);return (Attempt)CombatAccess.call(caster,"sourceMagicDamageBeforeResistance",PRE_MR,skill,attribute,source,elemental,CombatPlayerMagic.targetMr(mr,target),armor,description);}
        return attempt(CombatAccess.integerCall(caster,"ordinaryMagicDamage",ORDINARY,skill,attribute,source,elemental,mr,armor,description),-1,0);
    }
    static Attempt line(ClientSession caster,BattleSpellCatalog.DirectionalLineSpell spell,int source,int elemental,int mr,int armor,String description,ClientSession target) {
        return ordinary(caster,spell.skillId(),spell.attribute(),source,elemental,mr,armor,description,target);
    }
    static Attempt area(ClientSession caster,BattleSpellCatalog.AreaAttackSpell spell,int source,int elemental,int mr,int armor,String description,ClientSession target) {
        return ordinary(caster,spell.skillId(),spell.attribute(),source,elemental,mr,armor,description,target);
    }
    static Attempt finalBurn(int source,int armor,int offset,int mr,int hit,ClientSession target) {
        var result=BattleFinalBurn.resolve(source,armor,offset,mr,hit);
        if(!CombatRules.loaded()||target==null)return new Attempt(result.damage(),-1,hit,result);
        int raw=source-result.armorReduction();boolean protectedZero=CombatPlayerMagic.protectedZero(target,System.nanoTime());if(protectedZero)raw=0;
        var deferred=new BattleFinalBurn.Result(result.sourceMp(),result.armorReduction(),raw,raw,result.casterHpAfter(),result.casterMpAfter());
        return new Attempt(deferred.damage(),protectedZero?-2:CombatPlayerMagic.targetMr(mr,target),hit,deferred);
    }
    static int beforeCritical(BattleSpellCatalog.AttackSpell spell,int damage,boolean critical,ClientSession target) {
        return CombatRules.loaded()&&target!=null?damage:spell.damageAfterSpecialCritical(damage,critical);
    }
    static int finish(int damage,Attempt attempt,ClientSession caster,boolean critical) {
        if(!CombatRules.loaded()||attempt==null||attempt.magicResistance()<0)return damage;
        int type=caster.battleCharacterType();double factor=type<0||type>=KEYS.size()?1:CombatRules.number(KEYS.get(type),1);
        int scaled=(int)(damage*factor);if(critical)scaled*=2; // source int cast, then int *=2, then nonnegative guard
        return BattleMagicResistance.damageAfterResistance(Math.max(0,scaled),attempt.magicResistance(),attempt.originalMagicHit());
    }
    static int reduceMindBreak(int damage,ClientSession target) {
        return CombatRules.loaded()?cooking(target,damage,"source-mind-break"):damage;
    }
    static int cookingAfterMode(ClientSession target,int damage,String description,BattleSpellCatalog.MindBreakSpell mindBreak) {
        return CombatRules.loaded()&&mindBreak!=null?damage:cooking(target,damage,description);
    }
    private static int cooking(ClientSession target,int damage,String description) {
        return CombatPlayerMagic.reductions(target,damage,description);
    }
    /** Shock Stun and source doll procs also pass through calcPcMagicDamage. */
    static int direct(int damage,Attempt attempt,ClientSession caster,ClientSession target,boolean applyCooking) {
        if(!CombatRules.loaded()||target==null||attempt==null||attempt.magicResistance()<0)return damage;
        if(applyCooking) {
            damage=cooking(target,damage,"source-magic-doll");
            damage=CombatPlayerMagic.protect(target,damage,"source-magic-doll");
        }
        var decision=CombatPlayerMirror.decide(target,damage,true,ThreadLocalRandom.current().nextInt(100),System.nanoTime(),attempt);
        if(decision.triggered()) {CombatPlayerMirror.publish(caster,target,decision,applyCooking?"magic-doll":"shock-stun");return 0;}
        return finish(damage,attempt,caster,false);
    }
}
