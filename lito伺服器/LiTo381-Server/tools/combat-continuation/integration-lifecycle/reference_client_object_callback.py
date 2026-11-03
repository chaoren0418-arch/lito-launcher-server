"""Execute original x86 object callback in Unicorn with explicit send/tick/message stubs. Not actual cursor acceptance."""
from pathlib import Path
from datetime import datetime,timezone
import json,hashlib,struct,shutil,gc
import pefile,unicorn
from unicorn.x86_const import UC_X86_REG_ESP,UC_X86_REG_EIP,UC_X86_REG_EAX
R=Path(__file__).resolve().parent;D=R/'build';L=Path('D:/lineage開發/3-lito開發/lito登入器/LiTo381-Launcher')
read=lambda p:json.loads(p.read_text('utf-8'));sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest();pin=lambda p:dict(path=str(p.resolve()),sha256=sha(p))
def write(p,v):p.write_text(json.dumps(v,ensure_ascii=False,indent=2)+'\n','utf-8')
p=L.parent/'舊版分析與驗收/runtime/LiTo.unpacked-oep-current.exe';assert sha(p)=='4271d92fb2b9a2fa8d118dde0767f6d8a67a896227bcc24127438fcb5c2ab6e9';pe=pefile.PE(data=p.read_bytes(),fast_load=True);base=pe.OPTIONAL_HEADER.ImageBase;image=pe.get_memory_mapped_image();run=D/'client-object-callback-reference'/datetime.now(timezone.utc).strftime('%Y%m%dT%H%M%S%fZ');run.mkdir(parents=True);shutil.copyfile(__file__,run/Path(__file__).name);rows=[];checks=0
def need(value,message):
 global checks
 checks+=1
 if not value:raise AssertionError(message)
for skill in [26,42,68]:
 for target in ['explicit-self-shortcut','hover-self','hover-friendly','hover-enemy']:
  for ctrl in [False,True]:
   uc=unicorn.Uc(unicorn.UC_ARCH_X86,unicorn.UC_MODE_32);uc.mem_map(base,(len(image)+0xfff)&~0xfff);uc.mem_write(base,image);uc.mem_map(0xf000000,0x10000);uc.mem_map(0xf100000,0x3000);uc.mem_map(0xf200000,0x1000)
   w=lambda address,value:uc.mem_write(address,struct.pack('<I',value&0xffffffff));rd=lambda address:struct.unpack('<I',uc.mem_read(address,4))[0]
   player=0xf100000;peer=0xf101000;selfid=0x90001001;peerid=0x90001002;sp=0xf00f000;exitaddr=0xf200000;frames=[];messages=[];ticks=[]
   w(player+8,selfid);w(peer+8,peerid);w(player+0x4c,4000);w(peer+0x4c,4000);uc.mem_write(player+0x128,b'\x01');uc.mem_write(peer+0x128,b'\x02'if target=='hover-enemy'else b'\x01')
   w(0x834ee4,player);w(0x75a98c,player if target=='hover-self'else peer);w(0x834880,skill-1);uc.mem_write(0x83488c,b'\x00');uc.mem_write(0x63d550,bytes([ctrl]));uc.mem_write(0x64b8f6,bytes([ctrl]));w(0x64af98,4);w(sp,exitaddr);w(sp+4,-1 if target=='explicit-self-shortcut'else 0);w(sp+8,-1 if target=='explicit-self-shortcut'else 0);uc.reg_write(UC_X86_REG_ESP,sp)
   def hook(engine,address,size,data):
    if address not in [0x4c6ae0,0x4d69f0,0x417860]:return
    stack=engine.reg_read(UC_X86_REG_ESP);ret=rd(stack);args=[rd(stack+4*i)for i in range(1,6)]
    if address==0x4c6ae0:
     fmt,opcode,row,col,obj=args;raw=bytes([opcode&255,row&255,col&255])+struct.pack('<I',obj);frames.append(dict(format_address=hex(fmt),opcode=opcode,row=row,column=col,object_id=obj,raw=raw.hex()));engine.reg_write(UC_X86_REG_EAX,0)
    elif address==0x4d69f0:ticks.append(1000);engine.reg_write(UC_X86_REG_EAX,1000)
    else:messages.append(address);engine.reg_write(UC_X86_REG_EAX,0)
    engine.reg_write(UC_X86_REG_ESP,stack+4);engine.reg_write(UC_X86_REG_EIP,ret)
   hook_id=uc.hook_add(unicorn.UC_HOOK_CODE,hook);uc.emu_start(0x4c9a20,exitaddr,count=1000);need(uc.reg_read(UC_X86_REG_EIP)==exitaddr,'bounded original callback returns normally')
   accepted=target!='hover-enemy';need(len(frames)==int(accepted),'verbatim callback self/friendly path and differing relationship byte branch')
   if frames:
    expected=selfid if target in ['explicit-self-shortcut','hover-self']else peerid;raw=bytes([0x14,(skill-1)>>3,(skill-1)&7])+struct.pack('<I',expected);need(frames[0]['raw']==raw.hex(),'exact original7-byte selected-object request; no fake Ctrl or retarget');need(len(ticks)==1,'original accepted callback updates one cast clock')
   rows.append(dict(skill=skill,target=target,ctrl_cells=ctrl,frames=frames,returned=True));uc.hook_del(hook_id);del hook,rd,w,uc;gc.collect()
need(len(rows)==24 and sum(bool(x['frames'])for x in rows)==18,'full bounded original callback reference cases')
write(run/'cases.json',rows);v=dict(status='passed24-original-x86-object-callback-reference-cursor-and-field-acceptance-open',historical_image=pin(p),client_dispatch_audit=read(D/'client-skill-dispatch-audit-current.json')['manifest'],script=pin(run/Path(__file__).name),reference_cases=24,checks=checks,self_normal_and_Ctrl_cases=12,exact_plaintext_object_frames=18,callback='0x4C9A20',stubs=['0x4C6AE0 serializing sender captures original arguments','0x4D69F0 cast clock returns1000','0x417860 presentation message returns0'],not_emulated=['upstream cursor hit-testing and input dispatch','actual Ctrl key handling or foreground windows','loaded formal DLL process and encrypted field wire','server admission/effects'],cases=pin(run/'cases.json'),native_Hub_cases=0,created_Main=False,created_Hub=False,opened_game_UI=False,attached_process=False,actual_cursor_verified=False,fresh_field_packet_available=False,user_report_fixed=False,client_or_server_cause_confirmed=False,full_goal_achieved=False,formal_deployment_changed=False,Source_or_player_SQL_or_WAL_written=False)
write(run/'manifest.json',v);write(D/'client-object-callback-reference-current.json',dict(status=v['status'],manifest=pin(run/'manifest.json')));print(json.dumps(dict(status=v['status'],reference_cases=24,checks=checks,native_Hub_cases=0,user_report_fixed=False)))
