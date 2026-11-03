using System.Net;
using System.Net.Sockets;
using System.Text.Json;

namespace LiTo381Launcher;

public sealed class ServerProfileDefinition
{
    public const string DedicatedProfileFileName = "lito-shared.json";
    public int SchemaVersion { get; set; }
    public string ProfileId { get; set; } = string.Empty;
    public string DisplayName { get; set; } = string.Empty;
    public string Host { get; set; } = string.Empty;
    public int Port { get; set; }
    public string OriginSourceId { get; set; } = string.Empty;
    public string OriginSourceName { get; set; } = string.Empty;
    public string LocalServerConfig { get; set; } = string.Empty;
    public string RentalSourceServerConfig { get; set; } = string.Empty;
    public string RentalBattleServerConfig { get; set; } = string.Empty;
    public string[] ClassicSourceNames { get; set; } = [];
    public string StaticDatabase { get; set; } = string.Empty;
    public string[] DynamicDatabases { get; set; } = [];

    public bool IsLoopback => IPAddress.TryParse(Host, out var address) && IPAddress.IsLoopback(address);

    public static string FindDedicatedPath()
    {
        var candidates = new[]
        {
            Path.Combine(AppContext.BaseDirectory, "server-profiles", DedicatedProfileFileName),
            Path.Combine(Environment.CurrentDirectory, "server-profiles", DedicatedProfileFileName)
        };
        return candidates.FirstOrDefault(File.Exists) ?? candidates[0];
    }

    public static ServerProfileDefinition Load(string path)
    {
        var profile = JsonSerializer.Deserialize<ServerProfileDefinition>(
            File.ReadAllText(path),
            new JsonSerializerOptions { PropertyNameCaseInsensitive = true })
            ?? throw new InvalidDataException("Server profile is empty.");
        profile.Validate();
        return profile;
    }

    public void Validate()
    {
        if (SchemaVersion is not 1 and not 2 and not 3 and not 4)
        {
            throw new InvalidDataException("schemaVersion must be 1, 2, 3, or 4.");
        }
        if (string.IsNullOrWhiteSpace(ProfileId) || ProfileId.Any(character =>
                !char.IsAsciiLetterOrDigit(character) && character is not '.' and not '-' and not '_'))
        {
            throw new InvalidDataException("profileId 只能包含英數字、點、連字號與底線。");
        }
        if (string.IsNullOrWhiteSpace(DisplayName))
        {
            throw new InvalidDataException("displayName 不可空白。");
        }
        if (!IPAddress.TryParse(Host, out var address) || address.AddressFamily != AddressFamily.InterNetwork)
        {
            throw new InvalidDataException("host 必須是 IPv4 位址。");
        }
        Host = address.ToString();
        if (Port is < 1 or > 65535)
        {
            throw new InvalidDataException("port 必須介於 1 到 65535。");
        }

        if (SchemaVersion == 1)
        {
            OriginSourceId = ProfileId;
            OriginSourceName = DisplayName;
            if (IsLoopback) LocalServerConfig = "server.properties";
        }
        if (string.IsNullOrWhiteSpace(OriginSourceId) || OriginSourceId.Any(character =>
                !char.IsAsciiLetterOrDigit(character) && character is not '.' and not '-' and not '_'))
        {
            throw new InvalidDataException("originSourceId may contain only ASCII letters, digits, dot, dash, and underscore.");
        }
        if (string.IsNullOrWhiteSpace(OriginSourceName))
        {
            throw new InvalidDataException("originSourceName must not be empty.");
        }
        ValidateConfigFileName(LocalServerConfig, "localServerConfig");
        ValidateConfigFileName(RentalSourceServerConfig, "rentalSourceServerConfig");
        ValidateConfigFileName(RentalBattleServerConfig, "rentalBattleServerConfig");
        if (SchemaVersion >= 2 && IsLoopback && string.IsNullOrWhiteSpace(LocalServerConfig))
        {
            throw new InvalidDataException("loopback source profiles require localServerConfig.");
        }
        // A schema-4 SQL/source profile can serve StandardArena without owning
        // a rental pair.  Rental mode itself still requires both configs in
        // CreateLaunchPlan; reject partial pairs for every supported mode.
        if (SchemaVersion >= 3 && IsLoopback
                && (string.IsNullOrWhiteSpace(RentalSourceServerConfig)
                    != string.IsNullOrWhiteSpace(RentalBattleServerConfig)))
        {
            throw new InvalidDataException(
                "loopback rental server configs must be supplied together; StandardArena may omit both.");
        }
        if (SchemaVersion >= 4)
        {
            if (ClassicSourceNames.Length == 0
                    || ClassicSourceNames.Any(name => string.IsNullOrWhiteSpace(name)
                        || name.Length > 63 || name.Any(char.IsControl))
                    || ClassicSourceNames.Distinct(StringComparer.OrdinalIgnoreCase).Count()
                        != ClassicSourceNames.Length)
            {
                throw new InvalidDataException(
                    "schemaVersion 4 classicSourceNames 必須是唯一且不超過 63 字元的來源名稱。");
            }
            if (!DatabaseIdentifier(StaticDatabase)
                    || DynamicDatabases.Length != ClassicSourceNames.Length
                    || DynamicDatabases.Any(database => !DatabaseIdentifier(database))
                    || DynamicDatabases.Distinct(StringComparer.OrdinalIgnoreCase).Count()
                        != DynamicDatabases.Length
                    || DynamicDatabases.Contains(StaticDatabase, StringComparer.OrdinalIgnoreCase))
            {
                throw new InvalidDataException(
                    "schemaVersion 4 必須提供一個 staticDatabase，以及與來源一對一的唯一 dynamicDatabases。");
            }
        }
    }

    private static void ValidateConfigFileName(string value, string propertyName)
    {
        if (!string.IsNullOrWhiteSpace(value)
                && (!value.EndsWith(".properties", StringComparison.OrdinalIgnoreCase)
                    || !Path.GetFileName(value).Equals(value, StringComparison.OrdinalIgnoreCase)))
        {
            throw new InvalidDataException(
                $"{propertyName} must be a .properties file name without a path.");
        }
    }

    private static bool DatabaseIdentifier(string value) =>
        !string.IsNullOrWhiteSpace(value)
        && value.All(character => char.IsAsciiLetterOrDigit(character) || character == '_');
}
