using System.Diagnostics;
using System.Runtime.InteropServices;
using System.Security.Cryptography;
using System.Text;
using System.Text.Json;
using System.Text.Json.Nodes;

namespace LiTo381Launcher;

// Preparation transport only. Neither a Start event nor a prepared worker grants a native claim.
public sealed class CurrentIsolatedStartup : IDisposable
{
    public const string IsolatedClient = @"D:\lineage開發\3-lito開發\lito登入器\舊版分析與驗收\runtime\client-isolated";
    public const string Delivery = @"D:\lineage開發\3-lito開發\lito登入器\LiTo381-Launcher\runtime\launcher-exe-startup-recovery-20261008";
    private static readonly JsonSerializerOptions Json = new() { PropertyNamingPolicy = JsonNamingPolicy.SnakeCaseLower, WriteIndented = true };
    private readonly Configuration config;
    private readonly List<RetainedIdentity> retained = new();
    private readonly Identity gui;
    private readonly Pin guiAssembly;
    private readonly DateTimeOffset instanceEnd;
    private readonly string nonce = Guid.NewGuid().ToString("N");
    private State? terminal;
    private bool started;
    private bool disposed;
    public string InstancePath { get; }

    public record Pin(string Path, string Sha256, long Bytes);
    public record Identity(int Pid, long CreatedFiletime, int ParentPid, string Path, string ImageSha256, string CommandSha256);
    public record Provider(string Role, Identity Identity);
    public record Configuration(string Schema, string Mode, Identity? Coordinator, Provider[] Providers,
        Pin? Ready, DateTimeOffset QualifiedAtUtc, DateTimeOffset NotAfterUtc, string FailurePath, Pin? Review);
    public record State(bool CanStart, bool Failed, string Message);

    private CurrentIsolatedStartup(Configuration value)
    {
        config = value;
        gui = CaptureIdentity(Environment.ProcessId);
        var assemblyPath = typeof(CurrentIsolatedStartup).Assembly.Location;
        var assemblyRaw = File.ReadAllBytes(assemblyPath);
        guiAssembly = new Pin(assemblyPath, Convert.ToHexString(SHA256.HashData(assemblyRaw)), assemblyRaw.LongLength);
        instanceEnd = DateTimeOffset.UtcNow.AddSeconds(120) < config.NotAfterUtc
            ? DateTimeOffset.UtcNow.AddSeconds(120) : config.NotAfterUtc;
        InstancePath = Path.Combine(Delivery, "instances", nonce);
        PlainDirectory(Delivery);
        Directory.CreateDirectory(Path.GetDirectoryName(InstancePath)!);
        PlainDirectory(Path.GetDirectoryName(InstancePath)!);
        Directory.CreateDirectory(InstancePath);
        WriteNew("gui-instance.json", new { schema = "CURRENT_GUI_INSTANCE_1", instance_id = nonce, gui, gui_assembly = guiAssembly,
            created_at_utc = DateTimeOffset.UtcNow, not_after_utc = instanceEnd, mode = "preparation-only",
            native_started = false, provider_lease_created = false });
        try
        {
            retained.Add(new RetainedIdentity(value.Coordinator!, "coordinator"));
            foreach (var provider in value.Providers) retained.Add(new RetainedIdentity(provider.Identity, "provider/" + provider.Role));
        }
        catch { foreach (var identity in retained) identity.Dispose(); throw; }
    }

    public static CurrentIsolatedStartup Open(string path)
    {
        if (!Path.IsPathFullyQualified(path) || !File.Exists(path)) throw new InvalidOperationException("configuration: missing absolute preparation config");
        var node = JsonNode.Parse(File.ReadAllText(path))!.AsObject();
        var value = node.Deserialize<Configuration>(Json) ?? throw new InvalidOperationException("configuration: invalid");
        if (value.Schema != "LITO_CURRENT_GUI_PREPARATION_1" || value.Mode != "preparation-only") throw new InvalidOperationException("configuration: preparation-only mode required");
        // Existing finite coordinator failures take priority over readiness and missing-input messages.
        ThrowCoordinatorFailure(value.FailurePath);
        if (value.Coordinator is null) throw new InvalidOperationException("coordinator: missing retained worker identity (supplier: Root)");
        if (value.Ready is null || value.Providers is null || value.Providers.Length != 3 ||
            !value.Providers.Select(x => x.Role).Order().SequenceEqual(new[] { "helper", "mysql", "server" }))
            throw new InvalidOperationException("provider: missing fresh exact three-role READY qualification (supplier: Server/Root)");
        var failure = QualificationFailure(value, DateTimeOffset.UtcNow);
        if (failure is not null) throw new InvalidOperationException(failure);
        if (value.Review is null) throw new InvalidOperationException("review: missing Root GUI preparation protocol review");
        VerifyPin(value.Review, "review");
        using var review = JsonDocument.Parse(File.ReadAllText(value.Review.Path));
        var r = review.RootElement;
        node.Remove("review");
        var payloadSha = CanonicalHash(node);
        if (r.GetProperty("schema").GetString() != "ROOT_GUI_PREPARATION_REVIEW_1" ||
            r.GetProperty("reviewer").GetString() != "supervisor" || r.GetProperty("decision").GetString() != "accepted" ||
            r.GetProperty("scope").GetString() != "preparation-only-no-native-authority" ||
            r.GetProperty("contract_sha256").GetString() != payloadSha ||
            r.GetProperty("not_after_utc").GetDateTimeOffset() < value.NotAfterUtc)
            throw new InvalidOperationException("review: preparation contract mismatch");
        VerifyPin(value.Ready, "provider/READY");
        return new CurrentIsolatedStartup(value);
    }

