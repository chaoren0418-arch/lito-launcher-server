using System.Text.Json;
using System.Text.Json.Serialization;
using System.Security.Cryptography;

namespace LiTo381Launcher;

public enum LaunchMode
{
    ProtectedClient,
    RebuiltClient,
    ProtocolLab
}

public enum GameDisplayMode
{
    Windowed,
    Fullscreen,
    Auto
}

public enum SelectionFlowMode
{
    Native,
    Automatic
}

public enum NativeServiceMode
{
    StandardArena = 0,
    RentalServer = 1,
    // Keep 0.5.8 settings/CLI readable.  The executable argument retains the
    // original /ltpremiumsvr spelling, but the embedded p-marked server row
    // names the product as a rental server.
    [Obsolete("Use RentalServer; PremiumService is the native transport spelling, not a separate product.")]
    PremiumService = RentalServer
}

public sealed class LauncherSettings
{
    public string ProjectRoot { get; set; } = string.Empty;
    public string ClientDirectory { get; set; } = string.Empty;
    public string LiToRuntimeDirectory { get; set; } = string.Empty;
    public int LaunchTimeoutSeconds { get; set; }
    [JsonConverter(typeof(JsonStringEnumConverter<LaunchMode>))]
    public LaunchMode Mode { get; set; } = LaunchMode.ProtectedClient;
    [JsonConverter(typeof(JsonStringEnumConverter<GameDisplayMode>))]
    public GameDisplayMode DisplayMode { get; set; } = GameDisplayMode.Windowed;
    public int WindowScalePercent { get; set; } = 100;
    [JsonConverter(typeof(JsonStringEnumConverter<SelectionFlowMode>))]
    public SelectionFlowMode SelectionFlow { get; set; } = SelectionFlowMode.Native;
    [JsonConverter(typeof(JsonStringEnumConverter<NativeServiceMode>))]
    public NativeServiceMode NativeService { get; set; } = NativeServiceMode.StandardArena;
    public bool StartLocalServer { get; set; } = true;
    public bool NumericBreakthroughEnabled { get; set; } = true;
    public bool AllowNetwork { get; set; }
    public string ServerProfilePath { get; set; } = string.Empty;
    public bool OriginSourceSelectionInitialized { get; set; } = true;
}

// Editable preferences are separate from issued execution settings and authority.
public sealed record LauncherMenuPreferences(
    string ClientDirectory,
    [property: JsonConverter(typeof(JsonStringEnumConverter<GameDisplayMode>))] GameDisplayMode DisplayMode,
    int WindowScalePercent,
    [property: JsonConverter(typeof(JsonStringEnumConverter<NativeServiceMode>))] NativeServiceMode NativeService)
{
    public static LauncherMenuPreferences Default => new(
        @"D:\lineage開發\0-客戶端", GameDisplayMode.Windowed, 200, NativeServiceMode.StandardArena);

    public LauncherMenuPreferences Validate()
    {
        if (string.IsNullOrWhiteSpace(ClientDirectory) || !Path.IsPathFullyQualified(ClientDirectory))
            throw new ArgumentException("請選擇完整的 LiTo 客戶端目錄。");
        if (DisplayMode != GameDisplayMode.Windowed || WindowScalePercent is not (100 or 125 or 150 or 200)
            || NativeService is not (NativeServiceMode.StandardArena or NativeServiceMode.RentalServer))
            throw new ArgumentException("不支援的顯示、縮放或遊戲模式選項。");
        return this with { ClientDirectory = Path.GetFullPath(ClientDirectory) };
    }

    public void ApplyTo(LauncherSettings settings)
    {
        var value = Validate();
        settings.ClientDirectory = value.ClientDirectory;
        settings.DisplayMode = value.DisplayMode;
        settings.WindowScalePercent = value.WindowScalePercent;
        settings.NativeService = value.NativeService;
    }
}

public static class LauncherMenuPreferencesStore
{
    public static string PreferencesPath => Path.Combine(LauncherSettingsStore.SettingsDirectory, "interactive-menu-preferences.json");

    public static LauncherMenuPreferences Load()
    {
        try
        {
            return (JsonSerializer.Deserialize<LauncherMenuPreferences>(File.ReadAllText(PreferencesPath), LauncherJson.Options)
                ?? LauncherMenuPreferences.Default).Validate() with { ClientDirectory = @"D:\lineage開發\0-客戶端" };
        }
        catch (Exception ex) when (ex is IOException or UnauthorizedAccessException or JsonException or ArgumentException)
        {
            return LauncherMenuPreferences.Default;
        }
    }

    public static void Save(LauncherMenuPreferences value)
    {
        value = value.Validate();
        Directory.CreateDirectory(Path.GetDirectoryName(PreferencesPath)!);
        File.WriteAllText(PreferencesPath, JsonSerializer.Serialize(value, LauncherJson.Options));
    }
}

public static class LauncherJson
{
    public static readonly JsonSerializerOptions Options = new()
    {
        WriteIndented = true,
        PropertyNamingPolicy = JsonNamingPolicy.CamelCase,
        Converters = { new JsonStringEnumConverter() }
    };
}

