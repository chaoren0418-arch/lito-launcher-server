using System.Diagnostics;
using System.Globalization;
using System.Runtime.InteropServices;
using System.Security.Cryptography;
using System.Text;
using System.Text.Json;
using System.Text.Json.Nodes;
using Pin = LiTo381Launcher.CurrentIsolatedStartup.Pin;
using Identity = LiTo381Launcher.CurrentIsolatedStartup.Identity;

namespace LiTo381Launcher;

// Request transport only. The original entry owns admission, token, claim and native activation.
public sealed class AutomatedIsolatedStartup : IDisposable
{
    public const string Delivery = @"D:\lineage開發\3-lito開發\lito登入器\LiTo381-Launcher\runtime\launcher-automated-isolated-start-20261008";
    public const string Workflow = @"D:\lineage開發\3-lito開發\監製工作流";
    public const string DirectionPath = Workflow + @"\unattended-direction-20261008.json";
    public const string DirectionSha = "376E1AFAB8D7164AC1B1701FE7ECB4203862BD6D63A07FF22B2275BEA79CA720";
    public const string NativeSha = "CE6EE68ADA8FD37E6C096B1A27E46CB6888F1179F85DEFB5007AC60287360292";
    private static readonly JsonSerializerOptions Json = new() { PropertyNamingPolicy = JsonNamingPolicy.SnakeCaseLower, WriteIndented = true };
    private readonly List<CurrentIsolatedStartup.RetainedIdentity> retained = new();
    private readonly List<FileStream> files = new();
    private readonly string instanceId = Guid.NewGuid().ToString("N");
    private readonly DateTimeOffset clockAnchor = DateTimeOffset.UtcNow;
    private readonly long monotonicAnchor = Stopwatch.GetTimestamp();
    private readonly DateTimeOffset created;
    private readonly string attemptPath;
    private readonly Identity launcher;
    private readonly Pin assembly;
    private Configuration? config;
    private Pin? configPin, eventPin;
    private DateTimeOffset proofEnd;
    private bool disposed;
    private string stage = "attempt-created";

    public record OriginalIdentity(int Pid, string Filetime, int ParentPid, string Path,
        string ImageSha256, string CommandSha256, string CommandArgvDigest);
    public record Provider(string Role, OriginalIdentity Identity);
    public record Configuration(string Schema, string Mode, string AttemptNonce, OriginalIdentity Coordinator,
        Provider[] Providers, Pin Ready, Pin ProviderQualification, Pin Direction, DateTimeOffset QualifiedAtUtc,
        DateTimeOffset NotAfterUtc, double ProducerRetentionNotAfterEpoch, string ResultPath, Pin Review);

    private AutomatedIsolatedStartup()
    {
        PlainPath(Delivery);
        var parent = Path.Combine(Delivery, "attempts");
        Directory.CreateDirectory(parent); PlainPath(parent);
        attemptPath = Path.Combine(parent, instanceId);
        Directory.CreateDirectory(attemptPath); PlainPath(attemptPath);
        launcher = CurrentIsolatedStartup.CaptureIdentity(Environment.ProcessId);
        created = DateTimeOffset.FromFileTime(launcher.CreatedFiletime);
        assembly = FilePin(typeof(AutomatedIsolatedStartup).Assembly.Location);
        Write("attempt.json", new { schema = "LAUNCHER_AUTOMATED_START_ATTEMPT_1", instance_id = instanceId,
            launcher, launcher_assembly = assembly, observed_at_utc = created, mode = "automated-isolated-test",
            human_ready = false, human_click = false, native_started = false, product_accepted = false });
    }

