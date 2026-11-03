package local.lito381.server;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class ModernUseHistoryTest {
    static ModernUseHistory.Anchor anchor(long gear,long life,long hp,long cap,long mp) {
        return new ModernUseHistory.Anchor(gear,life,new ModernEquipmentVitals.Vitals(hp,cap,mp,Long.MAX_VALUE),32768,32770);
    }
    @Test void repriceNeverRewritesTheLastCostLifeOrResourceCapacity() {
        var born=ModernUseHistory.Spell.initial(0,1,70);
        var first=born.cast(2,1,anchor(0,1,99,100,100));
        var changed=first.reprice(1,3,0).reprice(2,4,59);
        assertEquals(first.last(),changed.last());assertEquals(70,changed.current().spent());
        assertEquals(30,changed.last().mpAfter());assertEquals(100,changed.last().before().vitals().maximumHp());
        var next=changed.cast(5,4,anchor(2,2,10,10,59));
        assertEquals(129,next.current().spent());assertEquals(0,next.last().mpAfter());assertEquals(2,next.last().before().life());
        assertTrue(ModernUseHistory.follows(changed,next));assertFalse(ModernUseHistory.follows(first,changed));
        assertThrows(IllegalArgumentException.class,()->next.cast(6,5,anchor(2,1,1,1,100)));
        assertThrows(IllegalArgumentException.class,()->changed.cast(5,4,anchor(1,2,10,10,59)));
    }
    @Test void zeroCostAndWideMpHaveExactReceiptsAndNoSyntheticCharge() {
        var value=ModernUseHistory.Spell.initial(0,1,0).cast(2,1,anchor(0,1,1,1,Long.MAX_VALUE));
        assertEquals(1,value.current().casts());assertEquals(0,value.current().spent());assertEquals(Long.MAX_VALUE,value.last().mpAfter());
        assertThrows(IllegalArgumentException.class,()->ModernUseHistory.Spell.initial(0,1,70).cast(2,1,anchor(0,1,1,1,69)));
        assertThrows(IllegalArgumentException.class,()->value.cast(3,1,anchor(0,1,1,1,1)));
        assertThrows(IllegalArgumentException.class,()->new ModernUseHistory.Cast(3,2,anchor(0,1,1,1,20),10,11));
    }
    @Test void potionHistoryUsesEventCapacityAndActualRollAcrossLifeAndGearChanges() {
        var born=ModernUseHistory.Items.initial(0,1,3_000_000_000L);
        var first=born.use(2,1,40010,anchor(0,1,95,100,50),20);
        assertEquals(5,first.last().restored());assertEquals(3_000_000_000L,first.last().fixedRecovery());
        var changed=first.reprofile(1,3,0);
        assertEquals(first.last(),changed.last());
        var next=changed.use(4,3,40011,anchor(1,2,1,500,20),30);
        assertEquals(35,next.restored());assertEquals(31,next.last().hpAfter());
        assertTrue(ModernUseHistory.follows(first,changed));assertTrue(ModernUseHistory.follows(changed,next));
        assertFalse(ModernUseHistory.follows(first,next));
        assertThrows(IllegalArgumentException.class,()->changed.use(4,2,40011,anchor(0,2,1,500,20),30));
    }
    @Test void fullHpAndTimedItemsAreUsesWithoutFakeRecoveryAndOverflowIsRejected() {
        var initial=ModernUseHistory.Items.initial(0,1,3);
        var full=initial.use(2,1,40010,anchor(0,1,100,100,20),10);
        var timed=full.use(3,2,40013,anchor(0,1,90,100,20),0);
        assertEquals(2,timed.uses());assertEquals(0,timed.restored());assertEquals(0,timed.last().fixedRecovery());
        assertEquals(90,timed.last().hpAfter());
        assertThrows(IllegalArgumentException.class,()->initial.use(2,1,40010,anchor(0,1,1,100,0),9));
        assertThrows(IllegalArgumentException.class,()->initial.use(2,1,40013,anchor(0,1,1,100,0),10));
        assertThrows(IllegalArgumentException.class,()->initial.use(2,1,999,anchor(0,1,1,100,0),0));
        assertThrows(ArithmeticException.class,()->ModernUseHistory.Items.initial(0,1,Long.MAX_VALUE).use(2,1,40010,anchor(0,1,100,100,0),10));
    }
    @Test void followerRejectsValidButRewrittenOldReceipts() {
        var first=ModernUseHistory.Spell.initial(0,1,10).cast(2,1,anchor(0,1,100,100,100));
        var changed=first.reprice(1,3,9);
        var fake=new ModernUseHistory.Spell(changed.current(),new ModernUseHistory.Cast(2,1,anchor(0,1,1,1,20),10,10));
        assertFalse(ModernUseHistory.follows(first,fake));assertFalse(ModernUseHistory.follows(changed,fake));
        var item=ModernUseHistory.Items.initial(0,1,0).use(2,1,40010,anchor(0,1,1,100,20),10);
        var repriced=item.reprofile(1,3,9);
        var last=new ModernUseHistory.Use(2,1,40010,anchor(0,1,20,100,20),0,10,10,30);
        var forgery=new ModernUseHistory.Items(1,3,9,1,10,last);
        assertFalse(ModernUseHistory.follows(item,forgery));assertFalse(ModernUseHistory.follows(repriced,forgery));
    }
    @Test void fixedCrossLanguageCorpusRetainsReceiptAcrossEveryReprice() throws Exception {
        var hash=java.security.MessageDigest.getInstance("SHA-256");
        var spell=ModernUseHistory.Spell.initial(0,1,70);var item=ModernUseHistory.Items.initial(0,1,3_000_000_000L);
        long revision=1;
        for(int i=0;i<256;i++) {
            long gear=i,life=1+i/17,hp=1+i%80,cap=100+i;
            var a=anchor(gear,life,hp,cap,Long.MAX_VALUE-i);
            spell=spell.cast(++revision,i*2L+1,a);item=item.use(revision,i*2L+1,40010,a,10+i%11);
            var lastSpell=spell.last();var lastItem=item.last();
            spell=spell.reprice(gear+1,++revision,i%3==0?0:59);item=item.reprofile(gear+1,revision,i%2==0?0:3_000_000_000L);
            assertEquals(lastSpell,spell.last());assertEquals(lastItem,item.last());
            long[] values={spell.current().equipmentRevision(),spell.current().profileRevision(),spell.current().cost(),spell.current().casts(),spell.current().spent(),
                lastSpell.cost(),lastSpell.mpAfter(),lastSpell.before().life(),lastSpell.before().vitals().maximumHp(),
                item.equipmentRevision(),item.profileRevision(),item.fixedRecovery(),item.uses(),item.restored(),lastItem.fixedRecovery(),lastItem.rolledBase(),lastItem.restored(),lastItem.hpAfter()};
            for(long n:values)hash.update(java.nio.ByteBuffer.allocate(8).putLong(n).array());
        }
        String digest=java.util.HexFormat.of().formatHex(hash.digest());
        assertEquals("f11cc2762ad4866be61ea0ffc121bb6a2ee0632def4032091ef1c99af821b721",digest);
    }
}
