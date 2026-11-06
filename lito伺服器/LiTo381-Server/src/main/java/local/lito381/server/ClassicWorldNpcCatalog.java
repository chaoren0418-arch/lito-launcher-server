package local.lito381.server;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.zip.GZIPInputStream;

/** Retained static NPC spawn rows for the independent classic world. */
final class ClassicWorldNpcCatalog {
    static final int MAP0_ROW_COUNT = 6;
    static final String MAP0_SOURCE_SHA256 =
            "7231F0A41723F1CC50744DB84E67F86E1CE2DA20D173C6355FA304F349216C1D";
    static final String MAP0_RESOURCE_SHA256 =
            "B36DF9BF0C9235BB25E0DC4AF7DA8AB66F544003E05B4DA1F8C34821185CD3B0";
    static final int MAP1_ROW_COUNT = 1;
    static final String MAP1_SOURCE_SHA256 =
            "F6589ED493FC167B1F3327A35331686209CCA33B6E382617589C8FCC01AD7D08";
    static final String MAP1_RESOURCE_SHA256 =
            "3B325FA94F47D38C755E5DF709540AC6F652752A1C18B15FECB9A761345FB3CB";
    static final int MAP2_ROW_COUNT = 29;
    static final String MAP2_SOURCE_SHA256 =
            "796EEB37E5225796504355E8BDD57FA8DD9DBDC98F8ACD94F0A5821D4A33FD75";
    static final String MAP2_RESOURCE_SHA256 =
            "C92D35460B0286BA0C684BCCA94EAC2ED8EBAB55C7A4A14A0A70309FF41829D6";
    static final int MAP14_ROW_COUNT = 0;
    static final String MAP14_SOURCE_SHA256 =
            "8263164069B03411156DCA1D6B564F777EED14697D36402D02A1086131AEE8BF";
    static final String MAP14_RESOURCE_SHA256 =
            "5541B0A0BD523A9F3FFFC206148AC90AD4B2F90CB83337F2D89EA038F4B10E56";
    static final int MAP13_ROW_COUNT = 1;
    static final String MAP13_SOURCE_SHA256 =
            "30C4CBF48877E3A4F1D2D2BB43F4E0CCB3243775F5FF531FA8601E8711EDD1D1";
    static final String MAP13_RESOURCE_SHA256 =
            "919A89E72062EED874CEDE42E99FEA438DED6382B534D30A07C526A030485C4E";
    static final int MAP237_ROW_COUNT = 1;
    static final String MAP237_SOURCE_SHA256 =
            "EA81E60258B40C373471505F12F2BBDE49E13904F7A595899141080DC44C8AC3";
    static final String MAP237_RESOURCE_SHA256 =
            "3E4FD986C5FF863F0FCC54C80F4E6682E9723B7A9CB3697839ADE59297C3EEEF";
    static final int MAP3_ROW_COUNT = 3;
    static final String MAP3_SOURCE_SHA256 =
            "A1C9B0D5FCA381CDC904C1AC6299BB354B6D6EBE34A3BA4EE7765D70F6173109";
    static final String MAP3_RESOURCE_SHA256 =
            "3EC7B4A0D9DE4E342C5914C36D510810E2CF83ACB69B60DAA4DCFF33F83B26F8";
    static final int MAP5_ROW_COUNT = 0;
    static final int MAP6_ROW_COUNT = 0;
    static final String MAP5_SOURCE_SHA256 =
            "8263164069B03411156DCA1D6B564F777EED14697D36402D02A1086131AEE8BF";
    static final String MAP6_SOURCE_SHA256 = MAP5_SOURCE_SHA256;
    static final String MAP5_RESOURCE_SHA256 =
            "4ADD99B1B66F646E49E9B611A395F6CCBC2A66FC444038FFA52414E218BC4FA1";
    static final String MAP6_RESOURCE_SHA256 =
            "1CDB80B3B7A5B3B6A62C57B3F2B3A78BC477EE4ED016A98EABA9830249C72175";
    static final int MAP70_ROW_COUNT = 5;
    static final int MAP83_ROW_COUNT = 0;
    static final int MAP84_ROW_COUNT = 0;
    static final String MAP70_SOURCE_SHA256 =
            "6EF28188ED502BFB2E8E635DCC6A9FD71FACF6A99BD3B861D459D0095767AB56";
    static final String MAP83_SOURCE_SHA256 = MAP5_SOURCE_SHA256;
    static final String MAP84_SOURCE_SHA256 = MAP5_SOURCE_SHA256;
    static final String MAP70_RESOURCE_SHA256 =
            "0301158AACDBC6D069F968744AFCC738B89602BBCF92C95C5F6CFED7C54597FA";
    static final String MAP83_RESOURCE_SHA256 =
            "942F4CD98CC2EF17F73C3F3607BE59BA4BA69CE5A5B205CEBF7A7CF63F1136E1";
    static final String MAP84_RESOURCE_SHA256 =
            "A04B1D9C886A4BF4A95A1F33B716EDB3345F1EA91DA0EA883B756CB76BEC5C49";
    static final int MAP4_ROW_COUNT = 641;
    /**
     * NPC 70740 has a retained template and a source-documented map-4
     * coordinate, but the selected spawnlist_npc omits its row.
     */
    static final int MAP4_SOURCE_RECONSTRUCTED_ROW_COUNT = 1;
    static final String MAP4_SOURCE_SHA256 =
            "2C754D512F340603FED76D8ABC482F820C252195704DBB918B595B7CD5FFCB9E";
    static final String MAP4_RESOURCE_SHA256 =
            "6EC6E401707B8DC4DC23172083E922587397CBAB666B611D3E570E4C8A489753";
    static final int MAP16_ROW_COUNT = 1;
    static final String MAP16_SOURCE_SHA256 =
            "2E267B38963307FD2F9208DE38DDB9AF3D86CBE48F2E4AFA2FC89EEA56DD4819";
    static final String MAP16_RESOURCE_SHA256 =
            "4BCC2BCAB09BE98B6AC8657675CDA06D8BC60CEB072269AE84FF6588643C0194";
    static final int MAP17_ROW_COUNT = 1;
    static final String MAP17_SOURCE_SHA256 =
            "4BAF69A14116E5705C7FD416D89B44740490E4AD082C61B77EA59A4B67CFE19D";
    static final String MAP17_RESOURCE_SHA256 =
            "72B4125394FA8D1C52968AD90E7F2F1A3DA7E0C5526D69C6A3D945987ABE40D1";
    static final int MAP22_ROW_COUNT = 0;
    static final String MAP22_SOURCE_SHA256 =
            "8263164069B03411156DCA1D6B564F777EED14697D36402D02A1086131AEE8BF";
    static final String MAP22_RESOURCE_SHA256 =
            "02DDCDD40ACC9CD4C0D799E9B4BD4E12D313B98AFCD14B5F233C3846FFC0FC61";
    static final int MAP25_ROW_COUNT = 0;
    static final int MAP26_ROW_COUNT = 0;
    static final int MAP27_ROW_COUNT = 0;
    static final int MAP28_ROW_COUNT = 1;
    static final String KNIGHT_CAVE_EMPTY_SOURCE_SHA256 =
            "8263164069B03411156DCA1D6B564F777EED14697D36402D02A1086131AEE8BF";
    static final String MAP28_SOURCE_SHA256 =
            "9BDD005E89B413A7B536D641679B13A5F7FBAABC16452E9239DC44E531767CE2";
    static final String MAP25_RESOURCE_SHA256 =
            "E20A10FB99CD7D3FC86707E8FA3220EA01567B1F2DCD11D511F963D6E3C18819";
    static final String MAP26_RESOURCE_SHA256 =
            "1B769C28E45002C9F9C52E6989AF57CC26681CC7EB1A46A01386A94F8D5D7ED7";
    static final String MAP27_RESOURCE_SHA256 =
            "D924A778ACB8D06A4462EB445D92930C8EE735FD6514FB0E3011577997A9431B";
    static final String MAP28_RESOURCE_SHA256 =
            "8401A7170639EEE8CE444425127D1E55E2EA28EB029BF1609F44B007C334692C";
    static final int MAP19_ROW_COUNT = 0;
    static final int MAP20_ROW_COUNT = 0;
    static final String MAP19_SOURCE_SHA256 =
            "8263164069B03411156DCA1D6B564F777EED14697D36402D02A1086131AEE8BF";
    static final String MAP20_SOURCE_SHA256 = MAP19_SOURCE_SHA256;
    static final String MAP19_RESOURCE_SHA256 =
            "75D12D7F3E07775833F512FD013B595FF281E68CDA4156F2E8BDCF70574E4108";
    static final String MAP20_RESOURCE_SHA256 =
            "F5DBF4A2E0576E7F96BC0BC4CC1ECDD535D41D9C7E2ACE9D14C39B717AB84EC0";
    static final int MAP30_ROW_COUNT = 0;
    static final String MAP30_SOURCE_SHA256 = MAP19_SOURCE_SHA256;
    static final String MAP30_RESOURCE_SHA256 =
            "C67F8E65B63A035EC72A1F44B757706C3921514F1D685E153BD4107CFB68D81B";
    static final int MAP31_ROW_COUNT = 0;
    static final String MAP31_SOURCE_SHA256 = MAP19_SOURCE_SHA256;
    static final String MAP31_RESOURCE_SHA256 =
            "EE88A3CB39E935C11D5EC581E0664A314359DBFECFBCAA030F8A177A97ACA2A7";
    static final int MAP32_ROW_COUNT = 2;
    static final String MAP32_SOURCE_SHA256 =
            "5D4A13D57F5A7A7F8F1A64F46C30190F760C047DAAF569D4B5B45AB168D80130";
    static final String MAP32_RESOURCE_SHA256 =
            "0854DEF2A32D302E9E0DFED72F8DDF7039A901CE8B05BB3E916D36DC2A545655";
    static final int MAP33_ROW_COUNT = 0;
    static final String MAP33_SOURCE_SHA256 = MAP19_SOURCE_SHA256;
    static final String MAP33_RESOURCE_SHA256 =
            "4EE3B5C231CD753D7739B8541AD4E7E0E06F5A1A2BE6055518DACA314FCBBE7D";
    static final int MAP35_ROW_COUNT = 0;
    static final String MAP35_SOURCE_SHA256 = MAP19_SOURCE_SHA256;
    static final String MAP35_RESOURCE_SHA256 =
            "D1D0B5F26B85FDA4E7B3E1FC372F935D2768E30369A9D63C89752A945519F939";
    static final int MAP36_ROW_COUNT = 0;
    static final String MAP36_SOURCE_SHA256 = MAP19_SOURCE_SHA256;
    static final String MAP36_RESOURCE_SHA256 =
            "52DD06B5BC08A1770D61A606F281AF38387CDD6C3D079F61DB00186D26E9E9A6";
    static final int MAP37_ROW_COUNT = 0;
    static final String MAP37_SOURCE_SHA256 = MAP19_SOURCE_SHA256;
    static final String MAP37_RESOURCE_SHA256 =
            "513CB2D544988C171A0671D291B6813C6725BF3CE0667C90CA84A22B585696ED";
    static final int MAP67_ROW_COUNT = 0;
    static final String MAP67_SOURCE_SHA256 = MAP19_SOURCE_SHA256;
    static final String MAP67_RESOURCE_SHA256 =
            "50D9BCFC4358454B0534E0DEA53A4775E404366EB2E4690CA31D251B65D0631E";
    static final int MAP350_ROW_COUNT = 2;
    static final String MAP350_SOURCE_SHA256 =
            "BE78548246E66233FC9AD74F054D557B9DD80E3F9A2C86D544F54FC1F231675C";
    static final String MAP350_RESOURCE_SHA256 =
            "D6E4017D5BB72483CE0C64731F844282781E4D1948972DC6CF1D93A17B8FADEE";
    static final int MAP209_ROW_COUNT = 0;
    static final int MAP213_ROW_COUNT = 0;
    static final String MAP209_SOURCE_SHA256 =
            "8263164069B03411156DCA1D6B564F777EED14697D36402D02A1086131AEE8BF";
    static final String MAP213_SOURCE_SHA256 = MAP209_SOURCE_SHA256;
    static final String MAP209_RESOURCE_SHA256 =
            "28CA33FF7611B064EFFE33ABBFAAB900395F723D7B5AACA99FB4DAF368DDB207";
    static final String MAP213_RESOURCE_SHA256 =
            "9E09AE0181842C226EB343C9A78BD1700493D25C7EA49D8BC5CAC56A9FD581D4";
    static final int MAP302_ROW_COUNT = 0;
    static final String MAP302_SOURCE_SHA256 = MAP209_SOURCE_SHA256;
    static final String MAP302_RESOURCE_SHA256 =
            "AB2950E588472185E648049709BC7185FAE2D397BB84E08EDC543F940F434330";
    static final int MAP445_ROW_COUNT = 1;
    static final String MAP445_SOURCE_SHA256 =
            "86F9AED7CF493AC4279BAB6260AFA487BC4951124A31ED7F708789EF1B034C39";
    static final String MAP445_RESOURCE_SHA256 =
            "DB5C264F5722E22120D2CCB79B1A44A50B0F09958CAAAF47636D6C873D6C5019";
    static final int MAP447_ROW_COUNT = 0;
    static final String MAP447_SOURCE_SHA256 = MAP209_SOURCE_SHA256;
    static final String MAP447_RESOURCE_SHA256 =
            "23BB6F832EB168220A0249C2835F37C5A82D6D3586F85D55AC309F1BA6394CF7";
    static final int MAP244_ROW_COUNT = 1;
    static final String MAP244_SOURCE_SHA256 =
            "5AFB6C2662F5AB4395299135C7EF86AC59C1B65CC52ADF94C2B00D19C9A9BB81";
    static final String MAP244_RESOURCE_SHA256 =
            "2E42C725C202147A6A6E8778A0E80947223DDFF1726CFBDADFFCB1A7D5BAD51B";
    static final int WORLD_TREE_MOTHER_SPAWN_ID = 1276;
    static final int WORLD_TREE_MOTHER_NPC_ID = 70844;
    static final int HORUN_SPAWN_ID = 588;
    static final int HORUN_NPC_ID = 70080;
    static final int WATER_DRAGON_GATE_SPAWN_ID = 1295;
    static final int WATER_DRAGON_SAELL_SPAWN_ID = 1296;

