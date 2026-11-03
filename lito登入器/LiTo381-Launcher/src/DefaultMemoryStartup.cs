using System.Text.Json;

namespace LiTo381Launcher;

internal sealed class DefaultMemoryStartup(Action<string> log, Action<string, object>? writeDiagnostic = null)
{
    private OwnedMemoryStartupProcess? server, bridge;
    private bool bridgeCreationAttempted;
    private bool nativeReadyQualified;
    private bool ChildOwnershipRetained => server?.OwnershipRetained == true || bridge?.OwnershipRetained == true
        || OwnedMemoryStartupProcess.HasRetainedCaptureFailure;
    internal bool OwnershipRetained => ChildOwnershipRetained || preChildCleanupFailed || ((serverCreationAttempted || bridgeCreationAttempted) && !CleanupQualified);
    internal bool PreChildCleanupPending => preChildCleanupFailed;
    internal bool CleanupQualified { get; private set; } = true;
    internal bool TerminalAccepted { get; private set; }
    internal string? ContextDirectory { get; private set; }
    internal Exception? LastOriginalFailure { get; private set; }
    internal Exception? DiagnosticWriteFailure { get; private set; }
    internal Exception? TerminalReceiptWriteFailure { get; private set; }
    internal MemorySourcePin? TerminalReceiptPin { get; private set; }
    internal MemorySourcePin? RootControllerTerminalPin { get; private set; }
    internal bool RootControllerTerminalRequired { get; private set; }
    private MemoryGuiControllerTransport? controllerTransport;
    private Exception? cleanupFailure;
    private MemorySourcePin? serverResultPin;
    private JsonElement? serverResult, nativeReadyEvent;
    private bool serverCreationAttempted;
    private bool preChildHandoffCompleted;
    private bool preChildCleanupFailed;
    private int diagnosticSequence;

