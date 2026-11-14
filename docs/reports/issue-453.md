# Issue #453（T155）：登入器資源釘選總盤點；可變資料不再用 SHA 擋（#449 Tile.idx）

## 結論

- 正式 standard 模式與外部入口（`--external-server-profile`）共用同一條檢查鏈：`DevelopmentStartup.CheckAvailability`／`ExternalServerStartup` → `development_startup.check_source` → `app\world_resource_preflight.configure` → native probe。這條鏈上**所有**對 `0-客戶端` 檔案的釘選都已列在下表，並和 #413 基準（`task/413` 的 `docs/reports/issue-413/client-baseline.tsv`，669 檔）與現在的客戶端逐一比對。
- 和現在客戶端不同、會擋啟動的共有 **19 項**：`TW13081901.txt`（#448 已處理）、`Tile.idx`、`Tile.pak` 內的 `effectlist2.xml`、`Sprite00～15.idx`（16 檔）。**全部歸類為 (a) 可變資料**，本任務一次處理完。#449 撞到 `Tile.idx` 之後，下一個會撞到的是 `effectlist2.xml`，再下一個是 `Sprite00.idx`；這三關現在都拿掉了。
- **(b) 必須固定**、而且現在都一致的：`LiTo.bin`、`0-客戶端\litoddraw.dll`、`lito381-compat.ini`（外部入口）、`LiTo\LTicon.idx/.pak`、LiTo 真正載入的 219 份圖示與 2 份 `list.ico` 位元組。保留釘選，沒有放寬。
- **(c) 交主控**：舊 ProtectedClient 介面（`LauncherService` → `LiToCompatibilityResources`）的大小／SHA 表；它不在正式路徑上，但 `LiTo\LTtext.idx/.pak`、`Mss32.dll` 與它的值不同，見下文。
- `common_catalog.bin` 是從**舊版** `TW13081901.txt` 編出來的獨立二進位，換了資源**仍然能用**；**不建議**在啟動時重建（理由見第 5 節）。

## 1. 會用 SHA／大小釘選客戶端檔案的位置（完整清單）