    public State Poll()
    {
        if (terminal is not null) return terminal;
        try
        {
            ThrowCoordinatorFailure(config.FailurePath);
            if (DateTimeOffset.UtcNow >= instanceEnd) throw new InvalidOperationException("GUI: current 120-second instance expired");
            var failure = QualificationFailure(config, DateTimeOffset.UtcNow);
            if (failure is not null) throw new InvalidOperationException(failure);
            foreach (var identity in retained) identity.Check();
            VerifyPin(config.Ready!, "provider/READY");
            return new State(!started, false, started
                ? "Start 已記錄；原生啟動仍待 Root 審查 GUI entry 契約。"
                : "隔離準備就緒；請親自按開始（有效期 120 秒內）。");
        }
        catch (Exception ex) { return terminal = new State(false, true, ex.Message); }
    }

    public void LatchFailure(string reason) => terminal ??= new State(false, true, reason);

    // Called only by MainForm's actual button-click branch. No CLI or automatic Shown invocation.
    public void HumanStart()
    {
        var state = Poll();
        if (!state.CanStart) throw new InvalidOperationException(state.Message);
        if (CaptureIdentity(Environment.ProcessId) != gui) throw new InvalidOperationException("GUI: identity changed");
        VerifyPin(guiAssembly, "GUI assembly");
        WriteNew("human-start.json", new { schema = "CURRENT_GUI_HUMAN_START_1", instance_id = nonce, gui, gui_assembly = guiAssembly,
            coordinator = config.Coordinator, observed_at_utc = DateTimeOffset.UtcNow,
            not_after_utc = instanceEnd, event_source = "MainForm.Start.Click", mode = "preparation-only",
            provider_lease_created = false, native_started = false, claim_created = false });
        started = true;
    }

    public static string? QualificationFailure(Configuration value, DateTimeOffset now)
    {
        if (now >= value.NotAfterUtc || value.QualifiedAtUtc > now || now - value.QualifiedAtUtc > TimeSpan.FromSeconds(120))
            return "provider: stale or expired qualification; historical READY is not live";
        return null;
    }

    public static void ThrowCoordinatorFailure(string path)
    {
        if (string.IsNullOrEmpty(path) || !Path.IsPathFullyQualified(path)) throw new InvalidOperationException("coordinator: missing absolute failure transport path");
        if (!File.Exists(path)) return;
        try
        {
            using var doc = JsonDocument.Parse(File.ReadAllText(path));
            var r = doc.RootElement;
            if (r.GetProperty("schema").GetString() != "ROOT_FINITE_COORDINATOR_FAILURE_1" ||
                r.GetProperty("product_accepted").GetBoolean()) throw new InvalidOperationException("coordinator: invalid terminal failure record");
            throw new InvalidOperationException("coordinator: " + r.GetProperty("reason").GetString());
        }
        catch (JsonException) { throw new InvalidOperationException("coordinator: unreadable terminal failure record"); }
    }

    public sealed class RetainedIdentity : IDisposable
    {
        private readonly Process process;
        private readonly Identity identity;
        private readonly string component;
        public RetainedIdentity(Identity expected, string role)
        {
            identity = expected; component = role;
            try { process = Process.GetProcessById(expected.Pid); _ = process.Handle; }
            catch (Exception ex) { throw new InvalidOperationException(role + ": process missing/exited", ex); }
            try { Check(); } catch { process.Dispose(); throw; }
        }
        public void Check()
        {
            try
            {
                if (process.HasExited) throw new InvalidOperationException(component + ": process-exited");
                if (CaptureIdentity(process.Id) != identity) throw new InvalidOperationException(component + ": exact identity mismatch");
            }
            catch (InvalidOperationException) { throw; }
            catch (Exception ex) { throw new InvalidOperationException(component + ": identity unavailable", ex); }
        }
        public void Dispose() => process.Dispose(); // Root owns worker/provider termination and child cleanup.
    }