    internal async Task RunAsync(LauncherMenuPreferences preferences, CancellationToken cancellation, string? contextRoot = null,
        Func<CancellationToken, Task>? beforeChildren = null)
    {
        // The real Start owns a fresh context even when a supplier has not released its source.
        var context = new MemoryStartContext(preferences, contextRoot);
        ContextDirectory = context.Directory;
        SafeLog("[啟動] " + context.Directory);
        MemoryStartupSources? sources = null;
        Exception? original = null;
        var phase = "source-preflight";
        try
        {
            RootControllerTerminalRequired = MemoryGuiControllerTransport.IsRequested;
            phase = "controller-terminal-transport";
            controllerTransport = MemoryGuiControllerTransport.Capture(context);
            phase = "source-preflight";
            sources = DefaultMemoryStartupRelease.RequireReviewed(preferences);
            cancellation.ThrowIfCancellationRequested();
            phase = "owned-test-pair-cleanup";
            if (beforeChildren is not null)
            {
                try { await beforeChildren(cancellation); }
                catch { preChildCleanupFailed = true; CleanupQualified = false; throw; }
            }
            preChildHandoffCompleted = true;
            cancellation.ThrowIfCancellationRequested();
            phase = "server-command";
            var serverCommand = OwnedMemoryStartupProcess.ServerCommand(sources.Python, sources.ServerEntry, context.RunId);
            phase = "server-creation";
            server = StartStage(context, () => { serverCreationAttempted = true; CleanupQualified = false; return new(serverCommand, log); }, cancellation);
            phase = "server-ready";
            var ready = MemoryStartupReady.Validate(await server.AwaitEventAsync("ready", context.ReadyRemaining, cancellation), context.RunId, sources);
            var readyPin = MemoryStartupJson.WriteExclusive(Path.Combine(context.Directory, "memory-ready.json"), ready);
            cancellation.ThrowIfCancellationRequested();
            if (!server.IsAlive) throw new InvalidOperationException("Memory Server exited before native request.");
            var request = MemoryStartupJson.WriteExclusive(Path.Combine(context.Directory, "native-request.json"), new
            {
                schema = "LITO_DEFAULT_MEMORY_NATIVE_REQUEST_1", run_id = context.RunId,
                launcher_identity = context.LauncherIdentity, launcher_assembly = context.Assembly,
                selected_preferences = context.Preferences, reviewed_lito_runtime_directory = sources.ReviewedLiToRuntimeDirectory,
                memory_server_owner = server.Identity, memory_ready = readyPin, created_at_utc = context.CreatedAtUtc,
                source_review = sources.RootSourceReview, original_native_admission_required = true,
                runtime_authorized = false, product_accepted = false
            });
            var bridgeCommand = OwnedMemoryStartupProcess.NativeCommand(sources.Python, sources.NativeBridge, request);
            phase = "bridge-creation";
            bridge = StartStage(context, () => { bridgeCreationAttempted = true; return new(bridgeCommand, log); }, cancellation);
            phase = "native-admission";
            var nativeReady = bridge.AwaitEventAsync("native_ready", context.ReadyRemaining, cancellation);
            if (await Task.WhenAny(nativeReady, server.Exit, server.ProtocolEnded) != nativeReady)
                throw new InvalidOperationException("Memory Server exited during original native admission.");
            nativeReadyEvent = await nativeReady;
            ValidateNativeReady(nativeReadyEvent.Value, context, sources);
            nativeReadyQualified = true;
            log("[啟動] 原版已取得本次啟動授權並觀察到視窗。");
            var remaining = context.SessionRemaining;
            if (remaining <= TimeSpan.Zero) throw new TimeoutException("Original memory session ceiling expired.");
            var cancelledOrExpired = Task.Delay(remaining, cancellation);
            var winner = await Task.WhenAny(bridge.Exit, server.Exit, server.ProtocolEnded, bridge.ProtocolEnded, cancelledOrExpired);
            if (winner == server.Exit || winner == server.ProtocolEnded) throw new InvalidOperationException("Memory Server exited or lost its protocol while native session was active.");
            if (winner == bridge.ProtocolEnded && !bridge.Exit.IsCompleted)
                await bridge.Exit.WaitAsync(TimeSpan.FromSeconds(MemoryStartContext.CleanupWaitSeconds), cancellation);
            if (winner == cancelledOrExpired)
            {
                cancellation.ThrowIfCancellationRequested();
                throw new TimeoutException("Original memory session ceiling expired.");
            }
            await bridge.StopAsync();
            ValidateBridgeCleanup(bridge, context.RunId);
            if (!bridge.Finished!.Value.GetProperty("accepted").GetBoolean() || bridge.ObservedExitCode != 0)
                throw new InvalidOperationException("Root bridge did not accept this original native session.");
        }
        catch (Exception error)
        {
            original = LastOriginalFailure = error;
            RecordFailure(context, phase, "before-cleanup", error);
            throw;
        }
        finally
        {
            try
            {
                // A refused source/pre-child request created no game helpers to clean up.
                if (sources is not null && (serverCreationAttempted || bridgeCreationAttempted))
                    await CleanupAsync(context.RunId, sources);
            }
            catch (Exception cleanupError)
            {
                cleanupFailure = cleanupError;
                LastOriginalFailure ??= cleanupError;
                RecordFailure(context, "cleanup", "cleanup-failed", cleanupError);
                if (original is not null) throw new AggregateException("Startup failed and owned cleanup remains unresolved.", original, cleanupError);
                throw;
            }
            finally
            {
                if (original is not null) RecordFailure(context, phase, "after-cleanup", original);
                PublishTerminalReceipt(context, sources);
            }
        }
    }

    internal static async Task PrepareOwnedTestPairAsync(Func<Task> stopAndAwait, Func<bool> cleanupQualified,
        CancellationToken cancellation)
    {
        cancellation.ThrowIfCancellationRequested();
        Exception? failure = null;
        try { await stopAndAwait(); }
        catch (Exception error) { failure = error; }
        // Terminal acceptance is distinct from verified release of this controller's resources.
        if (!cleanupQualified()) throw new InvalidOperationException("本次測試伺服器尚未確認停止，請重試清理後再啟動遊戲。", failure);
        cancellation.ThrowIfCancellationRequested();
    }

    private void RecordFailure(MemoryStartContext context, string phase, string position, Exception error)
    {
        try
        {
            var path = Path.Combine(context.Directory, $"failure-diagnostic-{++diagnosticSequence:00}.json");
            var value = new
            {
                schema = "LITO_DEFAULT_MEMORY_START_FAILURE_1", run_id = context.RunId,
                startup_request = Path.Combine(context.Directory, "startup-request.json"), phase, position,
                observed_at_utc = DateTimeOffset.UtcNow,
                error = new { type = error.GetType().FullName, error.Message, error.HResult, original = error.ToString() },
                original_failure = LastOriginalFailure?.ToString(),
                server_creation_attempted = serverCreationAttempted, bridge_creation_attempted = bridgeCreationAttempted,
                pre_child_handoff_completed = preChildHandoffCompleted,
                server_captured = server is not null, bridge_captured = bridge is not null,
                server_ownership_retained = server?.OwnershipRetained == true,
                bridge_ownership_retained = bridge?.OwnershipRetained == true,
                capture_failure_retained = OwnedMemoryStartupProcess.HasRetainedCaptureFailure,
                cleanup_qualified = CleanupQualified, terminal_accepted = TerminalAccepted,
                native_ready_qualified = nativeReadyQualified, runtime_authorized = false, product_accepted = false
            };
            if (writeDiagnostic is null) MemoryStartupJson.WriteExclusive(path, value);
            else writeDiagnostic(path, value);
        }
        catch (Exception diagnosticError)
        {
            DiagnosticWriteFailure = diagnosticError;
            SafeLog("啟動紀錄尚未儲存：" + diagnosticError.Message);
        }
    }

