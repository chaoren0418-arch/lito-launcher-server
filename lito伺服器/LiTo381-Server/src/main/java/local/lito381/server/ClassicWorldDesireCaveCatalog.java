package local.lito381.server;

import java.util.List;
import java.util.Map;

/**
 * Hash-locked selected 380_db / LiTo 3.81C contract for Desire Cave and the
 * Abandoned Land.  This class deliberately contains only facts proven by the
 * selected client and database; quest behaviour is layered on these contracts.
 */
final class ClassicWorldDesireCaveCatalog {
    static final List<Integer> MAP_IDS = List.of(
            600, 601, 602, 603, 604, 605, 606, 607, 778, 779);
    static final List<Integer> GETBACK_MAP_IDS = List.of(600, 601, 778, 779);

    static final String COMPONENT_SOURCE_SHA256 =
            "EB0B89DF5CF9A1D4C310780DC1FF88489948C53C28339C212F5222D31AC302BF";
    static final String DUNGEON_SOURCE_SHA256 =
            "C3164029A2475FDCC231C94324149D3F7BC9DACDB6C3FDC641719C9DF158835D";
    static final String MAPIDS_SOURCE_SHA256 =
            "662EB0F42F1FC53ADF4BA06BEE827D916EFA7FEC32A0A2194C2B51743D8D3DAC";
    static final String GETBACK_SOURCE_SHA256 =
            "99046D58DC7687FFDADFE7BFBC6A9F7C90F943B2C1183D4B2507D5906406E8C7";
    static final String NPC_TELEPORT_SOURCE_SHA256 =
            "E139FA44DF032DB923AF2084CF7760BA437A13A89F702577ABB1EC7824C6F33A";

    record ResourceContract(
            int rows, int secondaryCount, String sourceSha256,
            String resourceSha256) {
        ResourceContract {
            if (rows < 0 || secondaryCount < 0 || sourceSha256 == null
                    || sourceSha256.length() != 64 || resourceSha256 == null
                    || resourceSha256.length() != 64) {
                throw new IllegalArgumentException("invalid Desire Cave resource contract");
            }
        }
    }

    record MapFlags(
            boolean teleportable, boolean escapable, boolean resurrection,
            boolean painwand, boolean penalty, boolean takePets,
            boolean recallPets, boolean usableItem, boolean usableSkill) {}

    static final Map<Integer, String> MAP_SOURCE_SHA256 = Map.ofEntries(
            Map.entry(600, "0B40D8C25E97BF2FD430E055E302BF8B2A40E54625007B6EF1B570443EBF0FDF"),
            Map.entry(601, "D793ED4DB42C26F43393FC97433CE401AC36AF6E0A928B083FA88248ED40DFD7"),
            Map.entry(602, "A93BD617644B4345DB9503D7BBCDC360538BEBE84A3ADF606E470F916A24DCA7"),
            Map.entry(603, "A93BD617644B4345DB9503D7BBCDC360538BEBE84A3ADF606E470F916A24DCA7"),
            Map.entry(604, "928FA1CFD2E78F9E671678B9380AA58FA2EE6F193665FA8D6E81EF3019B88765"),
            Map.entry(605, "16B31FA85E7D78E8839E5172354E1A762F2441197DA1A21CBE10A4599D26F677"),
            Map.entry(606, "81E31754D042187D168555E0D88BE42265E076B2CC1F633E277E96DAB98CC56E"),
            Map.entry(607, "63FC735B5D22AE4D35E7DDB851DEEFC4831CD34A97DA2B8B8A94559FEBE58275"),
            Map.entry(778, "0B0DD4417111AEA3BFAA435EE4080AC7FF9F421C26AD61BF5DE81BF7B0C65405"),
            Map.entry(779, "14AD2127CB90FC8D85F02E6FA8694FB7652D74DB959C9563BCE824A339C3B78B"));