    public static int Run(string[] args)
    {
        AutomatedIsolatedStartup? service = null;
        Exception? primary = null;
        var exit = 2;
        try
        {
            service = new AutomatedIsolatedStartup();
            Need(args.Length == 4 && args[0] == "--automated-isolated-test" && args[2] == "--config-sha256", "arguments: exact automated invocation required");
            service.Open(args[1], args[3]);
            service.PublishRequest();
            service.ObserveUntilClosed();
            exit = 0;
        }
        catch (Exception error)
        {
            primary = error;
            if (service is not null) service.Write("failure.json", new { schema = "LAUNCHER_AUTOMATED_START_FAILURE_1",
                instance_id = service.instanceId, service.stage, observed_at_utc = DateTimeOffset.UtcNow,
                error_type = error.GetType().Name, reason = error.Message, native_started_by_producer = false,
                native_outcome = "unknown-unless-original-entry-receipt", root_cleanup_required = service.eventPin is not null,
                human_ready = false, human_click = false, product_accepted = false });
        }
        finally
        {
            if (service is not null)
            {
                service.Dispose();
                service.Write("exit.json", new { schema = "LAUNCHER_AUTOMATED_PROCESS_EXIT_INTENT_1", exit_code = exit,
                    primary_failure = primary?.Message, actual_process_exit_observed = false,
                    actual_exit_observer = "Root retained process handle", product_accepted = false });
            }
        }
        return exit;
    }

