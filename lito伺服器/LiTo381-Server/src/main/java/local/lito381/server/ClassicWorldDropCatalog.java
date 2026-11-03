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
import java.util.List;
import java.util.Map;
import java.util.random.RandomGenerator;
import java.util.zip.GZIPInputStream;

/** Hash-locked selected-source droplist and item wire metadata for classic WORLD. */
final class ClassicWorldDropCatalog {
    static final int MAP0_DROP_COUNT = 103;
    static final int MAP0_ITEM_COUNT = 65;
    static final String MAP0_SOURCE_SHA256 =
            "32314C4E4BC0A1363551EAF202528F21CB7CC0D4AE550AB3780F5B1BB9A5B3C5";
    static final String MAP0_RESOURCE_SHA256 =
            "3B04508E6A3806EF43118A806E58509902FA017F368C4692D16F5ABA26AB05A9";
    static final int MAP1_DROP_COUNT = 29;
    static final int MAP1_ITEM_COUNT = 26;
    static final String MAP1_SOURCE_SHA256 =
            "CFE0327124FF0F651C562D40D4BF8C5FE665FF8358B7727B3263D1DD37BB88F9";
    static final String MAP1_RESOURCE_SHA256 =
            "B1CBCDE701FF72CB37C79455DD3609317E62DFD4692852BE55E5D416E479E483";
    static final int MAP2_DROP_COUNT = 89;
    static final int MAP2_ITEM_COUNT = 53;
    static final String MAP2_SOURCE_SHA256 =
            "7CD6144DBC1FA76B4D67373FE89037518DEFEE30DC4C38BEEA6A796A723527C9";
    static final String MAP2_RESOURCE_SHA256 =
            "6AC468A811187D7AE8A5B8B5D6BA0329182BE13CEBCC3E9D8EB7F7A9847400A3";
    static final int MAP14_DROP_COUNT = 15;
    static final int MAP14_ITEM_COUNT = 14;
    static final String MAP14_SOURCE_SHA256 =
            "F839A84690B8FE7E5229D1452238C334F6323ACDEA1417B8B64E50E99056F08C";
    static final String MAP14_RESOURCE_SHA256 =
            "FA2DCA33EB3BBA3759D0C1BCB987D6FF584273ACB3AF0EFAC2B8EA265F65776F";
    static final int MAP13_DROP_COUNT = 43;
    static final int MAP13_ITEM_COUNT = 38;
    static final String MAP13_SOURCE_SHA256 =
            "A8231E42679AA4B56909B9C096C40465E56553AAACE7360445931922ED267CEE";
    static final String MAP13_RESOURCE_SHA256 =
            "04216F993A209560034E18DCFE2E2FD90721511DAC76FE55D834473AF5F14584";
    static final int MAP237_DROP_COUNT = 0;
    static final int MAP237_ITEM_COUNT = 0;
    static final String MAP237_SOURCE_SHA256 =
            "3FE93EDCC0A0A171C6BB8ADF328460441832AF757DA26B9DAB1E8C8A2963C828";
    static final String MAP237_RESOURCE_SHA256 =
            "313AC2C05DA4146334D5E6147249EAAC5A041736A652F27C3EC41F74A80B2D31";
    static final int MAP3_DROP_COUNT = 0;
    static final int MAP3_ITEM_COUNT = 0;
    static final String MAP3_SOURCE_SHA256 =
            "3FE93EDCC0A0A171C6BB8ADF328460441832AF757DA26B9DAB1E8C8A2963C828";
    static final String MAP3_RESOURCE_SHA256 =
            "DC6D34F0F4838235E3666CBDC8E46A179E217282D618CA99E56BC1C580021343";
    static final int MAP5_DROP_COUNT = 0;
    static final int MAP5_ITEM_COUNT = 0;
    static final int MAP6_DROP_COUNT = 0;
    static final int MAP6_ITEM_COUNT = 0;
    static final String MAP5_SOURCE_SHA256 =
            "3FE93EDCC0A0A171C6BB8ADF328460441832AF757DA26B9DAB1E8C8A2963C828";
    static final String MAP6_SOURCE_SHA256 = MAP5_SOURCE_SHA256;
    static final String MAP5_RESOURCE_SHA256 =
            "6860A933C8B9005E75E1D592B526ECFCDD8E7B10ADE3FBD1A114E6235C5CA464";
    static final String MAP6_RESOURCE_SHA256 =
            "852B7528E75202FECBA1F4D6D2215C6898C7368F5FAEFA09E9C26C9C2815B742";
    static final int MAP70_DROP_COUNT = 475;
    static final int MAP70_ITEM_COUNT = 45;
    static final int MAP83_DROP_COUNT = 0;
    static final int MAP83_ITEM_COUNT = 0;
    static final int MAP84_DROP_COUNT = 0;
    static final int MAP84_ITEM_COUNT = 0;
    static final String MAP70_SOURCE_SHA256 =
            "32A2452C8DA91EFF60A5DEFAB3B8B0D6585886BD7FBFD92EC32F1AB1FF78C8E4";
    static final String MAP83_SOURCE_SHA256 = MAP5_SOURCE_SHA256;
    static final String MAP84_SOURCE_SHA256 = MAP5_SOURCE_SHA256;
    static final String MAP70_RESOURCE_SHA256 =
            "7EC38DFD0A7BAA1E779E4B47E46D4F9B3A177CBD4EC49E021A0D29540A7DC722";
    static final String MAP83_RESOURCE_SHA256 =
            "844AC6BEFB77EC037F0F7F5CDA8B982771DC22BB6DCB71E9969EA2DA5CA3FE76";
    static final String MAP84_RESOURCE_SHA256 =
            "F7AB75170C3342E08C42D548AD56F2BC0B249D3437ABF51D59178498FC96AB89";
    static final int MAP4_DROP_COUNT = 721;
    static final int MAP4_ITEM_COUNT = 234;
    static final String MAP4_SOURCE_SHA256 =
            "7F7F5B58CE493EE43C25074AA5FAB7D4495A2C9752EDDE680618F21C19A3E43B";
    static final String MAP4_RESOURCE_SHA256 =
            "F3D827FDC78CCE3EE5298B21366186AD5ECA484D431476A5FFC76EF96083694D";
    static final int MAP16_DROP_COUNT = 0;
    static final int MAP16_ITEM_COUNT = 0;
    static final String MAP16_SOURCE_SHA256 =
            "3FE93EDCC0A0A171C6BB8ADF328460441832AF757DA26B9DAB1E8C8A2963C828";
    static final String MAP16_RESOURCE_SHA256 =
            "6AE56FAB8A7A4091A577D7AE155AA8F6F8A91D8CDA147A15444E6C3AC632396A";
    static final int MAP17_DROP_COUNT = 0;
    static final int MAP17_ITEM_COUNT = 0;
    static final String MAP17_SOURCE_SHA256 =
            "3FE93EDCC0A0A171C6BB8ADF328460441832AF757DA26B9DAB1E8C8A2963C828";
    static final String MAP17_RESOURCE_SHA256 =
            "5B69A055C2101A5287F6E3C140F77D843671FF913CB46B5CC64B6997EF364D7F";
    static final int MAP22_DROP_COUNT = 0;
    static final int MAP22_ITEM_COUNT = 0;
    static final String MAP22_SOURCE_SHA256 = MAP17_SOURCE_SHA256;
    static final String MAP22_RESOURCE_SHA256 =
            "933013A91B2DD4A5C91D6BCF5B8C0638B14BA03902230BFCD6A1196BFCBD19A5";
    static final int MAP19_DROP_COUNT = 2;
    static final int MAP19_ITEM_COUNT = 2;
    static final String MAP19_SOURCE_SHA256 =
            "4BBF3AC41C72C01D99EEA29A8DFED736AB224B6BF1E2929DEA935A847A8E0C56";
    static final String MAP19_RESOURCE_SHA256 =
            "314A52DA1665DA6242D86B933EBD34845745E971C50F19D82A065107326A4EEF";
    static final int MAP20_DROP_COUNT = 18;
    static final int MAP20_ITEM_COUNT = 14;
    static final String MAP20_SOURCE_SHA256 =
            "620F5C1E53ED24A9D3531A081AE486A7B1DA5158084E63742BEC9ED3504AC1CC";
    static final String MAP20_RESOURCE_SHA256 =
            "D1FDFE10CCE1F8C9CC376FFADFBD7DB5CC3F59EE0C85F1C42DDE46067FC01B1A";
    static final int MAP21_DROP_COUNT = 17;
    static final int MAP21_ITEM_COUNT = 11;
    static final String MAP21_SOURCE_SHA256 =
            "277D421099A0C7D3DBDF3D4E1FEF7259C6686AD63F7D8E33362A8FC1233277A9";
    static final String MAP21_RESOURCE_SHA256 =
            "1B859E592CF7F62933223A0FDDA45044D299F13E762D7DF6B0905B7ACE04BE45";
    static final int MAP30_DROP_COUNT = 44;
    static final int MAP30_ITEM_COUNT = 31;
    static final String MAP30_SOURCE_SHA256 =
            "0818148BEC638A789B25B25586293F900F2B0CDC90BCAA82D816B1205B652F22";
    static final String MAP30_RESOURCE_SHA256 =
            "3D5F624285C3790C77A46BF2EC2C596B78BA6DD4BB43D5E8227390FD5905F682";
    static final int MAP31_DROP_COUNT = 47;
    static final int MAP31_ITEM_COUNT = 35;
    static final String MAP31_SOURCE_SHA256 =
            "681B119C9E0F374EC66D7AEC3D999A103BCFD0C838E2B2054043E1D6B9A3D106";
    static final String MAP31_RESOURCE_SHA256 =
            "7EB4686A1570C9E727B76EB5C73444AC46E95C1574DC9FE4EFC1222E4CB69739";
    static final int MAP32_DROP_COUNT = 59;
    static final int MAP32_ITEM_COUNT = 44;
    static final String MAP32_SOURCE_SHA256 =
            "9A17EB377AC335ABDDFE28370D41EDECB88BE50F7645AEE36E1CA6D694F5A05C";
    static final String MAP32_RESOURCE_SHA256 =
            "AAAF681AA7D57D2C772A782B417FF05366A1BB3986C74C910B13FB8985CF0D2E";
    static final int MAP33_DROP_COUNT = 76;
    static final int MAP33_ITEM_COUNT = 58;
    static final String MAP33_SOURCE_SHA256 =
            "071089ACC99CAB540767E43B4B669B07E6680AADD025F3EB4D8F7E9A618F546B";
    static final String MAP33_RESOURCE_SHA256 =
            "56FCD140AACDBB959EC934580520C9B4AEA154029F60E966E20F0CC8B688F2CF";
    static final int MAP35_DROP_COUNT = 58;
    static final int MAP35_ITEM_COUNT = 46;
    static final String MAP35_SOURCE_SHA256 =
            "B5801C9B607B369D5680695A23CD44E7330AF421C5624B882507DC0A73293361";
    static final String MAP35_RESOURCE_SHA256 =
            "A46D3A5EE2C814223BD3B41CF551CA6598F925CA3F40D050148874A9EB01C5C5";
    static final int MAP36_DROP_COUNT = 46;
    static final int MAP36_ITEM_COUNT = 36;
    static final String MAP36_SOURCE_SHA256 =
            "70E43318B6AECB6AD16E30D81532EFE032E62A8E16ADFD8417315B35F80849FF";
    static final String MAP36_RESOURCE_SHA256 =
            "BEF81518817B5F101BBA1179F9D15800729B402C736E329352648335E18FE3A4";
    static final int MAP37_DROP_COUNT = 0;
    static final int MAP37_ITEM_COUNT = 0;
    static final String MAP37_SOURCE_SHA256 =
            "3FE93EDCC0A0A171C6BB8ADF328460441832AF757DA26B9DAB1E8C8A2963C828";
    static final String MAP37_RESOURCE_SHA256 =
            "5CDF2009B3D0342AE1ACA74AB1F6EC674717DD19A2E64E14C5D5F60CFE78A6FE";
    static final int MAP67_DROP_COUNT = 0;
    static final int MAP67_ITEM_COUNT = 0;
    static final String MAP67_SOURCE_SHA256 = MAP16_SOURCE_SHA256;
    static final String MAP67_RESOURCE_SHA256 =
            "399A1450249A315C9B22F3ABDEE187200542820C84D87AB78700D87705ABD9F3";
    static final int MAP209_DROP_COUNT = 0;
    static final int MAP209_ITEM_COUNT = 0;
    static final String MAP209_SOURCE_SHA256 =
            "3FE93EDCC0A0A171C6BB8ADF328460441832AF757DA26B9DAB1E8C8A2963C828";
    static final String MAP209_RESOURCE_SHA256 =
            "E82B08586A011C8D688E80C66CC3DBE25AE687732B43E08834C08F573AE7DD4E";
    static final int MAP213_DROP_COUNT = 0;
    static final int MAP213_ITEM_COUNT = 0;
    static final String MAP213_SOURCE_SHA256 = MAP209_SOURCE_SHA256;
    static final String MAP213_RESOURCE_SHA256 =
            "E7E9AD3F03F6004BAE2E8BFD99CF7EFD53267DE9053D08BBFC47E476ECDBFFE9";
    static final int MAP302_DROP_COUNT = 0;
    static final int MAP302_ITEM_COUNT = 0;
    static final String MAP302_SOURCE_SHA256 = MAP209_SOURCE_SHA256;
    static final String MAP302_RESOURCE_SHA256 =
            "9EB512F2E603EDE1F8B879C0E5569F571215EFC0A99522E351AE3983F8F55580";
    static final int MAP445_DROP_COUNT = 0;
    static final int MAP445_ITEM_COUNT = 0;
    static final String MAP445_SOURCE_SHA256 = MAP209_SOURCE_SHA256;
    static final String MAP445_RESOURCE_SHA256 =
            "F15D3CF8849FBC13AF1FE964D22812013E8031E644F007525D2492D42707231C";
    static final int MAP447_DROP_COUNT = 0;
    static final int MAP447_ITEM_COUNT = 0;
    static final String MAP447_SOURCE_SHA256 = MAP209_SOURCE_SHA256;
    static final String MAP447_RESOURCE_SHA256 =
            "52E8A17C2552C8A371728749AAA082AE7F33EB8BE8CBF6AB81F2A0D640582982";
    static final int MAP303_DROP_COUNT = 220;
    static final int MAP303_ITEM_COUNT = 29;
    static final String MAP303_SOURCE_SHA256 =
            "50599409894942FF78B8658D2F61F764633FE48D5964924B0B39237C945B909F";
    static final String MAP303_RESOURCE_SHA256 =
            "00F6336A2E2DF8E446F2C127E983276A46CED6EA5B7D3A8A04034B843200F903";
    static final int MAP244_DROP_COUNT = 64;
    static final int MAP244_ITEM_COUNT = 33;
    static final String MAP244_SOURCE_SHA256 =
            "2C13EBC0F79B15E2B03A03909801449C8EA3FBF2AE0BE908BA822952B565CC30";
    static final String MAP244_RESOURCE_SHA256 =
            "4F1FFE596B2F11AC002906753AADC676FF753A736E58C0992170328A8F1D32C5";
    private record SourceContract(
            int drops, int items, String sourceSha256, String resourceSha256) {}
    private static final Map<Integer, SourceContract> ANT_CAVE_SOURCE_CONTRACTS =
            Map.ofEntries(
                    Map.entry(43, new SourceContract(0, 0, MAP22_SOURCE_SHA256,
                            "9E5616C18F56D105A9D82A54B4CF355867717C773D9D80A23EC06F210B554E2F")),
                    Map.entry(44, new SourceContract(0, 0, MAP22_SOURCE_SHA256,
                            "7C2A3FD2988320F9E5263AC56DCF42FF5FAB01C30875FE420AD9D7BF15252F85")),
                    Map.entry(45, new SourceContract(0, 0, MAP22_SOURCE_SHA256,
                            "0AE938D8B26849764CF27992C56A1F9AEC001123824AD0181FB903A2EA8F5288")),
                    Map.entry(46, new SourceContract(0, 0, MAP22_SOURCE_SHA256,
                            "E5B409904697D8EE20BEC3DBEE5441F2C369A9C9DB0338CCE6EECB0AE4F22F43")),
                    Map.entry(47, new SourceContract(0, 0, MAP22_SOURCE_SHA256,
                            "FC69FFCC2CEFCD7DA6F8DB1B5086E4BA88ECA7C3C8386F82C19C1D7E077234F2")),
                    Map.entry(48, new SourceContract(0, 0, MAP22_SOURCE_SHA256,
                            "AF23357BA880A368898966CEEF4AD90DFCF36D5ABD00A424A9DFBE8A70A8AC7F")),
                    Map.entry(49, new SourceContract(51, 17,
                            "44938B3AF00783C77EE2532ECD89EA584078B55753F2307503BC676A3702C5C9",
                            "5D7717B35435FF4A40C61ABBC6A58F69D42B3B980BA7FB39F5F7D54531D29975")),
                    Map.entry(50, new SourceContract(51, 17,
                            "44938B3AF00783C77EE2532ECD89EA584078B55753F2307503BC676A3702C5C9",
                            "912DB5B89FE529F31259CD6BF5F53FF5517DF3BB316F5BB60AAEA27FE89CD548")),
                    Map.entry(51, new SourceContract(36, 16,
                            "1EBB2A1DD5B1DB0EEC699E6266B93809B76E0C1179645C82DAAFFC4D5DCC3AC1",
                            "547F589C350117D648939AB1543F8F8DEC3FCDF75611CEA72EDBF10A03B192CB")),
                    Map.entry(541, new SourceContract(7, 7,
                            "029B75D773F7F2EC3C33D718A14691CEBA0F1DC18A673E2AE75056CEDFAD7214",
                            "7974D05D760A895B68D3A897F2FD1632C04DCD7047538E5AE94850F34324195F")),
                    Map.entry(542, new SourceContract(7, 7,
                            "029B75D773F7F2EC3C33D718A14691CEBA0F1DC18A673E2AE75056CEDFAD7214",
                            "DCCD70B57E649821EE4A68C43D3C39CC0AC5F605890E13E788F1A63218600E17")),
                    Map.entry(543, new SourceContract(7, 7,
                            "029B75D773F7F2EC3C33D718A14691CEBA0F1DC18A673E2AE75056CEDFAD7214",
                            "DFEC73272D2FCA471548C221C9D8734F09DEB81027707A27C5829B9710ED5F79")));
    private static final Map<Integer, SourceContract> KNIGHT_CAVE_SOURCE_CONTRACTS =
            Map.of(
                    25, new SourceContract(84, 58,
                            "7EE5F266FB28579ED976913246FAB7450E1823002A28ADDED047E8B6C3DCA23A",
                            "00973EF9FAC32E70AEE2B3C37267ABF21060571EBE007BBA62E891F6E10C22E2"),
                    26, new SourceContract(46, 40,
                            "E647187DFE51D5075866F07730022E23969561465063F3CBBAD188D3CEA4B590",
                            "168D98F8D24A25CFA0535B2EB380A83431136E095967453AD16A85CD1B5A4606"),
                    27, new SourceContract(47, 32,
                            "9468DB70124E1361B52B4C19533A259ECEB4F56BD337B7B9A36D2E10B6FC0724",
                            "D0246B93BFC927C3E9AF763D0CAE3FEC13112FCBEE0D0512ADE616C23D60736D"),
                    28, new SourceContract(47, 32,
                            "9468DB70124E1361B52B4C19533A259ECEB4F56BD337B7B9A36D2E10B6FC0724",
                            "512D1988C0FCC5F36522B44AB0DE633A7F93386EC015BA5BBC999A4560E3F8CC"));
    static final Map<Integer, SourceContract> GLUDIO_DUNGEON_SOURCE_CONTRACTS =
            Map.of(
                    7, new SourceContract(53, 36,
                            "CB26409546451C37F1D0913A72834197E31B2B08F2EB815E98262EC9EC298F95",
                            "B606024E6C6347CD29A8D504D98F3ED9E01043FEA7DF9C684BAEA88402AB448D"),
                    8, new SourceContract(61, 45,
                            "41DA8D92A565E0256CDB80CA8C4AE16B460F93C8B5C5D71175E6BC5B070D475B",
                            "E794594EF080F29BAE107666FF62E33345951615E18A9041A9A5ECB3FFE69048"),
                    9, new SourceContract(42, 36,
                            "82BD95A5925F835712B4BCA2FD6E8CBF414A738E2E26982F657937A18AEF83B0",
                            "636431B3136286162535BF6674768506E68523396BF1C615C4FB4C503999EDF3"),
                    10, new SourceContract(66, 53,
                            "11BA057F1DE3E6C44B40B478198A45B1CA7ED81DEA0C43E5102AFBBD32BA6389",
                            "5FE4EA34761AE1EB93C86941AC9FB77A6A482D453DDCBF3F3C95BEE8AB1AA00E"),
                    11, new SourceContract(61, 40,
                            "C3EE69F71D2CFC5700838DA655F1F98EF89D8E0C3D575C159591CF25D7E13B3E",
                            "3E17F4D19AF046BFEF1C6B2C5D6D059D512C542E4496E261E26E88CCA6BFAE85"),
                    12, new SourceContract(90, 66,
                            "DED69D8BE839E62269C04B6EF6CFF5F40D8D301247F5C3FC18754229853A501D",
                            "26E18025A2D51761A7A08EB701E27AC64FF05F675B2A12D872ED87BC38A706EC"));
    static final Map<Integer, SourceContract> GIRAN_DUNGEON_SOURCE_CONTRACTS =
            Map.of(
                    53, new SourceContract(1, 1,
                            "87E27DB67E926A4F125EAD89649610DE2A68C76B3AEFF5E508A762C53D954752",
                            "5F0B24904E3079293B891E51F61187F6F0ABAD17C7A55A929423D1C19F5F3794"),
                    54, new SourceContract(0, 0,
                            "3FE93EDCC0A0A171C6BB8ADF328460441832AF757DA26B9DAB1E8C8A2963C828",
                            "D692779E8D5E143D537E617891016575101EEC24CCA280FC9BDE401D987E3554"),
                    55, new SourceContract(0, 0,
                            "3FE93EDCC0A0A171C6BB8ADF328460441832AF757DA26B9DAB1E8C8A2963C828",
                            "1A896B6DF4F26B7615139019DE292F7E6D875E81CF29CDD0BF1D60CAD706858D"),
                    56, new SourceContract(0, 0,
                            "3FE93EDCC0A0A171C6BB8ADF328460441832AF757DA26B9DAB1E8C8A2963C828",
                            "30176949B45433F84C1F2202768ED75EA860CC393291EECC2937AD3B73167965"));
    static final SourceContract MAP52_SOURCE_CONTRACT = new SourceContract(
            0, 0,
            "3FE93EDCC0A0A171C6BB8ADF328460441832AF757DA26B9DAB1E8C8A2963C828",
            "7612D907ACB2BBE439F8E8CE72CD42D5F37CDB4A190639D44D78A76B8CE4AE84");
    static final Map<Integer, SourceContract> WINDAWOOD_CASTLE_SOURCE_CONTRACTS =
            Map.of(
                    23, new SourceContract(93, 59,
                            "8E2333ECE326813EC1B6DEA81297D3B656B181053DB31A855A2A2FFD842A72B4",
                            "905457DE62F89EDFF6BE5A7A9986DE2184AD934466B13DE2BD160B1B1BA5612B"),
                    24, new SourceContract(71, 53,
                            "1E1FE6C073F5BDC3B22DFDB51F96EE2CB4C06F9A31C6E0339583F2EDA244F25C",
                            "1C98BB0783E3B5303E851EB0EF27B62269D1418DA2CE4AFB952483C08E53D05A"),
                    29, new SourceContract(0, 0,
                            "3FE93EDCC0A0A171C6BB8ADF328460441832AF757DA26B9DAB1E8C8A2963C828",
                            "41CED2FD9273C0B6611684C8B35D083FD4154ADA583C19F34F709921BE40D498"));
    static final Map<Integer, SourceContract> KENT_COMPONENT_SOURCE_CONTRACTS =
            Map.of(
                    15, new SourceContract(0, 0,
                            "3FE93EDCC0A0A171C6BB8ADF328460441832AF757DA26B9DAB1E8C8A2963C828",
                            "339EE654FC297C15B5FD00B62090B10D640251AAD5030ADEB865E7C897E8292C"),
                    240, new SourceContract(0, 0,
                            "3FE93EDCC0A0A171C6BB8ADF328460441832AF757DA26B9DAB1E8C8A2963C828",
                            "352BF54C5D7A6A1DC92E16E06FF61E615C3FA61717571D646DFFBF1196DB728F"),
                    241, new SourceContract(0, 0,
                            "3FE93EDCC0A0A171C6BB8ADF328460441832AF757DA26B9DAB1E8C8A2963C828",
                            "D7CD5A44FC91311F4EC9DDB0F8FCF2BC4EFB3689214C71560FAA6A93D525DC7B"),
                    242, new SourceContract(0, 0,
                            "3FE93EDCC0A0A171C6BB8ADF328460441832AF757DA26B9DAB1E8C8A2963C828",
                            "E6844976CC12665DE8D3C38AB6CEDB9BAC3F778EAD5CF36CEB59C2D365153AD9"),
                    243, new SourceContract(0, 0,
                            "3FE93EDCC0A0A171C6BB8ADF328460441832AF757DA26B9DAB1E8C8A2963C828",
                            "171F5E526BD203515F9D08547BCC9F9A5B91E20639F5AEDDA0EAB8990E9EE599"));
    static final Map<Integer, ClassicWorldIvoryTowerCatalog.SourceContract>
            IVORY_TOWER_SOURCE_CONTRACTS = ClassicWorldIvoryTowerCatalog.DROP_CONTRACTS;
    static final SourceContract MAP66_SOURCE_CONTRACT = new SourceContract(
            0, 0,
            ClassicWorldDwarfCastleCatalog.DROP_SOURCE_SHA256,
            ClassicWorldDwarfCastleCatalog.DROP_RESOURCE_SHA256);
    static final int CHANCE_SCALE = 1_000_000;
    static final int MAXIMUM_QUANTITY = 2_000_000_000;

