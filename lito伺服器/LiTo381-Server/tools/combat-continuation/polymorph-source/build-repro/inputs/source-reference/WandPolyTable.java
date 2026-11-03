package com.lineage.server.datatables;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.lineage.DatabaseFactory;
import com.lineage.server.utils.PerformanceTimer;

/**
 * 變身魔杖 - 變身清單與機率控制 (固定權重 1,000,000)
 */
public class WandPolyTable {

	private static final Logger _log = LogManager.getLogger(WandPolyTable.class);

	private static WandPolyTable _instance;

	// 固定分母：1,000,000
	private static final int TOTAL_PROBABILITY = 1000000;

	private class PolyDrop {
		int polyId;
		int chance;
		public PolyDrop(int id, int c) {
			polyId = id;
			chance = c;
		}
	}

	private final List<PolyDrop> _polys = new ArrayList<PolyDrop>();
	private final Random _random = new Random();

	// 檢查載入的總權重是否正確，方便除錯
	private int _loadedTotalChance = 0;

	public static WandPolyTable get() {
		if (_instance == null) {
			_instance = new WandPolyTable();
		}
		return _instance;
	}

	public void load() {
		final PerformanceTimer timer = new PerformanceTimer();
		_polys.clear();
		_loadedTotalChance = 0;

		try (Connection con = DatabaseFactory.get().getConnection();
				PreparedStatement pstm = con.prepareStatement("SELECT * FROM `道具_變形控制魔杖` WHERE `chance` > 0 ORDER BY `chance` ASC");
				ResultSet rs = pstm.executeQuery()) {

			while (rs.next()) {
				int id = -1;
				try {
					id = rs.getInt("poly_id");
					int polyId = id;
					int chance = rs.getInt("chance");
				
				_polys.add(new PolyDrop(polyId, chance));
				_loadedTotalChance += chance;
				} catch (final Exception e) {
					_log.error("載入資料失敗，已略過該筆 (主鍵: " + id + ")", e);
					continue;
				}
			}
			
		} catch (final SQLException e) {
			_log.error("載入 道具_變形控制魔杖 失敗! 請檢查資料表!", e);
		}
		
		_log.info("載入 道具_變形控制魔杖 ok: " + _polys.size() + " (" + timer.get() + "ms)");
		
		if (_loadedTotalChance != TOTAL_PROBABILITY) {
			_log.warn("注意：變身魔杖資料庫機率總和不等於 1,000,000 (目前:" + _loadedTotalChance + ")");
			_log.warn("若總和 < 100萬，未命中部分將回傳預設變身(骷髏)");
		}
	}

	/**
	 * 取得隨機變身 ID (基於 100萬 權重)
	 * @return polyId
	 */
	public int getRandomPolyId() {
		if (_polys.isEmpty()) {
			return 29; // 資料庫沒資料，回傳骷髏
		}

		// 取 1 ~ 1,000,000 的隨機數
		int randomPoint = _random.nextInt(TOTAL_PROBABILITY) + 1;
		int currentSum = 0;

		for (PolyDrop poly : _polys) {
			currentSum += poly.chance;
			// 命中區間
			if (randomPoint <= currentSum) {
				return poly.polyId;
			}
		}
		
		// 如果機率總和沒設定滿 100萬，或者隨機數落在了空缺區間
		// 預設回傳 骷髏 (29)
		return 29;
	}
}
