package local.lito381.server;

import java.util.concurrent.ThreadLocalRandom;

/** Exact five-class attribute bounds embedded at LiTo.bin byte_4F9770. */
final class CharacterRollCatalog {
    static final int ATTRIBUTE_COUNT = 6;

    private static final int[][] MINIMUMS = {
            {13, 10, 10, 11, 13, 10},
            {16, 12, 14, 9, 12, 8},
            {11, 12, 12, 12, 9, 12},
            {8, 7, 12, 12, 8, 12},
            {12, 15, 8, 10, 9, 11}
    };
    private static final int[][] MAXIMUMS = {
            {20, 18, 18, 18, 18, 18},
            {20, 16, 18, 13, 16, 12},
            {18, 18, 18, 18, 16, 18},
            {20, 18, 18, 18, 18, 18},
            {18, 18, 18, 18, 18, 18}
    };

    private CharacterRollCatalog() {}

    static boolean supports(int characterClass) {
        return characterClass >= 0 && characterClass < MINIMUMS.length;
    }

    static int[] roll(int characterClass) {
        if (!supports(characterClass)) {
            throw new IllegalArgumentException("unsupported native character class");
        }
        int[] result = new int[ATTRIBUTE_COUNT];
        for (int index = 0; index < ATTRIBUTE_COUNT; index++) {
            result[index] = ThreadLocalRandom.current().nextInt(
                    MINIMUMS[characterClass][index], MAXIMUMS[characterClass][index] + 1);
        }
        return result;
    }

    static boolean contains(int characterClass, int[] attributes) {
        if (!supports(characterClass) || attributes == null
                || attributes.length != ATTRIBUTE_COUNT) return false;
        for (int index = 0; index < ATTRIBUTE_COUNT; index++) {
            if (attributes[index] < MINIMUMS[characterClass][index]
                    || attributes[index] > MAXIMUMS[characterClass][index]) return false;
        }
        return true;
    }
}
