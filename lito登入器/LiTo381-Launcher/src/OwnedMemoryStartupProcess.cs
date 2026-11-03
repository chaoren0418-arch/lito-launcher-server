using System.Diagnostics;
using System.Text;
using System.Text.Json;
using System.Threading.Channels;

namespace LiTo381Launcher;

// Only the creation-owned Server helper or reviewed Root bridge uses this control lifetime.
internal sealed class OwnedMemoryStartupProcess
{
    private readonly Process process;
    private readonly Channel<JsonElement> events = Channel.CreateUnbounded<JsonElement>();
    private readonly Task stdout, stderr;
    private readonly Action<string> log;
    private int stopRequested;
    private readonly SemaphoreSlim controlWrite = new(1, 1);
    internal async Task SendCommandAsync(string command)
    {
        await controlWrite.WaitAsync();
        try
        {
            if (!IsAlive || Volatile.Read(ref stopRequested) != 0) throw new InvalidOperationException("啟動場次已結束。");
            await process.StandardInput.WriteLineAsync(JsonSerializer.Serialize(new { command })).WaitAsync(TimeSpan.FromSeconds(2));
            await process.StandardInput.FlushAsync().WaitAsync(TimeSpan.FromSeconds(2));
        }
        finally { controlWrite.Release(); }
    }
    internal MemoryProcessIdentity Identity { get; }
    internal Task Exit { get; }
    internal Task ProtocolEnded => stdout;
    internal bool OwnershipRetained { get; private set; } = true;
    internal bool IsAlive => OwnershipRetained && !process.HasExited;
    private int? observedExitCode;
    internal int? ObservedExitCode => observedExitCode ?? (OwnershipRetained && process.HasExited ? process.ExitCode : null);
    internal JsonElement? Finished { get; private set; }
    internal bool NativeReadyObserved { get; private set; }
    internal bool ReadyEventObserved { get; private set; }
    internal sealed record ProtocolDecoders(string StdoutName, int StdoutCodePage, string StderrName, int StderrCodePage);
    internal ProtocolDecoders Decoders { get; private set; } = new("unobserved", 0, "unobserved", 0);
    internal string? ProtocolFailure { get; private set; }
    internal string? LogFailure { get; private set; }
    internal static Encoding ProtocolEncoding => new UTF8Encoding(false, true);
    internal string ErrorTail { get; private set; } = "";

