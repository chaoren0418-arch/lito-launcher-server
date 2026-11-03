package local.lito381.server;

import static org.junit.jupiter.api.Assertions.*;
import static local.lito381.server.ModernArmorProcProtection.*;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ModernIncomingDamageTest {
    static Actor actor(){return ModernArmorProcProtectionTest.actor();}
    static Selection selection(){return ModernArmorProcProtectionTest.selection();}
    static State active(){return prepareHurt(selection(),initial(actor()),0,1000,1,1,true,b->0).after();}
    static ModernIncomingDamage.Protection protection(){return ModernIncomingDamage.at(active(),SECOND);}
    static final class Draws implements ModernPhysicalCombat.BoundedRandom {
        final List<Long> bounds=new ArrayList<>();final long[] sequence;int index;
        Draws(long... sequence){this.sequence=sequence;}
        public long nextLong(long bound){
            bounds.add(bound);
            if(sequence.length==0)return bound-1;
            assertTrue(index<sequence.length,"unexpected extra random draw: "+bounds);
            return sequence[index++];
        }
    }
    static ModernPhysicalCombat.Values physical(long bonus,long armor,long directional,long pvp) {
        var source=ModernPhysicalCombatTest.source(7,0,30,List.of(ModernPhysicalCombatTest.weapon(1,1,true,0,
                ModernEquipmentEvaluatorTest.bonus(ModernEquipmentFacts.Stat.MELEE_DAMAGE,bonus),ModernEquipmentFacts.TemplatePhysical.zero())));
        var old=ModernPhysicalCombatTest.fighter(source).values();
        return new ModernPhysicalCombat.Values(true,Long.MAX_VALUE,old.offense(),
                new ModernPhysicalCombat.Defense(10,0,0,armor,directional,pvp),old.unarmed(),old.weapon(),old.multiplier());
    }
    static ModernMagicCombat.Values magic(long mr,long armor) {
        return new ModernMagicCombat.Values(true,Long.MAX_VALUE,new ModernMagicCombat.Offense(13,0,1,0,1,0,0),
                new ModernMagicCombat.Defense(0,0,0,0,mr,mr,armor));
    }

    @Test void receiptPreservesUnclippedDamageAndZeroOneOddAndWideValues() {
        for(long damage:new long[]{0,1,3,101,1L<<40,Long.MAX_VALUE})for(long hp:new long[]{1,99,Long.MAX_VALUE}) {
            var impact=protection().apply(damage,hp);
            assertEquals(damage>0,impact.positiveIncoming());
            assertEquals(damage/2,impact.damage());assertEquals(Math.min(damage/2,hp),impact.applied());
            assertEquals(hp-impact.applied(),impact.remaining());assertEquals(impact.remaining()==0,impact.death());
            var normal=ModernIncomingDamage.NONE.apply(damage,hp);assertEquals(damage,normal.damage());
        }
        var incoming=protection().apply(1000,100);
        assertEquals(500,incoming.damage());assertEquals(100,incoming.applied());assertTrue(incoming.death());
        assertThrows(IllegalArgumentException.class,()->protection().apply(-1,100));
        assertThrows(IllegalArgumentException.class,()->protection().apply(1,0));
        assertThrows(IllegalArgumentException.class,()->protection().apply(BigInteger.ONE.shiftLeft(161),100));
        var huge=BigInteger.valueOf(Long.MAX_VALUE).multiply(BigInteger.TWO);
        assertEquals(Long.MAX_VALUE,protection().apply(huge,Long.MAX_VALUE).damage());
        assertThrows(ArithmeticException.class,()->ModernIncomingDamage.NONE.apply(huge,1));
    }
    @Test void positiveIncomingStillRollsWhenExistingProtectionRoundsHpLossToZero() {
        for(long damage:new long[]{0,1}) {
            var impact=protection().apply(damage,100);var draws=new Draws(0);
            var candidate=prepareHurt(selection(),impact,true,draws);
            assertEquals(100,candidate.hpAfter());
            assertEquals(damage,draws.index);
            assertEquals(damage,candidate.triggers().size());
            assertEquals(active().untilNanos(),candidate.after().untilNanos());
            if(damage==1)assertFalse(candidate.triggers().get(0).appliedProtection());
        }
        var wide=protection().apply(BigInteger.valueOf(Long.MAX_VALUE).multiply(BigInteger.TWO),Long.MAX_VALUE);
        assertTrue(wide.positiveIncoming());
    }
    @Test void meleeReducesAfterArmorDirectionalAndPvpButBeforeRemainingHpClippingWithoutExtraRng() {
        var a=physical(100,0,0,0);var d=physical(0,5,7,9);var normalDraws=new Draws();var protectedDraws=new Draws();
        var normal=ModernPhysicalCombat.resolveProtected(a,d,Long.MAX_VALUE,normalDraws,0,ModernIncomingDamage.NONE);
        var hit=ModernPhysicalCombat.resolveProtected(a,d,30,protectedDraws,0,protection());
        assertTrue(hit.outcome().hit());assertEquals(normal.outcome().rawDamage(),hit.outcome().rawDamage());
        assertEquals((normal.outcome().rawDamage()-21)/2,hit.impact().damage());
        assertTrue(hit.impact().damage()>30);assertEquals(30,hit.outcome().appliedDamage());assertTrue(hit.outcome().death());
        assertEquals(normalDraws.bounds,protectedDraws.bounds);
        assertEquals(ModernPhysicalCombat.resolveValues(a,d,Long.MAX_VALUE,new Draws(),0),normal.outcome());
    }
    @Test void bowKeepsArrowAndCriticalDrawOrderAndOnlyHalvesPostDefenseDamage() {
        var a=ModernRangedCombatTest.fighter(ModernRangedCombatTest.source(1,4)).values();
        var d=ModernRangedCombatTest.fighter(ModernRangedCombatTest.source(1,0)).values();var arrow=ModernAmmoTest.facts(40744);
        var normalDraws=new Draws();var protectedDraws=new Draws();
        var normal=ModernRangedCombat.resolve(a,d,arrow,100,normalDraws,0);
        var hit=ModernRangedCombat.resolveProtected(a,d,arrow,2,protectedDraws,0,protection());
        assertEquals(normal.rawDamage(),hit.outcome().rawDamage());assertEquals(normal.appliedDamage()/2,hit.impact().damage());
        assertEquals(2,hit.outcome().appliedDamage());assertEquals(normalDraws.bounds,protectedDraws.bounds);
    }
    @Test void allThreeMagicLanesProtectAfterMrAndDisintegrateCriticalWithSameRandomOrder() {
        for(int mr:new int[]{0,1,49,50,51,99,100,101,120,141,161,200,241,999})for(int armor:new int[]{0,5,20}) {
            var a=magic(0,0);var d=magic(mr,armor);
            var one=new Draws();var two=new Draws();
            var old=ModernMagicCombat.resolveValues(a,d,Long.MAX_VALUE,one,0);
            var next=ModernMagicCombat.resolveProtected(a,d,Long.MAX_VALUE,two,0,protection());
            assertEquals(old.rawDamage()/2,next.outcome().rawDamage());assertEquals(one.bounds,two.bounds);assertEquals(old.critical(),next.outcome().critical());
            one=new Draws();two=new Draws();
            var fireOld=ModernFireStorm.resolve(new ModernFireStorm.Values(a,0),new ModernFireStorm.Values(d,10),Long.MAX_VALUE,one,2);
            var fireNew=ModernFireStorm.resolveProtected(new ModernFireStorm.Values(a,0),new ModernFireStorm.Values(d,10),Long.MAX_VALUE,two,2,protection());
            assertEquals(fireOld.rawDamage()/2,fireNew.outcome().rawDamage());assertEquals(one.bounds,two.bounds);
            for(boolean critical:new boolean[]{false,true}) {
                var bounds1=new ArrayList<Long>();var bounds2=new ArrayList<Long>();
                var disOld=ModernDisintegrate.resolve(a,d,Long.MAX_VALUE,b->{bounds1.add(b);return b==15||critical?0:b-1;},0);
                var disNew=ModernDisintegrate.resolveProtected(a,d,Long.MAX_VALUE,b->{bounds2.add(b);return b==15||critical?0:b-1;},0,protection());
                assertEquals(disOld.rawDamage()/2,disNew.outcome().rawDamage());assertEquals(critical,disNew.outcome().critical());assertEquals(bounds1,bounds2);
            }
        }
        // Retained LiTo critical is after MR: 216 * .73 ->157, x2->314, /2->157.
        var hit=ModernDisintegrate.resolveProtected(ModernDisintegrateTest.values(12,0,0,0,0),
                ModernDisintegrateTest.values(12,0,0,51,0),Long.MAX_VALUE,b->0,0,protection());
        assertEquals(157,hit.outcome().rawDamage());
    }
    @Test void disintegrateProtectionRunsBeforeFinalI64ValidationAndNeverBeforeSpecialCritical() {
        var a=ModernDisintegrateTest.values(12,0,Long.MAX_VALUE,0,0);var d=ModernDisintegrateTest.values(12,0,0,0,0);
        assertThrows(ModernDisintegrate.CombatException.class,()->ModernDisintegrate.resolve(a,d,1,b->b==15?0:99,0));
        var hit=ModernDisintegrate.resolveProtected(a,d,Long.MAX_VALUE,b->b==15?0:99,0,protection());
        assertEquals(BigInteger.valueOf(Long.MAX_VALUE).add(BigInteger.valueOf(216)).divide(BigInteger.TWO).longValueExact(),hit.impact().damage());
        assertEquals(ModernDisintegrate.Failure.ARITHMETIC_OVERFLOW,assertThrows(ModernDisintegrate.CombatException.class,
                ()->ModernDisintegrate.resolveProtected(a,d,1,b->0,0,protection())).failure());
    }
    @Test void npcIncomingUsesItsOwnDefenseAndKeepsItsCatalogIdentityAndDrawOrder() {
        var c=ModernNpcMeleeTest.catalog();var attacker=ModernNpcMeleeTest.actor(c);
        var p=ModernNpcMeleeTest.player(ModernNpcMeleeTest.armed());var victim=ModernNpcMelee.victim(p);
        var owner=p.fighter().key();var actor=new Actor(new UUID(0,327),owner,1,0);
        var state=new State(actor,1,0,3*SECOND);var protectedDraws=new Draws();var normalDraws=new Draws();
        var normal=ModernNpcMelee.againstParticipant(c,attacker,victim,100,false,normalDraws);
        var hit=ModernNpcMelee.againstProtectedParticipant(c,attacker,victim,1,false,protectedDraws,ModernIncomingDamage.at(state,0));
        assertTrue(hit.outcome().hit());assertEquals(normal.rawDamage(),hit.outcome().rawDamage());
        assertEquals(normal.damage()/2,hit.impact().damage());assertEquals(normalDraws.bounds,protectedDraws.bounds);
        assertEquals(1,hit.outcome().appliedDamage());assertTrue(hit.outcome().death());
        var foreign=new State(new Actor(actor.match(),new TournamentPresence.CharacterKey(42,owner.objectId()),1,0),1,0,3*SECOND);
        assertThrows(IllegalArgumentException.class,()->ModernNpcMelee.againstProtectedParticipant(c,attacker,victim,1,false,
                b->{fail("binding before RNG");return 0;},ModernIncomingDamage.at(foreign,0)));
    }
    @Test void damageThenProcUsesOldStatusAndDeathDoesNotBecomeClippedHalfSurvival() {
        var sel=selection();var before=initial(actor());var a=physical(100,0,0,0);var d=physical(0,0,0,0);
        var hit=ModernPhysicalCombat.resolveProtected(a,d,1000,new Draws(),0,ModernIncomingDamage.at(before,0));
        var proc=prepareHurt(sel,hit.impact(),true,b->0);
        assertEquals(hit.outcome().remainingHp(),proc.hpAfter());assertEquals(hit.impact().damage(),hit.outcome().appliedDamage());
        var committed=acceptAt(proc,sel,before,1000,SECOND);
        assertEquals(4*SECOND,committed.untilNanos()); // Full duration begins at accepted commit, not calculation.
        var lethal=ModernPhysicalCombat.resolveProtected(a,d,1,new Draws(),0,ModernIncomingDamage.at(committed,2*SECOND));
        var death=prepareHurt(sel,lethal.impact(),true,b->0);
        assertEquals(0,death.hpAfter());assertTrue(death.triggers().isEmpty());
        assertEquals(0,acceptAt(death,sel,committed,1,2*SECOND).untilNanos());
    }
    @Test void expiryBetweenCalculationAndCommitRejectsWithoutRerollingAndExistingBuffNeverRefreshes() {
        var state=active();var protection=ModernIncomingDamage.at(state,SECOND);var sel=selection();
        var candidate=prepareHurt(sel,protection.apply(10,1000),true,b->0);
        assertTrue(protection.currentAt(state,2*SECOND));assertFalse(protection.currentAt(state,3*SECOND));
        assertEquals(3*SECOND,acceptAt(candidate,sel,state,1000,2*SECOND).untilNanos());
        assertEquals(Failure.STALE_CANDIDATE,assertThrows(ProcException.class,()->acceptAt(candidate,sel,state,1000,3*SECOND)).failure());
        assertThrows(ProcException.class,()->acceptAt(candidate,sel,state,1000,0));
        assertThrows(ProcException.class,()->acceptAt(candidate,sel,state,999,2*SECOND));
        assertThrows(ProcException.class,()->acceptAt(candidate,new Selection(actor(),1,sel.rules()),state,1000,2*SECOND));
        var revived=new State(new Actor(actor().match(),actor().owner(),actor().entity(),1),1,SECOND,3*SECOND);
        assertFalse(protection.currentAt(revived,SECOND));assertThrows(ProcException.class,()->acceptAt(candidate,sel,revived,1000,SECOND));
        assertFalse(protection.currentAt(state,0));assertFalse(ModernIncomingDamage.NONE.currentAt(state,0));
    }
    @Test void pureZeroDamageAndMissReceiptsCannotCreateAProcOrConsumeExtraRandom() {
        var before=initial(actor());var p=ModernIncomingDamage.at(before,0);
        var a=physical(0,0,0,0);var d=physical(0,1000,0,0);var draws=new Draws(0,1499,99);
        var evasive=new ModernPhysicalCombat.Values(true,d.maximumHp(),d.offense(),
                new ModernPhysicalCombat.Defense(-100,1500,0,1000,0,0),d.unarmed(),d.weapon(),d.multiplier());
        var missed=ModernPhysicalCombat.resolveProtected(a,evasive,1000,draws,0,p);
        assertFalse(missed.outcome().hit());assertEquals(List.of(20L,1500L,100L),draws.bounds);
        assertTrue(prepareHurt(selection(),missed.impact(),true,b->{fail("miss proc");return 0;}).triggers().isEmpty());
        var zero=ModernPhysicalCombat.resolveProtected(a,d,1000,new Draws(),0,p);
        assertEquals(0,zero.impact().damage());assertTrue(prepareHurt(selection(),zero.impact(),true,b->{fail("zero proc");return 0;}).triggers().isEmpty());
        assertThrows(ProcException.class,()->prepareHurt(selection(),ModernIncomingDamage.NONE.apply(1,1000),true,b->0));
    }
    @Test void typedCombatantAdaptersRejectForeignSourceAndBotProtectionBeforeAnyDraw() {
        var row=new ModernApplicationProtocol.CharacterRow(3333,"fixture",1,0,80);
        var a=ModernCombatant.human(ModernMagicCombatTest.createFighter(42,row,1000,100,13,1));
        var d=ModernCombatant.human(ModernMagicCombatTest.createFighter(7,row,1000,100,13,1));
        var applied=ModernCombatant.resolveProtectedPhysical(a,d,1000,new Draws(),0,null,protection());
        assertTrue(applied.outcome().hit());
        assertEquals(applied.outcome().appliedDamage(),applied.impact().applied());
        assertThrows(IllegalArgumentException.class,()->ModernCombatant.resolveProtectedPhysical(d,a,1000,b->{fail();return 0;},0,null,protection()));
        assertThrows(IllegalArgumentException.class,()->ModernCombatant.resolveProtectedLightArrow(d,a,1000,b->{fail();return 0;},0,protection()));
        var bot=ModernCombatant.bot(ModernParticipantId.bot(7,3333),ModernBotTemplate.roomTestDummy(1,0));
        assertThrows(IllegalArgumentException.class,()->ModernCombatant.resolveProtectedPhysical(a,bot,1,b->{fail();return 0;},0,null,protection()));
        var light=ModernCombatant.resolveProtectedLightArrow(a,d,1000,new Draws(),0,protection());
        assertEquals(light.outcome().appliedDamage(),light.impact().damage());
    }
    @Test void expiredProtectionIsIdenticalToUnprotectedDamageInAllSixLanes() {
        var expired=ModernIncomingDamage.at(active(),3*SECOND);
        assertFalse(expired.active());var a=physical(100,0,0,0);var d=physical(0,5,7,9);
        for(long hp:new long[]{1,100}) {
            assertEquals(ModernPhysicalCombat.resolveValues(a,d,hp,new Draws(),0),
                    ModernPhysicalCombat.resolveProtected(a,d,hp,new Draws(),0,expired).outcome());
            var bow=ModernRangedCombatTest.fighter(ModernRangedCombatTest.source(1,4)).values();
            var bowDefense=ModernRangedCombatTest.fighter(ModernRangedCombatTest.source(1,0)).values();
            var arrow=ModernAmmoTest.facts(40744);
            assertEquals(ModernRangedCombat.resolve(bow,bowDefense,arrow,hp,new Draws(),0),
                    ModernRangedCombat.resolveProtected(bow,bowDefense,arrow,hp,new Draws(),0,expired).outcome());
            var ma=magic(0,0);var md=magic(51,20);
            assertEquals(ModernMagicCombat.resolveValues(ma,md,hp,new Draws(),0),
                    ModernMagicCombat.resolveProtected(ma,md,hp,new Draws(),0,expired).outcome());
            var fa=new ModernFireStorm.Values(ma,0);var fd=new ModernFireStorm.Values(md,10);
            assertEquals(ModernFireStorm.resolve(fa,fd,hp,new Draws(),0),
                    ModernFireStorm.resolveProtected(fa,fd,hp,new Draws(),0,expired).outcome());
            assertEquals(ModernDisintegrate.resolve(ma,md,hp,new Draws(),0),
                    ModernDisintegrate.resolveProtected(ma,md,hp,new Draws(),0,expired).outcome());
            var c=ModernNpcMeleeTest.catalog();var npc=ModernNpcMeleeTest.actor(c);
            var victim=ModernNpcMelee.victim(ModernNpcMeleeTest.player(ModernNpcMeleeTest.armed()));
            assertEquals(ModernNpcMelee.againstParticipant(c,npc,victim,hp,false,new Draws()),
                    ModernNpcMelee.againstProtectedParticipant(c,npc,victim,hp,false,new Draws(),expired).outcome());
        }
    }
    @Test void equipmentRemovalKeepsTheOldLeaseButCannotProcAndStaleWorldEntityLifeOrReplayRejects() {
        var before=active();var empty=new Selection(actor(),1,List.of());
        var impact=ModernIncomingDamage.at(before,SECOND).apply(100,1000);
        var candidate=prepareHurt(empty,impact,true,b->{fail("removed armor cannot roll");return 0;});
        var committed=acceptAt(candidate,empty,before,1000,2*SECOND);
        assertEquals(950,candidate.hpAfter());assertEquals(3*SECOND,committed.untilNanos());
        assertThrows(ProcException.class,()->acceptAt(candidate,empty,committed,1000,2*SECOND));
        var expired=expire(committed,3*SECOND);assertEquals(0,expired.untilNanos());
        assertFalse(ModernIncomingDamage.at(expired,3*SECOND).active());
        for(var actor:List.of(new Actor(new UUID(0,99),actor().owner(),1,0),
                new Actor(actor().match(),actor().owner(),2,0),new Actor(actor().match(),actor().owner(),1,1))) {
            var current=new State(actor,before.sequence(),before.observedNanos(),before.untilNanos());
            assertFalse(impact.protection().currentAt(current,SECOND));
            assertEquals(Failure.STALE_CANDIDATE,assertThrows(ProcException.class,
                    ()->acceptAt(candidate,empty,current,1000,2*SECOND)).failure());
        }
        // Even a zero-loss receipt with the same HP cannot be replayed.
        var miss=prepareHurt(empty,ModernIncomingDamage.at(expired,3*SECOND).apply(0,950),true,b->{fail();return 0;});
        var next=acceptAt(miss,empty,expired,950,3*SECOND);
        assertThrows(ProcException.class,()->acceptAt(miss,empty,next,950,3*SECOND));
    }
    @Test void bowTypedAdapterPreservesWideOwnerIdentityAndForwardsTheProtectionReceipt() {
        var a=bowCombatant(7,0x1_0000_0001L,4);var d=bowCombatant(42,0x1_0000_0001L,0);
        var actor=new Actor(new UUID(0,327),d.identity().humanKey(),2,0);
        var protection=ModernIncomingDamage.at(new State(actor,1,0,3*SECOND),SECOND);
        var normalDraws=new Draws();var protectedDraws=new Draws();var arrow=ModernAmmoTest.facts(40744);
        var normal=ModernCombatant.resolvePhysical(a,d,100,normalDraws,0,arrow);
        var hit=ModernCombatant.resolveProtectedPhysical(a,d,2,protectedDraws,0,arrow,protection);
        assertTrue(hit.outcome().death());assertEquals(normal.rawDamage()/2,hit.impact().damage());
        assertEquals(normalDraws.bounds,protectedDraws.bounds);
        assertThrows(IllegalArgumentException.class,()->ModernCombatant.resolveProtectedPhysical(a,d,2,
                b->{fail("foreign bow defender binding before RNG");return 0;},0,arrow,protection()));
    }
    static ModernCombatant bowCombatant(int source,long objectId,int type) {
        var old=ModernPhysicalCombatTest.fixtureSource(source,new ModernApplicationProtocol.CharacterRow(objectId,"ranged",1,0,80),100,10,type);
        var qualified=new ModernPhysicalCombat.Source(old.input(),old.growth(),old.templates(),old.procCatalogIds(),null,
                type==0?Map.of():Map.of(1L,0L),type==0?Map.of():Map.of(1L,-1L));
        return ModernCombatant.human(ModernPhysicalCombatTest.fighter(qualified),true);
    }
}