    static final Map<Integer, String> GLUDIO_DUNGEON_SOURCE_SHA256 = Map.of(
            7, "8263164069B03411156DCA1D6B564F777EED14697D36402D02A1086131AEE8BF",
            8, "8263164069B03411156DCA1D6B564F777EED14697D36402D02A1086131AEE8BF",
            9, "8263164069B03411156DCA1D6B564F777EED14697D36402D02A1086131AEE8BF",
            10, "8263164069B03411156DCA1D6B564F777EED14697D36402D02A1086131AEE8BF",
            11, "8263164069B03411156DCA1D6B564F777EED14697D36402D02A1086131AEE8BF",
            12, "A8D05DA96475990D52C9AE19AAC2E7E00622DBEBA9758C65BB36D76109D14531");
    static final Map<Integer, String> GLUDIO_DUNGEON_RESOURCE_SHA256 = Map.of(
            7, "0D4506B37AEE6F524E305BADE6A8FDF1F5479158D6B2F82D64CE338A77D30E3F",
            8, "F1785435A6281AE2DBB284355A5FCD6880AB78AFB46C4C12B2D5F34827B73109",
            9, "92FD2B7F97B97401E70E79A97BE85A1A7D7D9340F8A2B5F324076FE2676213B5",
            10, "BF6404485DFB18962B62C31C520F61171C5FEF41D44D5C6D0CFF913415EB2C25",
            11, "2FF6894178465E84DF9A22B0E6B4C732DCD2011E92684EC34D90DF1C241413D5",
            12, "0DC1D0B0167DB79E03E934AAEC1D0041A7508087D4E9E63632DEB75EEA192EFF");
    static final Map<Integer, String> GIRAN_DUNGEON_SOURCE_SHA256 = Map.of(
            53, "8263164069B03411156DCA1D6B564F777EED14697D36402D02A1086131AEE8BF",
            54, "8263164069B03411156DCA1D6B564F777EED14697D36402D02A1086131AEE8BF",
            55, "8263164069B03411156DCA1D6B564F777EED14697D36402D02A1086131AEE8BF",
            56, "8263164069B03411156DCA1D6B564F777EED14697D36402D02A1086131AEE8BF");
    static final Map<Integer, String> GIRAN_DUNGEON_RESOURCE_SHA256 = Map.of(
            53, "E42758CE4D8C51E94A6175B92139A12B06089A71F31858F9B6D62766E67845E3",
            54, "9BD5ED4F5C4FE2426B5A55E0E67114FA6DF096087919B6CB01D745F459A55AFD",
            55, "0491792C98EB49B45CD1B22FBCE7725FAA1DA785C5E13FA5E670D374F8371B78",
            56, "4DF2C5881F8F67D6870D8E05C102E0B165EF2F8AF0D82E54619A09BBCBA3C0BC");
    static final int MAP52_ROW_COUNT = 13;
    static final String MAP52_SOURCE_SHA256 =
            "5B400EC1C2454846FD29572C738AEEB4D7701DAFA15C26DBE07AED931ED61089";
    static final String MAP52_RESOURCE_SHA256 =
            "63137C33215728E7AB47FFF05ECAB29CDCF89B5F490C6756AA0EE952906AC059";
    static final Map<Integer, SourceContract> WINDAWOOD_CASTLE_SOURCE_CONTRACTS =
            Map.of(
                    23, new SourceContract(0,
                            "8263164069B03411156DCA1D6B564F777EED14697D36402D02A1086131AEE8BF"),
                    24, new SourceContract(0,
                            "8263164069B03411156DCA1D6B564F777EED14697D36402D02A1086131AEE8BF"),
                    29, new SourceContract(13,
                            "2EE0A6684889C238B3FC9AD7EBBF961119ED6DE8EB7450050E02726346CD9E51"));
    static final Map<Integer, String> WINDAWOOD_CASTLE_RESOURCE_SHA256 = Map.of(
            23, "2B92510094ACC7CC63067D3432D838FAE7767EE49AE700DB3E53FDAFD5D34863",
            24, "2E7B9502EBB53F15022F1AD447D8EC9C29E9530F827CAA9A08759E3C0D6363AF",
            29, "80145E05DC052A5C1BC2E42D97BEE11E653B2C1FF8AB1C911045493464DFCDCA");
    static final Map<Integer, String> KENT_COMPONENT_RESOURCE_SHA256 = Map.of(
            15, "F7FAB2396508D9BA8C786505C79A8874F602AC0D28AF8330105A2922A8E2F8C2",
            240, "B75277897FF2E718581621EBA22F5064D90AAF431AD686867FBA8012CA68413F",
            241, "5ADC036D77BEC259801A20F8B5237B60DFFD47317300131C316AA4AB64C0A10A",
            242, "BB503D6A258680EC1F9F35A09653C83EB4A7FB194F5D50B8EB334DA4160E938F",
            243, "C4B268B7A8E602C5824C45A240BEC35E2419975E45EA39B63B05018445F54081");
    static final Map<Integer, ClassicWorldIvoryTowerCatalog.SourceContract>
            IVORY_TOWER_SOURCE_CONTRACTS = ClassicWorldIvoryTowerCatalog.NPC_CONTRACTS;
    static final int MAP66_ROW_COUNT = 13;
    static final String MAP66_SOURCE_SHA256 =
            ClassicWorldDwarfCastleCatalog.NPC_SOURCE_SHA256;
    static final String MAP66_RESOURCE_SHA256 =
            ClassicWorldDwarfCastleCatalog.NPC_RESOURCE_SHA256;

