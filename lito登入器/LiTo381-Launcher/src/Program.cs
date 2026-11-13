using System.Text.Json;

namespace LiTo381Launcher;

internal static class Program
{
    [STAThread]
    private static int Main(string[] args)
    {
        bool startMemoryServer, startMemoryGame, closeAfterMemoryGame;
        string? externalServerProfile;
        try
        {
            startMemoryServer = ResolveStartMemoryServer(args); startMemoryGame = ResolveStartMemoryGame(args); closeAfterMemoryGame = ResolveCloseAfterMemoryGame(args);
            externalServerProfile = ResolveExternalServerProfile(args);
        }
        catch (ArgumentException error) { Console.Error.WriteLine(error.Message); return 2; }
        if (args.Contains("--automated-isolated-test", StringComparer.OrdinalIgnoreCase))
            return AutomatedIsolatedStartup.Run(args);

        if (args.Contains("--server-orchestration-smoke-test", StringComparer.OrdinalIgnoreCase))
        {
            return RunServerOrchestrationSmokeTest(args);
        }
        if (args.Contains("--smoke-test", StringComparer.OrdinalIgnoreCase))
        {
            return RunSmokeTest(args);
        }

        ApplicationConfiguration.Initialize();
        var startDevelopment = args.Contains("--start-development-game", StringComparer.OrdinalIgnoreCase);
        if (startDevelopment && (!ResolveDefaultMemoryMode(args) || startMemoryServer || startMemoryGame || closeAfterMemoryGame))
            throw new ArgumentException("Development startup requires its own exclusive default GUI intent.");
        Application.Run(new MainForm(ResolveUiConfiguration(args), ResolveDefaultMemoryMode(args), startMemoryServer, startMemoryGame, closeAfterMemoryGame, startDevelopment, externalServerProfile));
        return 0;
    }

    internal const string ExternalServerProfileOption = "--external-server-profile";

    /// <summary>
    /// T140 / Issue #404: development/acceptance entry. Opens the default GUI but connects the
    /// native flow to an already running loopback Server described by the given profile; the
    /// bundled Server is not started and the port is not rewritten. Without the option the
    /// formal flow is unchanged (returns null).
    /// </summary>
    internal static string? ResolveExternalServerProfile(string[] args)
    {
        var index = Array.FindIndex(args, value => value.Equals(ExternalServerProfileOption, StringComparison.OrdinalIgnoreCase));
        if (index < 0) return null;
        if (Array.FindLastIndex(args, value => value.Equals(ExternalServerProfileOption, StringComparison.OrdinalIgnoreCase)) != index)
            throw new ArgumentException(ExternalServerProfileOption + " may be given only once.");
        if (index + 1 >= args.Length || args[index + 1].StartsWith("--", StringComparison.Ordinal) || string.IsNullOrWhiteSpace(args[index + 1]))
            throw new ArgumentException(ExternalServerProfileOption + " requires a profile JSON path.");
        if (!ResolveDefaultMemoryMode(args) || args.Any(arg => new[] {
                "--start-memory-server", "--start-memory-game", "--close-after-memory-game", "--start-development-game",
                "--automated-isolated-test", "--smoke-test", "--server-orchestration-smoke-test"
            }.Contains(arg, StringComparer.OrdinalIgnoreCase)))
            throw new ArgumentException(ExternalServerProfileOption + " opens the default GUI against an already running Server; incompatible launch modes refused.");
        return Path.GetFullPath(args[index + 1]);
    }

    internal static string? ResolveUiConfiguration(string[] args)
    {
        var isolated = Array.FindIndex(args, value => value.Equals("--current-isolated-test", StringComparison.OrdinalIgnoreCase));
        return isolated >= 0 ? (isolated + 1 < args.Length ? args[isolated + 1] : string.Empty)
            : null;
    }

    internal static bool ResolveDefaultMemoryMode(string[] args) =>
        !args.Contains("--current-isolated-test", StringComparer.OrdinalIgnoreCase)
        && !args.Contains("--legacy-formal-mode", StringComparer.OrdinalIgnoreCase);

    internal static bool ResolveStartMemoryServer(string[] args)
    {
        var requested = args.Contains("--start-memory-server", StringComparer.OrdinalIgnoreCase);
        if (requested && (args.Contains("--start-memory-game", StringComparer.OrdinalIgnoreCase) || !ResolveDefaultMemoryMode(args) || args.Any(arg => new[] {
            "--automated-isolated-test", "--smoke-test", "--server-orchestration-smoke-test"
        }.Contains(arg, StringComparer.OrdinalIgnoreCase))))
            throw new ArgumentException("--start-memory-server opens the default GUI and starts only its own memory test Server pair; incompatible launch modes refused.");
        return requested;
    }