    static final Map<Integer, String> MAP_RESOURCE_SHA256 = Map.ofEntries(
            Map.entry(600, "FA138FC4042B12F82FAD6EB3FFCD5B956FD465FAB45F42980CDE9D31A0DE78D0"),
            Map.entry(601, "6B3A5E5CEF1A4C47861E0009C611D5925E444CD62D279BF0691000BAEAE3D34C"),
            Map.entry(602, "DE77C88C605890E4CA1B9D21930CE23A545D2F21924F72CA82F607B40A1A79E6"),
            Map.entry(603, "37DF71D55CF00A098AF45D240DBE2FA0FAD6769981E188FC9BB5FFEA78DF0F28"),
            Map.entry(604, "B76819F8FFDF72AE64BB6437BAA08535209004DE129D184B5453EB141F3D84D1"),
            Map.entry(605, "5B74F2E2BBB32F63230438DAE78D6700B54926E003DC77E5C970F82C359D913E"),
            Map.entry(606, "A5B3115CF6D60A43AA044C5A9C8F6EDEFBA43CCAE058D12776594D3C438F3E91"),
            Map.entry(607, "8CA8E3C0B4B649876FCFAEF7C8BAE75E06FCB8B598054CF51C8784D33864B0DE"),
            Map.entry(778, "032564681723D9B248C28481119EEF730CD97B69CC107702C7352471DCA41760"),
            Map.entry(779, "D773CA0FC4B77BDD8E032441603A3CC25CD4209A28CD189C929FE836E89DC769"));

    static final Map<Integer, ResourceContract> NPC_CONTRACTS = Map.ofEntries(
            npc(600, 0, "8263164069B03411156DCA1D6B564F777EED14697D36402D02A1086131AEE8BF", "2F9E18C7D977F54DBA65222071F7424FCCB7C424E4216569BAD27DB7ACBC3423"),
            npc(601, 8, "58C0857DBAC77F0C6CD56323664746CE8CF76F388D86A367135D9B283D4DE091", "B15A9F3BCBB766BB7F3D301A9348E64CDC7191F98391230974EBB88F8779D1A8"),
            npc(602, 7, "8EDD47866FB5B808779624A11F5FC1CFF6D6D9CAA7B4D024C3FFBAD21BC53612", "7E0A973AE0413E8C4200DD673FC5AE839984301B878D9027FEA9D1AC29341729"),
            npc(603, 0, "8263164069B03411156DCA1D6B564F777EED14697D36402D02A1086131AEE8BF", "0A169DCC0637857B69101E25293CCDC8AAE598E177A41B5AA89371EC94738F7E"),
            npc(604, 0, "8263164069B03411156DCA1D6B564F777EED14697D36402D02A1086131AEE8BF", "91CC7507F7FA1E69266984CD9D3B974882A3FA7909F8D9CF690A8A761B335F26"),
            npc(605, 0, "8263164069B03411156DCA1D6B564F777EED14697D36402D02A1086131AEE8BF", "9D2E9978154C70298CA4A61BD5BC74FF35066A09CE44B8EB700F94375DE403F3"),
            npc(606, 0, "8263164069B03411156DCA1D6B564F777EED14697D36402D02A1086131AEE8BF", "8CFB264A0D83E4F5B5116F2A0B271661A4298EDFE26FCC133DAB22E8C61805E2"),
            npc(607, 0, "8263164069B03411156DCA1D6B564F777EED14697D36402D02A1086131AEE8BF", "B219C587978E9DA2F8945A7C0AC88332665AF1A0254B27F17295B20289D62865"),
            npc(778, 1, "E56E1C6DB4736355BCD7791305F467BC9253106122FFDD3189CDE4907D20285A", "E320D0AD372C7D4B89D7DC21498A6B7601C21C02D03E1AB9F520418B72CE436B"),
            npc(779, 0, "8263164069B03411156DCA1D6B564F777EED14697D36402D02A1086131AEE8BF", "C8977940B1A28BAFA7245410E1712CDAFA63A48BA81343432049D21DA2C91C81"));