    private record SourceContract(int rows, String sourceSha256) {}
    private static final Map<Integer, SourceContract> ANT_CAVE_SOURCE_CONTRACTS =
            Map.ofEntries(
                    Map.entry(43, new SourceContract(3,
                            "65EF7726D46EBB0A89506A99A1D146756166DDCA943E431A5BB215F445199209")),
                    Map.entry(44, new SourceContract(0, MAP19_SOURCE_SHA256)),
                    Map.entry(45, new SourceContract(2,
                            "8B4CA0ACA2BCCFC4BD53FDF67B4E6BE87FDF195DFE0F56525A3C41489A0D8FF1")),
                    Map.entry(46, new SourceContract(0, MAP19_SOURCE_SHA256)),
                    Map.entry(47, new SourceContract(0, MAP19_SOURCE_SHA256)),
                    Map.entry(48, new SourceContract(0, MAP19_SOURCE_SHA256)),
                    Map.entry(49, new SourceContract(0, MAP19_SOURCE_SHA256)),
                    Map.entry(50, new SourceContract(0, MAP19_SOURCE_SHA256)),
                    Map.entry(51, new SourceContract(0, MAP19_SOURCE_SHA256)),
                    Map.entry(541, new SourceContract(0, MAP19_SOURCE_SHA256)),
                    Map.entry(542, new SourceContract(0, MAP19_SOURCE_SHA256)),
                    Map.entry(543, new SourceContract(0, MAP19_SOURCE_SHA256)));
    private static final Map<Integer, SourceContract> KNIGHT_CAVE_SOURCE_CONTRACTS =
            Map.of(
                    25, new SourceContract(MAP25_ROW_COUNT, KNIGHT_CAVE_EMPTY_SOURCE_SHA256),
                    26, new SourceContract(MAP26_ROW_COUNT, KNIGHT_CAVE_EMPTY_SOURCE_SHA256),
                    27, new SourceContract(MAP27_ROW_COUNT, KNIGHT_CAVE_EMPTY_SOURCE_SHA256),
                    28, new SourceContract(MAP28_ROW_COUNT, MAP28_SOURCE_SHA256));
    private static final Map<Integer, SourceContract> GLUDIO_DUNGEON_SOURCE_CONTRACTS =
            Map.of(
                    7, new SourceContract(0, GLUDIO_DUNGEON_SOURCE_SHA256.get(7)),
                    8, new SourceContract(0, GLUDIO_DUNGEON_SOURCE_SHA256.get(8)),
                    9, new SourceContract(0, GLUDIO_DUNGEON_SOURCE_SHA256.get(9)),
                    10, new SourceContract(0, GLUDIO_DUNGEON_SOURCE_SHA256.get(10)),
                    11, new SourceContract(0, GLUDIO_DUNGEON_SOURCE_SHA256.get(11)),
                    12, new SourceContract(2, GLUDIO_DUNGEON_SOURCE_SHA256.get(12)));
    private static final Map<Integer, SourceContract> GIRAN_DUNGEON_SOURCE_CONTRACTS =
            Map.of(
                    53, new SourceContract(0, GIRAN_DUNGEON_SOURCE_SHA256.get(53)),
                    54, new SourceContract(0, GIRAN_DUNGEON_SOURCE_SHA256.get(54)),
                    55, new SourceContract(0, GIRAN_DUNGEON_SOURCE_SHA256.get(55)),
                    56, new SourceContract(0, GIRAN_DUNGEON_SOURCE_SHA256.get(56)));
    private static final Map<Integer, SourceContract> KENT_COMPONENT_SOURCE_CONTRACTS =
            Map.of(
                    15, new SourceContract(7,
                            "B5B4681508A75BB00FB700EE93A2FCA50CB5D23C2A3205198A6D392356599EDE"),
                    240, new SourceContract(0,
                            "8263164069B03411156DCA1D6B564F777EED14697D36402D02A1086131AEE8BF"),
                    241, new SourceContract(0,
                            "8263164069B03411156DCA1D6B564F777EED14697D36402D02A1086131AEE8BF"),
                    242, new SourceContract(0,
                            "8263164069B03411156DCA1D6B564F777EED14697D36402D02A1086131AEE8BF"),
                    243, new SourceContract(0,
                            "8263164069B03411156DCA1D6B564F777EED14697D36402D02A1086131AEE8BF"));

