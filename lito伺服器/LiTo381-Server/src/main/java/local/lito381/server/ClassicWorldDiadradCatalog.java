package local.lito381.server;

import java.util.List;
import java.util.Map;
import java.util.Set;

/** Hash-locked staged Diad fortress / Great Cavern service core maps320/330/400/401. */
final class ClassicWorldDiadradCatalog {
    static final List<Integer> MAP_IDS = List.of(320, 330, 400, 401);
    static final Set<Integer> EXCLUDED_CUSTOM_SPAWN_IDS = Set.of(1078, 1080);
    static final String ROUTES_SOURCE_SHA256 =
            "FAE1524A40D787058F22D343B0E7F392B2E56906D693358281C7DCE2CA9D1D7B";
    static final String ENABLED_ROUTES_SOURCE_SHA256 =
            "32A65E6DEE29EB5B8437582A34D207D6AD50B696F5D48F0F0CEF55DD2768B7FF";
    static final String FLAGS_SOURCE_SHA256 =
            "E89EFBF388C92348656020BA53BC2B37F0D3DCABB9AC6BAFF97F4F5A3A3F4F52";
    static final String GETBACK_SOURCE_SHA256 =
            "A161EC9970F22D5E5454B994C3499BA15ADA12FA281268FA957087F0495BAAB7";
    static final String NPC_ACTION_SOURCE_SHA256 =
            "6563773C04169C8CEFE891BCDDE9BA9C526A1F13D2D12060675622EB96A89F97";
    static final String CRAFTING_SLICE_SOURCE_SHA256 =
            "D23559914DE093B6EA2B8D6BE9EAE6A36D027BCE6B7C56F6D9F068FEC0AC96DE";
    static final String STORY_SLICE_SOURCE_SHA256 =
            "51F391130AF78D2007021231DAF6509AED85F03E32E7EE9170D6AF782FCDA259";

    record MapFlags(int mapId, String locationName,
                    int startX, int endX, int startY, int endY,
                    boolean escapable, boolean usableItem, boolean usableSkill,
                    boolean teleportable, boolean takePets, boolean recallPets) {}

    record SourceContract(int rows, int population, String sourceSha256,
                          String resourceSha256) {}

    static final Map<Integer, String> MAP_SOURCE_SHA256 = Map.of(
            320, "4938E73F131991E7302BADEF682DCB44BB71BD027296412C6DF1E4AF5F57BD08",
            330, "EF274A37FECF4B464FF31B99F6500CDD7249E6E2F26FD8EB10D5FC329A4EBA65",
            400, "ADE35C07F5C84AC1D01B03F83B249509957219D00012A2957D55CB9BAE3C1432",
            401, "E93FBA2AFAB3963013C1723C6EE6E180B9B070ABD67FAB60240448BB3BD85FE7");
    static final Map<Integer, String> MAP_RESOURCE_SHA256 = Map.of(
            320, "9A8E40E1B4FD0CB556C97E003BCF636DC374D64C9744732037E4EEC8E6CCB536",
            330, "90914DC36A06D6413A908F2FA2B0442070994C6A88CC0B5504D83DD63A60018F",
            400, "EDF37E8085CBCDADC2D9D4011E3123CDE00AB035FCD894CE968D4ABA2724A09E",
            401, "F59D210AC9182AF4450D97F173A6E4C00D9C6DAE97101225B43A64A7715DBE93");
    static final Map<Integer, SourceContract> NPC_CONTRACTS = Map.of(
            320, contract(3, 0,
                    "DA32FB6C01970A195A161079C2BDA803284BA0BEBD2D75756C061B9D4E5B8935",
                    "91B04B3EF1F83C3516F9E85A9DEB1527C0989896AC5BB8CAB178BC42B11BD99E"),
            330, contract(0, 0,
                    "8263164069B03411156DCA1D6B564F777EED14697D36402D02A1086131AEE8BF",
                    "B4CE93C11993EA488D005AE2C98A1000890D1DE842E9100247897A3E222AB47E"),
            400, contract(5, 0,
                    "C6858C7BBA77BD873054534CDA4110F8FBD085934D5DBD62D29281E538E16E89",
                    "2DD9E86860E5D472B7ACFD72283FAABC439DA54495EBFEFF8B8FE742C68829B2"),
            401, contract(0, 0,
                    "8263164069B03411156DCA1D6B564F777EED14697D36402D02A1086131AEE8BF",
                    "B2D0B358590BFAF4874071BE7ADB677985D1183B4D7D5222C292EB29A1CD8FFB"));
    static final Map<Integer, SourceContract> MONSTER_CONTRACTS = Map.of(
            320, contract(12, 378,
                    "BBF68E0AB6B6486822C3E1E1D1467725A50920B4BF9AAC980A650ADDB4D6E7AF",
                    "E242259339B5B761409C4A174F3EA2D5FAB2F665BB6D073BF80511172D26E7BE"),
            330, contract(0, 0,
                    "219255B3830F63B32C0D35542B91DF47B5532113719CB79075F5CD233B6DE5E7",
                    "1C1D7A77F7FF9D14BA9BF0F9B0DB4009E2C7E9A4DEDA165E1F6838970AC461DB"),
            400, contract(181, 997,
                    "12CCA04A5447C6A74B5977DC88BA3418B5CF2E7FB608BEF0D268766A51773A4B",
                    "1BE39359E65CD7698BD31C0D8D4D99600E498994F22EA32DE8EC814214571C4D"),
            401, contract(0, 0,
                    "219255B3830F63B32C0D35542B91DF47B5532113719CB79075F5CD233B6DE5E7",
                    "A43FD69A0883491743FC591925DADF0C819425FB52967BFF0FEE580873C58EFE"));
    static final Map<Integer, SourceContract> DROP_CONTRACTS = Map.of(
            320, contract(46, 32,
                    "30D0F973FF5DF478A1FAC95D8A3075B459AB24683BA09AB681D8DA4EEE7A94EA",
                    "CAF70AD7A382F8C2BD22EB1EE18B68548BC3ED8A45E0BA1C41C3FBBF530F3870"),
            330, contract(0, 0,
                    "3FE93EDCC0A0A171C6BB8ADF328460441832AF757DA26B9DAB1E8C8A2963C828",
                    "4494CD89C8E43F9A75D5AE2CDA63F48AA5345272C263F11A22F617F563101FF9"),
            400, contract(388, 139,
                    "9D61D2E88D881BC86A48E222FB860F19D4F4475C2B6B17B5B9364BDCB293554F",
                    "3D78861C1642E2AA5BEFE06281EEDBF53946C6FB8A5EE34ADAA236DEF7B96387"),
            401, contract(0, 0,
                    "3FE93EDCC0A0A171C6BB8ADF328460441832AF757DA26B9DAB1E8C8A2963C828",
                    "D2F6FC4B0A0902A0AD94B76B02B97E29C12B68A78E31CB2E3BFA7B923249559A"));
    static final Map<Integer, MapFlags> FLAGS = Map.of(
            320, flags(320, "Diad Fortress", 32640, 33087, 32768, 33087, false),
            330, flags(330, "Mineral Cave", 32640, 32831, 32768, 33023, true),
            400, flags(400, "Great Cavern Recluse Village", 32448, 33087, 32576, 33087, false),
            401, flags(401, "Great Cavern Recluse Area", 32704, 32895, 32768, 32895, false));

