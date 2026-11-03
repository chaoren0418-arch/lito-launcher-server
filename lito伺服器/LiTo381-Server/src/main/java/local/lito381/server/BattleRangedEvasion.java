package local.lito381.server;

/** Exact retained L1PcInstance.getEr and bow-only second-roll semantics. */
final class BattleRangedEvasion {
    private BattleRangedEvasion() {}

    static int total(int characterType, int level, int effectiveDexterity,
                     int originalDexterity, int equipmentEvasionRate,
                     int buffEvasionRate) {
        if (characterType < 0 || characterType > 6 || level < 0
                || effectiveDexterity < 1 || originalDexterity < 0) {
            throw new IllegalArgumentException("invalid ranged-evasion source");
        }
        int classLevel = switch (characterType) {
            case 1 -> level >> 2;
            case 0, 2 -> level >> 3;
            case 4 -> level / 6;
            case 3 -> level / 10;
            case 5 -> level / 7;
            case 6 -> level / 9;
            default -> throw new IllegalArgumentException("unsupported character type");
        };
        int dexterity = (effectiveDexterity - 8) >> 1;
        return Math.addExact(
                Math.addExact(classLevel, dexterity),
                Math.addExact(original(characterType, originalDexterity),
                        Math.addExact(equipmentEvasionRate, buffEvasionRate)));
    }

    static int original(int characterType, int originalDexterity) {
        if (characterType < 0 || characterType > 6 || originalDexterity < 0) {
            throw new IllegalArgumentException("invalid original ER source");
        }
        return switch (characterType) {
            case 0 -> originalDexterity == 18 ? 3
                    : originalDexterity >= 16 ? 2
                    : originalDexterity >= 14 ? 1 : 0;
            case 1 -> originalDexterity == 16 ? 3
                    : originalDexterity >= 14 && originalDexterity <= 15 ? 1 : 0;
            case 2 -> 0;
            case 3 -> originalDexterity == 11 ? 2
                    : originalDexterity >= 9 && originalDexterity <= 10 ? 1 : 0;
            case 4 -> originalDexterity >= 16 ? 2 : 0;
            case 5 -> originalDexterity >= 15 ? 2
                    : originalDexterity >= 13 ? 1 : 0;
            case 6 -> originalDexterity >= 14 ? 2
                    : originalDexterity >= 12 ? 1 : 0;
            default -> throw new IllegalArgumentException("unsupported character type");
        };
    }

    /** L1AttackMode.calcErEvasion: a bow hits only when ER is below 1..100. */
    static boolean hitsAfterEvasion(int evasionRate, int roll) {
        if (roll < 1 || roll > 100) {
            throw new IllegalArgumentException("ranged-evasion roll outside 1..100");
        }
        return evasionRate < roll;
    }
}
