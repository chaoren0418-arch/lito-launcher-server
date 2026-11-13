# Issue #356 — T125：技能時序與射程收尾（G09–G12、G16）

分支 `task/356`。範圍：`customer-gaps.md` Backlog 的 G09、G10、G11、G12、G16（含 G02 的伺服器端部分）。

## 結論一覽

| 項目 | `src/main` 現況 | 本 PR 動作 | 測試 |
|---|---|---|---|
| G09 29／44／76 ranged=−1 視野射程 | 候選 `NativeSourceVisibilityRange` 已含（與 `tools/combat-continuation/integration-lifecycle/NativeSourceVisibilityRange.java` 內容相同），ClientSession 13 個呼叫點（29×1、44×6、76×6） | **修正**：對照 1-伺服器 `src/com/lineage/server/types/Point.java:157-177`（`isInScreen`，由 `model/skill/L1SkillUse.java:1712` 對 ranged=−1 使用），補上第 19 格的反向邊界。原本只用 LiTo 正向外框 `BattleSpellCatalog.isInNativeScreen`，在 (19,0)／(0,19)／(18,−1)／(−1,18) 四格會拒絕 Source 允許的施法。76 的 NPC 選取（`NativeSourceSlowFailureMetadata.distance`）同步改用同一判定 | `g09CatalogRangeProjectsSourceScreen`、`g09SourceScreenIsSymmetricUnionOfNativeBounds`、`g09BoundaryDistances`、`g09CallSitesUseSourceVisibility` |
| G10 87 暈眩先於扣 HP | 候選 `NativeAcceptedShockMutation` 已含，BATTLE／WORLD 兩條 87 路徑都呼叫 `plan(...)`；`NativeAcceptedHpCompletion.run` 先 `acceptedTail` 再 `NativeAcceptedFreezeMutation.completed`→`NativeAcceptedShockMutation.completed` | 不改碼 | `g10ShockStunWaitsForAcceptedHp` |
| G11 50 冰凍時序 | 候選 `NativeAcceptedFreezeMutation` 已含（`src/main` 版本是 G11 收據時的版本；`tools/` 版本已延伸到 194） | 不改碼 | `g11IceLanceAcceptedFreeze` |
| G12 80 範圍冰凍時序 | 候選 `NativeAcceptedAreaFreezeMutation` 已含，ClientSession 的 BATTLE／WORLD 中心範圍冰凍都走它 | 不改碼 | `g12FreezingBlizzardAreaOrdering` |
| G16 7 技能 Ctrl 政策 | 候選 `NativeSelectedPlayerIntent` 已含（與 tools 版逐字相同），33 個 object＋48 個 attack 解碼端點＝收據的 81 個 | 不改碼 | `g16SelectedPlayerIntentPolicy` |

測試檔：`lito伺服器/LiTo381-Server/src/test/java/local/lito381/server/SkillTimingRangeGapsTest.java`。

## 做了什麼

1. `NativeSourceVisibilityRange.java`：新增 `inSourceScreen(dx,dy)`（1-伺服器 `Point.isInScreen` 的逐行移植），`distance`／`signedDistance` 改用它。該判定對 (dx,dy) 取負號不變，所以 `signedDistance` 的「施法者−目標」有號差值不用翻轉。只用於技能射程；畫面廣播仍走 `isInNativeScreen`。
2. `NativeSourceSlowFailureMetadata.distance`：76 NPC 主目標的 ranged=−1 判定改用 `inSourceScreen`，跟 PC 路徑一致。
3. 新增 `SkillTimingRangeGapsTest`（7 個測試）：
   - G09 是純函式，測實際行為：Source 列載入／未載入的射程投影、第 19 格邊界逐格比對（−25..25 全格）、座標版與有號差值版的邊界距離、呼叫點未繞過。
   - G10–G12：接受後生命週期需要真 `GameHub`＋socket（CI 沒有 `config/server.properties`，現有整合測試會 assume 跳過），所以測**不需連線的退化路徑**（沒有事件範圍時不建立 plan、`duration` 讀目錄秒數、`players` 不改成員、80 不走單體 batch），並用 `src/main` 原始碼鎖住守門條件與接線順序（HP 幀 accepted／非致死／HP>0 才發狀態；消魔與冰凍延後；佇列拒收即 abandon；動畫輸出 `emit(g)` 先於 `publish` HP；OPEN／未移動才施放）。
   - G16：7 技能（1／4／26／42／50／76／167，取自候選 `SelectedPlayerIntentProbe.java:23`）的 target_to 都觸發閘門；metadata 未載入時放行；政策本體與 33＋48 個呼叫點數鎖定。
