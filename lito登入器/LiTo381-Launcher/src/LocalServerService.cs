using System.Diagnostics;
using System.Net;
using System.Net.Sockets;
using System.Security.Cryptography;

namespace LiTo381Launcher;

public sealed record LocalServerEndpoint(string Role, string Kind, string Host, int Port)
{
    public override string ToString() => $"{Role}/{Kind}={Host}:{Port}";
}

public sealed record LocalServerLaunchInstance(
    string Role,
    string ServiceMode,
    string ConfigFileName,
    string ConfigPath,
    IReadOnlyList<LocalServerEndpoint> Endpoints,
    ProcessStartInfo StartInfo);

public sealed class LocalServerRuntime : IDisposable
{
    private readonly List<Process> _processes = [];
    private readonly Action<string>? _log;
    private bool _disposed;

    internal LocalServerRuntime(
        IReadOnlyList<LocalServerLaunchInstance> instances, Action<string>? log)
    {
        Instances = instances;
        _log = log;
    }

    public IReadOnlyList<LocalServerLaunchInstance> Instances { get; }
    public IReadOnlyList<int> ProcessIds => _processes.Select(process => process.Id).ToArray();

    internal void Add(Process process) => _processes.Add(process);

    public void Dispose()
    {
        if (_disposed) return;
        _disposed = true;
        for (var index = _processes.Count - 1; index >= 0; index--)
        {
            var process = _processes[index];
            try
            {
                if (!process.HasExited) process.Kill(entireProcessTree: true);
                process.WaitForExit(3000);
                _log?.Invoke($"[Server] 已停止 PID {process.Id}。");
            }
            catch (Exception ex)
            {
                _log?.Invoke($"[Server] 停止 PID {SafeProcessId(process)} 時發生錯誤：{ex.Message}");
            }
            finally
            {
                process.Dispose();
            }
        }
        _processes.Clear();
    }

    private static string SafeProcessId(Process process)
    {
        try { return process.Id.ToString(); }
        catch { return "<unknown>"; }
    }
}

public static class LocalServerService
{
    private const string ReadyMarker = "[LiTo] ready";

    public static IReadOnlyList<ValidationItem> Validate(
        string serverRoot, ServerProfileDefinition profile, NativeServiceMode serviceMode)
    {
        var items = new List<ValidationItem>();
        var serverJar = Path.Combine(serverRoot, "LiTo381-Server.jar");
        Add(items, "LiTo local server", serverJar);
        if (File.Exists(serverJar))
        {
            var actualHash = Sha256(serverJar);
            items.Add(new ValidationItem(
                "LiTo local server 0.7.864",
                actualHash.Equals(
                    LauncherService.ExpectedLocalServerSha256,
                    StringComparison.OrdinalIgnoreCase),
                actualHash.Equals(
                    LauncherService.ExpectedLocalServerSha256,
                    StringComparison.OrdinalIgnoreCase)
                    ? $"版本指紋 {actualHash}"
                    : $"Server 版本不符：{actualHash}；預期 {LauncherService.ExpectedLocalServerSha256}"));
        }
        items.Add(new ValidationItem(
            "完整素質橋接解析器",
            ExtendedCharacterStatsProtocol.SelfTest(),
            $"LiTo-Extended-Stats schema v{ExtendedCharacterStatsProtocol.SchemaVersion}"));

        var configNames = serviceMode == NativeServiceMode.RentalServer
            ? new[] { profile.RentalBattleServerConfig, profile.RentalSourceServerConfig }
            : new[] { profile.LocalServerConfig };
        foreach (var configName in configNames
                     .Where(name => !string.IsNullOrWhiteSpace(name))
                     .Distinct(StringComparer.OrdinalIgnoreCase))
        {
            try
            {
                Add(items, $"LiTo server config ({configName})", ResolveConfig(serverRoot, configName));
            }
            catch (Exception ex)
            {
                items.Add(new ValidationItem($"LiTo server config ({configName})", false, ex.Message));
            }
        }

        var java = FindJava17();
        items.Add(new ValidationItem("Java 17 runtime", java is not null, java ?? "找不到 Java 17"));
        try
        {
            var plan = CreateLaunchPlan(serverRoot, profile, serviceMode);
            items.Add(new ValidationItem(
                serviceMode == NativeServiceMode.RentalServer
                    ? "租賃城鎮／戰鬥雙服契約"
                    : "一般擂台伺服器契約",
                true,
                string.Join("；", plan.Select(instance =>
                    $"{instance.Role}:{instance.ConfigFileName} [{string.Join(", ", instance.Endpoints)}]"))));
        }
        catch (Exception ex)
        {
            items.Add(new ValidationItem(
                serviceMode == NativeServiceMode.RentalServer
                    ? "租賃城鎮／戰鬥雙服契約"
                    : "一般擂台伺服器契約",
                false,
                ex.Message));
        }
        return items;
    }

