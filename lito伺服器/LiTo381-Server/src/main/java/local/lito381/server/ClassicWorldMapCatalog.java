package local.lito381.server;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.GZIPInputStream;

/** Authoritative retained 3.81 classic-world collision maps. */
final class ClassicWorldMapCatalog {
    private static volatile LitoMapRepository externalMaps;
    /** Exact selected {@code mapids.markable=1} rows. */
    private static final Set<Integer> MARKABLE_MAP_IDS = Set.of(0, 4, 304, 6321);
    static final int TALKING_ISLAND_MAP_ID = 0;
    static final int ROYAL_TRIAL_CAVE_FIRST_MAP_ID = 1;
    static final int ROYAL_TRIAL_CAVE_SECOND_MAP_ID = 2;
    static final int UNDERSEA_TUNNEL_MAP_ID = 14;
    static final int UNDERSEA_TUNNEL_SEVENTH_FLOOR_MAP_ID = 13;
    static final int JIM_SECRET_ROOM_MAP_ID = 237;
    static final List<Integer> GLUDIO_DUNGEON_MAP_IDS = List.of(
            7, 8, 9, 10, 11, 12);
    static final List<Integer> GIRAN_DUNGEON_MAP_IDS = List.of(53, 54, 55, 56);
    static final int GIRAN_CASTLE_MAP_ID = 52;
    static final int KENT_CASTLE_MAP_ID = 15;
    static final List<Integer> KENT_OBER_DUNGEON_MAP_IDS = List.of(
            240, 241, 242, 243);
    static final List<Integer> KENT_COMPONENT_MAP_IDS = List.of(
            15, 240, 241, 242, 243);
    static final List<Integer> IVORY_TOWER_MAP_IDS =
            ClassicWorldIvoryTowerCatalog.MAP_IDS;
    static final List<Integer> ADEN_TOI_MAP_IDS =
            ClassicWorldAdenToiCatalog.MAP_IDS;
    static final List<Integer> HEINE_EVA_MAP_IDS =
            ClassicWorldHeineCatalog.MAP_IDS;
    static final List<Integer> SILENT_CAVERN_MAP_IDS =
            ClassicWorldSilentCavernCatalog.MAP_IDS;
    static final List<Integer> RASTABAD_OUM_MAP_IDS =
            ClassicWorldRastabadOumCatalog.MAP_IDS;
    static final List<Integer> DIADRAD_SERVICE_CORE_MAP_IDS =
            ClassicWorldDiadradCatalog.MAP_IDS;
    static final int DEMON_TEMPLE_MAP_ID = ClassicWorldDemonTempleCatalog.MAP_ID;
    static final List<Integer> ELEMENTAL_GRAVE_MAP_IDS =
            ClassicWorldElementalGraveCatalog.MAP_IDS;
    static final List<Integer> PIRATE_DUNGEON_MAP_IDS =
            ClassicWorldPirateDungeonCatalog.MAP_IDS;
    static final List<Integer> PIRATE_BACK_HALF_MAP_IDS =
            ClassicWorldPirateBackHalfCatalog.MAP_IDS;
    static final int LASTABAD_GATE_MAP_ID = ClassicWorldLastabadGateCatalog.MAP_ID;
    static final int REGENERATED_TEMPLE_MAP_ID = 2000;
    static final int WATER_DRAGON_LAIR_MAP_ID = 1011;
    static final int STRANGE_VILLAGE_MAP_ID = 613;
    static final List<Integer> WINDAWOOD_CASTLE_MAP_IDS = List.of(23, 24, 29);
    static final int DWARF_CASTLE_CAVE_MAP_ID =
            ClassicWorldDwarfCastleCatalog.MAP_ID;
    static final int WINDAWOOD_CASTLE_DUNGEON_FIRST_FLOOR_MAP_ID = 23;
    static final int WINDAWOOD_CASTLE_DUNGEON_SECOND_FLOOR_MAP_ID = 24;
    static final int WINDAWOOD_CASTLE_MAP_ID = 29;
    static final int GUNTER_HOUSE_MAP_ID = 3;
    static final int TALKING_ISLAND_TO_GLUDIO_SHIP_MAP_ID = 5;
    static final int GLUDIO_TO_TALKING_ISLAND_SHIP_MAP_ID = 6;
    static final int FORGOTTEN_ISLAND_MAP_ID = 70;
    static final int MAINLAND_TO_FORGOTTEN_ISLAND_SHIP_MAP_ID = 83;
    static final int FORGOTTEN_ISLAND_TO_MAINLAND_SHIP_MAP_ID = 84;
    static final List<Integer> FORGOTTEN_ISLAND_ROUTE_MAP_IDS = List.of(
            FORGOTTEN_ISLAND_MAP_ID,
            MAINLAND_TO_FORGOTTEN_ISLAND_SHIP_MAP_ID,
            FORGOTTEN_ISLAND_TO_MAINLAND_SHIP_MAP_ID);
    static final List<Integer> MAINLAND_SHIP_MAP_IDS = List.of(
            TALKING_ISLAND_MAP_ID,
            TALKING_ISLAND_TO_GLUDIO_SHIP_MAP_ID,
            GLUDIO_TO_TALKING_ISLAND_SHIP_MAP_ID);
    static final int WORLD_TREE_MAP_ID = 4;
    static final int HELL_MAP_ID = ClassicWorldHell.MAP_ID;
    static final int WORLD_TREE_X = 33051;
    static final int WORLD_TREE_Y = 32337;
    static final int WORLD_TREE_HEADING = 5;
    static final int HARTIN_LABORATORY_MAP_ID = 16;
    static final int ORC_ELVEN_DUNGEON_MAP_ID = 21;
    static final int ORC_ELVEN_DUNGEON_X = 32760;
    static final int ORC_ELVEN_DUNGEON_Y = 32761;
    static final int ORC_ELVEN_DUNGEON_HEADING = 5;
    static final int GERARD_TRIAL_DUNGEON_MAP_ID = 22;
    static final int GERARD_TRIAL_DUNGEON_X = 32769;
    static final int GERARD_TRIAL_DUNGEON_Y = 32767;
    static final int GERARD_TRIAL_DUNGEON_HEADING = 5;
    static final List<Integer> KNIGHT_CAVE_MAP_IDS = List.of(25, 26, 27, 28);
    static final List<Integer> ANT_CAVE_MAP_IDS = List.of(
            43, 44, 45, 46, 47, 48, 49, 50, 51, 541, 542, 543);
    static final int DRAGON_VALLEY_DUNGEON_FIRST_FLOOR_MAP_ID = 30;
    static final int DRAGON_VALLEY_DUNGEON_SECOND_FLOOR_MAP_ID = 31;
    static final int DRAGON_VALLEY_DUNGEON_THIRD_FLOOR_MAP_ID = 32;
    static final int DRAGON_VALLEY_DUNGEON_FOURTH_FLOOR_MAP_ID = 33;
    static final int DRAGON_VALLEY_DUNGEON_FIFTH_FLOOR_MAP_ID = 35;
    static final int DRAGON_VALLEY_DUNGEON_SIXTH_FLOOR_MAP_ID = 36;
    static final int ANTHARAS_LAIR_MAP_ID = 37;
    static final int VALAKAS_HABITAT_MAP_ID = 67;
    static final int NERUPA_CAVE_MAP_ID = 17;
    static final int NERUPA_CAVE_X = 32747;
    static final int NERUPA_CAVE_Y = 32809;
    static final int NERUPA_CAVE_HEADING = 6;
    static final int SLEEPING_DRAGON_FIRST_FLOOR_MAP_ID = 19;
    static final int SLEEPING_DRAGON_SECOND_FLOOR_MAP_ID = 20;
    static final int DARK_ELF_DUNGEON_MAP_ID = 209;
    static final int DARKMAR_DUNGEON_MAP_ID = 213;
    static final int FAIRY_DUNGEON_X = 32727;
    static final int FAIRY_DUNGEON_Y = 32799;
    static final int FAIRY_DUNGEON_HEADING = 5;
    static final int SEPIA_DUNGEON_MAP_ID = 302;
    static final int SEPIA_DUNGEON_X = 32740;
    static final int SEPIA_DUNGEON_Y = 32860;
    static final int SEPIA_DUNGEON_HEADING = 5;
    static final int PHANTASM_ISLAND_MAP_ID = 303;
    static final int PHANTASM_ISLAND_X = 32635;
    static final int PHANTASM_ISLAND_Y = 32817;
    static final int PHANTASM_ISLAND_HEADING = 5;
    static final int TAINTED_LAND_MAP_ID = 244;
    static final int TAINTED_LAND_X = 32982;
    static final int TAINTED_LAND_Y = 32808;
    static final int TAINTED_LAND_HEADING = 5;
    static final int GIRAN_TRADE_ZONE_MAP_ID = 350;
    static final int GIRAN_TRADE_ZONE_X = 32702;
    static final int GIRAN_TRADE_ZONE_Y = 32842;
    static final int GIRAN_TRADE_ZONE_HEADING = 5;
    static final int HIDDEN_DOCK_MAP_ID = 445;
    static final int HIDDEN_DOCK_SHIP_MAP_ID = 446;
    static final int HIDDEN_DOCK_X = 32738;
    static final int HIDDEN_DOCK_Y = 32857;
    static final int HIDDEN_DOCK_HEADING = 5;
    static final int PIRATE_ISLAND_SHIP_MAP_ID = 447;
    static final List<Integer> HOTEL_MAP_IDS = ClassicWorldHotelCatalog.MAP_IDS;
    static final String MAP0_SOURCE_SHA256 =
            "ECE432F3AF3CC20BCBF2722DEA31A379A9E6EE2B05D7DB8940F35CA52EC6BC44";
    static final String MAP1_SOURCE_SHA256 =
            "607E200E02ECD4BE8869CD963A8CD1FFBB2D8CBA4491471C6A2ED0CC38FA0206";
    static final String MAP2_SOURCE_SHA256 =
            "CBAC99CA93544A888FB5056FD3744744A789AACD4613E56C9DC7B7BD2FD050DD";
    static final String MAP14_SOURCE_SHA256 =
            "A06D0A5030C23EDB3AFA753B4FB11D98511C361CCE9F36F94B5C05D5693BE4B8";
    static final String MAP13_SOURCE_SHA256 =
            "37030C3209C340DEFF87C69AF82FB9372DEEDB6BE2A64B3997BC9D6959AE34D1";
    static final String MAP237_SOURCE_SHA256 =
            "16CC9C011687F58DB18F6CBB21E6EAB18AA794C8AED2F8384644D77AF2672494";
    static final String MAP3_SOURCE_SHA256 =
            "BDFE858C0DBC1D64F98A5ED0D1F8587B7E95AC51FB38449F2C7F91AC0E456C00";
    static final String MAP5_SOURCE_SHA256 =
            "8FEA5668F74B129C5A7FEE7EB4F7115D594713800E08A2A4DD8D9273D00E37B7";
    static final String MAP6_SOURCE_SHA256 =
            "020D4A177B8719BD26C86C4BBD8CF85FBE9A5C7FAAEB320F27A233990474F107";
    static final String MAP0_RESOURCE_SHA256 =
            "53F1A321812BF1153F4E6B5ED6932D9E67AA19C7CD9D022570DCC77A00A93DFD";
    static final String MAP1_RESOURCE_SHA256 =
            "44EBA43219D02EE38BC7743949875F566CB8076958405C0BF40015547A46A382";
    static final String MAP2_RESOURCE_SHA256 =
            "6A63486265CB6B629E8A6E8BEAF3A5326EB3501923EFB376781E7D02EB2E7EBC";
    static final String MAP14_RESOURCE_SHA256 =
            "C51186C25DC91856064A8F4290156F5E6C6E5E069D744FCD9189C32628D1788F";
    static final String MAP13_RESOURCE_SHA256 =
            "E14EF3D91E0C82E97998E9EE06F16BD7F07CFA0429EDE43B6872979D763C5880";
    static final String MAP237_RESOURCE_SHA256 =
            "B1F3FF44580EFF65925A02EE4C59EBE4829C3DFC1B31E18023268B613660A998";
    static final String MAP237_FLAGS_SOURCE_SHA256 =
            "3DDA0B3E991DB861BC2286131995BAB71A2C5E9FBD5BEF36B9A8FD73C1E78100";
    static final String MAP237_GETBACK_SOURCE_SHA256 =
            "8CE0E2D71DBD5BBEE8F6B0885E1E3BD3B7B71F50D50AA13FFD7F403A33A770EB";
    static final String GLUDIO_DUNGEON_FLAGS_SOURCE_SHA256 =
            "652D9C1C7BEE7FFA9D0DBA8B90AAECBB637AE489E1B35A98024D52C556128BD8";
    static final String GLUDIO_DUNGEON_FLAGS_SECONDARY_SOURCE_SHA256 =
            "E6748C9F230A5D4D5D5D4F117EC626C69D2E3381D52FA6E5FA689450622B4E62";
    static final String GLUDIO_DUNGEON_GETBACK_SOURCE_SHA256 =
            "3F3ED5B79CB30EF701931C76945244D6CB0789543AC1D99410D6632222A97B8E";
    static final Map<Integer, String> GLUDIO_DUNGEON_MAP_SOURCE_SHA256 = Map.of(
            7, "30DC27CD22F7362D50BA3DC4C825BA7FB1E39986957F255A41B545068E4CF5BC",
            8, "2D032D98B96B6F390FD529F400E8A7E62857839ECF00F932518E6ACFEEB3608D",
            9, "F62B0E0E5A92B431980C1F7AED48752B3A700A2F45E01019BC4D355196D8C950",
            10, "DCB7CA8024A74731DCFAEBDA24272143D60D1FE995A05A48FD00DA3BCC07ECFB",
            11, "24AEFEC51D0AC296049C65EC63B78661378DC3328D169B5EC390DA21CC6782DF",
            12, "EF6675D842BC99BB5D219673B074A9E9DA0BDE8EB8C08592C9DBF0E37B34B537");
    static final Map<Integer, String> GLUDIO_DUNGEON_MAP_RESOURCE_SHA256 = Map.of(
            7, "DBD23A98CC0AD10E02CD95ABE0C36521411EEEEF136327E9BD4C52FDA4A2179D",
            8, "35843D2E2D6DF0245952A5177F3D11EB52056C9C19451EC9ED8E40B316E4C7FB",
            9, "42F9D686EDDBA338DCD7B43D7A07E26A36DAFD2D51DDAA4074E550AFF3A3C083",
            10, "30C7D9DC396F98D0623E39420B1673AF55634952DF377322D87528EF3E868EDB",
            11, "F11E6F98641B9CBA3175981AC806D7BCDEDDCB60EAF67A6251DE1DF347B26238",
            12, "17EAEFF250244E67755A26F3BF75BB2B58443819E2456537492EA2918114D662");
    static final String GIRAN_DUNGEON_FLAGS_SOURCE_SHA256 =
            "CC51597B17D7603875D282BD74F0ABB775A51531A739FA4F8EB1D9C51E596ECD";
    static final String GIRAN_DUNGEON_FLAGS_SECONDARY_SOURCE_SHA256 =
            "D4FCB89DE78D4473DCA5B1F6C54B6F4B61C9C92D4B6B45756DF29A8DEFE47E39";
    static final String GIRAN_DUNGEON_GETBACK_SOURCE_SHA256 =
            "6CDB4C1182BE8234177A69D0FA82628CC5E80E68272768957E3C9F315C23C52E";
    static final Map<Integer, String> GIRAN_DUNGEON_MAP_SOURCE_SHA256 = Map.of(
            53, "AF5F342BB4585FBCA859555BA4A65EFC1C894C9DC54A8D438136CCACAF0D342D",
            54, "9D0352B2A125A09BD9BA8061ECB3A66BA9CF6BB008DF97B4E264063BCAFF09C4",
            55, "E00AF91A1C23DB7549A5B23688F20746F80E9ABE34E99A37E6D11E632F5FC56D",
            56, "09D8F9158BBBF705866E2BE2A6898FF74C6FC476C690616202059B8A7D35453C");
    static final Map<Integer, String> GIRAN_DUNGEON_MAP_RESOURCE_SHA256 = Map.of(
            53, "F96F82F54556264349515A0606D16744AE04B18677F09BEA92BAF1CB47435C55",
            54, "18545A7F8035C612947C3AF7D323BAB19659756A2D94911C17E613FFD241AE73",
            55, "EA35F65B4481A053A232900ED31D6A5B44ACF727C67A0ACC7EDCB277F9747EC6",
            56, "F852E27F549C3A6E1FD345F9EDECC28A41546726F6095294C32D58B66508C85A");
    static final String MAP52_SOURCE_SHA256 =
            "6833EB32DBE035390D9FC15343663CE9BAAA7C7C1770A40DA084E27CF76C98E3";
    static final String MAP52_RESOURCE_SHA256 =
            "5F5CA963C46F5F4A9FDE0FEB83CE7949F793F082C734E82E4D5ED583D2709DFB";
    static final String MAP52_FLAGS_SOURCE_SHA256 =
            "3BEF00DFDFC1C43DE310EABB20FF411BFACE77A20D1F6AC721F20E79E7E280D5";
    static final String MAP52_GETBACK_SOURCE_SHA256 =
            "40C796F598E56CD5C814F9171D714E6BB18BAFD29C09986FCF55743A6156EAA2";
    static final Map<Integer, String> KENT_COMPONENT_MAP_SOURCE_SHA256 = Map.of(
            15, "37DED79A6A1997E4B08FC573E6AE6189756E710050A0174903E6C981F08B9D87",
            240, "E852F9796CC33F9B32A44BA103934B3E85AC94D6E899F7706E310F2BF90C3F4D",
            241, "B7208A8341641B23E967E24B243FB0C0EB904CF46FF18DD9A4B20F210FB46CF7",
            242, "53546BA16B0A23B1582E81532F0F8F9B4E064EB486BF6609136DA5CDB529940F",
            243, "302D791D83A68DC6B17BF8A46655101E34532BEF2103058C7127D465804031A2");
    static final Map<Integer, String> KENT_COMPONENT_MAP_RESOURCE_SHA256 = Map.of(
            15, "58FD2891AD513AED9A918043EF1ED9FE4E63BBA7C44334D1086F3F99CA528714",
            240, "F7063A97518258CCC203AB3AB382850901F61F3C82EACB700C3EBC1DE3ABA6D4",
            241, "9D4643FA40E160E90F6910AA039D7C4726E79F42C7FA0EE16D3F3595FC63A583",
            242, "1BB3478536387C75D318260E73561AC5A8705465A67873BE620743F7E212AE35",
            243, "8A1ACD3F9B39442B8615356870BBF6D51D6D11CA2971C28965A1FA4C9538E3A3");
    static final String KENT_COMPONENT_FLAGS_SOURCE_SHA256 =
            "55E4726136D2DD2BB0B081FA3F182FCB23B72C1D001D6FB3D1E9D85E1E72037C";
    static final String KENT_COMPONENT_FLAGS_SECONDARY_SOURCE_SHA256 =
            "B9D96FF8A7394DD1166374DBF394F98121FFB11401C454F8BC6547B7A5484455";
    static final String KENT_COMPONENT_GETBACK_SOURCE_SHA256 =
            "31CD21444DACB2C87C7BB6C2EF451D9B6226678C3F26C4AEBA089E4E6033BBD8";
    static final Map<Integer, String> IVORY_TOWER_MAP_SOURCE_SHA256 =
            ClassicWorldIvoryTowerCatalog.MAP_SOURCE_SHA256;
    static final Map<Integer, String> IVORY_TOWER_MAP_RESOURCE_SHA256 =
            ClassicWorldIvoryTowerCatalog.MAP_RESOURCE_SHA256;
    static final String IVORY_TOWER_FLAGS_SOURCE_SHA256 =
            ClassicWorldIvoryTowerCatalog.FLAGS_SOURCE_SHA256;
    static final String IVORY_TOWER_GETBACK_SOURCE_SHA256 =
            ClassicWorldIvoryTowerCatalog.GETBACK_SOURCE_SHA256;
    static final Map<Integer, String> ADEN_TOI_MAP_SOURCE_SHA256 =
            ClassicWorldAdenToiCatalog.MAP_SOURCE_SHA256;
    static final Map<Integer, String> ADEN_TOI_MAP_RESOURCE_SHA256 =
            ClassicWorldAdenToiCatalog.MAP_RESOURCE_SHA256;
    static final String ADEN_TOI_FLAGS_SOURCE_SHA256 =
            ClassicWorldAdenToiCatalog.FLAGS_SOURCE_SHA256;
    static final String ADEN_TOI_GETBACK_SOURCE_SHA256 =
            ClassicWorldAdenToiCatalog.GETBACK_SOURCE_SHA256;
    static final Map<Integer, String> WINDAWOOD_CASTLE_MAP_SOURCE_SHA256 = Map.of(
            23, "021BFF8DBB06EAC2996FA53FF81B0674F3E7334F6DAF14A69AF6A8805AB924DC",
            24, "8ED31A762C585E48A653D5AD27416E2DD8B0156F8DCF0D5EDED94696AF1F851D",
            29, "B22EB771F1C55359236A7D102910CFC6E3E895F76959C98CADEE98D676AE8CBE");
    static final Map<Integer, String> WINDAWOOD_CASTLE_MAP_RESOURCE_SHA256 = Map.of(
            23, "28E0EBEFEC187D6779F464C3F2CA83737076E2BBDE7B5594122A4410D3FAC091",
            24, "3799A53A88A61B18AEE66AC3CDA1CB8CDDEECF87273A2616C0F6F64319293C03",
            29, "E6876F2E5C347C5FB03CA450983A321AB502022D2BB3DA62327E08DF088A1841");
    static final String WINDAWOOD_CASTLE_FLAGS_SOURCE_SHA256 =
            "7E1A9C604E4FA5066A144AAFAE16CBBF0F3118EF5AF7AE967606E4263CC47612";
    static final String WINDAWOOD_CASTLE_FLAGS_SECONDARY_SOURCE_SHA256 =
            "B092CB3AF4D016E32909314314E923BB11EE0767055E2623262BECA358AA2B46";
    static final String WINDAWOOD_CASTLE_GETBACK_SOURCE_SHA256 =
            "BAC44DBF688A38A2679DFEC3179F1C7EE919C8D1AEB3311816E76A5BCB000D4A";
    static final String MAP66_SOURCE_SHA256 =
            ClassicWorldDwarfCastleCatalog.MAP_SOURCE_SHA256;
    static final String MAP66_RESOURCE_SHA256 =
            ClassicWorldDwarfCastleCatalog.MAP_RESOURCE_SHA256;
    static final String MAP66_FLAGS_SOURCE_SHA256 =
            ClassicWorldDwarfCastleCatalog.MAP_FLAGS_SOURCE_SHA256;
    static final String MAP66_GETBACK_SOURCE_SHA256 =
            ClassicWorldDwarfCastleCatalog.MAP_GETBACK_SOURCE_SHA256;
    static final String MAP3_RESOURCE_SHA256 =
            "70A22F49D506C80169E497B9125DC9AFE4B542EFF3B6A3B5FF9292D24CDFB208";
    static final String MAP3_FLAGS_SOURCE_SHA256 =
            "800187B442AEF7EA69DF6C9345224EF5E33ECDFD46D7064DACC458F20ADC6A6B";
    static final String MAP3_GETBACK_SOURCE_SHA256 =
            ClassicWorldGetbackCatalog.MAP3_SOURCE_SHA256;
    static final String MAP5_RESOURCE_SHA256 =
            "7B7A907459C92C6EEEC65953F799A2082220AFD929FD10E5FC3BF709931C330B";
    static final String MAP6_RESOURCE_SHA256 =
            "1436C20720FB7FDB3008A8770C43B8A68856C6F2E3F2D4B6C489A951B1598C8E";
    static final String MAP70_SOURCE_SHA256 =
            "227F8434B0B6E39039EE859E67BC81610ED9F0D03CCF4DFA0A4DBF1EBE5670B2";
    static final String MAP83_SOURCE_SHA256 = MAP6_SOURCE_SHA256;
    static final String MAP84_SOURCE_SHA256 = MAP5_SOURCE_SHA256;
    static final String MAP70_RESOURCE_SHA256 =
            "2248FCCBDCBA57DCD0E88DC7AB7657869373084B780C716930FB5162B7AE4128";
    static final String MAP83_RESOURCE_SHA256 =
            "0E4AE2D8FDDBCE62CD9A428074F617619B1009CB809A9B683F82D5BB13241C73";
    static final String MAP84_RESOURCE_SHA256 =
            "FC2632FA403D177D6F14D6776E27F929EFB168DC826E4DB606AE401399FDFE2A";
    static final String FORGOTTEN_ISLAND_FLAGS_SOURCE_SHA256 =
            "10048B2E32C5897D075F362EEE02B357C118B2C03AB5E981B424D37A91CF2DD2";
    static final String FORGOTTEN_ISLAND_GETBACK_SOURCE_SHA256 =
            "2B420AC3956D183D069A7549841578724661630363C3944A3114C4B8D3862C2C";
    static final String MAP4_SOURCE_SHA256 =
            "A7E1747E46E4E4ACBB8DFC69848A3CF79B3AB1D3BEED82DDEE623D1D9BA95CAE";
    static final String MAP16_SOURCE_SHA256 =
            "9CACD6AB3A4A3B580D29A150B27A13090A2C502796793D110B0E782DD6FB3B2D";
    static final String MAP16_FLAGS_SOURCE_SHA256 =
            "2D71D90845F149A26A077889151D470EAE010E25D768821A9F038995B2ABA029";
    static final String MAP16_FLAGS_SECONDARY_SOURCE_SHA256 =
            "C07F37209557A17829E4C5CF9EB1248CAFF4F7B6B60F2140539347D3D53740F3";
    static final String MAP17_SOURCE_SHA256 =
            "4D97B6168DFAE69DB984F92B856D388C7F837792F428F217BAF51675B4A146FF";
    static final String MAP17_FLAGS_SOURCE_SHA256 =
            "0A0FBD109734A88039EDDE821D1BB8DBC55C3821E407F4073134BFFD7D28BD68";
    static final String MAP17_GETBACK_SOURCE_SHA256 =
            "6AF1307BD1298612896C24808E084D78D5C61977AC9058AF37C97FF3B946A05B";
    static final String MAP19_SOURCE_SHA256 =
            "A881C8663179AD1646B1A898B408701481D68C6A57495584F2ED1C1084E913EF";
    static final String MAP20_SOURCE_SHA256 =
            "E30F62CA1B21F7A155DF971C0AA70A534FD7550CAD8EB190F7A3A5CC9DA44A4A";
    static final String SLEEPING_DRAGON_FLAGS_SOURCE_SHA256 =
            "B5506FADA78E41375E2796DF4539021D17C7670755009031078B49844CE0C57B";
    static final String MAP21_SOURCE_SHA256 =
            "DD5478DA22D90E689296E9D3C014AC71D94A0AE86BF78FCD49AAA5AC8BD317DC";
    static final String MAP21_FLAGS_SOURCE_SHA256 =
            "2972CB1E5C130C075A4CBFF9E32D78CD7F71B4FEB17598F46FF2D5BB80AC8E1B";
    static final String MAP22_SOURCE_SHA256 =
            "0AF75942C1EAF9E0FAF89EBF853B62BDC71D877B402FDB4545268B5F3A0FF81C";
    static final String MAP22_FLAGS_SOURCE_SHA256 =
            "F69A2CB1905914484F21258FEA5EC2AFCEB29AFD28941D7BA89439C5B8370873";
    static final String MAP22_FLAGS_SECONDARY_SOURCE_SHA256 =
            "33AA4565F93AEA1117AB15412F84BE2003FE830199D63374CF0669E600A98EE3";
    static final String MAP22_GETBACK_SOURCE_SHA256 =
            "5EA42DBE903B1C662A6A9CFDF913F52D642D2C29D9CF7EBE77CEE306B9B2B78D";
    static final String KNIGHT_CAVE_FLAGS_SOURCE_SHA256 =
            "57CCC55086E8C34ADE2EBA7EF57D7FAE619753813A36E4928AFB49F3967F3703";
    static final String KNIGHT_CAVE_FLAGS_SECONDARY_SOURCE_SHA256 =
            "3B6BFE370B611CF5713322FFF71E7D6EB1E13FAA1868E951384D42E3C4D10B35";
    static final Map<Integer, String> KNIGHT_CAVE_MAP_SOURCE_SHA256 = Map.of(
            25, "9EE326AEB05C965DA42403FA0DC2B3C2FF6882AD77FB70CFE8979BC613FDFEF6",
            26, "0C6DE14B5B24317365999F74D03CA6229E9603E7352BC04ED66A994632037EE8",
            27, "0709449E9F82E3B2543950351FF8000557D689D2785E5D826ACF1B443E3DAF8B",
            28, "F77FA622AB52CE68F4BB92B93C56F2212B992EB20A7BC3A67C1612B90700077A");
    static final String ANT_CAVE_FLAGS_SOURCE_SHA256 =
            "51D221D9B22B3B13897FABEBC420207E73560AC22A7706DF3F2FB44B1119A7A9";
    static final String ANT_CAVE_GETBACK_SOURCE_SHA256 =
            "5E82372EFB4784CACFCEAA7FBA86513FEE86E0CD07923A47ABE046E8A81CE2BE";
    static final Map<Integer, String> ANT_CAVE_MAP_SOURCE_SHA256 = Map.ofEntries(
            Map.entry(43, "03A41B50AD71AA2586CDD47AD8D617F65929407A8ECDBDC93457C7DE814A64A4"),
            Map.entry(44, "718B51FE7E49B050CE03EDB590E92B83C8471D8F44E66FED8DF8FBA814A37C44"),
            Map.entry(45, "1E9AA806B430EDF04B52D5933EBA44DEF96248654C4BE19C0737C00108ECCA2F"),
            Map.entry(46, "8A6FC6469B69119BB27AB2052E206DC52E11194E8934FCFFF32F722B390F6A95"),
            Map.entry(47, "F741000DB18930757F2FF36CFE38F063A4CE41348AF784CEA25BAD15D8166F29"),
            Map.entry(48, "404A319001DC8E6D0715B3325235E96B5115384765C428CED5A236C818130CE2"),
            Map.entry(49, "68978A9E10CF351F891B5E993B60C1AFEA7ED05B1F324A14F9877BAA7E77832A"),
            Map.entry(50, "52A43E1596E675169CFE7811A6AE1E7EE828F949116CF8A607A3C68BE8EC1DDD"),
            Map.entry(51, "0F59425854F55A143C3A1E063327F859CD45101C3B0C35E7DCAD0AB53FA78851"),
            Map.entry(541, "6FF435345EC31F012ADDB26ED175889B19539AEF996BF14EF82CB706B93DD8BF"),
            Map.entry(542, "38EC6BF689A3057C355B8118DA9D468E9F0B82E3BF3033D86660BD0F38FAFCA2"),
            Map.entry(543, "1D5A02952A4A2CB3971E827891EFB6A9D026FCCF528DAE39ACB3D149CA995B84"));
    static final String MAP30_SOURCE_SHA256 =
            "6C85CDE838B383B9394AE12D5206FF9090D1AC2C524B05522239A6F3165E859B";
    static final String MAP30_FLAGS_SOURCE_SHA256 =
            "E2AB7E0CEA8D31BCA75540C2CECB5950917C5EE27C7577AD661AF8158191AC90";
    static final String MAP31_SOURCE_SHA256 =
            "7C2B45164739099EF9C5B10C8F86CCBDB8222FA6C0EE3454F1BD95649810C252";
    static final String MAP31_FLAGS_SOURCE_SHA256 =
            "398C54FE7C6F1F5E0C8DA4A5ECA21CCB5F1E96CC0ACAA8676143824321D86481";
    static final String MAP32_SOURCE_SHA256 =
            "299C4B519A40834B9761CCA44C1834B23D36A0A0F675B9D39618BA57F2E3ED96";
    static final String MAP32_FLAGS_SOURCE_SHA256 =
            "B71315FA5C1C09246CD2DF03CD8BC7377DD16003653D0BEF03A7F05F3D47F745";
    static final String MAP33_SOURCE_SHA256 =
            "93C763D46EFD572CF961EC47BD9A8113F6C8490265BA9FA8375DC906BE4A8381";
    static final String MAP33_FLAGS_SOURCE_SHA256 =
            "734C00AC6C6DBDE91947B4151678BCFAC562C60415A1147290B53564DBC2F392";
    static final String DRAGON_VALLEY_DUNGEON_FLAGS_SOURCE_SHA256 =
            "A25177B40B68BB3BEA736E9D1410A819936A2D03B0DE3CD6B346E169FA102EDD";
    static final String DRAGON_VALLEY_DUNGEON_FLAGS_SECONDARY_SOURCE_SHA256 =
            "DB84DC1F5333CF341B455104A3AC49F8146494562F0588FC8A1F20DED7742F4C";
    static final String DRAGON_VALLEY_DUNGEON_FLOORS_1_TO_4_FLAGS_SOURCE_SHA256 =
            "A234F9C0D2B75C2E1E002D0B921E8BE5734A3CC7CB726AB04EBB57C0C1A319E3";
    static final String DRAGON_VALLEY_DUNGEON_FLOORS_1_TO_4_FLAGS_SECONDARY_SOURCE_SHA256 =
            "963F368AC6D4823352437E4CD973DCEE591E76DCEC0703CD7A3002125CAF816C";
    static final String MAP35_SOURCE_SHA256 =
            "108C5C10D70396D68D5284DE912ACD1A97718DA0AB303E5BBE13BF797906AC0B";
    static final String MAP35_FLAGS_SOURCE_SHA256 =
            "0FD52FEB7C844AFE5ABB092B42583A1757A4EE8D70BB8726127909BBEDF6DF78";
    static final String MAP36_SOURCE_SHA256 =
            "3FE2DDA27FA7D31FC78459DA9AB3DD81792514BF936EC1F76EA99A862F7EE820";
    static final String MAP36_FLAGS_SOURCE_SHA256 =
            "D8629ACF496C12CEF65A4B00FA4D5DFC6ECC397D1E8CE861344BC0F508C2D219";
    static final String MAP37_SOURCE_SHA256 =
            "908DCFC256461974B1EEBC6BB38CD1322AF9C33AF17094D6141ED38D2B15D53D";
    static final String MAP37_FLAGS_SOURCE_SHA256 =
            "C8284A823B783B188956E49F16ECC440C8AC9913C8992F84469C5F8BBECF9EF4";
    static final String MAP67_SOURCE_SHA256 =
            "A4C22680348540BD77AF7A8EA708214C7E46515D86FE10894DDD4B499E23BAA2";
    static final String MAP67_FLAGS_SOURCE_SHA256 =
            "B2B3F217275C2FC8188DC1A1602167297B7F2E99D9C633B6CDEB60387C63E2AE";
    static final String DRAGON_VALLEY_DEEP_FLAGS_SOURCE_SHA256 =
            "9C4AD03705ACC812EA5824575316F7DBB344A066A8A32F2CBC682E7AA42BF27F";
    static final String DRAGON_VALLEY_DEEP_FLAGS_SECONDARY_SOURCE_SHA256 =
            "087C6DE240C9C7883086964B36F1D68A4FDF42938ACF7B1D7F45C89E83F30D40";
    static final String MAP209_SOURCE_SHA256 =
            "D611F8CA4C15BA9E59071858103CF7BCC7ADE2A0AC81A0D6DFDFDA8110337088";
    static final String MAP213_SOURCE_SHA256 =
            "3409EAD1F02A4105B61B020D7AABB3EDA504837E47F390553023E4E0CBDF1E06";
    static final String FAIRY_DUNGEON_FLAGS_SOURCE_SHA256 =
            "D02C8CE782A94E0C30AC6FB2F0155BCC3B36F5522E7025ED1D630069277855F2";
    static final String MAP302_SOURCE_SHA256 =
            "301534D948C572E2B5A22B7205FD55E4BCF954F74019457683EC543F5CCB8D08";
    static final String MAP302_FLAGS_SOURCE_SHA256 =
            "9D091C820FC90D166B17B8F268B6D8FF1FC6E4A6851B04DD437CC30D7A02B30D";
    static final String MAP303_SOURCE_SHA256 =
            "25337614DECA2D9707F1F68EC199C4BEF315A00E1CFB6B1460A7BFC561689D56";
    static final String MAP244_SOURCE_SHA256 =
            "8B2E7521E3F4BA6A801D7EF2E21A6697373BA346910A175BFD868183102DC603";
    static final String MAP244_FLAGS_SOURCE_SHA256 =
            "8AA4FB6AFEAFD7426F81F11819CC82C395A5C0CA9D913CF357FAF352227496F6";
    static final String MAP244_GETBACK_SOURCE_SHA256 =
            "B112C7B1A028AA18237E9BF9AD385503772965157013D974AC17CA441E51E559";
    static final String MAP350_SOURCE_SHA256 =
            "668BA1585A08631B0EAB1B2BDFDF9FE953C6628CDABA23BCB10882D578C87EBF";
    static final String MAP350_FLAGS_SOURCE_SHA256 =
            "BCC4FF31697E82364AF6CD88D7DAFDDBDA68F3321262E6D496D6CECE26DCA9DB";
    static final String MAP445_SOURCE_SHA256 =
            "FE48DB5DF1F4F50BAF177586B9448396FB0CB07A75980F27618E5034CFE28D95";
    static final String MAP445_FLAGS_SOURCE_SHA256 =
            "74F54017AC7C6CDB19B8469A97DAF2177813F64F1AE5EA4617483511861F251D";
    static final String MAP446_SOURCE_SHA256 =
            "020D4A177B8719BD26C86C4BBD8CF85FBE9A5C7FAAEB320F27A233990474F107";
    static final String MAP446_RESOURCE_SHA256 =
            "4CD5BE5712AC1D093A1F9E3014248DBAA9EE9C626A79EB514043F62AED8B5AC0";
    static final String MAP447_SOURCE_SHA256 =
            "8FEA5668F74B129C5A7FEE7EB4F7115D594713800E08A2A4DD8D9273D00E37B7";
    static final String MAP447_FLAGS_SOURCE_SHA256 =
            ClassicWorldDungeonCatalog.MAP447_FLAGS_SOURCE_SHA256;
    static final String ESCAPE_FLAGS_SOURCE_SHA256 =
            "A1F65C00C884BE5B8E8F7B05CB416C6E3F611D5D6B3FFA179A539F4FC4685ECA";
    static final String MAP2000_SOURCE_SHA256 =
            "92A563BF9097B647FEC020FC28A34B151DDAA5EA932A448BC8F963659AD52E88";
    static final String MAP2000_RESOURCE_SHA256 =
            "B53A63645A5AA3E9B5DC069CFD1C9AD8E737983FB6098A422C04B00653D2B40E";
    static final String MAP1011_SOURCE_SHA256 =
            "25CDF28088A5B7BA34F940C9B748D862685C3D3D653AA0D86101E901DA309492";
    static final String MAP1011_RESOURCE_SHA256 =
            "E4DFA4C7E57C9B5D5E6F34D6E06068D8019C9A13A068FDCAC8895815C78BA9EA";
    static final String MAP613_SOURCE_SHA256 =
            "F03B9CE022A2509408F89FA91D157A594E73945B3F6E355E874F0F3865FAC89A";
    static final String MAP613_RESOURCE_SHA256 =
            "9CD319540EC7BC638E912E74925CDDBC10C798E2A44FF7D1D07B43F48E93A598";
    static final String MAP666_SOURCE_SHA256 =
            "2DF903D97C556E5ECBA2A9527C6D3CCC1B371B474A4FC69E7599570F1EF8E90C";
    static final String MAP666_RESOURCE_SHA256 =
            "959F3AAA574319EAEE35F05443099E4C3474AB7309CCAF46B84E1D35F1BE4072";

