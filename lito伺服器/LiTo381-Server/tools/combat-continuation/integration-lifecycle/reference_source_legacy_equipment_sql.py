"""Compare the actual five archived SELECT recipes against independent canonical/renamed in-memory schemas.
Only owned SQLite memory fixtures are written. No Source/player DB connection, WAL, Hub or game class initialization.
"""
from pathlib import Path
from datetime import datetime,timezone
import json,hashlib,shutil,sqlite3
from contextlib import closing
R=Path(__file__).resolve().parent;D=R/'build';read=lambda p:json.loads(p.read_text('utf-8'));sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest();pin=lambda p:dict(path=str(p.resolve()),sha256=sha(p));write=lambda p,v:p.write_text(json.dumps(v,ensure_ascii=False,indent=2)+'\n','utf-8')
index=read(D/'source-legacy-equipment-sql-build-current.json');assert sha(Path(index['runtime']['path']))==index['runtime']['sha256'];build=read(Path(index['manifest']['path']));mapping_ref=next(x for x in build['inputs']if x['path'].endswith('.json'));assert sha(Path(mapping_ref['path']))==mapping_ref['sha256'];mapping=read(Path(mapping_ref['path']));run=D/'source-legacy-equipment-sql-reference'/datetime.now(timezone.utc).strftime('%Y%m%dT%H%M%S%fZ');run.mkdir(parents=True);shutil.copyfile(__file__,run/Path(__file__).name);q=lambda x:'`'+x.replace('`','``')+'`'
def database(renamed,profile):
 c=sqlite3.connect(':memory:');c.create_function('IF',3,lambda test,yes,no:yes if test else no)
 c.execute('CREATE TABLE character_items(id INTEGER,item_id INTEGER,count INTEGER,char_id INTEGER,is_equipped INTEGER)');c.executemany('INSERT INTO character_items VALUES(?,?,?,?,?)',[(1,200,3,7,1),(2,300,4,7,1),(3,400,5,7,0),(4,200,6,8,0)])
 c.execute('CREATE TABLE etcitem(item_id INTEGER,weight INTEGER)');c.execute('INSERT INTO etcitem VALUES(400,27)')
 for table,item in [('armor',200),('weapon',300)]:
  columns=mapping[table];names=[columns[name]if renamed else name for name in columns];assert len(names)==len(set(names));c.execute('CREATE TABLE '+q(table)+'('+','.join(q(n)for n in names)+')');values=[]
  for name in columns:
   value=profile+1
   if name=='item_id':value=item
   if name=='name':value=[table+' full name','',None,table+' unicode 裝備'][profile]
   if name=='name_id':value=table+' source name-id'
   if name=='type':value='armor'if table=='armor'else'sword'
   if name=='weight':value=13+profile
   if name=='trade':value=profile%2
   if name.startswith('use_'):value=1 if (len(name)+profile)%3 else 0
   values.append(value)
  c.execute('INSERT INTO '+q(table)+' VALUES('+','.join('?'for _ in names)+')',values)
 return c
def render(change,projected,variant):
 sql=change['projected'if projected else'original'];method=change['method']
 if method=='load':return sql%('armor','weapon','etcitem'),(7 if variant==0 else 999,)
 if method=='equippedFireResistance':return sql%'armor',(7 if variant==0 else 999,)
 if method=='rewardArmorDefinition':return sql.replace('\x01','armor'),(200 if variant==0 else 999,)
 assert method=='sourceInventorySql';table='armor'if "'armor' AS item_kind"in sql else'weapon';arguments=['i.id AS row_id,i.item_id AS catalog_id,i.count AS item_count,',table,' 'if variant==0 else' AND i.is_equipped=1 '];assert sql.count('\x01')==len(arguments)
 for value in arguments:sql=sql.replace('\x01',value,1)
 return sql,(7,)
cases=checks=old_schema_failures=0;rows=[]
try:
 for profile in range(4):
  with closing(database(False,profile))as canonical,closing(database(True,profile))as translated:
   for change in build['changes']:
    for variant in range(2):
     original,args=render(change,False,variant);projected,other=render(change,True,variant);assert args==other and original.count('?')==projected.count('?')==len(args);checks+=1
     expected=canonical.execute(original,args);labels=[x[0]for x in expected.description];values=expected.fetchall();actual=translated.execute(projected,args);assert [x[0]for x in actual.description]==labels;checks+=1;observed=actual.fetchall();assert observed==values;checks+=1+sum(map(len,observed))
     try:translated.execute(original,args).fetchall()
     except sqlite3.OperationalError:old_schema_failures+=1
     else:raise AssertionError('fixture must reproduce original English-column failure')
     rows.append(dict(profile=profile,method=change['method'],variant=variant,rows=len(observed),columns=len(labels)));cases+=1
 assert cases==old_schema_failures==40
 result=dict(status='passed40-canonical-versus-renamed-memory-SQL-parity-cases',runtime=index['runtime'],build=index['manifest'],cases=cases,checks=checks,reproduced_old_schema_failures=old_schema_failures,observations=rows,SQL_dialect='SQLite memory with IF stub; actual Source/MySQL/JDBC metadata and live character reload remain unqualified',Source_or_player_DB_connections=0,owned_fixture_SQLite_memory_connections=8,native_runtime_cases=0,created_Hub=False,full_goal_achieved=False,qualified_for_release=False,formal_deployment_changed=False,Source_or_player_SQL_or_WAL_written=False,inputs=[pin(run/Path(__file__).name),mapping_ref])
except BaseException as error:result=dict(status='failed',error=str(error),runtime=index['runtime']);raise
finally:write(run/'manifest.json',result);write(D/'source-legacy-equipment-sql-reference-current.json',dict(status=result['status'],runtime=index['runtime'],manifest=pin(run/'manifest.json')))
print(json.dumps(dict(status=result['status'],cases=cases,checks=checks)))
