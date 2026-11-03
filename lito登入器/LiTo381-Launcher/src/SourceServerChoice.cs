using System.Diagnostics;
using System.Text;
using System.Text.Json;

namespace LiTo381Launcher;

internal sealed record SourceServerChoice(int Id, string Name, string Database, int Port)
{
    internal const string ConfigRoot = @"D:\lineage開發\3-lito開發\lito伺服器\LiTo381-Server\config";
    public override string ToString() => $"{Id}. {Name}";
    internal static JsonElement EffectiveConfiguration()
    {
        var pins = FormalStandardSources.Reviewed;
        pins.Java.Verify(); pins.Jar.Verify();
        var start = new ProcessStartInfo(pins.Java.Path) { UseShellExecute=false, CreateNoWindow=true,
            RedirectStandardOutput=true, RedirectStandardError=true, StandardOutputEncoding=Encoding.UTF8, StandardErrorEncoding=Encoding.UTF8 };
        foreach (var arg in new[] { "-Dfile.encoding=UTF-8", "-cp", pins.Jar.Path+";"+Path.Combine(ConfigRoot,"../lib/*"),
            "local.lito381.server.UnifiedConfiguration", Path.Combine(ConfigRoot,"server.properties") }) start.ArgumentList.Add(arg);
        using var process = Process.Start(start) ?? throw new IOException("無法啟動設定驗證。");
        var output=process.StandardOutput.ReadToEndAsync();var error=process.StandardError.ReadToEndAsync();
        if(!process.WaitForExit(20000)) { process.Kill(); process.WaitForExit(); throw new IOException("讀取資料庫來源清單逾時。"); }
        if(process.ExitCode!=0) throw new InvalidDataException("設定／來源表檢查失敗："+error.GetAwaiter().GetResult().Trim());
        using var document=JsonDocument.Parse(output.GetAwaiter().GetResult());return document.RootElement.Clone();
    }
    internal static IReadOnlyList<SourceServerChoice> Load() => EffectiveConfiguration().GetProperty("routes").EnumerateArray()
        .Select(row=>new SourceServerChoice(row.GetProperty("id").GetInt32(),row.GetProperty("name").GetString()!,row.GetProperty("database").GetString()!,row.GetProperty("port").GetInt32())).ToArray();
    internal static string[] JvmOptions()
    {
        var settings=EffectiveConfiguration().GetProperty("settings");
        return new[] {"-Xms"+settings.GetProperty("memory.initial").GetString(),"-Xmx"+settings.GetProperty("memory.maximum").GetString(),"-XX:ActiveProcessorCount="+settings.GetProperty("cpu.limit").GetString()};
    }
}
