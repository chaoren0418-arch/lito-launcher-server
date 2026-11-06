package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

final class ClassicWorldDungeonCatalogTest {
    @Test
    void selectedSourcePinsAllEightMap445And447Rows() {
        List<ClassicWorldDungeonCatalog.Route> routes =
                ClassicWorldDungeonCatalog.all();
        // Routes 251-260 are Windawood, 261-272 Kent/Ober, 273-314
        // Ivory Tower, 315-318 Dwarf Castle/Cave and 319-371 Aden/TOI.
        assertEquals(ClassicWorldDungeonCatalog.TOTAL_ROUTE_COUNT, routes.size());
        assertEquals(486, ClassicWorldDungeonCatalog.PRE_378_ROUTE_COUNT);
        assertEquals(List.of(1, 2, 3, 4, 5, 6, 7, 8),
                routes.subList(0, 8).stream().map(
                        ClassicWorldDungeonCatalog.Route::routeId).toList());

        ClassicWorldDungeonCatalog.Route exit =
                ClassicWorldDungeonCatalog.find(445, 32738, 32851);
        assertEquals(4, exit.destinationMapId());
        assertEquals(32738, exit.destinationX());
        assertEquals(32475, exit.destinationY());
        assertEquals(2, exit.destinationHeading());
        assertEquals(0, exit.requiredItemCatalogId());
        assertFalse(exit.scheduledPirateShip());

        ClassicWorldDungeonCatalog.Route board =
                ClassicWorldDungeonCatalog.find(445, 32751, 32874);
        assertEquals(447, board.destinationMapId());
        assertEquals(32732, board.destinationX());
        assertEquals(32797, board.destinationY());
        assertEquals(4, board.destinationHeading());
        assertEquals(InventoryCatalog.SHIP_TICKET_PIRATE_ISLAND,
                board.requiredItemCatalogId());
        assertTrue(board.scheduledPirateShip());

        ClassicWorldDungeonCatalog.Route returnRoute =
                ClassicWorldDungeonCatalog.find(447, 32732, 32796);
        assertEquals(445, returnRoute.destinationMapId());
        assertEquals(32751, returnRoute.destinationX());
        assertEquals(32873, returnRoute.destinationY());
        assertEquals(0, returnRoute.destinationHeading());
        assertNull(ClassicWorldDungeonCatalog.find(447, 32734, 32796));

        // 正式 0.6.71 的原始地圖保留這 16 個無安全落點的路線；不得以修正版地圖代替。
        assertEquals(List.of(287, 288, 289, 290, 291, 292, 293, 294,
                299, 300, 301, 302, 307, 308, 309, 310), routes.stream()
                .filter(route -> !ClassicWorldDungeonCatalog
                        .destinationLandingSafe(route))
                .map(ClassicWorldDungeonCatalog.Route::routeId).toList());
    }

    @Test
    void selectedSourcePinsAllFourMap4And17NerupaRows() {
        assertEquals("B78983DE9C8E96C1AD19A68CE72761CCDD5211F023069C6E8245A04BEF7F980E",
                ClassicWorldDungeonCatalog.NERUPA_SOURCE_SHA256);
        ClassicWorldDungeonCatalog.Route enter =
                ClassicWorldDungeonCatalog.find(4, 33053, 32329);
        assertEquals(17, enter.destinationMapId());
        assertEquals(32747, enter.destinationX());
        assertEquals(32809, enter.destinationY());
        assertEquals(6, enter.destinationHeading());
        assertEquals(0, enter.requiredItemCatalogId());
        assertFalse(enter.scheduledPirateShip());

        ClassicWorldDungeonCatalog.Route secondEnter =
                ClassicWorldDungeonCatalog.find(4, 33054, 32329);
        assertEquals(17, secondEnter.destinationMapId());
        ClassicWorldDungeonCatalog.Route exit =
                ClassicWorldDungeonCatalog.find(17, 32748, 32809);
        assertEquals(4, exit.destinationMapId());
        assertEquals(33054, exit.destinationX());
        assertEquals(32328, exit.destinationY());
        assertEquals(0, exit.destinationHeading());
        assertEquals(4, ClassicWorldDungeonCatalog.all().stream()
                .filter(route -> route.sourceMapId() == 17
                        || route.destinationMapId() == 17).count());
    }

