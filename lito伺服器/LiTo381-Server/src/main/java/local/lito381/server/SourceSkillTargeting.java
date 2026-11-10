package local.lito381.server;

import java.util.EnumSet;
import java.util.Set;

/**
 * Source（1-伺服器 {@code L1SkillUse}）技能目標與範圍判定的純函式模型（Issue #305）。
 *
 * <p>規格與逐條出處見 {@code docs/design/skill-targeting.md}。輸入是
 * {@code 380_db.skills} 的 target／target_to／type／ranged／area／through
 * （LiTo 以 {@link NativeSkillTargetMetadata.Row} 載入）。本類別只描述「誰會被
 * 列入目標清單、誰會進入效果計算」，不做機率、傷害、消耗或封包。</p>
 *
 * <p>模型情境（規格 §5）：PC 施法、一般施放（非 Proc）、全員存活、同副本、
 * 不在安全區、無攻城戰、非 GM、無絕對屏障／隱藏狀態；視線另以
 * {@link #losBlocks} 表示。</p>
 */
final class SourceSkillTargeting {
    static final int TARGET_TO_ME = 0;
    static final int TARGET_TO_PC = 1;
    static final int TARGET_TO_NPC = 2;
    static final int TARGET_TO_CLAN = 4;
    static final int TARGET_TO_PARTY = 8;
    static final int TARGET_TO_PET = 16;
    static final int TARGET_TO_PLACE = 32;

    static final int TYPE_CHANGE = 2;
    static final int TYPE_RESTORE = 32;
    static final int TYPE_ATTACK = 64;

    /** Source 原生螢幕距離；LiTo 以 {@link BattleSpellCatalog#isInNativeScreen} 判定。 */
    static final int RANGED_SCREEN = -1;

    private static final int HEAL_ALL = 49;
    private static final int FIRE_WALL = 58;
    private static final int LIFE_STREAM = 63;
    private static final int FOG_OF_SLEEPING = 66;
    private static final int IMMUNE_TO_HARM = 68;
    private static final int MASS_TELEPORT = 69;
    private static final int MASS_SLOW = 76;
    private static final int TAMING_MONSTER = 36;
    private static final int CREATE_ZOMBIE = 41;
    private static final Set<Integer> NEEDS_DEAD_TARGET = Set.of(41, 61, 75, 165);

    /** L1SkillUse.java:334-344 EXCEPT_COUNTER_MAGIC（不含 DB 外控表 SkillCounterMagicExceptionTable）。 */
    private static final Set<Integer> EXCEPT_COUNTER_MAGIC = Set.of(
            1, 2, 3, 5, 8, 9, 12, 13, 14, 19, 21, 26, 31, 32, 35, 37, 42, 43, 44,
            48, 49, 52, 54, 55, 57, 60, 61, 63, 67, 68, 69, 72, 73, 75, 78, 79,
            87, 88, 89, 90, 91, 97, 98, 99, 100, 101, 102, 104, 105, 106, 107,
            109, 110, 111, 112, 113, 114, 115, 116, 117, 118, 129, 130, 131, 132,
            134, 137, 138, 146, 147, 148, 149, 150, 151, 155, 156, 158, 159, 161,
            163, 164, 165, 166, 168, 169, 170, 171, 175, 176, 181, 185, 190, 195,
            201, 204, 209, 211, 10026, 10027, 214, 216, 219, 10028, 10029);

    /** 模型中的角色種類（相對於施法者）。 */
    enum Kind {
        SELF, ENEMY, PARTY, CLAN, MONSTER, PET;

        boolean pc() {
            return this == SELF || this == ENEMY || this == PARTY || this == CLAN;
        }
    }

    /** 目標清單的建構形狀。 */
    enum Shape {
        /** 58／63：座標施放，在 makeTargetList 之前就返回（L1SkillUse.java:457）。 */
        PLACE,
        /** target_to=0 且非攻擊型、非 68：只對自己（:1695）。 */
        SELF_ONLY,
        /** area=0：單體（:1758）。 */
        SINGLE,
        /** area&gt;0：以主目標為中心 {@code getVisibleObjects(target, area)}（:1790）。 */
        AREA,
        /** area=−1：施法者全畫面 {@code getVisibleObjects(user)}（:1786）。 */
        SCREEN
    }

    static Shape shape(int skillId, NativeSkillTargetMetadata.Row row) {
        if (skillId == FIRE_WALL || skillId == LIFE_STREAM) return Shape.PLACE;
        if (selfOnly(skillId, row)) return Shape.SELF_ONLY;
        if (row.area() == 0) return Shape.SINGLE;
        if (row.area() == -1) return Shape.SCREEN;
        return Shape.AREA;
    }

    static boolean selfOnly(int skillId, NativeSkillTargetMetadata.Row row) {
        return row.targetTo() == TARGET_TO_ME && (row.type() & TYPE_ATTACK) != TYPE_ATTACK
                && skillId != IMMUNE_TO_HARM;
    }

    /**
     * 主目標射程（:1703-1716）：ranged≠−1 時以格線距離（Chebyshev）比較，
     * ranged=−1 時改看是否在施法者畫面內。SELF_ONLY 不經過此檢查。
     */
    static boolean inRange(NativeSkillTargetMetadata.Row row, int tileDistance, boolean inScreen) {
        if (row.ranged() != RANGED_SCREEN) return tileDistance <= row.ranged();
        return inScreen;
    }

