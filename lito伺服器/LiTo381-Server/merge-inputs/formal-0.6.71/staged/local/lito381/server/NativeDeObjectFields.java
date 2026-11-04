// DECOMPILED from base-0.6.71.jar, TODO review
package local.lito381.server;

import java.io.Serializable;
import local.lito381.server.NativeDeDeletionPorts;
import local.lito381.server.NativeDeObjectBindings;
import local.lito381.server.NativeDeObjectScope;
import local.lito381.server.NativeDeWorldLocation;
import local.lito381.server.NativeDeWorldMap;

abstract class NativeDeObjectFields
implements NativeDeDeletionPorts.ObjectRef,
Serializable {
    private static final long serialVersionUID = 1L;
    private volatile long _locationRevision;
    private final NativeDeWorldLocation _loc = new NativeDeWorldLocation();
    int showId = -1;
    protected volatile Object sourceDeWorldEntry;

    protected abstract int getId();

    public void setId(int n) {
        if (n != this.getId()) {
            throw new IllegalArgumentException("native object identity is immutable");
        }
    }

    public synchronized void setMap(short s) {
        NativeDeObjectScope.run(this, () -> this.source$setMap(s));
    }

    public synchronized void setMap(NativeDeWorldMap nativeDeWorldMap) {
        NativeDeObjectScope.run(this, () -> this.source$setMap(nativeDeWorldMap));
    }

    public synchronized void rebindWorldMap(short s) {
        NativeDeObjectScope.run(this, () -> this.source$rebindWorldMap(s));
    }

    public synchronized void setX(int n) {
        NativeDeObjectScope.run(this, () -> this.source$setX(n));
    }

    public synchronized void setY(int n) {
        NativeDeObjectScope.run(this, () -> this.source$setY(n));
    }

    public synchronized void setXY(int n, int n2) {
        NativeDeObjectScope.run(this, () -> this.source$setXY(n, n2));
    }

    public synchronized void forward(int n) {
        NativeDeObjectScope.run(this, () -> this.source$forward(n));
    }

    public synchronized void setLocation(NativeDeWorldLocation nativeDeWorldLocation) {
        NativeDeObjectScope.run(this, () -> this.source$setLocation(nativeDeWorldLocation));
    }

    public synchronized void setLocation(int n, int n2, int n3) {
        NativeDeObjectScope.run(this, () -> this.source$setLocation(n, n2, n3));
    }

    public void set_showId(int n) {
        NativeDeObjectScope.run(this, () -> this.source$set_showId(n));
    }

    public final long getLocationRevision() {
        return this._locationRevision;
    }

    private void markLocationChanged() {
        ++this._locationRevision;
    }

    public short getMapId() {
        return (short)this._loc.getMap().getId();
    }

    private void source$setMap(short n) {
        NativeDeWorldMap nativeDeWorldMap = NativeDeObjectScope.Maps.get().getMap((short)n);
        NativeDeWorldMap nativeDeWorldMap2 = this._loc.getMap();
        if (nativeDeWorldMap2 == nativeDeWorldMap) {
            return;
        }
        if (!nativeDeWorldMap2.isNull() && !nativeDeWorldMap.isNull() && nativeDeWorldMap2.getId() == n) {
            return;
        }
        int n2 = this._loc.getX();
        int n3 = this._loc.getY();
        int n4 = nativeDeWorldMap2.isNull() ? n : nativeDeWorldMap2.getId();
        this.markLocationChanged();
        this._loc.setMap(nativeDeWorldMap);
        this.notifyWorldSpatialIndex(n2, n3, n4);
    }

    public NativeDeWorldMap getMap() {
        return this._loc.getMap();
    }

    private void source$setMap(NativeDeWorldMap nativeDeWorldMap) {
        if (nativeDeWorldMap == null) {
            throw new NullPointerException();
        }
        NativeDeWorldMap nativeDeWorldMap2 = this._loc.getMap();
        if (nativeDeWorldMap2 == nativeDeWorldMap) {
            return;
        }
        if (!nativeDeWorldMap2.isNull() && !nativeDeWorldMap.isNull() && nativeDeWorldMap2.getId() == nativeDeWorldMap.getId()) {
            return;
        }
        int n = this._loc.getX();
        int n2 = this._loc.getY();
        int n3 = nativeDeWorldMap2.isNull() ? nativeDeWorldMap.getId() : nativeDeWorldMap2.getId();
        this.markLocationChanged();
        this._loc.setMap(nativeDeWorldMap);
        this.notifyWorldSpatialIndex(n, n2, n3);
    }

    private void source$rebindWorldMap(short n) {
        NativeDeWorldMap nativeDeWorldMap = NativeDeObjectScope.Maps.get().getMap((short)n);
        if (nativeDeWorldMap.isNull()) {
            return;
        }
        NativeDeWorldMap nativeDeWorldMap2 = this._loc.getMap();
        if (nativeDeWorldMap2 == nativeDeWorldMap) {
            return;
        }
        int n2 = this._loc.getX();
        int n3 = this._loc.getY();
        int n4 = nativeDeWorldMap2.isNull() ? n : nativeDeWorldMap2.getId();
        this.markLocationChanged();
        this._loc.setMap(nativeDeWorldMap);
        this.notifyWorldSpatialIndex(n2, n3, n4);
    }

    public int getX() {
        return this._loc.getX();
    }

    private void source$setX(int n) {
        int n2 = this._loc.getX();
        if (n2 == n) {
            return;
        }
        int n3 = this._loc.getY();
        int n4 = this._loc.getMapId();
        this.markLocationChanged();
        this._loc.setX(n);
        this.notifyWorldSpatialIndex(n2, n3, n4);
    }

    public int getY() {
        return this._loc.getY();
    }

    private void source$setY(int n) {
        int n2 = this._loc.getX();
        int n3 = this._loc.getY();
        if (n3 == n) {
            return;
        }
        int n4 = this._loc.getMapId();
        this.markLocationChanged();
        this._loc.setY(n);
        this.notifyWorldSpatialIndex(n2, n3, n4);
    }

    private void source$setXY(int n, int n2) {
        int n3 = this._loc.getX();
        int n4 = this._loc.getY();
        if (n3 == n && n4 == n2) {
            return;
        }
        int n5 = this._loc.getMapId();
        this.markLocationChanged();
        this._loc.set(n, n2);
        this.notifyWorldSpatialIndex(n3, n4, n5);
    }

    private void source$forward(int n) {
        int n2 = this._loc.getX();
        int n3 = this._loc.getY();
        int n4 = this._loc.getMapId();
        this.markLocationChanged();
        this._loc.forward(n);
        this.notifyWorldSpatialIndex(n2, n3, n4);
    }

    public NativeDeWorldLocation getLocation() {
        return this._loc;
    }

    private void source$setLocation(NativeDeWorldLocation nativeDeWorldLocation) {
        if (nativeDeWorldLocation == null) {
            throw new NullPointerException("loc");
        }
        int n = this._loc.getX();
        int n2 = this._loc.getY();
        int n3 = this._loc.getMapId();
        if (n == nativeDeWorldLocation.getX() && n2 == nativeDeWorldLocation.getY() && this._loc.getMap() == nativeDeWorldLocation.getMap()) {
            return;
        }
        this.markLocationChanged();
        this._loc.set(nativeDeWorldLocation);
        NativeDeObjectScope.World.get().updateObjectSector(this, n, n2, n3, this._loc.getX(), this._loc.getY(), this._loc.getMapId());
    }

    private void source$setLocation(int n, int n2, int n3) {
        int n4 = this._loc.getX();
        int n5 = this._loc.getY();
        int n6 = this._loc.getMapId();
        NativeDeWorldMap nativeDeWorldMap = NativeDeObjectScope.Maps.get().getMap((short)n3);
        if (n4 == n && n5 == n2 && this._loc.getMap() == nativeDeWorldMap) {
            return;
        }
        this.markLocationChanged();
        this._loc.set(n, n2, n3);
        NativeDeObjectScope.World.get().updateObjectSector(this, n4, n5, n6, this._loc.getX(), this._loc.getY(), this._loc.getMapId());
    }

    private void notifyWorldSpatialIndex(int n, int n2, int n3) {
        NativeDeObjectScope.World.get().updateObjectSector(this, n, n2, n3, this._loc.getX(), this._loc.getY(), this._loc.getMapId());
    }

    public double getLineDistance(NativeDeObjectFields nativeDeObjectFields) {
        return this.getLocation().getLineDistance(nativeDeObjectFields.getLocation());
    }

    public int getTileLineDistance(NativeDeObjectFields nativeDeObjectFields) {
        return this.getLocation().getTileLineDistance(nativeDeObjectFields.getLocation());
    }

    public int getTileDistance(NativeDeObjectFields nativeDeObjectFields) {
        return this.getLocation().getTileDistance(nativeDeObjectFields.getLocation());
    }

    public void onPerceive(NativeDeDeletionPorts.Viewer viewer) {
    }

    public void onAction(NativeDeDeletionPorts.Viewer viewer) {
    }

    public void onTalkAction(NativeDeDeletionPorts.Viewer viewer) {
    }

    public int get_showId() {
        return this.showId;
    }

    private void source$set_showId(int n) {
        this.showId = n;
        NativeDeObjectBindings.showChanged(this, n);
    }
}
