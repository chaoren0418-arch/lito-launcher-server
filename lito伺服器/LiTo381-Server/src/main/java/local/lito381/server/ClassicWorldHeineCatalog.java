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
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.GZIPInputStream;

/**
 * Hash-locked selected-source contract for the Heine / Eva component.
 *
 * <p>The component deliberately retains only the selected {@code 380_db}
 * rows.  The alternate source has a malformed map63 drop row and is evidence
 * only; it is never used to fill or override this catalog.</p>
 */
final class ClassicWorldHeineCatalog {
    static final List<Integer> MAP_IDS = List.of(59, 60, 61, 62, 63, 64, 65, 252, 253, 254);
    static final int CASTLE_ID = 5;
    static final int CASTLE_EXTERIOR_MAP_ID = 4;
    static final int INTERIOR_MAP_ID = 64;
    static final String COMPONENT_SOURCE_SHA256 =
            "F7CFA540A499D5A42F6FE1C7B09A21A4DE295427F3650DDC8A7C25843916484C";
    static final String ROUTES_SOURCE_SHA256 =
            "0D7168AD7CDA912CC7AAC20579E81D2FC8A029844FBC366977EB78A2180EA6B8";
    static final String FLAGS_SOURCE_SHA256 =
            "05BCF32D835E31593D60CDB239C411805BF03904A1A4CCA56FC64D970EEF635E";
    static final String GETBACK_SOURCE_SHA256 =
            "C79FED7E07DC6748A14982B72EBA9CE8B3A5FC006EBFE3ABE1C0F9774F538D66";
    static final String NPC_TELEPORT_SOURCE_SHA256 =
            "94C9A66A46A5429123FAA1DE648BA63CE3182488E8E3772CBD6F6E1EF689AD06";
    static final String NPC_ACTIONS_SOURCE_SHA256 =
            "FC7E6CC42CA8E812C2496CCBFEC4230FFC12B0A075C7C3F1CB17B06AFFF0A39F";
    static final String TEXT_SOURCE_SHA256 =
            "9F0C37452886C33DC99A134A577B8FB01A88E687A6B54290D964DF372098F25A";
    static final String DOORS_SOURCE_SHA256 =
            "9526F2916C94F7BA6A7695D9445C96CDA4CF21D9802672E0EA489B56B69EBEC4";
    static final String KEEPERS_SOURCE_SHA256 =
            "B9C887B828A743FA6F68FB5A838A8D5B679842137A1B17406463E8AB6B218CD7";
    static final String SHOP_SOURCE_SHA256 =
            "569D289086EEDC2D1EDCA79539BA9ED63D2AD3AA3F3E86C1C3076720494456CA";
    static final String SHOP_RESOURCE_SHA256 =
            "B1CCC6B4734D5CB4220E858F2891CEA85FC09C1C58F690345E5F0723BB4576ED";
    static final String CRAFT_SOURCE_SHA256 =
            "D7C4DAD2FDDE7BF08117F09AEF37FC108A49D8F10BC18274D023CA3C5AFCD167";
    static final String CRAFT_RESOURCE_SHA256 =
            "3ABA0D92356D8B07E722D7840B92C2DA5D82A81DB75BD3BDA0BBB2861001332D";
    static final String STORY_SOURCE_SHA256 =
            "D05E0DBF3E91375DA0B9502D48D2B02FD4E792A8B98817596D57DAE4C7CDACEE";

