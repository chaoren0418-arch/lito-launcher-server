"""Only direct provider writers and one containing initialization caller; stop at factories."""
import sys
sys.dont_write_bytecode=True
import json,hashlib,struct
from pathlib import Path
from datetime import datetime
import capstone
ROOT=Path(__file__).resolve().parent
OUT=ROOT/'Native-provider-writer-contract'
def sha(b):return hashlib.sha256(b).hexdigest().upper()
def pin(p):
    p=Path(p);b=p.read_bytes();return {'path':str(p),'bytes':len(b),'sha256':sha(b)}
def load(p):return json.loads(Path(p).read_text('utf-8-sig'))
def read(p):
    p=Path(p);before=pin(p);b=p.read_bytes();after=pin(p);return b,{'before':before,'content_sha256':sha(b),'after':after,'stable':before==after and before['sha256']==sha(b),'read_at':datetime.now().astimezone().isoformat()}
def save(n,j):
    OUT.mkdir(exist_ok=True);p=OUT/n;assert p.resolve().is_relative_to(ROOT);p.write_text(json.dumps(j,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
def main():
    previous=load(ROOT/'Native-filename-route-contract.json');pepin=next(q for q in previous['sources'] if Path(q['path']).name=='LiTo.unpacked-oep-current.exe');asmpin=next(q for q in previous['sources'] if Path(q['path']).name=='full-disassembly.asm');pb,pr=read(pepin['path']);ab,ar=read(asmpin['path']);assert pr['before']==pepin and ar['before']==asmpin and pr['stable'] and ar['stable'];ls=ab.decode('utf-8',errors='replace').splitlines()
    refs=[{'line':i+1,'text':s} for i,s in enumerate(ls) if ('64798C' in s or '6479E8' in s) and ('mov     dword_' in s or s.startswith('dword_'))]
    contexts=[('oneinitializerSpritecache',99204,99243),('sameinitializerfallback',99276,99313),('sameinitializerbucketstem_and_writer',99358,99417),('sameinitializerloop_tail',99429,99449)]
    save('direct-writer-ASM-navigation.json',{'read':ar,'direct_global_writer_references':refs,'initializer_identity':{'name':'fn_LoadGameData_Check','proc_line':99163,'IDA_endp_line':99170,'reachable_writer_tail_after_endp':True},'contexts':[{'name':name,'start_line':a,'end_line':z,'lines':[{'line':i,'text':ls[i-1]} for i in range(a,z+1)]} for name,a,z in contexts],'qualification':'Writer/caller navigation only, authoritativefixedPE below. No42EA50reader/factorybody, no otherinitializer/deepercaller or completewriterabsenceclaim.'})
    nt=struct.unpack_from('<I',pb,60)[0];n=struct.unpack_from('<H',pb,nt+6)[0];opts=struct.unpack_from('<H',pb,nt+20)[0];base=struct.unpack_from('<I',pb,nt+52)[0]
    def raw(v,l):
        rv=v-base
        for i in range(n):
            vs,st,rs,fp=struct.unpack_from('<4I',pb,nt+24+opts+i*40+8)
            if st<=rv and rv+l<=st+rs:
                off=fp+rv-st;return pb[off:off+l],{'source':pepin,'va':f'{v:08X}','rva':f'{rv:08X}','file_offset':off,'bytes':l}
        raise AssertionError('not raw-backed')
    cs=capstone.Cs(capstone.CS_ARCH_X86,capstone.CS_MODE_32);codes=[]
    for name,v,l in [('Sprite_loader_input_and_cache',0x42ECBA,52),('fallback_wrapper_and_writer',0x42ED22,54),('bucket_stem_loader_and_writer',0x42ED8E,112),('same_loop_tail_bound16',0x42EE17,32),('cleanup_zero_register_only',0x42EF48,2),('fallback_clear_statement_only',0x42F01F,6),('bucket_clear_statement_only',0x42F0C1,6)]:
        b,o=raw(v,l);ins=[{'va':f'{i.address:08X}','bytes':i.bytes.hex(),'mnemonic':i.mnemonic,'operands':i.op_str} for i in cs.disasm(b,v)];assert b''.join(bytes.fromhex(i['bytes']) for i in ins)==b;codes.append({'name':name,'origin':o,'raw_hex':b.hex(),'raw_sha256':sha(b),'instructions':ins,'scope':'Directwriter orsameoneinitializer inputs/loop only;reset keepsonlyzero-definition/storestatements,notcleanup/lifecyclebody. Privatefactory/allocator/formatter/reader bodies unopened.'})
    data=[]
    for name,v,l,expected in [('literal_Sprite',0x5055CC,7,b'Sprite\0'),('literal_Sprite_02d',0x505DC8,11,b'Sprite%02d\0'),('Sprite_source_object_cache_disk',0x647984,4,None),('Sprite_r_source_objects_disk',0x6479A8,64,None)]:
        b,o=raw(v,l);assert expected is None or b==expected;data.append({'name':name,'origin':o,'raw_hex':b.hex(),'raw_sha256':sha(b),'literal':expected[:-1].decode('ascii') if expected else None,'u32_values':list(struct.unpack('<'+'I'*(l//4),b)) if expected is None else None,'live_identity_proof':False})
    save('fixed-PE-provider-writer-boundaries.json',{'PE_read':pr,'code_slices':codes,'data_slices':data,'one_direct_initializer_scope':True,'first_private_boundary':'42E4F0baseinitializer/42E550namedfactory/42E6D0wrapper;onlytheircallsitearguments/returnedvaluesareadopted,noneoftheirbodiesopened.','no_42EA50_body_read':True,'no_member_or_asset_extraction':True})
    mb,mr=read(ROOT/'resources/archive-manifest.json');man=json.loads(mb);candidates=[{k:a[k] for k in ['namespace','stem','index_source','pak_source','records']} for a in man['archives'] if a['namespace']=='shared' and a['stem'] in ['Sprite','Sprite03']];assert len(candidates)==2
    save('existing-matching-stem-candidates.json',{'owned_manifest_read':mr,'candidates':candidates,'qualification':'Onlyexistingsealedmanifest metadata forSprite/Sprite03 matchingdirectliteralstemarguments. No sourcePAK/indexfilebody orcurrentSHAchecked;notqualifiedproof42E550opens .idx/.pak attheseabsolute paths. No guessedLTsurf/casepolicy, no index/member/803namessearch ornewfileinventory.'})
    print(json.dumps({'code_slices':len(codes),'code_bytes':sum(c['origin']['bytes'] for c in codes),'instruction_rows':sum(len(c['instructions']) for c in codes),'data_slices':len(data),'data_bytes':sum(d['origin']['bytes'] for d in data),'direct_writer_refs':len(refs),'matched_stem_candidates':len(candidates)},ensure_ascii=False))
if __name__=='__main__':main()
