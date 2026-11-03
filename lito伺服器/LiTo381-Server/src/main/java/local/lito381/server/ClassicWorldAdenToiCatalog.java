package local.lito381.server;

import java.util.List;
import java.util.Map;

/** Exact selected-source contracts for Aden Castle, TOI 1-10F and waterway. */
final class ClassicWorldAdenToiCatalog {
    static final int CASTLE_ID = 7;
    static final int ADEN_INTERIOR_MAP_ID = 300;
    static final int WATERWAY_MAP_ID = 301;
    static final List<Integer> TOI_MAP_IDS = List.of(
            101, 102, 103, 104, 105, 106, 107, 108, 109, 110);
    static final List<Integer> MAP_IDS = List.of(
            101, 102, 103, 104, 105, 106, 107, 108, 109, 110, 300, 301);

    static final String COMPONENT_SOURCE_SHA256 =
            "46159440BE30E1C4C457846289A63D964C2E8C77736F21F4009F8EB49221D4B5";
    static final String COLLISION_AUTHORITY_SHA256 =
            "540E0387CD659569CE42883202D309A7749CF605BEF3E6F121DA9A6578C0C9AC";
    static final String ROUTES_SOURCE_SHA256 =
            "15EE966A3726535ABE8DD8415A08ED09262DBE479355AFEA5EEFF5C417223176";
    static final String FLAGS_SOURCE_SHA256 =
            "DA17CC5D7CFEF5CD61A24FAEBC8AD12FAB6CDFFBBAB2D8953C1428C319F009F5";
    static final String GETBACK_SOURCE_SHA256 =
            "D664A9EF3EDD4BEA470784DB1EF771F27807316AEDD7EC3DF2C99B28A4FB621A";
    static final String NPC_TELEPORT_SOURCE_SHA256 =
            "697BDCFCF82C45149F1C1E412B2A9FC8718EF0645811D46A566ABD77459BE9F3";
    static final String NPC_ACTION_SOURCE_SHA256 =
            "849407622E138430901D5919541F8199E5D3F1AB1444B97C7DA3415AAB6DDEC7";
    static final String DOOR_SOURCE_SHA256 =
            "F2776DCFA97F55CCAD4CB03A35E17FE77B4F1E2708B78F261811792C0AEC11B9";
    static final String GATEKEEPER_SOURCE_SHA256 =
            "A5C4E48AED1DAC2A4B214C57DF45A3357341DC72E4AA8C4A36828E1D9F7EEC7D";
    static final String ACCOUNT_CASTLE_SOURCE_SHA256 =
            "5FD22CE757395E9BC72D1972F28DC2F185FCBE704358F197C3EB04D8F3AB0524";

    static final int TELEPORTER_SPAWN_ID = 84;
    static final int TELEPORTER_NPC_ID = 50047;
    static final int WAREHOUSE_SPAWN_ID = 276;
    static final int WAREHOUSE_NPC_ID = 60015;
    static final int WEAPON_ENCHANTER_SPAWN_ID = 743;
    static final int WEAPON_ENCHANTER_NPC_ID = 70686;
    static final int ARMOR_ENCHANTER_SPAWN_ID = 746;
    static final int ARMOR_ENCHANTER_NPC_ID = 70692;
    static final int COMMANDER_SPAWN_ID = 747;
    static final int COMMANDER_NPC_ID = 70695;
    static final int STEWARD_SPAWN_ID = 751;
    static final int STEWARD_NPC_ID = 70721;
    static final int STEWARD_X = 32898;
    static final int STEWARD_Y = 32524;
    static final int GATEKEEPER_SPAWN_ID = 932;
    static final int GATEKEEPER_NPC_ID = 70996;
    static final String OPEN_INNER_GATE_ACTION = "openigate";
    static final String CLOSE_INNER_GATE_ACTION = "closeigate";
    static final String MEMBER_GATE_HTML = "gatekeeper";
    static final String OUTSIDER_GATE_HTML = "gatekeeperop";
    static final String COMMANDER_DEMAND_ACTION = "demand";
    static final String COMMANDER_EMPTY_HTML = "biust2";
    static final List<String> TEXT_ONLY_GATE_ACTIONS = List.of(
            "healegate_heine outer gate north",
            "healegate_heine outer gate south",
            "autorepairon", "autorepairoff");

