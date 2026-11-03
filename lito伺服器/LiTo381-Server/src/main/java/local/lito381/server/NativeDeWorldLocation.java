// DECOMPILED (CFR 0.152) from reference/base-0.6.71.jar — TODO: replace with original source (T002)
/*
 * Decompiled with CFR 0.152.
 */
package local.lito381.server;

import local.lito381.server.NativeDeObjectScope;
import local.lito381.server.NativeDeWorldMap;
import local.lito381.server.NativeDeWorldPoint;

public final class NativeDeWorldLocation
extends NativeDeWorldPoint {
    protected volatile NativeDeWorldMap _map = NativeDeWorldMap.newNull();

    public NativeDeWorldLocation() {
    }

    public NativeDeWorldLocation(NativeDeWorldLocation nativeDeWorldLocation) {
        this(nativeDeWorldLocation._x, nativeDeWorldLocation._y, nativeDeWorldLocation._map);
    }

    public NativeDeWorldLocation(int n, int n2, int n3) {
        super(n, n2);
        this.setMap(n3);
    }

    public NativeDeWorldLocation(int n, int n2, NativeDeWorldMap nativeDeWorldMap) {
        super(n, n2);
        this._map = nativeDeWorldMap;
    }

    public NativeDeWorldLocation(NativeDeWorldPoint nativeDeWorldPoint, int n) {
        super(nativeDeWorldPoint);
        this.setMap(n);
    }

    public NativeDeWorldLocation(NativeDeWorldPoint nativeDeWorldPoint, NativeDeWorldMap nativeDeWorldMap) {
        super(nativeDeWorldPoint);
        this._map = nativeDeWorldMap;
    }

    public void set(NativeDeWorldLocation nativeDeWorldLocation) {
        this._map = nativeDeWorldLocation._map;
        this._x = nativeDeWorldLocation._x;
        this._y = nativeDeWorldLocation._y;
    }

    public void set(int n, int n2, int n3) {
        this.set(n, n2);
        this.setMap(n3);
    }

    public void set(int n, int n2, NativeDeWorldMap nativeDeWorldMap) {
        this.set(n, n2);
        this._map = nativeDeWorldMap;
    }

    public void set(NativeDeWorldPoint nativeDeWorldPoint, int n) {
        this.set(nativeDeWorldPoint);
        this.setMap(n);
    }

    public void set(NativeDeWorldPoint nativeDeWorldPoint, NativeDeWorldMap nativeDeWorldMap) {
        this.set(nativeDeWorldPoint);
        this._map = nativeDeWorldMap;
    }

    public NativeDeWorldMap getMap() {
        return this._map;
    }

    public int getMapId() {
        return this._map.getId();
    }

    public void setMap(NativeDeWorldMap nativeDeWorldMap) {
        this._map = nativeDeWorldMap;
    }

    public void setMap(int n) {
        this._map = NativeDeObjectScope.Maps.get().getMap((short)n);
    }

    @Override
    public boolean equals(Object object) {
        if (!(object instanceof NativeDeWorldLocation)) {
            return false;
        }
        NativeDeWorldLocation nativeDeWorldLocation = (NativeDeWorldLocation)object;
        return this.getMap() == nativeDeWorldLocation.getMap() && this.getX() == nativeDeWorldLocation.getX() && this.getY() == nativeDeWorldLocation.getY();
    }

    @Override
    public int hashCode() {
        return 7 * this._map.getId() + super.hashCode();
    }

    @Override
    public String toString() {
        return String.format("(%d, %d) on %d", this._x, this._y, this._map.getId());
    }

    void setLocation(int n, int n2, NativeDeWorldMap nativeDeWorldMap) {
        this.set(n, n2, nativeDeWorldMap);
    }
}
