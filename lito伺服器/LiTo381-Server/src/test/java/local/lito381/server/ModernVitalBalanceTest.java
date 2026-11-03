package local.lito381.server;

import static org.junit.jupiter.api.Assertions.*;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.util.HexFormat;
import org.junit.jupiter.api.Test;

class ModernVitalBalanceTest {
    @Test void selfHealingCommitsBothResourcesAtOneRevisionWithoutErasingEitherFlow() {
        var before=start().damage(2,100).spend(3,70);
        var after=before.restoreAndSpend(4,8,4);
        assertEquals(908,after.vitals().hp());assertEquals(176,after.vitals().mp());
        assertEquals(8,after.flows().restored());assertEquals(74,after.flows().spent());assertEquals(100,after.flows().damaged());
        assertEquals(4,after.revision());assertEquals(before.life(),after.life());assertEquals(before.equipmentRevision(),after.equipmentRevision());
        assertThrows(IllegalArgumentException.class,()->before.spend(4,4).restore(4,8));
        assertThrows(IllegalArgumentException.class,()->before.restore(4,8).spend(4,4));
        assertEquals(900,before.vitals().hp());assertEquals(180,before.vitals().mp());
    }
    @Test void combinedRecoveryRejectsDeadLifeOverhealOverdraftStaleRevisionAndOverflowWithoutPartialChange() {
        var before=start().damage(2,100);
        for(long[] input:new long[][]{{2,8,4},{3,-1,4},{3,8,-1},{3,101,4},{3,8,251}})
            assertThrows(IllegalArgumentException.class,()->before.restoreAndSpend(input[0],input[1],input[2]));
        assertThrows(IllegalArgumentException.class,()->before.damage(3,900).restoreAndSpend(4,1,0));
        var n=Long.MAX_VALUE;
        var limit=new ModernVitalBalance.State(2,0,0,1000,n,new ModernEquipmentVitals.Vitals(1000,1001,1,n),
                new ModernVitalBalance.Flows(0,0,n,n,0,1,0,n,0,0,0));
        assertThrows(ArithmeticException.class,()->limit.restoreAndSpend(3,1,0));
        assertThrows(ArithmeticException.class,()->limit.restoreAndSpend(3,0,1));
        assertEquals(n,limit.flows().restored());assertEquals(1,limit.vitals().mp());
        assertEquals(0,start().restoreAndSpend(2,0,0).flows().spent());
    }
    @Test void wideSelfHealingCorpusRetainsExactJavaAndRustIntegerTotals() {
        long maximum=Long.MAX_VALUE,hp=maximum-10000,mp=maximum,restored=0,spent=0;
        var state=ModernVitalBalance.State.initial(1,0,new ModernEquipmentVitals.Vitals(hp,maximum,mp,maximum));
        for(int i=1;i<=256;i++) {
            long gain=Math.min(maximum-hp,i*37L%101),cost=i*13L%5;
            hp+=gain;mp-=cost;restored+=gain;spent+=cost;
            state=state.restoreAndSpend(i+1L,gain,cost);
            assertEquals(hp,state.vitals().hp());assertEquals(mp,state.vitals().mp());
            assertEquals(restored,state.flows().restored());assertEquals(spent,state.flows().spent());
            assertEquals(0,state.flows().hpGain());assertEquals(0,state.flows().damaged());
        }
        assertEquals(10000,restored);assertEquals(513,spent);
        System.out.println("self-healing-balance-v1:"+hp+":"+mp+":"+restored+":"+spent+":256");
    }
    @Test void realCatchUpCanGainAndLoseInOneBatchWithoutErasingEitherFlow() {
        var a=ModernVitalBalance.State.initial(1,0,new ModernEquipmentVitals.Vitals(500,1000,200,250));
        var values=new ModernNaturalRegen.Values(80,1,20,0,0,0,225,5,-3,-2,1000,250);
        var due=new ModernNaturalRegen.Due(ModernNaturalRegen.Clock.initial(),4,2);
        long[] rolls={0,4,0,4};var i=new java.util.concurrent.atomic.AtomicInteger();
        var out=ModernNaturalRegen.resolve(values,due,500,200,b->rolls[i.getAndIncrement()]);
        var b=a.regenerateBatch(2,out.hpGain(),out.hpLoss(),out.mpGain(),out.mpLoss(),out.hp(),out.mp());
        assertEquals(4,b.flows().hpGain());assertEquals(4,b.flows().hpLoss());assertEquals(500,b.vitals().hp());
        assertEquals(2,b.flows().mpLoss());assertEquals(198,b.vitals().mp());assertEquals(0,b.flows().damaged());
        assertThrows(IllegalArgumentException.class,()->a.regenerate(2,4,4,0,2));
    }
    @Test void batchUsesWideArithmeticButRejectsWrongFinalValuesDeadLifeAndCounterOverflow() {
        var a=start();long n=Long.MAX_VALUE;
        var b=a.regenerateBatch(2,n,n,n,n,1000,250);assertEquals(n,b.flows().hpGain());assertEquals(n,b.flows().hpLoss());
        assertThrows(ArithmeticException.class,()->b.regenerateBatch(3,1,1,0,0,1000,250));
        assertThrows(IllegalArgumentException.class,()->a.regenerateBatch(2,4,4,0,0,999,250));
        assertThrows(IllegalArgumentException.class,()->a.regenerateBatch(2,4,4,0,0,1000,249));
        assertThrows(IllegalArgumentException.class,()->a.regenerateBatch(2,0,1000,0,0,0,250));
        assertThrows(IllegalArgumentException.class,()->a.damage(2,1000).regenerateBatch(3,1,0,0,0,1,250));
        assertThrows(IllegalArgumentException.class,()->a.regenerateBatch(2,-1,0,0,0,999,250));
        assertThrows(IllegalArgumentException.class,()->a.regenerateBatch(1,0,0,0,0,1000,250));
        assertEquals(ModernVitalBalance.Flows.ZERO,a.flows());
    }
    static ModernVitalBalance.State start() {
        return ModernVitalBalance.State.initial(1,0,new ModernEquipmentVitals.Vitals(1000,1000,250,250));
    }
    @Test void capacityIsNotBirthAndClippingIsNotDamageOrSpending() {
        var before=start().damage(2,100).spend(3,70);
        var down=before.equip(4,1,100,50);
        assertEquals(1000,down.birthHp());assertEquals(250,down.birthMp());
        assertEquals(100,down.flows().damaged());assertEquals(70,down.flows().spent());
        assertEquals(800,down.flows().hpClipped());assertEquals(130,down.flows().mpClipped());
        var up=down.equip(5,2,2000,900);
        assertEquals(100,up.vitals().hp());assertEquals(50,up.vitals().mp());assertEquals(down.flows(),up.flows());
        assertTrue(ModernVitalBalance.followsEquipment(before,down));
        assertTrue(ModernVitalBalance.followsEquipment(down,up));
        assertFalse(ModernVitalBalance.followsEquipment(before,up));
    }
    @Test void recoverySpendingActualRespawnGrantsAndChangingLifeCapacitiesConserveResources() {
        var a=start().damage(2,100).spend(3,70).regenerate(4,10,0,0,5).restore(5,20);
        var b=a.equip(6,1,100,50).equip(7,2,1000,250).damage(8,100);
        var dead=b.equip(9,3,200,100);assertEquals(0,dead.vitals().hp());
        var born=dead.respawn(10,120,25);
        assertEquals(120,born.vitals().hp());assertEquals(75,born.vitals().mp());assertEquals(1,born.life());
        assertEquals(830,born.flows().hpClipped());assertEquals(125,born.flows().mpClipped());
        assertEquals(10,born.flows().hpGain());assertEquals(20,born.flows().restored());
        assertEquals(200,born.flows().damaged());assertEquals(70,born.flows().spent());
        var again=born.damage(11,120).equip(12,4,400,100).respawn(13,400,0);
        assertEquals(520,again.flows().hpGranted());assertEquals(2,again.life());
        assertEquals(25,again.flows().mpGranted());assertEquals(75,again.spend(14,0).vitals().mp());
    }
    @Test void unappliedOverhealNegativeCostsDeadHealingAndStaleEpochsAreRejected() {
        var a=start();var dead=a.damage(2,1000);
        assertThrows(IllegalArgumentException.class,()->a.restore(2,1));
        assertThrows(IllegalArgumentException.class,()->a.damage(2,1001));
        assertThrows(IllegalArgumentException.class,()->a.spend(2,-1));
        assertThrows(IllegalArgumentException.class,()->a.spend(2,251));
        assertThrows(IllegalArgumentException.class,()->a.damage(1,1));
        assertThrows(IllegalArgumentException.class,()->a.equip(2,2,100,50));
        assertThrows(IllegalArgumentException.class,()->a.equip(2,1,0,50));
        assertThrows(IllegalArgumentException.class,()->a.respawn(2,1,0));
        assertThrows(IllegalArgumentException.class,()->dead.restore(3,1));
        assertThrows(IllegalArgumentException.class,()->dead.spend(3,0));
        assertThrows(IllegalArgumentException.class,()->dead.regenerate(3,1,0,0,0));
        assertThrows(IllegalArgumentException.class,()->dead.respawn(3,1001,0));
        assertThrows(IllegalArgumentException.class,()->a.regenerate(2,0,1000,0,0));
        assertThrows(IllegalArgumentException.class,()->a.damage(2,2).regenerate(3,1,1,0,0));
        assertEquals(ModernVitalBalance.Flows.ZERO,a.flows());
    }
    @Test void balancedForgeriesCannotReplaceBirthOrCombineEventsInAnEquipmentReceipt() {
        var before=start().damage(2,800);var after=before.equip(3,1,100,50);
        var forged=ModernVitalBalance.State.initial(3,1,after.vitals());
        assertFalse(ModernVitalBalance.followsEquipment(before,forged));
        assertFalse(ModernVitalBalance.followsEquipment(before,after.damage(4,1)));
        assertFalse(ModernVitalBalance.followsEquipment(before,before));
        assertFalse(ModernVitalBalance.followsEquipment(null,after));
        assertThrows(IllegalArgumentException.class,()->new ModernVitalBalance.State(3,1,0,999,250,after.vitals(),after.flows()));
        assertThrows(IllegalArgumentException.class,()->new ModernVitalBalance.State(3,1,1,1000,250,start().vitals(),ModernVitalBalance.Flows.ZERO));
        assertThrows(IllegalArgumentException.class,()->new ModernVitalBalance.Flows(-1,0,0,0,0,0,0,0,0,0,0));
    }
    @Test void wideConservationDoesNotOverflowButIndividualCountersCannotWrap() {
        long n=Long.MAX_VALUE;
        var full=new ModernVitalBalance.State(2,0,0,n,n,new ModernEquipmentVitals.Vitals(n,n,n,n),
                new ModernVitalBalance.Flows(n,n,n,n,0,n,n,0,0,0,0));
        assertThrows(ArithmeticException.class,()->full.damage(3,1));assertEquals(n,full.vitals().hp());
        var clipped=new ModernVitalBalance.State(2,0,0,n,n,new ModernEquipmentVitals.Vitals(2,n,2,n),
                new ModernVitalBalance.Flows(n,0,0,n-2,0,n,0,n-2,0,n,n));
        assertThrows(ArithmeticException.class,()->clipped.equip(3,1,1,1));
        var wide=ModernVitalBalance.State.initial(1,0,new ModernEquipmentVitals.Vitals(n,n,n,n)).damage(2,9_007_199_254_740_993L);
        assertEquals(n-9_007_199_254_740_993L,wide.vitals().hp());
        var exhausted=ModernVitalBalance.State.initial(1,n,start().vitals());
        assertThrows(ArithmeticException.class,()->exhausted.equip(2,0,1,0));
    }
    @Test void partialBirthAndZeroMpAreValidButEquipmentCannotRestoreAlreadyClippedResources() {
        var a=ModernVitalBalance.State.initial(1,8,new ModernEquipmentVitals.Vitals(7,100,0,0));
        var b=a.equip(2,9,3,0).equip(3,10,100,100);
        assertEquals(7,b.birthHp());assertEquals(3,b.vitals().hp());assertEquals(0,b.vitals().mp());
        assertEquals(4,b.flows().hpClipped());assertEquals(0,b.spend(4,0).flows().spent());
    }
    @Test void deterministicMixedEventCorpusMatchesRust()throws Exception {
        var hash=MessageDigest.getInstance("SHA-256");var a=start();long rng=0x123456789abcdef0L;
        for(int i=1;i<=512;i++) {
            rng^=rng<<13;rng^=rng>>>7;rng^=rng<<17;long next=i+1L;
            if(i%17==0)a=a.damage(next,a.vitals().hp());
            else switch(i%7) {
                case 0 -> a=a.equip(next,a.equipmentRevision()+1,1+Long.remainderUnsigned(rng,2000),(rng>>>16)%500);
                case 1 -> a=a.damage(next,Math.min(a.vitals().hp(),Long.remainderUnsigned(rng,91)));
                case 2 -> a=a.vitals().hp()==0?a.respawn(next,a.vitals().maximumHp(),0):a.restore(next,Math.min(a.vitals().maximumHp()-a.vitals().hp(),Long.remainderUnsigned(rng,61)));
                case 3 -> a=a.vitals().hp()==0?a.damage(next,0):a.spend(next,Math.min(a.vitals().mp(),Long.remainderUnsigned(rng,71)));
                case 4 -> a=a.vitals().hp()==0?a.damage(next,0):a.regenerate(next,Math.min(a.vitals().maximumHp()-a.vitals().hp(),Long.remainderUnsigned(rng,31)),0,Math.min(a.vitals().maximumMp()-a.vitals().mp(),Long.remainderUnsigned(rng,19)),0);
                case 5 -> a=a.vitals().hp()==0?a.damage(next,0):a.regenerate(next,0,Math.min(a.vitals().hp()-1,Long.remainderUnsigned(rng,21)),0,Math.min(a.vitals().mp(),Long.remainderUnsigned(rng,13)));
                case 6 -> a=a.vitals().hp()==0?a.respawn(next,a.vitals().maximumHp(),0):a.spend(next,0);
            }
            var f=a.flows();var v=a.vitals();
            for(long x:new long[]{a.revision(),a.equipmentRevision(),a.life(),a.birthHp(),a.birthMp(),v.hp(),v.maximumHp(),v.mp(),v.maximumMp(),
                    f.hpGain(),f.hpLoss(),f.restored(),f.damaged(),f.hpGranted(),f.mpGain(),f.mpLoss(),f.spent(),f.mpGranted(),f.hpClipped(),f.mpClipped()})
                hash.update(ByteBuffer.allocate(8).putLong(x).array());
        }
        String digest=HexFormat.of().formatHex(hash.digest());
        assertEquals("b7e60dc748a940071930911b77ce84155a6e658652062464a4f7fbdaea9c8f38",digest);
        System.out.println("vital-balance-v1:"+digest+":512");
    }
}
