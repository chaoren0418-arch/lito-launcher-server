package local.lito381.server;
import java.util.concurrent.ThreadLocalRandom;

/** Source skill72 PC calculation at the original native per-target boundaries. */
final class CombatCounterDetection {
    static CombatMagicDamage.Attempt prepare(ClientSession caster,int skill,int attribute,int source,int elemental,int mr,int armor,String description,ClientSession target,long now){
        var status=(BattleTimedStatus)CombatAccess.field(target,"timedStatus");
        if(CombatRules.loaded()&&(status.absoluteBarrierActive(now)||status.earthBindActive(now)||status.freezeActive(now)))return new CombatMagicDamage.Attempt(0,-2,0,null);
        return CombatMagicDamage.ordinary(caster,skill,attribute,source,elemental,mr,armor,description,target);
    }
    static int finish(int damage,CombatMagicDamage.Attempt attempt,ClientSession caster,ClientSession target,long now){return resolve(damage,attempt,caster,target,now,-1);}
    /** Fixed-roll mechanics fixture; production always draws its own source zero-based percentile. */
    static int resolve(int damage,CombatMagicDamage.Attempt attempt,ClientSession caster,ClientSession target,long now,int fixture){
        if(fixture < -1 || fixture>99)throw new IllegalArgumentException("invalid Counter Detection mirror fixture");
        if(CombatRules.loaded()&&attempt!=null&&attempt.magicResistance()==-2)return 0;
        if(!CombatRules.loaded()||attempt==null||attempt.magicResistance()<0)return damage;
        var status=(BattleCounterMirrorStatus)CombatAccess.field(target,"counterMirrorStatus");
        if(status.active(now)){
            int roll=fixture<0?ThreadLocalRandom.current().nextInt(100):fixture;
            var decision=CombatPlayerMirror.decide(target,damage,true,roll,now,attempt);
            if(decision.triggered()){CombatPlayerMirror.publish(caster,target,decision,damage==0?"counter-detection-source-zero":"counter-detection-source");return 0;}
        }
        return CombatMagicDamage.finish(damage,attempt,caster,false);
    }
}
