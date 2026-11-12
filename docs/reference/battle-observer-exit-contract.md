# BATTLE 中觀戰者主動離場：伺服器回覆契約（Issue #366，對應 lito-modern#189）

適用：`ClientSession.phase == BATTLE`、`observer == true`、該 session **不在** `battleRoster`（`GameHub.battleResultCombatants`）內、非租屋城鎮狀態。
判斷函式：`ClientSession.battleObserverMayLeaveAlone()`。不符合以上條件（例如參戰者）時行為不變，仍走 `settleRequestedBattleExit()`（`player-withdrawal` 結算）。

## 客戶端送出 `C_D0`（`C_LITO_EXIT_ROOM`，opcode-only）

觀戰者自己依序收到：

1. `S_D0` `S_LITO_EXIT_ROOM`：`c(0xD0) d(roomId) s(觀戰者 litoDisplayName)`——自己的離房列。
2. （僅在 native 競技經濟開啟且已初始化時）`sendPointStats()` 的點數狀態封包。
3. `S_CC` 房間列表列（`sendRoomList`，每個可見房一列；BATTLE 中的房依既有規則顯示或隱藏）。
4. （僅在 native 競技經濟開啟且已初始化時）系統訊息「已返回大廳…」。

伺服器狀態：`phase = CHANNEL`、`currentRoomId = 0`、`ready = false`、好友狀態改為 WAITING；`observer` 旗標**保留**（可直接再次以觀戰身分加入）。
若觀戰者是 `/argo` 進來的（`currentChannelId == ARGO_CHANNEL_ID`），一併離開 argo 頻道成員資格（`detachArgoRoomMembership`，與一般離房相同）。

## 客戶端送出 `C_D9`（`C_LITO_QUIT_FIGHT`，opcode-only）

觀戰者自己依序收到：

1. `S_F0` `S_LITO_QUIT_FIGHT`：`c(0xF0) c(0)`——接受 native 戰鬥中「退出」。
2. 之後與 `C_D0` 完全相同（`S_D0` 自己的離房列 → `S_CC` 房間列表 …）。

## 房內其他成員（參戰者與其他觀戰者）

- 只收到一筆 `S_D0`：`c(0xD0) d(roomId) s(離場觀戰者 litoDisplayName)`，用來把觀戰者從名單／觀戰人數移除。
- **不會**收到 `S_REMOVE_OBJECT`（觀戰者在戰場上本來就沒有實體物件；與觀戰者斷線時的 `notifyPeersOfDisconnect` 一致）。
- **不會**收到 `S_D3`／`S_F0`／倒數／房主變更；戰鬥、倒數、`battleRoster`、`battleForces`、勝敗紀錄都不變。
- 同頻道停在 CHANNEL（大廳）的玩家：由 `LobbyRoomDirectory.refresh` 重送房間列表（`S_CC`）。

## 不受影響的既有行為

- 參戰者 `C_D0`／`C_D9`：仍走 `settleRequestedBattleExit`（`player-withdrawal`，戰鬥中受 `lito.combatExitDelaySeconds` 限制）。
- 觀戰者斷線：仍走 `notifyPeersOfDisconnect`，不進 `registerBattleDisconnect`。
- 最後一名成員離房刪房／預設房 reset：沿用 `GameHub.leaveRoom` → `resetOrRemove`，觀戰者離場時房內仍有參戰者，因此不會觸發。
- 戰鬥進行中的房仍允許觀戰者重新加入（`tryJoinRoom` 只擋非觀戰者），重新加入後走既有的 late-observer `startBattle` bootstrap。

## 客戶端接線建議

- 觀戰中按「退出」：送 `C_D0`（或 native 路徑的 `C_D9`），等待自己的 `S_D0`（roomId 與本機顯示名相符）後拆戰場場景、回到房間列表（之後的 `S_CC` 列）。
- 收到別人的 `S_D0` 時，若名稱對應觀戰者，只更新觀戰名單／人數，不影響戰鬥 UI。
