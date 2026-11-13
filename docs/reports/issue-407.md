# Issue #407 — T141：回歸工具收據寫入的重複 keyword TypeError（#341 項 11）

分支：`task/407`　PR：#408　階段：code（第 1 次嘗試）

## 結論

- `replay_targeted.py:38`、`replay_player_cancellation.py:32` 的 `finally` 收據寫入已修正，不再拋 `TypeError: dict() got multiple values for keyword argument 'formal_deployment_changed'`；收據在成功／失敗兩種結尾都會落在 `passed`／`failed`，不會停在 `running`。
- `candidate_regression.py` 的 reconciliation 失敗訊息改為帶出根因（第一個失敗的 driver、其 log、manifest 的 error），不再只剩一個空的 `AssertionError`。
- 新增離線測試 `test_receipt_entrypoints.py`（9 案），掛在 CI `combat-regression-tools` job。

## 根因

`regression_support.formal_protection()`（`regression_support.py:215-216`）在 guard 量完 before／after 後，會把 `formal_deployment_changed` 寫進呼叫端傳入的 `protection` dict。兩個入口最後寫收據時用的是：

```python
dict(..., **dict(formal_deployment_changed=False, **protection))
```

內層 `dict()` 同時收到具名參數 `formal_deployment_changed=False` 和 `**protection` 裡的同名鍵，所以拋 `TypeError`。只要 guard 有量到 after 就會觸發（`--out` 模式下 body 失敗或成功都一樣），所以最初寫下的 `status=running, tests=[]` 收據永遠不會被覆寫。共用函式 `formal_protection` 本身的行為沒問題，問題只在兩個呼叫端的合併寫法。

## 做了什麼

| 檔案 | 改動 |
|---|---|
| `lito伺服器/LiTo381-Server/tools/combat-continuation/cancellation-battle-runtime/replay_targeted.py` | `**dict(formal_deployment_changed=False,**protection)` → `**{'formal_deployment_changed':False,**protection}`。用 dict literal 合併時，後面的鍵會蓋掉前面的，所以 guard 量到的值（例如 `True`）優先，預設 `False` 只在 guard 沒量到 after 時才用。 |
| `.../cancellation-battle-runtime/replay_player_cancellation.py` | 同上，只改這一行。 |
| `.../combat-continuation/candidate_regression.py` | 新增 `failure_summary()`；`full_regression.py` 回傳非 0 時先把根因印到 stderr；`reconcile()` 原本的「最後一筆必須是 `replay-native-de-item-state.py`」斷言保留，只加上訊息與根因；`results` 為空時也改成同一個斷言失敗（原本是 `IndexError`）。 |
| `.../combat-continuation/test_receipt_entrypoints.py`（新增） | 離線收據測試，說明見下節。 |
| `.github/workflows/build.yml` | `combat-regression-tools` job：py_compile 納入新測試，並新增「Offline entry receipt tests」步驟。 |

保留不動的部分：所有行為斷言（入口內的 `assert t.count(old)==1`、`runtime` 比對、子 driver `check=True`）、`formal_protection` 的正式檔保護與 `verify_formal_protection`、`except BaseException: status='failed'; raise` 失敗收據路徑、中途 `running` 收據的寫法。戰鬥判定與 `src/main` 都沒改。

## 怎麼驗證

測試在 CI 執行，本機沒有執行任何程式。

`test_receipt_entrypoints.py` 用 `runpy` 在同一個程序裡跑真正的入口腳本，`--jar/--out/--resources/--config` 都指到 `scratch/` 下的暫存樹（`scratch/` 已在 git ignore 內，測試結束就刪）。`subprocess.run` 換成 stub，`regression_support.pin` 只對正式 JAR／reference 兩個路徑回傳授權 SHA。所以 JAR、SQL、子 driver 都不會實際執行；入口讀取並改寫的是 repo 內真正的 27＋3 支 driver 原始碼，替換斷言照常生效。

| 測試 | targeted | player |
|---|---|---|
| 子 driver 失敗（#341 情境：第一顆 probe 連不到 SQL）→ 收據 `failed`、`formal_deployment_changed=False`、`formal_protection.status=passed` | ✔ | ✔ |
| 全部通過 → 收據 `passed`，tests 筆數為 27／3，`verify_formal_protection` 通過 | ✔ | ✔ |
| 執行中正式檔被改 → 收據 `failed`，且 `formal_deployment_changed=True`（確認量測值會蓋過預設值） | ✔ | ✔ |
| guard 啟動就拒絕（正式檔 SHA 不符）→ body 沒跑，收據 `failed` | ✔ | ✔ |
| `candidate_regression`：第 1 組 `-source.py` 失敗 → 斷言訊息與 stderr 帶出 `failed_driver`、log 路徑、manifest error | ✔（單案） | |