    private static final int MAGIC = 0x4C544D31; // LTM1
    private static final int HEADER_BYTES = 4 + 2 + (6 * 4) + 32;
    private static final int[] HEADING_X = {0, 1, 1, 1, 0, -1, -1, -1};
    private static final int[] HEADING_Y = {-1, -1, 0, 1, 1, 1, 0, -1};
    private static final List<Integer> SUPPORTED_MAP_IDS = List.of(
            GUNTER_HOUSE_MAP_ID, WORLD_TREE_MAP_ID,
            HARTIN_LABORATORY_MAP_ID, NERUPA_CAVE_MAP_ID,
            SLEEPING_DRAGON_FIRST_FLOOR_MAP_ID,
            SLEEPING_DRAGON_SECOND_FLOOR_MAP_ID, ORC_ELVEN_DUNGEON_MAP_ID,
            GERARD_TRIAL_DUNGEON_MAP_ID, 25, 26, 27, 28,
            43, 44, 45, 46, 47, 48, 49, 50, 51, 541, 542, 543,
            DRAGON_VALLEY_DUNGEON_FIRST_FLOOR_MAP_ID,
            DRAGON_VALLEY_DUNGEON_SECOND_FLOOR_MAP_ID,
            DRAGON_VALLEY_DUNGEON_THIRD_FLOOR_MAP_ID,
            DRAGON_VALLEY_DUNGEON_FOURTH_FLOOR_MAP_ID,
            DRAGON_VALLEY_DUNGEON_FIFTH_FLOOR_MAP_ID,
            DRAGON_VALLEY_DUNGEON_SIXTH_FLOOR_MAP_ID,
            ANTHARAS_LAIR_MAP_ID, VALAKAS_HABITAT_MAP_ID,
            DARK_ELF_DUNGEON_MAP_ID, DARKMAR_DUNGEON_MAP_ID,
            SEPIA_DUNGEON_MAP_ID, PHANTASM_ISLAND_MAP_ID, TAINTED_LAND_MAP_ID,
            GIRAN_TRADE_ZONE_MAP_ID,
            HIDDEN_DOCK_MAP_ID, PIRATE_ISLAND_SHIP_MAP_ID,
            TALKING_ISLAND_MAP_ID, TALKING_ISLAND_TO_GLUDIO_SHIP_MAP_ID,
            GLUDIO_TO_TALKING_ISLAND_SHIP_MAP_ID,
            FORGOTTEN_ISLAND_MAP_ID,
            MAINLAND_TO_FORGOTTEN_ISLAND_SHIP_MAP_ID,
            FORGOTTEN_ISLAND_TO_MAINLAND_SHIP_MAP_ID,
            // Added after every 0.7.331 map so its selected actor ordinals stay stable.
            ROYAL_TRIAL_CAVE_FIRST_MAP_ID, ROYAL_TRIAL_CAVE_SECOND_MAP_ID,
            UNDERSEA_TUNNEL_MAP_ID,
            // Appended after every 0.7.332 map to preserve all shipped IDs.
            UNDERSEA_TUNNEL_SEVENTH_FLOOR_MAP_ID,
            // Empty population leaf appended after 0.7.333 for stable ordering.
            JIM_SECRET_ROOM_MAP_ID,
            // Entire connected map7-12 component is appended after 0.7.334.
            // MonsterPopulation also appends its actors after every shipped ID.
            7, 8, 9, 10, 11, 12,
            // Entire Giran Dungeon component is appended after 0.7.335.
            53, 54, 55, 56,
            // Giran Castle interior is appended after every 0.7.336 map.
            GIRAN_CASTLE_MAP_ID,
            // Windawood is appended after every 0.7.337 map.
            23, 24, 29,
            // Kent Castle and all four Ober floors are appended after 0.7.338.
            15, 240, 241, 242, 243,
            // Native Ivory Tower component is appended after every 0.7.339 map.
            75, 76, 77, 78, 79, 80, 81, 82,
            // Dwarf Castle/Cave is appended after every 0.7.340 map.
            DWARF_CASTLE_CAVE_MAP_ID,
            // Aden interior, TOI 1-10F and waterway append after 0.7.341.
            101, 102, 103, 104, 105, 106, 107, 108, 109, 110, 300, 301,
            // Heine/Eva appends after every prior component so native object
            // ordinals already proved by .342 remain stable.
            59, 60, 61, 62, 63, 64, 65, 252, 253, 254,
            // These .344-.347 components were already loaded in MAPS and had
            // complete populations, but were omitted from the scheduler list.
            304, 305, 307, 308, 309, 310, 320, 330, 400, 401,
            DEMON_TEMPLE_MAP_ID,
            // Shared four-class regenerated-temple instance appends after all
            // previously shipped map ordinals.
            REGENERATED_TEMPLE_MAP_ID,
            // Selected map450 is appended after every .360 ordinal.  Its source
            // population is explicitly empty; later Lastabad floors stay gated.
            LASTABAD_GATE_MAP_ID,
            // Quest117 instance map appends after every .373 actor/map ordinal.
            WATER_DRAGON_LAIR_MAP_ID,
            // Quest117's retained gate map follows its instance map.
            STRANGE_VILLAGE_MAP_ID,
            // Abyss Lake / Elemental Grave and the Pirate Island landing are
            // appended after every 0.7.375 ordinal.
            420, 430, 440,
            // Pirate Island dungeon 1F-4F appends after every 0.7.376 ordinal.
            441, 442, 443, 444,
            // Pirate back-half, Kamyla quest, and Ship Grave are append-only.
            480, 481, 482, 483, 484,
            550, 551, 552, 553, 554, 555, 556, 557, 558,
            // Hidden-dock ship map appends after every 0.7.379 ordinal.
            HIDDEN_DOCK_SHIP_MAP_ID,
            // Nine native inns (room then meeting hall) append in retained
            // innkeeper order after every sealed 0.7.380 map ordinal.
            16384, 16896, 17408, 17920, 18432, 18944,
            19456, 19968, 20480, 20992, 21504, 22016,
            22528, 23040, 23552, 24064, 24576, 25088,
            // Desire Cave and Abandoned Land append after every sealed .381
            // ordinal, preserving all previously shipped runtime object IDs.
            600, 601, 602, 603, 604, 605, 606, 607, 778, 779,
            // Native Dragon Knight / Illusionist home towns append after the
            // sealed .382 ordinals, so existing runtime object ids stay fixed.
            1000, 1001,
            // Retained PK punishment map appends after every sealed .790 id.
            HELL_MAP_ID);
    private static final Map<Integer, ClassicMap> MAPS = Map.ofEntries(
            Map.entry(TALKING_ISLAND_MAP_ID, load("/maps/0.l38.gz")),
            Map.entry(ROYAL_TRIAL_CAVE_FIRST_MAP_ID, load("/maps/1.l38.gz")),
            Map.entry(ROYAL_TRIAL_CAVE_SECOND_MAP_ID, load("/maps/2.l38.gz")),
            Map.entry(UNDERSEA_TUNNEL_MAP_ID, load("/maps/14.l38.gz")),
            Map.entry(UNDERSEA_TUNNEL_SEVENTH_FLOOR_MAP_ID,
                    load("/maps/13.l38.gz")),
            Map.entry(JIM_SECRET_ROOM_MAP_ID, load("/maps/237.l38.gz")),
            Map.entry(7, load("/maps/7.l38.gz")),
            Map.entry(8, load("/maps/8.l38.gz")),
            Map.entry(9, load("/maps/9.l38.gz")),
            Map.entry(10, load("/maps/10.l38.gz")),
            Map.entry(11, load("/maps/11.l38.gz")),
            Map.entry(12, load("/maps/12.l38.gz")),
            Map.entry(53, load("/maps/53.l38.gz")),
            Map.entry(54, load("/maps/54.l38.gz")),
            Map.entry(55, load("/maps/55.l38.gz")),
            Map.entry(56, load("/maps/56.l38.gz")),
            Map.entry(GIRAN_CASTLE_MAP_ID, load("/maps/52.l38.gz")),
            Map.entry(23, load("/maps/23.l38.gz")),
            Map.entry(24, load("/maps/24.l38.gz")),
            Map.entry(29, load("/maps/29.l38.gz")),
            Map.entry(15, load("/maps/15.l38.gz")),
            Map.entry(240, load("/maps/240.l38.gz")),
            Map.entry(241, load("/maps/241.l38.gz")),
            Map.entry(242, load("/maps/242.l38.gz")),
            Map.entry(243, load("/maps/243.l38.gz")),
            Map.entry(75, load("/maps/75.l38.gz")),
            Map.entry(76, load("/maps/76.l38.gz")),
            Map.entry(77, load("/maps/77.l38.gz")),
            Map.entry(78, load("/maps/78.l38.gz")),
            Map.entry(79, load("/maps/79.l38.gz")),
            Map.entry(80, load("/maps/80.l38.gz")),
            Map.entry(81, load("/maps/81.l38.gz")),
            Map.entry(82, load("/maps/82.l38.gz")),
            Map.entry(DWARF_CASTLE_CAVE_MAP_ID, load("/maps/66.l38.gz")),
            Map.entry(101, load("/maps/101.l38.gz")),
            Map.entry(102, load("/maps/102.l38.gz")),
            Map.entry(103, load("/maps/103.l38.gz")),
            Map.entry(104, load("/maps/104.l38.gz")),
            Map.entry(105, load("/maps/105.l38.gz")),
            Map.entry(106, load("/maps/106.l38.gz")),
            Map.entry(107, load("/maps/107.l38.gz")),
            Map.entry(108, load("/maps/108.l38.gz")),
            Map.entry(109, load("/maps/109.l38.gz")),
            Map.entry(110, load("/maps/110.l38.gz")),
            Map.entry(300, load("/maps/300.l38.gz")),
            Map.entry(301, load("/maps/301.l38.gz")),
            Map.entry(59, load("/maps/59.l38.gz")),
            Map.entry(60, load("/maps/60.l38.gz")),
            Map.entry(61, load("/maps/61.l38.gz")),
            Map.entry(62, load("/maps/62.l38.gz")),
            Map.entry(63, load("/maps/63.l38.gz")),
            Map.entry(64, load("/maps/64.l38.gz")),
            Map.entry(65, load("/maps/65.l38.gz")),
            Map.entry(252, load("/maps/252.l38.gz")),
            Map.entry(253, load("/maps/253.l38.gz")),
            Map.entry(254, load("/maps/254.l38.gz")),
            Map.entry(304, load("/maps/304.l38.gz")),
            Map.entry(305, load("/maps/305.l38.gz")),
            Map.entry(307, load("/maps/307.l38.gz")),
            Map.entry(308, load("/maps/308.l38.gz")),
            Map.entry(309, load("/maps/309.l38.gz")),
            Map.entry(310, load("/maps/310.l38.gz")),
            Map.entry(320, load("/maps/320.l38.gz")),
            Map.entry(330, load("/maps/330.l38.gz")),
            Map.entry(400, load("/maps/400.l38.gz")),
            Map.entry(401, load("/maps/401.l38.gz")),
            Map.entry(DEMON_TEMPLE_MAP_ID, load("/maps/410.l38.gz")),
            Map.entry(420, load("/maps/420.l38.gz")),
            Map.entry(430, load("/maps/430.l38.gz")),
            Map.entry(440, load("/maps/440.l38.gz")),
            Map.entry(441, load("/maps/441.l38.gz")),
            Map.entry(442, load("/maps/442.l38.gz")),
            Map.entry(443, load("/maps/443.l38.gz")),
            Map.entry(444, load("/maps/444.l38.gz")),
            Map.entry(480, load("/maps/480.l38.gz")),
            Map.entry(481, load("/maps/481.l38.gz")),
            Map.entry(482, load("/maps/482.l38.gz")),
            Map.entry(483, load("/maps/483.l38.gz")),
            Map.entry(484, load("/maps/484.l38.gz")),
            Map.entry(550, load("/maps/550.l38.gz")),
            Map.entry(551, load("/maps/551.l38.gz")),
            Map.entry(552, load("/maps/552.l38.gz")),
            Map.entry(553, load("/maps/553.l38.gz")),
            Map.entry(554, load("/maps/554.l38.gz")),
            Map.entry(555, load("/maps/555.l38.gz")),
            Map.entry(556, load("/maps/556.l38.gz")),
            Map.entry(557, load("/maps/557.l38.gz")),
            Map.entry(558, load("/maps/558.l38.gz")),
            Map.entry(600, load("/maps/600.l38.gz")),
            Map.entry(601, load("/maps/601.l38.gz")),
            Map.entry(602, load("/maps/602.l38.gz")),
            Map.entry(603, load("/maps/603.l38.gz")),
            Map.entry(604, load("/maps/604.l38.gz")),
            Map.entry(605, load("/maps/605.l38.gz")),
            Map.entry(606, load("/maps/606.l38.gz")),
            Map.entry(607, load("/maps/607.l38.gz")),
            Map.entry(778, load("/maps/778.l38.gz")),
            Map.entry(779, load("/maps/779.l38.gz")),
            Map.entry(LASTABAD_GATE_MAP_ID, load("/maps/450.l38.gz")),
            Map.entry(REGENERATED_TEMPLE_MAP_ID, load("/maps/2000.l38.gz")),
            Map.entry(WATER_DRAGON_LAIR_MAP_ID, load("/maps/1011.l38.gz")),
            Map.entry(STRANGE_VILLAGE_MAP_ID, load("/maps/613.l38.gz")),
            Map.entry(GUNTER_HOUSE_MAP_ID, load("/maps/3.l38.gz")),
            Map.entry(TALKING_ISLAND_TO_GLUDIO_SHIP_MAP_ID,
                    load("/maps/5.l38.gz")),
            Map.entry(GLUDIO_TO_TALKING_ISLAND_SHIP_MAP_ID,
                    load("/maps/6.l38.gz")),
            Map.entry(FORGOTTEN_ISLAND_MAP_ID, load("/maps/70.l38.gz")),
            Map.entry(MAINLAND_TO_FORGOTTEN_ISLAND_SHIP_MAP_ID,
                    load("/maps/83.l38.gz")),
            Map.entry(FORGOTTEN_ISLAND_TO_MAINLAND_SHIP_MAP_ID,
                    load("/maps/84.l38.gz")),
            Map.entry(WORLD_TREE_MAP_ID, load("/maps/4.l38.gz")),
            Map.entry(HARTIN_LABORATORY_MAP_ID, load("/maps/16.l38.gz")),
            Map.entry(NERUPA_CAVE_MAP_ID, load("/maps/17.l38.gz")),
            Map.entry(SLEEPING_DRAGON_FIRST_FLOOR_MAP_ID,
                    load("/maps/19.l38.gz")),
            Map.entry(SLEEPING_DRAGON_SECOND_FLOOR_MAP_ID,
                    load("/maps/20.l38.gz")),
            Map.entry(ORC_ELVEN_DUNGEON_MAP_ID, load("/maps/21.l38.gz")),
            Map.entry(GERARD_TRIAL_DUNGEON_MAP_ID, load("/maps/22.l38.gz")),
            Map.entry(25, load("/maps/25.l38.gz")),
            Map.entry(26, load("/maps/26.l38.gz")),
            Map.entry(27, load("/maps/27.l38.gz")),
            Map.entry(28, load("/maps/28.l38.gz")),
            Map.entry(43, load("/maps/43.l38.gz")),
            Map.entry(44, load("/maps/44.l38.gz")),
            Map.entry(45, load("/maps/45.l38.gz")),
            Map.entry(46, load("/maps/46.l38.gz")),
            Map.entry(47, load("/maps/47.l38.gz")),
            Map.entry(48, load("/maps/48.l38.gz")),
            Map.entry(49, load("/maps/49.l38.gz")),
            Map.entry(50, load("/maps/50.l38.gz")),
            Map.entry(51, load("/maps/51.l38.gz")),
            Map.entry(541, load("/maps/541.l38.gz")),
            Map.entry(542, load("/maps/542.l38.gz")),
            Map.entry(543, load("/maps/543.l38.gz")),
            Map.entry(DRAGON_VALLEY_DUNGEON_FIRST_FLOOR_MAP_ID,
                    load("/maps/30.l38.gz")),
            Map.entry(DRAGON_VALLEY_DUNGEON_SECOND_FLOOR_MAP_ID,
                    load("/maps/31.l38.gz")),
            Map.entry(DRAGON_VALLEY_DUNGEON_THIRD_FLOOR_MAP_ID,
                    load("/maps/32.l38.gz")),
            Map.entry(DRAGON_VALLEY_DUNGEON_FOURTH_FLOOR_MAP_ID,
                    load("/maps/33.l38.gz")),
            Map.entry(DRAGON_VALLEY_DUNGEON_FIFTH_FLOOR_MAP_ID,
                    load("/maps/35.l38.gz")),
            Map.entry(DRAGON_VALLEY_DUNGEON_SIXTH_FLOOR_MAP_ID,
                    load("/maps/36.l38.gz")),
            Map.entry(ANTHARAS_LAIR_MAP_ID, load("/maps/37.l38.gz")),
            Map.entry(VALAKAS_HABITAT_MAP_ID, load("/maps/67.l38.gz")),
            Map.entry(DARK_ELF_DUNGEON_MAP_ID, load("/maps/209.l38.gz")),
            Map.entry(DARKMAR_DUNGEON_MAP_ID, load("/maps/213.l38.gz")),
            Map.entry(SEPIA_DUNGEON_MAP_ID, load("/maps/302.l38.gz")),
            Map.entry(PHANTASM_ISLAND_MAP_ID, load("/maps/303.l38.gz")),
            Map.entry(TAINTED_LAND_MAP_ID, load("/maps/244.l38.gz")),
            Map.entry(GIRAN_TRADE_ZONE_MAP_ID, load("/maps/350.l38.gz")),
            Map.entry(HIDDEN_DOCK_MAP_ID, load("/maps/445.l38.gz")),
            Map.entry(HIDDEN_DOCK_SHIP_MAP_ID, load("/maps/446.l38.gz")),
            Map.entry(PIRATE_ISLAND_SHIP_MAP_ID, load("/maps/447.l38.gz")),
            Map.entry(16384, load("/maps/16384.l38.gz")),
            Map.entry(16896, load("/maps/16896.l38.gz")),
            Map.entry(17408, load("/maps/17408.l38.gz")),
            Map.entry(17920, load("/maps/17920.l38.gz")),
            Map.entry(18432, load("/maps/18432.l38.gz")),
            Map.entry(18944, load("/maps/18944.l38.gz")),
            Map.entry(19456, load("/maps/19456.l38.gz")),
            Map.entry(19968, load("/maps/19968.l38.gz")),
            Map.entry(20480, load("/maps/20480.l38.gz")),
            Map.entry(20992, load("/maps/20992.l38.gz")),
            Map.entry(21504, load("/maps/21504.l38.gz")),
            Map.entry(22016, load("/maps/22016.l38.gz")),
            Map.entry(22528, load("/maps/22528.l38.gz")),
            Map.entry(23040, load("/maps/23040.l38.gz")),
            Map.entry(23552, load("/maps/23552.l38.gz")),
            Map.entry(24064, load("/maps/24064.l38.gz")),
            Map.entry(24576, load("/maps/24576.l38.gz")),
            Map.entry(25088, load("/maps/25088.l38.gz")),
            Map.entry(1000, load("/maps/1000.l38.gz")),
            Map.entry(1001, load("/maps/1001.l38.gz")),
            Map.entry(HELL_MAP_ID, load("/maps/666.l38.gz")));

