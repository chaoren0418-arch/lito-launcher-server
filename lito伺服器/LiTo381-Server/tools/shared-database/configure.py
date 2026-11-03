from pathlib import Path
import re
s=Path(__file__).resolve().parents[2]
p=s/'config/server.properties'
t=p.read_text('utf-8')
assert 'database.persistence.policy=' not in t, 'One-time migration already applied; edit config files directly.'
changes={
'origin.source.name':'LiTo 共用天堂資料（角色唯讀）',
'database.static.database':'380_db',
'database.dynamic.databases':'380_ac,380_ac_1,380_ac_2',
'database.source.weight.rates':'100.0,100.0,100.0',
'login.classic.server.names':'太陽神,月亮神,伺服器3',
'login.classic.server.ids':'1,2,3',
'database.read.only':'true',
'state.file':'../runtime/shared-database-memory/server.state',
'state.persistence.enabled':'false',
}
for k,v in changes.items():
 t,n=re.subn(r'^'+re.escape(k)+r'=.*$',lambda m:k+'='+v,t,flags=re.M)
 assert n==1,k
t=t.replace('LiTo BAT 正式伺服器外控；使用可寫入的獨立 SQL 副本。','LiTo 共用資料庫外控；BAT 與登入器共用此檔。')
t=t.replace('共用物品、技能、NPC、戰鬥規則及 LiTo 賽事紀錄所在的資料庫名稱。','天堂共用物品、技能與 NPC 定義庫，只讀取。')
t=t.replace('角色動態庫清單，以逗號分隔；順序須與 source-servers.properties 對應。','角色庫備援清單；啟用來源以 source-servers.properties 為準，不需同步修改此清單。')
t=t.replace('true 禁止一般角色資料回寫；false 使用可寫流程。目前為獨立 SQL 副本，設定 false。','共用來源必須 true；禁止回寫原角色、背包、帳號、BUFF 等。戰績另由專用流程寫入。')
t=t.replace('額外持久狀態檔；目前沿用已驗證場次的狀態，不能當作完整 SQL 備份。','記憶體模式的保留路徑；持久化關閉時不載入、不寫出舊場次資料。')
t=t.replace('是否保存額外持久狀態；正式基準使用 true。','必須 false：角色快照與場次物品只留在記憶體，避免舊存檔覆蓋來源。')
t+='''
# 共用來源保存策略：只回寫 character_lito 的勝敗場，禁止其他角色資料回寫。
database.persistence.policy=source-readonly-stats-only

# LiTo 專用設定庫（戰鬥規則、物件、怪物、玩家出生點、商店）。
database.lito.database=380_lito

# 每個啟用角色庫皆須建立此戰績表；只保存累計勝敗及版本資訊。
database.lito.character.table=character_lito
'''
p.write_text(t,encoding='utf-8')
lines=['# LiTo 來源選單：預留 8 格，目前開放 3 格。',
       '# 未來開放：在該格填入 database/name，將 enabled 改 true，再重新啟動。',
       '# 新角色庫須建立 character_lito，並授予 LiTo 帳號讀取及該表寫入權限。',
       '# ID 是穩定身份，請勿因停用來源而重編其他 ID。',
       '# 本檔為 BAT 與登入器共同來源，不需修改 server.properties 的備援清單。',
       '# 預留來源格數，範圍 1～8。','source.slots=8','']
for i in range(1,9):
 db=['380_ac','380_ac_1','380_ac_2'][i-1] if i<=3 else ''
 name=['太陽神','月亮神','伺服器3'][i-1] if i<=3 else f'伺服器{i}'
 lines += [f'# 來源 {i}：是否開放於來源選單。',f'source.{i}.enabled='+('true' if i<=3 else 'false'),
           '# 客戶端顯示名稱，可自行修改。',f'source.{i}.name={name}',
           '# 穩定來源 ID。',f'source.{i}.id={i}',
           '# 角色資料庫名稱（不是 .sql 檔名）；停用時可留空。',f'source.{i}.database={db}',
           '# 來源負重倍率，沿用原本 100.0。',f'source.{i}.weight.rate=100.0','']
(s/'config/source-servers.properties').write_text('\n'.join(lines),encoding='utf-8')
print('Configured 8 slots / 3 active sources; source state persistence disabled.')
