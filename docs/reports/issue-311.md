# Issue #311 — T110：魔法娃娃圖示不再送 0xFA（S_LITO_CLAN 衝突）

階段：code｜分支：`task/311`｜日期：2026-11-11

## 結論
- `ClientSession.sendNativeMagicDollIcon` 改成**不送任何封包**，只寫一行 `[LiTo] magic-doll-icon suppressed …` log，做法比照 Counter Magic／Absolute Barrier／藍水／料理圖示。
- 伺服器內部的娃娃狀態**完全保留**：`activeMagicDoll`、`activeMagicDollObjectIds`、計時（`remainingSeconds`／`serverMagicDollTick` 到期收回）、能力（powers／自動技能／水下呼吸）都不受影響。召喚時的 S_PUT_OBJECT 與 S_EFFECT 5935，以及收回時的 S_EFFECT 5936 照常廣播。
- **更正 R1 的前提**：目前 `src/main`（#138 B 階段併入、T029 保留正式 0.6.71 的寫法）的 `Protocol.magicDollIcon`／`magicDollCustomIcon` 寫的是 opcode `123`（**0x7B = S_EVENT**），**不是 0xFA**（`Protocol.java:15041`、`:15050`）。`client-protocol-needs.md` §5 R1 是依常數名稱 `S_PACKET_BOX` 推斷成 0xFA。不過 0x7B 的接收端 `sub_4B07C0` 只處理 0..40（`Protocol.java:2974` 附近 `ServerEventType` 的註解），子型別 56（0x38）與 154（0x9A）都不在範圍內，所以這仍然是沒有原生接收端的猜測封包。依任務要求改成靜默。

## 改動
| 檔案 | 內容 |
|---|---|
| `lito伺服器/LiTo381-Server/src/main/java/local/lito381/server/ClientSession.java` `sendNativeMagicDollIcon`（約 :20214） | 拿掉 `sendEncrypted(magicDollIcon／magicDollCustomIcon)`，改成只印 log；拿掉 `throws IOException`（呼叫端 :19918 召喚、:20178 收回不變） |
| `lito伺服器/LiTo381-Server/src/test/java/local/lito381/server/ClientSessionMagicDollIconSuppressionTest.java`（新增） | 參數化（iconId 0＝舊式 DOLL 56、iconId 2184＝自訂 154）：召喚 → 收回 → 再召喚 → 計時到期，逐一解析 `CaptureLog` 的 server_to_client 幀；斷言**沒有任何 0xFA 幀**、沒有 `7B 38…`／`7B 9A…` 幀；同時斷言 S_PUT_OBJECT 仍會送出、`activeMagicDoll` 與 `activeMagicDollObjectIds` 保留、到期後狀態清除、材料扣款正確。不依賴 `config/server.properties`（用 `src/test/resources/.../server.character-selftest.properties` 與記憶體 GameHub），CI 會實際執行。每階段的 opcode 序列會印在 stdout（`[#311] icon=… summon opcodes=[…]`）。 |
| `docs/reference/client-protocol-needs.md` §4 表格 0xFA 列、§5 R1 | 補上「實際寫成 0x7B、#311 已改為不送」 |
| `docs/reports/server-capability-matrix.md` 道具列 | R1 狀態更新 |

`Protocol.magicDollIcon`／`magicDollCustomIcon` 本身保留（`NativeMagicDollTest` 仍釘住位元組格式，日後找到原生接收端時可以直接用），只是 production 不再呼叫。

## 改動前後的封包序列（召喚，classic WORLD，自己）
依程式碼路徑整理（`useNativeMagicDoll` :19915–19920）：

| 順序 | 改動前 | 改動後 |
|---|---|---|
| 1 | 背包差異（S_INVENTORY 系列：扣材料、娃娃狀態） | 相同 |
| 2 | `S_PUT_OBJECT` 娃娃 actor（範圍內所有觀看者） | 相同 |
| 3 | `S_EFFECT` gfx 5935（召喚特效） | 相同 |
| 4 | （有 STATUS_UNDERWATER 能力時）`S_BREATH` | 相同 |
| 5 | **`7B 38 <秒 h> 00 00`**（iconId=0）或 **`7B 9A <秒 h> <icon h> 00 00 00 00`**（iconId>0） | **不送**（只寫 log） |
| 6 | 自動技能維護、`sendEquipmentStatusRefresh`（狀態刷新） | 相同 |