    private static final int MAGIC = 0x4C574E31; // LWN1
    private static final long OBJECT_BASE = 0x6400_0000L;
    private static final String TSV_HEADER = String.join("\t",
            "spawn_id", "npc_id", "x", "y", "heading", "movement_distance",
            "name_id", "impl", "gfx_id", "level", "hp", "mp", "exp", "lawful",
            "light_size", "passispeed", "atkspeed", "change_head",
            "normal_action", "chaotic_action");
    private static final Map<Integer, List<StaticNpc>> BY_MAP = Map.ofEntries(
            Map.entry(ClassicWorldMapCatalog.TALKING_ISLAND_MAP_ID,
                    load("/world/map0-static-npcs.lwn.gz")),
            Map.entry(ClassicWorldMapCatalog.ROYAL_TRIAL_CAVE_FIRST_MAP_ID,
                    load("/world/map1-static-npcs.lwn.gz")),
            Map.entry(ClassicWorldMapCatalog.ROYAL_TRIAL_CAVE_SECOND_MAP_ID,
                    load("/world/map2-static-npcs.lwn.gz")),
            Map.entry(ClassicWorldMapCatalog.UNDERSEA_TUNNEL_MAP_ID,
                    load("/world/map14-static-npcs.lwn.gz")),
            Map.entry(ClassicWorldMapCatalog.UNDERSEA_TUNNEL_SEVENTH_FLOOR_MAP_ID,
                    load("/world/map13-static-npcs.lwn.gz")),
            Map.entry(ClassicWorldMapCatalog.JIM_SECRET_ROOM_MAP_ID,
                    load("/world/map237-static-npcs.lwn.gz")),
            Map.entry(ClassicWorldMapCatalog.GUNTER_HOUSE_MAP_ID,
                    load("/world/map3-static-npcs.lwn.gz")),
            Map.entry(ClassicWorldMapCatalog.TALKING_ISLAND_TO_GLUDIO_SHIP_MAP_ID,
                    load("/world/map5-static-npcs.lwn.gz")),
            Map.entry(ClassicWorldMapCatalog.GLUDIO_TO_TALKING_ISLAND_SHIP_MAP_ID,
                    load("/world/map6-static-npcs.lwn.gz")),
            Map.entry(ClassicWorldMapCatalog.FORGOTTEN_ISLAND_MAP_ID,
                    load("/world/map70-static-npcs.lwn.gz")),
            Map.entry(ClassicWorldMapCatalog.MAINLAND_TO_FORGOTTEN_ISLAND_SHIP_MAP_ID,
                    load("/world/map83-static-npcs.lwn.gz")),
            Map.entry(ClassicWorldMapCatalog.FORGOTTEN_ISLAND_TO_MAINLAND_SHIP_MAP_ID,
                    load("/world/map84-static-npcs.lwn.gz")),
            Map.entry(ClassicWorldMapCatalog.WORLD_TREE_MAP_ID,
                    load("/world/map4-static-npcs.lwn.gz")),
            Map.entry(ClassicWorldMapCatalog.HARTIN_LABORATORY_MAP_ID,
                    load("/world/map16-static-npcs.lwn.gz")),
            Map.entry(ClassicWorldMapCatalog.NERUPA_CAVE_MAP_ID,
                    load("/world/map17-static-npcs.lwn.gz")),
            Map.entry(ClassicWorldMapCatalog.GERARD_TRIAL_DUNGEON_MAP_ID,
                    load("/world/map22-static-npcs.lwn.gz")),
            Map.entry(25, load("/world/map25-static-npcs.lwn.gz")),
            Map.entry(26, load("/world/map26-static-npcs.lwn.gz")),
            Map.entry(27, load("/world/map27-static-npcs.lwn.gz")),
            Map.entry(28, load("/world/map28-static-npcs.lwn.gz")),
            Map.entry(43, load("/world/map43-static-npcs.lwn.gz")),
            Map.entry(44, load("/world/map44-static-npcs.lwn.gz")),
            Map.entry(45, load("/world/map45-static-npcs.lwn.gz")),
            Map.entry(46, load("/world/map46-static-npcs.lwn.gz")),
            Map.entry(47, load("/world/map47-static-npcs.lwn.gz")),
            Map.entry(48, load("/world/map48-static-npcs.lwn.gz")),
            Map.entry(49, load("/world/map49-static-npcs.lwn.gz")),
            Map.entry(50, load("/world/map50-static-npcs.lwn.gz")),
            Map.entry(51, load("/world/map51-static-npcs.lwn.gz")),
            Map.entry(541, load("/world/map541-static-npcs.lwn.gz")),
            Map.entry(542, load("/world/map542-static-npcs.lwn.gz")),
            Map.entry(543, load("/world/map543-static-npcs.lwn.gz")),
            Map.entry(ClassicWorldMapCatalog.SLEEPING_DRAGON_FIRST_FLOOR_MAP_ID,
                    load("/world/map19-static-npcs.lwn.gz")),
            Map.entry(ClassicWorldMapCatalog.SLEEPING_DRAGON_SECOND_FLOOR_MAP_ID,
                    load("/world/map20-static-npcs.lwn.gz")),
            Map.entry(ClassicWorldMapCatalog.DRAGON_VALLEY_DUNGEON_FIRST_FLOOR_MAP_ID,
                    load("/world/map30-static-npcs.lwn.gz")),
            Map.entry(ClassicWorldMapCatalog.DRAGON_VALLEY_DUNGEON_SECOND_FLOOR_MAP_ID,
                    load("/world/map31-static-npcs.lwn.gz")),
            Map.entry(ClassicWorldMapCatalog.DRAGON_VALLEY_DUNGEON_THIRD_FLOOR_MAP_ID,
                    load("/world/map32-static-npcs.lwn.gz")),
            Map.entry(ClassicWorldMapCatalog.DRAGON_VALLEY_DUNGEON_FOURTH_FLOOR_MAP_ID,
                    load("/world/map33-static-npcs.lwn.gz")),
            Map.entry(ClassicWorldMapCatalog.DRAGON_VALLEY_DUNGEON_FIFTH_FLOOR_MAP_ID,
                    load("/world/map35-static-npcs.lwn.gz")),
            Map.entry(ClassicWorldMapCatalog.DRAGON_VALLEY_DUNGEON_SIXTH_FLOOR_MAP_ID,
                    load("/world/map36-static-npcs.lwn.gz")),
            Map.entry(ClassicWorldMapCatalog.ANTHARAS_LAIR_MAP_ID,
                    load("/world/map37-static-npcs.lwn.gz")),
            Map.entry(ClassicWorldMapCatalog.VALAKAS_HABITAT_MAP_ID,
                    load("/world/map67-static-npcs.lwn.gz")),
            Map.entry(ClassicWorldMapCatalog.DARK_ELF_DUNGEON_MAP_ID,
                    load("/world/map209-static-npcs.lwn.gz")),
            Map.entry(ClassicWorldMapCatalog.DARKMAR_DUNGEON_MAP_ID,
                    load("/world/map213-static-npcs.lwn.gz")),
            Map.entry(ClassicWorldMapCatalog.SEPIA_DUNGEON_MAP_ID,
                    load("/world/map302-static-npcs.lwn.gz")),
            Map.entry(ClassicWorldMapCatalog.TAINTED_LAND_MAP_ID,
                    load("/world/map244-static-npcs.lwn.gz")),
            Map.entry(ClassicWorldMapCatalog.GIRAN_TRADE_ZONE_MAP_ID,
                    load("/world/map350-static-npcs.lwn.gz")),
            Map.entry(ClassicWorldMapCatalog.HIDDEN_DOCK_MAP_ID,
                    load("/world/map445-static-npcs.lwn.gz")),
            Map.entry(ClassicWorldMapCatalog.PIRATE_ISLAND_SHIP_MAP_ID,
                    load("/world/map447-static-npcs.lwn.gz")),
            Map.entry(7, load("/world/map7-static-npcs.lwn.gz")),
            Map.entry(8, load("/world/map8-static-npcs.lwn.gz")),
            Map.entry(9, load("/world/map9-static-npcs.lwn.gz")),
            Map.entry(10, load("/world/map10-static-npcs.lwn.gz")),
            Map.entry(11, load("/world/map11-static-npcs.lwn.gz")),
            Map.entry(12, load("/world/map12-static-npcs.lwn.gz")),
            Map.entry(53, load("/world/map53-static-npcs.lwn.gz")),
            Map.entry(54, load("/world/map54-static-npcs.lwn.gz")),
            Map.entry(55, load("/world/map55-static-npcs.lwn.gz")),
            Map.entry(56, load("/world/map56-static-npcs.lwn.gz")),
            Map.entry(ClassicWorldMapCatalog.GIRAN_CASTLE_MAP_ID,
                    load("/world/map52-static-npcs.lwn.gz")),
            Map.entry(23, load("/world/map23-static-npcs.lwn.gz")),
            Map.entry(24, load("/world/map24-static-npcs.lwn.gz")),
            Map.entry(29, load("/world/map29-static-npcs.lwn.gz")),
            Map.entry(15, load("/world/map15-static-npcs.lwn.gz")),
            Map.entry(240, load("/world/map240-static-npcs.lwn.gz")),
            Map.entry(241, load("/world/map241-static-npcs.lwn.gz")),
            Map.entry(242, load("/world/map242-static-npcs.lwn.gz")),
            Map.entry(243, load("/world/map243-static-npcs.lwn.gz")),
            Map.entry(75, load("/world/map75-static-npcs.lwn.gz")),
            Map.entry(76, load("/world/map76-static-npcs.lwn.gz")),
            Map.entry(77, load("/world/map77-static-npcs.lwn.gz")),
            Map.entry(78, load("/world/map78-static-npcs.lwn.gz")),
            Map.entry(79, load("/world/map79-static-npcs.lwn.gz")),
            Map.entry(80, load("/world/map80-static-npcs.lwn.gz")),
            Map.entry(81, load("/world/map81-static-npcs.lwn.gz")),
            Map.entry(82, load("/world/map82-static-npcs.lwn.gz")),
            Map.entry(ClassicWorldDwarfCastleCatalog.MAP_ID,
                    load("/world/map66-static-npcs.lwn.gz")),
            Map.entry(101, load("/world/map101-static-npcs.lwn.gz")),
            Map.entry(102, load("/world/map102-static-npcs.lwn.gz")),
            Map.entry(103, load("/world/map103-static-npcs.lwn.gz")),
            Map.entry(104, load("/world/map104-static-npcs.lwn.gz")),
            Map.entry(105, load("/world/map105-static-npcs.lwn.gz")),
            Map.entry(106, load("/world/map106-static-npcs.lwn.gz")),
            Map.entry(107, load("/world/map107-static-npcs.lwn.gz")),
            Map.entry(108, load("/world/map108-static-npcs.lwn.gz")),
            Map.entry(109, load("/world/map109-static-npcs.lwn.gz")),
            Map.entry(110, load("/world/map110-static-npcs.lwn.gz")),
            Map.entry(300, load("/world/map300-static-npcs.lwn.gz")),
            Map.entry(301, load("/world/map301-static-npcs.lwn.gz")),
            Map.entry(59, load("/world/map59-static-npcs.lwn.gz")),
            Map.entry(60, load("/world/map60-static-npcs.lwn.gz")),
            Map.entry(61, load("/world/map61-static-npcs.lwn.gz")),
            Map.entry(62, load("/world/map62-static-npcs.lwn.gz")),
            Map.entry(63, load("/world/map63-static-npcs.lwn.gz")),
            Map.entry(64, load("/world/map64-static-npcs.lwn.gz")),
            Map.entry(65, load("/world/map65-static-npcs.lwn.gz")),
            Map.entry(252, load("/world/map252-static-npcs.lwn.gz")),
            Map.entry(253, load("/world/map253-static-npcs.lwn.gz")),
            Map.entry(254, load("/world/map254-static-npcs.lwn.gz")),
            Map.entry(304, load("/world/map304-static-npcs.lwn.gz")),
            Map.entry(305, load("/world/map305-static-npcs.lwn.gz")),
            Map.entry(307, load("/world/map307-static-npcs.lwn.gz")),
            Map.entry(308, load("/world/map308-static-npcs.lwn.gz")),
            Map.entry(309, load("/world/map309-static-npcs.lwn.gz")),
            Map.entry(310, load("/world/map310-static-npcs.lwn.gz")),
            Map.entry(320, load("/world/map320-static-npcs.lwn.gz")),
            Map.entry(330, load("/world/map330-static-npcs.lwn.gz")),
            Map.entry(400, load("/world/map400-static-npcs.lwn.gz")),
            Map.entry(401, load("/world/map401-static-npcs.lwn.gz")),
            Map.entry(ClassicWorldDemonTempleCatalog.MAP_ID,
                    load("/world/map410-static-npcs.lwn.gz")),
            Map.entry(ClassicWorldElementalGraveCatalog.ABYSS_LAKE_MAP_ID,
                    load("/world/map420-static-npcs.lwn.gz")),
            Map.entry(ClassicWorldElementalGraveCatalog.ELEMENTAL_GRAVE_MAP_ID,
                    load("/world/map430-static-npcs.lwn.gz")),
            Map.entry(ClassicWorldElementalGraveCatalog.PIRATE_ISLAND_MAP_ID,
                    append(load("/world/map440-static-npcs.lwn.gz"),
                            pirateRoxanne())),
            Map.entry(441, load("/world/map441-static-npcs.lwn.gz")),
            Map.entry(442, load("/world/map442-static-npcs.lwn.gz")),
            Map.entry(443, load("/world/map443-static-npcs.lwn.gz")),
            Map.entry(444, load("/world/map444-static-npcs.lwn.gz")),
            Map.entry(480, load("/world/map480-static-npcs.lwn.gz")),
            Map.entry(481, load("/world/map481-static-npcs.lwn.gz")),
            Map.entry(482, append(load("/world/map482-static-npcs.lwn.gz"),
                    pirateFrancu())),
            Map.entry(483, load("/world/map483-static-npcs.lwn.gz")),
            Map.entry(484, load("/world/map484-static-npcs.lwn.gz")),
            Map.entry(550, load("/world/map550-static-npcs.lwn.gz")),
            Map.entry(551, load("/world/map551-static-npcs.lwn.gz")),
            Map.entry(552, load("/world/map552-static-npcs.lwn.gz")),
            Map.entry(553, load("/world/map553-static-npcs.lwn.gz")),
            Map.entry(554, load("/world/map554-static-npcs.lwn.gz")),
            Map.entry(555, load("/world/map555-static-npcs.lwn.gz")),
            Map.entry(556, load("/world/map556-static-npcs.lwn.gz")),
            Map.entry(557, load("/world/map557-static-npcs.lwn.gz")),
            Map.entry(558, load("/world/map558-static-npcs.lwn.gz")),
            Map.entry(600, load("/world/map600-npcs.lwn.gz")),
            Map.entry(601, load("/world/map601-npcs.lwn.gz")),
            Map.entry(602, load("/world/map602-npcs.lwn.gz")),
            Map.entry(603, load("/world/map603-npcs.lwn.gz")),
            Map.entry(604, load("/world/map604-npcs.lwn.gz")),
            Map.entry(605, load("/world/map605-npcs.lwn.gz")),
            Map.entry(606, load("/world/map606-npcs.lwn.gz")),
            Map.entry(607, load("/world/map607-npcs.lwn.gz")),
            Map.entry(778, load("/world/map778-npcs.lwn.gz")),
            Map.entry(779, load("/world/map779-npcs.lwn.gz")),
            Map.entry(ClassicWorldLastabadGateCatalog.MAP_ID,
                    load("/world/map450-static-npcs.lwn.gz")),
            Map.entry(ClassicWorldWaterDragonCatalog.GATE_MAP_ID,
                    List.of(waterDragonGate())),
            Map.entry(ClassicWorldWaterDragonCatalog.MAP_ID,
                    List.of(waterDragonSaell())),
            Map.entry(1000, load("/world/map1000-static-npcs.lwn.gz")),
            Map.entry(1001, load("/world/map1001-static-npcs.lwn.gz")));

