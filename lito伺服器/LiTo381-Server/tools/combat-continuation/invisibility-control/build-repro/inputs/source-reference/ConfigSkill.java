package com.lineage.config;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * 職業技能設定（彙整）
 * 對應設定檔: ./config/職業_技能設定表.properties
 */
public final class ConfigSkill {

	private static final Logger _log = LogManager.getLogger(ConfigSkill.class);
	private static final String CONFIG_FILE = "./config/其他控制端/4-職業_技能設定表.properties";

	// ---- 幻術師 ----
	public static int ILLUSION_AVATAR_DAMAGE;
	public static double ILLUSION_AVATAR_DAMAGE_RATE;
	public static int JOY_OF_PAIN_COUNTDMG;
	public static int JOY_OF_PAIN_DMG;
	public static int MIRROR;

	// ---- 王族 ----
	public static double STRIKER_DMG;

	// ---- 騎士 ----
	public static int[] SHOCK_STUN_TIMER;
	public static double COUNTER_BARRIER_DMG;
	public static int SOLID_CARRIAGE_MODE;

	// ---- 法師 ----
	public static int DISINTEGRATE_CRITICAL_RND;
	public static int CRITICAL_SPR;
	public static int CURSE_READY_TIME;
	public static int CURSE_TIME;
	public static double IMMUNE_TO_HARM;
	/** 隱身狀態施法時是否解除隱身 */
	public static boolean INVIS_CAST_BREAK;

	// ---- 妖精 ----
	public static double TRIPLE_ARROW_DMG;
	public static double STRIKER_DMG2;
	public static int BLOODY_SOULADDMP;
	/** 生命的祝福 額外治癒量（與原計算治癒量相加） */
	public static int NATURES_BLESSING_HEAL;
	public static double SOUL_OF_FLAME_DAMAGE;
	public static double ELEMENTAL_FIRE;
	public static int ELEMENTAL_FIRE_RND;

	// ---- 黑暗妖精 ----
	public static int BURNING_CHANCE;
	public static double BURNING_DMG;
	public static int DOUBLE_BREAK_CHANCE;
	public static double DOUBLE_BREAK_DMG;

	// ---- 龍騎士 ----
	public static int FOE_SLAYER_DMG;
	public static int VULNERABILITY_ROM;
	public static int VULNERABILITY_1, VULNERABILITY_2, VULNERABILITY_3;
	/** 奪命之雷束縛時間：基礎秒數 + 0～隨機秒數（格式同 SHOCK_STUN_TIMER） */
	public static int[] SLAY_BREAK_TIMER;
	public static boolean SLAY_BREAK_NOT_TELEPORT;
	public static int MORTAL_BODY_ROM;
	public static int MORTAL_BODY_DMG;

