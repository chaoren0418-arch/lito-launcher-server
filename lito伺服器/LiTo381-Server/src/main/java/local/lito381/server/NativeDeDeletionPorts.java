// DECOMPILED from base-0.6.71.jar, TODO review
/*
 * Decompiled with CFR 0.152.
 */
package local.lito381.server;

import java.util.ArrayList;
import java.util.concurrent.locks.ReentrantLock;
import local.lito381.server.NativeDeCharacterFields;
import local.lito381.server.NativeDeDeletionOwner;
import local.lito381.server.NativeDeDeletionScope;
import local.lito381.server.NativeDeDeletionState;
import local.lito381.server.NativeDeInitializerKernel;
import local.lito381.server.NativeDeResurrectionEffects;

final class NativeDeDeletionPorts {
    private NativeDeDeletionPorts() {
    }

    static class MprTimer {
        MprTimer() {
        }

        static void remove(Monster monster) {
            NativeDeDeletionScope.owner().services.timer("mpr", monster);
        }
    }

    static class HprTimer {
        HprTimer() {
        }

        static void remove(Monster monster) {
            NativeDeDeletionScope.owner().services.timer("hpr", monster);
        }
    }

    static class DigestTimer {
        DigestTimer() {
        }

        static void remove(NativeDeDeletionState nativeDeDeletionState) {
            NativeDeDeletionScope.owner().services.timer("digest", nativeDeDeletionState);
        }
    }

    static class WorkTimer {
        WorkTimer() {
        }

        static void remove(NativeDeDeletionState nativeDeDeletionState) {
            NativeDeDeletionScope.owner().services.timer("work", nativeDeDeletionState);
        }
    }

    static class RestTimer {
        RestTimer() {
        }

        static void remove(NativeDeDeletionState nativeDeDeletionState) {
            NativeDeDeletionScope.owner().services.timer("rest", nativeDeDeletionState);
        }
    }

    static class DeleteTimer {
        DeleteTimer() {
        }

        static DeleteTimer get() {
            return new DeleteTimer();
        }

        void untrack(NativeDeDeletionState nativeDeDeletionState) {
            NativeDeDeletionScope.owner().services.timer("delete", nativeDeDeletionState);
        }
    }

    static class DeadTimer {
        DeadTimer() {
        }

        static DeadTimer get() {
            return new DeadTimer();
        }

        void untrack(NativeDeDeletionState nativeDeDeletionState) {
            nativeDeDeletionState.deletionOwner.timer.untrack(nativeDeDeletionState);
        }
    }

    static abstract class Monster
    extends NativeDeDeletionState {
        Monster(int n, NativeDeResurrectionEffects nativeDeResurrectionEffects, NativeDeDeletionOwner nativeDeDeletionOwner) {
            super(n, nativeDeResurrectionEffects, nativeDeDeletionOwner);
        }
    }

    static abstract class Illusory
    extends NativeDeDeletionState {
        Illusory(int n, NativeDeResurrectionEffects nativeDeResurrectionEffects, NativeDeDeletionOwner nativeDeDeletionOwner) {
            super(n, nativeDeResurrectionEffects, nativeDeDeletionOwner);
        }
    }

    static abstract class Summon
    extends NativeDeDeletionState {
        Summon(int n, NativeDeResurrectionEffects nativeDeResurrectionEffects, NativeDeDeletionOwner nativeDeDeletionOwner) {
            super(n, nativeDeResurrectionEffects, nativeDeDeletionOwner);
        }

        void setreSpawn(boolean bl) {
            this.respawn = bl;
        }
    }

    static abstract class Doll {
        Doll() {
        }

        abstract void deleteDoll();
    }

    static abstract class Viewer
    extends NativeDeCharacterFields
    implements ObjectRef {
        Viewer(int n) {
            super(2, n);
        }

        abstract OtherList get_otherList();

        abstract void sendPackets(Packet var1);
    }

    static final class RemoveObject
    extends Packet {
        final ObjectRef actor;

        RemoveObject(ObjectRef objectRef) {
            this.actor = objectRef;
        }
    }

    static class Packet {
        Packet() {
        }
    }

    static interface OtherList {
        public void removeIllusoryList(Integer var1);
    }

    static interface MobGroup {
        public int removeMember(NativeDeDeletionState var1);
    }

    static interface Hate {
        public void clear();
    }

    static interface Inventory {
        public void clearItems();
    }

    static interface Template {
        public boolean isBoss();
    }

    static interface Spawn {
        public int getId();

        public String getSpawnConfigTable();

        public void releaseActiveNpc(NativeDeDeletionState var1);

        public void executeSpawnTask(int var1, int var2);
    }

    static abstract class Boss {
        Boss() {
        }

        static Boss get() {
            return NativeDeDeletionScope.owner().services.boss();
        }

        abstract void onBossDeath(Spawn var1, String var2);
    }

    static abstract class DeWorld {
        DeWorld() {
        }

        static DeWorld get() {
            return NativeDeDeletionScope.owner().services.deWorld();
        }

        abstract void remove(String var1);
    }

    static abstract class Quest {
        Quest() {
        }

        static Quest get() {
            return NativeDeDeletionScope.owner().services.quest();
        }

        abstract boolean isQuest(int var1);

        abstract void remove(int var1, NativeDeDeletionState var2);
    }

    static abstract class World {
        World() {
        }

        static World get() {
            return NativeDeDeletionScope.owner().services.world();
        }

        abstract ArrayList<ObjectRef> getVisibleObjects(ObjectRef var1);

        abstract ArrayList<Viewer> getRecognizePlayer(ObjectRef var1);

        abstract boolean isObjectRegistered(ObjectRef var1);

        abstract void removeObject(ObjectRef var1);

        abstract void removeVisibleObject(ObjectRef var1);
    }

    static interface Log {
        public void warn(String var1, Object var2, Object var3, Object var4);

        public void warn(String var1, Object var2, Object var3, Object var4, Object var5);

        public void warn(String var1, Object var2, Object var3, Object var4, Object var5, Object var6);

        public void warn(String var1, Object var2, Object var3, Object var4, Object var5, Object var6, Object var7);

        public void warn(String var1, Object ... var2);

        public void error(String var1, Object var2, Object var3, Object var4);

        public void error(String var1, Object var2, Object var3, Object var4, Object var5);

        public void error(String var1, Object var2, Object var3, Object var4, Object var5, Object var6, Object var7);

        public void error(String var1, Throwable var2);

        public void info(String var1, Object ... var2);
    }

    static interface Services {
        public World world();

        public Quest quest();

        public DeWorld deWorld();

        public Boss boss();

        public Log log();

        public ArrayList<NativeDeInitializerKernel.Name> names();

        public ReentrantLock namesLock();

        public boolean bossAudit();

        public void timer(String var1, ObjectRef var2);

        public void stopAI(ObjectRef var1);

        public void cancelDelayedMove(ObjectRef var1);

        public void cancelChat(ObjectRef var1);

        public void onDeleteClaimed(ObjectRef var1);

        public void allTargetClear(ObjectRef var1);

        public void removeAllKnown(ObjectRef var1);

        public short mapId(ObjectRef var1);
    }

    static interface ObjectRef {
    }
}
