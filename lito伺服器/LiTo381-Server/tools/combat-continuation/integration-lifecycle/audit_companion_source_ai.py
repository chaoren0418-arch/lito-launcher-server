"""Read-only Source call-chain evidence for the next actor AI/perception implementation."""
from pathlib import Path
from datetime import datetime,timezone
import re,json,hashlib,shutil
R=Path(__file__).resolve().parent;D=R/'build';T=Path('D:/lineage開發/1-伺服器/src/com/lineage/server')
sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest();pin=lambda p:dict(path=str(p.resolve()),sha256=sha(p))
def write(p,v):p.write_text(json.dumps(v,ensure_ascii=False,indent=2)+'\n','utf-8')
def method(text,signature):
 assert text.count(signature)==1,signature
 masked=re.sub(r'//[^\n]*|/\*[\s\S]*?\*/|"(?:\\.|[^"\\])*"|\'(?:\\.|[^\'\\])*\'',lambda m:''.join('\n' if c=='\n' else ' ' for c in m[0]),text)
 start=text.index(signature);brace=masked.index('{',start);depth=0
 for n in range(brace,len(masked)):
  depth+=(masked[n]=='{')-(masked[n]=='}')
  if depth==0:return text[start:n+1]
 raise AssertionError('unclosed method '+signature)
run=D/'companion-source-ai-audits'/datetime.now(timezone.utc).strftime('%Y%m%dT%H%M%S%fZ');source_dir=run/'Source';source_dir.mkdir(parents=True);files={}
for relative in ['model/Instance/L1SummonInstance.java','model/Instance/L1NpcInstance.java','utils/Teleportation.java','model/L1Object.java']:
 original=T/relative;frozen=source_dir/original.name;shutil.copyfile(original,frozen);files[original.name]=dict(original=pin(original),frozen=pin(frozen))
summon=Path(files['L1SummonInstance.java']['frozen']['path']).read_text('utf-8');npc=Path(files['L1NpcInstance.java']['frozen']['path']).read_text('utf-8');teleport=Path(files['Teleportation.java']['frozen']['path']).read_text('utf-8')
obj=Path(files['L1Object.java']['frozen']['path']).read_text('utf-8')
bodies={
 'object-showId-setter':method(obj,'public void set_showId(int showId)'),
 'summon-noTarget':method(summon,'public boolean noTarget()'),
 'summon-command':method(summon,'public void onFinalAction(final L1PcInstance player, final String action)'),
 'npc-teleport-three-arguments':method(npc,'public void teleport(final int nx, final int ny, final int dir)'),
 'npc-teleport-current-map-delegate':method(npc,'private boolean teleportInternal(final int nx, final int ny, final int dir, final boolean logOnFail)'),
 'portal-pet-sync':method(teleport,'private static void syncPetsToOwnerLanding(final L1PcInstance pc, final short mapId, final int head)')
}
assert bodies['summon-noTarget'].count('teleport(newX, newY, _master.getHeading())')==2 and 'teleportTo(' not in bodies['summon-noTarget'];assert 'teleportInternal(nx, ny, dir, true)' in bodies['npc-teleport-three-arguments'] and 'teleportInternal(nx, ny, getMapId(), dir, logOnFail)' in bodies['npc-teleport-current-map-delegate'];assert 'teleport(pet, loc.getX(), loc.getY(), mapId, head)' in bodies['portal-pet-sync'];assert 'NumberUtils.toInt(action != null ? action.trim() : "", 0)' in bodies['summon-command']
methods={}
for name,body in bodies.items():
 out=run/(name+'.java.txt');out.write_text(body+'\n','utf-8');methods[name]=pin(out)
assert '_showId = showId;' in bodies['object-showId-setter'] and 'World.' not in bodies['object-showId-setter'];assert bodies['portal-pet-sync'].index('pet.set_showId(pc.get_showId())')<bodies['portal-pet-sync'].index('if (!teleport(pet, loc.getX(), loc.getY(), mapId, head))')
facts=[
 'Status3 noTarget returns immediately. Status4 scatters within distance<5 on the same map and otherwise changes to status3. Status5 returns to home within one tile and resets home to current position when path direction is unavailable.',
 'Default fake-master noTarget branch clears targets when map differs or distance>12; it tries ten terrain-only candidate centers within +/-2, then calls the three-argument NPC teleport.',
 'Ordinary aggressive status1 with an existing target returns before following. Otherwise a map difference or distance>30 calls three-argument NPC teleport around master numeric coordinates within +/-1; this overload retains the NPC current map, despite the follow/hard-pull comments.',
 'Ordinary same-map following starts beyond distance2. Ten unavailable path directions reset the counter and change status to3. No-target AI must be ported from the actual calls, not from comments describing cross-map teleport.',
 'The explicit portal pet-sync path separately supplies the destination map, copies instance/showId, searches randomLocation(3,false), keeps the original body on failed landing, and removes previous viewers only after successful relocation.',
 'Portal sync assigns the master showId before attempting landing; the setter is a plain field assignment, and failed landing does not restore it. Cross-instance failure semantics and visibility-index ownership need separate Source fixtures; ordinary map4-to0 cases with equal instance IDs do not prove this boundary.',
 'Owner command parsing uses NumberUtils.toInt on trimmed input. Numeric aliases such as padded/leading-zero 6 require an actual raw request gate, independently of group statuses1..5.'
]
v=dict(status='verified-readonly-Source-AI-teleport-call-chain-next-implementation-open',Source=files,methods=methods,facts=facts,Source_or_player_SQL_or_WAL_written=False,production_dispatch_changed=False,full_goal_achieved=False);write(run/'manifest.json',v);write(D/'companion-source-ai-audit-current.json',dict(status=v['status'],manifest=pin(run/'manifest.json'),full_goal_achieved=False));print(json.dumps(dict(status=v['status'],frozen_methods=len(methods),Source_files=len(files),AI_implementation_complete=False)))