    public static IReadOnlyList<LocalServerLaunchInstance> CreateLaunchPlan(
        string serverRoot, ServerProfileDefinition profile, NativeServiceMode serviceMode)
    {
        var java = FindJava17() ?? throw new FileNotFoundException("找不到 Java 17 runtime。");
        var jar = Path.GetFullPath(Path.Combine(serverRoot, "LiTo381-Server.jar"));
        if (!File.Exists(jar)) throw new FileNotFoundException("找不到 LiTo 本機伺服器。", jar);
        var actualServerHash = Sha256(jar);
        if (!actualServerHash.Equals(
                LauncherService.ExpectedLocalServerSha256,
                StringComparison.OrdinalIgnoreCase))
        {
            throw new InvalidDataException(
                $"LiTo 本機 Server 版本不符：{actualServerHash}；"
                + $"預期 {LauncherService.ExpectedLocalServerSha256}。"
                + "登入器、Compat 與 Server 必須使用同一套寬位元協定版本。");
        }

        if (serviceMode == NativeServiceMode.StandardArena)
        {
            var standard = LoadInstance(
                serverRoot, java, jar, "一般擂台", "standard",
                profile.LocalServerConfig, profile, true);
            return [standard];
        }
        if (serviceMode != NativeServiceMode.RentalServer)
        {
            throw new ArgumentOutOfRangeException(nameof(serviceMode), serviceMode, null);
        }
        if (string.IsNullOrWhiteSpace(profile.RentalSourceServerConfig)
                || string.IsNullOrWhiteSpace(profile.RentalBattleServerConfig))
        {
            throw new InvalidDataException(
                "此 profile 沒有 rentalSourceServerConfig/rentalBattleServerConfig；請升級為 schemaVersion 3。");
        }

        var source = LoadInstance(
            serverRoot, java, jar, "租賃城鎮", "rental-source",
            profile.RentalSourceServerConfig, profile, true);
        var battle = LoadInstance(
            serverRoot, java, jar, "租賃戰鬥", "rental-battle",
            profile.RentalBattleServerConfig, profile, false);
        ValidateRentalContract(serverRoot, source, battle);

        // The native town directory may advertise the battle endpoint immediately,
        // so make the destination listener ready before exposing the source.
        return [battle, source];
    }

    private static string Sha256(string path)
    {
        using var stream = File.OpenRead(path);
        return Convert.ToHexString(SHA256.HashData(stream));
    }

