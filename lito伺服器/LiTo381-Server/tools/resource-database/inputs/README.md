# 固定建置輸入

`base-0.6.61.jar` 是 0.6.66 增量修補所需的固定核心，SHA-256 為 `9719f45db324206617726ce06d1a9e2184e9a3b3aefab3d50f3ebbdfbf09cfac`。必須保留，不能以現行正式 JAR 替代。`../build.py` 驗證後產生 `../build/candidate.jar`；此目錄不保存建置輸出。

`asm-analysis-9.7.jar` 供唯讀資料流盤點及候選建置時的目標掛鉤分析，不打包進伺服器。來源為 [Maven Central 的 org.ow2.asm:asm-analysis:9.7](https://repo.maven.apache.org/maven2/org/ow2/asm/asm-analysis/9.7/asm-analysis-9.7.jar)，SHA-256 `7bc6bcbc21379948a0c8c467fb0f864206e5b818f6bc0b546872f5c9f941556f`，34,776 bytes；與既有 ASM／ASM Tree 9.7 配套。
