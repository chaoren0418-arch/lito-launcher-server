package local.lito381.server;

import java.math.BigInteger;
import java.util.List;
import java.util.Objects;
import static local.lito381.server.ModernEquipmentFacts.Stat.*;

/**
 * NEW_MATCH_NATURAL_REGEN_V1 selects the retained LiTo BattleNaturalHpRegen /
 * BattleNaturalMpRegen counters, not the separately modified classic executor.
 * New matches import no cooking, Life Stream, blue potion, meditation, doll,
 * concentration, barrier or weight-bypass skills. Food is the frozen source
 * Food(0..225), not the rounded UI percentage. No source HP/MP is written.
 *
 * All clocks are ACTIVE-relative: signed System.nanoTime origins never enter
 * this pure model. A plan is immutable and must be committed only after the
 * engine rechecks the ticket, equipment lifetime and phase after its RNG call.
 */
final class ModernNaturalRegen {
    static final String RULESET="NEW_MATCH_NATURAL_REGEN_V1";
    static final long SECOND_NANOS=1_000_000_000L;
    static final long MAX_ELAPSED_NANOS=1_800L*SECOND_NANOS;
    enum Activity { ATTACK(1), MOVE(2), NONE(4); final int increment;Activity(int value){increment=value;} }
    enum UnsupportedReason { NONE, CUSTOM_EXECUTOR, EQUIPMENT_PROC, LIFETIME_UNQUALIFIED }
    record ConRow(long id,long maximum) {
        ConRow { if(id<1||id>50||maximum<1)throw invalid(); }
    }
    record Growth(int sourceId,String database,List<ConRow> constitution) {
        Growth {
            if(sourceId<1)throw invalid();ModernPhysicalCombat.identifier(database);
            constitution=List.copyOf(constitution);if(constitution.size()!=50)throw invalid();
            for(int n=0;n<50;n++)if(constitution.get(n).id()!=n+1)throw invalid();
        }
        long maximum(long baseConstitution) {
            if(baseConstitution<0)throw invalid();
            return constitution.get((int)Math.max(1,Math.min(50,baseConstitution))-1).maximum();
        }
        @Override public String toString(){return "ModernRegenGrowth[redacted]";}
    }
    record Values(long level,int type,long constitution,long originalConstitution,long wisdom,long originalWisdom,
            long food,long constitutionHprMaximum,long equipmentHpRegen,long equipmentMpRegen,long maximumHp,long maximumMp) {
        Values {
            if(level<1||type<0||type>6||constitution<0||originalConstitution<0||wisdom<0||originalWisdom<0
                    ||food<0||food>225||constitutionHprMaximum<1||maximumHp<1||maximumMp<0)throw invalid();
            // Reject an unrepresentable signed source amount before admission.
            Math.addExact(Math.addExact(constitutionHprMaximum,originalHpRegen(type,originalConstitution)),equipmentHpRegen);
            Math.addExact(Math.addExact(1,originalHpRegen(type,originalConstitution)),equipmentHpRegen);
            Math.addExact(Math.addExact(wisdomBase(wisdom),originalMpRegen(type,originalWisdom)),equipmentMpRegen);
        }
        boolean fed(){return food>=3;}
    }
    record Profile(ModernCombatProfile combat,Values values,UnsupportedReason unsupportedReason) {
        Profile { Objects.requireNonNull(combat);Objects.requireNonNull(values);Objects.requireNonNull(unsupportedReason); }
        boolean supported(){return unsupportedReason==UnsupportedReason.NONE;}
        @Override public String toString(){return "ModernNaturalRegenProfile[redacted]";}
    }
    static Profile qualify(ModernPhysicalCombat.Source source,ModernCombatProfile combat,Growth growth) {
        return qualify(source,combat,growth,null);
    }
    static Profile qualify(ModernPhysicalCombat.Source source,ModernCombatProfile combat,Growth growth,
            ModernArmorProtectionLoadout protection) {
        return qualify(source,combat,growth,protection,null);
    }
    static Profile qualify(ModernPhysicalCombat.Source source,ModernCombatProfile combat,Growth growth,
            ModernArmorProtectionLoadout protection,ModernVipCombatProfile vip) {
        if(vip!=null)vip.require(source,combat);
        if((combat.armorPolicy()==ModernCombatProfile.ArmorPolicy.REBUILT_CHARACTER_EQUIPMENT_VIP)!=(vip!=null))throw invalid();
        Objects.requireNonNull(source);Objects.requireNonNull(combat);Objects.requireNonNull(growth);
        if(protection!=null)protection.require(source,combat);
        var equipment=source.input().equipment();
        if(!equipment.source().equals(combat.source())||growth.sourceId()!=combat.source().sourceId()
                ||!growth.database().equals(equipment.catalog().resourceDatabase()))throw invalid();
        var saved=combat.source().persisted();var row=combat.source().character();var effective=combat.effective();
        long maximum=effective.constitution()==saved.constitution()?growth.maximum(saved.constitution()):fallbackConHprMaximum(effective.constitution());
        var values=new Values(row.level(),row.characterClass(),effective.constitution(),saved.originalConstitution(),
                effective.wisdom(),saved.originalWisdom(),saved.food225(),maximum,combat.equipmentEvaluation().total(HP_REGEN),
                combat.equipmentEvaluation().total(MP_REGEN),effective.maximumHp(),effective.maximumMp());
        UnsupportedReason unsupported=UnsupportedReason.NONE;
        for(var flag:combat.equipped()) {
            var item=equipment.items().get(flag.objectId()).source();
            if(!flag.equipped()&&!item.equipped())continue;
            var life=equipment.lifetimes().view(item.objectId(),combat.evaluatedAtMillis());
            if(!life.usable()&&life.state()!=ModernItemLifetime.State.EXPIRED)unsupported=UnsupportedReason.LIFETIME_UNQUALIFIED;
            if((vip==null||!vip.permits(item,flag.equipped()))&&ModernVipEquipment.unresolvedCustom(source,item,flag.equipped()))unsupported=UnsupportedReason.CUSTOM_EXECUTOR;
            if(source.procCatalogIds().contains(item.template().catalogId())&&(protection==null||!protection.permits(item.objectId())))unsupported=UnsupportedReason.EQUIPMENT_PROC;
        }
        return new Profile(combat,values,unsupported);
    }
    record Clock(long nextSecondElapsedNanos,int hpCounter,int mpCounter,Activity hpActivity,Activity mpActivity) {
        Clock {
            if(nextSecondElapsedNanos<SECOND_NANOS||nextSecondElapsedNanos>MAX_ELAPSED_NANOS+SECOND_NANOS
                    ||nextSecondElapsedNanos%SECOND_NANOS!=0||hpCounter<0||hpCounter>=120||mpCounter<0||mpCounter>=64)throw invalid();
            Objects.requireNonNull(hpActivity);Objects.requireNonNull(mpActivity);
        }
        static Clock initial(){return new Clock(SECOND_NANOS,0,0,Activity.NONE,Activity.NONE);}
        Clock noteMove(){return note(Activity.MOVE);}
        Clock noteAttack(){return note(Activity.ATTACK);}
        private Clock note(Activity activity){return new Clock(nextSecondElapsedNanos,hpCounter,mpCounter,activity,activity);}
    }
    record Due(Clock clock,long hpEvents,long mpEvents) {
        Due { Objects.requireNonNull(clock);if(hpEvents<0||hpEvents>900||mpEvents<0||mpEvents>180)throw invalid(); }
    }
    private record Counter(int value,long due,Activity activity) {}
    static Due poll(Values values,Clock clock,long elapsedNanos,boolean hpEligible,boolean mpEligible) {
        Objects.requireNonNull(values);Objects.requireNonNull(clock);
        if(elapsedNanos<0||elapsedNanos>MAX_ELAPSED_NANOS||clock.hpCounter()>=hpThreshold(values.level(),values.type())
                ||clock.mpCounter()>=mpThreshold(values.type()))throw invalid();
        if(elapsedNanos<clock.nextSecondElapsedNanos())return new Due(clock,0,0);
        long seconds=(elapsedNanos-clock.nextSecondElapsedNanos())/SECOND_NANOS+1;
        var hp=counter(clock.hpCounter(),clock.hpActivity(),hpThreshold(values.level(),values.type()),seconds,hpEligible);
        var mp=counter(clock.mpCounter(),clock.mpActivity(),mpThreshold(values.type()),seconds,mpEligible);
        return new Due(new Clock(Math.addExact(clock.nextSecondElapsedNanos(),Math.multiplyExact(seconds,SECOND_NANOS)),
                hp.value(),mp.value(),hp.activity(),mp.activity()),hp.due(),mp.due());
    }
    private static Counter counter(int value,Activity activity,int threshold,long seconds,boolean eligible) {
        if(!eligible)return new Counter(value,0,activity);
        int first=value+activity.increment;long due=first>=threshold?1:0;int current=first>=threshold?0:first;
        long rest=seconds-1;
        long needed=(threshold-current+3L)/4;
        if(rest<needed)return new Counter(Math.toIntExact(current+4*rest),due,Activity.NONE);
        rest-=needed;due=Math.addExact(due,1);long period=threshold/4;
        due=Math.addExact(due,rest/period);current=Math.toIntExact((rest%period)*4);
        return new Counter(current,due,Activity.NONE);
    }
    /** Last before/after is the last NONZERO application, not the net batch delta. */
    record Outcome(Clock clock,long hp,long mp,long hpGain,long hpLoss,long hpBefore,long hpAfter,
            long mpGain,long mpLoss,long mpBefore,long mpAfter) {
        Outcome {
            Objects.requireNonNull(clock);if(hp<1||mp<0||hpGain<0||hpLoss<0||mpGain<0||mpLoss<0)throw invalid();
            if((hpGain==0&&hpLoss==0)!=(hpBefore==0&&hpAfter==0)||(mpGain==0&&mpLoss==0)!=(mpBefore==0&&mpAfter==0))throw invalid();
        }
        boolean changed(){return hpGain!=0||hpLoss!=0||mpGain!=0||mpLoss!=0;}
    }
    static Outcome resolve(Values values,Due due,long hp,long mp,ModernPhysicalCombat.BoundedRandom random) {
        Objects.requireNonNull(values);Objects.requireNonNull(due);Objects.requireNonNull(random);
        if(hp<1||hp>values.maximumHp()||mp<0||mp>values.maximumMp())throw invalid();
        long hpGain=0,hpLoss=0,hpBefore=0,hpAfter=0;
        for(long event=0;event<due.hpEvents()&&hp<values.maximumHp();event++) {
            long draw=random.nextLong(values.constitutionHprMaximum());
            if(draw<0||draw>=values.constitutionHprMaximum())throw invalid();
            long amount=hpAmount(values,Math.incrementExact(draw));long before=hp;
            hp=clamp(BigInteger.valueOf(hp).add(BigInteger.valueOf(amount)),1,values.maximumHp());
            if(hp!=before){hpBefore=before;hpAfter=hp;if(hp>before)hpGain=Math.addExact(hpGain,hp-before);else hpLoss=Math.addExact(hpLoss,before-hp);}
        }
        long mpGain=0,mpLoss=0,mpBefore=0,mpAfter=0,amount=mpAmount(values);
        // At most 180 bounded events. Preserve the last nonzero individual
        // tick even when several events catch up or the final event saturates.
        for(long event=0;event<due.mpEvents();event++) {
            long before=mp;mp=clamp(BigInteger.valueOf(mp).add(BigInteger.valueOf(amount)),0,values.maximumMp());
            if(mp!=before){mpBefore=before;mpAfter=mp;if(mp>before)mpGain=Math.addExact(mpGain,mp-before);else mpLoss=Math.addExact(mpLoss,before-mp);}
        }
        return new Outcome(due.clock(),hp,mp,hpGain,hpLoss,hpBefore,hpAfter,mpGain,mpLoss,mpBefore,mpAfter);
    }
    private static long clamp(BigInteger value,long min,long max){return value.max(BigInteger.valueOf(min)).min(BigInteger.valueOf(max)).longValueExact();}
    static long hpAmount(Values values,long baseRoll) {
        if(baseRoll<1||baseRoll>values.constitutionHprMaximum())throw invalid();
        return Math.addExact(Math.addExact(baseRoll,originalHpRegen(values.type(),values.originalConstitution())),values.equipmentHpRegen());
    }
    static long mpAmount(Values values){return Math.addExact(Math.addExact(wisdomBase(values.wisdom()),originalMpRegen(values.type(),values.originalWisdom())),values.equipmentMpRegen());}
    static int hpThreshold(long level,int type){valid(type,level);if(level<1)throw invalid();int index=(int)Math.min(10,level);if(level>=30&&type==1)index=11;return new int[]{30,25,20,16,14,12,11,10,9,3,2}[index-1]*4;}
    static int mpThreshold(int type){valid(type,0);return type==3?40:64;}
    static long fallbackConHprMaximum(long constitution){if(constitution<0)throw invalid();long con=Math.max(1,Math.min(50,constitution));return con<=15?1:con==16?2:con==17?3:Math.max(1,(con-12)/2);}
    static long wisdomBase(long wisdom){if(wisdom<0)throw invalid();return wisdom<=14?1:wisdom<=16?2:3;}
    static long originalHpRegen(int type,long con){valid(type,con);return switch(type){
        case 0->con==13||con==14?1:con==15||con==16?2:con==17?3:con==18?4:0;
        case 1->con==16||con==17?2:con==18?4:0;case 2->con==14||con==15?1:con==16?2:con>=17?3:0;
        case 3->con==17?1:con==18?2:0;case 4->con==11||con==12?1:con>=13?2:0;
        case 5->con==16||con==17?1:con==18?3:0;default->con==14||con==15?1:con>=16?2:0;};}
    static long originalMpRegen(int type,long wis){valid(type,wis);return switch(type){
        case 0->wis==13||wis==14?1:wis>=15?2:0;case 1->wis==11||wis==12?1:wis==13?2:0;
        case 2->wis>=15&&wis<=17?1:wis==18?2:0;case 3->wis==14||wis==15?1:wis==16||wis==17?2:wis==18?3:0;
        case 4->wis>=13?1:0;case 5->wis==15||wis==16?1:wis>=17?2:0;default->wis>=14&&wis<=16?1:wis>=17?2:0;};}
    private static void valid(int type,long value){if(type<0||type>6||value<0)throw invalid();}
    private static IllegalArgumentException invalid(){return new IllegalArgumentException("invalid qualified modern natural regen");}
    private ModernNaturalRegen(){}
}
