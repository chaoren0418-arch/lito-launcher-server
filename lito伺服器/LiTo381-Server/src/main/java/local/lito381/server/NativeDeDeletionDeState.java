// DECOMPILED (CFR 0.152) from reference/base-0.6.71.jar — TODO: replace with original source (T002)
/*
 * Decompiled with CFR 0.152.
 */
package local.lito381.server;

import java.util.ArrayList;
import local.lito381.server.NativeDeDeletionOwner;
import local.lito381.server.NativeDeDeletionPorts;
import local.lito381.server.NativeDeDeletionScope;
import local.lito381.server.NativeDeDeletionState;
import local.lito381.server.NativeDeInitializerKernel;
import local.lito381.server.NativeDeResurrectionEffects;

class NativeDeDeletionDeState
extends NativeDeDeletionState {
    NativeDeDeletionDeState(int n, NativeDeResurrectionEffects nativeDeResurrectionEffects, NativeDeDeletionOwner nativeDeDeletionOwner) {
        super(n, nativeDeResurrectionEffects, nativeDeDeletionOwner);
    }

    @Override
    public void deleteMe() {
        NativeDeDeletionScope.run(this.deletionOwner, this::sourceDeleteDe);
    }

    void stopAI() {
        this.deletionOwner.services.stopAI(this);
    }

    void cancelDelayedMove() {
        this.deletionOwner.services.cancelDelayedMove(this);
    }

    NativeDeInitializerKernel.Name get_deName() {
        return this.deName;
    }

    static ArrayList<NativeDeInitializerKernel.Name> getDeNameList() {
        return NativeDeDeletionScope.owner().services.names();
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public void sourceDeleteDe() {
        block16: {
            NativeDeDeletionScope.require(this);
            try {
                ArrayList<NativeDeInitializerKernel.Name> denameList;
                this.stopAI();
                this.cancelDelayedMove();
                try {
                    if (!this.getPetList().isEmpty()) {
                        Object[] pets;
                        for (Object obj : pets = this.getPetList().values().toArray()) {
                            try {
                                NativeDeDeletionPorts.Summon pet;
                                if (!(obj instanceof NativeDeDeletionPorts.Summon) || (pet = (NativeDeDeletionPorts.Summon)obj) == null) continue;
                                pet.setreSpawn(false);
                                pet.allTargetClear();
                                pet.deleteMe();
                            }
                            catch (Exception e) {
                                NativeDeDeletionScope._log().error("\u55ae\u4e00\u5bf5\u7269\u6e05\u7406\u5931\u6557", e);
                            }
                        }
                    }
                }
                catch (Exception e) {
                    NativeDeDeletionScope._log().error("\u5047\u4eba deleteMe \u6e05\u7406\u5bf5\u7269\u7570\u5e38: " + this.getName(), e);
                }
                String nameId = this.getNameId();
                if (nameId != null && !nameId.isEmpty()) {
                    NativeDeDeletionPorts.DeWorld.get().remove(nameId);
                }
                if (this.get_deName() == null || (denameList = NativeDeDeletionDeState.getDeNameList()) == null) break block16;
                NativeDeDeletionScope._denameListLock().lock();
                try {
                    denameList.remove(this.get_deName());
                }
                finally {
                    NativeDeDeletionScope._denameListLock().unlock();
                }
            }
            catch (Exception e) {
                NativeDeDeletionScope._log().error("\u5047\u4eba deleteMe \u57f7\u884c\u5931\u6557: " + this.getNameId(), e);
            }
            finally {
                if (this.getInventory() != null) {
                    this.getInventory().clearItems();
                }
                super.sourceDeleteNpc();
            }
        }
    }
}
