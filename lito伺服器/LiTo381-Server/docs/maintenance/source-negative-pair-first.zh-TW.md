# 受控NPC負面材料技能的首次成對生命週期

候選 `bf4aa2a61d488bd68c2cdaa0d9a7df3221f2e7848443f225c584ea137ddcbaf2`。本次只更新隔離候選。

29／152／153受控NPC施法已補首次來源、主人與NPC的status／life、Hub lifetime與application證據，覆蓋原roster鎖重試、precommit scene與Aim.valid。修正status替換仍扣款及來源status／Hub lifetime消失拋例外；不重建生命，不改原鎖重試／解鎖／producer／成本／免疫／RNG。共用self-pair意圖欄位在擷取後立即還原，新增First僅留在既有Aim並於原finish清空；無新增global root／TLS／queue／worker。

150次一般／Ctrl原主人鎖等待、1878斷言通過：normal／DRAINING／僅主人移動保留原成本；退役、status／life替換、來源／NPC移動、Main／STOPPED拒絕。Source召喚物必定失敗不抽機率、不消除既有Counter／Erase／Sleep或變動移速。69完整原方法inverse、2既有owner、5接點、1暫存Aim欄位、2新runtime class；20唯讀SQL reader位元保留。

獨立工具／plan閉包與固定輸入重建、124繼承plan15910檢查＋69退出193通過；既有NPC最終695／78、NPC成對456／60、PC成對312／42均在同SHA重跑。唯一真實45306場景及464入口統計仍逐項限縮，完整Source／各技能最終接收者與值資料生命週期／其餘12工作群未完成。不部署、不寫Source／玩家SQL／schema／WAL。

共同進度：[manifest](D:/lineage開發/3-lito開發/lito伺服器/LiTo381-Server/tools/combat-continuation/integration-lifecycle/build/source-completion-progress/20261027T053830199131Z/manifest.json)。
