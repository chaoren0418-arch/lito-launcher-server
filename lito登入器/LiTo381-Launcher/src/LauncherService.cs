using System.Diagnostics;
using System.Security.Cryptography;
using System.Security.Principal;

namespace LiTo381Launcher;

public sealed record ValidationItem(string Name, bool Passed, string Detail);

public sealed record ValidationResult(
    bool CanLaunch,
    string Mode,
    string? ClientSha256,
    IReadOnlyList<ValidationItem> Items,
    IReadOnlyList<string> LaunchArguments);

public static class LauncherService
{
    public const int NumericBreakthroughSchemaVersion = 4;
    public const string ExpectedProtectedSha256 =
        "CE6EE68ADA8FD37E6C096B1A27E46CB6888F1179F85DEFB5007AC60287360292";
    public const string ExpectedCompatibilityProxySha256 =
        "FAB11934D6377BB253525330A8B1E490906CFACD641394F13906167D9A593D7B";
    public const string ExpectedLocalServerSha256 =
        "3025E3DF1C28F55019E79D5F21F9C1587B7268FB6417D6A75A591B6AECACB4B4";

    public static async Task<ValidationResult> ValidateAsync(LauncherSettings settings)
    {
        var items = new List<ValidationItem>();
        var elevated = IsAdministrator();
        items.Add(new ValidationItem(
            "一般權限控制介面",
            true,
            elevated
                ? "目前由已提升程序啟動；功能相容，但一般使用不需要系統管理員權限"
                : "一般權限可用；LiTo.bin 由隔離的 RunAsInvoker 工作流程啟動"));
        var projectRoot = Normalize(settings.ProjectRoot);
        var clientDirectory = Normalize(settings.ClientDirectory);
        AddFile(items, "相容啟動工具", Path.Combine(projectRoot, "tools", "lito_runtime_probe.py"));
        AddFile(items, "文字索引", Path.Combine(clientDirectory, "Text.idx"));
        AddFile(items, "文字資料", Path.Combine(clientDirectory, "Text.pak"));

        var client = GetClientPath(settings.Mode, clientDirectory);
        AddFile(items, "遊戲客戶端", client);
        AddFile(items, "啟動腳本", GetStartScript(settings.Mode, projectRoot));

        var python = FindOnPath("python.exe") ?? FindOnPath("python");
        items.Add(new ValidationItem("Python runtime", python is not null, python ?? "PATH 中找不到 python"));

        string? hash = null;
        if (File.Exists(client))
        {
            hash = await Sha256Async(client);
            if (settings.Mode == LaunchMode.ProtectedClient)
            {
                items.Add(new ValidationItem(
                    "LiTo.bin 原版雜湊",
                    hash.Equals(ExpectedProtectedSha256, StringComparison.OrdinalIgnoreCase),
                    hash));
            }
            else
            {
                items.Add(new ValidationItem("衍生客戶端雜湊", true, hash));
            }
        }

        if (settings.Mode == LaunchMode.RebuiltClient)
        {
            AddFile(items, "same-boot manifest", client + ".json");
        }

        if (settings.Mode == LaunchMode.ProtectedClient)
        {
            var nativeClientArguments = GetNativeClientArguments(settings.NativeService);
            foreach (var item in await ClientResourceAudit.ValidateAsync(clientDirectory))
            {
                items.Add(item);
            }

            var liToRuntimeDirectory = GetLiToRuntimeDirectory(settings);
            items.Add(new ValidationItem("執行時間設定",
                settings.LaunchTimeoutSeconds is >= 0 and <= 3600,
                settings.LaunchTimeoutSeconds == 0 ? "等待原生客戶端退出" : $"限時 {settings.LaunchTimeoutSeconds} 秒；不視為自然退出"));
            foreach (var item in await LiToCompatibilityResources.ValidateAsync(
                         clientDirectory, liToRuntimeDirectory))
            {
                items.Add(item);
            }

            var displayDetail = settings.DisplayMode switch
            {
                GameDisplayMode.Windowed => "視窗化（DDSCL_NORMAL；預設）",
                GameDisplayMode.Fullscreen => "全螢幕（exclusive 640x480x16）",
                GameDisplayMode.Auto => "原版自動（依桌面色深判斷）",
                _ => throw new ArgumentOutOfRangeException()
            };
            items.Add(new ValidationItem("顯示模式", true, displayDetail));
            if (settings.NumericBreakthroughEnabled)
            {
                var compatible = settings.DisplayMode == GameDisplayMode.Windowed
                    && settings.StartLocalServer;
                items.Add(new ValidationItem(
                    "LiTo 32-bit 數值契約",
                    compatible,
                    compatible
                        ? $"schema {NumericBreakthroughSchemaVersion}：相容層與本機 Server 同步啟用 HP／MP／AC／等級／六素質／SP／MR／四屬性抗性 dword"
                        : "數值突破目前必須同時使用視窗模式與本機 LiTo Server"));
            }
            var nativeServiceDetail = settings.NativeService switch
            {
                NativeServiceMode.StandardArena =>
                    $"一般無界擂台；LiTo.bin 引數：{FormatClientArguments(nativeClientArguments)}（預設）",
                NativeServiceMode.RentalServer =>
                    $"官方租賃伺服器；內嵌 list-lcc 的租借列與 p/P 標記等價；LiTo.bin 原生引數：{FormatClientArguments(nativeClientArguments)}",
                _ => throw new ArgumentOutOfRangeException()
            };
            items.Add(new ValidationItem("原生服務入口", true, nativeServiceDetail));
            if (settings.DisplayMode == GameDisplayMode.Windowed)
            {
                var compatRoot = GetCompatRoot(projectRoot);
                var compatProxy = Path.Combine(compatRoot, "publish", "ddraw.dll");
                AddFile(items, "LiTo DirectDraw proxy", compatProxy);
                if (File.Exists(compatProxy))
                {
                    var compatHash = await Sha256Async(compatProxy);
                    var compatCurrent = compatHash.Equals(
                        ExpectedCompatibilityProxySha256,
                        StringComparison.OrdinalIgnoreCase);
                    items.Add(new ValidationItem(
                        "LiTo 結算返回相容層",
                        compatCurrent,
                        compatCurrent
                            ? $"版本指紋 {compatHash}；含原生 phase 5→2 結算 Confirm 的 C_D0 橋接"
                            : $"相容層版本不符：{compatHash}；預期 {ExpectedCompatibilityProxySha256}"));
                }
                AddFile(items, "LiTo window compatibility config", Path.Combine(compatRoot, "publish", "lito381-compat.ini"));
                items.Add(new ValidationItem(
                    "視窗縮放",
                    settings.WindowScalePercent is >= 75 and <= 250,
                    $"{settings.WindowScalePercent}%（虛擬座標固定 640×480）"));
            }
        }
        else if (settings.Mode == LaunchMode.ProtocolLab)
        {
            AddFile(items, "protocol-lab manifest", client + ".json");
            AddFile(items, "opcode map", Path.Combine(projectRoot, "analysis", "protocol-opcodes.json"));
        }

        if (!string.IsNullOrWhiteSpace(settings.ServerProfilePath))
        {
            if (settings.Mode != LaunchMode.ProtectedClient)
            {
                items.Add(new ValidationItem(
                    "Server profile 模式",
                    false,
                    "connect redirect 第一版只支援原始 protected LiTo.bin"));
            }
            else if (!File.Exists(settings.ServerProfilePath))
            {
                items.Add(new ValidationItem("Server profile", false, settings.ServerProfilePath));
            }
            else
            {
                try
                {
                    var profile = ServerProfileDefinition.Load(settings.ServerProfilePath);
                    var networkAllowed = profile.IsLoopback || settings.AllowNetwork;
                    items.Add(new ValidationItem(
                        "Server profile",
                        networkAllowed,
                        $"{profile.DisplayName}: source={profile.OriginSourceId}/{profile.OriginSourceName}, "
                        + $"endpoint={profile.Host}:{profile.Port}"
                        + (networkAllowed ? string.Empty : "（非回環位址必須允許外部網路）")));
                }
                catch (Exception ex)
                {
                    items.Add(new ValidationItem("Server profile", false, ex.Message));
                }
            }
        }

        if (settings.StartLocalServer && settings.Mode == LaunchMode.ProtectedClient)
        {
            if (string.IsNullOrWhiteSpace(settings.ServerProfilePath)
                    || !File.Exists(settings.ServerProfilePath))
            {
                items.Add(new ValidationItem(
                    "Local character source",
                    false,
                    "Select a valid origin-source profile before starting the local server."));
            }
            else
            {
                try
                {
                    var profile = ServerProfileDefinition.Load(settings.ServerProfilePath);
                    foreach (var item in LocalServerService.Validate(
                                 GetServerRoot(projectRoot), profile, settings.NativeService))
                    {
                        items.Add(item);
                    }
                }
                catch (Exception ex)
                {
                    items.Add(new ValidationItem("Local character source", false, ex.Message));
                }
            }
        }

        var launchArguments = CreateLaunchStartInfo(settings).ArgumentList.ToArray();
        return new ValidationResult(
            items.All(item => item.Passed), settings.Mode.ToString(), hash, items, launchArguments);
    }

