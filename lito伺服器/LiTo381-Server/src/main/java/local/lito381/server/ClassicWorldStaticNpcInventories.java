package local.lito381.server;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Runtime inventories for retained static {@code C_GiveItem} recipients.
 *
 * <p>Live guards and guardians are promoted into
 * {@link ClassicWorldMonsterPopulation}; every other retained receiver keeps
 * one inventory per ordinary-world map instance.  The player-side durable
 * commit completes before the staged NPC bag becomes visible.</p>
 */
final class ClassicWorldStaticNpcInventories {
    static final int STATIC_RECEIVER_POPULATION = 126;
    static final String RETAINED_GIVE_HANDLER_SHA256 =
            "21F4BD7FDAE6D6F30284AE0834E105D5158A82FF9C437B369D319D4178758212";
    static final String RETAINED_NPC_INSTANCE_SHA256 =
            "72E8DC365DAAC1B27D3F2C92672FCC77FF512825C4B6EAF9C96A6A87EA80EAD2";
    static final String SELECTED_DATABASE_SHA256 =
            "DA185BCA20CC254CB175EA2C367A2F5270514F4957B3AC99343C6101A2593766";

    enum GiveAdmission { ACCEPTED, UNKNOWN_TARGET, STALE_POSITION, CAPACITY }

    record Receipt(
            ClassicWorldNpcCatalog.StaticNpc npc, long destinationObjectId,
            int catalogId, int quantity, int lightRange) {
        Receipt {
            if (npc == null || destinationObjectId <= 0 || catalogId <= 0
                    || quantity <= 0 || lightRange < 0 || lightRange > 0xFF) {
                throw new IllegalArgumentException("invalid static NPC give receipt");
            }
        }
    }

    private record Key(int mapId, int instanceId, long objectId) {}

    private final Map<Key, RuntimeInventory> inventories = new LinkedHashMap<>();
    private final Map<Key, Random> randoms = new LinkedHashMap<>();

    synchronized GiveAdmission canReceiveGivenItem(
            int mapId, int instanceId, long objectId, int targetX, int targetY,
            RuntimeInventory.Item item, long quantity) {
        ClassicWorldNpcCatalog.StaticNpc npc = receiver(mapId, objectId);
        if (npc == null || item == null || quantity <= 0
                || quantity > item.quantity() || !item.transferable()) {
            return GiveAdmission.UNKNOWN_TARGET;
        }
        if (npc.x() != targetX || npc.y() != targetY) {
            return GiveAdmission.STALE_POSITION;
        }
        RuntimeInventory inventory = inventories.getOrDefault(
                new Key(mapId, instanceId, objectId), new RuntimeInventory());
        return canCarry(inventory, item, quantity)
                ? GiveAdmission.ACCEPTED : GiveAdmission.CAPACITY;
    }

    synchronized Receipt receiveGivenItemPersisted(
            int mapId, int instanceId, long objectId, int targetX, int targetY,
            RuntimeInventory.Item item, long quantity, DurableCommit commit)
            throws IOException {
        if (commit == null || canReceiveGivenItem(
                mapId, instanceId, objectId, targetX, targetY, item, quantity)
                != GiveAdmission.ACCEPTED) {
            return null;
        }
        ClassicWorldNpcCatalog.StaticNpc npc = receiver(mapId, objectId);
        Key key = new Key(mapId, instanceId, objectId);
        RuntimeInventory staged = inventories.getOrDefault(
                key, new RuntimeInventory()).copy();
        RuntimeInventory.Item destination = staged.addTransferredResult(item, quantity);
        if (destination == null) {
            throw new IllegalStateException("validated static NPC C_GIVE transfer changed");
        }
        // Retained L1NpcInstance.onGetItem runs refineItem, shuffle, then
        // optional digest.  The selected static receiver templates have no
        // refinement rule and digestitem=0, so shuffle is the only mutation.
        staged.shuffle(randoms.computeIfAbsent(key,
                ignored -> new Random(seed(key))));
        commit.commit();
        inventories.put(key, staged);
        return new Receipt(npc, destination.objectId(),
                Math.toIntExact(item.definition().catalogId()),
                Math.toIntExact(quantity),
                Math.max(npc.lightSize(), staged.activeLightRange()));
    }

    synchronized List<RuntimeInventory.Item> carriedItems(
            int mapId, int instanceId, long objectId) {
        RuntimeInventory inventory = inventories.get(
                new Key(mapId, instanceId, objectId));
        return inventory == null ? List.of() : inventory.items();
    }

    synchronized int lightRange(int mapId, int instanceId, long objectId) {
        ClassicWorldNpcCatalog.StaticNpc npc = receiver(mapId, objectId);
        if (npc == null) return 0;
        RuntimeInventory inventory = inventories.get(
                new Key(mapId, instanceId, objectId));
        return Math.max(npc.lightSize(),
                inventory == null ? 0 : inventory.activeLightRange());
    }

    static boolean receiverImplementation(String implementation) {
        return implementation != null && switch (implementation) {
            case "L1Npc", "L1Monster", "L1Guardian", "L1Teleporter", "L1Guard" -> true;
            default -> false;
        };
    }

    static boolean staticReceiver(ClassicWorldNpcCatalog.StaticNpc npc) {
        return npc != null && receiverImplementation(npc.implementation())
                && !ClassicWorldGuardianCatalog.isGuardian(npc)
                && !ClassicWorldGuardCatalog.isGuard(npc);
    }

    private static ClassicWorldNpcCatalog.StaticNpc receiver(
            int mapId, long objectId) {
        ClassicWorldNpcCatalog.StaticNpc npc =
                ClassicWorldNpcCatalog.npc(mapId, objectId);
        return staticReceiver(npc) ? npc : null;
    }

    private static boolean canCarry(
            RuntimeInventory inventory, RuntimeInventory.Item source, long quantity) {
        boolean stackTarget = source.definition().stackable()
                && inventory.items().stream().anyMatch(existing ->
                        existing.definition().catalogId()
                                == source.definition().catalogId()
                        && existing.definition().equals(source.definition())
                        && existing.tournamentReward() == source.tournamentReward());
        if (inventory.size() > ClassicWorldMonsterPickupCatalog.MAX_NPC_ITEMS
                || inventory.size() == ClassicWorldMonsterPickupCatalog.MAX_NPC_ITEMS
                    && !stackTarget) {
            return false;
        }
        int addedWeight = BattleInventoryWeight.itemWeight(
                quantity, source.definition().unitWeight());
        if (BattleInventoryWeight.addWeight(inventory.totalWeight(), addedWeight)
                > ClassicWorldMonsterPickupCatalog.MAX_NPC_WEIGHT) {
            return false;
        }
        return inventory.copy().addTransferred(source, quantity);
    }

    private static long seed(Key key) {
        return 0x4C31535441544943L
                ^ Integer.toUnsignedLong(key.mapId()) << 32
                ^ Integer.toUnsignedLong(key.instanceId())
                ^ key.objectId();
    }
}