    private void Open(string path, string sha)
    {
        stage = "configuration";
        Need(Path.IsPathFullyQualified(path) && IsHash(sha), "configuration: absolute path and uppercase SHA required");
        configPin = FilePin(path); Need(configPin.Sha256 == sha, "configuration: CLI SHA mismatch");
        var node = ReadPinned(configPin).AsObject();
        ExactKeys(node, "schema", "mode", "attempt_nonce", "coordinator", "providers", "ready", "provider_qualification", "direction", "qualified_at_utc", "not_after_utc", "producer_retention_not_after_epoch", "result_path", "review");
        config = node.Deserialize<Configuration>(Json) ?? throw new InvalidOperationException("configuration: invalid");
        ValidateOriginalNode(node["coordinator"]!.AsObject());
        foreach (var provider in node["providers"]!.AsArray())
        {
            ExactKeys(provider!.AsObject(), "role", "identity");
            ValidateOriginalNode(provider["identity"]!.AsObject());
        }
        Need(Timestamp(node, "qualified_at_utc") == config.QualifiedAtUtc && Timestamp(node, "not_after_utc") == config.NotAfterUtc,
            "configuration: explicit qualification/review timezone required");
        ValidateShape(config, DateTimeOffset.UtcNow);
        Need(IsWithin(config.Review.Path, Workflow), "review: Root owned source required");
        var review = ReadPinned(config.Review).AsObject();
        ExactKeys(review, "schema", "reviewer", "decision", "scope", "contract_sha256", "direction_pin", "producer_source_pin", "launcher_apphost_pin", "launcher_assembly_pin", "selected_native_bin_pin", "not_after_utc");
        Need(Text(review, "schema") == "ROOT_AUTOMATED_ISOLATED_LAUNCHER_REVIEW_1" && Text(review, "reviewer") == "supervisor" &&
            Text(review, "decision") == "accepted" && Text(review, "scope") == "automated-isolated-test-original-gates-only", "review: distinct automated source review required");
        node.Remove("review");
        Need(Text(review, "contract_sha256") == CurrentIsolatedStartup.CanonicalHash(node), "review: canonical config mismatch");
        Need(Timestamp(review, "not_after_utc") >= config.NotAfterUtc, "review: config extends reviewed bound");
        Need(review["direction_pin"]!.Deserialize<Pin>(Json) == config.Direction && config.Direction == new Pin(DirectionPath, DirectionSha, 3683), "direction: exact current user direction required");
        var direction = ReadPinned(config.Direction).AsObject();
        Need(Text(direction["authorization"]!.AsObject(), "schema") == "LITO_UNATTENDED_TEST_USER_DIRECTION_1", "direction: authorization schema");
        var source = review["producer_source_pin"]!.Deserialize<Pin>(Json)!;
        Need(Path.GetFullPath(source.Path) == Path.GetFullPath(Path.Combine(Delivery, "..", "..", "src", "AutomatedIsolatedStartup.cs")), "review: actual producer source path required");
        HoldBinary(source);
        var apphost = review["launcher_apphost_pin"]!.Deserialize<Pin>(Json)!;
        var dll = review["launcher_assembly_pin"]!.Deserialize<Pin>(Json)!;
        var native = review["selected_native_bin_pin"]!.Deserialize<Pin>(Json)!;
        Need(apphost.Path == launcher.Path && apphost.Sha256 == launcher.ImageSha256 && dll == assembly, "review: actual executable/assembly mismatch");
        Need(native.Path == Path.Combine(CurrentIsolatedStartup.IsolatedClient, "LiTo.bin") && native.Sha256 == NativeSha && native.Bytes == 1941568, "native: selected isolated source mismatch");
        HoldBinary(apphost); HoldBinary(dll); HoldBinary(native);
        Need(launcher.ParentPid == config.Coordinator.Pid, "coordinator: actual EXE direct parent mismatch");
        stage = "retained-current-identities";
        Retain(config.Coordinator, "coordinator");
        foreach (var provider in config.Providers) Retain(provider.Identity, "provider/" + provider.Role);
        var ready = ReadPinned(config.Ready).AsObject();
        Need(Text(ready, "schema") == "LITO_CURRENT_PROVIDER_READY_1" && Text(ready, "status") == "READY", "provider: current READY required");
        var providerEnd = ready["provider_not_after_epoch"]!.GetValue<double>();
        Need(double.IsFinite(providerEnd) && config.ProducerRetentionNotAfterEpoch <= providerEnd &&
            config.NotAfterUtc.ToUnixTimeMilliseconds() / 1000d <= providerEnd, "provider: passive retention/proof exceeds immutable READY end");
        var qualification = ReadPinned(config.ProviderQualification).AsObject();
        Need(Text(qualification, "schema") == "ROOT_AUTOMATED_NATIVE_PROVIDER_QUALIFICATION_1" && Text(qualification, "reviewer") == "supervisor" && Text(qualification, "status") == "current-identities-listeners-qualified" &&
            Text(qualification, "phase") == "pre-start", "provider: genuine current qualification required");
        Need(qualification["operator"]!.Deserialize<OriginalIdentity>(Json) == config.Coordinator &&
            qualification["READY_pin"]!.Deserialize<Pin>(Json) == config.Ready, "provider: qualification binding mismatch");
        foreach (var provider in config.Providers)
            Need(qualification["provider_processes"]![provider.Role]!.Deserialize<OriginalIdentity>(Json) == provider.Identity,
                "provider: qualified original seven-field role mismatch");
        var qualified = Timestamp(qualification, "at_utc");
        Need(qualified == config.QualifiedAtUtc, "provider: qualified timestamp mismatch");
        var now = DateTimeOffset.UtcNow;
        proofEnd = new[] { created.AddSeconds(120), config.NotAfterUtc, qualified.AddSeconds(120) }.Min();
        Need(now < proofEnd, "provider: expired proof before publication");
        Need(IsWithin(config.ResultPath, Workflow) && !File.Exists(config.ResultPath) && !File.Exists(config.ResultPath + ".terminal.json"), "result: Root owned unused observation paths required");
        PlainPath(config.ResultPath);
        Write("automated-instance.json", new { schema = "CURRENT_LAUNCHER_AUTOMATED_INSTANCE_1", instance_id = instanceId,
            launcher, launcher_assembly = assembly, created_at_utc = created, not_after_utc = proofEnd, mode = "automated-isolated-test" });
    }

