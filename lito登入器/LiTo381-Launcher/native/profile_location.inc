// sub_459AE0 copies a participant row before drawing the profile panel.
// Its leading room dword is also the native room protocol context and remains
// populated after S_D0. Resolve presentation without changing that context.
DWORD __fastcall ProfileLocationRoom(const BYTE* row) {
    const BYTE* own=reinterpret_cast<const BYTE*>(0x64ABF4);
    const DWORD room=*reinterpret_cast<const DWORD*>(row);
    const DWORD phase=*reinterpret_cast<const DWORD*>(0x64AF98);
    if(own[4] && strncmp(reinterpret_cast<const char*>(row+4),
            reinterpret_cast<const char*>(own+4),28)==0) {
        if(phase==1)return 0;
        if(phase==2)return *reinterpret_cast<const DWORD*>(own);
    }
    return room;
}
static DWORD g_profileLocationResume=0x45A633;
__declspec(naked) void ProfileLocationBridge() {
    __asm {
        pushad
        lea ecx, [ebp-108h]
        call ProfileLocationRoom
        mov dword ptr [esp+24], eax
        popad
        jmp dword ptr [g_profileLocationResume]
    }
}
void AddProfileLocationPatch(std::vector<NumericPatch>& patches) {
    AddNumericRelativePatch(patches,0x45A62D,{0x8B,0x8D,0xF8,0xFE,0xFF,0xFF},0xE9,
        reinterpret_cast<void*>(&ProfileLocationBridge),"profile-location-presentation");
}