    private static final int MAGIC = 0x4C574431; // LWD1
    private static final String DROP_HEADER =
            "mob_id\titem_id\tmin\tmax\tchance";
    private static final String ITEM_HEADER = String.join("\t",
            "item_id", "kind", "name_id", "use_token", "inventory_gfx",
            "ground_gfx", "blessing", "stackable", "tradable", "unit_weight",
            "item_description_id", "weapon_type", "damage_small", "damage_large",
            "material", "safe_enchant", "class_mask");
    private static final Catalog MAP4 = load("/world/map4-drops.lwd.gz");
    private static final Catalog MAP0 = load("/world/map0-drops.lwd.gz");
    private static final Catalog MAP1 = load("/world/map1-drops.lwd.gz");
    private static final Catalog MAP2 = load("/world/map2-drops.lwd.gz");
    private static final Catalog MAP14 = load("/world/map14-drops.lwd.gz");
    private static final Catalog MAP13 = load("/world/map13-drops.lwd.gz");
    private static final Catalog MAP237 = load("/world/map237-drops.lwd.gz");
    private static final Catalog MAP3 = load("/world/map3-drops.lwd.gz");
    private static final Catalog MAP5 = load("/world/map5-drops.lwd.gz");
    private static final Catalog MAP6 = load("/world/map6-drops.lwd.gz");
    private static final Catalog MAP70 = load("/world/map70-drops.lwd.gz");
    private static final Catalog MAP83 = load("/world/map83-drops.lwd.gz");
    private static final Catalog MAP84 = load("/world/map84-drops.lwd.gz");
    private static final Catalog MAP16 = load("/world/map16-drops.lwd.gz");
    private static final Catalog MAP17 = load("/world/map17-drops.lwd.gz");
    private static final Catalog MAP22 = load("/world/map22-drops.lwd.gz");
    private static final Catalog MAP25 = load("/world/map25-drops.lwd.gz");
    private static final Catalog MAP26 = load("/world/map26-drops.lwd.gz");
    private static final Catalog MAP27 = load("/world/map27-drops.lwd.gz");
    private static final Catalog MAP28 = load("/world/map28-drops.lwd.gz");
    private static final Catalog MAP43 = load("/world/map43-drops.lwd.gz");
    private static final Catalog MAP44 = load("/world/map44-drops.lwd.gz");
    private static final Catalog MAP45 = load("/world/map45-drops.lwd.gz");
    private static final Catalog MAP46 = load("/world/map46-drops.lwd.gz");
    private static final Catalog MAP47 = load("/world/map47-drops.lwd.gz");
    private static final Catalog MAP48 = load("/world/map48-drops.lwd.gz");
    private static final Catalog MAP49 = load("/world/map49-drops.lwd.gz");
    private static final Catalog MAP50 = load("/world/map50-drops.lwd.gz");
    private static final Catalog MAP51 = load("/world/map51-drops.lwd.gz");
    private static final Catalog MAP541 = load("/world/map541-drops.lwd.gz");
    private static final Catalog MAP542 = load("/world/map542-drops.lwd.gz");
    private static final Catalog MAP543 = load("/world/map543-drops.lwd.gz");
    private static final Catalog MAP19 = load("/world/map19-drops.lwd.gz");
    private static final Catalog MAP20 = load("/world/map20-drops.lwd.gz");
    private static final Catalog MAP21 = load("/world/map21-drops.lwd.gz");
    private static final Catalog MAP30 = load("/world/map30-drops.lwd.gz");
    private static final Catalog MAP31 = load("/world/map31-drops.lwd.gz");
    private static final Catalog MAP32 = load("/world/map32-drops.lwd.gz");
    private static final Catalog MAP33 = load("/world/map33-drops.lwd.gz");
    private static final Catalog MAP35 = load("/world/map35-drops.lwd.gz");
    private static final Catalog MAP36 = load("/world/map36-drops.lwd.gz");
    private static final Catalog MAP37 = load("/world/map37-drops.lwd.gz");
    private static final Catalog MAP67 = load("/world/map67-drops.lwd.gz");
    private static final Catalog MAP209 = load("/world/map209-drops.lwd.gz");
    private static final Catalog MAP213 = load("/world/map213-drops.lwd.gz");
    private static final Catalog MAP302 = load("/world/map302-drops.lwd.gz");
    private static final Catalog MAP445 = load("/world/map445-drops.lwd.gz");
    private static final Catalog MAP447 = load("/world/map447-drops.lwd.gz");
    private static final Catalog MAP303 = load("/world/map303-drops.lwd.gz");
    private static final Catalog MAP244 = load("/world/map244-drops.lwd.gz");
    private static final Catalog MAP7 = load("/world/map7-drops.lwd.gz");
    private static final Catalog MAP8 = load("/world/map8-drops.lwd.gz");
    private static final Catalog MAP9 = load("/world/map9-drops.lwd.gz");
    private static final Catalog MAP10 = load("/world/map10-drops.lwd.gz");
    private static final Catalog MAP11 = load("/world/map11-drops.lwd.gz");
    private static final Catalog MAP12 = load("/world/map12-drops.lwd.gz");
    private static final Catalog MAP53 = load("/world/map53-drops.lwd.gz");
    private static final Catalog MAP54 = load("/world/map54-drops.lwd.gz");
    private static final Catalog MAP55 = load("/world/map55-drops.lwd.gz");
    private static final Catalog MAP56 = load("/world/map56-drops.lwd.gz");
    private static final Catalog MAP52 = load("/world/map52-drops.lwd.gz");
    private static final Catalog MAP23 = load("/world/map23-drops.lwd.gz");
    private static final Catalog MAP24 = load("/world/map24-drops.lwd.gz");
    private static final Catalog MAP29 = load("/world/map29-drops.lwd.gz");
    private static final Catalog MAP15 = load("/world/map15-drops.lwd.gz");
    private static final Catalog MAP240 = load("/world/map240-drops.lwd.gz");
    private static final Catalog MAP241 = load("/world/map241-drops.lwd.gz");
    private static final Catalog MAP242 = load("/world/map242-drops.lwd.gz");
    private static final Catalog MAP243 = load("/world/map243-drops.lwd.gz");
    private static final Catalog MAP75 = load("/world/map75-drops.lwd.gz");
    private static final Catalog MAP76 = load("/world/map76-drops.lwd.gz");
    private static final Catalog MAP77 = load("/world/map77-drops.lwd.gz");
    private static final Catalog MAP78 = load("/world/map78-drops.lwd.gz");
    private static final Catalog MAP79 = load("/world/map79-drops.lwd.gz");
    private static final Catalog MAP80 = load("/world/map80-drops.lwd.gz");
    private static final Catalog MAP81 = load("/world/map81-drops.lwd.gz");
    private static final Catalog MAP82 = load("/world/map82-drops.lwd.gz");
    private static final Catalog MAP66 = load("/world/map66-drops.lwd.gz");
    private static final Catalog MAP101 = load("/world/map101-drops.lwd.gz");
    private static final Catalog MAP102 = load("/world/map102-drops.lwd.gz");
    private static final Catalog MAP103 = load("/world/map103-drops.lwd.gz");
    private static final Catalog MAP104 = load("/world/map104-drops.lwd.gz");
    private static final Catalog MAP105 = load("/world/map105-drops.lwd.gz");
    private static final Catalog MAP106 = load("/world/map106-drops.lwd.gz");
    private static final Catalog MAP107 = load("/world/map107-drops.lwd.gz");
    private static final Catalog MAP108 = load("/world/map108-drops.lwd.gz");
    private static final Catalog MAP109 = load("/world/map109-drops.lwd.gz");
    private static final Catalog MAP110 = load("/world/map110-drops.lwd.gz");
    private static final Catalog MAP300 = load("/world/map300-drops.lwd.gz");
    private static final Catalog MAP301 = load("/world/map301-drops.lwd.gz");
    private static final Catalog MAP59 = load("/world/map59-drops.lwd.gz");
    private static final Catalog MAP60 = load("/world/map60-drops.lwd.gz");
    private static final Catalog MAP61 = load("/world/map61-drops.lwd.gz");
    private static final Catalog MAP62 = load("/world/map62-drops.lwd.gz");
    private static final Catalog MAP63 = load("/world/map63-drops.lwd.gz");
    private static final Catalog MAP64 = load("/world/map64-drops.lwd.gz");
    private static final Catalog MAP65 = load("/world/map65-drops.lwd.gz");
    private static final Catalog MAP252 = load("/world/map252-drops.lwd.gz");
    private static final Catalog MAP253 = load("/world/map253-drops.lwd.gz");
    private static final Catalog MAP254 = load("/world/map254-drops.lwd.gz");
    private static final Catalog MAP304 = load("/world/map304-drops.lwd.gz");
    private static final Catalog MAP305 = load("/world/map305-drops.lwd.gz");
    private static final Catalog MAP307 = load("/world/map307-drops.lwd.gz");
    private static final Catalog MAP308 = load("/world/map308-drops.lwd.gz");
    private static final Catalog MAP309 = load("/world/map309-drops.lwd.gz");
    private static final Catalog MAP310 = load("/world/map310-drops.lwd.gz");
    private static final Catalog MAP320 = load("/world/map320-drops.lwd.gz");
    private static final Catalog MAP330 = load("/world/map330-drops.lwd.gz");
    private static final Catalog MAP400 = load("/world/map400-drops.lwd.gz");
    private static final Catalog MAP401 = load("/world/map401-drops.lwd.gz");
    private static final Catalog MAP410 = load("/world/map410-drops.lwd.gz");
    private static final Catalog MAP420 = load("/world/map420-drops.lwd.gz");
    private static final Catalog MAP430 = load("/world/map430-drops.lwd.gz");
    private static final Catalog MAP440 = load("/world/map440-drops.lwd.gz");
    private static final Catalog MAP441 = load("/world/map441-drops.lwd.gz");
    private static final Catalog MAP442 = load("/world/map442-drops.lwd.gz");
    private static final Catalog MAP443 = load("/world/map443-drops.lwd.gz");
    private static final Catalog MAP444 = load("/world/map444-drops.lwd.gz");
    private static final Catalog MAP480 = load("/world/map480-drops.lwd.gz");
    private static final Catalog MAP481 = load("/world/map481-drops.lwd.gz");
    private static final Catalog MAP482 = load("/world/map482-drops.lwd.gz");
    private static final Catalog MAP483 = load("/world/map483-drops.lwd.gz");
    private static final Catalog MAP484 = load("/world/map484-drops.lwd.gz");
    private static final Catalog MAP550 = load("/world/map550-drops.lwd.gz");
    private static final Catalog MAP551 = load("/world/map551-drops.lwd.gz");
    private static final Catalog MAP552 = load("/world/map552-drops.lwd.gz");
    private static final Catalog MAP553 = load("/world/map553-drops.lwd.gz");
    private static final Catalog MAP554 = load("/world/map554-drops.lwd.gz");
    private static final Catalog MAP555 = load("/world/map555-drops.lwd.gz");
    private static final Catalog MAP556 = load("/world/map556-drops.lwd.gz");
    private static final Catalog MAP557 = load("/world/map557-drops.lwd.gz");
    private static final Catalog MAP558 = load("/world/map558-drops.lwd.gz");
    private static final Catalog MAP450 = load("/world/map450-drops.lwd.gz");
    private static final Catalog MAP600 = load("/world/map600-drops.lwd.gz");
    private static final Catalog MAP601 = load("/world/map601-drops.lwd.gz");
    private static final Catalog MAP602 = load("/world/map602-drops.lwd.gz");
    private static final Catalog MAP603 = load("/world/map603-drops.lwd.gz");
    private static final Catalog MAP604 = load("/world/map604-drops.lwd.gz");
    private static final Catalog MAP605 = load("/world/map605-drops.lwd.gz");
    private static final Catalog MAP606 = load("/world/map606-drops.lwd.gz");
    private static final Catalog MAP607 = load("/world/map607-drops.lwd.gz");
    private static final Catalog MAP778 = load("/world/map778-drops.lwd.gz");
    private static final Catalog MAP779 = load("/world/map779-drops.lwd.gz");
    private static final Catalog MAP1000 = load("/world/map1000-drops.lwd.gz");
    private static final Catalog MAP1001 = load("/world/map1001-drops.lwd.gz");
    private static final Map<Integer, Catalog> BY_MAP = Map.ofEntries(
            Map.entry(MAP0.mapId(), MAP0),
            Map.entry(MAP1.mapId(), MAP1),
            Map.entry(MAP2.mapId(), MAP2),
            Map.entry(MAP14.mapId(), MAP14),
            Map.entry(MAP13.mapId(), MAP13),
            Map.entry(MAP237.mapId(), MAP237),
            Map.entry(MAP3.mapId(), MAP3),
            Map.entry(MAP5.mapId(), MAP5),
            Map.entry(MAP6.mapId(), MAP6),
            Map.entry(MAP70.mapId(), MAP70),
            Map.entry(MAP83.mapId(), MAP83),
            Map.entry(MAP84.mapId(), MAP84),
            Map.entry(MAP4.mapId(), MAP4),
            Map.entry(MAP16.mapId(), MAP16),
            Map.entry(MAP17.mapId(), MAP17),
            Map.entry(MAP22.mapId(), MAP22),
            Map.entry(MAP25.mapId(), MAP25),
            Map.entry(MAP26.mapId(), MAP26),
            Map.entry(MAP27.mapId(), MAP27),
            Map.entry(MAP28.mapId(), MAP28),
            Map.entry(MAP43.mapId(), MAP43),
            Map.entry(MAP44.mapId(), MAP44),
            Map.entry(MAP45.mapId(), MAP45),
            Map.entry(MAP46.mapId(), MAP46),
            Map.entry(MAP47.mapId(), MAP47),
            Map.entry(MAP48.mapId(), MAP48),
            Map.entry(MAP49.mapId(), MAP49),
            Map.entry(MAP50.mapId(), MAP50),
            Map.entry(MAP51.mapId(), MAP51),
            Map.entry(MAP541.mapId(), MAP541),
            Map.entry(MAP542.mapId(), MAP542),
            Map.entry(MAP543.mapId(), MAP543),
            Map.entry(MAP19.mapId(), MAP19),
            Map.entry(MAP20.mapId(), MAP20),
            Map.entry(MAP21.mapId(), MAP21),
            Map.entry(MAP30.mapId(), MAP30),
            Map.entry(MAP31.mapId(), MAP31),
            Map.entry(MAP32.mapId(), MAP32),
            Map.entry(MAP33.mapId(), MAP33),
            Map.entry(MAP35.mapId(), MAP35),
            Map.entry(MAP36.mapId(), MAP36),
            Map.entry(MAP37.mapId(), MAP37),
            Map.entry(MAP67.mapId(), MAP67),
            Map.entry(MAP209.mapId(), MAP209),
            Map.entry(MAP213.mapId(), MAP213),
            Map.entry(MAP302.mapId(), MAP302),
            Map.entry(MAP445.mapId(), MAP445),
            Map.entry(MAP447.mapId(), MAP447),
            Map.entry(MAP303.mapId(), MAP303),
            Map.entry(MAP244.mapId(), MAP244),
            Map.entry(MAP7.mapId(), MAP7),
            Map.entry(MAP8.mapId(), MAP8),
            Map.entry(MAP9.mapId(), MAP9),
            Map.entry(MAP10.mapId(), MAP10),
            Map.entry(MAP11.mapId(), MAP11),
            Map.entry(MAP12.mapId(), MAP12),
            Map.entry(MAP53.mapId(), MAP53),
            Map.entry(MAP54.mapId(), MAP54),
            Map.entry(MAP55.mapId(), MAP55),
            Map.entry(MAP56.mapId(), MAP56),
            Map.entry(MAP52.mapId(), MAP52),
            Map.entry(MAP23.mapId(), MAP23),
            Map.entry(MAP24.mapId(), MAP24),
            Map.entry(MAP29.mapId(), MAP29),
            Map.entry(MAP15.mapId(), MAP15),
            Map.entry(MAP240.mapId(), MAP240),
            Map.entry(MAP241.mapId(), MAP241),
            Map.entry(MAP242.mapId(), MAP242),
            Map.entry(MAP243.mapId(), MAP243),
            Map.entry(MAP75.mapId(), MAP75),
            Map.entry(MAP76.mapId(), MAP76),
            Map.entry(MAP77.mapId(), MAP77),
            Map.entry(MAP78.mapId(), MAP78),
            Map.entry(MAP79.mapId(), MAP79),
            Map.entry(MAP80.mapId(), MAP80),
            Map.entry(MAP81.mapId(), MAP81),
            Map.entry(MAP82.mapId(), MAP82),
            Map.entry(MAP66.mapId(), MAP66),
            Map.entry(MAP101.mapId(), MAP101),
            Map.entry(MAP102.mapId(), MAP102),
            Map.entry(MAP103.mapId(), MAP103),
            Map.entry(MAP104.mapId(), MAP104),
            Map.entry(MAP105.mapId(), MAP105),
            Map.entry(MAP106.mapId(), MAP106),
            Map.entry(MAP107.mapId(), MAP107),
            Map.entry(MAP108.mapId(), MAP108),
            Map.entry(MAP109.mapId(), MAP109),
            Map.entry(MAP110.mapId(), MAP110),
            Map.entry(MAP300.mapId(), MAP300),
            Map.entry(MAP301.mapId(), MAP301),
            Map.entry(MAP59.mapId(), MAP59),
            Map.entry(MAP60.mapId(), MAP60),
            Map.entry(MAP61.mapId(), MAP61),
            Map.entry(MAP62.mapId(), MAP62),
            Map.entry(MAP63.mapId(), MAP63),
            Map.entry(MAP64.mapId(), MAP64),
            Map.entry(MAP65.mapId(), MAP65),
            Map.entry(MAP252.mapId(), MAP252),
            Map.entry(MAP253.mapId(), MAP253),
            Map.entry(MAP254.mapId(), MAP254),
            Map.entry(MAP304.mapId(), MAP304),
            Map.entry(MAP305.mapId(), MAP305),
            Map.entry(MAP307.mapId(), MAP307),
            Map.entry(MAP308.mapId(), MAP308),
            Map.entry(MAP309.mapId(), MAP309),
            Map.entry(MAP310.mapId(), MAP310),
            Map.entry(MAP320.mapId(), MAP320),
            Map.entry(MAP330.mapId(), MAP330),
            Map.entry(MAP400.mapId(), MAP400),
            Map.entry(MAP401.mapId(), MAP401),
            Map.entry(MAP410.mapId(), MAP410),
            Map.entry(MAP420.mapId(), MAP420),
            Map.entry(MAP430.mapId(), MAP430),
            Map.entry(MAP440.mapId(), MAP440),
            Map.entry(MAP441.mapId(), MAP441),
            Map.entry(MAP442.mapId(), MAP442),
            Map.entry(MAP443.mapId(), MAP443),
            Map.entry(MAP444.mapId(), MAP444),
            Map.entry(MAP480.mapId(), MAP480),
            Map.entry(MAP481.mapId(), MAP481),
            Map.entry(MAP482.mapId(), MAP482),
            Map.entry(MAP483.mapId(), MAP483),
            Map.entry(MAP484.mapId(), MAP484),
            Map.entry(MAP550.mapId(), MAP550),
            Map.entry(MAP551.mapId(), MAP551),
            Map.entry(MAP552.mapId(), MAP552),
            Map.entry(MAP553.mapId(), MAP553),
            Map.entry(MAP554.mapId(), MAP554),
            Map.entry(MAP555.mapId(), MAP555),
            Map.entry(MAP556.mapId(), MAP556),
            Map.entry(MAP557.mapId(), MAP557),
            Map.entry(MAP558.mapId(), MAP558),
            Map.entry(MAP450.mapId(), MAP450),
            Map.entry(MAP600.mapId(), MAP600),
            Map.entry(MAP601.mapId(), MAP601),
            Map.entry(MAP602.mapId(), MAP602),
            Map.entry(MAP603.mapId(), MAP603),
            Map.entry(MAP604.mapId(), MAP604),
            Map.entry(MAP605.mapId(), MAP605),
            Map.entry(MAP606.mapId(), MAP606),
            Map.entry(MAP607.mapId(), MAP607),
            Map.entry(MAP778.mapId(), MAP778),
            Map.entry(MAP779.mapId(), MAP779),
            Map.entry(MAP1000.mapId(), MAP1000),
            Map.entry(MAP1001.mapId(), MAP1001));

