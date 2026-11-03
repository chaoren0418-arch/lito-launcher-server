"""Explicit 0.6.63 migration: prepare verified archive, then apply without overwrites."""
from pathlib import Path
import sys,json,hashlib,subprocess,zipfile
R=Path(__file__).resolve().parent;S=R.parents[1];B=R/'build/compact-063';B.mkdir(exist_ok=True)
L=S.parent.parent/'lito登入器/LiTo381-Launcher'
J=Path('C:/Program Files/Microsoft/jdk-17.0.17.10-hotspot/bin')
ADMIN=Path('D:/lineage開發/1-伺服器/config/sql.properties')
CP=str(R/'build/candidate.jar')+';'+str(S/'lib/mysql-connector-j-8.0.33.jar')
ARCHIVE=S/'backups/resource-database-before-0.6.63.zip'
KEEP={'skill_value','buff','projectile','map','zone','portal','npc_identity','map_page','release','pending'}
def sha(b):return hashlib.sha256(b).hexdigest()
def java(mode,*args):
 subprocess.run([str(J/'java.exe'),'-Dfile.encoding=UTF-8','-cp',str(B)+';'+CP,'local.lito381.server.CompactResources',mode,str(S/'config/server.properties'),str(ADMIN),str(B),*map(str,args)],check=True)
def q(v):return 'NULL' if v is None else "CONVERT(0x"+str(v).encode('utf-8').hex()+" USING utf8)" if str(v) else "''"
subprocess.run([str(J/'javac.exe'),'-encoding','UTF-8','-implicit:none','-cp',CP,'-d',str(B),str(R/'CompactResources.java')],check=True)
mode=sys.argv[1]
if mode=='prepare':
 if ARCHIVE.exists():raise SystemExit('Archive exists; refusing overwrite')
 (B/'original-counts.tsv').write_bytes((R/'legacy-counts-062.tsv').read_bytes())
 java('export')
 schemas=[json.loads(x) for x in (B/'schema.jsonl').read_text(encoding='utf-8').splitlines()]
 rows={r['table']:[] for r in schemas};hashes={k:[] for k in rows}
 for line in (B/'data.jsonl').read_text(encoding='utf-8').splitlines():
  r=json.loads(line);rows[r['table']].append(r['values']);h=hashlib.sha256()
  for v in r['values']:
   if v is None:h.update(b'-1:')
   else:
    x=v.encode('utf-8');h.update(str(len(x)).encode()+b':'+x)
  hashes[r['table']].append(h.hexdigest())
 for line in (B/'snapshot.tsv').read_text().splitlines():
  table,n,digest=line.split('\t');assert len(hashes[table])==int(n);assert sha(''.join(sorted(hashes[table])).encode())==digest
 # Restorable exact pre-migration SQL; data literals avoid quote/backslash ambiguities.
 with (B/'restore.sql').open('w',encoding='utf-8') as out:
  out.write('-- Explicit restoration only. Existing tables cause CREATE to fail. Select the intended database first.\nSET NAMES utf8;\n')
  for item in schemas:
   out.write(item['ddl']+';\n')
   for row in rows[item['table']]:out.write('INSERT INTO `'+item['table']+'` VALUES ('+','.join(q(v) for v in row)+');\n')
 with zipfile.ZipFile(ARCHIVE,'x',zipfile.ZIP_DEFLATED) as z:
  for name in ['data.jsonl','schema.jsonl','snapshot.tsv','original-counts.tsv','restore.sql']:z.write(B/name,name)
 with zipfile.ZipFile(ARCHIVE) as z:
  assert z.testzip() is None
  for name in z.namelist():assert sha(z.read(name))==sha((B/name).read_bytes())
 archive_ref='backups/'+ARCHIVE.name
 notes=[]
 def note(category,table,key,status,text,known='',missing='',required=''):
  notes.append([len(notes)+1,category,table,key,status,text,known,missing,required,archive_ref])
 for r in rows['lito_resource_pending']:
  # Original columns: id,item,status,known_evidence,missing_evidence,required_evidence.
  note('pending','lito_resource_pending',r[1],'unresolved',r[2],r[3],r[4],r[5])
 for item in schemas:
  table=item['table'];suffix=table.removeprefix('lito_resource_')
  if suffix in KEEP:continue
  # Original evidence_note follows evidence_status; all original fields remain in archive.
  import re
  columns=re.findall(r'^  `([^`]+)`',item['ddl'],re.M);index=columns.index('evidence_note')
  comments=sorted({r[index] for r in rows[table] if r[index]})
  note('archive',table,None,'archived-reference','此表未由正式程式讀取，已從正式庫移除；原始 '+str(len(rows[table]))+' 列及逐欄證據完整封存。','\n'.join(comments),'各筆未知值、衝突及來源位置見封存資料。','補齊證據並實作及驗證消費端後，才將必要欄位納入正式表。')
 item=next(x for x in schemas if x['table']=='lito_resource_buff');cols=re.findall(r'^  `([^`]+)`',item['ddl'],re.M)
 for values in rows['lito_resource_buff']:
  r=dict(zip(cols,values))
  if r['semantic_conflict']=='1':note('conflict','lito_resource_buff',r['skill_id'],'conflict',r['evidence_note'],'現有兩方來源已保留於封存。','原生 selector／slot／bitmap 與命名資源的語義尚未一致。','需客戶端實際顯示及資源追蹤證據；維持停用 SQL 覆寫。')
 assert len(notes)==22
 (B/'pending-inserts.sql').write_text('\n'.join('INSERT INTO `lito_stage063_pending` VALUES ('+','.join(q(v) for v in row)+');' for row in notes)+'\n',encoding='utf-8')
 result={'archive':str(ARCHIVE),'sha256':sha(ARCHIVE.read_bytes()),'original_tables':24,'retained_tables':10,'unified_notes':len(notes),'archive_rows':sum(map(len,rows.values())),'archive_content_verified':True}
 (B/'archive-verification.json').write_text(json.dumps(result,ensure_ascii=False,indent=2),encoding='utf-8');print(json.dumps(result,ensure_ascii=False))
elif mode=='apply':
 info=json.loads((B/'archive-verification.json').read_text(encoding='utf-8'));assert info['archive_content_verified'];assert str(ARCHIVE)==info['archive']
 java('apply',ARCHIVE,info['sha256'])
else:raise SystemExit('Use prepare or apply')
