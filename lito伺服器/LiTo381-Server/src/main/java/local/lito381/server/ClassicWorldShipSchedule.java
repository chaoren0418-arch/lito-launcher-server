package local.lito381.server;

/**
 * Exact LiTo 3.81 ship windows expressed against the selected client's
 * localtime view of the retained six-times Lineage clock. Distinct semantic
 * windows stay distinct so future source reconciliation cannot silently
 * couple unrelated routes.
 */
final class ClassicWorldShipSchedule {
    enum Window {
        ALWAYS(0, 86_400, 86_400, 1),
        HIDDEN_DOCK_BOARDING(0, 60 * 60, 3 * 3_600, 8),
        PIRATE_BOARDING(90 * 60, 150 * 60, 3 * 3_600, 8),
        HIDDEN_DOCK_DEPARTURE(90 * 60, 120 * 60, 3 * 3_600, 8),
        PIRATE_ISLAND_DEPARTURE(180 * 60, 210 * 60, 3 * 3_600, 8),
        // LiTo.bin sub_4C51B0: the two ship scenes repeat every four hours.
        TALKING_TO_GLUDIO_BOARDING(8 * 3_600, 9 * 3_600, 4 * 3_600, 4),
        GLUDIO_TO_TALKING_BOARDING(6 * 3_600, 7 * 3_600, 4 * 3_600, 4),
        TALKING_TO_GLUDIO_DEPARTURE(
                9 * 3_600, 9 * 3_600 + 60, 4 * 3_600, 4),
        GLUDIO_TO_TALKING_DEPARTURE(
                7 * 3_600, 7 * 3_600 + 60, 4 * 3_600, 4);

        private final int openSecond;
        private final int closeSecond;
        private final int cycleSeconds;
        private final int occurrences;

        Window(int openSecond, int closeSecond, int cycleSeconds, int occurrences) {
            this.openSecond = openSecond;
            this.closeSecond = closeSecond;
            this.cycleSeconds = cycleSeconds;
            this.occurrences = occurrences;
        }

        boolean openAt(int secondsOfDay) {
            requireSecondsOfDay(secondsOfDay);
            if (this == ALWAYS) return true;
            int duration = closeSecond - openSecond;
            int delta = secondsOfDay - openSecond;
            if (delta < 0 || delta >= (occurrences - 1) * cycleSeconds + duration) {
                return false;
            }
            return delta % cycleSeconds < duration;
        }

        int openingSecond() {
            return openSecond;
        }
    }

    private ClassicWorldShipSchedule() {}

    static Window boardingWindow(int selector) {
        return switch (selector) {
            case 0 -> Window.TALKING_TO_GLUDIO_BOARDING;
            case 1 -> Window.GLUDIO_TO_TALKING_BOARDING;
            default -> null;
        };
    }

    static Window departureWindow(int sourceMap) {
        return switch (sourceMap) {
            case ClassicWorldMapCatalog.HIDDEN_DOCK_SHIP_MAP_ID ->
                    Window.HIDDEN_DOCK_DEPARTURE;
            case ClassicWorldMapCatalog.PIRATE_ISLAND_SHIP_MAP_ID ->
                    Window.PIRATE_ISLAND_DEPARTURE;
            case ClassicWorldMapCatalog.TALKING_ISLAND_TO_GLUDIO_SHIP_MAP_ID ->
                    Window.TALKING_TO_GLUDIO_DEPARTURE;
            case ClassicWorldMapCatalog.GLUDIO_TO_TALKING_ISLAND_SHIP_MAP_ID ->
                    Window.GLUDIO_TO_TALKING_DEPARTURE;
            case ClassicWorldMapCatalog.MAINLAND_TO_FORGOTTEN_ISLAND_SHIP_MAP_ID ->
                    Window.GLUDIO_TO_TALKING_DEPARTURE;
            case ClassicWorldMapCatalog.FORGOTTEN_ISLAND_TO_MAINLAND_SHIP_MAP_ID ->
                    Window.TALKING_TO_GLUDIO_DEPARTURE;
            default -> null;
        };
    }

    static void requireSecondsOfDay(int secondsOfDay) {
        if (secondsOfDay < 0 || secondsOfDay >= 86_400) {
            throw new IllegalArgumentException("seconds of day outside native clock");
        }
    }
}
