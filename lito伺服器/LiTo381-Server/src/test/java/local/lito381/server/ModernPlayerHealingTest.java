package local.lito381.server;

import static org.junit.jupiter.api.Assertions.*;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class ModernPlayerHealingTest {
    static ModernPlayerHealing.Profile profile() {
        return ModernPlayerHealing.qualify(ModernPhysicalCombatTest.fighter(ModernBattleEngineHonorTest.source(
                7,new ModernApplicationProtocol.CharacterRow(3333,"fixture",1,0,80),100,100)));
    }
    static ModernSkills.Snapshot skills(ModernPlayerHealing.Profile profile) {
        return new ModernSkills.Snapshot(profile.key().sourceId(),profile.character(),
                List.of(new ModernSkills.Skill(1,"$heal",4,10,ModernSkills.Support.BASIC_HEAL)));
    }
    @Test void usesTheQualifiedNativeBasicHealNotPotionOrOtherHealingSkill() {
        var nativeSpell=BattleSpellCatalog.healingSpell(1);
        assertEquals(nativeSpell.skillId(),ModernPlayerHealing.SKILL_ID);
        assertEquals(nativeSpell.actionId(),ModernPlayerHealing.ACTION);
        assertEquals(nativeSpell.effectGfx(),ModernPlayerHealing.EFFECT);
        assertEquals(nativeSpell.reuseDelayMillis(),ModernPlayerHealing.REUSE_MILLIS);
        var p=profile();assertEquals(4,p.mpCost());assertEquals(2,p.calculation().diceCount());
        assertEquals(100,p.maximumHp());assertEquals(100,p.maximumMp());assertTrue(p.accepts(skills(p)));
    }
    @Test void missingGrowthCannotBecomeDefaultHealAndSourceQualifiedLearnedDataMustMatch() {
        var p=profile();var source=ModernBattleEngineHonorTest.source(7,p.character(),100,100);
        var missing=new ModernPhysicalCombat.Source(source.input(),source.growth(),source.templates(),source.procCatalogIds(),
                source.magicGrowth(),source.magicBows(),source.weaponArrowSprites(),source.regenGrowth());
        assertThrows(IllegalArgumentException.class,()->ModernPlayerHealing.qualify(ModernPhysicalCombatTest.fighter(missing)));
        assertFalse(p.accepts(new ModernSkills.Snapshot(42,p.character(),skills(p).skills())));
        assertFalse(p.accepts(new ModernSkills.Snapshot(7,p.character(),List.of())));
        assertFalse(p.accepts(new ModernSkills.Snapshot(7,p.character(),List.of(new ModernSkills.Skill(1,"$heal",4,10,ModernSkills.Support.UNIMPLEMENTED)))));
        assertFalse(p.accepts(new ModernSkills.Snapshot(7,new ModernApplicationProtocol.CharacterRow(3333,"changed",1,0,80),skills(p).skills())));
        assertFalse(p.accepts(null));assertFalse(p.toString().contains("3333"));
    }
    @Test void liveHealingHelmChangesCostButNotTheOldProfileOrSourceInventory() {
        var source=ModernHasteTest.source(7,1,1L<<40,20014);
        source=new ModernPhysicalCombat.Source(source.input(),source.growth(),source.templates(),source.procCatalogIds(),
                source.magicGrowth(),source.magicBows(),source.weaponArrowSprites(),source.regenGrowth(),
                ModernBattleEngineHonorTest.healing(7,source.growth().database(),0));
        var before=ModernPhysicalCombatTest.fighter(source);var p=ModernPlayerHealing.qualify(before);var q=source.input().equipment();
        assertEquals(2,p.mpCost());
        var changed=ModernPhysicalCombat.qualifyEquipmentChange(source,ModernCombatProfileTest.binding(q),
                ModernCombatProfileTest.stored(q,1,Map.of(1L,false)),1,before,23,7);
        var next=ModernPlayerHealing.qualify(changed);
        assertEquals(4,next.mpCost());assertEquals(1,next.equipmentRevision());assertEquals(p,ModernPlayerHealing.qualify(before));
        assertTrue(q.source().inventory().get(0).equipped());assertEquals(23,changed.profile().effective().initialHp());
    }
    @Test void allModifierCombinationsMatchNativeIncludingNegativeDamageRoundingOrder() {
        for(int flags=0;flags<16;flags++) for(int value=0;value<=257;value++) {
            var e=new ModernPlayerHealing.Effects((flags&1)!=0,(flags&2)!=0,(flags&4)!=0,(flags&8)!=0);
            assertEquals(BattleHealingRecovery.adjustedSingleTargetAmount(value,e.waterLife(),e.polluteWater(),e.dirtyCurrent(),e.healingErosion()),
                    ModernPlayerHealing.adjustedAmount(value,e));
        }
        assertEquals(2,ModernPlayerHealing.adjustedAmount(3,new ModernPlayerHealing.Effects(false,true,false,false)));
        assertEquals(1,ModernPlayerHealing.adjustedAmount(3,new ModernPlayerHealing.Effects(false,true,true,false)));
        assertEquals(2,ModernPlayerHealing.adjustedAmount(3,new ModernPlayerHealing.Effects(true,true,true,false)));
        assertThrows(ArithmeticException.class,()->ModernPlayerHealing.adjustedAmount(Long.MAX_VALUE,new ModernPlayerHealing.Effects(true,false,false,false)));
    }
    @Test void modifiersRunBeforeHpCapAndWaterLifeIsConsumedEvenAtFullHpOrUnderErosion() {
        var p=profile();var water=new ModernPlayerHealing.Effects(true,false,false,false);
        var a=ModernPlayerHealing.resolve(p,0,99,100,water,b->3);
        assertEquals(8,a.baseAmount());assertEquals(16,a.adjustedAmount());assertEquals(1,a.restored());assertFalse(a.after().waterLife());
        var full=ModernPlayerHealing.resolve(p,0,100,100,water,b->3);assertEquals(0,full.restored());assertFalse(full.after().waterLife());
        var erosion=new ModernPlayerHealing.Effects(true,true,false,true);
        var blocked=ModernPlayerHealing.resolve(p,0,1,100,erosion,b->3);
        assertEquals(-8,blocked.adjustedAmount());assertEquals(0,blocked.restored());assertEquals(1,blocked.remainingHp());
        assertEquals(new ModernPlayerHealing.Effects(false,true,false,true),blocked.after());
    }
    @Test void wideHealthPreservesPrecisionAndBadRandomOrDeadTargetsHaveNoOutcome() {
        var p=profile();var count=new AtomicInteger();long maximum=Long.MAX_VALUE;
        var result=ModernPlayerHealing.resolve(p,0,maximum-1,maximum,ModernPlayerHealing.Effects.NONE,b->{count.incrementAndGet();return 3;});
        assertEquals(maximum,result.remainingHp());assertEquals(1,result.restored());assertEquals(2,count.get());
        for(long hp:new long[]{-1,0,101})assertThrows(IllegalArgumentException.class,
                ()->ModernPlayerHealing.resolve(p,0,hp,100,ModernPlayerHealing.Effects.NONE,b->{throw new AssertionError("invalid target used RNG");}));
        for(long roll:new long[]{-1,4,Long.MAX_VALUE})assertThrows(IllegalArgumentException.class,
                ()->ModernPlayerHealing.resolve(p,0,1,100,ModernPlayerHealing.Effects.NONE,b->roll));
    }
    @Test void currentLawfulIsNotReplacedByTheImportedCharacterValue() {
        var p=profile();assertEquals(0,p.calculation().lawful());
        assertEquals(15,ModernPlayerHealing.resolve(p,32767,1,100,ModernPlayerHealing.Effects.NONE,b->3).baseAmount());
        assertEquals(8,ModernPlayerHealing.resolve(p,-32768,1,100,ModernPlayerHealing.Effects.NONE,b->3).baseAmount());
        assertThrows(IllegalArgumentException.class,()->ModernPlayerHealing.resolve(p,32768,1,100,
                ModernPlayerHealing.Effects.NONE,b->{throw new AssertionError("invalid lawful used RNG");}));
    }
}
