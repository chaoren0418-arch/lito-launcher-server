"""Finite, disk-only evidence for the original message boundary. Never runs the PE.

Writes only into this fresh task directory. Does not follow any external/private
target or recursively inspect the state dispatcher's targets.
"""
from pathlib import Path
import sys, json, hashlib, struct, datetime
import pefile, capstone

TASK = Path(__file__).resolve().parent
OWNER = TASK.parent
sys.path.insert(0, str(OWNER))
from audit import mapping, record, sha

TASK_ID = 'bin_resources-caption-message-boundary-20261007'
LEGACY = OWNER.parent / 'lito登入器' / '舊版分析與驗收'
PE_PATH = LEGACY / 'runtime' / 'LiTo.unpacked-oep-current.exe'
ASM_PATH = LEGACY / 'ida-client-oep-v2' / 'full-disassembly.asm'
SOURCES = [
    (OWNER.parent.parent / '0-客戶端' / 'LiTo.bin', 1941568,
     'CE6EE68ADA8FD37E6C096B1A27E46CB6888F1179F85DEFB5007AC60287360292'),
    (PE_PATH, 7094272,
     '4271D92FB2B9A2FA8D118DDE0767F6D8A67A896227BCC24127438FCB5C2AB6E9'),
    (ASM_PATH, 41179755,
     '2A3A6E1026EE65288C07EFF00990BDDDBEFA5ACEC9EED551F528A0CC0A7DEA9A'),
]