    static final class ClassicMap {
        /** Exact retained {@code L1V1Map} bits 0x30 classification. */
        enum ZoneType { NORMAL, SAFETY, COMBAT }

        private final int mapId;
        private final int startX;
        private final int endX;
        private final int startY;
        private final int endY;
        private final int width;
        private final int height;
        private final String sourceSha256;
        private final byte[] cells;
        private final LitoMapRepository.Definition externalDefinition;

        private ClassicMap(int mapId, int startX, int endX, int startY, int endY,
                           int width, int height, String sourceSha256, byte[] cells) {
            this(mapId, startX, endX, startY, endY, width, height,
                    sourceSha256, cells, null);
        }

        private ClassicMap(int mapId, int startX, int endX, int startY, int endY,
                           int width, int height, String sourceSha256, byte[] cells,
                           LitoMapRepository.Definition externalDefinition) {
            this.mapId = mapId;
            this.startX = startX;
            this.endX = endX;
            this.startY = startY;
            this.endY = endY;
            this.width = width;
            this.height = height;
            this.sourceSha256 = sourceSha256;
            this.cells = cells;
            this.externalDefinition = externalDefinition;
        }

        int mapId() { return mapId; }
        int startX() { return startX; }
        int endX() { return endX; }
        int startY() { return startY; }
        int endY() { return endY; }
        int width() { return width; }
        int height() { return height; }
        String sourceSha256() { return sourceSha256; }

