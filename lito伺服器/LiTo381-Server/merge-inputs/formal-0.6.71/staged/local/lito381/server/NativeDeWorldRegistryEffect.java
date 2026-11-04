// DECOMPILED from base-0.6.71.jar, TODO review
package local.lito381.server;

import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.ConcurrentHashMap;
import local.lito381.server.NativeDeWorldKinds;
import local.lito381.server.NativeDeWorldScope;

public class NativeDeWorldRegistryEffect {
    private final ConcurrentHashMap<Integer, NativeDeWorldKinds.Effect> _isEff = new ConcurrentHashMap();
    private Collection<NativeDeWorldKinds.Effect> _allEffValues;

    private NativeDeWorldRegistryEffect() {
    }

    public Collection<NativeDeWorldKinds.Effect> all() {
        try {
            Collection<NativeDeWorldKinds.Effect> vs = this._allEffValues;
            return vs != null ? vs : (this._allEffValues = Collections.unmodifiableCollection(this._isEff.values()));
        }
        catch (Exception e) {
            NativeDeWorldScope._log().error(e.getLocalizedMessage(), e);
            return null;
        }
    }

    public ConcurrentHashMap<Integer, NativeDeWorldKinds.Effect> map() {
        return this._isEff;
    }

    public void put(Integer key, NativeDeWorldKinds.Effect value) {
        try {
            this._isEff.put(key, value);
        }
        catch (Exception e) {
            NativeDeWorldScope._log().error(e.getLocalizedMessage(), e);
        }
    }

    public void remove(Integer key) {
        try {
            this._isEff.remove(key);
        }
        catch (Exception e) {
            NativeDeWorldScope._log().error(e.getLocalizedMessage(), e);
        }
    }

    public static NativeDeWorldRegistryEffect get() {
        return (NativeDeWorldRegistryEffect)NativeDeWorldScope.registry("Effect");
    }
}

