using System.Security.Cryptography;

namespace LiTo381Launcher;

/// <summary>
/// Validates the known-good resource generation used only by LiTo.bin.  The
/// selected TW client stays the source of truth for TW13081901.bin and is
/// guarded read-only by ClientResourceAudit.
/// </summary>
public static class LiToCompatibilityResources
{
    private static readonly int[] NativeLtMapDirectories =
    {
        500, 501, 502, 503, 504, 505, 506, 507, 508, 509, 510,
        512, 513, 514, 516, 517, 518, 519, 520, 521, 522, 524,
        525, 526, 527, 528, 529, 530, 531, 532, 534, 535, 540,
        541, 542, 543, 545, 546, 547, 548, 549, 550, 551, 552,
        553, 554, 555, 560, 562, 563, 564, 565, 566, 567, 568,
        569, 570, 573, 574, 16384, 16896, 17408
    };

    private static readonly IReadOnlyDictionary<string, long> ExpectedLengths =
        new Dictionary<string, long>(StringComparer.OrdinalIgnoreCase)
        {
            ["LiTo.bin"] = 1_941_568,
            ["Mss32.dll"] = 372_736,
            ["litolauncher-c.pak"] = 2_175_088,
            ["Sprite.idx"] = 2_585_972,
            ["Sprite.pak"] = 1_904_012_586,
            ["Text.idx"] = 1_044_152,
            ["Text.pak"] = 25_991_498,
            ["Tile.idx"] = 114_860,
            ["Tile.pak"] = 457_175_695,
            ["Sprite00.idx"] = 119_508,
            ["Sprite00.pak"] = 190_151_795,
            ["Sprite01.idx"] = 117_688,
            ["Sprite01.pak"] = 189_956_402,
            ["Sprite02.idx"] = 116_316,
            ["Sprite02.pak"] = 187_308_869,
            ["Sprite03.idx"] = 115_840,
            ["Sprite03.pak"] = 195_010_189,
            ["Sprite04.idx"] = 115_280,
            ["Sprite04.pak"] = 202_031_478,
            ["Sprite05.idx"] = 114_328,
            ["Sprite05.pak"] = 175_276_997,
            ["Sprite06.idx"] = 116_008,
            ["Sprite06.pak"] = 191_562_270,
            ["Sprite07.idx"] = 118_052,
            ["Sprite07.pak"] = 170_142_328,
            ["Sprite08.idx"] = 119_732,
            ["Sprite08.pak"] = 164_164_639,
            ["Sprite09.idx"] = 122_812,
            ["Sprite09.pak"] = 161_881_673,
            ["Sprite10.idx"] = 124_072,
            ["Sprite10.pak"] = 165_247_165,
            ["Sprite11.idx"] = 124_548,
            ["Sprite11.pak"] = 167_037_709,
            ["Sprite12.idx"] = 123_260,
            ["Sprite12.pak"] = 158_993_499,
            ["Sprite13.idx"] = 122_840,
            ["Sprite13.pak"] = 164_298_012,
            ["Sprite14.idx"] = 122_196,
            ["Sprite14.pak"] = 174_888_085,
            ["Sprite15.idx"] = 120_712,
            ["Sprite15.pak"] = 182_681_245,
            [Path.Combine("LiTo", "LTtext.idx")] = 10_056,
            [Path.Combine("LiTo", "LTtext.pak")] = 295_287,
            [Path.Combine("LiTo", "LTsprite.idx")] = 13_136,
            [Path.Combine("LiTo", "LTsprite.pak")] = 63_004_112,
            [Path.Combine("LiTo", "LTsurf.idx")] = 45_448,
            [Path.Combine("LiTo", "LTsurf.pak")] = 92_788_879,
            [Path.Combine("LiTo", "LTicon.idx")] = 6_360,
            [Path.Combine("LiTo", "LTicon.pak")] = 274_258,
            [Path.Combine("LiTo", "LTtile.idx")] = 33_884,
            [Path.Combine("LiTo", "LTtile.pak")] = 148_485_306
        };