    public static ProcessStartInfo CreateLaunchStartInfo(LauncherSettings settings)
    {
        var script = GetStartScript(settings.Mode, Normalize(settings.ProjectRoot));
        var startInfo = CreatePowerShellStartInfo(script, Normalize(settings.ProjectRoot));
        startInfo.ArgumentList.Add("-ClientDirectory");
        startInfo.ArgumentList.Add(Normalize(settings.ClientDirectory));
        startInfo.ArgumentList.Add("-TimeoutSeconds");
        startInfo.ArgumentList.Add(settings.LaunchTimeoutSeconds.ToString(System.Globalization.CultureInfo.InvariantCulture));
        if (settings.Mode == LaunchMode.ProtectedClient)
        {
            startInfo.ArgumentList.Add("-DisplayMode");
            startInfo.ArgumentList.Add(settings.DisplayMode.ToString());
            startInfo.ArgumentList.Add("-WindowScalePercent");
            startInfo.ArgumentList.Add(settings.WindowScalePercent.ToString());
            startInfo.ArgumentList.Add("-SelectionFlow");
            startInfo.ArgumentList.Add(settings.SelectionFlow.ToString());
            if (settings.NumericBreakthroughEnabled)
            {
                startInfo.ArgumentList.Add("-NumericBreakthrough");
                startInfo.ArgumentList.Add("-NumericBreakthroughSchema");
                startInfo.ArgumentList.Add(NumericBreakthroughSchemaVersion.ToString());
            }
            startInfo.ArgumentList.Add("-CompatDirectory");
            startInfo.ArgumentList.Add(Path.Combine(GetCompatRoot(Normalize(settings.ProjectRoot)), "publish"));
            startInfo.ArgumentList.Add("-LiToRuntimeDirectory");
            startInfo.ArgumentList.Add(GetLiToRuntimeDirectory(settings));
        }
        if (settings.AllowNetwork && settings.Mode != LaunchMode.ProtocolLab)
        {
            startInfo.ArgumentList.Add("-AllowNetwork");
        }
        if (!string.IsNullOrWhiteSpace(settings.ServerProfilePath))
        {
            startInfo.ArgumentList.Add("-ServerProfile");
            startInfo.ArgumentList.Add(Path.GetFullPath(settings.ServerProfilePath));
        }
        if (settings.Mode == LaunchMode.ProtectedClient
                && GetNativeClientArguments(settings.NativeService) is { Count: > 0 } clientArguments)
        {
            // Keep the array-valued PowerShell parameter last so no subsequent
            // named launcher option can be consumed as a client argument.
            startInfo.ArgumentList.Add("-ClientArguments");
            foreach (var argument in clientArguments)
            {
                startInfo.ArgumentList.Add(argument);
            }
        }
        return startInfo;
    }