    private void SafeLog(string message) { try { log(message); } catch { /* Logging cannot replace failure or prevent cleanup. */ } }

    private void PublishTerminalReceipt(MemoryStartContext context, MemoryStartupSources? sources)
    {
        try
        {
            var neverNative = !bridgeCreationAttempted;
            var finished = bridge?.Finished;
            bool? ObservedBool(string key) => finished is JsonElement value && value.TryGetProperty(key, out var field)
                && field.ValueKind is JsonValueKind.True or JsonValueKind.False ? field.GetBoolean() : null;
            var gameAccepted = TerminalAccepted && ObservedBool("native_created") == true && ObservedBool("native_exit_observed") == true
                && ObservedBool("native_forced") == false && ObservedBool("native_normal_exit") == true;
            TerminalReceiptPin = MemoryStartupJson.WriteExclusive(Path.Combine(context.Directory, "terminal-receipt.json"), new
            {
                schema = "LITO_DEFAULT_MEMORY_GAME_TERMINAL_1", run_id = context.RunId, startup_request = context.StartupRequest,
                launcher_identity = context.LauncherIdentity, launcher_assembly = context.Assembly,
                source_review = sources?.RootSourceReview, source_pins = sources,
                controller_handoff = controllerTransport?.HandoffPin,
                settled_at_utc = DateTimeOffset.UtcNow,
                pre_child_handoff_completed = preChildHandoffCompleted, pre_child_cleanup_failed = preChildCleanupFailed,
                server_creation_attempted = serverCreationAttempted, bridge_creation_attempted = bridgeCreationAttempted,
                server_identity = server?.Identity, bridge_identity = bridge?.Identity,
                server_finished = server?.Finished, bridge_finished = finished,
                server_result_pin = serverResultPin, server_result = serverResult, native_ready_event = nativeReadyEvent,
                server_exit_observed = server?.ObservedExitCode is not null, server_exit_code = server?.ObservedExitCode,
                bridge_exit_observed = bridge?.ObservedExitCode is not null, bridge_exit_code = bridge?.ObservedExitCode,
                server_ownership_retained = server?.OwnershipRetained, bridge_ownership_retained = bridge?.OwnershipRetained,
                native_created = neverNative ? false : ObservedBool("native_created"),
                native_exit_observed = neverNative ? false : ObservedBool("native_exit_observed"),
                native_forced = neverNative ? false : ObservedBool("native_forced"),
                native_normal_exit = neverNative ? false : ObservedBool("native_normal_exit"),
                native_ready_qualified = nativeReadyQualified, terminal_accepted = TerminalAccepted, game_accepted = gameAccepted,
                cleanup_qualified = CleanupQualified, ownership_retained = OwnershipRetained,
                original_failure = LastOriginalFailure?.ToString(), diagnostic_write_error = DiagnosticWriteFailure?.ToString(),
                cleanup_failure = cleanupFailure?.ToString(),
                runtime_authorized = false, product_accepted = false
            });
            if (controllerTransport is not null)
                RootControllerTerminalPin = controllerTransport.Publish(context, TerminalReceiptPin, CleanupQualified, OwnershipRetained, gameAccepted);
        }
        catch (Exception error) { TerminalReceiptWriteFailure = error; SafeLog("本次結束紀錄尚未儲存：" + error.Message); }
    }

