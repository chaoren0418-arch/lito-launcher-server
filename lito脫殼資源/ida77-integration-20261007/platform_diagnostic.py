"""Finite read-only native error diagnosis; no additional process launch."""
import sys
sys.dont_write_bytecode=True
import ctypes as C
from ctypes import wintypes as W
import json,struct
from pathlib import Path
from owned_process import HERE,save,digest,api
from private_profile import UE,bind,registration_state
profile=json.loads((HERE/'private-profile-candidate.json').read_text('utf-8'))
runtime=Path(profile['runtime_root']);exe=runtime/'python.exe'
GetFolder=bind(UE,'GetAppContainerFolderPath',[W.LPCWSTR,C.POINTER(C.c_void_p)],C.c_long)
OLE=C.WinDLL('ole32',use_last_error=True)
Free=bind(OLE,'CoTaskMemFree',[C.c_void_p],None)
pointer=C.c_void_p();hr=GetFolder(profile['app_sid'],C.byref(pointer))
try:folder=C.wstring_at(pointer) if pointer else None
finally:
    if pointer:Free(pointer)
GetAttr=api('GetFileAttributesW',[W.LPCWSTR],W.DWORD)
attributes=GetAttr(str(exe));native_error=C.get_last_error() if attributes==0xFFFFFFFF else None
data=exe.read_bytes();pe=struct.unpack_from('<I',data,60)[0]
copies=[]
for row in profile['python_dependencies']:
    copies.append({'source':row['source'],'destination':row['destination'],'expected_sha256':row['sha256'],
        'source_sha256_after':digest(row['source']),'copy_sha256_after':digest(row['destination']),
        'matching':digest(row['source'])==row['sha256']==digest(row['destination'])})
state=registration_state(profile['app_sid'],profile['moniker'])
record={'native_executable_readable_by_trusted_launcher':True,'native_GetFileAttributesW':attributes,
    'native_GetFileAttributesW_error':native_error,'executable':str(exe),'exe_sha256':digest(exe),
    'exe_is_PE':data[pe:pe+4]==b'PE\0\0','exe_machine':hex(struct.unpack_from('<H',data,pe+4)[0]),
    'exe_path_character_count':len(str(exe)),'runtime_directory_exists':runtime.is_dir(),'all_twelve_pinned_copies':copies,
    'GetAppContainerFolderPath':{'hresult_signed':hr,'hresult_hex':'0x%08X'%(hr&0xFFFFFFFF),'returned_path':folder},
    'registration_state_after_failed_launch':state,
    'registration_state_unchanged':state==profile['before_registration_state'],
    'additional_processes_launched':False,'CreateAppContainerProfile_or_global_configuration_write_called':False,
    'causal_limit':'Executable existence and profile lookup are directly observed. The exact internal missing object in CreateProcessW is not observed; do not assert it is necessarily profile registration.',
    'scope_blocker':'Only attempted compliant path is unregistered derived-SID LPAC. CreateProcessW rejected it with winerror 2 before creating a native probe. A registered profile would write outside BIN ownership and has not been attempted.'}
save(HERE/'platform-diagnostic.json',record)
print(json.dumps({'private_exe_exists':attributes!=0xFFFFFFFF,'all_copies_match':all(r['matching'] for r in copies),
    'folder_lookup_hresult':record['GetAppContainerFolderPath']['hresult_hex'],'registration_state_unchanged':record['registration_state_unchanged']}))
