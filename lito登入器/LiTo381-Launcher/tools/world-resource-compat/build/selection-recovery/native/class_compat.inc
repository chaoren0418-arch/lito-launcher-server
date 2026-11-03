#include "class_skills.inc"
#include "class_compat.h"
#include "profile_location.inc"
static LONG g_classCompatibilityState{};
char* __fastcall SevenClassName(char* output, int type, int sex) {
    const int id=LitoClasses::TextId(type,sex);
    const int count=*reinterpret_cast<int*>(0x834BC0);
    const auto table=*reinterpret_cast<char***>(0x834BC4);
    const char* text=(id>=0 && id<count && table && table[id]) ? table[id] : "";
    // This formatter is called with the native 132-byte character-info buffer.
    if(output) { std::size_t n=0; while(text[n] && n<131) {output[n]=text[n];++n;} output[n]=0; }
    return output;
}
const LitoClasses::Animation* __fastcall SevenClassPortrait(int type,int sex,BYTE* animate) {
    constexpr std::uintptr_t legacy[5][2]={{0x630130,0x6300D0},{0x62FFB0,0x62FF50},
        {0x62FEF0,0x62FE90},{0x630070,0x630010},{0x62FE30,0x62FDD0}};
    const bool valid=LitoClasses::Valid(type,sex);
    if(animate) *animate=valid && type!=4 ? 1 : 0;
    if(!valid) return reinterpret_cast<const LitoClasses::Animation*>(0x62FD70);
    if(type<5) return reinterpret_cast<const LitoClasses::Animation*>(legacy[type][sex]);
    return LitoClasses::ExtraPortraits[(type-5)*2+sex];
}
DWORD __fastcall DecodeSharedPortrait(BYTE* bytes,DWORD size,void* a,void* b,void* c,void* d,void* e) {
    LitoClasses::DecodePnx(bytes,size);
    return reinterpret_cast<DWORD (__fastcall*)(BYTE*,DWORD,void*,void*,void*,void*,void*)>(0x4B8B10)(bytes,size,a,b,c,d,e);
}
static WORD g_lobbyClassImages[4][4+61*73]{};
static bool g_lobbyClassImageReady[4]{};
static WORD* NativeClassBitmap(std::uintptr_t tableAddress,int id,int count) {
    if(id<0 || id>=count) return nullptr;
    auto table=*reinterpret_cast<DWORD**>(tableAddress);
    if(!table || !table[id]) return nullptr;
    auto object=reinterpret_cast<DWORD*>(table[id]);
    if(!object[1]) {
        auto vtable=reinterpret_cast<DWORD*>(object[0]);
        if(!vtable || !vtable[0]) return nullptr;
        reinterpret_cast<void (__thiscall*)(void*)>(vtable[0])(object);
    }
    return reinterpret_cast<WORD*>(object[1]);
}
bool __fastcall DrawSevenClassLobbyPortrait(int type,int sex) {
    WORD* bitmap=nullptr;
    const int legacy=LitoClasses::LegacyLobbyImage(type,sex);
    if(legacy>=0) bitmap=NativeClassBitmap(0x834BD4,legacy,0x1EC2);
    else if(LitoClasses::Valid(type,sex)) {
        const int index=(type-5)*2+sex;
        if(!g_lobbyClassImageReady[index]) {
            auto frame=NativeClassBitmap(0x834BD4,6640,0x1EC2);
            auto portrait=NativeClassBitmap(0x834BD0,LitoClasses::ExtraPortraits[index][3].first,LitoClasses::PngCount);
            g_lobbyClassImageReady[index]=LitoClasses::ComposeLobbyPortrait(g_lobbyClassImages[index],frame,portrait);
            Log("lobby class portrait type=%d sex=%d ready=%d",type,sex,g_lobbyClassImageReady[index]);
        }
        if(g_lobbyClassImageReady[index]) bitmap=g_lobbyClassImages[index];
    }
    auto surface=*reinterpret_cast<void**>(0x64B438);
    if(bitmap && surface) {
        reinterpret_cast<void (__cdecl*)(const void*,void*,int,int)>(0x4BA220)(bitmap,surface,14,31);
        return true;
    }
    return false;
}
__declspec(naked) void SevenClassLobbyDrawBridge() {
    __asm {
        movsx ecx, byte ptr [ebp-0D6h]
        movsx edx, byte ptr [ebp-0D5h]
        call DrawSevenClassLobbyPortrait
        or ebx, -1
        mov edi, 01Fh
        mov eax, 045A12Bh
        jmp eax
    }
}
static INIT_ONCE g_sharedStatusOnce=INIT_ONCE_STATIC_INIT;
static bool g_sharedStatusValid=false;
BOOL CALLBACK InitializeSharedStatus(PINIT_ONCE, PVOID, PVOID*) {
    char setting[3]{};
    DWORD count=GetEnvironmentVariableA("LITO_SHARED_EXPIRED_ICONS",setting,3);
    if(count && (count!=1 || (setting[0]!='0' && setting[0]!='1')))return TRUE;
    g_sharedExpiredIconsEnabled=!count || setting[0]=='1';
    g_sharedStatusValid=SharedStatusCatalog::Load(g_self, reinterpret_cast<void*>(0x6328D8), g_sharedStatusIcons);
    return TRUE; // memoize success or failure; no partially retried catalog
}
bool EnsureClassCompatibilityPatch() {
    if(InterlockedCompareExchange(&g_classCompatibilityState,0,0)==1) return true;
    std::vector<NumericPatch> patches;
    if(!InitOnceExecuteOnce(&g_sharedStatusOnce, InitializeSharedStatus, nullptr, nullptr) || !g_sharedStatusValid) {
        Log("shared status catalog rejected before class/status patch commit");
        return false;
    }
    Log("shared status catalog validated slots=95 preserved=1");
    AddSevenClassSkillPatches(patches);
    AddProfileLocationPatch(patches);
    AddNumericRelativePatch(patches,0x493DF0,{0x55,0x8B,0xEC,0x83,0xEC,0x30},0xE9,
        reinterpret_cast<void*>(&SevenClassName),"seven-class-name");
    AddNumericRelativePatch(patches,0x494E00,{0x55,0x8B,0xEC,0x8D,0x41,0x01},0xE9,
        reinterpret_cast<void*>(&SevenClassPortrait),"seven-class-portrait");
    AddNumericRelativePatch(patches,0x4D5319,{0xE8,0xF2,0x37,0xFE,0xFF},0xE8,
        reinterpret_cast<void*>(&DecodeSharedPortrait),"shared-portrait-pnx");
    // The same profile panel serves the lobby and room. Its original switch
    // leaves ESI as an unrelated object for types 5/6, then calls its vtable.
    AddNumericRelativePatch(patches,0x459E84,{0x8A,0x85,0x2A,0xFF,0xFF,0xFF},0xE9,
        reinterpret_cast<void*>(&SevenClassLobbyDrawBridge),"seven-class-lobby-portrait");
    for(auto address:{0x415E4Eu,0x415EBBu,0x4D4AB7u})
        patches.push_back({reinterpret_cast<BYTE*>(address),DwordBytes(LitoClasses::LegacyPngCount),
            DwordBytes(LitoClasses::PngCount),"portrait-png-count"});
    for(auto address:{0x4D4A7Eu,0x4D4B2Au})
        patches.push_back({reinterpret_cast<BYTE*>(address),DwordBytes(LitoClasses::LegacyPngCount*4),
            DwordBytes(LitoClasses::PngCount*4),"portrait-png-table-bytes"});
    // The static PNG table can precede DirectDraw initialization. Retain existing
    // objects and extend with the game's own allocator/constructor so its normal
    // destructor frees every object and the resized table on exit.
    auto& nativeTable=*reinterpret_cast<void***>(0x834BD0);
    auto oldTable=nativeTable;
    void** expanded=nullptr;
    auto alloc=reinterpret_cast<void* (__cdecl*)(std::size_t)>(0x4F118C);
    auto freeNative=reinterpret_cast<void (__cdecl*)(void*)>(0x4F103C);
    auto construct=reinterpret_cast<void* (__thiscall*)(void*,int)>(0x4D5220);
    auto discard=[&]() {
        if(!expanded) return;
        for(int i=LitoClasses::LegacyPngCount;i<LitoClasses::PngCount;++i)
            if(expanded[i]) freeNative(expanded[i]); // not loaded yet, no surface to destroy
        freeNative(expanded);
    };
    if(oldTable) {
        expanded=static_cast<void**>(alloc(LitoClasses::PngCount*sizeof(void*)));
        if(!expanded) return false;
        memset(expanded,0,LitoClasses::PngCount*sizeof(void*));
        memcpy(expanded,oldTable,LitoClasses::LegacyPngCount*sizeof(void*));
        for(int i=LitoClasses::LegacyPngCount;i<LitoClasses::PngCount;++i) {
            auto object=alloc(12);
            if(!object) {discard();return false;}
            expanded[i]=construct(object,i);
        }
        patches.push_back({reinterpret_cast<BYTE*>(0x834BD0),DwordBytes(reinterpret_cast<DWORD>(oldTable)),
            DwordBytes(reinterpret_cast<DWORD>(expanded)),"portrait-table-extension"});
    }
    if(!CommitNumericPatches(patches)) {discard();return false;}
    if(oldTable) freeNative(oldTable);
    InterlockedExchange(&g_classCompatibilityState,1);
    Log("seven-class selection installed: names=7 portraits=14 PNG=%d PNX=enabled",LitoClasses::PngCount);
    return true;
}
