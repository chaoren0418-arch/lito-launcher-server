// DECOMPILED (CFR 0.152) from reference/base-0.6.71.jar — TODO: replace with original source (T002)
/*
 * Decompiled with CFR 0.152.
 */
package local.lito381.server;

import java.util.Objects;
import local.lito381.server.NativeDeDeletionOwner;
import local.lito381.server.NativeDeNpcExecutor;
import local.lito381.server.NativeDeNpcMobSkillUse;
import local.lito381.server.NativeDeNpcTemplate;
import local.lito381.server.NativeDeNpcTemplatePorts;
import local.lito381.server.NativeDeNpcTemplateScope;
import local.lito381.server.NativeDePerceptionPorts;
import local.lito381.server.NativeDePerceptionState;
import local.lito381.server.NativeDeResurrectionEffects;

class NativeDeNpcTemplateState
extends NativeDePerceptionState {
    final NativeDeNpcTemplatePorts.Services initialization;
    public NativeDeNpcMobSkillUse _mobSkill;
    public NativeDeNpcExecutor TALK = null;
    public NativeDeNpcExecutor ACTION = null;
    public NativeDeNpcExecutor ATTACK = null;
    public NativeDeNpcExecutor DEATH = null;
    public NativeDeNpcExecutor WORK = null;
    public NativeDeNpcExecutor SPAWN = null;
    private int _passispeed;
    private int _atkspeed;
    private boolean _pickupItem;
    private boolean _Agro;
    private boolean _Agrocoi;
    private boolean _Agrososc;
    private int _lightSize;
    private int _tempLawful = 0;

    NativeDeNpcTemplateState(int n, NativeDeResurrectionEffects nativeDeResurrectionEffects, NativeDeDeletionOwner nativeDeDeletionOwner, NativeDePerceptionPorts.Services services, NativeDeNpcTemplatePorts.Services services2, NativeDeNpcTemplate nativeDeNpcTemplate) {
        super(n, nativeDeResurrectionEffects, nativeDeDeletionOwner, services);
        this.initialization = Objects.requireNonNull(services2);
        if (nativeDeNpcTemplate != null) {
            this.setting_template(nativeDeNpcTemplate);
        }
    }

    public void setting_template(NativeDeNpcTemplate nativeDeNpcTemplate) {
        NativeDeNpcTemplateScope.run(this, () -> this.sourceSettingTemplate(nativeDeNpcTemplate));
    }

    private void sourceSettingTemplate(NativeDeNpcTemplate nativeDeNpcTemplate) {
        int n;
        double d;
        this.template = nativeDeNpcTemplate;
        int n2 = 0;
        double d2 = 0.0;
        double d3 = 0.0;
        this.setName(nativeDeNpcTemplate.get_name());
        String string = this.newName(nativeDeNpcTemplate.get_npcId()) + nativeDeNpcTemplate.get_nameid();
        this.setNameId(string);
        if (nativeDeNpcTemplate.get_randomlevel() == 0) {
            this.setLevel(nativeDeNpcTemplate.get_level());
        } else {
            n2 = NativeDeNpcTemplateScope.random().nextInt(nativeDeNpcTemplate.get_randomlevel() - nativeDeNpcTemplate.get_level() + 1);
            d3 = nativeDeNpcTemplate.get_randomlevel() - nativeDeNpcTemplate.get_level();
            d2 = (double)n2 / d3;
            this.setLevel(n2 += nativeDeNpcTemplate.get_level());
        }
        if (nativeDeNpcTemplate.get_randomhp() == 0) {
            this.setMaxHp(nativeDeNpcTemplate.get_hp());
            this.setCurrentHpDirect(nativeDeNpcTemplate.get_hp());
        } else {
            d = d2 * (double)(nativeDeNpcTemplate.get_randomhp() - nativeDeNpcTemplate.get_hp());
            n = (int)((double)nativeDeNpcTemplate.get_hp() + d);
            this.setMaxHp(n);
            this.setCurrentHpDirect(n);
        }
        if (nativeDeNpcTemplate.get_randommp() == 0) {
            this.setMaxMp(nativeDeNpcTemplate.get_mp());
            this.setCurrentMpDirect(nativeDeNpcTemplate.get_mp());
        } else {
            d = d2 * (double)(nativeDeNpcTemplate.get_randommp() - nativeDeNpcTemplate.get_mp());
            n = (int)((double)nativeDeNpcTemplate.get_mp() + d);
            this.setMaxMp(n);
            this.setCurrentMpDirect(n);
        }
        if (nativeDeNpcTemplate.get_randomac() == 0) {
            this.setAc(nativeDeNpcTemplate.get_ac());
        } else {
            d = d2 * (double)(nativeDeNpcTemplate.get_randomac() - nativeDeNpcTemplate.get_ac());
            n = (int)((double)nativeDeNpcTemplate.get_ac() + d);
            this.setAc(n);
        }
        if (nativeDeNpcTemplate.get_randomlevel() == 0) {
            this.setStr(nativeDeNpcTemplate.get_str());
            this.setCon(nativeDeNpcTemplate.get_con());
            this.setDex(nativeDeNpcTemplate.get_dex());
            this.setInt(nativeDeNpcTemplate.get_int());
            this.setWis(nativeDeNpcTemplate.get_wis());
            this.setMr(nativeDeNpcTemplate.get_mr());
        } else {
            this.setStr((byte)Math.min((double)nativeDeNpcTemplate.get_str() + d3, 127.0));
            this.setCon((byte)Math.min((double)nativeDeNpcTemplate.get_con() + d3, 127.0));
            this.setDex((byte)Math.min((double)nativeDeNpcTemplate.get_dex() + d3, 127.0));
            this.setInt((byte)Math.min((double)nativeDeNpcTemplate.get_int() + d3, 127.0));
            this.setWis((byte)Math.min((double)nativeDeNpcTemplate.get_wis() + d3, 127.0));
            this.setMr((byte)Math.min((double)nativeDeNpcTemplate.get_mr() + d3, 127.0));
            this.addHitup((int)d3 * 2);
            this.addDmgup((int)d3 * 2);
        }
        this.setPassispeed(nativeDeNpcTemplate.get_passispeed());
        this.setAtkspeed(nativeDeNpcTemplate.get_atkspeed());
        this.setAgro(nativeDeNpcTemplate.is_agro());
        this.setAgrocoi(nativeDeNpcTemplate.is_agrocoi());
        this.setAgrososc(nativeDeNpcTemplate.is_agrososc());
        if (((NativeDeNpcTemplate)this.getNpcTemplate()).get_weakAttr() != 0) {
            switch (((NativeDeNpcTemplate)this.getNpcTemplate()).get_weakAttr()) {
                case 1: {
                    this.addEarth(-50);
                    break;
                }
                case 2: {
                    this.addFire(-50);
                    break;
                }
                case 4: {
                    this.addWater(-50);
                    break;
                }
                case 8: {
                    this.addWind(-50);
                    break;
                }
                case -1: {
                    this.addEarth(50);
                    break;
                }
                case -2: {
                    this.addFire(50);
                    break;
                }
                case -4: {
                    this.addWater(50);
                    break;
                }
                case -8: {
                    this.addWind(50);
                }
            }
        }
        int n3 = this.newGfx(nativeDeNpcTemplate.get_gfxid());
        this.setTempCharGfx(n3);
        this.setGfxId(n3);
        this.setGfxidInStatus(n3);
        if (nativeDeNpcTemplate.get_randomexp() == 0) {
            this.setExp(nativeDeNpcTemplate.get_exp());
        } else {
            int n4 = nativeDeNpcTemplate.get_exp() + NativeDeNpcTemplateScope.random().nextInt(nativeDeNpcTemplate.get_randomexp());
            this.setExp(n4);
        }
        if (nativeDeNpcTemplate.get_randomlawful() == 0) {
            this.setLawful(nativeDeNpcTemplate.get_lawful());
            this.setTempLawful(nativeDeNpcTemplate.get_lawful());
        } else {
            double d4 = d2 * (double)(nativeDeNpcTemplate.get_randomlawful() - nativeDeNpcTemplate.get_lawful());
            int n5 = (int)((double)nativeDeNpcTemplate.get_lawful() + d4);
            this.setLawful(n5);
            this.setTempLawful(n5);
        }
        this.setPickupItem(nativeDeNpcTemplate.is_picupitem());
        if (nativeDeNpcTemplate.is_bravespeed()) {
            this.setBraveSpeed(1);
        } else {
            this.setBraveSpeed(0);
        }
        this.setKarma(nativeDeNpcTemplate.getKarma());
        this.setLightSize(nativeDeNpcTemplate.getLightSize());
        if (nativeDeNpcTemplate.talk()) {
            this.TALK = nativeDeNpcTemplate.getNpcExecutor();
        }
        if (nativeDeNpcTemplate.action()) {
            this.ACTION = nativeDeNpcTemplate.getNpcExecutor();
        }
        if (nativeDeNpcTemplate.attack()) {
            this.ATTACK = nativeDeNpcTemplate.getNpcExecutor();
        }
        if (nativeDeNpcTemplate.death()) {
            this.DEATH = nativeDeNpcTemplate.getNpcExecutor();
        }
        if (nativeDeNpcTemplate.work()) {
            this.WORK = nativeDeNpcTemplate.getNpcExecutor();
            if (this.WORK.workTime() != 0) {
                NativeDeNpcTemplatePorts.WorkTimer.put(this, this.WORK.workTime());
            } else {
                this.WORK.work(this);
            }
        }
        if (nativeDeNpcTemplate.spawn()) {
            this.SPAWN = nativeDeNpcTemplate.getNpcExecutor();
            this.SPAWN.spawn(this);
        }
        this._mobSkill = new NativeDeNpcMobSkillUse(this);
        this.setWeaponBreakStack(0);
    }

    public void setGfxidInStatus(int n) {
        switch (n) {
            case 51: 
            case 110: 
            case 147: {
                this.setStatus(24);
                break;
            }
            case 57: 
            case 816: 
            case 3137: 
            case 3140: 
            case 3145: 
            case 3148: 
            case 3151: 
            case 7621: {
                this.setStatus(20);
                break;
            }
            case 111: {
                this.setStatus(4);
            }
        }
    }

    private String newName(int n) {
        String string = "";
        return string;
    }

    private int newGfx(int n) {
        int[] nArray = null;
        switch (n) {
            case 998: 
            case 999: 
            case 1002: 
            case 1003: {
                nArray = new int[]{998, 999, 1002, 1003};
                break;
            }
            case 1597: 
            case 1600: {
                nArray = new int[]{1597, 1600};
                break;
            }
            case 5942: {
                nArray = new int[]{5942, 5135, 5137, 5139, 5141, 5143, 5145, 5156, 5158, 5160, 5162};
                break;
            }
            case 1318: 
            case 1321: {
                nArray = new int[]{1318, 1321};
            }
        }
        if (nArray != null) {
            int n2 = NativeDeNpcTemplateScope.random().nextInt(nArray.length);
            n = nArray[n2];
        }
        return n;
    }

    public int getPassispeed() {
        return this._passispeed;
    }

    public void setPassispeed(int n) {
        this._passispeed = n;
    }

    public int getAtkspeed() {
        return this._atkspeed;
    }

    public void setAtkspeed(int n) {
        this._atkspeed = n;
    }

    public boolean isPickupItem() {
        return this._pickupItem;
    }

    public void setPickupItem(boolean bl) {
        this._pickupItem = bl;
    }

    @Override
    public NativeDeNpcTemplate getNpcTemplate() {
        return (NativeDeNpcTemplate)this.template;
    }

    @Override
    public String getNameId() {
        return this.nameId;
    }

    public void setNameId(String string) {
        this.nameId = string;
    }

    public boolean isAgro() {
        return this._Agro;
    }

    public void setAgro(boolean bl) {
        this._Agro = bl;
    }

    public boolean isAgrocoi() {
        return this._Agrocoi;
    }

    public void setAgrocoi(boolean bl) {
        this._Agrocoi = bl;
    }

    public boolean isAgrososc() {
        return this._Agrososc;
    }

    public void setAgrososc(boolean bl) {
        this._Agrososc = bl;
    }

    public int getLightSize() {
        return this._lightSize;
    }

    public void setLightSize(int n) {
        this._lightSize = n;
    }

    public int getTempLawful() {
        return this._tempLawful;
    }

    public void setTempLawful(int n) {
        this._tempLawful = n;
    }
}