    record DropRule(int mobId, int itemId, int minimum, int maximum, int chance) {
        DropRule {
            if (mobId <= 0 || itemId <= 0 || minimum <= 0 || maximum < minimum
                    || chance < 0 || chance > CHANCE_SCALE) {
                throw new IllegalArgumentException("invalid classic WORLD drop rule");
            }
        }
    }

    record ItemWire(int itemId, String kind, String nameId, String useToken,
                    int inventoryGfx, int groundGfx, int blessing,
                    boolean stackable, boolean tradable, int unitWeight,
                    int itemDescriptionId, String weaponType,
                    int damageSmall, int damageLarge, String material,
                    int safeEnchant, int classMask, boolean cantDelete) {
        ItemWire(int itemId, String kind, String nameId, String useToken,
                 int inventoryGfx, int groundGfx, int blessing,
                 boolean stackable, boolean tradable, int unitWeight,
                 int itemDescriptionId, String weaponType,
                 int damageSmall, int damageLarge, String material,
                 int safeEnchant, int classMask) {
            this(itemId, kind, nameId, useToken, inventoryGfx, groundGfx, blessing,
                    stackable, tradable, unitWeight, itemDescriptionId, weaponType,
                    damageSmall, damageLarge, material, safeEnchant, classMask, false);
        }

        ItemWire {
            if (itemId <= 0 || kind == null || nameId == null || nameId.isEmpty()
                    || useToken == null || useToken.isEmpty()
                    || inventoryGfx < 0 || inventoryGfx > 0xFFFF
                    || groundGfx <= 0 || groundGfx > 0xFFFF
                    || blessing < 0 || blessing > RuntimeInventory.BLESSING_UNIDENTIFIED
                    || unitWeight < 0 || itemDescriptionId < 0
                    || itemDescriptionId > 0xFFFF || weaponType == null
                    || damageSmall < 0 || damageSmall > 0xFF
                    || damageLarge < 0 || damageLarge > 0xFF
                    || material == null || safeEnchant < -1 || safeEnchant > 0x7F
                    || classMask < 0 || classMask > 0x7F) {
                throw new IllegalArgumentException("invalid classic WORLD item wire row");
            }
            if (!kind.equals("weapon") && !kind.equals("armor") && !kind.equals("etc")) {
                throw new IllegalArgumentException("invalid classic WORLD item kind");
            }
        }

        InventoryCatalog.Definition inventoryDefinition() {
            return inventoryDefinition(null);
        }

        InventoryCatalog.Definition inventoryDefinition(
                InventoryCatalog.NativeArmorStatus sourceArmorStatus) {
            InventoryCatalog.Definition semantic = InventoryCatalog.builtIn(itemId);
            return new InventoryCatalog.Definition(
                     itemId, nativeUseType(kind, useToken), inventoryGfx, groundGfx,
                     blessing, false, nameId, nameId, 0, 0, stackable, tradable,
                    weaponWalkAction(kind, weaponType),
                    semantic == null ? 0 : semantic.lightRange(),
                    semantic == null ? InventoryCatalog.Effect.NONE : semantic.effect(),
                    itemDescriptionId, unitWeight,
                    semantic == null ? 0 : semantic.fireResistance(),
                    nativeWeaponStatus(), sourceArmorStatus, cantDelete);
        }

        InventoryCatalog.NativeWeaponStatus nativeWeaponStatus() {
            if (!kind.equals("weapon")) return null;
            return new InventoryCatalog.NativeWeaponStatus(
                    damageSmall, damageLarge, materialCode(material), unitWeight,
                    classMask, safeEnchant);
        }
    }