    internal static ProcessStartInfo ServerCommand(MemorySourcePin python, MemorySourcePin entry, string runId)
    {
        var command = Command(python, entry);
        command.ArgumentList.Add("--serve"); command.ArgumentList.Add("--run-id"); command.ArgumentList.Add(runId);
        return command;
    }
    internal static ProcessStartInfo NativeCommand(MemorySourcePin python, MemorySourcePin bridge, MemorySourcePin request)
    {
        var command = Command(python, bridge);
        foreach (var value in new[] { "--launch-request", request.Path, "--request-bytes", request.Bytes.ToString(System.Globalization.CultureInfo.InvariantCulture), "--request-sha256", request.Sha256 })
            command.ArgumentList.Add(value);
        return command;
    }
    private static ProcessStartInfo Command(MemorySourcePin python, MemorySourcePin entry)
    {
        python.Verify(); entry.Verify();
        var command = new ProcessStartInfo(python.Path)
        {
            WorkingDirectory = Path.GetDirectoryName(entry.Path)!, UseShellExecute = false, CreateNoWindow = true,
            RedirectStandardInput = true, RedirectStandardOutput = true, RedirectStandardError = true,
            StandardOutputEncoding = ProtocolEncoding, StandardErrorEncoding = ProtocolEncoding
        };
        foreach (var value in new[] { "-B", "-X", "utf8", entry.Path }) command.ArgumentList.Add(value);
        return command;
    }
    internal OwnedMemoryStartupProcess(ProcessStartInfo command, Action<string> log,
        Func<Process, MemoryProcessIdentity>? capture = null,
        Action<Process>? recoveryClose = null, Func<Process, int, bool>? recoveryWait = null)
    {
        this.log = log;
        process = new Process { StartInfo = command };
        if (!process.Start()) { process.Dispose(); throw new InvalidOperationException("Unable to create owned startup helper."); }
        // Registration precedes every fallible capture/recovery operation on this newly acquired child.
        var retention = new CaptureFailureOwnership(process);
        RetainedStartupFailures.Add(retention);
        try
        {
            retention.CaptureLifetime();
            Identity = (capture ?? MemoryProcessIdentity.Capture)(process);
            CaptureDecoders();
            Exit = process.WaitForExitAsync();
            stdout = ReadOutput(); stderr = ReadErrors();
            RetainedStartupFailures.Remove(retention); // Ownership transferred only to this fully initialized object.
        }
        catch
        {
            retention.Recover(recoveryClose, recoveryWait); // Close/wait errors cannot bypass retention.
            throw;
        }
    }
    private static readonly List<CaptureFailureOwnership> RetainedStartupFailures = [];
    internal static bool HasRetainedCaptureFailure => RetainedStartupFailures.Count != 0;
    internal static IReadOnlyList<CaptureFailureOwnership> PendingCaptureFailures => RetainedStartupFailures.ToArray();
    internal static void RecoverCaptureFailures()
    {
        foreach (var retained in RetainedStartupFailures.ToArray().Reverse()) retained.Recover();
    }
    internal sealed class CaptureFailureOwnership(Process owned)
    {
        internal int? Pid { get; private set; }
        internal long? BirthFiletime { get; private set; }
        internal IntPtr? OriginalHandle { get; private set; }
        internal bool ExitVerified { get; private set; }
        internal bool HandleClosed { get; private set; }
        internal List<string> RecoveryErrors { get; } = [];
        internal void CaptureLifetime()
        {
            OriginalHandle = owned.Handle;
            Pid = owned.Id;
            BirthFiletime = owned.StartTime.ToUniversalTime().ToFileTimeUtc();
        }
        internal void Recover(Action<Process>? close = null, Func<Process, int, bool>? wait = null)
        {
            try { (close ?? (p => p.StandardInput.Close()))(owned); }
            catch (Exception error) { RecoveryErrors.Add("stdin close: " + error.Message); }
            try
            {
                ExitVerified = (wait ?? ((p, milliseconds) => p.WaitForExit(milliseconds)))(owned, MemoryStartContext.CleanupWaitSeconds * 1000)
                    && owned.HasExited;
            }
            catch (Exception error) { RecoveryErrors.Add("owned wait: " + error.Message); }
            if (!ExitVerified) return;
            try { owned.Dispose(); HandleClosed = true; }
            catch (Exception error) { RecoveryErrors.Add("owned dispose: " + error.Message); }
            if (HandleClosed) RetainedStartupFailures.Remove(this);
        }
    }
    private async Task ReadOutput()
    {
        Exception? failure = null;
        try
        {
            var count = 0;
            while (await process.StandardOutput.ReadLineAsync() is { } line)
            {
                CaptureDecoders();
                if (line.Length > 65536) throw new InvalidDataException("Owned startup protocol output limit exceeded.");
                using var document = JsonDocument.Parse(line);
                var value = document.RootElement.Clone();
                if (value.ValueKind != JsonValueKind.Object || !value.TryGetProperty("event", out var kind))
                    throw new InvalidDataException("Owned startup helper emitted invalid protocol output.");
                // A development session can return to source selection repeatedly.
                // Consume these progress receipts instead of accumulating them in
                // the one-shot readiness queue or exhausting its startup limit.
                if (kind.GetString() == "session_native_created")
                {
                    SafeLog($"[選服流程] 新 LiTo 工作階段 PID {value.GetProperty("identity").GetProperty("pid").GetInt32()} 已建立。");
                    continue;
                }
                if (kind.GetString() == "session_resources_ready")
                {
                    if (value.GetProperty("profile").GetString() != "tw13081901-v1"
                        || value.GetProperty("total_count").GetInt32() != 30285)
                        throw new InvalidDataException("Source session resource receipt mismatch.");
                    SafeLog("[選服流程] 共用資源已就緒，請於遊戲視窗繼續。");
                    continue;
                }
                if (kind.GetString() != "additional_client_result" && ++count > 64) throw new InvalidDataException("Owned startup protocol output limit exceeded.");
                if (kind.GetString() == "finished") Finished = value;
                if (kind.GetString() == "native_ready") NativeReadyObserved = true;
                if (kind.GetString() == "ready") ReadyEventObserved = true;
                await events.Writer.WriteAsync(value);
            }
        }
        catch (Exception error) { failure = error; ProtocolFailure = error.ToString(); }
        finally { events.Writer.TryComplete(failure); }
    }
    private async Task ReadErrors()
    {
        while (await process.StandardError.ReadLineAsync() is { } line)
        {
            ErrorTail = (ErrorTail + "\n" + line);
            if (ErrorTail.Length > 4096) ErrorTail = ErrorTail[^4096..];
            CaptureDecoders();
            SafeLog("[Startup] " + line);
        }
    }
    internal async Task<JsonElement> AwaitEventAsync(string expected, TimeSpan remaining, CancellationToken cancellation)
    {
        if (remaining <= TimeSpan.Zero) throw new TimeoutException("Original memory startup deadline expired.");
        using var bound = CancellationTokenSource.CreateLinkedTokenSource(cancellation);
        bound.CancelAfter(remaining);
        try
        {
            while (await events.Reader.WaitToReadAsync(bound.Token))
                while (events.Reader.TryRead(out var value))
                {
                    var kind = value.GetProperty("event").GetString();
                    if (kind == "failed" || kind == "finished")
                        throw new InvalidOperationException("Owned startup ended before " + expected + ": " + value);
                    if (kind == expected)
                    {
                        if (!IsAlive) throw new InvalidOperationException("Owned startup helper exited before readiness could be used.");
                        return value;
                    }
                }
            throw new InvalidOperationException("Owned startup EOF before " + expected + ": " + ErrorTail);
        }
        catch (OperationCanceledException) when (!cancellation.IsCancellationRequested)
        { throw new TimeoutException("Original memory startup deadline expired."); }
    }
    internal async Task StopAsync(bool sendCommand = true)
    {
        if (!OwnershipRetained) return;
        Exception? controlFailure = null;
        if (Interlocked.Exchange(ref stopRequested, 1) == 0)
        {
            try
            {
                if (sendCommand && !process.HasExited)
                {
                    await controlWrite.WaitAsync();
                    try {
                        await process.StandardInput.WriteLineAsync("{\"command\":\"stop\"}").WaitAsync(TimeSpan.FromSeconds(1));
                        await process.StandardInput.FlushAsync().WaitAsync(TimeSpan.FromSeconds(1));
                    } finally { controlWrite.Release(); }
                }
            }
            catch (Exception error) { controlFailure = error; }
            finally { process.StandardInput.Close(); } // EOF remains the genuine owned cleanup request.
        }
        try { await Exit.WaitAsync(TimeSpan.FromSeconds(MemoryStartContext.CleanupWaitSeconds)); }
        catch (TimeoutException) { throw new InvalidOperationException("Owned startup cleanup has not exited; ownership is retained. Root/Server cleanup evidence required."); }
        try { await Task.WhenAll(stdout, stderr).WaitAsync(TimeSpan.FromSeconds(1)); }
        finally { observedExitCode = process.ExitCode; OwnershipRetained = false; process.Dispose(); }
        if (controlFailure is not null) SafeLog("[Startup control/EOF] " + controlFailure.Message);
    }
    private void CaptureDecoders() => Decoders = new(process.StandardOutput.CurrentEncoding.WebName,
        process.StandardOutput.CurrentEncoding.CodePage, process.StandardError.CurrentEncoding.WebName, process.StandardError.CurrentEncoding.CodePage);
    private void SafeLog(string message)
    {
        try { log(message); }
        catch (Exception error) { LogFailure = error.ToString(); }
    }
}

