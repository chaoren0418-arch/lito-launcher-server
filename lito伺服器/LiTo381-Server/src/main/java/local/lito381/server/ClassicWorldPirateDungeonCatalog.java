package local.lito381.server;

import java.util.List;
import java.util.Map;

/** Hash-locked Pirate Island dungeon floors 1F through 4F. */
final class ClassicWorldPirateDungeonCatalog {
    static final List<Integer> MAP_IDS = List.of(441, 442, 443, 444);

    static final String ROUTES_SOURCE_SHA256 =
            "D72B1CAC50085DEC0934D6550E9FBD408629B68C536D048AC43DB999636521E3";
    static final String FLAGS_SOURCE_SHA256 =
            "165D9BEE4AD7C18676CAEFC8E2EDFE365F276413B1C28A3B104B6B51BBEF7E80";
    static final String GETBACK_SOURCE_SHA256 =
            "6FDFA3422FD0D8AFF30696D6EBDD26D4D7DC7744A7ECA76DAAF44DAC0E811A7F";
    static final String NPC_TELEPORT_SOURCE_SHA256 =
            "D9E8654E09C1A368DC6F39B9B82D5D23DEEEB1718322FB1A5CD36D841ECF94E1";
    static final String CRAFT_SOURCE_SHA256 =
            "8CA6B0C4209C6A2BE512D6EC6A69C6297C0B1C8704E7A6E3DC239FBB23142BF8";
    static final String CRAFT_RESOURCE_SHA256 =
            "C50ECF110D4629A0E86DBFFDFDD7DAE27D50F99C055600A6269FDECFD8C2BA1C";
    static final int CRAFTER_SPAWN_ID = 1015;
    static final int CRAFTER_NPC_ID = 81121;
    static final String CRAFTER_INITIAL_HTML = "pielemental";
    static final String CRAFT_ACTION = "request crystal of soul";
    static final int CRYSTAL_SPAWN_ID = 117;
    static final int CRYSTAL_NPC_ID = 50078;
    static final String CRYSTAL_INITIAL_HTML = "picrystal";
    static final String CRYSTAL_CONFIRM_HTML = "picrystal1";
    static final String CRYSTAL_CONFIRM_ACTION = "teleportURL";

    static final Map<Integer, String> MAP_SOURCE_SHA256 = Map.of(
            441, "80AE68E389B509B02D7D81ACFC46B7215B426D717ECFA4D1E3C104DC4BF95100",
            442, "9D7FFFB588FCEA5E36C016915AE052CBDBC9276C1552EA523C8FE1EC10DD06C4",
            443, "D395A9DDF3FB519729AAB52249F96D5F034BD86AE5906CACDB55DAAAC3C314A6",
            444, "721B2E0969061CE4B2E750C36FE646DBD5B6B085FA99D0BBA27493560D6D2F50");
    static final Map<Integer, String> MAP_RESOURCE_SHA256 = Map.of(
            441, "BF24234C1229302C7A94F869011963E052C8B63B6468558E66AF2143A09C4411",
            442, "01A866224C24A0C56D19E28283D67BB73E8E6F6DD5B783F5E888E8FE4EB317E1",
            443, "5EDCCF5EA50E50C96A33599127E36024C580860821D3B85B4A727025D7BA1577",
            444, "A2E8EE329069223406E33C4B3B033C8526A612B6507BE71569F19102EAF3D729");
    static final Map<Integer, String> CLIENT_MAP_MANIFEST_SHA256 = Map.of(
            441, "D87679FADF61995F00124DDB6D3F21D4396DC3E63F9FFBDD0B072D55F851EC08",
            442, "4F67766F54E6C35756A8460D5C892C0B5643649852AE362E8EE767EE80AF3B5E",
            443, "8DDE310F6D07A158C91C443DC646D89BA32B7FC052F68C0B2DAFDA9F3AD487B6",
            444, "30C3891302605BA810DDF75C9535380CC2A994B202A8C2C0B7A0A73BD23AD14C");

    record MapFlags(int mapId, String locationName,
                    int startX, int endX, int startY, int endY,
                    boolean escapable, boolean usableItem, boolean usableSkill,
                    boolean teleportable, boolean takePets, boolean recallPets) {}

    record SourceContract(int rows, int population, String sourceSha256,
                          String resourceSha256) {}

