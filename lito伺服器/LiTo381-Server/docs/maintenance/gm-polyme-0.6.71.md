# 0.6.71：L1PolyMe 自身變身

在遊戲世界輸入 `.polyme 外型代號`，例如 `.polyme 6080`。沿用來源 `L1PolyMe.java` 的 GM cause=2、7200 秒；重複輸入重設剩餘時間，不消耗道具／HP／MP。`L1PolyMe` 是資料表執行器名稱，聊天指令名稱是 `.polyme`。

定義：`380_lito.commands` 的 `name=polyme`、`class_name=L1PolyMe`、`access_level=200`、`system=0`。只以目前來源 `accounts.access_level` 判斷權限；SQL 失敗、缺列、處理器不符均拒絕。安裝 SQL 為 `db/lito_gm_polyme.sql`，重跑保留既有設定。本次已新增該列；未更動來源庫。

外型規則由共用靜態庫 `polymorphs` 唯讀取得，沿用 GM cause 位元及裝備／施法限制；GM 不受 minlevel 限制。不存在的表列仍須通過原生動畫驗證；不支援的資料、缺動畫及超出 1～65535 的代號會顯示拒絕提示。這不是任意 spr 播放器，也不新增 Source 外掛的變身額外能力。

只在遊戲世界生效；大廳／等待房、死亡、觀戰、換景中及來源禁止變身地圖拒絕。使用現有角色動作鎖與生命週期檢查，防止 SQL 讀取期間換角後套到新角色。沿用既有武器卸除與外型廣播、到期還原；離房轉入結算／大廳時清除變身，不回存角色外型。

建置：`python -B -X utf8 tools/gm-polyme/build.py`，固定輸入為 `tools/gm-polyme/inputs/base-0.6.70.jar`。只改 GM 分派、離房清理及新增 GmPolyMe；保留 0.6.70 其餘技能與職業圖示。

驗證：`tools/gm-polyme/test.py` 的 34 項檢查通過，包括真實聊天入口拒絕、來源 SQL 讀取、權限／格式／角色變更拒絕、雙玩家加密外型封包、7200 秒及重施、到期還原、結算確認與清理、不消耗及不存檔；既有 `.loc`／`.move`／Storm_Walk 回歸亦通過。遊戲內變身外觀仍待使用者實測。發布與啟動結果見相鄰登入器 `docs/maintenance/release-0.6.71.json`。