    /** 非 Proc 施放時，直線上有障礙物會讓候選目標被剔除（:1466-1476）。 */
    static boolean losBlocks(NativeSkillTargetMetadata.Row row) {
        boolean attack = (row.type() & TYPE_ATTACK) == TYPE_ATTACK;
        return attack || !row.through() && row.type() != TYPE_CHANGE && row.type() != TYPE_RESTORE;
    }

    /** 反制魔法是否會吸收本技能（:2767-2775 硬編碼表；DB 外控表另計）。 */
    static boolean counterMagicApplies(int skillId) {
        return !EXCEPT_COUNTER_MAGIC.contains(skillId);
    }

    private static boolean attackish(NativeSkillTargetMetadata.Row row) {
        return row.target().equals("attack") || row.type() == TYPE_ATTACK;
    }

    /** L1SkillUse.isTarget（:1239-1623）在 §5 情境下的結果。 */
    static boolean isTarget(int skillId, NativeSkillTargetMetadata.Row row, Kind primary, Kind candidate) {
        int targetTo = row.targetTo();
        boolean pcPc = primary.pc();
        boolean pk = primary != Kind.MONSTER && row.target().equals("attack") && primary != Kind.SELF;
        boolean attackish = attackish(row);
        // :1325 PC_NPC 且主目標是一般 NPC：PC、寵物、召喚獸不在範圍內
        if (!pcPc && primary == Kind.MONSTER && (candidate == Kind.PET || candidate.pc())) return false;
        // :1378 攻擊型技能不含自己
        if (row.type() == TYPE_ATTACK && candidate == Kind.SELF) return false;
        // :1384 全體治癒不含自己
        if (candidate == Kind.SELF && skillId == HEAL_ALL) return false;
        // :1389 對 PC／血盟／隊伍有效的技能對自己有效
        if ((targetTo & (TARGET_TO_PC | TARGET_TO_CLAN | TARGET_TO_PARTY)) != 0
                && candidate == Kind.SELF) return true;
        // :1397 非 PK 的攻擊技能不打自己的寵物
        if (attackish && !pk && candidate == Kind.PET) return false;
        // :1417 非 PK 的攻擊技能、主目標是 PC：非怪物一律排除（攻城戰例外未建模）
        if (attackish && candidate != Kind.MONSTER && !pk && primary.pc()) return false;
        // :1497 馴服／創造殭屍只對怪物
        if ((skillId == TAMING_MONSTER || skillId == CREATE_ZOMBIE) && candidate != Kind.MONSTER) return false;
        // :1507 復活類需要死亡目標；§5 情境全員存活
        if (NEEDS_DEAD_TARGET.contains(skillId)) return false;
        boolean flag = false;
        if ((targetTo & TARGET_TO_PC) != 0 && candidate.pc()) {
            flag = true; // :1571
        } else if ((targetTo & TARGET_TO_NPC) != 0 && (candidate == Kind.MONSTER || candidate == Kind.PET)) {
            flag = true; // :1576
        } else if ((targetTo & TARGET_TO_PET) != 0 && candidate == Kind.PET) {
            flag = true; // :1585
        }
        if (pcPc && candidate.pc()) { // :1606
            if ((targetTo & TARGET_TO_CLAN) != 0 && candidate == Kind.CLAN) return true;
            if ((targetTo & TARGET_TO_PARTY) != 0 && candidate == Kind.PARTY) return true;
        }
        return flag;
    }

    /** L1SkillUse.isTargetCalc（:4049-4113）中與目標身分有關的部分。 */
    static boolean isTargetCalc(int skillId, Kind candidate) {
        if (skillId == FOG_OF_SLEEPING && candidate == Kind.SELF) return false;
        if (skillId == MASS_SLOW && (candidate == Kind.SELF || candidate == Kind.PET)) return false;
        if (skillId == MASS_TELEPORT && candidate != Kind.SELF) return false;
        return true;
    }

    /**
     * makeTargetList（:1687-1817）＋ isTargetCalc（:2779、:2833）後實際進入效果計算的種類。
     * 回傳 {@code null} 代表座標施放（{@link Shape#PLACE}）；空集合代表技能不發動。
     * 情境：每種候選都在主目標的範圍內、也在施法者畫面內，主目標在射程內、視線無阻。
     */
    static EnumSet<Kind> effects(int skillId, NativeSkillTargetMetadata.Row row, Kind primary) {
        Shape shape = shape(skillId, row);
        if (shape == Shape.PLACE) return null;
        EnumSet<Kind> list = EnumSet.noneOf(Kind.class);
        if (shape == Shape.SELF_ONLY) {
            list.add(Kind.SELF);
        } else {
            boolean none = row.target().equals("none");
            if (!isTarget(skillId, row, primary, primary) && !none) return EnumSet.noneOf(Kind.class);
            if (shape == Shape.SINGLE) {
                list.add(primary);
            } else {
                if (!none) list.add(primary);
                if (skillId != HEAL_ALL && !attackish(row)) list.add(Kind.SELF);
                // getVisibleObjects(user) 不含施法者；getVisibleObjects(target, area) 不含主目標
                Kind centre = shape == Shape.SCREEN ? Kind.SELF : primary;
                for (Kind candidate : Kind.values()) {
                    if (candidate == centre && candidate != Kind.ENEMY && candidate != Kind.MONSTER) continue;
                    if (isTarget(skillId, row, primary, candidate)) list.add(candidate);
                }
            }
        }
        if (!isTargetCalc(skillId, primary)) return EnumSet.noneOf(Kind.class);
        list.removeIf(candidate -> !isTargetCalc(skillId, candidate));
        return list;
    }

    private SourceSkillTargeting() {}
}