    static final Map<Integer, MapFlags> FLAGS = Map.of(
            441, flags(441, "Pirate Island Dungeon 1F",
                    32640, 32895, 32704, 32959, true),
            442, flags(442, "Pirate Island Dungeon 2F",
                    32704, 32895, 32704, 32895, true),
            443, flags(443, "Pirate Island Dungeon 3F",
                    32640, 32895, 32640, 32959, true),
            444, flags(444, "Pirate Island Dungeon 4F",
                    32704, 32767, 32768, 32831, false));

    static final Map<Integer, SourceContract> NPC_CONTRACTS = Map.of(
            441, contract(0, 0,
                    "8263164069B03411156DCA1D6B564F777EED14697D36402D02A1086131AEE8BF",
                    "CB464D60E330BB14D519D03EF0438EE6CACC4B60A52AB05C811E6A28EA250150"),
            442, contract(0, 0,
                    "8263164069B03411156DCA1D6B564F777EED14697D36402D02A1086131AEE8BF",
                    "DB0C51806918974ECA6951484866D9293E12EAD412FC6274BC1221DB4436D9C1"),
            443, contract(0, 0,
                    "8263164069B03411156DCA1D6B564F777EED14697D36402D02A1086131AEE8BF",
                    "BE0C9D4DD8B9993AB0075F58C780E2D8D4CA2DA426131BE5502176B156B874B8"),
            444, contract(2, 0,
                    "D62463EDF8C62D7FBD139F4BD144431C9D69116A698737CD4FD0E292C94F3DF1",
                    "0E4781E52E82C977ECCA8D31959B5865D63A8807BC473E52565C648B2231C28B"));

    static final Map<Integer, SourceContract> MONSTER_CONTRACTS = Map.of(
            441, contract(13, 85,
                    "A8BBD7B848EDAFA7AA5E6DC27C32B585AF437F4DD45DC5FE71F63486AE942DD4",
                    "12EAE51C56A1FADE8BF05A598EAAC160507EBF3842168AF4C551339FE1FF09A4"),
            442, contract(12, 81,
                    "98BAD076805E2EA0609F1C1DAB0F3E744D3E890E5F66886DB6231AE5D76F55D3",
                    "D12E5C80EA631F1143C3AC589DB2061AAAB0595B9CE640CE9E1620E8AF1E004E"),
            443, contract(10, 85,
                    "FA0726F88CD1C9170322AAB939467CFC640B8B981896112A0153222F94704FE6",
                    "7598004704E966A9913CBB2AA946623311B57C7C104474B1B422C11C95E4C924"),
            444, contract(0, 0,
                    "219255B3830F63B32C0D35542B91DF47B5532113719CB79075F5CD233B6DE5E7",
                    "A6C2A329D14F7F12070AB23FDB460F7170304DD620747B5FE2D86098E33F2AC0"));

    static final Map<Integer, SourceContract> DROP_CONTRACTS = Map.of(
            441, contract(0, 0,
                    "3FE93EDCC0A0A171C6BB8ADF328460441832AF757DA26B9DAB1E8C8A2963C828",
                    "489C1B074CA5B3FD227597488776E1735E8BE22E165C2BD3FAF5E5E6F1041669"),
            442, contract(12, 12,
                    "3421E3D15B58326103B8F3AF16C2636E93A0EBCB171EB9037BAC7C735AFCBF69",
                    "4BCCF996860CFF7E2ED238E91C4F68508EB6E1C099C6BC1D853638FAB100BDE0"),
            443, contract(46, 39,
                    "761C7898A1DA0E48620C1BC5998FF9A4E472F6179DEE63A895773D26D62F2D93",
                    "90AFFBB1C421F3CDF78DD82FC71DBA3F39FDAE504AEF88BFA0E1C39BCBE924DE"),
            444, contract(0, 0,
                    "3FE93EDCC0A0A171C6BB8ADF328460441832AF757DA26B9DAB1E8C8A2963C828",
                    "498ED2FA5AEFC6626B397BA3F929E5D4F55CDD278E2226F3E381DEEF32CA2D2A"));

    private ClassicWorldPirateDungeonCatalog() {}

    static MapFlags flags(int mapId) {
        MapFlags result = FLAGS.get(mapId);
        if (result == null) {
            throw new IllegalArgumentException("not a Pirate Island dungeon map: " + mapId);
        }
        return result;
    }

    private static MapFlags flags(int mapId, String name,
                                  int startX, int endX, int startY, int endY,
                                  boolean teleportable) {
        return new MapFlags(mapId, name, startX, endX, startY, endY,
                true, true, true, teleportable, true, true);
    }

    private static SourceContract contract(
            int rows, int population, String source, String resource) {
        return new SourceContract(rows, population, source, resource);
    }
}