    static final int TELEPORTER_SPAWN_ID = 108;
    static final int TELEPORTER_NPC_ID = 50067;
    static final String TELEPORT_INITIAL_HTML = "telecheine1";
    static final String TELEPORT_CONFIRM_HTML = "telecheine2";
    static final String TELEPORT_CONFIRM_ACTION = "teleportURL";
    static final int WAREHOUSE_SPAWN_ID = 288;
    static final int WAREHOUSE_NPC_ID = 60029;
    static final int GUARD_NPC_ID = 70857;
    static final int WEAPON_ENCHANTER_SPAWN_ID = 881;
    static final int WEAPON_ENCHANTER_NPC_ID = 70861;
    static final int ARMOR_ENCHANTER_SPAWN_ID = 885;
    static final int ARMOR_ENCHANTER_NPC_ID = 70865;
    static final int STEWARD_SPAWN_ID = 889;
    static final int STEWARD_NPC_ID = 70880;
    static final int EVA_CRAFTER_SPAWN_ID = 886;
    static final int EVA_CRAFTER_NPC_ID = 70872;
    static final int ZYBRIL_SPAWN_ID = 1158;
    static final int ZYBRIL_NPC_ID = 71257;
    static final int BUAKHEU_SPAWN_ID = 589;
    static final int BUAKHEU_NPC_ID = 70081;
    static final String BUAKHEU_HTML = "buakheu1";
    static final String BUAKHEU_BUY_ACTION = "buy";
    static final String BUAKHEU_SELL_ACTION = "sell";
    static final String CASTLE_GATE_ACTION = "castlegate";
    static final String OPEN_GATE_ACTION = "openigate";
    static final String CLOSE_GATE_ACTION = "closeigate";
    static final String ASK_WAR_TIME_ACTION = "askwartime";
    static final String DECLARE_WAR_ACTION = "declarewar";

    record MapFlags(int mapId, String locationName,
                    int startX, int endX, int startY, int endY,
                    boolean escapable, boolean usableItem, boolean usableSkill,
                    boolean teleportable, boolean takePets, boolean recallPets) {
        MapFlags {
            if (!MAP_IDS.contains(mapId) || locationName == null || locationName.isBlank()
                    || startX <= 0 || endX < startX || startY <= 0 || endY < startY) {
                throw new IllegalArgumentException("invalid Heine map flags");
            }
        }
    }

    record SourceContract(int rows, int population, String sourceSha256,
                          String resourceSha256) {
        SourceContract {
            if (rows < 0 || population < 0 || sourceSha256 == null
                    || sourceSha256.length() != 64 || resourceSha256 == null
                    || resourceSha256.length() != 64) {
                throw new IllegalArgumentException("invalid Heine source contract");
            }
        }
    }

    record Door(int doorId, int gfxId, int x, int y, int mapId, int direction,
                int edgeLow, int edgeHigh, int maximumHp, int keeperNpcId) {
        Door {
            if (doorId < 2040 || doorId > 2042 || gfxId <= 0
                    || mapId != CASTLE_EXTERIOR_MAP_ID || direction != 0
                    || edgeLow <= 0 || edgeHigh < edgeLow || maximumHp < 0
                    || !GATEKEEPER_NPC_IDS.contains(keeperNpcId)) {
                throw new IllegalArgumentException("invalid Heine Castle door");
            }
        }

        long objectId() {
            return 0x6C00_0000L | doorId;
        }

        boolean controls(int cellX, int cellY) {
            return cellY == y + 1 && cellX >= edgeLow - 1 && cellX <= edgeHigh + 1;
        }
    }

    record ShopOffer(int wireOrderId, int sourceOrderId, int itemId,
                     int unitPrice, int packCount,
                     InventoryCatalog.Definition definition) {
        ShopOffer {
            if (wireOrderId < 0 || sourceOrderId < 0 || itemId <= 0 || unitPrice <= 0
                    || packCount <= 0 || definition == null
                    || definition.catalogId() != itemId) {
                throw new IllegalArgumentException("invalid Heine shop offer");
            }
        }
    }

    private record ShopRow(int npcId, int itemId, int sourceOrderId, int sellingPrice,
                           int packCount, int purchasingPrice, String note,
                           int dailyBuyLimit) {}
    private record ShopCatalog(List<ShopRow> rows,
                               Map<Integer, ClassicWorldDropCatalog.ItemWire> items) {}