        boolean contains(int x, int y) {
            return x >= startX && x <= endX && y >= startY && y <= endY;
        }

        int rawCell(int x, int y) {
            if (!contains(x, y)) return 0;
            return cells[(y - startY) * width + (x - startX)] & 0xFF;
        }

        ZoneType zoneType(int x, int y) {
            return switch (rawCell(x, y) & 0x30) {
                case 0x10 -> ZoneType.SAFETY;
                case 0x20 -> ZoneType.COMBAT;
                default -> ZoneType.NORMAL;
            };
        }

        boolean isSafetyZone(int x, int y) {
            return zoneType(x, y) == ZoneType.SAFETY;
        }

        boolean isCombatZone(int x, int y) {
            return zoneType(x, y) == ZoneType.COMBAT;
        }

        boolean isNormalZone(int x, int y) {
            return zoneType(x, y) == ZoneType.NORMAL;
        }

        /** Exact selected {@code mapids.penalty}; unknown maps fail closed. */
        boolean allowsDeathPenalty() {
            if (externalDefinition != null) return externalDefinition.deathPenalty();
            return ClassicWorldDeathPenaltyCatalog.allowsSelectedMap(mapId, true);
        }

        /** Exact retained L1V1Map standing rule for static map data. */
        boolean standingPassable(int x, int y) {
            int raw = rawCell(x, y);
            return raw != 0 && (raw & 0x80) == 0 && (raw & 0x03) != 0;
        }

