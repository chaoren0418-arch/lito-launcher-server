"""Refresh concise current evidence; leave historical receipts and deployment intact."""
from pathlib import Path
R=Path(__file__).resolve().parent;S=R.parents[1]
p=S/'docs/maintenance/combat-continuation-current.zh-TW.md';t=p.read_text('utf-8')
t=t.replace('目前執行前層合併重播，尚不計入 45 項已完整回歸專項；未改來源資料庫或正式程式。','4,504 案前層合併重播已通過，材料專項為 46／58；45／58 的完整 20＋18 組結果仍只屬於前層中毒候選。此次重播發現舊中毒消費端 fixture 只等待 HP 變動便檢查非同步送包完成；以相同 3 秒上限等待既有完成佇列清空，前後各 8 案通過，零持鎖送包斷言不變。4,464 個相同候選／編譯輸入／結果雜湊的已通過案件直接沿用，補跑修正 fixture 與後續計時器；原失敗保留。未改來源資料庫或正式程式。')
t=t.replace('建立於尚未完成行為驗證的地面束縛候選上','建立於已完成專項及組合重播的地面束縛候選上')
t=t.replace('不代表實際召回／馴服已驗收。','這兩組僅驗證純計算與設定，實際消費端另列如下。')
t=t.replace('真正項圈封包測試已編譯，尚未執行。','實際項圈召回 98 案／1,265 檢查、馴服 42 案／1,027 檢查、造屍 42 案／1,059 檢查均通過；材料 36／41 的交易及實體生命週期尚未驗收。')
t=t.replace('項圈、馴服及造屍的真實封包測試已編譯，包含只改當前職業加成、缺省騎士邊界及負值拒絕；未執行前不計入效果驗收。','項圈、馴服及造屍的真實封包測試已通過，包含只改當前職業加成、缺省騎士邊界及負值拒絕。測試修正保留原失敗：項圈封包須含結尾空字串；造屍使用實際 SUMMON 類型與可正常關閉的隔離重生排程器，重生期限設 60 秒避免立即補生。這些是 fixture 修正，沒有修改正式封包解析或 NPC 行為。六組測試與父層重播的雜湊核對見 `pet-controls/build/qualification-current.json`；新 SHA 的完整整合與 SQL／介面發布驗證仍開放。')
p.write_text(t,encoding='utf-8')
for rel in ['VALIDATION.md','docs/maintenance/combat-integration-current.md']:
 p=S/rel;t=p.read_text('utf-8');parts=t.split('\n\n',2)
 parts[1]='戰鬥續作（2026-10-19，未發布）：中毒／麻痺候選 `a53513d…` 已通過 45／58 項材料專項、4,072 案組合重播及同 SHA 的 20＋18 組完整回歸。大地屏障候選 `2681d9dc…` 通過專項與 4,504 案組合重播，材料專項 46／58；七職業寵物外控 `4319a8e7…` 的六組定向測試通過，含項圈／馴服／造屍實際封包。後兩者尚未完成最終完整回歸。材料專項不等於全部技能效果同源化；完整目標、來源外控與 SQL／原生介面驗證仍開放。正式維持 0.6.69。'+('[候選、證據與待辦](docs/maintenance/combat-continuation-current.zh-TW.md)。' if rel=='VALIDATION.md' else '[候選、證據與待辦](combat-continuation-current.zh-TW.md)。')
 p.write_text('\n\n'.join(parts),encoding='utf-8')