    record StaticNpc(
            int mapId, int spawnId, long objectId, int npcId,
            int x, int y, int heading, int movementDistance,
            String nameId, String implementation, int gfxId,
            int level, int hp, int mp, long experience, int lawful,
            int lightSize, int passispeed, int atkspeed, boolean changeHead,
            String normalAction, String chaoticAction) {
        StaticNpc {
            if (mapId < 0 || spawnId <= 0
                    || objectId != ClassicWorldNpcCatalog.objectId(spawnId)
                    || npcId <= 0 || x <= 0 || x > 0xFFFF || y <= 0 || y > 0xFFFF
                    || heading < 0 || heading > 7 || movementDistance < 0
                    || nameId == null
                    || implementation == null || implementation.isEmpty()
                    || gfxId < 0 || gfxId > 0xFFFF
                    || (gfxId == 0 && !implementation.equals("L1Bow"))
                    || level < 0 || level > 0xFF
                    || hp < 0 || mp < 0 || experience < 0 || experience > 0xFFFF_FFFFL
                    || lawful < Short.MIN_VALUE || lawful > Short.MAX_VALUE
                    || lightSize < 0 || lightSize > 0xFF
                    || passispeed < 0 || atkspeed < 0
                    || normalAction == null || chaoticAction == null) {
                throw new IllegalArgumentException("invalid retained classic NPC row " + spawnId);
            }
        }

        boolean fixedMerchant() {
            return movementDistance == 0 && implementation.equals("L1Merchant");
        }

        boolean fixedWarehouse() {
            return movementDistance == 0 && implementation.equals("L1Dwarf")
                    && (normalAction.equals("storage") && chaoticAction.equals("storage")
                        || ClassicWorldHeineCatalog.isWarehouse(this)
                        || ClassicWorldRastabadOumCatalog.isWarehouse(this)
                        || ClassicWorldDiadradCatalog.isWarehouse(this));
        }

        boolean housekeeper() {
            return implementation.equals("L1Housekeeper");
        }

        String initialHtml(long playerLawful) {
            return playerLawful < -1000 ? chaoticAction : normalAction;
        }
    }

    private ClassicWorldNpcCatalog() {}

    private static StaticNpc waterDragonGate() {
        return new StaticNpc(
                ClassicWorldWaterDragonCatalog.GATE_MAP_ID,
                WATER_DRAGON_GATE_SPAWN_ID,
                objectId(WATER_DRAGON_GATE_SPAWN_ID),
                ClassicWorldWaterDragonCatalog.GATE_NPC_ID,
                32809, 32896, 7, 0, "$7747", "quest2.Npc_DragonA2",
                7548, 0, 0, 0, 0, 0, 0, 0, 0,
                false, "", "");
    }

    private static StaticNpc waterDragonSaell() {
        return new StaticNpc(
                ClassicWorldWaterDragonCatalog.MAP_ID,
                WATER_DRAGON_SAELL_SPAWN_ID,
                objectId(WATER_DRAGON_SAELL_SPAWN_ID),
                ClassicWorldWaterDragonCatalog.SAELL_NPC_ID,
                32859, 32664, 4, 0, "$4346", "quest2.Npc_Saell",
                7904, 0, 0, 0, 0, 0, 0, 0, 0,
                false, "", "");
    }

    private static StaticNpc pirateFrancu() {
        return new StaticNpc(
                482, ClassicWorldPirateBackHalfCatalog.FRANCU_RECONSTRUCTED_SPAWN_ID,
                objectId(ClassicWorldPirateBackHalfCatalog.FRANCU_RECONSTRUCTED_SPAWN_ID),
                ClassicWorldPirateBackHalfCatalog.FRANCU_NPC_ID,
                32679, 32860, 6, 0, "$3731", "L1Merchant",
                4680, 0, 0, 0, 0, 0, 14, 0, 0,
                false, "francu1", "francu001");
    }

