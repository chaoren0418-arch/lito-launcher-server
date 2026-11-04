// DECOMPILED (CFR 0.152) from reference/base-0.6.71.jar — TODO: replace with original source (T002)
/*
 * Decompiled with CFR 0.152.
 */
package local.lito381.server;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import local.lito381.server.NativeDeDeletionState;
import local.lito381.server.NativeDeWorldAccess;
import local.lito381.server.NativeDeWorldQuestUser;
import local.lito381.server.NativeDeWorldScope;

public class NativeDeWorldRegistryQuest {
    private final Lock _lock = new ReentrantLock(true);
    private AtomicInteger _nextId;
    private final ConcurrentHashMap<Integer, NativeDeWorldQuestUser> _isQuest = new ConcurrentHashMap();

    private NativeDeWorldRegistryQuest() {
        this._nextId = new AtomicInteger(100);
    }

    public void rollbackNpcRegistration(int key, NativeDeDeletionState npc) {
        try {
            NativeDeWorldQuestUser value = this._isQuest.get(key);
            if (value != null) {
                value.rollbackNpcRegistration(npc);
            }
        }
        catch (Exception e) {
            NativeDeWorldScope._log().error("Quest NPC registration rollback failed: key={} objectId={}", (Object)key, (Object)(npc != null ? NativeDeWorldAccess.L1NpcInstance$getId(npc) : -1), (Object)e);
        }
    }

    public static NativeDeWorldRegistryQuest get() {
        return (NativeDeWorldRegistryQuest)NativeDeWorldScope.registry("Quest");
    }
}
