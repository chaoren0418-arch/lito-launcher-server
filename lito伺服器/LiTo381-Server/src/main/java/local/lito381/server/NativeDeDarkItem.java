// DECOMPILED (CFR 0.152) from reference/base-0.6.71.jar — TODO: replace with original source (T002)
/*
 * Decompiled with CFR 0.152.
 */
package local.lito381.server;

import local.lito381.server.NativeDeItemAbility;

public class NativeDeDarkItem {
    private final int _itemObjId;
    private final int _configId;
    private final String _title;
    private final NativeDeItemAbility _ability;

    public NativeDeDarkItem(int n, int n2, String string) {
        this._itemObjId = n;
        this._configId = n2;
        this._title = string;
        this._ability = new NativeDeItemAbility();
    }

    public int getItemObjId() {
        return this._itemObjId;
    }

    public int getConfigId() {
        return this._configId;
    }

    public String getTitle() {
        return this._title;
    }

    public void set(NativeDeItemAbility.AbilityType abilityType, int n) {
        this._ability.set(abilityType, n);
    }

    public NativeDeItemAbility getAbility() {
        return this._ability;
    }
}