    static final int CONSUL_NPC_ID = 50127;
    static final int ENO_NPC_ID = 70707;
    static final int ADELIO_NPC_ID = 71125;
    static final int ZIDAR_NPC_ID = 60035;
    static final int CL_ALCHEMIST_NPC_ID = 71113;
    static final int SI_ALCHEMIST_NPC_ID = 71114;
    static final int KALBASS_NPC_ID = 81120;
    static final int DSPY_NPC_ID = 80012;
    static final Set<Integer> CRAFTER_NPC_IDS = Set.of(
            CONSUL_NPC_ID, ENO_NPC_ID, ADELIO_NPC_ID,
            CL_ALCHEMIST_NPC_ID, SI_ALCHEMIST_NPC_ID, KALBASS_NPC_ID);
    private static final Map<Integer, Set<String>> TEXT_LINKS = Map.of(
            ADELIO_NPC_ID, Set.of(
                    "adelio1", "adelio2", "adelio3", "adelio4", "adelio5",
                    "adelio7", "adelio8", "adelio9", "adelio10", "adelio11",
                    "adelio12", "adelio13", "adelio14", "adelio15"),
            CL_ALCHEMIST_NPC_ID, Set.of("clalchemist2", "clalchemist3"),
            SI_ALCHEMIST_NPC_ID, Set.of("sialchemist2", "sialchemist3"),
            KALBASS_NPC_ID, Set.of(
                    "kalbass1", "kalbass2", "kalbass3", "kalbass4", "kalbass5"),
            ZIDAR_NPC_ID, Set.of("gawlwp", "pzidar"));

    private ClassicWorldDiadradCatalog() {}

    static MapFlags flags(int mapId) {
        MapFlags result = FLAGS.get(mapId);
        if (result == null) throw new IllegalArgumentException("not a Diad map: " + mapId);
        return result;
    }

    static boolean isWarehouse(ClassicWorldNpcCatalog.StaticNpc npc) {
        return npc != null && npc.mapId() == 400 && npc.npcId() == ZIDAR_NPC_ID;
    }

    static boolean isCrafter(ClassicWorldNpcCatalog.StaticNpc npc) {
        return npc != null && CRAFTER_NPC_IDS.contains(npc.npcId())
                && (npc.mapId() == 320 || npc.mapId() == 400);
    }

    static boolean isStoryNpc(ClassicWorldNpcCatalog.StaticNpc npc) {
        return npc != null && npc.mapId() == 400 && npc.npcId() == DSPY_NPC_ID;
    }

    static String linkedHtml(ClassicWorldNpcCatalog.StaticNpc npc, String action) {
        if (npc == null || action == null || action.isBlank()
                || !MAP_IDS.contains(npc.mapId())) return null;
        String normalized = action.toLowerCase(java.util.Locale.ROOT);
        Set<String> links = TEXT_LINKS.get(npc.npcId());
        return links != null && links.contains(normalized) ? normalized : null;
    }

    private static SourceContract contract(
            int rows, int population, String source, String resource) {
        return new SourceContract(rows, population, source, resource);
    }

    private static MapFlags flags(int mapId, String name,
                                  int startX, int endX, int startY, int endY,
                                  boolean teleportable) {
        return new MapFlags(mapId, name, startX, endX, startY, endY,
                true, true, true, teleportable, true, true);
    }
}
