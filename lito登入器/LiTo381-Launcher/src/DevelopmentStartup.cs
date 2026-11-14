using System.Diagnostics;
using System.Text.Json;

namespace LiTo381Launcher;

internal sealed class DevelopmentStartup(Action<string> log, Action<ExtendedCharacterStatsSnapshot>? extendedStats = null)
{
    internal const string Root = @"D:\lineage開發\3-lito開發\lito登入器\LiTo381-Launcher\app";
    private const string ServerRuns = @"D:\lineage開發\3-lito開發\lito登入器\LiTo381-Launcher\app\server\runs";
    internal const string EntrySha ="56532A2FF06DFCF2C1569681D997E84410C8EA68CB4F65B4ED9BCA0D7CDA8189";
    private OwnedMemoryStartupProcess? helper;
    internal bool CanAddClient { get; private set; }
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
            log($"[多開] 新遊戲視窗 PID {result.GetProperty("pid").GetInt32()} 已就緒，共用本次伺服器；請使用另一個帳號登入。");
        }
        finally { CanAddClient = replied && helper.IsAlive && !cancellation.IsCancellationRequested; }
    }
    internal bool OwnershipRetained => helper?.OwnershipRetained == true || OwnedMemoryStartupProcess.HasRetainedCaptureFailure;
    internal bool CleanupQualified { get; private set; } = true;
    private static MemorySourcePin Entry()
    {
        var pin = MemorySourcePin.Capture(Path.Combine(AppContext.BaseDirectory, "development_startup.py"));
        if (pin.Sha256 != EntrySha) throw new InvalidDataException("開發啟動 helper 指紋不符。");
        return pin;
    }
    internal static MemoryStartupAvailability CheckAvailability(LauncherMenuPreferences preferences)
    {
        try
        {
            preferences.Validate();
            if (preferences.NativeService != NativeServiceMode.StandardArena) throw new ArgumentException("本版使用正式一般無界擂台模式。");
            if (!preferences.ClientDirectory.Equals(@"D:\lineage開發\0-客戶端", StringComparison.OrdinalIgnoreCase))
                throw new ArgumentException("共用資源版請選擇 D:\\lineage開發\\0-客戶端。");
            var selected = MemorySourcePin.Capture(Path.Combine(preferences.ClientDirectory, "LiTo.bin"));
            if (selected.Sha256 != LauncherService.ExpectedProtectedSha256)
                throw new InvalidDataException("指定客戶端的 LiTo.bin 指紋不符。");
            selected.Verify(); Entry().Verify(); FormalStandardSources.Reviewed.Verify();
            return new(MemoryStartupAvailabilityKind.Available, "可啟動正式 Server 與 LiTo.bin（來源唯讀，僅保存勝敗）。", null);
        }
        catch (Exception error) { return new(MemoryStartupAvailabilityKind.SourceValidationFailed, error.Message, error); }
    }
    internal async Task RunAsync(LauncherMenuPreferences preferences, CancellationToken cancellation, int sourceId)
    {
        if (!SourceServerChoice.Load().Any(source => source.Id == sourceId)) throw new InvalidOperationException("選擇的來源伺服器未開放。");
        var available = CheckAvailability(preferences);
        if (!available.CanStart) throw available.Error!;
        var runId = "development-" + Guid.NewGuid().ToString("N");
        var receipt = Path.Combine(Root, "gui-runs", runId);
        Directory.CreateDirectory(receipt);
        using var current = Process.GetCurrentProcess();
        MemoryStartupJson.WriteExclusive(Path.Combine(receipt, "request.json"), new
        {
            run_id = runId, launcher_identity = MemoryProcessIdentity.Capture(current), selected_preferences = preferences, selected_source_id = sourceId,
            created_at_utc = DateTimeOffset.UtcNow, source_entry = Entry(), session_ceiling_seconds = (int?)null
        });
        Exception? failure = null;
        try
        {
            var sources = FormalStandardSources.Reviewed;
            var command = OwnedMemoryStartupProcess.ServerCommand(sources.Python, Entry(), runId);
            foreach (var value in new[] { "--client-directory", preferences.ClientDirectory, "--scale", preferences.WindowScalePercent.ToString(),
                "--service", preferences.NativeService.ToString(), "--source-id", sourceId.ToString() }) command.ArgumentList.Add(value);
            CleanupQualified = false;
            helper = new(command, line =>
            {
                log(line);
                var marker = line.IndexOf("[LiTo-Extended-Stats] ", StringComparison.Ordinal);
                if (marker >= 0 && ExtendedCharacterStatsProtocol.TryParse(line[marker..], out var snapshot)) extendedStats?.Invoke(snapshot);
            });
            log("[正式啟動] 新場次 " + runId);
            var ready = await helper.AwaitEventAsync("ready", TimeSpan.FromSeconds(40), cancellation);
            if (ready.GetProperty("database_mode").GetString() != "lito_mysql"
                || !ready.GetProperty("database_read_only").GetBoolean()
                || ready.GetProperty("selected_source_id").GetInt32() != sourceId
                || ready.GetProperty("service_mode").GetString() != "standard"
                || !ready.GetProperty("sql_connected").GetBoolean())
                throw new InvalidDataException("SQL 資料來源尚未通過啟動驗證。");
            FormalStandardReady.Validate(ready, runId, ServerRuns, sources.Java.Path);
            var selectedSource = SourceServerChoice.Load().Single(source => source.Id == sourceId);
            var sourceEndpoints = ready.GetProperty("source_endpoints").EnumerateArray()
                .Where(endpoint => endpoint.GetProperty("source_id").GetInt32() == sourceId).ToArray();
            int sourceOwner = ready.GetProperty("instances")[0].GetProperty("identity").GetProperty("pid").GetInt32();
            if (sourceEndpoints.Length != 2 || !sourceEndpoints.Select(endpoint => endpoint.GetProperty("kind").GetString()).Order().SequenceEqual(new[] { "GAME", "LOGIN" })
                || sourceEndpoints.Any(endpoint => endpoint.GetProperty("port").GetInt32() != selectedSource.Port
                    || endpoint.GetProperty("owner").GetProperty("pid").GetInt32() != sourceOwner
                    || !endpoint.GetProperty("native_key_received").GetBoolean()))
                throw new InvalidDataException("所選來源的登入／遊戲入口尚未就緒。");
            foreach (var instance in ready.GetProperty("instances").EnumerateArray())
            {
                var identity = instance.GetProperty("identity");
                var full7 = identity.GetProperty("full7").Deserialize<MemoryProcessIdentity>(MemoryStartupJson.Options)!;
                sources.ValidateJvmIdentity(full7, identity.GetProperty("pid").GetInt32(), identity.GetProperty("birth_filetime").GetInt64(),
                    helper.Identity.Pid, instance.GetProperty("argv").Deserialize<string[]>()!,
                    instance.GetProperty("config").GetProperty("path").GetString()!, runId, instance.GetProperty("role").GetString()!, ServerRuns);
                if (full7.ParentPid != helper.Identity.Pid || full7.Pid != identity.GetProperty("pid").GetInt32()
                    || full7.Filetime != identity.GetProperty("birth_filetime").GetInt64().ToString()
                    || !full7.Path.Equals(sources.Java.Path, StringComparison.OrdinalIgnoreCase) || full7.ImageSha256 != sources.Java.Sha256)
                    throw new InvalidDataException("開發 Server 的當次程序身分不符。");
            }
            MemoryStartupJson.WriteExclusive(Path.Combine(receipt, "server-ready.json"), ready);
            log("[正式啟動] 正式 standard 已就緒：共用天堂資料唯讀，僅保存 LiTo 勝敗戰績；四個邏輯頻道，開始原版客戶端。");
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
                log($"[正式啟動] LiTo.bin PID {observed.Pid} 已建立；尚不代表畫面或登入驗收。");
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
                run_id = runId, cleanup_qualified = CleanupQualified, ownership_retained = OwnershipRetained,
                failure = failure?.ToString(), helper_result = finished
            });
            if (!CleanupQualified) throw new InvalidOperationException("本次開發啟動清理尚未確認，保留控制。");
            if (failure is null && helper?.ObservedExitCode != 0) throw new InvalidOperationException("原版啟動 helper 回報失敗，請查看本次日誌。");
        }
    }
}