    record MapFlags(int mapId, String locationName,
                    int startX, int endX, int startY, int endY) {
        MapFlags {
            if (!MAP_IDS.contains(mapId) || locationName == null || locationName.isBlank()
                    || startX <= 0 || endX < startX || startY <= 0 || endY < startY) {
                throw new IllegalArgumentException("invalid Aden/TOI map flags");
            }
        }
    }

    record ResourceContract(int rows, int secondaryCount,
                            String sourceSha256, String resourceSha256) {
        ResourceContract {
            if (rows < 0 || secondaryCount < 0 || sourceSha256 == null
                    || sourceSha256.length() != 64 || resourceSha256 == null
                    || resourceSha256.length() != 64) {
                throw new IllegalArgumentException("invalid Aden/TOI resource contract");
            }
        }
    }

    record Door(int doorId, int gfxId, int x, int y, int mapId, int direction,
                int edgeLow, int edgeHigh, int maximumHp, int keeperNpcId) {
        Door {
            if (doorId != 2060 || gfxId <= 0 || mapId != 4
                    || direction != 0 || edgeLow <= 0 || edgeHigh < edgeLow
                    || maximumHp != 0 || keeperNpcId != GATEKEEPER_NPC_ID) {
                throw new IllegalArgumentException("invalid retained Aden inner gate");
            }
        }

        long objectId() {
            return 0x6C00_0000L | doorId;
        }

        int entranceY() {
            return y + 1;
        }

        boolean controls(int cellX, int cellY) {
            return cellY == entranceY()
                    && cellX >= edgeLow - 1 && cellX <= edgeHigh + 1;
        }
    }

    private static final Map<Integer, MapFlags> FLAGS = Map.ofEntries(
            Map.entry(101, flags(101, "傲慢之塔 1樓", 32704, 32895, 32704, 32895)),
            Map.entry(102, flags(102, "傲慢之塔 2樓", 32704, 32895, 32704, 32895)),
            Map.entry(103, flags(103, "傲慢之塔 3樓", 32704, 32895, 32704, 32895)),
            Map.entry(104, flags(104, "傲慢之塔 4樓", 32576, 32767, 32768, 32959)),
            Map.entry(105, flags(105, "傲慢之塔 5樓", 32576, 32767, 32768, 32959)),
            Map.entry(106, flags(106, "傲慢之塔 6樓", 32576, 33855, 32768, 32959)),
            Map.entry(107, flags(107, "傲慢之塔 7樓", 32576, 32767, 32768, 32959)),
            Map.entry(108, flags(108, "傲慢之塔 8樓", 32576, 32767, 32768, 32959)),
            Map.entry(109, flags(109, "傲慢之塔 9樓", 32576, 32767, 32768, 32959)),
            Map.entry(110, flags(110, "傲慢之塔 10樓", 32704, 32895, 32704, 32895)),
            Map.entry(300, flags(300, "亞丁內城", 32832, 32959, 32448, 32639)),
            Map.entry(301, flags(301, "傲慢之塔地下水路", 32576, 32767, 32768, 33023)));

    static final Map<Integer, String> MAP_SOURCE_SHA256 = Map.ofEntries(
            Map.entry(101, "6FF26EA3826903D274835A28290E0A5F0CD271B5E3970F149296A05FA979DD4A"),
            Map.entry(102, "33F4B35976C4307D5397FC4AAE4A4FC0615E076FB23B87F58613081553E472F1"),
            Map.entry(103, "8E3F88659D1A42967F57DC8FC9D902CC4254E50BC5057363C37049CA352C5ABE"),
            Map.entry(104, "5DFCF849F13FC240637C137DECDBD146F732D24E7A82EF687F7CF80F595B182F"),
            Map.entry(105, "D7EEC9788AC45BAF27F6FCEDAB99730A30391EA2ADEA44D1E6261B2F2AD9B388"),
            Map.entry(106, "44231C26C1C611D7E5653FFE7658F922EEE02C430AAD53EAD350F1776E30D10C"),
            Map.entry(107, "A61D6AF8A6D803AE64B8BA8C19AABA4B7130DABD9A0B3E71FDB5A9C58F9D75B2"),
            Map.entry(108, "94739BF39D711625180B43F59A4B89A1BE8ACB19AC5A7E607B23A8E6F8A4AE04"),
            Map.entry(109, "E1EBA86A53420A359E308EF78AABD119E72F01233EB26D70B4126BC349831CC7"),
            Map.entry(110, "5D97CF6C9146F0756BC1A26B6A3F9AA10E0DB86D6085FA7490A887FD139543C3"),
            Map.entry(300, "6FF7ECB6D534075295C96359A1771696B99C5AC3C3F169D746D90586412B9EB0"),
            Map.entry(301, "EE6C4488DD499AB419B0F07436E0D6A920D53DBFE9A25B4AA5B79A9A849524E7"));

