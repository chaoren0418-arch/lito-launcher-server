"""Freeze and adapt the unexecuted summon tests for the composed material/selection candidate."""
from pathlib import Path
import hashlib,json
R=Path(__file__).resolve().parent;M=R.parent/'summon-material'
changes=[]
def write(name,replacements=()):
    source=M/name;text=source.read_text('utf-8')
    for old,new in replacements:
        assert text.count(old)==1,(name,old,text.count(old))
        text=text.replace(old,new)
    dest=R/name;dest.write_text(text,encoding='utf-8')
    sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest()
    changes.append(dict(source=dict(path=str(source),sha256=sha(source)),adapted=dict(path=str(dest),sha256=sha(dest)),replacements=[dict(old=a,new=b) for a,b in replacements]))
write('SummonMaterialProbe.java',[
    ('/** Real skill51 packet and unchanged Source grade/cost/map branches. */','/** Real skill51 packet; Source grade ignores a no-ring selection DWORD. */'),
    ('mode==0||mode==6||mode==7||mode==1&&before','mode==0||mode==6||mode==7||mode==1&&before||mode==5&&!before'),
    ('mode==2||mode==3||mode==4||mode==5||mode==7','mode==2||mode==3||mode==4||mode==7')])
write('test.py',[("b=read(R/'build/build.json');frozen", "b=read(R/'build/build.json');b['base']=read(R.parent/'elemental-lifetime/build/build.json')['runtime'];frozen")])
for name in ['SummonFaultProbe.java','SummonStoppedProbe.java','SummonEdgesProbe.java']:write(name)
write('test_boundaries.py',[("old=Path(build['base']['path']);", "old=Path(read(R.parent/'elemental-lifetime/build/build.json')['runtime']['path']);")])
write('SummonChoicesProbe.java',[
    ('inv.toggleNativeArmor(ring.objectId())', 'inv.toggleRing(ring.objectId())'),
    ('need(pets.isEmpty()&&(int)get(pc,"playerMp")==1000&&(int)get(pc,"playerHp")==4000\n                        &&pc.nativeDeInventory(0).countCatalog(item)==count+3,"existing LiTo selection preflight rejects before any accepted cost");',
     'need(pets.isEmpty()&&(int)get(pc,"playerMp")==1000-cost(pc,51,row)&&(int)get(pc,"playerHp")==4000-row.hpCost()\n                        &&pc.nativeDeInventory(0).countCatalog(item)==3,"Source level failure after accepted resources and materials");\n                need(((Map<?,?>)get(pc,"spellReadyNanos")).containsKey(51),"accepted level failure retains reuse");')])
write('test_choices.py',[("build/inputs/source-reference/SUMMON_MONSTER.java", "build/inputs/SUMMON_MONSTER.java")])
(R/'test-adaptations.json').write_text(json.dumps(dict(changes=changes,comparison_base='elemental-lifetime b036; includes both material and selection changes',formal_deployment_changed=False),ensure_ascii=False,indent=2),encoding='utf-8')
