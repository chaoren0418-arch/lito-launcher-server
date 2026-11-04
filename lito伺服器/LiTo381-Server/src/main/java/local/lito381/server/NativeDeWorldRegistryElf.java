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

public class NativeDeWorldRegistryElf {
    private final ConcurrentHashMap<Integer, NativeDeDeletionPorts.Viewer> _isElf = new ConcurrentHashMap();
    private Collection<NativeDeDeletionPorts.Viewer> _allPlayer;

    private NativeDeWorldRegistryElf() {
    }

    public Collection<NativeDeDeletionPorts.Viewer> all() {
        try {
            Collection<NativeDeDeletionPorts.Viewer> vs = this._allPlayer;
            return vs != null ? vs : (this._allPlayer = Collections.unmodifiableCollection(this._isElf.values()));
        }
        catch (Exception e) {
            NativeDeWorldScope._log().error(e.getLocalizedMessage(), e);
            return null;
        }
    }

    public ConcurrentHashMap<Integer, NativeDeDeletionPorts.Viewer> map() {
        return this._isElf;
    }

    public void put(Integer key, NativeDeDeletionPorts.Viewer value) {
        try {
            this._isElf.put(key, value);
        }
        catch (Exception e) {
            NativeDeWorldScope._log().error(e.getLocalizedMessage(), e);
        }
    }

    public void remove(Integer key) {
        try {
            this._isElf.remove(key);
        }
        catch (Exception e) {
            NativeDeWorldScope._log().error(e.getLocalizedMessage(), e);
        }
    }

    public static NativeDeWorldRegistryElf get() {
        return (NativeDeWorldRegistryElf)NativeDeWorldScope.registry("Elf");
    }
}
