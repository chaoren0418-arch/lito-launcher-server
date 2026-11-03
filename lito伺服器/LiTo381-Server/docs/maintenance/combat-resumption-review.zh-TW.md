# 戰鬥技能同源化接續前核對

日期：2026-10-18。本次僅核對既有檔案、測試收據與對話中止原因；沒有執行候選程式、重跑行為測試、修改正式程式或資料庫。

## 已核對成果

- `tools/resource-database/build/native-item-weapon-material-progress-current.json` 中所有附路徑及 SHA-256 的直接引用，加上正式 JAR 與發布設定比對，共 55 筆全部相符。這是檔案完整性核對，不代表重新通過行為測試。
- 正式 JAR 仍為登入器 `app/server/LiTo381-SharedResources-candidate.jar`，SHA-256：`c02aaae8d54a27e30460be2e3c47709ee64d35f007a8be27ac74a327268b9539`，與 `deployment/release-inputs.json` 一致；正式版本仍為 0.6.68。
- 上一個完整階段候選 SHA-256：`df9464f06c41821f3abf7c29ccc5876abe1ead66e570a40308f1274e90521a37`。收據明列 `full_phase_qualified=false`、`full_goal_achieved=false`、`formal_deployment_changed=false`。
- `config/combat.properties` 的技能、職業傷害、角色設定與地圖四個來源路徑均存在。僅驗證路徑，不代表正式版已接入或控制效果全部通過。

## 避免重複工作

既有完整階段詳見 [武器附魔材料與動作提交施工紀錄](native-item-weapon-material-progress.zh-TW.md)：23 個材料技能已接入相應流程，仍有 35 個材料技能及跨職業、目標、Ctrl、範圍與效果生命週期待完成。既有驗證結果應依候選 SHA 與受影響範圍使用，不能把入口接入當成技能全部語義完成。

技能 73 的後續成果位於 `tools/resource-database/build/de-native-magical-weapon-material-development/`。目前有 `build-current.json`，以及 cast、fault、gm、stopped、edges、gm-fault 六組通過收據，均聲明候選 SHA 為 `ff647f9eee261876e138ebb8b4d0817f5b06e7bcad37663ab7da6de59525b633`。本次讀取其狀態與候選識別，未重新驗證這六組收據引用的全部證據。不得將此原型計為整體驗收完成，也不應忽略已存在的原始碼和測試而重寫。

## 中止與發布限制

原對話 `01a13b3b-bca9-73c2-8c87-0874f2d5c92a` 最後一輪 `01a14dbd-6119-7e12-baed-21836084e559` 為 failed，對話狀態為 systemError。平台訊息為「This content was flagged for possible cybersecurity risk」，沒有提供更細的被拒操作範圍。這不是建置失敗，也不是產品完成。

本次不透過新對話或其他工具重試被中止的技能 73 整合流程。此記錄只保存已完成成果及缺口，不代表平台限制已解除。

正式發布仍須具備完整行為驗證、所有必要差分與生命週期測試、原生整合驗收及一致的發布雜湊；不能用檔案存在、部分測試通過或 55 筆雜湊相符取代。尚未具備條件，本次維持正式版不變。

## 已開發成果的更新檢查

使用者隨後要求先更新已開發的資料庫與程式。本次檢查 `db/` 檔案、施工總紀錄與既有整合收據：

- `combat-acceptance-current.json` 的 passed 對應較早的 `efd4bc36…` 候選，不能移作最新 `df9464f0…` 或技能 73 原型的整體發布證據。
- `native-integrated-current.json` 明列仍須驗證完整 SharedResourceMain／共用 SQL 啟動與退役、啟動器正常及強制關閉，以及必要的原生 UI，並要求通過後才更新正式候選與 pins。
- 未在本次檢查的正式 `db/` 遷移檔及戰鬥施工紀錄中找到本輪技能同源化專用、已核准發布的新增資料庫遷移。既有 `db/candidates/resource-enrichment-0.6.60/` 是另一批資源提取候選，不能當成本輪新技能資料直接重新匯入。
- 本輪候選採共用技能資料及來源外控，新增程式讀取能力不等於需要改寫共用資料表。本次未連線更改或重新匯入 SQL，未替換正式 JAR。

平台錯誤的範圍不能擴大推論為整個遊戲專案被禁止。官方文件承認與資安無關的活動也可能觸發防護，疑似誤判可透過 `/feedback` 提交原場次供檢視。本次無法確定具體觸發原因，也未取得解除原中止流程限制的通知；既有成果發布資格與平台錯誤分開判斷。

官方說明：<https://learn.chatgpt.com/docs/cyber-safety>、<https://learn.chatgpt.com/docs/reference/troubleshooting>。
