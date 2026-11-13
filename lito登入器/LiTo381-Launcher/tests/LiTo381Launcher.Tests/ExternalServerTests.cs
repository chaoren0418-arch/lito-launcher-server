using System.Text.Json;
using Xunit;

namespace LiTo381Launcher.Tests;

// T140 / Issue #404: external-server acceptance entry. Pure validation only.
public sealed class ExternalServerProfileTests
{
    private const string Valid = """
        {
          "schemaVersion": 1, "kind": "external", "profileId": "t014-p24-external",
          "displayName": "T014 P24", "host": "127.0.0.1", "port": 42083,
          "staticDatabase": "p24_380db_lito_exp",
          "sources": [ { "id": 1, "name": "P24LiToExp", "database": "p24_accounts_lito_exp" } ]
        }
        """;

    private static string With(string property, string json)
    {
        var node = System.Text.Json.Nodes.JsonNode.Parse(Valid)!.AsObject();
        node[property] = System.Text.Json.Nodes.JsonNode.Parse(json);
        return node.ToJsonString();
    }

    [Fact]
    public void ValidProfileKeepsRequestedPort()
    {
        var profile = ExternalServerProfile.Parse(Valid);
        Assert.Equal("127.0.0.1", profile.Host);
        Assert.Equal(42083, profile.Port);
        Assert.Equal(new[] { ("127.0.0.1", 42083), ("127.0.0.1", 42084) }, profile.ExpectedEndpoints());
    }

    [Fact]
    public void CheckedInExperimentProfileIsValid()
    {
        var path = Path.Combine(AppContext.BaseDirectory, "external-profiles", "p24-experiment-42083.json");
        var profile = ExternalServerProfile.Load(path);
        Assert.Equal(42083, profile.Port);
        Assert.Equal("127.0.0.1", profile.Host);
        Assert.Contains(profile.Sources, source => source.Id == 1 && source.Database == "p24_accounts_lito_exp");
    }

    [Fact]
    public void MultipleSourcesUseBasePlusId()
    {
        var profile = ExternalServerProfile.Parse(With("sources",
            """[{"id":1,"name":"A","database":"db_a"},{"id":3,"name":"B","database":"db_b"}]"""));
        Assert.Equal(new[] { ("127.0.0.1", 42083), ("127.0.0.1", 42084), ("127.0.0.1", 42086) }, profile.ExpectedEndpoints());
    }

    [Theory]
    [InlineData("host", "\"192.168.1.10\"")]
    [InlineData("host", "\"0.0.0.0\"")]
    [InlineData("host", "\"localhost\"")]
    [InlineData("host", "\"::1\"")]
    [InlineData("host", "\"127.1\"")]
    [InlineData("port", "80")]
    [InlineData("port", "65530")]
    [InlineData("kind", "\"standard\"")]
    [InlineData("schemaVersion", "4")]
    [InlineData("profileId", "\"bad id\"")]
    [InlineData("displayName", "\" \"")]
    [InlineData("staticDatabase", "\"p24-static\"")]
    [InlineData("sources", "[]")]
    [InlineData("sources", """[{"id":9,"name":"A","database":"db_a"}]""")]
    [InlineData("sources", """[{"id":1,"name":"A,B","database":"db_a"}]""")]
    [InlineData("sources", """[{"id":1,"name":"A","database":"db_a"},{"id":1,"name":"B","database":"db_b"}]""")]
    [InlineData("sources", """[{"id":1,"name":"A","database":"db_a"},{"id":2,"name":"B","database":"db_a"}]""")]
    [InlineData("sources", """[{"id":1,"name":"A","database":"p24_380db_lito_exp"}]""")]
    [InlineData("sources", """[{"id":1,"name":"A","database":"db_a"},{"id":2,"name":"a","database":"db_b"}]""")]
    public void InvalidProfilesAreRejected(string property, string json) =>
        Assert.Throws<InvalidDataException>(() => ExternalServerProfile.Parse(With(property, json)));

    [Fact]
    public void FormalSharedProfileIsNotAnExternalProfile()
    {
        var formal = """
            {"schemaVersion":4,"profileId":"lito-shared","displayName":"x","host":"127.0.0.1","port":32000,
             "originSourceId":"lito-shared","originSourceName":"x","classicSourceNames":["a"],"staticDatabase":"s",
             "dynamicDatabases":["d"],"localServerConfig":"server.properties"}
            """;
        Assert.Throws<InvalidDataException>(() => ExternalServerProfile.Parse(formal));
    }
}

public sealed class ExternalServerStartupTests
{
    private static readonly ExternalServerProfile Profile = ExternalServerProfile.Parse("""
        {"schemaVersion":1,"kind":"external","profileId":"p","displayName":"P","host":"127.0.0.1","port":42083,
         "staticDatabase":"s","sources":[{"id":1,"name":"A","database":"d"}]}
        """);

