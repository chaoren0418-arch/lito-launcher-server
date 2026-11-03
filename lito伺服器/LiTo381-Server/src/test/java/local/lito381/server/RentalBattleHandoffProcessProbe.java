package local.lito381.server;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/** Separate-JVM probe used by the shared handoff integration test. */
public final class RentalBattleHandoffProcessProbe {
    private static final String ACCOUNT = "cross-process-account";
    private static final String CHARACTER = "source::RentalKnight";

    private RentalBattleHandoffProcessProbe() {}

    public static void main(String[] args) throws Exception {
        if (args.length < 2) throw new IllegalArgumentException("mode and journal required");
        String mode = args[0];
        RentalBattleHandoffStore store =
                new RentalBattleHandoffStore(Path.of(args[1]));
        if ("stage".equals(mode)) {
            RentalLoadoutPlan.Staged staged = RentalLoadoutPlan.require(1, 1).stage(
                    RentalLoadoutPlan.StatePolicy.IDENTIFIED_TABLE_SLOTS,
                    new AtomicLong(0x7400_0000L)::getAndIncrement);
            if (!store.stage(ACCOUNT, CHARACTER, staged.inventory(), 60_000)) {
                throw new IllegalStateException("stage rejected");
            }
            System.out.println("STAGED|" + signature(staged.inventory()));
            finishProbe();
            return;
        }
        if ("consume".equals(mode) && args.length == 3) {
            Path gate = Path.of(args[2]);
            long deadline = System.nanoTime() + 5_000_000_000L;
            while (!Files.exists(gate) && System.nanoTime() < deadline) {
                Thread.sleep(5);
            }
            if (!Files.exists(gate)) throw new IllegalStateException("gate timeout");
            RentalBattleHandoffStore.Stored stored =
                    store.consume(ACCOUNT, CHARACTER);
            System.out.println(stored == null
                    ? "EMPTY" : "CONSUMED|" + signature(stored.inventory()));
            finishProbe();
            return;
        }
        throw new IllegalArgumentException("unknown probe mode");
    }

    private static void finishProbe() {
        System.out.flush();
        System.err.flush();
        // This is a one-shot child JVM. Runtime catalogs can start non-daemon
        // compatibility workers, so returning from main alone is not an exit gate.
        System.exit(0);
    }

    private static String signature(RuntimeInventory inventory) {
        RuntimeInventory.Item weapon = inventory.equippedWeaponItem();
        String counts = inventory.catalogCounts().entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> entry.getKey() + "=" + entry.getValue())
                .collect(Collectors.joining(","));
        return weapon.definition().catalogId() + "|" + weapon.enchantLevel()
                + "|" + inventory.equippedPhysicalWeapon().weaponType()
                + "|" + inventory.equippedWalkAction()
                + "|" + inventory.size() + "|" + counts;
    }
}
