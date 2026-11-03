using System.Text.Json;
namespace LiTo381Launcher;
internal static class FormalStandardReady
{
    internal static readonly (string Role, string Kind, string Host, int Port)[] Endpoints =
    [ ("standard", "LOGIN", "127.0.0.1", 32000), ("standard", "GAME", "127.0.0.2", 32000) ];
    internal static JsonElement Validate(JsonElement ready, string runId, MemoryStartupSources sources)
        => Validate(ready, runId, sources.ServerRunsDirectory, sources.JvmImage);
    internal static JsonElement Validate(JsonElement ready, string runId, string serverRunsDirectory, string jvmImage)
    {
        if (ready.GetProperty("event").GetString() != "ready" || ready.TryGetProperty("fixture_only", out _)
            || ready.GetProperty("product_complete").GetBoolean()
            || !Path.GetFullPath(ready.GetProperty("run").GetString()!).Equals(Path.Combine(serverRunsDirectory, runId), StringComparison.OrdinalIgnoreCase))
            throw new InvalidDataException("Formal READY is not this new owned run.");
        var instances = ready.GetProperty("instances").EnumerateArray().ToArray();
        if (instances.Length != 1 || !instances.Select(x => x.GetProperty("role").GetString()).SequenceEqual(new[] { "standard" }))
            throw new InvalidDataException("Formal standard role required.");
        var owners = new Dictionary<string, int>();
        foreach (var instance in instances)
        {
            var identity = instance.GetProperty("identity");
            if (!instance.GetProperty("ready").GetBoolean() || instance.GetProperty("ready_marker").GetString() != "[LiTo] ready"
                || identity.GetProperty("pid").GetInt32() <= 0 || identity.GetProperty("birth_filetime").GetInt64() <= 0
                || !identity.GetProperty("image").GetString()!.Equals(jvmImage, StringComparison.OrdinalIgnoreCase))
                throw new InvalidDataException("Actual formal READY identity/marker mismatch.");
            owners.Add(instance.GetProperty("role").GetString()!, identity.GetProperty("pid").GetInt32());
        }
        if (owners.Values.Distinct().Count() != 1) throw new InvalidDataException("Distinct actual formal JVM identities required.");
        var endpoints = ready.GetProperty("endpoints").EnumerateArray().ToArray();
        if (endpoints.Length != 2) throw new InvalidDataException("Both formal standard endpoints required.");
        foreach (var expected in Endpoints)
        {
            var matches = endpoints.Where(x => x.GetProperty("role").GetString() == expected.Role && x.GetProperty("kind").GetString() == expected.Kind
                && x.GetProperty("host").GetString() == expected.Host && x.GetProperty("port").GetInt32() == expected.Port).ToArray();
            if (matches.Length != 1 || !matches[0].GetProperty("connect").GetBoolean() || matches[0].GetProperty("payload_bytes_sent").GetInt32() != 0
                || matches[0].GetProperty("owner").GetProperty("pid").GetInt32() != owners[expected.Role]
                || matches[0].GetProperty("owner").GetProperty("state").GetInt32() != 2
                || matches[0].GetProperty("owner").GetProperty("local_host").GetString() != expected.Host
                || matches[0].GetProperty("owner").GetProperty("local_port").GetInt32() != expected.Port)
                throw new InvalidDataException("Formal READY endpoint owner/connection mismatch; no alternate-port fallback.");
        }
        return ready;
    }
}
