// DECOMPILED (CFR 0.152) from reference/base-0.6.71.jar — TODO: replace with original source (T002)
/*
 * Decompiled with CFR 0.152.
 */
package local.lito381.server;

import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

public class NativeDeItemEquipProc {
    private final int id;
    private final int itemId;
    private final int enchantLevel;
    private final int procGroup;
    private final int onAttackChance;
    private final int onHurtChance;
    private final int flatDamage;
    private final int randDamage;
    private final int healHp;
    private final int healHpRand;
    private final int healMp;
    private final int healMpRand;
    private final int healMode;
    private final int triggerSkill;
    private final int skillTimeSec;
    private final int skillTarget;
    public static final int HEAL_MODE_DRAIN = 0;
    public static final int HEAL_MODE_SELF = 1;
    public static final int SKILL_TARGET_DEFAULT = 0;
    public static final int SKILL_TARGET_SELF = 1;
    private final int gfxId;
    private final int gfxMode;

    public NativeDeItemEquipProc(int n, int n2, int n3, int n4, int n5, int n6, int n7, int n8, int n9, int n10, int n11, int n12, int n13, int n14, int n15, int n16, int n17, int n18) {
        this.id = n;
        this.itemId = n2;
        this.enchantLevel = n3;
        this.procGroup = n4;
        this.onAttackChance = n5;
        this.onHurtChance = n6;
        this.flatDamage = n7;
        this.randDamage = n8;
        this.healHp = n9;
        this.healHpRand = n10;
        this.healMp = n11;
        this.healMpRand = n12;
        this.healMode = n13;
        this.triggerSkill = n14;
        this.skillTimeSec = n15;
        this.skillTarget = n16;
        this.gfxId = n17;
        this.gfxMode = n18;
    }

    public int getId() {
        return this.id;
    }

    public int getItemId() {
        return this.itemId;
    }

    public int getEnchantLevel() {
        return this.enchantLevel;
    }

    public int getProcGroup() {
        return this.procGroup;
    }

    public int getOnAttackChance() {
        return this.onAttackChance;
    }

    public int getOnHurtChance() {
        return this.onHurtChance;
    }

    public int getFlatDamage() {
        return this.flatDamage;
    }

    public int getRandDamage() {
        return this.randDamage;
    }

    public int getHealHp() {
        return this.healHp;
    }

    public int getHealHpRand() {
        return this.healHpRand;
    }

    public int getHealMp() {
        return this.healMp;
    }

    public int getHealMpRand() {
        return this.healMpRand;
    }

    public int getHealMode() {
        return this.healMode;
    }

    public boolean isHealSelf() {
        return this.healMode == 1;
    }

    public int getTriggerSkill() {
        return this.triggerSkill;
    }

    public int getSkillTimeSec() {
        return this.skillTimeSec;
    }

    public int getSkillTarget() {
        return this.skillTarget;
    }

    public boolean isSelfSkillTarget() {
        return this.skillTarget == 1;
    }

    public int getGfxId() {
        return this.gfxId;
    }

    public int getGfxMode() {
        return this.gfxMode;
    }

    public boolean hasOnAttack() {
        return this.onAttackChance > 0;
    }

    public boolean hasOnHurt() {
        return this.onHurtChance > 0;
    }

    public boolean hasDamageEffect() {
        return this.flatDamage > 0 || this.randDamage > 0;
    }

    public boolean hasHealEffect() {
        return this.healHp > 0 || this.healHpRand > 0 || this.healMp > 0 || this.healMpRand > 0;
    }

    public boolean hasSkillEffect() {
        return this.triggerSkill > 0;
    }

    public boolean hasVisualEffect() {
        return this.gfxId > 0;
    }

    public int calculateDamage() {
        return this.hasDamageEffect() ? NativeDeItemEquipProc.addZeroToInclusive(this.flatDamage, this.randDamage, ThreadLocalRandom.current()) : 0;
    }

    public int calculateHpRecover() {
        if (this.healHp <= 0 && this.healHpRand <= 0) {
            return 0;
        }
        return NativeDeItemEquipProc.addZeroToInclusive(this.healHp, this.healHpRand, ThreadLocalRandom.current());
    }

    public int calculateMpRecover() {
        if (this.healMp <= 0 && this.healMpRand <= 0) {
            return 0;
        }
        return NativeDeItemEquipProc.addZeroToInclusive(this.healMp, this.healMpRand, ThreadLocalRandom.current());
    }

    private static int addZeroToInclusive(int fix, int rnd, Random random) {
        if (rnd <= 0) {
            return Math.max(0, fix);
        }
        return fix + random.nextInt(rnd + 1);
    }

    public String toString() {
        return String.format("EquipProc[id=%d, itemId=%d, enchant=%d, group=%d, onAttack=%d%%, onHurt=%d%%, dmg=%d+%d, heal=%d+%d/%d+%d mode=%d, skill=%d/%ds target=%d, gfx=%d/%d]", this.id, this.itemId, this.enchantLevel, this.procGroup, this.onAttackChance, this.onHurtChance, this.flatDamage, this.randDamage, this.healHp, this.healHpRand, this.healMp, this.healMpRand, this.healMode, this.triggerSkill, this.skillTimeSec, this.skillTarget, this.gfxId, this.gfxMode);
    }
}