    static final Map<Integer, ResourceContract> MONSTER_CONTRACTS = Map.ofEntries(
            contract(600, 1, 12, "D810906CF34122C1C0F93558BB8B8E3C490DE79C95CB306F074471FF344B8439", "F066303A21D328356B592B8045DF50FE823D197B3EFEA9B2DFE1C9D4F3589FF4"),
            contract(601, 9, 57, "2D1AE549D46E59DF1323ABFB7E82E70B7BD4BD120CAD147AFDCBED8FA5252917", "6A64B8C3EBD94FB85902BD529C4D1305267EE3B20E042487B0B3579AADB785C0"),
            contract(602, 0, 0, "219255B3830F63B32C0D35542B91DF47B5532113719CB79075F5CD233B6DE5E7", "B93F7C36914C087F9447CE22BB730405B74830D478958B56BF98CA5516B89E0A"),
            contract(603, 0, 0, "219255B3830F63B32C0D35542B91DF47B5532113719CB79075F5CD233B6DE5E7", "942E07E5555CCB0019F42A6FD40EE6BAC67144F34FAF8D08B973D96C9564348B"),
            contract(604, 3, 100, "42A9D3365F8041D828FA84EF725D8A73D44FF0B3EBFD405E813C6F776EB0FC25", "0623A7ACBC659E8FB3D63269CB53A94ACE19D8CEBBC45145444D72A51D2BD111"),
            contract(605, 3, 60, "6E12D3917E8FD01CC0596AED924593261B38F9B43899BB16F254F33DE7B5D1E9", "6AC7797A229FFFD7FBA9816AB75C2B913B3BAB1222C6E11DBBFA18DCBF817E3A"),
            contract(606, 3, 60, "2346BB3FCB9176EC8F059BE62DEB68F71CA9397FA1132143DC021072B7E439E3", "53421249D09DDF1D76D2F025795D35E2039F1A26D8DC87E239A097F047B01D8B"),
            contract(607, 2, 60, "2F291CCD95F917F2242B39F93B1B4D518C01D4741B930C42359912E10CE68EFB", "0776C3F066B43DE6AD7DBA6DE05C372D7EAB179142999F5EC6068E98A1B95882"),
            contract(778, 13, 174, "F80BE4B6455A9D941A6FB8EFE2E02DE10CBE5F7FD2841792DF2A492E6D44A3E4", "0F46805CE1DFCD02FF8CAAC3C4CBCE37FC91A8AFEC395BA39B8FCF04D6C5BA67"),
            contract(779, 3, 60, "61DED3DA5F59E3A6BA0CEACEF6C4244DF84FCAB7145AF51533645B22229A3D46", "EAC49004358F6331DFFBCA460A6E01D50C596B17252B897D7625B1CC9D17655B"));

    static final Map<Integer, ResourceContract> DROP_CONTRACTS = Map.ofEntries(
            contract(600, 0, 0, "3FE93EDCC0A0A171C6BB8ADF328460441832AF757DA26B9DAB1E8C8A2963C828", "2C210B6D056E8FBB12B9BEC7BC5930F423D4314A8D399F3C3C9A07A4C9767300"),
            contract(601, 20, 14, "DD4606506E599CF9732D5B36B8E3ED58D4C5D8B7B8BA347FF968A45B47973214", "C3AE0DD3952214594B1C953BC40DFD89E3C94B35DACB9346FB45BFD6781D7A1F"),
            contract(602, 0, 0, "3FE93EDCC0A0A171C6BB8ADF328460441832AF757DA26B9DAB1E8C8A2963C828", "CC38608D2C6328F4CD3CCDFC11B73138105A10B3CCC133EC9215C0776EA52D23"),
            contract(603, 0, 0, "3FE93EDCC0A0A171C6BB8ADF328460441832AF757DA26B9DAB1E8C8A2963C828", "F0F66970881FC448BC294FC2A4DD75D00402FECBFCBF2A24BACEFA3AF9603FC0"),
            contract(604, 19, 12, "A15DD3D856851AB9B9D5EDA26EBAA07465930BB78189F80F4B8B28184443CF22", "D4C369920EBE36A862B4FCF1E74A0B9DEF0DBE540687D0F46BE751A127D6E7BA"),
            contract(605, 19, 12, "C0E3EA861DA672C7AF2FEC5D0E760265587BCA3B2F65FAA235AEAFA0513F8457", "3DF110964FCCE525DB207A3E48B4207D179BB8CB6E66487CA59C0F1AF6BA328A"),
            contract(606, 5, 5, "A351DA2FB2D7A421620066F1129E29FCCB34F0886A1B41DD42E2B3B4D13187CA", "32FF490F1CD1380504A5EDFB934F7F0E90C5C83BF273FD5AAE54D6F2AE5CCB4D"),
            contract(607, 12, 11, "5D48BE3D524F5C13D053787759A40BE6ACFFA1D983F1DCCF92A2CD9D3D0601FE", "54FBF42D18E10DD8B8055AF658BD557BA02246C835959CF720DEC5DA93A7A98C"),
            contract(778, 22, 9, "3A9670FB7CB8663389B2E90AFA712E4E7A91440BA70EF1B76CF7B1E7F8BA9188", "C8DBF4AFF8BF47EBFE330FC5E291118473721257C1DA83737825390FA02914CC"),
            contract(779, 5, 3, "C0DC8698498BFC219565619B5D83BB7F9201D66D3DB4419E3EBAA137BCC98108", "05060A7702C7F62E3260D43C61CE2C85F359EF6808D48DC7BBA728594743F1CC"));