    static final Map<Integer, String> MAP_RESOURCE_SHA256 = Map.ofEntries(
            Map.entry(101, "DA5FE66337C21A09D5F0F059282E06B9132CA3BD391D0FB96390293C2F418DE6"),
            Map.entry(102, "E2D7CA6CEAB5B29E5188E9D5E18A006463091A541ABDBCF4DF338280C8F51F95"),
            Map.entry(103, "3B907D76F3E6875AE672D0E2DE53FD5112CA74BFA88390D3A846DC11F8F672AE"),
            Map.entry(104, "94FD45F2653FE58B9441DD1E7C194B4261D05CE1807BD8C0041838DC1646AF02"),
            Map.entry(105, "386CEC66FFCD83B8E3F8234E3F76C9853EAE46C9E712F66044D696200B1B1449"),
            Map.entry(106, "03D566F17D5B1086C67008AA730B1F3E695F071F4CB1B12FC1068A3D435B8182"),
            Map.entry(107, "F3FC42017B89A52EA73560C5391959540D0D8843B3328A3FC2B183968616957B"),
            Map.entry(108, "9CFA4D4B38BC3EBB53384C6206A75118BA65C5D77C0319D34A0562D0ED8B6501"),
            Map.entry(109, "B4BA50A29E9E9904A1D9345966108A2EB8003AC10AF2AF4CFADD762BBDD4E082"),
            Map.entry(110, "A244CDC78FD889726166F70E591CDA60AF6897CA3FFDE4B757FED9C624D813B0"),
            Map.entry(300, "CBF733E1BBC856ECF4C1566207BB541FB9D4781CB7DD065DD0DA3C62554B41E6"),
            Map.entry(301, "8331B00E3BD693C1FE966D6BA4E4927F5063A7D2B8231C58DBFE324518C6368A"));

    static final Map<Integer, String> CLIENT_MAP_SHA256 = Map.ofEntries(
            Map.entry(101, "563C8E81774939A44B807E3A328E62D1E4C77F4C7B513EFD7274A7E2953E475A"),
            Map.entry(102, "DCDF17FEA43F7A2FEC98E432D73056EAE8E68E2E3DF32F7F1016B8261191BA9E"),
            Map.entry(103, "612F8412CF4867D1F169932E9DD47C5FFBF2B9006007234055F81C811771FDE2"),
            Map.entry(104, "FA09F87004F913E5E46B2369DA71988824AC2D880C7F1C708ECA8C07C2901F29"),
            Map.entry(105, "68A827A3E048BDCA66124C8B96E352A850903F9B07D66144DF8586337FA52ED9"),
            Map.entry(106, "60C09DDF52B713C09220CEB10BC7D884BDE6776A26282CF11BBA0D4F8313E0E1"),
            Map.entry(107, "53F4B8B14F087B11EF24E82A5ADD90DAA69DD60461BC277CBFBF644E9862B3C2"),
            Map.entry(108, "BDBB19BC459046CCCF2543DF77916A7AE1A89AFF49F12377A7383B85A0123A3D"),
            Map.entry(109, "8F0FA330A050AC35D8A1B1D9CA0B6B1ADB571D599BDB38D652959F9139623866"),
            Map.entry(110, "AAB101DD75B10C61238944CF3EDB5864756FE62049A3FC6163636A1193393C99"),
            Map.entry(300, "AA5D1766144382EF6FB7D970FB4CB49B5B278A7BE9FF987F971EF2A480AAE844"),
            Map.entry(301, "32AF1AE9A6B94F2550746A6EE8BC94CEC132C269E8DB8ECB7E2435323330C6D5"));

