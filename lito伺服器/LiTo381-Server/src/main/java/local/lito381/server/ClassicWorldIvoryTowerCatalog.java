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

/** Exact selected-source contracts for native Ivory Tower maps 75-82. */
final class ClassicWorldIvoryTowerCatalog {
    static final List<Integer> MAP_IDS = List.of(75, 76, 77, 78, 79, 80, 81, 82);
    static final String COMPONENT_SOURCE_SHA256 =
            "6C186C039828842BC62AADFF2BB112C98AB23437051C0FD0F3FE5E36DC27EA26";
    static final String ROUTES_SOURCE_SHA256 =
            "D3109E8B84442141BDEC6C4553FD7A8A7625427A12681A45C1BDB8C0FCC54988";
    static final String FLAGS_SOURCE_SHA256 =
            "502DF44F578B55731832D15B4EDC561296097602B9F29A66F2BC6DE2DAE7F121";
    static final String GETBACK_SOURCE_SHA256 =
            "A010EAC8AAFD5E41C822712431B47C3E4315FDCEE660FC2C2A27C86FE872B7FA";
    static final String NPC_ACTIONS_SOURCE_SHA256 =
            "603A43DC136294955D38F2E300CFD3B625ED76779B78881691747B2033FCFEF7";
    static final String SHOP_SOURCE_SHA256 =
            "17FAD142748D6758A81979657AAD1B625E14D11D8D54A14E9FEC6E50AF9E31EA";
    static final String SHOP_RESOURCE_SHA256 =
            "E135FCE18FE78666A76FB4C7A14DD63AD7BACEDFDA36B030307590E28D0852D9";
    static final String STORY_SLICE_SOURCE_SHA256 =
            "B1334996870ADAFB766928A302DB18A9E3F70612864C329FAD49FFE84938712B";
    static final String STORY_FULL_SOURCE_SHA256 =
            "176ECB72747E0761F08F5C089EED536A1DFDF0266FAE56F146DF7793066C903B";
    static final String ELEMENTAL_SKILLS_SOURCE_SHA256 =
            "FA800E1E0C72B498AB077956FEEF69442352EEABC5E5745CB6721E7C4127D898";
    static final String CRAFTING_EMPTY_SOURCE_SHA256 =
            "9AB356F3D6AF0176325AD83BB99E9941CDBDBB1B8A78DAE975721CD0123E71EB";
    static final int ROUTE_COUNT = 42;
    static final int STATIC_NPC_COUNT = 7;
    static final int MONSTER_POPULATION = 250;
    static final int DROP_ROW_COUNT = 262;
    static final int SHOP_ROW_COUNT = 38;
    static final int SHOP_ITEM_COUNT = 38;
    static final int STORY_ROW_COUNT = 73;

    static final int RINDA_NPC_ID = 70060;
    static final int VARYETH_NPC_ID = 70062;
    static final int KREISTER_NPC_ID = 70066;
    static final int PAGORU_NPC_ID = 70067;
    static final int ELLYONNE_NPC_ID = 70760;
    static final int DICARDING_NPC_ID = 70739;
    static final int PIETA_NPC_ID = 71200;
    static final Set<Integer> STORY_NPC_IDS = Set.of(DICARDING_NPC_ID, PIETA_NPC_ID);
    static final Set<Integer> ELEMENTAL_SKILL_IDS = Set.of(
            157, 159, 160, 163, 164, 165, 167,
            170, 171, 172, 173, 174, 175);

    record MapFlags(int mapId, String locationName,
                    int startX, int endX, int startY, int endY,
                    boolean escapable, boolean teleportable,
                    boolean usableItem, boolean usableSkill,
                    boolean takePets, boolean recallPets) {
        MapFlags {
            if (!MAP_IDS.contains(mapId) || locationName == null || locationName.isBlank()
                    || startX <= 0 || endX < startX || startY <= 0 || endY < startY) {
                throw new IllegalArgumentException("invalid Ivory Tower map flags");
            }
        }
    }