        /** Selected {@code 380_db.mapids}: map 350 denies new pet/summon recall. */
        boolean allowsPetRecall() {
            if (externalDefinition != null) return externalDefinition.recallPets();
            if (ClassicWorldBehemothSilveriaCatalog.supports(mapId)) {
                return ClassicWorldBehemothSilveriaCatalog.flags(mapId).recallPets();
            }
            if (ClassicWorldDesireCaveCatalog.supports(mapId)) {
                return ClassicWorldDesireCaveCatalog.flags(mapId).recallPets();
            }
            if (PIRATE_BACK_HALF_MAP_IDS.contains(mapId)) {
                return ClassicWorldPirateBackHalfCatalog.flags(mapId).recallPets();
            }
            if (PIRATE_DUNGEON_MAP_IDS.contains(mapId)) {
                return ClassicWorldPirateDungeonCatalog.flags(mapId).recallPets();
            }
            if (ELEMENTAL_GRAVE_MAP_IDS.contains(mapId)) {
                return ClassicWorldElementalGraveCatalog.flags(mapId).recallPets();
            }
            if (HEINE_EVA_MAP_IDS.contains(mapId)) {
                return ClassicWorldHeineCatalog.flags(mapId).recallPets();
            }
            return ANT_CAVE_MAP_IDS.contains(mapId) || KNIGHT_CAVE_MAP_IDS.contains(mapId)
                    || HOTEL_MAP_IDS.contains(mapId)
                    || GLUDIO_DUNGEON_MAP_IDS.contains(mapId)
                    || GIRAN_DUNGEON_MAP_IDS.contains(mapId)
                    || mapId == GIRAN_CASTLE_MAP_ID
                    || WINDAWOOD_CASTLE_MAP_IDS.contains(mapId)
                    || KENT_COMPONENT_MAP_IDS.contains(mapId)
                    || IVORY_TOWER_MAP_IDS.contains(mapId)
                    || ADEN_TOI_MAP_IDS.contains(mapId)
                    || HEINE_EVA_MAP_IDS.contains(mapId)
                    || SILENT_CAVERN_MAP_IDS.contains(mapId)
                    || RASTABAD_OUM_MAP_IDS.contains(mapId)
                    || DIADRAD_SERVICE_CORE_MAP_IDS.contains(mapId)
                    || mapId == DEMON_TEMPLE_MAP_ID
                    || mapId == LASTABAD_GATE_MAP_ID
                    || mapId == REGENERATED_TEMPLE_MAP_ID
                    || mapId == WATER_DRAGON_LAIR_MAP_ID
                    || mapId == DWARF_CASTLE_CAVE_MAP_ID
                    || mapId == ROYAL_TRIAL_CAVE_FIRST_MAP_ID
                    || mapId == ROYAL_TRIAL_CAVE_SECOND_MAP_ID
                    || mapId == UNDERSEA_TUNNEL_MAP_ID
                    || mapId == UNDERSEA_TUNNEL_SEVENTH_FLOOR_MAP_ID
                    || mapId == JIM_SECRET_ROOM_MAP_ID
                    || MAINLAND_SHIP_MAP_IDS.contains(mapId)
                    || FORGOTTEN_ISLAND_ROUTE_MAP_IDS.contains(mapId)
                    || mapId == GUNTER_HOUSE_MAP_ID
                    || mapId == WORLD_TREE_MAP_ID
                    || mapId == HELL_MAP_ID
                    || mapId == HARTIN_LABORATORY_MAP_ID
                    || mapId == NERUPA_CAVE_MAP_ID
                    || mapId == SLEEPING_DRAGON_FIRST_FLOOR_MAP_ID
                    || mapId == SLEEPING_DRAGON_SECOND_FLOOR_MAP_ID
                    || mapId == ORC_ELVEN_DUNGEON_MAP_ID
                    || mapId == GERARD_TRIAL_DUNGEON_MAP_ID
                    || mapId == DRAGON_VALLEY_DUNGEON_FIRST_FLOOR_MAP_ID
                    || mapId == DRAGON_VALLEY_DUNGEON_SECOND_FLOOR_MAP_ID
                    || mapId == DRAGON_VALLEY_DUNGEON_THIRD_FLOOR_MAP_ID
                    || mapId == DRAGON_VALLEY_DUNGEON_FOURTH_FLOOR_MAP_ID
                    || mapId == DRAGON_VALLEY_DUNGEON_FIFTH_FLOOR_MAP_ID
                    || mapId == DRAGON_VALLEY_DUNGEON_SIXTH_FLOOR_MAP_ID
                    || mapId == ANTHARAS_LAIR_MAP_ID
                    || mapId == VALAKAS_HABITAT_MAP_ID
                    || mapId == DARK_ELF_DUNGEON_MAP_ID
                    || mapId == DARKMAR_DUNGEON_MAP_ID
                    || mapId == SEPIA_DUNGEON_MAP_ID
                    || mapId == PHANTASM_ISLAND_MAP_ID
                    || mapId == TAINTED_LAND_MAP_ID
                    || mapId == HIDDEN_DOCK_MAP_ID
                    || mapId == HIDDEN_DOCK_SHIP_MAP_ID
                    || mapId == PIRATE_ISLAND_SHIP_MAP_ID;
        }