    static final Map<Integer, ResourceContract> NPC_CONTRACTS = Map.ofEntries(
            npc(101, 0, "FA1380F626DF3B0B647D2CE18369AFAB14C273F2E759C2D9C50346C19936F67B"),
            npc(102, 0, "2012709029267FB839C1A13117CD54706D055D16F4AEDF0810D97F40549B8375"),
            npc(103, 0, "F949083C2C7A5AC997D72C4EB3B36C89B9AF9282368F359E328AEB013824ACE4"),
            npc(104, 0, "DCD53A7F8C8ECFC4F5AE67691B9BC9AEB9AEA3BDCE36CAC02B66CD6C27620CFF"),
            npc(105, 0, "AD8E31B0CA56AFE37BF9738DEA7ABBA6D600BD85FBE7381C98EBF1EE68E217BF"),
            npc(106, 0, "D839C6B1FBEB14DAE95610DF024BF1C8B5704A06B3E99C24F0EA314495564BEE"),
            npc(107, 0, "871E92BD712802033598311EA3605EC5252709DB06D415344E4959E8DAE431E0"),
            npc(108, 0, "DD6CE59E537BF169B5D4246E57CF8E5FF957321E948D4322B547F69883909C6B"),
            npc(109, 0, "763DFA2DD585E3392D203B3C32664732803A577E62C6BDE4AD3F3877F32FF8D5"),
            npc(110, 0, "ABDD865884F24B02B68D60090246D7AB7E249839D86C6A54E6603616FD865A7C"),
            Map.entry(300, contract(6, 0,
                    "93563B2B3AF5EF598D29108940B3B6A7179130999F4BED8F0117FB7CA7870A37",
                    "5D9B1B8ACF82B5D856628D351FFAF568A26E22040CCCC5D6E9C9F8D93178D238")),
            npc(301, 0, "3C16F59478CC9D7F1B86E96F89BDEEA85449E8E3714FB6E01DA0BC68DA5196AD"));

    static final Map<Integer, ResourceContract> MONSTER_CONTRACTS = Map.ofEntries(
            monster(101, 0, 0, "219255B3830F63B32C0D35542B91DF47B5532113719CB79075F5CD233B6DE5E7", "E11A81F9FB053A97CBC54AB3DAF96085843A11A7923DBC6F14089117EACF4371"),
            monster(102, 4, 79, "49756B1D38CCCDD3C44E4801466585FF9254E6E4B2B2993ED22B5E06A5053192", "82D696C81A621CA986B4E06AF72BD241050455272BF64CCF5A59F9EAAD5AFD4E"),
            monster(103, 5, 92, "4C31BD185CAAC2FD1622717C62BFAB88BB5422B9B908422E64341360BA7D1AA5", "C8D40088E03DF41E16C1DFFF3268A6E20FBDC6DB98B6FCDBFD7DC6B0B940A4D3"),
            monster(104, 5, 82, "6DE2D12CDC5500249DAE724A933F6A81FBBC4621FA998DD3DA72223C0217A359", "B42612F6201FCB0FA2C26F46E5067BB1C6A851097CE59CB5BE3A622B31965454"),
            monster(105, 6, 76, "A5F2FCCEDE10D3FCA081A3C70CBC9BCED82A0FB3DBEC8DA6BF46E97E3D296F74", "5A5BBB91651E3D9CB73C8B21089A0BFBC2B2E279C65E34D97824E3C2C794011B"),
            monster(106, 0, 0, "219255B3830F63B32C0D35542B91DF47B5532113719CB79075F5CD233B6DE5E7", "988B194AECDDFC0ADBE270CF02FE2653C75C2409259DABB6E36A51F624146978"),
            monster(107, 6, 66, "F60F16A016068754D2D7128A48A02877D35E6A53A777605D9BF890344E695606", "5FAF678CB13CB06743C2721ED751B177DBCA600E3031BDA4910F9B3C7298C3A2"),
            monster(108, 6, 70, "6ABDBD7C21B482C3C565369BB2F64773074824C3835B842E202D1F82B1A80A87", "7D8524D3376FD36CC1936FC717DBF4A2D1FA17DBFF898B2D089EDF1DB9F30347"),
            monster(109, 6, 66, "3FCA5E233E70473A7DFC2C5A0C6134BB62BED48B2EA38232FD9E9D8E051FB40D", "BD825E9147C576851900D867B1D2B29B289187D22DDDFDAB4FAAA20FFC07691A"),
            monster(110, 6, 54, "0A9649CCB995638A5055C84390AA1068C571E5BC762B521AD877C560FBB960AE", "BD79D26DD26FB7841498A27EDAA9565DAFD3BD49D1B74D1C898C06762AE366A9"),
            monster(300, 0, 0, "219255B3830F63B32C0D35542B91DF47B5532113719CB79075F5CD233B6DE5E7", "25EBC07B0BF7178018176DFF0A45B7D9D1A12BDBCA61A80FF6450D349B8EAA81"),
            monster(301, 2, 40, "11C112D4AA5ADE072665F637C22ECE9C2EF0F54386BC0E21EC9A51909CAA7DA9", "CE5ADBC0C4C5350C3AE51D91C0DB2F04A30CC7A9BF65B2A9168898E9AC522EEA"));