    record SourceContract(int rows, int population, String sourceSha256,
                          String resourceSha256) {
        SourceContract {
            if (rows < 0 || population < 0 || sourceSha256 == null
                    || sourceSha256.length() != 64 || resourceSha256 == null
                    || resourceSha256.length() != 64) {
                throw new IllegalArgumentException("invalid Ivory Tower source contract");
            }
        }
    }

    record ShopOffer(int wireOrderId, int sourceOrderId, int itemId,
                     int unitPrice, int packCount, int purchasingPrice,
                     InventoryCatalog.Definition definition) {
        ShopOffer {
            if (wireOrderId < 0 || sourceOrderId < 0 || itemId <= 0
                    || unitPrice <= 0 || packCount <= 0 || purchasingPrice != -1
                    || definition == null || definition.catalogId() != itemId) {
                throw new IllegalArgumentException("invalid Ivory Tower shop offer");
            }
        }
    }

    private record ShopRow(int npcId, int itemId, int sourceOrderId,
                           int sellingPrice, int packCount,
                           int purchasingPrice, String note, int dailyBuyLimit) {}

    private record ShopCatalog(Map<Integer, List<ShopRow>> rowsByNpc,
                               Map<Integer, ClassicWorldDropCatalog.ItemWire> items) {}

    private static final Map<Integer, MapFlags> FLAGS = Map.of(
            75, flags(75, "象牙塔 1樓", 32704, 32831, 32768, 32959, true),
            76, flags(76, "象牙塔 2樓", 32704, 32831, 32768, 32895, true),
            77, flags(77, "象牙塔 3樓", 32704, 32831, 32768, 32895, true),
            78, flags(78, "象牙塔 4樓【無生物研究室】",
                    32832, 32959, 32704, 32831, true),
            79, flags(79, "象牙塔 5樓【黑魔法研究室】",
                    32704, 32831, 32768, 32895, true),
            80, flags(80, "象牙塔 6樓【魔物封印室】",
                    32704, 32831, 32768, 32895, false),
            81, flags(81, "象牙塔 7樓【魔物封印室】",
                    32704, 32831, 32768, 32895, false),
            82, flags(82, "象牙塔 8樓【惡魔封印室】",
                    32640, 32767, 32768, 32895, false));

    static final Map<Integer, String> MAP_SOURCE_SHA256 = Map.of(
            75, "8B3B5DCF53FF6FD78610A54F3092517C2593B65CD61E1E79A35D8D3BBECFAE58",
            76, "7899B6ABD306136BE3ABBD6F5FC75C0448C3CB4E2B2BC22E9E11FAC9C670BAF0",
            77, "34D2B2A2470E3494509B5146EB2533B5D82C182CAC50B5AE6DDD2C9C78D01657",
            78, "110DDF572F65DD7EDB89197E400100448F9E4A3B7EC0A6C533B21691DFA3F2B3",
            79, "F00FB563A4B642DB3FFB2D88B59CA0E5386BF4AC2FE0DC9AFE02151C92E0A488",
            80, "F96F8681950A36525F4999E62D6E76FA0911914B84417614D3BAEF4205C36B05",
            81, "284B6F40CA4C5DDA291DDE6966E93D67B6E56434B9876C1D962F7EDF29607097",
            82, "75C5D05ED9923D053C185D8AC28E6FBE08C3A0B53273D06195418E4DB8EB59B0");
    static final Map<Integer, String> MAP_RESOURCE_SHA256 = Map.of(
            75, "A04CFB630E5BF0A3E488EDBC08FAEB8E5DEC6BD9B8887C3700C259EE4884FDF9",
            76, "895C2704569657E55FF059305948DDE3AC41F66B44CFC6F039D54D0BF698A1B4",
            77, "B829A6B81E4155A0EEDFA8FC65D48CE6341863FD23B6A79333BA595CD5C4D4D8",
            78, "833C0777AE71C13D69670D8E4C052B7B7BBAA414F538E4ECFC7B8EB16815A1AD",
            79, "7336A4DAD1AFDE917F29D3A0C6F85E848FD8BBC28BFC6387FB4FA276C93DE76F",
            80, "AD8C7ADB6B3A8BA27A2EA415D53F497C3CB3369236C0DA3FCD48B2BA0C30F01B",
            81, "4A5179A87F74669E2725C28A72872E04DBB08B1AF45B4F92E21A389C4B772C8B",
            82, "A4568C027459995BF958FB9E47D4EDA1853BF4E4A4BE818F3BABFA4337E3E23F");
    static final Map<Integer, String> CLIENT_MAP_SHA256 = Map.of(
            75, "5A2134AD530290E0CF9EA36BD27437DAE1EE913C5E838F8384B7A94DEE5E0FCC",
            76, "57DF652DD3F2FE3B5FA3F324D8198EA3BA6B2A08853D55D3C545CC60DCF8C97B",
            77, "D6DB90EEC5BDE4AF6CBFE3E97B0939926E028CD01CAE479BA4D3109D7D5F8AB2",
            78, "30DD2A5AA9ED7E12E797FCD882A8712741028F46BECE75E663BBC3D9C9D96FBC",
            79, "AA20EB4365D70E6AF4AD557B4DD1AB60885F198EAED9E87528A6EE2718671886",
            80, "268B5A20A981D9680B8A5307E1E80E2CFB283B535B1E56CBCC69D8EB9CBE332F",
            81, "50C58A864EFC1C7ACCDB3E2EAF8392A76A6332FBFFFDB2207ADEEB358BF8F3BE",
            82, "986BBA97A905D101DBF2C25235F3BB736B163BD4B0F4F63ED122E1967934CBD7");

