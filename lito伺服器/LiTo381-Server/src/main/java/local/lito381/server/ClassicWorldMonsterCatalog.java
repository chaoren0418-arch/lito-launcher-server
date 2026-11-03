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
import java.util.zip.GZIPInputStream;

/** Hash-locked selected-source monster spawn definitions for classic WORLD. */
final class ClassicWorldMonsterCatalog {
    static final int MAP0_DEFINITION_COUNT = 53;
    static final int MAP0_POPULATION = 492;
    static final String MAP0_SOURCE_SHA256 =
            "DB2F2FD225097627E6BF1C140F67B1130F9F365231664C5AE3142C47A2CF9DF3";
    static final String MAP0_RESOURCE_SHA256 =
            "97C862C0A03DE45406AF04AC0AA4CC64C9F091A1605A481A7EFA003B6F68D56A";
    static final int MAP1_DEFINITION_COUNT = 4;
    static final int MAP1_POPULATION = 91;
    static final String MAP1_SOURCE_SHA256 =
            "A364914917D54A2F3516CEA22FEBC0AD383692A088A170DBD7A1784200D56E29";
    static final String MAP1_RESOURCE_SHA256 =
            "A5898EBD859D81458070507F25378948016034775C138A1E21F988D4F5209CC7";
    static final int MAP2_DEFINITION_COUNT = 10;
    static final int MAP2_POPULATION = 150;
    static final String MAP2_SOURCE_SHA256 =
            "CC75FBEBEF8B645DA7247639DCDEF6AC93290C26264FFA64C039AA52E85E0B17";
    static final String MAP2_RESOURCE_SHA256 =
            "87ADBA6FDCCA2983E98CA45E78B0B0FA9FF1C8DD5876AF0E24CCFC20F537795D";
    static final int MAP14_DEFINITION_COUNT = 3;
    static final int MAP14_POPULATION = 55;
    static final String MAP14_SOURCE_SHA256 =
            "1A17196311984726381E01C2E8DFF6CF9EE46B73C5E30FB0E3309686EC4E3948";
    static final String MAP14_RESOURCE_SHA256 =
            "8006D9460BDF24A831A80871DD8D300764813E86716126193E94ECF6AF8CE28A";
    static final int MAP13_DEFINITION_COUNT = 4;
    static final int MAP13_POPULATION = 45;
    static final String MAP13_SOURCE_SHA256 =
            "36F775C2DDA0875483DEA00D0C8CDF02026B5186A83D7620FD5B4F5870EA9942";
    static final String MAP13_RESOURCE_SHA256 =
            "9ED81F88576AAEFEE346C5DE94C076527A0D1857FFFE524DB6402A357501DF87";
    static final int MAP237_DEFINITION_COUNT = 0;
    static final int MAP237_POPULATION = 0;
    static final String MAP237_SOURCE_SHA256 =
            "219255B3830F63B32C0D35542B91DF47B5532113719CB79075F5CD233B6DE5E7";
    static final String MAP237_RESOURCE_SHA256 =
            "729645E2ECDB86952C6261B879797B763CE027C481A6FF7E86DE96C8417E0EC3";
    static final int MAP3_DEFINITION_COUNT = 0;
    static final int MAP3_POPULATION = 0;
    static final String MAP3_SOURCE_SHA256 =
            "219255B3830F63B32C0D35542B91DF47B5532113719CB79075F5CD233B6DE5E7";
    static final String MAP3_RESOURCE_SHA256 =
            "DAA17582CFF9F36CA66CE7ED99CB3B8F55EDBF35A0D3E8645258DF26CCB82559";
    static final int MAP5_DEFINITION_COUNT = 0;
    static final int MAP5_POPULATION = 0;
    static final int MAP6_DEFINITION_COUNT = 0;
    static final int MAP6_POPULATION = 0;
    static final String MAP5_SOURCE_SHA256 =
            "219255B3830F63B32C0D35542B91DF47B5532113719CB79075F5CD233B6DE5E7";
    static final String MAP6_SOURCE_SHA256 = MAP5_SOURCE_SHA256;
    static final String MAP5_RESOURCE_SHA256 =
            "29BE1244E2A807791BE027B0B861B6BFB00169720339D0172D75652D1E8544A2";
    static final String MAP6_RESOURCE_SHA256 =
            "528280D3E577277F9CF4EC5629DE292080E25A2F7C45C4B93DCE68BD007D90F5";
    static final int MAP70_DEFINITION_COUNT = 66;
    static final int MAP70_POPULATION = 506;
    static final int MAP83_DEFINITION_COUNT = 0;
    static final int MAP83_POPULATION = 0;
    static final int MAP84_DEFINITION_COUNT = 0;
    static final int MAP84_POPULATION = 0;
    static final String MAP70_SOURCE_SHA256 =
            "3D5DFEF4ADC9E9308ADC09E8BA660569EC7CF79E8DD27BC4785493AA9EB2AA7B";
    static final String MAP83_SOURCE_SHA256 = MAP5_SOURCE_SHA256;
    static final String MAP84_SOURCE_SHA256 = MAP5_SOURCE_SHA256;
    static final String MAP70_RESOURCE_SHA256 =
            "FF2700E624B45D596586DAF44C40BA8E40EA4006A90627FAEF5E68CCC1A492E0";
    static final String MAP83_RESOURCE_SHA256 =
            "DC043CDBCAD95BBB73F00789AC3C651C31317C20183C3F9098D0B9A75DCAA271";
    static final String MAP84_RESOURCE_SHA256 =
            "E5B128765B44D52F3D458D1AC2BD81B0E7ADEFCA73C10B0F6F33B6C2716410BE";
    static final int MAP4_DEFINITION_COUNT = 729;
    static final int MAP4_POPULATION = 5_984;
    static final String MAP4_SOURCE_SHA256 =
            "E6F965D75163CDB64EF493A96A6023F272659AD8DFDA3C26027E20AA4C06FFAE";
    static final String MAP4_RESOURCE_SHA256 =
            "07DD697E621E42767964FB1A4A28DC6733ACCAC4FEEF0EA456ED10DED11F2359";
    static final int MAP16_DEFINITION_COUNT = 0;
    static final int MAP16_POPULATION = 0;
    static final String MAP16_SOURCE_SHA256 =
            "219255B3830F63B32C0D35542B91DF47B5532113719CB79075F5CD233B6DE5E7";
    static final String MAP16_RESOURCE_SHA256 =
            "526113A55BC362886007C0F8BDFC67C57F7BDC3636529ECF341CC1A72F385506";
    static final int MAP17_DEFINITION_COUNT = 0;
    static final int MAP17_POPULATION = 0;
    static final String MAP17_SOURCE_SHA256 =
            "219255B3830F63B32C0D35542B91DF47B5532113719CB79075F5CD233B6DE5E7";
    static final String MAP17_RESOURCE_SHA256 =
            "C426D7E23D4556843B25B4F791F88D8281A8A751C11EA27E3D28DC8C6447EBD3";
    static final int MAP22_DEFINITION_COUNT = 0;
    static final int MAP22_POPULATION = 0;
    static final String MAP22_SOURCE_SHA256 = MAP17_SOURCE_SHA256;
    static final String MAP22_RESOURCE_SHA256 =
            "B14BCBDE4F4854B17CD6418D0A9CFDC42B5137A62157D230E2FA65BD07540D6A";
    static final int MAP19_DEFINITION_COUNT = 1;
    static final int MAP19_POPULATION = 15;
    static final String MAP19_SOURCE_SHA256 =
            "FC4CC8542118153911E2EA0FFDF6A5E7DA84C064F2E2625297238327497EE368";
    static final String MAP19_RESOURCE_SHA256 =
            "75D8FD9FD595C69E674691E4FACAEB914B36F07E4FEF371EA52D967908492339";
    static final int MAP20_DEFINITION_COUNT = 5;
    static final int MAP20_POPULATION = 70;
    static final String MAP20_SOURCE_SHA256 =
            "1828621C92456B3ABE7CB3A1FAC2DF2C72E3020CEF6C2CD778B49DEE2DB4C3B2";
    static final String MAP20_RESOURCE_SHA256 =
            "9DCB6403A42A5DFE13B0CF2821ACD1F4F673BF424804DDF85F68D42B2C275DDF";
    static final int MAP21_DEFINITION_COUNT = 6;
    static final int MAP21_POPULATION = 30;
    static final String MAP21_SOURCE_SHA256 =
            "4E1C88A728B31E0D2E19A7D3DE78628F3EC7B7C420200037E226EA4F8B5E25D1";
    static final String MAP21_RESOURCE_SHA256 =
            "963D53FAAEF7CB361FEC267D44FD14279305BD9E8DBFA5BC4BDA19C56D4F5999";
    static final int MAP30_DEFINITION_COUNT = 6;
    static final int MAP30_POPULATION = 75;
    static final String MAP30_SOURCE_SHA256 =
            "E29A21F7B0904BEAEA3C386115F534746425FFCF3D8F0B63401D44D9847918AA";
    static final String MAP30_RESOURCE_SHA256 =
            "1DD1F0A9806C616269E5F1074CC55632389AD4C2FDA41D9CA226108D53EAB678";
    static final int MAP31_DEFINITION_COUNT = 6;
    static final int MAP31_POPULATION = 70;
    static final String MAP31_SOURCE_SHA256 =
            "C002A0DAC658C5108689DABB9240B29E4F6F3D4AD5B3841FDA73FBB5D731D184";
    static final String MAP31_RESOURCE_SHA256 =
            "DEB3B2E436B3A9C1BAC4F4445C4C8470DB8447B2E5D31B12906534CB8BA4C37A";
    static final int MAP32_DEFINITION_COUNT = 7;
    static final int MAP32_POPULATION = 70;
    static final String MAP32_SOURCE_SHA256 =
            "539BAE50740A583A1C4A9DF3D4EF26246CBDF9B55D3F5E519200555673593477";
    static final String MAP32_RESOURCE_SHA256 =
            "38F2629727F2FC7208EA31D26AD815F248EE040660E69882B93AEFC5C451AB5F";
    static final int MAP33_DEFINITION_COUNT = 8;
    static final int MAP33_POPULATION = 85;
    static final String MAP33_SOURCE_SHA256 =
            "5727D10EF98C65D5B094FFFF83478F4CE34C49931B44A57CB72E197ACB7FFAC3";
    static final String MAP33_RESOURCE_SHA256 =
            "D91A7C1E8E9800BEC428387713471234E25A0C8A9AC0EF16CFBE615B2AB58FF1";
    static final int MAP35_DEFINITION_COUNT = 5;
    static final int MAP35_POPULATION = 55;
    static final String MAP35_SOURCE_SHA256 =
            "6E4AEE6F67A7BB2CED702F11195E74F4083D21517E4A10500176B2CCADB59E3C";
    static final String MAP35_RESOURCE_SHA256 =
            "3EE4D685E7A92E2856FD6E8789BD328D4F89382A75769AE787D8567BF54B6913";
    static final int MAP36_DEFINITION_COUNT = 4;
    static final int MAP36_POPULATION = 60;
    static final String MAP36_SOURCE_SHA256 =
            "3A36E32CBE8F8495CD4BF0F33614823F47B79F96EAE870F948A8775D5BE85309";
    static final String MAP36_RESOURCE_SHA256 =
            "10DCA8690FC1F94D976490BA251F2A436AE70CBC5D0BB48547DCBF45EF6451D3";
    static final int MAP37_DEFINITION_COUNT = 1;
    static final int MAP37_POPULATION = 15;
    static final String MAP37_SOURCE_SHA256 =
            "079FE49FE0EF15ABD82BC6C032265CB18A12569F489396259FBB9671EE16D094";
    static final String MAP37_RESOURCE_SHA256 =
            "CC2866A9DC083D3744993B44ED25B3C8096B46294702F5AA8F388E2F6729FC70";
    static final int MAP67_DEFINITION_COUNT = 0;
    static final int MAP67_POPULATION = 0;
    static final String MAP67_SOURCE_SHA256 = MAP16_SOURCE_SHA256;
    static final String MAP67_RESOURCE_SHA256 =
            "33E87E551C9989C3777D32A522B5091B36453C6BC1298A08EE764FCA6A2DD2A2";
    static final int MAP209_DEFINITION_COUNT = 0;
    static final int MAP209_POPULATION = 0;
    static final String MAP209_SOURCE_SHA256 =
            "219255B3830F63B32C0D35542B91DF47B5532113719CB79075F5CD233B6DE5E7";
    static final String MAP209_RESOURCE_SHA256 =
            "935FC9901F6801B5E05EEB439B91DE31F4F4088BE43054D6B45B317FB229D3AB";
    static final int MAP213_DEFINITION_COUNT = 0;
    static final int MAP213_POPULATION = 0;
    static final String MAP213_SOURCE_SHA256 = MAP209_SOURCE_SHA256;
    static final String MAP213_RESOURCE_SHA256 =
            "1F00DBDCEEEF90A4A5F3431B3003EDE7D8E325328D47F36656F6AD09E75E3139";
    static final int MAP302_DEFINITION_COUNT = 0;
    static final int MAP302_POPULATION = 0;
    static final String MAP302_SOURCE_SHA256 = MAP209_SOURCE_SHA256;
    static final String MAP302_RESOURCE_SHA256 =
            "AC079ADBC48836A613182B26773B35612FA88192766F6D1B0E003A739C8F22DF";
    static final int MAP445_DEFINITION_COUNT = 0;
    static final int MAP445_POPULATION = 0;
    static final String MAP445_SOURCE_SHA256 = MAP209_SOURCE_SHA256;
    static final String MAP445_RESOURCE_SHA256 =
            "3214C7EC5584588DFEF1811D591CCFAE1C7218D35B3ECF6C9E4C9EDA665A3DDB";
    static final int MAP447_DEFINITION_COUNT = 0;
    static final int MAP447_POPULATION = 0;
    static final String MAP447_SOURCE_SHA256 = MAP209_SOURCE_SHA256;
    static final String MAP447_RESOURCE_SHA256 =
            "917366AE9B122F4F13FC40799499A2B8B1FE362D02FA6E32E0C80BED2E4AA9C3";
    static final int MAP303_DEFINITION_COUNT = 45;
    static final int MAP303_POPULATION = 829;
    static final String MAP303_SOURCE_SHA256 =
            "0C2A96AE5DD0EB7AC7D9687698CA941B3B7C795FB2138D4DFB07DDDE4C1307D4";
    static final String MAP303_RESOURCE_SHA256 =
            "C887DB70425287AB36FAEC7E6A561136D91C8EF4EB67BA635ABE304A8BE54418";
    static final int MAP244_DEFINITION_COUNT = 22;
    static final int MAP244_POPULATION = 400;
    static final String MAP244_SOURCE_SHA256 =
            "9F120F74FCD16CB6B541F25F2CDF66B98E49C746EA6858F758EA45C8C2B08111";
    static final String MAP244_RESOURCE_SHA256 =
            "516AC5A48755B3C78C9D233532B8A161DF417B6D9E8F40A621DA2AF86815F94E";
    static final int MAP666_DEFINITION_COUNT = 13;
    static final int MAP666_POPULATION = 480;
    static final String MAP666_SOURCE_SHA256 =
            "6F1EF4C73B00C64C8D6CEAF17D76BF6439BEB3EFE72A09AD83CE202963A3CF40";
    static final String MAP666_RESOURCE_SHA256 =
            "993BCAD5F804D9AFC1A795694BF6B7828B31059F95C0F6C35F3C3596B9DDDEB5";
    static final int ANT_CAVE_POPULATION = 492;
    private record SourceContract(
            int rows, int population, String sourceSha256, String resourceSha256) {}
    private static final Map<Integer, SourceContract> ANT_CAVE_SOURCE_CONTRACTS =
            Map.ofEntries(
                    Map.entry(43, new SourceContract(1, 30,
                            "D5D9F0B084FD1A10551CA69E6FCFBEBC07374A50D0E14CD1E9B5F496C0627664",
                            "E3D27B1FC27856BF87DA5C21FD26A9BCF76BF9E1E6B03EEC43BAA7A78FC33857")),
                    Map.entry(44, new SourceContract(1, 35,
                            "D21C07B13E8CDA36FB10457C82F6CD199A88654464B98C6627C49DB6865E4462",
                            "E52B378015F0ECCAF6C728F8923BDEE35D9E78E32D8A3F4833FA92D50EADC48D")),
                    Map.entry(45, new SourceContract(1, 35,
                            "FB80AA7A073E9FC4C33595997849530F32ECBCC204A34595016D1C28AE7440C8",
                            "688EBF1E3A39E3FE5B26A93A08A4EB9372EB2EBA9BB382F22D0B8E0868511A96")),
                    Map.entry(46, new SourceContract(1, 35,
                            "62459285572D34008B62C2FAE152A56B9B07D684E8CF23666D5FFDF623C998EA",
                            "AF29A38FAB7010A5F4BAEB62C7247E76D3F2AD749762759AEFE5FB736061C322")),
                    Map.entry(47, new SourceContract(1, 35,
                            "36DACB445C02FEC717CA2EE354E9602647AE2DCA620BBE47E27C03CBE1A4BE2C",
                            "8EB4E5D039F13F7BEDAE17BEE5E2907E0AC5F7EE9D16F5FA550444C20557CB25")),
                    Map.entry(48, new SourceContract(1, 35,
                            "A4B5AB0CE6E8F8126E29E23D74357F1758819643599B43DC131EEBCFE614C58D",
                            "9668FB30CD8C8E90A579034083FCA78D0672FA507B7804B786268E20D3AA664C")),
                    Map.entry(49, new SourceContract(6, 110,
                            "FF0C4547D0CCC6738CDF83D855C128AAC262933929EAAFF4A816FF04F206E544",
                            "EBD89EED6A84F212AC3DD6B4062D6FD60A58C4C13A219616C2B924315A59BC46")),
                    Map.entry(50, new SourceContract(6, 100,
                            "F8E3DC72DFC615F3A21E1E8D3D37A2127C6B257B36EB79394855CDE4CFB23F7D",
                            "BA4CD17AD97A3F829AD27335BFDC024D37ACCBD665E6DC2D9B3489304F4065AF")),
                    Map.entry(51, new SourceContract(4, 70,
                            "F4D9315E4DDE45A721CE246F0D9EC31C16E2E2E6A8C8D33FC1720C116B8EE779",
                            "2EA28ECAA939B8F3E836340D5D159B26FE46D8FD8E7BB13FB36162607D18A601")),
                    Map.entry(541, new SourceContract(1, 1,
                            "AA67F34BC6D4635D54350101606787D32D57E830FE92B8A2B9CA8392DAC2F3E4",
                            "E8C1373FF9B901DCF4DE63A4D26FE406C8B4EF66DB132A664070833978A79476")),
                    Map.entry(542, new SourceContract(1, 1,
                            "B11494DAE309F84B51BD20D597D24716987188973CDCE2540E65C495552D26BB",
                            "001AC7B746B57860E65695B2DB6A412DF9CA9E8AD6DAC42C203761A86FF11722")),
                    Map.entry(543, new SourceContract(5, 5,
                            "0E9953A8FA3C309EAC82A63036A8C03DFFB1E3C1A56B638A4072BFA9ED07129C",
                            "99B21CB2B507934B25A2D5113791CF66F01B59FC6EDDBA42D41761A54504FE19")));
    private static final Map<Integer, SourceContract> KNIGHT_CAVE_SOURCE_CONTRACTS =
            Map.of(
                    25, new SourceContract(10, 77,
                            "FA472B833C5A34D630510065867956B70A08F9D586E04D6A52CA41D587FD3C49",
                            "EF70652B7451FB6B978FCE7C27B130473D4867D363FBC12B58D1899491EFF041"),
                    26, new SourceContract(5, 65,
                            "35740211B24255EA7119100478BC2447F411C2C613526D7B84B965D4317F43E4",
                            "3FABF7319699DB19E3BE0B7C4844DE349C063F32421DDF9F6F55BFD3FDC1112B"),
                    27, new SourceContract(4, 85,
                            "C02CA5E75968591AD93BA764312A349D0641F504CBE02A36E05024D34B5DDAD8",
                            "F7EBE74B0F148B022634B759AECBAB24272A058B67CE0AAB027041DC772E203E"),
                    28, new SourceContract(4, 75,
                            "586E5FD9B8094D3EA560DDD08743DCE5A8272664FF834CE0EC5F14B6C61F46CC",
                            "2E5A259BA1AB854875172B1729825B7BBEC1D1B5FD62AEDB7A1CC84A7635B71E"));
    static final Map<Integer, SourceContract> GLUDIO_DUNGEON_SOURCE_CONTRACTS =
            Map.of(
                    7, new SourceContract(6, 53,
                            "29E9E6D5C2B0E36682BBD6D893A4D44BFA815899517E7F09D380B88A5B30D648",
                            "33F1428F8B06ED663F5CBDC9B58C46578B89E0230924A3A0C140B3914967F4A5"),
                    8, new SourceContract(7, 58,
                            "946C5484F2DA8F7D82F71B4AE63E0BC9F1760CFEBABE552B333E475FF1ACCB7A",
                            "16F75B5889D7CE59510633024B732E31D8EE889CFC0F15258A8B8C99677F78D2"),
                    9, new SourceContract(5, 50,
                            "7AB200CF93DA809D92A99491A07F99A237DCEE9C0A5AF191052BA569AD79E407",
                            "993D09012B53B20EF207B70D1E26A10AE7ADDE98F2175742452335F26DE198C2"),
                    10, new SourceContract(7, 73,
                            "13A18FDDDF53D81000EE3BAD6925418D9D3A0C8F18964EF9AA57A55D7C8FF4AA",
                            "75928EA09CDB3037F1AC74F0AFD1FB30F5085CF56873F92F9E742005D9306548"),
                    11, new SourceContract(7, 80,
                            "24BDFFEE9C4589370ED85756078F04F89C2A8DAC25F4F858E0B9625924A420A3",
                            "BCF71C6E48BA64385C647B85FE2815960E0D8615B6694B1067B3C60AB233A233"),
                    12, new SourceContract(7, 61,
                            "795D6A5F9AA39D07DFB46C0D9AB5E51411356F474161BAAA48477601164E3FA0",
                            "DA47CB0D6B0F6FB765A90DCFA5EBD44EB1CB732D31998B6ED578542BC5B6A265"));
    static final Map<Integer, SourceContract> GIRAN_DUNGEON_SOURCE_CONTRACTS =
            Map.of(
                    53, new SourceContract(4, 65,
                            "BD005246AC511DD7394D8243CA0A1030E07AE04FC90D209854D0044B957648A6",
                            "625AADDE9CBF035AB164C022DD3F58EDC9FEB6A883B97AEECD4B214B7F871A6C"),
                    54, new SourceContract(3, 60,
                            "189D6B5B09B82A2E162AE08077BD25863D3FC85D4ED67D64E4C1C042851A5670",
                            "AC5EB44B268B8BB6DBCFAF38E4DB08D106ADF6D088ADD465183E9C4275B1285E"),
                    55, new SourceContract(3, 60,
                            "79BC0D966446C1AD4D8652ADE8B3888995C464B29CCDCAF4FD0109D7ED845A79",
                            "ED9515304CA0FC7771D412667358B8BF2EE682C35CF5034D2BFE304B5D09A9CE"),
                    56, new SourceContract(4, 80,
                            "1736D58BE7710815DEA4432B466E848F0931E7F1AD38E95798F00E10F51A545C",
                            "DB7D840568D66DF75935D24530B403C0005ED042E4F3CB6D54FB5478DDFF5BC0"));
    static final SourceContract MAP52_SOURCE_CONTRACT = new SourceContract(
            0, 0,
            "219255B3830F63B32C0D35542B91DF47B5532113719CB79075F5CD233B6DE5E7",
            "45E9941B3B5AF39C2D5599B32B854D6E5546A248C7B89248443B5083443BE6A1");
    static final Map<Integer, SourceContract> WINDAWOOD_CASTLE_SOURCE_CONTRACTS =
            Map.of(
                    23, new SourceContract(9, 85,
                            "9F50DC9478845A4266BD265853E305051D5A13FB1124AA4257999B15A4E6275D",
                            "68A9C5D5E4D7F60C6C0F11D47EAC6D452CCA999DDFC225C5E796635248C5E220"),
                    24, new SourceContract(7, 80,
                            "048D743689C939E14572E719BBF2672AE21D7E847EAA392EEB04418EAFD1DBF5",
                            "66B877568A50BBD83066668FE71532E7C5B5C44319813E1997EFB1526EE86195"),
                    29, new SourceContract(0, 0,
                            "219255B3830F63B32C0D35542B91DF47B5532113719CB79075F5CD233B6DE5E7",
                            "106355EEE32C45201923871A4A0A010C4CE6134D4B4F0A3294F754BB1901BC63"));
    static final Map<Integer, SourceContract> KENT_COMPONENT_SOURCE_CONTRACTS =
            Map.of(
                    15, new SourceContract(0, 0,
                            "219255B3830F63B32C0D35542B91DF47B5532113719CB79075F5CD233B6DE5E7",
                            "734A91D7BB05A4309920F691C07F42618B2BA0EB3906964E3DC2D4F7EF6F9379"),
                    240, new SourceContract(4, 96,
                            "329764635C265C1538D697DEF740A27A883553585012D9A757B30B958DD7168E",
                            "F3BAE8D6FC787B96E11EE6F9F57518C9627D75B78883534013DBCC9D88EA46B2"),
                    241, new SourceContract(6, 96,
                            "3261B9109B379E53A714B32C847AF4789D3E3682BC79F409F4C95A0718234FDC",
                            "F97929DC92993D968821246E109071D7FB7AB90565E84EC9AB9AFCDB78A400BE"),
                    242, new SourceContract(10, 80,
                            "AE0431E3BA2E78BAC85EBB450166F7E245E690BDD597221A59410698A712DD89",
                            "04801523A5F618AEB77DABE6DDFC8ACAB64DF45D8D8AC6F5ECA5AE5924762A9D"),
                    243, new SourceContract(4, 65,
                            "97526031D76A5070E919211DC4E169279B0DD046175FF3F82326B7CF47D490DE",
                            "D4FB71C4218FE37786D7B3D98BC73854F7680657DD5B04CCC9770B345F842BDA"));
    static final Map<Integer, ClassicWorldIvoryTowerCatalog.SourceContract>
            IVORY_TOWER_SOURCE_CONTRACTS =
                    ClassicWorldIvoryTowerCatalog.MONSTER_CONTRACTS;
    static final SourceContract MAP66_SOURCE_CONTRACT = new SourceContract(
            0, 0,
            ClassicWorldDwarfCastleCatalog.MONSTER_SOURCE_SHA256,
            ClassicWorldDwarfCastleCatalog.MONSTER_RESOURCE_SHA256);
    static final int KNIGHT_CAVE_POPULATION = 302;
    static final int GLUDIO_DUNGEON_POPULATION = 375;
    static final int GIRAN_DUNGEON_POPULATION = 265;
    static final int WINDAWOOD_CASTLE_POPULATION = 165;
    static final int KENT_COMPONENT_POPULATION = 337;
    static final int IVORY_TOWER_POPULATION =
            ClassicWorldIvoryTowerCatalog.MONSTER_POPULATION;
    static final int ADEN_TOI_POPULATION = 625;
    static final int HEINE_EVA_POPULATION = 779;
    static final int SILENT_CAVERN_POPULATION = 370;
    static final int RASTABAD_OUM_POPULATION = 721;
    static final int DIADRAD_SERVICE_CORE_POPULATION = 1_375;
    static final int DEMON_TEMPLE_POPULATION = 54;
    static final int ELEMENTAL_GRAVE_SLICE_POPULATION = 701;
    static final int PIRATE_DUNGEON_POPULATION = 251;
    static final int PIRATE_BACK_HALF_POPULATION = 772;
    static final int DESIRE_CAVE_POPULATION = 583;
    static final int LASTABAD_GATE_POPULATION = 0;
    static final int PRE_332_SELECTED_TOTAL_POPULATION =
            MAP0_POPULATION + MAP4_POPULATION + MAP70_POPULATION
                    + MAP19_POPULATION + MAP20_POPULATION
                    + MAP21_POPULATION + MAP30_POPULATION
                    + MAP31_POPULATION + MAP32_POPULATION
                    + MAP33_POPULATION + MAP35_POPULATION
                    + MAP36_POPULATION + MAP37_POPULATION
                    + MAP303_POPULATION + MAP244_POPULATION + ANT_CAVE_POPULATION
                    + KNIGHT_CAVE_POPULATION;
    static final int PRE_333_SELECTED_TOTAL_POPULATION =
            PRE_332_SELECTED_TOTAL_POPULATION + MAP1_POPULATION + MAP2_POPULATION
                    + MAP14_POPULATION;
    static final int PRE_335_SELECTED_TOTAL_POPULATION =
            PRE_333_SELECTED_TOTAL_POPULATION + MAP13_POPULATION;
    static final int PRE_333_TOTAL_POPULATION = PRE_333_SELECTED_TOTAL_POPULATION
            + ClassicWorldRoyalTrialCatalog.BLACK_KNIGHT_POPULATION
            + ClassicWorldRoyalTrialCatalog.STONE_GOLEM_POPULATION;
    static final int PRE_335_TOTAL_POPULATION = PRE_335_SELECTED_TOTAL_POPULATION
            + ClassicWorldRoyalTrialCatalog.BLACK_KNIGHT_POPULATION
            + ClassicWorldRoyalTrialCatalog.STONE_GOLEM_POPULATION;
    static final int PRE_336_SELECTED_TOTAL_POPULATION =
            PRE_335_SELECTED_TOTAL_POPULATION + GLUDIO_DUNGEON_POPULATION;
    static final int PRE_336_TOTAL_POPULATION =
            PRE_335_TOTAL_POPULATION + GLUDIO_DUNGEON_POPULATION;
    static final int PRE_378_SELECTED_TOTAL_POPULATION =
            PRE_336_SELECTED_TOTAL_POPULATION + GIRAN_DUNGEON_POPULATION
                    + WINDAWOOD_CASTLE_POPULATION + KENT_COMPONENT_POPULATION
                    + IVORY_TOWER_POPULATION + ADEN_TOI_POPULATION
                    + HEINE_EVA_POPULATION + SILENT_CAVERN_POPULATION
                    + RASTABAD_OUM_POPULATION + DIADRAD_SERVICE_CORE_POPULATION
                    + DEMON_TEMPLE_POPULATION + ELEMENTAL_GRAVE_SLICE_POPULATION
                    + PIRATE_DUNGEON_POPULATION;
    static final int PRE_378_TOTAL_POPULATION =
            PRE_336_TOTAL_POPULATION + GIRAN_DUNGEON_POPULATION
                    + WINDAWOOD_CASTLE_POPULATION + KENT_COMPONENT_POPULATION
                    + IVORY_TOWER_POPULATION + ADEN_TOI_POPULATION
                    + HEINE_EVA_POPULATION + SILENT_CAVERN_POPULATION
                    + RASTABAD_OUM_POPULATION + DIADRAD_SERVICE_CORE_POPULATION
                    + DEMON_TEMPLE_POPULATION + ELEMENTAL_GRAVE_SLICE_POPULATION
                    + PIRATE_DUNGEON_POPULATION;
    static final int SELECTED_TOTAL_POPULATION =
            PRE_378_SELECTED_TOTAL_POPULATION + PIRATE_BACK_HALF_POPULATION
                    + DESIRE_CAVE_POPULATION + MAP666_POPULATION;
    static final int TOTAL_POPULATION =
            PRE_378_TOTAL_POPULATION + PIRATE_BACK_HALF_POPULATION
                    + DESIRE_CAVE_POPULATION + MAP666_POPULATION;