    record RolledItem(ItemWire item, int quantity, int chanceRoll) {
        RolledItem {
            if (item == null || quantity <= 0 || quantity > MAXIMUM_QUANTITY
                    || chanceRoll <= 0 || chanceRoll > CHANCE_SCALE) {
                throw new IllegalArgumentException("invalid classic WORLD rolled item");
            }
        }
    }

    private record Catalog(int mapId, Map<Integer, List<DropRule>> dropsByMob,
                           Map<Integer, ItemWire> items) {}

    private ClassicWorldDropCatalog() {}

    static List<DropRule> rules(int mapId, int npcId) {
        return catalog(mapId).dropsByMob().getOrDefault(npcId, List.of());
    }

    static ItemWire item(int mapId, int itemId) {
        return catalog(mapId).items().get(itemId);
    }

    static List<ItemWire> items(int mapId) {
        return List.copyOf(catalog(mapId).items().values());
    }

    static int dropCount(int mapId) {
        return catalog(mapId).dropsByMob().values().stream()
                .mapToInt(List::size).sum();
    }

    static InventoryCatalog.Definition inventoryDefinition(int itemId) {
        ItemWire wire = itemAcrossMaps(itemId);
        return wire == null ? null : wire.inventoryDefinition();
    }

    static InventoryCatalog.NativeWeaponStatus nativeWeaponStatus(int itemId) {
        ItemWire wire = itemAcrossMaps(itemId);
        return wire == null ? null : wire.nativeWeaponStatus();
    }

