using System.Security.Cryptography;
using System.Text.Json;

namespace LiTo381Launcher;

internal sealed record FormalStandardSources(MemorySourcePin Python, MemorySourcePin Entry, MemorySourcePin Java, MemorySourcePin Jar)
{
    internal static readonly FormalStandardSources Reviewed = new(
        new(@"C:\Users\CHAOREN\AppData\Local\Programs\Python\Python312\python.exe", 104952, "4D6F5F81A4BCA11191C4C7C6B43632694D0A4CE74E068619D8FDC161D469859A"),
        new(@"D:\lineage開發\3-lito開發\lito登入器\LiTo381-Launcher\app\server\Start-Shared-World.py", 29817, "971A99971F52BDFAA7E7BB916AB325EA8500E79C8F5F03C237E171F5A8BBE3C5"),
        new(@"C:\Program Files\Microsoft\jdk-17.0.17.10-hotspot\bin\java.exe", 50208, "0D3815053960E0B7695A09E8C26406C53D92B13C71E946CCB6EB84E6A8028EDE"),
        new(@"D:\lineage開發\3-lito開發\lito登入器\LiTo381-Launcher\app\server\LiTo381-SharedResources-candidate.jar", 20054975, "E1498871C7BE6B287821C57EF5D95933DB1740D9B4D4A80F6C3465F21D99D498"));
    internal string RunsDirectory => Path.Combine(Path.GetDirectoryName(Entry.Path)!, "runs");
    internal void Verify()
    {
        foreach (var pin in new[] { Python, Entry, Java, Jar }) pin.Verify();
    }
    internal JsonElement ValidateReady(JsonElement ready, string runId, MemoryProcessIdentity helper)
    {
        FormalStandardReady.Validate(ready, runId, RunsDirectory, Java.Path);
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
        if (role != "standard" || !Path.GetFullPath(configPath).Equals(expectedConfig, StringComparison.OrdinalIgnoreCase)
            || full7.Pid != pid || full7.Filetime != birth.ToString(System.Globalization.CultureInfo.InvariantCulture)
            || full7.ParentPid != helperPid || !full7.Path.Equals(Java.Path, StringComparison.OrdinalIgnoreCase) || full7.ImageSha256 != Java.Sha256
            || !argv.SequenceEqual(new[] { Java.Path }.Concat(SourceServerChoice.JvmOptions()).Concat(new[] { "-Dlito.resourceProfile=tw13081901-v1", "-Dlito.extendedItemDetails=true", @"-Dlito.commonCatalog=D:\lineage開發\3-lito開發\lito登入器\LiTo381-Launcher\app\common_catalog.bin", "-jar", Jar.Path, configPath, "--service-mode=standard" }))
            || full7.CommandArgvDigest != Convert.ToHexString(SHA256.HashData(JsonSerializer.SerializeToUtf8Bytes(argv, MemoryStartupJson.Options)))
            || full7.CommandSha256.Length != 64 || full7.CommandSha256.Any(c => !"0123456789ABCDEF".Contains(c)))
            throw new InvalidDataException("Memory test JVM full7/current run command ownership mismatch.");
    }
}
