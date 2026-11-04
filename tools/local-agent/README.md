# 本機任務代理（local-agent）

雲端主控（Claude）需要**你電腦上的資源**時（客戶端 pak／map／sprite、MySQL、原生遊戲、逆向資料），會在 GitHub 開一個帶 `local-task` 標籤的 Issue。本程式在你電腦上監看這些 Issue，收到就用 **Codex 桌面版的 CLI**（或 Claude Code）在對應專案目錄執行，做完把結果回貼 Issue、關閉，有檔案就推到 `local-task/<issue 編號>` 分支。全程事件驅動，沒有排程。

```
Claude（雲端）──開 Issue [local-task]──▶ GitHub ◀──每分鐘掃一次── local-agent.ps1（你電腦）
                                            ▲                              │
                                            └── 結果留言／分支／關閉 ◀── codex exec（本機執行）
```

## 一次性準備（約 5 分鐘）
1. **gh CLI**：`winget install --id GitHub.cli -e`，然後 `gh auth login`（選 GitHub.com → HTTPS → 用瀏覽器登入 chaoren0418-arch）。
2. **Codex CLI**：Codex 桌面版已附帶；若 `codex --version` 找不到，到 Codex 桌面版設定裡安裝 CLI，或 `npm install -g @openai/codex`。
3. （可選）**Claude Code** 當第二個 runner：`npm install -g @anthropic-ai/claude-code`，然後 `claude` 登入一次。
4. 三個 repo 各加標籤（一次）：
   ```
   gh label create local-task -R chaoren0418-arch/tw-bin-engine -c 0E8A16 -d "需要本機執行的任務"
   gh label create running    -R chaoren0418-arch/tw-bin-engine -c FBCA04
   gh label create done       -R chaoren0418-arch/tw-bin-engine -c 0075CA
   gh label create failed     -R chaoren0418-arch/tw-bin-engine -c D93F0B
   ```
   （對 lito-launcher-server、lito-modern 各再跑一次，或執行 `setup-labels.cmd`。）

## 啟動
雙擊 `start-local-agent.cmd`（視窗保持開著就是在監聽）。要開機自動啟動：把這個 .cmd 的捷徑放進 `shell:startup`。
參數：`-Runner claude` 改用 Claude Code；`-Once` 只掃一輪；`-IntervalSeconds 30` 改掃描間隔。

## 測試
在任一 repo 開 Issue，標籤 `local-task`，內容例如：
```
執行 engine\啟動新版客戶端.cmd --check-only，把輸出存到 docs/local-task/issue-N/check-only.json，並回報 revision 與 uiStarted。
```
一分鐘內 Issue 會出現「本機代理已接手」，幾分鐘後出現結果。

## 安全界線
- 只處理 `chaoren0418-arch` 名下、`$Repos` 清單內的 repo。
- Codex 以 `--full-auto` 在專案目錄內執行；它遵守 AGENTS.md 的禁止事項（不碰機密、封存、正式 BIN，不啟停 DB）。要更嚴可改成預設模式，每步會問你。
- 新專案要納入：在 `local-agent.ps1` 的 `$Repos` 加一行即可（v2 儀表板之後會改成從設定檔讀）。