	/** 載入職業技能設定 */
	public static void load() {
		final Properties p = new Properties();
		final File file = new File(CONFIG_FILE);

		try {
			if (file.exists()) {
				try (var reader = new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8)) {
					p.load(reader);
				}
				_loadFromProperties(p);
				_log.info("已載入: 4-職業_技能設定表.properties");
			} else {
				_setDefaults();
				_log.warn("找不到設定檔: " + CONFIG_FILE + "，使用預設值");
			}
		} catch (final Exception e) {
			_setDefaults();
			_log.error("職業技能設定載入異常，使用預設值", e);
		}
	}

	private static void _loadFromProperties(final Properties p) {
		ILLUSION_AVATAR_DAMAGE = _getInt(p, "ILLUSION_AVATAR_DAMAGE", 5);
		ILLUSION_AVATAR_DAMAGE_RATE = _getDouble(p, "ILLUSION_AVATAR_DAMAGE_RATE", 1.5);
		JOY_OF_PAIN_COUNTDMG = _getInt(p, "JOY_OF_PAIN_COUNTDMG", 3);
		JOY_OF_PAIN_DMG = _getInt(p, "JOY_OF_PAIN_DMG", 1500);
		MIRROR = _getInt(p, "MIRROR", 5);

		STRIKER_DMG = _getDouble(p, "STRIKER_DMG", 1.0);

		SHOCK_STUN_TIMER = _parseSecondsBaseRandom(p.getProperty("SHOCK_STUN_TIMER", "2,2"), 2, 2);
		COUNTER_BARRIER_DMG = _getDouble(p, "COUNTER_BARRIER_DMG", 1.5);
		SOLID_CARRIAGE_MODE = _getInt(p, "SOLID_CARRIAGE_MODE", 1);

		DISINTEGRATE_CRITICAL_RND = _getInt(p, "DISINTEGRATE_CRITICAL_RND", 40);
		CRITICAL_SPR = _getInt(p, "CRITICAL_SPR", 11748);
		CURSE_READY_TIME = _getInt(p, "CURSE_READY_TIME", 5);
		CURSE_TIME = _getInt(p, "CURSE_TIME", 10);
		IMMUNE_TO_HARM = _getDouble(p, "IMMUNE_TO_HARM", 2.0);
		INVIS_CAST_BREAK = _getBoolean(p, "INVIS_CAST_BREAK", false);

		TRIPLE_ARROW_DMG = _getDouble(p, "Triple_Arrow_Dmg", 1.0);
		STRIKER_DMG2 = _getDouble(p, "STRIKER_DMG2", 1.2);
		BLOODY_SOULADDMP = _getInt(p, "BLOODY_SOULADDMP", 12);
		SOUL_OF_FLAME_DAMAGE = _getDouble(p, "SOUL_OF_FLAME_DAMAGE", 1.5);
		ELEMENTAL_FIRE = _getDouble(p, "ELEMENTAL_FIRE", 1.5);
		ELEMENTAL_FIRE_RND = _getInt(p, "ELEMENTAL_FIRE_RND", 33);

		BURNING_CHANCE = _getInt(p, "BURNING_CHANCE", 33);
		BURNING_DMG = _getDouble(p, "BURNING_DMG", 1.5);
		DOUBLE_BREAK_CHANCE = _getInt(p, "DOUBLE_BREAK_CHANCE", 15);
		DOUBLE_BREAK_DMG = _getDouble(p, "DOUBLE_BREAK_DMG", 1.5);

		FOE_SLAYER_DMG = _getInt(p, "FOE_SLAYER_DMG", 10);
		VULNERABILITY_ROM = _getInt(p, "VULNERABILITY_ROM", 90);
		VULNERABILITY_1 = _getInt(p, "VULNERABILITY_1", 10);
		VULNERABILITY_2 = _getInt(p, "VULNERABILITY_2", 20);
		VULNERABILITY_3 = _getInt(p, "VULNERABILITY_3", 30);
		SLAY_BREAK_TIMER = _parseSecondsBaseRandom(p.getProperty("SLAY_BREAK_TIMER", "4,0"), 4, 0);
		SLAY_BREAK_NOT_TELEPORT = _getBoolean(p, "SLAY_BREAK_NOT_TELEPORT", true);
		MORTAL_BODY_ROM = _getInt(p, "MORTAL_BODY_ROM", 23);
		MORTAL_BODY_DMG = _getInt(p, "MORTAL_BODY_DMG", 30);
	}

	/**
	 * 解析「基礎秒數,隨機秒數」：實際秒數 = 基礎 + 0～隨機（含端點）
	 * 例：2,2 → 2～4 秒
	 */
	private static int[] _parseSecondsBaseRandom(final String text, final int defaultBase, final int defaultRandom) {
		if (text == null || text.isBlank()) {
			return new int[] { defaultBase, defaultRandom };
		}
		final String[] parts = text.trim().split(",");
		if (parts.length < 2) {
			return new int[] { defaultBase, defaultRandom };
		}
		try {
			final int base = Math.max(0, Integer.parseInt(parts[0].trim()));
			final int randomSec = Math.max(0, Integer.parseInt(parts[1].trim()));
			return new int[] { base, randomSec };
		} catch (final NumberFormatException e) {
			_log.warn("時間格式錯誤（應為 基礎秒數,隨機秒數）: " + text);
			return new int[] { defaultBase, defaultRandom };
		}
	}

	private static int _getInt(final Properties p, final String key, final int defaultValue) {
		final String val = p.getProperty(key);
		if (val == null || val.isBlank()) return defaultValue;
		try {
			return Integer.parseInt(val.trim());
		} catch (final NumberFormatException e) {
			return defaultValue;
		}
	}

	private static double _getDouble(final Properties p, final String key, final double defaultValue) {
		final String val = p.getProperty(key);
		if (val == null || val.isBlank()) return defaultValue;
		try {
			return Double.parseDouble(val.trim());
		} catch (final NumberFormatException e) {
			return defaultValue;
		}
	}

	private static boolean _getBoolean(final Properties p, final String key, final boolean defaultValue) {
		final String val = p.getProperty(key);
		if (val == null || val.isBlank()) return defaultValue;
		return Boolean.parseBoolean(val.trim());
	}

	private static void _setDefaults() {
		ILLUSION_AVATAR_DAMAGE = 5;
		ILLUSION_AVATAR_DAMAGE_RATE = 1.5;
		JOY_OF_PAIN_COUNTDMG = 3;
		JOY_OF_PAIN_DMG = 1500;
		MIRROR = 5;

		STRIKER_DMG = 1.0;

		SHOCK_STUN_TIMER = new int[] { 2, 2 };
		COUNTER_BARRIER_DMG = 1.5;
		SOLID_CARRIAGE_MODE = 1;

		DISINTEGRATE_CRITICAL_RND = 40;
		CRITICAL_SPR = 11748;
		CURSE_READY_TIME = 5;
		CURSE_TIME = 10;
		IMMUNE_TO_HARM = 2.0;
		INVIS_CAST_BREAK = false;

		TRIPLE_ARROW_DMG = 1.0;
		STRIKER_DMG2 = 1.2;
		BLOODY_SOULADDMP = 12;
		NATURES_BLESSING_HEAL = 0;
		ELEMENTAL_FIRE = 1.5;
		ELEMENTAL_FIRE_RND = 33;

		BURNING_CHANCE = 33;
		BURNING_DMG = 1.5;
		DOUBLE_BREAK_CHANCE = 15;
		DOUBLE_BREAK_DMG = 1.5;

		FOE_SLAYER_DMG = 10;
		VULNERABILITY_ROM = 90;
		VULNERABILITY_1 = 10;
		VULNERABILITY_2 = 20;
		VULNERABILITY_3 = 30;
		SLAY_BREAK_TIMER = new int[] { 4, 0 };
		SLAY_BREAK_NOT_TELEPORT = true;
		MORTAL_BODY_ROM = 23;
		MORTAL_BODY_DMG = 30;
	}
}
