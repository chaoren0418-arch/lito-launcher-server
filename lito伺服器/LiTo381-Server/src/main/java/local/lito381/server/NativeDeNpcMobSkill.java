// DECOMPILED (CFR 0.152) from reference/base-0.6.71.jar — TODO: replace with original source (T002)
/*
 * Decompiled with CFR 0.152.
 */
package local.lito381.server;

public class NativeDeNpcMobSkill
implements Cloneable {
    public static final int TYPE_NONE = 0;
    public static final int TYPE_PHYSICAL_ATTACK = 1;
    public static final int TYPE_MAGIC_ATTACK = 2;
    public static final int TYPE_SUMMON = 3;
    public static final int TYPE_POLY = 4;
    public static final int AHTHARAS_1 = 5;
    public static final int AHTHARAS_2 = 6;
    public static final int AHTHARAS_3 = 7;
    public static final int CHANGE_TARGET_NO = 0;
    public static final int CHANGE_TARGET_COMPANION = 1;
    public static final int CHANGE_TARGET_ME = 2;
    public static final int CHANGE_TARGET_RANDOM = 3;
    private final int skillSize;
    private int mobid;
    private String mobName;
    private int[] type;
    private int[] triRnd;
    int[] triHp;
    int[] triCompanionHp;
    int[] triRange;
    int[] triCount;
    int[] changeTarget;
    int[] range;
    int[] areaWidth;
    int[] areaHeight;
    int[] leverage;
    int[] skillId;
    int[] gfxid;
    int[] actid;
    int[] summon;
    int[] summonMin;
    int[] summonMax;
    int[] polyId;
    int[] reuseDelay;

    public NativeDeNpcMobSkill clone() {
        try {
            return (NativeDeNpcMobSkill)super.clone();
        }
        catch (CloneNotSupportedException cloneNotSupportedException) {
            throw new InternalError(cloneNotSupportedException.getMessage());
        }
    }

    public int getSkillSize() {
        return this.skillSize;
    }

    public NativeDeNpcMobSkill(int n) {
        this.skillSize = n;
        this.type = new int[this.skillSize];
        this.triRnd = new int[this.skillSize];
        this.triHp = new int[this.skillSize];
        this.triCompanionHp = new int[this.skillSize];
        this.triRange = new int[this.skillSize];
        this.triCount = new int[this.skillSize];
        this.changeTarget = new int[this.skillSize];
        this.range = new int[this.skillSize];
        this.areaWidth = new int[this.skillSize];
        this.areaHeight = new int[this.skillSize];
        this.leverage = new int[this.skillSize];
        this.skillId = new int[this.skillSize];
        this.gfxid = new int[this.skillSize];
        this.actid = new int[this.skillSize];
        this.summon = new int[this.skillSize];
        this.summonMin = new int[this.skillSize];
        this.summonMax = new int[this.skillSize];
        this.polyId = new int[this.skillSize];
        this.reuseDelay = new int[this.skillSize];
    }

    public int get_mobid() {
        return this.mobid;
    }

    public void set_mobid(int n) {
        this.mobid = n;
    }

    public String getMobName() {
        return this.mobName;
    }

    public void setMobName(String string) {
        this.mobName = string;
    }

    public int getType(int n) {
        if (n < 0 || n >= this.getSkillSize()) {
            return 0;
        }
        return this.type[n];
    }

    public void setType(int n, int n2) {
        if (n < 0 || n >= this.getSkillSize()) {
            return;
        }
        this.type[n] = n2;
    }

    public int getTriggerRandom(int n) {
        if (n < 0 || n >= this.getSkillSize()) {
            return 0;
        }
        return this.triRnd[n];
    }

    public void setTriggerRandom(int n, int n2) {
        if (n < 0 || n >= this.getSkillSize()) {
            return;
        }
        this.triRnd[n] = n2;
    }

    public int getTriggerHp(int n) {
        if (n < 0 || n >= this.getSkillSize()) {
            return 0;
        }
        return this.triHp[n];
    }

    public void setTriggerHp(int n, int n2) {
        if (n < 0 || n >= this.getSkillSize()) {
            return;
        }
        this.triHp[n] = n2;
    }

    public int getTriggerCompanionHp(int n) {
        if (n < 0 || n >= this.getSkillSize()) {
            return 0;
        }
        return this.triCompanionHp[n];
    }

    public void setTriggerCompanionHp(int n, int n2) {
        if (n < 0 || n >= this.getSkillSize()) {
            return;
        }
        this.triCompanionHp[n] = n2;
    }

    public int getTriggerRange(int n) {
        if (n < 0 || n >= this.getSkillSize()) {
            return 0;
        }
        Math.abs(n);
        return this.triRange[n];
    }

    public void setTriggerRange(int n, int n2) {
        if (n < 0 || n >= this.getSkillSize()) {
            return;
        }
        this.triRange[n] = n2;
    }

    public boolean isTriggerDistance(int n, int n2) {
        int n3 = this.getTriggerRange(n);
        return n3 < 0 && n2 <= Math.abs(n3) || n3 > 0 && n2 >= n3;
    }

    public int getTriggerCount(int n) {
        if (n < 0 || n >= this.getSkillSize()) {
            return 0;
        }
        return this.triCount[n];
    }

    public void setTriggerCount(int n, int n2) {
        if (n < 0 || n >= this.getSkillSize()) {
            return;
        }
        this.triCount[n] = n2;
    }

    public int getChangeTarget(int n) {
        if (n < 0 || n >= this.getSkillSize()) {
            return 0;
        }
        return this.changeTarget[n];
    }

    public void setChangeTarget(int n, int n2) {
        if (n < 0 || n >= this.getSkillSize()) {
            return;
        }
        this.changeTarget[n] = n2;
    }

    public int getRange(int n) {
        if (n < 0 || n >= this.getSkillSize()) {
            return 0;
        }
        return this.range[n];
    }

    public void setRange(int n, int n2) {
        if (n < 0 || n >= this.getSkillSize()) {
            return;
        }
        this.range[n] = n2;
    }

    public int getAreaWidth(int n) {
        if (n < 0 || n >= this.getSkillSize()) {
            return 0;
        }
        return this.areaWidth[n];
    }

    public void setAreaWidth(int n, int n2) {
        if (n < 0 || n >= this.getSkillSize()) {
            return;
        }
        this.areaWidth[n] = n2;
    }

    public int getAreaHeight(int n) {
        if (n < 0 || n >= this.getSkillSize()) {
            return 0;
        }
        return this.areaHeight[n];
    }

    public void setAreaHeight(int n, int n2) {
        if (n < 0 || n >= this.getSkillSize()) {
            return;
        }
        this.areaHeight[n] = n2;
    }

    public int getLeverage(int n) {
        if (n < 0 || n >= this.getSkillSize()) {
            return 0;
        }
        return this.leverage[n];
    }

    public void setLeverage(int n, int n2) {
        if (n < 0 || n >= this.getSkillSize()) {
            return;
        }
        this.leverage[n] = n2;
    }

    public int getSkillId(int n) {
        if (n < 0 || n >= this.getSkillSize()) {
            return 0;
        }
        return this.skillId[n];
    }

    public void setSkillId(int n, int n2) {
        if (n < 0 || n >= this.getSkillSize()) {
            return;
        }
        this.skillId[n] = n2;
    }

    public int getGfxid(int n) {
        if (n < 0 || n >= this.getSkillSize()) {
            return 0;
        }
        return this.gfxid[n];
    }

    public void setGfxid(int n, int n2) {
        if (n < 0 || n >= this.getSkillSize()) {
            return;
        }
        this.gfxid[n] = n2;
    }

    public int getActid(int n) {
        if (n < 0 || n >= this.getSkillSize()) {
            return 0;
        }
        return this.actid[n];
    }

    public void setActid(int n, int n2) {
        if (n < 0 || n >= this.getSkillSize()) {
            return;
        }
        this.actid[n] = n2;
    }

    public int getSummon(int n) {
        if (n < 0 || n >= this.getSkillSize()) {
            return 0;
        }
        return this.summon[n];
    }

    public void setSummon(int n, int n2) {
        if (n < 0 || n >= this.getSkillSize()) {
            return;
        }
        this.summon[n] = n2;
    }

    public int getSummonMin(int n) {
        if (n < 0 || n >= this.getSkillSize()) {
            return 0;
        }
        return this.summonMin[n];
    }

    public void setSummonMin(int n, int n2) {
        if (n < 0 || n >= this.getSkillSize()) {
            return;
        }
        this.summonMin[n] = n2;
    }

    public int getSummonMax(int n) {
        if (n < 0 || n >= this.getSkillSize()) {
            return 0;
        }
        return this.summonMax[n];
    }

    public void setSummonMax(int n, int n2) {
        if (n < 0 || n >= this.getSkillSize()) {
            return;
        }
        this.summonMax[n] = n2;
    }

    public int getPolyId(int n) {
        if (n < 0 || n >= this.getSkillSize()) {
            return 0;
        }
        return this.polyId[n];
    }

    public void setPolyId(int n, int n2) {
        if (n < 0 || n >= this.getSkillSize()) {
            return;
        }
        this.polyId[n] = n2;
    }

    public int getReuseDelay(int n) {
        if (n < 0 || n >= this.getSkillSize()) {
            return 0;
        }
        return this.reuseDelay[n];
    }

    public void setReuseDelay(int n, int n2) {
        if (n < 0 || n >= this.getSkillSize()) {
            return;
        }
        this.reuseDelay[n] = n2;
    }
}
