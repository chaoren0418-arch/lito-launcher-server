package local.lito381.server;

import java.util.Map;
import java.util.Set;

/** Applies one native C28 warehouse transaction on isolated copies. */
final class ClassicWarehouseTransaction {
    static final int PERSONAL_RETRIEVE_ADENA_PER_ROW = 30;
    static final int PLEDGE_RETRIEVE_ADENA_PER_ROW = 30;
    static final int ELF_RETRIEVE_MITHRIL_PER_ROW = 2;

    record Outcome(boolean accepted, String reason,
                   RuntimeInventory inventory, RuntimeInventory warehouse) {
        Outcome {
            if (reason == null || inventory == null || warehouse == null) {
                throw new IllegalArgumentException("warehouse outcome fields are required");
            }
        }
    }

    @FunctionalInterface
    interface AdditionAdmission {
        boolean permits(RuntimeInventory current, InventoryCatalog.Definition definition,
                        long quantity);
    }

    private ClassicWarehouseTransaction() {}

    static Outcome apply(int mode, Map<Long, Long> requested, int submittedRows,
                         Set<Long> offeredObjectIds, RuntimeInventory inventory,
                         RuntimeInventory warehouse) {
        return apply(mode, requested, submittedRows, offeredObjectIds, inventory, warehouse,
                (current, definition, quantity) -> true);
    }

    static Outcome apply(int mode, Map<Long, Long> requested, int submittedRows,
                         Set<Long> offeredObjectIds, RuntimeInventory inventory,
                         RuntimeInventory warehouse, AdditionAdmission admission) {
        boolean depositMode = mode == 2 || mode == 4 || mode == 8;
        boolean retrieveMode = mode == 3 || mode == 5 || mode == 9;
        if ((!depositMode && !retrieveMode) || requested == null || requested.isEmpty()
                || submittedRows <= 0 || offeredObjectIds == null
                || inventory == null || warehouse == null || admission == null) {
            return rejected("invalid-envelope", inventory, warehouse);
        }
        RuntimeInventory nextInventory = inventory.copy();
        RuntimeInventory nextWarehouse = warehouse.copy();

        if (depositMode) {
            for (Map.Entry<Long, Long> entry : requested.entrySet()) {
                RuntimeInventory.Item item = nextInventory.item(entry.getKey());
                long quantity = entry.getValue() == null ? 0 : entry.getValue();
                if (item == null || !offeredObjectIds.contains(entry.getKey())
                        || quantity <= 0 || quantity > item.quantity()
                        || !item.transferable() || item.equipped()
                        || item.durability() != 0) {
                    return rejected("invalid-deposit-row", inventory, warehouse);
                }
                if (!nextWarehouse.add(item, quantity)) {
                    return rejected("warehouse-capacity", inventory, warehouse);
                }
                if (!nextInventory.remove(item.objectId(), quantity)) {
                    throw new IllegalStateException("validated warehouse deposit removal failed");
                }
            }
            return new Outcome(true, "accepted", nextInventory, nextWarehouse);
        }

        for (Map.Entry<Long, Long> entry : requested.entrySet()) {
            RuntimeInventory.Item item = nextWarehouse.item(entry.getKey());
            long quantity = entry.getValue() == null ? 0 : entry.getValue();
            if (item == null || !offeredObjectIds.contains(entry.getKey())
                    || quantity <= 0 || quantity > item.quantity()) {
                return rejected("invalid-retrieve-row", inventory, warehouse);
            }
            RuntimeInventory admitted = nextInventory.copy();
            if (!admitted.add(item, quantity)) {
                return rejected("inventory-capacity", inventory, warehouse);
            }
            if (!admission.permits(nextInventory, item.definition(), quantity)) {
                return rejected("inventory-weight", inventory, warehouse);
            }
            nextInventory = admitted;
            if (!nextWarehouse.remove(item.objectId(), quantity)) {
                throw new IllegalStateException("validated warehouse retrieval removal failed");
            }
        }
        long fee;
        try {
            int unitFee = mode == 9 ? ELF_RETRIEVE_MITHRIL_PER_ROW
                    : mode == 5 ? PLEDGE_RETRIEVE_ADENA_PER_ROW
                    : PERSONAL_RETRIEVE_ADENA_PER_ROW;
            fee = Math.multiplyExact((long) submittedRows, unitFee);
        } catch (ArithmeticException overflow) {
            return rejected("fee-overflow", inventory, warehouse);
        }
        long feeCatalog = mode == 9 ? InventoryCatalog.PURE_MITHRIL : InventoryCatalog.ADENA;
        if (!nextInventory.removeCatalog(feeCatalog, fee)) {
            return rejected(mode == 9 ? "insufficient-mithril" : "insufficient-adena",
                    inventory, warehouse);
        }
        return new Outcome(true, "accepted", nextInventory, nextWarehouse);
    }

    private static Outcome rejected(String reason, RuntimeInventory inventory,
                                    RuntimeInventory warehouse) {
        RuntimeInventory safeInventory = inventory == null ? new RuntimeInventory() : inventory.copy();
        RuntimeInventory safeWarehouse = warehouse == null ? new RuntimeInventory() : warehouse.copy();
        return new Outcome(false, reason, safeInventory, safeWarehouse);
    }
}