    internal static bool ResolveStartMemoryGame(string[] args)
    {
        var requested = args.Contains("--start-memory-game", StringComparer.OrdinalIgnoreCase);
        if (requested && (!ResolveDefaultMemoryMode(args) || args.Any(arg => new[] {
            "--start-memory-server", "--automated-isolated-test", "--smoke-test", "--server-orchestration-smoke-test"
        }.Contains(arg, StringComparer.OrdinalIgnoreCase))))
            throw new ArgumentException("--start-memory-game opens the default GUI with one game Start intent; incompatible launch modes refused.");
        return requested;
    }

    internal static bool ResolveCloseAfterMemoryGame(string[] args)
    {
        var requested = args.Contains("--close-after-memory-game", StringComparer.OrdinalIgnoreCase);
        if (requested && !ResolveStartMemoryGame(args))
            throw new ArgumentException("--close-after-memory-game requires the exclusive --start-memory-game intent.");
        return requested;
    }

    private static int RunServerOrchestrationSmokeTest(string[] args)
    {
        var output = GetArgument(args, "--output") is { } outputArgument
            ? Path.GetFullPath(outputArgument)
            : Path.Combine(Environment.CurrentDirectory, "server-orchestration-smoke-test.json");
        LocalServerRuntime? runtime = null;
        try
        {
            var projectRoot = GetArgument(args, "--project-root") is { } projectRootArgument
                ? Path.GetFullPath(projectRootArgument)
                : WorkspaceLocator.FindProjectRoot(string.Empty);
            var profilePath = GetArgument(args, "--server-profile") is { } profileArgument
                ? Path.GetFullPath(profileArgument)
                : ServerProfileDefinition.FindDedicatedPath();
            var modeValue = GetArgument(args, "--native-service") ?? nameof(NativeServiceMode.RentalServer);
            if (!Enum.TryParse<NativeServiceMode>(modeValue, true, out var serviceMode))
            {
                throw new ArgumentException("--native-service 必須是 StandardArena 或 RentalServer。");
            }
            var holdSecondsValue = GetArgument(args, "--hold-seconds") ?? "0";
            if (!int.TryParse(holdSecondsValue, out var holdSeconds)
                    || holdSeconds is < 0 or > 300)
            {
                throw new ArgumentException("--hold-seconds 必須介於 0 到 300。");
            }

            var profile = ServerProfileDefinition.Load(profilePath);
            var serverRoot = LauncherService.GetServerRoot(projectRoot);
            var numericBreakthrough = args.Contains(
                "--numeric-breakthrough", StringComparer.OrdinalIgnoreCase);
            var logs = new List<string>();
            var startedAt = DateTimeOffset.UtcNow;
            runtime = LocalServerService.StartAsync(
                serverRoot, profile, serviceMode, line => logs.Add(line), TimeSpan.FromSeconds(20),
                numericBreakthrough: numericBreakthrough)
                .GetAwaiter().GetResult();
            var instances = runtime.Instances.Select(instance => new
            {
                instance.Role,
                instance.ConfigFileName,
                Endpoints = instance.Endpoints.Select(endpoint => endpoint.ToString()).ToArray()
            }).ToArray();
            var processIds = runtime.ProcessIds.ToArray();
            var endpoints = runtime.Instances.SelectMany(instance => instance.Endpoints)
                .DistinctBy(endpoint => (endpoint.Host, endpoint.Port))
                .ToArray();

            if (holdSeconds > 0)
            {
                logs.Add($"[Smoke] 保持雙服 {holdSeconds} 秒，供外部加密協定驗收。");
                Thread.Sleep(TimeSpan.FromSeconds(holdSeconds));
            }

            runtime.Dispose();
            runtime = null;
            Thread.Sleep(250);
            var lingering = endpoints.Where(endpoint => LocalServerService.CanConnectAsync(
                    endpoint.Host, endpoint.Port, TimeSpan.FromMilliseconds(300))
                .GetAwaiter().GetResult()).ToArray();
            if (lingering.Length > 0)
            {
                throw new InvalidOperationException(
                    "登入器關閉後仍有端點可連線：" + string.Join("、", lingering.AsEnumerable()));
            }

            WriteJson(output, new
            {
                Status = "PASS",
                LauncherVersion = typeof(Program).Assembly.GetName().Version?.ToString(),
                ServiceMode = serviceMode.ToString(),
                NumericBreakthrough = numericBreakthrough,
                Profile = profile.ProfileId,
                StartedAtUtc = startedAt,
                StoppedAtUtc = DateTimeOffset.UtcNow,
                StartOrder = instances,
                ProcessIds = processIds,
                ReadyEndpointCount = endpoints.Length,
                HoldSeconds = holdSeconds,
                AllProcessesStopped = true,
                AllEndpointsReleased = true,
                Logs = logs
            });
            return 0;
        }
        catch (Exception ex)
        {
            runtime?.Dispose();
            WriteJson(output, new
            {
                Status = "FAIL",
                LauncherVersion = typeof(Program).Assembly.GetName().Version?.ToString(),
                Error = ex.ToString()
            });
            return 1;
        }
    }