    private static readonly IReadOnlyDictionary<string, string> ExpectedHashes =
        new Dictionary<string, string>(StringComparer.OrdinalIgnoreCase)
        {
            ["LiTo.bin"] = "CE6EE68ADA8FD37E6C096B1A27E46CB6888F1179F85DEFB5007AC60287360292",
            ["Mss32.dll"] = "24EA6B88E5DBCF9488A1D4E4ADBF5D13A6F7C90705608E9735BA1D2009E5EEE3",
            ["litolauncher-c.pak"] = "CB29F318985DB4A1D4D7845668B4AECC97B5F92594BE17BE6BCB37D1D36EBB79",
            ["Text.idx"] = "B23C985CDAD807808E996FED31DBCB174D920B17EEE65025535EE92BF76FEA82",
            ["Tile.idx"] = "C6C1E741C22E05503E1865BEC8692AD476EECE863738B2B8A96356006A37D4E2",
            [Path.Combine("LiTo", "LTtext.idx")] = "412AF669F06DF26CB1590E80E458D448ED7A304F3917911D94CF3BC2DA676F77",
            [Path.Combine("LiTo", "LTtext.pak")] = "2A7CD3DD3838FBCBC6E93F99F26B5CFA04685787671562F447C2D61D81E939C1"
        };

    private static readonly string[] PcActionRewardArchives =
    {
        Path.Combine("LiTo", "LTtext.idx"),
        Path.Combine("LiTo", "LTtext.pak")
    };

    private static readonly string[] RequiredUiFiles =
    {
        "0x0404.ini", "background.bmp", "launcher.Pak", "launcher-c.Pak",
        "Lineage.ini", "LineageTW.ini", "lineagemain.jpg", "logo.jpg",
        "selectserver.jpg"
    };

    public static async Task<IReadOnlyList<ValidationItem>> ValidateAsync(
        string selectedClientDirectory,
        string runtimeDirectory)
    {
        var items = new List<ValidationItem>();
        var selectedMismatches = FindLengthMismatches(selectedClientDirectory).ToArray();
        items.Add(new ValidationItem(
            "選定客戶端資源世代檢測",
            true,
            selectedMismatches.Length == 0
                ? "選定目錄符合 LiTo 2008 相容資源世代，仍使用隔離執行副本"
                : $"偵測到 {selectedMismatches.Length} 個新版或缺少資源；LiTo 將使用隔離相容副本，不改寫此目錄"));

        var runtimeProblems = FindLengthMismatches(runtimeDirectory).ToList();
        foreach (var file in RequiredUiFiles)
        {
            if (!File.Exists(Path.Combine(runtimeDirectory, file)))
            {
                runtimeProblems.Add("缺少 " + file);
            }
        }
        var skin = Path.Combine(runtimeDirectory, "skin");
        if (!Directory.Exists(skin) || !Directory.EnumerateFiles(skin, "*", SearchOption.AllDirectories).Any())
        {
            runtimeProblems.Add("缺少 skin\\ 登入 UI 資源");
        }
        foreach (var pair in new[] { "LTtext", "LTsprite", "LTsurf", "LTicon", "LTtile" })
        {
            foreach (var extension in new[] { ".idx", ".pak" })
            {
                var relative = Path.Combine("LiTo", pair + extension);
                if (!File.Exists(Path.Combine(runtimeDirectory, relative)))
                {
                    runtimeProblems.Add("缺少 " + relative);
                }
            }
        }
        ValidateNativeMapSet(runtimeDirectory, runtimeProblems);

        var actualHashes = new Dictionary<string, string>(StringComparer.OrdinalIgnoreCase);
        foreach (var (relativePath, expectedHash) in ExpectedHashes)
        {
            var path = Path.Combine(runtimeDirectory, relativePath);
            if (!File.Exists(path))
            {
                continue;
            }
            var actualHash = await Sha256Async(path);
            actualHashes[relativePath] = actualHash;
            if (!actualHash.Equals(expectedHash, StringComparison.OrdinalIgnoreCase))
            {
                runtimeProblems.Add($"{relativePath} SHA-256 不符");
            }
        }

        var pcActionRewardUiReady = PcActionRewardArchives.All(relativePath =>
            actualHashes.TryGetValue(relativePath, out var actualHash)
            && actualHash.Equals(ExpectedHashes[relativePath],
                StringComparison.OrdinalIgnoreCase));
        items.Add(new ValidationItem(
            "天堂小幫手領獎 UI",
            pcActionRewardUiReady,
            pcActionRewardUiReady
                ? "隔離 LTtext 已固定包含 pc_action 與 send_s_0～4；動作僅開放 SQL-first 領獎流程"
                : "LiTo\\LTtext 領獎覆蓋層缺少或指紋不符，禁止啟動以免開啟空白介面"));

        items.Add(new ValidationItem(
            "LiTo 隔離相容資源",
            runtimeProblems.Count == 0,
            runtimeProblems.Count == 0
                ? $"已通過 Text/Tile/Sprite00-15、Mss32、啟動 UI、5 組 LiTo\\LT* 與原生 62 個 LTmap 目錄檢測：{runtimeDirectory}"
                : string.Join("；", runtimeProblems.Take(8))
                  + (runtimeProblems.Count > 8 ? $"；其他 {runtimeProblems.Count - 8} 項" : string.Empty)));

        items.Add(new ValidationItem(
            "客戶端相容邊界",
            true,
            "僅支援指定 LiTo.bin：登入／戰鬥 opcode 狀態機、32-bit x86 記憶體位址、Numeric Schema 4 與 LT 資源編號皆為成套契約；更換 bin/exe 或資源世代會被拒絕，不會冒險啟動。"));

        items.Add(new ValidationItem(
            "TW/LiTo 執行隔離",
            !Path.GetFullPath(selectedClientDirectory).Equals(
                Path.GetFullPath(runtimeDirectory), StringComparison.OrdinalIgnoreCase),
            $"TW 監測目錄：{Path.GetFullPath(selectedClientDirectory)}；LiTo 執行目錄：{Path.GetFullPath(runtimeDirectory)}"));
        return items;
    }