    static final Map<Integer, SourceContract> NPC_CONTRACTS = Map.of(
            75, contract(2, 0, "0132D664803F401BDAB6939B7D6B9A994D4E673E9A3DE90667DB474D14167A69", "AD92CA66771A82EE00C8C91CF799B444B00553C85966592E4A8F21A3503BA15E"),
            76, contract(3, 0, "1DD6FA217D2BF02EA80021417864AFC7A56B53EF00290B55D7564697188E986F", "B55CF5DAC0B4381B427100A683FDFABF71B361C2F77859545A9D422742BC7693"),
            77, contract(2, 0, "5C2434CDB30409FE75E8A4F107F8DD9B8988D3123476C5A1593A7F2F82795641", "DC966050D63C717B28D5A2BB2BC5B87D5C23D6F445AE8F710B0C72116C55DE3F"),
            78, contract(0, 0, "8263164069B03411156DCA1D6B564F777EED14697D36402D02A1086131AEE8BF", "D95D544784536FE1FF824ADF413A59C6290B8815EBF3292183D5C04AC348FEA3"),
            79, contract(0, 0, "8263164069B03411156DCA1D6B564F777EED14697D36402D02A1086131AEE8BF", "F62BBCF654E1E0508C00BD6C382115DC35A8151E9D7FC968895EA0657B867507"),
            80, contract(0, 0, "8263164069B03411156DCA1D6B564F777EED14697D36402D02A1086131AEE8BF", "9330CC90FD403F8B4C8CF1F93A70964D58381CE17496C25A9E9BB659C50A3D41"),
            81, contract(0, 0, "8263164069B03411156DCA1D6B564F777EED14697D36402D02A1086131AEE8BF", "9E68F4E0D7EE533BB73CF18ECDB672697E64C1BA8232108747DB98620677B75B"),
            82, contract(0, 0, "8263164069B03411156DCA1D6B564F777EED14697D36402D02A1086131AEE8BF", "BB7E6D3E91C7201FFABB2A826252912F9B0AF104C46A6AE7BEDA3892AF6A80BF"));
    static final Map<Integer, SourceContract> MONSTER_CONTRACTS = Map.of(
            75, contract(1, 5, "FA505F24D5FF5026B0D86368683ACDB4BE9B16A36BD6868D1A65872CFED80B0A", "6C61A945AF416633C870BBD0AC4B525DF0AB190DB2140FE7018B7DFA8544B4FD"),
            76, contract(1, 5, "FBA86D94758A3676D5625B08E49AB5EAA244C2056F4B42D8DA699D7DC36F3B76", "4859F43A424F5B04123643124F332FA2E620478C864E73803FBB5F896919B591"),
            77, contract(1, 5, "F1AF2C41FEC39281D06053285E8677956B697CF57C6AD61F1A93186C661753B4", "AD23911D1E6AE0A28CF333A34D43A529D8BAD974F8883CA0466069C6A8C3B333"),
            78, contract(4, 40, "2AB9492D0BCB1E7B01A76F6E59948EA979E7CB68A4AEDEE75A3EC9B317BEF021", "8E9DF62ED483A7E0A2220BB50F1B2226A9BFABC5655C8EF48B53CAB893A7ADB7"),
            79, contract(4, 45, "8F2A248C100A384D922CA2FB1269D8296A9BD2FE5DB039D414DBE6E0E8C7A818", "C7B8553449CD57132E9EC5060B0AC2254671FDC85CF0906748FD622CDE2E2569"),
            80, contract(3, 45, "C53898010C83C97C579BE509B14E386603B6557DA67991E069CCC94714CC823A", "EA7DBCE847B245384AA6804691C03D171DF4D7BD0D303C3C88280A1AD71A26A8"),
            81, contract(3, 45, "D54CB2B15F742243CB81F1D7172805B4F0980538B3D047A8847977C569756D35", "B4A1888AABEB7F47093C816E8B24FAB8E04454A95A1B711BC15FD5D6E5BFFD9F"),
            82, contract(4, 60, "8F9B365341F5BF710D00F0B2A1E2D479B8BB1BF05988411E0E4F24214540BECD", "3728779D66DF994CE85C804D3E8AC45281AC65278A6DC018681B0DBA96A8B193"));
    static final Map<Integer, SourceContract> DROP_CONTRACTS = Map.of(
            75, contract(8, 8, "063940D8CB8CE420306F099BCBE469ADF0CB64AB8049C4CFD072E494F61AA22E", "4AB9C8E5189EFB791C6C8E42B18BE9356BC5D1E8634779B31BA79E4CECCF4B1F"),
            76, contract(8, 8, "063940D8CB8CE420306F099BCBE469ADF0CB64AB8049C4CFD072E494F61AA22E", "26F5A55F2A91CE138F5512B7CC4801A533E078394B4E33763F02588F3161CCCB"),
            77, contract(8, 8, "063940D8CB8CE420306F099BCBE469ADF0CB64AB8049C4CFD072E494F61AA22E", "74B24C49A5DACC29C8EC5DA9A40817FCA2D741E353C13EC1C2DEDFDE5FC9DCC5"),
            78, contract(57, 42, "E886F2F9B6D6FDB15271320C27DCC615B1544BFF4CC2F35E57865F0BD926B20D", "6FE719394EB1C721BDBA3448B357C1901BAB64972AB376D85A472B82932649FC"),
            79, contract(57, 42, "E886F2F9B6D6FDB15271320C27DCC615B1544BFF4CC2F35E57865F0BD926B20D", "F18114798EC350CF5C544CE3CDEA5EE288D44B7666914BC35AC95A74A20EC335"),
            80, contract(34, 30, "28B48B86192C06F38C22F24C1A260BCEF49F2DC7D1C3F1C195DE79B865CBF2D3", "6397A2C175636351D8284CB4D340CACFCB5D5B7F717EED355BADBBF459E5CFE4"),
            81, contract(34, 30, "28B48B86192C06F38C22F24C1A260BCEF49F2DC7D1C3F1C195DE79B865CBF2D3", "6AC159ACA317FC8E831C64BAE891D67F0C5427948D107A01315E4A61D04D2270"),
            82, contract(56, 42, "02CF5998DBDDA6FC02BDC904D3A7F05BDC5013A280777847BC64DB73D96AD626", "1D7CC9DF0616A2AC67CCDC2C1F212F160D4FF1C6DEB4DD00629BF22B3D494DC8"));

