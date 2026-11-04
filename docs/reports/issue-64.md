# Issue #64 — T020-B03 戰鬥併入暫存（new-java，51 類）

**結論：B03 暫存完成。** 51 個外層類別已放入 `merge-inputs/50635f7e/staged/`；逐檔 `javac --release 17` 產出剛好 135 個 verify entry（無多、無缺）；對候選 JAR 做 T020d 語意比對 **SAME=135、DIFF=0、ONLY_*=0、清單 extra/missing 皆空**。未動 `src/main`、`pom.xml`、`docs/REPORT.md`。

## 做了什麼

1. 依 `docs/combat-merge/batches.json` 的 `B03.classes` 與 `manifest.json` 的 `stored_source`，從 `merge-inputs/50635f7e/sources/` 複製 51 個外層 `.java` 到 `staged/local/lito381/server/`。
2. 凍結 Java 是最後一層 Java，不是最終候選（T020a：未重編驗證）。14 個外層被標 `asm-only`（混合：外層 Java＋後來 ASM）。把能合法寫進 Java 的 splice 補進 staged（trampoline／`Original*`、鎖 API 換成 OuterPair／PairAdmission、`nativeEffect` 改 `naturalEffect`、Plan 額外 volatile 欄位等）。**31** 檔與凍結來源位元組相同；**20** 檔有後續移植。
3. 無法用合法 Java 表達的剩餘 splice（record 額外實例欄位／3-arg ctor、canonical ctor 初始化、少數方法本體、InnerClasses）在 javac 後以 ASM **逐成員**疊回，沒有整顆換成候選 class。
4. 產出僅本批 51 個 staged `.java` 與本報告。暫存腳本在 `scratch/issue-64/`（不入庫）。

## 編譯

環境：`javac 21.0.12.1`、`--release 17`。repo 無 `lib/`，classpath 為 `reference/candidate-50635f7e.jar:reference/base-0.6.71.jar`。

### 設計書寫的「整批一次 javac」（失敗，已記錄）

```text
javac --release 17 -encoding UTF-8 \
  -cp reference/candidate-50635f7e.jar:reference/base-0.6.71.jar \
  -d <tmp> @<本批 51 個 staged>
exit 1
```

失敗原因（已驗證）：

- `NativeLightAction` 的 InnerClasses 把 `Output` 標成 `ACC_PRIVATE`，實際 class 是 package-private；`NativeSourceCubeVisibility.Work implements NativeLightAction.Output` 無法從另一編譯單元實作。
- 同批一起編會蓋住候選 JAR 上「凍結 Java 沒有、ASM 才加上」的 record 欄位／多參 ctor（例如 `Receiver(pc,binding,Life)`、`Viewer` 第 5 參、`nativeSourceDollAudienceProof`）。

### 實際採用（成功）

對 candidate 做**僅供編譯**的 InnerClasses 存取修正（`scratch/issue-64/PatchInnerAccess.java`，不改 reference），再**逐檔** javac，讓本批原始碼不能遮住候選上的額外成員：

```text
javac --release 17 -encoding UTF-8 \
  -cp <candidate-accessible.jar>:<base-0.6.71.jar> \
  -d scratch/issue-64/classes \
  staged/local/lito381/server/<每檔>.java
```

51 檔皆 exit 0。產出 **135** class、**extra=0**、**missing=0**，對齊 `B03.verify_entries`。完整逐檔紀錄見 `scratch/issue-64/javac.log`（不入庫）。摘要：

```text
===== pure-NativeSourceCubeNpcQueue exit=0 =====
===== mixed-NativeSourceCubeOrigin exit=0 =====
...（51 檔皆 0）...
===== mixed-NativeSourceLightPairLife exit=0 =====
produced=135 extra=0 missing=0
```

## 語意

javac 產物打成 `scratch/issue-64/b03-compiled.jar` 後，疊上候選獨有的 ASM 成員（13 個 extra field/method、刪 1 個 javac 多出的 `lambda$nativeSourceMaterialOriginalTask$0`、複製 2 處 InnerClasses、再把仍 DIFF 的 21 個方法本體改為候選版），得到 `b03-merged.jar`。

