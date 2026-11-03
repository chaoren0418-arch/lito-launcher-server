using System.Security.Cryptography;
using System.Text;

namespace LiTo381Launcher;

public sealed record SharedResourceState(
    string RelativePath,
    bool Exists,
    long Length,
    long LastWriteTimeUtcTicks,
    FileAttributes Attributes,
    string? Sha256,
    string? Error);

public sealed record SharedResourceChange(string RelativePath, string Detail);

public sealed record ClientResourceSnapshot(
    string ClientDirectory,
    string Fingerprint,
    IReadOnlyDictionary<string, SharedResourceState> Files,
    IReadOnlyList<string> Errors);

/// <summary>
/// Read-only validation and before/after guarding for files shared by the
/// original TW client and LiTo.bin.  The launcher never repairs or replaces
/// anything in this list.
/// </summary>
public static class ClientResourceAudit
{
    private const long FullHashLimitBytes = 8L * 1024 * 1024;

    private static readonly string[] LiToArchivePairs =
    {
        "LTtext", "LTsprite", "LTsurf", "LTicon", "LTtile"
    };

    private static readonly string[] BaseArchivePairs =
    {
        "Sprite", "Text", "Tile"
    };

    public static async Task<IReadOnlyList<ValidationItem>> ValidateAsync(string clientDirectory)
    {
        var items = new List<ValidationItem>();
        if (!Directory.Exists(clientDirectory))
        {
            items.Add(new ValidationItem("客戶端資源目錄", false, clientDirectory));
            return items;
        }

        var missingLiTo = new List<string>();
        foreach (var pair in LiToArchivePairs)
        {
            RequireFile(clientDirectory, Path.Combine("LiTo", pair + ".idx"), missingLiTo);
            RequireFile(clientDirectory, Path.Combine("LiTo", pair + ".pak"), missingLiTo);
        }
        RequirePopulatedDirectory(clientDirectory, Path.Combine("LiTo", "LTmap"), missingLiTo);
        RequirePopulatedDirectory(clientDirectory, Path.Combine("LiTo", "LTsound"), missingLiTo);
        items.Add(new ValidationItem(
            "LITO 專用 LT 資源",
            missingLiTo.Count == 0,
            missingLiTo.Count == 0
                ? "5 組 LT IDX/PAK、LTmap 與 LTsound 完整"
                : "缺少：" + string.Join(", ", missingLiTo)));

        var missingShared = new List<string>();
        foreach (var pair in BaseArchivePairs)
        {
            RequireFile(clientDirectory, pair + ".idx", missingShared);
            RequireFile(clientDirectory, pair + ".pak", missingShared);
        }
        for (var index = 0; index < 16; index++)
        {
            var pair = $"Sprite{index:D2}";
            RequireFile(clientDirectory, pair + ".idx", missingShared);
            RequireFile(clientDirectory, pair + ".pak", missingShared);
        }
        items.Add(new ValidationItem(
            "TW/LITO 共用封包配對",
            missingShared.Count == 0,
            missingShared.Count == 0
                ? "Sprite、Text、Tile 與 Sprite00-15 共 19 組 IDX/PAK 均成對存在；只檢測、不替換"
                : "缺少：" + string.Join(", ", missingShared)));

        var twClient = Path.Combine(clientDirectory, "TW13081901.bin");
        if (File.Exists(twClient))
        {
            var info = new FileInfo(twClient);
            var hash = await HashFileAsync(twClient);
            items.Add(new ValidationItem(
                "TW13081901 共存保護",
                true,
                $"已偵測 {info.Length:N0} bytes，SHA-256 {ShortHash(hash)}；LITO 流程不修改此檔"));
        }
        else
        {
            items.Add(new ValidationItem(
                "TW13081901 共存保護",
                true,
                "此客戶端目錄未包含 TW13081901.bin；不影響 LITO 驗證"));
        }

        var residue = Directory.EnumerateFiles(
                clientDirectory, "*.lito381-original-*", SearchOption.TopDirectoryOnly)
            .Select(Path.GetFileName)
            .OrderBy(name => name, StringComparer.OrdinalIgnoreCase)
            .ToArray();
        items.Add(new ValidationItem(
            "相容層還原狀態",
            residue.Length == 0,
            residue.Length == 0
                ? "沒有上次異常中斷留下的原檔備份"
                : "發現殘留備份，請先人工確認：" + string.Join(", ", residue)));

        var snapshot = await CaptureAsync(clientDirectory);
        items.Add(new ValidationItem(
            "共享資源版本指紋",
            snapshot.Errors.Count == 0,
            snapshot.Errors.Count == 0
                ? $"{ShortHash(snapshot.Fingerprint)}（記錄現況，不與官方版本比對、不自動降版）"
                : string.Join("；", snapshot.Errors)));
        return items;
    }

    public static async Task<ClientResourceSnapshot> CaptureAsync(string clientDirectory)
    {
        var root = Path.GetFullPath(clientDirectory);
        var files = new Dictionary<string, SharedResourceState>(StringComparer.OrdinalIgnoreCase);
        var errors = new List<string>();

        foreach (var relativePath in EnumerateGuardedRelativePaths())
        {
            var state = await CaptureFileAsync(root, relativePath);
            files[relativePath] = state;
            if (state.Error is not null)
            {
                errors.Add($"{relativePath}: {state.Error}");
            }
        }

        var fingerprintSource = string.Join("\n", files.Values
            .OrderBy(state => state.RelativePath, StringComparer.OrdinalIgnoreCase)
            .Select(state => string.Join("|",
                state.RelativePath,
                state.Exists,
                state.Length,
                state.LastWriteTimeUtcTicks,
                (int)state.Attributes,
                state.Sha256 ?? string.Empty,
                state.Error ?? string.Empty)));
        var fingerprint = Convert.ToHexString(
            SHA256.HashData(Encoding.UTF8.GetBytes(fingerprintSource)));
        return new ClientResourceSnapshot(root, fingerprint, files, errors);
    }

