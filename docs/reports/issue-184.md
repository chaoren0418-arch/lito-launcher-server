# Issue #184／T042 — CombatSkillDerived 技能表遺失調查

調查日期為 2026-11-05。工作分支為 `task/184`，工作目錄為 `scratch/wt/task-184`。本次交付僅此報告。

## 結論

**已驗證根因與引入層。** 2026-10-17 的 `de-construction-development` patch 將 `L1AttackList` remap 成 `NativeDeCharacterFields`，完整複製其巢狀 `CombatSkillDerived`，卻只移植 `L1Character` 的 instance fields 與選定方法。五個 `L1AttackList` static 表及 `load()` 因而缺席。此時新產生的損壞 inner class 與正式、候選中的 class bytes 完全相同。

**兩個指定 JAR 均無 `compute` 呼叫者。** 全 JAR 指令、方法 handle 與相關反射字串掃描，所得直接呼叫路徑與間接呼叫路徑均為空集合。原本會呼叫它的技能快照更新方法也未移植。因此目前證據支持「保留於 JAR 的潛伏連結錯誤」。尚無正常登入、習得技能、裝備或 buff 流程可抵達該方法的靜態證據。

**隔離純函式探針已重現錯誤。** 直接對正式與候選的原始 class 呼叫 `compute`，空集合及全 null 元素成功，任一非 null Integer 皆拋出 `NoSuchFieldError: SKU1`。本次掃描的 102 份現有 `.log` 沒有該錯誤或 `CombatSkillDerived` 紀錄。

## 做了什麼

- 先讀 `AGENTS.md`、`docs/REPORT.md`，檢查 `TASKS.md`，核對既有 `task/184` 與乾淨工作狀態。`TASKS.md` 未列 T042，依本 Issue 的指定範圍執行。
- 使用 Microsoft JDK 17.0.17、既有 Maven cache 的 ASM 9.7 及 Python 3.12，讀取兩個 JAR 的全部 class。以 `javap -p -c` 交叉核對相關方法。
- 搜尋已追蹤 Java／Python、`compile-stubs`、本機 `lito脫殼資源` 及伺服器工具。循本機 manifest 追到 construction patch 的凍結 inputs、source、前後 JAR，逐一比對 SHA-256。
- 對照 `D:/lineage開發/1-伺服器/src` 與七份舊參考來源的 `L1AttackList.java`，查明資料內容與載入時機。
- 只讀現存日誌，另在隔離 classloader 內執行六種輸入的函式探針，每個 JAR 各六案。

所有暫存腳本與證據放在本 worktree 的 `scratch/issue-184/`，class 放其 `target/`。調查未執行歷史 build 腳本，未啟動伺服器、遊戲或 DB，未讀 `config/server.properties`，未寫資料表或原始專案目錄。搜尋排除封存、備份及檔案硬連結。

## 已驗證

### 1. 調查標的及二進位身分

以下路徑以 worktree 的 `lito伺服器/LiTo381-Server/` 為基準。

| 標的 | SHA-256 | 掃描 class 數 |
|---|---|---:|
| `reference/base-0.6.71.jar` | `e1498871c7be6b287821c57ef5d95933db1740d9b4d4a80f6c3465f21d99d498` | 2,554 |
| `reference/candidate-50635f7e.jar` | `50635f7edca8ec6ce77787cc32371c7590b497e48ea202f8a679caf3f517b729` | 3,033 |

兩者的以下 entry 各自完全相同。

| `local/lito381/server/` 下 entry | SHA-256 |
|---|---|
| `NativeDeCharacterFields.class` | `0b063983e90d8849378333df1665f8b3bd29c5bd7f3f0b180185a578880d132d` |
| `NativeDeCharacterFields$CombatSkillDerived.class` | `3eb863d893ffe86c5baa780a6c056076a0437bab3267a2f84a764729bf42d0ab` |
| `NativeDeCharacterFields$SkillEffectSnapshot.class` | `8f9047f2df9be09567842568f42a090a8950b9e9a5e78bb2a8be5f3a3a71d31f` |

`NativeDeCharacterFields → NativeDeObjectFields → java.lang.Object` 的欄位宣告中，五表均缺席。`compute` 中五個 `GETSTATIC` owner 都是 `NativeDeCharacterFields`，descriptor 都是 `Ljava/util/HashMap;`。

### 2. 完整呼叫路徑結果與正常流程可達性