    public static async Task<LocalServerRuntime> StartAsync(
        string serverRoot,
        ServerProfileDefinition profile,
        NativeServiceMode serviceMode,
        Action<string>? log = null,
        TimeSpan? readyTimeout = null,
        Action<ExtendedCharacterStatsSnapshot>? extendedStats = null,
        bool numericBreakthrough = false)
    {
        var instances = CreateLaunchPlan(serverRoot, profile, serviceMode);
        var endpoints = instances.SelectMany(instance => instance.Endpoints)
            .DistinctBy(endpoint => (endpoint.Host, endpoint.Port))
            .ToArray();
        var occupied = new List<LocalServerEndpoint>();
        foreach (var endpoint in endpoints)
        {
            if (await CanConnectAsync(endpoint.Host, endpoint.Port, TimeSpan.FromMilliseconds(300)))
            {
                occupied.Add(endpoint);
            }
        }
        if (occupied.Count > 0)
        {
            throw new InvalidOperationException(
                "本機伺服器端點已被占用，無法確認其設定來源：" + string.Join("、", occupied));
        }

        var runtime = new LocalServerRuntime(instances, log);
        try
        {
            foreach (var instance in instances)
            {
                if (numericBreakthrough)
                {
                    instance.StartInfo.Environment["LITO_NUMERIC_BREAKTHROUGH"] =
                        LauncherService.NumericBreakthroughSchemaVersion.ToString();
                }
                var ready = new TaskCompletionSource<bool>(TaskCreationOptions.RunContinuationsAsynchronously);
                var process = new Process { StartInfo = instance.StartInfo, EnableRaisingEvents = true };
                process.OutputDataReceived += (_, args) =>
                {
                    if (args.Data is null) return;
                    if (ExtendedCharacterStatsProtocol.TryParse(args.Data, out var snapshot))
                    {
                        extendedStats?.Invoke(snapshot);
                    }
                    log?.Invoke($"[Server/{instance.Role}] {args.Data}");
                    if (args.Data.Contains(ReadyMarker, StringComparison.Ordinal)) ready.TrySetResult(true);
                };
                process.ErrorDataReceived += (_, args) =>
                {
                    if (args.Data is not null) log?.Invoke($"[Server/{instance.Role} ERR] {args.Data}");
                };
                process.Exited += (_, _) =>
                {
                    var exitCode = "unknown";
                    try { exitCode = process.ExitCode.ToString(); } catch { }
                    ready.TrySetException(new InvalidOperationException(
                        $"{instance.Role}伺服器提前結束，代碼 {exitCode}。"));
                };

                log?.Invoke($"[Server] 啟動 {instance.Role}；config={instance.ConfigFileName}");
                if (!process.Start()) throw new InvalidOperationException($"無法啟動{instance.Role}伺服器。");
                runtime.Add(process);
                process.BeginOutputReadLine();
                process.BeginErrorReadLine();
                await ready.Task.WaitAsync(readyTimeout ?? TimeSpan.FromSeconds(15));
            }

            foreach (var endpoint in endpoints)
            {
                if (!await CanConnectAsync(endpoint.Host, endpoint.Port, TimeSpan.FromSeconds(2)))
                {
                    throw new InvalidOperationException($"伺服器已回報 ready，但端點不可連線：{endpoint}");
                }
            }
            return runtime;
        }
        catch
        {
            runtime.Dispose();
            throw;
        }
    }

    public static async Task<bool> CanConnectAsync(string host, int port, TimeSpan timeout)
    {
        using var client = new TcpClient();
        try
        {
            await client.ConnectAsync(host, port).WaitAsync(timeout);
            return true;
        }
        catch
        {
            return false;
        }
    }