        /** Exact selected mapids rows: every imported classic map has escapable=1. */
        boolean allowsEscape() {
            if (externalDefinition != null) return externalDefinition.escapable();
            if (ClassicWorldBehemothSilveriaCatalog.supports(mapId)) {
                return ClassicWorldBehemothSilveriaCatalog.flags(mapId).escapable();
            }
            if (ClassicWorldDesireCaveCatalog.supports(mapId)) {
                return ClassicWorldDesireCaveCatalog.flags(mapId).escapable();
            }
            if (PIRATE_BACK_HALF_MAP_IDS.contains(mapId)) return true;
            if (PIRATE_DUNGEON_MAP_IDS.contains(mapId)) {
                return ClassicWorldPirateDungeonCatalog.flags(mapId).escapable();
            }
            if (ELEMENTAL_GRAVE_MAP_IDS.contains(mapId)) {
                return ClassicWorldElementalGraveCatalog.flags(mapId).escapable();
            }
            return ANT_CAVE_MAP_IDS.contains(mapId) || KNIGHT_CAVE_MAP_IDS.contains(mapId)
                    || HOTEL_MAP_IDS.contains(mapId)
                    || GLUDIO_DUNGEON_MAP_IDS.contains(mapId)
                    || GIRAN_DUNGEON_MAP_IDS.contains(mapId)
                    || mapId == GIRAN_CASTLE_MAP_ID
                    || WINDAWOOD_CASTLE_MAP_IDS.contains(mapId)
                    || KENT_COMPONENT_MAP_IDS.contains(mapId)
                    || IVORY_TOWER_MAP_IDS.contains(mapId)
                    || ADEN_TOI_MAP_IDS.contains(mapId)
                    || HEINE_EVA_MAP_IDS.contains(mapId)
                    || SILENT_CAVERN_MAP_IDS.contains(mapId)
                    || RASTABAD_OUM_MAP_IDS.contains(mapId)
                    || DIADRAD_SERVICE_CORE_MAP_IDS.contains(mapId)
                    || mapId == DEMON_TEMPLE_MAP_ID
                    || mapId == LASTABAD_GATE_MAP_ID
                    || mapId == REGENERATED_TEMPLE_MAP_ID
                    || mapId == WATER_DRAGON_LAIR_MAP_ID
                    || mapId == STRANGE_VILLAGE_MAP_ID
                    || mapId == DWARF_CASTLE_CAVE_MAP_ID
                    || mapId == ROYAL_TRIAL_CAVE_FIRST_MAP_ID
                    || mapId == ROYAL_TRIAL_CAVE_SECOND_MAP_ID
                    || mapId == UNDERSEA_TUNNEL_MAP_ID
                    || mapId == UNDERSEA_TUNNEL_SEVENTH_FLOOR_MAP_ID
                    || mapId == JIM_SECRET_ROOM_MAP_ID
                    || MAINLAND_SHIP_MAP_IDS.contains(mapId)
                    || FORGOTTEN_ISLAND_ROUTE_MAP_IDS.contains(mapId)
                    || mapId == GUNTER_HOUSE_MAP_ID
                    || mapId == WORLD_TREE_MAP_ID
                    || mapId == HELL_MAP_ID
                    || mapId == HARTIN_LABORATORY_MAP_ID
                    || mapId == NERUPA_CAVE_MAP_ID
                    || mapId == SLEEPING_DRAGON_FIRST_FLOOR_MAP_ID
                    || mapId == SLEEPING_DRAGON_SECOND_FLOOR_MAP_ID
                    || mapId == ORC_ELVEN_DUNGEON_MAP_ID
                    || mapId == GERARD_TRIAL_DUNGEON_MAP_ID
                    || mapId == DRAGON_VALLEY_DUNGEON_FIRST_FLOOR_MAP_ID
                    || mapId == DRAGON_VALLEY_DUNGEON_SECOND_FLOOR_MAP_ID
                    || mapId == DRAGON_VALLEY_DUNGEON_THIRD_FLOOR_MAP_ID
                    || mapId == DRAGON_VALLEY_DUNGEON_FOURTH_FLOOR_MAP_ID
                    || mapId == DRAGON_VALLEY_DUNGEON_FIFTH_FLOOR_MAP_ID
                    || mapId == DRAGON_VALLEY_DUNGEON_SIXTH_FLOOR_MAP_ID
                    || mapId == ANTHARAS_LAIR_MAP_ID
                    || mapId == VALAKAS_HABITAT_MAP_ID
                    || mapId == DARK_ELF_DUNGEON_MAP_ID
                    || mapId == DARKMAR_DUNGEON_MAP_ID
                    || mapId == SEPIA_DUNGEON_MAP_ID
                    || mapId == PHANTASM_ISLAND_MAP_ID
                    || mapId == TAINTED_LAND_MAP_ID
                    || mapId == GIRAN_TRADE_ZONE_MAP_ID
                    || mapId == HIDDEN_DOCK_MAP_ID
                    || mapId == HIDDEN_DOCK_SHIP_MAP_ID
                    || mapId == PIRATE_ISLAND_SHIP_MAP_ID;
        }

        /** Exact selected {@code mapids.markable}; required by C_BOOKMARK. */
        boolean allowsBookmark() {
            if (externalDefinition != null) return externalDefinition.markable();
            return MARKABLE_MAP_IDS.contains(mapId);
        }

