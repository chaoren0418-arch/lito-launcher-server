package local.lito381.server;

import java.math.BigInteger;
import java.util.List;
import java.util.Objects;

/** Qualified skill 1 only. No active status import; no tower identity is synthesized as a PC. */
final class ModernHonorHealing {
    static final int MAX_DICE=4096;
    record Row(long id,long healingBonus){Row{if(id<1||id>127)throw bad();}}
    record Growth(int sourceId,String database,List<Row> rows){
        Growth{ModernPhysicalCombat.identifier(database);rows=List.copyOf(rows);if(sourceId<1||rows.size()!=127)throw bad();for(int i=0;i<127;i++)if(rows.get(i).id()!=i+1)throw bad();}
    }
    record Profile(boolean supported,long diceCount,int mpCost,long lawful){
        Profile{if(diceCount<0||diceCount>MAX_DICE||mpCost<0||mpCost>4||lawful< -32768||lawful>32767)throw bad();}
        static Profile unavailable(){return new Profile(false,0,4,0);}
    }
    record Outcome(long amount,long restored,long remainingHp){Outcome{if(amount<0||restored<0||restored>amount||remainingHp<1)throw bad();}}
    static Profile qualify(ModernPhysicalCombat.Source source,ModernCombatProfile profile,ModernMagicCombat.Fighter magic){
        if(source.healingGrowth()==null)return null;
        if(!source.input().equipment().source().equals(profile.source()))throw bad();
        var p=profile.source().persisted();long intelligence=profile.effective().intelligence(),lawful=p.lawful();
        if(magic==null||!magic.supported()||intelligence<1||lawful< -32768||lawful>32767)return Profile.unavailable();
        long bonus=source.healingGrowth().rows().get((int)Math.min(127,intelligence)-1).healingBonus();
        long count;
        try{count=Math.max(0,Math.addExact(2,bonus));}catch(ArithmeticException failure){return Profile.unavailable();}
        if(count>MAX_DICE)return Profile.unavailable();
        boolean helm=false;
        for(var equipped:profile.equipped())if(equipped.equipped()) {
            var item=source.input().equipment().items().get(equipped.objectId()).source();
            if(item.template().kind()==ModernCharacterData.ItemKind.ARMOR&&item.template().catalogId()==20014)helm=true;
        }
        int reduction=Math.toIntExact(ModernMagicCombat.originalMpReduction(profile.source().character().characterClass(),p.originalIntelligence()));
        return new Profile(true,count,Math.max(0,(helm?2:4)-reduction),lawful);
    }
    static Outcome resolve(Profile profile,long hp,long maxHp,ModernPhysicalCombat.BoundedRandom random){
        Objects.requireNonNull(profile);Objects.requireNonNull(random);
        if(!profile.supported()||hp<1||hp>maxHp)throw bad();
        long rolled=0;
        for(int i=0;i<profile.diceCount();i++){long value=random.nextLong(4);if(value<0||value>=4)throw bad();rolled=Math.addExact(rolled,value+1);}
        // Lawful is /32768 binary-exact. Integer arithmetic preserves native truncation without narrowing i64.
        long amount=BigInteger.valueOf(rolled).multiply(BigInteger.valueOf(32768+Math.max(0,profile.lawful()))).divide(BigInteger.valueOf(32768)).longValueExact();
        long restored=Math.min(amount,Math.subtractExact(maxHp,hp));
        return new Outcome(amount,restored,Math.addExact(hp,restored));
    }
    private static IllegalArgumentException bad(){return new IllegalArgumentException("invalid qualified honor healing");}
    private ModernHonorHealing(){}
}