    @Test
    void selectedSourcePinsHartinLaboratoryRoundTrip() {
        assertEquals("7CC853A371A9D70C20F0B866D3CD52E17DE085BE25555BF7BBE7FBD33E63781D",
                ClassicWorldDungeonCatalog.HARTIN_SOURCE_SHA256);
        assertEquals("6849E165571F389F84B687B48A36B66EC907B7894BD2639598A7239464AA63DF",
                ClassicWorldDungeonCatalog.HARTIN_NORMALIZED_SOURCE_SHA256);

        ClassicWorldDungeonCatalog.Route enter =
                ClassicWorldDungeonCatalog.find(4, 33194, 32457);
        assertEquals(63, enter.routeId());
        assertEquals(16, enter.destinationMapId());
        assertEquals(32744, enter.destinationX());
        assertEquals(32813, enter.destinationY());
        assertEquals(4, enter.destinationHeading());
        assertEquals(0, enter.requiredItemCatalogId());

        ClassicWorldDungeonCatalog.Route exit =
                ClassicWorldDungeonCatalog.find(16, 32744, 32815);
        assertEquals(64, exit.routeId());
        assertEquals(4, exit.destinationMapId());
        assertEquals(33194, exit.destinationX());
        assertEquals(32458, exit.destinationY());
        assertEquals(0, exit.destinationHeading());
        assertEquals(2, ClassicWorldDungeonCatalog.all().stream()
                .filter(route -> route.sourceMapId() == 16
                        || route.destinationMapId() == 16).count());
    }

    @Test
    void selectedSourcePinsValakasHabitatRoundTripWithoutInventedBossGate() {
        assertEquals("8C2CFC56BE3D9C59A744C98DEBC5F0274B65B12AB156E19761B7E119DAA80FE9",
                ClassicWorldDungeonCatalog.VALAKAS_SOURCE_SHA256);
        assertEquals("39FC379EFAADB868E5ECA3DD7E2A35213E0270FCB2ED4A796E84D987902D49D7",
                ClassicWorldDungeonCatalog.VALAKAS_NORMALIZED_SOURCE_SHA256);

        ClassicWorldDungeonCatalog.Route enter =
                ClassicWorldDungeonCatalog.find(4, 33734, 32216);
        assertEquals(65, enter.routeId());
        assertEquals(67, enter.destinationMapId());
        assertEquals(32797, enter.destinationX());
        assertEquals(32746, enter.destinationY());
        assertEquals(6, enter.destinationHeading());
        assertEquals(0, enter.requiredItemCatalogId());
        assertFalse(enter.scheduledPirateShip());

        ClassicWorldDungeonCatalog.Route exit =
                ClassicWorldDungeonCatalog.find(67, 32798, 32746);
        assertEquals(66, exit.routeId());
        assertEquals(4, exit.destinationMapId());
        assertEquals(33734, exit.destinationX());
        assertEquals(32217, exit.destinationY());
        assertEquals(4, exit.destinationHeading());
        assertEquals(2, ClassicWorldDungeonCatalog.all().stream()
                .filter(route -> route.sourceMapId() == 67
                        || route.destinationMapId() == 67).count());
    }