    private static LocalServerLaunchInstance LoadInstance(
        string serverRoot,
        string java,
        string jar,
        string role,
        string serviceMode,
        string configFileName,
        ServerProfileDefinition profile,
        bool requireProfileEndpoint)
    {
        var configPath = ResolveConfig(serverRoot, configFileName);
        if (!File.Exists(configPath)) throw new FileNotFoundException($"找不到{role}設定。", configPath);
        var binding = ValidateProfileBinding(configPath, profile);
        if (!binding.Passed) throw new InvalidDataException($"{role}: {binding.Detail}");

        var properties = ReadEffectiveProperties(configPath, serviceMode);
        var loginHost = Required(properties, "bind", role);
        var gameHost = Required(properties, "game.bind", role);
        var port = RequiredPort(properties, "port", role);
        var channelPorts = ChannelPorts(properties, port, role);
        var advertisedHost = Required(properties, "advertised.host", role);
        var advertisedPort = RequiredPort(properties, "advertised.port", role);
        RequireLoopback(loginHost, $"{role} bind");
        RequireLoopback(gameHost, $"{role} game.bind");
        if (string.Equals(loginHost, gameHost, StringComparison.OrdinalIgnoreCase))
        {
            throw new InvalidDataException($"{role} bind 與 game.bind 不可相同。");
        }
        if (!string.Equals(advertisedHost, gameHost, StringComparison.OrdinalIgnoreCase)
                || advertisedPort != port)
        {
            throw new InvalidDataException(
                $"{role} advertised endpoint 必須等於 game.bind/port ({gameHost}:{port})。");
        }
        if (requireProfileEndpoint
                && (!string.Equals(loginHost, profile.Host, StringComparison.OrdinalIgnoreCase)
                    || port != profile.Port))
        {
            throw new InvalidDataException(
                $"{role} login endpoint {loginHost}:{port} 與 profile {profile.Host}:{profile.Port} 不符。");
        }

        var info = new ProcessStartInfo
        {
            FileName = java,
            WorkingDirectory = serverRoot,
            UseShellExecute = false,
            CreateNoWindow = true,
            RedirectStandardOutput = true,
            RedirectStandardError = true
        };
        info.ArgumentList.Add("-jar");
        info.ArgumentList.Add(jar);
        info.ArgumentList.Add(configPath);
        info.ArgumentList.Add($"--service-mode={serviceMode}");
        info.Environment["LITO_EXTENDED_STATS_SCHEMA"] =
            ExtendedCharacterStatsProtocol.SchemaVersion.ToString();
        var endpoints = new List<LocalServerEndpoint>
        {
            new(role, "LOGIN", loginHost, port)
        };
        for (var channelId = 0; channelId < channelPorts.Count; channelId++)
        {
            var kind = channelPorts.Count == 4
                ? $"GAME-CHANNEL-{channelId}" : "GAME";
            endpoints.Add(new(role, kind, gameHost, channelPorts[channelId]));
        }
        return new LocalServerLaunchInstance(
            role,
            serviceMode,
            configFileName,
            configPath,
            endpoints,
            info);
    }

    private static void ValidateRentalContract(
        string serverRoot, LocalServerLaunchInstance source, LocalServerLaunchInstance battle)
    {
        var sourceProperties = ReadEffectiveProperties(source.ConfigPath, source.ServiceMode);
        var battleProperties = ReadEffectiveProperties(battle.ConfigPath, battle.ServiceMode);
        RequireBoolean(sourceProperties, "database.read.only", true, source.Role);
        RequireBoolean(battleProperties, "database.read.only", true, battle.Role);
        RequireBoolean(sourceProperties, "lito.rental.town.directory.enabled", true, source.Role);
        RequireBoolean(battleProperties, "lito.rental.town.directory.enabled", false, battle.Role);

        var sourceJournal = ResolvePropertyPath(
            source.ConfigPath, Required(sourceProperties, "lito.rental.handoff.file", source.Role));
        var battleJournal = ResolvePropertyPath(
            battle.ConfigPath, Required(battleProperties, "lito.rental.handoff.file", battle.Role));
        if (!sourceJournal.Equals(battleJournal, StringComparison.OrdinalIgnoreCase))
        {
            throw new InvalidDataException("租賃城鎮端與戰鬥端必須共用同一個 handoff journal。");
        }
        var runtimeRoot = Path.GetFullPath(Path.Combine(serverRoot, "runtime"));
        if (!sourceJournal.StartsWith(runtimeRoot + Path.DirectorySeparatorChar,
                StringComparison.OrdinalIgnoreCase))
        {
            throw new InvalidDataException("租賃 handoff journal 必須位於伺服器 runtime 目錄內。");
        }

        var overlap = source.Endpoints.Select(endpoint => (endpoint.Host, endpoint.Port))
            .Intersect(battle.Endpoints.Select(endpoint => (endpoint.Host, endpoint.Port)))
            .ToArray();
        if (overlap.Length > 0)
        {
            throw new InvalidDataException("租賃城鎮端與戰鬥端不可共用監聽端點。");
        }

        // The native cross-process branch reconnects to the destination LOGIN
        // endpoint first. Its S_SERVER_LIST then performs the ordinary second
        // hop to the destination GAME endpoint.
        var battleLogin = battle.Endpoints.Single(endpoint => endpoint.Kind == "LOGIN");
        var routes = Required(sourceProperties, "lito.rental.teleport.routes", source.Role)
            .Split(',', StringSplitOptions.RemoveEmptyEntries | StringSplitOptions.TrimEntries)
            .Select(route => route.Split('|', StringSplitOptions.TrimEntries))
            .ToArray();
        foreach (var action in new[] { "LT_TELEPORT_B", "LT_TELEPORT_C" })
        {
            var matched = routes.Any(parts => parts.Length == 5
                && parts[0].Equals("TeleportMe", StringComparison.Ordinal)
                && parts[1].Equals(action, StringComparison.Ordinal)
                && parts[3].Equals(battleLogin.Host, StringComparison.OrdinalIgnoreCase)
                && int.TryParse(parts[4], out var routePort)
                && routePort == battleLogin.Port);
            if (!matched)
            {
                throw new InvalidDataException(
                    $"租賃城鎮端 {action} 必須精確路由至戰鬥 LOGIN 端 "
                    + $"{battleLogin.Host}:{battleLogin.Port}。");
            }
        }
    }