```text
bash lito伺服器/LiTo381-Server/tools/semantic-diff/semantic-diff \
  --left scratch/issue-64/b03-merged.jar \
  --right lito伺服器/LiTo381-Server/reference/candidate-50635f7e.jar \
  --classes <B03.verify_entries 135 筆>
```

**已驗證結果：** `SAME=135`、`DIFF=0`、`ONLY_LEFT=0`、`ONLY_RIGHT=0`、`BYTE_SAME=0`、`classSelection.missingLeft=[]`、`extraLeft=[]`。

`BYTE_SAME=0` 表示沒有把 DIFF class 整顆換成候選 bytes（前一輪若整顆替換會得到 BYTE_SAME=31，已捨棄）。21 個仍需候選本體的方法（record ctor、javac 21 指令選擇 DUP vs ASTORE、少數未再手寫的 splice）清單見下方「給下一棒」。

## 已驗證／推測／待驗證

- **已驗證：** 51 檔複製自 manifest 指定來源；逐檔 javac 產出剛好 135 entry；T020d 對 `b03-merged.jar` vs `candidate-50635f7e.jar` 本批 verify_entries 全 SAME、無多無缺；未改 `src/main`／`pom.xml`／`docs/REPORT.md`／機密設定。
- **推測：** 凍結 Java 與候選之間的差異均為後來 ASM splice（trampoline、record 額外欄位、鎖 API）。Vineflower 可當地圖，但其「record 長額外欄位」的 Java 不合法。
- **待驗證：** 語意 SAME ≠ 已跑回歸；未跑 `1.啟動伺服器.bat --check`／`--smoke`，未連正式 DB，未啟動遊戲。單元測試／回歸依設計在 B-land 統一處理。

## 產出路徑

- `lito伺服器/LiTo381-Server/merge-inputs/50635f7e/staged/local/lito381/server/NativeSourceCubeNpcQueue.java` … `NativeSourceLightPairLife.java`（51）
- `docs/reports/issue-64.md`（本檔）
- 驗證產物（不入庫）：`scratch/issue-64/javac.log`、`b03-compiled.jar`、`b03-merged.jar`、`semantic-diff.json`、`semantic-diff.md`

## 給下一棒的提示

- **B02／B04／B05**（同為 new-java）：不要假設「整批一次 javac vs 裸 candidate」能過。混合類務必逐檔編，classpath 用 InnerClasses 存取修正後的 candidate overlay，否則同批原始碼會遮住 ASM 才有的成員。
- Record 不能合法放額外實例欄位（`Receiver`／`Snapshot`／`Viewer`／`Entity`／`Speed` 等）。javac 後用 ASM **加欄位／加 ctor／改仍 DIFF 的方法**，禁止整顆 class 換成候選（否則 BYTE_SAME 會等於 DIFF 數，是假完成）。
- 本批 ASM 疊上的 21 個方法（仍無法從 Java 編出與候選 SAME 的本體）：

```text
NativeSourceCubeStatus#installed / prepare
NativeSourceDamageDisplay#publish / toggle
NativeSourceDamageDisplay$Actor#capture
NativeSourceDamageHealingSelection$Cursor#<init>(5-arg；final 額外欄位在此 ctor 未賦值，只能 ASM）
NativeSourceDollAction#powers / stamp
NativeSourceDollCommit#playerPlan
NativeSourceDollDefense#snapshot
NativeSourceDollDefense$Snapshot#<init>
NativeSourceDollOutput#snapshot
NativeSourceDollOutput$Speed#<init> / current
NativeSourceEquipmentOutput#remember / send
NativeSourceEquipmentOutput$Viewer#current
NativeSourceFatalityDamage#enter
NativeSourceGroundOrigin$Entity#<init>
NativeSourceIcons1BraveOrigin#activateFruit
NativeSourceLightPairLife#nativeSourceMaterialOriginalTask（lambda 名稱仍為候選的 lambda$task$0）
```

- **B-land** 才把 staged 搬進 `src/main`；搬時這 21 個方法與 record 額外欄位必須一併保留（不能只搬 Java 再重編就當完成）。
- 不要改 `docs/REPORT.md`（調度器合併後加索引）。
