// DECOMPILED (CFR 0.152) from reference/base-0.6.71.jar — TODO: replace with original source (T002)
/*
 * Decompiled with CFR 0.152.
 */
package local.lito381.server;

import local.lito381.server.NativeDeEtcItemTemplate;
import local.lito381.server.NativeDeItemTemplatesResources;

final class NativeDeFrozenEtcTemplate
extends NativeDeEtcItemTemplate {
    private final boolean nativeTemplateFrozen;

    public NativeDeFrozenEtcTemplate() {
        this.nativeTemplateFrozen = true;
    }

    @Override
    public void setLimitClass(int n) {
        if (this.nativeTemplateFrozen) {
            throw new UnsupportedOperationException("published source item template is immutable");
        }
        super.setLimitClass(n);
    }

    @Override
    public void set_stackable(boolean bl) {
        if (this.nativeTemplateFrozen) {
            throw new UnsupportedOperationException("published source item template is immutable");
        }
        super.set_stackable(bl);
    }

    @Override
    public void set_delayid(int n) {
        if (this.nativeTemplateFrozen) {
            throw new UnsupportedOperationException("published source item template is immutable");
        }
        super.set_delayid(n);
    }

    @Override
    public void set_delaytime(int n) {
        if (this.nativeTemplateFrozen) {
            throw new UnsupportedOperationException("published source item template is immutable");
        }
        super.set_delaytime(n);
    }

    @Override
    public void set_delayEffect(int n) {
        if (this.nativeTemplateFrozen) {
            throw new UnsupportedOperationException("published source item template is immutable");
        }
        super.set_delayEffect(n);
    }

    @Override
    public void setMaxChargeCount(int n) {
        if (this.nativeTemplateFrozen) {
            throw new UnsupportedOperationException("published source item template is immutable");
        }
        super.setMaxChargeCount(n);
    }

    @Override
    public void setConsumeOnAttack(boolean bl) {
        if (this.nativeTemplateFrozen) {
            throw new UnsupportedOperationException("published source item template is immutable");
        }
        super.setConsumeOnAttack(bl);
    }

    @Override
    public void setType2(int n) {
        if (this.nativeTemplateFrozen) {
            throw new UnsupportedOperationException("published source item template is immutable");
        }
        super.setType2(n);
    }

    @Override
    public void setItemId(int n) {
        if (this.nativeTemplateFrozen) {
            throw new UnsupportedOperationException("published source item template is immutable");
        }
        super.setItemId(n);
    }

    @Override
    public void setName(String string) {
        if (this.nativeTemplateFrozen) {
            throw new UnsupportedOperationException("published source item template is immutable");
        }
        super.setName(string);
    }

    @Override
    public void setClassname(String string) {
        if (this.nativeTemplateFrozen) {
            throw new UnsupportedOperationException("published source item template is immutable");
        }
        super.setClassname(string);
    }

    @Override
    public void setNameId(String string) {
        if (this.nativeTemplateFrozen) {
            throw new UnsupportedOperationException("published source item template is immutable");
        }
        super.setNameId(string);
    }

    @Override
    public void setType(int n) {
        if (this.nativeTemplateFrozen) {
            throw new UnsupportedOperationException("published source item template is immutable");
        }
        super.setType(n);
    }

    @Override
    public void setType1(int n) {
        if (this.nativeTemplateFrozen) {
            throw new UnsupportedOperationException("published source item template is immutable");
        }
        super.setType1(n);
    }

    @Override
    public void setMaterial(int n) {
        if (this.nativeTemplateFrozen) {
            throw new UnsupportedOperationException("published source item template is immutable");
        }
        super.setMaterial(n);
    }

    @Override
    public void setWeight(int n) {
        if (this.nativeTemplateFrozen) {
            throw new UnsupportedOperationException("published source item template is immutable");
        }
        super.setWeight(n);
    }

    @Override
    public void setGfxId(int n) {
        if (this.nativeTemplateFrozen) {
            throw new UnsupportedOperationException("published source item template is immutable");
        }
        super.setGfxId(n);
    }

    @Override
    public void setGroundGfxId(int n) {
        if (this.nativeTemplateFrozen) {
            throw new UnsupportedOperationException("published source item template is immutable");
        }
        super.setGroundGfxId(n);
    }

    @Override
    public void setItemDescId(int n) {
        if (this.nativeTemplateFrozen) {
            throw new UnsupportedOperationException("published source item template is immutable");
        }
        super.setItemDescId(n);
    }

    @Override
    public void setMinLevel(int n) {
        if (this.nativeTemplateFrozen) {
            throw new UnsupportedOperationException("published source item template is immutable");
        }
        super.setMinLevel(n);
    }

    @Override
    public void setMaxLevel(int n) {
        if (this.nativeTemplateFrozen) {
            throw new UnsupportedOperationException("published source item template is immutable");
        }
        super.setMaxLevel(n);
    }

    @Override
    public void setBless(int n) {
        if (this.nativeTemplateFrozen) {
            throw new UnsupportedOperationException("published source item template is immutable");
        }
        super.setBless(n);
    }

    @Override
    public void setTradable(boolean bl) {
        if (this.nativeTemplateFrozen) {
            throw new UnsupportedOperationException("published source item template is immutable");
        }
        super.setTradable(bl);
    }

    @Override
    public void setCantDelete(boolean bl) {
        if (this.nativeTemplateFrozen) {
            throw new UnsupportedOperationException("published source item template is immutable");
        }
        super.setCantDelete(bl);
    }

    @Override
    public void setToBeSavedAtOnce(boolean bl) {
        if (this.nativeTemplateFrozen) {
            throw new UnsupportedOperationException("published source item template is immutable");
        }
        super.setToBeSavedAtOnce(bl);
    }

    @Override
    public void setMaxUseTime(int n) {
        if (this.nativeTemplateFrozen) {
            throw new UnsupportedOperationException("published source item template is immutable");
        }
        super.setMaxUseTime(n);
    }

    @Override
    public void setFoodVolume(int n) {
        if (this.nativeTemplateFrozen) {
            throw new UnsupportedOperationException("published source item template is immutable");
        }
        super.setFoodVolume(n);
    }

    @Override
    public void setUseType(int n) {
        if (this.nativeTemplateFrozen) {
            throw new UnsupportedOperationException("published source item template is immutable");
        }
        super.setUseType(n);
    }

    @Override
    public void setDmgSmall(int n) {
        if (this.nativeTemplateFrozen) {
            throw new UnsupportedOperationException("published source item template is immutable");
        }
        super.setDmgSmall(n);
    }

    @Override
    public void setDmgLarge(int n) {
        if (this.nativeTemplateFrozen) {
            throw new UnsupportedOperationException("published source item template is immutable");
        }
        super.setDmgLarge(n);
    }

    @Override
    public void set_mode(int[] nArray) {
        if (this.nativeTemplateFrozen) {
            throw new UnsupportedOperationException("published source item template is immutable");
        }
        super.set_mode(nArray);
    }

    @Override
    public void setSetIds(String string) {
        if (this.nativeTemplateFrozen) {
            throw new UnsupportedOperationException("published source item template is immutable");
        }
        super.setSetIds(string);
    }

    @Override
    public void set_safeenchant(int n) {
        if (this.nativeTemplateFrozen) {
            throw new UnsupportedOperationException("published source item template is immutable");
        }
        super.set_safeenchant(n);
    }

    @Override
    public void setUseRoyal(boolean bl) {
        if (this.nativeTemplateFrozen) {
            throw new UnsupportedOperationException("published source item template is immutable");
        }
        super.setUseRoyal(bl);
    }

    @Override
    public void setUseKnight(boolean bl) {
        if (this.nativeTemplateFrozen) {
            throw new UnsupportedOperationException("published source item template is immutable");
        }
        super.setUseKnight(bl);
    }

    @Override
    public void setUseElf(boolean bl) {
        if (this.nativeTemplateFrozen) {
            throw new UnsupportedOperationException("published source item template is immutable");
        }
        super.setUseElf(bl);
    }

    @Override
    public void setUseMage(boolean bl) {
        if (this.nativeTemplateFrozen) {
            throw new UnsupportedOperationException("published source item template is immutable");
        }
        super.setUseMage(bl);
    }

    @Override
    public void setUseDarkelf(boolean bl) {
        if (this.nativeTemplateFrozen) {
            throw new UnsupportedOperationException("published source item template is immutable");
        }
        super.setUseDarkelf(bl);
    }

    @Override
    public void setUseDragonknight(boolean bl) {
        if (this.nativeTemplateFrozen) {
            throw new UnsupportedOperationException("published source item template is immutable");
        }
        super.setUseDragonknight(bl);
    }

    @Override
    public void setUseIllusionist(boolean bl) {
        if (this.nativeTemplateFrozen) {
            throw new UnsupportedOperationException("published source item template is immutable");
        }
        super.setUseIllusionist(bl);
    }

    @Override
    public void set_addstr(byte by) {
        if (this.nativeTemplateFrozen) {
            throw new UnsupportedOperationException("published source item template is immutable");
        }
        super.set_addstr(by);
    }

    @Override
    public void set_adddex(byte by) {
        if (this.nativeTemplateFrozen) {
            throw new UnsupportedOperationException("published source item template is immutable");
        }
        super.set_adddex(by);
    }

    @Override
    public void set_addcon(byte by) {
        if (this.nativeTemplateFrozen) {
            throw new UnsupportedOperationException("published source item template is immutable");
        }
        super.set_addcon(by);
    }

    @Override
    public void set_addint(byte by) {
        if (this.nativeTemplateFrozen) {
            throw new UnsupportedOperationException("published source item template is immutable");
        }
        super.set_addint(by);
    }

    @Override
    public void set_addwis(byte by) {
        if (this.nativeTemplateFrozen) {
            throw new UnsupportedOperationException("published source item template is immutable");
        }
        super.set_addwis(by);
    }

    @Override
    public void set_addcha(byte by) {
        if (this.nativeTemplateFrozen) {
            throw new UnsupportedOperationException("published source item template is immutable");
        }
        super.set_addcha(by);
    }

    @Override
    public void set_addhp(int n) {
        if (this.nativeTemplateFrozen) {
            throw new UnsupportedOperationException("published source item template is immutable");
        }
        super.set_addhp(n);
    }

    @Override
    public void set_addmp(int n) {
        if (this.nativeTemplateFrozen) {
            throw new UnsupportedOperationException("published source item template is immutable");
        }
        super.set_addmp(n);
    }

    @Override
    public void set_addhpr(int n) {
        if (this.nativeTemplateFrozen) {
            throw new UnsupportedOperationException("published source item template is immutable");
        }
        super.set_addhpr(n);
    }

    @Override
    public void set_addmpr(int n) {
        if (this.nativeTemplateFrozen) {
            throw new UnsupportedOperationException("published source item template is immutable");
        }
        super.set_addmpr(n);
    }

    @Override
    public void set_addsp(int n) {
        if (this.nativeTemplateFrozen) {
            throw new UnsupportedOperationException("published source item template is immutable");
        }
        super.set_addsp(n);
    }

    @Override
    public void set_mdef(int n) {
        if (this.nativeTemplateFrozen) {
            throw new UnsupportedOperationException("published source item template is immutable");
        }
        super.set_mdef(n);
    }

    @Override
    public void setHasteItem(boolean bl) {
        if (this.nativeTemplateFrozen) {
            throw new UnsupportedOperationException("published source item template is immutable");
        }
        super.setHasteItem(bl);
    }

    @Override
    public void setMagicDmgModifier(int n) {
        if (this.nativeTemplateFrozen) {
            throw new UnsupportedOperationException("published source item template is immutable");
        }
        super.setMagicDmgModifier(n);
    }

    @Override
    public void setAddMeleeRedux(int n) {
        if (this.nativeTemplateFrozen) {
            throw new UnsupportedOperationException("published source item template is immutable");
        }
        super.setAddMeleeRedux(n);
    }

    @Override
    public void setAddBowRedux(int n) {
        if (this.nativeTemplateFrozen) {
            throw new UnsupportedOperationException("published source item template is immutable");
        }
        super.setAddBowRedux(n);
    }

    @Override
    public void setAddStunSuccess(int n) {
        if (this.nativeTemplateFrozen) {
            throw new UnsupportedOperationException("published source item template is immutable");
        }
        super.setAddStunSuccess(n);
    }

    @Override
    public void setAddPotionRecovery(int n) {
        if (this.nativeTemplateFrozen) {
            throw new UnsupportedOperationException("published source item template is immutable");
        }
        super.setAddPotionRecovery(n);
    }

    @Override
    public void setAddPoisonResist(int n) {
        if (this.nativeTemplateFrozen) {
            throw new UnsupportedOperationException("published source item template is immutable");
        }
        super.setAddPoisonResist(n);
    }

    @Override
    public void setAddExpRate(int n) {
        if (this.nativeTemplateFrozen) {
            throw new UnsupportedOperationException("published source item template is immutable");
        }
        super.setAddExpRate(n);
    }

    @Override
    public void setAddPierce(int n) {
        if (this.nativeTemplateFrozen) {
            throw new UnsupportedOperationException("published source item template is immutable");
        }
        super.setAddPierce(n);
    }

    @Override
    public void setMagicBow(int n) {
        if (this.nativeTemplateFrozen) {
            throw new UnsupportedOperationException("published source item template is immutable");
        }
        super.setMagicBow(n);
    }

    @Override
    public void setArrowSpr(int n) {
        if (this.nativeTemplateFrozen) {
            throw new UnsupportedOperationException("published source item template is immutable");
        }
        super.setArrowSpr(n);
    }

    @Override
    public void setAddMagicHit(int n) {
        if (this.nativeTemplateFrozen) {
            throw new UnsupportedOperationException("published source item template is immutable");
        }
        super.setAddMagicHit(n);
    }

    @Override
    public void setGreaterTier(int n) {
        if (this.nativeTemplateFrozen) {
            throw new UnsupportedOperationException("published source item template is immutable");
        }
        super.setGreaterTier(n);
    }

    @Override
    public int[] get_mode() {
        return NativeDeItemTemplatesResources.copyArray(super.get_mode());
    }

    @Override
    public int[] getAllSetIds() {
        return NativeDeItemTemplatesResources.copyArray(super.getAllSetIds());
    }
}