目標完整方法為

```text
local/lito381/server/NativeDeCharacterFields$CombatSkillDerived
  #compute(Ljava/lang/Iterable;)Llocal/lito381/server/NativeDeCharacterFields$CombatSkillDerived;
```

| 檢查項 | 正式 | 候選 |
|---|---:|---:|
| 指向 `compute` 的方法呼叫指令 | 0 | 0 |
| 指向 `compute` 的 invokedynamic bootstrap argument method handle | 0 | 0 |
| 指向 `compute` 的 LDC method handle | 0 | 0 |
| `compute`／`CombatSkillDerived` 相關反射字串 LDC | 0 | 0 |
| 直接呼叫者集合 | `[]` | `[]` |
| 沿上述指令圖反向展開的間接呼叫路徑 | `[]` | `[]` |

每個 JAR 中指向 `CombatSkillDerived` 方法的普通呼叫只有兩處，均為六參數建構子。

1. `CombatSkillDerived.compute` 末尾建立回傳值。
2. `SkillEffectSnapshot.<clinit>` 建立六欄全零／false 的 `EMPTY_DERIVED`。

其他相關 handle 是 record 的 `toString`、`hashCode`、`equals` 使用的欄位 getter。`SkillEffectSnapshot.<clinit>` 直接使用 `EMPTY_DERIVED` 建立 `EMPTY`，沒有呼叫 `compute`。

全 JAR 的欄位使用也顯示同一結果。

- `_skillEffect`、`_skillEffectLock` 只在 `NativeDeCharacterFields.<init>(II)V` 寫入新集合及鎖。
- `_skillEffectSnapshot` 只在該建構子寫入 `SkillEffectSnapshot.EMPTY`，後續讀取點為 `hasSkillEffect(int)`。
- `SkillEffectSnapshot.combat` 只在其建構子寫入。
- 兩個 JAR 均未提供原來源的 `publishSkillEffectSnapshotLocked` 或 `updateCombatSkillCache` 呼叫鏈。

因此，指定 JAR 內沒有實際傳入此 `compute` 的 Iterable 來源。角色已習得技能、裝備或 buff 的存在，單獨不足以建立這條呼叫邊。這個結論限於兩個固定 SHA 的 JAR 內靜態呼叫圖，外部插件、動態拼接反射名稱及未提供的載入器行為列為待驗證。

### 3. 原來源應傳入的 Iterable

construction preparation 凍結的 `source/src/com/lineage/server/model/L1Character.java`，SHA 為 `56dad7e55d73924d631d28ee04a56413536f3921be13d732607c412f24148c29`。

其 `publishSkillEffectSnapshotLocked` 位於第 1726 行，流程如下。

```text
setSkillEffect／removeSkillEffect／killSkillEffectTimer
clearAllSkill／clearSkillEffectTimer／updateCombatSkillCache
  → publishSkillEffectSnapshotLocked()
  → Set.copyOf(_skillEffect.keySet())
  → L1AttackList.CombatSkillDerived.compute(activeSkillIds)
  → 寫入派生值及新的 SkillEffectSnapshot
```

凍結檔內呼叫 publication 的行號為 1466、1477、1508、1542、1566、1591、1630、1654、1720。其中 `setSkillEffect` 包含新增與替換兩條路，remove／kill 各含一般及 timer lease overload。

`_skillEffect` 是以技能效果 ID 為 key 的 `ConcurrentHashMap`。成功新增效果時，`setSkillEffect(int, int)` 將 boxed int 寫入 map，因此可產生非 null 元素。料理、龍之護鎧、耐力、聖結界等啟用中的效果可作為輸入。單純習得技能不會直接成為這份集合的 key，裝備須經效果新增流程才會影響此集合。

如果後續移植這條 publication 路徑而沿用目前損壞的 `compute`，第一個非空有效技能快照就會碰到缺欄位。來源只 catch `RuntimeException`，`NoSuchFieldError` 屬於 `Error`，該 catch 無法攔截。

### 4. 五表的真正 owner、資料與載入時機

真正 owner 是 `com.lineage.server.model.L1AttackList`。

