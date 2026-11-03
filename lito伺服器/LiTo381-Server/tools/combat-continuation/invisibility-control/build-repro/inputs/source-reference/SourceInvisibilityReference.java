package local.lito381.server; import java.util.Properties; final class SourceInvisibilityReference { static final int INVISIBILITY=60,BLIND_HIDING=97;
private static boolean _getBoolean(final Properties p, final String key, final boolean defaultValue) {
		final String val = p.getProperty(key);
		if (val == null || val.isBlank()) return defaultValue;
		return Boolean.parseBoolean(val.trim());
	}
static boolean shouldToggleOffInvisibilityRecast(final int skillId,
			final boolean hasInvisibility, final boolean hasBlindHiding,
			final boolean invisibilityEquipmentEquipped) {
		if (invisibilityEquipmentEquipped) {
			return false;
		}
		return (skillId == INVISIBILITY && hasInvisibility)
				|| (skillId == BLIND_HIDING && hasBlindHiding);
	}
static boolean enabled(Properties p){return _getBoolean(p,"INVIS_CAST_BREAK",false);}
}