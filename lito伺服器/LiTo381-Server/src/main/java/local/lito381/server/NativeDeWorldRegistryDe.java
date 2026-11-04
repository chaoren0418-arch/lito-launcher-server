// DECOMPILED (CFR 0.152) from reference/base-0.6.71.jar — TODO: replace with original source (T002)
/*
 * Decompiled with CFR 0.152.
 */
package local.lito381.server;

import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.ConcurrentHashMap;
import local.lito381.server.NativeDeDeletionDeState;
import local.lito381.server.NativeDeWorldScope;

public class NativeDeWorldRegistryDe {
    private final ConcurrentHashMap<String, NativeDeDeletionDeState> _isDe = new ConcurrentHashMap();
    private Collection<NativeDeDeletionDeState> _allDe;

    private NativeDeWorldRegistryDe() {
    }

    public Collection<NativeDeDeletionDeState> all() {
        try {
            Collection<NativeDeDeletionDeState> vs = this._allDe;
            return vs != null ? vs : (this._allDe = Collections.unmodifiableCollection(this._isDe.values()));
        }
        catch (Exception e) {
            NativeDeWorldScope._log().error(e.getLocalizedMessage(), e);
            return null;
        }
    }

    public ConcurrentHashMap<String, NativeDeDeletionDeState> map() {
        return this._isDe;
    }

    public NativeDeDeletionDeState getDe(String key) {
        try {
            return this._isDe.get(key);
        }
        catch (Exception e) {
            NativeDeWorldScope._log().error(e.getLocalizedMessage(), e);
            return null;
        }
    }

    public void put(String key, NativeDeDeletionDeState value) {
        try {
            this._isDe.put(key, value);
        }
        catch (Exception e) {
            NativeDeWorldScope._log().error(e.getLocalizedMessage(), e);
        }
    }

    public void remove(String key) {
        try {
            this._isDe.remove(key);
        }
        catch (Exception e) {
            NativeDeWorldScope._log().error(e.getLocalizedMessage(), e);
        }
    }

    public static NativeDeWorldRegistryDe get() {
        return (NativeDeWorldRegistryDe)NativeDeWorldScope.registry("De");
    }
}