    static final Map<Integer, String> MAP_SOURCE_SHA256 = Map.ofEntries(
            Map.entry(59, "A15A5BE042E15280F34A903FFB0441B3BD787C3C0110645B268E7D13FBB9E1B6"),
            Map.entry(60, "3FC4F6EE7740AAFBEF06099D17F81F6F5127D783FF79E16D0AF80C05CD64D757"),
            Map.entry(61, "6A071D76BC61382C2760205B82D6FB5C2EFB25CFB2651DAD76DDFF93BEC9FFF3"),
            Map.entry(62, "F216DCA6A11EFE2556205BC3F549363C242D84971DB8B3F3A04A2917278DF6DA"),
            Map.entry(63, "FF96666C54800502D3EBC2B18BABADC34ACE9D9DA6BABA9F5E32FBFCC080A4C0"),
            Map.entry(64, "7CFE6936B8997B68B192E39218CAF92B7010BCBECA69FCCE65931E348B5590CA"),
            Map.entry(65, "68ED93B62BD476D8E5EFAFC9167254147E3EACFC808EB8BCBB81516E178CF2F7"),
            Map.entry(252, "0A209D91B4B77833654C6EFB519E9D49B0906F07E0742F203AD448A8FF0E5CB1"),
            Map.entry(253, "C100BFC7A61B9C05B93462A22516E56E2B5C7E13FBE1DD1B6403F97A938AA566"),
            Map.entry(254, "894FF6B62A36C588B223BDCC7C5776CE494BE8081387CC68A52122FC818010EE"));

    static final Map<Integer, SourceContract> NPC_CONTRACTS = Map.ofEntries(
            contract(59, 0, 0, "8263164069B03411156DCA1D6B564F777EED14697D36402D02A1086131AEE8BF", "5D5BF719E849C698FD1203690486F75642C0C9E9DD77BD4BBCB4D16C8C6FCC83"),
            contract(60, 0, 0, "8263164069B03411156DCA1D6B564F777EED14697D36402D02A1086131AEE8BF", "E741E0E9576C1151F0FE0DAA47CC5A0A5B9481E7F1D3502155E4E9E46E7BE2A7"),
            contract(61, 0, 0, "8263164069B03411156DCA1D6B564F777EED14697D36402D02A1086131AEE8BF", "6A5E6C28DA258A432925C13FA115DBA1166CED855C8BF2036C6E0C0864BF578E"),
            contract(62, 2, 0, "34FDB3EA6A784C831DF9FF7E0A477D9898BFDA682006A74E6033EEF788D226DE", "E950B4FA3D7DD6D0C1C26E77D9C71B1B9FE1A0F5767E7AE4C1407591F68C7CB9"),
            contract(63, 1, 0, "FED6E2C4159FCA4494AC419076413CABE59131E9B4091074AC86C8B0DCEAFD22", "27D14BF1AC17C20B85A72B3B9033005573322A2206EE8CB2958B5CF44CEE7090"),
            contract(64, 13, 0, "EEAD9B089C120411D52892EA6680EAFB8F5A848A7B6891CB2A184ABBCFE47B80", "F5D0587152300AEB32E202360B0DD0A1B403ACC658D149B90C134011E0D6AE52"),
            contract(65, 0, 0, "8263164069B03411156DCA1D6B564F777EED14697D36402D02A1086131AEE8BF", "0EA8DF4DB6C92E731B3DB3747366DAAB7D7AF8C13628D20D527DED99988C5AC2"),
            contract(252, 0, 0, "8263164069B03411156DCA1D6B564F777EED14697D36402D02A1086131AEE8BF", "4ECCEC25532892CCC72AAD518B841CF4FEE964CC42111F7CD82FCD621A91C563"),
            contract(253, 0, 0, "8263164069B03411156DCA1D6B564F777EED14697D36402D02A1086131AEE8BF", "07C8395964856ECD3C525A17032D3E1BE146F4E6A7D73DCD16C03C9EEFFC6AFD"),
            contract(254, 0, 0, "8263164069B03411156DCA1D6B564F777EED14697D36402D02A1086131AEE8BF", "4FAC9BB6A1B896776EF1E8CAE75BAA8956B9828F44DE5EE04859646A8DC52D89"));

