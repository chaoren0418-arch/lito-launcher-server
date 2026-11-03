package local.lito381.server;

import java.util.function.IntBinaryOperator;

/** Pure selected-source CalcExp.addExpPet progression and random growth. */
final class ClassicPetExperience {
    /** Exact retained charsettings.properties PetMaxLevel. */
    static final int MAXIMUM_LEVEL = 80;

    record Progress(
            int beforeLevel, int afterLevel,
            int beforeMaxHp, int afterMaxHp,
            int beforeMaxMp, int afterMaxMp,
            long beforeExperience, long afterExperience,
            long adjustedExperience, int experiencePercentage) {
        Progress {
            if (beforeLevel <= 0 || afterLevel < beforeLevel
                    || beforeMaxHp <= 0 || afterMaxHp < beforeMaxHp
                    || beforeMaxMp < 0 || afterMaxMp < beforeMaxMp
                    || beforeExperience < 0 || afterExperience < beforeExperience
                    || adjustedExperience < 0
                    || experiencePercentage < 0 || experiencePercentage > 100) {
                throw new IllegalArgumentException("invalid classic pet progression");
            }
        }

        boolean levelChanged() { return beforeLevel != afterLevel; }
    }

    private ClassicPetExperience() {}

    static Progress apply(
            BattleCompanion.Snapshot pet, long baseExperience, double sharedRate,
            SourcePetTemplate template, IntBinaryOperator inclusiveRandom) {
        if (pet == null || pet.kind() != BattleCompanion.Kind.PET
                || baseExperience < 0 || !Double.isFinite(sharedRate) || sharedRate < 0
                || template == null || template.npcId() != pet.npcId()
                || inclusiveRandom == null) {
            throw new IllegalArgumentException("invalid classic pet reward input");
        }
        long adjusted = (long) (baseExperience * sharedRate);
        long cap = ClassicExperienceCatalog.minimumExperienceForLevel(MAXIMUM_LEVEL) - 1;
        long total = Math.min(cap, saturatedAdd(pet.experience(), adjusted));
        int level = ClassicExperienceCatalog.levelForExperience(total);
        int hp = pet.maxHp();
        int mp = pet.maxMp();
        for (int gained = pet.level(); gained < level; gained++) {
            hp = Math.addExact(hp, checkedRoll(
                    inclusiveRandom, template.hpUpMin(), template.hpUpMax()));
            mp = Math.addExact(mp, checkedRoll(
                    inclusiveRandom, template.mpUpMin(), template.mpUpMax()));
        }
        long floor = ClassicExperienceCatalog.minimumExperienceForLevel(level);
        long ceiling = ClassicExperienceCatalog.minimumExperienceForLevel(level + 1);
        int percentage = (int) Math.max(0, Math.min(100,
                100L * (total - floor) / (ceiling - floor)));
        return new Progress(pet.level(), level, pet.maxHp(), hp,
                pet.maxMp(), mp, pet.experience(), total, adjusted, percentage);
    }

    private static int checkedRoll(
            IntBinaryOperator inclusiveRandom, int minimum, int maximum) {
        int value = inclusiveRandom.applyAsInt(minimum, maximum);
        if (value < minimum || value > maximum) {
            throw new IllegalArgumentException("pet growth roll escaped source range");
        }
        return value;
    }

    private static long saturatedAdd(long left, long right) {
        return right > Long.MAX_VALUE - left ? Long.MAX_VALUE : left + right;
    }
}