| 表 | 宣告行 | 可見性 | 語意 | `put` 次數／最終 key 數 |
|---|---:|---|---|---:|
| SKU1 | 91 | protected static final | 近戰料理命中加成 | 5／5 |
| SKU2 | 94 | protected static final | 遠程料理命中加成 | 5／5 |
| SKD1 | 106 | protected static final | 近戰料理傷害加成 | 5／3 |
| SKD2 | 109 | protected static final | 遠程料理傷害加成 | 5／5 |
| SKD3 | 112 | public static final | 傷害加減值與減半旗標 | 31／31 |

資料來源是 `L1AttackList.load()` 第 242–308 行的 `put` 常數敘述。class 初始化先建立 HashMap，`load()` 再填入內容。技能數字由 `L1SkillId` 常數決定。這五張表沒有對應的 DB 查表或資源檔讀取程序。

現行舊伺服器來源 `GameServer.initialize()` 第 597 行呼叫 `L1AttackList.load()`。同一 load 方法前段還會讀 `SpawnBossTable.get().bossIds()` 填入另一張 `DNNPC` 表，GameServer 第 506 行先載入 SpawnBossTable。修正時應只抽取本次五表所需的固定資料，避免為了五表初始化而引入整個舊伺服器載入鏈。

凍結 `L1AttackList.java` 與本機 `D:/lineage開發/1-伺服器/src/com/lineage/server/model/L1AttackList.java` bytes 相同，SHA 均為 `e78ebc48d4cf566b5382ebbe3f123f0d8dfd6621d72d110140705a288dfa087d`。其 `compute` 位於第 586 行起，逐個 key 查表並加總。

使用現行 `L1SkillId.java` 展開的結果如下。其 SHA 為 `5b3dafdac87f504efdd11f70f207999badd7ae8f35b4370b513d4bdcbd678a14`。

| 表 | 最終 key → value |
|---|---|
| SKU1 | 3016→1、3024→1、3034→2、3042→2、3048→1 |
| SKU2 | 3019→1、3027→1、3032→1、3040→1、3049→1 |
| SKD1 | 3016→1、3024→1、3048→2 |
| SKD2 | 3019→1、3027→1、3032→1、3040→1、3049→2 |
| SKD3 | 3008–3015、3024–3031、3040–3047 各為 −5，3048–3051 各為 −2，181→−10，211→−15，68→68 |

SKD3 中 `value.equals(key)` 表示減半，68 對應聖結界。其餘值加到 `skd3FlatModifier`。`compute` 本身回傳旗標，不在這裡計算最終傷害。

SKD1 原始碼三次寫入 `COOKING_2_0_N`，最終只有三個 key。現有 `BattleCookingCatalog.java` 第 181–192 行已另行註明此來源重複鍵，並對第三級近戰料理採用 +2。後續應明確裁定採「凍結來源精確值」或「既有料理修正規則」，避免未聲明地改變傷害值。

### 5. 引入問題的精確 patch 邊界

以下 `B` 代表原始專案的唯讀目錄

```text
D:/lineage開發/3-lito開發/lito伺服器/LiTo381-Server/tools/resource-database/build
```

| 證據 | 路徑／身分 |
|---|---|
| build 指標 | `B/de-construction-development/build-current.json` |
| 凍結 build manifest | `B/de-construction-development/builds/20261017T010357818224Z/manifest.json` |
| manifest SHA | `303d7e66d9c83b3466eff5b693b1316f3053c6c49b559b95558dd8d63c920a25` |
| preparation | `B/de-construction-development/preparations/20261017T010407655869Z/` |
| 上一層 perception JAR SHA | `d1a261d2c6e1beff45fb9fed4541ed6b79e3b076f42626331eb21694b771c0ee` |
| 本層 construction JAR SHA | `d71a72d00e76c97f51dcce0af6ab9af39adf65162bd4e3c975f68f843aefe27e` |
| builder SHA | `600ed97a63add03c802319b79ab68ea18cca26f97ec099aff0f6d0651ec1f797` |

builder 是 `B/BuildDeConstruction.java`，與上述 build 的 `inputs/BuildDeConstruction.java` bytes 相同。