4. `docs/reports/customer-gaps.md` 更新 G09–G12、G16 五列的「建議」與「位置」欄。

## 怎麼驗證

- 本機只做 `mvn -B -q -o test-compile`（編譯通過，exit 0）。依本機執行限制，**未在本機跑測試**；`mvn -B test` 交給 PR 的 GitHub Actions CI。
- G09 邊界的 4 格差異先用 PowerShell 純算式逐格算過（不是執行編譯產物），再寫成測試期望值。
- fixture 列（`ROWS`）逐欄取自 `src/test/resources/server1/skills-targeting-181.tsv`。

## 已驗證／推測／待驗證

**已驗證**
- 5 項的候選修正類別都在 `src/main`，且已接上 ClientSession／NativePlayerCommit／NativeAreaMaterialAction（grep 呼叫點，數量如上表）。
- `NativeSelectedPlayerIntent.java`、`NativeSourceVisibilityRange.java`（修改前）與 `tools/` 候選逐字相同；三個 accepted mutation 與 tools 版只差後續延伸（194、`throws IOException`、indirect proof）。
- 1-伺服器 `Point.isInScreen` 有反向邊界、LiTo `isInNativeScreen` 沒有；兩者在曼哈頓距離 19 只差 4 格（算式驗證）。
- 伺服器端 `NativeSelectedPlayerIntent.allowed` 對 26 自施（`target==pc`）與友方（`!canAttack`）都放行，不需要 Ctrl。

**推測**
- G09 修正對玩家的可見影響很小：LiTo 廣播若同樣只用正向外框，客戶端可能本來就看不到那 4 格上的目標，也就點不到。修正後的行為跟 1-伺服器一致，但這 4 格在 LiTo 客戶端實際能不能選取目標，沒有現場證據。
- G02（26 要按 Ctrl 才放得出來）原因在客戶端游標／送包（例如沒按 Ctrl 時，送出的 target object ID 不是自己／友方，或根本沒送出）；伺服器端判斷沒有找到需要 Ctrl 的條件。
- 原始碼字串鎖只證明「守門條件與接線還在」，不等於證明競態下的次數（候選收據 33→0、16／13／8→0、4／4→0）。

**待驗證**
- `mvn -B test` CI 結果（看 PR）。
- G10–G12 在真連線／鎖等待下的時序：需整合測試（有 `config/server.properties` 的環境）或實機，交 Codex。
- G02／G16：現場抓 26 未按 Ctrl 自施／友方施放時的 C_USE_SPELL 封包（target object ID），才能判斷是不是客戶端送包。
- G09 邊界 4 格：實機在 (19,0) 等位置對 29／44／76 施放，確認客戶端能選取，而且伺服器現在會接受。
- CI 的 semantic-gap（non-blocking）會多出 `NativeSourceVisibilityRange`／`NativeSourceSlowFailureMetadata` 兩個與 0.6.71 不同的類別，這是預期中的差異。

## 產出路徑

- `lito伺服器/LiTo381-Server/src/main/java/local/lito381/server/NativeSourceVisibilityRange.java`
- `lito伺服器/LiTo381-Server/src/main/java/local/lito381/server/NativeSourceSlowFailureMetadata.java`
- `lito伺服器/LiTo381-Server/src/test/java/local/lito381/server/SkillTimingRangeGapsTest.java`
- `docs/reports/customer-gaps.md`（G09–G12、G16 五列）
- `docs/reports/issue-356.md`（本檔）

## 給下一棒的提示

- G10–G12 要做「行為級」測試，可以參考 `ClientSessionCubeShockIntegrationTest` 的 GameHub＋socket 架法，再配合候選 `tools/combat-continuation/integration-lifecycle/ShockAccepted*Probe.java`、`AreaFreezeAccepted*Probe.java` 的情境改寫成 JUnit；這類測試在 CI 會被 `LocalResources.assumeServerProperties()` 跳過，要在本機有設定檔的環境跑。
- `tools/` 的 `NativeAcceptedFreezeMutation` 已延伸到 194（`NativeSourceFreezeTarget.applies`），`src/main` 的 `applies` 也已含 194；兩邊其餘的差異（indirect proof 等）屬其他任務，不在 G11 範圍。
- G02 下一步是 M1 現場封包（登入器 `native/forced_combat.inc` 0x4C6B27 送出點）；伺服器端不需要再改。
- 若以後改 `ClientSession` 的選取玩家端點，`g16SelectedPlayerIntentPolicy` 的 33／48 計數要一起更新。
