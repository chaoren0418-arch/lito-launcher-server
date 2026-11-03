using System.Diagnostics;
using System.Runtime.InteropServices;
using System.Security.Cryptography;
using System.Text;
using System.Text.Json;
using System.Text.Json.Serialization;

namespace LiTo381Launcher;

public sealed record MemorySourcePin(string Path, long Bytes, string Sha256)
{
    public static MemorySourcePin Capture(string path)
    {
        path = System.IO.Path.GetFullPath(path);
        var raw = File.ReadAllBytes(path);
        return new(path, raw.LongLength, Convert.ToHexString(SHA256.HashData(raw)));
    }
    public void Verify()
    {
        if (!System.IO.Path.IsPathFullyQualified(Path) || Bytes <= 0 || Sha256.Length != 64
            || Sha256.Any(c => !"0123456789ABCDEF".Contains(c)))
            throw new InvalidDataException("Invalid reviewed startup source pin.");
        for (var node = new FileInfo(Path) as FileSystemInfo; node is not null; node = node is FileInfo f ? f.Directory : ((DirectoryInfo)node).Parent)
            if ((node.Attributes & FileAttributes.ReparsePoint) != 0) throw new InvalidDataException("Startup source reparse path refused.");
        if (Capture(Path) != this) throw new InvalidDataException("Reviewed startup source changed: " + Path);
    }
}

public sealed record MemoryProcessIdentity(int Pid, string Filetime, string Path, string ImageSha256,
    string CommandSha256, string CommandArgvDigest, int ParentPid)
{
    // Capture on the creation-owned Process handle while live; never query image again after exit.
    public static MemoryProcessIdentity Capture(Process process)
    {
        var handle = process.Handle;
        if (process.HasExited) throw new InvalidOperationException("Owned startup process already exited.");
        var basic = new BasicInformation();
        if (NtQueryBasic(handle, 0, ref basic, Marshal.SizeOf<BasicInformation>(), out _) != 0)
            throw new InvalidOperationException("Owned startup parent identity unavailable.");
        NtQueryBuffer(handle, 60, IntPtr.Zero, 0, out var size);
        if (size <= 0 || size > 65536) throw new InvalidOperationException("Owned startup command identity unavailable.");
        var buffer = Marshal.AllocHGlobal(size);
        try
        {
            if (NtQueryBuffer(handle, 60, buffer, size, out _) != 0) throw new InvalidOperationException("Owned startup command identity unavailable.");
            var command = Marshal.PtrToStringUni(Marshal.ReadIntPtr(buffer, IntPtr.Size == 8 ? 8 : 4), (ushort)Marshal.ReadInt16(buffer) / 2)!;
            var argvPointer = CommandLineToArgv(command, out var count);
            if (argvPointer == IntPtr.Zero) throw new InvalidOperationException("Owned startup argv identity unavailable.");
            string[] argv;
            try { argv = Enumerable.Range(0, count).Select(i => Marshal.PtrToStringUni(Marshal.ReadIntPtr(argvPointer, i * IntPtr.Size))!).ToArray(); }
            finally { LocalFree(argvPointer); }
            var imagePath = new StringBuilder(32768);
            var imageLength = imagePath.Capacity;
            if (process.HasExited || !QueryFullProcessImageName(handle, 0, imagePath, ref imageLength))
                throw new InvalidOperationException("Owned live startup image identity unavailable.");
            var image = MemorySourcePin.Capture(imagePath.ToString());
            return new(process.Id, process.StartTime.ToUniversalTime().ToFileTimeUtc().ToString(), image.Path, image.Sha256,
                Convert.ToHexString(SHA256.HashData(Encoding.UTF8.GetBytes(command))),
                Convert.ToHexString(SHA256.HashData(JsonSerializer.SerializeToUtf8Bytes(argv, MemoryStartupJson.Options))), (int)basic.ParentPid);
        }
        finally { Marshal.FreeHGlobal(buffer); }
    }
    [StructLayout(LayoutKind.Sequential)] private struct BasicInformation { public IntPtr Reserved1, Peb, Reserved2, Reserved3, Pid, ParentPid; }
    [DllImport("ntdll.dll", EntryPoint = "NtQueryInformationProcess")] private static extern int NtQueryBasic(IntPtr process, int type, ref BasicInformation info, int length, out int returned);
    [DllImport("ntdll.dll", EntryPoint = "NtQueryInformationProcess")] private static extern int NtQueryBuffer(IntPtr process, int type, IntPtr info, int length, out int returned);
    [DllImport("shell32.dll", EntryPoint = "CommandLineToArgvW", CharSet = CharSet.Unicode)] private static extern IntPtr CommandLineToArgv(string command, out int count);
    [DllImport("kernel32.dll")] private static extern IntPtr LocalFree(IntPtr memory);
    [DllImport("kernel32.dll", EntryPoint = "QueryFullProcessImageNameW", CharSet = CharSet.Unicode, SetLastError = true)]
    private static extern bool QueryFullProcessImageName(IntPtr process, int flags, StringBuilder path, ref int length);
}

