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

public class NativeDeWorldRegistryPet {
    private final ConcurrentHashMap<Integer, NativeDeWorldKinds.Pet> _isPet = new ConcurrentHashMap();
    private Collection<NativeDeWorldKinds.Pet> _allPetValues;

    private NativeDeWorldRegistryPet() {
    }

    public Collection<NativeDeWorldKinds.Pet> all() {
        try {
            Collection<NativeDeWorldKinds.Pet> vs = this._allPetValues;
            return vs != null ? vs : (this._allPetValues = Collections.unmodifiableCollection(this._isPet.values()));
        }
        catch (Exception e) {
            NativeDeWorldScope._log().error(e.getLocalizedMessage(), e);
            return null;
        }
    }

    public ConcurrentHashMap<Integer, NativeDeWorldKinds.Pet> map() {
        return this._isPet;
    }

    public void put(Integer key, NativeDeWorldKinds.Pet value) {
        try {
            this._isPet.put(key, value);
        }
        catch (Exception e) {
            NativeDeWorldScope._log().error(e.getLocalizedMessage(), e);
        }
    }

    public void remove(Integer key) {
        try {
            this._isPet.remove(key);
        }
        catch (Exception e) {
            NativeDeWorldScope._log().error(e.getLocalizedMessage(), e);
        }
    }

    public static NativeDeWorldRegistryPet get() {
        return (NativeDeWorldRegistryPet)NativeDeWorldScope.registry("Pet");
    }
}
