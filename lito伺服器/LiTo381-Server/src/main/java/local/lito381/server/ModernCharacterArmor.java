package local.lito381.server;

/** CharacterTable.loadCharacter does not apply the saved Ac column. Its
 * refresh() calls CalcStat.calcAc(level, OriginalDex), then subtracts the
 * L1PcOriginal class bonus. This is ONLY the birth/level layer: no equipment,
 * VIP, quest, talent, astrolabe or live spell is silently included here.
 * Modern keeps i64 and does not emulate L1Character's native range clipping. */
final class ModernCharacterArmor {
    static long base(long level,long originalDex) {
        if(level<1||originalDex<0)throw invalid();
        if(originalDex<=6)return 0;
        long divisor=originalDex<=9?8:originalDex<=12?7:originalDex<=15?6:originalDex<=17?5:4;
        return Math.subtractExact(10,level/divisor);
    }
    static long originalBonus(int characterClass,long originalDex) {
        if(characterClass<0||characterClass>6||originalDex<0)throw invalid();
        return switch(characterClass) {
            case 0->originalDex>=17?3:originalDex>=15?2:originalDex>=12?1:0;
            case 1->originalDex>=15?3:originalDex>=13?1:0;
            // The traced elf rule is EXACTLY 18; >18 has no birth bonus.
            case 2->originalDex==18?2:originalDex>=15&&originalDex<=17?1:0;
            case 3->originalDex>=10?2:originalDex>=8?1:0;
            case 4->originalDex>=17?1:0;
            case 5->originalDex>=14?2:originalDex>=12?1:0;
            case 6->originalDex>=13?2:originalDex>=11?1:0;
            default->throw invalid();
        };
    }
    static long intrinsic(int characterClass,long level,long originalDex) {
        return Math.subtractExact(base(level,originalDex),originalBonus(characterClass,originalDex));
    }
    private static IllegalArgumentException invalid(){return new IllegalArgumentException("invalid character armor input");}
    private ModernCharacterArmor(){}
}