    private async Task CleanupAsync(string runId, MemoryStartupSources sources)
    {
        var failures = new List<Exception>();
        var terminalFailures = new List<Exception>();
        var bridgeAccepted = false;
        if (bridgeCreationAttempted && bridge is null)
            throw new InvalidOperationException("Created bridge could not be captured; qualified lifecycle/cleanup is unavailable. Memory suppliers retained.");
        // Original bridge cleanup/Root ACK must complete before its memory suppliers are stopped.
        if (bridge is not null)
        {
            try { await bridge.StopAsync(); ValidateBridgeCleanup(bridge, runId); bridgeAccepted = bridge.Finished!.Value.GetProperty("accepted").GetBoolean(); }
            catch (Exception error) { failures.Add(error); }
            if (bridge.OwnershipRetained || failures.Count != 0)
                throw new AggregateException("Native bridge cleanup unresolved; memory suppliers retained.", failures);
        }
        if (server is not null)
        {
            try
            {
                await server.StopAsync();
                var finished = server.Finished ?? throw new InvalidDataException("Memory Server cleanup receipt absent.");
                var resultPath = Path.Combine(sources.ServerRunsDirectory, runId, "result.json");
                if (!Path.GetFullPath(finished.GetProperty("result").GetString()!).Equals(resultPath, StringComparison.OrdinalIgnoreCase))
                    throw new InvalidDataException("Memory Server terminal result is not this owned run.");
                var resultBytes = File.ReadAllBytes(resultPath);
                serverResultPin = new MemorySourcePin(Path.GetFullPath(resultPath), resultBytes.LongLength,
                    Convert.ToHexString(System.Security.Cryptography.SHA256.HashData(resultBytes)));
                using var result = JsonDocument.Parse(resultBytes);
                serverResult = result.RootElement.Clone();
                var terminal = AssessServerTerminal(result.RootElement, finished, server.ObservedExitCode);
                if (!terminal.CleanupQualified)
                    throw new InvalidDataException("Memory Server reverse cleanup unqualified.");
                TerminalAccepted = nativeReadyQualified && bridgeAccepted && bridge?.ObservedExitCode == 0 && terminal.Accepted;
                if (!terminal.Accepted)
                    terminalFailures.Add(new InvalidOperationException("Memory Server reported a failed terminal run: " + string.Join("; ", terminal.Errors)));
            }
            catch (Exception error) { failures.Add(error); }
        }
        CleanupQualified = failures.Count == 0 && !ChildOwnershipRetained;
        if (failures.Count != 0) throw new AggregateException("Owned startup cleanup/terminal qualification failed.", failures);
        log("[清理] 原版橋接及隔離記憶體伺服器已完成本次清理。");
        if (terminalFailures.Count != 0) throw new AggregateException("Memory Server terminal run failed after qualified cleanup.", terminalFailures);
    }

    internal static OwnedMemoryStartupProcess StartStage(MemoryStartContext context, Func<OwnedMemoryStartupProcess> create, CancellationToken cancellation)
    {
        cancellation.ThrowIfCancellationRequested();
        if (context.ReadyRemaining <= TimeSpan.Zero) throw new TimeoutException("Original memory startup deadline expired before helper creation.");
        return create();
    }

    internal static void ValidateNativeExecutablePath(string nativePath, string reviewedRuntimeDirectory)
    {
        if (!Path.IsPathFullyQualified(reviewedRuntimeDirectory) || !Path.IsPathFullyQualified(nativePath)
            || !Path.GetFullPath(nativePath).Equals(Path.Combine(Path.GetFullPath(reviewedRuntimeDirectory), "LiTo.bin"), StringComparison.OrdinalIgnoreCase))
            throw new InvalidDataException("Root bridge native executable does not match the separately reviewed runtime path.");
    }

    internal static void ValidateNativeReady(JsonElement ready, MemoryStartContext context, MemoryStartupSources sources)
    {
        if (ready.TryGetProperty("fixture_only", out _) || ready.GetProperty("event").GetString() != "native_ready"
            || ready.GetProperty("run_id").GetString() != context.RunId || ready.GetProperty("main_window_handle").GetInt64() <= 0)
            throw new InvalidDataException("Current original native admission/window evidence required.");
        var identity = ready.GetProperty("native").Deserialize<MemoryProcessIdentity>(MemoryStartupJson.Options)!;
        if (identity.Pid <= 0 || !long.TryParse(identity.Filetime, out var birth) || birth <= 0 || identity.ParentPid <= 0
            || new[] { identity.ImageSha256, identity.CommandSha256, identity.CommandArgvDigest }.Any(x => x.Length != 64 || x.Any(c => !"0123456789ABCDEF".Contains(c))))
            throw new InvalidDataException("Root bridge native full7 identity is unqualified.");
        ValidateNativeExecutablePath(identity.Path, sources.ReviewedLiToRuntimeDirectory);
        ready.GetProperty("admission_receipt").Deserialize<MemorySourcePin>(MemoryStartupJson.Options)!.Verify();
    }