internal static class MemoryStartupReady
{
    internal static readonly (string Role, string Kind, string Host, int Port)[] Endpoints =
    [ ("rental-battle", "LOGIN", "127.0.0.3", 32001), ("rental-battle", "GAME", "127.0.0.4", 32001),
      ("rental-source", "LOGIN", "127.0.0.1", 32000), ("rental-source", "GAME", "127.0.0.2", 32000) ];
    internal static JsonElement Validate(JsonElement ready, string runId, MemoryStartupSources sources)
        => Validate(ready, runId, sources.ServerRunsDirectory, sources.JvmImage);
    internal static JsonElement Validate(JsonElement ready, string runId, string serverRunsDirectory, string jvmImage)
    {
        if (ready.GetProperty("event").GetString() != "ready" || ready.TryGetProperty("fixture_only", out _)
            || ready.GetProperty("product_complete").GetBoolean()
            || !Path.GetFullPath(ready.GetProperty("run").GetString()!).Equals(Path.Combine(serverRunsDirectory, runId), StringComparison.OrdinalIgnoreCase))
            throw new InvalidDataException("Memory READY is not this new owned run.");
        var instances = ready.GetProperty("instances").EnumerateArray().ToArray();
        if (instances.Length != 2 || !instances.Select(x => x.GetProperty("role").GetString()).SequenceEqual(new[] { "rental-battle", "rental-source" }))
            throw new InvalidDataException("Actual memory battle/source roles required; SQL roles are not interchangeable.");
        var owners = new Dictionary<string, int>();
        foreach (var instance in instances)
        {
            var identity = instance.GetProperty("identity");
            if (!instance.GetProperty("ready").GetBoolean() || instance.GetProperty("ready_marker").GetString() != "[LiTo] ready"
                || identity.GetProperty("pid").GetInt32() <= 0 || identity.GetProperty("birth_filetime").GetInt64() <= 0
                || !identity.GetProperty("image").GetString()!.Equals(jvmImage, StringComparison.OrdinalIgnoreCase))
                throw new InvalidDataException("Actual memory READY identity/marker mismatch.");
            owners.Add(instance.GetProperty("role").GetString()!, identity.GetProperty("pid").GetInt32());
        }
        if (owners.Values.Distinct().Count() != 2) throw new InvalidDataException("Distinct actual memory JVM identities required.");
        var endpoints = ready.GetProperty("endpoints").EnumerateArray().ToArray();
        if (endpoints.Length != 4) throw new InvalidDataException("All four actual owned endpoints required.");
        foreach (var expected in Endpoints)
        {
            var matches = endpoints.Where(x => x.GetProperty("role").GetString() == expected.Role && x.GetProperty("kind").GetString() == expected.Kind
                && x.GetProperty("host").GetString() == expected.Host && x.GetProperty("port").GetInt32() == expected.Port).ToArray();
            if (matches.Length != 1 || !matches[0].GetProperty("connect").GetBoolean() || matches[0].GetProperty("payload_bytes_sent").GetInt32() != 0
                || matches[0].GetProperty("owner").GetProperty("pid").GetInt32() != owners[expected.Role]
                || matches[0].GetProperty("owner").GetProperty("state").GetInt32() != 2
                || matches[0].GetProperty("owner").GetProperty("local_host").GetString() != expected.Host
                || matches[0].GetProperty("owner").GetProperty("local_port").GetInt32() != expected.Port)
                throw new InvalidDataException("Memory READY endpoint owner/connection mismatch; no alternate-port fallback.");
        }
        return ready;
    }
}