1. `aliases(String)` 第 7 行將 `model/L1AttackList` 映射成 `character`，將 `L1AttackList$CombatSkillDerived` 映射成 `character+$CombatSkillDerived`。正式使用時 `character` 為 `NativeDeCharacterFields`。
2. `nested(...)` 第 14 行完整搬入 SkillEffectSnapshot、ViewerHpState、CombatSkillDerived。ClassRemapper 同時把 compute 內的五個 field owner 改成 CharacterFields。
3. `patch(...)` 第 15 行只搬 `L1Character` 中 `(access & ACC_STATIC) == 0` 的欄位。它沒有讀取 `L1AttackList.class` 的欄位／clinit／load。
4. 選定方法清單包含 `hasSkillEffect`，不包含 `publishSkillEffectSnapshotLocked`、`updateCombatSkillCache` 或技能增刪方法。因而缺少 compute 的呼叫入口。
5. `prepare-de-construction.py` 第 6–8 行凍結的是 `L1Character`、其兩個 inner class、`L1AttackList$CombatSkillDerived` 的 `.class`，以及兩份 Java 來源。沒有凍結 `L1AttackList.class` 作為移植輸入。

前後 JAR 實證如下。

| entry | perception 前一層 | construction 本層 | 正式與候選 |
|---|---|---|---|
| CombatSkillDerived | 不存在 | SHA `3eb863d8…` | 同一完整 SHA |
| SkillEffectSnapshot | 不存在 | SHA `8f9047f2…` | 同一完整 SHA |
| CharacterFields | 原有較小角色類 | 加入 instance fields 與 64 個選定方法 | 後續外層有其他改動，五表仍缺席 |

因此可以把問題定位到 **construction 層首次引入 inner class 時的依賴遺漏**。無須假設後續某層曾刪除已正確存在的五張表。正式與候選的該 inner class 自 construction 層起保持逐位元一致。

### 6. 搜尋交叉結果與既有驗證缺口

- `compile-stubs` 的九份 Java 沒有五表或 CombatSkillDerived，`L1SkillId` stub 只有拒絕使用的建構子。已追蹤及本機 `lito脫殼資源` Java／Python 搜尋沒有這些名稱。
- 已追蹤 tools 的 `invisibility-leases/build-repro/inputs/source-reference/L1Character.java` 第 1727–1730 行保留完整 `_skillEffect.keySet() → compute` 來源鏈，可供雲端接手者參閱。
- 本機 `BuildDeConstruction.java`、`prepare-de-construction.py`、凍結 inputs 與兩側 JAR 是此次根因的直接證據。工具搜尋以當前來源、sources／inputs 及指定凍結建置為準，重複歷次試跑目錄未逐份窮舉。早期廣域掃描因大量重複建置副本中止，後續採上述定點追溯。
- `VerifyDeConstruction.java` 檢查 102 個 instance fields、64 個選定來源方法、inner methods 的 remap 後一致性。它對 fresh helpers 的外部依賴檢查針對 `MethodInsnNode`，沒有解析 `FieldInsnNode` 是否能在 owner／父鏈找到宣告。相同缺失也存在於產生的 source oracle，故兩邊方法相等仍可通過。
- `DeVisibilityProbe.java` 直接用反射建立 `SkillEffectSnapshot`，使用 `EMPTY_DERIVED`，沒有執行 compute。該類是工具探針，未找到它在指定正式／候選 JAR 裡形成呼叫入口。
- git 提交 `efffc85f62c7b77e41d05cdd4d700bdbf57d3a57` 在反編譯後的 `src/main/NativeDeCharacterFields.java` 加入五個空 HashMap。這是後續編譯修補，未恢復資料填入。#72 的正式行為裁定與後續功能修正仍須分開處理。

舊參考來源亦有同名五表，差異如下。數字是來源中 `put` 敘述次數。

| `D:/lineage開發/1-伺服器參考用資源/` 下來源 | SKU1／SKU2／SKD1／SKD2／SKD3 | 有 CombatSkillDerived |
|---|---|---|
| 381n8src | 4／4／4／4／27 | 否 |
| korsrc | 5／5／5／5／31 | 否 |
| rk760src | 5／5／5／5／32 | 否 |
| 加錢src | 5／5／5／5／32 | 否 |
| 古老src | 5／5／5／5／31 | 是 |
| 群版src | 4／4／4／4／27 | 否 |
| 貓大src | 5／5／5／5／31 | 否 |

不同參考版的列數有差異，修正的資料基準應鎖定此次 construction 凍結來源及主控裁定。未讀取參考資源中的備份目錄。

### 7. 現有日誌查核

以 UTF-8 容錯讀取 `.log`，查找 `NoSuchFieldError`、`CombatSkillDerived` 及五表名稱的錯誤組合。只輸出檔案計數與命中位置，報告不附整份運行日誌。

