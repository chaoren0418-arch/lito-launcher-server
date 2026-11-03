"""Review new caller/Java semantics and C7 evidence adoption without runtime replay."""
from audit import *
import zipfile,importlib.util,sys
def load(n):return json.loads((ROOT/n).read_text('utf-8'))
def main():
    p=load('CD1-mode-step-contract.json');r=load('CD1-mode-step/ranges.json');checks=[]
    def ck(n,ok):
        assert ok,n
        checks.append({'check':n,'match':True})
    for q in p['sources']+list(p['existing_evidence'].values())+p['outputs']:ck('pin '+Path(q['path']).name,record(q['path'])==q)
    for q in p['prior_primary_pins']:
        a=record(q['path']);ck('prior '+Path(q['path']).name,a['bytes']==q['bytes']and a['sha256']==q['expected_sha256'])
    pe=pefile.PE(str(PE));cs=capstone.Cs(capstone.CS_ARCH_X86,capstone.CS_MODE_32);ins={}
    for q in r['new_ranges']+r['necessary_reused_raw']:
        va=int(q['va'],16);n=q.get('bytes',q.get('used_bytes'));raw=bytes.fromhex(q['raw_hex'])
        ck('raw PE '+q['va'],raw==pe.get_data(va-0x400000,n))
        ck('VA/RVA/offset '+q['va'],q['rva']==q['file_offset']==f'{va-0x400000:08X}'and q['raw_backed'])
        if 'source_pin'in q:
            ck('reused pin '+q['va'],record(q['source_pin']['path'])==q['source_pin'])
            ck('reused index '+q['va'],record(q['index']['path'])==q['index'])
        if q['kind']=='code':
            decoded=[{'va':f'{i.address:08X}','raw_hex':i.bytes.hex(),'mnemonic':i.mnemonic,'operands':i.op_str}for i in cs.disasm(raw,va)]
            ck('aligned disasm '+q['va'],decoded==q['instructions']and sum(len(bytes.fromhex(i['raw_hex']))for i in decoded)==n)
            ins.update({int(i['va'],16):i for i in decoded})
    for q in load('CD1-mode-step/instruction-witnesses.json'):
        i=ins[int(q['va'],16)];ck('operand '+q['va'],i['raw_hex']==q['raw_hex']and(i['mnemonic'],i['operands'])==(q['expected_mnemonic'],q['expected_operands']))
    # Only newly qualified caller-argument islands; do not replay Batch27 step/cipher models.
    caller_models=[]
    for entry,control,sprite,x in [(0x46D09E,0,0x187F,0x1A7),(0x46D0EC,1,0x1881,0x246)]:
        pc=entry;regs={'eax':'conditional allocated object'};stack=[];trace=[]
        while True:
            i=ins[pc];trace.append(f'{pc:08X}');mn=i['mnemonic'];op=i['operands']
            if mn=='push':stack.append(regs[op]if op in regs else int(op,0))
            elif mn=='mov':a,b=op.split(', ');regs[a]=regs[b]if b in regs else int(b,0)
            elif mn=='call':break
            else:raise AssertionError(i)
            pc+=len(bytes.fromhex(i['raw_hex']))
        args=list(reversed(stack));ck('new control caller '+str(control),i['operands']=='0x4154e0'and args==[sprite,x,242,0x46B7D0,control,0])
        caller_models.append({'control_parameter':control,'caller_VA':f'{pc:08X}','stack_order_sprite_x_y_callback_control_last':args,
          'trace':trace,'computed_only':True,'allocation_ctor_registration_click_not_qualified':True})
    ctor=[ins[a]['operands']for a in (0x4154E3,0x4154EC,0x4154FE)]
    ck('constructor first unknown forwarding',ctor==['eax, dword ptr [ebp + 0x18]','ecx, dword ptr [ebp + 0x14]','0x414910'])
    ck('no phase owner checks in bounded0/1branches',not any('0x64af98'in i['operands']or'0x63d54c'in i['operands'] for a,i in ins.items()if 0x46B7D0<=a<0x46B804))
    sys.dont_write_bytecode=True
    path=Path(p['existing_evidence']['class_readonly_parser']['path']);spec=importlib.util.spec_from_file_location('review_cf',path);mod=importlib.util.module_from_spec(spec);spec.loader.exec_module(mod)
    jarold=p['existing_evidence']['AD368_JAR'];jarnew=p['existing_evidence']['C7_JAR'];classes=load('CD1-mode-step/Java-bounded-class-methods.json');methods={}
    with zipfile.ZipFile(jarold['path'])as oz,zipfile.ZipFile(jarnew['path'])as nz:
        for row in classes:
            a=oz.read(row['entry']);b=nz.read(row['entry']);ca=mod.parse_class(a);cb=mod.parse_class(b)
            ck('class bytes '+row['class'],{'bytes':len(a),'sha256':sha(a)}==row['AD368_class']and{'bytes':len(b),'sha256':sha(b)}==row['C7_class'])
            for q in row['methods']:
                ma=next(x for x in ca['methods']if x['name']==q['name']and x['descriptor']==q['descriptor']);mb=next(x for x in cb['methods']if x['name']==q['name']and x['descriptor']==q['descriptor'])
                ck('actual code + CP symbols '+row['class']+'.'+q['name'],ma['code_hex']==mb['code_hex']and ma['instructions']==mb['instructions'])
                raw=bytes.fromhex(ma['code_hex']);start,end=q['bounded_interval']
                ck('bounded method '+row['class']+'.'+q['name'],sha(raw)==q['full_code_sha256']and raw[start:end].hex()==q['bounded_code_hex']and sha(raw[start:end])==q['bounded_code_sha256']and q['instructions']==[x for x in ma['instructions']if start<=x['offset']<end])
                methods[(row['class'],q['name'])]=q
    for q in load('CD1-mode-step/source-IL-witnesses.json'):
        lines=Path(q['source']['path']).read_text('utf-8').splitlines();start,end=q['lines_one_based'];text='\n'.join(lines[start-1:end])+'\n'
        ck('text exact '+q['source_key']+str(start),text==q['text']and sha(text.encode('utf-8'))==q['slice_utf8_sha256'])
    # Exact switch decoding connects wirebyte to enum and actual enum switch to delta.
    def switch(raw,off):
        at=(off+4)&~3;default=off+int.from_bytes(raw[at:at+4],'big',signed=True);count=int.from_bytes(raw[at+4:at+8],'big');pairs={}
        for k in range(count):
            pos=at+8+k*8;pairs[int.from_bytes(raw[pos:pos+4],'big',signed=True)]=off+int.from_bytes(raw[pos+4:pos+8],'big',signed=True)
        return default,pairs
    direction=methods[('Protocol$LitoModeStepDirection','fromWire')];draw=bytes.fromhex(direction['bounded_code_hex']);di={x['offset']:x for x in direction['instructions']}
    default,pairs=switch(draw,1);ck('wire direction switch',pairs=={1:28,2:34}and default==40)
    ck('wire1 NEXT wire2 PREVIOUS','Direction.NEXT:'in di[28]['cp_symbol']and'Direction.PREVIOUS:'in di[34]['cp_symbol'])
    synth=methods[('BattleRules$1','<clinit>')]['instructions'];mapping={}
    for k,i in enumerate(synth):
        if i.get('cp_symbol','').endswith('Direction.NEXT:Llocal/lito381/server/Protocol$LitoModeStepDirection;'):mapping['NEXT']=synth[k+2]['integer'];ck('NEXT switch-map store',synth[k+1]['mnemonic']=='invokevirtual'and synth[k+3]['opcode_hex']=='4f')
        if i.get('cp_symbol','').endswith('Direction.PREVIOUS:Llocal/lito381/server/Protocol$LitoModeStepDirection;'):mapping['PREVIOUS']=synth[k+2]['integer'];ck('PREVIOUS switch-map store',synth[k+1]['mnemonic']=='invokevirtual'and synth[k+3]['opcode_hex']=='4f')
    ck('enum switch map identity',mapping=={'NEXT':1,'PREVIOUS':2})
    mode=methods[('BattleRules$Mode','stepNative')];code=bytes.fromhex(mode['bounded_code_hex']);mi={x['offset']:x for x in mode['instructions']}
    # Small symbolic JVM interpreter for this finite pure method only. Classes never loaded.
    def step(value,direction_name):
        local={0:value,1:direction_name};stack=[];pc=0;trace=[]
        while True:
            i=mi[pc];op=int(i['opcode_hex'],16);mn=i['mnemonic'];arg=bytes.fromhex(i['operands_hex']);trace.append(pc);nxt=pc+1+len(arg)
            if 2<=op<=8:stack.append(op-3)
            elif op==16:stack.append(int.from_bytes(arg,'big',signed=True))
            elif op in (26,27,28,29):stack.append(local[op-26])
            elif op in (42,43,44,45):stack.append(local[op-42])
            elif op==21:stack.append(local[arg[0]])
            elif op in (59,60,61,62):local[op-59]=stack.pop()
            elif op in (75,76,77,78):local[op-75]=stack.pop()
            elif op==54:local[arg[0]]=stack.pop()
            elif op==188:stack.append([0]*stack.pop())
            elif op==89:stack.append(stack[-1])
            elif op==79:v=stack.pop();index=stack.pop();arr=stack.pop();arr[index]=v
            elif op==46:index=stack.pop();arr=stack.pop();stack.append(arr[index])
            elif op==190:stack.append(len(stack.pop()))
            elif op==132:local[arg[0]]+=int.from_bytes(arg[1:2],'big',signed=True)
            elif op==96:b=stack.pop();a=stack.pop();stack.append(a+b)
            elif op in (155,156):v=stack.pop();take=v<0 if op==155 else v>=0;nxt=i['branch_target']if take else nxt
            elif op in (160,161,162):b=stack.pop();a=stack.pop();take=a!=b if op==160 else a<b if op==161 else a>=b;nxt=i['branch_target']if take else nxt
            elif op==167:nxt=i['branch_target']
            elif op==171:default,pairs=switch(code,pc);nxt=pairs.get(stack.pop(),default)
            elif op==178:stack.append(mapping)
            elif op==182:
                assert 'Direction.ordinal:'in i['cp_symbol'];stack.append(stack.pop()) # symbolic enum key; syntheticmap identity checked above
            elif op==184:
                symbol=i['cp_symbol']
                if symbol=='java/lang/Math.floorMod:(II)I':b=stack.pop();a=stack.pop();stack.append(a%b)
                elif 'Mode.fromWire:'in symbol:stack.append(stack.pop()) # actual returned wire id, enum domain alreadyguarded0..5
                else:raise AssertionError(i)
            elif op==187:return {'exception':i['cp_symbol'],'trace':trace} # bounded known guard enters throw construction
            elif op==176:return {'value':stack.pop(),'trace':trace}
            else:raise AssertionError(i)
            pc=nxt;assert len(trace)<400
    order=[0,1,5,2,4,3];cycles=[]
    for value in order:
        for wire,name,delta in [(1,'NEXT',1),(2,'PREVIOUS',-1)]:
            result=step(value,name);expected=order[(order.index(value)+delta)%6]
            ck('relative cycle '+str(value)+'/'+str(wire),result.get('value')==expected)
            cycles.append({'current_wire_mode':value,'request_direction':wire,'enum':name,'next_wire_mode':expected,
              'instruction_offsets':result['trace'],'computed_only':True,'Java_executed':False,'dynamic_mode_witness':False})
    for value in (-1,6):ck('ordinary domain guard '+str(value),step(value,'NEXT').get('exception')=='java/lang/IllegalArgumentException')
    parser=methods[('Protocol','litoGameInfoRequest')]['instructions'];symbols=[i.get('cp_symbol','')for i in parser]
    ck('parser byte direction + exact length',any('Direction.fromWire:'in s for s in symbols)and any(i.get('integer')==1 and i['offset']==44 for i in parser))
    game=methods[('ClientSession','gameInfo')]['instructions'];gs=[i.get('cp_symbol','')for i in game]
    ck('parse before phase ordinary direction call',game[1]['offset']==1 and 'Protocol.litoGameInfoRequest:'in gs[1]and any('ClientSession$Phase.ROOM:'in x for x in gs)and any('changeRoomMode:'in x for x in gs))
    change=methods[('ClientSession','changeRoomMode')]['instructions'];calls=[(i['offset'],i.get('cp_symbol',''))for i in change if i['mnemonic'].startswith('invoke')]
    required=[(43,'Mode.stepNative:'),(130,'GameHub.updateSettings:'),(142,'resetRoomReadyAfterSettingsChange:'),(151,'Protocol.litoRoomMode:'),(158,'BattleRules.decode:'),(172,'Protocol.litoRoomMap:'),(236,'sendEncrypted:'),(250,'sendEncrypted:')]
    for off,fragment in required:ck('mode publication BCI'+str(off),any(a==off and fragment in s for a,s in calls))
    ck('mode failure snapshots',[(a,s)for a,s in calls if 'sendGameInfoSnapshot:'in s]==[(31,next(s for a,s in calls if a==31)),(137,next(s for a,s in calls if a==137))])
    snapshot=methods[('ClientSession','sendGameInfoSnapshot')]['instructions'];ss=[i.get('cp_symbol','')for i in snapshot]
    ck('self0/1 independent snapshot source',any('litoRoomSummary:'in s for s in ss)and any('litoRoomConfiguration:'in s for s in ss)and sum('sendEncrypted:'in s for s in ss)==2)
    update=methods[('GameHub','updateSettings')]['instructions'];u={i['offset']:i for i in update}
    for off,frag in [(25,'Room.owner:'),(34,'Room.battleStarted:'),(143,'Room.settings:'),(150,'Room.selectedMapId:'),(158,'Set.clear:'),(179,'MatchState.reconfigure:')]:ck('updateSettings BCI'+str(off),frag in u[off].get('cp_symbol',''))
    saved=load('CD1-mode-step/C7-saved-map-frame-witnesses.json');net=json.loads(Path(p['existing_evidence']['C7_network_report']['path']).read_text('utf-8'));frames=json.loads(Path(net['new_network']['frames']['path']).read_text('utf-8'))
    for q in saved:
        actual=frames[q['array_index_zero_based']];body=bytes.fromhex(actual['body_hex'])
        ck('C7 saved adoption '+str(q['array_index_zero_based']),q['source']==net['new_network']['frames']and q['body_hex']==actual['body_hex']and q['direction']==actual['direction']and q['epoch']==actual['epoch']and q['body_bytes']+2==q['frame_bytes']==actual['frame_bytes'])
        if 'map_u16LE'in q:ck('C7 map field '+str(q['array_index_zero_based']),q['map_u16LE']==int.from_bytes(body[-2:],'little'))
    byindex={q['array_index_zero_based']:q for q in saved}
    ck('C7 requested514 and509',byindex[73]['body_hex']=='d1030202'and byindex[79]['body_hex']=='d103fd01')
    ck('C7 FFFF current509 selfresponses',all(byindex[k]['body_hex']=='d103ffff'for k in (84,86))and all(byindex[k]['body_hex']=='d50301000000fd01'for k in (85,87)))
    ck('C7 saved scope is map notmode',not any(q['direction']=='C'and q['body_hex'].startswith('d102')for q in saved))
    versions=load('CD1-mode-step/version-evidence-table.json')
    ck('old absence notoverall absence','onlyoldscope'in versions[0]['map_scope']and versions[2]['JAR']['sha256'].startswith('C7B169C0'))
    ck('packet unchanged',record(ROOT/'server-packet-delta.json')==p['verification']['server_packet_unchanged'])
    ck('prior Batch27 immutable',record(ROOT/'CD1-map-outbound-contract.json')['sha256']=='9A2BA5AB692E92088E5B9BCA3C45A449EB04D7B3D2AD7AACB88EE7AAF43D598D')
    ck('knowncorroboration only',p['delta']['new_wire']==p['delta']['new_opcodes']==p['delta']['wire_corrections']==[])
    write('CD1-mode-step/computed-caller-cycle-models.json',{'conditional_ctor_callers':caller_models,'relative_cycle':cycles,
      'qualification':'New bounded Native argument and Java pure arithmetic symbolic models only; no old sender/cipher/D5 tests/network/runtime replay.'})
    review={'primary':record(ROOT/'CD1-mode-step-contract.json'),'all_match':True,'check_count':len(checks),'checks':checks,
      'new_code_ranges':7,'new_data_ranges':0,'necessary_reused_raw_ranges':3,'instruction_assertions':21,
      'bounded_Java_classes':6,'bounded_Java_methods':12,'source_IL_slices':13,'new_conditional_caller_models':2,'new_relative_cycle_models':12,
      'saved_C7_map_frame_rows_adopted':18,'new_capture_frames':0,'Native_or_mode_dynamic_witnesses':0,'prior_primary_pins_preserved':29,
      'old_PASS_or_network_replayed':False,'computed_models':record(ROOT/'CD1-mode-step/computed-caller-cycle-models.json'),
      'UI_phase_owner_transport_mode_dynamic_ACK_qualified':False}
    write('CD1-mode-step/independent-review.json',review);print(json.dumps({k:v for k,v in review.items()if k!='checks'},ensure_ascii=False))
if __name__=='__main__':main()