    private static void ValidateBridgeCleanup(OwnedMemoryStartupProcess owned, string runId)
    {
        var finished = owned.Finished ?? throw new InvalidDataException("Original bridge cleanup receipt absent.");
        if (finished.TryGetProperty("fixture_only", out _) || finished.GetProperty("event").GetString() != "finished"
            || finished.GetProperty("run_id").GetString() != runId)
            throw new InvalidDataException("Original native/Root ACK cleanup unqualified.");
        ValidateBridgeLifecycle(ReadBridgeLifecycle(finished, owned.NativeReadyObserved));
    }
    internal static BridgeLifecycle ReadBridgeLifecycle(JsonElement finished, bool nativeReadyObserved)
    {
        return new(
            finished.GetProperty("native_created").GetBoolean(), finished.GetProperty("native_exit_observed").GetBoolean(),
            finished.GetProperty("cleanup_verified").GetBoolean(), finished.GetProperty("accepted").GetBoolean(),
            nativeReadyObserved,
            finished.TryGetProperty("native", out var native) && native.ValueKind != JsonValueKind.Null,
            finished.TryGetProperty("admission_receipt", out var receipt) && receipt.ValueKind != JsonValueKind.Null);
    }

    internal sealed record BridgeLifecycle(bool NativeCreated, bool NativeExitObserved, bool CleanupVerified,
        bool Accepted, bool NativeReadyObserved, bool HasNativeIdentity, bool HasAdmissionReceipt);
    internal static void ValidateBridgeLifecycle(BridgeLifecycle state)
    {
        if (!state.CleanupVerified || (state.NativeCreated && !state.NativeExitObserved)
            || (!state.NativeCreated && (state.NativeExitObserved || state.NativeReadyObserved || state.HasNativeIdentity || state.HasAdmissionReceipt || state.Accepted))
            || (state.Accepted && !state.NativeReadyObserved))
            throw new InvalidDataException("Unknown or contradictory original native creation/exit/cleanup lifecycle; memory suppliers must be retained.");
    }

    internal sealed record ServerTerminal(bool CleanupQualified, bool Accepted, string[] Errors);
    internal static ServerTerminal AssessServerTerminal(JsonElement result, JsonElement finished, int? observedExitCode)
    {
        var errors = new List<string>();
        var cleanupErrors = ErrorCount(result, "cleanup_errors", required: true);
        if (result.TryGetProperty("failure", out var failure) && failure.ValueKind != JsonValueKind.Null)
            errors.Add("failure=" + failure);
        foreach (var key in new[] { "execution_errors", "publication_errors" })
            if (ErrorCount(result, key) != 0) errors.Add(key);
        if (cleanupErrors != 0) errors.Add("cleanup_errors");
        var cleanup = result.GetProperty("cleanup_verified").GetBoolean() && cleanupErrors == 0;
        var acceptedResult = result.GetProperty("accepted").GetBoolean();
        var state = result.GetProperty("state").GetString();
        if (state != (acceptedResult ? "sealed-success" : "sealed-failed"))
            throw new InvalidDataException("Malformed Server terminal state/acceptance.");
        if (finished.GetProperty("event").GetString() != "finished" || finished.GetProperty("product_complete").GetBoolean())
            throw new InvalidDataException("Malformed Server finished event.");
        if (observedExitCode is null || finished.GetProperty("exit_code").GetInt32() != observedExitCode)
            throw new InvalidDataException("Server reported exit does not match retained helper exit.");
        if (observedExitCode != 0 || !finished.GetProperty("accepted").GetBoolean() || !acceptedResult)
            errors.Add("Server exit/result/finished acceptance failed");
        if (acceptedResult && (!result.GetProperty("startup_ready_verified").GetBoolean() || !cleanup))
            errors.Add("Server success contradicts startup/cleanup");
        return new(cleanup, cleanup && errors.Count == 0, errors.ToArray());
    }
    private static int ErrorCount(JsonElement result, string key, bool required = false)
    {
        if (!result.TryGetProperty(key, out var errors))
        {
            if (required) throw new InvalidDataException("Server terminal field absent: " + key);
            return 0;
        }
        if (errors.ValueKind != JsonValueKind.Array) throw new InvalidDataException("Malformed Server error array: " + key);
        return errors.GetArrayLength();
    }
}
