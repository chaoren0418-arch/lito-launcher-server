// DECOMPILED (CFR 0.152) from reference/base-0.6.71.jar — TODO: replace with original source (T002)
/*
 * Decompiled with CFR 0.152.
 */
package local.lito381.server;

import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.ConcurrentHashMap;
import local.lito381.server.NativeDeWorldKinds;
import local.lito381.server.NativeDeWorldScope;

public class NativeDeWorldRegistryDoll {
    private final ConcurrentHashMap<Integer, NativeDeWorldKinds.Doll> _isDoll = new ConcurrentHashMap();
    private Collection<NativeDeWorldKinds.Doll> _allDollValues;

    private NativeDeWorldRegistryDoll() {
    }

    public Collection<NativeDeWorldKinds.Doll> all() {
        try {
            Collection<NativeDeWorldKinds.Doll> vs = this._allDollValues;
            return vs != null ? vs : (this._allDollValues = Collections.unmodifiableCollection(this._isDoll.values()));
        }
        catch (Exception e) {
            NativeDeWorldScope._log().error(e.getLocalizedMessage(), e);
            return null;
        }
    }

    public ConcurrentHashMap<Integer, NativeDeWorldKinds.Doll> map() {
        return this._isDoll;
    }

    public void put(Integer key, NativeDeWorldKinds.Doll value) {
        try {
            this._isDoll.put(key, value);
        }
        catch (Exception e) {
            NativeDeWorldScope._log().error(e.getLocalizedMessage(), e);
        }
    }

    public void remove(Integer key) {
        try {
            this._isDoll.remove(key);
        }
        catch (Exception e) {
            NativeDeWorldScope._log().error(e.getLocalizedMessage(), e);
        }
    }

    public static NativeDeWorldRegistryDoll get() {
        return (NativeDeWorldRegistryDoll)NativeDeWorldScope.registry("Doll");
    }
}