| 原始專案下範圍 | `.log` 數 | bytes | mtime 日期範圍，UTC | 相關命中 |
|---|---:|---:|---|---:|
| `lito伺服器/LiTo381-Server/logs` | 0 | 0 | 目錄不存在 | 0 |
| `lito伺服器/LiTo381-Server/loginfo` | 6 | 5,014 | 10-12 至 10-18 | 0 |
| `lito登入器/LiTo381-Launcher/app/server`，遞迴且排除 config | 22 | 123,804 | 10-13 至 10-22 | 0 |
| `lito登入器/LiTo381-Launcher/app/runs` | 74 | 2,649,811 | 10-13 至 10-22 | 0 |
| `lito登入器/LiTo381-Launcher/app/gui-runs` | 0 | 0 | 無 `.log` | 0 |

合計 102 份、2,778,629 bytes，讀取錯誤 0，這些日誌中的任意 `NoSuchFieldError` 也為 0。mtime 只代表檔案時間，日誌未逐份證明對應目前正式 SHA。結果僅涵蓋本機上述保留範圍，歷史封存及其他位置列為未驗證。

### 8. 隔離探針結果

探針只使用指定 JAR 的 URLClassLoader，以平台 classloader 為 parent，反射呼叫 public static `compute`。執行前已檢視該方法及相關初始化位元碼。沒有建立角色、呼叫 Main、建立網路連線或載入伺服器設定。這些輸入是明確標示的函式診斷案例。

| 輸入 Iterable | 正式 | 候選 |
|---|---|---|
| `List.of()` | 全零、減半 false | 相同 |
| `Arrays.asList(null, null)` | 全零、減半 false | 相同 |
| `List.of(181)` | `NoSuchFieldError: SKU1` | 相同 |
| `Arrays.asList(null, 68)` | `NoSuchFieldError: SKU1` | 相同 |
| `List.of(-1)` | `NoSuchFieldError: SKU1` | 相同 |
| Iterable 本身為 null | `NullPointerException`，iterator 呼叫處 | 相同 |

`javap` 顯示第一個非 null 元素通過 offset 47 的判斷後，立即在 offset 53 執行 `GETSTATIC SKU1`。是否為表內有效 ID，不影響連結錯誤。後四個缺失欄位因第一個錯誤先發生，本次以靜態解析確認其缺失。

## 推測

- construction 工作聚焦完整 instance 初始化、欄位搬移及 64 個方法，巢狀類採完整複製，可能因此把尚未使用的 compute 一併帶入；這可解釋選擇性搬移的形狀，當時作者意圖未取得確認。
- 現有日誌沒有錯誤，與 JAR 中缺少呼叫入口相符。日誌保存範圍有限，無法單憑零命中推斷歷史上從未被外部工具觸發。

## 建議修正與測試

### 建議修正位置及資料來源

1. 由 Claude 主控先核准功能修正的行為基準，沿用 #72 已裁定的 A 階段界線。此 Issue 已完成根因調查，後續另開 code 任務。
2. 功能修正落在 `src/main/java/local/lito381/server/NativeDeCharacterFields.java` 的資料提供及 `CombatSkillDerived.compute` 依賴；若仍須重建 patch 鏈，同步修正 `BuildDeConstruction.java` 的 static 依賴搬移策略與 `VerifyDeConstruction.java` 的欄位連結驗證。避免只修其中一種建置路徑。
3. 以 SHA 固定的 `L1AttackList.java` 第 242–308 行作為五表內容來源，採 JVM class 初始化或明確且必定先執行的唯讀 catalog 初始化。修正完成的驗收須同時包含宣告與實際非空內容。若保留舊 bytecode，owner、欄位名與 `HashMap` descriptor 必須相符；若改用新 catalog，所有讀取點須一起重編。
4. 若未來要讓 NativeDe 使用技能派生快取，另外完成效果增刪／timer lease、publication 與戰鬥讀取端的接線。需維持鎖與單一快照發佈的一致性，並檢查既有 buff／料理計算是否重複加成。
5. SKD1 重複鍵的規則另列驗收項。可重用現有 `BattleCookingCatalog`／`CombatSourceReductions` 的已核准規則，但須先比對全部五表語意，尤其 `CombatSourceReductions` 受 `CombatRules.loaded()` 影響，不能直接假設與原來源固定表完全等價。

### 必要測試

