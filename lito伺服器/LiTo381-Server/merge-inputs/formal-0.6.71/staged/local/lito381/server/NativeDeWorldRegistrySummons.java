// DECOMPILED from base-0.6.71.jar, TODO review
package local.lito381.server;

import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.ConcurrentHashMap;
import local.lito381.server.NativeDeDeletionPorts;
import local.lito381.server.NativeDeWorldScope;

public class NativeDeWorldRegistrySummons {
    private final ConcurrentHashMap<Integer, NativeDeDeletionPorts.Summon> _isSummons = new ConcurrentHashMap();
    private Collection<NativeDeDeletionPorts.Summon> _allSummonValues;

    private NativeDeWorldRegistrySummons() {
    }

    public Collection<NativeDeDeletionPorts.Summon> all() {
        try {
            Collection<NativeDeDeletionPorts.Summon> vs = this._allSummonValues;
            return vs != null ? vs : (this._allSummonValues = Collections.unmodifiableCollection(this._isSummons.values()));
        }
        catch (Exception e) {
            NativeDeWorldScope._log().error(e.getLocalizedMessage(), e);
            return null;
        }
    }

    public ConcurrentHashMap<Integer, NativeDeDeletionPorts.Summon> map() {
        return this._isSummons;
    }

    public void put(Integer key, NativeDeDeletionPorts.Summon value) {
        try {
            this._isSummons.put(key, value);
        }
        catch (Exception e) {
            NativeDeWorldScope._log().error(e.getLocalizedMessage(), e);
        }
    }

    public void remove(Integer key) {
        try {
            this._isSummons.remove(key);
        }
        catch (Exception e) {
            NativeDeWorldScope._log().error(e.getLocalizedMessage(), e);
        }
    }

    public static NativeDeWorldRegistrySummons get() {
        return (NativeDeWorldRegistrySummons)NativeDeWorldScope.registry("Summons");
    }
}