    /** Retained SetDrop row roll with map/rate defaults supplied by the caller. */
    static List<RolledItem> roll(int mapId, int npcId, double itemRate,
                                 double adenaRate, double mapRate,
                                 RandomGenerator random) {
        if (!Double.isFinite(itemRate) || itemRate < 0
                || !Double.isFinite(adenaRate) || adenaRate < 0
                || !Double.isFinite(mapRate) || mapRate < 0 || random == null) {
            throw new IllegalArgumentException("invalid classic WORLD drop rate/random");
        }
        Catalog catalog = catalog(mapId);
        List<RolledItem> result = new ArrayList<>();
        for (DropRule rule : catalog.dropsByMob().getOrDefault(npcId, List.of())) {
            int roll = random.nextInt(CHANCE_SCALE) + 1;
            double finalChance = rule.chance() * itemRate * mapRate;
            if (itemRate == 0 || finalChance < roll) continue;
            long quantity = rule.minimum();
            int span = rule.maximum() - rule.minimum() + 1;
            if (span > 1) quantity += random.nextInt(span);
            if (rule.itemId() == InventoryCatalog.ADENA) {
                quantity = multiplyAndClamp(quantity, adenaRate);
            }
            if (quantity <= 0) continue;
            ItemWire item = catalog.items().get(rule.itemId());
            if (item == null) {
                throw new IllegalStateException("drop item wire row vanished " + rule.itemId());
            }
            result.add(new RolledItem(item, (int) quantity, roll));
        }
        return List.copyOf(result);
    }

    private static Catalog catalog(int mapId) {
        return BY_MAP.getOrDefault(mapId, new Catalog(mapId, Map.of(), Map.of()));
    }

    private static ItemWire itemAcrossMaps(int itemId) {
        ItemWire selected = null;
        for (Catalog catalog : BY_MAP.values()) {
            ItemWire candidate = catalog.items().get(itemId);
            if (candidate == null) continue;
            if (selected != null && !selected.equals(candidate)) {
                throw new IllegalStateException(
                        "classic WORLD item metadata conflicts across maps: " + itemId);
            }
            selected = candidate;
        }
        return selected;
    }

    private static long multiplyAndClamp(long count, double multiplier) {
        if (count <= 0 || multiplier <= 0 || Double.isNaN(multiplier)) return 0;
        double value = count * multiplier;
        if (!Double.isFinite(value) || value >= MAXIMUM_QUANTITY) {
            return MAXIMUM_QUANTITY;
        }
        return Math.max(0, (long) value);
    }