    public static IReadOnlyList<string> GetNativeClientArguments(NativeServiceMode mode) => mode switch
    {
        NativeServiceMode.StandardArena => Array.Empty<string>(),
        NativeServiceMode.RentalServer => new[] { "/ltpremiumsvr" },
        _ => throw new ArgumentOutOfRangeException(nameof(mode), mode, null)
    };

    private static string FormatClientArguments(IReadOnlyList<string> arguments) =>
        arguments.Count == 0 ? "（無）" : string.Join(" ", arguments);

    public static ProcessStartInfo CreateProtocolBuildStartInfo(LauncherSettings settings)
    {
        var script = Path.Combine(Normalize(settings.ProjectRoot), "tools", "Build-LiTo381ProtocolLab.ps1");
        var startInfo = CreatePowerShellStartInfo(script, Normalize(settings.ProjectRoot));
        startInfo.ArgumentList.Add("-ClientDirectory");
        startInfo.ArgumentList.Add(Normalize(settings.ClientDirectory));
        return startInfo;
    }

    public static string GetReportsDirectory(LauncherSettings settings) => Path.Combine(
        Normalize(settings.ProjectRoot), "runtime", "launch-reports");

    public static string GetServerRoot(string projectRoot)
    {
        var parent = Directory.GetParent(Normalize(projectRoot))?.FullName ?? string.Empty;
        var classified = Path.GetFullPath(Path.Combine(parent, "..", "lito伺服器", "LiTo381-Server"));
        return File.Exists(Path.Combine(classified, "LiTo381-Server.jar"))
            ? classified : Path.Combine(parent, "LiTo381-Server");
    }

