package local.lito381.server; import java.util.Properties; final class SourceBindReference {
private static boolean _getBoolean(final Properties p, final String key, final boolean defaultValue) {
		final String val = p.getProperty(key);
		if (val == null || val.isBlank()) return defaultValue;
		return Boolean.parseBoolean(val.trim());
	}
static boolean configured(Properties p){return _getBoolean(p,"SLAY_BREAK_NOT_TELEPORT",true); }
}