# Issue #45 — T020f 候選 JAR 與凍結原始碼入庫

## 本次重驗（2026-11-04）

**資源驗收通過。** 本次開始時，既有入庫提交 `523110e` 與報告補充提交 `c49a91c` 已在 `task/45`，工作樹乾淨，遠端同為 `c49a91cabd6092202dd6d08209d187c927870cd1`。沿用既有材料，沒有重新複製或切換分支；本次只更新此報告。

重新執行 `py -3 -B -X utf8 scratch/issue-45/prepare.py`（不帶 `--write`）、`py -3 -B -X utf8 scratch/issue-45/verify.py --original` 與 `git -c core.whitespace=cr-at-eol diff b6dc4e6 HEAD --check`，全部 exit 0。來源與保存 JAR 的 SHA256 符合指定值；380 筆原始來源／保存來源／候選 class SHA 全數吻合；217 個原始路徑對應 207 份保存 Java；212 個交付檔皆未忽略、皆小於 100,000,000 bytes，最大 22,722,734 bytes。下方獨立核對程式仍可供雲端重驗。

已核對 #43 遠端分支為 `c10b7a8bd10ea7caa7343434f42277e890b40457`，其設計已改為 B01–B09 在 `staged/` 分批移植、最後 B-land 一次落地，故更新下方交接說明。本次未重新檢查或執行外部調度器，前次閘門診斷僅作歷史證據；目前閘門結果待調度器重跑確認。

## 前次紀錄：合併閘門誤判（2026-11-04）

**資源驗收通過；調度器閘門仍待修正（`needs:orchestrator`）。** 本次沿用 `task/45`，沒有切換或建立分支。前次提交 `523110e3961d061539e548a15282090ea5ef9ca1` 已包含根 `.gitignore` 第 91 行的 `!lito伺服器/LiTo381-Server/reference/candidate-50635f7e.jar`；無需再改忽略規則或重新複製材料。本次僅更新本報告。

### 已驗證的退回原因

唯讀檢視 `D:/dev-orchestrator/orchestrator/gate.py`（SHA256 `c4886b908535aef447e0043ca8706fa709e279abac0985a79262c97c221c177e`）：

- 第 13 行固定讀取 `contents/.gitignore?ref=main`，沒有使用待審分支的規則。
- 第 20 行直接跳過 `!` 開頭的例外；第 26–43 行以自訂 `fnmatch` 取第一個匹配規則，不具 Git 的完整忽略語意。
- 第 68–72 行據此將新增候選 JAR 判為 `*.jar`。即使改讀本任務分支，跳過否定規則仍會誤判。

已以 AST 單獨取出 `_ignored` 函式，輸入依上述篩選邏輯取得的實際 `HEAD:.gitignore`，重現回傳 `*.jar`；同一工作樹以 Git 原生命令檢查，結果為未忽略。未匯入或執行整個調度器、未寫入其程式／設定／狀態。

### 本次核對指令與結果

在 `D:/lineage開發/3-lito開發/scratch/wt/task-45` 執行：

```powershell
py -3 -B -X utf8 scratch/issue-45/prepare.py
py -3 -B -X utf8 scratch/issue-45/verify.py --original
git check-ignore --no-index -- lito伺服器/LiTo381-Server/reference/candidate-50635f7e.jar
# exit 1，無輸出：未忽略
git check-ignore --no-index -v -- lito伺服器/LiTo381-Server/reference/candidate-50635f7e.jar
# .gitignore:91:!lito伺服器/LiTo381-Server/reference/candidate-50635f7e.jar
# verbose 模式 exit 0 表示找到匹配規則；此處為 ! 例外，不能當作被忽略。
```

`prepare.py` 未帶 `--write`，只核對原始 current 收據、建置 manifest、來源 JAR 與來源 Java；沒有覆寫材料。兩支腳本均 exit 0：JAR SHA 符合指定值，380 筆 manifest／原始來源／保存來源／候選 class SHA 全數通過，207 份 Java 與 217 個原始路徑吻合，212 個交付檔均未忽略且小於 100,000,000 bytes，最大檔為 JAR（22,722,734 bytes）。修改報告前亦逐一核對前次提交的 212 個 Git blob 與工作檔，全部位元組相同。

雲端可使用下方「可重現的獨立核對程式」重驗材料，不需要本機原始資源。閘門維護者應以待審 commit 的 checkout 執行 `git check-ignore --no-index`，保留 Git 的否定規則及子目錄規則語意，再重跑 #45；本報告沒有宣稱外部閘門已通過。

## 做了什麼

在既有 `task/45` 工作樹備妥第二階段合併輸入材料。候選 JAR 由原專案的 `source-negative-final-frame-build-current.json` 指向的固定輸出複製，複製前後 SHA256 均為：

```text
50635f7edca8ec6ce77787cc32371c7590b497e48ea202f8a679caf3f517b729
```

