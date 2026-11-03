"""Recheck only the frozen material selector against current Source, without touching either project input."""
from pathlib import Path
import json,hashlib,difflib
R=Path(__file__).resolve().parent;S=R.parents[1];B=S/'tools/resource-database/build';D=R/'build/source-drift';D.mkdir(exist_ok=True)
read=lambda p:json.loads(p.read_text('utf-8'));sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest();pin=lambda p:dict(path=str(p.resolve()),sha256=sha(p))
projection=B/'de-native-cast-material-development/material-source-projection.json';v=read(projection)
inventory=Path('D:/lineage開發/1-伺服器/src/com/lineage/server/model/L1PcInventory.java');source=inventory.read_text('utf-8-sig')
assert sha(inventory)=='2f218f1362a209e03cb1e9b531194a92d406bf5ae4ea99b253bb21f907816863'
def block(marker):
 start=source.index(marker);a=source.index('{',start);depth=1;i=a+1
 while depth:
  if source[i]=='{':depth+=1
  elif source[i]=='}':depth-=1
  i+=1
 return source[start:i]
rows=[]
for marker,old in v['original_blocks'].items():
 current=block(marker);assert current==old,marker
 rows.append({'marker':marker,'equal':True,'sha256':hashlib.sha256(current.encode()).hexdigest()})
old=B/'de-native-cast-material-development/source-inputs/CharacterTable.java';current=Path('D:/lineage開發/1-伺服器/src/com/lineage/server/datatables/user/CharacterTable.java')
assert sha(old)=='f90c45e5257fae2839ebf5d12b367e443c7c0da3cd2626e364ac104914d93bc2' and sha(current)=='d402edd98e621e8bf97cb790ddb22a306a91005aab7fa4e90389aedba81b5781'
patch=D/'character-source-delta.patch';patch.write_text(''.join(difflib.unified_diff(old.read_text('utf-8').splitlines(True),current.read_text('utf-8').splitlines(True),fromfile='frozen',tofile='current')),encoding='utf-8')
receipt={'status':'passed-current-Source-material-selector-block-equality-only','projection':pin(projection),'current_inventory':pin(inventory),'equal_blocks':rows,'CharacterTable':{'frozen':pin(old),'current':pin(current),'diff':pin(patch),'review':'Only character-save map-time snapshot capture changed. This is not a material selector input and does not authorize adding Source character SQL writes to LiTo.'},'remaining':'Inventory transfer/quarantine/deadline persistence changes are outside these four equal blocks; full current-source inventory qualification is not claimed. Keep memory-only LiTo semantics.','formal_changed':False,'source_files_or_SQL_written':False}
(D/'selection-requalification.json').write_text(json.dumps(receipt,ensure_ascii=False,indent=2),encoding='utf-8');print('PASS four exact Source selector blocks unchanged; full inventory parity still open')
