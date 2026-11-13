# Issue #288：T070 M2 客戶缺口盤點（第一版）

## 結論

已產出 [`customer-gaps.md`](customer-gaps.md) 第一版：共 28 項，其中 **M2 主線候選 8 項**（G01–G08，全部有證據、建議「修」），**Backlog 候選 20 項**（G09–G28，每項附一行理由）。來源 (a)（M1 回歸）尚未併入，文件開頭已註明要等 #285 完成後補。

主線 8 項：
- G01（b）：營運用逐表授權的受限帳號部署時，正式 JAR 讀不到 `角色_智力成長`，啟動就失敗（#284 實際堆疊）；只要補授權清單、不改程式，可能歸到 M1 的 U1-2。
- G02（b）：技能 26 通暢氣脈術自施／友方要按 Ctrl（使用者回報、至今未修）。
- G03–G08（c）：167、194 單體技能誤中旁人；76 自身主目標扣費／顯示／範圍錯誤；29／152／153 准入與狀態差異；17／148／149 範圍與目標差異；40／103 遠距誤生效與漏找。都有各自 SHA 的同 SHA 收據。

## 做了什麼

- 讀了 `CLAUDE.md`、`PLAN.md`、`TASKS.md` T070、`docs/acceptance/CHECKLIST.md`、伺服器 `docs/HISTORY.md`、`VALIDATION.md`、`README.zh-TW.md`、`docs/maintenance/` 的 combat-continuation／closeout／source-negative-final-frame／buff-schema／gm-polyme／gm-travel／forced-combat，登入器 `release-0.6.71.json`、`world-resource-compat-current.zh-TW.md`，以及 #30、#83、#231、#284 報告。
- 關鍵判讀：#30 的類別比對顯示正式 0.6.71 的核心 ≈ 候選 `8d26c9d7…`（只差 4 個類別），所以來源 (c) 只看 `8d26` 之後的修正；使用者確認產品只有競技場，所以 WORLD 專屬修正全部進 Backlog。
- 唯讀查看原始專案的本機收據 `source-area-pair-first-*-current.json` 與其驗證 manifest，確認 `3ccbb6f5…` 的內容（受控 NPC 76／167 主次目標競態，狀態 `integration-open`）。

## 怎麼驗證

- 用 `scratch/issue-288/check_links.py` 檢查 `customer-gaps.md` 的 62 個證據連結：路徑都存在，`#L` 行號都沒超過檔案長度（`links 62 bad 0`）。
- 引用的數字逐一從引用行抄出；本機收據（不在 git 內）一律標「本機收據」並另附在 git 內的文件行號。

## 沒有驗證的部分

- 沒有啟動任何服務、沒有連 DB、沒有開遊戲，也沒有改程式、`PLAN.md`、`TASKS.md`、`docs/REPORT.md`。
- G03–G08「正式版也有同一缺陷」屬**推測**，還沒在正式 JAR 重現。
- 沒讀候選差異碼，「最小修改位置」只是依文件推估。
- 沒有宣稱缺資源，也沒有執行 PakViewer；G27（map 12000）只引用 #83 的舊觀察，標為待驗證。

## 產出路徑

- `docs/reports/customer-gaps.md`
- `docs/reports/issue-288.md`
- `scratch/issue-288/check_links.py`（不進 git）

## 給下一棒的提示

- **Claude（雲端）**：審查主線排序。G01 可能直接歸 M1 U1-2，它也是 #284／#285 的前置阻塞（補正式 JAR 實際讀表清單＋實驗庫授權）。
- **#285 完成後**：把 CHECKLIST「失敗／異常記錄」逐列併入 `customer-gaps.md` 當來源 (a)，重新排序後封頂 M2（PLAN U2-1：第 9 項起交使用者決定）。
- **G03–G08 拆任務時**：第一步一律是「在正式 JAR 重現＋修前截圖」，重現不到就降 Backlog；移植時別讓候選的舊 AC 限制覆蓋正式版的 NativeArmorStatus（#30:56）。
- **G02**：需要 M1 現場封包或游標證據才能定位，修改點優先看登入器 `native/forced_combat.inc`。