    static final Map<Integer, MapFlags> FLAGS = Map.ofEntries(
            Map.entry(600, allFlags()), Map.entry(601, allFlags()),
            Map.entry(602, allFlags()), Map.entry(603, allFlags()),
            Map.entry(604, new MapFlags(true, true, true, true, true,
                    false, false, true, true)),
            Map.entry(605, allFlags()), Map.entry(606, allFlags()),
            Map.entry(607, allFlags()), Map.entry(778, allFlags()),
            Map.entry(779, allFlags()));

    /* Exact selected npc.karma values for monsters present in the component. */
    static final Map<Integer, Integer> MONSTER_KARMA = Map.ofEntries(
            Map.entry(45736, -2), Map.entry(45739, -6),
            Map.entry(45740, -6), Map.entry(45741, -6),
            Map.entry(45742, -6), Map.entry(45743, -6),
            Map.entry(45744, -6), Map.entry(45745, -6),
            Map.entry(45746, -7), Map.entry(45747, -10),
            Map.entry(45748, -5), Map.entry(45750, -8),
            Map.entry(46068, -1), Map.entry(46069, -1),
            Map.entry(46070, 0), Map.entry(46072, 0),
            Map.entry(46074, -1), Map.entry(46075, -1),
            Map.entry(46076, -1), Map.entry(46077, -1),
            Map.entry(46078, -1), Map.entry(46079, -1),
            Map.entry(46080, -1), Map.entry(46081, -1),
            Map.entry(46082, -5), Map.entry(46085, -5),
            Map.entry(46088, -5), Map.entry(46091, -5));

    private ClassicWorldDesireCaveCatalog() {}

    static boolean supports(int mapId) {
        return MAP_SOURCE_SHA256.containsKey(mapId);
    }

    static MapFlags flags(int mapId) {
        MapFlags result = FLAGS.get(mapId);
        if (result == null) throw new IllegalArgumentException("not a Desire Cave map");
        return result;
    }

    static int monsterKarma(int npcId) {
        return MONSTER_KARMA.getOrDefault(npcId, 0);
    }

    /** Retained selected config/rates.properties RateKarma=1.0. */
    static int monsterKarmaDelta(int npcId, int playerKarmaLevel) {
        int karma = monsterKarma(npcId);
        if (karma != 0 && Integer.signum(playerKarmaLevel) != 0
                && Integer.signum(karma) != Integer.signum(playerKarmaLevel)) {
            karma *= 5;
        }
        return karma;
    }

    static boolean isDialogueNpc(ClassicWorldNpcCatalog.StaticNpc npc) {
        return npc != null && npc.npcId() >= 80058 && npc.npcId() <= 80074;
    }

    static String dialogueHtml(
            int npcId, int playerLevel, int currentGfx,
            GameStateStore.FactionKarmaBalance karma,
            Map<Integer, Integer> quests, Map<Long, Integer> inventory) {
        int karmaValue = karma == null ? 0 : karma.value();
        int karmaLevel = karma == null ? 0 : karma.level();
        return switch (npcId) {
            case 80058 -> playerLevel <= 44 ? "cpass03"
                    : playerLevel <= 51 ? "cpass02" : "cpass01";
            case 80059 -> dimensionDoorHtml(
                    karmaLevel, inventory, 40909, 40913, 40917,
                    new long[] {40912, 40910, 40911}, "wpass14");
            case 80060 -> dimensionDoorHtml(
                    karmaLevel, inventory, 40912, 40916, 40920,
                    new long[] {40909, 40910, 40911}, "wpass13");
            case 80061 -> dimensionDoorHtml(
                    karmaLevel, inventory, 40910, 40914, 40918,
                    new long[] {40909, 40912, 40911}, "wpass11");
            case 80062 -> dimensionDoorHtml(
                    karmaLevel, inventory, 40911, 40915, 40919,
                    new long[] {40909, 40912, 40910}, "wpass12");
            case 80063 -> "gpass01";
            case 80064 -> karmaLevel < 1 ? "meet003" : "meet001";
            case 80065 -> karmaLevel < 3 ? "uturn0" : "uturn1";
            case 80066 -> karmaValue >= 10_000_000 ? "betray01" : "betray02";
            case 80067 -> minicodHtml(currentGfx, karmaLevel, quests, inventory);
            case 80068 -> "fakebarlog1";
            case 80069 -> "ldemon1";
            case 80070 -> "tldemon1";
            case 80071 -> barlogEarringHtml(karmaLevel, inventory);
            case 80072 -> switch (karmaLevel) {
                case 1 -> "lsmith0"; case 2 -> "lsmith1";
                case 3 -> "lsmith2"; case 4 -> "lsmith3";
                case 5 -> "lsmith4"; case 6 -> "lsmith5";
                case 7 -> "lsmith7"; case 8 -> "lsmith8";
                default -> "";
            };
            case 80073 -> "l_hunt";
            case 80074 -> karmaValue >= 10_000_000 ? "infamous01" : "infamous02";
            default -> null;
        };
    }

