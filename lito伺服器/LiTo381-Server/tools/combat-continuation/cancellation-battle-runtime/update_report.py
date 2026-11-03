"""Update the existing concise report only after sealing final same-JAR evidence."""
from pathlib import Path
import json
R=Path(__file__).resolve().parent;S=R.parents[2]
q=json.loads((R/'build/qualification-current.json').read_text('utf8'))
assert q['same_sha_backend_regression_passed'] is True
assert q['runtime']['sha256']=='8d26c9d791e094ab3d3cd6c8dbe4218d99f96fcbe73858be5af534106c81cfb8'
assert q['targeted_summary']['cases']==2400 and q['targeted_summary']['checks']==45156
p=S/'docs/maintenance/combat-continuation-current.zh-TW.md';text=p.read_text('utf8')
rows={
'最新候選':'**`8d26c9d7…`**：同 SHA 通過 2,400 案／45,156 檢查、5,164 案組合及 20＋18 組回歸；SQL 啟動／所屬程序清理、重建一致性通過。另 24 案延遲送包邊界通過。**未發布，原生介面待集中實測**。',
'延續起點':'沿用此候選；新增 BATTLE NPC 接線與清理，既有相消機率、WORLD 替換、四項範圍技能、69／165 不重做。上一個已驗證退回點為 `13118eb9…`。',
'材料進度':'66／76／161／167、69／165 及 BATTLE NPC 相消皆納入本次回歸；靜態掃描仍有三個舊呼叫，包含靜態 NPC 後備與兩個預檢，不能以呼叫數代表完成。',
'本次更新':'BATTLE 普通 NPC type2：先建立與驗證動畫，再移除舊 NPC；期限、死亡保留屍體、關房／關服清理與材料故障回滾通過。正式版與來源資料庫未改動。',
'**相消 NPC 轉換**':'type1 靜態 NPC 名稱／外型／速度投影已有候選，AI 與副本隔離待補。type2 **WORLD＋BATTLE（Quest／Time Attack／GM）** 施法、期限、死亡／房間／關閉清理已接；本輪 276 案替換、12 案材料故障、128 案 NPC 身分與生命週期通過。動畫／模板無效時保留原 NPC 並走一般相消。限已支援的普通 NPC 建構，非完整 NPC factory。[整合收據](../../tools/combat-continuation/cancellation-battle-runtime/build/qualification-current.json)。',
'**範圍／群體技能**':'66／76／161／167 已補材料一次提交、固定目標與鎖外送包；69 含邀請角色綁定、跨圖回覆／失效清理；165 含玩家、PET、WORLD NPC、Quest／Time Attack 屍體及競態。全部在最終 SHA 重播通過，[收據](../../tools/combat-continuation/cancellation-battle-runtime/build/targeted/receipt.json)。其餘技能／目標仍依待辦。',
'其他相消與 NPC／寵物路徑':'WORLD NPC 材料與 PET／SUMMON 已有專項，BATTLE NPC 材料及等待期間換角／死亡／復活／房間重設已納入本輪。玩家相消另重播 296 案機率、材料故障及同盟／非同盟生命週期，[收據](../../tools/combat-continuation/cancellation-battle-runtime/build/player-cancellation/receipt.json)。召喚物轉移／復活競態、毒性調色盤、40／103 與其他別名仍待補。'
}
lines=[];seen=set()
for line in text.splitlines():
 if line.startswith('| 開發中候選 |'):continue
 if line.startswith('| '):
  key=line.split('|')[1].strip()
  if key in rows:line='| '+key+' | '+rows[key]+' |';seen.add(key)
 lines.append(line)
assert seen==set(rows)
text='\n'.join(lines)+'\n'
text=text.replace('../../tools/combat-continuation/call-nature-material/build/material-census/','../../tools/combat-continuation/cancellation-battle-runtime/build/material-census/')
needle='- [正式版 BUFF／選角驗收]'
text=text.replace(needle,'- [候選測試入口](../../tools/combat-continuation/cancellation-battle-runtime/Test-Candidate.bat)：200% 視窗，先核對封存候選與 21 項原生啟動輸入；本輪未自動開啟遊戲。\n'+needle)
p.write_text(text,'utf8')
p=S/'VALIDATION.md';lines=p.read_text('utf8').splitlines();count=0
for i,line in enumerate(lines):
 if line.startswith('| 69／165 整合候選／'):
  lines[i]='| NPC／範圍整合候選／`8d26c9d7…` | 同 SHA：2,400 案專項／45,156 檢查、5,164 案組合、20＋18 組回歸、SQL 啟動／所屬程序清理與重建一致通過；另 24 案延遲送包通過 | 原生介面依使用者安排集中實測；普通 NPC 替換已補，靜態 NPC／特殊核心等餘項見進度表，正式版未替換 |';count+=1
 if line.startswith('完整路徑、SHA 與測試收據見'):
  lines[i]='完整路徑、SHA 與測試收據見[候選索引](tools/combat-continuation/continuation-current.json)。本輪相消機率、WORLD／BATTLE 普通 NPC 替換及四項範圍技能、69／165 已納入同 SHA 自動驗證；其餘場景缺口保留於主要進度表，不能寫成全技能完成。'
assert count==1
p.write_text('\n'.join(lines)+'\n','utf8')
print('Updated existing concise progress and validation reports; no release or SQL changes')