    static final Map<Integer, SourceContract> MONSTER_CONTRACTS = Map.ofEntries(
            contract(59, 5, 65, "3CD777A72513505E34902F4F3AF405D8A2038576267B74C15C1F8447E0BD34EF", "ABDBC91A7FF7AB95621D0596880B8178698B9439262571F976D2E09BBD9BF7D9"),
            contract(60, 7, 86, "DC6330CB9BF5AF74516CCD77879E936D03FB388297CE32C430D0E47372F1E122", "B400A5AC08824F2197AF320C3C08D4E718C707593BDC49310287CD9D436A6274"),
            contract(61, 5, 47, "AF4F63A857E8E883877BF782BDAE4B021E1A36D87391766D8B4499C5FF085C8E", "27626814AF48DFBAB3CD2C976567AE434AF7115E3DAED5EF707F85E7472001DC"),
            contract(62, 0, 0, "219255B3830F63B32C0D35542B91DF47B5532113719CB79075F5CD233B6DE5E7", "9300D2B35A63C89A9B35074C39B3E2B0501F2C063ECD3EC332BB40AAAA169ADF"),
            contract(63, 18, 326, "60C132956ABE84C7E2D15671D16F5288B7848EBFDA029BC30900B71692C60221", "C57774956B9A2483A13DDADCACFF7F26CDF6E9F3E9F5C65271FFA62421935F56"),
            contract(64, 0, 0, "219255B3830F63B32C0D35542B91DF47B5532113719CB79075F5CD233B6DE5E7", "17553631C255E35C6C913AE1093471B9DEDC00ADF29C8159205B40ABF61EFD63"),
            contract(65, 0, 0, "219255B3830F63B32C0D35542B91DF47B5532113719CB79075F5CD233B6DE5E7", "C5C14BB420F72808877B631465D41CE82494B200E4E2B60932B033F4C03BA265"),
            contract(252, 9, 45, "BC433966B2D2FBA6D74C5097FB996CCA8CAF6A0CFCB1139E818DC981BB53B876", "F5B71D14614BBA516F767D4DED4B4853B0B2604DE5B5B2409C4413F796AD790E"),
            contract(253, 9, 90, "24244E054649D361C40400D59643D357FEF5047D22904AEB239FF2812C80D2AB", "844F6D52E7F8BE5F9C8B0A39107C6C035B337DE4310019180696323431859EF7"),
            contract(254, 8, 120, "62C22FF740C7BCAAE0A466C7A54827839059E66012273223202A302ABF9FA373", "4F0B2ECB4EC8A92CAB7B84CF5E0FCA299F209D5DC89B4897CF8FFD484E683EE0"));

    static final Map<Integer, SourceContract> DROP_CONTRACTS = Map.ofEntries(
            contract(59, 40, 26, "6447BBF45F784FDBB1B8EB9435FC06C34EAB864E70F4F37A1F74A9EC53D38C40", "1C18E79E0B73A84F241F3406AAE2ACFF6220D3C3616A123AB5B5F6268B097B41"),
            contract(60, 62, 38, "315178034850EF9F137E39BD0365E8C3D83F3484FAF777E8995094FE076D596F", "89937CDE48694532FCF4455473F4A2D7B331E4A14F33F48DC57E57834F524A0E"),
            contract(61, 43, 22, "55A559ED5712AD6A1D84CD1A18FC74B83AE6D314768D003CC18828AD21345909", "17DE973EE99C3D394055B5024591F761BE12F62145A4C55A463F17AFED09D890"),
            contract(62, 0, 0, "3FE93EDCC0A0A171C6BB8ADF328460441832AF757DA26B9DAB1E8C8A2963C828", "608EBDDFB801900418AE55BAB1044170D8696EF1CFB90B1C11D9A71631B45B1E"),
            contract(63, 78, 37, "A4E9FBF0046E0ADBD56C419C3E48AB378468470841474FF6B2F27B2C1604FF90", "DE358B68808431C49983F5DAB2A298FE336601A704961336ECD22BC09E05149A"),
            contract(64, 0, 0, "3FE93EDCC0A0A171C6BB8ADF328460441832AF757DA26B9DAB1E8C8A2963C828", "CFDBED7E671EC2371B4B0097A1A58E0AB8F04B919BAB542A67C5D43EBB835108"),
            contract(65, 0, 0, "3FE93EDCC0A0A171C6BB8ADF328460441832AF757DA26B9DAB1E8C8A2963C828", "C231C40FB3DC46D17F766753C6338E69F4A1F1306BFE97B99F33347A915FD67C"),
            contract(252, 4, 4, "E69ACD6A138A7D6BEEC61EB2A0938A98F2332B79C7A04A8E90FE5DE2005E5503", "217530608628F547D829105F07AA85D96B0553B2004216E67BC5D9269987A09B"),
            contract(253, 36, 23, "63A9991F2F68FAB0BB0F66EA68B437D8234AB44AFA725BAE58505DC6CD0DEE33", "2D6CBA9A8AF5A17445067A66DA1F573D60A6527CF879E3683E44F598C6D815B7"),
            contract(254, 18, 15, "9FDC8097094844851B61995C650B8BAD183178874AB30B9486298AC25E840DDE", "ACEC46A1872679A17C838B0693716CB8432BDCC9A44BFC8828C2B5D964D1A16B"));