| # | 位置 | 釘的客戶端檔案 | 在正式／外部路徑上？ |
|---|---|---|---|
| 1 | `src/development_startup.py` `BIN_SHA`、`check_source` | `LiTo.bin` | 是（兩者共用） |
| 2 | `src/development_startup.py` `UNPINNED`（#448） | `TW13081901.txt`（只查存在＋可讀） | 是 |
| 3 | `src/development_startup.py` `PINS`（32 筆） | 都是 `app\` 下我方交付檔，**沒有客戶端檔案** | 是 |
| 4 | `app/world_resource_preflight.py` ＋ `resources/world/inputs/effectlist2.xml.extraction.json` | `Tile.idx` 整檔 SHA、`Tile.pak` 內 `effectlist2.xml` 加密內容 SHA | 是 |
| 5 | `app/world_resource_preflight.py` ＋ `resources/world/icon-assets.json` | `index_pins`：`Sprite.idx`、`Sprite00～15.idx` 整檔 SHA；`capacity_candidates`：2 段 `list.ico`；`assets`：161 個圖示、219 段 `Sprite*.pak` 位元組 | 是 |
| 6 | `app/class_icon_bootstrap.py` ＋ `resources/class-icons/resources.json` `original` | `LiTo\LTicon.idx`、`LiTo\LTicon.pak` | 是（native 啟動時） |
| 7 | `app/named_ddraw.py:21`、`app/lito_runtime_probe_named.py:487/6853` | `LiTo.bin`（同 #1） | 是 |
| 8 | `src/DevelopmentStartup.cs:44-46` | `LiTo.bin`（同 #1） | 是 |
| 9 | `src/external_server_startup.py` `CLIENT_DDRAW_SHA`、`verify_client_compat` | `litoddraw.dll`、`lito381-compat.ini` | 只有外部入口（正式模式是自己寫入） |
| 10 | `src/DevelopmentStartup.cs:128`、`development_startup.py:170/293` | `native-probe.shared-ready.json` 的 `profile=tw13081901-v1`、`base_count=30000`、`total_count=30285` | 是；但這三個值來自我方 `common_catalog.bin`，**不是客戶端檔案** |
| 11 | `app/server/Start-Shared-World.py:262/284` | 伺服器啟動參數 `-Dlito.resourceProfile=tw13081901-v1` 與 `catalog-sha256` 日誌 | 是；比對的是 `app\common_catalog.bin`，不是客戶端 |
| 12 | `app/native_daynight_extension.py:7/14` | `D:\lineage開發\登入器bin資源\…\TW13081901.unpacked.exe` | 是；在客戶端目錄**之外**的我方拆解檔，不受客戶端更新影響 |
| 13 | `src/LiToCompatibilityResources.cs`（`ExpectedLengths` 49 筆、`ExpectedHashes` 7 筆）、`src/ClientResourceAudit.cs` | Text/Tile/Sprite*/LT* 等 | **否**：只有 `LauncherService.ValidateAsync` 在舊 ProtectedClient 設定介面呼叫；檢查的是隔離 runtime 目錄。`ClientResourceAudit` 只查存在＋記錄指紋（永遠不擋） |

另外看過、確認**不釘客戶端檔案**的：`shared_bootstrap.py`（只驗 `app\common_catalog.bin` 與 manifest）、`native_*` 擴充模組、litoddraw／相容層原始碼（`native/` 只在註解提到 TW 資源）、`Start-Shared-Formal.py`。歷史建置腳本（`server-patches/repair-03x`、`tools/caption-diagnostic`、`tools/world-resource-compat/candidate_startup.py`）不在執行路徑上，沒動。

## 2. 完整比對表（正式 standard 模式／外部入口）

#413 基準與現值：所有在基準內的檔案，**基準 SHA 與 2026-11-14 現值完全相同**（基準之後客戶端沒有再變），所以下表「現值」同時就是 #413 基準值。比對腳本：`scratch/t453/compare.py`（只讀，結果 `scratch/t453/compare.json`）。

| 檔案 | 釘選來源 | 釘選值 | 現值（＝#413 基準） | 結果 | 分類 | 處理 |
|---|---|---|---|---|---|---|
| `LiTo.bin` | #1／#7／#8 | `CE6EE68ADA8FD37E6C096B1A27E46CB6888F1179F85DEFB5007AC60287360292` | 同左 | 一致 | (b) | 保留 |
| `TW13081901.txt` | #2（#448 前的舊 PINS） | `7EBE2B464C8B1EA9C58AC985DD8FF4C823784D8231894262EDDCD7691CA94008` | `28C4FC600833FB8C609AB195A67FF926E8FFA571100D8D4B389A23BE8C2C8DB8` | **不同** | (a) | #448 已改為存在＋可讀，本任務沿用 |
| `Tile.idx` | #4 `archiveIndexSha256` | `D35A7E3B5B9D4BB432E7B8DB3FF5D9B0A79EB638672C5E025B900F9DE53B0BE6` | `9291FD800AC8CAE6DC355C1D8BBB965880561DE169FBC7EA22D174FFF9AD24DB` | **不同**（#449 擋在這裡） | (a) | 改為格式檢查（見第 4 節） |
| `Tile.pak` 內 `effectlist2.xml` | #4 `encodedSha256` | `6DE9938C07D28EF8BFC6F8AE61A1AAC786D64604574E4E44AA71C84F9395BF55`（92,814 bytes） | `0DAB4E39B2CC53A01B2D430C4CD58CC1636788DFEE28475FFAF6A8C63F0A1D89`（93,214 bytes） | **不同** | (a) | 改為解碼＋只比相容層用到的 95 筆 |
| `Sprite.idx` | #5 `index_pins` | `327C06372D87F6CF6D5176DA51DA5EF1F94DBF388D1588B735C5965B96B6A52B` | 同左 | 一致 | (a) | 一併改為格式＋定位檢查（同類檔，同規則） |
| `Sprite00.idx` | #5 | `AB8DCF18C2A63DDE97ABBFB23C7E9E4F35EFD0F1E2AB5ACF9F53CDED6F5F9817` | `15B46AE6995B5B3D64A27A509C78847F0329BA55360A200961A7ECB73B13C96C` | **不同** | (a) | 格式＋定位 |
| `Sprite01.idx` | #5 | `465274C542A7373AF97B1B4FC7C906827293FA000D9F0EA09546BB527E693D54` | `1E4C819DEBE37449691142025692AC1FF1D1F82C148830C76338E7FDFF8CDD8F` | **不同** | (a) | 同上 |
| `Sprite02.idx` | #5 | `BCAE6BC833E2A761554D660848496342AFE6B512716984710C52CEAF5CE0C7CE` | `A0208077CFA7E76280641F1A2BF7F7AEACC43B6F9A16E21771EBD732E791AE5C` | **不同** | (a) | 同上 |
| `Sprite03.idx` | #5 | `DCB4DA9F78507F580F753A420EC7B666131B1F3806F7CA6BC5928C8BA23F151E` | `BB26979CF21F7655B3F5D57E2C0D152F8CA7E755AD66F1801A1EDB455245CD72` | **不同** | (a) | 同上 |
| `Sprite04.idx` | #5 | `8C8E7D220DDBDE1F2FEEC6800A16AFB5900D3B90014C11450DDDDEC8ECA0C667` | `7C801F623F68BDCA50271DBDDDC2FEFBCAC990E9E9EF60776F48DAA73F9CD1F5` | **不同** | (a) | 同上 |
| `Sprite05.idx` | #5 | `DDCBDF73A075EB73DC87A01267F196002D2CC319AE00264961AE6EFB02698CEB` | `659B4CD84092E9DE5B76B0FFA4793B562213B42BCF0A9771EB0E85E6AA5E379B` | **不同** | (a) | 同上 |
| `Sprite06.idx` | #5 | `F102D2B343D1E471714853D1C1391A4BD610A3FA1B066340CDB80C882C31C1E2` | `45AA43F0EC1A1480E82C27379BD25B433F960F27160B87BF5E0DFE9F67D831E4` | **不同** | (a) | 同上 |
| `Sprite07.idx` | #5 | `79F844A872761014CFC74109FBCED128F29232941CCF23857D2AC01793A23AB6` | `C49468722B1265864CF66F169C09D1318F0DECD4817AF9F15CD8EBB67F6C98AB` | **不同** | (a) | 同上 |
| `Sprite08.idx` | #5 | `E5867DA3438190A77B283EC45A427819BEE1526D1071046FFD6BB8E375EBF961` | `4EB34A830BBAE9D3C3CE112D83A2CC3DD96D0312BFA798ED59542F403924C63C` | **不同** | (a) | 同上 |
| `Sprite09.idx` | #5 | `0C446E0A839911B02EB4ADECCCE3BCDB2BCEB39A0E39F9A40EC5914803CE73DB` | `CC8B83FE4010EE5A3FA470466A414931BD2E61308972D76C610ADA8EA1C04896` | **不同** | (a) | 同上 |
| `Sprite10.idx` | #5 | `90E10DCD775A475FCCB5C989BE842A3E38DEDE810333649EBCCBAAADECA55C57` | `7B4688CF2B01C7889A5E6EF8F06309C52C8AAD781537BFF92D23ED543E08FB8D` | **不同** | (a) | 同上 |
| `Sprite11.idx` | #5 | `C470F356D5C0B09166962F5B302317C667973B9D206CDE73BCC5598DE0463AEB` | `A190E3E9E54753D89B2AE1209910AD053E6BE1D857E895F01631E6DA9C7DD88E` | **不同** | (a) | 同上 |
| `Sprite12.idx` | #5 | `F529DA765CF96B6354FE5A8CB927D3A1610FAC265865C6B7245E97EEEE6D96A0` | `2C271EF696788BA1F44ACAF0A8FE93E4ABAE5C88A57D6103E3CCB84C4B319541` | **不同** | (a) | 同上 |
| `Sprite13.idx` | #5 | `803960DEF65A3B1713515C9350C9C60A90CB03888DD18DFA8F61C2272D6F9251` | `333D0670A4E2FE47C51B62F4F60DE2A579B4B7BC5B9F0910BB2A7F79502C671B` | **不同** | (a) | 同上 |
| `Sprite14.idx` | #5 | `D5D6A491280CE37A30790AA113768A4E0ADAFE6DD0533FBB3536031FB048187F` | `51802133F6BE2BB7A8E727C58F5DA299B9C67C707506E2931FF63B1A4D4633CF` | **不同** | (a) | 同上 |
| `Sprite15.idx` | #5 | `E4CE732730C62C820B6CABFCD818193CEAD90723823527996BC4F77E021725D0` | `874B53AFE762C43415E25204F4E8FCF405F36ADFACA6B9258BD84C1EB1D4C441` | **不同** | (a) | 同上 |
| `Sprite05.pak` @338589270 +7（`list.ico`） | #5 `capacity_candidates` | `F10F86317A3E7BA4915F0DD53E8F9CADB491885834942ADFF6B3073FCAD5FF6B` | 同左 | 一致 | (b) | 保留位元組 SHA，另加「`list.ico` 位置集合不變」 |
| `Sprite.pak` @527081327 +6（`list.ico`） | #5 `capacity_candidates` | `65E3609956A1EBE1755FE97000D3BD341674C0C94101FA761C2BE15E199E469D` | 同左 | 一致 | (b) | 同上 |
| `Sprite*.pak` 219 段圖示位元組（161 個圖示） | #5 `assets[].candidates` | 219 個 SHA（`icon-assets.json`） | 219／219 相同 | 一致 | (b) | 保留，另加「每個圖示在 17 個索引中的位置集合不變」 |
| `LiTo\LTicon.idx` | #6 | `2EA2E48A61C246A5D4CB2820C4C3431F487705C67459290A998D9DBE9017BAA7` | 同左 | 一致 | (b) | 保留（LiTo 專用檔，不是 TW 共用資料） |
| `LiTo\LTicon.pak` | #6 | `52FEC7B547B63B75BE1178D66E4C0E17EA5B48F089B47E1836EDD120C79C015D` | 同左 | 一致 | (b) | 保留 |
| `litoddraw.dll` | #9 | `E47BD6C6EC9979C8747A523E4457133802D19F673C8DC1E67707ABFA96216A8A` | 同左 | 一致 | (b) | 保留 |
| `lito381-compat.ini` | #9（依縮放產生的預期內容） | `ScalePercent=<所選縮放>` 的固定 9 行 | `…ScalePercent=200…`（`E8D601C1…25FD`） | 選 200% 時一致 | (b) | 保留（設定檔，不是資源） |

## 3. 不同檔案的分類理由

- **`Tile.idx`、`Sprite00～15.idx`（以及同類的 `Sprite.idx`）→ (a)**：這些是 TW 共用客戶端的封包索引，客戶端更新時會依規則重排、追加；LiTo 不讀索引本身的位元組，只依賴「我們用到的那幾個 member 在哪裡、內容是什麼」。實測：17 個 Sprite 索引全部是合法 28-byte 格式、每筆都在 `.pak` 範圍內；161 個狀態圖示在 17 個索引中的 (archive, offset, size) 集合**和 `icon-assets.json` 完全相同**（0 個搬移、0 個被其他封包遮蔽）；`list.ico` 只有 `Sprite.idx`、`Sprite05.idx` 兩筆，也和 `capacity_candidates` 相同。也就是索引內容變了，但 LiTo 實際載入的東西一個都沒變。
- **`Tile.pak` 內 `effectlist2.xml` → (a)**：用 PakViewer 唯讀解出（`pakviewer-cli pak extract …\Tile.idx effectlist2.xml`），依 PakViewer `XmlCracker` 的規則（AES-128-CBC）解碼後比對：新版 540 筆、舊版 538 筆，差異只有 **效果 517 多了 `graphic="517"`、新增 538／539 兩筆**。`shared_status.bin` 與相容層 DLL 只吃效果 0～75 與 19 筆擴充（82、91、92、107、110、116、118、120、121、123、126、128、129、130、512～516），這 95 筆的所有欄位、以及「第一個使用同一 graphic 的效果」都沒變；用 `catalog.compile_catalog` 的規則逐列比對，`shared_status.bin` 95 列圖示 0 個不符、擴充列身分 0 個不符、`PacketGraphics` 表相同。所以這是依規則更新的資料檔，換新後我方產物仍然有效。
- **`TW13081901.txt` → (a)**：使用者 2026-11-14 在 #447 裁定，#448 已處理。

沒有任何「現在不同」的檔案落在 (b)。

## 4. 修改內容

### `app/world_resource_preflight.py`（正式與外部入口都透過 `check_source` → `configure` 呼叫）

- **移除**：`Tile.idx` 整檔 SHA（`receipt["archiveIndexSha256"]`）、`effectlist2.xml` 加密內容 SHA（`receipt["encodedSha256"]`）、17 個 Sprite 索引整檔 SHA（`icon-assets.json` `index_pins[].sha256`）。收據與 `icon-assets.json` 本身沒改（仍在 `PINS` 裡），這些欄位只當來源紀錄。
- **新增** `read_index()`：索引長度必須是 `4 + 28×筆數`、每筆 `offset+size` ≤ `.pak` 大小（格式可讀）。
- **新增** `verify_effectlist()`：`effectlist2.xml` 在 `Tile.idx` 恰好一筆、≤ 2 MB → `decode_archive_xml()`（純 Python AES-128-CBC，不需第三方套件，與 PakViewer 規則相同；本機實測 93 KB 解碼 0.52 秒，結果與 `cryptography` 套件逐位元組相同）→ `parse_effects()`（與 `catalog.parse_effects` 相同的格式要求）→ `consumed_effects_sha256()` 必須等於 `CONSUMED_EFFECTS_SHA256 = 5b3e6bb09379a65f457e7d0087102dd2d149c30c9797b74059a038eabe7ca8e0`（由編 `shared_status.bin` 用的 `tools/world-resource-compat/build/inputs/effectlist2.xml` 算出；現客戶端算出來也是這個值）。這 95 筆一變就拒絕，訊息要求重建 `shared_status.bin`／相容層。
- **改寫** `verify_icon_routing()`：保留 219 段圖示、2 段 `list.ico` 的位元組 SHA；另加「每個圖示與 `list.ico` 在 17 個索引中的位置集合必須和收據相同」，所以圖示被搬走或被別的封包同名檔遮蔽仍會擋下（原本的整檔 SHA 隱含這個保證，現在改成明確檢查）。
- 本機以這個新版本**唯讀**跑 `verify_resource_sources()` 對真實 `0-客戶端`：通過，1.05 秒（`scratch/t453/verify_preflight.py --full`；沒有呼叫 `configure`、沒有寫任何檔）。

### 指紋連鎖更新

| 位置 | 新值 |
|---|---|
| `src/development_startup.py` `PINS[app\world_resource_preflight.py]`、`app/runtime-pins.json` | `D8AB4990D1AA40AC366B47DC9CE90D96351B2ED518B56BBE18BE3BE0C4B83EEA`（舊 `353C362E…9F34`） |
| `src/DevelopmentStartup.cs` `EntrySha`、`src/external_server_startup.py` `FORMAL_HELPER_SHA` | `E30E88C3485F12A97F1D59C74205D6DD40D5014DA7779C1574B6B251A2BB0402`（舊 `56532A2F…8189`） |
| `src/ExternalServerStartup.cs` `HelperSha` | `BDCE29438DBABA7BAF498841870023D5E3A8493D491C23EC555ABD8D89B7C3D6`（舊 `BCB58854…F3DD`） |

`development_startup.py` 另加 2 行註解（`UNPINNED` 下方）說明同規則；`PINS` 仍是 32 筆，其餘值不變。換行維持原狀（`development_startup.py` CRLF、`world_resource_preflight.py`／`external_server_startup.py` LF）。

### 測試與文件

- 新增 `lito登入器/LiTo381-Launcher/tests/test_world_resource_preflight.py`（20 個測試，純 Python、只用暫存目錄），並在 `.github/workflows/build.yml` 的 Launcher job 加一步執行：
  - AES：FIPS-197 標準向量；一段用 `cryptography` 加密、尾段不足 16 bytes 的樣本能還原；
  - 真實常數：repo 內 `build/inputs/effectlist2.xml` 算出的指紋等於 `CONSUMED_EFFECTS_SHA256`；
  - 索引：格式正確可讀、長度不符拒絕、member 超出 `.pak` 拒絕；
  - `effectlist2.xml`：其他 Tile member 增加＋效果 517 加 graphic＋追加新列 → **通過**；效果 5／75／107／516 改 icon → 拒絕；前面未使用的效果搶走已使用的 graphic → 拒絕；亂碼、筆數不符、缺 member、重複 member → 拒絕；
  - 圖示：索引追加無關 member → **通過**；圖示搬位置、同名遮蔽、位元組改變、`list.ico` 改變或多一份 → 拒絕；
  - 收據裡的 `archiveIndexSha256`／`encodedSha256`／`index_pins.sha256` 寫成無效值，`verify_resource_sources()` 仍通過；
  - `PINS` 與 `runtime-pins.json` 的 preflight 指紋等於檔案實際 SHA。
- `docs/acceptance/launcher-external-server.md`：客戶端檔案表加 3 列、新增「共用索引與 `effectlist2.xml` 不釘選整檔（T155）」一節、更新兩個 helper SHA、常見失敗訊息表加 3 列。

## 5. `common_catalog.bin` 換資源後還能不能用

- **產生依據**（已驗證）：`app/common_catalog_manifest.json` 記錄 `source_sha256 = 7ebe2b46…4008`（舊版 `TW13081901.txt`）與 `lito_source_sha256 = f55779ae…2603`；`tools/world-resource-compat/rebuild_animation.py` 用封存的編譯器把 TXT 編成 30,000＋285 筆動作表。它**不讀** `Tile`／`Sprite` 索引或 `effectlist2.xml`。
- **還能用**（已驗證＋推測）：`shared_bootstrap.install` 只驗 `common_catalog.bin` 自己的長度／SHA 與 manifest，再整塊寫進 native 記憶體；執行期不再讀客戶端 TXT。伺服器也以同一份 catalog 的 SHA（`Start-Shared-World.py:284`）對帳。所以換了客戶端資源，catalog 仍是一致、可載入的。代價是新版 TXT 多出的 30 個 sprite 定義不在 LiTo 共用目錄裡（#447 裁定接受）。
- **不建議在啟動時重建**：重建需要封存編譯器（`docs/history/development-evidence.zip`）、會改變 catalog SHA，而 catalog SHA 被 `PINS`、伺服器啟動檢查、`native-layout-patches.json`／manifest 的 metadata 鏈住；啟動時重建等於每次都產出未驗證的新 catalog，正式伺服器也會拒絕。若要收進新 sprite，應另開「重產 catalog＋重新驗證＋發版」任務。

## 6. (c) 交主控：舊 ProtectedClient 介面的釘選（不在正式路徑）

`LiToCompatibilityResources.cs` 只有在舊設定介面（非偏好設定 UI 的 `LauncherService.ValidateAsync`，`LaunchMode.ProtectedClient`）才會跑，檢查的是**隔離 runtime 目錄**，不是 `0-客戶端`；對 `0-客戶端` 只做「世代檢測」，結果永遠是通過（`LiToCompatibilityResources.cs:109-114`）。對照現在的 `0-客戶端`：

| 檔案 | 釘選 | 現在 `0-客戶端` | 說明 |
|---|---|---|---|
| `LiTo.bin` | SHA `CE6EE68A…0292`、大小 1,941,568 | 相同 | — |
| `Mss32.dll` | SHA `24EA6B88E5DBCF9488A1D4E4ADBF5D13A6F7C90705608E9735BA1D2009E5EEE3`、372,736 | `F79D29952AC2778F0589D109BA7021F2A7E4646245378F07F65FF8B0E620B007`（373,136） | **(c)**：音效 DLL，LiTo.bin 會載入；正式路徑沒有檢查，也沒報過問題 |
| `litolauncher-c.pak` | SHA `CB29F318…BB79`、2,175,088 | 不存在 | 舊介面用 |
| `Text.idx` | SHA `B23C985C…EA82`、1,044,152 | `43A342AD3929B541096630165C04794192C48359ECBF116B538EAFFC49741F6F`（1,049,948） | 2008 世代值，隔離副本用 |
| `Tile.idx` | SHA `C6C1E741…D4E2`、114,860 | `9291FD800AC8CAE6DC355C1D8BBB965880561DE169FBC7EA22D174FFF9AD24DB`（152,492） | 同上 |
| `Text.pak`、`Tile.pak`、`Sprite00～15.idx/.pak` | 2008 世代大小 | 全部不同 | 同上 |
| `LiTo\LTtext.idx` | SHA `412AF669F06DF26CB1590E80E458D448ED7A304F3917911D94CF3BC2DA676F77`、10,056 | `B8C0BB8C43AFFE59A90EBD2D26182269B6D60AF388C80963EAAEEDC8D8AE23C8`（9,888） | **(c)**：舊介面說它要含「天堂小幫手領獎」`pc_action`／`send_s_0～4` 覆蓋層；`0-客戶端` 的版本不同，正式路徑也沒檢查 |
| `LiTo\LTtext.pak` | SHA `2A7CD3DD3838FBCBC6E93F99F26B5CFA04685787671562F447C2D61D81E939C1`、295,287 | `517D853E3FE91B504953FF4B9B2EED682A3612D063F2D18E75E39FD5580F342D`（292,553） | **(c)**：同上 |
| `Sprite.idx/.pak`、`LiTo\LTsprite/LTsurf/LTicon/LTtile` | 大小 | 相同 | — |

建議：舊介面若已不用，主控可決定移除或標示停用；`LTtext` 是否需要領獎覆蓋層要另查（與本任務的啟動阻擋無關）。

## 已驗證

- 盤點：上表 #1～#13 的每一處都讀過原始碼；比對值由 `scratch/t453/compare.py` 唯讀計算（客戶端整檔 SHA、`.pak` 位元組區段 SHA、索引解析），與 #413 基準一致。
- `effectlist2.xml` 差異：PakViewer CLI 唯讀解出 → 解碼（純 Python 與 `cryptography` 結果相同）→ 只差效果 517 的 `graphic` 與新增 538／539；95 筆被使用的列、`shared_status.bin` 圖示列、擴充身分、`PacketGraphics` 都相同（`scratch/t453/effectlist_diff.py`、`effectlist-diff.json`）。
- 圖示定位：161 個圖示、2 份 `list.ico` 在新索引中的位置集合與收據相同；219＋2 段位元組 SHA 相同。
- 新版 `verify_resource_sources()` 對真實客戶端唯讀通過（1.05 秒）。
- `dotnet build …LiTo381Launcher.csproj -c Release`：成功，0 警告 0 錯誤；輸出目錄 `development_startup.py` = `E30E88C3…0402`、`external_server_startup.py` = `BDCE2943…C3D6`，與 C# 常數一致。
- `python -m py_compile`：四個 .py 通過。
- 依本機規定**沒有在本機跑單元測試**，交 PR 的 CI。

## 推測

- 以本分支建置的登入器（正式 standard 模式或 `--external-server-profile`），在目前客戶端下 `check_source` 會整段通過，往下走到 `litoddraw.dll`／ini 核對與端點檢查；#449 的 08 不會再被資源預檢擋下。
- 新增的效果 538／539 與效果 517 的 graphic 不在 LiTo 相容層的處理範圍，對 LiTo 伺服器送出的狀態沒有影響（LiTo 伺服器不送這些新效果）。
- **已部署的 0.6.71 `release/current` 仍是舊 helper**：它本來就會在 `TW13081901.txt`／`Tile.idx` 擋下；本 PR 合併、`app\world_resource_preflight.py` 更新後，它改成在 `Startup file changed: …world_resource_preflight.py` 擋下。兩種情況都不能用目前客戶端啟動，要恢復正式版需要發新版（和 #448 一樣，未裁定）。

## 待驗證

- CI：PR 的「Launcher (.NET 10 / WinForms)」job（`dotnet test`、`test_external_server_startup.py`、新增的 `test_world_resource_preflight.py`）。
- 實機（交 Codex／使用者）：以本分支建置的登入器重測 #414 項 08（登入器 → 大廳），確認不再出現 `shared Tile index changed`／`shared effectlist2.xml changed`／`shared icon index changed`，並確認狀態圖示（含魔法娃娃、龍之護鎧）顯示正常。

## 產出路徑

- 程式：`lito登入器/LiTo381-Launcher/app/world_resource_preflight.py`、`app/runtime-pins.json`、`src/development_startup.py`、`src/external_server_startup.py`、`src/DevelopmentStartup.cs`、`src/ExternalServerStartup.cs`
- 測試：`lito登入器/LiTo381-Launcher/tests/test_world_resource_preflight.py`、`.github/workflows/build.yml`（Launcher job 加一步）
- 文件：`docs/acceptance/launcher-external-server.md`、本報告
- 分析腳本（不進 git）：`scratch/t453/compare.py`、`effectlist_diff.py`、`verify_preflight.py`、`make_fixture.py`
- 分支：`task/453`

## 給下一棒的提示

- 主控：CI 綠燈合併後，開 #414 項 08 重測（`runner: codex-local`、`stage: test`），helper SHA 以 `docs/acceptance/launcher-external-server.md` 為準（`E30E88C3…`／`BDCE2943…`）。
- 以後客戶端再更新：只有「LiTo 實際用到的東西」變了才會擋——`effectlist2.xml` 那 95 筆（要重建 `shared_status.bin`＋相容層）、狀態圖示／`list.ico` 位置或位元組（要重新驗證圖示）、`LiTo.bin`、`litoddraw.dll`、`LTicon`。訊息與處理已寫進驗收文件的失敗訊息表。
- 改 `app\world_resource_preflight.py` 要同步：`development_startup.PINS` 與 `app\runtime-pins.json` → `DevelopmentStartup.EntrySha`＋`external_server_startup.FORMAL_HELPER_SHA` → `ExternalServerStartup.HelperSha`；`test_world_resource_preflight.PinTableTests` 會抓不一致。
- 第 6 節的 (c) 項（`LTtext` 領獎覆蓋層、`Mss32.dll`、舊 ProtectedClient 介面去留）需要主控決定是否另開任務。