    @Test
    void selectedSourcePinsSleepingDragonMap19Through21Chain() {
        assertEquals("4EFDF0BB261EB7AAD0B49FBB14DDAF16AFAD675DDC6540F278A51B645BF0099F",
                ClassicWorldDungeonCatalog.SLEEPING_DRAGON_SOURCE_SHA256);
        assertEquals("FFD6A07188C07855A616E6FFD8A409F3A330CA67E0FEF4B9299943EF3A7BFBEE",
                ClassicWorldDungeonCatalog.SLEEPING_DRAGON_SECONDARY_SOURCE_SHA256);

        ClassicWorldDungeonCatalog.Route enter =
                ClassicWorldDungeonCatalog.find(4, 32938, 32284);
        assertEquals(19, enter.destinationMapId());
        assertEquals(32805, enter.destinationX());
        assertEquals(32724, enter.destinationY());
        assertEquals(4, enter.destinationHeading());

        ClassicWorldDungeonCatalog.Route firstToSecond =
                ClassicWorldDungeonCatalog.find(19, 32772, 32783);
        assertEquals(20, firstToSecond.destinationMapId());
        assertEquals(32766, firstToSecond.destinationX());
        assertEquals(32796, firstToSecond.destinationY());
        assertEquals(4, firstToSecond.destinationHeading());

        ClassicWorldDungeonCatalog.Route secondToThird =
                ClassicWorldDungeonCatalog.find(20, 32730, 32732);
        assertEquals(21, secondToThird.destinationMapId());
        assertEquals(32731, secondToThird.destinationX());
        assertEquals(32736, secondToThird.destinationY());
        assertEquals(6, secondToThird.destinationHeading());

        ClassicWorldDungeonCatalog.Route thirdToSecond =
                ClassicWorldDungeonCatalog.find(21, 32732, 32736);
        assertEquals(20, thirdToSecond.destinationMapId());
        assertEquals(32731, thirdToSecond.destinationX());
        assertEquals(32732, thirdToSecond.destinationY());
        assertEquals(2, thirdToSecond.destinationHeading());

        ClassicWorldDungeonCatalog.Route secondToFirst =
                ClassicWorldDungeonCatalog.find(20, 32767, 32795);
        assertEquals(19, secondToFirst.destinationMapId());
        assertEquals(32772, secondToFirst.destinationX());
        assertEquals(32782, secondToFirst.destinationY());
        assertEquals(0, secondToFirst.destinationHeading());

        ClassicWorldDungeonCatalog.Route returnToWorld =
                ClassicWorldDungeonCatalog.find(19, 32805, 32723);
        assertEquals(4, returnToWorld.destinationMapId());
        assertEquals(32938, returnToWorld.destinationX());
        assertEquals(32283, returnToWorld.destinationY());
        assertEquals(0, returnToWorld.destinationHeading());
        assertEquals(12, ClassicWorldDungeonCatalog.all().stream()
                .filter(route -> route.routeId() >= 13
                        && route.routeId() <= 24).count());
    }

    @Test
    void selectedSourcePinsMap21InternalPassageAndMap32RoundTrip() {
        assertEquals("C7334C395E08E430C2041F7AD467DFD99FBA9436A9FE65C453E8A076FD26E843",
                ClassicWorldDungeonCatalog.DRAGON_VALLEY_THIRD_FLOOR_SOURCE_SHA256);
        assertEquals("E66E4809F177C6B81EC0AB1EA89CC60812877C2133EAAF0FABC970D35A674AB5",
                ClassicWorldDungeonCatalog.DRAGON_VALLEY_THIRD_FLOOR_SECONDARY_SOURCE_SHA256);

        ClassicWorldDungeonCatalog.Route internal =
                ClassicWorldDungeonCatalog.find(21, 32736, 32799);
        assertEquals(25, internal.routeId());
        assertEquals(21, internal.destinationMapId());
        assertEquals(32760, internal.destinationX());
        assertEquals(32763, internal.destinationY());
        assertEquals(0, internal.destinationHeading());

        ClassicWorldDungeonCatalog.Route internalReturn =
                ClassicWorldDungeonCatalog.find(21, 32760, 32764);
        assertEquals(26, internalReturn.routeId());
        assertEquals(21, internalReturn.destinationMapId());
        assertEquals(32737, internalReturn.destinationX());
        assertEquals(32799, internalReturn.destinationY());
        assertEquals(2, internalReturn.destinationHeading());

        ClassicWorldDungeonCatalog.Route enter =
                ClassicWorldDungeonCatalog.find(21, 32777, 32796);
        assertEquals(27, enter.routeId());
        assertEquals(32, enter.destinationMapId());
        assertEquals(32657, enter.destinationX());
        assertEquals(32872, enter.destinationY());
        assertEquals(2, enter.destinationHeading());

        ClassicWorldDungeonCatalog.Route exit =
                ClassicWorldDungeonCatalog.find(32, 32657, 32872);
        assertEquals(28, exit.routeId());
        assertEquals(21, exit.destinationMapId());
        assertEquals(32777, exit.destinationX());
        assertEquals(32796, exit.destinationY());
        assertEquals(0, exit.destinationHeading());
        assertEquals(4, ClassicWorldDungeonCatalog.all().stream()
                .filter(route -> route.routeId() >= 25
                        && route.routeId() <= 28).count());
    }