    private static final int SHOP_MAGIC = 0x4C575331; // LWS1
    private static final String SHOP_HEADER = String.join("\t",
            "npc_id", "item_id", "order_id", "selling_price", "pack_count",
            "purchasing_price", "note", "daily_buy_limit");
    private static final String ITEM_HEADER = String.join("\t",
            "item_id", "kind", "name_id", "use_token", "inventory_gfx",
            "ground_gfx", "blessing", "stackable", "tradable", "unit_weight",
            "item_description_id", "weapon_type", "damage_small", "damage_large",
            "material", "safe_enchant", "class_mask");
    private static final ShopCatalog SHOPS = loadShops();

    private ClassicWorldIvoryTowerCatalog() {}

    static MapFlags flags(int mapId) {
        MapFlags result = FLAGS.get(mapId);
        if (result == null) throw new IllegalArgumentException("not an Ivory Tower map");
        return result;
    }

    static boolean isStoryNpc(ClassicWorldNpcCatalog.StaticNpc npc) {
        return npc != null && npc.mapId() == 77 && STORY_NPC_IDS.contains(npc.npcId());
    }

    static boolean isEllyonne(ClassicWorldNpcCatalog.StaticNpc npc) {
        return npc != null && npc.mapId() == 76 && npc.npcId() == ELLYONNE_NPC_ID;
    }

