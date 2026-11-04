// DECOMPILED from base-0.6.71.jar, TODO review
package local.lito381.server;

import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import local.lito381.server.NativeDeDeletionDeState;
import local.lito381.server.NativeDeDeletionOwner;
import local.lito381.server.NativeDeDeletionPorts;
import local.lito381.server.NativeDePerceptionAccess;
import local.lito381.server.NativeDePerceptionCharVisualUpdate;
import local.lito381.server.NativeDePerceptionDoActionShop;
import local.lito381.server.NativeDePerceptionEmblem;
import local.lito381.server.NativeDePerceptionEmblemData;
import local.lito381.server.NativeDePerceptionFishing;
import local.lito381.server.NativeDePerceptionLiquor;
import local.lito381.server.NativeDePerceptionNPCPack_De;
import local.lito381.server.NativeDePerceptionPorts;
import local.lito381.server.NativeDePerceptionScope;
import local.lito381.server.NativeDePerceptionSkillBrave;
import local.lito381.server.NativeDePerceptionSkillHaste;
import local.lito381.server.NativeDePerceptionSpeed;
import local.lito381.server.NativeDeResurrectionEffects;
import local.lito381.server.NativeDeWorldContext;

class NativeDePerceptionState
extends NativeDeDeletionDeState {
    final NativeDePerceptionPorts.Services perception;
    NativeDePerceptionPorts.Shop _shopBehavior;
    NativeDePerceptionPorts.Fishing _fishingBehavior;
    int _aiType = 0;
    int _clanid = 0;
    String _clan_name = null;
    volatile boolean _isBackgroundMode = false;
    final AtomicLong _aiOwnerToken = new AtomicLong(0L);
    private boolean _isShop = false;
    private volatile Set<Integer> perceptionSkillIds = Set.of();

    NativeDePerceptionState(int n, NativeDeResurrectionEffects nativeDeResurrectionEffects, NativeDeDeletionOwner nativeDeDeletionOwner, NativeDePerceptionPorts.Services services) {
        super(n, nativeDeResurrectionEffects, nativeDeDeletionOwner);
        this.perception = Objects.requireNonNull(services);
    }

    NativeDeWorldContext context() {
        NativeDeDeletionPorts.Services services = this.deletionOwner.services;
        if (!(services instanceof NativeDeWorldContext)) {
            throw new IllegalStateException("perception requires concrete application World");
        }
        NativeDeWorldContext nativeDeWorldContext = (NativeDeWorldContext)services;
        return nativeDeWorldContext;
    }

    @Override
    public void onPerceive(NativeDeDeletionPorts.Viewer viewer) {
        NativeDePerceptionScope.run(this, () -> this.sourceOnPerceive(viewer));
    }

    void startAI() {
        this.perception.startAI(this);
    }

    void wakeup() {
        this.perception.wakeup(this);
    }

    @Override
    public boolean hasSkillEffect(int n) {
        return this.perceptionSkillIds.contains(n);
    }

    void skillSnapshot(Set<Integer> set) {
        this.perceptionSkillIds = Set.copyOf(set);
    }

    private void sourceOnPerceive(NativeDeDeletionPorts.Viewer viewer) {
        try {
            Object object;
            if (this._shopBehavior != null && !this._shopBehavior.isShop()) {
                this.start_shop();
            }
            if (!NativeDePerceptionAccess.Pc_tryAddKnownObject(viewer, this)) {
                return;
            }
            viewer.sendPackets(new NativeDePerceptionNPCPack_De(this));
            if (this.getClanid() > 0 && (object = NativeDePerceptionPorts.Emblems.get().get(this.getClanid())) != null) {
                viewer.sendPackets(new NativeDePerceptionEmblem((NativeDePerceptionEmblemData)object));
            }
            if (this.isShop()) {
                if (this._shopBehavior != null) {
                    object = this._shopBehavior.getShopChat();
                    viewer.sendPackets(new NativeDePerceptionDoActionShop(this.getId(), (String)(object != null ? object : "")));
                    viewer.sendPackets(new NativeDePerceptionCharVisualUpdate(this.getId(), 70));
                }
                return;
            }
            if (this.isFishing()) {
                viewer.sendPackets(new NativeDePerceptionFishing(this.getId(), 71, this.get_fishX(), this.get_fishY()));
                return;
            }
            if (!(this._aiType != 3 && this._aiType != 4 && this._aiType != 5 && this._aiType != 6 || this.isAiRunning() && !this._isBackgroundMode)) {
                this.startAI();
                this.wakeup();
            }
            this.sendSpeedStatusTo(viewer);
        }
        catch (Exception exception) {
            NativeDePerceptionScope._log().error(exception.getLocalizedMessage(), exception);
        }
    }

    public boolean isFishing() {
        return this._fishingBehavior != null && this._fishingBehavior.isFishing();
    }

    public int get_fishX() {
        return this._fishingBehavior != null ? this._fishingBehavior.getFishX() : -1;
    }

    public int get_fishY() {
        return this._fishingBehavior != null ? this._fishingBehavior.getFishY() : -1;
    }

    public int getClanid() {
        return this._clanid;
    }

    public String getClanname() {
        return this._clan_name;
    }

    public void start_shop() {
        if (this._shopBehavior != null) {
            this._shopBehavior.start();
        }
    }

    protected final void sendSpeedStatusTo(NativeDeDeletionPorts.Viewer viewer) {
        NativeDePerceptionSpeed.SpeedState speedState;
        int n;
        if (viewer == null) {
            return;
        }
        if (this.getMoveSpeed() != 0) {
            viewer.sendPackets(new NativeDePerceptionSkillHaste(this.getId(), this.getMoveSpeed(), 0));
        }
        if ((n = (speedState = NativeDePerceptionSpeed.stateOf(this)).fastAttackable() ? 6 : (speedState.fastMovable() ? 4 : (speedState.elfBrave() ? 3 : (speedState.brave() ? 1 : 0)))) != 0) {
            viewer.sendPackets(new NativeDePerceptionSkillBrave(this.getId(), n, 0));
        }
        if (speedState.braveX()) {
            viewer.sendPackets(new NativeDePerceptionLiquor(this.getId(), 8));
        }
    }

    public boolean isAiRunning() {
        return this._aiOwnerToken.get() != 0L;
    }

    public boolean isShop() {
        return this._isShop;
    }
}
