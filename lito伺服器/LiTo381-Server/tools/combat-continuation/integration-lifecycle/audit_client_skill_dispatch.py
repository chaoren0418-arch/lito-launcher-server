"""Read-only historical client image and formal DLL pin. No process attach, cursor UI or binary patch."""
from pathlib import Path
from datetime import datetime,timezone
import json,hashlib,struct,shutil,csv
import pefile,capstone
R=Path(__file__).resolve().parent;D=R/'build';L=Path('D:/lineage開發/3-lito開發/lito登入器/LiTo381-Launcher')
read=lambda p:json.loads(p.read_text('utf-8'));sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest();pin=lambda p:dict(path=str(p.resolve()),sha256=sha(p))
def write(p,v):p.write_text(json.dumps(v,ensure_ascii=False,indent=2)+'\n','utf-8')
p=L.parent/'舊版分析與驗收/runtime/LiTo.unpacked-oep-current.exe';assert sha(p)=='4271d92fb2b9a2fa8d118dde0767f6d8a67a896227bcc24127438fcb5c2ab6e9'
dll=L/'app/compat/litoddraw.dll';assert sha(dll)=='e47bd6c6ec9979c8747a523e4457133802d19f673c8dc1e67707abfa96216a8a'
run=D/'client-skill-dispatch-audit'/datetime.now(timezone.utc).strftime('%Y%m%dT%H%M%S%fZ');run.mkdir(parents=True);shutil.copyfile(__file__,run/Path(__file__).name)
pe=pefile.PE(data=p.read_bytes(),fast_load=True);base=pe.OPTIONAL_HEADER.ImageBase;md=capstone.Cs(capstone.CS_ARCH_X86,capstone.CS_MODE_32);regions=[]
for addr,size,label in [(0x4cb5f0,0x78d,'legacy-skill-dispatch-code-before-table'),(0x4cb741,0x14,'object-target-arm'),(0x4c9c80,0xad,'object-target-picker'),(0x4c9a20,0x123,'object-target-callback'),(0x4ca6e0,0x220,'ranged-target-picker'),(0x4c9e40,0x1b3,'ranged-target-callback')]:
 data=pe.get_data(addr-base,size);raw=run/(label+'.bin');raw.write_bytes(data);out=run/(label+'.asm.txt');out.write_text('\n'.join(f'{n.address:08x} {n.bytes.hex():<20} {n.mnemonic} {n.op_str}'for n in md.disasm(data,addr))+'\n','utf-8');regions.append(dict(start=hex(addr),length=size,bytes=pin(raw),disassembly=pin(out)))
table=pe.get_data(0x4cbd00-base,176);jump=pe.get_data(0x4cbc80-base,128);(run/'legacy-classification176.bin').write_bytes(table);(run/'legacy-jump-table.bin').write_bytes(jump)
matrix=read(Path(read(D/'all-targeted-source-paths-current.json')['manifest']['path']));rows=list(csv.DictReader(Path(matrix['matrix']['path']).open(encoding='utf-8',newline=''),delimiter='\t'))
for row in rows:
 skill=int(row['skill_id']);row['historical_client_dispatch']='outside native1..220 active-skill protocol; Source row may be an effect/NPC/special definition, individual applicability pending'
 if 1<=skill<=176:
  selector=table[skill-1];target=struct.unpack_from('<I',jump,selector*4)[0];row['historical_client_dispatch']=f'zero-based={skill-1}; selector={selector}; arm={target:#x}; historical unpatched image only'
  if target==0x4cb741:row['historical_client_dispatch']+='; picker4C9C80 -> callback4C9A20 -> object-target ccc-d'
 elif skill<=220:row['historical_client_dispatch']='legacy default selector-only arm4CBC27; formal DLL class_skills.inc supplies supported Dragon/Illusion target routes, individual static/field verification pending'
 row['client_dispatch_evidence_limit']='historical image + frozen DLL source locators only; actual loaded process/cursor/no-packet/field-wire not verified'
assert len(rows)==807
with (run/'target-matrix.tsv').open('w',encoding='utf-8',newline='')as f:w=csv.DictWriter(f,fieldnames=list(rows[0]),delimiter='\t');w.writeheader();w.writerows(rows)
sources=[]
for name in ['class_skills.inc','forced_combat.inc','forced_combat.h']:
 original=L/'native'/name;frozen=run/name;shutil.copyfile(original,frozen);sources.append(dict(original=pin(original),frozen=pin(frozen)))
v=dict(status='807-row-static-client-dispatch-locators-cursor-and-field-packet-unverified',historical_image=pin(p),formal_DLL=pin(dll),client_source=sources,static_inventory=read(D/'all-targeted-source-paths-current.json')['manifest'],regions=regions,classification176=pin(run/'legacy-classification176.bin'),jump_table=pin(run/'legacy-jump-table.bin'),matrix=pin(run/'target-matrix.tsv'),reported_skill26=dict(zero_based=25,selector=10,arm='0x4CB741',picker='0x4C9C80',callback='0x4C9A20',parsed_target='actual self/hover object id',normal_self_path_present_in_historical_callback=True,direct_Ctrl_requirement_visible_in_this_callback=False,upstream_pointer_input_and_loaded_DLL_behavior_unverified=True),product_scope=dict(user_clarification='lito只有競技場沒有WORLD 地圖',competition_only=True,WORLD_is_not_a_product_acceptance_gate=True),native_cases=0,created_Main=False,created_Hub=False,attached_process=False,opened_game_UI=False,actual_cursor_verified=False,fresh_field_packet_available=False,user_report_fixed=False,client_or_server_cause_confirmed=False,full_goal_achieved=False,formal_deployment_changed=False,Source_or_player_SQL_or_WAL_written=False)
write(run/'manifest.json',v);write(D/'client-skill-dispatch-audit-current.json',dict(status=v['status'],manifest=pin(run/'manifest.json')))
assert sha(p)==v['historical_image']['sha256']and sha(dll)==v['formal_DLL']['sha256']
print(json.dumps(dict(status=v['status'],rows=807,skill26_selector=10,skill26_callback='0x4C9A20',cursor_verified=False,game_UI_opened=False)))
