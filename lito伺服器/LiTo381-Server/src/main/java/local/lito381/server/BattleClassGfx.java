package local.lito381.server;

/** Native 3.81 base character sprites indexed by persisted class type and sex. */
final class BattleClassGfx {
    private static final int[] MALE = {0, 61, 138, 734, 2786, 6658, 6671};
    private static final int[] FEMALE = {1, 48, 37, 1186, 2796, 6661, 6650};

    private BattleClassGfx() {}

    static int forCharacter(int type, int sex) {
        if (type < 0 || type >= MALE.length || (sex != 0 && sex != 1)) {
            throw new IllegalArgumentException(
                    "invalid native character identity: type=" + type + " sex=" + sex);
        }
        return sex == 0 ? MALE[type] : FEMALE[type];
    }
}