    static int necessarySealCount(Map<Long, Integer> inventory) {
        int rulers = 0;
        for (long itemId : new long[] {40917, 40920, 40918, 40919}) {
            if (has(inventory, itemId)) rulers++;
        }
        return switch (rulers) {
            case 0 -> 10; case 1 -> 100; case 2 -> 200;
            default -> 500;
        };
    }

    private static String dimensionDoorHtml(
            int karmaLevel, Map<Long, Integer> inventory,
            long protection, long seal, long ruler,
            long[] otherProtections, String rulerHtml) {
        if (karmaLevel > 0) return "cpass03";
        if (has(inventory, 40921)) return "wpass02";
        if (has(inventory, ruler)) return rulerHtml;
        for (long itemId : otherProtections) {
            if (has(inventory, itemId)) return "wpass04";
        }
        if (has(inventory, protection)) {
            return count(inventory, seal) >= necessarySealCount(inventory)
                    ? "wpass06" : "wpass03";
        }
        return has(inventory, seal) ? "wpass08" : "wpass05";
    }

    private static String minicodHtml(
            int currentGfx, int karmaLevel, Map<Integer, Integer> quests,
            Map<Long, Integer> inventory) {
        int quest36 = quests == null ? 0 : quests.getOrDefault(36, 0);
        int quest37 = quests == null ? 0 : quests.getOrDefault(37, 0);
        if (quest36 == 255) return "minicod10";
        if (karmaLevel >= 1) return "minicod07";
        if (quest36 == 1) return currentGfx == 6034 ? "minicod03" : "minicod05";
        if (quest37 == 255 || has(inventory, 41121) || has(inventory, 41122)) {
            return "minicod01";
        }
        if (has(inventory, 41130) && has(inventory, 41131)) return "minicod06";
        return has(inventory, 41130) ? "minicod02" : "minicod08";
    }

    private static String barlogEarringHtml(
            int karmaLevel, Map<Long, Integer> inventory) {
        if (karmaLevel < 1 || karmaLevel > 8) return "lring0";
        int earringLevel = 0;
        for (int level = 1; level <= 8; level++) {
            if (has(inventory, 21019L + level)) {
                earringLevel = level;
                break;
            }
        }
        return earringLevel >= karmaLevel ? "lringd" : "lring" + karmaLevel;
    }

    private static boolean has(Map<Long, Integer> inventory, long itemId) {
        return count(inventory, itemId) > 0;
    }

    private static int count(Map<Long, Integer> inventory, long itemId) {
        return inventory == null ? 0 : inventory.getOrDefault(itemId, 0);
    }

    private static Map.Entry<Integer, ResourceContract> npc(
            int mapId, int rows, String sourceSha256, String resourceSha256) {
        return contract(mapId, rows, 0, sourceSha256, resourceSha256);
    }

    private static Map.Entry<Integer, ResourceContract> contract(
            int mapId, int rows, int secondaryCount,
            String sourceSha256, String resourceSha256) {
        return Map.entry(mapId, new ResourceContract(
                rows, secondaryCount, sourceSha256, resourceSha256));
    }

    private static MapFlags allFlags() {
        return new MapFlags(true, true, true, true, true,
                true, true, true, true);
    }
}