public static class LauncherSettingsStore
{
    public static string SettingsDirectory
    {
        get
        {
            var isolated = Environment.GetEnvironmentVariable("LITO_LAUNCHER_SETTINGS_DIRECTORY");
            if (!string.IsNullOrWhiteSpace(isolated))
            {
                if (!Path.IsPathFullyQualified(isolated))
                    throw new ArgumentException("Isolated settings directory must be an absolute path.");
                return Path.GetFullPath(isolated);
            }
            return Path.Combine(
                Environment.GetFolderPath(Environment.SpecialFolder.LocalApplicationData),
                "LiTo381Launcher");
        }
    }

    public static string SettingsPath => Path.Combine(SettingsDirectory, "settings.json");

    public static LauncherSettings Load()
    {
        if (!File.Exists(SettingsPath))
        {
            return new LauncherSettings();
        }

        try
        {
            return JsonSerializer.Deserialize<LauncherSettings>(File.ReadAllText(SettingsPath), LauncherJson.Options)
                ?? new LauncherSettings();
        }
        catch
        {
            return new LauncherSettings();
        }
    }

    public static void Save(LauncherSettings settings)
    {
        Directory.CreateDirectory(SettingsDirectory);
        File.WriteAllText(SettingsPath, JsonSerializer.Serialize(settings, LauncherJson.Options));
    }
}

public static class WorkspaceLocator
{
    public static string FindProjectRoot(string? preferred = null)
    {
        if (IsProjectRoot(preferred))
        {
            return Path.GetFullPath(preferred!);
        }

        foreach (var start in new[] { AppContext.BaseDirectory, Environment.CurrentDirectory })
        {
            var current = new DirectoryInfo(Path.GetFullPath(start));
            for (var depth = 0; current is not null && depth < 10; depth++, current = current.Parent)
            {
                if (IsProjectRoot(current.FullName))
                {
                    return current.FullName;
                }

                foreach (var relative in new[]
                {
                    "舊版分析與驗收",
                    Path.Combine("lito登入器", "舊版分析與驗收"),
                    Path.Combine("3-lito開發", "lito登入器", "舊版分析與驗收"),
                    "LiTo381-開發"
                })
                {
                    var candidate = Path.Combine(current.FullName, relative);
                    if (IsProjectRoot(candidate)) return candidate;
                }
            }
        }

        var localDefault = @"D:\lineage開發\LiTo381-開發";
        return IsProjectRoot(localDefault) ? localDefault : string.Empty;
    }

    private static bool IsProjectRoot(string? path)
    {
        if (string.IsNullOrWhiteSpace(path))
        {
            return false;
        }
        return File.Exists(Path.Combine(path, "tools", "Start-LiTo381.ps1"));
    }
}

public static class LiToClientLocator
{
    public static string Find(string? preferred, string projectRoot)
    {
        var isolatedRuntime = string.IsNullOrWhiteSpace(projectRoot)
            ? string.Empty
            : Path.GetFullPath(Path.Combine(projectRoot, "runtime", "client-full"));
        var candidates = new List<string>();
        if (!string.IsNullOrWhiteSpace(preferred)) candidates.Add(preferred);
        foreach (var start in new[] { projectRoot, AppContext.BaseDirectory, Environment.CurrentDirectory })
        {
            if (string.IsNullOrWhiteSpace(start)) continue;
            var current = new DirectoryInfo(Path.GetFullPath(start));
            for (var depth = 0; current is not null && depth < 10; depth++, current = current.Parent)
            {
                candidates.Add(Path.Combine(current.FullName, "0-客戶端"));
            }
        }
        candidates.Add(@"D:\380\天堂(Lineage 3.81C) (1)");
        candidates.Add(@"D:\380\天堂(Lineage 3.80C)");
        var root = @"D:\380";
        if (Directory.Exists(root))
        {
            candidates.AddRange(Directory.EnumerateDirectories(root));
        }
        foreach (var candidate in candidates.Distinct(StringComparer.OrdinalIgnoreCase))
        {
            try
            {
                var directory = Path.GetFullPath(candidate);
                if (directory.Equals(isolatedRuntime, StringComparison.OrdinalIgnoreCase)) continue;
                var client = Path.Combine(directory, "LiTo.bin");
                if (!File.Exists(client)
                        || !File.Exists(Path.Combine(directory, "Text.idx"))
                        || !File.Exists(Path.Combine(directory, "Text.pak"))) continue;
                var hash = Convert.ToHexString(SHA256.HashData(File.ReadAllBytes(client)));
                if (hash.Equals(LauncherService.ExpectedProtectedSha256,
                        StringComparison.OrdinalIgnoreCase)) return directory;
            }
            catch
            {
                // Ignore inaccessible or malformed candidates and continue discovery.
            }
        }
        if (string.IsNullOrWhiteSpace(preferred)) return string.Empty;
        try
        {
            return Path.GetFullPath(preferred);
        }
        catch (ArgumentException)
        {
            return string.Empty;
        }
    }
}
