using System.Diagnostics;
using System.Text.Json;

namespace LiTo381Launcher;

// T140 / Issue #404: acceptance entry "connect to an already running loopback Server".
// Mirrors DevelopmentStartup's native-client half; the Server Pair (JVM start, 32000
// READY contract, JVM identity) is replaced by an endpoint check on the external
// profile.  DevelopmentStartup and the formal standard flow are untouched.
internal sealed class ExternalServerStartup(ExternalServerProfile profile, string profilePath, Action<string> log,
    Action<ExtendedCharacterStatsSnapshot>? extendedStats = null)
{
    internal const string HelperFileName = "external_server_startup.py";
    private const string HelperSha = "3CAE67C05697510CA3B35C8354D7C14E362D4F19A145750A472FD665139428B9";
    private OwnedMemoryStartupProcess? helper;
    internal ExternalServerProfile Profile => profile;
    internal bool CanAddClient { get; private set; }
    internal bool OwnershipRetained => helper?.OwnershipRetained == true || OwnedMemoryStartupProcess.HasRetainedCaptureFailure;
    internal bool CleanupQualified { get; private set; } = true;

    // T139 / Issue #412: optional absolute directory for gui-runs/external-* receipts and the helper's
    // runs/external-* directory.  Unset keeps the original app directory.
    internal const string RunRootVariable = "LITO_LAUNCHER_RUN_ROOT";

    internal static string Describe(ExternalServerProfile profile) =>
        $"外部伺服器驗收模式：{profile.DisplayName}（{string.Join("、", profile.ExpectedEndpoints().Select(e => $"{e.Host}:{e.Port}"))}）；"
        + "不啟動內建伺服器、不覆寫埠；0-客戶端只讀取核對、不寫入。";

    private static bool IsReparsePoint(string path)
    {
        try { return (File.GetAttributes(path) & FileAttributes.ReparsePoint) != 0; }
        catch (FileNotFoundException) { return false; }
        catch (DirectoryNotFoundException) { return false; }
    }

    private static bool IsInside(string child, string parent)
    {
        var trimmedParent = Path.TrimEndingDirectorySeparator(Path.GetFullPath(parent));
        var trimmedChild = Path.TrimEndingDirectorySeparator(Path.GetFullPath(child));
        return trimmedChild.Equals(trimmedParent, StringComparison.OrdinalIgnoreCase)
            || trimmedChild.StartsWith(trimmedParent + Path.DirectorySeparatorChar, StringComparison.OrdinalIgnoreCase);
    }

    /// <summary>
    /// Validates the run root (value of <see cref="RunRootVariable"/>); null/blank means "not set".
    /// Same rules as resolve_run_root in external_server_startup.py.
    /// </summary>
    internal static string? ResolveRunRoot(string? value, string clientDirectory, Func<string, bool>? isReparsePoint = null)
    {
        if (string.IsNullOrWhiteSpace(value)) return null;
        if (!Path.IsPathFullyQualified(value)) throw new ArgumentException($"{RunRootVariable} 必須是絕對路徑：{value}");
        var full = Path.TrimEndingDirectorySeparator(Path.GetFullPath(value));
        var reparse = isReparsePoint ?? IsReparsePoint;
        for (var node = full; !string.IsNullOrEmpty(node); node = Path.GetDirectoryName(node))
            if (reparse(node)) throw new ArgumentException($"{RunRootVariable} 路徑含 junction／重新解析點，拒絕：{node}");
        if (!Directory.Exists(full)) throw new ArgumentException($"{RunRootVariable} 目錄不存在：{full}");
        if (IsInside(full, clientDirectory)) throw new ArgumentException($"{RunRootVariable} 不得位於客戶端目錄底下：{full}");
        return full;
    }

    internal static string? CurrentRunRoot(string clientDirectory) =>
        ResolveRunRoot(Environment.GetEnvironmentVariable(RunRootVariable), clientDirectory);

    /// <summary>Receipt directory for one run: &lt;run root or app&gt;\gui-runs\&lt;run id&gt;.</summary>
    internal static string ReceiptDirectory(string? runRoot, string runId) =>
        Path.Combine(runRoot ?? DevelopmentStartup.Root, "gui-runs", runId);

    private static MemorySourcePin Helper()
    {
        var pin = MemorySourcePin.Capture(Path.Combine(AppContext.BaseDirectory, HelperFileName));
        if (pin.Sha256 != HelperSha) throw new InvalidDataException("外部伺服器 helper 指紋不符。");
        return pin;
    }

    private static MemorySourcePin FormalHelper()
    {
        // The external helper imports the reviewed formal helper; both must be the reviewed bytes.
        var pin = MemorySourcePin.Capture(Path.Combine(AppContext.BaseDirectory, "development_startup.py"));
        if (pin.Sha256 != DevelopmentStartup.EntrySha) throw new InvalidDataException("開發啟動 helper 指紋不符。");
        return pin;
    }

    internal static MemoryStartupAvailability CheckAvailability(LauncherMenuPreferences preferences, ExternalServerProfile profile)
    {
        try
        {
            preferences.Validate();
            profile.Validate();
            if (preferences.NativeService != NativeServiceMode.StandardArena) throw new ArgumentException("外部伺服器驗收只支援一般無界擂台模式。");
            if (!preferences.ClientDirectory.Equals(@"D:\lineage開發\0-客戶端", StringComparison.OrdinalIgnoreCase))
                throw new ArgumentException("共用資源版請選擇 D:\\lineage開發\\0-客戶端。");
            var selected = MemorySourcePin.Capture(Path.Combine(preferences.ClientDirectory, "LiTo.bin"));
            if (selected.Sha256 != LauncherService.ExpectedProtectedSha256)
                throw new InvalidDataException("指定客戶端的 LiTo.bin 指紋不符。");
            selected.Verify(); Helper().Verify(); FormalHelper().Verify(); FormalStandardSources.Reviewed.Python.Verify();
            var runRoot = CurrentRunRoot(preferences.ClientDirectory);
            return new(MemoryStartupAvailabilityKind.Available,
                Describe(profile) + (runRoot is null ? "" : $"收據寫到 {runRoot}。"), null);
        }
        catch (Exception error) { return new(MemoryStartupAvailabilityKind.SourceValidationFailed, error.Message, error); }
    }

    /// <summary>Helper arguments after "--serve --run-id &lt;id&gt;"; pure for tests.</summary>
    internal static IReadOnlyList<string> HelperArguments(LauncherMenuPreferences preferences, int sourceId, string profilePath,
        string? runRoot = null)
    {
        List<string> arguments =
        [
            "--client-directory", preferences.ClientDirectory,
            "--scale", preferences.WindowScalePercent.ToString(System.Globalization.CultureInfo.InvariantCulture),
            "--service", preferences.NativeService.ToString(),
            "--source-id", sourceId.ToString(System.Globalization.CultureInfo.InvariantCulture),
            "--external-profile", Path.GetFullPath(profilePath)
        ];
        if (runRoot is not null) arguments.AddRange(["--run-root", runRoot]);
        return arguments;
    }

    /// <summary>Validates the helper READY event against the profile: every expected endpoint connected, no Server owned.</summary>
    internal static void ValidateReady(JsonElement ready, ExternalServerProfile profile, int sourceId)
    {
        if (ready.GetProperty("event").GetString() != "ready"
                || !ready.TryGetProperty("external_server", out var external) || external.ValueKind != JsonValueKind.True
                || !ready.TryGetProperty("server_owned", out var owned) || owned.ValueKind != JsonValueKind.False
                || ready.GetProperty("selected_source_id").GetInt32() != sourceId
                || ready.GetProperty("service_mode").GetString() != "standard"
                || ready.GetProperty("host").GetString() != profile.Host
                || ready.GetProperty("port").GetInt32() != profile.Port)
            throw new InvalidDataException("外部伺服器 READY 與設定不符。");
        var endpoints = ready.GetProperty("endpoints").EnumerateArray().ToArray();
        var expected = profile.ExpectedEndpoints();
        if (endpoints.Length != expected.Count) throw new InvalidDataException("外部伺服器端點數量不符。");
        foreach (var (host, port) in expected)
        {
            var matches = endpoints.Where(e => e.GetProperty("host").GetString() == host && e.GetProperty("port").GetInt32() == port).ToArray();
            if (matches.Length != 1 || !matches[0].GetProperty("connect").GetBoolean() || matches[0].GetProperty("payload_bytes_sent").GetInt32() != 0)
                throw new InvalidDataException($"外部伺服器端點 {host}:{port} 未就緒。");
        }
    }

    internal async Task AddClientAsync(CancellationToken cancellation)
    {
        if (!CanAddClient || helper is null) throw new InvalidOperationException("請先等待第一個遊戲視窗載入完成。");
        CanAddClient = false;
        bool replied = false;
        try
        {
            await helper.SendCommandAsync("add_client");
            var result = await helper.AwaitEventAsync("additional_client_result", TimeSpan.FromSeconds(145), cancellation);
            replied = true;
            if (!result.GetProperty("success").GetBoolean()) throw new InvalidOperationException(result.GetProperty("reason").GetString());
            log($"[多開] 新遊戲視窗 PID {result.GetProperty("pid").GetInt32()} 已就緒，連同一個外部伺服器；請使用另一個帳號登入。");
        }
        finally { CanAddClient = replied && helper.IsAlive && !cancellation.IsCancellationRequested; }
    }

    internal async Task RunAsync(LauncherMenuPreferences preferences, CancellationToken cancellation, int sourceId)
    {
        if (!profile.Sources.Any(source => source.Id == sourceId)) throw new InvalidOperationException("選擇的來源伺服器不在外部伺服器設定中。");
        var available = CheckAvailability(preferences, profile);
        if (!available.CanStart) throw available.Error!;
        var runId = "external-" + Guid.NewGuid().ToString("N");
        var runRoot = CurrentRunRoot(preferences.ClientDirectory);
        var receipt = ReceiptDirectory(runRoot, runId);
        Directory.CreateDirectory(receipt);
        using var current = Process.GetCurrentProcess();
        MemoryStartupJson.WriteExclusive(Path.Combine(receipt, "request.json"), new
        {
            run_id = runId, launcher_identity = MemoryProcessIdentity.Capture(current), selected_preferences = preferences, selected_source_id = sourceId,
            created_at_utc = DateTimeOffset.UtcNow, source_entry = Helper(), external_profile_path = Path.GetFullPath(profilePath), external_profile = profile,
            server_owned = false, run_root = runRoot, client_writes_by_launcher = 0
        });
        Exception? failure = null;
        try
        {
            var command = OwnedMemoryStartupProcess.ServerCommand(FormalStandardSources.Reviewed.Python, Helper(), runId);
            foreach (var value in HelperArguments(preferences, sourceId, profilePath, runRoot)) command.ArgumentList.Add(value);
            CleanupQualified = false;
            helper = new(command, line =>
            {
                log(line);
                var marker = line.IndexOf("[LiTo-Extended-Stats] ", StringComparison.Ordinal);
                if (marker >= 0 && ExtendedCharacterStatsProtocol.TryParse(line[marker..], out var snapshot)) extendedStats?.Invoke(snapshot);
            });
            log("[外部伺服器] 新場次 " + runId + "；" + Describe(profile));
            var ready = await helper.AwaitEventAsync("ready", TimeSpan.FromSeconds(40), cancellation);
            ValidateReady(ready, profile, sourceId);
            MemoryStartupJson.WriteExclusive(Path.Combine(receipt, "server-ready.json"), ready);
            log("[外部伺服器] 端點皆可連線（只做 TCP 連線、不送資料）；開始原版客戶端。");
            var native = await helper.AwaitEventAsync("native_created", TimeSpan.FromSeconds(45), cancellation);
            var id = native.GetProperty("identity");
            using (var process = Process.GetProcessById(id.GetProperty("pid").GetInt32()))
            {
                _ = process.Handle;
                var observed = MemoryProcessIdentity.Capture(process);
                if (observed.ParentPid != id.GetProperty("parent_pid").GetInt32()
                    || observed.Filetime != id.GetProperty("filetime").GetInt64().ToString()
                    || !observed.Path.Equals(id.GetProperty("path").GetString(), StringComparison.OrdinalIgnoreCase)
                    || observed.ImageSha256 != LauncherService.ExpectedProtectedSha256)
                    throw new InvalidDataException("原版客戶端當次程序身分不符。");
                MemoryStartupJson.WriteExclusive(Path.Combine(receipt, "native-created.json"), observed);
                log($"[外部伺服器] LiTo.bin PID {observed.Pid} 已建立；尚不代表畫面或登入驗收。");
            }
            var resources = await helper.AwaitEventAsync("resources_ready", TimeSpan.FromSeconds(95), cancellation);
            if (resources.GetProperty("pid").GetInt32() != id.GetProperty("pid").GetInt32()
                || resources.GetProperty("profile").GetString() != "tw13081901-v1"
                || resources.GetProperty("total_count").GetInt32() != 30285)
                throw new InvalidDataException("共用動畫資源尚未就緒。");
            log("[共同來源] TW 30000 筆＋LiTo 專用 285 筆已完成原生載入；請先在遊戲中選服，再輸入帳密。");
            CanAddClient = true;
            await helper.Exit.WaitAsync(cancellation);
        }
        catch (Exception error) { failure = error; throw; }
        finally
        {
            CanAddClient = false;
            if (helper is not null) await helper.StopAsync();
            else OwnedMemoryStartupProcess.RecoverCaptureFailures();
            var finished = helper?.Finished;
            CleanupQualified = !OwnershipRetained && finished is not null && finished.Value.GetProperty("cleanup_verified").GetBoolean();
            MemoryStartupJson.WriteExclusive(Path.Combine(receipt, "terminal.json"), new
            {
                run_id = runId, cleanup_qualified = CleanupQualified, ownership_retained = OwnershipRetained, server_owned = false,
                failure = failure?.ToString(), helper_result = finished
            });
            if (!CleanupQualified) throw new InvalidOperationException("本次外部伺服器啟動清理尚未確認，保留控制。");
            if (failure is null && helper?.ObservedExitCode != 0) throw new InvalidOperationException("外部伺服器啟動 helper 回報失敗，請查看本次日誌。");
        }
    }
}