        /** Selected mapids.teleportable; custom-area item routes remain explicit exceptions. */
        boolean allowsRandomTeleport() {
            if (externalDefinition != null) return externalDefinition.teleportable();
            if (ClassicWorldBehemothSilveriaCatalog.supports(mapId)) {
                return ClassicWorldBehemothSilveriaCatalog.flags(mapId).teleportable();
            }
            if (ClassicWorldDesireCaveCatalog.supports(mapId)) {
                return ClassicWorldDesireCaveCatalog.flags(mapId).teleportable();
            }
            if (PIRATE_BACK_HALF_MAP_IDS.contains(mapId)) {
                return ClassicWorldPirateBackHalfCatalog.flags(mapId).teleportable();
            }
            if (PIRATE_DUNGEON_MAP_IDS.contains(mapId)) {
                return ClassicWorldPirateDungeonCatalog.flags(mapId).teleportable();
            }
            if (ELEMENTAL_GRAVE_MAP_IDS.contains(mapId)) {
                return ClassicWorldElementalGraveCatalog.flags(mapId).teleportable();
            }
            if (HEINE_EVA_MAP_IDS.contains(mapId)) {
                return ClassicWorldHeineCatalog.flags(mapId).teleportable();
            }
            if (RASTABAD_OUM_MAP_IDS.contains(mapId)) {
                return ClassicWorldRastabadOumCatalog.flags(mapId).teleportable();
            }
            if (DIADRAD_SERVICE_CORE_MAP_IDS.contains(mapId)) {
                return ClassicWorldDiadradCatalog.flags(mapId).teleportable();
            }
            if (mapId == DEMON_TEMPLE_MAP_ID) {
                return ClassicWorldDemonTempleCatalog.flags().teleportable();
            }
            if (mapId == LASTABAD_GATE_MAP_ID) {
                return ClassicWorldLastabadGateCatalog.flags().teleportable();
            }
            if (mapId == REGENERATED_TEMPLE_MAP_ID) return false;
            if (mapId == WATER_DRAGON_LAIR_MAP_ID) return false;
            if (mapId == STRANGE_VILLAGE_MAP_ID) return false;
            if (mapId == HIDDEN_DOCK_SHIP_MAP_ID) return false;
            if (HOTEL_MAP_IDS.contains(mapId)) return false;
            return mapId != JIM_SECRET_ROOM_MAP_ID
                    && mapId != DWARF_CASTLE_CAVE_MAP_ID
                    && !KENT_OBER_DUNGEON_MAP_IDS.contains(mapId)
                    && (!IVORY_TOWER_MAP_IDS.contains(mapId)
                        || ClassicWorldIvoryTowerCatalog.flags(mapId).teleportable());
        }

        /** Exact retained L1V1Map eight-direction static edge rule. */
        boolean canStep(int fromX, int fromY, int toX, int toY) {
            int heading = heading(fromX, fromY, toX, toY);
            if (heading < 0 || !contains(fromX, fromY) || !standingPassable(toX, toY)) {
                return false;
            }
            int source = rawCell(fromX, fromY);
            int destination = rawCell(toX, toY);
            return switch (heading) {
                case 0 -> (source & 0x02) != 0;
                case 2 -> (source & 0x01) != 0;
                case 4 -> (destination & 0x02) != 0;
                case 6 -> (destination & 0x01) != 0;
                case 1, 3, 5, 7 -> (destination & 0x03) != 0;
                default -> false;
            };
        }

        /**
         * True when an exact source landing cell has at least one legal first step.
         * Some retained ground-portal rows deliberately target a non-standing stair
         * cell, so landing safety is stronger than merely testing that cell's raw
         * standing bits.
         */
        boolean hasWalkableExit(int x, int y) {
            if (!contains(x, y)) return false;
            for (int deltaY = -1; deltaY <= 1; deltaY++) {
                for (int deltaX = -1; deltaX <= 1; deltaX++) {
                    if ((deltaX != 0 || deltaY != 0)
                            && canStep(x, y, x + deltaX, y + deltaY)) {
                        return true;
                    }
                }
            }
            return false;
        }

        /** Exact retained static-map arrow-cell rule (dynamic doors are not in LTM1). */
        boolean arrowPassable(int x, int y) {
            return contains(x, y) && ((rawCell(x, y) & ~0x80) & 0x0C) != 0;
        }

        /**
         * Static-map port of retained {@code L1Character.hasLineOfSight}.
         * It visits every grid boundary crossed by the centre-to-centre ray;
         * this prevents non-45-degree projectiles from cutting a blocked corner.
         */
        boolean hasLineOfSight(int fromX, int fromY, int targetX, int targetY) {
            if (!contains(fromX, fromY) || !contains(targetX, targetY)) return false;
            int x = fromX;
            int y = fromY;
            if (x == targetX && y == targetY) return true;

            int dx = Math.abs(targetX - x);
            int dy = Math.abs(targetY - y);
            boolean exactDiagonalRay = dx == dy;
            int sx = Integer.compare(targetX, x);
            int sy = Integer.compare(targetY, y);
            int crossedX = 0;
            int crossedY = 0;
            int previousStepX = 0;
            int previousStepY = 0;

            long maximumSteps = (long) dx + dy;
            for (long step = 0;
                 step < maximumSteps && (x != targetX || y != targetY);
                 step++) {
                int nextX = x;
                int nextY = y;
                long nextXBoundary = crossedX < dx
                        ? (2L * crossedX + 1L) * dy : Long.MAX_VALUE;
                long nextYBoundary = crossedY < dy
                        ? (2L * crossedY + 1L) * dx : Long.MAX_VALUE;
                boolean moveX = nextXBoundary <= nextYBoundary;
                boolean moveY = nextYBoundary <= nextXBoundary;
                if (moveX) {
                    nextX += sx;
                    crossedX++;
                }
                if (moveY) {
                    nextY += sy;
                    crossedY++;
                }

                int stepX = nextX - x;
                int stepY = nextY - y;
                boolean previousCardinal = (previousStepX == 0) != (previousStepY == 0);
                boolean currentCardinal = (stepX == 0) != (stepY == 0);
                if (previousCardinal && currentCardinal
                        && (previousStepX != stepX || previousStepY != stepY)) {
                    int outsideX = x - previousStepX + stepX;
                    int outsideY = y - previousStepY + stepY;
                    if (!arrowPassable(outsideX, outsideY)) return false;
                }

                boolean diagonalPassable = false;
                if (moveX && moveY) {
                    int horizontalX = nextX;
                    int horizontalY = y;
                    int verticalX = x;
                    int verticalY = nextY;
                    boolean viaHorizontal = arrowPassable(horizontalX, horizontalY)
                            && projectileCardinalBoundaryOpen(
                                    x, y, nextX, y)
                            && projectileCardinalBoundaryOpen(
                                    nextX, y, nextX, nextY);
                    boolean viaVertical = arrowPassable(verticalX, verticalY)
                            && projectileCardinalBoundaryOpen(
                                    x, y, x, nextY)
                            && projectileCardinalBoundaryOpen(
                                    x, nextY, nextX, nextY);
                    diagonalPassable = exactDiagonalRay
                            ? viaHorizontal || viaVertical
                            : viaHorizontal && viaVertical;
                    if (!diagonalPassable) return false;
                }

                if (moveX && moveY) {
                    if (!diagonalPassable) return false;
                } else if (!projectileCardinalBoundaryOpen(
                        x, y, nextX, nextY)) {
                    return false;
                }
                if ((nextX != targetX || nextY != targetY)
                        && !arrowPassable(nextX, nextY)) {
                    return false;
                }
                x = nextX;
                y = nextY;
                previousStepX = stepX;
                previousStepY = stepY;
                if (x == targetX && y == targetY) return true;
            }
            return true;
        }

        boolean attackPosition(int fromX, int fromY, int targetX, int targetY,
                               int requestedRange) {
            int range = requestedRange <= 0 ? 1 : requestedRange;
            int dx = Math.abs(targetX - fromX);
            int dy = Math.abs(targetY - fromY);
            int distance = range >= 7 ? dx + dy : Math.max(dx, dy);
            return distance <= range
                    && hasLineOfSight(fromX, fromY, targetX, targetY);
        }

        private boolean projectileCardinalBoundaryOpen(
                int fromX, int fromY, int toX, int toY) {
            int direction = heading(fromX, fromY, toX, toY);
            return direction >= 0 && (direction & 1) == 0
                    && canStep(fromX, fromY, toX, toY);
        }
    }

    private ClassicWorldMapCatalog() {}

    static ClassicMap forMap(int mapId) {
        ClassicMap result = MAPS.get(mapId);
        if (result != null) return result;
        LitoMapRepository external = externalMaps;
        if (external != null && external.supports(mapId)) return external.load(mapId);
        throw new IllegalArgumentException("unsupported classic-world map: " + mapId);
    }

    static List<Integer> supportedMapIds() {
        return SUPPORTED_MAP_IDS;
    }

    static boolean supports(int mapId) {
        LitoMapRepository external = externalMaps;
        return MAPS.containsKey(mapId) || (external != null && external.supports(mapId));
    }

    static boolean embeddedSupports(int mapId) {
        return MAPS.containsKey(mapId);
    }

    static void installExternalMaps(LitoMapRepository maps) {
        externalMaps = maps;
    }

    static void removeExternalMaps(LitoMapRepository maps) {
        if (externalMaps == maps) externalMaps = null;
    }

    static ClassicMap externalMap(LitoMapRepository.Definition definition,
                                  String sourceSha256, byte[] cells) {
        return new ClassicMap(definition.mapId(), definition.startX(), definition.endX(),
                definition.startY(), definition.endY(), definition.width(), definition.height(),
                sourceSha256, cells, definition);
    }

    private static int heading(int fromX, int fromY, int toX, int toY) {
        int dx = toX - fromX;
        int dy = toY - fromY;
        for (int heading = 0; heading < HEADING_X.length; heading++) {
            if (dx == HEADING_X[heading] && dy == HEADING_Y[heading]) return heading;
        }
        return -1;
    }

