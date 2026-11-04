// DECOMPILED from base-0.6.71.jar, TODO review
package local.lito381.server;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import local.lito381.server.NativeDeNpcConstructionPorts;
import local.lito381.server.NativeDeNpcMoveExecutor;
import local.lito381.server.NativeDeNpcTemplateState;

class NativeDeNpcMoveState
extends NativeDeNpcMoveExecutor {
    final NativeDeNpcConstructionPorts.Services nativeServices;
    private int _aie = 0;
    private volatile PathState _currentPath = null;
    private volatile PathFailure _lastPathFailure = PathFailure.NONE;
    private final Lock _pathMutex = new ReentrantLock();
    private final NativeDeNpcTemplateState _npc;

    @Override
    public void setDirectionMove(int n) {
        this.nativeServices.movement().step(this._npc, n);
    }

    @Override
    public int moveDirection(int n, int n2) {
        return this.nativeServices.movement().direction(this._npc, n, n2);
    }

    @Override
    public int targetReverseDirection(int n, int n2) {
        return this.nativeServices.movement().reverse(this._npc, n, n2);
    }

    @Override
    public int checkObject(int n) {
        return this.nativeServices.movement().obstacle(this._npc, n);
    }

    @Override
    public int openDoor(int n) {
        return this.nativeServices.movement().door(this._npc, n);
    }

    public NativeDeNpcMoveState(NativeDeNpcTemplateState nativeDeNpcTemplateState, NativeDeNpcConstructionPorts.Services services) {
        this._npc = nativeDeNpcTemplateState;
        this.nativeServices = Objects.requireNonNull(services);
    }

    private void clearPathUnlocked() {
        this._currentPath = null;
        this._aie = 0;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private void synchronizePathAfterMove(int n, int n2) {
        this._pathMutex.lock();
        try {
            PathState pathState = this._currentPath;
            if (pathState == null || pathState.cxy == null || !pathState.hasRemaining()) {
                return;
            }
            int n3 = pathState.nextIndex;
            pathState.synchronizePosition(n, n2);
            if (pathState.nextIndex == n3) {
                this.clearPathUnlocked();
            }
        }
        finally {
            this._pathMutex.unlock();
        }
    }

    @Override
    public boolean wasLastPathUnreachable() {
        return this._lastPathFailure == PathFailure.UNREACHABLE;
    }

    @Override
    public void clear() {
        this._pathMutex.lock();
        try {
            this.clearPathUnlocked();
        }
        finally {
            this._pathMutex.unlock();
        }
    }

    private static final class PathState {
        final List<int[]> steps;
        final int[] cxy;
        final int goalX;
        final int goalY;
        int nextIndex;

        PathState(List<int[]> list, int[] nArray, int n, int n2) {
            this.steps = list;
            this.cxy = nArray != null ? (int[])nArray.clone() : null;
            this.goalX = n;
            this.goalY = n2;
            this.nextIndex = 0;
        }

        boolean hasRemaining() {
            return this.steps != null && this.nextIndex < this.steps.size();
        }

        boolean matchesGoal(int n, int n2) {
            return this.goalX == n && this.goalY == n2;
        }

        int[] peekStep() {
            return this.steps.get(this.nextIndex);
        }

        void synchronizePosition(int n, int n2) {
            while (this.hasRemaining()) {
                int[] nArray = this.peekStep();
                if (this.cxy[0] + nArray[0] != n || this.cxy[1] + nArray[1] != n2) {
                    return;
                }
                ++this.nextIndex;
            }
        }
    }

    public static enum PathFailure {
        NONE,
        TRANSIENT,
        UNREACHABLE;

    }
}
