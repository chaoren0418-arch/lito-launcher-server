"""Explicit inverse versions for Source153 visible-range admission and screen rejection."""
from pathlib import Path
import json,hashlib
R=Path(__file__).resolve().parent;out=R/'source-negative-range-fixtures';out.mkdir(exist_ok=True);sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest();pin=lambda p:dict(path=str(p.resolve()),sha256=sha(p))
source=R/'source-negative-failure-fixtures/SourceNegativeFailureStateProbe.java';original=source.read_text('utf-8');rows=[]
for reject in [False,True]:
 name='SourceNegativeScreenRejectProbe'if reject else'SourceNegativeVisibleRangeProbe';changes=[('SourceNegativeFailureStateProbe',name),('for(boolean phase:new boolean[]{false,true})','for(boolean phase:new boolean[]{false})'),('cases==216','cases==108')]
 if reject:
  changes += [('pet.relocate(x+6,y);','pet.relocate(x+18,y+18);need(!BattleSpellCatalog.isInNativeScreen(x,y,x+18,y+18),"actual Source screen rejects diagonal despite Chebyshev18");'),('.h(x+6).h(y)','.h(x+18).h(y+18)'),('boolean admitted=paid==cost(pc,selectedSkill,spell)&&spent==skill.reagentCount();','boolean admitted=paid==0&&spent==0&&!((Map<?,?>)get(pc,"spellReadyNanos")).containsKey(selectedSkill);'),('Source controlled class failure still pays one admitted cast, without NPC slow mutation','Source out-of-screen rejection pays zero and reserves no reuse, preserving NPC state')]
 else:
  changes += [('pet.relocate(x+6,y);','int distance=selectedSkill==152?10:12;pet.relocate(x+distance,y);need(BattleSpellCatalog.isInNativeScreen(x,y,x+distance,y),"actual Source visible selected position");'),('.h(x+6).h(y)','.h(x+distance).h(y)')]
 text=original
 for old,new in changes:assert old in text;text=text.replace(old,new)
 inverse=text
 for old,new in reversed(changes):assert new in inverse;inverse=inverse.replace(new,old)
 assert inverse==original
 target=out/(name+'.java');target.write_text(text,'utf-8');rows.append(dict(original=pin(source),versioned=pin(target),changes=changes))
(out/'manifest.json').write_text(json.dumps(dict(status='whole-file-inverse-range-fixtures',rows=rows,competition_only=True,native_cases_each=108),ensure_ascii=False,indent=2)+'\n','utf-8');print('PASS two inverse range fixtures')
