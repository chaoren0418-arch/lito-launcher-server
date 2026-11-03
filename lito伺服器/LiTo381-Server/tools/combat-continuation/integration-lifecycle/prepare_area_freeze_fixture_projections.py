"""Create narrow, whole-file reversible PC80 projections; preserve originals and freeze both in each run."""
from pathlib import Path
R=Path(__file__).resolve().parent
def project(name,original,changes,assertion,receipt):
 text=(R/original).read_text('utf-8');before=text
 for old,new,count in changes:
  assert text.count(old)==count,(name,old,text.count(old),count);text=text.replace(old,new)
 inverse=text
 for old,new,count in reversed(changes):assert inverse.count(new)==count,(name,new);inverse=inverse.replace(new,old)
 assert inverse==before
 (R/name).write_text(text,'utf-8')
 driver=(R/'test_area_freeze_accepted_stage.py').read_text('utf-8');a=driver.index('original=R/');z=driver.index('for p in [Path(__file__)')
 setup=f"original=R/{original!r};text=original.read_text('utf-8');changes={changes!r}\nfor old,new,count in changes:\n assert text.count(old)==count;text=text.replace(old,new)\ninverse=text\nfor old,new,count in reversed(changes):\n assert inverse.count(new)==count;inverse=inverse.replace(new,old)\nassert inverse==original.read_text('utf-8');(inputs/{name!r}).write_text(text,'utf-8')\nshutil.copyfile(R/'FreezeAcceptedStageProbe.java',inputs/'FreezeAcceptedStageProbe.java')\n"
 driver=driver[:a]+setup+driver[z:];driver=driver.replace('area-freeze-accepted-stage',receipt).replace('AreaFreezeAcceptedStageProbe',name.removesuffix('.java')).replace("assert obs['cases']==8 and obs['premature_erase_cases']==0 and obs['premature_freeze_cases']==0",assertion)
 (R/('test_'+receipt.replace('-','_')+'.py')).write_text(driver,'utf-8')
project('AreaFreezeAcceptedIoProbe.java','FreezeAcceptedIoProbe.java',[
 ('FreezeAcceptedIoProbe','AreaFreezeAcceptedIoProbe',2),
 ('selectedSkill=50','selectedSkill=80',1),('ctrl<2','ctrl<1',1),('before?8:32','before?4:16',2),
 ('var timed=(BattleTimedStatus)get(target,"timedStatus");','for(var m:members)if(m!=pc&&m!=target)set(m,"positionX",x+11);var timed=(BattleTimedStatus)get(target,"timedStatus");',1),
 (' static boolean observe(', ' static void send(ClientSession pc,long id)throws Exception{call(pc,"handle",new Class[]{byte[].class},new Protocol.Writer().c(Protocol.C_USE_SPELL).c(9).c(7).bytes());}\n static boolean observe(',1)
],"assert obs['cases']==16 and obs['actual_native_frame_monitor_waits']==16 and obs['stale_freeze_frames']==0",'area-freeze-accepted-io')
project('AreaFreezeAcceptedDurationProbe.java','FreezeAcceptedDurationProbe.java',[
 ('FreezeAcceptedDurationProbe','AreaFreezeAcceptedDurationProbe',2),('new int[]{50})','new int[]{80})',1),('values.get(50)','values.get(80)',1),('values.put(50,','values.put(80,',1),('cases==8&&queued==8','cases==4&&queued==4',1)
],"assert obs['cases']==4 and obs['wrong_snapshot_duration_cases']==0",'area-freeze-accepted-duration')
print('prepared two independently reversible PC80 projections; original files unchanged')
