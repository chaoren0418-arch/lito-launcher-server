using System.Security.Cryptography;
using System.Text.Json;

namespace LiTo381Launcher;

internal sealed record MemoryServerControlSources(MemorySourcePin Python, MemorySourcePin Entry, MemorySourcePin Java, MemorySourcePin Jar)
{
    internal static readonly MemoryServerControlSources Reviewed = new(
        new(@"C:\Users\CHAOREN\AppData\Local\Programs\Python\Python312\python.exe", 104952, "4D6F5F81A4BCA11191C4C7C6B43632694D0A4CE74E068619D8FDC161D469859A"),
        new(@"D:\lineage開發\3-lito開發\lito伺服器\LiTo381-Server\deployment\memory\Start-Isolated-Memory-Rental.py", 27505, "A1C060ACB7E6C5BBA76B17FC3C076021CD5BA5C521712D8E1040A935E2E683D9"),
        new(@"C:\Program Files\Microsoft\jdk-17.0.17.10-hotspot\bin\java.exe", 50208, "0D3815053960E0B7695A09E8C26406C53D92B13C71E946CCB6EB84E6A8028EDE"),
        new(@"D:\lineage開發\3-lito開發\lito伺服器\LiTo381-Server\LiTo381-Server.jar", 5765902, "3025E3DF1C28F55019E79D5F21F9C1587B7268FB6417D6A75A591B6AECACB4B4"));
    internal string RunsDirectory => Path.Combine(Path.GetDirectoryName(Jar.Path)!, "runtime", "memory-checks");
    internal void Verify()
    {
        foreach (var pin in new[] { Python, Entry, Java, Jar }) pin.Verify();
    }
    internal JsonElement ValidateReady(JsonElement ready, string runId, MemoryProcessIdentity helper)
    {
        MemoryStartupReady.Validate(ready, runId, RunsDirectory, Java.Path);
        foreach (var instance in ready.GetProperty("instances").EnumerateArray())
        {
            var identity = instance.GetProperty("identity");
            var full7 = identity.GetProperty("full7").Deserialize<MemoryProcessIdentity>(MemoryStartupJson.Options)!;
            ValidateJvmIdentity(full7, identity.GetProperty("pid").GetInt32(), identity.GetProperty("birth_filetime").GetInt64(), helper.Pid,
                instance.GetProperty("argv").Deserialize<string[]>()!, instance.GetProperty("config").GetProperty("path").GetString()!, runId,
                instance.GetProperty("role").GetString()!);
        }
        return ready;
    }
    internal void ValidateJvmIdentity(MemoryProcessIdentity full7, int pid, long birth, int helperPid, string[] argv, string configPath, string runId, string role, string? ownedRunsDirectory = null)
    {
        var expectedConfig = Path.Combine(ownedRunsDirectory ?? RunsDirectory, runId, "config", $"server.dev.{role}.properties");
        if (role is not ("rental-battle" or "rental-source") || !Path.GetFullPath(configPath).Equals(expectedConfig, StringComparison.OrdinalIgnoreCase)
            || full7.Pid != pid || full7.Filetime != birth.ToString(System.Globalization.CultureInfo.InvariantCulture)
            || full7.ParentPid != helperPid || !full7.Path.Equals(Java.Path, StringComparison.OrdinalIgnoreCase) || full7.ImageSha256 != Java.Sha256
            || !argv.SequenceEqual(new[] { Java.Path, "-jar", Jar.Path, configPath })
            || full7.CommandArgvDigest != Convert.ToHexString(SHA256.HashData(JsonSerializer.SerializeToUtf8Bytes(argv, MemoryStartupJson.Options)))
            || full7.CommandSha256.Length != 64 || full7.CommandSha256.Any(c => !"0123456789ABCDEF".Contains(c)))
            throw new InvalidDataException("Memory test JVM full7/current run command ownership mismatch.");
    }
}
