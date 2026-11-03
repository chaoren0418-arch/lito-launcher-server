package local.lito381.server;

import java.util.List;
import java.util.Map;

/** Hash-locked Abyss Lake / Elemental Grave slice and Pirate Island landing. */
final class ClassicWorldElementalGraveCatalog {
    static final int ABYSS_LAKE_MAP_ID = 420;
    static final int ELEMENTAL_GRAVE_MAP_ID = 430;
    static final int PIRATE_ISLAND_MAP_ID = 440;
    static final List<Integer> MAP_IDS = List.of(
            ABYSS_LAKE_MAP_ID, ELEMENTAL_GRAVE_MAP_ID, PIRATE_ISLAND_MAP_ID);

    static final String ROUTES_SOURCE_SHA256 =
            "9C131841A498E3A62D942661DE13EB60DFE9D74A71B3380A667CC8D6C5F61FD4";
    static final String ENABLED_ROUTES_SOURCE_SHA256 =
            "8DF57F95CDBAB1CD029200088483B065AE4FFE76BDB7EFA1BC4A4D710934C358";
    static final String FLAGS_SOURCE_SHA256 =
            "0F0C00D9920981869E53D677A31BAE028334DBEB63193F079CB269D06FEE5F06";
    static final String GETBACK_SOURCE_SHA256 =
            "2E12071BB746A1DA67E78165E93C7A2AA70340F72D9641F13E61713CB7642A19";
    static final String NPC_TELEPORT_SOURCE_SHA256 =
            "E4BF49500835C097E954874971E52E8FA58BFD26A2B63119DFE65EFE9BDC2CEC";
    static final String ENABLED_NPC_TELEPORT_SOURCE_SHA256 =
            "831C2FA892D14449C298132B21BED7F46C148A5A672C2E6E2B47189E8295ACF8";

    static final Map<Integer, String> MAP_SOURCE_SHA256 = Map.of(
            ABYSS_LAKE_MAP_ID,
            "528DA2A9EA9AB3A6523E5F129B81FACE864BB3AB2D7AAD65B1F7A20C795A73B1",
            ELEMENTAL_GRAVE_MAP_ID,
            "73EAE1315835EC1F4DC3EAAFC0D98758D053B090E8FC53198C4B15C137D4FC93",
            PIRATE_ISLAND_MAP_ID,
            "CF798709F913399C2F61FBB15926DD2E1BDACEEECBDB9F610DF1C727C281A39D");
    static final Map<Integer, String> MAP_RESOURCE_SHA256 = Map.of(
            ABYSS_LAKE_MAP_ID,
            "304E4CD6F80E22D019BF7214FF1BCC23FECCE150F1C1D1AA8B6EE7FB79C49E18",
            ELEMENTAL_GRAVE_MAP_ID,
            "4232A91DB616EDFCD08B708E415A48EB01479AC3EC8E21701ED1A6D708E2304B",
            PIRATE_ISLAND_MAP_ID,
            "8DB5F455B570948169E079239AA4B51C276A19C852AA6E1CA3225DA8522E00D0");
    static final Map<Integer, String> CLIENT_MAP_MANIFEST_SHA256 = Map.of(
            ABYSS_LAKE_MAP_ID,
            "FC11A282733CCE693D83B4CB101D882F7F929DBDDD9124750169617DB24D484E",
            ELEMENTAL_GRAVE_MAP_ID,
            "9E8CA09B4D6248ED4417C8CA80B4FC99095A2BC122AEF38A269077A98B873F00",
            PIRATE_ISLAND_MAP_ID,
            "2C292415DF0EAEBAAE5034D10B578122B11A9BF1A94FF86F09533006B5E8F588");

    record MapFlags(int mapId, String locationName,
                    int startX, int endX, int startY, int endY,
                    boolean escapable, boolean usableItem, boolean usableSkill,
                    boolean teleportable, boolean takePets, boolean recallPets) {}

    record SourceContract(int rows, int population, String sourceSha256,
                          String resourceSha256) {}