internal static class MemoryStartupJson
{
    internal static readonly JsonSerializerOptions Options = new()
    {
        PropertyNamingPolicy = JsonNamingPolicy.SnakeCaseLower,
        Encoder = System.Text.Encodings.Web.JavaScriptEncoder.UnsafeRelaxedJsonEscaping,
        Converters = { new JsonStringEnumConverter() }
    };
    internal static MemorySourcePin WriteExclusive(string path, object value)
    {
        using (var file = new FileStream(path, FileMode.CreateNew, FileAccess.Write, FileShare.None))
        { JsonSerializer.Serialize(file, value, Options); file.Flush(true); }
        return MemorySourcePin.Capture(path);
    }
}

public sealed record MemoryStartupSources(MemorySourcePin Python, MemorySourcePin ServerEntry,
    MemorySourcePin NativeBridge, MemorySourcePin RootSourceReview, string JvmImage, string ServerRunsDirectory,
    string ReviewedLiToRuntimeDirectory, NativeServiceMode[] SupportedServices);

public enum MemoryStartupAvailabilityKind { Available, InvalidPreferences, SourcesUnavailable, SourceValidationFailed }

public sealed record MemoryStartupAvailability(MemoryStartupAvailabilityKind Kind, string Message, Exception? Error)
{
    public bool CanStart => Kind == MemoryStartupAvailabilityKind.Available;
}

public static class DefaultMemoryStartupRelease
{
    // Root must supply these exact reviewed callable-source anchors in a fresh candidate.
    // No sidecar bool, historical handoff, fixture, default profile or absent per-run config can release execution.
    private static readonly MemoryStartupSources? ReviewedSources = null;
    public const string RefusedServerEntrySha = "8158B2D6B51E62DFFE4B2ECC1C68FA9F4F2A8A4E8DC738037651EC60589C34DB";
    public static MemoryStartupAvailability CheckAvailability(LauncherMenuPreferences preferences)
    {
        try { preferences.Validate(); }
        catch (Exception error) { return new(MemoryStartupAvailabilityKind.InvalidPreferences, "請確認客戶端目錄與遊戲設定，再按「重新檢查」。", error); }
        try
        {
            RequireReviewed(preferences);
            return new(MemoryStartupAvailabilityKind.Available, "可以啟動遊戲。", null);
        }
        catch (Exception error)
        {
            return ReviewedSources is null
                ? new(MemoryStartupAvailabilityKind.SourcesUnavailable, "遊戲啟動功能尚未備妥。可調整並儲存設定，或使用記憶體測試伺服器；更新後請按「重新檢查」。", error)
                : new(MemoryStartupAvailabilityKind.SourceValidationFailed, "目前的遊戲啟動檔案或選定模式無法使用。請確認設定或更新啟動器，再按「重新檢查」。", error);
        }
    }
    public static MemoryStartupSources RequireReviewed(LauncherMenuPreferences preferences)
    {
        var sources = ReviewedSources ?? throw new InvalidOperationException(
            "隔離記憶體伺服器及原版啟動橋接尚未完成審查發布；目前無法啟動。供應者：Root／Server。" );
        return ValidateSources(sources, preferences);
    }
    internal static MemoryStartupSources ValidateSources(MemoryStartupSources sources, LauncherMenuPreferences preferences)
    {
        preferences.Validate();
        if (sources.ServerEntry.Sha256 == RefusedServerEntrySha) throw new InvalidDataException("Defective memory Server entry is not released.");
        foreach (var pin in new[] { sources.Python, sources.ServerEntry, sources.NativeBridge, sources.RootSourceReview }) pin.Verify();
        if (!Path.IsPathFullyQualified(sources.JvmImage) || !Path.IsPathFullyQualified(sources.ServerRunsDirectory)
            || !Path.IsPathFullyQualified(sources.ReviewedLiToRuntimeDirectory)) throw new InvalidDataException("Exact memory startup source paths required.");
        if (!sources.SupportedServices.Contains(preferences.NativeService)) throw new InvalidOperationException("選定的遊戲模式尚未取得記憶體伺服器啟動契約；設定未被替換。" );
        using var review = JsonDocument.Parse(File.ReadAllBytes(sources.RootSourceReview.Path));
        var r = review.RootElement;
        if (r.GetProperty("schema").GetString() != "ROOT_DEFAULT_MEMORY_STARTUP_SOURCE_RELEASE_1"
            || r.GetProperty("reviewer").GetString() != "supervisor" || r.GetProperty("decision").GetString() != "accepted"
            || r.GetProperty("scope").GetString() != "memory-server-and-native-bridge-callable-sources-original-admission-required"
            || !r.GetProperty("original_native_admission_required").GetBoolean() || r.GetProperty("runtime_authorized").GetBoolean()
            || r.GetProperty("product_accepted").GetBoolean()
            || r.GetProperty("python").Deserialize<MemorySourcePin>(MemoryStartupJson.Options) != sources.Python
            || r.GetProperty("server_entry").Deserialize<MemorySourcePin>(MemoryStartupJson.Options) != sources.ServerEntry
            || r.GetProperty("native_bridge").Deserialize<MemorySourcePin>(MemoryStartupJson.Options) != sources.NativeBridge
            || r.GetProperty("jvm_image").GetString() != sources.JvmImage
            || r.GetProperty("reviewed_lito_runtime_directory").GetString() != sources.ReviewedLiToRuntimeDirectory
            || !r.GetProperty("supported_services").Deserialize<NativeServiceMode[]>(MemoryStartupJson.Options)!.SequenceEqual(sources.SupportedServices))
            throw new InvalidDataException("Root memory startup callable-source review mismatch.");
        return sources;
    }
}

