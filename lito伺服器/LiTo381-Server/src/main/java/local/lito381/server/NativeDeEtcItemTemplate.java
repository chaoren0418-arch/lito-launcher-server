// DECOMPILED (CFR 0.152) from reference/base-0.6.71.jar — TODO: replace with original source (T002)
/*
 * Decompiled with CFR 0.152.
 */
package local.lito381.server;

import local.lito381.server.NativeDeItemTemplate;

public class NativeDeEtcItemTemplate
extends NativeDeItemTemplate {
    private static final long serialVersionUID = 1L;
    private boolean _stackable;
    private int _delay_id;
    private int _delay_time;
    private int _delay_effect;
    private int _maxChargeCount;
    private boolean _consumeOnAttack = true;
    private int _limitClass = 127;

    public int getLimitClass() {
        return this._limitClass;
    }

    public void setLimitClass(int n) {
        this._limitClass = n;
    }

    @Override
    public boolean isStackable() {
        return this._stackable;
    }

    public void set_stackable(boolean bl) {
        this._stackable = bl;
    }

    public void set_delayid(int n) {
        this._delay_id = n;
    }

    @Override
    public int get_delayid() {
        return this._delay_id;
    }

    public void set_delaytime(int n) {
        this._delay_time = n;
    }

    @Override
    public int get_delaytime() {
        return this._delay_time;
    }

    @Override
    public void set_delayEffect(int n) {
        this._delay_effect = n;
    }

    @Override
    public int get_delayEffect() {
        return this._delay_effect;
    }

    public void setMaxChargeCount(int n) {
        this._maxChargeCount = n;
    }

    @Override
    public int getMaxChargeCount() {
        return this._maxChargeCount;
    }

    public boolean isConsumeOnAttack() {
        return this._consumeOnAttack;
    }

    public void setConsumeOnAttack(boolean bl) {
        this._consumeOnAttack = bl;
    }
}
