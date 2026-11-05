// DECOMPILED from base-0.6.71.jar, TODO review
package local.lito381.server;

import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.ConcurrentHashMap;
import local.lito381.server.NativeDeDeletionPorts;
import local.lito381.server.NativeDeWorldScope;

public class NativeDeWorldRegistryCrown {
    private final ConcurrentHashMap<Integer, NativeDeDeletionPorts.Viewer> _isCrown = new ConcurrentHashMap();
    private Collection<NativeDeDeletionPorts.Viewer> _allPlayer;

    private NativeDeWorldRegistryCrown() {
    }

    public Collection<NativeDeDeletionPorts.Viewer> all() {
        try {
            Collection<NativeDeDeletionPorts.Viewer> vs = this._allPlayer;
            return vs != null ? vs : (this._allPlayer = Collections.unmodifiableCollection(this._isCrown.values()));
        }
        catch (Exception e) {
            NativeDeWorldScope._log().error(e.getLocalizedMessage(), e);
            return null;
        }
    }

    public ConcurrentHashMap<Integer, NativeDeDeletionPorts.Viewer> map() {
        return this._isCrown;
    }

    public void put(Integer key, NativeDeDeletionPorts.Viewer value) {
        try {
            this._isCrown.put(key, value);
        }
        catch (Exception e) {
            NativeDeWorldScope._log().error(e.getLocalizedMessage(), e);
        }
    }

    public void remove(Integer key) {
        try {
            this._isCrown.remove(key);
        }
        catch (Exception e) {
            NativeDeWorldScope._log().error(e.getLocalizedMessage(), e);
        }
    }

    public static NativeDeWorldRegistryCrown get() {
        return (NativeDeWorldRegistryCrown)NativeDeWorldScope.registry("Crown");
    }
}