    public static void ValidateShape(Configuration value, DateTimeOffset now)
    {
        Need(value.Schema == "LITO_AUTOMATED_ISOLATED_LAUNCHER_1" && value.Mode == "automated-isolated-test", "configuration: automated mode required");
        Need(value.AttemptNonce is { Length: 32 } && value.AttemptNonce.All(c => "0123456789abcdef".Contains(c)), "configuration: attempt nonce required");
        ValidateIdentity(value.Coordinator);
        Need(value.Providers is { Length: 3 } && value.Providers.Select(p => p.Role).Order().SequenceEqual(new[] { "helper", "mysql", "server" }), "provider: exact three roles required");
        foreach (var provider in value.Providers) ValidateIdentity(provider.Identity);
        Need(value.Providers.Select(p => p.Identity.Pid).Append(value.Coordinator.Pid).Distinct().Count() == 4, "provider: distinct identities required");
        Need(value.QualifiedAtUtc <= now && now - value.QualifiedAtUtc < TimeSpan.FromSeconds(120) && now < value.NotAfterUtc && value.NotAfterUtc <= value.QualifiedAtUtc.AddSeconds(120), "provider: fresh immutable 120-second qualification required");
        var proofEpoch = value.NotAfterUtc.ToUnixTimeMilliseconds() / 1000d;
        Need(double.IsFinite(value.ProducerRetentionNotAfterEpoch) && value.ProducerRetentionNotAfterEpoch >= proofEpoch &&
            value.ProducerRetentionNotAfterEpoch <= proofEpoch + 300 + 110, "configuration: passive retention cap exceeds proof120/owner300/cleanup110");
    }

    private void PublishRequest()
    {
        stage = "request-publication"; CheckIdentities();
        Need(DateTimeOffset.UtcNow < proofEnd && Stopwatch.GetTimestamp() < MonotonicEnd(proofEnd) &&
            CurrentIsolatedStartup.CaptureIdentity(Environment.ProcessId) == launcher, "request: expired or changed producer");
        var review = ReadPinned(config!.Review).AsObject();
        Write("automated-start.json", new { schema = "CURRENT_LAUNCHER_AUTOMATED_START_1", instance_id = instanceId,
            attempt_nonce = config.AttemptNonce, launcher, launcher_assembly = assembly,
            producer_source_pin = review["producer_source_pin"]!.Deserialize<Pin>(Json), config_pin = configPin,
            direction_pin = config.Direction, review_pin = config.Review, coordinator = config.Coordinator,
            provider_qualification_pin = config.ProviderQualification, ready_pin = config.Ready,
            selected_native_bin_pin = review["selected_native_bin_pin"]!.Deserialize<Pin>(Json),
            observed_at_utc = DateTimeOffset.UtcNow, not_after_utc = proofEnd, event_source = "Program.AutomatedIsolatedTest",
            authority_kind = "user-directed-supervisor-automated-isolated-test", mode = "automated-isolated-test",
            human_ready = false, human_click = false, human_action_observed = false, provider_lease_created = false,
            native_started = false, claim_created = false });
        eventPin = FilePin(Path.Combine(attemptPath, "automated-start.json"));
    }

