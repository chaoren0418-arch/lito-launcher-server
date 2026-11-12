# Issue #357 — T126 可重現封裝：固定 `project.build.outputTimestamp`

分支：`task/357`

## 做了什麼

1. `lito伺服器/LiTo381-Server/pom.xml`：`<properties>` 加上 `project.build.outputTimestamp = 2026-11-04T00:00:00Z`（0.6.71 發版日）。
   - `maven-jar-plugin` 已是 3.3.0（從 3.2.0 開始支援 outputTimestamp），`jar` 與 `test-jar`（`modern`、`as-record-tests` classifier）三個 execution 都會套用。不需要升級外掛版本。
   - `maven-dependency-plugin` 3.6.0 只把依賴 JAR 複製到 `target/lib/`，不會重新封裝，不影響。
   - AsRecord 後處理（`tools/as-record/.../AsRecord.java:44-45`）用 `Files.walk(root)` 之後的 `.sorted()` 結果，`ScanRecordFields.java:22` 也有排序；沒有用到時間、亂數或 HashMap 迭代順序寫出檔案。
2. `.github/workflows/build.yml`：新增 job `server-reproducible`：
   - 連續跑兩次 `mvn -B -DskipTests clean package`（中間加 `clean`，不讓第二次沿用 `target/`；兩輪之間 sleep 3 秒，若有時間戳外漏一定會出現差異）。
   - 兩輪都把 `target/*.jar` 複製出來，對 `sha256sum` 清單做 `diff`，不同就失敗，並印出差異 JAR 的 `unzip -lv` 對照。
   - JAR 少於 3 個（main／modern／as-record-tests）也失敗，避免空集合也算通過。
   - SHA-256 清單與 commit 以 artifact `server-reproducible-jars` 上傳，給 M4 打包收據核對。
3. `docs/INSTALL.md` §4.1 新增「可重現封裝」小節：做法、CI job、本機核對指令與限制。
4. `TASKS.md`：T126 狀態改成 `review`。

## 怎麼驗證

- **已驗證**（靜態）：`pom.xml` 的 jar 外掛版本是 3.3.0，≥ 3.2.0；AsRecord 的檔案走訪有排序。
- **待驗證（交 CI）**：`server-reproducible` job 兩次建置 SHA-256 相同；`server`、`server-test` 維持綠燈。依本機執行限制，沒有在本機跑 `mvn`（建置途中會執行自己編譯的 AsRecord 工具），結果以本 PR 的 CI 為準，CI 結果會補到 PR 留言。

## 推測

- 同一版 Temurin 17、同一版 Maven 外掛下，javac 產出的位元組是確定的；換 JDK 主版本會改 MANIFEST 的 `Build-Jdk-Spec`，雜湊會跟著變，這是預期行為，不是退化。
- 安裝包 zip（`packaging/Build-Package.ps1`）本身的時間戳與 entry 順序**不在本任務範圍**；zip 內的 JAR 會變得可重現，但 zip 本身的 SHA 可能仍然每次不同。

## 未驗證什麼

- 沒有實機啟動伺服器（不影響執行行為：outputTimestamp 只改 JAR entry 的時間欄位）。
- 沒有比對 Windows 本機建出的 JAR 與 CI（Linux）的 JAR 是否同 SHA；理論上 JDK／外掛版本相同就一致，未實測。

## 產出路徑

- `lito伺服器/LiTo381-Server/pom.xml`
- `.github/workflows/build.yml`（job `server-reproducible`）
- `docs/INSTALL.md`（§4.1「可重現封裝」）
- `TASKS.md`
- `docs/reports/issue-357.md`

## 給下一棒的提示

- M4 打包收據可以直接引用 CI artifact `server-reproducible-jars/reproducible-jars.sha256`。
- 後續可以開一個任務讓 `packaging/Build-Package.ps1` 產出的 zip 也可重現（固定 entry 時間、排序 entry）；本 PR 沒做。
- 發新版時可以順手更新 `project.build.outputTimestamp`；不更新也不影響可重現性。
