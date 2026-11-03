package com.lineage.server.datatables;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.lineage.DatabaseFactory;
import com.lineage.server.model.L1PolyMorph;
import com.lineage.server.utils.PerformanceTimer;

/**
 * 人物變身資料
 * @author dexc
 *
 */
public class PolyTable {

	private static final Logger _log = LogManager.getLogger(PolyTable.class);

	private static PolyTable _instance;

	private static final Map<String, L1PolyMorph> _polymorphs = new HashMap<String, L1PolyMorph>();

	private static final Map<Integer, L1PolyMorph> _polyIdIndex = new HashMap<Integer, L1PolyMorph>();

	public static PolyTable get() {
		if (_instance == null) {
			_instance = new PolyTable();
		}
		return _instance;
	}

	public void load() {
		final PerformanceTimer timer = new PerformanceTimer();
		_polymorphs.clear();
		_polyIdIndex.clear();
		try (Connection cn = DatabaseFactory.get().getConnection();
				PreparedStatement ps = cn.prepareStatement("SELECT * FROM polymorphs");
				ResultSet rs = ps.executeQuery()) {
			this.fillPolyTable(rs);
		} catch (final SQLException e) {
			_log.error(e.getLocalizedMessage(), e);
		}
		_log.info("載入 polymorphs ok: " + _polymorphs.size() + " (" + timer.get()
				+ "ms)");
	}

	private void fillPolyTable(final ResultSet rs) throws SQLException {
		while (rs.next()) {
			int id = -1;
			try {
				id = rs.getInt("id");
			final String name = rs.getString("name");
			final int polyId = rs.getInt("polyid");
			final int minLevel = rs.getInt("minlevel");
			final int weaponEquipFlg = rs.getInt("weaponequip");
			final int armorEquipFlg = rs.getInt("armorequip");
			final boolean canUseSkill = rs.getBoolean("isSkillUse");
			final int causeFlg = rs.getInt("cause");
			final int arrowSpr = rs.getInt("arrow_spr");

			final L1PolyMorph poly = new L1PolyMorph(id, name, polyId, minLevel,
					weaponEquipFlg, armorEquipFlg, canUseSkill, causeFlg, arrowSpr);

			_polymorphs.put(name, poly);
			_polyIdIndex.put(polyId, poly);
			} catch (final Exception e) {
				_log.error("載入資料失敗，已略過該筆 (id: " + id + ")", e);
				continue;
			}
		}
	}

	public L1PolyMorph getTemplate(final String name) {
		return _polymorphs.get(name);
	}

	public L1PolyMorph getTemplate(final int polyId) {
		return _polyIdIndex.get(polyId);
	}

	public List<L1PolyMorph> findTemplatesByName(final String name) {
		final List<L1PolyMorph> polys = new ArrayList<>();
		if (name == null) {
			return polys;
		}
		final String keyword = name.toLowerCase(Locale.ROOT);
		for (final L1PolyMorph poly : _polyIdIndex.values()) {
			if (poly != null && containsName(poly.getName(), keyword)) {
				polys.add(poly);
			}
		}
		polys.sort((a, b) -> Integer.compare(a.getPolyId(), b.getPolyId()));
		return polys;
	}

	private static boolean containsName(final String value, final String keyword) {
		return value != null && value.toLowerCase(Locale.ROOT).contains(keyword);
	}
}