    private static ClassicMap load(String resourceName) {
        InputStream resource = ClassicWorldMapCatalog.class.getResourceAsStream(resourceName);
        if (resource == null) {
            throw new ExceptionInInitializerError("missing classic map resource " + resourceName);
        }
        try (DataInputStream in = new DataInputStream(new GZIPInputStream(resource))) {
            if (in.readInt() != MAGIC) throw new IOException("invalid classic map magic");
            int mapId = in.readUnsignedShort();
            int startX = in.readInt();
            int endX = in.readInt();
            int startY = in.readInt();
            int endY = in.readInt();
            int width = in.readInt();
            int height = in.readInt();
            byte[] sourceHash = in.readNBytes(32);
            if (sourceHash.length != 32) throw new IOException("truncated source hash");
            String sourceSha256 = HexFormat.of().withUpperCase().formatHex(sourceHash);
            int expectedWidth = Math.addExact(Math.subtractExact(endX, startX), 1);
            int expectedHeight = Math.addExact(Math.subtractExact(endY, startY), 1);
            if (width != expectedWidth || height != expectedHeight) {
                throw new IOException("classic map dimensions disagree with bounds");
            }
            int cellCount = Math.multiplyExact(width, height);
            byte[] cells = in.readNBytes(cellCount);
            if (cells.length != cellCount || in.read() != -1) {
                throw new IOException("classic map cell payload is truncated or has trailing data");
            }
            if (mapId == WORLD_TREE_MAP_ID
                    && !MAP4_SOURCE_SHA256.equals(sourceSha256)) {
                throw new IOException("classic map4 source hash mismatch");
            }
            if (mapId == HELL_MAP_ID
                    && !MAP666_SOURCE_SHA256.equals(sourceSha256)) {
                throw new IOException("classic map666 source hash mismatch");
            }
            if (mapId == TALKING_ISLAND_MAP_ID
                    && !MAP0_SOURCE_SHA256.equals(sourceSha256)) {
                throw new IOException("classic map0 source hash mismatch");
            }
            if (mapId == ROYAL_TRIAL_CAVE_FIRST_MAP_ID
                    && !MAP1_SOURCE_SHA256.equals(sourceSha256)) {
                throw new IOException("classic map1 source hash mismatch");
            }
            if (mapId == ROYAL_TRIAL_CAVE_SECOND_MAP_ID
                    && !MAP2_SOURCE_SHA256.equals(sourceSha256)) {
                throw new IOException("classic map2 source hash mismatch");
            }
            if (mapId == UNDERSEA_TUNNEL_MAP_ID
                    && !MAP14_SOURCE_SHA256.equals(sourceSha256)) {
                throw new IOException("classic map14 source hash mismatch");
            }
            if (mapId == UNDERSEA_TUNNEL_SEVENTH_FLOOR_MAP_ID
                    && !MAP13_SOURCE_SHA256.equals(sourceSha256)) {
                throw new IOException("classic map13 source hash mismatch");
            }
            if (mapId == JIM_SECRET_ROOM_MAP_ID
                    && !MAP237_SOURCE_SHA256.equals(sourceSha256)) {
                throw new IOException("classic map237 source hash mismatch");
            }
            if (mapId == GUNTER_HOUSE_MAP_ID
                    && !MAP3_SOURCE_SHA256.equals(sourceSha256)) {
                throw new IOException("classic map3 source hash mismatch");
            }
            if (mapId == TALKING_ISLAND_TO_GLUDIO_SHIP_MAP_ID
                    && !MAP5_SOURCE_SHA256.equals(sourceSha256)) {
                throw new IOException("classic map5 source hash mismatch");
            }
            if (mapId == GLUDIO_TO_TALKING_ISLAND_SHIP_MAP_ID
                    && !MAP6_SOURCE_SHA256.equals(sourceSha256)) {
                throw new IOException("classic map6 source hash mismatch");
            }
            if (mapId == FORGOTTEN_ISLAND_MAP_ID
                    && !MAP70_SOURCE_SHA256.equals(sourceSha256)) {
                throw new IOException("classic map70 source hash mismatch");
            }
            if (mapId == MAINLAND_TO_FORGOTTEN_ISLAND_SHIP_MAP_ID
                    && !MAP83_SOURCE_SHA256.equals(sourceSha256)) {
                throw new IOException("classic map83 source hash mismatch");
            }
            if (mapId == FORGOTTEN_ISLAND_TO_MAINLAND_SHIP_MAP_ID
                    && !MAP84_SOURCE_SHA256.equals(sourceSha256)) {
                throw new IOException("classic map84 source hash mismatch");
            }
            if (mapId == HARTIN_LABORATORY_MAP_ID
                    && !MAP16_SOURCE_SHA256.equals(sourceSha256)) {
                throw new IOException("classic map16 source hash mismatch");
            }
            if (mapId == NERUPA_CAVE_MAP_ID
                    && !MAP17_SOURCE_SHA256.equals(sourceSha256)) {
                throw new IOException("classic map17 source hash mismatch");
            }
            if (mapId == SLEEPING_DRAGON_FIRST_FLOOR_MAP_ID
                    && !MAP19_SOURCE_SHA256.equals(sourceSha256)) {
                throw new IOException("classic map19 source hash mismatch");
            }
            if (mapId == SLEEPING_DRAGON_SECOND_FLOOR_MAP_ID
                    && !MAP20_SOURCE_SHA256.equals(sourceSha256)) {
                throw new IOException("classic map20 source hash mismatch");
            }
            if (mapId == ORC_ELVEN_DUNGEON_MAP_ID
                    && !MAP21_SOURCE_SHA256.equals(sourceSha256)) {
                throw new IOException("classic map21 source hash mismatch");
            }
            if (mapId == GERARD_TRIAL_DUNGEON_MAP_ID
                    && !MAP22_SOURCE_SHA256.equals(sourceSha256)) {
                throw new IOException("classic map22 source hash mismatch");
            }
            String knightCaveSource = KNIGHT_CAVE_MAP_SOURCE_SHA256.get(mapId);
            if (knightCaveSource != null && !knightCaveSource.equals(sourceSha256)) {
                throw new IOException("classic knight-cave map source hash mismatch " + mapId);
            }
            String antCaveSource = ANT_CAVE_MAP_SOURCE_SHA256.get(mapId);
            if (antCaveSource != null && !antCaveSource.equals(sourceSha256)) {
                throw new IOException("classic ant-cave map source hash mismatch " + mapId);
            }
            String gludioDungeonSource = GLUDIO_DUNGEON_MAP_SOURCE_SHA256.get(mapId);
            if (gludioDungeonSource != null
                    && !gludioDungeonSource.equals(sourceSha256)) {
                throw new IOException(
                        "classic Gludio-dungeon map source hash mismatch " + mapId);
            }
            String giranDungeonSource = GIRAN_DUNGEON_MAP_SOURCE_SHA256.get(mapId);
            if (giranDungeonSource != null
                    && !giranDungeonSource.equals(sourceSha256)) {
                throw new IOException(
                        "classic Giran-dungeon map source hash mismatch " + mapId);
            }
            if (mapId == GIRAN_CASTLE_MAP_ID
                    && !MAP52_SOURCE_SHA256.equals(sourceSha256)) {
                throw new IOException("classic map52 source hash mismatch");
            }
            String windawoodSource = WINDAWOOD_CASTLE_MAP_SOURCE_SHA256.get(mapId);
            if (windawoodSource != null && !windawoodSource.equals(sourceSha256)) {
                throw new IOException(
                        "classic Windawood-castle map source hash mismatch " + mapId);
            }
            String kentSource = KENT_COMPONENT_MAP_SOURCE_SHA256.get(mapId);
            if (kentSource != null && !kentSource.equals(sourceSha256)) {
                throw new IOException(
                        "classic Kent-component map source hash mismatch " + mapId);
            }
            String ivoryTowerSource = IVORY_TOWER_MAP_SOURCE_SHA256.get(mapId);
            if (ivoryTowerSource != null && !ivoryTowerSource.equals(sourceSha256)) {
                throw new IOException(
                        "classic Ivory-Tower map source hash mismatch " + mapId);
            }
            String adenToiSource = ADEN_TOI_MAP_SOURCE_SHA256.get(mapId);
            if (adenToiSource != null && !adenToiSource.equals(sourceSha256)) {
                throw new IOException(
                        "classic Aden/TOI map source hash mismatch " + mapId);
            }
            if (mapId == DWARF_CASTLE_CAVE_MAP_ID
                    && !MAP66_SOURCE_SHA256.equals(sourceSha256)) {
                throw new IOException("classic map66 source hash mismatch");
            }
            if (mapId == DRAGON_VALLEY_DUNGEON_FIRST_FLOOR_MAP_ID
                    && !MAP30_SOURCE_SHA256.equals(sourceSha256)) {
                throw new IOException("classic map30 source hash mismatch");
            }
            if (mapId == DRAGON_VALLEY_DUNGEON_SECOND_FLOOR_MAP_ID
                    && !MAP31_SOURCE_SHA256.equals(sourceSha256)) {
                throw new IOException("classic map31 source hash mismatch");
            }
            if (mapId == DRAGON_VALLEY_DUNGEON_THIRD_FLOOR_MAP_ID
                    && !MAP32_SOURCE_SHA256.equals(sourceSha256)) {
                throw new IOException("classic map32 source hash mismatch");
            }
            if (mapId == DRAGON_VALLEY_DUNGEON_FOURTH_FLOOR_MAP_ID
                    && !MAP33_SOURCE_SHA256.equals(sourceSha256)) {
                throw new IOException("classic map33 source hash mismatch");
            }
            if (mapId == DRAGON_VALLEY_DUNGEON_FIFTH_FLOOR_MAP_ID
                    && !MAP35_SOURCE_SHA256.equals(sourceSha256)) {
                throw new IOException("classic map35 source hash mismatch");
            }
            if (mapId == DRAGON_VALLEY_DUNGEON_SIXTH_FLOOR_MAP_ID
                    && !MAP36_SOURCE_SHA256.equals(sourceSha256)) {
                throw new IOException("classic map36 source hash mismatch");
            }
            if (mapId == ANTHARAS_LAIR_MAP_ID
                    && !MAP37_SOURCE_SHA256.equals(sourceSha256)) {
                throw new IOException("classic map37 source hash mismatch");
            }
            if (mapId == VALAKAS_HABITAT_MAP_ID
                    && !MAP67_SOURCE_SHA256.equals(sourceSha256)) {
                throw new IOException("classic map67 source hash mismatch");
            }
            if (mapId == DARK_ELF_DUNGEON_MAP_ID
                    && !MAP209_SOURCE_SHA256.equals(sourceSha256)) {
                throw new IOException("classic map209 source hash mismatch");
            }
            if (mapId == DARKMAR_DUNGEON_MAP_ID
                    && !MAP213_SOURCE_SHA256.equals(sourceSha256)) {
                throw new IOException("classic map213 source hash mismatch");
            }
            if (mapId == SEPIA_DUNGEON_MAP_ID
                    && !MAP302_SOURCE_SHA256.equals(sourceSha256)) {
                throw new IOException("classic map302 source hash mismatch");
            }
            if (mapId == PHANTASM_ISLAND_MAP_ID
                    && !MAP303_SOURCE_SHA256.equals(sourceSha256)) {
                throw new IOException("classic map303 source hash mismatch");
            }
            if (mapId == TAINTED_LAND_MAP_ID
                    && !MAP244_SOURCE_SHA256.equals(sourceSha256)) {
                throw new IOException("classic map244 source hash mismatch");
            }
            if (mapId == GIRAN_TRADE_ZONE_MAP_ID
                    && !MAP350_SOURCE_SHA256.equals(sourceSha256)) {
                throw new IOException("classic map350 source hash mismatch");
            }
            if (mapId == HIDDEN_DOCK_MAP_ID
                    && !MAP445_SOURCE_SHA256.equals(sourceSha256)) {
                throw new IOException("classic map445 source hash mismatch");
            }
            if (mapId == HIDDEN_DOCK_SHIP_MAP_ID
                    && !MAP446_SOURCE_SHA256.equals(sourceSha256)) {
                throw new IOException("classic map446 source hash mismatch");
            }
            if (mapId == PIRATE_ISLAND_SHIP_MAP_ID
                    && !MAP447_SOURCE_SHA256.equals(sourceSha256)) {
                throw new IOException("classic map447 source hash mismatch");
            }
            String heineSource = ClassicWorldHeineCatalog.MAP_SOURCE_SHA256.get(mapId);
            if (heineSource != null && !heineSource.equals(sourceSha256)) {
                throw new IOException("classic Heine/Eva map source hash mismatch " + mapId);
            }
            String silentSource = ClassicWorldSilentCavernCatalog.MAP_SOURCE_SHA256.get(mapId);
            if (silentSource != null && !silentSource.equals(sourceSha256)) {
                throw new IOException("classic Silent Cavern map source hash mismatch " + mapId);
            }
            String rastabadSource = ClassicWorldRastabadOumCatalog.MAP_SOURCE_SHA256.get(mapId);
            if (rastabadSource != null && !rastabadSource.equals(sourceSha256)) {
                throw new IOException("classic Rastabad/Oum map source hash mismatch " + mapId);
            }
            String diadradSource = ClassicWorldDiadradCatalog.MAP_SOURCE_SHA256.get(mapId);
            if (diadradSource != null && !diadradSource.equals(sourceSha256)) {
                throw new IOException("classic Diad service-core map source hash mismatch " + mapId);
            }
            String elementalGraveSource =
                    ClassicWorldElementalGraveCatalog.MAP_SOURCE_SHA256.get(mapId);
            if (elementalGraveSource != null
                    && !elementalGraveSource.equals(sourceSha256)) {
                throw new IOException(
                        "classic Elemental-Grave map source hash mismatch " + mapId);
            }
            String pirateDungeonSource =
                    ClassicWorldPirateDungeonCatalog.MAP_SOURCE_SHA256.get(mapId);
            if (pirateDungeonSource != null
                    && !pirateDungeonSource.equals(sourceSha256)) {
                throw new IOException(
                        "classic Pirate-dungeon map source hash mismatch " + mapId);
            }
            String pirateBackHalfSource =
                    ClassicWorldPirateBackHalfCatalog.MAP_SOURCE_SHA256.get(mapId);
            if (pirateBackHalfSource != null
                    && !pirateBackHalfSource.equals(sourceSha256)) {
                throw new IOException(
                        "classic Pirate-back-half map source hash mismatch " + mapId);
            }
            if (mapId == LASTABAD_GATE_MAP_ID
                    && !ClassicWorldLastabadGateCatalog.MAP_SOURCE_SHA256
                            .equals(sourceSha256)) {
                throw new IOException("classic Lastabad-gate map source hash mismatch");
            }
            String hotelSource = ClassicWorldHotelCatalog.MAP_SOURCE_SHA256.get(mapId);
            if (hotelSource != null && !hotelSource.equals(sourceSha256)) {
                throw new IOException("classic hotel map source hash mismatch " + mapId);
            }
            String desireCaveSource =
                    ClassicWorldDesireCaveCatalog.MAP_SOURCE_SHA256.get(mapId);
            if (desireCaveSource != null
                    && !desireCaveSource.equals(sourceSha256)) {
                throw new IOException(
                        "classic Desire-Cave map source hash mismatch " + mapId);
            }
            String homeTownSource =
                    ClassicWorldBehemothSilveriaCatalog.MAP_SOURCE_SHA256.get(mapId);
            if (homeTownSource != null && !homeTownSource.equals(sourceSha256)) {
                throw new IOException(
                        "classic home-town map source hash mismatch " + mapId);
            }
            return new ClassicMap(mapId, startX, endX, startY, endY,
                    width, height, sourceSha256, cells);
        } catch (IOException | ArithmeticException error) {
            ExceptionInInitializerError wrapped = new ExceptionInInitializerError(
                    "cannot load classic map resource " + resourceName + ": " + error.getMessage());
            wrapped.initCause(error);
            throw wrapped;
        }
    }
}