    private static final Map<Integer, MapFlags> FLAGS = Map.ofEntries(
            Map.entry(59, flags(59, "海音地監 1樓", 32640, 32767, 32768, 32895, true, true, true)),
            Map.entry(60, flags(60, "海音地監 2樓", 32640, 32767, 32768, 32895, true, true, true)),
            Map.entry(61, flags(61, "海音地監 3樓", 32640, 32767, 32768, 32895, true, true, true)),
            Map.entry(62, flags(62, "伊娃神殿", 32640, 32767, 32768, 32895, true, true, true)),
            Map.entry(63, flags(63, "海音地監 4樓", 32576, 32895, 32640, 32959, true, false, false)),
            Map.entry(64, flags(64, "海音內城", 32512, 32639, 32768, 32895, true, true, true)),
            Map.entry(65, flags(65, "法利昂棲息地", 32704, 32831, 32768, 32895, false, false, false)),
            Map.entry(252, flags(252, "封閉的海音地監", 32576, 32767, 32832, 32895, false, true, true)),
            Map.entry(253, flags(253, "封閉的伊娃王國", 32704, 32831, 32832, 33023, false, false, false)),
            Map.entry(254, flags(254, "封閉的海音地監", 32576, 32767, 32768, 32959, false, true, true)));

    private static final Set<Integer> GATEKEEPER_NPC_IDS = Set.of(70862, 70863, 70992);
    private static final List<Door> DOORS = List.of(
            new Door(2040, 1336, 33523, 33385, 4, 0, 33522, 33523, 0, 70863),
            new Door(2041, 1745, 33524, 33470, 4, 0, 33522, 33524, 10_000, 70992),
            new Door(2042, 1336, 33525, 33344, 4, 0, 33524, 33525, 3_000, 70862));
    private static final ShopCatalog BUAKHEU_SHOP = loadShop();

    private ClassicWorldHeineCatalog() {}

    static MapFlags flags(int mapId) {
        MapFlags result = FLAGS.get(mapId);
        if (result == null) throw new IllegalArgumentException("not a Heine map: " + mapId);
        return result;
    }

    static List<Door> doors() { return DOORS; }

    static boolean isTeleporter(ClassicWorldNpcCatalog.StaticNpc npc) {
        return matches(npc, INTERIOR_MAP_ID, TELEPORTER_SPAWN_ID, TELEPORTER_NPC_ID,
                32579, 32811, "L1Teleporter", TELEPORT_INITIAL_HTML);
    }

    static boolean isWarehouse(ClassicWorldNpcCatalog.StaticNpc npc) {
        return matches(npc, INTERIOR_MAP_ID, WAREHOUSE_SPAWN_ID, WAREHOUSE_NPC_ID,
                32558, 32847, "L1Dwarf", "haidrim");
    }

    static boolean isGuard(ClassicWorldNpcCatalog.StaticNpc npc) {
        return npc != null && npc.mapId() == INTERIOR_MAP_ID && npc.npcId() == GUARD_NPC_ID
                && npc.implementation().equals("L1Guard") && npc.normalAction().equals("heguard6");
    }

    static boolean isWeaponEnchanter(ClassicWorldNpcCatalog.StaticNpc npc) {
        return matches(npc, INTERIOR_MAP_ID, WEAPON_ENCHANTER_SPAWN_ID,
                WEAPON_ENCHANTER_NPC_ID, 32557, 32846, "L1Merchant", "encwc");
    }

    static boolean isArmorEnchanter(ClassicWorldNpcCatalog.StaticNpc npc) {
        return matches(npc, INTERIOR_MAP_ID, ARMOR_ENCHANTER_SPAWN_ID,
                ARMOR_ENCHANTER_NPC_ID, 32557, 32845, "L1Merchant", "encac");
    }