    public static IReadOnlyList<SharedResourceChange> Compare(
        ClientResourceSnapshot before,
        ClientResourceSnapshot after)
    {
        var changes = new List<SharedResourceChange>();
        var paths = before.Files.Keys
            .Union(after.Files.Keys, StringComparer.OrdinalIgnoreCase)
            .OrderBy(path => path, StringComparer.OrdinalIgnoreCase);
        foreach (var path in paths)
        {
            before.Files.TryGetValue(path, out var oldState);
            after.Files.TryGetValue(path, out var newState);
            if (oldState is null || newState is null)
            {
                changes.Add(new SharedResourceChange(path, "檢測集合發生變化"));
                continue;
            }
            if (oldState.Error is not null || newState.Error is not null)
            {
                if (!string.Equals(oldState.Error, newState.Error, StringComparison.Ordinal))
                {
                    changes.Add(new SharedResourceChange(
                        path, $"讀取狀態改變：{oldState.Error ?? "正常"} -> {newState.Error ?? "正常"}"));
                }
                continue;
            }
            if (oldState.Exists != newState.Exists)
            {
                changes.Add(new SharedResourceChange(
                    path, oldState.Exists ? "檔案在 LITO 執行後消失" : "LITO 執行期間新增檔案"));
                continue;
            }
            if (!oldState.Exists)
            {
                continue;
            }

            var differences = new List<string>();
            if (oldState.Length != newState.Length)
                differences.Add($"大小 {oldState.Length:N0} -> {newState.Length:N0}");
            if (oldState.LastWriteTimeUtcTicks != newState.LastWriteTimeUtcTicks)
                differences.Add("最後寫入時間改變");
            if (oldState.Attributes != newState.Attributes)
                differences.Add($"屬性 {(int)oldState.Attributes} -> {(int)newState.Attributes}");
            if (oldState.Sha256 is not null && newState.Sha256 is not null
                && !oldState.Sha256.Equals(newState.Sha256, StringComparison.OrdinalIgnoreCase))
                differences.Add($"SHA-256 {ShortHash(oldState.Sha256)} -> {ShortHash(newState.Sha256)}");
            if (differences.Count > 0)
            {
                changes.Add(new SharedResourceChange(path, string.Join("；", differences)));
            }
        }
        return changes;
    }

    private static IEnumerable<string> EnumerateGuardedRelativePaths()
    {
        yield return "TW13081901.bin";
        yield return "TW13081901.pak";
        yield return "LiTo.bin";
        yield return "LiTo.exe";
        yield return "Mss32.dll";
        yield return "litolauncher-c.pak";
        foreach (var pair in BaseArchivePairs)
        {
            yield return pair + ".idx";
            yield return pair + ".pak";
        }
        for (var index = 0; index < 16; index++)
        {
            yield return $"Sprite{index:D2}.idx";
            yield return $"Sprite{index:D2}.pak";
        }
        foreach (var pair in LiToArchivePairs)
        {
            yield return Path.Combine("LiTo", pair + ".idx");
            yield return Path.Combine("LiTo", pair + ".pak");
        }
    }

    private static async Task<SharedResourceState> CaptureFileAsync(string root, string relativePath)
    {
        var fullPath = Path.Combine(root, relativePath);
        if (!File.Exists(fullPath))
        {
            return new SharedResourceState(
                relativePath, false, 0, 0, 0, null, null);
        }

        try
        {
            var info = new FileInfo(fullPath);
            var hash = info.Length <= FullHashLimitBytes
                ? await HashFileAsync(fullPath)
                : null;
            info.Refresh();
            return new SharedResourceState(
                relativePath,
                true,
                info.Length,
                info.LastWriteTimeUtc.Ticks,
                info.Attributes,
                hash,
                null);
        }
        catch (Exception ex)
        {
            return new SharedResourceState(
                relativePath, true, 0, 0, 0, null, ex.Message);
        }
    }

    private static async Task<string> HashFileAsync(string path)
    {
        await using var stream = new FileStream(
            path, FileMode.Open, FileAccess.Read,
            FileShare.ReadWrite | FileShare.Delete,
            1024 * 1024,
            FileOptions.Asynchronous | FileOptions.SequentialScan);
        return Convert.ToHexString(await SHA256.HashDataAsync(stream));
    }

    private static void RequireFile(string root, string relativePath, ICollection<string> missing)
    {
        if (!File.Exists(Path.Combine(root, relativePath)))
        {
            missing.Add(relativePath);
        }
    }

    private static void RequirePopulatedDirectory(
        string root, string relativePath, ICollection<string> missing)
    {
        var path = Path.Combine(root, relativePath);
        if (!Directory.Exists(path) || !Directory.EnumerateFiles(path, "*", SearchOption.AllDirectories).Any())
        {
            missing.Add(relativePath + Path.DirectorySeparatorChar);
        }
    }

    private static string ShortHash(string hash) => hash[..Math.Min(16, hash.Length)];
}