    private static ValidationItem ValidateProfileBinding(
        string configPath, ServerProfileDefinition profile)
    {
        var properties = ReadProperties(configPath);
        properties.TryGetValue("origin.source.id", out var sourceId);
        properties.TryGetValue("origin.source.name", out var sourceName);
        properties.TryGetValue("login.classic.server.names", out var advertisedNames);
        properties.TryGetValue("database.read.only", out var readOnly);
        properties.TryGetValue("database.mode", out var databaseMode);
        properties.TryGetValue("database.static.database", out var staticDatabase);
        properties.TryGetValue("database.dynamic.databases", out var dynamicDatabasesValue);
        var names = (advertisedNames ?? string.Empty)
            .Split(',', StringSplitOptions.RemoveEmptyEntries | StringSplitOptions.TrimEntries);
        var expectedNames = profile.SchemaVersion >= 4
            ? profile.ClassicSourceNames : [profile.OriginSourceName];
        var dynamicDatabases = (dynamicDatabasesValue ?? string.Empty)
            .Split(',', StringSplitOptions.RemoveEmptyEntries | StringSplitOptions.TrimEntries);
        var databaseLayoutMatches = profile.SchemaVersion < 4
            || (string.Equals(databaseMode, "lito_mysql", StringComparison.OrdinalIgnoreCase)
                && string.Equals(staticDatabase, profile.StaticDatabase,
                    StringComparison.OrdinalIgnoreCase)
                && dynamicDatabases.SequenceEqual(
                    profile.DynamicDatabases, StringComparer.OrdinalIgnoreCase));
        var passed = string.Equals(sourceId, profile.OriginSourceId, StringComparison.Ordinal)
            && string.Equals(sourceName, profile.OriginSourceName, StringComparison.Ordinal)
            && names.SequenceEqual(expectedNames, StringComparer.Ordinal)
            && databaseLayoutMatches
            && string.Equals(readOnly, "true", StringComparison.OrdinalIgnoreCase);
        var detail = passed
            ? $"profile={profile.OriginSourceId}/{profile.OriginSourceName}; "
                + $"S_SERVER_LIST=[{string.Join(",", names)}]; "
                + (profile.SchemaVersion >= 4
                    ? $"static={staticDatabase}; dynamic=[{string.Join(",", dynamicDatabases)}]; "
                    : string.Empty)
                + "database.read.only=true"
            : $"Profile/config mismatch: expected {profile.OriginSourceId}/{profile.OriginSourceName} "
                + $"with S_SERVER_LIST=[{string.Join(",", expectedNames)}] and read-only database layout; got "
                + $"id={sourceId ?? "<missing>"}, name={sourceName ?? "<missing>"}, "
                + $"servers=[{string.Join(",", names)}], mode={databaseMode ?? "<missing>"}, "
                + $"static={staticDatabase ?? "<missing>"}, "
                + $"dynamic=[{string.Join(",", dynamicDatabases)}], "
                + $"readOnly={readOnly ?? "<missing>"}.";
        return new ValidationItem("角色來源／資料庫路由綁定", passed, detail);
    }

