package local.lito381.server;

import static org.junit.jupiter.api.Assertions.*;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class ModernHonorHealingTest {
    @Test void qualifiedTwoDiceHealAndLawfulMultiplierRetainNativeTruncation(){
        var low=ModernHonorHealing.resolve(new ModernHonorHealing.Profile(true,2,4,0),10,100,b->0);assertEquals(2,low.amount());assertEquals(12,low.remainingHp());
        var high=ModernHonorHealing.resolve(new ModernHonorHealing.Profile(true,2,4,32767),10,100,b->3);assertEquals(15,high.amount());assertEquals(25,high.remainingHp());
        assertEquals(8,ModernHonorHealing.resolve(new ModernHonorHealing.Profile(true,2,4,-32768),10,100,b->3).amount());
    }
    @Test void capAndFullHealthStillRollSourceDiceWithoutOverhealing(){
        var draws=new AtomicInteger();var profile=new ModernHonorHealing.Profile(true,2,0,0);
        var result=ModernHonorHealing.resolve(profile,99,100,b->{draws.incrementAndGet();return 3;});assertEquals(8,result.amount());assertEquals(1,result.restored());assertEquals(2,draws.get());
        assertEquals(0,ModernHonorHealing.resolve(profile,100,100,b->{draws.incrementAndGet();return 3;}).restored());assertEquals(4,draws.get());
    }
    @Test void negativeSourceDiceBonusCanProduceZeroButMissingOrUnboundedEvidenceNeverBecomesFixedHealing(){
        var row=new ModernApplicationProtocol.CharacterRow(3333,"fixture",1,0,80);var source=ModernBattleEngineHonorTest.source(7,row,100,100);
        var fighter=ModernPhysicalCombatTest.fighter(source);assertTrue(fighter.healing().supported());assertEquals(2,fighter.healing().diceCount());assertEquals(4,fighter.healing().mpCost());
        for(long bonus:new long[]{-3,4095,Long.MAX_VALUE}){
            var modified=new ModernPhysicalCombat.Source(source.input(),source.growth(),source.templates(),source.procCatalogIds(),source.magicGrowth(),source.magicBows(),source.weaponArrowSprites(),source.regenGrowth(),ModernBattleEngineHonorTest.healing(7,"catalog",bonus));
            var h=ModernPhysicalCombatTest.fighter(modified).healing();if(bonus==-3){assertTrue(h.supported());assertEquals(0,h.diceCount());assertEquals(0,ModernHonorHealing.resolve(h,1,100,b->{throw new AssertionError();}).amount());}
            else assertFalse(h.supported());
        }
        var legacy=new ModernPhysicalCombat.Source(source.input(),source.growth(),source.templates(),source.procCatalogIds(),source.magicGrowth(),source.magicBows(),source.weaponArrowSprites(),source.regenGrowth());
        assertNull(ModernPhysicalCombatTest.fighter(legacy).healing());
    }
    @Test void full127SameSourceGrowthAndExactRowOrderAreRequired(){
        var g=ModernBattleEngineHonorTest.healing(7,"catalog",0);var rows=new ArrayList<>(g.rows());rows.remove(126);assertThrows(IllegalArgumentException.class,()->new ModernHonorHealing.Growth(7,"catalog",rows));
        rows.add(new ModernHonorHealing.Row(126,0));assertThrows(IllegalArgumentException.class,()->new ModernHonorHealing.Growth(7,"catalog",rows));
        var s=ModernBattleEngineHonorTest.source(7,new ModernApplicationProtocol.CharacterRow(3333,"fixture",1,0,80),100,100);
        assertThrows(IllegalArgumentException.class,()->new ModernPhysicalCombat.Source(s.input(),s.growth(),s.templates(),s.procCatalogIds(),s.magicGrowth(),s.magicBows(),s.weaponArrowSprites(),s.regenGrowth(),ModernBattleEngineHonorTest.healing(42,"catalog",0)));
    }
    @Test void invalidRandomAndInvalidLifeNeverReturnAnOutcome(){
        var p=new ModernHonorHealing.Profile(true,2,4,0);for(long bad:new long[]{-1,4,Long.MAX_VALUE})assertThrows(IllegalArgumentException.class,()->ModernHonorHealing.resolve(p,1,100,b->bad));
        assertThrows(IllegalArgumentException.class,()->ModernHonorHealing.resolve(p,0,100,b->0));assertThrows(IllegalArgumentException.class,()->ModernHonorHealing.resolve(ModernHonorHealing.Profile.unavailable(),1,100,b->0));
        assertThrows(IllegalArgumentException.class,()->new ModernHonorHealing.Profile(true,4097,4,0));assertThrows(IllegalArgumentException.class,()->new ModernHonorHealing.Profile(true,2,4,32768));
    }
}