internal sealed class MemoryStartContext
{
    internal const int ReadySetupSeconds = 45, CleanupWaitSeconds = 10, SessionCeilingSeconds = 900;
    private readonly Stopwatch elapsed = Stopwatch.StartNew();
    internal string RunId { get; } = "launcher-" + Guid.NewGuid().ToString("N");
    internal string Directory { get; }
    internal LauncherMenuPreferences Preferences { get; }
    internal MemoryProcessIdentity LauncherIdentity { get; }
    internal MemorySourcePin Assembly { get; }
    internal MemorySourcePin StartupRequest { get; }
    internal DateTimeOffset CreatedAtUtc { get; } = DateTimeOffset.UtcNow;
    internal TimeSpan ReadyRemaining => TimeSpan.FromSeconds(ReadySetupSeconds) - elapsed.Elapsed;
    internal TimeSpan SessionRemaining => TimeSpan.FromSeconds(SessionCeilingSeconds) - elapsed.Elapsed;
    internal MemoryStartContext(LauncherMenuPreferences preferences, string? contextRoot = null)
    {
        Preferences = preferences.Validate();
        using var process = Process.GetCurrentProcess();
        LauncherIdentity = MemoryProcessIdentity.Capture(process);
        Assembly = MemorySourcePin.Capture(typeof(MemoryStartContext).Assembly.Location);
        Directory = Path.Combine(contextRoot ?? Path.Combine(LauncherSettingsStore.SettingsDirectory, "default-memory-startup"), RunId);
        System.IO.Directory.CreateDirectory(Directory);
        StartupRequest = MemoryStartupJson.WriteExclusive(Path.Combine(Directory, "startup-request.json"), new
        {
            schema = "LITO_DEFAULT_MEMORY_START_REQUEST_1", run_id = RunId, launcher_identity = LauncherIdentity,
            launcher_assembly = Assembly, selected_preferences = Preferences, created_at_utc = CreatedAtUtc,
            ready_setup_seconds = ReadySetupSeconds, session_ceiling_seconds = SessionCeilingSeconds,
            original_native_admission_required = true, runtime_authorized = false, product_accepted = false
        });
    }
}