    static boolean hasShop(int npcId) {
        return SHOPS.rowsByNpc().containsKey(npcId);
    }

    static List<ShopOffer> shopOffers(int npcId) {
        List<ShopRow> rows = SHOPS.rowsByNpc().getOrDefault(npcId, List.of());
        List<ShopOffer> result = new ArrayList<>(rows.size());
        for (int index = 0; index < rows.size(); index++) {
            ShopRow row = rows.get(index);
            ClassicWorldDropCatalog.ItemWire wire = SHOPS.items().get(row.itemId());
            result.add(new ShopOffer(index, row.sourceOrderId(), row.itemId(),
                    row.sellingPrice(), Math.max(1, row.packCount()),
                    row.purchasingPrice(), wire.inventoryDefinition()));
        }
        return List.copyOf(result);
    }

    static InventoryCatalog.Definition inventoryDefinition(int itemId) {
        ClassicWorldDropCatalog.ItemWire wire = SHOPS.items().get(itemId);
        return wire == null ? null : wire.inventoryDefinition();
    }

    static InventoryCatalog.NativeWeaponStatus nativeWeaponStatus(int itemId) {
        ClassicWorldDropCatalog.ItemWire wire = SHOPS.items().get(itemId);
        return wire == null ? null : wire.nativeWeaponStatus();
    }

    private static MapFlags flags(int mapId, String name, int startX, int endX,
                                  int startY, int endY, boolean teleportable) {
        return new MapFlags(mapId, name, startX, endX, startY, endY,
                true, teleportable, true, true, true, true);
    }

    private static SourceContract contract(int rows, int population,
                                           String source, String resource) {
        return new SourceContract(rows, population, source, resource);
    }

    private static ShopCatalog loadShops() {
        String resourceName = "/world/ivory-tower-shops.lws.gz";
        InputStream resource = ClassicWorldIvoryTowerCatalog.class
                .getResourceAsStream(resourceName);
        if (resource == null) {
            throw new ExceptionInInitializerError("missing Ivory Tower shop resource");
        }
        try (DataInputStream in = new DataInputStream(new GZIPInputStream(resource))) {
            if (in.readInt() != SHOP_MAGIC || in.readUnsignedShort() != 75) {
                throw new IOException("invalid Ivory Tower shop header");
            }
            int shopCount = in.readInt();
            int itemCount = in.readInt();
            byte[] sourceHashBytes = in.readNBytes(32);
            if (sourceHashBytes.length != 32) throw new IOException("truncated shop hash");
            String sourceHash = HexFormat.of().withUpperCase().formatHex(sourceHashBytes);
            byte[] table = in.readAllBytes();
            if (shopCount != SHOP_ROW_COUNT || itemCount != SHOP_ITEM_COUNT
                    || !SHOP_SOURCE_SHA256.equals(sourceHash)
                    || !sourceHash.equals(sha256(table))) {
                throw new IOException("Ivory Tower shop source contract changed");
            }
            return parseShops(table, shopCount, itemCount);
        } catch (IOException | IllegalArgumentException error) {
            throw new ExceptionInInitializerError(error);
        }
    }

