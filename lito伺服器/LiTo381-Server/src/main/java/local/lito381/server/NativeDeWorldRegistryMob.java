// DECOMPILED (CFR 0.152) from reference/base-0.6.71.jar — TODO: replace with original source (T002)
/*
 * Decompiled with CFR 0.152.
 */
package local.lito381.server;

import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.ConcurrentHashMap;
import local.lito381.server.NativeDeDeletionPorts;
import local.lito381.server.NativeDeWorldScope;

public class NativeDeWorldRegistryMob {
    private final ConcurrentHashMap<Integer, NativeDeDeletionPorts.Monster> _isMob = new ConcurrentHashMap();
    private Collection<NativeDeDeletionPorts.Monster> _allMobValues;

    private NativeDeWorldRegistryMob() {
    }

    public Collection<NativeDeDeletionPorts.Monster> all() {
        try {
            Collection<NativeDeDeletionPorts.Monster> vs = this._allMobValues;
            return vs != null ? vs : (this._allMobValues = Collections.unmodifiableCollection(this._isMob.values()));
        }
        catch (Exception e) {
            NativeDeWorldScope._log().error(e.getLocalizedMessage(), e);
            return null;
        }
    }

    public ConcurrentHashMap<Integer, NativeDeDeletionPorts.Monster> map() {
        return this._isMob;
    }

    public void put(Integer key, NativeDeDeletionPorts.Monster value) {
        try {
            this._isMob.put(key, value);
        }
        catch (Exception e) {
            NativeDeWorldScope._log().error(e.getLocalizedMessage(), e);
        }
    }

    public void remove(Integer key) {
        try {
            this._isMob.remove(key);
        }
        catch (Exception e) {
            NativeDeWorldScope._log().error(e.getLocalizedMessage(), e);
        }
    }

    public static NativeDeWorldRegistryMob get() {
        return (NativeDeWorldRegistryMob)NativeDeWorldScope.registry("Mob");
    }
}
