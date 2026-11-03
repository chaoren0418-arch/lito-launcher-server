from pathlib import Path
import hashlib,json,re,zipfile
R=Path(__file__).resolve().parent;S=R.parents[1];L=S.parent.parent/'lito登入器/LiTo381-Launcher';B=R/'build'
sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest()
manifest=json.loads((B/'build-manifest.json').read_text('utf-8'))
assert sha(B/'candidate.jar')==manifest['candidate_sha256']
assert sha(S/'src/main/java/local/lito381/server/GmPolyMe.java')==manifest['source_sha256']
for log in ['PolyMeRegression','TravelRegression']:assert 'PASS ' in (B/(log+'.log')).read_text('utf-8')
jar=L/'app/server/LiTo381-SharedResources-candidate.jar'
assert sha(jar)==manifest['base_sha256'],'Formal core changed; rebase instead of overwriting'
changes={jar:(B/'candidate.jar').read_bytes()}
def text(p,t):changes[p]=t.encode('utf-8')
p=L/'app/server/Start-Shared-World.py';t=p.read_text('utf-8');assert manifest['base_sha256'] in t
text(p,t.replace(manifest['base_sha256'],manifest['candidate_sha256']))
p=S/'deployment/release-inputs.json';d=json.loads(p.read_text('utf-8-sig'));d['server_sha256']=manifest['candidate_sha256'];text(p,json.dumps(d,ensure_ascii=False,indent=2)+'\n')
for relative in ['src/LiTo381Launcher.csproj','src/MainForm.cs','tools/build_current.py','README.zh-TW.md']:
 p=L/relative;text(p,p.read_text('utf-8').replace('0.6.70','0.6.71'))
p=S/'README.zh-TW.md';t=p.read_text('utf-8');t=t.replace('**登入器 0.6.70','**登入器 0.6.71')
t=t.replace('`.item`、`.spawn`、`.de`、`.loc`、`.move` 只讀取','`.item`、`.spawn`、`.de`、`.loc`、`.move`、`.polyme` 只讀取')
t+='\n`.polyme 外型代號`：L1PolyMe 自身變身，持續 7200 秒，例如 `.polyme 6080`；使用共用 polymorphs，結算／離房清除，不寫回角色庫。[用法、驗證與限制](docs/maintenance/gm-polyme-0.6.71.md)。\n'
text(p,t)
p=S/'VALIDATION.md';t=p.read_text('utf-8');text(p,t.replace('# 驗收現況','# 驗收現況\n\n2026-10-23：正式 0.6.71 新增 L1PolyMe，沿用 0.6.70 核心；34 項變身與既有傳送回歸通過。[驗證範圍](docs/maintenance/gm-polyme-0.6.71.md)。以下保留各候選當時的驗收界線，不代表所有候選皆已發布。',1))
p=S/'docs/HISTORY.md';t=p.read_text('utf-8');text(p,t.replace('目前正式版本：**0.6.69**','目前正式版本：**0.6.71**',1).replace('最近發布：','最近發布：[0.6.71 L1PolyMe](maintenance/gm-polyme-0.6.71.md)；0.6.70 職業圖示發布收據位於相鄰登入器 docs/maintenance/release-0.6.70.json。\n\n先前發布：',1))
backup=L/'backups/pre-0.6.71-changed-files.zip'
extra=[L/p for p in ['src/development_startup.py','src/DevelopmentStartup.cs','src/FormalStandardSources.cs','app/runtime-pins.json','release/current/LiTo381Launcher.exe','release/current/LiTo381Launcher.dll','release/current/LiTo381Launcher.pdb','release/current/development_startup.py']]
rows=[]
with zipfile.ZipFile(backup,'x',zipfile.ZIP_DEFLATED) as z:
 for i,p in enumerate(dict.fromkeys([*changes,*extra])):
  row={'path':str(p),'sha256':sha(p)}
  if p==jar:row['fixed_input']=str(R/'inputs/base-0.6.70.jar')
  else:row['member']=f'files/{i}';z.writestr(row['member'],p.read_bytes())
  rows.append(row)
 z.writestr('manifest.json',json.dumps(rows,ensure_ascii=False,indent=2))
with zipfile.ZipFile(backup) as z:
 for row in rows:assert hashlib.sha256(z.read(row['member']) if 'member' in row else Path(row['fixed_input']).read_bytes()).hexdigest()==row['sha256']
for p,content in changes.items():p.write_bytes(content)
(B/'publication.json').write_text(json.dumps({'status':'staged-awaiting-validation','version':'0.6.71','jar_sha256':sha(jar),'backup':str(backup),'source_database_modified':False,'lito_database_added_row':'commands/polyme'},ensure_ascii=False,indent=2),'utf-8')
print('Staged 0.6.71; verified compact rollback archive; current server JAR and entry pins updated.')
