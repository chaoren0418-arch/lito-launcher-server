// DECOMPILED (CFR 0.152) from reference/base-0.6.71.jar — TODO: replace with original source (T002)
/*
 * Decompiled with CFR 0.152.
 */
package local.lito381.server;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import local.lito381.server.NativeDeDeletionState;

public class NativeDeWorldQuestUser {
    private final CopyOnWriteArrayList<NativeDeDeletionState> _npcList;
    private final AtomicBoolean _mobEmptyNotified = new AtomicBoolean();

    public List<NativeDeDeletionState> npcList() {
        return this._npcList;
    }

    public void addNpc(NativeDeDeletionState npc) {
        if (npc != null) {
            this._npcList.add(npc);
            this._mobEmptyNotified.set(false);
        }
    }

    public void rollbackNpcRegistration(NativeDeDeletionState npc) {
        if (npc != null && this._npcList.remove(npc)) {
            this._mobEmptyNotified.set(false);
        }
    }

    public NativeDeWorldQuestUser() {
        this._npcList = new CopyOnWriteArrayList();
    }
}
