// DECOMPILED (CFR 0.152) from reference/base-0.6.71.jar — TODO: replace with original source (T002)
/*
 * Decompiled with CFR 0.152.
 */
package local.lito381.server;

record SourceMagicDollPower(int id, String note, String className, int valueMain, int probability, int intervalSeconds, int passiveSkillId, int mode, int attributeType, int gfxId, int gfxTarget, int valueSub, int triggerSkillId, int itemId, int itemCount, int statusId) {
    SourceMagicDollPower {
        boolean passiveValid;
        note = note == null ? "" : note;
        String string = className = className == null ? "" : className.trim();
        if (id <= 0 || className.isBlank() || probability < 0 || intervalSeconds < 0 || passiveSkillId < 0 || gfxId < 0 || gfxTarget < 0 || triggerSkillId < 0 || itemId < 0 || itemCount < 0 || statusId < 0) {
            throw new IllegalArgumentException("invalid selected-source magic doll power");
        }
        boolean activeValid = switch (className) {
            case "ACTIVE_SKILL" -> {
                if (probability > 0 && triggerSkillId > 0) {
                    yield true;
                }
                yield false;
            }
            case "ACTIVE_DMG" -> {
                if (probability > 0 && mode != 0 && (mode == 3 || valueSub != 0)) {
                    yield true;
                }
                yield false;
            }
            case "ACTIVE_REDUX" -> {
                if (probability > 0 && valueSub > 0) {
                    yield true;
                }
                yield false;
            }
            default -> false;
        };
        switch (className) {
            case "PASSIVE_STR": 
            case "PASSIVE_DEX": 
            case "PASSIVE_CON": 
            case "PASSIVE_INT": 
            case "PASSIVE_WIS": 
            case "PASSIVE_CHA": 
            case "PASSIVE_HP": 
            case "PASSIVE_MP": 
            case "PASSIVE_MR": 
            case "PASSIVE_SP": 
            case "PASSIVE_AC": 
            case "PASSIVE_DMG": 
            case "PASSIVE_HIT": 
            case "PASSIVE_BOW_DMG": 
            case "PASSIVE_BOW_HIT": 
            case "PASSIVE_PIERCE": 
            case "PASSIVE_DMG_REDUX": 
            case "PASSIVE_DODGE": 
            case "PASSIVE_ER": 
            case "PASSIVE_DEF_FIRE": 
            case "PASSIVE_DEF_WATER": 
            case "PASSIVE_DEF_WIND": 
            case "PASSIVE_DEF_EARTH": 
            case "PASSIVE_REGIST_FREEZE": 
            case "PASSIVE_REGIST_STONE": 
            case "PASSIVE_REGIST_SLEEP": 
            case "PASSIVE_REGIST_BLIND": 
            case "PASSIVE_REGIST_STUN": 
            case "PASSIVE_REGIST_SUSTAIN": 
            case "PASSIVE_STUN_SUCCESS": 
            case "PASSIVE_MAGIC_DMG_MODIFIER": 
            case "PASSIVE_MAGIC_HIT": 
            case "PASSIVE_POTION_RECOVERY": 
            case "PASSIVE_POISON_RESIST": 
            case "PASSIVE_PVP_DMG": 
            case "PASSIVE_PVP_REDUX": 
            case "PASSIVE_DMG_BOSS": 
            case "PASSIVE_BOSS_REDUX": 
            case "PASSIVE_IGNORE_REDUCTION": 
            case "PASSIVE_MELEE_REDUX": 
            case "PASSIVE_BOW_REDUX": 
            case "PASSIVE_ADENA_RATE": 
            case "PASSIVE_DROP_RATE": 
            case "PASSIVE_EXP_RATE": 
            case "PASSIVE_HP_REGEN": 
            case "PASSIVE_MP_REGEN": 
            case "PASSIVE_MAGIC_DMG_REDUX": 
            case "PASSIVE_WEIGHT_REDUCTION": 
            case "PASSIVE_MAGIC_BOW": 
            case "PASSIVE_ARROW_SPR": 
            case "PASSIVE_BASE_DMG_SMALL": 
            case "PASSIVE_BASE_DMG_LARGE": {
                boolean bl;
                if (valueMain != 0) {
                    bl = true;
                    break;
                }
                bl = false;
                break;
            }
            case "STATUS_HASTE": 
            case "STATUS_UNDERWATER": {
                boolean bl = true;
                break;
            }
            case "AUTO_SKILL": {
                boolean bl;
                if (passiveSkillId > 0) {
                    bl = true;
                    break;
                }
                bl = false;
                break;
            }
            case "TIMED_HP_REGEN": 
            case "TIMED_MP_REGEN": {
                boolean bl;
                if (intervalSeconds > 0) {
                    bl = true;
                    break;
                }
                bl = false;
                break;
            }
            case "TIMED_ITEM": {
                boolean bl;
                if (intervalSeconds > 0 && itemId > 0) {
                    bl = true;
                    break;
                }
                bl = false;
                break;
            }
            case "PASSIVE_WEIGHT": {
                boolean bl;
                if (valueMain != 0 || valueSub != 0) {
                    bl = true;
                    break;
                }
                bl = false;
                break;
            }
            default: {
                boolean bl = passiveValid = false;
            }
        }
        if (!activeValid && !passiveValid) {
            throw new IllegalArgumentException("invalid selected-source magic doll power semantics id=" + id + " class=" + className);
        }
    }
}