    private static StaticNpc pirateRoxanne() {
        return new StaticNpc(
                ClassicWorldMerchantCatalog.ROXXANNE_MAP_ID,
                ClassicWorldMerchantCatalog.ROXXANNE_RECONSTRUCTED_SPAWN_ID,
                objectId(ClassicWorldMerchantCatalog.ROXXANNE_RECONSTRUCTED_SPAWN_ID),
                ClassicWorldMerchantCatalog.ROXXANNE_NPC_ID,
                32292, 33067, 4, 0, "$3438", "L1Merchant",
                2466, 0, 0, 0, 0, 0, 14, 0, 0,
                false, ClassicWorldMerchantCatalog.ROXXANNE_INITIAL_HTML,
                ClassicWorldMerchantCatalog.ROXXANNE_INITIAL_HTML);
    }

    /**
     * Duke's soldier required by the native KnightLv45 Searcher arrival.
     *
     * <p>The coordinate is retained in KnightLv45_1; every remaining actor
     * field comes from selected npc template 70740.  Heading four is the only
     * absent spawn field and is a harmless initial orientation because the
     * retained template turns toward interacting players.</p>
     */
    private static StaticNpc dukeSoldier() {
        int spawnId = ClassicWorldRescueFollowers.DUKE_SOLDIER_NPC_ID;
        return new StaticNpc(
                ClassicWorldRescueFollowers.MAP_ID, spawnId, objectId(spawnId),
                ClassicWorldRescueFollowers.DUKE_SOLDIER_NPC_ID,
                ClassicWorldRescueFollowers.DUKE_SOLDIER_X,
                ClassicWorldRescueFollowers.DUKE_SOLDIER_Y,
                4, 0, "$2267", "L1Merchant", 51,
                0, 0, 0, 0, 0, 14, 0, 0,
                true, "", "");
    }

    private static List<StaticNpc> append(List<StaticNpc> source, StaticNpc npc) {
        java.util.ArrayList<StaticNpc> result = new java.util.ArrayList<>(source);
        result.add(npc);
        return List.copyOf(result);
    }

    private static List<StaticNpc> appendAll(
            List<StaticNpc> source, List<StaticNpc> additions) {
        java.util.ArrayList<StaticNpc> result = new java.util.ArrayList<>(source);
        result.addAll(additions);
        return List.copyOf(result);
    }

    static List<StaticNpc> all(int mapId) {
        List<StaticNpc> source = BY_MAP.getOrDefault(mapId, List.of());
        if (mapId == 4) {
            source = append(source, dukeSoldier());
        }
        if (mapId == 4) {
            source = appendAll(source, ClassicWorldMonsterGuardCatalog.all());
        }
        List<StaticNpc> scarecrows = switch (mapId) {
            case 4, 304 -> ClassicWorldScarecrowCatalog.forMap(mapId);
            default -> List.of();
        };
        return CombatSpecialBuffs.npcs(
                scarecrows.isEmpty() ? source : appendAll(source, scarecrows),
                mapId);
    }

    static List<StaticNpc> fixedMerchants(int mapId) {
        return filtered(mapId, StaticNpc::fixedMerchant);
    }

    static List<StaticNpc> fixedMerchantsInScreen(int mapId, int x, int y) {
        return filtered(mapId, npc -> npc.fixedMerchant()
                && BattleSpellCatalog.isInNativeScreen(x, y, npc.x(), npc.y()));
    }

    static List<StaticNpc> inScreen(int mapId, int x, int y) {
        return filtered(mapId, npc -> BattleSpellCatalog.isInNativeScreen(
                x, y, npc.x(), npc.y()));
    }

    static StaticNpc npc(int mapId, long objectId) {
        return all(mapId).stream()
                .filter(candidate -> candidate.objectId() == objectId)
                .findFirst().orElse(null);
    }

    static StaticNpc fixedMerchant(int mapId, long objectId) {
        return all(mapId).stream()
                .filter(StaticNpc::fixedMerchant)
                .filter(npc -> npc.objectId() == objectId)
                .findFirst().orElse(null);
    }

    static StaticNpc fixedWarehouse(int mapId, long objectId) {
        return all(mapId).stream()
                .filter(StaticNpc::fixedWarehouse)
                .filter(npc -> npc.objectId() == objectId)
                .findFirst().orElse(null);
    }

    static List<StaticNpc> fixedWarehouses(int mapId) {
        return filtered(mapId, StaticNpc::fixedWarehouse);
    }

    static boolean fixedMerchantOccupies(int mapId, int x, int y) {
        return all(mapId).stream().anyMatch(npc ->
                npc.fixedMerchant() && npc.x() == x && npc.y() == y);
    }

    static boolean occupies(int mapId, int x, int y) {
        // Live L1Guardian/L1Guard rows are promoted into the moving monster
        // population. Their current Actor cell, not the immutable spawn
        // origin, owns collision after bootstrap. HP-zero L1Guard resources
        // deliberately remain static.
        return all(mapId).stream().anyMatch(npc ->
                !ClassicWorldGuardianCatalog.isGuardian(npc)
                        && !ClassicWorldGuardCatalog.isGuard(npc)
                        && npc.x() == x && npc.y() == y);
    }

    static long objectId(int spawnId) {
        if (spawnId <= 0 || spawnId > 0x00FF_FFFF) {
            throw new IllegalArgumentException("classic NPC spawn id is outside object range");
        }
        return OBJECT_BASE | spawnId;
    }

    private static List<StaticNpc> filtered(int mapId, Predicate<StaticNpc> predicate) {
        return all(mapId).stream().filter(predicate).toList();
    }