    public static Identity CaptureIdentity(int pid)
    {
        using var process = Process.GetProcessById(pid);
        if (process.HasExited) throw new InvalidOperationException("process-exited");
        var path = process.MainModule!.FileName;
        var info = new BasicInformation();
        if (NtQueryInformationProcess(process.Handle, 0, ref info, Marshal.SizeOf<BasicInformation>(), out _) != 0)
            throw new InvalidOperationException("parent identity unavailable");
        NtQueryInformationProcessBuffer(process.Handle, 60, IntPtr.Zero, 0, out var size);
        if (size <= 0 || size > 1024 * 1024) throw new InvalidOperationException("command identity unavailable");
        var buffer = Marshal.AllocHGlobal(size);
        try
        {
            if (NtQueryInformationProcessBuffer(process.Handle, 60, buffer, size, out _) != 0) throw new InvalidOperationException("command identity unavailable");
            var command = Marshal.PtrToStringUni(Marshal.ReadIntPtr(buffer, IntPtr.Size == 8 ? 8 : 4), (ushort)Marshal.ReadInt16(buffer) / 2)!;
            return new Identity(pid, process.StartTime.ToUniversalTime().ToFileTimeUtc(), (int)info.ParentPid,
                path, Convert.ToHexString(SHA256.HashData(File.ReadAllBytes(path))), Convert.ToHexString(SHA256.HashData(Encoding.UTF8.GetBytes(command))));
        }
        finally { Marshal.FreeHGlobal(buffer); }
    }

    [StructLayout(LayoutKind.Sequential)] private struct BasicInformation
    { public IntPtr Reserved1, Peb, Reserved2, Reserved3, Pid, ParentPid; }
    [DllImport("ntdll.dll")] private static extern int NtQueryInformationProcess(IntPtr process, int type, ref BasicInformation info, int length, out int returned);
    [DllImport("ntdll.dll", EntryPoint = "NtQueryInformationProcess")] private static extern int NtQueryInformationProcessBuffer(IntPtr process, int type, IntPtr info, int length, out int returned);

    private static void VerifyPin(Pin pin, string component)
    {
        if (!Path.IsPathFullyQualified(pin.Path) || !File.Exists(pin.Path)) throw new InvalidOperationException(component + ": pinned file missing");
        var raw = File.ReadAllBytes(pin.Path);
        if (raw.LongLength != pin.Bytes || Convert.ToHexString(SHA256.HashData(raw)) != pin.Sha256)
            throw new InvalidOperationException(component + ": pinned file changed");
    }
    public static string CanonicalHash(JsonObject node)
    {
        using var stream = new MemoryStream();
        using (var writer = new Utf8JsonWriter(stream, new JsonWriterOptions { Encoder = System.Text.Encodings.Web.JavaScriptEncoder.UnsafeRelaxedJsonEscaping }))
        {
            void Write(JsonElement element)
            {
                if (element.ValueKind == JsonValueKind.Object) { writer.WriteStartObject(); foreach (var p in element.EnumerateObject().OrderBy(p => p.Name, StringComparer.Ordinal)) { writer.WritePropertyName(p.Name); Write(p.Value); } writer.WriteEndObject(); }
                else if (element.ValueKind == JsonValueKind.Array) { writer.WriteStartArray(); foreach (var item in element.EnumerateArray()) Write(item); writer.WriteEndArray(); }
                else element.WriteTo(writer);
            }
            Write(JsonSerializer.SerializeToElement(node));
        }
        return Convert.ToHexString(SHA256.HashData(stream.ToArray()));
    }
    private static void PlainDirectory(string path)
    {
        for (var d = new DirectoryInfo(path); d is not null; d = d.Parent)
            if (d.Exists && (d.Attributes & FileAttributes.ReparsePoint) != 0) throw new InvalidOperationException("GUI output: reparse ancestor refused");
    }
    private void WriteNew(string name, object value)
    {
        PlainDirectory(InstancePath);
        AtomicWriteNew(Path.Combine(InstancePath, name), value);
    }
    public static void AtomicWriteNew(string finalPath, object value)
    {
        var temporary = finalPath + "." + Guid.NewGuid().ToString("N") + ".tmp";
        try
        {
            using (var file = new FileStream(temporary, FileMode.CreateNew, FileAccess.Write, FileShare.None))
            { JsonSerializer.Serialize(file, value, Json); file.Flush(true); }
            File.Move(temporary, finalPath, overwrite: false);
        }
        finally { if (File.Exists(temporary)) File.Delete(temporary); }
    }
    public void Dispose()
    {
        if (disposed) return;
        disposed = true;
        try { WriteNew("gui-cancel.json", new { schema = "CURRENT_GUI_CANCEL_1", instance_id = nonce, gui,
            observed_at_utc = DateTimeOffset.UtcNow, reason = "GUI closed", native_started = false,
            root_cleanup_required = true }); }
        finally { foreach (var identity in retained) identity.Dispose(); }
    }
}