    private static IReadOnlyDictionary<string, string> ReadProperties(string path)
    {
        var result = new Dictionary<string, string>(StringComparer.OrdinalIgnoreCase);
        foreach (var rawLine in File.ReadLines(path))
        {
            var line = rawLine.Trim();
            if (line.Length == 0 || line.StartsWith('#') || line.StartsWith('!')) continue;
            var separator = line.IndexOfAny(['=', ':']);
            if (separator <= 0) continue;
            result[line[..separator].Trim()] = line[(separator + 1)..].Trim();
        }
        return result;
    }

    private static IReadOnlyDictionary<string, string> ReadEffectiveProperties(
        string path, string serviceMode)
    {
        var result = new Dictionary<string, string>(
            ReadProperties(path), StringComparer.OrdinalIgnoreCase);
        void Set(string key, string value) => result[key] = value;

        switch (serviceMode.Trim().ToLowerInvariant())
        {
            case "standard":
                break;
            case "rental-source":
                result.Remove("lito.channel.ports");
                Set("bind", "127.0.0.1");
                Set("game.bind", "127.0.0.2");
                Set("port", "32000");
                Set("advertised.host", "127.0.0.2");
                Set("advertised.port", "32000");
                Set("server.name", "LiTo Shared Rental Town");
                Set("lito.rental.town.directory.enabled", "true");
                Set("lito.rental.town.room.id", "1342177281");
                Set("lito.rental.town.directory.name", "LiTo 租賃村莊");
                Set("lito.rental.town.directory.host", "127.0.0.1");
                Set("lito.rental.town.directory.port", "32000");
                Set("lito.rental.town.map", "500");
                Set("lito.rental.teleport.routes",
                    "TeleportMe|LT_TELEPORT_B|LiTo 租賃戰鬥|127.0.0.3|32001,"
                    + "TeleportMe|LT_TELEPORT_C|LiTo 租賃戰鬥|127.0.0.3|32001");
                Set("lito.rental.handoff.file", "../runtime/rental-handoff.state");
                Set("capture.directory", "../runtime/captures-rental-source");
                Set("state.file", "../runtime/server-rental-source.state");
                Set("state.persistence.enabled", "false");
                Set("classic.world.compatibility.enabled", "false");
                Set("classic.world.monster.ai.enabled", "false");
                break;
            case "rental-battle":
                result.Remove("lito.channel.ports");
                Set("bind", "127.0.0.3");
                Set("game.bind", "127.0.0.4");
                Set("port", "32001");
                Set("advertised.host", "127.0.0.4");
                Set("advertised.port", "32001");
                Set("server.name", "LiTo Shared Rental Battle");
                Set("lito.rental.town.directory.enabled", "false");
                Set("lito.rental.teleport.routes", string.Empty);
                Set("lito.rental.handoff.file", "../runtime/rental-handoff.state");
                Set("capture.directory", "../runtime/captures-rental-battle");
                Set("state.file", "../runtime/server-rental-battle.state");
                Set("state.persistence.enabled", "true");
                Set("classic.world.compatibility.enabled", "false");
                Set("classic.world.monster.ai.enabled", "false");
                break;
            default:
                throw new InvalidDataException(
                    $"不支援的伺服器模式：{serviceMode}。");
        }
        return result;
    }

