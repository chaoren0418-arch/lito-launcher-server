// Native roster codes are type+28; result-list codes are type+1.
// Keep all native rank marks, text, ready/status handling and old five classes.
static DWORD g_classLobbyResume=0x46351F,g_classLobbyLegacy=0x4633BA;
static DWORD g_classRoomResume=0x4637B5,g_classRoomLegacy=0x463650;
static DWORD g_classResultResume=0x463B2E,g_classResultLegacy=0x463B06;
static int ExtendedClassIconId(int type,int variant) {
    return type>=5 && type<=6 && variant>=0 && variant<5 ? 233+(type-5)*5+variant : -1;
}
static void* ExtendedClassBitmap(int type,int variant) {
    const int id=ExtendedClassIconId(type,variant);
    const int count=*reinterpret_cast<const int*>(0x64A95C);
    auto table=*reinterpret_cast<void***>(0x64A964);
    return id>=0 && id<count && table ? table[id] : nullptr;
}
static void __cdecl DrawExtendedClassIcon(void* row,int x,int y) {
    const int type=(*reinterpret_cast<unsigned*>(static_cast<BYTE*>(row)+8)&65535)-28;
    const int state=*reinterpret_cast<int*>(static_cast<BYTE*>(row)+0x70);
    const bool modern=*reinterpret_cast<int*>(0x63D54C)>=0x7726;
    const int variant=modern && state==34?4:modern && state==33?3:1;
    auto bitmap=ExtendedClassBitmap(type,variant);
    auto surface=*reinterpret_cast<void**>(0x64B438);
    if(!bitmap || !surface) return;
    auto draw=reinterpret_cast<void(__fastcall*)(void*,void*,void*,int,int)>(0x4B9980);
    draw(surface,bitmap,reinterpret_cast<void*>(0x835B3C),x,y);
    if(variant==1) {
        auto red=ExtendedClassBitmap(type,2);
        if(red) reinterpret_cast<void(__fastcall*)(void*,void*,int,int,int)>(0x4B9AF0)(surface,red,state,x,y);
    }
}
static void __cdecl DrawExtendedResultIcon(int code,int x,int y) {
    auto bitmap=ExtendedClassBitmap(code-1,0);auto surface=*reinterpret_cast<void**>(0x64B438);
    if(bitmap && surface) reinterpret_cast<void(__fastcall*)(void*,void*,void*,int,int)>(0x4B9980)
        (surface,bitmap,reinterpret_cast<void*>(0x835B3C),x,y);
}
__declspec(naked) void ClassLobbyIconBridge() {
    __asm {
        cmp edi,33
        jl legacy
        cmp edi,34
        jg legacy
        pushfd
        pushad
        push esi
        push ebx
        push ecx
        call DrawExtendedClassIcon
        add esp,12
        popad
        popfd
        jmp dword ptr [g_classLobbyResume]
    legacy:
        cmp dword ptr ds:[063D54Ch],07726h
        jmp dword ptr [g_classLobbyLegacy]
    }
}
__declspec(naked) void ClassRoomIconBridge() {
    __asm {
        cmp edi,33
        jl legacy
        cmp edi,34
        jg legacy
        pushfd
        pushad
        push esi
        push ebx
        push ecx
        call DrawExtendedClassIcon
        add esp,12
        popad
        popfd
        jmp dword ptr [g_classRoomResume]
    legacy:
        cmp dword ptr ds:[063D54Ch],07726h
        jmp dword ptr [g_classRoomLegacy]
    }
}
__declspec(naked) void ClassResultIconBridge() {
    __asm {
        cmp esi,6
        jl legacy
        cmp esi,7
        jg legacy
        pushfd
        pushad
        push ebx
        push edi
        push esi
        call DrawExtendedResultIcon
        add esp,12
        popad
        popfd
        jmp dword ptr [g_classResultResume]
    legacy:
        push ebx
        cmp esi,5
        push edi
        jmp dword ptr [g_classResultLegacy]
    }
}
static void AddExtendedClassIconPatches(std::vector<NumericPatch>& patches) {
    for(auto site:{0x4633B0u,0x463646u})
        AddNumericRelativePatch(patches,site,{0x81,0x3D,0x4C,0xD5,0x63,0,0x26,0x77,0,0},0xE9,
            site==0x4633B0u?reinterpret_cast<void*>(&ClassLobbyIconBridge):reinterpret_cast<void*>(&ClassRoomIconBridge),
            "seven-class-roster-icons");
    AddNumericRelativePatch(patches,0x463B01,{0x53,0x83,0xFE,0x05,0x57},0xE9,
        reinterpret_cast<void*>(&ClassResultIconBridge),"seven-class-result-icons");
}