    private void ObserveUntilClosed()
    {
        stage = "await-original-entry-observation";
        var retentionEnd = DateTimeOffset.FromUnixTimeMilliseconds(checked((long)(config!.ProducerRetentionNotAfterEpoch * 1000)));
        var received = false;
        while (DateTimeOffset.UtcNow < retentionEnd && Stopwatch.GetTimestamp() < MonotonicEnd(retentionEnd))
        {
            // Retained coordinator handle remains mandatory; provider death after import is an entry outcome.
            retained[0].Check(); Need(CaptureOriginalIdentity(config!.Coordinator.Pid) == config.Coordinator, "coordinator: exact original identity changed");
            if (!received && File.Exists(config.ResultPath))
            {
                var observation = ReadPinned(FilePin(config.ResultPath)).AsObject(); ValidateObservation(observation);
                var phase = Text(observation, "phase");
                Need(phase is "request_failed" or "actual_native_observed", "observation: unexpected first phase");
                if (phase == "actual_native_observed")
                {
                    var owner = observation["original_owner_not_after_epoch"]!.GetValue<double>();
                    var observed = Timestamp(observation, "observed_at_utc");
                    Need(double.IsFinite(owner) && owner <= observed.ToUnixTimeMilliseconds() / 1000d + 300 &&
                        observation["original_cleanup_seconds"]?.GetValue<int>() == 110 && owner + 110 <= config.ProducerRetentionNotAfterEpoch,
                        "observation: original owner300/cleanup110 outside passive retention cap");
                    retentionEnd = DateTimeOffset.FromUnixTimeMilliseconds(checked((long)((owner + 110) * 1000)));
                    var nativeIdentity = observation["native"]!.Deserialize<OriginalIdentity>(Json)!;
                    ValidateIdentity(nativeIdentity);
                    Need(nativeIdentity.Path == Path.Combine(CurrentIsolatedStartup.IsolatedClient, "LiTo.bin") &&
                        nativeIdentity.ImageSha256 == NativeSha && CaptureOriginalIdentity(nativeIdentity.Pid) == nativeIdentity,
                        "observation: actual selected native identity changed");
                    Need(observation["native_status_pin"] is not null && observation["entry_status_pin"] is not null, "observation: actual entry/native status pins required");
                }
                received = true;
                Write("entry-observation.json", observation);
                stage = "await-original-entry-and-companion-cleanup";
            }
            if (File.Exists(config.ResultPath + ".terminal.json"))
            {
                var terminal = ReadPinned(FilePin(config.ResultPath + ".terminal.json")).AsObject(); ValidateObservation(terminal);
                Need(Text(terminal, "phase") == "entry_and_companion_closed", "observation: terminal phase required");
                var entryCreated = terminal["entry_created"]!.GetValue<bool>();
                var companionCreated = terminal["companion_created"]!.GetValue<bool>();
                Need(terminal["actual_entry_exit_observed"]?.GetValue<bool>() == entryCreated &&
                    terminal["actual_companion_exit_observed"]?.GetValue<bool>() == companionCreated,
                    "observation: actual retained child exits or truthful never-created outcomes required");
                var cleanup = ReadPinned(terminal["cleanup_receipt_pin"]!.Deserialize<Pin>(Json)!).AsObject();
                Need(cleanup["complete"]?.GetValue<bool>() == true, "observation: original cleanup receipt incomplete");
                Write("terminal-observation.json", terminal); stage = "original-entry-closed"; return;
            }
            Thread.Sleep(100);
        }
        throw new InvalidOperationException(received ? "original owner deadline reached; cleanup outcome unknown" : "current 120-second request expired without original entry observation");
    }

    private double MonotonicEnd(DateTimeOffset end) => monotonicAnchor + (end - clockAnchor).TotalSeconds * Stopwatch.Frequency;

    private void ValidateObservation(JsonObject value)
    {
        Need(Text(value, "schema") == "ROOT_AUTOMATED_ENTRY_OBSERVATION_1" && Text(value, "reviewer") == "supervisor" &&
            value["observation_only"]?.GetValue<bool>() == true && value["lease_authority_created"]?.GetValue<bool>() == false &&
            value["deadline_renewed"]?.GetValue<bool>() == false && value["product_accepted"]?.GetValue<bool>() == false, "observation: request-only original evidence required");
        Need(value["direction_pin"]!.Deserialize<Pin>(Json) == config!.Direction && value["config_pin"]!.Deserialize<Pin>(Json) == configPin &&
            value["automated_start_event_pin"]!.Deserialize<Pin>(Json) == eventPin && value["operator"]!.Deserialize<OriginalIdentity>(Json) == config.Coordinator, "observation: exact attempt/config/operator mismatch");
        Need(Timestamp(value, "observed_at_utc") <= DateTimeOffset.UtcNow, "observation: future timestamp");
        if (value["entry_status_pin"] is JsonNode entry) ReadPinned(entry.Deserialize<Pin>(Json)!);
        if (value["native_status_pin"] is JsonNode native) ReadPinned(native.Deserialize<Pin>(Json)!);
        // These status pins describe Root's observation; they cannot replace the original consumer lease verifier.
    }

