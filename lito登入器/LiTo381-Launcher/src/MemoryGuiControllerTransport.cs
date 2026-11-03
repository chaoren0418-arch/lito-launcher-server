using System.Text.Json;

namespace LiTo381Launcher;

// Intent/terminal transport for a creation-owned Root GUI controller. It releases no game source.
internal sealed class MemoryGuiControllerTransport
{
    internal const string EnvironmentName = "LITO_ROOT_MEMORY_GUI_CONTROLLER_HANDOFF";
    internal static bool IsRequested => new[] { EnvironmentName, EnvironmentName + "_BYTES", EnvironmentName + "_SHA256" }
        .Any(name => !string.IsNullOrEmpty(Environment.GetEnvironmentVariable(name)));
    internal MemorySourcePin HandoffPin { get; }
    readonly JsonElement handoff;
    private MemoryGuiControllerTransport(MemorySourcePin pin, JsonElement value) { HandoffPin = pin; handoff = value; }
    internal static MemoryGuiControllerTransport? Capture(MemoryStartContext context)
    {
        if (!IsRequested) return null;
        var path = Environment.GetEnvironmentVariable(EnvironmentName);
        if (string.IsNullOrEmpty(path) || !long.TryParse(Environment.GetEnvironmentVariable(EnvironmentName + "_BYTES"), out var bytes))
            throw new InvalidDataException("Complete exact Root GUI controller transport pin required.");
        var pin = new MemorySourcePin(path, bytes, Environment.GetEnvironmentVariable(EnvironmentName + "_SHA256") ?? ""); pin.Verify();
        using var doc = JsonDocument.Parse(File.ReadAllBytes(pin.Path)); var value = doc.RootElement.Clone();
        Require(value.GetProperty("schema").GetString() == "ROOT_MEMORY_GUI_CREATION_CONTROLLER_HANDOFF_1"
            && value.GetProperty("intent_only").GetBoolean() && !value.GetProperty("native_claim_created").GetBoolean()
            && !value.GetProperty("provider_lease_created").GetBoolean() && !value.GetProperty("runtime_authorized").GetBoolean(), "Source-only Root controller handoff required.");
        var apphost = value.GetProperty("apphost").Deserialize<MemorySourcePin>(MemoryStartupJson.Options)!;
        var assembly = value.GetProperty("assembly").Deserialize<MemorySourcePin>(MemoryStartupJson.Options)!;
        apphost.Verify(); assembly.Verify();
        Require(apphost == MemorySourcePin.Capture(context.LauncherIdentity.Path) && assembly == context.Assembly, "Controller exact current GUI apphost/assembly rejected.");
        var controller = value.GetProperty("controller").Deserialize<MemoryProcessIdentity>(MemoryStartupJson.Options)!;
        Require(value.GetProperty("controller").EnumerateObject().Select(p => p.Name).Order().SequenceEqual(new[] {
            "command_argv_digest", "command_sha256", "filetime", "image_sha256", "parent_pid", "path", "pid"
        }) && controller.ParentPid > 0 && Path.IsPathFullyQualified(controller.Path)
            && new[] { controller.ImageSha256, controller.CommandSha256, controller.CommandArgvDigest }.All(d => d is not null && d.Length == 64 && d.All(c => "0123456789ABCDEF".Contains(c))), "Exact declared parent full7 metadata required.");
        Require(controller.Pid == context.LauncherIdentity.ParentPid && controller.Pid != context.LauncherIdentity.Pid
            && long.TryParse(controller.Filetime, out var birth) && birth > 0 && birth <= long.Parse(context.LauncherIdentity.Filetime), "Actual GUI parent controller PID/creation order rejected.");
        var reviewPin = value.GetProperty("source_review").Deserialize<MemorySourcePin>(MemoryStartupJson.Options)!; reviewPin.Verify();
        using var reviewDoc = JsonDocument.Parse(File.ReadAllBytes(reviewPin.Path)); var review = reviewDoc.RootElement;
        Require(review.GetProperty("schema").GetString() == "ROOT_MEMORY_ORDINARY_GAME_CONTROLLER_SOURCE_REVIEW_1"
            && review.GetProperty("reviewer").GetString() == "supervisor" && review.GetProperty("decision").GetString() == "accepted-source-only"
            && review.GetProperty("scope").GetString() == "creation-owned-ordinary-GUI-controller-no-native-grant"
            && !review.GetProperty("runtime_authorized").GetBoolean() && !review.GetProperty("product_accepted").GetBoolean(), "Reviewed GUI transport source is not game admission.");
        Require(review.GetProperty("apphost").Deserialize<MemorySourcePin>(MemoryStartupJson.Options) == apphost
            && review.GetProperty("assembly").Deserialize<MemorySourcePin>(MemoryStartupJson.Options) == assembly, "Current GUI transport review pins differ.");
        foreach (var name in new[] { "controller_source", "python", "direction", "whole_root_source_review", "candidate_owner_source_review", "terminal_contract_source" })
            review.GetProperty(name).Deserialize<MemorySourcePin>(MemoryStartupJson.Options)!.Verify();
        var python = review.GetProperty("python").Deserialize<MemorySourcePin>(MemoryStartupJson.Options)!;
        Require(controller.Path == python.Path && controller.ImageSha256 == python.Sha256, "Declared Root controller reviewed image rejected.");
        var clocks = value.GetProperty("clocks");
        foreach (var pair in new[] { ("setup_seconds", 45), ("session_seconds", 900), ("proof_seconds", 120), ("owner_seconds", 300), ("context_seconds", 600), ("cleanup_seconds", 110) })
            Require(clocks.GetProperty(pair.Item1).GetInt32() == pair.Item2 && review.GetProperty("clocks").GetProperty(pair.Item1).GetInt32() == pair.Item2, "Original controller clock roles required.");
        var run = value.GetProperty("run_root").GetString()!;
        Require(Path.IsPathFullyQualified(run) && Path.GetDirectoryName(Path.GetFullPath(pin.Path)) == Path.GetFullPath(run)
            && Path.GetDirectoryName(Path.GetFullPath(run)) == Path.GetFullPath(review.GetProperty("runs_root").GetString()!)
            && System.Text.RegularExpressions.Regex.IsMatch(Path.GetFileName(run), "\\Acontroller-[0-9a-f]{32}\\z")
            && Path.GetFileName(pin.Path) == "controller-handoff.json" && value.GetProperty("controller_session").GetString()!.Length >= 32, "Current allocated Root GUI controller run/session rejected.");
        return new(pin, value);
    }
    static void Require(bool condition, string reason) { if (!condition) throw new InvalidDataException(reason); }
    internal MemorySourcePin Publish(MemoryStartContext context, MemorySourcePin terminalPin, bool cleanupQualified, bool ownershipRetained, bool gameAccepted)
    {
        HandoffPin.Verify(); terminalPin.Verify();
        // Root independently checks its own retained full7 and GUI creation handle. We do not
        // label handoff-supplied parent metadata as an independent live query or native authority.
        return MemoryStartupJson.WriteExclusive(Path.Combine(handoff.GetProperty("run_root").GetString()!, "whole-game-terminal.json"), new
        {
            schema = "LITO_ROOT_OWNED_GUI_GAME_TERMINAL_1", controller_handoff = HandoffPin,
            controller = handoff.GetProperty("controller"), controller_session = handoff.GetProperty("controller_session").GetString(),
            controller_identity_independently_observed = false, launcher = context.LauncherIdentity, launcher_assembly = context.Assembly,
            source_review = handoff.GetProperty("source_review"), run_root = handoff.GetProperty("run_root").GetString(),
            run_id = context.RunId, evidence_pins = new { default_terminal = terminalPin },
            cleanup_qualified = cleanupQualified, ownership_retained = ownershipRetained, game_accepted = gameAccepted,
            runtime_authorized = false, product_accepted = false
        });
    }
}
