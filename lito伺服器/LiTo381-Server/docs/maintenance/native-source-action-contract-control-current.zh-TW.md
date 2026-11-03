# 凍結 Source action 演算法的實際 native 雙角色對照

2026-10-18：未改 production runtime、未部署。完整 Source API 與巢狀 pair 修正仍未通過；這是下一階段的可重現控制。

凍結目前來源 PlayerActionLock.java 與 target class，完整 class descriptor 投影到實際 ClientSession，只有兩個 signed-ID、一次 actual PC action-lock getter、八個編譯後 drain 呼叫轉接。原 Source finally 在正常／例外分支各有重複的 drain 指令。轉接可完整 inverse 回到同一份凍結 Source class 的 remapped body；未改排序、條件、鎖、finally 或 Runnable／Supplier 呼叫。fixture 產生單一 FrozenSourcePlayerActionLock class，實際 PC／MP producer／vital queue／lifetime 全從指定 immutable JAR 取得，沒有 production overlay 或另一份 PC/HP/MP 權威。

這不是執行來源伺服器的 L1PcInstance／L1Character runtime；它驗證的是凍結 Source 演算法接到目前實際 native authority 與 completion queue 的行為。完整 Source API 仍保持未啟用。

十六案二百六十六項檢查覆蓋兩種 memory-only 旗標、BATTLE／WORLD、top-level pair、外層 run 包 pair、外層 call 包 pair、同 unsigned ID 的兩個不同實體及外層主例外。兩個實際 NativePlayerCommit.debitMp 分別提交 7／9，原 MP snapshot 為 93／91；三十二個真實 TCP frame 由原 cipher 與 Python 獨立解密，輸出時不持任一角色的 action/status lock。

四個 top-level pair 案會排空雙方。十二個巢狀案中，inner pair 因 outer 仍持有其中一把鎖而跳過雙方 drain；outer 單角色 finally 只排空自己，另一實際 PC queue 留下一筆尚未發布的 MP completion。shared hub/application lifetime 此時仍有一筆 receipt；不能把 lifetime pending 誤作 per-PC 計數。控制先確認該 queue、無 peer wire 與 shared receipt，再明確呼叫既有 native drainer 進行 observation cleanup，釋放全部 receipt。**這項明確清理不是 Source 或 native 修正**。

三份準備失敗記錄保留：初始轉接數量把 source-level 四次 drain 誤認為四個編譯指令，實際正常／例外複本共八個，工具在執行 native fixture 前拒絕；初版 assertion 誤把 shared lifetime pending 當成 per-PC，且 cleanup failure 曾遮住主錯誤。修正觀察工具後，per-PC queue／wire 條件保持，shared receipt 改按實際 hub owner 檢查，例外清理明確排空雙方並保留主例外，未改 native 或 Source 演算法。

下一步必須處理 outer scope 的所有實體 participant，以及另一個 drainer 在 outer 第一把鎖仍持有時取得 peer queue 的情況。只記錄 producer-thread participants 不會自動阻止跨執行緒 drainer；應驗證 FIFO、非忙等、無新增 worker/timer、無失去喚醒／orphan receipt，並串接 HP handoff／重入／例外／owner retirement／Main normal/interrupt。真正並行的巢狀鎖排序、identity-hash tie、全部 callback/I/O hierarchy 尚未驗證，不能宣告完整 API 完成。

收據：`tools/resource-database/build/native-source-action-contract-control-current.json`。重驗：`python -B -X utf8 tools/resource-database/build/audit-source-action-pair.py`。保留本輪通過控制與準備失敗；無重複完成場次需要清理。
