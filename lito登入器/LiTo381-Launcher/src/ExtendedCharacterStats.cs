using System.Text.Json;

namespace LiTo381Launcher;

public sealed record ExtendedStatValues(
    int MaxHp,
    int MaxMp,
    int ArmorClass,
    int Level,
    int Strength,
    int Dexterity,
    int Constitution,
    int Wisdom,
    int Charisma,
    int Intelligence);

public sealed record ExtendedRuntimeStatValues(
    int MaxHp,
    int MaxMp,
    int ArmorClass,
    int Level,
    int Strength,
    int Dexterity,
    int Constitution,
    int Wisdom,
    int Charisma,
    int Intelligence,
    int SpellPowerModifier,
    int MagicResistanceModifier,
    int FireResistance,
    int WaterResistance,
    int WindResistance,
    int EarthResistance);

public sealed record ExtendedCharacterStatsSnapshot(
    int SchemaVersion,
    int NumericBreakthroughSchema,
    string Event,
    string Source,
    string Character,
    string Reason,
    ExtendedStatValues Database,
    ExtendedStatValues DirectoryShadow,
    ExtendedRuntimeStatValues? Runtime,
    bool AdjustedDirectory);

public static class ExtendedCharacterStatsProtocol
{
    public const string Marker = "[LiTo-Extended-Stats] ";
    public const int SchemaVersion = 2;

    public static bool TryParse(string line, out ExtendedCharacterStatsSnapshot snapshot)
    {
        snapshot = null!;
        if (string.IsNullOrWhiteSpace(line)
                || !line.StartsWith(Marker, StringComparison.Ordinal))
        {
            return false;
        }
        try
        {
            var parsed = JsonSerializer.Deserialize<ExtendedCharacterStatsSnapshot>(
                line.AsSpan(Marker.Length), LauncherJson.Options);
            if (parsed is null || parsed.SchemaVersion != SchemaVersion
                    || string.IsNullOrWhiteSpace(parsed.Event)
                    || string.IsNullOrWhiteSpace(parsed.Source)
                    || string.IsNullOrWhiteSpace(parsed.Character)
                    || parsed.Reason is null
                    || parsed.NumericBreakthroughSchema is < 0 or > 4
                    || parsed.Database is null || parsed.DirectoryShadow is null
                    || !DirectoryShadowValid(parsed.DirectoryShadow)
                    || (parsed.Event == "RUNTIME") != (parsed.Runtime is not null)
                    || (parsed.Runtime is not null
                        && !RuntimeValid(parsed.Runtime,
                            parsed.NumericBreakthroughSchema)))
            {
                return false;
            }
            snapshot = parsed;
            return true;
        }
        catch (JsonException)
        {
            return false;
        }
    }

    public static bool SelfTest()
    {
        const string sample = Marker
            + "{\"schemaVersion\":2,\"numericBreakthroughSchema\":4,"
            + "\"event\":\"RUNTIME\"," 
            + "\"source\":\"test-source\",\"character\":\"TestKnight\"," 
            + "\"reason\":\"battle-status\"," 
            + "\"database\":{\"maxHp\":100000,\"maxMp\":80000,\"armorClass\":-500,"
            + "\"level\":200,\"strength\":300,\"dexterity\":301,"
            + "\"constitution\":302,\"wisdom\":303,\"charisma\":304,\"intelligence\":305},"
            + "\"directoryShadow\":{\"maxHp\":32767,\"maxMp\":32767,\"armorClass\":-128,"
            + "\"level\":127,\"strength\":127,\"dexterity\":127,"
            + "\"constitution\":127,\"wisdom\":127,\"charisma\":127,\"intelligence\":127},"
            + "\"runtime\":{\"maxHp\":120000,\"maxMp\":90000,\"armorClass\":-650,"
            + "\"level\":200,\"strength\":320,\"dexterity\":321,"
            + "\"constitution\":322,\"wisdom\":323,\"charisma\":324,\"intelligence\":325,"
            + "\"spellPowerModifier\":400,\"magicResistanceModifier\":401,"
            + "\"fireResistance\":402,\"waterResistance\":403,"
            + "\"windResistance\":404,\"earthResistance\":405},"
            + "\"adjustedDirectory\":true}";
        var downgradedSchema = sample.Replace(
            "\"numericBreakthroughSchema\":4",
            "\"numericBreakthroughSchema\":3",
            StringComparison.Ordinal);
        return TryParse(sample, out var parsed)
            && parsed.Database.MaxHp == 100_000
            && parsed.DirectoryShadow.MaxHp == 32_767
            && parsed.Runtime?.MaxHp == 120_000
            && parsed.Runtime.EarthResistance == 405
            && parsed.AdjustedDirectory
            // Schema 3 still has signed-byte elemental fields. A launcher must
            // reject a server claim that would otherwise feed wide values into
            // an unpatched LiTo consumer.
            && !TryParse(downgradedSchema, out _);
    }

