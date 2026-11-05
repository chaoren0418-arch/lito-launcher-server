# REPORT

本檔由各輪任務更新「本輪變更」與「下一步」。歷史細節見 `docs/reports/`。

## 本輪變更

- Issue #2（T007）：`pom.xml` 版本改 0.6.71，預設 Main-Class 改 `local.lito381.server.SharedResourceMain`；CI 改 `mvn package` 並用 `jar tf` 產出／上傳 `docs/server-source-gap.md`。只在 reference 的 16 個類別已分類（待補 1：`NativeDeVisibilitySwitch`；刻意排除 15 個 inner／匿名類；Modern* 0）。PR #7 CI 已綠（Server + Launcher）。

## 下一步

- Issue #2：PR #7 CI 已綠（Server + Launcher）。下一步由 Claude 審合併，並把 `TASKS.md` T007 標 review／done；T008 補 `NativeDeVisibilitySwitch`；T005 修 testCompile 後再拿掉 `-Dmaven.test.skip=true`。
- 2026-11-04 — #12 T009: 測試依賴本機資源的全部加 assume 守門，讓 mvn test 全綠（code，codex-local，PR #15 已合併）→ [報告](reports/issue-12.md)
- 2026-11-04 — #20 T010: 修復 T009 清單上的真實測試失敗（約 30 案）（code，cursor-cloud，PR #22 已合併）→ [報告](reports/issue-20.md)
- 2026-11-04 — #24 T020: 戰鬥候選併入 src/main 的合併策略（設計）＋ M3 T040 拆任務（design，claude-chat，分支 task/24）→ [報告](reports/issue-24.md)
- 2026-11-04 — #4 T003: 登入器建置腳本去硬編路徑（code，codex-local，PR #8 已合併）→ [報告](reports/issue-4.md)
- 2026-11-04 — #32 T040: 安裝手冊＋打包腳本（code，cursor-cloud，PR #33 已合併）→ [報告](reports/issue-32.md)
- 2026-11-04 — #37 T012: 修不穩定測試 MonsterAggression productionWorldCompanionClearsProvenUnreachableTargetOnTwentiethRetry（code，codex-local，PR #39 已合併）→ [報告](reports/issue-37.md)
- 2026-11-04 — #43 T020b: 依類別差異訂戰鬥合併批次（設計）（design，claude-chat，分支 task/43）→ [報告](reports/issue-43.md)
- 2026-11-04 — #44 T020d: ASM 語意比對工具＋CI 語意差異報告（code，codex-local，PR #48 已合併）→ [報告](reports/issue-44.md)
- 2026-11-05 — #30 T020a: 戰鬥候選 × 正式 0.6.71 類別差異盤點（本機）（resource，codex-local，PR #56 已合併）→ [報告](reports/issue-30.md)
- 2026-11-05 — #45 T020f: 候選 50635f7e JAR 與凍結原始碼入庫（本機）（resource，codex-local，PR #58 已合併）→ [報告](reports/issue-45.md)
- 2026-11-05 — #59 T020e: 依語意差異訂 A 階段（src/main→正式 0.6.71）修補批次（設計）（design，claude-chat，PR #61 已合併）→ [報告](reports/issue-59.md)
- 2026-11-05 — #102 T020g: @AsRecord 轉換器（record 加 ASM 實例欄位的 Java 表達）＋Maven 建置接線（code，codex-local，PR 已合併）→ [報告](reports/issue-102.md)
- 2026-11-05 — #132 T020i: semantic-diff 正規化擴充（整數常數、ACC_SYNTHETIC、catch-any）＋核准方法的副作用序列比對（code，codex-local，PR 已合併）→ [報告](reports/issue-132.md)
- 2026-11-05 — #108 T020h: 舊伺服器 com.lineage.* 編譯期 stub（只供編譯、不打包）（code，codex-local，PR #147 已合併）→ [報告](reports/issue-108.md)
- 2026-11-05 — #135 T020i-2: semantic-diff 正規化第二批（InnerClasses、ACC_SUPER、隱式建構子、區域變數重編號、核准額外方法）（code，codex-local，PR #152 已合併）→ [報告](reports/issue-135.md)
- 2026-11-05 — #134 T020g-2: @AsRecord 轉換器接進 Maven 建置＋record-asm-fields.json 掃描（code，codex-local，PR #157 已合併）→ [報告](reports/issue-134.md)
- 2026-11-05 — #159 T041: 伺服器測試離房後計數偶發失敗（ModernWorldService 清理時序競態）（code，codex-local，PR 已合併）→ [報告](reports/issue-159.md)
- 2026-11-05 — #153 T020i-3: lambda 依內容比對、私有 lambda handle H5≡H7、@AsmFinal（一般類別 final 欄位）（code，codex-local，PR #164 已合併）→ [報告](reports/issue-153.md)
- 2026-11-05 — #166 T020i-4: semantic-diff 正規化第四批（轉呼叫 lambda≡直接 handle、相鄰保護範圍合併、單次使用暫存、捕獲參數排列）（code，codex-local，PR 已合併）→ [報告](reports/issue-166.md)
- 2026-11-05 — #174 T020i-5: semantic-diff 正規化第五批（例外涵蓋正規形、多餘 null 檢查、區塊內 local 讀取位置、Exceptions 屬性豁免）（code，codex-local，PR #178 已合併）→ [報告](reports/issue-174.md)
- 2026-11-05 — #76 T020-A05: T020-A05: src/main 對齊正式 0.6.71 暫存（other，27 類）（code，codex-local，PR 已合併）→ [報告](reports/issue-76.md)
- 2026-11-05 — #184 T042: 調查 CombatSkillDerived 技能表欄位（SKU1/SKU2/SKD1-3）遺失的潛伏 bug（本機）（resource，codex-local，PR #198 已合併）→ [報告](reports/issue-184.md)
- 2026-11-05 — #176 T020i-6: semantic-diff 第六批（巢狀類別存取放寬、無迴圈方法逐路徑比對、核准方法偏離）（code，codex-local，PR 已合併）→ [報告](reports/issue-176.md)
- 2026-11-06 — #63 T020-B02: 戰鬥併入暫存（new-java，51 類）（code，codex-local，PR #81 已合併）→ [報告](reports/issue-63.md)
- 2026-11-06 — #186 T020i-7: semantic-diff 的 $SwitchMap$ 兩側對稱改名、原樣轉拋 handler、private 呼叫指令（code，codex-local，PR 已合併）→ [報告](reports/issue-186.md)
- 2026-11-06 — #75 T020-A04: T020-A04: src/main 對齊正式 0.6.71 暫存（battle-combat，25 類）（code，codex-local，PR #106 已合併）→ [報告](reports/issue-75.md)
- 2026-11-06 — #74 T020-A03: T020-A03: src/main 對齊正式 0.6.71 暫存（nativede，47 類）（code，codex-local，PR #100 已合併）→ [報告](reports/issue-74.md)
- 2026-11-06 — #66 T020-B05: 戰鬥併入暫存（new-java，48 類）（code，codex-local，PR #88 已合併）→ [報告](reports/issue-66.md)
- 2026-11-06 — #77 T020-A06: T020-A06: src/main 對齊正式 0.6.71 暫存（other，27 類）（code，codex-local，PR #109 已合併）→ [報告](reports/issue-77.md)
- 2026-11-06 — #203 T020i-8: semantic-diff 依呼叫點配對 lambda＋不看分支方向的 CFG 比對＋可見性 bridge（code，codex-local，PR #215 已合併）→ [報告](reports/issue-203.md)