    private static JsonElement Ready(Action<Dictionary<string, object?>>? change = null)
    {
        var value = new Dictionary<string, object?>
        {
            ["event"] = "ready", ["external_server"] = true, ["server_owned"] = false, ["selected_source_id"] = 1,
            ["service_mode"] = "standard", ["host"] = "127.0.0.1", ["port"] = 42083,
            ["endpoints"] = new object[]
            {
                new Dictionary<string, object?> { ["host"] = "127.0.0.1", ["port"] = 42083, ["connect"] = true, ["payload_bytes_sent"] = 0 },
                new Dictionary<string, object?> { ["host"] = "127.0.0.1", ["port"] = 42084, ["connect"] = true, ["payload_bytes_sent"] = 0 }
            }
        };
        change?.Invoke(value);
        return JsonSerializer.SerializeToElement(value);
    }

    [Fact]
    public void ReadyWithAllEndpointsPasses() => ExternalServerStartup.ValidateReady(Ready(), Profile, 1);

    [Fact]
    public void ReadyWithFormalPortFails() =>
        Assert.Throws<InvalidDataException>(() => ExternalServerStartup.ValidateReady(Ready(v => v["port"] = 32000), Profile, 1));

    [Fact]
    public void ReadyOwningServerFails() =>
        Assert.Throws<InvalidDataException>(() => ExternalServerStartup.ValidateReady(Ready(v => v["server_owned"] = true), Profile, 1));

    [Fact]
    public void ReadyWithoutExternalMarkerFails() =>
        Assert.Throws<InvalidDataException>(() => ExternalServerStartup.ValidateReady(Ready(v => v.Remove("external_server")), Profile, 1));

    [Fact]
    public void ReadyWithUnreachableSourceEndpointFails() =>
        Assert.Throws<InvalidDataException>(() => ExternalServerStartup.ValidateReady(Ready(v => v["endpoints"] = new object[]
        {
            new Dictionary<string, object?> { ["host"] = "127.0.0.1", ["port"] = 42083, ["connect"] = true, ["payload_bytes_sent"] = 0 },
            new Dictionary<string, object?> { ["host"] = "127.0.0.1", ["port"] = 42084, ["connect"] = false, ["payload_bytes_sent"] = 0 }
        }), Profile, 1));

    [Fact]
    public void ReadyWithMissingEndpointFails() =>
        Assert.Throws<InvalidDataException>(() => ExternalServerStartup.ValidateReady(Ready(v => v["endpoints"] = new object[]
        {
            new Dictionary<string, object?> { ["host"] = "127.0.0.1", ["port"] = 42083, ["connect"] = true, ["payload_bytes_sent"] = 0 }
        }), Profile, 1));

    [Fact]
    public void ReadyForOtherSourceFails() =>
        Assert.Throws<InvalidDataException>(() => ExternalServerStartup.ValidateReady(Ready(), Profile, 2));

    [Fact]
    public void HelperArgumentsPassProfileAndPortUntouched()
    {
        var preferences = new LauncherMenuPreferences(@"D:\lineage開發\0-客戶端", GameDisplayMode.Windowed, 200, NativeServiceMode.StandardArena);
        var arguments = ExternalServerStartup.HelperArguments(preferences, 1, @"D:\x\p24-experiment-42083.json");
        Assert.Equal(new[]
        {
            "--client-directory", @"D:\lineage開發\0-客戶端", "--scale", "200", "--service", "StandardArena",
            "--source-id", "1", "--external-profile", @"D:\x\p24-experiment-42083.json"
        }, arguments);
    }
}

public sealed class ExternalServerOptionTests
{
    [Fact]
    public void AbsentOptionKeepsFormalFlow()
    {
        Assert.Null(Program.ResolveExternalServerProfile([]));
        Assert.True(Program.ResolveDefaultMemoryMode([]));
    }

    [Fact]
    public void OptionReturnsFullPath() =>
        Assert.Equal(Path.GetFullPath("p.json"), Program.ResolveExternalServerProfile(["--external-server-profile", "p.json"]));

    [Theory]
    [InlineData("--external-server-profile")]
    [InlineData("--external-server-profile", "--legacy-formal-mode")]
    [InlineData("--external-server-profile", "p.json", "--legacy-formal-mode")]
    [InlineData("--external-server-profile", "p.json", "--current-isolated-test", "c.json")]
    [InlineData("--external-server-profile", "p.json", "--start-memory-game")]
    [InlineData("--external-server-profile", "p.json", "--start-memory-server")]
    [InlineData("--external-server-profile", "p.json", "--start-development-game")]
    [InlineData("--external-server-profile", "p.json", "--smoke-test")]
    [InlineData("--external-server-profile", "a.json", "--external-server-profile", "b.json")]
    public void IncompatibleOptionsAreRefused(params string[] args) =>
        Assert.Throws<ArgumentException>(() => Program.ResolveExternalServerProfile(args));

    [Fact]
    public void FormalStandardEndpointsUnchanged() =>
        Assert.Equal(new[] { ("standard", "LOGIN", "127.0.0.1", 32000), ("standard", "GAME", "127.0.0.2", 32000) },
            FormalStandardReady.Endpoints);
}