    private static ShopCatalog parseShops(byte[] table, int shopCount, int itemCount)
            throws IOException {
        Map<Integer, List<ShopRow>> rowsByNpc = new LinkedHashMap<>();
        Map<Integer, ClassicWorldDropCatalog.ItemWire> items = new LinkedHashMap<>();
        Set<Integer> referencedItems = new LinkedHashSet<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                new ByteArrayInputStream(table), StandardCharsets.UTF_8))) {
            if (!"[shops]".equals(reader.readLine())
                    || !SHOP_HEADER.equals(reader.readLine())) {
                throw new IOException("Ivory Tower shop TSV header changed");
            }
            int rows = 0;
            int previousNpc = 0;
            int previousItem = 0;
            int previousOrder = -1;
            for (String line; (line = reader.readLine()) != null;) {
                if (line.equals("[items]")) break;
                String[] field = line.split("\t", -1);
                if (field.length != 8) throw new IOException("shop field count changed");
                ShopRow row = new ShopRow(integer(field[0]), integer(field[1]),
                        integer(field[2]), integer(field[3]), integer(field[4]),
                        integer(field[5]), field[6], integer(field[7]));
                if ((row.npcId() != RINDA_NPC_ID && row.npcId() != PAGORU_NPC_ID)
                        || row.itemId() <= 0 || row.sourceOrderId() < 0
                        || row.sellingPrice() <= 0 || row.packCount() != 0
                        || row.purchasingPrice() != -1 || row.dailyBuyLimit() != 0
                        || row.npcId() < previousNpc
                        || row.npcId() == previousNpc && row.itemId() < previousItem
                        || row.npcId() == previousNpc && row.itemId() == previousItem
                            && row.sourceOrderId() <= previousOrder) {
                    throw new IOException("invalid or unordered Ivory Tower shop row");
                }
                previousNpc = row.npcId();
                previousItem = row.itemId();
                previousOrder = row.sourceOrderId();
                rowsByNpc.computeIfAbsent(row.npcId(), ignored -> new ArrayList<>())
                        .add(row);
                referencedItems.add(row.itemId());
                rows++;
            }
            if (rows != shopCount || !ITEM_HEADER.equals(reader.readLine())) {
                throw new IOException("Ivory Tower shop/item boundary changed");
            }
            int previousItemId = 0;
            for (String line; (line = reader.readLine()) != null;) {
                String[] field = line.split("\t", -1);
                if (field.length != 17) throw new IOException("shop item field count changed");
                int itemId = integer(field[0]);
                if (itemId <= previousItemId) throw new IOException("shop items unordered");
                previousItemId = itemId;
                ClassicWorldDropCatalog.ItemWire wire =
                        new ClassicWorldDropCatalog.ItemWire(
                                itemId, field[1], field[2], field[3],
                                integer(field[4]), integer(field[5]), integer(field[6]),
                                integer(field[7]) != 0, integer(field[8]) != 0,
                                integer(field[9]), integer(field[10]), field[11],
                                integer(field[12]), integer(field[13]), field[14],
                                integer(field[15]), integer(field[16]));
                items.put(itemId, wire);
            }
        }
        if (items.size() != itemCount || !items.keySet().equals(referencedItems)
                || rowsByNpc.getOrDefault(RINDA_NPC_ID, List.of()).size() != 9
                || rowsByNpc.getOrDefault(PAGORU_NPC_ID, List.of()).size() != 29) {
            throw new IOException("Ivory Tower shop rows/items disagree");
        }
        rowsByNpc.replaceAll((ignored, rows) -> List.copyOf(rows));
        return new ShopCatalog(Map.copyOf(rowsByNpc), Map.copyOf(items));
    }

    private static int integer(String value) {
        return Integer.parseInt(value);
    }

    private static String sha256(byte[] bytes) throws IOException {
        try {
            return HexFormat.of().withUpperCase().formatHex(
                    MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (NoSuchAlgorithmException impossible) {
            throw new IOException("SHA-256 unavailable", impossible);
        }
    }
}