    static final Map<Integer, MapFlags> FLAGS = Map.of(
            ABYSS_LAKE_MAP_ID, new MapFlags(
                    ABYSS_LAKE_MAP_ID, "Abyss Lake",
                    32640, 32831, 32832, 33151,
                    true, true, true, true, false, false),
            ELEMENTAL_GRAVE_MAP_ID, new MapFlags(
                    ELEMENTAL_GRAVE_MAP_ID, "Elemental Grave",
                    32640, 33023, 32704, 33087,
                    true, true, true, false, true, true),
            PIRATE_ISLAND_MAP_ID, new MapFlags(
                    PIRATE_ISLAND_MAP_ID, "Pirate Island",
                    32256, 32767, 32768, 33279,
                    true, true, true, true, true, true));

    static final Map<Integer, SourceContract> NPC_CONTRACTS = Map.of(
            ABYSS_LAKE_MAP_ID, contract(1, 0,
                    "65CEE576DCA7F02C75A2B61446B0CBE84339AA283E7874C7AE99E354D453CC8F",
                    "10DA2152F8748E08839D10AFBAA7F838C2090AC455CF23C493A5A4CE536A8197"),
            ELEMENTAL_GRAVE_MAP_ID, contract(1, 0,
                    "12A7CE36E01723F6F164640EDDF6FF6D910331A1709C689FBB9390D7D6054AF0",
                    "2DD4B32173335BFD5735C98802E35F8B086C144B3B72C48B08977F319B27B47E"),
            PIRATE_ISLAND_MAP_ID, contract(2, 0,
                    "FA96377CF8C12154B5774E30EC06FE57D3B4671E9F5ABC470F14AB7DFDAA3F15",
                    "B4A07AD858FF85336A096A392DC94D2DC93E69B741DE6F93C9A61447AD4145D0"));

    static final Map<Integer, SourceContract> MONSTER_CONTRACTS = Map.of(
            ABYSS_LAKE_MAP_ID, contract(9, 69,
                    "A3A31A8585F52F600C746ACE74075087C535C8BFDAEEA7222ADED9E7DCD324FC",
                    "18004AB8650C2D9E3632F145AF145C2F808DBB550911BCAE6A53BCAE52CBD987"),
            ELEMENTAL_GRAVE_MAP_ID, contract(7, 15,
                    "CA39E35E97D6F101B049ABD64E60C14958F1239F0EE5DE3DE3A8B7620CF6DC7B",
                    "6F6F7B175811128C86B951208E404144BAB9F20566800053395D2C088C594E3D"),
            PIRATE_ISLAND_MAP_ID, contract(77, 617,
                    "41A2EB23E0EB90F3FFBE6BF5A15045082722D062D8F6660C47A415F677B24036",
                    "B6AE0F5A40EB194ED89258548B52EB484A4F894766E4229D96241E3D32ACDBA1"));

    static final Map<Integer, SourceContract> DROP_CONTRACTS = Map.of(
            ABYSS_LAKE_MAP_ID, contract(89, 44,
                    "69E5EE98B5B1E0AD9482E261165AC93782ADF1F5F5CF1908E667DE85A4E73D75",
                    "256BAF9735E9B597401CC4F52F68FB7DE27AEF41E7A8963D551C0EE64BF02461"),
            ELEMENTAL_GRAVE_MAP_ID, contract(28, 14,
                    "96FE886A672ECC4B303B5470CCF465C2FE9923875A64AD75224E7C4D8D238804",
                    "8325734B6EDD73B44B7BE8D8EABA7999E649582BD9CCC586D86626668B8C4B60"),
            PIRATE_ISLAND_MAP_ID, contract(84, 38,
                    "9B471A9D982E8AF8B3CE089D193635B34BD269B8721C34A9FBA15A61FB0E8311",
                    "D6913C719D63C213877682E32A84598CB58EF38CABDD730BEB5C5F50D240C2D5"));

    private ClassicWorldElementalGraveCatalog() {}

    static MapFlags flags(int mapId) {
        MapFlags result = FLAGS.get(mapId);
        if (result == null) {
            throw new IllegalArgumentException("not an Elemental Grave slice map: " + mapId);
        }
        return result;
    }

    private static SourceContract contract(
            int rows, int population, String source, String resource) {
        return new SourceContract(rows, population, source, resource);
    }
}