    private static string Required(
        IReadOnlyDictionary<string, string> properties, string key, string role)
    {
        if (!properties.TryGetValue(key, out var value) || string.IsNullOrWhiteSpace(value))
        {
            throw new InvalidDataException($"{role}缺少必要設定 {key}。");
        }
        return value;
    }

    private static int RequiredPort(
        IReadOnlyDictionary<string, string> properties, string key, string role)
    {
        var value = Required(properties, key, role);
        if (!int.TryParse(value, out var port) || port is < 1 or > 65535)
        {
            throw new InvalidDataException($"{role}的 {key} 不是有效連接埠：{value}");
        }
        return port;
    }

    private static IReadOnlyList<int> ChannelPorts(
        IReadOnlyDictionary<string, string> properties, int primaryPort, string role)
    {
        if (properties.TryGetValue("lito.channel.ports", out var value)
                && !string.IsNullOrWhiteSpace(value))
        {
            throw new InvalidDataException(
                $"{role}的 lito.channel.ports 已停用；四個頻道是同一 GAME 連接埠上的四個獨立大廳。");
        }
        return [primaryPort];
    }

    private static void RequireBoolean(
        IReadOnlyDictionary<string, string> properties, string key, bool expected, string role)
    {
        var value = Required(properties, key, role);
        if (!bool.TryParse(value, out var parsed) || parsed != expected)
        {
            throw new InvalidDataException($"{role}的 {key} 必須是 {expected.ToString().ToLowerInvariant()}。");
        }
    }

    private static void RequireLoopback(string host, string description)
    {
        if (!IPAddress.TryParse(host, out var address)
                || address.AddressFamily != AddressFamily.InterNetwork
                || !IPAddress.IsLoopback(address))
        {
            throw new InvalidDataException($"{description} 必須是 IPv4 loopback 位址。");
        }
    }

    private static string ResolvePropertyPath(string configPath, string value) =>
        Path.GetFullPath(Path.Combine(Path.GetDirectoryName(configPath)!, value));

    private static string ResolveConfig(string serverRoot, string configFileName)
    {
        if (string.IsNullOrWhiteSpace(configFileName)
                || !configFileName.EndsWith(".properties", StringComparison.OrdinalIgnoreCase)
                || !Path.GetFileName(configFileName).Equals(configFileName, StringComparison.OrdinalIgnoreCase))
        {
            throw new InvalidDataException("Invalid local server config file name.");
        }
        var configRoot = Path.GetFullPath(Path.Combine(serverRoot, "config"));
        var configPath = Path.GetFullPath(Path.Combine(configRoot, configFileName));
        if (!Path.GetDirectoryName(configPath)!.Equals(configRoot, StringComparison.OrdinalIgnoreCase))
        {
            throw new InvalidDataException("Local server config must stay inside the config directory.");
        }
        return configPath;
    }

    private static string? FindJava17()
    {
        var candidates = new List<string>();
        var javaHome = Environment.GetEnvironmentVariable("JAVA_HOME");
        if (!string.IsNullOrWhiteSpace(javaHome)) candidates.Add(Path.Combine(javaHome, "bin", "java.exe"));
        candidates.Add(@"C:\Program Files\Microsoft\jdk-17.0.17.10-hotspot\bin\java.exe");
        foreach (var directory in (Environment.GetEnvironmentVariable("PATH") ?? string.Empty)
                     .Split(Path.PathSeparator, StringSplitOptions.RemoveEmptyEntries | StringSplitOptions.TrimEntries))
        {
            candidates.Add(Path.Combine(directory, "java.exe"));
        }
        foreach (var candidate in candidates.Distinct(StringComparer.OrdinalIgnoreCase))
        {
            if (!File.Exists(candidate)) continue;
            try
            {
                var version = FileVersionInfo.GetVersionInfo(candidate).FileMajorPart;
                if (version >= 17) return candidate;
            }
            catch
            {
                // Keep looking for a verifiable Java 17 runtime.
            }
        }
        return null;
    }

    private static void Add(ICollection<ValidationItem> items, string name, string path) =>
        items.Add(new ValidationItem(name, File.Exists(path), path));
}
