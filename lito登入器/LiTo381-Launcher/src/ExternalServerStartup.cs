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
    private const string HelperSha = "D0FE39EB6E379875F04E28FBF12B532E8F6AA2EB54347161E905DD3C86191D32";
    private OwnedMemoryStartupProcess? helper;
    internal ExternalServerProfile Profile => profile;
    internal bool CanAddClient { get; private set; }
    internal bool OwnershipRetained => helper?.OwnershipRetained == true || OwnedMemoryStartupProcess.HasRetainedCaptureFailure;
    internal bool CleanupQualified { get; private set; } = true;

    internal static string Describe(ExternalServerProfile profile) =>
        $"外部伺服器驗收模式：{profile.DisplayName}（{string.Join("、", profile.ExpectedEndpoints().Select(e => $"{e.Host}:{e.Port}"))}）；"
        + "不啟動內建伺服器、不覆寫埠。";

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
            return new(MemoryStartupAvailabilityKind.Available, Describe(profile), null);
        }
        catch (Exception error) { return new(MemoryStartupAvailabilityKind.SourceValidationFailed, error.Message, error); }
    }

    /// <summary>Helper arguments after "--serve --run-id &lt;id&gt;"; pure for tests.</summary>
    internal static IReadOnlyList<string> HelperArguments(LauncherMenuPreferences preferences, int sourceId, string profilePath) =>
    [
        "--client-directory", preferences.ClientDirectory,
        "--scale", preferences.WindowScalePercent.ToString(System.Globalization.CultureInfo.InvariantCulture),
        "--service", preferences.NativeService.ToString(),
        "--source-id", sourceId.ToString(System.Globalization.CultureInfo.InvariantCulture),
        "--external-profile", Path.GetFullPath(profilePath)
    ];

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
        var receipt = Path.Combine(DevelopmentStartup.Root, "gui-runs", runId);
        Directory.CreateDirectory(receipt);
        using var current = Process.GetCurrentProcess();
        MemoryStartupJson.WriteExclusive(Path.Combine(receipt, "request.json"), new
        {
            run_id = runId, launcher_identity = MemoryProcessIdentity.Capture(current), selected_preferences = preferences, selected_source_id = sourceId,
            created_at_utc = DateTimeOffset.UtcNow, source_entry = Helper(), external_profile_path = Path.GetFullPath(profilePath), external_profile = profile,
            server_owned = false
        });
        Exception? failure = null;
        try
        {
            var command = OwnedMemoryStartupProcess.ServerCommand(FormalStandardSources.Reviewed.Python, Helper(), runId);
            foreach (var value in HelperArguments(preferences, sourceId, profilePath)) command.ArgumentList.Add(value);
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