    private static final int MAGIC = 0x4C574D31; // LWM1
    private static final String TSV_HEADER = String.join("\t",
            "spawn_id", "location", "count", "npc_id", "group_id",
            "locx", "locy", "randomx", "randomy",
            "locx1", "locy1", "locx2", "locy2", "heading",
            "min_respawn", "max_respawn", "respawn_screen", "movement_distance",
            "rest", "near_spawn", "name_id", "impl", "gfx_id", "level", "hp",
            "mp", "ac", "str", "con", "dex", "wis", "intel", "mr", "exp",
            "lawful", "size", "weak_attr", "ranged", "passispeed", "atkspeed",
            "bow_act_id",
            "undead", "poison_atk", "paralysis_atk", "agro", "damage_reduction",
            "hard", "doppel", "boss", "light_size", "change_head");
    private static final Map<Integer, List<SpawnDefinition>> BY_MAP = Map.ofEntries(
            Map.entry(ClassicWorldMapCatalog.TALKING_ISLAND_MAP_ID,
                    load("/world/map0-monsters.lwm.gz")),
            Map.entry(ClassicWorldMapCatalog.ROYAL_TRIAL_CAVE_FIRST_MAP_ID,
                    load("/world/map1-monsters.lwm.gz")),
            Map.entry(ClassicWorldMapCatalog.ROYAL_TRIAL_CAVE_SECOND_MAP_ID,
                    load("/world/map2-monsters.lwm.gz")),
            Map.entry(ClassicWorldMapCatalog.UNDERSEA_TUNNEL_MAP_ID,
                    load("/world/map14-monsters.lwm.gz")),
            Map.entry(ClassicWorldMapCatalog.UNDERSEA_TUNNEL_SEVENTH_FLOOR_MAP_ID,
                    load("/world/map13-monsters.lwm.gz")),
            Map.entry(ClassicWorldMapCatalog.JIM_SECRET_ROOM_MAP_ID,
                    load("/world/map237-monsters.lwm.gz")),
            Map.entry(ClassicWorldMapCatalog.GUNTER_HOUSE_MAP_ID,
                    load("/world/map3-monsters.lwm.gz")),
            Map.entry(ClassicWorldMapCatalog.TALKING_ISLAND_TO_GLUDIO_SHIP_MAP_ID,
                    load("/world/map5-monsters.lwm.gz")),
            Map.entry(ClassicWorldMapCatalog.GLUDIO_TO_TALKING_ISLAND_SHIP_MAP_ID,
                    load("/world/map6-monsters.lwm.gz")),
            Map.entry(ClassicWorldMapCatalog.FORGOTTEN_ISLAND_MAP_ID,
                    load("/world/map70-monsters.lwm.gz")),
            Map.entry(ClassicWorldMapCatalog.MAINLAND_TO_FORGOTTEN_ISLAND_SHIP_MAP_ID,
                    load("/world/map83-monsters.lwm.gz")),
            Map.entry(ClassicWorldMapCatalog.FORGOTTEN_ISLAND_TO_MAINLAND_SHIP_MAP_ID,
                    load("/world/map84-monsters.lwm.gz")),
            Map.entry(ClassicWorldMapCatalog.WORLD_TREE_MAP_ID,
                    load("/world/map4-monsters.lwm.gz")),
            Map.entry(ClassicWorldMapCatalog.HARTIN_LABORATORY_MAP_ID,
                    load("/world/map16-monsters.lwm.gz")),
            Map.entry(ClassicWorldMapCatalog.NERUPA_CAVE_MAP_ID,
                    load("/world/map17-monsters.lwm.gz")),
            Map.entry(ClassicWorldMapCatalog.GERARD_TRIAL_DUNGEON_MAP_ID,
                    load("/world/map22-monsters.lwm.gz")),
            Map.entry(25, load("/world/map25-monsters.lwm.gz")),
            Map.entry(26, load("/world/map26-monsters.lwm.gz")),
            Map.entry(27, load("/world/map27-monsters.lwm.gz")),
            Map.entry(28, load("/world/map28-monsters.lwm.gz")),
            Map.entry(43, load("/world/map43-monsters.lwm.gz")),
            Map.entry(44, load("/world/map44-monsters.lwm.gz")),
            Map.entry(45, load("/world/map45-monsters.lwm.gz")),
            Map.entry(46, load("/world/map46-monsters.lwm.gz")),
            Map.entry(47, load("/world/map47-monsters.lwm.gz")),
            Map.entry(48, load("/world/map48-monsters.lwm.gz")),
            Map.entry(49, load("/world/map49-monsters.lwm.gz")),
            Map.entry(50, load("/world/map50-monsters.lwm.gz")),
            Map.entry(51, load("/world/map51-monsters.lwm.gz")),
            Map.entry(541, load("/world/map541-monsters.lwm.gz")),
            Map.entry(542, load("/world/map542-monsters.lwm.gz")),
            Map.entry(543, load("/world/map543-monsters.lwm.gz")),
            Map.entry(ClassicWorldMapCatalog.SLEEPING_DRAGON_FIRST_FLOOR_MAP_ID,
                    load("/world/map19-monsters.lwm.gz")),
            Map.entry(ClassicWorldMapCatalog.SLEEPING_DRAGON_SECOND_FLOOR_MAP_ID,
                    load("/world/map20-monsters.lwm.gz")),
            Map.entry(ClassicWorldMapCatalog.ORC_ELVEN_DUNGEON_MAP_ID,
                    load("/world/map21-monsters.lwm.gz")),
            Map.entry(ClassicWorldMapCatalog.DRAGON_VALLEY_DUNGEON_FIRST_FLOOR_MAP_ID,
                    load("/world/map30-monsters.lwm.gz")),
            Map.entry(ClassicWorldMapCatalog.DRAGON_VALLEY_DUNGEON_SECOND_FLOOR_MAP_ID,
                    load("/world/map31-monsters.lwm.gz")),
            Map.entry(ClassicWorldMapCatalog.DRAGON_VALLEY_DUNGEON_THIRD_FLOOR_MAP_ID,
                    load("/world/map32-monsters.lwm.gz")),
            Map.entry(ClassicWorldMapCatalog.DRAGON_VALLEY_DUNGEON_FOURTH_FLOOR_MAP_ID,
                    load("/world/map33-monsters.lwm.gz")),
            Map.entry(ClassicWorldMapCatalog.DRAGON_VALLEY_DUNGEON_FIFTH_FLOOR_MAP_ID,
                    load("/world/map35-monsters.lwm.gz")),
            Map.entry(ClassicWorldMapCatalog.DRAGON_VALLEY_DUNGEON_SIXTH_FLOOR_MAP_ID,
                    load("/world/map36-monsters.lwm.gz")),
            Map.entry(ClassicWorldMapCatalog.ANTHARAS_LAIR_MAP_ID,
                    load("/world/map37-monsters.lwm.gz")),
            Map.entry(ClassicWorldMapCatalog.VALAKAS_HABITAT_MAP_ID,
                    load("/world/map67-monsters.lwm.gz")),
            Map.entry(ClassicWorldMapCatalog.DARK_ELF_DUNGEON_MAP_ID,
                    load("/world/map209-monsters.lwm.gz")),
            Map.entry(ClassicWorldMapCatalog.DARKMAR_DUNGEON_MAP_ID,
                    load("/world/map213-monsters.lwm.gz")),
            Map.entry(ClassicWorldMapCatalog.SEPIA_DUNGEON_MAP_ID,
                    load("/world/map302-monsters.lwm.gz")),
            Map.entry(ClassicWorldMapCatalog.TAINTED_LAND_MAP_ID,
                    load("/world/map244-monsters.lwm.gz")),
            Map.entry(ClassicWorldMapCatalog.HIDDEN_DOCK_MAP_ID,
                    load("/world/map445-monsters.lwm.gz")),
            Map.entry(ClassicWorldMapCatalog.PIRATE_ISLAND_SHIP_MAP_ID,
                    load("/world/map447-monsters.lwm.gz")),
            Map.entry(ClassicWorldMapCatalog.PHANTASM_ISLAND_MAP_ID,
                    load("/world/map303-monsters.lwm.gz")),
            Map.entry(7, load("/world/map7-monsters.lwm.gz")),
            Map.entry(8, load("/world/map8-monsters.lwm.gz")),
            Map.entry(9, load("/world/map9-monsters.lwm.gz")),
            Map.entry(10, load("/world/map10-monsters.lwm.gz")),
            Map.entry(11, load("/world/map11-monsters.lwm.gz")),
            Map.entry(12, load("/world/map12-monsters.lwm.gz")),
            Map.entry(53, load("/world/map53-monsters.lwm.gz")),
            Map.entry(54, load("/world/map54-monsters.lwm.gz")),
            Map.entry(55, load("/world/map55-monsters.lwm.gz")),
            Map.entry(56, load("/world/map56-monsters.lwm.gz")),
            Map.entry(ClassicWorldMapCatalog.GIRAN_CASTLE_MAP_ID,
                    load("/world/map52-monsters.lwm.gz")),
            Map.entry(23, load("/world/map23-monsters.lwm.gz")),
            Map.entry(24, load("/world/map24-monsters.lwm.gz")),
            Map.entry(29, load("/world/map29-monsters.lwm.gz")),
            Map.entry(15, load("/world/map15-monsters.lwm.gz")),
            Map.entry(240, load("/world/map240-monsters.lwm.gz")),
            Map.entry(241, load("/world/map241-monsters.lwm.gz")),
            Map.entry(242, load("/world/map242-monsters.lwm.gz")),
            Map.entry(243, load("/world/map243-monsters.lwm.gz")),
            Map.entry(75, load("/world/map75-monsters.lwm.gz")),
            Map.entry(76, load("/world/map76-monsters.lwm.gz")),
            Map.entry(77, load("/world/map77-monsters.lwm.gz")),
            Map.entry(78, load("/world/map78-monsters.lwm.gz")),
            Map.entry(79, load("/world/map79-monsters.lwm.gz")),
            Map.entry(80, load("/world/map80-monsters.lwm.gz")),
            Map.entry(81, load("/world/map81-monsters.lwm.gz")),
            Map.entry(82, load("/world/map82-monsters.lwm.gz")),
            Map.entry(ClassicWorldDwarfCastleCatalog.MAP_ID,
                    load("/world/map66-monsters.lwm.gz")),
            Map.entry(101, load("/world/map101-monsters.lwm.gz")),
            Map.entry(102, load("/world/map102-monsters.lwm.gz")),
            Map.entry(103, load("/world/map103-monsters.lwm.gz")),
            Map.entry(104, load("/world/map104-monsters.lwm.gz")),
            Map.entry(105, load("/world/map105-monsters.lwm.gz")),
            Map.entry(106, load("/world/map106-monsters.lwm.gz")),
            Map.entry(107, load("/world/map107-monsters.lwm.gz")),
            Map.entry(108, load("/world/map108-monsters.lwm.gz")),
            Map.entry(109, load("/world/map109-monsters.lwm.gz")),
            Map.entry(110, load("/world/map110-monsters.lwm.gz")),
            Map.entry(300, load("/world/map300-monsters.lwm.gz")),
            Map.entry(301, load("/world/map301-monsters.lwm.gz")),
            Map.entry(59, load("/world/map59-monsters.lwm.gz")),
            Map.entry(60, load("/world/map60-monsters.lwm.gz")),
            Map.entry(61, load("/world/map61-monsters.lwm.gz")),
            Map.entry(62, load("/world/map62-monsters.lwm.gz")),
            Map.entry(63, load("/world/map63-monsters.lwm.gz")),
            Map.entry(64, load("/world/map64-monsters.lwm.gz")),
            Map.entry(65, load("/world/map65-monsters.lwm.gz")),
            Map.entry(252, load("/world/map252-monsters.lwm.gz")),
            Map.entry(253, load("/world/map253-monsters.lwm.gz")),
            Map.entry(254, load("/world/map254-monsters.lwm.gz")),
            Map.entry(304, load("/world/map304-monsters.lwm.gz")),
            Map.entry(305, load("/world/map305-monsters.lwm.gz")),
            Map.entry(307, load("/world/map307-monsters.lwm.gz")),
            Map.entry(308, load("/world/map308-monsters.lwm.gz")),
            Map.entry(309, load("/world/map309-monsters.lwm.gz")),
            Map.entry(310, load("/world/map310-monsters.lwm.gz")),
            Map.entry(320, load("/world/map320-monsters.lwm.gz")),
            Map.entry(330, load("/world/map330-monsters.lwm.gz")),
            Map.entry(400, load("/world/map400-monsters.lwm.gz")),
            Map.entry(401, load("/world/map401-monsters.lwm.gz")),
            Map.entry(ClassicWorldDemonTempleCatalog.MAP_ID,
                    load("/world/map410-monsters.lwm.gz")),
            Map.entry(ClassicWorldElementalGraveCatalog.ABYSS_LAKE_MAP_ID,
                    load("/world/map420-monsters.lwm.gz")),
            Map.entry(ClassicWorldElementalGraveCatalog.ELEMENTAL_GRAVE_MAP_ID,
                    load("/world/map430-monsters.lwm.gz")),
            Map.entry(ClassicWorldElementalGraveCatalog.PIRATE_ISLAND_MAP_ID,
                    load("/world/map440-monsters.lwm.gz")),
            Map.entry(441, load("/world/map441-monsters.lwm.gz")),
            Map.entry(442, load("/world/map442-monsters.lwm.gz")),
            Map.entry(443, load("/world/map443-monsters.lwm.gz")),
            Map.entry(444, load("/world/map444-monsters.lwm.gz")),
            Map.entry(480, load("/world/map480-monsters.lwm.gz")),
            Map.entry(481, load("/world/map481-monsters.lwm.gz")),
            Map.entry(482, load("/world/map482-monsters.lwm.gz")),
            Map.entry(483, load("/world/map483-monsters.lwm.gz")),
            Map.entry(484, load("/world/map484-monsters.lwm.gz")),
            Map.entry(550, load("/world/map550-monsters.lwm.gz")),
            Map.entry(551, load("/world/map551-monsters.lwm.gz")),
            Map.entry(552, load("/world/map552-monsters.lwm.gz")),
            Map.entry(553, load("/world/map553-monsters.lwm.gz")),
            Map.entry(554, load("/world/map554-monsters.lwm.gz")),
            Map.entry(555, load("/world/map555-monsters.lwm.gz")),
            Map.entry(556, load("/world/map556-monsters.lwm.gz")),
            Map.entry(557, load("/world/map557-monsters.lwm.gz")),
            Map.entry(558, load("/world/map558-monsters.lwm.gz")),
            Map.entry(600, load("/world/map600-monsters.lwm.gz")),
            Map.entry(601, load("/world/map601-monsters.lwm.gz")),
            Map.entry(602, load("/world/map602-monsters.lwm.gz")),
            Map.entry(603, load("/world/map603-monsters.lwm.gz")),
            Map.entry(604, load("/world/map604-monsters.lwm.gz")),
            Map.entry(605, load("/world/map605-monsters.lwm.gz")),
            Map.entry(606, load("/world/map606-monsters.lwm.gz")),
            Map.entry(607, load("/world/map607-monsters.lwm.gz")),
            Map.entry(778, load("/world/map778-monsters.lwm.gz")),
            Map.entry(779, load("/world/map779-monsters.lwm.gz")),
            Map.entry(ClassicWorldLastabadGateCatalog.MAP_ID,
                    load("/world/map450-monsters.lwm.gz")),
            Map.entry(1000, load("/world/map1000-monsters.lwm.gz")),
            Map.entry(1001, load("/world/map1001-monsters.lwm.gz")),
            Map.entry(ClassicWorldHell.MAP_ID,
                    load("/world/map666-monsters.lwm.gz")));

