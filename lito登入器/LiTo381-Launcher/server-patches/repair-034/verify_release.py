from pathlib import Path
import json,hashlib,zipfile
r=Path(__file__).resolve().parent;w=r.parent;p=w/'publish-0.6.34'
sha=lambda f:hashlib.sha256(f.read_bytes()).hexdigest()
pins=json.loads((w/'launcher-build-inputs-0.6.34.json').read_text(encoding='utf-8'))
for name,digest in pins.items():assert sha(Path(name)).upper()==digest,name
assert sha(r/'candidate.jar')==sha(w/'server-gm-034/LiTo381-SharedResources-candidate.jar')
assert sha(p/'development_startup.py')==sha(w/'launcher-src-0.6.34/development_startup.py')
assert '"-Dlito.extendedItemDetails=true"' in (w/'launcher-src-0.6.34/FormalStandardSources.cs').read_text(encoding='utf-8')
with zipfile.ZipFile(w/'repair-033/candidate.jar') as old,zipfile.ZipFile(r/'candidate.jar') as new:
    changed=[n for n in old.namelist() if old.read(n)!=new.read(n)]
    assert sorted(changed)==sorted('local/lito381/server/'+n+'.class' for n in ['ClientSession','GameHub','BattleTimeAttackEncounter']),changed
    assert set(new.namelist())-set(old.namelist())=={'local/lito381/server/LiToGmCommands.class','local/lito381/server/LiToGmNpcs.class'}
assert 'PASS:' in (r/'gm-regression.log').read_text(encoding='utf-8')
formal=json.loads((w/'server-gm-034/formal-timing-test.json').read_text());assert formal['accepted']
existing=json.loads((r/'existing-test-result.json').read_text());assert existing['new_failures']==0
report={'version':'0.6.34','base':'0.6.33','artifacts':{str(f):sha(f) for f in [p/'LiTo381Launcher.exe',p/'development_startup.py',r/'candidate.jar']},'changed_classes':changed,'new_helpers':['LiToGmCommands','LiToGmNpcs'],'sql_policy':'isolated lito_play_20261011_static.commands; item/spawn access_level=200; original source and account privileges untouched','tests':existing,'formal_sql_startup':formal,'new_regression':'real SQL definitions and GM authorization, native chat entry, encrypted output, exact stack/name/enchant, persistent state reload, lobby provenance, AI, room isolation, NPC healing/haste/damage/death/reset','manual_game_test':False,'scope_limits':['spawn: L1Monster in ordinary arena modes only; max100 room actors; range0 uses nearby3 tiles','Quest/TimeAttack/PetRace scripted encounters and non-monster service NPCs explicitly refused','NPC combat behavior uses existing LiTo NPC engine, not every Source1 NPC special script','file reload tested; new GM SQL grant uses existing SQL-first tournament reward path; new-command SQL write roundtrip not separately exercised']}
(r/'release-0.6.34.json').write_text(json.dumps(report,ensure_ascii=False,indent=2),encoding='utf-8')
instructions='''LiTo 0.6.34 GM 指令版

請關閉舊版遊戲與登入器後，使用桌面「LiTo 0.6.34 GM指令版」啟動。
遊戲目標維持 D:\\lineage開發\\0-客戶端\\LiTo.bin。0.6.33 保留可回復。

權限：選定伺服器帳號 accounts.access_level 需 >= commands.access_level；這兩條預設200。
只在獨立 SQL 副本新增 item、spawn 指令設定，未提升任何帳號權限。

.item 道具ID或名稱 [數量=1] [強化值=0]
  .item 40743 100       建立100支箭
  .item 88889 1 7       建立1個+7 GM戒指1
  .item 治癒藥水 10     可使用資料表中的完整名稱（名稱中的空白省略）
  非堆疊道具每次最多10個；強化值 -128～127；堆疊道具強化固定0。
  驗證負重、格數及數量上限；成功後立即發布背包封包並保存。
  使用獨立於商店購買的永久獎勵來源標記，返回大廳不會被當成商店物品清空。
  重名請改用ID。未知ID、參數錯誤、權限不足均有畫面提示。

.spawn NPC_ID或名稱 [數量=1] [範圍=0]
  .spawn 45161 1 3      在附近建立1個史巴托
  .spawn 史巴托 3 5     在5格範圍建立最多3個史巴托
  目前開放一般戰鬥地圖中的 L1Monster 怪物，每次/每房最多100個（房間上限含屍體）。
  範圍0使用周圍3格；可指定0～20。附近空地不足會減少數量並提示。
  原有闖關、競速及寵物賽固定流程，以及商人/守衛/特殊功能NPC，本版明確拒絕。
  NPC使用SQL的ID、外觀、等級、HP/MP；接入LiTo NPC AI及戰鬥/狀態/屍體流程。
  不新增固定重生點；本場結束清除。GM怪物死亡不會觸發競速完成或其他波次。

參考來源：1-伺服器的 GMCommands、L1CreateItem、L1SpawnCmd。
驗證：新增指令回歸通過，正式SQL啟動通過；既有201項測試199通過，2項舊護甲封包期待值在0.6.33亦相同失敗，未新增失敗。
尚待人工驗收：新版本實際聊天操作、物品圖示/數量、NPC移動及戰鬥畫面。
'''
(r/'GM指令說明.txt').write_text(instructions,encoding='utf-8-sig');(p/'GM指令說明.txt').write_text(instructions,encoding='utf-8-sig')
print('PASS: 0.6.34 pins, isolated deployment, narrow class diff, command regression, baseline comparison, SQL formal readiness.')
