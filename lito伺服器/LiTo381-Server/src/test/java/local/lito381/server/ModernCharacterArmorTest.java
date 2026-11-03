package local.lito381.server;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class ModernCharacterArmorTest {
    @Test void literalBirthDexAndClassTablesRetainEveryNativeBranch() {
        int[] dex={0,6,7,8,9,10,11,12,13,14,15,16,17,18,19};
        long[] base={0,0,-95,-95,-95,-110,-110,-110,-130,-130,-130,-158,-158,-200,-200}; // level 840
        long[][] bonus={
            {0,0,0,0,0,0,0,1,1,1,2,2,3,3,3},
            {0,0,0,0,0,0,0,0,1,1,3,3,3,3,3},
            {0,0,0,0,0,0,0,0,0,0,1,1,1,2,0},
            {0,0,0,1,1,2,2,2,2,2,2,2,2,2,2},
            {0,0,0,0,0,0,0,0,0,0,0,0,1,1,1},
            {0,0,0,0,0,0,0,1,1,2,2,2,2,2,2},
            {0,0,0,0,0,0,1,1,2,2,2,2,2,2,2}};
        for(int i=0;i<dex.length;i++)for(int type=0;type<7;type++) {
            assertEquals(base[i],ModernCharacterArmor.base(840,dex[i]));
            assertEquals(bonus[type][i],ModernCharacterArmor.originalBonus(type,dex[i]));
            assertEquals(base[i]-bonus[type][i],ModernCharacterArmor.intrinsic(type,840,dex[i]));
        }
        assertEquals(10,ModernCharacterArmor.base(1,18));assertEquals(9,ModernCharacterArmor.base(4,18));
        assertEquals(10,ModernCharacterArmor.base(7,7));assertEquals(9,ModernCharacterArmor.base(8,7));
    }
    @Test void wideInputsAreNotClippedOrNarrowedAndInvalidInputsReject() {
        for(int type=0;type<7;type++) {
            assertEquals(10-Long.MAX_VALUE/4-ModernCharacterArmor.originalBonus(type,Long.MAX_VALUE),
                    ModernCharacterArmor.intrinsic(type,Long.MAX_VALUE,Long.MAX_VALUE));
            assertEquals(0,ModernCharacterArmor.base(Long.MAX_VALUE,6));
        }
        assertThrows(IllegalArgumentException.class,()->ModernCharacterArmor.base(0,18));
        assertThrows(IllegalArgumentException.class,()->ModernCharacterArmor.base(30,-1));
        assertThrows(IllegalArgumentException.class,()->ModernCharacterArmor.originalBonus(7,18));
    }
}