CI 結果：
- 第一次 push（run 43484484467）：`test_formal_protection` 14 案全過；新測試 9 案中 8 案通過。唯一失敗的是我的預期值寫錯：targeted 在 `--out` 模式下是 25 支 stage driver 加 2 支 replacement／fault，共 27 支，跟入口自己 `len(rows)==27` 的判斷一致，不是 29。已改正。
- 第二次 push（run 43486300669，commit `df9665ba`）：`Combat regression tools (offline, no DB)` job **success**，新測試 9 案與 `test_formal_protection` 14 案全過。其他 job 與本次改動無關（只動 Python 工具與這個 job），結果以 PR #408 最新 commit 為準。

## 已驗證／推測／待驗證

**已驗證**（CI 離線測試，run 43486300669 共 9 案全過）
- 兩個入口的成功路徑會寫出 `passed` 收據，筆數為 27／3。
- 兩個入口的失敗收據路徑都不再拋 `TypeError`，收據會落在 `failed`。
- 正式檔有變動時，收據的 `formal_deployment_changed` 是 `True`。
- 原本的 `test_formal_protection` 14 案沒有受影響。

**推測**
- #341 報告推測「成功完成行為回歸後，finally 一樣會觸發同一個錯誤」。靜態上確實如此。修正後的成功路徑已由 `*_success_*` 測試在 stub driver 下驗證，但真實 driver 的成功路徑還沒有實跑過。
- 其他入口（`replay_composed_materials.py`、`full_regression.py`、`affected_regressions.py`、`test_curse_clock_boundary.py`、`seal_validation.py`、`reconcile_replay.py`）都把 receipt dict 本身交給 `formal_protection`，沒有再用具名參數重複合併。以 grep 檢查，沒有同類寫法。

**待驗證**
- 真機重跑 #341 的 05／06（需要 P24Lab:43324 與有效 lease），確認實際 log 不再出現 `TypeError`，且收據狀態是 `failed`（連不上 DB 時）或 `passed`。
- 戰鬥行為等價（7,588 案）仍未驗證，不屬於本任務範圍。

## candidate reconciliation 錯誤呈現評估

- **原狀**：`candidate_regression.py` 在 `full_regression.py` 失敗後一律呼叫 `reconcile()`，而 `reconcile()` 第一行就斷言「最後一筆結果是 `replay-native-de-item-state.py`」。#341 的第 1 組 `-source.py` 因為連不到 SQL 而失敗，終端最後只剩一個沒有訊息的 `AssertionError`。真正的 SQL 錯誤只出現在更前面 `full_regression.py` 印出的 log 尾段，以及內層 log 檔，所以根因確實容易被掩蓋。
- **已改（小改）**：失敗時先印 `FULL REGRESSION FAILED before reconciliation: status=…; error=frozen qualification failed: <driver>; failed_driver=<第一個非 0 driver>; log=<路徑>; manifest=<路徑>`；斷言失敗訊息改為 `Not reconcilable (...); root cause: …`。是否能 reconcile 的條件完全不變。
- **沒改、建議另開任務**：
  1. `full_regression.py` 在 `--out` 模式下，guard 啟動前就失敗（例如 SHA 不符）時，manifest 的 `results` 是空的，只有 `error` 欄位，現在的摘要會顯示 `failed_driver=None`，可以接受。
  2. 更根本的做法是在 `full_regression` 之前先檢查 DB 連線（對 `--config` 的 host:port 做 TCP 檢查），連不上就直接失敗，避免跑 18 秒才看到結果。這會改變入口行為與失敗時機，超出「小改」，留給後續任務決定。

## 產出路徑

- `lito伺服器/LiTo381-Server/tools/combat-continuation/cancellation-battle-runtime/replay_targeted.py`
- `lito伺服器/LiTo381-Server/tools/combat-continuation/cancellation-battle-runtime/replay_player_cancellation.py`
- `lito伺服器/LiTo381-Server/tools/combat-continuation/candidate_regression.py`
- `lito伺服器/LiTo381-Server/tools/combat-continuation/test_receipt_entrypoints.py`
- `.github/workflows/build.yml`（`combat-regression-tools` job）
- `docs/reports/issue-407.md`（本檔）

## 給下一棒的提示

1. **Claude 審查**：重點看兩行修正的合併順序（量測值必須蓋過預設值，`*_formal_change_is_recorded` 測試有涵蓋），以及 `candidate_regression.py` 是否只多了訊息、沒有放寬 reconcile 條件。
2. **本機重測執行器（#341 後續）**：P24Lab 就緒後，用全新輸出目錄重跑 A／B（targeted／player）。項 11 應該消失；若 DB 仍連不上，收據會是 `failed`，不再是 `running`，seal 的拒絕訊息也會從「running」變成「failed」。
3. 可以考慮另開任務，在入口前加 DB 連線預檢（見上節第 2 點）。