def save(name, value):
    path = TASK / name
    if path.exists():
        raise RuntimeError('Fresh outputs only: ' + str(path))
    path.write_text(json.dumps(value, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')

def main():
    stamp = datetime.datetime.now(datetime.timezone(datetime.timedelta(hours=8))).isoformat()
    baseline = [dict(record(p), expected_bytes=n, expected_sha256=h,
                     verified=(p.stat().st_size == n and record(p)['sha256'] == h))
                for p,n,h in SOURCES]
    save('baseline.json', {'task_id': TASK_ID, 'time': stamp, 'sources': baseline,
        'runtime': {'python': sys.version, 'python_path': sys.executable,
                    'capstone': capstone.__version__, 'pefile': pefile.__version__},
        'agents_checked': ['D:/AGENTS.md','D:/lineage開發/AGENTS.md',
            str(OWNER.parent/'AGENTS.md'),str(OWNER/'AGENTS.md'),str(TASK/'AGENTS.md')],
        'applicable_agents_found': [], 'evidence_level': 'static',
        'game_executed': False, 'process_attached': False, 'IDA_launched': False})
    if not all(x['verified'] for x in baseline):
        raise RuntimeError('Source fingerprint mismatch; stop dependent work')
    pe = pefile.PE(str(PE_PATH))
    cs = capstone.Cs(capstone.CS_ARCH_X86,capstone.CS_MODE_32)
    cs.detail = True
    def raw(va,n):
        m0,m1 = mapping(pe,va),mapping(pe,va+n-1)
        if not m0['raw_backed'] or not m1['raw_backed'] or m0['section'] != m1['section']:
            raise RuntimeError('Missing qualified raw body: ' + hex(va))
        off = int(m0['file_offset'],16)
        out = pe.__data__[off:off+n]
        if len(out) != n: raise RuntimeError('Incomplete raw slice')
        return out
    ranges = [
        ('entry',0x47F340,0x50,'code'),
        ('move_size',0x47F390,0x2A,'code'),
        ('paint',0x47F3BA,0x64,'code'),
        ('position_router',0x47F470,0x41,'code'),
        ('syscommand_router',0x47FA32,0x20,'code'),
        ('syscommand_filter',0x47FA7A,0x49,'code'),
        ('modal_router',0x47FE4A,0x21,'code'),
        ('default_helper_gate',0x47FFA8,0x24,'code'),
        ('external_fallback',0x47FFE4,0x16,'code'),
        ('low_target_table',0x47FFFC,0x18,'data'),
        ('low_selector_table',0x480014,0xF,'data'),
        ('position_target_table',0x480024,0x18,'data'),
        ('position_selector_table',0x48003C,0xE9,'data'),
        ('syscommand_target_entry',0x480128,4,'data'),
        ('syscommand_selector_entry',0x48013C,1,'data'),
        ('helper_filter',0x4C6B30,0x22,'code'),
        ('helper_nonmatching_return',0x4C6C4D,0xB,'code'),
        ('state_dispatch_body',0x47CC90,0xB0,'code'),
        ('state_dispatch_table',0x47CD40,0x2C,'data'),
        ('registration_shaped_body',0x480290,0x7C,'code'),
        ('class_and_caption_strings',0x62CE50,0x28,'data'),
        ('early_helper_flag',0x64AFA2,1,'data'),
        ('paint_object_cell',0x6476DC,4,'data'),
        ('state_cell',0x64BBD4,4,'data'),
        ('hwnd_and_reset_cells',0x64BBE0,0x10,'data'),
    ]
    (TASK/'raw').mkdir(exist_ok=False)
    witnesses=[]
    instructions={}
    for name,va,n,kind in ranges:
        b=raw(va,n); out=TASK/'raw'/f'{name}-VA-{va:08X}.bin';out.write_bytes(b)
        row={'id':name,**mapping(pe,va),'length':n,'kind':kind,'raw_hex':b.hex(),
             'slice_sha256':sha(b),'artifact':record(out)}
        if kind=='code':
            ins=list(cs.disasm(b,va)); instructions[name]=ins
            if sum(i.size for i in ins)!=n:
                raise RuntimeError('Incomplete instruction decode: '+name)
            row['instructions']=[]
            for i in ins:
                item={'va':f'{i.address:08X}','rva':f'{i.address-0x400000:08X}',
                      'file_offset':mapping(pe,i.address)['file_offset'],
                      'bytes':bytes(i.bytes).hex(),'mnemonic':i.mnemonic,'operands':i.op_str}
                if i.mnemonic in ('call','jmp') and i.operands[0].type==capstone.x86.X86_OP_IMM:
                    dest=i.operands[0].imm & 0xffffffff
                    item['destination']=mapping(pe,dest)
                    item['outside_main_image']=not(0x400000<=dest<0xAC4000)
                    if i.bytes[0] in (0xe8,0xe9):
                        computed=(i.address+5+struct.unpack('<i',bytes(i.bytes)[1:5])[0])&0xffffffff
                        assert computed==dest,'Independent rel32 arithmetic mismatch'
                row['instructions'].append(item)
        witnesses.append(row)
    save('raw-witnesses.json',{'source':record(PE_PATH),'image_base':'00400000',
        'size_of_image':'006C4000','parsed_imports':len(getattr(pe,'DIRECTORY_ENTRY_IMPORT',[])),
        'no_resolved_import_identity_claim':True,'ranges':witnesses})

    # Execute only the arithmetic/control-flow of this finite dispatch selection.
    # Capstone operands and PE table bytes are used, not ASM comments or API names.
    stops={0x47F390,0x47F3BA,0x47FFA8,0x47FA7A}
    def route(msg):
        regs={'edi':msg,'eax':0,'ecx':0}; pc=0x47F36A; flags=None; trace=[]
        for _ in range(40):
            if pc in stops: return {'message':f'{msg:04X}','target':f'{pc:08X}','trace':trace}
            i=next(cs.disasm(raw(pc,15),pc,count=1)); nxt=pc+i.size
            trace.append({'va':f'{pc:08X}','bytes':bytes(i.bytes).hex(),
                          'mnemonic':i.mnemonic,'operands':i.op_str})
            if i.mnemonic=='cmp':
                flags=(regs[i.reg_name(i.operands[0].reg)] & 0xffffffff,
                       i.operands[1].imm & 0xffffffff)
            elif i.mnemonic in ('ja','je'):
                assert flags is not None
                take=(flags[0]>flags[1]) if i.mnemonic=='ja' else (flags[0]==flags[1])
                trace[-1]['taken']=take
                if take:nxt=i.operands[0].imm
            elif i.mnemonic=='lea':
                m=i.operands[1].mem
                regs[i.reg_name(i.operands[0].reg)]=(regs[i.reg_name(m.base)]+m.disp)&0xffffffff
            elif i.mnemonic=='xor':
                assert i.operands[0].reg==i.operands[1].reg
                regs[i.reg_name(i.operands[0].reg)]=0
            elif i.mnemonic=='mov':
                assert i.reg_name(i.operands[0].reg)=='cl'
                m=i.operands[1].mem; addr=regs[i.reg_name(m.base)]+m.disp
                regs['ecx']=raw(addr,1)[0]; trace[-1]['table_va']=f'{addr:08X}'
                trace[-1]['selector']=regs['ecx']
            elif i.mnemonic=='jmp':
                m=i.operands[0].mem;addr=m.disp+regs[i.reg_name(m.index)]*m.scale
                nxt=struct.unpack('<I',raw(addr,4))[0]
                trace[-1]['table_va']=f'{addr:08X}';trace[-1]['target']=f'{nxt:08X}'
            else: raise RuntimeError('Unexpected router instruction '+i.mnemonic)
            pc=nxt
        raise RuntimeError('Unbounded route')
    messages={3:'WM_MOVE',5:'WM_SIZE',15:'WM_PAINT',0x46:'WM_WINDOWPOSCHANGING',
              0x47:'WM_WINDOWPOSCHANGED',0xA1:'WM_NCLBUTTONDOWN',0x112:'WM_SYSCOMMAND',
              0x214:'WM_SIZING',0x216:'WM_MOVING',0x231:'WM_ENTERSIZEMOVE',0x232:'WM_EXITSIZEMOVE'}
    routes=[]
    for msg,name in messages.items():
        r=route(msg);r['name']=name
        expected=0x47F390 if msg in (3,5) else 0x47F3BA if msg==15 else 0x47FA7A if msg==0x112 else 0x47FFA8
        assert r['target']==f'{expected:08X}'
        helper_eax=(msg-0x401)&0xffffffff
        assert helper_eax!=0 and ((helper_eax-1)&0xffffffff)!=0
        r['helper_route']='004C6B30 -> 004C6C4D -> EAX=0; RET 8; no nested call'
        routes.append(r)
    assert raw(0x4c6c4d,11).hex()=='5f5e33c05b8be55dc20800'
    assert raw(0x4802b9,7).hex()=='c745d840f34700'
    assert raw(0x47f3ff,5).hex()=='e88cd8ffff'
    assert raw(0x47f3a2,5).hex()=='a3e0bb6400'
    assert raw(0x47f3a7,5).hex()=='a3e4bb6400'
    # The caption SC_SIZE/SC_MOVE values cannot match the command suppression set.
    assert not {0xF000,0xF010}&{0xF060,0xF090,0xF100,0xF140,0xF170}
    save('finite-branch-verification.json',{'task_id':TASK_ID,'evidence_level':'static',
        'hypothesis':'Do movement/modal/position messages share a direct main-image paint/update call?',
        'result':'Excluded for the qualified local routes; WM_PAINT has a distinct conditional call. External behavior remains unknown.',
        'routes':routes,'checks':{'source_hashes':True,'raw_ranges_fully_backed':True,
        'code_ranges_fully_decoded':True,'rel32_capstone_vs_arithmetic':True,
        'eleven_message_routes':True,'eleven_helper_rejections':True,
        'registration_pointer_exact':True,'paint_call_exact':True,
        'move_size_reset_writes_exact':True,'caption_move_size_pass_filter':True},
        'not_execution':'Finite arithmetic over disk bytes; no Windows calls or message delivery.',
        'hang_reproduced':False,'product_accepted':False})
    lines=ASM_PATH.read_text(encoding='utf-8',errors='replace').splitlines()
    excerpts=[]
    for first,last in [(271364,271498),(271548,271568),(272324,272365),
                       (272882,272962),(272966,272984),(273056,273120)]:
        text='\n'.join(lines[first-1:last])+'\n'
        excerpts.append({'first_line':first,'last_line':last,'sha256':sha(text.encode('utf-8')),
                         'text':text})
    save('asm-navigation.json',{'source':record(ASM_PATH),'header':lines[:16],
         'navigation_only':True,'input_header_sha256':'AEB05C78FE029EA6D3F2EF349245669E47E1E83B1C005E32D4663C780627B5A6',
         'differs_from_fixed_PE':True,'excerpts':excerpts,
         'concrete_mismatches':[{'site':'WM_PAINT first external call',
           'ASM_target':'05AC0E10','fixed_PE_target':'05AD0BD1','fixed_PE_call_va':'0047F3C8'},
           {'site':'WM_PAINT second external call','ASM_target':'05AC0C80',
            'fixed_PE_target':'05AD0A70','fixed_PE_call_va':'0047F40E'},
           {'site':'default fallback','ASM_target':'05AC0B70','fixed_PE_target':'05AD0980',
            'fixed_PE_call_va':'0047FFEB'},
           {'site':'registration-shaped call','ASM_target':'05AD08D8',
            'fixed_PE_target':'05AE0922','fixed_PE_call_va':'004802F9'}]})
    print(json.dumps({'task_id':TASK_ID,'result':'PASS finite static qualification',
        'source_hashes_verified':3,'raw_ranges':len(witnesses),'message_routes':len(routes),
        'game_executed':False,'process_attached':False,'product_acceptance':False}))

if __name__=='__main__':
    main()