| 測試層 | 驗收要求 |
|---|---|
| 二進位連結 | 對產物中的 GETSTATIC／PUTSTATIC、相關 handles 解析 owner、父鏈與 descriptor，五表零 unresolved，檢查 static 可見性與初始化 |
| 真實資料 | 五表 key/value 逐列對照凍結來源或核准差異，驗證 key 數 5／5／3／5／31，SKD1 採修正版時另列明確差異 |
| compute | 空集合、全 null、未知 ID、單一料理、近遠程分離、混合 null、重複元素、181、211、68及其組合；例如 `[181,211,68]` 的原來源期望 flat −25、halve true |
| 初始化 | 新 classloader 第一次呼叫已有完整資料，多執行緒首次讀取與重複初始化保持一致 |
| 狀態整合 | 未施放的習得技能、buff 新增／替換／移除／到期／清空、舊 timer lease 回呼、料理替換、相關裝備效果，確認快照及讀取端一致且不重複加成 |
| 建置回歸 | src/main 產物與核准功能基準比對，patch 重建產物也做同等檢查；更新 #72 刻意偏離的處置須經主控裁定 |
| 真機驗收 | 最後由使用者驗收角色登入、施放／料理／到期、進出房與戰鬥數值；本任務未執行 |

## 怎麼驗證與重現

本機診斷檔在 `scratch/issue-184/`，只保留於 worktree，不隨本報告提交。

| 檔案 | 用途 |
|---|---|
| `Inspect.java`／`jar-scan.txt` | ASM 掃描兩個 JAR 的全部方法呼叫、相關 handle／字串／欄位使用 |
| `Probe.java`／`probe.txt` | 原始 JAR 隔離函式探針，12 案 |
| `evidence.py`／`evidence.txt` | 追查 manifest、凍結 builder、前後 JAR 及 entry SHA |
| `javap-*.txt` | 四個階段相關類的反組譯 |
| `source_details.py`／`source-details.txt` | 五表解析與七份舊來源盤點 |
| `search.py` | 排除禁止路徑的來源搜尋，廣域掃描紀錄僅作探索用途 |
| `logs.py`／`log-summary.txt` | 日誌計數、範圍及錯誤字串查核 |

遠端接手者可直接以追蹤中的 reference JAR 執行下列只讀命令，核對 compute 的欄位引用與外層宣告。PowerShell 中含 `$` 的 class 名稱須維持單引號。

```powershell
$jar = 'lito伺服器/LiTo381-Server/reference/base-0.6.71.jar'
javap -p -c -classpath $jar 'local.lito381.server.NativeDeCharacterFields$CombatSkillDerived'
javap -p -c -classpath $jar local.lito381.server.NativeDeCharacterFields
javap -p -c -classpath $jar local.lito381.server.NativeDeObjectFields
# 候選使用 reference/candidate-50635f7e.jar 重複檢查。
```

全 JAR 複驗時，對每個 `.class` 以 ASM `ClassReader.accept(ClassNode, 0)` 讀取所有 methods，列出指向目標的 `MethodInsnNode`、`InvokeDynamicInsnNode.bsmArgs` 中 Handle 及 `LdcInsnNode` 中 Handle／相關 String。再列出 `_skillEffect`／`_skillEffectSnapshot`／五表的 `FieldInsnNode`。直接入邊為零，反向可達集合即為空。此流程不初始化被檢查的 class。

## 待驗證／未驗證什麼

- 外部插件、反射動態名稱、正式環境額外 classpath，以及現有保留範圍之外的日誌。
- 五表與完整技能快照接線後的遊戲行為及最終戰鬥數值。本次完成連結根因及純函式失敗條件驗證，尚未實作功能修正。
- SKD1 凍結來源重複鍵應保留或採既有料理修正版，等待主控裁定。
- 重複歷次 build／replay 副本未全面列舉；指定 construction 凍結輸入、前後產物與正式／候選 entry 的 SHA 關係已核對。

## 產出路徑與給下一棒的提示

唯一提交產出為 `docs/reports/issue-184.md`。

由 Claude 主控／雲端審查接手，依本報告開立功能修正及測試任務。優先帶上 construction builder 第 7、14、15 行與 `3eb863d8…` inner class 完整 SHA，要求同時驗證欄位解析及資料內容。需要原始凍結 inputs 時交給本機執行器，路徑已在本報告固定。使用者接手最後真機驗收。