收回（`recallNativeMagicDollLocked`，notifyOwner=true）：改動前會多送 `7B 38 00 00 00 00` 或 `7B 9A 00 00 <icon> 00 00 01 00`（remove=1）；改動後不送，S_EFFECT 5936 與移除 actor 照常。

範例（iconId=0、剩 300 秒）：改動前第 5 步明文 `7B 38 2C 01 00 00`（與 `NativeMagicDollTest` 釘住的位元組相同）。

## 已驗證
- **CI（PR #324，run 42933732915）**：`ClientSessionMagicDollIconSuppressionTest` 實際執行 2 案（Tests run: 2, Failures: 0, Errors: 0, Skipped: 0）；全體 `Tests run: 6896, Failures: 0, Errors: 0, Skipped: 117`。Server／Launcher／Package／Semantic gap 全綠。
- CI 實測的改動後 server_to_client opcode 序列（iconId 0 與 2184 兩案相同）：
  - 召喚：`[1A, 18, 0C, 0B, 37, 0D, 0C, 4D, 50, 52, 56]`：沒有 `FA`，也沒有 `7B`。
  - 收回：`[37, 15, 0D, 0C, 4D, 50, 52, 56]`：沒有 `FA`，也沒有 `7B`。
  - 改動前，依程式碼路徑推算，召喚序列在狀態刷新之前會多一個 `7B` 幀，收回序列也會多一個 `7B` 幀（見上表）。改動前的序列沒有在 CI 實測。
- `mvn -B -o -DskipTests test-compile`（JDK 17.0.17、Maven 3.9.16）本機編譯通過：main 與新測試都能編譯。
- 程式碼靜態確認：`sendNativeMagicDollIcon` 是 production 唯一呼叫 `magicDollIcon`／`magicDollCustomIcon` 的地方（grep `src/main`），改動後 production 已沒有任何呼叫。
- 現行 opcode 是 0x7B 而不是 0xFA：`Protocol.java:15041`、`:15050`，以及 `NativeMagicDollTest.java:27-29` 的註解「T029 keeps the formal 0.6.71 icon packet (opcode 123…)」。

## 推測
- 3.81C 客戶端收到 `7B 38…`／`7B 9A…` 時，`sub_4B07C0` 應該是落到 switch 的 default 分支直接忽略（依 0..40 範圍推斷，沒有重新反組譯確認 default 的行為）。所以改動前在畫面上很可能只是「沒有圖示」，改動後在玩家看來應該沒有差別；這次改動主要是去掉一個沒有根據的封包。
- 原 R1 描述的「血盟資訊錯亂」在現行程式碼上不會發生（因為送的不是 0xFA）；只有 #138 之前的舊程式碼才可能發生。

## 待驗證
- **實機（交給 Codex／使用者）**：在 WORLD 或戰鬥房召喚魔法娃娃，確認 (1) 娃娃 actor 與召喚特效正常顯示；(2) 血盟 UI 沒有變化；(3) 計時到期或再次使用時娃娃會被收回；(4) 伺服器 log 出現 `magic-doll-icon suppressed`。
- semantic-diff（non-blocking）會多出一個 `sendNativeMagicDollIcon` 與正式 0.6.71 的差異，這是刻意的。

## 未做
- 沒有找替代的原生圖示載體（3.81C 是否有娃娃計時圖示的接收端，需要另外逆向 `LiTo.bin`）。
- 沒有刪除 `Protocol.magicDollIcon`／`magicDollCustomIcon`。

## 產出路徑
- `docs/reports/issue-311.md`（本檔）
- `lito伺服器/LiTo381-Server/src/main/java/local/lito381/server/ClientSession.java`
- `lito伺服器/LiTo381-Server/src/test/java/local/lito381/server/ClientSessionMagicDollIconSuppressionTest.java`
- `docs/reference/client-protocol-needs.md`、`docs/reports/server-capability-matrix.md`

## 給下一棒的提示
- 如果 CI 上新測試失敗在 `useNativeMagicDoll` 前段（例如設定或背包欄位），可以參考 `ClientSessionNativeDropIntegrationTest.sourceMagicDollSummonsRecallsAndExpiresThroughNativePackets`。那個測試做的是同一套 setup，但被 `assumeServerProperties` 守門，所以 CI 上會跳過。
- 若日後要恢復娃娃圖示：先在 `LiTo.bin` 找有沒有娃娃計時圖示的接收端（S_EVENT 0..40 各 case，或其他 opcode），確認之後再把 `sendNativeMagicDollIcon` 接回去，並更新新測試的斷言。
- T111（同一矩陣列）可以接著做。