    private static List<StaticNpc> load(String resourceName) {
        byte[] packed;
        try (InputStream resource = ClassicWorldNpcCatalog.class
                .getResourceAsStream(resourceName)) {
            if (resource == null) throw new IOException(
                    "missing classic NPC resource " + resourceName);
            packed = resource.readAllBytes();
        } catch (IOException error) {
            throw new ExceptionInInitializerError(error);
        }
        try (DataInputStream in = new DataInputStream(new GZIPInputStream(
                new ByteArrayInputStream(packed)))) {
            if (in.readInt() != MAGIC) throw new IOException("invalid classic NPC magic");
            int mapId = in.readUnsignedShort();
            int rowCount = in.readInt();
            byte[] sourceHashBytes = in.readNBytes(32);
            if (sourceHashBytes.length != 32) throw new IOException("truncated source hash");
            String sourceHash = HexFormat.of().withUpperCase().formatHex(sourceHashBytes);
            byte[] table = in.readAllBytes();
            String observedHash = sha256(table);
            if (!sourceHash.equals(observedHash)) {
                throw new IOException("classic NPC table hash does not match header");
            }
            ClassicWorldDesireCaveCatalog.ResourceContract desireCave =
                    ClassicWorldDesireCaveCatalog.NPC_CONTRACTS.get(mapId);
            if (desireCave != null
                    && (rowCount != desireCave.rows()
                        || !sourceHash.equals(desireCave.sourceSha256())
                        || !sha256(packed).equals(desireCave.resourceSha256()))) {
                throw new IOException(
                        "classic Desire-Cave NPC contract changed " + mapId);
            }
            ClassicWorldPirateBackHalfCatalog.ComponentContract pirateBackHalf =
                    ClassicWorldPirateBackHalfCatalog.NPC_CONTRACTS.get(mapId);
            if (pirateBackHalf != null
                    && (rowCount != pirateBackHalf.rows()
                        || !sourceHash.equals(pirateBackHalf.sourceSha256())
                        || !sha256(packed).equals(pirateBackHalf.resourceSha256()))) {
                throw new IOException(
                        "classic Pirate-back-half NPC contract changed " + mapId);
            }
            ClassicWorldBehemothSilveriaCatalog.ResourceContract homeTown =
                    ClassicWorldBehemothSilveriaCatalog.NPC_CONTRACTS.get(mapId);
            if (homeTown != null
                    && (rowCount != homeTown.rows()
                        || !sourceHash.equals(homeTown.sourceSha256())
                        || !sha256(packed).equals(homeTown.resourceSha256()))) {
                throw new IOException(
                        "classic home-town NPC contract changed " + mapId);
            }
            if (mapId == ClassicWorldMapCatalog.WORLD_TREE_MAP_ID
                    && (rowCount != MAP4_ROW_COUNT || !MAP4_SOURCE_SHA256.equals(sourceHash))) {
                throw new IOException("classic map4 NPC source contract changed");
            }
            if (mapId == ClassicWorldMapCatalog.TALKING_ISLAND_MAP_ID
                    && (rowCount != MAP0_ROW_COUNT
                        || !MAP0_SOURCE_SHA256.equals(sourceHash))) {
                throw new IOException("classic map0 NPC source contract changed");
            }
            if (mapId == ClassicWorldMapCatalog.ROYAL_TRIAL_CAVE_FIRST_MAP_ID
                    && (rowCount != MAP1_ROW_COUNT
                        || !MAP1_SOURCE_SHA256.equals(sourceHash))) {
                throw new IOException("classic map1 NPC source contract changed");
            }
            if (mapId == ClassicWorldMapCatalog.ROYAL_TRIAL_CAVE_SECOND_MAP_ID
                    && (rowCount != MAP2_ROW_COUNT
                        || !MAP2_SOURCE_SHA256.equals(sourceHash))) {
                throw new IOException("classic map2 NPC source contract changed");
            }
            if (mapId == ClassicWorldMapCatalog.UNDERSEA_TUNNEL_MAP_ID
                    && (rowCount != MAP14_ROW_COUNT
                        || !MAP14_SOURCE_SHA256.equals(sourceHash))) {
                throw new IOException("classic map14 NPC source contract changed");
            }
            if (mapId == ClassicWorldMapCatalog.UNDERSEA_TUNNEL_SEVENTH_FLOOR_MAP_ID
                    && (rowCount != MAP13_ROW_COUNT
                        || !MAP13_SOURCE_SHA256.equals(sourceHash))) {
                throw new IOException("classic map13 NPC source contract changed");
            }
            if (mapId == ClassicWorldMapCatalog.JIM_SECRET_ROOM_MAP_ID
                    && (rowCount != MAP237_ROW_COUNT
                        || !MAP237_SOURCE_SHA256.equals(sourceHash))) {
                throw new IOException("classic map237 NPC source contract changed");
            }
            if (mapId == ClassicWorldMapCatalog.GUNTER_HOUSE_MAP_ID
                    && (rowCount != MAP3_ROW_COUNT
                        || !MAP3_SOURCE_SHA256.equals(sourceHash))) {
                throw new IOException("classic map3 NPC source contract changed");
            }
            if (mapId == ClassicWorldMapCatalog.TALKING_ISLAND_TO_GLUDIO_SHIP_MAP_ID
                    && (rowCount != MAP5_ROW_COUNT
                        || !MAP5_SOURCE_SHA256.equals(sourceHash))) {
                throw new IOException("classic map5 NPC source contract changed");
            }
            if (mapId == ClassicWorldMapCatalog.GLUDIO_TO_TALKING_ISLAND_SHIP_MAP_ID
                    && (rowCount != MAP6_ROW_COUNT
                        || !MAP6_SOURCE_SHA256.equals(sourceHash))) {
                throw new IOException("classic map6 NPC source contract changed");
            }
            if (mapId == ClassicWorldMapCatalog.FORGOTTEN_ISLAND_MAP_ID
                    && (rowCount != MAP70_ROW_COUNT
                        || !MAP70_SOURCE_SHA256.equals(sourceHash))) {
                throw new IOException("classic map70 NPC source contract changed");
            }
            if (mapId == ClassicWorldMapCatalog.MAINLAND_TO_FORGOTTEN_ISLAND_SHIP_MAP_ID
                    && (rowCount != MAP83_ROW_COUNT
                        || !MAP83_SOURCE_SHA256.equals(sourceHash))) {
                throw new IOException("classic map83 NPC source contract changed");
            }
            if (mapId == ClassicWorldMapCatalog.FORGOTTEN_ISLAND_TO_MAINLAND_SHIP_MAP_ID
                    && (rowCount != MAP84_ROW_COUNT
                        || !MAP84_SOURCE_SHA256.equals(sourceHash))) {
                throw new IOException("classic map84 NPC source contract changed");
            }
            if (mapId == ClassicWorldMapCatalog.HARTIN_LABORATORY_MAP_ID
                    && (rowCount != MAP16_ROW_COUNT
                        || !MAP16_SOURCE_SHA256.equals(sourceHash))) {
                throw new IOException("classic map16 NPC source contract changed");
            }
            if (mapId == ClassicWorldMapCatalog.NERUPA_CAVE_MAP_ID
                    && (rowCount != MAP17_ROW_COUNT
                        || !MAP17_SOURCE_SHA256.equals(sourceHash))) {
                throw new IOException("classic map17 NPC source contract changed");
            }
            if (mapId == ClassicWorldMapCatalog.GERARD_TRIAL_DUNGEON_MAP_ID
                    && (rowCount != MAP22_ROW_COUNT
                        || !MAP22_SOURCE_SHA256.equals(sourceHash))) {
                throw new IOException("classic map22 NPC source contract changed");
            }
            if (mapId == ClassicWorldMapCatalog.SLEEPING_DRAGON_FIRST_FLOOR_MAP_ID
                    && (rowCount != MAP19_ROW_COUNT
                        || !MAP19_SOURCE_SHA256.equals(sourceHash))) {
                throw new IOException("classic map19 NPC source contract changed");
            }
            if (mapId == ClassicWorldMapCatalog.SLEEPING_DRAGON_SECOND_FLOOR_MAP_ID
                    && (rowCount != MAP20_ROW_COUNT
                        || !MAP20_SOURCE_SHA256.equals(sourceHash))) {
                throw new IOException("classic map20 NPC source contract changed");
            }
            if (mapId == ClassicWorldMapCatalog.DRAGON_VALLEY_DUNGEON_FIRST_FLOOR_MAP_ID
                    && (rowCount != MAP30_ROW_COUNT
                        || !MAP30_SOURCE_SHA256.equals(sourceHash))) {
                throw new IOException("classic map30 NPC source contract changed");
            }
            if (mapId == ClassicWorldMapCatalog.DRAGON_VALLEY_DUNGEON_SECOND_FLOOR_MAP_ID
                    && (rowCount != MAP31_ROW_COUNT
                        || !MAP31_SOURCE_SHA256.equals(sourceHash))) {
                throw new IOException("classic map31 NPC source contract changed");
            }
            if (mapId == ClassicWorldMapCatalog.DRAGON_VALLEY_DUNGEON_THIRD_FLOOR_MAP_ID
                    && (rowCount != MAP32_ROW_COUNT
                        || !MAP32_SOURCE_SHA256.equals(sourceHash))) {
                throw new IOException("classic map32 NPC source contract changed");
            }
            if (mapId == ClassicWorldMapCatalog.DRAGON_VALLEY_DUNGEON_FOURTH_FLOOR_MAP_ID
                    && (rowCount != MAP33_ROW_COUNT
                        || !MAP33_SOURCE_SHA256.equals(sourceHash))) {
                throw new IOException("classic map33 NPC source contract changed");
            }
            if (mapId == ClassicWorldMapCatalog.DRAGON_VALLEY_DUNGEON_FIFTH_FLOOR_MAP_ID
                    && (rowCount != MAP35_ROW_COUNT
                        || !MAP35_SOURCE_SHA256.equals(sourceHash))) {
                throw new IOException("classic map35 NPC source contract changed");
            }
            if (mapId == ClassicWorldMapCatalog.DRAGON_VALLEY_DUNGEON_SIXTH_FLOOR_MAP_ID
                    && (rowCount != MAP36_ROW_COUNT
                        || !MAP36_SOURCE_SHA256.equals(sourceHash))) {
                throw new IOException("classic map36 NPC source contract changed");
            }
            if (mapId == ClassicWorldMapCatalog.ANTHARAS_LAIR_MAP_ID
                    && (rowCount != MAP37_ROW_COUNT
                        || !MAP37_SOURCE_SHA256.equals(sourceHash))) {
                throw new IOException("classic map37 NPC source contract changed");
            }
            if (mapId == ClassicWorldMapCatalog.VALAKAS_HABITAT_MAP_ID
                    && (rowCount != MAP67_ROW_COUNT
                        || !MAP67_SOURCE_SHA256.equals(sourceHash))) {
                throw new IOException("classic map67 NPC source contract changed");
            }
            if (mapId == ClassicWorldMapCatalog.GIRAN_TRADE_ZONE_MAP_ID
                    && (rowCount != MAP350_ROW_COUNT
                        || !MAP350_SOURCE_SHA256.equals(sourceHash))) {
                throw new IOException("classic map350 NPC source contract changed");
            }
            if (mapId == ClassicWorldMapCatalog.DARK_ELF_DUNGEON_MAP_ID
                    && (rowCount != MAP209_ROW_COUNT
                        || !MAP209_SOURCE_SHA256.equals(sourceHash))) {
                throw new IOException("classic map209 NPC source contract changed");
            }
            if (mapId == ClassicWorldMapCatalog.DARKMAR_DUNGEON_MAP_ID
                    && (rowCount != MAP213_ROW_COUNT
                        || !MAP213_SOURCE_SHA256.equals(sourceHash))) {
                throw new IOException("classic map213 NPC source contract changed");
            }
            if (mapId == ClassicWorldMapCatalog.SEPIA_DUNGEON_MAP_ID
                    && (rowCount != MAP302_ROW_COUNT
                        || !MAP302_SOURCE_SHA256.equals(sourceHash))) {
                throw new IOException("classic map302 NPC source contract changed");
            }
            if (mapId == ClassicWorldMapCatalog.TAINTED_LAND_MAP_ID
                    && (rowCount != MAP244_ROW_COUNT
                        || !MAP244_SOURCE_SHA256.equals(sourceHash))) {
                throw new IOException("classic map244 NPC source contract changed");
            }
            if (mapId == ClassicWorldMapCatalog.HIDDEN_DOCK_MAP_ID
                    && (rowCount != MAP445_ROW_COUNT
                        || !MAP445_SOURCE_SHA256.equals(sourceHash))) {
                throw new IOException("classic map445 NPC source contract changed");
            }
            if (mapId == ClassicWorldMapCatalog.PIRATE_ISLAND_SHIP_MAP_ID
                    && (rowCount != MAP447_ROW_COUNT
                        || !MAP447_SOURCE_SHA256.equals(sourceHash))) {
                throw new IOException("classic map447 NPC source contract changed");
            }
            SourceContract antCave = ANT_CAVE_SOURCE_CONTRACTS.get(mapId);
            if (antCave != null && (rowCount != antCave.rows()
                    || !antCave.sourceSha256().equals(sourceHash))) {
                throw new IOException("classic ant-cave NPC source contract changed " + mapId);
            }
            SourceContract knightCave = KNIGHT_CAVE_SOURCE_CONTRACTS.get(mapId);
            if (knightCave != null && (rowCount != knightCave.rows()
                    || !knightCave.sourceSha256().equals(sourceHash))) {
                throw new IOException("classic knight-cave NPC source contract changed " + mapId);
            }
            SourceContract gludioDungeon = GLUDIO_DUNGEON_SOURCE_CONTRACTS.get(mapId);
            if (gludioDungeon != null && (rowCount != gludioDungeon.rows()
                    || !gludioDungeon.sourceSha256().equals(sourceHash))) {
                throw new IOException("classic Gludio Dungeon NPC source contract changed "
                        + mapId);
            }
            SourceContract giranDungeon = GIRAN_DUNGEON_SOURCE_CONTRACTS.get(mapId);
            if (giranDungeon != null && (rowCount != giranDungeon.rows()
                    || !giranDungeon.sourceSha256().equals(sourceHash))) {
                throw new IOException("classic Giran Dungeon NPC source contract changed "
                        + mapId);
            }
            if (mapId == ClassicWorldMapCatalog.GIRAN_CASTLE_MAP_ID
                    && (rowCount != MAP52_ROW_COUNT
                        || !MAP52_SOURCE_SHA256.equals(sourceHash))) {
                throw new IOException("classic map52 NPC source contract changed");
            }
            SourceContract windawood = WINDAWOOD_CASTLE_SOURCE_CONTRACTS.get(mapId);
            if (windawood != null && (rowCount != windawood.rows()
                    || !windawood.sourceSha256().equals(sourceHash))) {
                throw new IOException(
                        "classic Windawood-castle NPC source contract changed " + mapId);
            }
            SourceContract kent = KENT_COMPONENT_SOURCE_CONTRACTS.get(mapId);
            if (kent != null && (rowCount != kent.rows()
                    || !kent.sourceSha256().equals(sourceHash))) {
                throw new IOException(
                        "classic Kent-component NPC source contract changed " + mapId);
            }
            ClassicWorldIvoryTowerCatalog.SourceContract ivoryTower =
                    IVORY_TOWER_SOURCE_CONTRACTS.get(mapId);
            if (ivoryTower != null && (rowCount != ivoryTower.rows()
                    || !ivoryTower.sourceSha256().equals(sourceHash))) {
                throw new IOException(
                        "classic Ivory-Tower NPC source contract changed " + mapId);
            }
            if (mapId == ClassicWorldDwarfCastleCatalog.MAP_ID
                    && (rowCount != MAP66_ROW_COUNT
                        || !MAP66_SOURCE_SHA256.equals(sourceHash))) {
                throw new IOException("classic map66 NPC source contract changed");
            }
            ClassicWorldAdenToiCatalog.ResourceContract adenToi =
                    ClassicWorldAdenToiCatalog.NPC_CONTRACTS.get(mapId);
            if (adenToi != null && (rowCount != adenToi.rows()
                    || !adenToi.sourceSha256().equals(sourceHash))) {
                throw new IOException(
                        "classic Aden/TOI NPC source contract changed " + mapId);
            }
            ClassicWorldHeineCatalog.SourceContract heine =
                    ClassicWorldHeineCatalog.NPC_CONTRACTS.get(mapId);
            if (heine != null && (rowCount != heine.rows()
                    || !heine.sourceSha256().equals(sourceHash))) {
                throw new IOException(
                        "classic Heine/Eva NPC source contract changed " + mapId);
            }
            ClassicWorldSilentCavernCatalog.SourceContract silent =
                    ClassicWorldSilentCavernCatalog.NPC_CONTRACTS.get(mapId);
            if (silent != null && (rowCount != silent.rows()
                    || !silent.sourceSha256().equals(sourceHash))) {
                throw new IOException(
                        "classic Silent Cavern NPC source contract changed " + mapId);
            }
            ClassicWorldRastabadOumCatalog.SourceContract rastabad =
                    ClassicWorldRastabadOumCatalog.NPC_CONTRACTS.get(mapId);
            if (rastabad != null && (rowCount != rastabad.rows()
                    || !rastabad.sourceSha256().equals(sourceHash))) {
                throw new IOException(
                        "classic Rastabad/Oum NPC source contract changed " + mapId);
            }
            ClassicWorldDiadradCatalog.SourceContract diadrad =
                    ClassicWorldDiadradCatalog.NPC_CONTRACTS.get(mapId);
            if (diadrad != null && (rowCount != diadrad.rows()
                    || !diadrad.sourceSha256().equals(sourceHash))) {
                throw new IOException(
                        "classic Diad service-core NPC source contract changed " + mapId);
            }
            ClassicWorldElementalGraveCatalog.SourceContract elementalGrave =
                    ClassicWorldElementalGraveCatalog.NPC_CONTRACTS.get(mapId);
            if (elementalGrave != null
                    && (rowCount != elementalGrave.rows()
                        || !elementalGrave.sourceSha256().equals(sourceHash))) {
                throw new IOException(
                        "classic Elemental-Grave NPC source contract changed " + mapId);
            }
            ClassicWorldPirateDungeonCatalog.SourceContract pirateDungeon =
                    ClassicWorldPirateDungeonCatalog.NPC_CONTRACTS.get(mapId);
            if (pirateDungeon != null
                    && (rowCount != pirateDungeon.rows()
                        || !pirateDungeon.sourceSha256().equals(sourceHash))) {
                throw new IOException(
                        "classic Pirate-dungeon NPC source contract changed " + mapId);
            }
            if (mapId == ClassicWorldLastabadGateCatalog.MAP_ID
                    && (rowCount != 0
                        || !ClassicWorldLastabadGateCatalog.NPC_SOURCE_SHA256
                                .equals(sourceHash))) {
                throw new IOException("classic Lastabad-gate NPC source contract changed");
            }

            List<StaticNpc> rows = parse(mapId, table);
            if (rows.size() != rowCount) {
                throw new IOException("classic NPC row count disagrees with header");
            }
            return List.copyOf(rows);
        } catch (IOException | IllegalArgumentException error) {
            throw new ExceptionInInitializerError(error);
        }
    }