    private static Catalog load(String resourceName) {
        byte[] packed;
        try (InputStream resource = ClassicWorldDropCatalog.class
                .getResourceAsStream(resourceName)) {
            if (resource == null) throw new IOException("missing drop resource " + resourceName);
            packed = resource.readAllBytes();
        } catch (IOException error) {
            throw new ExceptionInInitializerError(error);
        }
        requireResourceHash(resourceName, packed);
        try (DataInputStream in = new DataInputStream(new GZIPInputStream(
                new ByteArrayInputStream(packed)))) {
            if (in.readInt() != MAGIC) throw new IOException("invalid drop resource magic");
            int mapId = in.readUnsignedShort();
            int expectedDrops = in.readInt();
            int expectedItems = in.readInt();
            byte[] sourceHash = in.readNBytes(32);
            if (sourceHash.length != 32) throw new IOException("truncated drop source hash");
            byte[] table = in.readAllBytes();
            String observedSource = sha256(table);
            String headerSource = HexFormat.of().withUpperCase().formatHex(sourceHash);
            if (!observedSource.equals(headerSource)) {
                throw new IOException("drop source hash does not match table");
            }
            ClassicWorldDesireCaveCatalog.ResourceContract desireCave =
                    ClassicWorldDesireCaveCatalog.DROP_CONTRACTS.get(mapId);
            if (desireCave != null
                    && (!desireCave.sourceSha256().equals(headerSource)
                        || expectedDrops != desireCave.rows()
                        || expectedItems != desireCave.secondaryCount())) {
                throw new IOException(
                        "Desire-Cave drop source contract changed " + mapId);
            }
            ClassicWorldPirateBackHalfCatalog.ComponentContract pirateBackHalf =
                    ClassicWorldPirateBackHalfCatalog.DROP_CONTRACTS.get(mapId);
            if (pirateBackHalf != null
                    && (!pirateBackHalf.sourceSha256().equals(headerSource)
                        || expectedDrops != pirateBackHalf.rows()
                        || expectedItems != pirateBackHalf.secondaryCount())) {
                throw new IOException(
                        "Pirate-back-half drop source contract changed " + mapId);
            }
            ClassicWorldBehemothSilveriaCatalog.ResourceContract homeTown =
                    ClassicWorldBehemothSilveriaCatalog.DROP_CONTRACTS.get(mapId);
            if (homeTown != null
                    && (!homeTown.sourceSha256().equals(headerSource)
                        || expectedDrops != homeTown.rows()
                        || expectedItems != homeTown.population())) {
                throw new IOException(
                        "home-town drop source contract changed " + mapId);
            }
            if (mapId == ClassicWorldMapCatalog.WORLD_TREE_MAP_ID
                    && (!MAP4_SOURCE_SHA256.equals(headerSource)
                        || expectedDrops != MAP4_DROP_COUNT
                        || expectedItems != MAP4_ITEM_COUNT)) {
                throw new IOException("map4 drop source contract changed");
            }
            if (mapId == ClassicWorldMapCatalog.TALKING_ISLAND_MAP_ID
                    && (!MAP0_SOURCE_SHA256.equals(headerSource)
                        || expectedDrops != MAP0_DROP_COUNT
                        || expectedItems != MAP0_ITEM_COUNT)) {
                throw new IOException("map0 drop source contract changed");
            }
            if (mapId == ClassicWorldMapCatalog.ROYAL_TRIAL_CAVE_FIRST_MAP_ID
                    && (!MAP1_SOURCE_SHA256.equals(headerSource)
                        || expectedDrops != MAP1_DROP_COUNT
                        || expectedItems != MAP1_ITEM_COUNT)) {
                throw new IOException("map1 drop source contract changed");
            }
            if (mapId == ClassicWorldMapCatalog.ROYAL_TRIAL_CAVE_SECOND_MAP_ID
                    && (!MAP2_SOURCE_SHA256.equals(headerSource)
                        || expectedDrops != MAP2_DROP_COUNT
                        || expectedItems != MAP2_ITEM_COUNT)) {
                throw new IOException("map2 drop source contract changed");
            }
            if (mapId == ClassicWorldMapCatalog.UNDERSEA_TUNNEL_MAP_ID
                    && (!MAP14_SOURCE_SHA256.equals(headerSource)
                        || expectedDrops != MAP14_DROP_COUNT
                        || expectedItems != MAP14_ITEM_COUNT)) {
                throw new IOException("map14 drop source contract changed");
            }
            if (mapId == ClassicWorldMapCatalog.UNDERSEA_TUNNEL_SEVENTH_FLOOR_MAP_ID
                    && (!MAP13_SOURCE_SHA256.equals(headerSource)
                        || expectedDrops != MAP13_DROP_COUNT
                        || expectedItems != MAP13_ITEM_COUNT)) {
                throw new IOException("map13 drop source contract changed");
            }
            if (mapId == ClassicWorldMapCatalog.JIM_SECRET_ROOM_MAP_ID
                    && (!MAP237_SOURCE_SHA256.equals(headerSource)
                        || expectedDrops != MAP237_DROP_COUNT
                        || expectedItems != MAP237_ITEM_COUNT)) {
                throw new IOException("map237 drop source contract changed");
            }
            if (mapId == ClassicWorldMapCatalog.GUNTER_HOUSE_MAP_ID
                    && (!MAP3_SOURCE_SHA256.equals(headerSource)
                        || expectedDrops != MAP3_DROP_COUNT
                        || expectedItems != MAP3_ITEM_COUNT)) {
                throw new IOException("map3 drop source contract changed");
            }
            if (mapId == ClassicWorldMapCatalog.TALKING_ISLAND_TO_GLUDIO_SHIP_MAP_ID
                    && (!MAP5_SOURCE_SHA256.equals(headerSource)
                        || expectedDrops != MAP5_DROP_COUNT
                        || expectedItems != MAP5_ITEM_COUNT)) {
                throw new IOException("map5 drop source contract changed");
            }
            if (mapId == ClassicWorldMapCatalog.GLUDIO_TO_TALKING_ISLAND_SHIP_MAP_ID
                    && (!MAP6_SOURCE_SHA256.equals(headerSource)
                        || expectedDrops != MAP6_DROP_COUNT
                        || expectedItems != MAP6_ITEM_COUNT)) {
                throw new IOException("map6 drop source contract changed");
            }
            if (mapId == ClassicWorldMapCatalog.FORGOTTEN_ISLAND_MAP_ID
                    && (!MAP70_SOURCE_SHA256.equals(headerSource)
                        || expectedDrops != MAP70_DROP_COUNT
                        || expectedItems != MAP70_ITEM_COUNT)) {
                throw new IOException("map70 drop source contract changed");
            }
            if (mapId == ClassicWorldMapCatalog.MAINLAND_TO_FORGOTTEN_ISLAND_SHIP_MAP_ID
                    && (!MAP83_SOURCE_SHA256.equals(headerSource)
                        || expectedDrops != MAP83_DROP_COUNT
                        || expectedItems != MAP83_ITEM_COUNT)) {
                throw new IOException("map83 drop source contract changed");
            }
            if (mapId == ClassicWorldMapCatalog.FORGOTTEN_ISLAND_TO_MAINLAND_SHIP_MAP_ID
                    && (!MAP84_SOURCE_SHA256.equals(headerSource)
                        || expectedDrops != MAP84_DROP_COUNT
                        || expectedItems != MAP84_ITEM_COUNT)) {
                throw new IOException("map84 drop source contract changed");
            }
            if (mapId == ClassicWorldMapCatalog.HARTIN_LABORATORY_MAP_ID
                    && (!MAP16_SOURCE_SHA256.equals(headerSource)
                        || expectedDrops != MAP16_DROP_COUNT
                        || expectedItems != MAP16_ITEM_COUNT)) {
                throw new IOException("map16 drop source contract changed");
            }
            if (mapId == ClassicWorldMapCatalog.NERUPA_CAVE_MAP_ID
                    && (!MAP17_SOURCE_SHA256.equals(headerSource)
                        || expectedDrops != MAP17_DROP_COUNT
                        || expectedItems != MAP17_ITEM_COUNT)) {
                throw new IOException("map17 drop source contract changed");
            }
            if (mapId == ClassicWorldMapCatalog.GERARD_TRIAL_DUNGEON_MAP_ID
                    && (!MAP22_SOURCE_SHA256.equals(headerSource)
                        || expectedDrops != MAP22_DROP_COUNT
                        || expectedItems != MAP22_ITEM_COUNT)) {
                throw new IOException("map22 drop source contract changed");
            }
            if (mapId == ClassicWorldMapCatalog.SLEEPING_DRAGON_FIRST_FLOOR_MAP_ID
                    && (!MAP19_SOURCE_SHA256.equals(headerSource)
                        || expectedDrops != MAP19_DROP_COUNT
                        || expectedItems != MAP19_ITEM_COUNT)) {
                throw new IOException("map19 drop source contract changed");
            }
            if (mapId == ClassicWorldMapCatalog.SLEEPING_DRAGON_SECOND_FLOOR_MAP_ID
                    && (!MAP20_SOURCE_SHA256.equals(headerSource)
                        || expectedDrops != MAP20_DROP_COUNT
                        || expectedItems != MAP20_ITEM_COUNT)) {
                throw new IOException("map20 drop source contract changed");
            }
            if (mapId == ClassicWorldMapCatalog.ORC_ELVEN_DUNGEON_MAP_ID
                    && (!MAP21_SOURCE_SHA256.equals(headerSource)
                        || expectedDrops != MAP21_DROP_COUNT
                        || expectedItems != MAP21_ITEM_COUNT)) {
                throw new IOException("map21 drop source contract changed");
            }
            if (mapId == ClassicWorldMapCatalog.DRAGON_VALLEY_DUNGEON_FIRST_FLOOR_MAP_ID
                    && (!MAP30_SOURCE_SHA256.equals(headerSource)
                        || expectedDrops != MAP30_DROP_COUNT
                        || expectedItems != MAP30_ITEM_COUNT)) {
                throw new IOException("map30 drop source contract changed");
            }
            if (mapId == ClassicWorldMapCatalog.DRAGON_VALLEY_DUNGEON_SECOND_FLOOR_MAP_ID
                    && (!MAP31_SOURCE_SHA256.equals(headerSource)
                        || expectedDrops != MAP31_DROP_COUNT
                        || expectedItems != MAP31_ITEM_COUNT)) {
                throw new IOException("map31 drop source contract changed");
            }
            if (mapId == ClassicWorldMapCatalog.DRAGON_VALLEY_DUNGEON_THIRD_FLOOR_MAP_ID
                    && (!MAP32_SOURCE_SHA256.equals(headerSource)
                        || expectedDrops != MAP32_DROP_COUNT
                        || expectedItems != MAP32_ITEM_COUNT)) {
                throw new IOException("map32 drop source contract changed");
            }
            if (mapId == ClassicWorldMapCatalog.DRAGON_VALLEY_DUNGEON_FOURTH_FLOOR_MAP_ID
                    && (!MAP33_SOURCE_SHA256.equals(headerSource)
                        || expectedDrops != MAP33_DROP_COUNT
                        || expectedItems != MAP33_ITEM_COUNT)) {
                throw new IOException("map33 drop source contract changed");
            }
            if (mapId == ClassicWorldMapCatalog.DRAGON_VALLEY_DUNGEON_FIFTH_FLOOR_MAP_ID
                    && (!MAP35_SOURCE_SHA256.equals(headerSource)
                        || expectedDrops != MAP35_DROP_COUNT
                        || expectedItems != MAP35_ITEM_COUNT)) {
                throw new IOException("map35 drop source contract changed");
            }
            if (mapId == ClassicWorldMapCatalog.DRAGON_VALLEY_DUNGEON_SIXTH_FLOOR_MAP_ID
                    && (!MAP36_SOURCE_SHA256.equals(headerSource)
                        || expectedDrops != MAP36_DROP_COUNT
                        || expectedItems != MAP36_ITEM_COUNT)) {
                throw new IOException("map36 drop source contract changed");
            }
            if (mapId == ClassicWorldMapCatalog.ANTHARAS_LAIR_MAP_ID
                    && (!MAP37_SOURCE_SHA256.equals(headerSource)
                        || expectedDrops != MAP37_DROP_COUNT
                        || expectedItems != MAP37_ITEM_COUNT)) {
                throw new IOException("map37 drop source contract changed");
            }
            if (mapId == ClassicWorldMapCatalog.VALAKAS_HABITAT_MAP_ID
                    && (!MAP67_SOURCE_SHA256.equals(headerSource)
                        || expectedDrops != MAP67_DROP_COUNT
                        || expectedItems != MAP67_ITEM_COUNT)) {
                throw new IOException("map67 drop source contract changed");
            }
            if (mapId == ClassicWorldMapCatalog.DARK_ELF_DUNGEON_MAP_ID
                    && (!MAP209_SOURCE_SHA256.equals(headerSource)
                        || expectedDrops != MAP209_DROP_COUNT
                        || expectedItems != MAP209_ITEM_COUNT)) {
                throw new IOException("map209 drop source contract changed");
            }
            if (mapId == ClassicWorldMapCatalog.DARKMAR_DUNGEON_MAP_ID
                    && (!MAP213_SOURCE_SHA256.equals(headerSource)
                        || expectedDrops != MAP213_DROP_COUNT
                        || expectedItems != MAP213_ITEM_COUNT)) {
                throw new IOException("map213 drop source contract changed");
            }
            if (mapId == ClassicWorldMapCatalog.SEPIA_DUNGEON_MAP_ID
                    && (!MAP302_SOURCE_SHA256.equals(headerSource)
                        || expectedDrops != MAP302_DROP_COUNT
                        || expectedItems != MAP302_ITEM_COUNT)) {
                throw new IOException("map302 drop source contract changed");
            }
            if (mapId == ClassicWorldMapCatalog.HIDDEN_DOCK_MAP_ID
                    && (!MAP445_SOURCE_SHA256.equals(headerSource)
                        || expectedDrops != MAP445_DROP_COUNT
                        || expectedItems != MAP445_ITEM_COUNT)) {
                throw new IOException("map445 drop source contract changed");
            }
            if (mapId == ClassicWorldMapCatalog.PIRATE_ISLAND_SHIP_MAP_ID
                    && (!MAP447_SOURCE_SHA256.equals(headerSource)
                        || expectedDrops != MAP447_DROP_COUNT
                        || expectedItems != MAP447_ITEM_COUNT)) {
                throw new IOException("map447 drop source contract changed");
            }
            if (mapId == ClassicWorldMapCatalog.PHANTASM_ISLAND_MAP_ID
                    && (!MAP303_SOURCE_SHA256.equals(headerSource)
                        || expectedDrops != MAP303_DROP_COUNT
                        || expectedItems != MAP303_ITEM_COUNT)) {
                throw new IOException("map303 drop source contract changed");
            }
            if (mapId == ClassicWorldMapCatalog.TAINTED_LAND_MAP_ID
                    && (!MAP244_SOURCE_SHA256.equals(headerSource)
                        || expectedDrops != MAP244_DROP_COUNT
                        || expectedItems != MAP244_ITEM_COUNT)) {
                throw new IOException("map244 drop source contract changed");
            }
            SourceContract antCave = ANT_CAVE_SOURCE_CONTRACTS.get(mapId);
            if (antCave != null && (!antCave.sourceSha256().equals(headerSource)
                    || expectedDrops != antCave.drops()
                    || expectedItems != antCave.items())) {
                throw new IOException("ant-cave drop source contract changed " + mapId);
            }
            SourceContract knightCave = KNIGHT_CAVE_SOURCE_CONTRACTS.get(mapId);
            if (knightCave != null && (!knightCave.sourceSha256().equals(headerSource)
                    || expectedDrops != knightCave.drops()
                    || expectedItems != knightCave.items())) {
                throw new IOException("knight-cave drop source contract changed " + mapId);
            }
            SourceContract gludioDungeon = GLUDIO_DUNGEON_SOURCE_CONTRACTS.get(mapId);
            if (gludioDungeon != null
                    && (!gludioDungeon.sourceSha256().equals(headerSource)
                        || expectedDrops != gludioDungeon.drops()
                        || expectedItems != gludioDungeon.items())) {
                throw new IOException("Gludio Dungeon drop source contract changed "
                        + mapId);
            }
            SourceContract giranDungeon = GIRAN_DUNGEON_SOURCE_CONTRACTS.get(mapId);
            if (giranDungeon != null
                    && (!giranDungeon.sourceSha256().equals(headerSource)
                        || expectedDrops != giranDungeon.drops()
                        || expectedItems != giranDungeon.items())) {
                throw new IOException("Giran Dungeon drop source contract changed "
                        + mapId);
            }
            if (mapId == ClassicWorldMapCatalog.GIRAN_CASTLE_MAP_ID
                    && (!MAP52_SOURCE_CONTRACT.sourceSha256().equals(headerSource)
                        || expectedDrops != MAP52_SOURCE_CONTRACT.drops()
                        || expectedItems != MAP52_SOURCE_CONTRACT.items())) {
                throw new IOException("map52 drop source contract changed");
            }
            SourceContract windawood = WINDAWOOD_CASTLE_SOURCE_CONTRACTS.get(mapId);
            if (windawood != null
                    && (!windawood.sourceSha256().equals(headerSource)
                        || expectedDrops != windawood.drops()
                        || expectedItems != windawood.items())) {
                throw new IOException(
                        "Windawood-castle drop source contract changed " + mapId);
            }
            SourceContract kent = KENT_COMPONENT_SOURCE_CONTRACTS.get(mapId);
            if (kent != null
                    && (!kent.sourceSha256().equals(headerSource)
                        || expectedDrops != kent.drops()
                        || expectedItems != kent.items())) {
                throw new IOException(
                        "Kent-component drop source contract changed " + mapId);
            }
            ClassicWorldIvoryTowerCatalog.SourceContract ivoryTower =
                    IVORY_TOWER_SOURCE_CONTRACTS.get(mapId);
            if (ivoryTower != null
                    && (!ivoryTower.sourceSha256().equals(headerSource)
                        || expectedDrops != ivoryTower.rows()
                        || expectedItems != ivoryTower.population())) {
                throw new IOException(
                        "Ivory-Tower drop source contract changed " + mapId);
            }
            if (mapId == ClassicWorldDwarfCastleCatalog.MAP_ID
                    && (!MAP66_SOURCE_CONTRACT.sourceSha256().equals(headerSource)
                        || expectedDrops != MAP66_SOURCE_CONTRACT.drops()
                        || expectedItems != MAP66_SOURCE_CONTRACT.items())) {
                throw new IOException("map66 drop source contract changed");
            }
            ClassicWorldAdenToiCatalog.ResourceContract adenToi =
                    ClassicWorldAdenToiCatalog.DROP_CONTRACTS.get(mapId);
            if (adenToi != null
                    && (!adenToi.sourceSha256().equals(headerSource)
                        || expectedDrops != adenToi.rows()
                        || expectedItems != adenToi.secondaryCount())) {
                throw new IOException(
                        "Aden/TOI drop source contract changed " + mapId);
            }
            ClassicWorldHeineCatalog.SourceContract heine =
                    ClassicWorldHeineCatalog.DROP_CONTRACTS.get(mapId);
            if (heine != null
                    && (!heine.sourceSha256().equals(headerSource)
                        || expectedDrops != heine.rows()
                        || expectedItems != heine.population())) {
                throw new IOException("Heine/Eva drop source contract changed " + mapId);
            }
            ClassicWorldSilentCavernCatalog.SourceContract silent =
                    ClassicWorldSilentCavernCatalog.DROP_CONTRACTS.get(mapId);
            if (silent != null
                    && (!silent.sourceSha256().equals(headerSource)
                        || expectedDrops != silent.rows()
                        || expectedItems != silent.population())) {
                throw new IOException(
                        "Silent Cavern drop source contract changed " + mapId);
            }
            ClassicWorldRastabadOumCatalog.SourceContract rastabad =
                    ClassicWorldRastabadOumCatalog.DROP_CONTRACTS.get(mapId);
            if (rastabad != null
                    && (!rastabad.sourceSha256().equals(headerSource)
                        || expectedDrops != rastabad.rows()
                        || expectedItems != rastabad.population())) {
                throw new IOException(
                        "Rastabad/Oum drop source contract changed " + mapId);
            }
            ClassicWorldDiadradCatalog.SourceContract diadrad =
                    ClassicWorldDiadradCatalog.DROP_CONTRACTS.get(mapId);
            if (diadrad != null
                    && (!diadrad.sourceSha256().equals(headerSource)
                        || expectedDrops != diadrad.rows()
                        || expectedItems != diadrad.population())) {
                throw new IOException(
                        "Diad service-core drop source contract changed " + mapId);
            }
            ClassicWorldElementalGraveCatalog.SourceContract elementalGrave =
                    ClassicWorldElementalGraveCatalog.DROP_CONTRACTS.get(mapId);
            if (elementalGrave != null
                    && (!elementalGrave.sourceSha256().equals(headerSource)
                        || expectedDrops != elementalGrave.rows()
                        || expectedItems != elementalGrave.population())) {
                throw new IOException(
                        "Elemental-Grave drop source contract changed " + mapId);
            }
            ClassicWorldPirateDungeonCatalog.SourceContract pirateDungeon =
                    ClassicWorldPirateDungeonCatalog.DROP_CONTRACTS.get(mapId);
            if (pirateDungeon != null
                    && (!pirateDungeon.sourceSha256().equals(headerSource)
                        || expectedDrops != pirateDungeon.rows()
                        || expectedItems != pirateDungeon.population())) {
                throw new IOException(
                        "Pirate-dungeon drop source contract changed " + mapId);
            }
            if (mapId == ClassicWorldLastabadGateCatalog.MAP_ID
                    && (!ClassicWorldLastabadGateCatalog.DROP_SOURCE_SHA256
                            .equals(headerSource)
                        || expectedDrops != 0 || expectedItems != 0)) {
                throw new IOException("Lastabad-gate drop source contract changed");
            }
            Catalog result = parse(mapId, table);
            int drops = result.dropsByMob().values().stream().mapToInt(List::size).sum();
            if (drops != expectedDrops || result.items().size() != expectedItems) {
                throw new IOException("drop row/item count changed");
            }
            return result;
        } catch (IOException | RuntimeException error) {
            ExceptionInInitializerError wrapped = new ExceptionInInitializerError(
                    "cannot load classic drop resource " + resourceName
                            + ": " + error.getMessage());
            wrapped.initCause(error);
            throw wrapped;
        }
    }