    record MonsterTemplate(
            int npcId, String nameId, String implementation, int gfxId,
            int level, int hp, int mp, int armorClass,
            int strength, int constitution, int dexterity, int wisdom,
            int intelligence, int magicResistance, long experience, int lawful,
            String size, int weakAttribute, int ranged,
            int passispeed, int atkspeed, int bowActionId, int undead,
            int poisonAttack, int paralysisAttack, boolean aggressive,
            int damageReduction, boolean hard, boolean doppel, boolean boss,
            int lightSize, boolean changeHead) {
        MonsterTemplate {
            if (npcId <= 0 || nameId == null || implementation == null
                    || (!implementation.equals("L1Monster")
                        && !implementation.equals("L1Guardian")
                        && !implementation.equals("L1Guard"))
                    || gfxId <= 0 || gfxId > 0xFFFF || level <= 0 || level > 0xFF
                    || hp <= 0 || mp < 0 || experience < 0 || experience > 0xFFFF_FFFFL
                    || lawful < Short.MIN_VALUE || lawful > Short.MAX_VALUE
                    || size == null || (!size.equals("small") && !size.equals("large"))
                    || ranged < 0 || passispeed < 0 || atkspeed < 0
                    || bowActionId < 0 || bowActionId > 0xFFFF
                    || undead < 0 || poisonAttack < 0 || paralysisAttack < 0
                    || damageReduction < 0 || lightSize < 0 || lightSize > 0xFF) {
                throw new IllegalArgumentException("invalid classic monster template " + npcId);
            }
        }

        boolean large() {
            return size.equals("large");
        }

        boolean guardian() {
            return implementation.equals("L1Guardian");
        }

        boolean guard() {
            return implementation.equals("L1Guard");
        }

        int attackRange() {
            return ranged <= 0 ? 1 : ranged;
        }

        boolean projectileAtDistance(int chebyshevDistance) {
            return chebyshevDistance > 1 && bowActionId > 0;
        }

        int projectileAttackAction() {
            return switch (gfxId) {
                case 3860 -> 21;
                case 2716 -> 19;
                default -> 1;
            };
        }
    }

