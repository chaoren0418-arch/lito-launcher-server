# Native 背景：直接 writer 與一層 caller

已核固定PE，未追任何被呼叫private body。來源SHA、VA/RVA、rawhex與逐指令位置在fixed-source-evidence.json；結論在上一層Native-background-scene-contract.json。

- 64AFA0的兩個具名直接writer：481519／RVA81519與481563／RVA81563寫1。直接字串比對literal分別ltcharsvr@62D234與lteventcsvr@62D228；只有抵達且比對相等才写1。不是Directory／sex語義。固定disk byte0不是live initialized值，未證明其他間接writer不存在。
- 496F28／RVA96F28 call495520：64AFA0零選6314，非零選7482；先push圖片ID，再push7，ECX=[631ECC]／EDX=[631EC8]。既有fixed descriptor pair為630DF8／8；8不是角色容量。直接call無背景x/y參數。
- 496EB0的owner來源75F6E8，先讀[owner+0C]與5比對；無前置null guard。!=5直接選圖；==5先opaque helper，再用631E38／EDX5／push310／push0進495520，return EAX寫回75F6E8。這些數值尚沒有已授權的具名scene/type語義。
- 一層既有S03 handler4A7450：count0且signed[63D54C]<785B、64AFA3=0，於4A74CB呼叫496EB0；return後64AFA3=1。另一caller4AACF0只有64AFBC/64AFBD均0時於4AAD02 tailjmp496EB0。兩個caller的callee與上層dispatch不展開。

可接收的是conditional source predicates；仍不可指定Directory或draw(0,0)。精確停止：缺已接受的numeric owner/caller→scene名稱、495520結果→背景draw座標/transform/mode，以及同版本實際initialized owner/flag生命周期。既有四個portrait child的x/y不是背景位置。Modern／監製接收契約後須保留scene_binding與draw_position未知。

本次不改D25BED57 RGBA manifest，不重新產圖／抽取；0wire，不通知server；42E550/42E6D0/42EA50仍停止。既有外部pyc清理自動審查blocked by policy的事件保持open，未碰或重試。
