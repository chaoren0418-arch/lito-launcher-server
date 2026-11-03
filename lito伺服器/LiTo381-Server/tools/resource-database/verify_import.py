"""Verify compact runtime values against the pre-migration archive; export ten-table SQL."""
from pathlib import Path
import json,re,hashlib,subprocess,zipfile
R=Path(__file__).resolve().parent;S=R.parents[1];B=R/'build';A=S/'backups/resource-database-before-0.6.63.zip'
info=json.loads((B/'compact-063/archive-verification.json').read_text(encoding='utf-8'));assert hashlib.sha256(A.read_bytes()).hexdigest()==info['sha256']
keep={'skill_value','buff','projectile','map','zone','portal','npc_identity','map_page','release'}
with zipfile.ZipFile(A) as z:
 schemas=[json.loads(x) for x in z.read('schema.jsonl').decode().splitlines()];rows={}
 for line in z.read('data.jsonl').decode().splitlines():
  r=json.loads(line);rows.setdefault(r['table'],[]).append(r['values'])
lines=[]
for item in schemas:
 table=item['table']
 if table.removeprefix('lito_resource_') not in keep:continue
 columns=re.findall(r'^  `([^`]+)`',item['ddl'],re.M);fields=[f for f in columns if f not in {'evidence_note','note'}];indices=[columns.index(f) for f in fields];hashes=[]
 for row in rows[table]:
  h=hashlib.sha256()
  for i in indices:
   v=row[i]
   if v is None:h.update(b'-1:')
   else:
    b=v.encode();h.update(str(len(b)).encode()+b':'+b)
  hashes.append(h.hexdigest())
 lines.append('\t'.join([table,','.join(fields),str(len(hashes)),hashlib.sha256(''.join(sorted(hashes)).encode()).hexdigest()]))
expected=B/'compact-expected-content.tsv';expected.write_text('\n'.join(lines)+'\n',encoding='utf-8')
j=Path('C:/Program Files/Microsoft/jdk-17.0.17.10-hotspot/bin');cp=str(B/'candidate.jar')+';'+str(S/'lib/mysql-connector-j-8.0.33.jar')
subprocess.run([str(j/'javac.exe'),'-encoding','UTF-8','-implicit:none','-cp',cp,'-d',str(B/'tests'),*[str(R/n) for n in ['VerifyImported.java','CompactResources.java','VerifyCompact.java']]],check=True)
with (B/'compact-verification.log').open('w',encoding='utf-8') as f:
 for cls,args in [('VerifyImported',[str(expected)]),('VerifyCompact',[str(S/'db/lito_resource_compact.sql')])]:
  subprocess.run([str(j/'java.exe'),'-Dfile.encoding=UTF-8','-cp',str(B/'tests')+';'+cp,'local.lito381.server.'+cls,str(S/'config/server.properties'),*args],stdout=f,stderr=subprocess.STDOUT,check=True)
print((B/'compact-verification.log').read_text(encoding='utf-8'))