    record SpawnDefinition(
            int mapId, int spawnId, String location, int count, int groupId,
            int locX, int locY, int randomX, int randomY,
            int locX1, int locY1, int locX2, int locY2, int heading,
            int minRespawnSeconds, int maxRespawnSeconds,
            boolean respawnScreen, int movementDistance, boolean rest,
            int nearSpawn, MonsterTemplate template) {
        SpawnDefinition {
            if (mapId < 0 || spawnId <= 0 || location == null || count <= 0
                    || groupId < 0 || locX < 0 || locY < 0
                    || randomX < 0 || randomY < 0
                    || locX1 < 0 || locY1 < 0 || locX2 < 0 || locY2 < 0
                    || heading < 0 || heading > 7
                    || minRespawnSeconds < 0 || maxRespawnSeconds < 0
                    || movementDistance < 0 || nearSpawn < 0 || template == null) {
                throw new IllegalArgumentException("invalid classic monster spawn " + spawnId);
            }
        }

        boolean sourceAreaSpawn() {
            return locX1 != 0 && locY1 != 0 && locX2 != 0 && locY2 != 0;
        }

        boolean expandedCountAreaSpawn() {
            return count > 1 && locX1 == 0;
        }

        boolean randomSpawn() {
            return randomX != 0 || randomY != 0;
        }

        int areaMinX() {
            if (sourceAreaSpawn()) return Math.min(locX1, locX2);
            return locX - Math.min(count * 6, 30);
        }

        int areaMaxX() {
            if (sourceAreaSpawn()) return Math.max(locX1, locX2);
            return locX + Math.min(count * 6, 30);
        }

        int areaMinY() {
            if (sourceAreaSpawn()) return Math.min(locY1, locY2);
            return locY - Math.min(count * 6, 30);
        }

        int areaMaxY() {
            if (sourceAreaSpawn()) return Math.max(locY1, locY2);
            return locY + Math.min(count * 6, 30);
        }

        boolean areaSpawn() {
            return sourceAreaSpawn() || expandedCountAreaSpawn();
        }
    }