來源清單固定為 #30 分支提交 `31b5233e4d54dfd29d687616121b12628a0a75dc` 的 `docs/combat-merge/class-diff.json`，僅處理 `candidate_5063_vs_8d26` 中 `source_kind=java` 的 380 筆。設計依據為 #43 提交 `73d0a9b44a32416af8657947728e12f88838ebf3` 的 `docs/design/T020-combat-merge.md` §6-3。

manifest 每筆保留 `class_entry`、原路徑 `source`、`source_sha256`、`candidate_sha256`，另加 `stored_source` 指向此次保存的實際檔案。原路徑相對於 manifest 的 `original_root`；新路徑相對於 manifest 所在目錄。

## 同名來源的處理

380 筆類別對應 217 個原始路徑、202 個 Java 檔名群組。相同套件／檔名且 SHA 相同者共用一份檔案，原路徑仍逐筆保留。實際保存 **207 份 Java：201 份主版本＋6 份歷史變體**。

- 有外層 `.class` 的 Java 列時，套件根路徑保存該列 `source` 指定的來源；沒有改用 `source_candidates` 中的其他檔案。
- `NativeCreateZombieLifecycle`、`NativeSourceLightningLifetime`、`NativeSummonRetirement`、`NativeTamedWorldLifecycle` 的部分內部類別指向另一個 SHA，額外保存於 `sources/local/lito381/server/_variants/<來源完整 SHA256>/<原檔名>.java`。
- `NativeSourceNpcDamageDisplay.class` 本身是 `asm-only`，不在這 380 筆中；其 `$Proof` 與 `$Receipt` 各指向不同 Java 來源。兩份均放 `_variants`，不指定外層主版本。

這個配置避免不同內容的同名來源互相覆寫，且讓全部 380 筆都能逐筆核對存檔 SHA。下游應使用 `stored_source`，不要遞迴把 `_variants` 全部加入編譯。新增目錄內的 `.gitattributes` 以 `sources/** -text` 保存凍結來源原始位元組，避免 checkout 的換行轉換改變雜湊。

## 已驗證／怎麼驗證

- 已讀根目錄 `AGENTS.md`、`docs/REPORT.md`、`TASKS.md`、伺服器規則與其指定的維護文件。
- current 收據中的 JAR 路徑及 SHA 與 #30 完全一致；另核對收據指定的建置 manifest SHA。
- 複製前後 JAR SHA 一致，檔案大小 **22,722,734 bytes**。
- manifest **380 筆**，class_entry 唯一；來源路徑、來源 SHA、候選 class SHA 與 #30 逐筆一致。
- 原專案的來源檔、此次保存的來源檔以及 JAR 中的 class bytes 全部通過 SHA 核對。讀取前排除來源檔／祖先路徑的 reparse point、來源硬連結與封存／backup 路徑；複製以寫入新檔完成，沒有建立硬連結。
- 全部 **212 個提交檔案**均未被 `git check-ignore --no-index` 忽略；每個檔案小於 100,000,000 bytes，最大者為上述候選 JAR。
- Git 暫存區的 212 個 blob 與工作樹檔案位元組逐一相同，包含全部 207 份 Java。
- `git -c core.whitespace=cr-at-eol diff --cached --check` 通過（exit 0、無輸出）；提交範圍只含本任務指定路徑。預設檢查會把凍結來源既有 CRLF 的 CR 視為行尾空白，因此本次只在檢查命令辨識 CRLF，不修改來源或 repo 設定。

本機執行指令（於 repo 根目錄，暫存腳本不入庫）：

```powershell
py -3 -B -X utf8 scratch/issue-45/prepare.py --write
py -3 -B -X utf8 scratch/issue-45/verify.py --original --index
git -c core.whitespace=cr-at-eol diff --cached --check
```

第一條在複製前先完成所有雜湊檢查；拒絕覆寫已存在的材料。第二條的結果如下：

```text
PASS 380 manifest/source/class SHA; 217 original paths; 201 canonical + 6 variant Java files
PASS 212 files not ignored; all files < 100 MB; largest = 22722734 bytes
PASS 380 original source SHA
PASS staged paths and all 212 Git blob bytes match working files
```

### 可重現的獨立核對程式

雲端可先 `git fetch origin task/30` 取得上列固定提交；將下列程式存為 `scratch/issue-45/verify.py`，在 repo 根目錄執行 `python scratch/issue-45/verify.py`。不加參數即可只依 repo 內材料核對，無需原專案資源。`--original` 僅適用本機原始資源仍存在時；`--index` 僅適用本任務 212 檔全數暫存、尚未 commit 時。

