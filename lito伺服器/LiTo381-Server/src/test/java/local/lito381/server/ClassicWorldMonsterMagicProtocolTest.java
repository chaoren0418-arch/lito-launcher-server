package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.HexFormat;
import java.util.List;
import org.junit.jupiter.api.Test;

final class ClassicWorldMonsterMagicProtocolTest {
    @Test
    void iceConeUsesExactSelectedClientExtendedAttackLayout() {
        LocalResources.assumeNativeCatalogPins();
        ClassicWorldMonsterMagicSkillCatalog.Definition ice =
                ClassicWorldMonsterMagicSkillCatalog.forNpc(45224);
        byte[] packet = Protocol.attackSpell(
                ice.actionId(), 0x6500_1593L, 16, 10, 6,
                4_500_001L, ice.effectGfx(),
                33953, 32437, 33952, 32437);
        assertEquals(
                "231293150065100000000A0621AA4400DA2D00A184B57E"
                        + "A084B57E0000",
                HexFormat.of().withUpperCase().formatHex(packet));
    }

    @Test
    void tornadoUsesExactSelectedClientCasterCenteredAttackManyLayout() {
        ClassicWorldMonsterMagicSkillCatalog.Definition tornado =
                ClassicWorldMonsterMagicSkillCatalog.forNpc(45265);
        byte[] packet = Protocol.attackMany(
                tornado.actionId(), 0x6500_0EA6L, 33379, 32211,
                0, 4_500_001L, tornado.effectGfx(), 0,
                List.of(new Protocol.AttackManyTarget(16, true)));
        assertEquals(
                "3912A60E00656382D37D0021AA4400F60200000001001000000020",
                HexFormat.of().withUpperCase().formatHex(packet));
    }

    @Test
    void ckewCustomMagicUsesNativeNoDirectionAndTargetCenteredTypeBytes() {
        ClassicWorldMonsterMagicSkillCatalog.Definition wave =
                ClassicWorldMonsterMagicSkillCatalog.forNpcAll(87015).get(0);
        byte[] casterCentered = Protocol.attackMany(
                wave.actionId(), 0x6600_0800L, 32790, 32773,
                2, 4_500_002L, wave.effectGfx(), 0,
                List.of(new Protocol.AttackManyTarget(16, true)));
        assertEquals(
                "391200080066168005800222AA4400CE0900000001001000000020",
                HexFormat.of().withUpperCase().formatHex(casterCentered));

        ClassicWorldMonsterMagicSkillCatalog.Definition arrow =
                ClassicWorldMonsterMagicSkillCatalog.forNpcAll(87015).get(1);
        byte[] targetCentered = Protocol.attackMany(
                arrow.actionId(), 0x6600_0800L, 32790, 32773,
                2, 4_500_002L, arrow.effectGfx(), 8,
                List.of(new Protocol.AttackManyTarget(16, true)));
        assertEquals(
                "391200080066168005800222AA4400F20908000001001000000020",
                HexFormat.of().withUpperCase().formatHex(targetCentered));
    }
}
