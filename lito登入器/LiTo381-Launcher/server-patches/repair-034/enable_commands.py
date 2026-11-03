from pathlib import Path
import sys,os,subprocess,json
r=Path(__file__).resolve().parent
f=Path(r'D:\lineage開發\3-lito開發\lito伺服器\LiTo381-Server\runtime\formal-standard-20261011')
sys.path.insert(0,str(f))
from clone_databases import properties,PROJECT,BIN
cfg=properties(PROJECT/'config/database.properties')
env=os.environ.copy();env['MYSQL_PWD']=cfg.get('Password_LOGIN',cfg.get('Password',''))
def query(sql):
    p=subprocess.run([str(BIN/'mysql.exe'),'--protocol=TCP','--host=127.0.0.1','--user='+cfg.get('Login_LOGIN',cfg.get('Login','')),'--default-character-set=utf8','--batch','--skip-column-names'],input=sql.encode('utf-8'),env=env,capture_output=True,creationflags=subprocess.CREATE_NO_WINDOW)
    if p.returncode:raise RuntimeError(p.stderr.decode('utf-8',errors='replace'))
    return p.stdout.decode('utf-8')
before=query("SELECT name,access_level,class_name,note,system FROM lito_play_20261011_static.commands WHERE name IN ('item','spawn');")
backup=r/'commands-migration-before.tsv'
if not backup.exists():backup.write_text(before,encoding='utf-8')
source=query("SELECT name,access_level,class_name,note,system FROM lito_static.commands WHERE name IN ('item','spawn');")
(r/'source-command-policy.tsv').write_text(source,encoding='utf-8')
for name,executor,note in [('item','L1CreateItem','LiTo create item'),('spawn','L1SpawnCmd','LiTo spawn monster')]:
    query(f"INSERT INTO lito_play_20261011_static.commands (name,access_level,class_name,note,system) SELECT '{name}',200,'{executor}','{note}',0 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM lito_play_20261011_static.commands WHERE name='{name}');")
after=query("SELECT name,access_level,class_name,system FROM lito_play_20261011_static.commands WHERE name IN ('item','spawn');")
(r/'commands-migration-after.tsv').write_text(after,encoding='utf-8')
print(after)
print('Only two command definitions inserted in isolated static copy; original source and account privileges unchanged.')