    @Test
    void selectedSourcePinsMap31Through33DragonValleyTransitions() {
        assertEquals("37ED8FCA63742F9E848C923280E2946B33189136BF19E6A39C882BD135A71969",
                ClassicWorldDungeonCatalog.DRAGON_VALLEY_FLOOR_TRANSITIONS_SOURCE_SHA256);
        assertEquals("C10803A5B830BEABB06DC8EBD7D2EA7E0ECA2C010032726693B8419B5C0C367B",
                ClassicWorldDungeonCatalog
                        .DRAGON_VALLEY_FLOOR_TRANSITIONS_SECONDARY_SOURCE_SHA256);

        ClassicWorldDungeonCatalog.Route secondToThird =
                ClassicWorldDungeonCatalog.find(31, 32772, 32747);
        assertEquals(29, secondToThird.routeId());
        assertEquals(32, secondToThird.destinationMapId());
        assertEquals(32709, secondToThird.destinationX());
        assertEquals(32818, secondToThird.destinationY());
        assertEquals(4, secondToThird.destinationHeading());

        ClassicWorldDungeonCatalog.Route thirdToSecond =
                ClassicWorldDungeonCatalog.find(32, 32710, 32817);
        assertEquals(31, thirdToSecond.destinationMapId());
        assertEquals(32773, thirdToSecond.destinationX());
        assertEquals(32747, thirdToSecond.destinationY());
        assertEquals(1, thirdToSecond.destinationHeading());

        for (int sourceX : List.of(32746, 32747)) {
            ClassicWorldDungeonCatalog.Route thirdToFourth =
                    ClassicWorldDungeonCatalog.find(32, sourceX, 32859);
            assertEquals(33, thirdToFourth.destinationMapId());
            assertEquals(32670, thirdToFourth.destinationX());
            assertEquals(32868, thirdToFourth.destinationY());
            assertEquals(4, thirdToFourth.destinationHeading());
        }
        ClassicWorldDungeonCatalog.Route fourthToThird =
                ClassicWorldDungeonCatalog.find(33, 32670, 32867);
        assertEquals(33, fourthToThird.routeId());
        assertEquals(32, fourthToThird.destinationMapId());
        assertEquals(32746, fourthToThird.destinationX());
        assertEquals(32858, fourthToThird.destinationY());
        assertEquals(0, fourthToThird.destinationHeading());
        assertEquals(5, ClassicWorldDungeonCatalog.all().stream()
                .filter(route -> route.routeId() >= 29
                        && route.routeId() <= 33).count());
    }

    @Test
    void selectedSourcePinsAllMap30WorldAndSecondFloorTransitions() {
        assertEquals("B6C72F26DABD05BB1667C43DABBC3E8167DE1A1187DB6FD9D89846C62423658D",
                ClassicWorldDungeonCatalog
                        .DRAGON_VALLEY_FIRST_FLOOR_TRANSITIONS_SOURCE_SHA256);
        assertEquals("8E519DBEF1E4C794E683E5C78009CE6DABF2BCE197453F535E2D1539D62EE1B1",
                ClassicWorldDungeonCatalog
                        .DRAGON_VALLEY_FIRST_FLOOR_TRANSITIONS_SECONDARY_SOURCE_SHA256);

        ClassicWorldDungeonCatalog.Route worldEntrance =
                ClassicWorldDungeonCatalog.find(4, 33348, 32347);
        assertEquals(34, worldEntrance.routeId());
        assertEquals(30, worldEntrance.destinationMapId());
        assertEquals(32727, worldEntrance.destinationX());
        assertEquals(32726, worldEntrance.destinationY());
        assertEquals(4, worldEntrance.destinationHeading());

        ClassicWorldDungeonCatalog.Route worldExit =
                ClassicWorldDungeonCatalog.find(30, 32727, 32726);
        assertEquals(4, worldExit.destinationMapId());
        assertEquals(33348, worldExit.destinationX());
        assertEquals(32348, worldExit.destinationY());

        for (int sourceX : List.of(32732, 32733)) {
            ClassicWorldDungeonCatalog.Route toSecond =
                    ClassicWorldDungeonCatalog.find(30, sourceX, 32813);
            assertEquals(31, toSecond.destinationMapId());
            assertEquals(32760, toSecond.destinationX());
            assertEquals(32781, toSecond.destinationY());
        }
        ClassicWorldDungeonCatalog.Route toFirst =
                ClassicWorldDungeonCatalog.find(31, 32760, 32780);
        assertEquals(30, toFirst.destinationMapId());
        assertEquals(32733, toFirst.destinationX());
        assertEquals(32812, toFirst.destinationY());
        assertEquals(20, ClassicWorldDungeonCatalog.all().stream()
                .filter(route -> route.routeId() >= 34
                        && route.routeId() <= 53).count());
    }

