// DECOMPILED from base-0.6.71.jar, TODO review
package local.lito381.server;

import java.util.HashMap;
import local.lito381.server.NativeDeWorldKinds;
import local.lito381.server.NativeDeWorldScope;

public class NativeDeWorldRegistryBow {
    private final HashMap<Integer, NativeDeWorldKinds.Bow> _isBow = new HashMap();

    private NativeDeWorldRegistryBow() {
    }

    public HashMap<Integer, NativeDeWorldKinds.Bow> map() {
        return this._isBow;
    }

    public void put(Integer key, NativeDeWorldKinds.Bow value) {
        try {
            this._isBow.put(key, value);
        }
        catch (Exception e) {
            NativeDeWorldScope._log().error(e.getLocalizedMessage(), e);
        }
    }

    public void remove(Integer key) {
        try {
            this._isBow.remove(key);
        }
        catch (Exception e) {
            NativeDeWorldScope._log().error(e.getLocalizedMessage(), e);
        }
    }

    public static NativeDeWorldRegistryBow get() {
        return (NativeDeWorldRegistryBow)NativeDeWorldScope.registry("Bow");
    }
}

