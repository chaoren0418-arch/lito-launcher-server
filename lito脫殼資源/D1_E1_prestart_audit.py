"""Batch17 finite ready-self/countdown receive audit; no WORLD descendants."""
from audit import *
OUT=ROOT/'D1-E1-prestart'
FIXED='4271D92FB2B9A2FA8D118DDE0767F6D8A67A896227BCC24127438FCB5C2AB6E9'
PREVIOUS='50A26C6526BC3BE8365DA435AE50117A25265EDE1AB0DC1E2F170BAEA95006C4'

def load(n):return json.loads((ROOT/n).read_text(encoding='utf-8'))

def main():
    OUT.mkdir(exist_ok=True);pe=pefile.PE(str(PE));cs=capstone.Cs(capstone.CS_ARCH_X86,capstone.CS_MODE_32)
    sources=[record(p)for p in [PE,BIN,ASM]]
    assert [p['sha256']for p in sources]==[FIXED,'CE6EE68ADA8FD37E6C096B1A27E46CB6888F1179F85DEFB5007AC60287360292','2A3A6E1026EE65288C07EFF00990BDDDBEFA5ACEC9EED551F528A0CC0A7DEA9A']
    prev=load('SD0-receive-contract.json');pin=record(ROOT/'SD0-receive-contract.json');assert pin['sha256']==PREVIOUS
    prior=prev['prior_primary_pins']+[{**pin,'expected_sha256':PREVIOUS,'match':True}];assert len(prior)==18
    for r in prior:assert record(Path(r['path']))['sha256']==r['expected_sha256']
    server=ROOT.parent/'lito伺服器/server-room-ready-prestart-consumer-contract.json';sup=record(server);assert sup['sha256']=='57F324FE093C80A8DAE8B7E207A9A8A628AA508E04EBB38AFE17F3C33D6274F8'
    server_obj=json.loads(server.read_text(encoding='utf-8'));existing={'server_ready_prestart_contract':sup,'server_manifest':server_obj['manifest'],
      'native_dispatch':record(LEGACY/'analysis/NATIVE-S-DISPATCH.json'),'Batch15_comparison_contract':record(ROOT/'SCD-member-comparison-contract.json')}
    for key in ['native-ready-reference.json','consumer-handoff.json','existing-ready-countdown-wire.json']:
        q=server_obj['evidence'][key];assert record(Path(q['path']))==q;existing[key]=q
    assert record(Path(existing['server_manifest']['path']))==existing['server_manifest']
    packet=record(ROOT/'server-packet-delta.json');assert packet['sha256']=='B4C00BF1C7F8D5FC17F0B819965527A63E9F9F3BDE822E1F1D4BA59A7733B54C'
    ins={};owners={};ranges=[];inherited=[]
    def decode(raw,va,q):
        rows=[]
        for i in cs.disasm(raw,va):
            r={'va':f'{i.address:08X}','raw_hex':i.bytes.hex(),'mnemonic':i.mnemonic,'operands':i.op_str};rows.append(r);ins[i.address]=r;owners[i.address]=q
        assert sum(len(bytes.fromhex(r['raw_hex']))for r in rows)==len(raw),(hex(va),'alignment')
        return rows
    def take(va,q,n,kind,index):
        raw=Path(q['path']).read_bytes();assert record(Path(q['path']))==q and raw==pe.get_data(va-0x400000,len(raw))and n<=len(raw)
        inherited.append({**mapping(pe,va),'range':q,'index':index,'used_bytes':n,'kind':kind,'raw_matches_fixed_PE':True,'old_test_replayed':False})
        if kind=='code':decode(raw[:n],va,q)
    def reuse(index,va,n,kind='code'):
        row=next(r for r in load(index)['ranges']if int(r['va'],16)==va);take(va,row['output'],n,kind,record(ROOT/index))
    for index,va,n,kind in [
      ('priority-complete-tails.json',0x4A2A50,296,'code'),('SCE-navigation/ranges.json',0x4B29DB,56,'code'),('SCE-navigation/ranges.json',0x4B313F,30,'code'),
      ('skill-pane-frame-coordinate/ranges.json',0x469CA0,24,'code'),('SCE-navigation/ranges.json',0x469CB8,17,'code'),('SCE-navigation/ranges.json',0x46A244,20,'data'),
      ('SCE-navigation/ranges.json',0x469EC3,264,'code'),('SCE-navigation/ranges.json',0x41B410,15,'code'),('SCE-navigation/ranges.json',0x41AA90,5,'code'),
      ('SCE-navigation/ranges.json',0x41AAA0,5,'code'),('SCE-navigation/ranges.json',0x41B6C0,17,'code'),('SCE-navigation/ranges.json',0x41B6E0,53,'code'),
      ('SCE-navigation/ranges.json',0x4765F0,41,'code'),('SCD-member-comparison/ranges.json',0x4A966D,244,'code'),
      ('SCD-member-comparison/ranges.json',0x465530,70,'code'),('SCD-member-comparison/ranges.json',0x463BD0,13,'code'),
      ('receive-boundary/ranges.json',0x4D65F0,254,'code'),('receive-boundary/ranges.json',0x4D69F0,31,'code'),('S0C-status-owner/ranges.json',0x4F118C,6,'code'),
      ('SCD-member-comparison/ranges.json',0x4F103C,11,'code'),('SCD-member-comparison/ranges.json',0x4F1186,6,'code'),
      ('server-packet-evidence/ranges.json',0x5025A8,2,'data')]:reuse(index,va,n,kind)
    oldindex=ROOT.parent/'lito重建版/LiTo381-Modern/logs/continuation-20261003/r496-source-evidence.json';old=json.loads(oldindex.read_text(encoding='utf-8'))
    q=next(r for r in old['PE_fragments']if Path(r['path']).name=='pe-4A9550.bin');take(0x4A9550,q,88,'code',record(oldindex));existing['R496_evidence_index']=record(oldindex)
    specs=[(0x4A95A8,197,'code','D1_complete_self_phase_gap'),(0x4AABB0,272,'code','E1_complete_handler'),(0x469FCB,210,'code','scene_mode3_only'),
      (0x4B2F37,12,'code','D1_shared_dispatch'),(0x4B2F97,12,'code','E1_primary_dispatch'),(0x4B3200,5,'code','E1_fallback_ignore_return'),
      (0x4C8790,5,'code','sound_thunk_depth_stop'),(0x458C60,61,'code','count_format_notification_wrapper'),
      (0x471E30,35,'code','phase4_number_callback'),(0x471E60,32,'code','phase4_sentinel_FE_callback'),(0x471E80,32,'code','phase4_sentinel_FD_callback'),
      (0x634460,3,'data','D1_sc_format'),(0x4B3541,1,'data','D1_primary_map'),(0x4B33E4,4,'data','D1_primary_jump'),
      (0x4B3785,1,'data','D1_fallback_map'),(0x4B363C,4,'data','D1_fallback_jump'),(0x4B3551,1,'data','E1_primary_map'),
      (0x4B3404,4,'data','E1_primary_jump'),(0x4B3795,1,'data','E1_fallback_map'),(0x4B36B0,4,'data','E1_fallback_jump')]
    for va,n,kind,label in specs:
        raw=pe.get_data(va-0x400000,n);assert len(raw)==n;p=OUT/f'{va:08X}-{n:X}.bin';p.write_bytes(raw);q=record(p)
        ranges.append({**mapping(pe,va),'bytes':n,'kind':kind,'label':label,'raw_hex':raw.hex(),'output':q,'instructions':decode(raw,va,q)if kind=='code'else[]})
    def w(at):return {'fixed_PE_sha256':FIXED,**mapping(pe,at),**ins[at],'range':owners[at]}
    expected=[(0x4B2F39,'call','0x4a9550'),(0x4B2F99,'call','0x4aabb0'),(0x4B3204,'ret',''),
      (0x4A9561,'push','0x1c'),(0x4A9564,'push','0x634460'),(0x4A956A,'call','0x4a2a50'),
      (0x4A9577,'cmp','eax, 2'),(0x4A9581,'cmp','eax, 3'),(0x4A9588,'jne','0x4a975a'),(0x4A9597,'call','0x4d3a80'),
      (0x4A959E,'jne','0x4a965d'),(0x4A95A4,'cmp','byte ptr [ebp - 1], 1'),(0x4A95A8,'jne','0x4a963e'),
      (0x4A95BB,'sete','cl'),(0x4A95C0,'mov','byte ptr [0x648f1c], cl'),(0x4A95CF,'je','0x4a95d8'),
      (0x4A95D1,'call','0x47e360'),(0x4A95D8,'call','0x47cf00'),(0x4A95DD,'cmp','dword ptr [0x64af98], esi'),(0x4A95E3,'je','0x4a975a'),
      (0x4A95EE,'mov','ecx, 3'),(0x4A95F5,'je','0x4a9658'),(0x4A95F7,'call','0x469ca0'),
      (0x4A960D,'mov','dword ptr [0x64a9b4], edx'),(0x4A9613,'mov','dword ptr [0x64a9b8], eax'),(0x4A9618,'mov','dword ptr [0x64a9bc], ecx'),
      (0x4A961E,'mov','dword ptr [0x62c9c0], 0x469bd0'),(0x4A9628,'mov','dword ptr [0x62c9bc], 0x47b700'),(0x4A9632,'mov','dword ptr [0x62c9b8], 0x47b6f0'),
      (0x4A9644,'je','0x4a975a'),(0x4A964F,'mov','ecx, 2'),(0x4A9656,'jmp','0x4a95f5'),(0x4A9658,'call','0x469ca0'),(0x4A9663,'je','0x4a975a'),
      (0x4A96C6,'mov','byte ptr [edx + eax*4 + 0x44], cl'),(0x4A96CD,'movsx','eax, byte ptr [ebp - 1]'),(0x4A96D9,'call','0x465530'),
      (0x4A9742,'mov','byte ptr [edx + eax*4 + 0x44], cl'),(0x4A9755,'call','0x465530'),
      (0x469FD1,'mov','dword ptr [0x64af98], 3'),(0x46A07C,'mov','dword ptr [0x62c9b8], 0x46d640'),
      (0x46A086,'mov','dword ptr [0x62c9bc], 0x46d8a0'),(0x46A091,'mov','dword ptr [0x62c9c0], 0x46d630'),
      (0x4AABBF,'call','0x4a2a50'),(0x4AABCC,'cmp','eax, 3'),(0x4AABD1,'cmp','eax, 6'),(0x4AABDE,'cmp','al, 1'),
      (0x4AABE0,'mov','ecx, 0x2776'),(0x4AABE7,'mov','ecx, 0x2775'),(0x4AABEC,'call','0x4c8790'),
      (0x4AAC10,'and','edx, 0xff'),(0x4AAC1E,'call','0x458c60'),(0x4AAC23,'mov','eax, dword ptr [0x64af98]'),
      (0x4AAC2B,'cmp','eax, 4'),(0x4AAC2E,'jne','0x4aacbc'),(0x4AAC48,'mov','edx, 0x471e30'),(0x4AAC50,'call','0x4d65f0'),
      (0x4AAC5B,'add','edi, 0x3e8'),(0x4AAC6C,'cmp','cl, 5'),(0x4AAC73,'mov','edx, 0x471e60'),(0x4AAC78,'mov','ecx, 0x1388'),
      (0x4AAC85,'mov','edx, 0x471e80'),(0x4AAC96,'call','0x4d65f0'),(0x4AACA8,'push','-1'),(0x4AACB7,'call','0x4d65f0'),
      (0x471E32,'mov','byte ptr [0x62c478], cl'),(0x471E38,'jle','0x471e52'),(0x471E4D,'jmp','0x4c8790'),
      (0x471E65,'mov','byte ptr [0x62c478], 0xfe'),(0x471E7A,'jmp','0x4c8790'),(0x471E85,'mov','byte ptr [0x62c478], 0xfd'),(0x471E9A,'jmp','0x4c8790'),
      (0x4C8790,'jmp','0x4c85e0'),(0x458C79,'call','0x773f6680'),(0x458C94,'call','0x458bb0'),
      (0x4D65FE,'call','0x4d69f0'),(0x4D66CD,'mov','dword ptr [esi], edx'),(0x4D66CF,'mov','dword ptr [esi + 4], eax'),(0x4D66D5,'mov','dword ptr [esi + 8], eax'),
      (0x4D66DE,'mov','dword ptr [0x834c00], eax'),(0x4D6A04,'call','0x5010863')]
    assertions=[]
    for at,mn,op in expected:
        assert (ins[at]['mnemonic'],ins[at]['operands'])==(mn,op),(hex(at),ins.get(at));assertions.append({**w(at),'expected_mnemonic':mn,'expected_operands':op,'match':True})
    for mapva,b,jva,target in [(0x4B3541,0x77,0x4B33E4,0x4B2F37),(0x4B3785,0x33,0x4B363C,0x4B2F37),(0x4B3551,0x7F,0x4B3404,0x4B2F97),(0x4B3795,0x50,0x4B36B0,0x4B3200)]:
        assert pe.get_data(mapva-0x400000,1)==bytes([b])and int.from_bytes(pe.get_data(jva-0x400000,4),'little')==target
    assert pe.get_data(0x234460,3)==b'sc\0' and pe.get_data(0x1025A8,2)==b'c\0'
    captures=[];peer_orders=[]
    wire_obj=json.loads(Path(existing['existing-ready-countdown-wire.json']['path']).read_text(encoding='utf-8'))
    for label,example in zip(['5ED','49A'],wire_obj['examples']):
        q=example['original_frames'];p=Path(q['path']);assert record(p)==q;rows=json.loads(p.read_text(encoding='utf-8'))
        selected=[(i,r)for i,r in enumerate(rows)if r.get('direction')=='S'and r.get('body_hex','')[:2]in['d1','e1']]
        assert len(selected)==10
        for i,r in selected:
            body=bytes.fromhex(r['body_hex']);op=body[0]
            if op==0xD1:
                end=body.index(0,1);name=body[1:end];assert end+2==len(body)and body[-1]==1 and len(name)in[22,23]
                fields=[{'index':1,'type':'NUL terminated8bit alias','body_offset':1,'bytes':end,'raw_hex':name.hex(),'ASCII_capture_label':name.decode('ascii')},{'index':2,'type':'c/u8state','body_offset':end+1,'bytes':1,'value':1}]
            else:
                assert len(body)==2 and body[1]in[1,2,3];fields=[{'index':1,'type':'c/u8count','body_offset':1,'bytes':1,'value':body[1]}]
            assert r['frame_bytes']==len(body)+2
            captures.append({'source':q,'capture':label,'array_index_zero_based':i,'frame_1based':i+1,'epoch':r['epoch'],'direction':'S','opcode':op,
              'body_hex':body.hex(),'body_bytes':len(body),'frame_bytes':r['frame_bytes'],'fields':fields,'consumed':len(body),'remaining':0,
              'classification':'Known sealed wire corroboration; not new runtime/native phase/comparison/GUI acceptance'})
        for epoch in ['Host-GAME','Guest-GAME']:
            same=[r for r in captures if r['capture']==label and r['epoch']==epoch]
            assert [r['fields'][0]['value']for r in same if r['opcode']==0xE1]==[3,2,1]
            assert max(r['frame_1based']for r in same if r['opcode']==0xD1)<min(r['frame_1based']for r in same if r['opcode']==0xE1)
            peer_orders.append({'capture':label,'epoch':epoch,'D1_E1_ordinals':[r['frame_1based']for r in same],'E1_values':[3,2,1],'ordering':'Per-peer receipt order only; no Native phase or Java atomic transition proof'})
    assert len(captures)==20
    assert [(r['capture'],r['frame_1based'])for r in captures if r['opcode']==0xE1 and r['epoch']=='Guest-GAME'and r['fields'][0]['value']==3]==[('5ED',95),('49A',86)]
    wire=[{'direction':'server-to-client','opcode_hex':'D1','opcode_decimal':209,'schema':'sc','VA':'004A9550','RVA':'000A9550','fixed_PE_sha256':FIXED,
      'fields':[{'index':1,'type':'NUL8bitstring','endianness':'byte stream','encoding':'ActualNative lead/fold initializedauthority required; capturesASCII,JavaMS950 separate','capacity_including_NUL':28,'body_offset':1},
        {'index':2,'type':'8bitstate','endianness':'singlebyte','body_offset':'2+A','domain':'Known0NOT_READY/1READY/2SHOP_LIST; Native self tests exactly1, doesnotvalidate0..2here; recordbyte/UI MOVSX i8'}],
      'body_bytes':'3+A','frame_bytes':'5+A','absent_fields':['roomId','epoch','requestID','owner/source/physicalID'],'trigger':'Java ready publication priorE1 peracceptedcontract; not a local command intent or native self authority',
      'state_gate':'Nativephase2/3/6; self compare64ABF8; Batch15 tableguard','evidence':'Pinned completehandler assembledfrom2reusedraw+newgap;8sealedframes','unknown':'Actualnativephase/table/selfidentity, externalreaderbounds, world-preparation call effects stopped at call boundary'},
      {'direction':'server-to-client','opcode_hex':'E1','opcode_decimal':225,'schema':'c','VA':'004AABB0','RVA':'000AABB0','fixed_PE_sha256':FIXED,
       'fields':[{'index':1,'type':'8bitunsigneddisplay/count','endianness':'singlebyte','body_offset':1,'encoding':'none','Native_range_check':'none','Java_domain':'known3/2/1ticks;0cancellationsource/ILonly'}],
       'body_bytes':2,'frame_bytes':4,'trigger':'Java COUNTDOWN transition/tick/cancel publication; sameClientSessionROOM remainsseparate','states':'Nativeprimarydispatcher only; handlerphase3/6 soundmessage thenre-readphase; phase4 enqueuepresentationcallbacks; otherreturn',
       'absent_fields':['roomId','epoch/generation','deadline/timestamp','startgrant','owneridentity'],'evidence':'Pinned complete272bytehandler/directhelpers and12sealed3/2/1frames','unknown':'actualNativephase/UI/schedulerauthority, cancellation0 dynamicbehavior, tabledependentD1transition, Java publishrace/postcommit outsideNativequalification'}]
    d1={'phase_admission':[2,3,6],'room_gate':'none inpacket/handler; Nativeglobalcurrentcontext only','self_compare':{'call':w(0x4A9597),'left':'64ABF8','right':'incoming EBP-20','authority':'unchangedBatch15 initializedlead/fold guard, notplainASCII orzero tables'},
      'state1_self':{'sets648F1C':'initialphase==6 viaSETEqu beforeopaque preparationcall','preparation_calls':{'if':'648EB8 !=0 AND834C18 !=0','then':w(0x4A95D1),'else':w(0x4A95D8),'depth':'WORLD/preparation entry boundary only; bodies/descendants not read, executed, reconstructed orqualified'},
        'after_call':'re-read64AF98; if6returnbeforemembertail. OtherwiseECX3 ->469CA0; callbacks mayhavechangedphase. Noatomicworld/nativeownerready inferred'},
      'non1_self':{'values':'0/2/otherbyte; only1ready','phase6':'immediatereturnbeforephase/memberwrites','otherwise':'ECX2 ->469CA0, no47E360/47CF00preparationcall'},
      'callback_override':{'if':'64AFC7 !=0 forbothphase2/3paths','order':'call469CA0 first, then save resulting62C9B8/BC/C0 into64A9B4/B8/BC; install62C9C0=469BD0,BC=47B700,B8=47B6F0','oldcallbacks':'savedAFTERhelperreturns, notbeforeincomingD1','unknown':'virtual/runtimeobjects plusinstalledcallbacksactivation/body; no callback execution proof'},
      'common_tail':'re-readphase==6 returnsforbothself/nonself. Else combatant firstmatch state+44/UI465530 thenreturn; ifnotfoundotherkindfirstmatch. Batch15 tail/UIhelpersunchanged.',
      'phase6_detail':'Nonself6 skipsmembertail. Self1 maymake preparationcallthenifresultingphase6return; non1self6returnswithoutpreparation. FullNativehandler qualificationdoesnot grantprivatecall execution authority.',
      'no_new_send_claim':'D1codecontainsno4C6AE0call; opaque preparation/scene/callback descendants remainunknown. No inventedC_D2command fromreceipt.'}
    e1={'dispatch':'Primary mode64BBD4==4/signedphase>=3→4B2F97; fallbackmap50hex→4B3200pureepiloguereturn; unlikeD1 bothmaps handle',
      'phase3_6':{'sound':'count==1 ECX2776 else2775; EDX7F→4C8790 JMP4C85E0 depthstop','message':'834B90 signedcount>4C picks*[834B94]+130, else63D618; unsignedcount&FF→458C60 with5034A0 color; external773F6680 format then458BB0 ifresult>0; initializedresource/UI/externalunknown',
        'phase_write':'noneinE1handler','timer':'notqueuedwhilephase remains3/6','post_callback_phase':'re-read64AF98 aftercalls; callbacks couldchangestate to4, thenphase4queuepossible; nofrozenstateassumption'},
      'phase4':{'count':'n=u8no0..5rangecheck','number_events':'i=0..n-1:4D65F0(delay=i*1000,callback471E30,param=n-i)',
        'special_event':'ifn==5 delay5000 callback471E60 param0; else delayn*1000 callback471E80 param0',
        'final_event':'delay(n+1)*1000 callback471E30 param-1','n0':'nofirstloop; queues471E80 at0 and471E30(-1)at1000. NoWORLD/start orqueuecancelinstruction.',
        'callbacks':'471E30 storesCL→62C478, signedCL>0 and64B090==0 sound2777;471E60 storesFE→62C478 sound2778 if64B090==0;471E80 storesFD sound2779if64B090==0. Nonecallsphase/setworld/packetwriter.',
        'queue_helper':'existingcomplete4D65F0 gets4D69F0time, addsdelay andinserts12byte{due,callback,param}into834C08 withcount834C00/cap834C04, allocating/copyingifneeded. Noqueueclearing/callbackinvocation here.',
        'queue_unknowns':['clock4D69F0 existing31byteprefix/private05010863; noactualtimeauthority','allocator/free/external77409100 outcomes','initializedqueuecapacity/order/epoch','timerpump invocation/removal/clearing/cancellation notqualified; noothercallerdecompilation'],
        'scope':'OtherNativephase4 presentation schedule classifiedseparately fromroom3/6; notserverCOUNTDOWN orworldstart. NoE0startpacket invented.'},
      'E1_zero':'Java cancellation source/ILmeaning separatefromNative count0; Native3/6 chooses2775/message0, no directphase2reset; phase4n0queuespresentationevents. No real0frame in5ED/49A.',
      'local_send':'No directpacketwriter inE1handler,these3callbacks,queuehelper,soundthunkorformatwrapper; deeperopaquehelpersnotqualified. Adjacentunreferencedfunctionsnotadopted.',
      'freshness':'Noepoch/room/nonce/timestamp. Orderedexplicitadapter currentroommetadata; repeats/races maynotbeidentifiedfromwire. No localtimerexpiry syntheticWORLD.'}
    helpers={'scene_mode3':'469CA0prefix62C21C0 setsphase3andreturn; nonzerojump469FCB writesphase3 thenUIvisibility/selectionflags, virtual+20/+24, childflags0..7, defaultcallbacks46D640/46D8A0/46D630. NoWORLDbody followed.',
      'scene_mode2':'unchangedBatch14 469EC3 264bytebranch; phase2 UI/defaultcallbacks; virtualunknown remains.',
      'all_bounded_call_and_external_jump_sites':[{**w(at),'qualification':'directnativeinstruction/target only; unchangedreusedhelper orboundednewhelper qualified, allotherdeep/private/virtualeffects unknown'}for at in sorted(ins)if (ins[at]['mnemonic']=='call'or (ins[at]['mnemonic']=='jmp'and ins[at]['operands'].startswith('0x')and int(ins[at]['operands'],16)not in ins))and any(lo<=at<hi for lo,hi in [(0x4A9550,0x4A9761),(0x4AABB0,0x4AACC0),(0x469FCB,0x46A09D),(0x471E30,0x471E53),(0x471E60,0x471E80),(0x471E80,0x471EA0),(0x458C60,0x458C9D),(0x4C8790,0x4C8795),(0x4D65F0,0x4D66EE)])],
      'WORLD_depth_stops':[w(0x4A95D1),w(0x4A95D8)],'unknowns':['actualtables/selfglobal/vector/UIlifetime','47E360/47CF00world-preparation descendants','scenevirtualobjects/callbacks activation anddynamicphaseeffects','sound4C85E0/resources/backend','externalformat773F6680and458BB0UI','schedulerprivateclock/queuepump/lifetime','noE1/00/repeat/cancel/partialwirecoverage','Javaatomiccommit/publicationrace notNativeclaim']}
    consumer={'R515_gap':'Delegatedreference5ED1based95e103 equalssealedGuestframe95; noR515traceinspectedorreplayed. ParseknownE1 independentlyofNativecomparisonandGUIqualification inexplicitAD368adapter.',
      'three_state_domains':'JavaClientSessionROOM, GameHubWAITING/COUNTDOWN, Nativephase2/3/6 aredifferent. D1 self1 nominalphase3 onlyafterqualifiedprivatecall/scene; non1phase2 except6. State2shopnotboolready.',
      'ready_vs_intent':'D1receivedprojection isnotC_D2localintent, ownerauthority, allreadyprooforphysical/sourceownerready. IncomingD1/E1noroomepoch; bindownedtransportepoch/currentCEroom/localjoingeneration.',
      'countdown':'E1displayreceipt3/2/1; E1/00cancellationJava source/ILonly, neverstart. Adapter mayclearcountdisplay underexplicitpolicy; exactNativephase2resetnotprovenbyE1.',
      'stop':'No localcountdownexpiryWORLD/start/packet synthesis. Do notdecode/adoptfollowingWORLD/body, source/physical/worldentry requiresseparatequalification.',
      'guard':'KeepBatch15 lead/fold guard; opaqueNativepreparation/UI/callback/timer effects remainunknown. No newtable/provider/UIinjection orJava takeover.'}
    verification={'instruction_assertions':len(assertions),'new_code_ranges':11,'new_data_ranges':9,'inherited_raw_ranges':len(inherited),'complete_D1_handler_bytes':529,'complete_E1_handler_bytes':272,
      'sealed_D1_frames':8,'sealed_E1_frames':12,'prior_primary_pins':18,'all_match':True,'server_packet_unchanged':packet,'old_test_replayed':False}
    outputs={'ranges.json':{'source':sources[0],'ranges':ranges,'inherited_raw':inherited},'instruction-witnesses.json':{'assertions':assertions},
      'wire-contracts.json':{'contracts':wire},'D1-self-phase-boundary.json':d1,'E1-countdown-boundary.json':e1,'direct-helper-boundaries.json':helpers,
      'sealed-ready-countdown-frames.json':{'captures':captures,'per_peer_order':peer_orders,'cancellation_repeat_partial_native_qualified':False},'consumer-gap.json':consumer,'new-evidence-verification.json':verification}
    for n,o in outputs.items():write('D1-E1-prestart/'+n,o)
    primary={'schema':'lito.native-D1-E1-prestart-boundary.v1','updated_at':stamp(),'status':'FINITE_STATIC_READY_SELF_COUNTDOWN_BOUNDARY_SEALED_WORLD_ENTRY_STOPPED_GUARD_RETAINED',
      'sources':sources,'prior_primary_pins':prior,'existing_evidence':existing,'reusable_outputs':[record(p)for p in sorted(OUT.iterdir())if p.is_file()and p.name not in ['independent-review.json','READ-ME.txt']],
      'wire':wire,'D1_self':d1,'E1_countdown':e1,'direct_helpers':helpers,'consumer_gap':consumer,'verification':verification,
      'delta':{'new_wire':[],'wire_corrections':[],'new_opcodes':[],'known_corroboration':['D1scselfphasebranches','E1cdispatch/roomnotification/phase4presentationqueue','20existingsealedD1/E1frames'],
        'new_internal_boundary':'D1state1privatepreparationentry stopsbeforeWORLDdescendants; E1phase3/6doesnotdirectlychangephase,0isnotnativephase2reset/start'},
      'scope':{'writes':'ownresourcefolderonly','native_started':False,'GUI_input':False,'SQL':False,'provider_called_or_installed':False,'new_capture':False,'Java_taken_over':False,
        'intercepted_work_reinterpreted':False,'WORLD_entry_bodies_or_following_packets_analyzed':False,'battle_results_expanded':False,'physical_source_player_owner_ready':False,'overall_complete':False}}
    write('D1-E1-prestart-contract.json',primary);print(json.dumps({'primary':record(ROOT/'D1-E1-prestart-contract.json'),'verification':verification},ensure_ascii=False))

if __name__=='__main__':main()
