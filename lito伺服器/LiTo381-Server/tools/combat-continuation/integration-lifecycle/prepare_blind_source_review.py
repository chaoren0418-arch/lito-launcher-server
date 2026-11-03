"""New strict readonly review adapter. Never edit the sealed previous review or Source files."""
from pathlib import Path
R=Path(__file__).resolve().parent
source=R/'review_companion_wind_source.py';text=source.read_text('utf-8')
old=" else:assert a==b;scope='whole file unchanged since exact skill68 material/GM review'"
assert text.count(old)==1
new=""" elif p.name=='L1ItemInstance.java':
  assert sha(f)=='91f48a0e0cc7f05fdb8ac39770bb8f2a7d0c57a1f86e9bbcd688f7725a1ada3a'
  if a!=b:
   assert sha(p)=='0ba0e50cf7e47649a03c2f69b29a562d097ce5b54be4d8f2b55c848830f811b0'
   before='\\tprivate int _enchantLevel;// 物品強化質'
   after='\\tprivate volatile int _enchantLevel;// 戰鬥 Proc 直接讀取；穿戴中強化不一定重建 Proc 快取'
   assert a.count(before)==b.count(after)==1 and b.replace(after,before)==a
   restored=p.read_bytes().replace(after.encode('utf-8'),before.encode('utf-8'))
   assert restored==f.read_bytes() and hashlib.sha256(restored).hexdigest()=='91f48a0e0cc7f05fdb8ac39770bb8f2a7d0c57a1f86e9bbcd688f7725a1ada3a'
   scope='whole-file inverse exact for the sole enchant-level volatile modifier/comment; all constructor/getter/material/selector/role method bodies and data representations unchanged. This review qualifies material/role gates, not every concurrent equipment Proc producer.'
  else:scope='whole file unchanged since exact skill68 material/GM review'
"""+old
(R/'review_blind_scene_source.py').write_text(text.replace(old,new),'utf-8')