    public static string GetCompatRoot(string projectRoot) => Path.Combine(
        Directory.GetParent(Normalize(projectRoot))?.FullName ?? string.Empty,
        "LiTo381-Compat");

    public static string GetLiToRuntimeDirectory(string projectRoot)
    {
        var isolated = Path.Combine(Normalize(projectRoot), "runtime", "client-isolated");
        return Directory.Exists(isolated)
            ? isolated : Path.Combine(Normalize(projectRoot), "runtime", "client-full");
    }

    public static string GetLiToRuntimeDirectory(LauncherSettings settings)
    {
        if (string.IsNullOrWhiteSpace(settings.LiToRuntimeDirectory))
            return GetLiToRuntimeDirectory(settings.ProjectRoot);
        if (!Path.IsPathFullyQualified(settings.LiToRuntimeDirectory))
            throw new ArgumentException("LiTo isolated runtime must be an absolute path.");
        return Path.GetFullPath(settings.LiToRuntimeDirectory);
    }

    private static ProcessStartInfo CreatePowerShellStartInfo(string script, string workingDirectory)
    {
        var powershell = Path.Combine(
            Environment.GetFolderPath(Environment.SpecialFolder.Windows),
            "System32", "WindowsPowerShell", "v1.0", "powershell.exe");
        var info = new ProcessStartInfo
        {
            FileName = powershell,
            WorkingDirectory = workingDirectory,
            UseShellExecute = false,
            CreateNoWindow = true,
            RedirectStandardOutput = true,
            RedirectStandardError = true
        };
        info.ArgumentList.Add("-NoProfile");
        info.ArgumentList.Add("-ExecutionPolicy");
        info.ArgumentList.Add("Bypass");
        info.ArgumentList.Add("-File");
        info.ArgumentList.Add(script);
        return info;
    }

    private static string GetClientPath(LaunchMode mode, string clientDirectory) => mode switch
    {
        LaunchMode.ProtectedClient => Path.Combine(clientDirectory, "LiTo.bin"),
        LaunchMode.RebuiltClient => Path.Combine(clientDirectory, "LiTo.unpacked.full-same-boot.exe"),
        LaunchMode.ProtocolLab => Path.Combine(clientDirectory, "LiTo.protocol-lab.exe"),
        _ => throw new ArgumentOutOfRangeException(nameof(mode))
    };

    private static string GetStartScript(LaunchMode mode, string projectRoot) => mode switch
    {
        LaunchMode.ProtectedClient => Path.Combine(projectRoot, "tools", "Start-LiTo381.ps1"),
        LaunchMode.RebuiltClient => Path.Combine(projectRoot, "tools", "Start-LiTo381Rebuilt.ps1"),
        LaunchMode.ProtocolLab => Path.Combine(projectRoot, "tools", "Start-LiTo381ProtocolLab.ps1"),
        _ => throw new ArgumentOutOfRangeException(nameof(mode))
    };

    private static void AddFile(ICollection<ValidationItem> items, string name, string path)
    {
        items.Add(new ValidationItem(name, File.Exists(path), path));
    }

    private static string Normalize(string path) => string.IsNullOrWhiteSpace(path)
        ? string.Empty
        : Path.GetFullPath(path);

    private static string? FindOnPath(string executable)
    {
        if (Path.IsPathRooted(executable) && File.Exists(executable))
        {
            return executable;
        }
        foreach (var directory in (Environment.GetEnvironmentVariable("PATH") ?? string.Empty)
                     .Split(Path.PathSeparator, StringSplitOptions.RemoveEmptyEntries | StringSplitOptions.TrimEntries))
        {
            try
            {
                var candidate = Path.Combine(directory, executable);
                if (File.Exists(candidate))
                {
                    return candidate;
                }
            }
            catch
            {
                // Ignore malformed PATH entries and continue validating the rest.
            }
        }
        return null;
    }

    private static async Task<string> Sha256Async(string path)
    {
        await using var stream = new FileStream(
            path, FileMode.Open, FileAccess.Read, FileShare.Read, 1024 * 1024,
            FileOptions.Asynchronous | FileOptions.SequentialScan);
        var hash = await SHA256.HashDataAsync(stream);
        return Convert.ToHexString(hash);
    }

    private static bool IsAdministrator()
    {
        using var identity = WindowsIdentity.GetCurrent();
        return new WindowsPrincipal(identity)
            .IsInRole(WindowsBuiltInRole.Administrator);
    }
}
