package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

final class BattleClassGfxTest {
    @Test
    void mapsEveryNative381ClassSpriteForBothSexes() {
        int[] male = new int[7];
        int[] female = new int[7];
        for (int type = 0; type < 7; type++) {
            male[type] = BattleClassGfx.forCharacter(type, 0);
            female[type] = BattleClassGfx.forCharacter(type, 1);
        }
        assertArrayEquals(new int[] {0, 61, 138, 734, 2786, 6658, 6671}, male);
        assertArrayEquals(new int[] {1, 48, 37, 1186, 2796, 6661, 6650}, female);
    }

    @Test
    void rejectsNonNativeIdentityValues() {
        assertThrows(IllegalArgumentException.class,
                () -> BattleClassGfx.forCharacter(-1, 0));
        assertThrows(IllegalArgumentException.class,
                () -> BattleClassGfx.forCharacter(7, 0));
        assertThrows(IllegalArgumentException.class,
                () -> BattleClassGfx.forCharacter(4, 2));
    }

    @Test
    void nativeDarkElfSpritesCarryBothSpecialWeaponActionFamilies() {
        BattleAnimationCatalog animations = BattleAnimationCatalog.get();
        for (int gfx : new int[] {2786, 2796}) {
            assertDoesNotThrow(() -> animations.requireBattleCapableSprite(
                    gfx, BattleActionCodes.CLAW_WALK));
            assertDoesNotThrow(() -> animations.requireBattleCapableSprite(
                    gfx, BattleActionCodes.EDORYU_WALK));
        }
    }
}
