package local.lito381.server;

import java.util.Set;

/** Selected ConfigAlt.DORP_ITEM and ItemDropTable projection for C_DROP. */
final class NativePlayerDropCatalog {
    static final boolean RETAINED_GLOBAL_DROP_ENABLED = false;
    static final Set<Long> RETAINED_FORCE_DROPPABLE = Set.of(40056L);

    private NativePlayerDropCatalog() {}

    static boolean permits(boolean globalDropEnabled, boolean administrator,
                           long catalogId) {
        return administrator || globalDropEnabled
                || RETAINED_FORCE_DROPPABLE.contains(catalogId);
    }
}