    private static IEnumerable<string> FindLengthMismatches(string root)
    {
        foreach (var (relativePath, expectedLength) in ExpectedLengths)
        {
            var path = Path.Combine(root, relativePath);
            if (!File.Exists(path))
            {
                yield return $"缺少 {relativePath}";
                continue;
            }
            var actualLength = new FileInfo(path).Length;
            if (actualLength != expectedLength)
            {
                yield return $"{relativePath} 大小 {actualLength:N0} != {expectedLength:N0}";
            }
        }
    }

    private static void ValidateNativeMapSet(string root, ICollection<string> problems)
    {
        var mapRoot = Path.Combine(root, "LiTo", "LTmap");
        if (!Directory.Exists(mapRoot))
        {
            problems.Add("\u7f3a\u5c11 LiTo\\LTmap\\");
            return;
        }

        var expected = NativeLtMapDirectories.ToHashSet();
        var actual = new HashSet<int>();
        var invalidNames = new List<string>();
        foreach (var path in Directory.EnumerateDirectories(mapRoot, "*", SearchOption.TopDirectoryOnly))
        {
            var name = Path.GetFileName(path);
            if (int.TryParse(name, out var mapId))
            {
                actual.Add(mapId);
            }
            else
            {
                invalidNames.Add(name);
            }
        }

        var missing = expected.Except(actual).Order().ToArray();
        var extra = actual.Except(expected).Order().ToArray();
        if (missing.Length == 0 && extra.Length == 0 && invalidNames.Count == 0)
        {
            return;
        }

        var detail = new List<string> { $"LiTo\\LTmap 必須精確包含 62 個原生地圖目錄，目前 {actual.Count}" };
        if (missing.Length > 0) detail.Add("缺少 " + string.Join(",", missing));
        if (extra.Length > 0) detail.Add("多出 " + string.Join(",", extra));
        if (invalidNames.Count > 0) detail.Add("非數字目錄 " + string.Join(",", invalidNames));
        problems.Add(string.Join("；", detail));
    }

    private static async Task<string> Sha256Async(string path)
    {
        await using var stream = new FileStream(
            path, FileMode.Open, FileAccess.Read, FileShare.Read,
            1024 * 1024, FileOptions.Asynchronous | FileOptions.SequentialScan);
        return Convert.ToHexString(await SHA256.HashDataAsync(stream));
    }
}
