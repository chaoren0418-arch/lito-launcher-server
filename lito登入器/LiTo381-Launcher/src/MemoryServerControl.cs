using System.Diagnostics;
using System.Text.Json;

namespace LiTo381Launcher;

internal enum MemoryServerControlState { Idle, Preparing, Ready, Stopping, Stopped, Failed }

// Independent ordinary memory test pair; no game service preference, claim, lease or native bridge.
internal sealed class MemoryServerControl
{
    internal const int SetupSeconds = 45, SessionSeconds = 900, CleanupSeconds = 10;
    private readonly Action<string> log;
    private readonly Func<ProcessStartInfo, OwnedMemoryStartupProcess> createOwned;
    private readonly Func<MemoryServerControlSources> sources;
    private readonly string contextRoot;
    private readonly SemaphoreSlim lifetime = new(1, 1);
    private OwnedMemoryStartupProcess? helper;
    private MemoryServerControlSources? activeSources;
    private CancellationTokenSource? stop;
    private Stopwatch? clock;
    private bool creationAttempted;
    private string phase = "idle";
    private int diagnosticSequence;
    private bool readyPublished;
    private readonly Action<string, object> writeDiagnostic;
    internal Exception? LastOriginalFailure { get; private set; }
    internal string? DiagnosticWriteFailure { get; private set; }
    internal string? LogFailure { get; private set; }
    internal MemoryServerControlState State { get; private set; }
    internal string Message { get; private set; } = "尚未啟動記憶體測試伺服器。";
    internal string? RunId { get; private set; }
    internal bool CleanupQualified { get; private set; } = true;
    internal bool TerminalAccepted { get; private set; }
    internal bool OwnershipRetained => (creationAttempted && !CleanupQualified) || helper?.OwnershipRetained == true || OwnedMemoryStartupProcess.HasRetainedCaptureFailure;
    internal bool CanStart => !OwnershipRetained && State is not (MemoryServerControlState.Preparing or MemoryServerControlState.Ready or MemoryServerControlState.Stopping);
    internal bool CanClose => !OwnershipRetained && State is not (MemoryServerControlState.Preparing or MemoryServerControlState.Stopping);
    internal event Action? Changed;
    internal MemoryServerControl(Action<string> log, Func<MemoryServerControlSources>? sources = null,
        Func<ProcessStartInfo, OwnedMemoryStartupProcess>? createOwned = null, string? contextRoot = null,
        Action<string, object>? writeDiagnostic = null)
    {
        this.log = log;
        this.sources = sources ?? (() => MemoryServerControlSources.Reviewed);
        this.createOwned = createOwned ?? (command => new(command, log));
        this.contextRoot = contextRoot ?? Path.Combine(LauncherSettingsStore.SettingsDirectory, "memory-server-control");
        this.writeDiagnostic = writeDiagnostic ?? ((path, value) => MemoryStartupJson.WriteExclusive(path, value));
    }
    private void SetState(MemoryServerControlState state, string message)
    {
        State = state; Message = message; SafeLog("[記憶體測試伺服器] " + message);
        try { Changed?.Invoke(); }
        catch (Exception error) { LogFailure = error.ToString(); }
    }
    internal void RequestStop() => stop?.Cancel();
    internal async Task StartAsync()
    {
        await lifetime.WaitAsync();
        try
        {
            if (!CanStart) throw new InvalidOperationException("This owned memory test run is still active or has unqualified cleanup.");
            helper = null; activeSources = null; creationAttempted = false; CleanupQualified = true; TerminalAccepted = false;
            RunId = "launcher-server-" + Guid.NewGuid().ToString("N");
            readyPublished = false; diagnosticSequence = 0; LastOriginalFailure = null; DiagnosticWriteFailure = null;
            SetState(MemoryServerControlState.Preparing, "正在準備本次 battle/source 記憶體測試伺服器…");
            clock = Stopwatch.StartNew(); stop?.Dispose(); stop = new();
            phase = "source-verification";
            activeSources = sources(); activeSources.Verify();
            phase = "control-request-publication";
            var directory = Path.Combine(contextRoot, RunId);
            Directory.CreateDirectory(directory);
            using var gui = Process.GetCurrentProcess();
            MemoryStartupJson.WriteExclusive(Path.Combine(directory, "control-request.json"), new {
                schema = "LITO_MEMORY_SERVER_CONTROL_REQUEST_1", run_id = RunId,
                launcher = MemoryProcessIdentity.Capture(gui), launcher_assembly = MemorySourcePin.Capture(typeof(MemoryServerControl).Assembly.Location),
                sources = activeSources, scope = "rental-memory-test-pair-only", created_at_utc = DateTimeOffset.UtcNow,
                game_preferences_consumed = false, native_started = false, runtime_game_authorized = false, product_accepted = false });
            phase = "helper-command";
            var command = OwnedMemoryStartupProcess.ServerCommand(activeSources.Python, activeSources.Entry, RunId);
            stop.Token.ThrowIfCancellationRequested();
            if (RemainingSetup <= TimeSpan.Zero) throw new TimeoutException("Original memory test setup bound expired before helper creation.");
            creationAttempted = true; CleanupQualified = false; TerminalAccepted = false;
            phase = "helper-creation";
            helper = createOwned(command);
            phase = "await-ready";
            var ready = await helper.AwaitEventAsync("ready", RemainingSetup, stop.Token);
            phase = "ready-validation";
            activeSources.ValidateReady(ready, RunId, helper.Identity);
            if (!helper.IsAlive) throw new InvalidOperationException("Owned memory test helper exited before READY could be used.");
            phase = "ready-publication";
            MemoryStartupJson.WriteExclusive(Path.Combine(directory, "owned-memory-ready.json"), ready);
            readyPublished = true; phase = "ready";
            SetState(MemoryServerControlState.Ready, "本次記憶體測試伺服器已 READY：battle/source，四個端點已驗證。未啟動遊戲。");
            _ = WatchAsync();
        }
        catch (Exception error)
        {
            LastOriginalFailure = error;
            RecordDiagnostic(error, phase, "before-cleanup");
            try { await CleanupCoreAsync(); }
            catch (Exception cleanup)
            {
                RecordDiagnostic(cleanup, phase, "cleanup-failure");
                SetState(MemoryServerControlState.Failed, error.Message + "；清理尚未確認：" + cleanup.Message);
                throw new AggregateException(error, cleanup);
            }
            RecordDiagnostic(null, phase, "cleanup-complete");
            SetState(MemoryServerControlState.Failed, error is OperationCanceledException ? "本次準備已取消，擁有的伺服器已清理。" : error.Message);
            throw;
        }
        finally { lifetime.Release(); }
    }
    private TimeSpan RemainingSetup => TimeSpan.FromSeconds(SetupSeconds) - clock!.Elapsed;
    private async Task WatchAsync()
    {
        var owned = helper!;
        var cancellation = stop!;
        var remaining = TimeSpan.FromSeconds(SessionSeconds) - clock!.Elapsed;
        var expiry = Task.Delay(remaining > TimeSpan.Zero ? remaining : TimeSpan.Zero, cancellation.Token);
        var winner = await Task.WhenAny(owned.Exit, owned.ProtocolEnded, expiry);
        if (cancellation.IsCancellationRequested) return;
        await lifetime.WaitAsync();
        try
        {
            if (!ReferenceEquals(helper, owned) || CleanupQualified) return;
            SetState(MemoryServerControlState.Stopping, "本次伺服器限時結束或控制流程中斷，正在清理…");
            await CleanupCoreAsync();
            SetState(winner == expiry ? MemoryServerControlState.Stopped : MemoryServerControlState.Failed,
                winner == expiry ? "本次 900 秒限時已結束並完成清理。" : "本次伺服器控制流程提早結束，擁有的程序已清理。");
        }
        catch (Exception error) { RecordDiagnostic(error, phase, "watch-cleanup-failure"); SetState(MemoryServerControlState.Failed, "本次伺服器中斷／清理失敗：" + error.Message); }
        finally { lifetime.Release(); }
    }
    internal async Task StopAsync()
    {
        RequestStop(); // Interrupt the same preparing wait before acquiring its lifetime lock.
        await lifetime.WaitAsync();
        try
        {
            if (!creationAttempted && helper is null) return;
            SetState(MemoryServerControlState.Stopping, "正在停止並驗證本次擁有的記憶體測試伺服器…");
            await CleanupCoreAsync();
            SetState(TerminalAccepted ? MemoryServerControlState.Stopped : MemoryServerControlState.Failed,
                TerminalAccepted ? "本次伺服器已停止，反向清理及程序結束已驗證。" : "本次伺服器已清理，但執行結果未通過。請查看記錄。");
        }
        catch (Exception error) { RecordDiagnostic(error, phase, "stop-cleanup-failure"); SetState(MemoryServerControlState.Failed, "本次清理尚未確認，保留控制：" + error.Message); throw; }
        finally { lifetime.Release(); }
    }
    private async Task CleanupCoreAsync()
    {
        if (!creationAttempted) return;
        CleanupQualified = false;
        if (helper is null) throw new InvalidOperationException("Owned helper capture failed; no qualified terminal receipt is available.");
        phase = "cleanup-helper-control-exit";
        await helper.StopAsync();
        phase = "cleanup-terminal-verification";
        var finished = helper.Finished ?? throw new InvalidDataException("Owned Server finished receipt absent.");
        var resultPath = Path.Combine(activeSources!.RunsDirectory, RunId!, "result.json");
        if (finished.TryGetProperty("fixture_only", out _) || !Path.GetFullPath(finished.GetProperty("result").GetString()!).Equals(resultPath, StringComparison.OrdinalIgnoreCase))
            throw new InvalidDataException("Server cleanup result is not this owned genuine run.");
        using var report = JsonDocument.Parse(File.ReadAllBytes(resultPath));
        ValidateCleanupReport(report.RootElement, RunId!);
        var terminal = DefaultMemoryStartup.AssessServerTerminal(report.RootElement, finished, helper.ObservedExitCode);
        CleanupQualified = terminal.CleanupQualified && !helper.OwnershipRetained;
        TerminalAccepted = terminal.Accepted;
        if (!CleanupQualified) throw new InvalidDataException("Owned Server reverse cleanup unqualified.");
        if (!TerminalAccepted) SafeLog("[記憶體測試伺服器結果] " + string.Join("; ", terminal.Errors));
    }
    private void SafeLog(string message)
    {
        try { log(message); }
        catch (Exception error) { LogFailure = error.ToString(); }
    }
    private void RecordDiagnostic(Exception? error, string failedPhase, string position)
    {
        try
        {
            if (RunId is null) return;
            var directory = Path.Combine(contextRoot, RunId);
            Directory.CreateDirectory(directory);
            writeDiagnostic(Path.Combine(directory, $"failure-diagnostic-{++diagnosticSequence:D2}.json"), new {
                schema = "LITO_MEMORY_SERVER_FAILURE_DIAGNOSTIC_1", run_id = RunId, at_utc = DateTimeOffset.UtcNow,
                phase = failedPhase, position, exception_type = error?.GetType().FullName,
                exception_message = error?.Message, exception_hresult = error?.HResult, exception_stack = error?.StackTrace,
                exception_full = error?.ToString(), original_exception_full = LastOriginalFailure?.ToString(),
                producer = "Python -B -X utf8", requested_decoder = new { name = ProtocolEncoding.WebName, code_page = ProtocolEncoding.CodePage },
                observed_decoders = helper?.Decoders, protocol_failure = helper?.ProtocolFailure,
                ready_event_received = helper?.ReadyEventObserved == true, ready_published = readyPublished,
                helper_created = helper is not null, helper_identity = helper?.Identity,
                helper_exit_observed = helper?.Exit.IsCompleted == true, helper_exit_code = helper?.ObservedExitCode,
                helper_handle_retained = helper?.OwnershipRetained == true, ownership_retained = OwnershipRetained,
                cleanup_qualified = CleanupQualified, terminal_accepted = TerminalAccepted,
                log_failure = LogFailure ?? helper?.LogFailure, native_started = false, product_accepted = false });
        }
        catch (Exception diagnosticError)
        {
            DiagnosticWriteFailure = diagnosticError.ToString();
            SafeLog("[診斷檔案未寫入] " + diagnosticError.Message);
        }
    }
    private static System.Text.Encoding ProtocolEncoding => OwnedMemoryStartupProcess.ProtocolEncoding;
    internal static void ValidateCleanupReport(JsonElement report, string runId)
    {
        if (report.TryGetProperty("fixture_only", out _) || report.GetProperty("run_id").GetString() != runId
            || report.GetProperty("product_complete").GetBoolean() || report.GetProperty("native_started").GetBoolean() || report.GetProperty("sql_started").GetBoolean()
            || report.GetProperty("owned_listeners_remaining").GetArrayLength() != 0
            || report.GetProperty("cleanup").EnumerateArray().Any(row => !row.GetProperty("exited").GetBoolean() || !row.GetProperty("handle_closed").GetBoolean()))
            throw new InvalidDataException("Current Server owned child/listener cleanup evidence is unqualified.");
    }
}