    private static List<StaticNpc> parse(int mapId, byte[] table) throws IOException {
        List<StaticNpc> rows = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                new ByteArrayInputStream(table), StandardCharsets.UTF_8))) {
            if (!TSV_HEADER.equals(reader.readLine())) {
                throw new IOException("classic NPC TSV header changed");
            }
            int previousSpawnId = 0;
            for (String line; (line = reader.readLine()) != null;) {
                String[] field = line.split("\t", -1);
                if (field.length != 20) throw new IOException("classic NPC field count changed");
                int spawnId = integer(field[0]);
                if (spawnId <= previousSpawnId) {
                    throw new IOException("classic NPC spawn ids are not ordered");
                }
                previousSpawnId = spawnId;
                StaticNpc npc = new StaticNpc(
                        mapId, spawnId, objectId(spawnId), integer(field[1]),
                        integer(field[2]), integer(field[3]), integer(field[4]),
                        integer(field[5]), field[6], field[7], integer(field[8]),
                        integer(field[9]), integer(field[10]), integer(field[11]),
                        unsignedInteger(field[12]), integer(field[13]), integer(field[14]),
                        integer(field[15]), integer(field[16]), integer(field[17]) != 0,
                        field[18], field[19]);
                if (!ClassicWorldMapCatalog.forMap(mapId).contains(npc.x(), npc.y())) {
                    throw new IOException("classic NPC is outside imported map " + spawnId);
                }
                rows.add(npc);
            }
        }
        return rows;
    }

    private static int integer(String value) {
        return Integer.parseInt(value);
    }

    private static long unsignedInteger(String value) {
        return Long.parseLong(value);
    }

    private static String sha256(byte[] bytes) throws IOException {
        try {
            return HexFormat.of().withUpperCase().formatHex(
                    MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (NoSuchAlgorithmException impossible) {
            throw new IOException("SHA-256 is unavailable", impossible);
        }
    }
}
