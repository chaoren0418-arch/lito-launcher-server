package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

class ProtocolBattleTest {
    @Test void selectedMeditationRequestIsExactSelectorOnlyCcc() {
        byte[] nativeRequest = new Protocol.Writer().c(Protocol.C_USE_SPELL)
                .c(3).c(7).bytes();
        assertArrayEquals(new byte[] {0x14, 0x03, 0x07}, nativeRequest);
        Protocol.SpellSelector request =
                Protocol.selectorOnlySpellRequest(nativeRequest);
        assertEquals(BattleSpellCatalog.MEDITATION, request.skillId());
        assertEquals(3, request.row());
        assertEquals(7, request.column());
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.selectorOnlySpellRequest(new Protocol.Writer()
                        .c(Protocol.C_USE_SPELL).c(3).c(7).d(1).bytes()));

        assertArrayEquals(new byte[] {0x20, 0x10, 0x00, 0x00, 0x00, 0x13},
                Protocol.objectAction(16, 19));
        assertArrayEquals(new byte[] {0x37, 0x10, 0x00, 0x00, 0x00,
                        0x7D, 0x08},
                Protocol.effectObject(16, 2173));
    }

    @Test void selectedCounterMagicRequestIsExactSelectorOnlyCcc() {
        byte[] nativeRequest = new Protocol.Writer().c(Protocol.C_USE_SPELL)
                .c(3).c(6).bytes();
        assertArrayEquals(new byte[] {0x14, 0x03, 0x06}, nativeRequest);
        Protocol.SpellSelector request =
                Protocol.selectorOnlySpellRequest(nativeRequest);
        assertEquals(BattleSpellCatalog.COUNTER_MAGIC, request.skillId());
        assertEquals(3, request.row());
        assertEquals(6, request.column());
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.selectorOnlySpellRequest(new Protocol.Writer()
                        .c(Protocol.C_USE_SPELL).c(3).c(6).c(0).bytes()));

        assertArrayEquals(new byte[] {0x20, 0x10, 0x00, 0x00, 0x00, 0x13},
                Protocol.objectAction(16, 19));
        assertArrayEquals(new byte[] {0x37, 0x10, 0x00, 0x00, 0x00,
                        (byte) 0xCD, 0x29},
                Protocol.effectObject(16, 10701));
    }

    @Test void selectedResistFearRequestIsExactSelectorOnlyCcc() {
        byte[] nativeRequest = new Protocol.Writer().c(Protocol.C_USE_SPELL)
                .c(23).c(3).bytes();
        assertArrayEquals(new byte[] {0x14, 0x17, 0x03}, nativeRequest);
        Protocol.SpellSelector request =
                Protocol.selectorOnlySpellRequest(nativeRequest);
        assertEquals(BattleSpellCatalog.RESIST_FEAR, request.skillId());
        assertEquals(23, request.row());
        assertEquals(3, request.column());
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.selectorOnlySpellRequest(new Protocol.Writer()
                        .c(Protocol.C_USE_SPELL).c(23).c(3).d(1).bytes()));
    }

    @Test void selectedFoeSlayerRequestIsExactSelectorOnlyCcc() {
        byte[] nativeRequest = new Protocol.Writer().c(Protocol.C_USE_SPELL)
                .c(23).c(2).bytes();
        assertArrayEquals(new byte[] {0x14, 0x17, 0x02}, nativeRequest);
        Protocol.SpellSelector request =
                Protocol.selectorOnlySpellRequest(nativeRequest);
        assertEquals(BattleSpellCatalog.FOE_SLAYER, request.skillId());
        assertEquals(23, request.row());
        assertEquals(2, request.column());
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.selectorOnlySpellRequest(new Protocol.Writer()
                        .c(Protocol.C_USE_SPELL).c(23).c(2).d(1).bytes()));
    }

    @Test void selectedBloodlustRequestIsExactSelectorOnlyCcc() {
        byte[] nativeRequest = new Protocol.Writer().c(Protocol.C_USE_SPELL)
                .c(23).c(1).bytes();
        assertArrayEquals(new byte[] {0x14, 0x17, 0x01}, nativeRequest);
        Protocol.SpellSelector request =
                Protocol.selectorOnlySpellRequest(nativeRequest);
        assertEquals(BattleSpellCatalog.BLOODLUST, request.skillId());
        assertEquals(23, request.row());
        assertEquals(1, request.column());
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.selectorOnlySpellRequest(new Protocol.Writer()
                        .c(Protocol.C_USE_SPELL).c(23).c(1).d(1).bytes()));
    }

    @Test void selectedAwakenAntharasRequestIsExactSelectorOnlyCcc() {
        byte[] nativeRequest = new Protocol.Writer().c(Protocol.C_USE_SPELL)
                .c(23).c(0).bytes();
        assertArrayEquals(new byte[] {0x14, 0x17, 0x00}, nativeRequest);
        Protocol.SpellSelector request =
                Protocol.selectorOnlySpellRequest(nativeRequest);
        assertEquals(BattleSpellCatalog.AWAKEN_ANTHARAS, request.skillId());
        assertEquals(23, request.row());
        assertEquals(0, request.column());
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.selectorOnlySpellRequest(new Protocol.Writer()
                        .c(Protocol.C_USE_SPELL).c(23).c(0).d(1).bytes()));
    }

    @Test void selectedAwakenFafurionRequestIsExactSelectorOnlyCcc() {
        byte[] nativeRequest = new Protocol.Writer().c(Protocol.C_USE_SPELL)
                .c(23).c(5).bytes();
        assertArrayEquals(new byte[] {0x14, 0x17, 0x05}, nativeRequest);
        Protocol.SpellSelector request =
                Protocol.selectorOnlySpellRequest(nativeRequest);
        assertEquals(BattleSpellCatalog.AWAKEN_FAFURION, request.skillId());
        assertEquals(23, request.row());
        assertEquals(5, request.column());
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.selectorOnlySpellRequest(new Protocol.Writer()
                        .c(Protocol.C_USE_SPELL).c(23).c(5).d(1).bytes()));
    }

    @Test void selectedGuardBrakeRequestIsExactSelectorOnlyCcc() {
        byte[] nativeRequest = new Protocol.Writer().c(Protocol.C_USE_SPELL)
                .c(22).c(6).bytes();
        assertArrayEquals(new byte[] {0x14, 0x16, 0x06}, nativeRequest);
        Protocol.SpellSelector request =
                Protocol.selectorOnlySpellRequest(nativeRequest);
        assertEquals(BattleSpellCatalog.GUARD_BRAKE, request.skillId());
        assertEquals(22, request.row());
        assertEquals(6, request.column());
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.selectorOnlySpellRequest(new Protocol.Writer()
                        .c(Protocol.C_USE_SPELL).c(22).c(6).c(0).bytes()));
    }

    @Test void selectedBurningSlashRequestIsExactSelectorOnlyCcc() {
        byte[] nativeRequest = new Protocol.Writer().c(Protocol.C_USE_SPELL)
                .c(22).c(5).bytes();
        assertArrayEquals(new byte[] {0x14, 0x16, 0x05}, nativeRequest);
        Protocol.SpellSelector request =
                Protocol.selectorOnlySpellRequest(nativeRequest);
        assertEquals(BattleSpellCatalog.BURNING_SLASH, request.skillId());
        assertEquals(22, request.row());
        assertEquals(5, request.column());
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.selectorOnlySpellRequest(new Protocol.Writer()
                        .c(Protocol.C_USE_SPELL).c(22).c(5).c(0).bytes()));
    }

    @Test void selectedDragonSkinRequestIsExactSelectorOnlyCcc() {
        byte[] nativeRequest = new Protocol.Writer().c(Protocol.C_USE_SPELL)
                .c(22).c(4).bytes();
        assertArrayEquals(new byte[] {0x14, 0x16, 0x04}, nativeRequest);
        Protocol.SpellSelector request =
                Protocol.selectorOnlySpellRequest(nativeRequest);
        assertEquals(BattleSpellCatalog.DRAGON_SKIN, request.skillId());
        assertEquals(22, request.row());
        assertEquals(4, request.column());
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.selectorOnlySpellRequest(new Protocol.Writer()
                        .c(Protocol.C_USE_SPELL).c(22).c(4).c(0).bytes()));
    }

    @Test void selectedResistMagicRequestIsExactSelectorOnlyCcc() {
        byte[] nativeRequest = new Protocol.Writer().c(Protocol.C_USE_SPELL)
                .c(16).c(0).bytes();
        Protocol.SpellSelector request =
                Protocol.selectorOnlySpellRequest(nativeRequest);
        assertEquals(BattleSpellCatalog.RESIST_MAGIC, request.skillId());
        assertEquals(16, request.row());
        assertEquals(0, request.column());
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.selectorOnlySpellRequest(new Protocol.Writer()
                        .c(Protocol.C_USE_SPELL).c(16).c(0).c(0).bytes()));
    }

    @Test void selectedBodyToMindRequestIsExactSelectorOnlyCcc() {
        byte[] nativeRequest = new Protocol.Writer().c(Protocol.C_USE_SPELL)
                .c(16).c(1).bytes();
        assertArrayEquals(new byte[] {0x14, 0x10, 0x01}, nativeRequest);
        Protocol.SpellSelector request =
                Protocol.selectorOnlySpellRequest(nativeRequest);
        assertEquals(BattleSpellCatalog.BODY_TO_MIND, request.skillId());
        assertEquals(16, request.row());
        assertEquals(1, request.column());
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.selectorOnlySpellRequest(new Protocol.Writer()
                        .c(Protocol.C_USE_SPELL).c(16).c(1).c(0).bytes()));
    }

    @Test void selectedBloodySoulRequestIsExactSelectorOnlyCcc() {
        byte[] nativeRequest = new Protocol.Writer().c(Protocol.C_USE_SPELL)
                .c(18).c(1).bytes();
        assertArrayEquals(new byte[] {0x14, 0x12, 0x01}, nativeRequest);
        Protocol.SpellSelector request =
                Protocol.selectorOnlySpellRequest(nativeRequest);
        assertEquals(BattleSpellCatalog.BLOODY_SOUL, request.skillId());
        assertEquals(18, request.row());
        assertEquals(1, request.column());
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.selectorOnlySpellRequest(new Protocol.Writer()
                        .c(Protocol.C_USE_SPELL).c(18).c(1).c(0).bytes()));
    }

    @Test void repairedWorldTreeCallRequestIsExactSelectorOnlyCcc() {
        byte[] nativeRequest = new Protocol.Writer().c(Protocol.C_USE_SPELL)
                .c(16).c(2).bytes();
        assertArrayEquals(new byte[] {0x14, 0x10, 0x02}, nativeRequest);
        Protocol.SpellSelector request =
                Protocol.selectorOnlySpellRequest(nativeRequest);
        assertEquals(BattleSpellCatalog.WORLD_TREE_CALL, request.skillId());
        assertEquals(16, request.row());
        assertEquals(2, request.column());
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.selectorOnlySpellRequest(new Protocol.Writer()
                        .c(Protocol.C_USE_SPELL).c(16).c(2).c(0).bytes()));
    }

    @Test void counterMirrorUsesExactNativeCase133SelectorOnlyCcc() {
        byte[] nativeRequest = new Protocol.Writer().c(Protocol.C_USE_SPELL)
                .c(16).c(5).bytes();
        assertArrayEquals(new byte[] {0x14, 0x10, 0x05}, nativeRequest);
        Protocol.SpellSelector request =
                Protocol.selectorOnlySpellRequest(nativeRequest);
        assertEquals(BattleSpellCatalog.COUNTER_MIRROR, request.skillId());
        assertEquals(16, request.row());
        assertEquals(5, request.column());
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.selectorOnlySpellRequest(new Protocol.Writer()
                        .c(Protocol.C_USE_SPELL).c(16).c(5).c(0).bytes()));
    }

    @Test void clearMindUsesExactNativeCase136SelectorOnlyCcc() {
        byte[] nativeRequest = new Protocol.Writer().c(Protocol.C_USE_SPELL)
                .c(17).c(0).bytes();
        assertArrayEquals(new byte[] {0x14, 0x11, 0x00}, nativeRequest);
        Protocol.SpellSelector request =
                Protocol.selectorOnlySpellRequest(nativeRequest);
        assertEquals(BattleSpellCatalog.CLEAR_MIND, request.skillId());
        assertEquals(17, request.row());
        assertEquals(0, request.column());
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.selectorOnlySpellRequest(new Protocol.Writer()
                        .c(Protocol.C_USE_SPELL).c(17).c(0).c(0).bytes()));
    }

    @Test void resistElementalUsesExactNativeCase137SelectorOnlyCcc() {
        byte[] nativeRequest = new Protocol.Writer().c(Protocol.C_USE_SPELL)
                .c(17).c(1).bytes();
        assertArrayEquals(new byte[] {0x14, 0x11, 0x01}, nativeRequest);
        Protocol.SpellSelector request =
                Protocol.selectorOnlySpellRequest(nativeRequest);
        assertEquals(BattleSpellCatalog.RESIST_ELEMENTAL, request.skillId());
        assertEquals(17, request.row());
        assertEquals(1, request.column());
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.selectorOnlySpellRequest(new Protocol.Writer()
                        .c(Protocol.C_USE_SPELL).c(17).c(1).c(0).bytes()));
    }

    @Test void greaterElementalUsesExactNativeCase161SelectorOnlyCcc() {
        byte[] nativeRequest = new Protocol.Writer().c(Protocol.C_USE_SPELL)
                .c(20).c(1).bytes();
        assertArrayEquals(new byte[] {0x14, 0x14, 0x01}, nativeRequest);
        Protocol.SpellSelector request =
                Protocol.selectorOnlySpellRequest(nativeRequest);
        assertEquals(BattleSpellCatalog.GREATER_ELEMENTAL, request.skillId());
        assertEquals(20, request.row());
        assertEquals(1, request.column());
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.selectorOnlySpellRequest(new Protocol.Writer()
                        .c(Protocol.C_USE_SPELL).c(20).c(1).c(0).bytes()));
    }

    @Test
    void battleActorUsesPcStatusAndTailFieldsFromTheExactPutObjectReceiver() {
        Protocol.Reader actor = new Protocol.Reader(Protocol.battleActor(
                0x1122_3344L, 32730, 32798, 61, 4, 0, BattleTimedStatus.NORMAL,
                "TestKnight", "Arena Hero", 4, 0, "", 1234, 0xFF, 52));

        assertEquals(Protocol.S_PUT_OBJECT, actor.c());
        assertEquals(32730, actor.h());
        assertEquals(32798, actor.h());
        assertEquals(0x1122_3344L, actor.d());
        assertEquals(61, actor.h());
        assertEquals(4, actor.c()); // equipped weapon walk action
        assertEquals(0, actor.c()); // heading
        assertEquals(0, actor.c()); // light radius
        assertEquals(BattleTimedStatus.NORMAL, actor.c()); // authoritative move speed
        assertEquals(0, actor.d()); // public exp
        assertEquals(1234, actor.h()); // lawful source state
        assertEquals("TestKnight", actor.s());
        assertEquals("Arena Hero", actor.s());
        assertEquals(4, actor.c()); // STATUS_PC, not room type
        assertEquals(0, actor.d());
        assertEquals("", actor.s());
        assertEquals("", actor.s());
        assertEquals(0, actor.c()); // clan rank
        assertEquals(0xFF, actor.c()); // HP ratio
        assertEquals(0, actor.c()); // brave state
        assertEquals(52, actor.c()); // level
        assertEquals("", actor.s());
        assertEquals(0, actor.h());
        assertEquals(0, actor.c());
        assertEquals(0, actor.remaining());

        Protocol.Reader npc = new Protocol.Reader(Protocol.battleActor(
                2, 32740, 32798, 61, 0, 0, BattleTimedStatus.NORMAL,
                "ArenaBot", "", 0, 0xFF, 1));
        npc.c(); npc.h(); npc.h(); npc.d(); npc.h();
        npc.c(); npc.c(); npc.c(); npc.c(); npc.d(); npc.h();
        npc.s(); npc.s();
        assertEquals(0, npc.c()); // NPC/monster status

        assertThrows(IllegalArgumentException.class, () -> Protocol.battleActor(
                1, 1, 1, 61, 0, 0, BattleTimedStatus.NORMAL,
                "Actor", "", 4, 0, "", Short.MIN_VALUE - 1, 100, 1));
        assertThrows(IllegalArgumentException.class, () -> Protocol.battleActor(
                1, 1, 1, 61, 0, 0, BattleTimedStatus.NORMAL,
                "Actor", "", 4, 0, "", Short.MAX_VALUE + 1, 100, 1));

        Protocol.Reader headed = new Protocol.Reader(
                Protocol.battleActorWithHeading(
                        7, 32770, 32818, 61, 4, 6, 3,
                        BattleTimedStatus.NORMAL, "Headed", "", 4,
                        0, "", 0, 100, 50));
        headed.c(); headed.h(); headed.h(); headed.d(); headed.h();
        assertEquals(4, headed.c());
        assertEquals(6, headed.c());
        assertEquals(3, headed.c());
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.battleActorWithHeading(
                        7, 32770, 32818, 61, 4, 8, 3,
                        BattleTimedStatus.NORMAL, "Headed", "", 4,
                        0, "", 0, 100, 50));
    }

    @Test
    void changeAlignmentUsesExactNativeSignedWordMutationLayout() {
        assertArrayEquals(new byte[] {
                        0x59, 0x44, 0x33, 0x22, 0x11, (byte) 0xD2, 0x04
                }, Protocol.changeAlignment(0x1122_3344L, 1234));
        assertArrayEquals(new byte[] {
                        0x59, 0x44, 0x33, 0x22, 0x11, 0x2E, (byte) 0xFB
                }, Protocol.changeAlignment(0x1122_3344L, -1234));
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.changeAlignment(0, 0));
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.changeAlignment(1, Short.MIN_VALUE - 1));
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.changeAlignment(1, Short.MAX_VALUE + 1));
    }

    @Test
    void criminalStatusUsesExactNativePinkNameTimerLayout() {
        assertArrayEquals(new byte[] {
                        0x6A, 0x44, 0x33, 0x22, 0x11, (byte) 180
                }, Protocol.criminalStatus(0x1122_3344L, 180));
        assertArrayEquals(new byte[] {
                        0x6A, 0x44, 0x33, 0x22, 0x11, 0
                }, Protocol.criminalStatus(0x1122_3344L, 0));
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.criminalStatus(0, 180));
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.criminalStatus(1, -1));
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.criminalStatus(1, 0x100));
    }

    @Test
    void cloneAppearanceUsesExactNativeEnrichedObjectLayout() {
        assertArrayEquals(new byte[] {
                        0x79,
                        0x44, 0x33, 0x22, 0x11,
                        (byte) 0xDD, (byte) 0xCC, (byte) 0xBB, (byte) 0xAA,
                        0x3D, 0x00,
                        0x2E, (byte) 0xFB,
                        0x04,
                        'A', 'r', 'e', 'n', 'a', 0
                },
                Protocol.cloneAppearance(
                        0x1122_3344L, 0xAABB_CCDDL, 61, -1234, 4, "Arena"));
        assertArrayEquals(new byte[] {
                        0x79, 0x01, 0, 0, 0, 0, 0, 0, 0,
                        0x01, 0, 0, 0, 0, 0
                }, Protocol.cloneAppearance(1, 0, 1, 0, 0, ""));
        assertArrayEquals(new byte[] {
                        0x79, 0x01, 0, 0, 0, 0, 0, 0, 0,
                        0, 0, 0, 0, 0, 0
                }, Protocol.cloneAppearance(1, 0, 0, 0, 0, ""));

        assertThrows(IllegalArgumentException.class,
                () -> Protocol.cloneAppearance(0, 0, 61, 0, 4, "Arena"));
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.cloneAppearance(1, -1, 61, 0, 4, "Arena"));
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.cloneAppearance(1, 0, -1, 0, 4, "Arena"));
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.cloneAppearance(1, 0, 0x1_0000, 0, 4, "Arena"));
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.cloneAppearance(1, 0, 61, Short.MAX_VALUE + 1, 4, "Arena"));
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.cloneAppearance(1, 0, 61, 0, 0x100, "Arena"));
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.cloneAppearance(1, 0, 61, 0, 4, "Bad\nTitle"));
    }

    @Test
    void cloneCompatibilityWriterHasNoProductionRouteOrMutableArenaBotState()
            throws Exception {
        Path main = Path.of("src/main/java");
        int calls = 0;
        int symbolicRoutes = 0;
        try (var sources = Files.walk(main)) {
            for (Path path : sources.filter(Files::isRegularFile)
                    .filter(candidate -> candidate.getFileName().toString()
                            .endsWith(".java"))
                    .toList()) {
                String source = Files.readString(path, StandardCharsets.UTF_8);
                calls += occurrences(source, "Protocol.cloneAppearance(");
                symbolicRoutes += occurrences(source, "Protocol.S_CLONE");
            }
        }
        assertEquals(2, calls,
                "S_CLONE must remain the isolated mutation plus exact restore only");
        assertEquals(0, symbolicRoutes,
                "production source routes origin-unproved S_CLONE");

        String protocol = Files.readString(Path.of(
                "src/main/java/local/lito381/server/Protocol.java"),
                StandardCharsets.UTF_8);
        assertEquals(1, occurrences(protocol, "static byte[] cloneAppearance("));
        assertEquals(1, occurrences(protocol, ".c(S_CLONE)"));

        String session = Files.readString(Path.of(
                "src/main/java/local/lito381/server/ClientSession.java"),
                StandardCharsets.UTF_8);
        int fixtureStart = session.indexOf(
                "private boolean sendCloneAppearanceProbe(");
        int fixtureEnd = session.indexOf(
                "private boolean sendAttackAllProbe(", fixtureStart);
        assertTrue(fixtureStart >= 0 && fixtureEnd > fixtureStart);
        assertEquals(2, occurrences(
                session.substring(fixtureStart, fixtureEnd),
                "Protocol.cloneAppearance("));
        assertEquals(2, occurrences(session,
                "sendBattleObject(2, \"ArenaBot\", \"ArenaBot\", bot.x(), bot.y(), 61,"));
        String hub = Files.readString(Path.of(
                "src/main/java/local/lito381/server/GameHub.java"),
                StandardCharsets.UTF_8);
        assertFalse(hub.contains("botGfx"));
        assertFalse(hub.contains("setBotGfx"));
    }

    @Test
    void speedUsesExactNativeDchLayoutAndActorBootstrapUsesTheSameState() {
        Protocol.Reader speed = new Protocol.Reader(
                Protocol.speed(0x1122_3344L, Protocol.SpeedMode.HASTE, 300));
        assertEquals(Protocol.S_SPEED, speed.c());
        assertEquals(0x1122_3344L, speed.d());
        assertEquals(BattleTimedStatus.HASTE, speed.c());
        assertEquals(300, speed.h());
        assertEquals(0, speed.remaining());

        Protocol.Reader actor = new Protocol.Reader(Protocol.battleActor(
                0x1122_3344L, 32730, 32798, 61, 4, 0x16, BattleTimedStatus.HASTE,
                "TestKnight", "", 4, 100, 52));
        actor.c(); actor.h(); actor.h(); actor.d(); actor.h();
        actor.c(); actor.c();
        assertEquals(0x16, actor.c());
        assertEquals(BattleTimedStatus.HASTE, actor.c());

        assertThrows(IllegalArgumentException.class,
                () -> Protocol.speed(1, Protocol.SpeedMode.NORMAL, 1));
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.SpeedMode.fromWire(3));
    }

    @Test
    void polymorphUsesExactNativeDhcLayoutAndAuthoritativeWalkAction() {
        assertArrayEquals(new byte[] {
                        0x27, 0x44, 0x33, 0x22, 0x11, (byte) 0xC8, 0x0E, 0x04
                },
                Protocol.polymorph(0x1122_3344L, 3784, BattleActionCodes.SWORD_WALK));
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.polymorph(0, 3784, BattleActionCodes.SWORD_WALK));
        assertArrayEquals(new byte[] {
                        0x27, 0x01, 0x00, 0x00, 0x00, 0x00, 0x00, 0x04
                },
                Protocol.polymorph(1, 0, BattleActionCodes.SWORD_WALK));
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.polymorph(1, -1, BattleActionCodes.SWORD_WALK));
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.polymorph(1, 3784, BattleActionCodes.CHAINSWORD_WALK));
        assertArrayEquals(new byte[] {
                        0x27, 0x01, 0x00, 0x00, 0x00,
                        (byte) 0xB1, 0x03, 0x00
                },
                Protocol.petRacePolymorph(1, 945, BattleActionCodes.WALK));
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.polymorph(1, 945, BattleActionCodes.WALK));
    }

    @Test
    void soundEffectUsesExactNativeRepeatAndSoundWord() {
        assertArrayEquals(new byte[] {0x4A, 0x00, (byte) 0xB5, 0x01},
                Protocol.soundEffect(0, 437));
        assertArrayEquals(new byte[] {0x4A, 0x02, (byte) 0xFF, 0x7F},
                Protocol.soundEffect(2, 0x7FFF));
        assertThrows(IllegalArgumentException.class, () -> Protocol.soundEffect(-1, 437));
        assertThrows(IllegalArgumentException.class, () -> Protocol.soundEffect(3, 437));
        assertThrows(IllegalArgumentException.class, () -> Protocol.soundEffect(0, 0x8000));
    }

    @Test
    void drunkenUsesExactNativeObjectAndModeLayout() {
        assertArrayEquals(new byte[] {0x70, 0x44, 0x33, 0x22, 0x11, 0x00},
                Protocol.drunken(0x1122_3344L, Protocol.DrunkenMode.CLEAR));
        assertArrayEquals(new byte[] {0x70, 0x44, 0x33, 0x22, 0x11, 0x01},
                Protocol.drunken(
                        0x1122_3344L, Protocol.DrunkenMode.ORDINARY_LIQUOR));
        assertArrayEquals(new byte[] {0x70, 0x44, 0x33, 0x22, 0x11, 0x02},
                Protocol.drunken(0x1122_3344L, Protocol.DrunkenMode.CONFUSION));
        assertArrayEquals(new byte[] {0x70, 0x44, 0x33, 0x22, 0x11, 0x08},
                Protocol.drunken(0x1122_3344L, Protocol.DrunkenMode.CAKE_OR_NPC));
        assertThrows(IllegalArgumentException.class, () -> Protocol.drunken(
                0, Protocol.DrunkenMode.ORDINARY_LIQUOR));
        assertThrows(NullPointerException.class, () -> Protocol.drunken(1, null));
    }

    @Test
    void historicalEmotionOpcodeUsesExactNativeBraveDchLayout() {
        assertArrayEquals(new byte[] {
                0x62, 0x44, 0x33, 0x22, 0x11, 0x01, 0x2C, 0x01
                }, Protocol.braveSpeed(
                        0x1122_3344L, Protocol.BraveMode.FIRST_STAGE, 300));
        assertArrayEquals(new byte[] {
                        0x62, 0x44, 0x33, 0x22, 0x11, 0x00, 0x00, 0x00
                }, Protocol.braveSpeed(
                        0x1122_3344L, Protocol.BraveMode.NORMAL, 0));
        assertArrayEquals(new byte[] {
                        0x62, 0x44, 0x33, 0x22, 0x11, 0x04, 0x20, 0x00
                }, Protocol.braveSpeed(0x1122_3344L,
                        Protocol.BraveMode.MOVEMENT_BUFF, 32));
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.braveSpeed(0, Protocol.BraveMode.FIRST_STAGE, 300));
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.BraveMode.fromWire(2));
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.BraveMode.fromWire(7));
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.braveSpeed(1, Protocol.BraveMode.NORMAL, 1));
    }

    @Test
    void holyWalkUsesExactNativeCase51SelectorOnlyEnvelope() {
        Protocol.SpellSelector request = Protocol.selectorOnlySpellRequest(
                new Protocol.Writer().c(Protocol.C_USE_SPELL).c(6).c(3).bytes());
        assertEquals(BattleSpellCatalog.HOLY_WALK, request.skillId());
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.selectorOnlySpellRequest(new Protocol.Writer()
                        .c(Protocol.C_USE_SPELL).c(6).c(3).d(3).bytes()));
    }

    @Test
    void greaterHasteUsesExactNativeCase53SelectorOnlyAndSpeedMessages() {
        Protocol.SpellSelector request = Protocol.selectorOnlySpellRequest(
                new Protocol.Writer().c(Protocol.C_USE_SPELL).c(6).c(5).bytes());
        assertEquals(BattleSpellCatalog.GREATER_HASTE, request.skillId());
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.selectorOnlySpellRequest(new Protocol.Writer()
                        .c(Protocol.C_USE_SPELL).c(6).c(5).d(3).bytes()));

        assertArrayEquals(new byte[] {
                        0x29, 0x03, 0x00, 0x00, 0x00, 0x01, 0x60, 0x09
                }, Protocol.speed(3, Protocol.SpeedMode.HASTE, 2_400));
        assertArrayEquals(new byte[] {
                        0x29, 0x03, 0x00, 0x00, 0x00, 0x01, 0x00, 0x00
                }, Protocol.speed(3, Protocol.SpeedMode.HASTE, 0));
        assertArrayEquals(new byte[] {0x10, (byte) 0xB8, 0x00, 0x00},
                Protocol.messageCode(184));
        assertArrayEquals(new byte[] {0x10, (byte) 0xB9, 0x00, 0x00},
                Protocol.messageCode(185));
    }

    @Test
    void berserkersUsesExactNativeCase54ObjectOnlyEnvelopeAndEffect() {
        Protocol.ObjectSpellRequest request = Protocol.objectSpellRequest(
                new Protocol.Writer().c(Protocol.C_USE_SPELL).c(6).c(6)
                        .d(0x1122_3344L).bytes());
        assertEquals(BattleSpellCatalog.BERSERKERS, request.skillId());
        assertEquals(0x1122_3344L, request.targetObjectId());
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.objectSpellRequest(new Protocol.Writer()
                        .c(Protocol.C_USE_SPELL).c(6).c(6).d(0x1122_3344L)
                        .h(32700).h(32700).bytes()));
        assertArrayEquals(new byte[] {
                        0x37, 0x44, 0x33, 0x22, 0x11, 0x67, 0x0F
                }, Protocol.effectObject(0x1122_3344L, 3943));
    }

    @Test
    void shapeChangeUsesExactNativeCase66ObjectOnlyEnvelope() {
        byte[] packet = new Protocol.Writer().c(Protocol.C_USE_SPELL).c(8).c(2)
                .d(0x1122_3344L).bytes();
        assertArrayEquals(new byte[] {
                0x14, 0x08, 0x02, 0x44, 0x33, 0x22, 0x11
        }, packet);
        Protocol.ObjectSpellRequest request = Protocol.objectSpellRequest(packet);
        assertEquals(BattleSpellCatalog.SHAPE_CHANGE, request.skillId());
        assertEquals(0x1122_3344L, request.targetObjectId());
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.objectSpellRequest(new Protocol.Writer()
                        .c(Protocol.C_USE_SPELL).c(8).c(2)
                        .d(0x1122_3344L).c(0).bytes()));
    }

    @Test
    void immuneToHarmUsesExactNativeCase67ObjectOnlyEnvelopeAndMessages() {
        byte[] packet = new Protocol.Writer().c(Protocol.C_USE_SPELL).c(8).c(3)
                .d(0x1122_3344L).bytes();
        assertArrayEquals(new byte[] {
                0x14, 0x08, 0x03, 0x44, 0x33, 0x22, 0x11
        }, packet);
        Protocol.ObjectSpellRequest request = Protocol.objectSpellRequest(packet);
        assertEquals(BattleSpellCatalog.IMMUNE_TO_HARM, request.skillId());
        assertEquals(0x1122_3344L, request.targetObjectId());
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.objectSpellRequest(new Protocol.Writer()
                        .c(Protocol.C_USE_SPELL).c(8).c(3)
                        .d(0x1122_3344L).c(0).bytes()));
        assertArrayEquals(new byte[] {0x10, 0x3A, 0x01, 0x00},
                Protocol.messageCode(314));
        assertArrayEquals(new byte[] {0x10, 0x3B, 0x01, 0x00},
                Protocol.messageCode(315));
    }

    @Test
    void massTeleportUsesExactNativeCase68CoordinateEnvelopeAndQuestion748() {
        byte[] packet = new Protocol.Writer().c(Protocol.C_USE_SPELL).c(8).c(4)
                .h(504).h(32780).h(32783).bytes();
        assertArrayEquals(new byte[] {
                0x14, 0x08, 0x04, (byte) 0xF8, 0x01,
                0x0C, (byte) 0x80, 0x0F, (byte) 0x80
        }, packet);
        Protocol.MassTeleportSpellRequest request =
                Protocol.massTeleportSpellRequest(packet);
        assertEquals(BattleSpellCatalog.MASS_TELEPORT, request.skillId());
        assertEquals(504, request.mapId());
        assertEquals(32780, request.x());
        assertEquals(32783, request.y());
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.massTeleportSpellRequest(new Protocol.Writer()
                        .c(Protocol.C_USE_SPELL).c(8).c(4)
                        .h(504).h(32780).h(32783).c(0).bytes()));
        assertArrayEquals(new byte[] {0x24, (byte) 0xEC, 0x02, 0x00},
                Protocol.massTeleportAsk());
    }

    @Test
    void teleportUsesExactNativeCase4CoordinateEnvelope() {
        byte[] packet = new Protocol.Writer().c(Protocol.C_USE_SPELL).c(0).c(4)
                .h(564).h(0).h(0).bytes();
        assertArrayEquals(new byte[] {
                0x14, 0x00, 0x04, 0x34, 0x02, 0x00, 0x00, 0x00, 0x00
        }, packet);
        Protocol.TeleportSpellRequest request =
                Protocol.teleportSpellRequest(packet);
        assertEquals(BattleSpellCatalog.TELEPORT, request.skillId());
        assertEquals(564, request.mapId());
        assertEquals(0, request.x());
        assertEquals(0, request.y());
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.teleportSpellRequest(new Protocol.Writer()
                        .c(Protocol.C_USE_SPELL).c(0).c(4)
                        .h(564).h(0).h(0).c(0).bytes()));
    }

    @Test
    void callClanUsesExactNativeCase115NameEnvelopeAndQuestion729() {
        byte[] packet = new Protocol.Writer().c(Protocol.C_USE_SPELL)
                .c(14).c(3).s("TestClanMate").bytes();
        assertArrayEquals(new byte[] {
                0x14, 0x0E, 0x03,
                'T', 'e', 's', 't', 'C', 'l', 'a', 'n', 'M', 'a', 't', 'e', 0
        }, packet);
        Protocol.CallClanSpellRequest request =
                Protocol.callClanSpellRequest(packet);
        assertEquals(BattleSpellCatalog.CALL_CLAN, request.skillId());
        assertEquals("TestClanMate", request.targetName());
        assertArrayEquals(new byte[] {
                0x24, (byte) 0xD9, 0x02, 0x00
        }, Protocol.callClanAsk());
        Protocol.AskAnswer accepted = Protocol.askAnswer(new Protocol.Writer()
                .c(Protocol.C_ANSWER).h(729).c(1).s("").bytes());
        assertEquals(729, accepted.questionId());
        assertEquals(1, accepted.answer());
        assertEquals("", accepted.text());

        assertThrows(IllegalArgumentException.class,
                () -> Protocol.callClanSpellRequest(new Protocol.Writer()
                        .c(Protocol.C_USE_SPELL).c(14).c(3).s("").bytes()));
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.callClanSpellRequest(new Protocol.Writer()
                        .c(Protocol.C_USE_SPELL).c(14).c(3)
                        .s(" TestClanMate").bytes()));
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.callClanSpellRequest(new Protocol.Writer()
                        .c(Protocol.C_USE_SPELL).c(14).c(3)
                        .s("X".repeat(28)).bytes()));
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.callClanSpellRequest(new Protocol.Writer()
                        .c(Protocol.C_USE_SPELL).c(14).c(3)
                        .s("TestClanMate").c(0).bytes()));
    }

    @Test
    void runClanUsesRepairedSelectedCase117NameEnvelope() {
        byte[] packet = new Protocol.Writer().c(Protocol.C_USE_SPELL)
                .c(14).c(5).s("TestClanMate").bytes();
        assertArrayEquals(new byte[] {
                0x14, 0x0E, 0x05,
                'T', 'e', 's', 't', 'C', 'l', 'a', 'n', 'M', 'a', 't', 'e', 0
        }, packet);
        Protocol.RunClanSpellRequest request =
                Protocol.runClanSpellRequest(packet);
        assertEquals(BattleSpellCatalog.RUN_CLAN, request.skillId());
        assertEquals("TestClanMate", request.targetName());
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.runClanSpellRequest(new Protocol.Writer()
                        .c(Protocol.C_USE_SPELL).c(14).c(3)
                        .s("TestClanMate").bytes()));
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.runClanSpellRequest(new Protocol.Writer()
                        .c(Protocol.C_USE_SPELL).c(14).c(5).s("").bytes()));
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.runClanSpellRequest(new Protocol.Writer()
                        .c(Protocol.C_USE_SPELL).c(14).c(5)
                        .s("X".repeat(28)).bytes()));
    }

    @Test
    void tornadoUsesExactNativeCase52SelectorOnlyAndCenteredTypeZeroEnvelope() {
        Protocol.SpellSelector request = Protocol.selectorOnlySpellRequest(
                new Protocol.Writer().c(Protocol.C_USE_SPELL).c(6).c(4).bytes());
        assertEquals(BattleSpellCatalog.TORNADO, request.skillId());
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.selectorOnlySpellRequest(new Protocol.Writer()
                        .c(Protocol.C_USE_SPELL).c(6).c(4).d(3).bytes()));

        Protocol.Reader attack = new Protocol.Reader(Protocol.attackMany(
                18, 0x0102_0304L, 32730, 32798, 2, 4_500_004L, 758, 0,
                List.of(new Protocol.AttackManyTarget(0x1122_3344L, true))));
        assertEquals(Protocol.S_ATTACK_MANY, attack.c());
        assertEquals(18, attack.c());
        assertEquals(0x0102_0304L, attack.d());
        assertEquals(32730, attack.h());
        assertEquals(32798, attack.h());
        assertEquals(2, attack.c());
        assertEquals(4_500_004L, attack.d());
        assertEquals(758, attack.h());
        assertEquals(0, attack.c());
        assertEquals(0, attack.h());
        assertEquals(1, attack.h());
        assertEquals(0x1122_3344L, attack.d());
        assertEquals(0x20, attack.c());
        assertEquals(0, attack.remaining());
    }

    @Test
    void blizzardUsesExactNativeCase58SelectorOnlyAndCenteredTypeZeroEnvelope() {
        Protocol.SpellSelector request = Protocol.selectorOnlySpellRequest(
                new Protocol.Writer().c(Protocol.C_USE_SPELL).c(7).c(2).bytes());
        assertEquals(BattleSpellCatalog.BLIZZARD, request.skillId());
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.selectorOnlySpellRequest(new Protocol.Writer()
                        .c(Protocol.C_USE_SPELL).c(7).c(2).d(3).bytes()));

        Protocol.Reader attack = new Protocol.Reader(Protocol.attackMany(
                18, 0x0102_0304L, 32730, 32798, 2, 4_500_004L, 757, 0,
                List.of(new Protocol.AttackManyTarget(0x1122_3344L, true))));
        assertEquals(Protocol.S_ATTACK_MANY, attack.c());
        assertEquals(18, attack.c());
        assertEquals(0x0102_0304L, attack.d());
        assertEquals(32730, attack.h());
        assertEquals(32798, attack.h());
        assertEquals(2, attack.c());
        assertEquals(4_500_004L, attack.d());
        assertEquals(757, attack.h());
        assertEquals(0, attack.c());
        assertEquals(0, attack.h());
        assertEquals(1, attack.h());
        assertEquals(0x1122_3344L, attack.d());
        assertEquals(0x20, attack.c());
        assertEquals(0, attack.remaining());
    }

    @Test
    void earthquakeUsesExactNativeCase61SelectorOnlyAndCenteredTypeZeroEnvelope() {
        Protocol.SpellSelector request = Protocol.selectorOnlySpellRequest(
                new Protocol.Writer().c(Protocol.C_USE_SPELL).c(7).c(5).bytes());
        assertEquals(BattleSpellCatalog.EARTHQUAKE, request.skillId());
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.selectorOnlySpellRequest(new Protocol.Writer()
                        .c(Protocol.C_USE_SPELL).c(7).c(5).d(3).bytes()));

        Protocol.Reader attack = new Protocol.Reader(Protocol.attackMany(
                18, 0x0102_0304L, 32730, 32798, 2, 4_500_004L, 1812, 0,
                List.of(new Protocol.AttackManyTarget(0x1122_3344L, true))));
        assertEquals(Protocol.S_ATTACK_MANY, attack.c());
        assertEquals(18, attack.c());
        assertEquals(0x0102_0304L, attack.d());
        assertEquals(32730, attack.h());
        assertEquals(32798, attack.h());
        assertEquals(2, attack.c());
        assertEquals(4_500_004L, attack.d());
        assertEquals(1812, attack.h());
        assertEquals(0, attack.c());
        assertEquals(0, attack.h());
        assertEquals(1, attack.h());
        assertEquals(0x1122_3344L, attack.d());
        assertEquals(0x20, attack.c());
        assertEquals(0, attack.remaining());
    }

    @Test
    void fireStormUsesExactNativeCase69SelectorOnlyAndCenteredTypeZeroEnvelope() {
        Protocol.SpellSelector request = Protocol.selectorOnlySpellRequest(
                new Protocol.Writer().c(Protocol.C_USE_SPELL).c(8).c(5).bytes());
        assertEquals(BattleSpellCatalog.FIRE_STORM, request.skillId());
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.selectorOnlySpellRequest(new Protocol.Writer()
                        .c(Protocol.C_USE_SPELL).c(8).c(5).d(3).bytes()));

        Protocol.Reader attack = new Protocol.Reader(Protocol.attackMany(
                18, 0x0102_0304L, 32730, 32798, 2, 4_500_070L, 1819, 0,
                List.of(new Protocol.AttackManyTarget(0x1122_3344L, true))));
        assertEquals(Protocol.S_ATTACK_MANY, attack.c());
        assertEquals(18, attack.c());
        assertEquals(0x0102_0304L, attack.d());
        assertEquals(32730, attack.h());
        assertEquals(32798, attack.h());
        assertEquals(2, attack.c());
        assertEquals(4_500_070L, attack.d());
        assertEquals(1819, attack.h());
        assertEquals(0, attack.c());
        assertEquals(0, attack.h());
        assertEquals(1, attack.h());
        assertEquals(0x1122_3344L, attack.d());
        assertEquals(0x20, attack.c());
        assertEquals(0, attack.remaining());
    }

    @Test
    void silenceUsesExactNativeCase63TargetCoordinateEnvelopeAndEffect() {
        Protocol.AttackSpellRequest request = Protocol.attackSpellRequest(
                new Protocol.Writer().c(Protocol.C_USE_SPELL)
                        .c(7).c(7).d(0x1122_3344L).h(32731).h(32799).bytes());
        assertEquals(BattleSpellCatalog.SILENCE, request.skillId());
        assertEquals(7, request.row());
        assertEquals(7, request.column());
        assertEquals(0x1122_3344L, request.targetObjectId());
        assertEquals(32731, request.x());
        assertEquals(32799, request.y());
        assertArrayEquals(new byte[] {
                        0x37, 0x44, 0x33, 0x22, 0x11, (byte) 0x81, 0x08
                }, Protocol.effectObject(0x1122_3344L, 2177));
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.attackSpellRequest(new Protocol.Writer()
                        .c(Protocol.C_USE_SPELL).c(7).c(7).d(0x1122_3344L)
                        .h(32731).h(32799).c(0).bytes()));
    }

    @Test
    void secondCircleArrowsUseExactNativeObjectCoordinateEnvelopes() {
        for (int[] row : new int[][] {
                {6, BattleSpellCatalog.FIRE_ARROW},
                {7, BattleSpellCatalog.STALAC}
        }) {
            byte[] nativeRequest = new Protocol.Writer().c(Protocol.C_USE_SPELL)
                    .c(1).c(row[0]).d(0x1122_3344L)
                    .h(33314).h(32339).bytes();
            Protocol.AttackSpellRequest request =
                    Protocol.attackSpellRequest(nativeRequest);
            assertEquals(row[1], request.skillId());
            assertEquals(1, request.row());
            assertEquals(row[0], request.column());
            assertEquals(0x1122_3344L, request.targetObjectId());
            assertEquals(33314, request.x());
            assertEquals(32339, request.y());
            assertThrows(IllegalArgumentException.class,
                    () -> Protocol.attackSpellRequest(new Protocol.Writer()
                            .c(Protocol.C_USE_SPELL).c(1).c(row[0])
                            .d(0x1122_3344L).h(33314).h(32339).c(0).bytes()));
        }
    }

    @Test
    void decayPotionUsesExactNativeCase70TargetCoordinatesEffectAndBlockMessage() {
        byte[] nativeRequest = new Protocol.Writer().c(Protocol.C_USE_SPELL)
                .c(8).c(6).d(0x1122_3344L).h(32731).h(32799).bytes();
        assertArrayEquals(new byte[] {
                0x14, 0x08, 0x06, 0x44, 0x33, 0x22, 0x11,
                (byte) 0xDB, 0x7F, 0x1F, (byte) 0x80
        }, nativeRequest);
        Protocol.AttackSpellRequest request =
                Protocol.attackSpellRequest(nativeRequest);
        assertEquals(BattleSpellCatalog.DECAY_POTION, request.skillId());
        assertEquals(8, request.row());
        assertEquals(6, request.column());
        assertEquals(0x1122_3344L, request.targetObjectId());
        assertEquals(32731, request.x());
        assertEquals(32799, request.y());
        assertArrayEquals(new byte[] {
                        0x37, 0x44, 0x33, 0x22, 0x11, (byte) 0xB8, 0x08
                }, Protocol.effectObject(0x1122_3344L, 2232));

        Protocol.Reader blocked = new Protocol.Reader(Protocol.messageCode(698));
        assertEquals(Protocol.S_MESSAGE_CODE, blocked.c());
        assertEquals(698, blocked.h());
        assertEquals(0, blocked.c());
        assertEquals(0, blocked.remaining());
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.attackSpellRequest(new Protocol.Writer()
                        .c(Protocol.C_USE_SPELL).c(8).c(6).d(0x1122_3344L)
                        .h(32731).h(32799).c(0).bytes()));
    }

    @Test
    void counterDetectionUsesExactNativeCase71SelectorOnlyAndRangeEnvelope() {
        byte[] nativeRequest = new Protocol.Writer().c(Protocol.C_USE_SPELL)
                .c(8).c(7).bytes();
        assertArrayEquals(new byte[] {0x14, 0x08, 0x07}, nativeRequest);
        Protocol.SpellSelector request =
                Protocol.selectorOnlySpellRequest(nativeRequest);
        assertEquals(BattleSpellCatalog.COUNTER_DETECTION, request.skillId());
        assertEquals(8, request.row());
        assertEquals(7, request.column());
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.selectorOnlySpellRequest(new Protocol.Writer()
                        .c(Protocol.C_USE_SPELL).c(8).c(7).d(3).bytes()));

        Protocol.Reader attack = new Protocol.Reader(Protocol.attackMany(
                19, 0x0102_0304L, 32730, 32798, 2, 4_500_072L, 3934, 0,
                List.of(new Protocol.AttackManyTarget(0x1122_3344L, true))));
        assertEquals(Protocol.S_ATTACK_MANY, attack.c());
        assertEquals(19, attack.c());
        assertEquals(0x0102_0304L, attack.d());
        assertEquals(32730, attack.h());
        assertEquals(32798, attack.h());
        assertEquals(2, attack.c());
        assertEquals(4_500_072L, attack.d());
        assertEquals(3934, attack.h());
        assertEquals(0, attack.c());
        assertEquals(0, attack.h());
        assertEquals(1, attack.h());
        assertEquals(0x1122_3344L, attack.d());
        assertEquals(0x20, attack.c());
        assertEquals(0, attack.remaining());
    }

    @Test
    void lightningStormUsesExactNativeCase64TargetCoordinatesAndDirectionalArea() {
        Protocol.AttackSpellRequest request = Protocol.attackSpellRequest(
                new Protocol.Writer().c(Protocol.C_USE_SPELL)
                        .c(8).c(0).d(0x1122_3344L).h(32731).h(32799).bytes());
        assertEquals(BattleSpellCatalog.LIGHTNING_STORM, request.skillId());
        assertEquals(8, request.row());
        assertEquals(0, request.column());
        assertEquals(0x1122_3344L, request.targetObjectId());
        assertEquals(32731, request.x());
        assertEquals(32799, request.y());

        Protocol.Reader attack = new Protocol.Reader(Protocol.attackMany(
                18, 0x0102_0304L, 32730, 32798, 3, 4_500_065L, 3924,
                List.of(new Protocol.AttackManyTarget(0x1122_3344L, true),
                        new Protocol.AttackManyTarget(0x5566_7788L, false))));
        assertEquals(Protocol.S_ATTACK_MANY, attack.c());
        assertEquals(18, attack.c());
        assertEquals(0x0102_0304L, attack.d());
        assertEquals(32730, attack.h());
        assertEquals(32798, attack.h());
        assertEquals(3, attack.c());
        assertEquals(4_500_065L, attack.d());
        assertEquals(3924, attack.h());
        assertEquals(8, attack.c());
        assertEquals(0, attack.h());
        assertEquals(2, attack.h());
        assertEquals(0x1122_3344L, attack.d());
        assertEquals(0x20, attack.c());
        assertEquals(0x5566_7788L, attack.d());
        assertEquals(0, attack.c());
        assertEquals(0, attack.remaining());
    }

    @Test
    void meteorStrikeUsesExactNativeCase73TargetCoordinatesAndDirectionalArea() {
        Protocol.AttackSpellRequest request = Protocol.attackSpellRequest(
                new Protocol.Writer().c(Protocol.C_USE_SPELL)
                        .c(9).c(1).d(0x1122_3344L).h(32731).h(32799).bytes());
        assertEquals(BattleSpellCatalog.METEOR_STRIKE, request.skillId());
        assertEquals(9, request.row());
        assertEquals(1, request.column());
        assertEquals(0x1122_3344L, request.targetObjectId());
        assertEquals(32731, request.x());
        assertEquals(32799, request.y());

        Protocol.Reader attack = new Protocol.Reader(Protocol.attackMany(
                18, 0x0102_0304L, 32730, 32798, 3, 4_500_074L, 762,
                List.of(new Protocol.AttackManyTarget(0x1122_3344L, true),
                        new Protocol.AttackManyTarget(0x5566_7788L, false))));
        assertEquals(Protocol.S_ATTACK_MANY, attack.c());
        assertEquals(18, attack.c());
        assertEquals(0x0102_0304L, attack.d());
        assertEquals(32730, attack.h());
        assertEquals(32798, attack.h());
        assertEquals(3, attack.c());
        assertEquals(4_500_074L, attack.d());
        assertEquals(762, attack.h());
        assertEquals(8, attack.c());
        assertEquals(0, attack.h());
        assertEquals(2, attack.h());
        assertEquals(0x1122_3344L, attack.d());
        assertEquals(0x20, attack.c());
        assertEquals(0x5566_7788L, attack.d());
        assertEquals(0, attack.c());
        assertEquals(0, attack.remaining());
    }

    @Test
    void blindUsesTheExactNativeSingleModeByte() {
        assertArrayEquals(new byte[] {0x0A, 0x00}, Protocol.blind(0));
        assertArrayEquals(new byte[] {0x0A, 0x01}, Protocol.blind(1));
        assertArrayEquals(new byte[] {0x0A, 0x02}, Protocol.blind(2));
        assertThrows(IllegalArgumentException.class, () -> Protocol.blind(-1));
        assertThrows(IllegalArgumentException.class, () -> Protocol.blind(3));
    }

    @Test
    void changeLightUsesTheExactNativeObjectAndRangeLayout() {
        assertArrayEquals(new byte[] {0x1B, 0x44, 0x33, 0x22, 0x11, 0x16},
                Protocol.changeLight(0x1122_3344L, 0x16));
        assertArrayEquals(new byte[] {0x1B, 0x44, 0x33, 0x22, 0x11, 0x00},
                Protocol.changeLight(0x1122_3344L, 0));
        assertThrows(IllegalArgumentException.class, () -> Protocol.changeLight(0, 1));
        assertThrows(IllegalArgumentException.class, () -> Protocol.changeLight(1, 256));
    }

    @Test
    void poisonUsesTheExactNativeObjectGreenAndGrayLayout() {
        assertArrayEquals(new byte[] {0x32, 0x44, 0x33, 0x22, 0x11, 0, 0},
                Protocol.poison(0x1122_3344L, 0));
        assertArrayEquals(new byte[] {0x32, 0x44, 0x33, 0x22, 0x11, 1, 0},
                Protocol.poison(0x1122_3344L, 1));
        assertArrayEquals(new byte[] {0x32, 0x44, 0x33, 0x22, 0x11, 0, 1},
                Protocol.poison(0x1122_3344L, 2));
        assertThrows(IllegalArgumentException.class, () -> Protocol.poison(0, 1));
        assertThrows(IllegalArgumentException.class, () -> Protocol.poison(1, 3));
    }

    @Test
    void invisibilityUsesExactNativeSelfTickAndObjectModeLayouts() {
        Protocol.SelfSpellRequest request = Protocol.selfSpellRequest(
                new Protocol.Writer().c(Protocol.C_USE_SPELL)
                        .c(7).c(3).d(0x1122_3344L).bytes());
        assertEquals(BattleSpellCatalog.INVISIBILITY, request.skillId());
        assertEquals(0x1122_3344L, request.clientTick());
        assertArrayEquals(new byte[] {0x34, 0x44, 0x33, 0x22, 0x11, 1},
                Protocol.invisible(0x1122_3344L, 1));
        assertArrayEquals(new byte[] {0x34, 0x44, 0x33, 0x22, 0x11, 0},
                Protocol.invisible(0x1122_3344L, 0));
        assertArrayEquals(new byte[] {0x15, 0x44, 0x33, 0x22, 0x11},
                Protocol.removeObject(0x1122_3344L));
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.selfSpellRequest(new Protocol.Writer()
                        .c(Protocol.C_USE_SPELL).c(7).c(3).bytes()));
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.selfSpellRequest(new Protocol.Writer()
                        .c(Protocol.C_USE_SPELL).c(7).c(3).d(1).c(0).bytes()));
        assertThrows(IllegalArgumentException.class, () -> Protocol.invisible(1, 2));
        assertThrows(IllegalArgumentException.class, () -> Protocol.removeObject(0));
    }

    @Test
    void blindHidingUsesExactNativeCase96SelectorOnlyEnvelope() {
        byte[] nativeRequest = new Protocol.Writer().c(Protocol.C_USE_SPELL)
                .c(12).c(0).bytes();
        assertArrayEquals(new byte[] {0x14, 0x0C, 0x00}, nativeRequest);
        Protocol.SpellSelector request = Protocol.selectorOnlySpellRequest(nativeRequest);
        assertEquals(BattleSpellCatalog.BLIND_HIDING, request.skillId());
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.selectorOnlySpellRequest(new Protocol.Writer()
                        .c(Protocol.C_USE_SPELL).c(12).c(0).d(3).bytes()));
    }

    @Test
    void enchantVenomUsesExactNativeCase97SelectorOnlyEnvelope() {
        byte[] nativeRequest = new Protocol.Writer().c(Protocol.C_USE_SPELL)
                .c(12).c(1).bytes();
        assertArrayEquals(new byte[] {0x14, 0x0C, 0x01}, nativeRequest);
        Protocol.SpellSelector request = Protocol.selectorOnlySpellRequest(nativeRequest);
        assertEquals(BattleSpellCatalog.ENCHANT_VENOM, request.skillId());
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.selectorOnlySpellRequest(new Protocol.Writer()
                        .c(Protocol.C_USE_SPELL).c(12).c(1).d(3).bytes()));
    }

    @Test
    void summonMonsterUsesChoiceDwordAndExactEmptyHypertextClose() {
        Protocol.SummonSpellRequest request = Protocol.summonSpellRequest(
                new Protocol.Writer().c(Protocol.C_USE_SPELL)
                        .c(6).c(2).d(523).bytes());
        assertEquals(BattleSpellCatalog.SUMMON_MONSTER, request.skillId());
        assertEquals(523, request.selection());
        assertArrayEquals(new byte[] {
                        0x2A, 0x44, 0x33, 0x22, 0x11, 0, 0, 0, 0},
                Protocol.closeHypertext(0x1122_3344L));
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.summonSpellRequest(new Protocol.Writer()
                        .c(Protocol.C_USE_SPELL).c(6).c(2).bytes()));
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.summonSpellRequest(new Protocol.Writer()
                        .c(Protocol.C_USE_SPELL).c(6).c(2).d(0).bytes()));
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.summonSpellRequest(new Protocol.Writer()
                        .c(Protocol.C_USE_SPELL).c(6).c(2).d(7).c(0).bytes()));
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.closeHypertext(0));
    }

    @Test
    void curseParalysisUsesExactNativeTargetCoordinatesAndModeByte() {
        Protocol.AttackSpellRequest request = Protocol.attackSpellRequest(
                new Protocol.Writer().c(Protocol.C_USE_SPELL)
                        .c(4).c(0).d(0x1122_3344L).h(32731).h(32799).bytes());
        assertEquals(BattleSpellCatalog.CURSE_PARALYZE, request.skillId());
        assertEquals(0x1122_3344L, request.targetObjectId());
        assertEquals(32731, request.x());
        assertEquals(32799, request.y());
        assertArrayEquals(new byte[] {0x25, 0x02}, Protocol.paralysis(
                Protocol.ParalysisMode.PARALYSIS_APPLY));
        assertArrayEquals(new byte[] {0x25, 0x03}, Protocol.paralysis(
                Protocol.ParalysisMode.PARALYSIS_RELEASE));
    }

    @Test
    void tamingMonsterUsesExactNativeCase35ObjectCoordinateEnvelope() {
        Protocol.AttackSpellRequest request = Protocol.attackSpellRequest(
                new Protocol.Writer().c(Protocol.C_USE_SPELL)
                        .c(4).c(3).d(0x6C00_0001L).h(32731).h(32799).bytes());
        assertEquals(BattleSpellCatalog.TAMING_MONSTER, request.skillId());
        assertEquals(4, request.row());
        assertEquals(3, request.column());
        assertEquals(0x6C00_0001L, request.targetObjectId());
        assertEquals(32731, request.x());
        assertEquals(32799, request.y());
    }

    @Test
    void curePoisonUsesExactNativeCase8TargetOnlyEnvelope() {
        Protocol.ObjectSpellRequest request = Protocol.objectSpellRequest(
                new Protocol.Writer().c(Protocol.C_USE_SPELL)
                        .c(1).c(0).d(0x1122_3309L).bytes());
        assertEquals(BattleSpellCatalog.CURE_POISON, request.skillId());
        assertEquals(1, request.row());
        assertEquals(0, request.column());
        assertEquals(0x1122_3309L, request.targetObjectId());
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.objectSpellRequest(new Protocol.Writer()
                        .c(Protocol.C_USE_SPELL).c(1).c(0)
                        .d(0x1122_3309L).c(0).bytes()));
    }

    @Test
    void chillTouchUsesExactNativeCase9ObjectCoordinateEnvelope() {
        Protocol.AttackSpellRequest request = Protocol.attackSpellRequest(
                new Protocol.Writer().c(Protocol.C_USE_SPELL)
                        .c(1).c(1).d(0x1122_330AL).h(32731).h(32799).bytes());
        assertEquals(BattleSpellCatalog.CHILL_TOUCH, request.skillId());
        assertEquals(1, request.row());
        assertEquals(1, request.column());
        assertEquals(0x1122_330AL, request.targetObjectId());
        assertEquals(32731, request.x());
        assertEquals(32799, request.y());
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.attackSpellRequest(new Protocol.Writer()
                        .c(Protocol.C_USE_SPELL).c(1).c(1)
                        .d(0x1122_330AL).h(32731).bytes()));
    }

    @Test
    void vampiricTouchUsesExactNativeCase27ObjectCoordinateEnvelope() {
        byte[] payload = new Protocol.Writer().c(Protocol.C_USE_SPELL)
                .c(3).c(3).d(0x1122_331CL).h(33312).h(32339).bytes();
        assertArrayEquals(new byte[] {
                0x14, 0x03, 0x03, 0x1C, 0x33, 0x22, 0x11,
                0x20, (byte) 0x82, 0x53, 0x7E}, payload);
        Protocol.AttackSpellRequest request = Protocol.attackSpellRequest(payload);
        assertEquals(BattleSpellCatalog.VAMPIRIC_TOUCH, request.skillId());
        assertEquals(3, request.row());
        assertEquals(3, request.column());
        assertEquals(0x1122_331CL, request.targetObjectId());
        assertEquals(33312, request.x());
        assertEquals(32339, request.y());
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.attackSpellRequest(new Protocol.Writer()
                        .c(Protocol.C_USE_SPELL).c(3).c(3)
                        .d(0x1122_331CL).h(33312).bytes()));
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.attackSpellRequest(new Protocol.Writer()
                        .c(Protocol.C_USE_SPELL).c(3).c(3)
                        .d(0x1122_331CL).h(33312).h(32339).c(0).bytes()));
    }

    @Test
    void slowUsesExactNativeCase28ObjectCoordinateEnvelope() {
        byte[] payload = new Protocol.Writer().c(Protocol.C_USE_SPELL)
                .c(3).c(4).d(0x1122_331DL).h(33312).h(32339).bytes();
        assertArrayEquals(new byte[] {
                0x14, 0x03, 0x04, 0x1D, 0x33, 0x22, 0x11,
                0x20, (byte) 0x82, 0x53, 0x7E}, payload);
        Protocol.AttackSpellRequest request = Protocol.attackSpellRequest(payload);
        assertEquals(BattleSpellCatalog.SLOW, request.skillId());
        assertEquals(3, request.row());
        assertEquals(4, request.column());
        assertEquals(0x1122_331DL, request.targetObjectId());
        assertEquals(33312, request.x());
        assertEquals(32339, request.y());
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.attackSpellRequest(new Protocol.Writer()
                        .c(Protocol.C_USE_SPELL).c(3).c(4)
                        .d(0x1122_331DL).h(33312).bytes()));
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.attackSpellRequest(new Protocol.Writer()
                        .c(Protocol.C_USE_SPELL).c(3).c(4)
                        .d(0x1122_331DL).h(33312).h(32339).c(0).bytes()));
    }

    @Test
    void revealWeaknessUsesExactNativeCase22TargetOnlyEnvelope() {
        byte[] packet = new Protocol.Writer().c(Protocol.C_USE_SPELL)
                .c(2).c(6).d(0x1122_3317L).bytes();
        assertArrayEquals(new byte[] {
                0x14, 0x02, 0x06, 0x17, 0x33, 0x22, 0x11
        }, packet);
        Protocol.ObjectSpellRequest request = Protocol.objectSpellRequest(packet);
        assertEquals(BattleSpellCatalog.REVEAL_WEAKNESS, request.skillId());
        assertEquals(2, request.row());
        assertEquals(6, request.column());
        assertEquals(0x1122_3317L, request.targetObjectId());
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.objectSpellRequest(new Protocol.Writer()
                        .c(Protocol.C_USE_SPELL).c(2).c(6)
                        .d(0x1122_3317L).c(0).bytes()));
    }

    @Test
    void cursePoisonUsesExactNativeCase10ObjectCoordinateEnvelope() {
        Protocol.AttackSpellRequest request = Protocol.attackSpellRequest(
                new Protocol.Writer().c(Protocol.C_USE_SPELL)
                        .c(1).c(2).d(0x1122_330BL).h(32731).h(32799).bytes());
        assertEquals(BattleSpellCatalog.CURSE_POISON, request.skillId());
        assertEquals(1, request.row());
        assertEquals(2, request.column());
        assertEquals(0x1122_330BL, request.targetObjectId());
        assertEquals(32731, request.x());
        assertEquals(32799, request.y());
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.attackSpellRequest(new Protocol.Writer()
                        .c(Protocol.C_USE_SPELL).c(1).c(2)
                        .d(0x1122_330BL).h(32731).bytes()));
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.objectSpellRequest(new Protocol.Writer()
                        .c(Protocol.C_USE_SPELL).c(1).c(2)
                        .d(0x1122_330BL).h(32731).h(32799).bytes()));
    }

    @Test
    void removeCurseUsesExactNativeCase36TargetOnlyEnvelope() {
        Protocol.ObjectSpellRequest request = Protocol.objectSpellRequest(
                new Protocol.Writer().c(Protocol.C_USE_SPELL)
                        .c(4).c(4).d(0x1122_3344L).bytes());
        assertEquals(BattleSpellCatalog.REMOVE_CURSE, request.skillId());
        assertEquals(4, request.row());
        assertEquals(4, request.column());
        assertEquals(0x1122_3344L, request.targetObjectId());
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.objectSpellRequest(new Protocol.Writer()
                        .c(Protocol.C_USE_SPELL).c(4).c(4)
                        .d(0x1122_3344L).c(0).bytes()));
    }

    @Test
    void coneOfColdUsesExactNativeCase37ObjectCoordinateEnvelope() {
        Protocol.AttackSpellRequest request = Protocol.attackSpellRequest(
                new Protocol.Writer().c(Protocol.C_USE_SPELL)
                        .c(4).c(5).d(0x6C00_0002L).h(32733).h(32801).bytes());
        assertEquals(BattleSpellCatalog.CONE_OF_COLD, request.skillId());
        assertEquals(4, request.row());
        assertEquals(5, request.column());
        assertEquals(0x6C00_0002L, request.targetObjectId());
        assertEquals(32733, request.x());
        assertEquals(32801, request.y());
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.attackSpellRequest(new Protocol.Writer()
                        .c(Protocol.C_USE_SPELL).c(4).c(5).d(0x6C00_0002L)
                        .h(32733).h(32801).c(0).bytes()));
    }

    @Test
    void eruptionUsesExactNativeCase44ObjectCoordinateEnvelope() {
        Protocol.AttackSpellRequest request = Protocol.attackSpellRequest(
                new Protocol.Writer().c(Protocol.C_USE_SPELL)
                        .c(5).c(4).d(0x6C00_0045L).h(32729).h(32824).bytes());
        assertEquals(BattleSpellCatalog.ERUPTION, request.skillId());
        assertEquals(5, request.row());
        assertEquals(4, request.column());
        assertEquals(0x6C00_0045L, request.targetObjectId());
        assertEquals(32729, request.x());
        assertEquals(32824, request.y());
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.attackSpellRequest(new Protocol.Writer()
                        .c(Protocol.C_USE_SPELL).c(5).c(4).d(0x6C00_0045L)
                        .h(32729).h(32824).c(0).bytes()));
    }

    @Test
    void sunburstUsesExactNativeCase45ObjectCoordinateEnvelope() {
        Protocol.AttackSpellRequest request = Protocol.attackSpellRequest(
                new Protocol.Writer().c(Protocol.C_USE_SPELL)
                        .c(5).c(5).d(0x6C00_0046L).h(32729).h(32824).bytes());
        assertEquals(BattleSpellCatalog.SUNBURST, request.skillId());
        assertEquals(5, request.row());
        assertEquals(5, request.column());
        assertEquals(0x6C00_0046L, request.targetObjectId());
        assertEquals(32729, request.x());
        assertEquals(32824, request.y());
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.attackSpellRequest(new Protocol.Writer()
                        .c(Protocol.C_USE_SPELL).c(5).c(5).d(0x6C00_0046L)
                        .h(32729).h(32824).c(0).bytes()));
    }

    @Test
    void weaknessUsesExactNativeCase46ObjectCoordinateEnvelope() {
        Protocol.AttackSpellRequest request = Protocol.attackSpellRequest(
                new Protocol.Writer().c(Protocol.C_USE_SPELL)
                        .c(5).c(6).d(0x6C00_0047L).h(32729).h(32824).bytes());
        assertEquals(BattleSpellCatalog.WEAKNESS, request.skillId());
        assertEquals(5, request.row());
        assertEquals(6, request.column());
        assertEquals(0x6C00_0047L, request.targetObjectId());
        assertEquals(32729, request.x());
        assertEquals(32824, request.y());
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.attackSpellRequest(new Protocol.Writer()
                        .c(Protocol.C_USE_SPELL).c(5).c(6).d(0x6C00_0047L)
                        .h(32729).h(32824).c(0).bytes()));
    }

    @Test
    void diseaseUsesExactNativeCase55ObjectCoordinateEnvelopeAndEffect() {
        Protocol.AttackSpellRequest request = Protocol.attackSpellRequest(
                new Protocol.Writer().c(Protocol.C_USE_SPELL)
                        .c(6).c(7).d(0x1122_3344L).h(32729).h(32824).bytes());
        assertEquals(BattleSpellCatalog.DISEASE, request.skillId());
        assertEquals(6, request.row());
        assertEquals(7, request.column());
        assertEquals(0x1122_3344L, request.targetObjectId());
        assertEquals(32729, request.x());
        assertEquals(32824, request.y());
        assertArrayEquals(new byte[] {0x37, 0x44, 0x33, 0x22, 0x11,
                (byte) 0xB6, 0x08},
                Protocol.effectObject(0x1122_3344L, 2230));
    }

    @Test
    void holyWeaponUsesExactNativeCase7ObjectOnlyEnvelope() {
        Protocol.ObjectSpellRequest request = Protocol.objectSpellRequest(
                new Protocol.Writer().c(Protocol.C_USE_SPELL)
                        .c(0).c(7).d(0x6C00_0008L).bytes());
        assertEquals(BattleSpellCatalog.HOLY_WEAPON, request.skillId());
        assertEquals(0, request.row());
        assertEquals(7, request.column());
        assertEquals(0x6C00_0008L, request.targetObjectId());
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.objectSpellRequest(new Protocol.Writer()
                        .c(Protocol.C_USE_SPELL).c(0).c(7).bytes()));
    }

    @Test
    void blessWeaponUsesExactNativeCase47ObjectOnlyEnvelopeAndMessageArguments() {
        Protocol.ObjectSpellRequest request = Protocol.objectSpellRequest(
                new Protocol.Writer().c(Protocol.C_USE_SPELL)
                        .c(5).c(7).d(0x6C00_0048L).bytes());
        assertEquals(BattleSpellCatalog.BLESS_WEAPON, request.skillId());
        assertEquals(5, request.row());
        assertEquals(7, request.column());
        assertEquals(0x6C00_0048L, request.targetObjectId());
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.objectSpellRequest(new Protocol.Writer()
                        .c(Protocol.C_USE_SPELL).c(5).c(7).d(0x6C00_0048L)
                        .h(32700).h(32700).bytes()));

        Protocol.Reader success = new Protocol.Reader(
                Protocol.messageCode(161, "+0 $100", "$245", "$247"));
        assertEquals(Protocol.S_MESSAGE_CODE, success.c());
        assertEquals(161, success.h());
        assertEquals(3, success.c());
        assertEquals("+0 $100", success.s());
        assertEquals("$245", success.s());
        assertEquals("$247", success.s());
        assertEquals(0, success.remaining());
    }

    @Test
    void fireWeaponUsesExactNativeCase147ObjectOnlyEnvelope() {
        Protocol.ObjectSpellRequest request = Protocol.objectSpellRequest(
                new Protocol.Writer().c(Protocol.C_USE_SPELL)
                        .c(18).c(3).d(0x3810_1480L).bytes());
        assertEquals(BattleSpellCatalog.FIRE_WEAPON, request.skillId());
        assertEquals(18, request.row());
        assertEquals(3, request.column());
        assertEquals(0x3810_1480L, request.targetObjectId());
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.objectSpellRequest(new Protocol.Writer()
                        .c(Protocol.C_USE_SPELL).c(18).c(3)
                        .d(0x3810_1480L).h(32700).bytes()));
    }

    @Test
    void burningWeaponUsesExactNativeCase162ObjectOnlyEnvelope() {
        byte[] nativeRequest = new Protocol.Writer().c(Protocol.C_USE_SPELL)
                .c(20).c(2).d(0x3810_1630L).bytes();
        assertArrayEquals(new byte[] {
                0x14, 0x14, 0x02, 0x30, 0x16, 0x10, 0x38
        }, nativeRequest);
        Protocol.ObjectSpellRequest request =
                Protocol.objectSpellRequest(nativeRequest);
        assertEquals(BattleSpellCatalog.BURNING_WEAPON, request.skillId());
        assertEquals(20, request.row());
        assertEquals(2, request.column());
        assertEquals(0x3810_1630L, request.targetObjectId());
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.objectSpellRequest(new Protocol.Writer()
                        .c(Protocol.C_USE_SPELL).c(20).c(2).bytes()));
    }

    @Test
    void windShotUsesExactNativeCase148ObjectOnlyEnvelope() {
        Protocol.ObjectSpellRequest request = Protocol.objectSpellRequest(
                new Protocol.Writer().c(Protocol.C_USE_SPELL)
                        .c(18).c(4).d(0x3810_1490L).bytes());
        assertEquals(BattleSpellCatalog.WIND_SHOT, request.skillId());
        assertEquals(18, request.row());
        assertEquals(4, request.column());
        assertEquals(0x3810_1490L, request.targetObjectId());
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.objectSpellRequest(new Protocol.Writer()
                        .c(Protocol.C_USE_SPELL).c(18).c(4)
                        .d(0x3810_1490L).h(32700).bytes()));
    }

    @Test
    void fireBlessUsesExactNativeCase154SelectorOnlyEnvelope() {
        Protocol.SpellSelector request = Protocol.selectorOnlySpellRequest(
                new Protocol.Writer().c(Protocol.C_USE_SPELL)
                        .c(19).c(2).bytes());
        assertEquals(BattleSpellCatalog.FIRE_BLESS, request.skillId());
        assertEquals(19, request.row());
        assertEquals(2, request.column());
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.selectorOnlySpellRequest(new Protocol.Writer()
                        .c(Protocol.C_USE_SPELL).c(19).c(2).d(1).bytes()));
    }

    @Test
    void stormEyeUsesExactNativeCase155SelectorOnlyEnvelope() {
        Protocol.SpellSelector request = Protocol.selectorOnlySpellRequest(
                new Protocol.Writer().c(Protocol.C_USE_SPELL)
                        .c(19).c(3).bytes());
        assertEquals(BattleSpellCatalog.STORM_EYE, request.skillId());
        assertEquals(19, request.row());
        assertEquals(3, request.column());
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.selectorOnlySpellRequest(new Protocol.Writer()
                        .c(Protocol.C_USE_SPELL).c(19).c(3).d(1).bytes()));
    }

    @Test
    void naturesTouchUsesExactNativeCase157ObjectOnlyEnvelope() {
        Protocol.ObjectSpellRequest request = Protocol.objectSpellRequest(
                new Protocol.Writer().c(Protocol.C_USE_SPELL)
                        .c(19).c(5).d(0x3810_1580L).bytes());
        assertEquals(BattleSpellCatalog.NATURES_TOUCH, request.skillId());
        assertEquals(19, request.row());
        assertEquals(5, request.column());
        assertEquals(0x3810_1580L, request.targetObjectId());
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.objectSpellRequest(new Protocol.Writer()
                        .c(Protocol.C_USE_SPELL).c(19).c(5)
                        .d(0x3810_1580L).h(32700).bytes()));
    }

    @Test
    void earthBlessUsesExactNativeCase158SelectorOnlyEnvelope() {
        Protocol.SpellSelector request = Protocol.selectorOnlySpellRequest(
                new Protocol.Writer().c(Protocol.C_USE_SPELL)
                        .c(19).c(6).bytes());
        assertEquals(BattleSpellCatalog.EARTH_BLESS, request.skillId());
        assertEquals(19, request.row());
        assertEquals(6, request.column());
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.selectorOnlySpellRequest(new Protocol.Writer()
                        .c(Protocol.C_USE_SPELL).c(19).c(6).d(1).bytes()));
    }

    @Test
    void aquaProtectUsesExactNativeCase159ObjectOnlyEnvelope() {
        Protocol.ObjectSpellRequest request = Protocol.objectSpellRequest(
                new Protocol.Writer().c(Protocol.C_USE_SPELL)
                        .c(19).c(7).d(0x3810_1600L).bytes());
        assertEquals(BattleSpellCatalog.AQUA_PROTECTER, request.skillId());
        assertEquals(19, request.row());
        assertEquals(7, request.column());
        assertEquals(0x3810_1600L, request.targetObjectId());
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.objectSpellRequest(new Protocol.Writer()
                        .c(Protocol.C_USE_SPELL).c(19).c(7)
                        .d(0x3810_1600L).h(32700).bytes()));
    }

    @Test
    void areaOfSilenceUsesExactNativeCase160SelectorOnlyEnvelope() {
        Protocol.SpellSelector request = Protocol.selectorOnlySpellRequest(
                new Protocol.Writer().c(Protocol.C_USE_SPELL)
                        .c(20).c(0).bytes());
        assertEquals(BattleSpellCatalog.AREA_OF_SILENCE, request.skillId());
        assertEquals(20, request.row());
        assertEquals(0, request.column());
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.selectorOnlySpellRequest(new Protocol.Writer()
                        .c(Protocol.C_USE_SPELL).c(20).c(0).d(1).bytes()));
    }

    @Test
    void createMagicalWeaponUsesExactNativeCase72InventoryObjectEnvelope() {
        Protocol.ObjectSpellRequest request = Protocol.objectSpellRequest(
                new Protocol.Writer().c(Protocol.C_USE_SPELL)
                        .c(9).c(0).d(0x6C00_0049L).bytes());
        assertEquals(BattleSpellCatalog.CREATE_MAGICAL_WEAPON, request.skillId());
        assertEquals(9, request.row());
        assertEquals(0, request.column());
        assertEquals(0x6C00_0049L, request.targetObjectId());
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.objectSpellRequest(new Protocol.Writer()
                        .c(Protocol.C_USE_SPELL).c(9).c(0).d(0x6C00_0049L)
                        .c(0).bytes()));

        // Retained L1SkillUse prefixes "+0 " twice for an identified +0 weapon.
        Protocol.Reader success = new Protocol.Reader(
                Protocol.messageCode(161, "+0 +0 $100", "$245", "$247"));
        assertEquals(Protocol.S_MESSAGE_CODE, success.c());
        assertEquals(161, success.h());
        assertEquals(3, success.c());
        assertEquals("+0 +0 $100", success.s());
        assertEquals("$245", success.s());
        assertEquals("$247", success.s());
        assertEquals(0, success.remaining());
    }

    @Test
    void manaDrainUsesExactNativeCase38ObjectCoordinateEnvelope() {
        Protocol.AttackSpellRequest request = Protocol.attackSpellRequest(
                new Protocol.Writer().c(Protocol.C_USE_SPELL)
                        .c(4).c(6).d(0x6C00_0002L).h(32733).h(32801).bytes());
        assertEquals(BattleSpellCatalog.MANA_DRAIN, request.skillId());
        assertEquals(4, request.row());
        assertEquals(6, request.column());
        assertEquals(0x6C00_0002L, request.targetObjectId());
        assertEquals(32733, request.x());
        assertEquals(32801, request.y());
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.attackSpellRequest(new Protocol.Writer()
                        .c(Protocol.C_USE_SPELL).c(4).c(6).d(0x6C00_0002L)
                        .h(32733).h(32801).c(0).bytes()));
    }

    @Test
    void curseBlindUsesNativeObjectCoordinateRequestAndOneByteState() {
        Protocol.AttackSpellRequest request = Protocol.attackSpellRequest(
                new Protocol.Writer().c(Protocol.C_USE_SPELL)
                        .c(2).c(3).d(0x1122_3344L).h(32731).h(32799).bytes());
        assertEquals(BattleSpellCatalog.CURSE_BLIND, request.skillId());
        assertEquals(0x1122_3344L, request.targetObjectId());
        assertEquals(32731, request.x());
        assertEquals(32799, request.y());
        assertArrayEquals(new byte[] {(byte) Protocol.S_BLIND, 1}, Protocol.blind(1));
        assertArrayEquals(new byte[] {(byte) Protocol.S_BLIND, 0}, Protocol.blind(0));
    }

    @Test
    void darknessUsesExactNativeCase39ObjectCoordinateEnvelope() {
        Protocol.AttackSpellRequest request = Protocol.attackSpellRequest(
                new Protocol.Writer().c(Protocol.C_USE_SPELL)
                        .c(4).c(7).d(0x6C00_0002L).h(32733).h(32801).bytes());
        assertEquals(BattleSpellCatalog.DARKNESS, request.skillId());
        assertEquals(4, request.row());
        assertEquals(7, request.column());
        assertEquals(0x6C00_0002L, request.targetObjectId());
        assertEquals(32733, request.x());
        assertEquals(32801, request.y());
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.attackSpellRequest(new Protocol.Writer()
                        .c(Protocol.C_USE_SPELL).c(4).c(7).d(0x6C00_0002L)
                        .h(32733).h(32801).c(0).bytes()));
    }

    @Test
    void createZombieUsesExactNativeCase40CorpseObjectEnvelope() {
        Protocol.ObjectSpellRequest request = Protocol.objectSpellRequest(
                new Protocol.Writer().c(Protocol.C_USE_SPELL)
                        .c(5).c(0).d(0x6D00_0000L).bytes());
        assertEquals(BattleSpellCatalog.CREATE_ZOMBIE, request.skillId());
        assertEquals(5, request.row());
        assertEquals(0, request.column());
        assertEquals(0x6D00_0000L, request.targetObjectId());
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.objectSpellRequest(new Protocol.Writer()
                        .c(Protocol.C_USE_SPELL).c(5).c(0).d(0x6D00_0000L)
                        .h(32733).h(32801).bytes()));
    }

    @Test
    void fogOfSleepingUsesNativeObjectCoordinatesAndSleepModePair() {
        Protocol.AttackSpellRequest request = Protocol.attackSpellRequest(
                new Protocol.Writer().c(Protocol.C_USE_SPELL)
                        .c(8).c(1).d(0x1122_3344L).h(32731).h(32799).bytes());
        assertEquals(BattleSpellCatalog.FOG_OF_SLEEPING, request.skillId());
        assertEquals(0x1122_3344L, request.targetObjectId());
        assertEquals(32731, request.x());
        assertEquals(32799, request.y());
        assertArrayEquals(new byte[] {0x25, 0x0A}, Protocol.paralysis(
                Protocol.ParalysisMode.SLEEP_APPLY));
        assertArrayEquals(new byte[] {0x25, 0x0B}, Protocol.paralysis(
                Protocol.ParalysisMode.SLEEP_RELEASE));
    }

    @Test
    void massSlowUsesExactNativeCase75CoordinatesAndSlowSpeedResponse() {
        byte[] nativeRequest = new Protocol.Writer().c(Protocol.C_USE_SPELL)
                .c(9).c(3).d(0x1122_3344L).h(32731).h(32799).bytes();
        assertArrayEquals(new byte[] {
                0x14, 0x09, 0x03, 0x44, 0x33, 0x22, 0x11,
                (byte) 0xDB, 0x7F, 0x1F, (byte) 0x80
        }, nativeRequest);
        Protocol.AttackSpellRequest request =
                Protocol.attackSpellRequest(nativeRequest);
        assertEquals(BattleSpellCatalog.MASS_SLOW, request.skillId());
        assertEquals(0x1122_3344L, request.targetObjectId());
        assertEquals(32731, request.x());
        assertEquals(32799, request.y());
        assertArrayEquals(new byte[] {
                (byte) Protocol.S_SPEED, 0x44, 0x33, 0x22, 0x11,
                0x02, 0x40, 0x00
        }, Protocol.speed(0x1122_3344L, Protocol.SpeedMode.SLOW, 64));
    }

    @Test
    void disintegrateUsesExactNativeCase76TargetAndCoordinates() {
        byte[] nativeRequest = new Protocol.Writer().c(Protocol.C_USE_SPELL)
                .c(9).c(4).d(0x1122_3344L).h(32731).h(32799).bytes();
        assertArrayEquals(new byte[] {
                0x14, 0x09, 0x04, 0x44, 0x33, 0x22, 0x11,
                (byte) 0xDB, 0x7F, 0x1F, (byte) 0x80
        }, nativeRequest);
        Protocol.AttackSpellRequest request =
                Protocol.attackSpellRequest(nativeRequest);
        assertEquals(BattleSpellCatalog.DISINTEGRATE, request.skillId());
        assertEquals(0x1122_3344L, request.targetObjectId());
        assertEquals(32731, request.x());
        assertEquals(32799, request.y());
        assertArrayEquals(new byte[] {
                (byte) Protocol.S_MESSAGE_CODE, 0x60, 0x01, 0x01,
                0x24, 0x39, 0x36, 0x37, 0x00
        }, Protocol.messageCode(352, "$967"));
    }

    @Test
    void absoluteBarrierUsesExactNativeCase77SelectorOnlyPacket() {
        byte[] nativeRequest = new Protocol.Writer().c(Protocol.C_USE_SPELL)
                .c(9).c(5).bytes();
        assertArrayEquals(new byte[] {0x14, 0x09, 0x05}, nativeRequest);
        Protocol.SpellSelector request = Protocol.selectorOnlySpellRequest(nativeRequest);
        assertEquals(BattleSpellCatalog.ABSOLUTE_BARRIER, request.skillId());
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.selectorOnlySpellRequest(new Protocol.Writer()
                        .c(Protocol.C_USE_SPELL).c(9).c(5).c(0).bytes()));
    }

    @Test
    void advanceSpiritUsesExactNativeCase78PlayerObjectPacket() {
        byte[] nativeRequest = new Protocol.Writer().c(Protocol.C_USE_SPELL)
                .c(9).c(6).d(0x1122_3344L).bytes();
        assertArrayEquals(new byte[] {
                0x14, 0x09, 0x06, 0x44, 0x33, 0x22, 0x11
        }, nativeRequest);
        Protocol.ObjectSpellRequest request = Protocol.objectSpellRequest(nativeRequest);
        assertEquals(BattleSpellCatalog.ADVANCE_SPIRIT, request.skillId());
        assertEquals(0x1122_3344L, request.targetObjectId());
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.objectSpellRequest(new Protocol.Writer()
                        .c(Protocol.C_USE_SPELL).c(9).c(6).bytes()));
    }

    @Test
    void iceLanceUsesNativeObjectCoordinatesAndFreezeModePair() {
        Protocol.AttackSpellRequest request = Protocol.attackSpellRequest(
                new Protocol.Writer().c(Protocol.C_USE_SPELL)
                        .c(6).c(1).d(0x1122_3344L).h(32731).h(32799).bytes());
        assertEquals(BattleSpellCatalog.ICE_LANCE, request.skillId());
        assertEquals(0x1122_3344L, request.targetObjectId());
        assertEquals(32731, request.x());
        assertEquals(32799, request.y());
        assertArrayEquals(new byte[] {0x25, 0x0C}, Protocol.paralysis(
                Protocol.ParalysisMode.FREEZE_APPLY));
        assertArrayEquals(new byte[] {0x25, 0x0D}, Protocol.paralysis(
                Protocol.ParalysisMode.FREEZE_RELEASE));
    }

    @Test
    void earthBindUsesExactNativeObjectCoordinateEnvelopeAndFreezeModePair() {
        Protocol.AttackSpellRequest request = Protocol.attackSpellRequest(
                new Protocol.Writer().c(Protocol.C_USE_SPELL)
                        .c(19).c(4).d(0x1122_3344L).h(32731).h(32799).bytes());
        assertEquals(BattleSpellCatalog.EARTH_BIND, request.skillId());
        assertEquals(0x1122_3344L, request.targetObjectId());
        assertEquals(32731, request.x());
        assertEquals(32799, request.y());
        assertArrayEquals(new byte[] {0x25, 0x0C}, Protocol.paralysis(
                Protocol.ParalysisMode.FREEZE_APPLY));
        assertArrayEquals(new byte[] {0x25, 0x0D}, Protocol.paralysis(
                Protocol.ParalysisMode.FREEZE_RELEASE));
    }

    @Test
    void hitRatioUsesNativeDcLayoutAndAuthoritativeFloorPercentage() {
        Protocol.Reader packet = new Protocol.Reader(
                Protocol.hitRatio(0x1122_3344L, 37, 50));

        assertEquals(Protocol.S_HIT_RATIO, packet.c());
        assertEquals(0x1122_3344L, packet.d());
        assertEquals(74, packet.c());
        assertEquals(0, packet.remaining());
        assertEquals(0, Protocol.healthRatio(0, 50));
        assertEquals(100, Protocol.healthRatio(50, 50));
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.hitRatio(1, 51, 50));
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.hitRatio(1, 0, 0));
    }

    @Test
    void resurrectionUsesExactNativeCorpseResetLayout() {
        Protocol.Reader packet = new Protocol.Reader(
                Protocol.resurrect(0x1234_5678L, 0, 0x1020_3040L, 61));

        assertEquals(Protocol.S_RESURRECT, packet.c());
        assertEquals(0x1234_5678L, packet.d());
        assertEquals(0, packet.c());
        assertEquals(0x1020_3040L, packet.d());
        assertEquals(61, packet.h());
        assertEquals(0, packet.remaining());

        assertThrows(IllegalArgumentException.class,
                () -> Protocol.resurrect(0, 0, 1, 61));
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.resurrect(1, 256, 1, 61));
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.resurrect(1, 0, 0, 61));
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.resurrect(1, 0, 1, 0x1_0000));
    }

    @Test
    void resurrectionAskUsesNativeCompactQuestion321Layout() {
        Protocol.Reader packet = new Protocol.Reader(Protocol.resurrectionAsk());
        assertEquals(Protocol.S_ASK, packet.c());
        assertEquals(321, packet.h());
        assertEquals(0, packet.remaining());
    }

    @Test
    void greaterResurrectionAskUsesNativeGenericQuestion322EmptyStringLayout() {
        Protocol.Reader packet = new Protocol.Reader(Protocol.greaterResurrectionAsk());
        assertEquals(Protocol.S_ASK, packet.c());
        assertEquals(322, packet.h());
        assertEquals("", packet.s());
        assertEquals(0, packet.remaining());
    }

    @Test
    void groundSpellUsesNativeSelectorCoordinatesAndLocationEffectLayout() {
        Protocol.GroundSpellRequest request = Protocol.groundSpellRequest(
                new Protocol.Writer().c(Protocol.C_USE_SPELL)
                        .c(7).c(1).h(32731).h(32799).bytes());
        assertEquals(7, request.row());
        assertEquals(1, request.column());
        assertEquals(BattleSpellCatalog.FIRE_WALL, request.skillId());
        assertEquals(32731, request.x());
        assertEquals(32799, request.y());

        Protocol.Reader effect = new Protocol.Reader(
                Protocol.effectLocation(request.x(), request.y(), 168));
        assertEquals(Protocol.S_EFFECT_LOC, effect.c());
        assertEquals(32731, effect.h());
        assertEquals(32799, effect.h());
        assertEquals(168, effect.h());
        assertEquals(0, effect.remaining());

        assertThrows(IllegalArgumentException.class,
                () -> Protocol.groundSpellRequest(new Protocol.Writer()
                        .c(Protocol.C_USE_SPELL).c(7).c(1).h(1).bytes()));
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.spellSelector(new Protocol.Writer()
                        .c(Protocol.C_USE_SPELL).c(7).c(8).bytes()));
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.effectLocation(1, 2, 0));
    }

    @Test
    void groundSpellCatalogKeepsSkillIdsSeparateFromEffectResources() {
        BattleSpellCatalog.GroundSpell fireWall =
                BattleSpellCatalog.groundSpell(BattleSpellCatalog.FIRE_WALL);
        assertEquals(18, fireWall.actionId());
        assertEquals(168, fireWall.effectGfx());
        assertEquals(8, fireWall.maximumRange());
        assertEquals(60, fireWall.mpCost());
        assertEquals(10, fireWall.reuseDelayMillis());
        assertEquals(10, fireWall.durationSeconds());
        assertEquals(1_650, fireWall.pulseIntervalMillis());
        assertEquals(1, fireWall.effectRadius());
        assertEquals(8, fireWall.segmentCount());
        assertEquals(53, fireWall.effectiveMpCost(19, 0));
        assertEquals(51, fireWall.effectiveMpCost(19, 2));

        BattleSpellCatalog.GroundSpell lifeStream =
                BattleSpellCatalog.groundSpell(BattleSpellCatalog.LIFE_STREAM);
        assertEquals(7, lifeStream.row());
        assertEquals(6, lifeStream.column());
        assertEquals(19, lifeStream.actionId());
        assertEquals(2231, lifeStream.effectGfx());
        assertEquals(20, lifeStream.mpCost());
        assertEquals(320, lifeStream.durationSeconds());
        assertEquals(0, lifeStream.pulseIntervalMillis());
        assertEquals(3, lifeStream.effectRadius());
        assertEquals(40318, lifeStream.itemConsumeId());
        assertEquals(1, lifeStream.itemConsumeCount());
        assertEquals(13, lifeStream.effectiveMpCost(19, 0));
        assertEquals(11, lifeStream.effectiveMpCost(19, 2));
    }

    @Test
    void objectHealUsesNativeTargetAndEffectLayouts() {
        Protocol.ObjectSpellRequest request = Protocol.objectSpellRequest(
                new Protocol.Writer().c(Protocol.C_USE_SPELL)
                        .c(0).c(0).d(0x1122_3344L).bytes());
        assertEquals(BattleSpellCatalog.BASIC_HEAL, request.skillId());
        assertEquals(0x1122_3344L, request.targetObjectId());

        Protocol.Reader effect = new Protocol.Reader(
                Protocol.effectObject(request.targetObjectId(), 11722));
        assertEquals(Protocol.S_EFFECT, effect.c());
        assertEquals(0x1122_3344L, effect.d());
        assertEquals(11722, effect.h());
        assertEquals(0, effect.remaining());

        assertThrows(IllegalArgumentException.class,
                () -> Protocol.objectSpellRequest(new Protocol.Writer()
                        .c(Protocol.C_USE_SPELL).c(0).c(0).d(0).bytes()));
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.effectObject(0, 11722));
    }

    @Test
    void basicHealCatalogMatchesClassicSkillAndIntelligenceResources() {
        BattleSpellCatalog.HealingSpell heal =
                BattleSpellCatalog.healingSpell(BattleSpellCatalog.BASIC_HEAL);
        assertEquals(19, heal.actionId());
        assertEquals(11722, heal.effectGfx());
        assertEquals(4, heal.mpCost());
        assertEquals(10, heal.reuseDelayMillis());
        assertEquals(-1, heal.maximumRange());
        assertEquals(1, heal.diceCount(8));
        assertEquals(2, heal.diceCount(11));
        assertEquals(5, heal.diceCount(18));
        assertEquals(62, heal.diceCount(127));
        assertEquals(15, heal.applyLawfulRevision(10, 16384));
        assertThrows(IllegalArgumentException.class,
                () -> BattleSpellCatalog.healDiceBonus(128));
    }

    @Test
    void extraHealUsesExactNativeSelectorResourceFormulaAndManaOrdering() {
        Protocol.ObjectSpellRequest request = Protocol.objectSpellRequest(
                new Protocol.Writer().c(Protocol.C_USE_SPELL)
                        .c(2).c(2).d(0x5566_7788L).bytes());
        assertEquals(BattleSpellCatalog.EXTRA_HEAL, request.skillId());
        assertEquals(0x5566_7788L, request.targetObjectId());

        BattleSpellCatalog.HealingSpell heal =
                BattleSpellCatalog.healingSpell(BattleSpellCatalog.EXTRA_HEAL);
        assertEquals(2, heal.row());
        assertEquals(2, heal.column());
        assertEquals(19, heal.actionId());
        assertEquals(11723, heal.effectGfx());
        assertEquals(15, heal.mpCost());
        assertEquals(100, heal.reuseDelayMillis());
        assertEquals(-1, heal.maximumRange());
        assertEquals(2, heal.diceCount(8));
        assertEquals(4, heal.diceCount(14));
        assertEquals(8, heal.dieSides());
        assertEquals(24, heal.applyLawfulRevision(16, 16384));

        assertEquals(14, heal.effectiveMpCost(13, 0, false));
        assertEquals(13, heal.effectiveMpCost(14, 0, false));
        assertEquals(6, heal.effectiveMpCost(14, 0, true));
        assertEquals(4, heal.effectiveMpCost(14, 2, true));
        assertEquals(2, BattleSpellCatalog.originalMagicConsumeReduction(0, 13));
        assertEquals(0, BattleSpellCatalog.originalMagicConsumeReduction(3, 18));

        BattleSpellCatalog.HealingSpell basic =
                BattleSpellCatalog.healingSpell(BattleSpellCatalog.BASIC_HEAL);
        assertEquals(0, basic.effectiveMpCost(13, 2, true));
    }

    @Test
    void greaterHealMatchesNativeCase34ResourceAndUndeadRules() {
        Protocol.ObjectSpellRequest request = Protocol.objectSpellRequest(
                new Protocol.Writer().c(Protocol.C_USE_SPELL)
                        .c(4).c(2).d(0x6C00_0000L).bytes());
        assertEquals(BattleSpellCatalog.GREATER_HEAL, request.skillId());
        assertEquals(0x6C00_0000L, request.targetObjectId());

        BattleSpellCatalog.HealingSpell heal =
                BattleSpellCatalog.healingSpell(BattleSpellCatalog.GREATER_HEAL);
        assertEquals(4, heal.row());
        assertEquals(2, heal.column());
        assertEquals(19, heal.actionId());
        assertEquals(11724, heal.effectGfx());
        assertEquals(20, heal.mpCost());
        assertEquals(200, heal.reuseDelayMillis());
        assertEquals(-1, heal.maximumRange());
        assertEquals(BattleSpellCatalog.ATTRIBUTE_WATER, heal.attribute());
        assertEquals(77, heal.happenMessage());
        assertEquals(280, heal.failureMessage());
        assertEquals(7, heal.diceCount(8));
        assertEquals(9, heal.diceCount(14));
        assertEquals(8, heal.dieSides());
        assertEquals(60, heal.applyLawfulRevision(40, 16384));

        assertEquals(20, heal.effectiveMpCost(12, 0, false));
        assertEquals(19, heal.effectiveMpCost(13, 0, false));
        assertEquals(18, heal.effectiveMpCost(14, 0, false));
        assertEquals(17, heal.effectiveMpCost(15, 0, false));
        assertEquals(14, heal.effectiveMpCost(16, 2, true));
        assertEquals(40, heal.npcHpDelta(0, 40));
        assertEquals(-40, heal.npcHpDelta(1, 40));
        assertEquals(40, heal.npcHpDelta(2, 40));
        assertEquals(0, heal.npcHpDelta(3, 40));
        assertThrows(IllegalArgumentException.class,
                () -> heal.npcHpDelta(4, 40));
    }

    @Test
    void fullHealMatchesNativeCase56ObjectResourceFormulaAndManaOrdering() {
        Protocol.ObjectSpellRequest request = Protocol.objectSpellRequest(
                new Protocol.Writer().c(Protocol.C_USE_SPELL)
                        .c(7).c(0).d(0x7A00_0001L).bytes());
        assertEquals(BattleSpellCatalog.FULL_HEAL, request.skillId());
        assertEquals(0x7A00_0001L, request.targetObjectId());
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.objectSpellRequest(new Protocol.Writer()
                        .c(Protocol.C_USE_SPELL).c(7).c(0).d(0x7A00_0001L)
                        .h(32801).h(32885).bytes()));

        BattleSpellCatalog.HealingSpell heal =
                BattleSpellCatalog.healingSpell(BattleSpellCatalog.FULL_HEAL);
        assertEquals(7, heal.row());
        assertEquals(0, heal.column());
        assertEquals(19, heal.actionId());
        assertEquals(11725, heal.effectGfx());
        assertEquals(48, heal.mpCost());
        assertEquals(500, heal.reuseDelayMillis());
        assertEquals(-1, heal.maximumRange());
        assertEquals(14, heal.diceCount(8));
        assertEquals(16, heal.diceCount(14));
        assertEquals(17, heal.dieSides());
        assertEquals(BattleSpellCatalog.ATTRIBUTE_WATER | BattleSpellCatalog.ATTRIBUTE_WIND,
                heal.attribute());
        assertEquals(77, heal.happenMessage());
        assertEquals(280, heal.failureMessage());
        assertEquals(127, heal.applyLawfulRevision(85, 16384));

        assertEquals(48, heal.effectiveMpCost(12, 0, false));
        assertEquals(47, heal.effectiveMpCost(13, 0, false));
        assertEquals(42, heal.effectiveMpCost(18, 0, false));
        assertEquals(39, heal.effectiveMpCost(19, 2, true));
        assertEquals(85, heal.npcHpDelta(0, 85));
        assertEquals(-85, heal.npcHpDelta(1, 85));
        assertEquals(85, heal.npcHpDelta(2, 85));
        assertEquals(0, heal.npcHpDelta(3, 85));
    }

    @Test
    void healAllUsesExactSelectorOnlyClanPetScreenAndResourceMetadata() {
        Protocol.SpellSelector request = Protocol.selectorOnlySpellRequest(
                new Protocol.Writer().c(Protocol.C_USE_SPELL).c(6).c(0).bytes());
        assertEquals(BattleSpellCatalog.HEAL_ALL, request.skillId());
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.selectorOnlySpellRequest(new Protocol.Writer()
                        .c(Protocol.C_USE_SPELL).c(6).c(0).c(0).bytes()));

        BattleSpellCatalog.GroupHealingSpell heal =
                BattleSpellCatalog.groupHealingSpell(BattleSpellCatalog.HEAL_ALL);
        assertEquals(6, heal.row());
        assertEquals(0, heal.column());
        assertEquals(19, heal.actionId());
        assertEquals(759, heal.effectGfx());
        assertEquals(50, heal.mpCost());
        assertEquals(30, heal.hpCost());
        assertEquals(500, heal.reuseDelayMillis());
        assertEquals(-1, heal.area());
        assertEquals(7, heal.diceCount(8));
        assertEquals(9, heal.diceCount(14));
        assertEquals(8, heal.dieSides());
        assertEquals(56, heal.rollHealing(8, sides -> sides));
        assertEquals(84, heal.applyLawfulRevision(56, 16384));
        assertEquals(50, heal.effectiveMpCost(12, 0));
        assertEquals(49, heal.effectiveMpCost(13, 0));
        assertEquals(44, heal.effectiveMpCost(18, 0));
        assertEquals(42, heal.effectiveMpCost(18, 2));
        assertThrows(IllegalArgumentException.class,
                () -> heal.rollHealing(8, sides -> 0));

        ArenaMapCatalog.Arena passableArena = ArenaMapCatalog.forMap(554);
        assertTrue(BattleSpellCatalog.hasNativeLineOfSight(
                passableArena, 32801, 32885, 32800, 32885));
        ArenaMapCatalog.Arena blockedArena = ArenaMapCatalog.forMap(573);
        assertFalse(BattleSpellCatalog.hasNativeLineOfSight(
                blockedArena, 32704, 32768, 32705, 32768));
    }

    @Test
    void naturesBlessingUsesExactSelectorPartyPetWaterAndHealingMetadata() {
        Protocol.SpellSelector request = Protocol.selectorOnlySpellRequest(
                new Protocol.Writer().c(Protocol.C_USE_SPELL).c(20).c(3).bytes());
        assertEquals(BattleSpellCatalog.NATURES_BLESSING, request.skillId());
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.selectorOnlySpellRequest(new Protocol.Writer()
                        .c(Protocol.C_USE_SPELL).c(20).c(3).d(1).bytes()));

        BattleSpellCatalog.NaturesBlessingSpell heal =
                BattleSpellCatalog.naturesBlessingSpell(
                        BattleSpellCatalog.NATURES_BLESSING);
        assertEquals(20, heal.row());
        assertEquals(3, heal.column());
        assertEquals(21, heal.level());
        assertEquals(3, heal.number());
        assertEquals(19, heal.actionId());
        assertEquals(2244, heal.effectGfx());
        assertEquals(30, heal.mpCost());
        assertEquals(300, heal.reuseDelayMillis());
        assertEquals(8 | 16, heal.targetTo());
        assertEquals(-1, heal.area());
        assertEquals(10, heal.baseDiceCount());
        assertEquals(12, heal.dieSides());
        assertEquals(BattleSpellCatalog.ATTRIBUTE_WATER, heal.attribute());
        assertEquals(16, heal.type());
        assertEquals(8, heal.nativeId());
        assertTrue(heal.counterMagicBypass());
        assertEquals("natures-blessing", heal.key());
        assertEquals(9, heal.diceCount(8));
        assertEquals(11, heal.diceCount(14));
        assertEquals(108, heal.rollHealing(8, sides -> sides));
        assertEquals(162, heal.applyLawfulRevision(108, 16384));
        assertEquals(30, heal.effectiveMpCost(0));
        assertEquals(28, heal.effectiveMpCost(2));
        assertThrows(IllegalArgumentException.class,
                () -> heal.rollHealing(8, sides -> 0));
    }

    @Test
    void callOfNatureUsesExactNativeCase164ObjectOnlyCccd() {
        byte[] payload = new Protocol.Writer().c(Protocol.C_USE_SPELL)
                .c(20).c(4).d(0x3810_1650L).bytes();
        Protocol.ObjectSpellRequest request = Protocol.objectSpellRequest(payload);
        assertEquals(BattleSpellCatalog.CALL_OF_NATURE, request.skillId());
        assertEquals(0x3810_1650L, request.targetObjectId());
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.objectSpellRequest(new Protocol.Writer()
                        .c(Protocol.C_USE_SPELL).c(20).c(4).bytes()));
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.objectSpellRequest(new Protocol.Writer()
                        .c(Protocol.C_USE_SPELL).c(20).c(4)
                        .d(0x3810_1650L).h(32768).bytes()));
    }

    @Test
    void shieldUsesSelectorOnlyRequestAndExactNativeMageShieldResponse() {
        Protocol.SpellSelector request = Protocol.selectorOnlySpellRequest(
                new Protocol.Writer().c(Protocol.C_USE_SPELL).c(0).c(2).bytes());
        assertEquals(BattleSpellCatalog.SHIELD, request.skillId());
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.selectorOnlySpellRequest(new Protocol.Writer()
                        .c(Protocol.C_USE_SPELL).c(0).c(2).c(0).bytes()));

        Protocol.Reader packet = new Protocol.Reader(Protocol.mageShield(1_800, 5));
        assertEquals(Protocol.S_MAGE_SHIELD, packet.c());
        assertEquals(1_800, packet.h());
        assertEquals(5, packet.c());
        assertEquals(0, packet.remaining());

        BattleSpellCatalog.ShieldSpell spell =
                BattleSpellCatalog.shieldSpell(BattleSpellCatalog.SHIELD);
        assertEquals(19, spell.actionId());
        assertEquals(221, spell.effectGfx());
        assertEquals(8, spell.mpCost());
        assertEquals(0, spell.hpCost());
        assertEquals(8, spell.effectiveMpCost(2));
        assertEquals(1_800, spell.durationSeconds());
        assertEquals(-2, spell.armorClassModifier());
        assertEquals(5, spell.iconType());
    }

    @Test
    void shadowArmorUsesExactNativeCase98SelectorAndRetainedMetadata() {
        byte[] nativeRequest = new Protocol.Writer().c(Protocol.C_USE_SPELL)
                .c(12).c(2).bytes();
        assertArrayEquals(new byte[] {0x14, 0x0C, 0x02}, nativeRequest);
        Protocol.SpellSelector request = Protocol.selectorOnlySpellRequest(nativeRequest);
        assertEquals(BattleSpellCatalog.SHADOW_ARMOR, request.skillId());

        BattleSpellCatalog.ShieldSpell spell =
                BattleSpellCatalog.shieldSpell(BattleSpellCatalog.SHADOW_ARMOR);
        assertEquals(12, spell.row());
        assertEquals(2, spell.column());
        assertEquals(19, spell.actionId());
        assertEquals(2943, spell.effectGfx());
        assertEquals(10, spell.mpCost());
        assertEquals(5, spell.hpCost());
        assertEquals(0, spell.reuseDelayMillis());
        assertEquals(960, spell.durationSeconds());
        assertEquals(3, spell.iconType());
        assertEquals(-3, spell.armorClassModifier());
        assertEquals(10, spell.effectiveMpCost(0));
        assertEquals(9, spell.effectiveMpCost(1));
        assertEquals(8, spell.effectiveMpCost(2));
        assertEquals("shadow-armor", spell.key());
        assertNull(BattleSpellCatalog.shieldSpell(100));
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.selectorOnlySpellRequest(new Protocol.Writer()
                        .c(Protocol.C_USE_SPELL).c(12).c(2).d(15).bytes()));
    }

    @Test
    void earthSkinUsesExactNativeCase150ObjectOnlyEnvelopeAndAcContract() {
        byte[] nativeRequest = new Protocol.Writer().c(Protocol.C_USE_SPELL)
                .c(18).c(6).d(0x3810_1510L).bytes();
        Protocol.ObjectSpellRequest request =
                Protocol.objectSpellRequest(nativeRequest);
        assertEquals(BattleSpellCatalog.EARTH_SKIN, request.skillId());
        assertEquals(0x3810_1510L, request.targetObjectId());

        BattleSpellCatalog.ShieldSpell spell =
                BattleSpellCatalog.shieldSpell(BattleSpellCatalog.EARTH_SKIN);
        assertEquals(18, spell.row());
        assertEquals(6, spell.column());
        assertEquals(19, spell.actionId());
        assertEquals(2249, spell.effectGfx());
        assertEquals(15, spell.mpCost());
        assertEquals(0, spell.hpCost());
        assertEquals(0, spell.reuseDelayMillis());
        assertEquals(960, spell.durationSeconds());
        assertEquals(6, spell.iconType());
        assertEquals(-6, spell.armorClassModifier());
        assertEquals(BattleSpellCatalog.Targeting.PLAYER_OBJECT,
                spell.targeting());
        assertEquals(0, spell.happenMessage());
        assertEquals(725, spell.stopMessage());
        assertEquals(280, spell.failureMessage());
        assertEquals(15, spell.effectiveMpCost(2));
        assertEquals("earth-skin", spell.key());
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.objectSpellRequest(new Protocol.Writer()
                        .c(Protocol.C_USE_SPELL).c(18).c(6)
                        .d(0x3810_1510L).c(0).bytes()));
    }

    @Test void ironSkinUsesExactNativeCase167ObjectOnlyEnvelope() {
        byte[] nativeRequest = new Protocol.Writer().c(Protocol.C_USE_SPELL)
                .c(20).c(7).d(0x3810_1680L).bytes();
        Protocol.ObjectSpellRequest request =
                Protocol.objectSpellRequest(nativeRequest);
        assertEquals(BattleSpellCatalog.IRON_SKIN, request.skillId());
        assertEquals(0x3810_1680L, request.targetObjectId());
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.objectSpellRequest(new Protocol.Writer()
                        .c(Protocol.C_USE_SPELL).c(20).c(7)
                        .d(0x3810_1680L).c(0).bytes()));
    }

    @Test void exoticVitalizeUsesExactNativeCase168SelectorOnlyEnvelope() {
        byte[] nativeRequest = new Protocol.Writer().c(Protocol.C_USE_SPELL)
                .c(21).c(0).bytes();
        Protocol.SpellSelector request =
                Protocol.selectorOnlySpellRequest(nativeRequest);
        assertEquals(BattleSpellCatalog.EXOTIC_VITALIZE, request.skillId());
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.selectorOnlySpellRequest(new Protocol.Writer()
                        .c(Protocol.C_USE_SPELL).c(21).c(0).c(0).bytes()));
    }

    @Test void waterLifeUsesExactNativeCase169ObjectOnlyEnvelope() {
        byte[] nativeRequest = new Protocol.Writer().c(Protocol.C_USE_SPELL)
                .c(21).c(1).d(0x3810_1700L).bytes();
        Protocol.ObjectSpellRequest request =
                Protocol.objectSpellRequest(nativeRequest);
        assertArrayEquals(new byte[] {
                0x14, 0x15, 0x01, 0x00, 0x17, 0x10, 0x38
        }, nativeRequest);
        assertEquals(BattleSpellCatalog.WATER_LIFE, request.skillId());
        assertEquals(0x3810_1700L, request.targetObjectId());
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.objectSpellRequest(new Protocol.Writer()
                        .c(Protocol.C_USE_SPELL).c(21).c(1)
                        .d(0x3810_1700L).c(0).bytes()));
    }

    @Test void elementalFireUsesExactNativeCase170SelectorOnlyEnvelope() {
        byte[] nativeRequest = new Protocol.Writer().c(Protocol.C_USE_SPELL)
                .c(21).c(2).bytes();
        assertArrayEquals(new byte[] {0x14, 0x15, 0x02}, nativeRequest);
        Protocol.SpellSelector request =
                Protocol.selectorOnlySpellRequest(nativeRequest);
        assertEquals(BattleSpellCatalog.ELEMENTAL_FIRE, request.skillId());
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.selectorOnlySpellRequest(new Protocol.Writer()
                        .c(Protocol.C_USE_SPELL).c(21).c(2).d(15).bytes()));

        BattleSpellCatalog.PhysicalProcBuffSpell spell =
                BattleSpellCatalog.physicalProcBuffSpell(
                        BattleSpellCatalog.ELEMENTAL_FIRE);
        assertEquals(21, spell.row());
        assertEquals(2, spell.column());
        assertEquals(19, spell.actionId());
        assertEquals(4402, spell.effectGfx());
        assertEquals(20, spell.mpCost());
        assertEquals(100, spell.reuseDelayMillis());
        assertEquals(192, spell.durationSeconds());
        assertEquals(BattleSpellCatalog.ATTRIBUTE_FIRE, spell.attribute());
        assertEquals(2, spell.sourceType());
        assertEquals(33, spell.procChance());
        assertEquals(1.5D, spell.damageMultiplier());
        assertEquals(4, spell.nativeId());
        assertEquals(InventoryCatalog.SPIRIT_CRYSTAL, spell.itemConsumeId());
        assertEquals(1, spell.itemConsumeCount());
        assertEquals(20, spell.effectiveMpCost(18, 0));
        assertEquals(18, spell.effectiveMpCost(18, 2));
    }

    @Test void stormWalkUsesExactNativeCase171SelectorOnlyNoEffectEnvelope() {
        byte[] nativeRequest = new Protocol.Writer().c(Protocol.C_USE_SPELL)
                .c(21).c(3).bytes();
        assertArrayEquals(new byte[] {0x14, 0x15, 0x03}, nativeRequest);
        Protocol.SpellSelector request =
                Protocol.selectorOnlySpellRequest(nativeRequest);
        assertEquals(BattleSpellCatalog.STORM_WALK, request.skillId());
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.selectorOnlySpellRequest(new Protocol.Writer()
                        .c(Protocol.C_USE_SPELL).c(21).c(3).d(15).bytes()));

        BattleSpellCatalog.OtherNoEffectSpell spell =
                BattleSpellCatalog.otherNoEffectSpell(
                        BattleSpellCatalog.STORM_WALK);
        assertEquals(21, spell.row());
        assertEquals(3, spell.column());
        assertEquals(19, spell.actionId());
        assertEquals(0, spell.effectGfx());
        assertEquals(10, spell.mpCost());
        assertEquals(0, spell.reuseDelayMillis());
        assertEquals(0, spell.durationSeconds());
        assertEquals(BattleSpellCatalog.ATTRIBUTE_WIND, spell.attribute());
        assertEquals(128, spell.sourceType());
        assertEquals(8, spell.nativeId());
        assertEquals(0, spell.itemConsumeId());
        assertEquals(0, spell.itemConsumeCount());
        assertEquals(10, spell.effectiveMpCost(18, 0));
        assertEquals(8, spell.effectiveMpCost(18, 2));
    }

    @Test
    void physicalEnchantBuffsUseExactObjectTargetAndMageStatResponses() {
        Protocol.ObjectSpellRequest dexterityRequest = Protocol.objectSpellRequest(
                new Protocol.Writer().c(Protocol.C_USE_SPELL)
                        .c(3).c(1).d(3).bytes());
        assertEquals(BattleSpellCatalog.PHYSICAL_ENCHANT_DEX,
                dexterityRequest.skillId());
        assertEquals(3, dexterityRequest.targetObjectId());

        Protocol.Reader dexterity = new Protocol.Reader(
                Protocol.mageDexterity(1_200, 15, 5));
        assertEquals(Protocol.S_MAGE_DEXTERITY, dexterity.c());
        assertEquals(1_200, dexterity.h());
        assertEquals(15, dexterity.c());
        assertEquals(5, dexterity.c());
        assertEquals(0, dexterity.remaining());

        Protocol.Reader strength = new Protocol.Reader(
                Protocol.mageStrength(1_200, 18, 0, 5));
        assertEquals(Protocol.S_MAGE_STRENGTH, strength.c());
        assertEquals(1_200, strength.h());
        assertEquals(18, strength.c());
        assertEquals(0, strength.c());
        assertEquals(5, strength.c());
        assertEquals(0, strength.remaining());

        BattleSpellCatalog.AbilityBuffSpell dexteritySpell =
                BattleSpellCatalog.abilityBuffSpell(
                        BattleSpellCatalog.PHYSICAL_ENCHANT_DEX);
        BattleSpellCatalog.AbilityBuffSpell strengthSpell =
                BattleSpellCatalog.abilityBuffSpell(
                        BattleSpellCatalog.PHYSICAL_ENCHANT_STR);
        assertEquals(750, dexteritySpell.effectGfx());
        assertEquals(6342, strengthSpell.effectGfx());
        assertEquals(50, dexteritySpell.mpCost());
        assertEquals(1_200, strengthSpell.durationSeconds());
        assertEquals(5, dexteritySpell.modifier());
        assertEquals(5, strengthSpell.activeType());
        assertEquals(1, strengthSpell.stopType());
        assertEquals(50, dexteritySpell.effectiveMpCost(12, 0, false));
        assertEquals(47, dexteritySpell.effectiveMpCost(15, 0, false));
        assertEquals(21, dexteritySpell.effectiveMpCost(15, 2, true));
        assertEquals(45, strengthSpell.effectiveMpCost(17, 0, false));
        assertEquals(20, strengthSpell.effectiveMpCost(17, 2, true));
        assertEquals(8, BattleSpellCatalog.abilityBuffSpell(
                BattleSpellCatalog.DRESS_DEXTERITY)
                .effectiveMpCost(19, 2, true));
        assertThrows(IllegalArgumentException.class,
                () -> dexteritySpell.effectiveMpCost(0, 0, false));
        assertThrows(IllegalArgumentException.class,
                () -> dexteritySpell.effectiveMpCost(15, 3, false));
    }

    @Test
    void dressMightyUsesExactNativeCase108SelectorAndStrengthTypeTwo() {
        Protocol.SpellSelector request = Protocol.selectorOnlySpellRequest(
                new Protocol.Writer().c(Protocol.C_USE_SPELL).c(13).c(4).bytes());
        assertEquals(BattleSpellCatalog.DRESS_MIGHTY, request.skillId());
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.selectorOnlySpellRequest(new Protocol.Writer()
                        .c(Protocol.C_USE_SPELL).c(13).c(4).d(15).bytes()));

        BattleSpellCatalog.AbilityBuffSpell spell =
                BattleSpellCatalog.abilityBuffSpell(BattleSpellCatalog.DRESS_MIGHTY);
        assertEquals(13, spell.row());
        assertEquals(4, spell.column());
        assertEquals(19, spell.actionId());
        assertEquals(3909, spell.effectGfx());
        assertEquals(10, spell.mpCost());
        assertEquals(0, spell.reuseDelayMillis());
        assertEquals(960, spell.durationSeconds());
        assertEquals(BattleSpellCatalog.Ability.STRENGTH, spell.ability());
        assertEquals(2, spell.modifier());
        assertEquals(2, spell.activeType());
        assertEquals(2, spell.stopType());
        assertEquals(292, spell.happenMessage());
        assertEquals(293, spell.stopMessage());
        assertEquals(BattleSpellCatalog.RequestShape.SELECTOR_ONLY,
                spell.requestShape());

        Protocol.Reader active = new Protocol.Reader(
                Protocol.mageStrength(960, 15, 0, spell.activeType()));
        assertEquals(Protocol.S_MAGE_STRENGTH, active.c());
        assertEquals(960, active.h());
        assertEquals(15, active.c());
        assertEquals(0, active.c());
        assertEquals(2, active.c());
        assertEquals(0, active.remaining());
    }

    @Test
    void dressDexterityUsesExactNativeCase109SelectorAndDexterityTypeTwo() {
        Protocol.SpellSelector request = Protocol.selectorOnlySpellRequest(
                new Protocol.Writer().c(Protocol.C_USE_SPELL).c(13).c(5).bytes());
        assertEquals(BattleSpellCatalog.DRESS_DEXTERITY, request.skillId());
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.selectorOnlySpellRequest(new Protocol.Writer()
                        .c(Protocol.C_USE_SPELL).c(13).c(5).d(15).bytes()));

        BattleSpellCatalog.AbilityBuffSpell spell =
                BattleSpellCatalog.abilityBuffSpell(
                        BattleSpellCatalog.DRESS_DEXTERITY);
        assertEquals(13, spell.row());
        assertEquals(5, spell.column());
        assertEquals(19, spell.actionId());
        assertEquals(3910, spell.effectGfx());
        assertEquals(10, spell.mpCost());
        assertEquals(0, spell.reuseDelayMillis());
        assertEquals(960, spell.durationSeconds());
        assertEquals(BattleSpellCatalog.Ability.DEXTERITY, spell.ability());
        assertEquals(2, spell.modifier());
        assertEquals(2, spell.activeType());
        assertEquals(2, spell.stopType());
        assertEquals(294, spell.happenMessage());
        assertEquals(295, spell.stopMessage());
        assertEquals(BattleSpellCatalog.RequestShape.SELECTOR_ONLY,
                spell.requestShape());

        Protocol.Reader active = new Protocol.Reader(
                Protocol.mageDexterity(960, 17, spell.activeType()));
        assertEquals(Protocol.S_MAGE_DEXTERITY, active.c());
        assertEquals(960, active.h());
        assertEquals(17, active.c());
        assertEquals(2, active.c());
        assertEquals(0, active.remaining());
    }

    @Test
    void dressEvasionUsesExactNativeCase110SelectorAndErMetadata() {
        Protocol.SpellSelector request = Protocol.selectorOnlySpellRequest(
                new Protocol.Writer().c(Protocol.C_USE_SPELL).c(13).c(6).bytes());
        assertEquals(BattleSpellCatalog.DRESS_EVASION, request.skillId());
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.selectorOnlySpellRequest(new Protocol.Writer()
                        .c(Protocol.C_USE_SPELL).c(13).c(6).d(15).bytes()));

        BattleSpellCatalog.RangedEvasionBuffSpell spell =
                BattleSpellCatalog.rangedEvasionBuffSpell(
                        BattleSpellCatalog.DRESS_EVASION);
        assertEquals(111, spell.skillId());
        assertEquals(13, spell.row());
        assertEquals(6, spell.column());
        assertEquals(19, spell.actionId());
        assertEquals(11766, spell.effectGfx());
        assertEquals(20, spell.mpCost());
        assertEquals(0, spell.reuseDelayMillis());
        assertEquals(32, spell.durationSeconds());
        assertEquals(0, spell.attribute());
        assertEquals(2, spell.sourceType());
        assertEquals(64, spell.nativeId());
        assertEquals(12, spell.evasionRateModifier());
        assertEquals(20, spell.effectiveMpCost(8, 0));
        assertEquals(18, spell.effectiveMpCost(18, 2));
        assertEquals("dress-evasion", spell.key());
        assertNull(BattleSpellCatalog.rangedEvasionBuffSpell(110));
    }

    @Test
    void hasteUsesExactNativeCase42ObjectOnlyEnvelopeAndSpeedResponse() {
        Protocol.ObjectSpellRequest request = Protocol.objectSpellRequest(
                new Protocol.Writer().c(Protocol.C_USE_SPELL)
                        .c(5).c(2).d(0x6C00_0043L).bytes());
        assertEquals(BattleSpellCatalog.HASTE, request.skillId());
        assertEquals(0x6C00_0043L, request.targetObjectId());
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.objectSpellRequest(new Protocol.Writer()
                        .c(Protocol.C_USE_SPELL).c(5).c(2)
                        .d(0x6C00_0043L).h(32700).h(32700).bytes()));

        Protocol.Reader speed = new Protocol.Reader(Protocol.speed(
                0x6C00_0043L, Protocol.SpeedMode.HASTE, 1_200));
        assertEquals(Protocol.S_SPEED, speed.c());
        assertEquals(0x6C00_0043L, speed.d());
        assertEquals(BattleTimedStatus.HASTE, speed.c());
        assertEquals(1_200, speed.h());
        assertEquals(0, speed.remaining());
    }

    @Test
    void cancellationUsesExactNativeCase43ObjectOnlyEnvelopeAndStopResponses() {
        Protocol.ObjectSpellRequest request = Protocol.objectSpellRequest(
                new Protocol.Writer().c(Protocol.C_USE_SPELL)
                        .c(5).c(3).d(0x6C00_0044L).bytes());
        assertEquals(BattleSpellCatalog.CANCELLATION, request.skillId());
        assertEquals(0x6C00_0044L, request.targetObjectId());
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.objectSpellRequest(new Protocol.Writer()
                        .c(Protocol.C_USE_SPELL).c(5).c(3)
                        .d(0x6C00_0044L).h(32700).h(32700).bytes()));

        Protocol.Reader speed = new Protocol.Reader(Protocol.speed(
                0x6C00_0044L, Protocol.SpeedMode.NORMAL, 0));
        assertEquals(Protocol.S_SPEED, speed.c());
        assertEquals(0x6C00_0044L, speed.d());
        assertEquals(BattleTimedStatus.NORMAL, speed.c());
        assertEquals(0, speed.h());
        assertEquals(0, speed.remaining());

        Protocol.Reader brave = new Protocol.Reader(Protocol.braveSpeed(
                0x6C00_0044L, Protocol.BraveMode.NORMAL, 0));
        assertEquals(Protocol.S_EMOTION, brave.c());
        assertEquals(0x6C00_0044L, brave.d());
        assertEquals(BattleTimedStatus.BRAVE_NORMAL, brave.c());
        assertEquals(0, brave.h());
        assertEquals(0, brave.remaining());
    }

    @Test
    void nativeCharacterStatusMatchesRecoveredWeightLawfulAndResistanceTail() {
        Protocol.Reader status = new Protocol.Reader(Protocol.characterStatus(
                0x1234, 52, 1234, 18, 11, 10, 9, 8, 11,
                90, 7, 100, 10, -7, 40, 197,
                -123, 10, -5, 20, -30));
        assertEquals(Protocol.S_STATUS, status.c());
        assertEquals(0x1234, status.d());
        assertEquals(52, status.c());
        assertEquals(1234, status.d());
        assertEquals(18, status.c()); // STR
        assertEquals(8, status.c());  // INT
        assertEquals(11, status.c()); // WIS
        assertEquals(10, status.c()); // DEX
        assertEquals(11, status.c()); // CON
        assertEquals(9, status.c());  // CHA
        assertEquals(90, status.h());
        assertEquals(100, status.h());
        assertEquals(7, status.h());
        assertEquals(10, status.h());
        assertEquals(249, status.c());
        assertEquals(0, status.d());
        assertEquals(Protocol.nativeFoodGauge(40), status.c());
        assertEquals(Protocol.nativeWeightGauge(197), status.c());
        assertEquals(0xFF85, status.h());
        assertEquals(10, status.c());
        assertEquals(251, status.c());
        assertEquals(20, status.c());
        assertEquals(226, status.c());
        assertEquals(0, status.remaining());
    }

    @Test
    void cursePoisonCatalogMatchesClassicSkillAndDamagePoisonResources() {
        BattleSpellCatalog.PoisonSpell poison =
                BattleSpellCatalog.poisonSpell(BattleSpellCatalog.CURSE_POISON);
        assertEquals(11, poison.skillId());
        assertEquals(1, poison.row());
        assertEquals(2, poison.column());
        assertEquals(19, poison.actionId());
        assertEquals(745, poison.effectGfx());
        assertEquals(10, poison.mpCost());
        assertEquals(500, poison.reuseDelayMillis());
        assertEquals(1, poison.maximumRange());
        assertEquals(30, poison.durationSeconds());
        assertEquals(3_000, poison.tickIntervalMillis());
        assertEquals(5, poison.tickDamage());
        assertEquals(12, poison.probabilityDice());
        assertEquals(0, poison.bossModifier());
        assertEquals(-10, poison.lawfulDelta());
        assertEquals(280, poison.failureMessage());
        assertEquals(40318, poison.itemConsumeId());
        assertEquals(1, poison.itemConsumeCount());
        assertEquals(90, poison.probability(
                1, 52, 18, 2, 3, 20, false, sides -> sides));
        assertEquals(1, poison.probability(
                3, 1, 1, 0, 0, 100, false, sides -> 1));
        assertEquals(0, poison.probability(
                1, 52, 18, 2, 3, 20, true, sides -> sides));
        assertEquals(10, poison.effectiveMpCost(12, 0));
        assertEquals(9, poison.effectiveMpCost(13, 0));
        assertEquals(7, poison.effectiveMpCost(13, 2));
    }

    @Test
    void curseParalysisCatalogMatchesSelectedSkillAndNegativeControlRows() {
        BattleSpellCatalog.ParalysisSpell paralysis =
                BattleSpellCatalog.paralysisSpell(BattleSpellCatalog.CURSE_PARALYZE);
        assertEquals(33, paralysis.skillId());
        assertEquals(4, paralysis.row());
        assertEquals(0, paralysis.column());
        assertEquals(19, paralysis.actionId());
        assertEquals(746, paralysis.effectGfx());
        assertEquals(150, paralysis.mpCost());
        assertEquals(5_000, paralysis.reuseDelayMillis());
        assertEquals(5_000, paralysis.clientCastClockMillis());
        assertEquals(2, paralysis.maximumRange());
        assertEquals(5_000, paralysis.warningDelayMillis());
        assertEquals(10_000, paralysis.paralysisDurationMillis());
        assertEquals(-500, paralysis.lawfulDelta());
        assertEquals(280, paralysis.failureMessage());
        assertEquals(50, paralysis.probabilityHigh());
        assertEquals(40, paralysis.probabilityEqual());
        assertEquals(30, paralysis.probabilityLow());
        assertEquals(0.4D, paralysis.intelligenceMultiplier());
        assertEquals(0.05D, paralysis.magicResistanceMultiplier());
        assertFalse(paralysis.includeMagicLevel());
        assertTrue(paralysis.includeMagicHit());
        assertEquals(40318, paralysis.itemConsumeId());
        assertEquals(2, paralysis.itemConsumeCount());

        assertEquals(44, paralysis.probability(
                50, 50, 18, 7, 3, 20, 5));
        assertEquals(90, paralysis.probability(
                99, 1, 100, 0, 100, 0, -128));
        assertEquals(1, paralysis.probability(
                1, 99, 1, 0, 0, 100, 127));
        assertEquals(150, paralysis.effectiveMpCost(12, 0));
        assertEquals(144, paralysis.effectiveMpCost(16, 2));
    }

    @Test
    void callLightningCatalogMatchesNativeProducerAndSelectedResourceRow() {
        Protocol.AttackSpellRequest request = Protocol.attackSpellRequest(
                new Protocol.Writer().c(Protocol.C_USE_SPELL)
                        .c(4).c(1).d(0x6C000000L).h(32729).h(32824).bytes());
        assertEquals(BattleSpellCatalog.CALL_LIGHTNING, request.skillId());
        assertEquals(0x6C000000L, request.targetObjectId());
        assertEquals(32729, request.x());
        assertEquals(32824, request.y());

        BattleSpellCatalog.AttackSpell spell =
                BattleSpellCatalog.attackSpell(BattleSpellCatalog.CALL_LIGHTNING);
        assertEquals(34, spell.skillId());
        assertEquals(4, spell.row());
        assertEquals(1, spell.column());
        assertEquals(18, spell.actionId());
        assertEquals(11736, spell.effectGfx());
        assertEquals(18, spell.mpCost());
        assertEquals(10, spell.reuseDelayMillis());
        assertEquals(6, spell.maximumRange());
        assertEquals(32, spell.baseDamage());
        assertEquals(5, spell.diceCount());
        assertEquals(8, spell.dieSides());
        assertEquals(BattleSpellCatalog.ATTRIBUTE_WIND, spell.attribute());
        assertEquals(18, spell.effectiveMpCost(12, 0));
        assertEquals(12, spell.effectiveMpCost(16, 2));
    }

    @Test
    void invisibilityCatalogMatchesTheClassicSkillResource() {
        BattleSpellCatalog.InvisibilitySpell invisibility =
                BattleSpellCatalog.invisibilitySpell(BattleSpellCatalog.INVISIBILITY);
        assertEquals(60, invisibility.skillId());
        assertEquals(7, invisibility.row());
        assertEquals(3, invisibility.column());
        assertEquals(19, invisibility.actionId());
        assertEquals(15, invisibility.mpCost());
        assertEquals(0, invisibility.reuseDelayMillis());
        assertEquals(40318, invisibility.itemConsumeId());
        assertEquals(1, invisibility.itemConsumeCount());
        assertEquals(0, invisibility.effectGfx());
        assertEquals(0, invisibility.durationSeconds());
        assertEquals(BattleSpellCatalog.RequestShape.SELF_TICK,
                invisibility.requestShape());

        BattleSpellCatalog.InvisibilitySpell blindHiding =
                BattleSpellCatalog.invisibilitySpell(BattleSpellCatalog.BLIND_HIDING);
        assertEquals(97, blindHiding.skillId());
        assertEquals(12, blindHiding.row());
        assertEquals(0, blindHiding.column());
        assertEquals(19, blindHiding.actionId());
        assertEquals(2941, blindHiding.effectGfx());
        assertEquals(10, blindHiding.mpCost());
        assertEquals(9_990, blindHiding.reuseDelayMillis());
        assertEquals(32, blindHiding.durationSeconds());
        assertEquals(0, blindHiding.itemConsumeId());
        assertEquals(0, blindHiding.itemConsumeCount());
        assertEquals(BattleSpellCatalog.RequestShape.SELECTOR_ONLY,
                blindHiding.requestShape());
        assertEquals(10, blindHiding.effectiveMpCost(12, 0));
        assertEquals(9, blindHiding.effectiveMpCost(13, 1));
        assertEquals(8, blindHiding.effectiveMpCost(15, 2));
        assertNull(BattleSpellCatalog.invisibilitySpell(98));
    }

    @Test
    void enchantVenomCatalogMatchesTheClassicSkillAndMaterialRows() {
        BattleSpellCatalog.EnchantVenomSpell spell =
                BattleSpellCatalog.enchantVenomSpell(BattleSpellCatalog.ENCHANT_VENOM);
        assertEquals(98, spell.skillId());
        assertEquals(12, spell.row());
        assertEquals(1, spell.column());
        assertEquals(19, spell.actionId());
        assertEquals(2942, spell.effectGfx());
        assertEquals(10, spell.mpCost());
        assertEquals(1, spell.hpCost());
        assertEquals(0, spell.reuseDelayMillis());
        assertEquals(320, spell.durationSeconds());
        assertEquals(InventoryCatalog.SECOND_LEVEL_DARK_STONE,
                spell.itemConsumeId());
        assertEquals(1, spell.itemConsumeCount());
        assertEquals(10, spell.procPercent());
        assertEquals(30, spell.poisonDurationSeconds());
        assertEquals(3_000, spell.tickIntervalMillis());
        assertEquals(5, spell.tickDamage());
        assertEquals(10, spell.effectiveMpCost(0));
        assertEquals(8, spell.effectiveMpCost(2));

        InventoryCatalog.Definition stone = InventoryCatalog.require(
                InventoryCatalog.SECOND_LEVEL_DARK_STONE);
        assertEquals(0, stone.useType());
        assertEquals(1054, stone.icon());
        assertEquals(2898, stone.groundGfx());
        assertEquals(837, stone.itemDescriptionId());
        assertEquals(600, stone.unitWeight());
        assertNull(BattleSpellCatalog.enchantVenomSpell(97));
    }

    @Test
    void bringStoneUsesExactObjectTargetAndRetainedRefinementLadder() {
        long objectId = 0x7000_0100L;
        byte[] nativeRequest = new Protocol.Writer().c(Protocol.C_USE_SPELL)
                .c(12).c(3).d(objectId).bytes();
        assertArrayEquals(new byte[] {
                0x14, 0x0C, 0x03, 0x00, 0x01, 0x00, 0x70
        }, nativeRequest);
        Protocol.ObjectSpellRequest request = Protocol.objectSpellRequest(nativeRequest);
        assertEquals(BattleSpellCatalog.BRING_STONE, request.skillId());
        assertEquals(objectId, request.targetObjectId());

        BattleSpellCatalog.BringStoneSpell spell =
                BattleSpellCatalog.bringStoneSpell(BattleSpellCatalog.BRING_STONE);
        assertEquals(12, spell.row());
        assertEquals(3, spell.column());
        assertEquals(19, spell.actionId());
        assertEquals(2944, spell.effectGfx());
        assertEquals(5, spell.mpCost());
        assertEquals(0, spell.reuseDelayMillis());
        assertEquals(280, spell.failureMessage());
        assertEquals(403, spell.successMessage());
        assertEquals(5, spell.effectiveMpCost(0));
        assertEquals(3, spell.effectiveMpCost(2));
        assertEquals(54, spell.successThreshold(0, 52, 8));
        assertEquals(25, spell.successThreshold(1, 52, 8));
        assertEquals(12, spell.successThreshold(2, 52, 8));
        assertEquals(6, spell.successThreshold(3, 52, 8));
        assertTrue(spell.succeeds(7, 7));
        assertFalse(spell.succeeds(6, 7));

        long[] catalogs = {
                InventoryCatalog.FIRST_LEVEL_DARK_STONE,
                InventoryCatalog.SECOND_LEVEL_DARK_STONE,
                InventoryCatalog.THIRD_LEVEL_DARK_STONE,
                InventoryCatalog.FOURTH_LEVEL_DARK_STONE,
                InventoryCatalog.FIFTH_LEVEL_DARK_STONE
        };
        int[] icons = {1061, 1054, 1055, 1056, 1057};
        int[] groundGfx = {2897, 2898, 2899, 2900, 2901};
        int[] descriptions = {836, 837, 838, 839, 840};
        int[] weights = {300, 600, 900, 1_200, 1_500};
        for (int index = 0; index < catalogs.length; index++) {
            InventoryCatalog.Definition stone = InventoryCatalog.require(catalogs[index]);
            assertEquals(0, stone.useType());
            assertTrue(stone.stackable());
            assertEquals(icons[index], stone.icon());
            assertEquals(groundGfx[index], stone.groundGfx());
            assertEquals(descriptions[index], stone.itemDescriptionId());
            assertEquals(weights[index], stone.unitWeight());
            if (index < catalogs.length - 1) {
                BattleSpellCatalog.StoneRefinement refinement =
                        BattleSpellCatalog.stoneRefinement(catalogs[index]);
                assertEquals(index, refinement.tier());
                assertEquals(catalogs[index], refinement.sourceCatalogId());
                assertEquals(catalogs[index + 1], refinement.targetCatalogId());
                assertEquals("$" + (2475 + index), refinement.targetNameId());
            }
        }
        assertNull(BattleSpellCatalog.stoneRefinement(
                InventoryCatalog.FIFTH_LEVEL_DARK_STONE));
        assertNull(BattleSpellCatalog.bringStoneSpell(99));
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.objectSpellRequest(new Protocol.Writer()
                        .c(Protocol.C_USE_SPELL).c(12).c(3).bytes()));
    }

    @Test
    void movingAccelerationUsesExactSelectorOnlyModeFourContract() {
        byte[] nativeRequest = new Protocol.Writer().c(Protocol.C_USE_SPELL)
                .c(12).c(4).bytes();
        assertArrayEquals(new byte[] {0x14, 0x0C, 0x04}, nativeRequest);
        Protocol.SpellSelector request = Protocol.selectorOnlySpellRequest(nativeRequest);
        assertEquals(BattleSpellCatalog.MOVING_ACCELERATION, request.skillId());
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.selectorOnlySpellRequest(new Protocol.Writer()
                        .c(Protocol.C_USE_SPELL).c(12).c(4).d(15).bytes()));

        BattleSpellCatalog.MovementBuffSpell spell =
                BattleSpellCatalog.movementBuffSpell(
                        BattleSpellCatalog.MOVING_ACCELERATION);
        assertEquals(12, spell.row());
        assertEquals(4, spell.column());
        assertEquals(19, spell.actionId());
        assertEquals(2945, spell.effectGfx());
        assertEquals(10, spell.mpCost());
        assertEquals(0, spell.reuseDelayMillis());
        assertEquals(960, spell.durationSeconds());
        assertEquals(BattleSpellCatalog.ATTRIBUTE_UNSPECIFIED, spell.attribute());
        assertEquals(2, spell.sourceType());
        assertEquals(BattleTimedStatus.BRAVE_HOLY_WALK, spell.braveMode());
        assertEquals(0.665D, spell.movementMultiplier());
        assertEquals(665, spell.movementIntervalMillis(1_000));
        assertEquals(10, spell.effectiveMpCost(8, 0));
        assertEquals(8, spell.effectiveMpCost(8, 2));

        assertArrayEquals(new byte[] {
                        0x62, 0x0F, 0x00, 0x00, 0x00, 0x04, (byte) 0xC0, 0x03
                }, Protocol.braveSpeed(15,
                        Protocol.BraveMode.fromWire(spell.braveMode()),
                        spell.durationSeconds()));
    }

    @Test
    void windWalkUsesExactNativeCase149SelectorOnlyModeFourContract() {
        byte[] nativeRequest = new Protocol.Writer().c(Protocol.C_USE_SPELL)
                .c(18).c(5).bytes();
        assertArrayEquals(new byte[] {0x14, 0x12, 0x05}, nativeRequest);
        Protocol.SpellSelector request =
                Protocol.selectorOnlySpellRequest(nativeRequest);
        assertEquals(BattleSpellCatalog.WIND_WALK, request.skillId());
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.selectorOnlySpellRequest(new Protocol.Writer()
                        .c(Protocol.C_USE_SPELL).c(18).c(5).d(15).bytes()));

        BattleSpellCatalog.MovementBuffSpell spell =
                BattleSpellCatalog.movementBuffSpell(
                        BattleSpellCatalog.WIND_WALK);
        assertEquals(19, spell.actionId());
        assertEquals(2247, spell.effectGfx());
        assertEquals(15, spell.mpCost());
        assertEquals(960, spell.durationSeconds());
        assertEquals(BattleSpellCatalog.ATTRIBUTE_UNSPECIFIED, spell.attribute());
        assertEquals(BattleTimedStatus.BRAVE_HOLY_WALK, spell.braveMode());
        assertEquals(665, spell.movementIntervalMillis(1_000));
        assertArrayEquals(new byte[] {
                        0x62, 0x0F, 0x00, 0x00, 0x00, 0x04,
                        (byte) 0xC0, 0x03
                }, Protocol.braveSpeed(15,
                        Protocol.BraveMode.fromWire(spell.braveMode()),
                        spell.durationSeconds()));
    }

    @Test
    void burningSpiritUsesExactSelectorOnlyPhysicalProcContract() {
        byte[] nativeRequest = new Protocol.Writer().c(Protocol.C_USE_SPELL)
                .c(12).c(5).bytes();
        assertArrayEquals(new byte[] {0x14, 0x0C, 0x05}, nativeRequest);
        Protocol.SpellSelector request = Protocol.selectorOnlySpellRequest(nativeRequest);
        assertEquals(BattleSpellCatalog.BURNING_SPIRIT, request.skillId());
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.selectorOnlySpellRequest(new Protocol.Writer()
                        .c(Protocol.C_USE_SPELL).c(12).c(5).d(15).bytes()));

        BattleSpellCatalog.PhysicalProcBuffSpell spell =
                BattleSpellCatalog.physicalProcBuffSpell(
                        BattleSpellCatalog.BURNING_SPIRIT);
        assertEquals(12, spell.row());
        assertEquals(5, spell.column());
        assertEquals(19, spell.actionId());
        assertEquals(2946, spell.effectGfx());
        assertEquals(20, spell.mpCost());
        assertEquals(2_990, spell.reuseDelayMillis());
        assertEquals(320, spell.durationSeconds());
        assertEquals(BattleSpellCatalog.ATTRIBUTE_UNSPECIFIED, spell.attribute());
        assertEquals(2, spell.sourceType());
        assertEquals(33, spell.procChance());
        assertEquals(1.5D, spell.damageMultiplier());
        assertEquals(32, spell.nativeId());
        assertEquals(0, spell.happenMessage());
        assertEquals(0, spell.stopMessage());
        assertEquals(0, spell.itemConsumeId());
        assertEquals(0, spell.itemConsumeCount());
        assertEquals(20, spell.effectiveMpCost(8, 0));
        assertEquals(18, spell.effectiveMpCost(8, 2));
        assertNull(BattleSpellCatalog.physicalProcBuffSpell(101));
    }

    @Test
    void doubleBrakeUsesExactSelectorOnlyClawEdoryuContract() {
        byte[] nativeRequest = new Protocol.Writer().c(Protocol.C_USE_SPELL)
                .c(13).c(0).bytes();
        assertArrayEquals(new byte[] {0x14, 0x0D, 0x00}, nativeRequest);
        Protocol.SpellSelector request = Protocol.selectorOnlySpellRequest(nativeRequest);
        assertEquals(BattleSpellCatalog.DOUBLE_BRAKE, request.skillId());
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.selectorOnlySpellRequest(new Protocol.Writer()
                        .c(Protocol.C_USE_SPELL).c(13).c(0).d(15).bytes()));

        BattleSpellCatalog.PhysicalProcBuffSpell spell =
                BattleSpellCatalog.physicalProcBuffSpell(
                        BattleSpellCatalog.DOUBLE_BRAKE);
        assertEquals(105, spell.skillId());
        assertEquals(13, spell.row());
        assertEquals(0, spell.column());
        assertEquals(19, spell.actionId());
        assertEquals(2949, spell.effectGfx());
        assertEquals(30, spell.mpCost());
        assertEquals(0, spell.reuseDelayMillis());
        assertEquals(192, spell.durationSeconds());
        assertEquals(BattleSpellCatalog.ATTRIBUTE_UNSPECIFIED, spell.attribute());
        assertEquals(2, spell.sourceType());
        assertEquals(15, spell.procChance());
        assertEquals(1.5D, spell.damageMultiplier());
        assertEquals(1, spell.nativeId());
        assertEquals(823, spell.happenMessage());
        assertEquals(829, spell.stopMessage());
        assertEquals(InventoryCatalog.SECOND_LEVEL_DARK_STONE,
                spell.itemConsumeId());
        assertEquals(1, spell.itemConsumeCount());
        assertEquals(30, spell.effectiveMpCost(8, 0));
        assertEquals(28, spell.effectiveMpCost(8, 2));
        assertArrayEquals(new byte[] {0x10, 0x37, 0x03, 0},
                Protocol.messageCode(spell.happenMessage()));
        assertArrayEquals(new byte[] {0x10, 0x3D, 0x03, 0},
                Protocol.messageCode(spell.stopMessage()));
    }

    @Test
    void uncannyDodgeUsesExactSelectorOnlyDodgeContract() {
        byte[] nativeRequest = new Protocol.Writer().c(Protocol.C_USE_SPELL)
                .c(13).c(1).bytes();
        assertArrayEquals(new byte[] {0x14, 0x0D, 0x01}, nativeRequest);
        Protocol.SpellSelector request = Protocol.selectorOnlySpellRequest(nativeRequest);
        assertEquals(BattleSpellCatalog.UNCANNY_DODGE, request.skillId());
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.selectorOnlySpellRequest(new Protocol.Writer()
                        .c(Protocol.C_USE_SPELL).c(13).c(1).d(16).bytes()));

        BattleSpellCatalog.DodgeBuffSpell spell =
                BattleSpellCatalog.dodgeBuffSpell(
                        BattleSpellCatalog.UNCANNY_DODGE);
        assertEquals(106, spell.skillId());
        assertEquals(13, spell.row());
        assertEquals(1, spell.column());
        assertEquals(19, spell.actionId());
        assertEquals(11765, spell.effectGfx());
        assertEquals(20, spell.mpCost());
        assertEquals(0, spell.reuseDelayMillis());
        assertEquals(192, spell.durationSeconds());
        assertEquals(BattleSpellCatalog.ATTRIBUTE_UNSPECIFIED, spell.attribute());
        assertEquals(2, spell.sourceType());
        assertEquals(2, spell.nativeId());
        assertEquals(5, spell.dodgeModifier());
        assertEquals(20, spell.effectiveMpCost(8, 0));
        assertEquals(18, spell.effectiveMpCost(8, 2));
        assertNull(BattleSpellCatalog.dodgeBuffSpell(105));
    }

    @Test
    void enchantWeaponUsesExactCccdInventoryObjectContract() {
        long selectedWeaponObjectId = 0x7000_0012L;
        byte[] nativeRequest = new Protocol.Writer().c(Protocol.C_USE_SPELL)
                .c(1).c(3).d(selectedWeaponObjectId).bytes();
        assertArrayEquals(new byte[] {
                0x14, 0x01, 0x03, 0x12, 0x00, 0x00, 0x70
        }, nativeRequest);
        Protocol.ObjectSpellRequest request = Protocol.objectSpellRequest(nativeRequest);
        assertEquals(BattleSpellCatalog.ENCHANT_WEAPON, request.skillId());
        assertEquals(selectedWeaponObjectId, request.targetObjectId());
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.objectSpellRequest(new Protocol.Writer()
                        .c(Protocol.C_USE_SPELL).c(1).c(3).bytes()));
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.objectSpellRequest(new Protocol.Writer()
                        .c(Protocol.C_USE_SPELL).c(1).c(3)
                        .d(selectedWeaponObjectId).c(0).bytes()));

        BattleSpellCatalog.ItemWeaponEnchantSpell spell =
                BattleSpellCatalog.itemWeaponEnchantSpell(request.skillId());
        assertEquals(12, spell.skillId());
        assertEquals(2, spell.meleeDamage());
        assertEquals(2, spell.rangedDamage());
        assertEquals(161, spell.successMessage());
        assertArrayEquals(new byte[] {0x10, (byte) 0xA1, 0x00, 0},
                Protocol.messageCode(spell.successMessage()));
    }

    @Test
    void blessedArmorUsesExactCccdInventoryObjectContract() {
        long selectedArmorObjectId = 0x0F00_2008L;
        byte[] nativeRequest = new Protocol.Writer().c(Protocol.C_USE_SPELL)
                .c(2).c(4).d(selectedArmorObjectId).bytes();
        assertArrayEquals(new byte[] {
                0x14, 0x02, 0x04, 0x08, 0x20, 0x00, 0x0F
        }, nativeRequest);
        Protocol.ObjectSpellRequest request = Protocol.objectSpellRequest(nativeRequest);
        assertEquals(BattleSpellCatalog.BLESSED_ARMOR, request.skillId());
        assertEquals(selectedArmorObjectId, request.targetObjectId());
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.objectSpellRequest(new Protocol.Writer()
                        .c(Protocol.C_USE_SPELL).c(2).c(4).bytes()));
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.objectSpellRequest(new Protocol.Writer()
                        .c(Protocol.C_USE_SPELL).c(2).c(4)
                        .d(selectedArmorObjectId).c(0).bytes()));

        BattleSpellCatalog.ItemArmorEnchantSpell spell =
                BattleSpellCatalog.itemArmorEnchantSpell(request.skillId());
        assertEquals(-3, spell.armorClass());
        assertEquals(161, spell.successMessage());
        assertEquals(308, spell.expirationMessage());
    }

    @Test
    void shadowFangUsesExactCccdInventoryObjectContract() {
        long selectedWeaponObjectId = 0x7000_0042L;
        byte[] nativeRequest = new Protocol.Writer().c(Protocol.C_USE_SPELL)
                .c(13).c(2).d(selectedWeaponObjectId).bytes();
        assertArrayEquals(new byte[] {
                0x14, 0x0D, 0x02, 0x42, 0x00, 0x00, 0x70
        }, nativeRequest);
        Protocol.ObjectSpellRequest request = Protocol.objectSpellRequest(nativeRequest);
        assertEquals(BattleSpellCatalog.SHADOW_FANG, request.skillId());
        assertEquals(selectedWeaponObjectId, request.targetObjectId());
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.objectSpellRequest(new Protocol.Writer()
                        .c(Protocol.C_USE_SPELL).c(13).c(2).bytes()));
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.objectSpellRequest(new Protocol.Writer()
                        .c(Protocol.C_USE_SPELL).c(13).c(2)
                        .d(selectedWeaponObjectId).d(123).bytes()));

        BattleSpellCatalog.ItemWeaponEnchantSpell spell =
                BattleSpellCatalog.itemWeaponEnchantSpell(request.skillId());
        assertEquals(107, spell.skillId());
        assertEquals(InventoryCatalog.SECOND_LEVEL_DARK_STONE,
                spell.itemConsumeId());
        assertEquals(5, spell.meleeDamage());
        assertEquals(5, spell.rangedDamage());
        assertArrayEquals(new byte[] {0x10, 0x4F, 0x00, 0},
                Protocol.messageCode(spell.invalidItemMessage()));
    }

    @Test
    void finalBurnUsesExactNativeCase107ObjectCoordinateEnvelope() {
        byte[] nativeRequest = new Protocol.Writer().c(Protocol.C_USE_SPELL)
                .c(13).c(3).d(0x0102_0304L).h(32749).h(32812).bytes();
        assertArrayEquals(new byte[] {
                0x14, 0x0D, 0x03, 0x04, 0x03, 0x02, 0x01,
                (byte) 0xED, 0x7F, 0x2C, (byte) 0x80}, nativeRequest);
        Protocol.AttackSpellRequest request = Protocol.attackSpellRequest(nativeRequest);
        assertEquals(BattleSpellCatalog.FINAL_BURN, request.skillId());
        assertEquals(0x0102_0304L, request.targetObjectId());
        assertEquals(32749, request.x());
        assertEquals(32812, request.y());
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.attackSpellRequest(new Protocol.Writer()
                        .c(Protocol.C_USE_SPELL).c(13).c(3)
                        .d(0x0102_0304L).h(32749).bytes()));
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.attackSpellRequest(new Protocol.Writer()
                        .c(Protocol.C_USE_SPELL).c(13).c(3)
                        .d(0x0102_0304L).h(32749).h(32812).c(0).bytes()));
    }

    @Test
    void venomResistUsesExactSelectorOnlyThreePoisonFamilyContract() {
        byte[] nativeRequest = new Protocol.Writer().c(Protocol.C_USE_SPELL)
                .c(12).c(7).bytes();
        assertArrayEquals(new byte[] {0x14, 0x0C, 0x07}, nativeRequest);
        Protocol.SpellSelector request = Protocol.selectorOnlySpellRequest(nativeRequest);
        assertEquals(BattleSpellCatalog.VENOM_RESIST, request.skillId());
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.selectorOnlySpellRequest(new Protocol.Writer()
                        .c(Protocol.C_USE_SPELL).c(12).c(7).d(15).bytes()));

        BattleSpellCatalog.PoisonResistanceSpell spell =
                BattleSpellCatalog.poisonResistanceSpell(
                        BattleSpellCatalog.VENOM_RESIST);
        assertEquals(104, spell.skillId());
        assertEquals(12, spell.row());
        assertEquals(7, spell.column());
        assertEquals(19, spell.actionId());
        assertEquals(2948, spell.effectGfx());
        assertEquals(20, spell.mpCost());
        assertEquals(0, spell.reuseDelayMillis());
        assertEquals(320, spell.durationSeconds());
        assertEquals(BattleSpellCatalog.ATTRIBUTE_UNSPECIFIED, spell.attribute());
        assertEquals(2, spell.sourceType());
        assertEquals(128, spell.nativeId());
        assertEquals(0, spell.happenMessage());
        assertEquals(831, spell.stopMessage());
        assertEquals(0, spell.failureMessage());
        assertEquals(20, spell.effectiveMpCost(8, 0));
        assertEquals(18, spell.effectiveMpCost(8, 2));
        assertNull(BattleSpellCatalog.poisonResistanceSpell(103));
        assertArrayEquals(new byte[] {0x10, 0x3F, 0x03, 0},
                Protocol.messageCode(spell.stopMessage()));
    }

    @Test
    void shockSkinMagmaArrowUsesExactPost175SelectorOnlyContract() {
        byte[] nativeRequest = new Protocol.Writer().c(Protocol.C_USE_SPELL)
                .c(23).c(4).bytes();
        assertArrayEquals(new byte[] {0x14, 0x17, 0x04}, nativeRequest);
        Protocol.SpellSelector request =
                Protocol.selectorOnlySpellRequest(nativeRequest);
        assertEquals(BattleSpellCatalog.SHOCK_SKIN, request.skillId());
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.selectorOnlySpellRequest(new Protocol.Writer()
                        .c(Protocol.C_USE_SPELL).c(23).c(4).d(18).bytes()));
    }

    @Test
    void darkBlindUsesExactNativeCase102ObjectCoordinateEnvelope() {
        byte[] nativeRequest = new Protocol.Writer().c(Protocol.C_USE_SPELL)
                .c(12).c(6).d(0x0102_0304L).h(32749).h(32812).bytes();
        assertArrayEquals(new byte[] {
                0x14, 0x0C, 0x06, 0x04, 0x03, 0x02, 0x01,
                (byte) 0xED, 0x7F, 0x2C, (byte) 0x80}, nativeRequest);
        Protocol.AttackSpellRequest request = Protocol.attackSpellRequest(nativeRequest);
        assertEquals(BattleSpellCatalog.DARK_BLIND, request.skillId());
        assertEquals(0x0102_0304L, request.targetObjectId());
        assertEquals(32749, request.x());
        assertEquals(32812, request.y());
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.attackSpellRequest(new Protocol.Writer()
                        .c(Protocol.C_USE_SPELL).c(12).c(6)
                        .d(0x0102_0304L).h(32749).h(32812).c(0).bytes()));
    }

    @Test
    void trueTargetUsesExactNativeRequestMarkerAndClassicSkillMetadata() {
        Protocol.TrueTargetRequest request = Protocol.trueTargetRequest(
                new Protocol.Writer().c(Protocol.C_USE_SPELL).c(14).c(0)
                        .d(0x1122_3344L).h(32731).h(32799).s("集火").bytes());
        assertEquals(BattleSpellCatalog.TRUE_TARGET, request.skillId());
        assertEquals(0x1122_3344L, request.targetObjectId());
        assertEquals(32731, request.x());
        assertEquals(32799, request.y());
        assertEquals("集火", request.text());
        assertArrayEquals(new Protocol.Writer().c(Protocol.S_COMMAND_TARGET)
                        .d(0x1122_3344L).h(32730).h(32798).s("集火").bytes(),
                Protocol.commandTarget(0x1122_3344L, 32730, 32798, "集火"));

        BattleSpellCatalog.TrueTargetSpell spell =
                BattleSpellCatalog.trueTargetSpell(BattleSpellCatalog.TRUE_TARGET);
        assertEquals(14, spell.row());
        assertEquals(0, spell.column());
        assertEquals(15, spell.skillLevel());
        assertEquals(19, spell.actionId());
        assertEquals(1765, spell.effectGfx());
        assertEquals(1, spell.mpCost());
        assertEquals(0, spell.hpCost());
        assertEquals(0, spell.reuseDelayMillis());
        assertEquals(0, spell.durationSeconds());
        assertEquals(-1, spell.maximumRange());
        assertEquals(3, spell.targetTo());
        assertEquals(0, spell.attribute());
        assertEquals(2, spell.sourceType());
        assertEquals(1, spell.nativeId());
        assertEquals("$1995", spell.nameId());
        assertEquals(true, spell.counterMagicBypass());
        assertEquals(1, spell.effectiveMpCost(0));
        assertEquals(1, spell.effectiveMpCost(2));

        assertThrows(IllegalArgumentException.class, () -> Protocol.trueTargetRequest(
                new Protocol.Writer().c(Protocol.C_USE_SPELL).c(14).c(1)
                        .d(1).h(1).h(1).s("x").bytes()));
        assertThrows(IllegalArgumentException.class, () -> Protocol.trueTargetRequest(
                new Protocol.Writer().c(Protocol.C_USE_SPELL).c(14).c(0)
                        .d(0).h(1).h(1).s("x").bytes()));
        assertThrows(IllegalArgumentException.class, () -> Protocol.trueTargetRequest(
                new Protocol.Writer().c(Protocol.C_USE_SPELL).c(14).c(0)
                        .d(1).h(1).h(1).s("x".repeat(53)).bytes()));
        assertThrows(IllegalArgumentException.class, () -> Protocol.trueTargetRequest(
                new Protocol.Writer().c(Protocol.C_USE_SPELL).c(14).c(0)
                        .d(1).h(1).h(1).s("x\n").bytes()));
    }

    @Test
    void trueTargetUsesTheClassicNineteenTileScreenEnvelope() {
        assertEquals(true, BattleSpellCatalog.isInNativeScreen(100, 100, 118, 100));
        assertEquals(false, BattleSpellCatalog.isInNativeScreen(100, 100, 119, 100));
        assertEquals(true, BattleSpellCatalog.isInNativeScreen(100, 100, 81, 100));
        assertEquals(false, BattleSpellCatalog.isInNativeScreen(100, 100, 120, 100));
        assertEquals(true, BattleSpellCatalog.isInNativeScreen(100, 100, 109, 109));
    }

    @Test
    void royalAurasUseExactSelectorsAndClassicSkillMetadata() {
        Protocol.SpellSelector request = Protocol.selectorOnlySpellRequest(
                new Protocol.Writer().c(Protocol.C_USE_SPELL).c(14).c(1).bytes());
        assertEquals(BattleSpellCatalog.GLOWING_AURA, request.skillId());

        BattleSpellCatalog.RoyalAuraSpell spell =
                BattleSpellCatalog.royalAuraSpell(BattleSpellCatalog.GLOWING_AURA);
        assertEquals(14, spell.row());
        assertEquals(1, spell.column());
        assertEquals(15, spell.skillLevel());
        assertEquals(19, spell.actionId());
        assertEquals(2277, spell.effectGfx());
        assertEquals(40, spell.mpCost());
        assertEquals(0, spell.hpCost());
        assertEquals(0, spell.reuseDelayMillis());
        assertEquals(640, spell.durationSeconds());
        assertEquals(0, spell.maximumRange());
        assertEquals(-1, spell.area());
        assertEquals(8, spell.targetTo());
        assertEquals(0, spell.attribute());
        assertEquals(2, spell.sourceType());
        assertEquals(2, spell.nativeId());
        assertEquals("$1996", spell.nameId());
        assertEquals(0, spell.armorClass());
        assertEquals(5, spell.meleeHit());
        assertEquals(5, spell.rangedHit());
        assertEquals(20, spell.magicResistance());
        assertTrue(spell.counterMagicBypass());
        assertEquals(40, spell.effectiveMpCost(0));
        assertEquals(38, spell.effectiveMpCost(2));

        Protocol.SpellSelector shiningRequest = Protocol.selectorOnlySpellRequest(
                new Protocol.Writer().c(Protocol.C_USE_SPELL).c(14).c(2).bytes());
        assertEquals(BattleSpellCatalog.SHINING_AURA, shiningRequest.skillId());
        BattleSpellCatalog.RoyalAuraSpell shining =
                BattleSpellCatalog.royalAuraSpell(BattleSpellCatalog.SHINING_AURA);
        assertEquals(14, shining.row());
        assertEquals(2, shining.column());
        assertEquals(15, shining.skillLevel());
        assertEquals(19, shining.actionId());
        assertEquals(3941, shining.effectGfx());
        assertEquals(40, shining.mpCost());
        assertEquals(640, shining.durationSeconds());
        assertEquals(-1, shining.area());
        assertEquals(12, shining.targetTo());
        assertEquals(4, shining.nativeId());
        assertEquals("$1977", shining.nameId());
        assertEquals(-8, shining.armorClass());
        assertEquals(0, shining.meleeHit());
        assertEquals(0, shining.rangedHit());
        assertEquals(0, shining.magicResistance());
        assertTrue(shining.counterMagicBypass());
        assertEquals(38, shining.effectiveMpCost(2));

        Protocol.SpellSelector braveRequest = Protocol.selectorOnlySpellRequest(
                new Protocol.Writer().c(Protocol.C_USE_SPELL).c(14).c(4).bytes());
        assertEquals(BattleSpellCatalog.BRAVE_AURA, braveRequest.skillId());
        BattleSpellCatalog.RoyalAuraSpell brave =
                BattleSpellCatalog.royalAuraSpell(BattleSpellCatalog.BRAVE_AURA);
        assertEquals(14, brave.row());
        assertEquals(4, brave.column());
        assertEquals(15, brave.skillLevel());
        assertEquals(19, brave.actionId());
        assertEquals(3942, brave.effectGfx());
        assertEquals(40, brave.mpCost());
        assertEquals(640, brave.durationSeconds());
        assertEquals(-1, brave.area());
        assertEquals(8, brave.targetTo());
        assertEquals(16, brave.nativeId());
        assertEquals("$3178", brave.nameId());
        assertEquals(0, brave.armorClass());
        assertEquals(0, brave.meleeHit());
        assertEquals(0, brave.rangedHit());
        assertEquals(0, brave.magicResistance());
        assertEquals(5, brave.meleeDamage());
        assertTrue(brave.counterMagicBypass());
        assertEquals(38, brave.effectiveMpCost(2));
        assertNull(BattleSpellCatalog.royalAuraSpell(116));
    }

    @Test
    void physicalAttackEndsAtTheNativeZeroSequenceBranch() {
        Protocol.Reader hit = new Protocol.Reader(Protocol.physicalAttack(
                BattleActionCodes.ATTACK, 0x6000_0042L, 2, true, 6));
        assertEquals(Protocol.S_ATTACK, hit.c());
        assertEquals(BattleActionCodes.ATTACK, hit.c());
        assertEquals(0x6000_0042L, hit.d());
        assertEquals(2, hit.d());
        assertEquals(10, hit.c());
        assertEquals(6, hit.c());
        assertEquals(0, hit.d());
        assertEquals(0, hit.remaining());

        Protocol.Reader miss = new Protocol.Reader(Protocol.physicalAttack(
                BattleActionCodes.ATTACK, 0x6000_0042L, 2, false, 6));
        miss.c();
        miss.c();
        miss.d();
        miss.d();
        assertEquals(0, miss.c());
    }

    @Test
    void shockStunAttackUsesOrdinaryBranchWithByteClampedRealDamage() {
        Protocol.Reader attack = new Protocol.Reader(Protocol.shockStunAttack(
                BattleActionCodes.TWO_HAND_SWORD_ATTACK,
                0x6000_0042L, 0x6000_0043L, 300, 6));
        assertEquals(Protocol.S_ATTACK, attack.c());
        assertEquals(BattleActionCodes.TWO_HAND_SWORD_ATTACK, attack.c());
        assertEquals(0x6000_0042L, attack.d());
        assertEquals(0x6000_0043L, attack.d());
        assertEquals(0xFF, attack.c());
        assertEquals(6, attack.c());
        assertEquals(0, attack.d());
        assertEquals(0, attack.remaining());

        Protocol.Reader effect = new Protocol.Reader(Protocol.shockStunEffect(
                new BattleShockStunEffects.Effect(
                        0x7000_0042L, 0x6000_0043L, 32749, 32813, 5_000_000_000L)));
        assertEquals(Protocol.S_PUT_OBJECT, effect.c());
        assertEquals(32749, effect.h());
        assertEquals(32813, effect.h());
        assertEquals(0x7000_0042L, effect.d());
        assertEquals(BattleShockStunEffects.GFX, effect.h());
        assertEquals(0, effect.c());
        assertEquals(0, effect.c());
        assertEquals(0, effect.c());
        assertEquals(BattleTimedStatus.NORMAL, effect.c());
        assertEquals(0, effect.d());
        assertEquals(0, effect.h());
        assertEquals("", effect.s());
        assertEquals("", effect.s());
        assertEquals(0, effect.c());
        assertEquals(0, effect.d());
        assertEquals("", effect.s());
        assertEquals("", effect.s());
        assertEquals(0, effect.c());
        assertEquals(0xFF, effect.c());
        assertEquals(0, effect.c());
        assertEquals(0, effect.c());
        assertEquals("", effect.s());
        assertEquals(0, effect.h());
        assertEquals(0, effect.c());
        assertEquals(0, effect.remaining());
    }

    @Test
    void rangedPhysicalAttackUsesTheNativeExtendedProjectileBranch() {
        Protocol.Reader attack = new Protocol.Reader(Protocol.rangedPhysicalAttack(
                20, 0x6000_0042L, 0x1122_3344L, 17, 3,
                4_500_123L, 66, 32730, 32798, 32731, 32799));
        assertEquals(Protocol.S_ATTACK, attack.c());
        assertEquals(20, attack.c());
        assertEquals(0x6000_0042L, attack.d());
        assertEquals(0x1122_3344L, attack.d());
        assertEquals(17, attack.c());
        assertEquals(3, attack.c());
        assertEquals(4_500_123L, attack.d());
        assertEquals(66, attack.h());
        assertEquals(0x7f, attack.c());
        assertEquals(32730, attack.h());
        assertEquals(32798, attack.h());
        assertEquals(32731, attack.h());
        assertEquals(32799, attack.h());
        assertEquals(0, attack.c());
        assertEquals("", attack.s());
        assertEquals(0, attack.remaining());

        Protocol.Reader miss = new Protocol.Reader(Protocol.rangedPhysicalAttack(
                20, 0x6000_0042L, 0, 0, 3,
                4_500_124L, 66, 32730, 32798, 32731, 32799));
        miss.c();
        miss.c();
        miss.d();
        assertEquals(0, miss.d());
        assertEquals(0, miss.c());
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.rangedPhysicalAttack(
                        20, 0x6000_0042L, 0, 1, 3,
                        4_500_124L, 66, 32730, 32798, 32731, 32799));
    }

    @Test
    void boneBreakUsesSelectorOnlyRequestAndExactEffectActorBody() {
        byte[] request = new Protocol.Writer().c(Protocol.C_USE_SPELL)
                .c(25).c(7).bytes();
        assertEquals(BattleSpellCatalog.BONE_BREAK,
                Protocol.selectorOnlySpellRequest(request).skillId());

        Protocol.Reader effect = new Protocol.Reader(Protocol.boneBreakEffect(
                new BattleBoneBreakEffects.Effect(
                        0x7100_0042L, 0x6000_0043L,
                        32749, 32813, 5_000_000_000L)));
        assertEquals(Protocol.S_PUT_OBJECT, effect.c());
        assertEquals(32749, effect.h());
        assertEquals(32813, effect.h());
        assertEquals(0x7100_0042L, effect.d());
        assertEquals(BattleBoneBreakEffects.GFX, effect.h());
        assertEquals(0, effect.c());
        assertEquals(0, effect.c());
        assertEquals(0, effect.c());
        assertEquals(BattleTimedStatus.NORMAL, effect.c());
        assertEquals(0, effect.d());
        assertEquals(0, effect.h());
        assertEquals("", effect.s());
        assertEquals("", effect.s());
        assertEquals(0, effect.c());
        assertEquals(0, effect.d());
        assertEquals("", effect.s());
        assertEquals("", effect.s());
        assertEquals(0, effect.c());
        assertEquals(0xFF, effect.c());
        assertEquals(0, effect.c());
        assertEquals(0, effect.c());
        assertEquals("", effect.s());
        assertEquals(0, effect.h());
        assertEquals(0, effect.c());
        assertEquals(0, effect.remaining());
    }

    @Test
    void attackAllWriterUsesExactNativeFiveByteTargetStride() {
        Protocol.Reader attack = new Protocol.Reader(Protocol.attackAll(
                BattleActionCodes.ATTACK, 0x0102_0304L,
                List.of(new Protocol.AttackAllTarget(0x1122_3344L, true),
                        new Protocol.AttackAllTarget(0x5566_7788L, false))));
        assertEquals(Protocol.S_ATTACK_ALL, attack.c());
        assertEquals(BattleActionCodes.ATTACK, attack.c());
        assertEquals(0x0102_0304L, attack.d());
        assertEquals(2, attack.h());
        assertEquals(0x1122_3344L, attack.d());
        assertEquals(0x20, attack.c());
        assertEquals(0x5566_7788L, attack.d());
        assertEquals(0, attack.c());
        assertEquals(0, attack.remaining());

        assertThrows(IllegalArgumentException.class,
                () -> Protocol.attackAll(0, 1, List.of()));
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.attackAll(0, 0,
                        List.of(new Protocol.AttackAllTarget(1, true))));
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.attackAll(0, 1,
                        List.of(new Protocol.AttackAllTarget(0, true))));
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.attackAll(0, 1,
                        java.util.Collections.singletonList(null)));
    }

    @Test
    void lightArrowUsesNativeAttackRequestAndExtendedAttackResponse() {
        Protocol.AttackSpellRequest request = Protocol.attackSpellRequest(
                new Protocol.Writer().c(Protocol.C_USE_SPELL)
                        .c(0).c(3).d(0x1122_3344L).h(32731).h(32799).bytes());
        assertEquals(BattleSpellCatalog.LIGHT_ARROW, request.skillId());
        assertEquals(0x1122_3344L, request.targetObjectId());
        assertEquals(32731, request.x());
        assertEquals(32799, request.y());

        Protocol.Reader attack = new Protocol.Reader(Protocol.attackSpell(
                18, 0x0102_0304L, request.targetObjectId(), 10, 3,
                4_500_001L, 167, 32730, 32798, request.x(), request.y()));
        assertEquals(Protocol.S_ATTACK, attack.c());
        assertEquals(18, attack.c());
        assertEquals(0x0102_0304L, attack.d());
        assertEquals(0x1122_3344L, attack.d());
        assertEquals(10, attack.c());
        assertEquals(3, attack.c());
        assertEquals(4_500_001L, attack.d());
        assertEquals(167, attack.h());
        assertEquals(0, attack.c());
        assertEquals(32730, attack.h());
        assertEquals(32798, attack.h());
        assertEquals(32731, attack.h());
        assertEquals(32799, attack.h());
        assertEquals(0, attack.c());
        assertEquals("", attack.s());
        assertEquals(0, attack.remaining());
    }

    @Test
    void attackSpellAllowsRetainedSafetyZoneZeroTargetOnlyWithoutHitMarker() {
        Protocol.Reader attack = new Protocol.Reader(Protocol.attackSpell(
                18, 0x0102_0304L, 0, 0, 3,
                4_500_002L, 167, 32730, 32798, 32731, 32799));
        assertEquals(Protocol.S_ATTACK, attack.c());
        assertEquals(18, attack.c());
        assertEquals(0x0102_0304L, attack.d());
        assertEquals(0, attack.d());
        assertEquals(0, attack.c());
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.attackSpell(
                        18, 0x0102_0304L, 0, 10, 3,
                        4_500_002L, 167, 32730, 32798, 32731, 32799));
    }

    @Test
    void burningFireballUsesExactNativeAttackManyLayout() {
        byte[] nativeRequest = new Protocol.Writer().c(Protocol.C_USE_SPELL)
                .c(3).c(0).d(0x1122_3344L).h(32731).h(32799).bytes();
        Protocol.AttackSpellRequest request =
                Protocol.attackSpellRequest(nativeRequest);
        assertEquals(BattleSpellCatalog.BURNING_FIREBALL, request.skillId());
        assertEquals(0x1122_3344L, request.targetObjectId());
        assertEquals(32731, request.x());
        assertEquals(32799, request.y());
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.attackSpellRequest(java.util.Arrays.copyOf(
                        nativeRequest, nativeRequest.length - 1)));
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.attackSpellRequest(java.util.Arrays.copyOf(
                        nativeRequest, nativeRequest.length + 1)));

        Protocol.Reader attack = new Protocol.Reader(Protocol.attackMany(
                18, 0x0102_0304L, 32730, 32798, 3, 4_500_002L, 171,
                List.of(new Protocol.AttackManyTarget(0x1122_3344L, true),
                        new Protocol.AttackManyTarget(0x5566_7788L, false))));
        assertEquals(Protocol.S_ATTACK_MANY, attack.c());
        assertEquals(18, attack.c());
        assertEquals(0x0102_0304L, attack.d());
        assertEquals(32730, attack.h());
        assertEquals(32798, attack.h());
        assertEquals(3, attack.c());
        assertEquals(4_500_002L, attack.d());
        assertEquals(171, attack.h());
        assertEquals(8, attack.c());
        assertEquals(0, attack.c());
        assertEquals("", attack.s());
        assertEquals(2, attack.h());
        assertEquals(0x1122_3344L, attack.d());
        assertEquals(0x20, attack.c());
        assertEquals(0x5566_7788L, attack.d());
        assertEquals(0, attack.c());
        assertEquals(0, attack.remaining());

        Protocol.Reader empty = new Protocol.Reader(Protocol.attackMany(
                18, 1, 1, 1, 0, 1, 171, 0, List.of()));
        assertEquals(Protocol.S_ATTACK_MANY, empty.c());
        assertEquals(18, empty.c());
        assertEquals(1, empty.d());
        assertEquals(1, empty.h());
        assertEquals(1, empty.h());
        assertEquals(0, empty.c());
        assertEquals(1, empty.d());
        assertEquals(171, empty.h());
        assertEquals(0, empty.c());
        assertEquals(0, empty.c());
        assertEquals("", empty.s());
        assertEquals(0, empty.h());
        assertEquals(0, empty.remaining());
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.attackMany(18, 1, 1, 1, 0, 1, 171,
                        List.of(new Protocol.AttackManyTarget(0, true))));
    }

    @Test
    void earthJailUsesExactNativeCase29TargetCoordinateEnvelope() {
        byte[] nativeRequest = new Protocol.Writer().c(Protocol.C_USE_SPELL)
                .c(3).c(5).d(0x1122_3344L).h(32731).h(32799).bytes();
        assertArrayEquals(new byte[] {
                0x14, 0x03, 0x05, 0x44, 0x33, 0x22, 0x11,
                (byte) 0xDB, 0x7F, 0x1F, (byte) 0x80}, nativeRequest);
        Protocol.AttackSpellRequest request =
                Protocol.attackSpellRequest(nativeRequest);
        assertEquals(BattleSpellCatalog.EARTH_JAIL, request.skillId());
        assertEquals(3, request.row());
        assertEquals(5, request.column());
        assertEquals(0x1122_3344L, request.targetObjectId());
        assertEquals(32731, request.x());
        assertEquals(32799, request.y());
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.attackSpellRequest(java.util.Arrays.copyOf(
                        nativeRequest, nativeRequest.length - 1)));
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.attackSpellRequest(java.util.Arrays.copyOf(
                        nativeRequest, nativeRequest.length + 1)));

        Protocol.Reader attack = new Protocol.Reader(Protocol.attackMany(
                18, 0x0102_0304L, 32730, 32798, 6, 4_500_002L, 1_805,
                List.of(new Protocol.AttackManyTarget(0x1122_3344L, true),
                        new Protocol.AttackManyTarget(0x5566_7788L, false))));
        assertEquals(Protocol.S_ATTACK_MANY, attack.c());
        assertEquals(18, attack.c());
        assertEquals(0x0102_0304L, attack.d());
        assertEquals(32730, attack.h());
        assertEquals(32798, attack.h());
        assertEquals(6, attack.c());
        assertEquals(4_500_002L, attack.d());
        assertEquals(1_805, attack.h());
        assertEquals(8, attack.c());
        assertEquals(0, attack.c());
        assertEquals("", attack.s());
        assertEquals(2, attack.h());
        assertEquals(0x1122_3344L, attack.d());
        assertEquals(0x20, attack.c());
        assertEquals(0x5566_7788L, attack.d());
        assertEquals(0, attack.c());
        assertEquals(0, attack.remaining());
    }

    @Test
    void frozenCloudUsesExactNativeTargetedAreaRequest() {
        byte[] payload = new Protocol.Writer().c(Protocol.C_USE_SPELL)
                .c(2).c(5).d(0x1122_3344L).h(32731).h(32799).bytes();
        Protocol.AttackSpellRequest request = Protocol.attackSpellRequest(payload);
        assertEquals(BattleSpellCatalog.FROZEN_CLOUD, request.skillId());
        assertEquals(0x1122_3344L, request.targetObjectId());
        assertEquals(32731, request.x());
        assertEquals(32799, request.y());

        assertThrows(IllegalArgumentException.class,
                () -> Protocol.attackSpellRequest(java.util.Arrays.copyOf(
                        payload, payload.length - 1)));
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.attackSpellRequest(java.util.Arrays.copyOf(
                        payload, payload.length + 1)));
    }

    @Test
    void freezingBlizzardUsesSelectorOnlyAndNoDirectionAttackMany() {
        Protocol.SpellSelector request = Protocol.selectorOnlySpellRequest(
                new Protocol.Writer().c(Protocol.C_USE_SPELL).c(9).c(7).bytes());
        assertEquals(BattleSpellCatalog.FREEZING_BLIZZARD, request.skillId());

        Protocol.Reader attack = new Protocol.Reader(Protocol.attackMany(
                18, 0x0102_0304L, 32730, 32798, 6, 4_500_003L, 3933, 0,
                List.of(new Protocol.AttackManyTarget(0x1122_3344L, true),
                        new Protocol.AttackManyTarget(0x5566_7788L, false))));
        assertEquals(Protocol.S_ATTACK_MANY, attack.c());
        assertEquals(18, attack.c());
        assertEquals(0x0102_0304L, attack.d());
        assertEquals(32730, attack.h());
        assertEquals(32798, attack.h());
        assertEquals(6, attack.c());
        assertEquals(4_500_003L, attack.d());
        assertEquals(3933, attack.h());
        assertEquals(0, attack.c());
        assertEquals(0, attack.h());
        assertEquals(2, attack.h());
        assertEquals(0x1122_3344L, attack.d());
        assertEquals(0x20, attack.c());
        assertEquals(0x5566_7788L, attack.d());
        assertEquals(0, attack.c());
        assertEquals(0, attack.remaining());

        assertThrows(IllegalArgumentException.class,
                () -> Protocol.attackMany(18, 1, 1, 1, 0, 1, 3933, 1,
                        List.of(new Protocol.AttackManyTarget(2, true))));
    }

    @Test
    void reservedSkillsPreserveTheirExactNativeProducerShapes() {
        Protocol.SpellSelector wizardReserved =
                Protocol.selectorOnlySpellRequest(
                        new Protocol.Writer().c(Protocol.C_USE_SPELL)
                                .c(2).c(7).bytes());
        assertEquals(24, wizardReserved.skillId());

        for (int column : new int[] {3, 5, 6, 7}) {
            Protocol.SpellSelector request = Protocol.selectorOnlySpellRequest(
                    new Protocol.Writer().c(Protocol.C_USE_SPELL)
                            .c(11).c(column).bytes());
            assertEquals(89 + column, request.skillId());
        }

        Protocol.ObjectSpellRequest objectRequest = Protocol.objectSpellRequest(
                new Protocol.Writer().c(Protocol.C_USE_SPELL)
                        .c(11).c(4).d(0x0102_0304L).bytes());
        assertEquals(93, objectRequest.skillId());
        assertEquals(0x0102_0304L, objectRequest.targetObjectId());

        assertThrows(IllegalArgumentException.class,
                () -> Protocol.selectorOnlySpellRequest(
                        new Protocol.Writer().c(Protocol.C_USE_SPELL)
                                .c(11).c(3).c(0).bytes()));
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.selectorOnlySpellRequest(
                        new Protocol.Writer().c(Protocol.C_USE_SPELL)
                                .c(2).c(7).c(0).bytes()));
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.objectSpellRequest(
                        new Protocol.Writer().c(Protocol.C_USE_SPELL)
                                .c(11).c(4).bytes()));
    }

    @Test
    void learnedSpellBitmapsMatchTheExactCountAndEightSkillBuckets() {
        assertArrayEquals(new byte[] {0x1E, 0x01, 0x09},
                Protocol.addSpells(Set.of(1, 4)));
        assertArrayEquals(new byte[] {0x1E, 0x08, 0x09, 0, 0, 0, 0, 0, 0, 0x08},
                Protocol.addSpells(Set.of(1, 4, 60)));
        assertArrayEquals(new byte[] {0x1E, 0x08, 0x09, 0, 0, 0, 0x01, 0, 0, 0x08},
                Protocol.addSpells(Set.of(1, 4, 33, 60)));
        assertArrayEquals(new byte[] {0x1E, 0x08, 0x09, 0x04, 0, 0, 0, 0, 0, 0x42},
                Protocol.addSpells(Set.of(1, 4, 11, 58, 63)));
        assertArrayEquals(new byte[] {0x1F, 0x01, 0x08},
                Protocol.removeSpells(Set.of(4)));
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.addSpells(Set.of(221)));
    }

    @Test
    void lightArrowCatalogMatchesTheClassicSkillResource() {
        BattleSpellCatalog.AttackSpell spell =
                BattleSpellCatalog.attackSpell(BattleSpellCatalog.LIGHT_ARROW);
        assertEquals(0, spell.row());
        assertEquals(3, spell.column());
        assertEquals(18, spell.actionId());
        assertEquals(167, spell.effectGfx());
        assertEquals(3, spell.mpCost());
        assertEquals(10, spell.reuseDelayMillis());
        assertEquals(10, spell.maximumRange());
        assertEquals(6, spell.baseDamage());
        assertEquals(1, spell.diceCount());
        assertEquals(10, spell.dieSides());
        assertEquals(16, spell.attribute());
    }

    @Test
    void burningFireballCatalogMatchesTheClassicAreaSkillResource() {
        BattleSpellCatalog.AreaAttackSpell spell =
                BattleSpellCatalog.areaAttackSpell(BattleSpellCatalog.BURNING_FIREBALL);
        assertEquals(3, spell.row());
        assertEquals(0, spell.column());
        assertEquals(18, spell.actionId());
        assertEquals(171, spell.effectGfx());
        assertEquals(20, spell.mpCost());
        assertEquals(500, spell.reuseDelayMillis());
        assertEquals(6, spell.maximumRange());
        assertEquals(3, spell.areaRadius());
        assertEquals(0, spell.baseDamage());
        assertEquals(5, spell.diceCount());
        assertEquals(9, spell.dieSides());
        assertEquals(2, spell.attribute());
    }

    @Test
    void tripleArrowUsesExactNativeCase131TargetCoordinateEnvelope() {
        byte[] nativeRequest = new Protocol.Writer().c(Protocol.C_USE_SPELL)
                .c(16).c(3).d(0x1122_3344L).h(32749).h(32812).bytes();
        assertArrayEquals(new byte[] {
                0x14, 0x10, 0x03, 0x44, 0x33, 0x22, 0x11,
                (byte) 0xED, 0x7F, 0x2C, (byte) 0x80}, nativeRequest);
        Protocol.AttackSpellRequest request =
                Protocol.attackSpellRequest(nativeRequest);
        assertEquals(BattleSpellCatalog.TRIPLE_ARROW, request.skillId());
        assertEquals(16, request.row());
        assertEquals(3, request.column());
        assertEquals(0x1122_3344L, request.targetObjectId());
        assertEquals(32749, request.x());
        assertEquals(32812, request.y());
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.attackSpellRequest(new Protocol.Writer()
                        .c(Protocol.C_USE_SPELL).c(16).c(3)
                        .d(0x1122_3344L).h(32749).h(32812).c(0).bytes()));
    }

    @Test
    void elementalFallDownUsesExactNativeCase132TargetCoordinateEnvelope() {
        byte[] nativeRequest = new Protocol.Writer().c(Protocol.C_USE_SPELL)
                .c(16).c(4).d(0x1122_3344L).h(32749).h(32812).bytes();
        assertArrayEquals(new byte[] {
                0x14, 0x10, 0x04, 0x44, 0x33, 0x22, 0x11,
                (byte) 0xED, 0x7F, 0x2C, (byte) 0x80}, nativeRequest);
        Protocol.AttackSpellRequest request =
                Protocol.attackSpellRequest(nativeRequest);
        assertEquals(BattleSpellCatalog.ELEMENTAL_FALL_DOWN,
                request.skillId());
        assertEquals(16, request.row());
        assertEquals(4, request.column());
        assertEquals(0x1122_3344L, request.targetObjectId());
        assertEquals(32749, request.x());
        assertEquals(32812, request.y());
    }

    @Test
    void polluteWaterUsesExactNativeCase172TargetCoordinateEnvelope() {
        byte[] nativeRequest = new Protocol.Writer().c(Protocol.C_USE_SPELL)
                .c(21).c(4).d(0x1122_3344L).h(32749).h(32812).bytes();
        assertArrayEquals(new byte[] {
                0x14, 0x15, 0x04, 0x44, 0x33, 0x22, 0x11,
                (byte) 0xED, 0x7F, 0x2C, (byte) 0x80}, nativeRequest);
        Protocol.AttackSpellRequest request =
                Protocol.attackSpellRequest(nativeRequest);
        assertEquals(BattleSpellCatalog.POLLUTE_WATER, request.skillId());
        assertEquals(21, request.row());
        assertEquals(4, request.column());
        assertEquals(0x1122_3344L, request.targetObjectId());
        assertEquals(32749, request.x());
        assertEquals(32812, request.y());
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.attackSpellRequest(new Protocol.Writer()
                        .c(Protocol.C_USE_SPELL).c(21).c(4)
                        .d(0x1122_3344L).h(32749).h(32812).c(0).bytes()));
    }

    @Test
    void strikerGaleUsesExactNativeCase173TargetCoordinateEnvelope() {
        byte[] nativeRequest = new Protocol.Writer().c(Protocol.C_USE_SPELL)
                .c(21).c(5).d(0x1122_3344L).h(32749).h(32812).bytes();
        assertArrayEquals(new byte[] {
                0x14, 0x15, 0x05, 0x44, 0x33, 0x22, 0x11,
                (byte) 0xED, 0x7F, 0x2C, (byte) 0x80}, nativeRequest);
        Protocol.AttackSpellRequest request =
                Protocol.attackSpellRequest(nativeRequest);
        assertEquals(BattleSpellCatalog.STRIKER_GALE, request.skillId());
        assertEquals(21, request.row());
        assertEquals(5, request.column());
        assertEquals(0x1122_3344L, request.targetObjectId());
        assertEquals(32749, request.x());
        assertEquals(32812, request.y());
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.attackSpellRequest(new Protocol.Writer()
                        .c(Protocol.C_USE_SPELL).c(21).c(5)
                        .d(0x1122_3344L).h(32749).h(32812).c(0).bytes()));
    }

    @Test void soulOfFlameUsesExactNativeCase174SelectorOnlyEnvelope() {
        byte[] nativeRequest = new Protocol.Writer().c(Protocol.C_USE_SPELL)
                .c(21).c(6).bytes();
        assertArrayEquals(new byte[] {0x14, 0x15, 0x06}, nativeRequest);
        Protocol.SpellSelector request =
                Protocol.selectorOnlySpellRequest(nativeRequest);
        assertEquals(BattleSpellCatalog.SOUL_OF_FLAME, request.skillId());
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.selectorOnlySpellRequest(new Protocol.Writer()
                        .c(Protocol.C_USE_SPELL).c(21).c(6).d(15).bytes()));

        BattleSpellCatalog.PhysicalProcBuffSpell spell =
                BattleSpellCatalog.physicalProcBuffSpell(
                        BattleSpellCatalog.SOUL_OF_FLAME);
        assertEquals(21, spell.row());
        assertEquals(6, spell.column());
        assertEquals(19, spell.actionId());
        assertEquals(5833, spell.effectGfx());
        assertEquals(30, spell.mpCost());
        assertEquals(300, spell.reuseDelayMillis());
        assertEquals(64, spell.durationSeconds());
        assertEquals(BattleSpellCatalog.ATTRIBUTE_FIRE, spell.attribute());
        assertEquals(2, spell.sourceType());
        assertEquals(0, spell.procChance());
        assertEquals(1.5D, spell.damageMultiplier());
        assertEquals(64, spell.nativeId());
        assertEquals(InventoryCatalog.SPIRIT_CRYSTAL, spell.itemConsumeId());
        assertEquals(3, spell.itemConsumeCount());
        assertEquals(30, spell.effectiveMpCost(18, 0));
        assertEquals(28, spell.effectiveMpCost(18, 2));
    }

    @Test
    void allThreeNativeAttackProducersUseTheSameTargetCoordinateEnvelope() {
        for (int opcode : List.of(Protocol.C_ATTACK, Protocol.C_FAR_ATTACK,
                Protocol.C_ALT_ATTACK)) {
            Protocol.AttackRequest request = Protocol.attackRequest(
                    new Protocol.Writer().c(opcode).d(0x1122_3344L)
                            .h(32730).h(32798).bytes(), opcode);
            assertEquals(new Protocol.AttackRequest(0x1122_3344L, 32730, 32798), request);
        }
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.attackRequest(new Protocol.Writer()
                        .c(Protocol.C_ALT_ATTACK).d(0).h(1).h(2).bytes(),
                        Protocol.C_ALT_ATTACK));
    }

    @Test
    void physicalSelectedClientAttackCaptureParsesItsExactTargetAndCoordinates() {
        byte[] captured = {
                0x17, 0x02, 0x00, 0x00, 0x00,
                (byte) 0xE5, 0x7F, 0x21, (byte) 0x80
        };
        Protocol.AttackRequest request = Protocol.attackRequest(captured, Protocol.C_ATTACK);
        assertEquals(new Protocol.AttackRequest(2, 32741, 32801), request);

        assertThrows(IllegalArgumentException.class,
                () -> Protocol.attackRequest(new Protocol.Writer()
                        .c(Protocol.C_ATTACK).d(2).h(32741).h(32801).c(0).bytes(),
                        Protocol.C_ATTACK));
    }

    @Test
    void nativeMoveCarriesSourceTileAndDerivesExactlyOneAdjacentDestination() {
        int sourceX = 32729;
        int sourceY = 32797;
        int[][] expected = {
                {32729, 32796}, {32730, 32796}, {32730, 32797}, {32730, 32798},
                {32729, 32798}, {32728, 32798}, {32728, 32797}, {32728, 32796}
        };
        for (int heading = 0; heading < expected.length; heading++) {
            Protocol.MoveRequest request = Protocol.moveRequest(new Protocol.Writer()
                    .c(Protocol.C_MOVE).h(sourceX).h(sourceY).c(heading).bytes());
            assertEquals(sourceX, request.sourceX());
            assertEquals(sourceY, request.sourceY());
            assertEquals(heading, request.heading());
            assertEquals(expected[heading][0], request.x());
            assertEquals(expected[heading][1], request.y());
        }
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.moveRequest(new Protocol.Writer()
                        .c(Protocol.C_ATTACK).h(sourceX).h(sourceY).c(3).bytes()));
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.moveRequest(new Protocol.Writer()
                        .c(Protocol.C_MOVE).h(sourceX).h(sourceY).c(8).bytes()));
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.moveRequest(new Protocol.Writer()
                        .c(Protocol.C_MOVE).h(sourceX).h(sourceY).c(3).c(0).bytes()));
    }

    @Test
    void observerBoxUsesNativeHalfOpenRectangleLayout() throws Exception {
        Protocol.Reader packet = new Protocol.Reader(
                Protocol.observerBox(32704, 32768, 32768, 32832));

        assertEquals(Protocol.S_LITO_OBSERVER_BOX, packet.c());
        assertEquals(32704, packet.h());
        assertEquals(32768, packet.h());
        assertEquals(32768, packet.h());
        assertEquals(32832, packet.h());
        assertEquals(0, packet.remaining());
    }

    @Test
    void observerBoxRejectsEmptyOrInvertedBounds() {
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.observerBox(100, 100, 100, 101));
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.observerBox(100, 100, 101, 99));
    }

    @Test
    void minimapPlayerUsesNativeDhhhccContract() {
        Protocol.Reader packet = new Protocol.Reader(
                Protocol.minimapPlayer(0x12345678L, 32730, 32798, true));

        assertEquals(Protocol.S_LITO_MINIMAP, packet.c());
        assertEquals(0x12345678L, packet.d());
        assertEquals(0, packet.h());
        assertEquals(32730, packet.h());
        assertEquals(32798, packet.h());
        assertEquals(1, packet.c());
        assertEquals(1, packet.c());
        assertEquals(0, packet.remaining());
    }

    @Test
    void raceHudUsesAllFourNativeSubtypeLayouts() {
        Protocol.Reader roster = new Protocol.Reader(Protocol.raceRoster(1, List.of(
                new Protocol.RaceEntry(0, "Red"),
                new Protocol.RaceEntry(1, "Blue"))));
        assertEquals(Protocol.S_LITO_RACE_INFO, roster.c());
        assertEquals(1, roster.c());
        assertEquals(2, roster.h());
        assertEquals(1, roster.h());
        assertEquals(0, roster.c());
        assertEquals("Red", roster.s());
        assertEquals(1, roster.c());
        assertEquals("Blue", roster.s());
        assertEquals(0, roster.remaining());

        Protocol.Reader values = new Protocol.Reader(Protocol.raceValues(4, 1));
        assertEquals(Protocol.S_LITO_RACE_INFO, values.c());
        assertEquals(2, values.c());
        assertEquals(4, values.h()); // native four-lap race maximum
        assertEquals(1, values.h()); // first lap at race bootstrap
        assertEquals(0, values.remaining());

        Protocol.Reader timer = new Protocol.Reader(Protocol.raceTimerStart());
        assertEquals(Protocol.S_LITO_RACE_INFO, timer.c());
        assertEquals(0, timer.c());
        assertEquals(0, timer.remaining());

        Protocol.Reader time = new Protocol.Reader(Protocol.raceTime("Blue", 65_432));
        assertEquals(Protocol.S_LITO_RACE_INFO, time.c());
        assertEquals(3, time.c());
        assertEquals("Blue", time.s());
        assertEquals(65_432, time.d());
        assertEquals(0, time.remaining());
    }

    @Test
    void raceRosterUsesFFFFForObserverWithoutHighlightingACompetitor() {
        Protocol.Reader roster = new Protocol.Reader(Protocol.raceRoster(-1,
                List.of(new Protocol.RaceEntry(0, "Runner"))));
        assertEquals(Protocol.S_LITO_RACE_INFO, roster.c());
        assertEquals(1, roster.c());
        assertEquals(1, roster.h());
        assertEquals(0xFFFF, roster.h());
    }

    @Test
    void litoResetIsTheNativeOpcodeOnlyFrame() {
        Protocol.Reader packet = new Protocol.Reader(Protocol.litoReset());

        assertEquals(Protocol.S_LITO_RESET, packet.c());
        assertEquals(0, packet.remaining());
    }

    @Test
    void movementLockHandshakeUsesOpcodeOnlyFrames() {
        Protocol.Reader challenge = new Protocol.Reader(Protocol.movementLock());
        assertEquals(Protocol.S_BLINK, challenge.c());
        assertEquals(0, challenge.remaining());

        Protocol.movementUnlock(new Protocol.Writer().c(Protocol.C_BLINK).bytes());
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.movementUnlock(new Protocol.Writer()
                        .c(Protocol.C_BLINK).c(0).bytes()));
    }

    @Test
    void portalHandshakeUsesNativeLayouts() {
        assertEquals(new Protocol.EnterPortalRequest(32729, 32797),
                Protocol.enterPortalRequest(new Protocol.Writer()
                        .c(Protocol.C_ENTER_PORTAL).h(32729).h(32797).bytes()));

        Protocol.TeleportRequest implicitMap = Protocol.teleportRequest(
                new Protocol.Writer().c(Protocol.C_TELEPORT).h(32730).h(32798).bytes());
        assertEquals(new Protocol.TeleportRequest(false, -1, 32730, 32798), implicitMap);

        Protocol.TeleportRequest explicitMap = Protocol.teleportRequest(
                new Protocol.Writer().c(Protocol.C_TELEPORT)
                        .h(509).h(32731).h(32799).bytes());
        assertEquals(new Protocol.TeleportRequest(true, 509, 32731, 32799), explicitMap);

        Protocol.Reader challenge = new Protocol.Reader(Protocol.requestSummon(0x1122_3344L));
        assertEquals(Protocol.S_REQUEST_SUMMON, challenge.c());
        assertEquals(0, challenge.c());
        assertEquals(0x40, challenge.c());
        assertEquals(0x1122_3344L, challenge.d());
        assertEquals(0, challenge.remaining());

        assertEquals(0xA5, Protocol.returnSummon(new Protocol.Writer()
                .c(Protocol.C_RETURN_SUMMON).c(0xA5).bytes()));

        Protocol.Reader portal = new Protocol.Reader(Protocol.portal(509, 32731, 32799));
        assertEquals(Protocol.S_PORTAL, portal.c());
        assertEquals(509, portal.h());
        assertEquals(32731, portal.h());
        assertEquals(32799, portal.h());
        assertEquals(0, portal.remaining());

        Protocol.gotoPortal(new Protocol.Writer().c(Protocol.C_GOTO_PORTAL).bytes());
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.gotoPortal(new Protocol.Writer()
                        .c(Protocol.C_GOTO_PORTAL).c(0).bytes()));
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.enterPortalRequest(new Protocol.Writer()
                        .c(Protocol.C_ENTER_PORTAL).h(32729).bytes()));
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.enterPortalRequest(new Protocol.Writer()
                        .c(Protocol.C_ENTER_PORTAL).h(32729).h(32797).c(0).bytes()));
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.teleportRequest(new Protocol.Writer()
                        .c(Protocol.C_TELEPORT).h(1).bytes()));
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.returnSummon(new Protocol.Writer()
                        .c(Protocol.C_RETURN_SUMMON).bytes()));
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.returnSummon(new Protocol.Writer()
                        .c(Protocol.C_RETURN_SUMMON).c(0).c(0).bytes()));
    }

    @Test
    void teleportUserUsesExactBoundedNameEnvelope() {
        assertEquals("TestMage", Protocol.teleportUserRequest(new Protocol.Writer()
                .c(Protocol.C_TELEPORT_USER).s("TestMage").bytes()));
        assertEquals("Test Mage", Protocol.teleportUserRequest(new Protocol.Writer()
                .c(Protocol.C_TELEPORT_USER).s("Test Mage").bytes()));

        assertThrows(IllegalArgumentException.class,
                () -> Protocol.teleportUserRequest(new Protocol.Writer()
                        .c(Protocol.C_TELEPORT_USER).s("").bytes()));
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.teleportUserRequest(new Protocol.Writer()
                        .c(Protocol.C_TELEPORT_USER).s(" TestMage").bytes()));
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.teleportUserRequest(new Protocol.Writer()
                        .c(Protocol.C_TELEPORT_USER).s("Test\nMage").bytes()));
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.teleportUserRequest(new Protocol.Writer()
                        .c(Protocol.C_TELEPORT_USER).s("X".repeat(128)).bytes()));
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.teleportUserRequest(new Protocol.Writer()
                        .c(Protocol.C_TELEPORT_USER).s("TestMage").c(0).bytes()));
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.teleportUserRequest(new Protocol.Writer()
                        .c(Protocol.C_TELEPORT).s("TestMage").bytes()));
    }

    @Test
    void monitorPingEchoesTheNativeServerTargetByte() {
        assertEquals(0, Protocol.monitorPingRequest(
                new Protocol.Writer().c(Protocol.C_MONITOR_CONTROL).c(0).bytes()));

        Protocol.Reader response = new Protocol.Reader(Protocol.ping(0));
        assertEquals(Protocol.S_PING, response.c());
        assertEquals(0, response.c());
        assertEquals(0, response.remaining());

        assertThrows(IllegalArgumentException.class,
                () -> Protocol.monitorPingRequest(new Protocol.Writer()
                        .c(Protocol.C_MONITOR_CONTROL).c(1).bytes()));
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.monitorPingRequest(new Protocol.Writer()
                        .c(Protocol.C_MONITOR_CONTROL).c(0).c(0).bytes()));
        assertThrows(IllegalArgumentException.class, () -> Protocol.ping(5));
    }

    @Test
    void classicStatusDeltaPacketsUseTheExactNativeLayouts() {
        Protocol.Reader mana = new Protocol.Reader(Protocol.manaPoint(7, 10));
        assertEquals(Protocol.S_MANA_POINT, mana.c());
        assertEquals(7, mana.h());
        assertEquals(10, mana.h());
        assertEquals(0, mana.remaining());

        Protocol.Reader abilities = new Protocol.Reader(
                Protocol.abilityScores(13, 10, 11, 9, 8, 12, 2));
        assertEquals(Protocol.S_ABILITY_SCORES, abilities.c());
        for (int expected : new int[] {
                13, 8, 12, 11, 10, 9, Protocol.nativeWeightGauge(2)}) {
            assertEquals(expected, abilities.c());
        }
        assertEquals(0, abilities.remaining());

        Protocol.Reader experience = new Protocol.Reader(Protocol.experience(52, 123_456));
        assertEquals(Protocol.S_EXP, experience.c());
        assertEquals(52, experience.c());
        assertEquals(123_456, experience.d());
        assertEquals(0, experience.remaining());

        Protocol.Reader armor = new Protocol.Reader(Protocol.armorClass(-5, 1, -2, 3, -4));
        assertEquals(Protocol.S_AC, armor.c());
        assertEquals(0xFB, armor.c());
        assertEquals(1, armor.c());
        assertEquals(0xFE, armor.c());
        assertEquals(3, armor.c());
        assertEquals(0xFC, armor.c());
        assertEquals(0, armor.remaining());

        assertArrayEquals(new byte[] {0x56, 0x00, 0x00},
                Protocol.magicStatus(0, 0));
        assertArrayEquals(new byte[] {0x56, (byte) 0xFF, 0x7F},
                Protocol.magicStatus(-1, 127));

        assertArrayEquals(new byte[] {
                        0x4F, 0x44, 0x33, 0x22, 0x11,
                        'T', 'e', 's', 't', 'K', 'n', 'i', 'g', 'h', 't', 0
                }, Protocol.master(0x1122_3344L, "TestKnight"));

        assertThrows(IllegalArgumentException.class, () -> Protocol.manaPoint(11, 10));
        assertThrows(IllegalArgumentException.class, () -> Protocol.experience(0, 0));
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.armorClass(-129, 0, 0, 0, 0));
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.magicStatus(-129, 0));
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.magicStatus(0, 128));
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.master(0, "TestKnight"));
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.master(1, "bad\nname"));
    }

    @Test
    void arenaWhoMessageUsesNativeMessageCode51Envelope() {
        Protocol.Reader packet = new Protocol.Reader(Protocol.messageCode(0x51, "3"));

        assertEquals(Protocol.S_MESSAGE_CODE, packet.c());
        assertEquals(0x51, packet.h());
        assertEquals(1, packet.c());
        assertEquals("3", packet.s());
        assertEquals(0, packet.remaining());
    }

    @Test
    void systemMessageUsesExactNativeTypeAndStringLayout() {
        Protocol.Reader packet = new Protocol.Reader(
                Protocol.systemMessage(9, "Team 1 wins"));

        assertEquals(Protocol.S_MESSAGE, packet.c());
        assertEquals(9, packet.c());
        assertEquals("Team 1 wins", packet.s());
        assertEquals(0, packet.remaining());

        assertThrows(IllegalArgumentException.class,
                () -> Protocol.systemMessage(-1, "bad type"));
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.systemMessage(9, ""));
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.systemMessage(9, "bad\nmessage"));
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.systemMessage(9, "x".repeat(256)));
    }

    @Test
    void warningCodeUsesExactNativeHcAndRepeatedStringLayout() {
        Protocol.Reader packet = new Protocol.Reader(
                Protocol.warningCode(166, "\\f2Team 1 wins"));

        assertEquals(Protocol.S_WARNING_CODE, packet.c());
        assertEquals(166, packet.h());
        assertEquals(1, packet.c());
        assertEquals("\\f2Team 1 wins", packet.s());
        assertEquals(0, packet.remaining());

        assertThrows(IllegalArgumentException.class,
                () -> Protocol.warningCode(0x8000));
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.warningCode(166, new String[11]));
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.warningCode(166, "bad\nargument"));
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.warningCode(166, "x".repeat(256)));
    }

    @Test
    void litoParticipantBandWarningsCarryServerOwnedPerForceCounts() {
        Protocol.Reader minimum = new Protocol.Reader(
                Protocol.litoForceMinimumWarning(2));
        assertEquals(Protocol.S_WARNING_CODE, minimum.c());
        assertEquals(Protocol.LITO_FORCE_MINIMUM_PARTICIPANTS_WARNING, minimum.h());
        assertEquals(1, minimum.c());
        assertEquals("2", minimum.s());
        assertEquals(0, minimum.remaining());

        Protocol.Reader upperBound = new Protocol.Reader(
                Protocol.litoForceUpperBoundWarning(30));
        assertEquals(Protocol.S_WARNING_CODE, upperBound.c());
        assertEquals(Protocol.LITO_FORCE_UPPER_BOUND_WARNING, upperBound.h());
        assertEquals(1, upperBound.c());
        assertEquals("30", upperBound.s());
        assertEquals(0, upperBound.remaining());

        assertThrows(IllegalArgumentException.class,
                () -> Protocol.litoForceMinimumWarning(0));
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.litoForceUpperBoundWarning(-1));
    }

    @Test
    void litoWholeBattleWarningsCarryServerOwnedParticipantCounts() {
        Protocol.Reader minimum = new Protocol.Reader(
                Protocol.litoBattleMinimumWarning(2));
        assertEquals(Protocol.S_WARNING_CODE, minimum.c());
        assertEquals(Protocol.LITO_BATTLE_MINIMUM_PARTICIPANTS_WARNING, minimum.h());
        assertEquals(1, minimum.c());
        assertEquals("2", minimum.s());
        assertEquals(0, minimum.remaining());

        Protocol.Reader upperBound = new Protocol.Reader(
                Protocol.litoBattleUpperBoundWarning(61));
        assertEquals(Protocol.S_WARNING_CODE, upperBound.c());
        assertEquals(Protocol.LITO_BATTLE_UPPER_BOUND_WARNING, upperBound.h());
        assertEquals(1, upperBound.c());
        assertEquals("61", upperBound.s());
        assertEquals(0, upperBound.remaining());

        assertThrows(IllegalArgumentException.class,
                () -> Protocol.litoBattleMinimumWarning(0));
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.litoBattleUpperBoundWarning(-1));
    }

    @Test
    void weaponBreakUsesExactNativeCase26TargetCoordinateEnvelope() {
        byte[] nativeRequest = new Protocol.Writer().c(Protocol.C_USE_SPELL)
                .c(3).c(2).d(0x1122_3344L).h(32749).h(32812).bytes();
        assertArrayEquals(new byte[] {
                0x14, 0x03, 0x02, 0x44, 0x33, 0x22, 0x11,
                (byte) 0xED, 0x7F, 0x2C, (byte) 0x80}, nativeRequest);
        Protocol.AttackSpellRequest request =
                Protocol.attackSpellRequest(nativeRequest);
        assertEquals(BattleSpellCatalog.WEAPON_BREAK, request.skillId());
        assertEquals(3, request.row());
        assertEquals(2, request.column());
        assertEquals(0x1122_3344L, request.targetObjectId());
        assertEquals(32749, request.x());
        assertEquals(32812, request.y());
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.attackSpellRequest(java.util.Arrays.copyOf(
                        nativeRequest, nativeRequest.length - 1)));
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.attackSpellRequest(java.util.Arrays.copyOf(
                        nativeRequest, nativeRequest.length + 1)));
    }

    private static int occurrences(String text, String needle) {
        int count = 0;
        for (int at = 0; (at = text.indexOf(needle, at)) >= 0;
             at += needle.length()) count++;
        return count;
    }
}
