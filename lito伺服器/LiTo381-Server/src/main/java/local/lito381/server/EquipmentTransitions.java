package local.lito381.server;

/** Keep provenance with an inventory retained across a same-character selection.
 * No static session cache: switching source/character still gets fresh bindings. */
final class EquipmentTransitions {
    static Object preserve(Object fallback, ClientSession session, String field) {
        String loaded = (String) NativeRoomRules.get(session, "inventoryCharacter");
        String selected = (String) DurableBattleSettlement.call(session, "stateCharacter", new Class<?>[0]);
        return !loaded.isBlank() && loaded.equalsIgnoreCase(selected)
                ? NativeRoomRules.get(session, field) : fallback;
    }
}
