// DECOMPILED from base-0.6.71.jar, TODO review
/*
 * Decompiled with CFR 0.152.
 */
package local.lito381.server;

import java.util.Objects;
import java.util.Random;
import local.lito381.server.NativeDeControlCatalog;
import local.lito381.server.NativeDeFactoryControls;
import local.lito381.server.NativeDeInitializerKernel;
import local.lito381.server.NativeDeSprites;

abstract class NativeDeCachedFactoryServices
implements NativeDeInitializerKernel.Services {
    private final NativeDeFactoryControls.Snapshot controls;
    private final Random initializerRandom;
    private final Random tableRandom;

    NativeDeCachedFactoryServices(Random random, Random random2) {
        this(NativeDeFactoryControls.snapshot(), random, random2);
    }

    NativeDeCachedFactoryServices(NativeDeFactoryControls.Snapshot snapshot, Random random, Random random2) {
        this.controls = Objects.requireNonNull(snapshot);
        this.initializerRandom = Objects.requireNonNull(random);
        this.tableRandom = Objects.requireNonNull(random2);
        if (random == random2) {
            throw new IllegalArgumentException("source initializer and setting RNG streams are distinct");
        }
    }

    @Override
    public final int nextInt(int n) {
        return this.initializerRandom.nextInt(n);
    }

    @Override
    public final boolean nextBoolean() {
        return this.initializerRandom.nextBoolean();
    }

    @Override
    public final NativeDeInitializerKernel.Name[] getDeNameList() {
        return (NativeDeInitializerKernel.Name[])this.controls.catalog().names().selection().toArray(NativeDeInitializerKernel.Name[]::new);
    }

    @Override
    public final NativeDeInitializerKernel.Ability getAbility(int n) {
        return this.controls.catalog().abilities().values().get(n);
    }

    @Override
    public final NativeDeInitializerKernel.Setting getSettingById(int n) {
        NativeDeControlCatalog.Definition definition = this.controls.catalog().settings().byId().get(n);
        return definition == null ? null : definition.key();
    }

    @Override
    public final int getRandomPoly(int n, int n2) {
        return this.controls.catalog().settings().poly(n, n2, this.tableRandom::nextInt);
    }

    @Override
    public final int getRandomDoll(int n, int n2) {
        return this.controls.catalog().settings().doll(n, n2, this.tableRandom::nextInt);
    }

    @Override
    public final NativeDeInitializerKernel.PetInfo getPetInfo(int n, int n2) {
        return this.controls.catalog().settings().pet(n, n2, this.tableRandom::nextInt);
    }

    @Override
    public final int getAttackSpeed(int n, int n2) {
        return NativeDeSprites.catalog().attack(n, n2);
    }

    @Override
    public final int getMoveSpeed(int n, int n2) {
        return NativeDeSprites.catalog().move(n, n2);
    }
}