    private static Catalog parse(int mapId, byte[] table) throws IOException {
        Map<Integer, List<DropRule>> mutableDrops = new LinkedHashMap<>();
        Map<Integer, ItemWire> items = new LinkedHashMap<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                new ByteArrayInputStream(table), StandardCharsets.UTF_8))) {
            if (!"[drops]".equals(reader.readLine())
                    || !DROP_HEADER.equals(reader.readLine())) {
                throw new IOException("classic drop TSV header changed");
            }
            String line;
            while ((line = reader.readLine()) != null && !line.equals("[items]")) {
                String[] field = line.split("\t", -1);
                if (field.length != 5) throw new IOException("drop row field count changed");
                DropRule rule = new DropRule(
                        integer(field[0]), integer(field[1]), integer(field[2]),
                        integer(field[3]), integer(field[4]));
                mutableDrops.computeIfAbsent(rule.mobId(), ignored -> new ArrayList<>())
                        .add(rule);
            }
            if (line == null || !ITEM_HEADER.equals(reader.readLine())) {
                throw new IOException("classic item TSV header changed");
            }
            while ((line = reader.readLine()) != null) {
                String[] field = line.split("\t", -1);
                if (field.length != 17) throw new IOException("item row field count changed");
                ItemWire item = new ItemWire(
                        integer(field[0]), field[1], field[2], field[3],
                        integer(field[4]), integer(field[5]), integer(field[6]),
                        bool(field[7]), bool(field[8]), integer(field[9]),
                        integer(field[10]), field[11], integer(field[12]),
                        integer(field[13]), field[14], integer(field[15]),
                        integer(field[16]));
                if (items.put(item.itemId(), item) != null) {
                    throw new IOException("duplicate classic item " + item.itemId());
                }
            }
        }
        Map<Integer, List<DropRule>> drops = new LinkedHashMap<>();
        for (Map.Entry<Integer, List<DropRule>> entry : mutableDrops.entrySet()) {
            drops.put(entry.getKey(), List.copyOf(entry.getValue()));
        }
        for (List<DropRule> rules : drops.values()) {
            for (DropRule rule : rules) {
                if (!items.containsKey(rule.itemId())) {
                    throw new IOException("drop references absent item " + rule.itemId());
                }
            }
        }
        return new Catalog(mapId, Map.copyOf(drops), Map.copyOf(items));
    }

    private static int integer(String value) {
        return Integer.parseInt(value);
    }

    private static boolean bool(String value) {
        int parsed = integer(value);
        if (parsed != 0 && parsed != 1) throw new IllegalArgumentException("not a boolean");
        return parsed != 0;
    }

    static int nativeUseType(String kind, String token) {
        if (kind.equals("weapon")) return 1;
        int value = switch (token) {
            case "petitem" -> -12; case "other" -> -11; case "power" -> -10;
            case "book" -> -9; case "makecooking" -> -8; case "hpr" -> -7;
            case "mpr" -> -6; case "ticket" -> -5; case "petcollar" -> -4;
            case "sting" -> -3; case "arrow" -> -2; case "none" -> -1;
            case "normal" -> 0; case "weapon" -> 1; case "armor" -> 2;
            case "spell_1" -> 3; case "4" -> 4; case "spell_long" -> 5;
            case "ntele" -> 6; case "identify" -> 7; case "res" -> 8;
            case "home" -> 9; case "light" -> 10; case "11" -> 11;
            case "letter" -> 12; case "letter_card" -> 13; case "choice" -> 14;
            case "instrument" -> 15; case "sosc" -> 16; case "spell_short" -> 17;
            case "T" -> 18; case "cloak" -> 19; case "glove" -> 20;
            case "boots" -> 21; case "helm" -> 22; case "ring", "ring2" -> 23;
            case "amulet" -> 24; case "shield", "guarder" -> 25;
            case "dai" -> 26; case "zel" -> 27; case "blank" -> 28;
            case "btele" -> 29; case "spell_buff" -> 30; case "ccard" -> 31;
            case "ccard_w" -> 32; case "vcard" -> 33; case "vcard_w" -> 34;
            case "wcard" -> 35; case "wcard_w" -> 36; case "belt" -> 37;
            case "food" -> 38; case "spell_long2" -> 39; case "earring" -> 40;
            case "fishing_rod" -> 42; case "aid_left" -> 43;
            case "aid_right" -> 44; case "enc" -> 46;
            case "talisman", "talisman2", "talisman3", "talisman4",
                    "talisman5", "shoulder_left", "shoulder_right" -> 49;
            case "choice_doll" -> 55; case "pants" -> 70;
            default -> throw new IllegalArgumentException("unknown item use token " + token);
        };
        return value & 0xFF;
    }

    /** Exact retained L1Armor.UseType code; unlike LiTo's wire use type this
     * preserves distinct equipment positions which share one client UI path. */
    static int armorTypeCode(String token) {
        return switch (token) {
            case "none" -> 0;
            case "helm" -> 1;
            case "armor" -> 2;
            case "T" -> 3;
            case "cloak" -> 4;
            case "glove" -> 5;
            case "boots" -> 6;
            case "shield" -> 7;
            case "amulet" -> 8;
            case "ring" -> 9;
            case "belt" -> 10;
            case "ring2" -> 11;
            case "earring" -> 12;
            case "guarder" -> 13;
            case "aid_left" -> 14;
            case "aid_right" -> 15;
            case "pants" -> 17;
            case "talisman" -> 19;
            case "talisman2" -> 20;
            case "talisman3" -> 21;
            case "talisman4" -> 22;
            case "shoulder_left" -> 23;
            case "shoulder_right" -> 24;
            case "talisman5" -> 25;
            default -> throw new IllegalArgumentException(
                    "unknown retained armor use token " + token);
        };
    }

    static boolean armorTypeSupportsUseType(int armorType, int useType) {
        int expected = switch (armorType) {
            case 1 -> 22;
            case 2 -> 2;
            case 3 -> 18;
            case 4 -> 19;
            case 5 -> 20;
            case 6 -> 21;
            case 7, 13 -> 25;
            case 8 -> 24;
            case 9, 11 -> 23;
            case 10 -> 37;
            case 12 -> 40;
            case 14 -> 43;
            case 15 -> 44;
            case 17 -> 70;
            case 19, 20, 21, 22, 23, 24, 25 -> 49;
            default -> -1;
        };
        return expected == useType;
    }

    static int weaponWalkAction(String kind, String type) {
        if (!kind.equals("weapon")) return BattleActionCodes.WALK;
        return switch (type) {
            case "none" -> BattleActionCodes.WALK;
            case "sword" -> BattleActionCodes.SWORD_WALK;
            case "dagger" -> BattleActionCodes.DAGGER_WALK;
            case "tohandsword" -> BattleActionCodes.TWO_HAND_SWORD_WALK;
            case "bow", "singlebow" -> BattleActionCodes.BOW_WALK;
            case "spear", "singlespear" -> BattleActionCodes.SPEAR_WALK;
            case "blunt", "tohandblunt" -> BattleActionCodes.AXE_WALK;
            case "staff", "tohandstaff" -> BattleActionCodes.STAFF_WALK;
            case "throwingknife", "gauntlet" -> BattleActionCodes.THROWING_KNIFE_WALK;
            case "claw", "kiringku" -> BattleActionCodes.CLAW_WALK;
            case "edoryu" -> BattleActionCodes.EDORYU_WALK;
            case "chainsword" -> BattleActionCodes.CHAINSWORD_WALK;
            default -> throw new IllegalArgumentException("unknown weapon type " + type);
        };
    }

    static int materialCode(String material) {
        return switch (material) {
            case "none" -> 0; case "liquid" -> 1; case "web" -> 2;
            case "vegetation" -> 3; case "animalmatter" -> 4; case "paper" -> 5;
            case "cloth" -> 6; case "leather" -> 7; case "wood" -> 8;
            case "bone" -> 9; case "dragonscale" -> 10; case "iron" -> 11;
            case "steel" -> 12; case "copper" -> 13; case "silver" -> 14;
            case "gold" -> 15; case "platinum" -> 16; case "mithril" -> 17;
            case "blackmithril" -> 18; case "glass" -> 19; case "gemstone" -> 20;
            case "mineral" -> 21; case "oriharukon" -> 22;
            default -> throw new IllegalArgumentException("unknown item material " + material);
        };
    }

    private static void requireResourceHash(String resourceName, byte[] data) {
        for (Map.Entry<Integer, ClassicWorldDesireCaveCatalog.ResourceContract> entry
                : ClassicWorldDesireCaveCatalog.DROP_CONTRACTS.entrySet()) {
            if (resourceName.equals("/world/map" + entry.getKey()
                    + "-drops.lwd.gz")) {
                if (!entry.getValue().resourceSha256().equals(sha256(data))) {
                    throw new ExceptionInInitializerError(
                            "Desire-Cave drop resource hash mismatch "
                                    + entry.getKey());
                }
                return;
            }
        }
        for (Map.Entry<Integer, ClassicWorldPirateBackHalfCatalog.ComponentContract> entry
                : ClassicWorldPirateBackHalfCatalog.DROP_CONTRACTS.entrySet()) {
            if (resourceName.equals("/world/map" + entry.getKey()
                    + "-drops.lwd.gz")) {
                if (!entry.getValue().resourceSha256().equals(sha256(data))) {
                    throw new ExceptionInInitializerError(
                            "Pirate-back-half drop resource hash mismatch "
                                    + entry.getKey());
                }
                return;
            }
        }
        if (resourceName.equals("/world/map237-drops.lwd.gz")) {
            String actual = sha256(data);
            if (!MAP237_RESOURCE_SHA256.equals(actual)) {
                throw new ExceptionInInitializerError(
                        "map237 drop resource hash mismatch");
            }
            return;
        }
        if (resourceName.equals("/world/map1-drops.lwd.gz")
                || resourceName.equals("/world/map2-drops.lwd.gz")
                || resourceName.equals("/world/map14-drops.lwd.gz")
                || resourceName.equals("/world/map13-drops.lwd.gz")) {
            String expected = resourceName.equals("/world/map1-drops.lwd.gz")
                    ? MAP1_RESOURCE_SHA256
                    : resourceName.equals("/world/map2-drops.lwd.gz")
                        ? MAP2_RESOURCE_SHA256
                        : resourceName.equals("/world/map14-drops.lwd.gz")
                            ? MAP14_RESOURCE_SHA256 : MAP13_RESOURCE_SHA256;
            String actual = sha256(data);
            if (!expected.equals(actual)) {
                throw new ExceptionInInitializerError(
                        "royal-trial drop resource hash mismatch for " + resourceName);
            }
            return;
        }
        String mainlandShipExpected = resourceName.endsWith("map0-drops.lwd.gz")
                ? MAP0_RESOURCE_SHA256
                : resourceName.endsWith("map3-drops.lwd.gz")
                    ? MAP3_RESOURCE_SHA256
                : resourceName.endsWith("map5-drops.lwd.gz")
                    ? MAP5_RESOURCE_SHA256
                    : resourceName.endsWith("map6-drops.lwd.gz")
                        ? MAP6_RESOURCE_SHA256
                        : resourceName.endsWith("map70-drops.lwd.gz")
                            ? MAP70_RESOURCE_SHA256
                            : resourceName.endsWith("map83-drops.lwd.gz")
                                ? MAP83_RESOURCE_SHA256
                                : resourceName.endsWith("map84-drops.lwd.gz")
                                    ? MAP84_RESOURCE_SHA256 : null;
        if (mainlandShipExpected != null) {
            String actual = sha256(data);
            if (!mainlandShipExpected.equals(actual)) {
                throw new ExceptionInInitializerError(
                        "mainland ship drop resource hash mismatch for " + resourceName);
            }
            return;
        }
        for (Map.Entry<Integer, SourceContract> entry
                : ANT_CAVE_SOURCE_CONTRACTS.entrySet()) {
            if (resourceName.equals("/world/map" + entry.getKey()
                    + "-drops.lwd.gz")) {
                String actual = sha256(data);
                if (!entry.getValue().resourceSha256().equals(actual)) {
                    throw new ExceptionInInitializerError(
                            "ant-cave drop resource hash mismatch " + entry.getKey());
                }
                return;
            }
        }
        for (Map.Entry<Integer, SourceContract> entry
                : KNIGHT_CAVE_SOURCE_CONTRACTS.entrySet()) {
            if (resourceName.equals("/world/map" + entry.getKey()
                    + "-drops.lwd.gz")) {
                String actual = sha256(data);
                if (!entry.getValue().resourceSha256().equals(actual)) {
                    throw new ExceptionInInitializerError(
                            "knight-cave drop resource hash mismatch " + entry.getKey());
                }
                return;
            }
        }
        for (Map.Entry<Integer, SourceContract> entry
                : GLUDIO_DUNGEON_SOURCE_CONTRACTS.entrySet()) {
            if (resourceName.equals("/world/map" + entry.getKey()
                    + "-drops.lwd.gz")) {
                String actual = sha256(data);
                if (!entry.getValue().resourceSha256().equals(actual)) {
                    throw new ExceptionInInitializerError(
                            "Gludio Dungeon drop resource hash mismatch "
                                    + entry.getKey());
                }
                return;
            }
        }
        for (Map.Entry<Integer, SourceContract> entry
                : GIRAN_DUNGEON_SOURCE_CONTRACTS.entrySet()) {
            if (resourceName.equals("/world/map" + entry.getKey()
                    + "-drops.lwd.gz")) {
                String actual = sha256(data);
                if (!entry.getValue().resourceSha256().equals(actual)) {
                    throw new ExceptionInInitializerError(
                            "Giran Dungeon drop resource hash mismatch "
                                    + entry.getKey());
                }
                return;
            }
        }
        if (resourceName.equals("/world/map52-drops.lwd.gz")) {
            if (!MAP52_SOURCE_CONTRACT.resourceSha256().equals(sha256(data))) {
                throw new ExceptionInInitializerError(
                        "map52 drop resource hash mismatch");
            }
            return;
        }
        for (Map.Entry<Integer, SourceContract> entry
                : WINDAWOOD_CASTLE_SOURCE_CONTRACTS.entrySet()) {
            if (resourceName.equals("/world/map" + entry.getKey()
                    + "-drops.lwd.gz")) {
                if (!entry.getValue().resourceSha256().equals(sha256(data))) {
                    throw new ExceptionInInitializerError(
                            "Windawood-castle drop resource hash mismatch "
                                    + entry.getKey());
                }
                return;
            }
        }
        for (Map.Entry<Integer, SourceContract> entry
                : KENT_COMPONENT_SOURCE_CONTRACTS.entrySet()) {
            if (resourceName.equals("/world/map" + entry.getKey()
                    + "-drops.lwd.gz")) {
                if (!entry.getValue().resourceSha256().equals(sha256(data))) {
                    throw new ExceptionInInitializerError(
                            "Kent-component drop resource hash mismatch "
                                    + entry.getKey());
                }
                return;
            }
        }
        for (Map.Entry<Integer, ClassicWorldIvoryTowerCatalog.SourceContract> entry
                : IVORY_TOWER_SOURCE_CONTRACTS.entrySet()) {
            if (resourceName.equals("/world/map" + entry.getKey()
                    + "-drops.lwd.gz")) {
                if (!entry.getValue().resourceSha256().equals(sha256(data))) {
                    throw new ExceptionInInitializerError(
                            "Ivory-Tower drop resource hash mismatch "
                                    + entry.getKey());
                }
                return;
            }
        }
        if (resourceName.equals("/world/map66-drops.lwd.gz")) {
            if (!MAP66_SOURCE_CONTRACT.resourceSha256().equals(sha256(data))) {
                throw new ExceptionInInitializerError(
                        "map66 drop resource hash mismatch");
            }
            return;
        }
        for (Map.Entry<Integer, ClassicWorldAdenToiCatalog.ResourceContract> entry
                : ClassicWorldAdenToiCatalog.DROP_CONTRACTS.entrySet()) {
            if (resourceName.equals("/world/map" + entry.getKey()
                    + "-drops.lwd.gz")) {
                if (!entry.getValue().resourceSha256().equals(sha256(data))) {
                    throw new ExceptionInInitializerError(
                            "Aden/TOI drop resource hash mismatch "
                                    + entry.getKey());
                }
                return;
            }
        }
        for (Map.Entry<Integer, ClassicWorldHeineCatalog.SourceContract> entry
                : ClassicWorldHeineCatalog.DROP_CONTRACTS.entrySet()) {
            if (resourceName.equals("/world/map" + entry.getKey()
                    + "-drops.lwd.gz")) {
                if (!entry.getValue().resourceSha256().equals(sha256(data))) {
                    throw new ExceptionInInitializerError(
                            "Heine/Eva drop resource hash mismatch " + entry.getKey());
                }
                return;
            }
        }
        for (Map.Entry<Integer, ClassicWorldSilentCavernCatalog.SourceContract> entry
                : ClassicWorldSilentCavernCatalog.DROP_CONTRACTS.entrySet()) {
            if (resourceName.equals("/world/map" + entry.getKey()
                    + "-drops.lwd.gz")) {
                if (!entry.getValue().resourceSha256().equals(sha256(data))) {
                    throw new ExceptionInInitializerError(
                            "Silent Cavern drop resource hash mismatch " + entry.getKey());
                }
                return;
            }
        }
        for (Map.Entry<Integer, ClassicWorldRastabadOumCatalog.SourceContract> entry
                : ClassicWorldRastabadOumCatalog.DROP_CONTRACTS.entrySet()) {
            if (resourceName.equals("/world/map" + entry.getKey()
                    + "-drops.lwd.gz")) {
                if (!entry.getValue().resourceSha256().equals(sha256(data))) {
                    throw new ExceptionInInitializerError(
                            "Rastabad/Oum drop resource hash mismatch " + entry.getKey());
                }
                return;
            }
        }
        for (Map.Entry<Integer, ClassicWorldDiadradCatalog.SourceContract> entry
                : ClassicWorldDiadradCatalog.DROP_CONTRACTS.entrySet()) {
            if (resourceName.equals("/world/map" + entry.getKey()
                    + "-drops.lwd.gz")) {
                if (!entry.getValue().resourceSha256().equals(sha256(data))) {
                    throw new ExceptionInInitializerError(
                            "Diad service-core drop resource hash mismatch "
                                    + entry.getKey());
                }
                return;
            }
        }
        for (Map.Entry<Integer, ClassicWorldElementalGraveCatalog.SourceContract> entry
                : ClassicWorldElementalGraveCatalog.DROP_CONTRACTS.entrySet()) {
            if (resourceName.equals("/world/map" + entry.getKey()
                    + "-drops.lwd.gz")) {
                if (!entry.getValue().resourceSha256().equals(sha256(data))) {
                    throw new ExceptionInInitializerError(
                            "Elemental-Grave drop resource hash mismatch "
                                    + entry.getKey());
                }
                return;
            }
        }
        for (Map.Entry<Integer, ClassicWorldPirateDungeonCatalog.SourceContract> entry
                : ClassicWorldPirateDungeonCatalog.DROP_CONTRACTS.entrySet()) {
            if (resourceName.equals("/world/map" + entry.getKey()
                    + "-drops.lwd.gz")) {
                if (!entry.getValue().resourceSha256().equals(sha256(data))) {
                    throw new ExceptionInInitializerError(
                            "Pirate-dungeon drop resource hash mismatch "
                                    + entry.getKey());
                }
                return;
            }
        }
        for (Map.Entry<Integer, ClassicWorldBehemothSilveriaCatalog.ResourceContract> entry
                : ClassicWorldBehemothSilveriaCatalog.DROP_CONTRACTS.entrySet()) {
            if (resourceName.equals("/world/map" + entry.getKey()
                    + "-drops.lwd.gz")) {
                if (!entry.getValue().resourceSha256().equals(sha256(data))) {
                    throw new ExceptionInInitializerError(
                            "home-town drop resource hash mismatch " + entry.getKey());
                }
                return;
            }
        }
        if (resourceName.equals("/world/map410-drops.lwd.gz")) {
            if (!ClassicWorldDemonTempleCatalog.DROP_RESOURCE_SHA256
                    .equals(sha256(data))) {
                throw new ExceptionInInitializerError(
                        "Demon Temple drop resource hash mismatch");
            }
            return;
        }
        if (resourceName.equals("/world/map450-drops.lwd.gz")) {
            if (!ClassicWorldLastabadGateCatalog.DROP_RESOURCE_SHA256
                    .equals(sha256(data))) {
                throw new ExceptionInInitializerError(
                        "Lastabad-gate drop resource hash mismatch");
            }
            return;
        }
        String expected = resourceName.endsWith("map4-drops.lwd.gz")
                ? MAP4_RESOURCE_SHA256
                : resourceName.endsWith("map16-drops.lwd.gz")
                    ? MAP16_RESOURCE_SHA256
                : resourceName.endsWith("map17-drops.lwd.gz")
                    ? MAP17_RESOURCE_SHA256
                    : resourceName.endsWith("map22-drops.lwd.gz")
                        ? MAP22_RESOURCE_SHA256
                    : resourceName.endsWith("map19-drops.lwd.gz")
                        ? MAP19_RESOURCE_SHA256
                        : resourceName.endsWith("map20-drops.lwd.gz")
                            ? MAP20_RESOURCE_SHA256
                            : resourceName.endsWith("map21-drops.lwd.gz")
                                ? MAP21_RESOURCE_SHA256
                                : resourceName.endsWith("map30-drops.lwd.gz")
                                    ? MAP30_RESOURCE_SHA256
                                    : resourceName.endsWith("map31-drops.lwd.gz")
                                        ? MAP31_RESOURCE_SHA256
                                    : resourceName.endsWith("map32-drops.lwd.gz")
                                        ? MAP32_RESOURCE_SHA256
                                        : resourceName.endsWith("map33-drops.lwd.gz")
                                            ? MAP33_RESOURCE_SHA256
                                            : resourceName.endsWith("map35-drops.lwd.gz")
                                                ? MAP35_RESOURCE_SHA256
                                                : resourceName.endsWith("map36-drops.lwd.gz")
                                                    ? MAP36_RESOURCE_SHA256
                                                    : resourceName.endsWith("map37-drops.lwd.gz")
                                                        ? MAP37_RESOURCE_SHA256
                                            : resourceName.endsWith("map67-drops.lwd.gz")
                                                ? MAP67_RESOURCE_SHA256
                                            : resourceName.endsWith("map209-drops.lwd.gz")
                                                ? MAP209_RESOURCE_SHA256
                                                : resourceName.endsWith("map213-drops.lwd.gz")
                                                    ? MAP213_RESOURCE_SHA256
                                                    : resourceName.endsWith("map302-drops.lwd.gz")
                                                        ? MAP302_RESOURCE_SHA256
                                                    : resourceName.endsWith("map445-drops.lwd.gz")
                                                        ? MAP445_RESOURCE_SHA256
                                                        : resourceName.endsWith("map447-drops.lwd.gz")
                                                            ? MAP447_RESOURCE_SHA256
                                                            : resourceName.endsWith("map303-drops.lwd.gz")
                                                                ? MAP303_RESOURCE_SHA256
                                                                : resourceName.endsWith("map244-drops.lwd.gz")
                                                                    ? MAP244_RESOURCE_SHA256 : "";
        String actual = sha256(data);
        if (!expected.equals(actual)) {
            throw new ExceptionInInitializerError(
                    "classic drop resource hash mismatch for " + resourceName
                            + ": expected " + expected + ", got " + actual);
        }
    }

    private static String sha256(byte[] data) {
        try {
            return HexFormat.of().withUpperCase().formatHex(
                    MessageDigest.getInstance("SHA-256").digest(data));
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException(impossible);
        }
    }
}