    private static bool DirectoryShadowValid(ExtendedStatValues value) =>
        value.MaxHp is >= 1 and <= short.MaxValue
        && value.MaxMp is >= 1 and <= short.MaxValue
        && value.ArmorClass is >= sbyte.MinValue and <= sbyte.MaxValue
        && value.Level is >= 1 and <= sbyte.MaxValue
        && DirectoryAbilityValid(value.Strength)
        && DirectoryAbilityValid(value.Dexterity)
        && DirectoryAbilityValid(value.Constitution)
        && DirectoryAbilityValid(value.Wisdom)
        && DirectoryAbilityValid(value.Charisma)
        && DirectoryAbilityValid(value.Intelligence);

    private static bool RuntimeValid(ExtendedRuntimeStatValues value, int schema)
    {
        var vitalMaximum = schema >= 1 ? 999_999 : short.MaxValue;
        var armorMinimum = schema >= 1 ? -999_999 : sbyte.MinValue;
        var armorMaximum = schema >= 1 ? 999_999 : sbyte.MaxValue;
        var levelMaximum = schema >= 2 ? 999_999 : byte.MaxValue;
        var abilityMaximum = schema >= 2 ? 999_999 : 0xFE;
        var magicMinimum = schema >= 3 ? -999_999 : sbyte.MinValue;
        var magicMaximum = schema >= 3 ? 999_999 : sbyte.MaxValue;
        var elementMinimum = schema >= 4 ? -999_999 : sbyte.MinValue;
        var elementMaximum = schema >= 4 ? 999_999 : sbyte.MaxValue;
        return value.MaxHp is >= 1 && value.MaxHp <= vitalMaximum
            && value.MaxMp is >= 1 && value.MaxMp <= vitalMaximum
            && value.ArmorClass >= armorMinimum && value.ArmorClass <= armorMaximum
            && value.Level is >= 1 && value.Level <= levelMaximum
            && RuntimeAbilityValid(value.Strength, abilityMaximum)
            && RuntimeAbilityValid(value.Dexterity, abilityMaximum)
            && RuntimeAbilityValid(value.Constitution, abilityMaximum)
            && RuntimeAbilityValid(value.Wisdom, abilityMaximum)
            && RuntimeAbilityValid(value.Charisma, abilityMaximum)
            && RuntimeAbilityValid(value.Intelligence, abilityMaximum)
            && InRange(value.SpellPowerModifier, magicMinimum, magicMaximum)
            && InRange(value.MagicResistanceModifier, magicMinimum, magicMaximum)
            && InRange(value.FireResistance, elementMinimum, elementMaximum)
            && InRange(value.WaterResistance, elementMinimum, elementMaximum)
            && InRange(value.WindResistance, elementMinimum, elementMaximum)
            && InRange(value.EarthResistance, elementMinimum, elementMaximum);
    }

    private static bool DirectoryAbilityValid(int value) =>
        value is >= 0 and <= sbyte.MaxValue;

    private static bool RuntimeAbilityValid(int value, int maximum) =>
        value >= 1 && value <= maximum;

    private static bool InRange(int value, int minimum, int maximum) =>
        value >= minimum && value <= maximum;
}
