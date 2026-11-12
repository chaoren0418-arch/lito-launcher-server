# 租裝城鎮返回／轉場：伺服器回覆契約（Issue #368，對應 lito-modern T040／T055）

租裝城鎮場景在伺服器端是 `Phase.BATTLE`＋`rentalTownRoomState == true`，背後沒有 `GameHub` 房間（合成房號，預設 `0x50000001`）。
`C_D9`（`C_LITO_QUIT_FIGHT`）與 `C_D0`（`C_LITO_EXIT_ROOM`）的第一個 BATTLE 判斷只對「伺服器自己已經排定的租裝轉場」放行：

| 入口 | 放行條件（`ClientSession`） | 判斷函式 |
|---|---|---|
| `C_D9` | `rentalTownRoomState && (rentalTownTransferPending \|\| pendingTransition == RESTART_AWAIT_QUIT_FIGHT)` | `stagedRentalTownQuitFight()` |
| `C_D0` | `rentalTownRoomState && pendingTransition == RESTART_AWAIT_RETURN` | `stagedRentalTownReturn()` |

不符合條件時行為與 0.6.71 相同：走 `settleRequestedBattleExit()`。城鎮沒有 `GameHub` 房間，所以這條路徑不送任何封包、場景不變。

## 返回／重新開始（restart）

| 步驟 | 客戶端 | 伺服器回覆 | 伺服器狀態 |
|---|---|---|---|
| 1 | `C_RESTART` | `S_RESTART` | `pendingTransition = RESTART_AWAIT_QUIT_FIGHT` |
| 2 | `C_D9` | `S_F0`：`c(0xF0) c(0)` | `pendingTransition = RESTART_AWAIT_RETURN`，仍在 BATTLE／城鎮 |
| 3 | `C_D0` | `S_D0`：`c(0xD0) d(roomId) s(自己的 litoDisplayName)` → 每個租用品一筆 `S_REMOVE_INVENTORY`（含光之寶石、城鎮地圖、傳送石）→ `S_F5`（`S_LITO_VS_SERVER_ALLOW_JOIN`） | `phase = LOBBY`、`rentalTownRoomState = false`、`currentRoomId = 0`、`rentalItemSetSelection = -1`、`pendingTransition = RESTART_AWAIT_SOURCE_REFRESH` |
| 4 | `C_C8`（來源刷新） | 角色清單 | `pendingTransition = NONE`，可重新選租裝組合入村 |

步驟 3 時，同城鎮、同可見範圍的其他玩家收到 `S_REMOVE_OBJECT`（離開者）與離開者的 `S_D0`；不會收到 `S_D3`／`S_F0`。旅館租約依 `prepareRentalHotelDeparture` 既有規則清理。

## 傳送石／傳送師轉場（`rentalTownTransferPending`）

`C_D9` 直接完成城鎮離場：清租用品、`phase = LOBBY`、回 `S_F0(0)`；有設定跨端點（`pendingRentalTeleportEndpoint`）時另外釋放帳號鎖並在回覆後斷線。細節沿用 `quitFight()` 既有分支，本次只是讓它在 BATTLE 下執行得到。

## 錯序與重複

- 沒有 pending 轉場的 `C_D9`／`C_D0`：不送封包、場景不變。
- `C_RESTART` 後沒送 `C_D9` 就送 `C_D0`：不放行，`pendingTransition` 維持 `RESTART_AWAIT_QUIT_FIGHT`。
- 重複 `C_D9`（已是 `RESTART_AWAIT_RETURN`）：不再回 `S_F0`，狀態不變；之後的 `C_D0` 仍正常完成一次。
- 完成後重送 `C_D0`（已在 LOBBY）：不送封包。

## 不受影響

- 一般戰鬥的 `C_D9`／`C_D0`：仍是 `player-withdrawal` 結算，即使該 session 有 `RESTART_AWAIT_*`（放行條件要求 `rentalTownRoomState`）。
- #366 觀戰者獨自離場（`battleObserverMayLeaveAlone()` 要求 `!rentalTownRoomState`，與本契約互斥）。