    private void Retain(OriginalIdentity original, string role)
    {
        var handle = new CurrentIsolatedStartup.RetainedIdentity(Project(original), role);
        retained.Add(handle); Need(CaptureOriginalIdentity(original.Pid) == original, role + ": original argv identity mismatch");
    }
    private void CheckIdentities()
    {
        foreach (var identity in retained) identity.Check();
        Need(CaptureOriginalIdentity(config!.Coordinator.Pid) == config.Coordinator, "coordinator: original tuple mismatch");
        foreach (var provider in config.Providers) Need(CaptureOriginalIdentity(provider.Identity.Pid) == provider.Identity, "provider: original tuple mismatch");
    }
    public static Identity Project(OriginalIdentity original) => new(original.Pid, long.Parse(original.Filetime, CultureInfo.InvariantCulture), original.ParentPid, original.Path, original.ImageSha256, original.CommandSha256);
    public static void ValidateIdentity(OriginalIdentity identity)
    {
        Need(identity is not null && identity.Pid > 0 && identity.ParentPid > 0 && identity.Filetime is { Length: >= 17 } &&
            long.TryParse(identity.Filetime, NumberStyles.None, CultureInfo.InvariantCulture, out var stamp) && stamp > 0 &&
            Path.IsPathFullyQualified(identity.Path) && IsHash(identity.ImageSha256) && IsHash(identity.CommandSha256) && IsHash(identity.CommandArgvDigest), "identity: original seven fields required");
    }
    private static void ValidateOriginalNode(JsonObject value) => ExactKeys(value, "pid", "filetime", "parent_pid", "path", "image_sha256", "command_sha256", "command_argv_digest");
    public static OriginalIdentity CaptureOriginalIdentity(int pid)
    {
        var projection = CurrentIsolatedStartup.CaptureIdentity(pid);
        using var process = Process.GetProcessById(pid);
        NtQueryInformationProcess(process.Handle, 60, IntPtr.Zero, 0, out var size);
        Need(size > 0 && size <= 1024 * 1024, "identity: command unavailable");
        var memory = Marshal.AllocHGlobal(size);
        try
        {
            Need(NtQueryInformationProcess(process.Handle, 60, memory, size, out _) == 0, "identity: command query failed");
            var pointer = Marshal.ReadIntPtr(memory, IntPtr.Size == 8 ? 8 : 4);
            var length = (ushort)Marshal.ReadInt16(memory);
            Need(pointer.ToInt64() >= memory.ToInt64() && pointer.ToInt64() + length <= memory.ToInt64() + size, "identity: command buffer range");
            var command = Marshal.PtrToStringUni(pointer, length / 2)!;
            Need(Convert.ToHexString(SHA256.HashData(Encoding.UTF8.GetBytes(command))) == projection.CommandSha256, "identity: command changed during capture");
            var vector = CommandLineToArgvW(command, out var count);
            Need(vector != IntPtr.Zero, "identity: argv unavailable");
            try
            {
                var argv = Enumerable.Range(0, count).Select(i => Marshal.PtrToStringUni(Marshal.ReadIntPtr(vector, i * IntPtr.Size))!).ToArray();
                var raw = JsonSerializer.SerializeToUtf8Bytes(argv, new JsonSerializerOptions { Encoder = System.Text.Encodings.Web.JavaScriptEncoder.UnsafeRelaxedJsonEscaping });
                return new(projection.Pid, projection.CreatedFiletime.ToString(CultureInfo.InvariantCulture), projection.ParentPid,
                    projection.Path, projection.ImageSha256, projection.CommandSha256, Convert.ToHexString(SHA256.HashData(raw)));
            }
            finally { LocalFree(vector); }
        }
        finally { Marshal.FreeHGlobal(memory); }
    }
    [DllImport("ntdll.dll")] private static extern int NtQueryInformationProcess(IntPtr process, int type, IntPtr buffer, int length, out int returned);
    [DllImport("shell32.dll", CharSet = CharSet.Unicode)] private static extern IntPtr CommandLineToArgvW(string command, out int count);
    [DllImport("kernel32.dll")] private static extern IntPtr LocalFree(IntPtr memory);
    public static bool IsHash(string value) => value is { Length: 64 } && value.All(c => "0123456789ABCDEF".Contains(c));
    private static void Need(bool value, string reason) { if (!value) throw new InvalidOperationException(reason); }
    private static string Text(JsonObject value, string name) => value[name]?.GetValue<string>() ?? throw new InvalidOperationException("missing " + name);
    private static DateTimeOffset Timestamp(JsonObject value, string name)
    {
        var raw = Text(value, name);
        Need(raw.EndsWith('Z') || raw.Length >= 6 && (raw[^6] == '+' || raw[^6] == '-') && raw[^3] == ':', "timestamp: explicit timezone required");
        return DateTimeOffset.Parse(raw, CultureInfo.InvariantCulture);
    }
    private static void ExactKeys(JsonObject value, params string[] names) => Need(value.Select(p => p.Key).Order().SequenceEqual(names.Order()), "configuration/review: exact fields required");
    private static bool IsWithin(string path, string root) => Path.IsPathFullyQualified(path) && Path.GetFullPath(path).StartsWith(Path.GetFullPath(root).TrimEnd('\\') + "\\", StringComparison.OrdinalIgnoreCase);
    public static void PlainPath(string path)
    {
        Need(Path.IsPathFullyQualified(path) && !path.StartsWith(@"\\"), "path: local absolute required");
        for (var current = new FileInfo(path) as FileSystemInfo; current is not null; current = Directory.GetParent(current.FullName))
            if (File.Exists(current.FullName) || Directory.Exists(current.FullName))
                Need((File.GetAttributes(current.FullName) & FileAttributes.ReparsePoint) == 0, "path: reparse ancestor refused");
    }
    public static Pin FilePin(string path)
    {
        PlainPath(path); var raw = File.ReadAllBytes(path);
        return new Pin(path, Convert.ToHexString(SHA256.HashData(raw)), raw.LongLength);
    }
    private void HoldBinary(Pin pin)
    {
        Need(pin.Bytes > 0 && IsHash(pin.Sha256), "pin: shape required"); PlainPath(pin.Path);
        var file = new FileStream(pin.Path, FileMode.Open, FileAccess.Read, FileShare.Read);
        files.Add(file); Need(file.Length == pin.Bytes && Convert.ToHexString(SHA256.HashData(file)) == pin.Sha256, "pin: file changed"); file.Position = 0;
    }
    private JsonNode ReadPinned(Pin pin)
    {
        HoldBinary(pin);
        using var memory = new MemoryStream(); files[^1].CopyTo(memory); files[^1].Position = 0;
        using var doc = JsonDocument.Parse(memory.ToArray());
        void Unique(JsonElement element)
        {
            if (element.ValueKind == JsonValueKind.Object)
            {
                var names = new HashSet<string>(StringComparer.Ordinal);
                foreach (var member in element.EnumerateObject()) { Need(names.Add(member.Name), "json: duplicate key"); Unique(member.Value); }
            }
            else if (element.ValueKind == JsonValueKind.Array) foreach (var item in element.EnumerateArray()) Unique(item);
        }
        Unique(doc.RootElement); return JsonNode.Parse(memory.ToArray())!;
    }
    private void Write(string name, object value) => CurrentIsolatedStartup.AtomicWriteNew(Path.Combine(attemptPath, name), value);
    public void Dispose()
    {
        if (disposed) return; disposed = true;
        var errors = new List<string>();
        foreach (var handle in retained) try { handle.Dispose(); } catch (Exception error) { errors.Add(error.Message); }
        foreach (var file in files) try { file.Dispose(); } catch (Exception error) { errors.Add(error.Message); }
        Write("cleanup.json", new { schema = "LAUNCHER_AUTOMATED_RETAINED_RESOURCE_CLEANUP_1", instance_id = instanceId,
            complete = errors.Count == 0, retained_process_handles = retained.Count, retained_file_handles = files.Count,
            errors, foreign_process_termination_performed = false, native_cleanup_owner = "original entry/Root",
            actual_process_exit_observed = false, product_accepted = false });
    }
}
