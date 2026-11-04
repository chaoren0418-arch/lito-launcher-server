// DECOMPILED (CFR 0.152) from reference/base-0.6.71.jar — TODO: replace with original source (T002)
/*
 * Decompiled with CFR 0.152.
 */
package local.lito381.server;

import local.lito381.server.NativeDeInitializerKernel;

public class NativeDeInstanceName
extends NativeDeInitializerKernel.Name {
    private int _deobjid;
    private String _name;
    private int _classId;
    private int _sex;
    private int _clanid;
    private int _weaponType;
    private String _title;
    private String _deShop;

    public NativeDeInstanceName(int n, String string, int n2, int n3, int n4, int n5, String string2, String string3) {
        super(n, string, n2, n3, n4, n5, string2, string3);
        this._deobjid = n;
        this._name = string;
        this._classId = n2;
        this._sex = n3;
        this._clanid = n4;
        this._weaponType = n5;
        this._title = string2;
        this._deShop = string3;
    }

    @Override
    public int get_deobjid() {
        return this._deobjid;
    }

    @Override
    public void set_deobjid(int n) {
        this._deobjid = n;
    }

    @Override
    public String get_name() {
        return this._name;
    }

    @Override
    public void set_name(String string) {
        this._name = string;
    }

    @Override
    public int get_classId() {
        return this._classId;
    }

    @Override
    public void set_classId(int n) {
        this._classId = n;
    }

    @Override
    public int get_type() {
        return this._classId;
    }

    @Override
    public void set_type(int n) {
        this._classId = n;
    }

    @Override
    public int get_sex() {
        return this._sex;
    }

    @Override
    public void set_sex(int n) {
        this._sex = n;
    }

    @Override
    public int get_clanid() {
        return this._clanid;
    }

    @Override
    public void set_clanid(int n) {
        this._clanid = n;
    }

    @Override
    public int get_weaponType() {
        return this._weaponType;
    }

    @Override
    public void set_weaponType(int n) {
        this._weaponType = n;
    }

    @Override
    public String getTitle() {
        return this._title;
    }

    @Override
    public void setTitle(String string) {
        this._title = string;
    }

    @Override
    public String getDeShop() {
        return this._deShop;
    }

    @Override
    public void setDeShop(String string) {
        this._deShop = string;
    }
}