```python
import hashlib,json,pathlib,subprocess,sys,zipfile
root=pathlib.Path.cwd()
server=pathlib.Path('lito伺服器/LiTo381-Server')
bundle=server/'merge-inputs/50635f7e'
m=json.loads((bundle/'manifest.json').read_text(encoding='utf-8'))
sha=lambda b:hashlib.sha256(b).hexdigest()
pin=m['class_diff']
raw=subprocess.check_output(['git','show',pin['commit']+':'+pin['path']])
assert sha(raw)==pin['sha256']
d=json.loads(raw)
rows={r['class_entry']:r for c in d['comparisons'] if c['id']=='candidate_5063_vs_8d26' for r in c['rows'] if r.get('source_kind')=='java'}
entries=m['entries']
assert len(entries)==m['entry_count']==len(rows)==380
assert {e['class_entry'] for e in entries}==set(rows)
jar=pathlib.Path(m['candidate']['path'])
assert sha(jar.read_bytes())=='50635f7edca8ec6ce77787cc32371c7590b497e48ea202f8a679caf3f517b729'==m['candidate']['sha256']
assert jar.stat().st_size==m['candidate']['size_bytes']
with zipfile.ZipFile(jar) as z:
    for e in entries:
        r=rows[e['class_entry']]
        for k in ['source','source_sha256','candidate_sha256']:
            assert e[k]==r[k],(e['class_entry'],k)
        p=(bundle/e['stored_source']).resolve()
        assert p.is_relative_to((bundle/'sources').resolve())
        assert sha(p.read_bytes())==e['source_sha256'],p
        assert sha(z.read(e['class_entry']))==e['candidate_sha256']
        if '--original' in sys.argv:
            original=pathlib.Path(m['original_root'])/e['source']
            assert original.stat().st_nlink==1
            assert sha(original.read_bytes())==e['source_sha256']
actual={p.as_posix() for p in (bundle/'sources').rglob('*.java')}
assert actual=={(bundle/e['stored_source']).as_posix() for e in entries}
assert len(actual)==m['stored_source_count']==207
assert sum('/_variants/' not in p for p in actual)==m['canonical_source_count']==201
assert len({e['source'] for e in entries})==m['unique_original_source_count']==217
files=[pathlib.Path('.gitignore'),pathlib.Path('docs/reports/issue-45.md'),jar,*sorted(bundle.rglob('*'))]
files=[p for p in files if p.is_file()]
assert len(files)==212,len(files)
assert all(p.stat().st_nlink==1 for p in files)
assert all(p.stat().st_size<100_000_000 for p in files)
paths=b''.join(p.as_posix().encode('utf-8')+b'\0' for p in files)
ignored=subprocess.run(['git','check-ignore','--no-index','-z','--stdin'],input=paths,capture_output=True)
assert ignored.returncode==1 and not ignored.stdout,(ignored.returncode,ignored.stdout,ignored.stderr)
if '--index' in sys.argv:
    staged=set(subprocess.check_output(['git','diff','--cached','--name-only','-z']).decode('utf-8').strip('\0').split('\0'))
    assert staged=={p.as_posix() for p in files},staged
    for p in files:
        blob=subprocess.check_output(['git','show',':'+p.as_posix()])
        assert sha(blob)==sha(p.read_bytes()),('index byte mismatch',p)
print('PASS 380 manifest/source/class SHA; 217 original paths; 201 canonical + 6 variant Java files')
print('PASS 212 files not ignored; all files < 100 MB; largest =',max(p.stat().st_size for p in files),'bytes')
if '--original' in sys.argv: print('PASS 380 original source SHA')
if '--index' in sys.argv: print('PASS staged paths and all 212 Git blob bytes match working files')
```

## 推測

無以推測代替已驗證結果。來源雜湊與候選 class 雜湊分別吻合，不代表已證明重新編譯所得 class 與候選等價。

## 未驗證什麼

未重編譯 Java、未執行 Maven／候選回歸／語意比對，也未做遊戲實機驗收；本任務只交付輸入材料。未改 `src/main`、`pom.xml`、正式 JAR、機密設定或 `docs/REPORT.md`，未部署或啟停 DB／伺服器，未存取客戶端資源與封存內容。

## 產出路徑

- `lito伺服器/LiTo381-Server/reference/candidate-50635f7e.jar`
- `lito伺服器/LiTo381-Server/merge-inputs/50635f7e/manifest.json`
- `lito伺服器/LiTo381-Server/merge-inputs/50635f7e/sources/`（207 份 Java）
- `lito伺服器/LiTo381-Server/merge-inputs/50635f7e/.gitattributes`
- 根 `.gitignore`（新增候選 JAR 的單條例外）
- `docs/reports/issue-45.md`

## 給下一棒的提示

由調度器重跑 #45 閘門；若仍出現前次記錄的誤判，由調度器維護者（`needs:orchestrator`）確認待審分支選擇與 `!` 例外判定。本資源分支無需移除 `*.jar` 防護，也不應改名候選檔或刪除材料來規避檢查。

由雲端主控／Claude 審查本資源提交，合併後交 Cursor 依 #43 的 `docs/combat-merge/batches.json` 執行 B01–B09：先在 `merge-inputs/50635f7e/staged/` 移植，各批以候選 JAR 為 classpath 獨立編譯與語意核對，最後由 B-land 一次搬入 `src/main`。搬移 Java 時依 manifest 的 `stored_source` 定位，主版本僅供移植起點；歷史變體與 asm-only 外層仍需依 T020d 與候選 JAR 做語意核對。不要把整個 merge-inputs 目錄加入 Maven 編譯，也不要將輸入候選當成已通過驗收的正式成品。