    static boolean isSteward(ClassicWorldNpcCatalog.StaticNpc npc) {
        return matches(npc, INTERIOR_MAP_ID, STEWARD_SPAWN_ID, STEWARD_NPC_ID,
                32579, 32813, "L1Merchant", "fisher1");
    }

    static boolean isCrafter(ClassicWorldNpcCatalog.StaticNpc npc) {
        return matches(npc, 62, EVA_CRAFTER_SPAWN_ID, EVA_CRAFTER_NPC_ID,
                32739, 32815, "L1Merchant", "eveurol1");
    }

    static boolean isStoryNpc(ClassicWorldNpcCatalog.StaticNpc npc) {
        return matches(npc, 62, ZYBRIL_SPAWN_ID, ZYBRIL_NPC_ID,
                32740, 32800, "L1Merchant", "zybril16");
    }

    static boolean isBuakheu(ClassicWorldNpcCatalog.StaticNpc npc) {
        return matches(npc, 63, BUAKHEU_SPAWN_ID, BUAKHEU_NPC_ID,
                32799, 32896, "L1Merchant", BUAKHEU_HTML);
    }

    static boolean isGatekeeper(ClassicWorldNpcCatalog.StaticNpc npc) {
        return npc != null && npc.mapId() == CASTLE_EXTERIOR_MAP_ID
                && GATEKEEPER_NPC_IDS.contains(npc.npcId())
                && npc.normalAction().equals("gatekeeperop");
    }

    static boolean isInnerGatekeeper(ClassicWorldNpcCatalog.StaticNpc npc) {
        return isGatekeeper(npc) && npc.npcId() == 70863;
    }

    static List<ShopOffer> buakheuOffers() {
        List<ShopOffer> offers = new ArrayList<>();
        int wireOrder = 0;
        for (ShopRow row : BUAKHEU_SHOP.rows()) {
            ClassicWorldDropCatalog.ItemWire item = BUAKHEU_SHOP.items().get(row.itemId());
            offers.add(new ShopOffer(wireOrder++, row.sourceOrderId(), row.itemId(),
                    row.sellingPrice(), row.packCount() == 0 ? 1 : row.packCount(),
                    item.inventoryDefinition()));
        }
        return List.copyOf(offers);
    }

    static boolean isTextOnlyStewardAction(String action) {
        return action != null && Set.of("inex", "tax", "withdrawal", "cdeposit",
                "security", "employ", "arrange").contains(action.toLowerCase());
    }

    private static Map.Entry<Integer, SourceContract> contract(
            int mapId, int rows, int population, String source, String resource) {
        return Map.entry(mapId, new SourceContract(rows, population, source, resource));
    }

    private static MapFlags flags(int mapId, String name, int startX, int endX,
                                  int startY, int endY, boolean teleportable,
                                  boolean takePets, boolean recallPets) {
        return new MapFlags(mapId, name, startX, endX, startY, endY,
                true, true, true, teleportable, takePets, recallPets);
    }

    private static boolean matches(ClassicWorldNpcCatalog.StaticNpc npc,
                                   int mapId, int spawnId, int npcId, int x, int y,
                                   String implementation, String normalAction) {
        return npc != null && npc.mapId() == mapId && npc.spawnId() == spawnId
                && npc.npcId() == npcId && npc.objectId() == ClassicWorldNpcCatalog.objectId(spawnId)
                && npc.x() == x && npc.y() == y && npc.implementation().equals(implementation)
                && npc.normalAction().equals(normalAction) && npc.chaoticAction().equals(normalAction);
    }

    private static ShopCatalog loadShop() {
        String resourceName = "/world/map63-buakheu-shop.lws.gz";
        InputStream resource = ClassicWorldHeineCatalog.class.getResourceAsStream(resourceName);
        if (resource == null) throw new ExceptionInInitializerError("missing Heine shop resource");
        try (DataInputStream in = new DataInputStream(new GZIPInputStream(resource))) {
            if (in.readInt() != 0x4C575331 || in.readUnsignedShort() != 63) {
                throw new IOException("invalid Heine shop header");
            }
            int rows = in.readInt();
            int items = in.readInt();
            byte[] sourceHashBytes = in.readNBytes(32);
            if (sourceHashBytes.length != 32) throw new IOException("truncated Heine shop hash");
            String sourceHash = HexFormat.of().withUpperCase().formatHex(sourceHashBytes);
            byte[] table = in.readAllBytes();
            if (rows != 15 || items != 15 || !SHOP_SOURCE_SHA256.equals(sourceHash)
                    || !sourceHash.equals(sha256(table)) || !SHOP_RESOURCE_SHA256.equals(
                            sha256Resource(resourceName))) {
                throw new IOException("Heine shop source contract changed");
            }
            return parseShop(table, rows, items);
        } catch (IOException | IllegalArgumentException error) {
            throw new ExceptionInInitializerError(error);
        }
    }