    static final Map<Integer, ResourceContract> DROP_CONTRACTS = Map.ofEntries(
            drop(101, 0, 0, "3FE93EDCC0A0A171C6BB8ADF328460441832AF757DA26B9DAB1E8C8A2963C828", "9626DC96BA8BFA083B6E1AF6A134DAAF16E6368B3C80F0FA8807ECC6BE650E8E"),
            drop(102, 37, 27, "9D751115318BFCA5FC2FBDC480D6B6591FABDCCBF984B4ED86C05773F3722AE4", "D3EA372A38B007211AF82A0F223484387512460E150E17B258EF579BA66483EC"),
            drop(103, 56, 38, "E15FD7700604FDF9DD087E50E73634471EE63B7BC8B36F7CFF2331BF8447DF8F", "161937715D610DAD3C3048964F08DF16AE427C13C6D2F8532565CAFB7E64A8EB"),
            drop(104, 56, 38, "E15FD7700604FDF9DD087E50E73634471EE63B7BC8B36F7CFF2331BF8447DF8F", "B084F184A2D352BA49C8557000DA1FBBB282D69CE152ACC1B81DA4CAA731A2A5"),
            drop(105, 39, 34, "3D7A1A388F8141AF5D41E2D7A806BA56DE0538DCD62DC821A7A19750CD45D284", "581968C6B3B44F474278C43DEC6ED74339ED0C8192BFB25FEFBC2E0C35E3F33A"),
            drop(106, 0, 0, "3FE93EDCC0A0A171C6BB8ADF328460441832AF757DA26B9DAB1E8C8A2963C828", "E7C3BB7D379C518F70F0018FE436C6ECA69EE72901E7B69B7BDA905316351D90"),
            drop(107, 39, 34, "3D7A1A388F8141AF5D41E2D7A806BA56DE0538DCD62DC821A7A19750CD45D284", "54236C21463B5E4B3BD97C38D8AB2D53F76F3DCE5A2F50B5554C5FE28E720EB4"),
            drop(108, 39, 34, "3D7A1A388F8141AF5D41E2D7A806BA56DE0538DCD62DC821A7A19750CD45D284", "AE8DD56E3AD42D590811E65CF9D7793160FFC45015465BAA1F35BF7035C9D63D"),
            drop(109, 39, 34, "3D7A1A388F8141AF5D41E2D7A806BA56DE0538DCD62DC821A7A19750CD45D284", "3E4978E637F4C9EFB4C5FF5EEB554AB26E42991DE0C6455DBF63A5C997AEA0B7"),
            drop(110, 39, 34, "3D7A1A388F8141AF5D41E2D7A806BA56DE0538DCD62DC821A7A19750CD45D284", "E5C6DEDCBA9E772ABB70C7C192B8CA91EB79FA041E61A5F6CD5E6D1EBF6A6486"),
            drop(300, 0, 0, "3FE93EDCC0A0A171C6BB8ADF328460441832AF757DA26B9DAB1E8C8A2963C828", "3072BA4F3B784B3B83B03DB143BD1BECA6010096E91D958810FDE61AADB2D969"),
            drop(301, 19, 17, "D780D81ABEA53A989C04493F42557F339D81A7E048E4604AE4CA3EA452F8CB46", "879244BE7456653A7EEA9A7F1696C026A8BF3D39159DBD67446B27EA6520CD96"));

