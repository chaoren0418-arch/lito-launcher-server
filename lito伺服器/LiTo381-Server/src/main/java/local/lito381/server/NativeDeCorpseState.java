// DECOMPILED (CFR 0.152) from reference/base-0.6.71.jar — TODO: replace with original source (T002)
/*
 * Decompiled with CFR 0.152.
 */
package local.lito381.server;

import java.io.IOException;
import java.util.List;
import java.util.Objects;
import local.lito381.server.NativeDeCharacterFields;
import local.lito381.server.NativeDeCorpseClock;
import local.lito381.server.NativeDeCorpseCore;
import local.lito381.server.NativeDeCorpseTimers;
import local.lito381.server.NativeDeNpcVitalState;
import local.lito381.server.NativeDeResurrectableState;
import local.lito381.server.NativeDeResurrectionEffects;

class NativeDeCorpseState
extends NativeDeResurrectableState
implements NativeDeCorpseCore.Npc {
    final NativeDeCorpseTimers.Owner owner;

    NativeDeCorpseState(int n, NativeDeCharacterFields.Snapshot snapshot, NativeDeResurrectionEffects nativeDeResurrectionEffects, NativeDeCorpseTimers.Owner owner) throws IOException {
        super(n, snapshot, (NativeDeResurrectionEffects)new Effects(nativeDeResurrectionEffects, owner));
        this.owner = Objects.requireNonNull(owner);
    }

    NativeDeCorpseState(int n, int n2, NativeDeResurrectionEffects nativeDeResurrectionEffects, NativeDeCorpseTimers.Owner owner) {
        super(n, n2, (NativeDeResurrectionEffects)new Effects(nativeDeResurrectionEffects, owner));
        this.owner = Objects.requireNonNull(owner);
    }

    @Override
    protected long corpseNanoTime() {
        return this.owner.nanoTime();
    }

    @Override
    protected NativeDeCorpseClock corpseClock() {
        return this.owner;
    }

    @Override
    public int getId() {
        return super.getId();
    }

    @Override
    public int getNpcId() {
        return super.getNpcId();
    }

    @Override
    public int getX() {
        return super.getX();
    }

    @Override
    public int getY() {
        return super.getY();
    }

    @Override
    public short getMapId() {
        return this.owner.deletion.mapId(this);
    }

    @Override
    public void deleteMe() {
        this.owner.deletion.delete(this);
    }

    private static final class Effects
    implements NativeDeResurrectionEffects {
        final NativeDeResurrectionEffects delegate;
        final NativeDeCorpseTimers.Owner owner;

        Effects(NativeDeResurrectionEffects nativeDeResurrectionEffects, NativeDeCorpseTimers.Owner owner) {
            this.delegate = Objects.requireNonNull(nativeDeResurrectionEffects);
            this.owner = Objects.requireNonNull(owner);
        }

        @Override
        public NativeDeResurrectionEffects.RemoveObject newRemoveObject(int n) {
            return this.delegate.newRemoveObject(n);
        }

        @Override
        public void undoPoly(NativeDeCharacterFields nativeDeCharacterFields) {
            this.delegate.undoPoly(nativeDeCharacterFields);
        }

        @Override
        public List<? extends NativeDeResurrectionEffects.Viewer> recognizePlayers(NativeDeCharacterFields nativeDeCharacterFields) {
            return this.delegate.recognizePlayers(nativeDeCharacterFields);
        }

        @Override
        public void characterFailure(String string, Exception exception) {
            this.delegate.characterFailure(string, exception);
        }

        @Override
        public void corpseRejected(int n, int n2, long l) {
            this.delegate.corpseRejected(n, n2, l);
        }

        @Override
        public int npcId(NativeDeNpcVitalState nativeDeNpcVitalState) {
            return this.delegate.npcId(nativeDeNpcVitalState);
        }

        @Override
        public int x(NativeDeNpcVitalState nativeDeNpcVitalState) {
            return this.delegate.x(nativeDeNpcVitalState);
        }

        @Override
        public int y(NativeDeNpcVitalState nativeDeNpcVitalState) {
            return this.delegate.y(nativeDeNpcVitalState);
        }

        @Override
        public void broadcastResurrection(NativeDeCharacterFields nativeDeCharacterFields, NativeDeCharacterFields nativeDeCharacterFields2, int n) {
            this.delegate.broadcastResurrection(nativeDeCharacterFields, nativeDeCharacterFields2, n);
        }

        @Override
        public NativeDeResurrectionEffects.LoginSkill newSkillUse() {
            return this.delegate.newSkillUse();
        }

        @Override
        public void untrackCorpse(NativeDeNpcVitalState nativeDeNpcVitalState) {
            this.owner.untrack(nativeDeNpcVitalState);
        }
    }
}