    private static ShopCatalog parseShop(byte[] table, int expectedRows, int expectedItems)
            throws IOException {
        String shopHeader = String.join("\t", "npc_id", "item_id", "order_id",
                "selling_price", "pack_count", "purchasing_price", "note", "daily_buy_limit");
        String itemHeader = String.join("\t", "item_id", "kind", "name_id", "use_token",
                "inventory_gfx", "ground_gfx", "blessing", "stackable", "tradable", "unit_weight",
                "item_description_id", "weapon_type", "damage_small", "damage_large", "material",
                "safe_enchant", "class_mask");
        List<ShopRow> rows = new ArrayList<>();
        Map<Integer, ClassicWorldDropCatalog.ItemWire> items = new LinkedHashMap<>();
        Set<Integer> referenced = new LinkedHashSet<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                new ByteArrayInputStream(table), StandardCharsets.UTF_8))) {
            if (!"[shops]".equals(reader.readLine()) || !shopHeader.equals(reader.readLine())) {
                throw new IOException("Heine shop TSV header changed");
            }
            int lastItemId = 0;
            for (String line; (line = reader.readLine()) != null;) {
                if (line.equals("[items]")) break;
                String[] field = line.split("\t", -1);
                if (field.length != 8) throw new IOException("Heine shop field count changed");
                ShopRow row = new ShopRow(integer(field[0]), integer(field[1]), integer(field[2]),
                        integer(field[3]), integer(field[4]), integer(field[5]), field[6], integer(field[7]));
                if (row.npcId() != BUAKHEU_NPC_ID || row.itemId() <= lastItemId
                        || row.sourceOrderId() < 0 || row.sellingPrice() <= 0
                        || row.packCount() < 0 || row.purchasingPrice() != -1
                        || row.dailyBuyLimit() != 0) {
                    throw new IOException("invalid or unordered Heine shop row");
                }
                lastItemId = row.itemId();
                rows.add(row);
                referenced.add(row.itemId());
            }
            if (rows.size() != expectedRows || !itemHeader.equals(reader.readLine())) {
                throw new IOException("Heine shop/item boundary changed");
            }
            int lastWireItem = 0;
            for (String line; (line = reader.readLine()) != null;) {
                String[] field = line.split("\t", -1);
                if (field.length != 17) throw new IOException("Heine shop item field count changed");
                int itemId = integer(field[0]);
                if (itemId <= lastWireItem) throw new IOException("Heine shop items unordered");
                lastWireItem = itemId;
                items.put(itemId, new ClassicWorldDropCatalog.ItemWire(itemId, field[1], field[2], field[3],
                        integer(field[4]), integer(field[5]), integer(field[6]), integer(field[7]) != 0,
                        integer(field[8]) != 0, integer(field[9]), integer(field[10]), field[11],
                        integer(field[12]), integer(field[13]), field[14], integer(field[15]), integer(field[16])));
            }
        }
        if (items.size() != expectedItems || !items.keySet().equals(referenced)) {
            throw new IOException("Heine shop rows/items disagree");
        }
        return new ShopCatalog(List.copyOf(rows), Map.copyOf(items));
    }

    private static String sha256Resource(String name) throws IOException {
        InputStream stream = ClassicWorldHeineCatalog.class.getResourceAsStream(name);
        if (stream == null) throw new IOException("missing Heine resource " + name);
        try (stream) { return sha256(stream.readAllBytes()); }
    }

    private static int integer(String value) { return Integer.parseInt(value); }

    private static String sha256(byte[] bytes) throws IOException {
        try {
            return HexFormat.of().withUpperCase().formatHex(
                    MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (NoSuchAlgorithmException impossible) {
            throw new IOException("SHA-256 unavailable", impossible);
        }
    }
}
