package local.lito381.server; import java.util.Properties; final class SourceSolidReference {
private static int _getInt(final Properties p, final String key, final int defaultValue) {
		final String val = p.getProperty(key);
		if (val == null || val.isBlank()) return defaultValue;
		try {
			return Integer.parseInt(val.trim());
		} catch (final NumberFormatException e) {
			return defaultValue;
		}
	}

	
 static int mode(Properties p){return _getInt(p,"SOLID_CARRIAGE_MODE",1);}
 static boolean eligible(boolean hasShield,boolean hasArm,int mode){return hasShield || (mode == 1 && hasArm);}
}