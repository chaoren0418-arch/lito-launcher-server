package local.lito381.server;

import java.nio.file.Path;

/** Test-only subprocess entry. Does not load native Main/config/SQL/migrations. */
public final class ModernEquipmentStoreTestPeer {
    private ModernEquipmentStoreTestPeer() {}
    public static void main(String[] args) throws Exception {
        if (args.length != 2) throw new IllegalArgumentException("invalid test peer invocation");
        try (var store = new ModernEquipmentOverrideStore(Path.of(args[1]))) {
            if (args[0].equals("write")) {
                var committed = ModernEquipmentOverrideStoreTest.commit(store, ModernEquipmentOverrideStoreTest.KEY,
                        ModernEquipmentOverrideStore.Expected.absent(), ModernEquipmentOverrideStoreTest.replacement(true));
                if (!committed.changed() || committed.stored().revision() != 1) throw new AssertionError("peer write failed");
                System.out.println("WRITE_OK");
            } else if (args[0].equals("read")) {
                var value = store.read(ModernEquipmentOverrideStoreTest.KEY, ModernEquipmentOverrideStoreTest.scope()).orElseThrow();
                if (value.revision() != 1 || !value.value().equals(ModernEquipmentOverrideStoreTest.replacement(true))) {
                    throw new AssertionError("peer reopened value mismatch");
                }
                System.out.println("READ_OK");
            } else throw new AssertionError("peer unexpectedly acquired writer lock");
        } catch (ModernEquipmentOverrideStore.StoreException failure) {
            if (!args[0].equals("busy") || failure.failure() != ModernEquipmentOverrideStore.Failure.BUSY) throw failure;
            System.out.println("BUSY_OK");
        }
    }
}
