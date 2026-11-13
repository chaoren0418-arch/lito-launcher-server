using System.Net;
using System.Net.Sockets;
using System.Text;
using System.Text.Json;

namespace LiTo381Launcher;

// T140 / Issue #404: development/acceptance-only profile for a LiTo Server that is
// already running (e.g. the #284 experiment Server on 127.0.0.1:42083).  Selecting it
// never starts the bundled Server and never rewrites the port.  Loopback only; there is
// deliberately no AllowNetwork escape.  The formal standard flow does not read this type.
public sealed record ExternalServerSource(int Id, string Name, string Database);

public sealed class ExternalServerProfile
{
    public const string Kind = "external";
    public int SchemaVersion { get; set; }
    [System.Text.Json.Serialization.JsonPropertyName("kind")]
    public string ProfileKind { get; set; } = string.Empty;
    public string ProfileId { get; set; } = string.Empty;
    public string DisplayName { get; set; } = string.Empty;
    public string Host { get; set; } = string.Empty;
    public int Port { get; set; }
    public string StaticDatabase { get; set; } = string.Empty;
    public ExternalServerSource[] Sources { get; set; } = [];

    static ExternalServerProfile() => Encoding.RegisterProvider(CodePagesEncodingProvider.Instance);

    public static ExternalServerProfile Load(string path) => Parse(File.ReadAllText(path));

    public static ExternalServerProfile Parse(string json)
    {
        ExternalServerProfile? profile;
        try
        {
            profile = JsonSerializer.Deserialize<ExternalServerProfile>(json,
                new JsonSerializerOptions { PropertyNameCaseInsensitive = true });
        }
        catch (JsonException error) { throw new InvalidDataException("外部伺服器設定不是有效的 JSON：" + error.Message, error); }
        if (profile is null) throw new InvalidDataException("外部伺服器設定是空的。");
        profile.Validate();
        return profile;
    }

    /// <summary>Same rules as validate_profile in external_server_startup.py.</summary>
    public void Validate()
    {
        if (SchemaVersion != 1) throw new InvalidDataException("外部伺服器設定 schemaVersion 必須是 1。");
        if (ProfileKind != Kind) throw new InvalidDataException("外部伺服器設定 kind 必須是 \"external\"。");
        if (string.IsNullOrWhiteSpace(ProfileId) || ProfileId.Any(c => !char.IsAsciiLetterOrDigit(c) && c is not '.' and not '-' and not '_'))
            throw new InvalidDataException("profileId 只能包含英數字、點、連字號與底線。");
        if (string.IsNullOrWhiteSpace(DisplayName)) throw new InvalidDataException("displayName 不可空白。");
        DisplayName = DisplayName.Trim();
        if (!IPAddress.TryParse(Host?.Trim(), out var address) || address.AddressFamily != AddressFamily.InterNetwork
                || Host!.Trim().Count(c => c == '.') != 3)
            throw new InvalidDataException("host 必須是 IPv4 位址。");
        if (!IPAddress.IsLoopback(address))
            throw new InvalidDataException("外部伺服器只允許 loopback（127.0.0.0/8）。");
        Host = address.ToString();
        if (Port is < 1024 or > 65527) throw new InvalidDataException("port 必須介於 1024 到 65527。");
        if (!DatabaseIdentifier(StaticDatabase)) throw new InvalidDataException("staticDatabase 必須是英數字或底線。");
        if (Sources is null || Sources.Length is < 1 or > 8) throw new InvalidDataException("sources 必須列出 1 到 8 個來源伺服器。");
        var cp950 = Encoding.GetEncoding(950, EncoderFallback.ExceptionFallback, DecoderFallback.ExceptionFallback);
        foreach (var source in Sources)
        {
            if (source is null) throw new InvalidDataException("sources 不可有空項目。");
            if (source.Id is < 1 or > 8) throw new InvalidDataException("來源 id 必須介於 1 到 8。");
            if (string.IsNullOrWhiteSpace(source.Name) || source.Name.Contains(',') || source.Name.Any(c => c < 32 || c == 127))
                throw new InvalidDataException("來源名稱不可空白，也不可含逗號或控制字元。");
            int bytes;
            try { bytes = cp950.GetByteCount(source.Name); }
            catch (EncoderFallbackException) { throw new InvalidDataException("來源名稱必須能以 cp950 編碼。"); }
            if (bytes is < 1 or > 63) throw new InvalidDataException("來源名稱必須是 1 到 63 個 cp950 位元組。");
            if (!DatabaseIdentifier(source.Database)) throw new InvalidDataException("來源資料庫名稱必須是英數字或底線。");
        }
        if (Sources.Select(s => s.Id).Distinct().Count() != Sources.Length) throw new InvalidDataException("來源 id 不可重複。");
        if (Sources.Select(s => s.Database).Distinct(StringComparer.OrdinalIgnoreCase).Count() != Sources.Length
                || Sources.Any(s => s.Database.Equals(StaticDatabase, StringComparison.OrdinalIgnoreCase)))
            throw new InvalidDataException("來源資料庫不可重複，也不可與 staticDatabase 相同。");
        if (Sources.Select(s => s.Name).Distinct(StringComparer.OrdinalIgnoreCase).Count() != Sources.Length)
            throw new InvalidDataException("來源名稱不可重複。");
    }

    /// <summary>Login endpoints the native flow needs: base port plus base+id per source (formal rule, no port rewrite).</summary>
    public IReadOnlyList<(string Host, int Port)> ExpectedEndpoints() =>
        new[] { (Host, Port) }.Concat(Sources.Select(source => (Host, Port + source.Id))).ToArray();

    private static bool DatabaseIdentifier(string value) =>
        !string.IsNullOrWhiteSpace(value) && value.All(c => char.IsAsciiLetterOrDigit(c) || c == '_');
}