    private static string? GetArgument(string[] args, string name)
    {
        var index = Array.FindIndex(args, value => value.Equals(name, StringComparison.OrdinalIgnoreCase));
        return index >= 0 && index + 1 < args.Length ? args[index + 1] : null;
    }

    private static void WriteJson(string output, object value)
    {
        Directory.CreateDirectory(Path.GetDirectoryName(Path.GetFullPath(output))!);
        File.WriteAllText(output, JsonSerializer.Serialize(value, LauncherJson.Options));
    }

    private static int RunSmokeTest(string[] args)
    {
        try
        {
            var outputIndex = Array.FindIndex(args, value =>
                value.Equals("--output", StringComparison.OrdinalIgnoreCase));
            var output = outputIndex >= 0 && outputIndex + 1 < args.Length
                ? Path.GetFullPath(args[outputIndex + 1])
                : Path.Combine(Environment.CurrentDirectory, "launcher-smoke-test.json");

            var settings = LauncherSettingsStore.Load();
            settings.ProjectRoot = GetArgument(args, "--project-root") is { } projectRootArgument
                ? Path.GetFullPath(projectRootArgument)
                : WorkspaceLocator.FindProjectRoot(settings.ProjectRoot);
            var clientDirectoryIndex = Array.FindIndex(args, value =>
                value.Equals("--client-directory", StringComparison.OrdinalIgnoreCase));
            if (clientDirectoryIndex >= 0 && clientDirectoryIndex + 1 < args.Length)
            {
                settings.ClientDirectory = Path.GetFullPath(args[clientDirectoryIndex + 1]);
            }
            else
            {
                settings.ClientDirectory = LiToClientLocator.Find(
                    settings.ClientDirectory, settings.ProjectRoot);
            }
            settings.Mode = LaunchMode.ProtectedClient;
            settings.SelectionFlow = SelectionFlowMode.Native;
            settings.StartLocalServer = true;
            settings.NumericBreakthroughEnabled = true;
            settings.AllowNetwork = false;
            var nativeServiceIndex = Array.FindIndex(args, value =>
                value.Equals("--native-service", StringComparison.OrdinalIgnoreCase));
            if (nativeServiceIndex >= 0)
            {
                if (nativeServiceIndex + 1 >= args.Length
                        || !Enum.TryParse<NativeServiceMode>(
                            args[nativeServiceIndex + 1], true, out var nativeService))
                {
                    throw new ArgumentException(
                        "--native-service 必須是 StandardArena 或 RentalServer（0.5.8 值 PremiumService 仍可讀取）");
                }
                settings.NativeService = nativeService;
            }
            var profileIndex = Array.FindIndex(args, value =>
                value.Equals("--server-profile", StringComparison.OrdinalIgnoreCase));
            if (profileIndex >= 0 && profileIndex + 1 < args.Length)
            {
                settings.ServerProfilePath = Path.GetFullPath(args[profileIndex + 1]);
                settings.OriginSourceSelectionInitialized = true;
            }
            else
            {
                settings.ServerProfilePath = ServerProfileDefinition.FindDedicatedPath();
                settings.OriginSourceSelectionInitialized = true;
            }
            var validation = LauncherService.ValidateAsync(settings).GetAwaiter().GetResult();
            Directory.CreateDirectory(Path.GetDirectoryName(output)!);
            File.WriteAllText(output, JsonSerializer.Serialize(validation, LauncherJson.Options));
            return validation.CanLaunch ? 0 : 2;
        }
        catch (Exception ex)
        {
            var fallback = Path.Combine(Environment.CurrentDirectory, "launcher-smoke-test-error.txt");
            File.WriteAllText(fallback, ex.ToString());
            return 1;
        }
    }
}
