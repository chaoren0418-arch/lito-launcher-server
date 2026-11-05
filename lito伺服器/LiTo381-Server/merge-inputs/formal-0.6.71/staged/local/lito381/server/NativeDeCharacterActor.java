// DECOMPILED from base-0.6.71.jar, TODO review
/*
 * Decompiled with CFR 0.152.
 */
package local.lito381.server;

import java.io.IOException;
import java.util.Map;
import java.util.Objects;
import local.lito381.server.NativeDeCharacterFields;
import local.lito381.server.NativeDeCorpseState;
import local.lito381.server.NativeDeCorpseTimers;
import local.lito381.server.NativeDeInitializerKernel;
import local.lito381.server.NativeDeResurrectableState;
import local.lito381.server.NativeDeResurrectionEffects;
import local.lito381.server.NativeDeVitalState;

final class NativeDeCharacterActor
implements NativeDeInitializerKernel.Actor {
    private final NativeDeInitializerKernel.Actor npc;
    private final NativeDeCharacterFields fields;

    NativeDeCharacterActor(NativeDeInitializerKernel.Actor actor, NativeDeCharacterFields.Snapshot snapshot) throws IOException {
        this.npc = Objects.requireNonNull(actor);
        this.fields = new NativeDeResurrectableState(actor.getId(), snapshot);
    }

    NativeDeCharacterActor(NativeDeInitializerKernel.Actor actor, NativeDeCharacterFields.Snapshot snapshot, NativeDeResurrectionEffects nativeDeResurrectionEffects) throws IOException {
        this.npc = Objects.requireNonNull(actor);
        this.fields = new NativeDeResurrectableState(actor.getId(), snapshot, nativeDeResurrectionEffects);
    }

    NativeDeCharacterActor(NativeDeInitializerKernel.Actor actor, NativeDeCharacterFields.Snapshot snapshot, NativeDeResurrectionEffects nativeDeResurrectionEffects, NativeDeCorpseTimers.Owner owner) throws IOException {
        this.npc = Objects.requireNonNull(actor);
        this.fields = new NativeDeCorpseState(actor.getId(), snapshot, nativeDeResurrectionEffects, owner);
    }

    NativeDeVitalState vitalState() {
        return (NativeDeVitalState)this.fields;
    }

    NativeDeCharacterFields.Snapshot snapshot() {
        return this.fields.snapshot();
    }

    @Override
    public void setLevel(int n) {
        this.fields.setLevel(n);
    }

    @Override
    public int getLevel() {
        return this.fields.getLevel();
    }

    @Override
    public void setMaxHp(int n) {
        this.fields.setMaxHp(n);
    }

    @Override
    public void setCurrentHp(int n) {
        this.fields.setCurrentHp(n);
    }

    @Override
    public void setMaxMp(int n) {
        this.fields.setMaxMp(n);
    }

    @Override
    public void setCurrentMp(int n) {
        this.fields.setCurrentMp(n);
    }

    @Override
    public void setAc(int n) {
        this.fields.setAc(n);
    }

    @Override
    public void setLawful(int n) {
        this.fields.setLawful(n);
    }

    @Override
    public void setStr(int n) {
        this.fields.setStr(n);
    }

    @Override
    public void setDex(int n) {
        this.fields.setDex(n);
    }

    @Override
    public void setCon(int n) {
        this.fields.setCon(n);
    }

    @Override
    public void setInt(int n) {
        this.fields.setInt(n);
    }

    @Override
    public short getInt() {
        return this.fields.getInt();
    }

    @Override
    public void setWis(int n) {
        this.fields.setWis(n);
    }

    @Override
    public void setCha(int n) {
        this.fields.setCha(n);
    }

    @Override
    public void addSp(int n) {
        this.fields.addSp(n);
    }

    @Override
    public void addDmgup(int n) {
        this.fields.addDmgup(n);
    }

    @Override
    public void addHitup(int n) {
        this.fields.addHitup(n);
    }

    @Override
    public void setTempCharGfx(int n) {
        this.fields.setTempCharGfx(n);
    }

    @Override
    public int getTempCharGfx() {
        return this.fields.getTempCharGfx();
    }

    @Override
    public void setGfxId(int n) {
        this.fields.setGfxId(n);
    }

    @Override
    public void setStatus(int n) {
        this.fields.setStatus(n);
    }

    @Override
    public boolean isMorphEnable() {
        return this.npc.isMorphEnable();
    }

    @Override
    public int getId() {
        return this.npc.getId();
    }

    @Override
    public void setInitFailed(boolean bl) {
        this.npc.setInitFailed(bl);
    }

    @Override
    public void set_deName(NativeDeInitializerKernel.Name name) {
        this.npc.set_deName(name);
    }

    @Override
    public void removeFromDeNameList() {
        this.npc.removeFromDeNameList();
    }

    @Override
    public void setNameId(String string) {
        this.npc.setNameId(string);
    }

    @Override
    public String getNameId() {
        return this.npc.getNameId();
    }

    @Override
    public void setClassId(int n) {
        this.npc.setClassId(n);
    }

    @Override
    public int getClassId() {
        return this.npc.getClassId();
    }

    @Override
    public void setCurrentWeapon(int n) {
        this.npc.setCurrentWeapon(n);
    }

    @Override
    public int getCurrentWeapon() {
        return this.npc.getCurrentWeapon();
    }

    @Override
    public void set_ranged(int n) {
        this.npc.set_ranged(n);
    }

    @Override
    public void setBowActId(int n) {
        this.npc.setBowActId(n);
    }

    @Override
    public void setMoveSpeed(int n) {
        this.npc.setMoveSpeed(n);
    }

    @Override
    public void setBraveSpeed(int n) {
        this.npc.setBraveSpeed(n);
    }

    @Override
    public void setPassispeed(int n) {
        this.npc.setPassispeed(n);
    }

    @Override
    public void setAtkspeed(int n) {
        this.npc.setAtkspeed(n);
    }

    @Override
    public void set_clanid(int n) {
        this.npc.set_clanid(n);
    }

    @Override
    public void set_clanname(String string) {
        this.npc.set_clanname(string);
    }

    @Override
    public void set_emblem(NativeDeInitializerKernel.Emblem emblem) {
        this.npc.set_emblem(emblem);
    }

    @Override
    public void setTitle(String string) {
        this.npc.setTitle(string);
    }

    @Override
    public void broadcastPacketAll(NativeDeInitializerKernel.Packet packet) {
        this.npc.broadcastPacketAll(packet);
    }

    @Override
    public Map<?, ?> getDolls() {
        return this.npc.getDolls();
    }

    @Override
    public int getHeading() {
        return this.npc.getHeading();
    }
}
