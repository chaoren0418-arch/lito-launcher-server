package local.lito381.server; import java.util.Properties; final class SourceTripleReference {
private static double _getDouble(final Properties p, final String key, final double defaultValue) {
		final String val = p.getProperty(key);
		if (val == null || val.isBlank()) return defaultValue;
		try {
			return Double.parseDouble(val.trim());
		} catch (final NumberFormatException e) {
			return defaultValue;
		}
	}
static double configured(Properties p){return _getDouble(p,"Triple_Arrow_Dmg",1.0);}
static double[] multiplier(double _damageMultiplier,int remaining,int _weaponType2,double configured){
if (remaining > 0 && (_weaponType2 == 4 || _weaponType2 == 13)) {
			_damageMultiplier = Math.max(_damageMultiplier, configured);
			remaining--;
		}

return new double[]{_damageMultiplier,remaining};}
}