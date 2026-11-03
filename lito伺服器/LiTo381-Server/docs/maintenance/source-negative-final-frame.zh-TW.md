# 受控NPC負面材料技能的不可變最終送包

候選 `50635f7edca8ec6ce77787cc32371c7590b497e48ea202f8a679caf3f517b729`，只更新隔離程式。

29／152／153受控NPC已將首次來源／主人／NPC／接收者不可變值接入既有完整PC／Motion sender副本，在原cipher監視器重驗status容器、observer、Hub lifetime及application。修正舊29／152觀察者封包與153自接收的退役漏洞；已提交自身MP／HP／方向更新不被後續目標退役誤丟，非自身呈現仍保留來源／NPC位置與placement檢查。normal／DRAINING／僅主人移動保留，Main／STOPPED及替換身分拒絕；不改Source成本、免疫、RNG或競技／Ctrl。

完整原producer lambda副本保留所有呼叫／分支／handler；scoped BATTLE不再將Aim強參照放入finish callback，原finally關閉Aim、清空首次接收者表並釋放全部主人receipt後才等待IO。queued record不含Aim／Context／Frame／receipt／Member／Scope／Task／collection；無新global root／TLS／queue／worker。34完整原方法inverse、3既有owner、7接點＋1producer handle、1完整副本、1暫存Aim欄位、3新runtime class；原完整PC／Motion sender及20唯讀SQL reader位元不變。

300次最終等待通過：29／152一般／Ctrl、玩家／觀察者240次／2280斷言，153原扣款後自接收一般／Ctrl60次／480斷言；3組清理故障17斷言保留原primary、suppressed及全部pending／receipt釋放。獨立工具／plan閉包與固定輸入同位元重建、16103普通繼承檢查、150次首次等待1878斷言及原NPC／PC矩陣均在同SHA重跑。

全部58材料完整效果、PC／自然NPC／其他範圍與Source路徑、正式隔離合成、後端、中文MySQL與實機仍未完成；保留12工作群。不部署、不寫Source／玩家SQL／schema／WAL。

共同進度：[manifest](D:/lineage開發/3-lito開發/lito伺服器/LiTo381-Server/tools/combat-continuation/integration-lifecycle/build/source-completion-progress/20261027T062017367034Z/manifest.json)。
