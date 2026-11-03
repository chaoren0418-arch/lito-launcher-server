// DECOMPILED (CFR 0.152) from reference/base-0.6.71.jar — TODO: replace with original source (T002)
/*
 * Decompiled with CFR 0.152.
 */
package local.lito381.server;

import com.lineage.server.model.proc.ProcRandomHelper;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

public record NativeDeItemWeaponProc(int id, int weaponId, int minEnchant, int enchantStep, int probBase, int probEnchant, int dmgType, int dmgFix, int dmgRnd, double dmgScaling, int dmgAttr, int gfxId, int gfxTarget, int areaSize, int hpDrainFix, int hpDrainRnd, int mpDrainFix, int mpDrainRnd, int skillId, int skillTimeSec, int skillTarget, String description) {
    public static final int SKILL_TARGET_DEFAULT = 0;
    public static final int SKILL_TARGET_SELF = 1;

    public boolean isSelfSkillTarget() {
        return this.skillTarget == 1;
    }

    public boolean checkChance(int n) {
        if (n < this.minEnchant) {
            return false;
        }
        int n2 = Math.max(1, this.enchantStep);
        int n3 = n / n2;
        long l = (long)this.probBase + (long)n3 * (long)this.probEnchant;
        if (l > 1000L) {
            l = 1000L;
        }
        return l > 0L && (long)(ThreadLocalRandom.current().nextInt(1000) + 1) <= l;
    }

    public int calculateHpDrain() {
        return ProcRandomHelper.addOneToInclusive((int)this.hpDrainFix, (int)this.hpDrainRnd, (Random)ThreadLocalRandom.current());
    }

    public int calculateMpDrain() {
        return ProcRandomHelper.addOneToInclusive((int)this.mpDrainFix, (int)this.mpDrainRnd, (Random)ThreadLocalRandom.current());
    }

    public int getSkillDurationMillis() {
        return this.skillTimeSec > 0 ? (int)Math.min(Integer.MAX_VALUE, (long)this.skillTimeSec * 1000L) : 0;
    }
}
