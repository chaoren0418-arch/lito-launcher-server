package local.lito381.server;

import static org.junit.jupiter.api.Assertions.*;
import static local.lito381.server.ModernEquipmentFacts.*;
import static local.lito381.server.ModernVipEquipment.Field.*;
import java.util.*;
import org.junit.jupiter.api.Test;

class ModernVipCombatProfileTest {
    @Test void completeStateFeedsEveryNumericConsumerWithoutConvertingReductionIntoDamage() {
        var source=source(Map.of(STR,5L,INT,7L,HPR,13L,MPR,17L,DAMAGE_REDUCTION,23L,MAGIC_REDUCTION,29L,EXP_RATE,100L,ADENA_RATE,150L),false);
        var state=ModernVipCharacterStateTest.start(source,0);var fighter=ModernPhysicalCombat.qualifyVip(state,1000);
        assertTrue(fighter.supported());assertTrue(fighter.magic().supported());assertTrue(fighter.ranged().defenseSupported());assertTrue(fighter.regen().supported());
        assertTrue(fighter.healing().supported());assertSame(state.profile().character(),fighter.profile());
        assertSame(fighter.profile(),fighter.magic().profile());assertSame(fighter.profile(),fighter.ranged().profile());assertSame(fighter.profile(),fighter.regen().combat());
        assertEquals(23,fighter.defense().armorReduction());assertEquals(23,fighter.ranged().values().defense().armorReduction());
        assertEquals(23,fighter.magic().defense().armorReduction());assertEquals(29,fighter.magic().defense().magicOnlyReduction());
        assertEquals(0,fighter.offense().itemDamage());assertEquals(0,fighter.offense().templateDamage());
        assertEquals(13,fighter.regen().values().equipmentHpRegen());assertEquals(17,fighter.regen().values().equipmentMpRegen());
        assertEquals(100,fighter.vip().experienceBonusPercent());assertEquals(150,fighter.vip().adenaBonusPercent());
        assertEquals(ModernCombatProfile.ArmorPolicy.REBUILT_CHARACTER_EQUIPMENT_VIP,fighter.profile().armorPolicy());
    }
    @Test void oldRoutesStillRejectActiveVipAndCompleteProfileCannotLoseItsProof() {
        var source=source(Map.of(DAMAGE_REDUCTION,20L,MAGIC_REDUCTION,7L),false);var q=source.input().equipment();
        var old=ModernPhysicalCombat.qualify(source,ModernCombatProfileTest.binding(q),Optional.empty(),1000);
        assertFalse(old.supported());assertFalse(old.magic().supported());assertFalse(old.ranged().defenseSupported());assertFalse(old.regen().supported());
        var state=ModernVipCharacterStateTest.start(source,0);var fighter=ModernPhysicalCombat.qualifyVip(state,1000);var p=state.profile().character();
        assertThrows(IllegalArgumentException.class,()->ModernMagicCombat.qualify(source,p));
        assertThrows(IllegalArgumentException.class,()->ModernRangedCombat.qualify(source,p,fighter.defense(),1000));
        assertThrows(IllegalArgumentException.class,()->ModernNaturalRegen.qualify(source,p,source.regenGrowth()));
        assertThrows(IllegalArgumentException.class,()->ModernMagicCombat.qualify(source,old.profile(),null,fighter.vip()));
        var clone=new ModernPhysicalCombat.Source(source.input(),source.growth(),source.templates(),source.procCatalogIds(),source.magicGrowth(),
                source.magicBows(),source.weaponArrowSprites(),source.regenGrowth(),source.healingGrowth(),source.vipCatalog());
        assertThrows(IllegalArgumentException.class,()->ModernMagicCombat.qualify(clone,p,null,fighter.vip()));
        assertThrows(IllegalArgumentException.class,()->ModernRangedCombat.qualify(clone,p,fighter.defense(),1000,null,fighter.vip()));
        assertThrows(IllegalArgumentException.class,()->ModernNaturalRegen.qualify(clone,p,clone.regenGrowth(),null,fighter.vip()));
    }
    @Test void unknownCustomAndProcSiblingsRemainRejectedWithCompleteVip() {
        var source=source(Map.of(DAMAGE_REDUCTION,20L),true);var policies=new HashMap<>(source.templates());
        var weapon=policies.get(1L);policies.put(1L,new ModernPhysicalCombat.TemplatePolicy(weapon.kind(),"unknown.Executor",weapon.criticalMultiplier()));
        var custom=new ModernPhysicalCombat.Source(source.input(),source.growth(),policies,source.procCatalogIds(),source.magicGrowth(),
                source.magicBows(),source.weaponArrowSprites(),source.regenGrowth(),source.healingGrowth(),source.vipCatalog());
        var bad=ModernPhysicalCombat.qualifyVip(ModernVipCharacterStateTest.start(custom,0),1000);
        assertFalse(bad.magic().supported());assertFalse(bad.ranged().supported());assertFalse(bad.ranged().defenseSupported());assertFalse(bad.regen().supported());
        var proc=new ModernPhysicalCombat.Source(source.input(),source.growth(),source.templates(),Set.of(300086L),source.magicGrowth(),
                source.magicBows(),source.weaponArrowSprites(),source.regenGrowth(),source.healingGrowth(),source.vipCatalog());
        var blocked=ModernPhysicalCombat.qualifyVip(ModernVipCharacterStateTest.start(proc,0),1000);
        assertFalse(blocked.magic().supported());assertFalse(blocked.ranged().defenseSupported());assertFalse(blocked.regen().supported());
    }
    @Test void meleeAndBowConsumeGenericButNeverMagicOnlyReduction() {
        var plain=ModernPhysicalCombat.qualifyVip(ModernVipCharacterStateTest.start(source(Map.of(),false),0),1000);
        var generic=ModernPhysicalCombat.qualifyVip(ModernVipCharacterStateTest.start(source(Map.of(DAMAGE_REDUCTION,20L),false),0),1000);
        var magic=ModernPhysicalCombat.qualifyVip(ModernVipCharacterStateTest.start(source(Map.of(MAGIC_REDUCTION,1000L),false),0),1000);
        var attacker=ModernPhysicalCombatTest.createFighter(42,new ModernApplicationProtocol.CharacterRow(999,"opponent",0,0,300),1000,10,1);
        var normal=ModernPhysicalCombat.resolveValues(attacker.values(),plain.values(),100,b->b-1);
        var reduced=ModernPhysicalCombat.resolveValues(attacker.values(),generic.values(),100,b->b-1);
        assertTrue(normal.hit());assertEquals(Math.max(0,normal.appliedDamage()-20),reduced.appliedDamage());
        assertEquals(normal,ModernPhysicalCombat.resolveValues(attacker.values(),magic.values(),100,b->b-1));
        var bow=ModernPhysicalCombat.qualifyVip(ModernVipCharacterStateTest.start(source(Map.of(RANGED_DAMAGE,200L),true),0),1000);
        assertTrue(bow.ranged().supported());assertFalse(bow.supported());
        var arrow=ModernAmmoTest.facts(40744);
        var hit=ModernRangedCombat.resolve(bow.ranged().values(),plain.ranged().values(),arrow,100,b->b-1,0);
        var down=ModernRangedCombat.resolve(bow.ranged().values(),generic.ranged().values(),arrow,100,b->b-1,0);
        // Compare unsaturated remaining-HP-independent damage via a larger target.
        assertTrue(hit.hit());assertTrue(down.hit());assertEquals(hit.rawDamage(),down.rawDamage());
        assertEquals(hit,ModernRangedCombat.resolve(bow.ranged().values(),magic.ranged().values(),arrow,100,b->b-1,0));
        var largePlain=large(plain.ranged().values());var largeGeneric=large(generic.ranged().values());
        var a=ModernRangedCombat.resolve(bow.ranged().values(),largePlain,arrow,10000,b->b-1,0);
        var d=ModernRangedCombat.resolve(bow.ranged().values(),largeGeneric,arrow,10000,b->b-1,0);
        assertEquals(20,a.appliedDamage()-d.appliedDamage());
    }
    @Test void threeSpellsSubtractMagicOnlyBeforeMrAndBeforeIndependentCritical() {
        var attack=magicValues(30,0,0);long armor=5,only=7;
        for(long mr:new long[]{0,51,100,200,999}) {
            var defense=magicValues(12,mr,armor,only);var baseDefense=magicValues(12,0,0);
            var base=ModernMagicCombat.resolveValues(attack,baseDefense,10000,b->b-1);
            var actual=ModernMagicCombat.resolveValues(attack,defense,10000,b->b-1);
            assertEquals(ModernMagicCombat.afterResistance(Math.max(0,base.rawDamage()-armor-only),mr,0),actual.rawDamage());
            var stormBase=ModernFireStorm.resolve(new ModernFireStorm.Values(attack,0),new ModernFireStorm.Values(baseDefense,0),10000,b->b-1,0);
            var storm=ModernFireStorm.resolve(new ModernFireStorm.Values(attack,0),new ModernFireStorm.Values(defense,0),10000,b->b-1,0);
            assertEquals(ModernMagicCombat.afterResistance(Math.max(0,stormBase.rawDamage()-armor-only),mr,0),storm.rawDamage());
            var disBase=ModernDisintegrate.resolve(attack,baseDefense,10000,b->b-1,0);
            var dis=ModernDisintegrate.resolve(attack,defense,10000,b->b-1,0);
            assertEquals(ModernMagicCombat.afterResistance(Math.max(0,disBase.rawDamage()-armor-only),mr,0),dis.rawDamage());
            var critical=ModernDisintegrate.resolve(attack,defense,10000,b->b==100?0:b-1,0);
            assertEquals(dis.rawDamage()*2,critical.rawDamage());
        }
    }
    @Test void genericPoolIsSampledOnceAndFlatMagicReductionDoesNotAddRandomDraws() {
        var bounds=new ArrayList<Long>();var a=magicValues(30,0,0);var d=magicValues(12,0,20,7);
        var out=ModernMagicCombat.resolveValues(a,d,10000,b->{bounds.add(b);return b-1;});
        assertEquals(List.of(10L,100L,10L),bounds);
        long baseline=ModernMagicCombat.resolveValues(a,magicValues(12,0,0),10000,b->b-1).rawDamage();assertEquals(baseline-27,out.rawDamage());
        bounds.clear();ModernFireStorm.resolve(new ModernFireStorm.Values(a,0),new ModernFireStorm.Values(d,0),10000,b->{bounds.add(b);return b-1;},0);
        assertEquals(10,bounds.size());assertEquals(10,bounds.get(9));
        bounds.clear();ModernDisintegrate.resolve(a,d,10000,b->{bounds.add(b);return b-1;},0);
        assertEquals(11,bounds.size());assertEquals(10,bounds.get(10));
    }
    @Test void wideSignedMagicTermChecksFinalSubtractionAndNeverCreditsHp() {
        assertEquals(101,ModernMagicCombat.beforeResistance(100,Long.MIN_VALUE,Long.MAX_VALUE));
        assertEquals(0,ModernMagicCombat.beforeResistance(100,Long.MAX_VALUE,Long.MAX_VALUE));
        assertEquals(120,ModernMagicCombat.beforeResistance(100,-10,-10));
        assertThrows(ArithmeticException.class,()->ModernMagicCombat.beforeResistance(Long.MAX_VALUE,0,-1));
        var f=ModernPhysicalCombat.qualifyVip(ModernVipCharacterStateTest.start(source(Map.of(DAMAGE_REDUCTION,4_294_967_299L,MAGIC_REDUCTION,4_294_967_307L),false),0),1000);
        assertEquals(4_294_967_299L,f.defense().armorReduction());assertEquals(4_294_967_307L,f.magic().defense().magicOnlyReduction());
        var a=magicValues(30,0,0);var d=magicValues(12,0,0,Long.MAX_VALUE);
        var hit=ModernMagicCombat.resolveValues(a,d,100,b->b-1);assertEquals(0,hit.appliedDamage());assertEquals(100,hit.remainingHp());assertFalse(hit.death());
        assertEquals(0,ModernFireStorm.resolve(new ModernFireStorm.Values(a,0),new ModernFireStorm.Values(d,0),100,b->b-1,0).rawDamage());
        assertEquals(0,ModernDisintegrate.resolve(a,d,100,b->b-1,0).rawDamage());
    }
    @Test void removingAndExpiringAppliedStateRebuildsNumericConsumersWithoutRefreshingTemplates() {
        var source=source(Map.of(DAMAGE_REDUCTION,20L,MAGIC_REDUCTION,7L,HPR,13L),false);
        source=ModernVipLifecycleTest.timed(source,1000,"2026-10-01 00:00:00",1);var before=ModernVipCharacterStateTest.start(source,0);
        var fighter=ModernPhysicalCombat.qualifyVip(before,1999);assertEquals(20,fighter.defense().armorReduction());
        assertThrows(IllegalArgumentException.class,()->ModernPhysicalCombat.qualifyVip(before,2000));
        assertThrows(IllegalArgumentException.class,()->ModernPhysicalCombat.qualifyVip(before,999));
        var candidate=ModernVipCharacterState.prepareExpiry(before,2000).orElseThrow();
        var after=ModernVipCharacterStateTest.accept(candidate,before,2100,37,11);var empty=ModernPhysicalCombat.qualifyVip(after,2100);
        assertTrue(empty.supported());assertTrue(empty.magic().supported());assertTrue(empty.regen().supported());
        assertEquals(0,empty.defense().armorReduction());assertEquals(0,empty.magic().defense().magicOnlyReduction());assertEquals(0,empty.regen().values().equipmentHpRegen());
        assertEquals(37,empty.profile().effective().initialHp());assertEquals(11,empty.profile().effective().initialMp());
        assertThrows(IllegalArgumentException.class,()->fighter.vip().require(empty.vip().source(),empty.profile()));
    }
    @Test void numericSupportDoesNotAdmitTheOldEngineWithoutFrameAndResourceIntegration() {
        var s=source(Map.of(),false);var row=s.input().equipment().source().character();
        var rooms=new ModernRoomsTest.Fixture();var player=rooms.player(7,row.objectId(),row.level(),0);
        try {
            var scope=player.scope();
            var old=ModernPhysicalCombat.qualify(s,ModernCombatProfileTest.binding(s.input().equipment()),Optional.empty(),1000);
            assertDoesNotThrow(()->new ModernBattleEngine.PreparedMember(player.owner(),scope,row,new ModernWorldMap.Cell(1,1),100_000_000L,old,100_000_000L));
            var complete=ModernPhysicalCombat.qualifyVip(ModernVipCharacterStateTest.start(s,0),1000);
            assertThrows(IllegalArgumentException.class,()->new ModernBattleEngine.PreparedMember(player.owner(),scope,row,new ModernWorldMap.Cell(1,1),100_000_000L,complete,100_000_000L));
        } finally {rooms.rooms.disconnect(player.owner());rooms.presence.disconnect(player.owner());}
    }
    @Test void protectionCombinationRequiresTheSameQualifiedSourceAndKeepsBothProofs() {
        var source=ModernArmorProtectionLoadoutTest.withRules(source(Map.of(DAMAGE_REDUCTION,20L,MAGIC_REDUCTION,7L),false),Set.of(),Map.of());
        var state=ModernVipCharacterStateTest.start(source,0);
        var fighter=ModernPhysicalCombat.qualifyVipProtection(state,1000,ModernArmorProtectionLoadoutTest.bound(source));
        assertTrue(fighter.supported());assertTrue(fighter.magic().supported());assertTrue(fighter.regen().supported());
        assertNotNull(fighter.armorProtection());assertSame(state,fighter.vip().state());assertEquals(7,fighter.magic().defense().magicOnlyReduction());
        var copy=ModernArmorProtectionLoadoutTest.withRules(source,Set.of(),Map.of());
        assertThrows(IllegalArgumentException.class,()->ModernPhysicalCombat.qualifyVipProtection(state,1000,ModernArmorProtectionLoadoutTest.bound(copy)));
        assertThrows(IllegalArgumentException.class,()->ModernPhysicalCombat.qualifyEquipmentChange(source,
                ModernCombatProfileTest.binding(source.input().equipment()),Optional.empty(),1000,fighter,37,11));
    }
    static ModernMagicCombat.Values magicValues(long intelligence,long mr,long armor){return magicValues(intelligence,mr,armor,0);}
    static ModernMagicCombat.Values magicValues(long intelligence,long mr,long armor,long only) {
        return new ModernMagicCombat.Values(true,10000,new ModernMagicCombat.Offense(intelligence,0,Math.max(1,intelligence-12),0,0,0,0),
                new ModernMagicCombat.Defense(0,0,0,0,0,mr,armor,only));
    }
    private static ModernRangedCombat.Values large(ModernRangedCombat.Values v) {
        return new ModernRangedCombat.Values(v.supported(),v.defenseSupported(),10000,v.range(),v.evasion(),v.offense(),v.defense(),v.meleeDefense(),v.weapon(),v.multiplier());
    }
    static ModernPhysicalCombat.Source source(Map<ModernVipEquipment.Field,Long> fields,boolean bow) {
        var vip=ModernEquipmentEvaluatorTest.instance(1,300086,ModernCharacterData.ItemKind.ETC,true,0,"vip",List.of(),Ability.zero(),Ability.zero(),
                ModernEquipmentEvaluatorTest.emptyCards(),Stone.none(),TemplatePhysical.zero());
        var items=new ArrayList<Instance>();items.add(vip);
        if(bow) {
            var w=ModernPhysicalCombatTest.weapon(3,1,true,0,Ability.zero(),TemplatePhysical.zero());var t=w.template();
            items.add(new Instance(w.source(),new Template(t.metadata(),t.rules(),4,t.safeEnchant(),t.setIds(),t.base(),t.physical()),w.greater(),w.dark(),w.cards(),w.stone()));
        }
        var base=ModernPhysicalCombatTest.source(7,0,30,items);var q=base.input().equipment();var c=q.catalog();var rules=new HashMap<>(c.lookups());
        var values=new EnumMap<ModernVipEquipment.Field,Long>(ModernVipEquipment.Field.class);values.putAll(ModernVipEquipment.Abilities.zero().values());values.putAll(fields);
        var partial=Ability.zero();
        for(var e:fields.entrySet()) {
            var f=e.getKey();if(Set.of(CHA,AC,MAX_HP,MAX_MP,DAMAGE_REDUCTION,MAGIC_REDUCTION,EXP_RATE,ADENA_RATE).contains(f))continue;
            partial=partial.with(Stat.valueOf(f==HPR?"HP_REGEN":f==MPR?"MP_REGEN":f.name()),e.getValue());
        }
        rules.put(new RuleKey(RuleKind.VIP,ModernCharacterData.ItemKind.ETC,300086,0),RuleValue.present(partial));
        q=new Qualified(q.source(),q.items(),new Catalog(7,"catalog",rules,c.verifiedSetIds(),c.sets(),c.combos(),VipTableState.VERIFIED_PRESENT),q.lifetimes());
        var policies=new HashMap<>(base.templates());policies.put(300086L,new ModernPhysicalCombat.TemplatePolicy(ModernCharacterData.ItemKind.ETC,"add.VIP",null));
        var healing=new ArrayList<ModernHonorHealing.Row>();for(int i=1;i<=127;i++)healing.add(new ModernHonorHealing.Row(i,0));
        return new ModernPhysicalCombat.Source(new ModernCombatProfile.Input(q,true,base.input().weapons()),base.growth(),policies,Set.of(),ModernMagicCombatTest.growth(7),
                bow?Map.of(1L,0L):Map.of(),bow?Map.of(1L,-1L):Map.of(),ModernNaturalRegenTest.growth(7,"catalog",5),new ModernHonorHealing.Growth(7,"catalog",healing),
                new ModernVipEquipment.Catalog(7,"catalog",Map.of(300086L,new ModernVipEquipment.Vip(300086,0,505,0,new ModernVipEquipment.Abilities(values))),Map.of()));
    }
}
