# 核准的刻意偏離

只有 `deviations.txt` 的精確類別、方法與描述子可使用。這是交付決策的紀錄，不代表正式版本來就有相同行為，也不代表單元測試已完成。比較報表另列 `DEVIATION`；其餘方法與屬性仍驗證。

## NativeDeWorldLog.debug

識別：`local/lito381/server/NativeDeWorldLog#debug(Ljava/lang/String;Ljava/lang/Object;Ljava/lang/Object;)V`。

- **與正式版的差異**：正式介面缺少這個方法；A-land 交付版補上 `default` 實作，記錄 debug log。本任務只建立比較規則與清單，不修改介面來源。
- **理由**：依 #73／#153 主控裁定，正式 `NativeDeItemGreaterTierTable` 在選用欄位讀取失敗時會呼叫此方法，因正式介面沒有它而可能拋出 `NoSuchMethodError`。核准交付版將此路徑改為只記錄 log。
- **單元測試要求（A-land，待執行）**：模擬選用欄位讀取失敗，確認呼叫 default debug、保留適當訊息與參數，不拋出 `NoSuchMethodError` 或其他例外；另驗證正常讀取路徑不多記錯誤、必要欄位失敗仍走原有失敗處理。
- **比較界線**：只在方法存在左側、有效比較目標缺少時豁免；兩側皆存在仍比較內容，整個介面缺失不豁免。
- **核准**：[Issue #153](https://github.com/chaoren0418-arch/lito-launcher-server/issues/153)，依 #73。

## NativeDeInstanceState 的建構子欄位賦值順序

下列五個精確建構子使用 `ctor-prologue-order`。類別共同前綴為 `local/lito381/server/`，完整描述子以清單為準；以下 C、T、N 分別指 `NativeDeInstancePorts$Construction`、`NativeDeNpcTemplate`、`NativeDeInitializerKernel$Name`。

| 建構子參數 | 與正式版的差異 | A-land 單元測試要求（待執行） |
|---|---|---|
| `(C,T)` | 允許 `this.construction` 的欄位賦值由 super 前移至後 | 核對建構完成後 construction 相同；覆寫父鏈可觀測的回呼，確認不依賴提前賦值 |
| `(C,T,N)` | 同上；不允許移動 `NativeDeInstanceNames.name` | 同上，加 Name 解析成功與失敗，確認失敗時父建構子執行順序 |
| `(C,T,N,boolean)` | 同上 | 分別測 true/false，核對名稱及初始化旗標；確認 Name 失敗順序 |
| `(C,T,N,boolean,String)` | 同上 | 覆蓋字串有效、空值及既有邊界；確認 Name 呼叫順序與父鏈觀測 |
| `(C,T,N,boolean,boolean,String)` | 同上 | 覆蓋兩布林的組合，核對 construction、名稱及父鏈副作用順序 |

**理由**：Java 建構子語法無法在非 Object 的 super 呼叫前直接賦值自身欄位；主控核准有限的交付偏離。父類 `NativeDeNpcConstructionState` 的建構子具有副作用，不能將這項偏離稱為無條件語意等價。

**比較界線**：只有自有欄位接收已計算好的區域值或簡單常數的賦值可移動；其他指令保留。方向為左側在 super 後、右側在 super 前；反向不豁免。`NativeDeInstanceNames.name(...)` 的呼叫位置不在例外內，四個含 Name 建構子的現有差異仍須來源端處理。未核准的建構子不套用此規則。

**核准**：[#72 主控留言](https://github.com/chaoren0418-arch/lito-launcher-server/issues/72#issuecomment-6646156935)。

## 驗證狀態

- **已驗證**：比較器 fixture 涵蓋左側額外方法／清單外方法／兩側同名內容不同、核准建構子賦值前後移動／反向／額外呼叫／Name 呼叫位置改變。
- **推測**：以上來源端修補可滿足交付需求；比較器結果不證明執行期父鏈回呼不會觀察到差異。
- **待驗證**：本文件列出的 A-land 執行期單元測試，以及介面 default 實作與五個建構子的最終來源。

## #72：Name 建構子的 Java 17 委派表達

依[同一核准留言第 2 項](https://github.com/chaoren0418-arch/lito-launcher-server/issues/72#issuecomment-6646156935)，追加一個 private 建構子：

```text
local/lito381/server/NativeDeInstanceState#<init>(Llocal/lito381/server/NativeDeInstancePorts$Construction;Llocal/lito381/server/NativeDeInitializerKernel$Name;Llocal/lito381/server/NativeDeNpcTemplate;)V
```

四個公開 Name overload 先執行 `this(construction, NativeDeInstanceNames.name(construction, name), nativeDeNpcTemplate)`。private 建構子執行 super、寫入 construction 與共用初始化，公開 overload 再寫入各自旗標並呼叫 startNpc。Name 失敗時不會進入 private／父建構子。

新增建構子以一般 left-only deviation 列管；原五個 `ctor-prologue-order` 不擴大。新版比較器尚不展開建構子委派，因此四個公開 overload 仍為 DIFF，不代表已通過驗收。完整建構子鏈、正式 JAR 的子類清單（0 個）、虛擬方法解析與剩餘差異見 `docs/reports/issue-72.md`。A-land #78 仍須執行核准留言第 4 項兩個測試。