    @Test
    void selectedSourcePinsAllDeepDragonValleyTransitions() {
        assertEquals("261FDCF68365DC87AB3C929BCDDC92EA78F984825942BF7EE5A5242BD253DF70",
                ClassicWorldDungeonCatalog
                        .DRAGON_VALLEY_DEEP_TRANSITIONS_SOURCE_SHA256);
        assertEquals("C5259CFB09BFD107F1E7149436C298E0CB87EAD98F1F57682AD5F857C4D75529",
                ClassicWorldDungeonCatalog
                        .DRAGON_VALLEY_DEEP_TRANSITIONS_SECONDARY_SOURCE_SHA256);

        for (int sourceX : List.of(32728, 32729)) {
            ClassicWorldDungeonCatalog.Route toFifth =
                    ClassicWorldDungeonCatalog.find(33, sourceX, 32825);
            assertEquals(35, toFifth.destinationMapId());
            assertEquals(32746, toFifth.destinationX());
            assertEquals(32801, toFifth.destinationY());
            assertEquals(6, toFifth.destinationHeading());
        }
        assertEquals(33, ClassicWorldDungeonCatalog.find(35, 32747, 32801)
                .destinationMapId());
        for (int sourceY : List.of(32838, 32839)) {
            assertEquals(36, ClassicWorldDungeonCatalog.find(35, 32660, sourceY)
                    .destinationMapId());
        }
        assertEquals(35, ClassicWorldDungeonCatalog.find(36, 32664, 32845)
                .destinationMapId());
        for (int sourceX : List.of(32746, 32747)) {
            assertEquals(37, ClassicWorldDungeonCatalog.find(36, sourceX, 32824)
                    .destinationMapId());
        }
        ClassicWorldDungeonCatalog.Route lairExit =
                ClassicWorldDungeonCatalog.find(37, 32669, 32868);
        assertEquals(62, lairExit.routeId());
        assertEquals(36, lairExit.destinationMapId());
        assertEquals(32746, lairExit.destinationX());
        assertEquals(32823, lairExit.destinationY());
        assertEquals(2, lairExit.destinationHeading());
        assertEquals(9, ClassicWorldDungeonCatalog.all().stream()
                .filter(route -> route.routeId() >= 54
                        && route.routeId() <= 62).count());
    }

    @Test
    void retainedPirateScheduleRepeatsEveryThreeHoursWithExactBoundaries() {
        ClassicWorldDungeonCatalog.Route route =
                ClassicWorldDungeonCatalog.find(445, 32750, 32874);
        assertFalse(route.openAt(89 * 60 + 59));
        assertTrue(route.openAt(90 * 60));
        assertTrue(route.openAt(149 * 60 + 59));
        assertFalse(route.openAt(150 * 60));
        assertTrue(route.openAt(4 * 3_600 + 30 * 60));
        assertFalse(route.openAt(5 * 3_600 + 30 * 60));
        assertTrue(route.openAt(22 * 3_600 + 30 * 60));
        assertFalse(route.openAt(23 * 3_600 + 30 * 60));
        assertThrows(IllegalArgumentException.class, () -> route.openAt(-1));
        assertThrows(IllegalArgumentException.class, () -> route.openAt(86_400));
        assertTrue(ClassicWorldDungeonCatalog.find(445, 32738, 32851)
                .openAt(0));
    }

    @Test
    void retainedWaterDragonBossEntranceIsTheOnlyRoute464Trigger() {
        ClassicWorldDungeonCatalog.Route entrance =
                ClassicWorldDungeonCatalog.find(1011, 32941, 32671);
        assertEquals(464, entrance.routeId());
        assertEquals(1011, entrance.destinationMapId());
        assertEquals(32993, entrance.destinationX());
        assertEquals(32841, entrance.destinationY());
        assertEquals(6, entrance.destinationHeading());
        assertTrue(ClassicWorldDungeonCatalog
                .isWaterDragonBossEntranceRoute(entrance));

        ClassicWorldDungeonCatalog.Route reverse =
                ClassicWorldDungeonCatalog.find(1011, 32993, 32841);
        assertEquals(465, reverse.routeId());
        assertFalse(ClassicWorldDungeonCatalog
                .isWaterDragonBossEntranceRoute(reverse));
        assertEquals(7, ClassicWorldDungeonCatalog.all().stream()
                .filter(route -> route.routeId() >= 459
                        && route.routeId() <= 465).count());
    }
}