    private ClassicWorldMonsterCatalog() {}

    static List<SpawnDefinition> definitions(int mapId) {
        return BY_MAP.getOrDefault(mapId, List.of());
    }

    static SpawnDefinition definition(int mapId, int spawnId) {
        return definitions(mapId).stream()
                .filter(definition -> definition.spawnId() == spawnId)
                .findFirst().orElse(null);
    }

    private static List<SpawnDefinition> load(String resourceName) {
        byte[] packed;
        try (InputStream resource = ClassicWorldMonsterCatalog.class
                .getResourceAsStream(resourceName)) {
            if (resource == null) {
                throw new IOException("missing classic monster resource " + resourceName);
            }
            packed = resource.readAllBytes();
        } catch (IOException error) {
            throw new ExceptionInInitializerError(error);
        }
        requireResourceHash(resourceName, packed);

        try (DataInputStream in = new DataInputStream(new GZIPInputStream(
                new ByteArrayInputStream(packed)))) {
            if (in.readInt() != MAGIC) throw new IOException("invalid monster resource magic");
            int mapId = in.readUnsignedShort();
            int expectedRows = in.readInt();
            int expectedPopulation = in.readInt();
            byte[] sourceHash = in.readNBytes(32);
            if (sourceHash.length != 32) throw new IOException("truncated monster source hash");
            byte[] table = in.readAllBytes();
            String observedSource = sha256(table);
            String headerSource = HexFormat.of().withUpperCase().formatHex(sourceHash);
            if (!observedSource.equals(headerSource)) {
                throw new IOException("monster source hash does not match table");
            }
            ClassicWorldDesireCaveCatalog.ResourceContract desireCave =
                    ClassicWorldDesireCaveCatalog.MONSTER_CONTRACTS.get(mapId);
            if (desireCave != null
                    && (!desireCave.sourceSha256().equals(headerSource)
                        || expectedRows != desireCave.rows()
                        || expectedPopulation != desireCave.secondaryCount())) {
                throw new IOException(
                        "Desire-Cave monster source contract changed " + mapId);
            }
            ClassicWorldPirateBackHalfCatalog.ComponentContract pirateBackHalf =
                    ClassicWorldPirateBackHalfCatalog.MONSTER_CONTRACTS.get(mapId);
            if (pirateBackHalf != null
                    && (!pirateBackHalf.sourceSha256().equals(headerSource)
                        || expectedRows != pirateBackHalf.rows()
                        || expectedPopulation != pirateBackHalf.secondaryCount())) {
                throw new IOException(
                        "Pirate-back-half monster source contract changed " + mapId);
            }
            ClassicWorldBehemothSilveriaCatalog.ResourceContract homeTown =
                    ClassicWorldBehemothSilveriaCatalog.MONSTER_CONTRACTS.get(mapId);
            if (homeTown != null
                    && (!homeTown.sourceSha256().equals(headerSource)
                        || expectedRows != homeTown.rows()
                        || expectedPopulation != homeTown.population())) {
                throw new IOException(
                        "home-town monster source contract changed " + mapId);
            }
            if (mapId == ClassicWorldMapCatalog.WORLD_TREE_MAP_ID
                    && (!MAP4_SOURCE_SHA256.equals(headerSource)
                        || expectedRows != MAP4_DEFINITION_COUNT
                        || expectedPopulation != MAP4_POPULATION)) {
                throw new IOException("map4 monster source contract changed");
            }
            if (mapId == ClassicWorldMapCatalog.TALKING_ISLAND_MAP_ID
                    && (!MAP0_SOURCE_SHA256.equals(headerSource)
                        || expectedRows != MAP0_DEFINITION_COUNT
                        || expectedPopulation != MAP0_POPULATION)) {
                throw new IOException("map0 monster source contract changed");
            }
            if (mapId == ClassicWorldMapCatalog.ROYAL_TRIAL_CAVE_FIRST_MAP_ID
                    && (!MAP1_SOURCE_SHA256.equals(headerSource)
                        || expectedRows != MAP1_DEFINITION_COUNT
                        || expectedPopulation != MAP1_POPULATION)) {
                throw new IOException("map1 monster source contract changed");
            }
            if (mapId == ClassicWorldMapCatalog.ROYAL_TRIAL_CAVE_SECOND_MAP_ID
                    && (!MAP2_SOURCE_SHA256.equals(headerSource)
                        || expectedRows != MAP2_DEFINITION_COUNT
                        || expectedPopulation != MAP2_POPULATION)) {
                throw new IOException("map2 monster source contract changed");
            }
            if (mapId == ClassicWorldMapCatalog.UNDERSEA_TUNNEL_MAP_ID
                    && (!MAP14_SOURCE_SHA256.equals(headerSource)
                        || expectedRows != MAP14_DEFINITION_COUNT
                        || expectedPopulation != MAP14_POPULATION)) {
                throw new IOException("map14 monster source contract changed");
            }
            if (mapId == ClassicWorldMapCatalog.UNDERSEA_TUNNEL_SEVENTH_FLOOR_MAP_ID
                    && (!MAP13_SOURCE_SHA256.equals(headerSource)
                        || expectedRows != MAP13_DEFINITION_COUNT
                        || expectedPopulation != MAP13_POPULATION)) {
                throw new IOException("map13 monster source contract changed");
            }
            if (mapId == ClassicWorldMapCatalog.JIM_SECRET_ROOM_MAP_ID
                    && (!MAP237_SOURCE_SHA256.equals(headerSource)
                        || expectedRows != MAP237_DEFINITION_COUNT
                        || expectedPopulation != MAP237_POPULATION)) {
                throw new IOException("map237 monster source contract changed");
            }
            if (mapId == ClassicWorldMapCatalog.GUNTER_HOUSE_MAP_ID
                    && (!MAP3_SOURCE_SHA256.equals(headerSource)
                        || expectedRows != MAP3_DEFINITION_COUNT
                        || expectedPopulation != MAP3_POPULATION)) {
                throw new IOException("map3 monster source contract changed");
            }
            if (mapId == ClassicWorldMapCatalog.TALKING_ISLAND_TO_GLUDIO_SHIP_MAP_ID
                    && (!MAP5_SOURCE_SHA256.equals(headerSource)
                        || expectedRows != MAP5_DEFINITION_COUNT
                        || expectedPopulation != MAP5_POPULATION)) {
                throw new IOException("map5 monster source contract changed");
            }
            if (mapId == ClassicWorldMapCatalog.GLUDIO_TO_TALKING_ISLAND_SHIP_MAP_ID
                    && (!MAP6_SOURCE_SHA256.equals(headerSource)
                        || expectedRows != MAP6_DEFINITION_COUNT
                        || expectedPopulation != MAP6_POPULATION)) {
                throw new IOException("map6 monster source contract changed");
            }
            if (mapId == ClassicWorldMapCatalog.FORGOTTEN_ISLAND_MAP_ID
                    && (!MAP70_SOURCE_SHA256.equals(headerSource)
                        || expectedRows != MAP70_DEFINITION_COUNT
                        || expectedPopulation != MAP70_POPULATION)) {
                throw new IOException("map70 monster source contract changed");
            }
            if (mapId == ClassicWorldMapCatalog.MAINLAND_TO_FORGOTTEN_ISLAND_SHIP_MAP_ID
                    && (!MAP83_SOURCE_SHA256.equals(headerSource)
                        || expectedRows != MAP83_DEFINITION_COUNT
                        || expectedPopulation != MAP83_POPULATION)) {
                throw new IOException("map83 monster source contract changed");
            }
            if (mapId == ClassicWorldMapCatalog.FORGOTTEN_ISLAND_TO_MAINLAND_SHIP_MAP_ID
                    && (!MAP84_SOURCE_SHA256.equals(headerSource)
                        || expectedRows != MAP84_DEFINITION_COUNT
                        || expectedPopulation != MAP84_POPULATION)) {
                throw new IOException("map84 monster source contract changed");
            }
            if (mapId == ClassicWorldMapCatalog.HARTIN_LABORATORY_MAP_ID
                    && (!MAP16_SOURCE_SHA256.equals(headerSource)
                        || expectedRows != MAP16_DEFINITION_COUNT
                        || expectedPopulation != MAP16_POPULATION)) {
                throw new IOException("map16 monster source contract changed");
            }
            if (mapId == ClassicWorldMapCatalog.NERUPA_CAVE_MAP_ID
                    && (!MAP17_SOURCE_SHA256.equals(headerSource)
                        || expectedRows != MAP17_DEFINITION_COUNT
                        || expectedPopulation != MAP17_POPULATION)) {
                throw new IOException("map17 monster source contract changed");
            }
            if (mapId == ClassicWorldMapCatalog.GERARD_TRIAL_DUNGEON_MAP_ID
                    && (!MAP22_SOURCE_SHA256.equals(headerSource)
                        || expectedRows != MAP22_DEFINITION_COUNT
                        || expectedPopulation != MAP22_POPULATION)) {
                throw new IOException("map22 monster source contract changed");
            }
            if (mapId == ClassicWorldMapCatalog.SLEEPING_DRAGON_FIRST_FLOOR_MAP_ID
                    && (!MAP19_SOURCE_SHA256.equals(headerSource)
                        || expectedRows != MAP19_DEFINITION_COUNT
                        || expectedPopulation != MAP19_POPULATION)) {
                throw new IOException("map19 monster source contract changed");
            }
            if (mapId == ClassicWorldMapCatalog.SLEEPING_DRAGON_SECOND_FLOOR_MAP_ID
                    && (!MAP20_SOURCE_SHA256.equals(headerSource)
                        || expectedRows != MAP20_DEFINITION_COUNT
                        || expectedPopulation != MAP20_POPULATION)) {
                throw new IOException("map20 monster source contract changed");
            }
            if (mapId == ClassicWorldMapCatalog.ORC_ELVEN_DUNGEON_MAP_ID
                    && (!MAP21_SOURCE_SHA256.equals(headerSource)
                        || expectedRows != MAP21_DEFINITION_COUNT
                        || expectedPopulation != MAP21_POPULATION)) {
                throw new IOException("map21 monster source contract changed");
            }
            if (mapId == ClassicWorldMapCatalog.DRAGON_VALLEY_DUNGEON_FIRST_FLOOR_MAP_ID
                    && (!MAP30_SOURCE_SHA256.equals(headerSource)
                        || expectedRows != MAP30_DEFINITION_COUNT
                        || expectedPopulation != MAP30_POPULATION)) {
                throw new IOException("map30 monster source contract changed");
            }
            if (mapId == ClassicWorldMapCatalog.DRAGON_VALLEY_DUNGEON_SECOND_FLOOR_MAP_ID
                    && (!MAP31_SOURCE_SHA256.equals(headerSource)
                        || expectedRows != MAP31_DEFINITION_COUNT
                        || expectedPopulation != MAP31_POPULATION)) {
                throw new IOException("map31 monster source contract changed");
            }
            if (mapId == ClassicWorldMapCatalog.DRAGON_VALLEY_DUNGEON_THIRD_FLOOR_MAP_ID
                    && (!MAP32_SOURCE_SHA256.equals(headerSource)
                        || expectedRows != MAP32_DEFINITION_COUNT
                        || expectedPopulation != MAP32_POPULATION)) {
                throw new IOException("map32 monster source contract changed");
            }
            if (mapId == ClassicWorldMapCatalog.DRAGON_VALLEY_DUNGEON_FOURTH_FLOOR_MAP_ID
                    && (!MAP33_SOURCE_SHA256.equals(headerSource)
                        || expectedRows != MAP33_DEFINITION_COUNT
                        || expectedPopulation != MAP33_POPULATION)) {
                throw new IOException("map33 monster source contract changed");
            }
            if (mapId == ClassicWorldMapCatalog.DRAGON_VALLEY_DUNGEON_FIFTH_FLOOR_MAP_ID
                    && (!MAP35_SOURCE_SHA256.equals(headerSource)
                        || expectedRows != MAP35_DEFINITION_COUNT
                        || expectedPopulation != MAP35_POPULATION)) {
                throw new IOException("map35 monster source contract changed");
            }
            if (mapId == ClassicWorldMapCatalog.DRAGON_VALLEY_DUNGEON_SIXTH_FLOOR_MAP_ID
                    && (!MAP36_SOURCE_SHA256.equals(headerSource)
                        || expectedRows != MAP36_DEFINITION_COUNT
                        || expectedPopulation != MAP36_POPULATION)) {
                throw new IOException("map36 monster source contract changed");
            }
            if (mapId == ClassicWorldMapCatalog.ANTHARAS_LAIR_MAP_ID
                    && (!MAP37_SOURCE_SHA256.equals(headerSource)
                        || expectedRows != MAP37_DEFINITION_COUNT
                        || expectedPopulation != MAP37_POPULATION)) {
                throw new IOException("map37 monster source contract changed");
            }
            if (mapId == ClassicWorldMapCatalog.VALAKAS_HABITAT_MAP_ID
                    && (!MAP67_SOURCE_SHA256.equals(headerSource)
                        || expectedRows != MAP67_DEFINITION_COUNT
                        || expectedPopulation != MAP67_POPULATION)) {
                throw new IOException("map67 monster source contract changed");
            }
            if (mapId == ClassicWorldMapCatalog.DARK_ELF_DUNGEON_MAP_ID
                    && (!MAP209_SOURCE_SHA256.equals(headerSource)
                        || expectedRows != MAP209_DEFINITION_COUNT
                        || expectedPopulation != MAP209_POPULATION)) {
                throw new IOException("map209 monster source contract changed");
            }
            if (mapId == ClassicWorldMapCatalog.DARKMAR_DUNGEON_MAP_ID
                    && (!MAP213_SOURCE_SHA256.equals(headerSource)
                        || expectedRows != MAP213_DEFINITION_COUNT
                        || expectedPopulation != MAP213_POPULATION)) {
                throw new IOException("map213 monster source contract changed");
            }
            if (mapId == ClassicWorldMapCatalog.SEPIA_DUNGEON_MAP_ID
                    && (!MAP302_SOURCE_SHA256.equals(headerSource)
                        || expectedRows != MAP302_DEFINITION_COUNT
                        || expectedPopulation != MAP302_POPULATION)) {
                throw new IOException("map302 monster source contract changed");
            }
            if (mapId == ClassicWorldMapCatalog.HIDDEN_DOCK_MAP_ID
                    && (!MAP445_SOURCE_SHA256.equals(headerSource)
                        || expectedRows != MAP445_DEFINITION_COUNT
                        || expectedPopulation != MAP445_POPULATION)) {
                throw new IOException("map445 monster source contract changed");
            }
            if (mapId == ClassicWorldMapCatalog.PIRATE_ISLAND_SHIP_MAP_ID
                    && (!MAP447_SOURCE_SHA256.equals(headerSource)
                        || expectedRows != MAP447_DEFINITION_COUNT
                        || expectedPopulation != MAP447_POPULATION)) {
                throw new IOException("map447 monster source contract changed");
            }
            if (mapId == ClassicWorldMapCatalog.PHANTASM_ISLAND_MAP_ID
                    && (!MAP303_SOURCE_SHA256.equals(headerSource)
                        || expectedRows != MAP303_DEFINITION_COUNT
                        || expectedPopulation != MAP303_POPULATION)) {
                throw new IOException("map303 monster source contract changed");
            }
            if (mapId == ClassicWorldMapCatalog.TAINTED_LAND_MAP_ID
                    && (!MAP244_SOURCE_SHA256.equals(headerSource)
                        || expectedRows != MAP244_DEFINITION_COUNT
                        || expectedPopulation != MAP244_POPULATION)) {
                throw new IOException("map244 monster source contract changed");
            }
            if (mapId == ClassicWorldHell.MAP_ID
                    && (!MAP666_SOURCE_SHA256.equals(headerSource)
                        || expectedRows != MAP666_DEFINITION_COUNT
                        || expectedPopulation != MAP666_POPULATION)) {
                throw new IOException("map666 monster source contract changed");
            }
            SourceContract antCave = ANT_CAVE_SOURCE_CONTRACTS.get(mapId);
            if (antCave != null && (!antCave.sourceSha256().equals(headerSource)
                    || expectedRows != antCave.rows()
                    || expectedPopulation != antCave.population())) {
                throw new IOException("ant-cave monster source contract changed " + mapId);
            }
            SourceContract knightCave = KNIGHT_CAVE_SOURCE_CONTRACTS.get(mapId);
            if (knightCave != null && (!knightCave.sourceSha256().equals(headerSource)
                    || expectedRows != knightCave.rows()
                    || expectedPopulation != knightCave.population())) {
                throw new IOException("knight-cave monster source contract changed " + mapId);
            }
            SourceContract gludioDungeon = GLUDIO_DUNGEON_SOURCE_CONTRACTS.get(mapId);
            if (gludioDungeon != null
                    && (!gludioDungeon.sourceSha256().equals(headerSource)
                        || expectedRows != gludioDungeon.rows()
                        || expectedPopulation != gludioDungeon.population())) {
                throw new IOException("Gludio Dungeon monster source contract changed "
                        + mapId);
            }
            SourceContract giranDungeon = GIRAN_DUNGEON_SOURCE_CONTRACTS.get(mapId);
            if (giranDungeon != null
                    && (!giranDungeon.sourceSha256().equals(headerSource)
                        || expectedRows != giranDungeon.rows()
                        || expectedPopulation != giranDungeon.population())) {
                throw new IOException("Giran Dungeon monster source contract changed "
                        + mapId);
            }
            if (mapId == ClassicWorldMapCatalog.GIRAN_CASTLE_MAP_ID
                    && (!MAP52_SOURCE_CONTRACT.sourceSha256().equals(headerSource)
                        || expectedRows != MAP52_SOURCE_CONTRACT.rows()
                        || expectedPopulation != MAP52_SOURCE_CONTRACT.population())) {
                throw new IOException("map52 monster source contract changed");
            }
            SourceContract windawood = WINDAWOOD_CASTLE_SOURCE_CONTRACTS.get(mapId);
            if (windawood != null
                    && (!windawood.sourceSha256().equals(headerSource)
                        || expectedRows != windawood.rows()
                        || expectedPopulation != windawood.population())) {
                throw new IOException(
                        "Windawood-castle monster source contract changed " + mapId);
            }
            SourceContract kent = KENT_COMPONENT_SOURCE_CONTRACTS.get(mapId);
            if (kent != null
                    && (!kent.sourceSha256().equals(headerSource)
                        || expectedRows != kent.rows()
                        || expectedPopulation != kent.population())) {
                throw new IOException(
                        "Kent-component monster source contract changed " + mapId);
            }
            ClassicWorldIvoryTowerCatalog.SourceContract ivoryTower =
                    IVORY_TOWER_SOURCE_CONTRACTS.get(mapId);
            if (ivoryTower != null
                    && (!ivoryTower.sourceSha256().equals(headerSource)
                        || expectedRows != ivoryTower.rows()
                        || expectedPopulation != ivoryTower.population())) {
                throw new IOException(
                        "Ivory-Tower monster source contract changed " + mapId);
            }
            if (mapId == ClassicWorldDwarfCastleCatalog.MAP_ID
                    && (!MAP66_SOURCE_CONTRACT.sourceSha256().equals(headerSource)
                        || expectedRows != MAP66_SOURCE_CONTRACT.rows()
                        || expectedPopulation != MAP66_SOURCE_CONTRACT.population())) {
                throw new IOException("map66 monster source contract changed");
            }
            ClassicWorldAdenToiCatalog.ResourceContract adenToi =
                    ClassicWorldAdenToiCatalog.MONSTER_CONTRACTS.get(mapId);
            if (adenToi != null
                    && (!adenToi.sourceSha256().equals(headerSource)
                        || expectedRows != adenToi.rows()
                        || expectedPopulation != adenToi.secondaryCount())) {
                throw new IOException(
                        "Aden/TOI monster source contract changed " + mapId);
            }
            ClassicWorldHeineCatalog.SourceContract heine =
                    ClassicWorldHeineCatalog.MONSTER_CONTRACTS.get(mapId);
            if (heine != null
                    && (!heine.sourceSha256().equals(headerSource)
                        || expectedRows != heine.rows()
                        || expectedPopulation != heine.population())) {
                throw new IOException("Heine/Eva monster source contract changed " + mapId);
            }
            ClassicWorldSilentCavernCatalog.SourceContract silent =
                    ClassicWorldSilentCavernCatalog.MONSTER_CONTRACTS.get(mapId);
            if (silent != null
                    && (!silent.sourceSha256().equals(headerSource)
                        || expectedRows != silent.rows()
                        || expectedPopulation != silent.population())) {
                throw new IOException(
                        "Silent Cavern monster source contract changed " + mapId);
            }
            ClassicWorldRastabadOumCatalog.SourceContract rastabad =
                    ClassicWorldRastabadOumCatalog.MONSTER_CONTRACTS.get(mapId);
            if (rastabad != null
                    && (!rastabad.sourceSha256().equals(headerSource)
                        || expectedRows != rastabad.rows()
                        || expectedPopulation != rastabad.population())) {
                throw new IOException(
                        "Rastabad/Oum monster source contract changed " + mapId);
            }
            ClassicWorldDiadradCatalog.SourceContract diadrad =
                    ClassicWorldDiadradCatalog.MONSTER_CONTRACTS.get(mapId);
            if (diadrad != null
                    && (!diadrad.sourceSha256().equals(headerSource)
                        || expectedRows != diadrad.rows()
                        || expectedPopulation != diadrad.population())) {
                throw new IOException(
                        "Diad service-core monster source contract changed " + mapId);
            }
            ClassicWorldElementalGraveCatalog.SourceContract elementalGrave =
                    ClassicWorldElementalGraveCatalog.MONSTER_CONTRACTS.get(mapId);
            if (elementalGrave != null
                    && (!elementalGrave.sourceSha256().equals(headerSource)
                        || expectedRows != elementalGrave.rows()
                        || expectedPopulation != elementalGrave.population())) {
                throw new IOException(
                        "Elemental-Grave monster source contract changed " + mapId);
            }
            ClassicWorldPirateDungeonCatalog.SourceContract pirateDungeon =
                    ClassicWorldPirateDungeonCatalog.MONSTER_CONTRACTS.get(mapId);
            if (pirateDungeon != null
                    && (!pirateDungeon.sourceSha256().equals(headerSource)
                        || expectedRows != pirateDungeon.rows()
                        || expectedPopulation != pirateDungeon.population())) {
                throw new IOException(
                        "Pirate-dungeon monster source contract changed " + mapId);
            }
            if (mapId == ClassicWorldLastabadGateCatalog.MAP_ID
                    && (!ClassicWorldLastabadGateCatalog.MONSTER_SOURCE_SHA256
                            .equals(headerSource)
                        || expectedRows != 0 || expectedPopulation != 0)) {
                throw new IOException(
                        "Lastabad-gate monster source contract changed");
            }

            List<SpawnDefinition> result = parseTable(mapId, table);
            int population = result.stream().mapToInt(SpawnDefinition::count).sum();
            if (result.size() != expectedRows || population != expectedPopulation) {
                throw new IOException("monster row/population count changed");
            }
            return List.copyOf(result);
        } catch (IOException | RuntimeException error) {
            ExceptionInInitializerError wrapped = new ExceptionInInitializerError(
                    "cannot load classic monster resource " + resourceName
                            + ": " + error.getMessage());
            wrapped.initCause(error);
            throw wrapped;
        }
    }

