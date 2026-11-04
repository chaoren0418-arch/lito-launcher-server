// DECOMPILED from base-0.6.71.jar, TODO review
package local.lito381.server;

import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.ConcurrentHashMap;
import local.lito381.server.NativeDeWorldKinds;
import local.lito381.server.NativeDeWorldScope;

public class NativeDeWorldRegistryItem {
    private final ConcurrentHashMap<Integer, NativeDeWorldKinds.Item> _isItem = new ConcurrentHashMap();
    private Collection<NativeDeWorldKinds.Item> _allItemValues;

    private NativeDeWorldRegistryItem() {
    }

    public Collection<NativeDeWorldKinds.Item> all() {
        try {
            Collection<NativeDeWorldKinds.Item> vs = this._allItemValues;
            return vs != null ? vs : (this._allItemValues = Collections.unmodifiableCollection(this._isItem.values()));
        }
        catch (Exception e) {
            NativeDeWorldScope._log().error(e.getLocalizedMessage(), e);
            return null;
        }
    }

    public ConcurrentHashMap<Integer, NativeDeWorldKinds.Item> map() {
        return this._isItem;
    }

    public void put(Integer key, NativeDeWorldKinds.Item value) {
        try {
            this._isItem.put(key, value);
        }
        catch (Exception e) {
            NativeDeWorldScope._log().error(e.getLocalizedMessage(), e);
        }
    }

    public void remove(Integer key) {
        try {
            this._isItem.remove(key);
        }
        catch (Exception e) {
            NativeDeWorldScope._log().error(e.getLocalizedMessage(), e);
        }
    }

    public static NativeDeWorldRegistryItem get() {
        return (NativeDeWorldRegistryItem)NativeDeWorldScope.registry("Item");
    }
}

