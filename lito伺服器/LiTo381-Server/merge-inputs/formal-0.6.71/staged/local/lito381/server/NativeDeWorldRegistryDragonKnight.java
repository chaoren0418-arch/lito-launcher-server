// DECOMPILED from base-0.6.71.jar, TODO review
package local.lito381.server;

import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.ConcurrentHashMap;
import local.lito381.server.NativeDeDeletionPorts;
import local.lito381.server.NativeDeWorldScope;

public class NativeDeWorldRegistryDragonKnight {
    private final ConcurrentHashMap<Integer, NativeDeDeletionPorts.Viewer> _isDragonKnight = new ConcurrentHashMap();
    private Collection<NativeDeDeletionPorts.Viewer> _allPlayer;

    private NativeDeWorldRegistryDragonKnight() {
    }

    public Collection<NativeDeDeletionPorts.Viewer> all() {
        try {
            Collection<NativeDeDeletionPorts.Viewer> vs = this._allPlayer;
            return vs != null ? vs : (this._allPlayer = Collections.unmodifiableCollection(this._isDragonKnight.values()));
        }
        catch (Exception e) {
            NativeDeWorldScope._log().error(e.getLocalizedMessage(), e);
            return null;
        }
    }

    public ConcurrentHashMap<Integer, NativeDeDeletionPorts.Viewer> map() {
        return this._isDragonKnight;
    }

    public void put(Integer key, NativeDeDeletionPorts.Viewer value) {
        try {
            this._isDragonKnight.put(key, value);
        }
        catch (Exception e) {
            NativeDeWorldScope._log().error(e.getLocalizedMessage(), e);
        }
    }

    public void remove(Integer key) {
        try {
            this._isDragonKnight.remove(key);
        }
        catch (Exception e) {
            NativeDeWorldScope._log().error(e.getLocalizedMessage(), e);
        }
    }

    public static NativeDeWorldRegistryDragonKnight get() {
        return (NativeDeWorldRegistryDragonKnight)NativeDeWorldScope.registry("DragonKnight");
    }
}