    private static List<SpawnDefinition> parseTable(int mapId, byte[] table)
            throws IOException {
        List<SpawnDefinition> result = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                new ByteArrayInputStream(table), StandardCharsets.UTF_8))) {
            if (!TSV_HEADER.equals(reader.readLine())) {
                throw new IOException("classic monster TSV header changed");
            }
            String line;
            int previousSpawnId = 0;
            while ((line = reader.readLine()) != null) {
                String[] field = line.split("\t", -1);
                if (field.length != 51) {
                    throw new IOException("classic monster row has " + field.length + " fields");
                }
                int spawnId = integer(field, 0);
                if (spawnId <= previousSpawnId) {
                    throw new IOException("classic monster spawn ids are not ordered");
                }
                previousSpawnId = spawnId;
                MonsterTemplate template = new MonsterTemplate(
                        integer(field, 3), field[20], field[21], integer(field, 22),
                        integer(field, 23), integer(field, 24), integer(field, 25),
                        integer(field, 26), integer(field, 27), integer(field, 28),
                        integer(field, 29), integer(field, 30), integer(field, 31),
                        integer(field, 32), longInteger(field, 33), integer(field, 34),
                        field[35], integer(field, 36), integer(field, 37),
                        integer(field, 38), integer(field, 39), integer(field, 40),
                        integer(field, 41), integer(field, 42), integer(field, 43),
                        bool(field, 44), integer(field, 45), bool(field, 46),
                        bool(field, 47), bool(field, 48), integer(field, 49),
                        bool(field, 50));
                result.add(new SpawnDefinition(
                        mapId, spawnId, field[1], integer(field, 2), integer(field, 4),
                        integer(field, 5), integer(field, 6), integer(field, 7),
                        integer(field, 8), integer(field, 9), integer(field, 10),
                        integer(field, 11), integer(field, 12), integer(field, 13),
                        integer(field, 14), integer(field, 15), bool(field, 16),
                        integer(field, 17), bool(field, 18), integer(field, 19),
                        template));
            }
        }
        return result;
    }

    private static int integer(String[] field, int index) {
        return Integer.parseInt(field[index]);
    }

    private static long longInteger(String[] field, int index) {
        return Long.parseLong(field[index]);
    }

    private static boolean bool(String[] field, int index) {
        int value = integer(field, index);
        if (value != 0 && value != 1) {
            throw new IllegalArgumentException("non-boolean monster field " + index);
        }
        return value == 1;
    }

    private static void requireResourceHash(String resourceName, byte[] packed) {
        String observed = sha256(packed);
        for (Map.Entry<Integer, ClassicWorldDesireCaveCatalog.ResourceContract> entry
                : ClassicWorldDesireCaveCatalog.MONSTER_CONTRACTS.entrySet()) {
            if (resourceName.equals("/world/map" + entry.getKey()
                    + "-monsters.lwm.gz")) {
                if (!entry.getValue().resourceSha256().equals(observed)) {
                    throw new ExceptionInInitializerError(
                            "Desire-Cave monster resource hash mismatch "
                                    + entry.getKey());
                }
                return;
            }
        }
        for (Map.Entry<Integer, ClassicWorldPirateBackHalfCatalog.ComponentContract> entry
                : ClassicWorldPirateBackHalfCatalog.MONSTER_CONTRACTS.entrySet()) {
            if (resourceName.equals("/world/map" + entry.getKey()
                    + "-monsters.lwm.gz")) {
                if (!entry.getValue().resourceSha256().equals(observed)) {
                    throw new ExceptionInInitializerError(
                            "Pirate-back-half monster resource hash mismatch "
                                    + entry.getKey());
                }
                return;
            }
        }
        for (Map.Entry<Integer, ClassicWorldBehemothSilveriaCatalog.ResourceContract> entry
                : ClassicWorldBehemothSilveriaCatalog.MONSTER_CONTRACTS.entrySet()) {
            if (resourceName.equals("/world/map" + entry.getKey()
                    + "-monsters.lwm.gz")) {
                if (!entry.getValue().resourceSha256().equals(observed)) {
                    throw new ExceptionInInitializerError(
                            "home-town monster resource hash mismatch " + entry.getKey());
                }
                return;
            }
        }
        for (Map.Entry<Integer, SourceContract> entry
                : ANT_CAVE_SOURCE_CONTRACTS.entrySet()) {
            if (resourceName.equals("/world/map" + entry.getKey()
                    + "-monsters.lwm.gz")) {
                if (!entry.getValue().resourceSha256().equals(observed)) {
                    throw new ExceptionInInitializerError(
                            "ant-cave monster resource hash mismatch " + entry.getKey());
                }
                return;
            }
        }
        for (Map.Entry<Integer, SourceContract> entry
                : KNIGHT_CAVE_SOURCE_CONTRACTS.entrySet()) {
            if (resourceName.equals("/world/map" + entry.getKey()
                    + "-monsters.lwm.gz")) {
                if (!entry.getValue().resourceSha256().equals(observed)) {
                    throw new ExceptionInInitializerError(
                            "knight-cave monster resource hash mismatch " + entry.getKey());
                }
                return;
            }
        }
        for (Map.Entry<Integer, SourceContract> entry
                : GLUDIO_DUNGEON_SOURCE_CONTRACTS.entrySet()) {
            if (resourceName.equals("/world/map" + entry.getKey()
                    + "-monsters.lwm.gz")) {
                if (!entry.getValue().resourceSha256().equals(observed)) {
                    throw new ExceptionInInitializerError(
                            "Gludio Dungeon monster resource hash mismatch "
                                    + entry.getKey());
                }
                return;
            }
        }
        for (Map.Entry<Integer, SourceContract> entry
                : GIRAN_DUNGEON_SOURCE_CONTRACTS.entrySet()) {
            if (resourceName.equals("/world/map" + entry.getKey()
                    + "-monsters.lwm.gz")) {
                if (!entry.getValue().resourceSha256().equals(observed)) {
                    throw new ExceptionInInitializerError(
                            "Giran Dungeon monster resource hash mismatch "
                                    + entry.getKey());
                }
                return;
            }
        }
        if (resourceName.equals("/world/map52-monsters.lwm.gz")) {
            if (!MAP52_SOURCE_CONTRACT.resourceSha256().equals(observed)) {
                throw new ExceptionInInitializerError(
                        "map52 monster resource hash mismatch");
            }
            return;
        }
        for (Map.Entry<Integer, SourceContract> entry
                : WINDAWOOD_CASTLE_SOURCE_CONTRACTS.entrySet()) {
            if (resourceName.equals("/world/map" + entry.getKey()
                    + "-monsters.lwm.gz")) {
                if (!entry.getValue().resourceSha256().equals(observed)) {
                    throw new ExceptionInInitializerError(
                            "Windawood-castle monster resource hash mismatch "
                                    + entry.getKey());
                }
                return;
            }
        }
        for (Map.Entry<Integer, SourceContract> entry
                : KENT_COMPONENT_SOURCE_CONTRACTS.entrySet()) {
            if (resourceName.equals("/world/map" + entry.getKey()
                    + "-monsters.lwm.gz")) {
                if (!entry.getValue().resourceSha256().equals(observed)) {
                    throw new ExceptionInInitializerError(
                            "Kent-component monster resource hash mismatch "
                                    + entry.getKey());
                }
                return;
            }
        }
        for (Map.Entry<Integer, ClassicWorldIvoryTowerCatalog.SourceContract> entry
                : IVORY_TOWER_SOURCE_CONTRACTS.entrySet()) {
            if (resourceName.equals("/world/map" + entry.getKey()
                    + "-monsters.lwm.gz")) {
                if (!entry.getValue().resourceSha256().equals(observed)) {
                    throw new ExceptionInInitializerError(
                            "Ivory-Tower monster resource hash mismatch "
                                    + entry.getKey());
                }
                return;
            }
        }
        if (resourceName.equals("/world/map66-monsters.lwm.gz")) {
            if (!MAP66_SOURCE_CONTRACT.resourceSha256().equals(observed)) {
                throw new ExceptionInInitializerError(
                        "map66 monster resource hash mismatch");
            }
            return;
        }
        for (Map.Entry<Integer, ClassicWorldAdenToiCatalog.ResourceContract> entry
                : ClassicWorldAdenToiCatalog.MONSTER_CONTRACTS.entrySet()) {
            if (resourceName.equals("/world/map" + entry.getKey()
                    + "-monsters.lwm.gz")) {
                if (!entry.getValue().resourceSha256().equals(observed)) {
                    throw new ExceptionInInitializerError(
                            "Aden/TOI monster resource hash mismatch "
                                    + entry.getKey());
                }
                return;
            }
        }
        for (Map.Entry<Integer, ClassicWorldHeineCatalog.SourceContract> entry
                : ClassicWorldHeineCatalog.MONSTER_CONTRACTS.entrySet()) {
            if (resourceName.equals("/world/map" + entry.getKey()
                    + "-monsters.lwm.gz")) {
                if (!entry.getValue().resourceSha256().equals(observed)) {
                    throw new ExceptionInInitializerError(
                            "Heine/Eva monster resource hash mismatch " + entry.getKey());
                }
                return;
            }
        }
        for (Map.Entry<Integer, ClassicWorldSilentCavernCatalog.SourceContract> entry
                : ClassicWorldSilentCavernCatalog.MONSTER_CONTRACTS.entrySet()) {
            if (resourceName.equals("/world/map" + entry.getKey()
                    + "-monsters.lwm.gz")) {
                if (!entry.getValue().resourceSha256().equals(observed)) {
                    throw new ExceptionInInitializerError(
                            "Silent Cavern monster resource hash mismatch " + entry.getKey());
                }
                return;
            }
        }
        for (Map.Entry<Integer, ClassicWorldRastabadOumCatalog.SourceContract> entry
                : ClassicWorldRastabadOumCatalog.MONSTER_CONTRACTS.entrySet()) {
            if (resourceName.equals("/world/map" + entry.getKey()
                    + "-monsters.lwm.gz")) {
                if (!entry.getValue().resourceSha256().equals(observed)) {
                    throw new ExceptionInInitializerError(
                            "Rastabad/Oum monster resource hash mismatch " + entry.getKey());
                }
                return;
            }
        }
        for (Map.Entry<Integer, ClassicWorldDiadradCatalog.SourceContract> entry
                : ClassicWorldDiadradCatalog.MONSTER_CONTRACTS.entrySet()) {
            if (resourceName.equals("/world/map" + entry.getKey()
                    + "-monsters.lwm.gz")) {
                if (!entry.getValue().resourceSha256().equals(observed)) {
                    throw new ExceptionInInitializerError(
                            "Diad service-core monster resource hash mismatch "
                                    + entry.getKey());
                }
                return;
            }
        }
        for (Map.Entry<Integer, ClassicWorldElementalGraveCatalog.SourceContract> entry
                : ClassicWorldElementalGraveCatalog.MONSTER_CONTRACTS.entrySet()) {
            if (resourceName.equals("/world/map" + entry.getKey()
                    + "-monsters.lwm.gz")) {
                if (!entry.getValue().resourceSha256().equals(observed)) {
                    throw new ExceptionInInitializerError(
                            "Elemental-Grave monster resource hash mismatch "
                                    + entry.getKey());
                }
                return;
            }
        }
        for (Map.Entry<Integer, ClassicWorldPirateDungeonCatalog.SourceContract> entry
                : ClassicWorldPirateDungeonCatalog.MONSTER_CONTRACTS.entrySet()) {
            if (resourceName.equals("/world/map" + entry.getKey()
                    + "-monsters.lwm.gz")) {
                if (!entry.getValue().resourceSha256().equals(observed)) {
                    throw new ExceptionInInitializerError(
                            "Pirate-dungeon monster resource hash mismatch "
                                    + entry.getKey());
                }
                return;
            }
        }
        if (resourceName.equals("/world/map450-monsters.lwm.gz")) {
            if (!ClassicWorldLastabadGateCatalog.MONSTER_RESOURCE_SHA256
                    .equals(observed)) {
                throw new ExceptionInInitializerError(
                        "Lastabad-gate monster resource hash mismatch");
            }
            return;
        }
        if (resourceName.equals("/world/map4-monsters.lwm.gz")
                && !MAP4_RESOURCE_SHA256.equals(observed)) {
            throw new ExceptionInInitializerError("map4 monster resource hash mismatch");
        }
        if (resourceName.equals("/world/map0-monsters.lwm.gz")
                && !MAP0_RESOURCE_SHA256.equals(observed)) {
            throw new ExceptionInInitializerError("map0 monster resource hash mismatch");
        }
        if (resourceName.equals("/world/map1-monsters.lwm.gz")
                && !MAP1_RESOURCE_SHA256.equals(observed)) {
            throw new ExceptionInInitializerError("map1 monster resource hash mismatch");
        }
        if (resourceName.equals("/world/map2-monsters.lwm.gz")
                && !MAP2_RESOURCE_SHA256.equals(observed)) {
            throw new ExceptionInInitializerError("map2 monster resource hash mismatch");
        }
        if (resourceName.equals("/world/map14-monsters.lwm.gz")
                && !MAP14_RESOURCE_SHA256.equals(observed)) {
            throw new ExceptionInInitializerError("map14 monster resource hash mismatch");
        }
        if (resourceName.equals("/world/map13-monsters.lwm.gz")
                && !MAP13_RESOURCE_SHA256.equals(observed)) {
            throw new ExceptionInInitializerError("map13 monster resource hash mismatch");
        }
        if (resourceName.equals("/world/map237-monsters.lwm.gz")
                && !MAP237_RESOURCE_SHA256.equals(observed)) {
            throw new ExceptionInInitializerError("map237 monster resource hash mismatch");
        }
        if (resourceName.equals("/world/map3-monsters.lwm.gz")
                && !MAP3_RESOURCE_SHA256.equals(observed)) {
            throw new ExceptionInInitializerError("map3 monster resource hash mismatch");
        }
        if (resourceName.equals("/world/map5-monsters.lwm.gz")
                && !MAP5_RESOURCE_SHA256.equals(observed)) {
            throw new ExceptionInInitializerError("map5 monster resource hash mismatch");
        }
        if (resourceName.equals("/world/map6-monsters.lwm.gz")
                && !MAP6_RESOURCE_SHA256.equals(observed)) {
            throw new ExceptionInInitializerError("map6 monster resource hash mismatch");
        }
        if (resourceName.equals("/world/map70-monsters.lwm.gz")
                && !MAP70_RESOURCE_SHA256.equals(observed)) {
            throw new ExceptionInInitializerError("map70 monster resource hash mismatch");
        }
        if (resourceName.equals("/world/map83-monsters.lwm.gz")
                && !MAP83_RESOURCE_SHA256.equals(observed)) {
            throw new ExceptionInInitializerError("map83 monster resource hash mismatch");
        }
        if (resourceName.equals("/world/map84-monsters.lwm.gz")
                && !MAP84_RESOURCE_SHA256.equals(observed)) {
            throw new ExceptionInInitializerError("map84 monster resource hash mismatch");
        }
        if (resourceName.equals("/world/map16-monsters.lwm.gz")
                && !MAP16_RESOURCE_SHA256.equals(observed)) {
            throw new ExceptionInInitializerError("map16 monster resource hash mismatch");
        }
        if (resourceName.equals("/world/map17-monsters.lwm.gz")
                && !MAP17_RESOURCE_SHA256.equals(observed)) {
            throw new ExceptionInInitializerError("map17 monster resource hash mismatch");
        }
        if (resourceName.equals("/world/map22-monsters.lwm.gz")
                && !MAP22_RESOURCE_SHA256.equals(observed)) {
            throw new ExceptionInInitializerError("map22 monster resource hash mismatch");
        }
        if (resourceName.equals("/world/map19-monsters.lwm.gz")
                && !MAP19_RESOURCE_SHA256.equals(observed)) {
            throw new ExceptionInInitializerError("map19 monster resource hash mismatch");
        }
        if (resourceName.equals("/world/map20-monsters.lwm.gz")
                && !MAP20_RESOURCE_SHA256.equals(observed)) {
            throw new ExceptionInInitializerError("map20 monster resource hash mismatch");
        }
        if (resourceName.equals("/world/map21-monsters.lwm.gz")
                && !MAP21_RESOURCE_SHA256.equals(observed)) {
            throw new ExceptionInInitializerError("map21 monster resource hash mismatch");
        }
        if (resourceName.equals("/world/map30-monsters.lwm.gz")
                && !MAP30_RESOURCE_SHA256.equals(observed)) {
            throw new ExceptionInInitializerError("map30 monster resource hash mismatch");
        }
        if (resourceName.equals("/world/map31-monsters.lwm.gz")
                && !MAP31_RESOURCE_SHA256.equals(observed)) {
            throw new ExceptionInInitializerError("map31 monster resource hash mismatch");
        }
        if (resourceName.equals("/world/map32-monsters.lwm.gz")
                && !MAP32_RESOURCE_SHA256.equals(observed)) {
            throw new ExceptionInInitializerError("map32 monster resource hash mismatch");
        }
        if (resourceName.equals("/world/map33-monsters.lwm.gz")
                && !MAP33_RESOURCE_SHA256.equals(observed)) {
            throw new ExceptionInInitializerError("map33 monster resource hash mismatch");
        }
        if (resourceName.equals("/world/map35-monsters.lwm.gz")
                && !MAP35_RESOURCE_SHA256.equals(observed)) {
            throw new ExceptionInInitializerError("map35 monster resource hash mismatch");
        }
        if (resourceName.equals("/world/map36-monsters.lwm.gz")
                && !MAP36_RESOURCE_SHA256.equals(observed)) {
            throw new ExceptionInInitializerError("map36 monster resource hash mismatch");
        }
        if (resourceName.equals("/world/map37-monsters.lwm.gz")
                && !MAP37_RESOURCE_SHA256.equals(observed)) {
            throw new ExceptionInInitializerError("map37 monster resource hash mismatch");
        }
        if (resourceName.equals("/world/map67-monsters.lwm.gz")
                && !MAP67_RESOURCE_SHA256.equals(observed)) {
            throw new ExceptionInInitializerError("map67 monster resource hash mismatch");
        }
        if (resourceName.equals("/world/map209-monsters.lwm.gz")
                && !MAP209_RESOURCE_SHA256.equals(observed)) {
            throw new ExceptionInInitializerError("map209 monster resource hash mismatch");
        }
        if (resourceName.equals("/world/map213-monsters.lwm.gz")
                && !MAP213_RESOURCE_SHA256.equals(observed)) {
            throw new ExceptionInInitializerError("map213 monster resource hash mismatch");
        }
        if (resourceName.equals("/world/map302-monsters.lwm.gz")
                && !MAP302_RESOURCE_SHA256.equals(observed)) {
            throw new ExceptionInInitializerError("map302 monster resource hash mismatch");
        }
        if (resourceName.equals("/world/map445-monsters.lwm.gz")
                && !MAP445_RESOURCE_SHA256.equals(observed)) {
            throw new ExceptionInInitializerError("map445 monster resource hash mismatch");
        }
        if (resourceName.equals("/world/map447-monsters.lwm.gz")
                && !MAP447_RESOURCE_SHA256.equals(observed)) {
            throw new ExceptionInInitializerError("map447 monster resource hash mismatch");
        }
        if (resourceName.equals("/world/map303-monsters.lwm.gz")
                && !MAP303_RESOURCE_SHA256.equals(observed)) {
            throw new ExceptionInInitializerError("map303 monster resource hash mismatch");
        }
        if (resourceName.equals("/world/map244-monsters.lwm.gz")
                && !MAP244_RESOURCE_SHA256.equals(observed)) {
            throw new ExceptionInInitializerError("map244 monster resource hash mismatch");
        }
        if (resourceName.equals("/world/map666-monsters.lwm.gz")
                && !MAP666_RESOURCE_SHA256.equals(observed)) {
            throw new ExceptionInInitializerError("map666 monster resource hash mismatch");
        }
    }

    private static String sha256(byte[] bytes) {
        try {
            return HexFormat.of().withUpperCase().formatHex(
                    MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException(impossible);
        }
    }
}
