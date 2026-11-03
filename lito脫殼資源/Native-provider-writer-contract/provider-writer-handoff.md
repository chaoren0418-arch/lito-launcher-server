# Batch38：provider 直接 writer 與停止點

已核同一fn_LoadGameData_Check reachablepost-endpinitializer。Fallback：42E550收到literal `Sprite`，返回Xdefault寫647984；42E6D0收到Xdefault，返回Hdefault在VA42ED4B/RVA2ED4B寫64798C。兩cache/write均可能先寫0再走failure return，不授live有效provider。

Bucket：ESI從0，`Sprite%02d` formatter參數由r給出；42E550的X_r返回寫6479A8[r]，42E6D0(argX_r)返回H_r在VA42EDF7/RVA2EDF7寫6479E8[r]。同一loop尾incESI／cmp16／JL42ED98，domain0..15；沒有每個H_r緊接nonnull驗收的來源保證。

一筆具體source繫結：已封存79.png request→hashroute3，因此構造輸入為Sprite03，X_3位置6479B4、H_3位置6479F4；fallback為Sprite。這是qualifiedprovider construction-stem參數依賴，尚不是provider→實際Sprite03.pak/idx或Sprite.pak/idx檔案身分。

必要靜態string僅Sprite(5055CC)、Sprite%02d(505DC8)，已核fixedPEbytes。42E550／42E6D0就是第一個新的privatefactory邊界，body未開；allocator/base42E4F0、formatterbody、42EA50reader、decoder/renderer均未開。Fixeddisk647984與16個6479A8 sourceobject cells為0，不能提供initializedheap/fileidentity。

直接reset xref只留42EF48的EBXzero-definition、42F01F與42F0C1的EBX store statements；不擴cleanup生命週期、不宣稱liveclear/callee保持暫存器或全域indirectwriterabsence。

既有sealedmanifest確有shared Sprite與Sprite03同stem候選，可見existing-matching-stem-candidates.json的既有path／SHA。沒有新讀那些index/PAK內容、未把matchingbasename當factory的實際開檔path／extension／casepolicy，未猜LTsurf；更沒有memberoffset/length，所以不抽取。

此分支明確停止：剩一筆獨立qualifiedfactory/provider→實際backingfile/path/root/extension/SHA綁定資料。來源成立後，仍需memberoffset/length/encoding/blobSHA與尺寸才可抽取。terminal-provider-identity-gap.json列精確需求；不再自動逐層開privatefactory／reader，不因忙碌重包unknown。

Classifier、bankinitializer、五index/803filename搜尋與oldPASS均不重做。Batch37歷史原件不改；R527只讀不打斷，0Modernedit/build/test、Native/process/GUI、newruntime/art/assets/wire/servernotification。Blockedpyccleanup不碰／不重試。這是有限source writer交接，不是完整assetbinding或GUI產品驗收。
