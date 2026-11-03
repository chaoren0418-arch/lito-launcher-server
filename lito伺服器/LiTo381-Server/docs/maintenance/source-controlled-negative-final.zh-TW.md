# 受控NPC狀態技能的最終送包生命週期

候選 `e7701e624f503b7078714efc346c5481e6778acd6dd1ce5ce9e2399fa773bc4a`。本次只更新隔離候選。

受控NPC的173／174狀態技能已補首次來源／主人／actor／接收者證據，沿既有完整送包副本到原cipher監視器。修正實際解碼Ctrl提交後等待送包時，Main／生命替換仍漏出舊特效的缺口；DRAINING與提交後移動保留。首次接收者暫存表在送包等待前清空，佇列僅帶不可變值；沒有新增全域根／工作執行緒或重建生命。

同SHA獨立工具／plan閉包與固定輸入重建通過：124繼承plan共15910檢查＋69退出193；另列NPC最終695斷言／78等待、NPC成對456／60、PC成對312／42。51完整原方法inverse、4既有owner、7接點、1暫存Context欄位、3新runtime class。原PC／Motion sender位元不變。464次元素NPC與464次PC解碼入口仍含型別／layout不適用及正常拒絕，不能宣稱完整技能效果。

29／152／153入口原已接線，錯誤判斷的fallback實驗已封存撤回；接續核對Source目標類別、成本及效果。全部12工作群仍開放；不部署、不寫Source／玩家SQL／schema／WAL。

共同進度：[manifest](D:/lineage開發/3-lito開發/lito伺服器/LiTo381-Server/tools/combat-continuation/integration-lifecycle/build/source-completion-progress/20261027T051235518121Z/manifest.json)。


唯一NPC物件ID重驗：前版場景同時保留舊NPC123與真實元素NPC45306，兩者都為777，造成29／152／153原body先選舊actor並誤報距離；這是測試缺陷，未改生產選取／距離／競技規則。現以四角色各自不同且停用的primary、唯一真實45306重驗，60次成對／456斷言、78次最終等待／695斷言仍通過。464材料入口為64成本觀察、146拒絕、248非NPC封包layout、6無成本回應；116次Main／STOPPED無扣款送包。29／152／153敵方、自有及Ctrl同隊分別保留17／20／10MP和1材料；一般同隊仍拒絕。舊NPC入口統計由本證據取代，完整類別失敗／RNG／狀態／各技能生命週期仍待補齊。 證據：[20261027T051702830790Z](D:/lineage開發/3-lito開發/lito伺服器/LiTo381-Server/tools/combat-continuation/integration-lifecycle/build/source-completion-progress/20261027T051702830790Z/manifest.json)。