    private static final Door INNER_GATE = new Door(
            2060, 1336, 34091, 33203, 4, 0, 34090, 34091, 0, 70996);

    private ClassicWorldAdenToiCatalog() {}

    static MapFlags flags(int mapId) {
        MapFlags result = FLAGS.get(mapId);
        if (result == null) throw new IllegalArgumentException("not an Aden/TOI map");
        return result;
    }

    static Door innerGate() {
        return INNER_GATE;
    }

    static boolean isSteward(ClassicWorldNpcCatalog.StaticNpc npc) {
        return exactInterior(npc, STEWARD_SPAWN_ID, STEWARD_NPC_ID,
                STEWARD_X, STEWARD_Y);
    }

    static boolean isCommander(ClassicWorldNpcCatalog.StaticNpc npc) {
        return exactInterior(npc, COMMANDER_SPAWN_ID, COMMANDER_NPC_ID,
                32892, 32524);
    }

    static boolean isWeaponEnchanter(ClassicWorldNpcCatalog.StaticNpc npc) {
        return exactInterior(npc, WEAPON_ENCHANTER_SPAWN_ID,
                WEAPON_ENCHANTER_NPC_ID, 32876, 32536);
    }

    static boolean isArmorEnchanter(ClassicWorldNpcCatalog.StaticNpc npc) {
        return exactInterior(npc, ARMOR_ENCHANTER_SPAWN_ID,
                ARMOR_ENCHANTER_NPC_ID, 32875, 32536);
    }

    static boolean isGatekeeper(ClassicWorldNpcCatalog.StaticNpc npc) {
        return npc != null && npc.mapId() == 4
                && npc.spawnId() == GATEKEEPER_SPAWN_ID
                && npc.npcId() == GATEKEEPER_NPC_ID
                && npc.x() == 34092 && npc.y() == 33205;
    }

    static boolean isTextOnlyGateAction(String action) {
        return action != null && TEXT_ONLY_GATE_ACTIONS.stream()
                .anyMatch(candidate -> candidate.equalsIgnoreCase(action));
    }

    private static boolean exactInterior(ClassicWorldNpcCatalog.StaticNpc npc,
                                         int spawnId, int npcId, int x, int y) {
        return npc != null && npc.mapId() == ADEN_INTERIOR_MAP_ID
                && npc.spawnId() == spawnId && npc.npcId() == npcId
                && npc.x() == x && npc.y() == y;
    }

    private static MapFlags flags(int mapId, String name,
                                  int startX, int endX, int startY, int endY) {
        return new MapFlags(mapId, name, startX, endX, startY, endY);
    }

    private static Map.Entry<Integer, ResourceContract> npc(
            int mapId, int rows, String resource) {
        return Map.entry(mapId, contract(rows, 0,
                "8263164069B03411156DCA1D6B564F777EED14697D36402D02A1086131AEE8BF",
                resource));
    }

    private static Map.Entry<Integer, ResourceContract> monster(
            int mapId, int rows, int population, String source, String resource) {
        return Map.entry(mapId, contract(rows, population, source, resource));
    }

    private static Map.Entry<Integer, ResourceContract> drop(
            int mapId, int rows, int items, String source, String resource) {
        return Map.entry(mapId, contract(rows, items, source, resource));
    }

    private static ResourceContract contract(
            int rows, int secondaryCount, String source, String resource) {
        return new ResourceContract(rows, secondaryCount, source, resource);
    }
}
